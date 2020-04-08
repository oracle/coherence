/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.locator;


import com.tangosol.util.InvocableMap;
import com.tangosol.util.aggregator.ComparableMin;


/**
* Calculates a minimum of numeric values extracted from a set of MBeans. All
* the extracted Number objects will be treated as Java <tt>double</tt> values.
*
*
* @since Coherence 3.4
* @author ew 2008.01.28
*/

public class MinLocator
        extends AggregateLocator
    {
    public InvocableMap.EntryAggregator getAggregator()
        {
        return new ComparableMin(m_veColumn);
        }
   }
