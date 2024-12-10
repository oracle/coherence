/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent;

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
 * The identity of an acquirer, represented by the UUID of the member, and the ID
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
     * @param member    the member
     * @param threadId  the thread ID
     */
    public PermitAcquirer(Member member, long threadId)
        {
        f_memberId = member.getUuid();
        f_threadId = threadId;
        f_client   = member.isRemoteClient();
        }

    /**
     * Return the member UUID.
     *
     * @return the member UUID
     */
    public UUID getMemberId()
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

    /**
     * Return {@code true} if this permit acquirer is a remote client (Extend or gRPC).
     *
     * @return {@code true} if this permit acquirer is a remote client (Extend or gRPC)
     */
    public boolean isClient()
        {
        return f_client;
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
        return f_threadId == acquirer.f_threadId && f_memberId.equals(acquirer.f_memberId) && f_client == acquirer.f_client;
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(f_memberId, f_threadId, f_client);
        }

    @Override
    public String toString()
        {
        return "PermitAcquirer{" +
               "memberId=" + f_memberId +
               ", threadId=" + f_threadId +
               ", client=" + f_client +
               '}';
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        f_memberId = ExternalizableHelper.readObject(in);
        f_threadId = ExternalizableHelper.readLong(in);
        f_client   = in.readBoolean();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, f_memberId);
        ExternalizableHelper.writeLong(out, f_threadId);
        out.writeBoolean(f_client);
        }

    // ----- PortableObject interface -------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        f_memberId = in.readObject(0);
        f_threadId = in.readLong(1);
        f_client   = in.readBoolean(2);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, f_memberId);
        out.writeLong(1, f_threadId);
        out.writeBoolean(2, f_client);
        }

    // ---- data members ----------------------------------------------------

    /**
     * The member {@link UUID}.
     */
    private UUID f_memberId;

    /**
     * The thread ID.
     */
    private long f_threadId;

    /**
     * Flag indicating the member is a remote client.
     */
    private boolean f_client;
    }
