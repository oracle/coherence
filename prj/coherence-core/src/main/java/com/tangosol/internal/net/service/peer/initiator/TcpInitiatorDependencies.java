/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.initiator;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.net.SocketAddressProvider;
import com.tangosol.net.SocketOptions;

import java.net.SocketAddress;

/**
* The TcpInitiatorDependencies interface provides a TcpInitiator object with its external
* dependencies.
*
* @author pfm  2011.06.27
* @since Coherence 12.1.2
*/
public interface TcpInitiatorDependencies
        extends InitiatorDependencies
    {
    /**
     * Return the local SocketAddress that all Socket objects created by
     * this TcpInitiator will be bound to. If null, a SocketAddress created from an
     * ephemeral port and a valid local address will be used.
     *
     * @return the local SocketAddress
     */
    public SocketAddress getLocalAddress();

    /**
     * Return the AddressProvider builder used by the TcpInitiator to obtain the address or
     * addresses of the remote TcpAcceptor(s) that it will connect to.
     *
     * @return the remote AddressProvider
     */
    public ParameterizedBuilder<SocketAddressProvider> getRemoteAddressProviderBuilder();

    /**
     * Return the SocketOptions that the TcpInitiator will use when establishing socket
     * connections.
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
     * Return whether each remote AddressProvider address points to a
     * NameService which can be used to look up the remote address of the
     * ProxyService.
     *
     * @return whether each remote AddressProvider address points to a
     *         NameService which can be used to look up the remote address of the
     *         ProxyService
     */
    public boolean isNameServiceAddressProvider();
    }
