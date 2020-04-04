/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package jmx;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.InvocationService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.annotation.TransactionEvents;
import com.tangosol.net.events.partition.TransactionEvent;

import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.Registry;

import com.tangosol.coherence.component.util.SafeCluster;
import com.tangosol.coherence.component.net.management.Gateway;

import com.tangosol.util.BinaryEntry;

import common.AbstractFunctionalTest;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.MXBean;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.beans.ConstructorProperties;

import java.io.Serializable;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
* A collection of functional tests for Coherence JMX framework.
*
* @author gg 2010.07.22
*/
public class JmxTests
        extends AbstractFunctionalTest implements Serializable
    {

    // ----- constructors ---------------------------------------------------

    public JmxTests()
        {
        super(FILE_CFG_CACHE);
        }

    // ----- test methods ---------------------------------------------------

    @BeforeClass
    public static void _startup()
        {
        System.setProperty("tangosol.coherence.role", "main");
        System.setProperty("tangosol.coherence.log.level", "3");

        // we will control the startup manually
        }

    @AfterClass
    public static void _shutdown()
        {
        // we will control the shutdown manually
        }

    @After
    public void cleanup()
        {
        AbstractFunctionalTest._shutdown();
        }

    /**
     * Test the EventCount and EventBacklog attributes in the ServiceMBean
     */
    @Test
    public void testEventAttributes()
        {
        Properties propsMain = new Properties();
        propsMain.put("tangosol.coherence.management", "all");
        propsMain.put("tangosol.coherence.management.remote", "true");
        System.setProperty("tangosol.coherence.distributed.localstorage","true");
        System.getProperties().putAll(propsMain);

        AbstractFunctionalTest._startup();
        try
            {
            Cluster cluster = CacheFactory.getCluster();
            assertTrue(cluster.isRunning());
            assertEquals("cluster already exists", 1, cluster.getMemberSet().size());

            NamedCache   cache    = getNamedCache("events");
            CacheService service  = cache.getCacheService();
            String       sService = service.getInfo().getServiceName();
            int          iNodeId  = cluster.getLocalMember().getId();
            int          COUNT    = 100;

            for (int i = 1; i <= COUNT ;i++)
                {
                cache.put(i, i);
                }
            // there is an EventInterceptor registered that sleeps for 1s allowing
            // the assertion below to be valid if asked within that second

            long cEvents = (Long) getEventAttribute(sService, iNodeId, "EventCount");
            int cBacklog = (Integer) getEventAttribute(sService, iNodeId, "EventBacklog");

            // there is a slight chance that the events are processed extremely quickly
            if (cBacklog == 0)
                {
                assertEquals(COUNT, cEvents);
                }

            Eventually.assertThat(invoking(this).getEventAttribute(sService, iNodeId, "EventBacklog"), is(new Integer(0)));
            Eventually.assertThat(invoking(this).getEventAttribute(sService, iNodeId, "EventCount"), is(new Long(COUNT)));
            }
        finally
            {
            AbstractFunctionalTest._shutdown();
            }
        }

    /**
    * Test statusHA display under simple partition strategy, COH-10345
    */
    @Test
    public void testStatusHAPAS()
        throws JMException
        {
        try
            {
            testStatusHAHelper("PAS-cache", "ENDANGERED", "NODE-SAFE", "MACHINE-SAFE", "RACK-SAFE", "SITE-SAFE");
            }
        finally
            {
            AbstractFunctionalTest._shutdown();
            }
        }

    /**
    * Helper function for testStatusHAPAS()
    */
    public void testStatusHAHelper(String sStrategy, String sEndangered, String sNode, String sMachine, String sRack, String sSite)
        {
        System.setProperty("tangosol.coherence.log.level", "3");
        System.setProperty("tangosol.coherence.management", "all");
        System.setProperty("tangosol.coherence.distributed.localstorage","false");
        System.setProperty("tangosol.coherence.rack", "rack-test");
        System.setProperty("tangosol.coherence.site", "site-test");
        System.setProperty("tangosol.coherence.machine", "machine-test");

        AbstractFunctionalTest._startup();

        NamedCache   cache    = getNamedCache(sStrategy);
        CacheService service  = cache.getCacheService();
        String       sService = service.getInfo().getServiceName();

        Cluster cluster = CacheFactory.getCluster();
        assertTrue(cluster.isRunning());
        assertEquals("cluster already exists", 1, cluster.getMemberSet().size());

        int iNodeId = cluster.getLocalMember().getId();

        checkCacheStatusHA(sService, iNodeId, sEndangered);

        CoherenceClusterMember member1 = startCacheServerWithIdentity("server1-" + sStrategy, "site1", "rack1", "machine1");
        checkCacheStatusHA(sService, iNodeId, sEndangered);

        CoherenceClusterMember member2 = startCacheServerWithIdentity("server2-" + sStrategy, "site2", "rack2", "machine2");
        checkCacheStatusHA(sService, iNodeId, sSite);
        stopCacheServer("server2-" + sStrategy);
        member2.waitFor();

        CoherenceClusterMember member3 = startCacheServerWithIdentity("server3-" + sStrategy, "site1", "rack2", "machine3");
        checkCacheStatusHA(sService, iNodeId, sRack);
        stopCacheServer("server3-" + sStrategy);
        member3.waitFor();

        CoherenceClusterMember member4 = startCacheServerWithIdentity("server4-" + sStrategy, "site1", "rack1", "machine4");
        checkCacheStatusHA(sService, iNodeId, sMachine);
        stopCacheServer("server4-" + sStrategy);
        member4.waitFor();

        CoherenceClusterMember member5 = startCacheServerWithIdentity("server5-" + sStrategy, "site1", "rack1", "machine1");
        checkCacheStatusHA(sService, iNodeId, sNode);
        stopCacheServer("server5-" + sStrategy);
        member5.waitFor();

        stopCacheServer("server1-" + sStrategy);
        member1.waitFor();
        }

    /**
    * This node is "management=local-only, remote=false"
    */
    @Test
    public void testLocalOnlyFalse()
            throws JMException
        {
        Properties propsMain = new Properties();
        propsMain.put("coherence.management", "local-only");
        propsMain.put("coherence.management.remote", "false");
        System.getProperties().putAll(propsMain);
        Properties propsSecond = new Properties(propsMain);

        AbstractFunctionalTest._startup();
        try
            {
            Cluster cluster = CacheFactory.getCluster();
            assertTrue(cluster.isRunning());
            assertEquals("cluster already exists", 1, cluster.getMemberSet().size());

            Registry registry = cluster.getManagement();
            assertTrue("JMX is disabled", registry != null);

            MBeanServer serverJMX = MBeanHelper.findMBeanServer();
            String      sDomain   = registry.getDomainName();

            String sCluster = sDomain + ":" + Registry.CLUSTER_TYPE;
            assertTrue("Should be registered: " + sCluster, serverJMX.isRegistered(new ObjectName(sCluster)));

            String sNode1 = sDomain + ":" + Registry.NODE_TYPE + ",nodeId=" + cluster.getLocalMember().getId();
            assertTrue("Should be registered: " + sNode1, serverJMX.isRegistered(new ObjectName(sNode1)));

            // local-only, false <-> local-only, false
            CoherenceClusterMember member = startCacheServer("testLocalOnlyFalse1", PROJECT, null, propsSecond);
            try
                {
                Member member2 = findCacheServer("testLocalOnlyFalse1");
                assertTrue("Failed to start the testLocalOnlyFalse1 node", member2 != null);
                String sNode2 = sDomain + ":" + Registry.NODE_TYPE + ",nodeId=" + member2.getId();
                assertFalse("Should not be registered: " + sNode2, serverJMX.isRegistered(new ObjectName(sNode2)));
                }
            finally
                {
                stopCacheServer("testLocalOnlyFalse1", true);
                member.waitFor();
                assertTrue("Failed to stop", findCacheServer("testLocalOnlyFalse1") == null);
                }

            // local-only, false <-> all, true
            propsSecond.put("coherence.management", "all");
            propsSecond.put("coherence.management.remote", "true");
            member = startCacheServer("testLocalOnlyFalse2", PROJECT, null, propsSecond);
            try
                {
                Member member2 = findCacheServer("testLocalOnlyFalse2");
                assertTrue("Failed to start the testLocalOnlyFalse2 node", member2 != null);
                String sNode2 = sDomain + ":" + Registry.NODE_TYPE + ",nodeId=" + member2.getId();
                assertFalse("Should not be registered: " + sNode2, serverJMX.isRegistered(new ObjectName(sNode2)));

                Member memberThis = testCoh3500(cluster, member);

                sNode1 = registry.getDomainName() + ":" +
                    Registry.NODE_TYPE + ",nodeId=" + memberThis.getId();
                assertTrue("Should be registered after restart: " + sNode1,
                    serverJMX.isRegistered(new ObjectName(sNode1)));
                }
            finally
                {
                stopCacheServer("testLocalOnlyFalse2", true);
                member.waitFor();
                assertTrue("Failed to stop", findCacheServer("testLocalOnlyFalse2") == null);
                }
            }
        finally
            {
            AbstractFunctionalTest._shutdown();
            }
        }

    /**
    * This node is "management=local-only, remote=true"
    */
    @Test
    public void testLocalOnlyTrue()
            throws JMException
        {
        Properties propsMain = new Properties();
        propsMain.put("tangosol.coherence.management", "local-only");
        propsMain.put("tangosol.coherence.management.remote", "true");
        System.getProperties().putAll(propsMain);
        Properties propsSecond = new Properties(propsMain);

        AbstractFunctionalTest._startup();
        try
            {
            Cluster cluster = CacheFactory.getCluster();
            assertTrue(cluster.isRunning());
            assertEquals("cluster already exists", 1, cluster.getMemberSet().size());

            Registry registry = cluster.getManagement();
            assertTrue("JMX is disabled", registry != null);

            MBeanServer serverJMX = MBeanHelper.findMBeanServer();
            String      sDomain   = registry.getDomainName();

            String sCluster = sDomain + ":" + Registry.CLUSTER_TYPE;
            assertTrue("Should be registered: " + sCluster, serverJMX.isRegistered(new ObjectName(sCluster)));

            String sNode1 = sDomain + ":" + Registry.NODE_TYPE + ",nodeId=" + cluster.getLocalMember().getId();
            assertTrue("Should be registered: " + sNode1, serverJMX.isRegistered(new ObjectName(sNode1)));

            // local-only, true <-> local-only, true
            CoherenceClusterMember member = startCacheServer("testLocalOnlyTrue1", PROJECT, null, propsSecond);
                try
                {
                Member member2 = findCacheServer("testLocalOnlyTrue1");
                assertTrue("Failed to start the testLocalOnlyTrue1 node", member2 != null);
                String sNode2 = sDomain + ":" + Registry.NODE_TYPE + ",nodeId=" + member2.getId();
                assertFalse("Should not be registered: " + sNode2, serverJMX.isRegistered(new ObjectName(sNode2)));
                }
            finally
                {
                stopCacheServer("testLocalOnlyTrue1", true);
                member.waitFor();
                assertTrue("Failed to stop", findCacheServer("testLocalOnlyTrue1") == null);
                }

            // local-only, true <-> all, true
            propsSecond.put("tangosol.coherence.management", "all");
            propsSecond.put("tangosol.coherence.management.remote", "true");
            member = startCacheServer("testLocalOnlyTrue2", PROJECT, null, propsSecond);
            try
                {
                Member member2 = findCacheServer("testLocalOnlyTrue2");
                assertTrue("Failed to start the testLocalOnlyTrue2 node", member2 != null);
                String sNode2 = sDomain + ":" + Registry.NODE_TYPE + ",nodeId=" + member2.getId();
                assertFalse("Should not be registered: " + sNode2, serverJMX.isRegistered(new ObjectName(sNode2)));

                Member memberThis = testCoh3500(cluster, member);

                sNode1 = registry.getDomainName() + ":" +
                    Registry.NODE_TYPE + ",nodeId=" + memberThis.getId();
                assertTrue("Should be registered after restart: " + sNode1,
                    serverJMX.isRegistered(new ObjectName(sNode1)));
                }
            finally
                {
                stopCacheServer("testLocalOnlyTrue2");
                member.waitFor();
                assertTrue("Failed to stop", findCacheServer("testLocalOnlyTrue2") == null);
                }
            }
        finally
            {
            AbstractFunctionalTest._shutdown();
            }
        }


    /**
    * This node is "management=remote-only, remote=false"
    */
    @Test
    public void testRemoteOnlyFalse()
            throws JMException
        {
        Properties propsMain = new Properties();
        propsMain.put("coherence.management", "remote-only");
        propsMain.put("coherence.management.remote", "false");
        System.getProperties().putAll(propsMain);
        Properties propsSecond = new Properties(propsMain);

        AbstractFunctionalTest._startup();
        try
            {
            Cluster cluster = CacheFactory.getCluster();
            assertTrue(cluster.isRunning());
            assertEquals("cluster already exists", 1, cluster.getMemberSet().size());

            Registry registry = cluster.getManagement();
            assertTrue("JMX is disabled", registry != null);

            MBeanServer serverJMX = MBeanHelper.findMBeanServer();
            String      sDomain   = registry.getDomainName();

            assertEquals("Invalid domain name for " + registry, "Coherence", sDomain);

            String sCluster = sDomain + ":" + Registry.CLUSTER_TYPE;
            assertTrue("Should be registered: " + sCluster, serverJMX.isRegistered(new ObjectName(sCluster)));

            String sNode1 = sDomain + ":" + Registry.NODE_TYPE + ",nodeId=" + cluster.getLocalMember().getId();
            assertFalse("Should not be registered: " + sNode1, serverJMX.isRegistered(new ObjectName(sNode1)));

            // remote-only, false <-> remote-only, false
            CoherenceClusterMember member = startCacheServer("testRemoteOnlyFalse1", PROJECT, null, propsSecond);
            try
                {
                Member member2 = findCacheServer("testRemoteOnlyFalse1");
                assertTrue("Failed to start the testRemoteOnlyFalse1 node", member2 != null);
                String sNode2 = sDomain + ":" + Registry.NODE_TYPE + ",nodeId=" + member2.getId();
                assertFalse("Should not be registered: " + sNode2, serverJMX.isRegistered(new ObjectName(sNode2)));
                }
            finally
                {
                stopCacheServer("testRemoteOnlyFalse1");
                member.waitFor();
                assertTrue("Failed to stop", findCacheServer("testRemoteOnlyFalse1") == null);
                }

            // remote-only, false <-> none, true
            propsSecond.put("coherence.management", "none");
            propsSecond.put("coherence.management.remote", "true");
            member = startCacheServer("testRemoteOnlyFalse2", PROJECT, null, propsSecond);
            try
                {
                Member member2 = findCacheServer("testRemoteOnlyFalse2");
                assertTrue("Failed to start the testRemoteOnlyFalse2 node", member2 != null);
                String sNode2 = sDomain + ":" + Registry.NODE_TYPE + ",nodeId=" + member2.getId();
                Eventually.assertThat(invoking(this).isMBeanRegistered(serverJMX, new ObjectName(sNode2)), is(true));

                testCoh3500(cluster, member);

                // wait for the remote MBeans registration
                Eventually.assertThat(invoking(this).isMBeanRegistered(serverJMX, new ObjectName(sNode2)), is(true));
                }
            finally
                {
                stopCacheServer("testRemoteOnlyFalse2");
                member.waitFor();
                assertTrue("Failed to stop", findCacheServer("testRemoteOnlyFalse2") == null);
                }

            // remote-only, false <-> all, true
            propsSecond.put("coherence.management", "all");
            propsSecond.put("coherence.management.remote", "true");
            member = startCacheServer("testRemoteOnlyFalse3", PROJECT, null, propsSecond);
            try
                {
                Member member2 = findCacheServer("testRemoteOnlyFalse3");
                assertTrue("Failed to start the testRemoteOnlyFalse3 node", member2 != null);
                String sNode2 = sDomain + ":" + Registry.NODE_TYPE + ",nodeId=" + member2.getId();

                // wait for the remote MBeans registration
                Eventually.assertThat(invoking(this).isMBeanRegistered(serverJMX, new ObjectName(sNode2)), is(true));

                testCoh3500(cluster, member);
                }
            finally
                {
                stopCacheServer("testRemoteOnlyFalse3");
                member.waitFor();
                assertTrue("Failed to stop", findCacheServer("testRemoteOnlyFalse3") == null);
                }
            }
        finally
            {
            AbstractFunctionalTest._shutdown();
            }
        }

    /**
    * This node is "management=all, remote=false"
    */
    @Test
    public void testAllFalse()
            throws JMException
        {
        Properties propsMain = new Properties();
        propsMain.put("tangosol.coherence.management", "all");
        propsMain.put("tangosol.coherence.management.remote", "false");
        testAll(propsMain);
        }

    /**
    * This node is "management=all, remote=true"
    */
    @Test
    public void testAllTrue()
            throws JMException
        {
        Properties propsMain = new Properties();
        propsMain.put("tangosol.coherence.management", "all");
        propsMain.put("tangosol.coherence.management.remote", "true");
        testAll(propsMain);
        }

    private void testAll(Properties propsMain)
            throws JMException
        {
        System.getProperties().putAll(propsMain);
        Properties propsSecond = new Properties(propsMain);

        AbstractFunctionalTest._startup();
        try
            {
            Cluster cluster = CacheFactory.getCluster();
            assertTrue(cluster.isRunning());
            assertEquals("cluster already exists", 1, cluster.getMemberSet().size());

            Registry registry = cluster.getManagement();
            assertTrue("JMX is disabled", registry != null);

            MBeanServer serverJMX = MBeanHelper.findMBeanServer();
            String      sDomain   = registry.getDomainName();

            String sCluster = sDomain + ":" + Registry.CLUSTER_TYPE;
            assertTrue("Should be registered: " + sCluster, serverJMX.isRegistered(new ObjectName(sCluster)));

            String sNode1 = sDomain + ":" + Registry.NODE_TYPE + ",nodeId=" + cluster.getLocalMember().getId();
            assertTrue("Should be registered: " + sNode1, serverJMX.isRegistered(new ObjectName(sNode1)));

            // all, x <-> all, true
            propsSecond.put("tangosol.coherence.management.remote", "true");
            CoherenceClusterMember member = startCacheServer("testAll1", PROJECT, null, propsSecond);
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(2));

            try
                {
                Member member2 = findCacheServer("testAll1");
                assertTrue("Failed to start the testAll1 node", member2 != null);
                String sNode2 = sDomain + ":" + Registry.NODE_TYPE + ",nodeId=" + member2.getId();
                Eventually.assertThat(invoking(this).isMBeanRegistered(serverJMX, new ObjectName(sNode2)), is(true));
                }
            finally
                {
                stopCacheServer("testAll1");
                member.waitFor();
                Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));
                }

            // all, x <-> none, true
            propsSecond.put("tangosol.coherence.management", "none");
            propsSecond.put("tangosol.coherence.management.remote", "true");
            member = startCacheServer("testAll2", PROJECT, null, propsSecond);
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(2));

            try
                {
                Member member2 = findCacheServer("testAll2");
                assertTrue("Failed to start the testAll2 node", member2 != null);
                String sNode2 = sDomain + ":" + Registry.NODE_TYPE + ",nodeId=" + member2.getId();
                assertTrue("Should be registered: " + sNode2, serverJMX.isRegistered(new ObjectName(sNode2)));

                Member memberThis = testCoh3500(cluster, member);

                sNode1 = sDomain + ":" + Registry.NODE_TYPE +
                    ",nodeId=" + memberThis.getId();
                assertTrue("Should be registered after restart: " + sNode1,
                        serverJMX.isRegistered(new ObjectName(sNode1)));

                // wait for remote mbean registrations
                Eventually.assertThat(invoking(this).isMBeanRegistered(serverJMX, new ObjectName(sNode2)), is(true));
                }
            finally
                {
                stopCacheServer("testAll2");
                member.waitFor();
                Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));
                }
            }
        finally
            {
            AbstractFunctionalTest._shutdown();
            }
        }

    public boolean isMBeanRegistered(MBeanServer serverJMX, ObjectName objectName)
        {
        return serverJMX.isRegistered(objectName);
        }

    /**
    * Test described standard MBeans, COH-4200.
    */
    @Test
    public void testDescribedMBeans()
            throws MalformedObjectNameException, IntrospectionException, InstanceNotFoundException,
                ReflectionException
        {
        Properties propsMain = new Properties();
        propsMain.put("tangosol.coherence.management", "all");
        propsMain.put("tangosol.coherence.management.remote", "true");
        System.getProperties().putAll(propsMain);

        AbstractFunctionalTest._startup();
        try
            {
            Cluster  cluster  = CacheFactory.ensureCluster();
            Registry registry = cluster.getManagement();

            // register our custom annotated standard MBean
            registry.register("com.example:type=Test", new Described());

            // connect to local MBeanServer to retrieve info for the MBean
            MBeanServer serverJMX = MBeanHelper.findMBeanServer();
            ObjectName  mbeanName = new ObjectName("com.example:type=Test");
            MBeanInfo   info      = serverJMX.getMBeanInfo(mbeanName);

            // retrieve and check the description
            assertEquals(DescribedMBean.MBEAN_DESCRIPTION, info.getDescription());

            // retrieve and check attribute description
            MBeanAttributeInfo[] attributes = info.getAttributes();
            for (MBeanAttributeInfo attr : attributes)
                {
                if (attr.getName().equals("Count"))
                    {
                    assertEquals(DescribedMBean.ATTR_DESCRIPTION, attr.getDescription());
                    }
                }

            // retrieve and check operation description
            MBeanOperationInfo[] operations = info.getOperations();
            for (MBeanOperationInfo oper : operations)
                {
                if (oper.getName().equals("startServer"))
                    {
                    assertEquals(DescribedMBean.OPER_DESCRIPTION, oper.getDescription());
                    }
                }
            }
        finally
            {
            AbstractFunctionalTest._shutdown();
            }
        }

    /**
     * COH-3276. To test MBean, MXBean and @MXBean
     */
    @Test
    public void testMBeansAndMXBeans()
        {
        Properties propsMain = new Properties();
        propsMain.put("tangosol.coherence.management", "all");
        propsMain.put("tangosol.coherence.management.remote", "true");
        System.getProperties().putAll(propsMain);

        AbstractFunctionalTest._startup();
        try
            {
            Cluster     cluster = CacheFactory.ensureCluster();
            Registry   registry = cluster.getManagement();
            Queue<String> queue = new ArrayBlockingQueue<>(10);

            queue.add("Request-1");
            queue.add("Request-2");
            queue.add("Request-3");

            // Ensure that NotCompliantMBeanException does not occur in each case

            // register MBean
            QueueSampler1 mxbean1 = new QueueSampler1(queue);
            registry.register("jmx:type=QueueSampler1", mxbean1);

            // register MXBean
            QueueSampler2 mxbean2 = new QueueSampler2(queue);
            registry.register("jmx:type=QueueSampler2", mxbean2);

            // register @MXBean
            QueueSampler3 mxbean3 = new QueueSampler3(queue);
            registry.register("jmx:type=QueueSampler3", mxbean3);

            }
        catch (Exception e)
            {
            fail(e.getMessage());
            }
        finally
            {
            AbstractFunctionalTest._shutdown();
            }
        }

    /**
     * Test QuorumStatus attribute value of Service MBean when the service is suspended
     */
     @Test
     public void testQuorumStatusOfSuspendedService()
         {
         Properties propsClient = new Properties();
         propsClient.put("tangosol.coherence.management", "all");
         propsClient.put("tangosol.coherence.distributed.localstorage", "false");
         propsClient.put("tangosol.coherence.management.remote", "true");
         System.getProperties().putAll(propsClient);

         Properties propsServer = new Properties();
         propsServer.put("tangosol.coherence.management", "all");
         propsServer.put("tangosol.coherence.distributed.localstorage", "true");
         propsServer.put("tangosol.coherence.management.remote", "true");

         try
             {
             //start managed node first
             CoherenceClusterMember clusterMember = startCacheServer("ManagedNode", PROJECT, null, propsServer);
             //start client non-managed node
             Cluster                cluster       = CacheFactory.ensureCluster();

             Eventually.assertThat(invoking(clusterMember).getClusterSize(), is(2));
             Eventually.assertThat(invoking(clusterMember).isServiceRunning(INVOCATION_SERVICE_NAME), is(true));
             Eventually.assertThat(invoking(clusterMember).isServiceRunning("DistributedCache"), is(true));

             NamedCache  cache        = CacheFactory.getCache("dist");
             String      sServiceName = cache.getCacheService().getInfo().getServiceName();
             MBeanServer server       = MBeanHelper.findMBeanServer();
             ObjectName  oName        = new ObjectName("Coherence:type=Service,name=DistributedCache,nodeId="
                                                           + clusterMember.getLocalMemberId());

             cluster.suspendService(sServiceName);
             Eventually.assertThat(invoking(server, MBeanServer.class).getAttribute(oName, "QuorumStatus"),
                                      is("Suspended"));
             }
         catch (Exception e)
             {
             fail(e.getMessage());
             }
         finally
             {
             stopCacheServer("ManagedNode");
             AbstractFunctionalTest._shutdown();
             }
         }

    /**
     * Test that when logClusterState operation of ClusterMBean is invoked,
     * the thread dump is logged.
     */
    @Test
    public void testLogClusterNodeState()
        {
        CacheFactory.shutdown();

        System.setProperty("tangosol.coherence.management","all");
        System.setProperty("tangosol.coherence.management.remote","true");
        System.setProperty("test.log", "jdk");

        Logger     logger     = m_logger = Logger.getLogger("Coherence");
        LogHandler logHandler = new LogHandler();
        logger.addHandler(logHandler);

        logHandler.m_enabled  = true;

        MBeanServer serverJMX = MBeanHelper.findMBeanServer();
        Cluster     cluster   = CacheFactory.ensureCluster();

        CacheFactory.getCache("dist-cache");

        // invoke the logClusterState operation of ClusterMBean.
        try
            {
            serverJMX.invoke(new ObjectName("Coherence:type=Cluster"), "logClusterState",
                    new Object[]{cluster.getLocalMember().getRoleName()},
                    new String[]{String.class.getName()});
            }
        catch (MalformedObjectNameException | InstanceNotFoundException |
               MBeanException | ReflectionException e)
            {
            e.printStackTrace();
            fail();
            }

        // assert that the full thread dump is logged.
        try
            {
            Eventually.assertThat(invoking(logHandler).contains("Full Thread Dump"), is(true));
            }
        finally
            {
            logHandler.m_enabled = false;

            System.clearProperty("tangosol.coherence.management");
            System.clearProperty("tangosol.coherence.management.remote");
            System.clearProperty("test.log");
            }
        }

    /**
     * Test that when tangosol.coherence.management.extendedmbeanname is set to true,
     * and the member name is set, the thread dump is logged. COH-12831.
     */
    @Test
    public void testLogClusterNodeStateWithExtendedMBeanName()
        {
        CacheFactory.shutdown();

        System.setProperty("coherence.distributed.localstorage","true");
        System.setProperty("coherence.management","all");
        System.setProperty("coherence.management.remote","true");
        System.setProperty("coherence.management.extendedmbeanname", "true");
        System.setProperty("coherence.member", "grid-storage 1");
        System.setProperty("test.log","jdk");

        Logger     logger     = m_logger = Logger.getLogger("Coherence");
        LogHandler logHandler = new LogHandler();
        logger.addHandler(logHandler);

        logHandler.m_enabled = true;

        Cluster      cluster   = CacheFactory.ensureCluster();
        MBeanServer  serverJMX = MBeanHelper.findMBeanServer();

        CacheFactory.getCache("dist-cache");

        // invoke the logClusterState operation of ClusterMBean.
        try
            {
            String sMBeanName = cluster.getManagement().ensureGlobalName("Coherence:type=Cluster",
                cluster.getLocalMember());
            CacheFactory.log("testLogClusterNodeStateWithExtendedMBeanName: MBean Name=" + sMBeanName,
                CacheFactory.LOG_INFO);
            serverJMX.invoke(new ObjectName(sMBeanName), "logClusterState",
                    new Object[]{cluster.getLocalMember().getRoleName()},
                    new String[]{String.class.getName()});
            }
        catch (MalformedObjectNameException | InstanceNotFoundException |
               MBeanException | ReflectionException e)
            {
            e.printStackTrace();
            fail();
            }

        // assert that the full thread dump is logged.
        try
            {
            Eventually.assertThat(invoking(logHandler).contains("Full Thread Dump"), is(true));
            }
        finally
            {
            logHandler.m_enabled = false;

            System.clearProperty("coherence.distributed.localstorage");
            System.clearProperty("coherence.management");
            System.clearProperty("coherence.management.remote");
            System.clearProperty("coherence.management.extendedmbeanname");
            System.clearProperty("coherence.member");
            System.clearProperty("test.log");
            }
        }

    // ----- inner class: LogHandler ----------------------------------------

    /**
     * A jdk logging handler to capture log messages when enabled.
     */
    public static class LogHandler extends Handler
        {

        // ----- Handler methods --------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public void publish(LogRecord lr)
            {
            if (m_enabled)
                {
                m_listMessages.add(lr.getMessage());
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void flush()
            {
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws SecurityException
            {
            m_listMessages.clear();
            }

        /**
         * Return true if any of the accumulated messages contain the given
         * {@code sLine}.
         *
         * @return true if any of the accumulated messages contain the given
         *         {@code sLine}
        */
        public boolean contains(String sLine)
            {
            boolean[] af = new boolean[1];
            m_listMessages.forEach(s ->
                {
                if (s.contains(sLine))
                    {
                    af[0] = true;
                    }
                });
            return af[0];
            }

        // ----- data members -----------------------------------------------

        /**
         * Whether to collect log messages.
         */
        public volatile boolean m_enabled = false;

        /**
         * The log messages collected.
         */
        protected List<String> m_listMessages = Collections.synchronizedList(new LinkedList<>());
        }

    // helper classes for testMBeansAndMXBeans

    // interface and class for MBean tests
    public interface QueueSampler1MBean
        {
        public QueueSample getQueueSample();

        public void clearQueue();
        }

    public class QueueSampler1
            implements QueueSampler1MBean
        {
        private Queue<String> queue;

        public QueueSampler1(Queue<String> queue)
            {
            this.queue = queue;
            }

        public QueueSample getQueueSample()
            {
            synchronized (queue)
                {
                return new QueueSample(new Date(), queue.size(), queue.peek());
                }
            }

        public void clearQueue()
            {
            synchronized (queue)
                {
                queue.clear();
                }
            }
        }

    // interface and class for MXBean tests
    public interface QueueSamplerMXBean
        {
        public QueueSample getQueueSample();

        public void clearQueue();
        }

    public class QueueSampler2
            implements QueueSamplerMXBean
        {
        // import com.example.QueueSample;

        private Queue<String> queue;

        public QueueSampler2(Queue<String> queue)
            {
            this.queue = queue;
            }

        public QueueSample getQueueSample()
            {
            synchronized (queue)
                {
                return new QueueSample(new Date(), queue.size(), queue.peek());
                }
            }

        public void clearQueue()
            {
            synchronized (queue)
                {
                queue.clear();
                }
            }
        }

    // interface and class for @MXBean tests
    @MXBean
    public interface QueueSamplerAnno
        {
        public QueueSample getQueueSample();

        public void clearQueue();
        }

    public class QueueSampler3
            implements QueueSamplerAnno
        {
        private Queue<String> queue;

        public QueueSampler3(Queue<String> queue)
            {
            this.queue = queue;
            }

        public QueueSample getQueueSample()
            {
            synchronized (queue)
                {
                return new QueueSample(new Date(), queue.size(), queue.peek());
                }
            }

        public void clearQueue()
            {
            synchronized (queue)
                {
                queue.clear();
                }
            }
        }

    public class QueueSample
        {
        private final Date   date;
        private final int    size;
        private final String head;

        @ConstructorProperties({ "date", "size", "head" })
        public QueueSample(Date date, int size, String head)
            {
            this.date = date;
            this.size = size;
            this.head = head;
            }

        public Date getDate()
            {
            return date;
            }

        public int getSize()
            {
            return size;
            }

        public String getHead()
            {
            return head;
            }
        }

    // end of helper classes for testMBeansAndMXBeans

    /**
     * Test Local Emitter for a custom MBean. As a part of COH-5974
     */
    @Test
    public void testLocalEmitterMBean()
        throws MalformedObjectNameException,
               IntrospectionException, InstanceNotFoundException,
               ReflectionException
        {
        Properties propsMain = new Properties();
        propsMain.put("tangosol.coherence.management", "all");
        propsMain.put("tangosol.coherence.management.remote", "true");
        System.getProperties().putAll(propsMain);

        AbstractFunctionalTest._startup();
        try
            {
            Cluster     cluster   = CacheFactory.ensureCluster();
            Registry    registry  = cluster.getManagement();
            TestEmitter emitter   = new TestEmitter();

            registry.register(TestEmitter.EMITTER_NAME, emitter);

            // Retrieve the bean
            MBeanServer serverJMX = MBeanHelper.findMBeanServer();
            ObjectName  mbeanName = new ObjectName(TestEmitter.EMITTER_NAME);

            serverJMX.getMBeanInfo(mbeanName);

            // Register a listener
            TestListener listener = new TestListener();
            serverJMX.addNotificationListener(mbeanName, listener, null, null);

            // Check regular notification
            emitter.EmitNotification();
            assertEquals(1, listener.getCount(1));

            // Use the trigger api
            registry.getNotificationManager().trigger(TestEmitter.EMITTER_NAME, "TestNotification", "This is a test");
            assertEquals(2, listener.getCount(2));
            }
        finally
            {
            AbstractFunctionalTest._shutdown();
            }
        }

    /**
     * Test Remote Emitter for a custom MBean as a part of COH-5987. The remote test uses both the trigger API
     * as well as the WrapperJMX notification support. The test has two servers, the local one to the test
     * and a remote one. We use the RegisterEmitterInvocable via the invocation service to register the TestEmitter
     * on the remote server and validate that it's visible on the local server. The TestEmitter will be registered as
     * a LocalModel on the remote server and a RemoteModel in the local server. Because TestEmitter is a
     * NotificationBroadcaster, it will support adding notifications. Once we've validated that it's registered in
     * the local MBS, we then register our listener with the local MBS. This will cause the local MBS to register the
     * listener with the coherence management framework, leading to the listener being registered with the RemoteModel.
     * The RemoteModel in turn is then updated with a listener and it registers with the Emitter Mbean. When the
     * MBean then calls handle notification, the notification is sent back to the local server's RemoteModel which will
     * pass the notification on to the TestListener registered by the Local MBS. The TriggerInvocable is then used to
     * trigger a second notification using the Coherence trigger API.
     */
    @Test
    public void testRemoteEmitterMBean()
            throws MalformedObjectNameException,
            IntrospectionException, InstanceNotFoundException,
            ReflectionException, InterruptedException
        {
        Properties propsMain = new Properties();
        propsMain.put("tangosol.coherence.management", "all");
        propsMain.put("tangosol.coherence.management.remote", "false");
        System.getProperties().putAll(propsMain);

        Properties propsSecond = new Properties();
        propsSecond.put("tangosol.coherence.management", "none");
        propsSecond.put("tangosol.coherence.management.remote", "true");

        AbstractFunctionalTest._startup();
        try
            {
            Cluster     cluster   = CacheFactory.ensureCluster();
            Registry    registry  = cluster.getManagement();
            String      sDomain   = registry.getDomainName();

            assertTrue(cluster.isRunning());
            assertEquals("cluster already exists", 1, cluster.getMemberSet().size());

            CoherenceClusterMember clusterMember = startCacheServer("emitter2", PROJECT, null, propsSecond);
            Eventually.assertThat(invoking(clusterMember).getClusterSize(), is(2));
            Eventually.assertThat(invoking(clusterMember).isServiceRunning(INVOCATION_SERVICE_NAME), is(true));
            Eventually.assertThat(invoking(clusterMember).isServiceRunning("InvocationService"), is(true));

            final MBeanServer serverJMX = MBeanHelper.findMBeanServer();
            try
                {
                Member member2 = findCacheServer("emitter2");
                assertTrue("Failed to start the emitter2 node", member2 != null);

                String sNode2 = sDomain + ":" + Registry.NODE_TYPE + ",nodeId=" + member2.getId();
 
                // wait for the remote MBeans registration
                Eventually.assertThat(invoking(this).isMBeanRegistered(serverJMX, new ObjectName(sNode2)), is(true));

                // Get the invocation service
                InvocationService service    = (InvocationService) cluster.getService(INVOCATION_SERVICE_NAME);
                Set setMembers = Collections.singleton(member2);

                // register the custom mbean
                service.query(new RegisterEmitterInvocable(), setMembers);

                // validate that the emitter class has been registered for the node
                final String     sName =  TestEmitter.EMITTER_NAME + ",nodeId=" + member2.getId();
                final ObjectName oName = new ObjectName(sName);

                MBeanInfo info = serverJMX.getMBeanInfo(oName);

                assertEquals("Failed to register Emitter Bean:", info.getClassName(), TestEmitter.class.getName());

                // Add a notification listener
                TestListener listener = new TestListener();
                serverJMX.addNotificationListener(oName, listener, null, null);

                // Modify the remote value
                service.query(new ModifyEmitterInvocable(),  setMembers);

                // validate that we received the notification
                Eventually.assertThat("Incorrect notification count: ", invoking(listener).getCount(1), is(1));

                // Test the remote trigger
                service.query(new TriggerInvocable(), setMembers);

                // validate that we received the notification
                assertEquals("Incorrect notification count: ", 2, listener.getCount(2));

                // Stop the remote cache server and ensure that the custom MBean is unregistered.
                stopCacheServer("emitter2");

                Eventually.assertThat(invoking(this).isMBeanRegistered(serverJMX, oName), is(false));
                }
            finally
                {
                stopCacheServer("emitter2");
                assertTrue("Failed to stop", findCacheServer("emitter2") == null);
                }
            }
        finally
            {
            AbstractFunctionalTest._shutdown();
            }
        }

    // ---- helper methods --------------------------------------------------

    /**
    * Helper function to check if the statusHA is the same as the expected status
    *
    * @param sService     the service name that is tried to get the statusHA from
    * @param nodeId       the node ID of the local Member that is running the service
    * @param sTrueStatus  the expected status of statusHA
    */
    public void checkCacheStatusHA(String sService, int nodeId, String sTrueStatus)
        {
        try
            {
            MBeanServer server    = MBeanHelper.findMBeanServer();
            ObjectName  oBeanName = new ObjectName("Coherence:type=Service,name=" + sService + ",nodeId=" + nodeId);

            Eventually.assertThat(invoking(this).getMbeanAttribute(server, oBeanName, "StatusHA"), is((Object) sTrueStatus));
            }
        catch (Exception e)
            {
            Assert.fail(printStackTrace(e));
            }
        }

    public Object getMbeanAttribute(MBeanServer server, ObjectName oBeanName, String sName)
        {
        try
            {
            return server.getAttribute(oBeanName, sName);
            }
        catch (Exception e)
            {
            return null;
            }
        }

    /**
     * Helper function to get integer attribute from ServiceMBean.
     *
     * @param  sService   the service name that is tried to get this attribute from
     * @param  nodeId     the node Id of the local member that is running the service
     * @param  sAttribute the attribute name
     *
     * @return  the attribute value in Object.
     */
    public Object getEventAttribute(String sService, int nodeId, String sAttribute) {
        try
            {
            MBeanServer server    = MBeanHelper.findMBeanServer();
            ObjectName  oBeanName = new ObjectName("Coherence:type=Service,name=" + sService + ",nodeId=" + nodeId);

            return getMbeanAttribute(server, oBeanName, sAttribute);
            }
        catch (Exception e)
            {
            Assert.fail(printStackTrace(e));
            }
        return -1;
    }

    /**
    * Helper function to start a cache server with specified site name, rack name and machine name.
    *
    * @param sServerName  the name of the cache server
    * @param sSite        the site name of the cache server
    * @param sRack        the rack name of the cache server
    * @param sMachine     the machine name of the cache server
    */
    public CoherenceClusterMember startCacheServerWithIdentity(String sServerName, String sSite, String sRack, String sMachine)
        {
        Properties propsServer = new Properties();
        propsServer.put("tangosol.coherence.site", sSite);
        propsServer.put("tangosol.coherence.rack", sRack);
        propsServer.put("tangosol.coherence.machine", sMachine);

        CoherenceClusterMember server = startCacheServer(sServerName, "jmx", FILE_CFG_CACHE, propsServer);

        Member member = findCacheServer(sServerName);
        assertTrue("Failed to start the " + sServerName + " node", member != null);

        return server;
        }

    /**
    * Regression test for COH-3500.
    */
    private Member testCoh3500(Cluster cluster, CoherenceClusterMember member)
            throws JMException
        {
        SafeCluster _cluster  = (SafeCluster) cluster;
        Gateway     _registry = (Gateway) _cluster.getManagement();
        Map         mapLocal  = _registry.getLocalModels();

        Member memberBefore = _cluster.getLocalMember();
        int    cBefore      = mapLocal.size();
        int    nCount       = member.getClusterSize();

        // simulate an abnormal termination
        _cluster.getCluster().stop();

        Eventually.assertThat(invoking(member).getClusterSize(), is(nCount - 1));

        // simulate a restart
        _cluster.getOldestMember();

        Member memberAfter = _cluster.getLocalMember();
        int    cAfter      = mapLocal.size();

        assertTrue(memberBefore + " != " + memberAfter, memberBefore != memberAfter);
        assertTrue("Fail to unregister mbeans", cAfter <= cBefore);

        return memberAfter;
        }

    // ----- helper classes ---------------------------------------------------
    /**
     * An EventInterceptor implementation to hold off event processing.
     * (to cause event backlog)
     */
    @Interceptor(identifier = "MyTransactionInterceptor")
    @TransactionEvents(TransactionEvent.Type.COMMITTED)
    public static class TransactionInterceptor implements EventInterceptor<TransactionEvent>
        {
        public void onEvent(TransactionEvent event)
            {
            Set<BinaryEntry> set = event.getEntrySet();
            if (set != null && set.size() > 0)
                {
                for (BinaryEntry entry : set)
                    {
                    if (((Integer) entry.getKey()) == 1)
                        {
                        try
                            {
                            Thread.sleep(1000);
                            }
                        catch (Exception e)
                            {
                            }
                        }
                    }
                }
            }
        }

    /**
     * Listener used to validate notifications have occurred
     */
    public class TestListener implements NotificationListener
        {
        public synchronized void handleNotification(Notification notification, Object o)
            {
            m_iCount++;
            notifyAll();
            }

        public synchronized int getCount(int iExpected)
            {
            synchronized(this)
                {
                if (m_iCount == iExpected)
                    {
                    return m_iCount;
                    }
                else
                    {
                    try
                        {
                        wait(1000);
                        }
                    catch (InterruptedException e)
                        {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }

                    return m_iCount;
                    }
                }

            }

        private int m_iCount = 0;
        }

    /**
     * Invocable used to register and modify the TestEmitterMBean on a remote server.
     */
    public class RegisterEmitterInvocable
            extends AbstractInvocable implements Serializable
        {
        public void run()
            {
            Cluster  cluster    = CacheFactory.ensureCluster();
            Registry registry   = cluster.getManagement();
            String   sName      = registry.ensureGlobalName(TestEmitter.EMITTER_NAME);
            TestEmitter emitter = new TestEmitter();

            // Register the TestEmitter with the registry on this member
            CacheFactory.trace("Registering " + sName);
            registry.register(sName,  emitter);

            // Save off a copy of the bean to access later.
            cluster.getService("InvocationService").setUserContext(emitter);
            }
        }

    /**
     * Invocable used to register and modify the TestEmitterMBean on a remote server.
     */
    public class ModifyEmitterInvocable
            extends AbstractInvocable implements Serializable
        {
        public void run()
            {
            try
                {
                Cluster  cluster  = CacheFactory.ensureCluster();
                Registry registry = cluster.getManagement();

                registry.ensureGlobalName(TestEmitter.EMITTER_NAME);

                TestEmitter emitter = (TestEmitter) cluster.getService("InvocationService").getUserContext();

                emitter.EmitNotification();
                }
            catch(Exception e)
                {
                e.printStackTrace();
                }
            }
        }

    /**
     * Invocable used to register and modify the TestEmitterMBean on a remote server.
     */
    public class TriggerInvocable
            extends AbstractInvocable implements Serializable
        {

        public void run()
            {
            try
                {
                Cluster     cluster  = CacheFactory.ensureCluster();
                Registry    registry = cluster.getManagement();
                String      sName    = registry.ensureGlobalName(TestEmitter.EMITTER_NAME);

                registry.getNotificationManager().trigger(sName, "TestNotification", "This is a test");
                }
            catch(Exception e)
                {
                e.printStackTrace();
                }
            }
        }

    public static final String PROJECT = "jmx";

    /**
    * The name of the InvocationService used by all test methods.
    */
    public static String INVOCATION_SERVICE_NAME = "Management";

    /**
     * The cache configuration file with the required schemes used by the testStatusHADisplay method.
     */
    public final static String FILE_CFG_CACHE = "jmx-cache-config.xml";

    /**
     * A reference to logger to ensure it is not gc'd as jdk only holds a
     * weak reference to the logger.
     */
    private static Logger m_logger;
    }
