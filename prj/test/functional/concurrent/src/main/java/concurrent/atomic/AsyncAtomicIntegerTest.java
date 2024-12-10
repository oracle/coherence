/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicInteger;
import com.oracle.coherence.concurrent.atomic.AtomicInteger;
import com.tangosol.util.function.Remote;

import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

/**
 * Base class for {@link AsyncAtomicInteger} tests.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.12
 */
public abstract class AsyncAtomicIntegerTest
        extends AtomicIntegerTest
    {
    // ----- abstract methods -----------------------------------------------

    protected abstract AsyncAtomicInteger asyncValue();

    // ----- AtomicIntegerTest methods --------------------------------------

    @Override
    protected AtomicInteger value()
        {
        return new SyncAtomicInteger(asyncValue());
        }

    // ---- inner class: SyncAtomicInteger ----------------------------------

    /**
     * Synchronizes access to {@link AsyncAtomicInteger} for easier testing.
     */
    static class SyncAtomicInteger
            implements AtomicInteger
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a new {@code SyncAtomicInteger}.
         *
         * @param value  the {@link AsyncAtomicInteger} to delegate calls to
         */
        public SyncAtomicInteger(AsyncAtomicInteger value)
            {
            this.f_value = value;
            }

        // ----- AtomicInteger interface ------------------------------------

        @Override
        public AsyncAtomicInteger async()
            {
            return f_value;
            }

        @Override
        public int get()
            {
            return f_value.get().join();
            }

        @Override
        public void set(int nNewValue)
            {
            f_value.set(nNewValue).join();
            }

        @Override
        public int getAndSet(int nNewValue)
            {
            return f_value.getAndSet(nNewValue).join();
            }

        @Override
        public boolean compareAndSet(int nExpectedValue, int nNewValue)
            {
            return f_value.compareAndSet(nExpectedValue, nNewValue).join();
            }

        @Override
        public int getAndIncrement()
            {
            return f_value.getAndIncrement().join();
            }

        @Override
        public int getAndDecrement()
            {
            return f_value.getAndDecrement().join();
            }

        @Override
        public int getAndAdd(int nDelta)
            {
            return f_value.getAndAdd(nDelta).join();
            }

        @Override
        public int incrementAndGet()
            {
            return f_value.incrementAndGet().join();
            }

        @Override
        public int decrementAndGet()
            {
            return f_value.decrementAndGet().join();
            }

        @Override
        public int addAndGet(int nDelta)
            {
            return f_value.addAndGet(nDelta).join();
            }

        @Override
        public int getAndUpdate(Remote.IntUnaryOperator updateFunction)
            {
            return f_value.getAndUpdate(updateFunction).join();
            }

        @Override
        public int getAndUpdate(IntUnaryOperator updateFunction)
            {
            return f_value.getAndUpdate(updateFunction).join();
            }

        @Override
        public int updateAndGet(Remote.IntUnaryOperator updateFunction)
            {
            return f_value.updateAndGet(updateFunction).join();
            }

        @Override
        public int updateAndGet(IntUnaryOperator updateFunction)
            {
            return f_value.updateAndGet(updateFunction).join();
            }

        @Override
        public int getAndAccumulate(int nUpdate, Remote.IntBinaryOperator accumulatorFunction)
            {
            return f_value.getAndAccumulate(nUpdate, accumulatorFunction).join();
            }

        @Override
        public int getAndAccumulate(int nUpdate, IntBinaryOperator accumulatorFunction)
            {
            return f_value.getAndAccumulate(nUpdate, accumulatorFunction).join();
            }

        @Override
        public int accumulateAndGet(int nUpdate, Remote.IntBinaryOperator accumulatorFunction)
            {
            return f_value.accumulateAndGet(nUpdate, accumulatorFunction).join();
            }

        @Override
        public int accumulateAndGet(int nUpdate, IntBinaryOperator accumulatorFunction)
            {
            return f_value.accumulateAndGet(nUpdate, accumulatorFunction).join();
            }

        @Override
        public int compareAndExchange(int nExpectedValue, int nNewValue)
            {
            return f_value.compareAndExchange(nExpectedValue, nNewValue).join();
            }

        @Override
        public int intValue()
            {
            return f_value.intValue().join();
            }

        @Override
        public long longValue()
            {
            return f_value.longValue().join();
            }

        @Override
        public float floatValue()
            {
            return f_value.floatValue().join();
            }

        @Override
        public double doubleValue()
            {
            return f_value.doubleValue().join();
            }

        @Override
        public byte byteValue()
            {
            return f_value.byteValue().join();
            }

        @Override
        public short shortValue()
            {
            return f_value.shortValue().join();
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public String toString()
            {
            return f_value.toString();
            }

        // ----- data members -----------------------------------------------

        /**
         * Wrapped {@link AsyncAtomicInteger}.
         */
        protected final AsyncAtomicInteger f_value;
        }
    }
