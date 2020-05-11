/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.tablemodel.model;

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
 * A class to hold basic cluster data.
 *
 * @author tam  2013.11.14
 * @sine   12.1.3
 */
public class ClusterData
        extends AbstractData
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create ClusterData passing in the number of columns.
     */
    public ClusterData()
        {
        super(CLUSTER_SIZE + 1);
        }

    // ----- DataRetriever methods ------------------------------------------

    /**
     * {@inheritDoc}
     */
    public List<Map.Entry<Object, Data>> getJMXData(RequestSender requestSender, VisualVMModel model)
        {
        return new ArrayList<Map.Entry<Object, Data>>(getJMXDataMap(requestSender, model).entrySet());
        }

    /**
     * {@inheritDoc}
     */
    public String getReporterReport()
        {
        return REPORT_CLUSTER;
        }

    /**
     * {@inheritDoc}
     */
    public Data processReporterData(Object[] aoColumns, VisualVMModel model)
        {
        Data data = new ClusterData();

        data.setColumn(ClusterData.CLUSTER_NAME, aoColumns[2]);
        data.setColumn(ClusterData.LICENSE_MODE, aoColumns[3]);
        data.setColumn(ClusterData.VERSION, aoColumns[4]);
        data.setColumn(ClusterData.DEPARTURE_COUNT, new Integer(getNumberValue(aoColumns[5].toString())));
        data.setColumn(ClusterData.CLUSTER_SIZE, new Integer(getNumberValue(aoColumns[6].toString())));

        return data;
        }

    @Override
    public SortedMap<Object, Data> getAggregatedDataFromHttpQuerying(VisualVMModel     model,
                                                                     HttpRequestSender requestSender)
            throws Exception
        {
        return getJMXDataMap(requestSender, model);
        }

    // ----- DataRetriever methods ------------------------------------------

    protected SortedMap<Object, Data> getJMXDataMap(RequestSender requestSender, VisualVMModel model)
        {
        SortedMap<Object, Data> mapData = new TreeMap<Object, Data>();
        HttpRequestSender httpRequestSender = null;

        try
            {
            Set<ObjectName> clusterSet = requestSender.getAllClusters();

            if (requestSender instanceof HttpRequestSender)
                {
                httpRequestSender = (HttpRequestSender) requestSender;
            }

            for (Iterator<ObjectName> cacheNameIter = clusterSet.iterator(); cacheNameIter.hasNext(); )
                {
                ClusterData data           = new ClusterData();

                ObjectName  clusterObjName = cacheNameIter.next();

                AttributeList listAttr = requestSender.getAttributes(clusterObjName,
                        new String[] { ATTR_CLUSTER_NAME, ATTR_LICENSE_MODE, ATTR_VERSION,
                                ATTR_DEPARTURE_COUNT, ATTR_CLUSTER_SIZE });

                String sClusterName = getAttributeValueAsString(listAttr, ATTR_CLUSTER_NAME);

                // if we are using http request sender and the cluster name has not been set, then set it
                // so it can be used in the subsequent calls to be added to the base management URL
                // if we are connected to a WebLogic Server cluster via REST
                if (httpRequestSender != null && httpRequestSender.getClusterName() == null)
                    {
                    httpRequestSender.setClusterName(sClusterName);
                    }

                data.setColumn(ClusterData.CLUSTER_NAME, sClusterName);
                data.setColumn(ClusterData.LICENSE_MODE, getAttributeValueAsString(listAttr, ATTR_LICENSE_MODE));
                data.setColumn(ClusterData.VERSION, getAttributeValueAsString(listAttr, ATTR_VERSION));
                data.setColumn(ClusterData.DEPARTURE_COUNT,
                        Long.parseLong(getAttributeValueAsString(listAttr, ATTR_DEPARTURE_COUNT)));
                data.setColumn(ClusterData.CLUSTER_SIZE,
                        Integer.parseInt(getAttributeValueAsString(listAttr, ATTR_CLUSTER_SIZE)));

                mapData.put(data.getColumn(ClusterData.CLUSTER_NAME), data);
                }

            return mapData;
            }
        catch (Exception e)
            {
            LOGGER.log(Level.WARNING, "Error getting cluster statistics", e);

            return null;
            }
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = 7685333150479196716L;

    /**
     * Array index for cluster name.
     */
    public static final int CLUSTER_NAME = 0;

    /**
     * Array index for license mode.
     */
    public static final int LICENSE_MODE = 1;

    /**
     * Array index for version.
     */
    public static final int VERSION = 2;

    /**
     * Array index for departure count.
     */
    public static final int DEPARTURE_COUNT = 3;

    /**
     * Array index for cluster size;
     */
    public static final int CLUSTER_SIZE = 4;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(ClusterData.class.getName());

    /**
     * Report for cluster data.
     */
    public static final String REPORT_CLUSTER = "reports/visualvm/cluster-stats.xml";

    /**
     * JMX attribute name for Cluster Name.
     */
    private static final String ATTR_CLUSTER_NAME = "ClusterName";

    /**
     * JMX attribute name for License Mode.
     */
    private static final String ATTR_LICENSE_MODE = "LicenseMode";

    /**
     * JMX attribute name for Version.
     */
    private static final String ATTR_VERSION = "Version";

    /**
     * JMX attribute name for Memebrs Departure Count.
     */
    private static final String ATTR_DEPARTURE_COUNT = "MembersDepartureCount";

    /**
     * JMX attribute name for Cluster Size.
     */
    private static final String ATTR_CLUSTER_SIZE = "ClusterSize";
    }
