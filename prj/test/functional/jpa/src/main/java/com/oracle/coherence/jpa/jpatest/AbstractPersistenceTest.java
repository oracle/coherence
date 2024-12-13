/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.jpa.jpatest;

import com.tangosol.net.cache.CacheLoader;
import com.tangosol.net.cache.CacheStore;

import com.tangosol.util.Base;

import data.persistence.CompoundPerson1;
import data.persistence.CompoundPerson2;
import data.persistence.DomainClassPolicy;
import data.persistence.Person;

import org.eclipse.persistence.internal.jpa.EntityManagerImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.Query;

import static org.junit.Assert.*;

/**
* Abstract class for persistent CacheStore unit tests. JPA is used to create
* the initial data set.
*/
public abstract class AbstractPersistenceTest
        extends Base
    {
    // ----- test lifecycle -------------------------------------------------

    @Before
    public void setUp()
            throws Exception
        {
        initDomainClassPolicy();

        // create and use JPA to persist a sufficient amount of entity data
        m_emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
        EntityManager em = m_emf.createEntityManager();
        try
            {
            em.getTransaction().begin();
            em.persist(new Person(1, "John Wayne"));
            em.persist(new Person(2, "Joe Strummer"));
            em.persist(new Person(3, "Count Basie"));
            em.persist(new CompoundPerson1(1, "1", "John Wayne"));
            em.persist(new CompoundPerson1(2, "2", "Joe Strummer"));
            em.persist(new CompoundPerson1(3, "3", "Count Basie"));
            em.persist(new CompoundPerson2(1, "1", "John Wayne"));
            em.persist(new CompoundPerson2(2, "2", "Joe Strummer"));
            em.persist(new CompoundPerson2(3, "3", "Count Basie"));
            em.getTransaction().commit();
            }
        finally
            {
            em.close();
            }
        }

    @After
    public void tearDown()
            throws Exception
        {
        // use JPA to clean up the entity data
        EntityManager em = m_emf.createEntityManager();
        try
            {
            em.getTransaction().begin();
            Query q = em.createQuery("DELETE FROM Person p");
            q.executeUpdate();
            Query q2 = em.createQuery("DELETE FROM CompoundPerson1 p");
            q2.executeUpdate();
            Query q3 = em.createQuery("DELETE FROM CompoundPerson2 p");
            q3.executeUpdate();
            em.getTransaction().commit();
            }
        finally
            {
            // need to clear the object cache because of bug #6006423
            ((EntityManagerImpl) em).getServerSession()
                    .getIdentityMapAccessor()
                    .initializeIdentityMaps();

            em.close();
            m_emf.close();
            }
        }


    // ----- test methods ---------------------------------------------------

    @Test
    public void testLoadUnitTest()
            throws Exception
        {
        Base.out("testLoadUnitTest begin");

        Object oPk = createPk(3);
        Object o   = getLoader().load(oPk);
        assertNotNull("Result should be a single non-null entity", o);
        assertEquals( "Result should have a PK of 3", oPk, getPkFromEntity(o));

        Base.out("testLoadUnitTest done");
        }

    @Test
    public void testLoadAllUnitTest()
            throws Exception
        {
        Base.out("testLoadAllUnitTest begin");

        Object     oPk2 = createPk(2);
        Object     oPk3 = createPk(3);
        Collection col  = new HashSet();
        col.add(oPk2);
        col.add(oPk3);

        Map map = getLoader().loadAll(col);
        assertEquals("Wrong number of results returned", 2, map.size());

        Base.out("testLoadAllUnitTest done");
        }

    @Test
    public void testStoreUnitTest()
            throws Exception
        {
        Base.out("testStoreUnitTest begin");

        Object oPk = createPk(4);
        Object o   = newEntity(4, "John Smith");

        getStore().store(oPk, o);
        assertNotNull("Should return Person instance", getLoader().load(oPk));

        Base.out("testStoreUnitTest done");
        }

    @Test
    public void testStoreAllUnitTest()
            throws Exception
        {
        Base.out("testStoreAllUnitTest begin");

        HashMap map  = new HashMap();
        Object  oPk4 = createPk(4);
        Object  oPk5 = createPk(5);
        Object  oPk6 = createPk(6);
        map.put(oPk4, newEntity(4, "John Smith"));
        map.put(oPk5, newEntity(5, "Jim Cassidy"));
        map.put(oPk6, newEntity(6, "Brian Dowd"));
        getStore().storeAll(map);

        Object o = getLoader().load(oPk4);
        assertNotNull("Should return Person instance", o);
        assertEquals("Result should have a PK of 4", oPk4, getPkFromEntity(o));

        o = getLoader().load(oPk5);
        assertNotNull("Should return Person instance", o);
        assertEquals("Result should have a PK of 5", oPk5, getPkFromEntity(o));

        o = getLoader().load(oPk6);
        assertNotNull("Should return Person instance", o);
        assertEquals("Result should have a PK of 6", oPk6, getPkFromEntity(o));

        Base.out("testStoreAllUnitTest done");
        }

    @Test
    public void testEraseUnitTest()
            throws Exception
        {
        Base.out("testEraseUnitTest begin");

        Object oPk1 = createPk(1);
        Object oPk2 = createPk(2);
        Object oPk3 = createPk(3);
        getStore().erase(oPk3);

        assertNotNull("Should not have been deleted", getLoader().load(oPk1));
        assertNotNull("Should not have been deleted", getLoader().load(oPk2));
        assertNull("Should have been deleted", getLoader().load(oPk3));

        Base.out("testEraseUnitTest done");
        }

    @Test
    public void testEraseAllUnitTest()
            throws Exception
        {
        Base.out("testEraseAllUnitTest begin");

        Collection col  = new HashSet();
        Object     oPk1 = createPk(1);
        Object     oPk2 = createPk(2);
        Object     oPk3 = createPk(3);
        col.add(oPk1);
        col.add(oPk2);
        getStore().eraseAll(col);

        assertNull("Should have been deleted", getLoader().load(oPk1));
        assertNull("Should have been deleted", getLoader().load(oPk2));
        assertNotNull("Should not have been deleted", getLoader().load(oPk3));

        Base.out("testEraseAllUnitTest done");
        }


    // ----- helper methods -------------------------------------------------

    protected abstract void initDomainClassPolicy();

    protected Object createPk(int nId)
        {
        return getDomainPolicy().createPk(nId);
        }

    protected Object getPkFromEntity(Object o)
        {
        return getDomainPolicy().getPkFromEntity(o);
        }

    protected Object newEntity(int nId, String sName)
        {
        return getDomainPolicy().newEntity(nId, sName);
        }


    // ------ accessors -----------------------------------------------------

    protected DomainClassPolicy getDomainPolicy()
        {
        return m_policy;
        }

    protected CacheLoader getLoader()
        {
        return m_loader;
        }

    protected CacheStore getStore()
        {
        return m_store;
        }


    // ----- constants ------------------------------------------------------

    /**
    * See persistence.xml.
    */
    public static String PERSISTENCE_UNIT           = "TestUnit";

    /**
    * Entity names.
    */
    public static String ENTITY_PERSON              = "Person";
    public static String ENTITY_PERSON_ID_CLASS     = "CompoundPerson1";
    public static String ENTITY_PERSION_ID_EMBEDDED = "CompoundPerson2";


    // ----- data members ---------------------------------------------------

    protected EntityManagerFactory m_emf;
    protected DomainClassPolicy    m_policy;
    protected CacheLoader m_loader;
    protected CacheStore  m_store;
    }