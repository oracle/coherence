/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.internal;

import com.oracle.coherence.common.base.Continuation;
import com.oracle.coherence.common.base.Predicate;

import com.tangosol.net.CacheFactory;

import com.tangosol.net.events.Event;
import com.tangosol.net.events.EventDispatcher;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.annotation.Interceptor.Order;

import com.tangosol.util.Base;
import com.tangosol.util.SubSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Abstract base implementation of an {@link EventDispatcher}.
 *
 * @author rhan, mwj, nsa, rhl  2011.03.29
 * @since Coherence 12.1.2
 */
@SuppressWarnings("unchecked")
public class AbstractEventDispatcher
        implements EventDispatcher
    {
    // ------ constructors --------------------------------------------------

    /**
     * Default constructor.
     */
    public AbstractEventDispatcher()
        {
        this(new HashSet<Enum>());
        }

    /**
     * Standard constructor.
     *
     * @param setTypes  the event types supported by this dispatcher
     */
    public AbstractEventDispatcher(Set<Enum> setTypes)
        {
        f_mapInterceptors = new ConcurrentHashMap<Enum, List<NamedEventInterceptor<?>>>();

        // clone the given set of types adding events supported by the base dispatcher
        List      listTypes   = Arrays.asList(InterceptorRegistrationEvent.Type.values());
        Set<Enum> setAllTypes = new HashSet<Enum>(setTypes.size() + listTypes.size());

        setAllTypes.addAll(setTypes );
        setAllTypes.addAll(listTypes);

        f_setTypes = Collections.unmodifiableSet(setAllTypes);
        }

    // ----- EventDispatcher methods ----------------------------------------

    /**
     * {@inheritDoc}
     */
    public <E extends Event<? extends Enum>> void addEventInterceptor(EventInterceptor<E> incptr)
        {
        addEventInterceptor(null, incptr);
        }

    /**
     * {@inheritDoc}
     */
    public <T extends Enum<T>, E extends Event<T>> void addEventInterceptor(String sIdentifier,
             EventInterceptor<E> interceptor, Set<T> setTypes, boolean fFirst)
        {
        String sCacheName   = null;
        String sServiceName = null;

        if (interceptor instanceof NamedEventInterceptor)
            {
            // given a NamedEventInterceptor we retain the data held by the
            // NamedEventInterceptor but not provided by this method's signature
            NamedEventInterceptor<E> incptrNamed = (NamedEventInterceptor<E>) interceptor;

            interceptor  = incptrNamed.getInterceptor();
            sCacheName   = incptrNamed.getCacheName();
            sServiceName = incptrNamed.getServiceName();
            }

        NamedEventInterceptor<E> incptrNamed = new NamedEventInterceptor<E>(sIdentifier, interceptor,
                sCacheName, sServiceName, fFirst ? Order.HIGH : Order.LOW, null, (Set) setTypes);

        addEventInterceptor(incptrNamed);
        }

    /**
     * {@inheritDoc}
     */
    public <E extends Event<? extends Enum>> void addEventInterceptor(String sIdentifier, EventInterceptor<E> incptr)
        {
        NamedEventInterceptor<E> incptrNamed = incptr instanceof NamedEventInterceptor
                ? (NamedEventInterceptor<E>) incptr
                : new NamedEventInterceptor<E>(sIdentifier, incptr);

        if (incptrNamed.isAcceptable(this))
            {
            Set<Enum> setEventTypes = incptrNamed.getEventTypes();

            final Set<Enum> setTypes;
            if (setEventTypes == null)
                {
                // if the EventInterceptor wants all events we do not include
                // the InterceptorRegistrationEvents; the InterceptorRegistrationEvent
                // must be explicitly requested
                setTypes = f_setTypes;
                }
            else
                {
                setTypes = new SubSet(getSupportedTypes());
                setTypes.retainAll(setEventTypes);
                }

            if (!setTypes.isEmpty())
                {
                final DispatcherInterceptorEvent<E> event = instantiateEvent(
                        InterceptorRegistrationEvent.Type.INSERTING, incptrNamed, setTypes);

                // dispatch an event informing listeners of the new interceptor
                // both INSERTING and INSERTED; the pre-event provides an
                // opportunity for an EventInterceptor to change the EventInterceptor
                // being registered
                dispatchEvent(event, new Continuation()
                    {
                    public void proceed(Object o)
                        {
                        if (o instanceof Throwable)
                            {
                            CacheFactory.log("An EventInterceptor veto'd the registration of " + event.getInterceptor()
                                             + " for the event types " + event.getEventTypes());
                            throw Base.ensureRuntimeException((Throwable) o);
                            }

                        ConcurrentMap<Enum, List<NamedEventInterceptor<?>>> mapInterceptors = getInterceptorMap();

                        NamedEventInterceptor<E> incptrNamed = event.getNamedEventInterceptor();
                        for (Enum eventType : setTypes)
                            {
                            if (!mapInterceptors.containsKey(eventType))
                                {
                                mapInterceptors.putIfAbsent(eventType, new CopyOnWriteArrayList<NamedEventInterceptor<?>>());
                                }
                            List<NamedEventInterceptor<?>> listIncptr = mapInterceptors.get(eventType);

                            if (incptrNamed.isFirst())
                                {
                                listIncptr.add(0, incptrNamed);
                                }
                            else
                                {
                                listIncptr.add(incptrNamed);
                                }
                            }

                        dispatchEvent(instantiateEvent(InterceptorRegistrationEvent.Type.INSERTED,
                                                       incptrNamed, setTypes), null);
                        }
                    });
                }
            }
        }

    /**
     * {@inheritDoc}
     */
    public <E extends Event<? extends Enum>> void removeEventInterceptor(final String sName)
        {
        removeEventInterceptor(new Predicate<NamedEventInterceptor<E>>()
            {
            public boolean evaluate(NamedEventInterceptor<E> incptr)
                {
                return incptr != null && incptr.getRegisteredName().equals(sName);
                }
            });
        }

    /**
     * {@inheritDoc}
     */
    public <E extends Event<? extends Enum>> void removeEventInterceptor(final EventInterceptor<E> interceptor)
        {
        removeEventInterceptor(new Predicate<NamedEventInterceptor<E>>()
            {
            public boolean evaluate(NamedEventInterceptor<E> incptr)
                {
                return incptr != null && (incptr.getInterceptor() == interceptor || incptr == interceptor);
                }
            });
        }

    /**
     * {@inheritDoc}
     */
    public ConcurrentMap<Enum, List<NamedEventInterceptor<?>>> getInterceptorMap()
        {
        return f_mapInterceptors;
        }

    /**
     * {@inheritDoc}
     */
    public Set<Enum> getSupportedTypes()
        {
        return f_setTypes;
        }

    /**
     * Return statistics for this event dispatcher.
     *
     * @return statistics for this event dispatcher
     */
    public EventStats getStats()
        {
        return m_stats;
        }

    // ----- AbstractEventDispatcher methods --------------------------------

    /**
     * Return true iff an interceptor is subscribed to the specified event type.
     *
     * @param eventType  the event type to check against
     *
     * @return true iff an interceptor is subscribed to the event type
     */
    public boolean isSubscribed(Enum eventType)
        {
        return getInterceptorMap().containsKey(eventType);
        }

    // ----- internal methods -----------------------------------------------

    /**
     * Return a {@link Continuation} whose completion will dispatch the specified
     * {@link com.tangosol.net.events.Event} and invoke the specified continuation.
     *
     * @param event         the event to be dispatched
     * @param continuation  the continuation to complete after dispatching
     *
     * @return a continuation whose completion will dispatch the specified event
     */
    protected Continuation getDispatchContinuation(
                final AbstractEvent event, final Continuation continuation)
        {
        return new Continuation()
            {
            public void proceed(Object oResult)
                {
                try
                    {
                    List<NamedEventInterceptor<?>> list = getInterceptorMap().get(event.getType());
                    if (list != null)
                        {
                        // dispatch the event
                        event.dispatch(list);
                        }
                    }
                catch (RuntimeException e)
                    {
                    // this can only mean that event is Vetoable (see AbstractEvent#nextInterceptor)
                    if (continuation == null)
                        {
                        // preserve the original exception as the "cause"
                        throw e;
                        }
                    else
                        {
                        // the continuation is responsible for completing the control-flow
                        // (including exception handling)
                        oResult = e;
                        }
                    }
                finally
                    {
                    if (continuation != null)
                        {
                        continuation.proceed(oResult);
                        }
                    }
                }
            };
        }

    /**
     * Remove an {@link EventInterceptor} from this dispatcher. The {@link Predicate}
     * provided will be given a {@link NamedEventInterceptor} and if {@link Predicate#evaluate(Object)
     * evaluate} returns true the interceptor is removed from this dispatcher.
     *
     * @param predicate  a Predicate when given a NamedEventInterceptor can
     *                   determine whether the interceptor should be removed
     */
    protected <E extends Event<? extends Enum>> void removeEventInterceptor(Predicate<NamedEventInterceptor<E>> predicate)
        {
        Set<Enum>                setEventTypes = new HashSet<Enum>();
        NamedEventInterceptor<E> incptrNamed   = null;
        for (final Iterator iter = getInterceptorMap().entrySet().iterator(); iter.hasNext(); )
            {
            Entry entry = (Entry) iter.next();

            final List<NamedEventInterceptor<E>> listInterceptors = (List<NamedEventInterceptor<E>>) entry.getValue();
            // find the appropriate interceptor based on the predicate
            for (final NamedEventInterceptor<E> interceptor : listInterceptors)
                {
                if (predicate.evaluate(interceptor))
                    {
                    incptrNamed = interceptor;
                    setEventTypes.add((Enum) entry.getKey());

                    listInterceptors.remove(interceptor);
                    if (listInterceptors.isEmpty())
                        {
                        iter.remove();
                        }

                    break;
                    }
                }
            }

        if (incptrNamed != null && !setEventTypes.isEmpty())
            {
            // dispatch an event informing listeners of the removed interceptor
            dispatchEvent(instantiateEvent(InterceptorRegistrationEvent.Type.REMOVED,
                    incptrNamed, setEventTypes), /*continuation*/ null);
            }
        }

    /**
     * Dispatch the provided event.
     *
     * @param event  the event to dispatch
     * @param cont   the {@link Continuation} to call after the event has been
     *               delivered
     */
    protected void dispatchEvent(AbstractEvent<? extends Enum> event, Continuation<?> cont)
        {
        getDispatchContinuation(event, cont).proceed(null);
        }

    /**
     * Create an {@link InterceptorRegistrationEvent} implementation to notify
     * {@link EventInterceptor}s of an impending or enacted un/registration.
     *
     * @param eventType      the type of registration; INSERTING, INSERTED or REMOVED
     * @param incptr         the {@link NamedEventInterceptor} being registered
     * @param setEventTypes  the event types being registered against
     *
     * @param <E> the event type the interceptor is converned with
     *
     * @return an {@link InterceptorRegistrationEvent} implementation
     */
    protected <E extends Event<? extends Enum>> DispatcherInterceptorEvent<E> instantiateEvent(
              InterceptorRegistrationEvent.Type eventType, NamedEventInterceptor<E> incptr,
              Set<Enum> setEventTypes)
        {
        return new DispatcherInterceptorEvent<E>(this, eventType, incptr, setEventTypes);
        }

    // ----- inner class: DispatcherInterceptorEvent ------------------------

    /**
     * An {@link InterceptorRegistrationEvent} implementation allowing interception
     * of {@link EventInterceptor} un/registrations.
     *
     * @param <E> the {@link Event} the interceptor being un/registered will intercept
     *
     * @since Coherence 12.1.2
     */
    public static class DispatcherInterceptorEvent<E extends Event<? extends Enum>>
            extends AbstractEvent<InterceptorRegistrationEvent.Type>
            implements InterceptorRegistrationEvent<E>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a DispatcherInterceptorEvent using the specified type.
         *
         * @param dispatcher     the event dispatcher that raised this event
         * @param eventType      the type of {@link Event} raised
         * @param incptr         the {@link NamedEventInterceptor} being un/registered
         * @param setEventTypes  the event types the interceptor is being registered
         *                       against
         */
        public DispatcherInterceptorEvent(EventDispatcher dispatcher, InterceptorRegistrationEvent.Type eventType,
                                          NamedEventInterceptor<E> incptr, Set<Enum> setEventTypes)
            {
            super(dispatcher, eventType);

            assert incptr != null;
            m_incptr        = incptr;
            f_setEventTypes = setEventTypes;
            }

        // ----- AbstractEvent methods --------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean isMutableEvent()
            {
            return getType() == Type.INSERTING;
            }

        // ----- DispatcherInterceptorEvent methods -------------------------

        /**
         * {@inheritDoc}
         */
        public String getIdentifier()
            {
            return m_incptr.getRegisteredName();
            }

        /**
         * {@inheritDoc}
         */
        public Set<Enum> getEventTypes()
            {
            return f_setEventTypes;
            }

        /**
         * {@inheritDoc}
         */
        public EventInterceptor<E> getInterceptor()
            {
            return m_incptr.getInterceptor();
            }

        /**
         * {@inheritDoc}
         */
        public void setInterceptor(EventInterceptor<E> incptr)
            {
            if (isMutableEvent())
                {
                m_incptr = new NamedEventInterceptor<E>(incptr, m_incptr);
                }
            else
                {
                throw new IllegalStateException("Modifying the interceptor is not permitted for "
                        + getType() + " events");
                }
            }

        // ----- helpers ----------------------------------------------------

        /**
         * Return the {@link NamedEventInterceptor} this event was created with.
         *
         * @return the NamedEventInterceptor this event was created with
         */
        protected NamedEventInterceptor<E> getNamedEventInterceptor()
            {
            return m_incptr;
            }

        // ----- data members -----------------------------------------------

        /**
         * Set of event types the {@link EventInterceptor} is being registered
         * against pertinent to this {@link EventDispatcher}.
         */
        protected final Set<Enum> f_setEventTypes;

        /**
         * The {@link NamedEventInterceptor}, which wraps the user's {@link
         * EventInterceptor}, being registered with this {@link EventDispatcher}.
         */
        protected NamedEventInterceptor<E> m_incptr;
        }

    // ----- inner class: EventStats ----------------------------------------

    /**
     * EventStats is used to track statistics related to {@link Event} dispatching.
     *
     * @author pp  2011.10.12
     */
    public class EventStats
        {
        // ----- constructors -----------------------------------------------

        /**
         * EventStats constructor.
         */
        public EventStats()
            {
            }

        // ----- public methods ---------------------------------------------

        /**
         * Register the raising of an exception by an {@link EventInterceptor}.
         *
         * @param e            the exception raised
         * @param event        the event that was dispatched
         * @param interceptor  the interceptor that raised the exception
         */
        public void registerEventException(Exception e, Event event, EventInterceptor interceptor)
            {
            m_cExceptions++;
            m_sStackTrace = new Date() + "\n" + Base.printStackTrace(e);
            }

        /**
         * Reset the event statistics.
         */
        public void reset()
            {
            m_cExceptions = 0;
            m_sStackTrace = null;
            }

        /**
         * Build and return an array of strings to display the event statistics.
         *
         * @return  an array of strings to display event statistics
         */
        public String[] toStringArray()
            {
            List<String> listStats           = new ArrayList<String>();
            Set<String>  setInterceptorNames = new HashSet<String>();

            for (List<NamedEventInterceptor<?>> listInterceptor : getInterceptorMap().values())
                {
                for (NamedEventInterceptor interceptor : listInterceptor)
                    {
                    setInterceptorNames.add(interceptor.getRegisteredName());
                    }
                }

            listStats.add("Interceptors: " + setInterceptorNames);
            listStats.add("ExceptionCount: " + m_cExceptions);

            String sStackTrace = m_sStackTrace;
            listStats.add("LastException: " + (sStackTrace == null ? "" : sStackTrace));

            return listStats.toArray(new String[listStats.size()]);
            }

        // ----- object methods ---------------------------------------------

        /**
         * {@inheritDoc}
         */
        public String toString()
            {
            StringBuilder builder   = new StringBuilder();
            String[]      asDisplay = toStringArray();

            for (String s : asDisplay)
                {
                builder.append(s).append('\n');
                }

            return builder.toString();
            }

        // ----- data members ---------------------------------------------------

        /**
         * Number of exceptions thrown since the last statistics reset.
         */
        protected volatile int m_cExceptions;

        /**
         * The stacktrace of the last exception thrown.
         */
        protected volatile String m_sStackTrace;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The set of {@link Event} types that this {@link EventDispatcher} may raise.
     */
    protected final Set<Enum> f_setTypes;

    /**
     * A map of registered {@link EventInterceptor}s keyed by event type.
     */
    protected final ConcurrentMap<Enum, List<NamedEventInterceptor<?>>> f_mapInterceptors;

    /**
     * Statistics for this event dispatcher.
     */
    protected final EventStats m_stats = new EventStats();
    }
