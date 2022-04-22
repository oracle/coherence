/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.internal;

import com.tangosol.net.Coherence;
import com.tangosol.net.Session;

import com.tangosol.net.events.CoherenceDispatcher;
import com.tangosol.net.events.CoherenceLifecycleEvent;
import com.tangosol.net.events.NamedEventInterceptor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An implementation of a {@link CoherenceDispatcher} used by
 * a {@link Coherence} instance to dispatch events.
 *
 * @author Jonathan Knight  2020.11.10
 * @since 20.12
 */
@SuppressWarnings("rawtypes")
public class CoherenceEventDispatcher
        extends AbstractEventDispatcher
        implements CoherenceDispatcher

    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates a {@link CoherenceEventDispatcher} for a specific
     * {@link Coherence} instance.
     *
     * @param coherence  the {@link Coherence} instance this dispatcher
     *                   dispatches events for
     */
    public CoherenceEventDispatcher(Coherence coherence)
        {
        super(EVENT_TYPES);
        f_coherence = coherence;
        }

    // ----- CoherenceDispatcher methods ------------------------------------

    @Override
    public String getName()
        {
        return f_coherence.getName();
        }

    // ----- CoherenceEventDispatcher methods -------------------------------

    public void dispatchStarting()
        {
        dispatchEvent(CoherenceLifecycleEvent.Type.STARTING);
        }

    public void dispatchStarted()
        {
        dispatchEvent(CoherenceLifecycleEvent.Type.STARTED);
        }

    public void dispatchStopping()
        {
        dispatchEvent(CoherenceLifecycleEvent.Type.STOPPING);
        }

    public void dispatchStopped()
        {
        dispatchEvent(CoherenceLifecycleEvent.Type.STOPPED);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Helper to perform the dispatch of a {@link CoherenceLifecycleEvent}
     * being given its type
     *
     * @param eventType  the enum representing the event type
     */
    protected void dispatchEvent(CoherenceLifecycleEvent.Type eventType)
        {
        List<NamedEventInterceptor<?>> list = getInterceptorMap().get(eventType);
        if (list != null)
            {
            new LifecycleEvent(this, eventType, f_coherence).dispatch(list);
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
        public AbstractEvent(CoherenceEventDispatcher dispatcher, T eventType)
            {
            super(dispatcher, eventType);
            }

        // ----- Event interface --------------------------------------------

        public String getName()
            {
            return ((CoherenceEventDispatcher) m_dispatcher).getName();
            }

        @Override
        public CoherenceDispatcher getDispatcher()
            {
            return (CoherenceDispatcher) m_dispatcher;
            }
        }

    // ----- inner class: LifecycleEvent ------------------------------------

    /**
     * A {@link CoherenceLifecycleEvent} implementation raised by this dispatcher.
     */
    protected static class LifecycleEvent
            extends AbstractEvent<CoherenceLifecycleEvent.Type>
            implements CoherenceLifecycleEvent
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a cache truncate event.
         *
         * @param dispatcher  the dispatcher that raised this event
         * @param eventType   the event type
         * @param coherence   the {@link Coherence} instance related to the event
         */
        protected LifecycleEvent(CoherenceEventDispatcher dispatcher, Type eventType, Coherence coherence)
            {
            super(dispatcher, eventType);
            f_coherence = coherence;
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
                   ", Coherence=" + getName();
            }

        @Override
        public Coherence getCoherence()
            {
            return f_coherence;
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link Coherence} instance that the event is associated with.
         */
        private final Coherence f_coherence;
        }

    // ----- constants and data members -------------------------------------

    /**
     * The event types raised by this dispatcher.
     */
    protected static final Set<Enum> EVENT_TYPES = new HashSet<>();

    /**
     * The {@link Session}.
     */
    protected final Coherence f_coherence;

    // ----- static initializer ---------------------------------------------

    static
        {
        EVENT_TYPES.addAll(Arrays.asList(CoherenceLifecycleEvent.Type.values()));
        }
    }
