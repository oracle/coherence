/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import com.tangosol.util.function.Remote;

import java.util.concurrent.CompletableFuture;

import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

/**
 * An object reference that may be updated atomically.
 * <p>
 * Unlike {@link AtomicReference}, each method from this interface is non-blocking,
 * which allows asynchronous invocation and consumption of the return value
 * via {@code CompletableFuture} API. This is particularly useful when using
 * {@link AsyncRemoteAtomicReference remote} implementation, because of relatively
 * high latency associated with am inevitable network call, but we do provide a
 * {@link AsyncLocalAtomicReference local} implementation as well.
 *
 * @param <V>  the type of object referred to by this reference
 *
 * @author Aleks Seovic  2020.12.08
 * @since 21.12
 */
public interface AsyncAtomicReference<V>
    {
    /**
     * Returns the current value.
     *
     * @return the current value
     */
    CompletableFuture<V> get();

    /**
     * Sets the value to {@code newValue}.
     *
     * @param newValue  the new value
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     */
    CompletableFuture<Void> set(V newValue);

    /**
     * Atomically sets the value to {@code newValue} and returns the old value.
     *
     * @param newValue  the new value
     *
     * @return the previous value
     */
    CompletableFuture<V> getAndSet(V newValue);

    /**
     * Atomically sets the value to {@code newValue}
     * if the current value is <em>equal</em> to the {@code expectedValue}.
     *
     * @param expectedValue  the expected value
     * @param newValue       the new value
     *
     * @return {@code true} if successful. False return indicates that
     *         the actual value was not equal to the expected value.
     */
    CompletableFuture<Boolean> compareAndSet(V expectedValue, V newValue);

    /**
     * Atomically updates the current value with the results of
     * applying the given function, returning the previous value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     *
     * @param updateFunction  a side-effect-free function
     *
     * @return the previous value
     */
    CompletableFuture<V> getAndUpdate(Remote.UnaryOperator<V> updateFunction);

    /**
     * Atomically updates the current value with the results of
     * applying the given function, returning the previous value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     *
     * @param updateFunction  a side-effect-free function
     *
     * @return the previous value
     */
    CompletableFuture<V> getAndUpdate(UnaryOperator<V> updateFunction);

    /**
     * Atomically updates the current value with the results of
     * applying the given function, returning the updated value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     *
     * @param updateFunction  a side-effect-free function
     *
     * @return the updated value
     */
    CompletableFuture<V> updateAndGet(Remote.UnaryOperator<V> updateFunction);

    /**
     * Atomically updates the current value with the results of
     * applying the given function, returning the updated value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     *
     * @param updateFunction  a side-effect-free function
     *
     * @return the updated value
     */
    CompletableFuture<V> updateAndGet(UnaryOperator<V> updateFunction);

    /**
     * Atomically updates the current value with the results of
     * applying the given function to the current and given values,
     * returning the previous value.
     *
     * <p>The function should beside-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     * The function is applied with the current value as its first argument,
     * and the given update as the second argument.
     *
     * @param x                    the update value
     * @param accumulatorFunction  a side-effect-free function of two arguments
     *
     * @return the previous value
     */
    CompletableFuture<V> getAndAccumulate(V x, Remote.BinaryOperator<V> accumulatorFunction);

    /**
     * Atomically updates the current value with the results of
     * applying the given function to the current and given values,
     * returning the previous value.
     *
     * <p>The function should beside-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     * The function is applied with the current value as its first argument,
     * and the given update as the second argument.
     *
     * @param x                    the update value
     * @param accumulatorFunction  a side-effect-free function of two arguments
     *
     * @return the previous value
     */
    CompletableFuture<V> getAndAccumulate(V x, BinaryOperator<V> accumulatorFunction);

    /**
     * Atomically updates the current value with the results of
     * applying the given function to the current and given values,
     * returning the updated value.
     *
     * <p>The function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     * The function is applied with the current value as its first argument,
     * and the given update as the second argument.
     *
     * @param x                    the update value
     * @param accumulatorFunction  a side-effect-free function of two arguments
     *
     * @return the updated value
     */
    CompletableFuture<V> accumulateAndGet(V x, Remote.BinaryOperator<V> accumulatorFunction);

    /**
     * Atomically updates the current value with the results of
     * applying the given function to the current and given values,
     * returning the updated value.
     *
     * <p>The function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     * The function is applied with the current value as its first argument,
     * and the given update as the second argument.
     *
     * @param x                    the update value
     * @param accumulatorFunction  a side-effect-free function of two arguments
     *
     * @return the updated value
     */
    CompletableFuture<V> accumulateAndGet(V x, BinaryOperator<V> accumulatorFunction);

    /**
     * Atomically sets the value to {@code newValue} if the current value,
     * referred to as the <em>witness value</em>, is <em>equal</em> to the
     * {@code expectedValue}.
     *
     * @param expectedValue  the expected value
     * @param newValue       the new value
     *
     * @return the witness value, which will be the same as the expected value
     *         if successful
     */
    CompletableFuture<V> compareAndExchange(V expectedValue, V newValue);
    }
