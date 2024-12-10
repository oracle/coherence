/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */


package com.oracle.coherence.guides.serverevents.interceptors;

import java.io.Serializable;

import com.tangosol.net.events.EventInterceptor;

import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.annotation.LifecycleEvents;
import com.tangosol.net.events.application.LifecycleEvent;


/**
 * Interceptor for lifecycle events.
 *
 * @author Tim Middleton 2022.05.09
 */
// #tag::class[]
@Interceptor(identifier = "LifecycleEventsInterceptor")
@LifecycleEvents({LifecycleEvent.Type.ACTIVATING, LifecycleEvent.Type.ACTIVATED, LifecycleEvent.Type.DISPOSING})
public class LifecycleEventsInterceptor
        implements EventInterceptor<LifecycleEvent>, Serializable {

    @Override
    public void onEvent(LifecycleEvent event) {
        System.out.printf("Event %s received for ccf %s\n", event.getType().toString(), event.getConfigurableCacheFactory());
    }
}
// #end::class[]
