/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.util;


/**
 * A Histogram whose values are scaled such that low values have higher
 * resolution then high values.
 *
 * @author mf 2006.07.05
 */
public class ScaledHistogram
    extends Histogram
    {
    public ScaledHistogram()
        {
        this(1000000);
        }

    /**
     * Construct a ScaledHistogram.
     *
     * @param cMax  the maximum supported value
     */
    public ScaledHistogram(int cMax)
        {
        this (cMax, 100);
        }

    /**
     * Construct a ScaledHistogram.
     *
     * @param cMax      the maximum supported value
     * @param cHighRes  the number of singular values to store in an unscaled fashion
     */
    public ScaledHistogram(int cMax, int cHighRes)
        {
        super(computeSlot(cHighRes, cMax));

        f_cHighRes = cHighRes;
        }

    @Override
    public int getIndex(int nValue)
        {
        return computeSlot(f_cHighRes, nValue);
        }

    @Override
    public int getLabelMin(int nSlot)
        {
        return nSlot <= f_cHighRes
                ? nSlot
                : f_cHighRes + (int) Math.ceil(Math.pow(10, ((nSlot - f_cHighRes) / STRETCH)));
        }


    /**
     * Helper for computing slot for a given sample value.
     *
     * @param cHigh  the number of high precision slots
     * @param nValue the value
     *
     * @return the slot
     */
    protected static int computeSlot(int cHigh, int nValue)
        {
        return nValue <= cHigh
                ? nValue
                : cHigh + (int) ((Math.log(nValue - cHigh) / LOG10) * STRETCH);
        }

    protected final int f_cHighRes;
    public static final double LOG10 = Math.log(10);
    protected static final double STRETCH = 10;
    }
