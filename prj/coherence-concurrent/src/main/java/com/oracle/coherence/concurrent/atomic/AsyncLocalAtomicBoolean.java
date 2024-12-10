/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import java.util.concurrent.CompletableFuture;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Local implementation of {@link AsyncAtomicBoolean} interface,
 * that simply wraps {@code java.util.concurrent.atomic.AtomicBoolean}
 * instance and returns an already completed future from each method.
 *
 * @author Aleks Seovic  2020.12.07
 */
public class AsyncLocalAtomicBoolean
        implements AsyncAtomicBoolean
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct {@code LocalAtomicBoolean} instance.
     *
     * @param value  wrapped value
     */
    protected AsyncLocalAtomicBoolean(AtomicBoolean value)
        {
        f_fValue = value;
        }

    // ----- AsyncAtomicBoolean interface  ----------------------------------

    @Override
    public CompletableFuture<Boolean> get()
        {
        return completedFuture(f_fValue.get());
        }

    @Override
    public CompletableFuture<Void> set(boolean fNewValue)
        {
        f_fValue.set(fNewValue);
        return completedFuture(null);
        }

    @Override
    public CompletableFuture<Boolean> getAndSet(boolean fNewValue)
        {
        return completedFuture(f_fValue.getAndSet(fNewValue));
        }

    @Override
    public CompletableFuture<Boolean> compareAndSet(boolean fExpectedValue, boolean fNewValue)
        {
        return completedFuture(f_fValue.compareAndSet(fExpectedValue, fNewValue));
        }

    @Override
    public CompletableFuture<Boolean> compareAndExchange(boolean fExpectedValue, boolean fNewValue)
        {
        return completedFuture(f_fValue.compareAndExchange(fExpectedValue, fNewValue));
        }

    // ----- Object methods -------------------------------------------------

    /**
     * Returns the String representation of the current value.
     *
     * @return the String representation of the current value
     */
    @Override
    public String toString()
        {
        return Boolean.toString(get().join());
        }

    // ----- data members ---------------------------------------------------

    /**
     * Wrapped atomic boolean value.
     */
    protected final AtomicBoolean f_fValue;
    }
