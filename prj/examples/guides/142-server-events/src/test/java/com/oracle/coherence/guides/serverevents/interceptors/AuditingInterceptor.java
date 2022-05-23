/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.serverevents.interceptors;

import java.io.Serializable;

import com.oracle.coherence.guides.serverevents.model.AuditEvent;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.annotation.EntryEvents;
import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.partition.cache.EntryEvent;

/**
 * An {@link Interceptor} that will audit any insert, update or delete events on a cache.
 *
 * @author Tim Middleton 2022.05.04
 */
// #tag::class[]
@Interceptor(identifier = "AuditingInterceptor", order = Interceptor.Order.HIGH)  // <1>
@EntryEvents({EntryEvent.Type.INSERTED, EntryEvent.Type.UPDATED, EntryEvent.Type.REMOVED})  // <2>
public class AuditingInterceptor
        extends AbstractAuditingInterceptor
        implements EventInterceptor<EntryEvent<?, ?>>, Serializable {  // <3>

    @Override
    public void onEvent(EntryEvent<?, ?> event) {  // <4>
        String          oldValue  = null;
        String          newValue  = null;
        EntryEvent.Type eventType = event.getType();
        Object          key       = event.getKey();

        if (eventType == EntryEvent.Type.REMOVED || eventType == EntryEvent.Type.UPDATED) {  // <5>
            oldValue = event.getOriginalValue().toString();
        }
        if (eventType == EntryEvent.Type.INSERTED || eventType == EntryEvent.Type.UPDATED) {
            newValue = event.getValue().toString();
        }

        AuditEvent auditEvent = new AuditEvent("cache=" + event.getCacheName(), eventType.toString(),  // <6>
                String.format("key=%s, old=%s, new=%s", key, oldValue, newValue));
        getAuditCache().put(auditEvent.getId(), auditEvent);
    }
}
// #end::class[]
