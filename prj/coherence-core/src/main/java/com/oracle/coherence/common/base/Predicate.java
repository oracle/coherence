/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;


/**
 * Predicate represents a boolean test of an object.
 *
 * @param <T>  the type of the value to evaluate
 *
 * @author rhl 2011.11.14
 */
public interface Predicate<T>
    {
    /**
     * Return true iff the specified object satisfies the predicate.
     *
     * @param t  the object to evaluate
     *
     * @return true iff the specified object satisfies the predicate
     */
    public boolean evaluate(T t);
    }