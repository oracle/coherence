/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util;

import com.oracle.coherence.common.util.AssociationPile;

import com.tangosol.coherence.component.net.message.RequestMessage;
import com.tangosol.coherence.component.net.management.model.localModel.ServiceModel;
import com.tangosol.coherence.component.util.daemon.queueProcessor.Service;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;

import com.tangosol.internal.net.service.DefaultServiceDependencies;
import com.tangosol.internal.util.VirtualThreads;

import com.tangosol.net.DaemonPoolType;
import com.tangosol.net.Guardable;
import com.tangosol.net.Guardian;
import com.tangosol.net.PriorityTask;
import com.tangosol.net.cache.KeyAssociation;

import com.tangosol.util.ExternalizableHelper;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for virtual daemon pool task dispatch and accounting.
 *
 * @author Aleks Seovic  2026.04.24
 * @since 26.04
 */
public class VirtualDaemonPoolComponentTest
    {
    @Before
    public void before()
        {
        m_pool = createPool(0);
        m_pool.start();
        }

    @After
    public void after()
        {
        if (m_pool != null)
            {
            m_pool.stop();
            }
        }

    @Test
    public void shouldSerializeAssociatedTasksOnSameKey()
            throws Exception
        {
        AtomicInteger active    = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();

        BlockingTask taskFirst  = new BlockingTask(1, active, maxActive);
        BlockingTask taskSecond = new BlockingTask(1, active, maxActive);

        m_pool.add(taskFirst);
        assertTrue(taskFirst.awaitStarted());

        m_pool.add(taskSecond);
        assertFalse(taskSecond.awaitStarted(200));

        taskFirst.release();
        assertTrue(taskFirst.awaitDone());
        assertTrue(taskSecond.awaitStarted());
        taskSecond.release();
        assertTrue(taskSecond.awaitDone());

        assertThat(maxActive.get(), is(1));
        }

    @Test
    public void shouldRunAssociatedTasksOnDifferentKeysConcurrently()
            throws Exception
        {
        AtomicInteger active    = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();

        BlockingTask taskFirst  = new BlockingTask(1, active, maxActive);
        BlockingTask taskSecond = new BlockingTask(2, active, maxActive);

        m_pool.add(taskFirst);
        m_pool.add(taskSecond);

        assertTrue(taskFirst.awaitStarted());
        assertTrue(taskSecond.awaitStarted());
        assertTrue(maxActive.get() >= 2);
        assertThat(VirtualThreads.isVirtual(taskFirst.getThread()), is(true));
        assertThat(VirtualThreads.isVirtual(taskSecond.getThread()), is(true));

        taskFirst.release();
        taskSecond.release();

        assertTrue(taskFirst.awaitDone());
        assertTrue(taskSecond.awaitDone());
        }

    @Test
    public void shouldRunUnassociatedTasksConcurrently()
            throws Exception
        {
        AtomicInteger active    = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();

        BlockingTask taskFirst  = new BlockingTask(null, active, maxActive);
        BlockingTask taskSecond = new BlockingTask(null, active, maxActive);

        m_pool.add(taskFirst);
        m_pool.add(taskSecond);

        assertTrue(taskFirst.awaitStarted());
        assertTrue(taskSecond.awaitStarted());
        assertTrue(maxActive.get() >= 2);

        taskFirst.release();
        taskSecond.release();

        assertTrue(taskFirst.awaitDone());
        assertTrue(taskSecond.awaitDone());
        }

    @Test
    public void shouldBlockAssociatedTasksBehindAssociationAll()
            throws Exception
        {
        AtomicInteger active    = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();

        BlockingTask taskFirst  = new BlockingTask(1, active, maxActive);
        BlockingTask taskAll    = new BlockingTask(AssociationPile.ASSOCIATION_ALL, active, maxActive);
        BlockingTask taskSecond = new BlockingTask(2, active, maxActive);

        m_pool.add(taskFirst);
        assertTrue(taskFirst.awaitStarted());

        m_pool.add(taskAll);
        assertFalse(taskAll.awaitStarted(200));

        m_pool.add(taskSecond);
        assertFalse(taskSecond.awaitStarted(200));

        taskFirst.release();
        assertTrue(taskFirst.awaitDone());
        assertTrue(taskAll.awaitStarted());
        assertFalse(taskSecond.awaitStarted(200));

        taskAll.release();
        assertTrue(taskAll.awaitDone());
        assertTrue(taskSecond.awaitStarted());

        taskSecond.release();
        assertTrue(taskSecond.awaitDone());
        }

    @Test
    public void shouldAllowUnassociatedTasksDuringAssociationAll()
            throws Exception
        {
        AtomicInteger active    = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();

        BlockingTask taskAssociated   = new BlockingTask(1, active, maxActive);
        BlockingTask taskAll          = new BlockingTask(AssociationPile.ASSOCIATION_ALL, active, maxActive);
        BlockingTask taskUnassociated = new BlockingTask(null, active, maxActive);

        m_pool.add(taskAssociated);
        assertTrue(taskAssociated.awaitStarted());

        m_pool.add(taskAll);
        assertFalse(taskAll.awaitStarted(200));

        m_pool.add(taskUnassociated);
        assertTrue(taskUnassociated.awaitStarted());

        taskAssociated.release();
        assertTrue(taskAssociated.awaitDone());
        assertTrue(taskAll.awaitStarted());

        BlockingTask taskConcurrent = new BlockingTask(null, active, maxActive);
        m_pool.add(taskConcurrent);
        assertTrue(taskConcurrent.awaitStarted());

        taskUnassociated.release();
        taskConcurrent.release();
        assertTrue(taskUnassociated.awaitDone());
        assertTrue(taskConcurrent.awaitDone());

        taskAll.release();
        assertTrue(taskAll.awaitDone());
        }

    @Test
    public void shouldHonorTaskLimitForConcurrentExecution()
            throws Exception
        {
        m_pool.stop();
        m_pool = createPool(1);
        m_pool.start();

        AtomicInteger active    = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();

        BlockingTask taskFirst  = new BlockingTask(null, active, maxActive);
        BlockingTask taskSecond = new BlockingTask(null, active, maxActive);

        m_pool.add(taskFirst);
        assertTrue(taskFirst.awaitStarted());

        m_pool.add(taskSecond);
        assertFalse(taskSecond.awaitStarted(200));

        taskFirst.release();
        assertTrue(taskFirst.awaitDone());
        assertTrue(taskSecond.awaitStarted());

        taskSecond.release();
        assertTrue(taskSecond.awaitDone());
        assertThat(maxActive.get(), is(1));
        }

    @Test
    public void shouldNoOpThreadCountSettersOnVdp()
        {
        m_pool.stop();
        TestVirtualDaemonPool pool = createControlledPool(8);
        m_pool = pool;
        m_pool.start();

        pool.setDaemonCount(99);
        pool.setDaemonCountMin(99);
        pool.setDaemonCountMax(99);

        assertThat(pool.getDaemonCountMin(), is(0));
        assertThat(pool.getDaemonCountMax(), is(Integer.MAX_VALUE));
        assertThat(pool.getTaskLimit(), is(8));
        assertTrue(pool.hasNoEffectTrace("setThreadCount(99)"));
        assertTrue(pool.hasNoEffectTrace("setThreadCountMin(99)"));
        assertTrue(pool.hasNoEffectTrace("setThreadCountMax(99)"));
        }

    @Test
    public void shouldReshapeSemaphoreOnTaskLimitIncrease()
            throws Exception
        {
        m_pool.stop();
        m_pool = createPool(8);
        m_pool.start();

        AtomicInteger active    = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        BlockingTask[] tasks    = createBlockingTasks(64, null, active, maxActive);

        try
            {
            addAll(tasks);
            assertEventually(() -> m_pool.getActiveDaemonCount() == 8);

            m_pool.setTaskLimit(64);

            assertEventually(() -> m_pool.getActiveDaemonCount() == 64);
            assertThat(maxActive.get(), is(64));
            }
        finally
            {
            releaseAll(tasks);
            awaitAllDone(tasks);
            }
        }

    @Test
    public void shouldReshapeSemaphoreOnTaskLimitDecrease()
            throws Exception
        {
        m_pool.stop();
        m_pool = createPool(64);
        m_pool.start();

        AtomicInteger active    = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        BlockingTask[] tasks    = createBlockingTasks(80, null, active, maxActive);

        try
            {
            addAll(tasks);
            assertEventually(() -> m_pool.getActiveDaemonCount() == 64);

            m_pool.setTaskLimit(8);

            assertTrue(releaseStartedExceptFirst(tasks, 8) >= 56);
            assertEventually(() -> m_pool.getActiveDaemonCount() <= 8);
            assertThat(maxActive.get(), is(64));
            assertNoInterrupted(tasks);
            }
        finally
            {
            releaseAll(tasks);
            awaitAllDone(tasks);
            }
        }

    @Test
    public void shouldReshapeSemaphoreOnTaskLimitToUnbounded()
            throws Exception
        {
        m_pool.stop();
        m_pool = createPool(8);
        m_pool.start();

        AtomicInteger active    = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        BlockingTask[] tasks    = createBlockingTasks(80, null, active, maxActive);

        try
            {
            addAll(tasks);
            assertEventually(() -> m_pool.getActiveDaemonCount() == 8);

            m_pool.setTaskLimit(0);

            assertEventually(() -> m_pool.getActiveDaemonCount() == 80);
            assertThat(maxActive.get(), is(80));
            }
        finally
            {
            releaseAll(tasks);
            awaitAllDone(tasks);
            }
        }

    @Test
    public void shouldReshapeSemaphoreOnTaskLimitFromUnbounded()
            throws Exception
        {
        m_pool.stop();
        m_pool = createPool(0);
        m_pool.start();
        m_pool.setTaskLimit(8);

        AtomicInteger active    = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        BlockingTask[] tasks    = createBlockingTasks(80, null, active, maxActive);

        try
            {
            addAll(tasks);

            assertEventually(() -> m_pool.getActiveDaemonCount() == 8);
            assertThat(maxActive.get(), is(8));
            }
        finally
            {
            releaseAll(tasks);
            awaitAllDone(tasks);
            }
        }

    @Test
    public void shouldReportVirtualMBeanAttributes()
        {
        m_pool.stop();
        m_pool = createPool(10);
        m_pool.start();

        TestServiceModel model = new TestServiceModel("VirtualMBeanTest", m_pool);

        assertThat(model.getDaemonPoolType(), is("VIRTUAL"));
        assertThat(model.getTaskLimit(), is(10));
        }

    @Test
    public void shouldReportPlatformMBeanAttributes()
        {
        Service.DaemonPool pool = createPlatformPool(4, 10);
        TestServiceModel   model = new TestServiceModel("PlatformMBeanTest", pool);

        try
            {
            pool.start();

            assertThat(model.getDaemonPoolType(), is("PLATFORM"));
            assertThat(model.getTaskLimit(), is(-1));

            model.setTaskLimit(99);

            assertThat(model.getTaskLimit(), is(-1));
            assertTrue(model.hasTaskLimitNoEffectTrace("setTaskLimit(99)"));
            }
        finally
            {
            pool.stop();
            }
        }

    @Test
    public void shouldComputePoolSaturationForLegacyDp()
            throws Exception
        {
        Service.DaemonPool pool  = createPlatformPool(4, 10);
        TestServiceModel   model = new TestServiceModel("PlatformSaturationTest", pool);
        AtomicInteger      active = new AtomicInteger();
        AtomicInteger      max    = new AtomicInteger();
        BlockingTask[]     tasks  = createBlockingTasks(4, null, active, max);

        try
            {
            pool.start();
            for (BlockingTask task : tasks)
                {
                pool.add(task);
                }

            assertEventually(() -> pool.getActiveDaemonCount() == 4);
            assertThat(model.getPoolSaturation(), is(0.4d));
            }
        finally
            {
            releaseAll(tasks);
            awaitAllDone(tasks);
            pool.stop();
            }
        }

    @Test
    public void shouldComputePoolSaturationForBoundedVdp()
            throws Exception
        {
        m_pool.stop();
        m_pool = createPool(10);
        m_pool.start();

        TestServiceModel model  = new TestServiceModel("VirtualSaturationTest", m_pool);
        AtomicInteger    active = new AtomicInteger();
        AtomicInteger    max    = new AtomicInteger();
        BlockingTask[]   tasks  = createBlockingTasks(4, null, active, max);

        try
            {
            addAll(tasks);

            assertEventually(() -> m_pool.getActiveDaemonCount() == 4);
            assertThat(model.getPoolSaturation(), is(0.4d));
            }
        finally
            {
            releaseAll(tasks);
            awaitAllDone(tasks);
            }
        }

    @Test
    public void shouldReportNegativeOneForUnboundedVdpPoolSaturation()
        {
        TestServiceModel model = new TestServiceModel("UnboundedVirtualSaturationTest", m_pool);

        assertThat(model.getPoolSaturation(), is(-1.0d));
        }

    @Test
    public void shouldReportNegativeOneForPoolSaturationWhenPoolNotStarted()
        {
        Service.VirtualDaemonPool pool  = createPool(10);
        TestServiceModel          model = new TestServiceModel("StoppedVirtualSaturationTest", pool);

        assertThat(model.getPoolSaturation(), is(-1.0d));
        }

    @Test
    public void shouldReportLiveVtCountAsThreadCount()
            throws Exception
        {
        m_pool.stop();
        m_pool = createPool(64);
        m_pool.start();

        AtomicInteger active    = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        BlockingTask[] tasks    = createBlockingTasks(200, null, active, maxActive);

        try
            {
            addAll(tasks);

            assertEventually(() -> m_pool.getActiveDaemonCount() == 64);
            assertEventually(() -> m_pool.getDaemonCount() == 200);
            }
        finally
            {
            releaseAll(tasks);
            awaitAllDone(tasks);
            }
        }

    @Test
    public void shouldReportZeroForThreadCountMin()
        {
        assertThat(m_pool.getDaemonCountMin(), is(0));
        }

    @Test
    public void shouldReportMaxIntegerForThreadCountMax()
        {
        assertThat(m_pool.getDaemonCountMax(), is(Integer.MAX_VALUE));
        }

    @Test
    public void shouldReturnTrueForIsDynamic()
        {
        assertThat(m_pool.isDynamic(), is(true));
        }

    @Test
    public void shouldIncludeParkedVtsInBacklog()
            throws Exception
        {
        m_pool.stop();
        m_pool = createPool(64);
        m_pool.start();

        AtomicInteger active    = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        BlockingTask[] tasks    = createBlockingTasks(200, null, active, maxActive);

        try
            {
            addAll(tasks);

            assertEventually(() -> m_pool.getActiveDaemonCount() == 64);
            assertEventually(() -> m_pool.getBacklog() >= 100);
            }
        finally
            {
            releaseAll(tasks);
            awaitAllDone(tasks);
            }
        }

    @Test
    public void shouldIncludeMailboxQueueAndParkedInBacklog()
            throws Exception
        {
        m_pool.stop();
        TestVirtualDaemonPool pool = createControlledPool(64);
        m_pool = pool;
        m_pool.start();

        AtomicInteger active          = new AtomicInteger();
        AtomicInteger maxActive       = new AtomicInteger();
        BlockingTask[] tasksMailbox   = createBlockingTasks(100, "hot", active, maxActive);
        BlockingTask[] tasksDirect    = createBlockingTasks(100, null, active, maxActive);

        try
            {
            addAll(tasksMailbox);
            assertTrue(tasksMailbox[0].awaitStarted());

            addAll(tasksDirect);

            assertEventually(() -> pool.getQueuedBacklogCountForTest() > 0);
            assertEventually(() -> pool.getDaemonCount() > pool.getActiveDaemonCount());
            assertEventually(() -> pool.getBacklog() >= 100);
            }
        finally
            {
            releaseAll(tasksMailbox);
            releaseAll(tasksDirect);
            awaitAllDone(tasksMailbox);
            awaitAllDone(tasksDirect);
            }
        }

    @Test
    public void shouldClampNegativeBacklogToZero()
            throws Exception
        {
        m_pool.stop();
        m_pool = createPool(4);
        m_pool.start();

        AtomicInteger active    = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        BlockingTask[] tasks    = createBlockingTasks(40, null, active, maxActive);

        try
            {
            addAll(tasks);
            assertEventually(() -> m_pool.getActiveDaemonCount() == 4);

            for (int i = 0; i < 1_000; i++)
                {
                assertTrue(m_pool.getBacklog() >= 0);
                }

            releaseAll(tasks);

            for (int i = 0; i < 1_000; i++)
                {
                assertTrue(m_pool.getBacklog() >= 0);
                }
            }
        finally
            {
            releaseAll(tasks);
            awaitAllDone(tasks);
            }
        }

    @Test
    public void shouldKeepAssociationAllAheadOfLaterAssociatedWorkWhenTaskLimitIsOne()
            throws Exception
        {
        m_pool.stop();
        m_pool = createPool(1);
        m_pool.start();

        AtomicInteger active     = new AtomicInteger();
        AtomicInteger maxActive  = new AtomicInteger();
        BlockingTask  taskFirst  = new BlockingTask(1, active, maxActive);
        BlockingTask  taskAll    = new BlockingTask(AssociationPile.ASSOCIATION_ALL, active, maxActive);
        BlockingTask  taskSecond = new BlockingTask(2, active, maxActive);

        m_pool.add(taskFirst);
        assertTrue(taskFirst.awaitStarted());

        m_pool.add(taskAll);
        assertFalse(taskAll.awaitStarted(200));

        m_pool.add(taskSecond);
        assertFalse(taskSecond.awaitStarted(200));

        taskFirst.release();
        assertTrue(taskFirst.awaitDone());
        assertTrue(taskAll.awaitStarted());
        assertFalse(taskSecond.awaitStarted(200));

        taskAll.release();
        assertTrue(taskAll.awaitDone());
        assertTrue(taskSecond.awaitStarted());

        taskSecond.release();
        assertTrue(taskSecond.awaitDone());
        assertThat(maxActive.get(), is(1));
        }

    @Test
    public void shouldDrainLargeSingleKeyMailbox()
            throws Exception
        {
        m_pool.stop();
        TestVirtualDaemonPool pool = createControlledPool(0);
        m_pool = pool;
        m_pool.start();

        int            cTasks = 100_000;
        AtomicInteger  next   = new AtomicInteger();
        AtomicInteger  errors = new AtomicInteger();
        CountDownLatch done   = new CountDownLatch(cTasks);

        for (int i = 0; i < cTasks; i++)
            {
            m_pool.add(new OrderedTask("hot", i, next, errors, done));
            }

        assertTrue(done.await(30, TimeUnit.SECONDS));
        assertThat(next.get(), is(cTasks));
        assertThat(errors.get(), is(0));
        assertEventually(() -> pool.getBacklog() == 0);
        assertEventually(() -> pool.getMailboxCount() == 0);
        }

    @Test
    public void shouldBalancePermitsWhenAssociatedTaskThrows()
            throws Exception
        {
        m_pool.stop();
        TestVirtualDaemonPool pool = createControlledPool(1);
        m_pool = pool;
        m_pool.start();

        AtomicInteger active       = new AtomicInteger();
        AtomicInteger maxActive    = new AtomicInteger();
        ThrowingTask  taskThrowing = new ThrowingTask(1);
        BlockingTask  taskSameKey  = new BlockingTask(1, active, maxActive);
        BlockingTask  taskDirect   = new BlockingTask(null, active, maxActive);

        m_pool.add(taskThrowing);
        assertTrue(taskThrowing.awaitDone());
        assertEventually(() -> pool.getAvailableTaskPermits() == 1);

        m_pool.add(taskSameKey);
        assertTrue(taskSameKey.awaitStarted());

        m_pool.add(taskDirect);
        assertFalse(taskDirect.awaitStarted(200));

        taskSameKey.release();
        assertTrue(taskSameKey.awaitDone());
        assertTrue(taskDirect.awaitStarted());

        taskDirect.release();
        assertTrue(taskDirect.awaitDone());
        assertEventually(() -> pool.getAvailableTaskPermits() == 1);
        assertEventually(() -> pool.getBacklog() == 0);
        assertEventually(() -> pool.getMailboxCount() == 0);
        }

    @Test
    public void shouldRunImmediatePriorityTasksOnPlatformThreads()
            throws Exception
        {
        AtomicInteger active = new AtomicInteger();
        AtomicInteger max    = new AtomicInteger();

        BlockingTask taskAssociated = new BlockingTask(1, active, max);
        PriorityBlockingTask taskPriority = new PriorityBlockingTask(1, active, max,
                PriorityTask.SCHEDULE_IMMEDIATE);

        m_pool.add(taskAssociated);
        assertTrue(taskAssociated.awaitStarted());

        m_pool.add(taskPriority);
        assertTrue(taskPriority.awaitStarted());
        assertThat(taskPriority.getThread(), instanceOf(Thread.class));
        assertThat(VirtualThreads.isVirtual(taskPriority.getThread()), is(false));
        assertThat(taskPriority.getThread().getPriority(), is(Thread.MAX_PRIORITY));

        taskPriority.release();
        assertTrue(taskPriority.awaitDone());

        taskAssociated.release();
        assertTrue(taskAssociated.awaitDone());
        }

    @Test
    public void shouldRunFirstPriorityTasksOnPlatformThreads()
            throws Exception
        {
        AtomicInteger active = new AtomicInteger();
        AtomicInteger max    = new AtomicInteger();

        BlockingTask taskAssociated = new BlockingTask(1, active, max);
        PriorityBlockingTask taskPriority = new PriorityBlockingTask(1, active, max,
                PriorityTask.SCHEDULE_FIRST);

        m_pool.add(taskAssociated);
        assertTrue(taskAssociated.awaitStarted());

        m_pool.add(taskPriority);
        assertTrue(taskPriority.awaitStarted());
        assertThat(taskPriority.getThread(), instanceOf(Thread.class));
        assertThat(VirtualThreads.isVirtual(taskPriority.getThread()), is(false));
        assertThat(taskPriority.getThread().getPriority(), is(Thread.NORM_PRIORITY + 1));

        taskPriority.release();
        assertTrue(taskPriority.awaitDone());

        taskAssociated.release();
        assertTrue(taskAssociated.awaitDone());
        }

    @Test
    public void shouldBypassTaskLimitForPriorityTasks()
            throws Exception
        {
        m_pool.stop();
        m_pool = createPool(1);
        m_pool.start();

        AtomicInteger active = new AtomicInteger();
        AtomicInteger max    = new AtomicInteger();

        BlockingTask        taskRegular   = new BlockingTask(null, active, max);
        PriorityBlockingTask taskFirst    = new PriorityBlockingTask(1, active, max, PriorityTask.SCHEDULE_FIRST);
        PriorityBlockingTask taskImmediate = new PriorityBlockingTask(2, active, max, PriorityTask.SCHEDULE_IMMEDIATE);

        m_pool.add(taskRegular);
        assertTrue(taskRegular.awaitStarted());

        m_pool.add(taskFirst);
        m_pool.add(taskImmediate);

        assertTrue(taskFirst.awaitStarted());
        assertTrue(taskImmediate.awaitStarted());
        assertThat(VirtualThreads.isVirtual(taskFirst.getThread()), is(false));
        assertThat(VirtualThreads.isVirtual(taskImmediate.getThread()), is(false));
        assertThat(taskFirst.getThread().getPriority(), is(Thread.NORM_PRIORITY + 1));
        assertThat(taskImmediate.getThread().getPriority(), is(Thread.MAX_PRIORITY));

        taskFirst.release();
        taskImmediate.release();
        assertTrue(taskFirst.awaitDone());
        assertTrue(taskImmediate.awaitDone());

        taskRegular.release();
        assertTrue(taskRegular.awaitDone());
        }

    @Test
    public void shouldParseCooperativeNotifierFlushPolicyAliases()
        {
        TestVirtualDaemonPool pool = createControlledPool(0);

        int nIdle = pool.parseCooperativeNotifierFlushPolicyForTest("idle");
        assertThat(pool.parseCooperativeNotifierFlushPolicyForTest(null), is(nIdle));
        assertThat(pool.parseCooperativeNotifierFlushPolicyForTest(""), is(nIdle));
        assertThat(pool.parseCooperativeNotifierFlushPolicyForTest(" default "), is(nIdle));
        assertThat(pool.parseCooperativeNotifierFlushPolicyForTest("unknown"), is(nIdle));

        int nTask = pool.parseCooperativeNotifierFlushPolicyForTest("task");
        assertThat(pool.parseCooperativeNotifierFlushPolicyForTest("eager"), is(nTask));
        assertThat(nTask == nIdle, is(false));

        assertThat(pool.parseCooperativeNotifierFlushPolicyForTest("none"), is(nIdle));
        assertThat(pool.parseCooperativeNotifierFlushPolicyForTest("off"), is(nIdle));

        int nNone = pool.parseBenchmarkCooperativeNotifierFlushPolicyForTest("none");
        assertThat(pool.parseBenchmarkCooperativeNotifierFlushPolicyForTest("off"), is(nNone));
        assertThat(nNone == nIdle, is(false));
        }

    @Test
    public void shouldKeepNoFlushPolicyBenchmarkOnly()
        {
        // This test mutates global configuration properties and must remain
        // serial within a single test JVM.
        m_pool.stop();

        String sFlushPolicy          = System.getProperty(FLUSH_POLICY_PROPERTY);
        String sBenchmarkFlushPolicy = System.getProperty(BENCHMARK_FLUSH_POLICY_PROPERTY);

        try
            {
            System.setProperty(FLUSH_POLICY_PROPERTY, "none");
            System.clearProperty(BENCHMARK_FLUSH_POLICY_PROPERTY);

            TestVirtualDaemonPool pool = createControlledPool(0);
            m_pool = pool;
            m_pool.start();

            assertThat(pool.getCooperativeNotifierFlushPolicyForTest(),
                    is(pool.parseCooperativeNotifierFlushPolicyForTest("idle")));

            m_pool.stop();

            System.clearProperty(FLUSH_POLICY_PROPERTY);
            System.setProperty(BENCHMARK_FLUSH_POLICY_PROPERTY, "none");

            pool = createControlledPool(0);
            m_pool = pool;
            m_pool.start();

            assertThat(pool.getCooperativeNotifierFlushPolicyForTest(),
                    is(pool.parseBenchmarkCooperativeNotifierFlushPolicyForTest("none")));

            m_pool.stop();

            System.setProperty(FLUSH_POLICY_PROPERTY, "task");
            System.setProperty(BENCHMARK_FLUSH_POLICY_PROPERTY, "none");

            pool = createControlledPool(0);
            m_pool = pool;
            m_pool.start();

            assertThat(pool.getCooperativeNotifierFlushPolicyForTest(),
                    is(pool.parseBenchmarkCooperativeNotifierFlushPolicyForTest("none")));
            }
        finally
            {
            restoreProperty(FLUSH_POLICY_PROPERTY, sFlushPolicy);
            restoreProperty(BENCHMARK_FLUSH_POLICY_PROPERTY, sBenchmarkFlushPolicy);
            }
        }

    @Test
    public void shouldRecoverGuardedDirectTask()
            throws Exception
        {
        m_pool.stop();
        RecordingGuardian    guardian = new RecordingGuardian();
        TestVirtualDaemonPool pool    = createControlledPool(0, guardian);
        m_pool = pool;
        m_pool.start();

        AtomicInteger active    = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        BlockingTask  task      = new BlockingTask(null, active, maxActive);

        m_pool.add(task);
        assertTrue(task.awaitStarted());

        Guardable guardable = guardian.awaitLatestGuardable(1);
        assertTrue(pool.hasTrackedExecution(guardable));

        guardable.recover();

        assertTrue(task.awaitDone());
        assertTrue(task.wasInterrupted());
        assertEventually(() -> pool.getTrackedExecutionCount() == 0);
        assertFalse(pool.hasTrackedExecution(guardable));
        }

    @Test
    public void shouldRecoverGuardedMailboxTask()
            throws Exception
        {
        m_pool.stop();
        RecordingGuardian    guardian = new RecordingGuardian();
        TestVirtualDaemonPool pool    = createControlledPool(0, guardian);
        m_pool = pool;
        m_pool.start();

        AtomicInteger active    = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        BlockingTask  task      = new BlockingTask(1, active, maxActive);

        m_pool.add(task);
        assertTrue(task.awaitStarted());

        Guardable guardable = guardian.awaitLatestGuardable(1);
        assertTrue(pool.hasTrackedExecution(guardable));

        guardable.recover();

        assertTrue(task.awaitDone());
        assertTrue(task.wasInterrupted());
        assertEventually(() -> pool.getTrackedExecutionCount() == 0);
        }

    @Test
    public void shouldRecoverGuardedPriorityTask()
            throws Exception
        {
        m_pool.stop();
        RecordingGuardian    guardian = new RecordingGuardian();
        TestVirtualDaemonPool pool    = createControlledPool(0, guardian);
        m_pool = pool;
        m_pool.start();

        AtomicInteger       active = new AtomicInteger();
        AtomicInteger       max    = new AtomicInteger();
        PriorityBlockingTask task  = new PriorityBlockingTask(1, active, max, PriorityTask.SCHEDULE_IMMEDIATE);

        m_pool.add(task);
        assertTrue(task.awaitStarted());

        Guardable guardable = guardian.awaitLatestGuardable(1);
        assertTrue(pool.hasTrackedExecution(guardable));

        guardable.recover();

        assertTrue(task.awaitDone());
        assertTrue(task.wasInterrupted());
        assertThat(VirtualThreads.isVirtual(task.getThread()), is(false));
        assertEventually(() -> pool.getTrackedExecutionCount() == 0);
        }

    @Test
    public void shouldContinueMailboxAfterRecoveringGuardedTask()
            throws Exception
        {
        m_pool.stop();
        RecordingGuardian    guardian = new RecordingGuardian();
        TestVirtualDaemonPool pool    = createControlledPool(0, guardian);
        m_pool = pool;
        m_pool.start();

        AtomicInteger active    = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        BlockingTask  taskFirst = new BlockingTask(1, active, maxActive);
        BlockingTask  taskSecond = new BlockingTask(1, active, maxActive);

        m_pool.add(taskFirst);
        assertTrue(taskFirst.awaitStarted());

        m_pool.add(taskSecond);
        assertFalse(taskSecond.awaitStarted(200));

        Guardable guardable = guardian.awaitLatestGuardable(1);
        guardable.recover();

        assertTrue(taskFirst.awaitDone());
        assertTrue(taskFirst.wasInterrupted());
        assertTrue(taskSecond.awaitStarted());

        taskSecond.release();
        assertTrue(taskSecond.awaitDone());
        assertEventually(() -> pool.getTrackedExecutionCount() == 0);
        }

    @Test
    public void shouldCleanUpTrackedExecutionOnStopAndRecoverRace()
            throws Exception
        {
        m_pool.stop();
        RecordingGuardian    guardian = new RecordingGuardian();
        TestVirtualDaemonPool pool    = createControlledPool(0, guardian);
        m_pool = pool;
        m_pool.start();

        AtomicInteger active    = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        BlockingTask  task      = new BlockingTask(null, active, maxActive);

        m_pool.add(task);
        assertTrue(task.awaitStarted());

        Guardable guardable = guardian.awaitLatestGuardable(1);

        CompletableFuture<Void> futureRecover = CompletableFuture.runAsync(guardable::recover);
        pool.stop();
        futureRecover.get(5, TimeUnit.SECONDS);

        assertTrue(task.awaitDone());
        assertTrue(task.wasInterrupted());
        assertFalse(pool.isStarted());
        assertEventually(() -> pool.getTrackedExecutionCount() == 0);
        }

    @Test
    public void shouldRouteGetRequestsThroughKeyedMailbox()
            throws Exception
        {
        AtomicInteger      active     = new AtomicInteger();
        AtomicInteger      maxActive  = new AtomicInteger();
        BlockingGetRequest taskFirst  = new BlockingGetRequest(17L, "alpha", active, maxActive);
        BlockingGetRequest taskSecond = new BlockingGetRequest(17L, "alpha", active, maxActive);

        m_pool.add(taskFirst);
        assertTrue(taskFirst.awaitStarted());

        m_pool.add(taskSecond);
        assertFalse(taskSecond.awaitStarted(200));

        taskFirst.release();
        assertTrue(taskFirst.awaitDone());
        assertTrue(taskSecond.awaitStarted());
        assertThat(VirtualThreads.isVirtual(taskFirst.getThread()), is(true));
        assertThat(VirtualThreads.isVirtual(taskSecond.getThread()), is(true));

        taskSecond.release();
        assertTrue(taskSecond.awaitDone());
        assertThat(maxActive.get(), is(1));
        }

    @Test
    public void shouldKeepAssociatedReadOnlyRequestsOnKeyedMailbox()
            throws Exception
        {
        AtomicInteger          active     = new AtomicInteger();
        AtomicInteger          maxActive  = new AtomicInteger();
        ReadOnlyAssociatedTask taskFirst  = new ReadOnlyAssociatedTask(1, active, maxActive);
        ReadOnlyAssociatedTask taskSecond = new ReadOnlyAssociatedTask(1, active, maxActive);

        m_pool.add(taskFirst);
        assertTrue(taskFirst.awaitStarted());

        m_pool.add(taskSecond);
        assertFalse(taskSecond.awaitStarted(200));

        taskFirst.release();
        assertTrue(taskFirst.awaitDone());
        assertTrue(taskSecond.awaitStarted());

        taskSecond.release();
        assertTrue(taskSecond.awaitDone());
        assertThat(maxActive.get(), is(1));
        }

    @Test
    public void shouldStripRequestSuffixFromTaskTypeLabel()
        {
        assertThat(TestVirtualDaemonPool.taskTypeOfForTest(GetRequest.class), is("Get"));
        }

    @Test
    public void shouldNotStripNonRequestSuffixes()
        {
        assertThat(TestVirtualDaemonPool.taskTypeOfForTest(FooMessage.class), is("FooMessage"));
        assertThat(TestVirtualDaemonPool.taskTypeOfForTest(FooEvent.class), is("FooEvent"));
        assertThat(TestVirtualDaemonPool.taskTypeOfForTest(FooTask.class), is("FooTask"));
        }

    @Test
    public void shouldReturnEmptyLabelForAnonymousClass()
        {
        Runnable task = new Runnable()
            {
            @Override
            public void run()
                {
                }
            };

        assertThat(TestVirtualDaemonPool.taskTypeOfForTest(task.getClass()), is(""));
        }

    @Test
    public void shouldNotIncludeAssociationKeyInDrainerThreadName()
            throws Exception
        {
        Object                  oAssoc = new VerboseAssociation();
        AtomicReference<String> refName = new AtomicReference<>();
        GetRequest              task    = new GetRequest(oAssoc, refName);

        m_pool.add(task);
        assertTrue(task.awaitDone());

        String sName = refName.get();
        assertTrue("unexpected thread name: " + sName, sName.length() < 200);
        assertFalse(sName.contains("\n"));
        assertThreadNameMatches(sName, "^.*:M:[0-9A-F]{8}:Get:[0-9A-F]{8}$");
        }

    @Test
    public void shouldUseExpectedRoleLabels()
            throws Exception
        {
        AtomicReference<String> refDirect  = new AtomicReference<>();
        AtomicReference<String> refAll     = new AtomicReference<>();
        AtomicReference<String> refMailbox = new AtomicReference<>();

        GetRequest taskDirect  = new GetRequest(null, refDirect);
        GetRequest taskAll     = new GetRequest(AssociationPile.ASSOCIATION_ALL, refAll);
        GetRequest taskMailbox = new GetRequest("key", refMailbox);

        m_pool.add(taskDirect);
        m_pool.add(taskAll);
        m_pool.add(taskMailbox);

        assertTrue(taskDirect.awaitDone());
        assertTrue(taskAll.awaitDone());
        assertTrue(taskMailbox.awaitDone());

        assertThreadNameMatches(refDirect.get(), "^.*:D:Get:[0-9A-F]{8}$");
        assertThreadNameMatches(refAll.get(), "^.*:A:Get:[0-9A-F]{8}$");
        assertThreadNameMatches(refMailbox.get(), "^.*:M:[0-9A-F]{8}:Get:[0-9A-F]{8}$");
        }

    @Test
    public void shouldRestoreIdleMailboxNameAfterDrain()
            throws Exception
        {
        m_pool.stop();
        TestVirtualDaemonPool pool = createControlledPool(0);
        m_pool = pool;
        m_pool.start();

        pool.armDrainReconciliationBlock();

        GetRequest task = new GetRequest("key", new AtomicReference<>());
        m_pool.add(task);

        assertTrue(task.awaitDone());
        assertTrue(pool.awaitDrainReconciliationEntered());

        assertThreadNameMatches(pool.getDrainReconciliationThreadName(), "^.*:M:[0-9A-F]{8}$");

        pool.releaseDrainReconciliation();
        assertEventually(() -> pool.getMailboxCount() == 0);
        }

    @Test
    public void shouldUseStableMailboxIdentifierAcrossDrainerCycles()
            throws Exception
        {
        m_pool.stop();
        TestVirtualDaemonPool pool = createControlledPool(0);
        m_pool = pool;
        m_pool.start();

        pool.armDrainReconciliationBlock();

        AtomicReference<String> refFirst  = new AtomicReference<>();
        AtomicReference<String> refSecond = new AtomicReference<>();
        GetRequest              taskFirst  = new GetRequest("key", refFirst);
        GetRequest              taskSecond = new GetRequest("key", refSecond);

        m_pool.add(taskFirst);
        assertTrue(taskFirst.awaitDone());
        assertTrue(pool.awaitDrainReconciliationEntered());

        m_pool.add(taskSecond);
        assertTrue(taskSecond.awaitDone());

        assertThreadNameMatches(refFirst.get(), "^.*:M:[0-9A-F]{8}:Get:[0-9A-F]{8}$");
        assertThreadNameMatches(refSecond.get(), "^.*:M:[0-9A-F]{8}:Get:[0-9A-F]{8}$");
        assertThat(extractMailboxHex(refSecond.get()), is(extractMailboxHex(refFirst.get())));

        pool.releaseDrainReconciliation();
        assertEventually(() -> pool.getMailboxCount() == 0);
        }

    @Test
    public void shouldExecuteScheduledTasks()
            throws Exception
        {
        AtomicInteger active    = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        BlockingTask  task      = new BlockingTask(null, active, maxActive);

        m_pool.schedule(task, 50L);

        assertTrue(task.awaitStarted());
        task.release();
        assertTrue(task.awaitDone());
        }

    @Test
    public void shouldCancelScheduledTasksOnStop()
            throws Exception
        {
        AtomicInteger active    = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        BlockingTask  task      = new BlockingTask(null, active, maxActive);

        m_pool.schedule(task, 500L);
        m_pool.stop();

        assertFalse(task.awaitStarted(300));
        }

    @Test
    public void shouldStopWithTasksWaitingForPermit()
            throws Exception
        {
        m_pool.stop();
        m_pool = createPool(1);
        m_pool.start();

        AtomicInteger active     = new AtomicInteger();
        AtomicInteger maxActive  = new AtomicInteger();
        BlockingTask  taskFirst  = new BlockingTask(null, active, maxActive);
        BlockingTask  taskSecond = new BlockingTask(null, active, maxActive);

        m_pool.add(taskFirst);
        assertTrue(taskFirst.awaitStarted());

        m_pool.add(taskSecond);
        assertFalse(taskSecond.awaitStarted(200));

        m_pool.stop();

        assertTrue(taskFirst.awaitDone());
        assertFalse(taskSecond.awaitStarted(200));
        assertFalse(m_pool.isStarted());
        assertEventually(() -> m_pool.getActiveDaemonCount() == 0);
        assertEventually(() -> m_pool.getBacklog() == 0);
        }

    @Test
    public void shouldStopWithAssociatedTasksQueued()
            throws Exception
        {
        AtomicInteger active     = new AtomicInteger();
        AtomicInteger maxActive  = new AtomicInteger();
        BlockingTask  taskFirst  = new BlockingTask(1, active, maxActive);
        BlockingTask  taskSecond = new BlockingTask(1, active, maxActive);

        m_pool.add(taskFirst);
        assertTrue(taskFirst.awaitStarted());

        m_pool.add(taskSecond);
        assertFalse(taskSecond.awaitStarted(200));
        assertEventually(() -> m_pool.getBacklog() == 1);

        m_pool.stop();

        assertTrue(taskFirst.awaitDone());
        assertFalse(taskSecond.awaitStarted(200));
        assertFalse(m_pool.isStarted());
        assertEventually(() -> m_pool.getActiveDaemonCount() == 0);
        assertEventually(() -> m_pool.getBacklog() == 0);
        }

    @Test
    public void shouldNotSplitSameKeyWorkAcrossMailboxesDuringDrainReconciliation()
            throws Exception
        {
        m_pool.stop();
        TestVirtualDaemonPool pool = createControlledPool(0);
        m_pool = pool;
        m_pool.start();

        AtomicInteger active    = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        SignalingTask taskFirst = new SignalingTask(1);
        BlockingTask  taskSecond = new BlockingTask(1, active, maxActive);
        BlockingTask  taskThird  = new BlockingTask(1, active, maxActive);

        pool.armDrainReconciliationBlock();

        m_pool.add(taskFirst);
        assertTrue(taskFirst.awaitDone());
        assertTrue(pool.awaitDrainReconciliationEntered());

        m_pool.add(taskSecond);
        assertTrue(taskSecond.awaitStarted());

        pool.releaseDrainReconciliation();

        m_pool.add(taskThird);
        assertFalse(taskThird.awaitStarted(200));

        taskSecond.release();
        assertTrue(taskSecond.awaitDone());
        assertTrue(taskThird.awaitStarted());

        taskThird.release();
        assertTrue(taskThird.awaitDone());
        assertThat(maxActive.get(), is(1));
        }

    private static Service.VirtualDaemonPool createPool(int cTaskLimit)
        {
        return createPool(cTaskLimit, null);
        }

    private static Service.VirtualDaemonPool createPool(int cTaskLimit, Guardian guardian)
        {
        TestServiceDependencies deps = new TestServiceDependencies();
        deps.setThreadPriority(Thread.NORM_PRIORITY);
        deps.setDefaultWorkerThreadCountMin(1);
        deps.setDaemonPoolType(DaemonPoolType.VIRTUAL);
        deps.setTaskLimit(cTaskLimit);

        TestService service = new TestService("VirtualDaemonPoolComponentTest-" + cTaskLimit, guardian);
        service.setDependencies(deps);

        Service.VirtualDaemonPool pool = (Service.VirtualDaemonPool) service.getDaemonPool();
        pool.setInternalGuardian(service);

        return pool;
        }

    private static Service.DaemonPool createPlatformPool(int cMin, int cMax)
        {
        TestServiceDependencies deps = new TestServiceDependencies();
        deps.setThreadPriority(Thread.NORM_PRIORITY);
        deps.setWorkerThreadCountMin(cMin);
        deps.setWorkerThreadCountMax(cMax);
        deps.setWorkerThreadPriority(Thread.NORM_PRIORITY);

        TestService service = new TestService("PlatformDaemonPoolComponentTest");
        service.setDependencies(deps);

        Service.DaemonPool pool = (Service.DaemonPool) service.getDaemonPool();
        pool.setInternalGuardian(service);

        return pool;
        }

    private static TestVirtualDaemonPool createControlledPool(int cTaskLimit)
        {
        return createControlledPool(cTaskLimit, null);
        }

    private static TestVirtualDaemonPool createControlledPool(int cTaskLimit, Guardian guardian)
        {
        TestServiceDependencies deps = new TestServiceDependencies();
        deps.setThreadPriority(Thread.NORM_PRIORITY);
        deps.setDefaultWorkerThreadCountMin(1);
        deps.setDaemonPoolType(DaemonPoolType.VIRTUAL);
        deps.setTaskLimit(cTaskLimit);

        ControlledPoolService service = new ControlledPoolService("VirtualDaemonPoolControlledTest-" + cTaskLimit, guardian);
        service.setDependencies(deps);

        TestVirtualDaemonPool pool = service.getTestPool();
        pool.setInternalGuardian(service);

        return pool;
        }

    private static void assertEventually(BooleanSupplier supplier)
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

    private static void restoreProperty(String sName, String sValue)
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

    private static void assertThreadNameMatches(String sName, String sRegex)
        {
        assertTrue("unexpected thread name: " + sName, sName != null && sName.matches(sRegex));
        }

    private static String extractMailboxHex(String sName)
        {
        return sName.replaceFirst("^.*:M:([0-9A-F]{8})(?::.*)?$", "$1");
        }

    private static BlockingTask[] createBlockingTasks(int cTasks, Object oAssociation,
            AtomicInteger active, AtomicInteger maxActive)
        {
        BlockingTask[] tasks = new BlockingTask[cTasks];
        for (int i = 0; i < cTasks; i++)
            {
            tasks[i] = new BlockingTask(oAssociation, active, maxActive);
            }
        return tasks;
        }

    private void addAll(BlockingTask[] tasks)
        {
        for (BlockingTask task : tasks)
            {
            m_pool.add(task);
            }
        }

    private static void releaseAll(BlockingTask[] tasks)
        {
        for (BlockingTask task : tasks)
            {
            task.release();
            }
        }

    private static void awaitAllDone(BlockingTask[] tasks)
            throws InterruptedException
        {
        for (BlockingTask task : tasks)
            {
            assertTrue(task.awaitDone());
            }
        }

    private static int releaseStartedExceptFirst(BlockingTask[] tasks, int cKeep)
            throws InterruptedException
        {
        int cStarted  = 0;
        int cReleased = 0;
        for (BlockingTask task : tasks)
            {
            if (task.awaitStarted(1))
                {
                if (cStarted++ >= cKeep)
                    {
                    task.release();
                    ++cReleased;
                    }
                }
            }
        return cReleased;
        }

    private static void assertNoInterrupted(BlockingTask[] tasks)
        {
        for (BlockingTask task : tasks)
            {
            assertFalse(task.wasInterrupted());
            }
        }

    private static final class GetRequest
            implements Runnable, KeyAssociation
        {
        private GetRequest(Object oAssociation, AtomicReference<String> refName)
            {
            f_oAssociation = oAssociation;
            f_refName      = refName;
            }

        @Override
        public Object getAssociatedKey()
            {
            return f_oAssociation;
            }

        @Override
        public void run()
            {
            f_refName.set(Thread.currentThread().getName());
            f_done.countDown();
            }

        public boolean awaitDone()
                throws InterruptedException
            {
            return f_done.await(5, TimeUnit.SECONDS);
            }

        private final Object                  f_oAssociation;
        private final AtomicReference<String> f_refName;
        private final CountDownLatch          f_done = new CountDownLatch(1);
        }

    private static final class FooMessage
            implements Runnable
        {
        @Override
        public void run()
            {
            }
        }

    private static final class FooEvent
            implements Runnable
        {
        @Override
        public void run()
            {
            }
        }

    private static final class FooTask
            implements Runnable
        {
        @Override
        public void run()
            {
            }
        }

    private static final class VerboseAssociation
        {
        @Override
        public String toString()
            {
            return "line\n".repeat(800);
            }
        }

    public static class TestServiceModel
            extends ServiceModel
        {
        public TestServiceModel(String sName, DaemonPool pool)
            {
            super("TestServiceModel", null, true);
            f_pool = pool;
            set_ServiceName(sName);
            }

        public boolean hasTaskLimitNoEffectTrace(String sText)
            {
            return f_listTaskLimitTrace.stream().anyMatch(s -> s.contains(sText));
            }

        @Override
        protected DaemonPool get_DaemonPool()
            {
            return f_pool;
            }

        @Override
        protected void traceTaskLimitSetterNoEffect(int cTaskLimit, DaemonPool pool)
            {
            f_listTaskLimitTrace.add("setTaskLimit(" + cTaskLimit + ')');
            super.traceTaskLimitSetterNoEffect(cTaskLimit, pool);
            }

        private final DaemonPool f_pool;
        private final CopyOnWriteArrayList<String> f_listTaskLimitTrace = new CopyOnWriteArrayList<>();
        }

    public static class TestService
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

        private final Guardian m_guardian;
        }

    public static class ControlledPoolService
            extends TestService
        {
        public ControlledPoolService(String sName)
            {
            this(sName, null);
            }

        public ControlledPoolService(String sName, Guardian guardian)
            {
            super(sName, guardian);
            }

        public TestVirtualDaemonPool getTestPool()
            {
            return m_pool;
            }

        @Override
        protected com.tangosol.coherence.component.util.DaemonPool instantiateDaemonPool(boolean fVirtual)
            {
            if (!fVirtual)
                {
                return super.instantiateDaemonPool(false);
                }

            m_pool = new TestVirtualDaemonPool("VirtualDaemonPool", this, true);
            return m_pool;
            }

        private TestVirtualDaemonPool m_pool;
        }

    public static class TestVirtualDaemonPool
            extends Service.VirtualDaemonPool
        {
        public TestVirtualDaemonPool(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, fInit);
            }

        public void armDrainReconciliationBlock()
            {
            m_fBlockDrainReconciliation   = true;
            m_latchDrainReconciliation    = new CountDownLatch(1);
            m_latchReleaseReconciliation  = new CountDownLatch(1);
            }

        public boolean awaitDrainReconciliationEntered()
                throws InterruptedException
            {
            return m_latchDrainReconciliation.await(5, TimeUnit.SECONDS);
            }

        public void releaseDrainReconciliation()
            {
            m_latchReleaseReconciliation.countDown();
            }

        public int getTrackedExecutionCount()
            {
            java.util.concurrent.ConcurrentHashMap map = getActiveExecutions();
            return map == null ? 0 : map.size();
            }

        public boolean hasTrackedExecution(Object oGuardable)
            {
            java.util.concurrent.ConcurrentHashMap map = getActiveExecutions();
            return map != null && map.containsKey(oGuardable);
            }

        public int getAvailableTaskPermits()
            {
            java.util.concurrent.Semaphore permits = getTaskPermits();
            return permits == null ? Integer.MAX_VALUE : permits.availablePermits();
            }

        public int getMailboxCount()
            {
            java.util.concurrent.ConcurrentHashMap map = getKeyedMailboxes();
            return map == null ? 0 : map.size();
            }

        public int getQueuedBacklogCountForTest()
            {
            java.util.concurrent.atomic.AtomicInteger counter = getBacklogCount();
            return counter == null ? 0 : counter.get();
            }

        public int getCooperativeNotifierFlushPolicyForTest()
            {
            return getCooperativeNotifierFlushPolicy();
            }

        public int parseCooperativeNotifierFlushPolicyForTest(String sPolicy)
            {
            return parseCooperativeNotifierFlushPolicy(sPolicy);
            }

        public int parseBenchmarkCooperativeNotifierFlushPolicyForTest(String sPolicy)
            {
            return parseCooperativeNotifierFlushPolicy(sPolicy, true);
            }

        public static String taskTypeOfForTest(Class<?> clz)
            {
            return taskTypeOf(clz);
            }

        public String getDrainReconciliationThreadName()
            {
            return m_sDrainReconciliationThreadName;
            }

        public boolean hasNoEffectTrace(String sText)
            {
            return ensureNoEffectTraces().stream().anyMatch(s -> s.contains(sText));
            }

        @Override
        protected void traceThreadCountSetterNoEffect(String sSetter, int cDaemons)
            {
            if (is_Constructed())
                {
                ensureNoEffectTraces().add(sSetter + '(' + cDaemons + ')');
                }
            super.traceThreadCountSetterNoEffect(sSetter, cDaemons);
            }

        private CopyOnWriteArrayList<String> ensureNoEffectTraces()
            {
            if (m_listNoEffectTrace == null)
                {
                m_listNoEffectTrace = new CopyOnWriteArrayList<>();
                }
            return m_listNoEffectTrace;
            }

        @Override
        protected void reconcileMailboxAfterDrain(Object oAssoc, KeyedMailbox mailbox)
            {
            m_sDrainReconciliationThreadName = Thread.currentThread().getName();

            if (m_fBlockDrainReconciliation)
                {
                m_latchDrainReconciliation.countDown();

                try
                    {
                    m_latchReleaseReconciliation.await(5, TimeUnit.SECONDS);
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    return;
                    }
                finally
                    {
                    m_fBlockDrainReconciliation = false;
                    }
                }

            super.reconcileMailboxAfterDrain(oAssoc, mailbox);
            }

        private volatile boolean m_fBlockDrainReconciliation;
        private CountDownLatch    m_latchDrainReconciliation   = new CountDownLatch(0);
        private CountDownLatch    m_latchReleaseReconciliation = new CountDownLatch(0);
        private CopyOnWriteArrayList<String> m_listNoEffectTrace;
        private volatile String   m_sDrainReconciliationThreadName;
        }

    public static class RecordingGuardian
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

    public static class RecordingGuardContext
            implements Guardian.GuardContext
        {
        public RecordingGuardContext(Guardian guardian, Guardable guardable, long cTimeoutMillis)
            {
            f_guardian      = guardian;
            f_guardable     = guardable;
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
            m_fReleased = true;
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

        public boolean isReleased()
            {
            return m_fReleased;
            }

        private final long     f_cTimeoutMillis;
        private final Guardable f_guardable;
        private final Guardian  f_guardian;

        private volatile boolean m_fReleased;
        private volatile int     m_nState = STATE_HEALTHY;
        }

    public static class BlockingTask
            implements Runnable, KeyAssociation
        {
        public BlockingTask(Object oAssociation, AtomicInteger active, AtomicInteger maxActive)
            {
            f_oAssociation = oAssociation;
            f_active       = active;
            f_maxActive    = maxActive;
            }

        @Override
        public Object getAssociatedKey()
            {
            return f_oAssociation;
            }

        public Thread getThread()
            {
            return m_thread;
            }

        @Override
        public void run()
            {
            m_thread = Thread.currentThread();

            int cActive = f_active.incrementAndGet();
            f_maxActive.accumulateAndGet(cActive, Math::max);
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
                f_active.decrementAndGet();
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
            return awaitStarted(5_000);
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

        private final Object        f_oAssociation;
        private final AtomicInteger f_active;
        private final AtomicInteger f_maxActive;
        private final CountDownLatch f_started = new CountDownLatch(1);
        private final CountDownLatch f_release = new CountDownLatch(1);
        private final CountDownLatch f_done    = new CountDownLatch(1);

        private volatile boolean m_fInterrupted;
        private volatile Thread m_thread;
        }

    public static class PriorityBlockingTask
            extends BlockingTask
            implements PriorityTask
        {
        public PriorityBlockingTask(Object oAssociation, AtomicInteger active, AtomicInteger maxActive,
                int iSchedulingPriority)
            {
            super(oAssociation, active, maxActive);
            f_iSchedulingPriority = iSchedulingPriority;
            }

        @Override
        public long getExecutionTimeoutMillis()
            {
            return TIMEOUT_DEFAULT;
            }

        @Override
        public long getRequestTimeoutMillis()
            {
            return TIMEOUT_DEFAULT;
            }

        @Override
        public int getSchedulingPriority()
            {
            return f_iSchedulingPriority;
            }

        @Override
        public void runCanceled(boolean fAbandoned)
            {
            }

        private final int f_iSchedulingPriority;
        }

    public static class ReadOnlyAssociatedTask
            extends RequestMessage
            implements Runnable, KeyAssociation
        {
        public ReadOnlyAssociatedTask(Object oAssociation, AtomicInteger active, AtomicInteger maxActive)
            {
            f_oAssociation = oAssociation;
            f_active       = active;
            f_maxActive    = maxActive;
            }

        @Override
        public Object getAssociatedKey()
            {
            return f_oAssociation;
            }

        @Override
        public boolean isReadOnly()
            {
            return true;
            }

        @Override
        public void run()
            {
            int cActive = f_active.incrementAndGet();
            f_maxActive.accumulateAndGet(cActive, Math::max);
            f_started.countDown();

            try
                {
                f_release.await(5, TimeUnit.SECONDS);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                }
            finally
                {
                f_active.decrementAndGet();
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
            return awaitStarted(5_000);
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

        private final Object         f_oAssociation;
        private final AtomicInteger  f_active;
        private final AtomicInteger  f_maxActive;
        private final CountDownLatch f_started = new CountDownLatch(1);
        private final CountDownLatch f_release = new CountDownLatch(1);
        private final CountDownLatch f_done    = new CountDownLatch(1);
        }

    public static class BlockingGetRequest
            extends PartitionedCache.GetRequest
        {
        public BlockingGetRequest(long lCacheId, Object oKey, AtomicInteger active, AtomicInteger maxActive)
            {
            setCacheId(lCacheId);
            setKey(ExternalizableHelper.toBinary(oKey));
            f_active    = active;
            f_maxActive = maxActive;
            }

        public Thread getThread()
            {
            return m_thread;
            }

        @Override
        public String toString()
            {
            return "BlockingGetRequest(cacheId=" + getCacheId() + ", key=" + getKey() + ')';
            }

        @Override
        public void run()
            {
            m_thread = Thread.currentThread();

            int cActive = f_active.incrementAndGet();
            f_maxActive.accumulateAndGet(cActive, Math::max);
            f_started.countDown();

            try
                {
                f_release.await(5, TimeUnit.SECONDS);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                }
            finally
                {
                f_active.decrementAndGet();
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
            return awaitStarted(5_000);
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

        private final AtomicInteger  f_active;
        private final AtomicInteger  f_maxActive;
        private final CountDownLatch f_started = new CountDownLatch(1);
        private final CountDownLatch f_release = new CountDownLatch(1);
        private final CountDownLatch f_done    = new CountDownLatch(1);

        private volatile Thread m_thread;
        }

    public static class SignalingTask
            implements Runnable, KeyAssociation
        {
        public SignalingTask(Object oAssociation)
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
            f_done.countDown();
            }

        public boolean awaitDone()
                throws InterruptedException
            {
            return f_done.await(5, TimeUnit.SECONDS);
            }

        private final Object         f_oAssociation;
        private final CountDownLatch f_done = new CountDownLatch(1);
        }

    public static class OrderedTask
            implements Runnable, KeyAssociation
        {
        public OrderedTask(Object oAssociation, int nExpected, AtomicInteger next,
                AtomicInteger errors, CountDownLatch done)
            {
            f_oAssociation = oAssociation;
            f_nExpected    = nExpected;
            f_next         = next;
            f_errors       = errors;
            f_done         = done;
            }

        @Override
        public Object getAssociatedKey()
            {
            return f_oAssociation;
            }

        @Override
        public void run()
            {
            if (f_next.getAndIncrement() != f_nExpected)
                {
                f_errors.incrementAndGet();
                }
            f_done.countDown();
            }

        private final Object         f_oAssociation;
        private final int            f_nExpected;
        private final AtomicInteger  f_next;
        private final AtomicInteger  f_errors;
        private final CountDownLatch f_done;
        }

    public static class ThrowingTask
            implements Runnable, KeyAssociation
        {
        public ThrowingTask(Object oAssociation)
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
            try
                {
                throw new RuntimeException("intentional test exception");
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

        private final Object         f_oAssociation;
        private final CountDownLatch f_done = new CountDownLatch(1);
        }

    public static class TestServiceDependencies
            extends DefaultServiceDependencies
        {
        @Override
        public void setDefaultWorkerThreadCountMin(int cThreads)
            {
            super.setDefaultWorkerThreadCountMin(cThreads);
            }
        }

    private Service.VirtualDaemonPool m_pool;

    private static final String FLUSH_POLICY_PROPERTY =
            "coherence.daemonpool.virtual.flushPolicy";

    private static final String BENCHMARK_FLUSH_POLICY_PROPERTY =
            "coherence.daemonpool.virtual.benchmark.flushPolicy";
    }
