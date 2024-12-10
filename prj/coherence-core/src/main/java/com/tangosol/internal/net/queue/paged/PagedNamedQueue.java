/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.paged;

import com.tangosol.internal.net.queue.BaseNamedMapQueue;

import com.tangosol.internal.net.queue.PagedQueue;
import com.tangosol.internal.net.queue.model.QueueOfferResult;
import com.tangosol.internal.net.queue.model.QueuePollResult;

import com.tangosol.io.Serializer;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedQueue;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A {@link NamedQueue} implementation that stores data distributed over
 * a cluster in pages.
 *
 * @param <E>  the type of elements the queue contains
 */
public class PagedNamedQueue<E>
        extends BaseNamedMapQueue<PagedQueueKey, E>
        implements PagedQueue<E>
    {
    @SuppressWarnings({"unchecked"})
    public PagedNamedQueue(String sName, NamedCache<PagedQueueKey, E> cache)
        {
        super(sName, cache);

        CacheService cacheService = cache.getCacheService();
        String       sCacheName   = cache.getCacheName();
        ClassLoader  loader       = cache.getCacheService().getContextClassLoader();


        m_elementCache   = cache;
        m_bucketCache    = cacheService.ensureCache(PagedQueueCacheNames.Buckets.getCacheName(sCacheName), loader);
        m_queueInfoCache = cacheService.ensureCache(PagedQueueCacheNames.Info.getCacheName(sCacheName), loader);
        m_versionCache   = cacheService.ensureCache(PagedQueueCacheNames.Version.getCacheName(sCacheName), loader);

        m_queueInfo = m_queueInfoCache.invoke(sName, instantateInitialiseQueueInfoProcessor());

        m_elementCache.addIndex(PagedQueueKey.BUCKET_ID_EXTRACTOR, true, null);
        }

    // ----- BaseNamedCacheQueue methods ------------------------------------

    @Override
    public PagedQueueKey createKey(long id)
        {
        return new PagedQueueKey(m_keyHead.getHash(), id);
        }

    @Override
    public void clear()
        {
        m_bucketCache.invokeAll(new ClearQueueProcessor());
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
    public Iterator<E> iterator()
        {
        if (size() == 0)
            {
            return Collections.emptyIterator();
            }

        return new QueueIterator<>(this, m_queueInfo.getMaxBucketId());
        }

    @Override
    protected QueueOfferResult offerToTailInternal(E e)
        {
        if (e == null)
            {
            throw new NullPointerException("Null elements are not supported");
            }

        Binary                  binary       = ExternalizableHelper.toBinary(e, getSerializer());
        int                     tailBucketId = m_queueInfo.getTailBucketId();
        QueueOfferTailProcessor processor    = instantiateTailOfferProcessor(binary, m_queueInfo);
        QueueOfferResult        result       = m_bucketCache.invoke(tailBucketId, processor);

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
            result = m_bucketCache.invoke(tailBucketId, processor);
            }

        return result;
        }

    @Override
    protected QueuePollResult pollFromHeadInternal()
        {
        Binary binary = pollOrPeekHead(true);
        return toResult(binary);
        }

    @Override
    protected QueuePollResult peekAtHeadInternal()
        {
        Binary binary = pollOrPeekHead(false);
        return toResult(binary);
        }

    protected QueuePollResult toResult(Binary binary)
        {
        return new QueuePollResult(1, binary);
        }

    protected Binary pollOrPeekHead(boolean fPoll)
        {
        if (m_elementCache.isEmpty())
            {
            return null;
            }

        int                        headId    = m_queueInfo.getHeadBucketId();
        QueueVersionInfo           version   = m_queueInfo.getVersion();
        QueuePollPeekHeadProcessor processor = instantiatePollPeekHeadProcessor(fPoll, version);
        QueuePollResult            result    = m_bucketCache.invoke(headId, processor);

        while (result.getId() == QueuePollResult.RESULT_POLL_NEXT_PAGE && !m_elementCache.isEmpty())
            {
            HeadIncrementProcessor incrementor = new HeadIncrementProcessor(headId, version);
            m_queueInfo = m_queueInfoCache.invoke(m_sName, incrementor);
            version     = m_queueInfo.getVersion();
            headId      = m_queueInfo.getHeadBucketId();

            processor.setVersion(version);
            result = m_bucketCache.invoke(headId, processor);
            }

        return result.getBinaryElement();
        }

    // ----- helper methods -------------------------------------------------

    protected InitialiseQueueInfoProcessor instantateInitialiseQueueInfoProcessor()
        {
        return InitialiseQueueInfoProcessor.INSTANCE;
        }

    protected QueueOfferTailProcessor instantiateTailOfferProcessor(Binary binElement, QueueInfo queueInfo)
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
    protected Iterator<Binary> peekAtBucket(int bucketId, boolean fHeadFirst)
        {
        List<Binary> results = m_bucketCache.aggregate(Collections.singleton(bucketId),
                           new PeekWholeBucketAggregator<>(fHeadFirst));

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
        return m_bucketCache.getCacheService().getSerializer();
        }

    /**
     * Remove the element from the queue with the specified bucket and element ids.
     *
     * @param bucketId  the bucket id of the element to remove
     * @param elementId the element id within the bucket of the element to remove.
     */
    protected void removeElement(int bucketId, int elementId)
        {
        m_elementCache.remove(new PagedQueueKey(bucketId, elementId));
        }

    // ----- Inner Iterator Base class --------------------------------------

    /**
     * Ths class is an {@link Iterator} to iterate over the contents of this {@link PagedNamedQueue} in the correct
     * order.
     */
    protected abstract static class BaseQueueIterator<E> implements Iterator<E>
        {

        // ----- constructor -------------------------------------------------

        /**
         * Construct a new {@link BaseQueueIterator} to iterate over this {@link PagedNamedQueue}.
         * The {@link BaseQueueIterator}
         * is initialised in the constructor to point to the head bucket or if this {@link PagedNamedQueue} is empty then
         * is initialised to an empty {@link Iterator}.
         */
        public BaseQueueIterator(PagedNamedQueue<E> queue, boolean fHeadFirst, int maxBucketId)
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
        public E next()
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
                return ExternalizableHelper.fromBinary(m_currentBinary, m_serializer);
                }
            throw new NoSuchElementException("Iterator is exhausted");
            }

        // ----- data members ---------------------------------------------------

        /**
         * The {@link PagedNamedQueue} this {@link QueueIterator} iterates over
         */
        protected PagedNamedQueue<E> m_queue;

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
        protected Iterator<Binary> m_iterator;

        /**
         * The {@link Serializer} to use to deserialize elements of the queue
         */
        protected Serializer m_serializer;

        /**
         * The current Binary element
         */
        protected Binary m_currentBinary;
        }

    // ----- Inner Forward Iterator class ----------------------------------

    /**
     * Ths class is an {@link Iterator} to iterate over the contents of
     * a given {@link PagedNamedQueue} in head first order.
     */
    protected static class QueueIterator<E> extends BaseQueueIterator<E>
        {
        // ----- constructor -----------------------------------------------

        /**
         * Construct a new {@link QueueIterator} to iterate over this {@link PagedNamedQueue}.
         * The {@link QueueIterator} is initialised in the constructor to point to the head
         * bucket or if the {@link PagedNamedQueue} is empty then is initialised to an
         * empty {@link Iterator}.
         */
        public QueueIterator(PagedNamedQueue<E> queue, int maxBucketId)
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

    // ----- Inner Forward Iterator class ----------------------------------

    /**
     * Ths class is an {@link Iterator} to iterate over the contents of
     * a given {@link PagedNamedQueue} in tail first order.
     */
    protected static class QueueReverseIterator<E> extends BaseQueueIterator<E>
        {
        // ----- constructor -----------------------------------------------

        /**
         * Construct a new {@link QueueIterator} to iterate over this {@link PagedNamedQueue}.
         * The {@link QueueIterator} is initialised in the constructor to point to the tail
         * bucket or if the {@link PagedNamedQueue} is empty then is initialised to an
         * empty {@link Iterator}.
         */
        public QueueReverseIterator(PagedNamedQueue<E> queue, int maxBucketId)
            {
            super(queue, false, maxBucketId);
            }

        // ----- BaseQueueIterator methods ---------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        protected void moveToNextBucketId()
            {
            if (m_currentBucketId == 0)
                {
                m_currentBucketId = m_maxBucketId;
                }
            else
                {
                m_currentBucketId--;
                }
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The cache that holds the Queue information
     */
    protected NamedCache<String,QueueInfo> m_queueInfoCache;

    /**
     * The cache that holds the buckets.
     */
    protected NamedCache<Integer,Bucket> m_bucketCache;

    /**
     * The cache that holds the versions.
     */
    protected NamedCache<?, ?> m_versionCache;

    /**
     * The cache that holds the Queue elements
     */
    protected NamedCache<PagedQueueKey,E> m_elementCache;

    /**
     * The {@link QueueInfo} for this queue
     */
    protected QueueInfo m_queueInfo;
    }
