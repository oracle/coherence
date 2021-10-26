/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import com.tangosol.util.function.Remote;

import java.util.concurrent.CompletableFuture;

import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

/**
 * A {@code int} value that may be updated atomically.
 * <p>
 * Unlike {@link AtomicInteger}, each method from this interface is non-blocking,
 * which allows asynchronous invocation and consumption of the return value
 * via {@code CompletableFuture} API. This is particularly useful when using
 * {@link AsyncRemoteAtomicInteger remote} implementation, because of relatively
 * high latency associated with an inevitable network call, but we do provide a
 * {@link AsyncLocalAtomicInteger local} implementation as well.
 * <p>
 * An {@code AsyncAtomicInteger} is used in applications
 * such as atomically incremented sequence numbers, and cannot be used
 * as a replacement for a {@link Integer}.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.12
 */
public interface AsyncAtomicInteger
    {
    /**
     * Returns the current value.
     *
     * @return the current value
     */
    CompletableFuture<Integer> get();

    /**
     * Sets the value to {@code newValue}.
     *
     * @param nNewValue  the new value
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     */
    CompletableFuture<Void> set(int nNewValue);

    /**
     * Atomically sets the value to {@code newValue} and returns the old value.
     *
     * @param nNewValue  the new value
     *
     * @return the previous value
     */
    CompletableFuture<Integer> getAndSet(int nNewValue);

    /**
     * Atomically sets the value to {@code newValue}
     * if the current value {@code == expectedValue}.
     *
     * @param nExpectedValue  the expected value
     * @param nNewValue       the new value
     *
     * @return {@code true} if successful. False return indicates that
     *         the actual value was not equal to the expected value.
     */
    CompletableFuture<Boolean> compareAndSet(int nExpectedValue, int nNewValue);

    /**
     * Atomically increments the current value.
     *
     * <p>Equivalent to {@code getAndAdd(1)}.
     *
     * @return the previous value
     */
    CompletableFuture<Integer> getAndIncrement();

    /**
     * Atomically decrements the current value.
     *
     * <p>Equivalent to {@code getAndAdd(-1)}.
     *
     * @return the previous value
     */
    CompletableFuture<Integer> getAndDecrement();

    /**
     * Atomically adds the given value to the current value.
     *
     * @param nDelta  the value to add
     *
     * @return the previous value
     */
    CompletableFuture<Integer> getAndAdd(int nDelta);

    /**
     * Atomically increments the current value.
     *
     * <p>Equivalent to {@code addAndGet(1)}.
     *
     * @return the updated value
     */
    CompletableFuture<Integer> incrementAndGet();

    /**
     * Atomically decrements the current value.
     *
     * <p>Equivalent to {@code addAndGet(-1)}.
     *
     * @return the updated value
     */
    CompletableFuture<Integer> decrementAndGet();

    /**
     * Atomically adds the given value to the current value.
     *
     * @param nDelta  the value to add
     *
     * @return the updated value
     */
    CompletableFuture<Integer> addAndGet(int nDelta);

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
    CompletableFuture<Integer> getAndUpdate(Remote.IntUnaryOperator updateFunction);

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
    CompletableFuture<Integer> getAndUpdate(IntUnaryOperator updateFunction);

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
    CompletableFuture<Integer> updateAndGet(Remote.IntUnaryOperator updateFunction);

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
    CompletableFuture<Integer> updateAndGet(IntUnaryOperator updateFunction);

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
     * @param nUpdate              the update value
     * @param accumulatorFunction  a side-effect-free function of two arguments
     *
     * @return the previous value
     */
    CompletableFuture<Integer> getAndAccumulate(int nUpdate, Remote.IntBinaryOperator accumulatorFunction);

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
     * @param nUpdate              the update value
     * @param accumulatorFunction  a side-effect-free function of two arguments
     *
     * @return the previous value
     */
    CompletableFuture<Integer> getAndAccumulate(int nUpdate, IntBinaryOperator accumulatorFunction);

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
     * @param nUpdate              the update value
     * @param accumulatorFunction  a side-effect-free function of two arguments
     *
     * @return the updated value
     */
    CompletableFuture<Integer> accumulateAndGet(int nUpdate, Remote.IntBinaryOperator accumulatorFunction);

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
     * @param nUpdate              the update value
     * @param accumulatorFunction  a side-effect-free function of two arguments
     *
     * @return the updated value
     */
    CompletableFuture<Integer> accumulateAndGet(int nUpdate, IntBinaryOperator accumulatorFunction);

    /**
     * Atomically sets the value to {@code newValue} if the current value,
     * referred to as the <em>witness value</em>, {@code == expectedValue}.
     *
     * @param nExpectedValue  the expected value
     * @param nNewValue       the new value
     *
     * @return the witness value, which will be the same as the expected value
     *         if successful
     */
    CompletableFuture<Integer> compareAndExchange(int nExpectedValue, int nNewValue);

    /**
     * Returns the current value of this {@code AsyncAtomicInteger} as an
     * {@code int}.
     *
     * @return the numeric value represented by this object
     */
    CompletableFuture<Integer> intValue();

    /**
     * Returns the current value of this {@code AsyncAtomicInteger} as a
     * {@code long} after a widening primitive conversion.
     *
     * @return the numeric value represented by this object after conversion
     *         to type {@code long}
     */
    CompletableFuture<Long> longValue();

    /**
     * Returns the current value of this {@code AsyncAtomicInteger} as a
     * {@code float} after a widening primitive conversion.
     *
     * @return the numeric value represented by this object after conversion
     *         to type {@code float}
     */
    CompletableFuture<Float> floatValue();

    /**
     * Returns the current value of this {@code AsyncAtomicInteger} as a
     * {@code double} after a widening primitive conversion.
     *
     * @return the numeric value represented by this object after conversion
     *         to type {@code double}
     */
    CompletableFuture<Double> doubleValue();

    /**
     * Returns the current value of this {@code AsyncAtomicInteger} as a
     * {@code byte} after a narrowing primitive conversion.
     *
     * @return the numeric value represented by this object after conversion
     *         to type {@code byte}
     */
    CompletableFuture<Byte> byteValue();

    /**
     * Returns the current value of this {@code AsyncAtomicInteger} as a
     * {@code short} after a narrowing primitive conversion.
     *
     * @return the numeric value represented by this object after conversion
     *         to type {@code short}
     */
    CompletableFuture<Short> shortValue();
    }
