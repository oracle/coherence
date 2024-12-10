/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.util;

import com.oracle.coherence.common.base.Converter;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.PrintStream;
import java.time.LocalTime;

/**
 * Class for tracking samples in a Histogram.
 *
 * Note this implementation is not thread-safe.
 *
 * @author mf 2006.07.05
 */
public class Histogram
    implements  Cloneable, Externalizable
    {
    /**
    * Construct a histogram of a given size.
    *
    * @param cLabels the maximum array size for the histogram
    */
    public Histogram(int cLabels)
        {
        m_alResults = new long[cLabels];
        }

    public Object clone()
            throws CloneNotSupportedException
        {
        return super.clone();
        }

    public Histogram setFormatter(Converter<Double, String> formatter)
        {
        m_formatter = formatter;
        return this;
        }

    /**
     * Return the histogram index for a given sample.
     *
     * @param nSample  the sample
     *
     * @return the histogram index for the given sample
     */
    public int getIndex(int nSample)
        {
        return nSample;
        }

    /**
     * Return the minimum value for a label representing a range
     *
     * @param i  the histogram index
     *
     * @return the minimum value for the given index
     */
    public int getLabelMin(int i)
        {
        return i;
        }

    /**
     * Return the maximum value for a label representing a range
     *
     * @param i  the histogram index
     *
     * @return the maximum value for the given index
     */
    public int getLabelMax(int i)
        {
        return getLabelMin(i + 1) - 1;
        }

    /**
     * Return the median value for a given histogram index.
     *
     * @param i  the index
     *
     * @return the median value for the given index
     */
    public int getLabelMedian(int i)
        {
        int nMin = getLabelMin(i);
        int nMax = getLabelMax(i);
        return nMin + (nMax - nMin) / 2;
        }

    /**
     * Return the textual label corresponding to a given histogram index.
     *
     * @param i  the index
     *
     * @return the textual label for the given index
     */
    public String getLabelText(int i)
        {
        Converter<Double, String> formatter = m_formatter;
        if (i < 0)
            {
            return "n/a";
            }
        else if (i == 0)
            {
            // zero just means < 1
            return "<" + formatter.convert((double) getLabelMin(1));
            }
        else if (i == m_alResults.length - 1)
            {
            // result was over the max supported by the histogram
            return ">" + formatter.convert((double) getLabelMin(i));
            }
        int nMin    = getLabelMin(i);
        int nMax    = getLabelMax(i);
        int nMedian = getLabelMedian(i);
        int nRange  = nMax - nMin;

        if (nRange > 1)
             {
             return formatter.convert((double) nMedian) + " +/-" + formatter.convert((double) nRange / 2);
             }

        return formatter.convert((double) nMedian);
        }

    /**
     * Add a sample value to the histogram.
     *
     * @param nSampleValue  the sampled value.
     */
    public void addSample(int nSampleValue)
        {
        long[] alResults = m_alResults;
        // compute index
        int i = getIndex(nSampleValue);
        i = Math.max(0, Math.min(i, alResults.length - 1));

        ++alResults[i];
        }

    /**
     * Add all the samples from the supplied Histogram to this Histogram.
     *
     * @param histThat  the samples to add
     */
    public void addSamples(Histogram histThat)
        {
        long[] alResultThat = histThat.getResults();
        long[] alResult     = m_alResults;
        for (int i = 0, c = alResultThat.length; i < c; ++i)
            {
            alResult[i] += alResultThat[i];
            }
        }

    /**
     * Return a new Histogram which is the result of subtracting the supplied Histogram samples from this Histograms
     * samples
     *
     * @param histThat  the samples to subtract
     *
     * @return the resulting Histogram
     */
    public Histogram compare(Histogram histThat)
        {
        Histogram histDiff;

        try
            {
            histDiff = (Histogram) clone();
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }

        if (histThat == null)
            {
            histDiff.m_alResults = (long[]) getResults().clone();
            }
        else
            {

            long[] alResultThat = histThat.getResults();
            long[] alResult     = m_alResults;
            long[] alResultDiff = histDiff.m_alResults = new long[alResultThat.length];
            for (int i = 0, c = alResultThat.length; i < c; ++i)
                {
                alResultDiff[i] = alResult[i] - alResultThat[i];
                }
            }

        return histDiff;
        }

    public long[] getResults()
        {
        return m_alResults;
        }

    public Snapshot snapshot()
        {
        return new Snapshot(this);
        }

    public long getSampleCount()
        {
        long[]       alResults = m_alResults;
        long         cSamples  = 0;

        for (int i = 0, c = alResults.length; i < c; ++i)
            {
            long cSample = alResults[i];
            if (cSample > 0)
                {
                cSamples += cSample;
                }
            }

        return cSamples;
        }

    public String toString()
        {
        Snapshot                  snapshot  = new Snapshot(this);
        Converter<Double, String> formatter = m_formatter;

        StringBuilder sb = new StringBuilder();
        sb.append("samples ").append(snapshot.getSampleCount());
        sb.append("; avg ").append(formatter.convert(snapshot.getAverage()));
        sb.append("; stddev ").append(formatter.convert(snapshot.getStdDev()));
        sb.append("; min ").append(getLabelText(snapshot.getMin()));

        int[]    anPercentile = snapshot.getPercentiles();
        String[] asPercentage = snapshot.getPercentileNames();
        for (int i = 0, c = anPercentile.length; i < c; ++i)
            {
            if (i == anPercentile.length - 1 || anPercentile[i] != anPercentile[i + 1])
                {
                sb.append("; " + asPercentage[i] + " <" + formatter.convert((double) getLabelMax(anPercentile[i])));
                }
            // else; information is redundant with the next thing to be printed
            }
        sb.append("; max ").append(getLabelText(snapshot.getMax()));

        return sb.toString();
        }

    /**
     * Write histogram in spreadsheet loadable format.
     *
     * @param out  the output PrintStream
     */
    public void writeReport(PrintStream out)
        {
        long[] alResults = m_alResults;
        for (int i = 0, c = alResults.length; i < c; ++i)
            {
            long cSample = alResults[i];
            if (cSample > 0)
                {
                out.println(getLabelText(i) + '\t' + cSample);
                }
            }
        }

    /**
     * Write histogram header in spreadsheet loadable format.
     *
     * @param out  the output PrintStream
     */
    public void writeReportHeaderHz(PrintStream out)
        {
        long[] alResults = m_alResults;
        for (int i = 0, c = alResults.length; i < c; ++i)
            {
            out.print(getLabelText(i) + '\t');
            }
        out.println();
        }


    /**
     * Write horizontal histogram in spreadsheet loadable format.
     *
     * @param out  the output PrintStream
     */
    public void writeReportHz(PrintStream out)
        {
        // all results are written so that we may build a table of
        // multiple histograms and compare them
        long[] alResults = m_alResults;
        int cSkip = 0;
        for (int i = 0, c = alResults.length; i < c; ++i)
            {
            long cSample = alResults[i];
            if (cSample == 0)
                {
                // avoid a huge string of 0s at the end
                ++cSkip;
                }
            else
                {
                for (; cSkip > 0; --cSkip)
                    {
                    out.print("0\t");
                    }
                out.print(alResults[i] + "\t");
                }
            }
        out.println();
        }

    @Override
    public void readExternal(ObjectInput in)
            throws IOException, ClassNotFoundException
        {
        readExternal((DataInput) in);
        }

    public void readExternal(DataInput dataInput)
            throws IOException
        {
        int c = dataInput.readInt();

        m_alResults = c <= 0 ? new long[0] :
                        c < 0x7FFFFFF >> 3
                            ? readHistgram(dataInput, c)
                            : readLargeHistgram(dataInput, c);

        }

    @Override
    public void writeExternal(ObjectOutput out)
            throws IOException
        {
        writeExternal((DataOutput) out);
        }

    public void writeExternal(DataOutput dataOutput)
            throws IOException
        {
        long[] alResults = m_alResults;
        int c = alResults.length;
        dataOutput.writeInt(c);
        int iSkipStart = -1;
        for (int i = 0; i < c; ++i)
            {
            long n = alResults[i];
            if (n == 0)
                {
                if (iSkipStart < 0)
                    {
                    iSkipStart = i;
                    }
                }
            else
                {
                if (iSkipStart >= 0)
                    {
                    dataOutput.writeLong(iSkipStart - i);
                    iSkipStart = -1;
                    }
                dataOutput.writeLong(n);
                }
            }

        if (iSkipStart >= 0)
            {
            dataOutput.writeLong(iSkipStart - c);
            }
        }

    /**
     * Read histgram from a DataInput stream.
     *
     * @param in  a DataInput stream to read from
     *
     * @return an array of longs
     *
     * @throws IOException  if an I/O exception occurs
     */
    private long[] readHistgram(DataInput in, int c)
            throws IOException
        {
        long[] alResults = new long[c];
        for (int i = 0; i < c; )
            {
            long n = in.readLong();
            if (n < 0)
                {
                // skip over gaps
                i += -n;
                continue;
                }
            alResults[i++] = n;
            }

        return alResults;
        }

    /**
     * Read a histogram with large array.
     *
     * @param in       a DataInput stream to read from
     * @param cLength  length to read
     *
     * @return an array of longs
     *
     * @throws IOException  if an I/O exception occurs
     */
    private long[] readLargeHistgram(DataInput in, int cLength)
            throws IOException
        {
        int    cBatchMax = 0x3FFFFFF >> 3;
        int    cBatch    = cLength / cBatchMax + 1;
        long[] aMerged   = null;
        int    cRead     = 0;
        int    cAllocate = cBatchMax;
        long[] al;
        for (int i = 0; i < cBatch && cRead < cLength; i++)
            {
            al      = readHistgram(in, cAllocate);
            aMerged = Base.mergeLongArray(aMerged, al);
            cRead  += al.length;

            cAllocate = Math.min(cLength - cRead, cBatchMax);
            }

        return aMerged;
        }

    // ----- inner class Snapshot -------------------------------------------

    public static class Snapshot
        {
        // ----- constructors -----------------------------------------------

        Snapshot(Histogram histogram)
            {
            m_timestamp = LocalTime.now();
            m_iMin      = -1;
            m_iMax      = 0;
            m_cSamples  = 0;

            long[] alResults = histogram.getResults();
            long   cTotal    = 0;
            double cTotalSq  = 0;

            for (int i = 0, c = alResults.length; i < c; ++i)
                {
                long cSample = alResults[i];
                if (cSample > 0)
                    {
                    if (m_iMin == -1)
                        {
                        m_iMin = i;
                        }
                    m_iMax = i;
                    m_cSamples += cSample;

                    int nMedian = histogram.getLabelMedian(i);

                    cTotal   += cSample * nMedian;
                    cTotalSq += cSample * nMedian * nMedian;
                    }
                }

            // second pass compute percentiles
            m_anPercentile = new int[f_adPercentage.length - 1];

            long cRunningTotal = 0;
            long lLimit        = (long) (f_adPercentage[0] * m_cSamples);
            for (int i = 0, c = alResults.length, p = 0; i < c && p < m_anPercentile.length; ++i)
                {
                cRunningTotal += alResults[i];
                while (cRunningTotal > lLimit && p < m_anPercentile.length)
                    {
                    m_anPercentile[p] = i;
                    lLimit            = (long) (f_adPercentage[++p] * m_cSamples);
                    }
                }

            m_dAvg    = cTotal / (double) m_cSamples;
            m_dStddev = Math.sqrt(
                    ((m_cSamples * (double) cTotalSq) - (cTotal * (double) cTotal))
                   / (m_cSamples * (double) (m_cSamples - 1)));

            m_dAvg    = ((int) (m_dAvg      * 1000)) / 1000D;
            m_dStddev = ((int) (m_dStddev   * 1000)) / 1000D;
            }

        // ----- Snapshot methods -------------------------------------------

        public LocalTime getTimestamp()
            {
            return m_timestamp;
            }

        public double getAverage()
            {
            return m_dAvg;
            }

        public double getStdDev()
            {
            return m_dStddev;
            }

        public int get33Percentile()
            {
            return m_anPercentile[0];
            }

        public int get66Percentile()
            {
            return m_anPercentile[1];
            }

        public int get99Percentile()
            {
            return m_anPercentile[2];
            }

        public int get999Percentile()
            {
            return m_anPercentile[3];
            }

        public int get9999Percentile()
            {
            return m_anPercentile[4];
            }

        public int get99999Percentile()
            {
            return m_anPercentile[5];
            }

        // ----- helper methods ---------------------------------------------

        int[] getPercentiles()
            {
            return m_anPercentile;
            }

        String[] getPercentileNames()
            {
            return f_asPercentage;
            }

        int getMin()
            {
            return m_iMin;
            }

        int getMax()
            {
            return m_iMax;
            }

        long getSampleCount()
            {
            return m_cSamples;
            }

        // ----- data members -----------------------------------------------

        protected long m_cSamples;
        protected int m_iMin;
        protected int m_iMax;
        protected LocalTime m_timestamp;
        protected int[]   m_anPercentile;
        protected double m_dAvg;
        protected double m_dStddev;
        }

    // ----- constants ------------------------------------------------------

    private static final String[] f_asPercentage = new String[]{"33%", "66%", "99%", "99.9%", "99.99%", "99.999%"};
    private static final double[] f_adPercentage = new double[]{.33,   .66,   .99,   .999,    .9999,    .99999, 1};

    // ----- data members ---------------------------------------------------

    protected long[]  m_alResults;

    protected Converter<Double, String> m_formatter = new Converter<Double, String>()
        {
        @Override
        public String convert(Double value)
            {
            return value.toString();
            }
        };
    }


