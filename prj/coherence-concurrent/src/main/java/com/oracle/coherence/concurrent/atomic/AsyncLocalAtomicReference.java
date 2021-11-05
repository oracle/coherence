/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import com.tangosol.util.function.Remote;

import java.util.concurrent.CompletableFuture;

import java.util.concurrent.atomic.AtomicReference;

import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Local implementation of {@link AsyncAtomicReference } interface,
 * that simply wraps {@code java.util.concurrent.atomic.AtomicReference}
 * instance and returns an already completed future from each method.
 *
 * @param <V>  the type of object referred to by this reference
 *
 * @author Aleks Seovic  2020.12.08
 */
public class AsyncLocalAtomicReference<V>
        implements AsyncAtomicReference<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct {@code LocalAtomicReference<V>} instance.
     *
     * @param value  wrapped value
     */
    protected AsyncLocalAtomicReference(AtomicReference<V> value)
        {
        f_value = value;
        }

    // ----- AsyncAtomicReference interface ---------------------------------

    @Override
    public CompletableFuture<V> get()
        {
        return completedFuture(f_value.get());
        }

    @Override
    public CompletableFuture<Void> set(V newValue)
        {
        f_value.set(newValue);
        return completedFuture(null);
        }

    @Override
    public CompletableFuture<V> getAndSet(V newValue)
        {
        return completedFuture(f_value.getAndSet(newValue));
        }

    @Override
    public CompletableFuture<Boolean> compareAndSet(V expectedValue, V newValue)
        {
        return completedFuture(f_value.compareAndSet(expectedValue, newValue));
        }

    @Override
    public CompletableFuture<V> getAndUpdate(Remote.UnaryOperator<V> updateFunction)
        {
        return getAndUpdate((UnaryOperator<V>) updateFunction);
        }

    @Override
    public CompletableFuture<V> getAndUpdate(UnaryOperator<V> updateFunction)
        {
        return completedFuture(f_value.getAndUpdate(updateFunction));
        }

    @Override
    public CompletableFuture<V> updateAndGet(Remote.UnaryOperator<V> updateFunction)
        {
        return updateAndGet((UnaryOperator<V>) updateFunction);
        }

    @Override
    public CompletableFuture<V> updateAndGet(UnaryOperator<V> updateFunction)
        {
        return completedFuture(f_value.updateAndGet(updateFunction));
        }

    @Override
    public CompletableFuture<V> getAndAccumulate(V x, Remote.BinaryOperator<V> accumulatorFunction)
        {
        return getAndAccumulate(x, (BinaryOperator<V>) accumulatorFunction);
        }

    @Override
    public CompletableFuture<V> getAndAccumulate(V x, BinaryOperator<V> accumulatorFunction)
        {
        return completedFuture(f_value.getAndAccumulate(x, accumulatorFunction));
        }

    @Override
    public CompletableFuture<V> accumulateAndGet(V x, Remote.BinaryOperator<V> accumulatorFunction)
        {
        return accumulateAndGet(x, (BinaryOperator<V>) accumulatorFunction);
        }

    @Override
    public CompletableFuture<V> accumulateAndGet(V x, BinaryOperator<V> accumulatorFunction)
        {
        return completedFuture(f_value.accumulateAndGet(x, accumulatorFunction));
        }

    @Override
    public CompletableFuture<V> compareAndExchange(V expectedValue, V newValue)
        {
        return completedFuture(f_value.compareAndExchange(expectedValue, newValue));
        }

    // ----- Object methods -------------------------------------------------

    /**
     * Returns the String representation of the current value.
     *
     * @return the String representation of the current value
     */
    @Override
    public String toString()
        {
        return String.valueOf(get().join());
        }

    // ----- data members ---------------------------------------------------

    /**
     * Wrapped atomic value.
     */
    private final AtomicReference<V> f_value;
    }
