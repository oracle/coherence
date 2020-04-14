/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.io.BinaryStore;

import com.tangosol.util.Filter;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.ObservableMap;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
* An abstract base class for serialization-based caches.
*
* @author cp  2005.12.12
*/
public abstract class AbstractSerializationCache
        extends SerializationMap
        implements ObservableMap
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a serialization cache on top of a BinaryStore.
    *
    * @param store  the BinaryStore to use to write the serialized objects to
    */
    public AbstractSerializationCache(BinaryStore store)
        {
        super(store);
        }

    /**
    * Construct a serialization cache on top of a BinaryStore, using the
    * passed ClassLoader for deserialization.
    *
    * @param store  the BinaryStore to use to write the serialized objects to
    * @param loader the ClassLoader to use for deserialization
    */
    public AbstractSerializationCache(BinaryStore store, ClassLoader loader)
        {
        super(store, loader);
        }

    /**
    * Construct a serialization cache on top of a BinaryStore, optionally
    * storing only Binary keys and values.
    *
    * @param store       the BinaryStore to use to write the serialized
    *                    objects to
    * @param fBinaryMap  true indicates that this map will only manage
    *                    binary keys and values
    */
    public AbstractSerializationCache(BinaryStore store, boolean fBinaryMap)
        {
        super(store, fBinaryMap);
        }


    // ----- Map interface --------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public synchronized void clear()
        {
        if (hasListeners())
            {
            for (Iterator iter = getInternalKeySet().iterator(); iter.hasNext(); )
                {
                // unlike some CacheEvent cases, this event gets fired BEFORE
                // the entry is removed; moreover, deferring the event
                // processing (e.g. processing it on a different thread)
                // may yield the OldValue inaccessible or plain invalid
                dispatchPendingEvent(iter.next(), MapEvent.ENTRY_DELETED, null, false);
                }
            }

        super.clear();
        }

    /**
    * {@inheritDoc}
    */
    public Object put(Object oKey, Object oValue)
        {
        boolean fListeners = hasListeners();
        boolean fContains  = fListeners && getInternalKeySet().contains(oKey);

        Object oOrig = super.put(oKey, oValue);

        if (fListeners)
            {
            int nEvent = fContains ? MapEvent.ENTRY_UPDATED
                                   : MapEvent.ENTRY_INSERTED;
            dispatchEvent(new CacheEvent(this, nEvent, oKey, oOrig, oValue, false));
            }

        return oOrig;
        }

    /**
    * {@inheritDoc}
    */
    public void putAll(Map map)
        {
        if (hasListeners())
            {
            Set setKeys = getInternalKeySet();
            for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
                {
                Map.Entry entry  = (Map.Entry) iter.next();
                Object    oKey   = entry.getKey();
                int       nEvent = setKeys.contains(oKey)
                                   ? MapEvent.ENTRY_UPDATED
                                   : MapEvent.ENTRY_INSERTED;
                dispatchPendingEvent(oKey, nEvent, entry.getValue(), false);
                }
            }

        super.putAll(map);
        }

    /**
    * {@inheritDoc}
    */
    public Object remove(Object oKey)
        {
        Object oOrig = null;
        if (getInternalKeySet().contains(oKey))
            {
            oOrig = super.remove(oKey);

            if (hasListeners())
                {
                dispatchEvent(new CacheEvent(this, MapEvent.ENTRY_DELETED,
                                             oKey, oOrig, null, false));
                }
            }

        return oOrig;
        }


    // ----- AbstractKeySetBasedMap methods ---------------------------------

    /**
    * {@inheritDoc}
    */
    protected boolean removeBlind(Object oKey)
        {
        boolean fRemoved = false;
        if (getInternalKeySet().contains(oKey))
            {
            if (hasListeners())
                {
                dispatchPendingEvent(oKey, MapEvent.ENTRY_DELETED, null, false);
                }

            super.removeBlind(oKey);
            fRemoved = true;
            }
        return fRemoved;
        }


    // ----- ObservableMap methods ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public synchronized void addMapListener(MapListener listener)
        {
        addMapListener(listener, (Filter) null, false);
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void removeMapListener(MapListener listener)
        {
        removeMapListener(listener, (Filter) null);
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void addMapListener(MapListener listener, Object oKey, boolean fLite)
        {
        azzert(listener != null);

        MapListenerSupport support = m_listenerSupport;
        if (support == null)
            {
            support = m_listenerSupport = new MapListenerSupport();
            }

        support.addListener(listener, oKey, fLite);
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void removeMapListener(MapListener listener, Object oKey)
        {
        azzert(listener != null);

        MapListenerSupport support = m_listenerSupport;
        if (support != null)
            {
            support.removeListener(listener, oKey);
            if (support.isEmpty())
                {
                m_listenerSupport = null;
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void addMapListener(MapListener listener, Filter filter, boolean fLite)
        {
        azzert(listener != null);

        MapListenerSupport support = m_listenerSupport;
        if (support == null)
            {
            support = m_listenerSupport = new MapListenerSupport();
            }

        support.addListener(listener, filter, fLite);
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void removeMapListener(MapListener listener, Filter filter)
        {
        azzert(listener != null);

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


    // ----- AbstractSerializationCache -------------------------------------

    /**
    * Flush items that have expired.
    *
    * @since Coherence 3.2
    */
    public abstract void evict();


    // ----- accessors ------------------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected String getDescription()
        {
        return super.getDescription()
               + ", hasListeners=" + hasListeners();
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
    * Determine if this map has any listeners at all.
    *
    * @return true iff this ObservableMap has at least one MapListener
    */
    protected boolean hasListeners()
        {
        // m_listenerSupport defaults to null, and it is reset to null when
        // the last listener unregisters
        return m_listenerSupport != null;
        }

    /**
    * Dispatch an event that has not yet occurred, allowing the cache to
    * potentially avoid reading of the "original value" information.
    *
    * @param oKey        the key which the event is related to
    * @param nId         the event ID
    * @param oNewValue   the new value
    * @param fSynthetic  true if the event is synthetic
    */
    protected void dispatchPendingEvent(final Object oKey, int nId, Object oNewValue, boolean fSynthetic)
        {
        DeferredCacheEvent event = new DeferredCacheEvent(
                this, nId, oKey, null, oNewValue, fSynthetic)
            {
            @Override
            protected Object readOldValue()
                {
                return AbstractSerializationCache.this.get(oKey);
                }
            };

        try
            {
            dispatchEvent(event);
            }
        finally
            {
            // the contract between the DeferredCacheEvent and consumers of
            // the event states consumers must call getOldValue prior to
            // returning from fireEvent; it is possible before we deactivate
            // the DCE another thread calls DCE.getOldValue, as PartitionedCache
            // will place the event on the fabric, which would result in
            // performing an unnecessary load, however will not exhibit
            // correctness side affects
            event.deactivate();
            }
        }

    /**
    * Dispatch the passed event.
    *
    * @param evt   a CacheEvent object
    */
    protected void dispatchEvent(CacheEvent evt)
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


    // ----- data fields ----------------------------------------------------

    /**
    * The MapListenerSupport object.
    */
    private MapListenerSupport m_listenerSupport;
    }
