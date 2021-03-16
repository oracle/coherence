/*
 * Copyright (c) 2000, 2020, 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.sun.tools.visualvm.modules.coherence.tablemodel.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sun.tools.visualvm.modules.coherence.VisualVMModel;
import com.sun.tools.visualvm.modules.coherence.helper.HttpRequestSender;
import com.sun.tools.visualvm.modules.coherence.helper.JMXRequestSender;
import com.sun.tools.visualvm.modules.coherence.helper.RequestSender;

import javax.management.AttributeList;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sun.tools.visualvm.modules.coherence.helper.JMXUtils.getAttributeValueAsString;

/**
 * A class to hold node storage data.
 *
 * @author tam 
 * @since  12.2.1.4
 */
public class NodeStorageData
        extends AbstractData
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Create NodeStorageData passing in the number of columns.
     */
    public NodeStorageData()
        {
        super(STORAGE_ENABLED + 1);
        }

    @Override
    public List<Map.Entry<Object, Data>> getJMXData(RequestSender requestSender, VisualVMModel model)
        {
        // use either JMX or HTTP
        return new ArrayList<>(requestSender instanceof JMXRequestSender
                ? getJMXDataMap(requestSender, model).entrySet()
                : getAggregatedDataFromHttpQuerying(model, (HttpRequestSender) requestSender).entrySet());
        }

    @Override
    public String getReporterReport()
        {
        return null;
        }

    @Override
    public Data processReporterData(Object[] aoColumns, VisualVMModel model)
        {
        return null;
        }

    @Override
    public SortedMap<Object, Data> getAggregatedDataFromHttpQuerying(VisualVMModel model, HttpRequestSender requestSender)
        {
        try
            {
            JsonNode nodeStorage = requestSender.getNodeStorage();
            Map<Integer, Integer> mapNodes = new HashMap<>();

            JsonNode nodeDetails = nodeStorage.get("items");

            if (nodeDetails != null && nodeDetails.isArray())
                {
                for (int i = 0; i < nodeDetails.size(); i++)
                    {
                    JsonNode details = nodeDetails.get(i);
                    int nNodeId = details.get("nodeId").asInt();
                    int nOwnedPartitions = details.get("ownedPartitionsPrimary").asInt();
                    checkNode(mapNodes, nNodeId, nOwnedPartitions);
                    }
                }

            return populateMap(mapNodes);
            }

        catch (Exception e)
            {
            LOGGER.log(Level.WARNING, "Error getting node storage statistics", e);

            return null;
            }
        }

    /**
     * Check if a node is storage enabled or not byt checking if the owned
     * partitions > 0 on at least one for the services.
     *
     * @param mapNodes      {@link Map} of nodes
     * @param nNodeId       current node id
     * @param nOwnedPrimary current owned primary partitions
     */
    private void checkNode(Map<Integer, Integer> mapNodes, int nNodeId, int nOwnedPrimary)
        {
        if (mapNodes.containsKey(nNodeId))
            {
            // the map contains the nodeId so get the value for owned primary partitions
            int ownedPrimaryPartitions = mapNodes.get(nNodeId);

            if (ownedPrimaryPartitions <= 0 && nOwnedPrimary > 0)
                {
                // currently the node we are working with has no-storage enabled partitions
                // and the the current service and node does, so lets update it
                mapNodes.put(nNodeId, nOwnedPrimary);
                }
            // else fallthrough as we leave any node with > 0 with that value
            }
        else
            {
            // no entry exists so add it
            mapNodes.put(nNodeId, nOwnedPrimary);
            }
        }

    /**
     * Populate the return {@link Map} with the updated node storage details.
     *
     * @param mapNodes interim node {@link Map}
     * @return the storage nodes
     */
    private SortedMap<Object, Data> populateMap(Map<Integer, Integer> mapNodes)
        {
        SortedMap<Object, Data> mapData = new TreeMap<>();
        // populate the real return map
        mapNodes.forEach((k, v) ->
            {
            NodeStorageData data = new NodeStorageData();
            data.setColumn(NODE_ID, k);
            data.setColumn(STORAGE_ENABLED, v > 0);
            mapData.put(data.getColumn(NODE_ID), data);
            });
        return mapData;
        }

    /**
     * Returns the JMX Map data.
     *
     * @param requestSender the request sender to use
     * @param model         the {@link VisualVMModel} to use
     * @return the processed data
     */
    protected SortedMap<Object, Data> getJMXDataMap(RequestSender requestSender, VisualVMModel model)
        {
        try
            {
            if (requestSender instanceof HttpRequestSender)
                {
                return getAggregatedDataFromHttpQuerying(model, (HttpRequestSender) requestSender);
                }

            Set<ObjectName> clusterSet = requestSender.getAllServiceMembers();

            Map<Integer, Integer> mapNodes = new HashMap<>();

            // iterate though all service members and figure out if at least one storage-enabled service runs on a node.
            for (Iterator<ObjectName> cacheNameIter = clusterSet.iterator(); cacheNameIter.hasNext(); )
                {
                ObjectName  objectName     = cacheNameIter.next();
                int           nodeId       = Integer.valueOf(objectName.getKeyProperty(ATTR_NODE_ID));
                AttributeList listAttr     = requestSender.getAttributes(objectName, new String[] { ATTR_OWNED_PRIMARY });
                int           ownedPrimary = Integer.parseInt(getAttributeValueAsString(listAttr, ATTR_OWNED_PRIMARY));

                checkNode(mapNodes, nodeId, ownedPrimary);
                }

            return populateMap(mapNodes);
            }
        catch (Exception e)
            {
            LOGGER.log(Level.WARNING, "Error getting node storage statistics", e);

            return null;
            }
        }


    // ----- constants ------------------------------------------------------

    /**
     * Attribute name for Node Id.
     */
    private static final String ATTR_NODE_ID = "nodeId";

    /**
     * Attribute name for OwnedPartitionsPrimary.
     */
    private static final String ATTR_OWNED_PRIMARY = "OwnedPartitionsPrimary";
    
    /**
     * Array index for node id.
     */
    public static final int NODE_ID = 0;

    /**
     * Array index for storage enabled.
     */
    public static final int STORAGE_ENABLED = 1;
    
    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(NodeStorageData.class.getName());
    }
