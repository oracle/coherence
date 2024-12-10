/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.paged;

import com.tangosol.internal.net.queue.model.QueuePollResult;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.Converter;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * A base class for queue poll or peek entry processors.
 */
public abstract class BasePollPeekProcessor
        extends BasePagedQueueProcessor<QueuePollResult>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for POF
     */
    public BasePollPeekProcessor()
        {
        }

    /**
     * Create a new BasePollPeekProcessor with the specified action.
     *
     * @param fPoll    {@code true} if the action to perform is a poll, or {@code false} if the action is peek
     * @param version  the version of the bucket
     */
    protected BasePollPeekProcessor(boolean fPoll, QueueVersionInfo version)
        {
        m_fPoll   = fPoll;
        m_version = version;
        }

    // ----- AbstractProcessor methods --------------------------------------

    /**
     * Performs the required action against the targeted bucket.
     *
     * @param entry the entry in the bucket cache that this {@link BasePollPeekProcessor}
     *              has been invoked against
     *
     * @return the {@link QueuePollResult result} of the operation
     */
    @Override
    public final QueuePollResult process(InvocableMap.Entry<Integer,Bucket> entry)
        {
        BinaryEntry<Integer,Bucket> binaryEntry = (BinaryEntry<Integer,Bucket>) entry;

        if (!binaryEntry.isPresent())
            {
            BinaryEntry<Integer, QueueVersionInfo> versionEntry = getVersionBinaryEntry(binaryEntry);

            if (!versionEntry.isPresent())
                {
                return QueuePollResult.empty();
                }

            return QueuePollResult.nextPage();
            }

        Bucket bucket = entry.getValue();

        QueueVersionInfo version = bucket.getVersion();
        if (!isValidVersion(version))
            {
            return QueuePollResult.nextPage();
            }

        if (bucket.isEmpty())
            {
            removeEmptyBucket(bucket, binaryEntry);
            return QueuePollResult.nextPage();
            }

        QueuePollResult result = pollOrPeek(binaryEntry, bucket);

        binaryEntry.setValue(bucket);

        return result;
        }

    // ----- BasePollPeekProcessor implementation ---------------------------

    /**
     * Validate that this BasePollPeekProcessor can process the specified Bucket.
     *
     * @param version  the {@link QueueVersionInfo} for the bucket being polled or peeked
     *
     * @return true if this BasePollPeekProcessor can poll or peek at the specified bucket
     */
    protected abstract boolean isValidVersion(QueueVersionInfo version);

    /**
     * Obtain the {@link PagedQueueKey} for the first element to attempt
     * to poll or peek.
     *
     * @param bucket  the bucket being polled or peeked
     *
     * @return the next {@link PagedQueueKey} for the element to attempt
     *         to poll or peek
     */
    protected abstract PagedQueueKey firstQueueKey(Bucket bucket);

    /**
     * Obtain the next {@link PagedQueueKey} for the element to attempt
     * to poll or peek based on the values in the specified queueKey.
     *
     * @param bucket    the bucket being polled or peeked
     * @param queueKey  the {@link PagedQueueKey} to base the next key on
     *
     * @return the next {@link PagedQueueKey} for the element to attempt
     *         to poll or peek based on the values in the specified queueKey
     */
    protected abstract PagedQueueKey nextQueueKey(Bucket bucket, PagedQueueKey queueKey);

    /**
     * Update the Bucket due to the specified element being polled.
     *
     * @param bucket  the Bucket being polled
     * @param key     the key of the polled element
     */
    protected abstract void poll(Bucket bucket, PagedQueueKey key);

    /**
     * This method is called so that sub-classes can do processing
     * prior to removal of the empty bucket.
     *
     * @param bucket  the empty bucket that will be removed
     */
    protected abstract void notifyRemovingEmptyBucket(Bucket bucket);

    // ----- accessors  -----------------------------------------------------

    /**
     * Return {@code true} if the action that this {@link QueuePollPeekHeadProcessor}
     * will perform is a poll.
     */
    public boolean isPoll()
        {
        return m_fPoll;
        }

    /**
     * Set the bucket version that this processor will operate against.
     *
     * @param version  the bucket version that this processor will operate against
     */
    public void setVersion(QueueVersionInfo version)
        {
        m_version = version;
        }

    // ----- helper methods -------------------------------------------------

    protected void removeEmptyBucket(Bucket bucket, BinaryEntry<Integer,Bucket> bucketBinaryEntry)
        {
        notifyRemovingEmptyBucket(bucket);
        bucket.setAcceptingOffers(true);
        BinaryEntry<Integer,QueueVersionInfo> versionBinaryEntry = getVersionBinaryEntry(bucketBinaryEntry);
        versionBinaryEntry.setValue(bucket.getVersion());
        bucketBinaryEntry.remove(true);
        }

    /**
     * Poll or Peek at the next element in the bucket.
     *
     * @return the next element in the bucket.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected QueuePollResult pollOrPeek(BinaryEntry<Integer,Bucket> binaryEntry, Bucket bucket)
        {
        BackingMapManagerContext context           = binaryEntry.getContext();
        BackingMapContext        backingMapContext = binaryEntry.getBackingMapContext();
        String                   elementCacheName  = PagedQueueCacheNames.Elements.getCacheName(backingMapContext);
        BackingMapContext        elementMapContext = context.getBackingMapContext(elementCacheName);
        Converter                keyConverter      = context.getKeyToInternalConverter();
        PagedQueueKey            queueKey          = firstQueueKey(bucket);
        Binary                   binaryKey         = (Binary) keyConverter.convert(queueKey);
        BinaryEntry              elementEntry      = (BinaryEntry) elementMapContext.getBackingMapEntry(binaryKey);

        while (!elementEntry.isPresent())
            {
            queueKey = nextQueueKey(bucket, queueKey);
            if (queueKey == null)
                {
                bucket.markEmpty();
                return new QueuePollResult();
                }

            binaryKey = (Binary) keyConverter.convert(queueKey);
            elementEntry = (BinaryEntry) elementMapContext.getBackingMapEntry(binaryKey);
            bucket.setHead(queueKey.getElementId());
            binaryEntry.setValue(bucket);
            }

        Binary binElement = elementEntry.getBinaryValue();

        if (m_fPoll)
            {
            Binary binKey = elementEntry.getBinaryKey();
            long   nSize  = entrySize(binKey, binElement);
            bucket.decreaseBytesUsed(nSize);
            elementEntry.remove(false);
            poll(bucket, queueKey);
            }

        return new QueuePollResult(queueKey.getBucketId(), queueKey.getElementId(), binElement);
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public int getImplVersion()
        {
        return POF_IMPL_VERSION;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_fPoll   = in.readBoolean(0);
        m_version = in.readObject(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeBoolean(0, m_fPoll);
        out.writeObject(1, m_version);
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_fPoll   = in.readBoolean();
        m_version = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeBoolean(m_fPoll);
        ExternalizableHelper.writeObject(out, m_version);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The POF evolvable implementation version.
     */
    public static final int POF_IMPL_VERSION = 0;

    // ----- data members ---------------------------------------------------

    /**
     * {@code true} if the action to perform is a poll, or {@code false} if
     * the action is peek.
     */
    protected boolean m_fPoll;

    /** The version of the bucket to use */
    protected QueueVersionInfo m_version;
    }
