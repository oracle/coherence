/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.internal;


import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;


/**
* Helper class that contains various utility methods to simplify the usage of
* the SessionLocalBackingMap.
*
* @author gg 2011.05.18
*/
public abstract class SessionLocalHelper
    {
    /**
    * Retrieve a reference to the SessionLocalBackingMap for the
    * specified NamedCache.
    *
    * @param cache  the NamedCache to retrieve the local backing map for
    *
    * @return the corresponding SessionLocalBackingMap reference or null if
    *         the node is not storage enabled
    *
    * @throws ClassCastException if the specified NamedCache is not backed up
    *                            by the SessionLocalBackingMap
    */
    public static SessionLocalBackingMap getBackingMap(NamedCache cache)
        {
        BackingMapContext ctx = cache.getCacheService().getBackingMapManager().
            getContext().getBackingMapContext(cache.getCacheName());
        return ctx == null ? null : (SessionLocalBackingMap) ctx.getBackingMap();
        }

    /**
    * Get the value from the NamedCache backed up by the SessionLocalBackingMap.
    *
    * @param cache  the NamedCache to retrieve the value from
    * @param oKey   the key in Object format
    *
    * @return the corresponding value
    *
    * @throws ClassCastException if the specified NamedCache is not backed up
    *                            by the SessionLocalBackingMap
    */
    public static Object get(NamedCache cache, Object oKey)
        {
        BackingMapManagerContext ctxMgr = cache.getCacheService().getBackingMapManager().getContext();
        BackingMapContext        ctxMap = ctxMgr.getBackingMapContext(cache.getCacheName());

        // context could be null for storage-disabled nodes
        if (ctxMap != null)
            {
            Binary binKey = (Binary) ctxMgr.getKeyToInternalConverter().convert(oKey);

            // check if the key is owned by this node
            if (ctxMgr.isKeyOwned(binKey))
                {
                SessionLocalBackingMap mapBacking =
                    (SessionLocalBackingMap) ctxMap.getBackingMap();
                try
                    {
                    Object oValue = mapBacking.getObject(binKey);

                    // return the retrieved value ONLY if we still own the key
                    if (ctxMgr.isKeyOwned(binKey))
                        {
                        return oValue;
                        }
                    }
                catch (RuntimeException e)
                    {
                    // may be thrown if the ownership has moved
                    }
                }
            }

        // the key is not owned; go through the front door
        return cache.get(oKey);
        }

    /**
    * Put the value into the NamedCache backed up by the SessionLocalBackingMap.
    *
    * @param cache   the NamedCache to put the value into
    * @param oKey    the key in Object format
    * @param oValue  the value in Object format
    *
    * @throws ClassCastException if the specified NamedCache is not backed up
    *                            by the SessionLocalBackingMap
    */
    public static void put(NamedCache cache, Object oKey, Object oValue)
        {
        BackingMapManagerContext ctxMgr = cache.getCacheService().getBackingMapManager().getContext();
        BackingMapContext        ctxMap = ctxMgr.getBackingMapContext(cache.getCacheName());

        // context could be null for storage-disabled nodes
        if (ctxMap != null)
            {
            Binary binKey = (Binary) ctxMgr.getKeyToInternalConverter().convert(oKey);

            // check if the key is owned by this node
            if (ctxMgr.isKeyOwned(binKey))
                {
                SessionLocalBackingMap mapBacking =
                    (SessionLocalBackingMap) ctxMap.getBackingMap();
                try
                    {
                    mapBacking.putObject(binKey, oValue);

                    // assume success ONLY if we still own the key
                    if (ctxMgr.isKeyOwned(binKey))
                        {
                        return;
                        }
                    }
                catch (RuntimeException e)
                    {
                    // may be thrown if the ownership has moved
                    }
                }
            }

        // the key is not owned; go through the front door
        cache.putAll(Collections.singletonMap(oKey, oValue));
        }

    /**
    * Retrieve the value of the specified entry in Object format without paying
    * any serialization cost.
    *
    * @param entry  an Entry to retrive the value from
    *
    * @return the corresponding value
    *
    * @throws ClassCastException if the passed in entry is not a BinaryEntry or
    *         the corresponding backing map is not a SessionLocalBackingMap
    */
    public static Object getValue(InvocableMap.Entry entry)
        {
        BinaryEntry            entryBin   = (BinaryEntry) entry;
        SessionLocalBackingMap mapBacking = (SessionLocalBackingMap)
            entryBin.getBackingMap();
        return mapBacking.getObject(entryBin.getBinaryKey());
        }

    /**
    * Create an iterator for all the keys in the SessionLocalBackingMap
    *
    * @param cache  the NamedCache to retrieve the keys from
    *
    * @return an Iterator with all the keys from the SessionLocalBackingMap
    */
    public static Iterator getLocalKeysIterator(NamedCache cache)
        {
        SessionLocalBackingMap backingMap = getBackingMap(cache);
        if (backingMap != null)
            {
            Set binKeySet = backingMap.keySet();
            Iterator binKeyIterator = binKeySet.iterator();
            ArrayList origKeys = new ArrayList(binKeySet.size());
            while (binKeyIterator.hasNext()) {
              Binary binKey = (Binary) binKeyIterator.next();
              Object origKey = backingMap.convertKeyFromInternal(binKey);
              origKeys.add(origKey);
            }
            return origKeys.iterator();
            }
        return null;
        }

    /**
     * Check if a key exists in the SessionLocalBackingMap
     *
     * @param cache  the NamedCache
     * @param key    the key object to check for
     *
     * @return true, if the key exists in the SessionLocalBackingMap
     */
    public static boolean existsInBackingMap(NamedCache cache, Object key)
        {
        SessionLocalBackingMap backingMap = getBackingMap(cache);
        Object binKey = backingMap.convertKeyToInternal(key);
        return backingMap.containsKey(binKey);
        }
    }