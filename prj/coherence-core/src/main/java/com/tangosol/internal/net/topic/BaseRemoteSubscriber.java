/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.net.topic.NamedTopicSubscriber.TopicChannel;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;

import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.CommitResult;

import com.tangosol.util.Listeners;
import com.tangosol.util.TaskDaemon;

import java.util.concurrent.CompletableFuture;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A base class for a client side remote {@link SubscriberConnector}.
 *
 * @param <V>  the type of element the subscriber receives
 *
 * @author Jonathan Knight  2025.01.01
 */
public abstract class BaseRemoteSubscriber<V>
        implements SubscriberConnector<V>
    {
    /**
     * Create a {@link BaseRemoteSubscriber}.
     *
     * @param sTopicName    the topic name
     * @param subscriberId  the subscriber identifier
     * @param groupId       the subscriber group identifier
     */
    protected BaseRemoteSubscriber(String sTopicName, SubscriberId subscriberId, SubscriberGroupId groupId)
        {
        f_sTopicName        = sTopicName;
        f_subscriberId      = subscriberId;
        f_subscriberGroupId = groupId;
        }

    @Override
    public void postConstruct(ConnectedSubscriber<V> subscriber)
        {
        }

    @Override
    public void addListener(SubscriberListener listener)
        {
        f_listeners.add(listener);
        }

    @Override
    public void removeListener(SubscriberListener listener)
        {
        f_listeners.remove(listener);
        }

    @Override
    public boolean isGroupDestroyed()
        {
        return m_fGroupDestroyed;
        }

    @Override
    public boolean isDestroyed()
        {
        return m_fDestroyed;
        }

    @Override
    public boolean isReleased()
        {
        return m_fReleased;
        }

    @Override
    public TopicChannel createChannel(ConnectedSubscriber<V> subscriber, int nChannel)
        {
        return new RemoteChannel(nChannel);
        }

    @Override
    public long getConnectionTimestamp()
        {
        return m_connectionTimestamp;
        }

    @Override
    public long getSubscriptionId()
        {
        return m_subscriptionId;
        }

    @Override
    public CompletableFuture<ReceiveResult> receive(ConnectedSubscriber<V> subscriber, int nChannel,
            Position headPosition, long lVersion, int cMaxElements, ReceiveHandler handler)
        {
        CompletableFuture<ReceiveResult> future = new CompletableFuture<>();
        try
            {
            SimpleReceiveResult result = receiveInternal(nChannel, headPosition, lVersion, cMaxElements);
            handler.onReceive(lVersion, result, null, null);
            subscriber.setChannelHeadIfHigher(nChannel, result.getHead());
            future.complete(result);
            }
        catch (Throwable t)
            {
            try
                {
                handler.onReceive(lVersion, null, t, null);
                }
            catch (Exception e)
                {
                Logger.err(e);
                }
            future.completeExceptionally(t);
            }
        return future;
        }

    /**
     * Send a receive request to the proxy.
     *
     * @param nChannel      the channel identifier
     * @param headPosition  the heap position of the channel
     * @param lVersion      the channel version
     * @param cMaxElements  the maximum number of elements to return
     *
     * @return the result of the receive request
     */
    protected abstract SimpleReceiveResult receiveInternal(int nChannel, Position headPosition, long lVersion, int cMaxElements);

    @Override
    public CompletableFuture<CommitResult> commit(ConnectedSubscriber<V> subscriber, int nChannel, Position position)
        {
        CompletableFuture<CommitResult> future = new CompletableFuture<>();
        try
            {
            commitInternal(nChannel, position, (result, head) ->
                {
                subscriber.setChannelHeadIfHigher(nChannel, head);
                future.complete(result);
                });
            }
        catch (Throwable t)
            {
            future.completeExceptionally(t);
            }
        return future;
        }

    /**
     * Send a commit request to the proxy.
     *
     * @param nChannel  the channel to commit
     * @param position  the position to commit
     * @param handler   the {@link CommitHandler} to receive the results
     */
    protected abstract void commitInternal(int nChannel, Position position, CommitHandler handler);

    @Override
    public void heartbeat(ConnectedSubscriber<V> subscriber, boolean fAsync)
        {
        if (f_subscriberGroupId.isDurable())
            {
            sendHeartbeat(fAsync);
            }
        }

    protected abstract void sendHeartbeat(boolean fAsync);

    @Override
    public String getTypeName()
        {
        return getClass().getSimpleName();
        }

    @Override
    public void onInitialized(ConnectedSubscriber<V> subscriber)
        {
        }

    /**
     * Return the {@link SubscriberId} for this subscriber.
     *
     * @return the {@link SubscriberId} for this subscriber
     */
    public SubscriberId getSubscriberId()
        {
        return f_subscriberId;
        }

    /**
     * Return the {@link SubscriberGroupId} for this subscriber.
     *
     * @return the {@link SubscriberGroupId} for this subscriber
     */
    public SubscriberGroupId getSubscriberGroupId()
        {
        return f_subscriberGroupId;
        }

    @Override
    public void close()
        {
        if (m_daemon != null)
            {
            m_daemon.stop();
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Obtain the task daemon.
     *
     * @return the task daemon
     */
    protected TaskDaemon ensureTaskDaemon()
        {
        TaskDaemon daemon = m_daemon;
        if (daemon == null)
            {
            f_lock.lock();
            try
                {
                daemon = m_daemon;
                if (daemon == null)
                    {
                    daemon = m_daemon = new TaskDaemon(getClass().getSimpleName() + ":"
                            + f_sTopicName + ":" + f_subscriberId.getId());
                    }
                }
            finally
                {
                f_lock.unlock();
                }
            }
        return daemon;
        }

    public void onDisconnected()
        {
        m_subscriptionId = 0L;
        dispatchEvent(new SubscriberEvent(this, SubscriberEvent.Type.Disconnected));
        }

    /**
     * Dispatch a {@link SubscriberEvent}.
     *
     * @param event  the event to dispatch
     */
    public void dispatchEvent(SubscriberEvent event)
        {
        switch (event.getType())
            {
            case GroupDestroyed:
                m_fGroupDestroyed = true;
                m_subscriptionId  = 0L;
                break;
            case Destroyed:
                m_fDestroyed     = true;
                m_subscriptionId = 0L;
                break;
            case Released:
                m_fReleased      = true;
                m_subscriptionId = 0L;
                break;
            }
        event.dispatch(f_listeners);
        }

    // ----- inner class: CommitHandler -------------------------------------

    /**
     * A handler for commit results.
     */
    public interface CommitHandler
        {
        void committed(CommitResult result, Position head);
        }

    // ----- inner class: RemoteChannel -------------------------------------

    /**
     * Channel is a data structure which represents the state of a channel as known
     * by this subscriber.
     */
    public static class RemoteChannel
            extends TopicChannel
            implements Subscriber.Channel
        {
        public RemoteChannel(int nChannel)
            {
            m_head     = Position.EMPTY_POSITION;
            f_nChannel = nChannel;
            }

        @Override
        public int getId()
            {
            return f_nChannel;
            }

        // ----- Object methods ---------------------------------------------

        public String toString()
            {
            return "Channel=" + f_nChannel +
                    ", owned=" + m_fOwned +
                    ", empty=" + m_fEmpty +
                    ", version=" + m_lVersion.get() +
                    ", head=" + m_head +
                    ", polls=" + m_cPolls +
                    ", received=" + m_cReceived.getCount() +
                    ", committed=" + m_cCommited +
                    ", first=" + m_firstPolled +
                    ", firstTimestamp=" + m_firstPolledTimestamp +
                    ", last=" + m_lastPolled +
                    ", lastTimestamp=" + m_lastPolledTimestamp +
                    ", contended=" + m_fContended;
            }

        // ----- data members -----------------------------------------------

        private final int f_nChannel;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of the topic.
     */
    protected final String f_sTopicName;

    /**
     * The subscriber identifier.
     */
    protected final SubscriberId f_subscriberId;

    /**
     * The subscriber group identifier.
     */
    protected final SubscriberGroupId f_subscriberGroupId;

    /**
     * The daemon to use to complete async tasks.
     */
    protected TaskDaemon m_daemon;

    /**
     * The lock to synchronize state.
     */
    protected final Lock f_lock = new ReentrantLock();

    /**
     * The registered {@link SubscriberListener} instances.
     */
    protected final Listeners f_listeners = new Listeners();

    /**
     * A flag to indicate that this subscriber's group was destroyed.
     */
    protected boolean m_fGroupDestroyed = false;

    /**
     * A flag to indicate that this subscriber's topic was released.
     */
    protected boolean m_fReleased;

    /**
     * A flag to indicate that this subscriber's topic was destroyed.
     */
    protected boolean m_fDestroyed;

    /**
     * The unique identifier for the subscriber group.
     */
    protected long m_subscriptionId;

    /**
     * The subscriber's connection timestamp.
     */
    protected volatile long m_connectionTimestamp;
    }
