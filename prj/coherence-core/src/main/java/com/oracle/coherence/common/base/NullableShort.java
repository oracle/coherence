/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.base;

/**
 * A {@link Nullable} wrapper for short values.
 *
 * @since 24.03
 * @author Aleks Seovic  2024.01.08
 */
class NullableShort
        implements Nullable<Short>
    {
    // ---- constructor -----------------------------------------------------

    NullableShort(short value)
        {
        f_nValue = value;
        }

    // ---- Nullable interface ----------------------------------------------

    public Short get()
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
        if (o instanceof NullableShort)
            {
            NullableShort nullable = (NullableShort) o;
            return f_nValue == nullable.f_nValue;
            }
        return false;
        }

    public int hashCode()
        {
        return Short.hashCode(f_nValue);
        }

    public String toString()
        {
        return Short.toString(f_nValue);
        }

    // ---- data members ----------------------------------------------------

    private final short f_nValue;
    }
