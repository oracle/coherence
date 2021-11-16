/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.internal;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.concurrent.executor.ClusteredExecutorInfo;

import com.oracle.coherence.concurrent.executor.ThreadFactories;
import com.oracle.coherence.concurrent.executor.options.ClusterMember;
import com.oracle.coherence.concurrent.executor.options.Name;

import com.oracle.coherence.concurrent.executor.ClusteredAssignment;
import com.oracle.coherence.concurrent.executor.ClusteredExecutorService;

import com.oracle.coherence.concurrent.executor.options.Storage;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;

import com.tangosol.net.events.EventInterceptor;

import com.tangosol.net.events.application.LifecycleEvent;

import com.tangosol.util.Base;
import com.tangosol.util.ResourceRegistry;

import com.tangosol.util.function.Remote;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * A {@link LifecycleEvent} interceptor to automatically activate and deactivate
 * a local {@link ExecutorService} for executing tasks.
 */
public class LifecycleEventInterceptor
        implements EventInterceptor<LifecycleEvent>
    {
    // ----- EventInterceptor interface -------------------------------------

    @Override
    public void onEvent(LifecycleEvent event)
        {
        ExecutorTrace.log(() -> String.format("LifecycleEventInterceptor received event with type: %s", event.getType()));

        if (event.getType() == LifecycleEvent.Type.ACTIVATED)
            {
            try
                {
                // acquire the local member
                Member member = CacheFactory.getCluster().getLocalMember();

                // acquire the CCF that was just started
                ConfigurableCacheFactory configurableCacheFactory = event.getConfigurableCacheFactory();

                // attempt to acquire the cache to determine if we're storage enabled
                NamedCache<?, ?> cache = configurableCacheFactory.ensureCache(ClusteredAssignment.CACHE_NAME, null);

                if (cache != null && cache.isActive())
                    {
                    // attempt to acquire the cache service
                    CacheService service = cache.getCacheService();

                    if (service != null && service.isRunning())
                        {
                        Logger.info(() -> String.format(
                                "Establishing Executor for Member [%s] with the ClusteredExecutorService",
                                member.getId()));

                        // create a clustered ClusteredExecutorService for this member
                        ClusteredExecutorService clusteredExecutorService = new ClusteredExecutorService(service);

                        // establish an executor service for this member
                        ExecutorService managedExecutorService =
                            Executors.newSingleThreadExecutor(
                                    ThreadFactories.createThreadFactory(true, "ManagedExecutorService", null));

                        // register the JDK ExecutorService and TaskExecutorService
                        // with the configurable cache factory (so we can clean it up later)
                        ResourceRegistry ccfRegistry = configurableCacheFactory.getResourceRegistry();
                        ccfRegistry.registerResource(ExecutorService.class,
                                                     "ManagedExecutorService",
                                                     managedExecutorService);
                        ccfRegistry.registerResource(ClusteredExecutorService.class,
                                                     ClusteredExecutorService.class.getSimpleName(),
                                                     clusteredExecutorService);

                        // Legacy: register default executor service which will
                        //         be used for non-named executor submissions
                        Storage storage = Storage.enabled(service instanceof DistributedCacheService && ((DistributedCacheService) service).isLocalStorageEnabled());
                        clusteredExecutorService.register(managedExecutorService, storage, ClusterMember.INSTANCE);

                        // For each named ExecutorService registration that has a supplier
                        // will be registered on this member.
                        NamedCache<String, ClusteredExecutorInfo> executorInfos = configurableCacheFactory.ensureCache(ClusteredExecutorInfo.CACHE_NAME, null);
                        for (ClusteredExecutorInfo executorInfo : executorInfos.values())
                            {
                            Remote.Supplier<ExecutorService> supplier = executorInfo.getSupplier();
                            if (supplier != null)
                                {
                                clusteredExecutorService.register(supplier.get(),
                                                                  supplier,
                                                                  executorInfo.getOption(Name.class, null),
                                                                  storage,
                                                                  ClusterMember.INSTANCE);
                                }
                            }
                        }
                    else
                        {
                        if (Logger.isEnabled(Logger.WARNING))
                            {
                            String sMsg = "Unable to establishing Executor for Member [%s] with the"
                                          + " ClusteredExecutorService; the cache service does not exist"
                                          + " or is not running.";
                            Logger.warn(String.format(sMsg, member.getId()));
                            }

                        }
                    }
                else
                    {
                    if (Logger.isEnabled(Logger.WARNING))
                        {
                        String sMsg = "Unable to establishing Executor for Member [%s] with the"
                                      + " ClusteredExecutorService; the cache does not exist or is not active.";
                        Logger.warn(String.format(sMsg, member.getId()));
                        }

                    }
                }
            catch (Exception e)
                {
                //LOGGER.throwing(Logging.within(LifecycleEventInterceptor.class), "onEvent", e);

                throw Base.ensureRuntimeException(e);
                }
            }
        else if (event.getType() == LifecycleEvent.Type.DISPOSING)
            {
            ResourceRegistry registry = event.getConfigurableCacheFactory().getResourceRegistry();
            ExecutorService  managedExecutorService =
                    registry.getResource(ExecutorService.class, "ManagedExecutorService");

            if (managedExecutorService != null)
                {
                managedExecutorService.shutdownNow();
                }

            ClusteredExecutorService clusteredExecutorService =
                    registry.getResource(ClusteredExecutorService.class,
                                         ClusteredExecutorService.class.getSimpleName());

            if (clusteredExecutorService != null)
                {
                clusteredExecutorService.shutdownNow();
                }
            }
        }
    }
