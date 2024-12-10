/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.listener;

import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.MultiplexingMapListener;

import java.util.function.Consumer;

/**
 * An implementation of a {@link MapListener} that delegates to the appropriate
 * {@link Consumer event handler} based on the event type.
 *
 * @author as  2015.02.12
 * @since 12.2.1
 */
public class SimpleMapListener<K, V>
        implements MapListener<K, V>
    {
    // ---- event handler registration methods ------------------------------

    /**
     * Add the event handler for INSERT events.
     *
     * @param onInsert  the event handler to add
     *
     * @return  this MapListener
     */
    public SimpleMapListener<K, V> addInsertHandler(Consumer<? super MapEvent<? extends K, ? extends V>> onInsert)
        {
        m_onInsert = addHandler(m_onInsert,
                                (Consumer<MapEvent<? extends K, ? extends V>>) onInsert);
        return this;
        }

    /**
     * Add the event handler for UPDATE events.
     *
     * @param onUpdate  the event handler to execute
     *
     * @return  this MapListener
     */
    public SimpleMapListener<K, V> addUpdateHandler(Consumer<? super MapEvent<? extends K, ? extends V>> onUpdate)
        {
        m_onUpdate = addHandler(m_onUpdate,
                                (Consumer<MapEvent<? extends K, ? extends V>>) onUpdate);
        return this;
        }

    /**
     * Add the event handler for DELETE events.
     *
     * @param onDelete  the event handler to execute
     *
     * @return  this MapListener
     */
    public SimpleMapListener<K, V> addDeleteHandler(Consumer<? super MapEvent<? extends K, ? extends V>> onDelete)
        {
        m_onDelete = addHandler(m_onDelete,
                                (Consumer<MapEvent<? extends K, ? extends V>>) onDelete);
        return this;
        }

    /**
     * Add the event handler for all events.
     *
     * @param onEvent  the event handler to execute
     *
     * @return  this MapListener
     *
     * @see MultiplexingMapListener
     */
    public SimpleMapListener<K, V> addEventHandler(Consumer<? super MapEvent<? extends K, ? extends V>> onEvent)
        {
        return addInsertHandler(onEvent)
               .addUpdateHandler(onEvent)
               .addDeleteHandler(onEvent);
        }

    /**
     * Mark this listener as versioned as can be confirmed by {@link
     * MapListener#isVersionAware()}.
     *
     * @return this MapListener
     */
    public SimpleMapListener<K, V> versioned()
        {
        m_nCharacteristics |= VERSION_AWARE;

        return this;
        }

    /**
     * Mark this listener as synchronous as can be confirmed by {@link
     * MapListener#isSynchronous()} ()}.
     *
     * @return this MapListener
     */
    public SimpleMapListener<K, V> synchronous()
        {
        m_nCharacteristics |= SYNCHRONOUS;

        return this;
        }

    /**
     * Mark this listener as asynchronous as can be confirmed by {@link
     * MapListener#isAsynchronous()}.
     *
     * @return this MapListener
     */
    public SimpleMapListener<K, V> asynchronous()
        {
        m_nCharacteristics &= ~SYNCHRONOUS;

        return this;
        }

    // ---- MapListener interface -------------------------------------------

    @Override
    public void entryInserted(MapEvent<K, V> evt)
        {
        if (m_onInsert != null)
            {
            m_onInsert.accept(evt);
            }
        }

    @Override
    public void entryUpdated(MapEvent<K, V> evt)
        {
        if (m_onUpdate != null)
            {
            m_onUpdate.accept(evt);
            }
        }

    @Override
    public void entryDeleted(MapEvent<K, V> evt)
        {
        if (m_onDelete != null)
            {
            m_onDelete.accept(evt);
            }
        }

    @Override
    public int characteristics()
        {
        return m_nCharacteristics;
        }

    @Override
    public boolean equals(Object oThat)
        {
        return oThat != null &&
                super.equals(MapListenerSupport.unwrap((MapListener) oThat));
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Add a handler to a handler chain.
     *
     * @param handlerChain  the existing handler chain (could be null)
     * @param handler       the handler to add
     *
     * @return new handler chain
     */
    protected Consumer<MapEvent<? extends K, ? extends V>> addHandler(Consumer<MapEvent<? extends K, ? extends V>> handlerChain,
                                                                      Consumer<MapEvent<? extends K, ? extends V>> handler)
        {
        return handlerChain == null
               ? handler
               : handlerChain.andThen(handler);
        }

    // ---- data members ----------------------------------------------------

    /**
     * The event handler to execute on INSERT event.
     */
    protected Consumer<MapEvent<? extends K, ? extends V>> m_onInsert;

    /**
     * The event handler to execute on UPDATE event.
     */
    protected Consumer<MapEvent<? extends K, ? extends V>> m_onUpdate;

    /**
     * The event handler to execute on DELETE event.
     */
    protected Consumer<MapEvent<? extends K, ? extends V>> m_onDelete;

    /**
     * The characteristics of this MapListener.
     */
    protected int m_nCharacteristics = ASYNCHRONOUS;
    }
