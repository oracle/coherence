/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.federation;

import com.tangosol.net.events.partition.PartitionedServiceDispatcher;

/**
 * A FederatedServiceDispatcher dispatches {@link com.tangosol.net.events.Event}s
 * from a FederatedCacheService.  This dispatcher can raise:
 * <ul>
 *   <li>{@link FederatedChangeEvent}s</li>
 *   <li>{@link FederatedConnectionEvent}s</li>
 *   <li>{@link FederatedPartitionEvent}s</li>
 * </ul>
 *
 * @author Jonathan Knight 2021.01.14
 * @since  21.06
 */
public interface FederatedServiceDispatcher
        extends PartitionedServiceDispatcher
    {
    }
