/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.topic.Publisher;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.LongArray;

import com.tangosol.util.SimpleLongArray;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A simple serializable implementation of {@link PublishResult}.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class SimplePublishResult
        extends AbstractEvolvable
        implements PublishResult, ExternalizableLite, EvolvablePortableObject
    {
    /**
     * Default constructor for serialization.
     */
    public SimplePublishResult()
        {
        }

    /**
     * Create a result.
     *
     * @param nChannel       the channel published to
     * @param cAccepted      the number of elements accepted by the server
     * @param publishStatus  the status of the publish request
     * @param errors         the array of errors that occurred
     * @param cRemaining     the remaining space
     * @param oRetryCookie   the cookie to use to re-try a request
     * @param status         the status of the request
     */
    public SimplePublishResult(int nChannel, int cAccepted, LongArray<Publisher.Status> publishStatus,
            LongArray<Throwable> errors, int cRemaining, Object oRetryCookie, Status status)
        {
        m_nChannel      = nChannel;
        m_cAccepted     = cAccepted;
        m_publishStatus = publishStatus;
        m_errors        = errors;
        m_cRemaining    = cRemaining;
        m_oRetryCookie  = oRetryCookie;
        m_status        = status;
        }

    @Override
    public Status getStatus()
        {
        return m_status;
        }

    @Override
    public LongArray<Throwable> getErrors()
        {
        return m_errors;
        }

    @Override
    public int getAcceptedCount()
        {
        return m_cAccepted;
        }

    @Override
    public int getChannelId()
        {
        return m_nChannel;
        }

    @Override
    public LongArray<Publisher.Status> getPublishStatus()
        {
        return m_publishStatus;
        }

    @Override
    public int getRemainingCapacity()
        {
        return m_cRemaining;
        }

    @Override
    public Object getRetryCookie()
        {
        return m_oRetryCookie;
        }

    @Override
    public int getImplVersion()
        {
        return POF_IMPL_VERSION;
        }

    @Override
    @SuppressWarnings("unchecked")
    public void readExternal(PofReader in) throws IOException
        {
        m_nChannel      = in.readInt(0);
        m_cAccepted     = in.readInt(1);
        m_publishStatus = in.readLongArray(2, new SimpleLongArray());
        m_errors        = in.readLongArray(3, new SimpleLongArray());
        m_cRemaining    = in.readInt(4);
        m_oRetryCookie  = in.readObject(5);
        m_status        = in.readObject(6);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeInt(0, m_nChannel);
        out.writeInt(1, m_cAccepted);
        out.writeLongArray(2, m_publishStatus);
        out.writeLongArray(3, m_errors);
        out.writeInt(4, m_cRemaining);
        out.writeObject(5, m_oRetryCookie);
        out.writeObject(6, m_status);
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_nChannel = in.readInt();
        m_cAccepted = in.readInt();
        m_publishStatus = ExternalizableHelper.readObject(in);
        m_errors = ExternalizableHelper.readObject(in);
        m_cRemaining = in.readInt();
        m_oRetryCookie = ExternalizableHelper.readObject(in);
        m_status = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeInt(m_nChannel);
        out.writeInt(m_cAccepted);
        ExternalizableHelper.writeObject(out, m_publishStatus);
        ExternalizableHelper.writeObject(out, m_errors);
        out.writeInt(m_cRemaining);
        ExternalizableHelper.writeObject(out, m_oRetryCookie);
        ExternalizableHelper.writeObject(out, m_status);
        }

    public void setRetryCookie(Object oRetryCookie)
        {
        m_oRetryCookie = oRetryCookie;
        }

    @Override
    public String toString()
        {
        return "SimplePublishResult{" +
                "m_nChannel=" + m_nChannel +
                ", m_cAccepted=" + m_cAccepted +
                ", m_publishStatus=" + m_publishStatus +
                ", m_errors=" + m_errors +
                ", m_cRemaining=" + m_cRemaining +
                ", m_oRetryCookie=" + m_oRetryCookie +
                ", m_status=" + m_status +
                '}';
        }

    // ----- constants ------------------------------------------------------

    /**
     * The POF implementation version.
     */
    public static final int POF_IMPL_VERSION = 0;
    
    // ----- data members ---------------------------------------------------

    /**
     * The channel published to.
     */
    private int m_nChannel;
    
    /**
     * The number of elements accepted by the server.
     */
    private int m_cAccepted;
    
    /**
     * The status of the publish request.
     */
    private LongArray<Publisher.Status> m_publishStatus;
    
    /**
     * The array of errors that occurred.
     */
    private LongArray<Throwable> m_errors;
    
    /**
     * The remaining space.
     */
    private int m_cRemaining;
    
    /**
     * The cookie to use to re-try a request.
     */
    private Object m_oRetryCookie;
    
    /**
     * The status of the request.
     */
    private Status m_status;
    }
