/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.Member;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.UUID;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Objects;

/**
 * The identity of a lock owner, represented by the UID of the member, and the ID
 * of a thread holding or attempting to acquire the lock.
 *
 * @author Aleks Seovic  2021.10.19
 */
public class LockOwner
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for serialization interfaces
     */
    public LockOwner()
        {
        }

    /**
     * Construct {@code LockOwner} instance.
     *
     * @param member    the member
     * @param threadId  the thread ID
     */
    public LockOwner(Member member, long threadId)
        {
        m_memberId = member.getUuid();
        m_threadId = threadId;
        m_fClient  = member.isRemoteClient();
        }

    /**
     * Return the member UID.
     *
     * @return the member UID
     */
    public UUID getMemberId()
        {
        return m_memberId;
        }

    /**
     * Return the thread ID.
     *
     * @return the thread ID
     */
    public long getThreadId()
        {
        return m_threadId;
        }

    /**
     * Return {@code true} if this lock owner is a remote client (Extend or gRPC).
     *
     * @return {@code true} if this lock owner is a remote client (Extend or gRPC)
     */
    public boolean isClient()
        {
        return m_fClient;
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
        return m_threadId == lockOwner.m_threadId && m_memberId.equals(lockOwner.m_memberId) && m_fClient == lockOwner.m_fClient;
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_memberId, m_threadId, m_fClient);
        }

    @Override
    public String toString()
        {
        return "LockOwner{" +
               "memberId=" + m_memberId +
               ", threadId=" + m_threadId +
               ", client=" + m_fClient +
               '}';
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_memberId = ExternalizableHelper.readObject(in);
        m_threadId = in.readLong();
        m_fClient  = in.readBoolean();
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_memberId);
        out.writeLong(m_threadId);
        out.writeBoolean(m_fClient);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_memberId = in.readObject(1);
        m_threadId = in.readLong(2);
        m_fClient  = in.readBoolean(3);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(1, m_memberId);
        out.writeLong(2, m_threadId);
        out.writeBoolean(3, m_fClient);
        }

    // ---- data members ----------------------------------------------------

    /**
     * The member {@link UUID}.
     */
    private UUID m_memberId;

    /**
     * The thread ID.
     */
    private long m_threadId;

    /**
     * Flag indicating the member is a remote client.
     */
    private boolean m_fClient;
    }
