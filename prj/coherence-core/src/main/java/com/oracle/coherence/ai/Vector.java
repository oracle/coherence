/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PortableObject;

/**
 * A representation of a vector.
 *
 * @param <T>  the type of the vector
 */
public sealed interface Vector<T>
        extends ExternalizableLite, PortableObject
        permits BitVector, Int8Vector, Float32Vector
    {
    /**
     * Returns the number of dimensions in this vector.
     *
     * @return the number of dimensions in this vector
     */
    int dimensions();

    /**
     * Return the vector.
     * <p/>
     * Mutating the returned vector will result in changes to the internal
     * state of this vector.
     *
     * @return the actual wrapped vector
     */
    T get();

    /**
     * Return binary quantized value for this {@link Vector} as a bit vector.
     *
     * @return binary quantized value for this {@link Vector} as a bit vector
     */
    BitVector binaryQuant();
    }
