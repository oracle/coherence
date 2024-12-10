/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net;


import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.Set;


/**
* Wrapper ServerSocketChannel implementation that delegates all operations to
* a delegate ServerSocketChannel.
*
* @author mf  2010.05.19
*/
public class WrapperServerSocketChannel
    extends ServerSocketChannel
    implements WrapperSelector.WrapperSelectableChannel
    {
    // ----- constructors ---------------------------------------------------

    public WrapperServerSocketChannel(ServerSocketChannel channel, SelectorProvider provider)
            throws IOException
        {
        super(provider);

        f_delegate = channel;
        f_socket = wrapSocket(channel.socket());
        }


    // ----- WrapperServerSocketChannel methods -----------------------------

    /**
    * Produce a wrapper around the specified socket.
    *
    * @param socket  the socket to wrap
    * @return  the wrapper socket
    *
    * @throws IOException if an I/O error occurs
    */
    protected ServerSocket wrapSocket(ServerSocket socket)
            throws IOException
        {
        return new WrapperServerSocket(socket)
            {
            public Socket accept()
                    throws IOException
                {
                throw new UnsupportedOperationException();
                }

            public ServerSocketChannel getChannel()
                {
                return WrapperServerSocketChannel.this;
                }
            };
        }

    // ----- ServerSocketChannel methods ------------------------------------

    /**
    * Unsupported.
    *
    * @return never
    *
    * @throws UnsupportedOperationException not supported
    */
    public static ServerSocketChannel open()
        {
        throw new UnsupportedOperationException();
        }

    /**
    * {@inheritDoc}
    */
    public ServerSocket socket()
        {
        return f_socket;
        }

    /**
    * {@inheritDoc}
    */
    public SocketChannel accept()
            throws IOException
        {
        SocketChannel channel = f_delegate.accept();
        return channel == null
                ? null
                : new WrapperSocketChannel(channel, provider());
        }

    /**
    * {@inheritDoc}
    */
    protected void implCloseSelectableChannel()
            throws IOException
        {
        f_delegate.close();
        }

    /**
    * {@inheritDoc}
    */
    protected void implConfigureBlocking(boolean block)
            throws IOException
        {
        f_delegate.configureBlocking(block);
        }

    @Override
    public ServerSocketChannel bind(SocketAddress local, int backlog)
            throws IOException
        {
        return f_delegate.bind(local, backlog);
        }

    @Override
    public <T> ServerSocketChannel setOption(SocketOption<T> name, T value)
            throws IOException
        {
        return f_delegate.setOption(name, value);
        }


    // ----- NetworkChannel methods -----------------------------------------

    @Override
    public SocketAddress getLocalAddress()
            throws IOException
        {
        return f_delegate.getLocalAddress();
        }

    @Override
    public <T> T getOption(SocketOption<T> name)
            throws IOException
        {
        return f_delegate.getOption(name);
        }

    @Override
    public Set<SocketOption<?>> supportedOptions()
        {
        return f_delegate.supportedOptions();
        }


    // ----- WrapperSelectableChannel methods -------------------------------

    /**
    * {@inheritDoc}
    */
    public WrapperSelector.WrapperSelectionKey registerInternal(
                WrapperSelector selector, int ops, Object att)
            throws IOException
        {
        return new WrapperSelector.WrapperSelectionKey(selector,
                f_delegate.register(selector.getDelegate(), ops), att)
            {
            public SelectableChannel channel()
                {
                return WrapperServerSocketChannel.this;
                }
            };
        }

    // ----- data members ---------------------------------------------------

    /**
     * The delegate channel.
     */
    protected final ServerSocketChannel f_delegate;

    /**
     * The associated ServerSocket.
     */
    protected final ServerSocket f_socket;
    }
