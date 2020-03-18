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

import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.logging.Logger;

import javax.management.ObjectName;

/**
 * A class to hold Federated Destination data.
 *
 * @author bb  2014.01.29
 *
 * @since  12.2.1
 */
public class FederationDestinationData
        extends FederationData
    {

    // ----- DataRetriever methods ------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String getReporterReport()
        {
        return REPORT_DESTINATION;
        }

    /**
     * {@inheritDoc}
     */
    public Data processReporterData(Object[] aoColumns, VisualVMModel model)
        {
        Data data = new FederationDestinationData();

        for (Column col : Column.values())
            {
            int ordinal = col.ordinal();
            switch (col)
                {
                case KEY:
                    Pair<String, String> pair = new Pair<String, String>(aoColumns[Column.SERVICE.getColumn()].toString(),
                            aoColumns[Column.PARTICIPANT.getColumn()].toString());
                    data.setColumn(ordinal, pair);
                    break;
                case SERVICE:
                case PARTICIPANT:
                case STATUS:
                    data.setColumn(ordinal, aoColumns[col.getColumn()]);
                    break;
                case TOTAL_BYTES_SENT:
                case TOTAL_MSGS_SENT:
                    data.setColumn(ordinal, new Long (getNumberValue(aoColumns[col.getColumn()] == null ? "0" :  aoColumns[col.getColumn()].toString())));
                    break;
                }
            }
        return data;
        }

    @Override
    public SortedMap<Object, Data> getAggregatedDataFromHttpQuerying(VisualVMModel model,
                                                                     HttpRequestSender requestSender)
            throws Exception
        {
        Set<ObjectName> setServiceMembers = requestSender.getAllServiceMembers();
        Set<String>     setServices       = new HashSet<>();
        for (ObjectName objName : setServiceMembers)
            {
            String sServiceName     = objName.getKeyProperty("name");
            String sDomainPartition = objName.getKeyProperty("domainPartition");

            String sServiceType = requestSender.getAttribute(objName, "type");
            if (sServiceType != null && sServiceType.equals("FederatedCache"))
                {
                setServices.add(sDomainPartition == null ? sServiceName : sDomainPartition + "/" +  sServiceName);
                }
            }

        SortedMap<Object, Data> mapData = new TreeMap<Object, Data>();

        for (String sService : setServices)
            {
            String[] as               = sService.split("/");
            String   sDomainPartition = as.length == 2 ? as[0] : null;
            String   sServiceName     = as.length == 2 ? as[1] : as[0];
            JsonNode nodeRoot         = requestSender.getAggregatedOutgoingData(sServiceName, sDomainPartition);

            JsonNode nodeItems = nodeRoot.get("items");
            if (nodeItems != null && nodeItems.isArray())
                {
                for (int i = 0; i < nodeItems.size() ; i++)
                    {
                    Data     data             = new FederationDestinationData();
                    JsonNode nodeItem         = nodeItems.get(i);
                    String   sParticipantName = nodeItem.get("participantName").asText();

                    for (Column col : Column.values())
                        {
                        int ordinal = col.ordinal();
                        switch (col)
                            {
                            case KEY:
                                Pair<String, String> pair = new Pair<String, String>(sService, sParticipantName);
                                data.setColumn(ordinal, pair);
                                break;
                            case SERVICE:
                                data.setColumn(ordinal, sService);
                                break;
                            case PARTICIPANT:
                                data.setColumn(ordinal, sParticipantName);
                                break;
                            case TOTAL_BYTES_SENT:
                                data.setColumn(ordinal,
                                        Long.valueOf(getChildValue("sum", "bytesSentSecs", nodeItem)));
                                break;
                            case STATUS:
                                data.setColumn(ordinal,
                                        Long.valueOf(getChildValue("max", "status", nodeItem)));
                                break;
                            case TOTAL_MSGS_SENT:
                                data.setColumn(ordinal,
                                        Long.valueOf(getChildValue("sum", "msgsSentSecs", nodeItem)));
                                break;
                            }
                        }
                    mapData.put(data.getColumn(0), data);
                    }
                }
            }

        return mapData;
        }

    private String getChildValue(String sChildFieldName, String sFieldName, JsonNode rootNode)
        {
        JsonNode node = rootNode.get(sFieldName);
        if (node != null && node.isContainerNode())
            {
            return node.get(sChildFieldName).asText(null);
            }
        return null;
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = 7075440287604402611L;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(FederationDestinationData.class.getName());

    /**
     * Report for destination data.
     */
    public static final String REPORT_DESTINATION = "reports/visualvm/federation-destination-stats.xml";
    }
