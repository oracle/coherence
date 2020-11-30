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
import com.oracle.coherence.inject.MapEventTransformerBinding;
import com.oracle.coherence.inject.MapEventTransformerFactory;
import com.tangosol.util.MapEventTransformer;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.transformer.ExtractorEventTransformer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A CDI bean that produces {@link MapEventTransformer} instances
 * using {@link MapEventTransformerFactory} beans annotated with
 * {@link MapEventTransformerBinding} annotations.
 *
 * @author Jonathan Knight  2020.06.16
 * @since 20.06
 */
@ApplicationScoped
public class MapEventTransformerProducer
        implements AnnotatedMapListener.MapEventTransformerProducer
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code MapEventTransformerProducer} instance.
     *
     * @param beanManager  a {@code BeanManager} to use
     * @param resolver     a {@code MapEventTransformerFactoryResolver} to use
     */
    @Inject
    MapEventTransformerProducer(BeanManager                        beanManager,
                                ExtractorProducer                  extractorProducer,
                                MapEventTransformerFactoryResolver resolver)
        {
        f_beanManager       = beanManager;
        f_extractorProducer = extractorProducer;
        f_resolver          = resolver;
        }

    // ---- producer methods ------------------------------------------------

    /**
     * Produces {@link MapEventTransformer} based on injection point metadata.
     *
     * @param ip   an injection point
     * @param <K>  the type of event's key
     * @param <V>  the type of event's value
     * @param <U>  the type of resulting transformed value
     *
     * @return a {@link MapEventTransformer} instance
     */
    @Produces
    public <K, V, U> MapEventTransformer<K, V, U> getTransformer(InjectionPoint ip)
        {
        Annotated annotated = ip.getAnnotated();

        if (annotated != null)
            {
            return resolve(annotated.getAnnotations());
            }

        return null;
        }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <K, V, U> MapEventTransformer<K, V, U> resolve(Set<Annotation> annotations)
        {
        Optional<Annotation> optional = annotations.stream()
                .filter(a -> a.annotationType().isAnnotationPresent(MapEventTransformerBinding.class))
                .findFirst();

        if (optional.isPresent())
            {
            // there is MapEventTransformerBinding annotation
            Annotation annotation = optional.get();
            Class<? extends MapEventTransformerFactory> clazz = f_resolver.resolve(annotation);
            if (clazz != null)
                {
                MapEventTransformerFactory supplier = f_beanManager.createInstance().select(clazz).get();
                if (supplier != null)
                    {
                    return supplier.create(annotation);
                    }
                }
            else
                {
                throw new DefinitionException("Unsatisfied dependency - no MapEventTransformerFactory bean found annotated with " + annotation);
                }
            }
        else if (annotations.stream().anyMatch(a -> a.annotationType().isAnnotationPresent(ExtractorBinding.class)))
            {
            // there is one or more ExtractorBinding annotations
            ValueExtractor<Object, Object> extractor = f_extractorProducer.resolve(annotations);
            return new ExtractorEventTransformer(extractor);
            }

        // there are no transformer or extractor annotations.
        return null;
        }

    // ---- inner class: MapEventTransformerFactoryResolver ------------------------------

    /**
     * A resolver of {@link MapEventTransformerFactory} bean classes for a given {@link
     * MapEventTransformerBinding} annotation.
     */
    static class MapEventTransformerFactoryResolver
        {
        /**
         * Construct {@code MapEventTransformerFactoryResolver} instance.
         *
         * @param map  the map of filter bindings to filter factories
         */
        MapEventTransformerFactoryResolver(Map<AnnotationInstance, Class<? extends MapEventTransformerFactory<?, ?, ?, ?>>> map)
            {
            f_mapFactory = map;
            }

        /**
         * Obtain the {@link MapEventTransformerFactory} class for a given {@link
         * MapEventTransformerBinding} annotation.
         *
         * @param annotation the filter binding to obtain the {@link MapEventTransformerFactory}
         *                   class for
         * @param <A>        the type of the {@link MapEventTransformerBinding} annotation
         *
         * @return the {@link MapEventTransformerFactory} class for a given {@link
         * MapEventTransformerBinding} annotation
         */
        @SuppressWarnings("unchecked")
        <A extends Annotation> Class<? extends MapEventTransformerFactory<A, ?, ?, ?>> resolve(A annotation)
            {
            AnnotationInstance instance = AnnotationInstance.create(annotation);
            return (Class<? extends MapEventTransformerFactory<A, ?, ?, ?>>) f_mapFactory.get(instance);
            }

        // ---- data members ------------------------------------------------

        /**
         * The map of filter bindings to filter factories.
         */
        private final Map<AnnotationInstance, Class<? extends MapEventTransformerFactory<?, ?, ?, ?>>> f_mapFactory;
        }

    // ---- data members ----------------------------------------------------
    
    /**
     * The current Bean Manager.
     */
    private final BeanManager f_beanManager;

    private final ExtractorProducer f_extractorProducer;

    /**
     * The resolver that can resolve a {@link MapEventTransformerFactory} bean from a {@link
     * MapEventTransformerBinding} annotation.
     */
    private final MapEventTransformerFactoryResolver f_resolver;
    }
