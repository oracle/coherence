/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.function;

import com.tangosol.util.ValueExtractor;

import com.tangosol.util.comparator.InverseComparator;
import com.tangosol.util.comparator.SafeComparator;

import java.io.Serializable;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Helper interfaces and methods that enable capture of standard JDK
 * functional interfaces as serializable lambdas.
 *
 * @author as  2014.07.16
 * @since 12.2.1
 */
public class Remote
    {
    // ---- Consumers -------------------------------------------------------

    /**
     * Represents an operation that accepts a single input argument and returns
     * no result. Unlike most other functional interfaces, {@code Consumer} is
     * expected to operate via side-effects.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #accept(Object)}.
     *
     * @param <T> the type of the input to the operation
     */
    @FunctionalInterface
    public static interface Consumer<T>
            extends java.util.function.Consumer<T>, Serializable
        {
        /**
         * Returns a composed {@code Consumer} that performs, in sequence, this
         * operation followed by the {@code after} operation. If performing
         * either operation throws an exception, it is relayed to the caller of
         * the composed operation.  If performing this operation throws an
         * exception, the {@code after} operation will not be performed.
         *
         * @param after the operation to perform after this operation
         *
         * @return a composed {@code Consumer} that performs in sequence this
         * operation followed by the {@code after} operation
         *
         * @throws NullPointerException if {@code after} is null
         */
        default Consumer<T> andThen(Consumer<? super T> after)
            {
            Objects.requireNonNull(after);
            Consumer<T> self = this;
            return (T t) ->
                {
                self.accept(t);
                after.accept(t);
                };
            }
        }

    /**
     * Capture serializable Consumer.
     *
     * @param consumer lambda to capture
     * @param <T>      the type of the input to the operation
     *
     * @return serializable Consumer
     */
    public static <T> Consumer<T> consumer(Consumer<T> consumer)
        {
        return consumer;
        }

    /**
     * Represents an operation that accepts two input arguments and returns no
     * result.  This is the two-arity specialization of {@link Consumer}. Unlike
     * most other functional interfaces, {@code BiConsumer} is expected to
     * operate via side-effects.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #accept(Object, Object)}.
     *
     * @param <T> the type of the first argument to the operation
     * @param <U> the type of the second argument to the operation
     *
     * @see Consumer
     */
    @FunctionalInterface
    public static interface BiConsumer<T, U>
            extends java.util.function.BiConsumer<T, U>, Serializable
        {
        /**
         * Returns a composed {@code BiConsumer} that performs, in sequence,
         * this operation followed by the {@code after} operation. If performing
         * either operation throws an exception, it is relayed to the caller of
         * the composed operation.  If performing this operation throws an
         * exception, the {@code after} operation will not be performed.
         *
         * @param after the operation to perform after this operation
         *
         * @return a composed {@code BiConsumer} that performs in sequence this
         * operation followed by the {@code after} operation
         *
         * @throws NullPointerException if {@code after} is null
         */
        default BiConsumer<T, U> andThen(BiConsumer<? super T, ? super U> after)
            {
            Objects.requireNonNull(after);
            BiConsumer<T, U> self = this;
            return (l, r) ->
                {
                self.accept(l, r);
                after.accept(l, r);
                };
            }
        }

    /**
     * Capture serializable BiConsumer.
     *
     * @param biConsumer lambda to capture
     * @param <T>        the type of the input to the operation
     * @param <U>        the type of the second argument to the operation
     *
     * @return serializable BiConsumer
     */
    public static <T, U> BiConsumer<T, U> biConsumer(BiConsumer<T, U> biConsumer)
        {
        return biConsumer;
        }

    /**
     * Represents an operation that accepts a single {@code double}-valued
     * argument and returns no result.  This is the primitive type
     * specialization of {@link Consumer} for {@code double}.  Unlike most other
     * functional interfaces, {@code DoubleConsumer} is expected to operate via
     * side-effects.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #accept(double)}.
     *
     * @see Consumer
     */
    @FunctionalInterface
    public static interface DoubleConsumer
            extends java.util.function.DoubleConsumer, Serializable
        {
        /**
         * Returns a composed {@code DoubleConsumer} that performs, in sequence,
         * this operation followed by the {@code after} operation. If performing
         * either operation throws an exception, it is relayed to the caller of
         * the composed operation.  If performing this operation throws an
         * exception, the {@code after} operation will not be performed.
         *
         * @param after the operation to perform after this operation
         *
         * @return a composed {@code DoubleConsumer} that performs in sequence
         * this operation followed by the {@code after} operation
         *
         * @throws NullPointerException if {@code after} is null
         */
        default DoubleConsumer andThen(DoubleConsumer after)
            {
            Objects.requireNonNull(after);
            DoubleConsumer self = this;
            return (double t) ->
                {
                self.accept(t);
                after.accept(t);
                };
            }
        }

    /**
     * Capture serializable DoubleConsumer.
     *
     * @param consumer lambda to capture
     *
     * @return serializable DoubleConsumer
     */
    public static DoubleConsumer doubleConsumer(DoubleConsumer consumer)
        {
        return consumer;
        }

    /**
     * Represents an operation that accepts a single {@code int}-valued argument
     * and returns no result.  This is the primitive type specialization of
     * {@link Consumer} for {@code int}.  Unlike most other functional
     * interfaces, {@code IntConsumer} is expected to operate via side-effects.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #accept(int)}.
     *
     * @see Consumer
     */
    @FunctionalInterface
    public static interface IntConsumer
            extends java.util.function.IntConsumer, Serializable
        {
        /**
         * Returns a composed {@code IntConsumer} that performs, in sequence,
         * this operation followed by the {@code after} operation. If performing
         * either operation throws an exception, it is relayed to the caller of
         * the composed operation.  If performing this operation throws an
         * exception, the {@code after} operation will not be performed.
         *
         * @param after the operation to perform after this operation
         *
         * @return a composed {@code IntConsumer} that performs in sequence this
         * operation followed by the {@code after} operation
         *
         * @throws NullPointerException if {@code after} is null
         */
        default IntConsumer andThen(IntConsumer after)
            {
            Objects.requireNonNull(after);
            IntConsumer self = this;
            return (int t) ->
                {
                self.accept(t);
                after.accept(t);
                };
            }
        }

    /**
     * Capture serializable IntConsumer.
     *
     * @param consumer lambda to capture
     *
     * @return serializable IntConsumer
     */
    public static IntConsumer intConsumer(IntConsumer consumer)
        {
        return consumer;
        }

    /**
     * Represents an operation that accepts a single {@code long}-valued
     * argument and returns no result.  This is the primitive type
     * specialization of {@link Consumer} for {@code long}.  Unlike most other
     * functional interfaces, {@code LongConsumer} is expected to operate via
     * side-effects.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #accept(long)}.
     *
     * @see Consumer
     */
    @FunctionalInterface
    public static interface LongConsumer
            extends java.util.function.LongConsumer, Serializable
        {
        /**
         * Returns a composed {@code LongConsumer} that performs, in sequence,
         * this operation followed by the {@code after} operation. If performing
         * either operation throws an exception, it is relayed to the caller of
         * the composed operation.  If performing this operation throws an
         * exception, the {@code after} operation will not be performed.
         *
         * @param after the operation to perform after this operation
         *
         * @return a composed {@code LongConsumer} that performs in sequence
         * this operation followed by the {@code after} operation
         *
         * @throws NullPointerException if {@code after} is null
         */
        default LongConsumer andThen(LongConsumer after)
            {
            Objects.requireNonNull(after);
            LongConsumer self = this;
            return (long t) ->
                {
                self.accept(t);
                after.accept(t);
                };
            }
        }

    /**
     * Capture serializable LongConsumer.
     *
     * @param consumer lambda to capture
     *
     * @return serializable LongConsumer
     */
    public static LongConsumer longConsumer(LongConsumer consumer)
        {
        return consumer;
        }

    /**
     * Represents an operation that accepts an object-valued and a {@code
     * double}-valued argument, and returns no result.  This is the {@code
     * (reference, double)} specialization of {@link BiConsumer}. Unlike most
     * other functional interfaces, {@code ObjDoubleConsumer} is expected to
     * operate via side-effects.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #accept(Object, double)}.
     *
     * @param <T> the type of the object argument to the operation
     *
     * @see BiConsumer
     */
    @FunctionalInterface
    public static interface ObjDoubleConsumer<T>
            extends java.util.function.ObjDoubleConsumer<T>, Serializable
        {
        }

    /**
     * Capture serializable ObjDoubleConsumer.
     *
     * @param consumer lambda to capture
     * @param <T>      the type of the object argument to the operation
     *
     * @return serializable ObjDoubleConsumer
     */
    public static <T> ObjDoubleConsumer<T> objDoubleConsumer(ObjDoubleConsumer<T> consumer)
        {
        return consumer;
        }

    /**
     * Represents an operation that accepts an object-valued and a {@code
     * int}-valued argument, and returns no result.  This is the {@code
     * (reference, int)} specialization of {@link java.util.function.BiConsumer}.
     * Unlike most other functional interfaces, {@code ObjIntConsumer} is
     * expected to operate via side-effects.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #accept(Object, int)}.
     *
     * @param <T> the type of the object argument to the operation
     *
     * @see BiConsumer
     */
    @FunctionalInterface
    public static interface ObjIntConsumer<T>
            extends java.util.function.ObjIntConsumer<T>, Serializable
        {
        }

    /**
     * Capture serializable ObjIntConsumer.
     *
     * @param consumer lambda to capture
     * @param <T>      the type of the object argument to the operation
     *
     * @return serializable ObjIntConsumer
     */
    public static <T> ObjIntConsumer<T> objIntConsumer(ObjIntConsumer<T> consumer)
        {
        return consumer;
        }

    /**
     * Represents an operation that accepts an object-valued and a {@code
     * long}-valued argument, and returns no result.  This is the {@code
     * (reference, long)} specialization of {@link BiConsumer}. Unlike most
     * other functional interfaces, {@code ObjLongConsumer} is expected to
     * operate via side-effects.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #accept(Object, long)}.
     *
     * @param <T> the type of the object argument to the operation
     *
     * @see BiConsumer
     */
    @FunctionalInterface
    public static interface ObjLongConsumer<T>
            extends java.util.function.ObjLongConsumer<T>, Serializable
        {
        }

    /**
     * Capture serializable ObjLongConsumer.
     *
     * @param consumer lambda to capture
     * @param <T>      the type of the object argument to the operation
     *
     * @return serializable ObjLongConsumer
     */
    public static <T> ObjLongConsumer<T> objLongConsumer(ObjLongConsumer<T> consumer)
        {
        return consumer;
        }

    // ---- Functions -------------------------------------------------------

    /**
     * Represents a function that accepts one argument and produces a result.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #apply(Object)}.
     *
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     */
    @FunctionalInterface
    public static interface Function<T, R>
            extends java.util.function.Function<T, R>, Serializable
        {
        /**
         * Returns a composed function that first applies the {@code before}
         * function to its input, and then applies this function to the result.
         * If evaluation of either function throws an exception, it is relayed
         * to the caller of the composed function.
         *
         * @param <V>    the type of input to the {@code before} function, and
         *               to the composed function
         * @param before the function to apply before this function is applied
         *
         * @return a composed function that first applies the {@code before}
         * function and then applies this function
         *
         * @throws NullPointerException if before is null
         * @see #andThen(Function)
         */
        default <V> Function<V, R> compose(Function<? super V, ? extends T> before)
            {
            Objects.requireNonNull(before);
            Function<T, R> self = this;
            return (V v) -> self.apply(before.apply(v));
            }

        /**
         * Returns a composed function that first applies this function to its
         * input, and then applies the {@code after} function to the result. If
         * evaluation of either function throws an exception, it is relayed to
         * the caller of the composed function.
         *
         * @param <V>   the type of output of the {@code after} function, and of
         *              the composed function
         * @param after the function to apply after this function is applied
         *
         * @return a composed function that first applies this function and then
         * applies the {@code after} function
         *
         * @throws NullPointerException if after is null
         * @see #compose(Function)
         */
        default <V> Function<T, V> andThen(Function<? super R, ? extends V> after)
            {
            Objects.requireNonNull(after);
            Function<T, R> self = this;
            return (T t) -> after.apply(self.apply(t));
            }

        /**
         * Returns a function that always returns its input argument.
         *
         * @param <T> the type of the input and output objects to the function
         *
         * @return a function that always returns its input argument
         */
        static <T> Function<T, T> identity()
            {
            return t -> t;
            }
        }

    /**
     * Capture serializable Function.
     *
     * @param function lambda to capture
     * @param <T>      the type of the input to the function
     * @param <R>      the type of the result of the function
     *
     * @return serializable Function
     */
    public static <T, R> Function<T, R> function(Function<T, R> function)
        {
        return function;
        }

    /**
     * Represents a function that accepts two arguments and produces a result.
     * This is the two-arity specialization of {@link Function}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #apply(Object, Object)}.
     *
     * @param <T> the type of the first argument to the function
     * @param <U> the type of the second argument to the function
     * @param <R> the type of the result of the function
     *
     * @see Function
     */
    @FunctionalInterface
    public static interface BiFunction<T, U, R>
            extends java.util.function.BiFunction<T, U, R>, Serializable
        {
        /**
         * Returns a composed function that first applies this function to its
         * input, and then applies the {@code after} function to the result. If
         * evaluation of either function throws an exception, it is relayed to
         * the caller of the composed function.
         *
         * @param <V>   the type of output of the {@code after} function, and of
         *              the composed function
         * @param after the function to apply after this function is applied
         *
         * @return a composed function that first applies this function and then
         * applies the {@code after} function
         *
         * @throws NullPointerException if after is null
         */
        default <V> BiFunction<T, U, V> andThen(Function<? super R, ? extends V> after)
            {
            Objects.requireNonNull(after);
            BiFunction<T, U, R> self = this;
            return (T t, U u) -> after.apply(self.apply(t, u));
            }
        }

    /**
     * Capture serializable BiFunction.
     *
     * @param biFunction lambda to capture
     * @param <T>        the type of the first argument to the function
     * @param <U>        the type of the second argument to the function
     * @param <R>        the type of the result of the function
     *
     * @return serializable BiFunction
     */
    public static <T, U, R> BiFunction<T, U, R> biFunction(BiFunction<T, U, R> biFunction)
        {
        return biFunction;
        }

    /**
     * Represents a function that accepts a double-valued argument and produces
     * a result.  This is the {@code double}-consuming primitive specialization
     * for {@link Function}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #apply(double)}.
     *
     * @param <R> the type of the result of the function
     *
     * @see Function
     */
    @FunctionalInterface
    public static interface DoubleFunction<R>
            extends java.util.function.DoubleFunction<R>, Serializable
        {
        }

    /**
     * Capture serializable DoubleFunction.
     *
     * @param function lambda to capture
     * @param <R>      the type of the result of the function
     *
     * @return serializable DoubleFunction
     */
    public static <R> DoubleFunction<R> doubleFunction(DoubleFunction<R> function)
        {
        return function;
        }

    /**
     * Represents a function that accepts a double-valued argument and produces
     * an int-valued result.  This is the {@code double}-to-{@code int}
     * primitive specialization for {@link Function}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #applyAsInt(double)}.
     *
     * @see Function
     */
    @FunctionalInterface
    public static interface DoubleToIntFunction
            extends java.util.function.DoubleToIntFunction, Serializable
        {
        }

    /**
     * Capture serializable DoubleToIntFunction.
     *
     * @param function lambda to capture
     *
     * @return serializable DoubleToIntFunction
     */
    public static DoubleToIntFunction doubleToIntFunction(DoubleToIntFunction function)
        {
        return function;
        }

    /**
     * Represents a function that accepts a double-valued argument and produces
     * a long-valued result.  This is the {@code double}-to-{@code long}
     * primitive specialization for {@link Function}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #applyAsLong(double)}.
     *
     * @see Function
     */
    @FunctionalInterface
    public static interface DoubleToLongFunction
            extends java.util.function.DoubleToLongFunction, Serializable
        {
        }

    /**
     * Capture serializable DoubleToLongFunction.
     *
     * @param function lambda to capture
     *
     * @return serializable DoubleToLongFunction
     */
    public static DoubleToLongFunction doubleToLongFunction(DoubleToLongFunction function)
        {
        return function;
        }

    /**
     * Represents a function that accepts an int-valued argument and produces a
     * result.  This is the {@code int}-consuming primitive specialization for
     * {@link Function}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #apply(int)}.
     *
     * @param <R> the type of the result of the function
     *
     * @see Function
     */
    @FunctionalInterface
    public static interface IntFunction<R>
            extends java.util.function.IntFunction<R>, Serializable
        {
        }

    /**
     * Capture serializable IntFunction.
     *
     * @param function lambda to capture
     * @param <R>      the type of the result of the function
     *
     * @return serializable IntFunction
     */
    public static <R> IntFunction<R> intFunction(IntFunction<R> function)
        {
        return function;
        }

    /**
     * Represents a function that accepts an int-valued argument and produces a
     * double-valued result.  This is the {@code int}-to-{@code double}
     * primitive specialization for {@link Function}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #applyAsDouble(int)}.
     *
     * @see Function
     */
    @FunctionalInterface
    public static interface IntToDoubleFunction
            extends java.util.function.IntToDoubleFunction, Serializable
        {
        }

    /**
     * Capture serializable IntToDoubleFunction.
     *
     * @param function lambda to capture
     *
     * @return serializable IntToDoubleFunction
     */
    public static IntToDoubleFunction intToDoubleFunction(IntToDoubleFunction function)
        {
        return function;
        }

    /**
     * Represents a function that accepts an int-valued argument and produces a
     * long-valued result.  This is the {@code int}-to-{@code long} primitive
     * specialization for {@link Function}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #applyAsLong(int)}.
     *
     * @see Function
     */
    @FunctionalInterface
    public static interface IntToLongFunction
            extends java.util.function.IntToLongFunction, Serializable
        {
        }

    /**
     * Capture serializable IntToLongFunction.
     *
     * @param function lambda to capture
     *
     * @return serializable IntToLongFunction
     */
    public static IntToLongFunction intToLongFunction(IntToLongFunction function)
        {
        return function;
        }

    /**
     * Represents a function that accepts a long-valued argument and produces a
     * result.  This is the {@code long}-consuming primitive specialization for
     * {@link Function}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #apply(long)}.
     *
     * @param <R> the type of the result of the function
     *
     * @see Function
     */
    @FunctionalInterface
    public static interface LongFunction<R>
            extends java.util.function.LongFunction<R>, Serializable
        {
        }

    /**
     * Capture serializable LongFunction.
     *
     * @param function lambda to capture
     * @param <R>      the type of the result of the function
     *
     * @return serializable LongFunction
     */
    public static <R> LongFunction<R> longFunction(LongFunction<R> function)
        {
        return function;
        }

    /**
     * Represents a function that accepts a long-valued argument and produces a
     * double-valued result.  This is the {@code long}-to-{@code double}
     * primitive specialization for {@link Function}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #applyAsDouble(long)}.
     *
     * @see Function
     */
    @FunctionalInterface
    public static interface LongToDoubleFunction
            extends java.util.function.LongToDoubleFunction, Serializable
        {
        }

    /**
     * Capture serializable LongToDoubleFunction.
     *
     * @param function lambda to capture
     *
     * @return serializable LongToDoubleFunction
     */
    public static LongToDoubleFunction longToDoubleFunction(LongToDoubleFunction function)
        {
        return function;
        }

    /**
     * Represents a function that accepts a long-valued argument and produces an
     * int-valued result.  This is the {@code long}-to-{@code int} primitive
     * specialization for {@link Function}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #applyAsInt(long)}.
     *
     * @see Function
     */
    @FunctionalInterface
    public static interface LongToIntFunction
            extends java.util.function.LongToIntFunction, Serializable
        {
        }

    /**
     * Capture serializable LongToIntFunction.
     *
     * @param function lambda to capture
     *
     * @return serializable LongToIntFunction
     */
    public static LongToIntFunction longToIntFunction(LongToIntFunction function)
        {
        return function;
        }

    /**
     * Represents a function that produces a Comparable-valued result.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #apply(Object)}.
     *
     * @param <T> the type of the input to the function
     *
     * @see Function
     */
    @FunctionalInterface
    public static interface ToComparableFunction<T, R extends Comparable<? super R>>
            extends java.util.function.Function<T, R>, Serializable
        {
        }

    /**
     * Represents a function that produces a BigDecimal-valued result.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #apply(Object)}.
     *
     * @param <T> the type of the input to the function
     *
     * @see Function
     */
    @FunctionalInterface
    public static interface ToBigDecimalFunction<T>
            extends java.util.function.Function<T, BigDecimal>, Serializable
        {
        }

    /**
     * Represents a function that produces a double-valued result.  This is the
     * {@code double}-producing primitive specialization for {@link Function}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #applyAsDouble(Object)}.
     *
     * @param <T> the type of the input to the function
     *
     * @see Function
     */
    @FunctionalInterface
    public static interface ToDoubleFunction<T>
            extends java.util.function.ToDoubleFunction<T>, Serializable
        {
        }

    /**
     * Capture serializable ToDoubleFunction.
     *
     * @param function lambda to capture
     * @param <T>      the type of the input to the function
     *
     * @return serializable ToDoubleFunction
     */
    public static <T> ToDoubleFunction<T> toDoubleFunction(ToDoubleFunction<T> function)
        {
        return function;
        }

    /**
     * Represents a function that produces an int-valued result.  This is the
     * {@code int}-producing primitive specialization for {@link Function}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #applyAsInt(Object)}.
     *
     * @param <T> the type of the input to the function
     *
     * @see Function
     */
    @FunctionalInterface
    public static interface ToIntFunction<T>
            extends java.util.function.ToIntFunction<T>, Serializable
        {
        }

    /**
     * Capture serializable ToIntFunction.
     *
     * @param function lambda to capture
     * @param <T>      the type of the input to the function
     *
     * @return serializable ToIntFunction
     */
    public static <T> ToIntFunction<T> toIntFunction(ToIntFunction<T> function)
        {
        return function;
        }

    /**
     * Represents a function that produces a long-valued result.  This is the
     * {@code long}-producing primitive specialization for {@link Function}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #applyAsLong(Object)}.
     *
     * @param <T> the type of the input to the function
     *
     * @see Function
     */
    @FunctionalInterface
    public static interface ToLongFunction<T>
            extends java.util.function.ToLongFunction<T>, Serializable
        {
        }

    /**
     * Capture serializable ToLongFunction.
     *
     * @param function lambda to capture
     * @param <T>      the type of the input to the function
     *
     * @return serializable ToLongFunction
     */
    public static <T> ToLongFunction<T> toLongFunction(ToLongFunction<T> function)
        {
        return function;
        }

    /**
     * Represents a function that accepts two arguments and produces a
     * double-valued result.  This is the {@code double}-producing primitive
     * specialization for {@link BiFunction}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #applyAsDouble(Object, Object)}.
     *
     * @param <T> the type of the first argument to the function
     * @param <U> the type of the second argument to the function
     *
     * @see BiFunction
     */
    @FunctionalInterface
    public static interface ToDoubleBiFunction<T, U>
            extends java.util.function.ToDoubleBiFunction<T, U>, Serializable
        {
        }

    /**
     * Capture serializable ToDoubleBiFunction.
     *
     * @param biFunction lambda to capture
     * @param <T>        the type of the first argument to the function
     * @param <U>        the type of the second argument to the function
     *
     * @return serializable ToDoubleBiFunction
     */
    public static <T, U> ToDoubleBiFunction<T, U> toDoubleBiFunction(ToDoubleBiFunction<T, U> biFunction)
        {
        return biFunction;
        }

    /**
     * Represents a function that accepts two arguments and produces an
     * int-valued result.  This is the {@code int}-producing primitive
     * specialization for {@link BiFunction}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #applyAsInt(Object, Object)}.
     *
     * @param <T> the type of the first argument to the function
     * @param <U> the type of the second argument to the function
     *
     * @see BiFunction
     */
    @FunctionalInterface
    public static interface ToIntBiFunction<T, U>
            extends java.util.function.ToIntBiFunction<T, U>, Serializable
        {
        }

    /**
     * Capture serializable ToIntBiFunction.
     *
     * @param biFunction lambda to capture
     * @param <T>        the type of the first argument to the function
     * @param <U>        the type of the second argument to the function
     *
     * @return serializable ToIntBiFunction
     */
    public static <T, U> ToIntBiFunction<T, U> toIntBiFunction(ToIntBiFunction<T, U> biFunction)
        {
        return biFunction;
        }

    /**
     * Represents a function that accepts two arguments and produces a
     * long-valued result.  This is the {@code long}-producing primitive
     * specialization for {@link BiFunction}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #applyAsLong(Object, Object)}.
     *
     * @param <T> the type of the first argument to the function
     * @param <U> the type of the second argument to the function
     *
     * @see BiFunction
     */
    @FunctionalInterface
    public static interface ToLongBiFunction<T, U>
            extends java.util.function.ToLongBiFunction<T, U>, Serializable
        {
        }

    /**
     * Capture serializable ToLongBiFunction.
     *
     * @param biFunction lambda to capture
     * @param <T>        the type of the first argument to the function
     * @param <U>        the type of the second argument to the function
     *
     * @return serializable ToLongBiFunction
     */
    public static <T, U> ToLongBiFunction<T, U> toLongBiFunction(ToLongBiFunction<T, U> biFunction)
        {
        return biFunction;
        }

    // ---- Predicates ------------------------------------------------------

    /**
     * Represents a predicate (boolean-valued function) of one argument.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #test(Object)}.
     *
     * @param <T> the type of the input to the predicate
     */
    @FunctionalInterface
    public static interface Predicate<T>
            extends java.util.function.Predicate<T>, Serializable
        {
        /**
         * Returns a composed predicate that represents a short-circuiting
         * logical AND of this predicate and another.  When evaluating the
         * composed predicate, if this predicate is {@code false}, then the
         * {@code other} predicate is not evaluated.
         * <p>
         * Any exceptions thrown during evaluation of either predicate are
         * relayed to the caller; if evaluation of this predicate throws an
         * exception, the {@code other} predicate will not be evaluated.
         *
         * @param other a predicate that will be logically-ANDed with this
         *              predicate
         *
         * @return a composed predicate that represents the short-circuiting
         * logical AND of this predicate and the {@code other} predicate
         *
         * @throws NullPointerException if other is null
         */
        default Predicate<T> and(Predicate<? super T> other)
            {
            Objects.requireNonNull(other);
            Predicate<T> self = this;
            return (t) -> self.test(t) && other.test(t);
            }

        /**
         * Returns a predicate that represents the logical negation of this
         * predicate.
         *
         * @return a predicate that represents the logical negation of this
         * predicate
         */
        default Predicate<T> negate()
            {
            Predicate<T> self = this;
            return (t) -> !self.test(t);
            }

        /**
         * Returns a composed predicate that represents a short-circuiting
         * logical OR of this predicate and another.  When evaluating the
         * composed predicate, if this predicate is {@code true}, then the
         * {@code other} predicate is not evaluated.
         * <p>
         * <p>Any exceptions thrown during evaluation of either predicate are
         * relayed to the caller; if evaluation of this predicate throws an
         * exception, the {@code other} predicate will not be evaluated.
         *
         * @param other a predicate that will be logically-ORed with this
         *              predicate
         *
         * @return a composed predicate that represents the short-circuiting
         * logical OR of this predicate and the {@code other} predicate
         *
         * @throws NullPointerException if other is null
         */
        default Predicate<T> or(Predicate<? super T> other)
            {
            Objects.requireNonNull(other);
            Predicate<T> self = this;
            return (t) -> self.test(t) || other.test(t);
            }

        /**
         * Returns a predicate that tests if two arguments are equal according
         * to {@link Objects#equals(Object, Object)}.
         *
         * @param <T>       the type of arguments to the predicate
         * @param targetRef the object reference with which to compare for
         *                  equality, which may be {@code null}
         *
         * @return a predicate that tests if two arguments are equal according
         * to {@link Objects#equals(Object, Object)}
         */
        static <T> Predicate<T> isEqual(Object targetRef)
            {
            return (null == targetRef)
                   ? Objects::isNull
                   : o -> o.equals(targetRef);
            }
        }

    /**
     * Capture serializable Predicate.
     *
     * @param predicate lambda to capture
     * @param <T>       the type of the input to the predicate
     *
     * @return serializable Predicate
     */
    public static <T> Predicate<T> predicate(Predicate<T> predicate)
        {
        return predicate;
        }

    /**
     * Represents a predicate (boolean-valued function) of two arguments.  This
     * is the two-arity specialization of {@link Predicate}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #test(Object, Object)}.
     *
     * @param <T> the type of the first argument to the predicate
     * @param <U> the type of the second argument the predicate
     *
     * @see Predicate
     */
    @FunctionalInterface
    public static interface BiPredicate<T, U>
            extends java.util.function.BiPredicate<T, U>, Serializable
        {
        /**
         * Returns a composed predicate that represents a short-circuiting
         * logical AND of this predicate and another.  When evaluating the
         * composed predicate, if this predicate is {@code false}, then the
         * {@code other} predicate is not evaluated.
         * <p>
         * Any exceptions thrown during evaluation of either predicate are
         * relayed to the caller; if evaluation of this predicate throws an
         * exception, the {@code other} predicate will not be evaluated.
         *
         * @param other a predicate that will be logically-ANDed with this
         *              predicate
         *
         * @return a composed predicate that represents the short-circuiting
         * logical AND of this predicate and the {@code other} predicate
         *
         * @throws NullPointerException if other is null
         */
        default BiPredicate<T, U> and(BiPredicate<? super T, ? super U> other)
            {
            Objects.requireNonNull(other);
            BiPredicate<T, U> self = this;
            return (T t, U u) -> self.test(t, u) && other.test(t, u);
            }

        /**
         * Returns a predicate that represents the logical negation of this
         * predicate.
         *
         * @return a predicate that represents the logical negation of this
         * predicate
         */
        default BiPredicate<T, U> negate()
            {
            BiPredicate<T, U> self = this;
            return (T t, U u) -> !self.test(t, u);
            }

        /**
         * Returns a composed predicate that represents a short-circuiting
         * logical OR of this predicate and another.  When evaluating the
         * composed predicate, if this predicate is {@code true}, then the
         * {@code other} predicate is not evaluated.
         * <p>
         * Any exceptions thrown during evaluation of either predicate are
         * relayed to the caller; if evaluation of this predicate throws an
         * exception, the {@code other} predicate will not be evaluated.
         *
         * @param other a predicate that will be logically-ORed with this
         *              predicate
         *
         * @return a composed predicate that represents the short-circuiting
         * logical OR of this predicate and the {@code other} predicate
         *
         * @throws NullPointerException if other is null
         */
        default BiPredicate<T, U> or(BiPredicate<? super T, ? super U> other)
            {
            Objects.requireNonNull(other);
            BiPredicate<T, U> self = this;
            return (T t, U u) -> self.test(t, u) || other.test(t, u);
            }
        }

    /**
     * Capture serializable BiPredicate.
     *
     * @param biPredicate lambda to capture
     * @param <T>         the type of the first argument to the predicate
     * @param <U>         the type of the second argument the predicate
     *
     * @return serializable BiPredicate
     */
    public static <T, U> BiPredicate<T, U> biPredicate(BiPredicate<T, U> biPredicate)
        {
        return biPredicate;
        }

    /**
     * Represents a predicate (boolean-valued function) of one {@code
     * double}-valued argument. This is the {@code double}-consuming primitive
     * type specialization of {@link Predicate}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #test(double)}.
     *
     * @see Predicate
     */
    @FunctionalInterface
    public static interface DoublePredicate
            extends java.util.function.DoublePredicate, Serializable
        {
        /**
         * Returns a composed predicate that represents a short-circuiting
         * logical AND of this predicate and another.  When evaluating the
         * composed predicate, if this predicate is {@code false}, then the
         * {@code other} predicate is not evaluated.
         * <p>
         * Any exceptions thrown during evaluation of either predicate are
         * relayed to the caller; if evaluation of this predicate throws an
         * exception, the {@code other} predicate will not be evaluated.
         *
         * @param other a predicate that will be logically-ANDed with this
         *              predicate
         *
         * @return a composed predicate that represents the short-circuiting
         * logical AND of this predicate and the {@code other} predicate
         *
         * @throws NullPointerException if other is null
         */
        default DoublePredicate and(DoublePredicate other)
            {
            Objects.requireNonNull(other);
            DoublePredicate self = this;
            return (value) -> self.test(value) && other.test(value);
            }

        /**
         * Returns a predicate that represents the logical negation of this
         * predicate.
         *
         * @return a predicate that represents the logical negation of this
         * predicate
         */
        default DoublePredicate negate()
            {
            DoublePredicate self = this;
            return (value) -> !self.test(value);
            }

        /**
         * Returns a composed predicate that represents a short-circuiting
         * logical OR of this predicate and another.  When evaluating the
         * composed predicate, if this predicate is {@code true}, then the
         * {@code other} predicate is not evaluated.
         * <p>
         * Any exceptions thrown during evaluation of either predicate are
         * relayed to the caller; if evaluation of this predicate throws an
         * exception, the {@code other} predicate will not be evaluated.
         *
         * @param other a predicate that will be logically-ORed with this
         *              predicate
         *
         * @return a composed predicate that represents the short-circuiting
         * logical OR of this predicate and the {@code other} predicate
         *
         * @throws NullPointerException if other is null
         */
        default DoublePredicate or(DoublePredicate other)
            {
            Objects.requireNonNull(other);
            DoublePredicate self = this;
            return (value) -> self.test(value) || other.test(value);
            }
        }

    /**
     * Capture serializable DoublePredicate.
     *
     * @param predicate lambda to capture
     *
     * @return serializable DoublePredicate
     */
    public static DoublePredicate doublePredicate(DoublePredicate predicate)
        {
        return predicate;
        }

    /**
     * Represents a predicate (boolean-valued function) of one {@code
     * int}-valued argument. This is the {@code int}-consuming primitive type
     * specialization of {@link Predicate}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #test(int)}.
     *
     * @see Predicate
     */
    @FunctionalInterface
    public static interface IntPredicate
            extends java.util.function.IntPredicate, Serializable
        {
        /**
         * Returns a composed predicate that represents a short-circuiting
         * logical AND of this predicate and another.  When evaluating the
         * composed predicate, if this predicate is {@code false}, then the
         * {@code other} predicate is not evaluated.
         * <p>
         * Any exceptions thrown during evaluation of either predicate are
         * relayed to the caller; if evaluation of this predicate throws an
         * exception, the {@code other} predicate will not be evaluated.
         *
         * @param other a predicate that will be logically-ANDed with this
         *              predicate
         *
         * @return a composed predicate that represents the short-circuiting
         * logical AND of this predicate and the {@code other} predicate
         *
         * @throws NullPointerException if other is null
         */
        default IntPredicate and(IntPredicate other)
            {
            Objects.requireNonNull(other);
            IntPredicate self = this;
            return (value) -> self.test(value) && other.test(value);
            }

        /**
         * Returns a predicate that represents the logical negation of this
         * predicate.
         *
         * @return a predicate that represents the logical negation of this
         * predicate
         */
        default IntPredicate negate()
            {
            IntPredicate self = this;
            return (value) -> !self.test(value);
            }

        /**
         * Returns a composed predicate that represents a short-circuiting
         * logical OR of this predicate and another.  When evaluating the
         * composed predicate, if this predicate is {@code true}, then the
         * {@code other} predicate is not evaluated.
         * <p>
         * Any exceptions thrown during evaluation of either predicate are
         * relayed to the caller; if evaluation of this predicate throws an
         * exception, the {@code other} predicate will not be evaluated.
         *
         * @param other a predicate that will be logically-ORed with this
         *              predicate
         *
         * @return a composed predicate that represents the short-circuiting
         * logical OR of this predicate and the {@code other} predicate
         *
         * @throws NullPointerException if other is null
         */
        default IntPredicate or(IntPredicate other)
            {
            Objects.requireNonNull(other);
            IntPredicate self = this;
            return (value) -> self.test(value) || other.test(value);
            }
        }

    /**
     * Capture serializable IntPredicate.
     *
     * @param predicate lambda to capture
     *
     * @return serializable IntPredicate
     */
    public static IntPredicate intPredicate(IntPredicate predicate)
        {
        return predicate;
        }

    /**
     * Represents a predicate (boolean-valued function) of one {@code
     * long}-valued argument. This is the {@code long}-consuming primitive type
     * specialization of {@link Predicate}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #test(long)}.
     *
     * @see Predicate
     */
    @FunctionalInterface
    public static interface LongPredicate
            extends java.util.function.LongPredicate, Serializable
        {
        /**
         * Returns a composed predicate that represents a short-circuiting
         * logical AND of this predicate and another.  When evaluating the
         * composed predicate, if this predicate is {@code false}, then the
         * {@code other} predicate is not evaluated.
         * <p>
         * Any exceptions thrown during evaluation of either predicate are
         * relayed to the caller; if evaluation of this predicate throws an
         * exception, the {@code other} predicate will not be evaluated.
         *
         * @param other a predicate that will be logically-ANDed with this
         *              predicate
         *
         * @return a composed predicate that represents the short-circuiting
         * logical AND of this predicate and the {@code other} predicate
         *
         * @throws NullPointerException if other is null
         */
        default LongPredicate and(LongPredicate other)
            {
            Objects.requireNonNull(other);
            LongPredicate self = this;
            return (value) -> self.test(value) && other.test(value);
            }

        /**
         * Returns a predicate that represents the logical negation of this
         * predicate.
         *
         * @return a predicate that represents the logical negation of this
         * predicate
         */
        default LongPredicate negate()
            {
            LongPredicate self = this;
            return (value) -> !self.test(value);
            }

        /**
         * Returns a composed predicate that represents a short-circuiting
         * logical OR of this predicate and another.  When evaluating the
         * composed predicate, if this predicate is {@code true}, then the
         * {@code other} predicate is not evaluated.
         * <p>
         * Any exceptions thrown during evaluation of either predicate are
         * relayed to the caller; if evaluation of this predicate throws an
         * exception, the {@code other} predicate will not be evaluated.
         *
         * @param other a predicate that will be logically-ORed with this
         *              predicate
         *
         * @return a composed predicate that represents the short-circuiting
         * logical OR of this predicate and the {@code other} predicate
         *
         * @throws NullPointerException if other is null
         */
        default LongPredicate or(LongPredicate other)
            {
            Objects.requireNonNull(other);
            LongPredicate self = this;
            return (value) -> self.test(value) || other.test(value);
            }
        }

    /**
     * Capture serializable LongPredicate.
     *
     * @param predicate lambda to capture
     *
     * @return serializable LongPredicate
     */
    public static LongPredicate longPredicate(LongPredicate predicate)
        {
        return predicate;
        }

    // ---- Suppliers -------------------------------------------------------

    /**
     * Represents a supplier of results.
     * <p>
     * There is no requirement that a new or distinct result be returned each
     * time the supplier is invoked.
     * <p>
     * This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #get()}.
     *
     * @param <T> the type of results supplied by this supplier
     */
    @FunctionalInterface
    public static interface Supplier<T>
            extends java.util.function.Supplier<T>, Serializable
        {
        }

    /**
     * Capture serializable Supplier.
     *
     * @param supplier lambda to capture
     * @param <T>      the type of results supplied by this supplier
     *
     * @return serializable Supplier
     */
    public static <T> Supplier<T> supplier(Supplier<T> supplier)
        {
        return supplier;
        }

    /**
     * Represents a supplier of {@code boolean}-valued results.  This is the
     * {@code boolean}-producing primitive specialization of {@link Supplier}.
     * <p>
     * <p>There is no requirement that a new or distinct result be returned each
     * time the supplier is invoked.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #getAsBoolean()}.
     *
     * @see Supplier
     */
    @FunctionalInterface
    public static interface BooleanSupplier
            extends java.util.function.BooleanSupplier, Serializable
        {
        }

    /**
     * Capture serializable BooleanSupplier.
     *
     * @param supplier lambda to capture
     *
     * @return serializable BooleanSupplier
     */
    public static BooleanSupplier booleanSupplier(BooleanSupplier supplier)
        {
        return supplier;
        }

    /**
     * Represents a supplier of {@code double}-valued results.  This is the
     * {@code double}-producing primitive specialization of {@link Supplier}.
     * <p>
     * <p>There is no requirement that a distinct result be returned each time
     * the supplier is invoked.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #getAsDouble()}.
     *
     * @see Supplier
     */
    @FunctionalInterface
    public static interface DoubleSupplier
            extends java.util.function.DoubleSupplier, Serializable
        {
        }

    /**
     * Capture serializable DoubleSupplier.
     *
     * @param supplier lambda to capture
     *
     * @return serializable DoubleSupplier
     */
    public static DoubleSupplier doubleSupplier(DoubleSupplier supplier)
        {
        return supplier;
        }

    /**
     * Represents a supplier of {@code int}-valued results.  This is the {@code
     * int}-producing primitive specialization of {@link Supplier}.
     * <p>
     * <p>There is no requirement that a distinct result be returned each time
     * the supplier is invoked.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #getAsInt()}.
     *
     * @see Supplier
     */
    @FunctionalInterface
    public static interface IntSupplier
            extends java.util.function.IntSupplier, Serializable
        {
        }

    /**
     * Capture serializable IntSupplier.
     *
     * @param supplier lambda to capture
     *
     * @return serializable IntSupplier
     */
    public static IntSupplier intSupplier(IntSupplier supplier)
        {
        return supplier;
        }

    /**
     * Represents a supplier of {@code long}-valued results.  This is the {@code
     * long}-producing primitive specialization of {@link Supplier}.
     * <p>
     * <p>There is no requirement that a distinct result be returned each time
     * the supplier is invoked.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #getAsLong()}.
     *
     * @see Supplier
     */
    @FunctionalInterface
    public static interface LongSupplier
            extends java.util.function.LongSupplier, Serializable
        {
        }

    /**
     * Capture serializable LongSupplier.
     *
     * @param supplier lambda to capture
     *
     * @return serializable LongSupplier
     */
    public static LongSupplier longSupplier(LongSupplier supplier)
        {
        return supplier;
        }

    // ---- Binary Operators ------------------------------------------------

    /**
     * Represents an operation upon two operands of the same type, producing a
     * result of the same type as the operands.  This is a specialization of
     * {@link BiFunction} for the case where the operands and the result are all
     * of the same type.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #apply(Object, Object)}.
     *
     * @param <T> the type of the operands and result of the operator
     *
     * @see BiFunction
     * @see UnaryOperator
     */
    @FunctionalInterface
    public static interface BinaryOperator<T>
            extends BiFunction<T, T, T>,
                    java.util.function.BinaryOperator<T>,
                    Serializable
        {
        /**
         * Returns a {@link BinaryOperator} which returns the lesser of two
         * elements according to the specified {@code Comparable} value.
         *
         * @param <T>         the type of the input arguments of the comparator
         * @param comparable  a {@code Comparator} for comparing the two values
         *
         * @return a {@code BinaryOperator} which returns the greater of its
         * operands, according to the supplied {@code Comparator}
         *
         * @throws NullPointerException if the argument is null
         */
        public static <T, E extends Comparable<? super E>> BinaryOperator<T> minBy(ValueExtractor<? super T, ? extends E> comparable)
            {
            return minBy(comparator(comparable));
            }

        /**
         * Returns a {@link BinaryOperator} which returns the lesser of two
         * elements according to the specified {@code Comparator}.
         *
         * @param <T>        the type of the input arguments of the comparator
         * @param comparator a {@code Comparator} for comparing the two values
         *
         * @return a {@code BinaryOperator} which returns the lesser of its
         * operands, according to the supplied {@code Comparator}
         *
         * @throws NullPointerException if the argument is null
         */
        public static <T> BinaryOperator<T> minBy(Comparator<? super T> comparator)
            {
            Objects.requireNonNull(comparator);
            return (a, b) -> comparator.compare(a, b) <= 0 ? a : b;
            }

        /**
         * Returns a {@link BinaryOperator} which returns the lesser of two
         * elements according to the specified {@code Comparator}.
         *
         * @param <T>        the type of the input arguments of the comparator
         * @param comparator a {@code Comparator} for comparing the two values
         *
         * @return a {@code BinaryOperator} which returns the lesser of its
         * operands, according to the supplied {@code Comparator}
         *
         * @throws NullPointerException if the argument is null
         */
        public static <T> BinaryOperator<T> minBy(java.util.Comparator<? super T> comparator)
            {
            Objects.requireNonNull(comparator);
            return (a, b) -> comparator.compare(a, b) <= 0 ? a : b;
            }

        /**
         * Returns a {@link BinaryOperator} which returns the greater of two
         * elements according to the specified {@code Comparable} value.
         *
         * @param <T>        the type of the input arguments of the comparator
         * @param comparable a {@code Comparator} for comparing the two values
         *
         * @return a {@code BinaryOperator} which returns the greater of its
         * operands, according to the supplied {@code Comparator}
         *
         * @throws NullPointerException if the argument is null
         */
        public static <T, E extends Comparable<? super E>> BinaryOperator<T> maxBy(ValueExtractor<? super T, ? extends E> comparable)
            {
            return maxBy(comparator(comparable));
            }

        /**
         * Returns a {@link BinaryOperator} which returns the greater of two
         * elements according to the specified {@code Comparator}.
         *
         * @param <T>        the type of the input arguments of the comparator
         * @param comparator a {@code Comparator} for comparing the two values
         *
         * @return a {@code BinaryOperator} which returns the greater of its
         * operands, according to the supplied {@code Comparator}
         *
         * @throws NullPointerException if the argument is null
         */
        public static <T> BinaryOperator<T> maxBy(Comparator<? super T> comparator)
            {
            Objects.requireNonNull(comparator);
            return (a, b) -> comparator.compare(a, b) >= 0 ? a : b;
            }

        /**
         * Returns a {@link BinaryOperator} which returns the greater of two
         * elements according to the specified {@code Comparator}.
         *
         * @param <T>        the type of the input arguments of the comparator
         * @param comparator a {@code Comparator} for comparing the two values
         *
         * @return a {@code BinaryOperator} which returns the greater of its
         * operands, according to the supplied {@code Comparator}
         *
         * @throws NullPointerException if the argument is null
         */
        public static <T> BinaryOperator<T> maxBy(java.util.Comparator<? super T> comparator)
            {
            Objects.requireNonNull(comparator);
            return (a, b) -> comparator.compare(a, b) >= 0 ? a : b;
            }

        /**
         * {@code BinaryOperator<Map>} that merges the contents of its right
         * argument into its left argument, using the provided merge function to
         * handle duplicate keys.
         *
         * @param <K>           type of the map keys
         * @param <V>           type of the map values
         * @param <M>           type of the map
         * @param mergeFunction A merge function suitable for {@link
         *                      Map#merge(Object, Object, java.util.function.BiFunction) Map.merge()}
         *
         * @return a merge function for two maps
         */
        public static <K, V, M extends Map<K, V>> BinaryOperator<M> mapMerger(BinaryOperator<V> mergeFunction)
            {
            return mapMerger((java.util.function.BinaryOperator<V>) mergeFunction);
            }

        /**
         * {@code BinaryOperator<Map>} that merges the contents of its right
         * argument into its left argument, using the provided merge function to
         * handle duplicate keys.
         *
         * @param <K>           type of the map keys
         * @param <V>           type of the map values
         * @param <M>           type of the map
         * @param mergeFunction A merge function suitable for {@link
         *                      Map#merge(Object, Object, java.util.function.BiFunction) Map.merge()}
         *
         * @return a merge function for two maps
         */
        public static <K, V, M extends Map<K, V>> BinaryOperator<M> mapMerger(java.util.function.BinaryOperator<V> mergeFunction)
            {
            return (m1, m2) ->
                {
                for (Map.Entry<K, V> e : m2.entrySet())
                    {
                    m1.merge(e.getKey(), e.getValue(), mergeFunction);
                    }
                return m1;
                };
            }
        }

    /**
     * Capture serializable BinaryOperator.
     *
     * @param operator lambda to capture
     * @param <T>      the type of the operands and result of the operator
     *
     * @return serializable BinaryOperator
     */
    public static <T> BinaryOperator<T> binaryOperator(BinaryOperator<T> operator)
        {
        return operator;
        }

    /**
     * Represents an operation upon two {@code double}-valued operands and
     * producing a {@code double}-valued result.   This is the primitive type
     * specialization of {@link BinaryOperator} for {@code double}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #applyAsDouble(double, double)}.
     *
     * @see BinaryOperator
     * @see DoubleUnaryOperator
     */
    @FunctionalInterface
    public static interface DoubleBinaryOperator
            extends java.util.function.DoubleBinaryOperator, Serializable
        {
        }

    /**
     * Capture serializable DoubleBinaryOperator.
     *
     * @param operator lambda to capture
     *
     * @return serializable DoubleBinaryOperator
     */
    public static DoubleBinaryOperator doubleBinaryOperator(DoubleBinaryOperator operator)
        {
        return operator;
        }

    /**
     * Represents an operation upon two {@code int}-valued operands and
     * producing an {@code int}-valued result.   This is the primitive type
     * specialization of {@link BinaryOperator} for {@code int}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #applyAsInt(int, int)}.
     *
     * @see BinaryOperator
     * @see IntUnaryOperator
     */
    @FunctionalInterface
    public static interface IntBinaryOperator
            extends java.util.function.IntBinaryOperator, Serializable
        {
        }

    /**
     * Capture serializable IntBinaryOperator.
     *
     * @param operator lambda to capture
     *
     * @return serializable IntBinaryOperator
     */
    public static IntBinaryOperator intBinaryOperator(IntBinaryOperator operator)
        {
        return operator;
        }

    /**
     * Represents an operation upon two {@code long}-valued operands and
     * producing a {@code long}-valued result.   This is the primitive type
     * specialization of {@link BinaryOperator} for {@code long}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #applyAsLong(long, long)}.
     *
     * @see BinaryOperator
     * @see LongUnaryOperator
     */
    @FunctionalInterface
    public static interface LongBinaryOperator
            extends java.util.function.LongBinaryOperator, Serializable
        {
        }

    /**
     * Capture serializable LongBinaryOperator.
     *
     * @param operator lambda to capture
     *
     * @return serializable LongBinaryOperator
     */
    public static LongBinaryOperator longBinaryOperator(LongBinaryOperator operator)
        {
        return operator;
        }

    // ---- Unary Operators -------------------------------------------------

    /**
     * Represents an operation on a single operand that produces a result of the
     * same type as its operand.  This is a specialization of {@code Function}
     * for the case where the operand and result are of the same type.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #apply(Object)}.
     *
     * @param <T> the type of the operand and result of the operator
     *
     * @see Function
     */
    @FunctionalInterface
    public static interface UnaryOperator<T>
            extends Function<T, T>, java.util.function.UnaryOperator<T>,
                    Serializable
        {
        /**
         * Returns a unary operator that always returns its input argument.
         *
         * @param <T> the type of the input and output of the operator
         *
         * @return a unary operator that always returns its input argument
         */
        static <T> UnaryOperator<T> identity()
            {
            return t -> t;
            }
        }

    /**
     * Capture serializable UnaryOperator.
     *
     * @param operator lambda to capture
     * @param <T>      the type of the operand and result of the operator
     *
     * @return serializable UnaryOperator
     */
    public static <T> UnaryOperator<T> unaryOperator(UnaryOperator<T> operator)
        {
        return operator;
        }

    /**
     * Represents an operation on a single {@code double}-valued operand that
     * produces a {@code double}-valued result.  This is the primitive type
     * specialization of {@link UnaryOperator} for {@code double}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #applyAsDouble(double)}.
     *
     * @see UnaryOperator
     */
    @FunctionalInterface
    public static interface DoubleUnaryOperator
            extends java.util.function.DoubleUnaryOperator, Serializable
        {
        /**
         * Returns a composed operator that first applies the {@code before}
         * operator to its input, and then applies this operator to the result.
         * If evaluation of either operator throws an exception, it is relayed
         * to the caller of the composed operator.
         *
         * @param before the operator to apply before this operator is applied
         *
         * @return a composed operator that first applies the {@code before}
         * operator and then applies this operator
         *
         * @throws NullPointerException if before is null
         * @see #andThen(DoubleUnaryOperator)
         */
        default DoubleUnaryOperator compose(DoubleUnaryOperator before)
            {
            Objects.requireNonNull(before);
            DoubleUnaryOperator self = this;
            return (double v) -> self.applyAsDouble(before.applyAsDouble(v));
            }

        /**
         * Returns a composed operator that first applies this operator to its
         * input, and then applies the {@code after} operator to the result. If
         * evaluation of either operator throws an exception, it is relayed to
         * the caller of the composed operator.
         *
         * @param after the operator to apply after this operator is applied
         *
         * @return a composed operator that first applies this operator and then
         * applies the {@code after} operator
         *
         * @throws NullPointerException if after is null
         * @see #compose(DoubleUnaryOperator)
         */
        default DoubleUnaryOperator andThen(DoubleUnaryOperator after)
            {
            Objects.requireNonNull(after);
            DoubleUnaryOperator self = this;
            return (double t) -> after.applyAsDouble(self.applyAsDouble(t));
            }

        /**
         * Returns a unary operator that always returns its input argument.
         *
         * @return a unary operator that always returns its input argument
         */
        static DoubleUnaryOperator identity()
            {
            return t -> t;
            }
        }

    /**
     * Capture serializable DoubleUnaryOperator.
     *
     * @param operator lambda to capture
     *
     * @return serializable DoubleUnaryOperator
     */
    public static DoubleUnaryOperator doubleUnaryOperator(DoubleUnaryOperator operator)
        {
        return operator;
        }

    /**
     * Represents an operation on a single {@code int}-valued operand that
     * produces an {@code int}-valued result.  This is the primitive type
     * specialization of {@link UnaryOperator} for {@code int}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #applyAsInt(int)}.
     *
     * @see UnaryOperator
     */
    @FunctionalInterface
    public static interface IntUnaryOperator
            extends java.util.function.IntUnaryOperator, Serializable
        {
        /**
         * Returns a composed operator that first applies the {@code before}
         * operator to its input, and then applies this operator to the result.
         * If evaluation of either operator throws an exception, it is relayed
         * to the caller of the composed operator.
         *
         * @param before the operator to apply before this operator is applied
         *
         * @return a composed operator that first applies the {@code before}
         * operator and then applies this operator
         *
         * @throws NullPointerException if before is null
         * @see #andThen(IntUnaryOperator)
         */
        default IntUnaryOperator compose(IntUnaryOperator before)
            {
            Objects.requireNonNull(before);
            IntUnaryOperator self = this;
            return (int v) -> self.applyAsInt(before.applyAsInt(v));
            }

        /**
         * Returns a composed operator that first applies this operator to its
         * input, and then applies the {@code after} operator to the result. If
         * evaluation of either operator throws an exception, it is relayed to
         * the caller of the composed operator.
         *
         * @param after the operator to apply after this operator is applied
         *
         * @return a composed operator that first applies this operator and then
         * applies the {@code after} operator
         *
         * @throws NullPointerException if after is null
         * @see #compose(IntUnaryOperator)
         */
        default IntUnaryOperator andThen(IntUnaryOperator after)
            {
            Objects.requireNonNull(after);
            IntUnaryOperator self = this;
            return (int t) -> after.applyAsInt(self.applyAsInt(t));
            }

        /**
         * Returns a unary operator that always returns its input argument.
         *
         * @return a unary operator that always returns its input argument
         */
        static IntUnaryOperator identity()
            {
            return t -> t;
            }
        }

    /**
     * Capture serializable IntUnaryOperator.
     *
     * @param operator lambda to capture
     *
     * @return serializable IntUnaryOperator
     */
    public static IntUnaryOperator intUnaryOperator(IntUnaryOperator operator)
        {
        return operator;
        }

    /**
     * Represents an operation on a single {@code long}-valued operand that
     * produces a {@code long}-valued result.  This is the primitive type
     * specialization of {@link UnaryOperator} for {@code long}.
     * <p>
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #applyAsLong(long)}.
     *
     * @see UnaryOperator
     */
    @FunctionalInterface
    public static interface LongUnaryOperator
            extends java.util.function.LongUnaryOperator, Serializable
        {
        /**
         * Returns a composed operator that first applies the {@code before}
         * operator to its input, and then applies this operator to the result.
         * If evaluation of either operator throws an exception, it is relayed
         * to the caller of the composed operator.
         *
         * @param before the operator to apply before this operator is applied
         *
         * @return a composed operator that first applies the {@code before}
         * operator and then applies this operator
         *
         * @throws NullPointerException if before is null
         * @see #andThen(LongUnaryOperator)
         */
        default LongUnaryOperator compose(LongUnaryOperator before)
            {
            Objects.requireNonNull(before);
            LongUnaryOperator self = this;
            return (long v) -> self.applyAsLong(before.applyAsLong(v));
            }

        /**
         * Returns a composed operator that first applies this operator to its
         * input, and then applies the {@code after} operator to the result. If
         * evaluation of either operator throws an exception, it is relayed to
         * the caller of the composed operator.
         *
         * @param after the operator to apply after this operator is applied
         *
         * @return a composed operator that first applies this operator and then
         * applies the {@code after} operator
         *
         * @throws NullPointerException if after is null
         * @see #compose(LongUnaryOperator)
         */
        default LongUnaryOperator andThen(LongUnaryOperator after)
            {
            Objects.requireNonNull(after);
            LongUnaryOperator self = this;
            return (long t) -> after.applyAsLong(self.applyAsLong(t));
            }

        /**
         * Returns a unary operator that always returns its input argument.
         *
         * @return a unary operator that always returns its input argument
         */
        static LongUnaryOperator identity()
            {
            return t -> t;
            }
        }

    /**
     * Capture serializable LongUnaryOperator.
     *
     * @param operator lambda to capture
     *
     * @return serializable LongUnaryOperator
     */
    public static LongUnaryOperator longUnaryOperator(LongUnaryOperator operator)
        {
        return operator;
        }

    // ---- Comparator ------------------------------------------------------

    /**
     * A comparison function, which imposes a <i>total ordering</i> on some
     * collection of objects.  Comparators can be passed to a sort method (such
     * as {@code Collections.sort} or {@code Arrays.sort}) to allow precise control
     * over the sort order.  Comparators can also be used to control the order of
     * certain data structures (such as {@link SortedSet sorted sets} or {@link
     * SortedMap sorted maps}), or to provide an ordering for collections of
     * objects that don't have a {@link Comparable natural ordering}.<p>
     *
     * The ordering imposed by a comparator <tt>c</tt> on a set of elements
     * <tt>S</tt> is said to be <i>consistent with equals</i> if and only if
     * <tt>c.compare(e1, e2)==0</tt> has the same boolean value as
     * <tt>e1.equals(e2)</tt> for every <tt>e1</tt> and <tt>e2</tt> in
     * <tt>S</tt>.<p>
     *
     * Caution should be exercised when using a comparator capable of imposing an
     * ordering inconsistent with equals to order a sorted set (or sorted map).
     * Suppose a sorted set (or sorted map) with an explicit comparator <tt>c</tt>
     * is used with elements (or keys) drawn from a set <tt>S</tt>.  If the
     * ordering imposed by <tt>c</tt> on <tt>S</tt> is inconsistent with equals,
     * the sorted set (or sorted map) will behave "strangely."  In particular the
     * sorted set (or sorted map) will violate the general contract for set (or
     * map), which is defined in terms of <tt>equals</tt>.<p>
     *
     * For example, suppose one adds two elements {@code a} and {@code b} such that
     * {@code (a.equals(b) && c.compare(a, b) != 0)}
     * to an empty {@code TreeSet} with comparator {@code c}.
     * The second {@code add} operation will return
     * true (and the size of the tree set will increase) because {@code a} and
     * {@code b} are not equivalent from the tree set's perspective, even though
     * this is contrary to the specification of the
     * {@link Set#add Set.add} method.<p>
     *
     * Note: It is generally a good idea for comparators to also implement
     * <tt>java.io.Serializable</tt>, as they may be used as ordering methods in
     * serializable data structures (like {@link TreeSet}, {@link TreeMap}).  In
     * order for the data structure to serialize successfully, the comparator (if
     * provided) must implement <tt>Serializable</tt>.<p>
     *
     * For the mathematically inclined, the <i>relation</i> that defines the
     * <i>imposed ordering</i> that a given comparator <tt>c</tt> imposes on a
     * given set of objects <tt>S</tt> is:<pre>
     *       {(x, y) such that c.compare(x, y) &lt;= 0}.
     * </pre> The <i>quotient</i> for this total order is:<pre>
     *       {(x, y) such that c.compare(x, y) == 0}.
     * </pre>
     *
     * It follows immediately from the contract for <tt>compare</tt> that the
     * quotient is an <i>equivalence relation</i> on <tt>S</tt>, and that the
     * imposed ordering is a <i>total order</i> on <tt>S</tt>.  When we say that
     * the ordering imposed by <tt>c</tt> on <tt>S</tt> is <i>consistent with
     * equals</i>, we mean that the quotient for the ordering is the equivalence
     * relation defined by the objects' {@link Object#equals(Object)
     * equals(Object)} method(s):<pre>
     *     {(x, y) such that x.equals(y)}. </pre>
     *
     * <p>Unlike {@code Comparable}, a comparator may optionally permit
     * comparison of null arguments, while maintaining the requirements for
     * an equivalence relation.
     *
     * <p>This interface is a member of the Java Collections Framework.
     *
     * @param <T> the type of objects that may be compared by this comparator
     */
    @FunctionalInterface
    @SuppressWarnings("JavaDoc")
    public static interface Comparator<T>
            extends java.util.Comparator<T>, Serializable
        {
        /**
         * Returns a comparator that imposes the reverse ordering of this
         * comparator.
         *
         * @return a comparator that imposes the reverse ordering of this
         * comparator.
         */
        default Comparator<T> reversed()
            {
            return new InverseComparator<>(this);
            }

        /**
         * Returns a lexicographic-order comparator with another comparator. If
         * this {@code Comparator} considers two elements equal, i.e. {@code
         * compare(a, b) == 0}, {@code other} is used to determine the order.
         *
         * @param other the other comparator to be used when this comparator
         *              compares two objects that are equal.
         *
         * @return a lexicographic-order comparator composed of this and then
         * the other comparator
         *
         * @throws NullPointerException if the argument is null.
         *
         * @apiNote For example, to sort a collection of {@code String} based on
         * the length and then case-insensitive natural ordering, the comparator
         * can be composed using following code,
         * <p>
         * <pre>{@code
         *     Comparator<String> cmp = Comparator.comparingInt(String::length)
         *             .thenComparing(String.CASE_INSENSITIVE_ORDER);
         * }</pre>
         * @since 1.8
         */
        default Comparator<T> thenComparing(Comparator<? super T> other)
            {
            Objects.requireNonNull(other);
            Comparator<T> self = this;
            return (c1, c2) ->
                {
                int res = self.compare(c1, c2);
                return (res != 0) ? res : other.compare(c1, c2);
                };
            }

        /**
         * Returns a lexicographic-order comparator with a function that
         * extracts a key to be compared with the given {@code Comparator}.
         *
         * @param <U>           the type of the sort key
         * @param keyExtractor  the function used to extract the sort key
         * @param keyComparator the {@code Comparator} used to compare the sort
         *                      key
         *
         * @return a lexicographic-order comparator composed of this comparator
         * and then comparing on the key extracted by the keyExtractor function
         *
         * @throws NullPointerException if either argument is null.
         *
         * @implSpec This default implementation behaves as if {@code
         * thenComparing(comparing(keyExtractor, cmp))}
         * .
         * @see #comparing(Function, Comparator)
         * @see #thenComparing(Comparator)
         */
        default <U> Comparator<T> thenComparing(
                Function<? super T, ? extends U> keyExtractor,
                Comparator<? super U> keyComparator)
            {
            return thenComparing(comparing(keyExtractor, keyComparator));
            }

        /**
         * Returns a lexicographic-order comparator with a function that
         * extracts a {@code Comparable} sort key.
         *
         * @param <U>          the type of the {@link Comparable} sort key
         * @param keyExtractor the function used to extract the {@link
         *                     Comparable} sort key
         *
         * @return a lexicographic-order comparator composed of this and then
         * the {@link Comparable} sort key.
         *
         * @throws NullPointerException if the argument is null.
         *
         * @implSpec This default implementation behaves as if {@code
         * thenComparing(comparing(keyExtractor))}.
         *
         * @see #comparing(Function)
         * @see #thenComparing(Comparator)
         */
        default <U extends Comparable<? super U>> Comparator<T> thenComparing(
                Function<? super T, ? extends U> keyExtractor)
            {
            return thenComparing(comparing(keyExtractor));
            }

        /**
         * Returns a lexicographic-order comparator with a function that
         * extracts a {@code int} sort key.
         *
         * @param keyExtractor the function used to extract the integer sort
         *                     key
         *
         * @return a lexicographic-order comparator composed of this and then
         * the {@code int} sort key
         *
         * @throws NullPointerException if the argument is null.
         *
         * @implSpec This default implementation behaves as if {@code
         * thenComparing(comparingInt(keyExtractor))}.
         *
         * @see #comparingInt(ToIntFunction)
         * @see #thenComparing(Comparator)
         */
        default Comparator<T> thenComparingInt(ToIntFunction<? super T> keyExtractor)
            {
            return thenComparing(comparingInt(keyExtractor));
            }

        /**
         * Returns a lexicographic-order comparator with a function that
         * extracts a {@code long} sort key.
         *
         * @param keyExtractor the function used to extract the long sort key
         *
         * @return a lexicographic-order comparator composed of this and then
         * the {@code long} sort key
         *
         * @throws NullPointerException if the argument is null.
         *
         * @implSpec This default implementation behaves as if {@code
         * thenComparing(comparingLong(keyExtractor))}.
         *
         * @see #comparingLong(ToLongFunction)
         * @see #thenComparing(Comparator)
         */
        default Comparator<T> thenComparingLong(ToLongFunction<? super T> keyExtractor)
            {
            return thenComparing(comparingLong(keyExtractor));
            }

        /**
         * Returns a lexicographic-order comparator with a function that
         * extracts a {@code double} sort key.
         *
         * @param keyExtractor the function used to extract the double sort key
         *
         * @return a lexicographic-order comparator composed of this and then
         * the {@code double} sort key
         *
         * @throws NullPointerException if the argument is null.
         *
         * @implSpec This default implementation behaves as if {@code
         * thenComparing(comparingDouble(keyExtractor))}.
         *
         * @see #comparingDouble(ToDoubleFunction)
         * @see #thenComparing(Comparator)
         */
        default Comparator<T> thenComparingDouble(ToDoubleFunction<? super T> keyExtractor)
            {
            return thenComparing(comparingDouble(keyExtractor));
            }

        /**
         * Returns a comparator that imposes the reverse of the <em>natural
         * ordering</em>.
         *
         * @param <T> the {@link Comparable} type of element to be compared
         *
         * @return a comparator that imposes the reverse of the <i>natural
         * ordering</i> on {@code Comparable} objects.
         *
         * @see Comparable
         */
        public static <T extends Comparable<? super T>> Comparator<T> reverseOrder()
            {
            return new InverseComparator<>();
            }

        /**
         * Returns a comparator that compares {@link Comparable} objects in
         * natural order.
         *
         * @param <T> the {@link Comparable} type of element to be compared
         *
         * @return a comparator that imposes the <i>natural ordering</i> on
         * {@code Comparable} objects.
         *
         * @see Comparable
         * @since 1.8
         */
        @SuppressWarnings("unchecked")
        public static <T extends Comparable<? super T>> Comparator<T> naturalOrder()
            {
            return (Comparator<T>) SafeComparator.INSTANCE;
            }

        /**
         * Returns a null-friendly comparator that considers {@code null} to be
         * less than non-null. When both are {@code null}, they are considered
         * equal. If both are non-null, the specified {@code Comparator} is used
         * to determine the order. If the specified comparator is {@code null},
         * then the returned comparator considers all non-null values to be
         * equal.
         *
         * @param <T>        the type of the elements to be compared
         * @param comparator a {@code Comparator} for comparing non-null values
         *
         * @return a comparator that considers {@code null} to be less than
         * non-null, and compares non-null objects with the supplied {@code
         * Comparator}.
         */
        public static <T> Comparator<T> nullsFirst(Comparator<T> comparator)
            {
            return new SafeComparator<>(comparator, true);
            }

        /**
         * Returns a null-friendly comparator that considers {@code null} to be
         * greater than non-null. When both are {@code null}, they are
         * considered equal. If both are non-null, the specified {@code
         * Comparator} is used to determine the order. If the specified
         * comparator is {@code null}, then the returned comparator considers
         * all non-null values to be equal.
         *
         * @param <T>        the type of the elements to be compared
         * @param comparator a {@code Comparator} for comparing non-null values
         *
         * @return a comparator that considers {@code null} to be greater than
         * non-null, and compares non-null objects with the supplied {@code
         * Comparator}.
         */
        public static <T> Comparator<T> nullsLast(Comparator<T> comparator)
            {
            return new SafeComparator<>(comparator, false);
            }

        /**
         * Accepts a function that extracts a sort key from a type {@code T},
         * and returns a {@code Comparator<T>} that compares by that sort key
         * using the specified {@link Comparator}.
         *
         * @param <T>           the type of element to be compared
         * @param <U>           the type of the sort key
         * @param keyExtractor  the function used to extract the sort key
         * @param keyComparator the {@code Comparator} used to compare the sort
         *                      key
         *
         * @return a comparator that compares by an extracted key using the
         * specified {@code Comparator}
         *
         * @throws NullPointerException if either argument is null
         *
         * @apiNote For example, to obtain a {@code Comparator} that compares
         * {@code Person} objects by their last name ignoring case differences,
         * <p>
         * <pre>{@code
         *     Comparator<Person> cmp = Comparator.comparing(
         *             Person::getLastName,
         *             String.CASE_INSENSITIVE_ORDER);
         * }</pre>
         */
        public static <T, U> Comparator<T> comparing(
                Function<? super T, ? extends U> keyExtractor,
                Comparator<? super U> keyComparator)
            {
            Objects.requireNonNull(keyExtractor);
            Objects.requireNonNull(keyComparator);
            return (c1, c2) -> keyComparator.compare(keyExtractor.apply(c1),
                                                     keyExtractor.apply(c2));
            }

        /**
         * Accepts a function that extracts a {@link java.lang.Comparable
         * Comparable} sort key from a type {@code T}, and returns a {@code
         * Comparator<T>} that compares by that sort key.
         *
         * @param <T>          the type of element to be compared
         * @param <U>          the type of the {@code Comparable} sort key
         * @param keyExtractor the function used to extract the {@link
         *                     Comparable} sort key
         *
         * @return a comparator that compares by an extracted key
         *
         * @throws NullPointerException if the argument is null
         *
         * @apiNote For example, to obtain a {@code Comparator} that compares
         * {@code Person} objects by their last name,
         * <p>
         * <pre>{@code
         *     Comparator<Person> byLastName = Comparator.comparing(Person::getLastName);
         * }</pre>
         */
        public static <T, U extends Comparable<? super U>> Comparator<T> comparing(
                Function<? super T, ? extends U> keyExtractor)
            {
            Objects.requireNonNull(keyExtractor);
            return (c1, c2) -> keyExtractor.apply(c1).compareTo(keyExtractor.apply(c2));
            }

        /**
         * Accepts a function that extracts an {@code int} sort key from a type
         * {@code T}, and returns a {@code Comparator<T>} that compares by that
         * sort key.
         *
         * @param <T>          the type of element to be compared
         * @param keyExtractor the function used to extract the integer sort
         *                     key
         *
         * @return a comparator that compares by an extracted key
         *
         * @throws NullPointerException if the argument is null
         *
         * @see #comparing(Function)
         */
        public static <T> Comparator<T> comparingInt(ToIntFunction<? super T> keyExtractor)
            {
            Objects.requireNonNull(keyExtractor);
            return (c1, c2) -> Integer.compare(keyExtractor.applyAsInt(c1), keyExtractor.applyAsInt(c2));
            }

        /**
         * Accepts a function that extracts a {@code long} sort key from a type
         * {@code T}, and returns a {@code Comparator<T>} that compares by that
         * sort key.
         *
         * @param <T>          the type of element to be compared
         * @param keyExtractor the function used to extract the long sort key
         *
         * @return a comparator that compares by an extracted key
         *
         * @throws NullPointerException if the argument is null
         *
         * @see #comparing(Function)
         */
        public static <T> Comparator<T> comparingLong(ToLongFunction<? super T> keyExtractor)
            {
            Objects.requireNonNull(keyExtractor);
            return (c1, c2) -> Long.compare(keyExtractor.applyAsLong(c1), keyExtractor.applyAsLong(c2));
            }

        /**
         * Accepts a function that extracts a {@code double} sort key from a
         * type {@code T}, and returns a {@code Comparator<T>} that compares by
         * that sort key.
         *
         * @param <T>          the type of element to be compared
         * @param keyExtractor the function used to extract the double sort key
         *
         * @return a comparator that compares by an extracted key
         *
         * @throws NullPointerException if the argument is null
         *
         * @see #comparing(Function)
         */
        public static <T> Comparator<T> comparingDouble(ToDoubleFunction<? super T> keyExtractor)
            {
            Objects.requireNonNull(keyExtractor);
            return (c1, c2) -> Double.compare(keyExtractor.applyAsDouble(c1), keyExtractor.applyAsDouble(c2));
            }
        }

    /**
     * Capture serializable Comparator.
     *
     * @param comparator lambda to capture
     *
     * @return serializable Comparator
     */
    public static <T> Comparator<T> comparator(Comparator<T> comparator)
        {
        return comparator;
        }

    /**
     * Create {@link Comparator} for the specified extractor that returns a
     * {@link Comparable} value.
     *
     * @param extractor  a {@link ValueExtractor} that returns a {@code Comparable}
     *                   value
     *
     * @return a Comparator instance
     */
    public static <T, E extends Comparable<? super E>> Comparator<T> comparator(ValueExtractor<? super T, ? extends E> extractor)
        {
        return Comparator.comparing(extractor);
        }

    // ---- Runnable and Callable -------------------------------------------

    /**
     * The <code>Runnable</code> interface should be implemented by any
     * class whose instances are intended to be executed by a thread. The
     * class must define a method of no arguments called <code>run</code>.
     * <p>
     * This interface is designed to provide a common protocol for objects that
     * wish to execute code while they are active. For example,
     * <code>Runnable</code> is implemented by class <code>Thread</code>.
     * Being active simply means that a thread has been started and has not
     * yet been stopped.
     * <p>
     * In addition, <code>Runnable</code> provides the means for a class to be
     * active while not subclassing <code>Thread</code>. A class that implements
     * <code>Runnable</code> can run without subclassing <code>Thread</code>
     * by instantiating a <code>Thread</code> instance and passing itself in
     * as the target.  In most cases, the <code>Runnable</code> interface should
     * be used if you are only planning to override the <code>run()</code>
     * method and no other <code>Thread</code> methods.
     * This is important because classes should not be subclassed
     * unless the programmer intends on modifying or enhancing the fundamental
     * behavior of the class.
     */
    @FunctionalInterface
    public static interface Runnable
            extends java.lang.Runnable, Serializable
        {
        }

    /**
     * Capture serializable Runnable.
     *
     * @param runnable lambda to capture
     *
     * @return serializable Runnable
     */
    public static Runnable runnable(Runnable runnable)
        {
        return runnable;
        }

    /**
     * A task that returns a result and may throw an exception.
     * Implementors define a single method with no arguments called
     * {@code call}.
     *
     * <p>The {@code Callable} interface is similar to {@link
     * java.lang.Runnable}, in that both are designed for classes whose
     * instances are potentially executed by another thread.  A
     * {@code Runnable}, however, does not return a result and cannot
     * throw a checked exception.
     *
     * <p>The {@link java.util.concurrent.Executors} class contains utility
     * methods to convert from other common forms to {@code Callable} classes.
     *
     * @param <V> the result type of method {@code call}
     */
    @FunctionalInterface
    public static interface Callable<V>
            extends java.util.concurrent.Callable<V>, Serializable
        {
        }

    /**
     * Capture serializable Callable.
     *
     * @param callable lambda to capture
     *
     * @return serializable Callable
     */
    public static <V> Callable<V> callable(Callable<V> callable)
        {
        return callable;
        }
    }
