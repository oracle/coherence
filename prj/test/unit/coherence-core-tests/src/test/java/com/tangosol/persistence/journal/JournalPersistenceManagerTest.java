/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence.journal;

import com.oracle.coherence.persistence.PersistentStoreInfo;

import com.tangosol.io.FileHelper;
import com.tangosol.io.journal2.PartitionJournalConfig;

import com.tangosol.persistence.AbstractPersistenceManager;
import com.tangosol.persistence.AbstractPersistenceManager.AbstractPersistentStore;
import com.tangosol.persistence.AbstractPersistenceManagerTest;
import com.tangosol.persistence.bdb.BerkeleyDBManager;
import com.tangosol.persistence.journal.JournalPersistenceMBeanRegistry.ManagerRole;
import com.tangosol.persistence.journal.JournalPersistenceMBeanRegistry.MBeanRegistrar;

import com.tangosol.util.Binary;

import java.io.File;
import java.io.IOException;

import java.lang.reflect.Field;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for {@link JournalPersistenceManager}.
 *
 * @author rl  2026.03.07
 * @since 26.04
 */
public class JournalPersistenceManagerTest
        extends AbstractPersistenceManagerTest
    {
    // ----- AbstractPersistenceManagerTest methods ------------------------

    @Before
    public void resetSharedRegistry()
        {
        JournalPersistenceMBeanRegistry.resetSharedForTesting();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AbstractPersistenceManager createPersistenceManager()
            throws IOException
        {
        JournalPersistenceManager manager = new JournalPersistenceManager(m_fileData, m_fileTrash, null);
        manager.setJournalConfig(new PartitionJournalConfig().setMaximumFileSize(1024 * 1024));
        manager.setDaemonPool(m_pool);
        return manager;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void corruptPersistentStore(String sId)
            throws IOException
        {
        makePersistentStoreIncompatible(sId);
        }


    // ----- test methods ---------------------------------------------------

    /**
     * Verify multiple stores are created under separate directories.
     */
    @Test
    public void testMultipleStoresHaveIndependentDirectories()
        {
        AbstractPersistenceManager manager = m_manager;

        manager.open(TEST_STORE_ID, null);
        manager.open(TEST_FROM_STORE_ID, null);

        File fileStoreOne = new File(m_fileData, TEST_STORE_ID);
        File fileStoreTwo = new File(m_fileData, TEST_FROM_STORE_ID);

        assertTrue(fileStoreOne.isDirectory());
        assertTrue(fileStoreTwo.isDirectory());
        assertNotEquals(fileStoreOne.getAbsolutePath(), fileStoreTwo.getAbsolutePath());
        }

    /**
     * Verify journal-store emptiness is determined from persisted extent
     * metadata rather than the physical store directory contents.
     */
    @Test
    public void testIsEmptyUsesPersistedExtentMetadata()
        {
        AbstractPersistenceManager manager = m_manager;

        AbstractPersistentStore store = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);

        // Opening a store writes generic metadata, but it still represents an
        // empty partition until an extent is created.
        assertTrue(manager.isEmpty(TEST_STORE_ID));

        manager.close(TEST_STORE_ID);
        assertTrue(manager.isEmpty(TEST_STORE_ID));

        store = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);
        store.ensureExtent(1L);
        assertFalse(manager.isEmpty(TEST_STORE_ID));

        manager.close(TEST_STORE_ID);
        assertFalse(manager.isEmpty(TEST_STORE_ID));
        }

    /**
     * Verify a process failure before promotion publication cannot expose a
     * partially copied store in the live persistence directory.
     *
     * @throws IOException on test failure
     */
    @Test
    public void testInterruptedPromotionDoesNotPublishPartialStore()
            throws IOException
        {
        File fileData  = FileHelper.createTempDir();
        File fileTrash = FileHelper.createTempDir();
        InterruptingJournalPersistenceManager manager = null;
        JournalPersistenceManager             restart = null;

        try
            {
            manager = new InterruptingJournalPersistenceManager(fileData, fileTrash);
            manager.setJournalConfig(new PartitionJournalConfig().setMaximumFileSize(1024 * 1024));

            AbstractPersistentStore storeFrom =
                    (AbstractPersistentStore) manager.open(TEST_FROM_STORE_ID, null);
            Binary binKey   = new Binary(new byte[] {1});
            Binary binValue = new Binary(new byte[] {11});

            storeFrom.ensureExtent(1L);
            storeFrom.store(1L, binKey, binValue, null);

            manager.interruptBeforePublish();
            try
                {
                manager.open(TEST_STORE_ID, storeFrom);
                org.junit.Assert.fail("expected simulated process interruption");
                }
            catch (SimulatedProcessInterruption expected)
                {
                // expected
                }

            assertFalse(new File(fileData, TEST_STORE_ID).exists());

            manager.release();
            manager = null;

            restart = new JournalPersistenceManager(fileData, fileTrash, null);
            restart.setJournalConfig(new PartitionJournalConfig().setMaximumFileSize(1024 * 1024));

            assertThat(storeIds(restart), arrayContainingInAnyOrder(TEST_FROM_STORE_ID));
            AbstractPersistentStore storeRestarted =
                    (AbstractPersistentStore) restart.open(TEST_FROM_STORE_ID, null);
            assertEquals(binValue, storeRestarted.load(1L, binKey));
            }
        finally
            {
            if (manager != null)
                {
                manager.release();
                }
            if (restart != null)
                {
                restart.release();
                }
            FileHelper.deleteDir(fileData);
            FileHelper.deleteDir(fileTrash);
            }
        }

    /**
     * Verify a process failure before publishing a store converted from BDB
     * cannot expose a partially copied journal store.
     *
     * @throws IOException on test failure
     */
    @Test
    public void testInterruptedBdbPromotionDoesNotPublishPartialStore()
            throws IOException
        {
        File fileBdbData     = FileHelper.createTempDir();
        File fileJournalData = FileHelper.createTempDir();
        File fileTrash       = FileHelper.createTempDir();
        BerkeleyDBManager                      managerBdb     = null;
        InterruptingJournalPersistenceManager managerJournal = null;
        JournalPersistenceManager              restart        = null;

        try
            {
            managerBdb = new BerkeleyDBManager(fileBdbData, fileTrash, null);
            AbstractPersistentStore storeFrom =
                    (AbstractPersistentStore) managerBdb.open(TEST_FROM_STORE_ID, null);
            Binary binKey   = new Binary(new byte[] {2});
            Binary binValue = new Binary(new byte[] {22});

            storeFrom.ensureExtent(1L);
            storeFrom.store(1L, binKey, binValue, null);

            managerJournal = new InterruptingJournalPersistenceManager(fileJournalData, fileTrash);
            managerJournal.setJournalConfig(new PartitionJournalConfig().setMaximumFileSize(1024 * 1024));
            managerJournal.interruptBeforePublish();

            try
                {
                managerJournal.open(TEST_STORE_ID, storeFrom);
                org.junit.Assert.fail("expected simulated process interruption");
                }
            catch (SimulatedProcessInterruption expected)
                {
                // expected
                }

            assertFalse(new File(fileJournalData, TEST_STORE_ID).exists());
            assertEquals(binValue, storeFrom.load(1L, binKey));

            managerJournal.release();
            managerJournal = null;

            restart = new JournalPersistenceManager(fileJournalData, fileTrash, null);
            restart.setJournalConfig(new PartitionJournalConfig().setMaximumFileSize(1024 * 1024));

            assertEquals(0, storeIds(restart).length);
            }
        finally
            {
            if (managerBdb != null)
                {
                managerBdb.release();
                }
            if (managerJournal != null)
                {
                managerJournal.release();
                }
            if (restart != null)
                {
                restart.release();
                }
            FileHelper.deleteDir(fileBdbData);
            FileHelper.deleteDir(fileJournalData);
            FileHelper.deleteDir(fileTrash);
            }
        }

    /**
     * Verify one completed promotion cannot remove the shared staging root
     * while another promotion is about to create its private work directory.
     *
     * @throws Exception on test failure
     */
    @Test
    public void testConcurrentPromotionsDoNotDeleteSharedRoot()
            throws Exception
        {
        File fileData  = FileHelper.createTempDir();
        File fileTrash = FileHelper.createTempDir();
        DelayingJournalPersistenceManager manager  = null;
        ExecutorService                   executor = Executors.newSingleThreadExecutor();

        try
            {
            manager = new DelayingJournalPersistenceManager(fileData, fileTrash);
            manager.setJournalConfig(new PartitionJournalConfig().setMaximumFileSize(1024 * 1024));

            String sSourceOne = "source-one";
            String sSourceTwo = "source-two";
            String sTargetOne = "target-one";
            String sTargetTwo = "target-two";
            Binary binKeyOne   = new Binary(new byte[] {3});
            Binary binKeyTwo   = new Binary(new byte[] {4});
            Binary binValueOne = new Binary(new byte[] {33});
            Binary binValueTwo = new Binary(new byte[] {44});

            AbstractPersistentStore storeSourceOne =
                    (AbstractPersistentStore) manager.open(sSourceOne, null);
            AbstractPersistentStore storeSourceTwo =
                    (AbstractPersistentStore) manager.open(sSourceTwo, null);

            storeSourceOne.ensureExtent(1L);
            storeSourceOne.store(1L, binKeyOne, binValueOne, null);
            storeSourceTwo.ensureExtent(1L);
            storeSourceTwo.store(1L, binKeyTwo, binValueTwo, null);

            manager.delayPromotion(sTargetTwo);
            DelayingJournalPersistenceManager managerFinal = manager;
            Future<AbstractPersistentStore> futureTargetTwo = executor.submit(() ->
                    (AbstractPersistentStore) managerFinal.open(sTargetTwo, storeSourceTwo));

            assertTrue("second promotion did not reach the staging boundary",
                    manager.awaitPromotionDelay());

            AbstractPersistentStore storeTargetOne =
                    (AbstractPersistentStore) manager.open(sTargetOne, storeSourceOne);

            manager.continuePromotion();
            AbstractPersistentStore storeTargetTwo = futureTargetTwo.get(30, TimeUnit.SECONDS);

            assertEquals(binValueOne, storeTargetOne.load(1L, binKeyOne));
            assertEquals(binValueTwo, storeTargetTwo.load(1L, binKeyTwo));
            }
        finally
            {
            if (manager != null)
                {
                manager.continuePromotion();
                manager.release();
                }
            executor.shutdownNow();
            FileHelper.deleteDir(fileData);
            FileHelper.deleteDir(fileTrash);
            }
        }

    /**
     * Verify createSnapshot writes expected journal store files.
     *
     * @throws IOException on test failure
     */
    @Test
    public void testCreateSnapshotContainsExpectedFiles()
            throws IOException
        {
        JournalPersistenceManager manager = (JournalPersistenceManager) m_manager;

        AbstractPersistentStore store = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);
        store.ensureExtent(1L);
        store.store(1L, new Binary(new byte[] {1}), new Binary(new byte[] {11}), null);

        File fileSnapshot = FileHelper.createTempDir();
        try
            {
            manager.createSnapshot(fileSnapshot);

            File fileStoreSnapshot = new File(fileSnapshot, TEST_STORE_ID);

            assertTrue(fileStoreSnapshot.isDirectory());
            assertTrue(new File(fileStoreSnapshot, "extents.coh").exists());
            assertTrue(new File(fileStoreSnapshot, "extents").isDirectory());
            assertTrue(hasJournalFiles(fileStoreSnapshot));
            }
        finally
            {
            FileHelper.deleteDir(fileSnapshot);
            }
        }

    /**
     * Verify a snapshot can be opened as a functioning manager and used to
     * restore data.
     *
     * @throws IOException on test failure
     */
    @Test
    public void testCreateSnapshotCanBeOpenedAndRead()
            throws IOException
        {
        JournalPersistenceManager manager = (JournalPersistenceManager) m_manager;

        AbstractPersistentStore store = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);
        store.ensureExtent(1L);

        Binary binKey1 = new Binary(new byte[] {1});
        Binary binVal1 = new Binary(new byte[] {11});
        Binary binKey2 = new Binary(new byte[] {2});
        Binary binVal2 = new Binary(new byte[] {22});

        store.store(1L, binKey1, binVal1, null);
        store.store(1L, binKey2, binVal2, null);

        File fileSnapshot = FileHelper.createTempDir();
        JournalPersistenceManager snapshot = null;
        try
            {
            manager.createSnapshot(fileSnapshot);

            snapshot = new JournalPersistenceManager(fileSnapshot, null, "snapshot");
            snapshot.setJournalConfig(new PartitionJournalConfig().setMaximumFileSize(1024 * 1024));
            snapshot.setDaemonPool(m_pool);

            AbstractPersistentStore storeSnapshot = (AbstractPersistentStore) snapshot.open(TEST_STORE_ID, null);

            assertEquals(binVal1, storeSnapshot.load(1L, binKey1));
            assertEquals(binVal2, storeSnapshot.load(1L, binKey2));
            }
        finally
            {
            if (snapshot != null)
                {
                snapshot.release();
                }
            FileHelper.deleteDir(fileSnapshot);
            }
        }

    /**
     * Verify stores and data are discoverable after manager restart.
     *
     * @throws IOException on test failure
     */
    @Test
    public void testCloseReopenDiscoversStoresAndData()
            throws IOException
        {
        AbstractPersistenceManager manager = m_manager;

        AbstractPersistentStore store1 = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);
        store1.ensureExtent(1L);
        Binary binKey1 = new Binary(new byte[] {1});
        Binary binVal1 = new Binary(new byte[] {11});
        store1.store(1L, binKey1, binVal1, null);

        AbstractPersistentStore store2 = (AbstractPersistentStore) manager.open(TEST_FROM_STORE_ID, null);
        store2.ensureExtent(2L);
        Binary binKey2 = new Binary(new byte[] {2});
        Binary binVal2 = new Binary(new byte[] {22});
        store2.store(2L, binKey2, binVal2, null);

        manager.release();
        m_manager = manager = createPersistenceManager();

        assertThat(storeIds(manager), arrayContainingInAnyOrder(TEST_STORE_ID, TEST_FROM_STORE_ID));

        store1 = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);
        store2 = (AbstractPersistentStore) manager.open(TEST_FROM_STORE_ID, null);

        assertEquals(binVal1, store1.load(1L, binKey1));
        assertEquals(binVal2, store2.load(2L, binKey2));
        }

    /**
     * Verify deleting a store removes its directory and data.
     *
     * @throws IOException on test failure
     */
    @Test
    public void testDeleteStoreRemovesDirectoryAndData()
            throws IOException
        {
        AbstractPersistenceManager manager = m_manager;

        AbstractPersistentStore store = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);
        store.ensureExtent(1L);

        Binary binKey = new Binary(new byte[] {7});
        Binary binVal = new Binary(new byte[] {77});
        store.store(1L, binKey, binVal, null);

        File fileStore = new File(m_fileData, TEST_STORE_ID);
        assertTrue(fileStore.isDirectory());

        manager.delete(TEST_STORE_ID, false);

        assertFalse(fileStore.exists());
        assertEquals(0, storeIds(manager).length);

        store = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);
        store.ensureExtent(1L);
        assertNull(store.load(1L, binKey));
        }

    /**
     * Verify open extent count reflects opened extents across stores and
     * shrinks when stores are closed.
     */
    @Test
    public void testGetOpenExtentCount()
        {
        JournalPersistenceManager manager = (JournalPersistenceManager) m_manager;

        AbstractPersistentStore store1 = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);
        store1.ensureExtent(1L);
        assertEquals(0, manager.getOpenExtentCount());

        store1.store(1L, new Binary(new byte[] {1}), new Binary(new byte[] {11}), null);
        assertEquals(1, manager.getOpenExtentCount());

        AbstractPersistentStore store2 = (AbstractPersistentStore) manager.open(TEST_FROM_STORE_ID, null);
        store2.ensureExtent(2L);
        store2.store(2L, new Binary(new byte[] {2}), new Binary(new byte[] {22}), null);
        assertEquals(2, manager.getOpenExtentCount());

        manager.close(TEST_STORE_ID);
        assertEquals(1, manager.getOpenExtentCount());

        manager.close(TEST_FROM_STORE_ID);
        assertEquals(0, manager.getOpenExtentCount());
        }

    /**
     * Verify compaction progress summaries are non-null and distinguish idle
     * from active local compaction state.
     *
     * @throws Exception on reflection failure
     */
    @Test
    public void testGetCompactionProgressSummary()
            throws Exception
        {
        JournalPersistenceManager manager = (JournalPersistenceManager) m_manager;

        assertEquals("idle open-extents=0 compacting=0 requested=0",
                manager.getCompactionProgressSummary());

        JournalPersistenceManager.JournalPersistentStore store =
                (JournalPersistenceManager.JournalPersistentStore) manager.open(TEST_STORE_ID, null);
        store.ensureExtent(1L);
        store.store(1L, new Binary(new byte[] {1}), new Binary(new byte[] {11}), null);

        assertEquals("idle open-extents=1 compacting=0 requested=0",
                manager.getCompactionProgressSummary());

        Object state = getExtentState(store, 1L);

        setBooleanField(state, "m_fCompactionInProgress", true);
        try
            {
            assertEquals("active open-extents=1 compacting=1 requested=0",
                    manager.getCompactionProgressSummary());
            }
        finally
            {
            setBooleanField(state, "m_fCompactionInProgress", false);
            }
        }

    /**
     * Verify the manager starts with no recovery summary and records one when
     * an existing local extent is recovered.
     */
    @Test
    public void testGetLastRecoverySummary()
        {
        JournalPersistenceManager manager = (JournalPersistenceManager) m_manager;

        assertEquals(RecoverySummary.EMPTY, manager.getLastRecoverySummary());

        AbstractPersistentStore store = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);
        store.ensureExtent(1L);

        Binary binKey = new Binary(new byte[] {1});
        Binary binVal = new Binary(new byte[] {11});

        store.store(1L, binKey, binVal, null);
        manager.close(TEST_STORE_ID);

        long ldtStart = System.currentTimeMillis();

        store = (AbstractPersistentStore) manager.open(TEST_STORE_ID, null);

        assertEquals(binVal, store.load(1L, binKey));

        RecoverySummary summary = manager.getLastRecoverySummary();

        assertNotNull(summary);
        assertFalse(summary.getText().isEmpty());
        assertTrue(summary.getText().contains("Journal recovery summary: extent="));
        assertTrue(summary.getTimestampMillis() >= ldtStart);
        assertFalse(summary.getSourceTag().isEmpty());
        }

    /**
     * Verify MBean registration happens once at the first store-open moment.
     */
    @Test
    public void testMBeanAttachOnFirstActiveMoment()
        {
        JournalPersistenceManager manager   = (JournalPersistenceManager) m_manager;
        TestRegistrar             registrar = new TestRegistrar();

        manager.setMBeanContext("svc", ManagerRole.ACTIVE, registrar);

        manager.open(TEST_STORE_ID, null);
        manager.open(TEST_FROM_STORE_ID, null);

        assertEquals(1, registrar.getRegisterCount());
        assertEquals(0, registrar.getUnregisterCount());
        assertNotNull(registrar.getMBean("svc"));
        }

    /**
     * Verify MBean unregister happens when the manager is released.
     */
    @Test
    public void testMBeanDetachOnRelease()
        {
        JournalPersistenceManager manager   = (JournalPersistenceManager) m_manager;
        TestRegistrar             registrar = new TestRegistrar();

        manager.setMBeanContext("svc", ManagerRole.ACTIVE, registrar);
        manager.open(TEST_STORE_ID, null);

        assertNotNull(registrar.getMBean("svc"));

        manager.release();

        assertEquals(1, registrar.getRegisterCount());
        assertEquals(1, registrar.getUnregisterCount());
        assertNull(registrar.getMBean("svc"));
        }

    /**
     * Verify managers without MBean context do not register an MBean.
     */
    @Test
    public void testMBeanIsSilentForAuxiliaryManagers()
        {
        JournalPersistenceManager manager   = (JournalPersistenceManager) m_manager;
        TestRegistrar             registrar = new TestRegistrar();

        JournalPersistenceMBeanRegistry.shared(registrar);
        manager.open(TEST_STORE_ID, null);
        manager.release();

        assertEquals(0, registrar.getRegisterCount());
        assertEquals(0, registrar.getUnregisterCount());
        }

    /**
     * Verify a role is required when service context is present.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMBeanContextRejectsNullRoleWhenServiceNonNull()
        {
        ((JournalPersistenceManager) m_manager).setMBeanContext("svc", null, new TestRegistrar());
        }

    /**
     * Verify active/backup aggregation survives release in the opposite
     * order from attach.
     *
     * @throws IOException on test setup failure
     */
    @Test
    public void testReleaseInOppositeAttachOrderKeepsMBeanUntilLast()
            throws IOException
        {
        TestRegistrar             registrar = new TestRegistrar();
        JournalPersistenceManager active    = (JournalPersistenceManager) m_manager;
        JournalPersistenceManager backup    = createExtraManager("backup");

        active.setMBeanContext("svc", ManagerRole.ACTIVE, registrar);
        backup.setMBeanContext("svc", ManagerRole.BACKUP, registrar);

        active.open(TEST_STORE_ID, null);
        backup.open(TEST_FROM_STORE_ID, null);

        JournalPersistenceMBean mbean = registrar.getMBean("svc");

        assertNotNull(mbean);
        assertEquals(1, registrar.getRegisterCount());

        active.release();

        assertEquals(0, registrar.getUnregisterCount());
        assertNotNull(registrar.getMBean("svc"));
        assertEquals(backup.getOpenExtentCount(), mbean.getOpenExtentCount());

        backup.release();

        assertEquals(1, registrar.getUnregisterCount());
        assertNull(registrar.getMBean("svc"));
        }


    // ----- helpers --------------------------------------------------------

    /**
     * Create an additional manager rooted at a child temp directory.
     *
     * @param sName  manager name suffix
     *
     * @return the additional manager
     *
     * @throws IOException on setup failure
     */
    private JournalPersistenceManager createExtraManager(String sName)
            throws IOException
        {
        File fileData = new File(m_fileData, sName);

        JournalPersistenceManager manager = new JournalPersistenceManager(fileData, m_fileTrash, sName);
        manager.setJournalConfig(new PartitionJournalConfig().setMaximumFileSize(1024 * 1024));
        manager.setDaemonPool(m_pool);
        return manager;
        }

    /**
     * Return store ids known by the manager.
     *
     * @param manager  manager
     *
     * @return store ids
     */
    private String[] storeIds(AbstractPersistenceManager manager)
        {
        PersistentStoreInfo[] aInfo = manager.listStoreInfo();
        String[]              asId  = new String[aInfo.length];

        for (int i = 0; i < aInfo.length; i++)
            {
            asId[i] = aInfo[i].getId();
            }

        return asId;
        }

    /**
     * Return true if supplied directory contains at least one journal file.
     *
     * @param dir  directory to inspect
     *
     * @return true if journal files are present
     */
    private boolean hasJournalFiles(File dir)
        {
        File[] aFile = dir.listFiles();
        if (aFile == null)
            {
            return false;
            }

        for (File file : aFile)
            {
            if (file.isDirectory())
                {
                if (hasJournalFiles(file))
                    {
                    return true;
                    }
                }
            else if (JOURNAL_FILE_PATTERN.matcher(file.getName()).matches())
                {
                return true;
                }
            }

        return false;
        }

    /**
     * Return the extent state for the supplied extent.
     *
     * @param store      the journal store
     * @param lExtentId  the extent identifier
     *
     * @return the extent state
     *
     * @throws Exception on reflection failure
     */
    private Object getExtentState(JournalPersistenceManager.JournalPersistentStore store, long lExtentId)
            throws Exception
        {
        Field field = store.getClass().getDeclaredField("m_mapExtentState");
        field.setAccessible(true);

        Map<?, ?> mapState = (Map<?, ?>) field.get(store);

        return mapState.get(lExtentId);
        }

    /**
     * Set a boolean field via reflection.
     *
     * @param oTarget  target object
     * @param sField   field name
     * @param fValue   field value
     *
     * @throws Exception on reflection failure
     */
    private void setBooleanField(Object oTarget, String sField, boolean fValue)
            throws Exception
        {
        Field field = oTarget.getClass().getDeclaredField(sField);
        field.setAccessible(true);
        field.setBoolean(oTarget, fValue);
        }


    // ----- inner class: InterruptingJournalPersistenceManager -----------

    /**
     * Journal manager that simulates a hard process stop after staging has
     * completed but before the atomic publication point.
     */
    private static class InterruptingJournalPersistenceManager
            extends JournalPersistenceManager
        {
        private InterruptingJournalPersistenceManager(File fileData, File fileTrash)
                throws IOException
            {
            super(fileData, fileTrash, null);
            }

        private void interruptBeforePublish()
            {
            m_fInterrupt = true;
            }

        @Override
        protected void publishStagedStore(File dirPromotion, File dirStore)
                throws IOException
            {
            if (m_fInterrupt)
                {
                throw new SimulatedProcessInterruption();
                }
            super.publishStagedStore(dirPromotion, dirStore);
            }

        private boolean m_fInterrupt;
        }


    // ----- inner class: DelayingJournalPersistenceManager --------------

    /**
     * Journal manager that pauses one promotion immediately before creating
     * its private staging directory.
     */
    private static class DelayingJournalPersistenceManager
            extends JournalPersistenceManager
        {
        private DelayingJournalPersistenceManager(File fileData, File fileTrash)
                throws IOException
            {
            super(fileData, fileTrash, null);
            }

        private void delayPromotion(String sId)
            {
            m_sDelayedId = sId;
            }

        private boolean awaitPromotionDelay()
                throws InterruptedException
            {
            return f_latchDelayed.await(30, TimeUnit.SECONDS);
            }

        private void continuePromotion()
            {
            f_latchContinue.countDown();
            }

        @Override
        protected File createPromotionDirectory(String sId)
                throws IOException
            {
            if (sId.equals(m_sDelayedId))
                {
                f_latchDelayed.countDown();
                try
                    {
                    if (!f_latchContinue.await(30, TimeUnit.SECONDS))
                        {
                        throw new IOException("timed out waiting to continue promotion");
                        }
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    throw new IOException("interrupted while waiting to continue promotion", e);
                    }
                }

            return super.createPromotionDirectory(sId);
            }

        private volatile String m_sDelayedId;
        private final CountDownLatch f_latchDelayed  = new CountDownLatch(1);
        private final CountDownLatch f_latchContinue = new CountDownLatch(1);
        }


    // ----- inner class: SimulatedProcessInterruption --------------------

    /**
     * Error used to bypass ordinary persistence exception cleanup, matching a
     * process halt at the publication boundary.
     */
    private static class SimulatedProcessInterruption
            extends Error
        {
        }


    // ----- inner class: TestRegistrar ------------------------------------

    private static class TestRegistrar
            implements MBeanRegistrar
        {
        @Override
        public void register(String sName, Object mbean)
            {
            f_cRegister.incrementAndGet();
            f_mapMBeans.put(sName, (JournalPersistenceMBean) mbean);
            }

        @Override
        public void unregister(String sName)
            {
            f_cUnregister.incrementAndGet();
            f_mapMBeans.remove(sName);
            }

        public JournalPersistenceMBean getMBean(String sService)
            {
            return f_mapMBeans.get(JournalPersistenceMBeanRegistry.getMBeanName(sService));
            }

        public int getRegisterCount()
            {
            return f_cRegister.get();
            }

        public int getUnregisterCount()
            {
            return f_cUnregister.get();
            }

        private final Map<String, JournalPersistenceMBean> f_mapMBeans = new ConcurrentHashMap<>();

        private final AtomicInteger f_cRegister = new AtomicInteger();

        private final AtomicInteger f_cUnregister = new AtomicInteger();
        }


    // ----- constants ------------------------------------------------------

    /**
     * Journal file name pattern.
     */
    private static final Pattern JOURNAL_FILE_PATTERN = Pattern.compile("journal-(\\d{6})\\.coh");
    }
