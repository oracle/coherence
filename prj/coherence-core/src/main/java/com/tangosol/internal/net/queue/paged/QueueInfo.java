/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.paged;

import com.tangosol.internal.net.queue.PagedQueue;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

/**
 * A QueueInfo holds information about a specific
 * {@link com.tangosol.net.NamedQueue}
 */
public class QueueInfo
        extends AbstractEvolvable
        implements ExternalizableLite, EvolvablePortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for POF
     */
    public QueueInfo()
        {
        this(null);
        }

    /**
     * Creates a new QueueInfo with the given name
     */
    public QueueInfo(String sQueueName)
        {
        this(sQueueName, 0, 0, new QueueVersionInfo());
        }

    /**
     * Creates a new QueueInfo with the given name, head, tail and version information
     */
    public QueueInfo(String sQueueName, int nHead, int nTail, QueueVersionInfo version)
        {
        m_sQueueName   = sQueueName;
        m_headBucketId = nHead;
        m_tailBucketId = nTail;
        m_version      = version;
        }

    // ----- QueueInfo methods ------------------------------------------------

    /**
     * Obtain the name of the queue that this QueueInfo holds information for.
     *
     * @return the name of the queue that this QueueInfo holds information for.
     */
    public String getsQueueName()
        {
        return m_sQueueName;
        }

    /**
     * Returns the id of the bucket currently at the head of the queue.
     *
     * @return the current head bucket id.
     */
    public int getHeadBucketId()
        {
        return m_headBucketId;
        }

    /**
     * Set the id of the bucket that is currently at the head of the queue.
     *
     * @param headBucketId the head bucket id.
     */
    public void setHeadBucketId(int headBucketId)
        {
        m_headBucketId = headBucketId;
        }

    /**
     * Returns the id of the bucket currently at the tail of the queue.
     *
     * @return the current tail bucket id.
     */
    public int getTailBucketId()
        {
        return m_tailBucketId;
        }

    /**
     * Set the id of the bucket that is currently at the tail of the queue.
     *
     * @param tailBucketId the tail bucket id.
     */
    public void setTailBucketId(int tailBucketId)
        {
        m_tailBucketId = tailBucketId;
        }

    /**
     * Get the maximum number of elements that buckets in this queue
     * can contain.
     *
     * @return the maximum number of elements that buckets in this queue
     *         can contain.
     */
    public int getBucketSize()
        {
        return m_bucketSize;
        }

    /**
     * Set the maximum number of elements that buckets in this queue
     * can contain.
     *
     * @param bucketSize the maximum number of elements that buckets in this queue
     *                   can contain.
     */
    public void setBucketSize(int bucketSize)
        {
        m_bucketSize = bucketSize;
        }

    /**
     * Get the maximum bucket ID for this queue. This can be used to determine
     * the Queue's capacity by multiplying this value by the bucket size value.
     *
     * @return the maximum bucket DI for this Queue.
     */
    public int getMaxBucketId()
        {
        return m_maxBucketId;
        }

    /**
     * Set the maximum bucket ID for this queue. This will set the Queue's
     * capacity to the be this value multiplied by the bucket size value.
     *
     * @param maxBucketId the maximum bucket ID for this queue
     */
    public void setMaxBucketId(int maxBucketId)
        {
        m_maxBucketId = maxBucketId;
        }

    /**
     * Obtain the {@link QueueVersionInfo} for the Queue.
     *
     * @return the {@link QueueVersionInfo} for the Queue
     */
    public QueueVersionInfo getVersion()
        {
        return m_version;
        }

    /**
     * Returns true if the queue is currently full.
     *
     * @return true if the queue is currently full
     */
    public boolean isQueueFull()
        {
        return m_fQueueFull;
        }

    /**
     * Set the flag indicating whether the queue is full.
     *
     * @param fQueueFull  true if the queue is full, false if the queue is not full
     */
    public void setQueueFull(boolean fQueueFull)
        {
        m_fQueueFull = fQueueFull;
        }

    // ----- object methods -------------------------------------------------

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

        QueueInfo queueInfo = (QueueInfo) o;

        return Objects.equals(m_sQueueName, queueInfo.m_sQueueName);
        }

    @Override
    public int hashCode()
        {
        return m_sQueueName != null
               ? m_sQueueName.hashCode()
               : 0;
        }

    @Override
    public String toString()
        {
        return "QueueInfo[" + "name='" + m_sQueueName + '\'' + ", head=" + m_headBucketId + ", tail=" + m_tailBucketId
               + ", maxBucketID=" + m_maxBucketId + ", bucketSize=" + m_bucketSize + ", version=" + m_version + ']';
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public int getImplVersion()
        {
        return POF_VERSION;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_sQueueName   = in.readString(0);
        m_headBucketId = in.readInt(1);
        m_tailBucketId = in.readInt(2);
        m_bucketSize   = in.readInt(3);
        m_maxBucketId  = in.readInt(4);
        m_version      = in.readObject(5);
        m_fQueueFull   = in.readBoolean(6);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, m_sQueueName);
        out.writeInt(1, m_headBucketId);
        out.writeInt(2, m_tailBucketId);
        out.writeInt(3, m_bucketSize);
        out.writeInt(4, m_maxBucketId);
        out.writeObject(5, m_version);
        out.writeBoolean(6, m_fQueueFull);
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_sQueueName   = ExternalizableHelper.readSafeUTF(in);
        m_headBucketId = ExternalizableHelper.readInt(in);
        m_tailBucketId = ExternalizableHelper.readInt(in);
        m_bucketSize   = ExternalizableHelper.readInt(in);
        m_maxBucketId  = ExternalizableHelper.readInt(in);
        m_version      = ExternalizableHelper.readObject(in);
        m_fQueueFull   = in.readBoolean();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeSafeUTF(out, m_sQueueName);
        ExternalizableHelper.writeInt(out, m_headBucketId);
        ExternalizableHelper.writeInt(out, m_tailBucketId);
        ExternalizableHelper.writeInt(out, m_bucketSize);
        ExternalizableHelper.writeInt(out, m_maxBucketId);
        ExternalizableHelper.writeObject(out, m_version);
        out.writeBoolean(m_fQueueFull);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The POF evolvable implementation version.
     */
    public static final int POF_VERSION = 0;

    // ----- data members ---------------------------------------------------

    /**
     * The name of the Queue
     */
    protected String m_sQueueName;

    /**
     * The ID of the current head bucket
     */
    protected int m_headBucketId = 0;

    /**
     * The ID of the current tail bucket
     */
    protected int m_tailBucketId = 0;

    /**
     * The maximum number of elements that buckets
     * in this queue can contain.
     */
    protected int m_bucketSize = PagedQueue.DEFAULT_PAGE_CAPACITY_BYTES;

    /**
     * The maximum number of buckets that this queue can contain;
     */
    protected int m_maxBucketId = PagedQueue.DEFAULT_MAX_BUCKET_ID;

    /**
     * The version number for the head of the queue.
     */
    protected QueueVersionInfo m_version;

    /**
     * A flag indicating whether the Queue is full (i.e. the tail cannot
     * be incremented any further as it would hit the head).
     */
    protected boolean m_fQueueFull = false;
    }
