/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.tangosol.net.TopicService;

import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.TopicDependencies;

/**
 * A connector to connect a publisher to clustered topic resources.
 *
 * @author Jonathan Knight  2024.11.26
 */
public interface PublisherConnector<V>
    {
    /**
     * Return {@code true} if this connector is active.
     *
     * @return {@code true} if this connector is active
     */
    boolean isActive();

    /**
     * Return {@code true} if the publisher's topic has been destroyed.
     *
     * @return {@code true} if the publisher's topic has been destroyed
     */
    boolean isDestroyed();

    /**
     * Return {@code true} if the publisher's topic has been released.
     *
     * @return {@code true} if the publisher's topic has been released
     */
    boolean isReleased();

    /**
     * Return the identifier for the publisher.
     *
     * @return the identifier for the publisher
     */
    long getId();

    /**
     * Close this publisher connector.
     */
    void close();

    /**
     * Return the name of the topic being published to.
     *
     * @return the name of the topic being published to
     */
    String getTopicName();

    /**
     * Return the {@link TopicService} that manages the underlying topic.
     *
     * @return the {@link TopicService} that manages the underlying topic
     */
    TopicService getTopicService();

    /**
     * Return the dependencies for the underlying topic.
     *
     * @return the dependencies for the underlying topic
     */
    TopicDependencies getTopicDependencies();

    /**
     * Return the number of channels in the underlying topic.
     *
     * @return the number of channels in the underlying topic
     */
    int getChannelCount();

    /**
     * Ensure this connector is connected to the underlying topic.
     */
    void ensureConnected();

    /**
     * Add a {@link NamedTopicPublisher.PublisherListener} to receive events from
     * the underlying publisher.
     *
     * @param listener  the listener to add
     */
    void addListener(NamedTopicPublisher.PublisherListener listener);

    /**
     * Remove a previously added {@link NamedTopicPublisher.PublisherListener}.
     *
     * @param listener  the listener to remove
     */
    void removeListener(NamedTopicPublisher.PublisherListener listener);

    /**
     * Create a {@link PublisherChannelConnector}.
     *
     * @param nChannel the channel identifier
     * @return a {@link PublisherChannelConnector}
     */
    PublisherChannelConnector<V> createChannelConnector(int nChannel);

    /**
     * Return the maximum batch size for the subscriber.
     *
     * @return  the maximum batch size for the subscriber
     */
    long getMaxBatchSizeBytes();

    // ----- inner interface: ConnectorFactory ------------------------------

    /**
     * A factory to create {@link PublisherConnector} instances.
     * @param <V>
     */
    interface Factory<V>
        {
        /**
         * Create a {@link PublisherConnector}.
         *
         * @param options  the options to use
         *
         * @return a {@link PublisherConnector}
         */
        PublisherConnector<V> createPublisherConnector(Publisher.Option<? super V>[] options);
        }
    }
