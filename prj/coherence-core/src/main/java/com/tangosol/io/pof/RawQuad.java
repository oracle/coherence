/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.util.Binary;

import java.math.BigInteger;


/**
* An immutable POF 128-bit float.
*
* @author cp  2006.07.17
*
* @since Coherence 3.2
*/
public class RawQuad
        extends Number
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a RawQuad from the raw binary data.
    *
    * @param binBits  the raw binary form of the 128-bit float
    */
    public RawQuad(Binary binBits)
        {
        if (binBits.length() != 16)
            {
            throw new IllegalArgumentException("length != 128-bits (16-bytes)");
            }

        m_binBits = binBits;
        }

    /**
    * Construct a RawQuad from a double.
    *
    * @param dfl  the double value
    */
    public RawQuad(double dfl)
        {
        // TODO
        m_binBits = new Binary(new byte[16]);
        }

    /**
    * Construct a RawQuad from an unscaled integer value and a scale.
    *
    * @param nUnscaledValue  the unscaled value (mantissa)
    * @param nScale          the scale (exponent)
    */
    public RawQuad(BigInteger nUnscaledValue, int nScale)
        {
        // TODO
        m_binBits = new Binary(new byte[16]);
        }


    // ----- Number methods -------------------------------------------------

    /**
    * Returns the value of the specified number as an <code>int</code>.
    * This may involve rounding or truncation.
    *
    * @return  the numeric value represented by this object after conversion
    *          to type <code>int</code>.
    */
    public int intValue()
        {
        return (int) longValue();
        }

    /**
    * Returns the value of the specified number as a <code>long</code>.
    * This may involve rounding or truncation.
    *
    * @return  the numeric value represented by this object after conversion
    *          to type <code>long</code>.
    */
    public long longValue()
        {
        // TODO
        throw new UnsupportedOperationException();
        }

    /**
    * Returns the value of the specified number as a <code>float</code>.
    * This may involve rounding.
    *
    * @return  the numeric value represented by this object after conversion
    *          to type <code>float</code>.
    */
    public float floatValue()
        {
        return (float) doubleValue();
        }

    /**
    * Returns the value of the specified number as a <code>double</code>.
    * This may involve rounding.
    *
    * @return  the numeric value represented by this object after conversion
    *          to type <code>double</code>.
    */
    public double doubleValue()
        {
        // TODO
        throw new UnsupportedOperationException();
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the raw binary form of the 128-bit float.
    *
    * @return a 16-byte binary
    */
    public Binary getBits()
        {
        return m_binBits;
        }

    /**
    * Get the base-2 unscaled value (mantissa) of the quad value.
    * <p>
    * The name of this method is based on Java's BigDecimal.
    *
    * @return an unscaled binary integer value of up to 113 binary digits
    */
    public BigInteger unscaledValue()
        {
        // TODO
        throw new UnsupportedOperationException();
        }

    /**
    * Get the base-2 scale (exponent) of the quad value.
    * <p>
    * The name of this method is based on Java's BigDecimal.
    *
    * @return a binary exponent between -16382 to 16383 inclusive
    */
    public int scale()
        {
        // TODO
        throw new UnsupportedOperationException();
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare this object with another for equality.
    *
    * @param o  another object to compare to for equality
    *
    * @return true iff this object is equal to the other object
    */
    public boolean equals(Object o)
        {
        if (o instanceof RawQuad)
            {
            RawQuad that = (RawQuad) o;
            return this == that
                   || this.getBits().equals(that.getBits());
            }

        return false;
        }

    /**
    * Obtain the hashcode for this object.
    *
    * @return an integer hashcode
    */
    public int hashCode()
        {
        return getBits().hashCode();
        }

    /**
    * Format this object's data as a human-readable string.
    *
    * @return a string description of this object
    */
    public String toString()
        {
        // TODO
        return "quad=" + getBits().toString();
        }


    // ----- constants ------------------------------------------------------

    /**
    * An empty RawQuad value.
    */
    public static final RawQuad ZERO = new RawQuad(0.0);


    // ----- data members ---------------------------------------------------

    /**
    * The raw form of the 128-bit float.
    */
    private Binary m_binBits;
    }
