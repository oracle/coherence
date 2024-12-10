/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.util;

import java.util.ArrayList;
import java.util.List;

import com.oracle.coherence.common.util.Timers;
import com.oracle.coherence.common.base.Blocking;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit test of the @{link Timers} class.
 *
 * @author jh  2014.07.23
 */
public class TimersTest
    {
    @Test
    public void testScheduleNonBlockingTask()
            throws InterruptedException
        {
        AtomicInteger cTaskOrder = new AtomicInteger();

        List<TestRunnable> listTasks = new ArrayList<>(10);
        for (int i = 0; i < 10; ++i)
            {
            listTasks.add(new TestRunnable(i, cTaskOrder));
            }

        for (TestRunnable task : listTasks)
            {
            Timers.scheduleNonBlockingTask(task, 10L);
            }

        int i = 0;
        for (TestRunnable task : listTasks)
            {
            task.waitForCompletion();
            assertEquals(i++, task.getCompletionOrder());
            }
        }

    // ----- helper classes -------------------------------------------------

    private static class TestRunnable
            implements Runnable
        {
        public TestRunnable(int nOrder, AtomicInteger nCounter)
            {
            f_nOrder   = nOrder;
            f_nCounter = nCounter;
            }

        @Override
        public synchronized void run()
            {
            m_nOrder = f_nCounter.getAndIncrement();
            m_fRun   = true;

            notifyAll();
            }

        public synchronized boolean wasRun()
            {
            return m_fRun;
            }
        public synchronized int getCompletionOrder()
            {
            return m_nOrder;
            }

        public synchronized void waitForCompletion()
                throws InterruptedException
            {
            while (!wasRun())
                {
                Blocking.wait(this);
                }
            }

        public String toString()
            {
            return "TestRunnable{ScheduleOrder=" + f_nOrder + ", CompletionOrder=" + getCompletionOrder() + "}";
            }

        private boolean             m_fRun   = false;
        private int                 m_nOrder = -1;

        private final int f_nOrder;
        private final AtomicInteger f_nCounter;
        }
    }
