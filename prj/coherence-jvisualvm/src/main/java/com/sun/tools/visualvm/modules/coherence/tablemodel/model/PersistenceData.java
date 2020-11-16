/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.tablemodel.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sun.tools.visualvm.modules.coherence.VisualVMModel;

import com.sun.tools.visualvm.modules.coherence.helper.GraphHelper;
import com.sun.tools.visualvm.modules.coherence.helper.HttpRequestSender;
import com.sun.tools.visualvm.modules.coherence.helper.RequestSender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeList;
import javax.management.ObjectName;

import static com.sun.tools.visualvm.modules.coherence.helper.JMXUtils.*;

/**
 * A class to hold persistence data.
 *
 * @author tam  2013.11.14
 * @since  12.1.3
 */
public class PersistenceData
        extends AbstractData
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create PersistenceData passing in the number of columns.
     */
    public PersistenceData()
        {
        super(STATUS + 1);
        }

    // ----- DataRetriever methods ------------------------------------------

    /**
     * {@inheritDoc}
     */
    public List<Map.Entry<Object, Data>> getJMXData(RequestSender requestSender, VisualVMModel model)
        {
        SortedMap<Object, Data> mapData = new TreeMap<>();
        Data                    data;

        String sServiceName;

        if (!model.is1213AndAbove())
            {
            return null;
            }

        // erase the known domain partitions
        Set<String> setDomainPartitions = model.getDomainPartitions();
        setDomainPartitions.clear();

        try
            {
            if (requestSender instanceof HttpRequestSender)
                {
                return new ArrayList<>(getAggregatedDataFromHttpQuerying(model, (HttpRequestSender) requestSender).entrySet());
                }

            // firstly obtain the list of services that have PersistenceMode
            // not 'n/a'
            Set<ObjectName> servicesSet = requestSender.getAllServiceMembers();

            for (Iterator<ObjectName> serviceNameIter = servicesSet.iterator(); serviceNameIter.hasNext(); )
                {
                ObjectName serviceNameObjName = serviceNameIter.next();

                AttributeList listAttr = requestSender.getAttributes(serviceNameObjName,
                        new String[] { "PersistenceMode", "StorageEnabled" });
                String     sPersistenceMode   = getAttributeValueAsString(listAttr, "PersistenceMode");
                boolean    fStorageEnabled    = Boolean.parseBoolean(getAttributeValueAsString(listAttr, "StorageEnabled"));

                // only include if PersistenceMode != 'n/a'
                if (!"n/a".equals(sPersistenceMode) && fStorageEnabled)
                    {
                    sServiceName            = serviceNameObjName.getKeyProperty("name");
                    String sDomainPartition = serviceNameObjName.getKeyProperty("domainPartition");

                    if (sDomainPartition != null && !setDomainPartitions.contains(sDomainPartition))
                        {
                        setDomainPartitions.add(sDomainPartition);
                        }

                    // check for domain partition
                    if (sDomainPartition != null)
                        {
                        sServiceName = ServiceData.getFullServiceName(sDomainPartition, sServiceName);
                        }

                    // try to get data
                    data = mapData.get(sServiceName);

                    if (data == null)
                        {
                        mapData.put(sServiceName, createData(sServiceName, sPersistenceMode, sDomainPartition, requestSender));
                        }
                    }
                }

            // now loop through each individual service

            for (Iterator<Object> serviceNameIter = mapData.keySet().iterator(); serviceNameIter.hasNext(); )
                {
                String sServiceNameKey  = (String) serviceNameIter.next();
                String[] asParts        = ServiceData.getServiceParts(sServiceNameKey);
                String sDomainPartition = asParts.length == 2 ? asParts[0] : null;
                sServiceName            = sDomainPartition == null ? sServiceNameKey : asParts[1];

                // select only the current service so we can determine the number of storage-enabled
                // members.
                Set<ObjectName> resultSet = requestSender.getMembersOfService(sServiceName, sDomainPartition);

                int cNodes = 0;
                for (Iterator<ObjectName> iter = resultSet.iterator(); iter.hasNext(); )
                    {
                    ObjectName objectName = (ObjectName) iter.next();

                    // only include storage-enabled members
                    AttributeList listAttr = requestSender.getAttributes(objectName,
                            new String[] { "StorageEnabled", "PersistenceActiveSpaceUsed", "PersistenceLatencyMax", "PersistenceLatencyAverage" });

                    if (Boolean.parseBoolean(getAttributeValueAsString(listAttr, "StorageEnabled")))
                        {
                        data = (PersistenceData) mapData.get(sServiceNameKey);

                        // PersistenceActiveSpaceUsed is only valid for active persistence mode
                        if ("active".equals(data.getColumn(PERSISTENCE_MODE)))
                            {
                            data.setColumn(TOTAL_ACTIVE_SPACE_USED,
                                           (Long) data.getColumn(TOTAL_ACTIVE_SPACE_USED)
                                           + Long.parseLong(getAttributeValueAsString(listAttr, "PersistenceActiveSpaceUsed")));
                            }

                        // update the max (of the max latencies)
                        long nMaxLatency = Long.parseLong(getAttributeValueAsString(listAttr, "PersistenceLatencyMax"));

                        if (nMaxLatency > (Long) data.getColumn(MAX_LATENCY))
                            {
                            data.setColumn(MAX_LATENCY, nMaxLatency);
                            }

                        // count the nodes for working out the average at the end as we are adding up
                        // all of the averages, putting them in the average latency
                        // and average them
                        cNodes++;
                        data.setColumn(AVERAGE_LATENCY,
                                       (Float) data.getColumn(AVERAGE_LATENCY)
                                       + Float.parseFloat(getAttributeValueAsString(listAttr, "PersistenceLatencyAverage")));

                        mapData.put(sServiceNameKey, data);
                        }
                    }

                data = (PersistenceData) mapData.get(sServiceNameKey);

                // update the average of the averages only if > 1 service node
                if (cNodes > 0)
                    {
                    data.setColumn(AVERAGE_LATENCY,
                                   (Float) data.getColumn(AVERAGE_LATENCY) / cNodes);
                    }

                data.setColumn(TOTAL_ACTIVE_SPACE_USED_MB,
                               (Long) data.getColumn(TOTAL_ACTIVE_SPACE_USED) / GraphHelper.MB);

                mapData.put(sServiceNameKey, data);
                }

            return new ArrayList<Map.Entry<Object, Data>>(mapData.entrySet());
            }
        catch (Exception e)
            {
            LOGGER.log(Level.WARNING, "Error getting service statistics", e);

            return null;
            }
        }

    /**
     * {@inheritDoc }
     */
    public String getReporterReport()
        {
        return null;    // see comment below
        }

    /**
     * {@inheritDoc}
     */
    public Data processReporterData(Object[] aoColumns, VisualVMModel model)
        {
        // The reporter does not support running a method on an extracted value and therefore
        // we are not able to extract the size of the Snapshots String[] on the
        // Coherence:type=PersistenceSnapshot entry. If we were to do this with the reporter
        // we should have to issue a separate call to query JMX for the snapshot count.
        return null;
        }

    @Override
    public SortedMap<Object, Data> getAggregatedDataFromHttpQuerying(VisualVMModel model, HttpRequestSender requestSender)
            throws Exception
        {
        JsonNode    allStorageMembers   = requestSender.getAllStorageMembers();
        JsonNode    serviceItemsNode    = allStorageMembers.get("items");
        Set<String> setDomainPartitions = model.getDomainPartitions();

        Map<String,Integer>     mapNodeCount = new HashMap<>();
        SortedMap<Object, Data> mapData = new TreeMap<>();
        Data                    data;

        if (serviceItemsNode != null && serviceItemsNode.isArray())
            {
            for (int i = 0; i < ((ArrayNode) serviceItemsNode).size(); i++)
                {
                JsonNode details = serviceItemsNode.get(i);
                String sPersistenceMode = details.get("persistenceMode").asText();
                if (details.get("storageEnabled").asBoolean() && !"n/a".equals(sPersistenceMode))
                    {
                    String   sServiceName    = details.get("name").asText();
                    JsonNode domainPartition = details.get("domainPartition");
                    String sDomainPartition  = domainPartition == null ? null : domainPartition.asText();

                    if (sDomainPartition != null && !setDomainPartitions.contains(sDomainPartition))
                        {
                        setDomainPartitions.add(sDomainPartition);
                        }

                    // check for domain partition
                    if (sDomainPartition != null)
                        {
                        sServiceName = ServiceData.getFullServiceName(sDomainPartition, sServiceName);
                        }

                    // try to get data
                    data = mapData.get(sServiceName);

                    if (data == null)
                        {
                        data = createData(sServiceName, sPersistenceMode, sDomainPartition, requestSender);
                        mapData.put(sServiceName, data);
                        mapNodeCount.put(sServiceName, 0);
                        }

                    // update the details for each node
                    // PersistenceActiveSpaceUsed is only valid for active persistence mode
                    if ("active".equals(sPersistenceMode))
                        {
                        long nPersistenceActiveSpaceUsed = details.get("persistenceActiveSpaceUsed").asLong();
                        data.setColumn(TOTAL_ACTIVE_SPACE_USED,
                                (Long) data.getColumn(TOTAL_ACTIVE_SPACE_USED) + nPersistenceActiveSpaceUsed);
                        }

                    // update the max (of the max latencies)
                    long  nMaxLatency                = details.get("persistenceLatencyMax").asLong();
                    float nPersistenceLatencyAverage = (float) details.get("persistenceLatencyAverage").asDouble();

                    if (nMaxLatency > (Long) data.getColumn(PersistenceData.MAX_LATENCY))
                        {
                        data.setColumn(PersistenceData.MAX_LATENCY, nMaxLatency);
                        }

                    // count the nodes for working out the average at the end as we are adding up
                    // all of the averages, putting them in the average latency
                    // and average them
                    mapNodeCount.compute(sServiceName, (k,v) -> v + 1);
                    data.setColumn(AVERAGE_LATENCY,
                                   (Float) data.getColumn(AVERAGE_LATENCY)
                                   + nPersistenceLatencyAverage);

                    mapData.put(sServiceName, data);
                    }
                }

            // post processing to work out average of latencies and to convert space to MB
            for (Map.Entry<Object, Data> entry : mapData.entrySet())
                {
                Object k = entry.getKey();
                Data   v = entry.getValue();

                v.setColumn(TOTAL_ACTIVE_SPACE_USED_MB,
                           (Long) v.getColumn(TOTAL_ACTIVE_SPACE_USED) / GraphHelper.MB);

                int cNodes = mapNodeCount.get(k);
                // update the average of the averages only if > 1 service node
                if (cNodes > 0)
                    {
                    v.setColumn(AVERAGE_LATENCY,
                                   (Float) v.getColumn(AVERAGE_LATENCY) / cNodes);
                    }
                }
            }
        return mapData;
        }

    /**
     * Create a new {@link Data} entry for Persistence
     * @param sServiceName      service name
     * @param sPersistenceMode  persistence mode
     * @param sDomainPartition  domain partition
     * @param requestSender     {@link RequestSender} to use
     * @return a new {@link Data}
     * @throws Exception in case of errors.
     */
    private Data createData(String sServiceName, String sPersistenceMode, String sDomainPartition,
                            RequestSender requestSender) throws Exception {
        // create the entry
        String sStatus = "";
        Data data = new PersistenceData();

        data.setColumn(SERVICE_NAME, sServiceName);
        data.setColumn(PERSISTENCE_MODE, sPersistenceMode);
        data.setColumn(TOTAL_ACTIVE_SPACE_USED, 0L);
        data.setColumn(MAX_LATENCY, 0L);
        data.setColumn(AVERAGE_LATENCY, 0.0f);

        String[] asParts  = ServiceData.getServiceParts(sServiceName);
        String   sService = asParts.length == 2 ? asParts[1] : sServiceName;

        // get the number of snapshots from the PersistenceManager MBean
        String[] asSnapshots = requestSender.getSnapshots(sService, sDomainPartition);

        // get the current operation status from PersistenceManager MBean
        try
            {
            Set<ObjectName> setResult = requestSender
                    .getCompleteObjectName(new ObjectName(getMBeanName(sServiceName)));

            String sFQN = null;

            for (Object oResult : setResult)
                {
                sFQN = oResult.toString();
                break;
                }

            sStatus = (String) requestSender.getAttribute(new ObjectName(sFQN), "OperationStatus");
            }
        catch (Exception e)
            {
            // if we get here then its likely we are connected to a 12.1.3 cluster where
            // persistence was experimental, or an early 12.2.1 cluster before CurrentOperationStatus
            // was renamed to OperationStatus
            sStatus = (String) requestSender.getAttribute(
                    new ObjectName(getMBeanName(sServiceName)), "CurrentOperationStatus");
            }

        data.setColumn(SNAPSHOT_COUNT, asSnapshots == null ? 0 : asSnapshots.length);
        data.setColumn(STATUS, sStatus);

        return data;
    }

    // ----- helpers --------------------------------------------------------

    /**
     * Return the PersistenceManager MBean name. If the service name is prefixed
     * with a domain partition then use that in the key as well.
     *
     * @param sServiceName  the service name to use to construct the name
     *
     * @return the PersistenceManager MBean name
     */
    public static String getMBeanName(String sServiceName)
        {
        String[] asParts          = ServiceData.getServiceParts(sServiceName);
        String   sService         = asParts.length == 2 ? asParts[1] : sServiceName;
        String   sDomainPartition = asParts.length == 2 ? asParts[0] : null;
        
        if ("null".equals(sDomainPartition))
            {
            sDomainPartition = null;
            }

        return "Coherence:" + PERSISTENCE_MBEAN + ",service=" + sService +
                (sDomainPartition != null ? ",domainPartition=" + asParts[0] : "")
                + "," + PERSISTENCE_COORDINATOR +",*";
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = 7769559573242105947L;

    /**
     * MBean type for Persistence Manager.
     */
    public static final String PERSISTENCE_MBEAN = "type=Persistence";

    /**
     * Persistence Coordinator responsibility.
     */
    public static final String PERSISTENCE_COORDINATOR = "responsibility=PersistenceCoordinator";

    /**
     * Array index for service name.
     */
    public static final int SERVICE_NAME = 0;

    /**
     * Array index for persistence mode.
     */
    public static final int PERSISTENCE_MODE = 1;

    /**
     * Array index for total active space used.
     */
    public static final int TOTAL_ACTIVE_SPACE_USED = 2;

    /**
     * Array index for total active space used MB.
     */
    public static final int TOTAL_ACTIVE_SPACE_USED_MB = 3;

    /**
     * Array index for average latency
     */
    public static final int AVERAGE_LATENCY = 4;

    /**
     * Array index for max latency.
     */
    public static final int MAX_LATENCY = 5;

    /**
     * Array index for number of snapshots.
     */
    public static final int SNAPSHOT_COUNT = 6;

    /**
     * Array index for status.
      */
    public static final int STATUS = 7;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(PersistenceData.class.getName());
    }
