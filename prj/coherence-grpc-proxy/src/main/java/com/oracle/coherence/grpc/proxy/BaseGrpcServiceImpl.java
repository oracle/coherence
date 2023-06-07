/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.proxy;

import com.tangosol.internal.util.DefaultDaemonPoolDependencies;
import com.tangosol.io.NamedSerializerFactory;
import com.tangosol.io.Serializer;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.internal.ScopedReferenceStore;
import com.tangosol.net.management.Registry;
import io.grpc.Status;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A base class for gRPC services.
 * <p>
 * The asynchronous processing of {@link CompletionStage}s is done using an {@link DaemonPoolExecutor}
 * so as not to consume or block threads in the Fork Join Pool. The {@link DaemonPoolExecutor} is
 * configurable so that its thread counts can be controlled.
 *
 * @author Jonathan Knight  2023.02.03
 * @since 23.03
 */
public class BaseGrpcServiceImpl
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link BaseGrpcServiceImpl}.
     *
     * @param dependencies the {@link Dependencies} to use to configure the service
     */
    public BaseGrpcServiceImpl(Dependencies dependencies, String sMBeanName, String sPoolName)
        {
        f_dependencies         = dependencies;
        f_executor             = dependencies.getExecutor().orElseGet(() -> createDefaultExecutor(sPoolName));
        f_cacheFactorySupplier = dependencies.getCacheFactorySupplier().orElse(ConfigurableCacheFactorySuppliers.DEFAULT);
        f_serializerProducer   = dependencies.getNamedSerializerFactory().orElse(NamedSerializerFactory.DEFAULT);
        f_storeSerializer      = new ScopedReferenceStore<>(Serializer.class, s -> true, Serializer::getName, s -> null);

        dependencies.getTransferThreshold().ifPresent(this::setTransferThreshold);

        DaemonPoolExecutor.DaemonPoolManagement management = f_executor instanceof DaemonPoolExecutor
                ? ((DaemonPoolExecutor) f_executor).getManagement() : null;

        Registry registry = dependencies.getRegistry().orElseGet(() -> CacheFactory.getCluster().getManagement());

        f_metrics = new GrpcProxyMetrics(sMBeanName, management);
        f_metrics.registerMBean(registry);
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Obtain the gRPC metrics instance for this service.
     *
     * @return  the gRPC metrics instance for this service
     */
    public GrpcProxyMetrics getMetrics()
        {
        return f_metrics;
        }

    /**
     * Return the transfer threshold.
     *
     * @return the {@link #transferThreshold}
     */
    long getTransferThreshold()
        {
        return transferThreshold;
        }

    /**
     * Set the transfer threshold.
     *
     * @param lSize  the new transfer threshold
     */
    void setTransferThreshold(long lSize)
        {
        transferThreshold = lSize;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create the default {@link Executor}.
     *
     * @return  the default {@link Executor}
     */
    protected static Executor createDefaultExecutor(String sName)
        {
        DefaultDaemonPoolDependencies deps = new DefaultDaemonPoolDependencies();
        deps.setName(sName);
        deps.setThreadCountMin(1);
        deps.setThreadCount(1);
        deps.setThreadCountMax(Integer.MAX_VALUE);
        DaemonPoolExecutor executor = DaemonPoolExecutor.newInstance(deps);
        executor.start();
        return executor;
        }

    protected Serializer getSerializer(String sFormatRequest, String sFormatProxy,
            Supplier<Serializer> supplierSerializer, Supplier<ClassLoader> supplierLoader)
        {
        Serializer serializer;
        if (sFormatRequest == null || sFormatRequest.trim().isEmpty() || sFormatRequest.equals(sFormatProxy))
            {
            serializer = supplierSerializer.get();
            }
        else
            {
            ClassLoader loader = supplierLoader.get();
            serializer = f_storeSerializer.get(sFormatRequest, loader);

            if (serializer == null)
                {
                serializer = f_dependencies.getContext()
                        .map(c -> c.getNamedSerializer(sFormatRequest))
                        .orElse(null);
                }
            if (serializer == null)
                {
                serializer = f_serializerProducer.getNamedSerializer(sFormatRequest, loader);
                }
            if (serializer != null)
                {
                f_storeSerializer.put(serializer, loader);
                }
            }

        if (serializer == null)
            {
            throw Status.INVALID_ARGUMENT
                    .withDescription("invalid request format, cannot find serializer with name '" + sFormatRequest + "'")
                    .asRuntimeException();
            }
        return serializer;
        }

    // ----- inner interface: Dependencies ----------------------------------

    /**
     * The dependencies to configure a {@link BaseGrpcServiceImpl}.
     */
    public interface Dependencies
            extends GrpcServiceDependencies
        {
        /**
         * Return the function to use to obtain named {@link ConfigurableCacheFactory} instances.
         *
         * @return the function to use to obtain named {@link ConfigurableCacheFactory} instances
         */
        Optional<Function<String, ConfigurableCacheFactory>> getCacheFactorySupplier();
        }

    // ----- inner class: DefaultDependencies -------------------------------

    /**
     * The default {@link Dependencies} implementation.
     */
    public static class DefaultDependencies
            extends GrpcServiceDependencies.DefaultDependencies
            implements Dependencies
        {
        public DefaultDependencies()
            {
            }

        public DefaultDependencies(GrpcServiceDependencies deps)
            {
            super(deps);
            }

        public DefaultDependencies(Dependencies deps)
            {
            super(deps);
            if (deps != null)
                {
                m_ccfSupplier = deps.getCacheFactorySupplier().orElse(null);
                }
            }

        @Override
        public Optional<Function<String, ConfigurableCacheFactory>> getCacheFactorySupplier()
            {
            return Optional.ofNullable(m_ccfSupplier);
            }

        /**
         * Set the function to use to obtain named {@link ConfigurableCacheFactory} instances.
         *
         * @param ccfSupplier the function to use to obtain named {@link ConfigurableCacheFactory} instances
         */
        public void setConfigurableCacheFactorySupplier(Function<String, ConfigurableCacheFactory> ccfSupplier)
            {
            m_ccfSupplier = ccfSupplier;
            }

        // ----- data members -----------------------------------------------

        /**
         * The supplier of the {@link ConfigurableCacheFactory} to use.
         */
        private Function<String, ConfigurableCacheFactory> m_ccfSupplier;
        }

    // ----- constants --------------------------------------------------

    /**
     * A {@link Void} value to make it obvious the return value in Void methods.
     */
    protected static final Void VOID = null;

    /**
     * The default transfer threshold.
     */
    public static final long DEFAULT_TRANSFER_THRESHOLD = 524288L;

    // ----- data members -----------------------------------------------

    /**
     * The service {@link Dependencies}.
     */
    protected final Dependencies f_dependencies;

    /**
     * The function used to obtain ConfigurableCacheFactory instances for a
     * given scope name.
     */
    protected final Function<String, ConfigurableCacheFactory> f_cacheFactorySupplier;

    /**
     * The factory to use to lookup named {@link Serializer} instances.
     */
    protected final NamedSerializerFactory f_serializerProducer;

    /**
     * The {@link Executor} to use to hand off asynchronous tasks.
     */
    protected final Executor f_executor;

    /**
     * The proxy service metrics.
     */
    protected final GrpcProxyMetrics f_metrics;

    /**
     * The serializers store.
     */
    private final ScopedReferenceStore<Serializer> f_storeSerializer;

    /**
     * The transfer threshold used for paged requests.
     */
    protected long transferThreshold = DEFAULT_TRANSFER_THRESHOLD;
    }
