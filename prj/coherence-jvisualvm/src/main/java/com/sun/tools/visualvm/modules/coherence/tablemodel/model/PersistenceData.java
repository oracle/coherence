/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.tablemodel.model;

import com.sun.tools.visualvm.modules.coherence.VisualVMModel;

import com.sun.tools.visualvm.modules.coherence.helper.GraphHelper;
import com.sun.tools.visualvm.modules.coherence.helper.HttpRequestSender;
import com.sun.tools.visualvm.modules.coherence.helper.JMXUtils;
import com.sun.tools.visualvm.modules.coherence.helper.RequestSender;

import java.util.ArrayList;
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
        SortedMap<Object, Data> mapData = new TreeMap<Object, Data>();
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
            // firstly obtain the list of services that have PersistenceMode
            // not 'n/a'
            Set<ObjectName> servicesSet = requestSender.getAllServiceMembers();

            for (Iterator<ObjectName> serviceNameIter = servicesSet.iterator(); serviceNameIter.hasNext(); )
                {
                ObjectName serviceNameObjName = serviceNameIter.next();

                AttributeList listAttr = requestSender.getAttributes(serviceNameObjName,
                        new String[] { "PersistenceMode", "StorageEnabled" });
                String     sPersistenceMode   = JMXUtils.getAttributeValueAsString(listAttr, "PersistenceMode");
                boolean    fStorageEnabled    = Boolean.parseBoolean(JMXUtils.getAttributeValueAsString(listAttr, "StorageEnabled"));

                // only include if PersistenceMode != 'n/a'
                if (!"n/a".equals(sPersistenceMode) && fStorageEnabled)
                    {
                    data = null;

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
                    data = (PersistenceData) mapData.get(sServiceName);

                    if (data == null)
                        {
                        // create the entry
                        String sStatus = "";
                        data = new PersistenceData();

                        data.setColumn(PersistenceData.SERVICE_NAME, sServiceName);
                        data.setColumn(PersistenceData.PERSISTENCE_MODE, sPersistenceMode);
                        data.setColumn(PersistenceData.TOTAL_ACTIVE_SPACE_USED, new Long(0L));
                        data.setColumn(PersistenceData.MAX_LATENCY, new Long(0L));
                        data.setColumn(PersistenceData.AVERAGE_LATENCY, new Float(0.0f));

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
                                sFQN =  oResult.toString();
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

                        data.setColumn(PersistenceData.SNAPSHOT_COUNT, asSnapshots == null ? 0 : asSnapshots.length);
                        data.setColumn(PersistenceData.STATUS, sStatus);

                        mapData.put(sServiceName, data);
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

                data = null;

                for (Iterator<ObjectName> iter = resultSet.iterator(); iter.hasNext(); )
                    {
                    ObjectName objectName = (ObjectName) iter.next();

                    // only include storage-enabled members
                    AttributeList listAttr = requestSender.getAttributes(objectName,
                            new String[] { "StorageEnabled", "PersistenceActiveSpaceUsed", "PersistenceLatencyMax", "PersistenceLatencyAverage" });

                    if (Boolean.parseBoolean(JMXUtils.getAttributeValueAsString(listAttr, "StorageEnabled")))
                        {
                        data = (PersistenceData) mapData.get(sServiceNameKey);

                        // PersistenceActiveSpaceUsed is only valid for active persistence mode
                        if ("active".equals(data.getColumn(PersistenceData.PERSISTENCE_MODE)))
                            {
                            data.setColumn(PersistenceData.TOTAL_ACTIVE_SPACE_USED,
                                           (Long) data.getColumn(PersistenceData.TOTAL_ACTIVE_SPACE_USED)
                                           + Long.parseLong(JMXUtils.getAttributeValueAsString(listAttr, "PersistenceActiveSpaceUsed")));
                            }

                        // update the max (of the max latencies)
                        long nMaxLatency = Long.parseLong(JMXUtils.getAttributeValueAsString(listAttr, "PersistenceLatencyMax"));

                        if (nMaxLatency > (Long) data.getColumn(PersistenceData.MAX_LATENCY))
                            {
                            data.setColumn(PersistenceData.MAX_LATENCY, new Long(nMaxLatency));
                            }

                        // count the nodes for working out the average at the end as we are adding up
                        // all of the averages, putting them in the average latency
                        // and average them
                        cNodes++;
                        data.setColumn(PersistenceData.AVERAGE_LATENCY,
                                       (Float) data.getColumn(PersistenceData.AVERAGE_LATENCY)
                                       + Float.parseFloat(JMXUtils.getAttributeValueAsString(listAttr, "PersistenceLatencyAverage")));

                        mapData.put(sServiceNameKey, data);
                        }
                    }

                data = (PersistenceData) mapData.get(sServiceNameKey);

                // update the average of the averages only if > 1 service node
                if (cNodes > 0)
                    {
                    data.setColumn(PersistenceData.AVERAGE_LATENCY,
                                   (Float) data.getColumn(PersistenceData.AVERAGE_LATENCY) / cNodes);
                    }

                data.setColumn(PersistenceData.TOTAL_ACTIVE_SPACE_USED_MB,
                               (Long) data.getColumn(PersistenceData.TOTAL_ACTIVE_SPACE_USED) / GraphHelper.MB);

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
        // no reports being used, hence using default functionality provided in getJMXData
        return null;
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
