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
 * tail id if the current value is the same as
 * an expected value and the version of the tail is the
 * same as the expected version.
 * <p/>
 * If after incrementing the tail id would be equal to
 * the head id then an IllegalStateException will be thrown
 * as the tail cannot be incremented to equal the head id
 */
public class TailIncrementProcessor
        extends AbstractProcessor<String, QueueInfo, QueueInfo>
        implements ExternalizableLite, PortableObject
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for POF
     */
    public TailIncrementProcessor()
        {
        }

    /**
     * Create a new TailIncrementProcessor.
     *
     * @param expectedValue   the expected value to compare the current
     *                        tail id value to
     * @param expectedVersion the expected version number of the tail
     *                        of the queue
     * @throws IllegalStateException if after incrementing the tail
     *                               value would be equal to the head value.
     */
    public TailIncrementProcessor(int expectedValue, long expectedVersion)
        {
        this(expectedValue, expectedVersion, true);
        }

    /**
     * Create a new TailIncrementProcessor.
     *
     * @param expectedValue   the expected value to compare the current
     *                        tail id value to
     * @param expectedVersion the expected version number of the tail
     *                        of the queue
     * @param updateValue     flag to indicate whether the QueueInfo is updated
     *                        with the new head value
     * @throws IllegalStateException if after incrementing the tail
     *                               value would be equal to the head value.
     */
    public TailIncrementProcessor(int expectedValue, long expectedVersion, boolean updateValue)
        {
        m_expectedValue   = expectedValue;
        m_expectedVersion = expectedVersion;
        m_fUpdateValue    = updateValue;
        }


    // ----- AbstractProcessor methods --------------------------------------

    /**
     * This method will increment the tail bucket ID of the QueueInfo
     * if it is equal to the expected value and the tail version is
     * equal to the expected version.
     * <p>
     * If the expected value or expected version do not match the current value
     * then the tail ID is left unchanged.
     *
     * @param entry the cache entry containing the {@link QueueInfo} to
     *              be updated.
     * @return the {@link QueueInfo} from the entry
     */
    @Override
    public QueueInfo process(InvocableMap.Entry<String, QueueInfo> entry)
        {
        QueueInfo info = entry.getValue();

        if (info.isQueueFull())
            {
            return info;
            }

        QueueVersionInfo versionInfo = info.getVersion();
        long             tailVersion = versionInfo.getTailOfferVersion();
        int              tailValue   = info.getTailBucketId();

        if (tailValue != m_expectedValue || tailVersion != m_expectedVersion)
            {
            return info;
            }

        if (tailValue == info.getMaxBucketId())
            {
            tailValue = 0;
            }
        else
            {
            tailValue = tailValue + 1;
            }

        if (info.getHeadBucketId() == tailValue)
            {
            info.setQueueFull(true);
            if (m_fUpdateValue)
                {
                entry.setValue(info);
                }
            return info;
            }

        if (tailValue == 0)
            {
            versionInfo.incrementTailOfferVersion();
            }

        info.setTailBucketId(tailValue);
        if (m_fUpdateValue)
            {
            entry.setValue(info);
            }

        return info;
        }

    // ----- accessor methods -----------------------------------------------

    public void setUpdateValue(boolean fUpdateValue)
        {
        m_fUpdateValue = fUpdateValue;
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_expectedValue   = in.readInt(0);
        m_expectedVersion = in.readLong(1);
        m_fUpdateValue    = in.readBoolean(2);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeInt(0, m_expectedValue);
        out.writeLong(1, m_expectedVersion);
        out.writeBoolean(2, m_fUpdateValue);
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_expectedValue   = ExternalizableHelper.readInt(in);
        m_expectedVersion = ExternalizableHelper.readLong(in);
        m_fUpdateValue    = in.readBoolean();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeInt(out, m_expectedValue);
        ExternalizableHelper.writeLong(out, m_expectedVersion);
        out.writeBoolean(m_fUpdateValue);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The expected value of the tail id field in {@link QueueInfo}.
     */
    protected int m_expectedValue;

    /**
     * The expected value for the tail version field in the {@link QueueInfo}.
     */
    protected long m_expectedVersion;

    /**
     * Flag indicating whether to actually update the QueueInfo value with the decremented Head ID.
     */
    protected boolean m_fUpdateValue;
    }
