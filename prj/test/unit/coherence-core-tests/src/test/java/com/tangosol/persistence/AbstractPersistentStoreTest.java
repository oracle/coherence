/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence;

import com.oracle.coherence.common.base.Collector;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.persistence.AsyncPersistenceException;
import com.oracle.coherence.persistence.PersistentStore;

import com.tangosol.internal.util.DaemonPool;
import com.tangosol.internal.util.Daemons;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;

import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.persistence.AbstractPersistenceManager.AbstractPersistentStore;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.SimpleMapEntry;

import java.io.File;
import java.io.IOException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Abstract unit test for an AbstractPersistentStore subclass.
 *
 * @author jh  2012.06.13
 */
public abstract class AbstractPersistentStoreTest
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
        m_store   = (AbstractPersistentStore) m_manager.open(TEST_STORE_ID, null);
        }

    @After
    public void teardownTest()
        {
        m_manager.delete(TEST_STORE_ID, false);
        m_manager.release();
        m_pool.stop();

        try
            {
            FileHelper.deleteDir(m_file);
            }
        catch (IOException e)
            {
            // ignore
            }
        }

    protected abstract AbstractPersistenceManager createPersistenceManager()
            throws IOException;

    // ----- test methods ---------------------------------------------------

    @Test
    public void testGetDataDirectory()
        {
        assertEquals(new File(m_file, TEST_STORE_ID), m_store.getDataDirectory());
        }

    @Test
    public void testExtentLifecycle()
            throws IOException
        {
        AbstractPersistenceManager  manager = m_manager;
        AbstractPersistentStore     store   = m_store;

        assertEquals(0, store.extents().length);

        assertTrue(store.ensureExtent(1));
        assertFalse(store.ensureExtent(1));

        store.store(1, BINARY_KEY_1, BINARY_VALUE_1, null);
        store.store(1, BINARY_KEY_2, BINARY_VALUE_2, null);

        assertArrayEquals(new long[] {1}, store.extents());

        manager.release();
        m_manager = manager = createPersistenceManager();
        store = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);

        assertFalse(store.ensureExtent(1));
        assertArrayEquals(new long[] {1}, store.extents());

        store.moveExtent(1L, 2L);
        assertArrayEquals(new long[] {2}, store.extents());

        store.deleteExtent(2);
        assertEquals(0, store.extents().length);

        manager.release();
        m_manager = manager = createPersistenceManager();
        store = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);

        assertEquals(0, store.extents().length);
        }

    @Test
    public void testLoad()
        {
        AbstractPersistentStore store = m_store;

        // assert that a load in an unknown extent throws an exception
        try
            {
            store.load(1, BINARY_KEY_1);
            }
        catch (RuntimeException e)
            {
            // expected
            }
        store.ensureExtent(1);

        // assert that a load returns null for a new database
        assertNull(store.load(1, BINARY_KEY_1));
        }

    @Test
    public void testStore()
            throws IOException
        {
        AbstractPersistentStore store = m_store;

        // assert that a store in an unknown extent throws an exception
        try
            {
            store.store(1, BINARY_KEY_1, BINARY_VALUE_1, null);
            }
        catch (RuntimeException e)
            {
            // expected
            }
        store.ensureExtent(1);

        // assert that the store doesn't contain the key to be stored
        assertEquals(null, store.load(1, BINARY_KEY_1));

        // assert that a stored value is accessible after an auto-commit
        store.store(1, BINARY_KEY_1, BINARY_VALUE_1, null);
        assertEquals(BINARY_VALUE_1, store.load(1, BINARY_KEY_1));

        // close the store and assert that the value is still accessible
        m_manager.close(TEST_STORE_ID);
        m_store = store = (AbstractPersistentStore) m_manager.open(TEST_STORE_ID, null);
        assertEquals(BINARY_VALUE_1, store.load(1, BINARY_KEY_1));
        }

    @Test
    public void testStoreTx()
            throws IOException
        {
        AbstractPersistentStore store = m_store;
        store.ensureExtent(1);

        // assert that a store doesn't take effect until a commit
        Object oToken = store.begin();
        store.store(1, BINARY_KEY_1, BINARY_VALUE_1, oToken);
        store.abort(oToken);
        assertEquals(null, store.load(1, BINARY_KEY_1));

        // close the store and assert that the value still isn't accessible
        m_manager.close(TEST_STORE_ID);
        m_store = store = (AbstractPersistentStore) m_manager.open(TEST_STORE_ID, null);
        assertEquals(null, store.load(1, BINARY_KEY_1));

        // assert that a stored value is accessible after checkpoint
        oToken = store.begin();
        store.store(1, BINARY_KEY_1, BINARY_VALUE_1, oToken);
        store.commit(oToken);
        assertEquals(BINARY_VALUE_1, store.load(1, BINARY_KEY_1));

        // close the store and assert that the value is still accessible
        m_manager.close(TEST_STORE_ID);
        m_store = store = (AbstractPersistentStore) m_manager.open(TEST_STORE_ID, null);
        assertEquals(BINARY_VALUE_1, store.load(1, BINARY_KEY_1));
        }

    @Test
    public void testStoreTxAsync()
            throws IOException
        {
        AbstractPersistentStore store = m_store;
        store.ensureExtent(1);

        TestCollector collector = new TestCollector();

        // assert that a store doesn't take effect until a commit
        Object oReceipt = Integer.valueOf(7);
        Object oToken   = store.begin(collector, oReceipt);
        store.store(1, BINARY_KEY_1, BINARY_VALUE_1, oToken);
        store.abort(oToken);
        Object oValue = collector.waitForFlush();
        assertTrue(oValue instanceof AsyncPersistenceException);
        AsyncPersistenceException e = (AsyncPersistenceException) oValue;
        assertNull(e.getPersistenceEnvironment());
        assertEquals(m_manager, e.getPersistenceManager());
        assertEquals(m_store, e.getPersistentStore());
        assertEquals(oReceipt, e.getReceipt());
        assertEquals(null, store.load(1, BINARY_KEY_1));

        // close the store and assert that the value still isn't accessible
        m_manager.close(TEST_STORE_ID);
        m_store = store = (AbstractPersistentStore) m_manager.open(TEST_STORE_ID, null);
        assertEquals(null, store.load(1, BINARY_KEY_1));

        // assert that a stored value is accessible after checkpoint
        oToken = store.begin(collector, null);
        store.store(1, BINARY_KEY_1, BINARY_VALUE_1, oToken);
        store.commit(oToken);
        oValue = collector.waitForFlush();
        assertNull(oValue);
        assertEquals(BINARY_VALUE_1, store.load(1, BINARY_KEY_1));

        // close the store and assert that the value is still accessible
        m_manager.close(TEST_STORE_ID);
        m_store = store = (AbstractPersistentStore) m_manager.open(TEST_STORE_ID, null);
        assertEquals(BINARY_VALUE_1, store.load(1, BINARY_KEY_1));
        }

    @Test
    public void testErase()
            throws IOException
        {
        AbstractPersistentStore store = m_store;

        // assert that an erase in an unknown extent throws an exception
        try
            {
            store.erase(1, BINARY_KEY_1, null);
            }
        catch (RuntimeException e)
            {
            // expected
            }
        store.ensureExtent(1);

        store.store(1, BINARY_KEY_1, BINARY_VALUE_1, null);
        assertEquals(BINARY_VALUE_1, store.load(1, BINARY_KEY_1));

        // assert that an erase takes effect after an auto-commit
        store.erase(1, BINARY_KEY_1, null);
        assertEquals(null, store.load(1, BINARY_KEY_1));

        // close the store and assert that the value still isn't accessible
        m_manager.close(TEST_STORE_ID);
        m_store = store = (AbstractPersistentStore) m_manager.open(TEST_STORE_ID, null);
        assertEquals(null, store.load(1, BINARY_KEY_1));
        }

    @Test
    public void testEraseTx()
            throws IOException
        {
        AbstractPersistentStore store = m_store;
        store.ensureExtent(1);

        store.store(1, BINARY_KEY_1, BINARY_VALUE_1, null);
        assertEquals(BINARY_VALUE_1, store.load(1, BINARY_KEY_1));

        // assert that an erase doesn't take effect until a commit
        Object oToken = store.begin();
        store.erase(1, BINARY_KEY_1, oToken);
        store.abort(oToken);
        assertEquals(BINARY_VALUE_1, store.load(1, BINARY_KEY_1));

        // close the store and assert that the value is still accessible
        m_manager.close(TEST_STORE_ID);
        m_store = store = (AbstractPersistentStore) m_manager.open(TEST_STORE_ID, null);
        assertEquals(BINARY_VALUE_1, store.load(1, BINARY_KEY_1));

        // assert that an erase takes effect after a commit
        oToken = store.begin();
        store.erase(1, BINARY_KEY_1, oToken);
        store.commit(oToken);
        assertEquals(null, store.load(1, BINARY_KEY_1));

        // close the store and assert that the value still isn't accessible
        m_manager.close(TEST_STORE_ID);
        m_store = store = (AbstractPersistentStore) m_manager.open(TEST_STORE_ID, null);
        assertEquals(null, store.load(1, BINARY_KEY_1));
        }

    @Test
    public void testEraseTxAsync()
            throws IOException
        {
        AbstractPersistentStore store = m_store;
        store.ensureExtent(1);

        TestCollector collector = new TestCollector();

        store.store(1, BINARY_KEY_1, BINARY_VALUE_1, null);
        assertEquals(BINARY_VALUE_1, store.load(1, BINARY_KEY_1));

        // assert that an erase doesn't take effect until a commit
        Object oReceipt = Integer.valueOf(7);
        Object oToken   = store.begin(collector, oReceipt);
        store.erase(1, BINARY_KEY_1, oToken);
        store.abort(oToken);
        Object oValue = collector.waitForFlush();
        assertTrue(oValue instanceof AsyncPersistenceException);
        AsyncPersistenceException e = (AsyncPersistenceException) oValue;
        assertNull(e.getPersistenceEnvironment());
        assertEquals(m_manager, e.getPersistenceManager());
        assertEquals(m_store, e.getPersistentStore());
        assertEquals(oReceipt, e.getReceipt());
        assertEquals(BINARY_VALUE_1, store.load(1, BINARY_KEY_1));

        // close the store and assert that the value is still accessible
        m_manager.close(TEST_STORE_ID);
        m_store = store = (AbstractPersistentStore) m_manager.open(TEST_STORE_ID, null);
        assertEquals(BINARY_VALUE_1, store.load(1, BINARY_KEY_1));

        // assert that an erase takes effect after a commit
        oToken = store.begin(collector, null);
        store.erase(1, BINARY_KEY_1, oToken);
        store.commit(oToken);
        oValue = collector.waitForFlush();
        assertNull(oValue);
        assertEquals(null, store.load(1, BINARY_KEY_1));

        // close the store and assert that the value still isn't accessible
        m_manager.close(TEST_STORE_ID);
        m_store = store = (AbstractPersistentStore) m_manager.open(TEST_STORE_ID, null);
        assertEquals(null, store.load(1, BINARY_KEY_1));
        }

    @Test
    public void testIteration()
        {
        AbstractPersistentStore store = m_store;
        store.ensureExtent(1);
        store.ensureExtent(2);
        store.ensureExtent(Long.MAX_VALUE);

        store.store(1, BINARY_KEY_1, BINARY_VALUE_1, null);
        assertEquals(BINARY_VALUE_1, store.load(1, BINARY_KEY_1));

        store.store(2, BINARY_KEY_2, BINARY_VALUE_2, null);
        assertEquals(BINARY_VALUE_2, store.load(2, BINARY_KEY_2));

        store.store(Long.MAX_VALUE, BINARY_KEY_N, BINARY_VALUE_N, null);
        assertEquals(BINARY_VALUE_N, store.load(Long.MAX_VALUE, BINARY_KEY_N));

        for (int i = 0; i < 2; ++i)
            {
            // second run: close and reopen the store
            if (i == 1)
                {
                m_manager.close(TEST_STORE_ID);
                m_store = store = (AbstractPersistentStore) m_manager.open(TEST_STORE_ID, null);
                }

            // test full iteration
            final Map<Long, Map.Entry<ReadBuffer, ReadBuffer>> map = new HashMap<Long, Map.Entry<ReadBuffer, ReadBuffer>>();
            store.iterate(new PersistentStore.Visitor<ReadBuffer>()
                    {
                    @Override
                    public boolean visit(long lExtentId, ReadBuffer bufKey, ReadBuffer bufValue)
                        {
                        map.put(Long.valueOf(lExtentId),
                                new SimpleMapEntry(bufKey, bufValue));
                        return true;
                        }
                    });

            assertEquals(3, map.size());
            assertEquals(new SimpleMapEntry(BINARY_KEY_1, BINARY_VALUE_1),
                    map.get(Long.valueOf(1)));
            assertEquals(new SimpleMapEntry(BINARY_KEY_2, BINARY_VALUE_2),
                    map.get(Long.valueOf(2)));
            assertEquals(new SimpleMapEntry(BINARY_KEY_N, BINARY_VALUE_N),
                    map.get(Long.valueOf(Long.MAX_VALUE)));

            // test aborted iteration
            final Set<Long> set = new HashSet<Long>();
            store.iterate(new PersistentStore.Visitor<ReadBuffer>()
                    {
                    @Override
                    public boolean visit(long lExtentId, ReadBuffer bufKey, ReadBuffer bufValue)
                        {
                        set.add(Long.valueOf(lExtentId));
                        return false;
                        }
                    });

            assertEquals(1, set.size());
            }
        }

    // ----- inner class: TestCollector -------------------------------------

    /**
     * Collector implementation used for asynchronous tests.
     */
    public static class TestCollector
            implements Collector<Object>
        {
        @Override
        public synchronized void add(Object oValue)
            {
            m_oValue = oValue;
            }

        @Override
        public synchronized void flush()
            {
            f_flushed = true;
            notifyAll();
            }

        public synchronized Object waitForFlush()
            {
            try
                {
                while (!f_flushed)
                    {
                    Blocking.wait(this);
                    }
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw Base.ensureRuntimeException(e);
                }

            f_flushed = false;

            Object oValue = m_oValue;
            m_oValue = null;
            return oValue;
            }

        private boolean f_flushed;
        private Object  m_oValue;
        }

    // ----- data members ---------------------------------------------------

    protected File                       m_file;
    protected DaemonPool                 m_pool;
    protected AbstractPersistenceManager m_manager;
    protected AbstractPersistentStore    m_store;

    // ----- constants ------------------------------------------------------

    /**
     * The name of the test persistent store identifier.
     */
    public static String TEST_STORE_ID = AbstractPersistenceEnvironmentTest.TEST_STORE_ID;

    /**
     * Test Binary keys.
     */
    public static final Binary BINARY_KEY_1 = new Binary(new byte[] {0, 1, 2, 3, 4, 5, 6, 7});
    public static final Binary BINARY_KEY_2 = new Binary(new byte[] {7, 6, 5, 4, 3, 2, 1, 0});
    public static final Binary BINARY_KEY_N = new Binary(new byte[] {1, 1, 1, 1, 1, 1, 1, 1});

    /**
     * Test Binary values.
     */
    public static final Binary BINARY_VALUE_1 = new Binary(new byte[] {7, 6, 5, 4, 3, 2, 1, 7, 6, 5, 4, 3, 2, 1});
    public static final Binary BINARY_VALUE_2 = new Binary(new byte[] {1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7});
    public static final Binary BINARY_VALUE_N = new Binary(new byte[] {2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3});
    }
