/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.oracle.coherence.event.AnnotatedMapListener;

import com.oracle.coherence.inject.AnnotationInstance;
import com.oracle.coherence.inject.ExtractorBinding;
import com.oracle.coherence.inject.ExtractorFactory;
import com.oracle.coherence.inject.FilterBinding;
import com.oracle.coherence.inject.FilterFactory;
import com.oracle.coherence.inject.MapEventTransformerBinding;
import com.oracle.coherence.inject.MapEventTransformerFactory;
import com.oracle.coherence.event.EventObserverSupport;

import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.SessionProvider;

import com.tangosol.net.events.CoherenceLifecycleEvent;

import com.tangosol.net.events.application.LifecycleEvent;

import com.tangosol.net.events.internal.NamedEventInterceptor;

import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;

import com.tangosol.util.MapEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Priority;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;

import javax.enterprise.event.Observes;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;

import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import javax.enterprise.inject.spi.WithAnnotations;

/**
 * A Coherence CDI {@link Extension} that is used on both cluster members and
 * the clients.
 *
 * @author Jonathan Knight  2019.10.24
 * @author Aleks Seovic  2020.03.25
 *
 * @since 20.06
 */
public class CoherenceExtension
        implements Extension
    {
    // ---- client-side event support ---------------------------------------

    /**
     * Process observer methods for {@link CoherenceLifecycleEvent}s.
     *
     * @param event  the event to process
     */
    private void processCoherenceLifecycleEventObservers(
            @Observes ProcessObserverMethod<CoherenceLifecycleEvent, ?> event)
        {
        m_listInterceptors.add(new EventObserverSupport.CoherenceLifecycleEventHandler(new CdiEventObserver<>(event)));
        }

    /**
     * Process observer methods for {@link LifecycleEvent}s.
     *
     * @param event  the event to process
     */
    private void processLifecycleEventObservers(
            @Observes ProcessObserverMethod<LifecycleEvent, ?> event)
        {
        m_listInterceptors.add(new EventObserverSupport.LifecycleEventHandler(new CdiEventObserver<>(event)));
        }

    /**
     * Process observer methods for {@link CacheLifecycleEvent}s.
     *
     * @param event  the event to process
     */
    private void processCacheLifecycleEventObservers(
            @Observes ProcessObserverMethod<CacheLifecycleEvent, ?> event)
        {
        m_listInterceptors.add(new EventObserverSupport.CacheLifecycleEventHandler(new CdiEventObserver<>(event)));
        }

    /**
     * Process observer methods for {@link MapEvent}s.
     *
     * @param event  the event to process
     * @param <K>    the type of {@code EntryEvent} keys
     * @param <V>    the type of {@code EntryEvent} values
     */
    private <K, V> void processMapEventObservers(@Observes ProcessObserverMethod<MapEvent<K, V>, ?> event)
        {
        m_listListener.add(new AnnotatedMapListener<>(new CdiMapEventObserver<>(event),
                                                      event.getAnnotatedMethod().getAnnotations()));
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
     * Process {@link MapEventTransformerFactory} beans annotated with {@link MapEventTransformerBinding}.
     *
     * @param event  the event to process
     * @param <T>    the declared type of the injection point
     */
    private <T extends MapEventTransformerFactory<?, ?, ?, ?>> void processMapEventTransformerInjectionPoint(
            @Observes @WithAnnotations(MapEventTransformerBinding.class) ProcessAnnotatedType<T> event)
        {
        AnnotatedType<T> type = event.getAnnotatedType();
        type.getAnnotations()
                .stream()
                .filter(a -> a.annotationType().isAnnotationPresent(MapEventTransformerBinding.class))
                .map(AnnotationInstance::create)
                .forEach(a -> m_mapMapEventTransformerSupplier.put(a, type.getJavaClass()));
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

        // Register the MapEventTransformer producer bean that knows about all of the MapEventTransformerFactory beans
        MapEventTransformerProducer.MapEventTransformerFactoryResolver mapEventTransformerResolver
                = new MapEventTransformerProducer.MapEventTransformerFactoryResolver(m_mapMapEventTransformerSupplier);
        event.addBean()
                .produceWith(i -> mapEventTransformerResolver)
                .types(MapEventTransformerProducer.MapEventTransformerFactoryResolver.class)
                .qualifiers(Default.Literal.INSTANCE)
                .scope(ApplicationScoped.class)
                .beanClass(MapEventTransformerProducer.MapEventTransformerFactoryResolver.class);
        }

    /**
     * Returns the discovered map listeners.
     *
     * @return the discovered map listeners
     */
    public List<AnnotatedMapListener<?, ?>> getMapListeners()
        {
        return m_listListener;
        }

    // ---- lifecycle support -----------------------------------------------

    /**
     * Start {@link DefaultCacheServer} as a daemon and wait
     * for all services to start.
     *
     * @param event the event fired once the CDI container is initialized
     */
    @SuppressWarnings("unused")
    synchronized void startServer(@Observes @Priority(1) @Initialized(ApplicationScoped.class)
                                  Object event, BeanManager beanManager)
        {
        Instance<SessionConfiguration> configurations = beanManager.createInstance()
                .select(SessionConfiguration.class, Any.Literal.INSTANCE);

        Instance<SessionConfiguration.Provider> configurationProviders = beanManager.createInstance()
                .select(SessionConfiguration.Provider.class, Any.Literal.INSTANCE);

        Instance<InterceptorProvider> interceptorProviders = beanManager.createInstance()
                .select(InterceptorProvider.class, Any.Literal.INSTANCE);

        List<NamedEventInterceptor<?>> listInterceptor = m_listInterceptors.stream()
                .map(handler -> new NamedEventInterceptor<>(handler.getId(), handler))
                .collect(Collectors.toList());

        interceptorProviders.stream()
                .flatMap(provider -> StreamSupport.stream(provider.getInterceptors().spliterator(), false))
                .forEach(listInterceptor::add);

        // Create a configuration (include the default CCF as well as any discovered configurations)
        CoherenceConfiguration config = CoherenceConfiguration.builder()
                .named(Coherence.DEFAULT_NAME)
                .withSession(SessionConfiguration.defaultSession())
                .withSessions(configurations)
                .withSessionProviders(configurationProviders)
                .withEventInterceptors(listInterceptor)
                .build();

        // build and start the Coherence instance
        m_coherence = Coherence.builder(config).build();
        // wait for start-up to complete
        m_coherence.start().join();
        }

    /**
     * Stop the {@link Coherence} instance.
     *
     * @param event the event fired before the CDI container is shut down
     */
    @SuppressWarnings("unused")
    synchronized void stopServer(@Observes BeforeShutdown event)
        {
        SessionProvider.get().close();
        if (m_coherence != null)
            {
            m_coherence.close();
            }
        }

    /**
     * Returns the {@link Coherence} instance started by the extension.
     *
     * @return  the {@link Coherence} instance started by the extension
     */
    public Coherence getCoherence()
        {
        return m_coherence;
        }

    // ----- inner interface: InterceptorProvider ---------------------------

    /**
     * A provider of {@link NamedEventInterceptor} instances.
     */
    public interface InterceptorProvider
        {
        /**
         * Returns the {@link NamedEventInterceptor} instances.
         *
         * @return  the {@link NamedEventInterceptor} instances
         */
        Iterable<NamedEventInterceptor<?>> getInterceptors();
        }

    // ---- data members ----------------------------------------------------

    /**
     * A map of {@link FilterBinding} annotation to {@link FilterFactory} bean
     * class.
     */
    private final Map<AnnotationInstance, Class<? extends FilterFactory<?, ?>>> m_mapFilterSupplier = new HashMap<>();

    /**
     * A map of {@link ExtractorBinding} annotation to {@link
     * ExtractorFactory} bean class.
     */
    private final Map<AnnotationInstance, Class<? extends ExtractorFactory<?, ?, ?>>>
            m_mapExtractorSupplier = new HashMap<>();

    /**
     * A map of {@link MapEventTransformerBinding} annotation to {@link MapEventTransformerFactory} bean class.
     */
    private final Map<AnnotationInstance, Class<? extends MapEventTransformerFactory<?, ?, ?, ?>>>
            m_mapMapEventTransformerSupplier = new HashMap<>();

    /**
     * The {@link Coherence} instance.
     */
    private Coherence m_coherence;

    /**
     * A list of event interceptors for all discovered observer methods.
     */
    private final List<EventObserverSupport.EventHandler<?, ?>> m_listInterceptors = new ArrayList<>();

    /**
     * A list of discovered map listeners.
     */
    private final List<AnnotatedMapListener<?, ?>> m_listListener = new ArrayList<>();
    }
