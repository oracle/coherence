/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.oracle.coherence.common.base.Blocking;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;


/**
* A Daemon thread handler that asynchronously executes Runnable tasks, either
* at a scheduled time or "as soon as possible".
*
* @author cp  2003.10.09
* @author cp  2006.02.23 (Coherence 3.2) bulletproofing for use in the CQC
*/
public class TaskDaemon
        extends Daemon
        implements Executor
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.  Creates a TaskDaemon using default settings. The
    * daemon will not be automatically started.
    */
    public TaskDaemon()
        {
        super();
        }

    /**
    * Creates a TaskDaemon with the specified name. The daemon will not be
    * automatically started.
    *
    * @param sName  the thread name (may be null)
    */
    public TaskDaemon(String sName)
        {
        super(sName);
        }

    /**
    * Creates a TaskDaemon with a specified name and priority.
    *
    * @param sName      the thread name (may be null)
    * @param nPriority  the thread priority, between Thread.MIN_PRIORITY and
    *                   Thread.MAX_PRIORITY inclusive
    * @param fStart     pass true to start the thread immediately
    */
    public TaskDaemon(String sName, int nPriority, boolean fStart)
        {
        super(sName, nPriority, false);

        if (fStart)
            {
            start();
            }
        }

    /**
    * Creates a TaskDaemon with a specified name and priority.
    *
    * @param sName      the thread name (may be null)
    * @param nPriority  the thread priority, between Thread.MIN_PRIORITY and
    *                   Thread.MAX_PRIORITY inclusive
    * @param fStart     pass true to start the thread immediately
    * @param fFinish    pass true to makes sure ripe tasks are run before the
    *                   daemon shuts down
    * @param cMillisTimeout  the number of milliseconds to wait after the
    *                   previous task finished for a new task to be
    *                   submitted before automatically shutting down the
    *                   daemon thread
    */
    public TaskDaemon(String sName, int nPriority, boolean fStart,
                      boolean fFinish, int cMillisTimeout)
        {
        this(sName, nPriority, false);

        setFinishing(fFinish);
        setIdleTimeout(cMillisTimeout);

        if (fStart)
            {
            start();
            }
        }


    // ----- Runnable interface ---------------------------------------------

    /**
    * The task processing loop.
    */
    public void run()
        {
        try
            {
            // initialize the "last run time" to the current time so that the
            // daemon does not exit prematurely
            updateMostRecentTaskTime();

            while (!isStopping() || isFinishing())
                {
                Runnable task = takeNextRipeTask();
                if (task == null)
                    {
                    if (isStopping())
                        {
                        // no more tasks == all done
                        break;
                        }
                    }
                else
                    {
                    run(task);
                    }
                }
            }
        catch (VirtualMachineError e)
            {
            throw e;
            }
        catch (Throwable e)
            {
            err(e);
            err("(Daemon is exiting.)");
            }
        }


    // ----- Thread-related -------------------------------------------------

    /**
    * Request the daemon to stop, optionally completing tasks that have
    * already been scheduled and are ready to be run.
    *
    * @param fFinish  pass true if the daemon should finish any tasks that
    *                 have already been scheduled before stopping
    */
    public synchronized void stop(boolean fFinish)
        {
        setFinishing(fFinish);
        stop();
        }

    /**
    * Determine if the daemon will finish those scheduled tasks that are
    * ripe (presently due to be run) before stopping.
    *
    * @return true if the daemon is configured to finish any ripe scheduled
    *         tasks before stopping
    */
    public boolean isFinishing()
        {
        return m_fFinish;
        }

    /**
    * Specify whether the daemon will finish scheduled tasks before stopping.
    *
    * @param fFinish  pass true to force the daemon to finish any scheduled
    *                 tasks before stopping
    */
    public synchronized void setFinishing(boolean fFinish)
        {
        m_fFinish = fFinish;
        }

    /**
    * Determine the length of time that the daemon will live without any
    * activity before it stops itself.
    *
    * @return the timeout for the TaskDaemon's thread to live before being
    *         shut down
    */
    public long getIdleTimeout()
        {
        return m_cMillisTimeout;
        }

    /**
    * Configure the daemon's timeout. Note that if the daemon shuts itself
    * down, it will automatically restart when something is added to the
    * queue.
    *
    * @param cMillis  if greater than zero, the number of milliseconds that
    *                 the daemon will wait with nothing in the queue before
    *                 shutting itself down
    */
    public synchronized void setIdleTimeout(long cMillis)
        {
        m_cMillisTimeout = cMillis;
        if (isRunning())
            {
            // since the timeout changed, it could cause the daemon to shut
            // down
            notifyAll();
            }
        }


    // ----- task management ------------------------------------------------

    @Override
    public void execute(Runnable command)
        {
        executeTask(command);
        }

    /**
    * Schedule a task to be run by the daemon "as soon as possible".
    *
    * @param task  a Runnable object to invoke
    */
    public synchronized void executeTask(Runnable task)
        {
        scheduleTask(task, getSafeTimeMillis());
        }

    /**
    * Schedule a task to be run at the specified time, or as soon after
    * as possible.
    *
    * @param task  a Runnable object to invoke
    * @param ldt   a datetime value at which to run the task
    */
    public synchronized void scheduleTask(Runnable task, long ldt)
        {
        boolean fIsDaemon = getThread() == Thread.currentThread();
        if (isStopping() && !fIsDaemon)
            {
            throw new IllegalStateException("Daemon " + this
                + " is stopping; new tasks cannot be scheduled.");
            }

        LongArray arrayTasks = getTasks();
        List      listTasks  = (List) arrayTasks.get(ldt);
        boolean   fNew       = listTasks == null;
        if (fNew)
            {
            listTasks = new LinkedList();
            arrayTasks.set(ldt, listTasks);
            }
        listTasks.add(task);

        if (!isRunning())
            {
            start();
            }
        else if (!fIsDaemon && fNew && ldt == arrayTasks.getFirstIndex())
            {
            // wake up the daemon if we just scheduled the "next item to run"
            notifyAll();
            }
        }

    /**
    * Schedule a periodic task to be run "as soon as possible", and to repeat
    * at the specified interval.
    *
    * @param task             a Runnable object to invoke
    * @param cMillisInterval  the number of milliseconds to wait after the
    *                         task is run before running it again
    */
    public synchronized void executePeriodicTask(Runnable task, long cMillisInterval)
        {
        schedulePeriodicTask(task, getSafeTimeMillis(), cMillisInterval);
        }

    /**
    * Schedule a periodic task to be run at the specified time, and to
    * repeat at the specified interval.
    *
    * @param task             a Runnable object to invoke
    * @param ldtFirst         a datetime value at which to first run the task
    * @param cMillisInterval  the number of milliseconds to wait after the
    *                         task is run before running it again
    */
    public synchronized void schedulePeriodicTask(
            final Runnable task, long ldtFirst, long cMillisInterval)
        {
        scheduleTask(instantiatePeriodicTask(task, cMillisInterval), ldtFirst);
        }


    // ----- internal task management ---------------------------------------

    /**
    * Obtain the pending tasks.
    *
    * @return a LongArray keyed by SafeTimeMillis with a corresponding value
    *         being a List of tasks scheduled at that time
    */
    protected LongArray getTasks()
        {
        return m_arrayTasks;
        }

    /**
    * Wait for the next scheduled task is ripe (due or overdue), then
    * remove it from the pending schedule and return it.
    *
    * @return a task that is ripe to be run, or null if the TaskDaemon is
    *         shutting down and no task should be run
    * @throws InterruptedException if this thread is interrupted while waiting
     *        for the next task
    */
    protected synchronized Runnable takeNextRipeTask()
            throws InterruptedException
        {
        Runnable task = null;

        LongArray arrayTasks = getTasks();
        while (task == null)
            {
            // if the daemon should stop and it isn't configured to finish all
            // scheduled tasks, then there will be no more "ripe tasks" to run
            if (isStopping() && !isFinishing())
                {
                break;
                }

            long ldt = arrayTasks.getFirstIndex();
            if (ldt == -1L)
                {
                // if nothing is scheduled and the daemon is supposed to be
                // stopping, then it has finished
                if (isStopping())
                    {
                    break;
                    }

                // nothing scheduled; wait for something to be scheduled
                long cTimeoutMillis = getIdleTimeout();
                if (cTimeoutMillis > 0)
                    {
                    long ldtPrev = getMostRecentTaskTime();
                    long ldtStop = ldtPrev + cTimeoutMillis;
                    long ldtCur  = getSafeTimeMillis();
                    long cMillisWait = ldtStop - ldtCur;
                    if (cMillisWait > 0)
                        {
                        Blocking.wait(this, cMillisWait);
                        }
                    else
                        {
                        // terminating due to inactivity
                        stop();
                        break;
                        }
                    }
                else
                    {
                    Blocking.wait(this);
                    }
                }
            else
                {
                // something is scheduled; see how long to wait for it
                // to become ripe
                long lWait = ldt - getSafeTimeMillis();
                if (lWait > 0)
                    {
                    if (isStopping())
                        {
                        break;
                        }
                    else
                        {
                        Blocking.wait(this, lWait);
                        }
                    }
                else
                    {
                    // it is already ripe; remove it
                    List listTasks = (List) arrayTasks.get(ldt);
                    task = (Runnable) listTasks.remove(0);
                    if (listTasks.isEmpty())
                        {
                        arrayTasks.remove(ldt);
                        }
                    }
                }
            }

        return task;
        }

    /**
    * Execute a Runnable task.
    *
    * @param task  a Runnable object
    */
    protected void run(Runnable task)
        {
        if (task != null)
            {
            try
                {
                updateMostRecentTaskTime();
                task.run();
                updateMostRecentTaskTime();
                }
            catch (VirtualMachineError e)
                {
                throw e;
                }
            catch (ThreadDeath e)
                {
                throw e;
                }
            catch (Throwable e)
                {
                onException(e, task);
                }
            }
        }

    /**
    * Determine when the most recent task was run.
    *
    * @return the date/time at which the most recent task was run
    */
    protected long getMostRecentTaskTime()
        {
        return m_ldtLastTask;
        }

    /**
    * Set the time that the most recent task was run to the current time.
    */
    protected void updateMostRecentTaskTime()
        {
        m_ldtLastTask = getSafeTimeMillis();
        }


    // ----- inner class: PeriodicTask --------------------------------------

    /**
    * Create a task that will automatically be run on a periodic basis.
    *
    * @param task             the actual task to run
    * @param cMillisInterval  the period of time, in milliseconds, to
    *                         wait between runs of the task
    *
    * @return a task that will run itself periodically
    */
    protected Runnable instantiatePeriodicTask(Runnable task, long cMillisInterval)
        {
        azzert(cMillisInterval > 0, "interval must be greater than zero");

        return new PeriodicTask(task, cMillisInterval);
        }

    /**
    * A PeriodicTask is a task that automatically reschedules itself so that
    * it executes on a periodic basis.
    */
    public class PeriodicTask
            extends Base
            implements Runnable
        {
        /**
        * Construct a task that will automatically be run on a periodic
        * basis.
        *
        * @param task             the actual task to run
        * @param cMillisInterval  the period of time, in milliseconds, to
        *                         wait between runs of the task
        */
        public PeriodicTask(Runnable task, long cMillisInterval)
            {
            m_task            = task;
            m_cMillisInterval = cMillisInterval;
            }

        public void run()
            {
            try
                {
                m_task.run();
                }
            finally
                {
                TaskDaemon daemon = TaskDaemon.this;
                if (!daemon.isStopping())
                    {
                    daemon.scheduleTask(this,
                        getSafeTimeMillis() + m_cMillisInterval);
                    }
                }
            }

        /**
        * The task to run periodically.
        */
        private Runnable m_task;

        /**
        * The interval to wait between runs of the task.
        */
        private long m_cMillisInterval;
        }


    // ----- logging support ------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return "TaskDaemon{" + getDescription() + '}';
        }

    /**
    * {@inheritDoc}
    */
    protected String getDescription()
        {
        return super.getDescription()
            + ", MostRecentTaskTime=" + formatDateTime(getMostRecentTaskTime())
            + ", NextRipeTask=" + formatDateTime(Math.max(getTasks().getFirstIndex(), 0L))
            + ", Timeout=" + getIdleTimeout() + "ms"
            + ", Finishing=" + isFinishing();
        }

    /**
    * Process an exception that is thrown during a task execution.
    * The default implementation logs the exception and continues.
    *
    * @param e     Throwable object (a RuntimeException or an Error)
    * @param task  the task that caused the exception
    */
    protected void onException(Throwable e, Runnable task)
        {
        String sDaemon = String.valueOf(getThread());
        if (task == null)
            {
            err("An exception occurred on " + sDaemon + ":");
            }
        else
            {
            String sTask = "class " + task.getClass().getName();
            try
                {
                sTask = task.toString();
                }
            catch (Throwable eIgnore) {}

            err("An exception occurred on " + sDaemon
                + " while processing the task: " + sTask);
            }
        err(e);
        err("(The thread has logged the exception and is continuing.)");
        }


    // ----- data members ---------------------------------------------------

    /**
    * An ordered map keyed by Long datetime value for when a task is
    * scheduled, whose corresponding value is a List of tasks to run
    * at that scheduled time.
    */
    private LongArray m_arrayTasks = new SparseArray();

    /**
    * True if the daemon should finish any tasks that have already been
    * scheduled before stopping. This does not count tasks that are scheduled
    * for the future.
    */
    private volatile boolean m_fFinish;

    /**
    * The date/time at which the most recent task was executed. This value
    * is only relied on from the daemon thread itself, and thus can be
    * considered to be non-volatile.
    */
    private long m_ldtLastTask;

    /**
    * The timeout for the daemon. This is the number of milliseconds that
    * the daemon will wait with an empty task queue before shutting itself
    * down.
    */
    private long m_cMillisTimeout;
    }
