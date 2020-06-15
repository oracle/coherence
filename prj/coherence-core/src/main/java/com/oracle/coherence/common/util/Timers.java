/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.util;

import com.oracle.coherence.common.base.Disposable;
import com.oracle.coherence.common.internal.util.Timer;
import com.oracle.coherence.common.internal.util.TimerTask;

import java.util.concurrent.atomic.AtomicLong;

import java.util.Set;

/**
 * Timer related helpers.
 *
 * @author mf  2014.07.16
 */
public class Timers
    {
    /**
     * Schedule a short non-blocking task for future execution.
     *
     * @param task      the task to run
     * @param cMillis   the delay before execution
     *
     * @return a Disposable object which can be used to cancel the scheduled task
     */
    public static Disposable scheduleNonBlockingTask(final Runnable task, long cMillis)
        {
        return scheduleNonBlockingTask(task, cMillis, null);
        }

    /**
     * Schedule a short non-blocking task for future execution.
     *
     * @param task        the task to run
     * @param cMillis     the delay before execution
     * @param setPending  optional Set to add the returned Disposables to upon scheduling (i.e. now),
     *                    and to delete from upon running/canceling. Note, modifications to the set will
     *                    be performed while synchronized on the set.
     *
     * @return a Disposable object which can be used to cancel the scheduled task
     */
    public static Disposable scheduleNonBlockingTask(final Runnable task, long cMillis, Set<? super Disposable> setPending)
        {
        final AtomicLong cTasks = s_cNonBlockingTimerTasks;

        long cExpected;
        do
            {
            cExpected = cTasks.get();
            if (cExpected == 0)
                {
                // protect transition from 0, we may need to create the timer
                Class clz = Timers.class;
                synchronized (clz)
                    {
                    cExpected = cTasks.get();
                    if (cExpected == 0 && s_timerNonBlocking == null)
                        {
                        Timer timer = s_timerNonBlocking = new Timer("NonBlockingTimer", /*fDaemon*/ true);

                        // schedule a repeating task to shutdown the thread if it becomes idle (COH-6531)
                        timer.scheduleAtFixedRate(new TimerTask()
                            {
                            @Override
                            public void run()
                                {
                                if (cTasks.get() == 0)
                                    {
                                    synchronized (clz) // the capture of clz also ensures that the Timers class can't be GC'd, thus preserving it's static state
                                        {
                                        if (cTasks.get() == 0)
                                            {
                                            s_timerNonBlocking.cancel();
                                            s_timerNonBlocking = null;
                                            }
                                        }
                                    }
                                }
                            }, /*delay*/ 5000, /*interval*/ 5000);
                        }
                    // else; timer was still active; just revive it by setting the count

                    cTasks.incrementAndGet(); // under sync ensures that the above shutdown task won't see 0
                    break;
                    }
                }
            // else we have at least one task try to CAS to increment from non-zero
            }
        while (!cTasks.compareAndSet(cExpected, cExpected + 1));

        // we've ensured our task is in cTasks, thus the Timer cannot be shutdown and s_timer cannot be null
        TimerTask taskWrapper = new PendingTask(task, cTasks, setPending);

        if (setPending != null)
            {
            synchronized (setPending)
                {
                setPending.add(taskWrapper);
                }
            }

        s_timerNonBlocking.schedule(taskWrapper, cMillis);

        return taskWrapper;
        }

    // ----- inner class: PendingTask ---------------------------------------

    /**
     * PendingTask is a TimerTask which allows for "safe" cancellation.
     */
    protected static class PendingTask
        extends TimerTask
        {
        public PendingTask(Runnable task, AtomicLong cTasks, Set<? super TimerTask> set)
            {
            m_task   = task;
            m_cTasks = cTasks;
            m_set    = set;
            }

        public void run()
            {
            m_cTasks.decrementAndGet();
            Set<? super TimerTask> set = m_set;
            if (set != null)
                {
                synchronized (set)
                    {
                    set.remove(this);
                    }
                }

            Runnable task = m_task;
            if (task != null)
                {
                task.run();
                }
            }

        @Override
        public boolean cancel()
            {
            Set<? super TimerTask> set = m_set;
            if (set != null)
                {
                synchronized (set)
                    {
                    set.remove(this);
                    }
                m_set = null;
                }

            m_task = null; // release the task so avoid retaining garbage
            if (super.cancel())
                {
                m_cTasks.decrementAndGet();
                return true;
                }
            return false;
            }

        public String toString()
            {
            return "TimerTask{" + m_task + "}";
            }

        /**
         * The task to run, or null if cancelled.
         */
        protected Runnable m_task;

        /**
         * The number of pending tasks, this is to be decremented once the task is run (even if the task is null).
         */
        protected AtomicLong m_cTasks;

        /**
         * An optional set of pending tasks, this task is to be removed from the set once run or cancelled.
         */
        protected Set<? super TimerTask> m_set;
        }

    // ----- data members ----------------------------------------------------

    /**
     * Shared timer thread.
     */
    private static Timer s_timerNonBlocking;

    /**
     * The number of currently scheduled timer tasks.
     */
    private static final AtomicLong s_cNonBlockingTimerTasks = new AtomicLong();
    }
