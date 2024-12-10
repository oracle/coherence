/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.common;

import com.tangosol.util.Converter;

/**
 * An {@link InternalConverter} that uses two Coherence {@link Converter}s
 * as a means of performing conversions to and from internal representations.
 *
 * @param <T> the type of value to convert
 *
 * @author jf  2013.10.24
 * @since Coherence 12.1.3
 */
public class CoherenceConverterAdapter<T>
        implements InternalConverter<T>
    {
    // ----- constructors --------------------------------------------------

    /**
     * Constructs a {@link CoherenceConverterAdapter} given two
     * Coherence {@link Converter}s.
     *
     * @param fromConverter  a {@link Converter} to convert a value from an internal representation
     * @param toConverter    a {@link Converter} to convert a value to an internal representation
     */
    public CoherenceConverterAdapter(Converter fromConverter, Converter toConverter)
        {
        f_fromConverter = fromConverter;
        f_toConverter   = toConverter;
        }

    // ----- InternalConverter interface ------------------------------------

    @Override
    public Object toInternal(T value)
        {
        return f_toConverter.convert(value);
        }

    @Override
    public T fromInternal(Object object)
        {
        return (T) f_fromConverter.convert(object);
        }

    // ----- data members ---------------------------------------------------

    /**
     * A {@link Converter} to convert a value to an internal representation.
     */
    private final Converter f_toConverter;

    /**
     * A {@link Converter} to convert a value from an internal representation.
     */
    private final Converter f_fromConverter;
    }
