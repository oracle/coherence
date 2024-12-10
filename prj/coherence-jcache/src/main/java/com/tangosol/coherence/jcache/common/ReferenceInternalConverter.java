/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.common;

/**
 * An {@link InternalConverter} that simply returns a reference to the
 * provided value.
 *
 * @param <T>  the type of values to convert
 *
 * @author bo  2013.11.24
 * @since Coherence 12.1.3
 */
public class ReferenceInternalConverter<T>
        implements InternalConverter<T>
    {
    // ----- InternalConverter interface ------------------------------------

    @Override
    public T fromInternal(Object value)
        {
        return (T) value;
        }

    @Override
    public Object toInternal(T value)
        {
        return value;
        }
    }
