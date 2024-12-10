/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events;

import com.tangosol.net.Coherence;

/**
 * A CoherenceDispatcher raises {@link Event}s pertaining to
 * operations on a {@link Coherence} instance:
 * <ul>
 *   <li>{@link CoherenceLifecycleEvent CoherenceLifecycleEvents}</li>
 * </ul>
 *
 * @author Jonathan Knight  2020.11.10
 * @since 20.12
 */
public interface CoherenceDispatcher
        extends EventDispatcher
    {
    /**
     * Return the name of the {@link Coherence} instance that this
     * {@link CoherenceDispatcher} is associated with.
     *
     * @return  the name of the {@link Coherence} instance
     */
    String getName();
    }