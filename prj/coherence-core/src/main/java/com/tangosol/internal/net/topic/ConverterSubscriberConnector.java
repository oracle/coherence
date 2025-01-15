/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;

import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.TopicDependencies;

import com.tangosol.util.Binary;
import com.tangosol.util.Converter;

import java.time.Instant;

import java.util.Map;
import java.util.SortedSet;

import java.util.concurrent.CompletableFuture;

/**
 * A {@link ConverterSubscriberConnector} view an underlying {@link SubscriberConnector}
 * through a set of {@link Converter} instances.
 *
 * @param <F>  the type of element received from the underlying topic
 * @param <T>  the type of element this connector exposes
 *
 * @author Jonathan Knight  2024.11.26
 */
@SuppressWarnings("unchecked")
public class ConverterSubscriberConnector<F, T>
        implements SubscriberConnector<T>
    {
    /**
     * Constructor.
     *
     * @param connector  the underlying {@link SubscriberConnector}
     * @param topic      the parent {@link ConverterNamedTopic}
     */
    public ConverterSubscriberConnector(SubscriberConnector<F> connector, ConverterNamedTopic<F, T> topic)
        {
        this(connector, topic.getConverterUp(), topic.getConverterBinaryUp(),
                topic.getConverterDown(), topic.getConverterBinaryDown());
        }

    /**
     * Constructor.
     *
     * @param connector       the underlying {@link SubscriberConnector}
     * @param convUp          the Converter from the underlying {@link SubscriberConnector}
     * @param convBinaryUp    the converter that converts a {@link Binary} serialized in the underlying
     *                        connector's format to a {@link Binary} using the "from" serializer
     * @param convDown        the Converter to the underlying {@link SubscriberConnector}
     * @param convBinaryDown  the converter that converts a {@link Binary} serialized in the "from"
     *                        format to a {@link Binary} using the underlying connector's serializer
     */
    public ConverterSubscriberConnector(SubscriberConnector<F> connector, Converter<F, T> convUp,
            Converter<Binary, Binary> convBinaryUp, Converter<T, F> convDown, Converter<Binary, Binary> convBinaryDown)
        {
        f_connector      = connector;
        f_convUp         = convUp;
        f_convBinaryUp   = convBinaryUp;
        f_convDown       = convDown;
        f_convBinaryDown = convBinaryDown;
        }

    @Override
    public void postConstruct(ConnectedSubscriber<T> subscriber)
        {
        f_connector.postConstruct((ConnectedSubscriber<F>) subscriber);
        }

    @Override
    public Subscriber.Element<T> peek(int nChannel, Position position)
        {
        Subscriber.Element<F> element = f_connector.peek(nChannel, position);
        return new ConverterSubscriberElement<>(element, f_convUp, f_convBinaryDown);
        }

    @Override
    public int getRemainingMessages(SubscriberGroupId groupId, int[] anChannel)
        {
        return f_connector.getRemainingMessages(groupId, anChannel);
        }

    @Override
    public NamedTopicSubscriber.TopicChannel createChannel(ConnectedSubscriber<T> subscriber, int nChannel)
        {
        return f_connector.createChannel((ConnectedSubscriber<F>) subscriber, nChannel);
        }

    @Override
    public boolean isCommitted(SubscriberGroupId groupId, int nChannel, Position position)
        {
        return f_connector.isCommitted(groupId, nChannel, position);
        }

    @Override
    public void ensureConnected()
        {
        f_connector.ensureConnected();
        }

    @Override
    public Position[] initialize(ConnectedSubscriber<T> subscriber, boolean fForceReconnect, boolean fReconnect, boolean fDisconnected)
        {
        return f_connector.initialize((ConnectedSubscriber<F>) subscriber, fForceReconnect, fReconnect, fDisconnected);
        }

    @Override
    public boolean ensureSubscription(ConnectedSubscriber<T> subscriber, long subscriptionId, boolean fForceReconnect)
        {
        return f_connector.ensureSubscription((ConnectedSubscriber<F>) subscriber, subscriptionId, fForceReconnect);
        }

    @Override
    public TopicSubscription getSubscription(ConnectedSubscriber<T> subscriber, long id)
        {
        return f_connector.getSubscription((ConnectedSubscriber<F>) subscriber, id);
        }

    @Override
    public SortedSet<Integer> getOwnedChannels(ConnectedSubscriber<T> subscriber)
        {
        return f_connector.getOwnedChannels((ConnectedSubscriber<F>) subscriber);
        }

    @Override
    public CompletableFuture<ReceiveResult> receive(ConnectedSubscriber<T> subscriber, int nChannel, Position headPosition, long lVersion, ReceiveHandler handler)
        {
        return f_connector.receive((ConnectedSubscriber<F>) subscriber, nChannel, headPosition, lVersion, handler)
                .thenApply(result ->
                    {
                    if (result != null)
                        {
                        return new ConverterReceiveResult(result, f_convBinaryUp, f_convBinaryDown);
                        }
                    return null;
                    });
        }

    @Override
    public CompletableFuture<Subscriber.CommitResult> commit(ConnectedSubscriber<T> subscriber, int nChannel, Position position)
        {
        return f_connector.commit((ConnectedSubscriber<F>) subscriber, nChannel, position);
        }

    @Override
    public Map<Integer, SeekResult> seekToPosition(ConnectedSubscriber<T> subscriber, Map<Integer, Position> map)
        {
        return f_connector.seekToPosition((ConnectedSubscriber<F>) subscriber, map);
        }

    @Override
    public Map<Integer, SeekResult> seekToTimestamp(ConnectedSubscriber<T> subscriber, Map<Integer, Instant> map)
        {
        return f_connector.seekToTimestamp((ConnectedSubscriber<F>) subscriber, map);
        }

    @Override
    public Map<Integer, Position> getTopicHeads(int[] anChannel)
        {
        return f_connector.getTopicHeads(anChannel);
        }

    @Override
    public Map<Integer, Position> getTopicTails()
        {
        return f_connector.getTopicTails();
        }

    @Override
    public Map<Integer, Position> getLastCommittedInGroup(SubscriberGroupId groupId)
        {
        return f_connector.getLastCommittedInGroup(groupId);
        }

    @Override
    public TopicDependencies getTopicDependencies()
        {
        return f_connector.getTopicDependencies();
        }

    @Override
    public void heartbeat(ConnectedSubscriber<T> subscriber, boolean fAsync)
        {
        f_connector.heartbeat((ConnectedSubscriber<F>) subscriber, fAsync);
        }

    @Override
    public void closeSubscription(ConnectedSubscriber<T> subscriber, boolean fDestroyed)
        {
        f_connector.closeSubscription((ConnectedSubscriber<F>) subscriber, fDestroyed);
        }

    @Override
    public String getTypeName()
        {
        return f_connector.getTypeName();
        }

    @Override
    public boolean isActive()
        {
        return f_connector.isActive();
        }

    @Override
    public boolean isGroupDestroyed()
        {
        return f_connector.isGroupDestroyed();
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
    public void addListener(SubscriberListener listener)
        {
        f_connector.addListener(listener);
        }

    @Override
    public void removeListener(SubscriberListener listener)
        {
        f_connector.removeListener(listener);
        }

    @Override
    public void onInitialized(ConnectedSubscriber<T> subscriber)
        {
        f_connector.onInitialized((ConnectedSubscriber<F>) subscriber);
        }

    @Override
    public long getConnectionTimestamp()
        {
        return f_connector.getConnectionTimestamp();
        }

    @Override
    public long getSubscriptionId()
        {
        return f_connector.getSubscriptionId();
        }

    @Override
    public SubscriberId getSubscriberId()
        {
        return f_connector.getSubscriberId();
        }

    @Override
    public SubscriberGroupId getSubscriberGroupId()
        {
        return f_connector.getSubscriberGroupId();
        }

    @Override
    public void close()
        {
        f_connector.close();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The underlying {@link SubscriberConnector}.
     */
    private final SubscriberConnector<F> f_connector;

    /**
     * The converter to convert from the underlying topic.
     */
    private final Converter<F, T> f_convUp;

    /**
     * The converter that converts a {@link Binary} serialized in the underlying
     * connector's format to a {@link Binary} using the "from" serializer
     */
    private final Converter<Binary, Binary> f_convBinaryUp;

    /**
     * The converter to convert to the underlying topic.
     */
    private final Converter<T, F> f_convDown;

    /**
     * The converter that converts a {@link Binary} serialized in the "from"
     * format to a {@link Binary} using the underlying connector's serializer.
     */
    private final Converter<Binary, Binary> f_convBinaryDown;
    }
