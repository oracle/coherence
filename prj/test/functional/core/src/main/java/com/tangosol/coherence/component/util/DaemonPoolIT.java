/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.component.util;

import com.oracle.coherence.testing.junit.ThreadDumpOnTimeoutRule;
import com.oracle.coherence.common.util.AssociationPile;

import com.tangosol.coherence.component.util.daemon.queueProcessor.Service;

import com.tangosol.internal.net.service.DefaultServiceDependencies;

import com.tangosol.net.DaemonPoolType;
import com.tangosol.net.Guardable;
import com.tangosol.net.Guardian;
import com.tangosol.net.cache.KeyAssociation;

import com.tangosol.util.Base;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;

public class DaemonPoolIT
    {
    @Before
    public void rememberResizeTaskDefaults()
        {
        m_cGrowGraceCount               = DaemonPool.ResizeTask.getGrowGraceCount();
        m_dIdleFraction                 = DaemonPool.ResizeTask.getIdleFraction();
        m_nIdleLimit                    = DaemonPool.ResizeTask.getIdleLimit();
        m_cOverutilizedShakeCount       = DaemonPool.ResizeTask.getOverutilizedShakeCount();
        m_cOverutilizedShakeCountMax    = DaemonPool.ResizeTask.getOverutilizedShakeCountMax();
        m_cPeriodAdjust                 = DaemonPool.ResizeTask.getPeriodAdjust();
        m_cPeriodAdjustSlightlySlower   = DaemonPool.ResizeTask.getPeriodAdjustSlightlySlower();
        m_cPeriodMax                    = DaemonPool.ResizeTask.getPeriodMax();
        m_cPeriodMin                    = DaemonPool.ResizeTask.getPeriodMin();
        m_cPeriodShake                  = DaemonPool.ResizeTask.getPeriodShake();
        m_cPeriodShakeFast              = DaemonPool.ResizeTask.getPeriodShakeFast();
        m_cPeriodShakeFaster            = DaemonPool.ResizeTask.getPeriodShakeFaster();
        m_dResizeGrow                   = DaemonPool.ResizeTask.getResizeGrow();
        m_dResizeJitter                 = DaemonPool.ResizeTask.getResizeJitter();
        m_dResizeShake                  = DaemonPool.ResizeTask.getResizeShake();
        m_dResizeShrink                 = DaemonPool.ResizeTask.getResizeShrink();
        m_cShrinkCooldownMillis         = DaemonPool.ResizeTask.getShrinkCooldownMillis();

        DaemonPool.ResizeTask.setGrowGraceCount(2);
        DaemonPool.ResizeTask.setIdleFraction(0.333d);
        DaemonPool.ResizeTask.setIdleLimit(20);
        DaemonPool.ResizeTask.setOverutilizedShakeCount(10);
        DaemonPool.ResizeTask.setOverutilizedShakeCountMax(30);
        DaemonPool.ResizeTask.setPeriodAdjust(250L);
        DaemonPool.ResizeTask.setPeriodAdjustSlightlySlower(125L);
        DaemonPool.ResizeTask.setPeriodMax(10000L);
        DaemonPool.ResizeTask.setPeriodMin(100L);
        DaemonPool.ResizeTask.setPeriodShake(600000L);
        DaemonPool.ResizeTask.setPeriodShakeFast(30000L);
        DaemonPool.ResizeTask.setPeriodShakeFaster(10000L);
        DaemonPool.ResizeTask.setResizeGrow(1.0d);
        DaemonPool.ResizeTask.setResizeJitter(0.05d);
        DaemonPool.ResizeTask.setResizeShake(0.15d);
        DaemonPool.ResizeTask.setResizeShrink(0.25d);
        DaemonPool.ResizeTask.setShrinkCooldownMillis(200L);
        }

    @After
    public void restoreResizeTaskDefaults()
        {
        DaemonPool.ResizeTask.setGrowGraceCount(m_cGrowGraceCount);
        DaemonPool.ResizeTask.setIdleFraction(m_dIdleFraction);
        DaemonPool.ResizeTask.setIdleLimit(m_nIdleLimit);
        DaemonPool.ResizeTask.setOverutilizedShakeCount(m_cOverutilizedShakeCount);
        DaemonPool.ResizeTask.setOverutilizedShakeCountMax(m_cOverutilizedShakeCountMax);
        DaemonPool.ResizeTask.setPeriodAdjust(m_cPeriodAdjust);
        DaemonPool.ResizeTask.setPeriodAdjustSlightlySlower(m_cPeriodAdjustSlightlySlower);
        DaemonPool.ResizeTask.setPeriodMax(m_cPeriodMax);
        DaemonPool.ResizeTask.setPeriodMin(m_cPeriodMin);
        DaemonPool.ResizeTask.setPeriodShake(m_cPeriodShake);
        DaemonPool.ResizeTask.setPeriodShakeFast(m_cPeriodShakeFast);
        DaemonPool.ResizeTask.setPeriodShakeFaster(m_cPeriodShakeFaster);
        DaemonPool.ResizeTask.setResizeGrow(m_dResizeGrow);
        DaemonPool.ResizeTask.setResizeJitter(m_dResizeJitter);
        DaemonPool.ResizeTask.setResizeShake(m_dResizeShake);
        DaemonPool.ResizeTask.setResizeShrink(m_dResizeShrink);
        DaemonPool.ResizeTask.setShrinkCooldownMillis(m_cShrinkCooldownMillis);
        }

    @Test
    public void shouldAllowMultipleCallsToResizeTaskCancel()
        {
        DaemonPool            pool       = new DaemonPool();
        DaemonPool.ResizeTask resizeTask = (DaemonPool.ResizeTask) pool._newChild("ResizeTask");
        assertThat(resizeTask, is(notNullValue()));
        assertThat(resizeTask.getDaemonPool(), is(sameInstance(pool)));
        resizeTask.cancel();
        assertThat(resizeTask.getDaemonPool(), is(nullValue()));
        resizeTask.cancel();
        assertThat(resizeTask.getDaemonPool(), is(nullValue()));
        }

    /**
     * This test is somewhat convoluted, but it is to verify that BUG 35164134 is fixed,
     * where stopping the daemon pool while a resize task is resizing the pool caused
     * a deadlock.
     */
    @Test
    public void shouldNotDeadlockWhenStoppingPoolWhileResizing() throws Exception
        {
        DaemonPoolStub        pool       = new DaemonPoolStub();
        DaemonPool.ResizeTask resizeTask = (DaemonPool.ResizeTask) pool._newChild("ResizeTask");
        DaemonPool.Daemon[]   aDaemon    = new DaemonPool.Daemon[]
                {
                new DaemonPool.Daemon(),
                new DaemonPool.Daemon(),
                new DaemonPool.Daemon(),
                new DaemonPool.Daemon(),
                new DaemonPool.Daemon(),
                };

        pool.setDaemonCountMin(1);
        pool.setDaemonCountMax(10);
        pool.setDaemonCount(5);
        pool.setDaemons(aDaemon);
        pool.setStarted(true);

        CountDownLatch latchNotify = new CountDownLatch(1);
        CountDownLatch latchWait   = new CountDownLatch(1);
        pool.setLatch(latchNotify, latchWait);

        DaemonPool.ResizeTask mockTask = Mockito.spy(resizeTask);
        pool.setResizeTask(mockTask);

        doAnswer(new Answer()
            {
            @Override
            public Object answer(InvocationOnMock invocationOnMock)
                {
                latchWait.countDown();
                resizeTask.cancel();
                return null;
                }
            }).when(mockTask).cancel();

        resizeTask.setLastThreadCount(5);
        resizeTask.setActiveCountAverage(2.0d);
        resizeTask.setLastThroughput(1.0d);

        DaemonPool.ResizeTask.setIdleFraction(1.0d);

        CompletableFuture.runAsync(resizeTask);

        System.err.println("Waiting for ResizeTask to resize pool");
        latchNotify.await();
        System.err.println("ResizeTask is resizing pool, calling shutdown");
        pool.shutdown();
        System.err.println("Pool is shutdown");

        assertThat(resizeTask.getDaemonPool(), is(nullValue()));
        assertThat(pool.isStarted(), is(false));
        }

    @Test
    public void shouldDrainHotQueueWithIdleStealers() throws Exception
        {
        String          sSlotsPrevious = System.getProperty("coherence.daemonpool.slots");
        TestDaemonPool pool           = new TestDaemonPool();
        CountDownLatch latchDone      = new CountDownLatch(8);
        CountDownLatch latchRelease   = new CountDownLatch(1);
        AtomicInteger  cActive        = new AtomicInteger();
        AtomicInteger  cMaxActive     = new AtomicInteger();
        AtomicInteger  cStarted       = new AtomicInteger();
        AtomicInteger[] acSequence    = new AtomicInteger[4];

        for (int i = 0; i < acSequence.length; i++)
            {
            acSequence[i] = new AtomicInteger();
            }

        AtomicReference<Throwable> refFailure = new AtomicReference<>();

        try
            {
            System.setProperty("coherence.daemonpool.slots", "8");

            pool.setDaemonCountMin(4);
            pool.setDaemonCountMax(4);
            pool.setDaemonCount(4);
            pool.start();

            if (isWakeupNudgeEnabled())
                {
                waitForCondition(() -> pool.countIdleDaemons() == 4, 5000,
                        "expected all daemons to publish as idle before skewed work starts");
                }
            else
                {
                Base.sleep(250L);
                }

            for (int iAssoc = 0; iAssoc < 4; iAssoc++)
                {
                HotAssociation association = new HotAssociation(iAssoc);

                pool.add(new BlockingAssociatedTask(association, 0, latchRelease, latchDone,
                        cActive, cMaxActive, cStarted, acSequence, refFailure));
                pool.add(new BlockingAssociatedTask(association, 1, latchRelease, latchDone,
                        cActive, cMaxActive, cStarted, acSequence, refFailure));
                }

            if (isWakeupNudgeEnabled())
                {
                waitForCondition(() -> cStarted.get() == 4, 5000,
                        "expected one active task per hot-slot association with wake-up nudging enabled");
                assertThat(cMaxActive.get(), is(4));
                }
            else
                {
                Base.sleep(500L);
                assertThat(cStarted.get(), is(1));
                assertThat(cMaxActive.get(), is(1));
                }

            latchRelease.countDown();
            assertTrue("expected all skewed tasks to complete", latchDone.await(10, TimeUnit.SECONDS));
            assertThat(cStarted.get(), is(8));
            assertNoTaskFailure(refFailure);
            }
        finally
            {
            latchRelease.countDown();
            stopPool(pool);
            restoreProperty("coherence.daemonpool.slots", sSlotsPrevious);
            }
        }

    @Test
    public void shouldWakeOffQueueStealersWhenTargetQueueAlreadyHasSharedWaiters() throws Exception
        {
        String         sSlotsPrevious = System.getProperty("coherence.daemonpool.slots");
        TestDaemonPool pool           = new TestDaemonPool();
        CountDownLatch latchDone      = new CountDownLatch(3);
        CountDownLatch latchRelease   = new CountDownLatch(1);
        AtomicInteger  cActive        = new AtomicInteger();
        AtomicInteger  cMaxActive     = new AtomicInteger();
        AtomicInteger  cStarted       = new AtomicInteger();

        try
            {
            System.setProperty("coherence.daemonpool.slots", "2");

            pool.setDaemonCountMin(3);
            pool.setDaemonCountMax(3);
            pool.setDaemonCount(3);
            pool.start();

            if (isWakeupNudgeEnabled())
                {
                waitForCondition(() -> pool.countIdleDaemons() == 3, 5000,
                        "expected all daemons to publish as idle before shared-queue work starts");
                }
            else
                {
                Base.sleep(250L);
                }

            assertThat(pool.countDaemonsForSlot(0), is(2));

            for (int i = 0; i < 3; i++)
                {
                pool.add(new BlockingCountedAssociatedTask(new SlotAssociation(0, i), latchRelease, latchDone,
                        cStarted, cActive, cMaxActive));
                }

            if (isWakeupNudgeEnabled())
                {
                waitForCondition(() -> cStarted.get() == 3 && cMaxActive.get() == 3, 5000,
                        "expected off-queue daemons to steal once shared-queue waiters are already waking locally");
                assertThat(cMaxActive.get(), is(3));
                }
            else
                {
                Base.sleep(500L);
                assertThat(cStarted.get(), is(2));
                assertThat(cMaxActive.get(), is(2));
                }

            latchRelease.countDown();
            assertTrue("expected shared-queue tasks to complete", latchDone.await(10, TimeUnit.SECONDS));
            }
        finally
            {
            latchRelease.countDown();
            stopPool(pool);
            restoreProperty("coherence.daemonpool.slots", sSlotsPrevious);
            }
        }

    @Test
    public void shouldSkipWakeupNudgeWhileSplitSlotIsInactive() throws Exception
        {
        if (!isWakeupNudgeEnabled())
            {
            return;
            }

        String         sSlotsPrevious = System.getProperty("coherence.daemonpool.slots");
        TestDaemonPool pool           = new TestDaemonPool();
        CountDownLatch latchStartTask = new CountDownLatch(1);
        CountDownLatch latchActivate  = new CountDownLatch(1);
        CountDownLatch latchSplitDone = new CountDownLatch(1);

        try
            {
            System.setProperty("coherence.daemonpool.slots", "3");

            pool.setDaemonCountMin(2);
            pool.setDaemonCountMax(2);
            pool.setDaemonCount(2);
            pool.start();

            waitForCondition(() -> pool.countIdleDaemons() == 2, 5000,
                    "expected both daemons to publish as idle before queue-split setup");

            int iSharedSlot = pool.findSharedSlotIndex();
            assertTrue("expected a shared slot before triggering a queue split", iSharedSlot >= 0);

            pool.setStartTaskGate(latchStartTask, latchActivate);
            pool.requestStartDaemon();

            assertTrue("expected the split StartTask to pause before activating the split daemon",
                    latchStartTask.await(10, TimeUnit.SECONDS));
            waitForCondition(() -> pool.findInactiveSlotIndex() >= 0, 5000,
                    "expected one slot to remain inactive until StartTask activates the split daemon");

            int iInactiveSlot = pool.findInactiveSlotIndex();
            int cIdleBefore    = pool.countIdleDaemons();
            int cNudgesBefore  = pool.getWakeupNudgeCount();

            pool.add(new SignalAssociatedTask(new SlotAssociation(iInactiveSlot, 1), latchSplitDone));

            Base.sleep(250L);

            assertThat(pool.countIdleDaemons(), is(cIdleBefore));
            assertThat(pool.getWakeupNudgeCount(), is(cNudgesBefore));
            assertFalse("inactive-slot add should not complete before StartTask activates the split daemon",
                    latchSplitDone.await(250L, TimeUnit.MILLISECONDS));

            latchActivate.countDown();

            assertTrue("expected the queue-split task to complete after StartTask activation",
                    latchSplitDone.await(10, TimeUnit.SECONDS));
            waitForCondition(() -> pool.findInactiveSlotIndex() < 0, 5000,
                    "expected StartTask to reactivate the split slot once the new daemon starts");
            }
        finally
            {
            latchActivate.countDown();
            stopPool(pool);
            restoreProperty("coherence.daemonpool.slots", sSlotsPrevious);
            }
        }

    @Test
    public void shouldCleanIdleStackAfterReplacingAbandonedDaemon() throws Exception
        {
        if (!isWakeupNudgeEnabled())
            {
            return;
            }

        String         sSlotsPrevious = System.getProperty("coherence.daemonpool.slots");
        TestDaemonPool pool           = new TestDaemonPool();
        CountDownLatch latchDone      = new CountDownLatch(1);

        try
            {
            System.setProperty("coherence.daemonpool.slots", "2");

            pool.setDaemonCountMin(2);
            pool.setDaemonCountMax(2);
            pool.setDaemonCount(2);
            pool.start();

            waitForCondition(() -> pool.countIdleDaemons() == 2, 5000,
                    "expected both daemons to publish as idle before replacement");

            DaemonPool.Daemon daemonOld = pool.getIdleDaemonHead();
            assertThat(daemonOld, is(notNullValue()));

            daemonOld.setExiting(true);
            daemonOld.setDaemonType(DaemonPool.DAEMON_ABANDONED);

            pool.replaceDaemon(daemonOld);

            waitForCondition(() -> !pool.containsDaemon(daemonOld), 5000,
                    "expected replaced daemon to leave the pool daemon array");
            assertTrue("expected no duplicate idle entries after replacement", pool.countIdleDaemons() <= 2);

            pool.add(new SignalAssociatedTask(new HotAssociation(0), latchDone));

            assertTrue("expected follow-up task to complete", latchDone.await(10, TimeUnit.SECONDS));
            waitForCondition(() -> !pool.containsIdleDaemon(daemonOld), 5000,
                    "expected replaced daemon to be removed from the idle stack");
            waitForCondition(() -> daemonOld.getThread() == null, 5000,
                    "expected replaced daemon thread to exit after replacement");
            }
        finally
            {
            stopPool(pool);
            restoreProperty("coherence.daemonpool.slots", sSlotsPrevious);
            }
        }

    @Test
    public void shouldGrowEvenWhenUnderutilizedAfterShrink()
        {
        ResizeTaskDaemonPoolStub pool       = new ResizeTaskDaemonPoolStub();
        DaemonPool.ResizeTask    resizeTask = createResizeTask(pool, 4, 1, 10);

        resizeTask.setActiveCountAverage(0.5d);
        resizeTask.setLastThroughput(1.0d);

        assertThat(resizeTask.growDaemonPool("test"), is(8));
        assertThat(pool.getDaemonCount(), is(8));
        }

    @Test
    public void shouldShrinkLinearly()
        {
        ResizeTaskDaemonPoolStub pool       = new ResizeTaskDaemonPoolStub();
        DaemonPool.ResizeTask    resizeTask = createResizeTask(pool, 8, 1, 10);

        assertThat(resizeTask.shrinkDaemonPool("test"), is(7));
        assertThat(pool.getDaemonCount(), is(7));
        }

    @Test
    public void shouldWaitForConsecutiveDropsBeforeReversingGrowth()
        {
        ResizeTaskDaemonPoolStub pool       = new ResizeTaskDaemonPoolStub();
        DaemonPool.ResizeTask    resizeTask = createResizeTask(pool, 4, 1, 10);

        configureRun(resizeTask, pool, 4, 0L, 0L, 10.0d, 1, 500L, 4);
        pool.setStatsTaskCount(8L);

        resizeTask.run();

        assertThat(pool.getDaemonCount(), is(4));
        assertThat(pool.getScheduledMillis(), is(100L));
        assertThat(resizeTask.getConsecutiveDropsAfterGrow(), is(1));
        assertThat(resizeTask.getLastResize(), is(1));

        resizeTask.setLastRunMillis(Base.getSafeTimeMillis() - 1000L);
        pool.setStatsTaskCount(14L);

        resizeTask.run();

        assertThat(pool.getDaemonCount(), is(3));
        assertThat(pool.getScheduledMillis(), is(625L));
        assertThat(resizeTask.getConsecutiveDropsAfterGrow(), is(0));
        assertThat(resizeTask.getLastResize(), is(-1));
        }

    @Test
    public void shouldNotAccelerateAfterSuccessfulShrink()
        {
        ResizeTaskDaemonPoolStub pool       = new ResizeTaskDaemonPoolStub();
        DaemonPool.ResizeTask    resizeTask = createResizeTask(pool, 4, 1, 10);

        configureRun(resizeTask, pool, 4, 0L, 0L, 8.0d, -1, 500L, 2);
        pool.setStatsTaskCount(10000L);

        resizeTask.run();

        assertThat(pool.getDaemonCount(), is(3));
        assertThat(pool.getScheduledMillis(), is(500L));
        }

    @Test
    public void shouldPreserveLastResizeOnExternalResize()
        {
        ResizeTaskDaemonPoolStub pool       = new ResizeTaskDaemonPoolStub();
        DaemonPool.ResizeTask    resizeTask = createResizeTask(pool, 4, 1, 10);

        configureRun(resizeTask, pool, 4, 0L, 0L, 10.0d, -1, 500L, 4);
        pool.setDaemonCount(6);

        resizeTask.run();

        assertThat(resizeTask.getLastResize(), is(2));
        assertThat(resizeTask.getLastThreadCount(), is(6));
        }

    @Test
    public void shouldPreserveLastResizeOnStatisticsSuspect()
        {
        ResizeTaskDaemonPoolStub pool       = new ResizeTaskDaemonPoolStub();
        DaemonPool.ResizeTask    resizeTask = createResizeTask(pool, 4, 1, 10);

        configureRun(resizeTask, pool, 4, 0L, 0L, 10.0d, 1, 500L, 4);
        pool.setStatsLastResetMillis(Base.getSafeTimeMillis());

        resizeTask.run();

        assertThat(resizeTask.getLastResize(), is(1));
        assertThat(pool.getScheduledMillis(), is(100L));
        }

    @Test
    public void shouldIncrementConsecutiveOverutilizedWhenPinnedAtMax()
        {
        ResizeTaskDaemonPoolStub pool       = new ResizeTaskDaemonPoolStub();
        DaemonPool.ResizeTask    resizeTask = createResizeTask(pool, 4, 1, 4);

        DaemonPool.ResizeTask.setOverutilizedShakeCount(2);
        DaemonPool.ResizeTask.setOverutilizedShakeCountMax(3);

        configureRun(resizeTask, pool, 4, 0L, 0L, 10.0d, 0, 500L, 4);

        pool.setStatsTaskCount(10L);
        resizeTask.run();
        assertThat(resizeTask.getConsecutiveOverutilized(), is(1));
        assertThat(resizeTask.getEffectiveShakePeriod(), is(600000L));

        resizeTask.setLastRunMillis(Base.getSafeTimeMillis() - 1000L);
        pool.setStatsTaskCount(20L);
        resizeTask.run();
        assertThat(resizeTask.getConsecutiveOverutilized(), is(2));
        assertThat(resizeTask.getEffectiveShakePeriod(), is(30000L));

        resizeTask.setLastRunMillis(Base.getSafeTimeMillis() - 1000L);
        pool.setStatsTaskCount(30L);
        resizeTask.run();
        assertThat(resizeTask.getConsecutiveOverutilized(), is(3));
        assertThat(resizeTask.getEffectiveShakePeriod(), is(10000L));
        }

    @Test
    public void shouldResetConsecutiveOverutilizedOnResize()
        {
        ResizeTaskDaemonPoolStub pool       = new ResizeTaskDaemonPoolStub();
        DaemonPool.ResizeTask    resizeTask = createResizeTask(pool, 4, 1, 10);

        configureRun(resizeTask, pool, 4, 0L, 0L, 10.0d, 0, 500L, 4);
        resizeTask.setConsecutiveOverutilized(3);
        pool.setStatsTaskCount(10L);

        resizeTask.run();

        assertThat(pool.getDaemonCount(), is(8));
        assertThat(resizeTask.getConsecutiveOverutilized(), is(0));
        }

    @Test
    public void shouldUseAdaptiveShakePeriodsAfterSustainedOverutilization()
        {
        ResizeTaskDaemonPoolStub pool       = new ResizeTaskDaemonPoolStub();
        DaemonPool.ResizeTask    resizeTask = createResizeTask(pool, 4, 1, 10);

        resizeTask.setConsecutiveOverutilized(0);
        assertThat(resizeTask.getEffectiveShakePeriod(), is(600000L));

        resizeTask.setConsecutiveOverutilized(10);
        assertThat(resizeTask.getEffectiveShakePeriod(), is(30000L));

        resizeTask.setConsecutiveOverutilized(30);
        assertThat(resizeTask.getEffectiveShakePeriod(), is(10000L));
        }

    @Test
    public void shouldStopVirtualPoolWithAssociationAllQueuedBehindAssociatedWork()
            throws Exception
        {
        Service.VirtualDaemonPool pool = createVirtualPool(0);
        pool.start();

        VirtualBlockingTask taskAssociated = new VirtualBlockingTask(1);
        VirtualBlockingTask taskAll        = new VirtualBlockingTask(AssociationPile.ASSOCIATION_ALL);

        pool.add(taskAssociated);
        assertTrue(taskAssociated.awaitStarted());

        pool.add(taskAll);
        assertFalse(taskAll.awaitStarted(200));

        pool.stop();

        assertTrue(taskAssociated.awaitDone());
        assertFalse(taskAll.awaitStarted(200));
        assertFalse(pool.isStarted());
        assertEventually(() -> pool.getActiveDaemonCount() == 0);
        assertEventually(() -> pool.getBacklog() == 0);
        }

    @Test
    public void shouldStopVirtualPoolWithAssociatedWorkBlockedBehindAssociationAll()
            throws Exception
        {
        Service.VirtualDaemonPool pool = createVirtualPool(0);
        pool.start();

        VirtualBlockingTask taskAll        = new VirtualBlockingTask(AssociationPile.ASSOCIATION_ALL);
        VirtualBlockingTask taskAssociated = new VirtualBlockingTask(1);

        pool.add(taskAll);
        assertTrue(taskAll.awaitStarted());

        pool.add(taskAssociated);
        assertFalse(taskAssociated.awaitStarted(200));

        pool.stop();

        assertTrue(taskAll.awaitDone());
        assertFalse(taskAssociated.awaitStarted(200));
        assertFalse(pool.isStarted());
        assertEventually(() -> pool.getActiveDaemonCount() == 0);
        assertEventually(() -> pool.getBacklog() == 0);
        }

    @Test
    public void shouldRecoverGuardedAssociatedVirtualTask()
            throws Exception
        {
        RecordingGuardian       guardian   = new RecordingGuardian();
        Service.VirtualDaemonPool pool     = createVirtualPool(0, guardian);
        pool.start();

        VirtualBlockingTask taskFirst  = new VirtualBlockingTask(1);
        VirtualBlockingTask taskSecond = new VirtualBlockingTask(1);

        pool.add(taskFirst);
        assertTrue(taskFirst.awaitStarted());

        pool.add(taskSecond);
        assertFalse(taskSecond.awaitStarted(200));

        Guardable guardable = guardian.awaitLatestGuardable(1);
        guardable.recover();

        assertTrue(taskFirst.awaitDone());
        assertTrue(taskFirst.wasInterrupted());
        assertTrue(taskSecond.awaitStarted());

        taskSecond.release();
        assertTrue(taskSecond.awaitDone());

        pool.stop();
        assertFalse(pool.isStarted());
        assertEventually(() -> pool.getActiveDaemonCount() == 0);
        }

    @Test
    public void shouldEscalateGuardedVirtualTaskTerminateToServiceGuardable()
            throws Exception
        {
        RecordingGuardian     guardian = new RecordingGuardian();
        TestService           service  = createVirtualService(0, guardian);
        Service.VirtualDaemonPool pool = (Service.VirtualDaemonPool) service.getDaemonPool();
        pool.setAbandonThreshold(3);
        pool.start();

        InterruptSwallowingTask task = new InterruptSwallowingTask(1);
        pool.add(task);
        assertTrue(task.awaitStarted());

        Guardable guardable = guardian.awaitLatestGuardable(1);
        for (int i = 0, c = pool.getAbandonThreshold(); i < c; i++)
            {
            guardable.recover();
            }

        guardable.terminate();

        assertTrue(service.awaitTerminateInvoked());
        assertEventually(() -> task.getInterruptCount() > 0);

        task.release();
        assertTrue(task.awaitDone());

        pool.stop();
        assertFalse(pool.isStarted());
        assertEventually(() -> pool.getActiveDaemonCount() == 0);
        }

    private DaemonPool.ResizeTask createResizeTask(ResizeTaskDaemonPoolStub pool, int cThreads, int cMin, int cMax)
        {
        pool.setDaemonCountMin(cMin);
        pool.setDaemonCountMax(cMax);
        pool.setDaemonCount(cThreads);

        DaemonPool.ResizeTask resizeTask = (DaemonPool.ResizeTask) pool._newChild("ResizeTask");
        resizeTask.setLastShakeMillis(Base.getSafeTimeMillis());
        resizeTask.setLastThreadCount(cThreads);
        return resizeTask;
        }

    private void configureRun(DaemonPool.ResizeTask resizeTask, ResizeTaskDaemonPoolStub pool, int cThreads,
            long cTasksLast, long cActiveMillisLast, double dflLastThroughput, int cLastResize, long cPeriod,
            int cActiveDaemonCount)
        {
        long ldtNow = Base.getSafeTimeMillis();

        pool.setStatsTaskCount(cTasksLast);
        pool.setStatsActiveMillis(cActiveMillisLast);
        pool.setStatsLastResetMillis(0L);
        pool.setStatsLastResizeMillis(0L);
        pool.setActiveDaemonCount(cActiveDaemonCount);

        resizeTask.setLastRunMillis(ldtNow - 1000L);
        resizeTask.setLastResizeMillis(ldtNow - 2000L);
        resizeTask.setLastTaskCount(cTasksLast);
        resizeTask.setLastActiveMillis(cActiveMillisLast);
        resizeTask.setLastThroughput(dflLastThroughput);
        resizeTask.setLastResize(cLastResize);
        resizeTask.setPeriodMillis(cPeriod);
        resizeTask.setActiveCountAverage(cActiveDaemonCount);
        resizeTask.setLastShakeMillis(ldtNow);
        resizeTask.setLastThreadCount(cThreads);
        }

    private Service.VirtualDaemonPool createVirtualPool(int cTaskLimit)
        {
        return createVirtualPool(cTaskLimit, null);
        }

    private Service.VirtualDaemonPool createVirtualPool(int cTaskLimit, Guardian guardian)
        {
        TestService service = createVirtualService(cTaskLimit, guardian);
        return (Service.VirtualDaemonPool) service.getDaemonPool();
        }

    private TestService createVirtualService(int cTaskLimit, Guardian guardian)
        {
        TestServiceDependencies deps = new TestServiceDependencies();
        deps.setThreadPriority(Thread.NORM_PRIORITY);
        deps.setDefaultWorkerThreadCountMin(1);
        deps.setDaemonPoolType(DaemonPoolType.VIRTUAL);
        deps.setTaskLimit(cTaskLimit);

        TestService service = new TestService("VirtualDaemonPoolIT-" + cTaskLimit, guardian);
        service.setDependencies(deps);

        Service.VirtualDaemonPool pool = (Service.VirtualDaemonPool) service.getDaemonPool();
        pool.setInternalGuardian(service);
        return service;
        }

    private void assertEventually(BooleanSupplier supplier)
            throws Exception
        {
        long ldtDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < ldtDeadline)
            {
            if (supplier.getAsBoolean())
                {
                return;
                }

            Thread.sleep(10L);
            }

        assertTrue(supplier.getAsBoolean());
        }

    protected void assertNoTaskFailure(AtomicReference<Throwable> refFailure)
        {
        Throwable failure = refFailure.get();
        if (failure != null)
            {
            throw failure instanceof AssertionError
                    ? (AssertionError) failure
                    : new AssertionError("unexpected task failure", failure);
            }
        }

    protected static boolean isWakeupNudgeEnabled()
        {
        return Boolean.parseBoolean(System.getProperty("coherence.daemonpool.wakeup.nudge", "true"));
        }

    protected static void restoreProperty(String sName, String sValue)
        {
        if (sValue == null)
            {
            System.clearProperty(sName);
            }
        else
            {
            System.setProperty(sName, sValue);
            }
        }

    protected void stopPool(DaemonPool pool)
        {
        if (pool.isStarted())
            {
            pool.stop();
            }
        }

    protected void waitForCondition(BooleanSupplier supplier, long cMillis, String sMessage) throws Exception
        {
        long ldtStop = Base.getSafeTimeMillis() + cMillis;
        while (Base.getSafeTimeMillis() < ldtStop)
            {
            if (supplier.getAsBoolean())
                {
                return;
                }
            Base.sleep(10L);
            }

        assertTrue(sMessage, supplier.getAsBoolean());
        }

    // ----- inner class: DaemonPoolStub ------------------------------------

    protected static class DaemonPoolStub
            extends DaemonPool
        {
        @Override
        public synchronized void setDaemonCount(int cThreads)
            {
            if (m_latchNotify != null)
                {
                m_latchNotify.countDown();
                }
            if (m_latchWait != null)
                {
                try
                    {
                    m_latchWait.await();
                    }
                catch (InterruptedException e)
                    {
                    throw new RuntimeException(e);
                    }
                }
            super.setDaemonCount(cThreads);
            }

        public void setLatch(CountDownLatch latchNotify, CountDownLatch latchWait)
            {
            m_latchNotify = latchNotify;
            m_latchWait   = latchWait;
            }

        protected CountDownLatch m_latchNotify;

        protected CountDownLatch m_latchWait;
        }

    protected static class ResizeTaskDaemonPoolStub
            extends DaemonPool
        {
        @Override
        public void flushStats()
            {
            }

        @Override
        public int getActiveDaemonCount()
            {
            return m_cActiveDaemonCount;
            }

        @Override
        public long getStatsActiveMillis()
            {
            return m_cStatsActiveMillis;
            }

        @Override
        public long getStatsLastResetMillis()
            {
            return m_ldtStatsLastResetMillis;
            }

        @Override
        public long getStatsLastResizeMillis()
            {
            return m_ldtStatsLastResizeMillis;
            }

        @Override
        public long getStatsTaskCount()
            {
            return m_cStatsTaskCount;
            }

        @Override
        public void schedule(Runnable task, long cMillis)
            {
            m_cScheduledMillis = cMillis;
            }

        public long getScheduledMillis()
            {
            return m_cScheduledMillis;
            }

        public void setActiveDaemonCount(int cActiveDaemonCount)
            {
            m_cActiveDaemonCount = cActiveDaemonCount;
            }

        public void setStatsActiveMillis(long cStatsActiveMillis)
            {
            m_cStatsActiveMillis = cStatsActiveMillis;
            }

        public void setStatsLastResetMillis(long ldtStatsLastResetMillis)
            {
            m_ldtStatsLastResetMillis = ldtStatsLastResetMillis;
            }

        public void setStatsLastResizeMillis(long ldtStatsLastResizeMillis)
            {
            m_ldtStatsLastResizeMillis = ldtStatsLastResizeMillis;
            }

        public void setStatsTaskCount(long cStatsTaskCount)
            {
            m_cStatsTaskCount = cStatsTaskCount;
            }

        protected int  m_cActiveDaemonCount;
        protected long m_cScheduledMillis;
        protected long m_cStatsActiveMillis;
        protected long m_cStatsTaskCount;
        protected long m_ldtStatsLastResetMillis;
        protected long m_ldtStatsLastResizeMillis;
        }

    protected static class TestDaemonPool
            extends DaemonPool
        {
        @Override
        protected void nudgeIdleDaemon(com.oracle.coherence.common.util.AssociationPile queueTarget)
            {
            m_cWakeupNudges++;
            super.nudgeIdleDaemon(queueTarget);
            }

        @Override
        public DaemonPool.WrapperTask instantiateWrapperTask(Runnable task, boolean fAggressiveTimeout)
            {
            if (task instanceof DaemonPool.StartTask && ((DaemonPool.StartTask) task).getDaemon() != null
                    && m_latchStartTask != null)
                {
                task = new GatedStartTask(this, (DaemonPool.StartTask) task, m_latchStartTask, m_latchActivate);
                }

            return super.instantiateWrapperTask(task, fAggressiveTimeout);
            }

        public void setStartTaskGate(CountDownLatch latchStartTask, CountDownLatch latchActivate)
            {
            m_latchStartTask = latchStartTask;
            m_latchActivate  = latchActivate;
            }

        public DaemonPool.Daemon getIdleDaemonHead()
            {
            return getIdleDaemonStack().getReference();
            }

        public int getWakeupNudgeCount()
            {
            return m_cWakeupNudges;
            }

        public boolean containsDaemon(DaemonPool.Daemon daemon)
            {
            for (DaemonPool.Daemon daemonTest : getDaemons())
                {
                if (daemonTest == daemon)
                    {
                    return true;
                    }
                }
            return false;
            }

        public boolean containsIdleDaemon(DaemonPool.Daemon daemon)
            {
            return indexOfIdleDaemon(daemon) >= 0;
            }

        public int countDaemonsForSlot(int iSlot)
            {
            return countDaemonsForQueue(getWorkSlot(iSlot).getQueue());
            }

        public int countIdleDaemons()
            {
            int               cDaemons = 0;
            DaemonPool.Daemon daemon   = getIdleDaemonHead();

            while (daemon != null && cDaemons < 32)
                {
                cDaemons++;
                daemon = daemon.getNextIdleDaemon();
                }

            return cDaemons;
            }

        public int findInactiveSlotIndex()
            {
            for (int i = 0, c = getWorkSlotCount(); i < c; i++)
                {
                if (!getWorkSlot(i).isActive())
                    {
                    return i;
                    }
                }

            return -1;
            }

        public int findSharedSlotIndex()
            {
            for (int i = 0, c = getWorkSlotCount(); i < c; i++)
                {
                if (countSlotsForQueue(getWorkSlot(i).getQueue()) > 1)
                    {
                    return i;
                    }
                }

            return -1;
            }

        public void requestStartDaemon()
            {
            DaemonPool.StartTask task = (DaemonPool.StartTask) _newChild("StartTask");
            task.setStartCount(1);
            startDaemon(task);
            }

        protected int indexOfIdleDaemon(DaemonPool.Daemon daemonFind)
            {
            int               i      = 0;
            DaemonPool.Daemon daemon = getIdleDaemonHead();

            while (daemon != null && i < 32)
                {
                if (daemon == daemonFind)
                    {
                    return i;
                    }

                daemon = daemon.getNextIdleDaemon();
                i++;
                }

            return -1;
            }

        protected int countDaemonsForQueue(Object queue)
            {
            int cDaemons = 0;

            for (DaemonPool.Daemon daemon : getDaemons())
                {
                if (daemon.getQueue() == queue)
                    {
                    cDaemons++;
                    }
                }

            return cDaemons;
            }

        protected int countSlotsForQueue(Object queue)
            {
            int cSlots = 0;

            for (int i = 0, c = getWorkSlotCount(); i < c; i++)
                {
                if (getWorkSlot(i).getQueue() == queue)
                    {
                    cSlots++;
                    }
                }

            return cSlots;
            }

        protected int m_cWakeupNudges;
        protected volatile CountDownLatch m_latchStartTask;
        protected volatile CountDownLatch m_latchActivate;
        }

    protected static class GatedStartTask
            extends DaemonPool.StartTask
        {
        protected GatedStartTask(DaemonPool pool, DaemonPool.StartTask task, CountDownLatch latchStartTask,
                CountDownLatch latchActivate)
            {
            super(null, pool, true);

            setDaemon(task.getDaemon());
            setQueue(task.getQueue());
            setStartCount(task.getStartCount());
            setWorkSlotActivate(task.getWorkSlotActivate());

            f_latchStartTask = latchStartTask;
            f_latchActivate  = latchActivate;
            }

        @Override
        public void run()
            {
            f_latchStartTask.countDown();
            try
                {
                if (!f_latchActivate.await(10, TimeUnit.SECONDS))
                    {
                    throw new AssertionError("timed out waiting to activate split daemon");
                    }
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw new AssertionError("unexpected interruption while waiting to activate split daemon", e);
                }

            super.run();
            }

        private final CountDownLatch f_latchStartTask;
        private final CountDownLatch f_latchActivate;
        }

    protected static class HotAssociation
        {
        protected HotAssociation(int nId)
            {
            m_nId = nId;
            }

        public int getId()
            {
            return m_nId;
            }

        @Override
        public boolean equals(Object oThat)
            {
            if (this == oThat)
                {
                return true;
                }

            if (!(oThat instanceof HotAssociation))
                {
                return false;
                }

            return m_nId == ((HotAssociation) oThat).m_nId;
            }

        @Override
        public int hashCode()
            {
            return 0;
            }

        private final int m_nId;
        }

    protected static class SlotAssociation
        {
        protected SlotAssociation(int iSlot, int nId)
            {
            m_iSlot = iSlot;
            m_nId   = nId;
            }

        @Override
        public boolean equals(Object oThat)
            {
            if (this == oThat)
                {
                return true;
                }

            if (!(oThat instanceof SlotAssociation))
                {
                return false;
                }

            SlotAssociation that = (SlotAssociation) oThat;
            return m_iSlot == that.m_iSlot && m_nId == that.m_nId;
            }

        @Override
        public int hashCode()
            {
            return m_iSlot;
            }

        private final int m_iSlot;
        private final int m_nId;
        }

    protected static class BlockingAssociatedTask
            implements KeyAssociation, Runnable
        {
        protected BlockingAssociatedTask(HotAssociation association, int nSequence, CountDownLatch latchRelease,
                CountDownLatch latchDone, AtomicInteger cActive, AtomicInteger cMaxActive, AtomicInteger cStarted,
                AtomicInteger[] acSequence, AtomicReference<Throwable> refFailure)
            {
            f_association = association;
            f_nSequence   = nSequence;
            f_latchDone   = latchDone;
            f_latchRelease = latchRelease;
            f_cActive     = cActive;
            f_cMaxActive  = cMaxActive;
            f_cStarted    = cStarted;
            f_acSequence  = acSequence;
            f_refFailure  = refFailure;
            }

        @Override
        public Object getAssociatedKey()
            {
            return f_association;
            }

        @Override
        public void run()
            {
            int cActive = f_cActive.incrementAndGet();
            f_cStarted.incrementAndGet();
            updateMax(f_cMaxActive, cActive);

            try
                {
                int nExpected = f_acSequence[f_association.getId()].getAndIncrement();
                if (nExpected != f_nSequence)
                    {
                    f_refFailure.compareAndSet(null, new AssertionError("association " + f_association.getId()
                            + " ran out of order: expected " + nExpected + " but was " + f_nSequence));
                    }

                if (!f_latchRelease.await(10, TimeUnit.SECONDS))
                    {
                    f_refFailure.compareAndSet(null, new AssertionError("timed out waiting to release skewed task"));
                    }
                }
            catch (Throwable e)
                {
                f_refFailure.compareAndSet(null, e);
                }
            finally
                {
                f_cActive.decrementAndGet();
                f_latchDone.countDown();
                }
            }

        protected static void updateMax(AtomicInteger cMax, int cValue)
            {
            while (true)
                {
                int cMaxCurrent = cMax.get();
                if (cMaxCurrent >= cValue || cMax.compareAndSet(cMaxCurrent, cValue))
                    {
                    return;
                    }
                }
            }

        private final HotAssociation f_association;
        private final int f_nSequence;
        private final CountDownLatch f_latchDone;
        private final CountDownLatch f_latchRelease;
        private final AtomicInteger f_cActive;
        private final AtomicInteger f_cMaxActive;
        private final AtomicInteger f_cStarted;
        private final AtomicInteger[] f_acSequence;
        private final AtomicReference<Throwable> f_refFailure;
        }

    protected static class BlockingCountedAssociatedTask
            implements KeyAssociation, Runnable
        {
        protected BlockingCountedAssociatedTask(Object association, CountDownLatch latchRelease,
                CountDownLatch latchDone, AtomicInteger cStarted, AtomicInteger cActive, AtomicInteger cMaxActive)
            {
            f_association = association;
            f_latchRelease = latchRelease;
            f_latchDone    = latchDone;
            f_cStarted     = cStarted;
            f_cActive      = cActive;
            f_cMaxActive   = cMaxActive;
            }

        @Override
        public Object getAssociatedKey()
            {
            return f_association;
            }

        @Override
        public void run()
            {
            f_cStarted.incrementAndGet();
            int cActive = f_cActive.incrementAndGet();
            BlockingAssociatedTask.updateMax(f_cMaxActive, cActive);

            try
                {
                if (!f_latchRelease.await(10, TimeUnit.SECONDS))
                    {
                    throw new AssertionError("timed out waiting to release associated task");
                    }
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw new AssertionError("unexpected interruption while blocking an associated task", e);
                }
            finally
                {
                f_cActive.decrementAndGet();
                f_latchDone.countDown();
                }
            }

        private final Object f_association;
        private final CountDownLatch f_latchRelease;
        private final CountDownLatch f_latchDone;
        private final AtomicInteger f_cStarted;
        private final AtomicInteger f_cActive;
        private final AtomicInteger f_cMaxActive;
        }

    protected static class SignalAssociatedTask
            implements KeyAssociation, Runnable
        {
        protected SignalAssociatedTask(Object association, CountDownLatch latchDone)
            {
            f_association = association;
            f_latchDone   = latchDone;
            }

        @Override
        public Object getAssociatedKey()
            {
            return f_association;
            }

        @Override
        public void run()
            {
            f_latchDone.countDown();
            }

        private final Object f_association;
        private final CountDownLatch f_latchDone;
        }

    protected static class TestService
            extends Service
            implements Guardian
        {
        public TestService(String sName)
            {
            this(sName, null);
            }

        public TestService(String sName, Guardian guardian)
            {
            super(sName, null, false);

            __initPrivate();
            setServiceName(sName);
            m_guardian = guardian;
            setGuardable(new TestServiceGuard());
            }

        @Override
        public GuardContext guard(Guardable guardable)
            {
            Guardian guardian = m_guardian;
            return guardian == null ? null : guardian.guard(guardable);
            }

        @Override
        public GuardContext guard(Guardable guardable, long cMillis, float flPctRecover)
            {
            Guardian guardian = m_guardian;
            return guardian == null ? null : guardian.guard(guardable, cMillis, flPctRecover);
            }

        @Override
        public long getDefaultGuardTimeout()
            {
            Guardian guardian = m_guardian;
            return guardian == null ? 0L : guardian.getDefaultGuardTimeout();
            }

        @Override
        public float getDefaultGuardRecovery()
            {
            Guardian guardian = m_guardian;
            return guardian == null ? 0.0F : guardian.getDefaultGuardRecovery();
            }

        public boolean awaitTerminateInvoked()
                throws InterruptedException
            {
            return f_terminated.await(5, TimeUnit.SECONDS);
            }

        protected class TestServiceGuard
                extends Service.Guard
            {
            public TestServiceGuard()
                {
                super("Guard", TestService.this, true);
                }

            @Override
            public void terminate()
                {
                f_terminated.countDown();
                }
            }

        private final CountDownLatch f_terminated = new CountDownLatch(1);

        private final Guardian m_guardian;
        }

    protected static class VirtualBlockingTask
            implements Runnable, KeyAssociation
        {
        public VirtualBlockingTask(Object oAssociation)
            {
            f_oAssociation = oAssociation;
            }

        @Override
        public Object getAssociatedKey()
            {
            return f_oAssociation;
            }

        @Override
        public void run()
            {
            f_started.countDown();

            try
                {
                f_release.await(5, TimeUnit.SECONDS);
                }
            catch (InterruptedException e)
                {
                m_fInterrupted = true;
                Thread.currentThread().interrupt();
                }
            finally
                {
                f_done.countDown();
                }
            }

        public boolean awaitDone()
                throws InterruptedException
            {
            return f_done.await(5, TimeUnit.SECONDS);
            }

        public boolean awaitStarted()
                throws InterruptedException
            {
            return awaitStarted(5_000L);
            }

        public boolean awaitStarted(long cMillis)
                throws InterruptedException
            {
            return f_started.await(cMillis, TimeUnit.MILLISECONDS);
            }

        public void release()
            {
            f_release.countDown();
            }

        public boolean wasInterrupted()
            {
            return m_fInterrupted;
            }

        private final Object         f_oAssociation;
        private final CountDownLatch f_done    = new CountDownLatch(1);
        private final CountDownLatch f_release = new CountDownLatch(1);
        private final CountDownLatch f_started = new CountDownLatch(1);

        private volatile boolean m_fInterrupted;
        }

    protected static class InterruptSwallowingTask
            implements Runnable, KeyAssociation
        {
        public InterruptSwallowingTask(Object oAssociation)
            {
            f_oAssociation = oAssociation;
            }

        @Override
        public Object getAssociatedKey()
            {
            return f_oAssociation;
            }

        @Override
        public void run()
            {
            f_started.countDown();

            try
                {
                while (!m_fReleased)
                    {
                    try
                        {
                        m_fReleased = f_release.await(100L, TimeUnit.MILLISECONDS);
                        }
                    catch (InterruptedException e)
                        {
                        m_cInterrupts++;
                        Thread.interrupted();
                        }
                    }
                }
            finally
                {
                f_done.countDown();
                }
            }

        public boolean awaitDone()
                throws InterruptedException
            {
            return f_done.await(5, TimeUnit.SECONDS);
            }

        public boolean awaitStarted()
                throws InterruptedException
            {
            return f_started.await(5, TimeUnit.SECONDS);
            }

        public int getInterruptCount()
            {
            return m_cInterrupts;
            }

        public void release()
            {
            m_fReleased = true;
            f_release.countDown();
            }

        private final Object         f_oAssociation;
        private final CountDownLatch f_done    = new CountDownLatch(1);
        private final CountDownLatch f_release = new CountDownLatch(1);
        private final CountDownLatch f_started = new CountDownLatch(1);

        private volatile boolean m_fReleased;
        private volatile int     m_cInterrupts;
        }

    protected static class RecordingGuardian
            implements Guardian
        {
        @Override
        public GuardContext guard(Guardable guardable)
            {
            return guard(guardable, getDefaultGuardTimeout(), getDefaultGuardRecovery());
            }

        @Override
        public GuardContext guard(Guardable guardable, long cMillis, float flPctRecover)
            {
            RecordingGuardContext context = new RecordingGuardContext(this, guardable, cMillis);
            guardable.setContext(context);
            f_listContext.add(context);
            return context;
            }

        @Override
        public long getDefaultGuardTimeout()
            {
            return 1000L;
            }

        @Override
        public float getDefaultGuardRecovery()
            {
            return 0.5F;
            }

        public Guardable awaitLatestGuardable(int cExpected)
                throws InterruptedException
            {
            long ldtDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < ldtDeadline)
                {
                if (f_listContext.size() >= cExpected)
                    {
                    return f_listContext.get(cExpected - 1).getGuardable();
                    }

                Thread.sleep(10L);
                }

            assertTrue(f_listContext.size() >= cExpected);
            return f_listContext.get(cExpected - 1).getGuardable();
            }

        private final CopyOnWriteArrayList<RecordingGuardContext> f_listContext = new CopyOnWriteArrayList<>();
        }

    protected static class RecordingGuardContext
            implements Guardian.GuardContext
        {
        public RecordingGuardContext(Guardian guardian, Guardable guardable, long cTimeoutMillis)
            {
            f_guardian       = guardian;
            f_guardable      = guardable;
            f_cTimeoutMillis = cTimeoutMillis;
            }

        @Override
        public Guardian getGuardian()
            {
            return f_guardian;
            }

        @Override
        public Guardable getGuardable()
            {
            return f_guardable;
            }

        @Override
        public void heartbeat()
            {
            m_nState = STATE_HEALTHY;
            }

        @Override
        public void heartbeat(long cMillis)
            {
            m_nState = STATE_HEALTHY;
            }

        @Override
        public int getState()
            {
            return m_nState;
            }

        @Override
        public void release()
            {
            m_nState = STATE_HEALTHY;
            }

        @Override
        public long getSoftTimeoutMillis()
            {
            return f_cTimeoutMillis;
            }

        @Override
        public long getTimeoutMillis()
            {
            return f_cTimeoutMillis;
            }

        private final long      f_cTimeoutMillis;
        private final Guardable f_guardable;
        private final Guardian  f_guardian;

        private volatile int m_nState = STATE_HEALTHY;
        }

    protected static class TestServiceDependencies
            extends DefaultServiceDependencies
        {
        @Override
        public void setDefaultWorkerThreadCountMin(int cThreads)
            {
            super.setDefaultWorkerThreadCountMin(cThreads);
            }
        }

    // ----- data members ---------------------------------------------------

    protected int    m_cGrowGraceCount;
    protected double m_dIdleFraction;
    protected int    m_nIdleLimit;
    protected int    m_cOverutilizedShakeCount;
    protected int    m_cOverutilizedShakeCountMax;
    protected long   m_cPeriodAdjust;
    protected long   m_cPeriodAdjustSlightlySlower;
    protected long   m_cPeriodMax;
    protected long   m_cPeriodMin;
    protected long   m_cPeriodShake;
    protected long   m_cPeriodShakeFast;
    protected long   m_cPeriodShakeFaster;
    protected double m_dResizeGrow;
    protected double m_dResizeJitter;
    protected double m_dResizeShake;
    protected double m_dResizeShrink;
    protected long   m_cShrinkCooldownMillis;

    /**
     * This rule is to ensure that the shouldNotDeadlockWhenStoppingPoolWhileResizing does not
     * cause the test to hang forever as when it fails it may cause a deadlock.
     */
    @ClassRule
    public static final ThreadDumpOnTimeoutRule timeout = ThreadDumpOnTimeoutRule.after(5, TimeUnit.MINUTES);
    }
