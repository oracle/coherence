/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.topic;

import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.scheme.CachingScheme;
import com.tangosol.coherence.config.scheme.NamedTopicScheme;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicContentBackingMap;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicContentPartitionedBackingMap;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicPartitionedBackingMap;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriptionsBackingMap;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriptionsPartitionedBackingMap;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;

import java.util.Map;

/**
 * A base class for topic backing map managers.
 *
 * @param <D>  the type of the {@link TopicDependencies} used
 * @param <S>  the type of the {@link NamedTopicScheme} defining the topic
 *
 * @author Jonathan Knight 2002.09.10
 * @since 22.09
 */
public abstract class TopicBackingMapManager<D extends TopicDependencies, S extends NamedTopicScheme>
        extends ExtensibleConfigurableCacheFactory.Manager
    {
    /**
     * Create a {@link TopicBackingMapManager}.
     *
     * @param eccf  the owning {@link ExtensibleConfigurableCacheFactory}
     */
    protected TopicBackingMapManager(ExtensibleConfigurableCacheFactory eccf)
        {
        super(eccf);
        }

    /**
     * Find the {@link NamedTopicScheme} that defines a topic.
     *
     * @param sName  the name of the topic
     *
     * @return the {@link NamedTopicScheme} that defines the topic
     */
    public abstract S findTopicScheme(String sName);

    /**
     * Get the {@link TopicDependencies} for a topic.
     *
     * @param sName  the name of the topic
     *
     * @return the {@link TopicDependencies} for the topic
     */
    public abstract D getTopicDependencies(String sName);

    @Override
    @SuppressWarnings("rawtypes")
    protected Map instantiatePartitionedBackingMap(MapBuilder bldrMap, ParameterResolver resolver,
                                                   MapBuilder.Dependencies dependencies, CachingScheme scheme)
        {
        String sName = (String) resolver.resolve("cache-name").getExpression().evaluate(resolver);
        if (PagedTopicCaches.Names.SUBSCRIPTIONS.isA(sName))
            {
            return instantiatePartitionedBackingMap(bldrMap, resolver, dependencies,
                    scheme, PagedTopicSubscriptionsPartitionedBackingMap::new);
            }
        else if (PagedTopicCaches.Names.CONTENT.isA(sName))
            {
            return instantiatePartitionedBackingMap(bldrMap, resolver, dependencies,
                    scheme, PagedTopicContentPartitionedBackingMap::new);
            }
        return super.instantiatePartitionedBackingMap(bldrMap, resolver, dependencies, scheme);
        }
    }
