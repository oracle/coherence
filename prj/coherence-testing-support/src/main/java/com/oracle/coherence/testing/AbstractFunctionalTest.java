/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.TypeAssertion;

import java.util.Properties;

import static com.tangosol.net.cache.TypeAssertion.withTypes;

/**
* The base class for all tests for the current release of Coherence.
*
* This class is a port of the original AbstractFunctionalTest to use Oracle Tools
* instead of Ant.
*
* @author jk  2014.07.29
*/
public abstract class AbstractFunctionalTest
        extends AbstractTestInfrastructure
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public AbstractFunctionalTest()
        {
        super();
        }

    /**
    * Create a new AbstractFunctionalTest that will use the cache
    * configuration file with the given path to instantiate NamedCache
    * instances.
    *
    * @param sPath  the configuration resource name or file path
    */
    public AbstractFunctionalTest(String sPath)
        {
        super(sPath);
        }

    /**
    * Create a new AbstractFunctionalTest that will use the given factory
    * to instantiate NamedCache instances.
    *
    * @param factory  the ConfigurableCacheFactory used to instantiate
    *                 NamedCache instances
    */
    public AbstractFunctionalTest(ConfigurableCacheFactory factory)
        {
        super(factory);
        }

    // ----- AbstractFunctionalTest methods --------------------------------

    /**
    * Obtain a Coherence NamedCache by name.
    *
    * @param sCacheName  the name of the cache to obtain
    *
    * @return the NamedCache
    */
    protected <K, V> NamedCache<K, V> getNamedCache(String sCacheName)
        {
        return getNamedCache(sCacheName, getClass().getClassLoader(), null);
        }

    /**
    * Obtain a Coherence NamedCache by name.
    *
    * @param sCacheName     the name of the cache to obtain
    * @param typeAssertion  the {@link TypeAssertion} for the cache
    *
    * @return the NamedCache
    */
    protected <K, V> NamedCache<K, V> getNamedCache(String sCacheName, TypeAssertion<K, V> typeAssertion)
        {
        return getNamedCache(sCacheName, getClass().getClassLoader(), typeAssertion);
        }

    /**
    * Obtain a Coherence NamedCache by name.
    *
    * @param sCacheName      the name of the cache to obtain
    * @param loader          the ClassLoader used by the returned cache to
    *                        deserialize binary keys and values
     * @param typeAssertion  the {@link TypeAssertion} for the cache
    *
    * @return the NamedCache
    */
    protected <K, V> NamedCache<K, V> getNamedCache(String sCacheName, ClassLoader loader, TypeAssertion<K, V> typeAssertion)
        {
        return getFactory().ensureTypedCache(sCacheName, loader, typeAssertion);
        }

    protected <K, V> NamedCache<K, V> getNamedCache(String sCacheName, Class<K> clsKey, Class<V> clsValue)
        {
        return getFactory().ensureTypedCache(sCacheName, null, withTypes(clsKey, clsValue));
        }

    protected <K, V> NamedCache<K, V> getNamedCache(String sCacheName, ClassLoader loader, Class<K> clsKey, Class<V> clsValue)
        {
        return getFactory().ensureTypedCache(sCacheName, loader, withTypes(clsKey, clsValue));
        }

    /**
    * Release a cache and its associated resources.
    *
    * @param cache  the cache to be released
    */
    protected void releaseNamedCache(NamedCache cache)
        {
        getFactory().releaseCache(cache);
        }

    /**
     * The property used to turn off SE one distribution strategy.
     */
    protected static Properties PROPS_SEONE;
    static
        {
        PROPS_SEONE = new Properties();
        PROPS_SEONE.put("coherence.distribution.2server", "false");
        }
    }
