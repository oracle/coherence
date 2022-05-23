/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.serverevents.interceptors;

import com.oracle.coherence.guides.serverevents.model.AuditEvent;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.events.annotation.Interceptor;
        
import com.tangosol.util.UUID;

/**
 * Common class for {@link Interceptor}s to extend.
 *
 * @author Tim Middleton 2022.05.04
 */
public abstract class AbstractAuditingInterceptor {
    
    protected NamedCache<UUID, AuditEvent> events;

    public static final String AUDIT_CACHE = "audit-events";

    /**
     * Returns the audit cache when requested. Doing this in a constructor is not recommended
     * as it can lead to race conditions.
     *
     * @return a new or existing audit event {@link NamedCache}
     */
    protected NamedCache<UUID, AuditEvent> getAuditCache() {
        if (events == null) {
            NamedCache<UUID, AuditEvent> nc;
            synchronized (this) {
                nc = CacheFactory.getCache(AUDIT_CACHE);
                if (events == null) {
                    events = nc;
                }
            }
        }

        return events;
    }
}
