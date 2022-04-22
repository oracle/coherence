/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.internal;

import com.oracle.coherence.common.base.Disposable;

import com.tangosol.net.events.Event;
import com.tangosol.net.events.EventDispatcher;
import com.tangosol.net.events.EventDispatcherRegistry;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.net.events.NamedEventInterceptor;

import com.tangosol.net.security.LocalPermission;

import com.tangosol.util.Base;
import com.tangosol.util.RegistrationBehavior;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of both {@link EventDispatcherRegistry} &
 * {@link InterceptorRegistry}.
 *
 * @author mwj, rhan, nsa, rhl, hr 2011.03.29
 * @since Coherence 12.1.2
 */
public class Registry
        implements InterceptorRegistry, EventDispatcherRegistry, Disposable
    {

    // ------ constructors --------------------------------------------------

    /**
     * Default constructor.
     */
    public Registry()
        {
        m_mapInterceptors = new LinkedHashMap<>(); // Preserve the interceptor order
        m_setDispatchers  = Collections.newSetFromMap(new ConcurrentHashMap<EventDispatcher,Boolean>());
        }

    // ----- InterceptorRegistry interface ----------------------------------

    /**
     * {@inheritDoc}
     */
    public synchronized String registerEventInterceptor(EventInterceptor<?> interceptor)
        {
        return registerEventInterceptorInternal(null, interceptor, null);
        }

    /**
     * {@inheritDoc}
     */
    public synchronized String registerEventInterceptor(EventInterceptor<?> interceptor, RegistrationBehavior behavior)
        {
        return registerEventInterceptorInternal(null, interceptor, behavior);
        }

    /**
     * {@inheritDoc}
     */
    public synchronized String registerEventInterceptor(String sIdentifier, EventInterceptor<?> interceptor, RegistrationBehavior behavior)
        {
        return registerEventInterceptorInternal(sIdentifier, interceptor, behavior);
        }

    /**
     * {@inheritDoc}
     */
    public synchronized void unregisterEventInterceptor(String sIdentifier)
        {
        SecurityManager security = System.getSecurityManager();
        if (security != null)
            {
            security.checkPermission(
                new LocalPermission("Service.registerEventInterceptor"));
            }

        EventInterceptor interceptor = m_mapInterceptors.remove(sIdentifier);
        if (interceptor != null)
            {
            removeInterceptorFromDispatchers(sIdentifier);
            }
        }

    /**
     * {@inheritDoc}
     */
    public synchronized EventInterceptor<?> getEventInterceptor(String sIdentifier)
        {
        NamedEventInterceptor incptr = m_mapInterceptors.get(sIdentifier);

        return incptr == null ? null : incptr.getInterceptor();
        }

    // ----- Disposable methods ---------------------------------------------

    /**
     * {@inheritDoc}
     */
    public synchronized void dispose()
        {
        m_setDispatchers.clear();
        m_mapInterceptors.clear();
        }


    // ----- EventDispatcherRegistry interface ------------------------------

    /**
     * {@inheritDoc}
     */
    public synchronized void registerEventDispatcher(EventDispatcher dispatcher)
        {
        m_setDispatchers.add(dispatcher);

        introduceDispatcher(dispatcher);
        }

    /**
     * {@inheritDoc}
     */
    public synchronized void unregisterEventDispatcher(EventDispatcher dispatcher)
        {
        m_setDispatchers.remove(dispatcher);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Register the provided {@link EventInterceptor} with the identifier.
     *
     * @param sIdentifier  a unique identifier for the interceptor which upon
     *                     null will cause a default identifier to be generated
     * @param interceptor  the EventInterceptor to register
     * @param behavior     the behavior enacted upon discovering duplicate
     *                     interceptors
     *
     * @param <T>  the event that the interceptor accepts
     *
     * @return the identifier used to register the interceptor
     */
    protected <T extends Event<? extends Enum>> String registerEventInterceptorInternal(
            String sIdentifier, EventInterceptor<T> interceptor, RegistrationBehavior behavior)
        {
        NamedEventInterceptor<T> incptrNamed;

        if (interceptor instanceof NamedEventInterceptor)
            {
            incptrNamed = (NamedEventInterceptor<T>) interceptor;
            if (sIdentifier != null && !incptrNamed.getRegisteredName().equals(sIdentifier))
                {
                // clone the NamedEventInterceptor with the correct identifier
                incptrNamed = new NamedEventInterceptor<>(sIdentifier, incptrNamed.getInterceptor(),
                        incptrNamed.getCacheName(), incptrNamed.getServiceName(), incptrNamed.getOrder(),
                        behavior == null ? incptrNamed.getBehavior() : behavior,
                        incptrNamed.getEventTypes());
                }
            }
        else
            {
            incptrNamed = new NamedEventInterceptor<>(sIdentifier, interceptor, behavior);
            }

        registerEventInterceptorInternal(incptrNamed);

        return incptrNamed.getRegisteredName();
        }

    /**
     * Registers a unique {@link EventInterceptor} based on
     * {@link NamedEventInterceptor#getRegisteredName()}. The NamedEventInterceptor is
     * passed to the {@link EventDispatcher} however the wrapped
     * EventInterceptor is stored locally.
     *
     * @param incptrNamed  the NamedEventInterceptor to pass to the EventDispatcher
     */
    protected void registerEventInterceptorInternal(NamedEventInterceptor<?> incptrNamed)
        {
        SecurityManager security = System.getSecurityManager();
        if (security != null)
            {
            security.checkPermission(
                new LocalPermission("Service.registerEventInterceptor"));
            }

        String               sIdentifier = incptrNamed.getRegisteredName();
        RegistrationBehavior behavior    = incptrNamed.getBehavior();

        Map<String, NamedEventInterceptor<?>> mapInterceptors = m_mapInterceptors;

        if (mapInterceptors.containsKey(sIdentifier))
            {
            switch (behavior)
                {
                case IGNORE:
                    return;
                case REPLACE:
                    unregisterEventInterceptor(sIdentifier);
                    break;
                case ALWAYS:
                    // 1024 attempts in generating a unique identifier
                    for (int i = 0; i < 1024 && mapInterceptors.containsKey(sIdentifier); ++i)
                        {
                        sIdentifier = incptrNamed.generateName();
                        }
                    if (!mapInterceptors.containsKey(sIdentifier))
                        {
                        break;
                        }
                case FAIL:
                    throw new IllegalArgumentException(
                            "EventInterceptor " + incptrNamed + " is already registered");
                }
            }

        mapInterceptors.put(sIdentifier, incptrNamed);

        try
            {
            introduceInterceptor(incptrNamed);
            }
        catch (Exception e)
            {
            // Don't leave the interceptor partially registered with dispatchers
            unregisterEventInterceptor(sIdentifier);
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Introduce each currently registered {@link EventInterceptor} to the
     * newly registered {@link EventDispatcher}.
     *
     * @param dispatcher  the EventDispatcher to be introduced
     */
    protected void introduceDispatcher(EventDispatcher dispatcher)
        {
        for (Entry<String, NamedEventInterceptor<?>> entry : m_mapInterceptors.entrySet())
            {
            entry.getValue().introduceEventDispatcher(entry.getKey(), dispatcher);
            }
        }

    /**
     * Introduce the specified {@link NamedEventInterceptor} to each currently
     * registered {@link EventDispatcher}.
     *
     * @param incptrNamed  the NamedEventInterceptor to introduce
     */
    protected void introduceInterceptor(NamedEventInterceptor incptrNamed)
        {
        String sIdentifier = incptrNamed.getRegisteredName();
        for (EventDispatcher dispatcher : m_setDispatchers)
            {
            incptrNamed.introduceEventDispatcher(sIdentifier, dispatcher);
            }
        }

    /**
     * Call each currently registered {@link EventDispatcher} to remove the
     * specified {@link EventInterceptor}, by name.
     *
     * @param sIdentifier  the name of the EventInterceptor to be removed
     */
    protected void removeInterceptorFromDispatchers(String sIdentifier)
        {
        for (EventDispatcher dispatcher : m_setDispatchers)
            {
            dispatcher.removeEventInterceptor(sIdentifier);
            }
        }

    /**
     * The registered {@link EventInterceptor}s.
     */
    protected final Map<String, NamedEventInterceptor<?>> m_mapInterceptors;

    /**
     * The registered {@link EventDispatcher}s.
     */
    protected final Set<EventDispatcher> m_setDispatchers;
    }
