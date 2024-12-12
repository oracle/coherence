/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.topics;

import com.tangosol.net.events.EventDispatcher;

/**
 * A TopicLifecycleEventDispatcher raises {@link TopicLifecycleEvent}s.
 *
 * @author Jonathan Knight  2024.11.26
 */
public interface TopicLifecycleEventDispatcher
        extends EventDispatcher
    {
    /**
     * Return the name of the {@link com.tangosol.net.topic.NamedTopic topic}
     * that this dispatcher is associated with.
     *
     * @return  the topic name
     */
    public String getTopicName();

    /**
     * Return the optional name of the {@link com.tangosol.net.TopicService service}
     * that this dispatcher is associated with.
     *
     * @return  the service name that this dispatcher is associated with or {@code null}
     *          if this dispatcher is not associated with a topic service.
     */
    public String getServiceName();

    /**
     * Return the optional scope name that this dispatcher is associated with.
     *
     * @return  the scope name that this dispatcher is associated with or {@code null}
     *          if this dispatcher is not associated with a scope.
     */
    public String getScopeName();
    }
