/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.base;

/**
 * NaturalHasher provides a Hasher implementation based upon an object's
 * internal {@link Object#hashCode hashCode} and {@link Object#equals equals}
 * implementation.
 *
 * @param <V>  the value type
 *
 * @author mf  2011.01.07
 * @deprecated use {@link com.oracle.coherence.common.base.NaturalHasher} instead
 */
@Deprecated
public class NaturalHasher<V>
        extends com.oracle.coherence.common.base.NaturalHasher<V>
        implements Hasher<V>
    {
    // ----- constants ------------------------------------------------------

    /**
     * A singleton instance of the NaturalHasher.
     */
    public static final NaturalHasher INSTANCE = new NaturalHasher();
    }
