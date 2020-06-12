/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events;

/**
 * An EventInterceptor provides an implementation that is capable of
 * intercepting and processing {@link Event}s.
 * <p>
 * While it's possible for EventInterceptor instances to be reused, they
 * should be <strong>immutable</strong> or thread-safe such that an
 * interceptor could be dispatched by multiple threads concurrently.
 *
 * @author nsa, rhan, bo, mwj, rhl, hr 2011.03.29
 * @since Coherence 12.1.2
 *
 * @param <E>  the type of {@link Event} this interceptor accepts
 */
public interface EventInterceptor<E extends Event<? extends Enum>>
    {
    /**
     * Perform necessary processing of the specified {@link Event}.
     *
     * @param event  the Event to be processed
     *
     * @see com.tangosol.net.events.Event#nextInterceptor()
     */
    public void onEvent(E event);
    }