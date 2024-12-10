/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache;

import com.oracle.coherence.common.base.Disposable;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.application.ContainerContext;

import com.tangosol.coherence.jcache.common.JCacheIdentifier;
import com.tangosol.coherence.jcache.localcache.LocalCache;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;
import com.tangosol.util.Builder;
import com.tangosol.util.RegistrationBehavior;

import java.lang.Override;

import java.lang.ref.WeakReference;

import java.net.URI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import java.util.concurrent.ConcurrentHashMap;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;

import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.Configuration;

import javax.cache.spi.CachingProvider;

/**
 * The Coherence-based implementation of a {@link CacheManager}.
 *
 * @author jf  2013.06.18
 * @author bo  2013.12.17
 */
public class CoherenceBasedCacheManager
        implements CacheManager, Disposable
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a new {@link CoherenceBasedCacheManager}.
     *
     * @param provider     the {@link CachingProvider} that owns the {@link CacheManager}
     * @param ccf          the {@link ConfigurableCacheFactory} to use for {@link CoherenceBasedCache}s
     * @param uri          the {@link URI} of the {@link CacheManager}
     *                     (this must point to a valid Coherence Cache configuration file)
     * @param classLoader  the {@link ClassLoader} to use for loading {@link Cache}
     *                     resources, including keys, values, loaders, writers etc
     * @param properties   the custom configuration {@link Properties}
     *
     */
    public CoherenceBasedCacheManager(CoherenceBasedCachingProvider provider, ConfigurableCacheFactory ccf, URI uri,
                                      ClassLoader classLoader, Properties properties)
        {
        if (provider == null)
            {
            throw new NullPointerException("No CachingProvider specified");
            }

        if (ccf == null)
            {
            throw new NullPointerException("No Configurable Cache Factory specified");
            }

        if (classLoader == null)
            {
            throw new NullPointerException("No ClassLoader specified");
            }

        m_provider          = provider;
        m_ccf               = ccf;
        m_uri               = uri;
        m_refClassLoader    = new WeakReference<ClassLoader>(classLoader);
        m_properties        = properties == null ? new Properties() : (Properties) properties.clone();
        m_fClosed           = false;
        m_fContainerManaged = ccf instanceof ExtensibleConfigurableCacheFactory
                              && ((ExtensibleConfigurableCacheFactory) ccf).getApplicationContext() != null;

        if (m_fContainerManaged)
            {
            // there is a one to one relationship between an eccf and a container managed CacheManager
            // given current Coherence JCache in Container usage requirements. Those usage requirements
            // ensure all members of container use same classloader for looking up CacheManager by
            // calling Caching.setDefaultClassLoader() in JCacheLifeCycleInterceptor container activation
            // for jcache.
            //
            // Given JCache 1.0 Caching.getCachingProvider() creating a new CachingProvider per classloader
            // differing from ScopedCacheFactoryBuilder#getFactoryInternal looking over classloader hierarchy for default ECCF.
            // In non-container managed/standalonce case, there is potential for one ECCF to many CacheManagers.  For this reason,
            // only use ECCF resource registry for case that there is a one to one mapping. When Caching.getCachingProvider()
            // is called with different ClassLoaders that are in same ClassLoader hierarchy, there definitely
            // will be one CacheProvider/CacheManager per classloader while there only will be one default ECCF
            // across entire ClassLoader hierarchy.
            m_sResourceName = m_ccf.getResourceRegistry().registerResource(CacheManager.class, this);

            Logger.info("Container-managed CacheManager created using ConfigurableCacheFactory=" + m_ccf
                        + " uri=" + m_uri + " classloader=" + classLoader);
            }
        else
            {
            // register disposable CacheManager with CCF resource registry.  If CCF is disposed, CacheManager will be disposed.

            if (ccf instanceof ExtensibleConfigurableCacheFactory)
                {
                final CacheManager    thisOne = this;
                Builder<CacheManager> bldr    = new Builder<CacheManager>()
                    {
                    @Override
                    public CacheManager realize()
                        {
                        return thisOne;
                        }
                    };

                // while desirable to have a one to one relationship between CacheManager and CCF,
                // the ScopedCachedFactoryBuilder.getConfigurableCacheFactory(ClassLoader) searches up ClassLoader parent hierarchy
                // searching for a CCF registered with specified ClassLoader or one of its parents.
                // CacheManager is defined to be mapped one to one with ClassLoader, independent of parent ClassLoader's.

                // 7 JSR 107 TCK test use ClassLoaders that are in same ClassLoader hierarchy, so the following
                // code allows for Coherence implementation of ScopedCacheFactoryBuilder to associate one CCF with
                // a classloader or its parent classloaders.  This results in a one CCF to multiple CacheManager instances.

                // WORKAROUND:
                // register additional CacheManagers with CCF using a unique identifier generated by ALWAYS registration.
                // goal is to have CacheManager associated with CCF so when CCF is destroyed, the cachemanager will be closed at same time.

                m_sResourceName = ccf.getResourceRegistry().registerResource(
                        CacheManager.class, bldr, RegistrationBehavior.ALWAYS, null);
                }
            }
        }

    // ----- CoherenceBasedCacheManager methods -----------------------------

    /**
     * Provide access to meta cache that maps JCache cache names to JCache configuration.
     * The mapping is a string to a JCache Configuration object.
     *
     * @param cacheId JCache unique identifier
     * @return a named cache that maps JCache cache names to JCache configurations.
     */
    public CoherenceBasedCompleteConfiguration getCacheToConfigurationMapping(JCacheIdentifier cacheId)
        {
        return (CoherenceBasedCompleteConfiguration) getConfigurationCache().get(cacheId.getCanonicalCacheName());
        }

    /**
     * put entry with key of sName
     *
     * @param cacheId key of entry to put into _metaJCacheNameToConfig
     */
    public void putCacheToConfigurationMapping(JCacheIdentifier cacheId, CoherenceBasedCompleteConfiguration config)
        {
        CoherenceBasedCompleteConfiguration existingConfig = getCacheToConfigurationMapping(cacheId);

        // NOTE: if there already exist a configuration for cacheIdentifier in the metaconfigurationcache, it
        // indicates either another member already has created the cache and is using it OR another member
        // has created the JCache and exited without cleaning up afterwards.
        // For time being, allowing createCache to succeed iff the configuration for the cache being created exactly
        // matches the configuration for the cacheIdentier in the jcache-configurations cache table (meta cache
        // mapping jcacheIdentifiers to JCache configuration.

        if (existingConfig != null && !existingConfig.equals(config))
            {
            throw new IllegalStateException("CacheCreationFailure: Failed to create cache named "
                + cacheId.getCanonicalCacheName() + " with configuration: " + config + "\n"
                + "A cache with that name already exists with the different configuration: " + existingConfig);
            }

        getConfigurationCache().put(cacheId.getCanonicalCacheName(), config);
        }

    /**
     * remove entry with key of sName
     *
     * @param cacheId key of entry to remove from _metaJCacheNameToConfig
     */
    public void removeCacheToConfigurationMapping(JCacheIdentifier cacheId)
        {
        getConfigurationCache().remove(cacheId.getCanonicalCacheName());
        }

    /**
     * Obtains the {@link NamedCache} to be used for storing
     * {@link CoherenceBasedConfiguration}s when they need to be shared across a cluster
     * or made available to clients.
     *
     * @return  the {@link NamedCache} for {@link CoherenceBasedConfiguration}s
     */
    private NamedCache getConfigurationCache()
        {
        // we don't specify a serializer to allow Coherence to use the service-level classloader
        ClassLoader loader = null;

        // acquire the NamedCache that holds the JCache Configurations
        return m_ccf.ensureCache(CoherenceBasedCache.JCACHE_CONFIG_CACHE_NAME, loader);
        }

    // ----- CacheManager interface -----------------------------------------

    @Override
    public CachingProvider getCachingProvider()
        {
        return m_provider;
        }

    @Override
    public URI getURI()
        {
        return m_uri;
        }

    @Override
    public Properties getProperties()
        {
        return m_properties;
        }

    @Override
    public ClassLoader getClassLoader()
        {
        return m_refClassLoader.get();
        }

    @Override
    public <K, V, C extends Configuration<K, V>> Cache<K, V> createCache(String sJCacheName, C cfgJCache)
            throws IllegalArgumentException
        {
        ensureOpen();

        if (sJCacheName == null)
            {
            throw new NullPointerException("cacheName must not be null");
            }

        if (cfgJCache == null)
            {
            throw new NullPointerException("config must not be null");
            }

        if (cfgJCache instanceof CoherenceBasedConfiguration)
            {
            // make this instance thread safe, only allow one create cache at any point in time.
            synchronized (this)
                {
                CoherenceBasedConfiguration<K, V> cfgCoherence = (CoherenceBasedConfiguration) cfgJCache;
                CoherenceBasedCache<?, ?>         cache        = m_mapNameToJCache.get(sJCacheName);

                if (cache == null)
                    {
                    CoherenceBasedCache<K, V> cacheCreated = cfgCoherence.createCache(this, sJCacheName);

                    cache = m_mapNameToJCache.putIfAbsent(sJCacheName, cacheCreated);

                    if (cache == null)
                        {
                        // put succeeded, return the newCache which we would get with a m_mapNameToJCache.get(cacheName)
                        return cacheCreated;
                        }
                    }
                }
            }
        else if (cfgJCache instanceof Configuration)
            {
            CoherenceBasedConfiguration<K, V> cfgCoherence = m_provider.convertConfiguration(cfgJCache,
                                                                 getClassLoader());

            return createCache(sJCacheName, cfgCoherence);
            }
        else
            {
            throw new CacheException("Cache creation failed due to unknown Configuration type: "
                                     + cfgJCache.getClass().getName());
            }

        // unable to create a cache since one already existed.
        throw new CacheException("A cache named " + sJCacheName + " already exists.");
        }

    @Override
    public <K, V> Cache<K, V> getCache(String sJCacheName, Class<K> clzKey, Class<V> clzType)
        {
        if (isClosed())
            {
            throw new IllegalStateException();
            }

        if (clzKey == null)
            {
            throw new NullPointerException("clzKey can not be null");
            }

        if (clzType == null)
            {
            throw new NullPointerException("clzType can not be null");
            }

        Cache<?, ?>         cache         = m_mapNameToJCache.get(sJCacheName);
        Configuration<?, ?> configuration = cache == null ?
                getCacheToConfigurationMapping(new JCacheIdentifier(getURI().toString(), sJCacheName)) :
                cache.getConfiguration(Configuration.class);

        if (cache == null)
            {
            if (configuration == null)
                {
                return null;
                }
            else
                {
                // cache was previously closed, create a new Coherence JCache wrapper
                cache = createCache(sJCacheName, configuration);
                }
            }


        if (configuration.getKeyType() != null && configuration.getKeyType().equals(clzKey))
            {
            if (configuration.getValueType() != null && configuration.getValueType().equals(clzType))
                {
                return (Cache<K, V>) cache;
                }
            else
                {
                throw new ClassCastException("Incompatible cache value types specified, expected "
                        + configuration.getValueType() + " but " + clzType + " was specified");
                }
            }
        else
            {
            throw new ClassCastException("Incompatible cache key types specified, expected "
                    + configuration.getKeyType() + " but " + clzKey + " was specified");
            }
        }

    @Override
    public Cache getCache(String sJCacheName)
        {
        /**
         * https://github.com/jsr107/jsr107spec/issues/340
         * in 1.1 relaxed {@link CacheManager#getCache(String)} to not enforce a check.
         */
        return getCacheInternal(sJCacheName, false);
        }

    @Override
    public Iterable<String> getCacheNames()
        {
        // Added check to require this to not be closed for JSR 107 1.1.0
        ensureOpen();

        return Collections.unmodifiableSet(new TreeSet<String>(m_mapNameToJCache.keySet()));
        }

    @Override
    public void destroyCache(String sName)
        {
        ensureOpen();

        if (sName == null)
            {
            throw new NullPointerException();
            }

        CoherenceBasedCache<?, ?> cache = m_mapNameToJCache.remove(sName);

        // allow for destroy after cache has been closed in same process.
        // This works around TCK case usage of closing cache before destroying cache.
        if (cache == null)
            {
            // cache.close() allows for multiple invocations.
            // closing a closed cache is no-op.
            WeakReference<CoherenceBasedCache<?, ?>> refCache = m_closedMapNameToJCache.remove(sName);

            cache = refCache == null ? null : refCache.get();
            }

        if (cache == null)
            {
            // cover case that cache was created in another jvm and there is no local reference to it.
            JCacheIdentifier                    id     = new JCacheIdentifier(getURI().toString(), sName);
            CoherenceBasedCompleteConfiguration config = getCacheToConfigurationMapping(id);

            if (config != null)
                {
                config.destroyCache(this, sName);
                }
            }
        else
            {
            cache.close();
            cache.destroy();
            }
        }

    @Override
    public void enableStatistics(String sName, boolean fEnabled)
        {
        ensureOpen();

        if (sName == null)
            {
            throw new NullPointerException();
            }

        @SuppressWarnings("unchecked") final AbstractCoherenceBasedCache cache =
            (AbstractCoherenceBasedCache) getCacheInternal(sName, false);

        if (cache != null)
            {
            cache.setStatisticsEnabled(fEnabled);
            }
        else
            {
            throw new CacheException("no cache named " + sName);
            }
        }

    @Override
    public void enableManagement(String sName, boolean fEnabled)
        {
        ensureOpen();

        if (sName == null)
            {
            throw new NullPointerException();
            }

        @SuppressWarnings("unchecked") final AbstractCoherenceBasedCache cache =
            (AbstractCoherenceBasedCache) getCacheInternal(sName, false);

        if (cache != null)
            {
            cache.setManagementEnabled(fEnabled);
            }
        else
            {
            throw new CacheException("no cache named " + sName);
            }
        }

    @Override
    public synchronized void close()
        {
        if (isClosed())
            {
            return;
            }

        m_fClosed = true;

        // first release the CacheManager from the CacheProvider so that
        // future requests for this CacheManager won't return this one
        m_provider.release(getClassLoader(), getURI());

        ArrayList<Cache<?, ?>> cacheList;

        cacheList = new ArrayList<Cache<?, ?>>(m_mapNameToJCache.values());
        m_mapNameToJCache.clear();

        for (Cache<?, ?> cache : cacheList)
            {
            try
                {
                cache.close();
                }
            catch (Exception e)
                {
                Logger.warn("Error stopping cache " + cache + ": " + Base.printStackTrace(e));
                }
            }

        // Only manage ECCF for non container-managed Coherence JCache CacheManager.
        // Container deactivation manages eccf lifecycle and disposes of container-managed
        // cachemanager at same time.
        //
        // Without this step, JCache TCK and JCache functional test have issues
        // when using multiple cache configs in one jvm.
        if (!isContainerManaged())
            {
            // unregister this CacheManager from eccf.
            m_ccf.getResourceRegistry().unregisterResource(CacheManager.class, m_sResourceName);

            // only dispose eccf if no more CacheManagers referencing the eccf.
            if (m_ccf.getResourceRegistry().getResource(CacheManager.class) == null)
                {
                CacheFactory.getCacheFactoryBuilder().release(m_ccf);
                m_ccf.dispose();
                }
            }

        m_ccf = null;
        }

    @Override
    public void dispose()
        {
        if (!isClosed())
            {
            close();
            }
        }

    @Override
    public boolean isClosed()
        {
        return m_fClosed;
        }

    @Override
    public <T> T unwrap(Class<T> clz)
        {
        if (clz != null && clz.isInstance(m_ccf))
            {
            return (T) m_ccf;
            }
        else if (clz != null && clz.isInstance(this))
            {
            return (T) this;
            }
        else
            {
            throw new IllegalArgumentException("Unsupported unwrap(" + clz + ")");
            }
        }

    // ----- CoherenceBasedCacheManager methods -----------------------------

    /**
     * Return the domain partition if it exists and it is not the global domain partition.
     *
     * @return non-global domain partition if it exists for m_ccf
     */
    public String getDomainPartition()
        {
        String sResult = null;

        if (m_ccf instanceof ExtensibleConfigurableCacheFactory)
            {
            ContainerContext ctx = ((ExtensibleConfigurableCacheFactory) m_ccf).getContainerContext();

            sResult = ctx == null || ctx.isGlobalDomainPartition() ? null : ctx.getDomainPartition();
            }

        return sResult;
        }

    /**
     * Convenience and short cut method to get container-managed CacheManager of an ECCF.
     * @param eccf {@link com.tangosol.net.ExtensibleConfigurableCacheFactory}
     * @return  the container managed CacheManager of this ECCF if one exists.
     */
    public static CacheManager getContainerManagedCacheManager(ExtensibleConfigurableCacheFactory eccf)
        {
        return eccf == null || eccf.getApplicationContext() == null
               ? null : eccf.getResourceRegistry().getResource(CacheManager.class);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Lookup JCache Adapter <code>sName</code>in current {@link CacheManager} context.
     *
     * @param sName JCache Adapter map name
     * @param fTypeCheck true iff the map type should be checked
     * @param <K> key type
     * @param <V> value type
     *
     * @return JCache {@link Cache} if found, null otherwise.
     */
    private <K, V> Cache<K, V> getCacheInternal(String sName, boolean fTypeCheck)
        {
        ensureOpen();

        /*
         * Can't really verify that the K/V cast is safe but it is required by the API, using a
         * local variable for the cast to allow for a minimal scoping of @SuppressWarnings
         */
        @SuppressWarnings("unchecked") Cache<K, V> cache = (Cache<K, V>) m_mapNameToJCache.get(sName);

        // when cache is null, still a chance that the content still exists if JCache cache was closed but not destroyed.
        Configuration<?, ?>     configuration = cache == null ?
                getCacheToConfigurationMapping(new JCacheIdentifier(getURI().toString(), sName)) : cache.getConfiguration(Configuration.class);

        if (cache == null)
            {
            if (configuration == null)
                {
                return null;
                }
            else
                {
                // cache was previously closed, create a new Coherence JCache wrapper for existing cache content
                cache = (Cache<K, V>) createCache(sName, configuration);
                }
            }

        if (!fTypeCheck || (configuration.getKeyType().equals(Object.class)
                    && configuration.getValueType().equals(Object.class)))
            {
            return cache;
            }
        else
            {
            throw new IllegalArgumentException("Cache " + sName + " was " + "defined with specific types Cache<" + configuration.getKeyType() + ", " + configuration.getValueType() + "> "
                    + "in which case CacheManager.getCache(String, Class, Class) must be used");
            }
        }

    /**
     * Ensure open context.
     */
    private void ensureOpen()
        {
        if (isClosed())
            {
            throw new IllegalStateException();
            }
        }

    /**
     * Get ConfigurableCacheFactory context.
     *
     * @return {@link ConfigurableCacheFactory}
     */
    public ConfigurableCacheFactory getConfigurableCacheFactory()
        {
        ensureOpen();

        return m_ccf;
        }

    /**
     * release cache named <code>sName</code>
     *
     * @param sName JCache Adapter map to release
     */
    public void releaseCache(String sName)
        {
        CoherenceBasedCache<?, ?> closedCache = m_mapNameToJCache.remove(sName);

        if (closedCache != null)
            {
            m_closedMapNameToJCache.put(sName, new WeakReference<CoherenceBasedCache<?, ?>>(closedCache));
            }
        }

    /**
     * Validate if configurations map is valid with this instance.
     * @return true iff it is valid
     */
    public boolean validate()
        {
        boolean result = true;

        if (m_mapNameToJCache.size() > 0)
            {
            for (Map.Entry<String, CoherenceBasedCache<?, ?>> entry : m_mapNameToJCache.entrySet())
                {
                CoherenceBasedCache<?, ?> cache = entry.getValue();

                if (cache instanceof LocalCache)
                    {
                    // nothing to check.  not stored in cache to configuration mapping.
                    continue;
                    }

                // validate partitioned cache
                JCacheIdentifier      cacheId = cache.getIdentifier();
                CompleteConfiguration config  = getCacheToConfigurationMapping(cacheId);

                if (config == null)
                    {
                    result = false;
                    Logger.warn("CoherenceBasedCacheManager.validate failed.  No mapping for JCache " + cacheId
                                     + " in meta mapping of jcacheId to configuration.");
                    break;
                    }
                else if (!config.equals(cache.getConfiguration(CompleteConfiguration.class)))
                    {
                    result = false;

                    CoherenceBasedCompleteConfiguration inMemoryConfiguration =
                        cache.getConfiguration(CoherenceBasedCompleteConfiguration.class);

                    Logger.warn("CoherenceBasedCacheManager.validate failed due to differing Configurations. JCache "
                             + cacheId + "\nInMemory Configuration is:\n" + inMemoryConfiguration
                             + "\n replicated meta cache configuration is:\n" + config);
                    break;
                    }
                }
            }

        return result;
        }

    /**
     * Return whether this {@link CoherenceBasedCacheManager} is container managed.
     * @return true iff this {@link CoherenceBasedCacheManager} is container managed.
     */
    public boolean isContainerManaged()
        {
        return m_fContainerManaged;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder();

        sb.append("CoherenceBasedCacheManager uri=").append(m_uri).append(" clzLdr=").append(getClassLoader())
            .append(" container-managed=").append(isContainerManaged()).append(" eccf=")
            .append(getConfigurableCacheFactory()).append(" isclosed=").append(isClosed());

        return sb.toString();
        }

    // ----- data members ---------------------------------------------------

    private final ConcurrentHashMap<String, CoherenceBasedCache<?, ?>> m_mapNameToJCache =
        new ConcurrentHashMap<String, CoherenceBasedCache<?, ?>>();

    /**
     * Save closed JCaches so can still implement destroy on them.
     */
    private final ConcurrentHashMap<String, WeakReference<CoherenceBasedCache<?, ?>>> m_closedMapNameToJCache =
        new ConcurrentHashMap<String, WeakReference<CoherenceBasedCache<?, ?>>>();
    private final CoherenceBasedCachingProvider m_provider;
    private final URI                           m_uri;
    private final WeakReference<ClassLoader>    m_refClassLoader;
    private final Properties                    m_properties;
    private ConfigurableCacheFactory            m_ccf;
    private volatile boolean                    m_fClosed = false;

    /**
     * Is this instance Container Managed.
     */
    private final boolean                       m_fContainerManaged;

    /**
     * resource name for this instance's registration with m_ccf.
     */
    private String m_sResourceName;
    }
