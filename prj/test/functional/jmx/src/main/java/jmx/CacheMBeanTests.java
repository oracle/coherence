/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package jmx;

import com.tangosol.coherence.component.util.SafeCluster;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;

import com.tangosol.net.internal.ScopedServiceReferenceStore;
import com.tangosol.net.internal.ViewCacheService;

import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.Registry;

import com.tangosol.util.Base;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.io.Serializable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test Cache MBeans registered by the ConfigurableCacheFactory (ECCF or DCCF).
 *
 * @author pfm 2012.05.14
 */
@SuppressWarnings("serial")
public class CacheMBeanTests
        extends AbstractFunctionalTest implements Serializable
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor initializes the cache config
     */
    public CacheMBeanTests()
        {
        super(FILE_CFG_CACHE);
       }

    // ----- test lifecycle -------------------------------------------------

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

    @AfterClass
    public static void _shutdown()
        {
        AbstractFunctionalTest._shutdown();
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Test that a distributed cache has a back MBean and no front MBean.
     */
    @Test
    public void testDistCacheMBean()
        {
        validateCacheMBean("dist-test", true,  0, 0);
        validateCacheMBean("dist-test", false, 1, 8 * MB);
        }

    /**
     * Test that a near cache has a back MBean and a front MBean.
     */
    @Test
    public void testNearCacheMBean()
        {
        validateCacheMBean("near-test", true,  1, 100);
        validateCacheMBean("near-test", false, 1, 8 * MB);
        }

    /**
     * Test that MBeans report the accurate size for Near cache (for both front
     * and back cache).
     */
    @Test
    public void testNearCacheSize()
        {
        validateCacheSize("near-test", 7000);
        }

    /**
     * Test that MBeans report the accurate size for Read Write Backing Map
     * cache.
     */
    @Test
    public void testRWBMCacheSize()
        {
        validateCacheSize("readwrite-test", 8000);
        }

    /**
     * Test that MBeans report the accurate size for Overflow cache.
     */
    @Test
    public void testOverflowCacheSize()
        {
        validateCacheSize("overflow-test", 9000);
        }

    /**
     * Test that MBeans report the accurate size for RWBM with overflow cache.
     */
    @Test
    public void testRWBMWithOverflowCacheSize()
        {
        validateCacheSize("readwriteoverflow-test", 10000);
        }

    /**
     * Test that a view cache has a MBean.
     */
    @Test
    public void testViewCacheMBean()
        {
        validateViewMBean("view-test",  15);
        }
    // ----- helpers --------------------------------------------------------

    /**
     * Test that cache size of both front and back caches are reported
     * accurately by MBeans.
     *
     * @param sCacheName  cache that needs the size check
     * @param cCacheSize  expected size
     */
    protected void validateCacheSize(String sCacheName, int cCacheSize)
        {
        CacheFactory.log("Validating Cache Size for "+sCacheName);
        try
            {
            NamedCache cache         = getNamedCache(sCacheName);
            MBeanServer serverJMX    = MBeanHelper.findMBeanServer();
            Map<Integer, String> map = new HashMap<Integer, String>();

            for (int i = 0; i < cCacheSize; i++)
                {
                map.put(i, "Val" + i);
                }
            cache.putAll(map);

            // --- test the size of the back cache --------------------

            ObjectName nameMBean = getQueryName(cache, false);
            Set<ObjectName> setObjectNames = serverJMX.queryNames(nameMBean, null);

            for (ObjectName name : setObjectNames)
                {
                int cCacheSizeFromMBean = (Integer) serverJMX.getAttribute(name, "Size");
                CacheFactory.log(String.format("Size from MBean (%s) is: %d", name, cCacheSizeFromMBean));
            CacheFactory.log("Expected Size is: " + cCacheSize);
                assertEquals(cCacheSize, cCacheSizeFromMBean);
                }

            // --- test the size of the front cache (for near-cache) --

            // invoking get() 3 times, makes the front cache size equal to 3
            cache.get(1);
            cache.get(2);
            cache.get(3);

            nameMBean = getQueryName(cache, true);
            setObjectNames = serverJMX.queryNames(nameMBean, null);

            for (ObjectName name : setObjectNames)
                {
                int cCacheSizeFromMBean = (Integer) serverJMX.getAttribute(name, "Size");
                CacheFactory.log(String.format("Size from MBean (%s) is: %d", name, cCacheSizeFromMBean));
                CacheFactory.log("Expected Size is: 3");
                assertEquals(3, cCacheSizeFromMBean);
                }
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Test that both a front and back cache MBeans are registered when the cache is
     * created and unregistered when the cache is destroyed.  Only NearCache
     * has a front MBean.  Do a sanity check of the high-units to make sure
     * we have the correct MBean.
     *
     * @param sCacheName          the cache name
     * @param fFront              true for the front MBean; false for a back MBean
     * @param cExpectedCount      the number of MBeans expected
     * @param cExpectedHighUnits  the number of high units expected
     *
     */
    protected void validateCacheMBean(String sCacheName, boolean fFront,
            int cExpectedCount, int cExpectedHighUnits)
        {
        try
            {
            NamedCache cache = getNamedCache(sCacheName);

            // connect to local MBeanServer to retrieve info for the MBean
            MBeanServer serverJMX = MBeanHelper.findMBeanServer();

            // get the CacheMBean, which is a DynamicMBean
            ObjectName nameMBean = getQueryName(cache, fFront);
            Set<ObjectName> setObjectNames = serverJMX.queryNames(nameMBean, null);
            assertEquals(cExpectedCount, setObjectNames.size());

            for (ObjectName name : setObjectNames)
                {
                int cHighUnits = (Integer) serverJMX.getAttribute(name, "HighUnits");
                assertEquals(cExpectedHighUnits, cHighUnits);
                }

            // test that the MBean is destroyed when the cache is released
            getFactory().destroyCache(cache);
            setObjectNames = serverJMX.queryNames(nameMBean, null);
            assertTrue(setObjectNames.isEmpty());
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    protected void validateViewMBean(String sCacheName, int cCacheSize)
        {
        try
            {
            NamedCache cache = getNamedCache(sCacheName);
            Map<Integer, String> map = new HashMap<Integer, String>();

            for (int i = 0; i < cCacheSize; i++)
                {
                map.put(i, "Val" + i);
                }
            cache.putAll(map);

            // connect to local MBeanServer to retrieve info for the MBean
            MBeanServer serverJMX = MBeanHelper.findMBeanServer();

            String sName = "Coherence:name=" + cache.getCacheName()
                           + ",nodeId=1,service=" + cache.getCacheService().getInfo().getServiceName()
                           + "," + Registry.VIEW_TYPE;

            ObjectName      nameMBean      = new ObjectName(sName);
            Set<ObjectName> setObjectNames = serverJMX.queryNames(nameMBean, null);
            assertEquals(1, setObjectNames.size());

            for (ObjectName name : setObjectNames)
                {
                long cSize = (long) serverJMX.getAttribute(name, "Size");
                assertEquals(cCacheSize, cSize);

                long interval = (long) serverJMX.getAttribute(name, "ReconnectInterval");
                assertEquals(123L, interval);
                }


	    SafeCluster                 safeCluster = (SafeCluster) cache.getService().getCluster();
            ScopedServiceReferenceStore store       = safeCluster.getScopedServiceStore();
            Service                     service     = store.getService(ViewCacheService.KEY_CLUSTER_REGISTRY + "-DistributedCache");
            ((ViewCacheService)service).destroyCache(cache);

            setObjectNames = serverJMX.queryNames(nameMBean, null);
            assertTrue(setObjectNames.isEmpty());
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Return the ObjectName used in a query to get the cache MBeans for either
     * the front or back tier.
     *
     * @param cache  the cache
     * @param fFront true for the front MBean; false for a back MBean
     *
     * @return the ObjectName
     */
    protected ObjectName getQueryName(NamedCache cache, boolean fFront)
        {
        String sTier = fFront ? "front,loader=*" : "back";

        // examples:
        // "Coherence:name=dist-test,nodeId=1,service=DistributedCache,tier=back,type=Cache"
        // "Coherence:name=dist-test,nodeId=1,service=DistributedCache,tier=front,loader=*,type=Cache"
        String sName =  "Coherence:name=" + cache.getCacheName()
            + ",nodeId=1,service=" + cache.getCacheService().getInfo().getServiceName()
            + ",tier=" + sTier + ",type=Cache";

        try
            {
            return new ObjectName(sName);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    // ----- constants ------------------------------------------------------

    private static final int MB               = 1024 * 1024;

    /**
     * cache configuration file with the required schemes.
     */
    public final static String FILE_CFG_CACHE = "cache-config.xml";
    }
