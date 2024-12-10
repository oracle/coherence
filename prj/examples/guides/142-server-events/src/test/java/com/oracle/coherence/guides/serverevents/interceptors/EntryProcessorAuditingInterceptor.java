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
import com.tangosol.net.events.annotation.EntryProcessorEvents;
import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.partition.cache.EntryProcessorEvent;


/**
 * An {@link Interceptor} that will audit an entry process being called.
 *
 * @author Tim Middleton 2022.05.04
 */
// #tag::class[]
@Interceptor(identifier = "EntryProcessorAuditingInterceptor")  // <1>
@EntryProcessorEvents({EntryProcessorEvent.Type.EXECUTED})  // <2>
public class EntryProcessorAuditingInterceptor
        extends AbstractAuditingInterceptor
        implements EventInterceptor<EntryProcessorEvent>, Serializable {  // <3>

    @Override
    public void onEvent(EntryProcessorEvent event) {  // <4>
        AuditEvent auditEvent = new AuditEvent("cache=" + event.getCacheName(), event.getType().toString(),
                String.format("Entries=%d, processor=%s", event.getEntrySet().size(), event.getProcessor().toString()));
        getAuditCache().put(auditEvent.getId(), auditEvent);
    }
}
// #end::class[]
