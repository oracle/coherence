/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence;

import com.oracle.datagrid.persistence.OfflinePersistenceInfo;
import com.oracle.datagrid.persistence.PersistenceStatistics;
import com.oracle.datagrid.persistence.PersistenceTools;

import com.tangosol.util.Binary;
import com.tangosol.util.LongArray;

/**
 * Abstract implementation of PersistenceTools which can be extended for either
 * local or archived snapshot operations.
 *
 * @author tam/hr  2014.11.21
 * @since 12.2.1
 */
public abstract class AbstractPersistenceTools
        implements PersistenceTools
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs an abstract implementation of {@link PersistenceTools} that
     * can be utilized for both local or archived snapshot operations.
     *
     * @param info  the information collected about the snapshot
     */
    public AbstractPersistenceTools(OfflinePersistenceInfo info)
        {
        f_info = info;
        }

    // ----- PersistenceTools methods ---------------------------------------

    @Override
    public OfflinePersistenceInfo getPersistenceInfo()
        {
        return f_info;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder();

        if (f_info != null)
            {
            sb.append(f_info);
            }

        PersistenceStatistics stats = getStatistics();
        if (stats != null)
            {
            sb.append(stats);
            }

        return sb.toString();
        }

    // ----- inner class: StatsVisitor  -------------------------------------

    /**
     * An implementation of a {@link CachePersistenceHelper.Visitor} to collect details
     * statistics from the snapshot we are analysing.
     */
    protected static class StatsVisitor
            implements CachePersistenceHelper.Visitor
        {
        // ---- constructors ------------------------------------------------

        /**
         * Construct a new Stats visitor.
         *
         * @param stats  the statistics to update when visiting
         */
        public StatsVisitor(PersistenceStatistics stats)
            {
            f_stats = stats;
            }

        // ---- CachePersistenceHelper.Visitor interface --------------------

        @Override
        public boolean visitCacheEntry(long lOldCacheId, Binary binKey, Binary binValue)
            {
            String sCacheName = getCacheName(lOldCacheId);

            f_stats.addToBytes(sCacheName, binKey.length() + binValue.length());
            f_stats.incrementSize(sCacheName);

            return true;
            }

        @Override
        public boolean visitListener(long lOldCacheId, Binary binKey, long lListenerId, boolean fLite)
            {
            f_stats.incrementListeners(getCacheName(lOldCacheId));

            return true;
            }

        @Override
        public boolean visitLock(long lOldCacheId, Binary binKey, long lHolderId, long lHolderThreadId)
            {
            f_stats.incrementLocks(getCacheName(lOldCacheId));

            return true;
            }

        @Override
        public boolean visitIndex(long lOldCacheId, Binary binExtractor, Binary binComparator)
            {
            f_stats.incrementIndexes(getCacheName(lOldCacheId));

            return true;
            }

        @Override
        public boolean visitTrigger(long lOldCacheId, Binary binTrigger)
            {
            f_stats.incrementTriggers(getCacheName(lOldCacheId));

            return true;
            }

        // ----- helpers -----------------------------------------------------

        /**
         * Return the cache name based upon the old cache id.
         *
         * @param lOldCacheId  the old cache id to lookup
         *
         * @return the cache name based upon the old cache id
         */
        protected String getCacheName(long lOldCacheId)
            {
            if (m_laCaches == null)
                {
                throw new IllegalStateException("You must use setCaches() to set the caches");
                }

            return (String) m_laCaches.get(lOldCacheId);
            }

        // ----- accessors ---------------------------------------------------

        /**
         * Set the {@link LongArray} of caches for this visitor.
         *
         * @param laCaches  the LongArray of caches
         */
        public void setCaches(LongArray laCaches)
            {
            m_laCaches = laCaches;
            }

        // ----- data members ------------------------------------------------

        /**
         * A LongArray of cache id to cache name.
         */
        private LongArray m_laCaches = null;

        /**
         * The statistics being collected by this visitor.
         */
        private final PersistenceStatistics f_stats;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Information about the snapshot or archived snapshot.
     */
    protected final OfflinePersistenceInfo f_info;
    }
