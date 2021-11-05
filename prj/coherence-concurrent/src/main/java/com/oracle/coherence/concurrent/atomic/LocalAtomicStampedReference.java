/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * Local implementation of {@link com.oracle.coherence.concurrent.atomic.AtomicStampedReference AtomicStampedReference}
 * interface, that simply wraps {@code java.util.concurrent.atomic.AtomicStampedReference} instance.
 *
 * @param <V>  the type of object referred to by this reference
 *
 * @author Aleks Seovic  2020.12.09
 * @since 21.06
 */
public class LocalAtomicStampedReference<V>
        implements
        com.oracle.coherence.concurrent.atomic.AtomicStampedReference<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct {@code LocalAtomicStampedReference<V>} instance.
     *
     * @param value          initial value
     * @param nInitialStamp  initial stamp
     */
    protected LocalAtomicStampedReference(V value, int nInitialStamp)
        {
        this(new AtomicStampedReference<>(value, nInitialStamp));
        }

    /**
     * Construct {@code LocalAtomicStampedReference<V>} instance.
     *
     * @param value  wrapped value
     */
    protected LocalAtomicStampedReference(AtomicStampedReference<V> value)
        {
        f_value = value;
        }

    // ----- AtomicStampedReference interface -------------------------------

    @Override
    public AsyncLocalAtomicStampedReference<V> async()
        {
        return new AsyncLocalAtomicStampedReference<>(f_value);
        }

    @Override
    public V getReference()
        {
        return f_value.getReference();
        }

    @Override
    public int getStamp()
        {
        return f_value.getStamp();
        }

    @Override
    public V get(int[] iaStampHolder)
        {
        return f_value.get(iaStampHolder);
        }

    @Override
    public boolean compareAndSet(V expectedReference, V newReference, int nExpectedStamp, int nNewStamp)
        {
        return f_value.compareAndSet(expectedReference, newReference, nExpectedStamp, nNewStamp);
        }

    @Override
    public void set(V newReference, int nNewStamp)
        {
        f_value.set(newReference, nNewStamp);
        }

    @Override
    public boolean attemptStamp(V expectedReference, int nNewStamp)
        {
        return f_value.attemptStamp(expectedReference, nNewStamp);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        int[] aiStamp = new int[1];
        V     value   = f_value.get(aiStamp);

        return value + " (" + aiStamp[0] + ")";
        }

    // ----- data members ---------------------------------------------------

    /**
     * Wrapped atomic value.
     */
    private final AtomicStampedReference<V> f_value;
    }
