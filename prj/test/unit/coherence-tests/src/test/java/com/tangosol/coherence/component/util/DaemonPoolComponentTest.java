/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.component.util;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.net.cache.KeyAssociation;

import com.tangosol.util.Base;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Unit test of the DaemonPool component.
 *
 * @author jh  201
 */
public class DaemonPoolComponentTest
        extends DaemonPool
    {
    // ----- test lifecycle -------------------------------------------------

    @Before
    public void before()
        {
        assertFalse(isRunning());
        setDaemonCount(Runtime.getRuntime().availableProcessors() + 1);
        start();
        assertTrue(isRunning());
        }

    @After
    public void after()
        {
        stop();
        assertFalse(isRunning());
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void testResizeWithAssociatedBacklog()
            throws InterruptedException
        {
        for (int i = 1; i <= 4; ++i)
            {
            System.out.println("Test resize of " + i);
            testResizeWithAssociatedBacklog(i);
            }
        }

    protected void testResizeWithAssociatedBacklog(final int cThreadsDelta)
            throws InterruptedException
        {
        final int cWorkSlots = getWorkSlotCount();

        // perform resize on a different thread
        Thread threadResize = new Thread(() -> {
            while (getBacklog() == 0)
                {
                // allow the backlog to build up
                Base.sleep(1);
                }

            // perform the resize
            for (int nSign = 1; getBacklog() > 0; nSign = -nSign)
                {
                int cThreads = Math.max(cWorkSlots + nSign * cThreadsDelta, 1);
                setDaemonCount(cThreads);

                do
                    {
                    Base.sleep(1000);
                    System.out.println("DaemonCount=" + getDaemonCount()
                            + ", TargetDaemonCount="  + cThreads
                            + ", ActiveDaemonCount="  + getActiveDaemonCount()
                            + ", Backlog="            + getBacklog());
                    }
                while (isInTransition());
                }
            }, "ResizingThread");

        threadResize.start();

        AtomicInteger counter = new AtomicInteger(0);
        Random        random  = Base.getRandom();

        // add a bunch of associated work to the pool
        List<TestRunnable> listWork = new ArrayList<>(BACKLOG_SIZE);
        for (int i = 0; i < BACKLOG_SIZE; ++i)
            {
            TestRunnable task = new TestRunnable(0, i, counter);
            add(task);
            listWork.add(task);

            int nIdAssoc = random.nextInt(cWorkSlots);
            for (int j = 1; j < cWorkSlots; j++)
                {
                add(new TestRunnable(nIdAssoc, -1, null));
                }
            }

        // wait for the work to complete and validate the order of execution
        for (TestRunnable task : listWork)
            {
            Eventually.assertThat(invoking(task).wasExecuted(), is(true));

            assertTrue(task.wasExecutedInOrder());
            }

        Eventually.assertThat("Queue is stuck at " + getBacklog(),
                              invoking(this).getBacklog(), is(0));

        try
            {
            threadResize.join(5000);
            }
        catch (Exception e) {}

        assertFalse(threadResize + " didn't stop", threadResize.isAlive());
        }

    // ----- accessors ------------------------------------------------------

    @Override
    public synchronized void setDaemonCount(int cThreads)
        {
        setDaemonCountMin(cThreads);
        setDaemonCountMax(cThreads);
        super.setDaemonCount(cThreads);
        }

    // ----- inner class: TestRunnable --------------------------------------

    public static class TestRunnable
            extends Base
            implements Runnable, KeyAssociation
        {
        public TestRunnable(int nIdAssoc, int nId, AtomicInteger counter)
            {
            f_NIdAssoc = Integer.valueOf(nIdAssoc);
            f_nId      = nId;
            m_counter  = counter;
            }

        @Override
        public Object getAssociatedKey()
            {
            return f_NIdAssoc;
            }

        @Override
        public synchronized void run()
            {
            sleep(SLEEP_MILLIS);
            AtomicInteger counter = m_counter;
            if (counter != null)
                {
                m_fExecutedInOrder = counter.getAndIncrement() == f_nId;
                }
            m_fExecuted = true;
            notifyAll();
            }

        public synchronized boolean wasExecuted()
            {
            return m_fExecuted;
            }

        public synchronized boolean wasExecutedInOrder()
            {
            return m_fExecutedInOrder;
            }

        @Override
        public String toString()
            {
            return "TestRunnable(" + f_NIdAssoc + ", " + f_nId + ")";
            }

        private final Integer f_NIdAssoc;
        private final int f_nId;

        private final AtomicInteger m_counter;

        private boolean m_fExecuted;
        private boolean m_fExecutedInOrder;

        public static final long SLEEP_MILLIS = 10L;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The size of the task backlog to test with.
     */
    public static final int BACKLOG_SIZE = 256;
    }
