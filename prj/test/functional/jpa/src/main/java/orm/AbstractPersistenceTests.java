/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package orm;


import com.tangosol.net.NamedCache;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import data.persistence.Person;
import data.persistence.CompoundPerson1;
import data.persistence.CompoundPerson2;
import data.persistence.DomainClassPolicy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.Query;

import org.eclipse.persistence.internal.jpa.EntityManagerImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.*;


/**
* Abstract class for persistent CacheStore functional tests. JPA is used to
* create the initial data set.
*/
public abstract class AbstractPersistenceTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new AbstractPersistenceTests that will use the cache
    * configuration file with the given path to instantiate NamedCache
    * instances.
    *
    * @param sPath  the configuration resource name or file path
    */
    public AbstractPersistenceTests(String sPath)
        {
        super(sPath);
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("coherence.distributed.localstorage", "true");

        AbstractFunctionalTest._startup();
        }


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

        initCache();
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

        closeCache();
        }


    // ----- test methods ---------------------------------------------------

    @Test
    public void testLoad()
            throws Exception
        {
        out("testLoad begin");

        Object oPk3 = getDomainPolicy().createPk(3);
        Object o    = m_cache.get(oPk3);

        assertNotNull("Result should be a single non-null entity", o);
        assertEquals("Should return Person instance with id 3", oPk3,
                getDomainPolicy().getPkFromEntity(o));

        out("testLoad done");
        }

    @Test
    public void testLoadAll()
            throws Exception
        {
        out("testLoadAll begin");

        Collection col  = new HashSet();
        Object     oPk1 = createPk(1);
        Object     oPk3 = createPk(3);
        col.add(oPk1);
        col.add(oPk3);

        Map map = m_cache.getAll(col);
        assertNotNull("Result should be a non-null map of entities", map);
        assertTrue("Result should be a non-null map of 2 entities",
                map.size() == 2);

        out("testLoadAll done");
        }

    @Test
    public void testStore()
            throws Exception
        {
        out("testStore begin");

        Object oPerson = newEntity(4, "John Smith");
        Object oPk4    = createPk(4);
        m_cache.put(oPk4, oPerson);

        Object o = m_cache.get(oPk4);
        assertNotNull("Should return Person instance", o);
        assertEquals("Should return Person instance with id 4", oPk4,
                getPkFromEntity(o));

        out("testStore done");
        }

    @Test
    public void testStoreAll()
            throws Exception
        {
        out("testStoreAll begin");

        HashMap map = new HashMap();
        Object  oPk4 = createPk(4);
        Object  oPk5 = createPk(5);
        Object  oPk6 = createPk(6);
        map.put(oPk4, newEntity(4, "John Smith"));
        map.put(oPk5, newEntity(5, "Jim Cassidy"));
        map.put(oPk6, newEntity(6, "Brian Dowd"));
        m_cache.putAll(map);

        Object o = m_cache.get(oPk4);
        assertNotNull("Should return Person instance", o);
        assertEquals("Should return Person instance with id 4", oPk4,
                getPkFromEntity(o));

        o = m_cache.get(oPk5);
        assertNotNull("Should return Person instance", o);
        assertEquals("Should return Person instance with id 5", oPk5,
                getPkFromEntity(o));

        o = m_cache.get(oPk6);
        assertNotNull("Should return Person instance", o);
        assertEquals("Should return Person instance with id 6", oPk6,
                getPkFromEntity(o));

        out("testStoreAll done");
        }

    @Test
    public void testErase()
            throws Exception
        {
        out("testErase begin");

        Object oPk4 = createPk(4);
        Object oPk5 = createPk(5);
        m_cache.put(oPk4, newEntity(4, "John Smith"));
        m_cache.put(oPk5, newEntity(5, "Jim Cassidy"));

        Object o = m_cache.get(oPk4);
        assertNotNull("Should return Person instance", o);
        assertEquals("Should return Person instance with id 4", oPk4,
                getPkFromEntity(o));

        m_cache.remove(oPk4);
        o = m_cache.get(oPk4);
        assertNull("Should not return Person instance", o);

        o = m_cache.get(oPk5);
        assertNotNull("Should return Person instance", o);
        assertEquals("Should return Person instance with id 5", oPk5,
                getPkFromEntity(o));

        out("testErase done");
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

    protected abstract void initCache()
            throws Exception;

    protected void closeCache()
            throws Exception
        {
        if (m_cache != null)
            {
            m_cache.destroy();
            }
        }


    // ----- accessors ------------------------------------------------------

    protected DomainClassPolicy getDomainPolicy()
        {
        return m_policy;
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
    protected NamedCache           m_cache;
    }