/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.partition.cache;

/**
 * An event dispatched by a {@link PartitionedCacheDispatcher}.
 *
 * @param <T>  the type of event
 *
 * @author rhl/hr/gg  2012.09.21
 * @since Coherence 12.1.2
 */
public interface Event<T extends Enum<T>>
            extends com.tangosol.net.events.Event<T>
    {
    /**
     * {@inheritDoc}
     */
    @Override public PartitionedCacheDispatcher getDispatcher();
    }
