/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events;

/**
 * A CoherenceDispatcher raises {@link Event}s pertaining to
 * operations on a {@link com.tangosol.net.Coherence} instance:
 * <ul>
 *   <li>{@link CoherenceLifecycleEvent CoherenceLifecycleEvents}</li>
 * </ul>
 *
 * @author Jonathan Knight  2020.12.16
 * @since 20.12
 */
public interface SessionDispatcher
        extends EventDispatcher
    {
    /**
     * Return the name of the {@link com.tangosol.net.Session} instance that this
     * {@link SessionDispatcher} is associated with.
     *
     * @return  the name of the {@link com.tangosol.net.Session} instance
     */
    String getName();
    }
