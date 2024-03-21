/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.base;

/**
 * A {@link Nullable} wrapper for float values.
 *
 * @since 24.03
 * @author Aleks Seovic  2024.01.08
 */
class NullableFloat
        implements Nullable<Float>
    {
    // ---- constructor -----------------------------------------------------

    NullableFloat(float value)
        {
        f_fltValue = value;
        }

    // ---- Nullable interface ----------------------------------------------

    public Float get()
        {
        return f_fltValue;
        }

    // ---- Object methods --------------------------------------------------

    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o instanceof NullableFloat)
            {
            NullableFloat nullable = (NullableFloat) o;
            return f_fltValue == nullable.f_fltValue;
            }
        return false;
        }

    public int hashCode()
        {
        return Float.hashCode(f_fltValue);
        }

    public String toString()
        {
        return Float.toString(f_fltValue);
        }

    // ---- data members ----------------------------------------------------

    private final float f_fltValue;
    }
