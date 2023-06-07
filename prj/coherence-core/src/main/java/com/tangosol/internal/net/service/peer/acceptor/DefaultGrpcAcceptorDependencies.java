/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.oracle.coherence.common.net.TcpSocketProvider;
import com.tangosol.application.Context;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.net.grpc.GrpcDependencies;
import com.tangosol.net.grpc.GrpcAcceptorController;

/**
 * The default implementation of {@link GrpcAcceptorDependencies}.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public class DefaultGrpcAcceptorDependencies
        extends AbstractAcceptorDependencies
        implements GrpcAcceptorDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a {@link DefaultGrpcAcceptorDependencies} instance.
     */
    public DefaultGrpcAcceptorDependencies()
        {
        this(null);
        }

    /**
     * Construct a {@link DefaultGrpcAcceptorDependencies} instance, copying the values
     * from the specified {@link GrpcAcceptorDependencies} instance.
     *
     * @param deps  the optional {@link GrpcAcceptorDependencies} to copy
     */
    public DefaultGrpcAcceptorDependencies(GrpcAcceptorDependencies deps)
        {
        super(deps);

        if (deps != null)
            {
            setChannelzPageSize(deps.getChannelzPageSize());
            setContext(deps.getContext());
            setController(deps.getController());
            setInProcessName(deps.getInProcessName());
            setLocalAddress(deps.getLocalAddress());
            setLocalPort(deps.getLocalPort());
            setSocketProviderBuilder(deps.getSocketProviderBuilder());
            }
        }

    // ----- DefaultMemcachedAcceptorDependencies methods -------------------

    @Override
    public GrpcAcceptorController getController()
        {
        GrpcAcceptorController controller = m_controller;
        if (controller == null)
            {
            controller = m_controller = GrpcAcceptorController.discoverController();
            }
        return controller;
        }

    @Injectable("controller")
    public void setController(GrpcAcceptorController controller)
        {
        m_controller = controller;
        }

    @Override
    public SocketProviderBuilder getSocketProviderBuilder()
        {
        return m_builderSocketProvider;
        }

    /**
     * Set the {@link SocketProviderBuilder} that may be used by the gRPC server.
     *
     * @param builder  the {@link SocketProviderBuilder}
     */
    @Injectable("socket-provider")
    public void setSocketProviderBuilder(SocketProviderBuilder builder)
        {
        m_builderSocketProvider = builder;
        }

    @Override
    public int getLocalPort()
        {
        return m_nLocalPort;
        }

    /**
     * Set the local port.
     *
     * @param nPort  the local port
     */
    @Injectable("local-address/port")
    public void setLocalPort(int nPort)
        {
        m_nLocalPort = nPort;
        }

    @Override
    public String getLocalAddress()
        {
        return m_sLocalAddress;
        }

    /**
     * Set the local address.
     *
     * @param sAddress  the local address
     */
    @Injectable("local-address/address")
    public void setLocalAddress(String sAddress)
        {
        m_sLocalAddress = normalizeAddress(sAddress, getLocalPort());
        }

    @Override
    public String getInProcessName()
        {
        return m_sInProcessName;
        }

    /**
     * Set the name of the in-process gRPC server.
     *
     * @param sName  the name of the in-process gRPC server
     */
    @Injectable("in-process-name")
    public void setInProcessName(String sName)
        {
        m_sInProcessName = sName;
        }

    @Override
    public int getChannelzPageSize()
        {
        return m_nChannelzPageSize <= 0 ? GrpcDependencies.DEFAULT_CHANNELZ_MAX_PAGE_SIZE : m_nChannelzPageSize;
        }

    /**
     * Set the max page size for the Channelz service.
     *
     * @param nPageSize  the max page size for the Channelz service.
     */
    public void setChannelzPageSize(int nPageSize)
        {
        m_nChannelzPageSize = nPageSize;
        }

    /**
     * Set the application Context.
     *
     * @param context  the application Context
     */
    @Injectable
    public void setContext(Context context)
        {
        m_context = context;
        }

    @Override
    public Context getContext()
        {
        return m_context;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The gRPC server controller.
     */
    private GrpcAcceptorController m_controller;

    /**
     * The local address.
     */
    private String m_sLocalAddress = normalizeAddress(null, 0);

    /**
     * The local port.
     */
    private int m_nLocalPort = 0;

    /**
     * The SocketProviderBuilder.
     */
    private SocketProviderBuilder m_builderSocketProvider = new SocketProviderBuilder(TcpSocketProvider.DEMULTIPLEXED, true);

    /**
     * The name of the in-process gRPC server.
     */
    private String m_sInProcessName = GrpcDependencies.DEFAULT_IN_PROCESS_NAME;

    /**
     * The max page size for the Channelz service.
     */
    private int m_nChannelzPageSize;

    /**
     * An optional application {@link Context}.
     */
    private Context m_context;
    }
