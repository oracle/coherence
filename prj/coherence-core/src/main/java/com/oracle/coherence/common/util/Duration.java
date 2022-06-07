/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link Duration} represents an amount of time, with nanosecond accuracy.
 * <p>
 * This implementation is inspired (and copied mostly) from the earlier work by Cameron Purdy.
 *
 * @author bko  2011.07.19
 */
public class Duration
    {
    /**
     * Construct a {@link Duration} given another {@link Duration}.
     *
     * @param d  the {@link Duration}
     */
    public Duration(Duration d)
        {
        assert d != null;
        m_cNanos = d.m_cNanos;
        }

    // ----- constructors ---------------------------------------------------

    /**
     * Construct a {@link Duration} give a specified number of nano seconds.
     *
     * @param cNanos  the number of nano seconds in the {@link Duration}
     */
    public Duration(long cNanos)
        {
        assert cNanos >= 0;
        m_cNanos = cNanos;
        }

    /**
     * Construct a {@link Duration} by parsing the specified {@link String}.
     * <p>
     * The format of the {@link String} consists of one or more numbers, each with a specific {@link Magnitude}
     * separated by white space.
     * <p>
     * Note 1: Numbers may contain decimal places.
     * <p>
     * Note 2: Each number must be followed by a specific {@link Magnitude}.  For a more relaxed {@link String}-based
     * constructor, including the ability to specify a default {@link Magnitude}, use
     * {@link #Duration(String, Magnitude)} instead.
     * <p>
     * For example:  The following are valid {@link Duration}s.
     * "10s", "0ns", "1hr 10m", "5us", "100ms", "12m", "1.5h", "20m 10.5s"
     *
     * @param s  the string containing the {@link Duration}
     */
    public Duration(String s)
        {
        this(s, null);
        }

    /**
     * Construct a {@link Duration} given a specified amount of a {@link Magnitude}.
     * <p>
     * As the amount is a double precision value, the resulting {@link Duration}
     * will be rounded to the closest nanosecond.
     *
     * @param nAmount    the amount of the {@link Magnitude}
     * @param magnitude  the {@link Magnitude}
     */
    public Duration(double nAmount, Magnitude magnitude)
        {
        assert nAmount >= 0.0;
        m_cNanos = Math.round(nAmount * magnitude.getFactor());
        }

    /**
     * Construct a {@link Duration} given a specified amount of a {@link Magnitude}.
     *
     * @param nAmount    the amount of the {@link Magnitude}
     * @param magnitude  the {@link Magnitude}
     */
    public Duration(int nAmount, Magnitude magnitude)
        {
        assert nAmount >= 0;
        m_cNanos = nAmount * magnitude.getFactor();
        }

    /**
     * Construct a {@link Duration} by parsing the specified {@link String}.
     * <p>
     * The format of the {@link String} is either a number (without a specified {@link Magnitude}) or a {@link String}
     * containing one or more numbers, each with a specific {@link Magnitude} separated by white space.
     * <p>
     * Note: numbers may contain decimal places.
     * <p>
     * For example:  The following are valid {@link Duration}s.
     * "10s", "0ns", "1hr 10m", "0", "5us", "100ms", "12m", "1.5h", "20m 10.5s"
     *
     * @param s  the string containing the {@link Duration}
     * @param m  the default {@link Magnitude} to use if the specified {@link String} does not specify a
     *           {@link Magnitude}.  when <code>null</code> a {@link Magnitude} must be specified in the {@link String}.
     *           when not <code>null</code> the {@link String} may only contain a single number.
     */
    public Duration(String s, Magnitude m)
        {
        s = (s == null) ? null : s.trim();

        if ((s == null) || s.isEmpty())
            {
            throw new IllegalArgumentException("An empty or null string was provided.  Expected a duration size");
            }

        // when a default magnitude was specified, attempt to match just the number
        boolean fUsedDefault = false;

        if (m != null)
            {
            Matcher matcher = REGEX_NUMBER.matcher(s);

            if (matcher.matches())
                {
                String sAmount = matcher.group(1);
                double nAmount = Double.valueOf(sAmount);

                m_cNanos     = Math.round(nAmount * m.getFactor());
                fUsedDefault = true;
                }
            }

        // when a default magnitude wasn't used, attempt to match using explicit magnitudes
        if (!fUsedDefault && !s.equals("0"))
            {
            Matcher matcher = REGEX_PATTERN.matcher(s);

            if (!matcher.matches())
                {
                throw new IllegalArgumentException(String.format("The specified %s [%s] is invalid.",
                    this.getClass().getName(), s));
                }

            m_cNanos = 0;

            int i = 1;

            while (i < matcher.groupCount())
                {
                String sAmount = matcher.group(i + 1);

                if (sAmount != null)
                    {
                    // determine the amount of the magnitude
                    double nAmount = Double.valueOf(sAmount);

                    // determine the magnitude
                    String    sSuffix   = matcher.group(i + 2);
                    Magnitude magnitude = Magnitude.fromSuffix(sSuffix);

                    m_cNanos += Math.round(nAmount * magnitude.getFactor());
                    }

                i += 3;
                }
            }
        }

    // ----- Duration methods -----------------------------------------------

    /**
     * Obtains the number of nano seconds in the {@link Duration}.
     *
     * @return The number of nano seconds
     */
    public long getNanos()
        {
        return m_cNanos;
        }

    /**
     * Obtains the {@link Duration} in the specified {@link Magnitude} (rounded down).
     *
     * @param magnitude  the required {@link Magnitude}
     *
     * @return The number of units of the specified {@link Magnitude}.
     */
    public long as(Magnitude magnitude)
        {
        return m_cNanos / magnitude.getFactor();
        }

    /**
     * Obtains a {@link String} representation of the {@link Duration} using the most appropriate {@link Magnitude}
     * to simplify the representation.
     * <p>
     * Note: Using {@link #toString()} will result in a non-exact representation.
     *
     * @param fExact  indicates an <strong>exact</strong> value is required or if a rounded value will suffice.
     *
     * @return A {@link String}
     */
    public String toString(boolean fExact)
        {
        StringBuilder sbResult        = new StringBuilder();
        long          cRemainingNanos = m_cNanos;
        int           cLimit          = 2;

        for (Magnitude magnitude = Magnitude.HIGHEST; cRemainingNanos > 0 && (fExact || cLimit > 0);  magnitude = magnitude.previous())
            {
            long cMagnitudeUnits = cRemainingNanos / magnitude.getFactor();

            if (cMagnitudeUnits > 0)
                {
                cRemainingNanos -= cMagnitudeUnits * magnitude.getFactor();

                if (fExact || magnitude.ordinal() > Magnitude.SECOND.ordinal())
                    {
                    sbResult.append(cMagnitudeUnits);
                    }
                else // non-exact and seconds or less, express as fraction
                    {
                    long cNanosPerMagnitudeFractionUnit = magnitude.getFactor() / 1000;
                    long cMagnitudeFractionUnits = (cNanosPerMagnitudeFractionUnit > 0)
                                                   ? cRemainingNanos / cNanosPerMagnitudeFractionUnit : 0;
                    long cMagnitudeFractionNanos = cMagnitudeFractionUnits * cNanosPerMagnitudeFractionUnit;

                    double flRem = cMagnitudeFractionNanos * 1000 / magnitude.getFactor() / 1000.0;

                    if (flRem >= 0.01 && (fExact || cLimit > 1))
                        {
                        sbResult.append(String.format("%.2f", cMagnitudeUnits + flRem));
                        }
                    else
                        {
                        sbResult.append(cMagnitudeUnits);
                        }
                    cRemainingNanos = 0;
                    }

                sbResult.append(magnitude.getSuffix());
                --cLimit;
                }
            else if (sbResult.length() > 0)
                {
                --cLimit;
                }
            }

        if (sbResult.length() == 0)
            {
            return "0ns";
            }
        else
            {
            return sbResult.toString();
            }
        }

    /**
     * Return this {@link Duration} as a {@link java.time.Duration Java Time Duration}.
     *
     * @return this {@link Duration} as a {@link java.time.Duration Java Time Duration}
     */
    public java.time.Duration asJavaDuration()
        {
        return java.time.Duration.ofNanos(getNanos());
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
        {
        return 31 + (int) (m_cNanos ^ (m_cNanos >>> 32));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj)
        {
        return (this == obj) || ((obj != null) && (obj instanceof Duration) && ((Duration) obj).m_cNanos == m_cNanos);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return toString(false);
        }

    // ----- Magnitude Enumeration ------------------------------------------

    /**
     * The {@link Magnitude} of the {@link Duration}.
     */
    public enum Magnitude
        {
        NANO(1L, "ns"),
        MICRO(1000L, "us", "\u00B5s", "\u039Cs", "\u03BCs"),
        MILLI(1000000L, "ms"),
        SECOND(1000000000L, "s"),
        MINUTE(60 * 1000000000L, "m"),
        HOUR(60 * 60 * 1000000000L, "h"),
        DAY(24 * 60 * 60 * 1000000000L, "d");

        // ----- constructors -----------------------------------------------

        /**
         * Construct a {@link Magnitude}.
         *
         * @param nFactor    the factor which this Magnitude represents, relative to the number of nanoseconds
         * @param sSuffixes  the suffix (case-insensitive) of the magnitude
         */
        Magnitude(long nFactor, String... sSuffixes)
            {
            FACTOR   = nFactor;
            SUFFIXES = sSuffixes;
            }

        // ----- Magnitude methods ------------------------------------------

        /**
         * Determine the factor of the {@link Magnitude} relative to the number of
         * nanoseconds. For example, "MILLI" has a factor of 1000000.
         *
         * @return the factor of the {@link Magnitude}
         */
        public long getFactor()
            {
            return FACTOR;
            }

        /**
         * Obtain the default for the {@link Magnitude}. For example, "MILLI" has the suffix
         * "ms".
         *
         * @return the suffix of the {@link Magnitude}.
         */
        public String getSuffix()
            {
            return SUFFIXES[0];
            }

        /**
         * Determine if the passed suffix is compatible with this {@link Magnitude}'s suffix, ignoring case.
         *
         * @param s  the suffix to test
         *
         * @return true iff the passed string is compatible with the suffix of this {@link Magnitude}.
         */
        public boolean isSuffix(String s)
            {
            for (String sSuffix : SUFFIXES)
                {
                if (sSuffix.equalsIgnoreCase(s))
                    {
                    return true;
                    }
                }

            return false;
            }

        /**
         * Obtain the next order of {@link Magnitude} (above this one).
         *
         * @return the next order of {@link Magnitude} above this one or <code>null</code> if this is
         *         the {@link #HIGHEST}.
         */
        public Magnitude next()
            {
            if (this.equals(Magnitude.HIGHEST))
                {
                return null;
                }
            else
                {
                return Magnitude.VALUES[this.ordinal() + 1];
                }
            }

        /**
         * Obtain the previous order of {@link Magnitude} (above this one).
         *
         * @return the previous order of {@link Magnitude} or <code>null</code> if this is
         *         the {@link #LOWEST}.
         */
        public Magnitude previous()
            {
            if (this.equals(Magnitude.LOWEST))
                {
                return null;
                }
            else
                {
                return Magnitude.VALUES[this.ordinal() - 1];
                }
            }

        // ----- helpers ----------------------------------------------------

        /**
         * Determine the {@link Magnitude} given the specified suffix.
         *
         * @param sSuffix  the proposed suffix
         *
         * @return a {@link Magnitude} with the specified suffix
         */
        public static Magnitude fromSuffix(String sSuffix)
            {
            sSuffix = sSuffix.trim();

            if (sSuffix.length() == 0)
                {
                return Magnitude.NANO;
                }
            else if (sSuffix.length() > 0)
                {
                for (Magnitude magnitude : Magnitude.VALUES)
                    {
                    if (magnitude.isSuffix(sSuffix))
                        {
                        return magnitude;
                        }
                    }
                }

            throw new IllegalArgumentException(String.format("Unknown %s suffix [%s]", Magnitude.class.getName(),
                sSuffix));
            }

        // ----- constants --------------------------------------------------

        /**
         * Cached copy of the VALUES array to avoid garbage creation
         */
        private static final Magnitude[] VALUES = Magnitude.values();

        /**
         * The lowest defined order of {@link Magnitude}.
         */
        public final static Magnitude LOWEST = Magnitude.VALUES[0];

        /**
         * The highest defined order of {@link Magnitude}.
         */
        public final static Magnitude HIGHEST = Magnitude.VALUES[Magnitude.VALUES.length - 1];

        // ----- data members -----------------------------------------------

        /**
         * The number of nanoseconds in a single unit of this magnitude. For
         * example, a minute has 60 billion nanoseconds.
         */
        public final long FACTOR;

        /**
         * The suffixes that for the {@link Magnitude}. For example, "MILLI" has
         * the suffix "ms".
         */
        public final String[] SUFFIXES;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The pre-compiled regular expression {@link Pattern}s to match
     * a {@link Duration} specified as a {@link String}.
     */
    private static final String  NUMBER         = "(\\d+(?:\\.\\d+)?)";
    private static final String  PATTERN_MINUTE = "\\s*(" + NUMBER + "\\s*([Mm]))?";
    private static final String  PATTERN_HOUR   = "\\s*(" + NUMBER + "\\s*([Hh]))?";
    private static final String  PATTERN_DAY    = "\\s*(" + NUMBER + "\\s*([Dd]))?";
    private static final String  PATTERN_SECOND = "\\s*(" + NUMBER + "\\s*([Ss]))?";
    private static final String  PATTERN_NANO   = "\\s*(" + NUMBER + "\\s*([Nn][Ss]))?";
    private static final String  PATTERN_MILLI  = "\\s*(" + NUMBER + "\\s*([Mm][Ss]))?";
    private static final String  PATTERN_MICRO  = "\\s*(" + NUMBER + "\\s*([Uu\u00B5Mm\\u039C\\u03BC][Ss]))?";
    private static final Pattern REGEX_NUMBER   = Pattern.compile(NUMBER);
    private static final Pattern REGEX_PATTERN = Pattern.compile(PATTERN_DAY + PATTERN_HOUR + PATTERN_MINUTE
                                                     + PATTERN_SECOND + PATTERN_MILLI + PATTERN_MICRO + PATTERN_NANO);

    // ----- data members ---------------------------------------------------

    /**
     * The number of nanos in the {@link Duration}.
     */
    private long m_cNanos;
    }
