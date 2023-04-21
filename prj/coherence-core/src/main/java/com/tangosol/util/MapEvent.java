/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import com.tangosol.internal.tracing.Span;
import com.tangosol.internal.tracing.TracingHelper;

import com.tangosol.io.ClassLoaderAware;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;

import java.util.EventListener;
import java.util.EventObject;
import java.util.Map;

import javax.json.bind.annotation.JsonbProperty;

import static com.tangosol.net.cache.ReadWriteBackingMap.BIN_ERASE_PENDING;


/**
* An event which indicates that the content of a map has changed:
* <ul>
* <li>an entry has been added
* <li>an entry has been removed
* <li>an entry has been changed
* </ul>
* A MapEvent object is sent as an argument to the MapListener interface
* methods.  Null values may be provided for the old and the new values.
*
* @param <K>  the type of the Map entry key
* @param <V>  the type of the Map entry value
*
* @author gg  2002.02.11
*/
public class MapEvent<K, V>
        extends EventObject
        implements PortableObject
    {
    /**
     * Default constructor for serialization.
     */
    public MapEvent()
        {
        // We cannot pass a null source to the super class constructor
        // but the source is transient and will not have been serialized
        // so we pass a dummy source to the constructor and then set it
        // to null
        super(new Object());
        source = null;
        }

    /**
    * Constructs a new MapEvent.
    *
    * @param map         the ObservableMap object that fired the event
    * @param nId         this event's id, one of {@link #ENTRY_INSERTED},
    *                    {@link #ENTRY_UPDATED} or {@link #ENTRY_DELETED}
    * @param oKey        the key into the map
    * @param oValueOld   the old value (for update and delete events)
    * @param oValueNew   the new value (for insert and update events)
    */
    public MapEvent(ObservableMap<K, V> map, int nId, K oKey, V oValueOld, V oValueNew)
        {
        super(map);

        // write-behind remove entries have the fixed value BIN_ERASE_PENDING
        // until the erase call is successful
        if (nId == MapEvent.ENTRY_UPDATED && BIN_ERASE_PENDING.equals(oValueNew))
            {
            m_nId = MapEvent.ENTRY_DELETED;
            }
        else
            {
            m_nId = nId;
            }

        m_oKey      = oKey;
        m_oValueOld = oValueOld;
        m_oValueNew = oValueNew;
        }

    /**
    * Return an ObservableMap object on which this event has actually
    * occurred.
    *
    * @return an ObservableMap object
    */
    public ObservableMap getMap()
        {
        // IMPORTANT: The return type here must be raw as we transformers
        // may have changed the key and value types.
        return (ObservableMap) getSource();
        }

    /**
    * Return this event's id. The event id is one of the ENTRY_*
    * enumerated constants.
    *
    * @return an id
    */
    public int getId()
        {
        return m_nId;
        }

    /**
    * Return a key associated with this event.
    *
    * @return a key
    */
    public K getKey()
        {
        return m_oKey;
        }

    /**
    * Return an old value associated with this event.
    * <p>
    * The old value represents a value deleted from or updated in a map.
    * It is always null for "insert" notifications.
    *
    * @return an old value
    */
    public V getOldValue()
        {
        return m_oValueOld;
        }

    /**
    * Return a new value associated with this event.
    * <p>
    * The new value represents a new value inserted into or updated in
    * a map. It is always null for "delete" notifications.
    *
    * @return a new value
    */
    public V getNewValue()
        {
        return m_oValueNew;
        }

    /**
    * Return a Map Entry that represents the state of the Entry before the
    * change occurred that generated this event.
    *
    * @return a Map Entry representing the pre-event state of the Entry
    *
    * @since Coherence 3.6
    */
    public Map.Entry<K, V> getOldEntry()
        {
        return new SimpleMapEntry<K, V>()
            {
            public K getKey()
                {
                return MapEvent.this.getKey();
                }

            public V getValue()
                {
                return MapEvent.this.getOldValue();
                }

            public V setValue(V oValue)
                {
                throw new UnsupportedOperationException();
                }
            };
        }

    /**
    * Return a Map Entry that represents the state of the Entry after the
    * change occurred that generated this event.
    *
    * @return a Map Entry representing the post-event state of the Entry
    *
    * @since Coherence 3.6
    */
    public Map.Entry<K, V> getNewEntry()
        {
        return new SimpleMapEntry<K, V>()
            {
            public K getKey()
                {
                return MapEvent.this.getKey();
                }

            public V getValue()
                {
                return MapEvent.this.getNewValue();
                }

            public V setValue(V oValue)
                {
                throw new UnsupportedOperationException();
                }
            };
        }

    /**
     * Determine whether this event is an insert event.
     *
     * @return  {@code true} if this event is an insert event
     */
    public boolean isInsert()
        {
        return m_nId == ENTRY_INSERTED;
        }

    /**
     * Determine whether this event is an update event.
     *
     * @return  {@code true} if this event is an update event
     */
    public boolean isUpdate()
        {
        return m_nId == ENTRY_UPDATED;
        }

    /**
     * Determine whether this event is a delete event.
     *
     * @return  {@code true} if this event is a delete event
     */
    public boolean isDelete()
        {
        return m_nId == ENTRY_DELETED;
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_nId       = in.readInt(0);
        m_oKey      = in.readObject(1);
        m_oValueOld = in.readObject(2);
        m_oValueNew = in.readObject(3);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeInt(0, m_nId);
        out.writeObject(1, m_oKey);
        out.writeObject(2, m_oValueOld);
        out.writeObject(3, m_oValueNew);
        }

    // ----- Object methods -------------------------------------------------

    /**
    * Return a String representation of this MapEvent object.
    *
    * @return a String representation of this MapEvent object
    */
    public String toString()
        {
        String sEvt = getClass().getName();
        String sSrc = getSource().getClass().getName();

        return sEvt.substring(sEvt.lastIndexOf('.') + 1) + '{'
            +  sSrc.substring(sSrc.lastIndexOf('.') + 1)
            +  getDescription() + '}';
        }


    // ----- helpers ------------------------------------------------------

    /**
    * Dispatch this event to the specified listeners collection.
    * <p>
    * This call is equivalent to
    * <pre>
    *   dispatch(listeners, true);
    * </pre>
    *
    * @param listeners the listeners collection
    *
    * @throws ClassCastException if any of the targets is not
    *         an instance of MapListener interface
    */
    public void dispatch(Listeners listeners)
        {
        dispatch(listeners, true);
        }

    /**
    * Dispatch this event to the specified listeners collection.
    *
    * @param listeners the listeners collection
    * @param fStrict   if true then any RuntimeException thrown by event
    *                  handlers stops all further event processing and the
    *                  exception is re-thrown; if false then all exceptions
    *                  are logged and the process continues
    *
    * @throws ClassCastException if any of the targets is not
    *         an instance of MapListener interface
    */
    public void dispatch(Listeners listeners, boolean fStrict)
        {
        if (listeners != null)
            {
            EventListener[] targets = listeners.listeners();

            Span span = TracingHelper.getActiveSpan();
            if (span != null)
                {
                span.setMetadata(Span.Metadata.LISTENER_CLASSES.key(), listeners.getListenerClassNames());
                }

            for (int i = targets.length; --i >= 0; )
                {
                MapListener target = (MapListener) targets[i];
                try
                    {
                    if (shouldDispatch(target))
                        {
                        dispatch(target);
                        }
                    }
                catch (RuntimeException e)
                    {
                    if (fStrict || Thread.currentThread().isInterrupted())
                        {
                        throw e;
                        }
                    else
                        {
                        Base.err(e);
                        }
                    }
                }
            }
        }

    /**
    * Dispatch this event to the specified MapListener.
    *
    * @param listener  the listener
    */
    public void dispatch(MapListener<? super K, ? super V> listener)
        {
        Thread      thread  = Thread.currentThread();
        ClassLoader loader  = null;
        Object      oSource = getSource();

        // context class loader should be set to that of the source (cache)
        if (oSource instanceof ClassLoaderAware)
            {
            loader = thread.getContextClassLoader();
            thread.setContextClassLoader(
                ((ClassLoaderAware) oSource).getContextClassLoader());
            }

        try
            {
            if (shouldDispatch(listener))
                {
                switch (getId())
                    {
                    case MapEvent.ENTRY_INSERTED:
                        listener.entryInserted((MapEvent) this);
                        break;

                    case MapEvent.ENTRY_UPDATED:
                        listener.entryUpdated((MapEvent) this);
                        break;

                    case MapEvent.ENTRY_DELETED:
                        listener.entryDeleted((MapEvent) this);
                        break;
                    }
                }
            }
        finally
            {
            if (loader != null)
                {
                thread.setContextClassLoader(loader);
                }
            }
        }

    /**
    * Return true if the provided {@link MapListener} should receive this
    * event.
    *
    * @param listener  the MapListener to dispatch this event to
    *
    * @return true if the provided MapListener should receive the event
    */
    protected boolean shouldDispatch(MapListener listener)
        {
        return true;
        }

    /**
    * Get the event's description.
    *
    * @return this event's description
    */
    protected String getDescription()
        {
        switch (getId())
            {
            case ENTRY_INSERTED:
                return " inserted: key=" + getKey()
                     + ", value=" + getNewValue();

            case ENTRY_UPDATED:
                return " updated: key=" + getKey()
                     + ", old value=" + getOldValue()
                     + ", new value=" + getNewValue();

            case ENTRY_DELETED:
                return " deleted: key=" + getKey()
                     + ", value=" + getOldValue();

            default:
                throw new IllegalStateException();
            }
        }

    /**
    * Convert an event ID into a human-readable string.
    *
    * @param nId  an event ID, one of the ENTRY_* enumerated values
    *
    * @return a corresponding human-readable string, for example "inserted"
    */
    public static String getDescription(int nId)
        {
        switch (nId)
            {
            case ENTRY_INSERTED:
                return "inserted";

            case ENTRY_UPDATED:
                return "updated";

            case ENTRY_DELETED:
                return "deleted";

            default:
                return "<unknown: " + nId + '>';
            }
        }


    // ----- constants ------------------------------------------------------

    /**
    * This event indicates that an entry has been added to the map.
    */
    public static final int ENTRY_INSERTED = 1;

    /**
    * This event indicates that an entry has been updated in the map.
    */
    public static final int ENTRY_UPDATED  = 2;

    /**
    * This event indicates that an entry has been removed from the map.
    */
    public static final int ENTRY_DELETED  = 3;


    // ----- data members ---------------------------------------------------

    /**
    * The event's id.
    */
    @JsonbProperty("id")
    protected int    m_nId;

    /**
    * A key.
    */
    @JsonbProperty("key")
    protected K m_oKey;

    /**
    * A previous value.  May be null if not known.
    */
    @JsonbProperty(value = "oldValue", nillable = true)
    protected V m_oValueOld;

    /**
    * A new value.  May be null if not known.
    */
    @JsonbProperty(value = "newValue", nillable = true)
    protected V m_oValueNew;
    }
