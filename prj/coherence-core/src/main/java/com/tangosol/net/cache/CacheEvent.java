/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.ObservableMap;


/**
* An extension of the MapEvent which allows to differentiate between client
* driven (natural) events and cache internal (synthetic) events.
* <p>
* Consider a client code calling a remove() method for a cache. Quite
* naturally it causes a corresponding ENTRY_DELETED event. However, the same
* event could be as well caused by the client code calling put() forcing an
* entry eviction.  Alternatively, the put() method called by a client code
* naturally causes either ENTRY_INSERTED or ENTRY_UPDATED event. However, the
* same event could be as well caused by a client call to a get() method that
* in turn forces an entry insertion by a cache loader.
*
* @author gg  2003.09.12
* @since Coherence 2.3
*/
public class CacheEvent<K, V>
        extends MapEvent<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Constructs a new CacheEvent.
    *
    * @param map         the ObservableMap object that fired the event
    * @param nId         this event's id, one of {@link #ENTRY_INSERTED},
    *                    {@link #ENTRY_UPDATED} or {@link #ENTRY_DELETED}
    * @param oKey        the key into the map
    * @param oValueOld   the old value (for update and delete events)
    * @param oValueNew   the new value (for insert and update events)
    * @param fSynthetic  true iff the event is caused by the cache
    *                    internal processing such as eviction or loading
    */
    public CacheEvent(ObservableMap<K, V> map, int nId, K oKey,
                      V oValueOld, V oValueNew, boolean fSynthetic)
        {
        this(map, nId, oKey, oValueOld, oValueNew, fSynthetic, TransformationState.TRANSFORMABLE);
        }

    /**
    * Constructs a new CacheEvent.
    *
    * @param map         the ObservableMap object that fired the event
    * @param nId         this event's id, one of {@link #ENTRY_INSERTED},
    *                    {@link #ENTRY_UPDATED} or {@link #ENTRY_DELETED}
    * @param oKey        the key into the map
    * @param oValueOld   the old value (for update and delete events)
    * @param oValueNew   the new value (for insert and update events)
    * @param fSynthetic  true iff the event is caused by the cache
    *                    internal processing such as eviction or loading
    * @param fPriming    a flag indicating whether or not the event is a priming event
    */
    public CacheEvent(ObservableMap<K, V> map, int nId, K oKey,
                      V oValueOld, V oValueNew, boolean fSynthetic, boolean fPriming)
        {
        this(map, nId, oKey, oValueOld, oValueNew, fSynthetic, TransformationState.TRANSFORMABLE, fPriming);
        }


    /**
    * Constructs a new CacheEvent.
    *
    * @param map             the ObservableMap object that fired the event
    * @param nId             this event's id, one of {@link #ENTRY_INSERTED},
    *                        {@link #ENTRY_UPDATED} or {@link #ENTRY_DELETED}
    * @param oKey            the key into the map
    * @param oValueOld       the old value (for update and delete events)
    * @param oValueNew       the new value (for insert and update events)
    * @param fSynthetic      true iff the event is caused by the cache
    *                        internal processing such as eviction or loading
    * @param transformState  the {@link TransformationState state} describing how
    *                        this event has been or should be transformed
    */
    public CacheEvent(ObservableMap<K, V> map, int nId, K oKey,
                      V oValueOld, V oValueNew,
                      boolean fSynthetic, TransformationState transformState)
        {
        this(map, nId, oKey, oValueOld, oValueNew, fSynthetic, transformState, false);
        }

    /**
    * Constructs a new CacheEvent.
    *
    * @param map             the ObservableMap object that fired the event
    * @param nId             this event's id, one of {@link #ENTRY_INSERTED},
    *                        {@link #ENTRY_UPDATED} or {@link #ENTRY_DELETED}
    * @param oKey            the key into the map
    * @param oValueOld       the old value (for update and delete events)
    * @param oValueNew       the new value (for insert and update events)
    * @param fSynthetic      true iff the event is caused by the cache
    *                        internal processing such as eviction or loading
    * @param transformState  the {@link TransformationState state} describing how
    *                        this event has been or should be transformed
    * @param fPriming        a flag indicating whether or not the event is a priming event
    */
    public CacheEvent(ObservableMap<K, V> map, int nId, K oKey,
                      V oValueOld, V oValueNew,
                      boolean fSynthetic, TransformationState transformState,
                      boolean fPriming)
        {
        super(map, nId, oKey, oValueOld, oValueNew);

        m_fSynthetic     = fSynthetic;
        m_fPriming       = fPriming;
        m_transformState = transformState;
        }


    // ----- MapEvent methods -----------------------------------------------


    @Override
    public CacheEvent<K, V> with(int nPartition, long lVersion)
        {
        return (CacheEvent) super.with(nPartition, lVersion);
        }

    @Override
    protected boolean shouldDispatch(MapListener listener)
        {
        return super.shouldDispatch(listener) &&
                (!isPriming() || 
                 MapListenerSupport.isPrimingListener(listener) ||
                 listener.isVersionAware());
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return true iff this event is caused by the cache internal processing
    * such as eviction or loading
    *
    * @return true iff this event is caused by the cache internal processing
    */
    public boolean isSynthetic()
        {
        return m_fSynthetic;
        }

    /**
    * Return true iff this event is caused by a priming listener registration.
    *
    * @return true iff this event is caused by a priming listener registration
    */
    public boolean isPriming()
        {
        return m_fPriming;
        }

    /**
    * Return true iff this event is
    * {@link com.tangosol.util.MapEventTransformer#transform transformable}.
    * Non-transformable events will not be delivered to MapEventTransformer
    * listeners.
    *
    * @return true iff this event is transformable
    */
    public TransformationState getTransformationState()
        {
        return m_transformState;
        }

    /**
    * Get the event's description.
    *
    * @return this event's description
    */
    protected String getDescription()
        {
        String sDescr = super.getDescription();
        return (isSynthetic() ? sDescr + ", synthetic" : sDescr) +
               (isPriming()   ? ", priming" : "");
        }


    // ----- TransformationState enum ---------------------------------------

    /**
    * TransformationState describes how a CacheEvent has been or should be
    * {@link com.tangosol.util.MapEventTransformer#transform transformed}.
    */
    public enum TransformationState
        {
        /**
        * Value used to indicate that an event is non-transformable and should
        * not be passed to any transformer-based listeners.
        */
        NON_TRANSFORMABLE,

        /**
        * Value used to indicate that an event is transformable and could be
        * passed to transformer-based listeners.
        */
        TRANSFORMABLE,

        /**
        * Value used to indicate that an event has been transformed, and should
        * only be passed to transformer-based listeners.
        */
        TRANSFORMED,
        }


    // ----- data members ---------------------------------------------------

    /**
    * Flag indicating whether or not the event is synthetic.
    */
    protected boolean m_fSynthetic;

    /**
    * Flag indicating whether or not the event is a priming event (NearCache).
    */
    protected boolean m_fPriming;

    /**
    * The transformation state for this event.
    */
    protected TransformationState m_transformState = TransformationState.TRANSFORMABLE;
    }
