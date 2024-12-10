/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import java.util.Collection;
import java.util.Map;


/**
* An extension to the CacheMap interface that supports runtime configuration
* and monitoring of various caching properties.
*
* @since Coherence 3.5
* @author cp  2009-01-13
*/
public interface ConfigurableCacheMap
        extends CacheMap
    {
    // ----- size limiting support ------------------------------------------

    /**
    * Determine the number of units that the cache currently stores.
    * <p>
    * Note: It is expected that the return type will be widened to a
    * <tt>long</tt> in Coherence 4.
    *
    * @return the current size of the cache in units
    */
    public int getUnits();

    /**
    * Determine the limit of the cache size in units. The cache will prune
    * itself automatically once it reaches its maximum unit level. This is
    * often referred to as the "high water mark" of the cache.
    * <p>
    * Note: It is expected that the return type will be widened to a
    * <tt>long</tt> in Coherence 4.
    *
    * @return the limit of the cache size in units
    */
    public int getHighUnits();

    /**
    * Update the maximum size of the cache in units. This is often referred
    * to as the "high water mark" of the cache.
    * <p>
    * Note: It is expected that the parameter will be widened to a
    * <tt>long</tt> in Coherence 4.
    *
    * @param cMax  the new maximum size of the cache, in units
    */
    public void setHighUnits(int cMax);

    /**
    * Determine the point to which the cache will shrink when it prunes.
    * This is often referred to as a "low water mark" of the cache. If the
    * cache incrementally prunes, then this setting will have no effect.
    * <p>
    * Note: It is expected that the parameter will be widened to a
    * <tt>long</tt> in Coherence 4.
    *
    * @return the number of units that the cache prunes to
    */
    public int getLowUnits();

    /**
    * Specify the point to which the cache will shrink when it prunes.
    * This is often referred to as a "low water mark" of the cache.
    * <p>
    * Note: It is expected that the parameter will be widened to a
    * <tt>long</tt> in Coherence 4.
    *
    * @param cUnits  the number of units that the cache prunes to
    */
    public void setLowUnits(int cUnits);

    /**
    * Determine the factor by which the Units, LowUnits and HighUnits
    * properties are adjusted. Using a binary unit calculator, for example,
    * the factor <tt>1048576</tt> could be used to count megabytes instead
    * of bytes.
    * <p>
    * Note: This property exists only to avoid changing the type of the
    * units, low units and high units properties from 32-bit values to
    * 64-bit values. It is expected that the parameter will be dropped
    * in Coherence 4.
    *
    * @return the units factor; the default is 1
    */
    public int getUnitFactor();

    /**
    * Determine the factor by which the Units, LowUnits and HighUnits
    * properties are adjusted. Using a binary unit calculator, for example,
    * the factor <tt>1048576</tt> could be used to count megabytes instead
    * of bytes.
    * <p>
    * Note: This property exists only to avoid changing the type of the
    * units, low units and high units properties from 32-bit values to
    * 64-bit values. It is expected that the parameter will be dropped
    * in Coherence 4.
    *
    * @param nFactor  the units factor; the default is 1
    */
    public void setUnitFactor(int nFactor);


    // ----- eviction support -----------------------------------------------
    /**
    * Evict a specified key from the cache, as if it had expired from the
    * cache. If the key is not in the cache, then the method has no effect.
    *
    * @param oKey  the key to evict from the cache
    */
    public void evict(Object oKey);

    /**
    * Evict the specified keys from the cache, as if they had each expired
    * from the cache.
    * <p>
    * The result of this method is defined to be semantically the same as
    * the following implementation:
    *
    * <pre><tt>
    * for (Iterator iter = colKeys.iterator(); iter.hasNext(); )
    *     {
    *     Object oKey = iter.next();
    *     evict(oKey);
    *     }
    * </tt></pre>
    *
    * @param colKeys  a collection of keys to evict from the cache
    */
    public void evictAll(Collection colKeys);

    /**
    * Evict all entries from the cache that are no longer valid, and
    * potentially prune the cache size if the cache is size-limited
    * and its size is above the caching low water mark.
    */
    public void evict();

    /**
    * Obtain the registered EvictionApprover.
    *
    * @return the EvictionApprover (could be null)
    */
    public EvictionApprover getEvictionApprover();

    /**
    * Set the EvictionApprover for this ConfigurableCacheMap.
    *
    * @param approver  the EvictionApprover
    */
    public void setEvictionApprover(EvictionApprover approver);


    // ----- expiry support -------------------------------------------------

    /**
    * Determine the default "time to live" for each individual cache entry.
    *
    * @return the number of milliseconds that a cache entry value will live,
    *         or zero if cache entries are never automatically expired
    */
    public int getExpiryDelay();

    /**
    * Specify the default "time to live" for cache entries. This does not
    * affect the already-scheduled expiry of existing entries.
    *
    * @param cMillis  the number of milliseconds that cache entries will
    *                 live, or zero to disable automatic expiry
    */
    public void setExpiryDelay(int cMillis);

    /**
     * Determine the next expiry time for the cache entries. This value is
     * supposed to be used only by the "active" expiry algorithms, so for
     * implementations that choose to return the value of zero the entries
     * will be evicted as with pre-existing "passive" expiry approach.
     *
     * @return the earliest time (using the {@link
     *         com.tangosol.util.Base#getSafeTimeMillis() SafeClock}) that one
     *         or more cache entries will expire or zero if the cache is empty,
     *         its entries never expire or the implementation chooses to
     *         avoid the pro-active eviction
     */
    public long getNextExpiryTime();


    // ----- interface: Entry -----------------------------------------------

    /**
    * Locate a cache Entry in the cache based on its key.
    *
    * @param oKey  the key object to search for
    *
    * @return the Entry or null
    */
    public Entry getCacheEntry(Object oKey);

    /**
    * A cache Entry carries information additional to the base Map Entry in
    * order to support eviction and expiry.
    */
    public interface Entry
            extends Map.Entry
        {
        /**
        * Indicate to the entry that it has been touched, such as when it is
        * accessed or modified.
        */
        public void touch();

        /**
        * Determine the number of times that the cache entry has been
        * touched (since the touch count was last reset).
        *
        * @return the number of times that the cache entry has been touched
        */
        public int getTouchCount();

        /**
        * Determine when the cache entry was last touched.
        *
        * @return the date/time value, in millis, when the entry was most
        *         recently touched
        */
        public long getLastTouchMillis();

        /**
        * Determine when the cache entry will expire, if ever.
        *
        * @return the date/time value, in millis, when the entry will (or
        *         did) expire; zero indicates no expiry
        */
        public long getExpiryMillis();

        /**
        * Specify when the cache entry will expire, or disable expiry. Note
        * that if the cache is configured for automatic expiry, each
        * subsequent update to this cache entry will reschedule the expiry
        * time.
        *
        * @param lMillis  pass the date/time value, in millis, for when the
        *        entry will expire, or pass zero to disable automatic expiry
        */
        public void setExpiryMillis(long lMillis);

        /**
        * Determine the number of cache units used by this Entry.
        *
        * @return an integer value 0 or greater, with a larger value
        *         signifying a higher cost; -1 implies that the Entry
        *         has been discarded
        */
        public int getUnits();

        /**
        * Specify the number of cache units used by this Entry.
        *
        * @param cUnits  an integer value 0 or greater, with a larger value
        *                signifying a higher cost
        */
        public void setUnits(int cUnits);
        }

    // ----- interface: EvictionPolicy --------------------------------------

    /**
    * Obtain the current EvictionPolicy used by the cache.
    *
    * @return the EvictionPolicy used by the cache
    */
    public EvictionPolicy getEvictionPolicy();

    /**
    * Set the EvictionPolicy for the cache to use.
    *
    * @param policy  an EvictionPolicy
    */
    public void setEvictionPolicy(EvictionPolicy policy);

    /**
    * An eviction policy is an object that the cache provides with access
    * information, and when requested, the eviction policy selects and
    * evicts entries from the cache. If the eviction policy needs to be
    * aware of changes to the cache, it must implement the MapListener
    * interface; if it does, it will automatically be registered to receive
    * MapEvents.
    *
    * @see AbstractEvictionPolicy
    */
    public interface EvictionPolicy
        {
        /**
        * This method is called by the cache to indicate that an entry has
        * been touched.
        *
        * @param entry  the Cache Entry that has been touched
        */
        public void entryTouched(Entry entry);

        /**
        * This method is called by the cache when the cache requires the
        * eviction policy to evict entries.
        *
        * @param cMaximum  the maximum number of units that should remain
        *                  in the cache when the eviction is complete
        */
        public void requestEviction(int cMaximum);

        /**
        * Obtain the name of the eviction policy. This is intended to be
        * human readable for use in a monitoring tool; examples include
        * "LRU" and "LFU".
        *
        * @return the name of the eviction policy
        */
        public String getName();
        }


    // ----- interface: EvictionApprover ------------------------------------

    /**
    * EvictionApprover is used to approve the eviction of an entry
    * from the underlying ConfigurableCacheMap.  An approver could be
    * configured on a ConfigurableCacheMap to provide fine-grained control
    * over whether a particular entry is eligible for eviction.
    */
    public interface EvictionApprover
        {
        /**
        * Get an eviction approval for the specified entry.
        *
        * @param entry the entry for which the underlying ConfigurableCacheMap
        *              is seeking an eviction approval
        *
        * @return true iff the specified entry is allowed to be evicted
        */
        public boolean isEvictable(Entry entry);

        // ----- constants --------------------------------------------------

        /**
        * Never approving EvictionApprover.
        */
        public static final EvictionApprover DISAPPROVER = new EvictionApprover()
            {
            public boolean isEvictable(Entry entry)
                {
                return false;
                }
            };
        }


    // ----- interface: UnitCalculator --------------------------------------

    /**
    * Obtain the current UnitCalculator used by the cache.
    *
    * @return the UnitCalculator used by the cache
    */
    public UnitCalculator getUnitCalculator();

    /**
    * Set the UnitCalculator for the cache to use.
    *
    * @param calculator  a UnitCalculator
    */
    public void setUnitCalculator(UnitCalculator calculator);

    /**
    * A unit calculator is an object that can calculate the cost of caching
    * an object.
    */
    public interface UnitCalculator
        {
        /**
        * Calculate a cache cost for the specified cache entry key and value.
        *
        * @param oKey    the cache key to evaluate for unit cost
        * @param oValue  the cache value to evaluate for unit cost
        *
        * @return an integer value 0 or greater, with a larger value
        *         signifying a higher cost
        *
        * @throws IllegalArgumentException if any of the specified object types
        *         cannot be processed by this calculator
        */
        public int calculateUnits(Object oKey, Object oValue);

        /**
        * Obtain the name of the unit calculator. This is intended to be
        * human readable for use in a monitoring tool; examples include
        * "SimpleMemoryCalculator" and "BinaryMemoryCalculator".
        *
        * @return the name of the unit calculator
        */
        public String getName();
        }
    }
