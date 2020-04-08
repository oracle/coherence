/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.partition;

/**
 * A PartitionedServiceEvent captures information concerning an operation on
 * a PartitionedService. Sub interfaces provide more context on the event.
 *
 * @since Coherence 12.1.2
 *
 * @param <T> the type of event
 */
public interface Event<T extends Enum<T>>
        extends com.tangosol.net.events.Event<T>
    {
    /**
     * Return the {@link PartitionedServiceDispatcher} this event was
     * raised by.
     *
     * @return the PartitionedServiceDispatcher this event was raised by
     */
    @Override public PartitionedServiceDispatcher getDispatcher();
    }
