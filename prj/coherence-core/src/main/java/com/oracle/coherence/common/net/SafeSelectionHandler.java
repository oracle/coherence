/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net;


import java.io.IOException;
import java.nio.channels.SelectableChannel;


/**
 * SafeChannelHandler is an abstract SelectorService.Handler implementation
 * with additional error handling support.
 *
 * @param <C>  the SelectableChannel type
 *
 * @author mf  2010.11.17
 */
public abstract class SafeSelectionHandler<C extends SelectableChannel>
        implements SelectionService.Handler
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a SafeChannel for the specified channel.
     *
     * @param channel  the channel to handle
     */
    protected SafeSelectionHandler(C channel)
        {
        m_channel = channel;
        }


    // ----- SafeChannelHandler interface -----------------------------------

    /**
     * Called when the channel has been selected.
     * <p>
     * If this method throws an exception it will be handled by {@link
     * #onException}
     *
     * @param nOps  the selected ops
     *
     * @return the new interest set
     *
     * @throws IOException on an I/O error
     */
    protected abstract int onReadySafe(int nOps)
        throws IOException;

    /**
     * Called in the event that {@link #onReadySafe} resulted in an exception.
     * <p>
     * The default implementation simply closes the channel.
     *
     * @param t  the exception
     *
     * @return  the new interest set, the default implementation returns 0
     */
    protected int onException(Throwable t)
        {
        try
            {
            m_channel.close();
            }
        catch (IOException e) {}
        return 0;
        }

    /**
     * Return the associated channel.
     *
     * @return the channel
     */
    public C getChannel()
        {
        return m_channel;
        }


    // ----- Handler interface ----------------------------------------------

    /**
     * {@inheritDoc}
     */
    public final int onReady(int nOps)
        {
        try
            {
            return onReadySafe(nOps);
            }
        catch (Throwable t)
            {
            return onException(t);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The SelectableChannel.
     */
    private final C m_channel;
    }
