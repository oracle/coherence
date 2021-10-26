/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import java.util.concurrent.CompletableFuture;

import java.util.concurrent.atomic.AtomicStampedReference;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Local implementation of {@link AsyncAtomicStampedReference} interface, that
 * simply wraps {@code java.util.concurrent.atomic.AtomicStampedReference} instance
 * and returns an already completed future from each method.
 *
 * @param <V> the type of object referred to by this reference
 *
 * @author Aleks Seovic  2020.12.09
 */
public class AsyncLocalAtomicStampedReference<V>
        implements AsyncAtomicStampedReference<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct {@code AsyncLocalAtomicStampedReference} instance.
     *
     * @param value  wrapped value
     */
    protected AsyncLocalAtomicStampedReference(AtomicStampedReference<V> value)
        {
        f_value = value;
        }

    // ----- AsyncAtomicStampedReference interface --------------------------

    @Override
    public CompletableFuture<V> getReference()
        {
        return completedFuture(f_value.getReference());
        }

    @Override
    public CompletableFuture<Integer> getStamp()
        {
        return completedFuture(f_value.getStamp());
        }

    @Override
    public CompletableFuture<V> get(int[] iaStampHolder)
        {
        return completedFuture(f_value.get(iaStampHolder));
        }

    @Override
    public CompletableFuture<Boolean> compareAndSet(V expectedReference, V newReference,
                                                    int nExpectedStamp, int newStamp)
        {
        return completedFuture(f_value.compareAndSet(expectedReference, newReference, nExpectedStamp, newStamp));
        }

    @Override
    public CompletableFuture<Void> set(V newReference, int nNewStamp)
        {
        f_value.set(newReference, nNewStamp);
        return completedFuture(null);
        }

    @Override
    public CompletableFuture<Boolean> attemptStamp(V expectedReference, int nNewStamp)
        {
        return completedFuture(f_value.attemptStamp(expectedReference, nNewStamp));
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        int[] aiStamp = new int[1];
        V     value   = f_value.get(aiStamp);

        return value + " (" + aiStamp[0] + ')';
        }

    // ----- data members ---------------------------------------------------

    /**
     * Wrapped atomic value.
     */
    private final AtomicStampedReference<V> f_value;
    }
