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
import java.util.HashSet;
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
 * A class to hold basic service data.
 *
 * @author tam  2013.11.14
 * @since  12.1.3
 *
 */
public class ServiceData
        extends AbstractData
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create ServiceData passing in the number of columns.
     */
    public ServiceData()
        {
        super(PARTITIONS_PENDING + 1);
        }

    // ----- DataRetriever methods ------------------------------------------

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    public List<Map.Entry<Object, Data>> getJMXData(RequestSender sender, VisualVMModel model)
        {
        SortedMap<Object, Data> mapData              = new TreeMap<Object, Data>();
        Set<String>             setDistributedCaches = new HashSet<String>();
        Data                    data;

        try
            {
            // firstly obtain the list of services
            Set<ObjectName> servicesSet = sender.getAllServiceMembers();

            for (Iterator<ObjectName> cacheNameIter = servicesSet.iterator(); cacheNameIter.hasNext(); )
                {
                ObjectName cacheNameObjName = cacheNameIter.next();
                String     sServiceName     = cacheNameObjName.getKeyProperty("name");
                String     sDomainPartition = cacheNameObjName.getKeyProperty("domainPartition");

                if (sDomainPartition != null)
                    {
                    sServiceName = getFullServiceName(sDomainPartition, sServiceName);
                    }
                data = new ServiceData();

                data.setColumn(ServiceData.SERVICE_NAME, sServiceName);
                data.setColumn(ServiceData.MEMBERS, new Integer(0));
                data.setColumn(ServiceData.STORAGE_MEMBERS, new Integer(0));

                mapData.put(sServiceName, data);

                // if its dist cache or federated then save so we don't, double count size for repl
                // caches
                String sCacheType = (String) sender.getAttribute(cacheNameObjName, "Type");
                if (DISTRIBUTED_CACHE_TYPE.equals(sCacheType) || FEDERATED_CACHE_TYPE.equals(sCacheType) )
                    {
                    setDistributedCaches.add(sServiceName);
                    }
                }

            // update the distributed caches in the model for use later in
            // CacheStats
            model.setDistributedCaches(setDistributedCaches);

            // now loop through each individual service
            for (Iterator<Object> cacheNameIter = mapData.keySet().iterator(); cacheNameIter.hasNext(); )
                {
                String   sRawServiceName  = (String) cacheNameIter.next();
                String[] asServiceDetails = getDomainAndService(sRawServiceName);
                String   sDomainPartition = asServiceDetails[0];
                String   sServiceName     = asServiceDetails[1];
                // select only the current service so we can determine the number of storage-enabled
                // members.
                Set resultSet = sender.getMembersOfService(sServiceName, sDomainPartition);

                for (Iterator iter = resultSet.iterator(); iter.hasNext(); )
                    {
                    ObjectName objName = (ObjectName) iter.next();

                    data = mapData.get(sServiceName);

                    // only update the static information once as it will be the same across all members
                    if (data.getColumn(ServiceData.PARTITION_COUNT) == null)
                        {
                        AttributeList listAttr = sender.getAttributes(objName,
                            new String[]{ ATTR_PART_ENDANGERED, ATTR_PART_UNBALANCED, ATTR_PART_VULNERABLE,
                                          ATTR_STATUS_HA, ATTR_REQ_PENDING, ATTR_PARTITIONS_ALL });

                        data.setColumn(ServiceData.PARTITIONS_ENDANGERED,
                                Integer.parseInt(getAttributeValueAsString(listAttr, ATTR_PART_ENDANGERED)));
                        data.setColumn(ServiceData.PARTITIONS_UNBALANCED,
                                Integer.parseInt(getAttributeValueAsString(listAttr, ATTR_PART_UNBALANCED)));
                        data.setColumn(ServiceData.PARTITIONS_VULNERABLE,
                                Integer.parseInt(getAttributeValueAsString(listAttr, ATTR_PART_VULNERABLE)));
                        data.setColumn(ServiceData.STATUS_HA, (String) getAttributeValueAsString(listAttr, ATTR_STATUS_HA));
                        data.setColumn(ServiceData.PARTITIONS_PENDING,
                                       (int) (Long.parseLong(getAttributeValueAsString(listAttr, ATTR_REQ_PENDING))));
                        data.setColumn(ServiceData.PARTITION_COUNT,
                                Integer.parseInt(getAttributeValueAsString(listAttr, ATTR_PARTITIONS_ALL)));
                        }

                    data.setColumn(ServiceData.MEMBERS, (Integer) data.getColumn(ServiceData.MEMBERS) + 1);

                    if (Boolean.parseBoolean(sender.getAttribute(objName, "StorageEnabled")))
                        {
                        data.setColumn(ServiceData.STORAGE_MEMBERS,
                          (Integer) data.getColumn(ServiceData.STORAGE_MEMBERS) + 1);
                        }

                    mapData.put(sRawServiceName, data);
                    }
                }

            return new ArrayList<>(mapData.entrySet());
            }
        catch (Exception e)
            {
            LOGGER.log(Level.WARNING, "Error getting service statistics", e);

            return null;
            }
        }

    /**
     * {@inheritDoc}
     */
    public String getReporterReport()
        {
        return REPORT_SERVICE;
        }

    /**
     * {@inheritDoc}
     */
    public Data processReporterData(Object[] aoColumns, VisualVMModel model)
        {
        Data    data = new ServiceData();
        boolean fMT  = aoColumns[3] != null;
        int  nStart = 4;

        if (fMT)
            {
            data.setColumn(ServiceData.SERVICE_NAME, getFullServiceName(aoColumns[3].toString(),aoColumns[2].toString()));
            }
        else
            {
            data.setColumn(ServiceData.SERVICE_NAME, aoColumns[2]);
            }

        data.setColumn(ServiceData.STATUS_HA, aoColumns[nStart++]);
        data.setColumn(ServiceData.PARTITION_COUNT, new Integer(getNumberValue(aoColumns[nStart++].toString())));
        data.setColumn(ServiceData.PARTITIONS_ENDANGERED, new Integer(getNumberValue(aoColumns[nStart++].toString())));
        data.setColumn(ServiceData.PARTITIONS_VULNERABLE, new Integer(getNumberValue(aoColumns[nStart++].toString())));
        data.setColumn(ServiceData.PARTITIONS_UNBALANCED, new Integer(getNumberValue(aoColumns[nStart++].toString())));
        data.setColumn(ServiceData.STORAGE_MEMBERS, new Integer(getNumberValue(aoColumns[nStart++].toString())));
        data.setColumn(ServiceData.MEMBERS, new Integer(getNumberValue(aoColumns[nStart++].toString())));
        data.setColumn(ServiceData.PARTITIONS_PENDING, new Integer(getNumberValue(aoColumns[nStart++].toString())));

        // record the list of distributed & federated caches
        if (DISTRIBUTED_CACHE_TYPE.equals(aoColumns[nStart]) || FEDERATED_CACHE_TYPE.equals(aoColumns[nStart]))
            {
            setDistributedCaches.add(data.getColumn(ServiceData.SERVICE_NAME).toString());
            }

        // check if the service is federation
        if ("FederatedCache".equals(aoColumns[nStart]))
            {
            model.setFederationAvailable(true);
            }

        return data;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void preProcessReporterData(VisualVMModel model)
        {
        setDistributedCaches = new HashSet<String>();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SortedMap<Object, Data> postProcessReporterData(SortedMap<Object, Data> mapData, VisualVMModel model)
        {
        // update the distributed caches in the model for use later in
        // CacheStats
        model.setDistributedCaches(setDistributedCaches);

        return mapData;
        }

    @Override
    public SortedMap<Object, Data> getAggregatedDataFromHttpQuerying(VisualVMModel     model,
                                                                     HttpRequestSender requestSender) throws Exception
        {
        setDistributedCaches = new HashSet<String>();
        model.setDistributedCaches(setDistributedCaches);

        Set<ObjectName> setServiceMembers = requestSender.getAllServiceMembers();
        Set<String>     setServices       = new HashSet<>();
        for (ObjectName objName : setServiceMembers)
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
            JsonNode rootNode         = requestSender.getAggregatedServiceData(sServiceName, sDomainPartition);
            Data    data = new ServiceData();

            String sServiceType = rootNode.get("type").asText();
            data.setColumn(ServiceData.SERVICE_NAME, sService);

            JsonNode statusHaNode = rootNode.get("statusHA");
            String sStatusHA = statusHaNode == null ? "n/a" : statusHaNode.get(0).asText();

            data.setColumn(ServiceData.STATUS_HA, sStatusHA);

            data.setColumn(ServiceData.MEMBERS,
                    Integer.valueOf(getFirstMemberOfArray(rootNode,"memberCount")));

            // record the list of distributed, federated or replicated caches
            if (DISTRIBUTED_CACHE_TYPE.equals(sServiceType) || FEDERATED_CACHE_TYPE.equals(sServiceType) ||
                REPLICATED_CACHE_TYPE.equals(sServiceType))
                {
                data.setColumn(ServiceData.PARTITION_COUNT,
                        Integer.valueOf(getFirstMemberOfArray(rootNode,"partitionsAll")));

                if (!REPLICATED_CACHE_TYPE.equals(sServiceType))
                    {
                    setDistributedCaches.add(data.getColumn(ServiceData.SERVICE_NAME).toString());
                    }

                JsonNode storageEnabledNode      = rootNode.get("storageEnabled");
                JsonNode storageEnabledTrueNode  = storageEnabledNode == null ? null : storageEnabledNode.get("true");
                JsonNode storageEnabledTrueFalse = storageEnabledNode == null ? null : storageEnabledNode.get("false");
                String   storageEnabledMembers   = storageEnabledTrueNode == null ? "0" : storageEnabledTrueNode.asText();
                String   storageDisabledMembers  = storageEnabledTrueFalse == null ? "0" : storageEnabledTrueFalse.asText();
                
                // divide partitions stats by member count as these are aggregated from management over rest
                int nStorageMemberCount   = Integer.valueOf(storageEnabledMembers).intValue();
                int nTotalMemberCount     = Integer.valueOf(storageDisabledMembers).intValue() + nStorageMemberCount;
                int nPartitionsEndangered = Integer.valueOf(getFirstMemberOfArray(rootNode, "partitionsEndangered")).intValue();
                int nPartitionsVulnerable = Integer.valueOf(getFirstMemberOfArray(rootNode, "partitionsVulnerable")).intValue();
                int nPartitionsUnbalanced = Integer.valueOf(getFirstMemberOfArray(rootNode, "partitionsUnbalanced")).intValue();
                int nPartitionsPending    = Integer.valueOf(getChildValue("requestPendingCount", rootNode)).intValue();

                // retrieve data that is only relevant for Distributed or Federated services
                data.setColumn(ServiceData.PARTITIONS_ENDANGERED, Integer.valueOf(nPartitionsEndangered));
                data.setColumn(ServiceData.PARTITIONS_VULNERABLE, Integer.valueOf(nPartitionsVulnerable));
                data.setColumn(ServiceData.PARTITIONS_UNBALANCED, Integer.valueOf(nPartitionsUnbalanced));
                data.setColumn(ServiceData.PARTITIONS_PENDING, Integer.valueOf(nPartitionsPending / nTotalMemberCount));

                data.setColumn(ServiceData.STORAGE_MEMBERS, Integer.valueOf(nStorageMemberCount));
                }
            else
                {
                // is another type such as proxy/http
                data.setColumn(ServiceData.STORAGE_MEMBERS, Integer.valueOf(0));
                }

            // check if the service is federation
            if ("FederatedCache".equals(sServiceType))
                {
                model.setFederationAvailable(true);
                }

            mapData.put(data.getColumn(0), data);
            }
        return mapData;
        }

    /**
     * Get the first member of an array, if the provided field is an array. The default value in case
     * if not an array or null element is zero.
     *
     * @param nodeJson        the parent JSON node
     * @param sAttributeName  the attribute name
     *
     * @return the first member of the array
     */
    private String getFirstMemberOfArray(JsonNode nodeJson, String sAttributeName)
        {
        JsonNode partitionsVulnerableNode = nodeJson.get(sAttributeName);
        return partitionsVulnerableNode == null && partitionsVulnerableNode.isArray()
                ? 0 + ""
                : partitionsVulnerableNode.get(0).asText();
        }

    /**
     * Get the child attribute of a json node. The default value in case the child element is absent or null is zero.
     *
     * @param sAttributeName  the attribute name
     * @param nodeJson        the parent JSON node
     *
     * @return the value of a member variable or zero if null
     */
    private String getChildValue(String sAttributeName, JsonNode nodeJson)
        {
        JsonNode node = nodeJson.get(sAttributeName);
        if (node != null)
            {
            return node.asText();
            }
        return 0 + "";
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = -9192162461585760564L;

    /**
     * Array index for service name.
     */
    public static final int SERVICE_NAME = 0;

    /**
     * Array index for statusHA.
     */
    public static final int STATUS_HA = 1;

    /**
     * Array index for members.
     */
    public static final int MEMBERS = 2;

    /**
     * Array index for storage members.
     */
    public static final int STORAGE_MEMBERS = 3;

    /**
     * Array index for partition count.
     */
    public static final int PARTITION_COUNT = 4;

    /**
     * Array index for partitions endangered.
     */
    public static final int PARTITIONS_ENDANGERED = 5;

    /**
     * Array index for partitions vulnerable.
     */
    public static final int PARTITIONS_VULNERABLE = 6;

    /**
     * Array index for partitions unbalanced.
     */
    public static final int PARTITIONS_UNBALANCED = 7;

    /**
     * Array index for partitions pending.
     */

    public static final int PARTITIONS_PENDING = 8;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(ServiceData.class.getName());

    /**
     * Report for service data.
     */
    public static final String REPORT_SERVICE = "reports/visualvm/service-stats.xml";

    /**
     * Service of type DistributedCache.
     */
    private static final String DISTRIBUTED_CACHE_TYPE = "DistributedCache";

    /**
     * Service of type FederatedCache.
     */
    public static final String FEDERATED_CACHE_TYPE    = "FederatedCache";

    /**
     * Service of type ReplicatedCache.
     */
    public static final String REPLICATED_CACHE_TYPE    = "ReplicatedCache";

    // ----- data members ---------------------------------------------------

    /**
     * A {link Set} of the distributed caches so we can carry out checks later
     * in the data gathering process.
     */
    private Set<String> setDistributedCaches;

    /**
     * JMX attribute name for Partitions Endangered.
     */
    protected static final String ATTR_PART_ENDANGERED = "PartitionsEndangered";

    /**
     * JMX attribute name for Partitions Unbalanced.
     */
    protected static final String ATTR_PART_UNBALANCED = "PartitionsUnbalanced";

    /**
     * JMX attribute name for TPartitions Vulnerable.
     */
    protected static final String ATTR_PART_VULNERABLE = "PartitionsVulnerable";

    /**
     * JMX attribute name for StatusHA.
     */
    protected static final String ATTR_STATUS_HA = "StatusHA";

    /**
     * JMX attribute name for Request PendingCount.
     */
    protected static final String ATTR_REQ_PENDING = "RequestPendingCount";

    /**
     * JMX attribute name for Partitions All.
     */
    protected static final String ATTR_PARTITIONS_ALL = "PartitionsAll";
    }
