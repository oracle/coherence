/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;


/**
 * A Hasher provides an external means for producing hash codes and comparing
 * objects for equality.
 *
 * @author mf  2011.01.07
 */
public interface Hasher<V>
    {
    /**
     * Return a hash for the specified object.
     *
     * @param v  the object to hash
     *
     * @return the hash
     */
    public int hashCode(V v);

    /**
     * Compare two objects for equality.
     *
     * @param va  the first object to compare
     * @param vb  the second object to compare
     *
     * @return true iff the object are equal
     */
    public boolean equals(V va, V vb);


    // ---- static methods ---------------------------------------------------

    /**
     * Calculate a modulo of two integer numbers. For a positive dividend the
     * result is the same as the Java remainder operation (<code>n % m</code>).
     * For a negative dividend the result is still positive and equals to
     * (<code>n % m + m</code>).
     *
     * @param n  the dividend
     * @param m  the divisor (must be positive)
     *
     * @return the modulo
     */
    public static int mod(int n, int m)
        {
        int k = n % m;
        return k >= 0 ? k : k + m;
        }

    /**
     * Calculate a modulo of two long numbers. For a positive dividend the
     * result is the same as the Java remainder operation (<code>n % m</code>).
     * For a negative dividend the result is still positive and equals to
     * (<code>n % m + m</code>).
     *
     * @param n  the dividend
     * @param m  the divisor (must be positive)
     *
     * @return the modulo
     */
    public static long mod(long n, long m)
        {
        long k = n % m;
        return k >= 0 ? k : k + m;
        }
    }
