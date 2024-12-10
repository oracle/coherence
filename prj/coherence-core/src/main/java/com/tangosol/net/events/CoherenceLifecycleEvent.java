/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events;

import com.tangosol.net.Coherence;

/**
 * A {@link CoherenceLifecycleEvent} allows subscribers
 * to capture events pertaining to the lifecycle of
 * a {@link Coherence} instance.
 *
 * @author Jonathan Knight  2020.11.10
 * @since 20.12
 */
public interface CoherenceLifecycleEvent
        extends Event<CoherenceLifecycleEvent.Type>
    {
    /**
     * The {@link Coherence} instance that this event is associated with.
     *
     * @return the {@link Coherence} instance that this event is associated with
     */
    public Coherence getCoherence();

    @Override
    CoherenceDispatcher getDispatcher();

    // ----- constants ------------------------------------------------------

    /**
     * The emitted event types for a {@link CoherenceLifecycleEvent}.
     */
    public static enum Type
        {
        /**
         * {@link CoherenceLifecycleEvent}s of the type {@code STARTING} are raised
         * before a {@link Coherence} instance is started.
         */
        STARTING,

        /**
         * {@link CoherenceLifecycleEvent}s of the type {@code STARTED} are raised
         * after a {@link Coherence} instance is started.
         */
        STARTED,

        /**
         * {@link CoherenceLifecycleEvent}s of the type {@code STOPPING} are raised
         * before a {@link Coherence} instance is stopped.
         */
        STOPPING,

        /**
         * {@link CoherenceLifecycleEvent}s of the type {@code STOPPED} are raised
         * after a {@link Coherence} instance is stopped.
         */
        STOPPED,
        }
    }
