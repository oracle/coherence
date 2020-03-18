/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.base;


/**
 * A Hasher provides an external means for producing hash codes and comparing
 * objects for equality.
 *
 * @author mf  2011.01.07
 * @deprecated use {@link com.oracle.coherence.common.base.Hasher} instead
 */
@Deprecated
public interface Hasher<V>
        extends com.oracle.coherence.common.base.Hasher<V>
    {

    /**
     * @see com.oracle.coherence.common.base.Hasher#mod(int, int)
     * @deprecated use {@link com.oracle.coherence.common.base.Hasher#mod(int, int)}
     *             instead
     */
    public static int mod(int n, int m)
        {
        return com.oracle.coherence.common.base.Hasher.mod(n, m);
        }

    /**
     * @see com.oracle.coherence.common.base.Hasher#mod(long, long)
     * @deprecated use {@link com.oracle.coherence.common.base.Hasher#mod(long, long)}
     *             instead
     */
    public static long mod(long n, long m)
        {
        return com.oracle.coherence.common.base.Hasher.mod(n, m);
        }
    }
