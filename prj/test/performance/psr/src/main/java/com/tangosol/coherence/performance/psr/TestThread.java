/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.psr;


import com.oracle.coherence.common.base.Logger;

import com.tangosol.util.Base;


/**
* Thread used to execute tests.
*
* @author jh  2007.02.16
*/
public class TestThread
        extends Thread
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new TestThread that will execute the given task the specified
    * number of times.
    *
    * @param task   the test tast to execute
    * @param cIter  the number of times to execute the task
    */
    public TestThread(Task task, int cIter)
        {
        assert task != null;
        assert cIter > 0;

        m_task   = task;
        m_cIter  = cIter;
        m_result = new TestResult();

        setDaemon(true);
        setName("TestThread@" + hashCode());
        }


    // ----- Thread methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void run()
        {
        synchronized (this)
            {
            // @see #start
            m_fStart = true;
            notifyAll();
            }

        // wait until the task is scheduled
        synchronized (this)
            {
            while (!m_fExecute)
                {
                try
                    {
                    // @see #execute
                    wait();
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                    }
                }
            }

        TestResult result = getResult();
        result.start();
        try
            {
            for (int i = 0, c = m_cIter; i < c; ++i)
                {
                m_task.run(result);
                }
            }
        catch (Exception e)
            {
            Logger.err(e);
            }
        finally
            {
            result.stop();
            }

        synchronized (this)
            {
            m_fDone = true;

            // @see #waitForResult
            notifyAll();
            }
        }


    // ----- lifecycle methods ----------------------------------------------

    /**
    * Start the thread.
    * <p/>
    * This method will block until the thread has been successfully started
    * or the calling thread is interrupted.
    */
    public synchronized void start()
        {
        if (isAlive() || m_fExecute)
            {
            return;
            }

        super.start();
        try
            {
            while (!m_fStart)
                {
                // @see #run
                wait();
                }
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
            }
        }

    /**
    * Schedule the test task for immediate execution.
    */
    public synchronized void execute()
        {
        if (!isAlive())
            {
            throw new IllegalStateException("Thread is not running");
            }

        if (!m_fExecute)
            {
            m_fExecute = true;

            // @see #run
            notifyAll();
            }
        }

    /**
    * Wait for the thread to finish executing the test and return the result
    * of the execution.
    *
    * @param cMillis  the total amount of time (in milliseconds) to wait;
    *                 0 to wait indefinitely
    *
    * @return the result of executing the test; null if the execution did not
    *         finish before the specified timeout
    *
    * @throws InterruptedException if the calling thread is interrupted
    *         while waiting
    */
    public synchronized TestResult waitForResult(long cMillis)
            throws InterruptedException
        {
        if (!isAlive())
            {
            // not sure if this is possible, but just in case the thread
            // died before it had a chance to even run the task ...
            }
        else if (cMillis == 0L)
            {
            while (!m_fDone)
                {
                // @see #run
                wait();
                }
            }
        else
            {
            while (!m_fDone && cMillis > 0L)
                {
                long ldtStart = Base.getSafeTimeMillis();
                // @see #run
                wait(cMillis);
                cMillis -= (Base.getSafeTimeMillis() - ldtStart);
                }
            }

        return m_fDone ? getResult() : null;
        }


    // ----- Task inner interface -------------------------------------------

    /**
    * An executable test task.
    */
    public interface Task
        {
        /**
        * Execute the task and update the given results.
        *
        * @param result  the results of execution
        */
        public void run(TestResult result);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the test task.
    *
    * @return the test task
    */
    public Task getTask()
        {
        return m_task;
        }

    /**
    * Return the number of times that this thread will execute the test task.
    *
    * @return the number of times that this thread will execute the test task
    */
    public int getIterationCount()
        {
        return m_cIter;
        }

    /**
    * Return the current result of the test.
    *
    * @return the test result
    */
    public TestResult getResult()
        {
        return m_result;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The test task.
    */
    private final Task m_task;

    /**
    * The number of times to execute the test task.
    */
    private final int m_cIter;

    /**
    * The result of executing the test task.
    */
    private final TestResult m_result;

    /**
    * True iff the thread has been started.
    */
    private boolean m_fStart;

    /**
    * True iff execute() has been called.
    */
    private boolean m_fExecute;

    /**
    * True iff the test has finished.
    */
    private boolean m_fDone;
    }
