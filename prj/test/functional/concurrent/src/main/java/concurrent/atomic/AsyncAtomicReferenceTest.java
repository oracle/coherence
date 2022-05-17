/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicReference;
import com.oracle.coherence.concurrent.atomic.AtomicReference;
import com.tangosol.util.function.Remote;

import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

/**
 * Base class for {@link AsyncAtomicReference} tests.
 *
 * @author Aleks Seovic  2020.12.08
 * @since 21.12
 */
public abstract class AsyncAtomicReferenceTest
        extends AtomicReferenceTest
    {
    // ----- abstract methods -----------------------------------------------

    protected abstract AsyncAtomicReference<String> asyncValue();

    // ----- AtomicReferenceTest methods ------------------------------------

    @Override
    protected AtomicReference<String> value()
        {
        return new SyncAtomicReference<>(asyncValue());
        }

    // ---- inner class: SyncAtomicReference --------------------------------

    /**
     * Synchronizes access to {@link AsyncAtomicReference} for easier testing.
     *
     * @param <V>  the type of object referred to by this reference
     */
    static class SyncAtomicReference<V>
            implements AtomicReference<V>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a new {@code SyncAtomicReference}.
         *
         * @param value  the {@link AsyncAtomicReference} to delegate calls to
         */
        public SyncAtomicReference(AsyncAtomicReference<V> value)
            {
            this.f_value = value;
            }

        // ----- AtomicReference interface ----------------------------------

        @Override
        public AsyncAtomicReference<V> async()
            {
            return f_value;
            }

        @Override
        public V get()
            {
            return f_value.get().join();
            }

        @Override
        public void set(V newValue)
            {
            f_value.set(newValue).join();
            }

        @Override
        public V getAndSet(V newValue)
            {
            return f_value.getAndSet(newValue).join();
            }

        @Override
        public boolean compareAndSet(V expectedValue, V newValue)
            {
            return f_value.compareAndSet(expectedValue, newValue).join();
            }

        @Override
        public V getAndUpdate(Remote.UnaryOperator<V> updateFunction)
            {
            return f_value.getAndUpdate(updateFunction).join();
            }

        @Override
        public V getAndUpdate(UnaryOperator<V> updateFunction)
            {
            return f_value.getAndUpdate(updateFunction).join();
            }

        @Override
        public V updateAndGet(Remote.UnaryOperator<V> updateFunction)
            {
            return f_value.updateAndGet(updateFunction).join();
            }

        @Override
        public V updateAndGet(UnaryOperator<V> updateFunction)
            {
            return f_value.updateAndGet(updateFunction).join();
            }

        @Override
        public V getAndAccumulate(V x, Remote.BinaryOperator<V> accumulatorFunction)
            {
            return f_value.getAndAccumulate(x, accumulatorFunction).join();
            }

        @Override
        public V getAndAccumulate(V x, BinaryOperator<V> accumulatorFunction)
            {
            return f_value.getAndAccumulate(x, accumulatorFunction).join();
            }

        @Override
        public V accumulateAndGet(V x, Remote.BinaryOperator<V> accumulatorFunction)
            {
            return f_value.accumulateAndGet(x, accumulatorFunction).join();
            }

        @Override
        public V accumulateAndGet(V x, BinaryOperator<V> accumulatorFunction)
            {
            return f_value.accumulateAndGet(x, accumulatorFunction).join();
            }

        @Override
        public V compareAndExchange(V expectedValue, V newValue)
            {
            return f_value.compareAndExchange(expectedValue, newValue).join();
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public String toString()
            {
            return f_value.toString();
            }

        // ----- data members -----------------------------------------------

        protected final AsyncAtomicReference<V> f_value;
        }
    }
