/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common;

import com.oracle.coherence.common.base.Classes;
import com.tangosol.coherence.config.Config;
import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.grpc.GrpcDependencies;
import io.grpc.Status;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A factory of {@link ConfigurableCacheFactory} supplier functions
 * that can return a {@link ConfigurableCacheFactory} for a given
 * scope name.
 *
 * @author Jonathan Knight  2020.09.24
 */
public interface ConfigurableCacheFactorySuppliers
    {
    /**
     * Returns an instance of a fixed supplier that only returns one of
     * the specified set of {@link ConfigurableCacheFactory} instances.
     *
     * @return an instance of a fixed supplier
     */
    static Function<String, ConfigurableCacheFactory> fixed(ConfigurableCacheFactory... ccfs)
        {
        return new FixedCacheFactorySupplier(ccfs);
        }

    // ----- inner class: DefaultCacheFactorySupplier -----------------------

    /**
     * The default {@link ConfigurableCacheFactory} supplier.
     * <p>
     * This supplier will return the default {@link ConfigurableCacheFactory} from the
     * {@link com.tangosol.net.CacheFactoryBuilder} for the default, or {@code null} scope.
     * For other scope values a System property of {@code coherence.cacheconfig.scope-name}
     * must have been set to the URI of the required cache configuration file.
     */
    class DefaultCacheFactorySupplier
            implements Function<String, ConfigurableCacheFactory>
        {
        @Override
        public ConfigurableCacheFactory apply(String scope)
            {
            if (scope == null || GrpcDependencies.DEFAULT_SCOPE.equals(scope))
                {
                return CacheFactory.getCacheFactoryBuilder()
                        .getConfigurableCacheFactory(Classes.getContextClassLoader());
                }

            // Try to find a Session with the required scope in a Coherence instance
            ConfigurableCacheFactory[] aCCF = Coherence.findSessionsByScope(scope)
                    .stream()
                    .filter(ConfigurableCacheFactorySession.class::isInstance)
                    .map(ConfigurableCacheFactorySession.class::cast)
                    .map(ConfigurableCacheFactorySession::getConfigurableCacheFactory)
                    .toArray(ConfigurableCacheFactory[]::new);

            if (aCCF.length == 1)
                {
                return aCCF[0];
                }
            else if (aCCF.length > 1)
                {
                throw Status.INVALID_ARGUMENT
                        .withDescription("Multiple ConfigurableCacheFactory instances have been configured with scope name "+ scope)
                        .asRuntimeException();
                }

            String sURI = Config.getProperty("coherence.cacheconfig." + scope);
            if (sURI != null && !sURI.isEmpty())
                {
                return CacheFactory.getCacheFactoryBuilder()
                        .getConfigurableCacheFactory(sURI, Classes.getContextClassLoader());
                }

            throw Status.INVALID_ARGUMENT
                    .withDescription("cannot locate ConfigurableCacheFactory with scope name "+ scope)
                    .asRuntimeException();
            }
        }

    // ----- inner class: FixedCacheFactorySupplier -------------------------

    /**
     * A fixed {@link ConfigurableCacheFactory} supplier that only supplies
     * the {@link ConfigurableCacheFactory} instances provided when it was
     * constructed.
     */
    class FixedCacheFactorySupplier
            implements Function<String, ConfigurableCacheFactory>
        {
        public FixedCacheFactorySupplier(ConfigurableCacheFactory... ccfs)
            {
            Map<String, ConfigurableCacheFactory> map = new HashMap<>();
            for (ConfigurableCacheFactory ccf : ccfs)
                {
                map.put(ccf.getScopeName(), ccf);
                }
            f_mapCCF = map;
            }

        @Override
        public ConfigurableCacheFactory apply(String scope)
            {
            if (scope == null)
                {
                scope = GrpcDependencies.DEFAULT_SCOPE;
                }

            ConfigurableCacheFactory ccf = f_mapCCF.get(scope);
            if (ccf != null)
                {
                return ccf;
                }

            if (GrpcDependencies.DEFAULT_SCOPE.equals(scope))
                {
                return CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(Classes.getContextClassLoader());
                }

            throw Status.INVALID_ARGUMENT
                    .withDescription("cannot locate ConfigurableCacheFactory with scope name "+ scope)
                    .asRuntimeException();
            }

        private final Map<String, ConfigurableCacheFactory> f_mapCCF;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The default instance of the DefaultCacheFactorySupplier.
     */
    Function<String, ConfigurableCacheFactory> DEFAULT = new DefaultCacheFactorySupplier();
    }
