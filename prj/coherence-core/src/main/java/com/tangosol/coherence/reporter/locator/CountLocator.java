/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.locator;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.aggregator.Count;


/**
* Class to calculate the maximum data value for a column
*
* @author ew 2008.01.28
* @since Coherence 3.4
*/
public class CountLocator
        extends AggregateLocator
    {
    public InvocableMap.EntryAggregator getAggregator()
        {
        return new Count();
        }
    }
