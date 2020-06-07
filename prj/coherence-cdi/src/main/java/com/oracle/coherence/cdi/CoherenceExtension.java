/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.oracle.coherence.cdi.events.CacheServerStarted;
import com.tangosol.net.DefaultCacheServer;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;

import javax.enterprise.inject.Default;

import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;

import org.jboss.weld.environment.se.events.ContainerInitialized;

/**
 * A Coherence CDI {@link Extension}.
 *
 * @author Jonathan Knight  2019.10.24
 * @author Aleks Seovic  2020.03.25
 */
class CoherenceExtension
        implements Extension
    {
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

    /**
     * Start {@link com.tangosol.net.DefaultCacheServer} as a daemon and wait
     * for all services to start.
     *
     * @param event the event fired once the CDI container is initialized
     */
    private void startCacheServer(@Observes ContainerInitialized event, Event<CacheServerStarted> servicesStartedEvent)
        {
        DefaultCacheServer.startServerDaemon().waitForServiceStart();
        servicesStartedEvent.fire(new CacheServerStarted());
        }

    // ---- data members ----------------------------------------------------

    /**
     * A map of {@link FilterBinding} annotation to {@link FilterFactory} bean
     * class.
     */
    private Map<AnnotationInstance, Class<? extends FilterFactory<?, ?>>> m_mapFilterSupplier = new HashMap<>();

    /**
     * A map of {@link ExtractorBinding} annotation to {@link
     * ExtractorFactory} bean class.
     */
    private Map<AnnotationInstance, Class<? extends ExtractorFactory<?, ?, ?>>> m_mapExtractorSupplier = new HashMap<>();
    }
