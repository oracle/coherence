/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.tablemodel;

import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.JTable;

import javax.swing.table.AbstractTableModel;

/**
 * An abstract implementation of {@link AbstractTableModel} which is extended
 * to provide the data for the {@link JTable} implementations.
 *
 * @param <K>  The key object for each row - the type of this will determine sorting
 * @param <V>  The value object for each row
 *
 * @author tam  2013.11.14
 * @since  12.1.3
 */
public abstract class AbstractCoherenceTableModel<K, V>
        extends AbstractTableModel
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Creates the mode with the string of column names.
     *
     * @param asColumnNames  the column names
     */
    public AbstractCoherenceTableModel(String[] asColumnNames)
        {
        this.asColumnNames = asColumnNames;
        }

    // ----- TableModel methods ---------------------------------------------

    /**
     * Returns the column count for this model.
     *
     * @return the column count for this model
     */
    public int getColumnCount()
        {
        if (asColumnNames == null)
            {
            throw new IllegalStateException("No definition of AbstractMeasures for this model. " + this.getClass());
            }

        return asColumnNames.length;
        }

    /**
     * {@inheritDoc}
     */
    public int getRowCount()
        {
        return dataList == null ? 0 : dataList.size();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getColumnName(int col)
        {
        if (asColumnNames == null)
            {
            throw new IllegalStateException("No definition of AbstractMeasures for this model. " + this.getClass());
            }

        return asColumnNames[col];
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getColumnClass(int col)
        {
        if (getValueAt(0, col) != null)
            {
            return getValueAt(0, col).getClass();
            }
        else
            {
            return (String.class);
            }
        }

    /**
     * {@inheritDoc}
     */
    public Object getValueAt(int row, int col)
        {
        if (dataList == null || dataList.size() == 0)
            {
            return null;
            }

        Map.Entry<K, V> entry = dataList.get(row);

        if (entry != null)
            {
            Object value = entry.getValue();

            if (value instanceof Data)
                {
                return ((Data) value).getColumn(col);
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

    // ----- AbstractCoherenceTableModel methods ----------------------------

    /**
     * Returns the data list for this model.
     *
     * @param dataList the data list for this model
     */
    public void setDataList(List<Map.Entry<K, V>> dataList)
        {
        this.dataList = dataList;
        }

    /**
     * Returns the column names for this model.
     *
     * @return the column names for this model
     */
    public String[] getColumnNames()
        {
        return asColumnNames;
        }


    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = 8366117699998286805L;

    // ----- data members ---------------------------------------------------

    /**
     * The data list for the model.
     */
    protected List<Map.Entry<K, V>> dataList = Collections.emptyList();

    /**
     * The column names for the model.
     */
    protected String[] asColumnNames;
    }
