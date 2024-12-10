/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net.socketbus;


import com.oracle.coherence.common.net.exabus.Event;
import com.oracle.coherence.common.net.exabus.EndPoint;
import com.oracle.coherence.common.io.SingleBufferSequence;


/**
 * SingleBufferMessageEvent is an implementation of a Bus Event for Messages
 * over a single SharedBuffer.
 *
 * @author mf  2010.12.07
 */
public class SingleBufferMessageEvent
        extends SingleBufferSequence
        implements Event
    {
    /**
     * Construct a SingleBufferSequence around a single ByteBuffer.
     * <p>
     * The BufferSequence will directly reference the supplied buffer, subsequent
     * modifications to the buffer or its positions are not allowed.
     *
     * @param src     the associated connection
     * @param buffer  the buffer
     */
    public SingleBufferMessageEvent(SocketMessageBus.MessageConnection src,
            SharedBuffer buffer)
        {
        super(/*manager*/ null, buffer.get().slice());
        m_src          = src;
        m_bufferShared = buffer;
        }

    /**
     * Construct an "unsafe" SingleBufferMessageEvent around a single ByteBuffer.
     * <p>
     * The BufferSequence will directly reference the supplied buffer,
     * subsequent modifications to the buffer are not allowed.  The buffer's positions will
     * not be relied upon and thus are safe to update externally.
     *
     * @param src     the associated connection
     * @param buffer   the buffer
     * @param nPos     the position within the buffer
     * @param cb       the number of bytes
     */
    public SingleBufferMessageEvent(SocketMessageBus.MessageConnection src, SharedBuffer buffer, int nPos, int cb)
        {
        super(/*manager*/ null, buffer.get(), nPos, cb);
        m_src          = src;
        m_bufferShared = buffer;
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
        return m_src == null ? null : m_src.getPeer();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getContent()
        {
        return this;
        }

    /**
     * {@inheritDoc}
     */
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

        m_bufferShared.dispose();
        m_buffer = null;
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
     * The SharedBuffer which manages buffer.
     */
    protected final SharedBuffer m_bufferShared;
    }
