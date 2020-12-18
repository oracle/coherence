/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events;

import com.tangosol.net.Session;

/**
 * A {@link SessionLifecycleEvent} allows subscribers
 * to capture events pertaining to the lifecycle of
 * a {@link Session} instance.
 *
 * @author Jonathan Knight  2020.12.16
 * @since 20.12
 */
public interface SessionLifecycleEvent
        extends Event<SessionLifecycleEvent.Type>
    {
    /**
     * The {@link Session} instance that this event is associated with.
     *
     * @return the {@link Session} instance that this event is associated with
     */
    public Session getSession();

    @Override
    SessionDispatcher getDispatcher();

    // ----- constants ------------------------------------------------------

    /**
     * The emitted event types for a {@link SessionLifecycleEvent}.
     */
    public static enum Type
        {
        /**
         * {@link SessionLifecycleEvent}s of the type {@code STARTING} are raised
         * before a {@link Session} instance is started.
         */
        STARTING,

        /**
         * {@link SessionLifecycleEvent}s of the type {@code STARTED} are raised
         * after a {@link Session} instance is started.
         */
        STARTED,

        /**
         * {@link SessionLifecycleEvent}s of the type {@code STOPPING} are raised
         * before a {@link Session} instance is stopped.
         */
        STOPPING,

        /**
         * {@link SessionLifecycleEvent}s of the type {@code STOPPED} are raised
         * after a {@link Session} instance is stopped.
         */
        STOPPED,
        }
    }
