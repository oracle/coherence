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
import java.util.List;
import java.util.Map;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class to hold Federated Destination data.
 *
 * @author bb  2014.01.29
 *
 * @since  12.2.1
 */
public class FederationDestinationDetailsData
        extends AbstractData
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create ServiceData passing in the number of columns.
     */
    public FederationDestinationDetailsData()
        {
        super(Column.values().length);
        }

    // ----- DataRetriever methods ------------------------------------------

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    public List<Map.Entry<Object, Data>> getJMXData(RequestSender requestSender, VisualVMModel model)
        {
        return null;
        }

    /**
     * {@inheritDoc}
     */
    public String getReporterReport()
        {
        return REPORT_DESTINATION_DETAILS;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String preProcessReporterXML(VisualVMModel model, String sReporterXML)
        {
        // the report XML contains the following tokens that require substitution:
        // %SERVICE_NAME%
        // %PARTICIPANT_NAME%
        Pair<String, String> serviceParticipant = model.getSelectedServiceParticipant();

        String query = serviceParticipant == null ? sReporterXML :
               sReporterXML.replaceAll("%SERVICE_NAME%", serviceParticipant.getX())
                           .replaceAll("%PARTICIPANT_NAME%", serviceParticipant.getY());

        return query;
        }

    /**
     * {@inheritDoc}
     */
    public Data processReporterData(Object[] aoColumns, VisualVMModel model)
        {
        Data data = new FederationDestinationDetailsData();

        for (Column col : Column.values())
            {
            int ordinal = col.ordinal();
            switch (col)
                {
                case NODE_ID:
                case STATE:
                    data.setColumn(ordinal, aoColumns[col.getColumn()]);
                    break;
                case TOTAL_BYTES_SENT:
                case TOTAL_ENTRIES_SENT:
                case TOTAL_RECORDS_SENT:
                case TOTAL_MSG_SENT:
                case TOTAL_MSG_UNACKED:
                case REPLICATE_PERCENT_COMPLETE:
                case REPLICATE_TIME_REMAINING:
                case LAST_REPLICATE_TOTAL_TIME:
                case RECORD_BACKLOG_DELAY_TIME_PERCENTILE_MILLIS:
                case MSG_NETWORK_ROUND_TRIP_TIME_PERCENTILE_MILLIS:
                case MSG_APPLY_TIME_PERCENTILE_MILLIS:
                case BYTES_REPLICATED_SECS:
                case MSG_REPLICATED_SECS:
                    data.setColumn(ordinal, new Long (getNumberValue(aoColumns[col.getColumn()].toString())));
                    break;
                case CURRENT_BANDWIDTH:
                    data.setColumn(ordinal, new Float (aoColumns[col.getColumn()].toString()));
                    break;
                case MAX_BANDWIDTH:
                case ERROR_DESCRIPTION:
                case SEND_TIMEOUT_MILLIS:
                case GEO_IP:
                    data.setColumn(ordinal, aoColumns[col.getColumn()]);
                    break;
                default:
                    LOGGER.log(Level.SEVERE, "Unknown column name " + col);
                }
            }
        return data;
        }

    /**
     * Defines the data collected from destination MBeans.
     */
    public enum Column
        {
        NODE_ID(1),
        STATE(2),
        CURRENT_BANDWIDTH(3),
        TOTAL_BYTES_SENT(4),
        TOTAL_ENTRIES_SENT(5),
        TOTAL_RECORDS_SENT(6),
        TOTAL_MSG_SENT(7),
        TOTAL_MSG_UNACKED(8),
        REPLICATE_PERCENT_COMPLETE(9),
        REPLICATE_TIME_REMAINING(10),
        LAST_REPLICATE_TOTAL_TIME(11),
        RECORD_BACKLOG_DELAY_TIME_PERCENTILE_MILLIS(12),
        MSG_NETWORK_ROUND_TRIP_TIME_PERCENTILE_MILLIS(13),
        MSG_APPLY_TIME_PERCENTILE_MILLIS(14),
        BYTES_REPLICATED_SECS(15),
        MSG_REPLICATED_SECS(16),
        MAX_BANDWIDTH(17),
        ERROR_DESCRIPTION(18),
        SEND_TIMEOUT_MILLIS(19),
        GEO_IP(20);

        Column(int nCol)
            {
            f_nCol = nCol;
            }

        /**
         * Returns the column number for this enum.
         *
         * @return the column number
         */
        public int getColumn()
            {
            return f_nCol;
            }

        /**
         * The column number associates with thw enum.
         */
        protected final int f_nCol;
        }

    @Override
    public SortedMap<Object, Data> getAggregatedDataFromHttpQuerying(VisualVMModel     model,
                                                                     HttpRequestSender requestSender) throws Exception
        {
        Pair<String, String> serviceParticipant = model.getSelectedServiceParticipant();
        if (serviceParticipant == null)
            {
            return null;
            }

        String sServiceName     = serviceParticipant.getX();
        String sParticipantName = serviceParticipant.getY();

        JsonNode rootNode = requestSender.getOutgoingDataForParticipant(sServiceName, sParticipantName);

        JsonNode nodeFederationItems = rootNode.get("items");

        SortedMap<Object, Data> mapData = new TreeMap<Object, Data>();
        if (nodeFederationItems != null && nodeFederationItems.isArray())
            {
            for (int j = 0; j < nodeFederationItems.size(); j++)
                {
                JsonNode participantNode = nodeFederationItems.get(j);
                Data     data            = new FederationDestinationDetailsData();

                for (Column col : Column.values())
                    {
                    int ordinal = col.ordinal();
                    switch (col)
                        {
                        case NODE_ID:
                            data.setColumn(ordinal, participantNode.get("nodeId").asText());
                            break;
                        case STATE:
                            data.setColumn(ordinal, participantNode.get("state").asText());
                            break;
                        case TOTAL_BYTES_SENT:
                            data.setColumn(ordinal, Long.valueOf(getNumberValue((participantNode.get("totalBytesSent").asText()))));
                            break;
                        case TOTAL_ENTRIES_SENT:
                            data.setColumn(ordinal, Long.valueOf(getNumberValue((participantNode.get("totalEntriesSent").asText()))));
                            break;

                        case TOTAL_RECORDS_SENT:
                            data.setColumn(ordinal, Long.valueOf(getNumberValue(participantNode.get("totalRecordsSent").asText())));
                            break;

                        case TOTAL_MSG_SENT:
                            data.setColumn(ordinal, Long.valueOf(getNumberValue(participantNode.get("totalMsgSent").asText())));
                            break;

                        case TOTAL_MSG_UNACKED:
                            data.setColumn(ordinal, Long.valueOf(getNumberValue(participantNode.get("totalMsgUnacked").asText())));
                            break;

                        case RECORD_BACKLOG_DELAY_TIME_PERCENTILE_MILLIS:
                            data.setColumn(ordinal, Long.valueOf(getNumberValue(participantNode.get("recordBacklogDelayTimePercentileMillis").asText())));
                            break;

                        case MSG_NETWORK_ROUND_TRIP_TIME_PERCENTILE_MILLIS:
                            data.setColumn(ordinal, Long.valueOf(getNumberValue(participantNode.get("msgNetworkRoundTripTimePercentileMillis").asText())));
                            break;

                        case MSG_APPLY_TIME_PERCENTILE_MILLIS:
                            data.setColumn(ordinal, Long.valueOf(getNumberValue(participantNode.get("msgApplyTimePercentileMillis").asText())));
                            break;

                        case BYTES_REPLICATED_SECS:
                            data.setColumn(ordinal, Long.valueOf(getNumberValue(participantNode.get("bytesSentSecs").asText())));
                            break;

                        case MSG_REPLICATED_SECS:
                            data.setColumn(ordinal, Long.valueOf(getNumberValue(participantNode.get("msgsSentSecs").asText())));
                            break;

                        case REPLICATE_TIME_REMAINING:
                            data.setColumn(ordinal, Long.valueOf(getNumberValue(participantNode.get("estimatedReplicateAllRemainingTime").asText())));
                            break;

                        case LAST_REPLICATE_TOTAL_TIME:
                            data.setColumn(ordinal, Long.valueOf(getNumberValue(participantNode.get("replicateAllTotalTime").asText())));
                            break;

                        case REPLICATE_PERCENT_COMPLETE:
                            // protect from null value coming back from pre 12.2.1.2.0 cluster
                            data.setColumn(ordinal, Long.valueOf(getNumberValue(participantNode.get("replicateAllPercentComplete").asText())));
                            break;
                        case CURRENT_BANDWIDTH:
                            data.setColumn(ordinal, Float.valueOf(participantNode.get("currentBandwidth").asText()));
                            break;
                        case MAX_BANDWIDTH:
                            data.setColumn(ordinal, participantNode.get("maxBandwidth").asText());
                            break;

                        case ERROR_DESCRIPTION:
                            data.setColumn(ordinal, participantNode.get("errorDescription").asText());
                            break;

                        case SEND_TIMEOUT_MILLIS:
                            data.setColumn(ordinal, participantNode.get("sendTimeoutMillis").asText());
                            break;

                        case GEO_IP:
                            data.setColumn(ordinal, participantNode.get("geoIp").asText());
                            break;
                        default:
                            LOGGER.log(Level.SEVERE, "Unknown column name " + col);
                        }
                    }
                mapData.put(data.getColumn(0), data);
                }
            }
        return mapData;

        }


    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = 5322461621579606184L;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(FederationDestinationDetailsData.class.getName());

    /**
     * Report for destination data.
     */
    public static final String REPORT_DESTINATION_DETAILS = "reports/visualvm/federation-destination-detail-stats.xml";
    }
