/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net;


import com.oracle.coherence.common.base.Hasher;
import com.oracle.coherence.common.base.NaturalHasher;

import java.net.InetAddress;
import java.net.SocketAddress;


/**
 * InetSocketAddressHasher is a Hasher which supports both IPv4 and IPv6
 * based InetSocketAddresses.
 * <p>
 * Additionally this hasher supports InetSocketAddress32 objects.
 *
 * @author mf  2011.01.11
 */
public class InetSocketAddressHasher
        implements Hasher<SocketAddress>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode(SocketAddress addr)
        {
        if (addr == null)
            {
            // null or unsupported type
            return 0;
            }
        InetAddress ip  = InetSocketAddressComparator.getAddress(addr);
        return InetSocketAddressComparator.getPort(addr) + (ip == null
            ? NaturalHasher    .INSTANCE.hashCode(
                InetSocketAddressComparator.getHostName(addr))
            : InetAddressHasher.INSTANCE.hashCode(ip));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(SocketAddress addrA, SocketAddress addrB)
        {
        try
            {
            return InetSocketAddressComparator.INSTANCE.compare(addrA, addrB) == 0;
            }
        catch (Exception e)
            {
            return false;
            }
        }


    // ----- constants ------------------------------------------------------

    /**
     * Default instance of the InetSocketAddressHasher.
     */
    public static final InetSocketAddressHasher INSTANCE =
            new InetSocketAddressHasher();
    }
