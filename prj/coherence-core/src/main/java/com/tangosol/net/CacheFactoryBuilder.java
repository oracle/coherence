/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.internal.net.DefaultSessionProvider;

import com.tangosol.run.xml.XmlElement;

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
        return DefaultSessionProvider.INSTANCE.createSession(configuration, context);
        }

    @Override
    default void releaseSession(Session session)
        {
        if (session instanceof ConfigurableCacheFactorySession)
            {
            release(((ConfigurableCacheFactorySession) session).getConfigurableCacheFactory());
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Default URI identifier.
     */
    public String URI_DEFAULT = "$Default$";
    }
