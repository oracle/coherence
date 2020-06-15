/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events;

import java.util.Set;

/**
 * An {@link EventDispatcher} is responsible for dispatching {@link Event}s to
 * {@link EventInterceptor}s for processing.
 *
 * @author bo, nsa, rhan, mwj, rhl, hr 2011.03.29
 * @since Coherence 12.1.2
 */
public interface EventDispatcher
    {
    /**
     * Add an {@link EventInterceptor} to this dispatcher to be used to process
     * {@link Event}s. The EventInterceptor will be analyzed to determine
     * applicability to this dispatcher and an identifier will be generated
     * if not specified via an annotation. The generated identifier is the
     * fully qualified class name.
     *
     * @param interceptor  the EventInterceptor to add
     *
     * @param <E>  the Event the interceptor accepts
     */
    public <E extends Event<? extends Enum>> void addEventInterceptor(EventInterceptor<E> interceptor);

    /**
     * Add a uniquely identified {@link EventInterceptor} to this dispatcher to
     * be used to process {@link Event}s. The EventInterceptor will be analyzed
     * to determine applicability to this dispatcher.
     *
     * @param sIdentifier  the unique name of the {@link EventInterceptor} to add
     * @param interceptor  the {@link EventInterceptor} to add
     *
     * @param <E>  the Event the interceptor accepts
     */
    public <E extends Event<? extends Enum>> void addEventInterceptor(String sIdentifier, EventInterceptor<E> interceptor);

    /**
     * Add a uniquely identified {@link EventInterceptor} to this dispatcher to
     * be used to process {@link Event}s.
     *
     * @param sIdentifier  the unique name of the {@link EventInterceptor} to add
     * @param interceptor  the {@link EventInterceptor} to add
     * @param setTypes     the {@link Event} types the specified interceptor is
     *                     subscribing to, or null to subscribe to all events
     * @param fFirst       true iff the {@link EventInterceptor} should be added
     *                     to the head of this dispatcher's interceptor chain
     *
     * @param <E>  the Event the interceptor accepts
     * @param <T>  the type of events dispatched by Event {@link E}
     */
    public <T extends Enum<T>, E extends Event<T>> void addEventInterceptor(String sIdentifier,
             EventInterceptor<E> interceptor, Set<T> setTypes, boolean fFirst);

    /**
     * Remove an {@link EventInterceptor} from this dispatcher.
     *
     * @param interceptor  the EventInterceptor to be removed from the dispatcher
     *                     based on identity reference
     *
     * @param <E>  the Event the interceptor accepts
     */
    public <E extends Event<? extends Enum>> void removeEventInterceptor(EventInterceptor<E> interceptor);

    /**
     * Remove an {@link EventInterceptor} from this dispatcher.
     *
     * @param sIdentifier  the unique name identifying the EventInterceptor
     *                     to remove
     */
    public <E extends Event<? extends Enum>> void removeEventInterceptor(String sIdentifier);

    /**
     * Return the set of {@link Event} types this {@link EventDispatcher}
     * supports.
     *
     * @return the set of Event types this EventDispatcher supports
     */
    public Set<Enum> getSupportedTypes();

    // ----- inner-interface: InterceptorRegistrationEvent ------------------

    /**
     * An InterceptorRegistrationEvent allows {@link EventInterceptor}s to observe
     * other EventInterceptors being added or removed from an {@link EventDispatcher}
     * instance.
     *
     * @param <E> the {@link Event} the interceptor being un/registered will intercept
     *
     * @since Coherence 12.1.2
     *
     * @see Type
     */
    public interface InterceptorRegistrationEvent<E extends Event<? extends Enum>>
            extends Event<InterceptorRegistrationEvent.Type>
        {

        /**
         * Return the identifier the {@link EventInterceptor} was registered
         * with.
         *
         * @return the identifier the EventInterceptor was registered with
         */
        public String getIdentifier();

        /**
         * Return the Event Types the {@link EventInterceptor} being registered
         * will intercept. As this event is emitted under the scope of an {@link
         * EventDispatcher}, these event types will either be the entire set or
         * subset of {@link EventDispatcher#getSupportedTypes() supported types}
         * on the EventDispatcher.
         *
         * @return the Event Types the EventInterceptor will intercept
         */
        public Set<Enum> getEventTypes();

        /**
         * Return the {@link EventInterceptor} that is either:
         * <ol>
         *     <li>in the process of {@link Type#INSERTING registering}</li>
         *     <li>has been {@link Type#INSERTED registered}</li>
         *     <li>has been {@link Type#REMOVED removed}</li>
         * </ol>
         *
         * @return the EventInterceptor being un/registered
         */
        public EventInterceptor<E> getInterceptor();

        /**
         * Set the {@link EventInterceptor} that should be registered in place
         * of the EventInterceptor originally being registered.
         *
         * @param incptr the EventInterceptor that should be registered
         */
        public void setInterceptor(EventInterceptor<E> incptr);

        // ----- inner-enum: Type -------------------------------------------

        /**
         * The InterceptorRegistrationEvent types.
         */
        public enum Type
            {
            /**
             * An INSERTING event is raised prior to the {@link EventInterceptor}
             * being registered with the {@link EventDispatcher}. This event
             * provides an opportunity for an EventInterceptor to veto the
             * registration or change the EventInterceptor that is ultimately
             * registered with the EventDispatcher.
             */
            INSERTING,

            /**
             * An INSERTED event is raised after the {@link EventInterceptor}
             * was successfully registered with the {@link EventDispatcher}.
             * This is a post event thus the registration can not be veto'd nor
             * can {@link InterceptorRegistrationEvent#setInterceptor(EventInterceptor)
             * setInterceptor} be invoked.
             */
            INSERTED,

            /**
             * A REMOVED event is raised after the {@link EventInterceptor}
             * was successfully removed from the {@link EventDispatcher}.
             * This is a post event thus the unregistration can not be veto'd nor
             * can {@link InterceptorRegistrationEvent#setInterceptor(EventInterceptor)
             * setInterceptor} be invoked.
             */
            REMOVED
            }
        }
    }
