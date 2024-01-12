/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.base;

/**
 * A {@link Nullable} wrapper for integer values.
 *
 * @since 24.03
 * @author Aleks Seovic  2024.01.08
 */
class NullableInt
        implements Nullable<Integer>
    {
    // ---- constructor -----------------------------------------------------

    NullableInt(int value)
        {
        f_nValue = value;
        }

    // ---- Nullable interface ----------------------------------------------

    public Integer get()
        {
        return f_nValue;
        }

    // ---- Object methods --------------------------------------------------

    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o instanceof NullableInt)
            {
            NullableInt nullable = (NullableInt) o;
            return f_nValue == nullable.f_nValue;
            }
        return false;
        }

    public int hashCode()
        {
        return Integer.hashCode(f_nValue);
        }

    public String toString()
        {
        return Integer.toString(f_nValue);
        }

    // ---- data members ----------------------------------------------------

    private final int f_nValue;
    }
