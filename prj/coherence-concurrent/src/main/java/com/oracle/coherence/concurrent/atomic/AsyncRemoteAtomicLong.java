/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import com.tangosol.net.AsyncNamedMap;

import com.tangosol.util.function.Remote;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

/**
 * The remote implementation of {@link AsyncAtomicLong}, backed by a
 * Coherence {@code NamedMap} entry.
 * <p>
 * Every method in this class is guaranteed to execute effectively-once, and provides
 * cluster-wide atomicity guarantees for the backing atomic value. However,
 * keep in mind that this comes at a significant cost -- each method invocation
 * results in a network call to a remote owner of the backing atomic value,
 * which means that each operation has significantly higher latency than a
 * corresponding {@link AsyncLocalAtomicLong local} implementation.
 *
 * @author Aleks Seovic  2020.12.03
 * @since 21.12
 */
public class AsyncRemoteAtomicLong
        implements AsyncAtomicLong
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a new {@code AsyncRemoteAtomicLong}.
     *
     * @param mapAtomic  the map that holds this atomic value
     * @param sName      the name of this atomic value
     */
    protected AsyncRemoteAtomicLong(AsyncNamedMap<String, AtomicLong> mapAtomic, String sName)
        {
        f_mapAtomic = mapAtomic;
        f_sName     = sName;
        }

    // ----- AsyncAtomicLong interface --------------------------------------

    @Override
    public CompletableFuture<Long> get()
        {
        return invoke(AtomicLong::get, false);
        }

    @Override
    public CompletableFuture<Void> set(long lNewValue)
        {
        return invoke(value ->
               {
               value.set(lNewValue);
               return null;
               });
        }

    @Override
    public CompletableFuture<Long> getAndSet(long lNewValue)
        {
        return invoke(value -> value.getAndSet(lNewValue));
        }

    @Override
    public CompletableFuture<Boolean> compareAndSet(long lExpectedValue, long lNewValue)
        {
        return invoke(value -> value.compareAndSet(lExpectedValue, lNewValue));
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
        return invoke(value -> value.getAndAdd(lDelta));
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
        return invoke(value -> value.addAndGet(lDelta));
        }

    @Override
    public CompletableFuture<Long> getAndUpdate(Remote.LongUnaryOperator updateFunction)
        {
        return getAndUpdate((LongUnaryOperator) updateFunction);
        }

    @Override
    public CompletableFuture<Long> getAndUpdate(LongUnaryOperator updateFunction)
        {
        return invoke(value -> value.getAndUpdate(updateFunction));
        }

    @Override
    public CompletableFuture<Long> updateAndGet(Remote.LongUnaryOperator updateFunction)
        {
        return updateAndGet((LongUnaryOperator) updateFunction);
        }

    @Override
    public CompletableFuture<Long> updateAndGet(LongUnaryOperator updateFunction)
        {
        return invoke(value -> value.updateAndGet(updateFunction));
        }

    @Override
    public CompletableFuture<Long> getAndAccumulate(long lUpdate, Remote.LongBinaryOperator accumulatorFunction)
        {
        return getAndAccumulate(lUpdate, (LongBinaryOperator) accumulatorFunction);
        }

    @Override
    public CompletableFuture<Long> getAndAccumulate(long lUpdate, LongBinaryOperator accumulatorFunction)
        {
        return invoke(value -> value.getAndAccumulate(lUpdate, accumulatorFunction));
        }

    @Override
    public CompletableFuture<Long> accumulateAndGet(long lUpdate, Remote.LongBinaryOperator accumulatorFunction)
        {
        return accumulateAndGet(lUpdate, (LongBinaryOperator) accumulatorFunction);
        }

    @Override
    public CompletableFuture<Long> accumulateAndGet(long lUpdate, LongBinaryOperator accumulatorFunction)
        {
        return invoke(value -> value.accumulateAndGet(lUpdate, accumulatorFunction));
        }

    @Override
    public CompletableFuture<Long> compareAndExchange(long lExpectedValue, long lNewValue)
        {
        return invoke(value -> value.compareAndExchange(lExpectedValue, lNewValue));
        }

    @Override
    public CompletableFuture<Integer> intValue()
        {
        return get().thenApply(Long::intValue);
        }

    @Override
    public CompletableFuture<Long> longValue()
        {
        return get();
        }

    @Override
    public CompletableFuture<Float> floatValue()
        {
        return get().thenApply(Long::floatValue);
        }

    @Override
    public CompletableFuture<Double> doubleValue()
        {
        return get().thenApply(Long::doubleValue);
        }

    @Override
    public CompletableFuture<Byte> byteValue()
        {
        return get().thenApply(Long::byteValue);
        }

    @Override
    public CompletableFuture<Short> shortValue()
        {
        return get().thenApply(Long::shortValue);
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

    // ----- helpers methods ------------------------------------------------

    /**
     * Apply specified function against the remote object and return the result.
     *
     * <p>Any changes the function makes to the remote object will be preserved.
     *
     * @param function  the function to apply
     * @param <R>       the type of the result
     *
     * @return the result of the function applied to a remote object
     */
    protected <R> CompletableFuture<R> invoke(Remote.Function<AtomicLong, R> function)
        {
        return invoke(function, true);
        }

    /**
     * Apply specified function against the remote object and return the result.
     *
     * <p>If the {@code fMutate} argument is {@code true}, any changes to the
     * remote object will be preserved.
     *
     * @param function  the function to apply
     * @param fMutate   flag specifying whether the function mutates the object
     * @param <R>       the type of the result
     *
     * @return the result of the function applied to a remote object
     */
    protected <R> CompletableFuture<R> invoke(Remote.Function<AtomicLong, R> function, boolean fMutate)
        {
        return f_mapAtomic.invoke(f_sName, entry ->
                {
                AtomicLong value  = entry.getValue();
                R          result = function.apply(value);

                if (fMutate)
                    {
                    entry.setValue(value);
                    }
                return result;
                });
        }

    // ----- data members ---------------------------------------------------

    /**
     * The map that holds this atomic value.
     */
    private final AsyncNamedMap<String, AtomicLong> f_mapAtomic;

    /**
     * The name of this atomic value.
     */
    private final String f_sName;
    }
