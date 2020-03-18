/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter;


import com.tangosol.run.xml.XmlElement;

import java.util.Set;

/**
* This interface is used by the Reporter to get display information on a column.
*
* @author ew 2008.01.28
* @since Coherence 3.4
*/
public interface ReportColumnView
    {
    /**
    * Configure the Report Column view.
    *
    * @param xmlConfig the configuration XML
    */
    public void configure(XmlElement xmlConfig);

    /**
    * Set the QueryHandler context for the view.   The QueryHandler is the
    * "source" of the view data.
    *
    * @param handler the configuration XML
    */
    public void setQueryHandler(QueryHandler handler);

    /**
    * Obtain the formatted data.
    *
    * @param oKey   the key identifier
    *
    * @return the value to be included in the report
    */
    public String getOutputString(Object oKey);

    /**
    * Obtain the column header.
    *
    * @return the column header.
    */
    public String getHeader();

    /**
    * Determine if the column should be included in the output file.
    *
    * @return true if the column is included;  false otherwise
    */
    public boolean isVisible();

    /**
    * Determine if the value of the column is a single [detail] value.
    * Aggregates and constants often return false.
    *
    * @return true if multiple values exist; false otherwise
    */
    public boolean isRowDetail();

    /**
    * Obtain the string identifier for the ColumnView.
    *
    * @return the column identifier
    */
    public String getId();
    }

