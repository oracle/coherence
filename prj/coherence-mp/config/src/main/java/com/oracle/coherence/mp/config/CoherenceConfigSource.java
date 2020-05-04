/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.mp.config;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheFactoryBuilder;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.application.LifecycleEvent;

import java.util.Collections;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * An implementation of {@link org.eclipse.microprofile.config.spi.ConfigSource}
 * that reads configuration properties from a Coherence cache.
 * <p/>
 * Be default, this config source have the ordinal of 500 (which implies that
 * any config properties in it will override properties with the same name from
 * standard MP config sources (files, environment variables and Java system
 * properties). The ordinal value can be changed via {@code
 * coherence.mp.configsource.ordinal} config property.
 * <p/>
 *
 * @author Aleks Seovic  2019.10.12
 * @since Coherence 14.1.2
 */
public class CoherenceConfigSource
        implements ConfigSource, EventInterceptor<LifecycleEvent>
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

    /**
     * Activate {@code CoherenceConfigSource} if present in the specified {@link
     * org.eclipse.microprofile.config.Config config}.
     *
     * @param config  configuration object to activate source in
     */
    public static void activate(Config config)
        {
        for (ConfigSource source : config.getConfigSources())
            {
            if (source instanceof CoherenceConfigSource)
                {
                ((CoherenceConfigSource) source).activate();
                }
            }
        }

    // ---- ConfigSource interface ------------------------------------------

    @Override
    public Map<String, String> getProperties()
        {
        return m_configCache == null
               ? Collections.emptyMap()
               : Collections.unmodifiableMap(m_configCache);
        }

    @Override
    public int getOrdinal()
        {
        return m_nOrdinal;
        }

    @Override
    public String getValue(String key)
        {
        return m_configCache == null ? null : m_configCache.get(key);
        }

    @Override
    public String getName()
        {
        return "coherence";
        }

    // ---- EventInterceptor interface --------------------------------------

    @Override
    public void onEvent(LifecycleEvent e)
        {
        if (e.getType() == LifecycleEvent.Type.ACTIVATED)
            {
            Config config = ConfigProvider.getConfig();
            activate(config);
            }
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Activate this config source.
     */
    synchronized CoherenceConfigSource activate()
        {
        CacheFactoryBuilder cfb = CacheFactory.getCacheFactoryBuilder();
        ConfigurableCacheFactory ccf = cfb.getConfigurableCacheFactory(CACHE_CONFIG, null);
        try
            {
            ccf.activate();
            }
        catch (IllegalStateException ignore)
            {
            // ignore exception thrown if the CCF is already active
            }

        m_configCache = ccf.ensureCache("config", null);
        return this;
        }

    /**
     * For testing.
     *
     * @return internal config cache
     */
    NamedCache<String, String> getConfigCache()
        {
        return m_configCache;
        }

    // ---- data members ----------------------------------------------------

    /**
     * Default ordinal for this {@link ConfigSource}.
     */
    private static final int DEFAULT_ORDINAL = 500;

    /**
     * The name of the system property that can be used to change the ordinal
     * for this {@link ConfigSource}.
     */
    public static final String ORDINAL_PROPERTY = "coherence.mp.config.source.ordinal";

    /**
     * The name of internal cache configuration file that defines cache service
     * and a cache that will be used to store config properties.
     */
    private static final String CACHE_CONFIG = "com/oracle/coherence/mp/config/cache-config.xml";

    /**
     * An ordinal/priority for this {@link ConfigSource}.
     */
    private final int m_nOrdinal;

    /**
     * The cache used to store configuration properties.
     */
    private volatile NamedCache<String, String> m_configCache;
    }
