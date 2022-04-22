/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.internal;

import com.tangosol.net.Session;

import com.tangosol.net.events.NamedEventInterceptor;
import com.tangosol.net.events.SessionDispatcher;
import com.tangosol.net.events.SessionLifecycleEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An implementation of a {@link SessionDispatcher} used by
 * a {@link Session} instance to dispatch events.
 *
 * @author Jonathan Knight  2020.11.10
 * @since 20.12
 */
@SuppressWarnings("rawtypes")
public class SessionEventDispatcher
        extends AbstractEventDispatcher
        implements SessionDispatcher

    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates a {@link SessionEventDispatcher} for a specific
     * {@link Session} instance.
     *
     * @param session  the {@link Session} instance this dispatcher
     *                   dispatches events for
     */
    public SessionEventDispatcher(Session session)
        {
        super(EVENT_TYPES);
        f_session = session;
        }

    // ----- SessionDispatcher methods --------------------------------------

    @Override
    public String getName()
        {
        return f_session.getName();
        }

    // ----- SessionEventDispatcher methods -------------------------------

    public void dispatchStarting()
        {
        dispatchEvent(SessionLifecycleEvent.Type.STARTING);
        }

    public void dispatchStarted()
        {
        dispatchEvent(SessionLifecycleEvent.Type.STARTED);
        }

    public void dispatchStopping()
        {
        dispatchEvent(SessionLifecycleEvent.Type.STOPPING);
        }

    public void dispatchStopped()
        {
        dispatchEvent(SessionLifecycleEvent.Type.STOPPED);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Helper to perform the dispatch of a {@link SessionLifecycleEvent}
     * being given its type
     *
     * @param eventType  the enum representing the event type
     */
    protected void dispatchEvent(SessionLifecycleEvent.Type eventType)
        {
        List<NamedEventInterceptor<?>> list = getInterceptorMap().get(eventType);
        if (list != null)
            {
            new LifecycleEvent(this, eventType, f_session).dispatch(list);
            }
        }

    // ----- inner class: AbstractEvent -------------------------------------

    /**
     * A {@link com.tangosol.net.events.Event} implementation providing
     * access to the dispatcher.
     */
    protected abstract static class AbstractEvent<T extends Enum<T>>
            extends com.tangosol.net.events.internal.AbstractEvent<T>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct an AbstractEvent with the provided dispatcher
         * and event type.
         *
         * @param dispatcher  the dispatcher that raised this event
         * @param eventType   the event type
         */
        public AbstractEvent(SessionEventDispatcher dispatcher, T eventType)
            {
            super(dispatcher, eventType);
            }

        // ----- Event interface --------------------------------------------

        public String getName()
            {
            return ((SessionEventDispatcher) m_dispatcher).getName();
            }

        @Override
        public SessionDispatcher getDispatcher()
            {
            return (SessionDispatcher) m_dispatcher;
            }
        }

    // ----- inner class: LifecycleEvent ------------------------------------

    /**
     * A {@link SessionLifecycleEvent} implementation raised by this dispatcher.
     */
    protected static class LifecycleEvent
            extends AbstractEvent<SessionLifecycleEvent.Type>
            implements SessionLifecycleEvent
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a cache truncate event.
         *
         * @param dispatcher  the dispatcher that raised this event
         * @param eventType   the event type
         * @param coherence   the {@link Session} instance related to the event
         */
        protected LifecycleEvent(SessionEventDispatcher dispatcher, Type eventType, Session coherence)
            {
            super(dispatcher, eventType);
            f_session = coherence;
            }

        // ----- AbstractEvent methods --------------------------------------

        @Override
        protected boolean isMutableEvent()
            {
            return false;
            }

        @Override
        protected String getDescription()
            {
            return super.getDescription() +
                   ", Session=" + getName();
            }

        @Override
        public Session getSession()
            {
            return f_session;
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link Session} instance that the event is associated with.
         */
        private final Session f_session;
        }

    // ----- constants and data members -------------------------------------

    /**
     * The event types raised by this dispatcher.
     */
    protected static final Set<Enum> EVENT_TYPES = new HashSet<>();

    /**
     * The {@link Session}.
     */
    protected final Session f_session;

    // ----- static initializer ---------------------------------------------

    static
        {
        EVENT_TYPES.addAll(Arrays.asList(SessionLifecycleEvent.Type.values()));
        }
    }
