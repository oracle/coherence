/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import javax.annotation.Priority;

import javax.enterprise.event.Observes;

import javax.enterprise.inject.spi.BeforeShutdown;

import javax.enterprise.inject.spi.Extension;

/**
 * A gRPC client side CDI {@link Extension}.
 *
 * @author Jonathan Knight  2020.01.10
 * @since 20.06
 */
public class ClientCdiExtension
        implements Extension
    {
    /**
     * Clean-up any client side resources.
     * <p>
     * This observer has a priority so that it executes before other
     * observers, such as the gRPC server shutdown.
     *
     * @param event  the {@link BeforeShutdown} event (ignored)
     */
    protected void shutdown(@Observes @Priority(1) BeforeShutdown event)
        {
        RemoteSessions.instance().shutdown();
        }
    }
