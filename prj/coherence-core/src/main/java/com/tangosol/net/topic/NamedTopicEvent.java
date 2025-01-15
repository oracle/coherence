/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.topic;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.internal.tracing.Span;
import com.tangosol.internal.tracing.TracingHelper;

import com.tangosol.io.ClassLoaderAware;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Listeners;
import com.tangosol.util.MapListener;


import javax.json.bind.annotation.JsonbProperty;
import java.io.IOException;

import java.util.EventListener;
import java.util.EventObject;

import java.util.Objects;

/**
 * An event related to a {@link NamedTopic}.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class NamedTopicEvent
        extends EventObject
        implements PortableObject
    {
    /**
     * Default constructor for serialization.
     */
    public NamedTopicEvent()
        {
        super(new Object());
        source = null;
        }

    /**
     * Create an event.
     *
     * @param source  the source of the event
     * @param type    the type of the event
     */
    public NamedTopicEvent(NamedTopic<?> source, Type type)
        {
        super(source);
        m_type = Objects.requireNonNull(type);
        }

    // ----- accessors ------------------------------------------------------

    @Override
    public NamedTopic<?> getSource()
        {
        return (NamedTopic<?>) super.getSource();
        }

    /**
      * Returns the type of the event.
      *
      * @return the type of the event
      */
    public Type getType()
        {
        return m_type;
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        String sType = in.readString(0);
        m_type = Type.valueOf(sType);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, m_type.name());
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
        dispatch(listeners, true);
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
                NamedTopicListener target = (NamedTopicListener) targets[i];
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
    public void dispatch(NamedTopicListener listener)
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
    protected boolean shouldDispatch(NamedTopicListener listener)
        {
        return true;
        }

    /**
     * Create a new {@link NamedTopicEvent} the same as this event, but with a different source.
     *
     * @param source  the new event source
     *
     * @return a new {@link NamedTopicEvent} the same as this event, but with a different source
     *
     * @throws NullPointerException if the {@code source} parameter is {@code null}
     */
    public NamedTopicEvent replaceSource(NamedTopic<?> source)
        {
        return new NamedTopicEvent(Objects.requireNonNull(source), m_type);
        }

    // ----- constants ------------------------------------------------------

    public enum Type
        {
        /**
         * The event is a destroyed event.
         */
        Destroyed;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The event's type.
     */
    @JsonbProperty("type")
    protected Type m_type;
    }
