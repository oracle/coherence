/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package jmx;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.internal.PartitionSize;
import com.tangosol.net.management.MBeanHelper;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.filter.AndFilter;
import com.tangosol.util.filter.BetweenFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.IndexAwareFilter;
import com.tangosol.util.filter.OrFilter;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import data.persistence.Person;

import java.io.Serializable;

import java.util.Map;
import java.util.Set;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Tests related to StorageManager MBean
 *
 * @author aag 2012.12.11
 */

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
        System.setProperty("coherence.role", "main");
        System.setProperty("coherence.log.level", "3");
        System.setProperty("coherence.management", "all");
        System.setProperty("coherence.management.remote", "true");
        System.setProperty("coherence.distributed.localstorage", "true");

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
            cache.put(i, i + 1);
            cache.get(i);
            }

        ObjectName name = getQueryName(cache);

        Eventually.assertDeferred(() -> getListenerKeyCount(server, name), is(100));

        for (int i = 0; i < 100; i++ )
            {
            cache.remove(i);
            }

        Eventually.assertDeferred(() -> getListenerKeyCount(server, name), is(0));
        }

    /**
     * Static method to return listener key count given a {@link MBeanServer} and {@link ObjectName}.
     *
     * @param server  {@link MBeanServer} to query
     * @param name    {@link ObjectName} to use
     * @return the value of the listener key count
     */
    private static int getListenerKeyCount(MBeanServer server, ObjectName name)
        {
        try
            {
            return (Integer) server.getAttribute(name, "ListenerKeyCount");
            }
        catch (Exception e)
            {
            return -1;
            }
        }

    /**
     * Test cache clear operation.
     *
     * @throws Exception
     */
    @Test
    public void testClearOperation()
            throws Exception
        {
        final AtomicInteger atomicInsert = new AtomicInteger();
        final AtomicInteger atomicDelete = new AtomicInteger();
        NamedCache<Integer, Integer> cache = getNamedCache("dist-clear");
        MapListener<Integer, Integer> listener = new MapListener<>()
            {
            public void entryInserted(MapEvent<Integer, Integer> evt)
                {
                atomicInsert.incrementAndGet();
                }

            public void entryUpdated(MapEvent<Integer, Integer> evt)
                {
                }

            public void entryDeleted(MapEvent<Integer, Integer> evt)
                {
                atomicDelete.incrementAndGet();
                }
            };

        cache.addMapListener(listener);
        MBeanServer server = MBeanHelper.findMBeanServer();

        for (int i = 0; i < 100; i++)
            {
            cache.put(i, i + 1);
            }
        assertEquals(100, cache.size());
        Eventually.assertDeferred(atomicInsert::get, is(100));
        ObjectName name = getQueryName(cache);
        server.invoke(name, "clearCache", null, null);
        Eventually.assertDeferred(cache::size, is(0));
        Eventually.assertDeferred(atomicDelete::get, is(100));
        Long ClearCount = (Long) server.getAttribute(name, "ClearCount");
        assertEquals("expected ClearCount to be 1", Long.valueOf(1), ClearCount);
        }

    /**
     * Test reportPartitionStats operation.
     *
     * @throws Exception which could be ReflectionException, InstanceNotFoundException or MBeanException.
     */
    @Test
    public void testReportPartitionStats()
            throws Exception
        {
        NamedCache<Integer, Integer> cache = getNamedCache("dist-stats");
        MBeanServer server = MBeanHelper.findMBeanServer();

        for (int i = 0; i < 100; i++)
            {
            cache.put(i, i + 1);
            }
        
        ObjectName name = getQueryName(cache);
        String sJson = (String) server.invoke(name, "reportPartitionStats", new Object[]{"json"}, new String[]{"java.lang.String"});
        assertNotNull(sJson);
        assertEquals(sJson.substring(0,1), "[");

        String sCSV = (String) server.invoke(name, "reportPartitionStats", new Object[]{"csv"}, new String[]{"java.lang.String"});
        assertNotNull(sCSV);
        String[] asLines = sCSV.split("\n");
        assertTrue(asLines.length > 0);
        for (String asLine : asLines)
            {
            assertTrue(asLine.contains(","));
            }
        }

    /**
     * Test StorageManager size() operation.
     */
    @Test
    public void testSizeOperation()
        {
        NamedCache<Integer, Integer> cache = getNamedCache("dist-size");
        MBeanServer server = MBeanHelper.findMBeanServer();

        for (int i = 0; i < 100; i++)
            {
            cache.put(i, i + 1);
            }
        assertEquals(100, cache.size());
        ObjectName name = getQueryName(cache);
        Eventually.assertDeferred(() ->
            {
            try
                {
                return (Integer) server.invoke(name, "size", null, null);
                }
            catch (Exception e)
                {
                Assert.fail(e.getMessage());
                }
            return 0;
            }, is(100));
        }

    /**
     * Test cache truncate operation.
     *
     * @throws Exception
     */
    @Test
    public void testTruncateOperation()
            throws Exception
        {
        final AtomicInteger atomicInsert = new AtomicInteger();
        final AtomicInteger atomicDelete = new AtomicInteger();
        NamedCache<Integer, Integer> cache = getNamedCache("dist-truncate");
        MapListener<Integer, Integer> listener = new MapListener<>()
            {
            public void entryInserted(MapEvent<Integer, Integer> evt)
                {
                atomicInsert.incrementAndGet();
                }

            public void entryUpdated(MapEvent<Integer, Integer> evt)
                {
                }

            public void entryDeleted(MapEvent<Integer, Integer> evt)
                {
                atomicDelete.incrementAndGet();
                }
            };

        cache.addMapListener(listener);
        MBeanServer server = MBeanHelper.findMBeanServer();

        for (int i = 0; i < 100; i++)
            {
            cache.put(i, i + 1);
            }
        assertEquals(100, cache.size());
        Eventually.assertDeferred(atomicInsert::get, is(100));
        ObjectName name = getQueryName(cache);
        server.invoke(name, "truncateCache", null, null);
        Eventually.assertDeferred(cache::size, is(0));
        Eventually.assertDeferred(atomicDelete::get, is(0), Eventually.delayedBy(2, TimeUnit.SECONDS));
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

            for (int i = 1; i <= 2_000; i++)
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

            Set    entries = cache.entrySet(new SleepFilter(35L).and(filter));

            CacheFactory.log("Cache size after applying filter is " + entries.size());
            assertEquals(102, entries.size());

            // --- check the size of attribute description --------------------

            ObjectName      nameMBean      = getQueryName(cache);
            Set<ObjectName> setObjectNames = server.queryNames(nameMBean, null);

            for (ObjectName name : setObjectNames)
                {
                long   lMaxQueryDuration     = (long)   server.getAttribute(name, "MaxQueryDurationMillis");
                String sAttributeDescription = (String) server.getAttribute(name, "MaxQueryDescription");

                CacheFactory.log("MaxQueryDurationMillis: " + lMaxQueryDuration);
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

    /**
     * A simple Filter implementation that will introduce pause into query execution,
     * in order to hit MaxQueryThresholdMillis (30ms by default).
     */
    public static class SleepFilter implements IndexAwareFilter, Serializable
        {
        public SleepFilter()
            {
            this(0);
            }

        public SleepFilter(long cMillis)
            {
            f_cMillis = cMillis;
            }

        @Override
        public boolean evaluate(Object o)
            {
            return true;
            }

        @Override
        public boolean evaluateEntry(Map.Entry entry)
            {
            try
                {
                Thread.sleep(2);
                }
            catch (InterruptedException ignore)
                {
                }
            return true;
            }

        @Override
        public int calculateEffectiveness(Map mapIndexes, Set setKeys)
            {
            return 1;
            }

        @Override
        public Filter applyIndex(Map mapIndexes, Set setKeys)
            {
            try
                {
                Thread.sleep(f_cMillis);
                }
            catch (InterruptedException ignore)
                {
                }
            return null;
            }

        private final long f_cMillis;
        }
    }
