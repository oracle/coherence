/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.psr;


/**
* A Histogram whose values are scaled such that low values have higher
* resolution then high values.
*/
public class ScaledHistogram
        extends Histogram
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor necessary for POF serialization.
    */
    public ScaledHistogram()
        {
        }

    /**
    * Construct a histogram of a given size.
    *
    * @param cLabels the maximum sample array size for the histogram
    * @param sUnits  the unit of measure for the histogram
    */
    public ScaledHistogram(int cLabels, String sUnits)
        {
        super(cLabels, sUnits);
        }


    // ----- Histogram methods ----------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected int getIndex(long nSample)
        {
        if (nSample <= 10)
            {
            return (int) nSample;
            }

        return super.getIndex((long) ((Math.log(nSample) / LOG_10) * 10));
        }

    /**
    * {@inheritDoc}
    */
    protected long getLabelMin(int i)
        {
         if (i <= 10)
            {
            return i;
            }

        return (int) Math.ceil(Math.pow(10, (i / 10.0)));
        }


    // ----- constants ------------------------------------------------------

    /**
    * Cached value of Math.log(10).
    */
    public static final double LOG_10 = Math.log(10);
    }
