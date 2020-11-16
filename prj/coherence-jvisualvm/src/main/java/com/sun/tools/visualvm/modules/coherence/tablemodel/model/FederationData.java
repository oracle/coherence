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
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A class to hold Federated Destination data.
 *
 * @author bb  2014.01.29
 *
 * @since  12.2.1
 */
public abstract class FederationData
        extends AbstractData
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create ServiceData passing in the number of columns.
     */
    public FederationData()
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
     * Retrieve the {@link Set} of federated services.
     * @param requestSender {@link HttpRequestSender}
     * @return the {@link Set} of federated services
     * @throws Exception in case of errors
     */
    protected Set<String> retrieveFederatedServices(HttpRequestSender requestSender) throws Exception
        {
        Set<String> setServices       = new HashSet<>();
        JsonNode            allStorageMembers = requestSender.getAllStorageMembers();
        JsonNode            serviceItemsNode  = allStorageMembers.get("items");

        if (serviceItemsNode != null && serviceItemsNode.isArray())
            {
            for (int i = 0; i < ((ArrayNode) serviceItemsNode).size(); i++)
                {
                JsonNode details = serviceItemsNode.get(i);
                String sServiceName = details.get("name").asText();
                JsonNode domainPartition = details.get("domainPartition");
                String sDomainPartition = domainPartition == null ? null : domainPartition.asText();
                String sType = details.get("type").asText();

                String sService = sDomainPartition == null ? sServiceName : sDomainPartition + "/" +  sServiceName;

                if (!setServices.contains(sService) && "FederatedCache".equals(sType))
                    {
                    setServices.add(sService);
                    }
                }
            }
         return setServices;
         }

    /**
     * Defines the data collected from destination MBeans, origin MBeans and aggregations.
     */
    public enum Column
        {
        KEY(0),
        SERVICE(1),
        PARTICIPANT(2),
        STATUS(3),
        // column is not sequential because it is coming from origin mbean while the above two
        // are coming from destination mbean.
        TOTAL_BYTES_SENT(4),
        TOTAL_BYTES_RECEIVED(3),
        TOTAL_MSGS_SENT(5),
        TOTAL_MSGS_RECEIVED(4);
        ;

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

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = -5166985357635016554L;
    }
