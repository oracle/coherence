/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic;

import com.tangosol.coherence.config.scheme.NamedTopicScheme;
import com.tangosol.coherence.config.scheme.TopicScheme;
import com.tangosol.config.expression.ParameterResolver;


import com.tangosol.net.ExtensibleConfigurableCacheFactory;

import com.tangosol.net.topic.TopicBackingMapManager;
import com.tangosol.net.topic.TopicDependencies;

import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The {@link com.tangosol.net.topic.TopicBackingMapManager} for a gRPC topic.
 *
 * @author Jonathan Knight  2025.01.01
 */
@SuppressWarnings("rawtypes")
public class DefaultTopicBackingMapManager
        extends TopicBackingMapManager<TopicDependencies, NamedTopicScheme>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link DefaultTopicBackingMapManager}.
     *
     * @param eccf the owning {@link ExtensibleConfigurableCacheFactory}
     */
    public DefaultTopicBackingMapManager(ExtensibleConfigurableCacheFactory eccf)
        {
        super(eccf);
        }

    // ----- TopicBackingMapManager methods ---------------------------------

    @Override
    public NamedTopicScheme findTopicScheme(String sName)
        {
        return getCacheFactory().getCacheConfig().findSchemeByTopicName(sName);
        }
    
    @Override
    public TopicDependencies getTopicDependencies(String sTopicName)
        {
        TopicDependencies deps = m_mapDeps.get(sTopicName);
        if (deps == null)
            {
            m_lock.lock();
            try
                {
                deps = m_mapDeps.computeIfAbsent(sTopicName, this::createTopicDependencies);
                }
            finally
                {
                m_lock.unlock();
                }
            }
        return deps;
        }

    @Override
    public Map instantiateBackingMap(String sName)
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public void releaseBackingMap(String sName, Map map)
        {
        throw new UnsupportedOperationException();
        }

    // ----- helper methods -------------------------------------------------

    private TopicDependencies createTopicDependencies(String sName)
        {
        ClassLoader       loader   = getContextClassLoader();
        ParameterResolver resolver = getCacheFactory().getParameterResolver(sName, loader, null);
        TopicScheme       scheme   = findTopicScheme(sName);
        if (scheme == null)
            {
            throw new IllegalStateException("Cannot find paged-topic-scheme for topic " + sName);
            }
        return scheme.createConfiguration(resolver, loader);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The lock to use to synchronize access to internal state.
     */
    private final Lock m_lock = new ReentrantLock(true);

    /**
     * A map of {@link TopicDependencies} keyed by topic name.
     */
    private final Map<String, TopicDependencies> m_mapDeps = new HashMap<>();
    }
