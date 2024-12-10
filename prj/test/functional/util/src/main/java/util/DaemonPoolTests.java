/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package util;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.common.base.Associated;

import com.tangosol.coherence.component.util.DaemonPool;
import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;

import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.cache.KeyAssociation;
import com.tangosol.net.InvocationObserver;
import com.tangosol.net.InvocationService;

import com.tangosol.net.Member;
import com.tangosol.util.Base;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.core.Is.is;


/**
 * DaemonPool tests.
 *
 * @author gg 2015.05.18
 */
public class DaemonPoolTests
        extends AbstractFunctionalTest
    {
    @Test
    public void testPool()
        {
        InvocationService service = (InvocationService) CacheFactory.getService("invocation");

        final AtomicReference<Object> atomicResult = new AtomicReference();

        class Client extends Thread
            {
            public Client(int i)
                {
                f_index = i;
                }
            public void run()
                {
                final AtomicLong atomicCounter = new AtomicLong();
                try
                    {
                    InvocationObserver observer = new InvocationObserver()
                        {
                        public void memberCompleted(Member member, Object oResult)
                            {
                            atomicCounter.set((Integer) oResult);
                            }

                        public void memberFailed(Member member, Throwable eFailure)
                            {
                            atomicResult.set(eFailure);
                            }

                        public void memberLeft(Member member)
                            {
                            }

                        public void invocationCompleted()
                            {
                            }
                        };

                    int iCount = 0;
                    while (atomicResult.get() == null)
                        {
                        service.execute(new Incrementor(f_index, iCount++), null, observer);

                        // local flow control
                        if (atomicCounter.get() < iCount + 2000)
                            {
                            Base.sleep(1);
                            }
                        }
                    }
                catch (RuntimeException t)
                    {
                    atomicResult.set(t);
                    }
                }

            final int f_index;
            }

        int ASSOCIATION_COUNT = 8;
        int TEST_DURATION = 60000;

        s_aiCounter = new int[ASSOCIATION_COUNT];
        s_aThread = new Thread[ASSOCIATION_COUNT];

        for (int i = 0; i < ASSOCIATION_COUNT; i++)
            {
            new Client(i).start();
            }

        Grid _service   = (Grid) ((SafeService) service).getRunningService();
        DaemonPool pool = _service.getDaemonPool();

        System.out.println("DaemonPoolTests: testPool will run for " + (TEST_DURATION/1000) + " seconds");

        long ldtStop = Base.getSafeTimeMillis() + TEST_DURATION;
        for (boolean fIncrement = true; atomicResult.get() == null; fIncrement = !fIncrement)
            {
            int cThreadsOld = pool.getDaemonCount();
            int cDelta      = 1 + Base.getRandom().nextInt(4);
            int cThreadsNew = Math.max(1, fIncrement ? cThreadsOld + cDelta : cThreadsOld - cDelta);

            if (cThreadsNew == 10) cThreadsNew = 1;

            pool.setDaemonCount(cThreadsNew);

            Base.sleep(500);
            if (Base.getSafeTimeMillis() > ldtStop)
                {
                atomicResult.set(Boolean.TRUE);
                }
            }
        Object oResult = atomicResult.get();

        if (oResult instanceof RuntimeException)
            {
            throw (RuntimeException) oResult;
            }
        }

    /**
     * Test that a deadlock does not happen in the following scenario:
     * Two daemon threads, D1 and D2
     * 1. D2 processes a job from the queue associated with D1
     * 2. daemon pool resizes to stop D1
     * 3. the job in #1 creates a new thread that adds a new job to the queue
     *    associated with D1.
     *
     * See COH-13974 for details.
     */
    @Test
    public void  testCoh13974()
        {
        System.setProperty("coherence.daemonpool.slots", "2");

        DaemonPool pool = s_pool = new DaemonPool();
        pool.setDaemonCountMin(2);
        pool.setDaemonCountMax(2);
        pool.setDaemonCount(2);
        pool.start();

        pool.add(new LockTask(2, 1, TYPE.JOB_SLEEP));

        LockTask job2 = new LockTask(2, 2, TYPE.JOB_STOP);
        pool.add(job2);

        Eventually.assertThat(invoking(job2).isAddSuccess(), is(true));
        }


    /**
     * Test that a deadlock does not happen in the following scenario:
     * Two daemon threads, D1 and D2,  three workslot S0, S1 and S2
     * 1. D2 processes a job from the queue associated with D1
     * 2. daemon pool resizes to start new daemon D3
     * 3. the job in #1 creates a new thread that adds a new job to the queue
     *    associated with D1.
     */
    @Test
    public void  testCoh14026()
        {
        System.setProperty("coherence.daemonpool.slots", "3");

        DaemonPool pool = s_pool = new DaemonPool();
        pool.setDaemonCountMin(2);
        pool.setDaemonCountMax(2);
        pool.setDaemonCount(2);
        pool.start();

        pool.add(new LockTask(2, 1, TYPE.JOB_SLEEP));

        LockTask job2 = new LockTask(2, 2, TYPE.JOB_START);
        pool.add(job2);

        pool.add(new LockTask(1, 3, TYPE.JOB_NONE)); // wake up D2

        Eventually.assertThat(invoking(job2).isAddSuccess(), is(true));
        }


    // helper classes
    public static class Incrementor
            extends AbstractInvocable
            implements Associated<Integer>
        {
        public Incrementor(int index, int nExpect)
            {
            m_index   = index;
            m_nExpect = nExpect;
            }

        public Integer getAssociatedKey()
            {
            return m_index;
            }

        public void run()
            {
            int nValueOld = s_aiCounter[m_index];

            if (nValueOld != m_nExpect)
                {
                String sMsg = "FAILURE for key " + m_index + ": expected value " +
                    m_nExpect + " but was " + nValueOld + "; last update by " + s_aThread[m_index];
                CacheFactory.log(sMsg, CacheFactory.LOG_ERR);
                throw new RuntimeException(sMsg);
                }

            s_aiCounter[m_index] = nValueOld + 1;
            s_aThread[m_index] = Thread.currentThread();

            setResult(nValueOld);
            }

        public String toString()
            {
            return "Incrementor{Expected=" + m_nExpect + "}";
            }

        protected int m_index;
        protected int m_nExpect;
        }


    public class LockTask
            implements Runnable, KeyAssociation
        {
        public LockTask(int nSlot, int nJob, TYPE action)
            {
            m_nSlot  = nSlot;
            m_nJob   = nJob;
            m_action = action;
            }

        @Override
        public void run()
            {
            switch (m_action)
                {
                case JOB_SLEEP:
                    Base.sleep(1000);
                    break;

                case JOB_START:
                case JOB_STOP:
                    if (m_action == TYPE.JOB_START)
                        {
                        s_pool.setDaemonCount(3);  // resize up
                        }
                    else
                        {
                        s_pool.setDaemonCount(1); // resize down
                        }

                    // wait for startTask/stopTask to be processed
                    Base.sleep(1000);


                    // add a new task to the pool from another thread
                    new Thread()
                        {
                        public void run()
                            {
                            s_pool.add(new LockTask(2, 3, TYPE.JOB_NONE));

                            s_fDone = true;
                            }
                        }.start();

                    Eventually.assertThat(invoking(this).isAddSuccess(), is(true));
                    break;

                case JOB_NONE:
                    break;
                }
            }

        @Override
        public Object getAssociatedKey()
            {
            return m_action == TYPE.JOB_START ? m_nSlot + m_nJob * 3 : m_nSlot * m_nJob;
            }

        /**
         * Return true if the new task is successfully added to the pool.
         *
         * @return true if the task is successfully added to the pool
         */
        public boolean isAddSuccess()
            {
            return s_fDone;
            }

        private int     m_nJob;
        private int     m_nSlot;
        private boolean s_fDone;
        private TYPE    m_action;
        }


    // enum action for LockTask
    static public enum TYPE
        {
        JOB_START,
        JOB_STOP,
        JOB_SLEEP,
        JOB_NONE
        };

    static int[] s_aiCounter;
    static Thread[] s_aThread;

    private DaemonPool s_pool;
    }
