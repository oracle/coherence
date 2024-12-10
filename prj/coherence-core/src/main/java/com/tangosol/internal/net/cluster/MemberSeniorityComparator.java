/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.cluster;

import com.tangosol.net.Member;

import java.util.Comparator;

/**
 * Comparator implementation that can be used to sort Member instances by
 * seniority (oldest first).
 *
 * @author jh  2012.11.06
 */
public class MemberSeniorityComparator
        implements Comparator<Member>
    {
    /**
     * Compare two Member instances by seniority.
     *
     * @param member1  the first Member to compare
     * @param member2  the second Member to compare
     *
     * @return a negative integer, 0, or a positive integer if the first
     *         Member is older than, the same age, or younger than the
     *         second, respectively
     */
    @Override
    public int compare(Member member1, Member member2)
        {
        long ldtDelta = member1.getTimestamp() - member2.getTimestamp();
        return ldtDelta <  0 ? (int) Math.max(ldtDelta, Integer.MIN_VALUE)
                             : (int) Math.min(ldtDelta, Integer.MAX_VALUE);
        }

    // ----- constants ------------------------------------------------------

    /**
     * Singleton instance of this class.
     */
    public static MemberSeniorityComparator INSTANCE = new MemberSeniorityComparator();
    }
