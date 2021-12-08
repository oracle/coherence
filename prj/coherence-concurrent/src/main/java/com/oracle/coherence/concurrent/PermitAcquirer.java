/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent;

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
 * The identity of a acquirer, represented by the UID of the member, and the ID
 * of a thread holding or attempting to acquire permit.
 *
 * @author Vaso Putica  2021.11.30
 */
public class PermitAcquirer
        implements ExternalizableLite, PortableObject
    {
    /**
     * Default constructor (necessary for the ExternalizableLite interface).
     */
    public PermitAcquirer()
        {
        }

    /**
     * Construct {@code PermitAcquirer} instance.
     *
     * @param memberId  the member UID
     * @param threadId  the thread ID
     */
    public PermitAcquirer(UID memberId, long threadId)
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
        PermitAcquirer acquirer = (PermitAcquirer) o;
        return f_threadId == acquirer.f_threadId && f_memberId.equals(acquirer.f_memberId);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(f_memberId, f_threadId);
        }

    @Override
    public String toString()
        {
        return "PermitAcquirer{" +
               "memberId=" + f_memberId +
               ", threadId=" + f_threadId +
               '}';
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        f_memberId = ExternalizableHelper.readObject(in);
        f_threadId = ExternalizableHelper.readLong(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, f_memberId);
        ExternalizableHelper.writeLong(out, f_threadId);
        }

    // ----- PortableObject interface -------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        f_memberId = in.readObject(0);
        f_threadId = in.readLong(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, f_memberId);
        out.writeLong(1, f_threadId);
        }

    // ---- data members ----------------------------------------------------

    private UID f_memberId;
    private long f_threadId;
    }
