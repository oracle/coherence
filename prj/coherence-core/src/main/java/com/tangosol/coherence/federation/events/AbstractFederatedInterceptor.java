/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.federation.events;

import com.tangosol.coherence.federation.ChangeRecord;
import com.tangosol.coherence.federation.ChangeRecordUpdater;

import com.tangosol.net.events.EventDispatcher;
import com.tangosol.net.events.EventDispatcherAwareInterceptor;
import com.tangosol.net.events.federation.FederatedChangeEvent;
import com.tangosol.net.events.federation.FederatedServiceDispatcher;

import java.util.Map;

/**
 * An abstract implementation of an {@link com.tangosol.net.events.EventInterceptor}
 * for {@link FederatedChangeEvent}s applicable to remote participants.
 *
 * @author cl  2014.06.09
 */
public abstract class AbstractFederatedInterceptor<K, V>
        implements EventDispatcherAwareInterceptor<FederatedChangeEvent>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public void onEvent(FederatedChangeEvent event)
        {
        Map<String, Iterable<ChangeRecord<K, V>>> mapChanges = event.getChanges();

        for (Map.Entry<String, Iterable<ChangeRecord<K, V>>> entry : mapChanges.entrySet())
            {
            for (ChangeRecord<K, V> record : entry.getValue())
                {
                getChangeRecordUpdater().update(event.getParticipant(), entry.getKey(), record);
                }
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void introduceEventDispatcher(String sIdentifier, EventDispatcher dispatcher)
        {
        if (dispatcher instanceof FederatedServiceDispatcher)
            {
            dispatcher.addEventInterceptor(sIdentifier, this);
            }
        }

    /**
     * Return a {@link ChangeRecordUpdater} which will be called to update each {@link ChangeRecord}.
     *
     * @return a ChangeRecordUpdater
     */
    public abstract ChangeRecordUpdater<K, V> getChangeRecordUpdater();
    }
