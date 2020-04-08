/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package jmx;


import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.management.MBeanHelper;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.filter.AndFilter;
import com.tangosol.util.filter.BetweenFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.OrFilter;

import common.AbstractFunctionalTest;

import data.persistence.Person;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.Serializable;

import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;


/**
 * Tests related to StorageManager MBean
 *
 * @author aag 2012.12.11
 */

@SuppressWarnings("serial")
public class StorageManagerMBeanTests
        extends AbstractFunctionalTest
        implements Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor
     */
    public StorageManagerMBeanTests()
        {}

    // ----- test lifecycle -------------------------------------------------

    /**
     * Method description
     */
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("tangosol.coherence.role", "main");
        System.setProperty("tangosol.coherence.log.level", "3");
        System.setProperty("tangosol.coherence.management", "all");
        System.setProperty("tangosol.coherence.management.remote", "true");
        System.setProperty("tangosol.coherence.distributed.localstorage", "true");

        AbstractFunctionalTest._startup();
        }

    /**
     * Method description
     */
    @AfterClass
    public static void _shutdown()
        {
        AbstractFunctionalTest._shutdown();
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Test that the attribute MaxQueryDescription is within the specified char
     * limit
     */
    @Test
    public void testMaxQueryDescription()
        {
        validateMaxQueryDescriptionSize("dist-test", "MaxQueryDescription", 1500);
        }

    /**
     * Regression test for COH13113.  Make sure there is no leaking of keylisteners after data removed.
     * @throws Exception
     */
    @Test
    public void testRegressionCoh13113()  throws Exception
        {
        NamedCache cache = getNamedCache("near-coh13113");
        MBeanServer server = MBeanHelper.findMBeanServer();

        for (int i = 0; i < 100; i++ )
            {
            cache.put(new Integer(i), new Integer(i+1));
            cache.get(new Integer(i));
            }

        ObjectName name = getQueryName(cache);
        Integer keyListenerCount = (Integer) server.getAttribute(name, "ListenerKeyCount");
        assertEquals("expected ListenerKeyCount to be 100", new Integer(100), keyListenerCount);
        for (int i = 0; i < 100; i++ )
            {
            cache.remove(new Integer(i));
            }

        Integer afterRemoveKeyListenerCount =  (Integer) server.getAttribute(name, "ListenerKeyCount");
        assertEquals("COH-13113 regression: expected ListenerKeyCount to be 0, if non-zero, still a leak when removing KeyListener", new Integer(0), afterRemoveKeyListenerCount);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Test that size of the specified attribute is within the specified limit
     *
     * @param sCacheName cache that needs the size check
     * @param sAttribute attribute to be tested
     * @param cSize limit on the number of characters
     */
    protected void validateMaxQueryDescriptionSize(String sCacheName, String sAttribute, int cSize)
        {
        CacheFactory.log("Validating MaxQueryDescription Size for " + sCacheName);

        try
            {
            NamedCache  cache  = getNamedCache(sCacheName);
            MBeanServer server = MBeanHelper.findMBeanServer();

            // --- load data and the filter  --------------------

            for (int i = 1; i <= 2000; i++)
                {
                cache.put("p" + i, new Person(i, "p" + i));
                }

            // building a complex filter so that the query execution is:
            // longer than 30 ms (default value for MaxQueryThresholdMillis)
            Filter filter1 = new BetweenFilter("getId", 700, 899);
            Filter filter2 = new BetweenFilter("getId", 800, 999);
            Filter filter3 = new EqualsFilter("getId", 901);
            Filter filter4 = new EqualsFilter("getName", "p902");
            Filter filterA = new AndFilter(filter1, filter2);
            Filter filterB = new OrFilter(filter3, filter4);
            Filter filter  = new OrFilter(filterA, filterB);

            Set    entries = cache.entrySet(filter);

            CacheFactory.log("Cache size after applying filter is " + entries.size());

            // --- check the size of attribute description --------------------

            ObjectName      nameMBean      = getQueryName(cache);
            Set<ObjectName> setObjectNames = server.queryNames(nameMBean, null);

            for (ObjectName name : setObjectNames)
                {
                String sAttributeDescription = (String) server.getAttribute(name, "MaxQueryDescription");

                CacheFactory.log("MaxQueryDescription: " + sAttributeDescription);
                CacheFactory.log("For Attribute " + sAttribute + ", expected size is < " + cSize + " and actual is "
                                 + sAttributeDescription.length());

                // ensuring that description size is within the specified limit.
                assertTrue(sAttributeDescription.length() < cSize);
                }

            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Return the ObjectName used in a query to get the Storage Manager MBean
     * for the specified cache
     *
     * @param cache the cache
     *
     * @return the ObjectName
     */
    protected ObjectName getQueryName(NamedCache cache)
        {
        // example:
        // Coherence:type=StorageManager,service=DistributedCache,cache=dist-test,nodeId=1
        String sName = "Coherence:type=StorageManager,service=" + cache.getCacheService().getInfo().getServiceName()
                + ",cache=" + cache.getCacheName() + ",nodeId=1";

        try
            {
            return new ObjectName(sName);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }
    }
