/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import java.util.concurrent.CompletableFuture;

/**
 * An {@code AtomicMarkableReference} maintains an object reference
 * along with a mark bit, that can be updated atomically.
 * <p>
 * Unlike {@link AtomicMarkableReference}, each method from this interface is non-blocking,
 * which allows asynchronous invocation and consumption of the return value
 * via {@code CompletableFuture} API. This is particularly useful when using
 * {@link AsyncRemoteAtomicMarkableReference remote} implementation, because of relatively
 * high latency associated with am inevitable network call, but we do provide a
 * {@link AsyncLocalAtomicMarkableReference local} implementation as well.
 *
 * @param <V>  the type of object referred to by this reference
 *
 * @author Aleks Seovic  2020.12.09
 * @since 21.12
 */
public interface AsyncAtomicMarkableReference<V>
    {
    /**
     * Returns the current value of the reference.
     *
     * @return the current value of the reference
     */
    CompletableFuture<V> getReference();

    /**
     * Returns the current value of the mark.
     *
     * @return the current value of the mark
     */
    CompletableFuture<Boolean> isMarked();

    /**
     * Returns the current values of both the reference and the mark.
     * Typical usage is {@code boolean[1] holder; ref = v.get(holder); }.
     *
     * @param abMarkHolder an array of size of at least one. On return,
     *                     {@code markHolder[0]} will hold the value of the mark
     *
     * @return the current value of the reference
     */
    CompletableFuture<V> get(boolean[] abMarkHolder);

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
    CompletableFuture<Boolean> compareAndSet(V expectedReference, V newReference,
            boolean fExpectedMark, boolean fNewMark);

    /**
     * Unconditionally sets the value of both the reference and mark.
     *
     * @param newReference  the new value for the reference
     * @param fNewMark      the new value for the mark
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     */
    CompletableFuture<Void> set(V newReference, boolean fNewMark);

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
    CompletableFuture<Boolean> attemptMark(V expectedReference, boolean fNewMark);
    }
