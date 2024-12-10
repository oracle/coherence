/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import com.tangosol.util.function.Remote;

import java.util.concurrent.atomic.AtomicLong;

import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

/**
 * Local implementation of {@link com.oracle.coherence.concurrent.atomic.AtomicLong AtomicLong}
 * interface, that simply wraps {@code java.util.concurrent.atomic.AtomicLong} instance.
 *
 * @author Aleks Seovic  2020.12.03
 * @since 21.12
 */
public class LocalAtomicLong
        extends Number
        implements com.oracle.coherence.concurrent.atomic.AtomicLong
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct {@code LocalAtomicLong} instance.
     *
     * @param lValue  initial value
     */
    LocalAtomicLong(long lValue)
        {
        this(new AtomicLong(lValue));
        }

    /**
     * Construct {@code LocalAtomicLong} instance.
     *
     * @param lValue  wrapped value
     */
    LocalAtomicLong(AtomicLong lValue)
        {
        f_lValue = lValue;
        }

    // ---- AtomicLong API --------------------------------------------------

    @Override
    public AsyncLocalAtomicLong async()
        {
        return new AsyncLocalAtomicLong(f_lValue);
        }

    @Override
    public long get()
        {
        return f_lValue.get();
        }

    @Override
    public void set(long lNewValue)
        {
        f_lValue.set(lNewValue);
        }

    @Override
    public long getAndSet(long lNewValue)
        {
        return f_lValue.getAndSet(lNewValue);
        }

    @Override
    public boolean compareAndSet(long lExpectedValue, long lNewValue)
        {
        return f_lValue.compareAndSet(lExpectedValue, lNewValue);
        }

    @Override
    public long getAndIncrement()
        {
        return f_lValue.getAndIncrement();
        }

    @Override
    public long getAndDecrement()
        {
        return f_lValue.getAndDecrement();
        }

    @Override
    public long getAndAdd(long lDelta)
        {
        return f_lValue.getAndAdd(lDelta);
        }

    @Override
    public long incrementAndGet()
        {
        return f_lValue.incrementAndGet();
        }

    @Override
    public long decrementAndGet()
        {
        return f_lValue.decrementAndGet();
        }

    @Override
    public long addAndGet(long lDelta)
        {
        return f_lValue.addAndGet(lDelta);
        }

    @Override
    public long getAndUpdate(Remote.LongUnaryOperator updateFunction)
        {
        return getAndUpdate((LongUnaryOperator) updateFunction);
        }

    @Override
    public long getAndUpdate(LongUnaryOperator updateFunction)
        {
        return f_lValue.getAndUpdate(updateFunction);
        }

    @Override
    public long updateAndGet(Remote.LongUnaryOperator updateFunction)
        {
        return updateAndGet((LongUnaryOperator) updateFunction);
        }

    @Override
    public long updateAndGet(LongUnaryOperator updateFunction)
        {
        return f_lValue.updateAndGet(updateFunction);
        }

    @Override
    public long getAndAccumulate(long lUpdate, Remote.LongBinaryOperator accumulatorFunction)
        {
        return getAndAccumulate(lUpdate, (LongBinaryOperator) accumulatorFunction);
        }

    @Override
    public long getAndAccumulate(long lUpdate, LongBinaryOperator accumulatorFunction)
        {
        return f_lValue.getAndAccumulate(lUpdate, accumulatorFunction);
        }

    @Override
    public long accumulateAndGet(long lUpdate, Remote.LongBinaryOperator accumulatorFunction)
        {
        return accumulateAndGet(lUpdate, (LongBinaryOperator) accumulatorFunction);
        }

    @Override
    public long accumulateAndGet(long lUpdate, LongBinaryOperator accumulatorFunction)
        {
        return f_lValue.accumulateAndGet(lUpdate, accumulatorFunction);
        }

    @Override
    public long compareAndExchange(long lExpectedValue, long lNewValue)
        {
        return f_lValue.compareAndExchange(lExpectedValue, lNewValue);
        }

    @Override
    public int intValue()
        {
        return f_lValue.intValue();
        }

    @Override
    public long longValue()
        {
        return f_lValue.longValue();
        }

    @Override
    public float floatValue()
        {
        return f_lValue.floatValue();
        }

    @Override
    public double doubleValue()
        {
        return f_lValue.doubleValue();
        }

    @Override
    public byte byteValue()
        {
        return f_lValue.byteValue();
        }

    @Override
    public short shortValue()
        {
        return f_lValue.shortValue();
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return f_lValue.toString();
        }

    // ----- data members ---------------------------------------------------

    /**
     * Wrapped atomic long value.
     */
    private final AtomicLong f_lValue;
    }
