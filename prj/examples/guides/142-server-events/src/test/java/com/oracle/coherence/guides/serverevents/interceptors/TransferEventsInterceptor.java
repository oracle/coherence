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
import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.annotation.TransferEvents;
import com.tangosol.net.events.partition.TransferEvent;

/**
 * Interceptor for transfer events.
 *
 * @author Tim Middleton 2022.05.09
 */
// #tag::class[]
@Interceptor(identifier = "TransferEventsInterceptor")  // <1>
@TransferEvents({TransferEvent.Type.ARRIVED, TransferEvent.Type.DEPARTING, TransferEvent.Type.LOST}) // <2>
public class TransferEventsInterceptor
        extends AbstractAuditingInterceptor
        implements EventInterceptor<TransferEvent>, Serializable {  // <3>

    @Override
    public void onEvent(TransferEvent event) {  // <4>
        AuditEvent auditEvent = new AuditEvent("partition=" + event.getPartitionId(), event.getType().toString(),
                String.format("Partitions from remote member %s", event.getRemoteMember()));
        getAuditCache().put(auditEvent.getId(), auditEvent);
    }
}
// #end::class[]
