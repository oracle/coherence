/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;


/**
 * IdentityHasher provides a Hasher implementation based upon an object's
 * {@link System#identityHashCode(Object) identity hashCode} and reference
 * equality.
 *
 * @param <V>  the value type
 *
 * @author mf  2011.01.07
 */
public class IdentityHasher<V>
        implements Hasher<V>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode(V o)
        {
        return System.identityHashCode(o);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(V va, V vb)
        {
        return va == vb;
        }


    // ----- constants ------------------------------------------------------

    /**
     * A singleton instance of the IdentityHasher.
     */
    public static final IdentityHasher INSTANCE = new IdentityHasher();
    }
