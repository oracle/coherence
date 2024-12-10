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
 * A {@link MemorySize} represents an amount of memory, with byte accuracy.
 * <p>
 * Measurements are based on the base two standard (using octets) and not base ten as outlined by the IEC.
 * <p>
 * eg: In this implementation 1 kilobyte = 1024 bytes, not 1000 bytes.
 *
 * @author cp, bko  2011.07.11
 */
public class MemorySize
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a {@link MemorySize} give a specified number of bytes.
     *
     * @param cBytes  the number of bytes in the memory size
     */
    public MemorySize(long cBytes)
        {
        assert cBytes >= 0;
        m_cBytes = cBytes;
        }

    /**
     * Constructs a {@link MemorySize} based on another {@link MemorySize}.
     *
     * @param m  the {@link MemorySize} (not <code>null</code>)
     */
    public MemorySize(MemorySize m)
        {
        assert m != null;
        m_cBytes = m.m_cBytes;
        }

    /**
     * Construct a {@link MemorySize} by parsing the specified {@link String}.
     * <p>
     * The format of the {@link String} is a number followed by a {@link Magnitude}.
     * <p>
     * Note: numbers may contain decimal places.
     * <p>
     * For example:  The following are valid {@link MemorySize}s.
     * "10m", "0k", "1b", "1.75gb"
     *
     * @param s  the string containing the {@link MemorySize}
     */
    public MemorySize(String s)
        {
        this(s, null);
        }

    /**
     * Construct a {@link MemorySize} given a specified amount of a {@link Magnitude}.
     * <p>
     * As the amount is a double precision value, the resulting {@link MemorySize}
     * will be rounded to the closest byte.
     *
     * @param nAmount    the amount of the {@link Magnitude}
     * @param magnitude  the {@link Magnitude}
     */
    public MemorySize(double nAmount, Magnitude magnitude)
        {
        assert nAmount >= 0.0;
        m_cBytes = Math.round(nAmount * magnitude.getByteCount());
        }

    /**
     * Construct a {@link MemorySize} given a specified amount of a {@link Magnitude}.
     *
     * @param nAmount    the amount of the {@link Magnitude}
     * @param magnitude  the {@link Magnitude}
     */
    public MemorySize(int nAmount, Magnitude magnitude)
        {
        assert nAmount >= 0;
        m_cBytes = nAmount * magnitude.getByteCount();
        }

    /**
     * Construct a {@link MemorySize} by parsing the specified {@link String}.
     * <p>
     * The format of the {@link String} is a number possibly followed by a specific {@link Magnitude}.
     * <p>
     * Note: numbers may contain decimal places.
     * <p>
     * For example:  The following are valid {@link MemorySize}s.
     * "10m", "0k", "1b", "0", "1.75gb"
     *
     * @param s  the string containing the {@link MemorySize}
     * @param m  the default {@link Magnitude} to use if the specified {@link String} does not specify a
     *           {@link Magnitude}.  when <code>null</code> a {@link Magnitude} specified in the {@link String} is used
     *           and if not present, {@link Magnitude#BYTES} is assumed
     */
    public MemorySize(String s, Magnitude m)
        {
        s = (s == null) ? null : s.trim();

        if ((s == null) || s.isEmpty())
            {
            throw new IllegalArgumentException("An empty or null string was provided.  Expected a memory size");
            }

        if (!s.equals("0"))
            {
            Matcher matcher = REGEX_PATTERN.matcher(s);

            if (!matcher.matches())
                {
                throw new IllegalArgumentException(String.format("The specified %s [%s] is invalid.",
                    this.getClass().getName(), s));
                }

            // determine the desired magnitude from the suffix (using group 2) or use the specified default
            String    sSuffix   = matcher.group(2);
            Magnitude magnitude = sSuffix == null || sSuffix.trim().isEmpty() ? m : Magnitude.fromSuffix(sSuffix);

            // when there's no magnitude we default to bytes
            if (magnitude == null)
                {
                magnitude = Magnitude.BYTES;
                }

            // determine the amount (using group 1)
            double nAmount = Double.valueOf(matcher.group(1));

            m_cBytes = Math.round(nAmount * magnitude.getByteCount());
            }
        }

    // ----- MemorySize methods ---------------------------------------------

    /**
     * Obtain the {@link MemorySize} as a value in the specified {@link Magnitude}.
     *
     * @param magnitude  the {@link Magnitude}
     *
     * @return the number of units of the specified {@link Magnitude} that make up the {@link MemorySize}
     */
    public double as(Magnitude magnitude)
        {
        return ((double) m_cBytes) / magnitude.getByteCount();
        }

    /**
     * Obtain the number of bytes represented by the {@link MemorySize}.
     *
     * @return the number of bytes.
     */
    public long getByteCount()
        {
        return m_cBytes;
        }

    /**
     * Obtains a {@link String} representation of the {@link MemorySize} using the most appropriate {@link Magnitude}
     * to simplify the representation.
     * <p>
     * Note: Using {@link #toString()} will result in a non-exact representation.
     *
     * @param fExact  indicates an <strong>exact</strong> value is required or if an approximate value
     *                (with three significant digits) will suffice
     *
     * @return a {@link String}
     */
    public String toString(boolean fExact)
        {
        Magnitude magnitude = Magnitude.BYTES;
        long      nBytes    = m_cBytes;

        // find the highest magnitude to represent the number of bytes appropriately
        while ((magnitude.next() != null) && (nBytes >= magnitude.next().getByteCount())
            && ((fExact && (nBytes % magnitude.next().getByteCount()) % (magnitude.next().getByteCount() / 4) == 0)
                || !fExact))
            {
            magnitude = magnitude.next();
            }

        long          cMagnitudeUnits    = nBytes / magnitude.getByteCount();
        long          nRemainder         = nBytes % magnitude.getByteCount();
        int           cSignificantDigits = 3;
        StringBuilder bldrString         = new StringBuilder();

        bldrString.append(cMagnitudeUnits);

        int cDigits          = bldrString.length();
        int cRemainingDigits = cSignificantDigits - cDigits;

        if ((cRemainingDigits > 0) && (nRemainder > 0))
            {
            int  nSignificanceFactor = (int) Math.pow(10, cRemainingDigits);
            long nDecimals = (long) Math.floor(nRemainder * (double) nSignificanceFactor / magnitude.getByteCount());

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

        return bldrString.toString();
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
        {
        return 31 + (int) (m_cBytes ^ (m_cBytes >>> 32));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj)
        {
        return (this == obj)
               || ((obj != null) && (obj instanceof MemorySize) && ((MemorySize) obj).m_cBytes == m_cBytes);
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
     * The {@link Magnitude} of the {@link MemorySize}.
     */
    public enum Magnitude
        {
        BYTES(0, "B", "bytes"),
        KB(10, "KB", "kilobytes"),
        MB(20, "MB", "megabytes"),
        GB(30, "GB", "gigabytes"),
        TB(40, "TB", "terabytes"),
        PB(50, "PB", "petabytes"),
        EB(60, "EB", "exabytes");

        // ----- constructors -----------------------------------------------

        /**
         * Construct a {@link Magnitude}
         *
         * @param cShift        the number of bits to shift a count of {@link Magnitude}
         *                      units to the left in order to calculate a byte count
         * @param sSuffix       the String suffix that represents this {@link Magnitude}
         * @param sDescription  the description of the {@link Magnitude}
         */
        Magnitude(int cShift, String sSuffix, String sDescription)
            {
            SHIFT_COUNT = cShift;
            SUFFIX      = sSuffix.trim();
            SUFFIX_CHAR = (sSuffix.length() > 0) ? Character.toUpperCase(sSuffix.charAt(0)) : 0;
            DESCRIPTION = sDescription;
            BYTE_COUNT  = 1L << SHIFT_COUNT;
            BIT_MASK    = BYTE_COUNT - 1;
            }

        // ----- Magnitude methods ------------------------------------------

        /**
         * Determine the number of bytes in a single unit of this {@link Magnitude}
         * For example, a kilobyte has 1024 bytes.
         *
         * @return the number of bytes in a single unit of this {@link Magnitude}
         */
        public long getByteCount()
            {
            return BYTE_COUNT;
            }

        /**
         * Obtain the name of the {@link Magnitude}  For example, a kilobyte has the
         * description "kilobyte".
         *
         * @return the {@link Magnitude}'s description
         */
        public String getDescription()
            {
            return DESCRIPTION;
            }

        /**
         * Obtain the bit mask that when applied will return the fractional
         * (right-most) bits that are below this {@link Magnitude} unit. For example,
         * a kilobyte has a mask that includes the least significant 10 bits.
         *
         * @return the mask highlighting the {@link Magnitude}'s fractional bits
         */
        public long getResidualBitMask()
            {
            return BIT_MASK;
            }

        /**
         * Obtain the suffix of the {@link Magnitude}.
         *
         * @return a {@link String}
         */
        public String getSuffix()
            {
            return SUFFIX;
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
            return s.equalsIgnoreCase(SUFFIX) || (Character.toUpperCase(s.charAt(0)) == SUFFIX_CHAR);
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
                return Magnitude.BYTES;
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
         * Cached copy of the values array to avoid garbage creation
         */
        private static final Magnitude[] VALUES = Magnitude.values();

        /**
         * The lowest defined order of {@link Magnitude}.
         */
        public final static Magnitude LOWEST = VALUES[0];

        /**
         * The highest defined order of {@link Magnitude}.
         */
        public final static Magnitude HIGHEST = VALUES[VALUES.length - 1];

        // ----- data members -----------------------------------------------

        /**
         * The bit mask that highlights all of the fractional (right-most)
         * bits that are below this {@link Magnitude} unit. For example, a kilobyte
         * has a mask that includes the least significant 10 bits.
         */
        private final long BIT_MASK;

        /**
         * The number of bytes in a single unit of this magnitude. For
         * example, a kilobyte has 1024 bytes.
         */
        private final long BYTE_COUNT;

        /**
         * The description of this {@link Magnitude}. For example, a kilobyte has the name
         * "kilobyte".
         */
        private final String DESCRIPTION;

        /**
         * The number of bits that a size value would have to be shifted to
         * the right in order to eliminate any fraction of a {@link Magnitude} unit
         * (for example, anything less than a megabyte) and to reduce a size
         * value to a count of {@link Magnitude} units (for example, the number of
         * megabytes); or the number of bits that a number of {@link Magnitude} units
         * (such as a number of megabytes) would have to be shifted to the
         * left in order to convert it to a size value (the number of bytes).
         */
        private final int SHIFT_COUNT;

        /**
         * The suffix that represents this {@link Magnitude}. For example, a
         * kilobyte has the suffix "KB".
         */
        private final String SUFFIX;

        /**
         * The single character abbreviation of the {@link #SUFFIX} (in upper-case).
         */
        private final char SUFFIX_CHAR;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The pre-compiled regular expression {@link Pattern} to match
     * a {@link MemorySize} specified as a {@link String}.
     */
    private static final Pattern REGEX_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)([kKMmGgTtPpEe]?[Bb]?)");

    // ----- data members ---------------------------------------------------

    /**
     * The number of bytes in the {@link MemorySize}.
     */
    private long m_cBytes;
    }
