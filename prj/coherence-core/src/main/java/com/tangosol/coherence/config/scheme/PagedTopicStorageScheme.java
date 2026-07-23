/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriptionsBackingMap;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.util.ObservableMap;

import java.util.Map;

/**
 * A scheme that builds the inner scheme of the backing map scheme of a topic.
 *
 * @author jk 2015.05.29
 * @since Coherence 14.1.1
 */
public class PagedTopicStorageScheme
        extends WrapperCachingScheme
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link PagedTopicStorageScheme}.
     *
     * @param schemeStorage  the {@link CachingScheme} defining the storage scheme for the topic
     * @param topicScheme    the {@link PagedTopicScheme} defining the topic
     */
    public PagedTopicStorageScheme(CachingScheme schemeStorage, PagedTopicScheme topicScheme)
        {
        super(schemeStorage);
        f_schemeTopic = topicScheme;
        }

    // ----- CachingScheme methods ------------------------------------------

    @Override
    public Map realizeMap(ParameterResolver resolver, Dependencies dependencies)
        {
        CacheService   service = f_schemeTopic.ensureConfiguredService(resolver, dependencies);
        ObservableMap  map     = (ObservableMap) super.realizeMap(resolver, dependencies);
        String         sName   = (String) resolver.resolve("cache-name").getExpression().evaluate(resolver);

        if (PagedTopicCaches.Names.SUBSCRIPTIONS.equals(PagedTopicCaches.Names.fromCacheName(sName)))
            {
            BackingMapManagerContext context = service.getBackingMapManager().getContext();
            map = new PagedTopicSubscriptionsBackingMap(map, context);
            }
        return map;
        }

    @Override
    public NamedCache realizeCache(ParameterResolver resolver, Dependencies dependencies)
        {
        f_schemeTopic.ensureConfiguredService(resolver, dependencies);

        return super.realizeCache(resolver, dependencies);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link PagedTopicStorageScheme} defining the topic configuration.
     */
    private final PagedTopicScheme f_schemeTopic;
    }
