/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.net.internal.SocketAddressHelper;

import com.tangosol.util.ClassHelper;

import java.net.SocketAddress;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * CompositeSocketAddressProvider is a composite of one or more
 * SocketAddressProviders. This SocketAddressProvider will provide addresses from
 * all registered providers or addresses.
 *
 * @author phf 2012.03.05
 *
 * @since Coherence 12.1.2
 */
public class CompositeSocketAddressProvider
        implements SocketAddressProvider
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Construct a CompositeSocketAddressProvider from the specified {@link SocketAddress}.
     *
     * @param address  the initial wrapped {@link SocketAddress}
     */
    public CompositeSocketAddressProvider(SocketAddress address)
        {
        addAddress(address);
        }

    /**
     * Construct a CompositeSocketAddressProvider from the specified
     * {@link SocketAddressProvider}.
     *
     * @param provider  the initial wrapped {@link SocketAddressProvider}
     */
    public CompositeSocketAddressProvider(SocketAddressProvider provider)
        {
        addProvider(provider);
        }

    // ----- CompositeSocketAddressProvider methods -------------------------

    /**
     * Add a {@link SocketAddress}.
     *
     * @param address  the {@link SocketAddress} to add
     */
    public void addAddress(SocketAddress address)
        {
        m_listProviders.add(new SingleAddressProvider(address));
        }

    /**
     * Add a {@link SocketAddressProvider}.
     *
     * @param provider  the {@link SocketAddressProvider} to add
     */
    public void addProvider(SocketAddressProvider provider)
        {
        m_listProviders.add(provider);
        }

    /**
     * Get the current address provider which is being used.
     * 
     * @return the current {@link SocketAddressProvider} or null if all
     *         providers have been used
     */
    private SocketAddressProvider getCurrentProvider()
        {
        SocketAddressProvider provider = m_currentProvider;
        if (provider == null)
            {
            provider = m_currentProvider = getNextProvider();
            }
        return provider;
        }

    /**
     * Iterate to the next address provider.
     * 
     * @return the next {@link SocketAddressProvider} or null if all
     *         providers have been used
     */
    private SocketAddressProvider getNextProvider()
        {
        Iterator<SocketAddressProvider> iter = m_iterInternal;
        if (iter == null)
            {
            iter =  m_listProviders.iterator();
            }
        SocketAddressProvider provider = m_currentProvider = iter.hasNext() ? iter.next() : null;
        if (provider == null)
            {
            // reset the iterator in case this provider is reused
            iter = null;
            }
        m_iterInternal = iter;
        return provider;
        }

    // ----- SocketAddressProvider interface---------------------------------

    /**
     * {@inheritDoc}
     */
    public void accept()
        {
        SocketAddressProvider provider = getCurrentProvider();
        if (provider != null)
            {
            provider.accept();
            }
        }

    /**
     * {@inheritDoc}
     */
    public void reject(Throwable eCause)
        {
        SocketAddressProvider provider = getCurrentProvider();
        if (provider != null)
            {
            provider.reject(eCause);
            }
        }

    /**
     * {@inheritDoc}
     */
    public SocketAddress getNextAddress()
        {
        for (SocketAddressProvider provider = getCurrentProvider();
             provider != null; provider = getNextProvider())
            {
            SocketAddress address = provider.getNextAddress();
            if (address != null)
                {
                return address;
                }
            }
        return null;
        }

    // ----- Object overrides -----------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass())
            + "{Providers=" + m_listProviders + '}';
        }

    // ----- inner class SingleAddressProvider ------------------------------

    /**
     * SocketAddressProvider wrapper for a single address dynamically added to
     * this provider.
     */
    private class SingleAddressProvider
            implements SocketAddressProvider
        {

        // ----- constructors -----------------------------------------------

        /**
         * Set the SocketAddress for this {@link SocketAddressProvider}.
         *
         * @param address  the address which will be returned by this
         *                 {@link SocketAddressProvider}
         */
        public SingleAddressProvider(SocketAddress address)
            {
            m_Address       = address;
            m_fAddressGiven = false;
            }

        // ----- SocketAddressProvider interface ----------------------------

        /**
         * {@inheritDoc}
         */
        public void accept()
            {
            // no-op
            }

        /**
         * {@inheritDoc}
         */
        public SocketAddress getNextAddress()
            {
            // toggle m_fAddressGiven in case of reuse
            return (m_fAddressGiven = !m_fAddressGiven) ? m_Address : null;
            }

        /**
         * {@inheritDoc}
         */
        public void reject(Throwable eCause)
            {
            // no-op
            }

        // ----- Object overrides -------------------------------------------

        /**
         * {@inheritDoc}
         */
        public String toString()
            {
            return "Address=" + SocketAddressHelper.toString(m_Address);
            }

        // ----- data fields ------------------------------------------------

        /**
         * The {@link SocketAddress} returned by this {@link SocketAddressProvider}.
         */
        private SocketAddress m_Address;

        /**
         * Whether the {@link SocketAddress} has already been returned.
         */
        private boolean m_fAddressGiven;
        }

    // ----- data fields ----------------------------------------------------

    /**
     * Map of Providers
     */
    private final LinkedList<SocketAddressProvider> m_listProviders = new LinkedList<SocketAddressProvider>();

    /**
     * Iterator used to iterate through the addresses and address providers.
     */
    private Iterator<SocketAddressProvider> m_iterInternal;
    
    /**
     * Current address provider being used.
     */
    private SocketAddressProvider m_currentProvider;
    }
