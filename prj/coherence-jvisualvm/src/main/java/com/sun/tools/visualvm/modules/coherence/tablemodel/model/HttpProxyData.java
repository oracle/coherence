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
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;
import javax.management.ObjectName;

/**
 * A class to hold basic Http proxy data.
 *
 * @author tam  2015.08.28
 * @since  12.2.1.1
 */
public class HttpProxyData extends AbstractData
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create HttpProxyData passing in the number of columns.
     */
    public HttpProxyData()
        {
        super(AVERAGE_REQ_TIME + 1);
        }

    // ----- DataRetriever methods ------------------------------------------

    @Override
    public List<Map.Entry<Object, Data>> getJMXData(RequestSender requestSender, VisualVMModel model)
        {
        // only reporter is supported
        return null;
        }

    @Override
    public String getReporterReport()
        {
        return REPORT_HTTP_PROXY;
        }

    @Override
    public Data processReporterData(Object[] aoColumns, VisualVMModel model)
        {
        Data    data = new HttpProxyData();
        boolean fMT  = aoColumns[2] != null;
        int  nStart = 3;

        if (fMT)
            {
            data.setColumn(HttpProxyData.SERVICE_NAME, getFullServiceName(aoColumns[2].toString(),aoColumns[1].toString()));
            }
        else
            {
            data.setColumn(HttpProxyData.SERVICE_NAME, aoColumns[1]);
            }

        String        sHttpServer = (String) aoColumns[nStart++];
        sHttpServer = sHttpServer == null ? "unknown"
                                          : sHttpServer.substring(sHttpServer.lastIndexOf('.') + 1);

        data.setColumn(HttpProxyData.HTTP_SERVER_TYPE, sHttpServer);
        data.setColumn(HttpProxyData.MEMBER_COUNT, Integer.valueOf(getNumberValue(aoColumns[nStart++].toString())));
        data.setColumn(HttpProxyData.TOTAL_REQUEST_COUNT, Long.valueOf(getNumberValue(aoColumns[nStart++].toString())));
        data.setColumn(HttpProxyData.TOTAL_ERROR_COUNT, Long.valueOf(getNumberValue(aoColumns[nStart++].toString())));
        data.setColumn(HttpProxyData.AVERAGE_REQ_PER_SECOND, Float.valueOf(aoColumns[nStart++].toString()));
        data.setColumn(HttpProxyData.AVERAGE_REQ_TIME, Float.valueOf(aoColumns[nStart++].toString()));

        return data;
        }

    @Override
    public SortedMap<Object, Data> getAggregatedDataFromHttpQuerying(VisualVMModel     model,
                                                                     HttpRequestSender requestSender) throws Exception
        {
        JsonNode                rootNode             = requestSender.getDataForProxyMembers();
        SortedMap<Object, Data> mapData              = new TreeMap<>();
        JsonNode                nodeProxyMemberItems = rootNode.get("items");
        
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

                String protocol = proxyDetails.get("protocol").asText();
                if ("http".equals(protocol))
                    {
                    String   sHttpServer    = "unknown";
                    JsonNode httpServerType = proxyDetails.get("httpServerType");

                    if (httpServerType != null)
                        {
                        sHttpServer = httpServerType.asText();
                        }

                    Data data = mapData.get(sService);
                    if (data == null) {
                        data = new HttpProxyData();
                        data.setColumn(SERVICE_NAME, sService);
                        data.setColumn(HTTP_SERVER_TYPE, sHttpServer);
                        data.setColumn(MEMBER_COUNT, 0);
                        data.setColumn(TOTAL_REQUEST_COUNT, 0);
                        data.setColumn(TOTAL_ERROR_COUNT, 0);
                    }

                    data.setColumn(MEMBER_COUNT, (int) data.getColumn(MEMBER_COUNT) + 1);
                    data.setColumn(TOTAL_REQUEST_COUNT, (int) data.getColumn(TOTAL_REQUEST_COUNT)
                                                        + proxyDetails.get("totalRequestCount").asInt());
                    data.setColumn(TOTAL_ERROR_COUNT, (int) data.getColumn(TOTAL_ERROR_COUNT)
                                                        + proxyDetails.get("totalErrorCount").asInt());

                    mapData.put(sService, data);
                    }
                }
            }

        return mapData;
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = 1759802484601825295L;

    /**
     * Array index for service name.
     */
    public static final int SERVICE_NAME = 0;

    /**
     * Array index for http server type.
     */
    public static final int HTTP_SERVER_TYPE = 1;

    /**
     * Array index for Members.
     */
    public static final int MEMBER_COUNT = 2;

    /**
     * Array index for TotalRequestCount.
     */
    public static final int TOTAL_REQUEST_COUNT = 3;

    /**
     * Array index for TotalErrorCount.
     */
    public static final int TOTAL_ERROR_COUNT = 4;

    /**
     * Array index for AverageRequestsPerSecond.
     */
    public static final int AVERAGE_REQ_PER_SECOND = 5;

    /**
     * Array index for AverageRequestTime.
     */
    public static final int AVERAGE_REQ_TIME = 6;

    /**
     * Report for proxy server data.
     */
    public static final String REPORT_HTTP_PROXY = "reports/visualvm/http-proxy-stats.xml";
    }