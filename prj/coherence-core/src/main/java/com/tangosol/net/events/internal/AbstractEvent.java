/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.internal;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.tracing.Scope;
import com.tangosol.internal.tracing.Span;
import com.tangosol.internal.tracing.TracingHelper;

import com.tangosol.net.events.Event;
import com.tangosol.net.events.EventDispatcher;
import com.tangosol.net.events.EventInterceptor;

import com.tangosol.net.events.internal.AbstractEventDispatcher.EventStats;

import com.tangosol.util.Base;

import java.util.Collection;
import java.util.Iterator;

/**
 * Abstract base implementation of {@link Event}.
 *
 * @author mwj, nsa, rhl  2011.03.29
 * @since Coherence 12.1.2
 *
 * @param <T>  the type of event
 */
public abstract class AbstractEvent<T extends Enum<T>>
        implements Event<T>
    {

    // ----- constructor ----------------------------------------------------

    /**
     * Construct a new {@link Event} using the specified type.
     *
     * @param eventType  the type of {@link Event} raised
     */
    public AbstractEvent(T eventType)
        {
        this(null, eventType);
        }

    /**
     * Construct a new {@link Event} using the specified type.
     *
     * @param dispatcher  the event dispatcher that raised this event
     * @param eventType   the type of {@link Event} raised
     */
    public AbstractEvent(EventDispatcher dispatcher, T eventType)
        {
        m_dispatcher = dispatcher;
        m_eventType  = eventType;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the iterator of remaining interceptors to dispatch to.
     *
     * @return the iterator of interceptors
     */
    protected Iterator<? extends EventInterceptor<?>> getIterator()
        {
        return m_iterInterceptor;
        }

    /**
     * Set the iterator of interceptors to dispatch to.
     *
     * @param iter  the iterator of interceptors
     */
    protected void setIterator(Iterator<? extends EventInterceptor<?>> iter)
        {
        m_iterInterceptor = iter;
        }

    // ----- Event interface ------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public T getType()
        {
        return m_eventType;
        }

    /**
     * {@inheritDoc}
     */
    public EventDispatcher getDispatcher()
        {
        return m_dispatcher;
        }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void nextInterceptor()
        {
        EventDispatcher dispatcher = m_dispatcher;
        EventStats      stats      = null;
        boolean         fTracing   = TracingHelper.isEnabled();

        if (dispatcher instanceof AbstractEventDispatcher)
            {
            stats = ((AbstractEventDispatcher) dispatcher).getStats();
            }

        // EventInterceptors receiving this event can optionally call
        // nextInterceptor (recursively) to explicitly control dispatch to the
        // following interceptor(s), and subsequently observe any side-effects.
        // If the interceptor does not recurse, we continue the dispatch chain
        // iteratively.
        for (Iterator<? extends EventInterceptor<?>> iter = getIterator(); iter.hasNext(); )
            {
            EventInterceptor interceptor = iter.next();
            Span             span        = null;
            Scope            scope       = null;

            if (fTracing)
                {
                span  = TracingHelper.newSpan("process", this)
                        .withMetadata("interceptor", interceptor.getClass().getName())
                        .startSpan();
                scope = TracingHelper.getTracer().withSpan(span);
                }

            try
                {
                // dispatch the event
                interceptor.onEvent(this);
                }
            catch (Exception e)
                {
                TracingHelper.augmentSpanWithErrorDetails(span, true, e);
                if (stats != null)
                    {
                    stats.registerEventException(e, this, interceptor);
                    }

                if (isVetoable())
                    {
                    String sMsg = "Exception vetoed by \"" + interceptor + "\".";
                    if (fTracing)
                        {
                        span.log(sMsg);
                        }
                    // Drain the iterator so no one else can re-start processing
                    while (iter.hasNext())
                        {
                        iter.next();
                        }
                    throw Base.ensureRuntimeException(e, sMsg);
                    }
                else
                    {
                    Logger.err("Exception caught while dispatching to \"" + interceptor + "\": ", e);
                    }
                }
            finally
                {
                if (fTracing)
                    {
                    scope.close();
                    span.end();
                    }
                }
            }
        }

    // ----- AbstractEvent methods ------------------------------------------

    /**
     * Dispatch this event over the specified interceptors.
     *
     * @param colIter  the interceptors to trigger for this event
     */
    protected void dispatch(Collection<? extends EventInterceptor<?>> colIter)
        {
        setIterator(colIter.iterator());
        nextInterceptor();
        }

    /**
     * Return true if this represents a mutable operation.
     *
     * @return true if this event represents a mutable operation
     */
    protected boolean isMutableEvent()
        {
        return false;
        }

    /**
     * Return true iff an exception raised by an {@link EventInterceptor} during
     * the processing of this event should interrupt the {@link
     * EventInterceptor} chaining of the {@link Event} and terminate the
     * operation.
     *
     * @return true iff an exception raised during processing of this event
     *         should terminate the operation
     */
    protected boolean isVetoable()
        {
        // by default, all mutable events are terminal
        return isMutableEvent();
        }

    /**
    * Returns this Event's description.
    *
    * @return  this Event's description
    */
    protected String getDescription()
        {
        return "Type=" + getType();
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String toString()
        {
        String sEvt = getClass().getName();
        return sEvt.substring(sEvt.lastIndexOf('.') + 1) + '{' + getDescription() + '}';
        }

    // ----- data members ---------------------------------------------------

    /**
     * The event type being raised.
     */
    protected final T m_eventType;

    /**
     * The iterator used by nextInterceptor to iterate through a chain of interceptors.
     */
    protected Iterator<? extends EventInterceptor<?>> m_iterInterceptor;

    /**
     * Event dispatcher that raised this event.
     */
    protected final EventDispatcher m_dispatcher;
    }
