/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.io;


import com.oracle.coherence.common.io.BufferManager;
import com.oracle.coherence.common.io.BufferSequence;
import com.oracle.coherence.common.io.MultiBufferSequence;
import com.oracle.coherence.common.io.Buffers;
import com.oracle.coherence.common.io.SingleBufferSequence;

import com.tangosol.io.MultiBufferWriteBuffer;
import com.tangosol.io.WriteBuffer;
import com.tangosol.io.nio.ByteBufferWriteBuffer;

import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.List;


/**
* The BufferSequenceWriteBufferPool is a {@link MultiBufferWriteBuffer.WriteBufferPool
* WriteBufferPool} implementation that is backed by a {@link BufferManager} and is used
* to produce {@link BufferSequence}s.
* <p>
* This implementation tracks all ByteBuffer objects allocated by the
* underlying BufferManager in order to produce a BufferSequence.
*
* @author gg  2010.12.31
*/
public class BufferSequenceWriteBufferPool
        implements MultiBufferWriteBuffer.WriteBufferPool
    {
    /**
    * Construct BufferManagerWriteBufferPool based on the specified BufferManager.
    *
    * @param mgr  the underlying BufferManager
    */
    public BufferSequenceWriteBufferPool(BufferManager mgr)
        {
        m_mgr = mgr;
        }

    // ----- WriteBufferPool interface --------------------------------------

    /**
    * {@inheritDoc}
    */
    public int getMaximumCapacity()
        {
        return Integer.MAX_VALUE;
        }

    /**
    * {@inheritDoc}
    */
    public WriteBuffer allocate(int cbPreviousTotal)
        {
        ByteBuffer bufNew  = m_mgr.acquireSum(cbPreviousTotal);
        ByteBuffer bufLast = m_buffer;

        if (bufLast != null)
            {
            List<ByteBuffer> listBuffers = m_listBuffers;
            if (listBuffers == null)
                {
                listBuffers = m_listBuffers = new ArrayList<ByteBuffer>();
                }
            bufLast.flip();
            listBuffers.add(bufLast);
            }
        m_buffer = bufNew;

        return new ByteBufferWriteBuffer(bufNew);
        }

    /**
    * This method should not be called.
    */
    public void release(WriteBuffer buffer)
        {
        throw new IllegalStateException();
        }

    // ----- helpers --------------------------------------------------------

    /**
    * Create a BufferSequence based on the list of ByteBuffers allocated
    * by this adapter. Subsequent modifications to the buffers and buffer
    * positions are not allowed.
    *
    * @return the BufferSequence representing allocated buffers
    */
    public BufferSequence toBufferSequence()
        {
        List<ByteBuffer> listBuffers = m_listBuffers;
        BufferManager    manager     = m_mgr;
        ByteBuffer       bufLast     = m_buffer;

        if (listBuffers == null)
            {
            // never flushed; avoid creating an unnecessary List
            if (bufLast == null || bufLast.position() == 0)
                {
                if (bufLast != null)
                    {
                    manager.release(bufLast);
                    }
                return Buffers.getEmptyBufferSequence();
                }

            bufLast.flip();
            return new SingleBufferSequence(manager, manager.truncate(bufLast));
            }
        else
            {
            bufLast.flip();
            listBuffers.add(manager.truncate(bufLast));
            return new MultiBufferSequence(manager,
                listBuffers.toArray(new ByteBuffer[listBuffers.size()]));
            }
        }

    // ----- data fields ----------------------------------------------------

    /**
    * The underlying BufferManager.
    */
    private BufferManager m_mgr;

    /**
    * The current (last allocated) buffer.
    */
    protected ByteBuffer m_buffer;

    /**
    * A list of ByteBuffers allocated by the underlying BufferManager.
    */
    private List<ByteBuffer> m_listBuffers = null;
    }