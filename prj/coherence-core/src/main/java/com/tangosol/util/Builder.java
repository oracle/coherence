/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

/**
 * A {@link Builder} provides a mechanism for realizing a specific class of
 * object as required.
 * <p>
 * This is an abstract representation of a dynamic no-args constructor.
 *
 * @author bo 2012.10.25
 *
 * @param <T>  the type of object to realize
 */
public interface Builder<T>
    {
    /**
     * Realizes an instance of type T.
     *
     * @return a (possibly new) instance of type T
     */
    public T realize();
    }
