/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.oracle.coherence.cdi.CdiInterceptorSupport.CacheLifecycleEventHandler;
import com.oracle.coherence.cdi.CdiInterceptorSupport.EntryEventHandler;
import com.oracle.coherence.cdi.CdiInterceptorSupport.EntryProcessorEventHandler;
import com.oracle.coherence.cdi.CdiInterceptorSupport.EventHandler;
import com.oracle.coherence.cdi.CdiInterceptorSupport.LifecycleEventHandler;
import com.oracle.coherence.cdi.CdiInterceptorSupport.TransactionEventHandler;
import com.oracle.coherence.cdi.CdiInterceptorSupport.TransferEventHandler;
import com.oracle.coherence.cdi.CdiInterceptorSupport.UnsolicitedCommitEventHandler;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultCacheServer;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.net.events.application.LifecycleEvent;
import com.tangosol.net.events.partition.TransactionEvent;
import com.tangosol.net.events.partition.TransferEvent;
import com.tangosol.net.events.partition.UnsolicitedCommitEvent;
import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.EntryProcessorEvent;

import com.tangosol.util.MapEvent;
import com.tangosol.util.RegistrationBehavior;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import javax.enterprise.inject.spi.WithAnnotations;

import org.jboss.weld.environment.se.events.ContainerInitialized;

/**
 * A Coherence CDI {@link Extension}.
 *
 * @author Jonathan Knight  2019.10.24
 * @author Aleks Seovic  2020.03.25
 *
 * @since 14.1.2
 */
public class CoherenceExtension
        implements Extension
    {
    // ---- client-side event support ---------------------------------------

    /**
     * Process observer methods for {@link MapEvent}s.
     *
     * @param event  the event to process
     * @param <K>    the type of {@code EntryEvent} keys
     * @param <V>    the type of {@code EntryEvent} values
     */
    private <K, V> void processMapEventObservers(
            @Observes ProcessObserverMethod<MapEvent<K, V>, ?> event)
        {
        addMapListener(new CdiMapListener<>(event.getObserverMethod(),
                                            event.getAnnotatedMethod().getAnnotations()));
        }


    // ---- server-side interceptors support --------------------------------

    /**
     * Process observer methods for {@link LifecycleEvent}s.
     *
     * @param event  the event to process
     */
    private void processLifecycleEventObservers(
            @Observes ProcessObserverMethod<LifecycleEvent, ?> event)
        {
        m_lstInterceptors.add(new LifecycleEventHandler(event.getObserverMethod()));
        }

    /**
     * Process observer methods for {@link CacheLifecycleEvent}s.
     *
     * @param event  the event to process
     */
    private void processCacheLifecycleEventObservers(
            @Observes ProcessObserverMethod<CacheLifecycleEvent, ?> event)
        {
        m_lstInterceptors.add(new CacheLifecycleEventHandler(event.getObserverMethod()));
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
        m_lstInterceptors.add(new EntryEventHandler<>(event.getObserverMethod()));
        }

    /**
     * Process observer methods for {@link EntryProcessorEvent}s.
     *
     * @param event  the event to process
     */
    private void processEntryProcessorEventObservers(
            @Observes ProcessObserverMethod<EntryProcessorEvent, ?> event)
        {
        m_lstInterceptors.add(new EntryProcessorEventHandler(event.getObserverMethod()));
        }

   /**
     * Process observer methods for {@link TransactionEvent}s.
     *
     * @param event  the event to process
     */
    private void processTransactionEventObservers(
            @Observes ProcessObserverMethod<TransactionEvent, ?> event)
        {
        m_lstInterceptors.add(new TransactionEventHandler(event.getObserverMethod()));
        }

   /**
     * Process observer methods for {@link TransferEvent}s.
     *
     * @param event  the event to process
     */
    private void processTransferEventObservers(
            @Observes ProcessObserverMethod<TransferEvent, ?> event)
        {
        m_lstInterceptors.add(new TransferEventHandler(event.getObserverMethod()));
        }

   /**
     * Process observer methods for {@link UnsolicitedCommitEvent}s.
     *
     * @param event  the event to process
     */
    private void processUnsolicitedCommitEventObservers(
            @Observes ProcessObserverMethod<UnsolicitedCommitEvent, ?> event)
        {
        m_lstInterceptors.add(new UnsolicitedCommitEventHandler(event.getObserverMethod()));
        }

    // ---- Filter and Extractor injection support --------------------------

    /**
     * Process {@link FilterFactory} beans annotated with {@link FilterBinding}.
     *
     * @param event  the event to process
     * @param <T>    the declared type of the injection point
     */
    private <T extends FilterFactory<?, ?>> void processFilterInjectionPoint(
            @Observes @WithAnnotations(FilterBinding.class) ProcessAnnotatedType<T> event)
        {
        AnnotatedType<T> type = event.getAnnotatedType();
        type.getAnnotations()
                .stream()
                .filter(a -> a.annotationType().isAnnotationPresent(FilterBinding.class))
                .map(AnnotationInstance::create)
                .forEach(a -> m_mapFilterSupplier.put(a, type.getJavaClass()));
        }

    /**
     * Process {@link ExtractorFactory} beans annotated with
     * {@link ExtractorBinding}.
     *
     * @param event  the event to process
     * @param <T>    the declared type of the injection point
     */
    private <T extends ExtractorFactory<?, ?, ?>> void processValueExtractorInjectionPoint(
            @Observes @WithAnnotations(ExtractorBinding.class) ProcessAnnotatedType<T> event)
        {
        AnnotatedType<T> type = event.getAnnotatedType();
        type.getAnnotations()
                .stream()
                .filter(a -> a.annotationType().isAnnotationPresent(ExtractorBinding.class))
                .map(AnnotationInstance::create)
                .forEach(a -> m_mapExtractorSupplier.put(a, type.getJavaClass()));
        }

    /**
     * Register dynamic beans for this extension.
     *
     * @param event  the event to use for dynamic bean registration
     */
    private void addBeans(@Observes final AfterBeanDiscovery event)
        {
        // Register the filter producer bean that knows about all of the FilterFactory beans
        FilterProducer.FilterFactoryResolver filterResolver = new FilterProducer.FilterFactoryResolver(m_mapFilterSupplier);
        event.addBean()
                .produceWith(i -> filterResolver)
                .types(FilterProducer.FilterFactoryResolver.class)
                .qualifiers(Default.Literal.INSTANCE)
                .scope(ApplicationScoped.class)
                .beanClass(FilterProducer.FilterFactoryResolver.class);

        // Register the value extractor producer bean that knows about all of the ValueExtractorFactory beans
        ExtractorProducer.ValueExtractorFactoryResolver extractorResolver
                = new ExtractorProducer.ValueExtractorFactoryResolver(m_mapExtractorSupplier);
        event.addBean()
                .produceWith(i -> extractorResolver)
                .types(ExtractorProducer.ValueExtractorFactoryResolver.class)
                .qualifiers(Default.Literal.INSTANCE)
                .scope(ApplicationScoped.class)
                .beanClass(ExtractorProducer.ValueExtractorFactoryResolver.class);
        }

    // ---- lifecycle support -----------------------------------------------

    /**
     * Start {@link com.tangosol.net.DefaultCacheServer} as a daemon and wait
     * for all services to start.
     *
     * @param event the event fired once the CDI container is initialized
     */
    @SuppressWarnings("unused")
    private void startCacheServer(@Observes ContainerInitialized event)
        {
        DefaultCacheServer.startServerDaemon(getEventInterceptors()).waitForServiceStart();
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Return all event interceptors created from the discovered observer
     * methods.
     *
     * @return all discovered event interceptors
     */
    Map<String, EventInterceptor<?>> getEventInterceptors()
        {
        return m_lstInterceptors.stream()
                .collect(Collectors.toMap(EventHandler::getId, Function.identity()));
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
        m_lstInterceptors.forEach(interceptor ->
                registry.registerEventInterceptor(interceptor.getId(), interceptor, RegistrationBehavior.IGNORE));
        return ccf;
        }

    /**
     * Add specified listener to the collection of discovered observer-based listeners.
     *
     * @param listener  the listener to add
     */
    void addMapListener(CdiMapListener<?, ?> listener)
        {
        String svc   = listener.getServiceName();
        String cache = listener.getCacheName();

        Map<String, Set<CdiMapListener<?, ?>>> mapByCache = m_mapListeners.computeIfAbsent(svc, s -> new HashMap<>());
        Set<CdiMapListener<?, ?>> setListeners = mapByCache.computeIfAbsent(cache, c -> new HashSet<>());
        setListeners.add(listener);
        }

    /**
     * Return all map listeners that should be registered for a particular
     * service and cache combination.
     *
     * @param serviceName  the name of the service
     * @param cacheName    the name of the cache
     *
     * @return a set of all listeners that should be registered
     */
    Set<CdiMapListener<?, ?>> getMapListeners(String serviceName, String cacheName)
        {
        HashSet<CdiMapListener<?, ?>> setResults = new HashSet<>();
        collectMapListeners(setResults, "*", "*");
        collectMapListeners(setResults, "*", cacheName);
        collectMapListeners(setResults, serviceName, "*");
        collectMapListeners(setResults, serviceName, cacheName);

        return setResults;
        }

    /**
     * Add all map listeners for the specified service and cache combination to
     * the specified result set.
     *
     * @param setResults   the set of results to accumulate listeners into
     * @param serviceName  the name of the service
     * @param cacheName    the name of the cache
     */
    private void collectMapListeners(HashSet<CdiMapListener<?, ?>> setResults, String serviceName, String cacheName)
        {
        Map<String, Set<CdiMapListener<?, ?>>> mapByCache = m_mapListeners.get(serviceName);
        if (mapByCache != null)
            {
            setResults.addAll(mapByCache.getOrDefault(cacheName, Collections.emptySet()));
            }
        }

    // ---- data members ----------------------------------------------------

    /**
     * A list of event interceptors for all discovered observer methods.
     */
    private final Map<String, Map<String, Set<CdiMapListener<?, ?>>>> m_mapListeners = new HashMap<>();

    /**
     * A list of event interceptors for all discovered observer methods.
     */
    private final List<EventHandler<?, ?>> m_lstInterceptors = new ArrayList<>();

    /**
     * A map of {@link FilterBinding} annotation to {@link FilterFactory} bean
     * class.
     */
    private final Map<AnnotationInstance, Class<? extends FilterFactory<?, ?>>> m_mapFilterSupplier = new HashMap<>();

    /**
     * A map of {@link ExtractorBinding} annotation to {@link
     * ExtractorFactory} bean class.
     */
    private final Map<AnnotationInstance, Class<? extends ExtractorFactory<?, ?, ?>>> m_mapExtractorSupplier = new HashMap<>();
    }
