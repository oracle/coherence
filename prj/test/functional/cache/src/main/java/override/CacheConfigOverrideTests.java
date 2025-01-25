/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 * 
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package override;

import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.concurrent.Queues;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceSession;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedBlockingQueue;
import com.tangosol.net.NamedCache;
import com.tangosol.net.QueueService;
import com.tangosol.net.RequestPolicyException;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.net.events.internal.InterceptorManager;

import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.run.xml.XmlHelper;

import org.junit.Test;

import java.util.Enumeration;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * The {@link CacheConfigOverrideTests} class contains multiple tests for overriding the cache configuration.
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

    // ----- helper methods -------------------------------------------------

    public void setupTestProps()
        {
        System.setProperty(WellKnownAddress.PROPERTY, LocalHost.loopback().getAddress());
        System.setProperty(LocalHost.PROPERTY, LocalHost.loopback().getAddress());
        System.setProperty("test.unicast.port", String.valueOf(getAvailablePorts().next()));
        System.setProperty(IPv4Preferred.JAVA_NET_PREFER_IPV4_STACK, "true");

        // this test requires local storage to be enabled
        System.setProperty(LocalStorage.PROPERTY, "true");

        setupProps();
        }

    public void clearTestProps()
        {
        System.clearProperty(WellKnownAddress.PROPERTY);
        System.clearProperty(LocalHost.PROPERTY);
        System.clearProperty(IPv4Preferred.JAVA_NET_PREFER_IPV4_STACK);
        System.clearProperty(LocalStorage.PROPERTY);
        System.clearProperty("test.unicast.port");
        System.clearProperty("coherence.cacheconfig");
        System.clearProperty("coherence.cacheconfig.override");
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void testCacheConfigOverride()
        {
        setupTestProps();
        System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE);

        Cluster cluster = startCluster();

        try
            {
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
            if (cluster != null)
                {
                cluster.shutdown();

                Eventually.assertDeferred(() -> cluster.isRunning(), is(false));
                }

            clearTestProps();            
            }
        }

    @Test
    public void testCacheConfigOverrideSingleScheme()
            throws Exception
        {
        setupTestProps();
        System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE);
        System.setProperty("coherence.cacheconfig.override", "override/cache-config-override-two.xml");

        Cluster cluster = startCluster();

        try
            {
            NamedCache myCache = CacheFactory.getCache("my-cache-wildcard");
            assertNotNull(myCache);
            NamedCache myCacheTwo = CacheFactory.getCache("my-cache-two");
            assertNotNull(myCacheTwo);

            assertTrue(cluster.isRunning());
            assertEquals("cluster already exists", 1, cluster.getMemberSet().size());

            // Check for correct QuorumStatus for service "$SYS:MyCache" without write
            MBeanServer         serverJMX  = MBeanHelper.findMBeanServer();
            String              objectName = "Coherence:type=Service,name=\"$SYS:MyCacheService\",*";
            Set<ObjectInstance> mbeanObj   = serverJMX.queryMBeans(new ObjectName(objectName), null);
            assertTrue(mbeanObj.size() == 1);

            ObjectName serviceON    = new ObjectName(mbeanObj.iterator().next().getObjectName().toString());
            String     quorumStatus = (String) serverJMX.getAttribute(serviceON, "QuorumStatus");
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
            assertEquals(mbeanObj.size(), 1);

            serviceON    = new ObjectName(mbeanObj.iterator().next().getObjectName().toString());
            quorumStatus = (String) serverJMX.getAttribute(serviceON, "QuorumStatus");
            assertNotNull(quorumStatus);
            assertEquals(quorumStatus, "allowed-actions=distribution, restore, read, write, recover");

            myCacheTwo.put("1", "ONE");
            assertEquals("ONE", myCacheTwo.get("1"));
            }
        finally
            {
            if (cluster != null)
                {
                cluster.shutdown();

                Eventually.assertDeferred(() -> cluster.isRunning(), is(false));
                }

            clearTestProps();
            }
        }

    @Test
    public void testCacheConfigOverrideMultipeSchemes()
            throws Exception
        {
        setupTestProps();
        System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE);
        System.setProperty("coherence.cacheconfig.override", "override/cache-config-override-prepend.xml");

        Cluster cluster = startCluster();

        try
            {
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
            MBeanServer         serverJMX  = MBeanHelper.findMBeanServer();
            String              objectName = "Coherence:type=Service,name=\"$SYS:MyCacheService\",*";
            Set<ObjectInstance> mbeanObj   = serverJMX.queryMBeans(new ObjectName(objectName), null);
            assertTrue(mbeanObj.size() == 1);

            ObjectName serviceON    = new ObjectName(mbeanObj.iterator().next().getObjectName().toString());
            String     quorumStatus = (String) serverJMX.getAttribute(serviceON, "QuorumStatus");
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
            if (cluster != null)
                {
                cluster.shutdown();

                Eventually.assertDeferred(() -> cluster.isRunning(), is(false));
                }

            clearTestProps();
            }
        }

    @Test
    public void testAddProxySchemeUsingCacheOverride()
            throws Exception
        {
        setupTestProps();
        System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE);
        System.setProperty("coherence.cacheconfig.override", "override/cache-config-override-proxy.xml");

        Cluster cluster = startCluster();

        try
            {
            cluster.ensureService("override:ProxyService", "Proxy").start();

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
            MBeanServer         serverJMX  = MBeanHelper.findMBeanServer();
            String              objectName = "Coherence:type=Service,name=\"override:ProxyService\",*";
            Set<ObjectInstance> mbeanObj   = serverJMX.queryMBeans(new ObjectName(objectName), null);
            assertTrue(mbeanObj.size() == 1);
            }
        finally
            {
            if (cluster != null)
                {
                cluster.shutdown();

                Eventually.assertDeferred(() -> cluster.isRunning(), is(false));
                }

            clearTestProps();
            }
        }

    @Test
    public void testCacheConfigOverrideSysPropNoDefault()
            throws Exception
        {
        setupTestProps();
        System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE_NO_DEFAULT);
        System.setProperty("coherence.cacheconfig.override", "override/cache-config-override-ns.xml");

        Cluster cluster = startCluster();

        try
            {
            NamedCache myCacheOverride = CacheFactory.getCache("my-cache-override");
            assertNotNull(myCacheOverride);
            Enumeration cacheNames = myCacheOverride.getCacheService().getCacheNames();
            assertTrue(cacheNames.hasMoreElements());
            assertEquals("my-cache-override", cacheNames.nextElement());
            assertEquals("$SYS:MyCacheService", myCacheOverride.getService().getInfo().getServiceName());

            // Check for correct QuorumStatus for service "$SYS:MyCacheService" with write
            MBeanServer         serverJMX  = MBeanHelper.findMBeanServer();
            String              objectName = "Coherence:type=Service,name=\"$SYS:MyCacheService\",*";
            Set<ObjectInstance> mbeanObj   = serverJMX.queryMBeans(new ObjectName(objectName), null);
            assertTrue(mbeanObj.size() == 1);

            ObjectName serviceON    = new ObjectName(mbeanObj.iterator().next().getObjectName().toString());
            String     quorumStatus = (String) serverJMX.getAttribute(serviceON, "QuorumStatus");
            assertNotNull(quorumStatus);
            assertEquals(quorumStatus, "allowed-actions=distribution, restore, read, write, recover");
            }
        finally
            {
            if (cluster != null)
                {
                cluster.shutdown();

                Eventually.assertDeferred(() -> cluster.isRunning(), is(false));
                }

            clearTestProps();
            }
        }

    @Test
    public void testCacheConfigOverrideCustomeNSHandler()
            throws Exception
        {
        setupTestProps();
        System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE_NS_HANDLER);
        System.setProperty("coherence.cacheconfig.override", "override/cache-config-override-ns.xml");

        Cluster cluster = startCluster();

        try
            {
            NamedCache myCacheOverride = CacheFactory.getCache("my-cache-override");
            assertNotNull(myCacheOverride);
            Enumeration cacheNames = myCacheOverride.getCacheService().getCacheNames();
            assertTrue(cacheNames.hasMoreElements());
            assertEquals("my-cache-override", cacheNames.nextElement());
            assertEquals("$SYS:MyCacheService", myCacheOverride.getService().getInfo().getServiceName());

            // Check for correct QuorumStatus for service "$SYS:MyCacheService" with write
            MBeanServer         serverJMX  = MBeanHelper.findMBeanServer();
            String              objectName = "Coherence:type=Service,name=\"$SYS:MyCacheService\",*";
            Set<ObjectInstance> mbeanObj   = serverJMX.queryMBeans(new ObjectName(objectName), null);
            assertTrue(mbeanObj.size() == 1);

            ObjectName serviceON    = new ObjectName(mbeanObj.iterator().next().getObjectName().toString());
            String     quorumStatus = (String) serverJMX.getAttribute(serviceON, "QuorumStatus");
            assertNotNull(quorumStatus);
            assertEquals(quorumStatus, "allowed-actions=distribution, restore, read, write, recover");
            }
        finally
            {
            if (cluster != null)
                {
                cluster.shutdown();

                Eventually.assertDeferred(() -> cluster.isRunning(), is(false));
                }

            clearTestProps();
            }
        }

    @Test
    public void testCacheConfigOverrideWithInterceptors()
            throws Exception
        {
        setupTestProps();
        System.setProperty("coherence.cacheconfig.override", "override/cache-config-override-interceptors.xml");

        Cluster cluster = startCluster();

        try
            {
            ExtensibleConfigurableCacheFactory.Dependencies deps = ExtensibleConfigurableCacheFactory.DependenciesHelper
                    .newInstance(XmlHelper.loadFileOrResource(FILE_CFG_CACHE, null, null));
            ConfigurableCacheFactory                        ccf  = new ExtensibleConfigurableCacheFactory(deps);

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
            if (cluster != null)
                {
                cluster.shutdown();

                Eventually.assertDeferred(() -> cluster.isRunning(), is(false));
                }

            clearTestProps();
            }
        }

    @Test
    public void testCacheConfigOverrideInterceptorWithName()
            throws Exception
        {
        setupTestProps();
        System.setProperty("coherence.cacheconfig.override", "override/cache-config-override-interceptor-with-name.xml");

        Cluster cluster = startCluster();

        try
            {
            Eventually.assertDeferred(() -> cluster.isRunning(), is(true));

            ExtensibleConfigurableCacheFactory.Dependencies deps = ExtensibleConfigurableCacheFactory.DependenciesHelper
                    .newInstance(XmlHelper.loadFileOrResource(FILE_CFG_CACHE_NO_DEFAULT, null, null));

            ConfigurableCacheFactory ccf             = new ExtensibleConfigurableCacheFactory(deps);
            NamedCache               myCacheOverride = ccf.ensureCache("my-cache-interceptor", null);
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
            if (cluster != null)
                {
                cluster.shutdown();

                Eventually.assertDeferred(() -> cluster.isRunning(), is(false));
                }

            clearTestProps();
            }
        }

    @Test
    public void testCacheConfigOverrideWithInterceptorsOnly()
            throws Exception
        {
        setupTestProps();

        Cluster cluster = startCluster();

        try
            {
            Eventually.assertDeferred(() -> cluster.isRunning(), is(true));

            ExtensibleConfigurableCacheFactory.Dependencies deps = ExtensibleConfigurableCacheFactory.DependenciesHelper
                    .newInstance(XmlHelper.loadFileOrResource(FILE_CFG_CACHE_INTERCEPTORS, null, null));

            ConfigurableCacheFactory ccf      = new ExtensibleConfigurableCacheFactory(deps);
            InterceptorManager       iManager = deps.getResourceRegistry().getResource(InterceptorManager.class);
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
            // assert that correct interceptor i.e OverrideInterceptor is already invoked by
            // checking cache "my-cache-interceptor" which can only exist and active if
            // OverrideInterceptor is fired when CCF is activated.
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
            if (cluster != null)
                {
                cluster.shutdown();

                Eventually.assertDeferred(() -> cluster.isRunning(), is(false));
                }

            clearTestProps();
            }
        }

    @Test
    public void testCacheConfigWithNoOverrideValue()
            throws Exception
        {
        setupTestProps();
        System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE_TEST);

        Cluster cluster = startCluster();

        try
            {
            NamedCache myCacheOverride = CacheFactory.getCache("test-1");
            assertNotNull(myCacheOverride);
            Enumeration cacheNames = myCacheOverride.getCacheService().getCacheNames();
            assertTrue(cacheNames.hasMoreElements());
            assertEquals("test-1", cacheNames.nextElement());
            assertEquals("TestService", myCacheOverride.getService().getInfo().getServiceName());
            }
        finally
            {
            if (cluster != null)
                {
                cluster.shutdown();

                Eventually.assertDeferred(() -> cluster.isRunning(), is(false));
                }

            clearTestProps();
            }
        }

    @Test
    public void testCacheConfigOverrideTopics()
            throws Exception
        {
        setupTestProps();
        System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE_TEST);
        System.setProperty("coherence.cacheconfig.override", "override/cache-config-override-topics.xml");

        Cluster cluster = startCluster();

        try
            {
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
            if (cluster != null)
                {
                cluster.shutdown();

                Eventually.assertDeferred(() -> cluster.isRunning(), is(false));
                }

            clearTestProps();
            }
        }

    @Test
    public void testCacheConfigWithExistingTopicsMappingAndOverride()
            throws Exception
        {
        setupTestProps();
        System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE_TOPICS);
        System.setProperty("coherence.cacheconfig.override", "override/cache-config-override-topics.xml");

        Cluster cluster = startCluster();

        try
            {
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
            if (cluster != null)
                {
                cluster.shutdown();

                Eventually.assertDeferred(() -> cluster.isRunning(), is(false));
                }

            clearTestProps();
            }
        }

    @Test
    public void testConcurrentOverride()
            throws Exception
        {
        setupTestProps();
        System.setProperty("coherence.concurrent.cacheconfig.override", "override/test-override.xml");

        try (Coherence coherence = Coherence.clusterMember().start().join())
            {
            try (NamedBlockingQueue<Integer> q = Queues.pagedQueue("elastic-numbers"))
                {
                QueueService qs = q.getService();
                assertEquals("$SYS:OverrideQueue", qs.getInfo().getServiceName());
                assertEquals("DistributedCache", qs.getInfo().getServiceType());

                for (int i = 0; i < 10; i++)
                    {
                    q.add(i);
                    }

                Integer e = q.poll();
                while (e != null)
                    {
                    Thread.sleep(1000L);
                    System.out.println(e);
                    e = q.poll();
                    }
                }
            }
        finally
            {
            clearTestProps();
            System.clearProperty("coherence.concurrent.cacheconfig.override");
            }
        }

    @Test
    public void testConcurrentOverrideChangeBackingMap()
            throws Exception
        {
        setupTestProps();
        System.setProperty("coherence.concurrent.cacheconfig.override", "override/concurrent-override.xml");

        try (Coherence coherence = Coherence.clusterMember().start().join())
            {
            try (NamedBlockingQueue<Integer> q = Queues.pagedQueue("elastic-numbers"))
                {
                QueueService qs = q.getService();
                assertEquals("$SYS:ConcurrentQueueService", qs.getInfo().getServiceName());
                assertEquals("DistributedCache", qs.getInfo().getServiceType());

                for (int i = 0; i < 10; i++)
                    {
                    q.add(i);
                    }

                Integer e = q.poll();
                while (e != null)
                    {
                    Thread.sleep(1000L);
                    System.out.println(e);
                    e = q.poll();
                    }
                }
            }
        finally
            {
            clearTestProps();
            System.clearProperty("coherence.concurrent.cacheconfig.override");
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
