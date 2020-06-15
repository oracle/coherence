/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.internal.net.ssl;


import com.oracle.coherence.common.internal.net.WrapperServerSocket;
import com.oracle.coherence.common.net.SSLSocketProvider;


import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;


/**
* Wrapper server socket implementation that performs hostname verfication
* during connect.
*
* @author jh  2010.04.27
*/
public class SSLServerSocket
        extends WrapperServerSocket
    {
    // ----- constructor ----------------------------------------------------

    /**
    * Create a new SSLServerSocket that delegates all operations to the given
    * server socket.
    *
    * @param socket    the delegate server socket
    * @param provider  the SSLSocketProvider that created this SSLServerSocket
    *
    * @throws IOException on error opening the socket
    */
    public SSLServerSocket(ServerSocket socket, SSLSocketProvider provider)
            throws IOException
        {
        super(socket);
        if (provider == null)
            {
            throw new IllegalArgumentException();
            }
        m_provider = provider;
        }


    // ----- ServerSocket methods -------------------------------------------

    /**
    * {@inheritDoc}
    */
    public Socket accept()
            throws IOException
        {
        return new SSLSocket(m_delegate.accept(), m_provider);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return "SSLServerSocket{" + m_delegate + "}";
        }


    // ----- data members ---------------------------------------------------

    /**
    * The SSLSocketProvider that created this SSLSocket.
    */
    protected final SSLSocketProvider m_provider;
    }
