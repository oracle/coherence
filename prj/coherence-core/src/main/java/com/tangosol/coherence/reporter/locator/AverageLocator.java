/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.locator;


import com.tangosol.util.aggregator.DoubleAverage;
import com.tangosol.util.InvocableMap;

/**
* Calculates an average for values of any numberic type extracted from a set
* of MBeans.  All the extracted objects will be treated as Java <tt>double</tt>
* values. If the set of entries is empty, a <tt>null</tt> result is returned.
*
* @author ew 2008.01.28
* @since Coherence 3.4
*/
public class AverageLocator
        extends AggregateLocator
    {
    /**
    * @inheritDoc
    */
    public InvocableMap.EntryAggregator getAggregator()
        {
        return new DoubleAverage(m_veColumn);
        }
   }
