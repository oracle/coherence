/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.internal;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.concurrent.executor.TaskExecutorService;

import com.oracle.coherence.concurrent.executor.options.ClusterMember;
import com.oracle.coherence.concurrent.executor.options.Storage;

import com.oracle.coherence.concurrent.executor.ClusteredAssignment;
import com.oracle.coherence.concurrent.executor.ClusteredExecutorService;
import com.oracle.coherence.concurrent.executor.ThreadFactories;

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
                        ccfRegistry.registerResource(TaskExecutorService.class,
                                                     TaskExecutorService.class.getSimpleName(),
                                                     clusteredExecutorService);

                        // register the executor service for this member
                        if (service instanceof DistributedCacheService)
                            {
                            DistributedCacheService distributedCacheService = (DistributedCacheService) service;

                            clusteredExecutorService.register(
                                    managedExecutorService,
                                    Storage.enabled(distributedCacheService.isLocalStorageEnabled()),
                                    ClusterMember.INSTANCE);
                            }
                        else
                            {
                            clusteredExecutorService.register(managedExecutorService, ClusterMember.INSTANCE);
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
            ExecutorService managedExecutorService =
                event.getConfigurableCacheFactory()
                        .getResourceRegistry().getResource(ExecutorService.class, "ManagedExecutorService");

            if (managedExecutorService != null)
                {
                managedExecutorService.shutdownNow();
                }
            }
        }
    }
