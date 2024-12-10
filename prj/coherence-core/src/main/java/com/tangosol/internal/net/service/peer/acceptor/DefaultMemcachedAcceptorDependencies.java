/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.oracle.coherence.common.net.TcpSocketProvider;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.coherence.memcached.server.MemcachedServer;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.net.AddressProvider;

/**
 * The DefaultMemcachedAcceptorDependencies class provides a default implementation of
 * MemcachedAcceptorDependencies.
 *
 * @author bb 2013.05.01
 *
 * @since Coherence 12.1.3
 */
public class DefaultMemcachedAcceptorDependencies
        extends AbstractAcceptorDependencies
        implements MemcachedAcceptorDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a DefaultMemcachedAcceptorDependencies object.
     */
    public DefaultMemcachedAcceptorDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultMemcachedAcceptorDependencies object, copying the values from the
     * specified MemcachedAcceptorDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultMemcachedAcceptorDependencies(MemcachedAcceptorDependencies deps)
        {
        super(deps);

        if (deps != null)
            {
            m_sAuthMethod           = deps.getAuthMethod();
            m_fBinaryPassthru       = deps.isBinaryPassThru();
            m_bldrAddressProvider   = deps.getAddressProviderBuilder();
            m_sCacheName            = deps.getCacheName();
            m_memcachedServer       = deps.getMemcachedServer();
            m_builderSocketProvider = deps.getSocketProviderBuilder();
            }
        }

    // ----- DefaultMemcachedAcceptorDependencies methods -------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public MemcachedServer getMemcachedServer()
        {
        if (m_memcachedServer == null)
            {
            m_memcachedServer = new MemcachedServer();
            }

        return m_memcachedServer;
        }

    /**
     * Set the Memcached server.
     *
     * @param server  the Memcached server
     */
    @Injectable("memcached-server")
    public void setMemcachedServer(MemcachedServer server)
        {
        m_memcachedServer = server;
        }

    /**
     * Set binary-pass-thru.
     *
     * @param  fBinaryPassthru
     */
    @Injectable("interop-enabled")
    public void setBinaryPassThru(boolean fBinaryPassthru)
        {
        m_fBinaryPassthru = fBinaryPassthru;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isBinaryPassThru()
        {
        return m_fBinaryPassthru;
        }

    /**
     * Set the cache name.
     *
     * @param sCacheName  the cache name
     */
    @Injectable("cache-name")
    public void setCacheName(String sCacheName)
        {
        m_sCacheName = sCacheName;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCacheName()
        {
        return m_sCacheName;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAuthMethod()
        {
        return m_sAuthMethod;
        }

    /**
     * Set the client authentication method.
     *
     * @param sMethod  the client authentication method
     */
    @Injectable("memcached-auth-method")
    public void setAuthMethod(String sMethod)
        {
        m_sAuthMethod = sMethod;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketProviderBuilder getSocketProviderBuilder()
        {
        return m_builderSocketProvider;
        }

    /**
     * Set the SocketProviderBuilder that may be used by the MemcachedServer to create
     * a SocketProvider to open ServerSocketChannels.
     *
     * @param builder  the socket provider builder
     */
    @Injectable("socket-provider")
    public void setSocketProviderBuilder(SocketProviderBuilder builder)
        {
        m_builderSocketProvider = builder;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParameterizedBuilder<AddressProvider> getAddressProviderBuilder()
        {
        return m_bldrAddressProvider;
        }

    /**
     * Set the Address provider.
     *
     * @param provider  the address provider
     */
    @Injectable("address-provider")
    public void setAddressProviderBuilder(ParameterizedBuilder<AddressProvider> provider)
        {
        m_bldrAddressProvider = provider;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return super.toString() + "MemcachedAcceptor: SocketProviderBuilder=" + getSocketProviderBuilder()
               + ", LocalAddressProvider=" + getAddressProviderBuilder() + "}";
        }

    // ----- data fields and constants --------------------------------------

    /**
     * The authentication method.
     */
    private String m_sAuthMethod = "none";

    /**
     * Binary Pass-thru enabled flag.
     */
    private boolean m_fBinaryPassthru;

    /**
     * The {@link AddressProvider} builder.
     */
    private ParameterizedBuilder<AddressProvider> m_bldrAddressProvider;

    /**
     * The cache name.
     */
    private String m_sCacheName = "";

    /**
     * The Memcached server.
     */
    private MemcachedServer m_memcachedServer;

    /**
     * The SocketProviderBuilder.
     */
    private SocketProviderBuilder m_builderSocketProvider = new SocketProviderBuilder(TcpSocketProvider.DEMULTIPLEXED, true);
    }