/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.io;


import com.oracle.coherence.common.io.BufferManager;
import com.tangosol.io.MultiBufferWriteBuffer;
import com.tangosol.io.WriteBuffer;
import com.tangosol.io.MultiBufferWriteBuffer.WriteBufferPool;
import com.tangosol.io.nio.ByteBufferWriteBuffer;


/**
 * A {@link WriteBufferPool} which delegates to a {@link BufferManager}.
 *
 * @author coh 2011.12.09
 * 
 * @since Coherence 12.1.2
 */
public class BufferManagerWriteBufferPool
        implements MultiBufferWriteBuffer.WriteBufferPool
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a new instance wrapping the supplied {@link BufferManager manager}
     *
     * @param mgr the {@link BufferManager manager} to adapt as a {@link WriteBufferPool}
     */
    public BufferManagerWriteBufferPool(BufferManager mgr)
        {
        m_mgr = mgr;
        }

    // ----- WriteBufferPool implementation ---------------------------------

    /**
     * @{inheritDoc}
     */
    @Override
    public WriteBuffer allocate(int cbPreviousTotal)
        {
        return new ByteBufferWriteBuffer(m_mgr.acquireSum(cbPreviousTotal));
        }

    /**
     * @{inheritDoc}
     */
    @Override
    public int getMaximumCapacity()
        {
        return Integer.MAX_VALUE;
        }

    /**
     * @{inheritDoc}
     */
    @Override
    public void release(WriteBuffer buffer)
        {
        m_mgr.release(((ByteBufferWriteBuffer) buffer).getByteBuffer());
        }

    // ----- data members ---------------------------------------------------

    /**
     * The BufferManager to adapt.
     */
    private final BufferManager m_mgr;
    }
