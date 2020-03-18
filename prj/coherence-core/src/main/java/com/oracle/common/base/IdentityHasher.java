/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.base;


import com.oracle.coherence.common.base.Hasher;

/**
 * IdentityHasher provides a Hasher implementation based upon an object's
 * {@link System#identityHashCode(Object) identity hashCode} and reference
 * equality.
 *
 * @param <V>  the value type
 *
 * @author mf  2011.01.07
 * @deprecated use {@link com.oracle.coherence.common.base.IdentityHasher} instead
 */
@Deprecated
public class IdentityHasher<V>
        extends com.oracle.coherence.common.base.IdentityHasher<V>
        implements Hasher<V>
    {
    // ----- constants ------------------------------------------------------

    /**
     * A singleton instance of the IdentityHasher.
     */
    public static final IdentityHasher INSTANCE = new IdentityHasher();
    }
