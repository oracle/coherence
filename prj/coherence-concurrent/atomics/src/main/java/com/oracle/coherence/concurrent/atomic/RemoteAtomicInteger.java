/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import com.tangosol.net.NamedMap;

import com.tangosol.util.function.Remote;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

/**
 * The remote implementation of {@link com.oracle.coherence.concurrent.atomic.AtomicInteger AtomicInteger},
 * backed by a Coherence {@code NamedMap} entry.
 * <p>
 * Every method in this class is guaranteed to execute effectively-once, and provides
 * cluster-wide atomicity guarantees for the backing atomic value. However,
 * keep in mind that this comes at a significant cost -- each method invocation
 * results in a network call to a remote owner of the backing atomic value,
 * which means that each operation has significantly higher latency than a
 * corresponding {@link LocalAtomicInteger local} implementation.
 * <p>
 * To somewhat reduce that performance penalty, consider using non-blocking
 * {@link AsyncAtomicInteger} implementation instead.
 *
 * @author Aleks Seovic  2020.12.03
 * @since 21.06
 */
public class RemoteAtomicInteger
        extends Number
        implements com.oracle.coherence.concurrent.atomic.AtomicInteger
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct {@code RemoteAtomicInteger} instance.
     *
     * @param mapAtomic  the map that holds this atomic value
     * @param sName      the name of this atomic value
     */
    protected RemoteAtomicInteger(NamedMap<String, AtomicInteger> mapAtomic, String sName)
        {
        f_mapAtomic = mapAtomic;
        f_sName     = sName;
        }

    // ----- AtomicInteger interface ----------------------------------------

    @Override
    public AsyncRemoteAtomicInteger async()
        {
        return new AsyncRemoteAtomicInteger(f_mapAtomic.async(), f_sName);
        }

    @Override
    public int get()
        {
        return invoke(AtomicInteger::get, false);
        }

    @Override
    public void set(int nNewValue)
        {
        invoke(value ->
               {
               value.set(nNewValue);
               return null;
               });
        }

    @Override
    public int getAndSet(int nNewValue)
        {
        return invoke(value -> value.getAndSet(nNewValue));
        }

    @Override
    public boolean compareAndSet(int nExpectedValue, int nNewValue)
        {
        return invoke(value -> value.compareAndSet(nExpectedValue, nNewValue));
        }

    @Override
    public int getAndIncrement()
        {
        return getAndAdd(1);
        }

    @Override
    public int getAndDecrement()
        {
        return getAndAdd(-1);
        }

    @Override
    public int getAndAdd(int nDelta)
        {
        return invoke(value -> value.getAndAdd(nDelta));
        }

    @Override
    public int incrementAndGet()
        {
        return addAndGet(1);
        }

    @Override
    public int decrementAndGet()
        {
        return addAndGet(-1);
        }

    @Override
    public int addAndGet(int nDelta)
        {
        return invoke(value -> value.addAndGet(nDelta));
        }

    @Override
    public int getAndUpdate(Remote.IntUnaryOperator updateFunction)
        {
        return getAndUpdate((IntUnaryOperator) updateFunction);
        }

    @Override
    public int getAndUpdate(IntUnaryOperator updateFunction)
        {
        return invoke(value -> value.getAndUpdate(updateFunction));
        }

    @Override
    public int updateAndGet(Remote.IntUnaryOperator updateFunction)
        {
        return updateAndGet((IntUnaryOperator) updateFunction);
        }

    @Override
    public int updateAndGet(IntUnaryOperator updateFunction)
        {
        return invoke(value -> value.updateAndGet(updateFunction));
        }

    @Override
    public int getAndAccumulate(int nUpdate, Remote.IntBinaryOperator accumulatorFunction)
        {
        return getAndAccumulate(nUpdate, (IntBinaryOperator) accumulatorFunction);
        }

    @Override
    public int getAndAccumulate(int nUpdate, IntBinaryOperator accumulatorFunction)
        {
        return invoke(value -> value.getAndAccumulate(nUpdate, accumulatorFunction));
        }

    @Override
    public int accumulateAndGet(int nUpdate, Remote.IntBinaryOperator accumulatorFunction)
        {
        return accumulateAndGet(nUpdate, (IntBinaryOperator) accumulatorFunction);
        }

    @Override
    public int accumulateAndGet(int nUpdate, IntBinaryOperator accumulatorFunction)
        {
        return invoke(value -> value.accumulateAndGet(nUpdate, accumulatorFunction));
        }

    @Override
    public int compareAndExchange(int nExpectedValue, int nNewValue)
        {
        return invoke(value -> value.compareAndExchange(nExpectedValue, nNewValue));
        }

    @Override
    public int intValue()
        {
        return get();
        }

    @Override
    public long longValue()
        {
        return get();
        }

    @Override
    public float floatValue()
        {
        return (float) get();
        }

    @Override
    public double doubleValue()
        {
        return get();
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
        return Integer.toString(get());
        }

    // ----- helper methods -------------------------------------------------

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
    protected <R> R invoke(Remote.Function<AtomicInteger, R> function)
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
    protected <R> R invoke(Remote.Function<AtomicInteger, R> function, boolean fMutate)
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
    private final NamedMap<String, AtomicInteger> f_mapAtomic;

    /**
     * The name of this atomic value.
     */
    private final String f_sName;
    }
