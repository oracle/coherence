/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.tablemodel.model;

import com.fasterxml.jackson.databind.JsonNode;

import com.sun.tools.visualvm.modules.coherence.VisualVMModel;

import com.sun.tools.visualvm.modules.coherence.helper.HttpRequestSender;
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

import static com.sun.tools.visualvm.modules.coherence.helper.JMXUtils.getAttributeValueAsString;

/**
 * A class to hold detailed service member data for a selected service.
 *
 * @author tam  2013.11.14
 * @since  12.1.3
 */
public class ServiceMemberData
        extends AbstractData
    {
    /**
     * Create ServiceMemberData passing in the number of columns.
     */
    public ServiceMemberData()
        {
        super(REQUEST_AVERAGE_DURATION + 1);
        }

    // ----- DataRetriever methods ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public List<Map.Entry<Object, Data>> getJMXData(RequestSender sender, VisualVMModel model)
        {
        String sSelectedService = model.getSelectedService();
        Data   data;

        if (sSelectedService != null)
            {
            SortedMap<Object, Data> mapData = new TreeMap<Object, Data>();
            String[] asParts                = ServiceData.getServiceParts(sSelectedService);
            String   sDomainPartition       = asParts.length == 1 ? null : asParts[0];

            sSelectedService = sDomainPartition == null ? sSelectedService : asParts[1];

            try
                {
                Set<ObjectName> servicesSet = sender.getMembersOfService(sSelectedService, sDomainPartition);

                for (Iterator<ObjectName> serviceNameIter = servicesSet.iterator(); serviceNameIter.hasNext(); )
                    {
                    ObjectName serviceNameObjName = serviceNameIter.next();

                    int    nodeId = Integer.valueOf(serviceNameObjName.getKeyProperty("nodeId"));

                    // ignore if have no domain partition in selected service and we cant find the domain partition key
                    if (sDomainPartition == null && serviceNameObjName.getKeyProperty("domainPartition") != null)
                        {
                        continue;
                        }

                    data = new ServiceMemberData();

                    AttributeList listAttr = sender.getAttributes(serviceNameObjName,
                        new String[] { ATTR_TASK_BACKLOG, ATTR_THREAD_COUNT, ATTR_THREAD_IDLE_COUNT,
                                       ATTR_REQ_AVG_DUR, ATTR_TASK_AVG_DUR });

                    data.setColumn(ServiceMemberData.NODE_ID, nodeId);
                    data.setColumn(ServiceMemberData.TASK_BACKLOG, Integer.parseInt(getAttributeValueAsString(listAttr, ATTR_TASK_BACKLOG)));
                    data.setColumn(ServiceMemberData.THREAD_COUNT, Integer.parseInt(getAttributeValueAsString(listAttr, ATTR_THREAD_COUNT)));
                    data.setColumn(ServiceMemberData.THREAD_IDLE_COUNT, Integer.parseInt(getAttributeValueAsString(listAttr, ATTR_THREAD_IDLE_COUNT)));
                    data.setColumn(ServiceMemberData.REQUEST_AVERAGE_DURATION, Float.parseFloat(getAttributeValueAsString(listAttr, ATTR_REQ_AVG_DUR)));
                    data.setColumn(ServiceMemberData.TASK_AVERAGE_DURATION, Float.parseFloat(getAttributeValueAsString(listAttr, ATTR_TASK_AVG_DUR)));

                    if ((Integer) data.getColumn(ServiceMemberData.THREAD_COUNT) > 0)
                        {
                        float threadUtil = ((float) ((Integer) data.getColumn(ServiceMemberData.THREAD_COUNT)
                                                     - (Integer) data
                                                         .getColumn(ServiceMemberData
                                                             .THREAD_IDLE_COUNT))) / (Integer) data
                                                                 .getColumn(ServiceMemberData.THREAD_COUNT);

                        data.setColumn(ServiceMemberData.THREAD_UTILISATION_PERCENT, threadUtil);
                        }

                    mapData.put(nodeId, data);
                    }

                return new ArrayList<Map.Entry<Object, Data>>(mapData.entrySet());

                }
            catch (Exception e)
                {
                LOGGER.log(Level.WARNING, "Error getting service member statistics", e);

                return null;
                }
            }
        else
            {
            // no selected service
            return null;
            }
        }

    /**
     * {@inheritDoc}
     */
    public String getReporterReport()
        {
        return REPORT_SERVICE_MEMBER;
        }

    /**
     * {@inheritDoc}
     */
    public Data processReporterData(Object[] aoColumns, VisualVMModel model)
        {
        Data data = null;
        boolean fMT  = aoColumns[3] != null;
        int  nStart = 4;

        String  sServiceName = fMT ?
                ServiceData.getFullServiceName(aoColumns[3].toString(), aoColumns[2].toString()) :
                aoColumns[2].toString();

        // Service member data are only collected when the a service is selected.
        // we need to only include rows where the service matches
        if (model.getSelectedService() != null && model.getSelectedService().equals(sServiceName))
            {
            data = new ServiceMemberData();

            data.setColumn(ServiceMemberData.NODE_ID, new Integer(getNumberValue(aoColumns[nStart++].toString())));
            data.setColumn(ServiceMemberData.TASK_BACKLOG, new Integer(getNumberValue(aoColumns[nStart++].toString())));
            data.setColumn(ServiceMemberData.THREAD_COUNT, new Integer(getNumberValue(aoColumns[nStart++].toString())));
            data.setColumn(ServiceMemberData.THREAD_IDLE_COUNT, new Integer(getNumberValue(aoColumns[nStart++].toString())));
            data.setColumn(ServiceMemberData.REQUEST_AVERAGE_DURATION, new Float(aoColumns[nStart++].toString()));
            data.setColumn(ServiceMemberData.TASK_AVERAGE_DURATION, new Float(aoColumns[nStart++].toString()));

            if ((Integer) data.getColumn(ServiceMemberData.THREAD_COUNT) > 0)
                {
                float threadUtil = ((float) ((Integer) data.getColumn(ServiceMemberData.THREAD_COUNT)
                                             - (Integer) data
                                                 .getColumn(ServiceMemberData.THREAD_IDLE_COUNT))) / (Integer) data
                                                     .getColumn(ServiceMemberData.THREAD_COUNT);

                data.setColumn(ServiceMemberData.THREAD_UTILISATION_PERCENT, threadUtil);
                }
            }

        return data;
        }

    @Override
    public SortedMap<Object, Data> getAggregatedDataFromHttpQuerying(VisualVMModel model, HttpRequestSender requestSender) throws Exception
        {
        JsonNode                nodeRoot         = requestSender.getDataForServiceMembers();
        SortedMap<Object, Data> mapData          = new TreeMap<Object, Data>();
        JsonNode                nodeServiceItems = nodeRoot.get("items");

        if (nodeServiceItems != null && nodeServiceItems.isArray())
            {
            for (int i = 0; i < nodeServiceItems.size(); i++)
                {
                JsonNode nodeServiceMember   = nodeServiceItems.get(i);
                JsonNode domainPartitionNode = nodeServiceMember.get("domainPartition");
                String   sDomainPartition    = domainPartitionNode == null ? null : domainPartitionNode.asText();
                String   sName               = nodeServiceMember.get("name").asText();

                String sServiceName = sDomainPartition != null
                        ? ServiceData.getFullServiceName(sDomainPartition, sName)
                        : sName;

                // Service member data are only collected when the a service is selected.
                // we need to only include rows where the service matches
                if (model.getSelectedService() != null && model.getSelectedService().equals(sServiceName))
                    {
                    Data data = new ServiceMemberData();

                    data.setColumn(ServiceMemberData.NODE_ID,
                            Integer.valueOf(nodeServiceMember.get("nodeId").asText()));
                    data.setColumn(ServiceMemberData.TASK_BACKLOG,
                            Integer.valueOf(nodeServiceMember.get("taskBacklog").asText()));
                    data.setColumn(ServiceMemberData.THREAD_COUNT,
                            Integer.valueOf(nodeServiceMember.get("threadCount").asText()));
                    data.setColumn(ServiceMemberData.THREAD_IDLE_COUNT,
                            Integer.valueOf(nodeServiceMember.get("threadIdleCount").asText()));
                    data.setColumn(ServiceMemberData.REQUEST_AVERAGE_DURATION,
                            Float.valueOf(nodeServiceMember.get("requestAverageDuration").asText()));
                    data.setColumn(ServiceMemberData.TASK_AVERAGE_DURATION,
                            Float.valueOf(nodeServiceMember.get("taskAverageDuration").asText()));

                    if ((Integer) data.getColumn(ServiceMemberData.THREAD_COUNT) > 0)
                        {
                        float threadUtil = ((float) ((Integer) data.getColumn(ServiceMemberData.THREAD_COUNT)
                                - (Integer) data
                                .getColumn(ServiceMemberData.THREAD_IDLE_COUNT))) / (Integer) data
                                .getColumn(ServiceMemberData.THREAD_COUNT);

                        data.setColumn(ServiceMemberData.THREAD_UTILISATION_PERCENT, threadUtil);
                        }
                    mapData.put(data.getColumn(0), data);
                    }
                }
            }
        return mapData;
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = 255698662206697681L;

    /**
     * Array index for node id.
     */
    public static final int NODE_ID = 0;

    /**
     * Array index for thread count.
     */
    public static final int THREAD_COUNT = 1;

    /**
     * Array index for thread idle count.
     */
    public static final int THREAD_IDLE_COUNT = 2;

    /**
     * Array index for thread utilization percent.
     */
    public static final int THREAD_UTILISATION_PERCENT = 3;

    /**
     * Array index for task average duration.
     */
    public static final int TASK_AVERAGE_DURATION = 4;

    /**
     * Array index for task backlog.
     */
    public static final int TASK_BACKLOG = 5;

    /**
     * Array index for request average duration.
     */
    public static final int REQUEST_AVERAGE_DURATION = 6;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(ServiceMemberData.class.getName());

    /**
     * Report for service members.
     */
    public static final String REPORT_SERVICE_MEMBER = "reports/visualvm/service-member-stats.xml";

    /**
     * JMX attribute name for Task Backlog.
     */
    protected static final String ATTR_TASK_BACKLOG = "TaskBacklog";

    /**
     * JMX attribute name for Thread Count.
     */
    protected static final String ATTR_THREAD_COUNT = "ThreadCount";

    /**
     * JMX attribute name for Thread Idle Count.
     */
    protected static final String ATTR_THREAD_IDLE_COUNT = "ThreadIdleCount";

    /**
     * JMX attribute name for Request Average Duration.
     */
    protected static final String ATTR_REQ_AVG_DUR = "RequestAverageDuration";

    /**
     * JMX attribute name for Task Average Duration.
     */
    protected static final String ATTR_TASK_AVG_DUR = "TaskAverageDuration";
    }
