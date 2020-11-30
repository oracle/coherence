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
public class FederationOriginData
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
        Data data = new FederationOriginData();

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
                    data.setColumn(ordinal, aoColumns[col.getColumn()]);
                    break;
                case TOTAL_BYTES_RECEIVED:
                case TOTAL_MSGS_RECEIVED:
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
        Set<String> setServices = retrieveFederatedServices(requestSender);
        
        SortedMap<Object, Data> mapData = new TreeMap<>();

        for (String sService : setServices)
            {
            String[] as               = sService.split("/");
            String   sServiceName     = as.length == 2 ? as[1] : as[0];
            String   sDomainPartition = as.length == 2 ? as[0] : null;
            JsonNode rootNode         = requestSender.getAggregatedIncomingData(sServiceName, sDomainPartition);

            JsonNode itemsNode = rootNode.get("items");
            if (itemsNode != null && itemsNode.isArray())
                {
                for (int i = 0; i < itemsNode.size() ; i++)
                    {
                    Data     data             = new FederationOriginData();
                    JsonNode itemNode         = itemsNode.get(i);
                    String   sParticipantName = itemNode.get("participantName").asText();

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
                            case TOTAL_BYTES_RECEIVED:
                                data.setColumn(ordinal,
                                        Long.valueOf(getChildValue("sum", "bytesReceivedSecs", itemNode)));
                                break;
                            case TOTAL_MSGS_RECEIVED:
                                data.setColumn(ordinal,
                                        Long.valueOf(getChildValue("sum", "msgsReceivedSecs", itemNode)));
                                break;
                            }
                        }
                    mapData.put(data.getColumn(0), data);
                    }
                }
            }

        return mapData;
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = -2052543737741081327L;

    /**
     * Report for destination data.
     */
    public static final String REPORT_DESTINATION = "reports/visualvm/federation-origin-stats.xml";
    }
