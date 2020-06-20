/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server;

import com.oracle.coherence.cdi.server.CdiInterceptorSupport.CacheLifecycleEventHandler;
import com.oracle.coherence.cdi.server.CdiInterceptorSupport.EntryEventHandler;
import com.oracle.coherence.cdi.server.CdiInterceptorSupport.EntryProcessorEventHandler;
import com.oracle.coherence.cdi.server.CdiInterceptorSupport.EventHandler;
import com.oracle.coherence.cdi.server.CdiInterceptorSupport.LifecycleEventHandler;
import com.oracle.coherence.cdi.server.CdiInterceptorSupport.TransactionEventHandler;
import com.oracle.coherence.cdi.server.CdiInterceptorSupport.TransferEventHandler;
import com.oracle.coherence.cdi.server.CdiInterceptorSupport.UnsolicitedCommitEventHandler;

import com.oracle.coherence.common.collections.ConcurrentHashMap;

import com.tangosol.config.ConfigurationException;

import com.tangosol.net.CacheFactoryBuilder;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.ScopedCacheFactoryBuilder;
import com.tangosol.net.ServiceMonitor;
import com.tangosol.net.SimpleServiceMonitor;

import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.net.events.application.LifecycleEvent;
import com.tangosol.net.events.federation.FederatedChangeEvent;
import com.tangosol.net.events.federation.FederatedConnectionEvent;
import com.tangosol.net.events.federation.FederatedPartitionEvent;
import com.tangosol.net.events.partition.TransactionEvent;
import com.tangosol.net.events.partition.TransferEvent;
import com.tangosol.net.events.partition.UnsolicitedCommitEvent;
import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.EntryProcessorEvent;

import com.tangosol.util.RegistrationBehavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessObserverMethod;

/**
 * A Coherence CDI {@link Extension} that shouyld only be used within the
 * cluster members.
 *
 * @author Jonathan Knight  2019.10.24
 * @author Aleks Seovic  2020.03.25
 *
 * @since 20.06
 */
public class CoherenceServerExtension
        implements Extension
    {
    // ---- server-side interceptors support --------------------------------

    /**
     * Process observer methods for {@link LifecycleEvent}s.
     *
     * @param event  the event to process
     */
    private void processLifecycleEventObservers(
            @Observes ProcessObserverMethod<LifecycleEvent, ?> event)
        {
        m_listInterceptors.add(new LifecycleEventHandler(event.getObserverMethod()));
        }

    /**
     * Process observer methods for {@link CacheLifecycleEvent}s.
     *
     * @param event  the event to process
     */
    private void processCacheLifecycleEventObservers(
            @Observes ProcessObserverMethod<CacheLifecycleEvent, ?> event)
        {
        m_listInterceptors.add(new CacheLifecycleEventHandler(event.getObserverMethod()));
        }

    /**
     * Process observer methods for {@link EntryEvent}s.
     *
     * @param event  the event to process
     * @param <K>    the type of {@code EntryEvent} keys
     * @param <V>    the type of {@code EntryEvent} values
     */
    private <K, V> void processEntryEventObservers(
            @Observes ProcessObserverMethod<EntryEvent<K, V>, ?> event)
        {
        m_listInterceptors.add(new EntryEventHandler<>(event.getObserverMethod()));
        }

    /**
     * Process observer methods for {@link EntryProcessorEvent}s.
     *
     * @param event  the event to process
     */
    private void processEntryProcessorEventObservers(
            @Observes ProcessObserverMethod<EntryProcessorEvent, ?> event)
        {
        m_listInterceptors.add(new EntryProcessorEventHandler(event.getObserverMethod()));
        }

   /**
     * Process observer methods for {@link TransactionEvent}s.
     *
     * @param event  the event to process
     */
    private void processTransactionEventObservers(
            @Observes ProcessObserverMethod<TransactionEvent, ?> event)
        {
        m_listInterceptors.add(new TransactionEventHandler(event.getObserverMethod()));
        }

   /**
     * Process observer methods for {@link TransferEvent}s.
     *
     * @param event  the event to process
     */
    private void processTransferEventObservers(
            @Observes ProcessObserverMethod<TransferEvent, ?> event)
        {
        m_listInterceptors.add(new TransferEventHandler(event.getObserverMethod()));
        }

   /**
     * Process observer methods for {@link UnsolicitedCommitEvent}s.
     *
     * @param event  the event to process
     */
    private void processUnsolicitedCommitEventObservers(
            @Observes ProcessObserverMethod<UnsolicitedCommitEvent, ?> event)
        {
        m_listInterceptors.add(new UnsolicitedCommitEventHandler(event.getObserverMethod()));
        }

    // >>>> COMMERCIAL FEATURE ----------------------------------------------

    /**
      * Process observer methods for {@link FederatedConnectionEvent}s.
      *
      * @param event  the event to process
      */
     private void processFederatedConnectionEventObservers(
             @Observes ProcessObserverMethod<FederatedConnectionEvent, ?> event)
         {
         FederationEventHandlers.FederatedConnectionEventHandler eventHandler =
                 new FederationEventHandlers.FederatedConnectionEventHandler(event.getObserverMethod());
         m_listInterceptors.add(eventHandler);
         }

    /**
      * Process observer methods for {@link FederatedChangeEvent}s.
      *
      * @param event  the event to process
      */
     private void processFederatedChangeEventObservers(
             @Observes ProcessObserverMethod<FederatedChangeEvent, ?> event)
         {
         FederationEventHandlers.FederatedChangeEventHandler eventHandler =
                 new FederationEventHandlers.FederatedChangeEventHandler(event.getObserverMethod());
         m_listInterceptors.add(eventHandler);
         }

    /**
      * Process observer methods for {@link FederatedPartitionEvent}s.
      *
      * @param event  the event to process
      */
     private void processFederatedPartitionEventObservers(
             @Observes ProcessObserverMethod<FederatedPartitionEvent, ?> event)
         {
         FederationEventHandlers.FederatedPartitionEventHandler eventHandler =
                 new FederationEventHandlers.FederatedPartitionEventHandler(event.getObserverMethod());
         m_listInterceptors.add(eventHandler);
         }

    // <<<< COMMERCIAL FEATURE ----------------------------------------------

    // ---- lifecycle support -----------------------------------------------

    /**
     * Start {@link DefaultCacheServer} as a daemon and wait
     * for all services to start.
     *
     * @param event the event fired once the CDI container is initialized
     */
    @SuppressWarnings("unused")
    synchronized void startServer(@Observes @Initialized(ApplicationScoped.class) Object event, BeanManager beanManager)
        {
        m_beanManager = beanManager;
        CacheFactoryBuilder cfb = m_cacheFactoryBuilder = new CdiCacheFactoryBuilder();

        // start system CCF
        ConfigurableCacheFactory ccfSys = m_ccfSys = cfb.getConfigurableCacheFactory("coherence-system-config.xml", null);
        initializeConfigurableCacheFactory(ccfSys);

        // start discovered CCFs
        Instance<ScopeInitializer> initializers = beanManager.createInstance().select(ScopeInitializer.class, Any.Literal.INSTANCE);
        for (ScopeInitializer initializer : initializers)
            {
            ConfigurableCacheFactory ccf = initializer.getConfigurableCacheFactory(cfb);
            if (ccf != null)
                {
                initializeConfigurableCacheFactory(ccf);
                }
            }

        // start default/application CCF using DCS
        ConfigurableCacheFactory ccfApp = m_ccfApp = cfb.getConfigurableCacheFactory(null);
        initializeConfigurableCacheFactory(ccfApp);
        DefaultCacheServer.startServerDaemon(ccfApp).waitForServiceStart();
        }

    /**
     * Stop {@link DefaultCacheServer}.
     *
     * @param event the event fired before the CDI container is shut down
     */
    @SuppressWarnings("unused")
    synchronized void stopServer(@Observes BeforeShutdown event)
        {
        f_listServiceMonitors.forEach(ServiceMonitor::stopMonitoring);
        f_listCCF.forEach(ConfigurableCacheFactory::dispose);
        DefaultCacheServer.shutdown();
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Initialize specified {@link ConfigurableCacheFactory} with discovered
     * event observers and {@link BeanManager}.
     *
     * @param ccf  the ConfigurableCacheFactory to initialize
     */
    public void initializeConfigurableCacheFactory(ConfigurableCacheFactory ccf)
        {
        registerInterceptors(ccf);
        ccf.getResourceRegistry().registerResource(BeanManager.class, "beanManager", m_beanManager);
        ccf.activate();
        f_listCCF.add(0, ccf);

        if (ccf instanceof ExtensibleConfigurableCacheFactory)
            {
            ExtensibleConfigurableCacheFactory eccf = (ExtensibleConfigurableCacheFactory) ccf;
            ServiceMonitor serviceMon = new SimpleServiceMonitor();

            serviceMon.setConfigurableCacheFactory(eccf);
            serviceMon.registerServices(eccf.getServiceMap());
            f_listServiceMonitors.add(0, serviceMon);
            }
        }

    /**
     * Return the {@code CacheFactoryBuilder builder} to obtain
     * {@link ConfigurableCacheFactory cache factories} from.
     *
     * @return the {@code CacheFactoryBuilder builder} to use
     */
    public CdiCacheFactoryBuilder getCacheFactoryBuilder()
        {
        return m_cacheFactoryBuilder;
        }

    /**
     * Return default {@link ConfigurableCacheFactory cache factory} for the application.
     *
     * @return the default {@link ConfigurableCacheFactory cache factory} for the application
     */
    public ConfigurableCacheFactory getDefaultCacheFactory()
        {
        return m_ccfApp;
        }

    /**
     * Return system {@link ConfigurableCacheFactory cache factory}.
     *
     * @return the system {@link ConfigurableCacheFactory cache factory}
     */
    ConfigurableCacheFactory getSystemCacheFactory()
        {
        return m_ccfSys;
        }

    /**
     * Register all discovered interceptors with the specified {@link ConfigurableCacheFactory}.
     *
     * @param ccf  the cache factory to register interceptors with
     *
     * @return the {@link ConfigurableCacheFactory} after interceptor registration
     */
    ConfigurableCacheFactory registerInterceptors(ConfigurableCacheFactory ccf)
        {
        InterceptorRegistry registry = ccf.getInterceptorRegistry();
        for (EventHandler<?, ?> handler : m_listInterceptors)
            {
            if (handler.getScopeName() == null || handler.getScopeName().equals(ccf.getScopeName()))
                {
                registry.registerEventInterceptor(handler.getId(), handler, RegistrationBehavior.FAIL);
                }
            }

        return ccf;
        }

    // ---- inner class: CdiCacheFactoryBuilder -----------------------------

    /**
     * Custom implementation of {@link CacheFactoryBuilder} that changes
     * the default name of the application cache configuration file to load.
     */
    static class CdiCacheFactoryBuilder
            extends ScopedCacheFactoryBuilder
        {
        public CdiCacheFactoryBuilder()
            {
            super((sConfigURI, loader, sScopeName) ->
                  {
                  // extract scope name from the config URI, if present
                  String[] asParts = sConfigURI.split("@");
                  return asParts.length > 1
                          ? asParts[0]
                          : sScopeName;
                  });
            }

        @Override
        protected String resolveURI(String sConfigURI)
            {
            // strip scope name from the config URI, if present
            String[] asParts = sConfigURI.split("@");
            if (asParts.length > 1)
                {
                sConfigURI = asParts[1];
                }

            return URI_DEFAULT.equals(sConfigURI)
                    ? DEFAULT_CONFIG
                    : super.resolveURI(sConfigURI);
            }

        @Override
        protected ConfigurableCacheFactory buildFactory(String sConfigURI, ClassLoader loader)
            {
            ConfigurableCacheFactory ccf = super.buildFactory(sConfigURI, loader);
            String sScope = ccf.getScopeName();
            if (sScope != null && !sScope.isEmpty())
                {
                ConfigurableCacheFactory previous = f_mapByScope.putIfAbsent(sScope, ccf);
                if (previous != null)
                    {
                    throw new ConfigurationException("Non-unique ConfigurableCacheFactory scope",
                                                     "Make sure that each scoped cache factory has a unique scope name.");
                    }
                }
            return ccf;
            }

        /**
         * Return a {@link ConfigurableCacheFactory} with the specified scope name,
         * or {@code null} if the scope doesn't exist.
         *
         * @param sScopeName  the name of the scope to get the cache factory for
         *
         * @return a {@link ConfigurableCacheFactory} with the specified scope name,
         *         or {@code null} if the scope doesn't exist
         */
        ConfigurableCacheFactory getConfigurableCacheFactory(String sScopeName)
            {
            return f_mapByScope.get(sScopeName);
            }

        // ---- data members ------------------------------------------------

        private static final String DEFAULT_CONFIG = "coherence-config.xml";

        /**
         * Mapping used to associate cache factories with scopes.
         */
        private final Map<String, ConfigurableCacheFactory> f_mapByScope =
                new ConcurrentHashMap<>();
       }

    // ---- data members ----------------------------------------------------

    /**
     * The {@link BeanManager} for this extension.
     */
    private BeanManager m_beanManager;

    /**
     * The {@link CacheFactoryBuilder} to use when creating new cache factories.
     */
    private CdiCacheFactoryBuilder m_cacheFactoryBuilder;

    /**
     * A list of {@link ConfigurableCacheFactory} instances initialized by this extension.
     */
    private final List<ConfigurableCacheFactory> f_listCCF = new ArrayList<>();

    /**
     * A list of {@link ServiceMonitor}s initialized by this extension.
     */
    private final List<ServiceMonitor> f_listServiceMonitors = new ArrayList<>();

    /**
     * System {@link ConfigurableCacheFactory}.
     */
    private ConfigurableCacheFactory m_ccfSys;

    /**
     * Application {@link ConfigurableCacheFactory}.
     */
    private ConfigurableCacheFactory m_ccfApp;

    /**
     * A list of event interceptors for all discovered observer methods.
     */
    private final List<EventHandler<?, ?>> m_listInterceptors = new ArrayList<>();
    }
