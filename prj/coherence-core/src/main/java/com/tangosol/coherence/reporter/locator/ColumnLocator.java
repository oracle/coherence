/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.locator;


import com.tangosol.coherence.reporter.DataSource;
import com.tangosol.coherence.reporter.JMXQueryHandler;
import com.tangosol.coherence.reporter.QueryHandler;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;

import java.util.Set;


/**
* ColumnLocator is used to include custom information (for filtering or display)
* in a Report.
*
* @author ew 2008.01.28
* @since Coherence 3.4
*/
public interface ColumnLocator
    {
    /**
    * Configure the ColumnLocator.
    *
    * @param xmlConfig the XML configuration
    */
    public void configure(XmlElement xmlConfig);

    /**
    * Configure the ColumnLocator.
    *
    * @param xmlConfig the XML configuration
    * @param handler   the JMXQueryHandler associated with the Locator
    * @param source    the data source for the locator
    */
    public void configure(XmlElement xmlConfig, JMXQueryHandler handler, DataSource source);

    /**
    * Get the string Identifier of the QuerySource.
    *
    * @return the string Identifier of the QuerySource
    */
    public String getId();

    /**
    * Configure the queryHandler context for the Locator.  The handler is used
    * to instantiate related Locators.
    *
    * @param handler the handler associated with the Locator
    */
    public void setQuery(QueryHandler handler);

    /**
    * Locate and return the data value.
    *
    * @param oKey the key for the data
    *
    * @return  the aggregate or extracted value
    */
    public Object getValue(Object oKey);

    /**
    * Determine if the underlying data source is an aggregate value.
    *
    * @return true if the value is an aggregate
    */
    public boolean isAggregate();

    /**
    * Determine if the underlying data source has detailed values.
    *
    * @return true if the value is detailed
    */
    public boolean isRowDetail();
    /**
    * Clean up or process information after the completion of the query.
    *
    * @param setResults the set of keys from the query.
    */
    public void reset(Set setResults);

    /**
    * Obtain the EntryAggregator for the column.
    *
    * @return the EntryAggregator
    */
    public InvocableMap.EntryAggregator getAggregator();

    /**
    * Obtain the ValueExtractor for the column.
    *
    * @return the ValueExtractor
    */
    public ValueExtractor getExtractor();

    /**
    * Set the DataSource for the ColumnLocator.
    *
    * @param source the source for the data
    */
    public void setDataSource(DataSource source);

    /**
    * Get the configuration XML for the ColumnLocator
    *
    * @return an XmlElement containing the configuration
    */
    public XmlElement getConfig();
    }