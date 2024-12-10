/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.util.Filter;

import java.util.Map;


/**
* EntryFilter provides an extension to Filter for those cases in which both
* a key and a value may be necessary to evaluate the conditional inclusion
* of a particular object.
*
* @param <K>  the type of the Map entry key
* @param <V>  the type of the Map entry value
*
* @author cp/gg 2002.11.01
*/
public interface EntryFilter<K, V>
    extends Filter<V>
    {
    /**
    * Apply the test to a Map Entry.
    *
    * @param entry  the Map Entry to evaluate; never null
    *
    * @return true if the test passes, false otherwise
    */
    public boolean evaluateEntry(Map.Entry<? extends K, ? extends V> entry);
    }
