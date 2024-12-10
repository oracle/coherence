/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;


import com.tangosol.net.Member;

import com.tangosol.util.LongArray;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * PartitionInfo holds information related to the client-side routing of
 * asynchronous requests for the specified partition.
 *
 * @author gg/rhl 01.07.2013
 * @since Coherence 12.1.3
 */
public class PartitionInfo
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct the PartitionInfo for a given target Member.
     *
     * @param memberTarget       the target member
     * @param nOwnershipVersion  the ownership version of the partition
     */
    public PartitionInfo(Member memberTarget, int nOwnershipVersion)
        {
        f_memberTarget      = memberTarget;
        f_nOwnershipVersion = nOwnershipVersion;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the target Member.
     *
     * @return the target Member
     */
    public Member getTarget()
        {
        return f_memberTarget;
        }

    /**
     * Return the ownership version of the partition.
     *
     * @return the ownership version
     */
    public int getOwnershipVersion()
        {
        return f_nOwnershipVersion;
        }

    /**
     * Obtain the atomic counter for outstanding asynchronous requests.
     *
     * @return the atomic counter
     */
    public AtomicInteger getCounter()
        {
        return f_atomicCounter;
        }

    /**
     * Return the array of deferred requests.
     *
     * @return the array of deferred requests
     */
    public LongArray getDeferredRequests()
        {
        return m_laDeferredRequests;
        }

    /**
     * Set the array of deferred requests.
     *
     * @param laRequests  the array of deferred requests
     */
    public void setDeferredRequests(LongArray laRequests)
        {
        m_laDeferredRequests = laRequests;
        }

    @Override
    public String toString()
        {
        return "PartitionInfo{" +
                "counter=" + f_atomicCounter.get() +
                ", member=" + f_memberTarget +
                ", ownershipVersion=" + f_nOwnershipVersion +
                ", deferredRequests=" + (m_laDeferredRequests == null ? "n/a" : String.valueOf(m_laDeferredRequests.getSize())) +
                '}';
        }

    // ----- data fields ----------------------------------------------------

    /**
     * Atomic counter for outstanding asynchronous requests.
     */
    private final AtomicInteger f_atomicCounter = new AtomicInteger();

    /**
     * The target Member.
     * <p>
     * When the ownership changes, a new PartitionInfo will be instantiated and
     * inserted into PartitionedCache.RequestCoordinator#PartitionInfoArray.
     */
    private final Member f_memberTarget;

    /**
     * The ownership version of the partition.
     * <p>
     * When the ownership changes, a new PartitionInfo will be instantiated and
     * inserted into PartitionedCache.RequestCoordinator#PartitionInfoArray.
     */
    private final int f_nOwnershipVersion;

    /**
     * The LongArray (keyed by request SUID) of deferred requests. This array
     * gets instantiated (value is not null) in two cases:
     * <ul>
     *   <li>the ownership has changed and the new owner is not the same as the
     *       Target member;
     *   <li>during PartitionInfo initialization the partition has no owner.
     * </ul>
     * In either case, the DeferredRequests array will be "alive" for a
     * relatively short period of time - until all the pending requests are
     * responded to.
     * <p>
     * See documentation for PartitionedCache.RequestCoordinator component.
     */
    private volatile LongArray m_laDeferredRequests;
    }
