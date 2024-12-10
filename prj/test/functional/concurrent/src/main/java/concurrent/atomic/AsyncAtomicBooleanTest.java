/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicBoolean;
import com.oracle.coherence.concurrent.atomic.AtomicBoolean;

/**
 * Base class for {@link AsyncAtomicBoolean} tests.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.12
 */
public abstract class AsyncAtomicBooleanTest
        extends AtomicBooleanTest
    {
    // ----- abstract methods -----------------------------------------------

    protected abstract AsyncAtomicBoolean asyncValue();

    // ----- AtomicBooleanTest methods --------------------------------------

    @Override
    protected AtomicBoolean value()
        {
        return new SyncAtomicBoolean(asyncValue());
        }

    // ----- inner class: SyncAtomicBoolean ---------------------------------

    /**
     * Synchronizes access to {@link AsyncAtomicBoolean} for easier testing.
     */
    static class SyncAtomicBoolean
            implements AtomicBoolean
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a new {@code SyncAtomicBoolean}.
         *
         * @param value  the {@link AsyncAtomicBoolean} to delegate calls to
         */
        public SyncAtomicBoolean(AsyncAtomicBoolean value)
            {
            this.f_value = value;
            }

        // ----- AtomicBoolean interface ------------------------------------

        @Override
        public AsyncAtomicBoolean async()
            {
            return f_value;
            }

        @Override
        public boolean get()
            {
            return f_value.get().join();
            }

        @Override
        public void set(boolean fNewValue)
            {
            f_value.set(fNewValue).join();
            }

        @Override
        public boolean getAndSet(boolean fNewValue)
            {
            return f_value.getAndSet(fNewValue).join();
            }

        @Override
        public boolean compareAndSet(boolean fExpectedValue, boolean fNewValue)
            {
            return f_value.compareAndSet(fExpectedValue, fNewValue).join();
            }

        @Override
        public boolean compareAndExchange(boolean fExpectedValue, boolean fNewValue)
            {
            return f_value.compareAndExchange(fExpectedValue, fNewValue).join();
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public String toString()
            {
            return f_value.toString();
            }

        // ----- data members -----------------------------------------------

        /**
         * Wrapped {@link AsyncAtomicBoolean}.
         */
        protected final AsyncAtomicBoolean f_value;
        }
    }
