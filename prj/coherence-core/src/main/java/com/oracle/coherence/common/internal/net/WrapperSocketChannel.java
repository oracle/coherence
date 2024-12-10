/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.internal.net;

import java.io.IOException;

import java.net.SocketOption;
import java.nio.ByteBuffer;

import java.nio.channels.SocketChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.spi.SelectorProvider;

import java.net.Socket;
import java.net.SocketAddress;
import java.util.Set;


/**
* Wrapper SocketChannel implementation that delegates all operations to a
* delegate SocketChannel.
*
* @author mf  2010.05.19
*/
public class WrapperSocketChannel
    extends SocketChannel
    implements WrapperSelector.WrapperSelectableChannel
    {
    // ----- constructors ---------------------------------------------------

    public WrapperSocketChannel(SocketChannel channel, SelectorProvider provider)
        {
        super(provider);

        f_delegate = channel;
        f_socket   = wrapSocket(channel.socket());
        }


    // ----- WrapperSocketChannel methods -----------------------------------

    /**
    * Produce a wrapper around the specified socket.
    *
    * @param socket  the socket to wrap
    * @return  the wrapper socket
    */
    protected Socket wrapSocket(Socket socket)
        {
        return new WrapperSocket(socket)
            {
            public SocketChannel getChannel()
                {
                return WrapperSocketChannel.this;
                }
            };
        }


    // ----- SocketChannel methods ------------------------------------------

    /**
    * Unsupported.
    *
    * @return never
    *
    * @throws UnsupportedOperationException not supported
    */
    public static SocketChannel open()
        {
        throw new UnsupportedOperationException();
        }

    /**
    * {@inheritDoc}
    */
    public Socket socket()
        {
        return f_socket;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isConnected()
        {
        return f_delegate.isConnected();
        }

    /**
    * {@inheritDoc}
    */
    public boolean isConnectionPending()
        {
        return f_delegate.isConnectionPending();
        }

    /**
    * {@inheritDoc}
    */
    public boolean connect(SocketAddress remote)
            throws IOException
        {
        return f_delegate.connect(remote);
        }

    /**
    * {@inheritDoc}
    */
    public boolean finishConnect()
            throws IOException
        {
        return f_delegate.finishConnect();
        }

    /**
    * {@inheritDoc}
    */
    public int read(ByteBuffer dst)
            throws IOException
        {
        return f_delegate.read(dst);
        }

    /**
    * {@inheritDoc}
    */
    public long read(ByteBuffer[] dsts, int offset, int length)
            throws IOException
        {
        return f_delegate.read(dsts, offset, length);
        }

    /**
    * {@inheritDoc}
    */
    public int write(ByteBuffer src)
            throws IOException
        {
        return f_delegate.write(src);
        }

    /**
    * {@inheritDoc}
    */
    public long write(ByteBuffer[] srcs, int offset, int length)
            throws IOException
        {
        return f_delegate.write(srcs, offset, length);
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
    public SocketChannel bind(SocketAddress local)
            throws IOException
        {
        return f_delegate.bind(local);
        }

    @Override
    public <T> SocketChannel setOption(SocketOption<T> name, T value)
            throws IOException
        {
        return f_delegate.setOption(name, value);
        }

    @Override
    public SocketChannel shutdownInput()
            throws IOException
        {
        return f_delegate.shutdownInput();
        }

    @Override
    public SocketChannel shutdownOutput()
            throws IOException
        {
        return f_delegate.shutdownOutput();
        }

    @Override
    public SocketAddress getRemoteAddress()
            throws IOException
        {
        return f_delegate.getRemoteAddress();
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
        return new WrapperSelector.WrapperSelectionKey(selector, f_delegate.register(
                selector.getDelegate(), ops), att)
            {
            public SelectableChannel channel()
                {
                return WrapperSocketChannel.this;
                }
            };
        }


    // ----- data memberse --------------------------------------------------

    /**
     * The delegate SocketChannel.
     */
    protected final SocketChannel f_delegate;

    /**
     * The associated WrapperSocket.
     */
    protected final Socket f_socket;
    }
