/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.io.ClassLoaderAware;
import com.tangosol.util.Listeners;
import com.tangosol.util.MapListener;

import java.util.EventListener;
import java.util.EventObject;

/**
 * A base event class for topic related events.
 *
 * @param <S>  the type of the event source
 * @param <T>  the event type
 * @param <L>  the event listener type
 *
 * @author Jonathan Knight  2024.11.26
 */
@SuppressWarnings("rawtypes")
public abstract class BaseTopicEvent<S, T, L extends BaseTopicEvent.Listener>
        extends EventObject
    {
    /**
     * Create an event.
     *
     * @param source  the event source
     * @param type    the event type
     */
    public BaseTopicEvent(S source, T type)
        {
        super(source);
        m_type = type;
        }

    // ----- accessors ------------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public S getSource()
        {
        return (S) super.getSource();
        }

    /**
      * Returns the type of the event.
      *
      * @return the type of the event
      */
    public T getType()
        {
        return m_type;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * Return a String representation of this MapEvent object.
     *
     * @return a String representation of this MapEvent object
     */
    public String toString()
        {
        String sEvt  = getClass().getName();
        String sSrc  = getSource().getClass().getName();

        return sEvt.substring(sEvt.lastIndexOf('.') + 1) + '{'
            +  sSrc.substring(sSrc.lastIndexOf('.') + 1)
            +  m_type + '}';
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Dispatch this event to the specified listener collection.
     * <p/>
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
        dispatch(listeners, false);
        }

    /**
     * Dispatch this event to the specified listener collection.
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
    @SuppressWarnings("unchecked")
    public void dispatch(Listeners listeners, boolean fStrict)
        {
        if (listeners != null)
            {
            EventListener[] targets = listeners.listeners();
            for (int i = targets.length; --i >= 0; )
                {
                L target = (L) targets[i];
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
                        Logger.err(e);
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
    @SuppressWarnings("unchecked")
    public void dispatch(L listener)
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
                listener.onEvent(this);
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
    protected boolean shouldDispatch(L listener)
        {
        return true;
        }

    // ----- inner interface: Listener --------------------------------------

    /**
     * A listener that listens to topic events.
     *
     * @param <E>  the type of the event
     */
    public interface Listener<E extends EventObject>
            extends EventListener
        {
        /**
         * Called when an event is dispatched to this listener.
         *
         * @param evt  the event
         */
        void onEvent(E evt);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The event's type.
     */
    protected final T m_type;
    }
