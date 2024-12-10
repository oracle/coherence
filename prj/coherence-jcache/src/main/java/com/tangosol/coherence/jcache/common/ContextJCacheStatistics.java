/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.common;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.concurrent.atomic.AtomicLong;

/**
 * JCache Statistics implementation.
 *
 * JCache cache statistics differed enough from current Coherence cache statistics that
 * just ended up maintaining statistics separately.
 *
 * Examples of differences include put of same value is optimized in Coherence implementation
 * but JCache considers them 2 distinct puts. (could not pass jsr 107 tck with that behavior)
 * Additionally, coherence does not count removals at the time this was written.
 *
 * @author jf  2013.10.24
 * @since Coherence 12.1.3
 */
public class ContextJCacheStatistics
        extends AbstractJCacheStatistics
        implements JCacheStatistics, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs ...
     *
     */
    public ContextJCacheStatistics()
        {
        // for deserialization
        }

    /**
     * Constructs JCacheStatistics for cache <code>id</code>
     *
     * @param id unique JCache cache identifier
     */
    public ContextJCacheStatistics(JCacheIdentifier id)
        {
        m_id = id;
        }

    // ----- JCacheStatistics interface  ------------------------------------

    @Override
    public void registerHits(int count, long lStartMillis)
        {
        m_cGets.addAndGet(count);
        m_cHits.addAndGet(count);
        registerHitsCompleted(lStartMillis);
        }

    @Override
    public void registerMisses(int count, long lStartMillis)
        {
        m_cGets.addAndGet(count);
        m_cMisses.addAndGet(count);
        registerMissesCompleted(lStartMillis);
        }

    @Override
    public void registerPuts(long count, long lStartMillis)
        {
        m_cPuts.addAndGet(count);
        registerPutsCompleted(lStartMillis);
        }

    @Override
    public void registerPutsCompleted(long lStartMillis)
        {
        m_cPutsMillis.addAndGet(Helper.getCurrentTimeMillis() - lStartMillis);
        }

    @Override
    public void registerRemoves(long count, long lStartMillis)
        {
        m_cRemove.addAndGet(count);
        registerRemoveCompleted(lStartMillis);
        }

    @Override
    public void registerRemove()
        {
        m_cRemove.incrementAndGet();
        }

    @Override
    public void registerHitsCompleted(long lStartMillis)
        {
        m_cHitsMillis.addAndGet(Helper.getCurrentTimeMillis() - lStartMillis);
        }

    @Override
    public void registerMissesCompleted(long lStartMillis)
        {
        m_cMissesMillis.addAndGet(Helper.getCurrentTimeMillis() - lStartMillis);
        }

    @Override
    public void registerRemoveCompleted(long lStartMillis)
        {
        m_cRemoveMillis.addAndGet(Helper.getCurrentTimeMillis() - lStartMillis);
        }

    @Override
    public JCacheIdentifier getIdentifier()
        {
        return m_id;
        }

    @Override
    public JCacheStatistics add(JCacheStatistics stats)
        {
        m_cGets.addAndGet(stats.getCacheGets());
        m_cHits.addAndGet(stats.getCacheHits());
        m_cHitsMillis.addAndGet(stats.getCacheHitsMillis());

        m_cMisses.addAndGet(stats.getCacheMisses());
        m_cMissesMillis.addAndGet(stats.getCacheMissesMillis());

        m_cPuts.addAndGet(stats.getCachePuts());
        m_cPutsMillis.addAndGet(stats.getCachePutsMillis());

        m_cRemove.addAndGet(stats.getCacheRemovals());
        m_cRemoveMillis.addAndGet(stats.getCacheRemoveMillis());

        // note that cache evictions are not being counted at this time.

        return this;
        }

    @Override
    public long getCacheHitsMillis()
        {
        return m_cHitsMillis.get();
        }

    @Override
    public long getCacheMissesMillis()
        {
        return m_cMissesMillis.get();
        }

    @Override
    public long getCachePutsMillis()
        {
        return m_cPutsMillis.get();
        }

    @Override
    public long getCacheRemoveMillis()
        {
        return m_cRemoveMillis.get();
        }

    // ----- CacheStatisticsMXBean interface ----------------------------------

    @Override
    public void clear()
        {
        m_cGets.set(0);
        m_cHits.set(0);
        m_cHitsMillis.set(0);

        m_cMisses.set(0);
        m_cMissesMillis.set(0);

        m_cPuts.set(0);
        m_cPutsMillis.set(0);

        m_cRemove.set(0);
        m_cRemoveMillis.set(0);
        }

    @Override
    public long getCacheHits()
        {
        return m_cHits.get();
        }

    @Override
    public float getCacheHitPercentage()
        {
        return getCacheGets() == 0 ? 0 : (((float) getCacheHits() / (float) getCacheGets()) * 100.0f);
        }

    @Override
    public long getCacheMisses()
        {
        return m_cMisses.get();
        }

    @Override
    public float getCacheMissPercentage()
        {
        return getCacheGets() == 0 ? 0 : (float) (((float) getCacheMisses() / (float) getCacheGets()) * 100.0f);
        }

    @Override
    public long getCacheGets()
        {
        return m_cGets.get();
        }

    @Override
    public long getCachePuts()
        {
        return m_cPuts.get();
        }

    @Override
    public long getCacheRemovals()
        {
        return m_cRemove.get();
        }

    @Override
    public long getCacheEvictions()
        {
        // not implemented.  JCache does not perform evictions. Coherence does not provide this information directly.
        // since this is implementation specific, no jsr 107 tck tests for this stat.
        return 0;
        }

    @Override
    public float getAverageGetTime()
        {
        long gets = getCacheGets();

        return gets == 0 ? 0 : ((float) (getCacheMissesMillis() + getCacheHitsMillis()) / (float) gets);

        }

    @Override
    public float getAveragePutTime()
        {
        long puts = getCachePuts();

        return puts == 0 ? 0 : ((float) getCachePutsMillis()) / (float) puts;
        }

    @Override
    public float getAverageRemoveTime()
        {
        long count = getCacheRemovals();

        return count == 0 ? 0 : (float) getCacheRemoveMillis() / (float) count;
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_id = (JCacheIdentifier) in.readObject(0);
        m_cGets.set(in.readLong(1));
        m_cHits.set(in.readLong(2));
        m_cHitsMillis.set(in.readLong(3));
        m_cMisses.set(in.readLong(4));
        m_cMissesMillis.set(in.readLong(5));
        m_cPuts.set(in.readLong(6));
        m_cPutsMillis.set(in.readLong(7));
        m_cRemove.set(in.readLong(8));
        m_cRemoveMillis.set(in.readLong(9));
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_id);
        out.writeLong(1, m_cGets.get());
        out.writeLong(2, m_cHits.get());
        out.writeLong(3, m_cHitsMillis.get());
        out.writeLong(4, m_cMisses.get());
        out.writeLong(5, m_cMissesMillis.get());
        out.writeLong(6, m_cPuts.get());
        out.writeLong(7, m_cPutsMillis.get());
        out.writeLong(8, m_cRemove.get());
        out.writeLong(9, m_cRemoveMillis.get());
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_id = (JCacheIdentifier) ExternalizableHelper.readObject(in);
        m_cGets.set(ExternalizableHelper.readLong(in));
        m_cHits.set(ExternalizableHelper.readLong(in));
        m_cHitsMillis.set(ExternalizableHelper.readLong(in));
        m_cMisses.set(ExternalizableHelper.readLong(in));
        m_cMissesMillis.set(ExternalizableHelper.readLong(in));
        m_cPuts.set(ExternalizableHelper.readLong(in));
        m_cPutsMillis.set(ExternalizableHelper.readLong(in));
        m_cRemove.set(ExternalizableHelper.readLong(in));
        m_cRemoveMillis.set(ExternalizableHelper.readLong(in));
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_id);
        ExternalizableHelper.writeLong(out, m_cGets.get());
        ExternalizableHelper.writeLong(out, m_cHits.get());
        ExternalizableHelper.writeLong(out, m_cHitsMillis.get());
        ExternalizableHelper.writeLong(out, m_cMisses.get());
        ExternalizableHelper.writeLong(out, m_cMissesMillis.get());
        ExternalizableHelper.writeLong(out, m_cPuts.get());
        ExternalizableHelper.writeLong(out, m_cPutsMillis.get());
        ExternalizableHelper.writeLong(out, m_cRemove.get());
        ExternalizableHelper.writeLong(out, m_cRemoveMillis.get());
        }

    // ----- constants ------------------------------------------------------
    private static final long serialVersionUID = -1L;

    // ----- data members ---------------------------------------------------

    private final AtomicLong m_cGets         = new AtomicLong(0);
    private final AtomicLong m_cHits         = new AtomicLong(0);
    private final AtomicLong m_cHitsMillis   = new AtomicLong(0);
    private final AtomicLong m_cMisses       = new AtomicLong(0);
    private final AtomicLong m_cMissesMillis = new AtomicLong(0);
    private final AtomicLong m_cPuts         = new AtomicLong(0);
    private final AtomicLong m_cPutsMillis   = new AtomicLong(0);
    private final AtomicLong m_cRemove       = new AtomicLong(0);
    private final AtomicLong m_cRemoveMillis = new AtomicLong(0);
    private JCacheIdentifier m_id;
    }
