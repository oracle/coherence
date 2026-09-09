/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService$PartitionControl;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService$PinningIterator;

import com.tangosol.util.Binary;
import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.SegmentedConcurrentMap;
import com.tangosol.util.WrapperConcurrentMap;

import com.tangosol.net.partition.PartitionSet;

import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests for key locking in {@link PartitionedCache}.
 *
 * @author fryp  2026.07.14
 * @since 26.04
 */
public class PartitionedCacheLockKeyTest
    {
    @Test
    public void shouldExitPartitionWhenResourceLockThrows()
        {
        RuntimeException     expected = new RuntimeException("lock failed");
        TestPartitionedCache service  = new TestPartitionedCache(new TestResourceCoordinator(expected));

        RuntimeException actual = assertThrows(RuntimeException.class,
                () -> service.lockKey(null, new Binary(), true));

        assertSame(expected, actual);
        assertEquals(1, service.getEnterCount());
        assertEquals(1, service.getExitCount());
        assertEquals(PARTITION, service.getExitedPartition());
        }

    @Test
    public void shouldSuppressPartitionExitFailureOntoResourceLockFailure()
        {
        RuntimeException     eLock    = new RuntimeException("lock failed");
        RuntimeException     eExit    = new RuntimeException("exit failed");
        TestPartitionedCache service  = new TestPartitionedCache(new TestResourceCoordinator(eLock));
        service.setExitFailure(eExit);

        RuntimeException actual = assertThrows(RuntimeException.class,
                () -> service.lockKey(null, new Binary(), true));

        assertSame(eLock, actual);
        assertEquals(1, actual.getSuppressed().length);
        assertSame(eExit, actual.getSuppressed()[0]);
        assertEquals(1, service.getEnterCount());
        assertEquals(1, service.getExitCount());
        assertEquals(PARTITION, service.getExitedPartition());
        }

    @Test
    public void shouldKeepPartitionEnteredWhenResourceLockSucceeds()
        {
        TestPartitionedCache service = new TestPartitionedCache(new TestResourceCoordinator(true));

        assertTrue(service.lockKey(null, new Binary(), true));
        assertEquals(1, service.getEnterCount());
        assertEquals(0, service.getExitCount());
        }

    @Test
    public void shouldExitPartitionWhenIndexReadinessCheckThrows()
        {
        RuntimeException     expected = new RuntimeException("index wait failed");
        TestPartitionedCache service  = new TestPartitionedCache(new TestResourceCoordinator(true));
        service.setIndexFailure(expected);

        RuntimeException actual = assertThrows(RuntimeException.class,
                () -> service.pinPartition(PARTITION));

        assertSame(expected, actual);
        assertEquals(1, service.getEnterCount());
        assertEquals(1, service.getExitCount());
        assertEquals(PARTITION, service.getExitedPartition());
        }

    @Test
    public void shouldExitPartitionWhenResourceUnlockThrows()
        {
        RuntimeException        expected    = new RuntimeException("unlock failed");
        TestResourceCoordinator coordinator = new TestResourceCoordinator(true);
        TestPartitionedCache    service     = new TestPartitionedCache(coordinator);
        coordinator.setUnlockFailure(expected);

        RuntimeException actual = assertThrows(RuntimeException.class,
                () -> service.unlockKey(null, new Binary(), true));

        assertSame(expected, actual);
        assertEquals(1, service.getExitCount());
        assertEquals(PARTITION, service.getExitedPartition());
        }

    @Test
    public void shouldRollbackPreviouslyPinnedPartitionsWhenLaterPinThrows()
        {
        RuntimeException           expected = new RuntimeException("pin failed");
        FailingPinPartitionedCache service  = new FailingPinPartitionedCache(expected);
        PartitionSet               parts    = new PartitionSet(PARTITION_COUNT);
        parts.add(1);
        parts.add(2);

        RuntimeException actual = assertThrows(RuntimeException.class,
                () -> service.pinPartitions(parts));

        assertSame(expected, actual);
        assertTrue(service.getUnpinnedPartitions().contains(1));
        assertFalse(service.getUnpinnedPartitions().contains(2));
        assertEquals(1, service.getUnpinnedPartitions().cardinality());
        }

    @Test
    public void shouldReleasePhysicalLockWhenStatusRegistrationThrows()
        {
        RuntimeException             expected    = new RuntimeException("status failed");
        TrackingConcurrentMap        mapControl  = new TrackingConcurrentMap();
        FailingStatusCoordinator     coordinator = new FailingStatusCoordinator(expected);
        TestPartitionedCache         service     = new TestPartitionedCache(coordinator);
        PartitionedCache$Storage                      storage     = new PartitionedCache$Storage(null, null, false);
        Binary                       binKey      = new Binary();
        coordinator.setService(service);
        storage.setResourceControlMap(mapControl);

        RuntimeException actual = assertThrows(RuntimeException.class,
                () -> coordinator.lock(storage, binKey, -1L));

        assertSame(expected, actual);
        assertEquals(1, mapControl.getLockCount());
        assertEquals(1, mapControl.getUnlockCount());
        }

    @Test
    public void shouldExitPartitionWhenContendedResourceLockIsInterrupted() throws Exception
        {
        ContentionResourceCoordinator coordinator  = new ContentionResourceCoordinator();
        TestPartitionedCache          service      = new TestPartitionedCache(coordinator);
        PartitionedCache$Storage                       storage      = new PartitionedCache$Storage(null, null, false);
        Binary                        binKey       = new Binary();
        ConcurrentMap                 mapControl   = coordinator.instantiateControlMap();
        CountDownLatch                latchLocked  = new CountDownLatch(1);
        CountDownLatch                latchRelease = new CountDownLatch(1);
        AtomicReference<Throwable>    errorHolder  = new AtomicReference<>();
        AtomicReference<Throwable>    errorWaiter  = new AtomicReference<>();
        AtomicBoolean                 fInterrupted = new AtomicBoolean();

        coordinator.setService(service);
        storage.setResourceControlMap(mapControl);

        Thread threadHolder = new Thread(() ->
            {
            boolean fLocked = false;
            try
                {
                fLocked = mapControl.lock(binKey, -1L);
                if (!fLocked)
                    {
                    throw new AssertionError("Holder failed to acquire the key lock");
                    }
                latchLocked.countDown();
                latchRelease.await();
                }
            catch (Throwable e)
                {
                errorHolder.set(e);
                }
            finally
                {
                if (fLocked)
                    {
                    mapControl.unlock(binKey);
                    }
                latchLocked.countDown();
                }
            }, "lock-holder");

        Thread threadWaiter = new Thread(() ->
            {
            try
                {
                if (service.lockKey(storage, binKey, true))
                    {
                    errorWaiter.set(new AssertionError("Contended lock unexpectedly succeeded"));
                    service.unlockKey(storage, binKey, true);
                    }
                }
            catch (Throwable e)
                {
                errorWaiter.set(e);
                fInterrupted.set(Thread.currentThread().isInterrupted());
                }
            }, "lock-waiter");

        try
            {
            threadHolder.start();
            assertTrue("holder failed to acquire the key lock",
                    latchLocked.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS));

            threadWaiter.start();
            assertTrue("waiter did not contend for the key lock",
                    coordinator.awaitContention(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS));

            threadWaiter.interrupt();
            threadWaiter.join(TimeUnit.SECONDS.toMillis(TEST_TIMEOUT_SECONDS));

            assertFalse("interrupted waiter did not terminate", threadWaiter.isAlive());
            assertTrue(errorWaiter.get() instanceof RuntimeException);
            assertTrue("interrupt status was not preserved", fInterrupted.get());
            assertEquals(1, service.getEnterCount());
            assertEquals(1, service.getExitCount());
            assertEquals(PARTITION, service.getExitedPartition());
            }
        finally
            {
            if (threadWaiter.isAlive())
                {
                threadWaiter.interrupt();
                }
            latchRelease.countDown();
            threadHolder.join(TimeUnit.SECONDS.toMillis(TEST_TIMEOUT_SECONDS));
            threadWaiter.join(TimeUnit.SECONDS.toMillis(TEST_TIMEOUT_SECONDS));
            }

        assertFalse("lock holder did not terminate", threadHolder.isAlive());
        assertFalse("lock waiter did not terminate", threadWaiter.isAlive());
        assertNull(errorHolder.get());
        assertTrue("physical key lock was not reusable", mapControl.lock(binKey, 1000L));
        assertTrue(mapControl.unlock(binKey));
        }

    @Test
    public void shouldPreserveFirstFailureAcrossInvocationAndExternalCleanup()
        {
        RuntimeException                eStatus        = new RuntimeException("status cleanup failed");
        RuntimeException                eContextUnpin  = new RuntimeException("context unpin failed");
        RuntimeException                eExternalUnpin = new RuntimeException("external unpin failed");
        FailingCleanupPartitionedCache  service        = new FailingCleanupPartitionedCache(
                eContextUnpin, eExternalUnpin);
        FailingCleanupInvocationContext context        =
                new FailingCleanupInvocationContext(service, eStatus);
        PartitionSet                    partsExternal  = new PartitionSet(PARTITION_COUNT);

        context.getPinnedPartitions().add(1);
        partsExternal.add(2);

        RuntimeException actual = assertThrows(RuntimeException.class,
                () -> service.releaseAndUnpin(context, partsExternal));

        assertSame(eStatus, actual);
        assertEquals(2, actual.getSuppressed().length);
        assertSame(eContextUnpin, actual.getSuppressed()[0]);
        assertSame(eExternalUnpin, actual.getSuppressed()[1]);
        assertTrue(context.getPinnedPartitions().isEmpty());
        assertTrue(context.getPrePinnedPartitions().isEmpty());
        }

    @Test
    public void shouldRollbackPinningIteratorWhenLaterPinThrows()
        {
        RuntimeException         expected  = new RuntimeException("iterator pin failed");
        Binary                   binKeyOne = new Binary(new byte[] {1});
        Binary                   binKeyTwo = new Binary(new byte[] {2});
        IteratorPartitionedCache service   = new IteratorPartitionedCache(binKeyOne, binKeyTwo);
        FailingPinningIterator   iterator  = new FailingPinningIterator(service, expected);
        Set<Binary>              setKeys   = new LinkedHashSet<>();

        setKeys.add(binKeyOne);
        setKeys.add(binKeyTwo);
        iterator.setFullSet(setKeys);

        assertTrue(iterator.hasNext());
        assertSame(binKeyOne, iterator.next());

        RuntimeException actual = assertThrows(RuntimeException.class, iterator::hasNext);

        assertSame(expected, actual);
        assertTrue(service.getUnpinnedPartitions().contains(1));
        assertEquals(1, service.getUnpinnedPartitions().cardinality());
        assertTrue(iterator.getPinnedPartitions().isEmpty());
        }

    @Test
    public void shouldReleaseOwnershipWhenInvocationRegistrationThrows()
        {
        RuntimeException                     expected    = new RuntimeException("context registration failed");
        ContentionResourceCoordinator        coordinator = new ContentionResourceCoordinator();
        TestPartitionedCache                 service     = new TestPartitionedCache(coordinator);
        PartitionedCache$Storage                              storage     = new PartitionedCache$Storage(null, null, false);
        Binary                               binKey      = new Binary();
        ConcurrentMap                        mapControl  = coordinator.instantiateControlMap();
        FailingRegistrationInvocationContext context     =
                new FailingRegistrationInvocationContext(service, expected);

        coordinator.setService(service);
        storage.setEntryStatusMap(new ConcurrentHashMap<>());
        storage.setResourceControlMap(mapControl);

        RuntimeException actual = assertThrows(RuntimeException.class,
                () -> context.lockEntry(storage, binKey, true));

        assertSame(expected, actual);
        assertEquals(1, service.getEnterCount());
        assertEquals(1, service.getExitCount());
        assertTrue("physical key lock was not released", mapControl.lock(binKey, 1000L));
        assertTrue(mapControl.unlock(binKey));
        }

    // ----- helper classes -------------------------------------------------

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static class TestPartitionedCache
            extends PartitionedCache
        {
        TestPartitionedCache(PartitionedCache$ResourceCoordinator coordinator)
            {
            super(null, null, false);

            m_coordinator = coordinator;
            }

        @Override
        public int getKeyPartition(Binary binKey)
            {
            return PARTITION;
            }

        @Override
        public PartitionedService$PartitionControl getPartitionControl(int nPartition)
            {
            return null;
            }

        @Override
        public boolean enterPartition(int nPartition, long cWait)
            {
            ++m_cEnter;
            return true;
            }

        @Override
        public boolean enterPartition(int nPartition)
            {
            ++m_cEnter;
            return true;
            }

        @Override
        public boolean isConcurrent()
            {
            return true;
            }

        @Override
        public boolean isPrimaryOwner(int nPartition)
            {
            return true;
            }

        @Override
        public int getPartitionCount()
            {
            return PARTITION_COUNT;
            }

        @Override
        public PartitionedCache$ResourceCoordinator getResourceCoordinator()
            {
            return m_coordinator;
            }

        @Override
        public void exitPartition(int nPartition)
            {
            ++m_cExit;
            m_nExitedPartition = nPartition;
            if (m_eExit != null)
                {
                throw m_eExit;
                }
            }

        @Override
        public void ensureIndexReady(int nPartition)
            {
            if (m_eIndex != null)
                {
                throw m_eIndex;
                }
            }

        boolean pinPartition(int nPartition)
            {
            return pinOwnedPartition(nPartition, -1);
            }

        void setIndexFailure(RuntimeException e)
            {
            m_eIndex = e;
            }

        void setExitFailure(RuntimeException e)
            {
            m_eExit = e;
            }

        int getEnterCount()
            {
            return m_cEnter;
            }

        int getExitCount()
            {
            return m_cExit;
            }

        int getExitedPartition()
            {
            return m_nExitedPartition;
            }

        private final PartitionedCache$ResourceCoordinator m_coordinator;

        private int m_cEnter;
        private int m_cExit;
        private int m_nExitedPartition = -1;
        private RuntimeException m_eExit;
        private RuntimeException m_eIndex;
        }

    private static class ContentionResourceCoordinator
            extends PartitionedCache$ResourceCoordinator
        {
        ContentionResourceCoordinator()
            {
            super(null, null, false);
            }

        @Override
        public com.tangosol.internal.util.BMEventFabric.EventQueue ensureEventQueue()
            {
            return null;
            }

        @Override
        public PartitionedCache getService()
            {
            return m_service;
            }

        @Override
        public void onContend(Object contender, SegmentedConcurrentMap.LockableEntry entry)
            {
            m_latchContention.countDown();
            }

        @Override
        public void onUncontend(Object contender, SegmentedConcurrentMap.LockableEntry entry)
            {
            }

        boolean awaitContention(long cTimeout, TimeUnit unit) throws InterruptedException
            {
            return m_latchContention.await(cTimeout, unit);
            }

        void setService(PartitionedCache service)
            {
            m_service = service;
            }

        private final CountDownLatch m_latchContention = new CountDownLatch(1);

        private PartitionedCache m_service;
        }

    private static class FailingCleanupPartitionedCache
            extends TestPartitionedCache
        {
        FailingCleanupPartitionedCache(RuntimeException... aFailure)
            {
            super(new TestResourceCoordinator(true));

            m_aFailure = aFailure;
            }

        @Override
        public void unpinPartitions(PartitionSet partitions)
            {
            if (m_iFailure < m_aFailure.length)
                {
                throw m_aFailure[m_iFailure++];
                }
            }

        void releaseAndUnpin(PartitionedCache$InvocationContext context, PartitionSet partitions)
            {
            releaseInvocationContextAndUnpin(context, partitions, true);
            }

        private final RuntimeException[] m_aFailure;

        private int m_iFailure;
        }

    private static class FailingCleanupInvocationContext
            extends PartitionedCache$InvocationContext
        {
        FailingCleanupInvocationContext(PartitionedCache service, RuntimeException expected)
            {
            super(null, service, true);

            m_expected = expected;
            setPrePinnedPartitions(new PartitionSet(PARTITION_COUNT));
            }

        @Override
        protected void clearStatuses()
            {
            throw m_expected;
            }

        private final RuntimeException m_expected;
        }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static class FailingRegistrationInvocationContext
            extends PartitionedCache$InvocationContext
        {
        FailingRegistrationInvocationContext(PartitionedCache service, RuntimeException expected)
            {
            super(null, service, true);

            setPrePinnedPartitions(new PartitionSet(PARTITION_COUNT));
            setStorageStatusMap(new HashMap()
                {
                @Override
                public Object put(Object key, Object value)
                    {
                    throw expected;
                    }
                });
            }

        @Override
        public int getKeyPartition(Object oKey)
            {
            return getService().getKeyPartition((Binary) oKey);
            }
        }

    private static class IteratorPartitionedCache
            extends TestPartitionedCache
        {
        IteratorPartitionedCache(Binary binKeyOne, Binary binKeyTwo)
            {
            super(new TestResourceCoordinator(true));

            m_binKeyOne = binKeyOne;
            m_binKeyTwo = binKeyTwo;
            }

        @Override
        public int getKeyPartition(Binary binKey)
            {
            return binKey == m_binKeyOne ? 1 : binKey == m_binKeyTwo ? 2 : 0;
            }

        @Override
        public void unpinPartitions(PartitionSet partitions)
            {
            m_partsUnpinned = new PartitionSet(partitions);
            }

        PartitionSet getUnpinnedPartitions()
            {
            return m_partsUnpinned;
            }

        private final Binary m_binKeyOne;
        private final Binary m_binKeyTwo;

        private PartitionSet m_partsUnpinned;
        }

    private static class FailingPinningIterator
            extends PartitionedService$PinningIterator
        {
        FailingPinningIterator(PartitionedCache service, RuntimeException expected)
            {
            super(null, service, true);

            m_expected = expected;
            }

        @Override
        protected boolean enterPartition(int nPartition)
            {
            if (++m_cPins == 2)
                {
                throw m_expected;
                }
            return true;
            }

        private final RuntimeException m_expected;

        private int m_cPins;
        }

    private static class TestResourceCoordinator
            extends PartitionedCache$ResourceCoordinator
        {
        TestResourceCoordinator(boolean fResult)
            {
            super(null, null, false);

            m_fResult  = fResult;
            m_expected = null;
            }

        TestResourceCoordinator(RuntimeException expected)
            {
            super(null, null, false);

            m_fResult  = false;
            m_expected = expected;
            }

        @Override
        public boolean lock(PartitionedCache$Storage storage, Binary binKey, long cWait)
            {
            if (m_expected != null)
                {
                throw m_expected;
                }
            return m_fResult;
            }

        @Override
        public void unlock(PartitionedCache$Storage storage, Binary binKey)
            {
            if (m_eUnlock != null)
                {
                throw m_eUnlock;
                }
            }

        void setUnlockFailure(RuntimeException e)
            {
            m_eUnlock = e;
            }

        private final boolean          m_fResult;
        private final RuntimeException m_expected;
        private RuntimeException       m_eUnlock;
        }

    private static class FailingPinPartitionedCache
            extends PartitionedCache
        {
        FailingPinPartitionedCache(RuntimeException expected)
            {
            super(null, null, false);

            m_expected = expected;
            }

        @Override
        protected boolean pinOwnedPartition(int nPartition, int nVersion)
            {
            if (++m_cPins == 2)
                {
                throw m_expected;
                }
            return true;
            }

        @Override
        public int getPartitionCount()
            {
            return PARTITION_COUNT;
            }

        @Override
        public void unpinPartitions(PartitionSet partitions)
            {
            m_partsUnpinned = new PartitionSet(partitions);
            }

        PartitionSet pinPartitions(PartitionSet partitions)
            {
            return pinOwnedPartitions(partitions);
            }

        PartitionSet getUnpinnedPartitions()
            {
            return m_partsUnpinned;
            }

        private final RuntimeException m_expected;

        private int          m_cPins;
        private PartitionSet m_partsUnpinned;
        }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static class TrackingConcurrentMap
            extends WrapperConcurrentMap
        {
        TrackingConcurrentMap()
            {
            super(new HashMap());
            }

        @Override
        public boolean lock(Object oKey, long cWait)
            {
            ++m_cLocks;
            return true;
            }

        @Override
        public boolean unlock(Object oKey)
            {
            ++m_cUnlocks;
            return true;
            }

        int getLockCount()
            {
            return m_cLocks;
            }

        int getUnlockCount()
            {
            return m_cUnlocks;
            }

        private int m_cLocks;
        private int m_cUnlocks;
        }

    private static class FailingStatusCoordinator
            extends PartitionedCache$ResourceCoordinator
        {
        FailingStatusCoordinator(RuntimeException expected)
            {
            super(null, null, false);

            m_expected = expected;
            }

        @Override
        public com.tangosol.internal.util.BMEventFabric.EventQueue ensureEventQueue()
            {
            return null;
            }

        @Override
        public PartitionedCache$Storage$EntryStatus ensureStatus(PartitionedCache$Storage storage, Binary binKey)
            {
            throw m_expected;
            }

        @Override
        public PartitionedCache getService()
            {
            return m_service;
            }

        void setService(PartitionedCache service)
            {
            m_service = service;
            }

        private final RuntimeException m_expected;

        private PartitionedCache m_service;
        }

    private static final int PARTITION            = 17;
    private static final int PARTITION_COUNT      = 31;
    private static final int TEST_TIMEOUT_SECONDS = 10;
    }
