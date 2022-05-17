/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicStampedReference;
import com.oracle.coherence.concurrent.atomic.AtomicStampedReference;

/**
 * Base class for {@link AsyncAtomicStampedReference} tests.
 *
 * @author Aleks Seovic  2020.12.09
 */
public abstract class AsyncAtomicStampedReferenceTest
        extends AtomicStampedReferenceTest
    {
    // ----- abstract methods -----------------------------------------------

    protected abstract AsyncAtomicStampedReference<String> asyncValue();

    // ----- AtomicStampedReferenceTest methods -----------------------------
    @Override
    protected AtomicStampedReference<String> value()
        {
        return new SyncAtomicStampedReference<>(asyncValue());
        }

    // ---- inner class: SyncAtomicStampedReference ------------------------

    /**
     * Synchronizes access to {@link AsyncAtomicStampedReference} for easier testing.
     *
     * @param <V>  the type of object referred to by this reference
     */
    static class SyncAtomicStampedReference<V>
            implements AtomicStampedReference<V>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a new {@code SyncAtomicStampedReference}.
         *
         * @param value  the {@link AsyncAtomicStampedReference} to delegate calls to
         */
        public SyncAtomicStampedReference(AsyncAtomicStampedReference<V> value)
            {
            this.f_value = value;
            }

        // ----- AtomicStampedReference interface ---------------------------

        @Override
        public AsyncAtomicStampedReference<V> async()
            {
            return f_value;
            }

        @Override
        public V getReference()
            {
            return f_value.getReference().join();
            }

        @Override
        public int getStamp()
            {
            return f_value.getStamp().join();
            }

        @Override
        public V get(int[] iaStampHolder)
            {
            return f_value.get(iaStampHolder).join();
            }

        @Override
        public boolean compareAndSet(V expectedReference, V newReference, int nExpectedStamp, int nNewStamp)
            {
            return f_value.compareAndSet(expectedReference, newReference, nExpectedStamp, nNewStamp).join();
            }

        @Override
        public void set(V newReference, int nNewStamp)
            {
            f_value.set(newReference, nNewStamp).join();
            }

        @Override
        public boolean attemptStamp(V expectedReference, int nNewStamp)
            {
            return f_value.attemptStamp(expectedReference, nNewStamp).join();
            }

        // ----- Object methods ---------------------------------------------

        public String toString()
            {
            return f_value.toString();
            }

        // ----- data members -----------------------------------------------

        protected final AsyncAtomicStampedReference<V> f_value;
        }
    }
