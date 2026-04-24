/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.component.util;

import com.oracle.coherence.testing.junit.ThreadDumpOnTimeoutRule;
import com.tangosol.util.Base;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
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
