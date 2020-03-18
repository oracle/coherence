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
 * A class to hold basic member data.
 *
 * @author tam  2013.11.14
 * @since  12.1.3
 */
public class MemberData
        extends AbstractData
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create MemberData passing in the number of columns.
     *
     */
    public MemberData()
        {
        super(PRODUCT_EDITION + 1);
        }

    // ----- DataRetriever methods ------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Map.Entry<Object, Data>> getJMXData(RequestSender requestSender, VisualVMModel model)
        {
        SortedMap<Object, Data> mapData = new TreeMap<Object, Data>();
        Data                    data;

        try
            {
            Set<ObjectName> setNodeNames = requestSender.getAllClusterMembers();

            for (Iterator<ObjectName> nodIter = setNodeNames.iterator(); nodIter.hasNext(); )
                {
                ObjectName nodeNameObjName = (ObjectName) nodIter.next();

                Integer    nodeId          = Integer.valueOf(nodeNameObjName.getKeyProperty("nodeId"));

                data = new MemberData();

                AttributeList listAttr = requestSender.getAttributes(nodeNameObjName,
                  new String[] { ATTR_PUB_SUCCESS_RATE, ATTR_REC_SUCCESS_RATE, ATTR_MEM_MAX_MB,
                                 ATTR_MEM_AVAIL_MB, ATTR_SEND_Q_SIZE, ATTR_UNICAST_ADDR,
                                 ATTR_ROLE_NAME, ATTR_UNICAST_PORT, ATTR_PRODUCT_EDITION });

                data.setColumn(MemberData.NODE_ID, nodeId);
                data.setColumn(MemberData.PUBLISHER_SUCCESS, Float.parseFloat(getAttributeValueAsString(listAttr, ATTR_PUB_SUCCESS_RATE)));
                data.setColumn(MemberData.RECEIVER_SUCCESS, Float.parseFloat(getAttributeValueAsString(listAttr, ATTR_REC_SUCCESS_RATE)));
                data.setColumn(MemberData.MAX_MEMORY, Integer.parseInt(getAttributeValueAsString(listAttr, ATTR_MEM_MAX_MB)));
                data.setColumn(MemberData.FREE_MEMORY, Integer.parseInt(getAttributeValueAsString(listAttr , ATTR_MEM_AVAIL_MB)));
                data.setColumn(MemberData.SENDQ_SIZE, Integer.parseInt(getAttributeValueAsString(listAttr, ATTR_SEND_Q_SIZE)));

                data.setColumn(MemberData.USED_MEMORY,
                               (Integer) data.getColumn(MemberData.MAX_MEMORY)
                               - (Integer) data.getColumn(MemberData.FREE_MEMORY));

                data.setColumn(MemberData.ADDRESS, (String) getAttributeValueAsString(listAttr, ATTR_UNICAST_ADDR));

                data.setColumn(MemberData.ROLE_NAME, (String) getAttributeValueAsString(listAttr, ATTR_ROLE_NAME));
                data.setColumn(MemberData.PRODUCT_EDITION, (String) getAttributeValueAsString(listAttr, ATTR_PRODUCT_EDITION));
                data.setColumn(MemberData.PORT, Integer.parseInt(getAttributeValueAsString(listAttr, ATTR_UNICAST_PORT)));
                data.setColumn(MemberData.STORAGE_ENABLED, "true");

                mapData.put(nodeId, data);
                }
            return new ArrayList<Map.Entry<Object, Data>>(mapData.entrySet());

            }
        catch (Exception e)
            {
            LOGGER.log(Level.WARNING, "Error getting member statistics", e);

            return null;
            }
        }

    /**
     * {@inheritDoc}
     */
    public String getReporterReport()
        {
        return REPORT_MEMBERS;
        }

    /**
     * {@inheritDoc}
     */
    public Data processReporterData(Object[] aoColumns, VisualVMModel model)
        {
        Data data = new MemberData();

        data.setColumn(MemberData.NODE_ID, new Integer(getNumberValue(aoColumns[2].toString())));
        data.setColumn(MemberData.PUBLISHER_SUCCESS, new Float(aoColumns[3].toString()));
        data.setColumn(MemberData.RECEIVER_SUCCESS, new Float(aoColumns[4].toString()));

        data.setColumn(MemberData.SENDQ_SIZE, new Integer(getNumberValue(aoColumns[5].toString())));

        data.setColumn(MemberData.MAX_MEMORY, new Integer(getNumberValue(aoColumns[6].toString())));
        data.setColumn(MemberData.FREE_MEMORY, new Integer(getNumberValue(aoColumns[7].toString())));

        data.setColumn(MemberData.USED_MEMORY,
                       (Integer) data.getColumn(MemberData.MAX_MEMORY)
                       - (Integer) data.getColumn(MemberData.FREE_MEMORY));

        data.setColumn(MemberData.ADDRESS, aoColumns[8].toString());

        data.setColumn(MemberData.ROLE_NAME, aoColumns[9].toString());
        data.setColumn(MemberData.PORT, new Integer(getNumberValue(aoColumns[10].toString())));
        data.setColumn(MemberData.PRODUCT_EDITION, aoColumns[14].toString());
        data.setColumn(MemberData.STORAGE_ENABLED, "true");

        return data;
        }

    @Override
    public SortedMap<Object, Data> getAggregatedDataFromHttpQuerying(VisualVMModel model, HttpRequestSender requestSender)
            throws Exception
        {
        JsonNode                rootClusterMembers = requestSender.getListOfClusterMembers();
        SortedMap<Object, Data> mapData         = new TreeMap<Object, Data>();

        JsonNode itemsNode = rootClusterMembers.get("items");

        if (itemsNode != null && itemsNode.isArray())
            {
            for (int i = 0; i < ((ArrayNode) itemsNode).size(); i++)
                {
                JsonNode clusterMember = (JsonNode) itemsNode.get(i);

                Data data = new MemberData();

                data.setColumn(MemberData.NODE_ID, Integer.valueOf(clusterMember.get("nodeId").asText()));
                data.setColumn(MemberData.PUBLISHER_SUCCESS,
                        Float.valueOf(clusterMember.get("publisherSuccessRate").asText()));
                data.setColumn(MemberData.RECEIVER_SUCCESS,
                        Float.valueOf(clusterMember.get("receiverSuccessRate").asText()));
                data.setColumn(MemberData.SENDQ_SIZE,
                        Integer.valueOf(getNumberValue(clusterMember.get("sendQueueSize").asText())));

                data.setColumn(MemberData.MAX_MEMORY,
                        Integer.valueOf(getNumberValue(clusterMember.get("memoryMaxMB").asText())));
                data.setColumn(MemberData.FREE_MEMORY,
                        Integer.valueOf(getNumberValue(clusterMember.get("memoryAvailableMB").asText())));

                data.setColumn(MemberData.USED_MEMORY,
                        (Integer) data.getColumn(MemberData.MAX_MEMORY)
                                - (Integer) data.getColumn(MemberData.FREE_MEMORY));
                data.setColumn(MemberData.ADDRESS, clusterMember.get("unicastAddress").asText());
                data.setColumn(MemberData.ROLE_NAME, clusterMember.get("roleName").asText());
                data.setColumn(MemberData.PRODUCT_EDITION, clusterMember.get("productEdition").asText());
                data.setColumn(MemberData.PORT, Integer.valueOf(getNumberValue(clusterMember.get("unicastPort").asText())));
                data.setColumn(MemberData.STORAGE_ENABLED, "true");
                mapData.put(data.getColumn(0), data);
                }
            }
        return mapData;
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = -1079052422314214578L;

    /**
     * Array index for node id.
     */
    public static int NODE_ID = 0;

    /**
     * Array index for address.
     */
    public static int ADDRESS = 1;

    /**
     * Array index for machine name.
     */
    public static int PORT = 2;

    /**
     * Array index for role name.
     */
    public static int ROLE_NAME = 3;

    /**
     * Array index for publisher success rate.
     */
    public static int PUBLISHER_SUCCESS = 4;

    /**
     * Array index for receiver success rate.
     */
    public static int RECEIVER_SUCCESS = 5;

    /**
     * Array index for send queue size.
     */
    public static int SENDQ_SIZE = 6;

    /**
     * Array index for max memory.
     */
    public static int MAX_MEMORY = 7;

    /**
     * Array index for used memory.
     */
    public static int USED_MEMORY = 8;

    /**
     * Array index for free memory.
     */
    public static int FREE_MEMORY = 9;

    /**
     * Array index for storage-enabled.
     */
    public static int STORAGE_ENABLED = 10;

    /**
     * Array index for ProductEdition.
     */
    public static int PRODUCT_EDITION = 11;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(MemberData.class.getName());

    /**
     * Report for node data.
     */
    public static final String REPORT_MEMBERS = "reports/visualvm/member-stats.xml";

    /**
     * JMX attribute name for Publisher Success Rate.
     */
    private static final String ATTR_PUB_SUCCESS_RATE = "PublisherSuccessRate";

    /**
     * JMX attribute name for Receiver Success Rate.
     */
    private static final String ATTR_REC_SUCCESS_RATE = "ReceiverSuccessRate";

    /**
     * JMX attribute name for Memory Max MB.
     */
    private static final String ATTR_MEM_MAX_MB = "MemoryMaxMB";

    /**
     * JMX attribute name for Memory Available MB.
     */
    private static final String ATTR_MEM_AVAIL_MB = "MemoryAvailableMB";

    /**
     * JMX attribute name for Send Queue Size.
     */
    private static final String ATTR_SEND_Q_SIZE = "SendQueueSize";

    /**
     * JMX attribute name for Unicast Address.
     */
    private static final String ATTR_UNICAST_ADDR = "UnicastAddress";

    /**
     * JMX attribute name for Role Name.
     */
    private static final String ATTR_ROLE_NAME= "RoleName";

    /**
     * JMX attribute name for Unicast Port.
     */
    private static final String ATTR_UNICAST_PORT = "UnicastPort";

    /**
     * JMX Attributes for Product Edition.
     */
    private static final String ATTR_PRODUCT_EDITION = "ProductEdition";
    }
