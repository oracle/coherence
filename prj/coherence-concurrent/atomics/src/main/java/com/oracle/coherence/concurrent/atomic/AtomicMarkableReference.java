/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;

/**
 * An {@code AtomicMarkableReference} maintains an object reference
 * along with a mark bit, that can be updated atomically.
 *
 * @param <V>  the type of object referred to by this reference
 *
 * @author Aleks Seovic  2020.12.09
 * @since 21.12
 */
public interface AtomicMarkableReference<V>
    {
    /**
     * Return non-blocking API for this atomic reference.
     *
     * @return non-blocking API for this atomic reference
     */
    AsyncAtomicMarkableReference<V> async();

    /**
     * Returns the current value of the reference.
     *
     * @return the current value of the reference
     */
    V getReference();

    /**
     * Returns the current value of the mark.
     *
     * @return the current value of the mark
     */
    boolean isMarked();

    /**
     * Returns the current values of both the reference and the mark.
     * Typical usage is {@code boolean[1] holder; ref = v.get(holder); }.
     *
     * @param abMarkHolder an array of size of at least one. On return,
     *                     {@code markHolder[0]} will hold the value of the mark.
     *
     * @return the current value of the reference
     */
    V get(boolean[] abMarkHolder);

    /**
     * Atomically sets the value of both the reference and mark
     * to the given update values if the
     * current reference is equal to the expected reference
     * and the current mark is equal to the expected mark.
     *
     * @param expectedReference  the expected value of the reference
     * @param newReference       the new value for the reference
     * @param fExpectedMark      the expected value of the mark
     * @param fNewMark           the new value for the mark
     *
     * @return {@code true} if successful
     */
    boolean compareAndSet(V expectedReference, V newReference, boolean fExpectedMark, boolean fNewMark);

    /**
     * Unconditionally sets the value of both the reference and mark.
     *
     * @param newReference  the new value for the reference
     * @param fNewMark      the new value for the mark
     */
    void set(V newReference, boolean fNewMark);

    /**
     * Atomically sets the value of the mark to the given update value
     * if the current reference is equal to the expected
     * reference.  Any given invocation of this operation may fail
     * (return {@code false}) spuriously, but repeated invocation
     * when the current value holds the expected value and no other
     * thread is also attempting to set the value will eventually
     * succeed.
     *
     * @param expectedReference  the expected value of the reference
     * @param fNewMark           the new value for the mark
     *
     * @return {@code true} if successful
     */
    boolean attemptMark(V expectedReference, boolean fNewMark);

    // ----- inner class: Serializer ----------------------------------------

    /**
     * POF serializer implementation.
     *
     * @param <V>  the type of object referred to by this reference
     */
    class Serializer<V>
            implements PofSerializer<java.util.concurrent.atomic.AtomicMarkableReference<V>>
        {
        // ----- PofSerializer interface ------------------------------------

        @Override
        public void serialize(PofWriter out, java.util.concurrent.atomic.AtomicMarkableReference<V> value)
                throws IOException
            {
            boolean[] aMark = new boolean[1];

            out.writeObject(0, value.get(aMark));
            out.writeBoolean(1, aMark[0]);
            out.writeRemainder(null);
            }

        @Override
        public java.util.concurrent.atomic.AtomicMarkableReference<V> deserialize(PofReader in)
                throws IOException
            {
            V       value = in.readObject(0);
            boolean fMark = in.readBoolean(1);

            in.readRemainder();
            return new java.util.concurrent.atomic.AtomicMarkableReference<>(value, fMark);
            }
        }
    }
