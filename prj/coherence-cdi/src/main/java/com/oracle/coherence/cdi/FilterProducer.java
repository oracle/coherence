/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.QueryHelper;

import java.lang.annotation.Annotation;

import java.util.ArrayList;
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
 * A CDI bean that produces {@link com.tangosol.util.Filter} instances using
 * {@link FilterFactory} beans annotated with {@link FilterBinding}
 * annotations.
 *
 * @author Jonathan Knight  2019.10.24
 * @since 20.06
 */
@ApplicationScoped
class FilterProducer
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code FilterProducer} instance.
     * 
     * @param beanManager  a {@code BeanManager} to use
     * @param filterFactoryResolver     a {@code FilterFactoryResolver} to use
     */
    @Inject
    FilterProducer(BeanManager beanManager, FilterFactoryResolver filterFactoryResolver)
        {
        f_beanManager           = beanManager;
        f_filterFactoryResolver = filterFactoryResolver;
        }

    // ---- producer methods ------------------------------------------------

    /**
     * Produces {@link Filter} based on injection point metadata.
     *
     * @param injectionPoint  an injection point
     * @param <T>             the type of objects to filter
     *
     * @return a {@link Filter} instance
     */
    @Produces
    @SuppressWarnings("unchecked")
    <T> com.tangosol.util.Filter<T> getFilter(InjectionPoint injectionPoint)
        {
        List<Filter<?>> list = new ArrayList<>();
        Annotated annotated = injectionPoint.getAnnotated();

        if (annotated != null)
            {
            for (Annotation annotation : annotated.getAnnotations())
                {
                if (annotation.annotationType().isAnnotationPresent(FilterBinding.class))
                    {
                    Class<? extends FilterFactory> clazz = f_filterFactoryResolver.resolve(annotation);
                    if (clazz != null)
                        {
                        FilterFactory supplier = f_beanManager.createInstance().select(clazz).get();
                        if (supplier != null)
                            {
                            list.add(supplier.create(annotation));
                            }
                        }
                    else
                        {
                        throw new DefinitionException("Unsatisfied dependency - no FilterFactory bean found annotated with " + annotation);
                        }
                    }
                }
            }

        Filter[] aFilters = list.toArray(new Filter[0]);

        if (aFilters.length == 0)
            {
            return Filters.always();
            }
        else if (aFilters.length == 1)
            {
            return aFilters[0];
            }
        else
            {
            return Filters.all(aFilters);
            }
        }

    // ---- inner class: AlwaysFilterSupplier -------------------------------

    /**
     * A {@link FilterFactory} that produces {@link com.tangosol.util.filter.AlwaysFilter}
     * instances.
     */
    @AlwaysFilter
    @ApplicationScoped
    static class AlwaysFilterSupplier
            implements FilterFactory<AlwaysFilter, Object>
        {
        @Override
        public Filter<Object> create(AlwaysFilter annotation)
            {
            return Filters.always();
            }
        }

    // ---- inner class: WhereFilterSupplier --------------------------------

    /**
     * A {@link FilterFactory} that produces {@link com.tangosol.util.Filter}
     * instances from a CohQL where clause.
     */
    @WhereFilter("")
    @ApplicationScoped
    static class WhereFilterSupplier
            implements FilterFactory<WhereFilter, Object>
        {
        @Override
        @SuppressWarnings("unchecked")
        public Filter<Object> create(WhereFilter annotation)
            {
            String where = annotation.value();
            if (where.trim().isEmpty())
                {
                return Filters.always();
                }
            return QueryHelper.createFilter(where);
            }
        }

    // ---- inner class: FilterFactoryResolver ------------------------------

    /**
     * A resolver of {@link FilterFactory} bean classes for a given {@link
     * FilterBinding} annotation.
     */
    static class FilterFactoryResolver
        {
        /**
         * Construct {@code FilterFactoryResolver} instance.
         *
         * @param mapFilterFactory  the map of filter bindings to filter factories
         */
        FilterFactoryResolver(Map<AnnotationInstance, Class<? extends FilterFactory<?, ?>>> mapFilterFactory)
            {
            m_mapFilterFactory = mapFilterFactory;
            }

        /**
         * Obtain the {@link FilterFactory} class for a given {@link
         * FilterBinding} annotation.
         *
         * @param annotation the filter binding to obtain the {@link FilterFactory}
         *                   class for
         * @param <A>        the type of the {@link FilterBinding} annotation
         *
         * @return the {@link FilterFactory} class for a given {@link
         * FilterBinding} annotation
         */
        @SuppressWarnings("unchecked")
        <A extends Annotation> Class<? extends FilterFactory<A, ?>> resolve(A annotation)
            {
            AnnotationInstance instance = AnnotationInstance.create(annotation);
            return (Class<? extends FilterFactory<A, ?>>) m_mapFilterFactory.get(instance);
            }

        // ---- data members ------------------------------------------------

        /**
         * The map of filter bindings to filter factories.
         */
        private Map<AnnotationInstance, Class<? extends FilterFactory<?, ?>>> m_mapFilterFactory;
        }

    // ---- data members ----------------------------------------------------
    
    /**
     * The current Bean Manager.
     */
    private final BeanManager f_beanManager;

    /**
     * The resolver that can resolve a {@link FilterFactory} bean from a {@link
     * FilterBinding} annotation.
     */
    private final FilterFactoryResolver f_filterFactoryResolver;
    }
