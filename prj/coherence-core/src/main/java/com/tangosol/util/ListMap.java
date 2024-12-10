/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.LinkedHashMap;
import java.util.Map;


/**
* As of Coherence 3.2, the ListMap simply extends Java's own LinkedHashMap,
* which became available in JDK 1.4.
*/
public class ListMap<K, V>
        extends LinkedHashMap<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a ListMap.
    */
    public ListMap()
        {
        }

    /**
    * Construct a ListMap with the same mappings as the given map.
    *
    * @param map the map whose mappings are to be placed in this map
    */
    public ListMap(Map<? extends K, ? extends V> map)
        {
        super(map);
        }
    }
