/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.component.util;

import com.oracle.coherence.testing.junit.ThreadDumpOnTimeoutRule;
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

    // ----- data members ---------------------------------------------------

    /**
     * This rule is to ensure that the shouldNotDeadlockWhenStoppingPoolWhileResizing does not
     * cause the test to hang forever as when it fails it may cause a deadlock.
     */
    @ClassRule
    public static final ThreadDumpOnTimeoutRule timeout = ThreadDumpOnTimeoutRule.after(5, TimeUnit.MINUTES);
    }
