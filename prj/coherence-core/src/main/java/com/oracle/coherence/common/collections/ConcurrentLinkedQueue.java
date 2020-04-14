/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.collections;

/**
 * ConcurrentLinkedQueue is a low-contention unbounded Queue implementation.
 * <p>
 * Unlike the java.util.concurrent collections, the ConcurrentLinkedQueue supports <tt>null</tt> elements.
 * Note however that this queue does not support out-of-order element removal, and as such
 * {@link #remove(Object)}, {@link #removeAll(java.util.Collection)}, {@link #retainAll(java.util.Collection)},
 * and {@link java.util.Iterator#remove()} are not supported.
 * </p>
 *
 * @param <V>  the type of the values that will be stored in the queue
 *
 * @author mf 2014.01.09
 */
public class ConcurrentLinkedQueue<V>
        extends java.util.concurrent.ConcurrentLinkedQueue<V>
    {
    // Note: this class is purely retained to conveniently modify the hierarchy
    //       of some internal queue implementations, while currently j.u.c.CLQ
    //       has proven to have better performance
    }