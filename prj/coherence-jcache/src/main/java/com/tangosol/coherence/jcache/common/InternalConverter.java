/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.common;

/**
 * Converts values of a specified type to and from an internal representation.
 * <p>
 * {@link InternalConverter}s are typically used convert cache keys and values
 * to and from an appropriate internal representation.
 * <p>
 * The internal representation is declared as an Object as the type is typically
 * unknown until runtime.
 *
 * @param <T> the type of value to convert
 *
 * @author bo  2013.11.24
 * @since Coherence 12.1.3
 */

public interface InternalConverter<T>
    {
    /**
     * Converts a value to an internal representation.
     *
     * @param value  the value to convert
     * @return  an internal representation of the value
     */
    Object toInternal(T value);

    /**
     * Converts an internal representation of a value to a value.
     *
     * @param object  the internal representation of the value
     * @return the value
     */
    T fromInternal(Object object);
    }
