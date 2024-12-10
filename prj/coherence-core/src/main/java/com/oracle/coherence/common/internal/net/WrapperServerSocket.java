/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.internal.net;


import java.io.IOException;

import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.ServerSocket;

import java.nio.channels.ServerSocketChannel;


/**
* Wrapper server socket which delegates all operations to a delegate socket.
*
* @author jh/mf  2010.04.27
*/
public class WrapperServerSocket
        extends ServerSocket
    {
    // ----- constructor ----------------------------------------------------

    /**
    * Create a new ServerSocket that delegates all operations to the given
    * server socket.
    *
    * @param socket    the delegate server socket
    *
    * @throws IOException on error opening the socket
    */
    public WrapperServerSocket(ServerSocket socket)
            throws IOException
        {
        if (socket == null)
            {
            throw new IllegalArgumentException();
            }
        m_delegate = socket;
        }


    // ----- ServerSocket methods -------------------------------------------

    /**
    * {@inheritDoc}
    */
    public Socket accept()
            throws IOException
        {
        return m_delegate.accept();
        }

    /**
    * {@inheritDoc}
    */
    public void bind(SocketAddress endpoint)
            throws IOException
        {
        m_delegate.bind(endpoint);
        }

    /**
    * {@inheritDoc}
    */
    public void bind(SocketAddress endpoint, int backlog)
            throws IOException
        {
        m_delegate.bind(endpoint, backlog);
        }

    /**
    * {@inheritDoc}
    */
    public void close()
            throws IOException
        {
        super.close(); // just to free underlying FD
        m_delegate.close();
        }

    /**
    * {@inheritDoc}
    */
    public ServerSocketChannel getChannel()
        {
        return m_delegate.getChannel();
        }

    /**
    * {@inheritDoc}
    */
    public InetAddress getInetAddress()
        {
        return m_delegate.getInetAddress();
        }

    /**
    * {@inheritDoc}
    */
    public int getLocalPort()
        {
        return m_delegate.getLocalPort();
        }

    /**
    * {@inheritDoc}
    */
    public SocketAddress getLocalSocketAddress()
        {
        return m_delegate.getLocalSocketAddress();
        }

    /**
    * {@inheritDoc}
    */
    public int getReceiveBufferSize()
            throws SocketException
        {
        return m_delegate.getReceiveBufferSize();
        }

    /**
    * {@inheritDoc}
    */
    public boolean getReuseAddress()
            throws SocketException
        {
        return m_delegate.getReuseAddress();
        }

    /**
    * {@inheritDoc}
    */
    public int getSoTimeout()
            throws IOException
        {
        return m_delegate.getSoTimeout();
        }

    /**
    * {@inheritDoc}
    */
    public boolean isBound()
        {
        return m_delegate.isBound();
        }

    /**
    * {@inheritDoc}
    */
    public boolean isClosed()
        {
        return m_delegate.isClosed();
        }

    /**
    * {@inheritDoc}
    */
    public void setPerformancePreferences(int nConnectionTime,
            int nLatency, int nBandwidth)
        {
        m_delegate.setPerformancePreferences(nConnectionTime, nLatency,
                nBandwidth);
        }

    /**
    * {@inheritDoc}
    */
    public void setReceiveBufferSize(int cb)
            throws SocketException
        {
        m_delegate.setReceiveBufferSize(cb);
        }

    /**
    * {@inheritDoc}
    */
    public void setReuseAddress(boolean fReuse)
            throws SocketException
        {
        m_delegate.setReuseAddress(fReuse);
        }

    /**
    * {@inheritDoc}
    */
    public void setSoTimeout(int cSecs)
            throws SocketException
        {
        m_delegate.setSoTimeout(cSecs);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return m_delegate.toString();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The delegate socket.
    */
    protected final ServerSocket m_delegate;
    }
