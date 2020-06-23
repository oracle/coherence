/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.cdi;

import com.oracle.coherence.cdi.events.Created;
import com.oracle.coherence.cdi.events.Destroyed;
import com.oracle.coherence.cdi.events.Truncated;
import com.tangosol.net.NamedMap;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.util.Objects;

/**
 * A producer of {@link RemoteMapLifecycleEvent}s.
 *
 * @author Jonathan Knight  2020.06.22
 * @since 20.06
 */
@ApplicationScoped
class RemoteMapLifecycleEventProducer
        implements RemoteMapLifecycleEvent.Dispatcher
    {
    // ----- constructors ---------------------------------------------------

    @Inject
    public RemoteMapLifecycleEventProducer(@Created   Event<RemoteMapLifecycleEvent> created,
                                           @Truncated Event<RemoteMapLifecycleEvent> truncated,
                                           @Destroyed Event<RemoteMapLifecycleEvent> destroyed)
        {
        f_createdEventDispatcher   = created;
        f_truncatedEventDispatcher = truncated;
        f_destroyedEventDispatcher = destroyed;
        }

    // ----- RemoteMapEventProducer methods ---------------------------------

    public void dispatch(NamedMap<?, ?> map, String sScope, String sSession, String sService, RemoteMapLifecycleEvent.Type type)
        {
        RemoteMapEvent mapEvent = new RemoteMapEvent(map, sScope, sSession, sService, type);
        switch (type)
            {
            case Created:
                f_createdEventDispatcher.fireAsync(mapEvent);
                f_createdEventDispatcher.fire(mapEvent);
                break;
            case Truncated:
                f_truncatedEventDispatcher.fireAsync(mapEvent);
                f_truncatedEventDispatcher.fire(mapEvent);
                break;
            case Destroyed:
                f_destroyedEventDispatcher.fireAsync(mapEvent);
                f_destroyedEventDispatcher.fire(mapEvent);
                break;
            default:
                // ignored
            }
        }

    // ----- inner class: RemoteMapEvent ------------------------------------

    private static class RemoteMapEvent
            implements RemoteMapLifecycleEvent
        {
        private RemoteMapEvent(NamedMap<?, ?> map, String sScope, String sSession, String sService, RemoteMapLifecycleEvent.Type type)
            {
            f_map      = Objects.requireNonNull(map);
            f_sScope   = sScope == null ? Scope.DEFAULT : sScope;
            f_sSession = sSession == null ? Remote.DEFAULT_NAME : sSession;
            f_sService = sService;
            f_type     = Objects.requireNonNull(type);
            }

        @Override
        public String getMapName()
            {
            return f_map.getName();
            }

        @Override
        public String getScope()
            {
            return f_sScope;
            }

        @Override
        public String getSessionName()
            {
            return f_sSession;
            }

        @Override
        public String getServiceName()
            {
            return f_sService;
            }

        @Override
        public NamedMap<?, ?> getMap()
            {
            return f_map;
            }

        @Override
        public Type getType()
            {
            return f_type;
            }

        // ----- data members -----------------------------------------------

        private final NamedMap<?, ?> f_map;

        private final String f_sScope;

        private final String f_sSession;

        private final String f_sService;

        private final RemoteMapLifecycleEvent.Type f_type;
        }

    // ----- data members ---------------------------------------------------

    private final Event<RemoteMapLifecycleEvent> f_createdEventDispatcher;
    private final Event<RemoteMapLifecycleEvent> f_truncatedEventDispatcher;
    private final Event<RemoteMapLifecycleEvent> f_destroyedEventDispatcher;
    }
