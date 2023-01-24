/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.partitions.books;

import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.ValueExtractor;

/**
 * A {@link ValueExtractor} implementation to allow queries to be made directly
 * against a binary backing map using standard Coherence filters.
 * <p>
 * This class is not serializable and is intended for use inside entry processors
 * or aggregators to query backing maps.
 *
 * @param <T>  the underlying type to extract from after being deserialized
 * @param <E>  the type of the extracted value
 *
 * @author Jonathan Knight 2023.01.14
 * @since 22.06.4
 */
public class BinaryValueExtractor<T, E>
        implements ValueExtractor<Binary, E>
    {
    /**
     * Create a {@link BinaryValueExtractor}.
     *
     * @param delegate   the extractor to delegate to
     * @param converter  the {@link Converter} to convert the {@link Binary} value
     *                   to a value to pass to the delegate {@link ValueExtractor}
     */
    public BinaryValueExtractor(ValueExtractor<T, E> delegate, Converter<Binary, T> converter)
        {
        m_delegate  = delegate;
        m_converter = converter;
        }

    // ----- ValueExtractor -------------------------------------------------

    @Override
    public E extract(Binary target)
        {
        T value = m_converter.convert(target);
        return m_delegate.extract(value);
        }

    @Override
    public int getTarget()
        {
        return m_delegate.getTarget();
        }

    @Override
    public String getCanonicalName()
        {
        return m_delegate.getCanonicalName();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * A factory method to create a {@link BinaryValueExtractor}.
     *
     * @param delegate   the extractor to delegate to
     * @param converter  the {@link Converter} to convert the {@link Binary} value
     *                   to a value to pass to the delegate {@link ValueExtractor}
     *
     * @return the {@link BinaryValueExtractor} that will extract from a {@link Binary} value
     *
     * @param <T>  the underlying type to extract from after being deserialized
     * @param <E>  the type of the extracted value
     */
    public static <T, E> ValueExtractor<Binary, E> of(ValueExtractor<T, E> delegate, Converter<Binary, T> converter)
        {
        return new BinaryValueExtractor<>(delegate, converter);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The delegate {@link ValueExtractor}.
     */
    private final ValueExtractor<T, E> m_delegate;

    /**
     * The {@link Converter} to convert the {@link Binary} value
     * to a value to pass to the delegate {@link ValueExtractor}
     */
    private final Converter<Binary, T> m_converter;
    }
