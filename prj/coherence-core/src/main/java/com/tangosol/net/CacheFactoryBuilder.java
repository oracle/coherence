/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.oracle.coherence.common.base.Classes;

import com.tangosol.coherence.config.Config;

import com.tangosol.config.expression.ChainedParameterResolver;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.expression.PropertiesParameterResolver;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;

import com.tangosol.internal.net.ScopedUriScopeResolver;

import com.tangosol.run.xml.XmlElement;

import java.util.Collections;
import java.util.Map;

/**
* CacheFactoryBuilder provides the means for building and managing configurable
* cache factories across class loaders in a pluggable fashion.
* <p>
* This is an advanced facility that could be used within J2EE or OSGi containers
* to provide a class loader based application isolation.
*
* @author gg,rhl  2009.07.14
*
* @since Coherence 3.5.1
*/
public interface CacheFactoryBuilder
        extends SessionProvider
    {
    /**
    * Return the default ConfigurableCacheFactory for a given class loader.
    *
    * @param loader  class loader for which the configuration should be
    *                used; must not be null
    *
    * @return the default ConfigurableCacheFactory for a given class loader
    */
    public ConfigurableCacheFactory getConfigurableCacheFactory(ClassLoader loader);

    /**
    * Return the ConfigurableCacheFactory for a given URI and class loader.
    *
    * @param sConfigURI  the configuration URI; must not be null
    * @param loader      class loader for which the configuration should be
    *                    used; must not be null
    *
    * @return the ConfigurableCacheFactory for a given URI and class loader
    */
    public ConfigurableCacheFactory getConfigurableCacheFactory(String sConfigURI,
                                                                ClassLoader loader); 

    /**
    * Return the ConfigurableCacheFactory for a given URI and class loader.
    *
    * @param sConfigURI  the configuration URI; must not be null
    * @param loader      class loader for which the configuration should be
    *                    used; must not be null
    * @param resolver    the optional {@link ParameterResolver} to use to resolve
     *                   configuration parameters
    *
    * @return the ConfigurableCacheFactory for a given URI and class loader
    */
    public ConfigurableCacheFactory getConfigurableCacheFactory(String sConfigURI,
                                                                ClassLoader loader,
                                                                ParameterResolver resolver);

    /**
    * Dynamically set the default cache configuration for a given class loader.
    * If a ConfigurableCacheFactory for the given class loader already exists,
    * the factory will be released.
    *
    * @param loader      class loader for which the configuration should be
    *                    used; must not be null
    * @param xmlConfig   cache configuration in xml element format
    */
    public void setCacheConfiguration(ClassLoader loader, XmlElement xmlConfig);

    /**
    * Dynamically set the cache configuration for a given URI and class loader.
    * If a ConfigurableCacheFactory for the given URI and class loader already
    * exists, the factory will be released.
    *
    * @param sConfigURI  the configuration URI; must not be null
    * @param loader      class loader for which the configuration should be
    *                    used; must not be null
    * @param xmlConfig   cache configuration in xml element format
    */
    public void setCacheConfiguration(String sConfigURI, ClassLoader loader, XmlElement xmlConfig);

    /**
    * Dynamically set the {@link ConfigurableCacheFactory} for a given URI
    * and class loader. If a ConfigurableCacheFactory for the given URI and
    * class loader already exists and the replacement is requested, the
    * factory will be released.
    *
    * @param ccf         the ConfigurableCacheFactory instance
    * @param sConfigURI  the configuration URI; must not be null
    * @param loader      class loader for which the configuration should be
    *                    used; must not be null
    * @param fReplace    specifies whether to replace a ConfigurableCacheFactory
    *                    if one is already registered
    *
    * @return  the previous ConfigurableCacheFactory associated with the URI
    *          and loader, if any
    *
    * @since Coherence 12.1.2
    */
    public ConfigurableCacheFactory setConfigurableCacheFactory(ConfigurableCacheFactory ccf, String sConfigURI,
                ClassLoader loader, boolean fReplace);

    /**
    * Release all ConfigurableCacheFactory objects for a given ClassLoader.
    *
    * @param loader  the class loader for which all associated cache factories
    *                should be released
    */
    public void releaseAll(ClassLoader loader);

    /**
    * Release the specified ConfigurableCacheFactory.
    *
    * @param factory  the ConfigurableCacheFactory to release
    */
    public void release(ConfigurableCacheFactory factory);

    // ----- SessionProvider methods ----------------------------------------

    @Override
    default Context createSession(SessionConfiguration configuration, Context context)
        {
        String            sConfigLocation   = configuration.getConfigUri()
                                                           .orElse(CacheFactoryBuilder.URI_DEFAULT);
        String            scopeName         = configuration.getScopeName();
        ClassLoader       loader            = configuration.getClassLoader().orElse(Classes.getContextClassLoader());
        String            name              = configuration.getName();
        String            sConfigUri        = ScopedUriScopeResolver.encodeScope(sConfigLocation, scopeName);
        ParameterResolver resolverCfg       = configuration.getParameterResolver().orElse(null);

        if (context.getMode() == Coherence.Mode.Client)
            {
            // If this is a client we override the coherence.client if it has not already been
            // set to be "remote" so that we force any session using the default cache config
            // file to be an extend client.
            String sProp = Config.getProperty("coherence.client", "remote");
            Map<String, String> map = Collections.singletonMap("coherence.client", sProp);
            resolverCfg = new ChainedParameterResolver(resolverCfg, new PropertiesParameterResolver(map));
            }

        // this request assumes the class loader for the session can be used
        // for loading both the configuration descriptor and the cache models
        ConfigurableCacheFactory factory = getConfigurableCacheFactory(sConfigUri, loader, resolverCfg);
        return context.complete(new ConfigurableCacheFactorySession(factory, loader, name));
        }

    // ----- constants ------------------------------------------------------

    /**
     * Default URI identifier.
     */
    public String URI_DEFAULT = "$Default$";
    }
