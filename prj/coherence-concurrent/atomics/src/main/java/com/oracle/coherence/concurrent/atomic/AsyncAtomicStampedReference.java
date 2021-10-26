/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import java.util.concurrent.CompletableFuture;

/**
 * An {@code AtomicStampedReference} maintains an object reference
 * along with an integer "stamp", that can be updated atomically.
 * <p>
 * Unlike {@link AtomicStampedReference}, each method from this interface is non-blocking,
 * which allows asynchronous invocation and consumption of the return value
 * via {@code CompletableFuture} API. This is particularly useful when using
 * {@link AsyncRemoteAtomicStampedReference remote} implementation, because of relatively
 * high latency associated with am inevitable network call, but we do provide a
 * {@link AsyncLocalAtomicStampedReference local} implementation as well.
 *
 * @param <V>  the type of object referred to by this reference
 *
 * @author Aleks Seovic  2020.12.09
 * @since 21.12
 */
public interface AsyncAtomicStampedReference<V>
    {
    /**
     * Returns the current value of the reference.
     *
     * @return the current value of the reference
     */
    CompletableFuture<V> getReference();

    /**
     * Returns the current value of the stamp.
     *
     * @return the current value of the stamp
     */
    CompletableFuture<Integer> getStamp();

    /**
     * Returns the current values of both the reference and the stamp.
     * Typical usage is {@code int[1] holder; ref = v.get(holder); }.
     *
     * @param iaStampHolder  an array of size of at least one.  On return,
     *                       {@code stampHolder[0]} will hold the value of the stamp
     *
     * @return the current value of the reference
     */
    CompletableFuture<V> get(int[] iaStampHolder);

    /**
     * Atomically sets the value of both the reference and stamp
     * to the given update values if the current reference is equal
     * to the expected reference and the current stamp is equal to
     * the expected stamp.
     *
     * @param expectedReference  the expected value of the reference
     * @param newReference       the new value for the reference
     * @param nExpectedStamp     the expected value of the stamp
     * @param nNewStamp          the new value for the stamp
     *
     * @return {@code true} if successful
     */
    CompletableFuture<Boolean> compareAndSet(V expectedReference, V newReference, int nExpectedStamp, int nNewStamp);

    /**
     * Unconditionally sets the value of both the reference and stamp.
     *
     * @param newReference  the new value for the reference
     * @param nNewStamp     the new value for the stamp
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     */
    CompletableFuture<Void> set(V newReference, int nNewStamp);

    /**
     * Atomically sets the value of the stamp to the given update value
     * if the current reference is equal to the expected
     * reference.  Any given invocation of this operation may fail
     * (return {@code false}) spuriously, but repeated invocation
     * when the current value holds the expected value and no other
     * thread is also attempting to set the value will eventually
     * succeed.
     *
     * @param expectedReference  the expected value of the reference
     * @param nNewStamp          the new value for the stamp
     *
     * @return {@code true} if successful
     */
    CompletableFuture<Boolean> attemptStamp(V expectedReference, int nNewStamp);
    }
