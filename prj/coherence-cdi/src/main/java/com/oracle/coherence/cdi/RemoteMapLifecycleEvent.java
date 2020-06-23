/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.tangosol.net.NamedMap;

/**
 * A RemoteMapLifecycleEvent allows subscribers to capture events pertaining to
 * the lifecycle of a remote {@link NamedMap}.
 * <p>
 * A remote {@link NamedMap} is a map that typically exists on a client as a proxy to
 * an remote map on the server.
 *
 * @author Jonathan Knight  2020.06.22
 * @since 20.06
 */
public interface RemoteMapLifecycleEvent
    {
    /**
     * The name of the map that the event is associated with.
     *
     * @return the name of the map that the event is associated with
     */
    String getMapName();

    /**
     * The name of the session that the event is associated with.
     *
     * @return the name of the session that the event is associated with
     */
    String getSessionName();

    /**
     * The name of the service that the event is associated with.
     *
     * @return the name of the service that the event is associated with
     */
    String getServiceName();

    /**
     * The {@link NamedMap} that this event is associated with.
     *
     * @return the {@link NamedMap} that this event is associated with
     */
    NamedMap<?, ?> getMap();

    /**
     * The scope name that this event is associated with.
     *
     * @return the scope name that this event is associated with
     */
    String getScope();

    /**
     * The type of this event.
     *
     * @return type of this event
     */
    Type getType();

    // ----- inner enum: Type -----------------------------------------------

    /**
     * The type of event.
     */
    enum Type
        {
        /**
         * The remote {@link NamedMap} was created.
         */
        Created,
        /**
         * The remote {@link NamedMap} was truncated.
         */
        Truncated,
        /**
         * The remote {@link NamedMap} was destroyed.
         */
        Destroyed
        }

    // ----- inner interface: Dispatcher ------------------------------------

    /**
     * A dispatcher of {@link RemoteMapLifecycleEvent}s
     */
    interface Dispatcher
        {
        /**
         * Dispatch a {@link RemoteMapLifecycleEvent}.
         *
         * @param map       the {@link NamedMap} that the event is for
         * @param sScope    the name of the scope that the map is in
         * @param sSession  the name of the {@link com.tangosol.net.Session} that owns the map
         * @param sService  the name of the service that owns the map
         * @param type      the event {@link Type}
         */
        void dispatch(NamedMap<?, ?> map, String sScope, String sSession, String sService, RemoteMapLifecycleEvent.Type type);

        /**
         * A no-op implementation of a {@link Dispatcher}.
         *
         * @return  no-op implementation of a {@link Dispatcher}
         */
        static Dispatcher nullImplementation()
            {
            return (map, sScope, sSession, sService, type) -> {};
            }
        }
    }
