/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.util;


import com.tangosol.util.Base;


/**
* MovingAverage tracks the moving average over a number of samples.
*
* @author mf 2007.03.06
* @since Coherence 3.3
*/
public class MovingAverage
       extends Base
    {
    // ---- constructors ----------------------------------------------------

    /**
    * Construct a MovingAverage for a small data set.
    */
    public MovingAverage()
        {
        this(100, 1);
        }

    /**
    * Construct a MovingAverage for a given number of samples at a given
    * resolution.  The storage space for the MovingAverage is proportional to
    * (samples / resolution).  Thus a larger resolution allows for averaging
    * over a greater number of samples, though with decreased accuracy.
    *
    * @param cSamples     the maximum number of samples to average over
    * @param nResolution  the number of samples to coalesce
    */
    public MovingAverage(int cSamples, int nResolution)
        {
        azzert(nResolution > 0, "resolution must be positive");
        azzert(cSamples >= (nResolution * 2), "samples must be >= 2*resolution");

        m_alBucket    = new long[cSamples / nResolution];
        m_nResolution = nResolution;
        }


    // ---- MovingAverage methods -------------------------------------------

    /**
    * Reset the moving average.
    */
    public synchronized void reset()
        {
        long[] alBucket = m_alBucket;
        for (int i = 0, c = alBucket.length; i < c; ++i)
            {
            alBucket[i] = 0L;
            }

        m_lTotal           = 0L;
        m_iBucket          = 0;
        m_cSamplesFixed    = 0;
        m_cSamplesVariable = 0;
        }

    /**
    * Add a sample to the moving average.
    *
    * @param iValue  the sampled value
    */
    public synchronized void addSample(int iValue)
        {
        long[] alBucket    = m_alBucket;
        int    iBucket     = m_iBucket;
        int    cSamples    = m_cSamplesVariable + 1;
        int    nResolution = m_nResolution;

        if (cSamples > nResolution)
            {
            // bucket is now full
            int cBucketMax = alBucket.length;

            m_iBucket = iBucket = (iBucket + 1) % cBucketMax;
            cSamples  = 1; // first sample in new bucket

            if (m_cSamplesFixed < (cBucketMax - 1) * nResolution)
                {
                // first use of this bucket
                // fixed number of samples grows
                m_cSamplesFixed += nResolution;
                }
            else
                {
                // recycle old bucket
                m_lTotal -= alBucket[iBucket];
                alBucket[iBucket] = 0;
                }
            }

        // add sample to current bucket
        alBucket[iBucket]  += iValue;
        m_lTotal           += iValue;
        m_cSamplesVariable  = cSamples;
        }

    /**
    * Return the moving average.
    *
    * @return the average, or 0 if there are no samples
    */
    public int getAverage()
        {
        int cSamples = getSampleCount();
        // round up the average
        return cSamples == 0 ? 0 :
            (int) ((m_lTotal + (cSamples >>> 1)) / cSamples);
        }

    /**
    * Return the moving average as a double.
    *
    * @return the average, or 0.0 if there are no samples
    */
    public double getDoubleAverage()
        {
        int cSamples = getSampleCount();
        return cSamples == 0 ? 0.0 : m_lTotal / (double) cSamples;
        }

    /**
    * Return an estimate as to the standard deviation for the samples.
    * The accuracy of the estimate decreases for resolutions over 1.
    *
    * @return the standard deviation, or 0.0 if there are no samples
    */
    public double getStandardDeviation()
        {
        long[] alBucket    = m_alBucket;
        int    iBucket     = m_iBucket;
        double dResolution = m_nResolution;
        int    cVar        = m_cSamplesVariable;
        int    cBuckets    = m_cSamplesFixed / (int) (dResolution + (cVar > 0 ? 1 : 0));
        int    cSamples    = getSampleCount();
        double dAverage    = getDoubleAverage();
        double dTotal      = 0.0;

        for (int i = 0; i < cBuckets; ++i)
            {
            double dSamples = i == iBucket ? cVar : dResolution;
            double dDelta   = alBucket[i] / dSamples - dAverage;
            dTotal += dDelta * dDelta * dSamples;
            }
        return cSamples == 0 ? 0.0 : Math.sqrt(dTotal / getSampleCount());
        }

    /**
    * Return the number of samples represented in the moving average.
    *
    * @return the sample count
    */
    public int getSampleCount()
        {
        return m_cSamplesFixed + m_cSamplesVariable;
        }


    // ---- Object interface ------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return "average = " + getDoubleAverage() +
               "; stddev = " + getStandardDeviation() +
               ", over " + getSampleCount() + " samples";
        }


    // ---- test methods ----------------------------------------------------

    /**
    * Unit test of MovingAverage.
    *
    * @param asArg [span] [resolution] [interval] [samples] [max]
    */
    public static void main(String[] asArg)
        {
        int cSpan     = asArg.length > 0 ? Integer.parseInt(asArg[0]) : 1000;
        int nRes      = asArg.length > 1 ? Integer.parseInt(asArg[1]) : 10;
        int nInterval = asArg.length > 2 ? Integer.parseInt(asArg[2]) : 100;
        int cSample   = asArg.length > 3 ? Integer.parseInt(asArg[3]) : 1000;

        MovingAverage avg = new MovingAverage(cSpan, nRes);

        for (int i = 0; i < cSample; ++i)
            {
            avg.addSample(getRandom().nextInt(i + 1));
            if ((i % nInterval) == 0)
                {
                out(avg);
                }
            }
        }

    // ---- data members ----------------------------------------------------

    /**
    * The array of buckets containing samples.
    */
    protected long[] m_alBucket;

    /**
    * The maximum number of samples per bucket.
    */
    protected int m_nResolution;

    /**
     * The index of the current bucket.
     */
    protected int m_iBucket;

    /**
    * The sum of all samples covered by the moving average.
    */
    protected long m_lTotal;

    /**
    * The fixed number of samples in the moving average.
    */
    protected int m_cSamplesFixed;

    /**
    * The number of samples in the current bucket.
    */
    protected int m_cSamplesVariable;
    }
