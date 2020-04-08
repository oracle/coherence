/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.internal;


import java.util.Comparator;


/**
* Comparator for Packet and PacketIdentifier objects.
*
* @author mf 2005.04.13
*/
public class PacketComparator
       implements Comparator
    {
    // ----- Comparator interface -----------------------------------

    /**
    * Compare two PacketIdentifiers for order. Return a negative integer,
    * zero, or a positive integer as the first argument is less than, equal
    * to, or greater than the second.
    *
    * @param o1  the first PacketIdentifier to be compared
    * @param o2  the second PacketIdentifier to be compared
    *
    * @return a negative integer, zero, or a positive integer as the first
    *         argument is less than, equal to, or greater than the second
    *
    * @throws ClassCastException if the arguments' types prevent them from being
    *                            compared by this Comparator
    */
    public int compare(Object o1, Object o2)
        {
        return compare((PacketIdentifier) o1, (PacketIdentifier) o2);
        }


    // ----- helper method for comparison ---------------------------

    /**
    * Compare two PacketIdentifiers.
    *
    * @param id1  the first PacketIdentifier to compare
    * @param id2  the first PacketIdentifier to compare
    *
    * @return a negative integer, zero, or a positive integer as the first
    *         argument is less than, equal to, or greater than the second
    */
    public static int compare(PacketIdentifier id1,
                              PacketIdentifier id2)
        {
        if (id1 == id2)
            {
            return 0;
            }
        if (id1 == null)
            {
            return -1;
            }
        if (id2 == null)
            {
            return 1;
            }

        long lMsgId1 = id1.getFromMessageId();
        long lMsgId2 = id2.getFromMessageId();
        return lMsgId1 < lMsgId2 ? -1 :
               lMsgId1 > lMsgId2 ?  1 :
               id1.getMessagePartIndex() - id2.getMessagePartIndex();
        }
    }