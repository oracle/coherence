/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;


import com.tangosol.util.Base;


/**
 * A utility class that contains methods to work with truncated integer values.
 *
 * @author cp, gg 2015.06.05
 */
public abstract class Trint
    {
    // ----- Trint14 support ------------------------------------------------

    /**
     * Convert a long value to a "trint14".
     *
     * @param l  the long value to convert to a trint14
     *
     * @return  the equivalent unsigned 14-bits integer value (a "trint14");
     *          note that the returned value can never be zero
     */
    public static int makeTrint14(long l)
        {
        return TRINT14_PRESENT | ((int) l & TRINT14_MASK);
        }

    /**
     * Convert a 14-bits unsigned integer ("trint14") to a long value. This
     * guesses what the long value should be based on its proximity to the
     * passed "current" long value.
     * <p>
     * Unlike trint24, which is used for packet translation and is much more
     * error-critical, trint14 is used for the version encoding and supports a
     * concept "no-value" (-1), which indicates that there is no information
     * to make a guess or that the gap is to large to make a valid guess.
     * Additionally, since the current version can increases monotonically, the
     * returned value can never exceed the "current" value passed in.
     *
     * @param nTrint    the unsigned 14-bits integer value (a "trint14")
     * @param lCurrent  the long value that the trint will be translated based on
     *                 ("close to and less than")
     *
     * @return the long value represented by the trint or zero if there is no
     *         value in the trint or it cannot be calculated
     */
    public static long translateTrint14(int nTrint, long lCurrent)
        {
        if (lCurrent < 0)
            {
            throw new IllegalArgumentException("Negative current value");
            }

        if ((nTrint & TRINT14_PRESENT) == 0)
            {
            // no value present
            return -1;
            }

        long lLo = Math.max(0, lCurrent - TRINT14_MAX_VARIANCE);
        long lHi = lCurrent;

        nTrint &= TRINT14_MAX_VALUE;

        long lBase = lCurrent >>> 13;
        for (int i = -1; i <= 0; ++i)
            {
            long lGuess = ((lBase + i) << 13) | nTrint;
            if (lGuess >= lLo && lGuess <= lHi)
                {
                return lGuess;
                }
            }
        return -1;
        }

    // ----- constants -------------------------------------------------------

    /**
     * The bit mask for Trint14, which uses 14 bits:
     *  - 1 most significant bit for the presence of the value, and
     *  - 13 bits for the value itself
     */
    public static final int TRINT14_PRESENT = 0x20_00;
    public static final int TRINT14_MASK    = 0x1F_FF;

    /**
     * The domain span for Trint14 is 0x20_00.
     */
    public static final int TRINT14_DOMAIN_SPAN = TRINT14_MASK + 1;

    /**
     * The maximum value for Trint14, is 0x1F_FF
     */
    public static final int TRINT14_MAX_VALUE = TRINT14_DOMAIN_SPAN - 1;

    /**
     * The maximum variance (from a "current" value) for Trint14 is equal to its
     * max value, or 0x1F_FF.
     */
    public static final int TRINT14_MAX_VARIANCE = TRINT14_MAX_VALUE;

    /**
     * Unit test.
     */
    public static void main(String[] asArg)
        {
        if (0 != translateTrint14(makeTrint14(0), 0) ||
            0 != translateTrint14(makeTrint14(0), 1000))
            {
            throw new RuntimeException("Invalid trint for zero");
            }

        for (long lCurrent = 0, lIter = 1; lCurrent < 1024l * Integer.MAX_VALUE; lIter++)
            {
            long lTest  = lCurrent;
            int  nTrint = makeTrint14(lTest);

            lCurrent += Base.getRandom().nextInt(TRINT14_MAX_VALUE);

            if (lTest != translateTrint14(nTrint, lCurrent))
                {
                throw new RuntimeException("Invalid trint=" + nTrint
                    + "; lTest=" + lTest + "; lCurrent=" + lCurrent);
                }

            if (lIter % 1000000 == 0)
                {
                Base.out(Long.toHexString(lCurrent));
                }
            }
        }

    // ----- Trint24 support ------------------------------------------------

    /**
     * Convert a long value to a trint24.
     *
     * @param l  the long value to convert to a trint24
     *
     * @return the equivalent unsigned 3-byte integer value (a "trint24")
     */
    public static int makeTrint24(long l)
        {
        return (int) (l & TRINT24_MASK);
        }

    /**
     * Convert a three-byte unsigned integer ("trint24") to a long value. This
     * guesses what the long value should be based on its proximity to the
     * passed "current" long value.
     *
     * @param nTrint     the unsigned 3-byte integer value (a "trint24")
     * @param lCurrent   the long value that the trint will be translated based on
     *                  ("close to")
     *
     * @return the long value represented by the trint
     */
    public static long translateTrint24(int nTrint, long lCurrent)
        {
        long lLo = lCurrent - TRINT24_MAX_VARIANCE;
        long lHi = lCurrent + TRINT24_MAX_VARIANCE;

        // @since Coherence 2.2
        // only use the known trint hexits; this bullet-proofs against
        // accidental multiple "translate" calls, and against the "hack"
        // that bit-ors the poll trints with TRINT_DOMAIN_SPAN (that
        // forces them to be non-zero trints even when they wrap around)
        nTrint &= TRINT24_MAX_VALUE;

        long lBase = lCurrent >>> 24;
        for (int i = -1; i <= 1; ++i)
            {
            long lGuess = ((lBase + i) << 24) | nTrint;
            if (lGuess >= lLo && lGuess <= lHi)
                {
                // @since Coherence 2.2
                // 1) disallow negative trints because they are used as indexes
                // 2) disallow zero value trints because all windowed arrays
                //    for which trints are translated start at 1
                if (lGuess < 1L)
                    {
                    // there is only one acceptable case in which the value is
                    // negative, and that is when the current is unknown, which
                    // implies that it is zero (although we will also accept
                    // one just in case the current was primed like the windowed
                    // arrays are) ... since packets can come out-of-order, the
                    // assertion allows for some "slop"
                    if (lCurrent > 0x800L)
                        {
                        // This can happen if there is an extended duration between packets
                        // from the transmitting node, which its FromMessageId may naturally move
                        // well beyond the window size.  In this case we will not translate the
                        // trint into the same value which the transmitter used, but it will still
                        // be unique from our perspective, which is all that is required.
                        // COH-767 downgraded this from an exception to a debug message
                        Base.log("Large gap while initializing packet translation; "
                            + "current=" + lCurrent + " packet=" + nTrint + " value=" + lGuess);
                        }
                    lGuess += TRINT24_DOMAIN_SPAN;
                    Base.azzert(lGuess >= 1L);
                    }

                return lGuess;
                }
            }
        throw new IllegalStateException("translateTrint failed: nTrint="
            + nTrint + ", lCurrent=" + lCurrent);
        }

    /**
     * The bit mask for Trint24, which uses 6 hexits (3 bytes).
     */
    public static final int TRINT24_MASK = 0x00_FF_FF_FF;

    /**
     * The domain span for Trint24 is 0x01_00_00_00.
     */
    public static final int TRINT24_DOMAIN_SPAN = TRINT24_MASK + 1;

    /**
     * The maximum value for Trint24 is 0xFF_FF_FF.
     */
    public static final int TRINT24_MAX_VALUE = TRINT24_DOMAIN_SPAN - 1;

    /**
     * The maximum variance (from a "current" value) for Trint24 is half its domain
     * span, or 0x00_80_00_00.
     */
    public static final int TRINT24_MAX_VARIANCE = TRINT24_DOMAIN_SPAN >> 1;
    }
