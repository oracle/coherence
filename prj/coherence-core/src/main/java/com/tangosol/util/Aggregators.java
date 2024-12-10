/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.internal.util.Daemons;

import com.tangosol.util.aggregator.AsynchronousAggregator;
import com.tangosol.util.aggregator.BigDecimalAverage;
import com.tangosol.util.aggregator.BigDecimalMax;
import com.tangosol.util.aggregator.BigDecimalMin;
import com.tangosol.util.aggregator.BigDecimalSum;
import com.tangosol.util.aggregator.ComparableMax;
import com.tangosol.util.aggregator.ComparableMin;
import com.tangosol.util.aggregator.CompositeAggregator;
import com.tangosol.util.aggregator.Count;
import com.tangosol.util.aggregator.DistinctValues;
import com.tangosol.util.aggregator.DoubleAverage;
import com.tangosol.util.aggregator.DoubleMax;
import com.tangosol.util.aggregator.DoubleMin;
import com.tangosol.util.aggregator.DoubleSum;
import com.tangosol.util.aggregator.GroupAggregator;
import com.tangosol.util.aggregator.LongMax;
import com.tangosol.util.aggregator.LongMin;
import com.tangosol.util.aggregator.LongSum;
import com.tangosol.util.aggregator.ReducerAggregator;
import com.tangosol.util.aggregator.ScriptAggregator;
import com.tangosol.util.aggregator.TopNAggregator;

import com.tangosol.util.function.Remote;

import java.math.BigDecimal;

import java.util.Comparator;
import java.util.concurrent.Executor;

/**
 * Simple Aggregator DSL.
 * <p>
 * The methods in this class are for the most part simple factory methods for
 * various {@link InvocableMap.StreamingAggregator} classes, but in some cases
 * provide additional type safety.
 * <p>
 * They also tend to make the code more readable, especially if imported
 * statically, so their use is strongly encouraged in lieu of direct construction
 * of {@code InvocableMap.StreamingAggregator} classes.
 *
 * @author lh, hr  2018.06.12
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Aggregators
    {
    /**
     * Return an AsynchronousAggregator for a given streaming aggregator.
     *
     * @param aggregator  the underlying streaming aggregator
     *
     * @param <K> the type of the Map entry keys
     * @param <V> the type of the Map entry values
     * @param <P> the type of the intermediate result during the parallel stage
     * @param <R> the type of the value returned by the StreamingAggregator
     */
    public static <K, V, P, R> AsynchronousAggregator<K, V, P, R>
        asynchronous(InvocableMap.StreamingAggregator<K, V, P, R> aggregator)
        {
        return new AsynchronousAggregator(aggregator);
        }

    /**
     * Return an AsynchronousAggregator for a given streaming aggregator.
     *
     * @param aggregator  the underlying streaming aggregator
     * @param executor    an optional {@link Executor} to complete the future on,
     *                    if not provided the {@link Daemons#commonPool()} is used
     *
     * @param <K> the type of the Map entry keys
     * @param <V> the type of the Map entry values
     * @param <P> the type of the intermediate result during the parallel stage
     * @param <R> the type of the value returned by the StreamingAggregator
     */
    public static <K, V, P, R> AsynchronousAggregator<K, V, P, R>
        asynchronous(InvocableMap.StreamingAggregator<K, V, P, R> aggregator, Executor executor)
        {
        return new AsynchronousAggregator(aggregator, executor);
        }

    /**
     * Return an AsynchronousAggregator for a given streaming aggregator.
     *
     * @param aggregator    the underlying streaming aggregator
     * @param iUnitOrderId  the unit-of-order id for this aggregator
     */
    public static <K, V, P, R> AsynchronousAggregator<K, V, P, R>
    asynchronous(InvocableMap.StreamingAggregator<K, V, P, R> aggregator, int iUnitOrderId)
        {
        return new AsynchronousAggregator(aggregator, iUnitOrderId);
        }

    /**
     * Return an AsynchronousAggregator for a given streaming aggregator.
     *
     * @param aggregator    the underlying streaming aggregator
     * @param iUnitOrderId  the unit-of-order id for this aggregator
     * @param executor      an optional {@link Executor} to complete the future on,
     *                      if not provided the {@link Daemons#commonPool()} is used
     */
    public static <K, V, P, R> AsynchronousAggregator<K, V, P, R>
        asynchronous(InvocableMap.StreamingAggregator<K, V, P, R> aggregator,
                     int iUnitOrderId, Executor executor)
        {
        return new AsynchronousAggregator(aggregator, iUnitOrderId, executor);
        }

    /**
     * Return an aggregator that calculates a average of the numeric values extracted
     * from a set of entries in a Map. All the extracted Number objects will be treated
     * as Java <tt>double</tt> values.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is a {@link Number}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, Double>
        average(ValueExtractor<? super T, ? extends Number> extractor)
        {
        return new DoubleAverage(extractor);
        }

    /**
     * Return an aggregator that calculates a average of the numeric values extracted
     * from a set of entries in a Map. All the extracted Number objects will be treated
     * as Java <tt>double</tt> values.
     *
     * @param sMethod  the name of the method that returns a value in the form
     *                 of any Java object that is a {@link Number}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     */
    public static <K, V> InvocableMap.StreamingAggregator<K, V, ?, Double> average(String sMethod)
        {
        return new DoubleAverage(Extractors.extract(sMethod));
        }

    /**
     * Return an aggregator that calculates a maximum of the <tt>double</tt> values extracted
     * from a set of entries in a Map.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is a {@link Double}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, Double>
        max(Remote.ToDoubleFunction<? super T> extractor)
        {
        return new DoubleMax(ValueExtractor.of((T t) -> extractor.applyAsDouble(t)));
        }

    /**
     * Return an aggregator that calculates a minimum of the <tt>double</tt> values extracted
     * from a set of entries in a Map.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is a {@link Double}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, Double>
        min(Remote.ToDoubleFunction<? super T> extractor)
        {
        return new DoubleMin(ValueExtractor.of((T t) -> extractor.applyAsDouble(t)));
        }

    /**
     * Return an aggregator that calculates a sum of the <tt>double</tt> values extracted
     * from a set of entries in a Map.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is a {@link Double}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, Double>
        sum(Remote.ToDoubleFunction<? super T> extractor)
        {
        return new DoubleSum(ValueExtractor.of((T t) -> extractor.applyAsDouble(t)));
        }

    /**
     * Return an aggregator that calculates an average of the <tt>double</tt> values extracted
     * from a set of entries in a Map.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is a {@link Double}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, Double>
        average(Remote.ToDoubleFunction<? super T> extractor)
        {
        return new DoubleAverage(ValueExtractor.of((T t) -> extractor.applyAsDouble(t)));
        }

    /**
     * Return an aggregator that calculates a maximum of the numeric values extracted
     * from a set of entries in a Map. All the extracted Number objects will be treated
     * as Java <tt>double</tt> values.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is a {@link Number}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, Double>
        doubleMax(ValueExtractor<? super T, ? extends Number> extractor)
        {
        return new DoubleMax(extractor);
        }

    /**
     * Return an aggregator that calculates a maximum of the numeric values extracted
     * from a set of entries in a Map. All the extracted Number objects will be treated
     * as Java <tt>double</tt> values.
     *
     * @param sMethod  the name of the method that returns a value in the form
     *                 of any Java object that is a {@link Number}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     */
    public static <K, V> InvocableMap.StreamingAggregator<K, V, ?, Double> doubleMax(String sMethod)
        {
        return new DoubleMax(Extractors.extract(sMethod));
        }

    /**
     * Return an aggregator that calculates a minimum of the numeric values extracted
     * from a set of entries in a Map. All the extracted Number objects will be treated
     * as Java <tt>double</tt> values.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is a {@link Number}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, Double>
        doubleMin(ValueExtractor<? super T, ? extends Number> extractor)
        {
        return new DoubleMin(extractor);
        }

    /**
     * Return an aggregator that calculates a minimum of the numeric values extracted
     * from a set of entries in a Map. All the extracted Number objects will be treated
     * as Java <tt>double</tt> values.
     *
     * @param sMethod  the name of the method that returns a value in the form
     *                 of any Java object that is a {@link Number}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     */
    public static <K, V> InvocableMap.StreamingAggregator<K, V, ?, Double> doubleMin(String sMethod)
        {
        return new DoubleMin(Extractors.extract(sMethod));
        }

    /**
     * Return an aggregator that calculates a sum of the numeric values extracted
     * from a set of entries in a Map. All the extracted Number objects will be treated
     * as Java <tt>double</tt> values.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is a {@link Number}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, Double>
        doubleSum(ValueExtractor<? super T, ? extends Number> extractor)
        {
        return new DoubleSum(extractor);
        }

    /**
     * Return an aggregator that calculates a sum of the numeric values extracted
     * from a set of entries in a Map. All the extracted Number objects will be treated
     * as Java <tt>double</tt> values.
     *
     * @param sMethod  the name of the method that returns a value in the form
     *                 of any Java object that is a {@link Number}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     */
    public static <K, V> InvocableMap.StreamingAggregator<K, V, ?, Double> doubleSum(String sMethod)
        {
        return new DoubleSum(Extractors.extract(sMethod));
        }

    /**
     * Return an aggregator that calculates a maximum of the <tt>int</tt> values extracted
     * from a set of entries in a Map.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is an {@link Integer}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, Long>
        max(Remote.ToIntFunction<? super T> extractor)
        {
        return new LongMax(ValueExtractor.of((T t) -> (long) extractor.applyAsInt(t)));
        }

    /**
     * Return an aggregator that calculates a minimum of the <tt>int</tt> values extracted
     * from a set of entries in a Map.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is an {@link Integer}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, Long>
        min(Remote.ToIntFunction<? super T> extractor)
        {
        return new LongMin(ValueExtractor.of((T t) -> (long) extractor.applyAsInt(t)));
        }

    /**
     * Return an aggregator that calculates a sum of the <tt>int</tt> values extracted
     * from a set of entries in a Map.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is an {@link Integer}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, Long>
        sum(Remote.ToIntFunction<? super T> extractor)
        {
        return new LongSum(ValueExtractor.of((T t) -> (long) extractor.applyAsInt(t)));
        }

    /**
     * Return an aggregator that calculates an average of the <tt>int</tt> values extracted
     * from a set of entries in a Map.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is an {@link Integer}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, Double>
        average(Remote.ToIntFunction<? super T> extractor)
        {
        return new DoubleAverage(ValueExtractor.of((T t) -> (long) extractor.applyAsInt(t)));
        }

    /**
     * Return an aggregator that calculates a maximum of the <tt>long</tt> values extracted
     * from a set of entries in a Map.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is a {@link Long}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, Long>
        max(Remote.ToLongFunction<? super T> extractor)
        {
        return new LongMax(ValueExtractor.of((T t) -> extractor.applyAsLong(t)));
        }

    /**
     * Return an aggregator that calculates a minimum of the <tt>long</tt> values extracted
     * from a set of entries in a Map.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is a {@link Long}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, Long>
        min(Remote.ToLongFunction<? super T> extractor)
        {
        return new LongMin(ValueExtractor.of((T t) -> extractor.applyAsLong(t)));
        }

    /**
     * Return an aggregator that calculates a sum of the <tt>long</tt> values extracted
     * from a set of entries in a Map.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is a {@link Long}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, Long>
        sum(Remote.ToLongFunction<? super T> extractor)
        {
        return new LongSum(ValueExtractor.of((T t) -> extractor.applyAsLong(t)));
        }

    /**
     * Return an aggregator that calculates an average of the <tt>long</tt> values extracted
     * from a set of entries in a Map.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is a {@link Long}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, Double>
        average(Remote.ToLongFunction<? super T> extractor)
        {
        return new DoubleAverage(ValueExtractor.of((T t) -> extractor.applyAsLong(t)));
        }

    /**
     * Return an aggregator that calculates a maximum of the numeric values extracted
     * from a set of entries in a Map. All the extracted Number objects will be treated
     * as Java <tt>long</tt> values.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is a {@link Number}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, Long>
        longMax(ValueExtractor<? super T, ? extends Number> extractor)
        {
        return new LongMax(extractor);
        }

    /**
     * Return an aggregator that calculates a maximum of the numeric values extracted
     * from a set of entries in a Map. All the extracted Number objects will be treated
     * as Java <tt>long</tt> values.
     *
     * @param sMethod  the name of the method that returns a value in the form
     *                 of any Java object that is a {@link Number}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     */
    public static <K, V> InvocableMap.StreamingAggregator<K, V, ?, Long> longMax(String sMethod)
        {
        return new LongMax(Extractors.extract(sMethod));
        }

    /**
     * Return an aggregator that calculates a minimum of the numeric values extracted
     * from a set of entries in a Map. All the extracted Number objects will be treated
     * as Java <tt>long</tt> values.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is a {@link Number}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, Long>
        longMin(ValueExtractor<? super T, ? extends Number> extractor)
        {
        return new LongMin(extractor);
        }

    /**
     * Return an aggregator that calculates a minimum of the numeric values extracted
     * from a set of entries in a Map. All the extracted Number objects will be treated
     * as Java <tt>long</tt> values.
     *
     * @param sMethod  the name of the method that returns a value in the form
     *                 of any Java object that is a {@link Number}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     */
    public static <K, V> InvocableMap.StreamingAggregator<K, V, ?, Long> longMin(String sMethod)
        {
        return new LongMin(Extractors.extract(sMethod));
        }

    /**
     * Return an aggregator that calculates a sum of the numeric values extracted
     * from a set of entries in a Map. All the extracted Number objects will be treated
     * as Java <tt>long</tt> values.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is a {@link Number}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, Long>
        longSum(ValueExtractor<? super T, ? extends Number> extractor)
        {
        return new LongSum(extractor);
        }

    /**
     * Return an aggregator that calculates a sum of the numeric values extracted
     * from a set of entries in a Map. All the extracted Number objects will be treated
     * as Java <tt>long</tt> values.
     *
     * @param sMethod  the name of the method that returns a value in the form
     *                 of any Java object that is a {@link Number}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     */
    public static <K, V> InvocableMap.StreamingAggregator<K, V, ?, Long> longSum(String sMethod)
        {
        return new LongSum(Extractors.extract(sMethod));
        }

    /**
     * Return an aggregator that calculates a maximum of the <tt>BigDecimal</tt> values extracted
     * from a set of entries in a Map.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is an {@link BigDecimal}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, BigDecimal>
        max(Remote.ToBigDecimalFunction<? super T> extractor)
        {
        return new BigDecimalMax(ValueExtractor.of((T t) -> extractor.apply(t)));
        }

    /**
     * Return an aggregator that calculates a minimum of the <tt>BigDecimal</tt> values extracted
     * from a set of entries in a Map.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is an {@link BigDecimal}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, BigDecimal>
        min(Remote.ToBigDecimalFunction<? super T> extractor)
        {
        return new BigDecimalMin(ValueExtractor.of((T t) -> extractor.apply(t)));
        }

    /**
     * Return an aggregator that calculates a sum of the <tt>BigDecimal</tt> values extracted
     * from a set of entries in a Map.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is an {@link BigDecimal}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, BigDecimal>
        sum(Remote.ToBigDecimalFunction<? super T> extractor)
        {
        return new BigDecimalSum(ValueExtractor.of((T t) -> extractor.apply(t)));
        }

    /**
     * Return an aggregator that calculates an average of the <tt>BigDecimal</tt> values extracted
     * from a set of entries in a Map.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is an {@link BigDecimal}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, BigDecimal>
        average(Remote.ToBigDecimalFunction<? super T> extractor)
        {
        return new BigDecimalAverage(ValueExtractor.of((T t) -> extractor.apply(t)));
        }

    /**
     * Return an aggregator that calculates a average of the numeric values extracted
     * from a set of entries in a Map. All the extracted Number objects will be treated
     * as {@link BigDecimal} values.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is a {@link Number}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, BigDecimal>
        bigDecimalAverage(ValueExtractor<? super T, ? extends Number> extractor)
        {
        return new BigDecimalAverage(extractor);
        }

    /**
     * Return an aggregator that calculates a average of the numeric values extracted
     * from a set of entries in a Map. All the extracted Number objects will be treated
     * as {@link BigDecimal} values.
     *
     * @param sMethod  the name of the method that returns a value in the form
     *                 of any Java object that is a {@link Number}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     */
    public static <K, V> InvocableMap.StreamingAggregator<K, V, ?, BigDecimal>  bigDecimalAverage(String sMethod)
        {
        return bigDecimalAverage(Extractors.extract(sMethod));
        }

    /**
     * Return an aggregator that calculates a maximum of the numeric values extracted
     * from a set of entries in a Map. All the extracted Number objects will be treated
     * as {@link BigDecimal} values.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is a {@link Number}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, BigDecimal>
        bigDecimalMax(ValueExtractor<? super T, ? extends Number> extractor)
        {
        return new BigDecimalMax(extractor);
        }

    /**
     * Return an aggregator that calculates a maximum of the numeric values extracted
     * from a set of entries in a Map. All the extracted Number objects will be treated
     * as {@link BigDecimal} values.
     *
     * @param sMethod  the name of the method that returns a value in the form
     *                 of any Java object that is a {@link Number}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     */
    public static <K, V> InvocableMap.StreamingAggregator<K, V, ?, BigDecimal>  bigDecimalMax(String sMethod)
        {
        return bigDecimalMax(Extractors.extract(sMethod));
        }

    /**
     * Return an aggregator that calculates a minimum of the numeric values extracted
     * from a set of entries in a Map. All the extracted Number objects will be treated
     * as {@link BigDecimal} values.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is a {@link Number}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, BigDecimal>
        bigDecimalMin(ValueExtractor<? super T, ? extends Number> extractor)
        {
        return new BigDecimalMin(extractor);
        }

    /**
     * Return an aggregator that calculates a minimum of the numeric values extracted
     * from a set of entries in a Map. All the extracted Number objects will be treated
     * as {@link BigDecimal} values.
     *
     * @param sMethod  the name of the method that returns a value in the form
     *                 of any Java object that is a {@link Number}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     */
    public static <K, V> InvocableMap.StreamingAggregator<K, V, ?, BigDecimal>  bigDecimalMin(String sMethod)
        {
        return bigDecimalMin(Extractors.extract(sMethod));
        }

    /**
     * Return an aggregator that calculates a sum of the numeric values extracted
     * from a set of entries in a Map. All the extracted Number objects will be treated
     * as {@link BigDecimal} values.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is a {@link Number}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T> InvocableMap.StreamingAggregator<K, V, ?, BigDecimal>
        bigDecimalSum(ValueExtractor<? super T, ? extends Number> extractor)
        {
        return  new BigDecimalSum(extractor);
        }

    /**
     * Return an aggregator that calculates a sum of the numeric values extracted
     * from a set of entries in a Map. All the extracted Number objects will be treated
     * as {@link BigDecimal} values.
     *
     * @param sMethod  the name of the method that returns a value in the form
     *                 of any Java object that is a {@link Number}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     */
    public static <K, V> InvocableMap.StreamingAggregator<K, V, ?, BigDecimal>  bigDecimalSum(String sMethod)
        {
        return bigDecimalSum(Extractors.extract(sMethod));
        }

    /**
     * Return an aggregator that calculates a maximum of the <tt>Comparable</tt> values extracted
     * from a set of entries in a Map.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is a {@link Comparable}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T, R extends Comparable<? super R>> InvocableMap.StreamingAggregator<K, V, ?, R>
        max(Remote.ToComparableFunction<? super T, ? extends R> extractor)
        {
        return new ComparableMax(ValueExtractor.of((T t) -> extractor.apply(t)));
        }

    /**
     * Return an aggregator that calculates a minimum of the <tt>Comparable</tt> values extracted
     * from a set of entries in a Map.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object that is a {@link Comparable}
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     */
    public static <K, V, T, R extends Comparable<? super R>> InvocableMap.StreamingAggregator<K, V, ?, R>
        min(Remote.ToComparableFunction<? super T, ? extends R> extractor)
        {
        return new ComparableMin(ValueExtractor.of((T t) -> extractor.apply(t)));
        }

    /**
     * Return an aggregator that calculates a maximum of the {@link Comparable} values
     * extracted from a set of entries in a Map. All the extracted objects will be
     * treated as {@link Comparable} values.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any object that implements {@link Comparable}
     *                   interface
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     * @param <R>  the type of the aggregation result
     */
    public static <K, V, T, R extends Comparable<? super R>> InvocableMap.StreamingAggregator<K, V, ?, R>
        comparableMax(ValueExtractor<? super T, ? extends R> extractor)
        {
        return new ComparableMax(extractor);
        }

    /**
     * Return an aggregator that calculates a maximum of the values extracted from a set
     * of entries in a Map. All the extracted objects will ordered using the specified
     * {@link Comparator}.
     *
     * @param extractor  the extractor that provides an object to be compared
     * @param comparator the comparator used to compare the extracted object
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     * @param <R>  the type of the aggregation result
     */
    public static <K, V, T, R extends Comparable<? super R>> InvocableMap.StreamingAggregator<K, V, ?, R>
        comparableMax(ValueExtractor<? super T, ? extends R> extractor, Comparator<? super R> comparator)
        {
        return  new ComparableMax(extractor, comparator);
        }

    /**
     * Return an aggregator that calculates a maximum of the {@link Comparable} values
     * extracted from a set of entries in a Map. All the extracted objects will be
     * treated as {@link Comparable} values.
     *
     * @param sMethod  the name of the method that returns a value in the form
     *                 of any object that implements {@link Comparable}
     *                 interface
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     * @param <R>  the type of the aggregation result
     */
    public static <K, V, T, R extends Comparable<? super R>> InvocableMap.StreamingAggregator<K, V, ?, R>
        comparableMax(String sMethod)
        {
        return comparableMax(Extractors.extract(sMethod));
        }

    /**
     * Return an aggregator that calculates a maximum of the values extracted from a set
     * of entries in a Map. All the extracted objects will ordered using the specified
     * {@link Comparator}.
     *
     * @param sMethod    the name of the method that returns the value to be compared
     * @param comparator the comparator used to compare the extracted object
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     * @param <R>  the type of the aggregation result
     */
    public static <K, V, T, R extends Comparable<? super R>> InvocableMap.StreamingAggregator<K, V, ?, R>
        comparableMax(String sMethod, Comparator<? super R> comparator)
        {
        return new ComparableMax(Extractors.extract(sMethod), comparator);
        }

    /**
     * Return an aggregator that calculates a minimum of the {@link Comparable} values
     * extracted from a set of entries in a Map. All the extracted objects will be
     * treated as {@link Comparable} values.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any object that implements {@link Comparable}
     *                   interface
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     * @param <R>  the type of the aggregation result
     */
    public static <K, V, T, R extends Comparable<? super R>> InvocableMap.StreamingAggregator<K, V, ?, R>
        comparableMin(ValueExtractor<? super T, ? extends R> extractor)
        {
        return new ComparableMin(extractor);
        }

    /**
     * Return an aggregator that calculates a minimum of the values extracted from a set
     * of entries in a Map. All the extracted objects will ordered using the specified
     * {@link Comparator}.
     *
     * @param extractor  the extractor that provides an object to be compared
     * @param comparator the comparator used to compare the extracted object
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     * @param <R>  the type of the aggregation result
     */
    public static <K, V, T, R extends Comparable<? super R>> InvocableMap.StreamingAggregator<K, V, ?, R>
        comparableMin(ValueExtractor<? super T, ? extends R> extractor, Comparator<? super R> comparator)
        {
        return new ComparableMin(extractor, comparator);
        }

    /**
     * Return an aggregator that calculates a minimum of the {@link Comparable} values
     * extracted from a set of entries in a Map. All the extracted objects will be
     * treated as {@link Comparable} values.
     *
     * @param sMethod  the name of the method that returns a value in the form
     *                 of any object that implements {@link Comparable}
     *                 interface
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <R>  the type of the aggregation result
     */
    public static <K, V, R extends Comparable<? super R>> InvocableMap.StreamingAggregator<K, V, ?, R>
        comparableMin(String sMethod)
        {
        return comparableMin(Extractors.extract(sMethod));
        }

    /**
     * Return an aggregator that calculates a minimum of the values extracted from a set
     * of entries in a Map. All the extracted objects will ordered using the specified
     * {@link Comparator}.
     *
     * @param sMethod  the name of the method that returns a value in the form
     *                 of any object that implements {@link Comparable}
     *                 interface
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <R>  the type of the aggregation result
     */
    public static <K, V, R extends Comparable<? super R>> InvocableMap.StreamingAggregator<K, V, ?, R>
        comparableMin(String sMethod, Comparator<? super R> comparator)
        {
        return new ComparableMin(Extractors.extract(sMethod), comparator);
        }

    /**
     * Return an aggregator that calculates the count of the entries in a Map.
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     */
    public static <K, V> Count<K, V> count()
        {
        return new Count<>();
        }

    /**
     * Return an aggregator that calculates the set of distinct values from the entries in a Map.
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     */
    public static <K, V> DistinctValues<K, V, ?, V> distinctValues()
        {
        return new DistinctValues<>(Extractors.identity());
        }

    /**
     * Return an aggregator that calculates the set of distinct values extracted from the entries in a Map.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any Java object
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     * @param <R>  the type of the aggregation result
     */
    public static <K, V, T, R> DistinctValues<K, V, T, R>
        distinctValues(ValueExtractor<? super T, ? extends R> extractor)
        {
        return new DistinctValues<>(extractor);
        }

    /**
     * Return an aggregator that calculates the set of distinct values extracted from the entries in a Map.
     *
     * @param sMethod  the name of the method that returns a value in the form
     *                 of any Java object
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <R>  the type of the aggregation result
     */
    public static <K, V, R> DistinctValues<K, V, ?, R> distinctValues(String sMethod)
        {
        return distinctValues(Extractors.extract(sMethod));
        }

    /**
     * Return an aggregator that calculates the combined set of results from a number of aggregators.
     *
     * @param aAggregator  an array of EntryAggregator objects; may not be null
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     *
     * @throws NullPointerException  if the aggregator array is null
     */
    public static <K, V> CompositeAggregator<K, V>
        composite(InvocableMap.EntryAggregator... aAggregator)
        {
        return new CompositeAggregator<>(aAggregator);
        }

    /**
     * Create an instance of group aggregator based on a specified property or method
     * name(s) and an {@link InvocableMap.EntryAggregator}.
     *
     * @param aggregator an underlying EntryAggregator
     * @param asNames    the property or method name(s) to extract values from. The resulting
     *                   {@link ValueExtractor} is used to split InvocableMap entries into distinct
     *                   groups.
     *
     * @param <K>  the type of the Map entry keys
     * @param <V>  the type of the Map entry values
     * @param <E>  the type of the extracted value
     * @param <R>  the type of the group aggregator result
     */
    public static <K, V, E, R> GroupAggregator<K, V, ?, E, R>
        grouping(InvocableMap.EntryAggregator<K, V, R> aggregator, String... asNames)
        {
        return grouping(aggregator, null, asNames);
        }

    /**
     * Create an instance of group aggregator based on a specified property or method
     * name(s) and an {@link InvocableMap.EntryAggregator}.
     *
     * @param aggregator an underlying EntryAggregator
     * @param filter     an optional Filter object that will be used to evaluate
     *                   results of each individual group aggregation
     * @param asNames    the property or method name(s) to extract values from. The resulting
     *                   {@link ValueExtractor} is used to split InvocableMap entries into distinct
     *                   groups.
     *
     * @param <K>  the type of the Map entry keys
     * @param <V>  the type of the Map entry values
     * @param <E>  the type of the extracted value
     * @param <R>  the type of the group aggregator result
     */
    public static <K, V, E, R> GroupAggregator<K, V, ?, E, R>
    grouping(InvocableMap.EntryAggregator<K, V, R> aggregator,
             Filter                                filter,
             String...                             asNames)
        {
        if (asNames == null || asNames.length == 0)
            {
            throw new IllegalArgumentException("Names parameter cannot be null or empty");
            }

        ValueExtractor<? super V, ? extends E> extractor;
        if (asNames.length == 1)
            {
            extractor = Extractors.extract(asNames[0]);
            }
        else
            {
            extractor = (ValueExtractor<? super V, ? extends E>) Extractors.multi(asNames);
            }

        return grouping(extractor, aggregator, filter);
        }

    /**
     * Create an instance of group aggregator based on a specified property or method
     * name(s) and an {@link InvocableMap.EntryAggregator}.
     *
     * @param extractor  a ValueExtractor that will be used to split a set of
     *                   InvocableMap entries into distinct groups
     * @param aggregator an underlying EntryAggregator
     *
     * @param <K>  the type of the Map entry keys
     * @param <V>  the type of the Map entry values
     * @param <T>  the type of the value to extract from
     * @param <E>  the type of the extracted value
     * @param <R>  the type of the group aggregator result
     */
    public static <K, V, T, E, R> GroupAggregator<K, V, T, E, R>
        grouping(ValueExtractor<? super T, ? extends E> extractor, InvocableMap.EntryAggregator<K, V, R> aggregator)
        {
        return grouping(extractor, aggregator, null);
        }

    /**
     * Create an instance of group aggregator based on a specified property or method
     * name(s) and an {@link InvocableMap.EntryAggregator}.
     *
     * @param extractor   a ValueExtractor that will be used to split a set of
     *                    InvocableMap entries into distinct groups
     * @param aggregator  an underlying EntryAggregator
     * @param filter      an optional Filter object used to filter out results
     *                    of individual group aggregation results
     *
     * @param <K>  the type of the Map entry keys
     * @param <V>  the type of the Map entry values
     * @param <T>  the type of the value to extract from
     * @param <E>  the type of the extracted value
     * @param <R>  the type of the group aggregator result
     */
    public static <K, V, T, E, R> GroupAggregator<K, V, T, E, R>
        grouping(ValueExtractor<? super T, ? extends E>                     extractor,
                 InvocableMap.EntryAggregator<? super K, ? super V, R> aggregator,
                 Filter                                                filter)
        {
        return GroupAggregator.createInstance(extractor, aggregator, filter);
        }

    /**
     * Return an aggregator that calculates the top n of the {@link Comparable} values
     * extracted from a set of entries in a Map. All the extracted objects will be
     * treated as {@link Comparable} values.
     *
     * @param extractor  the extractor that provides a value in the form of
     *                   any object that implements {@link Comparable}
     *                   interface
     * @param cResults   the maximum number of results to return
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     * @param <R>  the type of the aggregation result
     */
    public static <K, V, T, R extends Comparable<? super R>> TopNAggregator<K, V, T, R>
        topN(ValueExtractor<? super T, ? extends R> extractor, int cResults)
        {
        return new TopNAggregator<>(extractor, null, cResults);
        }

    /**
     * Return an aggregator that calculates the top n of the values extracted from a set
     * of entries in a Map. All the extracted objects will ordered using the specified
     * {@link Comparator}.
     *
     * @param extractor  the extractor that provides an object to be compared
     * @param comparator the comparator used to compare the extracted object
     * @param cResults   the maximum number of results to return
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     * @param <R>  the type of the aggregation result
     */
    public static <K, V, T, R> TopNAggregator<K, V, T, R>
        topN(ValueExtractor<? super T, ? extends R> extractor, Comparator<? super R> comparator, int cResults)
        {
        return  new TopNAggregator<>(extractor, comparator, cResults);
        }

    /**
     * Return an aggregator that calculates the top n of the {@link Comparable} values
     * extracted from a set of entries in a Map. All the extracted objects will be
     * treated as {@link Comparable} values.
     *
     * @param sMethod  the name of the method that returns a value in the form
     *                 of any object that implements {@link Comparable}
     *                 interface
     * @param cResults the maximum number of results to return
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     * @param <R>  the type of the aggregation result
     */
    public static <K, V, T, R extends Comparable<? super R>> TopNAggregator<K, V, T, R>
        topN(String sMethod, int cResults)
        {
        return new TopNAggregator(Extractors.extract(sMethod), null, cResults);
        }

    /**
     * Return an aggregator that calculates the top n of the values extracted from a set
     * of entries in a Map. All the extracted objects will ordered using the specified
     * {@link Comparator}.
     *
     * @param sMethod    the name of the method that returns the value to be compared
     * @param comparator the comparator used to compare the extracted object
     * @param cResults   the maximum number of results to return
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     * @param <R>  the type of the aggregation result
     */
    public static <K, V, T, R> TopNAggregator<K, V, T, R>
        topN(String sMethod, Comparator<? super R> comparator, int cResults)
        {
        return new TopNAggregator(Extractors.extract(sMethod), comparator, cResults);
        }

    /**
     * Return an aggregator that will return the extracted value for each entry in the map.
     *
     * @param extractor  the extractor that provides an value(s) to be returned
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <T>  the type of the value to extract from
     * @param <R>  the type of the aggregation result
     */
    public static <K, V, T, R extends Comparable<? super R>> ReducerAggregator<K, V, T, R>
        reduce(ValueExtractor<? super T, ? extends R> extractor)
        {
        return new ReducerAggregator(extractor);
        }

    /**
     * Return an aggregator that will return the extracted value for each entry in the map.
     *
     * @param sMethod  the name of the method to use to obtain the value
     *
     * @param <K>  the type of the entry's key
     * @param <V>  the type of the entry's value
     * @param <R>  the type of the aggregation result
     */
    public static <K, V, T, R extends Comparable<? super R>> ReducerAggregator<K, V, T, R>
        reduce(String sMethod)
        {
        return new ReducerAggregator(Extractors.extract(sMethod));
        }

    /**
     * Return an aggregator that is implemented in a script using the specified
     * language.
     *
     * @param sLanguage  the string specifying one of the supported languages
     * @param sName      the name of the {@link InvocableMap.StreamingAggregator}
     *                   that needs to be evaluated
     * @param aoArgs     the arguments to be passed to the
     *                   {@link InvocableMap.StreamingAggregator}
     * @param <K>        the type of key that the
     *                   {@link InvocableMap.StreamingAggregator} will receive
     * @param <V>        the type of value that the
     *                   {@link InvocableMap.StreamingAggregator} will receive
     * @param <P>        the type of partial result this
     *                   {@link InvocableMap.StreamingAggregator} will return
     * @param <R>        the type of final result this
     *                   {@link InvocableMap.StreamingAggregator} will return
     *
     * @return an instance of {@link InvocableMap.StreamingAggregator}
     *
     * @throws ScriptException           if the {@code script} cannot be loaded or
     *                                   any errors occur during its execution
     * @throws IllegalArgumentException  if the specified language is not supported
     */
    public static <K, V, P, R> ScriptAggregator<K, V, P, R> script(
            String sLanguage, String sName, Object... aoArgs)
        {
        return script(sLanguage, sName, 0, aoArgs);
        }

    /**
     * Return an aggregator that is implemented in a script using the specified
     * language.
     *
     * @param sLanguage        the string specifying one of the supported languages
     * @param sName            the name of the {@link InvocableMap.StreamingAggregator}
     *                         that needs to be evaluated
     * @param characteristics  a bit mask representing the set of characteristics
     *                         of this aggregator
     * @param aoArgs           the arguments to be passed to the
     *                         {@link InvocableMap.StreamingAggregator}
     * @param <K>              the type of key that the
     *                         {@link InvocableMap.StreamingAggregator} will receive
     * @param <V>              the type of value that the
     *                         {@link InvocableMap.StreamingAggregator} will receive
     * @param <P>              the type of partial result this
     *                         {@link InvocableMap.StreamingAggregator} will return
     * @param <R>              the type of final result this
     *                         {@link InvocableMap.StreamingAggregator} will return
     *
     * @return an instance of {@link InvocableMap.StreamingAggregator}
     *
     * @throws ScriptException           if the {@code script} cannot be loaded or
     *                                   any errors occur during its execution
     * @throws IllegalArgumentException  if the specified language is not supported
     *
     * @see InvocableMap.StreamingAggregator#characteristics()
     */
    public static <K, V, P, R> ScriptAggregator<K, V, P, R> script(
            String sLanguage, String sName, int characteristics, Object... aoArgs)
        {
        return new ScriptAggregator<>(sLanguage, sName, characteristics, aoArgs);
        }
    }
