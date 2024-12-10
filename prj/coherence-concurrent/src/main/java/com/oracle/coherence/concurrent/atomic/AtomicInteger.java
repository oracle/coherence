/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.function.Remote;

import java.io.IOException;

import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

/**
 * An {@code int} value that may be updated atomically.
 * <p>
 * An {@code AtomicInteger} is used in applications
 * such as atomically incremented counters, and cannot be used
 * as a replacement for an {@code Integer}.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.12
 */
public interface AtomicInteger
    {
    /**
     * Return non-blocking API for this atomic value.
     *
     * @return non-blocking API for this atomic value
     */
    AsyncAtomicInteger async();

    /**
     * Returns the current value.
     *
     * @return the current value
     */
    int get();

    /**
     * Sets the value to {@code newValue}.
     *
     * @param nNewValue  the new value
     */
    void set(int nNewValue);

    /**
     * Atomically sets the value to {@code newValue} and returns the old value.
     *
     * @param nNewValue  the new value
     *
     * @return the previous value
     */
    int getAndSet(int nNewValue);

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
    boolean compareAndSet(int nExpectedValue, int nNewValue);

    /**
     * Atomically increments the current value.
     *
     * <p>Equivalent to {@code getAndAdd(1)}.
     *
     * @return the previous value
     */
    int getAndIncrement();

    /**
     * Atomically decrements the current value.
     *
     * <p>Equivalent to {@code getAndAdd(-1)}.
     *
     * @return the previous value
     */
    int getAndDecrement();

    /**
     * Atomically adds the given value to the current value.
     *
     * @param nDelta  the value to add
     *
     * @return the previous value
     */
    int getAndAdd(int nDelta);

    /**
     * Atomically increments the current value.
     *
     * <p>Equivalent to {@code addAndGet(1)}.
     *
     * @return the updated value
     */
    int incrementAndGet();

    /**
     * Atomically decrements the current value.
     *
     * <p>Equivalent to {@code addAndGet(-1)}.
     *
     * @return the updated value
     */
    int decrementAndGet();

    /**
     * Atomically adds the given value to the current value.
     *
     * @param nDelta  the value to add
     *
     * @return the updated value
     */
    int addAndGet(int nDelta);

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
    int getAndUpdate(Remote.IntUnaryOperator updateFunction);

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
    int getAndUpdate(IntUnaryOperator updateFunction);

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
    int updateAndGet(Remote.IntUnaryOperator updateFunction);

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
    int updateAndGet(IntUnaryOperator updateFunction);

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
    int getAndAccumulate(int nUpdate, Remote.IntBinaryOperator accumulatorFunction);

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
    int getAndAccumulate(int nUpdate, IntBinaryOperator accumulatorFunction);

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
    int accumulateAndGet(int nUpdate, Remote.IntBinaryOperator accumulatorFunction);

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
    int accumulateAndGet(int nUpdate, IntBinaryOperator accumulatorFunction);

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
    int compareAndExchange(int nExpectedValue, int nNewValue);

    /**
     * Returns the current value of this {@code AtomicInteger} as an
     * {@code int}.
     *
     * @return the numeric value represented by this object
     */
    int intValue();

    /**
     * Returns the current value of this {@code AtomicInteger} as a
     * {@code long} after a widening primitive conversion.
     *
     * @return the numeric value represented by this object after conversion
     *         to type {@code long}
     */
    long longValue();

    /**
     * Returns the current value of this {@code AtomicInteger} as a
     * {@code float} after a widening primitive conversion.
     *
     * @return the numeric value represented by this object after conversion
     *         to type {@code float}
     */
    float floatValue();

    /**
     * Returns the current value of this {@code AtomicInteger} as a
     * {@code double} after a widening primitive conversion.
     *
     * @return the numeric value represented by this object after conversion
     *         to type {@code double}
     */
    double doubleValue();

    /**
     * Returns the current value of this {@code AtomicInteger} as a
     * {@code byte} after a narrowing primitive conversion.
     *
     * @return the numeric value represented by this object after conversion
     *         to type {@code byte}
     */
    byte byteValue();

    /**
     * Returns the current value of this {@code AtomicInteger} as a
     * {@code short} after a narrowing primitive conversion.
     *
     * @return the numeric value represented by this object after conversion
     *         to type {@code short}
     */
    short shortValue();

    // ----- inner class: Serializer ----------------------------------------

    /**
     * POF serializer implementation.
     */
    class Serializer
            implements PofSerializer<java.util.concurrent.atomic.AtomicInteger>
        {
        // ----- PofSerializer interface ------------------------------------

        @Override
        public void serialize(PofWriter out, java.util.concurrent.atomic.AtomicInteger value)
                throws IOException
            {
            out.writeInt(0, value.get());
            out.writeRemainder(null);
            }

        @Override
        public java.util.concurrent.atomic.AtomicInteger deserialize(PofReader in)
                throws IOException
            {
            int nValue = in.readInt(0);

            in.readRemainder();
            return new java.util.concurrent.atomic.AtomicInteger(nValue);
            }
        }
    }
