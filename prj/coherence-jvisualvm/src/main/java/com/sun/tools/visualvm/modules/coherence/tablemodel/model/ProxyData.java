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
 * A class to hold basic proxy data.
 *
 * @author tam  2013.11.14
 * @since  12.1.3
 */
public class ProxyData
        extends AbstractData
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create ProxyData passing in the number of columns.
     */
    public ProxyData()
        {
        super(TOTAL_MSG_SENT + 1);
        }

    // ----- DataRetriever methods ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public List<Map.Entry<Object, Data>> getJMXData(RequestSender requestSender, VisualVMModel model)
        {
        SortedMap<Object, Data> mapData = new TreeMap<Object, Data>();
        Data                    data;

        try
            {
            Set<ObjectName> proxyNamesSet = requestSender.getAllProxyServerMembers();

            for (Iterator<ObjectName> nodIter = proxyNamesSet.iterator(); nodIter.hasNext(); )
                {
                ObjectName proxyNameObjName = (ObjectName) nodIter.next();

                String sServiceName     = proxyNameObjName.getKeyProperty("name");
                String sDomainPartition = proxyNameObjName.getKeyProperty("domainPartition");
                String sProtocol        = PROTOCOL_TCP;

                if (model.getClusterVersionAsInt() >= 122110)
                    {
                    sProtocol = (String) requestSender.getAttribute(proxyNameObjName, "Protocol");
                    }

                // only include the NameService if the model tells us we should  and
                // if its not the NameService, include anyway
                if ((("NameService".equals(sServiceName) && model.isIncludeNameService())
                      || !"NameService".equals(sServiceName)) &&
                       PROTOCOL_TCP.equals(sProtocol))
                    {
                    data = new ProxyData();

                    String sActualServiceName = (sDomainPartition == null ? "" : sDomainPartition + AbstractData.SERVICE_SEP) +
                                                 sServiceName;

                    AttributeList listAttr = requestSender.getAttributes(proxyNameObjName,
                       new String[] { ATTR_HOSTIP, ATTR_CONNECTION_COUNT, ATTR_OUTGOING_MSG_BACKLOG,
                                      ATTR_TOTAL_BYTE_REC, ATTR_TOTAL_BYTE_SENT, ATTR_TOTAL_MSG_REC,
                                      ATTR_TOTAL_MSG_SENT });

                    data.setColumn(ProxyData.NODE_ID, Integer.valueOf(proxyNameObjName.getKeyProperty("nodeId")));
                    data.setColumn(ProxyData.SERVICE_NAME, sActualServiceName);

                    data.setColumn(ProxyData.HOST_PORT, (String) getAttributeValueAsString(listAttr, ATTR_HOSTIP));
                    data.setColumn(ProxyData.CONNECTION_COUNT, Integer.parseInt(getAttributeValueAsString(listAttr, ATTR_CONNECTION_COUNT)));
                    data.setColumn(ProxyData.OUTGOING_MSG_BACKLOG, Long.parseLong(getAttributeValueAsString(listAttr, ATTR_OUTGOING_MSG_BACKLOG)));
                    data.setColumn(ProxyData.TOTAL_BYTES_RECEIVED, Long.parseLong(getAttributeValueAsString(listAttr, ATTR_TOTAL_BYTE_REC)));
                    data.setColumn(ProxyData.TOTAL_BYTES_SENT, Long.parseLong(getAttributeValueAsString(listAttr, ATTR_TOTAL_BYTE_SENT)));
                    data.setColumn(ProxyData.TOTAL_MSG_RECEIVED, Long.parseLong(getAttributeValueAsString(listAttr, ATTR_TOTAL_MSG_REC)));
                    data.setColumn(ProxyData.TOTAL_MSG_SENT, Long.parseLong(getAttributeValueAsString(listAttr, ATTR_TOTAL_MSG_SENT)));

                    mapData.put((String) getAttributeValueAsString(listAttr, ATTR_HOSTIP), data);
                    }

                }

            return new ArrayList<>(mapData.entrySet());
            }
        catch (Exception e)
            {
            LOGGER.log(Level.WARNING, "Error getting proxy statistics", e);

            return null;
            }
        }

    /**
     * {@inheritDoc}
     */
    public String getReporterReport()
        {
        return REPORT_PROXY;
        }

    /**
     * {@inheritDoc}
     */
    public Data processReporterData(Object[] aoColumns, VisualVMModel model)
        {
        String sServiceName = aoColumns[3].toString();
        Data   data         = null;

        // only include the NameService if the model tells us we should  and
        // if its not the NameService, include anyway
        if (("NameService".equals(sServiceName) && model.isIncludeNameService()) || !"NameService".equals(sServiceName))
            {
            data = new ProxyData();

            data.setColumn(ProxyData.HOST_PORT, aoColumns[2].toString());
            data.setColumn(ProxyData.SERVICE_NAME, aoColumns[3].toString());
            data.setColumn(ProxyData.NODE_ID, new Integer(getNumberValue(aoColumns[4].toString())));

            data.setColumn(ProxyData.CONNECTION_COUNT, new Integer(getNumberValue(aoColumns[5].toString())));
            data.setColumn(ProxyData.OUTGOING_MSG_BACKLOG, new Long(getNumberValue(aoColumns[6].toString())));
            data.setColumn(ProxyData.TOTAL_BYTES_RECEIVED, new Long(getNumberValue(aoColumns[7].toString())));
            data.setColumn(ProxyData.TOTAL_BYTES_SENT, new Long(getNumberValue(aoColumns[8].toString())));
            data.setColumn(ProxyData.TOTAL_MSG_RECEIVED, new Long(getNumberValue(aoColumns[9].toString())));
            data.setColumn(ProxyData.TOTAL_MSG_SENT, new Long(getNumberValue(aoColumns[10].toString())));

            if (aoColumns.length == 12 && aoColumns[11] != null)
                {
                // domain partition is present
                data.setColumn(ProxyData.SERVICE_NAME, aoColumns[11].toString() + AbstractData.SERVICE_SEP +
                                                       aoColumns[3].toString());
                }
            }

        return data;
        }

    @Override
    public SortedMap<Object, Data> getAggregatedDataFromHttpQuerying(VisualVMModel     model,
                                                                     HttpRequestSender requestSender)
            throws Exception
        {
        JsonNode                rootNode             = requestSender.getDataForProxyMembers();
        SortedMap<Object, Data> mapData              = new TreeMap<Object, Data>();
        JsonNode                nodeProxyMemberItems = rootNode.get("items");
        if (nodeProxyMemberItems != null && nodeProxyMemberItems.isArray())
            {
            for (int k = 0; k < ((ArrayNode) nodeProxyMemberItems).size(); k++)
                {
                JsonNode proxyNode = (JsonNode) nodeProxyMemberItems.get(k);

                String sServiceName = proxyNode.get("name").asText();
                String sProtocol    = proxyNode.get("protocol").asText();
                if (("NameService".equals(sServiceName) && model.isIncludeNameService())
                        || !"NameService".equals(sServiceName) &&
                        PROTOCOL_TCP.equals(sProtocol))
                    {
                    ProxyData data = new ProxyData();

                    data.setColumn(ProxyData.HOST_PORT, proxyNode.get("hostIP").asText());
                    data.setColumn(ProxyData.SERVICE_NAME, sServiceName);
                    data.setColumn(ProxyData.NODE_ID,
                            Integer.valueOf(proxyNode.get("nodeId").asText()));
                    data.setColumn(ProxyData.CONNECTION_COUNT,
                            Integer.valueOf(proxyNode.get("connectionCount").asText()));
                    data.setColumn(ProxyData.OUTGOING_MSG_BACKLOG,
                            Long.valueOf(proxyNode.get("outgoingMessageBacklog").asText()));
                    data.setColumn(ProxyData.TOTAL_BYTES_RECEIVED,
                            Long.valueOf(proxyNode.get("totalBytesReceived").asText()));
                    data.setColumn(ProxyData.TOTAL_BYTES_SENT,
                            Long.valueOf(proxyNode.get("totalBytesSent").asText()));
                    data.setColumn(ProxyData.TOTAL_MSG_RECEIVED,
                            Long.valueOf(proxyNode.get("totalMessagesReceived").asText()));
                    data.setColumn(ProxyData.TOTAL_MSG_SENT,
                            Long.valueOf(proxyNode.get("totalMessagesSent").asText()));

                    JsonNode sDomainPartition = proxyNode.get("domainPartition");
                    if (sDomainPartition != null)
                        {
                        // domain partition is present
                        data.setColumn(ProxyData.SERVICE_NAME, sDomainPartition + AbstractData.SERVICE_SEP +
                                sServiceName);
                        }

                    mapData.put(data.getColumn(0), data);
                    }
                }
            }
        return mapData;
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = 1789802484301825295L;

    /**
     * Array index for host/port.
     */
    public static final int HOST_PORT = 0;

    /**
     * Array index for service name.
     */
    public static final int SERVICE_NAME = 1;

    /**
     * Array index for Node id.
     */
    public static final int NODE_ID = 2;

    /**
     * Array index for connection count.
     */
    public static final int CONNECTION_COUNT = 3;

    /**
     * Array index for outgoing message backlog.
     */
    public static final int OUTGOING_MSG_BACKLOG = 4;

    /**
     * Array index for total bytes received.
     */
    public static final int TOTAL_BYTES_RECEIVED = 5;

    /**
     * Array index for total bytes sent.
     */
    public static final int TOTAL_BYTES_SENT = 6;

    /**
     * Array index for total messages received.
     */
    public static final int TOTAL_MSG_RECEIVED = 7;

    /**
     * Array index for total messages sent.
     */
    public static final int TOTAL_MSG_SENT = 8;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(ProxyData.class.getName());

    /**
     * Report for proxy server data.
     */
    public static final String REPORT_PROXY = "reports/visualvm/proxy-stats.xml";

    /**
     * JMX attribute name for HostIP.
     */
    private static final String ATTR_HOSTIP = "HostIP";

    /**
     * JMX attribute name for Connection Count.
     */
    private static final String ATTR_CONNECTION_COUNT = "ConnectionCount";

    /**
     * JMX attribute name for Outgoing Message Backlog.
     */
    private static final String ATTR_OUTGOING_MSG_BACKLOG = "OutgoingMessageBacklog";

    /**
     * JMX attribute name for Outgoing Total Bytes Received.
     */
    private static final String ATTR_TOTAL_BYTE_REC = "TotalBytesReceived";

    /**
     * JMX attribute name for Outgoing Total Bytes Sent.
     */
    private static final String ATTR_TOTAL_BYTE_SENT = "TotalBytesSent";

    /**
     * JMX attribute name for Outgoing Total Messages Received.
     */
    private static final String ATTR_TOTAL_MSG_REC = "TotalMessagesReceived";

    /**
     * JMX attribute name for Outgoing Total Messages Sent.
     */
    private static final String ATTR_TOTAL_MSG_SENT = "TotalMessagesSent";

    /**
     * Protocol for ConnectionManager MBean for proxy server.
     */
    private static final String PROTOCOL_TCP = "tcp";
    }