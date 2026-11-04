/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence.journal;

import com.oracle.coherence.persistence.PersistentStore;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.io.ReadBuffer;
import com.tangosol.io.journal2.PartitionJournal;
import com.tangosol.io.journal2.PartitionJournalConfig;

import com.tangosol.persistence.AbstractPersistenceManager;
import com.tangosol.persistence.AbstractPersistenceManager.AbstractPersistentStore;
import com.tangosol.persistence.AbstractPersistentStoreTest;

import com.tangosol.util.Binary;

import com.tangosol.io.FileHelper;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;

import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

/**
 * Unit tests for {@link JournalPersistenceManager.JournalPersistentStore}.
 *
 * @author rl  2026.03.07
 * @since 26.04
 */
public class JournalPersistentStoreTest
        extends AbstractPersistentStoreTest
    {
    // ----- AbstractPersistentStoreTest methods ----------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected AbstractPersistenceManager createPersistenceManager()
            throws IOException
        {
        JournalPersistenceManager manager = new JournalPersistenceManager(m_file, null, "test-journal");
        manager.setJournalConfig(new PartitionJournalConfig().setMaximumFileSize(1024 * 1024));
        manager.setDaemonPool(m_pool);
        return manager;
        }


    // ----- test methods ---------------------------------------------------

    /**
     * Verify deleting one extent removes only that extent's data.
     *
     * @throws IOException on test failure
     */
    @Test
    public void testDeleteExtentRemovesOnlyTargetExtent()
            throws IOException
        {
        AbstractPersistentStore store = m_store;

        store.ensureExtent(1L);
        store.ensureExtent(2L);

        Binary binKey11 = new Binary(new byte[] {1, 1});
        Binary binVal11 = new Binary(new byte[] {11});
        Binary binKey12 = new Binary(new byte[] {1, 2});
        Binary binVal12 = new Binary(new byte[] {12});
        Binary binKey21 = new Binary(new byte[] {2, 1});
        Binary binVal21 = new Binary(new byte[] {21});

        store.store(1L, binKey11, binVal11, null);
        store.store(1L, binKey12, binVal12, null);
        store.store(2L, binKey21, binVal21, null);

        store.deleteExtent(1L);

        assertFalse(store.containsExtent(1L));
        assertTrue(store.containsExtent(2L));
        assertEquals(binVal21, store.load(2L, binKey21));

        m_manager.close(TEST_STORE_ID);
        m_store = store = (AbstractPersistentStore) m_manager.open(TEST_STORE_ID, null);

        assertFalse(store.containsExtent(1L));
        assertTrue(store.containsExtent(2L));
        assertEquals(binVal21, store.load(2L, binKey21));

        store.ensureExtent(1L);
        assertNull(store.load(1L, binKey11));
        assertNull(store.load(1L, binKey12));
        }

    /**
     * Verify truncating an extent clears data but keeps the extent.
     */
    @Test
    public void testTruncateExtentClearsButPreservesExtent()
        {
        AbstractPersistentStore store = m_store;

        store.ensureExtent(1L);

        Binary binKey1 = new Binary(new byte[] {1});
        Binary binVal1 = new Binary(new byte[] {11});
        Binary binKey2 = new Binary(new byte[] {2});
        Binary binVal2 = new Binary(new byte[] {22});

        store.store(1L, binKey1, binVal1, null);
        store.store(1L, binKey2, binVal2, null);

        store.truncateExtent(1L);

        assertTrue(store.containsExtent(1L));
        assertNull(store.load(1L, binKey1));
        assertNull(store.load(1L, binKey2));

        Set<Long> setExtents = toExtentSet(store.extents());
        assertTrue(setExtents.contains(1L));

        Binary binVal3 = new Binary(new byte[] {33});
        store.store(1L, binKey1, binVal3, null);
        assertEquals(binVal3, store.load(1L, binKey1));
        }

    /**
     * Verify close/reopen recovery restores all entries written before checkpoint.
     *
     * @throws IOException on test failure
     */
    @Test
    public void testRecoveryFromCheckpoint()
            throws IOException
        {
        AbstractPersistentStore store = m_store;

        store.ensureExtent(1L);

        Binary binKey1 = new Binary(new byte[] {1});
        Binary binVal1 = new Binary(new byte[] {10});
        Binary binKey2 = new Binary(new byte[] {2});
        Binary binVal2 = new Binary(new byte[] {20});
        Binary binKey3 = new Binary(new byte[] {3});
        Binary binVal3 = new Binary(new byte[] {30});

        store.store(1L, binKey1, binVal1, null);
        store.store(1L, binKey2, binVal2, null);
        store.store(1L, binKey3, binVal3, null);

        m_manager.close(TEST_STORE_ID);
        m_store = store = (AbstractPersistentStore) m_manager.open(TEST_STORE_ID, null);

        assertEquals(binVal1, store.load(1L, binKey1));
        assertEquals(binVal2, store.load(1L, binKey2));
        assertEquals(binVal3, store.load(1L, binKey3));
        }

    /**
     * Verify iteration over multiple extents yields expected tuples.
     */
    @Test
    public void testMultipleExtentsIteration()
        {
        AbstractPersistentStore store = m_store;

        store.ensureExtent(1L);
        store.ensureExtent(2L);
        store.ensureExtent(3L);

        Binary binKey11 = new Binary(new byte[] {1, 1});
        Binary binVal11 = new Binary(new byte[] {11});
        Binary binKey21 = new Binary(new byte[] {2, 1});
        Binary binVal21 = new Binary(new byte[] {21});
        Binary binKey31 = new Binary(new byte[] {3, 1});
        Binary binVal31 = new Binary(new byte[] {31});

        store.store(1L, binKey11, binVal11, null);
        store.store(2L, binKey21, binVal21, null);
        store.store(3L, binKey31, binVal31, null);

        Map<Binary, Long>   mapKeyToExtent = new HashMap<>();
        Map<Binary, Binary> mapKeyToValue  = new HashMap<>();

        store.iterate(new PersistentStore.Visitor<ReadBuffer>()
            {
            @Override
            public boolean visit(long lExtentId, ReadBuffer bufKey, ReadBuffer bufValue)
                {
                Binary binKey = bufKey.toBinary();

                mapKeyToExtent.put(binKey, lExtentId);
                mapKeyToValue.put(binKey, bufValue.toBinary());
                return true;
                }
            });

        assertEquals(3, mapKeyToExtent.size());
        assertEquals(Long.valueOf(1L), mapKeyToExtent.get(binKey11));
        assertEquals(Long.valueOf(2L), mapKeyToExtent.get(binKey21));
        assertEquals(Long.valueOf(3L), mapKeyToExtent.get(binKey31));

        assertEquals(binVal11, mapKeyToValue.get(binKey11));
        assertEquals(binVal21, mapKeyToValue.get(binKey21));
        assertEquals(binVal31, mapKeyToValue.get(binKey31));
        }

    /**
     * Verify recovery materialization streams final live entries without
     * changing normal iterate semantics.
     */
    @Test
    public void testIterateRecoveryMaterialized()
        {
        JournalPersistenceManager.JournalPersistentStore store =
                (JournalPersistenceManager.JournalPersistentStore) m_store;

        store.ensureExtent(1L);

        Binary binKeyA = new Binary(new byte[] {10});
        Binary binKeyB = new Binary(new byte[] {20});
        Binary binKeyC = new Binary(new byte[] {30});

        Binary binValA1 = new Binary(new byte[] {1});
        Binary binValA2 = new Binary(new byte[] {2});
        Binary binValB  = new Binary(new byte[] {3});
        Binary binValC  = new Binary(new byte[] {4});

        store.store(1L, binKeyA, binValA1, null);
        store.store(1L, binKeyB, binValB, null);
        store.store(1L, binKeyA, binValA2, null);
        store.store(1L, binKeyC, binValC, null);
        store.erase(1L, binKeyB, null);

        List<Binary> listVisited = new ArrayList<>();
        Map<Binary, Binary> mapValue = new HashMap<>();

        store.iterateRecoveryMaterialized(new PersistentStore.Visitor<ReadBuffer>()
            {
            @Override
            public boolean visit(long lExtentId, ReadBuffer bufKey, ReadBuffer bufValue)
                {
                Binary binKey = bufKey.toBinary();
                listVisited.add(binKey);
                mapValue.put(binKey, bufValue.toBinary());
                return true;
                }
            });

        assertEquals(2, listVisited.size());
        assertTrue(listVisited.contains(binKeyA));
        assertTrue(listVisited.contains(binKeyC));
        assertEquals(binValA2, mapValue.get(binKeyA));
        assertEquals(binValC, mapValue.get(binKeyC));
        assertFalse(mapValue.containsKey(binKeyB));
        }

    /**
     * Verify recovery materialization leaves reopened source extents cold.
     *
     * @throws Exception on test failure
     */
    @Test
    public void testIterateRecoveryMaterializedLeavesSourceExtentsCold()
            throws Exception
        {
        JournalPersistenceManager.JournalPersistentStore store =
                (JournalPersistenceManager.JournalPersistentStore) m_store;

        store.ensureExtent(1L);

        Binary binKey1   = new Binary(new byte[] {1});
        Binary binKey2   = new Binary(new byte[] {2});
        Binary binValue1 = new Binary(new byte[] {11});
        Binary binValue2 = new Binary(new byte[] {22});

        store.store(1L, binKey1, binValue1, null);
        store.store(1L, binKey2, binValue2, null);

        m_manager.close(TEST_STORE_ID);
        m_store = store = (JournalPersistenceManager.JournalPersistentStore) m_manager.open(TEST_STORE_ID, null);

        assertEquals(0, getExtentStateCount(store));

        Map<Binary, Binary> mapRecovered = new HashMap<>();
        store.iterateRecoveryMaterialized(new PersistentStore.Visitor<ReadBuffer>()
            {
            @Override
            public boolean visit(long lExtentId, ReadBuffer bufKey, ReadBuffer bufValue)
                {
                mapRecovered.put(bufKey.toBinary(), bufValue.toBinary());
                return true;
                }
            });

        assertEquals(binValue1, mapRecovered.get(binKey1));
        assertEquals(binValue2, mapRecovered.get(binKey2));
        assertEquals(0, getExtentStateCount(store));

        store.moveExtents(new long[] {1L}, new long[] {2L});

        assertFalse(store.containsExtent(1L));
        assertTrue(store.containsExtent(2L));
        assertEquals(binValue1, store.load(2L, binKey1));
        assertEquals(binValue2, store.load(2L, binKey2));
        }

    /**
     * Verify the same user key can exist independently in multiple extents.
     *
     * @throws IOException on test failure
     */
    @Test
    public void testSameKeyAcrossExtents()
            throws IOException
        {
        AbstractPersistentStore store = m_store;

        store.ensureExtent(1L);
        store.ensureExtent(2L);

        Binary binKey   = new Binary(new byte[] {9, 9});
        Binary binVal1  = new Binary(new byte[] {11});
        Binary binVal2  = new Binary(new byte[] {22});
        Binary binVal2b = new Binary(new byte[] {23});

        store.store(1L, binKey, binVal1, null);
        store.store(2L, binKey, binVal2, null);

        assertEquals(binVal1, store.load(1L, binKey));
        assertEquals(binVal2, store.load(2L, binKey));

        m_manager.close(TEST_STORE_ID);
        m_store = store = (AbstractPersistentStore) m_manager.open(TEST_STORE_ID, null);

        assertEquals(binVal1, store.load(1L, binKey));
        assertEquals(binVal2, store.load(2L, binKey));

        store.erase(1L, binKey, null);
        assertNull(store.load(1L, binKey));
        assertEquals(binVal2, store.load(2L, binKey));

        store.store(2L, binKey, binVal2b, null);
        assertEquals(binVal2b, store.load(2L, binKey));

        store.deleteExtent(2L);
        store.ensureExtent(2L);
        assertNull(store.load(2L, binKey));
        }


    /**
     * Verify that periodic checkpointing occurs when the bytes-written
     * threshold is exceeded, and that all data survives close/reopen.
     *
     * @throws IOException on test failure
     */
    @Test
    public void testPeriodicCheckpoint()
            throws IOException
        {
        File fileDir = FileHelper.createTempDir();
        try
            {
            PartitionJournalConfig config = new PartitionJournalConfig()
                    .setMaximumFileSize(1024 * 1024)
                    .setCheckpointBytesThreshold(512)
                    .setAdaptiveCheckpointInitialWrites(0);

            JournalPersistenceManager manager =
                    new JournalPersistenceManager(fileDir, null, "test-ckpt");
            manager.setJournalConfig(config);
            manager.setDaemonPool(m_pool);

            AbstractPersistentStore store =
                    (AbstractPersistentStore) manager.open("partition-0", null);
            store.ensureExtent(1L);
            File fileStoreDir = store.getDataDirectory();

            byte[] abValue = new byte[128];
            int    cTotal  = 15;

            for (int i = 0; i < 10; i++)
                {
                store.store(1L, toKey(i), new Binary(abValue), null);
                }

            Eventually.assertDeferred(() -> containsFileNamed(fileStoreDir, "checkpoint.coh"),
                    is(true), within(5, TimeUnit.SECONDS));

            for (int i = 10; i < cTotal; i++)
                {
                store.store(1L, toKey(i), new Binary(abValue), null);
                }

            manager.close("partition-0");
            store = (AbstractPersistentStore) manager.open("partition-0", null);

            for (int i = 0; i < cTotal; i++)
                {
                assertNotNull("entry " + i + " should be recoverable",
                        store.load(1L, toKey(i)));
                }

            manager.close("partition-0");
            manager.release();
            }
        finally
            {
            FileHelper.deleteDir(fileDir);
            }
        }

    /**
     * A failed shutdown checkpoint must not retain any extent's file handles.
     *
     * @throws Exception on test failure
     */
    @Test
    public void testCloseReleasesHandlesAfterCheckpointFailure()
            throws Exception
        {
        assertCloseReleasesHandlesAfterFailure(false);
        }

    /**
     * A failed extent-list write must not prevent closing the extent journals.
     *
     * @throws Exception on test failure
     */
    @Test
    public void testCloseReleasesHandlesAfterMetadataFailure()
            throws Exception
        {
        assertCloseReleasesHandlesAfterFailure(true);
        }

    /**
     * Async checkpoint file writes must obey the same I/O lock as synchronous
     * writers without preventing foreground mutations while that lock is busy.
     *
     * @throws Exception on test failure
     */
    @Test
    public void testAsyncCheckpointSerializesFileWrite()
            throws Exception
        {
        m_store.ensureExtent(1L);
        m_store.store(1L, toKey(1), new Binary(new byte[] {42}), null);
        Object state    = getExtentState((JournalPersistenceManager.JournalPersistentStore) m_store, 1L);
        Object snapshot = invokeMethod(state, "captureCheckpointSnapshot", new Class<?>[] {String.class}, "periodic");
        Lock   lock     = (Lock) getFieldValue(state, "f_lockCheckpointWrite");
        File   file     = new File((File) getFieldValue(state, "m_dirExtent"), "checkpoint.coh");
        PartitionJournalConfig config = (PartitionJournalConfig) getFieldValue(m_manager, "m_journalConfig");
        long cbThreshold = config.getCheckpointBytesThreshold();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread thread = new Thread(() ->
            {
            try
                {
                invokeMethod(state, "runAsyncCheckpoint", new Class<?>[] {snapshot.getClass()}, snapshot);
                }
            catch (Throwable t)
                {
                error.set(t);
                }
            }, "JournalPersistentStoreTest-checkpoint-writer");
        thread.setDaemon(true);
        lock.lock();
        try
            {
            synchronized (getFieldValue(state, "f_checkpointLock"))
                {
                setBooleanField(state, "m_fCheckpointInProgress", true);
                }
            thread.start();
            Eventually.assertDeferred(thread::getState, is(Thread.State.WAITING), within(5, TimeUnit.SECONDS));
            assertFalse("checkpoint must not be written outside the I/O lock", file.exists());
            config.setCheckpointBytesThreshold(1L);
            m_store.store(1L, toKey(2), new Binary(new byte[] {43}), null);
            }
        finally
            {
            config.setCheckpointBytesThreshold(cbThreshold);
            lock.unlock();
            thread.join(TimeUnit.SECONDS.toMillis(5));
            }
        assertFalse("checkpoint writer should finish", thread.isAlive());
        assertNull("checkpoint writer should succeed", error.get());
        assertTrue("checkpoint should now exist", file.isFile());
        assertFalse(getBooleanFieldValue(state, "m_fCheckpointInProgress"));
        }

    /**
     * Close must suppress late follow-up work and drain existing tasks even
     * when interrupted, preserving the interrupt status after cleanup.
     *
     * @throws Exception on test failure
     */
    @Test
    public void testCloseQuiescesBackgroundWorkWhenInterrupted()
            throws Exception
        {
        m_store.ensureExtent(1L);
        m_store.store(1L, toKey(1), new Binary(new byte[] {42}), null);
        Object state = getExtentState((JournalPersistenceManager.JournalPersistentStore) m_store, 1L);
        Object checkpointLock = getFieldValue(state, "f_checkpointLock");
        Object compactionLock = getFieldValue(state, "f_compactionLock");
        PartitionJournalConfig config = (PartitionJournalConfig) getFieldValue(m_manager, "m_journalConfig");
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicReference<Boolean> interrupted = new AtomicReference<>();
        synchronized (checkpointLock)
            {
            setBooleanField(state, "m_fCheckpointInProgress", true);
            }
        synchronized (compactionLock)
            {
            setBooleanField(state, "m_fCompactionInProgress", true);
            }
        Thread thread = new Thread(() ->
            {
            try
                {
                m_manager.close(TEST_STORE_ID);
                interrupted.set(Thread.currentThread().isInterrupted());
                }
            catch (Throwable t)
                {
                error.set(t);
                }
            }, "JournalPersistentStoreTest-interrupted-close");
        thread.setDaemon(true);
        thread.start();
        try
            {
            Eventually.assertDeferred(() -> getBooleanFieldValue(state, "m_fBackgroundWorkSuspended"),
                    is(true), within(5, TimeUnit.SECONDS));
            thread.interrupt();
            thread.join(100L);
            assertTrue("interruption must not skip the checkpoint drain", thread.isAlive());
            synchronized (checkpointLock)
                {
                setBooleanField(state, "m_fCheckpointInProgress", false);
                checkpointLock.notifyAll();
                }
            config.setCheckpointBytesThreshold(1L);
            invokeMethod(state, "maybeCheckpoint", new Class<?>[0]);
            invokeMethod(state, "requestCompaction", new Class<?>[0]);
            assertFalse("close must reject follow-up checkpoints",
                    getBooleanFieldValue(state, "m_fCheckpointInProgress"));
            assertFalse("close must reject follow-up compaction",
                    getBooleanFieldValue(state, "m_fCompactionRequested"));
            assertNotNull("journal must stay open until compaction drains", getFieldValue(state, "m_journal"));
            }
        finally
            {
            synchronized (checkpointLock)
                {
                setBooleanField(state, "m_fCheckpointInProgress", false);
                checkpointLock.notifyAll();
                }
            synchronized (compactionLock)
                {
                setBooleanField(state, "m_fCompactionInProgress", false);
                compactionLock.notifyAll();
                }
            thread.join(TimeUnit.SECONDS.toMillis(5));
            }
        assertFalse("close should complete", thread.isAlive());
        assertNull("close should succeed", error.get());
        assertEquals("close should restore the interrupt status", Boolean.TRUE, interrupted.get());
        assertNull("journal should be closed", getFieldValue(state, "m_journal"));
        m_store = (AbstractPersistentStore) m_manager.open(TEST_STORE_ID, null);
        assertNotNull(m_store.load(1L, toKey(1)));
        }

    /**
     * Snapshot-capture failure must release the checkpoint reservation.
     *
     * @throws Exception on test failure
     */
    @Test
    public void testCheckpointCaptureFailureClearsReservation()
            throws Exception
        {
        m_store.ensureExtent(1L);
        m_store.store(1L, toKey(1), new Binary(new byte[] {42}), null);
        Object state = getExtentState((JournalPersistenceManager.JournalPersistentStore) m_store, 1L);
        PartitionJournalConfig config = (PartitionJournalConfig) getFieldValue(m_manager, "m_journalConfig");
        config.setCheckpointBytesThreshold(1L);
        Field journalField = state.getClass().getDeclaredField("m_journal");
        journalField.setAccessible(true);
        PartitionJournal journal = (PartitionJournal) journalField.get(state);
        PartitionJournal failing = spy(journal);
        doThrow(new IllegalStateException("injected capture failure")).when(failing).getCurrentFileId();
        try
            {
            // fail after reservation, rather than at the scheduler's initial guard
            journalField.set(state, failing);
            invokeMethod(state, "maybeCheckpoint", new Class<?>[0]);
            assertFalse("capture failure must release the reservation",
                    getBooleanFieldValue(state, "m_fCheckpointInProgress"));
            }
        finally
            {
            journalField.set(state, journal);
            Object lock = getFieldValue(state, "f_checkpointLock");
            synchronized (lock)
                {
                setBooleanField(state, "m_fCheckpointInProgress", false);
                lock.notifyAll();
                }
            }
        }

    /**
     * Verify periodic checkpointing can be triggered by the adaptive
     * write-count threshold even when the byte threshold is not reached.
     *
     * @throws IOException on test failure
     */
    @Test
    public void testAdaptiveWriteCountCheckpoint()
            throws IOException
        {
        File fileDir = FileHelper.createTempDir();
        try
            {
            PartitionJournalConfig config = new PartitionJournalConfig()
                    .setMaximumFileSize(1024 * 1024)
                    .setCheckpointBytesThreshold(1024 * 1024)
                    .setAdaptiveCheckpointInitialWrites(2L)
                    .setAdaptiveCheckpointMinWrites(2L)
                    .setAdaptiveCheckpointMaxWrites(2L)
                    .setAdaptiveCheckpointTargetMillis(1000.0d)
                    .setAdaptiveCheckpointWarmupCount(0);

            JournalPersistenceManager manager =
                    new JournalPersistenceManager(fileDir, null, "test-ckpt-adaptive");
            manager.setJournalConfig(config);
            manager.setDaemonPool(m_pool);

            AbstractPersistentStore store =
                    (AbstractPersistentStore) manager.open("partition-0", null);
            store.ensureExtent(1L);
            File fileStoreDir = store.getDataDirectory();

            Binary binValue = new Binary(new byte[32]);

            store.store(1L, toKey(1), binValue, null);
            store.store(1L, toKey(2), binValue, null);

            Eventually.assertDeferred(() -> containsFileNamed(fileStoreDir, "checkpoint.coh"),
                    is(true), within(5, TimeUnit.SECONDS));

            manager.close("partition-0");
            manager.release();
            }
        finally
            {
            FileHelper.deleteDir(fileDir);
            }
        }

    /**
     * Verify a successful checkpoint reclaims older full journal files that
     * are no longer referenced by the radix tree.
     *
     * @throws IOException on test failure
     */
    @Test
    public void testCheckpointReclaimsUnreferencedFullFiles()
            throws IOException
        {
        File fileDir = FileHelper.createTempDir();
        try
            {
            PartitionJournalConfig config = new PartitionJournalConfig()
                    .setMaximumFileSize(4 * 1024)
                    .setCheckpointBytesThreshold(4 * 1024)
                    .setAdaptiveCheckpointInitialWrites(0);

            JournalPersistenceManager manager =
                    new JournalPersistenceManager(fileDir, null, "test-ckpt-reclaim");
            manager.setJournalConfig(config);
            manager.setDaemonPool(m_pool);

            AbstractPersistentStore store =
                    (AbstractPersistentStore) manager.open("partition-0", null);
            store.ensureExtent(1L);

            File   fileExtent = extentDir(store.getDataDirectory(), 1L);
            Binary binKey     = toKey(1);
            byte[] abValue    = new byte[3_000];

            for (int i = 0; i < 4; i++)
                {
                store.store(1L, binKey, new Binary(abValue), null);
                }

            Eventually.assertDeferred(() -> containsFileNamed(fileExtent, "checkpoint.coh"),
                    is(true), within(5, TimeUnit.SECONDS));
            Eventually.assertDeferred(() -> countJournalFiles(fileExtent) <= 2,
                    is(true), within(5, TimeUnit.SECONDS));

            manager.close("partition-0");
            store = (AbstractPersistentStore) manager.open("partition-0", null);

            assertNotNull(store.load(1L, binKey));

            manager.close("partition-0");
            manager.release();
            }
        finally
            {
            FileHelper.deleteDir(fileDir);
            }
        }

    /**
     * Verify checkpoint-driven reclamation keeps older full files that are
     * still referenced by the current tree.
     *
     * @throws IOException on test failure
     */
    @Test
    public void testCheckpointRetainsReferencedFullFiles()
            throws IOException
        {
        File fileDir = FileHelper.createTempDir();
        try
            {
            PartitionJournalConfig config = new PartitionJournalConfig()
                    .setMaximumFileSize(4 * 1024)
                    .setCheckpointBytesThreshold(5 * 1024)
                    .setAdaptiveCheckpointInitialWrites(0);

            JournalPersistenceManager manager =
                    new JournalPersistenceManager(fileDir, null, "test-ckpt-retain");
            manager.setJournalConfig(config);
            manager.setDaemonPool(m_pool);

            AbstractPersistentStore store =
                    (AbstractPersistentStore) manager.open("partition-0", null);
            store.ensureExtent(1L);

            File   fileExtent = extentDir(store.getDataDirectory(), 1L);
            Binary binKey1    = toKey(1);
            Binary binKey2    = toKey(2);
            Binary binValue1  = new Binary(new byte[3_000]);
            Binary binValue2  = new Binary(new byte[3_000]);

            store.store(1L, binKey1, binValue1, null);
            store.store(1L, binKey2, binValue2, null);

            Eventually.assertDeferred(() -> containsFileNamed(fileExtent, "checkpoint.coh"),
                    is(true), within(5, TimeUnit.SECONDS));
            Eventually.assertDeferred(() -> countJournalFiles(fileExtent) >= 2,
                    is(true), within(5, TimeUnit.SECONDS));

            manager.close("partition-0");
            store = (AbstractPersistentStore) manager.open("partition-0", null);

            assertEquals(binValue1, store.load(1L, binKey1));
            assertEquals(binValue2, store.load(1L, binKey2));

            manager.close("partition-0");
            manager.release();
            }
        finally
            {
            FileHelper.deleteDir(fileDir);
            }
        }

    /**
     * Verify an explicit checkpoint reclaims older full journal files that are
     * no longer referenced by the radix tree.
     *
     * @throws Exception on test failure
     */
    @Test
    public void testManualCheckpointReclaimsUnreferencedFullFiles()
            throws Exception
        {
        File fileDir = FileHelper.createTempDir();
        try
            {
            PartitionJournalConfig config = new PartitionJournalConfig()
                    .setMaximumFileSize(4 * 1024)
                    .setCheckpointBytesThreshold(0);

            JournalPersistenceManager manager =
                    new JournalPersistenceManager(fileDir, null, "test-manual-ckpt-reclaim");
            manager.setJournalConfig(config);
            manager.setDaemonPool(m_pool);

            AbstractPersistentStore store =
                    (AbstractPersistentStore) manager.open("partition-0", null);
            store.ensureExtent(1L);

            File   fileExtent = extentDir(store.getDataDirectory(), 1L);
            Binary binKey     = toKey(1);
            byte[] abValue    = new byte[3_000];

            for (int i = 0; i < 4; i++)
                {
                store.store(1L, binKey, new Binary(abValue), null);
                }

            assertTrue("expected multiple journal files before checkpoint",
                    countJournalFiles(fileExtent) > 1);

            Object state = getExtentState((JournalPersistenceManager.JournalPersistentStore) store, 1L);
            Method method = state.getClass().getDeclaredMethod("writeCheckpoint");
            method.setAccessible(true);
            method.invoke(state);

            assertTrue(containsFileNamed(fileExtent, "checkpoint.coh"));
            assertTrue("expected only the live journal file and optional active successor to remain",
                    countJournalFiles(fileExtent) <= 2);

            manager.close("partition-0");
            store = (AbstractPersistentStore) manager.open("partition-0", null);

            assertNotNull(store.load(1L, binKey));

            manager.close("partition-0");
            manager.release();
            }
        finally
            {
            FileHelper.deleteDir(fileDir);
            }
        }

    /**
     * Verify a successful checkpoint can trigger background compaction for a
     * partially live older file.
     *
     * @throws Exception on test failure
     */
    @Test
    public void testCheckpointSchedulesCompactionForPartiallyLiveFile()
            throws Exception
        {
        File fileDir = FileHelper.createTempDir();
        try
            {
            PartitionJournalConfig config = new PartitionJournalConfig()
                    .setMaximumFileSize(2304)
                    .setCheckpointBytesThreshold(0);

            JournalPersistenceManager manager =
                    new JournalPersistenceManager(fileDir, null, "test-ckpt-compact");
            manager.setJournalConfig(config);
            manager.setDaemonPool(m_pool);

            AbstractPersistentStore store =
                    (AbstractPersistentStore) manager.open("partition-0", null);
            store.ensureExtent(1L);

            File   fileExtent = extentDir(store.getDataDirectory(), 1L);
            File   fileOld    = new File(fileExtent, "journal-000001.coh");
            Binary binKey1    = toKey(1);
            Binary binKey2    = toKey(2);
            Binary binKey3    = toKey(3);
            Binary binValue1  = new Binary(new byte[400]);
            Binary binValue2  = new Binary(new byte[400]);
            Binary binValue3  = new Binary(new byte[400]);
            Binary binValue4  = new Binary(new byte[400]);
            Binary binValue5  = new Binary(new byte[400]);
            Binary binValue6  = new Binary(new byte[400]);

            store.store(1L, binKey1, binValue1, null);
            store.store(1L, binKey2, binValue2, null);
            store.store(1L, binKey1, binValue3, null);
            store.store(1L, binKey1, binValue4, null);
            store.store(1L, binKey2, binValue5, null);
            store.store(1L, binKey3, binValue6, null);

            assertTrue("expected original partially live file to exist", fileOld.isFile());

            Object state = getExtentState((JournalPersistenceManager.JournalPersistentStore) store, 1L);
            Method method = state.getClass().getDeclaredMethod("writeCheckpoint");
            method.setAccessible(true);
            method.invoke(state);

            Eventually.assertDeferred(() -> containsFileNamed(fileExtent, "checkpoint.coh"),
                    is(true), within(5, TimeUnit.SECONDS));
            Eventually.assertDeferred(fileOld::exists,
                    is(false), within(5, TimeUnit.SECONDS));

            manager.close("partition-0");
            store = (AbstractPersistentStore) manager.open("partition-0", null);

            assertEquals(binValue4, store.load(1L, binKey1));
            assertEquals(binValue5, store.load(1L, binKey2));
            assertEquals(binValue6, store.load(1L, binKey3));

            manager.close("partition-0");
            manager.release();
            }
        finally
            {
            FileHelper.deleteDir(fileDir);
            }
        }

    /**
     * Verify the next checkpoint can compact a FULL file that contains both
     * live entries and superseded tickets.
     *
     * @throws Exception on test failure
     */
    @Test
    public void testNextCheckpointCompactsFullFileWithSupersededTickets()
            throws Exception
        {
        File fileDir = FileHelper.createTempDir();
        try
            {
            PartitionJournalConfig config = new PartitionJournalConfig()
                    .setMaximumFileSize(2304)
                    .setCheckpointBytesThreshold(0);

            JournalPersistenceManager manager =
                    new JournalPersistenceManager(fileDir, null, "test-ckpt-compact-superseded");
            manager.setJournalConfig(config);
            manager.setDaemonPool(m_pool);

            AbstractPersistentStore store =
                    (AbstractPersistentStore) manager.open("partition-0", null);
            store.ensureExtent(1L);

            File   fileExtent = extentDir(store.getDataDirectory(), 1L);
            File   fileOld    = new File(fileExtent, "journal-000001.coh");
            File   fileNew    = new File(fileExtent, "journal-000002.coh");
            Binary binKey1    = toKey(1);
            Binary binKey2    = toKey(2);
            Binary binKey3    = toKey(3);
            Binary binValue1  = new Binary(new byte[400]);
            Binary binValue2  = new Binary(new byte[400]);
            Binary binValue3  = new Binary(new byte[400]);
            Binary binValue4  = new Binary(new byte[400]);
            Binary binValue5  = new Binary(new byte[400]);
            Binary binValue6  = new Binary(new byte[400]);
            Binary binValue7  = new Binary(new byte[400]);

            store.store(1L, binKey1, binValue1, null);
            store.store(1L, binKey2, binValue2, null);
            store.store(1L, binKey1, binValue3, null);
            store.store(1L, binKey2, binValue4, null);
            store.store(1L, binKey3, binValue5, null);
            store.store(1L, binKey3, binValue6, null);
            store.store(1L, binKey1, binValue7, null);

            assertTrue("expected older full file to exist", fileOld.isFile());
            assertTrue("expected later journal file to exist", fileNew.isFile());

            Object state = getExtentState((JournalPersistenceManager.JournalPersistentStore) store, 1L);
            Method method = state.getClass().getDeclaredMethod("writeCheckpoint");
            method.setAccessible(true);
            method.invoke(state);

            Eventually.assertDeferred(() -> containsFileNamed(fileExtent, "checkpoint.coh"),
                    is(true), within(5, TimeUnit.SECONDS));
            Eventually.assertDeferred(fileOld::exists,
                    is(false), within(5, TimeUnit.SECONDS));

            manager.close("partition-0");
            store = (AbstractPersistentStore) manager.open("partition-0", null);

            assertEquals(binValue7, store.load(1L, binKey1));
            assertEquals(binValue4, store.load(1L, binKey2));
            assertEquals(binValue6, store.load(1L, binKey3));

            manager.close("partition-0");
            manager.release();
            }
        finally
            {
            FileHelper.deleteDir(fileDir);
            }
        }

    /**
     * Verify truncate waits for any in-flight async checkpoint to complete
     * before tearing down the extent.
     *
     * @throws Exception on test failure
     */
    @Test
    public void testTruncateWaitsForCheckpointCompletion()
            throws Exception
        {
        JournalPersistenceManager.JournalPersistentStore store =
                (JournalPersistenceManager.JournalPersistentStore) m_store;

        Binary binKey   = new Binary(new byte[] {1});
        Binary binValue = new Binary(new byte[] {42});

        store.ensureExtent(1L);
        store.store(1L, binKey, binValue, null);

        Object state = getExtentState(store, 1L);
        assertWaitsForCheckpointCompletion(state, "truncate", () -> store.truncateExtent(1L));
        assertTrue("extent should remain after truncate", store.containsExtent(1L));
        assertNull("truncated extent should not retain prior entries", store.load(1L, binKey));
        }

    /**
     * Verify store close waits for any in-flight async checkpoint to
     * complete before releasing extent state.
     *
     * @throws Exception on test failure
     */
    @Test
    public void testCloseWaitsForCheckpointCompletion()
            throws Exception
        {
        JournalPersistenceManager.JournalPersistentStore store =
                (JournalPersistenceManager.JournalPersistentStore) m_store;

        Binary binKey   = new Binary(new byte[] {1});
        Binary binValue = new Binary(new byte[] {42});

        store.ensureExtent(1L);
        store.store(1L, binKey, binValue, null);

        Object state = getExtentState(store, 1L);

        assertWaitsForCheckpointCompletion(state, "close", () -> m_manager.close(TEST_STORE_ID));

        m_store = (AbstractPersistentStore) m_manager.open(TEST_STORE_ID, null);
        }

    /**
     * Verify move waits for any in-flight async checkpoint to complete
     * before moving the extent directory.
     *
     * @throws Exception on test failure
     */
    @Test
    public void testMoveExtentWaitsForCheckpointCompletion()
            throws Exception
        {
        JournalPersistenceManager.JournalPersistentStore store =
                (JournalPersistenceManager.JournalPersistentStore) m_store;

        Binary binKey   = new Binary(new byte[] {1});
        Binary binValue = new Binary(new byte[] {42});

        store.ensureExtent(1L);
        store.store(1L, binKey, binValue, null);

        Object state = getExtentState(store, 1L);

        assertWaitsForCheckpointCompletion(state, "move",
                () -> store.moveExtents(new long[] {1L}, new long[] {2L}));

        assertFalse("source extent should be removed after move", store.containsExtent(1L));
        assertTrue("destination extent should exist after move", store.containsExtent(2L));
        assertEquals("moved extent should retain its data", binValue, store.load(2L, binKey));
        }

    /**
     * Verify delete waits for any in-flight async checkpoint to complete
     * before removing the extent directory.
     *
     * @throws Exception on test failure
     */
    @Test
    public void testDeleteExtentWaitsForCheckpointCompletion()
            throws Exception
        {
        JournalPersistenceManager.JournalPersistentStore store =
                (JournalPersistenceManager.JournalPersistentStore) m_store;

        Binary binKey   = new Binary(new byte[] {1});
        Binary binValue = new Binary(new byte[] {42});

        store.ensureExtent(1L);
        store.store(1L, binKey, binValue, null);

        Object state = getExtentState(store, 1L);
        File   fileExtent = (File) getFieldValue(state, "m_dirExtent");
        Object lock = getFieldValue(state, "f_checkpointLock");

        synchronized (lock)
            {
            setBooleanField(state, "m_fCheckpointInProgress", true);
            }

        store.deleteExtent(1L);

        Thread.sleep(250L);
        assertTrue("extent directory should remain while checkpoint is in flight", fileExtent.exists());
        assertEquals("extent state should remain registered while delete is blocked",
                1, getExtentStateCount(store));

        synchronized (lock)
            {
            setBooleanField(state, "m_fCheckpointInProgress", false);
            lock.notifyAll();
            }

        Eventually.assertDeferred(fileExtent::exists, is(false), within(5, TimeUnit.SECONDS));
        Eventually.assertDeferred(() -> getExtentStateCountUnchecked(store), is(0), within(5, TimeUnit.SECONDS));
        }

    /**
     * Verify async checkpoint write failure does not leave the extent stuck
     * and journal replay can still recover the data.
     *
     * @throws Exception on test failure
     */
    @Test
    public void testCheckpointWriteFailureRecoversGracefully()
            throws Exception
        {
        File fileDir = FileHelper.createTempDir();
        try
            {
            PartitionJournalConfig config = new PartitionJournalConfig()
                    .setMaximumFileSize(1024 * 1024)
                    .setCheckpointBytesThreshold(1024L);

            JournalPersistenceManager manager =
                    new JournalPersistenceManager(fileDir, null, "test-ckpt-failure");
            manager.setJournalConfig(config);
            manager.setDaemonPool(m_pool);

            JournalPersistenceManager.JournalPersistentStore store =
                    (JournalPersistenceManager.JournalPersistentStore) manager.open("partition-0", null);
            store.ensureExtent(1L);

            Binary binKey   = new Binary(new byte[] {1});
            Binary binValue = new Binary(new byte[] {7, 7, 7});

            store.store(1L, binKey, binValue, null);

            Object state = getExtentState(store, 1L);
            File   fileExtent = (File) getFieldValue(state, "m_dirExtent");
            File   fileCheckpoint = new File(fileExtent, "checkpoint.coh");

            assertTrue(fileExtent.isDirectory());
            assertTrue("failed to create blocking checkpoint directory", fileCheckpoint.mkdir());

            config.setCheckpointBytesThreshold(1L);
            store.store(1L, binKey, binValue, null);

            Eventually.assertDeferred(() -> getBooleanFieldValue(state, "m_fCheckpointInProgress"),
                    is(false), within(5, TimeUnit.SECONDS));
            assertEquals(binValue, store.load(1L, binKey));

            assertTrue("failed to remove blocking checkpoint directory", fileCheckpoint.delete());

            manager.close("partition-0");
            store = (JournalPersistenceManager.JournalPersistentStore) manager.open("partition-0", null);

            assertEquals(binValue, store.load(1L, binKey));

            manager.close("partition-0");
            manager.release();
            }
        finally
            {
            FileHelper.deleteDir(fileDir);
            }
        }

    /**
     * Verify a late async checkpoint completion after extent close does not
     * fail the worker thread.
     *
     * @throws Exception on test failure
     */
    @Test
    public void testAsyncCheckpointFinalizeAfterCloseIsIgnored()
            throws Exception
        {
        File fileDir = FileHelper.createTempDir();
        try
            {
            PartitionJournalConfig config = new PartitionJournalConfig()
                    .setMaximumFileSize(1024 * 1024)
                    .setCheckpointBytesThreshold(1024L);

            JournalPersistenceManager manager =
                    new JournalPersistenceManager(fileDir, null, "test-ckpt-late-finalize");
            manager.setJournalConfig(config);
            manager.setDaemonPool(m_pool);

            JournalPersistenceManager.JournalPersistentStore store =
                    (JournalPersistenceManager.JournalPersistentStore) manager.open("partition-0", null);
            store.ensureExtent(1L);
            store.store(1L, new Binary(new byte[] {1}), new Binary(new byte[] {9, 9, 9}), null);

            Object state = getExtentState(store, 1L);
            Object snapshot = invokeMethod(state, "captureCheckpointSnapshot",
                    new Class<?>[] {String.class}, "periodic");

            setBooleanField(state, "m_fCheckpointInProgress", true);
            invokeMethod(state, "closeInternal", new Class<?>[0]);

            invokeMethod(state, "runAsyncCheckpoint",
                    new Class<?>[] {snapshot.getClass()}, snapshot);

            assertFalse("checkpoint should no longer be in progress",
                    getBooleanFieldValue(state, "m_fCheckpointInProgress"));

            manager.release();
            }
        finally
            {
            FileHelper.deleteDir(fileDir);
            }
        }

    /**
     * Verify adaptive checkpointing backs off the write trigger when the
     * median checkpoint duration exceeds the configured budget.
     *
     * @throws Exception on test failure
     */
    @Test
    public void testAdaptiveCheckpointBackoff()
            throws Exception
        {
        File fileDir = FileHelper.createTempDir();
        try
            {
            PartitionJournalConfig config = new PartitionJournalConfig()
                    .setMaximumFileSize(1024 * 1024)
                    .setCheckpointBytesThreshold(1024 * 1024)
                    .setAdaptiveCheckpointInitialWrites(8L)
                    .setAdaptiveCheckpointMinWrites(4L)
                    .setAdaptiveCheckpointMaxWrites(64L)
                    .setAdaptiveCheckpointTargetMillis(1.0d)
                    .setAdaptiveCheckpointWarmupCount(0);

            JournalPersistenceManager manager =
                    new JournalPersistenceManager(fileDir, null, "test-ckpt-adaptive-backoff");
            manager.setJournalConfig(config);
            manager.setDaemonPool(m_pool);

            JournalPersistenceManager.JournalPersistentStore store =
                    (JournalPersistenceManager.JournalPersistentStore) manager.open("partition-0", null);
            store.ensureExtent(1L);
            store.store(1L, toKey(1), new Binary(new byte[] {1}), null);

            Object state = getExtentState(store, 1L);
            invokeLongMethod(state, "recordAdaptiveCheckpointDuration", "periodic", 500_000L);
            assertEquals(8L, getLongFieldValue(state, "m_cCheckpointWriteTrigger"));

            invokeLongMethod(state, "recordAdaptiveCheckpointDuration", "periodic", 3_000_000L);
            assertEquals(16L, getLongFieldValue(state, "m_cCheckpointWriteTrigger"));
            assertEquals(0, getIntFieldValue(state, "m_cCheckpointSamples"));

            manager.close("partition-0");
            manager.release();
            }
        finally
            {
            FileHelper.deleteDir(fileDir);
            }
        }

    /**
     * Verify adaptive checkpointing clamps the write trigger at the configured
     * maximum.
     *
     * @throws Exception on test failure
     */
    @Test
    public void testAdaptiveCheckpointMaxClamp()
            throws Exception
        {
        File fileDir = FileHelper.createTempDir();
        try
            {
            PartitionJournalConfig config = new PartitionJournalConfig()
                    .setMaximumFileSize(1024 * 1024)
                    .setCheckpointBytesThreshold(1024 * 1024)
                    .setAdaptiveCheckpointInitialWrites(4L)
                    .setAdaptiveCheckpointMinWrites(4L)
                    .setAdaptiveCheckpointMaxWrites(8L)
                    .setAdaptiveCheckpointTargetMillis(1.0d)
                    .setAdaptiveCheckpointWarmupCount(0);

            JournalPersistenceManager manager =
                    new JournalPersistenceManager(fileDir, null, "test-ckpt-adaptive-max");
            manager.setJournalConfig(config);
            manager.setDaemonPool(m_pool);

            JournalPersistenceManager.JournalPersistentStore store =
                    (JournalPersistenceManager.JournalPersistentStore) manager.open("partition-0", null);
            store.ensureExtent(1L);
            store.store(1L, toKey(1), new Binary(new byte[] {1}), null);

            Object state = getExtentState(store, 1L);

            for (int i = 0; i < 5; i++)
                {
                invokeLongMethod(state, "recordAdaptiveCheckpointDuration", "periodic", 3_000_000L);
                assertTrue("adaptive trigger should never exceed configured max",
                        getLongFieldValue(state, "m_cCheckpointWriteTrigger")
                                <= config.getAdaptiveCheckpointMaxWrites());
                }

            assertEquals(config.getAdaptiveCheckpointMaxWrites(),
                    getLongFieldValue(state, "m_cCheckpointWriteTrigger"));

            manager.close("partition-0");
            manager.release();
            }
        finally
            {
            FileHelper.deleteDir(fileDir);
            }
        }

    /**
     * Verify adaptive checkpoint warmup samples do not change the write
     * trigger until warmup completes.
     *
     * @throws Exception on test failure
     */
    @Test
    public void testAdaptiveCheckpointWarmupSkip()
            throws Exception
        {
        File fileDir = FileHelper.createTempDir();
        try
            {
            PartitionJournalConfig config = new PartitionJournalConfig()
                    .setMaximumFileSize(1024 * 1024)
                    .setCheckpointBytesThreshold(1024 * 1024)
                    .setAdaptiveCheckpointInitialWrites(8L)
                    .setAdaptiveCheckpointMinWrites(4L)
                    .setAdaptiveCheckpointMaxWrites(64L)
                    .setAdaptiveCheckpointTargetMillis(1.0d)
                    .setAdaptiveCheckpointWarmupCount(3);

            JournalPersistenceManager manager =
                    new JournalPersistenceManager(fileDir, null, "test-ckpt-adaptive-warmup");
            manager.setJournalConfig(config);
            manager.setDaemonPool(m_pool);

            JournalPersistenceManager.JournalPersistentStore store =
                    (JournalPersistenceManager.JournalPersistentStore) manager.open("partition-0", null);
            store.ensureExtent(1L);
            store.store(1L, toKey(1), new Binary(new byte[] {1}), null);

            Object state = getExtentState(store, 1L);

            for (int i = 0; i < 3; i++)
                {
                invokeLongMethod(state, "recordAdaptiveCheckpointDuration", "periodic", 3_000_000L);
                }

            assertEquals("warmup samples should not change the trigger",
                    8L, getLongFieldValue(state, "m_cCheckpointWriteTrigger"));
            assertEquals(0, getIntFieldValue(state, "m_cAdaptiveCheckpointWarmupRemaining"));

            invokeLongMethod(state, "recordAdaptiveCheckpointDuration", "periodic", 3_000_000L);
            assertEquals("trigger should back off after warmup completes",
                    16L, getLongFieldValue(state, "m_cCheckpointWriteTrigger"));

            manager.close("partition-0");
            manager.release();
            }
        finally
            {
            FileHelper.deleteDir(fileDir);
            }
        }


    // ----- helpers --------------------------------------------------------

    /**
     * Create a two-byte test key from an integer index.
     * Uses two bytes to avoid colliding with BinaryRadixTree's
     * internal decoration format markers.
     *
     * @param n  key index
     *
     * @return binary key
     */
    private Binary toKey(int n)
        {
        return new Binary(new byte[] {(byte) (n >>> 8), (byte) n});
        }

    /**
     * Convert extent id array to a set.
     *
     * @param alExtentIds  extent ids
     *
     * @return set of extent ids
     */
    private Set<Long> toExtentSet(long[] alExtentIds)
        {
        Set<Long> set = new HashSet<>();
        for (long lExtentId : alExtentIds)
            {
            set.add(lExtentId);
            }
        return set;
        }

    /**
     * Inject a close failure and verify both handles and recovered values.
     *
     * @param fMetadata  whether to fail the extent list instead of a checkpoint
     *
     * @throws Exception on test failure
     */
    private void assertCloseReleasesHandlesAfterFailure(boolean fMetadata)
            throws Exception
        {
        JournalPersistenceManager.JournalPersistentStore store =
                (JournalPersistenceManager.JournalPersistentStore) m_store;
        List<RandomAccessFile> handles = new ArrayList<>();
        File obstacle = null;
        try
            {
            for (long lExtent = 1; lExtent <= 2; ++lExtent)
                {
                store.ensureExtent(lExtent);
                store.store(lExtent, toKey(1), new Binary(new byte[] {42}), null);
                assertNotNull(store.load(lExtent, toKey(1)));
                Object journal = getFieldValue(getExtentState(store, lExtent), "m_journal");
                Map<?, ?> files = (Map<?, ?>) getFieldValue(journal, "m_mapFiles");
                for (Object info : files.values())
                    {
                    for (String sField : new String[] {"m_rafAppend", "m_rafRead"})
                        {
                        RandomAccessFile handle = (RandomAccessFile) getFieldValue(info, sField);
                        if (handle != null)
                            {
                            assertTrue("test must retain a live descriptor", handle.getFD().valid());
                            handles.add(handle);
                            }
                        }
                    }
                }
            assertFalse("test must capture actual journal handles", handles.isEmpty());
            obstacle = fMetadata
                    ? new File(store.getDataDirectory(), "extents.coh.tmp")
                    : new File(extentDir(store.getDataDirectory(), 1L), "checkpoint.coh");
            assertTrue("failed to create I/O obstacle", obstacle.mkdir());
            assertTrue(new File(obstacle, "do-not-replace").createNewFile());

            m_manager.close(TEST_STORE_ID);
            for (RandomAccessFile handle : handles)
                {
                assertFalse("close failure must not leak a journal descriptor", handle.getFD().valid());
                }
            assertEquals("all extent states should be released", 0, getExtentStateCount(store));

            FileHelper.deleteDir(obstacle);
            obstacle = null;
            m_store = (AbstractPersistentStore) m_manager.open(TEST_STORE_ID, null);
            for (long lExtent = 1; lExtent <= 2; ++lExtent)
                {
                assertEquals("journal replay must preserve extent " + lExtent,
                        new Binary(new byte[] {42}), m_store.load(lExtent, toKey(1)));
                }
            }
        finally
            {
            // close retained handles even when testing the unfixed implementation
            for (RandomAccessFile handle : handles)
                {
                handle.close();
                }
            if (obstacle != null)
                {
                FileHelper.deleteDir(obstacle);
                }
            }
        }

    /**
     * Return {@code true} if the named file exists anywhere under the
     * supplied directory.
     *
     * @param fileDir  root directory
     * @param sName    target filename
     *
     * @return {@code true} if a matching file exists
     */
    private boolean containsFileNamed(File fileDir, String sName)
        {
        File[] aFiles = fileDir.listFiles();
        if (aFiles == null)
            {
            return false;
            }

        for (File file : aFiles)
            {
            if (file.isDirectory())
                {
                if (containsFileNamed(file, sName))
                    {
                    return true;
                    }
                }
            else if (sName.equals(file.getName()))
                {
                return true;
                }
            }
        return false;
        }

    /**
     * Return the per-extent directory for the supplied store data directory.
     *
     * @param fileStore  the store data directory
     * @param lExtentId  the extent id
     *
     * @return the extent directory
     */
    private File extentDir(File fileStore, long lExtentId)
        {
        return new File(new File(fileStore, "extents"), String.format("%016d", lExtentId));
        }

    /**
     * Count journal files in the supplied extent directory.
     *
     * @param fileExtent  the extent directory
     *
     * @return journal file count
     */
    private int countJournalFiles(File fileExtent)
        {
        File[] aFiles = fileExtent.listFiles((dir, sName) ->
                sName.startsWith("journal-") && sName.endsWith(".coh"));
        return aFiles == null ? 0 : aFiles.length;
        }

    /**
     * Return the number of opened extent states via reflection.
     *
     * @param store  store to inspect
     *
     * @return opened extent-state count
     *
     * @throws Exception on reflection failure
     */
    @SuppressWarnings("unchecked")
    private int getExtentStateCount(JournalPersistenceManager.JournalPersistentStore store)
            throws Exception
        {
        Field field = store.getClass().getDeclaredField("m_mapExtentState");
        field.setAccessible(true);
        return ((Map<Long, ?>) field.get(store)).size();
        }

    /**
     * Return the number of opened extent states via reflection, wrapping
     * checked failures for deferred assertions.
     *
     * @param store  store to inspect
     *
     * @return opened extent-state count
     */
    private int getExtentStateCountUnchecked(JournalPersistenceManager.JournalPersistentStore store)
        {
        try
            {
            return getExtentStateCount(store);
            }
        catch (Exception e)
            {
            throw new IllegalStateException("failed to read extent-state count", e);
            }
        }

    /**
     * Return the opened extent state for the supplied extent id.
     *
     * @param store      store to inspect
     * @param lExtentId  extent id
     *
     * @return the extent state
     *
     * @throws Exception on reflection failure
     */
    @SuppressWarnings("unchecked")
    private Object getExtentState(JournalPersistenceManager.JournalPersistentStore store, long lExtentId)
            throws Exception
        {
        Field field = store.getClass().getDeclaredField("m_mapExtentState");
        field.setAccessible(true);

        Map<Long, ?> map = (Map<Long, ?>) field.get(store);
        Object       state = map.get(lExtentId);

        assertNotNull("extent state should exist for extent " + lExtentId, state);
        return state;
        }

    /**
     * Return a reflected field value.
     *
     * @param oTarget  target object
     * @param sField   field name
     *
     * @return the field value
     *
     * @throws Exception on reflection failure
     */
    private Object getFieldValue(Object oTarget, String sField)
            throws Exception
        {
        Field field = oTarget.getClass().getDeclaredField(sField);
        field.setAccessible(true);
        return field.get(oTarget);
        }

    /**
     * Return a reflected long field value.
     *
     * @param oTarget  target object
     * @param sField   field name
     *
     * @return the field value
     *
     * @throws Exception on reflection failure
     */
    private long getLongFieldValue(Object oTarget, String sField)
            throws Exception
        {
        Field field = oTarget.getClass().getDeclaredField(sField);
        field.setAccessible(true);
        return field.getLong(oTarget);
        }

    /**
     * Return a reflected boolean field value.
     *
     * @param oTarget  target object
     * @param sField   field name
     *
     * @return the field value
     *
     * @throws Exception on reflection failure
     */
    private boolean getBooleanFieldValue(Object oTarget, String sField)
        {
        try
            {
            Field field = oTarget.getClass().getDeclaredField(sField);
            field.setAccessible(true);
            return field.getBoolean(oTarget);
            }
        catch (Exception e)
            {
            throw new IllegalStateException("failed to read boolean field " + sField, e);
            }
        }

    /**
     * Return a reflected int field value.
     *
     * @param oTarget  target object
     * @param sField   field name
     *
     * @return the field value
     *
     * @throws Exception on reflection failure
     */
    private int getIntFieldValue(Object oTarget, String sField)
            throws Exception
        {
        Field field = oTarget.getClass().getDeclaredField(sField);
        field.setAccessible(true);
        return field.getInt(oTarget);
        }

    /**
     * Set a reflected boolean field.
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

    /**
     * Invoke a reflected method with a {@link String} and {@code long}
     * argument.
     *
     * @param oTarget   target object
     * @param sMethod   method name
     * @param sArg      string argument
     * @param lArg      long argument
     *
     * @throws Exception on reflection failure
     */
    private void invokeLongMethod(Object oTarget, String sMethod, String sArg, long lArg)
            throws Exception
        {
        Method method = oTarget.getClass().getDeclaredMethod(sMethod, String.class, long.class);
        method.setAccessible(true);
        method.invoke(oTarget, sArg, lArg);
        }

    /**
     * Invoke a reflected method with arbitrary arguments.
     *
     * @param oTarget       target object
     * @param sMethod       method name
     * @param aTypes        parameter types
     * @param aArgs         arguments
     *
     * @return invocation result
     *
     * @throws Exception on reflection failure
     */
    private Object invokeMethod(Object oTarget, String sMethod, Class<?>[] aTypes, Object... aArgs)
            throws Exception
        {
        Method method = oTarget.getClass().getDeclaredMethod(sMethod, aTypes);
        method.setAccessible(true);
        return method.invoke(oTarget, aArgs);
        }

    /**
     * Assert that the supplied action blocks while checkpoint completion is
     * pending, and then completes once the checkpoint is released.
     *
     * @param state    the reflected extent state
     * @param sAction  action label
     * @param action   the action to execute
     *
     * @throws Exception on test failure
     */
    private void assertWaitsForCheckpointCompletion(Object state, String sAction, Runnable action)
            throws Exception
        {
        Object lock = getFieldValue(state, "f_checkpointLock");

        synchronized (lock)
            {
            setBooleanField(state, "m_fCheckpointInProgress", true);
            }

        CountDownLatch           latchStarted = new CountDownLatch(1);
        CountDownLatch           latchDone    = new CountDownLatch(1);
        AtomicReference<Throwable> refError   = new AtomicReference<>();

        Thread thread = new Thread(() ->
            {
            latchStarted.countDown();
            try
                {
                action.run();
                }
            catch (Throwable t)
                {
                refError.set(t);
                }
            finally
                {
                latchDone.countDown();
                }
            }, "JournalPersistentStoreTest-" + sAction);
        thread.setDaemon(true);
        thread.start();

        try
            {
            assertTrue(sAction + " thread should start", latchStarted.await(1, TimeUnit.SECONDS));
            assertFalse(sAction + " should wait while checkpoint is in flight",
                    latchDone.await(250, TimeUnit.MILLISECONDS));
            }
        finally
            {
            synchronized (lock)
                {
                setBooleanField(state, "m_fCheckpointInProgress", false);
                lock.notifyAll();
                }
            }

        assertTrue(sAction + " should finish after checkpoint completion",
                latchDone.await(5, TimeUnit.SECONDS));
        assertNull(sAction + " should complete without error", refError.get());
        thread.join(TimeUnit.SECONDS.toMillis(1));
        }
    }
