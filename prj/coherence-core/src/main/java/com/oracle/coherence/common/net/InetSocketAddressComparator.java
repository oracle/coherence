/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net;


import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Comparator;


/**
 * Comparator implementation suitable for comparing InetSocketAddress objects.
 * <p>
 * Additionally this comparator supports InetSocketAddress32 objects.
 *
 * @author mf  2010.11.03
 */
public class InetSocketAddressComparator
        implements Comparator<SocketAddress>
    {
    /**
     * {@inheritDoc}
     */
    public int compare(SocketAddress addrA, SocketAddress addrB)
        {
        if (addrA == addrB)
            {
            return 0;
            }
        else if (addrA == null)
            {
            return -1;
            }
        else if (addrB == null)
            {
            return 1;
            }

        InetAddress ipA = getAddress(addrA);
        InetAddress ipB = getAddress(addrB);
        int         n;

        if (ipA == null && ipB == null)
            {
            String sHostA = getHostName(addrA);
            String sHostB = getHostName(addrB);
            n = sHostA == sHostB ? 0 // only for null == null
              : sHostA == null   ? -1
              : sHostB == null   ? 1
              : sHostA.compareTo(sHostB);
            }
        else if (ipA != null && ipB != null)
            {
            // compare by IP
            n = InetAddressComparator.INSTANCE.compare(ipA, ipB);
            }
        else
            {
            // not comparable
            throw new IllegalArgumentException("cannot compare resolved to unresolved addresses");
            }

        return n == 0
            ? getPort(addrA) - getPort(addrB)
            : n;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return the hostname for the specified SocketAddress.
     *
     * @param addr  the SocketAddress
     *
     * @return the hostname
     */
    static String getHostName(SocketAddress addr)
        {
        return addr instanceof InetSocketAddress
                ? ((InetSocketAddress) addr).getHostName()
                : ((InetSocketAddress32) addr).getHostName();
        }

    /**
     * Return the InetAddress for the specified SocketAddress.
     *
     * @param addr  the SocketAddress
     *
     * @return the InetAddress
     */
    static InetAddress getAddress(SocketAddress addr)
        {
        return addr instanceof InetSocketAddress
                ? ((InetSocketAddress) addr).getAddress()
                : ((InetSocketAddress32) addr).getAddress();
        }

    /**
     * Return the port for the specified SocketAddress.
     *
     * @param addr  the SocketAddress
     *
     * @return the port
     */
    static int getPort(SocketAddress addr)
        {
        return addr instanceof InetSocketAddress
                ? ((InetSocketAddress) addr).getPort()
                : ((InetSocketAddress32) addr).getPort();
        }


    /**
     * Reusable instance of the comparator.
     */
    public static final InetSocketAddressComparator INSTANCE =
            new InetSocketAddressComparator();
    }
