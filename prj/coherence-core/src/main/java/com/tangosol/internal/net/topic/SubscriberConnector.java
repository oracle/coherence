/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.tangosol.internal.net.topic.NamedTopicSubscriber.TopicChannel;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberInfo;

import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.TopicDependencies;

import com.tangosol.util.Binary;
import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;

import java.time.Instant;

import java.util.Map;
import java.util.SortedSet;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import java.util.function.Consumer;

/**
 * A connector to connect to server side subscriber.
 *
 * @author Jonathan Knight  2024.11.26
 */
public interface SubscriberConnector<V>
    {
    /**
     * Called by the subscriber at the end of construction.
     *
     * @param subscriber  the subscriber
     */
    void postConstruct(ConnectedSubscriber<V> subscriber);

    /**
     * Peek at the specified topic position.
     *
     * @param nChannel  the channel to peek at
     * @param position  the position to peek
     *
     * @return the element in the topic
     */
    Subscriber.Element<V> peek(int nChannel, Position position);

    /**
     * Obtain the remaining messages for the specified channels.
     *
     * @param groupId    the subscriber group
     * @param anChannel  the channels (or null or empty array for all channels)
     *
     * @return the remaining messages
     */
    int getRemainingMessages(SubscriberGroupId groupId, int[] anChannel);

    /**
     * Create a topic channel instance.
     *
     * @param subscriber  the {@link ConnectedSubscriber}
     * @param nChannel    the identifier for the channel to create
     *
     * @return a topic channel instance
     */
    TopicChannel createChannel(ConnectedSubscriber<V> subscriber, int nChannel);

    /**
     * Returns {@code true} if the specified {@link Position} has been committed in the specified
     * channel.
     * <p>
     * If the channel parameter is not a channel owned by this subscriber then even if the result
     * returned is {@code false}, the position could since have been committed by the owning subscriber.
     *
     * @param groupId   the subscriber group identifier
     * @param nChannel  the channel
     * @param position  the position within the channel to check
     *
     * @return {@code true} if the specified {@link Position} has been committed in the
     * specified channel, or {@code false} if the position is not committed or
     * this subscriber
     */
    boolean isCommitted(SubscriberGroupId groupId, int nChannel, Position position);

    /**
     * Ensure the subscriber is connected to the underlying resources.
     */
    void ensureConnected();

    /**
     * Create the subscription with the underlying topic.
     *
     * @param subscriber       the subscriber to initialize
     * @param fForceReconnect  A flag to indicate that the reconnect logic should force a reconnect
     *                         request even if the subscriber is in the config map
     * @param fReconnect       this is a reconnection of an existing subscriber
     * @param fDisconnected    {@code true} if the subscriber was initially disconnected
     *
     *
     * @return  the head positions
     */
    Position[] initialize(ConnectedSubscriber<V> subscriber, boolean fForceReconnect, boolean fReconnect, boolean fDisconnected);

    /**
     * Ensure the subscription exists.
     *
     * @param subscriber       the subscriber to ensure
     * @param subscriptionId   the subscription identifier
     * @param fForceReconnect  {@code true} to force a subscriber reconnect
     * @return {@code} true if the subscription exist
     */
    boolean ensureSubscription(ConnectedSubscriber<V> subscriber, long subscriptionId, boolean fForceReconnect);

    /**
     * Return the {@link TopicSubscription} for the specified identifier.
     *
     * @param subscriber  the subscriber
     * @param id          the subscription identifier
     *
     * @return the {@link TopicSubscription} for the specified identifier,
     * or {@code null} if the subscription does not exist
     */
    TopicSubscription getSubscription(ConnectedSubscriber<V> subscriber, long id);

    /**
     * Obtain the set of channels owned by a subscription.
     *
     * @param subscriber the subscriber
     * @return the channels owned by the subscription
     */
    SortedSet<Integer> getOwnedChannels(ConnectedSubscriber<V> subscriber);

    /**
     * Perform a receive operation.
     *
     * @param subscriber    the subscriber
     * @param nChannel      the channel to poll
     * @param headPosition  the head position of the channel
     * @param lVersion      the channel version
     * @param cMaxElements  the maximum number of elements to return
     * @param handler       the response handler
     *
     * @return a {@link CompletableFuture} that will complete with the completion of the receive operation
     */
    CompletableFuture<ReceiveResult> receive(ConnectedSubscriber<V> subscriber,
            int nChannel, Position headPosition, long lVersion, int cMaxElements, ReceiveHandler handler);

    /**
     * Asynchronously Commit a position.
     *
     * @param subscriber  the subscriber
     * @param nChannel    the channel to commit
     * @param position    the position in the channel to commit
     * @return a {@link CompletableFuture} that is completed with the result of the commit operation
     */
    CompletableFuture<Subscriber.CommitResult> commit(ConnectedSubscriber<V> subscriber, int nChannel, Position position);

    /**
     * Return the head positions for a set of channels in the topic that this
     * connector is connected to.
     *
     * @param anChannel  the channels to obtain the head positions for
     *
     * @return a map of channel and the head position in that channel
     */
    Map<Integer, Position> getTopicHeads(int[] anChannel);

    /**
     * Return the current tail positions for the topic this connector is connected to.
     *
     * @return a map of channel and the current tail positions for the
     *         channel in the topic
     */
    Map<Integer, Position> getTopicTails();

    /**
     * Return a map of channels and the last committed position for the channel
     * in a specific subscriber group.
     *
     * @param groupId  the subscriber group identifier
     *
     * @return a map of channels and the last committed position for the channel
     *         in the subscriber group
     */
    Map<Integer, Position> getLastCommittedInGroup(SubscriberGroupId groupId);

    /**
     * Return the dependencies for the topic.
     *
     * @return the dependencies for the topic
     */
    TopicDependencies getTopicDependencies();

    /**
     * If this is not an anonymous subscriber send a heartbeat to the server.
     *
     * @param subscriber  the subscriber
     * @param fAsync      {@code true} to invoke the heartbeat processor asynchronously
     */
    void heartbeat(ConnectedSubscriber<V> subscriber, boolean fAsync);

    /**
     * Close a subscription.
     *
     * @param subscriber the subscriber identifier
     * @param fDestroyed {@code true} if the subscription is destroyed
     */
    void closeSubscription(ConnectedSubscriber<V> subscriber, boolean fDestroyed);

    /**
     * Return the descriptive name for this connector.
     *
     * @return the descriptive name for this connector
     */
    String getTypeName();

    /**
     * Return {@code true} if this connector is active.
     *
     * @return {@code true} if this connector is active
     */
    boolean isActive();

    /**
     * Return {@code true} if the subscriber group has been destroyed.
     *
     * @return {@code true} if the subscriber group has been destroyed
     */
    boolean isGroupDestroyed();

    /**
     * Return {@code true} if the subscriber's topic has been destroyed.
     *
     * @return {@code true} if the subscriber's topic has been destroyed
     */
    boolean isDestroyed();

    /**
     * Return {@code true} if the subscriber's topic has been released.
     *
     * @return {@code true} if the subscriber's topic has been released
     */
    boolean isReleased();

    /**
     * Add a {@link SubscriberListener} to this connector.
     *
     * @param listener  the listener to add
     */
    void addListener(SubscriberListener listener);

    /**
     * Remove a {@link SubscriberListener} from this connector.
     *
     * @param listener  the listener to remove
     */
    void removeListener(SubscriberListener listener);

    /**
     * Move the head of the specified channel to a new position.
     *
     * @param subscriber  the subscriber
     * @param map         the map of channel to position within the channel to seek to
     *
     * @return the map of channel to result of the seek operation
     */
    Map<Integer, SeekResult> seekToPosition(ConnectedSubscriber<V> subscriber, Map<Integer, Position> map);

    /**
     * Obtain the next position in the channel after the specified timestamp.
     *
     * @param subscriber  the subscriber
     * @param map         the map of channel to time instant within the channel to seek to
     *
     * @return the map of channel to result of the seek operation
     */
    Map<Integer, SeekResult> seekToTimestamp(ConnectedSubscriber<V> subscriber, Map<Integer, Instant> map);

    /**
     * Indicate to the connector that the subscriber has been intialized.
     */
    void onInitialized(ConnectedSubscriber<V> subscriber);

    /**
     * Return the subscriber's connection timestamp.
     *
     * @return the subscriber's connection timestamp
     */
    long getConnectionTimestamp();

    /**
     * Return the unique identifier for the subscriber group.
     *
     * @return  the unique identifier for the subscriber group
     */
    long getSubscriptionId();

    /**
     * Return the unique identifier for the subscriber.
     *
     * @return the unique identifier for the subscriber
     */
    SubscriberId getSubscriberId();

    /**
     * Return the subscriber group identifier.
     *
     * @return  the subscriber group identifier
     */
    SubscriberGroupId getSubscriberGroupId();

    /**
     * Close the connector.
     */
    void close();

    // ----- inner interface: Factory ---------------------------------------

    /**
     * A factory that can create a {@link SubscriberConnector}.
     *
     * @param <V>  the type of elements being subscribed to
     */
    interface Factory<V>
        {
        /**
         * Create a {@link SubscriberConnector}.
         *
         * @return a {@link SubscriberConnector}
         */
        <U> SubscriberConnector<U> createSubscriberConnector(Subscriber.Option<? super V, U>[] options);
        }

    // ----- inner interface: ConnectedSubscriber ---------------------------

    /**
     * A subscriber that uses a {@link SubscriberConnector}.
     */
    interface ConnectedSubscriber<V>
        {
        /**
         * Close the connected subscriber.
         */
        void close();

        /**
         * Return the {@link SubscriberId subscriber identifier}.
         *
         * @return  the {@link SubscriberId subscriber identifier}
         */
        SubscriberId getSubscriberId();

        /**
         * Return the subscriber group identifier.
         *
         * @return the {@link SubscriberGroupId subscriber group identifier}
         */
        SubscriberGroupId getSubscriberGroupId();

        /**
         * Return the {@link SubscriberInfo.Key subscriber key}.
         *
         * @return the {@link SubscriberInfo.Key subscriber key}
         */
        SubscriberInfo.Key getKey();

        /**
         * Returns the subscriber's notification identifier.
         *
         * @return the subscriber's notification identifier
         */
        int getNotificationId();

        /**
         * Return the subscription identifier.
         *
         * @return the subscription identifier
         */
        long getSubscriptionId();

        /**
         * Return the optional subscriber group filter.
         *
         * @return the optional subscriber group filter
         */
        Filter<?> getFilter();

        /**
         * Return the optional subscriber group converter.
         *
         * @return the optional subscriber group converter
         */
        ValueExtractor<?,?> getConverter();

        /**
         * Return the {@link Executor} to use for async operations.
         *
         * @return the {@link Executor} to use for async operations
         */
        Executor getExecutor();

        /**
         * Update the specified channel under the channel lock.
         *
         * @param nChannel  the channel to update
         * @param fn        the function to apply to update the channel
         */
        void updateChannel(int nChannel, Consumer<TopicChannel> fn);

        /**
         * Return the connection timestamp.
         *
         * @return  the connection timestamp
         */
        long getConnectionTimestamp();

        /**
         * Return the {@link SubscriberConnector} used to connect this subscriber.
         *
         * @return the {@link SubscriberConnector} used to connect this subscriber
         */
        SubscriberConnector<V> getConnector();

        /**
         * Set the head position for a channel.
         *
         * @param nChannel  the channel identifier
         * @param head      the new head position
         */
        void setChannelHeadIfHigher(int nChannel, Position head);

        /**
         * Perform an asynchronous commit of a channel.
         *
         * @param nChannel  the channel to commit
         * @param position  the position within the channel to commit
         *
         * @return the result of the commit operation
         */
        CompletableFuture<Subscriber.CommitResult> commitAsync(int nChannel, Position position);

        /**
         * Obtain the current head of a channel as currently known by the local
         * state of ths subscriber.
         *
         * @param nChannel  the channel to obtain the head for
         *
         * @return the current head of a channel as currently known by the local
         *         state of ths subscriber
         */
        Position getChannelHead(int nChannel);

        /**
         * Asynchronously poll the topic for a message
         *.
         * @param nChannel  the channel to poll
         * @param cMaxElements  the maximum number of elements to receive
         * @param handler   the {@link SubscriberConnector.ReceiveHandler} to handle the result
         *
         * @return a {@link CompletableFuture} that will complete with the result of the poll
         */
        CompletableFuture<ReceiveResult> receive(int nChannel, int cMaxElements, SubscriberConnector.ReceiveHandler handler);

        /**
         * Update a channel that has been repositioned by a seek operation.
         *
         * @param nChannel  the channel to reposition
         * @param result    the result of the seek operation
         *
         * @return the position seeked to
         */
        Position updateSeekedChannel(int nChannel, SeekResult result);

        /**
         * Create a subscriber element.
         *
         * @param binary    the binary element
         * @param nChannel  the channel
         * @return
         */
        Subscriber.Element<V> createElement(Binary binary, int nChannel);
        }
    
    // ----- inner interface ReceiveHandler ---------------------------------

    /**
     * A handler to process receive operation callbacks.
     */
    @FunctionalInterface
    public interface ReceiveHandler
        {
        /**
         * Process the result of a receive operation.
         *
         * @param lVersion     the channel version
         * @param result       the {@link ReceiveResult}
         * @param error        any error that occurred
         * @param continuation the continuation to execute
         */
        void onReceive(long lVersion, ReceiveResult result, Throwable error, Continuation continuation);
        }

    // ----- inner class: Continuation --------------------------------------

    /**
     * A continuation to be invoked by the receive handler.
     */
    @FunctionalInterface
    interface Continuation
        {
        /**
         * The continuation function.
         */
        void onContinue();
        }

    // ----- inner class: SubscriberListener ---------------------------------

    /**
     * A listener that receives events related to a subscriber.
     */
    interface SubscriberListener
            extends BaseTopicEvent.Listener<SubscriberEvent>
        {
        /**
         * Receives {@link SubscriberEvent events}.
         *
         * @param evt the {@link SubscriberEvent events} from the subscriber
         */
        void onEvent(SubscriberEvent evt);
        }

    // ----- inner class: SubscriberEvent -----------------------------------

    /**
     * An event related to a subscriber.
     */
    class SubscriberEvent
            extends BaseTopicEvent<SubscriberConnector<?>, SubscriberEvent.Type, SubscriberListener>
        {
        /**
         * Create a {@link SubscriberEvent}.
         *
         * @param source  the event source
         * @param type    the type of the event
         */
        public SubscriberEvent(SubscriberConnector source, Type type)
            {
            this(source, type, null, null);
            }

        /**
         * Create a {@link SubscriberEvent}.
         *
         * @param source     the event source
         * @param type       the type of the event
         * @param anChannel  the channels associated with the event
         */
        public SubscriberEvent(SubscriberConnector source, Type type, int[] anChannel)
            {
            this(source, type, anChannel, null);
            }

        /**
         * Create a {@link SubscriberEvent}.
         *
         * @param source      the event source
         * @param type        the type of the event
         * @param setChannel  the channels associated with the event
         */
        public SubscriberEvent(SubscriberConnector source, Type type, SortedSet<Integer> setChannel)
            {
            this(source, type, null, setChannel);
            }

        /**
         * Create a {@link SubscriberEvent}.
         *
         * @param source      the event source
         * @param type        the type of the event
         * @param anChannel   the channels associated with the event
         * @param setChannel  the channels associated with the event
         */
        public SubscriberEvent(SubscriberConnector source, Type type, int[] anChannel, SortedSet<Integer> setChannel)
            {
            super(source, type);
            m_setChannel = setChannel;
            m_anChannel  = anChannel;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Obtain the channels this event applies to.
         *
         * @return the channels this event applies to
         */
        public int[] getPopulatedChannels()
            {
            return m_anChannel;
            }

        /**
         * Obtain the channels this event applies to.
         *
         * @return the channels this event applies to
         */
        public SortedSet<Integer> getAllocatedChannels()
            {
            return m_setChannel;
            }

        /**
         * Return a copy of this event with an updated source.
         *
         * @param source  the new event source
         *
         * @return  a copy of this event with an updated source
         */
        @SuppressWarnings("rawtypes")
        public SubscriberEvent withNewSource(SubscriberConnector source)
            {
            return new SubscriberEvent(source, m_type, m_anChannel, m_setChannel);
            }

        // ----- constants --------------------------------------------------

        public enum Type
            {
            /**
             * The event indicates the subscriber group was destroyed.
             */
            GroupDestroyed,
            /**
             * The event is a channel allocation event.
             */
            ChannelAllocation,
            /**
             * The event is a channels lost event.
             */
            ChannelsLost,
            /**
             * The event is a channel populated event.
             */
            ChannelPopulated,
            /**
             * The head position of a channel has changed
             */
            ChannelHead,
            /**
             * The event is an unsubscribed event.
             */
            Unsubscribed,
            /**
             * The parent topic was destroyed.
             */
            Destroyed,
            /**
             * The parent topic was released.
             */
            Released,
            /**
             * The subscriber was disconnected.
             */
            Disconnected;
            }

        // ----- data members -----------------------------------------------

        /**
         * The channels the event relates to.
         */
        private final int[] m_anChannel;

        /**
         * The channels the event relates to.
         */
        private final SortedSet<Integer> m_setChannel;
        }
    }
