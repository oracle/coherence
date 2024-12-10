/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.util.Base;


/**
* An abstract base class for the JCache CacheLoader.
*
* @author cp 2003.05.29
*/
public abstract class AbstractCacheLoader<K, V>
        extends Base
        implements CacheLoader<K, V>
    {
    // ----- CacheLoader interface ------------------------------------------

    /**
    * Return the value associated with the specified key, or null if the
    * key does not have an associated value in the underlying store.
    *
    * @param key  key whose associated value is to be returned
    *
    * @return the value associated with the specified key, or
    *         <tt>null</tt> if no value is available for that key
    */
    public abstract V load(K key);
    }
