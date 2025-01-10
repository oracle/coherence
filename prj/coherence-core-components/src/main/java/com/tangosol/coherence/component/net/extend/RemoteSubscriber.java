/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.net.extend;

import com.tangosol.coherence.component.net.extend.messageFactory.NamedTopicFactory;

import com.tangosol.internal.net.topic.BaseRemoteSubscriber;
import com.tangosol.internal.net.topic.SeekResult;
import com.tangosol.internal.net.topic.SimpleReceiveResult;
import com.tangosol.internal.net.topic.SubscriberConnector;
import com.tangosol.internal.net.topic.TopicSubscription;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;

import com.tangosol.net.messaging.Channel;

import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.Element;
import com.tangosol.net.topic.TopicDependencies;

import java.time.Instant;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A client side remote {@link SubscriberConnector}.
 *
 * @param <V>  the type of element the subscriber receives
 *
 * @author Jonathan Knight  2024.11.26
 */
public class RemoteSubscriber<V>
        extends BaseRemoteSubscriber<V>
        implements SubscriberConnector<V>
    {
    /**
     * Create a {@link BaseRemoteSubscriber}.
     *
     * @param sTopicName     the topic name
     * @param remoteChannel  the channel to connect to the proxy server
     * @param subscriberId   the subscriber identifier
     * @param groupId        the subscriber group identifier
     */
    public RemoteSubscriber(String sTopicName, RemoteSubscriberChannel<V> remoteChannel,
            SubscriberId subscriberId, SubscriberGroupId groupId)
        {
        super(sTopicName, subscriberId, groupId);
        f_remoteChannel = Objects.requireNonNull(remoteChannel);
        remoteChannel.setSubscriber(this);
        }

    @Override
    public boolean isActive()
        {
        Channel channel = f_remoteChannel.getChannel();
        return channel != null && channel.isOpen();
        }

    @Override
    public void ensureConnected()
        {
        f_remoteChannel.ensureChannel();
        }

    @Override
    public Position[] initialize(ConnectedSubscriber<V> subscriber, boolean fForceReconnect, boolean fReconnect, boolean fDisconnected)
        {
        Object[] aoResult = f_remoteChannel.send(NamedTopicFactory.TYPE_ID_INITIALIZE_SUBSCRIPTION, NamedTopicFactory.InitializeSubscriptionRequest.class,
                request ->
                    {
                    request.setForceReconnect(fForceReconnect);
                    request.setReconnect(fReconnect);
                    request.setDisconnected(fDisconnected);
                    });
        m_subscriptionId      = (Long) aoResult[0];
        m_connectionTimestamp = (Long) aoResult[1];
        Object[]   aoPosition = (Object[]) aoResult[2];
        Position[] aHead      = new Position[aoPosition.length];
        for (int i = 0; i < aoPosition.length; i++)
            {
            aHead[i] = (Position) aoPosition[i];
            }
        return aHead;
        }

    @Override
    public boolean ensureSubscription(ConnectedSubscriber<V> subscriber, long subscriptionId, boolean fForceReconnect)
        {
        return f_remoteChannel.send(NamedTopicFactory.TYPE_ID_ENSURE_SUBSCRIPTION, NamedTopicFactory.EnsureSubscriptionRequest.class,
                request ->
                    {
                    request.setSubscriptionId(subscriptionId);
                    request.setForceReconnect(fForceReconnect);
                    });
        }

    @Override
    public TopicSubscription getSubscription(ConnectedSubscriber<V> subscriber, long id)
        {
        return f_remoteChannel.send(NamedTopicFactory.TYPE_ID_GET_SUBSCRIPTION, NamedTopicFactory.GetSubscriptionRequest.class,
                request -> request.setSubscriptionId(id));
        }

    @Override
    public int getRemainingMessages(SubscriberGroupId groupId, int... anChannel)
        {
        return f_remoteChannel.send(NamedTopicFactory.TYPE_ID_REMAINING_MESSAGES, NamedTopicFactory.GetRemainingMessagesRequest.class,
                request ->
                    {
                    request.setSubscriberGroup(groupId.getGroupName());
                    request.setChannels(anChannel);
                    });
        }

    @Override
    public SortedSet<Integer> getOwnedChannels(ConnectedSubscriber<V> subscriber)
        {
        Collection<Integer> col = f_remoteChannel.send(NamedTopicFactory.TYPE_ID_GET_OWNED_CHANNELS);
        return new TreeSet<>(col);
        }

    @Override
    protected SimpleReceiveResult receiveInternal(int nChannel, Position headPosition, long lVersion)
        {
        return f_remoteChannel.send(NamedTopicFactory.TYPE_ID_RECEIVE, NamedTopicFactory.ReceiveRequest.class, request ->
            {
            request.setChannel(nChannel);
            request.setPosition(headPosition);
            request.setVersion(lVersion);
            });
        }

    @Override
    public Element<V> peek(int nChannel, Position position)
        {
        return f_remoteChannel.send(NamedTopicFactory.TYPE_ID_PEEK, NamedTopicFactory.PeekRequest.class, request ->
            {
            request.setChannel(nChannel);
            request.setPosition(position);
            });
        }

    @Override
    protected void commitInternal(int nChannel, Position position, CommitHandler handler)
        {
        Object[] aoResult = f_remoteChannel.send(NamedTopicFactory.TYPE_ID_COMMIT, NamedTopicFactory.CommitRequest.class, request ->
                {
                request.setChannel(nChannel);
                request.setPosition(position);
                });

        Subscriber.CommitResult result = (Subscriber.CommitResult) aoResult[NamedTopicFactory.CommitRequest.RESPONSE_ID_RESULT];
        Position                head   = (Position) aoResult[NamedTopicFactory.CommitRequest.RESPONSE_ID_HEAD];
        handler.committed(result, head);
        }

    @Override
    public boolean isCommitted(SubscriberGroupId groupId, int nChannel, Position position)
        {
        f_remoteChannel.ensureChannel();
        return f_remoteChannel.send(NamedTopicFactory.TYPE_ID_IS_COMMITTED, NamedTopicFactory.IsCommitedRequest.class, request ->
            {
            request.setChannel(nChannel);
            request.setPosition(position);
            });
        }

    @Override
    public Map<Integer, Position> getLastCommittedInGroup(SubscriberGroupId groupId)
        {
        return f_remoteChannel.send(NamedTopicFactory.TYPE_ID_GET_LAST_COMMITTED);
        }

    @Override
    public Map<Integer, Position> getTopicHeads(int[] anChannel)
        {
        return f_remoteChannel.send(NamedTopicFactory.TYPE_ID_GET_HEADS, NamedTopicFactory.GetHeadsRequest.class, request ->
            {
            request.setChannels(anChannel);
            });
        }

    @Override
    public Map<Integer, Position> getTopicTails()
        {
        return f_remoteChannel.send(NamedTopicFactory.TYPE_ID_GET_TAILS);
        }

    @Override
    public Map<Integer, SeekResult> seekToPosition(ConnectedSubscriber<V> subscriber, Map<Integer, Position> map)
        {
        List<Map.Entry<Integer, SeekResult>> list;
        list = f_remoteChannel.send(NamedTopicFactory.TYPE_ID_SEEK, NamedTopicFactory.SeekRequest.class, request ->
            {
            request.setPositions(map);
            });
        return list.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

    @Override
    public Map<Integer, SeekResult> seekToTimestamp(ConnectedSubscriber<V> subscriber, Map<Integer, Instant> map)
        {
        return f_remoteChannel.send(NamedTopicFactory.TYPE_ID_SEEK, NamedTopicFactory.SeekRequest.class, request ->
            {
            request.setTimestamps(map);
            });
        }

    @Override
    protected void sendHeartbeat(boolean fAsync)
        {
        f_remoteChannel.send(NamedTopicFactory.TYPE_ID_HEARTBEAT, NamedTopicFactory.HeartbeatRequest.class,
                request -> request.setAsync(fAsync));
        }

    @Override
    public TopicDependencies getTopicDependencies()
        {
        return f_remoteChannel.getTopicService().getTopicBackingMapManager()
                .getTopicDependencies(f_sTopicName);
        }

    @Override
    public void closeSubscription(ConnectedSubscriber<V> subscriber, boolean fDestroyed)
        {
        try
            {
            // when this is called due to certain connection error, e.g. ping
            // timeout, the channel could be null and closed.
            com.tangosol.net.messaging.Channel channel = f_remoteChannel.getChannel();
            if (channel != null)
                {
                channel.close();
                }
            }
        catch (RuntimeException e)
            {
            // ignored
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The channel to connect to the proxy server.
     */
    private final RemoteSubscriberChannel<V> f_remoteChannel;
    }
