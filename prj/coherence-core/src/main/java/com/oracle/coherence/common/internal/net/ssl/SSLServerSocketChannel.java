/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.internal.net.ssl;


import com.oracle.coherence.common.internal.net.WrapperServerSocketChannel;
import com.oracle.coherence.common.net.SSLSocketProvider;

import java.io.IOException;

import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;


/**
* SSLServerSocketChannel is a ServerSocketChannel which accepts SSL clients
* and returns SSLSocketChannels.
*
* SSLServerSocketChannel does not currently support blocking channels, it must
* be configured and used in non-blocking mode only.
*
* @author mf  2010.04.27
* @since Coherence 3.6
*/
public class SSLServerSocketChannel
        extends WrapperServerSocketChannel
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an SSLServerSocketChannel which wraps an un-secured
    * ServerSocketChannel.
    *
    * @param channel   the un-secuired ServerSocketChannel
    * @param provider  the SSLSocketProvider associated with this Channel
    *
    * @throws IOException if an I/O error occurs
    */
    public SSLServerSocketChannel(ServerSocketChannel channel,
            SSLSocketProvider provider)
            throws IOException
        {
        super(channel, new SSLSelectorProvider(channel.provider()));

        m_providerSocket = provider;
        m_fBlocking      = channel.isBlocking();
        }


    // ----- ServerSocketChannel methods ------------------------------------

    /**
    * {@inheritDoc}
    */
    public SocketChannel accept()
            throws IOException
        {
        if (m_fBlocking)
            {
            throw new IllegalBlockingModeException();
            }
        SocketChannel chan = f_delegate.accept();
        return chan == null
                ? null
                : new SSLSocketChannel(chan, getSocketProvider());
        }


    // ----- AbstractSelectableChannel methods ------------------------------

    /**
    * {@inheritDoc}
    */
    protected void implConfigureBlocking(boolean block)
            throws IOException
        {
        if (block)
            {
            throw new IllegalBlockingModeException();
            }
        f_delegate.configureBlocking(block);
        m_fBlocking = block;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "SSLServerSocketChannel(" + socket() + ")";
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return the SocketProvider which produced this socket.
     *
     * @return the SocketProvider
     */
    protected SSLSocketProvider getSocketProvider()
        {
        return m_providerSocket;
        }


    // ----- data members ---------------------------------------------------

    /**
     * The SSLSocketProvider associated with this socket.
     */
    protected final SSLSocketProvider m_providerSocket;

    /**
    * A cached copy of the configured blocking mode.
    */
    protected boolean m_fBlocking;
    }
