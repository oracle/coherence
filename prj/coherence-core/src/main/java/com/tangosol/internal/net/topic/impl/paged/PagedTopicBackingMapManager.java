/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.coherence.config.scheme.PagedTopicScheme;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.internal.net.topic.impl.paged.statistics.PagedTopicStatistics;

import com.tangosol.net.ExtensibleConfigurableCacheFactory;

import com.tangosol.net.topic.TopicBackingMapManager;

import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The {@link TopicBackingMapManager} for a paged topic.
 *
 * @author Jonathan Knight 2022.09.10
 * @since 23.03
 */
@SuppressWarnings("rawtypes")
public class PagedTopicBackingMapManager
        extends TopicBackingMapManager<PagedTopicDependencies, PagedTopicScheme>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link PagedTopicBackingMapManager}.
     *
     * @param eccf the owning {@link ExtensibleConfigurableCacheFactory}
     */
    public PagedTopicBackingMapManager(ExtensibleConfigurableCacheFactory eccf)
        {
        super(eccf);
        }

    // ----- TopicBackingMapManager methods ---------------------------------

    @Override
    public PagedTopicScheme findTopicScheme(String sName)
        {
        return (PagedTopicScheme) getCacheFactory().getCacheConfig().findSchemeByTopicName(sName);
        }
    
    @Override
    public PagedTopicDependencies getTopicDependencies(String sTopicName)
        {
        PagedTopicDependencies deps = m_mapDeps.get(sTopicName);
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
        m_lock.lock();
        try
            {
            Map    map        = super.instantiateBackingMap(sName);
            String sTopicName = PagedTopicCaches.Names.getTopicName(sName);
            ensureStatistics(sTopicName);
            return map;
            }
        finally
            {
            m_lock.unlock();
            }
        }

    @Override
    public void releaseBackingMap(String sName, Map map)
        {
        m_lock.lock();
        try
            {
            super.releaseBackingMap(sName, map);
            String sTopicName = PagedTopicCaches.Names.getTopicName(sName);
            m_mapDeps.remove(sTopicName);
            m_mapStatistics.remove(sTopicName);
            }
        finally
            {
            m_lock.unlock();
            }
        }

    // ----- PagedTopicBackingMapManager methods ----------------------------

    /**
     * Returns the {@link PagedTopicStatistics} for a topic.
     *
     * @param sTopicName  the name of the topic
     *
     * @return the {@link PagedTopicStatistics} for the topic of {@code null}
     *         if no statistics exist for the topic
     */
    public PagedTopicStatistics getStatistics(String sTopicName)
        {
        PagedTopicStatistics statistics = m_mapStatistics.get(sTopicName);
        if (statistics == null)
            {
            m_lock.lock();
            try
                {
                statistics = ensureStatistics(sTopicName);
                }
            finally
                {
                m_lock.unlock();
                }
            }
        return statistics;
        }

    // ----- helper methods -------------------------------------------------

    private PagedTopicDependencies createTopicDependencies(String sName)
        {
        ClassLoader       loader   = getContextClassLoader();
        ParameterResolver resolver = getCacheFactory().getParameterResolver(sName, loader, getContext(), null);
        PagedTopicScheme  scheme   = findTopicScheme(sName);
        if (scheme == null)
            {
            throw new IllegalStateException("Cannot find paged-topic-scheme for topic " + sName);
            }
        return scheme.createConfiguration(resolver, loader);
        }

    private PagedTopicStatistics createStatistics(PagedTopicDependencies dependencies, String sTopicName)
        {
        return new PagedTopicStatistics(dependencies.getConfiguredChannelCount(), sTopicName);
        }

    private PagedTopicStatistics ensureStatistics(String sTopicName)
        {
        PagedTopicDependencies dependencies = m_mapDeps.computeIfAbsent(sTopicName, this::createTopicDependencies);
        return m_mapStatistics.computeIfAbsent(sTopicName, s -> createStatistics(dependencies, sTopicName));
        }

    // ----- data members ---------------------------------------------------

    /**
     * The lock to use to synchronize access to internal state.
     */
    private final Lock m_lock = new ReentrantLock(true);

    /**
     * A map of {@link PagedTopicDependencies} keyed by topic name.
     */
    private final Map<String, PagedTopicDependencies> m_mapDeps = new HashMap<>();

    /**
     * A map of {@link PagedTopicStatistics} keyed by topic name.
     */
    private final Map<String, PagedTopicStatistics> m_mapStatistics = new HashMap<>();
    }
