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

import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

/**
 * A {@code long} value that may be updated atomically.
 * <p>
 * An {@code AtomicLong} is used in applications
 * such as atomically incremented sequence numbers, and cannot be used
 * as a replacement for a {@link java.lang.Long}.
 *
 * @author Aleks Seovic  2020.12.03
 * @since 21.12
 */
public interface AtomicLong
    {
    /**
     * Return non-blocking API for this atomic value.
     *
     * @return non-blocking API for this atomic value
     */
    AsyncAtomicLong async();

    /**
     * Returns the current value.
     *
     * @return the current value
     */
    long get();

    /**
     * Sets the value to {@code newValue}.
     *
     * @param lNewValue  the new value
     */
    void set(long lNewValue);

    /**
     * Atomically sets the value to {@code newValue} and returns the old value.
     *
     * @param lNewValue  the new value
     *
     * @return the previous value
     */
    long getAndSet(long lNewValue);

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
    boolean compareAndSet(long lExpectedValue, long lNewValue);

    /**
     * Atomically increments the current value.
     *
     * <p>Equivalent to {@code getAndAdd(1)}.
     *
     * @return the previous value
     */
    long getAndIncrement();

    /**
     * Atomically decrements the current value.
     *
     * <p>Equivalent to {@code getAndAdd(-1)}.
     *
     * @return the previous value
     */
    long getAndDecrement();

    /**
     * Atomically adds the given value to the current value.
     *
     * @param lDelta  the value to add
     *
     * @return the previous value
     */
    long getAndAdd(long lDelta);

    /**
     * Atomically increments the current value.
     *
     * <p>Equivalent to {@code addAndGet(1)}.
     *
     * @return the updated value
     */
    long incrementAndGet();

    /**
     * Atomically decrements the current value.
     *
     * <p>Equivalent to {@code addAndGet(-1)}.
     *
     * @return the updated value
     */
    long decrementAndGet();

    /**
     * Atomically adds the given value to the current value.
     *
     * @param lDelta  the value to add
     *
     * @return the updated value
     */
    long addAndGet(long lDelta);

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
    long getAndUpdate(Remote.LongUnaryOperator updateFunction);

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
    long getAndUpdate(LongUnaryOperator updateFunction);

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
    long updateAndGet(Remote.LongUnaryOperator updateFunction);

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
    long updateAndGet(LongUnaryOperator updateFunction);

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
    long getAndAccumulate(long lUpdate, Remote.LongBinaryOperator accumulatorFunction);

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
    long getAndAccumulate(long lUpdate, LongBinaryOperator accumulatorFunction);

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
    long accumulateAndGet(long lUpdate, Remote.LongBinaryOperator accumulatorFunction);

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
    long accumulateAndGet(long lUpdate, LongBinaryOperator accumulatorFunction);

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
    long compareAndExchange(long lExpectedValue, long lNewValue);

    /**
     * Returns the current value of this {@code AtomicLong} as an
     * {@code int} after a narrowing primitive conversion.
     *
     * @return the numeric value represented by this object after conversion
     *         to type {@code int}
     */
    int intValue();

    /**
     * Returns the current value of this {@code AtomicLong} as a
     * {@code long}.
     *
     * @return the numeric value represented by this object
     */
    long longValue();

    /**
     * Returns the current value of this {@code AtomicLong} as a
     * {@code float} after a widening primitive conversion.
     *
     * @return the numeric value represented by this object after conversion
     *         to type {@code float}
     */
    float floatValue();

    /**
     * Returns the current value of this {@code AtomicLong} as a
     * {@code double} after a widening primitive conversion.
     *
     * @return the numeric value represented by this object after conversion
     *         to type {@code double}
     */
    double doubleValue();

    /**
     * Returns the current value of this {@code AtomicLong} as a
     * {@code byte} after a narrowing primitive conversion.
     *
     * @return the numeric value represented by this object after conversion
     *         to type {@code byte}
     */
    byte byteValue();

    /**
     * Returns the current value of this {@code AtomicLong} as a
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
            implements PofSerializer<java.util.concurrent.atomic.AtomicLong>
        {

        // ----- PofSerializer interface ------------------------------------

        @Override
        public void serialize(PofWriter out, java.util.concurrent.atomic.AtomicLong value)
                throws IOException
            {
            out.writeLong(0, value.get());
            out.writeRemainder(null);
            }

        @Override
        public java.util.concurrent.atomic.AtomicLong deserialize(PofReader in)
                throws IOException
            {
            long nValue = in.readLong(0);

            in.readRemainder();
            return new java.util.concurrent.atomic.AtomicLong(nValue);
            }
        }
    }
