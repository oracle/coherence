/*
 * Copyright (c) 2016, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.internal;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.concurrent.config.ConcurrentConfiguration;

import com.oracle.coherence.concurrent.executor.ClusteredExecutorService;

import com.oracle.coherence.concurrent.executor.util.Caches;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;

import com.tangosol.net.events.Event;
import com.tangosol.net.events.EventInterceptor;

import com.tangosol.net.events.SessionLifecycleEvent;

import com.tangosol.net.events.application.LifecycleEvent;

import com.tangosol.util.Base;
import com.tangosol.util.ResourceRegistry;

import java.util.concurrent.ExecutorService;

/**
 * A {@link LifecycleEvent} interceptor to automatically activate and deactivate
 * a local {@link ExecutorService} for executing tasks.
 */
public class LifecycleEventInterceptor
        implements EventInterceptor<Event<?>>
    {
    // ----- EventInterceptor interface -------------------------------------

    @Override
    public void onEvent(Event<?> event)
        {
        ExecutorTrace.log(() -> String.format("LifecycleEventInterceptor received event with type: %s", event.getType()));

        if (event.getType() == LifecycleEvent.Type.ACTIVATED)
            {
            m_fClosed = false;
            LifecycleEvent lifecycleEvent = (LifecycleEvent) event;
            try
                {
                // acquire the local member
                Member member = CacheFactory.getCluster().getLocalMember();

                // acquire the CCF that was just started
                ConfigurableCacheFactory configurableCacheFactory = lifecycleEvent.getConfigurableCacheFactory();

                // attempt to acquire the cache to determine if we're storage enabled
                NamedCache<?, ?> cache = Caches.assignments(configurableCacheFactory);

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

                        // register the JDK ExecutorService and TaskExecutorService
                        // with the configurable cache factory (so we can clean it up later)
                        ResourceRegistry ccfRegistry = configurableCacheFactory.getResourceRegistry();
                        ccfRegistry.registerResource(ClusteredExecutorService.class,
                                                     ClusteredExecutorService.class.getSimpleName(),
                                                     clusteredExecutorService);

                        ConcurrentConfiguration configuration = ConcurrentConfiguration.get();
                        configuration.setExecutorService(clusteredExecutorService);
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
                ExecutorTrace.throwing(LifecycleEventInterceptor.class, "onEvent", e);

                throw Base.ensureRuntimeException(e);
                }
            }
        else if (event.getType() == LifecycleEvent.Type.DISPOSING ||
                 event.getType() == SessionLifecycleEvent.Type.STOPPING)
            {
            if (!m_fClosed)
                {
                m_fClosed = true;
                ResourceRegistry registry = getRegistry(event);

                ClusteredExecutorService clusteredExecutorService =
                        registry.getResource(ClusteredExecutorService.class,
                                             ClusteredExecutorService.class.getSimpleName());

                if (clusteredExecutorService != null)
                    {
                    clusteredExecutorService.shutdownNow();
                    }

                Hook.runShutdownHooks(registry);

                ConcurrentConfiguration.get().reset();
                }
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return the {@link ResourceRegistry} of the underlying
     * {@link ConfigurableCacheFactory} associated with the event.
     *
     * @param event  the {@link Event}
     *
     * @return the {@link ResourceRegistry} of the underlying
     *         {@link ConfigurableCacheFactory} associated with the event
     */
    protected ResourceRegistry getRegistry(Event<?> event)
        {
        if (event instanceof LifecycleEvent)
            {
            return ((LifecycleEvent) event).getConfigurableCacheFactory().getResourceRegistry();
            }
        else
            {
            return ((SessionLifecycleEvent) event).getSession().getResourceRegistry();
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * Flag indicating that the event processing associated with the termination
     * of the executor runtime has been processed.
     */
    protected boolean m_fClosed;
    }
