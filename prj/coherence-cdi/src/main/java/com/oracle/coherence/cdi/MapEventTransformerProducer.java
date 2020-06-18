/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.tangosol.util.MapEventTransformer;
import com.tangosol.util.QueryHelper;
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
import java.util.Collections;
import java.util.Map;

/**
 * A CDI bean that produces {@link MapEventTransformer} instances
 * using {@link MapEventTransformerFactory} beans annotated with
 * {@link MapEventTransformerBinding} annotations.
 *
 * @author Jonathan Knight  2020.06.16
 * @since 20.06
 */
@ApplicationScoped
class MapEventTransformerProducer
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
    <K, V, U> MapEventTransformer<K, V, U> getTransformer(InjectionPoint ip)
        {
        Annotated annotated = ip.getAnnotated();

        if (annotated != null)
            {
            for (Annotation annotation : annotated.getAnnotations())
                {
                MapEventTransformer<K, V, U> transformer = resolve(annotation);
                if (transformer != null)
                    {
                    return transformer;
                    }
                }
            }

        return null;
        }

    @SuppressWarnings({"unchecked", "rawtypes"})
    <K, V, U> MapEventTransformer<K, V, U> resolve(Annotation annotation)
        {
        if (annotation.annotationType().isAnnotationPresent(MapEventTransformerBinding.class))
            {
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
        else if (annotation.annotationType().isAnnotationPresent(ExtractorBinding.class))
            {
            ValueExtractor<Object, Object> extractor = f_extractorProducer.resolve(Collections.singleton(annotation));
            return new ExtractorEventTransformer(extractor);
            }
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
