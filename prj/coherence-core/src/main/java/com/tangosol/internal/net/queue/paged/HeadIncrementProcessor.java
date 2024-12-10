/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.paged;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;

import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
 * This entry processor increments a {@link QueueInfo}
 * head id if the current value is the same as
 * an expected value.
 * <p/>
 * If the current head value is equal to the tail
 * value then an IllegalStateException will be thrown
 * as the head cannot be incremented past the tail
 */
public class HeadIncrementProcessor
        extends AbstractProcessor<String, QueueInfo, QueueInfo>
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for POF
     */
    public HeadIncrementProcessor()
        {
        }

    /**
     * Create a new HeadIncrementProcessor.
     *
     * @param expectedValue   the expected value to compare the current
     *                        head id value to.
     * @param expectedVersion the expected version of the tail of the queue
     * @throws IllegalStateException if before incrementing the head
     *                               value is equal to the tail value.
     */
    public HeadIncrementProcessor(int expectedValue, QueueVersionInfo expectedVersion)
        {
        this(expectedValue, expectedVersion, true);
        }

    /**
     * Create a new HeadIncrementProcessor.
     *
     * @param expectedValue   the expected value to compare the current
     *                        head id value to.
     * @param expectedVersion the expected version of the tail of the queue
     * @throws IllegalStateException if before incrementing the head
     *                               value is equal to the tail value.
     */
    public HeadIncrementProcessor(int expectedValue, QueueVersionInfo expectedVersion, boolean updateValue)
        {
        m_expectedValue   = expectedValue;
        m_expectedVersion = expectedVersion;
        m_fUpdateValue    = updateValue;
        }

    // ----- AbstractProcessor methods --------------------------------------

    @Override
    public QueueInfo process(InvocableMap.Entry<String, QueueInfo> entry)
        {
        QueueInfo info = entry.getValue();

        int  headValue   = info.getHeadBucketId();
        long headVersion = info.getVersion().getHeadPollVersion();
        long expected    = m_expectedVersion.getHeadPollVersion();

        if (headValue == m_expectedValue && headVersion <= expected)
            {
            if (headValue == info.getTailBucketId())
                {
                throw new IllegalStateException("Head cannot be incremented past Tail");
                }

            if (headValue == info.getMaxBucketId())
                {
                headValue = 0;
                info.getVersion().incrementHeadPollVersion();
                }
            else
                {
                headValue = headValue + 1;
                }

            info.setHeadBucketId(headValue);
            info.setQueueFull(false);

            if (m_fUpdateValue)
                {
                entry.setValue(info);
                }
            }

        return info;
        }

    // ----- accessors ------------------------------------------------------

    public void setUpdateValue(boolean fUpdateValue)
        {
        m_fUpdateValue = fUpdateValue;
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_expectedValue   = in.readInt(0);
        m_expectedVersion = in.readObject(1);
        m_fUpdateValue    = in.readBoolean(2);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeInt(0, m_expectedValue);
        out.writeObject(1, m_expectedVersion);
        out.writeBoolean(2, m_fUpdateValue);
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_expectedValue = ExternalizableHelper.readInt(in);
        m_expectedVersion = ExternalizableHelper.readObject(in);
        m_fUpdateValue = in.readBoolean();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeInt(out, m_expectedValue);
        ExternalizableHelper.writeObject(out, m_expectedVersion);
        out.writeBoolean(m_fUpdateValue);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The expected value of the head id field in {@link QueueInfo}
     */
    protected int m_expectedValue;

    /**
     * The expected value for the tail version field in the {@link QueueInfo}.
     */
    protected QueueVersionInfo m_expectedVersion;

    /**
     * Flag indicating whether to actually update the QueueInfo value with the incremented Head ID.
     */
    protected boolean m_fUpdateValue;
    }
