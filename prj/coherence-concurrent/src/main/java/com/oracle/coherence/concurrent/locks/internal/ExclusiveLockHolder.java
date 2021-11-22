/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks.internal;

import com.oracle.coherence.concurrent.locks.LockOwner;

import com.tangosol.net.Member;
import com.tangosol.net.ServiceInfo;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.UID;

import java.io.Serializable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import java.util.stream.Collectors;

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
     * Return {@code true} if this lock is currently owned by the specified
     * member, regardless of which thread on that member owns it.
     *
     * @return {@code true} if this lock is currently owned by he specified
     *                      member
     */
    public boolean isLockedByMember(UID memberId)
        {
        return isLocked() && m_lockOwner.getMemberId().equals(memberId);
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
     * Return {@code true} if the lock for the specified member is
     * currently pending.
     *
     * @return {@code true} if the lock for the specified member is
     *                      currently pending
     */
    public boolean isPendingForMember(UID memberId)
        {
        return m_setPending.stream().anyMatch(owner -> owner.getMemberId().equals(memberId));
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

    /**
     * Remove all the locks, both the active and pending, that are owned by
     * a specified member.
     *
     * @param memberId  the UID of a member to remove the locks for
     *
     * @return {@code true} if this holder was modified
     */
    protected boolean removeLocksFor(UID memberId)
        {
        boolean fModified = false;

        if (isLockedByMember(memberId))
            {
            m_lockOwner = null;
            fModified = true;
            }

        Iterator<LockOwner> it = m_setPending.iterator();
        while (it.hasNext())
            {
            LockOwner owner = it.next();
            if (owner.getMemberId().equals(memberId))
                {
                it.remove();
                fModified = true;
                }
            }

        return fModified;
        }

    /**
     * Remove all the locks, both the active and pending, that are NOT owned by
     * one of the specified members.
     *
     * @param setMemberIds  the UIDs of the member to retain the locks for
     *
     * @return {@code true} if this holder was modified
     */
    protected boolean retainLocksFor(Set<UID> setMemberIds)
        {
        boolean fModified = false;

        if (isLocked() && !setMemberIds.contains(m_lockOwner.getMemberId()))
            {
            m_lockOwner = null;
            fModified = true;
            }

        Iterator<LockOwner> it = m_setPending.iterator();
        while (it.hasNext())
            {
            LockOwner owner = it.next();
            if (!setMemberIds.contains(owner.getMemberId()))
                {
                it.remove();
                fModified = true;
                }
            }

        return fModified;
        }

    @Override
    public String toString()
        {
        return "ExclusiveLockHolder{" +
               "locked=" + isLocked() +
               ", owner=" + getOwner() +
               ", pendingLocks=" + getPendingLocks() +
               '}';
        }

    // ----- inner class: RemoveLocks ---------------------------------------

    /**
     * An EntryProcessor that will remove locks for the provided member ID,
     * or all the locks for the members that are not in the cluster any longer
     * (if the specified member ID is {@code null}).
     */
    public static class RemoveLocks
            implements InvocableMap.EntryProcessor<String, ExclusiveLockHolder, Void>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Default no-arg constructor.
         */
        public RemoveLocks()
            {
            }

        /**
         * Create a RemoveLocks processor with the given member id.
         *
         * @param memberId  remove all LockHolders that have this member id,
         *                  or all the locks for the members that are not in the
         *                  cluster any longer (if the specified member ID is
         *                  {@code null}).
         */
        public RemoveLocks(UID memberId)
            {
            m_memberId = memberId;
            }

        @SuppressWarnings("unchecked")
        @Override
        public Void process(InvocableMap.Entry<String, ExclusiveLockHolder> entry)
            {
            ExclusiveLockHolder holder = entry.getValue();

            if (m_memberId == null)  // clean up locks for invalid member UIDs
                {
                ServiceInfo info = ((BinaryEntry<String, ExclusiveLockHolder>) entry)
                        .getContext().getCacheService().getInfo();
                Set<Member> setServiceMembers = info.getServiceMembers();
                Set<UID>    setValidMemberIds = setServiceMembers.stream()
                        .map(Member::getUid)
                        .collect(Collectors.toSet());
                if (holder.retainLocksFor(setValidMemberIds))
                    {
                    entry.setValue(holder);
                    }
                }
            else
                {
                if (holder.removeLocksFor(m_memberId))
                    {
                    entry.setValue(holder);
                    }
                }
            return null;
            }

        // ----- data members -----------------------------------------------

        /**
         * The member UID to remove all the locks for.
         */
        protected UID m_memberId;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The current owner of this lock.
     */
    private LockOwner m_lockOwner;

    /**
     * The set of pending lock requests.
     */
    private final Set<LockOwner> m_setPending = new HashSet<>();
    }
