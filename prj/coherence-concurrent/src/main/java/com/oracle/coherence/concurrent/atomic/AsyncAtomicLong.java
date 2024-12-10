/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import com.tangosol.util.function.Remote;

import java.util.concurrent.CompletableFuture;

import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

/**
 * A {@code long} value that may be updated atomically.
 * <p>
 * Unlike {@link AtomicLong}, each method from this interface is non-blocking,
 * which allows asynchronous invocation and consumption of the return value
 * via {@code CompletableFuture} API. This is particularly useful when using
 * {@link AsyncRemoteAtomicLong remote} implementation, because of relatively
 * high latency associated with an inevitable network call, but we do provide a
 * {@link AsyncLocalAtomicLong local} implementation as well.
 * <p>
 * An {@code AsyncAtomicLong} is used in applications
 * such as atomically incremented sequence numbers, and cannot be used
 * as a replacement for a {@link java.lang.Long}.
 *
 * @author Aleks Seovic  2020.12.03
 * @since 21.12
 */
public interface AsyncAtomicLong
    {
    /**
     * Returns the current value.
     *
     * @return the current value
     */
    CompletableFuture<Long> get();

    /**
     * Sets the value to {@code newValue}.
     *
     * @param lNewValue  the new value
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     */
    CompletableFuture<Void> set(long lNewValue);

    /**
     * Atomically sets the value to {@code newValue} and returns the old value.
     *
     * @param lNewValue  the new value
     *
     * @return the previous value
     */
    CompletableFuture<Long> getAndSet(long lNewValue);

    /**
     * Atomically sets the value to {@code newValue}
     * if the current value {@code == expectedValue}.
     *
     * @param lExpectedValue  the expected value
     * @param lNewValue       the new value
     *
     * @return {@code true} if successful. False return indicates that
     *         the actual value was not equal to the expected value.
     */
    CompletableFuture<Boolean> compareAndSet(long lExpectedValue, long lNewValue);

    /**
     * Atomically increments the current value.
     *
     * <p>Equivalent to {@code getAndAdd(1)}.
     *
     * @return the previous value
     */
    CompletableFuture<Long> getAndIncrement();

    /**
     * Atomically decrements the current value.
     *
     * <p>Equivalent to {@code getAndAdd(-1)}.
     *
     * @return the previous value
     */
    CompletableFuture<Long> getAndDecrement();

    /**
     * Atomically adds the given value to the current value.
     *
     * @param lDelta  the value to add
     *
     * @return the previous value
     */
    CompletableFuture<Long> getAndAdd(long lDelta);

    /**
     * Atomically increments the current value.
     *
     * <p>Equivalent to {@code addAndGet(1)}.
     *
     * @return the updated value
     */
    CompletableFuture<Long> incrementAndGet();

    /**
     * Atomically decrements the current value.
     *
     * <p>Equivalent to {@code addAndGet(-1)}.
     *
     * @return the updated value
     */
    CompletableFuture<Long> decrementAndGet();

    /**
     * Atomically adds the given value to the current value.
     *
     * @param lDelta  the value to add
     *
     * @return the updated value
     */
    CompletableFuture<Long> addAndGet(long lDelta);

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
    CompletableFuture<Long> getAndUpdate(Remote.LongUnaryOperator updateFunction);

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
    CompletableFuture<Long> getAndUpdate(LongUnaryOperator updateFunction);

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
    CompletableFuture<Long> updateAndGet(Remote.LongUnaryOperator updateFunction);

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
    CompletableFuture<Long> updateAndGet(LongUnaryOperator updateFunction);

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
     * @param lUpdate              the update value
     * @param accumulatorFunction  a side-effect-free function of two arguments
     *
     * @return the previous value
     */
    CompletableFuture<Long> getAndAccumulate(long lUpdate, Remote.LongBinaryOperator accumulatorFunction);

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
     * @param lUpdate              the update value
     * @param accumulatorFunction  a side-effect-free function of two arguments
     *
     * @return the previous value
     */
    CompletableFuture<Long> getAndAccumulate(long lUpdate, LongBinaryOperator accumulatorFunction);

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
     * @param lUpdate              the update value
     * @param accumulatorFunction  a side-effect-free function of two arguments
     *
     * @return the updated value
     */
    CompletableFuture<Long> accumulateAndGet(long lUpdate, Remote.LongBinaryOperator accumulatorFunction);

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
     * @param lUpdate              the update value
     * @param accumulatorFunction  a side-effect-free function of two arguments
     *
     * @return the updated value
     */
    CompletableFuture<Long> accumulateAndGet(long lUpdate, LongBinaryOperator accumulatorFunction);

    /**
     * Atomically sets the value to {@code newValue} if the current value,
     * referred to as the <em>witness value</em>, {@code == expectedValue}.
     *
     * @param lExpectedValue  the expected value
     * @param lNewValue       the new value
     *
     * @return the witness value, which will be the same as the expected value
     *         if successful
     */
    CompletableFuture<Long> compareAndExchange(long lExpectedValue, long lNewValue);

    /**
     * Returns the current value of this {@code DistributedAtomicLong} as an
     * {@code int} after a narrowing primitive conversion.
     *
     * @return the current value of this {@code DistributedAtomicLong} as an
     *         {@code int} after a narrowing primitive conversion
     */
    CompletableFuture<Integer> intValue();

    /**
     * Returns the current value of this {@code DistributedAtomicLong} as a
     * {@code long}.
     *
     * Equivalent to {@link #get()}.
     *
     * @return the current value of this {@code DistributedAtomicLong} as a
     *         {@code long}
     */
    CompletableFuture<Long> longValue();

    /**
     * Returns the current value of this {@code DistributedAtomicLong} as a
     * {@code float} after a widening primitive conversion.
     *
     * @return the current value of this {@code DistributedAtomicLong} as a
     *         {@code float} after a widening primitive conversion.
     */
    CompletableFuture<Float> floatValue();

    /**
     * Returns the current value of this {@code DistributedAtomicLong} as a
     * {@code double} after a widening primitive conversion.
     *
     * @return the current value of this {@code DistributedAtomicLong} as a
     *         {@code double} after a widening primitive conversion
     */
    CompletableFuture<Double> doubleValue();

    /**
     * Returns the value of the specified number as a {@code byte}.
     *
     * <p>This implementation returns the result of {@link #intValue} cast
     * to a {@code byte}.
     *
     * @return the numeric value represented by this object after conversion
     *         to type {@code byte}.
     */
    CompletableFuture<Byte> byteValue();

    /**
     * Returns the value of the specified number as a {@code short}.
     *
     * <p>This implementation returns the result of {@link #intValue} cast
     * to a {@code short}.
     *
     * @return the numeric value represented by this object after conversion
     *         to type {@code short}.
     */
    CompletableFuture<Short> shortValue();
    }
