/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import com.tangosol.util.function.Remote;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

/**
 * Local implementation of {@link com.oracle.coherence.concurrent.atomic.AtomicInteger AtomicInteger}
 * interface, that simply wraps {@code java.util.concurrent.atomic.AtomicInteger} instance.
 *
 * @author Aleks Seovic  2020.12.03
 * @since 21.12
 */
public class LocalAtomicInteger
        extends Number
        implements com.oracle.coherence.concurrent.atomic.AtomicInteger
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct {@code LocalAtomicInteger} instance.
     *
     * @param value  initial value
     */
    LocalAtomicInteger(int value)
        {
        this(new AtomicInteger(value));
        }

    /**
     * Construct {@code LocalAtomicInteger} instance.
     *
     * @param value  wrapped value
     */
    LocalAtomicInteger(AtomicInteger value)
        {
        f_nValue = value;
        }

    // ----- AtomicInteger interface ----------------------------------------

    @Override
    public AsyncLocalAtomicInteger async()
        {
        return new AsyncLocalAtomicInteger(f_nValue);
        }

    @Override
    public int get()
        {
        return f_nValue.get();
        }

    @Override
    public void set(int nNewValue)
        {
        f_nValue.set(nNewValue);
        }

    @Override
    public int getAndSet(int nNewValue)
        {
        return f_nValue.getAndSet(nNewValue);
        }

    @Override
    public boolean compareAndSet(int nExpectedValue, int nNewValue)
        {
        return f_nValue.compareAndSet(nExpectedValue, nNewValue);
        }

    @Override
    public int getAndIncrement()
        {
        return f_nValue.getAndIncrement();
        }

    @Override
    public int getAndDecrement()
        {
        return f_nValue.getAndDecrement();
        }

    @Override
    public int getAndAdd(int nDelta)
        {
        return f_nValue.getAndAdd(nDelta);
        }

    @Override
    public int incrementAndGet()
        {
        return f_nValue.incrementAndGet();
        }

    @Override
    public int decrementAndGet()
        {
        return f_nValue.decrementAndGet();
        }

    @Override
    public int addAndGet(int nDelta)
        {
        return f_nValue.addAndGet(nDelta);
        }

    @Override
    public int getAndUpdate(Remote.IntUnaryOperator updateFunction)
        {
        return getAndUpdate((IntUnaryOperator) updateFunction);
        }

    @Override
    public int getAndUpdate(IntUnaryOperator updateFunction)
        {
        return f_nValue.getAndUpdate(updateFunction);
        }

    @Override
    public int updateAndGet(Remote.IntUnaryOperator updateFunction)
        {
        return updateAndGet((IntUnaryOperator) updateFunction);
        }

    @Override
    public int updateAndGet(IntUnaryOperator updateFunction)
        {
        return f_nValue.updateAndGet(updateFunction);
        }

    @Override
    public int getAndAccumulate(int nUpdate, Remote.IntBinaryOperator accumulatorFunction)
        {
        return getAndAccumulate(nUpdate, (IntBinaryOperator) accumulatorFunction);
        }

    @Override
    public int getAndAccumulate(int nUpdate, IntBinaryOperator accumulatorFunction)
        {
        return f_nValue.getAndAccumulate(nUpdate, accumulatorFunction);
        }

    @Override
    public int accumulateAndGet(int nUpdate, Remote.IntBinaryOperator accumulatorFunction)
        {
        return accumulateAndGet(nUpdate, (IntBinaryOperator) accumulatorFunction);
        }

    @Override
    public int accumulateAndGet(int nUpdate, IntBinaryOperator accumulatorFunction)
        {
        return f_nValue.accumulateAndGet(nUpdate, accumulatorFunction);
        }

    @Override
    public int compareAndExchange(int nExpectedValue, int nNewValue)
        {
        return f_nValue.compareAndExchange(nExpectedValue, nNewValue);
        }

    @Override
    public int intValue()
        {
        return f_nValue.intValue();
        }

    @Override
    public long longValue()
        {
        return f_nValue.longValue();
        }

    @Override
    public float floatValue()
        {
        return f_nValue.floatValue();
        }

    @Override
    public double doubleValue()
        {
        return f_nValue.doubleValue();
        }

    @Override
    public byte byteValue()
        {
        return f_nValue.byteValue();
        }

    @Override
    public short shortValue()
        {
        return f_nValue.shortValue();
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return f_nValue.toString();
        }

    // ----- data members ---------------------------------------------------

    /**
     * Wrapped atomic int value.
     */
    private final AtomicInteger f_nValue;
    }
