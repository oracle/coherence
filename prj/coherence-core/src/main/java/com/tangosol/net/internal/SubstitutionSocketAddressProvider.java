/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;

import com.oracle.coherence.common.internal.net.MultiplexedSocketProvider;
import com.oracle.coherence.common.net.InetAddresses;
import com.oracle.coherence.common.net.InetSocketAddress32;
import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.SocketAddressProvider;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * An SocketAddressProvider which substitutes "unset" portions of an address.
 *
 * @author mf
 *
 * @since Coherence 12.2.1
 */
public class SubstitutionSocketAddressProvider
        implements SocketAddressProvider
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Return a new  SubstitutionAddressProvider
     *
     * @param delegate   the delegate provider
     * @param addr       the value to replace wildcard addresses with
     * @param nBasePort  the value to replace ephemeral base port values with
     * @param nSubPort   the value to replace ephemeral sub port values with
     */
    public SubstitutionSocketAddressProvider(SocketAddressProvider delegate, InetAddress addr, int nBasePort, int nSubPort)
        {
        f_delegate  = delegate;
        f_addr      = addr;
        f_nBasePort = nBasePort;
        f_nSubPort  = nSubPort;
        }

    // ----- SocketAddressProvider interface --------------------------------------

    @Override
    public SocketAddress getNextAddress()
        {
        SocketAddress address  = f_delegate.getNextAddress();
        if (address != null)
            {
            InetAddress   addrInet  = InetAddresses.getAddress(address);
            int           nPort     = InetAddresses.getPort(address);
            int           nPortBase = MultiplexedSocketProvider.getBasePort(nPort);
            int           nPortSub  = MultiplexedSocketProvider.getSubPort(nPort);

            if (addrInet.isAnyLocalAddress())
                {
                addrInet = f_addr;
                }

            if (nPortBase == 0)
                {
                nPortBase = f_nBasePort;
                }

            if (nPortSub == 0) // only 0 if ephemeral
                {
                nPortSub = f_nSubPort;
                }

            address = address instanceof InetSocketAddress
                    ? new InetSocketAddress(addrInet, nPort)
                    : new InetSocketAddress32(addrInet, MultiplexedSocketProvider.getPort(nPortBase, nPortSub));
            }

        return address;
        }

    @Override
    public void accept()
        {
        f_delegate.accept();
        }

    @Override
    public void reject(Throwable eCause)
        {
        f_delegate.reject(eCause);
        }


    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return f_delegate.toString();
        }

    @Override
    public int hashCode()
        {
        return f_delegate.hashCode();
        }

    // ----- factory helpers ------------------------------------------------

    /**
     * Return a ParameterizedBuilder<SocketAddressProvider> which substitutes the specified port for any zero address or ports.
     *
     * @param delegate   the delegate builder
     * @param address    the new address value
     * @param nPortBase  the new base port value
     * @param nPortSub   the new sub port value
     *
     * @return the factory
     */
    public static ParameterizedBuilder<SocketAddressProvider> createBuilder(ParameterizedBuilder<SocketAddressProvider> delegate, InetAddress address, int nPortBase, int nPortSub)
        {
        return new ParameterizedBuilder<SocketAddressProvider>()
            {
            @Override
            public SocketAddressProvider realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
                {
                return new SubstitutionSocketAddressProvider(delegate.realize(resolver, loader, listParameters), address, nPortBase, nPortSub);
                }
            };
        }

    // ----- data fields ----------------------------------------------------

    /**
     * The delegate provider.
     */
    private final SocketAddressProvider f_delegate;

    /**
     * The address to replace wildcard address with
     */
    private final InetAddress f_addr;

    /**
     * The port to replace ephemeral base port values with
     */
    private final int f_nBasePort;

    /**
     * The port to replace ephemeral sub port values with
     */
    private final int f_nSubPort;
    }
