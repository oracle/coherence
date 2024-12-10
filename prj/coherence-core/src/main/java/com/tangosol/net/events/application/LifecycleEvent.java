/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.application;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.events.Event;

/**
 * A LifecycleEvent encapsulates a lifecycle action that has
 * occurred on a {@link ConfigurableCacheFactory}. The
 * following events can be raised:
 * <ol>
 *     <li>{@link Type#ACTIVATED ACTIVATED}</li>
 *     <li>{@link Type#DISPOSING DISPOSING}</li>
 * </ol>
 * Note: {@link LifecycleEvent}s are dispatched to interceptors by
 * the same thread calling the lifecycle methods on the
 * ConfigurableCacheFactory implementation. This thread
 * may well be synchronized thus interceptors must ensure any spawned
 * threads do not synchronize on the same CCF object.
 *
 * @author nsa/hr 2012.08.24
 * @since Coherence 12.1.2
 */
public interface LifecycleEvent
        extends Event<LifecycleEvent.Type>
    {

    /**
     * Returns the {@link ConfigurableCacheFactory}
     * instance that was activated or about to be disposed.
     *
     * @return the ConfigurableCacheFactory instance
     *         that was activated or about to be disposed
     */
    public ConfigurableCacheFactory getConfigurableCacheFactory();

    // ----- constants --------------------------------------------------

    /**
     * The {@link LifecycleEvent} types.
     */
    public static enum Type
        {
        /**
         * This {@link LifecycleEvent} is dispatched prior to the activation
         * of a {@link ConfigurableCacheFactory}. This typically suggests that
         * any {@code autostart} services have not been started, however will
         * be after this event is emitted. See
         * {@link ConfigurableCacheFactory#activate()} for more details.
         */
        ACTIVATING,

        /**
         * This {@link LifecycleEvent} is dispatched when a {@link
         * ConfigurableCacheFactory}
         * has been activated. This typically means that all
         * {@code autostart} services have been started. See
         * {@link ConfigurableCacheFactory#activate()})
         * for more details.
         */
        ACTIVATED,

        /**
         * This {@link LifecycleEvent} is dispatched when a {@link
         * ConfigurableCacheFactory}
         * is about to be disposed. After interceptors are notified
         * of this event typically the ConfigurableCacheFactory
         * will shut down all of it's services and clean up it's
         * resources.
         */
        DISPOSING
        }
    }
