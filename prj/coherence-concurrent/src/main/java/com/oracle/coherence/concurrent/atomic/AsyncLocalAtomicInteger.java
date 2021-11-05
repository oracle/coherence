/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import com.tangosol.util.function.Remote;

import java.util.concurrent.CompletableFuture;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Local implementation of {@link AsyncAtomicInteger} interface,
 * that simply wraps {@code java.util.concurrent.atomic.AtomicInteger}
 * instance and returns an already completed future from each method.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.12
 */
public class AsyncLocalAtomicInteger
        implements AsyncAtomicInteger
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct {@code LocalAtomicInteger} instance.
     *
     * @param value  wrapped value
     */
    protected AsyncLocalAtomicInteger(AtomicInteger value)
        {
        f_nValue = value;
        }

    // ----- AsyncAtomicInteger interface -----------------------------------

    @Override
    public CompletableFuture<Integer> get()
        {
        return completedFuture(f_nValue.get());
        }

    @Override
    public CompletableFuture<Void> set(int nNewValue)
        {
        f_nValue.set(nNewValue);
        return completedFuture(null);
        }

    @Override
    public CompletableFuture<Integer> getAndSet(int nNewValue)
        {
        return completedFuture(f_nValue.getAndSet(nNewValue));
        }

    @Override
    public CompletableFuture<Boolean> compareAndSet(int nExpectedValue, int nNewValue)
        {
        return completedFuture(f_nValue.compareAndSet(nExpectedValue, nNewValue));
        }

    @Override
    public CompletableFuture<Integer> getAndIncrement()
        {
        return getAndAdd(1);
        }

    @Override
    public CompletableFuture<Integer> getAndDecrement()
        {
        return getAndAdd(-1);
        }

    @Override
    public CompletableFuture<Integer> getAndAdd(int nDelta)
        {
        return completedFuture(f_nValue.getAndAdd(nDelta));
        }

    @Override
    public CompletableFuture<Integer> incrementAndGet()
        {
        return addAndGet(1);
        }

    @Override
    public CompletableFuture<Integer> decrementAndGet()
        {
        return addAndGet(-1);
        }

    @Override
    public CompletableFuture<Integer> addAndGet(int nDelta)
        {
        return completedFuture(f_nValue.addAndGet(nDelta));
        }

    @Override
    public CompletableFuture<Integer> getAndUpdate(Remote.IntUnaryOperator updateFunction)
        {
        return getAndUpdate((IntUnaryOperator) updateFunction);
        }

    @Override
    public CompletableFuture<Integer> getAndUpdate(IntUnaryOperator updateFunction)
        {
        return completedFuture(f_nValue.getAndUpdate(updateFunction));
        }

    @Override
    public CompletableFuture<Integer> updateAndGet(Remote.IntUnaryOperator updateFunction)
        {
        return updateAndGet((IntUnaryOperator) updateFunction);
        }

    @Override
    public CompletableFuture<Integer> updateAndGet(IntUnaryOperator updateFunction)
        {
        return completedFuture(f_nValue.updateAndGet(updateFunction));
        }

    @Override
    public CompletableFuture<Integer> getAndAccumulate(int x, Remote.IntBinaryOperator accumulatorFunction)
        {
        return getAndAccumulate(x, (IntBinaryOperator) accumulatorFunction);
        }

    @Override
    public CompletableFuture<Integer> getAndAccumulate(int nUpdate, IntBinaryOperator accumulatorFunction)
        {
        return completedFuture(f_nValue.getAndAccumulate(nUpdate, accumulatorFunction));
        }

    @Override
    public CompletableFuture<Integer> accumulateAndGet(int nUpdate, Remote.IntBinaryOperator accumulatorFunction)
        {
        return accumulateAndGet(nUpdate, (IntBinaryOperator) accumulatorFunction);
        }

    @Override
    public CompletableFuture<Integer> accumulateAndGet(int nUpdate, IntBinaryOperator accumulatorFunction)
        {
        return completedFuture(f_nValue.accumulateAndGet(nUpdate, accumulatorFunction));
        }

    @Override
    public CompletableFuture<Integer> compareAndExchange(int nExpectedValue, int nNewValue)
        {
        return completedFuture(f_nValue.compareAndExchange(nExpectedValue, nNewValue));
        }

    @Override
    public CompletableFuture<Integer> intValue()
        {
        return completedFuture(f_nValue.intValue());
        }

    @Override
    public CompletableFuture<Long> longValue()
        {
        return completedFuture(f_nValue.longValue());
        }

    @Override
    public CompletableFuture<Float> floatValue()
        {
        return completedFuture(f_nValue.floatValue());
        }

    @Override
    public CompletableFuture<Double> doubleValue()
        {
        return completedFuture(f_nValue.doubleValue());
        }

    @Override
    public CompletableFuture<Byte> byteValue()
        {
        return completedFuture(f_nValue.byteValue());
        }

    @Override
    public CompletableFuture<Short> shortValue()
        {
        return completedFuture(f_nValue.shortValue());
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
        return Integer.toString(get().join());
        }

    // ----- data members ---------------------------------------------------

    /**
     * Wrapped atomic int value.
     */
    private final AtomicInteger f_nValue;
    }
