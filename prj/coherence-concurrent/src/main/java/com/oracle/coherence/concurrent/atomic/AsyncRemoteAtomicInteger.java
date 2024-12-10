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

import java.util.concurrent.atomic.AtomicInteger;

import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

/**
 * The remote implementation of {@link AsyncAtomicInteger}, backed by a
 * Coherence {@code NamedMap} entry.
 * <p>
 * Every method in this class is guaranteed to execute effectively-once, and provides
 * cluster-wide atomicity guarantees for the backing atomic value. However,
 * keep in mind that this comes at a significant cost -- each method invocation
 * results in a network call to a remote owner of the backing atomic value,
 * which means that each operation has significantly higher latency than a
 * corresponding {@link AsyncLocalAtomicInteger local} implementation.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.12
 */
public class AsyncRemoteAtomicInteger
        implements AsyncAtomicInteger
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a new {@code AsyncRemoteAtomicInteger}.
     *
     * @param mapAtomic  the map that holds this atomic value
     * @param sName      the name of this atomic value
     */
    protected AsyncRemoteAtomicInteger(AsyncNamedMap<String, AtomicInteger> mapAtomic, String sName)
        {
        f_mapAtomic = mapAtomic;
        f_sName     = sName;
        }

    // ----- AsyncAtomicInteger interface -----------------------------------

    @Override
    public CompletableFuture<Integer> get()
        {
        return invoke(AtomicInteger::get, false);
        }

    @Override
    public CompletableFuture<Void> set(int nNewValue)
        {
        return invoke(value ->
               {
               value.set(nNewValue);
               return null;
               });
        }

    @Override
    public CompletableFuture<Integer> getAndSet(int nNewValue)
        {
        return invoke(value -> value.getAndSet(nNewValue));
        }

    @Override
    public CompletableFuture<Boolean> compareAndSet(int nExpectedValue, int nNewValue)
        {
        return invoke(value -> value.compareAndSet(nExpectedValue, nNewValue));
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
        return invoke(value -> value.getAndAdd(nDelta));
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
        return invoke(value -> value.addAndGet(nDelta));
        }

    @Override
    public CompletableFuture<Integer> getAndUpdate(Remote.IntUnaryOperator updateFunction)
        {
        return getAndUpdate((IntUnaryOperator) updateFunction);
        }

    @Override
    public CompletableFuture<Integer> getAndUpdate(IntUnaryOperator updateFunction)
        {
        return invoke(value -> value.getAndUpdate(updateFunction));
        }

    @Override
    public CompletableFuture<Integer> updateAndGet(Remote.IntUnaryOperator updateFunction)
        {
        return updateAndGet((IntUnaryOperator) updateFunction);
        }

    @Override
    public CompletableFuture<Integer> updateAndGet(IntUnaryOperator updateFunction)
        {
        return invoke(value -> value.updateAndGet(updateFunction));
        }

    @Override
    public CompletableFuture<Integer> getAndAccumulate(int nUpdate, Remote.IntBinaryOperator accumulatorFunction)
        {
        return getAndAccumulate(nUpdate, (IntBinaryOperator) accumulatorFunction);
        }

    @Override
    public CompletableFuture<Integer> getAndAccumulate(int nUpdate, IntBinaryOperator accumulatorFunction)
        {
        return invoke(value -> value.getAndAccumulate(nUpdate, accumulatorFunction));
        }

    @Override
    public CompletableFuture<Integer> accumulateAndGet(int nUpdate, Remote.IntBinaryOperator accumulatorFunction)
        {
        return accumulateAndGet(nUpdate, (IntBinaryOperator) accumulatorFunction);
        }

    @Override
    public CompletableFuture<Integer> accumulateAndGet(int nUpdate, IntBinaryOperator accumulatorFunction)
        {
        return invoke(value -> value.accumulateAndGet(nUpdate, accumulatorFunction));
        }

    @Override
    public CompletableFuture<Integer> compareAndExchange(int nExpectedValue, int nNewValue)
        {
        return invoke(value -> value.compareAndExchange(nExpectedValue, nNewValue));
        }

    @Override
    public CompletableFuture<Integer> intValue()
        {
        return get();
        }

    @Override
    public CompletableFuture<Long> longValue()
        {
        return get().thenApply(Integer::longValue);
        }

    @Override
    public CompletableFuture<Float> floatValue()
        {
        return get().thenApply(Integer::floatValue);
        }

    @Override
    public CompletableFuture<Double> doubleValue()
        {
        return get().thenApply(Integer::doubleValue);
        }

    @Override
    public CompletableFuture<Byte> byteValue()
        {
        return get().thenApply(Integer::byteValue);
        }

    @Override
    public CompletableFuture<Short> shortValue()
        {
        return get().thenApply(Integer::shortValue);
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
    protected <R> CompletableFuture<R> invoke(Remote.Function<AtomicInteger, R> function)
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
    protected <R> CompletableFuture<R> invoke(Remote.Function<AtomicInteger, R> function, boolean fMutate)
        {
        return f_mapAtomic.invoke(f_sName, entry ->
                {
                AtomicInteger value  = entry.getValue();
                R             result = function.apply(value);

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
    private final AsyncNamedMap<String, AtomicInteger> f_mapAtomic;

    /**
     * The name of this atomic value.
     */
    private final String f_sName;
    }
