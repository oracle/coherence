/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.memcached.server;

import com.oracle.coherence.common.io.BufferManager;

import java.nio.channels.SocketChannel;

/**
 * Connection Factory to create connections that can handle memcached binary
 * or ASCII protocol.  Currently only the binary protocol is supported.
 * <p>
 * ConnectionFactory is not thread-safe but it used by a single SelectionService
 * thread which services the ServerSocketConnection and accepts new client sockets.
 *
 * @author bb 2013.05.01
 *
 * @since Coherence 12.1.3
 */
public class ConnectionFactory
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructor.
     *
     * @param bufMgr   the BufferManager
     * @param fBinary  flag indicating if Binary connections need to be created
     */
    public ConnectionFactory(BufferManager bufMgr, boolean fBinary)
        {
        m_bufferManager = bufMgr;
        m_fBinary       = fBinary;
        }

    // ----- ConnectionFactory methods --------------------------------------

    /**
     * Create a Connection.
     *
     * @param channel  SocketChannel that is wrapped by the Connection.
     *
     * @return Connection
     */
    public Connection createConnection(SocketChannel channel)
        {
        return m_fBinary ? new BinaryConnection(m_bufferManager, channel, m_nConnId++) : null;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The BufferManager.
     */
    protected BufferManager m_bufferManager;

    /**
     * Flag to indicate if binary connection is needed.
     */
    protected boolean       m_fBinary;

    /**
     * Connection id counter.
     */
    protected int           m_nConnId;
    }