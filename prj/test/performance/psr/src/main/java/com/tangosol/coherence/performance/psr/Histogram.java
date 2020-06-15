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

import java.io.IOException;
import java.io.PrintStream;


/**
* Class for tracking samples in a histogram.
*
* @author mf,jh  2008.06.18
*/
public class Histogram
        implements PortableObject, Cloneable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor necessary for POF serialization.
    */
    public Histogram()
        {
        super();
        }

    /**
    * Construct a histogram of a given size.
    *
    * @param cLabels the maximum sample array size for the histogram
    * @param sUnits  the unit of measure for the histogram
    */
    public Histogram(int cLabels, String sUnits)
        {
        setResults(new long[cLabels]);
        setUnits(sUnits);
        }


    // ----- Histogram methods ----------------------------------------------

    /**
    * Add a sample to the histogram.
    *
    * @param nSample  the sample value
    */
    public void addSample(long nSample)
        {
        ++getResults()[getIndex(nSample)];
        }

    /**
    * Add all samples from the given histogram to this histogram.
    *
    * @param histThat  the source histogram
    */
    public void addSamples(Histogram histThat)
        {
        if (histThat == null)
            {
            return;
            }
        ensureCompatible(histThat);

        long[] alResultsThat = histThat.getResults();
        long[] alResultsThis = getResults();

        for (int i = 0, c = alResultsThat.length; i < c; ++i)
            {
            alResultsThis[i] += alResultsThat[i];
            }
        }

    /**
    * Return a new histogram that represents the delta between this histogram
    * and the specified histogram (i.e. this - that).
    *
    * @param histThat  the histogram to subtract from this histogram
    *
    * @return the delta histogram
    */
    public Histogram computeDelta(Histogram histThat)
        {
        if (histThat != null)
            {
            ensureCompatible(histThat);
            }

        Histogram histDiff;
        try
            {
            histDiff = (Histogram) clone();
            }
        catch (CloneNotSupportedException e)
            {
            throw new RuntimeException(e);
            }

        if (histThat == null)
            {
            histDiff.setResults((long[]) getResults().clone());
            }
        else
            {
            long[] alResultsThat = histThat.getResults();
            long[] alResultsThis = getResults();
            int    cLabels       = alResultsThat.length;
            long[] alResultsDiff = new long[cLabels];
            for (int i = 0; i < cLabels; ++i)
                {
                alResultsDiff[i] = alResultsThis[i] - alResultsThat[i];
                }

            histDiff.setResults(alResultsDiff);
            }

        return histDiff;
        }

    /**
    * Write histogram in spreadsheet loadable format.
    *
    * @param out      the stream to write the report to
    * @param fHeader  if true, output column labels
    */
    public void writeReport(PrintStream out, boolean fHeader)
        {
        long[] alResults = getResults();
        int    cLabels   = alResults.length;

        // output the header
        if (fHeader)
            {
            for (int i = 0; i < cLabels; ++i)
                {
                out.print(getLabelText(i) + '\t');
                }
            out.println();
            }

        // all results are written so that we may build a table of
        // multiple histograms and compare them
        for (int i = 0, cSkip = 0; i < cLabels; ++i)
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


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public Object clone()
            throws CloneNotSupportedException
        {
        return super.clone();
        }

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return toString(false);
        }

    /**
    * {@inheritDoc}
    */
    public String toString(boolean fVerbose)
        {
        long[]       alResults = getResults();
        StringBuffer sbHist    = fVerbose ? new StringBuffer() : null;
        long         cSamples  = 0;
        long         cTotal    = 0;
        int          iMin      = -1;
        int          iMax      = 0;
        double       cTotalSq  = 0;
        for (int i = 0, c = alResults.length; i < c; ++i)
            {
            long cSample = alResults[i];
            if (cSample > 0)
                {
                if (iMin == -1)
                    {
                    iMin = i;
                    }
                iMax = i;
                if (sbHist != null)
                    {
                    sbHist.append(getLabelText(i))
                          .append('=')
                          .append(cSample)
                          .append('\n');
                    }
                cSamples += cSample;

                long nMedian = getLabelMedian(i);
                long nSample = cSample * nMedian;

                cTotal   += nSample;
                cTotalSq += nSample * nMedian;
                }
            }

        double dAvg    = cTotal / (double) cSamples;
        double dStdDev = Math.sqrt(
                ((cSamples * cTotalSq) - (cTotal * (double) cTotal))
               / (cSamples * (double) (cSamples - 1)));

        dAvg    = ((long) (dAvg    * 10)) / 10.0;
        dStdDev = ((long) (dStdDev * 10)) / 10.0;

        String sUnits = getUnits();

        StringBuffer sb = new StringBuffer()
            .append("samples=") .append(cSamples)
            .append("; total=") .append(cTotal).append(sUnits)
            .append("; min=")   .append(getLabelText(iMin))
            .append("; max=")   .append(getLabelText(iMax))
            .append("; avg=")   .append(dAvg).append(sUnits)
            .append("; stddev=").append(dStdDev).append(sUnits);

        if (sbHist != null)
            {
            sb.append('\n').append(sbHist);
            }

        return sb.toString();
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        int    iProp     = 0;
        int    cLabels   = in.readInt(iProp++);
        long[] alResults = new long[cLabels];
        for (int i = 0; i < cLabels; )
            {
            long n = in.readLong(iProp++);
            if (n < 0)
                {
                // skip over gaps
                i += -n;
                continue;
                }
            alResults[i++] = n;
            }

        setUnits(in.readString(iProp));
        setResults(alResults);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        int    iProp      = 0;
        int    iSkipStart = -1;
        long[] alResults  = getResults();
        int    cLabels    = alResults.length;

        // write the results
        out.writeInt(iProp++, cLabels);
        for (int i = 0; i < cLabels; ++i)
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
                    out.writeLong(iProp++, iSkipStart - i);
                    iSkipStart = -1;
                    }
                out.writeLong(iProp++, n);
                }
            }
        if (iSkipStart >= 0)
            {
            out.writeLong(iProp++, iSkipStart - cLabels);
            }

        // write the units
        out.writeString(iProp, getUnits());
        }


    // ----- internal methods -----------------------------------------------

    /**
    * Return the index in the underlying storage array that the given sample
    * should be stored in.
    *
    * @param nSample  the sample to be stored
    *
    * @return the index in the sample storage array that would store the
    *         given sample
    */
    protected int getIndex(long nSample)
        {
        int nLabelMax = getResults().length - 1;
        return nSample > nLabelMax ? nLabelMax: (int) nSample;
        }

    /**
    * Return the minimum sample value that can be stored in the storage array
    * at the specified index.
    *
    * @param i  the index
    *
    * @return the minimum sample value that can be stored in the storage
    *         array at the specified index
    */
    protected long getLabelMin(int i)
        {
        return i;
        }

    /**
    * Return the maximum sample value that can be stored in the storage array
    * at the specified index.
    *
    * @param i  the index
    *
    * @return the maximum sample value that can be stored in the storage
    *         array at the specified index
    */
    protected long getLabelMax(int i)
        {
        return getLabelMin(i + 1) - 1;
        }

    /**
    * Return the median sample value that can be stored in the storage array
    * at the specified index.
    *
    * @param i  the index
    *
    * @return the median sample value that can be stored in the storage
    *         array at the specified index
    */
    protected long getLabelMedian(int i)
        {
        long nMin = getLabelMin(i);
        long nMax = getLabelMax(i);
        return nMin + (nMax - nMin) / 2;
        }

    /**
    * Return a textual label corresponding to a given storage array index.
    *
    * @param i  the index
    *
    * @return a textual lable for the given index
    */
    protected String getLabelText(int i)
        {
        if (i == getResults().length - 1)
            {
            // result was over the max supported by the histogram
            return "over-max";
            }

        long nMin    = getLabelMin(i);
        long nMax    = getLabelMax(i);
        long nMedian = getLabelMedian(i);
        long nRange  = nMax - nMin;

        String sUnits = getUnits();

        if (nRange > 1)
             {
             return nMedian + sUnits + " +/-" + (nRange / 2);
             }
        return Long.toString(nMedian) + sUnits;
        }

    /**
    * Ensure that the given histogram is compatible with this histogram.
    *
    * @param histThat  the histogram to ensure compatibility with
    */
    protected void ensureCompatible(Histogram histThat)
        {
        if (getClass().equals(histThat.getClass()) &&
            getUnits().equals(histThat.getUnits()) &&
            getResults().length == histThat.getResults().length)
            {
            return;
            }

        throw new IllegalArgumentException("incompatible histogram");
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the array of results.
    *
    * @return the array of results
    */
    protected long[] getResults()
        {
        return m_alResults;
        }

    /**
    * Configure the array of results.
    *
    * @param alResults  the array of results
    */
    protected void setResults(long[] alResults)
        {
        assert alResults != null;
        m_alResults = alResults;
        }

    /**
    * Return the unit of measure.
    *
    * @return the unit of measure
    */
    public String getUnits()
        {
        String sUnits = m_sUnits;
        return sUnits == null ? "" : sUnits;
        }

    /**
    * Configure the unit of measure.
    *
    * @param sUnits  the unit of measure
    */
    protected void setUnits(String sUnits)
        {
        m_sUnits = sUnits;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The array of results.
    */
    protected long[] m_alResults;

    /**
    * The unit of measure.
    */
    protected String m_sUnits;
    }
