/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package jmx;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.tangosol.io.FileHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.NamedCache;

import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;

import com.tangosol.persistence.CachePersistenceHelper;

import com.tangosol.util.Base;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.filter.EqualsFilter;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.ObjectName;

import java.io.File;
import java.io.Serializable;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.testsupport.deferred.Eventually.within;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * MBeanServerProxyTests is responsible for testing the MBeanServerProxy.
 *
 * @author hr  2018.02.26
 */
public class MBeanServerProxyTests
        extends AbstractFunctionalTest implements Serializable
    {
    // ----- constructors ---------------------------------------------------

    public MBeanServerProxyTests()
        {
        super(FILE_CFG_CACHE);
        }

    // ----- test methods ---------------------------------------------------

    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.role", "main");
        System.setProperty("coherence.log.level", "3");

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

    @Test
    public void testExecute()
        {
        Properties props = new Properties();
        props.put("coherence.management", "none");
        props.put("coherence.distributed.localstorage", "false");
        props.put("coherence.management.remote", "true");

        MBeanServerProxy mBeanProxy = CacheFactory.ensureCluster().getManagement().getMBeanServerProxy();

        try (CoherenceClusterMember server1 = startCacheServer("CacheServer-1", PROJECT, null, props);
             CoherenceClusterMember server2 = startCacheServer("CacheServer-2", PROJECT, null, props))
            {
            CacheFactory.getCache("foo");

            final String MBEAN_NAME_PREFIX = "Coherence:type=Service,name=DistributedCache,nodeId=";
            Eventually.assertThat(invoking(this).isMBeanReady(mBeanProxy,
                    MBEAN_NAME_PREFIX + "1", MBEAN_NAME_PREFIX + "2"), is(true));

            String[] asOwnership = mBeanProxy.execute(mbs ->
                {
                try
                    {
                    final String MBEAN_PREFIX   = "Coherence:type=Service,name=DistributedCache,nodeId=";
                    final String OPERATION_NAME = "reportOwnership";

                    Object[] aoParams     = new Object[] {true};
                    String[] asParamTypes = new String[] {"boolean"};
                    String[] asReturn     = new String[2];

                    asReturn[0] = (String) mbs.invoke(new ObjectName(MBEAN_PREFIX + "1"),
                            OPERATION_NAME, aoParams, asParamTypes);

                    asReturn[1] = (String) mbs.invoke(new ObjectName(MBEAN_PREFIX + "2"),
                            OPERATION_NAME, aoParams, asParamTypes);

                    return asReturn;
                    }
                catch (Throwable e)
                    {
                    CacheFactory.log("Error thrown: " + Base.getStackTrace(e), CacheFactory.LOG_INFO);

                    throw Base.ensureRuntimeException(e);
                    }
                });

            assertEquals(2, asOwnership.length);

            Arrays.stream(asOwnership).forEach(sOwnership ->
                    assertNotEquals("n/a", sOwnership));
            }
        }

    /**
     * Test that the remote managed node is ownership senior.
     * proxy.invoke call flow:  MBeanServerProxy => remote managed node gateway Local => LocalModel
     */
    @Test
    public void testMBeansServerProxyLocal()
        {
        File fileBase = null;
        try
            {
            fileBase = FileHelper.createTempDir();

            Properties propsMain = new Properties();
            propsMain.put("coherence.management", "none");
            propsMain.put("coherence.distributed.localstorage", "true");
            propsMain.put("coherence.management.remote", "true");

            // set default persistence directory so as not to put files in default directory ~/coherence
            propsMain.put(CachePersistenceHelper.DEFAULT_BASE_DIR_PROPERTY, fileBase.getAbsolutePath());

            System.getProperties().putAll(propsMain);

            Properties propsSecond = new Properties();
            propsSecond.put("coherence.management", "all");
            propsSecond.put("coherence.distributed.localstorage", "true");
            propsSecond.put("coherence.management.remote", "true");
            propsSecond.put(CachePersistenceHelper.DEFAULT_BASE_DIR_PROPERTY, fileBase.getAbsolutePath());

            // start managed node first
            CoherenceClusterMember clusterMember = startCacheServer("ManagedNodeLocal", PROJECT, null, propsSecond);

            // start client non-managed node
            CacheFactory.ensureCluster();

            Eventually.assertThat(invoking(clusterMember).getClusterSize(), is(2));
            Eventually.assertThat(invoking(clusterMember).isServiceRunning(INVOCATION_SERVICE_NAME), is(true));
            Eventually.assertThat(invoking(clusterMember).isServiceRunning("InvocationService"), is(true));

            testMBeanServerProxy();
            }
        catch (Exception e)
            {
            fail(e.getMessage());
            }
        finally
            {
            stopCacheServer("ManagedNode");
            AbstractFunctionalTest._shutdown();

            if (fileBase != null)
                {
                FileHelper.deleteDirSilent(fileBase);
                }
            }
        }

    /**
     * Test that the remote managed node is not ownership senior.
     * proxy.invoke call flow:  MBeanServerProxy => (remote managed node gateway Local => RemoteModel) => remote non-managed node
     */
    @Test
    public void testMBeansServerProxyRemote()
        {
        File fileBase = null;

        try
            {
            fileBase = FileHelper.createTempDir();

            Properties propsMain = new Properties();
            propsMain.put("coherence.management", "none");
            propsMain.put("coherence.distributed.localstorage", "true");
            propsMain.put("coherence.management.remote", "true");

            // set default persistence directory so as not to put files in default directory ~/coherence
            propsMain.put(CachePersistenceHelper.DEFAULT_BASE_DIR_PROPERTY, fileBase.getAbsolutePath());

            System.getProperties().putAll(propsMain);

            Properties propsSecond = new Properties();
            propsSecond.put("coherence.management", "all");
            propsSecond.put("coherence.distributed.localstorage", "true");
            propsSecond.put("coherence.management.remote", "true");
            propsSecond.put(CachePersistenceHelper.DEFAULT_BASE_DIR_PROPERTY, fileBase.getAbsolutePath());

            //start client non-managed node
            CacheFactory.ensureCluster();

            //start managed node, which is not senior
            CoherenceClusterMember clusterMember = startCacheServer("ManagedNodeRemote", PROJECT, null, propsSecond);
            Eventually.assertThat(invoking(clusterMember).getClusterSize(), is(2));
            Eventually.assertThat(invoking(clusterMember).isServiceRunning(INVOCATION_SERVICE_NAME), is(true));
            Eventually.assertThat(invoking(clusterMember).isServiceRunning("InvocationService"), is(true));

            testMBeanServerProxy();
            }
        catch (Exception e)
            {
            fail(e.getMessage());
            }
        finally
            {
            stopCacheServer("ManagedNode");
            AbstractFunctionalTest._shutdown();

            if (fileBase != null)
                {
                FileHelper.deleteDirSilent(fileBase);
                }
            }
        }

    /**
     * Test the reportNodeState operation on the Node MBean.
     */
    @Test
    public void testReportNodeState()
        {
        Properties propsMain = new Properties();
        propsMain.put("coherence.distributed.localstorage","true");
        propsMain.put("coherence.distributed.threads", "2");
        propsMain.put("coherence.management", "all");
        propsMain.put("coherence.management.remote", "true");

        // set the following JVM args to ensure the waiting Poll is considered
        // 'outstanding'
        propsMain.put("coherence.guard.timeout", "10000");
        propsMain.put("coherence.service.startuptimeout", "10000");
        System.getProperties().putAll(propsMain);

        AbstractFunctionalTest._startup();

        try
            {
            Cluster     cluster   = CacheFactory.ensureCluster();
            Registry    registry  = cluster.getManagement();

            assertTrue(cluster.isRunning());
            assertEquals("cluster already exists", 1, cluster.getMemberSet().size());

            NamedCache cache          = CacheFactory.getCache("foo");
            MBeanServerProxy mbsProxy = registry.getMBeanServerProxy();

            CompletableFuture future = cache.async().invoke(1, entry ->
                {
                Base.sleep(10000L);
                return null;
                });

            Base.sleep(6000L);

            String sResult = (String) mbsProxy.invoke("type=Node,nodeId=" + CacheFactory.getCluster().getLocalMember().getId()
                 , "reportNodeState", new Object[0], new String[0]);

            assertTrue("Result does not contain correct string value, as it contains: " + sResult
                    , sResult != null && sResult.indexOf("*** Outstanding polls: ***") > 0);

            try
                {
                future.get();
                }
            catch (InterruptedException | ExecutionException e)
                {
                // expected - ignore
                }
            }
        finally
            {
            AbstractFunctionalTest._shutdown();

            Properties propsSys = System.getProperties();
            propsMain.forEach((key, value) -> propsSys.remove(key));
            }
        }

    /**
     * Test the reportEnvironment operation on the Node MBean.
     */
    @Test
    public void testReportEnvironment()
    {
        Properties propsMain = new Properties();
        propsMain.put("coherence.distributed.localstorage","true");
        propsMain.put("coherence.distributed.threads", "2");
        propsMain.put("coherence.management", "all");
        propsMain.put("coherence.management.remote", "true");

        // set the following JVM args to ensure the waiting Poll is considered
        // 'outstanding'
        propsMain.put("coherence.guard.timeout", "10000");
        propsMain.put("coherence.service.startuptimeout", "10000");
        System.getProperties().putAll(propsMain);

        AbstractFunctionalTest._startup();

        try
        {
            Cluster     cluster   = CacheFactory.ensureCluster();
            Registry    registry  = cluster.getManagement();

            assertTrue(cluster.isRunning());
            assertEquals("cluster already exists", 1, cluster.getMemberSet().size());

            NamedCache cache          = CacheFactory.getCache("foo");
            MBeanServerProxy mbsProxy = registry.getMBeanServerProxy();

            CompletableFuture future = cache.async().invoke(1, entry ->
            {
                Base.sleep(10000L);
                return null;
            });

            Base.sleep(6000L);

            String sResult = (String) mbsProxy.invoke("type=Node,nodeId=" + CacheFactory.getCluster().getLocalMember().getId()
                    , "reportEnvironment", new Object[0], new String[0]);

            assertTrue("Result does not contain correct string value, as it contains: " + sResult
                    , sResult != null && sResult.indexOf("Java Vendor:") > 0);

            assertTrue("Result does not contain correct string value, as it contains: " + sResult
                    , sResult != null && sResult.indexOf("Java Virtual Machine:") > 0);

            assertTrue("Result does not contain correct string value, as it contains: " + sResult
                    , sResult != null && sResult.indexOf("Java Runtime Environment:") > 0);

            assertTrue("Result does not contain correct string value, as it contains: " + sResult
                    , sResult != null && sResult.indexOf("System Properties:") > 0);

            assertTrue("Result does not contain correct string value, as it contains: " + sResult
                    , sResult != null && sResult.indexOf("coherence.distributed.threads : 2") > 0);

            assertTrue("Result does not contain correct string value, as it contains: " + sResult
                    , sResult != null && sResult.indexOf("coherence.management.remote : true") > 0);

            assertTrue("Result does not contain correct string value, as it contains: " + sResult
                    , sResult != null && sResult.indexOf("coherence.guard.timeout : 1000") > 0);

            try
            {
                future.get();
            }
            catch (InterruptedException | ExecutionException e)
            {
                // expected - ignore
            }
        }
        finally
        {
            AbstractFunctionalTest._shutdown();

            Properties propsSys = System.getProperties();
            propsMain.forEach((key, value) -> propsSys.remove(key));
        }
    }

    // ---- helper methods --------------------------------------------------

    /**
     * Determine whether the list of MBean exist
     * @param mBeanProxy  the MBeanServerProxy
     * @param objNames    the list of MBean object names
     *
     * @return true if all the MBean object exist
     */
    // must be public - used in Eventually.assertThat call.
    public boolean isMBeanReady(MBeanServerProxy mBeanProxy, String... objNames)
        {
        try
            {
            boolean result = true;
            for (int i = 0; i < objNames.length && result; i++)
                {
                result = result && mBeanProxy.isMBeanRegistered(objNames[i]);
                }
            return result;
            }
        catch(Throwable t)
            {
            return false;
            }
        }

    /**
     * test class for testMBeanServerProxy*
     */
    protected void testMBeanServerProxy()
            throws Exception
        {
        Cluster          cluster  = CacheFactory.ensureCluster();
        Registry         registry = cluster.getManagement();
        MBeanServerProxy proxy    = registry.getMBeanServerProxy();

        NamedCache cache = CacheFactory.getCache("dist");
        cache.size(); // touch the cache

        String sMBeanName = registry.ensureGlobalName(
                "Coherence:type=PartitionAssignment,service=DistributedCache,responsibility=DistributionCoordinator");

        // test isMBeanRegistered()
        assertTrue("The MBean " + sMBeanName + " is not registered",
                proxy.isMBeanRegistered(sMBeanName));

        // test queryNames()
        try
            {
            String sMsg = "The retrieved MBeans do not match the expected pattern: ";

            Set<String> setResult = proxy.queryNames(sMBeanName, null);
            assertEquals(1, setResult.size());
            assertTrue(sMsg + sMBeanName, validateResult(setResult, sMBeanName));

            String sPattern = "Coherence:type=Cache,*";
            setResult = proxy.queryNames(sPattern, null);
            assertEquals(2, setResult.size());
            assertTrue(sMsg + sPattern, validateResult(setResult, "Coherence:type=Cache,"));

            setResult = proxy.queryNames((String) null, null);
            assertTrue(setResult.size() > 10);
            assertTrue(sMsg + sMBeanName, validateResult(setResult, sMBeanName));
            assertTrue(sMsg + "java.lang:type ", validateResult(setResult, "java.lang:type="));

            EqualsFilter<ObjectName, String> filter = new EqualsFilter<>("getDomain", "Coherence");
            setResult = proxy.queryNames((String) null, filter);
            assertTrue("The retrieved MBeans should not include JVM MBeans! ", !validateResult(setResult, "java.lang:type="));

            filter    = new EqualsFilter<>(new ReflectionExtractor("getKeyProperty", new Object[]{"service"}), "DistributedCache");
            setResult = proxy.queryNames((String) null, filter);
            assertTrue(sMsg + "service=DistributedCache", validateResult(setResult, "service=DistributedCache"));
            }
        catch (RuntimeException e)
            {
            fail("Test queryNames should pass, got RuntimeException instead: " + e.getMessage());
            }

        // test getAttribute()
        try
            {
            proxy.getAttribute(sMBeanName, "HAStatus");
            }
        catch (RuntimeException e)
            {
            fail("Test getAttribute should pass, got RuntimeException instead: " + e.getMessage());
            }

        // test should fail with invalid attribute name
        try
            {
            proxy.getAttribute(sMBeanName, "StatusHA");
            fail("Test should fail with invalid MBean attribute name");
            }
        catch (RuntimeException e) {}


        // test proxy.invoke for primitive argument type
        try
            {
            proxy.invoke(sMBeanName, "reportScheduledDistributions", new Object[]{Boolean.TRUE}, new String[]{"boolean"});
            }
        catch (RuntimeException e)
            {
            fail("Test proxy.invoke for primitive argument type should pass, got RuntimeException instead: "
                    + e.getMessage());
            }

        // test proxy.invoke for primitive argument type with null signature
        try
            {
            proxy.invoke(sMBeanName, "reportScheduledDistributions", new Object[]{Boolean.TRUE}, null);
            }
        catch (RuntimeException e)
            {
            fail("Test proxy.invoke for primitive argument type with null signature should pass, got RuntimeException instead: "
                    + e.getMessage());
            }

        sMBeanName = registry.ensureGlobalName(
            CachePersistenceHelper.getMBeanName("DistributedCache"));

        waitForIdleStatus(proxy, sMBeanName);

        List listSnapshots = Arrays.asList((String[]) proxy.getAttribute(sMBeanName, "Snapshots"));
        if (listSnapshots.contains("snapshot-proxy1"))
            {
            // remove the created snapshots
            proxy.invoke(sMBeanName, "removeSnapshot", new String[]{"snapshot-proxy1"}, null);
            }
        waitForIdleStatus(proxy, sMBeanName);

        if (listSnapshots.contains("snapshot-proxy2"))
            {
            // remove the created snapshots
            proxy.invoke(sMBeanName, "removeSnapshot", new String[]{"snapshot-proxy2"}, null);
            }
        waitForIdleStatus(proxy, sMBeanName);

        // test proxy.invoke for none-primitive argument type
        try
            {
            proxy.invoke(sMBeanName, "createSnapshot", new String[]{"snapshot-proxy1"}, new String[]{String.class.getName()});
            }
        catch (RuntimeException e)
            {
            fail("Test proxy.invoke should pass, get RuntimeException instead: " + e.getMessage());
            }
        waitForIdleStatus(proxy, sMBeanName);

        // test proxy.invoke for none-primitive argument type with null signature
        try
            {
            proxy.invoke(sMBeanName, "createSnapshot", new String[]{"snapshot-proxy2"}, null);
            }
        catch (RuntimeException e)
            {
            fail("Test proxy.invoke with null signature should pass, get RuntimeException instead: " + e.getMessage());
            }
        waitForIdleStatus(proxy, sMBeanName);

        listSnapshots = Arrays.asList((String[]) proxy.getAttribute(sMBeanName, "Snapshots"));

        assertTrue("The snapshot-proxy1 was not created successfully", listSnapshots.contains("snapshot-proxy1"));
        assertTrue("The snapshot-proxy2 was not created successfully", listSnapshots.contains("snapshot-proxy2"));
        }

    /**
     * Helper method for testMBeanServerProxy
     */

    protected void waitForIdleStatus(MBeanServerProxy proxy, String sName)
        {
        Eventually.assertThat(invoking(proxy).getAttribute(sName, "OperationStatus"), is((Object) "Idle"),
                within(60 * 2, TimeUnit.SECONDS));
        }

    /**
     * Return true if the result set contains the specified string.
     *
     * @param setResult  the set of strings to be checked for
     * @param sName      the specified string
     *
     * @return true if the result set contains the specified
     */
    protected boolean validateResult(Set<String> setResult, String sName)
        {
        for (String s : setResult)
            {
            if (s.contains(sName))
                {
                return true;
                }
            }

        return false;
        }

    // ----- constants ------------------------------------------------------

    public static final String PROJECT = "jmx";

    /**
     * The name of the InvocationService used by all test methods.
     */
    public static String INVOCATION_SERVICE_NAME = "Management";

    /**
     * The cache configuration file with the required schemes used by the testStatusHADisplay method.
     */
    public final static String FILE_CFG_CACHE = "jmx-cache-config.xml";
    }
