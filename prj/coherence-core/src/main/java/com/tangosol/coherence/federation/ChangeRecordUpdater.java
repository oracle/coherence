/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.federation;

import com.tangosol.net.events.federation.FederatedChangeEvent;

/**
 * ChangeRecordUpdater allows to update the given {@link ChangeRecord} while processing the
 * {@link FederatedChangeEvent}.
 *
 * @author cl  2014.06.09
 */
public interface ChangeRecordUpdater<K, V>
    {
    /**
     * Update the {@link ChangeRecord}.
     *
     * @param sParticipant  the participant name of the destination
     * @param sCacheName    the cache name
     * @param record        the ChangeRecord which includes the details about the change
     */
    public void update(String sParticipant, String sCacheName, ChangeRecord<K, V> record);
    }
