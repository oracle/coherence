/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.tangosol.coherence.config.ParameterList;

import com.tangosol.coherence.config.builder.AddressProviderBuilder;
import com.tangosol.coherence.config.builder.LocalAddressProviderBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.coherence.config.builder.WrapperSocketAddressProviderBuilder;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.AddressProvider;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.NameService;
import com.tangosol.net.OperationalContext;
import com.tangosol.net.SocketAddressProvider;
import com.tangosol.net.SocketOptions;
import com.tangosol.net.SocketProviderFactory;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;

import java.net.InetAddress;
import java.net.SocketException;

/**
 * The DefaultTcpAcceptorDependencies class provides a default implementation of
 * TcpAcceptorDependencies.
 *
 * @author pfm 2011.06.27
 * @since 12.1.2
 */
@SuppressWarnings("rawtypes")
public class DefaultTcpAcceptorDependencies
        extends AbstractAcceptorDependencies
        implements TcpAcceptorDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a DefaultTcpAcceptorDependencies object.
     */
    public DefaultTcpAcceptorDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultTcpAcceptorDependencies object, copying the values from the
     * specified TcpAcceptorDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultTcpAcceptorDependencies(TcpAcceptorDependencies deps)
        {
        super(deps);

        if (deps != null)
            {
            m_bldrAddressProviderLocal = deps.getLocalAddressProviderBuilder();
            m_bldrFilterAuthorizedHost = deps.getAuthorizedHostFilterBuilder();
            m_cbDefaultLimitBytes      = deps.getDefaultLimitBytes();
            m_cDefaultLimitMessages    = deps.getDefaultLimitMessages();
            m_cbDefaultNominalBytes    = deps.getDefaultNominalBytes();
            m_cDefaultNominalMessages  = deps.getDefaultNominalMessages();
            m_cbDefaultSuspectBytes    = deps.getDefaultSuspectBytes();
            m_cDefaultSuspectMessages  = deps.getDefaultSuspectMessages();
            m_bufferPoolConfigIncoming = deps.getIncomingBufferPoolConfig();
            m_cListenBacklog           = deps.getListenBacklog();
            m_bufferPoolConfigOutgoing = deps.getOutgoingBufferPoolConfig();
            m_socketOptions            = deps.getSocketOptions();
            m_builderSocketProvider    = deps.getSocketProviderBuilder();
            m_fSuspectProtocolEnabled  = deps.isSuspectProtocolEnabled();
            }
        }

    // ----- DefaultTcpAcceptorDependencies methods -------------------------

    /**
     * Sets the {@link OperationalContext} in which the
     * {@link TcpAcceptorDependencies} are operating. These are
     * required to determine default values when they are not injected.
     *
     * @param ctxOperational  the {@link OperationalContext}
     */
    @Injectable("com.tangosol.net.OperationalContext")
    public void setOperationalContext(OperationalContext ctxOperational)
        {
        m_ctxOperational = ctxOperational;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParameterizedBuilder<Filter> getAuthorizedHostFilterBuilder()
        {
        return m_bldrFilterAuthorizedHost;
        }

    /**
     * Set the authorized host filter builder.
     *
     * @param builder  the authorized host filter builder
     */
    @Injectable("authorized-hosts")
    public void setAuthorizedHostFilterBuilder(ParameterizedBuilder<Filter> builder)
        {
        m_bldrFilterAuthorizedHost = builder;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDefaultLimitBytes()
        {
        return m_cbDefaultLimitBytes;
        }

    /**
     * Set the default backlog limit in bytes.
     *
     * @param cbSize  the backlog limit in bytes
     */
    @Injectable("limit-buffer-size")
    public void setDefaultLimitBytes(long cbSize)
        {
        m_cbDefaultLimitBytes = cbSize;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDefaultLimitMessages()
        {
        return m_cDefaultLimitMessages;
        }

    /**
     * Set the default backlog limit in messages.
     *
     * @param cMessages  the backlog limit in messages
     */
    @Injectable("limit-buffer-length")
    public void setDefaultLimitMessages(int cMessages)
        {
        m_cDefaultLimitMessages = cMessages;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDefaultNominalBytes()
        {
        return m_cbDefaultNominalBytes;
        }

    /**
     * Set the default nominal backlog size in bytes.
     *
     * @param cbSize  the backlog size in bytes
     */
    @Injectable("nominal-buffer-size")
    public void setDefaultNominalBytes(long cbSize)
        {
        m_cbDefaultNominalBytes = cbSize;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDefaultNominalMessages()
        {
        return m_cDefaultNominalMessages;
        }

    /**
     * Set the default nominal backlog size in messages.
     *
     * @param cMessages  the backlog size in messages
     */
    @Injectable("nominal-buffer-length")
    public void setDefaultNominalMessages(int cMessages)
        {
        m_cDefaultNominalMessages = cMessages;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDefaultSuspectBytes()
        {
        return m_cbDefaultSuspectBytes;
        }

    /**
     * Set the default suspect backlog size in bytes.
     *
     * @param cbSize  the backlog size in bytes
     */
    @Injectable("suspect-buffer-size")
    public void setDefaultSuspectBytes(long cbSize)
        {
        m_cbDefaultSuspectBytes = cbSize;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDefaultSuspectMessages()
        {
        return m_cDefaultSuspectMessages;
        }

    /**
     * Set the default suspect backlog size in messages.
     *
     * @param cMessages  the backlog size in messages
     */
    @Injectable("suspect-buffer-length")
    public void setDefaultSuspectMessages(int cMessages)
        {
        m_cDefaultSuspectMessages = cMessages;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferPoolConfig getIncomingBufferPoolConfig()
        {
        BufferPoolConfig config = m_bufferPoolConfigIncoming;

        if (config == null)
            {
            m_bufferPoolConfigIncoming = config = new PoolConfig();
            }

        return config;
        }

    /**
     * Set the incoming BufferPoolConfig.
     *
     * @param config  the BufferPoolConfig
     */
    @Injectable("incoming-buffer-pool")
    public void setIncomingBufferPoolConfig(BufferPoolConfig config)
        {
        m_bufferPoolConfigIncoming = config;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getListenBacklog()
        {
        return m_cListenBacklog;
        }

    /**
     * Set the listen backlog.
     *
     * @param cConnections  the listen backlog
     */
    @Injectable("listen-backlog")
    public void setListenBacklog(int cConnections)
        {
        m_cListenBacklog = cConnections;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParameterizedBuilder<SocketAddressProvider> getLocalAddressProviderBuilder()
        {
        ParameterizedBuilder<SocketAddressProvider> bldrSocketAddressProvider = m_bldrAddressProviderLocal;
        if (bldrSocketAddressProvider == null)
            {
            // m_bldrAddressProviderLocal may be accessed by multiple threads
            // concurrently as services are starting and looking up the
            // NameService's AddressProvider
            synchronized (this)
                {
                bldrSocketAddressProvider = m_bldrAddressProviderLocal;
                if (bldrSocketAddressProvider == null)
                    {
                    int nPort = getDefaultPort();
                    bldrSocketAddressProvider = m_bldrAddressProviderLocal =
                        new WrapperSocketAddressProviderBuilder(
                            new LocalAddressProviderBuilder(getDefaultHost(), nPort, nPort))
                            .setEphemeral(true);
                    }
                }
            }
        return bldrSocketAddressProvider;
        }

    /**
     * Set the local AddressProvider builder
     *
     * @param bldr  the local AddressProvider builder
     */
    @Injectable("address-provider")
    public void setLocalAddressProviderBuilder(AddressProviderBuilder bldr)
        {
        if (bldr != null)
            {
            boolean fSubPortEphemeral = false;
            if (bldr instanceof LocalAddressProviderBuilder)
                {
                LocalAddressProviderBuilder lBldr = (LocalAddressProviderBuilder) bldr;

                fSubPortEphemeral = lBldr.getPortMinOriginal() == -1;

                // set host and port at realize() time if needed
                bldr = new AddressProviderBuilder()
                    {
                    @Override
                    public AddressProvider createAddressProvider(ClassLoader loader)
                        {
                        return realize(null, loader, null);
                        }

                    @Override
                    public AddressProvider realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
                        {
                        LocalAddressProviderBuilder lBldr = m_LBldr;
                        InetAddress addr = lBldr.resolveAddress();
                        if (addr == null)
                            {
                            lBldr.setAddress(getDefaultHost());
                            }
                        int nPort = lBldr.getPortMin();
                        if (nPort == -1)
                            {
                            nPort = getDefaultPort();
                            lBldr.setPortMin(nPort).setPortMax(nPort);
                            }
                        return m_LBldr.realize(resolver, loader, listParameters);
                        }

                    private final LocalAddressProviderBuilder m_LBldr = lBldr;
                    };
                }

            setLocalSocketAddressProviderBuilder(new WrapperSocketAddressProviderBuilder(bldr)
                .setEphemeral(fSubPortEphemeral));
            }
        }
    /**
     * Set the local SocketAddressProvider builder.
     *
     * @param bldr  the local AddressProvider builder
     */
    public void setLocalSocketAddressProviderBuilder(ParameterizedBuilder<SocketAddressProvider> bldr)
        {
        m_bldrAddressProviderLocal = bldr;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferPoolConfig getOutgoingBufferPoolConfig()
        {
        BufferPoolConfig config = m_bufferPoolConfigOutgoing;

        if (config == null)
            {
            m_bufferPoolConfigOutgoing = config = new PoolConfig();
            }

        return config;
        }

    /**
     * Set the outgoing BufferPoolConfig.
     *
     * @param config  the BufferPoolConfig
     */
    @Injectable("outgoing-buffer-pool")
    public void setOutgoingBufferPoolConfig(BufferPoolConfig config)
        {
        m_bufferPoolConfigOutgoing = config;
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
        if (m_builderSocketProvider == null)
            {
            m_builderSocketProvider = createDefaultSocketProviderBuilder();
            }
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
    public boolean isSuspectProtocolEnabled()
        {
        return m_fSuspectProtocolEnabled;
        }

    /**
     * Set the suspect protocol enabled flag.
     *
     * @param fEnabled  true if the suspect protocol is enabled
     */
    @Injectable("suspect-protocol-enabled")
    public void setSuspectProtocolEnabled(boolean fEnabled)
        {
        m_fSuspectProtocolEnabled = fEnabled;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultTcpAcceptorDependencies validate()
        {
        super.validate();

        return this;
        }

    /**
     * Return {@code true} if these dependencies can be configured to use the global socket provider builder.
     *
     * @return {@code true} if these dependencies can be configured to use the global socket provider builder
     */
    public boolean canUseGlobalSocketProvider()
        {
        return true;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return super.toString() + "{AuthorizedHostFilterBuilder=" + getAuthorizedHostFilterBuilder() + ", DefaultLimitBytes="
               + getDefaultLimitBytes() + ", DefaultLimitMessages=" + getDefaultLimitMessages()
               + ", DefaultNominalBytes=" + getDefaultNominalBytes() + ", DefaultNominalMessages="
               + getDefaultNominalMessages() + ", DefaultSuspectBytes=" + getDefaultSuspectBytes()
               + ", DefaultSuspectMessages=" + getDefaultSuspectMessages() + ", IncomingBufferPoolConfig="
               + getIncomingBufferPoolConfig() + ", ListenBacklog=" + getListenBacklog() + ", LocalAddressProvider="
               + getLocalAddressProviderBuilder() + ", OutgoingBufferPoolConfig=" + getOutgoingBufferPoolConfig()
               + ", SocketOptions=" + getSocketOptions() + ", SocketProviderBuilder=" + getSocketProviderBuilder()
               + ", SuspectProtocolEnabled=" + isSuspectProtocolEnabled() + "}";
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create the default {@link SocketProviderBuilder} to use if one has not been set.
     *
     * @return the default {@link SocketProviderBuilder} to use if one has not been set
     */
    protected SocketProviderBuilder createDefaultSocketProviderBuilder()
        {
        return new SocketProviderBuilder(SocketProviderFactory.DEFAULT_SOCKET_PROVIDER, true);
        }

    /**
     * Get the {@link OperationalContext} in which the
     * {@link TcpAcceptorDependencies} are operating. If the
     * OperationalContext has not been set, then obtain it via
     * {@link CacheFactory#getCluster()}.
     *
     * @return the OperationalContext in which the TcpAcceptorDependencies
     *         are operating
     */
    protected OperationalContext ensureOperationalContext()
        {
        OperationalContext ctxOperational = m_ctxOperational;
        if (ctxOperational == null)
            {
            // CODI did not inject the OperationContext so get it now. This happens with empty <acceptor-config>.
            ctxOperational = m_ctxOperational = (OperationalContext) CacheFactory.getCluster();
            }
        return ctxOperational;
        }

    /**
     * Return the default host to use if no host is specified in the configuration.
     *
     * @return the default host
     *
     * @since 12.2.1
     */
    protected InetAddress getDefaultHost()
        {
        OperationalContext ctx = ensureOperationalContext();

        if (ctx instanceof Cluster)
            {
            NameService service = ((Cluster) ctx).getResourceRegistry().getResource(NameService.class);
            if (service != null)
                {
                // default to NameService InetAddress
                return service.getLocalAddress();
                }
            }

        return ctx.getLocalMember().getAddress();
        }

    /**
     * Return the default port number to use if no port is specified in the configuration.
     *
     * @return the default port
     *
     * @since 12.2.1
     */
    protected int getDefaultPort()
        {
        return ensureOperationalContext().getLocalTcpPort();
        }

    // ----- inner classes --------------------------------------------------

    /**
     * The PoolConfig class is a default implementation of BufferPoolConfig.
     */
    public static class PoolConfig
            implements BufferPoolConfig
        {
        /**
         * {@inheritDoc}
         */
        @Override
        public int getBufferSize()
            {
            return m_cbBufferSize;
            }

        /**
         * Set the buffer size in bytes.
         *
         * @param cbSize  the buffer size
         *
         * @return this object
         */
        @Injectable
        public PoolConfig setBufferSize(int cbSize)
            {
            m_cbBufferSize = cbSize;

            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getBufferType()
            {
            return m_nBufferType;
            }

        /**
         * Set the buffer type.
         *
         * @param nType  the buffer type; either TYPE_DIRECT or TYPE_HEAP
         *
         * @return this object
         */
        @Injectable
        public PoolConfig setBufferType(int nType)
            {
            m_nBufferType = nType;

            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getCapacity()
            {
            return m_cbCapacity;
            }

        /**
         * Set the buffer pool capacity in bytes.
         *
         * @param cbCapacity  the capacity in bytes
         *
         * @return this object
         */
        @Injectable
        public PoolConfig setCapacity(int cbCapacity)
            {
            m_cbCapacity = cbCapacity;

            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
            {
            return "BufferPoolConfig" + "{BufferSize=" + getBufferSize() + ", BufferType="
                   + (getBufferType() == TYPE_DIRECT ? "direct" : "heap") + ", Capacity=" + getCapacity() + "}";
            }

        // ----- data fields and constants ----------------------------------

        /**
         * The buffer size.
         */
        private int m_cbBufferSize = 2048;

        /**
         * The buffer type.
         */
        private int m_nBufferType = TYPE_DIRECT;

        /**
         * The buffer pool capacity.
         */
        private int m_cbCapacity;
        }

    // ----- data fields and constants --------------------------------------

    /**
     * The local SocketAddressProvider builder
     */
    private volatile ParameterizedBuilder<SocketAddressProvider> m_bldrAddressProviderLocal;

    /**
     * The incoming buffer pool configuration.
     */
    private BufferPoolConfig m_bufferPoolConfigIncoming;

    /**
     * The outgoing buffer pool configuration.
     */
    private BufferPoolConfig m_bufferPoolConfigOutgoing;

    /**
     * The default backlog limit in bytes.
     */
    private long m_cbDefaultLimitBytes = 100000000;

    /**
     * The default backlog limit in messages.
     */
    private int m_cDefaultLimitMessages = 60000;

    /**
     * The default nominal backlog size in bytes.
     */
    private long m_cbDefaultNominalBytes = 2000000;

    /**
     * The default nominal backlog size in messages.
     */
    private int m_cDefaultNominalMessages = 2000;

    /**
     * The default suspect backlog size in bytes.
     */
    private long m_cbDefaultSuspectBytes = 10000000;

    /**
     * The default suspect backlog size in messages.
     */
    private int m_cDefaultSuspectMessages = 10000;

    /**
     * The authorized host filter builder.
     */
    private ParameterizedBuilder<Filter> m_bldrFilterAuthorizedHost;

    /**
     * The listen backlog in connections.
     */
    private int m_cListenBacklog;

    /**
     * The SocketOptions.
     */
    private SocketOptions m_socketOptions;

    /**
     * The SocketProviderBuilder.
     */
    private SocketProviderBuilder m_builderSocketProvider;

    /**
     * The suspect protocol enabled flag.
     */
    private boolean m_fSuspectProtocolEnabled = true;

    /**
     * The {@link OperationalContext} in which the {@link TcpAcceptorDependencies}
     * are operating.
     */
    private OperationalContext m_ctxOperational;
    }
