/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.partition;

import com.tangosol.net.PartitionedService;
import com.tangosol.net.events.EventDispatcher;

/**
 * A PartitionedServiceDispatcher dispatches {@link com.tangosol.net.events.Event}s
 * from a {@link PartitionedService}.  This dispatcher can raise:
 * <ul>
 *   <li>{@link TransactionEvent}s</li>
 *   <li>{@link TransferEvent}s</li>
 * </ul>
 *
 * @author rhan, nsa, rhl, hr  2011.03.29
 * @since  Coherence 12.1.2
 */
public interface PartitionedServiceDispatcher
        extends EventDispatcher
    {
    /**
     * Return the {@link PartitionedService} for this dispatcher.
     *
     * @return the {@link PartitionedService} for this dispatcher
     */
    public PartitionedService getService();

    /**
     * Return the name of the {@link PartitionedService service} that this
     * PartitionedServiceDispatcher is associated with.
     *
     * @return  the service name
     */
    public default String getServiceName()
        {
        return getService().getInfo().getServiceName();
        }
    }
