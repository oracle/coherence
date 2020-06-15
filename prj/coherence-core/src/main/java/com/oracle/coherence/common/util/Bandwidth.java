/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link Bandwidth} represents an amount of memory (measured in bits) being
 * transferred per second.
 * <p>
 * Measurements are based on the decimal system as outlined by the IEC.
 * <p>
 * eg: In this implementation 1 kbps = 1000 bits per second, not 1024 bits per second
 *
 * @author cp, bko  2011.07.15
 */
public class Bandwidth
    {

    // ----- constructors -----------------------------------------------

    /**
     * Construct a {@link Bandwidth} by parsing the specified {@link String}.
     *
     * @param s  The {@link String} containing the definition of a {@link Bandwidth}
     */
    public Bandwidth(String s)
        {
        s = (s == null)
            ? null
            : s.trim();

        if ((s == null) || s.isEmpty())
            {
            throw new IllegalArgumentException("An empty or null string was provided.  Expected a bandwidth");
            }

        if (!s.equals("0"))
            {
            Matcher matcher = REGEX_PATTERN.matcher(s);

            if (!matcher.matches())
                {
                throw new IllegalArgumentException(String.format("The specified %s [%s] is invalid.", this.getClass().getName(), s));
                }

            // determine the rate (using group 3)
            Rate rate = Rate.fromSuffix(matcher.group(3));

            // determine the magnitude (using group 2)
            Magnitude magnitude = Magnitude.fromSuffix(matcher.group(2));

            // determine the amount (using group 1)
            double cUnits = Double.valueOf(matcher.group(1));

            m_cBits = rate.toBits(Math.round(cUnits * magnitude.getFactor()));
            }
        }

    /**
     * Construct a {@link Bandwidth} given a specified units and {@link Rate}.
     * <p>
     * As the amount is a double precision value, the resulting {@link Bandwidth} will be rounded to the closest unit.
     *
     * @param cUnits        The number of units of the {@link Magnitude} and {@link Rate}.
     * @param rate          The {@link Rate}
     */
    public Bandwidth(double cUnits, Rate rate)
        {
        assert cUnits >= 0.0;
        m_cBits = rate.toBits(Math.round(cUnits));
        }

    /**
     * Construct a {@link Bandwidth} given a specified units and {@link Rate}.
     *
     * @param cUnits        The number of units of the {@link Magnitude} and {@link Rate}.
     * @param rate          The {@link Rate}
     */
    public Bandwidth(int cUnits, Rate rate)
        {
        this((long) cUnits, rate);
        }

    /**
     * Construct a {@link Bandwidth} give a specified number of bytes.
     *
     * @param cBytes  The number of bytes in the memory size
     * @param rate    The {@link Rate}
     */
    public Bandwidth(long cBytes, Rate rate)
        {
        assert cBytes >= 0;
        m_cBits = rate.toBits(cBytes);
        }

    // ----- Bandwidth methods ---------------------------------------------

    /**
     * Obtain the {@link Bandwidth} as a value in the specified {@link Magnitude}.
     *
     * @param magnitude  The {@link Magnitude}
     *
     * @return The maximum number of units of the specified {@link Magnitude}
     *         for the {@link Bandwidth}.
     */
    public long as(Magnitude magnitude)
        {
        return m_cBits / magnitude.getFactor();
        }

    /**
     * Obtain the {@link Bandwidth} as a value in the specified {@link Rate}.
     *
     * @param rate  The {@link Rate}
     *
     * @return The number of units of the specified {@link Rate}
     *         for the {@link Bandwidth}.
     */
    public long as(Rate rate)
        {
        return rate.fromBits(m_cBits);
        }

    /**
     * Obtains a {@link String} representation of the {@link Bandwidth} (in {@link Rate#BITS}).
     *
     * @param fExact  Indicates an <strong>exact</strong> value is required or if a rounded value will suffice.
     *
     * @return A {@link String}
     */
    public String toString(boolean fExact)
        {
        Magnitude magnitude = Magnitude.BASE;
        long      nBits     = m_cBits;

        // find the highest magnitude to represent the number appropriately
        while ((magnitude.next() != null) && (nBits >= magnitude.next().getFactor())
                && ((fExact && (nBits % magnitude.next().getFactor()) % (magnitude.next().getFactor() / 4) == 0) ||!fExact))
            {
            magnitude = magnitude.next();
            }

        long          cMagnitudeUnits    = nBits / magnitude.getFactor();
        long          nRemainder         = nBits % magnitude.getFactor();
        int           cSignificantDigits = 3;
        StringBuilder bldrString         = new StringBuilder();

        bldrString.append(cMagnitudeUnits);

        int cDigits          = bldrString.length();
        int cRemainingDigits = cSignificantDigits - cDigits;

        if ((cRemainingDigits > 0) && (nRemainder > 0))
            {
            int  nSignificanceFactor = (int) Math.pow(10, cRemainingDigits);
            long nDecimals           = (long) Math.floor(nRemainder * (double) nSignificanceFactor / magnitude.getFactor());

            if (nDecimals > 0)
                {
                bldrString.append(".");

                int cLeadingZeros = cRemainingDigits - (int)Math.log10(nDecimals) - 1;
                for (int i = 0; i < cLeadingZeros; i++)
                {
                    bldrString.append('0');
                }

                bldrString.append(nDecimals);
                }
            }

        bldrString.append(magnitude.getSuffix());
        bldrString.append("b/s");

        return bldrString.toString();
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
        {
        return 31 + (int) (m_cBits ^ (m_cBits >>> 32));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj)
        {
        return (this == obj) || ((obj != null) && (obj instanceof Bandwidth) && ((Bandwidth) obj).m_cBits == m_cBits);
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
     * A {@link Magnitude} of {@link Bandwidth}.
     */
    public enum Magnitude
        {
        BASE(1L, ""),
        KILO(1000L, "kilo"),
        MEGA(1000000L, "mega"),
        GIGA(1000000000L, "giga"),
        TERA(1000000000000L, "tera"),
        PETA(1000000000000000L, "peta"),
        EXA(1000000000000000000L, "exa");

        // ----- constructors -----------------------------------------------

        /**
         * Construct a {@link Magnitude}
         *
         * @param nFactor       The factor of the {@link Magnitude}
         * @param sDescription  The description of the {@link Magnitude}
         */
        Magnitude(long nFactor, String sDescription)
            {
            DESCRIPTION = sDescription.trim();
            SUFFIX      = DESCRIPTION.isEmpty()
                          ? ""
                          : DESCRIPTION.substring(0, 1);
            FACTOR      = nFactor;
            }

        // ----- Magnitude methods ------------------------------------------

        /**
         * Obtain the name of the {@link Magnitude}.  For example, a kilo has the
         * description "kilo".
         *
         * @return The {@link Magnitude}'s description
         */
        public String getDescription()
            {
            return DESCRIPTION;
            }

        /**
         * Obtain the suffix of the {@link Magnitude}.
         *
         * @return A {@link String}
         */
        public String getSuffix()
            {
            return SUFFIX;
            }

        /**
         * Obtain the factor of the {@link Magnitude}.
         *
         * @return The factor
         */
        public long getFactor()
            {
            return FACTOR;
            }

        /**
         * Determine if the passed suffix is compatible with this {@link Magnitude}'s suffix, ignoring case.
         *
         * @param s  The suffix to test
         *
         * @return true iff the passed string is compatible with the suffix of this {@link Magnitude}.
         */
        public boolean isSuffix(String s)
            {
            return s.equalsIgnoreCase(SUFFIX);
            }

        /**
         * Obtain the next order of {@link Magnitude} (above this one).
         *
         * @return The next order of {@link Magnitude} above this one or <code>null</code> if this is
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
         * @return The previous order of {@link Magnitude} or <code>null</code> if this is
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
         * @param sSuffix  The proposed suffix
         *
         * @return A {@link Magnitude} with the specified suffix
         */
        public static Magnitude fromSuffix(String sSuffix)
            {
            sSuffix = sSuffix.trim();

            if (sSuffix.length() == 0)
                {
                return Magnitude.BASE;
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

            throw new IllegalArgumentException(String.format("Unknown %s suffix [%s]", Magnitude.class.getName(), sSuffix));
            }

        // ----- data members -----------------------------------------------

        /**
         * The description of this {@link Magnitude}. For example, a kilo has the name "kilo".
         */
        private final String DESCRIPTION;

        /**
         * The number of order of the magnitude. For example, a kilo is 1000.
         */
        private final long FACTOR;

        /**
         * The suffix that represents this {@link Magnitude}. For example, a kilobyte has the suffix "K".
         */
        private final String SUFFIX;

        // ----- constants --------------------------------------------------

        /**
         * Cached copy of the values array to avoid garbage creation
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
        }

    // ----- Rate Enumeration -----------------------------------------------

    /**
     * A {@link Rate} of a {@link Bandwidth} per second.
     */
    public enum Rate
        {
        BITS(0, "b"),
        BYTES(3, "B");

        // ----- constructors -----------------------------------------------

        /**
         * Construct a {@link Rate}.
         *
         * @param cShift    The binary (left) shift that the {@link Rate} requires to convert to a number of bits
         * @param sSuffix   The suffix for the {@link Rate}
         */
        Rate(int cShift, String sSuffix)
            {
            SHIFT  = cShift;
            SUFFIX = sSuffix;
            }

        // ----- Rate methods -----------------------------------------------

        /**
         * Determine the name of the {@link Rate}.
         *
         * @return "bits" or "bytes"
         */
        public String getDescription()
            {
            return name().toLowerCase();
            }

        /**
         * Obtain the suffix that identifies the {@link Rate}.
         *
         * @return "b" for "bits", or "B" for "bytes"
         */
        public String getSuffix()
            {
            return SUFFIX;
            }

        /**
         * Convert the specified number of units of this {@link Rate} into a bits {@link Rate}.
         *
         * @param cUnits  The number of units of this {@link Rate}
         *
         * @return The number of {@link Rate#BITS} units.
         */
        public long toBits(long cUnits)
            {
            return cUnits << SHIFT;
            }

        /**
         * Convert the specified number of bits to units of this {@link Rate}.
         *
         * @param cBits  The number of bits.
         *
         * @return  The number of units of this {@link Rate}.
         */
        public long fromBits(long cBits)
            {
            return cBits >> SHIFT;
            }

        // ----- helpers ----------------------------------------------------

        /**
         * Convert a number of units of the specified {@link Rate} to another {@link Rate}.
         *
         * @param cUnits    The number of units
         * @param rateFrom  The {@link Rate} to convert from
         * @param rateTo    The {@link Rate} to convert to
         *
         * @return the number of bits
         */
        public static long convert(long cUnits, Rate rateFrom, Rate rateTo)
            {
            if (rateFrom.equals(rateTo))
                {
                return cUnits;
                }
            else
                {
                return rateTo.fromBits(rateFrom.toBits(cUnits));
                }
            }

        /**
         * Determine the {@link Rate} given the specified suffix.
         *
         * @param sSuffix  The proposed suffix
         *
         * @return A {@link Rate} with the specified suffix
         */
        public static Rate fromSuffix(String sSuffix)
            {
            sSuffix = sSuffix.trim();

            if (sSuffix.length() == 0)
                {
                return Rate.BITS;
                }
            else if (sSuffix.length() > 0)
                {
                for (Rate rate : Rate.VALUES)
                    {
                    if (rate.getSuffix().equals(sSuffix))
                        {
                        return rate;
                        }
                    }
                }

            throw new IllegalArgumentException(String.format("Unknown %s suffix [%s]", Rate.class.getName(), sSuffix));
            }

        // ----- data members -----------------------------------------------

        /**
         * Cached copy of the values array to avoid garbage creation
         */
        private static final Rate[] VALUES = Rate.values();

        /**
         * The binary shift that the {@link Rate} requires to convert a number
         * of units to or from the corresponding number of bytes.
         */
        private final int SHIFT;

        /**
         * The one-character suffix for the {@link Rate}.
         */
        private final String SUFFIX;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The number of {@link Rate#BITS} in the {@link Bandwidth}.
     */
    private long m_cBits;

    // ----- constants ------------------------------------------------------

    /**
     * The pre-compiled regular expression {@link Pattern} to match
     * a {@link Bandwidth} specified as a {@link String}.
     */
    private static final Pattern REGEX_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)([kKMmGgTtPpEe]?)/?([Bb]?)[Pp/]?[Ss]");
    }
