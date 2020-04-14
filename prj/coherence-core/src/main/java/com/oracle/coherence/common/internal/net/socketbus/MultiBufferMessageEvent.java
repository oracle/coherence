/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net.socketbus;


import com.oracle.coherence.common.io.MultiBufferSequence;
import com.oracle.coherence.common.io.BufferManager;
import com.oracle.coherence.common.net.exabus.Event;
import com.oracle.coherence.common.net.exabus.EndPoint;

import java.nio.ByteBuffer;


/**
 * MultiBufferMessageEvent is an implementation of a Bus Event for Messages
 * over a series of Buffers where the first and last buffer in the series come
 * from SharedBuffers.
 *
 * @author mf  2010.12.04
 */
public class MultiBufferMessageEvent
        extends MultiBufferSequence
        implements Event
    {
    /**
     * Construct a MultiBufferMessageEvent.
     *
     * @param src       the associated MessageConnection
     * @param manager   the buffer manager
     * @param aBuffer   the buffers
     * @param of        the offset of the first buffer in the sequence
     * @param cBuffer   the number of buffers in the sequence
     * @param cbBuffer  the total byte size of the sequence
     * @param buff0     the SharedBuffer associated with the first buffer
     * @param buffN     the SharedBuffer associated with the last buffer
     */
    public MultiBufferMessageEvent(SocketMessageBus.MessageConnection src,
            BufferManager manager,
            ByteBuffer[] aBuffer, int of, int cBuffer, long cbBuffer,
            SharedBuffer buff0, SharedBuffer buffN)
        {
        super(manager, aBuffer, of, cBuffer, cbBuffer);
        m_src     = src;
        m_buffer0 = buff0;
        m_bufferN = buffN;
        }


    // ----- Event interface ------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getType()
        {
        return Type.MESSAGE;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public EndPoint getEndPoint()
        {
        return m_src.getPeer();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getContent()
        {
        return this;
        }

    @Override
    public Object dispose(boolean fTakeContent)
        {
        SocketMessageBus.MessageConnection src = m_src;
        m_src = null;

        if (fTakeContent) // half dispose
            {
            src.onMessageDispose(this); // will intentionally NPE if double disposed
            return this; // return early leave BufferSequence intact
            }
        else if (src != null) // full dispose of Event
            {
            src.onMessageDispose(this);
            }
        // else src == null; // dispose of decoupled BufferSequence

        BufferManager manager = f_manager;
        ByteBuffer[]  aBuffer = m_aBuffer;

        m_buffer0.dispose();

        // release "middle" buffers
        for (int i = f_ofBuffer + 1, c = f_ofBuffer + f_cBuffer - 1; i < c; ++i)
            {
            manager.release(aBuffer[i]);
            }

        m_bufferN.dispose();

        m_aBuffer = null;
        return null;
        }

    // ----- Disposable interface -------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose()
        {
        dispose(/*fTakeContent*/ false);
        }


    // ----- Object interface -----------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String toString()
        {
        return getType() + " event for " + getEndPoint() + " containing " +
               getLength() + " bytes";
        }



    // ----- data members ---------------------------------------------------

    /**
     * The MessageConnection associated with the event, or <tt>null</tt> if dispose(true) has been called.
     */
    protected SocketMessageBus.MessageConnection m_src;

    /**
     * The SharedBuffer which manages the first accessable buffer in the
     * array, i.e. m_aBuffer[m_ofBuffer]
     */
    protected final SharedBuffer m_buffer0;

    /**
     * The SharedBuffer which manages the last accessable buffer in the array,
     * i.e. m_aBuffer[m_ofBuffer + m_cBuffer - 1]
     */
    protected final SharedBuffer m_bufferN;
    }
