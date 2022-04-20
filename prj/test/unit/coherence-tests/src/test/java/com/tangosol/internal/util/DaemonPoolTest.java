/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.net.GuardSupport;
import com.tangosol.net.Guardable;
import com.tangosol.net.Guardian;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.common.base.Blocking;
import com.tangosol.net.Guardian.GuardContext;

import com.tangosol.net.cache.KeyAssociation;

import com.tangosol.util.Base;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mockito.InOrder;

import org.mockito.invocation.InvocationOnMock;

import org.mockito.stubbing.Answer;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

/**
 * Unit test of the DaemonPool implementation.
 *
 * @author jh  2014.06.26
 */
public class DaemonPoolTest
        extends Base
    {

    // ----- test lifecycle -------------------------------------------------

    @Before
    public void before()
        {
        final int THREAD_COUNT = 2;

        Guardian                 guardian      = mock(Guardian.class);
        final List<GuardContext> listCtx       = new ArrayList<>(THREAD_COUNT);
        final List<Guardable>    listGuardable = new ArrayList<>(THREAD_COUNT);

        doAnswer(new Answer()
            {
            public Object answer(InvocationOnMock invocation)
                {
                GuardContext ctx       = mock(Guardian.GuardContext.class);
                Guardable    guardable = (Guardable) invocation.getArguments()[0];
                guardable.setContext(ctx);

                when(ctx.getGuardable()).thenReturn(guardable);

                listCtx.add(ctx);
                listGuardable.add(guardable);

                return ctx;
                }
            }).when(guardian).guard(any(Guardable.class));

        DefaultDaemonPoolDependencies deps = new DefaultDaemonPoolDependencies();
        deps.setGuardian(guardian);
        deps.setName(s_sPoolName);
        deps.setThreadCount(THREAD_COUNT);
        deps.setThreadGroup(null);
        deps.setThreadPriority(Thread.MIN_PRIORITY);

        DaemonPool pool = Daemons.newDaemonPool(deps);
        assertFalse(pool.isRunning());
        assertEquals(guardian, pool.getDependencies().getGuardian());
        assertEquals(s_sPoolName, pool.getDependencies().getName());
        assertEquals(THREAD_COUNT, pool.getDependencies().getThreadCount());
        assertNull(pool.getDependencies().getThreadGroup());
        assertEquals(Thread.MIN_PRIORITY, pool.getDependencies().getThreadPriority());
        pool.start();
        assertTrue(pool.isRunning());
        assertFalse(pool.isStuck());

        assertEquals(THREAD_COUNT, listCtx.size());
        assertEquals(THREAD_COUNT, listGuardable.size());

        m_listCtx        = listCtx;
        m_listGuardable  = listGuardable;
        m_pool           = pool;
        }

    @After
    public void after()
        {
        DaemonPool pool = m_pool;
        m_pool = null;
        pool.stop();
        assertFalse(pool.isRunning());
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void testDaemonPoolAdd()
        {
        DaemonPool pool = m_pool;

        TestRunnable task = new TestRunnable();
        pool.add(task);

        for (int i = 0; !task.isComplete() && i < 10; ++i)
            {
            sleep(250L);
            }
        assertTrue(task.isComplete());
        }

    @Test
    public void testDaemonPoolSchedule()
        {
        DaemonPool pool = m_pool;

        TestRunnable task = new TestRunnable();
        pool.schedule(task, 1000L);

        sleep(500L);
        assertFalse(task.isComplete());
        for (int i = 0; !task.isComplete() && i < 10; ++i)
            {
            sleep(250L);
            }
        assertTrue(task.isComplete());
        }

    @Test
    public void testGuardian()
        {
        DaemonPool   pool    = m_pool;
        GuardContext ctx     = m_listCtx.get(0);
        InOrder      inOrder = inOrder(ctx);

        // test successful task execution
        TestRunnable task = new TestRunnable(0, 5L);
        pool.add(task);
        task.waitForCompletion();
        sleep(1000L);

        inOrder.verify(ctx, atLeast(2)).heartbeat();
        }

    @Test
    public void testGuardianRecovery()
        {
        DaemonPool pool      = m_pool;
        Guardable  guardable = m_listGuardable.get(0);

        // test successful task execution after recovery
        TestRunnable task = new TestRunnable(0, 4000L);
        pool.add(task);

        Eventually.assertThat(invoking(task).getGuardable(), is(notNullValue()));
        guardable = task.getGuardable();
        sleep(1000L);

        guardable.recover();
        task.waitForCompletion();

        assertTrue(task.wasInterrupted());
        }

    @Test
    public void testGuardianTermination()
        {
        DaemonPool pool      = m_pool;
        Guardable  guardable = m_listGuardable.get(0);

        // test unsuccessful task execution after termination
        TestRunnable task = new TestRunnable(0, 4000L);
        pool.add(task);

        Eventually.assertThat(invoking(task).getGuardable(), is(notNullValue()));
        guardable = task.getGuardable();

        sleep(1000L);

        guardable.terminate();
        task.waitForCompletion();

        assertFalse(task.wasInterrupted());

        // test successful task execution
        task = new TestRunnable(0, 5L);
        pool.add(task);
        task.waitForCompletion();

        assertFalse(task.wasInterrupted());
        }

    // ----- application entry point ----------------------------------------

    public static void main(String[] asArgs)
            throws IOException, InterruptedException
        {
        int nId = 0;

        DaemonPoolTest test = new DaemonPoolTest();
        test.before();

        DaemonPool pool = test.m_pool;

        List<TestRunnable> listTasks = new ArrayList<>();
        try
            {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("> ");
            for (String s = reader.readLine(); s != null; s = reader.readLine())
                {
                String sCmd = "list";
                int    nArg = -1;

                StringTokenizer st = new StringTokenizer(s);
                if (st.hasMoreTokens())
                    {
                    sCmd = st.nextToken();
                    }
                if (st.hasMoreTokens())
                    {
                    try
                        {
                        nArg = Integer.valueOf(st.nextToken());
                        }
                    catch (NumberFormatException e)
                        {
                        // ignore
                        }
                    }

                switch (sCmd)
                    {
                    case "start":
                        {
                        TestRunnable task = new TestRunnable(nId++, nArg, pool);
                        listTasks.add(task);
                        pool.add(task);
                        }
                        break;

                    case "stop":
                        {
                        if (listTasks.size() > 0)
                            {
                            TestRunnable task = listTasks.remove(0);
                            task.stop();
                            task.waitForCompletion();
                            }
                        }
                        break;

                    case "bye":
                        return;

                    case "list":
                    default:
                        {
                        System.out.println(listTasks.size() + " tasks running:");
                        for (TestRunnable task : listTasks)
                            {
                            System.out.println(task);
                            }
                        }
                    }
                System.out.print("> ");
                }
            }
        finally
            {
            for (TestRunnable task : listTasks)
                {
                task.stop();
                }
            for (TestRunnable task : listTasks)
                {
                task.waitForCompletion();
                }
            listTasks.clear();

            test.after();
            }
        }

    // ----- inner class: TestRunnable --------------------------------------

    public static class TestRunnable
            implements Runnable, KeyAssociation
        {
        public TestRunnable()
            {
            this(0);
            }

        public TestRunnable(int nId)
            {
            this(nId, 10);
            }

        public TestRunnable(int nId, long cMillis)
            {
            this(nId, cMillis, null);
            }

        public TestRunnable(int nId, long cMillis, DaemonPool pool)
            {
            if (cMillis <= 0)
                {
                throw new IllegalArgumentException();
                }

            f_nId     = nId;
            f_cMillis = cMillis;
            f_pool    = pool;
            }

        @Override
        public synchronized void run()
            {
            assertTrue(Thread.currentThread().getName().startsWith(s_sPoolName));

            m_guardable = GuardSupport.getThreadContext().getGuardable();

            long cMillis = f_cMillis;
            try
                {
                while (cMillis > 0 && !m_fStop)
                    {
                    long ldtStart = System.currentTimeMillis();
                    Blocking.wait(this, cMillis);
                    cMillis = Math.max(0, cMillis - (System.currentTimeMillis() - ldtStart));
                    }
                }
            catch (InterruptedException e)
                {
                m_fInterrupted = m_fFinished = true;
                Thread.currentThread().interrupt();
                }
            finally
                {
                if (m_fFinished |= (m_fStop || f_pool == null))
                    {
                    notifyAll();
                    }
                else
                    {
                    f_pool.add(this);
                    }
                }
            }

        @Override
        public Object getAssociatedKey()
            {
            return Integer.valueOf(f_nId);
            }

        public Guardable getGuardable()
            {
            return m_guardable;
            }

        public synchronized void stop()
            {
            m_fStop = true;
            }

        public synchronized boolean isComplete()
            {
            return m_fFinished;
            }

        public synchronized void waitForCompletion()
            {
            while (!m_fFinished)
                {
                try
                    {
                    Blocking.wait(this);
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    throw ensureRuntimeException(e);
                    }
                }
            }

        public synchronized boolean wasInterrupted()
            {
            return m_fInterrupted;
            }

        public String toString()
            {
            return "TestRunnable(" + f_nId + ", " + f_cMillis + " ms.)";
            }

        private final int        f_nId;
        private final long       f_cMillis;
        private final DaemonPool f_pool;

        private boolean   m_fStop;
        private boolean   m_fInterrupted;
        private boolean   m_fFinished;
        private volatile  Guardable m_guardable;
        }

    // ----- data members ---------------------------------------------------

    private List<GuardContext>  m_listCtx;
    private List<Guardable>     m_listGuardable;
    private DaemonPool          m_pool;

    private static final String s_sPoolName = "MyPool";
    }
