/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;

import com.oracle.coherence.common.internal.net.MultiplexedSocketProvider;

import com.oracle.coherence.common.net.InetSocketAddress32;

import com.tangosol.net.CompositeSocketAddressProvider;
import com.tangosol.net.SocketAddressProvider;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * The WrapperSocketAddressProvider is a {@link CompositeSocketAddressProvider}
 * which converts {@link InetSocketAddress} addresses provided by the inner
 * addresses and address providers to {@link InetSocketAddress32}.
 *
 * @author phf,wl 2012.03.05
 *
 * @since Coherence 12.1.2
 */
public class WrapperSocketAddressProvider
        extends CompositeSocketAddressProvider
    {

    // ----- constructors ---------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public WrapperSocketAddressProvider(SocketAddressProvider provider)
        {
        this(provider, -1);
        }

    /**
     * {@inheritDoc}
     */
    public WrapperSocketAddressProvider(SocketAddress address)
        {
        this(address, -1);
        }

    /**
     * Construct a WrapperSocketAddressProvider from the specified {@link SocketAddress}.
     *
     * @param provider  the initial wrapped {@link SocketAddressProvider}
     * @param nSubPort  the sub-port to add to each address returned by {@link #getNextAddress()}
     */
    public WrapperSocketAddressProvider(SocketAddressProvider provider, int nSubPort)
        {
        super(provider);
        m_nSubPort = nSubPort;
        }

    /**
     * Construct a WrapperSocketAddressProvider from the specified {@link SocketAddress}.
     *
     * @param address   the initial wrapped {@link SocketAddress}
     * @param nSubPort  the sub-port to add to each address returned by {@link #getNextAddress()}
     */
    public WrapperSocketAddressProvider(SocketAddress address, int nSubPort)
        {
        super(address);
        m_nSubPort = nSubPort;
        }

    // ----- SocketAddressProvider interface --------------------------------

    /**
     * {@inheritDoc}
     */
    public SocketAddress getNextAddress()
        {
        SocketAddress address = super.getNextAddress();

        if (address instanceof InetSocketAddress32)
            {
            InetSocketAddress32 baseAddress = (InetSocketAddress32) address;
            address = new InetSocketAddress32(baseAddress.getAddress(),
                MultiplexedSocketProvider.getPort(MultiplexedSocketProvider.getBasePort(baseAddress.getPort()),
                m_nSubPort));
            }
        else if (address instanceof InetSocketAddress)
            {
            InetSocketAddress baseAddress = (InetSocketAddress) address;
            address = new InetSocketAddress32(baseAddress.getAddress(),
                MultiplexedSocketProvider.getPort(baseAddress.getPort(), m_nSubPort));
            }

        return address;
        }

    // ----- factory methods ------------------------------------------------

    /**
     * Create a {@link WrapperSocketAddressProvider} which converts addresses
     * provided by the inner addresses and address providers to
     * {@link InetSocketAddress32} format with the ephemeral sub-port value (0) set.
     *
     * @param address  the {@link SocketAddress} to wrap
     *
     * @return a {@link WrapperSocketAddressProvider}
     */
    public static WrapperSocketAddressProvider createEphemeralSubPortSocketAddressProvider(
            SocketAddress address)
        {
        return new WrapperSocketAddressProvider(address, 0);
        }

    /**
     * Create a {@link WrapperSocketAddressProvider} which converts addresses
     * provided by the inner addresses and address providers to
     * {@link InetSocketAddress32} format with the ephemeral sub-port value (0) set.
     *
     * @param provider  the {@link SocketAddressProvider} to wrap
     *
     * @return a {@link WrapperSocketAddressProvider}
     */
    public static WrapperSocketAddressProvider createEphemeralSubPortSocketAddressProvider(
            SocketAddressProvider provider)
        {
        return new WrapperSocketAddressProvider(provider, 0);
        }

    // ----- data fields ----------------------------------------------------

    /**
     * The sub-port to attach to all {@link InetSocketAddress32} addresses
     * returned by {@link #getNextAddress()}.
     */
    private int m_nSubPort;
    }
