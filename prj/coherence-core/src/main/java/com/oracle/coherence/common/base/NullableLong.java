/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.base;

/**
 * A {@link Nullable} wrapper for long values.
 *
 * @since 24.03
 * @author Aleks Seovic  2024.01.08
 */
class NullableLong
        implements Nullable<Long>
    {
    // ---- constructor -----------------------------------------------------

    NullableLong(long value)
        {
        f_lValue = value;
        }

    // ---- Nullable interface ----------------------------------------------

    public Long get()
        {
        return f_lValue;
        }

    // ---- Object methods --------------------------------------------------

    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o instanceof NullableLong)
            {
            NullableLong nullable = (NullableLong) o;
            return f_lValue == nullable.f_lValue;
            }
        return false;
        }

    public int hashCode()
        {
        return Long.hashCode(f_lValue);
        }

    public String toString()
        {
        return Long.toString(f_lValue);
        }

    // ---- data members ----------------------------------------------------

    private final long f_lValue;
    }
