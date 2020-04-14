/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.tangosol.util.Extractors;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.MultiExtractor;

import java.lang.annotation.Annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.InjectionPoint;

import javax.inject.Inject;

/**
 * A CDI bean that produces {@link ValueExtractor} instances using {@link
 * ValueExtractorFactory} beans annotated with {@link ValueExtractorBinding}
 * annotations.
 *
 * @author Jonathan Knight  2019.10.25
 */
@ApplicationScoped
class ValueExtractorProducer
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code ValueExtractorProducer} instance.
     *
     * @param beanManager  a {@code BeanManager} to use
     * @param extractorFactoryResolver     a {@code ValueExtractorFactoryResolver} to use
     */
    @Inject
    ValueExtractorProducer(BeanManager beanManager, ValueExtractorFactoryResolver extractorFactoryResolver)
        {
        f_beanManager = beanManager;
        f_extractorFactoryResolver = extractorFactoryResolver;
        }

    // ---- producer methods ------------------------------------------------

    /**
     * Create an instance of a {@link ValueExtractor} based on injection point
     * metadata.
     *
     * @param injectionPoint  the injection point to create an extractor for
     * @param <T>             the type of object to extract the value from
     * @param <E>             the type of extracted value
     *
     * @return an instance of a {@link ValueExtractor}
     */
    @Produces
    @SuppressWarnings("unchecked")
    <T, E> ValueExtractor<T, E> getValueExtractor(InjectionPoint injectionPoint)
        {
        List<ValueExtractor> list      = new ArrayList<>();
        Annotated            annotated = injectionPoint.getAnnotated();

        if (annotated != null)
            {
            for (Annotation annotation : annotated.getAnnotations())
                {
                if (annotation.annotationType().isAnnotationPresent(ValueExtractorBinding.class))
                    {
                    Class<? extends ValueExtractorFactory> clazz = f_extractorFactoryResolver.resolve(annotation);
                    if (clazz != null)
                        {
                        ValueExtractorFactory supplier = f_beanManager.createInstance().select(clazz).get();
                        if (supplier != null)
                            {
                            ValueExtractor extractor = supplier.create(annotation);
                            if (extractor instanceof MultiExtractor)
                                {
                                Collections.addAll(list, ((MultiExtractor) extractor).getExtractors());
                                }
                            else
                                {
                                list.add(extractor);
                                }
                            }
                        }
                    else
                        {
                        throw new DefinitionException(
                                "unsatisfied dependency - no ValueExtractorFactory bean found annotated with " + annotation);
                        }
                    }
                }
            }

        ValueExtractor[] aExtractors = list.toArray(new ValueExtractor[0]);
        if (aExtractors.length == 0)
            {
            return null;
            }
        else if (aExtractors.length == 1)
            {
            return aExtractors[0];
            }
        else
            {
            return Extractors.multi(aExtractors);
            }
        }

    // ---- inner class: UniversalExtractorSupplier -------------------------

    /**
     * A {{@link ValueExtractorFactory} that produces{@link ValueExtractor}
     * instances for a given property or method name.
     */
    @PropertyExtractor("")
    @ApplicationScoped
    static class UniversalExtractorSupplier
            implements ValueExtractorFactory<PropertyExtractor, Object, Object>
        {
        @Override
        public ValueExtractor<Object, Object> create(PropertyExtractor annotation)
            {
            return Extractors.extract(annotation.value());
            }
        }

    // ---- inner class: UniversalExtractorsSupplier ------------------------

    /**
     * A {{@link ValueExtractorFactory} that produces {@link
     * com.tangosol.util.extractor.MultiExtractor} containing {@link
     * ValueExtractor} instances produced from the annotations contained in a
     * {@link PropertyExtractor.Extractors} annotation.
     */
    @PropertyExtractor.Extractors({})
    @ApplicationScoped
    static class UniversalExtractorsSupplier
            implements ValueExtractorFactory<PropertyExtractor.Extractors, Object, Object>
        {
        @Override
        @SuppressWarnings("unchecked")
        public ValueExtractor<Object, Object> create(PropertyExtractor.Extractors annotation)
            {
            ValueExtractor[] extractors = Arrays.stream(annotation.value())
                    .map(f_extractorSupplier::create)
                    .toArray(ValueExtractor[]::new);
            return Extractors.multi(extractors);
            }

        // ---- data members ------------------------------------------------

        /**
         * Extractor supplier.
         */
        private final UniversalExtractorSupplier f_extractorSupplier = new UniversalExtractorSupplier();
        }

    // ---- inner class: ChainedExtractorSupplier ---------------------------

    /**
     * A {{@link ValueExtractorFactory} that produces chained {@link
     * ValueExtractor} instances for an array of property or method names.
     */
    @ChainedExtractor("")
    @ApplicationScoped
    static class ChainedExtractorSupplier
            implements ValueExtractorFactory<ChainedExtractor, Object, Object>
        {
        @Override
        public ValueExtractor<Object, Object> create(ChainedExtractor annotation)
            {
            return Extractors.chained(annotation.value());
            }
        }

    // ---- inner class: ChainedExtractorsSupplier --------------------------

    /**
     * A {{@link ValueExtractorFactory} that produces {@link
     * com.tangosol.util.extractor.MultiExtractor} containing {@link
     * ValueExtractor} instances produced from the annotations contained in a
     * {@link ChainedExtractor.Extractors} annotation.
     */
    @ChainedExtractor.Extractors({})
    @ApplicationScoped
    static class ChainedExtractorsSupplier
            implements
            ValueExtractorFactory<ChainedExtractor.Extractors, Object, Object>
        {
        @Override
        @SuppressWarnings("unchecked")
        public ValueExtractor<Object, Object> create(ChainedExtractor.Extractors annotation)
            {
            ValueExtractor[] extractors = Arrays.stream(annotation.value())
                    .map(f_ExtractorSupplier::create)
                    .toArray(ValueExtractor[]::new);
            return Extractors.multi(extractors);
            }

        // ---- data members ------------------------------------------------

        /**
         * Extractor supplier.
         */
        private final ChainedExtractorSupplier f_ExtractorSupplier = new ChainedExtractorSupplier();
        }

    // ---- inner class: PofExtractorSupplier -------------------------------

    /**
     * A {{@link ValueExtractorFactory} that produces{@link ValueExtractor}
     * instances for a given property or method name.
     */
    @PofExtractor(0)
    @ApplicationScoped
    static class PofExtractorSupplier
            implements ValueExtractorFactory<PofExtractor, Object, Object>
        {
        @Override
        @SuppressWarnings("unchecked")
        public ValueExtractor<Object, Object> create(PofExtractor annotation)
            {
            Class clazz = annotation.type().equals(Object.class)
                          ? null
                          : annotation.type();
            return Extractors.fromPof(clazz, annotation.value());
            }
        }

    // ---- inner class: PofExtractorsSupplier -------------------------------

    /**
     * A {{@link ValueExtractorFactory} that produces {@link
     * com.tangosol.util.extractor.MultiExtractor} containing {@link
     * ValueExtractor} instances produced from the annotations contained in a
     * {@link PofExtractor.Extractors} annotation.
     */
    @PofExtractor.Extractors({})
    @ApplicationScoped
    static class PofExtractorsSupplier
            implements
            ValueExtractorFactory<PofExtractor.Extractors, Object, Object>
        {
        @Override
        @SuppressWarnings("unchecked")
        public ValueExtractor<Object, Object> create(PofExtractor.Extractors annotation)
            {
            ValueExtractor[] extractors = Arrays.stream(annotation.value())
                    .map(f_extractorSupplier::create)
                    .toArray(ValueExtractor[]::new);
            return Extractors.multi(extractors);
            }

        // ---- data members ------------------------------------------------

        /**
         * Extractor supplier.
         */
        private final PofExtractorSupplier f_extractorSupplier = new PofExtractorSupplier();
        }

    // ---- inner class: ValueExtractorFactoryResolver ----------------------

    /**
     * A resolver of {{@link ValueExtractorFactory} bean classes for a given
     * {{@link ValueExtractorBinding} annotation.
     */
    static class ValueExtractorFactoryResolver
        {
        /**
         * Construct {@code ValueExtractorFactoryResolver} instance.
         *
         * @param mapExtractorFactory  the map of extractor bindings to extractor factories
         */
        ValueExtractorFactoryResolver(Map<AnnotationInstance, Class<? extends ValueExtractorFactory<?, ?, ?>>> mapExtractorFactory)
            {
            m_mapExtractorFactory = mapExtractorFactory;
            }

        /**
         * Obtain the {{@link ValueExtractorFactory} class for a given {{@link
         * ValueExtractorBinding} annotation.
         *
         * @param annotation the extractor binding to obtain the {{@link
         *                   ValueExtractorFactory} class for
         * @param <A>        the type of the {{@link ValueExtractorBinding}
         *                   annotation
         *
         * @return the {{@link ValueExtractorFactory} class for a given {{@link
         * ValueExtractorBinding} annotation
         */
        @SuppressWarnings("unchecked")
        <A extends Annotation> Class<? extends ValueExtractorFactory<A, ?, ?>> resolve(A annotation)
            {
            AnnotationInstance instance = AnnotationInstance.create(annotation);
            return (Class<? extends ValueExtractorFactory<A, ?, ?>>) m_mapExtractorFactory.get(instance);
            }

        // ---- data members ----------------------------------------------------

        /**
         * The map of extractor bindings to extractor factories.
         */
        private Map<AnnotationInstance, Class<? extends ValueExtractorFactory<?, ?, ?>>> m_mapExtractorFactory;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The current Bean Manager.
     */
    private final BeanManager f_beanManager;

    /**
     * The resolver that can resolve a {{@link ValueExtractorFactory} bean from
     * a {{@link ValueExtractorBinding} annotation.
     */
    private final ValueExtractorFactoryResolver f_extractorFactoryResolver;
    }
