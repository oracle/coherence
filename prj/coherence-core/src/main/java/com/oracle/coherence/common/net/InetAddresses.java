/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net;


import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Predicate;
import com.oracle.coherence.common.base.Timeout;
import com.oracle.coherence.common.collections.ConcurrentHashMap;
import com.oracle.coherence.common.internal.net.MultiplexedSocketProvider;
import com.oracle.coherence.common.util.Duration;
import com.oracle.coherence.common.util.SafeClock;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;


/**
* Helper class that encapsulates common InetAddress functionality.
*/
public abstract class InetAddresses
    {
    /**
    * Obtain the "best" local host address which matches the supplied predicate.
    *
    * @param predicate  the predicate to match
    *
    * @return the InetAddress
    *
    * @throws UnknownHostException if no match can be found
    */
    public static InetAddress getLocalAddress(Predicate<InetAddress> predicate)
            throws UnknownHostException
        {
        InetAddress addrLocal = InetAddress.getLocalHost();
        InetAddress addrBest  = null;
        int         nMTUBest  = 0;
        int         nMTULocal = 0;
        Map         mapAddr   = getAllLocalMTUs();

        for (Iterator iter = mapAddr.entrySet().iterator(); iter.hasNext();)
            {
            Map.Entry   entry = (Map.Entry) iter.next();
            InetAddress addr  = (InetAddress) entry.getKey();

            if (!predicate.evaluate(addr))
                {
                continue;
                }

            // addr is acceptable, prefer a higher MTU. In the case of equal
            // MTUs we compare addresses for order to ensure that we will
            // return a consistent result between invocations.
            Integer oMTU = (Integer) entry.getValue();
            int     nMTU = oMTU == null ? 0 : oMTU.intValue();

            if (addr.equals(addrLocal))
                {
                nMTULocal = nMTU;
                }

            if (nMTU > nMTUBest ||
               (nMTU == nMTUBest && compare(addr, addrBest) < 0))
                {
                addrBest = addr;
                nMTUBest = nMTU;
                }
            }

        if (addrBest == null)
            {
            throw new UnknownHostException("No local address matching " + predicate);
            }

        // prefer the OS defined localhost assuming all else is equal
        return nMTUBest == nMTULocal && predicate.evaluate(addrLocal)
                ? addrLocal : addrBest;
        }

    /**
     * Return the local InetAddress represented by the specified string.
     * <p>
     * Note: if an explicit address is supplied and it is non-local then it will be assumed to
     * be an external NAT address.
     * </p>
     *
     * @param sAddr  the string address, either hostname, literal ip, or subnet and mask
     *
     * @return the InetAddress
     *
     * @throws UnknownHostException  if the string cannot be resolved
     */
    public static InetAddress getLocalAddress(String sAddr)
            throws UnknownHostException
        {
        if (sAddr == null || sAddr.isEmpty() || sAddr.equals("localhost"))
            {
            return getLocalHost();
            }
        else if (sAddr.equals("0.0.0.0") || sAddr.equals("::0") || sAddr.equals("::")) // wildcard
            {
            return ADDR_ANY;
            }
        else if (sAddr.indexOf('/') != -1) // CIDR
            {
            return getLocalAddress(new IsSubnetMask(sAddr));
            }
        else
            {
            return InetAddress.getByName(sAddr);
            }
        }

    /**
    * Obtain the local host address. If at all possible, ensure that the
    * returned address is not a loopback, wildcard or a link local address.
    *
    * @return the InetAddress that is the best fit for the local host address
    *
    * @throws UnknownHostException if no match can be found
    *
    * @see <a href="http://developer.java.sun.com/developer/bugParade/bugs/4665037.html">
    *      Sun's Bug Parade</a>
    */
    public static InetAddress getLocalHost()
            throws UnknownHostException
        {
        InetAddress addrLocal = s_addrLocalhost;
        if (addrLocal != null &&
            isLocalAddress(addrLocal)) // avoid returning dropped DHCP
            {
            return addrLocal;
            }

        boolean          fNonRoutable = false;
        Set<InetAddress> setExclude   = null;
        for (;;)
            {
            try
                {
                final boolean          fNonRoutablePass = fNonRoutable;
                final Set<InetAddress> setExcludePass   = setExclude;
                InetAddress addr = getLocalAddress(new Predicate<InetAddress>()
                    {
                    @Override
                    public boolean evaluate(InetAddress addr)
                        {
                        return (fNonRoutablePass || IsRoutable.INSTANCE.evaluate(addr)) && // avoids loopback initially
                               (setExcludePass == null || !setExcludePass.contains(addr)); // avoid previous rejections
                        }
                    });

                if (!isLocalReachableAddress(addr, 300))  // avoids temporary IPv6 addresses, and VPN blocked addresses
                    {
                    if (setExclude == null)
                        {
                        setExclude = new HashSet<>();
                        }
                    setExclude.add(addr);
                    continue;
                    }

                return s_addrLocalhost = addr; // cache and return
                }
            catch (UnknownHostException e)
                {
                if (fNonRoutable)
                    {
                    return InetAddress.getLocalHost();
                    }
                fNonRoutable = true; // include non-routable on next pass, i.e. allow loopback
                }
            }
        }

    /**
     * The IsRoutable predicate evaluates to true for any InetAddress which is
     * externally routable.
     */
    public static class IsRoutable
            implements Predicate<InetAddress>
        {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean evaluate(InetAddress addr)
            {
            return !addr.isLoopbackAddress() &&
                   !addr.isAnyLocalAddress() &&
                   !addr.isLinkLocalAddress();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
            {
            return "is routable";
            }

        /**
         * Singleton instance.
         */
        public static final IsRoutable INSTANCE = new IsRoutable();
        }

    /**
     * IsSubnetMask predicate evaluates to true for any address with matches the
     * pattern for the masked bits
     */
    public static class IsSubnetMask
            implements Predicate<InetAddress>
        {
        /**
         * Construct a predicate for the given pattern and mask.
         *
         * @param addrPattern  the pattern to match
         * @param addrMask     the mask identifying the portion of the pattern to match
         */
        public IsSubnetMask(InetAddress addrPattern, InetAddress addrMask)
            {
            m_abPattern    = addrPattern.getAddress();
            m_abMask       = addrMask.getAddress();
            m_sDescription = addrPattern + "/" + addrMask;

            if (m_abPattern.length != m_abMask.length)
                {
                throw new IllegalArgumentException(
                        "pattern and mask must be of the same byte length");
                }
            }

        /**
         * Construct a predicate for the given pattern and mask bit count.
         *
         * @param addrPattern  the pattern to match
         * @param cMaskBits    the number of mask bits
         */
        public IsSubnetMask(InetAddress addrPattern, int cMaskBits)
            {
            m_abPattern    = addrPattern.getAddress();
            m_abMask       = new byte[m_abPattern.length];
            m_sDescription = addrPattern + "/" + cMaskBits;

            setSubnetMask(m_abMask, cMaskBits);
            }

        /**
         * Construct a predicate for the given pattern and slash mask.
         *
         * @see <a href="http://en.wikipedia.org/wiki/CIDR_notation">
         *      CIDR Notation</a>
         *
         * @param sAddr  the pattern and mask
         */
        public IsSubnetMask(String sAddr)
            {
            try
                {
                m_sDescription = sAddr;

                int         ofSubnetMask = sAddr.indexOf('/');
                InetAddress addr = ofSubnetMask == -1
                        ? InetAddress.getByName(sAddr)
                        : InetAddress.getByName(sAddr.substring(0, ofSubnetMask));

                byte[] abPattern = m_abPattern = addr.getAddress();

                if (ofSubnetMask == -1 || sAddr.indexOf('.', ofSubnetMask) == -1)
                    {
                    byte[] abMask = m_abMask = new byte[abPattern.length];

                    setSubnetMask(abMask, ofSubnetMask == -1
                            ? abPattern.length * 8 // no slash == full mask
                            : Integer.valueOf(sAddr.substring(ofSubnetMask + 1)));
                    }
                else
                    {
                    m_abMask = InetAddress.getByName(sAddr.substring
                            (ofSubnetMask + 1)).getAddress();
                    }
                }
            catch (UnknownHostException e)
                {
                throw new IllegalArgumentException("dns names are not supported");
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean evaluate(InetAddress addr)
            {
            byte[] ab     = addr.getAddress();
            byte[] abPat  = m_abPattern;
            byte[] abMask = m_abMask;

            if (ab.length != abPat.length)
                {
                return false;
                }
            for (int i = ab.length - 1; i >= 0; --i)
                {
                byte bMask = abMask[i];
                if ((ab[i] & bMask) != (abPat[i] & bMask))
                    {
                    return false;
                    }
                }

            return true;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
            {
            return "IsSubnetMask(" + m_sDescription + ")";
            }

        protected String m_sDescription;
        protected byte[] m_abPattern;
        protected byte[] m_abMask;
        }

    /**
    * Compare two InetAddresses for ordering purposes.
    *
    * @param addrA  the first address to compare
    * @param addrB  the second address to compare
    *
    * @return a negative integer, zero, or a positive integer as the first
    *         argument is less than, equal to, or greater than the second
    */
    public static int compare(InetAddress addrA, InetAddress addrB)
        {
        return InetAddressComparator.INSTANCE.compare(addrA, addrB);
        }

    /**
    * Return the MTU for the specified local address.
    *
    * @param addr  the local address
    *
    * @return  the MTU of the specified address, or 0 if the MTU can not be
    *          identified
    */
    public static int getLocalMTU(InetAddress addr)
        {
        try
            {
            NetworkInterface ni = NetworkInterface.getByInetAddress(addr);
            if (ni == null)
                {
                throw new IllegalArgumentException("The specified address \"" +
                        addr + "\" is not a local address.");
                }
            return getLocalMTU(ni);
            }
        catch (SocketException e) {}

        return 0;
        }

    /**
    * Return the MTU for the specified NetworkInterface.
    *
    * @param ni  the network interface
    *
    * @return the MTU of the specified address, or 0 if the MTU can not be
    *         identified
    */
    public static int getLocalMTU(NetworkInterface ni)
        {
        try
            {
            int nMTU = ni.getMTU();
            // Windows 7 + JRE 1.6 reports -1 for loopback, apparently
            // converting a unsigned 32b int max value into a signed 32b int
            return nMTU < 0 ? Integer.MAX_VALUE : nMTU;
            }
        catch (Exception e) {}

        return 0;
        }

    /**
     * Return this machines MTU.
     *
     * @return the machine's MTU
     */
    public static int getLocalMTU()
        {
        // we couldn't find it, either because of the above described error, or because the socket is bound to a
        // NAT address; just return the box's minimum MTU

        int nMtu = 65535; // start with largest allowed by IP
        try
            {
            for (Enumeration enmrNI = NetworkInterface.getNetworkInterfaces();
                 enmrNI != null && enmrNI.hasMoreElements();)
                {
                int nMtuNic = InetAddresses.getLocalMTU((NetworkInterface) enmrNI.nextElement());
                if (nMtuNic > 0)
                    {
                    nMtu = Math.min(nMtu, nMtuNic);
                    }
                }
            }
        catch (SocketException e) {}

        return nMtu;
        }

    /**
     * Return a local address which is in the same subnet as the specified address.
     *
     * @param addr  the address to find a local peer of
     *
     * @return a local address in the same subnet as the specified address or <tt>null</tt> if none is found
     */
    public static InetAddress getLocalPeer(InetAddress addr)
        {
        for (InetAddress addrLocal : getAllLocalAddresses())
            {
            if (isInSubnet(addr, /*addrSubnet*/ addrLocal, getLocalSubnetLength(addrLocal)))
                {
                return addrLocal;
                }
            }

        return null;
        }

    /**
     * Return true if the specified address is part of the specified subnet
     *
     * @param addr        the address to test
     * @param addrSubnet  the subnet
     * @param cBitSubnet  the number of valid bits in the subnet address
     *
     * @return return true if the specified address is in the specified subnet
     */
    public static boolean isInSubnet(InetAddress addr, InetAddress addrSubnet, int cBitSubnet)
        {
        byte[] ab    = addr.getAddress();
        byte[] abPat = addrSubnet.getAddress();

        if (ab.length != abPat.length)
            {
            return false;
            }

        for (int i = 0; i < ab.length && cBitSubnet > 0; ++i)
            {
            byte bMask;
            if (cBitSubnet < 8)
                {
                bMask = 0;
                for (int j = 0; j < cBitSubnet; ++j)
                    {
                    bMask |= (1 << cBitSubnet - j);
                    }
                }
            else
                {
                bMask = (byte) 0x0FF;
                }

            if ((ab[i] & bMask) != (abPat[i] & bMask))
                {
                return false;
                }

            cBitSubnet -= 8;
            }

        return true;
        }

    /**
     * Return the InetAddress representing the subnet for the specified local address.
     *
     * @param addr  the local address
     *
     * @return the subnet address
     */
    public static InetAddress getLocalSubnetAddress(InetAddress addr)
        {
        int cBits = getLocalSubnetLength(addr);
        byte[] abAddr = addr.getAddress();

        for (int i = 0, c = abAddr.length; i < c; ++i)
            {
            if (cBits == 0)
                {
                // zero out entire byte
                abAddr[i] = 0;
                }
            else if (cBits < 8)
                {
                // partial byte is part of subnet, zero out remainder
                byte cZero = (byte) (8 - cBits);
                abAddr[i]  = (byte) ((abAddr[i] >> cZero) << cZero);
                cBits      = 0;
                }
            else
                {
                // full byte is part of subnet
                cBits -= 8;
                }
            }

        try
            {
            return InetAddress.getByAddress(abAddr);
            }
        catch (UnknownHostException e)
            {
            throw new IllegalStateException(e);
            }
        }

    /**
     * Return the bit length for the subnet of the specified local address.
     *
     * @param addr  the local address
     *
     * @return the subnet address
     */
    public static short getLocalSubnetLength(InetAddress addr)
        {
        try
            {
            NetworkInterface ni = NetworkInterface.getByInetAddress(addr);
            if (ni == null)
                {
                throw new IllegalArgumentException("The specified address \"" +
                                                           addr + "\" is not a local address.");
                }

            for (InterfaceAddress addrIf : ni.getInterfaceAddresses())
                {
                if (addrIf.getAddress().equals(addr))
                    {
                    short cBits = addrIf.getNetworkPrefixLength();

                    // for some reason 0 is sometimes returned for ipv4s, there is no correct
                    // answer here, but anything is better then 0, note in such cases the
                    // broadcast address is also null so we can't even try to derive it
                    return cBits == 0 ? 8 : cBits;
                    }
                }
            }
        catch (IOException e)
            {
            throw new IllegalStateException(e);
            }

        throw new IllegalStateException();
        }

    /**
    * Return a list of all InetAddress objects bound to all the network
    * interfaces on this machine.
    *
    * @return a list of InetAddress objects
    */
    public static List<InetAddress> getAllLocalAddresses()
        {
        return getLocalAddresses(o -> true);
        }

    /**
     * Return a list of InetAddresses bound to all the network
     * interfaces on this machine matching the specified predicate.
     *
     * @param predicate the predicate to match
     *
     * @return a list of InetAddress objects
     */
    public static List<InetAddress> getLocalAddresses(Predicate<InetAddress> predicate)
        {
        List<InetAddress> listAddr = new ArrayList<>();

        try
            {
            for (Enumeration enmrNI = NetworkInterface.getNetworkInterfaces();
                 enmrNI != null && enmrNI.hasMoreElements();)
                {
                NetworkInterface ni = (NetworkInterface) enmrNI.nextElement();
                for (Enumeration<InetAddress> enmrAddr = ni.getInetAddresses();
                     enmrAddr.hasMoreElements();)
                    {
                    InetAddress addr = enmrAddr.nextElement();
                    if (predicate.evaluate(addr))
                        {
                        listAddr.add(addr);
                        }
                    }
                }
            }
        catch (SocketException e) {}

        return listAddr;
        }

    /**
     * Returns a collection of all local bindable addresses.
     *
     * Note: this collection will also include NAT address which have been previously
     *       {@link #isNatLocalAddress(InetAddress, int, int, int) identified}. I.e. external addresses which can be
     *        bound to via {@link TcpSocketProvider#MULTIPLEXED multiplexed socket provider}.
     *
     * @return a list of all local bindable addresses
     *
     * @since 12.2.1
     */
    public static Collection<InetAddress> getLocalBindableAddresses()
        {
        if (!s_fDequeAddrBindablePopulated)
            {
            synchronized (s_dequeAddrBindable)
                {
                if (!s_fDequeAddrBindablePopulated)
                    {
                    s_dequeAddrBindable.addAll(getLocalAddresses(
                        new Predicate<InetAddress>()
                                {
                                public boolean evaluate(InetAddress addr)
                                    {
                                    if (addr.isAnyLocalAddress())
                                        {
                                        return false;
                                        }
                                    else if (addr instanceof Inet4Address)
                                        {
                                        return true;
                                        }
                                    else
                                        {
                                        // only return Inet6Addresses which are bindable
                                        try (ServerSocket socket = new ServerSocket(0, 0, addr))
                                            {
                                            return true;
                                            }
                                        catch (IOException e)
                                            {
                                            }
                                        return false;
                                        }
                                    }
                                }));
                    s_fDequeAddrBindablePopulated = true;
                    }
                }
            }

        return Collections.unmodifiableCollection(s_dequeAddrBindable);
        }

    /**
    * Return a map of all InetAddress and MTUs bound to all the network
    * interfaces on this machine.  If the MTU cannot be obtained a null
    * value will be used.
    *
    * @return a map of Integer MTU values keyed by the corresponding InetAddress
    */
    public static Map getAllLocalMTUs()
        {
        Map mapAddr = new HashMap();

        try
            {
            for (Enumeration enmrNI = NetworkInterface.getNetworkInterfaces();
                 enmrNI != null && enmrNI.hasMoreElements();)
                {
                NetworkInterface ni   = (NetworkInterface) enmrNI.nextElement();
                int              nMTU = getLocalMTU(ni);
                Object           oMTU = nMTU == 0 ? null : Integer.valueOf(nMTU);
                for (Enumeration enmrAddr = ni.getInetAddresses();
                     enmrAddr.hasMoreElements();)
                    {
                    mapAddr.put(enmrAddr.nextElement(), oMTU);
                    }
                }
            }
        catch (SocketException e) {}

        return mapAddr;
        }

    /**
    * Clean an IPv6 address by removing the brackets.
    *
    * @param sAddr  an address
    *
    * @return an IPv6 address without brackets or original string if not IPv6
    *         literal
    */
    private static String unbracketAddressString(String sAddr)
        {
        if (sAddr.charAt(0) == '[')
            {
            int ofBracket = sAddr.indexOf(']', 1);
            if (ofBracket < 0)
                {
                throw new IllegalArgumentException("invalid IPv6 address");
                }
            sAddr = sAddr.substring(1, ofBracket);
            }
        return sAddr;
        }


    /**
     * Determine if a host string is a hostname.
     *
     * @param sHost the host string
     *
     * @return true if host string is a hostname
     */
    public static boolean isHostName(String sHost)
        {
        return sHost.length() != 0 && sHost.indexOf(":") < 0
                && !sHost.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$");
        }

    /**
     * Return true if the specified address string represents the wildcard address
     *
     * @param sAddr  the address to test
     *
     * @return true if the specified address is the wildcard address
     */
    public static boolean isAnyLocalAddress(String sAddr)
        {
        return sAddr != null && (sAddr.equals("::0") || sAddr.equals("0.0.0.0"));
        }

    /**
     * Return true if the supplied port is believed to be in the ephemeral port range.
     *
     * Note that while a port of 0 can used to request an ephemeral port it is not itself
     * considered to be ephemeral.
     *
     * @param nPort  the port to query
     *
     * @return true if the port is believed to be ephemeral
     */
    public static boolean isEphemeral(int nPort)
        {
        if (nPort < 0 || nPort > 65535) // decode multiplexed ports
            {
            nPort = MultiplexedSocketProvider.getBasePort(nPort);
            }

        int    nLow;
        int    nHigh;
        String sOS = System.getProperty("os.name").toLowerCase().trim();

        if ("linux".equals(sOS))
            {
            // we have custom linux code because it doesn't use the IANA suggested range and at least RedHat uses a
            // stupid lower bound of 1024 so we're likely to run into random bind failures on RedHat and presumably its
            // derivatives.

            // read /proc to get actual range and reservations, these aren't actual files on disk but rather virtual
            // files served up by the kernel and access should be immediate so we don't try to cache the results
            // see https://www.kernel.org/doc/Documentation/networking/ip-sysctl.txt or details on the formats of these
            // files note despite the names these files are for both ipv4 and ipv6 as well as both UDP and TCP

            // try to check if it reserved, if so then it can't be ephemeral
            try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/sys/net/ipv4/ip_local_reserved_ports"))))
                {
                StringTokenizer sTok = new StringTokenizer(in.readLine(), ",");
                while (sTok.hasMoreElements())
                    {
                    String sReserved = sTok.nextToken().trim();
                    int    ofRange   = sReserved.indexOf('-');
                    if (ofRange == -1)
                        {
                        if (Integer.parseInt(sReserved) == nPort)
                            {
                            return false;
                            }
                        }
                    else
                        {
                        int nLowRes  = Integer.parseInt(sReserved.substring(0, ofRange).trim());
                        int nHighRes = Integer.parseInt(sReserved.substring(ofRange + 1).trim());

                        if (nPort >= nLowRes && nPort <= nHighRes)
                            {
                            return false;
                            }
                        }
                    }
                }
            catch (Throwable t) {}

            // try to read configured ephemeral range
            try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("/proc/sys/net/ipv4/ip_local_port_range"))))
                {
                StringTokenizer sTok = new StringTokenizer(in.readLine());
                nLow  = Integer.parseInt(sTok.nextToken().trim());
                nHigh = Integer.parseInt(sTok.nextToken().trim());
                }
            catch (Throwable t)
                {
                // use standard Linux defaults
                nLow  = 32768;
                nHigh = 61000;
                }
            }
        else // assume IANA suggested defaults which is quite typical other then for Linux
            {
            nLow  = 49152;
            nHigh = 65535;
            }

        return nPort >= nLow && nPort <= nHigh;
        }

    /**
     * Return the InetSocketAddress represented by the specified string.
     *
     * @param sAddr  a legal address string
     * @param nPort  a default port to use if sAddr does not contain one
     *
     * @return a non-null InetSocketAddress object
     *
     * @throws UnknownHostException if no match can be found
    */
    public static InetSocketAddress getSocketAddress(String sAddr, int nPort)
        throws UnknownHostException
        {
        if (sAddr == null)
            {
            throw new IllegalArgumentException("address cannot be null");
            }

        String sHost = unbracketAddressString(sAddr);
        int    ofPort;
        if (sAddr.equals(sHost))
            {
            // ipv4 address
            // still need to parse for both host and port
            ofPort = sAddr.lastIndexOf(':');
            int ofEnd = ofPort != -1 ? ofPort : sAddr.length();
            sHost = sAddr.substring(0, ofEnd);
            }
        else
            {
            // ipv6 address
            // we already have the host, just grab the port if it exists
            ofPort = sAddr.indexOf(':', sAddr.indexOf(']'));
            }

        if (ofPort != -1)
            {
            // pull port off of address
            nPort = Integer.parseInt(sAddr.substring(ofPort + 1));
            }
        if (sHost.equals("*"))
            {
            return new InetSocketAddress(nPort);
            }
        if (sHost.equals("localhost") || sAddr.length() == 0)
            {
            return new InetSocketAddress(InetAddresses.getLocalHost(), nPort);
            }

        InetSocketAddress addr = new InetSocketAddress(sHost, nPort);
        if (addr.getAddress() == null)
            {
            throw new UnknownHostException("could not resolve address \"" + sAddr + "\"");
            }
        return addr;
        }

    /**
     * Obtain the {@link InetAddress} from a given {@link SocketAddress}.
     * <p>
     * Throws an {@link IllegalArgumentException} if sockAddr is not a
     * {@link InetSocketAddress} or {@link InetSocketAddress32}.
     *
     * @param sockAddr  the {@link SocketAddress}
     *
     * @return the {@link InetAddress}
     *
     * @since 12.2.1
     */
    public static InetAddress getAddress(SocketAddress sockAddr)
        {
        if (sockAddr instanceof InetSocketAddress)
            {
            return ((InetSocketAddress) sockAddr).getAddress();
            }
        if (sockAddr instanceof InetSocketAddress32)
            {
            return ((InetSocketAddress32) sockAddr).getAddress();
            }
        throw new IllegalArgumentException("Cannot obtain an address from class " + sockAddr.getClass());
        }

    /**
     * Obtain the port from a given {@link SocketAddress}.
     * <p>
     * Throws an {@link IllegalArgumentException} if sockAddr is not a
     * {@link InetSocketAddress} or {@link InetSocketAddress32}.
     *
     * @param sockAddr  the {@link SocketAddress}
     *
     * @return the port
     *
     * @since 12.2.1
     */
    public static int getPort(SocketAddress sockAddr)
        {
        if (sockAddr instanceof InetSocketAddress)
            {
            return ((InetSocketAddress) sockAddr).getPort();
            }
        if (sockAddr instanceof InetSocketAddress32)
            {
            return ((InetSocketAddress32) sockAddr).getPort();
            }
        throw new IllegalArgumentException("Cannot obtain a port from class " + sockAddr.getClass());
        }

    /**
     * Return a new SocketAddress of the same type but with the specified address
     * <p>
     * Throws an {@link IllegalArgumentException} if sockAddr is not a
     * {@link InetSocketAddress} or {@link InetSocketAddress32}.
     *
     * @param sockAddr  the {@link SocketAddress}
     * @param addr      the new address
     *
     * @return the new SocketAddress
     *
     * @since 12.2.3
     */
    public static SocketAddress setAddress(SocketAddress sockAddr, InetAddress addr)
        {
        if (sockAddr instanceof InetSocketAddress)
            {
            return new InetSocketAddress(addr, ((InetSocketAddress) sockAddr).getPort());
            }
        if (sockAddr instanceof InetSocketAddress32)
            {
            return new InetSocketAddress32(addr, ((InetSocketAddress32) sockAddr).getPort());
            }
        throw new IllegalArgumentException("Cannot set address for class " + sockAddr.getClass());
        }


    /**
     * Return a new SocketAddress of the same type but with the specified port.
     * <p>
     * Throws an {@link IllegalArgumentException} if sockAddr is not a
     * {@link InetSocketAddress} or {@link InetSocketAddress32}.
     *
     * @param sockAddr  the {@link SocketAddress}
     * @param nPort     the new port
     *
     * @return the new SocketAddress
     *
     * @since 12.2.3
     */
    public static SocketAddress setPort(SocketAddress sockAddr, int nPort)
        {
        if (sockAddr instanceof InetSocketAddress)
            {
            return new InetSocketAddress(((InetSocketAddress) sockAddr).getAddress(), nPort);
            }
        if (sockAddr instanceof InetSocketAddress32)
            {
            return new InetSocketAddress32(((InetSocketAddress32) sockAddr).getAddress(), nPort);
            }
        throw new IllegalArgumentException("Cannot set port for class " + sockAddr.getClass());
        }

    /**
     * Return true if the specified address is a local address.
     *
     * @param addr  the address to check
     *
     * @return true if the address is a local address
     */
    public static boolean isLocalAddress(InetAddress addr)
        {
        try
            {
            return addr.isLoopbackAddress() || addr.isAnyLocalAddress() || checkLocalAddress(addr);
            }
        catch (SocketException e)
            {
            return false;
            }
        }

    /**
     * Return true iff the specified address is local and bindable.
     *
     * @param addr  the address to test
     *
     * @return true iff the specified address is local and bindable
     */
    public static boolean isLocalBindableAddress(InetAddress addr)
        {
        try (ServerSocket socket = new ServerSocket(0, 0, addr))
            {
            return true;
            }
        catch (IOException e)
            {
            return false;
            }
        }

    /**
     * Return true iff the specified address is local and reachable.
     *
     * @param addr     the address to test
     * @param cMillis  the timeout value in milliseconds
     *
     * @return true iff the specified address is local and reachable
     */
    public static boolean isLocalReachableAddress(InetAddress addr, int cMillis)
        {
        try (ServerSocket socket = new ServerSocket(0, 0, addr))
            {
            try (Socket client = new Socket())
                {
                Blocking.connect(client, socket.getLocalSocketAddress(), cMillis);
                return true;
                }
            }
        catch (IOException e)
            {
            return false;
            }
        }

    /**
     * Return true if any {@link #isNatLocalAddress(InetAddress, int) local NAT} addresses have been identified.
     *
     * @return true if any local NAT addresses have been identified
     */
    public static boolean hasNatLocalAddress()
        {
        return s_refSetNAT.get() != null;
        }

    /**
     * Return true iff the specified non-local address is NAT'd to a local address.
     *
     * @param addr   the socket address to test
     *
     * @return true iff the specified non-local address routes to a local address
     */
    public static boolean isNatLocalAddress(SocketAddress addr)
        {
        return isNatLocalAddress(getAddress(addr), getPort(addr));
        }

    /**
     * Return true iff the specified non-local address is NAT'd to a local address.
     *
     * @param addr   the address to test
     * @param nPort  the port to test
     *
     * @return true iff the specified non-local address routes to a local address
     */
    public static boolean isNatLocalAddress(InetAddress addr, int nPort)
        {
        return isNatLocalAddress(addr, nPort, nPort);
        }

    /**
     * Return true iff the specified non-local address is NAT'd to a local address.
     *
     * This method takes a range of ports to test through, though once there is a single positive match
     * no further testing will be preformed.  Specification of a port range is only necessary if a firewall
     * may block the NAT'd traffic, or if NAT'ing is only available on specific ports.
     *
     * @param addr      the address to test
     * @param nPortMin  the lower bound on the range of ports to test
     * @param nPortMax  the upper bound on the range of ports to test
     *
     * @return true iff the specified non-local address routes to a local address
     */
    public static boolean isNatLocalAddress(InetAddress addr, int nPortMin, int nPortMax)
        {
        return isNatLocalAddress(addr, nPortMin, nPortMax, (int) NAT_CHECK_TIMEOUT);
        }

    /**
     * Return true iff the specified non-local address is NAT'd to a local address.
     *
     * This method takes a range of ports to test through, though once there is a single positive match
     * no further testing will be preformed.  Specification of a port range is only necessary if a firewall
     * may block the NAT'd traffic, or if NAT'ing is only available on specific ports.
     *
     * @param addr      the address to test
     * @param nPortMin  the lower bound on the range of ports to test
     * @param nPortMax  the upper bound on the range of ports to test
     * @param cMillis   the timeout value in milliseconds
     *
     * @return true iff the specified non-local address routes to a local address
     */
    public static boolean isNatLocalAddress(InetAddress addr, int nPortMin, int nPortMax, int cMillis)
        {
        if (cMillis <= 0L || isLocalAddress(addr))
            {
            return false;
            }

        Set<InetSocketAddress> setNAT = s_refSetNAT.get();
        if (setNAT == null)
            {
            setNAT = Collections.newSetFromMap(new ConcurrentHashMap<>());
            if (!s_refSetNAT.compareAndSet(null, setNAT))
                {
                setNAT = s_refSetNAT.get();
                }
            }

        // It's possible we've already bound to this port and are retesting because of some other multiplexed
        // socket.  Here we use multiplexed sockets with ephemeral sub-ports, this ensures we aren't blocked
        // by and that we don't block other concurrent users of the same port.  Also we allow the specified
        // ports to be multiplexed, we just strip of the sub port since ultimately we only need to check if the
        // base is NAT'd.
        nPortMin = MultiplexedSocketProvider.getBasePort(nPortMin);
        nPortMax = Math.max(nPortMin, MultiplexedSocketProvider.getBasePort(nPortMax));

        InetSocketAddress addrSockBase = new InetSocketAddress(addr, nPortMin);
        if (setNAT.contains(addrSockBase) || // test if someone has already verified this range (from the base)
            setNAT.contains(new InetSocketAddress(addr, 0))) // or has verified that all ports appear to be mapped
            {
            return true;
            }

        try (Timeout t = Timeout.after(cMillis))
            {
            try (ServerSocket server = TcpSocketProvider.MULTIPLEXED.openServerSocket())
                {
                for (int nPort = nPortMin; nPort <= nPortMax && !server.isBound(); ++nPort)
                    {
                    try
                        {
                        // bind to ephemeral address, and specified port
                        server.bind(new InetSocketAddress32(MultiplexedSocketProvider.getPort(nPort, 0)));
                        }
                    catch (IOException e)
                        {
                        if (nPort == nPortMax)
                            {
                            // we couldn't find any free ports on which to conduct the test
                            return false;
                            }
                        // else; try next port
                        }
                    }

                // test if we can connect to the NAT address and port and receive the connection on our binding
                try (Socket clientOut = TcpSocketProvider.MULTIPLEXED.openSocket())
                    {
                    Blocking.connect(clientOut, new InetSocketAddress32(addr, server.getLocalPort()));

                    final byte[] MAGIC = generateMagic();

                    try (OutputStream out = clientOut.getOutputStream())
                        {
                        out.write(MAGIC);
                        }

                    // This timeout would seem to be allowed to be set much shorter, we've already established the
                    // connection above so it should be ready to come out without any delay.  We don't want to assume
                    // too much about how the underlying sockets work, and it is possible that the inbound connection
                    // isn't available immediately. In fact since we use multiplexed sockets this is quite true as we
                    // have to wait for the header, and connect finishing only ensures that the header has been sent,
                    // it may not have yet been received, thus the full timeout is quite relevant.
                    server.setSoTimeout((int) Timeout.remainingTimeoutMillis());

                    try (Socket clientIn = server.accept())
                        {
                        byte[] abIn = new byte[MAGIC.length];
                        try (InputStream is = clientIn.getInputStream())
                            {
                            // either 8 bytes were read or the connection was closed; regardless check what was read
                            // and compare to what is expected
                            is.read(abIn);
                            }

                        // verify that the connection is truly from us by ensuring the generated 'magic' payload
                        // is what we receive; we can not assume any information about source ip / port as when
                        // routed through a NAT that NAT can rewrite the source IP to appear like it is the NAT
                        // host in addition to using a different port
                        if (Arrays.equals(abIn, MAGIC))
                            {
                            // cache the addr (with base port) to avoid all this mess on future checks
                            // we don't cache failures as most failures would be timeouts which could be transient
                            setNAT.add(addrSockBase);

                            // we add NAT addresses to the start of the cached bindable address list to give them
                            // priority when used as a source in #getRoutes
                            synchronized (s_dequeAddrBindable)
                                {
                                InetAddress addrIP = addrSockBase.getAddress();
                                if (!s_dequeAddrBindable.contains(addrIP))
                                    {
                                    s_dequeAddrBindable.addFirst(addrIP);
                                    }
                                }
                            return true;
                            }

                        return false;
                        }
                    }
                }
            }
        catch (IOException | InterruptedException e)
            {
            return false;
            }
        }

    /**
     * Return an InetAddress object given the raw IP address.
     *
     * @param abAddr  the raw IP address in network byte order
     *
     * @return the InetAddress object
     *
     * @throws UnknownHostException if no match can be found
     */
    public static InetAddress getByAddress(byte[] abAddr)
            throws UnknownHostException
        {
        if (abAddr == null)
            {
            return null;
            }

        return InetAddress.getByAddress(abAddr);
        }

    /**
    * Converts an IPv4 compatible address to a long value.
    *
    * @param addr  an instance of InetAddress to convert to a long
    *
    * @return  a long value holding the IPv4 address
    */
    public static long toLong(InetAddress addr)
        {
        byte[] ab = addr.getAddress();
        int    of = ab.length == 4 ? 0 : 12;
        return ((((long) ab[of + 0]) & 0xFFL) << 24)
             | ((((long) ab[of + 1]) & 0xFFL) << 16)
             | ((((long) ab[of + 2]) & 0xFFL) <<  8)
             | ((((long) ab[of + 3]) & 0xFFL)      );
        }

    /**
     * Select the appropriate source addresses for connecting to the specified destination addresses.
     *
     * @param collSource  the source addresses
     * @param collDest    the destination addresses
     *
     * @return the source addresses which are believed to have routes to the destinations
     */
    public static Collection<InetAddress> getRoutes(Iterable<? extends InetAddress> collSource, Iterable<? extends InetAddress> collDest)
        {
        // find source address with the largest IP overlap with a destination address, i.e. most likely on the same subnet
        List<InetAddress> listAddr = new ArrayList<>();
        int               cbBest   = 0;

        for (InetAddress addrDest : collDest)
            {
            byte[] abDest = addrDest.getAddress();
            for (InetAddress addrSrc : collSource)
                {
                byte[] abSrc  = addrSrc.getAddress();
                int    ofSrc  = 0;
                int    ofDest = 0;
                if (abDest.length != abSrc.length)
                    {
                    // comparing ipv6 vs ipv4, but it could be an ipv6 encoded ipv4, skip over leading zeros
                    for (; ofDest < abDest.length && abDest[ofDest] == 0; ++ofDest);
                    for (; ofSrc  < abSrc.length  && abSrc [ofSrc]  == 0; ++ofSrc);

                    if (abDest.length - ofDest != abSrc.length - ofSrc)
                        {
                        continue; // not comparable; move onto next source
                        }
                    }

                // Note: the following comparison is byte based, technically it should be bit based though subnets
                // are in practice never really split that way
                int cbEqual = 0;
                for (int cb = abSrc.length - ofSrc; cbEqual < cb && abSrc[ofSrc + cbEqual] == abDest[ofDest + cbEqual]; ++cbEqual);

                if (cbEqual >= cbBest)
                    {
                    // addrSrc is at least as good as anything we've found so far
                    // if it better then toss all others
                    if (cbEqual > cbBest)
                        {
                        cbBest = cbEqual;
                        listAddr.clear(); // discard all worse matches
                        }
                    listAddr.add(addrSrc);
                    }
                }
            }

        return listAddr;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Set the specified number of bits of a byte array representing a subnet
     * mask.
     *
     * @param ab     the array to fill
     * @param cBits  the number of bits to set
     */
    protected static void setSubnetMask(byte[] ab, int cBits)
        {
        if (ab.length * 8 < cBits)
            {
            throw new IllegalArgumentException("subnet mask of " + cBits +
                    " exceeds address length of " + ab.length * 8);
            }

        int cBytes = cBits / 8;
        for (int i = 0; i < cBytes; ++i)
            {
            ab[i] = -1;
            }

        for (int i = 0, c = cBits % 8; i < c; ++i)
            {
            ab[cBytes] |= 1 << (7 - i);
            }
        }

    /**
     * Return a unique byte array.
     *
     * @return a unique byte array
     */
    protected static byte[] generateMagic()
        {
        int nRnd = ThreadLocalRandom.current().nextInt();
        return new byte[]
                {
                0x52, 0x41, 0x4A, 0x41,
                (byte) ((nRnd >> 24) & 0xFF),
                (byte) ((nRnd >> 16) & 0xFF),
                (byte) ((nRnd >>  8) & 0xFF),
                (byte) ((nRnd)       & 0xFF)
                };
        }

    /**
     * Return true if the given address is a local address.
     *
     * @param addr  the InetAddress to check
     *
     * @return true if the given address is a local address
     *
     * @throws SocketException
     */
    protected static boolean checkLocalAddress(InetAddress addr)
            throws SocketException
        {
        long cTimeout = INETADDRESS_TIMEOUT.get();
        if (SafeClock.INSTANCE.getSafeTimeMillis() > cTimeout)
            {
            try
                {
                Set<InetAddress> setAddresses = new HashSet<>();
                for (Enumeration<NetworkInterface> enmr = NetworkInterface.getNetworkInterfaces(); enmr.hasMoreElements(); )
                    {
                    NetworkInterface iface = enmr.nextElement();

                    for (Enumeration<InetAddress> enmrAddr = iface.getInetAddresses(); enmrAddr.hasMoreElements(); )
                        {
                        setAddresses.add(enmrAddr.nextElement());
                        }
                    }

                if (INETADDRESS_TIMEOUT.compareAndSet(cTimeout, SafeClock.INSTANCE.getSafeTimeMillis() + INETADDRESS_REFRESH))
                    {
                    LOCAL_ADDRESSES.clear();
                    LOCAL_ADDRESSES.addAll(setAddresses);
                    }
                }
            catch (SocketException e)
                { // denigrates to NI.getByInetAddress
                }
            }

        // check our cache of addresses and if not present delegate to the
        // more expensive check
        return LOCAL_ADDRESSES.contains(addr) ||
                NetworkInterface.getByInetAddress(addr) != null;
        }

    // ----- data fields and constants --------------------------------------

    /**
     * The value of system property "java.net.preferIPv4Stack".
     *
     * @see <a href="http://download.oracle.com/javase/7/docs/technotes/guides/net/ipv6_guide/index.html">
     *      Networking IPv6 User Guide</a>
     */
    public static final boolean PreferIPv4Stack = Boolean.getBoolean("java.net.preferIPv4Stack");

    /**
     * The value of system property "java.net.preferIPv6Addresses".
     *
     * @see <a href="http://download.oracle.com/javase/7/docs/technotes/guides/net/ipv6_guide/index.html">
     *      Networking IPv6 User Guide</a>
     */
    public static final boolean PreferIPv6Addresses = Boolean.getBoolean("java.net.preferIPv6Addresses");

    /**
     * The wildcard address.
     */
    public static final InetAddress ADDR_ANY = new InetSocketAddress((InetAddress) null, 0).getAddress();

    /**
     * The absolute time that the cache of InetAddresses should be considered
     * valid. After this point they are stale and should be refreshed to be
     * aligned with what the OS reports.
     */
    private static final AtomicLong INETADDRESS_TIMEOUT = new AtomicLong();

    /**
     * The data structure used to hold the InetAddresses.
     */
    private static final Set<InetAddress> LOCAL_ADDRESSES = new CopyOnWriteArraySet<>();

    /**
     * Cached localhost address.
     */
    private static InetAddress s_addrLocalhost;

    /**
     * Cached local NAT addresses.
     */
    private static final AtomicReference<Set<InetSocketAddress>> s_refSetNAT = new AtomicReference<>();

    /**
     * Cached local (and NAT) bindable addresses.
     */
    private static final Deque<InetAddress> s_dequeAddrBindable = new ConcurrentLinkedDeque<>();

    /**
     * True once s_dequeAddrBindable has been populated with local dddresses.
     */
    private static volatile boolean s_fDequeAddrBindablePopulated;

    /**
     * The default timeout for testing if an address is NAT'd.
     *
     * We use a rather high default value here.  The reason is that it is assumed that the isNatLocalAddress will
     * normally pass as the caller must have had some indication which would suggest that it is in fact a local NAT
     * address.  The test will involve going out through the NAT, and as such could have packet loss so we want to
     * allow TCP enough time to retry a connect if needed.  So successes should be fast, but we allow them to be slow
     * in case of packet loss.  A failure will also normally be fast.  If the NAT address references another machine
     * and that machine has no listener on the port then we'll get a reject and be done quickly.  So we're only slow
     * on a failure if there we manage to connect to someone else, or if our connect attempt has no accept or reject.
     */
    protected static final long NAT_CHECK_TIMEOUT = new Duration(
            System.getProperty(InetAddresses.class.getName() + ".natCheckTimeout", "10s"))
            .as(Duration.Magnitude.MILLI);

    /**
     * The default time the cache of local addresses is assumed to be correct.
     * Once this threshold is reached the local state is brought back in sync
     * with all local inet addresses.
     *
     * The primary benefit of this is to avoid the horrendously expensive call
     * to {@link NetworkInterface#getByInetAddress(InetAddress)} on windows which
     * should have no ill effects on linux.
     */
    protected static final long INETADDRESS_REFRESH = new Duration(
                System.getProperty(InetAddresses.class.getName() + ".localAddressCacheTimeout", "1h"))
                .as(Duration.Magnitude.MILLI);
    }
