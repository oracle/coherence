/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.safeService.safeTopicService;

import com.tangosol.coherence.Component;
import com.tangosol.coherence.component.util.SafeNamedTopic;
import com.tangosol.internal.net.topic.NamedTopicSubscriber;
import com.tangosol.internal.net.topic.NamedTopicPublisher;
import com.tangosol.internal.net.topic.PublisherConnector;
import com.tangosol.internal.net.topic.SubscriberConnector;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

/**
 * A safe wrapper around a {@link com.tangosol.net.topic.NamedTopic}.
 *
 * @param <V>  the type of element in the topic
 *
 * @author Jonathan Knight  2024.11.26
 */
public class SafeSimpleNamedTopic<V>
        extends SafeNamedTopic<V>
    {
    public SafeSimpleNamedTopic()
        {
        this(null, null, true);
        }

    public SafeSimpleNamedTopic(String sName, Component compParent, boolean fInit)
        {
        super(sName, compParent, fInit);
        }

    @Override
    @SuppressWarnings("unchecked")
    public PublisherConnector<V> createPublisherConnector(Publisher.Option<? super V>[] options)
        {
        PublisherConnector.Factory<V> factory = (PublisherConnector.Factory<V>) getRunningNamedTopic();
        return factory.createPublisherConnector(options);
        }

    @Override
    @SuppressWarnings("unchecked")
    public <U> SubscriberConnector<U> createSubscriberConnector(Subscriber.Option<? super V, U>[] options)
        {
        SubscriberConnector.Factory<V> factory = (SubscriberConnector.Factory<V>) getRunningNamedTopic();
        return factory.createSubscriberConnector(options);
        }
    }
