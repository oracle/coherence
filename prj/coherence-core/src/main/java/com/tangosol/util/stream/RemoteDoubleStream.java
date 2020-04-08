/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.stream;

import com.tangosol.util.function.Remote;

import java.util.DoubleSummaryStatistics;
import java.util.OptionalDouble;
import java.util.PrimitiveIterator;
import java.util.Spliterator;

import java.util.function.BiConsumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;

/**
 * A sequence of primitive double-valued elements supporting sequential and parallel
 * aggregate operations.  This is the {@code double} primitive specialization of
 * {@link RemoteStream}.
 *
 * <p>The following example illustrates an aggregate operation using
 * {@link RemoteStream} and {@link RemoteDoubleStream}, computing the sum of the weights of the
 * red widgets:
 *
 * <pre>{@code
 *     double sum = widgets.stream()
 *                         .filter(w -> w.getColor() == RED)
 *                         .mapToDouble(w -> w.getWeight())
 *                         .sum();
 * }</pre>
 * <p>
 * This interface is an extension of {@code java.util.stream.DoubleStream} that
 * captures lambdas used as method arguments as serializable lambdas.
 *
 * @author as  2014.09.11
 * @since 12.2.1
 *
 * @see RemoteStream
 * @see <a href="package-summary.html">com.tangosol.util.stream</a>
 */
public interface RemoteDoubleStream
        extends java.util.stream.DoubleStream, BaseRemoteStream<Double, java.util.stream.DoubleStream>
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
    RemoteDoubleStream filter(DoublePredicate predicate);

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
    default RemoteDoubleStream filter(Remote.DoublePredicate predicate)
        {
        return filter((DoublePredicate) predicate);
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
    RemoteDoubleStream map(DoubleUnaryOperator mapper);

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
    default RemoteDoubleStream map(Remote.DoubleUnaryOperator mapper)
        {
        return map((DoubleUnaryOperator) mapper);
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
    <U> RemoteStream<U> mapToObj(DoubleFunction<? extends U> mapper);

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
    default <U> RemoteStream<U> mapToObj(Remote.DoubleFunction<? extends U> mapper)
        {
        return mapToObj((DoubleFunction<? extends U>) mapper);
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
    RemoteIntStream mapToInt(DoubleToIntFunction mapper);

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
    default RemoteIntStream mapToInt(Remote.DoubleToIntFunction mapper)
        {
        return mapToInt((DoubleToIntFunction) mapper);
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
    RemoteLongStream mapToLong(DoubleToLongFunction mapper);

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
    default RemoteLongStream mapToLong(Remote.DoubleToLongFunction mapper)
        {
        return mapToLong((DoubleToLongFunction) mapper);
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
     *               {@code DoubleStream} of new values
     *               
     * @return the new stream
     * 
     * @see RemoteStream#flatMap(Function)
     */
    RemoteDoubleStream flatMap(DoubleFunction<? extends java.util.stream.DoubleStream> mapper);

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
     *               {@code DoubleStream} of new values
     *
     * @return the new stream
     *
     * @see RemoteStream#flatMap(Function)
     */
    default RemoteDoubleStream flatMap(Remote.DoubleFunction<? extends java.util.stream.DoubleStream> mapper)
        {
        return flatMap((DoubleFunction<? extends java.util.stream.DoubleStream>) mapper);
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
    RemoteDoubleStream peek(DoubleConsumer action);

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
    default RemoteDoubleStream peek(Remote.DoubleConsumer action)
        {
        return peek((DoubleConsumer) action);
        }

    /**
     * Returns a stream consisting of the distinct elements of this stream. The
     * elements are compared for equality according to
     * {@link java.lang.Double#compare(double, double)}.
     * <p>
     * This is a <em>stateful intermediate operation</em>.
     *
     * @return the result stream
     */
    java.util.stream.DoubleStream distinct();

    /**
     * Returns a stream consisting of the elements of this stream in sorted
     * order. The elements are compared for equality according to
     * {@link java.lang.Double#compare(double, double)}.
     * <p>
     * This is a <em>stateful intermediate operation</em>.
     *
     * @return the result stream
     */
    java.util.stream.DoubleStream sorted();

    /**
     * Returns a stream consisting of the elements of this stream, truncated
     * to be no longer than {@code maxSize} in length.
     * <p>
     * This is a <em>short-circuiting stateful intermediate operation</em>.
     *
     * @param maxSize the number of elements the stream should be limited to
     *                
     * @return the new stream
     * 
     * @throws IllegalArgumentException if {@code maxSize} is negative
     */
    java.util.stream.DoubleStream limit(long maxSize);

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
    java.util.stream.DoubleStream skip(long n);

    /**
     * Performs an action for each element of this stream.
     * <p>
     * This is a <em>terminal operation</em>.
     * <p>
     * For parallel stream pipelines, this operation does <em>not</em>
     * guarantee to respect the encounter order of the stream, as doing so
     * would sacrifice the benefit of parallelism.  For any given element, the
     * action may be performed at whatever time and in whatever thread the
     * library chooses.  If the action accesses shared state, it is
     * responsible for providing the required synchronization.
     *
     * @param action a <em>non-interfering</em> action to perform on the elements
     */
    void forEach(DoubleConsumer action);

    /**
     * Performs an action for each element of this stream, guaranteeing that
     * each element is processed in encounter order for streams that have a
     * defined encounter order.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param action a <em>non-interfering</em> action to perform on the elements
     *               
     * @see #forEach(DoubleConsumer)
     */
    void forEachOrdered(DoubleConsumer action);

    /**
     * Returns an array containing the elements of this stream.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @return an array containing the elements of this stream
     */
    double[] toArray();

    /**
     * Performs a <em>reduction</em> on the elements of this stream, using the 
     * provided identity value and an <em>associative</em> accumulation function, 
     * and returns the reduced value.  This is equivalent to:
     * <pre>{@code
     *     double result = identity;
     *     for (double element : this stream)
     *         result = accumulator.applyAsDouble(result, element)
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
    double reduce(double identity, DoubleBinaryOperator op);

    /**
     * Performs a <em>reduction</em> on the elements of this stream, using the
     * provided identity value and an <em>associative</em> accumulation function,
     * and returns the reduced value.  This is equivalent to:
     * <pre>{@code
     *     double result = identity;
     *     for (double element : this stream)
     *         result = accumulator.applyAsDouble(result, element)
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
    default double reduce(double identity, Remote.DoubleBinaryOperator op)
        {
        return reduce(identity, (DoubleBinaryOperator) op);
        }

    /**
     * Performs a <em>reduction</em> on the elements of this stream, using an
     * <em>associative</em> accumulation function, and returns an 
     * {@code OptionalDouble} describing the reduced value, if any. This is 
     * equivalent to:
     * <pre>{@code
     *     boolean foundAny = false;
     *     double result = null;
     *     for (double element : this stream) {
     *         if (!foundAny) {
     *             foundAny = true;
     *             result = element;
     *         }
     *         else
     *             result = accumulator.applyAsDouble(result, element);
     *     }
     *     return foundAny ? OptionalDouble.of(result) : OptionalDouble.empty();
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
     * @see #reduce(double, DoubleBinaryOperator)
     */
    OptionalDouble reduce(DoubleBinaryOperator op);

    /**
     * Performs a <em>reduction</em> on the elements of this stream, using an
     * <em>associative</em> accumulation function, and returns an
     * {@code OptionalDouble} describing the reduced value, if any. This is
     * equivalent to:
     * <pre>{@code
     *     boolean foundAny = false;
     *     double result = null;
     *     for (double element : this stream) {
     *         if (!foundAny) {
     *             foundAny = true;
     *             result = element;
     *         }
     *         else
     *             result = accumulator.applyAsDouble(result, element);
     *     }
     *     return foundAny ? OptionalDouble.of(result) : OptionalDouble.empty();
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
     * @see #reduce(double, DoubleBinaryOperator)
     */
    default OptionalDouble reduce(Remote.DoubleBinaryOperator op)
        {
        return reduce((DoubleBinaryOperator) op);
        }

    /**
     * Performs a <em>mutable reduction</em> operation on the elements of this
     * stream.  A mutable reduction is one in which the reduced value is a mutable
     * result container, such as an {@code ArrayList}, and elements are incorporated
     * by updating the state of the result rather than by replacing the result.
     * <p>
     * This produces a result equivalent to:
     * <pre>{@code
     *     R result = supplier.get();
     *     for (double element : this stream)
     *         accumulator.accept(result, element);
     *     return result;
     * }</pre>
     * <p>
     * Like {@link #reduce(double, DoubleBinaryOperator)}, {@code collect}
     * operations can be parallelized without requiring additional
     * synchronization.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param <R> type of the result
     * @param supplier    a function that creates a new result container. For a
     *                    parallel execution, this function may be called
     *                    multiple times and must return a fresh value each time.
     * @param accumulator an <em>associative</em>, <em>non-interfering</em>,
     *                    <em>stateless</em> function for incorporating an
     *                    additional element into a result
     * @param combiner an <em>associative</em>, <em>non-interfering</em>,
     *                    <em>stateless</em> function for combining two values,
     *                    which must be compatible with the accumulator function
     *
     * @return the result of the reduction
     *
     * @see RemoteStream#collect(Supplier, BiConsumer, BiConsumer)
     */
    <R> R collect(Supplier<R> supplier,
                  ObjDoubleConsumer<R> accumulator,
                  BiConsumer<R, R> combiner);

    /**
     * Performs a <em>mutable reduction</em> operation on the elements of this
     * stream.  A mutable reduction is one in which the reduced value is a mutable
     * result container, such as an {@code ArrayList}, and elements are incorporated
     * by updating the state of the result rather than by replacing the result.
     * <p>
     * This produces a result equivalent to:
     * <pre>{@code
     *     R result = supplier.get();
     *     for (double element : this stream)
     *         accumulator.accept(result, element);
     *     return result;
     * }</pre>
     * <p>
     * Like {@link #reduce(double, DoubleBinaryOperator)}, {@code collect}
     * operations can be parallelized without requiring additional
     * synchronization.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @param <R> type of the result
     * @param supplier    a function that creates a new result container. For a
     *                    parallel execution, this function may be called
     *                    multiple times and must return a fresh value each time.
     * @param accumulator an <em>associative</em>, <em>non-interfering</em>,
     *                    <em>stateless</em> function for incorporating an
     *                    additional element into a result
     * @param combiner an <em>associative</em>, <em>non-interfering</em>,
     *                    <em>stateless</em> function for combining two values,
     *                    which must be compatible with the accumulator function
     *
     * @return the result of the reduction
     *
     * @see RemoteStream#collect(Supplier, BiConsumer, BiConsumer)
     */
    default <R> R collect(Remote.Supplier<R> supplier,
                          Remote.ObjDoubleConsumer<R> accumulator,
                          Remote.BiConsumer<R, R> combiner)
        {
        return collect((Supplier<R>) supplier,
                       (ObjDoubleConsumer<R>) accumulator,
                       (BiConsumer<R, R>) combiner);
        }

    /**
     * Returns the sum of elements in this stream.
     * <p>
     * Summation is a special case of a <em>reduction</em>. If
     * floating-point summation were exact, this method would be
     * equivalent to:
     *
     * <pre>{@code
     *     return reduce(0, Double::sum);
     * }</pre>
     *
     * However, since floating-point summation is not exact, the above
     * code is not necessarily equivalent to the summation computation
     * done by this method.
     * <p>
     * If any stream element is a NaN or the sum is at any point a NaN
     * then the sum will be NaN.
     * <p>
     * The value of a floating-point sum is a function both
     * of the input values as well as the order of addition
     * operations. The order of addition operations of this method is
     * intentionally not defined to allow for implementation
     * flexibility to improve the speed and accuracy of the computed
     * result.
     * <p>
     * In particular, this method may be implemented using compensated
     * summation or other technique to reduce the error bound in the
     * numerical sum compared to a simple summation of {@code double}
     * values.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @return the sum of elements in this stream
     */
    double sum();

    /**
     * Returns an {@code OptionalDouble} describing the minimum element of this
     * stream, or an empty OptionalDouble if this stream is empty.  The minimum
     * element will be {@code Double.NaN} if any stream element was NaN. Unlike
     * the numerical comparison operators, this method considers negative zero
     * to be strictly smaller than positive zero. This is a special case of a
     * <em>reduction</em> and is equivalent to:
     * <pre>{@code
     *     return reduce(Double::min);
     * }</pre>
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @return an {@code OptionalDouble} containing the minimum element of this
     * stream, or an empty optional if the stream is empty
     */
    OptionalDouble min();

    /**
     * Returns an {@code OptionalDouble} describing the maximum element of this
     * stream, or an empty OptionalDouble if this stream is empty.  The maximum
     * element will be {@code Double.NaN} if any stream element was NaN. Unlike
     * the numerical comparison operators, this method considers negative zero
     * to be strictly smaller than positive zero. This is a special case of a
     * <em>reduction</em> and is equivalent to:
     * <pre>{@code
     *     return reduce(Double::max);
     * }</pre>
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @return an {@code OptionalDouble} containing the maximum element of this
     * stream, or an empty optional if the stream is empty
     */
    OptionalDouble max();

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
     * Returns an {@code OptionalDouble} describing the arithmetic
     * mean of elements of this stream, or an empty optional if this
     * stream is empty.
     * <p>
     * If any recorded value is a NaN or the sum is at any point a NaN
     * then the average will be NaN.
     * <p>
     * The average returned can vary depending upon the order in
     * which values are recorded.
     * <p>
     * This method may be implemented using compensated summation or
     * other technique to reduce the error bound in the {@link #sum
     * numerical sum} used to compute the average.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @return an {@code OptionalDouble} containing the average element of this
     * stream, or an empty optional if the stream is empty
     */
    OptionalDouble average();

    /**
     * Returns a {@code DoubleSummaryStatistics} describing various summary data
     * about the elements of this stream. This is a special case of a <em>reduction</em>.
     * <p>
     * This is a <em>terminal operation</em>.
     *
     * @return a {@code DoubleSummaryStatistics} describing various summary data
     * about the elements of this stream
     */
    DoubleSummaryStatistics summaryStatistics();

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
    boolean anyMatch(DoublePredicate predicate);

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
    default boolean anyMatch(Remote.DoublePredicate predicate)
        {
        return anyMatch((DoublePredicate) predicate);
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
    boolean allMatch(DoublePredicate predicate);

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
    default boolean allMatch(Remote.DoublePredicate predicate)
        {
        return allMatch((DoublePredicate) predicate);
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
    boolean noneMatch(DoublePredicate predicate);

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
    default boolean noneMatch(Remote.DoublePredicate predicate)
        {
        return noneMatch((DoublePredicate) predicate);
        }

    /**
     * Returns an {@link OptionalDouble} describing the first element of this
     * stream, or an empty {@code OptionalDouble} if the stream is empty.  If
     * the stream has no encounter order, then any element may be returned.
     * <p>
     * This is a <em>short-circuiting terminal operation</em>.
     *
     * @return an {@code OptionalDouble} describing the first element of this
     * stream, or an empty {@code OptionalDouble} if the stream is empty
     */
    OptionalDouble findFirst();

    /**
     * Returns an {@link OptionalDouble} describing some element of the stream,
     * or an empty {@code OptionalDouble} if the stream is empty.
     * <p>
     * This is a <em>short-circuiting terminal operation</em>.
     *
     * <p>The behavior of this operation is explicitly nondeterministic; it is
     * free to select any element in the stream.  This is to allow for maximal
     * performance in parallel operations; the cost is that multiple invocations
     * on the same source may not return the same result.  (If a stable result
     * is desired, use {@link #findFirst()} instead.)
     *
     * @return an {@code OptionalDouble} describing some element of this stream,
     * or an empty {@code OptionalDouble} if the stream is empty
     * @see #findFirst()
     */
    OptionalDouble findAny();

    /**
     * Returns a {@code Stream} consisting of the elements of this stream,
     * boxed to {@code Double}.
     * <p>
     * This is an <em>intermediate operation</em>.
     *
     * @return a {@code Stream} consistent of the elements of this stream,
     * each boxed to a {@code Double}
     */
    RemoteStream<Double> boxed();

    @Override
    RemoteDoubleStream sequential();

    @Override
    RemoteDoubleStream parallel();

    @Override
    PrimitiveIterator.OfDouble iterator();

    @Override
    Spliterator.OfDouble spliterator();
    }
