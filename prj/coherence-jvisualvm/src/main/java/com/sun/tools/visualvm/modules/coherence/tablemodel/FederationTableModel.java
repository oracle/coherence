/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.sun.tools.visualvm.modules.coherence.tablemodel;

import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.FederationDestinationData;

import java.util.Map;

/**
 * A model for holding federation data.
 *
 * @author cl  2014.02.17
 * @since  12.2.1
 */
public class FederationTableModel
        extends AbstractCoherenceTableModel<Object, Data>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates a table model with the given columns.
     *
     * @param asColumns the columns for this table model
     */
    public FederationTableModel(String[] asColumns)
        {
        super(asColumns);
        }

    /**
     * {@inheritDoc}
     */
    public Object getValueAt(int row, int col)
        {
        Map.Entry<Object, Data> entry = dataList.get(row);

        if (entry != null)
            {
            Object value = entry.getValue();

            if (value instanceof Data)
                {
                    Data data = (Data) value;
                switch(col)
                    {
                    case 0 : return data.getColumn(FederationDestinationData.Column.SERVICE.ordinal());
                    case 1 : return data.getColumn(FederationDestinationData.Column.PARTICIPANT.ordinal());
                    case 2 : return data.getColumn(FederationDestinationData.Column.STATUS.ordinal());
                    case 3 : return data.getColumn(FederationDestinationData.Column.TOTAL_BYTES_SENT.ordinal());
                    case 4 : return data.getColumn(FederationDestinationData.Column.TOTAL_MSGS_SENT.ordinal());
                    case 5 : return data.getColumn(FederationDestinationData.Column.TOTAL_BYTES_RECEIVED.ordinal());
                    case 6 : return data.getColumn(FederationDestinationData.Column.TOTAL_MSGS_RECEIVED.ordinal());
                    default : return null;
                    }
                }
            else
                {
                return null;
                }
            }
        else
            {
            return null;
            }
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = -8299887270471460520L;
    }
