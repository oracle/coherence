/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import java.util.concurrent.atomic.AtomicMarkableReference;

/**
 * Local implementation of {@link com.oracle.coherence.concurrent.atomic.AtomicMarkableReference
 * AtomicMarkableReference} interface, that simply wraps {@code java.util.concurrent.atomic.AtomicMarkableReference}
 * instance.
 *
 * @param <V>  the type of object referred to by this reference
 *
 * @author Aleks Seovic  2020.12.09
 * @since 21.12
 */
public class LocalAtomicMarkableReference<V>
        implements com.oracle.coherence.concurrent.atomic.AtomicMarkableReference<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct {@code LocalAtomicMarkableReference<V>} instance.
     *
     * @param value        initial value
     * @param initialMark  initial mark
     */
    protected LocalAtomicMarkableReference(V value, boolean initialMark)
        {
        this(new AtomicMarkableReference<>(value, initialMark));
        }

    /**
     * Construct {@code LocalAtomicMarkableReference<V>} instance.
     *
     * @param value  wrapped value
     */
    protected LocalAtomicMarkableReference(AtomicMarkableReference<V> value)
        {
        f_value = value;
        }

    // ----- AtomicMarkableReference interface ------------------------------

    @Override
    public AsyncLocalAtomicMarkableReference<V> async()
        {
        return new AsyncLocalAtomicMarkableReference<>(f_value);
        }

    @Override
    public V getReference()
        {
        return f_value.getReference();
        }

    @Override
    public boolean isMarked()
        {
        return f_value.isMarked();
        }

    @Override
    public V get(boolean[] abMarkHolder)
        {
        return f_value.get(abMarkHolder);
        }

    @Override
    public boolean compareAndSet(V expectedReference, V newReference, boolean fExpectedMark, boolean fNewMark)
        {
        return f_value.compareAndSet(expectedReference, newReference, fExpectedMark, fNewMark);
        }

    @Override
    public void set(V newReference, boolean fNewMark)
        {
        f_value.set(newReference, fNewMark);
        }

    @Override
    public boolean attemptMark(V expectedReference, boolean fNewMark)
        {
        return f_value.attemptMark(expectedReference, fNewMark);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        boolean[] abMark = new boolean[1];
        V         value  = f_value.get(abMark);

        return value + " (" + abMark[0] + ")";
        }

    // ----- data members ---------------------------------------------------

    /**
     * Wrapped atomic value.
     */
    private final AtomicMarkableReference<V> f_value;
    }
