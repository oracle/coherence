/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.grpc;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.internal.net.service.extend.remote.DefaultRemoteCacheServiceDependencies;
import com.tangosol.internal.tracing.TracingHelper;
import com.tangosol.internal.util.DaemonPoolDependencies;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;
import com.tangosol.io.SerializerFactory;
import com.tangosol.net.grpc.GrpcChannelDependencies;

/**
 * A default implementation of {@link RemoteGrpcServiceDependencies}.
 *
 * @author Jonathan Knight  2023.02.02
 * @since 23.03
 */
public abstract class DefaultRemoteGrpcServiceDependencies
        extends DefaultRemoteCacheServiceDependencies
        implements RemoteGrpcServiceDependencies
    {
    /**
     * Create a {@link DefaultRemoteGrpcServiceDependencies}.
     */
    protected DefaultRemoteGrpcServiceDependencies()
        {
        this(null);
        }

    /**
     * Create a {@link DefaultRemoteGrpcServiceDependencies} by copying
     * the specified {@link RemoteGrpcServiceDependencies}.
     *
     * @param deps  the {@link RemoteGrpcServiceDependencies} to copy
     */
    protected DefaultRemoteGrpcServiceDependencies(RemoteGrpcServiceDependencies deps)
        {
        super(deps);
        setChannelDependencies(getChannelDependencies());
        setSerializerFactory(getSerializerFactory());
        setEnableTracing(isTracingEnabled());
        setRemoteClusterName(getRemoteClusterName());
        setDaemonPoolDependencies(getDaemonPoolDependencies());
        }

    /**
     * Set the {@link GrpcChannelDependencies}.
     *
     * @param deps  the {@link GrpcChannelDependencies}
     */
    @Injectable("grpc-channel")
    public void setChannelDependencies(GrpcChannelDependencies deps)
        {
        m_channelDependencies = deps;
        }

    /**
     * Returns the {@link GrpcChannelDependencies}.
     *
     * @return the {@link GrpcChannelDependencies}
     */
    @Override
    public GrpcChannelDependencies getChannelDependencies()
        {
        return m_channelDependencies;
        }

    /**
     * Set whether distributed tracing is enabled.
     *
     * @param fEnableTracing an {@link Expression} to determine whether
     *                       distributed tracing is enabled
     */
    @Injectable("enable-tracing")
    public void setEnableTracing(Expression<Boolean> fEnableTracing)
        {
        m_fEnableTracing = fEnableTracing;
        }

    /**
     * Returns the {@link Expression} to determine whether distributed
     * tracing is enabled.
     *
     * @return the {@link Expression} to determine whether distributed tracing
     *         is enabled
     */
    public Expression<Boolean> isTracingEnabled()
        {
        return m_fEnableTracing;
        }

    @Override
    public DaemonPoolDependencies getDaemonPoolDependencies()
        {
        return ensureThreadPoolDependencies();
        }

    /**
     * Set the {@link DaemonPoolDependencies}.
     *
     * @param deps  the {@link DaemonPoolDependencies}
     */
    public void setDaemonPoolDependencies(DaemonPoolDependencies deps)
        {
        m_daemonPoolDependencies = new DefaultDaemonPoolDependencies(deps);
        }

    /**
     * Returns the scope name to use to obtain resources in the remote cluster.
     *
     * @return the scope name to use to obtain resources in the remote cluster
     */
    @Override
    public String getRemoteScopeName()
        {
        return m_sScopeNameRemote;
        }

    /**
     * Set the scope name to use to obtain resources in the remote cluster.
     *
     * @param sName  the scope name to use to obtain resources in the remote cluster
     */
    @Injectable("remote-scope-name")
    public void setRemoteScopeName(String sName)
        {
        m_sScopeNameRemote = sName;
        }

    /**
     * Set the remote cluster name.
     *
     * @param sName  the name of the remote cluster
     */
    @Injectable("cluster-name")
    public void setRemoteClusterName(String sName)
        {
        super.setRemoteClusterName(sName);
        }

    @Override
    public SerializerFactory getSerializerFactory()
        {
        return m_serializerFactory;
        }

    @Override
    @Injectable("serializer")
    public void setSerializerFactory(SerializerFactory factory)
        {
        m_serializerFactory = factory;
        }

    /**
     * Set the thread count.
     *
     * @param cThreads  the thread count
     */
    @Injectable("thread-count")
    public void setThreadCount(int cThreads)
        {
        ensureThreadPoolDependencies().setThreadCount(cThreads);
        }

    /**
     * Set the maximum thread count.
     *
     * @param cThreads  the maximum thread count
     */
    @Injectable("thread-count-max")
    public void setThreadCountMax(int cThreads)
        {
        ensureThreadPoolDependencies().setThreadCountMax(cThreads);
        }

    /**
     * Set the minimum thread count.
     *
     * @param cThreads  the minimum thread count
     */
    @Injectable("thread-count-min")
    public void setThreadCountMin(int cThreads)
        {
        ensureThreadPoolDependencies().setThreadCountMin(cThreads);
        }

    /**
     * Set the thread pool worker thread priority.
     *
     * @param nPriority  the thread pool worker thread priority
     */
    @Injectable("worker-priority")
    public void setThreadPriority(int nPriority)
        {
        ensureThreadPoolDependencies().setThreadPriority(nPriority);
        }

    // ----- helper methods -------------------------------------------------

    protected DefaultDaemonPoolDependencies ensureThreadPoolDependencies()
        {
        DefaultDaemonPoolDependencies deps = m_daemonPoolDependencies;
        if (deps == null)
            {
            deps = m_daemonPoolDependencies = new DefaultDaemonPoolDependencies();
            }
        return deps;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The channel dependencies.
     */
    private GrpcChannelDependencies m_channelDependencies;

    /**
     * The name of the scope to use to obtain resources in the remote cluster.
     */
    private String m_sScopeNameRemote;

    /**
     * The expression to determine whether distributed tracing is enabled.
     */
    private Expression<Boolean> m_fEnableTracing = new LiteralExpression<>(TracingHelper.isEnabled());

    /**
     * The daemon pool dependencies.
     */
    private DefaultDaemonPoolDependencies m_daemonPoolDependencies;

    /**
     * The serializer factory.
     */
    private SerializerFactory m_serializerFactory;
    }
