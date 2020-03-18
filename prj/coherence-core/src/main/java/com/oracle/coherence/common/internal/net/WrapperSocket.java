/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.internal.net;


import com.oracle.coherence.common.base.Blocking;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

import java.nio.channels.SocketChannel;


/**
* Wrapper socket implementation that delegates all operations to a delegate
* socket.
*
* @author jh/mf  2010.04.27
*/
public class WrapperSocket
        extends Socket
    {
    // ----- constructor ----------------------------------------------------

    /**
    * Create a new Socket that delegates all operations to the given
    * socket.
    *
    * @param socket  the delegate socket
    */
    public WrapperSocket(Socket socket)
        {
        if (socket == null)
            {
            throw new IllegalArgumentException();
            }
        m_delegate = socket;
        }


    // ----- Socket methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void bind(SocketAddress addr)
            throws IOException
        {
        m_delegate.bind(addr);
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
    public void connect(SocketAddress addr)
            throws IOException
        {
        connect(addr, 0);
        }

    /**
    * {@inheritDoc}
    */
    public void connect(SocketAddress addr, int cMillis)
            throws IOException
        {
        Blocking.connect(m_delegate, addr, cMillis);
        }

    /**
    * {@inheritDoc}
    */
    public SocketChannel getChannel()
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
    public InputStream getInputStream()
            throws IOException
        {
        return m_delegate.getInputStream();
        }

    /**
    * {@inheritDoc}
    */
    public boolean getKeepAlive()
            throws SocketException
        {
        return m_delegate.getKeepAlive();
        }

    /**
    * {@inheritDoc}
    */
    public InetAddress getLocalAddress()
        {
        return m_delegate.getLocalAddress();
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
    public boolean getOOBInline()
            throws SocketException
        {
        return m_delegate.getOOBInline();
        }

    /**
    * {@inheritDoc}
    */
    public OutputStream getOutputStream()
            throws IOException
        {
        return m_delegate.getOutputStream();
        }

    /**
    * {@inheritDoc}
    */
    public int getPort()
        {
        return m_delegate.getPort();
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
    public SocketAddress getRemoteSocketAddress()
        {
        return m_delegate.getRemoteSocketAddress();
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
    public int getSendBufferSize()
            throws SocketException
        {
        return m_delegate.getSendBufferSize();
        }

    /**
    * {@inheritDoc}
    */
    public int getSoLinger()
            throws SocketException
        {
        return m_delegate.getSoLinger();
        }

    /**
    * {@inheritDoc}
    */
    public int getSoTimeout()
            throws SocketException
        {
        return m_delegate.getSoTimeout();
        }

    /**
    * {@inheritDoc}
    */
    public boolean getTcpNoDelay()
            throws SocketException
        {
        return m_delegate.getTcpNoDelay();
        }

    /**
    * {@inheritDoc}
    */
    public int getTrafficClass()
            throws SocketException
        {
        return m_delegate.getTrafficClass();
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
    public boolean isConnected()
        {
        return m_delegate.isConnected();
        }

    /**
    * {@inheritDoc}
    */
    public boolean isInputShutdown()
        {
        return m_delegate.isInputShutdown();
        }

    /**
    * {@inheritDoc}
    */
    public boolean isOutputShutdown()
        {
        return m_delegate.isOutputShutdown();
        }

    /**
    * {@inheritDoc}
    */
    public void sendUrgentData(int nData)
            throws IOException
        {
        m_delegate.sendUrgentData(nData);
        }

    /**
    * {@inheritDoc}
    */
    public void setKeepAlive(boolean fKeepAlive)
            throws SocketException
        {
        m_delegate.setKeepAlive(fKeepAlive);
        }

    /**
    * {@inheritDoc}
    */
    public void setOOBInline(boolean fInline)
            throws SocketException
        {
        m_delegate.setOOBInline(fInline);
        }

    /**
    * {@inheritDoc}
    */
    public void setPerformancePreferences(int nConnectionTime, int nLatency,
            int nBandwidth)
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
    public void setSendBufferSize(int cb)
            throws SocketException
        {
        m_delegate.setSendBufferSize(cb);
        }

    /**
    * {@inheritDoc}
    */
    public void setSoLinger(boolean fLinger, int cSecs)
            throws SocketException
        {
        m_delegate.setSoLinger(fLinger, cSecs);
        }

    /**
    * {@inheritDoc}
    */
    public void setSoTimeout(int cMillis)
            throws SocketException
        {
        m_delegate.setSoTimeout(cMillis);
        }

    /**
    * {@inheritDoc}
    */
    public void setTcpNoDelay(boolean fNoDelay)
            throws SocketException
        {
        m_delegate.setTcpNoDelay(fNoDelay);
        }

    /**
    * {@inheritDoc}
    */
    public void setTrafficClass(int nClass)
            throws SocketException
        {
        m_delegate.setTrafficClass(nClass);
        }

    /**
    * {@inheritDoc}
    */
    public void shutdownInput()
            throws IOException
        {
        m_delegate.shutdownInput();
        }

    /**
    * {@inheritDoc}
    */
    public void shutdownOutput()
            throws IOException
        {
        m_delegate.shutdownOutput();
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
    protected Socket m_delegate;
    }
