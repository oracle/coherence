/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.partition.cache;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.events.EventDispatcher;


/**
 * A PartitionedCacheDispatcher raises the following server-side {@link
 * com.tangosol.net.events.Event}s pertaining to backing-map operations:
 * <ul>
 *   <li>{@link EntryEvent}s</li>
 *   <li>{@link EntryProcessorEvent}s</li>
 * </ul>
 *
 * @author rhan, nsa, rhl, hr  2011.03.29
 * @since Coherence 12.1.2
 */
public interface PartitionedCacheDispatcher
        extends EventDispatcher
    {
    /**
     * Return the {@link BackingMapContext} for this dispatcher.
     *
     * @return the BackingMapContext for this dispatcher
     */
    public BackingMapContext getBackingMapContext();
    }