/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks.internal;

import com.oracle.coherence.concurrent.locks.LockOwner;
import java.io.Serializable;

import java.util.HashSet;
import java.util.Set;

/**
 * A data structure that encapsulates server-side exclusive locking logic,
 * and keeps track of the information about lock owner and pending locks that
 * can be introspected by the clients.
 *
 * @since 21.12
 * @author Aleks Seovic  2021.10.19
 */
public class ExclusiveLockHolder
        implements Serializable
    {
    /**
     * Return {@code true} if this lock is currently owned by anyone.
     *
     * @return {@code true} if this lock is currently owned by anyone
     */
    public boolean isLocked()
        {
        return m_lockOwner != null;
        }

    /**
     * Return {@code true} if this lock is currently owned by the specified
     * {@link LockOwner}.
     *
     * @return {@code true} if this lock is currently owned by he specified
     *                      {@link LockOwner}
     */
    public boolean isLockedBy(LockOwner owner)
        {
        return isLocked() && m_lockOwner.equals(owner);
        }

    /**
     * Return {@code true} if the lock for the specified {@link LockOwner} is
     * currently pending.
     *
     * @return {@code true} if the lock for the specified {@link LockOwner} is
     *                      currently pending
     */
    public boolean isPending(LockOwner owner)
        {
        return m_setPending.contains(owner);
        }

    /**
     * Attempt to acquire the lock.
     *
     * @param owner  the lock owner to acquire the lock for
     *
     * @return {@code true} if the lock was successfully acquired
     */
    public boolean lock(LockOwner owner)
        {
        if (isLockedBy(owner))
            {
            return true;
            }
        else if (isLocked())
            {
            m_setPending.add(owner);
            return false;
            }
        else
            {
            m_setPending.remove(owner);
            m_lockOwner = owner;
            return true;
            }
        }

    /**
     * Attempt to release the lock.
     *
     * @param owner  the lock owner to release the lock for
     *
     * @return {@code true} if the specified owner held the lock, and the
     *                      lock was successfully released
     */
    public boolean unlock(LockOwner owner)
        {
        if (isLockedBy(owner))
            {
            m_lockOwner = null;
            return true;
            }
        return false;
        }

    /**
     * Return the current lock owner, if any.
     *
     * @return  the current lock owner, if any; {@code null} otherwise
     */
    public LockOwner getOwner()
        {
        return m_lockOwner;
        }

    /**
     * Return the set of pending locks.
     *
     * @return  the set of pending locks
     */
    public Set<? extends LockOwner> getPendingLocks()
        {
        return m_setPending;
        }

    @Override
    public String toString()
        {
        return "ExclusiveLock{" +
               "locked=" + isLocked() +
               ", owner=" + getOwner() +
               ", pendingLocks=" + getPendingLocks() +
               '}';
        }

    // ---- data members ----------------------------------------------------

    /**
     * The current owner of this lock.
     */
    private LockOwner m_lockOwner;

    /**
     * The send of pending lock requests.
     */
    private final Set<LockOwner> m_setPending = new HashSet<>();
    }
