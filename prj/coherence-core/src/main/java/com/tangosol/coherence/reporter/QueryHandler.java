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
* Implementation independent QueryHandler interface.
*
* @author ew 2008.03.22
* @since Coherence 3.4
*/
public interface QueryHandler
    {
    /**
    * Set the XML Context and the query pattern for the Query
    *
    * @param xmlQuery   the Query XML
    * @param xmlContext the context for the Query.
    */
    public void setContext(XmlElement xmlQuery, XmlElement xmlContext);

    /**
    * Get the XML Context for the Query
    *
    * @return the Context XML
    */
    public XmlElement getContext();

    /**
    * Execute the Query
    */
    public void execute();

    /**
    * Get the keys from the query.
    *
    * @return a set of keys
    */
    public Set getKeys();

    /**
    * Get the value at for a particular key and column combination
    *
    * @param key     a key returned from getKeys
    * @param column  a column defined by the Query.
    *
    * @return the value from the query.
    */
    public Object getValue(Object key, Object column);

    /**
    * Determine if a particular column is an aggregate value or a detailed value
    *
    * @param  column the column identifier
    *
    * @return true if the column is an aggregate
    */
    public boolean isAggregate(Object column);

    /**
    * Determine if a particular column requires multiple rows to display.
    * Constant values are not aggregates but are also not detail.
    *
    * @param column the column identifier
    * @return true if the column is an aggregate
    */
    public boolean isDetail(Object column);

    /**
    * Return whether true if any ObjectNames under the query expression that
    * limits the report has a multi-tenant attribute.
    *
    * @return true if ObjectNames with a multi-tenant attibute exist
    */
    public boolean isMultiTenant();

    /**
    *  Replace macros in the template with values from key results
    *
    * @param sTemplate  a string template that contains macros.  A macro is a
    *                   curly brace {<column identifier>}.
    * @param oKey       a key value from the query.
    *
    * @return a string where the macros are replaced with values extracted from
    *         the query
    */
    public String replaceMacros(String sTemplate, Object oKey);

    /**
    * Set a correlated target key for the query.  This correlated key will be
    * used to extract outer query values to be used within the inner query.
    *
    * @param oTarget the outer query target key
    */
    public void setCorrelated(Object oTarget);

    /**
    * Execute the post process of the query handler.
    */
    public void postProcess();

    /**
    * Get the key set for a group by query.
    *
    * @return a set of keys to access the results
    */
    public Set getGroupKeys();
    }
