/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence;

import com.oracle.coherence.persistence.PersistenceException;
import com.oracle.coherence.persistence.PersistentStore.Visitor;

import com.tangosol.internal.util.DaemonPool;
import com.tangosol.internal.util.Daemons;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;

import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.persistence.AbstractPersistenceManager.AbstractPersistentStore;

import java.io.File;
import java.io.IOException;

import java.util.Date;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Abstract unit test for an AbstractPersistenceEnvironment subclass.
 *
 * @author jh  2013.05.23
 */
public abstract class AbstractPersistenceEnvironmentTest
    {
    // ----- test lifecycle -------------------------------------------------

    @Before
    public void setupTest()
            throws IOException
        {
        m_fileActive   = FileHelper.createTempDir();
        m_fileSnapshot = FileHelper.createTempDir();
        m_fileTrash    = FileHelper.createTempDir();

        DefaultDaemonPoolDependencies deps = new DefaultDaemonPoolDependencies();
        deps.setThreadCount(1);

        m_pool = Daemons.newDaemonPool(deps);
        m_pool.start();

        m_env = createPersistenceEnvironment();
        }

    @After
    public void teardownTest()
        {
        m_env.release();
        m_pool.stop();

        try
            {
            FileHelper.deleteDir(m_fileActive);
            }
        catch (IOException e)
            {
            // ignore
            }

        try
            {
            FileHelper.deleteDir(m_fileSnapshot);
            }
        catch (IOException e)
            {
            // ignore
            }

        try
            {
            FileHelper.deleteDir(m_fileTrash);
            }
        catch (IOException e)
            {
            // ignore
            }
        }

    protected abstract AbstractPersistenceEnvironment createPersistenceEnvironment() throws IOException;

    // ----- test methods ---------------------------------------------------

    @Test
    public void testEnsurePersistenceException()
        {
        AbstractPersistenceEnvironment env = m_env;

        final RuntimeException eCause = new RuntimeException("cause");

        PersistenceException e = env.ensurePersistenceException(null);
        assertEquals(env, e.getPersistenceEnvironment());
        assertNull(e.getPersistenceManager());
        assertNull(e.getPersistentStore());
        assertNull(e.getMessage());

        e = env.ensurePersistenceException(eCause);
        assertEquals(env, e.getPersistenceEnvironment());
        assertNull(e.getPersistenceManager());
        assertNull(e.getPersistentStore());
        assertEquals(eCause.toString(), e.getMessage());
        assertEquals(eCause, e.getCause());

        e = env.ensurePersistenceException(e);
        assertEquals(env, e.getPersistenceEnvironment());
        assertNull(e.getPersistenceManager());
        assertNull(e.getPersistentStore());
        assertEquals(eCause.toString(), e.getMessage());
        assertEquals(eCause, e.getCause());

        AbstractPersistenceManager manager = (AbstractPersistenceManager) env.openActive();

        e = manager.ensurePersistenceException(null);
        assertEquals(env, e.getPersistenceEnvironment());
        assertEquals(manager, e.getPersistenceManager());
        assertNull(e.getPersistentStore());
        assertNull(e.getMessage());

        e = manager.ensurePersistenceException(eCause);
        assertEquals(env, e.getPersistenceEnvironment());
        assertEquals(manager, e.getPersistenceManager());
        assertNull(e.getPersistentStore());
        assertEquals(eCause.toString(), e.getMessage());
        assertEquals(eCause, e.getCause());

        e = manager.ensurePersistenceException(e);
        assertEquals(env, e.getPersistenceEnvironment());
        assertEquals(manager, e.getPersistenceManager());
        assertNull(e.getPersistentStore());
        assertEquals(eCause.toString(), e.getMessage());
        assertEquals(eCause, e.getCause());

        AbstractPersistentStore store = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);

        e = store.ensurePersistenceException(null);
        assertEquals(env, e.getPersistenceEnvironment());
        assertEquals(manager, e.getPersistenceManager());
        assertEquals(store, e.getPersistentStore());
        assertNull(e.getMessage());

        e = store.ensurePersistenceException(eCause);
        assertEquals(env, e.getPersistenceEnvironment());
        assertEquals(manager, e.getPersistenceManager());
        assertEquals(store, e.getPersistentStore());
        assertEquals(eCause.toString(), e.getMessage());
        assertEquals(eCause, e.getCause());

        e = store.ensurePersistenceException(e);
        assertEquals(env, e.getPersistenceEnvironment());
        assertEquals(manager, e.getPersistenceManager());
        assertEquals(store, e.getPersistentStore());
        assertEquals(eCause.toString(), e.getMessage());
        assertEquals(eCause, e.getCause());
        }

    @Test
    public void testOpenActive()
        {
        AbstractPersistenceEnvironment env = m_env;
        assertEquals(0, env.listSnapshots().length);

        AbstractPersistenceManager manager = (AbstractPersistenceManager) env.openActive();
        assertNotNull(manager);
        assertTrue(manager == env.openActive());

        manager.release();
        assertNull(env.m_managerActive);
        assertNull(manager.m_env);
        }

    @Test
    public void testCreateEmptySnapshot()
        {
        AbstractPersistenceEnvironment env = m_env;
        assertEquals(0, env.listSnapshots().length);

        AbstractPersistenceManager snapshot = (AbstractPersistenceManager)
                env.createSnapshot(TEST_SNAPSHOT, null);
        assertNotNull(snapshot);
        assertArrayEquals(new String[] { TEST_SNAPSHOT }, env.listSnapshots());
        assertTrue(new File(m_fileSnapshot, TEST_SNAPSHOT).isDirectory());
        assertEquals(TEST_SNAPSHOT, snapshot.getName());
        assertEquals(0, snapshot.list().length);

        try
            {
            env.createSnapshot(TEST_SNAPSHOT, null);
            fail();
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }

        snapshot.release();
        assertNull(snapshot.m_env);
        assertTrue(env.f_mapSnapshots.isEmpty());
        assertArrayEquals(new String[] { TEST_SNAPSHOT }, env.listSnapshots());
        }

    @Test
    public void testCreateActiveSnapshot()
            throws IOException
        {
        AbstractPersistenceEnvironment env = m_env;
        assertEquals(0, env.listSnapshots().length);

        AbstractPersistenceManager manager = (AbstractPersistenceManager) env.openActive();
        assertNotNull(manager);

        AbstractPersistentStore store = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);
        store.ensureExtent(1L);
        store.store(1L,
                AbstractPersistentStoreTest.BINARY_KEY_1,
                AbstractPersistentStoreTest.BINARY_VALUE_1,
                null);

        String[] as = manager.list();
        assertEquals(1, as.length);

        AbstractPersistenceManager snapshot = (AbstractPersistenceManager)
                env.createSnapshot(TEST_SNAPSHOT, manager);

        File fileSnapshot = new File(m_fileSnapshot, TEST_SNAPSHOT);
        assertNotNull(snapshot);
        assertArrayEquals(new String[] { TEST_SNAPSHOT }, env.listSnapshots());
        assertTrue(fileSnapshot.isDirectory());
        assertEquals(TEST_SNAPSHOT, snapshot.getName());
        assertEquals(1, snapshot.list().length);

        // assert that the PersistentStore metadata file exists
        Properties prop = manager.readMetadata(new File(fileSnapshot, TEST_STORE_ID));
        assertEquals(String.valueOf(manager.getImplVersion()),
                prop.getProperty(CachePersistenceHelper.META_IMPL_VERSION));
        assertEquals(manager.getStorageFormat(),
                prop.getProperty(CachePersistenceHelper.META_STORAGE_FORMAT));
        assertEquals(String.valueOf(manager.getStorageVersion()),
                prop.getProperty(CachePersistenceHelper.META_STORAGE_VERSION));

        TestVisitor visitor = new TestVisitor();
        snapshot.open(TEST_STORE_ID, null).iterate(visitor);
        assertEquals(1, visitor.getVisitCount());

        snapshot.release();
        assertNull(snapshot.m_env);
        assertTrue(env.f_mapSnapshots.isEmpty());
        assertArrayEquals(new String[] { TEST_SNAPSHOT }, env.listSnapshots());
            }

    @Test
    public void testCreateActiveSnapshotPartitioned()
            throws IOException
            {
        AbstractPersistenceEnvironment env = m_env;
        assertEquals(0, env.listSnapshots().length);

        AbstractPersistenceManager manager = (AbstractPersistenceManager) env.openActive();
        assertNotNull(manager);

        AbstractPersistentStore store = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);
        store.ensureExtent(1L);
        store.store(1L,
                AbstractPersistentStoreTest.BINARY_KEY_1,
                AbstractPersistentStoreTest.BINARY_VALUE_1,
                null);

        String[] as = manager.list();
        assertEquals(1, as.length);

        AbstractPersistenceManager snapshot = (AbstractPersistenceManager)
                env.createSnapshot(TEST_SNAPSHOT);

        snapshot.open(store.getId(), store, /*collector*/ null);

        File fileSnapshot = new File(m_fileSnapshot, TEST_SNAPSHOT);
        assertNotNull(snapshot);
        assertArrayEquals(new String[] { TEST_SNAPSHOT }, env.listSnapshots());
        assertTrue(fileSnapshot.isDirectory());
        assertEquals(TEST_SNAPSHOT, snapshot.getName());
        assertEquals(1, snapshot.list().length);

        // assert that the PersistentStore metadata file exists
        Properties prop = manager.readMetadata(new File(fileSnapshot, TEST_STORE_ID));
        assertEquals(String.valueOf(manager.getImplVersion()),
                prop.getProperty(CachePersistenceHelper.META_IMPL_VERSION));
        assertEquals(manager.getStorageFormat(),
                prop.getProperty(CachePersistenceHelper.META_STORAGE_FORMAT));
        assertEquals(String.valueOf(manager.getStorageVersion()),
                prop.getProperty(CachePersistenceHelper.META_STORAGE_VERSION));

        TestVisitor visitor = new TestVisitor();
        snapshot.open(TEST_STORE_ID, null).iterate(visitor);
        assertEquals(1, visitor.getVisitCount());

        snapshot.release();
        assertNull(snapshot.m_env);
        assertTrue(env.f_mapSnapshots.isEmpty());
        assertArrayEquals(new String[] { TEST_SNAPSHOT }, env.listSnapshots());
        }

    @Test
    public void testOpenSnapshot()
        {
        AbstractPersistenceEnvironment env = m_env;
        assertEquals(0, env.listSnapshots().length);

        AbstractPersistenceManager manager = (AbstractPersistenceManager) env.openActive();
        assertNotNull(manager);

        AbstractPersistentStore store = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);
        store.ensureExtent(1L);
        store.store(1L,
                AbstractPersistentStoreTest.BINARY_KEY_1,
                AbstractPersistentStoreTest.BINARY_VALUE_1,
                null);

        String[] as = manager.list();
        assertEquals(1, as.length);

        AbstractPersistenceManager snapshot = (AbstractPersistenceManager)
                env.createSnapshot(TEST_SNAPSHOT, manager);

        snapshot.release();
        assertNull(snapshot.m_env);
        assertTrue(env.f_mapSnapshots.isEmpty());

        snapshot = (AbstractPersistenceManager) env.openSnapshot(TEST_SNAPSHOT);
        assertNotNull(snapshot);
        assertArrayEquals(new String[] { TEST_SNAPSHOT }, env.listSnapshots());
        assertTrue(new File(m_fileSnapshot, TEST_SNAPSHOT).isDirectory());
        assertEquals(TEST_SNAPSHOT, snapshot.getName());
        assertEquals(1, snapshot.list().length);

        TestVisitor visitor = new TestVisitor();
        snapshot.open(TEST_STORE_ID, null).iterate(visitor);
        assertEquals(1, visitor.getVisitCount());

        try
            {
            env.createSnapshot(TEST_SNAPSHOT, null);
            fail();
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }

        snapshot.release();
        assertNull(snapshot.m_env);
        assertTrue(env.f_mapSnapshots.isEmpty());
        assertArrayEquals(new String[] { TEST_SNAPSHOT }, env.listSnapshots());
        }

    @Test
    public void testRemoveSnapshot()
        {
        AbstractPersistenceEnvironment env = m_env;
        assertEquals(0, env.listSnapshots().length);

        AbstractPersistenceManager snapshot = (AbstractPersistenceManager)
                env.createSnapshot(TEST_SNAPSHOT, null);
        assertNotNull(snapshot);
        assertArrayEquals(new String[] { TEST_SNAPSHOT }, env.listSnapshots());
        assertTrue(new File(m_fileSnapshot, TEST_SNAPSHOT).isDirectory());
        assertEquals(TEST_SNAPSHOT, snapshot.getName());
        assertEquals(0, snapshot.list().length);

        try
            {
            env.createSnapshot(TEST_SNAPSHOT, null);
            fail();
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }

        env.removeSnapshot(TEST_SNAPSHOT);
        assertFalse(new File(m_fileSnapshot, TEST_SNAPSHOT).isDirectory());

        snapshot.release();
        assertNull(snapshot.m_env);
        assertTrue(env.f_mapSnapshots.isEmpty());
        assertEquals(0, env.listSnapshots().length);
        }

    // ----- TestVisitor inner class ----------------------------------------

    public static class TestVisitor
            implements Visitor<ReadBuffer>
        {
        @Override
        public boolean visit(long lExtentId, ReadBuffer bufKey, ReadBuffer bufValue)
            {
            m_cVisits++;
            return true;
            }

        public int getVisitCount()
            {
            return m_cVisits;
            }

        private int m_cVisits;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The name of the test snapshot.
     */
    public static final String TEST_SNAPSHOT = "snapshot-1";

    /**
     * The name of the test persistent store identifier.
     */
    public static String TEST_STORE_ID = GUIDHelper.generateGUID(1, 1L,
                                                                 new Date().getTime(), GUIDHelperTest.getMockMember(1));

    // ----- data members ---------------------------------------------------

    protected File                           m_fileActive;
    protected File                           m_fileSnapshot;
    protected File                           m_fileTrash;
    protected DaemonPool                     m_pool;
    protected AbstractPersistenceEnvironment m_env;
    }
