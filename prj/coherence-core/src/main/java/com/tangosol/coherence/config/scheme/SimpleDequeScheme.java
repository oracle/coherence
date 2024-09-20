/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.oracle.coherence.common.util.Options;

import com.tangosol.coherence.config.builder.MapBuilder;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.internal.net.queue.DefaultNamedQueueDependencies;
import com.tangosol.internal.net.queue.NamedMapDeque;
import com.tangosol.internal.net.queue.NamedQueueDependencies;

import com.tangosol.internal.net.queue.SimpleNamedMapDeque;
import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedCollection;
import com.tangosol.net.NamedQueue;
import com.tangosol.net.QueueService;
import com.tangosol.net.Service;
import com.tangosol.net.ValueTypeAssertion;
import com.tangosol.net.cache.NearCache;

/**
 * A {@link SimpleDequeScheme} is responsible for building a simple
 * {@link NamedMapDeque} where the queue contents are stored in a
 * single partition.
 */
@SuppressWarnings("rawtypes")
public class SimpleDequeScheme
        extends DistributedScheme
        implements NamedQueueScheme<NamedMapDeque>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link SimpleDequeScheme}.
     */
    public SimpleDequeScheme()
        {
        super(new DefaultNamedQueueDependencies());
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
        return type.isAssignableFrom(NamedMapDeque.class);
        }

    @Override
    public SimpleDequeScheme getNamedCollectionBuilder(Class<? extends NamedCollection> clz, Options<NamedCollection.Option> options)
        {
        if (clz.isAssignableFrom(NamedQueue.class))
            {
            return this;
            }
        return null;
        }

    // ----- ServiceScheme methods ------------------------------------------

    @Override
    public <V> NamedMapDeque realize(ValueTypeAssertion<V> typeConstraint, ParameterResolver resolver, Dependencies deps)
        {
        ExtensibleConfigurableCacheFactory eccf =
                (ExtensibleConfigurableCacheFactory) deps.getConfigurableCacheFactory();

        String                  sQueueName = deps.getCacheName();
        NamedCache<QueueKey, ?> cache      = eccf.ensureCache(sQueueName, null);
        if (cache instanceof NearCache)
            {
            // optimize out the NearCache as we do not do plain gets for a queue
            cache = ((NearCache<QueueKey, ?>) cache).getBackCache();
            }
        return new SimpleNamedMapDeque<>(sQueueName, cache);
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
     * Create a {@link NamedQueueDependencies} based on the values contained in this scheme.
     *
     * @param resolver  the {@link ParameterResolver} to use to resolve configuration values
     * @param loader    the {@link ClassLoader} to use
     *
     * @return  a {@link NamedQueueDependencies} based on the values contained in this scheme
     */
    public NamedQueueDependencies createConfiguration(ParameterResolver resolver, ClassLoader loader)
        {
        return new DefaultNamedQueueDependencies();
        }

    // ----- data members ---------------------------------------------------

    /**
     * A singleton instance of a {@link SimpleDequeScheme}.
     */
    public static final SimpleDequeScheme INSTANCE = new SimpleDequeScheme();
    }
