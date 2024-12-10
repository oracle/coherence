/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import java.util.concurrent.CompletableFuture;

/**
 * A {@code boolean} value that may be updated atomically.
 * <p>
 * Unlike {@link AtomicBoolean}, each method from this interface is non-blocking,
 * which allows asynchronous invocation and consumption of the return value
 * via {@code CompletableFuture} API. This is particularly useful when using
 * {@link AsyncRemoteAtomicBoolean remote} implementation, because of relatively
 * high latency associated with an inevitable network call, but we do provide a
 * {@link AsyncLocalAtomicBoolean local} implementation as well.
 * <p>
 * An {@code AsyncAtomicBoolean} is used in applications such as atomically updated
 * flags, and cannot be used as a replacement for a {@link java.lang.Boolean}.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.12
 */
public interface AsyncAtomicBoolean
    {
    /**
     * Returns the current value.
     *
     * @return the current value
     */
    CompletableFuture<Boolean> get();

    /**
     * Sets the value to {@code newValue}.
     *
     * @param fNewValue  the new value
     *
     * @return a {@link CompletableFuture} that can be used to determine whether
     *         the operation completed
     */
    CompletableFuture<Void> set(boolean fNewValue);

    /**
     * Atomically sets the value to {@code newValue} and returns the old value.
     *
     * @param fNewValue  the new value
     *
     * @return the previous value
     */
    CompletableFuture<Boolean> getAndSet(boolean fNewValue);

    /**
     * Atomically sets the value to {@code newValue}
     * if the current value {@code == expectedValue}.
     *
     * @param fExpectedValue  the expected value
     * @param fNewValue       the new value
     *
     * @return {@code true} if successful. False return indicates that
     *         the actual value was not equal to the expected value.
     */
    CompletableFuture<Boolean> compareAndSet(boolean fExpectedValue, boolean fNewValue);

    /**
     * Atomically sets the value to {@code newValue} if the current value,
     * referred to as the <em>witness value</em>, {@code == expectedValue}.
     *
     * @param fExpectedValue  the expected value
     * @param fNewValue       the new value
     *
     * @return the witness value, which will be the same as the expected value
     *         if successful
     */
    CompletableFuture<Boolean> compareAndExchange(boolean fExpectedValue, boolean fNewValue);
    }
