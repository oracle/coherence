/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.bootstrap;

// # tag::code[]
import com.tangosol.net.Coherence;
import com.tangosol.net.events.CoherenceLifecycleEvent;

public class WebServerController
        implements Coherence.LifecycleListener
    {
    @Override
    public void onEvent(CoherenceLifecycleEvent event)
        {
        switch (event.getType())
            {
            case STARTED:
                server.start();
                break;
            case STOPPING:
                server.stop();
                break;
            }
        }

    private final HttpServer server = new HttpServer();
    }
// # end::code[]
