/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.base;

/**
 * A {@link Nullable} wrapper for double values.
 *
 * @since 24.03
 * @author Aleks Seovic  2024.01.08
 */
class NullableDouble
        implements Nullable<Double>
    {
    // ---- constructor -----------------------------------------------------

    NullableDouble(double value)
        {
        f_dblValue = value;
        }

    // ---- Nullable interface ----------------------------------------------

    public Double get()
        {
        return f_dblValue;
        }

    // ---- Object methods --------------------------------------------------

    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o instanceof NullableDouble)
            {
            NullableDouble nullable = (NullableDouble) o;
            return f_dblValue == nullable.f_dblValue;
            }
        return false;
        }

    public int hashCode()
        {
        return Double.hashCode(f_dblValue);
        }

    public String toString()
        {
        return Double.toString(f_dblValue);
        }

    // ---- data members ----------------------------------------------------

    private final double f_dblValue;
    }
