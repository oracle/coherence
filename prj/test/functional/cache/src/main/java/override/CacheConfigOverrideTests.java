/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package override;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.CoherenceSession;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.RequestPolicyException;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.net.events.internal.InterceptorManager;

import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.run.xml.XmlHelper;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Enumeration;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import static org.hamcrest.CoreMatchers.is;
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

    @Test
    public void testCacheConfigOverrideCustomeNSHandler()
            throws Exception
        {
        try
            {
            System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE_NS_HANDLER);
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

    @Test
    public void testCacheConfigOverrideWithInterceptors()
            throws Exception
        {
        try
            {
            System.setProperty("coherence.cacheconfig.override",
                    "override/cache-config-override-interceptors.xml");

            AbstractFunctionalTest._startup();

            ExtensibleConfigurableCacheFactory.Dependencies deps =
                    ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance(
                            XmlHelper.loadFileOrResource(FILE_CFG_CACHE, null, null));
            ConfigurableCacheFactory ccf = new ExtensibleConfigurableCacheFactory(deps);

            NamedCache myCacheOverride = ccf.ensureCache("my-cache-interceptor", null);
            assertNotNull(myCacheOverride);

            Enumeration cacheNames = myCacheOverride.getCacheService().getCacheNames();
            assertTrue(cacheNames.hasMoreElements());
            assertEquals("my-cache-interceptor", cacheNames.nextElement());
            assertEquals("$SYS:DistributedCacheWithInterceptor",
                    myCacheOverride.getService().getInfo().getServiceName());

            InterceptorRegistry iRegistry = deps.getResourceRegistry().getResource(InterceptorRegistry.class);
            assertNotNull(iRegistry);

            EventInterceptor overrideInterceptor = iRegistry.getEventInterceptor(OverrideInterceptor.class.getName());
            assertNotNull(overrideInterceptor);
            }
        finally
            {
            System.clearProperty("coherence.cacheconfig.override");
            AbstractFunctionalTest._shutdown();
            }
        }

    @Test
    public void testCacheConfigOverrideInterceptorWithName()
            throws Exception
        {
        try
            {
            System.setProperty("coherence.cacheconfig.override",
                    "override/cache-config-override-interceptor-with-name.xml");

            Cluster cluster = AbstractFunctionalTest.startCluster();
            Eventually.assertDeferred(() -> cluster.isRunning(), is(true));

            ExtensibleConfigurableCacheFactory.Dependencies deps =
                    ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance(
                            XmlHelper.loadFileOrResource(FILE_CFG_CACHE_NO_DEFAULT, null, null));

            ConfigurableCacheFactory ccf = new ExtensibleConfigurableCacheFactory(deps);
            NamedCache myCacheOverride = ccf.ensureCache("my-cache-interceptor", null);
            assertNotNull(myCacheOverride);

            Enumeration cacheNames = myCacheOverride.getCacheService().getCacheNames();
            assertTrue(cacheNames.hasMoreElements());
            assertEquals("my-cache-interceptor", cacheNames.nextElement());
            assertEquals("$SYS:DistributedCacheWithInterceptor",
                    myCacheOverride.getService().getInfo().getServiceName());

            CacheService service = myCacheOverride.getCacheService();
            Eventually.assertDeferred(() -> service.isRunning(), is(true));

            InterceptorManager iManager = deps.getResourceRegistry().getResource(InterceptorManager.class);
            assertNotNull(iManager);

            InterceptorRegistry iRegistry = deps.getResourceRegistry().getResource(InterceptorRegistry.class);
            assertNotNull(iRegistry);

            // Get interceptor by name
            EventInterceptor testInterceptor = iRegistry.getEventInterceptor("test-interceptor");
            assertNotNull(testInterceptor);
            assertTrue(testInterceptor instanceof OverrideInterceptor);

            EventInterceptor baseInterceptor = iRegistry.getEventInterceptor(BaseInterceptor.class.getName());
            assertNotNull(baseInterceptor);

            EventInterceptor newInterceptor = iRegistry.getEventInterceptor(NewInterceptor.class.getName());
            assertNotNull(newInterceptor);
            }
        finally
            {
            System.clearProperty("coherence.cacheconfig.override");
            AbstractFunctionalTest._shutdown();
            }
        }

    @Test
    public void testCacheConfigOverrideWithInterceptorsOnly()
            throws Exception
        {
        try
            {
            Cluster cluster = AbstractFunctionalTest.startCluster();
            Eventually.assertDeferred(() -> cluster.isRunning(), is(true));

            ExtensibleConfigurableCacheFactory.Dependencies deps =
                    ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance(
                            XmlHelper.loadFileOrResource(FILE_CFG_CACHE_INTERCEPTORS, null, null));

            ConfigurableCacheFactory ccf = new ExtensibleConfigurableCacheFactory(deps);
            InterceptorManager iManager = deps.getResourceRegistry().getResource(InterceptorManager.class);
            assertNotNull(iManager);

            InterceptorRegistry iRegistry = deps.getResourceRegistry().getResource(InterceptorRegistry.class);
            assertNotNull(iRegistry);

            // Get interceptor by name
            EventInterceptor testInterceptor = iRegistry.getEventInterceptor("test-interceptor");
            assertNotNull(testInterceptor);
            assertTrue(testInterceptor instanceof OverrideInterceptor);

            EventInterceptor newInterceptor = iRegistry.getEventInterceptor("new-interceptor");
            assertNotNull(newInterceptor);
            assertTrue(newInterceptor instanceof NewInterceptor);

            ccf.activate();
            // assert that correct interceptor i.e OverrideInterceptor is already invoked by checking
            // cache "my-cache-interceptor" which can only exist and active if OverrideInterceptor is
            // fired when CCF is activated.
            assertTrue(ccf.isCacheActive("my-cache-interceptor", null));

            // Now assert the value in cache which is put when the interceptor was fired.
            NamedCache<String, String> myCacheInterceptor = ccf.ensureCache("my-cache-interceptor", null);
            assertEquals(testInterceptor.getClass().getName(), myCacheInterceptor.get("interceptor-name"));
            Enumeration cacheNames = myCacheInterceptor.getCacheService().getCacheNames();
            assertTrue(cacheNames.hasMoreElements());
            assertEquals("my-cache-interceptor", cacheNames.nextElement());
            assertEquals("$SYS:MyCacheService", myCacheInterceptor.getService().getInfo().getServiceName());

            CacheService service = myCacheInterceptor.getCacheService();
            Eventually.assertDeferred(() -> service.isRunning(), is(true));
            }
        finally
            {
            AbstractFunctionalTest._shutdown();
            }
        }

    @Test
    public void testCacheConfigWithNoOverrideValue()
            throws Exception
        {
        try
            {
            System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE_TEST);

            AbstractFunctionalTest._startup();

            NamedCache myCacheOverride = CacheFactory.getCache("test-1");
            assertNotNull(myCacheOverride);
            Enumeration cacheNames = myCacheOverride.getCacheService().getCacheNames();
            assertTrue(cacheNames.hasMoreElements());
            assertEquals("test-1", cacheNames.nextElement());
            assertEquals("TestService", myCacheOverride.getService().getInfo().getServiceName());
            }
        finally
            {
            System.clearProperty("coherence.cacheconfig");
            AbstractFunctionalTest._shutdown();
            }
        }

    @Test
    public void testCacheConfigOverrideTopics()
            throws Exception
        {
        try
            {
            System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE_TEST);
            System.setProperty("coherence.cacheconfig.override", "override/cache-config-override-topics.xml");

            AbstractFunctionalTest._startup();

            NamedCache myCachePrepend = CacheFactory.getCache("test-cache");
            assertNotNull(myCachePrepend);
            Enumeration cacheNames = myCachePrepend.getCacheService().getCacheNames();
            assertTrue(cacheNames.hasMoreElements());
            assertEquals("test-cache", cacheNames.nextElement());
            assertEquals("TestService", myCachePrepend.getService().getInfo().getServiceName());

            NamedTopic topic = new CoherenceSession().getTopic("topic-override");
            assertNotNull(topic);
            assertEquals("MyTopicOverrideService", topic.getService().getInfo().getServiceName());
            }
        finally
            {
            System.clearProperty("coherence.cacheconfig");
            AbstractFunctionalTest._shutdown();
            }
        }

    @Test
    public void testCacheConfigWithExistingTopicsMappingAndOverride()
            throws Exception
        {
        try
            {
            System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE_TOPICS);
            System.setProperty("coherence.cacheconfig.override", "override/cache-config-override-topics.xml");

            AbstractFunctionalTest._startup();

            NamedCache myCachePrepend = CacheFactory.getCache("test-cache");
            assertNotNull(myCachePrepend);
            Enumeration cacheNames = myCachePrepend.getCacheService().getCacheNames();
            assertTrue(cacheNames.hasMoreElements());
            assertEquals("test-cache", cacheNames.nextElement());
            assertEquals("TestService", myCachePrepend.getService().getInfo().getServiceName());

            NamedTopic topic = new CoherenceSession().getTopic("topic-override");
            assertNotNull(topic);
            assertEquals("MyTopicOverrideService", topic.getService().getInfo().getServiceName());
            }
        finally
            {
            System.clearProperty("coherence.cacheconfig");
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
     * Cache configuration file with xml-override attribute specified without default.
     */
    public final static String FILE_CFG_CACHE_NS_HANDLER = "override/cache-config-custom-ns-handler.xml";

    /**
     * Cache configuration file with an override file containing interceptors only.
     */
    public final static String FILE_CFG_CACHE_INTERCEPTORS = "override/cache-config-interceptors.xml";

    /**
     * Simple cache configuration file for test.
     */
    public final static String FILE_CFG_CACHE_TEST = "override/test-cache-config.xml";

    /**
     * coherence cache configuration with topics.
     */
    public final static String FILE_CFG_CACHE_TOPICS = "override/cache-config-with-topics.xml";

    /**
     * Operational override file.
     */
    public final static String FILE_CFG_OP = "override/operational-override.xml";
    }
