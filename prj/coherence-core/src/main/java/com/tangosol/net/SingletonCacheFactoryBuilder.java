/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.run.xml.XmlElement;

/**
 * Implementation of {@link CacheFactoryBuilder} that maintains a single
 * {@link ConfigurableCacheFactory} instance regardless of the requesting
 * ClassLoader if a URI is not provided.  This builder preserves behavior
 * prior to Coherence 3.5.1.
 * <p>
 * As of Coherence 3.7, this implementation no longer provides various
 * "validation strategies". The SingletonCacheFactoryBuilder is functionally
 * equivalent to the pre 3.7 "compatibility" mode. For more advanced usage
 * it is recommended to configure the {@link ScopedCacheFactoryBuilder} or, in
 * rare cases, configure a custom {@link ScopedCacheFactoryBuilder} extension.
 *
 * @author rhl  2009.07.14
 * @author pp   2011.01.20
 *
 * @since Coherence 3.5.1
 */
public class SingletonCacheFactoryBuilder
        extends ScopedCacheFactoryBuilder
    {
    // ----- accesssors ---------------------------------------------------

    /**
     * Return the singleton cache factory. A singleton cache factory is an
     * unnamed configurable cache factory that does not depend on a class
     * loader.
     *
     * @return the singleton cache factory
     */
    protected ConfigurableCacheFactory getSingletonFactory()
        {
        ConfigurableCacheFactory ccf = m_ccfSingleton;

        if (ccf == null)
            {
            synchronized (this)
                {
                ccf = m_ccfSingleton;

                if (ccf == null)
                    {
                    ccf = buildFactory(URI_DEFAULT, getClass().getClassLoader());
                    setSingletonFactory(ccf);
                    }
                }
            }

        return ccf;
        }

    /**
     * Set the singleton cache factory (see {@link #getSingletonFactory}).
     *
     * @param ccf  the singleton configurable cache factory
     */
    protected synchronized void setSingletonFactory(ConfigurableCacheFactory ccf)
        {
        m_ccfSingleton = ccf;
        }

    // ----- CacheFactoryBuilder interface --------------------------------

    /**
     * {@inheritDoc}
     */
    public synchronized void setCacheConfiguration(String sConfigURI, ClassLoader loader, XmlElement xmlConfig)
        {
        if (URI_DEFAULT.equals(sConfigURI))
            {
            setSingletonFactory(buildFactory(sConfigURI, loader));
            }
        else
            {
            super.setCacheConfiguration(sConfigURI, loader, xmlConfig);
            }
        }

    /**
     * {@inheritDoc}
     */
    public synchronized void releaseAll(ClassLoader loader)
        {
        setSingletonFactory(null);
        super.releaseAll(loader);
        }

    /**
     * {@inheritDoc}
     */
    public synchronized void release(ConfigurableCacheFactory factory)
        {
        if (factory == getSingletonFactory())
            {
            setSingletonFactory(null);
            }
        else
            {
            super.release(factory);
            }
        }

    // ----- helpers ------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    protected ConfigurableCacheFactory getFactory(String sConfigURI, ClassLoader loader, ParameterResolver resolver)
        {
        return URI_DEFAULT.equals(sConfigURI)
               ? getSingletonFactory()
               : super.getFactory(sConfigURI, loader, resolver);
        }


    // ----- data members -------------------------------------------------

    /**
     * Singleton {@link ConfigurableCacheFactory} instance for the default URI.
     */
    protected volatile ConfigurableCacheFactory m_ccfSingleton;
    }
