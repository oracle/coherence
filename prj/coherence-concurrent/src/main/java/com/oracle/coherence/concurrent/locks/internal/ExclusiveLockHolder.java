/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates.
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

import java.util.Set;

import java.util.stream.Collectors;

/**
 * A data structure that encapsulates server-side exclusive locking logic.
 *
 * @since 21.12
 * @author Aleks Seovic  2021.10.19
 */
public class ExclusiveLockHolder
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for serialization interfaces
     */
    public ExclusiveLockHolder()
        {}

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
     * @param owner  the {@link LockOwner}
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
     * @param memberId  the member {@link UUID}
     *
     * @return {@code true} if this lock is currently owned by he specified
     *                      member
     */
    public boolean isLockedByMember(UUID memberId)
        {
        return isLocked() && m_lockOwner.getMemberId().equals(memberId);
        }

    /**
     * Return {@code true} if this lock is currently owned by the remote client
     * (Extend or gRPC).
     *
     * @return {@code true} if this lock is currently owned by the remote client
     */
    public boolean isLockedByClient()
        {
        return isLocked() && m_lockOwner.isClient();
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
            return false;
            }
        else
            {
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
     * Remove the lock, if it's owned by the specified member.
     *
     * @param memberId  the UID of a member to remove the locks for
     *
     * @return {@code true} if this holder was modified
     */
    protected boolean removeLocksFor(UUID memberId)
        {
        if (isLockedByMember(memberId))
            {
            m_lockOwner = null;
            return true;
            }

        return false;
        }

    /**
     * Remove the lock if it's NOT owned by one of the specified cluster members
     * or a remote client (Extend or gRPC).
     *
     * @param setMemberIds  the UUIDs of the cluster members to retain the locks for
     *
     * @return {@code true} if this holder was modified
     */
    protected boolean retainLocksFor(Set<UUID> setMemberIds)
        {
        if (isLocked() && !isLockedByClient() && !setMemberIds.contains(m_lockOwner.getMemberId()))
            {
            m_lockOwner = null;
            return true;
            }

        return false;
        }

    @Override
    public String toString()
        {
        return "ExclusiveLockHolder{" +
               "locked=" + isLocked() +
               ", owner=" + getOwner() +
               '}';
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_lockOwner = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_lockOwner);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_lockOwner = in.readObject(0);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_lockOwner);
        }

    // ----- inner class: RemoveLocks ---------------------------------------

    /**
     * An EntryProcessor that will remove locks for the provided member ID,
     * or all the locks for the members that are not in the cluster any longer
     * (if the specified member ID is {@code null}).
     */
    public static class RemoveLocks
            implements InvocableMap.EntryProcessor<String, ExclusiveLockHolder, Void>, ExternalizableLite, PortableObject
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
        public RemoveLocks(UUID memberId)
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
         * The member UID to remove all the locks for.
         */
        protected UUID m_memberId;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The current owner of this lock.
     */
    private LockOwner m_lockOwner;
    }
