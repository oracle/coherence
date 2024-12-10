/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks.internal;

import com.oracle.coherence.concurrent.locks.LockOwner;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.Member;
import com.tangosol.net.ServiceInfo;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.UUID;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A data structure that encapsulates server-side read/write locking logic.
 *
 * @since 21.12
 * @author Aleks Seovic  2021.10.19
 */
public class ReadWriteLockHolder
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for serialization interfaces
     */
    public ReadWriteLockHolder()
        {}

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
    public boolean isWriteLockedByMember(UUID memberId)
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
    public boolean isReadLockedByMember(UUID memberId)
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
    public boolean isLockedByMember(UUID memberId)
        {
        return isWriteLockedByMember(memberId) || isReadLockedByMember(memberId);
        }

    /**
     * Return {@code true} if this lock is currently owned exclusively
     * by a client.
     *
     * @return {@code true} if this lock is currently owned exclusively by
     *         a client
     */
    public boolean isWriteLockedByClient()
        {
        return isWriteLocked() && m_writeLock.isClient();
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
            return false;
            }
        else
            {
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
            m_setReadLocks.add(owner);
            return true;
            }
        else
            {
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
     * Remove all the locks that are owned by a specified member.
     *
     * @param memberId  the UID of a member to remove the locks for
     *
     * @return {@code true} if this holder was modified
     */
    protected boolean removeLocksFor(UUID memberId)
        {
        boolean fModified = false;

        if (isWriteLockedByMember(memberId))
            {
            m_writeLock = null;
            fModified = true;
            }

        fModified = removeLocksFor(m_setReadLocks, memberId) || fModified;
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
    private boolean removeLocksFor(Set<LockOwner> set, UUID memberId)
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
     * Remove all the locks that are NOT owned by one of the specified members.
     *
     * @param setMemberIds  the UIDs of the valid members to retain the locks for
     *
     * @return {@code true} if this holder was modified
     */
    protected boolean retainLocksFor(Set<UUID> setMemberIds)
        {
        boolean fModified = false;

        if (isWriteLocked() && !isWriteLockedByClient() && !setMemberIds.contains(m_writeLock.getMemberId()))
            {
            m_writeLock = null;
            fModified = true;
            }

        fModified = retainLocksFor(m_setReadLocks, setMemberIds) || fModified;
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
    private boolean retainLocksFor(Set<LockOwner> set, Set<UUID> setMemberIds)
        {
        boolean fModified = false;

        Iterator<LockOwner> it = set.iterator();
        while (it.hasNext())
            {
            LockOwner owner = it.next();
            if (!owner.isClient() && !setMemberIds.contains(owner.getMemberId()))
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
               '}';
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_writeLock = ExternalizableHelper.readObject(in);
        ExternalizableHelper.readCollection(in, m_setReadLocks, null);
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_writeLock);
        ExternalizableHelper.writeCollection(out, m_setReadLocks);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_writeLock = in.readObject(0);
        in.readCollection(1, m_setReadLocks);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_writeLock);
        out.writeCollection(1, m_setReadLocks);
        }

    // ----- inner class: RemoveLocks ---------------------------------------

    /**
     * An EntryProcessor that will remove locks for the provided member ID,
     * or all the locks for the members that are not in the cluster any longer
     * (if the specified member ID is {@code null}).
     */
    public static class RemoveLocks
            implements InvocableMap.EntryProcessor<String, ReadWriteLockHolder, Void>, ExternalizableLite, PortableObject
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
        public RemoveLocks(UUID memberId)
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
                Set<UUID>   setValidMemberIds = setServiceMembers.stream()
                        .map(Member::getUuid)
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

        // ----- ExternalizableLite interface ---------------------------

        @Override
        public void readExternal(DataInput in)
                throws IOException
            {
            m_memberId = ExternalizableHelper.readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out)
                throws IOException
            {
            ExternalizableHelper.writeObject(out, m_memberId);
            }

        // ----- PortableObject interface -------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            m_memberId = in.readObject(0);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeObject(0, m_memberId);
            }

        // ----- data members -----------------------------------------------

        /**
         * The member UUID to remove all the locks for.
         */
        protected UUID m_memberId;
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
    }
