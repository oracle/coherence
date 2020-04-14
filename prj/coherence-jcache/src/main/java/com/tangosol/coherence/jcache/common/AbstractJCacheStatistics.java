/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.common;

/**
 * Shared implementation between implementations of JCacheStatistics.
 *
 * @author jf  2104.1.29
 * @since Coherence 12.1.3
 */
public abstract class AbstractJCacheStatistics
        implements JCacheStatistics
    {
    // ----- Object methods ---------------------------------------------------

    @Override
    public String toString()
        {
        StringBuffer buf = new StringBuffer(100);

        buf.append("CachePuts: ").append(getCachePuts()).append("   Average Put Time(ms): ").append(getAveragePutTime())
            .append(" Total Time(ms): ").append(this.getCachePutsMillis()).append(LINE_SEPARATOR);
        buf.append("CacheRemovals: ").append(getCacheRemovals()).append("   Average Removals Time: ")
            .append(getAverageRemoveTime()).append(" Total Time(ms)=").append(getCacheRemoveMillis())
            .append(LINE_SEPARATOR);
        buf.append("CacheGets: ").append(getCacheGets()).append("   Average Get Time(ms): ").append(getAverageGetTime())
            .append(LINE_SEPARATOR);
        buf.append("CacheHits: ").append(getCacheHits()).append(" Total Time(ms):").append(getCacheHitsMillis())
            .append(LINE_SEPARATOR);
        buf.append("CacheMisses: ").append(getCacheMisses()).append(" Total Time(ms):").append(getCacheMissesMillis())
            .append(LINE_SEPARATOR);
        buf.append("CacheHitPercentage: ").append(getCacheHitPercentage()).append("    CacheMissPercentage: ")
            .append(getCacheMissPercentage()).append(LINE_SEPARATOR);

        return buf.toString();
        }

    // ----- constants ------------------------------------------------------
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    }
