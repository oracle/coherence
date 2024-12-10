/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicMarkableReference;
import com.oracle.coherence.concurrent.atomic.AtomicMarkableReference;

/**
 * Base class for {@link AsyncAtomicMarkableReference} tests.
 *
 * @author Aleks Seovic  2020.12.09
 * @since 21.12
 */
public abstract class AsyncAtomicMarkableReferenceTest
        extends AtomicMarkableReferenceTest
    {
    // ----- abstract methods -----------------------------------------------

    protected abstract AsyncAtomicMarkableReference<String> asyncValue();

    // ----- AtomicMarkableReferenceTest methods ----------------------------

    @Override
    protected AtomicMarkableReference<String> value()
        {
        return new SyncAtomicMarkableReference<>(asyncValue());
        }

    // ---- inner class: SyncAtomicMarkableReference ------------------------

    /**
     * Synchronizes access to {@link AsyncAtomicMarkableReference} for easier testing.
     *
     * @param <V>  the type of object referred to by this reference
     */
    static class SyncAtomicMarkableReference<V>
            implements AtomicMarkableReference<V>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a new {@code SyncAtomicMarkableReference}.
         *
         * @param value  the {@link AsyncAtomicMarkableReference} to delegate calls to
         */
        public SyncAtomicMarkableReference(AsyncAtomicMarkableReference<V> value)
            {
            this.f_value = value;
            }

        // ----- AsyncAtomicMarkableReference interface ---------------------

        @Override
        public AsyncAtomicMarkableReference<V> async()
            {
            return f_value;
            }

        @Override
        public V getReference()
            {
            return f_value.getReference().join();
            }

        @Override
        public boolean isMarked()
            {
            return f_value.isMarked().join();
            }

        @Override
        public V get(boolean[] abMarkHolder)
            {
            return f_value.get(abMarkHolder).join();
            }

        @Override
        public boolean compareAndSet(V expectedReference, V newReference, boolean fExpectedMark, boolean fNewMark)
            {
            return f_value.compareAndSet(expectedReference, newReference, fExpectedMark, fNewMark).join();
            }

        @Override
        public void set(V newReference, boolean fNewMark)
            {
            f_value.set(newReference, fNewMark).join();
            }

        @Override
        public boolean attemptMark(V expectedReference, boolean fNewMark)
            {
            return f_value.attemptMark(expectedReference, fNewMark).join();
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public String toString()
            {
            return f_value.toString();
            }

        // ----- data members -----------------------------------------------

        /**
         * Wrapped {@link AsyncAtomicMarkableReference}.
         */
        protected final AsyncAtomicMarkableReference<V> f_value;
        }
    }
