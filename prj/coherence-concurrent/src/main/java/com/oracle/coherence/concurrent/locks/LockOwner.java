/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.UID;

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
     * @param memberId  the member UID
     * @param threadId  the thread ID
     */
    public LockOwner(UID memberId, long threadId)
        {
        m_memberId = memberId;
        m_threadId = threadId;
        }

    /**
     * Return the member UID.
     *
     * @return the member UID
     */
    public UID getMemberId()
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
        return m_threadId == lockOwner.m_threadId && m_memberId.equals(lockOwner.m_memberId);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_memberId, m_threadId);
        }

    @Override
    public String toString()
        {
        return "LockOwner{" +
               "memberId=" + m_memberId +
               ", threadId=" + m_threadId +
               '}';
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_memberId = ExternalizableHelper.readObject(in);
        m_threadId = in.readLong();
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_memberId);
        out.writeLong(m_threadId);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_memberId = in.readObject(1);
        m_threadId = in.readLong(2);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(1, m_memberId);
        out.writeLong(2, m_threadId);
        }

    // ---- data members ----------------------------------------------------

    private UID m_memberId;
    private long m_threadId;
    }
