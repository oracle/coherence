/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
* An ObservableMap implementation that extends the SafeHashMap.
* <p>
* This Map implements the ObservableMap interface, meaning it provides
* event notifications to any interested listener for each insert, update and
* delete.
*
* @author cp  2002.02.12
*/
public class ObservableHashMap<K, V>
        extends SafeHashMap<K, V>
        implements ObservableMap<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the ObservableHashMap.
    */
    public ObservableHashMap()
        {
        super();
        }

    /**
    * Construct an ObservableHashMap using the specified settings.
    *
    * @param cInitialBuckets  the initial number of hash buckets, 0 &lt; n
    * @param flLoadFactor     the acceptable load factor before resizing
    *                         occurs, 0 &lt; n, such that a load factor of 1.0
    *                         causes resizing when the number of entries
    *                         exceeds the number of buckets
    * @param flGrowthRate     the rate of bucket growth when a resize occurs,
    *                         0 &lt; n, such that a growth rate of 1.0 will
    *                         double the number of buckets:
    *                         bucketCount = bucketCount * (1 + growthRate)
    */
    public ObservableHashMap(int cInitialBuckets, float flLoadFactor, float flGrowthRate)
        {
        super(cInitialBuckets, flLoadFactor, flGrowthRate);
        }


    // ----- Map interface --------------------------------------------------

    /**
    * Store a value in the cache.
    *
    * @param key    the key with which to associate the cache value
    * @param value  the value to cache
    *
    * @return the value that was cached associated with that key, or null if
    *         no value was cached associated with that key
    */
    public synchronized V put(K key, V value)
        {
        // COH-6009: map mutations must be synchronized with event dispatch
        // to ensure in-order delivery
        return super.put(key, value);
        }

    /**
    * Remove an entry from the cache.
    *
    * @param oKey the key of a cached value
    *
    * @return the value that was cached associated with that key, or null if
    *         no value was cached associated with that key
    */
    public synchronized V remove(Object oKey)
        {
        // COH-6009: map mutations must be synchronized with event dispatch
        // to ensure in-order delivery
        Entry<K, V> entry = (Entry<K, V>) getEntryInternal(oKey);
        if (entry == null)
            {
            return null;
            }
        else
            {
            removeEntryInternal(entry);
            entry.onRemove();
            return entry.getValue();
            }
        }

    /**
    * Remove everything from the cache, notifying any registered listeners.
    */
    public synchronized void clear()
        {
        clear(false);
        }

    // ----- ObservableMap methods ------------------------------------------

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void addMapListener(MapListener listener)
        {
        addMapListener(listener, (Filter) null, false);
        }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void removeMapListener(MapListener listener)
        {
        removeMapListener(listener, (Filter) null);
        }

    @Override
    public synchronized void addMapListener(MapListener<? super K, ? super V> listener, K key, boolean fLite)
        {
        Base.azzert(listener != null);

        MapListenerSupport support = m_listenerSupport;
        if (support == null)
            {
            support = m_listenerSupport = new MapListenerSupport();
            }

        support.addListener(listener, key, fLite);
        }

    @Override
    public synchronized void removeMapListener(MapListener<? super K, ? super V> listener, K key)
        {
        Base.azzert(listener != null);

        MapListenerSupport support = m_listenerSupport;
        if (support != null)
            {
            support.removeListener(listener, key);
            if (support.isEmpty())
                {
                m_listenerSupport = null;
                }
            }
        }

    @Override
    public synchronized void addMapListener(MapListener<? super K, ? super V> listener, Filter filter, boolean fLite)
        {
        Base.azzert(listener != null);

        MapListenerSupport support = m_listenerSupport;
        if (support == null)
            {
            support = m_listenerSupport = new MapListenerSupport();
            }

        support.addListener(listener, filter, fLite);
        }

    @Override
    public synchronized void removeMapListener(MapListener<? super K, ? super V> listener, Filter filter)
        {
        Base.azzert(listener != null);

        MapListenerSupport support = m_listenerSupport;
        if (support != null)
            {
            support.removeListener(listener, filter);
            if (support.isEmpty())
                {
                m_listenerSupport = null;
                }
            }
        }

    // ----- ObservableHashMap methods --------------------------------------

    /**
     * Removes all mappings from this map.
     *
     * Note: the removal of entries caused by this truncate operation will
     * not be observable.
     *
     * @since 12.2.1.4
     */
    public synchronized void truncate()
        {
        clear(true);
        }

    // ----- event dispatching ----------------------------------------------

    /**
    * Accessor for the MapListenerSupport for sub-classes.
    *
    * @return the MapListenerSupport, or null if there are no listeners
    */
    protected MapListenerSupport getMapListenerSupport()
        {
        return m_listenerSupport;
        }

    /**
    * Determine if the OverflowMap has any listeners at all.
    *
    * @return true iff this OverflowMap has at least one MapListener
    */
    protected boolean hasListeners()
        {
        // m_listenerSupport defaults to null, and it is reset to null when
        // the last listener unregisters
        return m_listenerSupport != null;
        }

    /**
    * Dispatch the passed event.
    *
    * @param evt   a CacheEvent object
    */
    protected void dispatchEvent(MapEvent evt)
        {
        MapListenerSupport listenerSupport = getMapListenerSupport();
        if (listenerSupport != null)
            {
            // the events can only be generated while the current thread
            // holds the monitor on this map
            synchronized (this)
                {
                listenerSupport.fireEvent(evt, false);
                }
            }
        }


    // ----- inner class: Entry ---------------------------------------------

    /**
    * Factory method.  This method exists to allow the Cache class to be
    * easily inherited from by allowing the Entry class to be easily
    * sub-classed.
    *
    * @return an instance of Entry that holds the passed cache value
    */
    protected SafeHashMap.Entry<K, V> instantiateEntry()
        {
        return new Entry<>();
        }

    /**
    * A holder for a cached value.
    *
    * @author cp  2000.12.12 (StmtCacheValue)
    * @author cp  2001.04.19
    */
    @SuppressWarnings("TypeParameterHidesVisibleType")
    protected class Entry<K, V>
            extends SafeHashMap.Entry<K, V>
        {
        // ----- Map.Entry interface ------------------------------------

        /**
        * This method is invoked when the containing Map has actually
        * added this Entry to itself.
        */
        @SuppressWarnings("unchecked")
        protected void onAdd()
            {
            // issue add notification
            ObservableHashMap map = ObservableHashMap.this;
            if (map.hasListeners())
                {
                map.dispatchEvent(new MapEvent(map, MapEvent.ENTRY_INSERTED,
                    getKey(), null, getValue()));
                }
            }

        /**
        * Update the cached value.
        *
        * @param oValue  the new value to cache
        *
        * @return the old cache value
        */
        @SuppressWarnings("unchecked")
        public V setValue(V oValue)
            {
            ObservableHashMap map = ObservableHashMap.this;
            synchronized (map)
                {
                // perform the entry update
                V oPrev = super.setValue(oValue);

                // note: previous to Coherence 3.1, there was an optimization
                // that would only raise an event if the reference was changing,
                // i.e. if (oPrev != oValue) {..}

                // issue update notification
                if (map.hasListeners())
                    {
                    map.dispatchEvent(new MapEvent(map, MapEvent.ENTRY_UPDATED,
                        getKey(), oPrev, oValue));
                    }

                return oPrev;
                }
            }

        /**
        * Called to inform the Entry that it has been removed.
        */
        @SuppressWarnings("unchecked")
        protected void onRemove()
            {
            // issue remove notification
            ObservableHashMap map = ObservableHashMap.this;
            if (map.hasListeners())
                {
                map.dispatchEvent(new MapEvent(map, MapEvent.ENTRY_DELETED,
                    getKey(), getValue(), null));
                }
            }
        }

    // ----- helper methods -------------------------------------------------

    @SuppressWarnings("unchecked")
    protected void clear(boolean fTruncate)
        {
        // COH-6009: map mutations must be synchronized with event dispatch
        // to ensure in-order delivery

        if (fTruncate)
            {
            // clear the map
            super.clear();
            }
        else
            {
            // grab a copy of all entries
            AtomicReferenceArray aeBucket = m_aeBucket;

            // clear the map
            super.clear();

            // walk all buckets
            for (int i = 0, c = aeBucket.length(); i < c; ++i)
                {
                // walk all entries in the bucket
                Entry entry = (Entry) aeBucket.get(i);
                while (entry != null)
                    {
                    entry.onRemove();
                    entry = (Entry) entry.m_eNext;
                    }
                }
            }
        }

    // ----- data members ---------------------------------------------------

    /**
    * The MapListenerSupport object.
    */
    protected transient MapListenerSupport m_listenerSupport;
    }
