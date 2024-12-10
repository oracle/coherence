/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.stream;

import com.tangosol.internal.util.DoubleSummaryStatistics;
import com.tangosol.internal.util.IntSummaryStatistics;
import com.tangosol.internal.util.LongSummaryStatistics;

import com.tangosol.internal.util.collection.PortableCollection;
import com.tangosol.internal.util.collection.PortableList;
import com.tangosol.internal.util.collection.PortableMap;
import com.tangosol.internal.util.collection.PortableSet;
import com.tangosol.internal.util.collection.PortableSortedSet;

import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.internal.util.stream.collectors.AveragingDoubleCollector;
import com.tangosol.internal.util.stream.collectors.AveragingIntCollector;
import com.tangosol.internal.util.stream.collectors.AveragingLongCollector;
import com.tangosol.internal.util.stream.collectors.BiReducingCollector;
import com.tangosol.internal.util.stream.collectors.CollectingAndThenCollector;
import com.tangosol.internal.util.stream.collectors.CollectionCollector;
import com.tangosol.internal.util.stream.collectors.GroupingByCollector;
import com.tangosol.internal.util.stream.collectors.MapCollector;
import com.tangosol.internal.util.stream.collectors.MappingCollector;
import com.tangosol.internal.util.stream.collectors.ReducingCollector;
import com.tangosol.internal.util.stream.collectors.SummarizingDoubleCollector;
import com.tangosol.internal.util.stream.collectors.SummarizingIntCollector;
import com.tangosol.internal.util.stream.collectors.SummarizingLongCollector;
import com.tangosol.internal.util.stream.collectors.SummingDoubleCollector;
import com.tangosol.internal.util.stream.collectors.SummingIntCollector;
import com.tangosol.internal.util.stream.collectors.SummingLongCollector;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.SimpleHolder;
import com.tangosol.util.SortedBag;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.comparator.SafeComparator;

import com.tangosol.util.function.Remote;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;

/**
 * Static factory for various {@link RemoteCollector}s that can be executed in
 * parallel in a distributed environment.
 *
 * @author as  2014.10.01
 * @since 12.2.1
 *
 * @see RemoteCollector
 * @see java.util.stream.Collectors
 */
public abstract class RemoteCollectors
    {
    /**
     * Returns a {@code Collector} that accumulates the input elements into a
     * new {@code Collection}, in encounter order.  The {@code Collection} is
     * created by the provided factory.
     *
     * @param <T>       the type of the input elements
     * @param <C>       the type of the resulting {@code Collection}
     * @param supplier  a {@code Supplier} which returns a new, empty
     *                  {@code Collection} of the appropriate type
     *
     * @return a {@code Collector} which collects all the input elements into a
     * {@code Collection}, in encounter order
     */
    public static <T, C extends Collection<T>> RemoteCollector<T, ?, C> toCollection(Remote.Supplier<C> supplier)
        {
        return new CollectionCollector<>(supplier);
        }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a
     * new {@code List}. There are no guarantees on the type, mutability,
     * serializability, or thread-safety of the {@code List} returned; if more
     * control over the returned {@code List} is required, use {@link
     * #toCollection(Remote.Supplier)}.
     *
     * @param <T> the type of the input elements
     *
     * @return a {@code Collector} which collects all the input elements into a
     * {@code List}, in encounter order
     */
    public static <T> RemoteCollector<T, ?, List<T>> toList()
        {
        return toCollection(PortableList::new);
        }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a
     * new {@code SortedBag}.
     *
     * @param <T> the type of the input elements
     *
     * @return a {@code Collector} which collects all the input elements into a
     * {@code List}, in encounter order
     */
    public static <T> RemoteCollector<T, ?, Collection<T>> toSortedBag()
        {
        return toSortedBag(SafeComparator.INSTANCE);
        }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a
     * new {@code SortedBag}.
     *
     * @param <T>         the type of the input elements
     * @param comparable  a {@link ValueExtractor} that returns a {@code Comparable}
     *                    value
     *
     * @return a {@code Collector} which collects all the input elements into a
     * {@code List}, in encounter order
     */
    public static <T, E extends Comparable<? super E>> RemoteCollector<T, ?, Collection<T>> toSortedBag(ValueExtractor<? super T, ? extends E> comparable)
        {
        return toSortedBag(Remote.comparator(comparable));
        }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a
     * new {@code SortedBag}.
     *
     * @param <T>         the type of the input elements
     * @param comparator  a comparator for type T
     *
     * @return a {@code Collector} which collects all the input elements into a
     * {@code List}, in encounter order
     */
    public static <T> RemoteCollector<T, ?, Collection<T>> toSortedBag(Comparator<? super T> comparator)
        {
        Remote.Supplier<Collection<T>> supplier = () -> new SortedBag<>(comparator);
        return toCollection(() -> new PortableCollection<>(supplier));
        }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a
     * new {@code SortedBag}.
     *
     * @param <T> the type of the input elements
     * @param comparator  a comparator for type T
     *
     * @return a {@code Collector} which collects all the input elements into a
     * {@code List}, in encounter order
     */
    public static <T> RemoteCollector<T, ?, Collection<T>> toSortedBag(Remote.Comparator<? super T> comparator)
        {
        Remote.Supplier<Collection<T>> supplier = () -> new SortedBag<>(comparator);
        return toCollection(() -> new PortableCollection<>(supplier));
        }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a
     * new {@code Set}. There are no guarantees on the type, mutability,
     * serializability, or thread-safety of the {@code Set} returned; if more
     * control over the returned {@code Set} is required, use {@link
     * #toCollection(Remote.Supplier)}.
     * <p>
     * This is an {@link RemoteCollector.Characteristics#UNORDERED unordered}
     * Collector.
     *
     * @param <T> the type of the input elements
     *
     * @return a {@code Collector} which collects all the input elements into a
     * {@code Set}
     */
    public static <T> RemoteCollector<T, ?, Set<T>> toSet()
        {
        return toCollection(PortableSet::new);
        }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a
     * new {@code SortedSet}. There are no guarantees on the type, mutability,
     * serializability, or thread-safety of the {@code SortedSet} returned;
     * if more control over the returned {@code SortedSet} is required, use
     * {@link #toCollection(Remote.Supplier)}.
     * <p>
     * This is an {@link RemoteCollector.Characteristics#UNORDERED unordered}
     * Collector.
     *
     * @param <T> the type of the input elements
     *
     * @return a {@code Collector} which collects all the input elements into a
     * {@code SortedSet}
     */
    public static <T> RemoteCollector<T, ?, SortedSet<T>> toSortedSet()
        {
        return toCollection(PortableSortedSet::new);
        }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a
     * new {@code SortedSet}. There are no guarantees on the type, mutability,
     * serializability, or thread-safety of the {@code SortedSet} returned;
     * if more control over the returned {@code SortedSet} is required, use
     * {@link #toCollection(Remote.Supplier)}.
     * <p>
     * This is an {@link RemoteCollector.Characteristics#UNORDERED unordered}
     * Collector.
     *
     * @param <T>         the type of the input elements
     * @param comparable  a {@link ValueExtractor} that returns a {@code Comparable}
     *                    value
     *
     * @return a {@code Collector} which collects all the input elements into a
     * {@code SortedSet}
     */
    public static <T, E extends Comparable<? super E>> RemoteCollector<T, ?, SortedSet<T>> toSortedSet(ValueExtractor<? super T, ? extends E> comparable)
        {
        return toSortedSet(Remote.comparator(comparable));
        }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a
     * new {@code SortedSet}. There are no guarantees on the type, mutability,
     * serializability, or thread-safety of the {@code SortedSet} returned;
     * if more control over the returned {@code SortedSet} is required, use
     * {@link #toCollection(Remote.Supplier)}.
     * <p>
     * This is an {@link RemoteCollector.Characteristics#UNORDERED unordered}
     * Collector.
     *
     * @param <T>         the type of the input elements
     * @param comparator  a comparator for type T
     *
     * @return a {@code Collector} which collects all the input elements into a
     * {@code SortedSet}
     */
    public static <T> RemoteCollector<T, ?, SortedSet<T>> toSortedSet(Comparator<? super T> comparator)
        {
        return toCollection(() -> new PortableSortedSet<>(comparator));
        }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a
     * new {@code SortedSet}. There are no guarantees on the type, mutability,
     * serializability, or thread-safety of the {@code SortedSet} returned;
     * if more control over the returned {@code SortedSet} is required, use
     * {@link #toCollection(Remote.Supplier)}.
     * <p>
     * This is an {@link RemoteCollector.Characteristics#UNORDERED unordered}
     * Collector.
     *
     * @param <T>         the type of the input elements
     * @param comparator  a comparator for type T
     *
     * @return a {@code Collector} which collects all the input elements into a
     * {@code SortedSet}
     */
    public static <T> RemoteCollector<T, ?, SortedSet<T>> toSortedSet(Remote.Comparator<? super T> comparator)
        {
        return toCollection(() -> new PortableSortedSet<>(comparator));
        }

    /**
     * Adapts a {@code Collector} accepting elements of type {@code U} to one
     * accepting elements of type {@code T} by applying a mapping function to
     * each input element before accumulation.
     *
     * @param <T>        the type of the input elements
     * @param <U>        type of elements accepted by downstream collector
     * @param <A>        intermediate accumulation type of the downstream
     *                   collector
     * @param <R>        result type of collector
     * @param mapper     a function to be applied to the input elements
     * @param downstream a collector which will accept mapped values
     *
     * @return a collector which applies the mapping function to the input
     * elements and provides the mapped results to the downstream collector
     *
     * @apiNote The {@code mapping()} collectors are most useful when used in a
     * multi-level reduction, such as downstream of a {@code groupingBy} or
     * {@code partitioningBy}.  For example, given a stream of {@code Person},
     * to accumulate the set of last names in each city:
     * <pre>{@code
     *     Map<City, Set<String>> lastNamesByCity
     *         = people.stream().collect(groupingBy(Person::getCity,
     *                                              mapping(Person::getLastName,
     * toSet())));
     * }</pre>
     */
    public static <T, U, A, R>
    RemoteCollector<T, A, R> mapping(Remote.Function<? super T, ? extends U> mapper,
                                     RemoteCollector<? super U, A, R> downstream)
        {
        return new MappingCollector(mapper, downstream);
        }

    /**
     * Adapts a {@code Collector} to perform an additional finishing
     * transformation.  For example, one could adapt the {@link #toList()}
     * collector to always produce an immutable list with:
     * <pre>{@code
     *     List<String> people
     *         = people.stream().collect(collectingAndThen(toList(),
     * Collections::unmodifiableList));
     * }</pre>
     *
     * @param <T>        the type of the input elements
     * @param <A>        intermediate accumulation type of the downstream
     *                   collector
     * @param <R>        result type of the downstream collector
     * @param <RR>       result type of the resulting collector
     * @param downstream a collector
     * @param finisher   a function to be applied to the final result of the
     *                   downstream collector
     *
     * @return a collector which performs the action of the downstream
     *         collector, followed by an additional finishing step
     */
    public static <T, A, R, RR>
    RemoteCollector<T, A, RR> collectingAndThen(RemoteCollector<T, A, R> downstream,
                                                Remote.Function<R, RR> finisher)
        {
        return new CollectingAndThenCollector<>(downstream, finisher);
        }

    /**
     * Returns a {@code Collector} accepting elements of type {@code T} that
     * counts the number of input elements.  If no elements are present, the
     * result is 0.
     *
     * @param <T> the type of the input elements
     *
     * @return a {@code Collector} that counts the input elements
     *
     * @implSpec This produces a result equivalent to:
     * <pre>{@code
     *     reducing(0L, e -> 1L, Long::sum)
     * }</pre>
     */
    public static <T> RemoteCollector<T, ?, Long> counting()
        {
        return reducing(0L, e -> 1L, Long::sum);
        }

    /**
     * Returns a {@code Collector} that produces the minimal element according
     * to a given {@code Comparable} attribute, described as an {@code Optional<T>}.
     *
     * @param <T>         the type of the input elements
     * @param comparable  a {@link ValueExtractor} that returns a {@code Comparable}
     *                    value
     *
     * @return a {@code Collector} that produces the minimal value
     *
     * @implSpec This produces a result equivalent to:
     * <pre>{@code
     *     reducing(Remote.BinaryOperator.minBy(comparator))
     * }</pre>
     */
    public static <T, E extends Comparable<? super E>> RemoteCollector<T, ?, Optional<T>> minBy(ValueExtractor<? super T, ? extends E> comparable)
        {
        return reducing(Remote.BinaryOperator.minBy(comparable));
        }

    /**
     * Returns a {@code Collector} that produces the minimal element according
     * to a given {@code Comparator}, described as an {@code Optional<T>}.
     *
     * @param <T>        the type of the input elements
     * @param comparator a {@code Comparator} for comparing elements
     *
     * @return a {@code Collector} that produces the minimal value
     *
     * @implSpec This produces a result equivalent to:
     * <pre>{@code
     *     reducing(Remote.BinaryOperator.minBy(comparator))
     * }</pre>
     */
    public static <T> RemoteCollector<T, ?, Optional<T>> minBy(Remote.Comparator<? super T> comparator)
        {
        return reducing(Remote.BinaryOperator.minBy(comparator));
        }

    /**
     * Returns a {@code Collector} that produces the maximal element according
     * to a given {@code Comparable} attribute, described as an {@code Optional<T>}.
     *
     * @param <T>         the type of the input elements
     * @param comparable  a {@link ValueExtractor} that returns a {@code Comparable}
     *                    value
     *
     * @return a {@code Collector} that produces the minimal value
     *
     * @implSpec This produces a result equivalent to:
     * <pre>{@code
     *     reducing(Remote.BinaryOperator.minBy(comparator))
     * }</pre>
     */
    public static <T, E extends Comparable<? super E>> RemoteCollector<T, ?, Optional<T>> maxBy(ValueExtractor<? super T, ? extends E> comparable)
        {
        return reducing(Remote.BinaryOperator.maxBy(comparable));
        }

    /**
     * Returns a {@code Collector} that produces the maximal element according
     * to a given {@code Comparator}, described as an {@code Optional<T>}.
     *
     * @param <T>        the type of the input elements
     * @param comparator a {@code Comparator} for comparing elements
     *
     * @return a {@code Collector} that produces the maximal value
     *
     * @implSpec This produces a result equivalent to:
     * <pre>{@code
     *     reducing(BinaryOperator.maxBy(comparator))
     * }</pre>
     */
    public static <T> RemoteCollector<T, ?, Optional<T>> maxBy(Remote.Comparator<? super T> comparator)
        {
        return reducing(Remote.BinaryOperator.maxBy(comparator));
        }

    /**
     * Returns a {@code Collector} that produces the sum of a integer-valued
     * function applied to the input elements.  If no elements are present, the
     * result is 0.
     *
     * @param <T>       the type of the stream elements
     * @param <U>       the type of the objects to extract from, which should be
     *                  either the same as {@code T}, or the key or value type
     *                  if the {@code T} is {@code InvocableMap.Entry}
     * @param extractor a function extracting the property to be summed
     *
     * @return a {@code Collector} that produces the sum of a derived property
     */
    public static <T, U> RemoteCollector<T, ?, Integer>
    summingInt(ValueExtractor<? super U, ? extends Number> extractor)
        {
        ValueExtractor<? super U, ? extends Number> ex = Lambdas.ensureRemotable(extractor);

        Remote.ToIntFunction<? super T> mapper =
                t -> t instanceof InvocableMap.Entry
                     ? ((InvocableMap.Entry<?, ?>) t).extract(ex).intValue()
                     : ex.extract((U) t).intValue();

        return new SummingIntCollector<>(mapper);
        }

    /**
     * Returns a {@code Collector} that produces the sum of a long-valued
     * function applied to the input elements.  If no elements are present, the
     * result is 0.
     *
     * @param <T>       the type of the stream elements
     * @param <U>       the type of the objects to extract from, which should be
     *                  either the same as {@code T}, or the key or value type
     *                  if the {@code T} is {@code InvocableMap.Entry}
     * @param extractor a function extracting the property to be summed
     *
     * @return a {@code Collector} that produces the sum of a derived property
     */
    public static <T, U> RemoteCollector<T, ?, Long>
    summingLong(ValueExtractor<? super U, ? extends Number> extractor)
        {
        ValueExtractor<? super U, ? extends Number> ex = Lambdas.ensureRemotable(extractor);

        Remote.ToLongFunction<? super T> mapper =
                t -> t instanceof InvocableMap.Entry
                     ? ((InvocableMap.Entry<?, ?>) t).extract(ex).longValue()
                     : ex.extract((U) t).longValue();

        return new SummingLongCollector<>(mapper);
        }

    /**
     * Returns a {@code Collector} that produces the sum of a double-valued
     * function applied to the input elements.  If no elements are present, the
     * result is 0.
     * <p>
     * The sum returned can vary depending upon the order in which values are
     * recorded, due to accumulated rounding error in addition of values of
     * differing magnitudes. Values sorted by increasing absolute magnitude tend
     * to yield more accurate results.  If any recorded value is a {@code NaN}
     * or the sum is at any point a {@code NaN} then the sum will be {@code
     * NaN}.
     *
     * @param <T>       the type of the stream elements
     * @param <U>       the type of the objects to extract from, which should be
     *                  either the same as {@code T}, or the key or value type
     *                  if the {@code T} is {@code InvocableMap.Entry}
     * @param extractor a function extracting the property to be summed
     *
     * @return a {@code Collector} that produces the sum of a derived property
     */
    public static <T, U> RemoteCollector<T, ?, Double>
    summingDouble(ValueExtractor<? super U, ? extends Number> extractor)
        {
        ValueExtractor<? super U, ? extends Number> ex = Lambdas.ensureRemotable(extractor);

        Remote.ToDoubleFunction<? super T> mapper =
                t -> t instanceof InvocableMap.Entry
                     ? ((InvocableMap.Entry<?, ?>) t).extract(ex).doubleValue()
                     : ex.extract((U) t).doubleValue();

        return new SummingDoubleCollector<>(mapper);
        }

    /**
     * Returns a {@code Collector} that produces the arithmetic mean of an
     * integer-valued function applied to the input elements.  If no elements
     * are present, the result is 0.
     *
     * @param <T>       the type of the stream elements
     * @param <U>       the type of the objects to extract from, which should be
     *                  either the same as {@code T}, or the key or value type
     *                  if the {@code T} is {@code InvocableMap.Entry}
     * @param extractor a function extracting the property to be summed
     *
     * @return a {@code Collector} that produces the sum of a derived property
     */
    public static <T, U> RemoteCollector<T, ?, Double>
    averagingInt(ValueExtractor<? super U, ? extends Number> extractor)
        {
        ValueExtractor<? super U, ? extends Number> ex = Lambdas.ensureRemotable(extractor);

        Remote.ToIntFunction<? super T> mapper =
                t -> t instanceof InvocableMap.Entry
                     ? ((InvocableMap.Entry<?, ?>) t).extract(ex).intValue()
                     : ex.extract((U) t).intValue();

        return new AveragingIntCollector<>(mapper);
        }

    /**
     * Returns a {@code Collector} that produces the arithmetic mean of a
     * long-valued function applied to the input elements.  If no elements are
     * present, the result is 0.
     *
     * @param <T>       the type of the stream elements
     * @param <U>       the type of the objects to extract from, which should be
     *                  either the same as {@code T}, or the key or value type
     *                  if the {@code T} is {@code InvocableMap.Entry}
     * @param extractor a function extracting the property to be averaged
     *
     * @return a {@code Collector} that produces the average of a derived property
     */
    public static <T, U> RemoteCollector<T, ?, Double>
    averagingLong(ValueExtractor<? super U, ? extends Number> extractor)
        {
        ValueExtractor<? super U, ? extends Number> ex = Lambdas.ensureRemotable(extractor);

        Remote.ToLongFunction<? super T> mapper =
                t -> t instanceof InvocableMap.Entry
                     ? ((InvocableMap.Entry<?, ?>) t).extract(ex).longValue()
                     : ex.extract((U) t).longValue();

        return new AveragingLongCollector<>(mapper);
        }

    /**
     * Returns a {@code Collector} that produces the arithmetic mean of a
     * double-valued function applied to the input elements.  If no elements are
     * present, the result is 0.
     * <p>
     * The average returned can vary depending upon the order in which values
     * are recorded, due to accumulated rounding error in addition of values of
     * differing magnitudes. Values sorted by increasing absolute magnitude tend
     * to yield more accurate results.  If any recorded value is a {@code NaN}
     * or the sum is at any point a {@code NaN} then the average will be {@code
     * NaN}.
     *
     * @param <T>       the type of the stream elements
     * @param <U>       the type of the objects to extract from, which should be
     *                  either the same as {@code T}, or the key or value type
     *                  if the {@code T} is {@code InvocableMap.Entry}
     * @param extractor a function extracting the property to be summed
     *
     * @return a {@code Collector} that produces the sum of a derived property
     *
     * @implNote The {@code double} format can represent all consecutive
     * integers in the range -2<sup>53</sup> to 2<sup>53</sup>. If the pipeline
     * has more than 2<sup>53</sup> values, the divisor in the average
     * computation will saturate at 2<sup>53</sup>, leading to additional
     * numerical errors.
     */
    public static <T, U> RemoteCollector<T, ?, Double>
    averagingDouble(ValueExtractor<? super U, ? extends Number> extractor)
        {
        ValueExtractor<? super U, ? extends Number> ex = Lambdas.ensureRemotable(extractor);

        Remote.ToDoubleFunction<? super T> mapper =
                t -> t instanceof InvocableMap.Entry
                     ? ((InvocableMap.Entry<?, ?>) t).extract(ex).doubleValue()
                     : ex.extract((U) t).doubleValue();

        return new AveragingDoubleCollector<>(mapper);
        }

    /**
     * Returns a {@code Collector} which performs a reduction of its input
     * elements under a specified {@code BinaryOperator} using the provided
     * identity.
     *
     * @param <T>      element type for the input and output of the reduction
     * @param identity the identity value for the reduction (also, the value
     *                 that is returned when there are no input elements)
     * @param op       a {@code BinaryOperator<T>} used to reduce the input
     *                 elements
     *
     * @return a {@code Collector} which implements the reduction operation
     *
     * @apiNote The {@code reducing()} collectors are most useful when used in a
     * multi-level reduction, downstream of {@code groupingBy} or {@code
     * partitioningBy}.  To perform a simple reduction on a stream, use {@link
     * RemoteStream#reduce(Object, BinaryOperator)}} instead.
     * @see #reducing(Remote.BinaryOperator)
     * @see #reducing(Object, Remote.Function, Remote.BinaryOperator)
     */
    public static <T> RemoteCollector<T, SimpleHolder<T>, T> reducing(T identity, Remote.BinaryOperator<T> op)
        {
        return new ReducingCollector<>(identity, op);
        }

    /**
     * Returns a {@code Collector} which performs a reduction of its input
     * elements under a specified {@code BinaryOperator}.  The result is
     * described as an {@code Optional<T>}.
     *
     * @param <T> element type for the input and output of the reduction
     * @param op  a {@code BinaryOperator<T>} used to reduce the input elements
     *
     * @return a {@code Collector} which implements the reduction operation
     *
     * @apiNote The {@code reducing()} collectors are most useful when used in a
     * multi-level reduction, downstream of {@code groupingBy} or {@code
     * partitioningBy}.  To perform a simple reduction on a stream, use {@link
     * RemoteStream#reduce(BinaryOperator)} instead.
     * <p>
     * For example, given a stream of {@code Person}, to calculate tallest
     * person in each city:
     * <pre>{@code
     *     Comparator<Person> byHeight = Comparator.comparing(Person::getHeight);
     *     Map<City, Person> tallestByCity
     *         = people.stream().collect(groupingBy(Person::getCity,
     * reducing(BinaryOperator.maxBy(byHeight))));
     * }</pre>
     * @see #reducing(Object, Remote.BinaryOperator)
     * @see #reducing(Object, Remote.Function, Remote.BinaryOperator)
     */
    public static <T> RemoteCollector<T, ?, Optional<T>> reducing(Remote.BinaryOperator<T> op)
        {
        return collectingAndThen(reducing(null, op), Optional::ofNullable);
        }

    /**
     * Returns a {@code Collector} which performs a reduction of its input
     * elements under a specified mapping function and {@code BinaryOperator}.
     * This is a generalization of {@link #reducing(Object, Remote.BinaryOperator)}
     * which allows a transformation of the elements before reduction.
     *
     * @param <T>      the type of the input elements
     * @param <U>      the type of the mapped values
     * @param identity the identity value for the reduction (also, the value
     *                 that is returned when there are no input elements)
     * @param mapper   a mapping function to apply to each input value
     * @param op       a {@code BinaryOperator<U>} used to reduce the mapped
     *                 values
     *
     * @return a {@code Collector} implementing the map-reduce operation
     *
     * @apiNote The {@code reducing()} collectors are most useful when used in a
     * multi-level reduction, downstream of {@code groupingBy} or {@code
     * partitioningBy}.  To perform a simple map-reduce on a stream, use {@link
     * RemoteStream#map(Function)} and {@link RemoteStream#reduce(Object, BinaryOperator)}
     * instead.
     * <p>
     * For example, given a stream of {@code Person}, to calculate the
     * longest last name of residents in each city:
     * <pre>{@code
     *     Comparator<String> byLength = Comparator.comparing(String::length);
     *     Map<City, String> longestLastNameByCity
     *         = people.stream().collect(groupingBy(Person::getCity,
     *                                              reducing(Person::getLastName,
     * BinaryOperator.maxBy(byLength))));
     * }</pre>
     * @see #reducing(Object, Remote.BinaryOperator)
     * @see #reducing(Remote.BinaryOperator)
     */
    public static <T, U> RemoteCollector<T, ?, U> reducing(U identity,
                                Remote.Function<? super T, ? extends U> mapper,
                                Remote.BinaryOperator<U> op)
        {
        return mapping(mapper, reducing(identity, op));
        }

    /**
     * Returns a {@code Collector} which performs a reduction of its input
     * elements under a specified mapping function and {@code BinaryOperator}.
     * This is a generalization of {@link #reducing(Object, Remote.BinaryOperator)}
     * which allows a transformation of the elements before reduction.
     *
     * @param <T>      the type of the input elements
     * @param <U>      the type of the mapped values
     * @param identity the identity value for the reduction (also, the value
     *                 that is returned when there are no input elements)
     * @param mapper   a mapping function to apply to each input value
     * @param op       a {@code BinaryOperator<U>} used to reduce the mapped
     *                 values
     *
     * @return a {@code Collector} implementing the map-reduce operation
     *
     * @apiNote The {@code reducing()} collectors are most useful when used in a
     * multi-level reduction, downstream of {@code groupingBy} or {@code
     * partitioningBy}.  To perform a simple map-reduce on a stream, use {@link
     * RemoteStream#map(Function)} and {@link RemoteStream#reduce(Object, BinaryOperator)}
     * instead.
     * <p>
     * For example, given a stream of {@code Person}, to calculate the
     * longest last name of residents in each city:
     * <pre>{@code
     *     Comparator<String> byLength = Comparator.comparing(String::length);
     *     Map<City, String> longestLastNameByCity
     *         = people.stream().collect(groupingBy(Person::getCity,
     *                                              reducing(Person::getLastName,
     * BinaryOperator.maxBy(byLength))));
     * }</pre>
     * @see #reducing(Object, Remote.BinaryOperator)
     * @see #reducing(Remote.BinaryOperator)
     */
    public static <T, U> RemoteCollector<T, ?, U> reducing(U identity,
                                Remote.BiFunction<? super U, ? super T, ? extends U> mapper,
                                Remote.BinaryOperator<U> op)
        {
        return new BiReducingCollector<>(identity, mapper, op);
        }

    /**
     * Returns a {@code Collector} implementing a "group by" operation on input
     * elements of type {@code T}, grouping elements according to a
     * classification function, and returning the results in a {@code Map}.
     * <p>
     * The classification function maps elements to some key type {@code K}.
     * The collector produces a {@code Map<K, List<T>>} whose keys are the
     * values resulting from applying the classification function to the input
     * elements, and whose corresponding values are {@code List}s containing the
     * input elements which map to the associated key under the classification
     * function.
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code Map} or {@code List} objects returned.
     *
     * @param <T>        the type of the stream elements
     * @param <U>        the type of the objects to extract from, which should be
     *                   either the same as {@code T}, or the key or value type
     *                   if the {@code T} is {@code InvocableMap.Entry}
     * @param <K>        the type of the keys
     * @param classifier the classifier function mapping input elements to keys
     *
     * @return a {@code Collector} implementing the group-by operation
     *
     * @implSpec This produces a result similar to:
     * <pre>{@code
     *     groupingBy(classifier, toList());
     * }</pre>
     * @see #groupingBy(ValueExtractor, RemoteCollector)
     * @see #groupingBy(ValueExtractor, Remote.Supplier, RemoteCollector)
     */
    public static <T, U, K> RemoteCollector<T, ?, Map<K, List<T>>>
    groupingBy(ValueExtractor<? super U, ? extends K> classifier)
        {
        return groupingBy(classifier, toList());
        }

    /**
     * Returns a {@code Collector} implementing a cascaded "group by" operation
     * on input elements of type {@code T}, grouping elements according to a
     * classification function, and then performing a reduction operation on the
     * values associated with a given key using the specified downstream {@code
     * Collector}.
     * <p>
     * The classification function maps elements to some key type {@code K}.
     * The downstream collector operates on elements of type {@code T} and
     * produces a result of type {@code D}. The resulting collector produces a
     * {@code Map<K, D>}.
     * <p>
     * There are no guarantees on the type, mutability, serializability, or
     * thread-safety of the {@code Map} returned.
     * <p>
     * For example, to compute the set of last names of people in each city:
     * <pre>{@code
     *     Map<City, Set<String>> namesByCity
     *         = people.stream().collect(groupingBy(Person::getCity,
     *                                              mapping(Person::getLastName,
     * toSet())));
     * }</pre>
     *
     * @param <T>        the type of the stream elements
     * @param <U>        the type of the objects to extract from, which should be
     *                   either the same as {@code T}, or the key or value type
     *                   if the {@code T} is {@code InvocableMap.Entry}
     * @param <K>        the type of the keys
     * @param <A>        the intermediate accumulation type of the downstream
     *                   collector
     * @param <D>        the result type of the downstream reduction
     * @param classifier a classifier function mapping input elements to keys
     * @param downstream a {@code Collector} implementing the downstream
     *                   reduction
     *
     * @return a {@code Collector} implementing the cascaded group-by operation
     *
     * @see #groupingBy(ValueExtractor)
     * @see #groupingBy(ValueExtractor, Remote.Supplier, RemoteCollector)
     */
    public static <T, U, K, A, D>
    RemoteCollector<T, ?, Map<K, D>> groupingBy(ValueExtractor<? super U, ? extends K> classifier,
                                                RemoteCollector<? super T, A, D> downstream)
        {
        return groupingBy(classifier, HashMap::new, downstream);
        }

    /**
     * Returns a {@code Collector} implementing a cascaded "group by" operation
     * on input elements of type {@code T}, grouping elements according to a
     * classification function, and then performing a reduction operation on the
     * values associated with a given key using the specified downstream {@code
     * Collector}.  The {@code Map} produced by the Collector is created with
     * the supplied factory function.
     * <p>
     * The classification function maps elements to some key type {@code K}.
     * The downstream collector operates on elements of type {@code T} and
     * produces a result of type {@code D}. The resulting collector produces a
     * {@code Map<K, D>}.
     * <p>
     * For example, to compute the set of last names of people in each city,
     * where the city names are sorted:
     * <pre>{@code
     *     Map<City, Set<String>> namesByCity
     *         = people.stream().collect(groupingBy(Person::getCity,
     * TreeMap::new,
     *                                              mapping(Person::getLastName,
     * toSet())));
     * }</pre>
     *
     * @param <T>        the type of the stream elements
     * @param <U>        the type of the objects to extract from, which should be
     *                   either the same as {@code T}, or the key or value type
     *                   if the {@code T} is {@code InvocableMap.Entry}
     * @param <K>        the type of the keys
     * @param <A>        the intermediate accumulation type of the downstream
     *                   collector
     * @param <D>        the result type of the downstream reduction
     * @param <M>        the type of the resulting {@code Map}
     * @param extractor  an extractor function mapping input elements to keys
     * @param downstream a {@code Collector} implementing the downstream
     *                   reduction
     * @param mapFactory a function which, when called, produces a new empty
     *                   {@code Map} of the desired type
     *
     * @return a {@code Collector} implementing the cascaded group-by operation
     *
     * @see #groupingBy(ValueExtractor, RemoteCollector)
     * @see #groupingBy(ValueExtractor)
     */
    public static <T, U, K, D, A, M extends Map<K, D>>
    RemoteCollector<T, ?, M> groupingBy(ValueExtractor<? super U, ? extends K> extractor,
                                        Remote.Supplier<M> mapFactory,
                                        RemoteCollector<? super T, A, D> downstream)
        {
        ValueExtractor<? super U, ? extends K> ex = Lambdas.ensureRemotable(extractor);

        Remote.Function<? super T, ? extends K> classifier =
                t -> t instanceof InvocableMap.Entry
                     ? ((InvocableMap.Entry<?, ?>) t).extract(ex)
                     : ex.extract((U) t);

        return new GroupingByCollector<>(classifier, downstream, mapFactory);
        }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code Map}
     * whose keys and values are the result of applying the provided mapping
     * functions to the input elements.
     * <p>
     * If the mapped keys contains duplicates (according to {@link
     * Object#equals(Object)}), an {@code IllegalStateException} is thrown when
     * the collection operation is performed.  If the mapped keys may have
     * duplicates, use {@link #toMap(ValueExtractor, ValueExtractor, Remote.BinaryOperator)}
     * instead.
     *
     * @param <T>         the type of the stream elements
     * @param <U1>        the type of the objects to extract keys from, which
     *                    should be either the same as {@code T}, or the key or
     *                    value type if the {@code T} is {@code InvocableMap.Entry}
     * @param <U2>        the type of the objects to extract values from, which
     *                    should be either the same as {@code T}, or the key or
     *                    value type if the {@code T} is {@code InvocableMap.Entry}
     * @param <K>         the output type of the key mapping function
     * @param <V>         the output type of the value mapping function
     * @param keyMapper   a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     *
     * @return a {@code Collector} which collects elements into a {@code Map}
     * whose keys and values are the result of applying mapping functions to the
     * input elements
     *
     * @apiNote It is common for either the key or the value to be the input
     * elements. In this case, the utility method {@link
     * java.util.function.Function#identity()} may be helpful. For example, the
     * following produces a {@code Map} mapping students to their grade point
     * average:
     * <pre>{@code
     *     Map<Student, Double> studentToGPA
     *         students.stream().collect(toMap(Functions.identity(),
     *                                         student ->
     * computeGPA(student)));
     * }</pre>
     * And the following produces a {@code Map} mapping a unique identifier to
     * students:
     * <pre>{@code
     *     Map<String, Student> studentIdToStudent
     *         students.stream().collect(toMap(Student::getId,
     *                                         Functions.identity());
     * }</pre>
     * @see #toMap(ValueExtractor, ValueExtractor, Remote.BinaryOperator)
     * @see #toMap(ValueExtractor, ValueExtractor, Remote.BinaryOperator, Remote.Supplier)
     */
    public static <T, U1, U2, K, V>
    RemoteCollector<T, ?, Map<K, V>> toMap(ValueExtractor<? super U1, ? extends K> keyMapper,
                                           ValueExtractor<? super U2, ? extends V> valueMapper)
        {
        return toMap(keyMapper, valueMapper, null, PortableMap::new);
        }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code Map}
     * whose keys and values are the result of applying the provided mapping
     * functions to the input elements.
     * <p>
     * If the mapped keys contains duplicates (according to {@link
     * Object#equals(Object)}), the value mapping function is applied to each
     * equal element, and the results are merged using the provided merging
     * function.
     *
     * @param <T>           the type of the stream elements
     * @param <U1>          the type of the objects to extract keys from, which
     *                      should be either the same as {@code T}, or the key or
     *                      value type if the {@code T} is {@code InvocableMap.Entry}
     * @param <U2>          the type of the objects to extract values from, which
     *                      should be either the same as {@code T}, or the key or
     *                      value type if the {@code T} is {@code InvocableMap.Entry}
     * @param <K>           the output type of the key mapping function
     * @param <V>           the output type of the value mapping function
     * @param keyMapper     a mapping function to produce keys
     * @param valueMapper   a mapping function to produce values
     * @param mergeFunction a merge function, used to resolve collisions between
     *                      values associated with the same key, as supplied to
     *                      {@link Map#merge(Object, Object, BiFunction)}
     *
     * @return a {@code Collector} which collects elements into a {@code Map}
     * whose keys are the result of applying a key mapping function to the input
     * elements, and whose values are the result of applying a value mapping
     * function to all input elements equal to the key and combining them using
     * the merge function
     *
     * @apiNote There are multiple ways to deal with collisions between multiple
     * elements mapping to the same key.  The other forms of {@code toMap}
     * simply use a merge function that throws unconditionally, but you can
     * easily write more flexible merge policies.  For example, if you have a
     * stream of {@code Person}, and you want to produce a "phone book" mapping
     * name to address, but it is possible that two persons have the same name,
     * you can do as follows to gracefully deals with these collisions, and
     * produce a {@code Map} mapping names to a concatenated list of addresses:
     * <pre>{@code
     *     Map<String, String> phoneBook
     *         people.stream().collect(toMap(Person::getName,
     *                                       Person::getAddress,
     *                                       (s, a) -> s + ", " + a));
     * }</pre>
     *
     * @see #toMap(ValueExtractor, ValueExtractor)
     * @see #toMap(ValueExtractor, ValueExtractor, Remote.BinaryOperator, Remote.Supplier)
     */
    public static <T, U1, U2, K, V>
    RemoteCollector<T, ?, Map<K, V>> toMap(
            ValueExtractor<? super U1, ? extends K> keyMapper,
            ValueExtractor<? super U2, ? extends V> valueMapper,
            Remote.BinaryOperator<V> mergeFunction)
        {
        return toMap(keyMapper, valueMapper, mergeFunction, PortableMap::new);
        }

    /**
     * Returns a {@code Collector} that accumulates elements into a {@code Map}
     * whose keys and values are the result of applying the provided mapping
     * functions to the input elements.
     * <p>
     * If the mapped keys contains duplicates (according to {@link
     * Object#equals(Object)}), the value mapping function is applied to each
     * equal element, and the results are merged using the provided merging
     * function.  The {@code Map} is created by a provided supplier function.
     *
     * @param <T>            the type of the stream elements
     * @param <U1>           the type of the objects to extract keys from, which
     *                       should be either the same as {@code T}, or the key or
     *                       value type if the {@code T} is {@code InvocableMap.Entry}
     * @param <U2>           the type of the objects to extract values from, which
     *                       should be either the same as {@code T}, or the key or
     *                       value type if the {@code T} is {@code InvocableMap.Entry}
     * @param <K>            the output type of the key mapping function
     * @param <V>            the output type of the value mapping function
     * @param <M>            the type of the resulting {@code Map}
     * @param keyExtractor   a mapping function to produce keys
     * @param valueExtractor a mapping function to produce values
     * @param mergeFunction a merge function, used to resolve collisions between
     *                      values associated with the same key, as supplied to
     *                      {@link Map#merge(Object, Object, BiFunction)}
     * @param mapSupplier   a function which returns a new, empty {@code Map}
     *                      into which the results will be inserted
     *
     * @return a {@code Collector} which collects elements into a {@code Map}
     * whose keys are the result of applying a key mapping function to the input
     * elements, and whose values are the result of applying a value mapping
     * function to all input elements equal to the key and combining them using
     * the merge function
     *
     * @see #toMap(ValueExtractor, ValueExtractor)
     * @see #toMap(ValueExtractor, ValueExtractor, Remote.BinaryOperator)
     */
    public static <T, U1, U2, K, V, M extends Map<K, V>>
    RemoteCollector<T, ?, M> toMap(
            ValueExtractor<? super U1, ? extends K> keyExtractor,
            ValueExtractor<? super U2, ? extends V> valueExtractor,
            Remote.BinaryOperator<V> mergeFunction,
            Remote.Supplier<M> mapSupplier)
        {
        ValueExtractor<? super U1, ? extends K> keyEx = Lambdas.ensureRemotable(keyExtractor);
        ValueExtractor<? super U2, ? extends V> valueEx = Lambdas.ensureRemotable(valueExtractor);

        Remote.Function<? super T, ? extends K> keyMapper =
                t -> t instanceof InvocableMap.Entry
                     ? ((InvocableMap.Entry<?, ?>) t).extract(keyEx)
                     : keyEx.extract((U1) t);

        Remote.Function<? super T, ? extends V> valueMapper =
                t -> t instanceof InvocableMap.Entry
                     ? ((InvocableMap.Entry<?, ?>) t).extract(valueEx)
                     : valueEx.extract((U2) t);

        return new MapCollector<>(keyMapper, valueMapper, mergeFunction, mapSupplier);
        }

    /**
     * Returns a {@code Collector} which applies an {@code int}-producing
     * mapping function to each input element, and returns summary statistics
     * for the resulting values.
     *
     * @param <T>        the type of the stream elements
     * @param <U>        the type of the objects to extract from, which should be
     *                   either the same as {@code T}, or the key or value type
     *                   if the {@code T} is {@code InvocableMap.Entry}
     * @param extractor  a mapping function to apply to each element
     *
     * @return a {@code Collector} implementing the summary-statistics reduction
     *
     * @see #summarizingDouble(ValueExtractor)
     * @see #summarizingLong(ValueExtractor)
     */
    public static <T, U> RemoteCollector<T, ?, IntSummaryStatistics>
    summarizingInt(ValueExtractor<? super U, ? extends Number> extractor)
        {
        ValueExtractor<? super U, ? extends Number> ex = Lambdas.ensureRemotable(extractor);

        Remote.ToIntFunction<? super T> mapper =
                t -> t instanceof InvocableMap.Entry
                     ? ((InvocableMap.Entry<?, ?>) t).extract(ex).intValue()
                     : ex.extract((U) t).intValue();

        return new SummarizingIntCollector<>(mapper);
        }

    /**
     * Returns a {@code Collector} which applies an {@code long}-producing
     * mapping function to each input element, and returns summary statistics
     * for the resulting values.
     *
     * @param <T>        the type of the stream elements
     * @param <U>        the type of the objects to extract from, which should be
     *                   either the same as {@code T}, or the key or value type
     *                   if the {@code T} is {@code InvocableMap.Entry}
     * @param extractor  a mapping function to apply to each element
     *
     * @return a {@code Collector} implementing the summary-statistics reduction
     *
     * @see #summarizingDouble(ValueExtractor)
     * @see #summarizingInt(ValueExtractor)
     */
    public static <T, U> RemoteCollector<T, ?, LongSummaryStatistics>
    summarizingLong(ValueExtractor<? super U, ? extends Number> extractor)
        {
        ValueExtractor<? super U, ? extends Number> ex = Lambdas.ensureRemotable(extractor);

        Remote.ToLongFunction<? super T> mapper =
                t -> t instanceof InvocableMap.Entry
                     ? ((InvocableMap.Entry<?, ?>) t).extract(ex).longValue()
                     : ex.extract((U) t).longValue();

        return new SummarizingLongCollector<>(mapper);
        }

    /**
     * Returns a {@code Collector} which applies an {@code double}-producing
     * mapping function to each input element, and returns summary statistics
     * for the resulting values.
     *
     * @param <T>        the type of the stream elements
     * @param <U>        the type of the objects to extract from, which should be
     *                   either the same as {@code T}, or the key or value type
     *                   if the {@code T} is {@code InvocableMap.Entry}
     * @param extractor  a mapping function to apply to each element
     *
     * @return a {@code Collector} implementing the summary-statistics reduction
     *
     * @see #summarizingLong(ValueExtractor)
     * @see #summarizingInt(ValueExtractor)
     */
    public static <T, U> RemoteCollector<T, ?, DoubleSummaryStatistics>
    summarizingDouble(ValueExtractor<? super U, ? extends Number> extractor)
        {
        ValueExtractor<? super U, ? extends Number> ex = Lambdas.ensureRemotable(extractor);

        Remote.ToDoubleFunction<? super T> mapper =
                t -> t instanceof InvocableMap.Entry
                     ? ((InvocableMap.Entry<?, ?>) t).extract(ex).doubleValue()
                     : ex.extract((U) t).doubleValue();

        return new SummarizingDoubleCollector<>(mapper);
        }
    }
