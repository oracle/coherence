/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server;

import com.oracle.coherence.event.EventObserverSupport;

import com.oracle.coherence.event.Backlog;
import com.oracle.coherence.event.Connecting;
import com.oracle.coherence.event.Disconnected;
import com.oracle.coherence.event.Error;
import com.oracle.coherence.event.CommittingLocal;
import com.oracle.coherence.event.ParticipantName;
import com.oracle.coherence.event.CommittingRemote;
import com.oracle.coherence.event.Replicating;
import com.oracle.coherence.event.Synced;
import com.oracle.coherence.event.Syncing;

import com.oracle.coherence.event.EventObserverSupport.EventObserver;

import com.tangosol.net.events.EventDispatcher;

import com.tangosol.net.events.federation.FederatedChangeEvent;
import com.tangosol.net.events.federation.FederatedConnectionEvent;
import com.tangosol.net.events.federation.FederatedPartitionEvent;
import com.tangosol.net.events.federation.FederatedServiceDispatcher;

import java.lang.annotation.Annotation;

import java.util.function.Function;

/**
 * Federation event handlers that allow CDI observers to handle any federation
 * event.
 *
 * @author Aleks Seovic  2020.04.13
 * @since 20.06
 */
@SuppressWarnings("unused")
public class FederationEventHandlers
    {
    // ---- inner class: FederationEventHandler -----------------------------

   /**
    * Abstract base class for all observer-based federation interceptors.
    *
    * @param <E>  the type of {@link com.tangosol.net.events.Event} this interceptor accepts
    * @param <T>  the enumeration of event types E supports
    */
   static abstract class FederationEventHandler<E extends com.tangosol.net.events.Event<T>, T extends Enum<T>>
           extends EventObserverSupport.ServiceEventHandler<E, T>
       {
       protected FederationEventHandler(EventObserver<E> observer,
                                        Class<T> classType,
                                        Function<E, String> fnParticipantName)
           {
           super(observer, classType);

           m_fnParticipantName = fnParticipantName;

           String participantName = null;

           for (Annotation a : observer.getObservedQualifiers())
               {
               if (a instanceof ParticipantName)
                   {
                   participantName = ((ParticipantName) a).value();
                   }
               }

           m_participantName = participantName;
           }

       @Override
       protected boolean isApplicable(EventDispatcher dispatcher, String sScopeName)
           {
           return dispatcher instanceof FederatedServiceDispatcher && super.isApplicable(dispatcher, sScopeName);
           }

       @Override
       protected boolean shouldFire(E event)
           {
           return m_participantName == null || m_participantName.equals(m_fnParticipantName.apply(event));
           }

       // ---- data members ----------------------------------------------------

       protected final String m_participantName;
       protected final Function<E, String> m_fnParticipantName;
       }

    // ---- inner class: FederatedConnectionEventHandler --------------------

    /**
     * Handler for {@link FederatedConnectionEvent}s.
     */
    static class FederatedConnectionEventHandler
            extends FederationEventHandler<FederatedConnectionEvent, FederatedConnectionEvent.Type>
        {
        FederatedConnectionEventHandler(EventObserver<FederatedConnectionEvent> observer)
            {
            super(observer, FederatedConnectionEvent.Type.class, FederatedConnectionEvent::getParticipantName);

            for (Annotation a : observer.getObservedQualifiers())
                {
                if (a instanceof Connecting)
                    {
                    addType(FederatedConnectionEvent.Type.CONNECTING);
                    }
                else if (a instanceof Disconnected)
                    {
                    addType(FederatedConnectionEvent.Type.DISCONNECTED);
                    }
                else if (a instanceof Backlog)
                    {
                    Backlog backlog = (Backlog) a;
                    if (backlog.value() == Backlog.Type.EXCESSIVE)
                        {
                        addType(FederatedConnectionEvent.Type.BACKLOG_EXCESSIVE);
                        }
                    else
                        {
                        addType(FederatedConnectionEvent.Type.BACKLOG_NORMAL);
                        }
                    }
                else if (a instanceof Error)
                    {
                    addType(FederatedConnectionEvent.Type.ERROR);
                    }
                }
            }
        }

    // ---- inner class: FederatedChangeEventHandler ------------------------

    /**
     * Handler for {@link FederatedChangeEvent}s.
     */
    static class FederatedChangeEventHandler
            extends FederationEventHandler<FederatedChangeEvent, FederatedChangeEvent.Type>
        {
        FederatedChangeEventHandler(EventObserver<FederatedChangeEvent> observer)
            {
            super(observer, FederatedChangeEvent.Type.class, FederatedChangeEvent::getParticipant);

            for (Annotation a : observer.getObservedQualifiers())
                {
                if (a instanceof CommittingLocal)
                    {
                    addType(FederatedChangeEvent.Type.COMMITTING_LOCAL);
                    }
                else if (a instanceof CommittingRemote)
                    {
                    addType(FederatedChangeEvent.Type.COMMITTING_REMOTE);
                    }
                else if (a instanceof Replicating)
                    {
                    addType(FederatedChangeEvent.Type.REPLICATING);
                    }
                }
            }
        }

    // ---- inner class: FederatedChangeEventHandler -------------------------

    /**
     * Handler for {@link FederatedPartitionEvent}s.
     */
    static class FederatedPartitionEventHandler
            extends FederationEventHandler<FederatedPartitionEvent, FederatedPartitionEvent.Type>
        {
        FederatedPartitionEventHandler(EventObserver<FederatedPartitionEvent> observer)
            {
            super(observer, FederatedPartitionEvent.Type.class, FederatedPartitionEvent::getParticipant);

            for (Annotation a : observer.getObservedQualifiers())
                {
                if (a instanceof Syncing)
                    {
                    addType(FederatedPartitionEvent.Type.SYNCING);
                    }
                else if (a instanceof Synced)
                    {
                    addType(FederatedPartitionEvent.Type.SYNCED);
                    }
                }
            }
        }
    }
