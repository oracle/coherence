/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;


/**
* An abstract base class for custom cache eviction policies.
*
* @author jh  2005.12.14
*/
public abstract class AbstractEvictionPolicy
        extends Base
        implements ConfigurableCacheMap.EvictionPolicy, MapListener
    {
    // ----- EvictionPolicy interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public String getName()
        {
        return ClassHelper.getSimpleName(getClass());
        }


    // ------ MapListener interface -----------------------------------------

    /**
    * {@inheritDoc}
    */
    public void entryInserted(MapEvent evt)
        {
        entryUpdated(getEntry(evt));
        }

    /**
    * {@inheritDoc}
    */
    public void entryUpdated(MapEvent evt)
        {
        entryUpdated(getEntry(evt));
        }

    /**
    * {@inheritDoc}
    */
    public void entryDeleted(MapEvent evt)
        {
        ensureCache(evt);
        }


    // ----- AbstractEvictionPolicy methods ---------------------------------

    /**
    * This method is called to indicate that an entry has been either
    * inserted or updated.
    *
    * @param entry the Entry that has been inserted or updated
    */
    public abstract void entryUpdated(ConfigurableCacheMap.Entry entry);


    // ----- internal helpers -----------------------------------------------

    /**
    * Return the ConfigurableCacheMap that uses this eviction policy.
    * <p>
    * If the ConfigurableCacheMap property has not been intialized, it is set
    * to the ConfigurableCacheMap that raised the given event.
    *
    * @param evt  the MapEvent raised by the ConfigurableCacheMap that uses
    *             this eviction policy
    *
    * @return the ConfigurableCacheMap that uses this eviction policy
    */
    protected ConfigurableCacheMap ensureCache(MapEvent evt)
        {
        ConfigurableCacheMap cache = m_cache;
        if (cache == null)
            {
            try
                {
                cache = m_cache = (ConfigurableCacheMap) evt.getMap();
                }
            catch (ClassCastException e)
                {
                throw new IllegalArgumentException("Illegal map type: " +
                        evt.getMap().getClass().getName());
                }
            }

        return cache;
        }

    /**
    * Return the map entry associated with the given map event.
    *
    * @param evt  a map event raised by the cache that uses this eviction
    *             policy
    *
    * @return the map entry associated with the given event
    */
    protected ConfigurableCacheMap.Entry getEntry(MapEvent evt)
        {
        ConfigurableCacheMap cache = ensureCache(evt);
        if (cache instanceof LocalCache)
            {
            LocalCache lc = (LocalCache) cache;
            return (ConfigurableCacheMap.Entry) lc.getEntryInternal(evt.getKey());
            }
        else
            {
            // if the ConfigurableCacheMap.getCacheEntry() implementation
            // causes an eviction or notifies the EvictionPolicy that the
            // entry has been touched, we'll go into an infinite recursion
            try
                {
                return cache.getCacheEntry(evt.getKey());
                }
            catch (StackOverflowError e)
                {
                throw new StackOverflowError(cache.getClass().getName()
                        + "#getCacheEntry() implementation causes an infinite"
                        + " recursion when used with " + getClass().getName()
                        + "#getEntry() implementation (inherited from "
                        + AbstractEvictionPolicy.class.getName() + ")");
                }
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the ConfigurableCacheMap that uses this eviction policy. The
    * ConfigurableCacheMap is set the first time a map event is processed by
    * the eviction policy.
    *
    * @return the ConfigurableCacheMap or null if a map event has not yet
    *         been processed by this eviction policy
    */
    protected ConfigurableCacheMap getCache()
        {
        return m_cache;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The ConfigurableCacheMap that is using this eviction policy.
    */
    private ConfigurableCacheMap m_cache;
    }
