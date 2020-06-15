/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.helper;

import com.sun.tools.visualvm.modules.coherence.Localization;

import com.sun.tools.visualvm.modules.coherence.tablemodel.model.PersistenceData;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.Attribute;

import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

/**
 * The {@link RequestSender} based on JMX.
 *
 * @author shyaradh 11.10.2017
 *
 * @since Coherence 12.2.1.4.0
 */
public class JMXRequestSender
        implements RequestSender
    {
    // ------ constructors --------------------------------------------------

    /**
     * Create a {@link JMXRequestSender} object.
     *
     * @param connection  the {@link MBeanServerConnection} to be used by by sender
     */
    public JMXRequestSender(MBeanServerConnection connection)
        {
        this.f_connection = connection;
        }

    @Override
    public List<Attribute> getAllAttributes(ObjectName objName)
            throws Exception
        {
        MBeanInfo            info         = f_connection.getMBeanInfo(objName);
        MBeanAttributeInfo[] attrInfo     = info.getAttributes();
        String[]             asAttributes = new String[attrInfo.length];
        int                  i            = 0;

        // add the attributes
        for (MBeanAttributeInfo attributeInfo : attrInfo)
            {
            asAttributes[i++] = attributeInfo.getName();
            }

        Arrays.sort(asAttributes);

        return f_connection.getAttributes(objName, asAttributes).asList();
        }

    // ------ RequestSender interface ---------------------------------------

    @Override
    public String getAttribute(ObjectName objectName, String attribute)
            throws Exception
        {
        return f_connection.getAttribute(objectName, attribute) + "";
        }

    @Override
    public AttributeList getAttributes(ObjectName objectName, String[] asAttribute)
            throws Exception
        {
        return f_connection.getAttributes(objectName, asAttribute);
        }

    @Override
    public Set<ObjectName> getAllCacheMembers()
            throws Exception
        {
        return f_connection.queryNames(new ObjectName("Coherence:type=Cache,*"), null);
        }

    @Override
    public Set<ObjectName> getAllJournalMembers(String sJournalType)
            throws Exception
        {
        return f_connection.queryNames(new ObjectName("Coherence:type=Journal,name="
                + sJournalType + ",*"), null);
        }

    @Override
    public Set getCacheMembers(String sServiceName, String sCacheName, String sDomainPartition)
            throws Exception
        {
        return f_connection.queryNames(new ObjectName("Coherence:type=Cache,service=" + sServiceName
                + (sDomainPartition != null ? ",domainPartition=" + sDomainPartition : "")
                + ",name=" + sCacheName + ",*"), null);
        }

    @Override
    public Set<ObjectName> getCacheStorageMembers(String sServiceName, String sCacheName, String sDomainPartition)
            throws Exception
        {

        return f_connection.queryNames(new ObjectName("Coherence:type=StorageManager,service="
                + sServiceName + (sDomainPartition != null ? ",domainPartition=" + sDomainPartition : "")
                + ",cache=" + sCacheName + ",*"), null);
        }

    @Override
    public Set<ObjectName> getAllClusters()
            throws Exception
        {
        return f_connection.queryNames(new ObjectName("Coherence:type=Cluster,*"), null);
        }

    @Override
    public Set<ObjectName> getAllCoherenceWebMembers(String sSessionManager)
            throws Exception
        {
        return f_connection.queryNames(new ObjectName("Coherence:type=" +sSessionManager + ",*"), null);
        }

    @Override
    public Set<ObjectName> getCoherenceWebMembersForApplication(String sSessionManager, String sAppId)
            throws Exception
        {
        return f_connection.queryNames(new ObjectName("Coherence:type=" + sSessionManager + ",appId=" + sAppId
                + ",*"), null);
        }

    @Override
    public Set<ObjectName> getClusterMemberOS(int nodeId)
            throws Exception
        {
        return f_connection.queryNames(new ObjectName(
                "Coherence:type=Platform,Domain=java.lang,subType=OperatingSystem,nodeId="
                + nodeId + ",*"), null);
        }

    @Override
    public Set<ObjectName> getAllClusterMembers()
            throws Exception
        {
        return f_connection.queryNames(new ObjectName("Coherence:type=Node,*"), null);
        }

    @Override
    public Set<ObjectName> getAllServiceMembers()
            throws Exception
        {
        return f_connection.queryNames(new ObjectName("Coherence:type=Service,*"), null);
        }

    @Override
    public Set<ObjectName> getMembersOfService(String sServiceName, String sDomainPartition)
            throws Exception
        {
        return f_connection.queryNames(new ObjectName("Coherence:type=Service,name=" + sServiceName +
                (sDomainPartition != null ? ",domainPartition=" + sDomainPartition : "") + ",*"), null);
        }

    @Override
    public Set<ObjectName> getAllProxyServerMembers()
            throws Exception
        {
        return f_connection.queryNames(new ObjectName("Coherence:type=ConnectionManager,*"), null);
        }

    @Override
    public Set<ObjectName> getCompleteObjectName(ObjectName objectName)
            throws Exception
        {
        return f_connection.queryNames(objectName, null);
        }

    @Override
    public Set<ObjectName> getPartitionAssignmentObjectName(String sService, String sDomainPartition)
            throws Exception
        {
        String sQuery = "Coherence:type=PartitionAssignment,service="
                + sService + (sDomainPartition != null ? ",domainPartition=" + sDomainPartition : "")
                + ",responsibility=DistributionCoordinator,*";
        return f_connection.queryNames(new ObjectName(sQuery), null);
        }

    @Override
    public String getScheduledDistributions(String sService, String sDomainPartition)
            throws Exception
        {
        // look up the full name of the MBean in case we are in container
        Set<ObjectName> setResult = getPartitionAssignmentObjectName(sService, sDomainPartition);

        String sFQN = null;

        for (Object oResult : setResult)
            {
            sFQN = oResult.toString();
            break;
            }

        return (String) invoke(new ObjectName(sFQN), "reportScheduledDistributions",
                new Object[]{true}, new String[]{boolean.class.getName()});
        }

    @Override
    public Set<Object[]> getPartitionAssignmentAttributes(String sService, String sDomainPartition)
            throws Exception
        {
        // look up the full name of the MBean in case we are in container
        Set<ObjectName> setResult = getPartitionAssignmentObjectName(sService, sDomainPartition);

        String sFQN = null;

        for (Object oResult : setResult)
            {
            sFQN = oResult.toString();
            break;


            }
        return JMXUtils.runJMXQuery(f_connection, sFQN, new JMXUtils.JMXField[]{
                new JMXUtils.Attribute("AveragePartitionSizeKB"),
                new JMXUtils.Attribute("MaxPartitionSizeKB"),
                new JMXUtils.Attribute("AverageStorageSizeKB"),
                new JMXUtils.Attribute("MaxStorageSizeKB"),
                new JMXUtils.Attribute("MaxLoadNodeId")});
        }

    @Override
    public void invokeFederationOperation(String sService, String sOperation, String sParticipant)
            throws Exception
        {
        String sObjName = getFederationManagerObjectName(sService);
        invoke(new ObjectName(sObjName), sOperation, new Object[]{sParticipant}, new String[]{String.class.getName()});
        }

    @Override
    public Integer retrievePendingIncomingMessages(String sService)
            throws Exception
        {
        return (Integer) invoke(new ObjectName(getFederationManagerObjectName(sService)),
                "retrievePendingIncomingMessages", new Object[]{}, new String[]{});
        }

    @Override
    public Integer retrievePendingOutgoingMessages(String sService)
            throws Exception
        {
        return (Integer) invoke(new ObjectName(getFederationManagerObjectName(sService)),
                "retrievePendingOutgoingMessages", new Object[]{}, new String[]{});
        }

    @Override
    public String getNodeState(Integer nNodeId)
            throws Exception
        {
        // look up the full name of the MBean in case we are in container
        Set<ObjectName> setResult = getCompleteObjectName(
                new ObjectName("Coherence:type=Node,nodeId=" + nNodeId + ",*"));

        String sFQN = null;

        for (Object oResult : setResult)
            {
            sFQN = oResult.toString();
            break;
            }

        return (String) invoke(new ObjectName(sFQN), "reportNodeState", new Object[0], new String[0]);
        }

    @Override
    public String[] getSnapshots(String sService, String sDomainPartition)
            throws Exception
        {
        String sServiceName = sDomainPartition == null
                ? sService
                : PersistenceData.getFullServiceName(sDomainPartition, sService);
        Set<ObjectName> setResult = getCompleteObjectName(new ObjectName(PersistenceData.getMBeanName(sServiceName)));

        String sFQN = null;

        for (Object oResult : setResult)
            {
            sFQN = oResult.toString();
            break;
            }

        return (String[]) f_connection.getAttribute(new ObjectName(sFQN), "Snapshots");
        }

    @Override
    public String[] getArchivedSnapshots(String sService, String sDomainPartition)
            throws Exception
        {
        Set<ObjectName> setResult = getCompleteObjectName(new ObjectName(PersistenceData.getMBeanName(
                PersistenceData.getFullServiceName(sDomainPartition, sService))));

        String sFQN = null;

        for (Object oResult : setResult)
            {
            sFQN = oResult.toString();
            break;
            }

        return (String[]) invoke(new ObjectName(sFQN), "listArchivedSnapshots", null, null);
        }

    @Override
    public void executePersistenceOperation(String sService,
                                            String sDomainPartition,
                                            String sOperationName,
                                            String sSnapshotName)
            throws Exception
        {
        ObjectName      objectName = new ObjectName(PersistenceData.getMBeanName(PersistenceData.getFullServiceName(sDomainPartition, sService)));
        Set<ObjectName> setResult  = getCompleteObjectName(objectName);

        String sFQN = null;

        for (Object oResult : setResult)
            {
            sFQN = oResult.toString();
            break;
            }
        invoke(new ObjectName(sFQN), sOperationName, new Object[]{sSnapshotName},
                new String[]{String.class.getName()});
        }

    // ------ JMXRequestSender methods --------------------------------------

    /**
     * Retrieve the Reporter MBean for the local member Id. We do a query to get the object
     * as it may have additional key values due to a container environment.
     *
     * @param server the {@link MBeanServerConnection} to use to query
     *
     * @return the reporter for the local member Id
     */
    public String getReporterObjectName(MBeanServerConnection server, int nLocalMemberId)
        {
        String sQuery  = "Coherence:type=Reporter,nodeId=" + nLocalMemberId + ",*";
        try
            {
            Set<ObjectName> setResult = server.queryNames(new ObjectName(sQuery), null);
            for (Object oResult : setResult)
                {
                return oResult.toString();
                }
            }
        catch (Exception e)
            {
            throw new RuntimeException("Unable to obtain reporter for nodeId=" + nLocalMemberId +
                    ": " + e.getMessage());
            }
        return null;
        }

    /**
     * Retrieve the local member id from the Coherence Cluster MBean known as
     * Coherence:type=Cluster.
     *
     * @return the local member id or 0 it no Coherence
     */
    public int getLocalMemberId()
        {
        int memberId = 0;

        try
            {
            memberId = (Integer) JMXUtils.runJMXQuerySingleResult(f_connection, "Coherence:type=Cluster,*",
                    new JMXUtils.Attribute("LocalMemberId"));
            }
        catch (Exception e)
            {
            LOGGER.log(Level.WARNING, Localization.getLocalText("ERR_local_member", new String[] {e.getMessage()}));
            }

        return memberId;
        }

    /**
     * Remove a notification listener.
     *
     * @param objName   the MBean ObjectName
     * @param listener  the JMX listener
     *
     * @throws Exception thrown in case of errors
     */
    public void removeNotificationListener(ObjectName objName, NotificationListener listener)
            throws Exception
        {
        f_connection.removeNotificationListener(objName, listener);
        }


    /**
     * Add a JMX notification for the operations which are triggered on the provided MBean.
     *
     * @param objName   the MBean ObjectName
     * @param listener  the JMX listener
     * @param filter    the filter
     * @param handback  the handbacl
     *
     * @throws Exception thrown in case of errors
     */
    public void addNotificationListener(ObjectName           objName,
                                        NotificationListener listener,
                                        NotificationFilter   filter,
                                        Object               handback)
            throws Exception
        {
        f_connection.addNotificationListener(objName, listener, filter, handback);
        }

    /**
     * Return the fully qualified ObjectName for the ReporterMBean.
     *
     * @param nLocalMemberId  the member id on which the reporter is running
     *
     * @return the fully qualified ObjectName
     */
    public String getReporterObjectName(int nLocalMemberId)
        {
        return getReporterObjectName(f_connection, nLocalMemberId);
        }

    /**
     * Invoke an operation on an MBean.
     *
     * @param objectName  the ObjectName of the MBean
     * @param opName      the operation name
     * @param arguments   the arguments to the operation
     * @param signature   the signature of the operation
     *
     * @return the result of the MBean operation
     *
     * @throws Exception thrown in case of errors
     */
    public Object invoke(ObjectName objectName, String opName, Object[] arguments, String[] signature)
            throws Exception
        {
        return f_connection.invoke(objectName, opName, arguments, signature);
        }

    /**
     * Helper method to get MBean's object name
     *
     * @param  sService   service name
     *
     * @return  the object name of the MBean
     */
    protected String getFederationManagerObjectName(String sService)
            throws Exception
        {
        String sObjName = null;
        String sQuery = "Coherence:type=Federation,service="
                + sService + ",responsibility=Coordinator,*";

        // look up the full name of the MBean in case we are in container
        Set<ObjectName> setResult = getCompleteObjectName(new ObjectName(sQuery));

        for (Object oResult : setResult)
            {
            return oResult.toString();
            }
        return null;
        }

    // ------ constants -----------------------------------------------------

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(JMXRequestSender.class.getName());

    // ------ data members --------------------------------------------------

    /**
     * The {@link MBeanServerConnection} to use.
     */
    private final MBeanServerConnection f_connection;
    }
