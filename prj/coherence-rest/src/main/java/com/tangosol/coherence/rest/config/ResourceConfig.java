/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.config;

import com.tangosol.coherence.rest.DefaultKeyConverter;
import com.tangosol.coherence.rest.KeyConverter;

import com.tangosol.util.Base;

import java.lang.reflect.Constructor;

import java.util.HashMap;
import java.util.Map;

/**
 * The ResourceConfig class encapsulates information related to a Coherence
 * REST resource.
 *
 * @author vp 2011.07.08
 */
public class ResourceConfig
    {

    //----- accessors -------------------------------------------------------

    /**
     * Determine the name of the cache.
     *
     * @return the cache name
     */
    public String getCacheName()
        {
        return m_sCacheName;
        }

    /**
     * Set the name of the cache.
     *
     * @param sName the cache name
     */
    public void setCacheName(String sName)
        {
        m_sCacheName = sName;
        }

    /**
     * Determine the class of the cache key.
     *
     * @return the key class
     */
    public Class getKeyClass()
        {
        return m_clzKey;
        }

    /**
     * Set the key class.
     *
     * @param clz the key class
     */
    public void setKeyClass(Class clz)
        {
        m_clzKey = clz;
        }

    /**
     * Determine the class of the cached values.
     *
     * @return the value class
     */
    public Class getValueClass()
        {
        return m_clzValue;
        }

    /**
     * Set the value class.
     *
     * @param clz the value class
     */
    public void setValueClass(Class clz)
        {
        m_clzValue = clz;
        }

    /**
     * Set the key converter class.
     *
     * @param clz the key converter class
     */
    public void setKeyConverterClass(Class clz)
        {
        if (clz == null || KeyConverter.class.isAssignableFrom(clz))
            {
            m_clzKeyConverter = clz;
            }
        else
            {
            throw new IllegalArgumentException("class \"" + clz.getName()
                    + "\" does not implement the KeyConverter interface");
            }
        }

    /**
     * Return a KeyConverter instance for this resource.
     *
     * @return key converter instance
     */
    public KeyConverter getKeyConverter()
        {
        KeyConverter keyConverter = m_keyConverter;
        if (keyConverter == null)
            {
            m_keyConverter = keyConverter = createKeyConverter();
            }

        return keyConverter;
        }

    /**
     * Return a map of marshaller classes keyed by media type.
     *
     * @return a map of REST marshaller classes keyed by media type
     */
    public Map<String, Class> getMarshallerMap()
        {
        return m_mapMarshallers;
        }

    /**
     * Set the map of marshaller classes keyed by media type.
     *
     * @param map  a map of marshaller classes keyed by media type
     */
    public void setMarshallerMap(Map<String, Class> map)
        {
        m_mapMarshallers = map;
        }

    /**
     * Return the max size of result set this resource is allowed to return.
     *
     * @return the max size of result set allowed for this resource
     */
    public int getMaxResults()
        {
        return m_cMaxResults;
        }

    /**
     * Set the max size of result set this resource is allowed to return.
     *
     * @param cMaxResults  max size of result set this resource is allowed to
     *                     return
     */
    public void setMaxResults(int cMaxResults)
        {
        m_cMaxResults = cMaxResults;
        }

    /**
     * Return the query configuration for this resource.
     *
     * @return a QueryConfig instance
     */
    public QueryConfig getQueryConfig()
        {
        return m_configQuery;
        }

    /**
     * Set the query configuration for this resource.
     *
     * @param config the query configuration
     */
    public void setQueryConfig(QueryConfig config)
        {
        m_configQuery = config;
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Create key converter instance.
     *
     * @return key converter instance
     */
    protected KeyConverter createKeyConverter()
        {
        Class clzKey          = m_clzKey;
        Class clzKeyConverter = m_clzKeyConverter;
        if (clzKeyConverter == null)
            {
            return new DefaultKeyConverter(clzKey);
            }

        Constructor ctor = null;
        try
            {
            ctor = clzKeyConverter.getConstructor(Class.class);
            }
        catch (NoSuchMethodException e)
            {
            // ignore
            }

        try
            {
            return ctor == null
                   ? (KeyConverter) clzKeyConverter.newInstance()
                   : (KeyConverter) ctor.newInstance(clzKey);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e,
                                              "unable to create key converter for class \""
                                              + clzKey.getName() + "\" and converter class \""
                                              + clzKeyConverter.getName() + "\"");
            }
        }

    // ---- data members ----------------------------------------------------

    /**
     * Name of the cache.
     */
    private String m_sCacheName;

    /**
     * Key class of the cached entries.
     */
    private Class m_clzKey;

    /**
     * Value class of the cached entries.
     */
    private Class m_clzValue;

    /**
     * Key converter class.
     */
    private Class m_clzKeyConverter;

    /**
     * Key converter to use.
     */
    private KeyConverter m_keyConverter;

    /**
     * Max size of result set this resource will return.
     */
    private int m_cMaxResults;

    /**
     * Marshaller mapping.
     */
    private Map<String, Class> m_mapMarshallers = new HashMap<String, Class>();

    /**
     * Queries configured for resource.
     */
    private QueryConfig m_configQuery;
    }