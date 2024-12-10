/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.persistence;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PersistenceStatistics provides statistics in relation to the entries and
 * metadata persisted to allow recovery of Coherence caches. These statistics
 * are accumulated from either actively persisted data, snapshots or archived
 * snapshots. Fundamentally these statistics provide a means to validate the
 * integrity of the persisted data but also provide an insight into the data
 * and metadata stored.
 * <p>
 * The usage of this data structure is intended to pivot around cache names,
 * thus to output the byte size of all caches the following would typically be
 * executed:
 * <pre><code>
 *     PersistenceStatistics stats = ...;
 *     for (String sCacheName : stats)
 *         {
 *         long cb = stats.getCacheBytes(sCacheName);
 *         System.out.printf("%s has %d bytes\n", sCacheName, cb);
 *         }
 * </code></pre>
 *
 * @author  tam/hr  2014.11.18
 * @since   12.2.1
 */
public class PersistenceStatistics
        implements Iterable<String>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new instance to store persistence statistics.
     */
    public PersistenceStatistics()
        {
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder("Cache Statistics\n\n");
        f_mapStats.values().forEach(sb::append);
        return sb.toString();
        }

    // ----- Iterable interface ---------------------------------------------

    @Override
    public Iterator<String> iterator()
        {
        return f_mapStats.keySet().iterator();
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Increment the size (count of entries) for a cache.
     *
     * @param sCacheName  the cache to increment
     */
    public void incrementSize(String sCacheName)
        {
        ensureCacheStats(sCacheName).incrementSize();
        }

    /**
     * Add number of raw bytes stored for a cache.
     *
     * @param sCacheName  the cache name to add bytes to
     * @param cBytes      the number of bytes to add
     */
    public void addToBytes(String sCacheName, long cBytes)
        {
        ensureCacheStats(sCacheName).addToBytes(cBytes);
        }

    /**
     * Increment the index count for a cache.
     *
     * @param sCacheName  the cache to increment for
     */
    public void incrementIndexes(String sCacheName)
        {
        ensureCacheStats(sCacheName).incrementIndexes();
        }

    /**
     * Increment the trigger count for a cache.
     *
     * @param sCacheName  the cache to increment for
     */
    public void incrementTriggers(String sCacheName)
        {
        ensureCacheStats(sCacheName).incrementTriggers();
        }

    /**
     * Increment the lock count for a cache.
     *
     * @param sCacheName  the cache to increment for
     */
    public void incrementLocks(String sCacheName)
        {
        ensureCacheStats(sCacheName).incrementTriggers();
        }

    /**
     * Increment the listener count for a cache.
     *
     * @param sCacheName  the cache to increment
     */
    public void incrementListeners(String sCacheName)
        {
        ensureCacheStats(sCacheName).incrementListeners();
        }

    /**
     * Return the size (count) of number of entries.
     *
     * @param sCacheName  the cache to return the size
     *
     * @return the size (count) of number of entries
     */
    public long getCacheSize(String sCacheName)
        {
        CachePersistenceStatistics stats = f_mapStats.get(sCacheName);

        return stats == null ? -1 : stats.getSize();
        }

    /**
     * Return the number of raw bytes for a given cache.
     *
     * @param sCacheName  the cache name to return bytes
     *
     * @return the number of raw bytes for a given cache
     */
    public long getCacheBytes(String sCacheName)
        {
        CachePersistenceStatistics stats = f_mapStats.get(sCacheName);

        return stats == null ? -1 : stats.getBytes();
        }

    /**
     * Return the number of indexes for a given cache.
     *
     * @param sCacheName  the cache name to return indexes
     *
     * @return the number of indexes for a given cache
     */
    public int getIndexCount(String sCacheName)
        {
        CachePersistenceStatistics stats = f_mapStats.get(sCacheName);

        return stats == null ? -1 : stats.getIndexCount();
        }

    /**
     * Return the number of triggers for a given cache.
     *
     * @param sCacheName  the cache name to return triggers
     *
     * @return the number of triggers for a given cache
     */
    public int getTriggerCount(String sCacheName)
        {
        CachePersistenceStatistics stats = f_mapStats.get(sCacheName);

        return stats == null ? -1 : stats.getTriggerCount();
        }

    /**
     * Return the number of locks for a given cache.
     *
     * @param sCacheName  the cache name to return locks
     *
     * @return the number of locks for a given cache
     */
    public int getLockCount(String sCacheName)
        {
        CachePersistenceStatistics stats = f_mapStats.get(sCacheName);

        return stats == null ? -1 : stats.getLockCount();
        }

    /**
     * Return the number of listeners for a given cache.
     *
     * @param sCacheName  the cache name to return listeners
     *
     * @return the number of listeners for a given cache
     */
    public int getListenerCount(String sCacheName)
        {
        CachePersistenceStatistics stats = f_mapStats.get(sCacheName);

        return stats == null ? -1 : stats.getListenerCount();
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Ensure a CachePersistenceStatistics object exists for a cache.
     *
     * @param sCacheName  the cache name relating to the CachePersistenceStatistics
     *
     * @return a CachePersistenceStatistics object for the given cache
     */
    protected CachePersistenceStatistics ensureCacheStats(String sCacheName)
        {
        CachePersistenceStatistics stats = f_mapStats.get(sCacheName);

        if (stats == null)
            {
            f_mapStats.put(sCacheName, stats = new CachePersistenceStatistics(sCacheName));
            }
        return stats;
        }

    // ----- inner class: CachePersistenceStatistics  -----------------------

    /**
     * A holder for statics pertaining to an individual cache.
     */
     protected class CachePersistenceStatistics
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a new instance for a given cache.
         *
         * @param sCacheName  the cache name to create stats
         */
        protected CachePersistenceStatistics(String sCacheName)
            {
            f_sCacheName = sCacheName;
            }

        // ----- CachePersistenceStatistics methods -------------------------

        /**
         * Increment the size (count) of entries.
         */
        public void incrementSize()
            {
            m_cSize++;
            }

        /**
         * Add the number of bytes to the count of bytes.
         *
         * @param cBytes  the number of bytes to add
         */
        public void addToBytes(long cBytes)
            {
            m_cBytes += cBytes;
            }

        /**
         * Increment the number of triggers.
         */
        public void incrementTriggers()
            {
            m_cTriggers++;
            }

        /**
         * Increment the number of indexes.
         */
        public void incrementIndexes()
            {
            m_cIndexes++;
            }

        /**
         * Increment the number of locks.
         */
        public void incrementLocks()
            {
            m_cLocks++;
            }

        /**
         * Increment the number of listeners.
         */
        public void incrementListeners()
            {
            m_cListeners++;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the cache name.
         *
         * @return the cache name
         */
        public String getCacheName()
            {
            return f_sCacheName;
            }

        /**
         * Return the size (count) of number of entries.
         *
         * @return he size (count) of number of entries
         */
        public long getSize()
            {
            return m_cSize;
            }

        /**
         * Return the number of bytes stored for the cache.
         *
         * @return  the number of bytes stored for the cache
         */
        public long getBytes()
            {
            return m_cBytes;
            }

        /**
         * Return the number of indexes.
         *
         * @return the number of indexes
         */
        public int getIndexCount()
            {
            return m_cIndexes;
            }

        /**
         * Return the number of triggers.
         *
         * @return the number of triggers
         */
        public int getTriggerCount()
            {
            return m_cTriggers;
            }

        /**
         * Return the number of locks.
         *
         * @return the number of locks
         */
        public int getLockCount()
            {
            return m_cLocks;
            }

        /**
         * Return the number of listeners.
         *
         * @return the number of listeners
         */
        public int getListenerCount()
            {
            return m_cListeners;
            }

        @Override
        public String toString()
            {
            StringBuilder sb = new StringBuilder("PersistenceStatistics(cacheName=");
            sb.append(f_sCacheName)
              .append(", size=")
              .append(m_cSize)
              .append(", bytes=")
              .append(m_cBytes)
              .append(", indexes=")
              .append(m_cIndexes)
              .append(", triggers=")
              .append(m_cTriggers)
              .append(", locks=")
              .append(m_cLocks)
              .append(", listeners=")
              .append(m_cListeners)
              .append(")");

            return sb.toString();
            }

        // ----- data members ----------------------------------------------

        /**
         * The cache name to store statistics about.
         */
        private final String f_sCacheName;

        /**
         * The size (count) of entries in the cache.
         */
        protected long m_cSize = 0L;

        /**
         * The number of bytes stores for both key and values.
         */
        protected long m_cBytes = 0L;

        /**
         * The number of triggers stored.
         */
        protected int m_cTriggers = 0;

        /**
         * The number of indexes stored.
         */
        protected int m_cIndexes = 0;

        /**
         * The number of locks stored.
         */
        protected int m_cLocks = 0;

        /**
         * The number of listeners stored.
         */
        protected int m_cListeners = 0;
        }

    // ----- data members --------------------------------------------------

    /**
     * The map of CachePersistenceStatistics for all caches.
     */
    private final Map<String, CachePersistenceStatistics> f_mapStats = new LinkedHashMap<>();;
    }