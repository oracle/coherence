/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing;

import com.tangosol.net.GuardSupport;

import com.tangosol.net.cache.CacheStore;

import com.tangosol.util.ObservableMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Assert;

/**
* {@link CacheStore} implementation for testing Federated cache with CacheStore
* functionality.
* <p>
* The FederatedCacheStore stores all items in an {@link ObservableMap} that can be
* retrieved using the {@link #getStorageMap} method.
*/
public class FederatedCacheStore
        extends TestCacheStore
    {
    // ----- CacheStore interface -------------------------------------------

    /**
    * Return the value associated with the specified key, or key itself if the
    * key does not have an associated value in the underlying store.
    *
    * @param oKey key whose associated value is to be returned
    *
    * @return the value associated with the specified key, or <tt>null</tt> if
    *         no value is available for that key
    */
    public Object load(Object oKey)
        {
        log(isVerboseLoad(), "load(" + oKey + ")");
        logMethodInvocation("load");

        delay(getDurationLoad());

        checkForFailure(getFailureKeyLoad(), oKey);
        Object value = getStorageMap().get(oKey);
        if (value == null)
            {
            value = oKey;
            }
        return value;
        }

    /**
    * Return the values associated with each the specified keys in the passed
    * collection. If a key does not have an associated value in the underlying
    * store, then the return map that have an entry for that key with the key
    * as its value.
    *
    * @param colKeys a collection of keys to load
    *
    * @return a Map of keys to associated values for the specified keys
    */
    public Map loadAll(Collection colKeys)
        {
        log(isVerboseLoadAll(), "loadAll(" + colKeys + ")");
        logMethodInvocation("loadAll");

        delay(getDurationLoadAll());

        if (Thread.currentThread().getName().startsWith("ReadThread:"))
            {
            Assert.assertNotNull(GuardSupport.getThreadContext());
            }

        Map map = new HashMap();
        for (Iterator iterKeys = colKeys.iterator(); iterKeys.hasNext(); )
            {
            Object oKey = iterKeys.next();
            Object oValue;

            checkForFailure(getFailureKeyLoadAll(), oKey);
            oValue = getStorageMap().get(oKey);

            if (oValue != null)
                {
                map.put(oKey, oValue);
                }
            else
                {
                map.put(oKey, oKey);
                }
            }

        return map;
        }
    }
