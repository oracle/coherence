/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.tablemodel.model;

/**
 * An interface allowing storage and retrieval of Coherence related data
 * from an object.
 *
 * @author tam  2013.11.14
 * @since  12.1.3
 */
public interface Data
    {

    // ----- Data methods ---------------------------------------------------

    /**
     * Get a given column value for the column index.
     *
     * @param nColumn the column index
     *
     * @return a given column value for the column index
     */
    public Object getColumn(int nColumn);

    /**
     * Sets a given column value for the column index
     *
     * @param nColumn the column index
     * @param oValue  the value to set
     */
    public void setColumn(int nColumn, Object oValue);
    }
