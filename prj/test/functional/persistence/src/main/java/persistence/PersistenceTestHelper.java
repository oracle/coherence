/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.base.Timeout;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.ServiceInfo;

import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;

import com.tangosol.persistence.CachePersistenceHelper;

import com.tangosol.util.Base;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

import com.tangosol.util.filter.AlwaysFilter;

import com.tangosol.util.processor.ConditionalPut;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.hamcrest.Matchers;

/**
 * Helper for common persistence tests functionality such as create and recover
 * snapshots.
 *
 * @author tam/jh  2014.08.22
 */
public class PersistenceTestHelper
    {
    // ----- constructors ---------------------------------------------------

    public PersistenceTestHelper()
        {
        Cluster cluster = CacheFactory.ensureCluster();

        m_registry = cluster.getManagement();

        if (m_registry == null)
            {
            throw new RuntimeException("Unable to retrieve Registry from cluster");
            }

        m_mbsProxy = m_registry.getMBeanServerProxy();

        ensureMBeanRegistration(m_mbsProxy, Registry.CLUSTER_TYPE);
        }

    /**
     * Create a snapshot of the specified service with the specified name.
     *
     * @param sService   the name of the service to snapshot
     * @param sSnapshot  the name of the snapshot to create
     *
     *
     * @throws MBeanException
     */
    public void createSnapshot(String sService, String sSnapshot)
            throws MBeanException
        {
        invokeOperationWithWait("createSnapshot", sService, sSnapshot);
        }

    /**
     * Recover a snapshot of the specified service with the specified name.
     *
     * @param sService   the name of the service to recover
     * @param sSnapshot  the name of the snapshot to recover
     *
     * @throws MBeanException if any MBean operations fail
     */
    public void recoverSnapshot(String sService, String sSnapshot)
            throws MBeanException
        {
        invokeOperationWithWait("recoverSnapshot", sService, sSnapshot);
        }

    /**
     * Remove a snapshot of the specified service with the specified name.
     *
     * @param sService   the name of the service to remove
     * @param sSnapshot  the name of the snapshot to remove
     *
     * @throws MBeanException if any MBean operations fail
     */
    public void removeSnapshot(String sService, String sSnapshot)
            throws MBeanException
        {
        invokeOperationWithWait("removeSnapshot", sService, sSnapshot);
        }

    /**
     * Archive a snapshot of the specified service with the specified name.
     *
     * @param sService   the name of the service to archive
     * @param sSnapshot  the name of the snapshot to archive
     *
     * @throws MBeanException if any MBean operations fail
     */
    public void archiveSnapshot(String sService, String sSnapshot)
            throws MBeanException
        {
        invokeOperationWithWait("archiveSnapshot", sService, sSnapshot);
        }

    /**
     * Retrieve an archived snapshot of the specified service with the specified name.
     *
     * @param sService   the name of the service to retrieve
     * @param sSnapshot  the name of the snapshot to retrieve
     *
     * @throws MBeanException if any MBean operations fail
     */
    public void retrieveArchivedSnapshot(String sService, String sSnapshot)
            throws MBeanException
        {
        invokeOperationWithWait("retrieveArchivedSnapshot", sService, sSnapshot);
        }

    /**
     * Purge a snapshot of the specified service with the specified name
     * from a central archive location.
     *
     * @param sService   the name of the service to purge
     * @param sSnapshot  the name of the snapshot to purge
     *
     * @throws MBeanException if any MBean operations fail
     */
    public void removeArchivedSnapshot(String sService, String sSnapshot)
            throws MBeanException
        {
        invokeOperationWithWait("removeArchivedSnapshot", sService, sSnapshot);
        }

    /**
     * Issue an operation and wait for the operation to be complete by
     * polling the "Idle" attribute of the PersistenceCoordinator for the service.
     * This method will poll continuously until an "Idle" status has been reached
     * or until timeout set by a calling thread has been raised. e.g.<br>
     * <pre>
     * try (Timeout t = Timeout.after(120, TimeUnit.SECONDS))
     *     {
     *     helper.invokeOperationWithWait("createSnapshot", "Service", "snapshot");
     *     }
     * </pre>
     * When called from CohQL, the TIMEOUT value set in CohQL will be used to interrupt
     * the operation if it has not completed.  <br>
     * Note: Even though and exception is raised, the MBean operation will still
     * execute to completion, but CohQL will return immediately without waiting.
     *
     * @param sOperation    the operation to execute
     * @param sServiceName  the name of the service to execute operation on
     * @param sSnapshot     the snapshot name
     *
     * @throws MBeanException if any MBean related errors
     */
    private void invokeOperationWithWait(String sOperation, String sServiceName, String sSnapshot)
            throws MBeanException
        {
        boolean fisIdle;
        String sBeanName = getMBeanName(sServiceName);

        System.out.println("Operation=" + sOperation + ", service=" + sServiceName + ", snapshot=" + sSnapshot);
        try (Timeout t = Timeout.after(300, TimeUnit.SECONDS))
            {
            m_mbsProxy.invoke(sBeanName, sOperation, new String[] {sSnapshot}, new String[] {"java.lang.String"});

            while (true)
                {
                Blocking.sleep(500L);
                fisIdle = (boolean) getAttribute(sBeanName, "Idle");

                if (fisIdle)
                    {
                    // idle means the operation has completed as we are guaranteed an up-to-date
                    // attribute value just after an operation was called
                    return;
                    }
                }
            }
        catch (Exception e)
            {
            m_mbsProxy.invoke("Coherence:type=Cluster", "logClusterState", new Object[] {null}, new String[] {"java.lang.String"});
            throw Base.ensureRuntimeException(e, "Unable to complete operation " + sOperation + " for service "
                                              + sServiceName);
            }
        }

    /**
     * Invoke a JMX operation and return a value.
     *
     * @param sOperation  the operation to execute
     * @param sService    the service to invoke the operation against
     * @param asArgs      the arguments to pass to the operation
     * @param asArgsType  the type of arguments
     *
     * @return the result of the operation
     */
    public Object invokeOperationWithReturn(String sOperation, String sService,
                                                   String[] asArgs,String[] asArgsType)
        {
        try
            {
            return m_mbsProxy.invoke(getMBeanName(sService), sOperation, asArgs, asArgsType);
            }
        catch (Exception e)
            {
            fail(Base.printStackTrace(e));
            }

        return (Object) null;
        }

    /**
     * List the snapshots for the specified service.
     *
     * @param sService   the name of the service to list snapshots for
     *
     * @return the snapshots for the specified service
     */
    public String[] listSnapshots(String sService)
        {
        return (String[]) m_mbsProxy.getAttribute(getMBeanName(sService), "Snapshots");
        }

    /**
     * Return the PersistenceManager MBean name.
     *
     * @param sService  the name of the service to return the name for
     *
     * @return the MBean name
     */
    private String getMBeanName(String sService)
        {
        return m_registry.ensureGlobalName(
            CachePersistenceHelper.getMBeanName(sService));
        }

    /**
     * Helper function to check if the statusHA is the same as the expected status
     *
     * @param sService     the service name that is tried to get the statusHA from
     * @param nodeId       the node ID of the local Member that is running the service
     * @param sTrueStatus  the expected status of statusHA
     *
     * @return the String that shows current statusHA of given service and node
     */
    public static void checkCacheStatusHA(String sService, int nodeId, String sTrueStatus)
        {
        try
            {
            MBeanServer server    = MBeanHelper.findMBeanServer();
            ObjectName  oBeanName = new ObjectName("Coherence:type=Service,name=" + sService + ",nodeId=" + nodeId);

            Eventually.assertThat(invoking(server).getAttribute(oBeanName, "StatusHA"), is((Object) sTrueStatus));
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Get the StatusHA for a service via MBean server.
     *
     * @param sServiceName  the service name to retrieve statusHA for
     *
     * @return the statusHA of service
     */
    public static String getStatusHA(String sServiceName)
        {
        try
            {
            int nStorageMemberId = getStorageEnabledMember(sServiceName);

            if (nStorageMemberId == -1)
                {
                throw new RuntimeException("Unable to find storage-enabled members for service " + sServiceName);
                }
            MBeanServer server = MBeanHelper.findMBeanServer();

            ObjectName oBeanName = new ObjectName(Registry.SERVICE_TYPE + ",name=" + sServiceName + ",nodeId="
                        + nStorageMemberId);
            return (String) server.getAttribute(oBeanName, "StatusHA");
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "Error getting statusHA");
            }

        }

    /**
     * Return a member id of a storage-enable member for the given service.
     *
     * @param sServiceName  the service name to retrieve member for
     *
     * @return the member id of a storage-enable member for the service or
     *         -1 if none found
     */
    private static int getStorageEnabledMember(String sServiceName)
            throws MalformedObjectNameException, AttributeNotFoundException, MBeanException,
            ReflectionException, InstanceNotFoundException
        {
        Cluster cluster         = CacheFactory.getCluster();
        ServiceInfo serviceInfo = cluster.getServiceInfo(sServiceName);
        Set<Member> setMembers  = serviceInfo.getServiceMembers();
        MBeanServer server      = MBeanHelper.findMBeanServer();

        for (Member member : setMembers)
            {
            ObjectName oBeanName = new ObjectName(Registry.SERVICE_TYPE + ",name=" + sServiceName + ",nodeId=" + member.getId());

            // see if this member is storage-enabled
            if ((Boolean) server.getAttribute(oBeanName, "StorageEnabled"))
                {
                return member.getId();
                }
            }

        return -1;
        }

    /**
     * Populate a cache with data.
     *
     * @param nc    the cache to populate
     * @param cMax  the amount of objects to insert
     */
    public static void populateData(NamedCache nc, int cMax)
        {
        populateData(nc, 0, cMax);
        }

    /**
     * Populate a cache with data.
     *
     * @param nc      the cache to populate
     * @param nStart  the start key
     * @param cMax    the amount of objects to insert
     */
    public static void populateData(NamedCache nc, int nStart, int cMax)
        {
        Map<Integer, Integer> mapBuffer = new HashMap<>();
        int                   BATCH     = 1000;

        for (int i = 0; i < cMax; i++)
            {
            mapBuffer.put(i + nStart, i + nStart);

            if (i % BATCH == 0)
                {
                nc.putAll(mapBuffer);
                mapBuffer.clear();
                }
            }

        if (!mapBuffer.isEmpty())
            {
            nc.putAll(mapBuffer);
            }
        }

    public static void populateData(NamedCache nc, int nStart, int cMax, boolean fInvoke)
        {
        Map<Integer, Integer> mapBuffer = new HashMap<>();
        int                   BATCH     = 10000;

        for (int i = 0; i < cMax; i++)
            {
            mapBuffer.put(i + nStart, i + nStart);

            byte[] array = new byte[1024];
            new Random().nextBytes(array);
            if (i % BATCH == 0)
                {
                nc.invokeAll(mapBuffer.keySet(), new ConditionalPut(AlwaysFilter.INSTANCE, array));
                mapBuffer.clear();
                }
            }

        if (!mapBuffer.isEmpty())
            {
            byte[] array = new byte[1024];
            new Random().nextBytes(array);
            nc.invokeAll(mapBuffer.keySet(), new ConditionalPut(AlwaysFilter.INSTANCE, array));
            nc.putAll(mapBuffer);
            }
        }

    /**
     * Ensure that a object name is registered as there can be a race condition
     * as some MBeans are registered async.
     *
     * @param sObjectName  the object name to ensure
     */
    private void ensureMBeanRegistration(MBeanServerProxy proxy, String sObjectName)
        {
        int      nCounter = 3 * 1000;    // 30 seconds , 3,000 * 10ms wait

        // wait for registration of sObjectName as the registration is done
        // async and may not be complete before our first call after ensureCluster().
        while (!proxy.isMBeanRegistered(sObjectName))
            {

            try
                {
                Blocking.sleep(10L);
                }
            catch (InterruptedException e)
                {
                }

            if (--nCounter <= 0)
                {
                // fail-safe in case cluster never registered
                throw new RuntimeException("MBean " + sObjectName + " was not registered after 30 seconds.");
                }
            }
        }

    /**
     * Return control when either:
     * <ol>
     *     <li>the persistence mbean has been registered or</li>
     *     <li>the timeout of 60s has been reached</li>
     * </ol>
     * Note: if timeout has been reached all persistence mbeans will be output
     *       to the logger
     *
     * @param sName  the service name
     */
    public static void ensurePersistenceMBean(String sName)
        {
        MBeanServerProxy proxy = CacheFactory.getCluster().getManagement().getMBeanServerProxy();
        String sPersistenceMBean = "Coherence:" + CachePersistenceHelper.getMBeanName(sName);
        try
            {
            Eventually.assertThat(invoking(proxy).isMBeanRegistered(sPersistenceMBean), Matchers.is(true));
            }
        catch (Exception e)
            {
            Logger.info("Could not find MBean (" + sPersistenceMBean + "); following has been registered:\n" +
                    proxy.queryNames("type=Persistence,*", null));

            throw e;
            }
        }

    /**
     * Return an attribute name from an MBean.
     *
     * @param sObjectName  object name to query
     * @param sAttribute   attribute to retrieve from object name
     *
     * @return the value of the attribute
     */
    private Object getAttribute(String sObjectName, String sAttribute)
        {
        return m_mbsProxy.getAttribute(sObjectName, sAttribute);
        }

    // ----- data members ---------------------------------------------------

    /**
     * MBean server proxy for JMX operations and attribute retrieval for online mode.
     */
    private MBeanServerProxy m_mbsProxy;

    /**
     * Management Registry if we are connected to a cluster.
     */
    private Registry m_registry;
    }