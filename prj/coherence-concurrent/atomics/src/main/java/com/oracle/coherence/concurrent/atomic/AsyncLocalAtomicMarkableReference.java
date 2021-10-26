/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import java.util.concurrent.CompletableFuture;

import java.util.concurrent.atomic.AtomicMarkableReference;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Local implementation of {@link AsyncAtomicMarkableReference} interface, that
 * simply wraps {@code java.util.concurrent.atomic.AtomicMarkableReference} instance
 * and returns an already completed future from each method.
 *
 * @param <V>  the type of object referred to by this reference
 *
 * @author Aleks Seovic  2020.12.09
 * @since 21.12
 */
public class AsyncLocalAtomicMarkableReference<V>
        implements AsyncAtomicMarkableReference<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct {@code AsyncLocalAtomicMarkableReference} instance.
     *
     * @param value  wrapped value
     */
    protected AsyncLocalAtomicMarkableReference(AtomicMarkableReference<V> value)
        {
        f_value = value;
        }

    // ---- AsyncAtomicMarkableReference API --------------------------------

    @Override
    public CompletableFuture<V> getReference()
        {
        return completedFuture(f_value.getReference());
        }

    @Override
    public CompletableFuture<Boolean> isMarked()
        {
        return completedFuture(f_value.isMarked());
        }

    @Override
    public CompletableFuture<V> get(boolean[] abMarkHolder)
        {
        return completedFuture(f_value.get(abMarkHolder));
        }

    @Override
    public CompletableFuture<Boolean> compareAndSet(V expectedReference, V newReference,
                                                    boolean fExpectedMark, boolean fNewMark)
        {
        return completedFuture(f_value.compareAndSet(expectedReference, newReference, fExpectedMark, fNewMark));
        }

    @Override
    public CompletableFuture<Void> set(V newReference, boolean fNewMark)
        {
        f_value.set(newReference, fNewMark);
        return completedFuture(null);
        }

    @Override
    public CompletableFuture<Boolean> attemptMark(V expectedReference, boolean fNewMark)
        {
        return completedFuture(f_value.attemptMark(expectedReference, fNewMark));
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        boolean[] abMark = new boolean[1];
        V         value  = f_value.get(abMark);

        return value + " (" + abMark[0] + ')';
        }

    // ----- data members ---------------------------------------------------

    /**
     * Wrapped atomic value.
     */
    private final AtomicMarkableReference<V> f_value;
    }
