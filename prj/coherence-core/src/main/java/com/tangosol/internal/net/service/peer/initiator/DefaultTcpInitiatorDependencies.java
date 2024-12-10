/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.initiator;

import com.tangosol.coherence.config.builder.AddressProviderBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.coherence.config.builder.WrapperSocketAddressProviderBuilder;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.net.AddressProvider;
import com.tangosol.net.SocketAddressProvider;
import com.tangosol.net.SocketOptions;
import com.tangosol.net.SocketProviderFactory;

import com.tangosol.util.Base;

import java.net.SocketAddress;
import java.net.SocketException;

/**
 * The DefaultTcpInitiatorDependencies class provides a default implementation of
 * TcpInitiatorDependencies.
 *
 * @author pfm 2011.06.27
 * @since Coherence 12.1.2
 */
public class DefaultTcpInitiatorDependencies
        extends DefaultInitiatorDependencies
        implements TcpInitiatorDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a DefaultTcpInitiatorDependencies object.
     */
    public DefaultTcpInitiatorDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultTcpInitiatorDependencies object, copying the values from the
     * specified TcpInitiatorDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultTcpInitiatorDependencies(TcpInitiatorDependencies deps)
        {
        super(deps);

        if (deps != null)
            {
            m_localAddress                = deps.getLocalAddress();
            m_bldrAddressProviderRemote   = deps.getRemoteAddressProviderBuilder();
            m_socketOptions               = deps.getSocketOptions();
            m_builderSocketProvider       = deps.getSocketProviderBuilder();
            m_fNameServiceAddressProvider = deps.isNameServiceAddressProvider();
            }
        }

    // ----- DefaultTcpInitiatorDependencies methods ------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketAddress getLocalAddress()
        {
        return m_localAddress;
        }

    /**
     * Set the local InetSocketAddress.
     *
     * @param address  the local InetSocketAddress
     */
    @Injectable("local-address")
    public void setLocalAddress(SocketAddress address)
        {
        m_localAddress = address;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParameterizedBuilder<SocketAddressProvider> getRemoteAddressProviderBuilder()
        {
        return m_bldrAddressProviderRemote;
        }

    /**
     * Set the remote AddressProvider builder.
     * <p>
     * After calling this method, {@link #isNameServiceAddressProvider()} will return <code>false</code>.
     *
     * @param bldr  the remote AddressProvider builder
     */
    @Injectable("remote-addresses")
    public void setRemoteAddressProviderBuilder(AddressProviderBuilder bldr)
        {
        if (bldr == null)
            {
            setRemoteSocketAddressProviderBuilder(null);
            }
        else
            {
            setRemoteSocketAddressProviderBuilder(new WrapperSocketAddressProviderBuilder(bldr));
            }
        }

    /**
     * Set the remote SocketAddressProvider builder.
     * <p>
     * After calling this method, {@link #isNameServiceAddressProvider()} will return <code>false</code>.
     *
     * @param bldr  the remote AddressProvider builder
     */
    public void setRemoteSocketAddressProviderBuilder(ParameterizedBuilder<SocketAddressProvider> bldr)
        {
        m_fNameServiceAddressProvider = false;
        m_bldrAddressProviderRemote = bldr;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketOptions getSocketOptions()
        {
        SocketOptions options = m_socketOptions;

        if (options == null)
            {
            m_socketOptions = options = new SocketOptions();

            try
                {
                options.setOption(SocketOptions.SO_KEEPALIVE, Boolean.TRUE);
                options.setOption(SocketOptions.TCP_NODELAY, Boolean.TRUE);
                options.setOption(SocketOptions.SO_LINGER, 0);
                }
            catch (SocketException e)
                {
                throw Base.ensureRuntimeException(e);
                }
            }

        return options;
        }

    /**
     * Set the SocketOptions.
     *
     * @param options  the SocketOptions
     */
    @Injectable
    public void setSocketOptions(SocketOptions options)
        {
        m_socketOptions = options;
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
     * Set the SocketProviderBuilder.
     *
     * @param builder  the SocketProviderBuilder
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
    public boolean isNameServiceAddressProvider()
        {
        return m_fNameServiceAddressProvider || m_bldrAddressProviderRemote == null;
        }

    /**
     * Sets the remote AddressProvider is for connections to a NameService.
     * (set to <code>null</code> to disable)
     * <p>
     * After calling this method, {@link #isNameServiceAddressProvider()} will
     * return <code>true</code> (assuming a non-null value was provided).
     *
     * @param bldr  the {@link AddressProvider} builder for the NameService
     */
    @Injectable("name-service-addresses")
    public void setNameServiceAddressProviderBuilder(AddressProviderBuilder bldr)
        {
        setRemoteAddressProviderBuilder(bldr);
        m_fNameServiceAddressProvider = bldr != null;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultTcpInitiatorDependencies validate()
        {
        super.validate();

        Base.checkNotNull(getSocketProviderBuilder(), "SocketProviderBuilder");

        return this;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return super.toString() + "{LocalAddress=" + getLocalAddress() + ", RemoteAddressProviderBldr="
               + getRemoteAddressProviderBuilder() + ", SocketOptions=" + getSocketOptions() + ", SocketProvideBuilderr="
               + getSocketProviderBuilder() + ", isNameServiceAddressProvider=" + isNameServiceAddressProvider() + "}";
        }


    // ----- data fields and constants --------------------------------------

    /**
     * The remote SocketAddressProvider builder.
     */
    private ParameterizedBuilder<SocketAddressProvider> m_bldrAddressProviderRemote;

    /**
     * The local SocketAddress.
     */
    private SocketAddress m_localAddress;

    /**
     * The SocketOptions.
     */
    private SocketOptions m_socketOptions;

    /**
     * The SocketProviderBuilder.
     */
    private SocketProviderBuilder m_builderSocketProvider = new SocketProviderBuilder(SocketProviderFactory.DEFAULT_SOCKET_PROVIDER, true);

    /**
     * Whether the remote AddressProvider is for connections to a NameService.
     */
    private boolean m_fNameServiceAddressProvider = false;
    }
