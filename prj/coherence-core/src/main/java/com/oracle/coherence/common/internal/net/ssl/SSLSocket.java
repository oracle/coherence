/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.internal.net.ssl;


import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Timeout;
import com.oracle.coherence.common.internal.net.WrapperSocket;
import com.oracle.coherence.common.net.SSLSocketProvider;

import java.io.IOException;

import java.net.SocketAddress;
import java.net.Socket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;


/**
* Wrapper socket implementation that performs hostname verification during
* connect.
*
* @author jh  2010.04.27
*/
public class SSLSocket
        extends WrapperSocket
    {
    // ----- constructor ----------------------------------------------------

    /**
    * Create a new SSLSocket that delegates all operations to the given
    * socket.
    *
    * @param socket    the delegate socket
    * @param provider  the SSLSocketProvider that created this SSLSocket
    *
    * @throws IOException if an I/O error occurs
    */
    public SSLSocket(Socket socket, SSLSocketProvider provider)
            throws IOException
        {
        super(socket);
        if (provider == null)
            {
            throw new IllegalArgumentException();
            }
        m_provider = provider;
        if (socket.isConnected())
            {
            m_delegate = wrapSocket(socket, false);
            }
        }


    // ----- Socket methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public synchronized void connect(SocketAddress addr, int cMillis)
            throws IOException
        {
        Socket delegate       = m_delegate;
        int    cMillisRestore = delegate.getSoTimeout();
        try (Timeout t = Timeout.after(cMillis == 0 ? Long.MAX_VALUE : cMillis))
            {
            Blocking.connect(delegate, addr);

            // include SSL Handshake in the connect timeout by setting SO_TIMEOUT
            long cMillisRemaining = Timeout.remainingTimeoutMillis();
            delegate.setSoTimeout(cMillisRemaining >= Integer.MAX_VALUE ? 0 : (int) cMillisRemaining);
            m_delegate = wrapSocket(delegate, true);
            }
        catch (InterruptedException e)
            {
            throw new SocketTimeoutException(e.getMessage());
            }
        finally
            {
            if (!delegate.isClosed())
                {
                try
                    {
                    delegate.setSoTimeout(cMillisRestore);
                    }
                catch (IOException e) {}
                }
            }
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return "SSLSocket{" + m_delegate + "}";
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Wrap the supplied plain Socket as an SSLSocket. The supplied socket must
    * be connected.
    *
    * @param delegate     the socket to delegate to
    * @param fClientMode  if the ssl socket should be in client or server mode
    *
    * @return the corresponding SSLSocket
    *
    * @throws IOException if an I/O error occurs
    */
    public javax.net.ssl.SSLSocket wrapSocket(Socket delegate, boolean fClientMode)
            throws IOException
        {
        try
            {
            SSLSocketProvider              provider = m_provider;
            SSLSocketProvider.Dependencies deps     = provider.getDependencies();
            InetAddress addrConnected = delegate.getInetAddress();

            javax.net.ssl.SSLSocket sslDelegate = (javax.net.ssl.SSLSocket)
                     deps.getSSLContext().getSocketFactory()
                    .createSocket(delegate, addrConnected.getHostName(),
                            delegate.getPort(), true);

            sslDelegate.setUseClientMode(fClientMode);
            if (!fClientMode)
                {
                switch (deps.getClientAuth())
                    {
                    case wanted:
                        sslDelegate.setNeedClientAuth(false);
                        sslDelegate.setWantClientAuth(true);
                        break;
                    case required:
                        sslDelegate.setWantClientAuth(true);
                        sslDelegate.setNeedClientAuth(true);
                        break;
                    case none:
                    default:
                        sslDelegate.setWantClientAuth(false);
                        sslDelegate.setNeedClientAuth(false);
                        break;
                    }
                }

            String[] asCiphers =  deps.getEnabledCipherSuites();
            if (asCiphers != null)
                {
                sslDelegate.setEnabledCipherSuites(asCiphers);
                }

            String[] asProtocols =  deps.getEnabledProtocolVersions();
            if (asProtocols != null)
                {
                sslDelegate.setEnabledProtocols(asProtocols);
                }

            provider.ensureSessionValidity(sslDelegate.getSession(), delegate);

            return sslDelegate;
            }
        catch (Exception e)
            {
            try
                {
                delegate.close();
                }
            catch (IOException ee)
                {
                // ignore
                }
            if (e instanceof IOException)
                {
                throw (IOException) e;
                }
            throw new RuntimeException(e);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
    * The SSLSocketProvider that created this SSLSocket.
    */
    protected final SSLSocketProvider m_provider;
    }
