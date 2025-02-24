/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.client.common.topics;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;

import com.tangosol.net.Coherence;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.NamedEventInterceptor;

import com.tangosol.net.events.internal.AbstractEventDispatcher;

import com.tangosol.net.events.partition.cache.PartitionedCacheDispatcher;

import com.tangosol.net.events.topics.TopicLifecycleEvent;
import com.tangosol.net.events.topics.TopicLifecycleEventDispatcher;

import com.tangosol.net.topic.NamedTopic;


import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A {@link TopicLifecycleEventDispatcher} used by a {@link GrpcRemoteTopicService}
 * to dispatch topic lifecycle events.
 *
 * @author Jonathan Knight  2025.01.25
 */
public class GrpcTopicLifecycleEventDispatcher
        extends AbstractEventDispatcher
        implements TopicLifecycleEventDispatcher
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link GrpcTopicLifecycleEventDispatcher}.
     *
     * @param sTopicName  the name of the topic to dispatch lifecycle events for
     * @param service     the {@link GrpcRemoteTopicService} that owns the topic
     */
    public GrpcTopicLifecycleEventDispatcher(String sTopicName, GrpcRemoteTopicService service)
        {
        super(EVENT_TYPES);
        f_sTopicName = sTopicName;
        f_service    = Objects.requireNonNull(service);
        }

    // ----- TopicLifecycleEventDispatcher methods --------------------------

    @Override
    public String getTopicName()
        {
        return f_sTopicName;
        }

    @Override
    public String getServiceName()
        {
        return f_service == null ? "" : f_service.getInfo().getServiceName();
        }

    @Override
    public String getScopeName()
        {
        return f_service == null ? Coherence.SYSTEM_SCOPE : f_service.getScopeName();
        }

    // ----- RemoteSessionDispatcher methods --------------------------------

    public void dispatchTopicCreated(NamedTopic<?> topic)
        {
        dispatchTopicEvent(TopicLifecycleEvent.Type.CREATED, topic);
        }

    public void dispatchTopicDestroyed(NamedTopic<?> topic)
        {
        dispatchTopicEvent(TopicLifecycleEvent.Type.DESTROYED, topic);
        }

    // ----- helper methods -------------------------------------------------


    public GrpcRemoteTopicService getService()
        {
        return f_service;
        }

    /**
     * Helper to perform the dispatch of a {@link TopicLifecycleEvent}
     * being given its type
     *
     * @param eventType  the enum representing the event type
     * @param topic      the related topic
     */
    protected void dispatchTopicEvent(TopicLifecycleEvent.Type eventType, NamedTopic<?> topic)
        {
        List<NamedEventInterceptor<?>> list = getInterceptorMap().get(eventType);
        if (list != null)
            {
            new GrpcTopicLifecycleEvent(this, eventType, topic).dispatch(list);
            }
        }

    // ----- inner class: AbstractEvent -------------------------------------

    /**
     * An event implementation providing
     * access to the dispatcher.
     */
    protected abstract static class AbstractEvent<T extends Enum<T>>
            extends com.tangosol.net.events.internal.AbstractEvent<T>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct an AbstractEvent with the provided dispatcher
         * and event type.
         *
         * @param dispatcher  the dispatcher that raised this event
         * @param eventType   the event type
         */
        public AbstractEvent(GrpcTopicLifecycleEventDispatcher dispatcher, T eventType)
            {
            super(dispatcher, eventType);
            }
        }

    // ----- inner class: TopicLifecycleEvent -------------------------------

    /**
     * {@link TopicLifecycleEvent} implementation raised by this dispatcher.
     */
    protected static class GrpcTopicLifecycleEvent
            extends AbstractEvent<TopicLifecycleEvent.Type>
            implements TopicLifecycleEvent
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a topic truncate event.
         *
         * @param dispatcher   the dispatcher that raised this event
         * @param eventType    the event type
         */
        protected GrpcTopicLifecycleEvent(GrpcTopicLifecycleEventDispatcher dispatcher, Type eventType, NamedTopic<?> topic)
            {
            super(dispatcher, eventType);
            f_topic = topic;
            }

        // ----- AbstractEvent methods --------------------------------------

        @Override
        protected String getDescription()
            {
            return super.getDescription() +
                   ", Session=" + getSessionName() +
                   ", Scope=" + getScopeName() +
                   ", Topic=" + getTopicName();
            }

        @Override
        public String getTopicName()
            {
            return f_topic.getName();
            }

        @Override
        public String getServiceName()
            {
            return f_topic.getService().getInfo().getServiceName();
            }

        @Override
        public String getScopeName()
            {
            return getEventDispatcher().getScopeName();
            }

        @Override
        public String getSessionName()
            {
            GrpcTopicLifecycleEventDispatcher dispatcher   = (GrpcTopicLifecycleEventDispatcher) getEventDispatcher();
            GrpcRemoteTopicService            topicService = dispatcher.getService();

            String sName = topicService
                    .getTopicBackingMapManager()
                    .getCacheFactory()
                    .getResourceRegistry()
                    .getResource(String.class, ConfigurableCacheFactorySession.SESSION_NAME);

            return sName == null ? Coherence.DEFAULT_NAME : sName;
            }

        @Override
        public TopicLifecycleEventDispatcher getEventDispatcher()
            {
            return (TopicLifecycleEventDispatcher) m_dispatcher;
            }

        @Override
        public PartitionedCacheDispatcher getDispatcher()
            {
            return null;
            }

        // overridden to make the method accessible from this dispatcher class
        @Override
        protected void dispatch(Collection<? extends EventInterceptor<?>> colIter)
            {
            super.dispatch(colIter);
            }

        // ----- data members -----------------------------------------------

        /**
         * The topic that the event is associated with.
         */
        private final NamedTopic<?> f_topic;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The event types raised by this dispatcher.
     */
    @SuppressWarnings("rawtypes")
    protected static final Set<Enum> EVENT_TYPES = new HashSet<>();

    // ----- data members ---------------------------------------------------

    /**
     * The name of the topic.
     */
    private final String f_sTopicName;

    /**
     * The {@link GrpcRemoteTopicService} owning the topic.
     */
     private final GrpcRemoteTopicService f_service;

    // ----- static initializer ---------------------------------------------

    static
        {
        EVENT_TYPES.addAll(Arrays.asList(TopicLifecycleEvent.Type.values()));
        }
    }
