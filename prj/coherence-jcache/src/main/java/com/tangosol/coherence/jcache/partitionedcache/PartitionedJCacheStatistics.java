/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.partitionedcache;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.util.Duration;

import com.tangosol.coherence.config.Config;

import com.tangosol.coherence.jcache.common.AbstractJCacheStatistics;
import com.tangosol.coherence.jcache.common.ContextJCacheStatistics;
import com.tangosol.coherence.jcache.common.Helper;
import com.tangosol.coherence.jcache.common.JCacheContext;
import com.tangosol.coherence.jcache.common.JCacheIdentifier;
import com.tangosol.coherence.jcache.common.JCacheStatistics;

import com.tangosol.coherence.jcache.partitionedcache.processors.BinaryEntryHelper;
import com.tangosol.coherence.jcache.partitionedcache.processors.GetPartitionCountEP;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.NamedCache;
import com.tangosol.net.partition.SimplePartitionKey;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.AbstractExtractor;

import static com.tangosol.coherence.jcache.Constants.DEFAULT_PARTITIONED_CACHE_STATISTICS_REFRESHTIME;
import static com.tangosol.coherence.jcache.Constants.PARTITIONED_CACHE_STATISTICS_REFRESHTIME_SYSTEM_PROPERTY;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * compute cache statistics across all storage-enabled data members.
 *
 *
 * @author jf  2014.1.21
 * @since Coherence 12.1.3
 */
public class PartitionedJCacheStatistics
        extends AbstractJCacheStatistics
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs {@link PartitionedJCacheStatistics}
     *
     */
    public PartitionedJCacheStatistics()
        {
        // deserialization
        m_ncache    = null;
        m_extractor = null;
        }

    /**
     * Construct a {@link PartitionedJCacheStatistics} for a distributed cache.
     *
     * @param cache a partitioned cache
     */
    public PartitionedJCacheStatistics(PartitionedCache cache)
        {
        m_extractor = new CacheStatisticsExtractor(cache.getIdentifier());
        m_ncache    = (NamedCache) cache.unwrap(NamedCache.class);
        m_stats     = new ContextJCacheStatistics(cache.getIdentifier());
        }

    // ----- JCacheStatistics interface -------------------------------------

    @Override
    public void registerHits(int count, long lStartMillis)
        {
        m_stats.registerHits(count, lStartMillis);
        }

    @Override
    public void registerMisses(int count, long lStartMillis)
        {
        m_stats.registerMisses(count, lStartMillis);
        }

    @Override
    public void registerPuts(long count, long lStartMillis)
        {
        m_stats.registerPuts(count, lStartMillis);
        }

    @Override
    public void registerPutsCompleted(long lStartMillis)
        {
        m_stats.registerPutsCompleted(lStartMillis);
        }

    @Override
    public void registerRemoves(long count, long lStartMillis)
        {
        m_stats.registerRemoves(count, lStartMillis);
        }

    @Override
    public void registerRemove()
        {
        m_stats.registerRemove();
        }

    @Override
    public void registerHitsCompleted(long lStartMillis)
        {
        m_stats.registerHitsCompleted(lStartMillis);
        }

    @Override
    public void registerMissesCompleted(long lStartMillis)
        {
        m_stats.registerMissesCompleted(lStartMillis);
        }

    @Override
    public void registerRemoveCompleted(long lStartMillis)
        {
        m_stats.registerRemoveCompleted(lStartMillis);
        }

    @Override
    public JCacheIdentifier getIdentifier()
        {
        return m_stats.getIdentifier();
        }

    @Override
    public JCacheStatistics add(JCacheStatistics stats)
        {
        m_stats.add(stats);

        return this;
        }

    @Override
    public long getCacheHitsMillis()
        {
        refreshClusterJCacheStatistics();

        return m_stats.getCacheHitsMillis();
        }

    @Override
    public long getCacheMissesMillis()
        {
        refreshClusterJCacheStatistics();

        return m_stats.getCacheMissesMillis();
        }

    @Override
    public long getCachePutsMillis()
        {
        refreshClusterJCacheStatistics();

        return m_stats.getCachePutsMillis();
        }

    @Override
    public long getCacheRemoveMillis()
        {
        refreshClusterJCacheStatistics();

        return m_stats.getCacheRemoveMillis();
        }

    // ----- CacheStatisticsMXBean interface --------------------------------

    @Override
    public void clear()
        {
        // never throttle clear. always allow it to occur.
        aggregateStatistics(new PartitionedCacheStatisticsClear(getIdentifier(), m_extractor));
        m_stats.clear();
        }

    @Override
    public long getCacheHits()
        {
        refreshClusterJCacheStatistics();

        return m_stats.getCacheHits();
        }

    @Override
    public float getCacheHitPercentage()
        {
        refreshClusterJCacheStatistics();

        return m_stats.getCacheHitPercentage();
        }

    @Override
    public long getCacheMisses()
        {
        refreshClusterJCacheStatistics();

        return m_stats.getCacheMisses();
        }

    @Override
    public float getCacheMissPercentage()
        {
        refreshClusterJCacheStatistics();

        return m_stats.getCacheMissPercentage();
        }

    @Override
    public long getCacheGets()
        {
        refreshClusterJCacheStatistics();

        return m_stats.getCacheGets();
        }

    @Override
    public long getCachePuts()
        {
        refreshClusterJCacheStatistics();

        return m_stats.getCachePuts();
        }

    @Override
    public long getCacheRemovals()
        {
        refreshClusterJCacheStatistics();

        return m_stats.getCacheRemovals();
        }

    @Override
    public float getAverageGetTime()
        {
        refreshClusterJCacheStatistics();

        return m_stats.getAverageGetTime();
        }

    @Override
    public float getAveragePutTime()
        {
        refreshClusterJCacheStatistics();

        return m_stats.getAveragePutTime();
        }

    @Override
    public float getAverageRemoveTime()
        {
        refreshClusterJCacheStatistics();

        return m_stats.getAverageRemoveTime();
        }

    @Override
    public long getCacheEvictions()
        {
        // Not implemented at all.
        // refreshClusterJCacheStatistics();
        return m_stats.getCacheEvictions();
        }

    // ----- PartitionedJCacheStatistics methods -----------------------------

    /**
     * @return the duration between statistics refresh
     */
    public long getRefreshFrequency()
        {
        if (m_cRefreshFrequencyMillis == NOT_SET)
            {
            String durationStringValue = Config.getProperty(PARTITIONED_CACHE_STATISTICS_REFRESHTIME_SYSTEM_PROPERTY,
                    DEFAULT_PARTITIONED_CACHE_STATISTICS_REFRESHTIME);
            Duration duration;

            try
                {
                duration = new Duration(durationStringValue);
                }
            catch (IllegalArgumentException e)
                {
                Logger.warn("Invalid value \"" + durationStringValue + "\" for system property \""
                                 + PARTITIONED_CACHE_STATISTICS_REFRESHTIME_SYSTEM_PROPERTY + "\" using default of "
                                 + DEFAULT_PARTITIONED_CACHE_STATISTICS_REFRESHTIME);
                duration = new Duration(DEFAULT_PARTITIONED_CACHE_STATISTICS_REFRESHTIME);
                }

            m_cRefreshFrequencyMillis = duration.as(Duration.Magnitude.MILLI);
            }

        return m_cRefreshFrequencyMillis;
        }

    /**
     * Set the max frequency to allow cache statistics refresh across all members.
     *
     * @param m_ldtRefreshFrequency the duration between statistics refresh
     */
    public void setRefreshFrequency(long m_ldtRefreshFrequency)
        {
        this.m_cRefreshFrequencyMillis = m_ldtRefreshFrequency;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Aggregate JCacheStatistics over all storage-enabled members.
     */
    private void refreshClusterJCacheStatistics()
        {
        if (Helper.getCurrentTimeMillis() - m_ldtLastRefresh >= getRefreshFrequency())
            {
            InvocableMap.EntryAggregator entryAggregator = new PartitionedCacheStatisticsAggregator(getIdentifier(),
                                                               m_extractor);

            m_stats          = (ContextJCacheStatistics) aggregateStatistics(entryAggregator);
            m_ldtLastRefresh = Helper.getCurrentTimeMillis();
            }
        }

    /**
     * aggregate over storage enabled members using <code>entryAggregator</code>.
     * Only execute entryAggregator once per storage member. Needed context
     * from binaryEntry to get appropriate registry resource containing
     * appropriate JCache context.
     *
     * @param entryAggregator entryAggregator to use
     *
     * @return result of entryAggregator.
     */
    private Object aggregateStatistics(InvocableMap.EntryAggregator entryAggregator)
        {
        return m_ncache.aggregate(getKeys(), entryAggregator);
        }

    /**
     * Get SimplePartitionKey for each partition.
     *
     * @return Collection of keys.
     */
    private Collection getKeys()
        {
        if (m_keys == null)
            {
            int nPartitions = (int) m_ncache.invoke(SimplePartitionKey.getPartitionKey(0), new GetPartitionCountEP());
            Collection keys = new ArrayList(nPartitions);

            for (int i = 0; i < nPartitions; i++)
                {
                keys.add(SimplePartitionKey.getPartitionKey(i));
                }

            m_keys = keys;
            }

        return m_keys;
        }

    // ---- inner classes ---------------------------------------------------

    /**
     * Get CacheStatistics from binEntry's context for JCache id.
     */
    static public class CacheStatisticsExtractor
            extends AbstractExtractor
            implements ExternalizableLite, PortableObject
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs {@link CacheStatisticsExtractor}
         *
         */
        public CacheStatisticsExtractor()
            {
            // deserialization
            }

        /**
         * Constructs {@link CacheStatisticsExtractor} to get JCacheStatistics
         * associated with id.
         *
         * @param id JCache unique identifier for a cache.
         */
        public CacheStatisticsExtractor(JCacheIdentifier id)
            {
            m_id = id;
            }

        // ----- AbstractExtractor methods ----------------------------------

        /**
         *
         * @param entry  an Entry object to provide context to lookup up
         *               appropriate resource registry.
         *
         * @return JCache CacheStatistics looked up via entry's context.
         */
        @Override
        public Object extractFromEntry(Map.Entry entry)
            {
            BinaryEntry      binEntry = entry instanceof BinaryEntry ? (BinaryEntry) entry : null;
            JCacheStatistics result   = null;

            if (binEntry != null)
                {
                JCacheContext ctx = BinaryEntryHelper.getContext(m_id, binEntry);

                result = ctx != null ? ctx.getStatistics() : null;
                }

            return result;
            }

        // ----- ExternalizableLite interface -------------------------------

        @Override
        public void readExternal(DataInput in)
                throws IOException
            {
            // since unused by this implementation, not serializing field AbstractExtractor.m_nTarget
            m_id = readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out)
                throws IOException
            {
            // since unused by this implementation, not serializing field AbstractExtractor.m_nTarget
            writeObject(out, m_id);
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            // since unused by this implementation, not serializing field AbstractExtractor.m_nTarget
            m_id = in.readObject(0);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            // since unused by this implementation, not serializing field AbstractExtractor.m_nTarget
            out.writeObject(0, m_id);
            }

        // ----- data members -----------------------------------------------
        private JCacheIdentifier m_id;
        }

    /**
     * Collect JCache CacheStatistics from all storage-enabled members.
     */
    static public class PartitionedCacheStatisticsAggregator
            implements InvocableMap.StreamingAggregator,
                       ExternalizableLite, PortableObject
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs {@link PartitionedCacheStatisticsAggregator}
         *
         */
        public PartitionedCacheStatisticsAggregator()
            {
            // deserialization constructor
            }

        /**
         * Constructs an aggregator to aggregate JCache Cache Statistics from
         * all storage-enabled servers for a JCache implemented as a Partitioned Cache.
         *
         * @param id the JCache cache to collect cache statistics for.
         * @param valueExtractor extract CacheStatistics via binary entry context.
         */
        public PartitionedCacheStatisticsAggregator(JCacheIdentifier id, ValueExtractor valueExtractor)
            {
            m_extractor = valueExtractor;
            m_id        = id;
            m_stats     = new ContextJCacheStatistics(id);
            }

        // ----- StreamingAggregator methods --------------------------------

        @Override
        public InvocableMap.StreamingAggregator supply()
            {
            return new PartitionedCacheStatisticsAggregator(m_id, m_extractor);
            }

        @Override
        public boolean accumulate(InvocableMap.Entry entry)
            {
            process(entry.extract(m_extractor));
            return false;  // short-circuit within each member
            }

        @Override
        public boolean combine(Object partialResult)
            {
            process(partialResult);
            return true;  // process partial results from all the members
            }

        @Override
        public Object getPartialResult()
            {
            return m_stats;
            }

        @Override
        public Object finalizeResult()
            {
            return m_stats;
            }

        @Override
        public int characteristics()
            {
            return PARALLEL;
            }

        // ----- helper methods ---------------------------------------------

        protected void process(Object o)
            {
            if (o instanceof ContextJCacheStatistics)
                {
                m_stats.add((ContextJCacheStatistics) o);
                }
            }

        // ----- ExternalizableLite interface -----------------------------------

        @Override
        public void readExternal(DataInput in)
                throws IOException
            {
            in.readBoolean();
            m_extractor = ExternalizableHelper.readObject(in);
            m_stats     = ExternalizableHelper.readObject(in);
            m_id        = ExternalizableHelper.readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out)
                throws IOException
            {
            out.writeBoolean(false);  // m_fParallel
            ExternalizableHelper.writeObject(out, m_extractor);
            ExternalizableHelper.writeObject(out, m_stats);
            ExternalizableHelper.writeObject(out, m_id);
            }

        // ----- PortableObject interface ---------------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            m_extractor = in.readObject(1);
            m_stats     = in.readObject(2);
            m_id        = in.readObject(3);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeBoolean(0, false);  // m_fParallel
            out.writeObject(1, m_extractor);
            out.writeObject(2, m_stats);
            out.writeObject(3, m_id);
            }

        // ----- data members -----------------------------------------------

        /*
         *  result of aggregating JCache Cache Statistics from all storage-enabled
         *  servers.
         */
        private ContextJCacheStatistics m_stats;
        private ValueExtractor          m_extractor;
        private JCacheIdentifier        m_id;
        }

    /**
     * Clear JCache CacheStatistics from all storage-enabled members.
     */
    static public class PartitionedCacheStatisticsClear
            implements InvocableMap.StreamingAggregator,
                       ExternalizableLite, PortableObject
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs {@link PartitionedCacheStatisticsClear}
         *
         */
        public PartitionedCacheStatisticsClear()
            {
            // deserialization constructor
            }

        /**
         * Constructs an aggregator to aggregate JCache Cache Statistics from
         * all storage-enabled servers for a JCache implemented as a Partitioned Cache.
         *
         * @param id the JCache cache to collect cache statistics for.
         * @param valueExtractor extract CacheStatistics via binary entry context.
         */
        public PartitionedCacheStatisticsClear(JCacheIdentifier id, ValueExtractor valueExtractor)
            {
            m_extractor = valueExtractor;
            }

        // ----- StreamingAggregator methods --------------------------------

        @Override
        public InvocableMap.StreamingAggregator supply()
            {
            return new PartitionedCacheStatisticsClear(null, m_extractor);
            }

        @Override
        public boolean accumulate(InvocableMap.Entry entry)
            {
            process(entry.extract(m_extractor));
            return false;
            }

        @Override
        public boolean combine(Object partialResult)
            {
            process(partialResult);
            return true;
            }

        @Override
        public Object getPartialResult()
            {
            return null;
            }

        @Override
        public Object finalizeResult()
            {
            return null;
            }

        @Override
        public int characteristics()
            {
            return PARALLEL;
            }

        // ----- helper methods ---------------------------------------------

        protected void process(Object o)
            {
            final JCacheStatistics stats = o instanceof ContextJCacheStatistics ? (JCacheStatistics) o : null;

            if (stats != null)
                {
                stats.clear();
                }
            }

        // ----- ExternalizableLite interface -----------------------------------

        @Override
        public void readExternal(DataInput in)
                throws IOException
            {
            in.readBoolean();
            m_extractor = ExternalizableHelper.readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out)
                throws IOException
            {
            out.writeBoolean(false);  // m_fParallel
            ExternalizableHelper.writeObject(out, m_extractor);
            }

        // ----- PortableObject interface ---------------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            m_extractor = in.readObject(1);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeBoolean(0, false);  // m_fParallel
            out.writeObject(1, m_extractor);
            }

        // ----- data members -----------------------------------------------

        private ValueExtractor m_extractor;
        }

    // ----- constants ------------------------------------------------------

    private static final long NOT_SET = -1L;

    // ----- data members ---------------------------------------------------

    private NamedCache m_ncache;

    /**
     *  CacheStatistics extractor
     */
    private AbstractExtractor m_extractor;

    /**
     * Aggregation of LocalJCacheStatistics from all storage-enabled servers of m_ncache.
     */
    private ContextJCacheStatistics m_stats = null;

    /**
     * last time m_stats was refreshed with all cache statistics from cluster
     * members.
     */
    private long m_ldtLastRefresh = 0L;

    /**
     * In milliseconds, duration between updating cache statistics across cluster.
     */
    private long m_cRefreshFrequencyMillis = NOT_SET;

    /**
     * Cache of keys spanning all partitions of m_ncache.
     * Lazily initialized by {@link #getKeys}.
     */
    private Collection<SimplePartitionKey> m_keys = null;
    }
