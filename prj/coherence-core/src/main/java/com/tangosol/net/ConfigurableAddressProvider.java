/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * ConfigurableAddressProvider is an implementation of the AddressProvider
 * interface based on a static list of addresses configured in an XML element
 * that contains one or more items in the following format:
 * <pre>
 * &lt;socket-address&gt;
 * &nbsp;&nbsp;&lt;address&gt;...&lt;/address&gt;
 * &nbsp;&nbsp;&lt;port&gt;...&lt;/port&gt;
 * &lt;/socket-address&gt;
 * ...
 * &lt;address&gt;...&lt;/address&gt;
 * </pre>
 * The order of items in the configured list will be randomized to provide basic
 * load balancing.
 * <p>
 * This implementation is not thread safe.
 *
 * @author gg 2008-08-18
 * @since Coherence 3.4
 */
public class ConfigurableAddressProvider
        extends AbstractSet
        implements DescribableAddressProvider
    {
    /**
     * Construct an instance of ConfigurableAddressProvider based on the
     * specified XML element.
     * <p>
     * Unresolvable addresses will be skipped.
     *
     * @param xmlConfig  the XML element that contains the configuration info
     */
    @Deprecated
    public ConfigurableAddressProvider(XmlElement xmlConfig)
        {
        this(xmlConfig, /* fSafe */ true);
        }

    /**
     * Constructs a {@link ConfigurableAddressProvider} using the specified
     * {@link AddressHolder}s.
     *
     * @param addressHolders  the {@link AddressHolder}s
     * @param fSafe           true if the provider skips unresolved addresses
     */
    public ConfigurableAddressProvider(Iterable<AddressHolder> addressHolders, boolean fSafe)
        {
        List<AddressHolder> listHolders = new ArrayList<>();

        for (AddressHolder holder : addressHolders)
            {
            String sHost = holder.getHost();
            if (sHost.contains(","))
                {
                Arrays.stream(sHost.split(","))
                        .map(String::trim)
                        .forEach(s -> listHolders.add(new AddressHolder(s, holder.getPort())));
                }
            else
                {
                listHolders.add(holder);
                }
            }

        m_fSafe       = fSafe;
        m_listHolders = sortHolders(listHolders);
        }

    /**
     * Construct an instance of ConfigurableAddressProvider based on the
     * specified XML element.
     *
     * @param xmlConfig  the XML element that contains the configuration info
     * @param fSafe      true if the provider skips unresolved addresses
     */
    @Deprecated
    public ConfigurableAddressProvider(XmlElement xmlConfig, boolean fSafe)
        {
        configure(xmlConfig);
        m_fSafe = fSafe;
        }

    /**
     * Creates an instances of ConfigurableAddressProvider or
     * RefreshableAddressProvider that refresh the address list
     * of the ConfigurableAddressProvider asynchronously.
     *
     * @param xmlConfig  the XML element that contains the configuration info
     *
     * @return an instance of the corresponding AddressProvider implementation
     */
    @Deprecated
    public static AddressProvider makeProvider(XmlElement xmlConfig)
        {
        ConfigurableAddressProvider ap = new ConfigurableAddressProvider(xmlConfig);

        return ap.m_fResolve ? new RefreshableAddressProvider(ap) : ap;
        }

    // ----- AddressProvider interface --------------------------------------

    /**
    * {@inheritDoc}
    */
    public synchronized InetSocketAddress getNextAddress()
        {
        List list   = m_listHolders;
        int  cItems = list.size();

        if (cItems == 0)
            {
            return null;
            }

        Iterator<InetSocketAddress> iterAddr = m_iterAddr;
        int                         iLast    = m_iLast;
        boolean                     fSafe    = m_fSafe;
        AddressHolder               holder   = null;
        InetSocketAddress           address;

        do
            {
            while (iterAddr == null || !iterAddr.hasNext())
                {
                // select next configured address
                iLast = m_iLast = (iLast + 1) % cItems;

                holder = (AddressHolder) list.get(iLast);

                if (holder.isPending())
                    {
                    reset();
                    return null;
                    }

                holder.setPending(true);
                iterAddr = m_iterAddr = resolveAddress(holder.getHost(), holder.getPort());
                }

            address = iterAddr.next();

            // ensure the address can be resolved
            if (fSafe && address.isUnresolved())
                {
                if (holder != null && !holder.isReported())
                    {
                    holder.setReported(true);
                    Base.log("The ConfigurableAddressProvider is skipping the unresolvable address \"" + address
                                     + "\".");
                    }
                address = null;
                }
            }
        while (address == null);

        return address;
        }

    /**
    * {@inheritDoc}
    */
    public void accept()
        {
        reset(m_iLast);
        }

    /**
    * {@inheritDoc}
    */
    public void reject(Throwable eCause)
        {
        }

    // ----- Set interface --------------------------------------------------

    /**
     * Returns the number of elements in this collection.  If the collection
     * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of elements in this collection
     */
    public int size()
        {
        return m_listHolders.size();
        }

    /**
     * Returns an iterator over the elements contained in this collection.
     *
     * @return an iterator over the elements contained in this collection
     */
    public Iterator iterator()
        {
        return new Iterator<InetSocketAddress>()
            {
            public boolean hasNext()
                {
                return (m_iterAddr != null && m_iterAddr.hasNext()) || f_iterHolder.hasNext();
                }

            public InetSocketAddress next()
                {
                Iterator<InetSocketAddress> iterAddr = m_iterAddr;
                if (iterAddr != null && iterAddr.hasNext())
                    {
                    return iterAddr.next();
                    }

                AddressHolder holder = f_iterHolder.next();

                iterAddr = m_iterAddr = resolveAddress(holder.getHost(), holder.getPort());

                return iterAddr.next();
                }

            public void remove()
                {
                f_iterHolder.remove();
                m_iterAddr = null;
                }

            /**
             * Iterator of AddressHolder list.
             */
            private final Iterator<AddressHolder> f_iterHolder = m_listHolders.iterator();

            /**
             * Iterator of addresses for current holder.
             */
            private Iterator<InetSocketAddress> m_iterAddr;
            };
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Make all addresses iterable, starting at the first address.
     */
    protected void reset()
        {
        reset(-1);
        }

    /**
     * Make all addresses iterable, starting at the index after the specified
     * one.
     *
     * @param iLast  the index of the last address returned
     */
    protected synchronized void reset(int iLast)
        {
        // reset all holders
        List list = m_listHolders;

        for (int i = 0, c = list.size(); i < c; i++)
            {
            ((AddressHolder) list.get(i)).setPending(false);
            }

        m_iterAddr = null;
        m_iLast = iLast;
        }

    /**
     * Configure this ConfigurableAddressProvider based on the specified XML.
     *
     * @param xmlConfig  the XML element that contains the configuration info
     */
    @Deprecated
    protected void configure(XmlElement xmlConfig)
        {
        List list = new ArrayList();

        for (XmlElement xmlAddr : (List<XmlElement>) xmlConfig.getElementList())
            {
            String sAddr;
            int nPort;

            switch (xmlAddr.getName())
                {
                case "socket-address":
                    if (xmlConfig.getName().equalsIgnoreCase("well-known-addresses"))
                        {
                        Logger.warn("The use of <socket-address> for the <well-known-addresses> element is deprecated and the <port> value is ignored. Use <address> instead.");
                        }

                    sAddr = xmlAddr.getSafeElement("address").getString().trim();
                    nPort = xmlAddr.getSafeElement("port").getInt();
                    break;

                case "host-address":
                case "address":
                    sAddr = xmlAddr.getString().trim();
                    nPort = 0;
                    break;

                default:
                    continue;
                }

            String[] saAddresses = Arrays.stream(sAddr.split(","))
                    .map(String::trim)
                    .toArray(String[]::new);

            for (String sAddress : saAddresses)
                {
                if (sAddress.isEmpty())
                    {
                    // ignore empty elements
                    continue;
                    }
                m_fResolve |= InetAddressHelper.isHostName(sAddress);
                try
                    {
                    list.add(new AddressHolder(sAddress, nPort).validate());
                    }
                catch (RuntimeException e)
                    {
                    throw Base.ensureRuntimeException(e, "Invalid configuration element: " + xmlAddr);
                    }
                }
            }

        m_listHolders = sortHolders(list);
        }

    /**
     * Sort the holders in the order to be returned by the {@link
     * #getNextAddress()} method.  This implementation randomizes the holder
     * list for simple load balancing.
     *
     * @param list  the original list retrieved from the configuration
     *
     * @return the re-ordered list
     */
    protected List sortHolders(List list)
        {
        return Base.randomize(list);
        }

    // ----- Object methods -------------------------------------------------

    /**
     * AddressProvider instances should compare to be <tt>equals()</tt> iff
     * they should be expected to consistently produce the same resulting set
     * of addresses.
     * <p>
     * Note: the general contract of <tt>hashCode</tt> and <tt>equals()</tt>
     *       should be preserved; AddressProviders which compare
     *       <tt>equals()</tt> should have the same hashCode.
     *
     * @param o  the Object to compare this AddressProvider to for equality
     *
     * @return true iff this AddressProvider is equal to the specified object
     */
    public boolean equals(Object o)
        {
        if (!(o instanceof ConfigurableAddressProvider))
            {
            return false;
            }

        ConfigurableAddressProvider that = (ConfigurableAddressProvider) o;
        List listThis = this.m_listHolders;
        List listThat = that.m_listHolders;

        if (listThat.size() != listThis.size())
            {
            return false;
            }

        for (Iterator iter = listThis.iterator(); iter.hasNext(); )
            {
            if (!listThat.contains(iter.next()))
                {
                return false;
                }
            }

        return true;
        }

    /**
     * Return the hash code for this AddressProvider.
     *
     * @return the hash code for this AddressProvider
     */
    public int hashCode()
        {
        // the hash code must not take the ordering into account of TCMP passes the hash over the wire
        // to provide a warning if the WKA lists are not equal

        int nHash = 0;
        for (AddressHolder h : m_listHolders)
            {
            nHash += Objects.hash(h);
            }

        return nHash;
        }

    /**
     * Return a string representation of this ConfigurableAddressProvider.
     *
     * @return a string representation of the list of configured addresses
     */
    public synchronized String toString()
        {
        StringBuffer sb = new StringBuffer().append('[');

        for (Iterator iter = m_listHolders.iterator(); iter.hasNext(); )
            {
            AddressHolder holder = (AddressHolder) iter.next();
            sb.append(holder.getHost()).append(':').append(holder.getPort());

            if (iter.hasNext())
                {
                sb.append(',');
                }
            }

        sb.append(']');

        return sb.toString();
        }

    /**
     * {@inheritDoc}
     */
    public synchronized String[] getAddressDescriptions()
        {
        List holders = m_listHolders;
        String[] asAddr = new String[holders.size()];
        StringBuilder sb = new StringBuilder();
        int i = 0;

        for (Iterator iter = holders.iterator(); iter.hasNext(); )
            {
            AddressHolder holder = (AddressHolder) iter.next();

            sb.append(holder.getHost()).append(':').append(holder.getPort());
            asAddr[i++] = sb.toString();
            sb.setLength(0);
            }

        return asAddr;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Resolve an address and port.
     *
     * @param sHost  the host
     * @param nPort  the port
     *
     * @return the InetSocketAddress
     */
    protected synchronized Iterator<InetSocketAddress> resolveAddress(final String sHost, final int nPort)
        {
        try
            {
            return new Iterator<InetSocketAddress>()
            {
            @Override
            public boolean hasNext()
                {
                return m_iAddr < f_aAddr.length;
                }

            @Override
            public InetSocketAddress next()
                {
                if (hasNext())
                    {
                    return new InetSocketAddress(f_aAddr[m_iAddr++], nPort);
                    }
                throw new NoSuchElementException();
                }

            @Override
            public void remove()
                {
                throw new UnsupportedOperationException();
                }

            final InetAddress[] f_aAddr = "localhost".equals(sHost)
                    ? new InetAddress[]{InetAddressHelper.getLocalAddress(sHost)}
                    : (InetAddress[]) Base.randomize(InetAddress.getAllByName(sHost));
                int m_iAddr;
                };
            }
        catch (UnknownHostException e)
            {
            // return unresolved address
            return Collections.singleton(new InetSocketAddress(sHost, nPort)).iterator();
            }
        }

    // ----- inner classes --------------------------------------------------

    /**
     * A stateful holder for an obtaining an InetSocketAddress object.
     */
    public static class AddressHolder
        {
        /**
         * Construct an AddressHolder for the specified host and port.
         *
         * @param sHost  the hostname
         * @param nPort  the port number
         */
        public AddressHolder(String sHost, int nPort)
            {
            m_sHost = sHost;
            m_nPort = nPort;
            }

        /**
         * Throw IllegalArgumentException if any values are invalid.
         *
         * @return this
         */
        public AddressHolder validate()
            {
            if (m_sHost == null)
                {
                throw new IllegalArgumentException("host may not be null");
                }

            if (m_nPort < 0 || m_nPort > 0xFFFF)
                {
                throw new IllegalArgumentException("port " + m_nPort + " out of range of 0 to " + 0xFFFF);
                }
            return this;
            }

        // ----- accessors ------------------------------------------------

        /**
         * Check whether or not the underlying address has been accepted.
         *
         * @return true iff the underlying address has not yet been accepted
         */
        protected boolean isPending()
            {
            return m_fPending;
            }

        /**
         * Set or clear the "pending" flag.
         *
         * @param fPending  the flag value
         */
        protected void setPending(boolean fPending)
            {
            m_fPending = fPending;
            }

        /**
         * Check whether or not the underlying address has been reported
         * as unresolvable.
         *
         * @return true iff the underlying address has been reported as
         *         unresolvable
         */
        protected boolean isReported()
            {
            return m_fReported;
            }

        /**
         * Set of clear the "reported" flag.
         *
         * @param fReported  the flag value
         */
        protected void setReported(boolean fReported)
            {
            m_fReported = fReported;
            }

        /**
         * Return the host name.
         *
         * @return the host name
         */
        protected String getHost()
            {
            return m_sHost;
            }

        /**
         * Return the port number.
         *
         * @return the port number
         */
        protected int getPort()
            {
            return m_nPort;
            }

        // ----- Object methods -------------------------------------------

        /**
         * Return true iff this ProvidedAddress is equal to the specified
         * Object.  AddressHolders are considered equal if they represent the
         * same address.
         *
         * @param o  the object to compare to this ProvidedAddress for equality
         *
         * @return true iff this AddressHolders is equal to the specified
         *         object
         */
        public boolean equals(Object o)
            {
            if (o instanceof AddressHolder)
                {
                AddressHolder that = (AddressHolder) o;

                return m_nPort == that.m_nPort && Base.equals(m_sHost, that.m_sHost);
                }

            return false;
            }

        /**
         * Return the hash code for this ProvidedAddress.
         *
         * @return the hash code for this ProvidedAddress
         */
        public int hashCode()
            {
            return Base.hashCode(m_sHost) ^ m_nPort;
            }

        // ----- data fields ----------------------------------------------

        /**
         * The configured address, either hostname or IP address.
         */
        protected String m_sHost;

        /**
         * The configured port.
         */
        protected int m_nPort;

        /**
         * A flag indicating that the underlying address has been provided
         * to a client, but has not yet been accepted.
         */
        private boolean m_fPending;

        /**
         * Specifies if this address has already been reported as unresolved.
         */
        private boolean m_fReported;
        }

    // ----- data fields ----------------------------------------------------

    /**
     * A read-only list of ProvidedAddress objects.
     */
    protected List<AddressHolder> m_listHolders;

    /**
     * An address iterator for the previously resolved address.
     */
    protected Iterator<InetSocketAddress> m_iterAddr;

    /**
     * Index of the last returned address.
     */
    protected int m_iLast = -1;

    /**
     * Specifies if the provider is only to return resolved addresses.
     */
    protected boolean m_fSafe;

    /**
     * Specifies if the list of address need DNS resolution.
     */
    @Deprecated
    public boolean m_fResolve;
    }
