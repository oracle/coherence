/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.base;

/**
 * A {@link Nullable} wrapper for boolean values.
 *
 * @since 24.03
 * @author Aleks Seovic  2024.01.08
 */
class NullableBoolean
        implements Nullable<Boolean>
    {
    // ---- constructor -----------------------------------------------------

    NullableBoolean(boolean value)
        {
        f_fValue = value;
        }

    // ---- Nullable interface ----------------------------------------------

    public Boolean get()
        {
        return f_fValue;
        }

    // ---- Object methods --------------------------------------------------

    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o instanceof NullableBoolean)
            {
            NullableBoolean nullable = (NullableBoolean) o;
            return f_fValue == nullable.f_fValue;
            }
        return false;
        }

    public int hashCode()
        {
        return Boolean.hashCode(f_fValue);
        }

    public String toString()
        {
        return Boolean.toString(f_fValue);
        }

    // ---- constants -------------------------------------------------------

    static final NullableBoolean TRUE  = new NullableBoolean(true);
    static final NullableBoolean FALSE = new NullableBoolean(false);

    // ---- data members ----------------------------------------------------

    private final boolean f_fValue;
    }
