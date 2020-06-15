/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.psr;


import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;

import java.io.IOException;
import java.io.PrintStream;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;


/**
* Class that encapsulates test result data.
*
* @author jh  2007.02.15
*/
public class TestResult
        extends Base
        implements PortableObject, Cloneable, Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public TestResult()
        {
        super();
        }

    /**
    * Create a new TestResult with the specified data.
    *
    * @param cMillis      the test duration in milliseconds
    * @param cSuccess     the number of successful operations performed by
    *                     the test
    * @param cFailure     the number of failed operations performed by the
    *                     test
    * @param cb           the number of bytes transfered by the test
    * @param histLatency  the histogram of latency samples
    */
    public TestResult(long cMillis, long cSuccess, long cFailure, long cb,
            Histogram histLatency)
        {
        setDuration    (cMillis);
        setSuccessCount(cSuccess);
        setFailureCount(cFailure);
        setByteCount   (cb);
        setLatency     (histLatency);
        }


    // ----- PortableObject -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        setDuration    (in.readLong(0));
        setSuccessCount(in.readLong(1));
        setFailureCount(in.readLong(2));
        setByteCount   (in.readLong(3));
        setLatency     ((Histogram) in.readObject(4));
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeLong  (0, getDuration());
        out.writeLong  (1, getSuccessCount());
        out.writeLong  (2, getFailureCount());
        out.writeLong  (3, getByteCount());
        out.writeObject(4, getLatency());
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return "TestResult("   +
               "Start="        + m_ldtStart        +
               ", End="        + m_ldtStop         +
               ", Duration="   + getDuration()     + "ms" +
               ", Successes="  + getSuccessCount() +
               ", Failures="   + getFailureCount() +
               ", Bytes="      + getByteCount()    +
               ", Rate="       + getRate()         + "ops" +
               ", Throughput=" + toBandwidthString(getThroughput(), false) +
               ", Latency="    + getLatency()      + ")";
        }


    // ----- helper methods -------------------------------------------------

    /**
    * Start timing the test.
    */
    public void start()
        {
        if (m_ldtStart == 0L)
            {
            m_ldtStart = System.currentTimeMillis();
            }
        }

    /**
    * Stop timing the test.
    */
    public void stop()
        {
        if (m_ldtStart == 0L)
            {
            return;
            }
        if (m_ldtStop == 0L)
            {
            m_cMillis = (m_ldtStop = System.currentTimeMillis()) - m_ldtStart;
            }
        }

    /**
    * Incorporate the data of the given TestResult into this TestResult.
    *
    * @param result  the TestResult to incorporate
    */
    public void add(TestResult result)
        {
        if (result == null)
            {
            return;
            }

        incSuccessCount(result.getSuccessCount());
        incFailureCount(result.getFailureCount());
        incByteCount   (result.getByteCount());
        getLatency().addSamples(result.getLatency());
        }

    /**
    * Encorporate the data of the given collection of TestResult objects into
    * this TestResult.
    *
    * @param colResult  the collection of TestResult objects to encorporate
    */
    public void add(Collection colResult)
        {
        if (colResult == null)
            {
            return;
            }

        for (Iterator iter = colResult.iterator(); iter.hasNext(); )
            {
            add((TestResult) iter.next());
            }
        }

    /**
    * Return a new TestResult that represents the delta between this
    * TestResult and the specified TestResult (i.e. this - that).
    *
    * @param resultThat  the TestResult to subtract from this TestResult
    *
    * @return the delta TestResult
    */
    public TestResult computeDelta(TestResult resultThat)
        {
        TestResult resultDiff;
        try
            {
            resultDiff = (TestResult) clone();
            if (resultThat == null)
                {
                resultDiff.setLatency((Histogram) getLatency().clone());
                }
            else
                {
                resultDiff.setSuccessCount(getSuccessCount() - resultThat.getSuccessCount());
                resultDiff.setFailureCount(getFailureCount() - resultThat.getFailureCount());
                resultDiff.setByteCount   (getByteCount()    - resultThat.getByteCount());
                resultDiff.setLatency     (getLatency().computeDelta(resultThat.getLatency()));
                }
            }
        catch (CloneNotSupportedException e)
            {
            throw new RuntimeException(e);
            }

        return resultDiff;
        }

    /**
    * Write results in spreadsheet loadable format.
    *
    * @param out the stream to write the report to
    */
    public void writeReport(PrintStream out)
        {
        out.print("Duration (ms):");
        out.print('\t');
        out.print("Successful Operations:");
        out.print('\t');
        out.print("Failed Operations:");
        out.print('\t');
        out.print("Byte Count:");
        out.print('\t');
        out.print("Rate (ops):");
        out.print('\t');
        out.print("Throughput (Bps):");
        out.print('\t');
        out.print("Latency:");
        out.println();

        out.print(getDuration());
        out.print('\t');
        out.print(getSuccessCount());
        out.print('\t');
        out.print(getFailureCount());
        out.print('\t');
        out.print(getByteCount());
        out.print('\t');
        out.print(getRate());
        out.print('\t');
        out.print(getThroughput());
        out.print('\t');
        out.print(getLatency());

        out.println();
        out.println();
        out.println("Latency Report:");
        getLatency().writeReport(out, true);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the test duration in milliseconds.
    *
    * @return the number of milliseconds that the test took
    */
    public long getDuration()
        {
        return m_cMillis;
        }

    /**
    * Set the test duration in milliseconds.
    *
    * @param cMillis  the number of milliseconds that the test took
    */
    public void setDuration(long cMillis)
        {
        assert cMillis >= 0L;
        m_cMillis = cMillis;
        }

    /**
    * Increment the test duration by the specified number of milliseconds.
    *
    * @param cMillis  the number of milliseconds to increment the duration by
    */
    public void incDuration(long cMillis)
        {
        assert cMillis >= 0L;
        m_cMillis += cMillis;
        }

    /**
    * Return the total number of operations (successful and unsuccessful)
    * performed by the test.
    *
    * @return the total number of operations
    */
    public long getOperationCount()
        {
        return m_cSuccess + m_cFailure;
        }

    /**
    * Return the number of failed operations performed by the test.
    *
    * @return the number of failed operations
    */
    public long getFailureCount()
        {
        return m_cFailure;
        }

    /**
    * Set the number of failed operations performed by the test.
    *
    * @param cFailure  the number of failed operations
    */
    public void setFailureCount(long cFailure)
        {
        assert cFailure >= 0L;
        m_cFailure = cFailure;
        }

    /**
    * Increment the number of failed operations performed by the test by
    * the specified amount.
    *
    * @param cFailure  the number of operations to increment the total by
    */
    public void incFailureCount(long cFailure)
        {
        assert cFailure >= 0L;
        m_cFailure += cFailure;
        }

    /**
    * Return the number of successful operations performed by the test.
    *
    * @return the number of successful operations
    */
    public long getSuccessCount()
        {
        return m_cSuccess;
        }

    /**
    * Set the number of successful operations performed by the test.
    *
    * @param cSuccess  the number of successful operations
    */
    public void setSuccessCount(long cSuccess)
        {
        assert cSuccess >= 0L;
        m_cSuccess = cSuccess;
        }

    /**
    * Increment the number of failed operations performed by the test by
    * the specified amount.
    *
    * @param cSuccess  the number of operations to increment the total by
    */
    public void incSuccessCount(long cSuccess)
        {
        assert cSuccess >= 0L;
        m_cSuccess += cSuccess;
        }

    /**
    * Return the total number of bytes transfered by the test.
    *
    * @return the total number of bytes transfered
    */
    public long getByteCount()
        {
        return m_cb;
        }

    /**
    * Set the total number of bytes transfered by the test.
    *
    * @param cb  the total number of bytes transfered
    */
    public void setByteCount(long cb)
        {
        assert cb >= 0L;
        m_cb = cb;
        }

    /**
    * Increment the total number of bytes transfered by the test by the
    * specified amount.
    *
    * @param cb  the number of bytes to increase the total by
    */
    public void incByteCount(long cb)
        {
        assert cb >= 0L;
        m_cb += cb;
        }

    /**
    * Return the histogram of latency samples gathered during the test.
    *
    * @return the latency histogram
    */
    public Histogram getLatency()
        {
        Histogram histLatency = m_histLatency;
        if (histLatency == null)
            {
            setLatency(histLatency = new ScaledHistogram(50, "ms"));
            }
        return histLatency;
        }

    /**
    * Configure the histogram of latency samples.
    *
    * @param histLatency  the latency histogram
    */
    public void setLatency(Histogram histLatency)
        {
        assert histLatency != null;
        m_histLatency = histLatency;
        }

    /**
    * Calculate the throughput of the test in bytes per second.
    *
    * @return the calculated throughput
    */
    public long getThroughput()
        {
        long cMillis = getDuration();
        if (cMillis == 0L)
            {
            return 0L;
            }

        long cb = getByteCount();
        if (cb > Long.MAX_VALUE / 1000L) // avoid overflow
            {
            return (cb / cMillis) * 1000L;
            }
        else
            {
            return (cb * 1000L) / cMillis;
            }
        }

    /**
    * Calcluate the rate of the test in operations per second.
    *
    * @return the calculated rate
    */
    public long getRate()
        {
        long cMillis = getDuration();
        if (cMillis == 0L)
            {
            return 0L;
            }

        long cOps = getOperationCount();
        if (cOps > Long.MAX_VALUE / 1000L) // avoid overflow
            {
            return (cOps / cMillis) * 1000L;
            }
        else
            {
            return (cOps * 1000L) / cMillis;
            }
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected Object clone()
            throws CloneNotSupportedException
        {
        return super.clone();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The total test time in milliseconds.
    */
    private long m_cMillis;

    /**
    * The number of successful operations.
    */
    private long m_cSuccess;

    /**
    * The number of failed operations.
    */
    private long m_cFailure;

    /**
    * The total number of bytes transfered.
    */
    private long m_cb;

    /**
    * The histogram of latency samples.
    */
    private transient Histogram m_histLatency;

    /**
    * The start time of the test.
    */
    private transient long m_ldtStart;

    /**
    * The stop time of the test.
    */
    private transient long m_ldtStop;
    }
