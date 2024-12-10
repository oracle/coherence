/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.federation;

import com.tangosol.net.events.Event;

/**
 * Represents a change in replication state of a partition during replication.
 *
 * <strong>NOTE:</strong> This event is dispatched per partition.
 *
 * @author cl 2015.12.02
 *
 * @since 12.2.1
 */
public interface FederatedPartitionEvent
        extends Event<FederatedPartitionEvent.Type>
    {
    /**
     * Obtain the partition Id for this event.
     *
     * @return the partition id
     */
    int getPartitionId();

    /**
     * Obtain the participant name where the partition is replicating to.
     *
     * @return the participant name
     */
    String getParticipant();

    /**
     * Obtain the participant name where this event is raised from.
     *
     * @return the local participant name
     */
    String getLocalParticipant();

    /**
     * The type of {@link FederatedPartitionEvent}s.
     */
    enum Type
        {
        /**
         * Dispatched when the partition is being replicated due to a
         * {@link com.tangosol.internal.federation.FederationManagerMBean#replicateAll} operation.
         */
        SYNCING,

        /**
         * Dispatched when the partition has been confirmed to be replicated at the destination for
         * {@link com.tangosol.internal.federation.FederationManagerMBean#replicateAll} operation.
         */
        SYNCED
        }
    }
