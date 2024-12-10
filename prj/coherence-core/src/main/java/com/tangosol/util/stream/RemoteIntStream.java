/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.stream;

import com.tangosol.internal.util.IntSummaryStatistics;

import com.tangosol.util.function.Remote;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.PrimitiveIterator;
import java.util.Spliterator;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;

/**
 * A sequence of primitive int-valued elements supporting sequential and
 * parallel aggregate operations.  This is the {@code int} primitive
 * specialization of {@link RemoteStream}.
 * <p>
 * The following example illustrates an aggregate operation using {@link
 * RemoteStream} and {@link RemoteIntStream}, computing the sum of the weights of the red
 * widgets:
 * <pre>{@code
 *     int sum = widgets.stream()
 *                      .filter(w -> w.getColor() == RED)
 *                      .mapToInt(w -> w.getWeight())
 *                      .sum();
 * }</pre>
 * <p>
 * This interface is an extension of {@code java.util.stream.IntStream} that
 * captures lambdas used as method arguments as serializable lambdas.
 *
 * @author as  2014.09.11
 * @since 12.2.1
 *
 * @see RemoteStream
 * @see <a href="package-summary.html">com.tangosol.util.stream</a>
 */
public interface RemoteIntStream
        extends java.util.stream.IntStream, BaseRemoteStream<Integer, java.util.stream.IntStream>
    {
    /**
     * Returns a stream consisting of the elements of this stream that match
     * the given predicate.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param predicate a <em>non-interfering</em>, <em>stateless</em>
     *                  predicate to apply to each element to determine if it
     *                  should be included
     *                  
     * @return the new stream
     */
    RemoteIntStream filter(IntPredicate predicate);

    /**
     * Returns a stream consisting of the elements of this stream that match
     * the given predicate.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param predicate a <em>non-interfering</em>, <em>stateless</em>
     *                  predicate to apply to each element to determine if it
     *                  should be included
     *                  
     * @return the new stream
     */
    default RemoteIntStream filter(Remote.IntPredicate predicate)
        {
        return filter((IntPredicate) predicate);
        }

    /**
     * Returns a stream consisting of the results of applying the given
     * function to the elements of this stream.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element
     *               
     * @return the new stream
     */
    RemoteIntStream map(IntUnaryOperator mapper);

    /**
     * Returns a stream consisting of the results of applying the given
     * function to the elements of this stream.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element
     *               
     * @return the new stream
     */
    default RemoteIntStream map(Remote.IntUnaryOperator mapper)
        {
        return map((IntUnaryOperator) mapper);
        }

    /**
     * Returns an object-valued {@code Stream} consisting of the results of
     * applying the given function to the elements of this stream.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param <U> the element type of the new stream
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element
     *               
     * @return the new stream
     */
    <U> RemoteStream<U> mapToObj(IntFunction<? extends U> mapper);

    /**
     * Returns an object-valued {@code Stream} consisting of the results of
     * applying the given function to the elements of this stream.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @param <U> the element type of the new stream
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element
     *               
     * @return the new stream
     */
    default <U> RemoteStream<U> mapToObj(Remote.IntFunction<? extends U> mapper)
        {
        return mapToObj((IntFunction<? extends U>) mapper);
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
    RemoteLongStream mapToLong(IntToLongFunction mapper);

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
    default RemoteLongStream mapToLong(Remote.IntToLongFunction mapper)
        {
        return mapToLong((IntToLongFunction) mapper);
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
    RemoteDoubleStream mapToDouble(IntToDoubleFunction mapper);

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
    default RemoteDoubleStream mapToDouble(Remote.IntToDoubleFunction mapper)
        {
        return mapToDouble((IntToDoubleFunction) mapper);
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
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element which produces a
     *               {@code IntStream} of new values
     *               
     * @return the new stream
     * 
     * @see RemoteStream#flatMap(Function)
     */
    RemoteIntStream flatMap(IntFunction<? extends java.util.stream.IntStream> mapper);

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
     * @param mapper a <em>non-interfering</em>, <em>stateless</em>
     *               function to apply to each element which produces a
     *               {@code IntStream} of new values
     *               
     * @return the new stream
     * 
     * @see RemoteStream#flatMap(Function)
     */
    default RemoteIntStream flatMap(Remote.IntFunction<? extends java.util.stream.IntStream> mapper)
        {
        return flatMap((IntFunction<? extends java.util.stream.IntStream>) mapper);
        }

    /**
     * Returns a stream consisting of the elements of this stream, additionally
     * performing the provided action on each element as elements are consumed
     * from the resulting stream.
     * <p>
     * This is an <em>intermediate operation</em>.
     * <p>
     * For parallel stream pipelines, the action may be called at
     * whatever time and in whatever thread the element is made available by the
     * upstream operation.  If the action modifies shared state,
     * it is responsible for providing the required synchronization.
     *
     * @param action a <em>non-interfering</em> action to perform on the elements as
     *               they are consumed from the stream
     *               
     * @return the new stream
     */
    RemoteIntStream peek(IntConsumer action);

    /**
     * Returns a stream consisting of the elements of this stream, additionally
     * performing the provided action on each element as elements are consumed
     * from the resulting stream.
     * <p>
     * This is an <em>intermediate operation</em>.
     * <p>
     * For parallel stream pipelines, the action may be called at
     * whatever time and in whatever thread the element is made available by the
     * upstream operation.  If the action modifies shared state,
     * it is responsible for providing the required synchronization.
     *
     * @param action a <em>non-interfering</em> action to perform on the elements as
     *               they are consumed from the stream
     *               
     * @return the new stream
     */
    default RemoteIntStream peek(Remote.IntConsumer action)
        {
        return peek((IntConsumer) action);
        }

    /**
     * Returns a stream consisting of the distinct elements of this stream.
     * <p>
     * This is a <em>stateful intermediate operation</em>.
     *
     * @return the new stream
     */
    java.util.stream.IntStream distinct();

    /**
     * Returns a stream consisting of the elements of this stream in sorted
     * order.
     * <p>
     * This is a <em>stateful intermediate operation</em>.
     *
     * @return the new stream
     */
    java.util.stream.IntStream sorted();

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
    java.util.stream.IntStream limit(long maxSize);

    /**
     * Returns a stream consisting of the remaining elements of this stream
     * after discarding the first {@code n} elements of the stream. If this
     * stream contains fewer than {@code n} elements then an empty stream will
     * be returned.
     * <p>
     * This is a <em>stateful intermediate operation</em>.
     *
     * @param n the number of leading elements to skip
     *
     * @return the new stream
     *
     * @throws IllegalArgumentException if {@code n} is negative
     */
    java.util.stream.IntStream skip(long n);

    /**
     * Performs an action for each element of this stream.
     * <p>
     * This is a <em>terminal operation</em>.
     * <p>
     * For parallel stream pipelines, this operation does <em>not</em>
     * guarantee to respect the encounter order of the stream, as doing so would
     * sacrifice the benefit of parallelism.  For any given element, the action
     * may be performed at whatever time and in whatever thread the library
     * chooses.  If the action accesses shared state, it is responsible for
     * providing the required synchronization.
     *
     * @param action a <em>non-interfering</em> action to perform on the elements
     */
    void forEach(IntConsumer action);

    /**
     * Performs an action for each element of this stream, guaranteeing that
     * each element is processed in encounter order for streams that have a
     * defined encounter order.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param action a <em>non-interfering</em> action to perform on the elements
     *
     * @see #forEach(IntConsumer)
     */
    void forEachOrdered(IntConsumer action);

    /**
     * Returns an array containing the elements of this stream.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @return an array containing the elements of this stream
     */
    int[] toArray();

    /**
     * Performs a <em>reduction</em> on the elements of this stream, using the 
     * provided identity value and an <em>associative</em> accumulation function, 
     * and returns the reduced value.  This is equivalent to:
     * <pre>{@code
     *     int result = identity;
     *     for (int element : this stream)
     *         result = accumulator.applyAsInt(result, element)
     *     return result;
     * }</pre>
     *
     * but is not constrained to execute sequentially.
     * <p>
     * The {@code identity} value must be an identity for the accumulator
     * function. This means that for all {@code x},
     * {@code accumulator.apply(identity, x)} is equal to {@code x}.
     * The {@code accumulator} function must be an <em>associative</em> function.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param identity the identity value for the accumulating function
     * @param op an <em>associative</em>, <em>non-interfering</em>, <em>stateless</em>
     *           function for combining two values
     *           
     * @return the result of the reduction
     * 
     * @see #sum()
     * @see #min()
     * @see #max()
     * @see #average()
     */
    int reduce(int identity, IntBinaryOperator op);

    /**
     * Performs a <em>reduction</em> on the elements of this stream, using the 
     * provided identity value and an <em>associative</em> accumulation function, 
     * and returns the reduced value.  This is equivalent to:
     * <pre>{@code
     *     int result = identity;
     *     for (int element : this stream)
     *         result = accumulator.applyAsInt(result, element)
     *     return result;
     * }</pre>
     *
     * but is not constrained to execute sequentially.
     * <p>
     * The {@code identity} value must be an identity for the accumulator
     * function. This means that for all {@code x},
     * {@code accumulator.apply(identity, x)} is equal to {@code x}.
     * The {@code accumulator} function must be an <em>associative</em> function.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param identity the identity value for the accumulating function
     * @param op an <em>associative</em>, <em>non-interfering</em>, <em>stateless</em>
     *           function for combining two values
     *           
     * @return the result of the reduction
     * 
     * @see #sum()
     * @see #min()
     * @see #max()
     * @see #average()
     */
    default int reduce(int identity, Remote.IntBinaryOperator op)
        {
        return reduce(identity, (IntBinaryOperator) op);
        }

    /**
     * Performs a <em>reduction</em> on the elements of this stream, using an
     * <em>associative</em> accumulation function, and returns an 
     * {@code OptionalInt} describing the
     * reduced value, if any. This is equivalent to:
     * <pre>{@code
     *     boolean foundAny = false;
     *     int result = null;
     *     for (int element : this stream) {
     *         if (!foundAny) {
     *             foundAny = true;
     *             result = element;
     *         }
     *         else
     *             result = accumulator.applyAsInt(result, element);
     *     }
     *     return foundAny ? OptionalInt.of(result) : OptionalInt.empty();
     * }</pre>
     * 
     * but is not constrained to execute sequentially.
     * <p>
     * The {@code accumulator} function must be an <em>associative</em> function.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param op an <em>associative</em>, <em>non-interfering</em>, <em>stateless</em>
     *           function for combining two values
     *           
     * @return the result of the reduction
     * 
     * @see #reduce(int, IntBinaryOperator)
     */
    OptionalInt reduce(IntBinaryOperator op);

    /**
     * Performs a <em>reduction</em> on the elements of this stream, using an
     * <em>associative</em> accumulation function, and returns an 
     * {@code OptionalInt} describing the
     * reduced value, if any. This is equivalent to:
     * <pre>{@code
     *     boolean foundAny = false;
     *     int result = null;
     *     for (int element : this stream) {
     *         if (!foundAny) {
     *             foundAny = true;
     *             result = element;
     *         }
     *         else
     *             result = accumulator.applyAsInt(result, element);
     *     }
     *     return foundAny ? OptionalInt.of(result) : OptionalInt.empty();
     * }</pre>
     * 
     * but is not constrained to execute sequentially.
     * <p>
     * The {@code accumulator} function must be an <em>associative</em> function.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param op an <em>associative</em>, <em>non-interfering</em>, <em>stateless</em>
     *           function for combining two values
     *           
     * @return the result of the reduction
     * 
     * @see #reduce(int, IntBinaryOperator)
     */
    default OptionalInt reduce(Remote.IntBinaryOperator op)
        {
        return reduce((IntBinaryOperator) op);
        }

    /**
     * Performs a <em>mutable reduction</em> operation on the elements of this 
     * stream.  A mutable reduction is one in which the reduced value is a 
     * mutable result container, such as an {@code ArrayList}, and elements are 
     * incorporated by updating the state of the result rather than by replacing 
     * the result. This produces a result equivalent to:
     * <pre>{@code
     *     R result = supplier.get();
     *     for (int element : this stream)
     *         accumulator.accept(result, element);
     *     return result;
     * }</pre>
     * <p>
     * Like {@link #reduce(int, IntBinaryOperator)}, {@code collect}
     * operations can be parallelized without requiring additional
     * synchronization.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param <R>         type of the result
     * @param supplier    a function that creates a new result container. For a
     *                    parallel execution, this function may be called
     *                    multiple times and must return a fresh value each
     *                    time.
     * @param accumulator an <a href="package-summary.html#Associativity">associative</a>,
     *                    <em>non-interfering</em>,
     *                    <em>stateless</em>
     *                    function for incorporating an additional element into
     *                    a result
     * @param combiner    an <a href="package-summary.html#Associativity">associative</a>,
     *                    <em>non-interfering</em>,
     *                    <em>stateless</em>
     *                    function for combining two values, which must be
     *                    compatible with the accumulator function
     *
     * @return the result of the reduction
     *
     * @see java.util.stream.Stream#collect(Supplier, BiConsumer, BiConsumer)
     */
    <R> R collect(Supplier<R> supplier,
                  ObjIntConsumer<R> accumulator,
                  BiConsumer<R, R> combiner);

    /**
     * Performs a <em>mutable reduction</em> operation on the elements of this 
     * stream.  A mutable reduction is one in which the reduced value is a 
     * mutable result container, such as an {@code ArrayList}, and elements are 
     * incorporated by updating the state of the result rather than by replacing 
     * the result. This produces a result equivalent to:
     * <pre>{@code
     *     R result = supplier.get();
     *     for (int element : this stream)
     *         accumulator.accept(result, element);
     *     return result;
     * }</pre>
     * <p>
     * Like {@link #reduce(int, IntBinaryOperator)}, {@code collect}
     * operations can be parallelized without requiring additional
     * synchronization.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param <R>         type of the result
     * @param supplier    a function that creates a new result container. For a
     *                    parallel execution, this function may be called
     *                    multiple times and must return a fresh value each
     *                    time.
     * @param accumulator an <a href="package-summary.html#Associativity">associative</a>,
     *                    <em>non-interfering</em>,
     *                    <em>stateless</em>
     *                    function for incorporating an additional element into
     *                    a result
     * @param combiner    an <a href="package-summary.html#Associativity">associative</a>,
     *                    <em>non-interfering</em>,
     *                    <em>stateless</em>
     *                    function for combining two values, which must be
     *                    compatible with the accumulator function
     *
     * @return the result of the reduction
     *
     * @see java.util.stream.Stream#collect(Supplier, BiConsumer, BiConsumer)
     */
    default <R> R collect(Remote.Supplier<R> supplier,
                  Remote.ObjIntConsumer<R> accumulator,
                  Remote.BiConsumer<R, R> combiner)
        {
        return collect((Supplier<R>) supplier,
                       (ObjIntConsumer<R>) accumulator,
                       (BiConsumer<R, R>) combiner);
        }

    /**
     * Returns the sum of elements in this stream.  This is a special case of a
     * <em>reduction</em> and is equivalent to:
     * <pre>{@code
     *     return reduce(0, Integer::sum);
     * }</pre>
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @return the sum of elements in this stream
     */
    int sum();

    /**
     * Returns an {@code OptionalInt} describing the minimum element of this
     * stream, or an empty optional if this stream is empty.  This is a special
     * case of a <em>reduction</em> and is equivalent to:
     * <pre>{@code
     *     return reduce(Integer::min);
     * }</pre>
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @return an {@code OptionalInt} containing the minimum element of this
     * stream, or an empty {@code OptionalInt} if the stream is empty
     */
    OptionalInt min();

    /**
     * Returns an {@code OptionalInt} describing the maximum element of this
     * stream, or an empty optional if this stream is empty.  This is a special
     * case of a <em>reduction</em> and is equivalent to:
     * <pre>{@code
     *     return reduce(Integer::max);
     * }</pre>
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @return an {@code OptionalInt} containing the maximum element of this
     * stream, or an empty {@code OptionalInt} if the stream is empty
     */
    OptionalInt max();

    /**
     * Returns the count of elements in this stream.  This is a special case of
     * a <em>reduction</em> and is equivalent to:
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
     * Returns an {@code OptionalDouble} describing the arithmetic mean of
     * elements of this stream, or an empty optional if this stream is empty.
     * This is a special case of a <em>reduction</em>.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @return an {@code OptionalDouble} containing the average element of this
     * stream, or an empty optional if the stream is empty
     */
    OptionalDouble average();

    /**
     * Returns an {@code IntSummaryStatistics} describing various summary data
     * about the elements of this stream.  This is a special case of a
     * <em>reduction</em>.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @return an {@code IntSummaryStatistics} describing various summary data
     * about the elements of this stream
     */
    IntSummaryStatistics summaryStatistics();

    /**
     * Returns whether any elements of this stream match the provided
     * predicate.  May not evaluate the predicate on all elements if not
     * necessary for determining the result.  If the stream is empty then
     * {@code false} is returned and the predicate is not evaluated.
     * <p>
     * This is a <em>short-circuiting terminal operation</em>.
     *
     * @param predicate a <em>non-interfering</em>, <em>stateless</em>
     *                  predicate to apply to elements of this stream
     *
     * @return {@code true} if any elements of the stream match the provided
     * predicate, otherwise {@code false}
     */
    boolean anyMatch(IntPredicate predicate);

    /**
     * Returns whether any elements of this stream match the provided
     * predicate.  May not evaluate the predicate on all elements if not
     * necessary for determining the result.  If the stream is empty then
     * {@code false} is returned and the predicate is not evaluated.
     * <p>
     * This is a <em>short-circuiting terminal operation</em>.
     *
     * @param predicate a <em>non-interfering</em>, <em>stateless</em>
     *                  predicate to apply to elements of this stream
     *
     * @return {@code true} if any elements of the stream match the provided
     * predicate, otherwise {@code false}
     */
    default boolean anyMatch(Remote.IntPredicate predicate)
        {
        return anyMatch((IntPredicate) predicate);
        }

    /**
     * Returns whether all elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for
     * determining the result.  If the stream is empty then {@code true} is
     * returned and the predicate is not evaluated.
     * <p>
     * This is a <em>short-circuiting terminal operation</em>.
     *
     * @param predicate a <em>non-interfering</em>, <em>stateless</em>
     *                  predicate to apply to elements of this stream
     *
     * @return {@code true} if either all elements of the stream match the
     * provided predicate or the stream is empty, otherwise {@code false}
     */
    boolean allMatch(IntPredicate predicate);

    /**
     * Returns whether all elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for
     * determining the result.  If the stream is empty then {@code true} is
     * returned and the predicate is not evaluated.
     * <p>
     * This is a <em>short-circuiting terminal operation</em>.
     *
     * @param predicate a <em>non-interfering</em>, <em>stateless</em>
     *                  predicate to apply to elements of this stream
     *
     * @return {@code true} if either all elements of the stream match the
     * provided predicate or the stream is empty, otherwise {@code false}
     */
    default boolean allMatch(Remote.IntPredicate predicate)
        {
        return allMatch((IntPredicate) predicate);
        }

    /**
     * Returns whether no elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for
     * determining the result.  If the stream is empty then {@code true} is
     * returned and the predicate is not evaluated.
     * <p>
     * This is a <em>short-circuiting terminal operation</em>.
     *
     * @param predicate a <em>non-interfering</em>, <em>stateless</em>
     *                  predicate to apply to elements of this stream
     *
     * @return {@code true} if either no elements of the stream match the
     * provided predicate or the stream is empty, otherwise {@code false}
     */
    boolean noneMatch(IntPredicate predicate);

    /**
     * Returns whether no elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for
     * determining the result.  If the stream is empty then {@code true} is
     * returned and the predicate is not evaluated.
     * <p>
     * This is a <em>short-circuiting terminal operation</em>.
     *
     * @param predicate a <em>non-interfering</em>, <em>stateless</em>
     *                  predicate to apply to elements of this stream
     *
     * @return {@code true} if either no elements of the stream match the
     * provided predicate or the stream is empty, otherwise {@code false}
     */
    default boolean noneMatch(Remote.IntPredicate predicate)
        {
        return noneMatch((IntPredicate) predicate);
        }

    /**
     * Returns an {@link OptionalInt} describing the first element of this
     * stream, or an empty {@code OptionalInt} if the stream is empty.  If the
     * stream has no encounter order, then any element may be returned.
     * <p>
     * This is a <em>short-circuiting terminal operation</em>.
     *
     * @return an {@code OptionalInt} describing the first element of this
     * stream, or an empty {@code OptionalInt} if the stream is empty
     */
    OptionalInt findFirst();

    /**
     * Returns an {@link OptionalInt} describing some element of the stream, or
     * an empty {@code OptionalInt} if the stream is empty.
     * <p>
     * This is a <em>short-circuiting terminal operation</em>.
     * <p>
     * The behavior of this operation is explicitly nondeterministic; it is
     * free to select any element in the stream.  This is to allow for maximal
     * performance in parallel operations; the cost is that multiple invocations
     * on the same source may not return the same result.  (If a stable result
     * is desired, use {@link #findFirst()} instead.)
     *
     * @return an {@code OptionalInt} describing some element of this stream, or
     * an empty {@code OptionalInt} if the stream is empty
     *
     * @see #findFirst()
     */
    OptionalInt findAny();

    /**
     * Returns a {@code LongStream} consisting of the elements of this stream,
     * converted to {@code long}.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @return a {@code LongStream} consisting of the elements of this stream,
     * converted to {@code long}
     */
    RemoteLongStream asLongStream();

    /**
     * Returns a {@code DoubleStream} consisting of the elements of this stream,
     * converted to {@code double}.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @return a {@code DoubleStream} consisting of the elements of this stream,
     * converted to {@code double}
     */
    RemoteDoubleStream asDoubleStream();

    /**
     * Returns a {@code Stream} consisting of the elements of this stream, each
     * boxed to an {@code Integer}.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @return a {@code Stream} consistent of the elements of this stream, each
     * boxed to an {@code Integer}
     */
    RemoteStream<Integer> boxed();

    @Override
    RemoteIntStream sequential();

    @Override
    RemoteIntStream parallel();

    @Override
    PrimitiveIterator.OfInt iterator();

    @Override
    Spliterator.OfInt spliterator();
    }
