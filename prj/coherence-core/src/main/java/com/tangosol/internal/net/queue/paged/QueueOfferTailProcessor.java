/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.paged;

import com.tangosol.internal.net.queue.model.QueueOfferResult;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
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
import java.util.Collection;
import java.util.Objects;

import static com.tangosol.internal.net.queue.paged.Utils.unsignedIncrement;

/**
 * This implementation of an {@link InvocableMap.EntryProcessor} is for adding (or offering)
 * an element to the tail of a queue. Elements of a distributed queue are stored in buckets and this
 * {@link QueueOfferTailProcessor} runs against a specific bucket id (which is an {@link Integer} value).
 * <p/>
 * This {@link QueueOfferTailProcessor} is invoked against a bucket id on the buckets cache and then
 * uses Coherence partition level transactions to add the elements to the elements cache. This
 * ensures that only a single process can put elements into the queue at any one time hence
 * synchronising queue updates and controlling creation of new buckets.
 * <p/>
 * The results returned by invoking this {@link QueueOfferTailProcessor} will be an empty {@link Collection}
 * if all the elements were added to the queue or a {@link Collection} of any remaining elements
 * that were not added to the bucket, usually due to the bucket becoming full.
 * <strong>Note:</strong> any un-added elements returned to the caller will be returned in
 * {@link Binary} form.
 * <p/>
 * When this {@link QueueOfferTailProcessor} is serialized on the "client" process to send to the
 * storage node containing the bucket the elements to be added are serialized to Binary. On the
 * receiving storage node when this {@link QueueOfferTailProcessor} is deserialized the elements
 * as not deserialized and are kept in their Binary form to make the process a little more
 * efficient. This also means that the {@link Object} form of the elements is only used by
 * the calling process so there it is not necessary to have a Java Class for the element
 * on the classpath of the storage nodes.
 */
@SuppressWarnings("rawtypes")
public class QueueOfferTailProcessor
        extends BasePagedQueueProcessor<QueueOfferResult>
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for serialization
     */
    public QueueOfferTailProcessor()
        {
        }

    /**
     * Create a {@link QueueOfferTailProcessor} to put the specified elements into
     * the queue.
     *
     * @param binElement      the element to be added to the queue
     * @param version         the version of the bucket
     * @param nMaxBucketSize  the maximum number of elements that can be held by a bucket
     */
    public QueueOfferTailProcessor(Binary binElement, QueueVersionInfo version, int nMaxBucketSize)
        {
        m_binElement     = Objects.requireNonNull(binElement);
        m_bucketVersion  = version;
        m_nMaxBucketSize = nMaxBucketSize;
        }

    // ----- InvocableMap.EntryProcessor implementation ---------------------

    /**
     * This method will add the elements to the elements cache using Coherence
     * partition level transactions.
     *
     * @param entry the BinaryEntry containing the {@link Bucket} that the elements
     *              are being added to.
     *
     * @return a {@link Collection} containing any elements that could not be added to
     *         the bucket or an empty {@link Collection} if all the elements were added.
     */
    @SuppressWarnings("unchecked")
    @Override
    public final QueueOfferResult process(InvocableMap.Entry<Integer,Bucket> entry)
        {
        BinaryEntry<Integer,Bucket>  binaryEntry = entry.asBinaryEntry();
        Bucket                       bucket      = ensureBucket(binaryEntry, m_nMaxBucketSize, m_bucketVersion);

        if (!bucket.isAcceptingOffers() || !isValidVersion(bucket))
            {
            return new QueueOfferResult(0, QueueOfferResult.RESULT_FAILED_RETRY);
            }

        BackingMapManagerContext context            = binaryEntry.getContext();
        BackingMapContext        backingMapContext  = binaryEntry.getBackingMapContext();
        String                   sElementCacheName  = PagedQueueCacheNames.Elements.getCacheName(backingMapContext);
        BackingMapContext        elementMapContext  = context.getBackingMapContext(sElementCacheName);
        int                      bucketId           = bucket.getId();
        int                      headId             = bucket.getHead();
        int                      tailId             = unsignedIncrement(bucket.getTail());
        int                      nResult;

        if (headId != tailId)
            {
            Converter keyConverter     = context.getKeyToInternalConverter();
            PagedQueueKey key          = new PagedQueueKey(bucketId, tailId);
            Binary        binKey       = (Binary) keyConverter.convert(key);
            BinaryEntry   elementEntry = (BinaryEntry) elementMapContext.getBackingMapEntry(binKey);
            long          nSize        = entrySize(binKey, m_binElement);

            if (bucket.increaseBytesUsed(nSize))
                {
                elementEntry.updateBinaryValue(m_binElement);
                bucket.setTail(tailId);
                nResult = QueueOfferResult.RESULT_SUCCESS;
                }
            else
                {
                bucket.setAcceptingOffers(false);
                nResult = QueueOfferResult.RESULT_FAILED_RETRY;
                }
            }
        else
            {
            bucket.setAcceptingOffers(false);
            nResult = QueueOfferResult.RESULT_FAILED_RETRY;
            }

        entry.setValue(bucket);
        BinaryEntry<Integer,QueueVersionInfo> versionBinaryEntry = getVersionBinaryEntry(binaryEntry);
        if (versionBinaryEntry.isPresent())
            {
            versionBinaryEntry.remove(true);
            }

        return new QueueOfferResult(bucket.m_id, bucket.m_tail, nResult);
        }

    // ----- helper methods -------------------------------------------------

    protected boolean isValidVersion(Bucket bucket)
        {
        QueueVersionInfo versionInfo = bucket.getVersion();
        return versionInfo.getTailOfferVersion() == m_bucketVersion.getTailOfferVersion();
        }

    // ----- PortableObject implementation ----------------------------------

    @Override
    public int getImplVersion()
        {
        return POF_IMPL_VERSION;
        }

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_nMaxBucketSize = in.readInt(0);
        m_bucketVersion  = in.readObject(1);
        m_binElement     = in.readBinary(2);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeInt(0, m_nMaxBucketSize);
        out.writeObject(1, m_bucketVersion);
        out.writeBinary(2, m_binElement);
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_nMaxBucketSize = ExternalizableHelper.readInt(in);
        m_bucketVersion  = ExternalizableHelper.readObject(in);
        m_binElement     = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeInt(out, m_nMaxBucketSize);
        ExternalizableHelper.writeObject(out, m_bucketVersion);
        ExternalizableHelper.writeObject(out, m_binElement);
        }


    // ----- constants ------------------------------------------------------

    /**
     * The POF evolvable implementation version.
     */
    public static final int POF_IMPL_VERSION = 0;

    // ----- data members ---------------------------------------------------

    /**
     * The element to be added to the bucket
     */
    protected Binary m_binElement;

    /**
     * The maximum size of the bucket elements
     */
    protected int m_nMaxBucketSize;

    /**
     * The version of the bucket
     */
    protected QueueVersionInfo m_bucketVersion;
    }
