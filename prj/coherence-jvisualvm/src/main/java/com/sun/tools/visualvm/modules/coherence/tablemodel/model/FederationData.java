/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.sun.tools.visualvm.modules.coherence.tablemodel.model;


import com.sun.tools.visualvm.modules.coherence.VisualVMModel;

import com.sun.tools.visualvm.modules.coherence.helper.RequestSender;

import java.util.List;
import java.util.Map;


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
