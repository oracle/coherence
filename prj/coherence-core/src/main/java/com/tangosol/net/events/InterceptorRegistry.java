/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events;

import com.tangosol.util.RegistrationBehavior;

/**
 * An InterceptorRegistry manages the registration of {@link EventInterceptor}s
 * and facilitates the introduction of EventInterceptors to {@link
 * EventDispatcher}s.
 * <p>
 * Registering an EventInterceptor will cause it to be introduced to all
 * currently registered and future EventDispatchers. EventInterceptors
 * can assume responsibility for determining whether or not to bind to each
 * EventDispatcher by implementing
 * {@link EventDispatcherAwareInterceptor#introduceEventDispatcher}.
 * <p>
 * The semantics of how to act upon discovering a duplicate interceptor, based
 * on identifier, can be prescribed via the {@link RegistrationBehavior} enum.
 *
 * @author rhan, nsa, mwj, rhl, hr 2011.03.29
 * @since Coherence 12.1.2
 */
public interface InterceptorRegistry
    {
    /**
     * Register an {@link EventInterceptor} uniquely identified based on the
     * presence of an {@link com.tangosol.net.events.annotation.Interceptor
     * annotation} or default to the fully qualified class name. The
     * EventInterceptor will be introduced to all current and future
     * {@link EventDispatcher}s.
     *
     * @param interceptor  the EventInterceptor to register
     *
     * @return a string identifier used to register the interceptor
     *
     * @throws IllegalArgumentException if an EventInterceptor with the
     *         same identifier is already registered
     */
    public String registerEventInterceptor(EventInterceptor<?> interceptor);

    /**
     * Register an {@link EventInterceptor} uniquely identified based on the
     * presence of an {@link com.tangosol.net.events.annotation.Interceptor
     * annotation} or default to the fully qualified class name. The {@link
     * RegistrationBehavior} specifies how to act upon registering a duplicate
     * interceptor. The EventInterceptor will be introduced to all current and
     * future {@link EventDispatcher}s.
     *
     * @param interceptor  the EventInterceptor to register
     * @param behavior     the behavior enacted upon discovering duplicate
     *                     interceptors
     *
     * @return a string identifier used to register the interceptor
     *
     * @throws IllegalArgumentException if an EventInterceptor with the
     *         same identifier is already registered
     */
    public String registerEventInterceptor(EventInterceptor<?> interceptor, RegistrationBehavior behavior);

    /**
     * Register a uniquely identified {@link EventInterceptor}. The
     * EventInterceptor will be introduced to all current and future
     * {@link EventDispatcher}s.
     *
     * @param sIdentifier  the unique name identifying the EventInterceptor
     * @param interceptor  the EventInterceptor to register
     * @param behavior     the behavior enacted upon discovering duplicate
     *                     interceptors
     *
     * @throws IllegalArgumentException if an EventInterceptor with the same
     *         identifier is already registered
     */
    public String registerEventInterceptor(String sIdentifier, EventInterceptor<?> interceptor, RegistrationBehavior behavior);

    /**
     * Unregister an {@link EventInterceptor}, and remove it from all {@link
     * EventDispatcher}s.
     *
     * @param sIdentifier  the unique identifier of the interceptor to unregister
     */
    public void unregisterEventInterceptor(String sIdentifier);

    /**
     * Return a registered {@link EventInterceptor}.
     *
     * @return sIdentifier  the unique identifier of the interceptor
     */
    public EventInterceptor<?> getEventInterceptor(String sIdentifier);
    }
