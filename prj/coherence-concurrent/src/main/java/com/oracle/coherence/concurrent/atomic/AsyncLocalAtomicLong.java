/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import com.tangosol.util.function.Remote;

import java.util.concurrent.CompletableFuture;

import java.util.concurrent.atomic.AtomicLong;

import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Local implementation of {@link AsyncAtomicLong} interface,
 * that simply wraps {@code java.util.concurrent.atomic.AtomicLong}
 * instance and returns an already completed future from each method.
 *
 * @author Aleks Seovic  2020.12.03
 */
public class AsyncLocalAtomicLong
        implements AsyncAtomicLong
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct {@code LocalAtomicLong} instance.
     *
     * @param value  wrapped value
     */
    protected AsyncLocalAtomicLong(AtomicLong value)
        {
        f_lValue = value;
        }

    // ----- AsyncAtomicLong interface --------------------------------------

    @Override
    public CompletableFuture<Long> get()
        {
        return completedFuture(f_lValue.get());
        }

    @Override
    public CompletableFuture<Void> set(long lNewValue)
        {
        f_lValue.set(lNewValue);
        return completedFuture(null);
        }

    @Override
    public CompletableFuture<Long> getAndSet(long lNewValue)
        {
        return completedFuture(f_lValue.getAndSet(lNewValue));
        }

    @Override
    public CompletableFuture<Boolean> compareAndSet(long lExpectedValue, long lNewValue)
        {
        return completedFuture(f_lValue.compareAndSet(lExpectedValue, lNewValue));
        }

    @Override
    public CompletableFuture<Long> getAndIncrement()
        {
        return getAndAdd(1);
        }

    @Override
    public CompletableFuture<Long> getAndDecrement()
        {
        return getAndAdd(-1);
        }

    @Override
    public CompletableFuture<Long> getAndAdd(long lDelta)
        {
        return completedFuture(f_lValue.getAndAdd(lDelta));
        }

    @Override
    public CompletableFuture<Long> incrementAndGet()
        {
        return addAndGet(1);
        }

    @Override
    public CompletableFuture<Long> decrementAndGet()
        {
        return addAndGet(-1);
        }

    @Override
    public CompletableFuture<Long> addAndGet(long lDelta)
        {
        return completedFuture(f_lValue.addAndGet(lDelta));
        }

    @Override
    public CompletableFuture<Long> getAndUpdate(Remote.LongUnaryOperator updateFunction)
        {
        return getAndUpdate((LongUnaryOperator) updateFunction);
        }

    @Override
    public CompletableFuture<Long> getAndUpdate(LongUnaryOperator updateFunction)
        {
        return completedFuture(f_lValue.getAndUpdate(updateFunction));
        }

    @Override
    public CompletableFuture<Long> updateAndGet(Remote.LongUnaryOperator updateFunction)
        {
        return updateAndGet((LongUnaryOperator) updateFunction);
        }

    @Override
    public CompletableFuture<Long> updateAndGet(LongUnaryOperator updateFunction)
        {
        return completedFuture(f_lValue.updateAndGet(updateFunction));
        }

    @Override
    public CompletableFuture<Long> getAndAccumulate(long lUpdate, Remote.LongBinaryOperator accumulatorFunction)
        {
        return getAndAccumulate(lUpdate, (LongBinaryOperator) accumulatorFunction);
        }

    @Override
    public CompletableFuture<Long> getAndAccumulate(long lUpdate, LongBinaryOperator accumulatorFunction)
        {
        return completedFuture(f_lValue.getAndAccumulate(lUpdate, accumulatorFunction));
        }

    @Override
    public CompletableFuture<Long> accumulateAndGet(long lUpdate, Remote.LongBinaryOperator accumulatorFunction)
        {
        return accumulateAndGet(lUpdate, (LongBinaryOperator) accumulatorFunction);
        }

    @Override
    public CompletableFuture<Long> accumulateAndGet(long lUpdate, LongBinaryOperator accumulatorFunction)
        {
        return completedFuture(f_lValue.accumulateAndGet(lUpdate, accumulatorFunction));
        }

    @Override
    public CompletableFuture<Long> compareAndExchange(long lExpectedValue, long lNewValue)
        {
        return completedFuture(f_lValue.compareAndExchange(lExpectedValue, lNewValue));
        }

    @Override
    public CompletableFuture<Integer> intValue()
        {
        return completedFuture(f_lValue.intValue());
        }

    @Override
    public CompletableFuture<Long> longValue()
        {
        return completedFuture(f_lValue.longValue());
        }

    @Override
    public CompletableFuture<Float> floatValue()
        {
        return completedFuture(f_lValue.floatValue());
        }

    @Override
    public CompletableFuture<Double> doubleValue()
        {
        return completedFuture(f_lValue.doubleValue());
        }

    @Override
    public CompletableFuture<Byte> byteValue()
        {
        return completedFuture(f_lValue.byteValue());
        }

    @Override
    public CompletableFuture<Short> shortValue()
        {
        return completedFuture(f_lValue.shortValue());
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
        return Long.toString(get().join());
        }

    // ----- data members ---------------------------------------------------

    /**
     * Wrapped atomic long value.
     */
    private final AtomicLong f_lValue;
    }
