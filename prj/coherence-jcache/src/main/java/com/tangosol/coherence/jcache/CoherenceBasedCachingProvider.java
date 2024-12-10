/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.application.Context;

import com.tangosol.coherence.config.Config;

import com.tangosol.coherence.jcache.localcache.LocalCacheConfiguration;

import com.tangosol.coherence.jcache.partitionedcache.PartitionedCacheConfiguration;
import com.tangosol.coherence.jcache.passthroughcache.PassThroughCacheConfiguration;

import com.tangosol.coherence.jcache.remotecache.RemoteCacheConfiguration;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;

import com.tangosol.util.Base;

import static com.tangosol.coherence.jcache.Constants.DEFAULT_COHERENCE_JCACHE_CONFIGURATION_CLASS_NAME_SYSTEM_PROPERTY;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

import java.util.concurrent.atomic.AtomicReference;

import javax.cache.CacheException;
import javax.cache.CacheManager;

import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.configuration.OptionalFeature;

import javax.cache.spi.CachingProvider;

/**
 * The Coherence-based implementation of a {@link CachingProvider}.
 *
 * Added support to recognize and work within a container environment.
 * Calls to JCache API from container code should call CachingProvider#getCacheManager(null, null, null)
 * in order that container descriptor file coherence-application.xml is used to initialize CacheManager.
 * The Coherence implementation integration is described in {@link com.tangosol.application.ContainerAdapter}.
 * Coherence JCache cache configuration and optional pof configuration must be referenced in coherence-application.xml
 * as detailed in {@link com.tangosol.application.ContainerAdapter} javadoc.
 *
 * @author bb  2013.04.08
 * @author bo  2013.12.17
 * @author jf  2014.06.24
 */
public class CoherenceBasedCachingProvider
        implements CachingProvider
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link CoherenceBasedCachingProvider}.
     */
    public CoherenceBasedCachingProvider()
        {
        m_mapClzldrToMgrMap = new WeakHashMap<ClassLoader, HashMap<URI, CacheManager>>();
        m_fContainerContext = false;
        }

    // ----- CachingProvider interface --------------------------------------

    /**
     * Return a CacheManager.
     * <p>
     * To get a container managed {@link CacheManager}, any container code calling this method MUST
     * call this method with a null uri parameter.  Calling with a non-null value results in a non-container managed
     * {@link CacheManager}. Container managed instances are configured via GAR configuration and are created/closed
     * by container.
     * <p>
     * Note: the jcache container unit CoherenceBasedCacheManagerTest#testJCacheContainerActivation
     * confirms that if the getCacheManager call within the container has the cache config uri that
     * exactly matches the one in the GAR configuration, the code does work correctly and the
     * {@link CoherenceBasedCacheManager#isContainerManaged()} returns true.
     * <p>
     * If this method is called from a container context with a non-null URI, the returned {@link CacheManager} is considered
     * unmanaged.
     *
     * @param u A {@link URI} referencing a coherence cache config containing JCacheNamespace or JCacheExtendNamespace.
     *          A value of null denotes to compute the default URI for this {@link CacheManager}.
     * @param cl classloader
     * @param p  Coherence JCache implementation specific {@link Properties}
     * @return  {@link CacheManager}
     */
    @Override
    public synchronized CacheManager getCacheManager(URI u, ClassLoader cl, Properties p)
        {
        ExtensibleConfigurableCacheFactory eccf              = null;
        boolean                            fContainerManaged = isContainerContext() && u == null;

        if (isContainerContext() && !fContainerManaged)
            {
            // Allow unmanaged Coherence Based JCache CacheManager
            Logger.warn("CoherenceBasedCachingProvider.getCacheManager(): "
                        + "called from a container context but returning a non-container "
                        + "managed javax.cache.CacheManager due to non-null uri parameter=" + u);
            }

        if (fContainerManaged)
            {
            if (cl == null)
                {
                cl = Base.getContextClassLoader();
                }

            // get the default ECCF for this classloader that was configured by container activation.
            eccf = (ExtensibleConfigurableCacheFactory) CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory(cl);


            // initialize container managed CacheManager via GAR configuration used to configure default ECCF
            Context ctx = eccf.getApplicationContext();

            u  = computeURI(eccf);
            cl = ctx.getClassLoader();

            CacheManager mgrContainer = CoherenceBasedCacheManager.getContainerManagedCacheManager(eccf);

            if (mgrContainer != null)
                {
                Logger.fine("getCacheManager found existing container-managed CacheManager uri=" + u + " classloader=" + cl);
                return mgrContainer;
                }
            }

        Properties                 props       = (p == null) ? getDefaultProperties() : p;
        URI                        uri         = (u == null) ? getDefaultURI() : u;
        ClassLoader                cldr        = (cl == null) ? getDefaultClassLoader() : cl;

        HashMap<URI, CacheManager> mapUriToMgr = m_mapClzldrToMgrMap.get(cldr);

        if (mapUriToMgr == null)
            {
            mapUriToMgr = new HashMap<URI, CacheManager>();
            }

        CacheManager mgr = mapUriToMgr.get(uri);

        if (mgr == null)
            {
            if (isContainerContext() && fContainerManaged)
                {
                mgr = new CoherenceBasedCacheManager(this, eccf, uri, cldr, props);
                }
            else
                {
                mgr = createCacheMananger(uri, cldr, props);
                }

            mapUriToMgr.put(uri, mgr);
            }
        else
            {
            Logger.finest("getCacheManager found existing CacheManager uri=" + uri + " classloader=" + cldr);
            }

        if (!m_mapClzldrToMgrMap.containsKey(cldr))
            {
            m_mapClzldrToMgrMap.put(cldr, mapUriToMgr);
            }

        return mgr;
        }

    /**
     * Returns a non-null ClassLoader.
     *
     * @return a ClassLoader
     */
    @Override
    public ClassLoader getDefaultClassLoader()
        {
        return Base.getContextClassLoader();
        }

    @Override
    public URI getDefaultURI()
        {
        String  kind = Config.getProperty(Constants.DEFAULT_COHERENCE_JCACHE_CONFIGURATION_CLASS_NAME_SYSTEM_PROPERTY);
        boolean fExtendEnabled = isExtendClient(kind);

        String uri = Config.getProperty(Constants.DEFAULT_COHERENCE_CONFIGURATION_URI_SYSTEM_PROPERTY, fExtendEnabled
                                        ? Constants.DEFAULT_COHERENCE_JCACHE_EXTEND_CLIENT_CONFIGURATION_URI
                                        : Constants.DEFAULT_COHERENCE_JCACHE_CONFIGURATION_URI);

        try
            {
            return new URI(uri);
            }
        catch (URISyntaxException e)
            {
            throw new CacheException("Failed to create the default URI for the javax.cache CoherenceAdapter Implementation",
                                     e);
            }
        }

    @Override
    public Properties getDefaultProperties()
        {
        // there are no default properties for this provider
        return new Properties();
        }

    @Override
    public CacheManager getCacheManager(URI uri, ClassLoader cl)
        {
        return getCacheManager(uri, cl, getDefaultProperties());
        }

    @Override
    public CacheManager getCacheManager()
        {
        return getCacheManager(null, null, null);
        }

    @Override
    public synchronized void close()
        {
        WeakHashMap<ClassLoader, HashMap<URI, CacheManager>> mapClassLoaderToMgrMap = m_mapClzldrToMgrMap;

        m_mapClzldrToMgrMap = new WeakHashMap<ClassLoader, HashMap<URI, CacheManager>>();

        for (Map.Entry<ClassLoader, HashMap<URI, CacheManager>> entry : mapClassLoaderToMgrMap.entrySet())
            {
            for (CacheManager mgr : entry.getValue().values())
                {
                mgr.close();
                }
            }

        m_fContainerContext = false;
        m_defaultConfigurationClassName = new AtomicReference<String>(null);
        }

    @Override
    public synchronized void close(ClassLoader cl)
        {
        ClassLoader                clzldr       = (cl == null) ? getDefaultClassLoader() : cl;

        HashMap<URI, CacheManager> mapUriToMgrs = m_mapClzldrToMgrMap.remove(clzldr);

        if (mapUriToMgrs != null)
            {
            for (CacheManager mgr : mapUriToMgrs.values())
                {
                mgr.close();
                }
            }
        }

    @Override
    public synchronized void close(URI u, ClassLoader cl)
        {
        URI                        uri         = (u == null) ? getDefaultURI() : u;
        ClassLoader                clzldr      = (cl == null) ? getDefaultClassLoader() : cl;

        HashMap<URI, CacheManager> mapUriToMgr = m_mapClzldrToMgrMap.get(clzldr);

        if (mapUriToMgr != null)
            {
            CacheManager mgr = mapUriToMgr.remove(uri);

            if (mgr != null)
                {
                mgr.close();
                }

            if (mapUriToMgr.size() == 0)
                {
                m_mapClzldrToMgrMap.remove(clzldr);
                }
            }
        }

    @Override
    public boolean isSupported(OptionalFeature feature)
        {
        switch (feature)
            {
            case STORE_BY_REFERENCE:
                String defaultConfigurationClassName = getDefaultCoherenceBasedConfigurationClassName();

                // The Coherence JCache Adapter implementation has one provider and multiple implementations.

                // to pass JSR 107 TCK which only uses generic JCache Configuration,
                // return this value based on what Coherence JCache Adapter implementation is
                // configured to be used by default.

                return defaultConfigurationClassName.contains("LocalCacheConfiguration");

            default:
                return false;
            }
        }

    // ----- CoherenceBasedCachingProvider methods --------------------------

    public boolean isContainerContext()
        {
        return m_fContainerContext;
        }

    /**
     * Releases the CacheManager with the specified URI and ClassLoader
     * from this CachingProvider.  This does not close the CacheManager.  It
     * simply releases it from being tracked by the CachingProvider.
     * <p>
     * This method does nothing if a CacheManager matching the specified
     * parameters is not being tracked.
     *
     * @param c  the ClassLoader of the CacheManager
     * @param u          the URI of the CacheManager
     */
    public synchronized void release(ClassLoader c, URI u)
        {
        URI                        uri         = (u == null) ? getDefaultURI() : u;
        ClassLoader                cl          = (c == null) ? getDefaultClassLoader() : c;

        HashMap<URI, CacheManager> mapUriToMgr = m_mapClzldrToMgrMap.get(cl);

        if (mapUriToMgr != null)
            {
            mapUriToMgr.remove(uri);

            if (mapUriToMgr.size() == 0)
                {
                m_mapClzldrToMgrMap.remove(cl);
                }
            }
        }

    /**
     * Constructs a suitable {@link CacheManager} for the specified parameters.
     * <p>
     * This method may be overridden by sub-classes to provide specialized
     * {@link CacheManager} implementations.
     *
     * @param uri          the {@link CacheManager} {@link URI}
     * @param classLoader  the {@link ClassLoader} for the returned {@link CacheManager}
     * @param properties   the custom {@link Properties} for the {@link CacheManager}
     *
     * @return  a new {@link CacheManager}
     */
    protected CacheManager createCacheMananger(URI uri, ClassLoader classLoader, Properties properties)
        {
        // when a URI hasn't been provided, use the default
        if (uri == null)
            {
            uri = getDefaultURI();
            }

        // when properties haven't been provided, use the default
        if (properties == null)
            {
            properties = getDefaultProperties();
            }

        // if a getClassLoader() or Thread.getContextClassLoader() returns null,
        // then be sure to use method getDefaultClassLoader that guarantees to not return null.
        if (classLoader == null)
            {
            classLoader = getDefaultClassLoader();
            }

        // we need the URI as a String for Coherence
        String sURI = uri.toString();

        // attempt to have the CacheFactoryBuilder load the ConfigurableCacheFactory for the URI
        ConfigurableCacheFactory ccf;

        try
            {
            if (Logger.isEnabled(Logger.FINER))
                {
                Logger.finer("ConfigurableCacheFactory being configured using configuration file=[" + sURI
                                 + "] classloader=" + classLoader);
                }

            ccf = CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(sURI, classLoader);

            Logger.info("getConfigurableCacheFactory returned ccf=" + ccf + " classLoader Hierarchy="
                             + toStringClassLoaderHierachy(classLoader));

            assert(ccf != null);

            CoherenceBasedCacheManager mgr = new CoherenceBasedCacheManager(this, ccf, uri, classLoader, properties);


            return mgr;
            }
        catch (RuntimeException e)
            {
            throw new CacheException("Error processing " + sURI, e);
            }
        }

    /**
     * Print out ClassLoader hierarchy
     *
     * @param ldr ClassLoader to add itself and all parents reachable from it.
     *
     * @return result with ClassLoader and its parent(s) classloader hierarchy.
     */
    private static String toStringClassLoaderHierachy(ClassLoader ldr)
        {
        StringBuilder bldr = new StringBuilder("Child ");
        ClassLoader   next = ldr;

        do
            {
            bldr.append("ClassLoader[hashcode=").append(next.hashCode() + "] [classloader=" + next + "]");
            next = next.getParent();

            if (next != null)
                {
                bldr.append(" Parent");
                }
            }
        while (next != null);

        return bldr.toString();
        }

    /**
     * Converts a {@link Configuration} into a {@link CoherenceBasedConfiguration} so that we may
     * create {@link CoherenceBasedCache}s.
     *
     * @param cfg          the {@link Configuration} to convert
     * @param classLoader  the {@link ClassLoader} to use to locate a suitable {@link CoherenceBasedConfiguration}
     *                     (when necessary)
     *
     * @param <K>  the key type
     * @param <V>  the value type
     *
     * @return a {@link CoherenceBasedConfiguration} for use with a {@link CoherenceBasedCacheManager}
     */
    protected <K, V> CoherenceBasedConfiguration<K, V> convertConfiguration(Configuration<K, V> cfg,
        ClassLoader classLoader)
        {
        if (cfg instanceof CoherenceBasedConfiguration)
            {
            return (CoherenceBasedConfiguration) cfg;
            }
        else if (cfg instanceof CompleteConfiguration)
            {
            CompleteConfiguration<K, V> cfgComplete = (CompleteConfiguration) cfg;
            String                      sClassName  = getDefaultCoherenceBasedConfigurationClassName();

            try
                {
                Class<?> clsConfiguration = classLoader.loadClass(sClassName);

                if (CoherenceBasedConfiguration.class.isAssignableFrom(clsConfiguration))
                    {
                    Constructor constructor = clsConfiguration.getConstructor(CompleteConfiguration.class);

                    return (CoherenceBasedConfiguration) constructor.newInstance(cfgComplete);
                    }
                else
                    {
                    throw new ClassCastException("The specified configuration class [" + sClassName
                                                 + "] does not implement "
                                                 + CoherenceBasedConfiguration.class.getCanonicalName());
                    }
                }
            catch (ClassNotFoundException e)
                {
                throw new UnsupportedOperationException("Failed to load the specified configuration class ["
                    + sClassName + "]", e);
                }
            catch (NoSuchMethodException e)
                {
                throw new UnsupportedOperationException("The specified configuration class [" + sClassName
                    + "] does not have a public constructor taking a single CompleteConfiguration argument", e);
                }
            catch (InstantiationException e)
                {
                throw new UnsupportedOperationException("The specified configuration class [" + sClassName
                    + "] could not be instantiated", e);
                }
            catch (IllegalAccessException e)
                {
                throw new UnsupportedOperationException("The specified configuration class [" + sClassName
                    + "] is not accessible", e);
                }
            catch (InvocationTargetException e)
                {
                throw new UnsupportedOperationException("The specified configuration class [" + sClassName
                    + "] could not be instantiated", e);
                }
            }
        else
            {
            Logger.warn("CoherenceBasedCachingProvider: Ignoring unknown configuration class "
                + cfg.getClass().getCanonicalName() + " defaulting"
                + " to basic javax.cache.configuration.MutableConfiguration initialized with base javax.cache.configuration.Configuration "
                + "values taken from configuration parameter.");

            // the provided configuration is unknown to this provider so lets instead create a MutableConfiguration
            MutableConfiguration<K, V> cfgMutable = new MutableConfiguration<K, V>();

            cfgMutable.setTypes(cfg.getKeyType(), cfg.getValueType());
            cfgMutable.setStoreByValue(cfg.isStoreByValue());

            // now create a configuration based on our MutableConfiguration
            return convertConfiguration(cfgMutable, classLoader);
            }
        }

    /**
     * Determines the fully-qualified-class-name of the default
     * {@link CoherenceBasedConfiguration} class to use when provided
     * with a JCache {@link Configuration}.
     *
     * @return  the fully-qualified-class-name of a {@link CoherenceBasedConfiguration}
     */
    public String getDefaultCoherenceBasedConfigurationClassName()
        {
        if (m_defaultConfigurationClassName.get() == null)
            {
            // determine the fully-qualified-class-name based on the system-property
            String sClassName = Config.getProperty(DEFAULT_COHERENCE_JCACHE_CONFIGURATION_CLASS_NAME_SYSTEM_PROPERTY,
                    LocalCacheConfiguration.class.getCanonicalName());

            // translate aliases into the fully-qualified-class-name
            if (sClassName.equalsIgnoreCase("local"))
                {
                sClassName = LocalCacheConfiguration.class.getCanonicalName();
                }
            else if (sClassName.equalsIgnoreCase("partitioned"))
                {
                sClassName = PartitionedCacheConfiguration.class.getCanonicalName();
                }
            else if (sClassName.equals("passthrough"))
                {
                sClassName = PassThroughCacheConfiguration.class.getCanonicalName();
                }
            else if (sClassName.equalsIgnoreCase("extend") || sClassName.equalsIgnoreCase("remote"))
                {
                sClassName = RemoteCacheConfiguration.class.getCanonicalName();
                }

            boolean result = m_defaultConfigurationClassName.compareAndSet(null, sClassName);

            if (result)
                {
                Logger.info("Mapping general javax.cache.Configuration implementation to "
                            + "CoherenceBased JCacheConfiguration of " + sClassName);
                }
            }

        return m_defaultConfigurationClassName.get();
        }

    /**
     * Place CoherenceBasedCachingProvider into Container Context mode.
     */
    public void enableContainerContext()
        {
        m_fContainerContext = true;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * compute if extend client by inspecting value of defaultJCacheConfiguration class value.
     *
     * @param defaultJCacheConfigurationClass either system property or getCacheManager property for {@link Constants#DEFAULT_COHERENCE_JCACHE_CONFIGURATION_CLASS_NAME_SYSTEM_PROPERTY}
     *
     * @return true iff a RemoteCacheConfiguration would be created based on the above mentioned property.
     */
    private static boolean isExtendClient(String defaultJCacheConfigurationClass)
        {
        return defaultJCacheConfigurationClass != null
               && (defaultJCacheConfigurationClass.equalsIgnoreCase("remote")
                   || defaultJCacheConfigurationClass.equalsIgnoreCase("extend")
                   || defaultJCacheConfigurationClass
                       .equalsIgnoreCase("com.tangosol.coherence.jcache.remotecache.RemoteConfiguration"));

        }

    /**
     * compute Cache Config URI from ECCF. CacheManager gets cache config uri from ECCF in container-managed mode.
     * @param eccf current eccf
     * @return URI computed from eccf.
     */
    private URI computeURI(ExtensibleConfigurableCacheFactory eccf)
        {
        final String CACHE_CONFIG_URI = eccf.getApplicationContext().getCacheConfigURI();
        URI          u                = null;

        try
            {
            u = new URI(CACHE_CONFIG_URI);
            }
        catch (URISyntaxException e)
            {
            throw new CacheException("Not valid URI syntax used in application context cache config uri "
                                     + CACHE_CONFIG_URI, e);
            }

        return u;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The CacheManagers scoped by ClassLoader and URI.
     */
    private WeakHashMap<ClassLoader, HashMap<URI, CacheManager>> m_mapClzldrToMgrMap;

    /**
     * Map {@link MutableConfiguration} and all non Coherence based {@link Configuration} to this default Coherence Based Cache Configuration.
     *
     * Potential values include fully qualified cannonical name of {@link LocalCacheConfiguration} and {@link PartitionedCacheConfiguration}.
     */
    private AtomicReference<String> m_defaultConfigurationClassName = new AtomicReference<String>(null);

    /**
     * Running in a ContainerContext
     */
    private boolean                 m_fContainerContext;
    }
