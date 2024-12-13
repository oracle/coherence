/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.jpa;

import com.tangosol.net.cache.CacheStore;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * JPA implementation of the {@link CacheStore} interface.
 * <p>
 * Use this class as a full load and store implementation that uses any JPA
 * implementation to load and store entities to and from a data store. The
 * entities must be mapped to the data store and a JPA persistence unit
 * configuration must exist.
 * <p>
 * NOTE: The persistence unit is assumed to be set to use RESOURCE_LOCAL
 * transactions.
 *
 * @author mlk 2007.04.20, jh 2007.05.18
 */
public class JpaCacheStore<K, V>
        extends JpaCacheLoader<K, V>
        implements CacheStore<K, V>
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
    public JpaCacheStore(String sEntityName, String sEntityClassName, String sUnitName)
        {
        super(sEntityName, sEntityClassName, sUnitName);
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
    public JpaCacheStore(String sEntityName, String sEntityClassName, String sUnitName, ClassLoader loader)
        {
        super(sEntityName, sEntityClassName, sUnitName, loader);
        }


    // ----- CacheStore interface -------------------------------------------

    @Override
    public void store(K oKey, V oValue)
        {
        EntityManager     em = getEntityManager();
        EntityTransaction tx = null;
        try
            {
            tx = em.getTransaction();

            tx.begin();
            em.merge(oValue);
            tx.commit();
            }
        catch (RuntimeException e)
            {
            rollback(tx);
            throw e;
            }
        finally
            {
            em.close();
            }
        }

    @Override
    public void storeAll(Map<? extends K, ? extends V> mapEntries)
        {
        EntityManager     em = getEntityManager();
        EntityTransaction tx = null;
        try
            {
            tx = em.getTransaction();

            tx.begin();
            for (Iterator<? extends V> iter = mapEntries.values().iterator(); iter.hasNext();)
                {
                em.merge(iter.next());
                }
            tx.commit();
            }
        catch (RuntimeException e)
            {
            rollback(tx);
            throw e;
            }
        finally
            {
            em.close();
            }
        }

    @Override
    public void erase(K oKey)
        {
        EntityManager     em = getEntityManager();
        EntityTransaction tx = null;
        try
            {
            tx = em.getTransaction();

            tx.begin();
            Object oValue = em.find(m_sEntityClass, oKey);
        	if (oValue != null)
                {
                em.remove(oValue);
                }
            tx.commit();
            }
        catch (RuntimeException e)
            {
            rollback(tx);
            throw e;
            }
        finally
            {
            em.close();
            }
        }

    @Override
    public void eraseAll(Collection<? extends K> colKeys)
        {
        EntityManager     em = getEntityManager();
        EntityTransaction tx = null;
        try
            {
            tx = em.getTransaction();

            tx.begin();
            for (Iterator<? extends K> iter = colKeys.iterator(); iter.hasNext();)
                {
                K oKey   = iter.next();
                Object oValue = em.find(m_sEntityClass, oKey);
                if (oValue != null)
                    {
                    em.remove(oValue);
                    }
                }
            tx.commit();
            }
        catch (RuntimeException e)
            {
            rollback(tx);
            throw e;
            }
        finally
            {
            em.close();
            }
        }


    // ----- helper methods -------------------------------------------------

    /**
     * Rollback the given EntityTransaction if it is not null and is active.
     *
     * @param tx the EntityTransaction; may be null
     */
    protected void rollback(EntityTransaction tx)
        {
        try
            {
            if (tx != null && tx.isActive())
                {
                tx.rollback();
                }
            }
        catch (RuntimeException e) {}
        }
    }