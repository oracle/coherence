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
        return new ArrayList<Map.Entry<Object, Data>>(getJMXDataMap(requestSender, model).entrySet());
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
            throws Exception
        {
        return getJMXDataMap(requestSender, model);
        }

    /**
     * Returns the JMX Map data.
     *
     * @param requestSender  the request sender to use
     * @param model          the {@link VisualVMModel} to use
     * @return  the processed data
     */
    protected SortedMap<Object, Data> getJMXDataMap(RequestSender requestSender, VisualVMModel model)
        {
        SortedMap<Object, Data> mapData = new TreeMap<>();

        try
            {
            Set<ObjectName> clusterSet = requestSender.getAllServiceMembers();

            Map<Integer, Integer> mapNodes = new HashMap<>();

            // iterate though all service members and figure out if at least one storage-enabled service runs on a node.
            for (Iterator<ObjectName> cacheNameIter = clusterSet.iterator(); cacheNameIter.hasNext(); )
                {
                ObjectName  objectName     = cacheNameIter.next();
                Integer     nodeId         = Integer.valueOf(objectName.getKeyProperty(ATTR_NODE_ID));
                AttributeList listAttr     = requestSender.getAttributes(objectName, new String[] { ATTR_OWNED_PRIMARY });
                Integer       ownedPrimary = Integer.parseInt(getAttributeValueAsString(listAttr, ATTR_OWNED_PRIMARY));

                if (mapNodes.containsKey(nodeId))
                    {
                    // the map contains the nodeId so get the value for owned primary partitions
                    int ownedPrimaryPartitions = mapNodes.get(nodeId);

                    if (ownedPrimaryPartitions <= 0 && ownedPrimary > 0)
                        {
                        // currently the node we are working with has no-storage enabled partitions
                        // and the the current service and node does, so lets update it
                        mapNodes.put(nodeId, ownedPrimary);
                        }
                    // else fallthrough as we leave any node with > 0 with that value
                    }
                else
                    {
                    // no entry exists so add it
                    mapNodes.put(nodeId, ownedPrimary);
                    }
                }

            // populate the real return map
            mapNodes.forEach((k,v) ->
                {
                NodeStorageData data = new NodeStorageData();
                data.setColumn(NODE_ID, k);
                data.setColumn(STORAGE_ENABLED, v.intValue() > 0);
                mapData.put(data.getColumn(NODE_ID), data);
                }
            );
            
            return mapData;
            }
        catch (Exception e)
            {
            LOGGER.log(Level.WARNING, "Error getting cluster statistics", e);

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
