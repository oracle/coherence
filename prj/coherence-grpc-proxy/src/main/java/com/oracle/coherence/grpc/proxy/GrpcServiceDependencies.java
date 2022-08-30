/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.proxy;

import com.tangosol.io.NamedSerializerFactory;
import com.tangosol.io.Serializer;
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

    // ----- inner class: DefaultDependencies -------------------------------

    /**
     * The default {@link NamedCacheServiceImpl.Dependencies} implementation.
     */
    class DefaultDependencies
            implements GrpcServiceDependencies
        {
        public DefaultDependencies()
            {
            }

        public DefaultDependencies(GrpcServiceDependencies deps)
            {
            if (deps !=  null)
                {
                m_executor          = deps.getExecutor().orElse(null);
                m_registry          = deps.getRegistry().orElse(null);
                m_serializerFactory = deps.getNamedSerializerFactory().orElse(null);
                m_transferThreshold = deps.getTransferThreshold().orElse(null);
                }
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
         * @param registry  the management {@link Registry} to register
         *                  the proxy MBean with
         */
        public void setRegistry(Registry registry)
            {
            m_registry = registry;
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
         * The transfer threshold to use for paged requests.
         */
        private Long m_transferThreshold;

        /**
         * The {@link Registry} to use to register metric MBeans.
         */
        private Registry m_registry;
        }
    }
