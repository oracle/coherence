/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.search;

import com.oracle.coherence.ai.QueryResult;
import com.tangosol.util.Binary;
import com.tangosol.util.Converter;

import java.util.Objects;

/**
 * A wrapper around a {@link BinaryQueryResult} that can convert
 * the result to a different type.
 *
 * @param <K>  the type of the vector keys
 * @param <V>  the type of the query result
 */
@SuppressWarnings("unchecked")
public class ConverterResult<K, V>
        implements QueryResult<K, V>
    {
    /**
     * Create a {@link ConverterResult}.
     *
     * @param wrapped    the {@link BinaryQueryResult} to convert
     * @param converter  the {@link Converter} to convert binary serialized values to Object form
     */
    public ConverterResult(BinaryQueryResult wrapped, Converter<Binary, ?> converter)
        {
        this.wrapped   = Objects.requireNonNull(wrapped);
        this.converter = Objects.requireNonNull(converter);
        }

    /**
     * Return the wrapped {@link BinaryQueryResult}.
     *
     * @return the wrapped {@link BinaryQueryResult}
     */
    public BinaryQueryResult getBinaryQueryResult()
        {
        return wrapped;
        }

    /**
     * Return the {@link Converter} used to convert serialized binary
     * data to Object format data.
     *
     * @return the {@link Converter} used to convert serialized binary
     *         data to Object format data
     */
    public Converter<Binary, ?> getConverter()
        {
        return converter;
        }

    @Override
    public double getDistance()
        {
        return wrapped.getDistance();
        }

    @Override
    public K getKey()
        {
        Binary binKey = wrapped.getKey();
        return binKey == null ? null : (K) converter.convert(binKey);
        }

    @Override
    public V getValue()
        {
        Binary binValue = wrapped.getValue();
        return binValue == null ? null : (V) converter.convert(binValue);
        }

    @Override
    public String toString()
        {
        Object oKey = getKey();
        return "ConverterResult{" +
                " result=" + getDistance() +
                ", key=" + oKey +
                '}';
        }

    // ----- data members ---------------------------------------------------

    /**
     * The wrapped {@link BinaryQueryResult}.
     */
    protected final BinaryQueryResult wrapped;

    /**
     * The {@link Converter} that can deserialize binary data.
     */
    protected final Converter<Binary, ?> converter;
    }
