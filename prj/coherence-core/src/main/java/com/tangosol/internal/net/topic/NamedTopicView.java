/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.net.Service;
import com.tangosol.net.TopicService;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.NamedTopicEvent;
import com.tangosol.net.topic.NamedTopicListener;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Filter;
import com.tangosol.util.Listeners;
import com.tangosol.util.ValueExtractor;

import java.util.Objects;
import java.util.Set;

import java.util.stream.Collectors;

/**
 * An implementation of a {@link NamedTopic} that uses a {@link NamedTopicConnector}
 * to connect to remote topic resources.
 *
 * @param <V>  the type of the topic values
 *
 * @author Jonathan Knight  2024.11.26
 */
@SuppressWarnings("unchecked")
public class NamedTopicView<V>
        implements NamedTopic<V>, PublisherConnector.Factory<V>, SubscriberConnector.Factory<V>
    {
    /**
     * Create a {@link NamedTopicView}.
     *
     * @param connector  the {@link NamedTopicConnector} to use
     *
     * @throws NullPointerException if the {@link NamedTopicConnector} is {@code null}
     */
    public NamedTopicView(NamedTopicConnector<V> connector)
        {
        connector.setConnectedNamedTopic(this);
        f_connector = Objects.requireNonNull(connector);
        f_sName     = connector.getName();
        f_listeners = new Listeners();
        }

    /**
     * Obtain the {@link NamedTopicConnector} being used.
     *
     * @return the {@link NamedTopicConnector} being used
     */
    public NamedTopicConnector<V> getConnector()
        {
        return f_connector;
        }

    /**
     * Dispatch an event to registered listeners.
     *
     * @param type  the type of the event
     */
    public void dispatchEvent(NamedTopicEvent.Type type)
        {
        NamedTopicEvent event = new NamedTopicEvent(this, type);
        event.dispatch(f_listeners);
        }

    // ----- NamedTopic methods ---------------------------------------------

    @Override
    public boolean isActive()
        {
        return f_connector.isActive();
        }

    @Override
    public boolean isDestroyed()
        {
        return f_connector.isDestroyed();
        }

    @Override
    public boolean isReleased()
        {
        return f_connector.isReleased();
        }

    @Override
    public TopicService getTopicService()
        {
        return f_connector.getTopicService();
        }

    @Override
    public void close()
        {
        f_connector.close();
        }

    @Override
    public Publisher<V> createPublisher(Publisher.Option<? super V>... options)
        {
        PublisherConnector<V> connector = f_connector.createPublisher(options);
        return new NamedTopicPublisher<>(this, connector, options);
        }

    @Override
    public <U> Subscriber<U> createSubscriber(Subscriber.Option<? super V, U>... options)
        {
        return f_connector.createSubscriber(options);
        }

    @Override
    public void ensureSubscriberGroup(String sGroup, Filter<?> filter, ValueExtractor<?, ?> extractor)
        {
        f_connector.ensureSubscriberGroup(sGroup, filter, extractor);
        }

    @Override
    public void destroySubscriberGroup(String sGroup)
        {
        f_connector.destroySubscriberGroup(sGroup);
        }

    @Override
    public Set<String> getSubscriberGroups()
        {
        return getTopicService().getSubscriberGroups(f_sName)
                .stream()
                .filter(id -> !id.isAnonymous())
                .map(SubscriberGroupId::getGroupName)
                .collect(Collectors.toSet());
        }

    @Override
    public int getChannelCount()
        {
        return getTopicService().getChannelCount(f_sName);
        }

    @Override
    public int getRemainingMessages(String sSubscriberGroup, int... anChannel)
        {
        return f_connector.getRemainingMessages(sSubscriberGroup, anChannel);
        }

    @Override
    public String getName()
        {
        return f_sName;
        }

    @Override
    public Service getService()
        {
        return f_connector.getTopicService();
        }

    @Override
    public void destroy()
        {
        f_connector.destroy();
        }

    @Override
    public void release()
        {
        f_connector.release();
        }

    @Override
    public void addListener(NamedTopicListener listener)
        {
        f_listeners.add(listener);
        }

    @Override
    public void removeListener(NamedTopicListener listener)
        {
        f_listeners.remove(listener);
        }

    @Override
    public PublisherConnector<V> createPublisherConnector(Publisher.Option<? super V>[] options)
        {
        return f_connector.createPublisherConnector(options);
        }

    @Override
    public <U> SubscriberConnector<U> createSubscriberConnector(Subscriber.Option<? super V, U>[] options)
        {
        return f_connector.createSubscriberConnector(options);
        }

    // ----- object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        NamedTopicView<?> that = (NamedTopicView<?>) o;
        return Objects.equals(f_connector, that.f_connector);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(f_connector);
        }

    @Override
    public String toString()
        {
        return getClass().getSimpleName()
                + "(name=" + f_sName
                + ", connector=" + f_connector
                + ")";
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link NamedTopicConnector} to use to connect to remote topic resources.
     */
    private final NamedTopicConnector<V> f_connector;

    /**
     * The name of the topic.
     */
    private final String f_sName;

    /**
     * The listeners registered with this topic.
     */
    private final Listeners f_listeners;
    }
