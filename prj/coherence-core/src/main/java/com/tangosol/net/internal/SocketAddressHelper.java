/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;

import com.oracle.coherence.common.internal.net.MultiplexedSocketProvider;

import com.oracle.coherence.common.net.InetSocketAddress32;

import com.tangosol.net.InetAddressHelper;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Helper class that encapsulates common {@link SocketAddress} functionality.
 *
 * @author phf 2012.03.15
 *
 * @since Coherence 12.1.2
 */
public class SocketAddressHelper
    {
    /**
     * Return a String representation of the given {@link SocketAddress}.
     *
     * @param address  the {@link SocketAddress}
     *
     * @return a String representation of the given {@link SocketAddress}
     */
    public static String toString(SocketAddress address)
        {
        if (address == null || address instanceof InetSocketAddress)
            {
            return toString((InetSocketAddress) address);
            }

        if (address instanceof InetSocketAddress32)
            {
            return toString((InetSocketAddress32) address);
            }

        return address.toString();
        }

    /**
     * Return a String representation of the given {@link InetSocketAddress}.
     *
     * @param address  the {@link InetSocketAddress}
     *
     * @return a String representation of the given {@link InetSocketAddress}
     */
    public static String toString(InetSocketAddress address)
        {
        if (address == null)
            {
            return "null";
            }

        String sHost = address.isUnresolved() ? address.getHostName()
            : InetAddressHelper.toString(address.getAddress());
        return sHost + ':' + address.getPort();
        }

    /**
     * Return a String representation of the given {@link InetSocketAddress32}.
     *
     * @param address  the {@link InetSocketAddress32}
     *
     * @return a String representation of the given {@link InetSocketAddress32}
     */
    public static String toString(InetSocketAddress32 address)
        {
        if (address == null)
            {
            return "null";
            }

        String sHost = address.isUnresolved() ? address.getHostName()
            : InetAddressHelper.toString(address.getAddress());
        int    nPort = address.getPort();
        String sPort = Integer.toString(MultiplexedSocketProvider.getBasePort(nPort));
        if (MultiplexedSocketProvider.isPortExtended(nPort))
            {
            sPort = sPort + '.' + MultiplexedSocketProvider.getSubPort(nPort);
            }
        return sHost + ':' + sPort;
        }
    }
