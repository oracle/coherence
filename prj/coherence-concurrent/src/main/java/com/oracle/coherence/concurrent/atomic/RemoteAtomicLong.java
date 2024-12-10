/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import com.tangosol.net.NamedMap;

import com.tangosol.util.function.Remote;

import java.util.concurrent.atomic.AtomicLong;

import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

/**
 * The remote implementation of {@link com.oracle.coherence.concurrent.atomic.AtomicLong AtomicLong},
 * backed by a Coherence {@code NamedMap} entry.
 * <p>
 * Every method in this class is guaranteed to execute effectively-once, and provides
 * cluster-wide atomicity guarantees for the backing atomic value. However,
 * keep in mind that this comes at a significant cost -- each method invocation
 * results in a network call to a remote owner of the backing atomic value,
 * which means that each operation has significantly higher latency than a
 * corresponding {@link LocalAtomicLong local} implementation.
 * <p>
 * To somewhat reduce that performance penalty, consider using non-blocking
 * {@link AsyncAtomicLong} implementation instead.
 *
 * @author Aleks Seovic  2020.12.03
 * @since 21.06
 */
public class RemoteAtomicLong
        extends Number
        implements com.oracle.coherence.concurrent.atomic.AtomicLong
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct {@code RemoteAtomicLong} instance.
     *
     * @param mapAtomic  the map that holds this atomic value
     * @param sName      the name of this atomic value
     */
    RemoteAtomicLong(NamedMap<String, AtomicLong> mapAtomic, String sName)
        {
        f_mapAtomic = mapAtomic;
        f_sName     = sName;
        }

    // ----- AtomicLong interface -------------------------------------------

    @Override
    public AsyncRemoteAtomicLong async()
        {
        return new AsyncRemoteAtomicLong(f_mapAtomic.async(), f_sName);
        }

    @Override
    public long get()
        {
        return invoke(AtomicLong::get, false);
        }

    @Override
    public void set(long lNewValue)
        {
        invoke(value ->
               {
               value.set(lNewValue);
               return null;
               });
        }

    @Override
    public long getAndSet(long lNewValue)
        {
        return invoke(value -> value.getAndSet(lNewValue));
        }

    @Override
    public boolean compareAndSet(long lExpectedValue, long lNewValue)
        {
        return invoke(value -> value.compareAndSet(lExpectedValue, lNewValue));
        }

    @Override
    public long getAndIncrement()
        {
        return getAndAdd(1);
        }

    @Override
    public long getAndDecrement()
        {
        return getAndAdd(-1);
        }

    @Override
    public long getAndAdd(long lDelta)
        {
        return invoke(value -> value.getAndAdd(lDelta));
        }

    @Override
    public long incrementAndGet()
        {
        return addAndGet(1);
        }

    @Override
    public long decrementAndGet()
        {
        return addAndGet(-1);
        }

    @Override
    public long addAndGet(long lDelta)
        {
        return invoke(value -> value.addAndGet(lDelta));
        }

    @Override
    public long getAndUpdate(Remote.LongUnaryOperator updateFunction)
        {
        return getAndUpdate((LongUnaryOperator) updateFunction);
        }

    @Override
    public long getAndUpdate(LongUnaryOperator updateFunction)
        {
        return invoke(value -> value.getAndUpdate(updateFunction));
        }

    @Override
    public long updateAndGet(Remote.LongUnaryOperator updateFunction)
        {
        return updateAndGet((LongUnaryOperator) updateFunction);
        }

    @Override
    public long updateAndGet(LongUnaryOperator updateFunction)
        {
        return invoke(value -> value.updateAndGet(updateFunction));
        }

    @Override
    public long getAndAccumulate(long lUpdate, Remote.LongBinaryOperator accumulatorFunction)
        {
        return getAndAccumulate(lUpdate, (LongBinaryOperator) accumulatorFunction);
        }

    @Override
    public long getAndAccumulate(long lUpdate, LongBinaryOperator accumulatorFunction)
        {
        return invoke(value -> value.getAndAccumulate(lUpdate, accumulatorFunction));
        }

    @Override
    public long accumulateAndGet(long lUpdate, Remote.LongBinaryOperator accumulatorFunction)
        {
        return accumulateAndGet(lUpdate, (LongBinaryOperator) accumulatorFunction);
        }

    @Override
    public long accumulateAndGet(long lUpdate, LongBinaryOperator accumulatorFunction)
        {
        return invoke(value -> value.accumulateAndGet(lUpdate, accumulatorFunction));
        }

    @Override
    public long compareAndExchange(long lExpectedValue, long lNewValue)
        {
        return invoke(value -> value.compareAndExchange(lExpectedValue, lNewValue));
        }

    @Override
    public int intValue()
        {
        return (int) get();
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
        return (double) get();
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
        return Long.toString(get());
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
    protected <R> R invoke(Remote.Function<AtomicLong, R> function)
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
    protected <R> R invoke(Remote.Function<AtomicLong, R> function, boolean fMutate)
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
    private final NamedMap<String, AtomicLong> f_mapAtomic;

    /**
     * The name of this atomic value.
     */
    private final String f_sName;
    }
