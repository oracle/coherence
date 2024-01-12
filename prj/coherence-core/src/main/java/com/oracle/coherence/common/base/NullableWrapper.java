/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.base;

import java.util.Objects;

/**
 * A {@link Nullable} wrapper for reference values that do not implement
 * {@link Nullable} interface directly.
 *
 * @since 24.03
 * @author Aleks Seovic  2024.01.08
 */
class NullableWrapper<T>
        implements Nullable<T>
    {
    // ---- constructor -----------------------------------------------------

    NullableWrapper(T value)
        {
        f_value = value;
        }

    // ---- Nullable interface ----------------------------------------------

    public T get()
        {
        return f_value;
        }

    // ---- Object methods --------------------------------------------------

    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o instanceof Nullable<?>)
            {
            Nullable<?> nullable = (Nullable<?>) o;
            return Objects.equals(f_value, nullable.get());
            }
        return false;
        }

    public int hashCode()
        {
        return f_value == null ? 0 : f_value.hashCode();
        }

    public String toString()
        {
        return f_value == null ? null : f_value.toString();
        }

    // ---- data members ----------------------------------------------------

    private final T f_value;
    }
