/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.proxy.common;

import com.oracle.coherence.grpc.GrpcService;
import com.tangosol.application.Context;
import com.tangosol.internal.util.DaemonPool;
import com.tangosol.io.NamedSerializerFactory;
import com.tangosol.io.Serializer;
import com.tangosol.net.grpc.GrpcDependencies;
import com.tangosol.net.management.Registry;

import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * The dependencies for a gRPC bindable service.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public interface GrpcServiceDependencies
        extends GrpcService.Dependencies
    {
    /**
     * Return the {@link NamedSerializerFactory}.
     *
     * @return the {@link NamedSerializerFactory}
     */
    Optional<NamedSerializerFactory> getNamedSerializerFactory();

    /**
     * Return the {@link Executor}.
     *
     * @return the {@link Executor}
     */
    Optional<Executor> getExecutor();

    /**
     * Return the {@link DaemonPool}.
     *
     * @return the {@link DaemonPool}
     */
    Optional<DaemonPool> getDaemonPool();

    /**
     * Return the transfer threshold.
     *
     * @return the transfer threshold
     */
    Optional<Long> getTransferThreshold();

    /**
     * Return the optional management {@link Registry} to register
     * the proxy MBean with.
     *
     * @return the optional management {@link Registry} to register
     * the proxy MBean with
     */
    Optional<Registry> getRegistry();

    /**
     * Return the optional application {@link Context}.
     *
     * @return the optional application {@link Context}
     */
    @Override
    Optional<Context> getContext();

    /**
     * Return the type of the gRPC server.
     *
     * @return the type of the gRPC server
     */
    GrpcDependencies.ServerType getServerType();

    // ----- inner class: DefaultDependencies -------------------------------

    /**
     * The default {@link GrpcServiceDependencies} implementation.
     */
    class DefaultDependencies
            implements GrpcServiceDependencies
        {
        /**
         * Create a {@link DefaultDependencies}.
         *
         * @param serverType the type of the gRPC server
         */
        public DefaultDependencies(GrpcDependencies.ServerType serverType)
            {
            m_serverType = serverType;
            }

        /**
         * Create a {@link DefaultDependencies}.
         *
         * @param deps the dependencies to copy
         */
        public DefaultDependencies(GrpcServiceDependencies deps)
            {
            m_serverType = deps.getServerType();
            deps.getExecutor().ifPresent(this::setExecutor);
            deps.getRegistry().ifPresent(this::setRegistry);
            deps.getNamedSerializerFactory().ifPresent(this::setSerializerFactory);
            deps.getTransferThreshold().ifPresent(this::setTransferThreshold);
            deps.getContext().ifPresent(this::setContext);
            deps.getDaemonPool().ifPresent(this::setDaemonPool);
            }

        @Override
        public Optional<NamedSerializerFactory> getNamedSerializerFactory()
            {
            return Optional.ofNullable(m_serializerFactory);
            }

        /**
         * Set the {@link NamedSerializerFactory}.
         *
         * @param serializerFactory the {@link NamedSerializerFactory}
         */
        public void setSerializerFactory(NamedSerializerFactory serializerFactory)
            {
            m_serializerFactory = serializerFactory;
            }

        @Override
        public Optional<Executor> getExecutor()
            {
            return Optional.ofNullable(m_executor);
            }

        /**
         * Set the {@link Executor}.
         *
         * @param executor the {@link Executor}
         */
        public void setExecutor(Executor executor)
            {
            m_executor = executor;
            }

        @Override
        public Optional<DaemonPool> getDaemonPool()
            {
            return Optional.ofNullable(m_pool);
            }

        /**
         * Set the {@link DaemonPool}.
         *
         * @param pool the {@link DaemonPool}
         */
        public void setDaemonPool(DaemonPool pool)
            {
            m_pool = pool;
            }

        @Override
        public Optional<Long> getTransferThreshold()
            {
            return Optional.ofNullable(m_transferThreshold);
            }

        /**
         * Set the transfer threshold.
         *
         * @param transferThreshold the transfer threshold
         */
        public void setTransferThreshold(Long transferThreshold)
            {
            m_transferThreshold = transferThreshold;
            }

        @Override
        public Optional<Registry> getRegistry()
            {
            return Optional.ofNullable(m_registry);
            }

        /**
         * Set the management {@link Registry} to register the proxy MBean with.
         *
         * @param registry the management {@link Registry} to register
         *                 the proxy MBean with
         */
        public void setRegistry(Registry registry)
            {
            m_registry = registry;
            }

        @Override
        public Optional<Context> getContext()
            {
            return Optional.ofNullable(m_context);
            }

        /**
         * Set the {@link Context}.
         *
         * @param context the {@link Context}
         */
        public void setContext(Context context)
            {
            m_context = context;
            }

        @Override
        public GrpcDependencies.ServerType getServerType()
            {
            return m_serverType;
            }

        // ----- data members -----------------------------------------------

        /**
         * A factory to produce {@link Serializer} instances.
         */
        private NamedSerializerFactory m_serializerFactory;

        /**
         * The {@link Executor} to use for async operations.
         */
        private Executor m_executor;

        /**
         * The {@link DaemonPool} to use for async operations.
         */
        private DaemonPool m_pool;

        /**
         * The transfer threshold to use for paged requests.
         */
        private Long m_transferThreshold;

        /**
         * The {@link Registry} to use to register metric MBeans.
         */
        private Registry m_registry;

        /**
         * The {@link Context}.
         */
        private Context m_context;

        /**
         * The type of gRPC server the service will be deployed into.
         */
        private final GrpcDependencies.ServerType m_serverType;
        }
    }
