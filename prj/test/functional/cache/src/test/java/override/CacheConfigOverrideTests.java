/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package override;

import common.AbstractFunctionalTest;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.NamedCache;
import com.tangosol.net.RequestPolicyException;
import com.tangosol.net.management.MBeanHelper;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Enumeration;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import static org.junit.Assert.*;

/**
 * The {@link CacheConfigOverrideTests} class contains multiple tests for
 * overriding the cache configuration.
 */
public class CacheConfigOverrideTests
        extends AbstractFunctionalTest
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
     public CacheConfigOverrideTests()
         {
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
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void _shutdown()
        {
        // no-op since each test shutdowns the cluster in a finally block
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void testCacheConfigOverride()
        {
        try
            {
            System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE);

            AbstractFunctionalTest._startup();

            NamedCache myCacheOverride = CacheFactory.getCache("my-cache-override");
            assertNotNull(myCacheOverride);
            Enumeration cacheNames = myCacheOverride.getCacheService().getCacheNames();
            assertTrue(cacheNames.hasMoreElements());
            assertEquals("my-cache-override", cacheNames.nextElement());
            assertEquals("$SYS:MyCacheService", myCacheOverride.getService().getInfo().getServiceName());

            NamedCache myCacheOverride2 = CacheFactory.getCache("my-cache-override-2");
            assertNotNull(myCacheOverride2);
            cacheNames = myCacheOverride2.getCacheService().getCacheNames();
            assertTrue(cacheNames.hasMoreElements());
            assertEquals("my-cache-override-2", cacheNames.nextElement());
            assertEquals("$SYS:MyCacheService2", myCacheOverride2.getService().getInfo().getServiceName());

            NamedCache myCache = CacheFactory.getCache("my-cache-wildcard");
            assertNotNull(myCache);
            cacheNames = myCache.getCacheService().getCacheNames();
            assertTrue(cacheNames.hasMoreElements());
            assertEquals("my-cache-wildcard", cacheNames.nextElement());
            assertEquals("$SYS:MyCacheServiceOverride", myCache.getService().getInfo().getServiceName());
            }
        finally
            {
            System.clearProperty("coherence.cacheconfig");
            AbstractFunctionalTest._shutdown();
            }
        }

    @Test
    public void testCacheConfigOverrideSingleScheme()
            throws Exception
        {
        try
            {
            System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE);
            System.setProperty("coherence.cacheconfig.override", "override/cache-config-override-two.xml");

            AbstractFunctionalTest._startup();

            NamedCache myCache = CacheFactory.getCache("my-cache-wildcard");
            assertNotNull(myCache);
            NamedCache myCacheTwo = CacheFactory.getCache("my-cache-two");
            assertNotNull(myCacheTwo);

            Cluster cluster = CacheFactory.getCluster();
            assertTrue(cluster.isRunning());
            assertEquals("cluster already exists", 1, cluster.getMemberSet().size());

            // Check for correct QuorumStatus for service "$SYS:MyCache" without write
            MBeanServer serverJMX        = MBeanHelper.findMBeanServer();
            String      objectName       = "Coherence:type=Service,name=\"$SYS:MyCacheService\",*";
            Set<ObjectInstance> mbeanObj = serverJMX.queryMBeans(new ObjectName(objectName), null);
            assertTrue(mbeanObj.size() == 1);

            ObjectName  serviceON    = new ObjectName(mbeanObj.iterator().next().getObjectName().toString());
            String      quorumStatus = (String) serverJMX.getAttribute(serviceON, "QuorumStatus");
            assertNotNull(quorumStatus);
            assertEquals(quorumStatus, "allowed-actions=distribution, restore, read, recover");

            try
                {
                myCache.put("1", "ONE");
                fail("Exception should have been thrown when writing data to my-cache");
                }
            catch (Exception e)
                {
                assertTrue(e instanceof RequestPolicyException);
                }

            // Check for correct QuorumStatus for service "$SYS:MyCacheService2" with write
            objectName = "Coherence:type=Service,name=\"$SYS:MyCacheService2\",*";
            mbeanObj   = serverJMX.queryMBeans(new ObjectName(objectName), null);
            assertTrue(mbeanObj.size() == 1);

            serviceON    = new ObjectName(mbeanObj.iterator().next().getObjectName().toString());
            quorumStatus = (String) serverJMX.getAttribute(serviceON, "QuorumStatus");
            assertNotNull(quorumStatus);
            assertEquals(quorumStatus, "allowed-actions=distribution, restore, read, write, recover");

            myCacheTwo.put("1", "ONE");
            assertEquals("ONE", myCacheTwo.get("1"));
            }
        finally
            {
            System.clearProperty("coherence.cacheconfig");
            System.clearProperty("coherence.cacheconfig.override");
            AbstractFunctionalTest._shutdown();
            }
        }

    @Test
    public void testCacheConfigOverrideMultipeSchemes()
            throws Exception
        {
        try
            {
            System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE);
            System.setProperty("coherence.cacheconfig.override",
                    "override/cache-config-override-prepend.xml");

            AbstractFunctionalTest._startup();

            // Check for caching schemes with expected service
            NamedCache myCache = CacheFactory.getCache("my-cache-wildcard");
            assertNotNull(myCache);
            Enumeration cacheNames = myCache.getCacheService().getCacheNames();
            assertTrue(cacheNames.hasMoreElements());
            assertEquals("my-cache-wildcard", cacheNames.nextElement());
            assertEquals("$SYS:MyCacheService", myCache.getService().getInfo().getServiceName());

            NamedCache myCachePrepend = CacheFactory.getCache("my-cache-prepend");
            assertNotNull(myCachePrepend);
            cacheNames = myCachePrepend.getCacheService().getCacheNames();
            assertTrue(cacheNames.hasMoreElements());
            assertEquals("my-cache-prepend", cacheNames.nextElement());
            assertEquals("$SYS:MyCacheService2", myCachePrepend.getService().getInfo().getServiceName());

            // Check for correct QuorumStatus for service "$SYS:MyCacheService" with write
            MBeanServer serverJMX        = MBeanHelper.findMBeanServer();
            String      objectName       = "Coherence:type=Service,name=\"$SYS:MyCacheService\",*";
            Set<ObjectInstance> mbeanObj = serverJMX.queryMBeans(new ObjectName(objectName), null);
            assertTrue(mbeanObj.size() == 1);

            ObjectName  serviceON    = new ObjectName(mbeanObj.iterator().next().getObjectName().toString());
            String      quorumStatus = (String) serverJMX.getAttribute(serviceON, "QuorumStatus");
            assertNotNull(quorumStatus);
            assertEquals(quorumStatus, "allowed-actions=distribution, restore, read, write, recover");

            myCache.put("1", "ONE");
            assertEquals("ONE", myCache.get("1"));

            // Check for correct QuorumStatus for service "$SYS:MyCacheService2" with write
            objectName = "Coherence:type=Service,name=\"$SYS:MyCacheService2\",*";
            mbeanObj   = serverJMX.queryMBeans(new ObjectName(objectName), null);
            assertTrue(mbeanObj.size() == 1);

            serviceON    = new ObjectName(mbeanObj.iterator().next().getObjectName().toString());
            quorumStatus = (String) serverJMX.getAttribute(serviceON, "QuorumStatus");
            assertNotNull(quorumStatus);
            assertEquals(quorumStatus, "allowed-actions=distribution, restore, read, write, recover");

            myCachePrepend.put("1", "ONE");
            assertEquals("ONE", myCachePrepend.get("1"));
            }
        finally
            {
            System.clearProperty("coherence.cacheconfig");
            System.clearProperty("coherence.cacheconfig.override");
            AbstractFunctionalTest._shutdown();
            }
        }

    @Test
    public void testAddProxySchemeUsingCacheOverride()
            throws Exception
        {
        try
            {
            System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE);
            System.setProperty("coherence.cacheconfig.override",
                    "override/cache-config-override-proxy.xml");

            AbstractFunctionalTest._startup();
            CacheFactory.getCluster().ensureService("override:ProxyService", "Proxy").start();

            // Check for caching schemes with expected service
            NamedCache myCache = CacheFactory.getCache("my-cache-wildcard");
            assertNotNull(myCache);
            Enumeration cacheNames = myCache.getCacheService().getCacheNames();
            assertTrue(cacheNames.hasMoreElements());
            assertEquals("my-cache-wildcard", cacheNames.nextElement());
            assertEquals("$SYS:MyCacheService", myCache.getService().getInfo().getServiceName());

            NamedCache myCacheTwo = CacheFactory.getCache("my-cache-two");
            assertNotNull(myCacheTwo);
            cacheNames = myCacheTwo.getCacheService().getCacheNames();
            assertTrue(cacheNames.hasMoreElements());

            // Check for Proxy Service
            MBeanServer serverJMX        = MBeanHelper.findMBeanServer();
            String      objectName       = "Coherence:type=Service,name=\"override:ProxyService\",*";
            Set<ObjectInstance> mbeanObj = serverJMX.queryMBeans(new ObjectName(objectName), null);
            assertTrue(mbeanObj.size() == 1);
            }
        finally
            {
            System.clearProperty("coherence.cacheconfig");
            System.clearProperty("coherence.cacheconfig.override");
            AbstractFunctionalTest._shutdown();
            }
        }

    @Test
    public void testCacheConfigOverrideSysPropNoDefault()
            throws Exception
        {
        try
            {
            System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE_NO_DEFAULT);
            System.setProperty("coherence.cacheconfig.override",
                    "override/cache-config-override-ns.xml");

            AbstractFunctionalTest._startup();

            NamedCache myCacheOverride = CacheFactory.getCache("my-cache-override");
            assertNotNull(myCacheOverride);
            Enumeration cacheNames = myCacheOverride.getCacheService().getCacheNames();
            assertTrue(cacheNames.hasMoreElements());
            assertEquals("my-cache-override", cacheNames.nextElement());
            assertEquals("$SYS:MyCacheService", myCacheOverride.getService().getInfo().getServiceName());

            // Check for correct QuorumStatus for service "$SYS:MyCacheService" with write
            MBeanServer serverJMX        = MBeanHelper.findMBeanServer();
            String      objectName       = "Coherence:type=Service,name=\"$SYS:MyCacheService\",*";
            Set<ObjectInstance> mbeanObj = serverJMX.queryMBeans(new ObjectName(objectName), null);
            assertTrue(mbeanObj.size() == 1);

            ObjectName  serviceON    = new ObjectName(mbeanObj.iterator().next().getObjectName().toString());
            String      quorumStatus = (String) serverJMX.getAttribute(serviceON, "QuorumStatus");
            assertNotNull(quorumStatus);
            assertEquals(quorumStatus, "allowed-actions=distribution, restore, read, write, recover");
            }
        finally
            {
            System.clearProperty("coherence.cacheconfig");
            System.clearProperty("coherence.cacheconfig.override");
            AbstractFunctionalTest._shutdown();
            }
        }

    // ----- constants and data members -------------------------------------

    /**
     * Cache configuration file with xml-override attribute specified.
     */
    public final static String FILE_CFG_CACHE = "override/cache-config.xml";

    /**
     * Cache configuration file with xml-override attribute specified without default.
     */
    public final static String FILE_CFG_CACHE_NO_DEFAULT = "override/cache-config-nodefault.xml";

    /**
     * Operational override file.
     */
    public final static String FILE_CFG_OP = "override/operational-override.xml";
    }