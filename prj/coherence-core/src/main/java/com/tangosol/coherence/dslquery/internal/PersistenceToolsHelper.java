/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.internal;

import com.oracle.coherence.common.base.Blocking;

import com.oracle.coherence.persistence.PersistenceException;
import com.tangosol.coherence.dslquery.CohQLException;
import com.tangosol.coherence.dslquery.ExecutionContext;
import com.tangosol.coherence.dsltools.precedence.OPScanner;
import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.Terms;

import com.tangosol.io.FileHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.Member;
import com.tangosol.net.Service;
import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;

import com.tangosol.persistence.CachePersistenceHelper;
import com.tangosol.persistence.PersistenceEnvironmentInfo;

import com.tangosol.util.Base;
import com.tangosol.util.Filters;
import com.tangosol.util.WrapperException;

import java.io.File;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.management.MBeanException;

import static com.tangosol.persistence.CachePersistenceHelper.getMBeanName;

/**
 * Various helper classes to support calling Persistence operations
 * from within CohQL.
 *
 * @author  tam 2014.02.14
 * @since 12.2.1
 */
public class PersistenceToolsHelper
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new PersistenceToolsHelper which can be used to issue
     * persistence related commands from CohQL. No-args constructor is used
     * when no tracing is required.
     */
    public PersistenceToolsHelper()
        {
        this(null);
        }

    /**
     * Construct a new PersistenceToolsHelper which can be used to issue
     * persistence related commands from CohQL.
     *
     * @param out  the PrintWriter to write trace messages to
     */
    public PersistenceToolsHelper(PrintWriter out)
        {
        Cluster cluster = CacheFactory.ensureCluster();

        m_registry = cluster.getManagement();
        m_out      = out;

        if (m_registry == null)
            {
            throw new CohQLException("Unable to retrieve Registry from cluster");
            }

        m_mbsProxy = m_registry.getMBeanServerProxy();

        ensureMBeanRegistration(Registry.CLUSTER_TYPE);
        }

    // ----- PersistenceToolsHelper methods----------------------------------

    /**
     * Ensure a {@link PersistenceToolsHelper} exists within the CohQL {@link ExecutionContext}
     * which can be used to issue cluster related Persistence commands.
     * If it doesn't, then create a new one.
     *
     * @param ctx  current CohQL {@link ExecutionContext}
     *
     * @return the existing PersistenceToolsHelper or a new one if doesn't exist
     *
     * @throws CohQLException if we are unable to retrieve a new API
     */
    public static PersistenceToolsHelper ensurePersistenceToolsHelper(ExecutionContext ctx)
            throws CohQLException
        {
        PersistenceToolsHelper helper = ctx.getResourceRegistry().getResource(PersistenceToolsHelper.class, HELPER);

        try
            {
            if (helper == null)
                {
                helper = new PersistenceToolsHelper(ctx.isTraceEnabled() ? ctx.getWriter() : null);

                ctx.getResourceRegistry().registerResource(PersistenceToolsHelper.class, HELPER, helper);
                }
            }
        catch (Exception e)
            {
            throw ensureCohQLException(e, "Unable to instantiate PersistenceToolsHelper");
            }

        return helper;
        }

    /**
     * Issue an operation and wait for the operation to be complete by
     * polling the "Idle" attribute of the PersistenceCoordinator for the service.
     * This method will poll continuously until an "Idle" status has been reached
     * or until timeout set by a calling thread has been raised. e.g.<br>
     * <pre>
     * try (Timeout t = Timeout.after(120, TimeUnit.SECONDS))
     *     {
     *     helper.invokeOperationWithWait("createSnapshot", "snapshot", "Service");
     *     }
     * </pre>
     * When called from CohQL, the TIMEOUT value set in CohQL will be used to interrupt
     * the operation if it has not completed.  <br>
     * Note: Even though and exception is raised, the MBean operation will still
     * execute to completion, but CohQL will return immediately without waiting.
     *
     * @param sOperation    the operation to execute
     * @param sSnapshot     the snapshot name
     * @param sServiceName  the name of the service to execute operation on
     *
     * @throws MBeanException if any MBean related errors
     */
    public void invokeOperationWithWait(String sOperation, String sSnapshot, String sServiceName)
            throws MBeanException
        {
        boolean fisIdle;

        try
            {
            invokeOperation(sOperation, sServiceName, new String[] {sSnapshot}, new String[] {"java.lang.String"});

            String sBeanName = getPersistenceMBean(sServiceName);

            // COH-20778 wait a bit longer to avoid false positive of "Idle" check on window
            Blocking.sleep(SLEEP_TIME);

            while (true)
                {
                Blocking.sleep(SLEEP_TIME);
                fisIdle = (boolean) getAttribute(sBeanName, "Idle");
                traceMessage("Idle = " + fisIdle);

                if (fisIdle)
                    {
                    // idle means the operation has completed as we are guaranteed an up-to-date
                    // attribute value just after an operation was called
                    return;
                    }

                traceMessage("Operation " + sOperation + " not yet complete, waiting "
                        + SLEEP_TIME + "ms");
                }
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "Unable to complete operation " +
                    sOperation + " for service " + sServiceName);
            }
        }

    /**
     * Invoke an operation against a PersistenceManagerMBean associated to the
     * given service name.
     *
     * @param sOperation    the operation to execute
     * @param sServiceName  the name of the service to execute operation on
     * @param aoParams      the parameters of the operation
     * @param asParamTypes  the parameter types of the operation
     *
     * @throws MBeanException if an error occurred invoking the MBean
     */
    public void invokeOperation(String sOperation, String sServiceName, Object[] aoParams, String[] asParamTypes)
            throws MBeanException
        {
        String sBeanName = getPersistenceMBean(sServiceName);

        traceMessage("Invoking " + sOperation + " on " + sBeanName +
                " using params = " + Arrays.toString(aoParams));

        m_mbsProxy.invoke(sBeanName, sOperation, aoParams, asParamTypes);
        }

    /**
     * Validate that a service name exists for the current cluster.
     *
     * @param sServiceName the service name to check
     *
     * @return true if the service exists
     */
    public boolean serviceExists(String sServiceName)
        {
        try
            {
            Map<String, String[]> mapServices = listServices();

            return mapServices != null && mapServices.containsKey(sServiceName);
            }

        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "Error validating service");
            }
        }

    /**
     * Validate that a snapshot exists for a given service.
     *
     * @param sServiceName  the service name to check
     * @param sSnapshotName the snapshot name to check
     *
     * @return true if the snapshot exists for the service
     */
    public boolean snapshotExists(String sServiceName, String sSnapshotName)
        {
        try
            {
            String[] asSnapshots = listSnapshots(sServiceName);

            return asSnapshots != null && Arrays.asList(asSnapshots).contains(sSnapshotName);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "Error validating snapshot");
            }
        }

    /**
     * Validate that an archived snapshot exists for a given service.
     *
     * @param sServiceName  the service name to check
     * @param sSnapshotName the archived snapshot name to check
     *
     * @return true if the archived snapshot exists for the service
     */
    public boolean archivedSnapshotExists(String sServiceName, String sSnapshotName)
        {
        try
            {
            String[] asSnapshots = listArchivedSnapshots(sServiceName);

            return asSnapshots != null && Arrays.asList(asSnapshots).contains(sSnapshotName);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "Error validating archived snapshots");
            }
        }

    /**
     * Validate that a snapshot exists across all services.
     *
     * @param sSnapshotName the snapshot name to check
     *
     * @throws CohQLException if the condition is not met
     */
    public void validateSnapshotExistsForAllServices(String sSnapshotName)
        {
        StringBuilder sb = new StringBuilder();

        try
            {
            for (Map.Entry<String, String[]> entry : listSnapshots().entrySet())
                {
                String[] asSnapshots = entry.getValue();

                if (!Arrays.asList(asSnapshots).contains(sSnapshotName))
                    {
                    sb.append("The snapshot ").append(sSnapshotName)
                      .append(" does not exist on service ").append(entry.getKey())
                      .append('\n');
                    }
                }
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "Error validating snapshot");
            }

        if (sb.length() > 0)
            {
            throw new CohQLException(sb.toString());
            }
        }

    /**
     * Validate that an archived snapshot exists across all services to ensure
     * success for a retrieve or purge operation.
     *
     * @param sSnapshotName the archived snapshot name to check
     *
     * @throws CohQLException if the condition is met
     */
    public void validateArchivedSnapshotExistsForAllServices(String sSnapshotName)
        {
        StringBuilder sb = new StringBuilder();

        try
            {
            for (Map.Entry<String, String[]> entry : listServices().entrySet())
                {
                String   sServiceName        = entry.getKey();
                String[] asArchivedSnapshots = listArchivedSnapshots(sServiceName);

                if (!Arrays.asList(asArchivedSnapshots).contains(sSnapshotName))
                    {
                    sb.append("The archived snapshot ").append(sSnapshotName)
                      .append(" does not exist on service ").append(sServiceName)
                      .append('\n');
                    }
                }
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "Error validating snapshot");
            }

        if (sb.length() > 0)
            {
            throw new CohQLException(sb.toString());
            }
        }

    /**
     * List all the services configured for active or on-demand mode and information
     * about them including: persistence-mode, QuorumStatus and current operation status
     * from the PersistenceSnapshotMBean.
     *
     * @return a {@link Map} of services and related information
     */
    public Map<String, String[]> listServices()
        {
        Map<String, String[]> mapResults = new HashMap<>();

        for (Map.Entry<String, String> entry : getPersistenceServices().entrySet())
            {
            String sServiceName     = entry.getKey();
            String sPersistenceMode = entry.getValue();

            String[] asResults = getServiceInfo(sServiceName);

            mapResults.put(sServiceName, new String[] {sPersistenceMode, asResults[0], asResults[1]});
            }

        return mapResults;
        }

    /**
     * List all the services configured for active or on-demand mode and display the
     * persistence environment implementation.
     *
     * @return a {@link List} of services and related information
     */
    public List<String> listServicesEnvironment()
        {
        List<String> listInfo = new ArrayList<>();

        for (String sServiceName : getPersistenceServices().keySet())
            {
            String sMBean = getStorageEnabledMember(sServiceName);

            if (sMBean == null)
                {
                throw new RuntimeException("Unable to find storage-enabled members for service " + sServiceName);
                }

            String sEnvironment = (String) getAttribute(sMBean, "PersistenceEnvironment");

            listInfo.add(sServiceName + " - " + sEnvironment);
            }

        return listInfo;
        }

    /**
     * List the snapshots for the specified service.
     *
     * @param sServiceName   the name of the service to list snapshots for
     *
     * @return the snapshots for the specified service or an empty String[]
     *         if none exist
     */
    public String[] listSnapshots(String sServiceName)
        {
        try
            {
            String[] asSnapshots = (String[]) getAttribute(
                    getPersistenceMBean(sServiceName), "Snapshots");

            return asSnapshots == null ? NO_SNAPSHOTS : asSnapshots;
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * List the snapshots for all services.
     *
     * @return a {@link java.util.Map} of services and their snapshots.
     */
    public Map<String, String[]> listSnapshots()
        {
        Map<String, String[]> mapResults = new HashMap<>();

        for (Map.Entry<String, String> entry : getPersistenceServices().entrySet())
            {
            String sServiceName = entry.getKey();

            mapResults.put(sServiceName, listSnapshots(sServiceName));

            }

        return mapResults;
        }

    /**
     * Return a list of archived snapshots for a given service.
     *
     * @param sServiceName   the name of the service to query
     *
     * @return a {@link String}[] of archived snapshots for the given service
     */
    public String[] listArchivedSnapshots(String sServiceName)
        {
        try
            {
            return (String[]) m_mbsProxy.invoke(
                getPersistenceMBean(sServiceName), "listArchivedSnapshots",
                new String[] {}, new String[] {});
            }
        catch (Exception e)
            {
            throw new RuntimeException("Unable to execute listArchivedSnapshots for service "
                + sServiceName + ": " + e.getMessage());
            }
        }

    /**
     * List the archived snapshots for all services.
     *
     * @return a {@link java.util.Map} of services and their archived snapshots.
     */
    public Map<String, String[]> listArchivedSnapshots()
        {
        Map<String, String[]> mapResults = new HashMap<>();

        // go through each of the services returned and retrieve the snapshots.
        for (String sServiceName : getPersistenceServices().keySet())
            {
            try
                {
                mapResults.put(sServiceName, listArchivedSnapshots(sServiceName));
                }
            catch (Exception e)
                {
                if (e instanceof RuntimeException && e.getMessage().contains("MBeanException"))
                    {
                    // ignore as we may not have an archiver defined for the service
                    }
                else
                    {
                    throw ensureCohQLException(e, "Unable to list archived snapshots");
                    }
                }
            }

        return mapResults;
        }

    /**
     * Return the archiver configured for the given service.
     *
     * @param sServiceName the name of the service to query
     *
     * @return the archiver configured for the given services or 'n/a' if none exists
     */
    public String getArchiver(String sServiceName)
        {
        String sMBean = getStorageEnabledMember(sServiceName);

        if (sMBean == null)
            {
            throw new RuntimeException("Unable to find storage-enabled members for service " + sServiceName);
            }

        return (String) getAttribute(sMBean, "PersistenceSnapshotArchiver");
        }

    /**
     * Resume a given service.
     *
     * @param sServiceName the service to resume
     */
    public void resumeService(String sServiceName)
        {
        try
            {
            m_mbsProxy.invoke(Registry.CLUSTER_TYPE, RESUME_SERVICE, new String[] {sServiceName},
                              new String[] {"java.lang.String"});
            }
        catch (Exception e)
            {
            throw new RuntimeException("Unable to resume service " + e.getMessage());
            }
        }

    /**
     * Suspend a given service.
     *
     * @param sServiceName the service to suspend
     */
    public void suspendService(String sServiceName)
        {
        try
            {
            m_mbsProxy.invoke(Registry.CLUSTER_TYPE, SUSPEND_SERVICE, new String[] {sServiceName},
                              new String[] {"java.lang.String"});
            }
        catch (Exception e)
            {
            throw new RuntimeException("Unable to resume service " + e.getMessage());
            }
        }

    /**
     * Ensures that the specified service is in a ready state to begin snapshot operations.
     * Ie. The service should not have operations that are running. This call will
     * wait for any processes to complete if fWait is true.  <br>
     * This method will poll continuously until an "Idle" status has been reached
     * or until timeout set by a calling thread has been raised.
     *
     * @param fWait            if true and the service is not Idle then wait, otherwise
     *                         throw an exception
     * @param sServiceToCheck  the service to check for or null for all services
     *
     * @throws CohQLException if any services are not in a proper state
     */
    public void ensureReady(boolean fWait, String sServiceToCheck)
        {
        try
            {
            while (true)
                {
                String sStatus = getOperationStatus(sServiceToCheck);

                if (STATUS_IDLE.equals(sStatus))
                    {
                    // operation is Idle
                    break;
                    }
                else
                    {
                    if (fWait)
                        {
                        Blocking.sleep(SLEEP_TIME);
                        }
                    else
                        {
                        throw new CohQLException("The service " + sServiceToCheck
                                                 + " currently has an operation in progress: \n" + sStatus
                                                 + "\nPlease use LIST SERVICES to determine when service is ready.");
                        }
                    }
                }
            }
        catch (Exception e)
            {
            throw ensureCohQLException(e, "Error during ensureReady");
            }
        }

    /**
     * Ensures that the services are in a ready state to begin snapshot operations.
     * Ie. they should not have operations that are running. If the context is
     * silent then we will wait, otherwise will fail fast.
     *
     * @param ctx       context
     * @param sService  the service to wait to be ready or if null, then all services
     *
     * @throws CohQLException if any services are not in a proper state
     */
    public void ensureReady(ExecutionContext ctx, String sService)
        {
        ensureReady(ctx.isSilent(), sService);
        }

    /**
     * Return a CohQLException with the given cause. If the specified
     * cause is an instance of CohQLException, the given throwable will
     * be returned as is; otherwise, a new CohQLException will be
     * allocated and returned.
     *
     * @param eCause  an optional cause
     * @param sMsg    an optional detail message
     *
     * @return a CohQLException with the given cause and detail message
     */
    public static CohQLException ensureCohQLException(Throwable eCause, String sMsg)
        {
        StringBuilder sb    = new StringBuilder(sMsg);
        Throwable     cause = eCause;

        // check for exception raised from Mbean Server or PersistenceException
        if ((eCause instanceof WrapperException && eCause.getCause() instanceof RuntimeException) ||
            (eCause instanceof PersistenceException))
            {
            Throwable t = eCause.getCause();
            sb.append(" - ").append(eCause.getMessage());
            if (t != null)
                {
                sb.append('\n').append(t.getMessage());
                cause = t.getCause();
                if (cause != null)
                    {
                    sb.append('\n').append(cause.getMessage());
                    sb.append('\n').append(cause.getCause());
                    }
                }
            }

        return eCause instanceof CohQLException ?
            (CohQLException) eCause : new CohQLException(sb.toString(), cause);
        }

    /**
     * Output a trace message to the defined {@link java.io.PrintWriter}.
     *
     * @param sMessage the message to output
     */
    private void traceMessage(String sMessage)
        {
        if (isTraceEnabled())
            {
            m_out.println(new Date(Base.getSafeTimeMillis()) + " : " + sMessage);
            m_out.flush();
            }
        }

    public String getOperationStatus(String sServiceName)
        {
        return (String) getAttribute(getPersistenceMBean(sServiceName), "OperationStatus");
        }

    /**
     * Validate that a snapshot does not exist across all services.
     *
     * @param sSnapshotName the snapshot name to check
     *
     */
    private void validateNoSnapshotExistsForAllServices(String sSnapshotName)
        {
        StringBuilder sb = new StringBuilder();

        try
            {
            for (Map.Entry<String, String[]> entry : listSnapshots().entrySet())
                {
                String[] asSnapshots = entry.getValue();

                if (Arrays.asList(asSnapshots).contains(sSnapshotName))
                    {
                    sb.append("The snapshot ").append(sSnapshotName)
                      .append(" already exists on service ").append(entry.getKey())
                      .append('\n');
                    }
                }
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "Error validating snapshot");
            }

        if (sb.length() > 0)
            {
            throw new CohQLException(sb.toString());
            }
        }

    /**
     * Return a {@link Map} of services that are configured for persistence as either
     * active or on-demand.
     *
     * @return the {@link Map} of services with service name as key and
     *         persistence mode as value
     */
    private Map<String, String> getPersistenceServices()
        {
        Map<String, String> mapServices = new HashMap<>();

        String sQuery = COHERENCE + Registry.PARTITION_ASSIGNMENT_TYPE + ",responsibility=DistributionCoordinator,*";

        Set<String> setServiceNames = m_mbsProxy.queryNames(sQuery, null)
                                        .stream()
                                        .map(s->s.replaceAll("^.*type=PartitionAssignment", "")
                                                 .replaceAll(",responsibility=DistributionCoordinator", "")
                                                 .replaceAll("domainPartition=.*,", "")
                                                 .replaceAll(",service=", ""))
                                        .collect(Collectors.toSet());

        setServiceNames.forEach(s ->
            {
            Optional<String> serviceMBean =
                    m_mbsProxy.queryNames(COHERENCE + Registry.SERVICE_TYPE + ",name=" + s + ",*", null)
                         .stream().findAny();
            if (serviceMBean.isPresent())
                {
                String sServiceMbean = serviceMBean.get();
                Map<String, Object> mapServiceAttr = m_mbsProxy.getAttributes(sServiceMbean, Filters.always());
                mapServices.put(s, (String) mapServiceAttr.get("PersistenceMode"));
                }
            });

        return mapServices;
        }

    /**
     * Return the ObjectName of a storage-enable member for the given service.
     *
     * @param sServiceName  the service name to retrieve ObjectName for
     *
     * @return the ObjectName of a storage-enable member for the service an null if none
     */
    private String getStorageEnabledMember(String sServiceName)
        {
        Set<String> setServices = m_mbsProxy.queryNames(COHERENCE + Registry.SERVICE_TYPE + ",name=" + sServiceName + ",*", null);

        // find the first storage-enabled member
        for (String sMbean : setServices)
            {
            if ((Integer) getAttribute(sMbean, "OwnedPartitionsPrimary") > 0)
                {
                return sMbean;
                }
            }
        
        return null;
        }

    /**
     * Ensure that a object name is registered as there can be a race condition
     * as some MBeans are registered async.
     *
     * @param sObjectName  the object name to ensure
     */
    private void ensureMBeanRegistration(String sObjectName)
        {
        boolean  fLogged  = false;
        int      nCounter = 3 * 1000;    // 30 seconds , 3,000 * 10ms wait

        // wait for registration of sObjectName as the registration is done
        // async and may not be complete before our first call after ensureCluster().
        while (!m_mbsProxy.isMBeanRegistered(sObjectName))
            {
            if (isTraceEnabled() && !fLogged)
                {
                traceMessage("Waiting for " + sObjectName + " to be registered");
                fLogged = true;
                }

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
                throw new RuntimeException("MBean " + sObjectName + " was not registered after 30 seconds." +
                             " You must be running an MBean Server within the cluster to use 'Persistence' commands.");
                }
            }

            if (isTraceEnabled() && fLogged)
                {
                traceMessage(sObjectName + " is now registered");
                }
        }

    /**
     * Return service information for the list services command. The values returned are:
     * <ol>
     *     <li>[0] - QuorumStatus</li>
     *     <li>[1] - OperationStatus</li>
     * </ol>
     *
     * @param sServiceName  the name of the service to query
     *
     * @return a {@link String} array of information
     */
    private String[] getServiceInfo(String sServiceName)
        {
        String sMBean = getStorageEnabledMember(sServiceName);

        if (sMBean == null)
            {
            throw new RuntimeException("Unable to find storage-enabled members for service " + sServiceName);
            }

        String sQuorumStatus = (String) getAttribute((sMBean), "QuorumStatus");

        String sMBeanName = getMBeanName(sServiceName);
        if (sMBean.contains("domainPartition"))
            {
            String sDomainPartition = sMBean.replaceAll("^.*,domainPartition=", "domainPartition=")
                                            .replaceAll(",.*$", "");
            sMBeanName = getMBeanName(sServiceName) + "," + sDomainPartition;
            }
        String sOperationStatus = (String) getAttribute(sMBeanName, "OperationStatus");

        return new String[] {sQuorumStatus, sOperationStatus};
        }

    /**
     * Return the PersistenceManager MBean name.
     *
     * @param sServiceName  the name of the service to return the name for
     *
     * @return the MBean name
     */
    public String getPersistenceMBean(String sServiceName)
        {
        return ensureGlobalName(getMBeanName(sServiceName));
        }

    /**
     * Return the Service MBean name.
     *
     * @param sServiceName  the name of the service to return the name for
     * @param member        the member of the service to return the name for
     *
     * @return the MBean name
     */
    public String getServiceMBean(String sServiceName, Member member)
        {
        return m_registry.ensureGlobalName(
            Registry.SERVICE_TYPE + ",name=" + sServiceName, member);
        }

    /**
     * Return true if the service is federated or distributed
     *
     * @param sType  the service type
     *
     * @return true if the service is federated or distributed
     */
    private static boolean isValidServiceType(String sType)
        {
        return "DistributedCache".equals(sType) || "FederatedCache".equals(sType);
        }

    /**
     * Return a global name for the given MBean Name.
     *
     * @param sName  the MBean to get global name for.
     *
     * @return the global name.
     */
    private String ensureGlobalName(String sName)
        {
        return m_registry.ensureGlobalName(sName);
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

    // ----- helpers --------------------------------------------------------

    /**
     * Return a term for a given scanner representing the specified name.
     * If the end of statement is reached then an CohQLException is raised.
     *
     * @param s             OPScanner to use
     * @param sName         the name to assign the new term
     * @param sDescription  a description for any exception
     * @param sCommand      the command name
     *
     * @return a new term
     *
     * @throws CohQLException if end of statement is reached
     */
    public static Term getNextTerm(OPScanner s, String sName, String sDescription, String sCommand)
        {
        if (s.isEndOfStatement())
            {
            throw new CohQLException(sDescription + " required for " + sCommand);
            }

        return Terms.newTerm(sName, AtomicTerm.createString(s.getCurrentAsStringWithAdvance()));
        }

    /**
     * Return the snapshot directory for a given service and snapshot.
     *
     * @param ccf           ConfigurableCacheFactory to use to get dependencies
     * @param sSnapshot     the snapshot name to use
     * @param sServiceName  the service name to use
     *
     * @return a File representing the snapshot directory
     */
    public static File getSnapshotDirectory(ConfigurableCacheFactory ccf, String sSnapshot, String sServiceName)
        {
        if (ccf instanceof ExtensibleConfigurableCacheFactory)
            {
            PersistenceEnvironmentInfo info =
                CachePersistenceHelper.getEnvironmentInfo((ExtensibleConfigurableCacheFactory) ccf, sServiceName);

            if (info == null)
                {
                throw new CohQLException("Unable to get persistence environment info for service " +
                                         sServiceName + " and snapshot " + sSnapshot);
                }

            return new File(info.getPersistenceSnapshotDirectory(), FileHelper.toFilename(sSnapshot));
            }

        throw new UnsupportedOperationException("ConfigurableCacheFactory is not an instance of ExtensibleConfigurableCacheFactory");
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Set the {@link java.io.PrintWriter} for any messages to go to.
     *
     * @param out the {@link java.io.PrintWriter} to use
     */
    public void setPrintWriter(PrintWriter out)
        {
        m_out = out;
        }

    /**
     * Return if trace is enabled.
     *
     * @return if trace is enabled
     */
    public boolean isTraceEnabled()
        {
        return m_out != null;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Coherence Prefix.
     */
    private static final String COHERENCE = "Coherence:";

    /**
     * JMX operation to create a snapshot.
     */
    public static final String CREATE_SNAPSHOT = "createSnapshot";

    /**
     * JMX operation to recover a snapshot.
     */
    public static final String RECOVER_SNAPSHOT = "recoverSnapshot";

    /**
     * JMX operation to remove a snapshot.
     */
    public static final String REMOVE_SNAPSHOT = "removeSnapshot";

    /**
     * JMX operation to archive a snapshot
     */
    public static final String ARCHIVE_SNAPSHOT = "archiveSnapshot";

    /**
     * JMX operation to retrieve an archived snapshot
     */
    public static final String RETRIEVE_ARCHIVED_SNAPSHOT = "retrieveArchivedSnapshot";

    /**
     * JMX operation to remove an archived snapshot
     */
    public static final String REMOVE_ARCHIVED_SNAPSHOT = "removeArchivedSnapshot";

    /**
     * JMX operation to suspend a service.
     */
    public static final String SUSPEND_SERVICE = "suspendService";

    /**
     * JMX operation to resume a service.
     */
    public static final String RESUME_SERVICE = "resumeService";

    /**
     * JMX operation to force recovery.
     */
    public static final String FORCE_RECOVERY = "forceRecovery";

    /**
     * Idle status.
     */
    private static final String STATUS_IDLE = "Idle";

    /**
     * Sleep time between checking operation completion.
     */
    private static final long SLEEP_TIME = 2000L;

    /**
     * Cluster Tools registry key.
     */
    private static final String HELPER = "persistence_tools_helper";

    /**
     * Signifies no snapshots were found.
     */
    private static final String[] NO_SNAPSHOTS = new String[0];

    // ----- data members ---------------------------------------------------

    /**
     * A PrintWriter to output any informational messages.
     */
    private PrintWriter m_out = null;

    /**
     * MBean server proxy for JMX operations and attribute retrieval for online mode.
     */
    private MBeanServerProxy m_mbsProxy;

    /**
     * Management Registry if we are connected to a cluster.
     */
    private Registry m_registry;
    }
