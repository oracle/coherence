/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.memcached.server;

import com.oracle.coherence.common.base.Continuation;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.io.BufferManager;
import com.oracle.coherence.common.io.BufferManagers;

import com.oracle.coherence.common.net.SafeSelectionHandler;
import com.oracle.coherence.common.net.SelectionService;
import com.oracle.coherence.common.net.SelectionServices;
import com.oracle.coherence.common.net.SocketProvider;

import com.tangosol.coherence.memcached.DefaultRequestHandler;
import com.tangosol.coherence.memcached.Request;
import com.tangosol.coherence.memcached.RequestHandler;

import com.tangosol.net.Service;
import com.tangosol.net.security.IdentityAsserter;

import com.tangosol.util.Base;

import java.io.IOException;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import java.util.List;

import java.util.concurrent.Executor;

/**
 * MemcachedServer handles the lower level SocketChannel interactions for the
 * MemcachedAcceptor.
 * <p>
 * The MemcachedServer utilizes the "FMW-commons" SelectionService to perform
 * channel I/O.
 *
 * @author bb 2013.05.01
 *
 * @since Coherence 12.1.3
 */
public class MemcachedServer
    {
    /**
     * Set the socket provider.
     *
     * @param provider  SocketProvider
     */
    public void setSocketProvider(SocketProvider provider)
        {
        m_provider = provider;
        }

    /**
     * Set the cache name configured for the memcached acceptor.
     *
     * @param sCacheName  the cache name
     */
    public void setCacheName(String sCacheName)
        {
        m_sCacheName = sCacheName;
        }

    /**
     * Set if binary-pass-thru is configured.
     *
     * @param fBinaryPassThru  BinaryPassThru flag
     */
    public void setBinaryPassthru(boolean fBinaryPassThru)
        {
        m_fBinaryPassThru = fBinaryPassThru;
        }

    /**
     * Set the memcached acceptor server address.
     *
     * @param sAddr  the Acceptor Address
     */
    public void setLocalAddress(String sAddr)
        {
        m_sAddr = sAddr;
        }

    /**
     * Set the memcached acceptor port.
     *
     * @param nPort  the Acceptor Port
     */
    public void setLocalPort(int nPort)
        {
        m_nPort = nPort;
        }

    /**
     * Set the Executor.
     *
     * @param executor  the TaskExecutor
     */
    public void setExecutor(Executor executor)
        {
        m_executor = executor;
        }

    /**
     * Set the BufferManager.
     *
     * @param manager  the BufferManager
     */
    public void setBufferManager(BufferManager manager)
        {
        m_bufferManager = manager;
        }

    /**
     * Set the SASL authentication method.
     *
     * @param sAuthMethod  the Auth method
     */
    public void setAuthMethod(String sAuthMethod)
        {
        m_sAuthMethod = sAuthMethod;
        }

    /**
     * Set the Proxy Service that is embedding this memcached adapter.
     *
     * @param service  the parent proxy service
     */
    public void setParentService(Service service)
        {
        m_parentService = service;
        }

    /**
     * Set the configured Identity Asserter.
     *
     * @param asserter IdentityAsserter
     */
    public void setIdentityAsserter(IdentityAsserter asserter)
        {
        m_identityAsserter = asserter;
        }

    /**
     * Start the server.
     *
     * @throws IOException
     */
    public void start()
            throws IOException
        {
        ServerSocketChannel srvrChannel = m_srvrChannel = m_provider.openServerSocketChannel();
        srvrChannel.configureBlocking(false);
        srvrChannel.socket().bind(m_provider.resolveAddress(m_sAddr + ":" + m_nPort));

        SelectionService selectionService = m_selectionService = SelectionServices.getDefaultService();
        selectionService.register(srvrChannel, new AcceptHandler(
                    srvrChannel, new ConnectionFactory(m_bufferManager, true)));
        }

    /**
     * Stop the server.
     *
     * @throws IOException
     */
    public void stop()
            throws IOException
        {
        if (m_srvrChannel != null)
            {
            m_srvrChannel.close();
            }
        }

    // ----- inner class: AcceptHandler -------------------------------------

    /**
     * AcceptHandler accepts new memcached client connections.
     */
    protected class AcceptHandler
            extends SafeSelectionHandler<ServerSocketChannel>
        {
        /**
         * Construct an AcceptHandler for accepting new client socket connections.
         *
         * @param srvrChannel  the ServerSocketChannel
         * @param connFactory  ConnectionFactory for creating connection objects on top of the connected sockets.
         */
        protected AcceptHandler(ServerSocketChannel srvrChannel, ConnectionFactory connFactory)
            {
            super(srvrChannel);
            m_connFactory = connFactory;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected int onReadySafe(int nOps)
                throws IOException
            {
            SocketChannel chan = null;
            try
                {
                chan = getChannel().accept();
                if (chan != null)
                    {
                    chan.configureBlocking(false);
                    Connection     conn       = m_connFactory.createConnection(chan);
                    MessageHandler msgHandler = new MessageHandler(conn, m_executor);
                    // create a new RequestHandler per client connection.
                    RequestHandler reqHandler = new DefaultRequestHandler(m_sCacheName, m_parentService, m_sAuthMethod,
                                                m_fBinaryPassThru, m_identityAsserter, m_executor, msgHandler);
                    msgHandler.setRequestHandler(reqHandler);
                    conn.setFlowControl(msgHandler);
                    m_selectionService.register(chan, msgHandler);
                    }
                }
            catch (final IOException e)
                {
                if (chan == null)
                    {
                    // ServerSocketChannel gone bad.
                    // We need to restart the service by running this task
                    // in the proxy daemon pool.
                    m_executor.execute(new Runnable()
                        {
                        @Override
                        public void run()
                            {
                            // this will restart the service.
                            throw Base.ensureRuntimeException(e);
                            }
                        });

                    // de-register the server socket channel from the SelectionService
                    throw new RuntimeException(e);
                    }
                else
                    // error with new channel; just close it
                    {
                    try
                        {
                        chan.close();
                        }
                    catch (IOException e1) {}
                    }
                }
            return OP_ACCEPT;
            }

        /**
         * The ConnectionFactory.
         */
        protected ConnectionFactory m_connFactory;
        }

    // ----- inner class: MessageHandler ------------------------------------

    /**
     * MessageHandler handles messages on the connected socket channel.
     */
    protected class MessageHandler
            extends SafeSelectionHandler<SocketChannel>
            implements Connection.ConnectionFlowControl
        {
        /**
         * Construct a message handler.
         *
         * @param conn      Memcached Connection
         * @param handler   Request handler
         * @param executor  Task executor
         */
        protected MessageHandler(Connection conn, Executor executor)
            {
            super(conn.getChannel());
            m_conn     = conn;
            m_executor = executor;
            }

        /**
         * Set the RequestHandler.
         *
         * @param handler  RequestHandler
         */
        public void setRequestHandler(RequestHandler handler)
            {
            m_handler = handler;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected int onReadySafe(int nOps)
                throws IOException
            {
            int nFlag = m_nOpRead;
            if ((nOps & nFlag) != 0)
                {
                nFlag = handleRead();
                }
            if ((nOps & OP_WRITE) != 0)
                {
                nFlag |= handleWrite();
                }
            return nFlag;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void resumeWrites()
            {
            SocketChannel channel = getChannel();
            try
                {
                m_selectionService.register(channel, this);
                }
            catch (IOException ioe)
                {
                Logger.err("Failed to resume writes. Closing channel.");
                closeChannel(channel);
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void pauseReads()
            {
            m_nOpRead = 0;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void resumeReads()
            {
            SocketChannel channel = getChannel();
            try
                {
                m_nOpRead = OP_READ;
                m_selectionService.register(channel, this);
                }
            catch (IOException ioe)
                {
                Logger.err("Failed to resume read. Closing channel.");
                closeChannel(channel);
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isReadPaused()
            {
            return m_nOpRead == 0;
            }

        /**
         * Handle socket channel reads.
         *
         * @return SelectionService's operation-set bit for read operations.
         *
         * @throws IOException
         */
        protected int handleRead()
                throws IOException
            {
            List<Request> requestList = m_conn.read();
            for (Request request : requestList)
                {
                onRequest(request);
                }
            RequestHandler handler = m_handler;
            handler.flush();
            if (handler.checkBacklog(null))
                {
                 BacklogEndedContinuation backlogContinuation = new BacklogEndedContinuation();
                 if (handler.checkBacklog(backlogContinuation))
                     {
                     backlogContinuation.pause();
                     }
                }
            return m_nOpRead;
            }

        /**
         * Handle socket channel writes.
         *
         * @return SelectionService's operation-set bit for write operations if there
         *         are pending data to be written or 0 to de-register for write operations.
         *
         * @throws IOException
         */
        protected int handleWrite()
                throws IOException
            {
            return m_conn.write();
            }

        /**
         * Submit the request to the task executor.
         *
         * @param request  Request to process
         */
        protected void onRequest(Request request)
            {
            new Task(request, m_handler).run();
            }

        /**
         * Close socket channel.
         *
         * @param channel  socket channel to close
         */
        protected void closeChannel(SocketChannel channel)
            {
            try
                {
                channel.close();
                }
            catch (IOException ex) { } /*ignore*/
            }

        /**
         * Continuation that is called when the underlying cache service backlog ends.
         */
        protected class BacklogEndedContinuation
                implements Continuation<Void>
            {

            /**
             * {@inheritDoc}
             */
            @Override
            public void proceed(Void r)
                {
                m_fResumed = true;
                resumeReads();
                }

            /**
             * Pause reading from the memcached connection.
             */
            public void pause()
                {
                pauseReads();
                if (m_fResumed)
                    {
                    resumeReads();
                    }
                }

            // ----- data members --------------------------------------------

            /**
             * Flag to indicate if continuation has been resumed.
             */
            protected volatile boolean m_fResumed = false;
            }

        // ----- data members -----------------------------------------------

        /**
         * Memcached connection
         */
        protected Connection m_conn;

        /**
         * Request Handler
         */
        protected RequestHandler m_handler;

        /**
         * Task Executor
         */
        protected Executor m_executor;

        /**
         * SelectionService's read operation flag
         */
        protected volatile int m_nOpRead = OP_READ;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Selection Service for handling socket channels
     */
    protected SelectionService m_selectionService;

    /**
     * Server socket channel
     */
    protected ServerSocketChannel m_srvrChannel;

    /**
     * Parent Proxy Service
     */
    protected Service m_parentService;

    /**
     * Named cache name
     */
    protected String m_sCacheName;

    /**
     * Flag to indicate if binary-pass-thru is configured.
     */
    protected boolean m_fBinaryPassThru;

    /**
     * Configured SASL authentication method.
     */
    protected String m_sAuthMethod;

    /**
     * Server Ip-address
     */
    protected String m_sAddr;

    /**
     * Server port
     */
    protected int m_nPort;

    /**
     * Socket Provider
     */
    protected SocketProvider m_provider;

    /**
     * TaskExecutor
     */
    protected Executor m_executor;

    /**
     * Identity Asserter
     */
    protected IdentityAsserter m_identityAsserter;

    /**
     * Buffer Manager
     */
    protected BufferManager m_bufferManager = BufferManagers.getHeapManager();
    }
