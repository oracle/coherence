/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net;


import java.net.InetAddress;
import java.util.Comparator;


/**
 * Comparator implementation suitable for comparing InetAddresses.
 *
 * @author mf  2010.11.03
 */
public class InetAddressComparator
        implements Comparator<InetAddress>
    {
    /**
     * {@inheritDoc}
     */
    public int compare(InetAddress addrA, InetAddress addrB)
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

        byte[] abA = addrA.getAddress();
        byte[] abB = addrB.getAddress();

        // to allow ipv4 and ipv6 address to be "correctly" compared we
        // will do the comparison backwards, discarding any leading zeros

        int ofA;
        int cbA;
        for (ofA = 0, cbA = abA.length; ofA < cbA && abA[ofA] == 0; ++ofA);
        int cbsA = cbA - ofA;

        int ofB;
        int cbB;
        for (ofB = 0, cbB = abB.length; ofB < cbB && abB[ofB] == 0; ++ofB);
        int cbsB = cbB - ofB;

        // if the significant parts are of different lengths, then the address
        // cannot be equal, so just base the decision upon length
        if (cbsA < cbsB)
            {
            return -1;
            }
        else if (cbsB < cbsA)
            {
            return 1;
            }
        else
            {
            // significant parts are of comparable length
            for (int i = 0; i < cbsA; ++i)
                {
                int n = (0xFF & (int) abA[ofA + i]) - (0xFF & (int) abB[ofB + i]); // unsigned comparison
                if (n != 0)
                  {
                  return n < 0 ? -1 : 1;
                  }
                }
            return 0;
            }
        }

    /**
     * Reusable instance of the comparator.
     */
    public static final InetAddressComparator INSTANCE =
            new InetAddressComparator();
    }
