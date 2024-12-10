/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.common.base.Continuation;

/**
 * This extension allows composition of Continuations.
 *
 * @since 21.06
 */
public interface ComposableContinuation
        extends Continuation<Object>
    {
    /**
     * Composes this {@code ComposableContinuation} with the specified
     * {@code ComposableContinuation} to produce a {@code ComposableContinuation} that
     * replaces the need for this and the specified {@code ComposableContinuation}.
     *
     * @param continuation  the {@link ComposableContinuation} to compose
     *
     * @return the composed {@link ComposableContinuation}
     */
    ComposableContinuation compose(ComposableContinuation continuation);
    }
