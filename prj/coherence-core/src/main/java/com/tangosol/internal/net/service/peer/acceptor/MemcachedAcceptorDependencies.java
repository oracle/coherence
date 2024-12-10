/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.coherence.memcached.server.MemcachedServer;

import com.tangosol.net.AddressProvider;
import com.tangosol.net.SocketAddressProvider;

/**
 * The MemcachedAcceptorDependencies interface provides a MemcachedAcceptor object
 * with its external dependencies.
 *
 * @author bb 2013.05.01
 *
 * @since Coherence 12.1.3
 */
public interface MemcachedAcceptorDependencies
        extends AcceptorDependencies
    {
    /**
     * Return the MemcachedServer.
     *
     * @return the MemcachedServer
     */
    MemcachedServer getMemcachedServer();

    /**
     * Return the cache-name used by the memcached acceptor.
     *
     * @return cache name
     */
    String getCacheName();

    /**
     * Return if binary pass-thru enabled for the memcached acceptor.
     *
     * @return true iff binary pass thru enabled
     */
    boolean isBinaryPassThru();

    /**
     * Return the authentication mechanism used by the memcached acceptor.
     * <p>
     * Valid values <b>PLAIN</b> for SASL PLAIN mechanism, and <b>NONE</b> for
     * no authentication.
     *
     * @return the authentication mechanism
     */
    String getAuthMethod();

    /**
     * Return the SocketProviderBuilder that may be used by the HttpAcceptor to create a
     * SocketProvider to open ServerSocketChannels.
     *
     * @return the {@link SocketProviderBuilder}
     */
    SocketProviderBuilder getSocketProviderBuilder();

    /**
     * Return the {@link SocketAddressProvider} builder.
     *
     * @return the SocketAddressProvider builder
     */
    ParameterizedBuilder<AddressProvider> getAddressProviderBuilder();
    }
