/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import com.tangosol.util.function.Remote;

import java.util.concurrent.atomic.AtomicReference;

import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

/**
 * Local implementation of {@link com.oracle.coherence.concurrent.atomic.AtomicReference AtomicReference}
 * interface, that simply wraps {@code java.util.concurrent.atomic.AtomicReference} instance.
 *
 * @param <V>  the type of object referred to by this reference
 *
 * @author Aleks Seovic  2020.12.08
 */
public class LocalAtomicReference<V>
        implements com.oracle.coherence.concurrent.atomic.AtomicReference<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct {@code LocalAtomicReference<V>} instance.
     *
     * @param value  initial value
     */
    protected LocalAtomicReference(V value)
        {
        this(new AtomicReference<>(value));
        }

    /**
     * Construct {@code LocalAtomicReference<V>} instance.
     *
     * @param value  wrapped value
     */
    protected LocalAtomicReference(AtomicReference<V> value)
        {
        f_value = value;
        }

    // ----- AtomicReference interface --------------------------------------

    @Override
    public AsyncLocalAtomicReference<V> async()
        {
        return new AsyncLocalAtomicReference<>(f_value);
        }

    @Override
    public V get()
        {
        return f_value.get();
        }

    @Override
    public void set(V newValue)
        {
        f_value.set(newValue);
        }

    @Override
    public V getAndSet(V newValue)
        {
        return f_value.getAndSet(newValue);
        }

    @Override
    public boolean compareAndSet(V expectedValue, V newValue)
        {
        return f_value.compareAndSet(expectedValue, newValue);
        }

    @Override
    public V getAndUpdate(Remote.UnaryOperator<V> updateFunction)
        {
        return getAndUpdate((UnaryOperator<V>) updateFunction);
        }

    @Override
    public V getAndUpdate(UnaryOperator<V> updateFunction)
        {
        return f_value.getAndUpdate(updateFunction);
        }

    @Override
    public V updateAndGet(Remote.UnaryOperator<V> updateFunction)
        {
        return updateAndGet((UnaryOperator<V>) updateFunction);
        }

    @Override
    public V updateAndGet(UnaryOperator<V> updateFunction)
        {
        return f_value.updateAndGet(updateFunction);
        }

    @Override
    public V getAndAccumulate(V x, Remote.BinaryOperator<V> accumulatorFunction)
        {
        return getAndAccumulate(x, (BinaryOperator<V>) accumulatorFunction);
        }

    @Override
    public V getAndAccumulate(V x, BinaryOperator<V> accumulatorFunction)
        {
        return f_value.getAndAccumulate(x, accumulatorFunction);
        }

    @Override
    public V accumulateAndGet(V x, Remote.BinaryOperator<V> accumulatorFunction)
        {
        return accumulateAndGet(x, (BinaryOperator<V>) accumulatorFunction);
        }

    @Override
    public V accumulateAndGet(V x, BinaryOperator<V> accumulatorFunction)
        {
        return f_value.accumulateAndGet(x, accumulatorFunction);
        }

    @Override
    public V compareAndExchange(V expectedValue, V newValue)
        {
        return f_value.compareAndExchange(expectedValue, newValue);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return f_value.toString();
        }

    // ----- data members ---------------------------------------------------

    /**
     * Wrapped atomic value.
     */
    private final AtomicReference<V> f_value;
    }
