/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net;


import com.oracle.coherence.common.base.Hasher;

import java.net.InetAddress;


/**
 * InetAddressHasher is a Hasher which supports both IPv4 and IPv6
 * InetAddresses.
 *
 * @author mf  2011.01.11
 */
public class InetAddressHasher
        implements Hasher<InetAddress>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode(InetAddress addr)
        {
        if (addr == null)
            {
            return 0;
            }

        // produce a hash which will allow the ipv6 and ipv4 representaion
        // of the same address to hash to the same value
        int nHash = 0;
        for (byte b : addr.getAddress())
            {
            nHash += b;
            }
        return nHash;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(InetAddress addrA, InetAddress addrB)
        {
        return InetAddressComparator.INSTANCE.compare(addrA, addrB) == 0;
        }


    // ----- constants ------------------------------------------------------

    /**
     * Default instance of the InetAddressHasher.
     */
    public static final InetAddressHasher INSTANCE = new InetAddressHasher();
    }
