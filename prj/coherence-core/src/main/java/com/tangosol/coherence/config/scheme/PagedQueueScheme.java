/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.oracle.coherence.common.util.MemorySize;

import com.oracle.coherence.common.util.Options;
import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.builder.NamedEventInterceptorBuilder;
import com.tangosol.coherence.config.builder.UnitCalculatorBuilder;

import com.tangosol.coherence.config.unit.Units;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.config.injection.SimpleInjector;

import com.tangosol.internal.net.queue.DefaultPagedQueueDependencies;
import com.tangosol.internal.net.queue.NamedMapQueue;
import com.tangosol.internal.net.queue.PagedQueue;
import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.internal.net.queue.paged.BinaryPagedNamedQueue;
import com.tangosol.internal.net.queue.paged.PagedNamedQueue;
import com.tangosol.internal.net.queue.paged.PagedQueueKey;
import com.tangosol.internal.net.service.grid.DefaultPagedQueueServiceDependencies;
import com.tangosol.internal.net.queue.PagedQueueDependencies;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedCollection;
import com.tangosol.net.NamedQueue;
import com.tangosol.net.Service;
import com.tangosol.net.QueueService;
import com.tangosol.net.ValueTypeAssertion;

import com.tangosol.net.cache.NearCache;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ResourceResolver;
import com.tangosol.util.ResourceResolverHelper;

import java.util.List;

/**
 * A {@link PagedQueueScheme} is responsible for building a queue.
 */
@SuppressWarnings("rawtypes")
public class PagedQueueScheme
        extends DistributedScheme
        implements NamedQueueScheme<NamedMapQueue>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link PagedQueueScheme}.
     */
    public PagedQueueScheme()
        {
        super(new DefaultPagedQueueServiceDependencies());
        }

    // ----- ServiceScheme interface  ---------------------------------------

    /**
     * Return the service type.
     */
    @Override
    public String getServiceType()
        {
        return "DistributedQueue";
        }

    // ----- QueueScheme methods --------------------------------------------

    @Override
    public <T extends NamedCollection> boolean realizes(Class<T> type)
        {
        return type.isAssignableFrom(PagedNamedQueue.class);
        }

    @Override
    public PagedQueueScheme getNamedCollectionBuilder(Class<? extends NamedCollection> clz, Options<NamedCollection.Option> options)
        {
        if (clz.isAssignableFrom(NamedQueue.class))
            {
            return this;
            }
        return null;
        }

    /**
     * Return the binary limit of the size of a page in a queue. Contains the target number
     * of bytes that can be placed in a page before the page is deemed to be full.
     * Legal values are positive integers.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the page size
     */
    public Units getPageSize(ParameterResolver resolver)
        {
        return m_exprPageSize.evaluate(resolver);
        }

    /**
     * Set the page size.
     *
     * @param expr  the page high units expression
     */
    @Injectable("page-size")
    public void setPageSize(Expression<Units> expr)
        {
        m_exprPageSize = expr;
        }

    @Override
    @Injectable("interceptors")
    public void setEventInterceptorBuilders(List<NamedEventInterceptorBuilder> listBuilders)
        {
        super.setEventInterceptorBuilders(listBuilders);
        }

    // ----- ServiceScheme methods ------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public <V> NamedMapQueue<?, V> realize(ValueTypeAssertion<V> typeConstraint, ParameterResolver resolver, Dependencies deps)
        {
        ExtensibleConfigurableCacheFactory eccf =
                (ExtensibleConfigurableCacheFactory) deps.getConfigurableCacheFactory();

        NamedMapQueue pagedQueue;

        String sQueueName = deps.getCacheName();
        if (NullImplementation.getClassLoader().equals(deps.getClassLoader()))
            {
            pagedQueue = (NamedMapQueue) new BinaryPagedNamedQueue(sQueueName, eccf);
            }
        else
            {
            NamedCache<PagedQueueKey, V> cache = eccf.ensureCache(sQueueName, null);
            if (cache instanceof NearCache)
                {
                // optimize out the NearCache as we do not do plain gets for a queue
                cache = ((NearCache<PagedQueueKey, V>) cache).getBackCache();
                }
            pagedQueue = new PagedNamedQueue<>(sQueueName, cache);
            }
        return pagedQueue;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Ensure service and its queue configuration.
     * <p>
     * DistributedQueueConfiguration is registered in corresponding service's resource registry.
     *
     * @param resolver       the ParameterResolver
     * @param deps           the {@link MapBuilder} dependencies
     *
     * @return corresponding QueueService for this scheme
     */
    public QueueService ensureConfiguredService(ParameterResolver resolver, Dependencies deps)
        {
        return getOrEnsureService(deps);
        }

    /**
     * Get or ensure service corresponding to this scheme.
     * <p>
     * Optimized to avoid ensureService synchronization on cluster and service
     * when possible. This behavior is required on server side. Intermittent deadlock occurs
     * calling ensureService on server side from inside service implementation.
     *
     * @return {@link QueueService}
     */
    private QueueService getOrEnsureService(Dependencies deps)
        {
        ExtensibleConfigurableCacheFactory eccf =
                (ExtensibleConfigurableCacheFactory) deps.getConfigurableCacheFactory();

        Service service = CacheFactory.getCluster().getService(getScopedServiceName());

        if (service == null)
            {
            service = eccf.ensureService(this);
            }

        if (service instanceof QueueService)
            {
            return (QueueService) service;
            }
        else
            {
            throw new IllegalArgumentException("Error: the configured service " + service.getInfo().getServiceName()
                    + " is not a QueueService");
            }
        }

    /**
     * Create a {@link PagedQueueDependencies} based on the values contained in this scheme.
     *
     * @param resolver  the {@link ParameterResolver} to use to resolve configuration values
     * @param loader    the {@link ClassLoader} to use
     *
     * @return  a {@link PagedQueueDependencies} based on the values contained in this scheme
     */
    public PagedQueueDependencies createConfiguration(ParameterResolver resolver, ClassLoader loader)
        {
        SimpleInjector   injector         = new SimpleInjector();
        ResourceResolver resourceResolver = ResourceResolverHelper.resourceResolverFrom(PagedQueueScheme.class, this);

        injector.inject(this, ResourceResolverHelper.resourceResolverFrom(resourceResolver, resourceResolver));

        Units   pageSize    = getPageSize(resolver);
        long    cbPage      = pageSize.getUnitCount();

        if (cbPage <= 0)
            {
            // if page size not set use the calculators page size
            cbPage = PagedQueue.DEFAULT_PAGE_CAPACITY_BYTES;
            }
        else if (cbPage > Integer.MAX_VALUE)
            {
            cbPage = Integer.MAX_VALUE;
            }

        DefaultPagedQueueDependencies dependencies = new DefaultPagedQueueDependencies();
        dependencies.setPageCapacity((int) cbPage);

        return dependencies;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The page capacity
     */
    private Expression<Units> m_exprPageSize = new LiteralExpression<>(new Units(new MemorySize(PagedQueue.DEFAULT_PAGE_CAPACITY_BYTES)));

    /**
     * A singleton instance of the {@link PagedQueueScheme}.
     */
    public static final PagedQueueScheme INSTANCE = new PagedQueueScheme();
    }
