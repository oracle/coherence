/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.util.Listeners;

import java.util.EventListener;

/**
 * A base class for {@link GrpcConnection} implementations.
 *
 * @author Jonathan Knight  2025.01.25
 */
public abstract class BaseGrpcConnection
        implements GrpcConnection
    {
    @Override
    public void addConnectionListener(ConnectionListener listener)
        {
        f_connectionListeners.add(listener);
        }

    @Override
    public void removeConnectionListener(ConnectionListener listener)
        {
        f_connectionListeners.remove(listener);
        }

    /**
     * Dispatch a connected {@link ConnectionEvent}.
     */
    protected void dispatchConnected()
        {
        dispatchConnectionEvent(ConnectionEvent.Type.Connected);
        }

    /**
     * Dispatch a disconnected {@link ConnectionEvent}.
     */
    protected void dispatchDisconnected()
        {
        dispatchConnectionEvent(ConnectionEvent.Type.Disconnected);
        }

    /**
     * Dispatch a {@link ConnectionEvent}.
     *
     * @param type  the type of the event to dispatch
     */
    protected void dispatchConnectionEvent(ConnectionEvent.Type type)
        {
        ConnectionEvent event     = new ConnectionEvent(this, type);
        EventListener[] listeners = f_connectionListeners.listeners();
        for (EventListener listener : listeners)
            {
            try
                {
                ((ConnectionListener) listener).onConnectionEvent(event);
                }
            catch (Throwable t)
                {
                Logger.err("Caught exception dispatching connection event to " + listener, t);
                }
            }
        }
    // ----- data members ---------------------------------------------------

    /**
     * The connection event listeners.
     */
    private final Listeners f_connectionListeners = new Listeners();
    }
