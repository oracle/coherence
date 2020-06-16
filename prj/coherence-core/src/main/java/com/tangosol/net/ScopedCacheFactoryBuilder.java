/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.net.ExtensibleConfigurableCacheFactory.Dependencies;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.ClassHelper;
import com.tangosol.util.CopyOnWriteMap;
import com.tangosol.util.LiteMap;
import com.tangosol.util.Resources;

import java.io.IOException;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import static com.tangosol.util.Base.ensureClassLoader;
import static com.tangosol.util.Base.ensureRuntimeException;
import static com.tangosol.util.Base.getOriginalException;

/**
 * Implementation of {@link CacheFactoryBuilder} that manages multiple
 * instances of {@link ConfigurableCacheFactory}.  This implementation
 * supports isolation of cache configurations via the following mechanisms:
 * <ol>
 * <li>It parses the cache configuration file for the {@code <scope-name>}
 *     attribute.  If this element exists, this attribute will be set
 *     on the CCF instance.
 * <li>The scope name can be explicitly passed to the {@link #instantiateFactory}
 *     method.
 * </ol>
 * <p>
 * The scope name may be used by the {@link ConfigurableCacheFactory} instance
 * as a service name prefix.
 * </p>
 *
 * @author pp  2010.01.20
 *
 * @since Coherence 3.7
 */
public class ScopedCacheFactoryBuilder
        implements CacheFactoryBuilder
    {
    // ----- constructors -------------------------------------------------

    /**
     * Default constructor; reads scope resolver configuration from
     * operational configuration file (tangosol-coherence.xml).
     */
    public ScopedCacheFactoryBuilder()
        {
        XmlElement xmlConfig   = CacheFactory.getCacheFactoryBuilderConfig();
        XmlElement xmlResolver = xmlConfig.getElement("scope-resolver");
        if (xmlResolver != null)
            {
            try
                {
                m_scopeResolver = (ScopeResolver) XmlHelper.createInstance(
                        xmlResolver, getClass().getClassLoader(), null);
                }
            catch (Exception e)
                {
                throw ensureRuntimeException(e, "Could not create scope resolver");
                }
            }
        }

    /**
     * Constructor to provide a custom scope resolver.
     *
     * @param resolver scope resolver
     */
    public ScopedCacheFactoryBuilder(ScopeResolver resolver)
        {
        m_scopeResolver = resolver;
        }


    // ----- accessors ----------------------------------------------------

    /**
     * Obtain the scope resolver for this builder.
     *
     * @return scope resolver
     */
    public ScopeResolver getScopeResolver()
        {
        return m_scopeResolver;
        }


    // ----- CacheFactoryBuilder interface --------------------------------

    /**
     * {@inheritDoc}
     */
    public ConfigurableCacheFactory getConfigurableCacheFactory(ClassLoader loader)
        {
        return getFactory(URI_DEFAULT, loader);
        }

    /**
     * {@inheritDoc}
     */
    public ConfigurableCacheFactory getConfigurableCacheFactory(String sConfigURI, ClassLoader loader)
        {
        return getFactory(sConfigURI, loader);
        }

    /**
     * {@inheritDoc}
     */
    public void setCacheConfiguration(ClassLoader loader, XmlElement xmlConfig)
        {
        setCacheConfiguration(URI_DEFAULT, loader, xmlConfig);
        }

    /**
     * {@inheritDoc}
     */
    public synchronized void setCacheConfiguration(String sConfigURI, ClassLoader loader, XmlElement xmlConfig)
        {
        loader = ensureClassLoader(loader);

        Map<String, ConfigurableCacheFactory> mapCCF = m_mapByLoader.get(loader);
        ConfigurableCacheFactory ccf = mapCCF == null ? null : mapCCF.get(sConfigURI);
        if (ccf != null)
            {
            release(ccf);
            }

        URL url = resolveURL(resolveURI(sConfigURI), loader);
        setXmlConfig(loader, url, xmlConfig);
        }

    /**
     * {@inheritDoc}
     */
    public ConfigurableCacheFactory setConfigurableCacheFactory(ConfigurableCacheFactory ccf,
            String sConfigURI, ClassLoader loader, boolean fReplace)
        {
        loader = ensureClassLoader(loader);

        Map<String, ConfigurableCacheFactory> mapCCF = m_mapByLoader.get(loader);
        ConfigurableCacheFactory ccfOld = mapCCF == null ? null : mapCCF.get(sConfigURI);

        if (ccfOld != null)
            {
            if (!fReplace)
                {
                return ccfOld;
                }
            release(ccfOld);
            }

        mapCCF = ensureConfigCCFMap(loader);
        mapCCF.put(sConfigURI, ccf);

        return ccfOld;
        }

    /**
     * {@inheritDoc}
     */
    public synchronized void releaseAll(ClassLoader loader)
        {
        m_mapByLoader.remove(ensureClassLoader(loader));
        }

    /**
     * {@inheritDoc}
     */
    public synchronized void release(ConfigurableCacheFactory factory)
        {
        // track ClassLoaders that no longer have any associated factories;
        // since the iterator for mapByLoader is read only, these ClassLoaders
        // must be removed via the Map itself (the "front door")
        Set<ClassLoader> setLoader = new HashSet();

        Map<ClassLoader, Map<String, ConfigurableCacheFactory>> mapByLoader = m_mapByLoader;
        for (Map.Entry<ClassLoader, Map<String, ConfigurableCacheFactory>> entry : mapByLoader.entrySet())
            {
            Map<String, ConfigurableCacheFactory> mapCCF = entry.getValue();
            for (Iterator<ConfigurableCacheFactory> iterCCF = mapCCF.values().iterator(); iterCCF.hasNext(); )
                {
                if (factory.equals(iterCCF.next()))
                    {
                    iterCCF.remove();
                    }
                }
            if (mapCCF.isEmpty())
                {
                // this ClassLoader no longer has any associated factories; track for removal
                setLoader.add(entry.getKey());
                }
            }

        Map<ClassLoader, Map<URI, XmlElement>> mapConfigByLoader = m_mapConfigByLoader;

        mapByLoader.keySet().removeAll(setLoader);
        mapConfigByLoader.keySet().removeAll(setLoader);
        }


    // ----- helper methods -----------------------------------------------

    /**
     * Helper method to return a {@link ConfigurableCacheFactory} instance for the
     * specified URI and class loader.
     *
     * @param sConfigURI  the configuration URI to return a {@link ConfigurableCacheFactory} for
     * @param loader      the loader to return a CCF for
     *
     * @return a {@link ConfigurableCacheFactory} instance
     */
    protected ConfigurableCacheFactory getFactory(final String sConfigURI, final ClassLoader loader)
        {
        return System.getSecurityManager() == null
                ? getFactoryInternal(sConfigURI, loader)
                : AccessController.doPrivileged((PrivilegedAction<ConfigurableCacheFactory>)
                    () -> getFactoryInternal(sConfigURI, loader));
        }

    /**
     * Implementation of {@link #getFactory(String, ClassLoader)}.
     */
    private ConfigurableCacheFactory getFactoryInternal(String sConfigURI, ClassLoader loader)
        {
        ClassLoader loaderSearch = loader = ensureClassLoader(loader);

        // most likely code path: retrieve existing factory from provided
        // ClassLoader or its parents; create if it doesn't exist

        // Note: Returning a CCF associated with the parent's class loader
        // may disallow loading classes bound to the given class loader;
        // however this constraint is introduced to accommodate for the EAR /
        // WAR / GAR use case
        Map<String, ConfigurableCacheFactory> mapCCF;
        do
            {
            mapCCF = m_mapByLoader.get(loaderSearch);
            }
        while ((mapCCF == null || !mapCCF.containsKey(sConfigURI))
              && (loaderSearch = loaderSearch.getParent()) != null);

        ConfigurableCacheFactory ccf = mapCCF == null ? null : mapCCF.get(sConfigURI);
        if (ccf == null)
            {
            synchronized (this)
                {
                mapCCF = ensureConfigCCFMap(loader);
                ccf    = mapCCF.get(sConfigURI);

                if (ccf == null)
                    {
                    ccf = buildFactory(sConfigURI, loader);
                    }
                mapCCF.put(sConfigURI, ccf);
                }
            }
        return ccf;
        }

    /**
     * Ensure that a map from URI to ConfigurableCacheFactory for the specified
     * loader exists (creating it if necessary).
     *
     * @param loader  the class loader to which the map corresponds
     *
     * @return a map from URI to ConfigurableCacheFactory
     */
    protected synchronized Map<String, ConfigurableCacheFactory> ensureConfigCCFMap(ClassLoader loader)
        {
        Map<ClassLoader, Map<String, ConfigurableCacheFactory>> mapByLoader = m_mapByLoader;
        Map<String, ConfigurableCacheFactory> mapCCF = mapByLoader.get(loader);

        if (mapCCF == null)
            {
            mapCCF = new LiteMap();
            mapByLoader.put(loader, mapCCF);
            }
        return mapCCF;
        }

    /**
     * Ensure that a map from URL to ConfigurableCacheFactory for the specified
     * loader exists (creating it if necessary).
     *
     * @param loader  the class loader to which the map corresponds
     *
     * @return a Map from URL to ConfigurableCacheFactory
     */
    protected synchronized Map<URI, XmlElement> ensureConfigMap(ClassLoader loader)
        {
        Map<ClassLoader, Map<URI, XmlElement>> mapConfigByLoader = m_mapConfigByLoader;
        Map<URI, XmlElement> mapConfig = mapConfigByLoader.get(loader);

        if (mapConfig == null)
            {
            mapConfig = new HashMap<>();
            mapConfigByLoader.put(loader, mapConfig);
            }
        return mapConfig;
        }

    /**
     * Return the {@link XmlElement XML config} relating to the provided
     * ClassLoader and URL, or null.
     *
     * @param loader  the ClassLoader the XML was registered with
     * @param url     the URL the XML was registered with
     *
     * @return the XML config relating to the provided ClassLoader and URL,
     *         or null
     */
    protected XmlElement getXmlConfig(ClassLoader loader, URL url)
        {
        Map<URI, XmlElement> mapXml = ensureConfigMap(loader);
        try
            {
            return mapXml.get(url.toURI());
            }
        catch (URISyntaxException e) {}

        return null;
        }

    /**
     * Register the provided {@link XmlElement XML config} with the ClassLoader
     * and URL.
     *
     * @param loader  the ClassLoader the XML is to be registered with
     * @param url     the URL the XML is to be registered with
     * @param xml     the XML config to register
     */
    protected void setXmlConfig(ClassLoader loader, URL url, XmlElement xml)
        {
        Map<URI, XmlElement> mapXml = ensureConfigMap(loader);
        try
            {
            mapXml.put(url.toURI(), xml);
            }
        catch (URISyntaxException e) {}
        }

    /**
     * Load the XML configuration from the specified URI.
     *
     * @param sConfigURI  the configuration URI; must not be null
     * @param loader      class loader to use
     *
     * @return the XML configuration, or null if the config could not be loaded
     */
    protected synchronized XmlElement loadConfigFromURI(String sConfigURI, ClassLoader loader)
        {
        URL        url       = resolveURL(sConfigURI, loader);
        XmlElement xmlConfig = getXmlConfig(loader, url);

        if (xmlConfig == null)
            {
            try
                {
                xmlConfig = XmlHelper.loadResource(url, "cache configuration", loader);
                setXmlConfig(loader, url, xmlConfig);
                }
            catch (Exception e)
                {
                if (e instanceof RuntimeException)
                    {
                    Throwable eOrig = getOriginalException((RuntimeException) e);
                    if (eOrig instanceof IOException)
                        {
                        StringBuilder sb = new StringBuilder("Could not load cache configuration resource " + url);
                        Throwable cause  = eOrig.getCause();

                        if (cause != null)
                            {
                            sb.append(", Cause:").append(cause.getMessage());
                            }
                        e = new IOException(sb.toString());
                        }
                    }
                throw ensureRuntimeException(e);
                }
            }

        return xmlConfig;
        }

    /**
     * Return the XML configuration used for the construction of a {@link ConfigurableCacheFactory}.
     *
     * @return the {@link XmlElement} that contains construction configuration
     */
    protected XmlElement getConfigurableCacheFactoryConfig()
        {
        return CacheFactory.getConfigurableCacheFactoryConfig();
        }

    /**
     * Construct and configure a {@link ConfigurableCacheFactory} for the specified
     * cache config URI and {@link ClassLoader}.
     *
     * @param sConfigURI      the URI to the cache configuration
     * @param loader          the {@link ClassLoader} associated with the factory
     *
     * @return a ConfigurableCacheFactory for the specified XML configuration
     */
    protected ConfigurableCacheFactory buildFactory(String sConfigURI, ClassLoader loader)
        {
        String     sResolved  = resolveURI(sConfigURI);
        XmlElement xmlConfig  = loadConfigFromURI(sResolved, loader);

        ScopeResolver resolver = getScopeResolver();
        String        sScope   = resolver == null ? null :
            resolver.resolveScopeName(sConfigURI, loader, null);

        return instantiateFactory(loader, xmlConfig,
            getConfigurableCacheFactoryConfig(), null, sScope);
        }

    /**
     * Create a new instance of {@link ConfigurableCacheFactory} based on a given
     * {@link ClassLoader} and cache configuration XML.
     *
     * @param loader         the {@link ClassLoader} used to instantiate the {@link ConfigurableCacheFactory}
     * @param xmlConfig      the {@link XmlElement} containing the cache configuration
     * @param xmlFactory     the {@link XmlElement} containing the factory definition
     * @param sPofConfigURI  the POF configuration URI
     * @param sScopeName     an optional scope name
     *
     * @return the {@link ConfigurableCacheFactory} created
     */
    protected ConfigurableCacheFactory instantiateFactory(ClassLoader loader, XmlElement xmlConfig,
            XmlElement xmlFactory, String sPofConfigURI, String sScopeName)
        {
        // temporarily allow user to select ECCF via a property
        String sClass = xmlFactory.getSafeElement("class-name").getString();
        try
            {
            if (sClass.equals(ExtensibleConfigurableCacheFactory.class.getName()))
                {
                Dependencies dependencies = ExtensibleConfigurableCacheFactory.DependenciesHelper.
                    newInstance(xmlConfig, loader, sPofConfigURI, sScopeName);

                ExtensibleConfigurableCacheFactory eccf = new ExtensibleConfigurableCacheFactory(dependencies);
                eccf.setConfigClassLoader(loader);
                return eccf;
                }
            else if (sClass.equals(DefaultConfigurableCacheFactory.class.getName()))
                {
                DefaultConfigurableCacheFactory dccf = new DefaultConfigurableCacheFactory(xmlConfig);
                dccf.setConfigClassLoader(loader);

                sScopeName = xmlConfig.getSafeElement("scope-name").getString(sScopeName);
                if (sScopeName != null && sScopeName.length() > 0)
                    {
                    dccf.setScopeName(sScopeName);
                    }
                return dccf;
                }
            else
                {
                ConfigurableCacheFactory ccf = (ConfigurableCacheFactory)
                        XmlHelper.createInstance(xmlFactory, loader, null);

                Method methSetConfig = ClassHelper.findMethod(ccf.getClass(), "setConfig",
                        new Class[]{XmlElement.class}, false);
                if (methSetConfig != null)
                    {
                    ClassHelper.invoke(ccf, "setConfig", new Object[]{xmlConfig});
                    }
                return ccf;
                }
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e, "Failed to instantiate a class from the xmlConfiguration "
                    + xmlConfig);
            }
         }

    /**
     * Resolve the URI that identifies the cache configuration.  The URI provided
     * may be a normal URL or Resource, or it may be a "special" default URI that
     * is used when a specific cache configuration file is not indicated (for
     * example, if the user requests a factory via {@link CacheFactory#getConfigurableCacheFactory()}.
     * If the "default" URI is requested, the URI is resolved to the default
     * cache configuration name indicated in the operational configuration file;
     * otherwise the provided URI is returned.
     *
     * @param sConfigURI  the passed in URI
     *
     * @return the resolved URI
     *
     * @see #URI_DEFAULT
     */
    protected String resolveURI(String sConfigURI)
        {
        if (sConfigURI.equals(URI_DEFAULT))
            {
            // by convention, the "default URI" is the first parameter
            // passed to DCCF per the operational configuration
            // (typically "coherence-cache-config.xml")
            XmlElement xmlFactory = CacheFactory.getConfigurableCacheFactoryConfig();
            Object []  aoParam    = XmlHelper.parseInitParams(xmlFactory.getSafeElement("init-params"));
            return aoParam.length > 0 && aoParam[0] instanceof String
                        ? (String) aoParam[0] : ExtensibleConfigurableCacheFactory.FILE_CFG_CACHE;
            }
        else
            {
            return sConfigURI;
            }
        }

    /**
     * Resolve the URL based on the provided configuration URI.  The resolution
     * consists of locating the URI as a resource or a file and the creation
     * of a corresponding URL.  If the URI cannot be located, a "placeholder"
     * file URL will be created.
     *
     * @param sConfigURI  the configuration URI to make a URL out of
     * @param loader      the {@link ClassLoader} to use
     *
     * @return a {@link URL} for the resource
     */
    protected URL resolveURL(String sConfigURI, ClassLoader loader)
        {
        URL url = Resources.findFileOrResource(sConfigURI, loader);
        if (url == null)
            {
            // if the URL does not exist, we will create a file URL so that
            // we can associate a cache configuration with a URI that doesn't
            // exist as a file or resource
            try
                {
                url = new URL((sConfigURI.contains(":") ? "" : "file://")
                        + sConfigURI);
                }
            catch (MalformedURLException e)
                {
                throw ensureRuntimeException(e, "The configuration URI contains illegal characters for a URL " +
                        sConfigURI);
                }
            }
        return url;
        }

    // ----- constants and data members -----------------------------------

    /**
     * Scope resolver used to resolve scope name upon CCF construction.
     */
    protected ScopeResolver m_scopeResolver;

    /**
     * Mapping used to associate class loaders with the cache factories that are
     * configured on them.  The map is (weakly) keyed by class loader instances
     * and holds a maps of URI to ConfigurableCacheFactory as a values
     * (e.g. Map&lt;ClassLoader, Map&lt;URI, ConfigurableCacheFactory&gt;&gt;).
     */
    protected Map<ClassLoader, Map<String, ConfigurableCacheFactory>> m_mapByLoader =
            new CopyOnWriteMap(WeakHashMap.class);

    /**
     * Mapping used to associate class loaders with specific configuration elements.
     * The map is (weakly) keyed by class loader instances and holds a map
     * of URL to XmlElement as values.
     */
    protected Map<ClassLoader, Map<URI, XmlElement>> m_mapConfigByLoader =
            new CopyOnWriteMap(WeakHashMap.class);
    }
