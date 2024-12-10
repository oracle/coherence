/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.proxy.common;

import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.grpc.GrpcService;
import com.tangosol.application.ContainerContext;
import com.tangosol.application.Context;
import com.tangosol.coherence.config.scheme.ServiceScheme;
import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;
import com.tangosol.internal.util.collection.ConvertingNamedCache;
import com.tangosol.io.NamedSerializerFactory;
import com.tangosol.io.Serializer;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Coherence;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.NearCache;
import com.tangosol.net.grpc.GrpcDependencies;
import com.tangosol.net.internal.ScopedReferenceStore;
import com.tangosol.net.management.Registry;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.NullImplementation;
import io.grpc.Status;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
        implements GrpcService
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
     * Return the service dependencies.
     *
     * @return the service dependencies
     */
    public Dependencies getDependencies()
        {
        return f_dependencies;
        }

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
     * Return the {@link Executor} this service is using for async-requests.
     *
     * @return the {@link Executor} this service is using for async-requests
     */
    public Executor getExecutor()
        {
        return f_executor;
        }

    /**
     * Return the transfer threshold.
     *
     * @return the {@link #transferThreshold}
     */
    public long getTransferThreshold()
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
     * Return the {@link ConfigurableCacheFactory} for the specified scope name.
     *
     * @param sScope  the scope name for the {@link ConfigurableCacheFactory} to find
     *
     * @return  the {@link ConfigurableCacheFactory} with the specified scope name
     */
    @SuppressWarnings("resource")
    public ConfigurableCacheFactory getCCF(String sScope)
        {
        Optional<Context> optional         = f_dependencies.getContext();
        ContainerContext  containerContext = null;
        Context           context;
        String            sMTName;
        String            sScopeFinal;

        if (optional.isPresent())
            {
            context = optional.get();
            String sAppName = context.getApplicationName();

            containerContext = context.getContainerContext();
            sMTName          = ServiceScheme.getScopePrefix(sAppName, containerContext);

            if (sScope.isEmpty() || Objects.equals(sAppName, sScope) || Objects.equals(sMTName, sScope))
                {
                sScopeFinal = sAppName;
                }
            else
                {
                sScopeFinal = sAppName + sScope;
                }
            }
        else
            {
            sScopeFinal = sScope;
            sMTName     = null;
            context     = null;
            }

        if (containerContext != null)
            {
            ClassLoader loader = context.getClassLoader();
            Coherence coherence = Coherence.getInstances(loader)
                    .stream()
                    .filter(c -> c.getName().equals(sMTName))
                    .filter(c -> Objects.equals(context, c.getConfiguration().getApplicationContext().orElse(null)))
                    .findFirst()
                    .orElse(null);

            if (coherence == null)
                {
                String sNames = Coherence.getInstances(loader)
                        .stream()
                        .map(Coherence::getName)
                        .map(s -> Coherence.DEFAULT_NAME.equals(s) ? "<default>" : s)
                        .collect(Collectors.joining(","));

                throw new IllegalStateException("No Coherence instance exists with name " + sMTName + " scopeFinal=" + sScopeFinal + " [" + sNames + "]" );
                }

            String sScopes = coherence.getSessionScopeNames().stream()
                    .map(s -> Coherence.DEFAULT_NAME.equals(s) ? "<default>" : s)
                    .collect(Collectors.joining(","));

            return coherence.getSessionsWithScope(sScopeFinal)
                    .stream()
                    .findFirst()
                    .filter(s -> s instanceof ConfigurableCacheFactorySession)
                    .map(ConfigurableCacheFactorySession.class::cast)
                    .map(ConfigurableCacheFactorySession::getConfigurableCacheFactory)
                    .orElseThrow(() -> new IllegalStateException("cannot locate a session with scope " + sScopeFinal
                            + " Coherence instance '" + sMTName + "' contains [" + sScopes + "]"));
            }
        else
            {
            try
                {
                return f_cacheFactorySupplier.apply(sScopeFinal);
                }
            catch (Exception e)
                {
                throw Exceptions.ensureRuntimeException(e);
                }
            }
        }


    /**
     * Obtain an {@link NamedCache}.
     *
     * @param scope      the scope name to use to obtain the CCF to get the cache from
     * @param cacheName  the name of the cache
     *
     * @return the {@link NamedCache} with the specified name
     */
    protected NamedCache<Binary, Binary> getPassThroughCache(String scope, String cacheName)
        {
        return getCache(scope, cacheName, true);
        }

    /**
     * Obtain an {@link NamedCache}.
     *
     * @param sScope      the scope name to use to obtain the CCF to get the cache from
     * @param sCacheName  the name of the cache
     * @param fPassThru   {@code true} to use a binary pass-thru cache
     *
     * @return the {@link NamedCache} with the specified name
     */
    protected NamedCache<Binary, Binary> getCache(String sScope, String sCacheName, boolean fPassThru)
        {
        if (sCacheName == null || sCacheName.trim().isEmpty())
            {
            throw Status.INVALID_ARGUMENT
                    .withDescription(INVALID_CACHE_NAME_MESSAGE)
                    .asRuntimeException();
            }

        Context                  context          = f_dependencies.getContext().orElse(null);
        ContainerContext         containerContext = context == null ? null : context.getContainerContext();
        ConfigurableCacheFactory ccf              = getCCF(sScope);

        if (containerContext != null)
            {
            return containerContext.runInDomainPartitionContext(createCallable(ccf, sCacheName, fPassThru));
            }
        else
            {
            try
                {
                return createCallable(ccf, sCacheName, fPassThru).call();
                }
            catch (Exception e)
                {
                throw Exceptions.ensureRuntimeException(e);
                }
            }
        }

    @SuppressWarnings("unchecked")
    private Callable<NamedCache<Binary, Binary>> createCallable(ConfigurableCacheFactory ccf,
                                                                String sCacheName, boolean fPassThru)
        {
        return () ->
            {
            ClassLoader                loader = fPassThru ? NullImplementation.getClassLoader()
                                                          : Classes.getContextClassLoader();
            NamedCache<Binary, Binary> cache  = ccf.ensureCache(sCacheName, loader);

            // optimize front-cache out of storage enabled proxies
            boolean near = cache instanceof NearCache;
            if (near)
                {
                CacheService service = cache.getCacheService();
                if (service instanceof DistributedCacheService
                        && ((DistributedCacheService) service).isLocalStorageEnabled())
                    {
                    cache = ((NearCache<Binary, Binary>) cache).getBackCache();
                    near = false;
                    }
                }

            if (near)
                {
                return new ConvertingNamedCache(cache,
                        NullImplementation.getConverter(),
                        ExternalizableHelper.CONVERTER_STRIP_INTDECO,
                        NullImplementation.getConverter(),
                        NullImplementation.getConverter());
                }
            else
                {
                return cache;
                }
            };
        }

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
            serializer = getSerializer(sFormatRequest, loader);
            }

        if (serializer == null)
            {
            throw Status.INVALID_ARGUMENT
                    .withDescription("invalid request format, cannot find serializer with name '" + sFormatRequest + "'")
                    .asRuntimeException();
            }
        return serializer;
        }

    /**
     * Return a named serializer.
     *
     * @param sFormat  the name of the serializer
     * @param loader   the class loader to use to create the serializer
     *
     * @return a named serializer
     *
     * @throws io.grpc.StatusRuntimeException if no serializer is configured with the specified name
     */
    public Serializer getSerializer(String sFormat, ClassLoader loader)
        {
        Serializer serializer = f_storeSerializer.get(sFormat, loader);

        if (serializer == null)
            {
            serializer = f_dependencies.getContext()
                    .map(c -> c.getNamedSerializer(sFormat))
                    .orElse(null);
            }
        if (serializer == null)
            {
            serializer = f_serializerProducer.getNamedSerializer(sFormat, loader);
            }
        if (serializer != null)
            {
            f_storeSerializer.put(serializer, loader);
            }

        if (serializer == null)
            {
            throw Status.INVALID_ARGUMENT
                    .withDescription("invalid request format, cannot find serializer with name '" + sFormat + "'")
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
        public DefaultDependencies(GrpcDependencies.ServerType serverType)
            {
            super(serverType);
            }

        public DefaultDependencies(GrpcServiceDependencies deps)
            {
            super(deps);
            }

        public DefaultDependencies(Dependencies deps)
            {
            super(deps);
            m_ccfSupplier = deps.getCacheFactorySupplier().orElse(null);
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

    /**
     * Error message used when the cache name is missing for a request.
     */
    public static final String INVALID_CACHE_NAME_MESSAGE = "invalid request, cache name cannot be null or empty";

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
