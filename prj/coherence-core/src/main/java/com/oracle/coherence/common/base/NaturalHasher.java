/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;


/**
 * NaturalHasher provides a Hasher implementation based upon an object's
 * internal {@link Object#hashCode hashCode} and {@link Object#equals equals}
 * implementation.
 *
 * @param <V>  the value type
 *
 * @author mf  2011.01.07
 */
public class NaturalHasher<V>
        implements Hasher<V>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode(V o)
        {
        return o == null ? 0 : o.hashCode();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(V va, V vb)
        {
        return va == vb || (va != null && va.equals(vb));
        }


    // ----- constants ------------------------------------------------------

    /**
     * A singleton instance of the NaturalHasher.
     */
    public static final NaturalHasher INSTANCE = new NaturalHasher();
    }
