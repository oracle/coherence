/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.jpa;

import com.tangosol.net.cache.CacheLoader;

import com.tangosol.util.Base;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * JPA implementation of the {@link CacheLoader} interface.
 * <p>
 * Use this class as a load-only implementation that uses any JPA
 * implementation to load entities from a data store. The entities must be
 * mapped to the data store and a JPA persistence unit configuration must
 * exist.
 * <p>
 * Use the {@link JpaCacheStore} class for a full load and store implementation.
 *
 * @author mlk 2007.04.20, jh 2007.05.18
 * @see JpaCacheStore
 */
public class JpaCacheLoader<K, V>
        extends Base
        implements CacheLoader<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructor which accepts an entity name, class name, and persistence
     * unit name.
     *
     * @param sEntityName      the JPA name of the entity
     * @param sEntityClassName the fully-qualified class name of the entity
     * @param sUnitName        the name of the persistence unit
     */
    public JpaCacheLoader(String sEntityName, String sEntityClassName, String sUnitName)
        {
        initialize(sEntityName, sEntityClassName, sUnitName, null);
        }

    /**
     * Constructor which accepts an entity name, class name, persistence unit
     * name, and classloader.
     *
     * @param sEntityName      the JPA name of the entity
     * @param sEntityClassName the fully-qualified class name of the entity
     * @param sUnitName        the name of the persistence unit
     * @param loader           the ClassLoader used to load the entity class
     */
    public JpaCacheLoader(String sEntityName, String sEntityClassName, String sUnitName, ClassLoader loader)
        {
        initialize(sEntityName, sEntityClassName, sUnitName, loader);
        }

    // ----- CacheLoader interface ------------------------------------------

    @SuppressWarnings({"unchecked"})
    @Override
    public V load(K oKey)
        {
        EntityManager em = getEntityManager();
        try
            {
            return (V) em.find(m_sEntityClass, oKey);
            }
        finally
            {
            em.close();
            }
        }

    @Override
    public Map<K, V>  loadAll(Collection<? extends K> colKeys)
        {
        EntityManager em = getEntityManager();
        try
            {
            Map<K, V> mapResult = new HashMap<>();
            for (Iterator<? extends K> iter = colKeys.iterator(); iter.hasNext();)
                {
                K oKey   = iter.next();
                @SuppressWarnings({"unchecked"})
                V oValue = (V) em.find(m_sEntityClass, oKey);

                if (oValue != null)
                    {
                    mapResult.put(oKey, oValue);
                    }
                }

            return mapResult;
            }
        finally
            {
            em.close();
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Initialize this instance with the relevant metadata for the entity
     * being stored.
     *
     * @param sEntityName      the JPA name of the entity
     * @param sEntityClassName the fully-qualified class name of the entity
     * @param sUnitName        the name of the persistence unit
     * @param loader           the ClassLoader used to load the entity class
     */
    protected void initialize(String sEntityName, String sEntityClassName, String sUnitName, ClassLoader loader)
        {
        // non-null validity check
        if ((sEntityName == null) | (sEntityClassName == null) | (sUnitName == null))
            {
            throw new IllegalArgumentException("Entity name, fully-qualified entity"
                    + " class name, and persistence unit name must be specified");
            }
        this.m_sEntityName = sEntityName;

        // ensure that we have ClassLoader
        if (loader == null)
            {
            loader = getContextClassLoader();
            }

        try
            {
            this.m_sEntityClass = loader.loadClass(sEntityClassName);
            }
        catch (ClassNotFoundException e)
            {
            throw ensureRuntimeException(e, "Class " + sEntityClassName
                    + " could not be loaded");
            }

        // obtain the EntityManagerFactory
        synchronized (s_mapFactories)
            {
            m_emf = s_mapFactories.get(sUnitName);

            // if an EntityManagerFactory has not been created, do so and
            // store it for other instances to use
            if (m_emf == null)
                {
                s_mapFactories.put(sUnitName,
                        m_emf = Persistence.createEntityManagerFactory(sUnitName));
                }
            }
        }

    /**
     * Creates and returns an EntityManager. A new instance is created each
     * time this method is called.
     *
     * @return a new EntityManager to use for a subsequent operation
     */
    protected EntityManager getEntityManager()
        {
        return m_emf.createEntityManager();
        }

    // ----- constants ------------------------------------------------------

    /**
     * Map of all shared entity manager factories for all persistence units.
     */
    protected static final Map<String, EntityManagerFactory> s_mapFactories = new HashMap<>();

    // ----- data members ---------------------------------------------------

    /**
     * Name of the entity that this CacheLoader is managing.
     */
    protected String m_sEntityName;

    /**
     * The entity class that this CacheLoader is managing.
     */
    protected Class<?> m_sEntityClass;

    /**
     * The EntityManagerFactory from which EntityManager instances are obtained.
     */
    protected EntityManagerFactory m_emf;
    }