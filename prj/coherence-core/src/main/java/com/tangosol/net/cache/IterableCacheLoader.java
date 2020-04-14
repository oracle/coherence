/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import java.util.Iterator;


/**
* A JCache CacheLoader that can iterate its underlying contents.
*
* @since Coherence 2.5
* @author cp 2004.09.22
*/
public interface IterableCacheLoader<K, V>
        extends CacheLoader<K, V>
    {
    /**
    * Iterate all keys in the underlying store.
    *
    * @return a read-only iterator of the keys in the underlying store
    *
    * @throws UnsupportedOperationException  if the underlying store is not
    *         iterable
    */
    public Iterator<K> keys();
    }
