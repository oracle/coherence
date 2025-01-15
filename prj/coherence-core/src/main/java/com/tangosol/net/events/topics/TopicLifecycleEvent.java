/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.topics;

/**
 * A TopicLifecycleEvent allows subscribers to capture events pertaining to
 * the lifecycle of a topic.
 *
 * @author Jonathan Knight  2024.11.26
 */
public interface TopicLifecycleEvent
        extends com.tangosol.net.events.Event<TopicLifecycleEvent.Type>
    {
    /**
     * Return the {@link TopicLifecycleEventDispatcher} this event was raised by.
     *
     * @return the TopicLifecycleEventDispatcher this event was raised by
     */
    public TopicLifecycleEventDispatcher getEventDispatcher();

    /**
     * The name of the topic that the event is associated with.
     *
     * @return the name of the topic that the event is associated with
     */
    public String getTopicName();

    /**
     * The name of the service that the event is associated with.
     *
     * @return the name of the service that the event is associated with {@code null}
     *         if this event is not associated with a service
     */
    public String getServiceName();

    /**
     * The scope name that this event is associated with.
     *
     * @return the scope name that this event is associated with or {@code null}
     *         if this event is not associated with a scope
     */
    public String getScopeName();

    /**
     * The optional Session name that this event is associated with.
     *
     * @return the optional Session name that this event is associated with or
     *         {@code null} if this event is not associated with a Session
     */
    public String getSessionName();

    // ----- constants ------------------------------------------------------

    /**
     * The emitted event types for a {@link TopicLifecycleEvent}.
     */
    public static enum Type
        {
        /**
         * {@link TopicLifecycleEvent}s of the type {@code CREATED} are raised
         * when the relevant data structures to support a topic are created locally.
         * <p>
         * This event can be raised on both ownership enabled and disabled members.
         * <p>
         * The event may be raised based on a "natural" call to {@link
         * com.tangosol.net.TopicService#ensureTopic(String, ClassLoader) ensureTopic},
         * or for synthetic reasons such as a member joining an existing service
         * with pre-existing topics.
         */
        CREATED,

        /**
         * {@link TopicLifecycleEvent}s of the type {@code DESTROYED} are raised
         *  when a storage for a given topic is destroyed (usually as a result
         *  of a call to {@link com.tangosol.net.topic.NamedTopic#destroy destroy}).
         */
        DESTROYED,

        /**
         * {@link TopicLifecycleEvent}s of the type {@code TRUNCATED} are raised
         * when a storage for a given topic is truncated as a result of a call
         * to NamedTopic truncate().
         * <p>
         * Truncate provides unobservable removal of all data associated to a
         * topic thus this event notifies subscribers of the execution of a
         * truncate operation, intentionally, without the associated entries.
         */
        TRUNCATED
        }
    }