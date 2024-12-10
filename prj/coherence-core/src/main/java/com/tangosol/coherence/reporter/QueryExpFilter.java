/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter;


import com.tangosol.util.Filter;

import javax.management.ObjectName;
import javax.management.QueryEval;
import javax.management.QueryExp;

/**
* Wrapper class to map Coherence Filters into JMX QueryExp Interface.
*
* @author ew 2008.02.19
* @since Coherence 3.4
*/
public class QueryExpFilter
    extends QueryEval
    implements QueryExp

    {
    /**
    * Construct a QueryExp Wrapper for the Filter
    *
    * @param filter the Filter to be wrapped.
    */
    public QueryExpFilter(Filter filter)
        {
        m_filter = filter;
        }

    /**
    * @inheritDoc
    */
    public boolean apply(ObjectName objectName)
        {
        return m_filter == null || m_filter.evaluate(objectName);
        }

    /**
    * The wrapped Filter.
    */
    protected Filter m_filter;
    }
