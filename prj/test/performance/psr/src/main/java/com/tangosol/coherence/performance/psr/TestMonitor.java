/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.psr;


import com.tangosol.util.Base;


/**
* Utility class that allows one or more threads to wait for a specified
* number of TestResult objects to be posted by one or more threads.
*
* @author jh  2007.02.15
*/
public class TestMonitor
        extends Base
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public TestMonitor()
        {
        this(0);
        }

    /**
    * Create a new TestMonitor for the specified number of TestResult objects.
    *
    * @param cResult  the total number of TestResult objects expected to be
    *                 posted by one or more threads
    */
    public TestMonitor(int cResult)
        {
        assert cResult >= 0;

        m_cResult = cResult;
        m_result  = new TestResult();
        }


    // ----- monitor methods ------------------------------------------------

    /**
    * Notify all waiting threads that the specified TestResult has been posted.
    *
    * @param result  the newly posted result
    */
    public synchronized void notify(TestResult result)
        {
        assert result != null;

        getResult().add(result);
        if (++m_cResultCurrent == getResultCount())
            {
            getResult().stop();
            }
        notifyAll();
        }

    /**
    * Wait for the specified number of TestResult objects to be posted.
    *
    * @param cResult  the number of TestResult objects
    * @param cMillis  the total amount of time (in milliseconds) to wait;
    *                 0 to wait indefinitely
    *
    * @return true iff the specified number of TestResults were posted before
    *         the specified timeout
    *
    * @throws InterruptedException if the calling thread is interrupted
    *         while waiting
    */
    public synchronized boolean wait(int cResult, long cMillis)
            throws InterruptedException
        {
        assert cResult >= 0;
        assert cMillis >= 0L;

        if (cMillis == 0L)
            {
            while (m_cResultCurrent < cResult)
                {
                wait();
                }
            }
        else
            {
            while (m_cResultCurrent < cResult && cMillis > 0L)
                {
                long ldtStart = getSafeTimeMillis();
                wait(cMillis);
                cMillis -= (getSafeTimeMillis() - ldtStart);
                }
            }

        return m_cResultCurrent == cResult;
        }

    /**
    * Wait for all TestResult objects to be posted.
    *
    * @param cMillis  the total amount of time (in milliseconds) to wait;
    *                 0 to wait indefinitely
    *
    * @return true iff the all TestResults were posted before the specified
    *         timeout
    *
    * @throws InterruptedException if the calling thread is interrupted
    *         while waiting
    */
    public synchronized boolean waitAll(long cMillis)
            throws InterruptedException
        {
        return wait(getResultCount(), cMillis);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the total number of TestResult objects that are expected.
    *
    * @return the total number of expected TestResult objects
    */
    public synchronized int getResultCount()
        {
        return m_cResult;
        }

    /**
    * Set the total number of TestResult objects that are expected.
    *
    * @param cResult  the total number of expected TestResult objects
    */
    public synchronized void setResultCount(int cResult)
        {
        assert cResult >= 0;
        m_cResult = cResult;

        getResult().start();
        notifyAll();
        }

    /**
    * Return the aggregate of the TestResult objects that have been posted.
    *
    * @return the aggregate TestResult
    */
    public TestResult getResult()
        {
        return m_result;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The aggregate TestResult.
    */
    private final TestResult m_result;

    /**
    * The total number of expected TestResult objects.
    */
    private int m_cResult;

    /**
    * The number of TestResult objects that have been received.
    */
    private int m_cResultCurrent;
    }
