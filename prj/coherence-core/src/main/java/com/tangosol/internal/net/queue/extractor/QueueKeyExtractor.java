/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.extractor;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.net.queue.NamedMapQueue;
import com.tangosol.internal.net.queue.model.QueueKey;

import com.tangosol.net.BackingMapContext;

import com.tangosol.net.cache.BinaryMemoryCalculator;
import com.tangosol.net.cache.ConfigurableCacheMap;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.EntryExtractor;
import com.tangosol.util.extractor.IndexAwareExtractor;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * A custom {@link ValueExtractor} to extract the {@link QueueKey}
 * from an entry.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class QueueKeyExtractor
        extends EntryExtractor
        implements IndexAwareExtractor
    {
    /**
     * Default constructor for serialization.
     */
    public QueueKeyExtractor()
        {
        }

    // ----- EntryExtractor methods -----------------------------------------

    @Override
    public Long extract(Object oTarget)
        {
        return (oTarget instanceof QueueKey) ? ((QueueKey) oTarget).getId() : -1L;
        }

    @Override
    public Long extractFromEntry(Map.Entry entry)
        {
        QueueKey key = (QueueKey) entry.getKey();
        return key == null ? -1L : key.getId();
        }

    @Override
    public String getCanonicalName()
        {
        return "Key";
        }

    // ----- IndexAwareExtractor methods ------------------------------------

    @Override
    @SuppressWarnings("deprecation")
    public MapIndex createIndex(boolean fOrdered, Comparator comparator, Map mapIndex, BackingMapContext ctx)
        {
        MapIndex index = (MapIndex) mapIndex.get(INSTANCE);
        if (index instanceof QueueIndex)
            {
            return null;
            }

        if (index != null)
            {
            Logger.warn("Replacing a previous index was created with extractor "
                    + INSTANCE + " which was not a " + QueueIndex.class);
            }

        long          cBytesMax  = NamedMapQueue.MAX_QUEUE_SIZE;
        ObservableMap backingMap = ctx.getBackingMap();
        if (backingMap instanceof ConfigurableCacheMap)
            {
            ConfigurableCacheMap map = (ConfigurableCacheMap) backingMap;
            if (map.getUnitCalculator() instanceof BinaryMemoryCalculator)
                {
                long nHighUnits = (long) map.getHighUnits() * (long) map.getUnitFactor();
                if (nHighUnits > 0)
                    {
                    cBytesMax = Math.min(nHighUnits, cBytesMax);
                    }
                }
            }

        int      nHash    = QueueKey.calculateQueueHash(ctx.getCacheName());
        MapIndex indexNew = new QueueIndex(nHash, cBytesMax);
        mapIndex.put(INSTANCE, indexNew);
        return indexNew;
        }

    @Override
    public MapIndex destroyIndex(Map mapIndex)
        {
        return (MapIndex) mapIndex.remove(INSTANCE);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        return o != null && o.getClass() == QueueKeyExtractor.class;
        }

    @Override
    public int hashCode()
        {
        return QueueKeyExtractor.class.hashCode();
        }

    @Override
    public String toString()
        {
        return "QueueKeyExtractor";
        }

    // ----- helper methods -------------------------------------------------

    public static <E> ValueExtractor<E, Long> instance()
        {
        return INSTANCE;
        }

    // ----- QueueIndex class: ----------------------------------------------

    /**
     * A custom {@link MapIndex} to make obtaining the
     * head and tail of a queue easier.
     */
    public static class QueueIndex
            implements MapIndex
        {
        public QueueIndex(int nQueueHash, long cBytesMax)
            {
            m_nQueueHash = nQueueHash;
            m_cBytesMax  = cBytesMax;
            }

        /**
         * Return the queue size in bytes.
         *
         * @return  the queue size in bytes
         */
        public long getQueueSize()
            {
            return m_queueSize.longValue();
            }

        /**
         * Return the maximum allowed queue size.
         *
         * @return the maximum allowed queue size
         */
        public long getMaxQueueSize()
            {
            return m_cBytesMax;
            }

        /**
         * Return the maximum allowed queue size.
         *
         * @param cBytesMax  the maximum allowed queue size
         */
        public void setMaxQueueSize(long cBytesMax)
            {
            m_cBytesMax = Math.min(NamedMapQueue.MAX_QUEUE_SIZE, cBytesMax);
            }

        /**
         * Obtain the head of the queue.
         *
         * @param nDefault  the default value to return if the queue is empty
         *
         * @return the head of the queue, or {@code  null} if the queue is empty
         */
        public long head(long nDefault)
            {
            return QueueIndex.head(m_map, nDefault);
            }

        public long nextHeadOffer()
            {
            return m_cHead.getAndDecrement();
            }

        public long nextTailOffer()
            {
            return m_cTail.getAndIncrement();
            }

        /**
         * Obtain the tail of the queue.
         *
         * @param nDefault  the default value to return if the queue is empty
         *
         * @return the head of the queue, or {@link null} i
         */
        public long tail(long nDefault)
            {
            return QueueIndex.tail(m_map, nDefault);
            }

        public SortedMap<Long, Object> tailMap(long fromId)
            {
            return Collections.unmodifiableSortedMap(m_map.tailMap(fromId, false));
            }

        public SortedMap<Long, Object> headMap(long fromId)
            {
            return Collections.unmodifiableSortedMap(m_map.headMap(fromId, false).descendingMap());
            }

        public Object getHeadBinaryKey(long nDefaultId)
            {
            long nHead =QueueIndex.head(m_map, nDefaultId);
            return m_map.get(nHead);
            }

        public Object getTailBinaryKey(long nDefaultId)
            {
            long nHead = QueueIndex.tail(m_map, nDefaultId);
            return m_map.get(nHead);
            }

        @Override
        public ValueExtractor getValueExtractor()
            {
            return INSTANCE;
            }

        @Override
        public boolean isOrdered()
            {
            return true;
            }

        @Override
        public boolean isPartial()
            {
            return true;
            }

        @Override
        public Map getIndexContents()
            {
            return Collections.emptyMap();
            }

        @Override
        public Object get(Object key)
            {
            return null;
            }

        @Override
        public Comparator getComparator()
            {
            return INSTANCE;
            }

        @Override
        public void insert(Map.Entry entry)
            {
            updateInternal(entry);
            }

        @Override
        public void update(Map.Entry entry)
            {
            updateInternal(entry);
            }

        @Override
        public void delete(Map.Entry entry)
            {
            long nId = ((QueueKey) entry.getKey()).getId();
            m_cHead.getAndUpdate(c -> c == nId ? c + 1 : c);
            m_cTail.getAndUpdate(c -> c == nId ? c - 1 : c);
            
            m_map.remove(nId);
            BinaryEntry<?, ?> binaryEntry = (BinaryEntry<?, ?>) entry;
            long cBytes = CALCULATOR.calculateUnits(binaryEntry.getBinaryKey(), binaryEntry.getBinaryValue());
            m_queueSize.add(-cBytes);
            }

        // ----- helper methods ---------------------------------------------

        public boolean isEmpty()
            {
            return m_map.isEmpty();
            }

        public static long head(SortedMap<Long, ?> map, long nDefault)
            {
            if (!map.isEmpty())
                {
                try
                    {
                    return map.firstKey();
                    }
                catch (NoSuchElementException e)
                    {
                    // Ideally we should not get here but if the map became empty
                    // after the isEmpty() check then we will get here.
                    // We can ignore this exception
                    }
                }
            return nDefault;
            }

        public static long tail(SortedMap<Long, ?> map, long nDefault)
            {
            if (!map.isEmpty())
                {
                try
                    {
                    return map.lastKey();
                    }
                catch (NoSuchElementException e)
                    {
                    // Ideally we should not get here but if the map became empty
                    // after the isEmpty() check then we will get here.
                    // We can ignore this exception
                    }
                }
            return nDefault;
            }

        private void updateInternal(Map.Entry entry)
            {
            QueueKey queueKey = (QueueKey) entry.getKey();
            long     nId      = queueKey.getId();
            Object   oKey     = entry instanceof BinaryEntry ? ((BinaryEntry) entry).getBinaryKey() : queueKey;
            m_cHead.getAndUpdate(c -> Math.min(c, nId));
            m_cTail.getAndUpdate(c -> Math.max(c, nId));
            m_map.put(nId, oKey);
            if (entry instanceof BinaryEntry<?,?>)
                {
                BinaryEntry<?, ?> binaryEntry = (BinaryEntry<?, ?>) entry;
                long cBytes = CALCULATOR.calculateUnits(binaryEntry.getBinaryKey(), binaryEntry.getBinaryValue());
                m_queueSize.add(cBytes);
                }
            }

        // ----- data members -----------------------------------------------

        private static final BinaryMemoryCalculator CALCULATOR = new BinaryMemoryCalculator();

        private final int m_nQueueHash;

        private final AtomicLong m_cHead = new AtomicLong();

        private final AtomicLong m_cTail = new AtomicLong();

        /**
         * The maximum allowed size for the queue.
         */
        private long m_cBytesMax;

        private final ConcurrentSkipListMap<Long, Object> m_map = new ConcurrentSkipListMap<>();

        /**
         * The approximate size of the queue in bytes.
         */
        private final LongAdder m_queueSize = new LongAdder();
        }

    // ----- data members ---------------------------------------------------

    public static final QueueKeyExtractor INSTANCE = new QueueKeyExtractor();
    }
