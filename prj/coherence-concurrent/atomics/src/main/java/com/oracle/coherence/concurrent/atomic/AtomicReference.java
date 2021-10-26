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

import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

/**
 * An object reference that may be updated atomically.
 *
 * @param <V>  the type of object referred to by this reference
 *
 * @author Aleks Seovic  2020.12.08
 */
public interface AtomicReference<V>
    {
    /**
     * Return non-blocking API for this atomic reference.
     *
     * @return non-blocking API for this atomic reference
     */
    AsyncAtomicReference<V> async();

    /**
     * Returns the current value.
     *
     * @return the current value
     */
    V get();

    /**
     * Sets the value to {@code newValue}.
     *
     * @param newValue the new value
     */
    void set(V newValue);

    /**
     * Atomically sets the value to {@code newValue} and returns the old value.
     *
     * @param newValue  the new value
     *
     * @return the previous value
     */
    V getAndSet(V newValue);

    /**
     * Atomically sets the value to {@code newValue}
     * if the current value is <em>equal</em> to the {@code expectedValue}.
     *
     * @param expectedValue  the expected value
     * @param newValue       the new value
     *
     * @return {@code true} if successful. False return indicates that
     * the actual value was not equal to the expected value.
     */
    boolean compareAndSet(V expectedValue, V newValue);

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
    V getAndUpdate(Remote.UnaryOperator<V> updateFunction);

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
    V getAndUpdate(UnaryOperator<V> updateFunction);

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
    V updateAndGet(Remote.UnaryOperator<V> updateFunction);

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
    V updateAndGet(UnaryOperator<V> updateFunction);

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
    V getAndAccumulate(V x, Remote.BinaryOperator<V> accumulatorFunction);

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
    V getAndAccumulate(V x, BinaryOperator<V> accumulatorFunction);

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
    V accumulateAndGet(V x, Remote.BinaryOperator<V> accumulatorFunction);

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
    V accumulateAndGet(V x, BinaryOperator<V> accumulatorFunction);

    /**
     * Atomically sets the value to {@code newValue} if the current value,
     * referred to as the <em>witness value</em>, is <em>equal</em> to the
     * {@code == expectedValue}.
     *
     * @param expectedValue  the expected value
     * @param newValue       the new value
     *
     * @return the witness value, which will be the same as the expected value
     *         if successful
     */
    V compareAndExchange(V expectedValue, V newValue);

    // ----- inner class: Serializer ----------------------------------------

    /**
     * POF serializer implementation.
     *
     * @param <V>  the type of object referred to by this reference
     */
    class Serializer<V>
            implements PofSerializer<java.util.concurrent.atomic.AtomicReference<V>>
        {
        // ----- PofSerializer interface ------------------------------------

        @Override
        public void serialize(PofWriter out, java.util.concurrent.atomic.AtomicReference<V> value)
                throws IOException
            {
            out.writeObject(0, value.get());
            out.writeRemainder(null);
            }

        @Override
        public java.util.concurrent.atomic.AtomicReference<V> deserialize(PofReader in)
                throws IOException
            {
            V value = in.readObject(0);

            in.readRemainder();
            return new java.util.concurrent.atomic.AtomicReference<>(value);
            }
        }
    }
