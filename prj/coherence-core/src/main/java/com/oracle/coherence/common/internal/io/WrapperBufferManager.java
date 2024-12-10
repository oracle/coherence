/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.io;

import com.oracle.coherence.common.io.BufferManager;

import java.nio.ByteBuffer;

/**
 * WrapperBufferManager is a BufferManager wrapper.
 *
 * @author coh 2011.12.08
 */
public abstract class WrapperBufferManager
        implements BufferManager
    {
    // ----- constructors---------------------------------------------

    /**
     * Create a new WrapperBufferManager.
     *
     * @param delegate  the BufferManager to delegate to
     */
    public WrapperBufferManager(BufferManager delegate)
        {
        f_delegate = delegate;
        }

    // ----- Object interface ---------------------------------

    @Override
    public String toString()
        {
        return String.format("%s(delegate=%s)", getClass().getSimpleName(), f_delegate);
        }

    // ----- BufferManager interface ---------------------------------

    @Override
    public void dispose()
        {
        f_delegate.dispose();
        }

    @Override
    public ByteBuffer acquire(int cbMin)
        {
        return f_delegate.acquire(cbMin);
        }

    @Override
    public ByteBuffer acquirePref(int cbPref)
        {
        return f_delegate.acquirePref(cbPref);
        }

    @Override
    public ByteBuffer acquireSum(int cbSum)
        {
        return f_delegate.acquireSum(cbSum);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer truncate(ByteBuffer buff)
        {
        return f_delegate.truncate(buff);
        }

    @Override
    public void release(ByteBuffer buff)
        {
        f_delegate.release(buff);
        }

    @Override
    public long getCapacity()
        {
        return f_delegate.getCapacity();
        }

    // -----  data members --------------------------------------------------

    /**
     * The BufferManager to delegate to.
     */
    protected final BufferManager f_delegate;
    }