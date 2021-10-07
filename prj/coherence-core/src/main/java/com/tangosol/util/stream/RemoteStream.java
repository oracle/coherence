/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.stream;

import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.internal.util.stream.StreamSupport;

import com.tangosol.util.Filters;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.comparator.ExtractorComparator;
import com.tangosol.util.comparator.InverseComparator;

import com.tangosol.util.function.Remote;

import java.util.Comparator;
import java.util.Optional;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * This interface is an extension of {@code java.util.stream.Stream} that captures
 * lambdas used as method arguments as serializable lambdas.
 *
 * @param <T> the type of the stream elements
 *
 * @author as  2014.08.11
 * @since  12.2.1
 *
 * @see RemoteIntStream
 * @see RemoteLongStream
 * @see RemoteDoubleStream
 * @see <a href="package-summary.html">com.tangosol.util.stream</a>
 */
public interface RemoteStream<T>
        extends Stream<T>, BaseRemoteStream<T, Stream<T>>
    {
    /**
     * Create a {@link RemoteStream} of specified map's entries.
     *
     * @param map  the map to create a remote stream for
     * @param <K>  the type of map keys
     * @param <V>  the type of map values
     *
     * @return a {@link RemoteStream} of specified map's entries
     */
    public static <K, V> RemoteStream<InvocableMap.Entry<K, V>> entrySet(InvocableMap<K, V> map)
        {
        return StreamSupport
                .entryStream(map, true, null, Filters.always());
        }

    /**
     * Create a {@link RemoteStream} of specified map's keys.
     *
     * @param map  the map to create a remote stream for
     * @param <K>  the type of map keys
     * @param <V>  the type of map values
     *
     * @return a {@link RemoteStream} of specified map's keys
     */
    public static <K, V> RemoteStream<K> keySet(InvocableMap<K, V> map)
        {
        return StreamSupport
                .entryStream(map, true, null, Filters.always())
                .map(InvocableMap.Entry::getKey);
        }

    /**
     * Create a {@link RemoteStream} of specified map's values.
     *
     * @param map  the map to create a remote stream for
     * @param <K>  the type of map keys
     * @param <V>  the type of map values
     *
     * @return a {@link RemoteStream} of specified map's values
     */
    public static <K, V> RemoteStream<V> values(InvocableMap<K, V> map)
        {
        return StreamSupport
                .entryStream(map, true, null, Filters.always())
                .map(InvocableMap.Entry::getValue);
        }

    /**
     * Returns an equivalent stream that is sequential.  May return itself,
     * either because the stream was already sequential, or because the
     * underlying stream state was modified to be sequential.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @return a sequential stream
     */
    RemoteStream<T> sequential();

    /**
     * Returns an equivalent stream that is parallel.  May return itself, either
     * because the stream was already parallel, or because the underlying stream
     * state was modified to be parallel.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @return a parallel stream
     */
    RemoteStream<T> parallel();

    /**
     * Returns an equivalent stream that is <em>unordered</em>.
     * May return itself, either because the stream was already unordered, or
     * because the underlying stream state670G was modified to be unordered.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @return an unordered stream
     */
    RemoteStream<T> unordered();

    /**
     * Returns a stream consisting of the elements of this stream that match the
     * given predicate.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param predicate a non-interfering, stateless predicate to apply to each
     *                  element to determine if it should be included
     *
     * @return the new stream
     */
    RemoteStream<T> filter(Predicate<? super T> predicate);

    /**
     * Returns a stream consisting of the elements of this stream that match the
     * given predicate.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param predicate a non-interfering, stateless predicate to apply to each
     *                  element to determine if it should be included
     *
     * @return the new stream
     */
    default RemoteStream<T> filter(Remote.Predicate<? super T> predicate)
        {
        return filter((Predicate<? super T>) predicate);
        }

    /**
     * Returns a stream consisting of the results of applying the given function
     * to the elements of this stream.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element
     *
     * @return the new stream
     */
    <R> RemoteStream<R> map(Function<? super T, ? extends R> mapper);

    /**
     * Returns a stream consisting of the results of applying the given function
     * to the elements of this stream.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param <R>    the type of resulting stream elements
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element
     *
     * @return the new stream
     */
    default <R> RemoteStream<R> map(Remote.Function<? super T, ? extends R> mapper)
        {
        return map((Function<? super T, ? extends R>) mapper);
        }

    /**
     * Returns a stream consisting of the results of applying the given extractor
     * to the elements of this stream.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param <R>    the type of resulting stream elements
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element
     *
     * @return the new stream
     */
    default <R> RemoteStream<R> map(ValueExtractor<? super T, ? extends R> mapper)
        {
        return map((Function<? super T, ? extends R>) Lambdas.ensureRemotable(mapper));
        }

    /**
     * Returns an {@code IntStream} consisting of the results of applying the
     * given function to the elements of this stream.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element
     *               
     * @return the new stream
     */
    RemoteIntStream mapToInt(ToIntFunction<? super T> mapper);

    /**
     * Returns an {@code IntStream} consisting of the results of applying the
     * given function to the elements of this stream.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element
     *               
     * @return the new stream
     */
    default RemoteIntStream mapToInt(Remote.ToIntFunction<? super T> mapper)
        {
        return mapToInt((ToIntFunction<? super T>) mapper);
        }

    /**
     * Returns an {@code IntStream} consisting of the results of applying the
     * given extractor to the elements of this stream.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element
     *
     * @return the new stream
     */
    default RemoteIntStream mapToInt(ValueExtractor<? super T, ? extends Number> mapper)
        {
        return mapToInt((ToIntFunction<? super T>) Lambdas.ensureRemotable(mapper));
        }

    /**
     * Returns a {@code LongStream} consisting of the results of applying the
     * given function to the elements of this stream.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element
     *               
     * @return the new stream
     */
    RemoteLongStream mapToLong(ToLongFunction<? super T> mapper);

    /**
     * Returns a {@code LongStream} consisting of the results of applying the
     * given function to the elements of this stream.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element
     *               
     * @return the new stream
     */
    default RemoteLongStream mapToLong(Remote.ToLongFunction<? super T> mapper)
        {
        return mapToLong((ToLongFunction<? super T>) mapper);
        }

    /**
     * Returns an {@code LongStream} consisting of the results of applying the
     * given extractor to the elements of this stream.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element
     *
     * @return the new stream
     */
    default RemoteLongStream mapToLong(ValueExtractor<? super T, ? extends Number> mapper)
        {
        return mapToLong((ToLongFunction<? super T>) Lambdas.ensureRemotable(mapper));
        }

    /**
     * Returns a {@code DoubleStream} consisting of the results of applying the
     * given function to the elements of this stream.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element
     *               
     * @return the new stream
     */
    RemoteDoubleStream mapToDouble(ToDoubleFunction<? super T> mapper);

    /**
     * Returns a {@code DoubleStream} consisting of the results of applying the
     * given function to the elements of this stream.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element
     *               
     * @return the new stream
     */
    default RemoteDoubleStream mapToDouble(Remote.ToDoubleFunction<? super T> mapper)
        {
        return mapToDouble((ToDoubleFunction<? super T>) mapper);
        }

    /**
     * Returns an {@code DoubleStream} consisting of the results of applying the
     * given extractor to the elements of this stream.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element
     *
     * @return the new stream
     */
    default RemoteDoubleStream mapToDouble(ValueExtractor<? super T, ? extends Number> mapper)
        {
        return mapToDouble((ToDoubleFunction<? super T>) Lambdas.ensureRemotable(mapper));
        }

    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element.  Each mapped stream is
     * {@link BaseRemoteStream#close() closed} after its contents have been placed
     * into this stream.  (If a mapped stream is {@code null} an empty stream 
     * is used, instead.)
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param <R> The element type of the new stream
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element which produces a stream
     *               of new values
     *               
     * @return the new stream
     */
    <R> RemoteStream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper);

    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element.  Each mapped stream is
     * {@link BaseRemoteStream#close() closed} after its contents have been placed
     * into this stream.  (If a mapped stream is {@code null} an empty stream 
     * is used, instead.)
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param <R> The element type of the new stream
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element which produces a stream
     *               of new values
     *               
     * @return the new stream
     */
    default <R> RemoteStream<R> flatMap(Remote.Function<? super T, ? extends Stream<? extends R>> mapper)
        {
        Function<? super T, ? extends Stream<? extends R>> safeMapper =
                Remote.function(t -> t == null ? null : mapper.apply(t));
        return flatMap(safeMapper);
        }

    /**
     * Returns an {@code IntStream} consisting of the results of replacing each
     * element of this stream with the contents of a mapped stream produced by
     * applying the provided mapping function to each element.  Each mapped
     * stream is {@link BaseRemoteStream#close() closed} after its contents have been
     * placed into this stream.  (If a mapped stream is {@code null} an empty 
     * stream is used, instead.)
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element which produces a stream
     *               of new values
     *               
     * @return the new stream
     * 
     * @see #flatMap(Function)
     */
    RemoteIntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper);

    /**
     * Returns an {@code IntStream} consisting of the results of replacing each
     * element of this stream with the contents of a mapped stream produced by
     * applying the provided mapping function to each element.  Each mapped
     * stream is {@link BaseRemoteStream#close() closed} after its contents have been
     * placed into this stream.  (If a mapped stream is {@code null} an empty 
     * stream is used, instead.)
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element which produces a stream
     *               of new values
     *               
     * @return the new stream
     * 
     * @see #flatMap(Function)
     */
    default RemoteIntStream flatMapToInt(Remote.Function<? super T, ? extends IntStream> mapper)
        {
        Function<? super T, ? extends IntStream> safeMapper =
                Remote.function(t -> t == null ? null : mapper.apply(t));
        return flatMapToInt(safeMapper);
        }

    /**
     * Returns an {@code LongStream} consisting of the results of replacing each
     * element of this stream with the contents of a mapped stream produced by
     * applying the provided mapping function to each element.  Each mapped
     * stream is {@link BaseRemoteStream#close() closed} after its contents have been
     * placed into this stream.  (If a mapped stream is {@code null} an empty 
     * stream is used, instead.)
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element which produces a stream
     *               of new values
     *               
     * @return the new stream
     * 
     * @see #flatMap(Function)
     */
    RemoteLongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper);

    /**
     * Returns an {@code LongStream} consisting of the results of replacing each
     * element of this stream with the contents of a mapped stream produced by
     * applying the provided mapping function to each element.  Each mapped
     * stream is {@link BaseRemoteStream#close() closed} after its contents have been
     * placed into this stream.  (If a mapped stream is {@code null} an empty 
     * stream is used, instead.)
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element which produces a stream
     *               of new values
     *               
     * @return the new stream
     * 
     * @see #flatMap(Function)
     */
    default RemoteLongStream flatMapToLong(Remote.Function<? super T, ? extends LongStream> mapper)
        {
        Function<? super T, ? extends LongStream> safeMapper =
                Remote.function(t -> t == null ? null : mapper.apply(t));
        return flatMapToLong(safeMapper);
        }

    /**
     * Returns an {@code DoubleStream} consisting of the results of replacing
     * each element of this stream with the contents of a mapped stream produced
     * by applying the provided mapping function to each element.  Each mapped
     * stream is {@link BaseRemoteStream#close() closed} after its contents have been
     * placed into this stream.  (If a mapped stream is {@code null} an empty 
     * stream is used, instead.)
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element which produces a stream
     *               of new values
     *               
     * @return the new stream
     * 
     * @see #flatMap(Function)
     */
    RemoteDoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper);

    /**
     * Returns an {@code DoubleStream} consisting of the results of replacing
     * each element of this stream with the contents of a mapped stream produced
     * by applying the provided mapping function to each element.  Each mapped
     * stream is {@link BaseRemoteStream#close() closed} after its contents have been
     * placed into this stream.  (If a mapped stream is {@code null} an empty 
     * stream is used, instead.)
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element which produces a stream
     *               of new values
     *               
     * @return the new stream
     * 
     * @see #flatMap(Function)
     */
    default RemoteDoubleStream flatMapToDouble(Remote.Function<? super T, ? extends DoubleStream> mapper)
        {
        Function<? super T, ? extends DoubleStream> safeMapper =
                Remote.function(t -> t == null ? null : mapper.apply(t));
        return flatMapToDouble(safeMapper);
        }

    /**
     * Returns a stream consisting of the elements of this stream, additionally
     * performing the provided action on each element as elements are consumed
     * from the resulting stream.
     * <p>
     * This is an <em>intermediate operation</em>.
     * <p>
     * For parallel stream pipelines, the action may be called at whatever
     * time and in whatever thread the element is made available by the upstream
     * operation.  If the action modifies shared state, it is responsible for
     * providing the required synchronization.
     *
     * @param action a non-interfering action to perform on the elements as they
     *               are consumed from the stream
     *
     * @return the new stream
     */
    RemoteStream<T> peek(Consumer<? super T> action);

    /**
     * Returns a stream consisting of the elements of this stream, additionally
     * performing the provided action on each element as elements are consumed
     * from the resulting stream.
     * <p>
     * This is an <em>intermediate operation</em>.
     * <p>
     * For parallel stream pipelines, the action may be called at whatever
     * time and in whatever thread the element is made available by the upstream
     * operation.  If the action modifies shared state, it is responsible for
     * providing the required synchronization.
     *
     * @param action a non-interfering action to perform on the elements as they
     *               are consumed from the stream
     *
     * @return the new stream
     */
    default RemoteStream<T> peek(Remote.Consumer<? super T> action)
        {
        return peek((Consumer<? super T>) action);
        }

    /**
     * Returns a stream consisting of the elements of this stream, truncated to
     * be no longer than {@code maxSize} in length.
     * <p>
     * This is a <em>short-circuiting stateful intermediate operation</em>.
     *
     * @param maxSize the number of elements the stream should be limited to
     *
     * @return the new stream
     *
     * @throws IllegalArgumentException if {@code maxSize} is negative
     */
    Stream<T> limit(long maxSize);

    /**
     * Returns a stream consisting of the remaining elements of this stream
     * after discarding the first {@code n} elements of the stream.
     * If this stream contains fewer than {@code n} elements then an
     * empty stream will be returned.
     * <p>
     * This is a <em>stateful intermediate operation</em>.
     *
     * @param n the number of leading elements to skip
     *          
     * @return the new stream
     * 
     * @throws IllegalArgumentException if {@code n} is negative
     */
    Stream<T> skip(long n);

    /**
     * Returns a stream consisting of the distinct elements (according to
     * {@link Object#equals(Object)}) of this stream.
     * <p>
     * For ordered streams, the selection of distinct elements is stable
     * (for duplicated elements, the element appearing first in the encounter
     * order is preserved.)  For unordered streams, no stability guarantees
     * are made.
     * <p>
     * This is a <em>stateful intermediate operation</em>.
     *
     * @return the new stream
     */
    Stream<T> distinct();

    /**
     * Returns a stream consisting of the elements of this stream, sorted
     * according to natural order.  If the elements of this stream are not
     * {@code Comparable}, a {@code java.lang.ClassCastException} may be thrown
     * when the terminal operation is executed.
     * <p>
     * For ordered streams, the sort is stable.  For unordered streams, no
     * stability guarantees are made.
     * <p>
     * This is a <em>stateful intermediate operation</em>.
     *
     * @return the new stream
     */
    RemoteStream<T> sorted();

    /**
     * Returns a stream consisting of the elements of this stream, sorted
     * according to the provided {@code Comparator}.
     * <p>
     * For ordered streams, the sort is stable.  For unordered streams, no
     * stability guarantees are made.
     * <p>
     * This is a <em>stateful intermediate operation</em>.
     *
     * @param comparator a <em>non-interfering</em>, <em>stateless</em>
     *                   {@code Comparator} to be used to compare stream elements
     *                   
     * @return the new stream
     */
    RemoteStream<T> sorted(Comparator<? super T> comparator);

    /**
     * Returns a stream consisting of the elements of this stream, sorted
     * according to the provided {@code Comparator}.
     *
     * <p>For ordered streams, the sort is stable.  For unordered streams, no
     * stability guarantees are made.
     *
     * <p>This is a <em>stateful
     * intermediate operation</em>.
     *
     * @param comparator a <em>non-interfering</em>, <em>stateless</em>
     *                   {@code Comparator} to be used to compare stream elements
     *                   
     * @return the new stream
     */
    default RemoteStream<T> sorted(Remote.Comparator<? super T> comparator)
        {
        return sorted(comparator, false);
        }

    /**
     * Returns a stream consisting of the elements of this stream, sorted
     * according to the provided {@code Comparator}.
     *
     * <p>For ordered streams, the sort is stable.  For unordered streams, no
     * stability guarantees are made.
     *
     * <p>This is a <em>stateful
     * intermediate operation</em>.
     *
     * @param comparator a <em>non-interfering</em>, <em>stateless</em>
     *                   {@code Comparator} to be used to compare stream elements
     * @param fInverse   a flag specifying whether to invert the sort order
     *
     * @return the new stream
     */
    default RemoteStream<T> sorted(Remote.Comparator<? super T> comparator, boolean fInverse)
        {
        Comparator<? super T> c = fInverse ? new InverseComparator<>(comparator) : comparator;
        return sorted(c);
        }

    /**
     * Returns a stream consisting of the elements of this stream, sorted
     * according to attribute extracted by the provided {@code ValueExtractor}.
     *
     * <p>For ordered streams, the sort is stable.  For unordered streams, no
     * stability guarantees are made.
     *
     * <p>This is a <em>stateful
     * intermediate operation</em>.
     *
     * @param <U>       a super type of the value to extract from
     * @param extractor a <em>non-interfering</em>, <em>stateless</em>
     *                  {@code ValueExtractor} to be used to extract the attribute
     *                  that should be used to compare stream elements
     *
     * @return the new stream
     */
    default <U> RemoteStream<T> sorted(ValueExtractor<? super U, ? extends Comparable> extractor)
        {
        return sorted(extractor, false);
        }

    /**
     * Returns a stream consisting of the elements of this stream, sorted
     * according to attribute extracted by the provided {@code ValueExtractor}.
     *
     * <p>For ordered streams, the sort is stable.  For unordered streams, no
     * stability guarantees are made.
     *
     * <p>This is a <em>stateful
     * intermediate operation</em>.
     *
     * @param <U>       the super type of value to extract from
     * @param extractor a <em>non-interfering</em>, <em>stateless</em>
     *                  {@code ValueExtractor} to be used to extract the attribute
     *                  that should be used to compare stream elements
     * @param fInverse  a flag specifying whether to invert natural sort order
     *
     * @return the new stream
     */
    default <U> RemoteStream<T> sorted(ValueExtractor<? super U, ? extends Comparable> extractor, boolean fInverse)
        {
        Comparator<T> comparator = new ExtractorComparator(extractor);
        if (fInverse)
            {
            comparator = new InverseComparator<>(comparator);
            }

        return sorted(comparator);
        }

    /**
     * Performs an action for each element of this stream.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param action a non-interfering action to perform on the elements
     */
    void forEach(Consumer<? super T> action);

    /**
     * Performs an action for each element of this stream, in the encounter
     * order of the stream if the stream has a defined encounter order.
     * <p>
     * This is a <em>terminal operation</em>.
     * <p>
     * This operation processes the elements one at a time, in encounter
     * order if one exists.  Performing the action for one element
     * <em>happens-before</em> performing the action for subsequent elements,
     * but for any given element, the action may be performed in whatever thread
     * the library chooses.
     *
     * @param action a <em>non-interfering</em> action to perform on the elements
     *
     * @see #forEach(Consumer)
     */
    void forEachOrdered(Consumer<? super T> action);

    /**
     * Returns an array containing the elements of this stream.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @return an array containing the elements of this stream
     */
    Object[] toArray();

    /**
     * Returns an array containing the elements of this stream, using the
     * provided {@code generator} function to allocate the returned array, as
     * well as any additional arrays that might be required for a partitioned
     * execution or for resizing.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param generator a function which produces a new array of the desired
     *                  type and the provided length
     *
     * @return an array containing the elements in this stream
     *
     * @throws ArrayStoreException if the runtime type of the array returned
     *                             from the array generator is not a supertype
     *                             of the runtime type of every element in this
     *                             stream
     */
    <A> A[] toArray(IntFunction<A[]> generator);

    /**
     * Performs a reduction on the elements of this stream, using the provided
     * identity value and an associative accumulation function, and returns the
     * reduced value.  This is equivalent to:
     * <pre>{@code
     *     T result = identity;
     *     for (T element : this stream)
     *         result = accumulator.apply(result, element)
     *     return result;
     * }</pre>
     * <p>
     * but is not constrained to execute sequentially.
     * <p>
     * The {@code identity} value must be an identity for the accumulator
     * function. This means that for all {@code t}, {@code
     * accumulator.apply(identity, t)} is equal to {@code t}. The {@code
     * accumulator} function must be an associative function.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param identity    the identity value for the accumulating function
     * @param accumulator an associative, non-interfering, stateless function
     *                    for combining two values
     *
     * @return the result of the reduction
     */
    T reduce(T identity, BinaryOperator<T> accumulator);

    /**
     * Performs a reduction on the elements of this stream, using the provided
     * identity value and an associative accumulation function, and returns the
     * reduced value.  This is equivalent to:
     * <pre>{@code
     *     T result = identity;
     *     for (T element : this stream)
     *         result = accumulator.apply(result, element)
     *     return result;
     * }</pre>
     * <p>
     * but is not constrained to execute sequentially.
     * <p>
     * The {@code identity} value must be an identity for the accumulator
     * function. This means that for all {@code t}, {@code
     * accumulator.apply(identity, t)} is equal to {@code t}. The {@code
     * accumulator} function must be an associative function.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param identity    the identity value for the accumulating function
     * @param accumulator an associative, non-interfering, stateless function
     *                    for combining two values
     *
     * @return the result of the reduction
     */
    default T reduce(T identity, Remote.BinaryOperator<T> accumulator)
        {
        return reduce(identity, (BinaryOperator<T>) accumulator);
        }

    /**
     * Performs a reduction on the elements of this stream, using an associative
     * accumulation function, and returns an {@code Optional} describing the
     * reduced value, if any. This is equivalent to:
     * <pre>{@code
     *     boolean foundAny = false;
     *     T result = null;
     *     for (T element : this stream) {
     *         if (!foundAny) {
     *             foundAny = true;
     *             result = element;
     *         }
     *         else
     *             result = accumulator.apply(result, element);
     *     }
     *     return foundAny ? Optional.of(result) : Optional.empty();
     * }</pre>
     * <p>
     * but is not constrained to execute sequentially.
     * <p>
     * The {@code accumulator} function must be an associative function.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param accumulator an associative, non-interfering, stateless function
     *                    for combining two values
     *
     * @return an {@link Optional} describing the result of the reduction
     *
     * @throws NullPointerException if the result of the reduction is null
     * @see #reduce(Object, Remote.BinaryOperator)
     * @see #min(Comparator)
     * @see #max(Comparator)
     */
    Optional<T> reduce(BinaryOperator<T> accumulator);

    /**
     * Performs a reduction on the elements of this stream, using an associative
     * accumulation function, and returns an {@code Optional} describing the
     * reduced value, if any. This is equivalent to:
     * <pre>{@code
     *     boolean foundAny = false;
     *     T result = null;
     *     for (T element : this stream) {
     *         if (!foundAny) {
     *             foundAny = true;
     *             result = element;
     *         }
     *         else
     *             result = accumulator.apply(result, element);
     *     }
     *     return foundAny ? Optional.of(result) : Optional.empty();
     * }</pre>
     * <p>
     * but is not constrained to execute sequentially.
     * <p>
     * The {@code accumulator} function must be an associative function.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param accumulator an associative, non-interfering, stateless function
     *                    for combining two values
     *
     * @return an {@link Optional} describing the result of the reduction
     *
     * @throws NullPointerException if the result of the reduction is null
     * @see #reduce(Object, Remote.BinaryOperator)
     * @see #min(Comparator)
     * @see #max(Comparator)
     */
    default Optional<T> reduce(Remote.BinaryOperator<T> accumulator)
        {
        return reduce((BinaryOperator<T>) accumulator);
        }

    /**
     * Performs a reduction on the elements of this stream, using the provided
     * identity, accumulation and combining functions.  This is equivalent to:
     * <pre>{@code
     *     U result = identity;
     *     for (T element : this stream)
     *         result = accumulator.apply(result, element)
     *     return result;
     * }</pre>
     * <p>
     * but is not constrained to execute sequentially.
     * <p>
     * The {@code identity} value must be an identity for the combiner function.
     *  This means that for all {@code u}, {@code combiner(identity, u)} is
     * equal to {@code u}.  Additionally, the {@code combiner} function must be
     * compatible with the {@code accumulator} function; for all {@code u} and
     * {@code t}, the following must hold:
     * <pre>{@code
     *     combiner.apply(u, accumulator.apply(identity, t)) ==
     * accumulator.apply(u, t)
     * }</pre>
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param identity    the identity value for the combiner function
     * @param accumulator an associative, non-interfering, stateless function
     *                    for incorporating an additional element into a result
     * @param combiner    an associative, non-interfering, stateless function
     *                    for combining two values, which must be compatible
     *                    with the accumulator function
     *
     * @return the result of the reduction
     *
     * @see #reduce(Remote.BinaryOperator)
     * @see #reduce(Object, Remote.BinaryOperator)
     */
    <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner);

    /**
     * Performs a reduction on the elements of this stream, using the provided
     * identity, accumulation and combining functions.  This is equivalent to:
     * <pre>{@code
     *     U result = identity;
     *     for (T element : this stream)
     *         result = accumulator.apply(result, element)
     *     return result;
     * }</pre>
     * <p>
     * but is not constrained to execute sequentially.
     * <p>
     * The {@code identity} value must be an identity for the combiner function.
     *  This means that for all {@code u}, {@code combiner(identity, u)} is
     * equal to {@code u}.  Additionally, the {@code combiner} function must be
     * compatible with the {@code accumulator} function; for all {@code u} and
     * {@code t}, the following must hold:
     * <pre>{@code
     *     combiner.apply(u, accumulator.apply(identity, t)) ==
     * accumulator.apply(u, t)
     * }</pre>
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param <U>         the type of stream elements
     * @param identity    the identity value for the combiner function
     * @param accumulator an associative, non-interfering, stateless function
     *                    for incorporating an additional element into a result
     * @param combiner    an associative, non-interfering, stateless function
     *                    for combining two values, which must be compatible
     *                    with the accumulator function
     *
     * @return the result of the reduction
     *
     * @see #reduce(Remote.BinaryOperator)
     * @see #reduce(Object, Remote.BinaryOperator)
     */
    default <U> U reduce(U identity, Remote.BiFunction<U, ? super T, U> accumulator, Remote.BinaryOperator<U> combiner)
        {
        return reduce(identity, (BiFunction<U, ? super T, U>) accumulator, (BinaryOperator<U>) combiner);
        }

    /**
     * Performs a mutable reduction operation on the elements of this stream. A
     * mutable reduction is one in which the reduced value is a mutable result
     * container, such as an {@code ArrayList}, and elements are incorporated by
     * updating the state of the result rather than by replacing the result.
     * This produces a result equivalent to:
     * <pre>{@code
     *     R result = supplier.get();
     *     for (T element : this stream)
     *         accumulator.accept(result, element);
     *     return result;
     * }</pre>
     * <p>
     * Like {@link #reduce(Object, Remote.BinaryOperator)}, {@code collect}
     * operations can be parallelized without requiring additional
     * synchronization.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param supplier    a function that creates a new result container. For a
     *                    parallel execution, this function may be called
     *                    multiple times and must return a fresh value each
     *                    time.
     * @param accumulator an associative, non-interfering, stateless function
     *                    for incorporating an additional element into a result
     * @param combiner    an associative, non-interfering, stateless function
     *                    for combining two values, which must be compatible
     *                    with the accumulator function
     *
     * @return the result of the reduction
     */
    <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner);

    /**
     * Performs a mutable reduction operation on the elements of this stream.  A
     * mutable reduction is one in which the reduced value is a mutable result
     * container, such as an {@code ArrayList}, and elements are incorporated by
     * updating the state of the result rather than by replacing the result.
     * This produces a result equivalent to:
     * <pre>{@code
     *     R result = supplier.get();
     *     for (T element : this stream)
     *         accumulator.accept(result, element);
     *     return result;
     * }</pre>
     * <p>
     * Like {@link #reduce(Object, Remote.BinaryOperator)}, {@code collect}
     * operations can be parallelized without requiring additional
     * synchronization.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param <R>         the result type
     * @param supplier    a function that creates a new result container. For a
     *                    parallel execution, this function may be called
     *                    multiple times and must return a fresh value each
     *                    time.
     * @param accumulator an associative, non-interfering, stateless function
     *                    for incorporating an additional element into a result
     * @param combiner    an associative, non-interfering, stateless function
     *                    for combining two values, which must be compatible
     *                    with the accumulator function
     *
     * @return the result of the reduction
     */
    default <R> R collect(Remote.Supplier<R> supplier, Remote.BiConsumer<R, ? super T> accumulator, Remote.BiConsumer<R, R> combiner)
        {
        return collect((Supplier<R>) supplier, (BiConsumer<R, ? super T>) accumulator, (BiConsumer<R, R>) combiner);
        }

    /**
     * Performs a mutable reduction operation on the elements of this stream
     * using a {@code Collector}.  A {@code Collector} encapsulates the
     * functions used as arguments to {@link #collect(Remote.Supplier,
     * Remote.BiConsumer, Remote.BiConsumer)}, allowing for reuse of collection
     * strategies and composition of collect operations such as multiple-level
     * grouping or partitioning.
     * <p>
     * If the stream is parallel, and the {@code Collector} is {@link
     * RemoteCollector.Characteristics#CONCURRENT concurrent}, and either the stream
     * is unordered or the collector is {@link RemoteCollector.Characteristics#UNORDERED
     * unordered}, then a concurrent reduction will be performed (see {@link
     * RemoteCollector} for details on concurrent reduction.)
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param collector the {@code Collector} describing the reduction
     *
     * @return the result of the reduction
     *
     * @see #collect(Remote.Supplier, Remote.BiConsumer, Remote.BiConsumer)
     */
    default <R, A> R collect(Collector<? super T, A, R> collector)
        {
        if (collector instanceof RemoteCollector)
            {
            return collect((RemoteCollector<? super T, A, R>) collector);
            }

        throw new UnsupportedOperationException("java.util.stream.Collector is not supported. "
                                                + "Please use com.tangosol.util.stream.RemoteCollector instead.");
        }

    /**
     * Performs a mutable reduction operation on the elements of this stream
     * using a {@code Collector}.  A {@code Collector} encapsulates the
     * functions used as arguments to {@link #collect(Remote.Supplier,
     * Remote.BiConsumer, Remote.BiConsumer)}, allowing for reuse of collection
     * strategies and composition of collect operations such as multiple-level
     * grouping or partitioning.
     * <p>
     * If the stream is parallel, and the {@code Collector} is {@link
     * RemoteCollector.Characteristics#CONCURRENT concurrent}, and either the stream
     * is unordered or the collector is {@link RemoteCollector.Characteristics#UNORDERED
     * unordered}, then a concurrent reduction will be performed (see {@link
     * RemoteCollector} for details on concurrent reduction.)
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param <R>       the type of the result
     * @param <A>       the intermediate accumulation type of the Collector
     * @param collector the {@code Collector} describing the reduction
     *
     * @return the result of the reduction
     *
     * @see #collect(Remote.Supplier, Remote.BiConsumer, Remote.BiConsumer)
     */
    <R, A> R collect(RemoteCollector<? super T, A, R> collector);

    /**
     * Returns the minimum element of this stream according to the provided
     * {@code Comparator}.  This is a special case of a reduction.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param comparator a non-interfering, stateless {@code Comparator} to
     *                   compare elements of this stream
     *
     * @return an {@code Optional} describing the minimum element of this
     * stream, or an empty {@code Optional} if the stream is empty
     */
    Optional<T> min(Comparator<? super T> comparator);

    /**
     * Returns the minimum element of this stream according to the provided
     * {@code Comparator}.  This is a special case of a reduction.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param comparator a non-interfering, stateless {@code Comparator} to
     *                   compare elements of this stream
     *
     * @return an {@code Optional} describing the minimum element of this
     * stream, or an empty {@code Optional} if the stream is empty
     */
    default Optional<T> min(Remote.Comparator<? super T> comparator)
        {
        return min((Comparator<? super T>) comparator);
        }

    /**
     * Returns the minimum element of this stream according to the attribute
     * extracted by the provided {@code ValueExtractor}. This is a special case of a reduction.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param <U>       a super type of the value to extract from
     * @param extractor a <em>non-interfering</em>, <em>stateless</em>
     *                  {@code ValueExtractor} to be used to extract the attribute
     *                  that should be used to compare stream elements
     *
     * @return an {@code Optional} describing the minimum element of this
     * stream, or an empty {@code Optional} if the stream is empty
     */
    default <U> Optional<T> min(ValueExtractor<? super U, ? extends Comparable> extractor)
        {
        return min(new ExtractorComparator(extractor));
        }

    /**
     * Returns the maximum element of this stream according to the provided
     * {@code Comparator}.  This is a special case of a reduction.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param comparator a non-interfering, stateless {@code Comparator} to
     *                   compare elements of this stream
     *
     * @return an {@code Optional} describing the maximum element of this
     * stream, or an empty {@code Optional} if the stream is empty
     */
    Optional<T> max(Comparator<? super T> comparator);

    /**
     * Returns the maximum element of this stream according to the provided
     * {@code Comparator}.  This is a special case of a reduction.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param comparator a non-interfering, stateless {@code Comparator} to
     *                   compare elements of this stream
     *
     * @return an {@code Optional} describing the maximum element of this
     * stream, or an empty {@code Optional} if the stream is empty
     */
    default Optional<T> max(Remote.Comparator<? super T> comparator)
        {
        return max((Comparator<? super T>) comparator);
        }

    /**
     * Returns the maximum element of this stream according to the attribute
     * extracted by the provided {@code ValueExtractor}. This is a special case of a reduction.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param <U>       a super type of the value to extract from
     * @param extractor a <em>non-interfering</em>, <em>stateless</em>
     *                  {@code ValueExtractor} to be used to extract the attribute
     *                  that should be used to compare stream elements
     *
     * @return an {@code Optional} describing the maximum element of this
     * stream, or an empty {@code Optional} if the stream is empty
     */
    default <U> Optional<T> max(ValueExtractor<? super U, ? extends Comparable> extractor)
        {
        return max(new ExtractorComparator(extractor));
        }

    /**
     * Returns the count of elements in this stream.  This is a special case of
     * a reduction and is equivalent to:
     * <pre>{@code
     *     return mapToLong(e -> 1L).sum();
     * }</pre>
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @return the count of elements in this stream
     */
    long count();

    /**
     * Returns whether any elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for
     * determining the result.
     * <p>
     * This is a <em>short-circuiting terminal operation</em>.
     *
     * @param predicate a non-interfering, stateless predicate to apply to
     *                  elements of this stream
     *
     * @return {@code true} if any elements of the stream match the provided
     * predicate, otherwise {@code false}
     */
    boolean anyMatch(Predicate<? super T> predicate);

    /**
     * Returns whether any elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for
     * determining the result.
     * <p>
     * This is a <em>short-circuiting terminal operation</em>.
     *
     * @param predicate a non-interfering, stateless predicate to apply to
     *                  elements of this stream
     *
     * @return {@code true} if any elements of the stream match the provided
     * predicate, otherwise {@code false}
     */
    default boolean anyMatch(Remote.Predicate<? super T> predicate)
        {
        return anyMatch((Predicate<? super T>) predicate);
        }

    /**
     * Returns whether all elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for
     * determining the result.
     * <p>
     * This is a <em>short-circuiting terminal operation</em>.
     *
     * @param predicate a non-interfering, stateless predicate to apply to
     *                  elements of this stream
     *
     * @return {@code true} if either all elements of the stream match the
     * provided predicate or the stream is empty, otherwise {@code false}
     */
    boolean allMatch(Predicate<? super T> predicate);

    /**
     * Returns whether all elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for
     * determining the result.
     * <p>
     * This is a <em>short-circuiting terminal operation</em>.
     *
     * @param predicate a non-interfering, stateless predicate to apply to
     *                  elements of this stream
     *
     * @return {@code true} if either all elements of the stream match the
     * provided predicate or the stream is empty, otherwise {@code false}
     */
    default boolean allMatch(Remote.Predicate<? super T> predicate)
        {
        return allMatch((Predicate<? super T>) predicate);
        }

    /**
     * Returns whether no elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for
     * determining the result.
     * <p>
     * This is a <em>short-circuiting terminal operation</em>.
     *
     * @param predicate a non-interfering, stateless predicate to apply to
     *                  elements of this stream
     *
     * @return {@code true} if either no elements of the stream match the
     * provided predicate or the stream is empty, otherwise {@code false}
     */
    boolean noneMatch(Predicate<? super T> predicate);

    /**
     * Returns whether no elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for
     * determining the result.
     * <p>
     * This is a <em>short-circuiting terminal operation</em>.
     *
     * @param predicate a non-interfering, stateless predicate to apply to
     *                  elements of this stream
     *
     * @return {@code true} if either no elements of the stream match the
     * provided predicate or the stream is empty, otherwise {@code false}
     */
    default boolean noneMatch(Remote.Predicate<? super T> predicate)
        {
        return noneMatch((Predicate<? super T>) predicate);
        }

    /**
     * Returns an {@link Optional} describing the first element of this stream,
     * or an empty {@code Optional} if the stream is empty.  If the stream has
     * no encounter order, then any element may be returned.
     * <p>
     * This is a <em>short-circuiting terminal operation</em>.
     *
     * @return an {@code Optional} describing the first element of this stream,
     * or an empty {@code Optional} if the stream is empty
     *
     * @throws NullPointerException if the element selected is null
     */
    Optional<T> findFirst();

    /**
     * Returns an {@link Optional} describing some element of the stream, or an
     * empty {@code Optional} if the stream is empty.
     * <p>
     * This is a <em>short-circuiting terminal operation</em>.
     * <p>
     * The behavior of this operation is explicitly nondeterministic; it is free
     * to select any element in the stream.  This is to allow for maximal
     * performance in parallel operations; the cost is that multiple invocations
     * on the same source may not return the same result.  (If a stable result
     * is desired, use {@link #findFirst()} instead.)
     *
     * @return an {@code Optional} describing some element of this stream, or an
     * empty {@code Optional} if the stream is empty
     *
     * @throws NullPointerException if the element selected is null
     *
     * @see #findFirst()
     */
    Optional<T> findAny();

    // ---- helpers ---------------------------------------------------------

    /**
     * Convert stream of numbers into {@link RemoteIntStream}.
     *
     * @param <T>     the type of input stream elements
     * @param stream  the stream of numbers
     *
     * @return a {@code RemoteIntStream} instance
     */
    public static <T extends Number> RemoteIntStream toIntStream(RemoteStream<T> stream)
        {
        return stream.mapToInt(Number::intValue);
        }

    /**
     * Convert stream of numbers into {@link RemoteLongStream}.
     *
     * @param <T>     the type of input stream elements
     * @param stream  the stream of numbers
     *
     * @return a {@code RemoteLongStream} instance
     */
    public static <T extends Number> RemoteLongStream toLongStream(RemoteStream<T> stream)
        {
        return stream.mapToLong(Number::longValue);
        }

    /**
     * Convert stream of numbers into {@link RemoteDoubleStream}.
     *
     * @param <T>     the type of input stream elements
     * @param stream  the stream of numbers
     *
     * @return a {@code RemoteDoubleStream} instance
     */
    public static <T extends Number> RemoteDoubleStream toDoubleStream(RemoteStream<T> stream)
        {
        return stream.mapToDouble(Number::doubleValue);
        }
    }
