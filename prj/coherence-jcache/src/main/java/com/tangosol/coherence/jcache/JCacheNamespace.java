/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache;

import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.CacheMapping;
import com.tangosol.coherence.config.ParameterMacroExpression;
import com.tangosol.coherence.config.ResourceMappingRegistry;
import com.tangosol.coherence.config.ServiceSchemeRegistry;
import com.tangosol.coherence.config.SimpleParameterList;
import com.tangosol.coherence.config.builder.InstanceBuilder;
import com.tangosol.coherence.config.scheme.BackingMapScheme;
import com.tangosol.coherence.config.scheme.CacheStoreScheme;
import com.tangosol.coherence.config.scheme.DistributedScheme;
import com.tangosol.coherence.config.scheme.LocalScheme;
import com.tangosol.coherence.config.scheme.NearScheme;
import com.tangosol.coherence.config.scheme.ReadWriteBackingMapScheme;
import com.tangosol.coherence.config.scheme.ServiceScheme;
import com.tangosol.coherence.config.xml.processor.PersistenceProcessor;

import com.tangosol.coherence.jcache.common.ContainerHelper;
import com.tangosol.coherence.jcache.partitionedcache.PartitionedCacheBinaryEntryStore;
import com.tangosol.coherence.jcache.partitionedcache.PartitionedCacheConfigurationMapListener;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.injection.Injector;
import com.tangosol.config.injection.SimpleInjector;
import com.tangosol.config.xml.AbstractNamespaceHandler;
import com.tangosol.config.xml.NamespaceHandler;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.internal.net.service.grid.DefaultPartitionedCacheDependencies;
import com.tangosol.internal.net.service.grid.PersistenceDependencies;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.events.InterceptorRegistry;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.MapListener;
import com.tangosol.util.RegistrationBehavior;
import com.tangosol.util.ResourceRegistry;

import java.net.URI;

/**
 * A {@link NamespaceHandler} to enable and enhance Coherence-based
 * configurations to be used with the Coherence-based JCache implementation
 *
 * @author bo  2013.11.06
 * @since Coherence 12.1.3
 */
public class JCacheNamespace
        extends AbstractNamespaceHandler
    {
    // ----- NamespaceHandler interface -------------------------------------

    @Override
    public void onEndNamespace(ProcessingContext processingContext, XmlElement xmlElement, String s, URI uri)
        {
        super.onEndNamespace(processingContext, xmlElement, s, uri);

        CacheConfig cacheConfig = processingContext.getCookie(CacheConfig.class);

        if (cacheConfig == null)
            {
            throw new ConfigurationException("Can't locate the Coherence Configuration.  This only occurs when the "
                + uri + " namespace is used outside of a Coherence Cache Configuration", "Please ensure that the "
                    + uri + " is defined with in the scope of a Coherence Cache Configuration");
            }
        else
            {
            ResourceRegistry        registryResources     = processingContext.getResourceRegistry();
            ServiceSchemeRegistry   registryServices      = cacheConfig.getServiceSchemeRegistry();
            ResourceMappingRegistry registryCacheMappings = cacheConfig.getMappingRegistry();

            // register a jcache-lifecycle-interceptor to enable container activation
            // for coherence based jcache implementation.  The registration name is used for lookup in junit test
            // that validates that interceptor was called.
            registryResources.getResource(InterceptorRegistry.class)
                .registerEventInterceptor("jcache-lifecycle-interceptor", new ContainerHelper.JCacheLifecycleInterceptor(),
                                          RegistrationBehavior.ALWAYS);


            // short-term workaround for COH-12123
            PersistenceDependencies persistenceDeps = new PersistenceProcessor().onProcess(processingContext,
                                                          xmlElement);

            // we may need to inject some values into our services
            Injector injector = new SimpleInjector();

            // ---- ensure the <near-scheme> for the jcache configuration cache is defined ----
            ServiceScheme schemeService =
                registryServices.findSchemeBySchemeName(CoherenceBasedCache.JCACHE_CONFIG_SCHEME_NAME);

            // can not generate distributed service since need all Coherence system properties to work.
            // if pof is set, the config needs it for application listeners, cache loader, cache writer, expiry policy, ...
            DistributedScheme schemeBack =
                (DistributedScheme) registryServices
                    .findSchemeBySchemeName(CoherenceBasedCache.JCACHE_CONFIG_BACK_SCHEME_NAME);

            if (schemeBack == null)
                {
                // generate it. will not recognize coherence system properties for this distributed service.
                schemeBack = new DistributedScheme();

                schemeBack.setSchemeName(CoherenceBasedCache.JCACHE_CONFIG_BACK_SCHEME_NAME);
                schemeBack.setServiceName(CoherenceBasedCache.JCACHE_CONFIG_BACK_SCHEME_SERVICE_NAME);

                // construct the BackingMapScheme
                BackingMapScheme schemeBackingMap = new BackingMapScheme();

                schemeBackingMap.setInnerScheme(new LocalScheme());

                schemeBack.setBackingMapScheme(schemeBackingMap);
                schemeBack.setAutoStart(true);

                DefaultPartitionedCacheDependencies dependencies = new DefaultPartitionedCacheDependencies();

                dependencies.setPersistenceDependencies(persistenceDeps);

                // TODO: use resolver to fix generated Configuration missing xml preprocessor injection
                // of system properties into cache-config/operational config.  This solution was incorrect
                // since it impacted global resource registry used for processing context. Keeping it as
                // a reminder of what needs to be done to get all system properties referenced in operational config
                // to be resolved when DistributedScheme is just created by a NamespaceHandler such as this one.

                /*
                 * String localStorage = Config.getProperty("coherence.distributed.localstorage");
                 *
                 * if (localStorage != null && Boolean.valueOf(localStorage) != dependencies.isOwnershipCapable())
                 *   {
                 *   registryResources.registerResource(Boolean.TYPE, "local-storage", Boolean.valueOf(localStorage));
                 *   }
                 */
                injector.inject(dependencies, registryResources);
                schemeBack.setServiceDependencies(dependencies);
                injector.inject(schemeBack, registryResources);
                registryServices.register(schemeBack);
                }

            if (schemeService == null)
                {
                // construct the NearScheme
                NearScheme schemeNear = new NearScheme();

                schemeNear.setSchemeName(CoherenceBasedCache.JCACHE_CONFIG_SCHEME_NAME);
                schemeNear.setServiceName(CoherenceBasedCache.JCACHE_CONFIG_SERVICE_NAME);
                schemeNear.setInvalidationStrategy(new LiteralExpression<String>("all"));

                InstanceBuilder<MapListener> bldrMapListener =
                    new InstanceBuilder<MapListener>(PartitionedCacheConfigurationMapListener.class);

                schemeNear.setListenerBuilder(bldrMapListener);

                LocalScheme schemeFront = new LocalScheme();

                schemeNear.setFrontScheme(schemeFront);

                // Back scheme
                schemeNear.setBackScheme(schemeBack);
                schemeNear.setAutoStart(true);

                // register the JCache Configuration Scheme
                registryServices.register(schemeNear);
                }

            // ---- ensure the cache mapping for the jcache configuration is defined ----
            CacheMapping mapping = registryCacheMappings.findCacheMapping(CoherenceBasedCache.JCACHE_CONFIG_CACHE_NAME);

            // register a specific non-wildcard mapping for CoherenceBasedCache.JCACHE_CONFIG_CACHE_NAME
            if (mapping == null || mapping.usesWildcard())
                {
                mapping = new CacheMapping(CoherenceBasedCache.JCACHE_CONFIG_CACHE_NAME,
                                           CoherenceBasedCache.JCACHE_CONFIG_SCHEME_NAME);
                registryCacheMappings.register(mapping);
                }

            // ---- ensure the <distributed-scheme> for the jcache partitioned caches is defined ----
            schemeService = registryServices.findSchemeBySchemeName(CoherenceBasedCache.JCACHE_PARTITIONED_SCHEME_NAME);

            if (schemeService == null)
                {
                // construct the BinaryEntryStore
                // (this adapts a JCache Cache Writer into a Coherence CacheStore)
                InstanceBuilder     bldrBinaryEntryStore = new InstanceBuilder(PartitionedCacheBinaryEntryStore.class);
                SimpleParameterList listParameters       = new SimpleParameterList();

                listParameters.add(new Parameter("sCacheName", String.class,
                                                 new ParameterMacroExpression("{cache-name}", String.class)));
                listParameters.add(new Parameter("ctxBackingMap", BackingMapManagerContext.class,
                                                 new ParameterMacroExpression("{manager-context}",
                                                     BackingMapManagerContext.class)));
                listParameters.add(new Parameter("classLoader", ClassLoader.class,
                                                 new ParameterMacroExpression("{class-loader}", ClassLoader.class)));
                bldrBinaryEntryStore.setConstructorParameterList(listParameters);

                CacheStoreScheme schemeCacheStore = new CacheStoreScheme();

                schemeCacheStore.setCustomBuilder(bldrBinaryEntryStore);

                // construct the ReadWriteBackingMap
                ReadWriteBackingMapScheme schemeRWBM  = new ReadWriteBackingMapScheme();
                LocalScheme               schemeLocal = new LocalScheme();

                schemeRWBM.setCacheStoreScheme(schemeCacheStore);
                schemeRWBM.setInternalScheme(schemeLocal);

                // construct the BackingMapScheme
                BackingMapScheme schemeBackingMap = new BackingMapScheme();

                schemeBackingMap.setInnerScheme(schemeRWBM);

                // construct the DistributedScheme
                DistributedScheme schemeDistributed = new DistributedScheme();

                schemeDistributed.setSchemeName(CoherenceBasedCache.JCACHE_PARTITIONED_SCHEME_NAME);
                schemeDistributed.setServiceName(CoherenceBasedCache.JCACHE_PARTITIONED_SERVICE_NAME);
                schemeDistributed.setBackingMapScheme(schemeBackingMap);
                schemeDistributed.setAutoStart(true);

                DefaultPartitionedCacheDependencies dependencies = new DefaultPartitionedCacheDependencies();

                dependencies.setPersistenceDependencies(persistenceDeps);

                injector.inject(dependencies, registryResources);

                schemeDistributed.setServiceDependencies(dependencies);

                injector.inject(schemeDistributed, registryResources);

                // register the DistributedScheme
                registryServices.register(schemeDistributed);
                }

            // ---- ensure the cache mapping for the jcache partitioned caches is defined ----
            mapping = registryCacheMappings.findCacheMapping(CoherenceBasedCache.JCACHE_PARTITIONED_CACHE_NAME_PATTERN);

            if (mapping == null)
                {
                mapping = new CacheMapping(CoherenceBasedCache.JCACHE_PARTITIONED_CACHE_NAME_PATTERN,
                                           CoherenceBasedCache.JCACHE_PARTITIONED_SCHEME_NAME);
                registryCacheMappings.register(mapping);
                }

            // ---- ensure the <local-scheme> for the jcache local caches is defined ----
            schemeService = registryServices.findSchemeBySchemeName(CoherenceBasedCache.JCACHE_LOCAL_SCHEME_NAME);

            if (schemeService == null)
                {
                // construct the LocalScheme
                LocalScheme schemeLocal = new LocalScheme();

                schemeLocal.setSchemeName(CoherenceBasedCache.JCACHE_LOCAL_SCHEME_NAME);
                schemeLocal.setServiceName(CoherenceBasedCache.JCACHE_LOCAL_SERVICE_NAME);
                injector.inject(schemeLocal, registryResources);

                // register the LocalScheme
                registryServices.register(schemeLocal);
                }

            // ---- ensure the cache mapping for the jcache local caches is defined ----
            mapping = registryCacheMappings.findCacheMapping(CoherenceBasedCache.JCACHE_LOCAL_CACHE_NAME_PATTERN);

            if (mapping == null)
                {
                mapping = new CacheMapping(CoherenceBasedCache.JCACHE_LOCAL_CACHE_NAME_PATTERN,
                                           CoherenceBasedCache.JCACHE_LOCAL_SCHEME_NAME);
                registryCacheMappings.register(mapping);
                }
            }
        }
    }
