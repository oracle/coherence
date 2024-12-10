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
import com.tangosol.net.events.annotation.CacheLifecycleEvents;
import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;

/**
 * Interceptor for cache lifecycle events.
 *
 * @author Tim Middleton 2022.05.09
 */
// #tag::class[]
@Interceptor(identifier = "CacheLifecycleEventsInterceptor") // <1>
@CacheLifecycleEvents( {CacheLifecycleEvent.Type.CREATED, CacheLifecycleEvent.Type.DESTROYED, CacheLifecycleEvent.Type.TRUNCATED})  // <2>
public class CacheLifecycleEventsInterceptor
        extends AbstractAuditingInterceptor
        implements EventInterceptor<CacheLifecycleEvent>, Serializable {  // <3>

    @Override
    public void onEvent(CacheLifecycleEvent event) {  // <4>
        AuditEvent auditEvent = new AuditEvent("cache=" + event.getCacheName(), event.getType().toString(),
                String.format("Event from service %s", event.getServiceName()));
        getAuditCache().put(auditEvent.getId(), auditEvent);
    }
}
// #end::class[]
