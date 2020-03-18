/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.application;

import com.tangosol.net.ConfigurableCacheFactory;

/**
 * An EventDispatcher dispatches {@link LifecycleEvent}s
 * on the behalf of a {@link ConfigurableCacheFactory}.
 *
 * This dispatcher can raise the following events:
 * <ul>
 *   <li>{@link LifecycleEvent.Type#ACTIVATED}</li>
 *   <li>{@link LifecycleEvent.Type#DISPOSING}</li>
 * </ul>
 *
 * @author nsa/hr 2012.08.24
 * @since Coherence 12.1.2
 */
@Deprecated
public interface EventDispatcher
        extends com.tangosol.net.events.EventDispatcher
    {

    /**
     * Dispatch the {@link LifecycleEvent.Type#ACTIVATED} event to all
     * concerned {@link com.tangosol.net.events.EventInterceptor}.
     *
     * @param ccf  the {@link ConfigurableCacheFactory}
     *             that raised the event
     */
    public void dispatchActivated(ConfigurableCacheFactory ccf);


    /**
     * Dispatch the {@link LifecycleEvent.Type#DISPOSING} event to all
     * concerned {@link com.tangosol.net.events.EventInterceptor}.
     *
     * @param ccf  the {@link ConfigurableCacheFactory}
     *             that raised the event
     */
    public void dispatchDisposing(ConfigurableCacheFactory ccf);
    }
