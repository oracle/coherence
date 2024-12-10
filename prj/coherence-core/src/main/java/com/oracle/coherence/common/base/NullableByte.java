/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.base;

/**
 * A {@link Nullable} wrapper for byte values.
 *
 * @since 24.03
 * @author Aleks Seovic  2024.01.08
 */
class NullableByte
        implements Nullable<Byte>
    {
    // ---- constructor -----------------------------------------------------

    NullableByte(byte value)
        {
        f_bValue = value;
        }

    // ---- Nullable interface ----------------------------------------------

    public Byte get()
        {
        return f_bValue;
        }

    // ---- Object methods --------------------------------------------------

    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o instanceof NullableByte)
            {
            NullableByte nullable = (NullableByte) o;
            return f_bValue == nullable.f_bValue;
            }
        return false;
        }

    public int hashCode()
        {
        return Byte.hashCode(f_bValue);
        }

    public String toString()
        {
        return Byte.toString(f_bValue);
        }

    // ---- data members ----------------------------------------------------

    private final byte f_bValue;
    }
