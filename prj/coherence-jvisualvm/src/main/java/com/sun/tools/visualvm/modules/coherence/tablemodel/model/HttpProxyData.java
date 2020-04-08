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
        data.setColumn(HttpProxyData.MEMBER_COUNT, new Integer(getNumberValue(aoColumns[nStart++].toString())));
        data.setColumn(HttpProxyData.TOTAL_REQUEST_COUNT, new Long(getNumberValue(aoColumns[nStart++].toString())));
        data.setColumn(HttpProxyData.TOTAL_ERROR_COUNT, new Long(getNumberValue(aoColumns[nStart++].toString())));
        data.setColumn(HttpProxyData.AVERAGE_REQ_PER_SECOND, new Float(aoColumns[nStart++].toString()));
        data.setColumn(HttpProxyData.AVERAGE_REQ_TIME, new Float(aoColumns[nStart++].toString()));

        return data;
        }

    @Override
    public SortedMap<Object, Data> getAggregatedDataFromHttpQuerying(VisualVMModel     model,
                                                                     HttpRequestSender requestSender) throws Exception
        {
        Set<ObjectName> setProxyMembers = requestSender.getAllProxyServerMembers();

        Set<String> setServices = new HashSet<>();
        for (ObjectName objName : setProxyMembers)
            {
            String sServiceName     = objName.getKeyProperty("name");
            String sDomainPartition = objName.getKeyProperty("domainPartition");

            setServices.add(sDomainPartition == null ? sServiceName : sDomainPartition + "/" +  sServiceName);
            }

        SortedMap<Object, Data> mapData = new TreeMap<Object, Data>();

        for (String sService : setServices)
            {
            String[] as               = sService.split("/");
            String   sServiceName     = as.length == 2 ? as[1] : as[0];
            String   sDomainPartition = as.length == 2 ? as[0] : null;
            JsonNode rootNode         = requestSender.getAggregatedProxyData(sServiceName, sDomainPartition);
            String   protocol         = rootNode.get("protocol").get(0).textValue();

            if (protocol.equals("http"))
                {

                String   sHttpServer    = "unknown";
                JsonNode httpServerType = rootNode.get("httpServerType");

                if (httpServerType != null && httpServerType.isArray())
                    {
                    sHttpServer = httpServerType.get(0).asText();
                    }

                Data data = new HttpProxyData();

                data.setColumn(HttpProxyData.SERVICE_NAME, sService);
                data.setColumn(HttpProxyData.HTTP_SERVER_TYPE, sHttpServer);
                data.setColumn(HttpProxyData.MEMBER_COUNT,
                        Integer.parseInt(getChildValue("count", "totalRequestCount", rootNode)));
                data.setColumn(HttpProxyData.TOTAL_REQUEST_COUNT,
                        Integer.parseInt(getChildValue("sum", "totalRequestCount", rootNode)));
                data.setColumn(HttpProxyData.TOTAL_ERROR_COUNT,
                        Integer.parseInt(getChildValue("sum", "totalErrorCount", rootNode)));

                float nSumRequestPerSecond = Float.parseFloat(getChildValue("sum",
                        "requestsPerSecond", rootNode));
                float nCountRequestPerSecond = Float.parseFloat(getChildValue("count",
                        "requestsPerSecond", rootNode));

                data.setColumn(HttpProxyData.AVERAGE_REQ_PER_SECOND, nCountRequestPerSecond != 0
                        ? nSumRequestPerSecond/nCountRequestPerSecond
                        : 0);

                float nSumAverageRequestTime = Float.parseFloat(getChildValue("sum",
                        "averageRequestTime", rootNode));
                float nCountAverageRequestTime = Float.parseFloat(getChildValue("count",
                        "averageRequestTime", rootNode));

                data.setColumn(HttpProxyData.AVERAGE_REQ_TIME, nCountAverageRequestTime != 0
                        ? nSumAverageRequestTime/nCountAverageRequestTime
                        : 0);

                mapData.put(data.getColumn(0), data);
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