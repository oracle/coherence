/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks;

import com.tangosol.util.UID;

import java.io.Serializable;

import java.util.Objects;

/**
 * The identity of a lock owner, represented by the UID of the member, and the ID
 * of a thread holding or attempting to acquire the lock.
 *
 * @author Aleks Seovic  2021.10.19
 */
public class LockOwner implements Serializable
    {
    /**
     * Construct {@code LockOwner} instance.
     *
     * @param memberId  the member UID
     * @param threadId  the thread ID
     */
    public LockOwner(UID memberId, long threadId)
        {
        this.f_memberId = memberId;
        this.f_threadId = threadId;
        }

    /**
     * Return the member UID.
     *
     * @return the member UID
     */
    public UID getMemberId()
        {
        return f_memberId;
        }

    /**
     * Return the thread ID.
     *
     * @return the thread ID
     */
    public long getThreadId()
        {
        return f_threadId;
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        LockOwner lockOwner = (LockOwner) o;
        return f_threadId == lockOwner.f_threadId && f_memberId.equals(lockOwner.f_memberId);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(f_memberId, f_threadId);
        }

    @Override
    public String toString()
        {
        return "LockOwner{" +
               "memberId=" + f_memberId +
               ", threadId=" + f_threadId +
               '}';
        }

    // ---- data members ----------------------------------------------------

    private final UID f_memberId;
    private final long f_threadId;
    }
