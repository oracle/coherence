/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.base;

/**
 * An empty {@link Nullable} value.
 *
 * @since 24.03
 * @author Aleks Seovic  2024.01.08
 */
class NullableEmpty<T>
        implements Nullable<T>
    {
    // ---- Nullable interface ----------------------------------------------

    public T get()
        {
        return null;
        }

    // ---- Object methods --------------------------------------------------

    public int hashCode()
        {
        return 0;
        }

    public boolean equals(Object obj)
        {
        return this == obj;
        }

    public String toString()
        {
        return null;
        }

    // ---- singleton -------------------------------------------------------

    static final Nullable<?> INSTANCE = new NullableEmpty<>();
    }
