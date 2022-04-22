/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.internal;

import com.tangosol.net.ConfigurableCacheFactory;

import com.tangosol.net.events.NamedEventInterceptor;
import com.tangosol.net.events.application.EventDispatcher;
import com.tangosol.net.events.application.LifecycleEvent.Type;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A {@link EventDispatcher} implementation
 * used by a {@link ConfigurableCacheFactory} implementation
 * to notify registered {@link com.tangosol.net.events.EventInterceptor}s
 * of lifecycle events: ACTIVATED or DISPOSING. This implementation
 * encapsulates the capability of dispatching these events.
 *
 * @author nsa/hr 2012.08.24
 * @since Coherence 12.1.2
 */
public class ConfigurableCacheFactoryDispatcher
        extends AbstractEventDispatcher
        implements EventDispatcher
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a CacheFactoryDispatcher.
     */
    public ConfigurableCacheFactoryDispatcher()
        {
        super(EVENT_TYPES);
        }

    // ----- ConfigurableCacheFactoryDispatcher methods ---------------------

    /**
     * Dispatch the {@link Type#ACTIVATING} event to all concerned
     * {@link com.tangosol.net.events.EventInterceptor EventInterceptors}.
     *
     * @param ccf  the {@link ConfigurableCacheFactory} that raised the event
     */
    public void dispatchActivating(ConfigurableCacheFactory ccf)
        {
        dispatchEvent(LifecycleEvent.Type.ACTIVATING, ccf);
        }

    /**
     * {@inheritDoc}
     */
    public void dispatchActivated(ConfigurableCacheFactory ccf)
        {
        dispatchEvent(LifecycleEvent.Type.ACTIVATED, ccf);
        }

    /**
     * {@inheritDoc}
     */
    public void dispatchDisposing(ConfigurableCacheFactory ccf)
        {
        dispatchEvent(LifecycleEvent.Type.DISPOSING, ccf);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Helper to perform the dispatch of an event being given its type
     *
     * @param eventType  the enum representing the event type
     * @param ccf        the related CCF instance
     */
    protected void dispatchEvent(Type eventType, ConfigurableCacheFactory ccf)
        {
        List<NamedEventInterceptor<?>> list = getInterceptorMap().get(eventType);
        if (list != null)
            {
            new LifecycleEvent(this, eventType, ccf).dispatch(list);
            }
        }

    // ----- inner class: LifecycleEvent ------------------------------------

    /**
     * A LifecycleEvent encapsulates the event caused by a CCF that
     * EventInterceptor may react to and interrogate.
     */
    protected class LifecycleEvent
            extends AbstractEvent<Type>
            implements com.tangosol.net.events.application.LifecycleEvent
        {

        // ---- constructors ------------------------------------------------

        /**
         * Construct a LifecycleEvent.
         *
         * @param dispatcher  the dispatcher that raised this event
         * @param eventType   the event type
         */
        protected LifecycleEvent(EventDispatcher dispatcher, Type eventType, ConfigurableCacheFactory ccf)
            {
            super(dispatcher, eventType);
            m_ccf = ccf;
            }

        // ----- LifecycleEvent methods -------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public ConfigurableCacheFactory getConfigurableCacheFactory()
            {
            return m_ccf;
            }

        /**
         * The ConfigurableCacheFactory associated with this
         * dispatcher.
         */
        protected final ConfigurableCacheFactory m_ccf;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The event types raised by this dispatcher.
     */
    protected static final Set<Enum> EVENT_TYPES = new HashSet<Enum>()
        {{
        add(LifecycleEvent.Type.ACTIVATING);
        add(LifecycleEvent.Type.ACTIVATED);
        add(LifecycleEvent.Type.DISPOSING);
        }};
    }