/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.events.federation;

import com.tangosol.coherence.federation.ChangeRecord;
import com.tangosol.net.events.Event;

import java.util.Map;

/**
 * Represents all of the changes that have occurred against a single partition
 * in a Federated Cache as part of a partition transaction.
 *
 * @author cl 2014.06.09
 *
 * @since 12.2.1
 *
 * @see ChangeRecord
 */
public interface FederatedChangeEvent
    extends Event<FederatedChangeEvent.Type>
    {
    /**
     * Obtain the {@link ChangeRecord}s that are part of a transaction,
     * organized by the name of the cache on which the changes occurred.
     *
     * @param <K>  the key type
     * @param <V>  the value type
     *
     * @return  a map of cache names to {@link ChangeRecord}s
     */
    public <K, V> Map<String, Iterable<ChangeRecord<K, V>>> getChanges();

    /**
     * Obtain the name of the Participant for the {@link FederatedChangeEvent}.
     * <p>
     * For the {@link com.tangosol.net.events.federation.FederatedChangeEvent.Type#COMMITTING_LOCAL}
     * and {@link com.tangosol.net.events.federation.FederatedChangeEvent.Type#COMMITTING_REMOTE}
     * events this will be the local Participant name.
     * <p>
     * For the {@link com.tangosol.net.events.federation.FederatedChangeEvent.Type#REPLICATING}
     * event this will be either a remote Participant name or an interceptor
     * Participant name.  Therefore, the interceptor must filter the events by
     * the Participant name to get the events intended for it.
     *
     * @return the Participant name.
     */
    public String getParticipant();

    /**
     * The types of {@link FederatedChangeEvent}s.
     */
    public static enum Type
        {

        /**
         * Dispatched before {@link ChangeRecord}s from the local Participant
         * are committed locally.
         */
        COMMITTING_LOCAL,

        /**
         * Dispatched before {@link ChangeRecord}s from other Participants
         * are committed locally.
         */
        COMMITTING_REMOTE,

        /**
         * Dispatched as {@link ChangeRecord}s from the local Participant
         * are replicated to other Participants.
         */
        REPLICATING
        }
    }
