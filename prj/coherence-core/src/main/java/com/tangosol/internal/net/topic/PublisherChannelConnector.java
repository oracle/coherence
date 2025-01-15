/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.tangosol.net.topic.TopicDependencies;

import com.tangosol.util.Binary;

import java.util.List;

import java.util.concurrent.CompletableFuture;

import java.util.function.BiConsumer;

/**
 * A connector that connects a {@link NamedTopicPublisherChannel} to a
 * clustered topic.
 *
 * @param <V>  the type of element published to the topic
 *
 * @author Jonathan Knight  2024.11.26
 */
public interface PublisherChannelConnector<V>
    {
    /**
     * Return the channel this connector publishes to.
     *
     * @return the channel this connector publishes to
     */
    int getChannel();

    /**
     * Close this connector.
     */
    void close();

    /**
     * Return the name of the topic this connector publishes to.
     *
     * @return the name of the topic this connector publishes to
     */
    String getTopicName();

    /**
     * Ensure this connector is connected to the underlying topic.
     */
    void ensureConnected();

    /**
     * Returns {@code true} if this connector is active.
     *
     * @return {@code true} if this connector is active
     */
    boolean isActive();

    /**
     * Initialize the publisher.
     *
     * @return an opaque cookie to pass to the {@link #offer(Object, List, int, BiConsumer)} method.
     */
    @SuppressWarnings("TypeParameterExplicitlyExtendsObject")
    CompletableFuture<? extends Object> initialize();

    /**
     * Publish a value to the underlying topic.
     *
     * @param oCookie          the cookie used by the connector
     * @param listBinary       the list of binary values to publish
     * @param nNotifyPostFull  {@code true} to configure notifications for this publisher when the topic is full
     * @param handler          the handler to receive the result of the publish operation
     */
    void offer(Object oCookie, List<Binary> listBinary, int nNotifyPostFull, BiConsumer<PublishResult, Throwable> handler);

    /**
     * Perform any set-up required to retry an offer.
     *
     * @param oCookie  the cookie used by the connector.
     *
     * @return a {@link CompletableFuture} that completes when the set-up is completed
     */
    @SuppressWarnings("TypeParameterExplicitlyExtendsObject")
    CompletableFuture<? extends Object> prepareOfferRetry(Object oCookie);

    /**
     * Return the dependencies for the topic.
     *
     * @return the dependencies for the topic
     */
    TopicDependencies getTopicDependencies();
    }
