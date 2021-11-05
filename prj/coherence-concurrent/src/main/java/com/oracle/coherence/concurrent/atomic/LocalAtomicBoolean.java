/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Local implementation of {@link com.oracle.coherence.concurrent.atomic.AtomicBoolean AtomicBoolean}
 * interface, that simply wraps {@code java.util.concurrent.atomic.AtomicBoolean} instance.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.12
 */
public class LocalAtomicBoolean
        implements com.oracle.coherence.concurrent.atomic.AtomicBoolean
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct {@code LocalAtomicBoolean} instance.
     *
     * @param fValue  initial value
     */
    protected LocalAtomicBoolean(boolean fValue)
        {
        this(new AtomicBoolean(fValue));
        }

    /**
     * Construct {@code LocalAtomicBoolean} instance.
     *
     * @param fValue  wrapped value
     */
    protected LocalAtomicBoolean(AtomicBoolean fValue)
        {
        f_fValue = fValue;
        }

    // ----- AtomicBoolean interface ----------------------------------------

    @Override
    public AsyncLocalAtomicBoolean async()
        {
        return new AsyncLocalAtomicBoolean(f_fValue);
        }

    @Override
    public boolean get()
        {
        return f_fValue.get();
        }

    @Override
    public void set(boolean fNewValue)
        {
        f_fValue.set(fNewValue);
        }

    @Override
    public boolean getAndSet(boolean fNewValue)
        {
        return f_fValue.getAndSet(fNewValue);
        }

    @Override
    public boolean compareAndSet(boolean fExpectedValue, boolean fNewValue)
        {
        return f_fValue.compareAndSet(fExpectedValue, fNewValue);
        }

    @Override
    public boolean compareAndExchange(boolean fExpectedValue, boolean fNewValue)
        {
        return f_fValue.compareAndExchange(fExpectedValue, fNewValue);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return f_fValue.toString();
        }

    // ----- data members ---------------------------------------------------

    /**
     * Wrapped atomic boolean value.
     */
    private final AtomicBoolean f_fValue;
    }
