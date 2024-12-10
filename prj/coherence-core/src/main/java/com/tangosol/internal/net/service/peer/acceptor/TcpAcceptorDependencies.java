/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.net.SocketAddressProvider;
import com.tangosol.net.SocketOptions;

import com.tangosol.util.Filter;

/**
* The TcpAcceptorDependencies interface provides a TcpAcceptor object with its external
* dependencies.
*
* @author pfm  2011.06.27
* @since Coherence 12.1.2
*/
public interface TcpAcceptorDependencies
        extends AcceptorDependencies
    {
    /**
     * Return the optional {@link ParameterizedBuilder} of {@link Filter} that is used by the TcpAcceptor to
     * determine whether to accept a particular TcpInitiator. The {@link Filter#evaluate} method will
     * be passed the java.net.InetAddress of the client. Implementations should return "true"
     * to allow the client to connect.
     *
     * @return the authorized host filter builder
     */
    public ParameterizedBuilder<Filter> getAuthorizedHostFilterBuilder();

    /**
     * Return the default backlog limit in bytes.  If the backlog exceeds
     * this size then the connection will be terminated.
     *
     * @return the default limit buffer size in bytes
     */
    public long getDefaultLimitBytes();

    /**
     * Return the default backlog limit in messages.  If the backlog exceeds
     * this message count then the connection will be terminated.
     *
     * @return the default limit message count
     */
    public int getDefaultLimitMessages();

    /**
     * Return the default nominal backlog size in bytes.  If the backlog falls
     * below this size then the connection will no longer be suspect.
     *
     * @return the nominal buffer size in bytes
     */
    public long getDefaultNominalBytes();

    /**
     * Return the default nominal backlog size in messages.  If the backlog falls
     * below this count then the connection will no longer be suspect..
     *
     * @return the nominal message count
     */
    public int getDefaultNominalMessages();

    /**
     * Return the default suspect backlog size in bytes.  If the backlog exceeds
     * this size then the connection becomes suspect.
     *
     * @return the default suspect buffer size in bytes
     */
    public long getDefaultSuspectBytes();

    /**
     * Return the default suspect backlog size in messages.  If the backlog exceeds
     * this count then the connection becomes suspect.
     *
     * @return the default suspect message count
     */
    public int getDefaultSuspectMessages();

    /**
     * Return the incoming buffer pool configuration.
     *
     * @return the incoming buffer pool configuration
     */
    public BufferPoolConfig getIncomingBufferPoolConfig();

    /**
     * Return the listen backlog.
     *
     * @return the listen backlog
     */
    public int getListenBacklog();

    /**
     * Return the local {@link SocketAddressProvider} builder.
     *
     * @return the local SocketAddressProvider builder
     */
    public ParameterizedBuilder<SocketAddressProvider> getLocalAddressProviderBuilder();

    /**
     * Return the outgoing buffer pool configuration.
     *
     * @return the outgoing buffer pool configuration
     */
    public BufferPoolConfig getOutgoingBufferPoolConfig();

    /**
     * Return the SocketOptions used by the TcpAcceptor.
     *
     * @return the SocketOptions
     */
    public SocketOptions getSocketOptions();

    /**
     * Return the SocketProviderBuilder used by the TcpAcceptor to open ServerSocketChannels.
     *
     * @return the socket provider builder
     */
    public SocketProviderBuilder getSocketProviderBuilder();

    /**
     * Return true if the suspect protocol is enabled.
     *
     * @return true if the suspect protocol is enabled
     */
    public boolean isSuspectProtocolEnabled();

    // ----- inner interface  -----------------------------------------------

    /**
     * The BufferPoolConfig interface specifies the configuration of a buffer pool.
     */
    public static interface BufferPoolConfig
        {
        /**
         * Return the size, in bytes, of newly allocated ByteBuffer objects.
         *
         * @return the buffer size
         */
        public int getBufferSize();

        /**
         * Return the type of newly allocated ByteBuffer objects.  The type will be
         * either TYPE_DIRECT or TYPE_HEAP.
         *
         * @return the buffer type
         */
        public int getBufferType();

        /**
         * Return the maximum size in bytes the pool can grow to.  If less than or equal
         * to zero the pool size is unlimited.
         *
         * @return the capacity
         */
        public int getCapacity();

        // -----  constants  ------------------------------------------------

        /**
         * A direct (off-heap) Java NIO ByteBuffer.
         */
        public static final int TYPE_DIRECT = 0;

        /**
         * A non-direct (on-heap) Java NIO ByteBuffer.
         */
        public static final int TYPE_HEAP = 1;
        }
    }
