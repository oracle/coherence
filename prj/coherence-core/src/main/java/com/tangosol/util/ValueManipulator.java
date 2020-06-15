/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


/**
* ValueManipulator represents a composition of {@link ValueExtractor} and
* {@link ValueUpdater} implementations.
*
* @param <T>  the type of object
* @param <V>  the type of value that will be extracted/updated from/on object
*
* @author gg 2005.10.31
* @since Coherence 3.1
*/
public interface ValueManipulator<T, V>
    {
    /**
    * Retrieve the underlying ValueExtractor reference.
    *
    * @return the ValueExtractor
    */
    public ValueExtractor<T, V> getExtractor();

    /**
    * Retrieve the underlying ValueUpdater reference.
    *
    * @return the ValueUpdater
    */
    public ValueUpdater<T, V> getUpdater();
    }