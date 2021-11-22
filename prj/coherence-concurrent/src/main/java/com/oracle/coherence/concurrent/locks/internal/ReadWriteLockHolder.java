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
 * A data structure that encapsulates server-side read/write locking logic,
 * and keeps track of the information about lock owners and pending locks that
 * can be introspected by the clients.
 *
 * @since 21.12
 * @author Aleks Seovic  2021.10.19
 */
public class ReadWriteLockHolder
        implements Serializable
    {
    /**
     * Return {@code true} if this lock is currently owned exclusively
     * by anyone.
     *
     * @return {@code true} if this lock is currently owned exclusively by
     *         anyone
     */
    public boolean isWriteLocked()
        {
        return m_writeLock != null;
        }

    /**
     * Return {@code true} if this lock is currently locked for reads
     * by one or more threads.
     *
     * @return {@code true} if this lock is currently locked for reads by
     *         anyone
     */
    public boolean isReadLocked()
        {
        return !m_setReadLocks.isEmpty();
        }

    /**
     * Return {@code true} if this lock is currently locked for reads
     * or writes by anyone.
     *
     * @return {@code true} if this lock is currently locked for reads
     *         or writes by anyone
     */
    public boolean isLocked()
        {
        return isWriteLocked() || isReadLocked();
        }

    /**
     * Return {@code true} if this lock is currently owned exclusively
     * by the specified owner.
     *
     * @param owner  the lock owner to check
     *
     * @return {@code true} if this lock is currently owned exclusively by
     *         the specified owner
     */
    public boolean isWriteLockedBy(LockOwner owner)
        {
        return isWriteLocked() && m_writeLock.equals(owner);
        }

    /**
     * Return {@code true} if this lock is currently owned exclusively
     * by the specified member, regardless of which thread on that member owns it.
     *
     * @param memberId  the ID of the member to check
     *
     * @return {@code true} if this lock is currently owned exclusively by
     *         the specified member
     */
    public boolean isWriteLockedByMember(UID memberId)
        {
        return isWriteLocked() && m_writeLock.getMemberId().equals(memberId);
        }

    /**
     * Return {@code true} if this lock is currently locked for reads
     * by the specified owner.
     *
     * @param owner  the lock owner to check
     *
     * @return {@code true} if this lock is currently locked for reads by
     *         the specified owner
     */
    public boolean isReadLockedBy(LockOwner owner)
        {
        return isReadLocked() && m_setReadLocks.contains(owner);
        }

    /**
     * Return {@code true} if this lock is currently locked for reads
     * by the specified member, regardless of which thread on that member locked it.
     *
     * @param memberId  the ID of the member to check
     *
     * @return {@code true} if this lock is currently locked for reads by
     *         the specified member
     */
    public boolean isReadLockedByMember(UID memberId)
        {
        return isReadLocked() && m_setReadLocks.stream().anyMatch(lo -> lo.getMemberId().equals(memberId));
        }

    /**
     * Return {@code true} if this lock is currently locked for reads
     * by the specified owner.
     *
     * @param owner  the lock owner to check
     *
     * @return {@code true} if this lock is currently locked for reads by
     *         the specified owner
     */
    public boolean isLockedBy(LockOwner owner)
        {
        return isWriteLockedBy(owner) || isReadLockedBy(owner);
        }

    /**
     * Return {@code true} if this lock is currently locked for either reads or writes
     * by the specified member, regardless of which thread on that member locked it.
     *
     * @param memberId  the ID of the member to check
     *
     * @return {@code true} if this lock is currently locked for either reads or writes by
     *         the specified member
     */
    public boolean isLockedByMember(UID memberId)
        {
        return isWriteLockedByMember(memberId) || isReadLockedByMember(memberId);
        }

    /**
     * Return {@code true} if there is a pending write lock
     * by the specified owner.
     *
     * @param owner  the lock owner to check
     *
     * @return {@code true} if there is a pending write lock by
     *         the specified owner
     */
    public boolean isPendingWrite(LockOwner owner)
        {
        return m_setPendingWrite.contains(owner);
        }

    /**
     * Return {@code true} if there is a pending read lock
     * by the specified owner.
     *
     * @param owner  the lock owner to check
     *
     * @return {@code true} if there is a pending read lock by
     *         the specified owner
     */
    public boolean isPendingRead(LockOwner owner)
        {
        return m_setPendingRead.contains(owner);
        }

    /**
     * Return {@code true} if there is a pending read or write lock
     * by the specified owner.
     *
     * @param owner  the lock owner to check
     *
     * @return {@code true} if there is a pending read or write lock by
     *         the specified owner
     */
    public boolean isPending(LockOwner owner)
        {
        return isPendingWrite(owner) || isPendingRead(owner);
        }

    /**
     * Attempt to obtain write lock, and return {@code true} if successful.
     *
     * @param owner  the lock owner to obtain the lock for
     *
     * @return {@code true} if the lock was successfully obtained
     */
    public boolean lockWrite(LockOwner owner)
        {
        if (isWriteLockedBy(owner))
            {
            return true;
            }
        else if (isLocked())
            {
            m_setPendingWrite.add(owner);
            return false;
            }
        else
            {
            m_setPendingWrite.remove(owner);
            m_writeLock = owner;
            return true;
            }
        }

    /**
     * Release write lock, and return {@code true} if successful.
     *
     * @param owner  the lock owner to release the lock for
     *
     * @return {@code true} if the lock was successfully released
     */
    public boolean unlockWrite(LockOwner owner)
        {
        if (isWriteLockedBy(owner))
            {
            m_writeLock = null;
            return true;
            }
        return false;
        }

    /**
     * Attempt to obtain read lock, and return {@code true} if successful.
     *
     * @param owner  the lock owner to obtain the lock for
     *
     * @return {@code true} if the lock was successfully obtained
     */
    public boolean lockRead(LockOwner owner)
        {
        if (isReadLockedBy(owner))
            {
            return true;
            }
        else if (isWriteLockedBy(owner) || !isWriteLocked())
            {
            m_setPendingRead.remove(owner);
            m_setReadLocks.add(owner);
            return true;
            }
        else
            {
            m_setPendingRead.add(owner);
            return false;
            }
        }

    /**
     * Release read lock, and return {@code true} if successful.
     *
     * @param owner  the lock owner to release the lock for
     *
     * @return {@code true} if the lock was successfully released
     */
    public boolean unlockRead(LockOwner owner)
        {
        if (isReadLockedBy(owner))
            {
            m_setReadLocks.remove(owner);
            return true;
            }
        return false;
        }

    /**
     * Return the owner of a write lock, if any.
     *
     * @return the owner of a write lock, if any; {@code null} otherwise
     */
    public LockOwner getWriteLock()
        {
        return m_writeLock;
        }

    /**
     * Return the set of read lock owners.
     *
     * @return the set of read lock owners; could be empty
     */
    public Set<? extends LockOwner> getReadLocks()
        {
        return m_setReadLocks;
        }

    /**
     * Return the count of active read locks.
     *
     * @return the count of active read locks
     */
    public int getReadLockCount()
        {
        return m_setReadLocks.size();
        }

    /**
     * Return the set of pending write locks.
     *
     * @return the set of pending write locks; could be empty
     */
    public Set<? extends LockOwner> getPendingWriteLocks()
        {
        return m_setPendingWrite;
        }

    /**
     * Return the set of pending read locks.
     *
     * @return the set of pending read locks; could be empty
     */
    public Set<? extends LockOwner> getPendingReadLocks()
        {
        return m_setPendingRead;
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

        if (isWriteLockedByMember(memberId))
            {
            m_writeLock = null;
            fModified = true;
            }

        fModified = removeLocksFor(m_setReadLocks, memberId) || fModified;
        fModified = removeLocksFor(m_setPendingRead, memberId) || fModified;
        fModified = removeLocksFor(m_setPendingWrite, memberId) || fModified;

        return fModified;
        }

    /**
     * Remove all the locks that are owned by a specified member from a
     * specified set of locks.
     *
     * @param memberId  the UID of a member to remove the locks for
     *
     * @return {@code true} if the set was modified
     */
    private boolean removeLocksFor(Set<LockOwner> set, UID memberId)
        {
        boolean fModified = false;

        Iterator<LockOwner> it = set.iterator();
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
     * @param setMemberIds  the UIDs of the valid members to retain the locks for
     *
     * @return {@code true} if this holder was modified
     */
    protected boolean retainLocksFor(Set<UID> setMemberIds)
        {
        boolean fModified = false;

        if (isWriteLocked() && !setMemberIds.contains(m_writeLock.getMemberId()))
            {
            m_writeLock = null;
            fModified = true;
            }

        fModified = retainLocksFor(m_setReadLocks, setMemberIds) || fModified;
        fModified = retainLocksFor(m_setPendingRead, setMemberIds) || fModified;
        fModified = retainLocksFor(m_setPendingWrite, setMemberIds) || fModified;

        return fModified;
        }

    /**
     * Remove all the locks that are NOT owned by one of the specified members
     * from the specified set.
     *
     * @param set           the set to remove the locks for invalid members from
     * @param setMemberIds  the UIDs of the valid members to retain the locks for
     *
     * @return {@code true} if this holder was modified
     */
    private boolean retainLocksFor(Set<LockOwner> set, Set<UID> setMemberIds)
        {
        boolean fModified = false;

        Iterator<LockOwner> it = set.iterator();
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

    // ---- Object methods --------------------------------------------------

    @Override
    public String toString()
        {
        return "ReadWriteLockHolder{" +
               "writeLocked=" + isWriteLocked() +
               ", readLocked=" + isReadLocked() +
               ", writeLockOwner=" + getWriteLock() +
               ", readLocks=" + getReadLocks() +
               ", pendingWriteLocks=" + getPendingWriteLocks() +
               ", pendingReadLocks=" + getPendingReadLocks() +
               '}';
        }

    // ----- inner class: RemoveLocks ---------------------------------------

    /**
     * An EntryProcessor that will remove locks for the provided member ID,
     * or all the locks for the members that are not in the cluster any longer
     * (if the specified member ID is {@code null}).
     */
    public static class RemoveLocks
            implements InvocableMap.EntryProcessor<String, ReadWriteLockHolder, Void>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Default constructor (for deserialization).
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
        public Void process(InvocableMap.Entry<String, ReadWriteLockHolder> entry)
            {
            ReadWriteLockHolder holder = entry.getValue();

            if (m_memberId == null)  // clean up locks for invalid member UIDs
                {
                ServiceInfo info = ((BinaryEntry<String, ReadWriteLockHolder>) entry)
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
     * The owner of the exclusive write lock.
     */
    private LockOwner m_writeLock;

    /**
     * The set of read lock owners.
     */
    private final Set<LockOwner> m_setReadLocks = new HashSet<>();

    /**
     * The set of pending read locks.
     */
    private final Set<LockOwner> m_setPendingRead = new HashSet<>();

    /**
     * The set of pending write locks.
     */
    private final Set<LockOwner> m_setPendingWrite = new HashSet<>();
    }
