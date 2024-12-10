/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.common.base.Disposable;

import com.oracle.coherence.common.base.Lockable;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.SchemeMappingRegistry;

import com.tangosol.coherence.config.builder.NamedEventInterceptorBuilder;
import com.tangosol.coherence.config.builder.InstanceBuilder;

import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.Parameter;

import com.tangosol.io.AsyncBinaryStore;
import com.tangosol.io.AsyncBinaryStoreManager;
import com.tangosol.io.BinaryStore;
import com.tangosol.io.BinaryStoreManager;
import com.tangosol.io.ClassLoaderAware;

import com.tangosol.io.bdb.BerkeleyDBBinaryStoreManager;

import com.tangosol.io.nio.BinaryMap;
import com.tangosol.io.nio.BinaryMapStore;
import com.tangosol.io.nio.ByteBufferManager;
import com.tangosol.io.nio.MappedBufferManager;
import com.tangosol.io.nio.MappedStoreManager;

import com.tangosol.net.topic.NamedTopic;

import com.tangosol.net.cache.AbstractBundler;
import com.tangosol.net.cache.BinaryEntryStore;
import com.tangosol.net.cache.BundlingNamedCache;
import com.tangosol.net.cache.CacheLoader;
import com.tangosol.net.cache.CacheStore;
import com.tangosol.net.cache.ConfigurableCacheMap;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.MapCacheStore;
import com.tangosol.net.cache.NearCache;
import com.tangosol.net.cache.OverflowMap;
import com.tangosol.net.cache.ReadWriteBackingMap;
import com.tangosol.net.cache.SerializationCache;
import com.tangosol.net.cache.SerializationMap;
import com.tangosol.net.cache.SerializationPagedCache;
import com.tangosol.net.cache.SimpleOverflowMap;
import com.tangosol.net.cache.SimpleSerializationMap;
import com.tangosol.net.cache.VersionedBackingMap;
import com.tangosol.net.cache.VersionedNearCache;

import com.tangosol.net.events.EventDispatcherRegistry;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.net.events.NamedEventInterceptor;

import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.internal.InterceptorManager;
import com.tangosol.net.events.internal.Registry;

import com.tangosol.net.internal.ScopedCacheReferenceStore;

import com.tangosol.net.management.MBeanHelper;

import com.tangosol.net.partition.ObservableSplittingBackingCache;
import com.tangosol.net.partition.ObservableSplittingBackingMap;
import com.tangosol.net.partition.PartitionAwareBackingMap;
import com.tangosol.net.partition.ReadWriteSplittingBackingMap;

import com.tangosol.net.security.Security;
import com.tangosol.net.security.StorageAccessAuthorizer;

import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.SimpleValue;
import com.tangosol.run.xml.XmlConfigurable;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.run.xml.XmlValue;

import com.tangosol.util.AbstractKeyBasedMap;
import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapSet;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.RegistrationBehavior;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SafeHashMap;
import com.tangosol.util.SimpleResourceRegistry;

import java.io.File;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;


/**
* <strong>This class has now been deprecated.
* It remains part of the distribution of Coherence only for the purpose of
* providing backwards compatibility for those applications that have extend it
* or depend on specific implementation semantics.</strong>
* <p>
* <strong>Developers are strongly encouraged to refactor the implementations that depend
* on this class to use the extension mechanisms prescribed
* by the {@link ExtensibleConfigurableCacheFactory}.</strong>
* <p>
* <strong>At some point in the future this class will be removed.  No further development
* or enhancement of this class will occur going forward.</strong>
* <p>
* <strong>It is strongly recommended that developers get a ConfigurableCacheFactory instance via
* CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(), rather than
* instantiate a DefaultConfigurableCacheFactory instance directly.
* </strong>
* <hr>
* <p>
* DefaultConfigurableCacheFactory provides a facility to access caches declared
* in a "coherence-cache-config.xsd" compliant configuration file.
* <p>
* This class is designed to be easily extendable with a collection of factory
* methods allowing subclasses to customize it by overriding any subset of
* cache instantiation routines or even allowing addition of custom schemes.
* <p>
* There are various ways of using this factory:
* <pre>
*   ConfigurableCacheFactory factory =
*       new DefaultConfigurableCacheFactory(sPath);
*   ...
*   ClassLoader loader  = getClass().getClassLoader();
*   NamedCache cacheOne = factory.ensureCache("one", loader);
*   NamedCache cacheTwo = factory.ensureCache("two", loader);
* </pre>
* Using this approach allows an easy customization by extending the
* DefaultConfigurableCacheFactory and changing the instantiation line:
* <pre>
*   ConfigurableCacheFactory factory = new CustomConfigurableCacheFactory();
*   ...
* </pre>
*
* Another option is using the static version of the "ensureCache" call:
* <pre>
*   ClassLoader loader  = getClass().getClassLoader();
*   NamedCache cacheOne = CacheFactory.getCache("one", loader);
*   NamedCache cacheTwo = CacheFactory.getCache("two", loader);
* </pre>
* which uses an instance of ConfigurableCacheFactory obtained by
* {@link CacheFactory#getConfigurableCacheFactory()}.
*
* @see CacheFactory#getCache(String, ClassLoader, NamedCache.Option...)
*
* @author gg  2003.05.26
*
* @since Coherence 2.2
*/
@Deprecated
public class DefaultConfigurableCacheFactory
        extends Base
        implements ConfigurableCacheFactory
    {
    // ----- constructors -------------------------------------------------

    /**
    * Construct a default DefaultConfigurableCacheFactory using the
    * default configuration file name.
    */
    public DefaultConfigurableCacheFactory()
        {
        this(loadConfig(FILE_CFG_CACHE));
        }

    /**
    * Construct a DefaultConfigurableCacheFactory using the specified path to
    * a "coherence-cache-config.xsd" compliant configuration file or resource.
    *
    * @param sPath  the configuration resource name or file path
    */
    public DefaultConfigurableCacheFactory(String sPath)
        {
        this(loadConfig(sPath));
        }

    /**
    * Construct a DefaultConfigurableCacheFactory using the specified path to
    * a "coherence-cache-config.xsd" compliant configuration file or resource.
    *
    * @param sPath   the configuration resource name or file path
    * @param loader  (optional) ClassLoader that should be used to load the
    *                configuration resource
    */
    public DefaultConfigurableCacheFactory(String sPath, ClassLoader loader)
        {
        this(loadConfig(sPath, loader), loader);
        }

    /**
    * Construct a DefaultConfigurableCacheFactory using the specified
    * configuration XML.
    *
    * @param xmlConfig  the configuration XmlElement
    */
    public DefaultConfigurableCacheFactory(XmlElement xmlConfig)
        {
        this(xmlConfig, null);
        }

    /**
    * Construct a DefaultConfigurableCacheFactory using the specified
    * configuration XML.
    *
    * @param xmlConfig  the configuration XmlElement
    * @param loader  (optional) ClassLoader that should be used to load the
    *                configuration resource
    */
    public DefaultConfigurableCacheFactory(XmlElement xmlConfig, ClassLoader loader)
        {
        setConfigClassLoader(loader);
        setConfig(xmlConfig);

        Logger.fine("Created cache factory " + this.getClass().getName());
        }


    // ----- ConfigurableCacheFactory interface -----------------------------

    /**
    * Obtain the factory configuration XML.
    *
    * @return the configuration XML
    */
    public XmlElement getConfig()
        {
        return (XmlElement) m_xmlConfig.clone();
        }

    /**
    * Obtain a mutable reference to the factory configuration XML.
    * <p>
    * Note: The caller may not modify the resulting XmlElement's in any way.
    *
    * @return the configuration XML
    */
    protected XmlElement getConfigUnsafe()
        {
        return m_xmlConfig;
        }

    /**
    * Specify the factory configuration XML.
    *
    * @param xmlConfig  the configuration XML
    */
    public void setConfig(XmlElement xmlConfig)
        {
        xmlConfig = (XmlElement) xmlConfig.clone();
        XmlHelper.replaceSystemProperties(xmlConfig, "system-property");

        String sScopeName = xmlConfig.getSafeElement("scope-name").getString();
        if (!sScopeName.isEmpty())
            {
            m_sScopeName = sScopeName;
            }

        m_xmlConfig = xmlConfig;

        ResourceRegistry registry = m_registry;
        if (registry != null)
            {
            registry.dispose();
            }
        registry = m_registry = new SimpleResourceRegistry();

        Registry eventRegistry = new Registry();
        registry.registerResource(InterceptorRegistry.class, eventRegistry);
        registry.registerResource(EventDispatcherRegistry.class, eventRegistry);

        // No-op InterceptorManager
        CacheConfig cacheConfig = new CacheConfig(new NullParameterResolver());

        cacheConfig.addCacheMappingRegistry(new SchemeMappingRegistry());

        InterceptorManager manager = new InterceptorManager(cacheConfig, getConfigClassLoader(), registry);

        registry.registerResource(InterceptorManager.class, manager);

        configureInterceptors(xmlConfig);

        m_store.clear();
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public <K, V> NamedCache<K, V> ensureCache(String sCacheName,
                                               ClassLoader loader,
                                               NamedCache.Option... options)
        {
        NamedCache cache;

        if (sCacheName == null || sCacheName.length() == 0)
            {
            throw new IllegalArgumentException("Cache name cannot be null");
            }
        loader = ensureClassLoader(loader);

        ScopedCacheReferenceStore store = m_store;

        while (true)
            {
            cache = store.getCache(sCacheName, loader);

            if (cache != null && cache.isActive())
                {
                // the common path; the cache reference is active and reusable
                checkPermission(cache);
                return cache;
                }

            if (cache != null && !cache.isDestroyed() && !cache.isReleased())
                {
                try
                    {
                    // this can only indicate that the cache was "disconnected" due to a
                    // network failure or an abnormal cache service termination;
                    // the underlying cache service will “restart" during the very next call
                    // to any of the NamedCache API method (see COH-15083 for details)
                    checkPermission(cache);
                    return cache;
                    }
                catch (IllegalStateException e)
                    {
                    // Fail to access due to cluster or service being explicitly stopped or shutdown
                    // and not properly restarted.
                    // Remove this cache and the process of returning a new one ensures all services needed by cache.
                    store.releaseCache(cache, loader);
                    }
                }

            store.clearInactiveCacheRefs();

            CacheInfo  infoCache    = findSchemeMapping(sCacheName);
            XmlElement xmlScheme    = resolveScheme(infoCache);
            String     sSchemeType  = xmlScheme.getName();
            int        nSchemeType  = translateSchemeType(sSchemeType);
            int        iHashCurrent = loader == null ? 0 : loader.hashCode();

            xmlScheme.addAttribute("tier").setString("front"); // mark the "entry point"
            pushCacheContext("tier=front,loader=" + iHashCurrent);

            cache = configureCache(infoCache, xmlScheme, loader);

            // TODO: The knowledge of transactional cache is left out of
            // ScopedReferenceStore. We should re-consider.
            if (nSchemeType == SCHEME_TRANSACTIONAL)
                {
                return cache;
                }

            if (store.putCacheIfAbsent(cache, loader) == null)
                {
                break;
                }
            }

        //NOTE: this is an unsafe cast as DCCF doesn't support type checking
        return cache;
        }

    /**
    * {@inheritDoc}
    */
    public void releaseCache(NamedCache cache)
        {
        releaseCache(cache, /*fDestroy*/ false);
        }

    /**
    * {@inheritDoc}
    */
    public void destroyCache(NamedCache cache)
        {
        releaseCache(cache, /*fDestroy*/ true);
        }

    /**
     * This method will throw an {@link UnsupportedOperationException} as
     * {@link NamedTopic}s are not supported by DefaultConfigurableCacheFactory.
     */
    @Override
    public <V> NamedTopic<V> ensureTopic(String sName, ClassLoader loader, NamedTopic.Option... options)
        {
        throw new UnsupportedOperationException("NamedTopic is not supported by DefaultConfigurableCacheFactory");
        }

    /**
     * This method will throw an {@link UnsupportedOperationException} as
     * {@link NamedTopic}s are not supported by DefaultConfigurableCacheFactory.
     */
    @Override
    public void releaseTopic(NamedTopic<?> topic)
        {
        throw new UnsupportedOperationException("NamedTopic is not supported by DefaultConfigurableCacheFactory");
        }

    /**
     * This method will throw an {@link UnsupportedOperationException} as
     * {@link NamedTopic}s are not supported by DefaultConfigurableCacheFactory.
     */
    @Override
    public void destroyTopic(NamedTopic<?> topic)
        {
        throw new UnsupportedOperationException("NamedTopic is not supported by DefaultConfigurableCacheFactory");
        }

    /**
    * {@inheritDoc}
    */
    public Service ensureService(String sServiceName)
        {
        return ensureService(findServiceScheme(sServiceName));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void activate()
        {
        // do nothing
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void dispose()
        {
        // do nothing
        }
    
    /**
    * {@inheritDoc}
    */
    public boolean isCacheActive(String sCacheName,  ClassLoader loader) 
        {
        return m_store.getCache(sCacheName, ensureClassLoader(loader)) != null;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean isTopicActive(String sTopicName, ClassLoader loader)
        {
        throw new UnsupportedOperationException("NamedTopic is not supported by DefaultConfigurableCacheFactory");
        }

    // ----- helpers and inheritance support --------------------------------

    /**
    * Return the scope name for this ConfigurableCacheFactory.  If specified,
    * this name will be used as a prefix for the name of all services created
    * by this factory.
    *
    * @return the scope name for this ConfigurableCacheFactory; may be null
    */
    public String getScopeName()
        {
        return m_sScopeName;
        }

    /**
    * Set the scope name for this ConfigurableCacheFactory.  Note that this
    * method should <strong>only</strong> be invoked by a {@link CacheFactoryBuilder}
    * when initializing this ConfigurableCacheFactory instance.
    * <p>
    * This method will be removed in a future release.
    *
    * @param sScopeName  the scope name for this ConfigurableCacheFactory
    *
    * @throws IllegalStateException  if this setter is invoked after a
    *                                service start
    */
    public void setScopeName(String sScopeName)
        {
        if (m_store.getNames().isEmpty())
            {
            m_sScopeName = sScopeName;
            }
        else
            {
            throw new IllegalStateException("Scope cannot be set after service start");
            }
        }

    /**
    * Return the class loader used to load the configuration for this factory.
    *
    * @return the class loader to use for loading the config
    */
    protected ClassLoader getConfigClassLoader()
        {
        return m_loader;
        }

    /**
    * Return the {@link ResourceRegistry} for this factory.
    *
    * @return the ResourceRegistry for this factory
    */
    public ResourceRegistry getResourceRegistry()
        {
        return m_registry;
        }

    /**
    * Return the {@link InterceptorRegistry} used to register event interceptors
    * for this factory.
    *
    * @return the InterceptorRegistry for this factory
    */
    public InterceptorRegistry getInterceptorRegistry()
        {
        ResourceRegistry registry = m_registry;

        return registry == null ? null : registry.getResource(InterceptorRegistry.class);
        }

    /**
    * Check if the current user is allowed to "join" the cache.
    *
    * @param cache  the cache
    *
    * @throws SecurityException if permission is denied
    */
    protected static void checkPermission(NamedCache cache)
        {
        // This is a secondary check to make sure that cached references
        // are allowed with the current Subject. The primary check is in
        // SafeCacheService.ensureCache().
        Service service = cache.getCacheService();
        Security.checkPermission(service.getCluster(), service.getInfo().
                getServiceName(), cache.getCacheName(), "join");
        }

    /**
    * Set the class loader used to load the configuration for this factory.
    *
    * @param loader  the class loader to use for loading the config
    */
    public void setConfigClassLoader(ClassLoader loader)
        {
        m_loader = ensureClassLoader(loader);
        }

    /**
    * Load the configuration from a file or resource.
    *
    * @param sName  the name of file or resource.
    *
    * @return the configuration XML
    */
    public static XmlDocument loadConfig(String sName)
        {
        return loadConfig(sName, null);
        }

    /**
    * Load the configuration from a file or resource.
    *
    * @param sName   the name of file or resource.
    * @param loader  (optional) ClassLoader that should be used to load the
    *                configuration resource
    *
    * @return the configuration XML
    */
    public static XmlDocument loadConfig(String sName, ClassLoader loader)
        {
        return XmlHelper.loadFileOrResourceOrDefault(sName, "cache configuration", loader);
        }

    /**
    * Load the configuration as a resource.
    *
    * @param sResource  the resource name
    * @param loader  (optional) ClassLoader that should be used to load the
    *                configuration resource
    *
    * @return the configuration XML
    */
    public static XmlDocument loadConfigAsResource(String sResource, ClassLoader loader)
        {
        return loadConfig(sResource, loader);
        }

    /**
    * Load the configuration from a file or directory.
    *
    * @param file  file or directory to load the configuration from
    *
    * @return the configuration XML
    */
    public static XmlDocument loadConfigFromFile(File file)
        {
        if (file.isDirectory())
            {
            file = new File(file, FILE_CFG_CACHE);
            }

        return loadConfig(file.getAbsolutePath(), null);
        }

    /**
    * In the configuration XML find a "cache-mapping" element associated with a
    * given cache name.
    *
    * @param sCacheName  the value of the "cache-name" element to look for
    *
    * @return a CacheInfo object associated with a given cache name
    */
    public CacheInfo findSchemeMapping(String sCacheName)
        {
        XmlElement xmlDefaultMatch = null;
        XmlElement xmlPrefixMatch  = null;
        XmlElement xmlExactMatch   = null;
        String     sSuffix         = null;

        for (Iterator iter = m_xmlConfig.getSafeElement("caching-scheme-mapping").
                getElements("cache-mapping"); iter.hasNext();)
            {
            XmlElement xmlMapping = (XmlElement) iter.next();

            String sName = xmlMapping.getSafeElement("cache-name").getString();
            if (sName.equals(sCacheName))
                {
                xmlExactMatch = xmlMapping;
                break;
                }
            else if (sName.equals("*"))
                {
                xmlDefaultMatch = xmlMapping;
                }
            else
                {
                int cchPrefix = sName.indexOf('*');
                if (cchPrefix >= 0)
                    {
                    String sPrefix = sName.substring(0, cchPrefix);
                    if (sCacheName.startsWith(sPrefix))
                        {
                        if (cchPrefix != sName.length() - 1)
                            {
                            throw new IllegalArgumentException(
                                "Invalid wildcard pattern:\n" + xmlMapping);
                            }
                        xmlPrefixMatch = xmlMapping;
                        sSuffix        = sCacheName.substring(cchPrefix);
                        }
                    }
                }
            }

        XmlElement xmlMatch;

        if (xmlExactMatch != null)
            {
            xmlMatch = xmlExactMatch;
            sSuffix  = "";
            }
        else if (xmlPrefixMatch != null)
            {
            xmlMatch = xmlPrefixMatch;
            }
        else
            {
            xmlMatch = xmlDefaultMatch;
            sSuffix  = sCacheName;
            }

        if (xmlMatch == null)
            {
            throw new IllegalArgumentException(
                    "No scheme for cache: \"" + sCacheName +'"');
            }

        String sScheme = xmlMatch.getSafeElement("scheme-name").getString();
        Map    mapAttr = new HashMap();
        for (Iterator iter = xmlMatch.getSafeElement("init-params").
                getElements("init-param"); iter.hasNext();)
            {
            XmlElement xmlParam = (XmlElement) iter.next();
            String     sName    = xmlParam.getSafeElement("param-name" ).getString();
            String     sValue   = xmlParam.getSafeElement("param-value").getString();

            if (sName.length() != 0)
                {
                int ofReplace = sValue.indexOf('*');
                if (ofReplace >= 0 && sSuffix != null)
                    {
                    sValue = sValue.substring(0, ofReplace) + sSuffix +
                             sValue.substring(ofReplace + 1);
                    }
                mapAttr.put(sName, sValue);
                }
            }

        return new CacheInfo(sCacheName, sScheme, mapAttr);
        }

    /**
    * In the configuration XML find a "scheme" element associated with a
    * given cache and resolve it (recursively) using the "scheme-ref"
    * elements. The returned XML is always a clone of the actual configuration
    * and could be safely modified.
    *
    * @param info  the cache info
    *
    * @return a resolved "scheme" element associated with a given cache
    */
    public XmlElement resolveScheme(CacheInfo info)
        {
        XmlElement xmlScheme = findScheme(info.getSchemeName());

        info.replaceAttributes(xmlScheme);

        return resolveScheme(xmlScheme, info, false, true);
        }

    /**
    * In the configuration XML find a "scheme" element associated with a given
    * scheme name.
    *
    * @param sSchemeName  the value of the "scheme-name" element to look for
    *
    * @return a "scheme" element associated with a given scheme name
    */
    protected XmlElement findScheme(String sSchemeName)
        {
        XmlElement xmlScheme = findScheme(m_xmlConfig, sSchemeName);
        if (xmlScheme != null)
            {
            return (XmlElement) xmlScheme.clone();
            }

        throw new IllegalArgumentException("Missing scheme: \"" + sSchemeName + '"');
        }

    /**
    * In the specified configuration XML, find a "scheme" element associated with
    * the specified scheme name.
    *
    * @param xmlConfig    the xml configuration
    *
    * @param sSchemeName  the value of the "scheme-name" element to look for
    * @return a "scheme" element associated with a given scheme name, or null if
    *         none is found
    */
    protected static XmlElement findScheme(XmlElement xmlConfig, String sSchemeName)
        {
        if (sSchemeName != null)
            {
            for (Iterator iter = xmlConfig.getSafeElement("caching-schemes").
                    getElementList().iterator(); iter.hasNext();)
                {
                XmlElement xml = (XmlElement) iter.next();
                if (xml.getSafeElement("scheme-name").getString().equals(sSchemeName))
                    {
                    return xml;
                    }
                }
            }
        return null;
        }

    /**
    * Collect a map keyed by service names with values of corresponding service
    * schemes in the specified cache configuration.
    *
    * @param xmlConfig  the cache configuration
    *
    * @return a map keyed by service names with values of service schemes
    */
    public static Map collectServiceSchemes(XmlElement xmlConfig)
        {
        HashMap mapService = new HashMap();
        for (Iterator iter = xmlConfig.getSafeElement("caching-schemes").
                 getElementList().iterator(); iter.hasNext();)
            {
            XmlElement xmlScheme = (XmlElement) iter.next();
            collectServiceSchemes(xmlScheme, xmlConfig, mapService);
            }
        return mapService;
        }

    /**
    * Collect the service-schemes referenced by the specified scheme element in
    * the cache configuration and update the specified mapping of service names
    * to the associated service schemes.
    *
    * @param xmlScheme   a scheme element
    * @param xmlConfig   the cache configuration
    * @param mapService  a map of service name to service scheme
    */
    protected static void collectServiceSchemes(XmlElement xmlScheme, XmlElement xmlConfig, HashMap mapService)
        {
        String sSchemeType  = xmlScheme.getName();
        String sServiceName = xmlScheme.getSafeElement("service-name").getString();
        String sServiceType = null;

        switch (translateStandardSchemeType(sSchemeType))
            {
            case SCHEME_REPLICATED:
                sServiceType = CacheService.TYPE_REPLICATED;
                break;

            case SCHEME_OPTIMISTIC:
                sServiceType = CacheService.TYPE_OPTIMISTIC;
                break;

            case SCHEME_DISTRIBUTED:
                sServiceType = CacheService.TYPE_DISTRIBUTED;
                break;

            case SCHEME_LOCAL:
            case SCHEME_OVERFLOW:
            case SCHEME_DISK:
            case SCHEME_EXTERNAL:
            case SCHEME_EXTERNAL_PAGED:
            case SCHEME_FLASHJOURNAL:
            case SCHEME_RAMJOURNAL:
            case SCHEME_CLASS:
                sServiceType = CacheService.TYPE_LOCAL;
                break;

            case SCHEME_NEAR:
                {
                XmlElement xmlBack = resolveScheme(
                    xmlConfig, xmlScheme.getSafeElement("back-scheme"), null, true, true, false);
                collectServiceSchemes(xmlBack, xmlConfig, mapService);
                return;
                }

            case SCHEME_VERSIONED_NEAR:
                {
                XmlElement xmlVer  = resolveScheme(
                    xmlConfig, xmlScheme.getSafeElement("version-transient-scheme"), null, true, true, false);
                XmlElement xmlBack = resolveScheme(
                    xmlConfig, xmlScheme.getSafeElement("back-scheme"), null, true, true, false);

                collectServiceSchemes(xmlVer,  xmlConfig, mapService);
                collectServiceSchemes(xmlBack, xmlConfig, mapService);
                return;
                }

            case SCHEME_INVOCATION:
                sServiceType = InvocationService.TYPE_DEFAULT;
                break;

            case SCHEME_PROXY:
                sServiceType = "Proxy";
                break;

            case SCHEME_REMOTE_CACHE:
                sServiceType = CacheService.TYPE_REMOTE;
                break;

            case SCHEME_REMOTE_INVOCATION:
                sServiceType = InvocationService.TYPE_REMOTE;
                break;
            }

        if (sServiceName.length() == 0)
            {
            sServiceName = sServiceType;
            }

        if (sServiceName != null)
            {
            mapService.put(sServiceName, xmlScheme);
            }
        }

    /**
    * In the configuration XML find a "scheme" element associated with a
    * given service name.
    *
    * @param sServiceName  the value of the "service-name" element to look for
    *
    * @return a "scheme" element associated with a given service name
    */
    protected XmlElement findServiceScheme(String sServiceName)
        {
        if (sServiceName != null)
            {
            for (Iterator iter = m_xmlConfig.getSafeElement("caching-schemes").
                    getElementList().iterator(); iter.hasNext();)
                {
                XmlElement xml = (XmlElement) iter.next();

                if (xml.getSafeElement("service-name").getString().equals(sServiceName))
                    {
                    return (XmlElement) xml.clone();
                    }
                }
            }

        throw new IllegalArgumentException("Missing scheme for service: \"" + sServiceName + '"');
        }

    /**
    * Resolve the specified "XYZ-scheme" by retrieving the base element
    * referred to by the "scheme-ref" element, resolving it recursively,
    * and combining it with the specified overrides and cache specific attributes.
    *
    * @param xmlConfig  the cache configuration xml
    * @param xmlScheme  a scheme element to resolve
    * @param info       the cache info (optional)
    * @param fChild     if true, the actual cache scheme is the only "xyz-scheme"
    *                   child of the specified xmlScheme element;
    *                   otherwise it's the xmlScheme element itself
    * @param fRequired  if true, the child scheme must be present; false otherwise
    * @param fApply     if true, apply the specified overrides and cache-specific
    *                   attributes to the base scheme element; otherwise return
    *                   a reference to the base scheme element
    *
    * @return a "scheme" element referred to by the "scheme-ref" value;
    *         null if the child is missing and is not required
    */
    protected static XmlElement resolveScheme(XmlElement xmlConfig, XmlElement xmlScheme,
            CacheInfo info, boolean fChild, boolean fRequired, boolean fApply)
        {
        if (fChild)
            {
            XmlElement xmlChild = null;
            for (Iterator iter = xmlScheme.getElementList().iterator(); iter.hasNext();)
                {
                XmlElement xml = (XmlElement) iter.next();

                if (xml.getName().endsWith("-scheme"))
                    {
                    if (xmlChild == null)
                        {
                        xmlChild = xml;
                        }
                    else
                        {
                        throw new IllegalArgumentException(
                            "Scheme contains more then one child scheme:\n" + xmlScheme);
                        }
                    }
                }

            if (xmlChild == null)
                {
                if (fRequired)
                    {
                    String sName = xmlScheme.getName();
                    if (xmlScheme == xmlScheme.getParent().getElement(sName))
                        {
                        throw new IllegalArgumentException(
                            "Child scheme is missing at:\n" + xmlScheme);
                        }
                    else
                        {
                        throw new IllegalArgumentException(
                            "Element \"" + sName + "\" is missing at:\n" + xmlScheme.getParent());
                        }
                    }
                return null;
                }
            xmlScheme = xmlChild;
            }

        String sRefName = xmlScheme.getSafeElement("scheme-ref").getString();
        if (sRefName.length() == 0)
            {
            return xmlScheme;
            }

        XmlElement xmlBase = findScheme(xmlConfig, sRefName);
        if (xmlBase == null)
            {
            throw new IllegalArgumentException("Unresolved reference to scheme:\n" +
                sRefName);
            }

        xmlBase = fApply ? (XmlElement) xmlBase.clone() : xmlBase;

        if (!xmlScheme.getName().equals(xmlBase.getName()))
            {
            throw new IllegalArgumentException("Reference does not match the scheme type: scheme=\n" +
                xmlScheme + "\nbase=" + xmlBase);
            }
        if (xmlScheme.equals(xmlBase))
            {
            throw new IllegalArgumentException("Circular reference in scheme:\n" +
                xmlScheme);
            }

        if (info != null)
            {
            info.replaceAttributes(xmlBase);
            }

        XmlElement xmlResolve = resolveScheme(
            xmlConfig, xmlBase, info, false, false, fApply);

        if (fApply)
            {
            for (Iterator iter = xmlScheme.getElementList().iterator();
                 iter.hasNext();)
                {
                XmlHelper.replaceElement(xmlResolve, (XmlElement) iter.next());
                }
            }
        return xmlResolve;
        }

    /**
    * Resolve the specified "XYZ-scheme" by retrieving the base element
    * referred to by the "scheme-ref" element, resolving it recursively,
    * and combining it with the specified overrides and cache specific attributes.
    *
    * @param xmlScheme  a scheme element to resolve
    * @param info       the cache info (optional)
    * @param fChild     if true, the actual cache scheme is the only "xyz-scheme"
    *                   child of the specified xmlScheme element;
    *                   otherwise it's the xmlScheme element itself
    * @param fRequired  if true, the child scheme must be present; false otherwise
    *
    * @return a "scheme" element associated with a given cache name; null if
    *         the child is missing and is not required
    */
    protected XmlElement resolveScheme(XmlElement xmlScheme, CacheInfo info, boolean fChild, boolean fRequired)
        {
        return resolveScheme(m_xmlConfig, xmlScheme, info, fChild, fRequired, true);
        }

    /**
    * Obtain the NamedCache reference for the cache service defined by the
    * specified scheme.
    *
    * @param info       the cache info
    * @param xmlScheme  the scheme element for the cache
    * @param loader     (optional) ClassLoader that should be used to
    *                   deserialize objects in the cache
    *
    * @return NamedCache instance
    */
    protected NamedCache ensureCache(
            CacheInfo info, XmlElement xmlScheme, ClassLoader loader)
        {
        try
            {
            // The CacheService will check permission for the service and
            // cache. If a cache reference is handed out later, then permission
            // should be checked using DefaultConfigurableCacheFactory.
            // checkPermission().
            CacheService service = (CacheService) ensureService(xmlScheme);
            return service.ensureCache(info.getCacheName(), loader);
            }
        catch (ClassCastException e)
            {
            throw new IllegalArgumentException("Invalid scheme:\n" + xmlScheme);
            }
        }

    /**
    * Ensure the service for the specified scheme.
    *
    * @param xmlScheme  the scheme
    *
    * @return running Service corresponding to the scheme
    */
    public Service ensureService(XmlElement xmlScheme)
        {
        return ensureServiceInternal(
            resolveScheme(xmlScheme, null, false, false));
        }

    /**
    * Ensure the service for the specified scheme.
    *
    * @param xmlScheme  the scheme
    *
    * @return running Service corresponding to the scheme
    */
    protected Service ensureServiceInternal(XmlElement xmlScheme)
        {
        String  sSchemeType = xmlScheme.getName();
        int     nSchemeType = translateSchemeType(sSchemeType);
        Cluster cluster;
        String  sServiceType;

        switch (nSchemeType)
            {
            case SCHEME_REPLICATED:
                cluster      = CacheFactory.ensureCluster();
                sServiceType = CacheService.TYPE_REPLICATED;
                break;

            case SCHEME_OPTIMISTIC:
                cluster      = CacheFactory.ensureCluster();
                sServiceType = CacheService.TYPE_OPTIMISTIC;
                break;

            case SCHEME_DISTRIBUTED:
                cluster      = CacheFactory.ensureCluster();
                sServiceType = CacheService.TYPE_DISTRIBUTED;
                break;

            case SCHEME_LOCAL:
            case SCHEME_OVERFLOW:
            case SCHEME_DISK:
            case SCHEME_EXTERNAL:
            case SCHEME_EXTERNAL_PAGED:
            case SCHEME_CLASS:
            case SCHEME_FLASHJOURNAL:
            case SCHEME_RAMJOURNAL:
                cluster      = CacheFactory.getCluster();
                sServiceType = CacheService.TYPE_LOCAL;
                break;

            case SCHEME_NEAR:
                return ensureServiceInternal(
                    resolveScheme(xmlScheme.getSafeElement("back-scheme"), null, true, true));

            case SCHEME_VERSIONED_NEAR:
                {
                XmlElement xmlVer  = resolveScheme(
                    xmlScheme.getSafeElement("version-transient-scheme"), null, true, true);
                XmlElement xmlBack = resolveScheme(
                    xmlScheme.getSafeElement("back-scheme"), null, true, true);

                ensureServiceInternal(xmlVer);
                return ensureServiceInternal(xmlBack);
                }

            case SCHEME_INVOCATION:
                cluster      = CacheFactory.ensureCluster();
                sServiceType = InvocationService.TYPE_DEFAULT;
                break;

            case SCHEME_PROXY:
                cluster      = CacheFactory.ensureCluster();
                sServiceType = "Proxy";
                break;

            case SCHEME_REMOTE_CACHE:
                cluster      = CacheFactory.getCluster();
                sServiceType = CacheService.TYPE_REMOTE;
                break;

            case SCHEME_REMOTE_INVOCATION:
                cluster      = CacheFactory.getCluster();
                sServiceType = InvocationService.TYPE_REMOTE;
                break;

            case SCHEME_TRANSACTIONAL:
                throw new UnsupportedOperationException("Transactions are not supported in Coherence CE");

            default:
                throw new UnsupportedOperationException("ensureService: " + sSchemeType);
            }

        String sServiceName = xmlScheme.getSafeElement("service-name").getString();
        if (sServiceName.length() == 0)
            {
            sServiceName = sServiceType;
            }
        sServiceName = getScopedServiceName(sServiceName);

        // Note: SafeCluster implements Lockable (COH-23345)
        try (Lockable.Unlockable unlockable = ((Lockable) cluster).exclusively())
            {
            Service service = cluster.ensureService(sServiceName, sServiceType);
            if (service.isRunning())
                {
                if (service instanceof CacheService)
                    {
                    validateBackingMapManager((CacheService) service);
                    }
                }
            else
                {
                // merge the standard service config parameters
                XmlElement xmlConfig = CacheFactory.getServiceConfig(sServiceType);
                if (xmlConfig != null)
                    {
                    List listStandard = xmlConfig.getElementList();
                    for (int i = 0, c = listStandard.size(); i < c; i++)
                        {
                        XmlElement xmlParamStandard = (XmlElement) listStandard.get(i);
                        String     sParamName       = xmlParamStandard.getName();
                        XmlElement xmlParam         = xmlScheme.getElement(sParamName);

                        if (xmlParam != null && !XmlHelper.isEmpty(xmlParam))
                            {
                            xmlParam = resolveScheme
                                (m_xmlConfig, xmlParam, null, false, false, true);
                            listStandard.set(i, xmlParam.clone());
                            }
                        }
                    }

                // resolve nested serializers for remote and proxy services
                XmlElement xmlSub;
                switch (nSchemeType)
                    {
                    case SCHEME_PROXY:
                        xmlSub = xmlConfig.getSafeElement("acceptor-config");
                        resolveSerializer(xmlSub);
                        xmlSub = xmlSub.getElement("tcp-acceptor");
                        if (xmlSub != null)
                            {
                            resolveSocketProvider(xmlSub);
                            }
                        break;

                    case SCHEME_REMOTE_CACHE:
                    case SCHEME_REMOTE_INVOCATION:
                        xmlSub = xmlConfig.getSafeElement("initiator-config");
                        resolveSerializer(xmlSub);
                        xmlSub = xmlSub.getElement("tcp-initiator");
                        if (xmlSub != null)
                            {
                            resolveSocketProvider(xmlSub);
                            }
                        break;
                    }

                resolveSerializer(xmlConfig);

                // configure and start the service
                service.configure(xmlConfig);
                if (service instanceof CacheService)
                    {
                    BackingMapManager mgr =
                        instantiateBackingMapManager(nSchemeType, xmlScheme);
                    registerBackingMapManager(mgr);
                    ((CacheService) service).setBackingMapManager(mgr);
                    }

                startService(service);
                }
            return service;
            }
        }

    /**
    * Start the given {@link Service}.  Extensions of this class can
    * override this method to provide pre/post start functionality.
    *
    * @param service  the {@link Service} to start
    */
    protected void startService(Service service)
        {
        service.start();
        }

    /**
    * Apply the scope name prefix to the given service name.  The
    * name will be formatted as such: [scope name]:[service name]
    *
    * @param sServiceName  the service name
    *
    * @return the service name with scope prefix, if there is a scope
    * name configured
    */
    public String getScopedServiceName(String sServiceName)
        {
        String sScopeName = getScopeName();
        return (sScopeName == null || sScopeName.length() == 0) ?
                sServiceName : sScopeName + ":" + sServiceName;
        }

    /**
    * Resolve and inject service serializer elements based on defaults
    * defined in the cache configuration.
    *
    * @param xmlConfig  the configuration element to examine and modify
    */
    protected void resolveSerializer(XmlElement xmlConfig)
        {
        // check if the "serializer" element is missing or empty
        XmlElement xmlSerializer = xmlConfig.getElement("serializer");
        if (xmlSerializer == null || XmlHelper.isEmpty(xmlSerializer))
            {
            // remove an empty serializer element from the service config
            if (xmlSerializer != null)
                {
                XmlHelper.removeElement(xmlConfig, "serializer");
                }

            // apply the default serializer (if specified)
            xmlSerializer = getConfig().findElement("defaults/serializer");
            if (xmlSerializer != null)
                {
                xmlConfig.getElementList().add(xmlSerializer);
                }
            }
        }

    /**
    * Resolve and inject service socket-provider elements based on defaults
    * defined in the cache configuration.
    *
    * @param xmlConfig  the configuration element to examine and modify
    */
    protected void resolveSocketProvider(XmlElement xmlConfig)
        {
        // check if the "socket-provider" element is missing or empty
        XmlElement xmlProvider = xmlConfig.getElement("socket-provider");
        if (xmlProvider == null || XmlHelper.isEmpty(xmlProvider))
            {
            // remove an empty provider element from the service config
            if (xmlProvider != null)
                {
                XmlHelper.removeElement(xmlConfig, "socket-provider");
                }

            // apply the default socket-provider (if specified)
            xmlProvider = getConfig().findElement("defaults/socket-provider");
            if (xmlProvider != null)
                {
                xmlConfig.getElementList().add(xmlProvider);
                }
            }
        }

    /**
    * Instantiate a BackingMapManager for a given scheme type.
    * <p>
    * Note: we rely on the BackingMapManager implementations to have their
    * equals() method test for object identity using the "==" operator (in
    * other words <b>not</b> to override the hashCode() and equals() methods).
    *
    * @param nSchemeType  the scheme type (one of the SCHEME-* constants)
    * @param xmlScheme    the scheme used to configure the corresponding service
    *
    * @return a new BackingMapManager instance
    */
    protected BackingMapManager instantiateBackingMapManager(
            int nSchemeType, XmlElement xmlScheme)
        {
        switch (nSchemeType)
            {
            case SCHEME_TRANSACTIONAL:
            default:
                return new Manager();
            }
        }

    /**
    * Register the specified BackingMapManager as a "valid" one. That registry
    * is used to identify services configured and started by this factory and
    * prevent accidental usage of (potentially incompatible) cache services
    * with the same name created by other factories.
    *
    * @param mgr a BackingMapManager instance instantiated by this factory
    */
    protected void registerBackingMapManager(BackingMapManager mgr)
        {
        m_setManager.add(mgr);
        }

    /**
    * Ensures that the backing map manager of the specified service was
    * configured by this (or equivalent) factory. This validation is performed
    * to prevent accidental usage of (potentially incompatible) cache services
    * with the same name created by other factories.
    *
    * @param service  the CacheService to validate
    */
    protected void validateBackingMapManager(CacheService service)
        {
        BackingMapManager manager = service.getBackingMapManager();
        if (m_setManager.contains(manager))
            {
            return;
            }

        if (!(manager instanceof Manager))
            {
            throw new IllegalStateException("Service \"" + service.getInfo().getServiceName() +
                "\" has been started " + (manager == null ?
                    "without a BackingMapManager" :
                    "with a non-compatible BackingMapManager: " + manager));
            }

        DefaultConfigurableCacheFactory that = ((Manager) manager).getCacheFactory();

        if (that != this && !equals(that.getConfig(), this.getConfig()))
            {
            throw new IllegalStateException("Service \"" + service.getInfo().getServiceName() +
                "\" has been started by the factory with a different configuration descriptor");
            }
        }

    /**
    * Ensures a cache for given scheme.
    *
    * @param info       the cache info
    * @param xmlScheme  the corresponding scheme
    * @param loader     ClassLoader that should be used to deserialize
    *                   objects in the cache
    *
    * @return a named cache created according to the description
    *         in the configuration
    */
    public NamedCache configureCache(CacheInfo info, XmlElement xmlScheme, ClassLoader loader)
        {
        String     sSchemeType = xmlScheme.getName();
        NamedCache cache;

        switch (translateSchemeType(sSchemeType))
            {
            case SCHEME_CLASS:
                // check whether this class-scheme naturally implements
                // the NamedCache and return it without SafeCache "wrapping"
                Object oCache = instantiateAny(info, xmlScheme, null, loader);
                if (oCache instanceof NamedCache)
                    {
                    return (NamedCache) oCache;
                    }

                // break through for any other classes
            case SCHEME_REPLICATED:
            case SCHEME_OPTIMISTIC:
            case SCHEME_LOCAL:
            case SCHEME_OVERFLOW:
            case SCHEME_DISK:
            case SCHEME_EXTERNAL:
            case SCHEME_EXTERNAL_PAGED:
            case SCHEME_FLASHJOURNAL:
            case SCHEME_RAMJOURNAL:
                cache = ensureCache(info, xmlScheme, loader);
                break;

            case SCHEME_DISTRIBUTED:
                // TODO: move it up along with other schemes when bundling is
                //       implemented by the service itself
            case SCHEME_REMOTE_CACHE:
                {
                cache = ensureCache(info, xmlScheme, loader);

                XmlElement xmlBundling = xmlScheme.getElement("operation-bundling");
                if (xmlBundling != null)
                    {
                    cache = instantiateBundlingNamedCache(cache, xmlBundling);
                    }
                break;
                }

            case SCHEME_NEAR:
                {
                String     sCacheCtx = popCacheContext();
                XmlElement xmlFront  = resolveScheme(xmlScheme.getSafeElement("front-scheme"), info, true, true);
                Map        mapFront  = configureBackingMap(info, xmlFront, null, loader, null);
                XmlElement xmlBack   = resolveScheme(xmlScheme.getSafeElement("back-scheme"), info, true, true);
                NamedCache cacheBack = configureCache(info, xmlBack, loader);
                String     sStrategy = xmlScheme.getSafeElement("invalidation-strategy").getString("auto");
                int        nStrategy = sStrategy.equalsIgnoreCase("none")    ? NearCache.LISTEN_NONE
                                     : sStrategy.equalsIgnoreCase("present") ? NearCache.LISTEN_PRESENT
                                     : sStrategy.equalsIgnoreCase("all")     ? NearCache.LISTEN_ALL
                                     : sStrategy.equalsIgnoreCase("auto")    ? NearCache.LISTEN_AUTO
                                     : sStrategy.equalsIgnoreCase("logical") ? NearCache.LISTEN_LOGICAL
                                     :                                         Integer.MIN_VALUE;

                NearCache cacheNear;
                if (nStrategy == Integer.MIN_VALUE)
                    {
                    Logger.warn("Invalid invalidation strategy of '" + sStrategy +
                            "'; proceeding with default of 'auto'");
                    nStrategy = NearCache.LISTEN_AUTO;
                    }

                String sSubclass = xmlScheme.getSafeElement("class-name").getString();
                if (sSubclass.length() == 0)
                    {
                    cacheNear = instantiateNearCache(mapFront, cacheBack, nStrategy);
                    }
                else
                    {
                    Object[] aoParam = new Object[] {mapFront, cacheBack, nStrategy};
                    cacheNear = (NearCache) instantiateSubclass(sSubclass, NearCache.class, loader,
                        aoParam, xmlScheme.getElement("init-params"));
                    }

                if (sCacheCtx != null)
                    {
                    cacheNear.setRegistrationContext(sCacheCtx);
                    register(cacheNear, sCacheCtx);
                    }
                cache = cacheNear;
                break;
                }

            case SCHEME_VERSIONED_NEAR:
                {
                String     sCacheCtx = popCacheContext();
                XmlElement xmlFront  = resolveScheme(xmlScheme.getSafeElement("front-scheme"), info, true, true);
                Map        mapFront  = configureBackingMap(info, xmlFront, null, loader, null);
                XmlElement xmlBack   = resolveScheme(xmlScheme.getSafeElement("back-scheme"), info, true, true);
                NamedCache cacheBack = ensureCache(info, xmlBack, loader);
                XmlElement xmlVer    = xmlScheme.getSafeElement("version-transient-scheme");
                String     sSuffix   = xmlVer.getSafeElement("cache-name-suffix").getString("-version");

                xmlVer = resolveScheme(xmlVer, info, true, true);

                NamedCache cacheVer = ensureCache(info.getSyntheticInfo(sSuffix), xmlVer, loader);
                NearCache cacheNear;

                String sSubclass = xmlScheme.getSafeElement("class-name").getString();
                if (sSubclass.length() == 0)
                    {
                    cacheNear = instantiateVersionedNearCache(mapFront, cacheBack, cacheVer);
                    }
                else
                    {
                    Object[] aoParam = new Object[] {mapFront, cacheBack, cacheVer};
                    cacheNear = (NearCache) instantiateSubclass(sSubclass, VersionedNearCache.class, loader,
                        aoParam, xmlScheme.getElement("init-params"));
                    }

                if (sCacheCtx != null)
                    {
                    cacheNear.setRegistrationContext(sCacheCtx);
                    register(cacheNear, sCacheCtx);
                    }
                cache = cacheNear;
                break;
                }

            case SCHEME_TRANSACTIONAL:
                throw new UnsupportedOperationException("Transactions are not supported in Coherence CE");

            default:
                throw new UnsupportedOperationException("configureCache: " + sSchemeType);
            }

        verifyMapListener(info, cache, xmlScheme, null, loader, null);

        return cache;
        }

    /**
    * Construct an NearCache using the specified parameters.
    * <p>
    * This method exposes a corresponding NearCache
    * {@link NearCache#NearCache(Map, NamedCache, int) constructor}
    * and is provided for the express purpose of allowing its override.
    */
    protected NearCache instantiateNearCache(Map mapFront, NamedCache mapBack, int nStrategy)
        {
        return new NearCache(mapFront, mapBack, nStrategy);
        }

    /**
    * Construct an VersionedNearCache using the specified parameters.
    * <p>
    * This method exposes a corresponding VersionedNearCache
    * {@link VersionedNearCache#VersionedNearCache(Map, NamedCache, NamedCache) constructor}
    * and is provided for the express purpose of allowing its override.
    */
    protected VersionedNearCache instantiateVersionedNearCache(Map mapLocal, NamedCache mapDist, NamedCache mapVersion)
        {
        return new VersionedNearCache(mapLocal, mapDist, mapVersion);
        }

    /**
    * Configures a backing map according to the scheme.
    *
    * @param info          the cache info
    * @param xmlScheme     the scheme element for cache configuration
    * @param context       BackingMapManagerContext to be used
    * @param loader        the ClassLoader to instantiate necessary classes
    * @param mapListeners  map of registered map listeners keyed by the
    *                      corresponding map references
    *
    * @return a backing map configured according to the scheme
    */
    public Map configureBackingMap(CacheInfo info, XmlElement xmlScheme,
            BackingMapManagerContext context, ClassLoader loader, Map mapListeners)
        {
        xmlScheme = resolveBackingMapScheme(info, xmlScheme);

        int    nType = translateSchemeType(xmlScheme.getName());
        String sCtx  = popCacheContext();
        Map    map;

        if (loader == null && context != null)
            {
            loader = context.getClassLoader();
            }

        String sPartitioned = xmlScheme.getSafeAttribute("partitioned").getString();
        if (isPartitioned(sPartitioned, nType)) // default is "false"
            {
            if (nType == SCHEME_READ_WRITE_BACKING)
                {
                map = instantiateReadWriteBackingMap(info, xmlScheme, context, mapListeners);
                }
            else
                {
                XmlElement xmlTemp = (XmlElement) xmlScheme.clone();
                xmlTemp.setAttribute("partitioned", new SimpleValue("false"));

                PartitionedBackingMapManager mgrInner =
                    new PartitionedBackingMapManager(info, xmlTemp, context, loader);
                mgrInner.init(context);

                PartitionAwareBackingMap pabm;
                String sCacheName = info.getCacheName();


                if (sPartitioned.equals("observable") ||
                    nType == SCHEME_FLASHJOURNAL || nType == SCHEME_RAMJOURNAL)
                    {
                    // for "observable" and the flash/ram journal schemes
                    // the ObservableSplittingBackingMap should be used
                    pabm = new ObservableSplittingBackingMap(mgrInner, sCacheName);
                    }
                else
                    {
                    // default behavior for everything else
                    pabm = new ObservableSplittingBackingCache(mgrInner, sCacheName);
                    }
                map = pabm;
                }
            }
        else
            {
            switch (nType)
                {
                case SCHEME_LOCAL:
                    map = instantiateLocalCache(info, xmlScheme, context, loader);
                    break;

                case SCHEME_READ_WRITE_BACKING:
                    if (context == null)
                        {
                        throw new IllegalArgumentException("ReadWriteBackingMap requires BackingMapManagerContext");
                        }
                    map = instantiateReadWriteBackingMap(info, xmlScheme, context, mapListeners);
                    break;

                case SCHEME_VERSIONED_BACKING:
                    if (context == null)
                        {
                        throw new IllegalArgumentException("VersionedBackingMap requires BackingMapManagerContext");
                        }
                    map = instantiateVersionedBackingMap(info, xmlScheme, context, mapListeners);
                    break;

                case SCHEME_OVERFLOW:
                    map = instantiateOverflowBackingMap(info, xmlScheme, context, loader, mapListeners);
                    break;

                case SCHEME_DISK:
                    map = instantiateDiskBackingMap(info, xmlScheme, context, loader);
                    break;

                case SCHEME_EXTERNAL:
                    map = instantiateExternalBackingMap(info, xmlScheme, context, loader);
                    break;

                case SCHEME_EXTERNAL_PAGED:
                    map = instantiatePagedExternalBackingMap(info, xmlScheme, context, loader);
                    break;

                case SCHEME_REMOTE_CACHE:
                    map = configureCache(info, xmlScheme, loader);
                    break;

                case SCHEME_FLASHJOURNAL:
                    map = instantiateFlashJournalBackingMap(info, xmlScheme, context, loader);
                    break;

                case SCHEME_RAMJOURNAL:
                    map = instantiateRamJournalBackingMap(info, xmlScheme, context, loader);
                    break;

                case SCHEME_CLASS:
                    map = instantiateMap(info, xmlScheme, context, loader);
                    break;

                default:
                    throw new UnsupportedOperationException("configureBackingMap: " + xmlScheme.getName());
                }
            }

        // don't register event listeners and MBeans for individual partitions
        if (xmlScheme.getAttribute("partition-name") == null)
            {
            verifyMapListener(info, map, xmlScheme, context, loader, mapListeners);

            if (sCtx != null && context != null)
                {
                register(context.getCacheService(), info.getCacheName(), sCtx, map);
                }
            }

        return map;
        }

    /**
    * Traverse the specified scheme to find an enclosed "backing-map-scheme" or
    * a scheme that could serve as such.
    *
    * @param info       the cache info
    * @param xmlScheme  the scheme element for cache configuration
    *
    * @return the resolved backing map scheme
    */
    public XmlElement resolveBackingMapScheme(CacheInfo info, XmlElement xmlScheme)
        {
        String sSchemeType = xmlScheme.getName();

        switch (translateSchemeType(sSchemeType))
            {
            case SCHEME_REPLICATED:
            case SCHEME_OPTIMISTIC:
            case SCHEME_DISTRIBUTED:
                {
                XmlElement xmlBM  = xmlScheme.getSafeElement("backing-map-scheme");
                XmlElement xmlMap = resolveScheme(xmlBM, info, true, true);

                // transfer a non-default value as an attribute to the actual map config
                XmlElement xmlPartitioned = xmlBM.getSafeElement("partitioned");
                if (xmlPartitioned != null)
                    {
                    xmlMap.addAttribute("partitioned").setString(xmlPartitioned.getString());
                    }
                return xmlMap;
                }
            case SCHEME_NEAR:
                {
                XmlElement xmlBack = resolveScheme(xmlScheme.getSafeElement("back-scheme"), info, true, true);
                return resolveBackingMapScheme(info, xmlBack);
                }
            case SCHEME_VERSIONED_NEAR:
                {
                // we need to figure out what backing map is requested:
                // the one for the cache data itself or the one for version info
                XmlElement xmlBack  = resolveScheme(xmlScheme.getSafeElement("back-scheme"), info, true, true);
                XmlElement xmlVer   = xmlScheme.getSafeElement("version-transient-scheme");
                String     sSuffix  = xmlVer.getSafeElement("cache-name-suffix").getString("-version");

                xmlVer = resolveScheme(xmlVer, info, true, true);

                return resolveBackingMapScheme(info,
                    info.getCacheName().endsWith(sSuffix) ? xmlVer : xmlBack);
                }
            default:
                return xmlScheme;
            }
        }

    /**
    * Check whether or not a MapListener has to be instantiated and
    * added to a Map according to a scheme definition.
    *
    * @param info          the cache info
    * @param map           an ObservableMap to add a listener to
    * @param xmlScheme     the corresponding scheme
    * @param context       BackingMapManagerContext to be used
    * @param loader        ClassLoader that should be used to instantiate
    *                      a MapListener object
    * @param mapListeners  map of registered map listeners keyed by the
    *                      corresponding map references
    *
    * @throws IllegalArgumentException if the listener is required, but the
    *         map does not implement ObservableMap interface or if the
    *         listener cannot be instantiated
    */
    protected void verifyMapListener(CacheInfo info, Map map, XmlElement xmlScheme,
            BackingMapManagerContext context, ClassLoader loader, Map mapListeners)
        {
        // CONSIDER: allow to configure filter-based listener and replace
        //      mapListeners values with MapListenerSupport objects
        XmlElement xmlListener = xmlScheme.getSafeElement("listener");
        XmlElement xmlClass    = resolveScheme(xmlListener, info, true, false);
        if (xmlClass != null)
            {
            String sTier = xmlScheme.getSafeAttribute("tier").getString();
            if (sTier.length() > 0)
                {
                // The "tier" is a synthetic attribute only injected for the top
                // level scheme definition. A listener could be defined at this
                // level in two cases:
                // (a) standard top level cache definition
                // (b) local cache using the same scheme for both front and
                //     back tier declaration
                // To cover a very esoteric case when one desires to listen
                // to the actual LocalCache instance that serves as a
                // backing map for a "local-scheme" we support an undocumented
                // "target" attribute with a valid value of "backing-map"
                boolean fBackOnly = xmlListener.getSafeAttribute("target").
                                        getString().equals("backing-map");
                if (sTier.equals("front") && fBackOnly)
                    {
                    // the backing-map listener is requested
                    return;
                    }
                if (sTier.equals("back") && !fBackOnly)
                    {
                    // prevent local cache registering a listener twice:
                    // once for the front tier and the second time for the back
                    return;
                    }
                }

            MapListener listener = instantiateMapListener(info, xmlClass, context, loader);
            try
                {
                ((ObservableMap) map).addMapListener(listener);
                if (mapListeners != null)
                    {
                    mapListeners.put(map, listener);
                    }
                }
            catch (ClassCastException e)
                {
                throw new IllegalArgumentException("Map is not observable: " + map.getClass());
                }
            }
        }

    /**
    * Create a ReadWriteBackingMap using the "read-write-backing-map-scheme" element.
    *
    * @param info          the cache info
    * @param xmlRWBM       "read-write-backing-map-scheme" element
    * @param context       BackingMapManagerContext to be used
    * @param mapListeners  map of registered map listeners keyed by the
    *                      corresponding map references
    *
    * @return a newly instantiated Map
    */
    protected Map instantiateReadWriteBackingMap(
            CacheInfo info, XmlElement xmlRWBM, BackingMapManagerContext context, Map mapListeners)
        {
        XmlElement  xmlInternal     = resolveScheme(xmlRWBM.getSafeElement("internal-cache-scheme"), info, true, true);
        XmlElement  xmlMisses       = resolveScheme(xmlRWBM.getSafeElement("miss-cache-scheme"), info, true, false);
        XmlElement  xmlStore        = resolveScheme(xmlRWBM.getSafeElement("cachestore-scheme"), info, false, false);
        ClassLoader loader          = context.getClassLoader();
        Map         mapMisses       = xmlMisses == null ? null :
                                            instantiateLocalCache(info, xmlMisses, context, loader);
        boolean     fReadOnly       = xmlRWBM.getSafeElement("read-only").getBoolean();
        double      dflRefreshAhead = convertDouble(xmlRWBM.getSafeElement("refresh-ahead-factor"));
        double      dflWriteFactor  = convertDouble(xmlRWBM.getSafeElement("write-batch-factor"));
        int         cWriteRequeue   = convertInt(xmlRWBM.getSafeElement("write-requeue-threshold"));
        String      sSplitting      = xmlRWBM.getSafeAttribute("partitioned").getString();
        boolean     fSplitting      = isPartitioned(sSplitting, translateSchemeType(xmlInternal.getName()));
        long        cStoreTimeout   = parseTime(xmlRWBM.getSafeElement("cachestore-timeout").getString("0"));
        boolean     fRethrow        = xmlRWBM.getSafeElement("rollback-cachestore-failures").getBoolean(true);
        int         cBatchSize      = convertInt(xmlRWBM.getSafeElement("write-max-batch-size"), 128);

        if (!fRethrow)
            {
            Logger.warn("The rollback-cachestore-failures setting is explicitly " +
                    "configured to prevent CacheStore exceptions from being " +
                    "propagated to the client; this setting is not recommended " +
                    "and has been deprecated");
            }

        // get the write behind delay; if the "write-delay" element exists, try
        // to parse it; otherwise, parse the "write-delay-seconds" element
        long   cWriteBehindMillis;
        String sWriteDelay = xmlRWBM.getSafeElement("write-delay").getString();
        if (sWriteDelay == null || sWriteDelay.length() == 0)
            {
            cWriteBehindMillis = 1000L * xmlRWBM.getSafeElement("write-delay-seconds").getInt();
            }
        else
            {
            cWriteBehindMillis = parseTime(sWriteDelay, UNIT_S);
            }
        int cWriteBehindSec = cWriteBehindMillis == 0 ? 0 :
                Math.max(1, (int) (cWriteBehindMillis / 1000));

        ObservableMap mapInternal;
        try
            {
            if (fSplitting)
                {
                xmlInternal = (XmlElement) xmlInternal.clone();
                xmlInternal.addAttribute("partitioned").setString(sSplitting);
                }
            mapInternal = (ObservableMap) configureBackingMap(info, xmlInternal, context, null, mapListeners);
            }
        catch (ClassCastException e)
            {
            throw new IllegalArgumentException("Map is not observable:\n" + xmlInternal);
            }

        Object           store       = instantiateCacheStore(info, xmlStore, context, loader);
        BinaryEntryStore storeBinary = null;

        if (store instanceof BinaryEntryStore)
            {
            // If the store implements the BinaryEntryStore interface, use it.
            // The only exception from that rule is the SCHEME_REMOTE_CACHE case,
            // (which always returns the SafeNamedCache), that was de-optimized
            // due to the Serializers incompatibility
            if (!(store instanceof NamedCache
                     && store instanceof ClassLoaderAware
                     && ((ClassLoaderAware) store).getContextClassLoader() !=
                        NullImplementation.getClassLoader()))
                {
                storeBinary = (BinaryEntryStore) store;
                }
            }

        ReadWriteBackingMap rwbm;

        String sSubclass = xmlRWBM.getSafeElement("class-name").getString();
        if (sSubclass.length() == 0)
            {
            if (storeBinary == null)
                {
                CacheLoader storeObject = (CacheLoader) store;
                rwbm = fSplitting ?
                    instantiateReadWriteSplittingBackingMap(context, (PartitionAwareBackingMap) mapInternal,
                        mapMisses, storeObject, fReadOnly, cWriteBehindSec, dflRefreshAhead) :
                    instantiateReadWriteBackingMap(context, mapInternal, mapMisses, storeObject, fReadOnly,
                        cWriteBehindSec, dflRefreshAhead);
                }
            else
                {
                rwbm = fSplitting ?
                    instantiateReadWriteSplittingBackingMap(context, (PartitionAwareBackingMap) mapInternal,
                        mapMisses, storeBinary, fReadOnly, cWriteBehindSec, dflRefreshAhead) :
                    instantiateReadWriteBackingMap(context, mapInternal, mapMisses, storeBinary, fReadOnly,
                        cWriteBehindSec, dflRefreshAhead);
                }
            }
        else
            {
            Object[] aoParam = storeBinary == null ?
                new Object[] {context, mapInternal, mapMisses, store, fReadOnly,
                              cWriteBehindSec, dflRefreshAhead} :
                new Object[] {context, mapInternal, mapMisses, storeBinary, fReadOnly,
                              cWriteBehindSec, dflRefreshAhead};
            rwbm = (ReadWriteBackingMap) instantiateSubclass(sSubclass, ReadWriteBackingMap.class, loader,
                aoParam, xmlRWBM.getElement("init-params"));
            }

        // Read/Write Threads will have the cache name appended to the thread name
        rwbm.setCacheName(info.getCacheName());
        rwbm.setRethrowExceptions(fRethrow);
        rwbm.setWriteBatchFactor(dflWriteFactor);
        rwbm.setWriteRequeueThreshold(cWriteRequeue);
        rwbm.setWriteMaxBatchSize(cBatchSize);
        if (cWriteBehindMillis != 1000L * cWriteBehindSec)
            {
            rwbm.setWriteBehindMillis(cWriteBehindMillis);
            }
        rwbm.setCacheStoreTimeoutMillis(cStoreTimeout);

        XmlElement xmlBundling = xmlStore.getElement("operation-bundling");
        if (xmlBundling != null)
            {
            ReadWriteBackingMap.StoreWrapper storeWrapper = rwbm.getCacheStore();
            for (Iterator iter = xmlBundling.getElements("bundle-config");
                    iter.hasNext();)
                {
                XmlElement xmlBundle = (XmlElement) iter.next();

                String sOperation = xmlBundle.getSafeElement("operation-name").getString("all");
                int    cBundle    = convertInt(xmlBundle.getSafeElement("preferred-size"));

                if (sOperation.equals("all"))
                    {
                    initializeBundler(storeWrapper.ensureLoadBundler(cBundle), xmlBundle);
                    initializeBundler(storeWrapper.ensureStoreBundler(cBundle), xmlBundle);
                    initializeBundler(storeWrapper.ensureEraseBundler(cBundle), xmlBundle);
                    }
                else if (sOperation.equals("load"))
                    {
                    initializeBundler(storeWrapper.ensureLoadBundler(cBundle), xmlBundle);
                    }
                else if (sOperation.equals("store"))
                    {
                    initializeBundler(storeWrapper.ensureStoreBundler(cBundle), xmlBundle);
                    }
                else if (sOperation.equals("erase"))
                    {
                    initializeBundler(storeWrapper.ensureEraseBundler(cBundle), xmlBundle);
                    }
                else
                    {
                    throw new IllegalArgumentException(
                        "Invalid \"operation-name\" element:\n" + xmlBundle);
                    }
                }
            }

        return rwbm;
        }

    /**
    * Construct a ReadWriteBackingMap using the specified parameters.
    * <p>
    * This method exposes a corresponding ReadWriteBackingMap
    * {@link ReadWriteBackingMap#ReadWriteBackingMap(BackingMapManagerContext, ObservableMap, Map, CacheLoader, boolean, int, double) constructor}
    * and is provided for the express purpose of allowing its override.
    */
    protected ReadWriteBackingMap instantiateReadWriteBackingMap(BackingMapManagerContext context,
            ObservableMap mapInternal, Map mapMisses, CacheLoader store, boolean fReadOnly,
            int cWriteBehindSeconds, double dflRefreshAheadFactor)
        {
        return new ReadWriteBackingMap(context, mapInternal, mapMisses, store, fReadOnly, cWriteBehindSeconds, dflRefreshAheadFactor);
        }

    /**
    * Construct a ReadWriteBackingMap using the specified parameters.
    * <p>
    * This method exposes a corresponding ReadWriteBackingMap
    * {@link ReadWriteBackingMap#ReadWriteBackingMap(BackingMapManagerContext, ObservableMap, Map, BinaryEntryStore, boolean, int, double) constructor}
    * and is provided for the express purpose of allowing its override.
    */
    protected ReadWriteBackingMap instantiateReadWriteBackingMap(BackingMapManagerContext context,
            ObservableMap mapInternal, Map mapMisses, BinaryEntryStore storeBinary, boolean fReadOnly,
            int cWriteBehindSeconds, double dflRefreshAheadFactor)
        {
        return new ReadWriteBackingMap(context, mapInternal, mapMisses, storeBinary, fReadOnly, cWriteBehindSeconds, dflRefreshAheadFactor);
        }

    /**
    * Construct a ReadWriteSplittingBackingMap using the specified parameters.
    * <p>
    * This method exposes a corresponding ReadWriteSplittingBackingMap
    * {@link ReadWriteSplittingBackingMap#ReadWriteSplittingBackingMap(BackingMapManagerContext, PartitionAwareBackingMap, Map, CacheLoader, boolean, int, double) constructor}
    * and is provided for the express purpose of allowing its override.
    */
    protected ReadWriteSplittingBackingMap instantiateReadWriteSplittingBackingMap(BackingMapManagerContext context,
            PartitionAwareBackingMap mapInternal, Map mapMisses, CacheLoader store, boolean fReadOnly,
            int cWriteBehindSeconds, double dflRefreshAheadFactor)
        {
        return new ReadWriteSplittingBackingMap(context, mapInternal, mapMisses, store, fReadOnly, cWriteBehindSeconds, dflRefreshAheadFactor);
        }

    /**
    * Construct a ReadWriteSplittingBackingMap using the specified parameters.
    * <p>
    * This method exposes a corresponding ReadWriteSplittingBackingMap
    * {@link ReadWriteSplittingBackingMap#ReadWriteSplittingBackingMap(BackingMapManagerContext, PartitionAwareBackingMap, Map, BinaryEntryStore, boolean, int, double) constructor}
    * and is provided for the express purpose of allowing its override.
    */
    protected ReadWriteSplittingBackingMap instantiateReadWriteSplittingBackingMap(BackingMapManagerContext context,
            PartitionAwareBackingMap mapInternal, Map mapMisses, BinaryEntryStore storeBinary, boolean fReadOnly,
            int cWriteBehindSeconds, double dflRefreshAheadFactor)
        {
        return new ReadWriteSplittingBackingMap(context, mapInternal, mapMisses, storeBinary, fReadOnly, cWriteBehindSeconds, dflRefreshAheadFactor);
        }

    /**
    * Create a VersionedBackingMap using the "versioned-backing-map-scheme" element.
    *
    * @param info          the cache info
    * @param xmlVBM        "versioned-backing-map-scheme" element
    * @param context       BackingMapManagerContext to be used
    * @param mapListeners  map of registered map listeners keyed by the
    *                      corresponding map references
    *
    * @return a newly instantiated Map
    */
    protected Map instantiateVersionedBackingMap(
            CacheInfo info, XmlElement xmlVBM, BackingMapManagerContext context, Map mapListeners)
        {
        XmlElement xmlPersist     = xmlVBM.getElement("version-persistent-scheme");
        String     sPersistSuffix = "-persist";
        if (xmlPersist != null)
            {
            sPersistSuffix = xmlPersist.getSafeElement("cache-name-suffix").getString(sPersistSuffix);
            xmlPersist     = resolveScheme(xmlPersist, info, true, false);
            if (info.getCacheName().endsWith(sPersistSuffix) && xmlPersist != null)
                {
                return configureBackingMap(info, xmlPersist, context, null, mapListeners);
                }
            }

        XmlElement xmlTrans     = xmlVBM.getElement("version-transient-scheme");
        String     sTransSuffix = "-version";
        if (xmlTrans != null)
            {
            sTransSuffix = xmlTrans.getSafeElement("cache-name-suffix").getString(sTransSuffix);
            xmlTrans     = resolveScheme(xmlTrans, info, true, false);
            if (info.getCacheName().endsWith(sTransSuffix) && xmlTrans != null)
                {
                return configureBackingMap(info, xmlTrans, context, null, mapListeners);
                }
            }

        XmlElement  xmlInternal     = resolveScheme(xmlVBM.getSafeElement("internal-cache-scheme"), info, true, true);
        XmlElement  xmlMisses       = resolveScheme(xmlVBM.getSafeElement("miss-cache-scheme"), info, true, false);
        XmlElement  xmlStore        = resolveScheme(xmlVBM.getSafeElement("cachestore-scheme"), info, false, false);
        ClassLoader loader          = context.getClassLoader();
        Map         mapMisses       = xmlMisses == null ? null :
                                          instantiateLocalCache(info, xmlMisses, context, loader);
        boolean     fReadOnly       = xmlVBM.getSafeElement("read-only").getBoolean();
        double      dflRefreshAhead = convertDouble(xmlVBM.getSafeElement("refresh-ahead-factor"));
        boolean     fRethrow        = xmlVBM.getSafeElement("rollback-cachestore-failures").getBoolean(true);
        double      dflWriteFactor  = convertDouble(xmlVBM.getSafeElement("write-batch-factor"));
        int         cWriteRequeue   = convertInt(xmlVBM.getSafeElement("write-requeue-threshold"));
        NamedCache  cachePersist    = xmlPersist == null ? null :
                                          ensureCache(info.getSyntheticInfo(sPersistSuffix), xmlPersist, loader);
        NamedCache  cacheTrans      = xmlTrans == null ? null :
                                          ensureCache(info.getSyntheticInfo(sTransSuffix), xmlTrans, loader);
        boolean     fManageTrans    = xmlVBM.getSafeElement("manage-transient").getBoolean();

        if (!fRethrow)
            {
            Logger.warn("The rollback-cachestore-failures setting is explicitly " +
                    "configured to prevent CacheStore exceptions from being " +
                    "propagated to the client; this setting is not recommended " +
                    "and has been deprecated");
            }

        // get the write behind delay; if the "write-delay" element exists, try
        // to parse it; otherwise, parse the "write-delay-seconds" element
        long   cWriteBehindMillis;
        String sWriteDelay = xmlVBM.getSafeElement("write-delay").getString();
        if (sWriteDelay == null || sWriteDelay.length() == 0)
            {
            cWriteBehindMillis = 1000L * xmlVBM.getSafeElement("write-delay-seconds").getInt();
            }
        else
            {
            cWriteBehindMillis = parseTime(sWriteDelay, UNIT_S);
            }
        int cWriteBehindSec = cWriteBehindMillis == 0 ? 0 :
            Math.max(1, (int) (cWriteBehindMillis / 1000));

        ObservableMap mapInternal;
        try
            {
            mapInternal = (ObservableMap) configureBackingMap(info, xmlInternal, context, null, mapListeners);
            }
        catch (ClassCastException e)
            {
            throw new IllegalArgumentException("Map is not observable:\n" + xmlInternal);
            }

        VersionedBackingMap vbm;

        CacheLoader store = (CacheLoader) instantiateCacheStore(info, xmlStore, context, loader);

        String sSubclass = xmlVBM.getSafeElement("class-name").getString();
        if (sSubclass.length() == 0)
            {
            vbm = store instanceof CacheStore ?
                instantiateVersionedBackingMap(context, mapInternal, mapMisses, (CacheStore) store, fReadOnly, cWriteBehindSec,
                    dflRefreshAhead, cacheTrans, cachePersist, fManageTrans) :
                instantiateVersionedBackingMap(context, mapInternal, mapMisses, store,
                    cacheTrans, cachePersist, fManageTrans);
            }
        else
            {
            Object[] aoParam = store instanceof CacheStore ?
                new Object[] {context, mapInternal, mapMisses, store, fReadOnly, cWriteBehindSec,
                              dflRefreshAhead, cacheTrans, cachePersist, fManageTrans} :
                new Object[] {context, mapInternal, mapMisses, store,
                              cacheTrans, cachePersist, fManageTrans};
            vbm = (VersionedBackingMap) instantiateSubclass(sSubclass, VersionedBackingMap.class, loader,
                aoParam, xmlVBM.getElement("init-params"));
            }

        vbm.setRethrowExceptions(fRethrow);
        vbm.setWriteBatchFactor(dflWriteFactor);
        vbm.setWriteRequeueThreshold(cWriteRequeue);
        if (cWriteBehindMillis != 1000L * cWriteBehindSec)
            {
            vbm.setWriteBehindMillis(cWriteBehindMillis);
            }

        return vbm;
        }

    /**
    * Construct a VersionedBackingMap using the specified parameters.
    * <p>
    * This method exposes a corresponding VersionedBackingMap
    * {@link VersionedBackingMap#VersionedBackingMap(BackingMapManagerContext, ObservableMap, Map,
    * CacheStore, boolean, int, double, NamedCache, NamedCache, boolean) constructor}
    * and is provided for the express purpose of allowing its override.
    */
    protected VersionedBackingMap instantiateVersionedBackingMap(BackingMapManagerContext context,
            ObservableMap mapInternal, Map mapMisses, CacheStore store, boolean fReadOnly,
            int cWriteBehindSeconds, double dflRefreshAheadFactor, NamedCache mapVersionTransient,
            NamedCache mapVersionPersist, boolean fManageTransient)
        {
        return new VersionedBackingMap(context, mapInternal, mapMisses, store, fReadOnly, cWriteBehindSeconds,
            dflRefreshAheadFactor, mapVersionTransient, mapVersionPersist, fManageTransient);
        }

    /**
    * Construct a VersionedBackingMap using the specified parameters.
    * <p>
    * This method exposes a corresponding VersionedBackingMap
    * {@link VersionedBackingMap#VersionedBackingMap(BackingMapManagerContext, ObservableMap, Map,
    * CacheLoader, NamedCache, NamedCache, boolean) constructor}
    * and is provided for the express purpose of allowing its override.
    */
    protected VersionedBackingMap instantiateVersionedBackingMap(BackingMapManagerContext context,
            ObservableMap mapInternal, Map mapMisses, CacheLoader loader,
            NamedCache mapVersionTransient, NamedCache mapVersionPersist, boolean fManageTransient)
        {
        return new VersionedBackingMap(context, mapInternal, mapMisses, loader,
            mapVersionTransient, mapVersionPersist, fManageTransient);
        }

    /**
    * Create a backing Map using the "local-scheme" element.
    *
    * @param info      the cache info
    * @param xmlLocal  "local-scheme" element
    * @param context   BackingMapManagerContext to be used
    * @param loader    the ClassLoader to instantiate necessary classes
    *
    * @return a newly instantiated Map
    */
    protected Map instantiateLocalCache(CacheInfo info, XmlElement xmlLocal, BackingMapManagerContext context, ClassLoader loader)
        {
        int cHighUnits         = (int) parseMemorySize(xmlLocal.getSafeElement("high-units").getString("0"));
        int cLowUnits          = (int) parseMemorySize(xmlLocal.getSafeElement("low-units" ).getString("0"));
        int cExpiryDelayMillis = (int) parseTime(xmlLocal.getSafeElement("expiry-delay").getString("0"), UNIT_S);

        // check and default all of the Cache options
        if (cHighUnits <= 0)
            {
            cHighUnits = Integer.MAX_VALUE;
            }
        if (cLowUnits <= 0)
            {
            cLowUnits = (int) (cHighUnits * LocalCache.DEFAULT_PRUNE);
            }
        if (cExpiryDelayMillis < 0)
            {
            cExpiryDelayMillis = 0;
            }

        // configure and return the LocalCache
        LocalCache cache;
        String sSubclass = xmlLocal.getSafeElement("class-name").getString();
        if (sSubclass.length() == 0)
            {
            cache = instantiateLocalCache(cHighUnits, cExpiryDelayMillis);
            }
        else
            {
            Object[] aoParam = new Object[] {cHighUnits, cExpiryDelayMillis};
            cache = (LocalCache) instantiateSubclass(sSubclass, LocalCache.class, loader,
                aoParam, xmlLocal.getElement("init-params"));
            }
        cache.setLowUnits(cLowUnits);

        XmlElement xmlEviction = xmlLocal.getElement("eviction-policy");
        if (xmlEviction != null)
            {
            String sEvictionType = xmlEviction.getString();
            int    nEvictionType = sEvictionType.equalsIgnoreCase("HYBRID") ? LocalCache.EVICTION_POLICY_HYBRID
                                 : sEvictionType.equalsIgnoreCase("LRU")    ? LocalCache.EVICTION_POLICY_LRU
                                 : sEvictionType.equalsIgnoreCase("LFU")    ? LocalCache.EVICTION_POLICY_LFU
                                 :                                            Integer.MIN_VALUE;

            if (nEvictionType >= 0)
                {
                cache.setEvictionType(nEvictionType);
                }
            else
                {
                XmlElement xmlClass = xmlEviction.getElement("class-scheme");
                if (xmlClass == null)
                    {
                    throw new IllegalArgumentException("Unknown eviction policy:\n"
                            + xmlEviction);
                    }

                try
                    {
                    cache.setEvictionPolicy((ConfigurableCacheMap.EvictionPolicy)
                        instantiateAny(info, xmlClass, context, loader));
                    }
                catch (Exception e)
                    {
                    throw Base.ensureRuntimeException(e,
                            "Error instantiating custom eviction policy:\n"
                            + xmlEviction);
                    }
                }
            }

        configureUnitCalculator(xmlLocal, cache, info, context, loader);

        XmlElement  xmlStore = resolveScheme(xmlLocal.getSafeElement("cachestore-scheme"), info, false, false);
        CacheLoader store    = (CacheLoader) instantiateCacheStore(info, xmlStore, context, loader);
        if (store != null)
            {
            cache.setCacheLoader(store);
            }

        if (xmlLocal.getSafeElement("pre-load").getBoolean())
            {
            try
                {
                cache.loadAll();
                }
            catch (Throwable e)
                {
                String sText = "An exception occurred while pre-loading the \"" + info.getCacheName() + "\" cache:"
                    + '\n' + indentString(getStackTrace(e), "    ")
                    + "\nThe following configuration was used for the \"" + info.getCacheName() + "\" cache:"
                    + '\n' + indentString(xmlLocal.toString(), "    ");
                if (!(e instanceof Error))
                    {
                    sText += "\n(The exception has been logged and will be ignored.)";
                    }
                Logger.warn(sText);
                if (e instanceof Error)
                    {
                    throw (Error) e;
                    }
                }
            }

        return cache;
        }

    /**
    * Configure a UnitCalculator for the specified ConfigurableCacheMap.
    *
    * @param xmlCache  cache scheme that may contain a "unit-calculator" element
    * @param cache     the corresponding ConfigurableCacheMap
    * @param info      the cache info
    * @param context   BackingMapManagerContext to be used
    * @param loader    the ClassLoader to instantiate necessary classes
    */
    protected void configureUnitCalculator(XmlElement xmlCache, ConfigurableCacheMap cache,
                CacheInfo info,  BackingMapManagerContext context, ClassLoader loader)
        {
        XmlElement xmlCalculator = xmlCache.getElement("unit-calculator");
        if (xmlCalculator == null)
            {
            return;
            }

        ConfigurableCacheMap.UnitCalculator calculator;

        String sType = xmlCalculator.getString();
        if (sType.equalsIgnoreCase("FIXED"))
            {
            calculator = LocalCache.INSTANCE_FIXED;
            }
        else if (sType.equalsIgnoreCase("BINARY"))
            {
            calculator = LocalCache.INSTANCE_BINARY;
            }
        else
            {
            XmlElement xmlClass = xmlCalculator.getElement("class-scheme");
            if (xmlClass == null)
                {
                throw new IllegalArgumentException("Unknown unit calculator:\n"
                        + xmlCalculator);
                }

            try
                {
                calculator = (ConfigurableCacheMap.UnitCalculator)
                    instantiateAny(info, xmlClass, context, loader);
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e,
                        "Error instantiating custom unit calculator:\n"
                        + xmlCalculator);
                }
            }

        if (calculator != null)
            {
            cache.setUnitCalculator(calculator);

            XmlElement xmlFactor = xmlCache.getElement("unit-factor");
            if (xmlFactor != null)
                {
                cache.setUnitFactor((int) parseMemorySize(xmlFactor.getString("1")));
                }
            }
        }

    /**
    * Construct a LocalCache using the specified parameters.
    * <p>
    * This method exposes a corresponding LocalCache
    * {@link LocalCache#LocalCache(int, int) constructor}
    * and is provided for the express purpose of allowing its override.
    *
    * @param cUnits         high watermark
    * @param cExpiryMillis  the expiry value
    *
    * @return a newly instantiated LocalCache
    */
    protected LocalCache instantiateLocalCache(int cUnits, int cExpiryMillis)
        {
        return new LocalCache(cUnits, cExpiryMillis);
        }

    /**
    * Create a backing Map using the "overflow-scheme" element.
    *
    * @param info          the cache info
    * @param xmlOverflow   "overflow-scheme" element
    * @param context       BackingMapManagerContext to be used
    * @param loader        the ClassLoader to instantiate necessary classes
    * @param mapListeners  map of registered map listeners keyed by the
    *                      corresponding map references
    *
    * @return a newly instantiated Map
    */
    protected Map instantiateOverflowBackingMap(CacheInfo info, XmlElement xmlOverflow,
            BackingMapManagerContext context, ClassLoader loader, Map mapListeners)
        {
        XmlElement xmlFront  = resolveScheme(xmlOverflow.getSafeElement("front-scheme"), info, true, true);
        XmlElement xmlBack   = resolveScheme(xmlOverflow.getSafeElement("back-scheme"),  info, true, true);
        XmlElement xmlMisses = resolveScheme(xmlOverflow.getSafeElement("miss-cache-scheme"), info, true, false);
        Map        mapFront  = configureBackingMap(info, xmlFront, context, loader, mapListeners);
        Map        mapBack   = configureBackingMap(info, xmlBack,  context, loader, mapListeners);
        Map        mapMisses = xmlMisses == null ? null
                               : instantiateLocalCache(info, xmlMisses, context, loader);

        String  sSubclass         = xmlOverflow.getSafeElement("class-name").getString();
        boolean fExplicit         = sSubclass != null && sSubclass.length() > 0;
        int     cExpiryMillis     = (int) parseTime(xmlOverflow.getSafeElement("expiry-delay").getString("0"), UNIT_S);
        boolean fExpiry           = cExpiryMillis > 0
                                    || xmlOverflow.getSafeElement("expiry-enabled").getBoolean();
        boolean fObservable       = mapBack instanceof ObservableMap;
        boolean fExplicitOverflow = fExplicit && sSubclass.equals(OverflowMap.class.getName());
        boolean fExplicitSimple   = fExplicit && sSubclass.equals(SimpleOverflowMap.class.getName());

        Map mapOverflow;
        try
            {
            if (fExplicit && !fExplicitSimple && !fExplicitOverflow)
                {
                // figure out which type of overflow they are instantiating
                try
                    {
                    Class clz = ExternalizableHelper.loadClass(sSubclass, loader,
                            OverflowMap.class.getClassLoader());
                    if (OverflowMap.class.isAssignableFrom(clz))
                        {
                        fExplicitOverflow = true;
                        }
                    else if (SimpleOverflowMap.class.isAssignableFrom(clz))
                        {
                        fExplicitSimple = true;
                        }
                    else
                        {
                        throw new IllegalArgumentException(sSubclass
                            + " is not a sub-class of either OverflowMap or SimpleOverflowMap");
                        }
                    }
                catch (Exception e)
                    {
                    throw ensureRuntimeException(e);
                    }

                // prepare constructor parameters
                Object[] aoParam = fExplicitSimple && mapMisses != null
                        ? new Object[] {(ObservableMap) mapFront, mapBack, mapMisses}
                        : new Object[] {(ObservableMap) mapFront, mapBack};
                XmlElement xmlParams = xmlOverflow.getElement("init-params");

                // instantiate the overflow
                mapOverflow = (Map) instantiateSubclass(sSubclass,
                        OverflowMap.class, loader, aoParam, xmlParams);
                }
            else if (fExplicitSimple)
                {
                mapOverflow = instantiateSimpleOverflowMap((ObservableMap) mapFront, mapBack, mapMisses);
                }
            else
                {
                mapOverflow = instantiateOverflowMap((ObservableMap) mapFront, mapBack, fExpiry);
                }
            }
        catch (ClassCastException e)
            {
            throw new IllegalArgumentException("FrontMap is not observable: " + mapFront.getClass());
            }

        // post-instantiation configuration of options
        // and explanation of ignored options etc.
        if (mapOverflow instanceof OverflowMap)
            {
            if (cExpiryMillis > 0)
                {
                ((OverflowMap) mapOverflow).setExpiryDelay(cExpiryMillis);
                }

            if (mapMisses != null)
                {
                Logger.warn("Cache " + info.getCacheName()
                        + " of scheme " + info.getSchemeName()
                        + " has a \"miss-cache-scheme\" configured; since"
                        + " the default OverflowMap implementation has been"
                        + " selected, the miss cache will not be used.");
                }
            }
        else if (mapOverflow instanceof SimpleOverflowMap)
            {
            if (fExpiry)
                {
                Logger.warn("Cache " + info.getCacheName()
                        + " of scheme " + info.getSchemeName()
                        + " has \"expiry-enabled\" set to true or"
                        + " \"expiry-delay\" configured; these settings will"
                        + " have no effect, and expiry will not work,"
                        + " because the scheme explicitly ues a"
                        + " SimpleOverflowMap.");
                }

            if (fObservable)
                {
                Logger.warn("Cache " + info.getCacheName()
                        + " of scheme " + info.getSchemeName()
                        + " has a \"back-scheme\" that is observable;"
                        + " the events from the back map will be ignored"
                        + " because the scheme explicitly uses a"
                        + " SimpleOverflowMap, and this could result in"
                        + " missing events if the back map actively expires"
                        + " and/or evicts its entries.");
                }
            }

        return mapOverflow;
        }

    /**
    * Construct an OverflowMap using the specified parameters.
    * <p>
    * This method exposes a corresponding OverflowMap
    * {@link OverflowMap#OverflowMap(ObservableMap, Map) constructor}
    * and is provided for the express purpose of allowing its override.
    */
    protected OverflowMap instantiateOverflowMap(ObservableMap mapFront, Map mapBack, boolean fExpiry)
        {
        OverflowMap map = new OverflowMap(mapFront, mapBack);
        if (fExpiry)
            {
            map.setExpiryEnabled(true);
            }
        return map;
        }

    /**
    * Construct a SimpleOverflowMap using the specified parameters.
    * <p>
    * This method exposes a corresponding SimpleOverflowMap
    * {@link SimpleOverflowMap#SimpleOverflowMap(ObservableMap, Map, Map) constructor}
    * and is provided for the express purpose of allowing its override.
    */
    protected SimpleOverflowMap instantiateSimpleOverflowMap(ObservableMap mapFront, Map mapBack, Map mapMisses)
        {
        return new SimpleOverflowMap(mapFront, mapBack, mapMisses);
        }

    /**
    * Create a backing Map using the "disk-scheme" element.
    *
    * @param info     the cache info
    * @param xmlDisk  "disk-scheme" element
    * @param context  BackingMapManagerContext to be used
    * @param loader   the ClassLoader to instantiate necessary classes
    *
    * @return a newly instantiated Map
    *
    * @deprecated As of Coherence 3.0, replaced by
    *             {@link #instantiateExternalBackingMap} and
    *             {@link #instantiatePagedExternalBackingMap}
    */
    protected Map instantiateDiskBackingMap(CacheInfo info, XmlElement xmlDisk,
            BackingMapManagerContext context, ClassLoader loader)
        {
        String  sSubclass     = xmlDisk.getSafeElement("class-name").getString();
        String  sFS           = xmlDisk.getSafeElement("file-manager").getString();
        String  sPath         = xmlDisk.getSafeElement("directory").getString();
        int     cHighUnits    = convertInt(xmlDisk.getSafeElement("high-units"));
        int     cPages        = convertInt(xmlDisk.getSafeElement("page-limit"));
        String  sTarget       = xmlDisk.getSafeAttribute("target").getString(); // see $Storage.instantiateBackupMap()
        boolean fAsync        = xmlDisk.getSafeElement("async").getBoolean();
        int     cbMaxAsync    = (int) (parseMemorySize(xmlDisk.getSafeElement("async-limit").getString("0")));
        int     cPageSecs     = (int) (parseTime(xmlDisk.getSafeElement("page-duration").getString("0"), UNIT_S) / 1000L);
        int     cExpiryMillis = (int) parseTime(xmlDisk.getSafeElement("expiry-delay").getString("0"), UNIT_S);

        if (sPath.length() == 0)
            {
            // deprecated but supported
            sPath = xmlDisk.getSafeElement("root-directory").getString();
            }

        File    file       = sPath.length() == 0 ? null : new File(sPath);
        boolean fPaged     = cPages > 0 && cPageSecs > 0;
        boolean fBackup    = sTarget.equals("backup");
        boolean fBinaryMap = context != null && CacheService.TYPE_DISTRIBUTED.
                                equals(context.getCacheService().getInfo().getServiceType());

        BinaryStore        store    = null;
        BinaryStoreManager storeMgr = null;

        if (sFS.equalsIgnoreCase("NIO-file"))
            {
            long cbInit = parseMemorySize(xmlDisk.getSafeElement("initial-size").getString("1"), POWER_M);
            long cbMax  = parseMemorySize(xmlDisk.getSafeElement("maximum-size").getString("1024"), POWER_M);

            // Bounds check:
            // 1 <= cbInitSize <= cbMaxSize <= Integer.MAX_VALUE - 1023
            // (Integer.MAX_VALUE - 1023 is the largest integer multiple of 1024)
            int cbMaxSize  = (int) Math.min(Math.max(cbMax, 1L), (long) Integer.MAX_VALUE - 1023);
            int cbInitSize = (int) Math.min(Math.max(cbInit, 1L), cbMaxSize);

            if (fPaged)
                {
                storeMgr = new MappedStoreManager(cbInitSize, cbMaxSize, file);
                }
            else
                {
                ByteBufferManager bufferMgr = new MappedBufferManager(cbInitSize, cbMaxSize, file);

                store = new BinaryMapStore(new BinaryMap(bufferMgr));
                }
            }
        else
            {
            throw new UnsupportedOperationException("file-manager: " + sFS);
            }

        if (fAsync)
            {
            if (store != null)
                {
                store = instantiateAsyncBinaryStore(store, cbMaxAsync);
                }
            else if (storeMgr != null)
                {
                storeMgr = instantiateAsyncBinaryStoreManager(storeMgr, cbMaxAsync);
                }
            else
                {
                throw new UnsupportedOperationException("async option without BinaryStore or BinaryStoreManager!");
                }
            }

        if (fPaged)
            {
            if (sSubclass.length() == 0)
                {
                return fBinaryMap ?
                    instantiateSerializationPagedCache(storeMgr, cPages, cPageSecs, true, fBackup) :
                    instantiateSerializationPagedCache(storeMgr, cPages, cPageSecs, loader);
                }
            else
                {
                Object[] aoParam = fBinaryMap ?
                    new Object[] {storeMgr, cPages, cPageSecs, Boolean.TRUE, fBackup} :
                    new Object[] {storeMgr, cPages, cPageSecs, loader};

                return (Map) instantiateSubclass(sSubclass, SerializationPagedCache.class, loader,
                    aoParam, xmlDisk.getElement("init-params"));
                }
            }
        else
            {
            return instantiateSerializationMap(store, fBinaryMap, loader,
                    cHighUnits, cExpiryMillis, sSubclass, xmlDisk.getElement("init-params"));
            }
        }

    /**
    * Instantiate a SerializationMap, SerializationCache,
    * SimpleSerializationMap, or any sub-class thereof.
    *
    * @param store          a BinaryStore to use to write serialized data to
    * @param fBinaryMap     true if the only data written to the Map will
    *                       already be in Binary form
    * @param loader         the ClassLoader to use (if not a Binary map)
    * @param cHighUnits     the max size in units for the serialization cache
    * @param cExpiryMillis  the expiry time in milliseconds for the cache
    * @param sSubclass      the sub-class name (or "")
    * @param xmlInitParams  the init params for the sub-class
    *
    * @return a BinaryMap, SerializationMap, SerializationCache,
    *         SimpleSerializationMap, or a subclass thereof
    */
    protected Map instantiateSerializationMap(BinaryStore store,
            boolean fBinaryMap, ClassLoader loader,
            int cHighUnits, int cExpiryMillis,
            String sSubclass, XmlElement xmlInitParams)
        {
        if (sSubclass.length() == 0)
            {
            if (cHighUnits > 0 || cExpiryMillis > 0)
                {
                SerializationCache cache = fBinaryMap
                        ? instantiateSerializationCache(store, cHighUnits, true)
                        : instantiateSerializationCache(store, cHighUnits, loader);
                if (cExpiryMillis > 0)
                    {
                    cache.setExpiryDelay(cExpiryMillis);
                    }
                return cache;
                }
            else if (fBinaryMap && store.getClass() == BinaryMapStore.class)
                {
                // optimization: instead of taking binary objects, writing
                // them through a serialization map that knows that they are
                // binary into a BinaryStore that wraps a BinaryMap, we just
                // use the BinaryMap directly
                return ((BinaryMapStore) store).getBinaryMap();
                }
            else
                {
                return fBinaryMap
                        ? instantiateSerializationMap(store, true)
                        : instantiateSerializationMap(store, loader);
                }
            }
        else
            {
            if (cHighUnits > 0 || cExpiryMillis > 0)
                {
                Object[] aoParam = fBinaryMap
                        ? new Object[] {store, cHighUnits, Boolean.TRUE}
                        : new Object[] {store, cHighUnits, loader};
                SerializationCache cache = (SerializationCache) instantiateSubclass(sSubclass,
                        SerializationCache.class, loader, aoParam, xmlInitParams);
                if (cExpiryMillis > 0)
                    {
                    cache.setExpiryDelay(cExpiryMillis);
                    }
                return cache;
                }
            else
                {
                Object[] aoParam = fBinaryMap
                        ? new Object[] {store, Boolean.TRUE}
                        : new Object[] {store, loader};

                // the custom class may subclass one of the following:
                //
                //   (1) SerializationMap
                //   (2) SimpleSerializationMap
                //
                // the common ancestor of these classes is AbstractKeyBasedMap
                Map map = (Map) instantiateSubclass(sSubclass,
                        AbstractKeyBasedMap.class, loader, aoParam,
                        xmlInitParams);

                if (map instanceof SerializationMap ||
                    map instanceof SimpleSerializationMap)
                    {
                    return map;
                    }

                throw new IllegalArgumentException(sSubclass
                        + " does not extend either "
                        + SerializationMap.class.getName()
                        + " or "
                        + SimpleSerializationMap.class.getName());
                }
            }
        }

    /**
    * Create a backing Map using the "external-scheme" element.
    *
    * @param info        the cache info
    * @param xmlExternal "external-scheme" element
    * @param context     BackingMapManagerContext to be used
    * @param loader      the ClassLoader to instantiate necessary classes
    *
    * @return a newly instantiated Map
    */
    protected Map instantiateExternalBackingMap(CacheInfo info,
            XmlElement xmlExternal, BackingMapManagerContext context,
            ClassLoader loader)
        {
        String      sSubclass     = xmlExternal.getSafeElement("class-name").getString();
        int         cHighUnits    = (int) parseMemorySize(xmlExternal.getSafeElement("high-units" ).getString("0"));
        int         cExpiryMillis = (int) parseTime(xmlExternal.getSafeElement("expiry-delay").getString("0"), UNIT_S);
        boolean     fBinaryMap    = context != null && CacheService.TYPE_DISTRIBUTED.equals(context.getCacheService().getInfo().getServiceType());
        BinaryStore store         = instantiateBinaryStoreManager(xmlExternal, loader, false).createBinaryStore();

        Map map = instantiateSerializationMap(store, fBinaryMap, loader,
                cHighUnits, cExpiryMillis, sSubclass, xmlExternal.getElement("init-params"));
        if (map instanceof ConfigurableCacheMap)
            {
            configureUnitCalculator(
                xmlExternal, (ConfigurableCacheMap) map, info, context, loader);
            }
        return map;
        }

    /**
    * Create a backing Map using the "paged-external-scheme" element.
    *
    * @param info      the cache info
    * @param xmlPaged  "paged-external-scheme" element
    * @param context   BackingMapManagerContext to be used
    * @param loader    the ClassLoader to instantiate necessary classes
    *
    * @return a newly instantiated Map
    */
    protected Map instantiatePagedExternalBackingMap(CacheInfo info,
            XmlElement xmlPaged, BackingMapManagerContext context,
            ClassLoader loader)
        {
        String  sSubclass      = xmlPaged.getSafeElement("class-name").getString();
        int     cPages         = convertInt(xmlPaged.getSafeElement("page-limit"));
        int     cPageSecs      = (int) (parseTime(xmlPaged.getSafeElement("page-duration").getString("5"), UNIT_S) / 1000L);
        boolean fBackup        = xmlPaged.getSafeAttribute("target").getString().equals("backup");
        boolean fBinaryMap     = context != null && CacheService.TYPE_DISTRIBUTED.equals(context.getCacheService().getInfo().getServiceType());
        BinaryStoreManager mgr = instantiateBinaryStoreManager(xmlPaged, loader, true);

        if (sSubclass.length() == 0)
            {
            return fBinaryMap ?
                instantiateSerializationPagedCache(mgr, cPages, cPageSecs, true, fBackup) :
                instantiateSerializationPagedCache(mgr, cPages, cPageSecs, loader);
            }
        else
            {
            Object[] aoParam = fBinaryMap ?
                new Object[] {mgr, cPages, cPageSecs, Boolean.TRUE, fBackup} :
                new Object[] {mgr, cPages, cPageSecs, loader};

            return (Map) instantiateSubclass(sSubclass, SerializationPagedCache.class, loader,
                aoParam, xmlPaged.getElement("init-params"));
            }
        }


    /**
     * Create a backing Map using the "flashjournal-scheme" element.
     *
     * @param info        the cache info
     * @param xmlJournal "flashjournal-scheme" element
     * @param context     BackingMapManagerContext to be used
     * @param loader      the ClassLoader to instantiate necessary classes
     *
     * @return a newly instantiated Map
     */
    protected Map instantiateFlashJournalBackingMap(CacheInfo info,
            XmlElement xmlJournal, BackingMapManagerContext context,
            ClassLoader loader)
        {
        throw new UnsupportedOperationException("Elastic Data features are not supported in Coherence CE");
        }

    /**
    * Create a backing Map using the "ramjournal-scheme" element.
    *
    * @param info        the cache info
    * @param xmlJournal "ramjournal-scheme" element
    * @param context     BackingMapManagerContext to be used
    * @param loader      the ClassLoader to instantiate necessary classes
    *
    * @return a newly instantiated Map
    */
    protected Map instantiateRamJournalBackingMap(CacheInfo info,
            XmlElement xmlJournal, BackingMapManagerContext context,
            ClassLoader loader)
        {
        throw new UnsupportedOperationException("Elastic Data features are not supported in Coherence CE");
        }

    /**
    * Create a BinaryStoreManager using the specified XML configuration. The
    * given XML configuration must contain a valid child BinaryStoreManager
    * element:
    * <ul>
    * <li>async-store-manager</li>
    * <li>custom-store-manager</li>
    * <li>bdb-store-manager</li>
    * <li>nio-file-manager</li>
    * </ul>
    *
    * @param xmlConfig the XmlElement that contains the configuration info for
    *                  the BinaryStoreManager that will be instantiated
    * @param loader    the ClassLoader to instantiate necessary classes
    * @param fPaged    flag indicating whether or not the returned
    *                  BinaryStoreManager will be used by a
    *                  SerializationPagedCache
    *
    * @return a newly instantiated BinaryStoreManager created using the given
    *         XML configuration
    */
    protected BinaryStoreManager instantiateBinaryStoreManager(XmlElement xmlConfig,
            ClassLoader loader, boolean fPaged)
        {
        for (Iterator iter = xmlConfig.getElementList().iterator(); iter.hasNext();)
            {
            XmlElement xmlStore = (XmlElement) iter.next();
            String     sType    = xmlStore.getName();

            // parse common configuration elements
            String     sSubclass  = xmlStore.getSafeElement("class-name").getString();
            XmlElement xmlParams  = xmlStore.getElement("init-params");
            File       fileDir    = null;
            int        cbMaxSize  = 0;
            int        cbInitSize = 0;

            if (sType.equals("nio-file-manager") ||
                sType.equals("bdb-store-manager"))
                {
                String sPath = xmlStore.getSafeElement("directory").getString();
                fileDir      = sPath.length() == 0 ? null : new File(sPath);
                }
            if (sType.equals("nio-file-manager"))
                {
                long cbInit = parseMemorySize(xmlStore.getSafeElement("initial-size").getString("1"), POWER_M);
                long cbMax  = parseMemorySize(xmlStore.getSafeElement("maximum-size").getString("1024"), POWER_M);

                // bounds check:
                // 1 <= cbInitSize <= cbMaxSize <= Integer.MAX_VALUE - 1023
                // (Integer.MAX_VALUE - 1023 is the largest integer multiple of 1024)
                cbMaxSize  = (int) Math.min(Math.max(cbMax, 1L), (long) Integer.MAX_VALUE - 1023);
                cbInitSize = (int) Math.min(Math.max(cbInit, 1L), cbMaxSize);

                // warn about changes to configured values
                if (cbInitSize != cbInit)
                    {
                    Logger.warn("Invalid initial-size specified for " +
                            sType + "; changed to: " + cbInitSize + " bytes");
                    }
                if (cbMaxSize != cbMax)
                    {
                    Logger.warn("Invalid maximum-size specified for " +
                            sType + "; changed to: " + cbMaxSize + " bytes");
                    }
                }

            // bdb-store
            if (sType.equals("bdb-store-manager"))
                {
                String sStoreName = xmlStore.getSafeElement("store-name").getString();

                try
                    {
                    if (sSubclass.length() == 0)
                        {
                        BerkeleyDBBinaryStoreManager bdbMgr =
                                new BerkeleyDBBinaryStoreManager(fileDir, sStoreName);
                        if (xmlParams != null)
                            {
                            XmlElement xmlInit = new SimpleElement("config");
                            XmlHelper.transformInitParams(xmlInit, xmlParams);
                            bdbMgr.setConfig(xmlInit);
                            }
                        return bdbMgr;
                        }
                    else
                        {
                        Object[] aoParam = new Object[] {fileDir, sStoreName};
                        return (BinaryStoreManager) instantiateSubclass(sSubclass,
                                BerkeleyDBBinaryStoreManager.class, loader, aoParam,
                                xmlParams);
                        }
                    }
                catch (NoClassDefFoundError e)
                    {
                    String sMsg = "Berkeley DB JE libraries are required to utilize a 'bdb-store-manager'," +
                                  " visit www.sleepycat.com for additional information.";
                    throw ensureRuntimeException(e, sMsg);
                    }
                }

            // nio-file
            if (sType.equals("nio-file-manager"))
                {
                if (sSubclass.length() == 0)
                    {
                    return new MappedStoreManager(cbInitSize, cbMaxSize, fileDir);
                    }
                else
                    {
                    Object[] aoParam = new Object[] {cbInitSize, cbMaxSize, fileDir};
                    return (BinaryStoreManager) instantiateSubclass(sSubclass,
                            MappedStoreManager.class, loader, aoParam, xmlParams);
                    }
                }

            // async-store
            if (sType.equals("async-store-manager"))
                {
                int cbMaxAsync = (int) (parseMemorySize(xmlStore.getSafeElement("async-limit")
                                                                .getString("0")));
                BinaryStoreManager mgr = instantiateBinaryStoreManager(xmlStore, loader, fPaged);

                if (sSubclass.length() == 0)
                    {
                    return instantiateAsyncBinaryStoreManager(mgr, cbMaxAsync);
                    }
                else
                    {
                    Object[] aoParam = cbMaxAsync <= 0 ? new Object[] {mgr}
                                                       : new Object[] {mgr, cbMaxAsync};
                    return (BinaryStoreManager) instantiateSubclass(sSubclass,
                            AsyncBinaryStoreManager.class, loader, aoParam, xmlParams);
                    }
                }

            // custom-store
            if (sType.equals("custom-store-manager"))
                {
                if (sSubclass.length() == 0)
                    {
                    throw new IllegalArgumentException("Missing class-name:\n" +
                            xmlStore);
                    }

                return (BinaryStoreManager) instantiateSubclass(sSubclass,
                        BinaryStoreManager.class, loader, null, xmlParams);
                }
            }

        throw new IllegalArgumentException(
                "Missing BinaryStoreManager configuration:\n" + xmlConfig);
        }

    /**
    * Construct an AsyncBinaryStore using the specified parameters.
    *
    * @param store       the BinaryStore to make asynchronous
    * @param cbMaxAsync  the maximum amount of "async writes" data that will
    *                    be queued
    *
    * @return a new AsyncBinaryStore wrapping the passed BinaryStore
    */
    protected AsyncBinaryStore instantiateAsyncBinaryStore(BinaryStore store, int cbMaxAsync)
        {
        return cbMaxAsync <= 0 ? new AsyncBinaryStore(store)
                               : new AsyncBinaryStore(store, cbMaxAsync);
        }

    /**
    * Construct an AsyncBinaryStoreManager using the specified parameters.
    *
    * @param storeMgr    the BinaryStoreManager to make asynchronous
    * @param cbMaxAsync  the maximum amount of "async writes" data that will
    *                    be queued
    *
    * @return a new AsyncBinaryStoreManager wrapping the passed BinaryStoreManager
    */
    protected AsyncBinaryStoreManager instantiateAsyncBinaryStoreManager(BinaryStoreManager storeMgr, int cbMaxAsync)
        {
        return cbMaxAsync <= 0 ? new AsyncBinaryStoreManager(storeMgr)
                               : new AsyncBinaryStoreManager(storeMgr, cbMaxAsync);
        }

    /**
    * Construct an SerializationPagedCache using the specified parameters.
    * <p>
    * This method exposes a corresponding SerializationPagedCache
    * {@link SerializationPagedCache#SerializationPagedCache(BinaryStoreManager, int, int, ClassLoader) constructor}
    * and is provided for the express purpose of allowing its override.
    */
    protected SerializationPagedCache instantiateSerializationPagedCache(BinaryStoreManager storeMgr,
            int cPages, int cPageSecs, ClassLoader loader)
        {
        return new SerializationPagedCache(storeMgr, cPages, cPageSecs, loader);
        }

    /**
    * Construct an SerializationPagedCache using the specified parameters.
    * <p>
    * This method exposes a corresponding SerializationPagedCache
    * {@link SerializationPagedCache#SerializationPagedCache(BinaryStoreManager, int, int, boolean, boolean) constructor}
    * and is provided for the express purpose of allowing its override.
    */
    protected SerializationPagedCache instantiateSerializationPagedCache(BinaryStoreManager storeMgr,
            int cPages, int cPageSecs, boolean fBinaryMap, boolean fPassive)
        {
        return new SerializationPagedCache(storeMgr, cPages, cPageSecs, fBinaryMap, fPassive);
        }

    /**
    * Construct an SerializationCache using the specified parameters.
    * <p>
    * This method exposes a corresponding SerializationCache
    * {@link SerializationCache#SerializationCache(BinaryStore, int, ClassLoader) constructor}
    * and is provided for the express purpose of allowing its override.
    */
    protected SerializationCache instantiateSerializationCache(BinaryStore store, int cMax, ClassLoader loader)
        {
        return new SerializationCache(store, cMax, loader);
        }

    /**
    * Construct an SerializationCache using the specified parameters.
    * <p>
    * This method exposes a corresponding SerializationCache
    * {@link SerializationCache#SerializationCache(BinaryStore, int, boolean) constructor}
    * and is provided for the express purpose of allowing its override.
    */
    protected SerializationCache instantiateSerializationCache(BinaryStore store, int cMax, boolean fBinaryMap)
        {
        return new SerializationCache(store, cMax, fBinaryMap);
        }

    /**
    * Construct an SerializationMap using the specified parameters.
    * <p>
    * This method exposes a corresponding SerializationMap
    * {@link SerializationMap#SerializationMap(BinaryStore, ClassLoader) constructor}
    * and is provided for the express purpose of allowing its override.
    */
    protected SerializationMap instantiateSerializationMap(BinaryStore store, ClassLoader loader)
        {
        return new SerializationMap(store, loader);
        }

    /**
    * Construct an SerializationMap using the specified parameters.
    * <p>
    * This method exposes a corresponding SerializationMap
    * {@link SerializationMap#SerializationMap(BinaryStore, boolean) constructor}
    * and is provided for the express purpose of allowing its override.
    */
    protected SerializationMap instantiateSerializationMap(BinaryStore store, boolean fBinaryMap)
        {
        return new SerializationMap(store, fBinaryMap);
        }

    /**
    * Construct a SimpleSerializationMap using the specified parameters.
    * <p>
    * This method exposes a corresponding SerializationMap {@link
    * SimpleSerializationMap#SimpleSerializationMap(BinaryStore, ClassLoader)
    * constructor} and is provided for the express purpose of allowing its
    * override.
    *
    * @since Coherence 3.7
    */
    protected SimpleSerializationMap instantiateSimpleSerializationMap(
            BinaryStore store, ClassLoader loader)
        {
        return new SimpleSerializationMap(store, loader);
        }

    /**
    * Construct a SimpleSerializationMap using the specified parameters.
    * <p>
    * This method exposes a corresponding SerializationMap {@link
    * SimpleSerializationMap#SimpleSerializationMap(BinaryStore, boolean)
    * constructor} and is provided for the express purpose of allowing its
    * override.
    *
    * @since Coherence 3.7
    */
    protected SimpleSerializationMap instantiateSimpleSerializationMap(
            BinaryStore store, boolean fBinaryMap)
        {
        return new SimpleSerializationMap(store, fBinaryMap);
        }

    /**
    * Create a BundlingNamedCache using the "operation-bundling" element.
    *
    * @param cache        the wrapped cache
    * @param xmlBundling  the "operation-bundling" element
    *
    * @return a newly instantiated BundlingNamedCache
    */
    protected BundlingNamedCache instantiateBundlingNamedCache(NamedCache cache,
            XmlElement xmlBundling)
        {
        BundlingNamedCache cacheBundle = new BundlingNamedCache(cache);
        for (Iterator iter = xmlBundling.getElements("bundle-config");
                iter.hasNext();)
            {
            XmlElement xmlBundle = (XmlElement) iter.next();

            String sOperation = xmlBundle.getSafeElement("operation-name").getString("all");
            int    cBundle    = convertInt(xmlBundle.getSafeElement("preferred-size"));

            if (sOperation.equals("all"))
                {
                initializeBundler(cacheBundle.ensureGetBundler(cBundle), xmlBundle);
                initializeBundler(cacheBundle.ensurePutBundler(cBundle), xmlBundle);
                initializeBundler(cacheBundle.ensureRemoveBundler(cBundle), xmlBundle);
                }
            else if (sOperation.equals("get"))
                {
                initializeBundler(cacheBundle.ensureGetBundler(cBundle), xmlBundle);
                }
            else if (sOperation.equals("put"))
                {
                initializeBundler(cacheBundle.ensurePutBundler(cBundle), xmlBundle);
                }
            else if (sOperation.equals("remove"))
                {
                initializeBundler(cacheBundle.ensureRemoveBundler(cBundle), xmlBundle);
                }
            else
                {
                throw new IllegalArgumentException(
                    "Invalid \"operation-name\" element:\n" + xmlBundle);
                }
            }

        return cacheBundle;
        }

    /**
    * Initialize the specified bundler using the "bundle-config" element.
    *
    * @param bundler    the bundler
    * @param xmlBundle  a "bundle-config" element
    */
    protected void initializeBundler(AbstractBundler bundler, XmlElement xmlBundle)
        {
        if (bundler != null)
            {
            bundler.setThreadThreshold(
                convertInt(xmlBundle.getSafeElement("thread-threshold"), 4));
            bundler.setDelayMillis(
                convertInt(xmlBundle.getSafeElement("delay-millis"), 1));
            bundler.setAllowAutoAdjust(
                xmlBundle.getSafeElement("auto-adjust").getBoolean(false));
            }
        }

    /**
    * Create a backing Map using the "class-scheme" element.
    * This method is a thin wrapper around
    * {@link #instantiateAny instantiateAny}.
    *
    * @param info      the cache info
    * @param xmlClass  "class-scheme" element
    * @param context   BackingMapManagerContext to be used
    * @param loader    the ClassLoader to instantiate necessary classes
    *
    * @return a newly instantiated Map
    */
    protected Map instantiateMap(CacheInfo info, XmlElement xmlClass,
            BackingMapManagerContext context, ClassLoader loader)
        {
        try
            {
            return (Map) instantiateAny(info, xmlClass, context, loader);
            }
        catch (ClassCastException e)
            {
            throw new IllegalArgumentException("Not a map:\n" + xmlClass);
            }
        }

    /**
    * Create a CacheLoader, CacheStore or BinaryEntryStore using the
    * "cachestore-scheme" element.
    *
    * @param info      the cache info
    * @param xmlStore  "cachestore-scheme" element for the store or loader
    * @param context   BackingMapManagerContext to be used
    * @param loader    the ClassLoader to instantiate necessary classes
    *
    * @return a newly instantiated CacheLoader, CacheStore or BinaryEntryStore
    */
    protected Object instantiateCacheStore(CacheInfo info, XmlElement xmlStore,
            BackingMapManagerContext context, ClassLoader loader)
        {
        xmlStore = resolveScheme(xmlStore, info, true, false);
        if (xmlStore == null || XmlHelper.isEmpty(xmlStore))
            {
            return null;
            }

        String sSchemeType = xmlStore.getName();
        try
            {
            switch (translateSchemeType(sSchemeType))
                {
                case SCHEME_CLASS:
                    return instantiateAny(info, xmlStore, context, loader);

                case SCHEME_REMOTE_CACHE:
                    {
                    NamedCache cacheRemote = configureCache(info, xmlStore,
                        NullImplementation.getClassLoader());
                    if (!isSerializerCompatible(
                            cacheRemote.getCacheService(), context.getCacheService()))
                        {
                        Service service = context.getCacheService();
                        ExternalizableHelper.reportIncompatibleSerializers(cacheRemote,
                            service.getInfo().getServiceName(), service.getSerializer());
                        cacheRemote.release();
                        cacheRemote = configureCache(info, xmlStore, loader);
                        }
                    return cacheRemote;
                    }
                default:
                    throw new UnsupportedOperationException(
                        "instantiateCacheStore: " + sSchemeType);
                }
            }
        catch (ClassCastException e)
            {
            throw new IllegalArgumentException(
                "Not a CacheLoader:\n" + xmlStore);
            }
        }

    /**
    * Create a MapListener using the using the "class-scheme" element.
    * If the value of any "param-value" element contains the literal
    * "{cache-name}", replace it with the actual cache name.
    *
    * @param info      the cache info
    * @param xmlClass  "class-scheme" element
    * @param context   BackingMapManagerContext to be used
    * @param loader    the ClassLoader to instantiate necessary classes
    *
    * @return a newly instantiated MapListener
    */
    protected MapListener instantiateMapListener(CacheInfo info, XmlElement xmlClass,
            BackingMapManagerContext context, ClassLoader loader)
        {
        try
            {
            return (MapListener) instantiateAny(info, xmlClass, context, loader);
            }
        catch (ClassCastException e)
            {
            throw new IllegalArgumentException("Not a listener:\n" + xmlClass);
            }
        }

    /**
    * Create an Object using "class-scheme" element.
    * <p>
    * If the value of any "param-value" element contains the literal
    * "{cache-name}", replace it with the actual cache name.<br>
    * If the value of "param-value" element is "{class-loader}"
    * and "param-type" element is "java.lang.ClassLoader" replace it
    * with the current ClassLoader object.<br>
    * If the value of "param-value" element is "{manager-context}"
    * and "param-type" element is "com.tangosol.net.BackingMapManagerContext"
    * replace it with the current BackingMapManagerContext object.<br>
    * Finally, if the value of "param-type" is "{scheme-ref}" then the
    * "param-value" should be a name of the scheme that will be used in place
    * of the value.
    *
    * @param info      the cache info
    * @param xmlClass  "class-scheme" element
    * @param context   BackingMapManagerContext to be used
    * @param loader    the ClassLoader to instantiate necessary classes
    *
    * @return a newly instantiated Object
    */
    public Object instantiateAny(final CacheInfo info, XmlElement xmlClass,
            final BackingMapManagerContext context, final ClassLoader loader)
        {
        if (translateSchemeType(xmlClass.getName()) != SCHEME_CLASS)
            {
            throw new IllegalArgumentException("Invalid class definition: " + xmlClass);
            }

        XmlHelper.ParameterResolver resolver = new XmlHelper.ParameterResolver()
            {
            public Object resolveParameter(String sType, String sValue)
                {
                if (sValue.equals(CLASS_LOADER))
                    {
                    // sType extends "java.lang.ClassLoader"
                    return loader;
                    }
                if (sValue.equals(MGR_CONTEXT))
                    {
                    // sType implements "com.tangosol.net.BackingMapManagerContext"
                    return context;
                    }
                if (sType.equals(SCHEME_REF))
                    {
                    // sValue is the scheme name
                    XmlElement xmlScheme   = resolveScheme(new CacheInfo(
                            info.getCacheName(), sValue, info.getAttributes()));
                    String     sSchemeType = xmlScheme.getName();
                    switch (translateSchemeType(sSchemeType))
                        {
                        case SCHEME_REPLICATED:
                        case SCHEME_OPTIMISTIC:
                        case SCHEME_DISTRIBUTED:
                        case SCHEME_NEAR:
                        case SCHEME_VERSIONED_NEAR:
                        case SCHEME_REMOTE_CACHE:
                            return configureCache(info, xmlScheme, loader);

                        case SCHEME_INVOCATION:
                        case SCHEME_REMOTE_INVOCATION:
                            return ensureServiceInternal(xmlScheme);

                        case SCHEME_LOCAL:
                        case SCHEME_OVERFLOW:
                        case SCHEME_DISK:
                        case SCHEME_EXTERNAL:
                        case SCHEME_EXTERNAL_PAGED:
                        case SCHEME_READ_WRITE_BACKING:
                        case SCHEME_VERSIONED_BACKING:
                        case SCHEME_FLASHJOURNAL:
                        case SCHEME_RAMJOURNAL:
                            {
                            Map mapListeners = context == null ? null :
                                ((Manager) context.getManager()).m_mapBackingMapListeners;
                            return configureBackingMap(
                                info, xmlScheme, context, null, mapListeners);
                            }
                        case SCHEME_CLASS:
                            return instantiateAny(info, xmlScheme, context, loader);

                        default:
                            throw new UnsupportedOperationException(
                                "instantiateAny: " + sSchemeType);
                        }
                    }

                if (sType.equals(CACHE_REF))
                    {
                    // sValue is the referenced cache name
                    return ensureCache(sValue, loader);
                    }

                return XmlHelper.ParameterResolver.UNRESOLVED;
                }
            };

        return XmlHelper.createInstance(xmlClass, loader, resolver);
        }

    /**
    * Construct an instance of the specified class using the specified
    * parameters.
    *
    * @param sClass     the class name
    * @param clzSuper   the super class of the newly instantiated class
    * @param loader     the ClassLoader to instantiate necessary classes
    * @param aoParam    the constructor parameters
    * @param xmlParams  the "init-params" XmlElement (optional)
    *
    * @return a newly instantiated Object
    */
    public Object instantiateSubclass(String sClass, Class clzSuper, ClassLoader loader,
                                         Object[] aoParam, XmlElement xmlParams)
        {
        if (sClass == null || sClass.length() == 0 || clzSuper == null )
            {
            throw new IllegalArgumentException(
                "Class name and super class must be specified");
            }

        try
            {
            Class clz = ExternalizableHelper.loadClass(sClass, loader,
                            clzSuper.getClassLoader());
            if (!clzSuper.isAssignableFrom(clz))
                {
                throw new IllegalArgumentException(
                    clzSuper + " is not a super-class of " + clz);
                }

            Object oTarget;
            if (aoParam == null)
                {
                oTarget = clz.newInstance();
                }
            else
                {
                oTarget = ClassHelper.newInstance(clz, aoParam);
                }

            if (xmlParams != null && oTarget instanceof XmlConfigurable)
                {
                XmlElement xmlConfig = new SimpleElement("config");
                XmlHelper.transformInitParams(xmlConfig, xmlParams);

                ((XmlConfigurable) oTarget).setConfig(xmlConfig);
                }
            return oTarget;
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e,
                "Fail to instantiate subclass: " + sClass + " of " + clzSuper);
            }
        }

    /**
    * Release all resources associated with the specified backing map.
    *
    * @param map           the map being released
    * @param mapListeners  map of registered map listeners keyed by the
    *                      corresponding map references
    */
    public void release(Map map, Map mapListeners)
        {
        // remove known map listener
        if (map instanceof ObservableMap && mapListeners != null)
            {
            MapListener listener = (MapListener) mapListeners.get(map);
            if (listener != null)
                {
                ((ObservableMap) map).removeMapListener(listener);
                mapListeners.remove(map);
                }
            }

        // process recursively
        if (map instanceof LocalCache)
            {
            CacheLoader loader = ((LocalCache) map).getCacheLoader();

            if (loader instanceof MapCacheStore)
                {
                release(((MapCacheStore) loader).getMap(), mapListeners);
                }
            else
                {
                release(loader);
                }
            }
        else if (map instanceof OverflowMap)
            {
            release(((OverflowMap) map).getFrontMap(), mapListeners);
            release(((OverflowMap) map).getBackMap() , mapListeners);
            }
        else if (map instanceof ReadWriteBackingMap)
            {
            ((ReadWriteBackingMap) map).release();
            release(((ReadWriteBackingMap) map).getInternalCache(), mapListeners);
            }
        else if (map instanceof SerializationMap)
            {
            release(((SerializationMap) map).getBinaryStore());
            }
        else if (map instanceof SimpleSerializationMap)
            {
            release (((SimpleSerializationMap) map).getBinaryStore());
            }
        else if (map instanceof BinaryMap)
            {
            ByteBufferManager bufmgr = ((BinaryMap) map).getBufferManager();
            if (bufmgr instanceof MappedBufferManager)
                {
                ((MappedBufferManager) bufmgr).close();
                }
            }

        // regardless of the above, the map may be disposable as well
        if (map instanceof Disposable)
            {
            ((Disposable) map).dispose();
            }
        }

    /**
    * Release all resources associated with the specified loader.
    *
    * @param loader  the cache loader being released
    */
    protected void release(CacheLoader loader)
        {
        if (loader instanceof Disposable)
            {
            ((Disposable) loader).dispose();
            }
        else
            {
            try
                {
                ClassHelper.invoke(loader, "close", ClassHelper.VOID);
                }
            catch (Exception e) {}
            }
        }

    /**
    * Release all resources associated with the specified binary store.
    *
    * @param store  the binary store being released
    */
    protected void release(BinaryStore store)
        {
        if (store instanceof Disposable)
            {
            ((Disposable) store).dispose();
            }
        else
            {
            try
                {
                ClassHelper.invoke(store, "close", ClassHelper.VOID);
                }
            catch (Exception e) {}
            }
        }

    /**
    * Release a cache managed by this factory, optionally destroying it.
    *
    * @param cache     the cache to release
    * @param fDestroy  true to destroy the cache as well
    */
    protected void releaseCache(NamedCache cache, boolean fDestroy)
        {
        ScopedCacheReferenceStore store      = m_store;
        String                    sCacheName = cache.getCacheName();
        ClassLoader               loader     = cache instanceof ClassLoaderAware
                ? ((ClassLoaderAware) cache).getContextClassLoader()
                : getContextClassLoader();

        Runnable  runRelease =  () ->
            {
            // allow cache to release/destroy internal resources
            if (fDestroy)
                {
                cache.destroy();
                }
            else
                {
                cache.release();
                }
            };

        if (store.releaseCache(cache, loader, runRelease))
            {
            // nothing to do
            }
        else if (cache.isActive())
            {
            // active, but not managed by this factory
            throw new IllegalArgumentException("The cache " + sCacheName +
                    " was created using a different factory; that same" +
                    " factory should be used to release the cache.");
            }
        }

    /**
    * Translate the scheme name into the scheme type. Valid scheme types are
    * any of the SCHEME_* constants.
    *
    * @param sScheme  the scheme name
    *
    * @return the scheme type
    */
    public int translateSchemeType(String sScheme)
        {
        return translateStandardSchemeType(sScheme);
        }

    /**
    * Translate the scheme name into the scheme type. Valid scheme types are
    * any of the SCHEME_* constants.
    *
    * @param sScheme  the scheme name
    *
    * @return the scheme type
    */
    public static int translateStandardSchemeType(String sScheme)
        {
        return sScheme.equals("replicated-scheme")              ? SCHEME_REPLICATED
             : sScheme.equals("optimistic-scheme")              ? SCHEME_OPTIMISTIC
             : sScheme.equals("distributed-scheme")             ? SCHEME_DISTRIBUTED
             : sScheme.equals("local-scheme")                   ? SCHEME_LOCAL
             : sScheme.equals("overflow-scheme")                ? SCHEME_OVERFLOW
             : sScheme.equals("disk-scheme")                    ? SCHEME_DISK
             : sScheme.equals("external-scheme")                ? SCHEME_EXTERNAL
             : sScheme.equals("paged-external-scheme")          ? SCHEME_EXTERNAL_PAGED
             : sScheme.equals("class-scheme")                   ? SCHEME_CLASS
             : sScheme.equals("near-scheme")                    ? SCHEME_NEAR
             : sScheme.equals("versioned-near-scheme")          ? SCHEME_VERSIONED_NEAR
             : sScheme.equals("read-write-backing-map-scheme")  ? SCHEME_READ_WRITE_BACKING
             : sScheme.equals("versioned-backing-map-scheme")   ? SCHEME_VERSIONED_BACKING
             : sScheme.equals("invocation-scheme")              ? SCHEME_INVOCATION
             : sScheme.equals("proxy-scheme")                   ? SCHEME_PROXY
             : sScheme.equals("remote-cache-scheme")            ? SCHEME_REMOTE_CACHE
             : sScheme.equals("remote-invocation-scheme")       ? SCHEME_REMOTE_INVOCATION
             : sScheme.equals("transactional-scheme")           ? SCHEME_TRANSACTIONAL
             : sScheme.equals("paged-topic-scheme")             ? SCHEME_PAGED_TOPIC
             :                                                    SCHEME_UNKNOWN;
        }

    /**
    * Determines whether or not the specified Map is optimized for a
    * {@link Map#putAll putAll()} operation versus a regular
    * {@link Map#put put()} operation.
    *
    * @param map  a Map instance to check
    *
    * @return true if putAll should be preferred over put if the return value
    *          is not needed; false otherwise
    */
    public static boolean isPutAllOptimized(Map map)
        {
        if (map instanceof SafeHashMap || map instanceof HashMap)
            {
            return false;
            }

        if (map instanceof ReadWriteBackingMap)
            {
            ReadWriteBackingMap mapRW = (ReadWriteBackingMap) map;
            return mapRW.isWriteThrough()
                    || isPutAllOptimized(mapRW.getInternalCache());
            }

        // assume it is for all other types
        return true;
        }

    /**
    * Determine whether the provided map allows reference access to the keys it holds.
    *
    * @param map  a Map instance to check
    *
    * @return true iff the Map's keys can be canonicalized
    */
    public static boolean isCanonicalKeySupported(Map map)
        {
        // only CCMs allow us to fetch the original key (#getCacheEntry)
        if (map instanceof ConfigurableCacheMap)
            {
            return true;
            }

        return false;
        }

    /**
    * Return the request timeout based on the {@link XmlElement}.
    *
    * @param xmlScheme  the xml scheme that stores the request timeout
    *
    * @return the request timeout
    */
    protected static long getRequestTimeout(XmlElement xmlScheme)
        {
        String sTimeout;
        switch (translateStandardSchemeType(xmlScheme.getName()))
            {
            case SCHEME_REMOTE_CACHE:
            case SCHEME_REMOTE_INVOCATION:
                sTimeout = xmlScheme.getSafeElement("initiator-config/outgoing-message-handler/request-timeout")
                        .getString();
                break;
            default:
                sTimeout = xmlScheme.getSafeElement("request-timeout").getString();
            }
        return sTimeout.isEmpty() ? -1 : XmlHelper.parseTime(sTimeout);
        }

    /**
    * Parse undocumented values of the <tt>&lt;partitioned&gt;</tt> element or
    * attribute value to determine if the backing map is partitioned.
    *
    * @param sPartitioned  the value of the <tt>&lt;partitioned&gt;</tt> element
    * @param nSchemeType  the type of the scheme
    *
    * @return true if the backing map is partitioned
    *
    * @since Coherence 3.6
    */
    private static boolean isPartitioned(String sPartitioned, int nSchemeType)
        {
        if (sPartitioned.length() == 0)
            {
            // if no particular value is specified, we'll check the scheme type
            switch (nSchemeType)
                {
                // flash and ram journals are by default partitioned
                case SCHEME_FLASHJOURNAL:
                case SCHEME_RAMJOURNAL:
                    return true;

                default:
                    return false;
                }
            }

        if (sPartitioned.equals("observable")) // do NOT doc!
            {
            return true;
            }

        Boolean BPartitioned = (Boolean) XmlHelper.convert(sPartitioned, XmlValue.TYPE_BOOLEAN);
        if (BPartitioned == null)
            {
            throw new IllegalArgumentException("Invalid \"partitioned\" value: \""
                    + sPartitioned + "\"");
            }

        return BPartitioned.booleanValue();
        }

    /**
    * Determines whether or not the serializers for the specified services are
    * compatible.  In other words, this method returns true iff object
    * serialized with the first Serializer can be deserialized by the second
    * and visa versa.
    *
    * @param serviceThis  the first Service
    * @param serviceThat  the second Service
    *
    * @return true iff the two Serializers are stream compatible
    */
    protected boolean isSerializerCompatible(Service serviceThis, Service serviceThat)
        {
        return ExternalizableHelper.isSerializerCompatible(
            serviceThis.getSerializer(), serviceThat.getSerializer());
        }

    // ----- Interceptor support --------------------------------------------

    /**
     * Using the provided base XML find all interceptors instantiating as
     * appropriate and registering with the Events {@link Registry}.
     * <p>
     * Interceptors may exist in either the <tt>caching-scheme-mapping</tt>
     * or within a <tt>distributed-scheme</tt>. The former allows restricting
     * events based on cache named whilst the latter can be bound at service
     * level.
     *
     * @param xmlConfig  the base cache configuration xml
     *
     * @since 12.1.2
     */
    protected void configureInterceptors(XmlElement xmlConfig)
        {
        // register global interceptors
        XmlElement xmlGlobalIncptrs = xmlConfig.getElement("interceptors");
        if (xmlGlobalIncptrs != null)
            {
            for (Iterator iterInterceptor = xmlGlobalIncptrs.getElements("interceptor");
                 iterInterceptor.hasNext();)
                {
                XmlElement xmlInterceptor = (XmlElement) iterInterceptor.next();
                registerInterceptor(xmlInterceptor, "", "");
                }
            }

        // process caching-scheme-mapping section
        for (Iterator iter = xmlConfig.getSafeElement("caching-scheme-mapping").
                getElements("cache-mapping"); iter.hasNext();)
            {
            XmlElement xmlMapping      = (XmlElement) iter.next();
            XmlElement xmlInterceptors = xmlMapping.getElement("interceptors");

            if (xmlInterceptors == null)
                {
                continue;
                }

            XmlElement xmlScheme    = findScheme(xmlMapping.getSafeElement("scheme-name").getString());
            String     sServiceName = xmlScheme.getSafeElement("service-name").getString(null);
            String     sCacheName   = xmlMapping.getSafeElement("cache-name").getString(null);

            for (Iterator iterInterceptor = xmlInterceptors.getElements("interceptor");
                 iterInterceptor.hasNext();)
                {
                XmlElement xmlInterceptor = (XmlElement) iterInterceptor.next();
                registerInterceptor(xmlInterceptor, sCacheName, sServiceName);
                }
            }

        // process cache-schemes section
        for (Iterator iter = xmlConfig.getSafeElement("caching-schemes").
                getElementList().iterator(); iter.hasNext();)
            {
            XmlElement xmlScheme       = (XmlElement) iter.next();
            String     sSchemeType     = xmlScheme.getName();
            XmlElement xmlInterceptors = xmlScheme.getElement("interceptors");

            if (xmlInterceptors == null || translateStandardSchemeType(sSchemeType) != SCHEME_DISTRIBUTED)
                {
                continue;
                }

            String sServiceName = xmlScheme.getSafeElement("service-name").getString();
            for (Iterator iterInterceptor = xmlInterceptors.getElements("interceptor");
                 iterInterceptor.hasNext();)
                {
                XmlElement xmlInterceptor = (XmlElement) iterInterceptor.next();
                registerInterceptor(xmlInterceptor, "", sServiceName);
                }
            }
        }

    /**
     * This method will instantiate an {@link EventInterceptor} based on the
     * XML provided. Additionally the service and cache names will be passed
     * to the interceptor if possible. These values should be empty strings
     * if they are to be ignored.
     * <p>
     * This method will register the {@link EventInterceptor} instantiated
     * with the events registry; {@link Registry}.
     *
     * @param xmlInterceptor  the xml containing the definition of the
     *                        interceptor
     * @param sCacheName      the cache name or an empty string
     * @param sServiceName    the service name or an empty string
     *
     * @throws IllegalArgumentException if a nested <tt>instance</tt>
     *         or <tt>class-scheme</tt> is absent, an interceptor identifier
     *         is missing or the {@link EventInterceptor} implementation can
     *         not be resolved
     *
     * @since Coherence 12.1.2
     */
    protected void registerInterceptor(XmlElement xmlInterceptor, String sCacheName, String sServiceName)
        {
        if (xmlInterceptor == null)
            {
            return;
            }

        // mandatory elements
        XmlElement xmlClass = xmlInterceptor.getElement("instance");
                   xmlClass = xmlClass == null ? xmlInterceptor.getElement("class-scheme") : xmlClass;

        // validate
        if (xmlClass == null)
            {
            throw new IllegalArgumentException(String.format(
                    "Interceptor specified in [cacheName: %s, serviceName: %s] must specify class",
                    sCacheName, sServiceName));
            }

        String sClassName = xmlClass.getSafeElement("class-name").getString();

        NamedEventInterceptorBuilder builder = new NamedEventInterceptorBuilder();
        builder.setCustomBuilder(new InstanceBuilder<EventInterceptor>(sClassName, XmlHelper.parseInitParams(xmlClass.getSafeElement("init-params"))));

        // optional attributes
        RegistrationBehavior behavior    = com.tangosol.util.RegistrationBehavior.ALWAYS;
        String               sIdentifier = xmlInterceptor.getSafeElement("name").getString();
        if (sIdentifier.length() > 0)
            {
            builder.setName(sIdentifier);
            // if name is explicitly configured then fail if there is a duplicate
            behavior = RegistrationBehavior.FAIL;
            }
        builder.setRegistrationBehavior(behavior);

        String sOrder = xmlInterceptor.getSafeElement("order").getString();
        if (sOrder.length() > 0)
            {
            builder.setOrder(Interceptor.Order.valueOf(sOrder));
            }

        // create a parameter resolver containing the cache pattern and service name
        // so that the builder can use it if required
        ResolvableParameterList resolver = new ResolvableParameterList();
        if (sCacheName != null && !sCacheName.isEmpty())
            {
            resolver.add(new Parameter("cache-name", sCacheName));
            }
        if (sServiceName != null && !sServiceName.isEmpty())
            {
            resolver.add(new Parameter("service-name", sServiceName));
            }

        NamedEventInterceptor interceptor = builder.realize(resolver, getConfigClassLoader(), null);

        getInterceptorRegistry().registerEventInterceptor(interceptor);
        }

    // ----- Registration support --------------------------------------------

    /**
    * Register the specified NamedCache with the cluster registry.
    *
    * @param cache     the NamedCache object to register
    * @param sContext  the cache context (tier)
    *
    * @deprecated as of Coherence 3.7.1; use {@link
    *   MBeanHelper#registerCacheMBean(NamedCache, String)} instead
    */
    protected void register(NamedCache cache, String sContext)
        {
        MBeanHelper.registerCacheMBean(cache, sContext);
        }

    /**
    * Register the specified cache with the cluster registry.
    *
    * @param service     the CacheService that the cache belongs to
    * @param sCacheName  the cache name
    * @param sContext    the cache context (tier)
    * @param map         the cache object to register
    *
    * @deprecated as of Coherence 3.7.1; use {@link
    *   MBeanHelper#registerCacheMBean(CacheService, String, String, Map)} instead
    */
    protected void register(CacheService service, String sCacheName,
                            String sContext, Map map)
        {
        MBeanHelper.registerCacheMBean(service, sCacheName, sContext, map);
        }

    /**
    * Unregister all the managed objects that belong to the specified cache
    * from the cluster registry.
    *
    * @param service     the CacheService that the cache belongs to
    * @param sCacheName  the cache name

    * @deprecated as of Coherence 3.7.1; use {@link
    *   MBeanHelper#unregisterCacheMBean(CacheService, String, String)} instead
    */
    protected void unregister(CacheService service, String sCacheName)
        {
        MBeanHelper.unregisterCacheMBean(service, sCacheName, "tier=back");
        }

    /**
    * Unregister the caches for a given cache name and context
    * from the cluster registry.
    *
    * @param sCacheName  the cache name
    * @param sContext    the cache context (tier)
    *
    * @deprecated as of Coherence 3.7.1; use {@link
    *   MBeanHelper#unregisterCacheMBean(String, String)} instead
    */
    protected void unregister(String sCacheName, String sContext)
        {
        MBeanHelper.unregisterCacheMBean(sCacheName, sContext);
        }

    /**
    * Push cache context into a thread-local storage.
    *
    * @param sContext  cache context (tag)
    */
    protected void pushCacheContext(String sContext)
        {
        m_tlo.set(sContext);
        }

    /**
    * Pop cache context from a thread-local storage.
    *
    * @return  the popped cache context
    */
    protected String popCacheContext()
        {
        String s = (String) m_tlo.get();
        m_tlo.set(null); // ThreadLocal.remove() is 1.5 only
        return s;
        }

    /**
    * Convert the value in the specified {@link XmlValue} to an int.  If the
    * conversion fails, a warning will be logged.
    *
    * @param xmlValue  the element expected to contain an int value
    *
    * @return the int value in the provided element, or 0 upon a
    *         conversion failure
    */
    protected int convertInt(XmlValue xmlValue)
        {
        return convertInt(xmlValue, 0);
        }

    /**
    * Convert the value in the specified {@link XmlValue} to an int.  If the
    * conversion fails, a warning will be logged.
    *
    * @param xmlValue  the element expected to contain an int value
    * @param nDefault  the value that will be returned if the element does
    *                  not contain a value that can be converted to int
    *
    * @return the int value in the provided element, or nDefault upon a
    *         conversion failure
    */
    protected int convertInt(XmlValue xmlValue, int nDefault)
        {
        try
            {
            String  sValue = xmlValue.getString();
            Integer I      = (Integer) XmlHelper.convert(sValue, XmlValue.TYPE_INT);

            return I == null ? nDefault : I.intValue();
            }
        catch (RuntimeException e)
            {
            reportConversionError(xmlValue, "int", String.valueOf(nDefault), e);
            return nDefault;
            }
        }

    /**
    * Convert the value in the specified {@link XmlValue} to a long.  If the
    * conversion fails, a warning will be logged.
    *
    * @param xmlValue  the element expected to contain a long value
    *
    * @return the long value in the provided element, or 0 upon a
    *         conversion failure
    */
    protected long convertLong(XmlValue xmlValue)
        {
        long lDefault = 0;
        try
            {
            String  sValue = xmlValue.getString();
            Long    L      = (Long) XmlHelper.convert(sValue, XmlValue.TYPE_LONG);

            return L == null ? lDefault : L.longValue();
            }
        catch (RuntimeException e)
            {
            reportConversionError(xmlValue, "long", String.valueOf(lDefault), e);
            return lDefault;
            }
        }

    /**
    * Convert the value in the specified {@link XmlValue} to a double. If the
    * conversion fails, a warning will be logged.
    *
    * @param xmlValue  the element expected to contain a double value
    *
    * @return the double value in the provided element, or 0.0 upon a
    *         conversion failure
    */
    protected double convertDouble(XmlValue xmlValue)
        {
        double dDefault = 0.0;
        try
            {
            String  sValue = xmlValue.getString();
            Double  D      = (Double) XmlHelper.convert(sValue, XmlValue.TYPE_DOUBLE);

            return D == null ? dDefault : D.doubleValue();
            }
        catch (RuntimeException e)
            {
            reportConversionError(xmlValue, "double", String.valueOf(dDefault), e);
            return dDefault;
            }
        }

    /**
    * Log a failed type conversion.
    *
    * @param xmlValue  element that contains the value that failed conversion
    * @param sType     type that conversion was attempted to
    * @param sDefault  default value that will be substituted
    * @param e         root cause of failed type conversion
    */
    protected void reportConversionError(XmlValue xmlValue, String sType,
            String sDefault, RuntimeException e)
        {
        Logger.warn("Error converting " + xmlValue + " to " + sType + "; proceeding with default value of "
                    + sDefault + '\n', e);
        }

    // ----- inner classes --------------------------------------------------

    /**
    * BackingMapManager implementation that uses the configuration XML to
    * create the required backing maps and provides client access to those maps.
    */
    public class Manager
            extends AbstractBackingMapManager
        {
        // ----- constructors -------------------------------------------

        public Manager()
            {
            }

        // ----- BackingMapManager interface ----------------------------

        /**
        * {@inheritDoc}
        */
        public void init(BackingMapManagerContext context)
            {
            super.init(context);

            m_mapBackingMap          = new HashMap();
            m_mapBackingMapListeners = new IdentityHashMap();
            }

        /**
        * {@inheritDoc}
        */
        public Map instantiateBackingMap(String sName)
            {
            CacheInfo  infoCache = findSchemeMapping(sName);
            XmlElement xmlScheme = resolveScheme(infoCache);

            xmlScheme.addAttribute("tier").setString("back"); // mark the "entry point"
            pushCacheContext("tier=back");

            Map map = configureBackingMap(
                infoCache, xmlScheme, getContext(), null, m_mapBackingMapListeners);

            setBackingMap(sName, map);
            return map;
            }

        /**
        * {@inheritDoc}
        */
        public boolean isBackingMapPersistent(String sName)
            {
            CacheInfo  infoCache = findSchemeMapping(sName);
            XmlElement xmlScheme = resolveScheme(infoCache);

            return !xmlScheme.getSafeElement("transient").getBoolean(true);
            }

        /**
        * {@inheritDoc}
        */
        public boolean isBackingMapSlidingExpiry(String sName)
            {
            CacheInfo  infoCache = findSchemeMapping(sName);
            XmlElement xmlScheme = resolveScheme(infoCache);

            return !xmlScheme.getSafeElement("sliding-expiry").getBoolean();
            }

        /**
        * {@inheritDoc}
        */
        public StorageAccessAuthorizer getStorageAccessAuthorizer(String sName)
            {
            return null;
            }

            /**
        * {@inheritDoc}
        */
        public void releaseBackingMap(String sName, Map map)
            {
            unregister(getContext().getCacheService(), sName);

            release(map, m_mapBackingMapListeners);

            setBackingMap(sName, null);
            }


        // ---- helpers -------------------------------------------------

        /**
        * Get the backing Map associated with a given cache.
        *
        * @param sName  the cache name
        *
        * @return a Map associated with the specified name
        */
        public Map getBackingMap(String sName)
            {
            return m_mapBackingMap == null ? null :
                (Map) m_mapBackingMap.get(sName);
            }

        /**
        * Associate the specified backing Map with a given name.
        *
        * @param sName  the cache name
        * @param map    the backing map associated with the specified name
        */
        protected void setBackingMap(String sName, Map map)
            {
            if (map != null && getBackingMap(sName) != null)
                {
                throw new IllegalArgumentException("BackingMap is not resettable: " + sName);
                }
            m_mapBackingMap.put(sName, map);
            }

        /**
        * Obtain the "container" DefaultConfigurableCacheFactory that created
        * this manager and which this manager is bound to.
        *
        * @return the DefaultConfigurableCacheFactory that created this manager
        */
        public DefaultConfigurableCacheFactory getCacheFactory()
            {
            return DefaultConfigurableCacheFactory.this;
            }


        // ----- data fields --------------------------------------------

        /**
        * The map of backing maps keyed by corresponding cache names.
        */
        protected Map m_mapBackingMap;

        /**
        * The map of backing map listeners keyed by the corresponding backing
        * map references.
        */
        protected Map m_mapBackingMapListeners;
        }


    /**
    * CacheInfo is a placeholder for cache attributes retrieved during parsing
    * the corresponding cache mapping element.
    */
    public static class CacheInfo
        {
        /**
        * Construct a CacheInfo object.
        *
        * @param sCacheName    the cache name
        * @param sSchemeName   the corresponding scheme name
        * @param mapAttribute  the corresponding map of attributes
        */
        public CacheInfo(String sCacheName, String sSchemeName, Map mapAttribute)
            {
            m_sCacheName   = sCacheName;
            m_sSchemeName  = sSchemeName;
            m_mapAttribute = mapAttribute;
            }

        // ----- accessors ------------------------------------------------

        /**
        * Obtain the cache name.
        *
        * @return  the cache name
        */
        public String getCacheName()
            {
            return m_sCacheName;
            }

        /**
        * Obtain the scheme name.
        *
        * @return  the scheme name
        */
        public String getSchemeName()
            {
            return m_sSchemeName;
            }

        /**
        * Obtain the attribute map.
        *
        * @return  the attribute map
        */
        public Map getAttributes()
            {
            return m_mapAttribute;
            }

        // ----- helpers --------------------------------------------------

        /**
        * Find and replace the attributes names in "{}" format with the
        * corresponding values for this cache info.
        * <p>
        * Note: the content of the specified XmlElement could be modified,
        * so the caller is supposed to clone the passed in XML if necessary.
        *
        * @param xml  the XmlElement to replace "{}" attributes at
        */
        public void replaceAttributes(XmlElement xml)
            {
            for (Iterator iter = xml.getElementList().iterator(); iter.hasNext();)
                {
                XmlElement xmlChild = (XmlElement) iter.next();
                if (!xmlChild.isEmpty())
                    {
                    String       sText     = xmlChild.getString();
                    int          ofStart   = sText.indexOf('{');
                    int          ofEnd     = -1;
                    boolean      fReplace  = false;
                    StringBuffer sbTextNew = new StringBuffer();

                    while (ofStart >= 0)
                        {
                        sbTextNew.append(sText.substring(ofEnd + 1, ofStart));

                        ofEnd = sText.indexOf('}', ofStart);
                        if (ofEnd < 0)
                            {
                            Logger.err("Invalid attribute format: " + sText);
                            fReplace = false;
                            break;
                            }

                        String   sAttribute = sText.substring(ofStart, ofEnd + 1);        // "{name value}"
                        String   sAttrName  = sText.substring(ofStart + 1, ofEnd).trim(); // "name value"
                        String   sDefault   = null;
                        String[] asToken    = sAttrName.split("\\s+");
                        if (asToken.length == 2)
                            {
                            sAttrName = asToken[0];
                            sDefault  = asToken[1];
                            }
                        String sValue = sAttribute.equals(CACHE_NAME) ?
                                getCacheName() : (String) getAttributes().get(sAttrName);

                        if (sValue == null)
                            {
                            if (sDefault == null)
                                {
                                if (!sAttribute.equals(CLASS_LOADER)
                                 && !sAttribute.equals(MGR_CONTEXT)
                                 && !sAttribute.equals(SCHEME_REF)
                                 && !sAttribute.equals(CACHE_REF))
                                    {
                                    Logger.warn("Missing parameter definition: "
                                        + sAttribute + " for cache \""
                                        + getCacheName() + '"');
                                    }
                                fReplace = false;
                                break;
                                }
                            else
                                {
                                sValue = sDefault;
                                }
                            }

                        sbTextNew.append(sValue);
                        fReplace = true;
                        ofStart  = sText.indexOf('{', ofEnd);
                        }

                    if (fReplace)
                        {
                        sbTextNew.append(sText.substring(ofEnd + 1));
                        xmlChild.setString(sbTextNew.toString());
                        }
                    }
                replaceAttributes(xmlChild);
                }
            }

        /**
        * Generate a synthetic CacheInfo for a cache that has a name suffixed
        * with the specified string.
        *
        * @param sSuffix  the cache name suffix
        *
        * @return  the "cloned" synthetic CacheInfo
        */
        public CacheInfo getSyntheticInfo(String sSuffix)
            {
            return new CacheInfo(getCacheName() + sSuffix, null, getAttributes());
            }


        // ----- data fields ----------------------------------------------

        /**
        * The cache name.
        */
        protected String m_sCacheName;

        /**
        * The corresponding scheme name.
        */
        protected String m_sSchemeName;

        /**
        * Map of scheme attributes.
        */
        protected Map m_mapAttribute;
        }

    /**
    * BackingMapManager implementation used by PartitionAwareBackingMap(s) to
    * lazily configure the enclosing PABM based on the configuration settings of
    * the enclosed maps.
    */
    protected class PartitionedBackingMapManager
            extends AbstractBackingMapManager
        {
        protected PartitionedBackingMapManager(CacheInfo info, XmlElement xmlScheme,
                BackingMapManagerContext context, ClassLoader loader)
            {
            m_info      = info;
            m_xmlScheme = xmlScheme;
            m_context   = context;
            m_loader    = loader;
            }

        // ----- BackingMapManager interface ------------------------------

        /**
        * {@inheritDoc}
        */
        public Map instantiateBackingMap(String sName)
            {
            XmlElement xmlScheme = m_xmlScheme;

            // the "partition-name" attribute serves as a flag to the
            // configureBackingMap() method indicating that the resulting
            // map is a part of the composite PABM topology
            xmlScheme.addAttribute("partition-name").setString(sName);

            return configureBackingMap(m_info, xmlScheme, m_context, m_loader, null);
            }

        /**
        * {@inheritDoc}
        */
        public boolean isBackingMapPersistent(String sName)
            {
            // this method should never be called
            throw new UnsupportedOperationException();
            }

        /**
        * {@inheritDoc}
        */
        public boolean isBackingMapSlidingExpiry(String sName)
            {
            // this method should never be called
            throw new UnsupportedOperationException();
            }

        /**
        * {@inheritDoc}
        */
        public StorageAccessAuthorizer getStorageAccessAuthorizer(String sName)
            {
            return null;
            }

        /**
        * {@inheritDoc}
        */
        public void releaseBackingMap(String sName, Map map)
            {
            release(map, null);
            }

        // ----- accessors and helpers ------------------------------------

        /**
        * Obtain the "container" DefaultConfigurableCacheFactory that created
        * this manager and which this manager is bound to.
        *
        * @return the DefaultConfigurableCacheFactory that created this manager
        */
        public DefaultConfigurableCacheFactory getCacheFactory()
            {
            return DefaultConfigurableCacheFactory.this;
            }

        // ----- data fields ----------------------------------------------

        /**
        * The CacheInfo for the enclosed backing maps.
        */
        protected CacheInfo m_info;

        /**
        * The xml configuration for the enclosed backing maps.
        */
        protected XmlElement m_xmlScheme;
        /**
        * The BackingMapManagerContext to pass to the enclosed backing maps.
        */
        protected BackingMapManagerContext m_context;

        /**
        * The ClassLoader to pass to the enclosed backing maps.
        */
        protected ClassLoader m_loader;
        }


    // ----- data fields and constants --------------------------------------

    /**
    * The default configuration file name.
    */
    public static final String FILE_CFG_CACHE = "coherence-cache-config.xml";

    /**
    * The name of the replaceable parameter representing the cache name.
    */
    public static final String CACHE_NAME   = "{cache-name}";

    /**
    * The name of the replaceable parameter representing the class loader.
    */
    public static final String CLASS_LOADER = "{class-loader}";

    /**
    * The name of the replaceable parameter representing the backing map
    * manager context.
    */
    public static final String MGR_CONTEXT  = "{manager-context}";

    /**
    * The name of the replaceable parameter representing a scheme reference.
    */
    public static final String SCHEME_REF   = "{scheme-ref}";

    /**
    * The name of the replaceable parameter representing a cache reference.
    */
    public static final String CACHE_REF    = "{cache-ref}";

    /**
    * The unknown scheme type.
    */
    public static final int SCHEME_UNKNOWN            = 0;

    /**
    * The replicated cache scheme.
    */
    public static final int SCHEME_REPLICATED         = 1;

    /**
    * The optimistic cache scheme.
    */
    public static final int SCHEME_OPTIMISTIC         = 2;

    /**
    * The distributed cache scheme.
    */
    public static final int SCHEME_DISTRIBUTED        = 3;

    /**
    * The near cache scheme.
    */
    public static final int SCHEME_NEAR               = 4;

    /**
    * The versioned near cache scheme.
    */
    public static final int SCHEME_VERSIONED_NEAR     = 5;

    /**
    * The local cache scheme.
    */
    public static final int SCHEME_LOCAL              = 6;

    /**
    * The overflow map scheme.
    */
    public static final int SCHEME_OVERFLOW           = 7;

    /**
    * The disk scheme.
    *
    * @deprecated As of Coherence 3.0, replaced by {@link #SCHEME_EXTERNAL}
    *             and {@link #SCHEME_EXTERNAL_PAGED}
    */
    public static final int SCHEME_DISK               = 8;

    /**
    * The external scheme.
    */
    public static final int SCHEME_EXTERNAL           = 9;

    /**
    * The paged-external scheme.
    */
    public static final int SCHEME_EXTERNAL_PAGED     = 10;

    /**
    * The custom class scheme.
    */
    public static final int SCHEME_CLASS              = 11;

    /**
    * The read write backing map scheme.
    */
    public static final int SCHEME_READ_WRITE_BACKING = 12;

    /**
    * The versioned backing map scheme.
    */
    public static final int SCHEME_VERSIONED_BACKING  = 13;

    /**
    * The invocation service scheme.
    */
    public static final int SCHEME_INVOCATION         = 14;

    /**
    * The proxy service scheme.
    */
    public static final int SCHEME_PROXY              = 15;

    /**
    * The remote cache scheme.
    */
    public static final int SCHEME_REMOTE_CACHE       = 16;

    /**
    * The remote invocation scheme.
    */
    public static final int SCHEME_REMOTE_INVOCATION  = 17;

    /**
    * The transactional cache scheme.
    */
    public static final int SCHEME_TRANSACTIONAL      = 18;

    /**
    * The flash journal cache scheme.
    */
    public static final int SCHEME_FLASHJOURNAL       = 19;

    /**
    * The ram journal cache scheme.
    */
    public static final int SCHEME_RAMJOURNAL         = 20;

    /**
     * The paged topic scheme.
     */
    public static final int SCHEME_PAGED_TOPIC        = 21;

    /**
    * The configuration XML.
    */
    private XmlElement m_xmlConfig;

    /**
    * The class loader used to load the configuration.
    */
    private ClassLoader m_loader;

    /**
    * Store that holds cache references scoped by class loader and optionally,
    * if configured, Subject.
    */
    protected ScopedCacheReferenceStore m_store = new ScopedCacheReferenceStore();

    /**
    * Thread local storage for cache context.
    */
    private ThreadLocal m_tlo = new ThreadLocal();

    /**
    * Scope name associated with this cache factory.
    */
    private String m_sScopeName;

    /**
    * A Set of BackingMapManager instances registered by this factory.
    * <p>
    * Note: we rely on the BackingMapManager classes *not* to override the
    * hashCode() and equals() methods.
    */
    protected Set m_setManager = new MapSet(new WeakHashMap());

    /**
    * The {@link ResourceRegistry} for configuration.
    */
    protected ResourceRegistry m_registry;
    }
