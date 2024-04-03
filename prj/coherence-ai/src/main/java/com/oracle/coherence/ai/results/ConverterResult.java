/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.results;

import com.oracle.coherence.ai.Converters;
import com.oracle.coherence.ai.QueryResult;

import com.tangosol.io.ReadBuffer;

import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.NullImplementation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A wrapper around a {@link BinaryQueryResult} that can convert
 * the result to a different type.
 *
 * @param <K>  the type of the vector keys
 * @param <M>  the type of the vector metadata
 * @param <R>  they type of the vector
 */
@SuppressWarnings("unchecked")
public abstract class ConverterResult<K, M, R>
        implements QueryResult<R, K, M>
    {
    protected ConverterResult(BinaryQueryResult wrapped, Converter<Binary, Object> converter)
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
    public Converter<Binary, Object> getConverter()
        {
        return converter;
        }

    @Override
    public float getResult()
        {
        return wrapped.getResult();
        }

    @Override
    public Optional<K> getKey()
        {
        return wrapped.getKey().map(binary -> (K) converter.convert(binary));
        }

    @Override
    public Optional<ReadBuffer> getBinaryVector()
        {
        return wrapped.getBinaryVector();
        }

    @Override
    public Optional<M> getMetadata()
        {
        return wrapped.getMetadata().map(binary -> (M) converter.convert(binary));
        }


    // ----- factory methods ------------------------------------------------

    /**
     * Return a converter that will convert a {@link BinaryQueryResult} instance
     * to a {@link QueryResult} of a {@code double} vector.
     *
     * @param converter  the {@link Converter} to use to deserialize data
     * @param <K>        the type of the keys used to identify vectors
     * @param <M>        the type of the vector metadata
     *
     * @return a converter that will convert a {@link BinaryQueryResult} instance
     *         to a {@link QueryResult} of a {@code double} vector
     */
    public static <K, M> Converter<BinaryQueryResult, QueryResult<double[], K, M>>
            doubleResultConverter(Converter<Binary, Object> converter)
        {
        return result -> forDoubles(result, converter);
        }

    /**
     * Converter a {@link BinaryQueryResult} instance to a
     * {@link QueryResult} of a {@code double} vector.
     *
     * @param wrapped    the {@link BinaryQueryResult} to convert
     * @param converter  the {@link Converter} to use to deserialize data
     * @param <K>        the type of the keys used to identify vectors
     * @param <M>        the type of the vector metadata
     *
     * @return a {@link QueryResult} of a {@code double} vector
     */
    public static <K, M> QueryResult<double[], K, M> forDoubles(BinaryQueryResult wrapped, Converter<Binary, Object> converter)
        {
        return new DoubleConverterResult<>(wrapped, converter);
        }

    /**
     * Converter a {@link List} of {@link BinaryQueryResult} instances to a
     * {@link List} of {@link QueryResult} instances for {@code double} vectors.
     *
     * @param wrapped    the {@link List} of {@link BinaryQueryResult} instances to convert
     * @param converter  the {@link Converter} to use to deserialize data
     * @param <K>        the type of the keys used to identify vectors
     * @param <M>        the type of the vector metadata
     *
     * @return a {@link List} of {@link BinaryQueryResult} instances to a
     *         {@link List} of {@link QueryResult} instances for
     *         {@code double} vectors
     */
    public static <K, M> List<QueryResult<double[], K, M>> listOfDoubleResults(List<BinaryQueryResult> wrapped, Converter<Binary, Object> converter)
        {
        return ConverterCollections.getList(wrapped,
                ConverterResult.doubleResultConverter(converter),
                NullImplementation.getConverter());
        }

    /**
     * Return a converter that will convert a {@link BinaryQueryResult} instance
     * to a {@link QueryResult} of a {@code float} vector.
     *
     * @param converter  the {@link Converter} to use to deserialize data
     * @param <K>        the type of the keys used to identify vectors
     * @param <M>        the type of the vector metadata
     *
     * @return a converter that will convert a {@link BinaryQueryResult} instance
     *         to a {@link QueryResult} of a {@code float} vector
     */
    public static <K, M> Converter<BinaryQueryResult, QueryResult<float[], K, M>>
            floatResultConverter(Converter<Binary, Object> converter)
        {
        return result -> forFloats(result, converter);
        }

    /**
     * Converter a {@link BinaryQueryResult} instance to a
     * {@link QueryResult} of a {@code float} vector.
     *
     * @param wrapped    the {@link BinaryQueryResult} to convert
     * @param converter  the {@link Converter} to use to deserialize data
     * @param <K>        the type of the keys used to identify vectors
     * @param <M>        the type of the vector metadata
     *
     * @return a {@link QueryResult} of a {@code float} vector
     */
    public static <K, M> QueryResult<float[], K, M> forFloats(BinaryQueryResult wrapped, Converter<Binary, Object> converter)
        {
        return new FloatConverterResult<>(wrapped, converter);
        }

    /**
     * Converter a {@link List} of {@link BinaryQueryResult} instances to a
     * {@link List} of {@link QueryResult} instances for {@code float} vectors.
     *
     * @param wrapped    the {@link List} of {@link BinaryQueryResult} instances to convert
     * @param converter  the {@link Converter} to use to deserialize data
     * @param <K>        the type of the keys used to identify vectors
     * @param <M>        the type of the vector metadata
     *
     * @return a {@link List} of {@link BinaryQueryResult} instances to a
     *         {@link List} of {@link QueryResult} instances for
     *         {@code float} vectors
     */
    public static <K, M> List<QueryResult<float[], K, M>> listOfFloatResults(List<BinaryQueryResult> wrapped, Converter<Binary, Object> converter)
        {
        return ConverterCollections.getList(wrapped,
                ConverterResult.floatResultConverter(converter),
                NullImplementation.getConverter());
        }

    /**
     * Return a converter that will convert a {@link BinaryQueryResult} instance
     * to a {@link QueryResult} of a {@code int} vector.
     *
     * @param converter  the {@link Converter} to use to deserialize data
     * @param <K>        the type of the keys used to identify vectors
     * @param <M>        the type of the vector metadata
     *
     * @return a converter that will convert a {@link BinaryQueryResult} instance
     *         to a {@link QueryResult} of a {@code int} vector
     */
    public static <K, M> Converter<BinaryQueryResult, QueryResult<int[], K, M>>
            intResultConverter(Converter<Binary, Object> converter)
        {
        return result -> forInts(result, converter);
        }

    /**
     * Converter a {@link BinaryQueryResult} instance to a
     * {@link QueryResult} of a {@code int} vector.
     *
     * @param wrapped    the {@link BinaryQueryResult} to convert
     * @param converter  the {@link Converter} to use to deserialize data
     * @param <K>        the type of the keys used to identify vectors
     * @param <M>        the type of the vector metadata
     *
     * @return a {@link QueryResult} of a {@code int} vector
     */
    public static <K, M> QueryResult<int[], K, M> forInts(BinaryQueryResult wrapped, Converter<Binary, Object> converter)
        {
        return new IntConverterResult<>(wrapped, converter);
        }

    /**
     * Converter a {@link List} of {@link BinaryQueryResult} instances to a
     * {@link List} of {@link QueryResult} instances for {@code int} vectors.
     *
     * @param wrapped    the {@link List} of {@link BinaryQueryResult} instances to convert
     * @param converter  the {@link Converter} to use to deserialize data
     * @param <K>        the type of the keys used to identify vectors
     * @param <M>        the type of the vector metadata
     *
     * @return a {@link List} of {@link BinaryQueryResult} instances to a
     *         {@link List} of {@link QueryResult} instances for
     *         {@code int} vectors
     */
    public static <K, M> List<QueryResult<int[], K, M>> listOfIntResults(List<BinaryQueryResult> wrapped, Converter<Binary, Object> converter)
        {
        return ConverterCollections.getList(wrapped,
                ConverterResult.intResultConverter(converter),
                NullImplementation.getConverter());
        }

    /**
     * Return a converter that will convert a {@link BinaryQueryResult} instance
     * to a {@link QueryResult} of a {@code long} vector.
     *
     * @param converter  the {@link Converter} to use to deserialize data
     * @param <K>        the type of the keys used to identify vectors
     * @param <M>        the type of the vector metadata
     *
     * @return a converter that will convert a {@link BinaryQueryResult} instance
     *         to a {@link QueryResult} of a {@code long} vector
     */
    public static <K, M> Converter<BinaryQueryResult, QueryResult<long[], K, M>>
            longResultConverter(Converter<Binary, Object> converter)
        {
        return result -> forLongs(result, converter);
        }

    /**
     * Converter a {@link BinaryQueryResult} instance to a
     * {@link QueryResult} of a {@code long} vector.
     *
     * @param wrapped    the {@link BinaryQueryResult} to convert
     * @param converter  the {@link Converter} to use to deserialize data
     * @param <K>        the type of the keys used to identify vectors
     * @param <M>        the type of the vector metadata
     *
     * @return a {@link QueryResult} of a {@code long} vector
     */
    public static <K, M> QueryResult<long[], K, M> forLongs(BinaryQueryResult wrapped, Converter<Binary, Object> converter)
        {
        return new LongConverterResult<>(wrapped, converter);
        }

    /**
     * Converter a {@link List} of {@link BinaryQueryResult} instances to a
     * {@link List} of {@link QueryResult} instances for {@code long} vectors.
     *
     * @param wrapped    the {@link List} of {@link BinaryQueryResult} instances to convert
     * @param converter  the {@link Converter} to use to deserialize data
     * @param <K>        the type of the keys used to identify vectors
     * @param <M>        the type of the vector metadata
     *
     * @return a {@link List} of {@link BinaryQueryResult} instances to a
     *         {@link List} of {@link QueryResult} instances for
     *         {@code long} vectors
     */
    public static <K, M> List<QueryResult<long[], K, M>> listOfLongResults(List<BinaryQueryResult> wrapped, Converter<Binary, Object> converter)
        {
        return ConverterCollections.getList(wrapped,
                ConverterResult.longResultConverter(converter),
                NullImplementation.getConverter());
        }

    /**
     * Return a converter that will convert a {@link BinaryQueryResult} instance
     * to a {@link QueryResult} of a {@code short} vector.
     *
     * @param converter  the {@link Converter} to use to deserialize data
     * @param <K>        the type of the keys used to identify vectors
     * @param <M>        the type of the vector metadata
     *
     * @return a converter that will convert a {@link BinaryQueryResult} instance
     *         to a {@link QueryResult} of a {@code short} vector
     */
    public static <K, M> Converter<BinaryQueryResult, QueryResult<short[], K, M>>
            shortResultConverter(Converter<Binary, Object> converter)
        {
        return result -> forShorts(result, converter);
        }

    /**
     * Converter a {@link BinaryQueryResult} instance to a
     * {@link QueryResult} of a {@code short} vector.
     *
     * @param wrapped    the {@link BinaryQueryResult} to convert
     * @param converter  the {@link Converter} to use to deserialize data
     * @param <K>        the type of the keys used to identify vectors
     * @param <M>        the type of the vector metadata
     *
     * @return a {@link QueryResult} of a {@code short} vector
     */
    public static <K, M> QueryResult<short[], K, M> forShorts(BinaryQueryResult wrapped, Converter<Binary, Object> converter)
        {
        return new ShortConverterResult<>(wrapped, converter);
        }

    /**
     * Converter a {@link List} of {@link BinaryQueryResult} instances to a
     * {@link List} of {@link QueryResult} instances for {@code short} vectors.
     *
     * @param wrapped    the {@link List} of {@link BinaryQueryResult} instances to convert
     * @param converter  the {@link Converter} to use to deserialize data
     * @param <K>        the type of the keys used to identify vectors
     * @param <M>        the type of the vector metadata
     *
     * @return a {@link List} of {@link BinaryQueryResult} instances to a
     *         {@link List} of {@link QueryResult} instances for
     *         {@code short} vectors
     */
    public static <K, M> List<QueryResult<short[], K, M>> listOfShortResults(List<BinaryQueryResult> wrapped, Converter<Binary, Object> converter)
        {
        return ConverterCollections.getList(wrapped,
                ConverterResult.shortResultConverter(converter),
                NullImplementation.getConverter());
        }

    // ----- inner class DoubleConverterResult ------------------------------

    /**
     * A {@link ConverterResult} to convert {@code double} vector results.
     *
     * @param <K>  the type of the vector keys
     * @param <M>  the type of the vector metadata
     */
    protected static class DoubleConverterResult<K, M>
            extends ConverterResult<K, M, double[]>
        {
        /**
         * Create a {@link DoubleConverterResult}.
         *
         * @param wrapped    the {@link BinaryQueryResult} to convert
         * @param converter  the {@link Converter} to use to deserialize data
         */
        public DoubleConverterResult(BinaryQueryResult wrapped, Converter<Binary, Object> converter)
            {
            super(wrapped, converter);
            }

        @Override
        public Optional<double[]> getVector()
            {
            return wrapped.getBinaryVector().map(Converters::doublesFromReadBuffer);
            }
        }

    // ----- inner class FloatConverterResult -------------------------------

    /**
     * A {@link ConverterResult} to convert {@code float} vector results.
     *
     * @param <K>  the type of the vector keys
     * @param <M>  the type of the vector metadata
     */
    protected static class FloatConverterResult<K, M>
            extends ConverterResult<K, M, float[]>
        {
        /**
         * Create a {@link FloatConverterResult}.
         *
         * @param wrapped    the {@link BinaryQueryResult} to convert
         * @param converter  the {@link Converter} to use to deserialize data
         */
        public FloatConverterResult(BinaryQueryResult wrapped, Converter<Binary, Object> converter)
            {
            super(wrapped, converter);
            }

        @Override
        public Optional<float[]> getVector()
            {
            return wrapped.getBinaryVector().map(Converters::floatsFromReadBuffer);
            }
        }

    // ----- inner class IntConverterResult ---------------------------------

    /**
     * A {@link ConverterResult} to convert {@code int} vector results.
     *
     * @param <K>  the type of the vector keys
     * @param <M>  the type of the vector metadata
     */
    protected static class IntConverterResult<K, M>
            extends ConverterResult<K, M, int[]>
        {
        /**
         * Create a {@link IntConverterResult}.
         *
         * @param wrapped    the {@link BinaryQueryResult} to convert
         * @param converter  the {@link Converter} to use to deserialize data
         */
        public IntConverterResult(BinaryQueryResult wrapped, Converter<Binary, Object> converter)
            {
            super(wrapped, converter);
            }

        @Override
        public Optional<int[]> getVector()
            {
            return wrapped.getBinaryVector().map(Converters::intsFromReadBuffer);
            }
        }

    // ----- inner class LongConverterResult -------------------------------0

    /**
     * A {@link ConverterResult} to convert {@code long} vector results.
     *
     * @param <K>  the type of the vector keys
     * @param <M>  the type of the vector metadata
     */
    protected static class LongConverterResult<K, M>
            extends ConverterResult<K, M, long[]>
        {
        /**
         * Create a {@link LongConverterResult}.
         *
         * @param wrapped    the {@link BinaryQueryResult} to convert
         * @param converter  the {@link Converter} to use to deserialize data
         */
        public LongConverterResult(BinaryQueryResult wrapped, Converter<Binary, Object> converter)
            {
            super(wrapped, converter);
            }

        @Override
        public Optional<long[]> getVector()
            {
            return wrapped.getBinaryVector().map(Converters::longsFromReadBuffer);
            }
        }

    // ----- inner class ShortConverterResult -------------------------------

    /**
     * A {@link ConverterResult} to convert {@code short} vector results.
     *
     * @param <K>  the type of the vector keys
     * @param <M>  the type of the vector metadata
     */
    protected static class ShortConverterResult<K, M>
            extends ConverterResult<K, M, short[]>
        {
        /**
         * Create a {@link ShortConverterResult}.
         *
         * @param wrapped    the {@link BinaryQueryResult} to convert
         * @param converter  the {@link Converter} to use to deserialize data
         */
        public ShortConverterResult(BinaryQueryResult wrapped, Converter<Binary, Object> converter)
            {
            super(wrapped, converter);
            }

        @Override
        public Optional<short[]> getVector()
            {
            return wrapped.getBinaryVector().map(Converters::shortsFromReadBuffer);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The wrapped {@link BinaryQueryResult}.
     */
    protected final BinaryQueryResult wrapped;

    /**
     * The {@link Converter} that can deserialize binary data.
     */
    protected final Converter<Binary, Object> converter;
    }
