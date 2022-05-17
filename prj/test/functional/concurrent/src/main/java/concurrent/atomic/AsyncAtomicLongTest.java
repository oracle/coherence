/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicLong;
import com.oracle.coherence.concurrent.atomic.AtomicLong;
import com.tangosol.util.function.Remote;

import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

/**
 * Base class for {@link AsyncAtomicLong} tests.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.12
 */
public abstract class AsyncAtomicLongTest
        extends AtomicLongTest
    {
    // ----- abstract methods -----------------------------------------------

    protected abstract AsyncAtomicLong asyncValue();

    // ----- AtomicLongTest methods -----------------------------------------

    @Override
    protected AtomicLong value()
        {
        return new SyncAtomicLong(asyncValue());
        }

    // ---- inner class: SyncAtomicLong -------------------------------------

    /**
     * Synchronizes access to {@link AsyncAtomicLong} for easier testing.
     */
    static class SyncAtomicLong
            implements AtomicLong
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a new {@code SyncAtomicLong}.
         *
         * @param value  the {@link AsyncAtomicLong} to delegate calls to
         */
        public SyncAtomicLong(AsyncAtomicLong value)
            {
            this.f_value = value;
            }

        // ----- AtomicLong interface ---------------------------------------

        @Override
        public AsyncAtomicLong async()
            {
            return f_value;
            }

        @Override
        public long get()
            {
            return f_value.get().join();
            }

        @Override
        public void set(long lNewValue)
            {
            f_value.set(lNewValue).join();
            }

        @Override
        public long getAndSet(long lNewValue)
            {
            return f_value.getAndSet(lNewValue).join();
            }

        @Override
        public boolean compareAndSet(long lExpectedValue, long lNewValue)
            {
            return f_value.compareAndSet(lExpectedValue, lNewValue).join();
            }

        @Override
        public long getAndIncrement()
            {
            return f_value.getAndIncrement().join();
            }

        @Override
        public long getAndDecrement()
            {
            return f_value.getAndDecrement().join();
            }

        @Override
        public long getAndAdd(long lDelta)
            {
            return f_value.getAndAdd(lDelta).join();
            }

        @Override
        public long incrementAndGet()
            {
            return f_value.incrementAndGet().join();
            }

        @Override
        public long decrementAndGet()
            {
            return f_value.decrementAndGet().join();
            }

        @Override
        public long addAndGet(long lDelta)
            {
            return f_value.addAndGet(lDelta).join();
            }

        @Override
        public long getAndUpdate(Remote.LongUnaryOperator updateFunction)
            {
            return f_value.getAndUpdate(updateFunction).join();
            }

        @Override
        public long getAndUpdate(LongUnaryOperator updateFunction)
            {
            return f_value.getAndUpdate(updateFunction).join();
            }

        @Override
        public long updateAndGet(Remote.LongUnaryOperator updateFunction)
            {
            return f_value.updateAndGet(updateFunction).join();
            }

        @Override
        public long updateAndGet(LongUnaryOperator updateFunction)
            {
            return f_value.updateAndGet(updateFunction).join();
            }

        @Override
        public long getAndAccumulate(long lUpdate, Remote.LongBinaryOperator accumulatorFunction)
            {
            return f_value.getAndAccumulate(lUpdate, accumulatorFunction).join();
            }

        @Override
        public long getAndAccumulate(long lUpdate, LongBinaryOperator accumulatorFunction)
            {
            return f_value.getAndAccumulate(lUpdate, accumulatorFunction).join();
            }

        @Override
        public long accumulateAndGet(long lUpdate, Remote.LongBinaryOperator accumulatorFunction)
            {
            return f_value.accumulateAndGet(lUpdate, accumulatorFunction).join();
            }

        @Override
        public long accumulateAndGet(long lUpdate, LongBinaryOperator accumulatorFunction)
            {
            return f_value.accumulateAndGet(lUpdate, accumulatorFunction).join();
            }

        @Override
        public long compareAndExchange(long lExpectedValue, long lNewValue)
            {
            return f_value.compareAndExchange(lExpectedValue, lNewValue).join();
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

        protected final AsyncAtomicLong f_value;
        }
    }
