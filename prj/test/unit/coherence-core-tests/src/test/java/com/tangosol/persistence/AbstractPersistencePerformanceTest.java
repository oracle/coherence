/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence;

import com.oracle.coherence.persistence.PersistentStore.Visitor;

import com.tangosol.internal.util.DaemonPool;
import com.tangosol.internal.util.Daemons;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.persistence.AbstractPersistenceManager.AbstractPersistentStore;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BitHelper;
import com.tangosol.util.ClassHelper;

import java.io.File;
import java.io.IOException;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Abstract performance test for an AbstractPersistenceManager subclass.
 *
 * @author jh  2012.11.13
 */
public abstract class AbstractPersistencePerformanceTest
    {

    // ----- test lifecycle -------------------------------------------------

    @Before
    public void setupTest()
            throws IOException
        {
        m_file = FileHelper.createTempDir();

        DefaultDaemonPoolDependencies deps = new DefaultDaemonPoolDependencies();
        deps.setThreadCount(1);

        m_pool = Daemons.newDaemonPool(deps);
        m_pool.start();

        m_manager = createPersistenceManager();
        }

    @After
    public void teardownTest()
        {
        m_manager.release();
        m_pool.stop();
        }

    protected abstract AbstractPersistenceManager createPersistenceManager()
            throws IOException;

    // ----- test methods ---------------------------------------------------

    @Test
    public void testWarmup()
            throws IOException
        {
        System.out.println(ClassHelper.getSimpleName(getClass())
                + ".testWarmup():");

        AbstractPersistentStore store  = ensurePersistentStore();
        try
            {
            ReadBuffer[] abKeys = createKeySequence(100);
            for (int c = 0; c < 4; ++c)
                {
                populateStore(store, abKeys, BINARY_SMALL_VALUE);
                consumeStore(store, abKeys);
                populateStore(store, abKeys, BINARY_LARGE_VALUE);
                consumeStore(store, abKeys);
                }

            Base.randomize(abKeys);
            for (int c = 0; c < 4; ++c)
                {
                populateStore(store, abKeys, BINARY_SMALL_VALUE);
                consumeStore(store, abKeys);
                populateStore(store, abKeys, BINARY_LARGE_VALUE);
                consumeStore(store, abKeys);
                }
            }
        finally
            {
            deletePersistentStore();
            }
        }

    @Test
    public void testSmallRandomLoad()
            throws IOException
        {
        System.out.println(ClassHelper.getSimpleName(getClass())
                + ".testSmallRandomLoad():");

        // create a random sequence of serialized integer keys
        ReadBuffer[] abKeys = createKeySequence(10000);
        Base.randomize(abKeys);

        for (int c = 0; c < 10; ++c)
            {
            profileLoad(abKeys, BINARY_SMALL_VALUE);
            }

        System.out.println();
        }

    @Test
    public void testSmallSequentialLoad()
            throws IOException
        {
        System.out.println(ClassHelper.getSimpleName(getClass())
                + ".testSmallSequentialLoad():");

        // create a sequence of serialized integer keys
        ReadBuffer[] abKeys = createKeySequence(10000);

        for (int c = 0; c < 10; ++c)
            {
            profileLoad(abKeys, BINARY_SMALL_VALUE);
            }

        System.out.println();
        }

    @Test
    public void testLargeRandomLoad()
            throws IOException
        {
        System.out.println(ClassHelper.getSimpleName(getClass())
                + ".testLargeRandomLoad():");

        // create a random sequence of serialized integer keys
        ReadBuffer[] abKeys = createKeySequence(1000);
        Base.randomize(abKeys);

        for (int c = 0; c < 10; ++c)
            {
            profileLoad(abKeys, BINARY_LARGE_VALUE);
            }

        System.out.println();
        }

    @Test
    public void testLargeSequentialLoad()
            throws IOException
        {
        System.out.println(ClassHelper.getSimpleName(getClass())
                + ".testLargeSequentialLoad():");

        // create a sequence of serialized integer keys
        ReadBuffer[] abKeys = createKeySequence(1000);

        for (int c = 0; c < 10; ++c)
            {
            profileLoad(abKeys, BINARY_LARGE_VALUE);
            }

        System.out.println();
        }

    @Test
    public void testSmallIteration()
            throws IOException
        {
        System.out.println(ClassHelper.getSimpleName(getClass())
                + ".testSmallIteration():");

        // create a sequence of serialized integer keys
        ReadBuffer[] abKeys = createKeySequence(10000);

        for (int c = 0; c < 10; ++c)
            {
            profileIteration(abKeys, BINARY_SMALL_VALUE);
            }

        System.out.println();
        }

    @Test
    public void testLargeIteration()
            throws IOException
        {
        System.out.println(ClassHelper.getSimpleName(getClass())
                + ".testLargeIteration():");

        // create a sequence of serialized integer keys
        ReadBuffer[] abKeys = createKeySequence(1000);

        for (int c = 0; c < 10; ++c)
            {
            profileIteration(abKeys, BINARY_LARGE_VALUE);
            }

        System.out.println();
        }

    @Test
    public void testSmallRandomStore()
            throws IOException
        {
        System.out.println(ClassHelper.getSimpleName(getClass())
                + ".testSmallRandomStore():");

        // create a random sequence of serialized integer keys
        ReadBuffer[] abKeys = createKeySequence(10000);
        Base.randomize(abKeys);

        for (int c = 0; c < 10; ++c)
            {
            profileStore(abKeys, BINARY_SMALL_VALUE);
            }

        System.out.println();
        }

    @Test
    public void testSmallSequentialStore()
            throws IOException
        {
        System.out.println(ClassHelper.getSimpleName(getClass())
                + ".testSmallSequentialStore():");

        // create a sequence of serialized integer keys
        ReadBuffer[] abKeys = createKeySequence(10000);

        for (int c = 0; c < 10; ++c)
            {
            profileStore(abKeys, BINARY_SMALL_VALUE);
            }

        System.out.println();
        }

    @Test
    public void testLargeRandomStore()
            throws IOException
        {
        System.out.println(ClassHelper.getSimpleName(getClass())
                + ".testLargeRandomStore():");

        // create a random sequence of serialized integer keys
        ReadBuffer[] abKeys = createKeySequence(1000);
        Base.randomize(abKeys);

        for (int c = 0; c < 10; ++c)
            {
            profileStore(abKeys, BINARY_LARGE_VALUE);
            }

        System.out.println();
        }

    @Test
    public void testLargeSequentialStore()
            throws IOException
        {
        System.out.println(ClassHelper.getSimpleName(getClass())
                + ".testLargeSequentialStore():");

        // create a sequence of serialized integer keys
        ReadBuffer[] abKeys = createKeySequence(1000);

        for (int c = 0; c < 10; ++c)
            {
            profileStore(abKeys, BINARY_LARGE_VALUE);
            }

        System.out.println();
        }

    //@Test
    public void testContinuousLoad()
        {
        System.out.println(ClassHelper.getSimpleName(getClass())
                + ".testContinuousLoad():");

        AbstractPersistentStore store = ensurePersistentStore();
        store.ensureExtent(1);

        // create a sequence of serialized integer keys
        ReadBuffer[] abKeys = createKeySequence(1000);

        // populate the store with initial data
        populateStore(store, abKeys, BINARY_MEDIUM_VALUE);

        long ldtStart = System.currentTimeMillis();
        for (int nNew = abKeys.length, c = 0; nNew < Integer.MAX_VALUE; ++nNew)
            {
            ReadBuffer abKeyCreate = convertIntToReadBuffer(nNew);
            ReadBuffer abKeyUpdate = convertIntToReadBuffer(nNew - (abKeys.length / 2));
            ReadBuffer abKeyErase  = convertIntToReadBuffer(nNew - abKeys.length);
            ReadBuffer abValue     = Base.getRandomBinary(BINARY_MEDIUM_VALUE.length(),
                    BINARY_MEDIUM_VALUE.length());

            // store a new mapping
            store.store(1, abKeyCreate, abValue, null);

            // update an existing mapping
            store.store(1, abKeyUpdate, abValue, null);

            // erase an existing mapping
            store.erase(1, abKeyErase, null);

            if (++c == abKeys.length)
                {
                long ldtEnd = System.currentTimeMillis();
                outputResults("continuous", ldtStart, ldtEnd, abKeys.length);

                ldtStart = System.currentTimeMillis();
                c = 0;
                }
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return a store that can be used for tests, creating one if necessary.
     *
     * @return a PersistentStore
     */
    protected AbstractPersistentStore ensurePersistentStore()
        {
        AbstractPersistentStore store = m_store;
        if (store == null)
            {
            Long t = new Date().getTime();

            m_store = store = (AbstractPersistentStore) m_manager.open(
                    GUIDHelper.generateGUID(1, 1L, t, GUIDHelperTest.getMockMember(1)),
                    null);
            }
        return store;
        }

    /**
     * Delete the current test store, if necessary.
     */
    protected void deletePersistentStore()
        {
        AbstractPersistentStore store = m_store;
        if (store != null)
            {
            m_manager.delete(store.getId(), false);
            m_store = null;
            }
        }

    /**
     * Write the given integer to a buffer, returning a read-only view.
     *
     * @param n  the integer to convert to a buffer
     *
     * @return a read only buffer
     */
    protected ReadBuffer convertIntToReadBuffer(int n)
        {
        return new ByteArrayReadBuffer(BitHelper.toBytes(n));
        }

    /**
     * Create and return a sequence of serialized integers: [0..c)
     *
     * @param c  the number of integers in the sequence
     *
     * @return the sequence of serialized integers
     */
    protected ReadBuffer[] createKeySequence(int c)
        {
        if (c < 0)
            {
            throw new IllegalArgumentException("negative sequence size");
            }

        ReadBuffer[] ab = new ReadBuffer[c];
        for (int i = 0; i < c; ++i)
            {
            ab[i] = convertIntToReadBuffer(i);
            }

        return ab;
        }

    /**
     * Populate the given store with the given keys and associated value.
     *
     * @param store     the store to populate
     * @param abKeys    the keys to store
     * @param binValue  the value for each key to store
     */
    protected void populateStore(AbstractPersistentStore store,
            ReadBuffer[] abKeys, Binary binValue)
        {
        store.ensureExtent(1);
        for (int i = 0, c = abKeys.length; i < c; ++i)
            {
            store.store(1, abKeys[i], binValue, null /*oToken*/);
            }
        }

    /**
     * Load the given keys from the given store.
     *
     * @param store   the store to load from
     * @param abKeys  the keys to load
     */
    protected void consumeStore(AbstractPersistentStore store,
            ReadBuffer[] abKeys)
        {
        store.ensureExtent(1);
        for (int i = 0, c = abKeys.length; i < c; ++i)
            {
            store.load(1, abKeys[i]);
            }
        }

    /**
     * Output profiling results.
     *
     * @param sDesc     a short description of the results
     * @param ldtStart  the time the test started
     * @param ldtEnd    the time the test completed
     * @param cOps      the number of operations tested
     */
    protected void outputResults(String sDesc, long ldtStart, long ldtEnd, int cOps)
        {
        long ldtDelta = ldtEnd - ldtStart;
        long cRate    = (long)((cOps * 1000.0)/ldtDelta);
        System.out.println(sDesc + ": " + ldtDelta + "ms, " + cRate + "ops/s");
        }

    /**
     * Profile storing the given keys and associated value.
     *
     * @param abKeys    the keys to store
     * @param binValue  the value for each key to store
     */
    protected void profileStore(ReadBuffer[] abKeys, Binary binValue)
        {
        AbstractPersistentStore store = ensurePersistentStore();
        try
            {
            long ldtStart = System.currentTimeMillis();
            populateStore(store, abKeys, binValue);
            long ldtEnd = System.currentTimeMillis();

            outputResults("store", ldtStart, ldtEnd, abKeys.length);
            }
        finally
            {
            deletePersistentStore();
            }
        }

    /**
     * Profile loading the given keys and associated value.
     *
     * @param abKeys    the keys to load
     * @param binValue  the value for each key to load
     */
    protected void profileLoad(ReadBuffer[] abKeys, Binary binValue)
        {
        AbstractPersistentStore store = ensurePersistentStore();
        try
            {
            populateStore(store, abKeys, binValue);

            long ldtStart = System.currentTimeMillis();
            consumeStore(store, abKeys);
            long ldtEnd = System.currentTimeMillis();

            outputResults("load", ldtStart, ldtEnd, abKeys.length);
            }
        finally
            {
            deletePersistentStore();
            }
        }

    /**
     * Profile iterating a store that contains the given keys and associated
     * values.
     *
     * @param abKeys    the keys to load
     * @param binValue  the value for each key to load
     */
    protected void profileIteration(ReadBuffer[] abKeys, Binary binValue)
        {
        AbstractPersistentStore store = ensurePersistentStore();
        try
            {
            populateStore(store, abKeys, binValue);

            long ldtStart = System.currentTimeMillis();
            store.iterate(new Visitor<ReadBuffer>()
                    {
                    @Override
                    public boolean visit(long lExtentId, ReadBuffer abKey, ReadBuffer binValue)
                        {
                        return true;
                        }
                    });
            long ldtEnd = System.currentTimeMillis();

            outputResults("iteration", ldtStart, ldtEnd, abKeys.length);
            }
        finally
            {
            deletePersistentStore();
            }
        }

    // ----- data members ---------------------------------------------------

    protected File                       m_file;
    protected DaemonPool                 m_pool;
    protected AbstractPersistenceManager m_manager;
    protected AbstractPersistentStore    m_store;

    // ----- constants ------------------------------------------------------

    /**
     * Test Binary values.
     */
    public static final Binary BINARY_SMALL_VALUE  = Base.getRandomBinary(64, 64);         // 64 bytes
    public static final Binary BINARY_MEDIUM_VALUE = Base.getRandomBinary(10240, 10240);   // 10k
    public static final Binary BINARY_LARGE_VALUE  = Base.getRandomBinary(102400, 102400); // 100k
    }
