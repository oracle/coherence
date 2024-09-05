/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.paged;

import com.tangosol.internal.net.queue.BaseBinaryNamedMapQueue;
import com.tangosol.internal.net.queue.PagedQueue;
import com.tangosol.internal.net.queue.model.QueueOfferResult;
import com.tangosol.internal.net.queue.model.QueuePollResult;
import com.tangosol.io.Serializer;
import com.tangosol.net.CacheService;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedMap;
import com.tangosol.net.NamedQueue;
import com.tangosol.net.Session;
import com.tangosol.net.options.WithClassLoader;
import com.tangosol.util.Binary;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.NullImplementation;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A {@link NamedQueue} implementation that stores data distributed over
 * a cluster in pages.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class BinaryPagedNamedQueue
        extends BaseBinaryNamedMapQueue
        implements PagedQueue<Binary>
    {
    /**
     * Create a {@link BinaryPagedNamedQueue}.
     *
     * @param sName    the name of the queue
     * @param session  the {@link Session} to obtain the underlying binary pass-thru cache
     */
    public BinaryPagedNamedQueue(String sName, Session session)
        {
        this(sName, session.getCache(sName, WithClassLoader.nullImplementation()));
        }

    /**
     * Create a {@link BinaryPagedNamedQueue}.
     *
     * @param sName  the name of the queue
     * @param eccf   the {@link ExtensibleConfigurableCacheFactory} to obtain the underlying binary pass-thru cache
     */
    public BinaryPagedNamedQueue(String sName, ExtensibleConfigurableCacheFactory eccf)
        {
        this(sName, eccf.ensureCache(sName, NullImplementation.getClassLoader()));
        }

    /**
     * Create a {@link BinaryPagedNamedQueue}.
     *
     * @param sName  the name of the queue
     * @param cache  the underlying binary pass-thru cache
     */
    public BinaryPagedNamedQueue(String sName, NamedCache<Binary, Binary> cache)
        {
        super(sName, cache);

        CacheService cacheService  = cache.getCacheService();

        m_elementCache   = cache;
        m_bucketCache    = PagedQueueCacheNames.Buckets.ensureBinaryMap(sName, cacheService);
        m_versionCache   = PagedQueueCacheNames.Version.ensureBinaryMap(sName, cacheService);
        m_queueInfoCache = PagedQueueCacheNames.Info.ensureBinaryMap(sName, cacheService);

        m_queueInfo = m_queueInfoCache.invoke(sName, instantateInitialiseQueueInfoProcessor());

        m_elementCache.addIndex(PagedQueueKey.BUCKET_ID_EXTRACTOR, true, null);
        }

    // ----- BaseNamedCacheQueue methods ------------------------------------

    @Override
    public void clear()
        {
        m_bucketCache.invokeAll((InvocableMap.EntryProcessor) new ClearQueueProcessor());
        }

    @Override
    public void release()
        {
        super.release();
        release(m_bucketCache);
        release(m_queueInfoCache);
        release(m_versionCache);
        }

    @Override
    public void destroy()
        {
        super.destroy();
        destroy(m_bucketCache);
        destroy(m_queueInfoCache);
        destroy(m_versionCache);
        }

    @Override
    public Iterator<Binary> iterator()
        {
        if (size() == 0)
            {
            return Collections.emptyIterator();
            }

        return new BinaryQueueIterator(this, m_queueInfo.getMaxBucketId());
        }

    @Override
    protected QueueOfferResult offerToTailInternal(Binary binary)
        {
        if (binary == null)
            {
            throw new NullPointerException("Null elements are not supported");
            }

        int                         tailBucketId = m_queueInfo.getTailBucketId();
        InvocableMap.EntryProcessor processor    = instantiateTailOfferProcessor(binary, m_queueInfo);
        Binary                      binKey       = m_converterKeyToInternal.convert(tailBucketId);
        Binary                      binResult    = (Binary) m_bucketCache.invoke(binKey, processor);
        QueueOfferResult            result       = (QueueOfferResult) m_converterValueFromInternal.convert(binResult);

        while (result.getResult() == QueueOfferResult.RESULT_FAILED_RETRY)
            {
            long                   version     = m_queueInfo.getVersion().getTailOfferVersion();
            TailIncrementProcessor incrementor = new TailIncrementProcessor(tailBucketId, version);
            m_queueInfo = m_queueInfoCache.invoke(m_sName, incrementor);
            if (m_queueInfo.isQueueFull())
                {
                //noinspection ResultOfMethodCallIgnored
                peek();
                m_queueInfo = m_queueInfoCache.invoke(m_sName, incrementor);
                if (m_queueInfo.isQueueFull())
                    {
                    return new QueueOfferResult(0, QueueOfferResult.RESULT_FAILED_CAPACITY);
                    }
                }
            tailBucketId = m_queueInfo.getTailBucketId();
            binKey       = m_converterKeyToInternal.convert(tailBucketId);
            binResult    = (Binary) m_bucketCache.invoke(binKey, processor);
            result       = (QueueOfferResult) m_converterValueFromInternal.convert(binResult);
            }

        return result;
        }

    @Override
    protected QueuePollResult pollFromHeadInternal()
        {
        com.tangosol.util.Binary binary = pollOrPeekHead(true);
        return toResult(binary);
        }

    @Override
    protected QueuePollResult peekAtHeadInternal()
        {
        com.tangosol.util.Binary binary = pollOrPeekHead(false);
        return toResult(binary);
        }

    protected QueuePollResult toResult(com.tangosol.util.Binary binary)
        {
        return new QueuePollResult(1, binary);
        }

    protected com.tangosol.util.Binary pollOrPeekHead(boolean fPoll)
        {
        if (m_elementCache.isEmpty())
            {
            return null;
            }

        int                         headId    = m_queueInfo.getHeadBucketId();
        QueueVersionInfo            version   = m_queueInfo.getVersion();
        QueuePollPeekHeadProcessor  processor = instantiatePollPeekHeadProcessor(fPoll, version);
        Binary                      binKey    = m_converterKeyToInternal.convert(headId);
        Binary                      binResult = (Binary) m_bucketCache.invoke(binKey, (InvocableMap.EntryProcessor) processor);
        QueuePollResult             result    = (QueuePollResult) m_converterValueFromInternal.convert(binResult);

        while (result.getId() == QueuePollResult.RESULT_POLL_NEXT_PAGE && !m_elementCache.isEmpty())
            {
            HeadIncrementProcessor incrementor = new HeadIncrementProcessor(headId, version);
            m_queueInfo = m_queueInfoCache.invoke(m_sName, incrementor);
            version     = m_queueInfo.getVersion();
            headId      = m_queueInfo.getHeadBucketId();
            binKey      = m_converterKeyToInternal.convert(headId);

            processor.setVersion(version);
            binResult = (Binary) m_bucketCache.invoke(binKey, (InvocableMap.EntryProcessor) processor);
            result    = (QueuePollResult) m_converterValueFromInternal.convert(binResult);
            }

        return result.getBinaryElement();
        }

    // ----- helper methods -------------------------------------------------

    protected InitialiseQueueInfoProcessor instantateInitialiseQueueInfoProcessor()
        {
        return InitialiseQueueInfoProcessor.INSTANCE;
        }

    protected QueueOfferTailProcessor instantiateTailOfferProcessor(com.tangosol.util.Binary binElement, QueueInfo queueInfo)
        {
        QueueVersionInfo version    = queueInfo.getVersion();
        int              bucketSize = queueInfo.getBucketSize();
        return new QueueOfferTailProcessor(binElement, version, bucketSize);
        }

    protected QueuePollPeekHeadProcessor instantiatePollPeekHeadProcessor(boolean fPoll, QueueVersionInfo version)
        {
        return new QueuePollPeekHeadProcessor(fPoll, version);
        }

    /**
     * Returns an {@link Iterator} that iterates over the elements in the bucket with the specified id. If there is no
     * bucket with the specified id then null is returned.
     *
     * @param bucketId   the id of the bucket to get an {@link Iterator} for
     * @param fHeadFirst flag to indicate whether the elements of the bucket are returned head first or tail first.
     *
     * @return an {@link Iterator} over the contents of the specified bucket or null if the bucket does not exist
     */
    protected Iterator<com.tangosol.util.Binary> peekAtBucket(int bucketId, boolean fHeadFirst)
        {
        Binary                       binKey     = m_converterKeyToInternal.convert(bucketId);
        InvocableMap.EntryAggregator aggregator = new PeekWholeBucketAggregator<>(fHeadFirst);
        Binary                       binResult  = (Binary) m_bucketCache.aggregate(Collections.singleton(binKey), aggregator);

        if (binResult == null)
            {
            return null;
            }

        List<com.tangosol.util.Binary> results = (List<Binary>) m_converterValueFromInternal.convert(binResult);
        if (results == null || results.isEmpty())
            {
            return null;
            }

        return results.iterator();
        }

    /**
     * Returns the id of the current head bucket. This is a transient value as by the time this method returns the head
     * bucket could have changed due to another thread or process removing the last element from the current head
     * bucket.
     *
     * @return the id of the current head bucket
     */
    protected int refreshHeadBucketId()
        {
        m_queueInfo = m_queueInfoCache.invoke(m_sName, instantateInitialiseQueueInfoProcessor());
        return m_queueInfo.getHeadBucketId();
        }

    /**
     * Returns the id of the current tail bucket. This is a transient value as by the time this method returns the tail
     * bucket could have changed due to another thread or process adding an element to the tail bucket that fills the
     * bucket and hence a new tail bucket is created.
     *
     * @return the id of the current tail bucket
     */
    protected int refreshTailBucketId()
        {
        m_queueInfo = m_queueInfoCache.invoke(m_sName, instantateInitialiseQueueInfoProcessor());
        return m_queueInfo.getTailBucketId();
        }

    /**
     * Returns the id of the next bucket above the specified bucket or -1 if there is no higher bucket. This is a
     * transient value as by the time this method returns the next bucket could have changed due to another thread or
     * process removing the last element from the that bucket.
     *
     * @return the id of the next bucket or -1 if there is no next bucket
     */
    protected int findNextBucketId(int bucketId)
        {
        if (bucketId == m_queueInfo.getMaxBucketId())
            {
            bucketId = 0;
            }
        else
            {
            bucketId++;
            }

        return isValidBucketId(bucketId) ? bucketId : -1;
        }

    /**
     * Returns the id of the previous bucket below the specified bucket or -1 if there is no lower bucket. This is a
     * transient value as by the time this method returns the previous bucket could have changed due to another thread
     * or process changing the queue
     *
     * @return the id of the next bucket or -1 if there is no next bucket
     */
    protected int findPreviousBucketId(int bucketId)
        {

        if (bucketId == 0)
            {
            bucketId = m_queueInfo.getMaxBucketId();
            }
        else
            {
            bucketId--;
            }

        return isValidBucketId(bucketId) ? bucketId : -1;
        }

    /**
     * Returns true if the specified bucket ID is valid for the current Queue.
     *
     * @param bucketId the bucket ID to validate.
     *
     * @return true if the specified bucket ID is valid for the current Queue.
     */
    protected boolean isValidBucketId(int bucketId)
        {
        m_queueInfo = m_queueInfoCache.invoke(m_sName, instantateInitialiseQueueInfoProcessor());
        int head = m_queueInfo.getHeadBucketId();
        int tail = m_queueInfo.getTailBucketId();

        if (head <= tail && bucketId >= head && bucketId <= tail)
            {
            return true;
            }
        else if (head > tail && (bucketId >= head || (bucketId >= 0 && bucketId <= tail)))
            {
            return true;
            }

        return false;
        }

    /**
     * Obtains the serializer from the underlying cache service.
     *
     * @return the serializer from the underlying cache service.
     */
    protected Serializer getSerializer()
        {
        return m_bucketCache.getService().getSerializer();
        }

    /**
     * Remove the element from the queue with the specified bucket and element ids.
     *
     * @param bucketId  the bucket id of the element to remove
     * @param elementId the element id within the bucket of the element to remove.
     */
    protected void removeElement(int bucketId, int elementId)
        {
        m_elementCache.remove(m_converterKeyToInternal.convert(new PagedQueueKey(bucketId, elementId)));
        }

    // ----- Inner Iterator Base class --------------------------------------

    /**
     * Ths class is an {@link Iterator} to iterate over the contents of this {@link BinaryPagedNamedQueue} in the correct
     * order.
     */
    protected abstract static class BaseBinaryQueueIterator implements Iterator<Binary>
        {

        // ----- constructor -------------------------------------------------

        /**
         * Construct a new {@link BaseBinaryQueueIterator} to iterate over this {@link BinaryPagedNamedQueue}.
         * The {@link BaseBinaryQueueIterator}
         * is initialised in the constructor to point to the head bucket or if this {@link BinaryPagedNamedQueue} is empty then
         * is initialised to an empty {@link Iterator}.
         */
        public BaseBinaryQueueIterator(BinaryPagedNamedQueue queue, boolean fHeadFirst, int maxBucketId)
            {
            m_queue       = queue;
            m_fHeadFirst  = fHeadFirst;
            m_maxBucketId = maxBucketId;

            if (queue.isEmpty())
                {
                m_iterator = null;
                }
            else
                {
                m_currentBucketId = fHeadFirst
                                    ? m_queue.refreshHeadBucketId()
                                    : m_queue.refreshTailBucketId();
                m_serializer      = m_queue.getSerializer();
                m_iterator        = m_queue.peekAtBucket(m_currentBucketId, fHeadFirst);
                }
            }

        // ----- BaseQueueIterator methods --------------------------------------

        /**
         * Move the {@link #m_currentBucketId} to point to the next bucket to be read.
         */
        protected abstract void moveToNextBucketId();

        // ----- Iterator implementation ----------------------------------------

        /**
         * Returns {@code true} if the iteration has more elements. (In other words, returns {@code true} if {@link
         * #next} would return an element rather than throwing an exception.)
         *
         * @return {@code true} if the iteration has more elements
         */
        @Override
        public boolean hasNext()
            {
            while (m_iterator == null || !m_iterator.hasNext())
                {
                moveToNextBucketId();
                m_iterator = m_queue.peekAtBucket(m_currentBucketId, m_fHeadFirst);

                while (m_iterator == null || !m_iterator.hasNext())
                    {
                    m_currentBucketId = m_fHeadFirst
                                        ? m_queue.findNextBucketId(m_currentBucketId)
                                        : m_queue.findPreviousBucketId(m_currentBucketId);

                    if (m_currentBucketId == -1)
                        {
                        return false;
                        }
                    m_iterator = m_queue.peekAtBucket(m_currentBucketId, m_fHeadFirst);
                    }
                }

            return m_iterator != null && m_iterator.hasNext();
            }

        /**
         * Returns the next element in the iteration.
         *
         * @return the next element in the iteration
         * @throws NoSuchElementException if the iteration has no more elements
         */
        @Override
        public Binary next()
            {
            if (m_iterator != null)
                {
                if (!m_iterator.hasNext())
                    {
                    // the iterator is finished, call hasNext() to try and refresh
                    if (!hasNext())
                        {
                        throw new NoSuchElementException("Iterator is exhausted");
                        }
                    }
                m_currentBinary = m_iterator.next();
                return m_currentBinary;
                }
            throw new NoSuchElementException("Iterator is exhausted");
            }

        // ----- data members ---------------------------------------------------

        /**
         * The {@link BinaryPagedNamedQueue} this {@link BinaryQueueIterator} iterates over
         */
        protected BinaryPagedNamedQueue m_queue;

        /**
         * The maximum bucket ID allowed in the Queue being iterated over
         */
        protected int m_maxBucketId;

        /**
         * Flag indicating whether this iterator is head first or tail first.
         */
        protected boolean m_fHeadFirst;

        /**
         * The id of the current bucket that is being iterated over
         */
        protected int m_currentBucketId;

        /**
         * The {@link Iterator} to iterate over the current bucket elements
         */
        protected Iterator<com.tangosol.util.Binary> m_iterator;

        /**
         * The {@link Serializer} to use to deserialize elements of the queue
         */
        protected Serializer m_serializer;

        /**
         * The current Binary element
         */
        protected com.tangosol.util.Binary m_currentBinary;
        }

    // ----- Inner Forward Iterator class ----------------------------------

    /**
     * Ths class is an {@link Iterator} to iterate over the contents of
     * a given {@link BinaryPagedNamedQueue} in head first order.
     */
    protected static class BinaryQueueIterator extends BaseBinaryQueueIterator
        {
        // ----- constructor -----------------------------------------------

        /**
         * Construct a new {@link BinaryQueueIterator} to iterate over this {@link BinaryPagedNamedQueue}.
         * The {@link BinaryQueueIterator} is initialised in the constructor to point to the head
         * bucket or if the {@link BinaryPagedNamedQueue} is empty then is initialised to an
         * empty {@link Iterator}.
         */
        public BinaryQueueIterator(BinaryPagedNamedQueue queue, int maxBucketId)
            {
            super(queue, true, maxBucketId);
            }

        // ----- BaseQueueIterator methods ---------------------------------

        @Override
        protected void moveToNextBucketId()
            {
            if (m_currentBucketId == m_maxBucketId)
                {
                m_currentBucketId = 0;
                }
            else
                {
                m_currentBucketId++;
                }
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The cache that holds the Queue information
     */
    protected NamedMap<String,QueueInfo> m_queueInfoCache;

    /**
     * The cache that holds the buckets.
     */
    protected NamedMap<Binary, Binary> m_bucketCache;

    /**
     * The cache that holds the versions.
     */
    protected NamedMap<?, ?> m_versionCache;

    /**
     * The cache that holds the Queue elements
     */
    protected NamedMap<Binary, Binary> m_elementCache;

    /**
     * The {@link QueueInfo} for this queue
     */
    protected QueueInfo m_queueInfo;
    }
