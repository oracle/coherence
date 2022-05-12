/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.mp.config;

import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.cdi.events.Activated;
import com.oracle.coherence.cdi.events.MapName;
import com.oracle.coherence.cdi.events.ScopeName;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.net.CacheService;
import com.tangosol.net.Coherence;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedMap;

import com.tangosol.net.events.EventDispatcher;
import com.tangosol.net.events.EventDispatcherAwareInterceptor;
import com.tangosol.net.events.application.LifecycleEvent;

import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;
import com.tangosol.net.events.partition.cache.CacheLifecycleEventDispatcher;
import com.tangosol.net.events.partition.cache.PartitionedCacheDispatcher;
import com.tangosol.util.MapEvent;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;

import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;

import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * An implementation of {@link org.eclipse.microprofile.config.spi.ConfigSource}
 * that reads configuration properties from a Coherence map.
 * <p>
 * Be default, this config source has the ordinal of 500. That implies that
 * any config properties in it will override properties with the same name from
 * standard MP config sources (files, environment variables and Java system
 * properties). The ordinal value can be changed via {@code coherence.config.ordinal}
 * config property.
 * <p>
 * This config source is also mutable. It can be injected into any application 
 * class by the CDI container, and the values can be modified by calling
 * {@link }
 * @author Aleks Seovic  2019.10.12
 * @since 20.06
 */
@ApplicationScoped
public class CoherenceConfigSource
        implements ConfigSource, EventDispatcherAwareInterceptor<CacheLifecycleEvent>
    {
    // ---- ConfigSource interface ------------------------------------------

    /**
     * Construct {@code CoherenceConfigSource} instance.
     */
    public CoherenceConfigSource()
        {
        // create config that uses only default sources
        ConfigProviderResolver resolver = ConfigProviderResolver.instance();
        ConfigBuilder builder = resolver.getBuilder();
        Config config = builder.addDefaultSources().build();

        m_nOrdinal = config.getOptionalValue(ORDINAL_PROPERTY, Integer.class).orElse(DEFAULT_ORDINAL);
        }

    // ---- ConfigSource interface ------------------------------------------

    @Override
    public Map<String, String> getProperties()
        {
        return m_configMap == null
               ? Collections.emptyMap()
               : Collections.unmodifiableMap(m_configMap);
        }

    @Override
    public int getOrdinal()
        {
        return m_nOrdinal;
        }

    @Override
    public String getValue(String key)
        {
        if (m_configMap == null)
            {
            return null;
            }
        CacheService service = m_configMap.getService();
        if (service instanceof PartitionedCache)
            {
            if (((PartitionedCache) service).isServiceThread(true))
                {
                return null;
                }
            }
        return m_configMap.get(key);
        }

    @Override
    public String getName()
        {
        return "CoherenceConfigSource";
        }

    // ---- mutation support ------------------------------------------------

    /**
     * Set the value of a configuration property.
     *
     * @param sKey    configuration property key
     * @param sValue  the new value to set
     *
     * @return the previous value of the specified configuration property
     */
    public String setValue(String sKey, String sValue)
        {
        if (m_configMap == null)
            {
            return null;
            }

        CacheService service = m_configMap.getService();
        if (service instanceof PartitionedCache)
            {
            if (((PartitionedCache) service).isServiceThread(true))
                {
                return null;
                }
            }
        return m_configMap.put(sKey, sValue);
        }

    // ---- property change notification ------------------------------------

    void onPropertyChange(@Observes @ScopeName(Coherence.SYSTEM_SCOPE) @MapName(MAP_NAME) MapEvent<String, String> event)
        {
        ConfigPropertyChanged changed = new ConfigPropertyChanged(event);
        m_propertyChanged.fireAsync(changed);
        m_propertyChanged.fire(changed);
        }

    // ---- lifecycle observer ----------------------------------------------

    void onSystemScopeActivated(@Observes @ScopeName(Coherence.SYSTEM_SCOPE) @Activated LifecycleEvent e)
        {
        ConfigurableCacheFactory ccf = e.getConfigurableCacheFactory();
        ccf.getInterceptorRegistry().registerEventInterceptor(this);
        CompletableFuture.runAsync(() -> ccf.ensureCache(MAP_NAME, null));
        }

    @Override
    public void introduceEventDispatcher(String sIdentifier, EventDispatcher dispatcher)
        {
        if (dispatcher instanceof CacheLifecycleEventDispatcher && ((CacheLifecycleEventDispatcher) dispatcher).getCacheName().equals(MAP_NAME))
            {
            dispatcher.addEventInterceptor("$SYS:Config", this);
            }
        }

    @Override
    @SuppressWarnings("unchecked")
    public void onEvent(CacheLifecycleEvent event)
        {
        if (!event.getCacheName().equals(MAP_NAME))
            {
            return;
            }

        if (event.getType() == CacheLifecycleEvent.Type.CREATED)
            {
            m_configMap = ((PartitionedCacheDispatcher) event.getEventDispatcher()).getBackingMapContext()
                    .getManagerContext().getCacheService().ensureCache(MAP_NAME, Classes.getContextClassLoader());
            }
        }

    /**
     * For testing.
     *
     * @return internal config map
     */
    public NamedMap<String, String> getConfigMap()
        {
        return m_configMap;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The name of the map that is used to store configuration properties.
     */
    private static final String MAP_NAME = "sys$config";

    /**
     * Default ordinal for this {@link ConfigSource}.
     */
    private static final int DEFAULT_ORDINAL = 500;

    /**
     * The name of the system property that can be used to change the ordinal
     * for this {@link ConfigSource}.
     */
    public static final String ORDINAL_PROPERTY = "coherence.config.ordinal";

    /**
     * An ordinal/priority for this {@link ConfigSource}.
     */
    private final int m_nOrdinal;

    /**
     * The map used to store configuration properties.
     */
    private volatile NamedMap<String, String> m_configMap;

    /**
     * An event dispatcher for property change events.
     */
    @Inject
    private Event<ConfigPropertyChanged> m_propertyChanged;
    }
