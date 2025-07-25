/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.safeNamedTopic;

import com.tangosol.coherence.component.util.SafeNamedTopic;

import com.tangosol.internal.net.topic.NamedTopicSubscriber;
import com.tangosol.internal.net.topic.ReceiveResult;
import com.tangosol.internal.net.topic.SeekResult;
import com.tangosol.internal.net.topic.SubscriberConnector;
import com.tangosol.internal.net.topic.TopicSubscription;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;

import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.TopicDependencies;

import com.tangosol.util.Listeners;

import java.time.Instant;

import java.util.Arrays;
import java.util.Map;
import java.util.SortedSet;

import java.util.concurrent.CompletableFuture;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A safe wrapper around a {@link SubscriberConnector}.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class SafeSubscriberConnector<V>
        implements SubscriberConnector<V>
    {
    /**
     * Create a {@link SafeSubscriberConnector}.
     *
     * @param safeTopic  the {@link SafeNamedTopic}
     * @param options    the options used to configure the connector
     */
    @SuppressWarnings("rawtypes")
    public SafeSubscriberConnector(SafeNamedTopic<?> safeTopic, Subscriber.Option[] options)
        {
        f_safeTopic = safeTopic;
        f_options   = Arrays.copyOf(options, options.length);
        }

    @Override
    public void postConstruct(ConnectedSubscriber<V> subscriber)
        {
        ensureRunningConnector().postConstruct(subscriber);
        }

    @Override
    public Subscriber.Element<V> peek(int nChannel, Position position)
        {
        return ensureRunningConnector().peek(nChannel, position);
        }

    @Override
    public int getRemainingMessages(SubscriberGroupId groupId, int[] anChannel)
        {
        return ensureRunningConnector().getRemainingMessages(groupId, anChannel);
        }

    @Override
    public NamedTopicSubscriber.TopicChannel createChannel(ConnectedSubscriber<V> subscriber, int nChannel)
        {
        return ensureRunningConnector().createChannel(subscriber, nChannel);
        }

    @Override
    public boolean isCommitted(SubscriberGroupId groupId, int nChannel, Position position)
        {
        return ensureRunningConnector().isCommitted(groupId, nChannel, position);
        }

    @Override
    public void ensureConnected()
        {
        ensureRunningConnector().ensureConnected();
        }

    @Override
    public Position[] initialize(ConnectedSubscriber<V> subscriber, boolean fForceReconnect, boolean fReconnect, boolean fDisconnected)
        {
        return ensureRunningConnector().initialize(subscriber, fForceReconnect, fReconnect, fDisconnected);
        }

    @Override
    public long getConnectionTimestamp()
        {
        try
            {
            return m_connector == null ? 0L : m_connector.getConnectionTimestamp();
            }
        catch (Exception e)
            {
            return 0L;
            }
        }

    @Override
    public long getSubscriptionId()
        {
        try
            {
            return m_connector == null ? 0L : m_connector.getSubscriptionId();
            }
        catch (Exception e)
            {
            return 0L;
            }
        }

    @Override
    public SubscriberId getSubscriberId()
        {
        return ensureRunningConnector().getSubscriberId();
        }

    @Override
    public SubscriberGroupId getSubscriberGroupId()
        {
        return ensureRunningConnector().getSubscriberGroupId();
        }

    @Override
    public void close()
        {
        if (m_connector != null)
            {
            m_connector.close();
            }
        }

    @Override
    public boolean isSimple()
        {
        return ensureRunningConnector().isSimple();
        }

    @Override
    public boolean ensureSubscription(ConnectedSubscriber<V> subscriber, long subscriptionId, boolean fForceReconnect)
        {
        return ensureRunningConnector().ensureSubscription(subscriber, subscriptionId, fForceReconnect);
        }

    @Override
    public TopicSubscription getSubscription(ConnectedSubscriber<V> subscriber, long id)
        {
        return ensureRunningConnector().getSubscription(subscriber, id);
        }

    @Override
    public SortedSet<Integer> getOwnedChannels(ConnectedSubscriber<V> subscriber)
        {
        return ensureRunningConnector().getOwnedChannels(subscriber);
        }

    @Override
    public CompletableFuture<ReceiveResult> receive(ConnectedSubscriber<V> subscriber, int nChannel, Position headPosition, long lVersion, int cMaxElements, ReceiveHandler handler)
        {
        return ensureRunningConnector().receive(subscriber, nChannel, headPosition, lVersion, cMaxElements, handler);
        }

    @Override
    public CompletableFuture<Subscriber.CommitResult> commit(ConnectedSubscriber<V> subscriber, int nChannel, Position position)
        {
        return ensureRunningConnector().commit(subscriber, nChannel, position);
        }

    @Override
    public Map<Integer, Position> getTopicHeads(int[] anChannel)
        {
        return ensureRunningConnector().getTopicHeads(anChannel);
        }

    @Override
    public Map<Integer, Position> getTopicTails()
        {
        return ensureRunningConnector().getTopicTails();
        }

    @Override
    public Map<Integer, Position> getLastCommittedInGroup(SubscriberGroupId groupId)
        {
        return ensureRunningConnector().getLastCommittedInGroup(groupId);
        }

    @Override
    public TopicDependencies getTopicDependencies()
        {
        return f_safeTopic.getTopicService()
                .getTopicBackingMapManager()
                .getTopicDependencies(f_safeTopic.getTopicName());
        }

    @Override
    public void heartbeat(ConnectedSubscriber<V> subscriber, boolean fAsync)
        {
        ensureRunningConnector().heartbeat(subscriber, fAsync);
        }

    @Override
    public void closeSubscription(ConnectedSubscriber<V> subscriber, boolean fDestroyed)
        {
        if (isActive())
            {
            m_connector.closeSubscription(subscriber, fDestroyed);
            }
        }

    @Override
    public String getTypeName()
        {
        try
            {
            return m_connector.getTypeName();
            }
        catch (Exception e)
            {
            return "Unknown";
            }
        }

    @Override
    public boolean isActive()
        {
        try
            {
            return m_connector.isActive();
            }
        catch (Exception e)
            {
            return false;
            }
        }

    @Override
    public boolean isGroupDestroyed()
        {
        return m_fGroupDestroyed || (m_connector != null && m_connector.isGroupDestroyed());
        }

    @Override
    public boolean isDestroyed()
        {
        return m_fDestroyed || (m_connector != null && m_connector.isDestroyed());
        }

    @Override
    public boolean isReleased()
        {
        return m_fReleased || (m_connector != null && m_connector.isReleased());
        }

    @Override
    public void addListener(SubscriberListener listener)
        {
        f_Listeners.add(listener);
        }

    @Override
    public void removeListener(SubscriberListener listener)
        {
        f_Listeners.remove(listener);
        }

    @Override
    public Map<Integer, SeekResult> seekToPosition(ConnectedSubscriber<V> subscriber, Map<Integer, Position> map)
        {
        return ensureRunningConnector().seekToPosition(subscriber, map);
        }

    @Override
    public Map<Integer, SeekResult> seekToTimestamp(ConnectedSubscriber<V> subscriber, Map<Integer, Instant> map)
        {
        return ensureRunningConnector().seekToTimestamp(subscriber, map);
        }

    @Override
    public void onInitialized(ConnectedSubscriber<V> subscriber)
        {
        ensureRunningConnector().onInitialized(subscriber);
        }

    // ----- helper methods -------------------------------------------------

    @SuppressWarnings({"unchecked"})
    public SubscriberConnector<V> ensureRunningConnector()
        {
        SubscriberConnector<V> connector = m_connector;
        if (connector == null || !connector.isActive())
            {
            f_lock.lock();
            try
                {
                connector = m_connector;
                if (connector == null || !connector.isActive())
                    {
                    if (isReleased() || isDestroyed() || isGroupDestroyed())
                        {
                        String reason = isDestroyed() ? "was explicitly destroyed"
                                : isReleased()
                                    ? "was explicitly released"
                                    : "subscriber group explicitly was destroyed";
                        throw new IllegalStateException("SafeSubscriberConnector " + reason);
                        }

                    SubscriberConnector.Factory<V> factory = (Factory<V>) f_safeTopic;
                    connector = m_connector = factory.createSubscriberConnector(f_options);
                    connector.addListener(f_Listener);
                    }
                }
            finally
                {
                f_lock.unlock();
                }
            }
        return connector;
        }

    // ----- inner class: Listener ------------------------------------------

    protected class Listener
            implements SubscriberListener
        {
        @Override
        public void onEvent(SubscriberEvent evt)
            {
            switch (evt.getType())
                {
                case GroupDestroyed:
                    m_fGroupDestroyed = true;
                    break;
                case Destroyed:
                    m_fDestroyed = true;
                    break;
                case Released:
                    m_fReleased = true;
                    break;
                }
            SubscriberEvent event = evt.withNewSource(SafeSubscriberConnector.this);
            event.dispatch(f_Listeners);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The underlying connector this safe layer wraps.
     */
    private SubscriberConnector<V> m_connector;

    /**
     * The lock to manage state changes.
     */
    private final Lock f_lock = new ReentrantLock();

    /**
     * The {@link SubscriberListener} instances registered with this connector.
     */
    private final Listeners f_Listeners = new Listeners();

    /**
     * The {@link SubscriberListener} that receives events from the underlying connector.
     */
    private final Listener f_Listener = new Listener();

    /**
     * The {@link SafeNamedTopic}.
     */
    private final SafeNamedTopic<?> f_safeTopic;

    /**
     * The options used to configure the connector.
     */
    @SuppressWarnings("rawtypes")
    private final Subscriber.Option[] f_options;

    /**
     * A flag to indicate that this subscriber's group was destroyed.
     */
    private boolean m_fGroupDestroyed = false;

    /**
     * A flag to indicate that this subscriber's topic was released.
     */
    private boolean m_fReleased;

    /**
     * A flag to indicate that this subscriber's topic was destroyed.
     */
    private boolean m_fDestroyed;
    }
