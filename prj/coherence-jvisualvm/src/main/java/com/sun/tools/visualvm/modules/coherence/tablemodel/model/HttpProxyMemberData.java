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

import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.stream.Collectors;

import javax.management.AttributeList;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import java.util.List;
import java.util.Map;

/**
 * A class to hold detailed http proxy member data for a selected service.
 *
 * @author tam  2015.08.28
 * @since  12.2.1.1
 */
public class HttpProxyMemberData extends AbstractData
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create HttpProxyMemberData passing in the number of columns.
     */
    public HttpProxyMemberData()
        {
        super(RESPONSE_COUNT_5 + 1);
        }

    // ----- DataRetriever methods ------------------------------------------


    @Override
    public List<Map.Entry<Object, Data>> getJMXData (RequestSender requestSender, VisualVMModel model)
        {
        return null;
        }

    @Override
    public String getReporterReport ()
        {
        return REPORT_HTTP_PROXY_DETAIL;
        }

    @Override
    public Data processReporterData (Object[] aoColumns, VisualVMModel model)
        {
        Data data   = new HttpProxyMemberData();
        int  nStart = 1;

        data.setColumn(HttpProxyMemberData.NODE_ID, new Integer(getNumberValue(aoColumns[nStart++].toString())));
        data.setColumn(HttpProxyMemberData.HOST_IP, aoColumns[nStart++]);
        data.setColumn(HttpProxyMemberData.AVG_REQ_TIME, new Float(aoColumns[nStart++].toString()));
        data.setColumn(HttpProxyMemberData.REQ_PER_SECOND, new Float(aoColumns[nStart++].toString()));
        data.setColumn(HttpProxyMemberData.TOTAL_ERROR_COUNT, new Long(getNumberValue(aoColumns[nStart++].toString())));
        data.setColumn(HttpProxyMemberData.TOTAL_REQUEST_COUNT, new Long(getNumberValue(aoColumns[nStart++].toString())));
        // skip domainPartition
        nStart++;

        data.setColumn(HttpProxyMemberData.RESPONSE_COUNT_1, new Long(getNumberValue(aoColumns[nStart++].toString())));
        data.setColumn(HttpProxyMemberData.RESPONSE_COUNT_2, new Long(getNumberValue(aoColumns[nStart++].toString())));
        data.setColumn(HttpProxyMemberData.RESPONSE_COUNT_3, new Long(getNumberValue(aoColumns[nStart++].toString())));
        data.setColumn(HttpProxyMemberData.RESPONSE_COUNT_4, new Long(getNumberValue(aoColumns[nStart++].toString())));
        data.setColumn(HttpProxyMemberData.RESPONSE_COUNT_5, new Long(getNumberValue(aoColumns[nStart++].toString())));

        return data;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String preProcessReporterXML(VisualVMModel model, String sReporterXML)
        {
        // the report XML contains the following tokens that require substitution:
        // %SERVICE_NAME%

        // see if we have domainPartition key
        String sServiceName     = model.getSelectedHttpProxyService();
        String sDomainPartition = null;

        if (sServiceName != null)
            {
            String[] asServiceDetails = getDomainAndService(sServiceName);
            sServiceName              = asServiceDetails[1];
            sDomainPartition          = asServiceDetails[0];
            }

        return sServiceName == null ? sReporterXML :
               sReporterXML.replaceAll("%SERVICE_NAME%", sServiceName +
                                                         (sDomainPartition !=
                                                          null ?
                                                          ",domainPartition=" +
                                                          sDomainPartition : ""));
        }

    @Override
    public SortedMap<Object, Data> getAggregatedDataFromHttpQuerying(VisualVMModel model, HttpRequestSender requestSender)
            throws Exception
        {
        String sSelectedService = model.getSelectedHttpProxyService();

        if (sSelectedService != null)
            {
            SortedMap<Object, Data> mapData = new TreeMap<>();

            JsonNode dataForProxyMembers  = requestSender.getDataForProxyMembers();
            JsonNode nodeProxyMemberItems = dataForProxyMembers.get("items");

            if (nodeProxyMemberItems != null && nodeProxyMemberItems.isArray())
                {
                for (int k = 0; k < ((ArrayNode) nodeProxyMemberItems).size(); k++)
                    {
                    JsonNode proxyDetails     = nodeProxyMemberItems.get(k);
                    String   sServiceName     = proxyDetails.get("name").asText();
                    JsonNode domainPartition  = proxyDetails.get("domainPartition");
                    String   sDomainPartition = domainPartition == null ? null : domainPartition.asText();
                    String   sService         = sDomainPartition == null
                                                ? sServiceName : sDomainPartition + "/" +  sServiceName;

                    // only include selected service
                    if (sService.equals(sSelectedService))
                        {
                        Data data = new HttpProxyMemberData();

                        data.setColumn(HttpProxyMemberData.NODE_ID, proxyDetails.get("nodeId").asInt());
                        data.setColumn(HttpProxyMemberData.HOST_IP, proxyDetails.get("hostIP").asText());
                        data.setColumn(HttpProxyMemberData.AVG_REQ_TIME,
                                Float.valueOf(proxyDetails.get("averageRequestTime").asText()));
                        data.setColumn(HttpProxyMemberData.REQ_PER_SECOND,
                                Float.valueOf(proxyDetails.get("requestsPerSecond").asText()));
                        data.setColumn(HttpProxyMemberData.TOTAL_ERROR_COUNT, proxyDetails.get("totalErrorCount").asLong());
                        data.setColumn(HttpProxyMemberData.TOTAL_REQUEST_COUNT, proxyDetails.get("totalRequestCount").asLong());
                        data.setColumn(HttpProxyMemberData.RESPONSE_COUNT_1, proxyDetails.get("responseCount1xx").asLong());
                        data.setColumn(HttpProxyMemberData.RESPONSE_COUNT_2, proxyDetails.get("responseCount2xx").asLong());
                        data.setColumn(HttpProxyMemberData.RESPONSE_COUNT_3, proxyDetails.get("responseCount3xx").asLong());
                        data.setColumn(HttpProxyMemberData.RESPONSE_COUNT_4, proxyDetails.get("responseCount4xx").asLong());
                        data.setColumn(HttpProxyMemberData.RESPONSE_COUNT_5, proxyDetails.get("responseCount5xx").asLong());

                        mapData.put(data.getColumn(0), data);
                        }
                    }
                    return mapData;
                }
            }
        return null;
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = 1559872484801825295L;

    /**
     * Array index for service name.
     */
    public static final int NODE_ID = 0;

    /**
     * Array index for hostIP.
     */
    public static final int HOST_IP = 1;

    /**
     * Array index for AverageRequestTime.
     */
    public static final int AVG_REQ_TIME = 2;

    /**
     * Array index for RequestsPerSecond.
     */
    public static final int REQ_PER_SECOND = 3;

    /**
     * Array index for TotalRequestCount.
     */
    public static final int TOTAL_REQUEST_COUNT = 4;

    /**
     * Array index for TotalErrorCount.
     */
    public static final int TOTAL_ERROR_COUNT = 5;

    /**
     * Array index for ResponseCount1xx.
     */
    public static final int RESPONSE_COUNT_1 = 6;

    /**
     * Array index for ResponseCount2xx.
     */
    public static final int RESPONSE_COUNT_2 = 7;

    /**
     * Array index for ResponseCount3xx.
     */
    public static final int RESPONSE_COUNT_3 = 8;

    /**
     * Array index for ResponseCount4xx.
     */
    public static final int RESPONSE_COUNT_4 = 9;

    /**
     * Array index for ResponseCount5xx.
     */
    public static final int RESPONSE_COUNT_5 = 10;

    /**
     * Report for proxy server data.
     */
    public static final String REPORT_HTTP_PROXY_DETAIL = "reports/visualvm/http-proxy-stats-detail.xml";
    }
