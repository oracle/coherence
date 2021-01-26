/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.oracle.coherence.cdi.events.AnnotatedMapListener;
import com.oracle.coherence.cdi.events.EventObserverSupport;

import com.tangosol.net.Coherence;

import com.tangosol.net.SessionProvider;

import com.tangosol.net.events.CoherenceLifecycleEvent;
import com.tangosol.net.events.SessionLifecycleEvent;
import com.tangosol.net.events.application.LifecycleEvent;
import com.tangosol.net.events.internal.NamedEventInterceptor;
import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;

import com.tangosol.util.MapEvent;

import javax.annotation.Priority;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;

import javax.enterprise.event.Observes;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * Process observer methods for {@link SessionLifecycleEvent}s.
     *
     * @param event  the event to process
     */
    private void processSessionLifecycleEventObservers(
            @Observes ProcessObserverMethod<SessionLifecycleEvent, ?> event)
        {
        m_listInterceptors.add(new EventObserverSupport.SessionLifecycleEventHandler(new CdiEventObserver<>(event)));
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
     * Start {@link Coherence} instance and wait for all services to start.
     *
     * @param event the event fired once the CDI container is initialized
     */
    @SuppressWarnings("unused")
    synchronized void startServer(@Observes @Priority(1) @Initialized(ApplicationScoped.class)
                                  Object event, BeanManager beanManager)
        {
        m_coherence = ensureCoherence(beanManager);
        }

    /**
     * Return the list of discovered event observers.
     *
     * @return the list of discovered event observers
     */
    List<EventObserverSupport.EventHandler<?, ?>> getInterceptors()
        {
        return m_listInterceptors;
        }

    /**
     * Ensure that a {@link Coherence} bean is resolvable and started.
     *
     * @param beanManager  the bean manager to use to resolve the {@link Coherence} bean
     *
     * @return  the {@link Coherence} bean
     * @throws IllegalStateException if no {@link Coherence} bean is resolvable
     */
    public static Coherence ensureCoherence(BeanManager beanManager)
        {
        Instance<Coherence> instance = beanManager.createInstance()
                .select(Coherence.class, Name.Literal.of(Coherence.DEFAULT_NAME));

        if (instance.isResolvable())
            {
            return instance.get();
            }

        throw new IllegalStateException("Cannot resolve default Coherence instance");
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
        Coherence.closeAll();
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
