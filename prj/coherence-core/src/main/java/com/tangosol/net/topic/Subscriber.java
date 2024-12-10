/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.topic;

import com.oracle.coherence.common.base.Exceptions;

import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;
import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.FlowControl;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;

import com.tangosol.util.ValueExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.time.Instant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


/**
 * A {@link Subscriber} subscribes either directly to a {@link NamedTopic} or to a {@link NamedTopic#getSubscriberGroups()
 * subscriber group} of a {@link NamedTopic}. Each value published to a {@link NamedTopic} is delivered to all of its
 * {@link NamedTopic#getSubscriberGroups() subscriber groups} and direct (anonymous) {@link Subscriber}s.
 * <p>
 * The factory methods {@link NamedTopic#createSubscriber(Subscriber.Option[])} or
 * {@link com.tangosol.net.Session#createSubscriber(String, Option[])}
 * allows one to specify one or more {@link Subscriber.Option}s to configure the {@link Subscriber}.
 * The {@link Subscriber.Name#inGroup(String)} option specifies the subscriber group for the {@link Subscriber} to join.
 * If this option is not specified, the {@link Subscriber} is a direct (anonymous) subscriber to the topic.
 * All Subscriber options and defaults are summarized in a table in {@link Option}.
 *
 * <h3>Channels</h3>
 * Topics use the concept of channels to improve scalability. This is similar to how Coherence uses partition for caches
 * but to avoid confusion the name channel was chosen. The default is the next prime above the square root of the
 * partition count configured for the underlying cache service, which for the default partition count of 257 is 17
 * channels.
 * <p>
 * {@link Publisher Publishers} publish messages to a channel based on their ordering configuration. Subscribers then
 * subscribe from channels by assigning channel ownership to subscribers. An anonymous subscriber has ownership of all
 * channels. A subscriber that is part of a subscriber group has ownership of a sub-set of the available channels.
 * <p>
 * Channel count is configurable, but ideally should not be set too high nor too low. For example setting the channel
 * count to 1, would mean that all publishers contend to publish to a single channel, and that only one subscriber
 * in a subscriber group will be able to receive messages. Setting the channel count too high (above say the number of
 * publishers) may mean that some channels never receive any messages and are wasted. Finding the appropriate value is
 * admittedly non-trivial, however when faced with maxing out throughput from a publisher's perspective this is a
 * configuration that can be tweaked.
 *
 * <h3>Subscriber Groups</h3>
 * Subscribers can be part of a subscriber group. Within each subscriber group, each value is only
 * {@link #receive() received} by one of the {@link Subscriber group members}, enabling distributed, parallel
 * processing of the values delivered to the subscriber group. Thus, each subscriber group in effect behaves like a
 * queue over the topic data.
 * <p>
 * Subscribers in a group can be considered durable, if they are closed, or fail, then message processing will continue
 * from the next element after the last committed position when subscriber reconnect.
 * <p>
 * To maintain ordering or messages, only a single subscriber in a group polls for messages from a channel.
 * Each subscriber in a group is allocated ownership of a sub-set of the channels in a topic. This means that
 * the maximum number of subscribers in a group that could be doing any work would be the same as the topic's
 * channel count. If there are more subscribers in a group that there are channels, then the additional subscribers
 * will not be allocated ownership of any channels and hence will receive no messages.
 * It may seem that increasing the channel count to a higher number would therefore allow more subscribers to work in
 * parallel, but this is not necessarily the case.
 * <p>
 * Channels are allocated to subscribers when they are created. As more subscribers in a group are created channel
 * ownership is rebalanced, existing subscribers may lose ownership of channels that are allocated to new subscribers.
 * As subscribers are {@link Subscriber#close() closed} (or die or are timed out), again channel ownership is rebalanced
 * over the remaining subscribers.
 * <p>
 * Subscribers in a group have a configurable timeout. If a subscriber does not call receive within the timeout it is
 * considered dead and the channels it owns will be reallocated to other subscribers in the group. This is to stop
 * channel starvation where no subscriber in a group is polling from a channel.
 *
 * <h3>Positions</h3>
 * Elements in a {@link NamedTopic} are published to a channel have a unique {@link Position} within that channel.
 * A {@link Position} is an opaque representation of the underlying position as theoretically the implementation of
 * the {@link Position} could change for different types of topic. Positions are used in various places in the API,
 * for example, positions can be committed, and they can be used to move the subscriber to back or forwards within
 * a channel. A {@link Position} is serializable, so they can be stored and recovered to later reset a subscriber
 * to a desired position. Positions are {@link Comparable} so positions for elements can be used to determine whether
 * how two elements related to each other within a channel.
 *
 * <h3>Committing</h3>
 * Subscribers in a group are durable, so if they disconnect and reconnect, messages processing will restart from the
 * correct position.
 * A Subscriber can {@link Element#commit() commit an element} or {@link Subscriber#commit(int, Position) commit a
 * position in a channel}. After a successful commit, then on reconnection the first message received from a channel
 * will be <i>the next message <b>after</b> the committed position</i>.
 * <p>
 * When a position is committed, this will also commit any earlier positions in the channel. For example, if
 * five elements are received and commit is called only on the last element, this effectively also commits the previous
 * four elements.
 * <p>
 * When topics are configured not to retain elements, received elements will be removed as they are committed by
 * subscribers.
 * <p>
 * If a subscriber in a subscriber group is timed-out (or dies) and its channel ownership is reallocated to other
 * subscribers in the group, those subscribers will start to receive messages from the last committed position for
 * of the channels from the failed subscriber.
 * <p>
 * Commits may be performed synchronously (using {@link #commit(int, Position)}) or asynchronously (using
 * {@link #commitAsync(int, Position)}). There is no facility for automatic commit of messages, all calls
 * to commit <i>must</i> be done manually by application code.
 *
 * <h3>Seeking</h3>
 * A new subscriber will start to receive message from the head position of a channel (or the last committed position
 * for a durable subscriber) and continue to receive messages in order until the tail is reached.
 * It is possible to reposition a subscriber (backwards or forwards) using one of the {@link #seek(int, Instant)}
 * methods. Seeking applies a new position to a specific channel. If an attempt is made to position the channel before
 * the first available message (the channel's head) then the subscriber will be positioned at the head.
 * Correspondingly, if an attempt is made to position a subscriber way beyond the last position in a channel
 * (the channel's tail) then the subscriber will be positioned at the tail of the channel and will receive the
 * next message published to that channel.
 * <p>
 * It is also possible to seek to a timestamp. Published messages are given a timestamp, which is the
 * {@link com.tangosol.net.Cluster#getTimeMillis() Coherence cluster time} on the storage enabled cluster member that
 * accepted the published message. Using the {@link #seek(int, Instant)} method a subscriber can be repositioned so that
 * the next message received from a channel is the first message with a timestamp <i>greater than</i> the timestamp
 * used in the seek call. For example to position channel 0 so that the next message received is the first message after
 * 20:30 on July 5th 2021:
 * <pre>
 *     Instant timestamp = LocalDateTime.of(2021, Month.JULY, 5, 20, 30)
 *             .toInstant(ZoneOffset.UTC);
 *     subscriber.seek(0, timestamp);
 * </pre>
 *
 * <h4>Seeking Forwards</h4>
 * It is important to note that seeking forwards is skipping over messages, those skipped message will never
 * be received once another commit is executed. Moving forwards does not alter the commit position, so when a
 * subscriber has committed a position, then moves forwards and later fails, it will restart back at the commit.
 *
 * <h4>Seeking Backwards Over Previous Commits</h4>
 * When topics are configured not to retain elements removal of elements occurs as their positions are committed, so
 * they can never be received again.
 * This means that in this case, it would not be possible to seek backwards further than the last commit, as those
 * elements no longer exist in the topic.
 * <p>
 * When topics are configured to retain elements then it is possible to seek backwards further than the last commit.
 * This effectively rolls-back those commits and the previously committed messages after the new seek position will
 * be re-received. If a subscriber fails after moving back in this fashion it will restart at the rolled-back position.
 *
 * <h3>Receiving Elements</h3>
 * Receiving elements from a topic is an asynchronous operation, calls to {@link #receive()} (or the batch
 * version {@link #receive(int)}) return a {@link CompletableFuture}. If multiple calls are made to receive,
 * the returned futures will complete in the correct order to maintain message ordering in a channel.
 * To maintain ordering, the futures are completed by a single daemon thread. This means that code using
 * any of the synchronous {@link CompletableFuture} handling patterns, such as
 * {@link CompletableFuture#thenApply(java.util.function.Function)} or
 * {@link CompletableFuture#thenAccept(java.util.function.Consumer)}, etc. will run on
 * the same daemon thread, so application code in the handler methods must complete before the next receive future
 * will be completed. Again, this is intentional, to maintain strict ordering of processing of received elements.
 * If the {@link CompletableFuture} asynchronous handler methods are used such as,
 * {@link CompletableFuture#thenApplyAsync(java.util.function.Function)}
 * or {@link CompletableFuture#thenAcceptAsync(java.util.function.Consumer)}, etc.
 * then the application code handling the received element will execute on another thread, and at this point there
 * are no ordering guarantees.
 * <p>
 * It is important that application code uses the correct handling of the returned futures to both maintain
 * ordering (if that is important to the application) and also to have correct error handling, and not lose exceptions,
 * which is easy to do in poorly written asynchronous future handler code.
 *
 * <h3>Clean-Up</h3>
 * Subscribers should ideally be closed when application code finishes with them. This will clean up server-side
 * resources associated with a subscriber.
 * <p>
 * It is also important (possibly more important) to {@link NamedTopic#destroySubscriberGroup(String) clean up
 * subscriber groups} that are no longer required. Failure to delete a subscriber group will cause messages to be
 * retained on the server that would otherwise have been removed, so consuming more server-side resources such as
 * heap and disc.
 *
 * @param <V>  the type of the value returned by the subscriber
 *
 * @author jf/jk/mf 2015.06.03
 * @since Coherence 14.1.1
 */
public interface Subscriber<V>
        extends AutoCloseable
    {
    /**
     * Receive a value from the topic.  If there is no value available then the future will complete according to
     * the {@link CompleteOnEmpty} option used to create the {@link Subscriber}.
     * <p>
     * Note: If the returned future is {@link CompletableFuture#cancel(boolean) cancelled} it is possible that a value
     * may still be considered by the topic to have been received by this group, while the group would consider this
     * a lost value. Subscriber implementations will make its best effort to prevent such loss, but it cannot be
     * guaranteed and thus cancellation is not advisable.
     * <p>
     * The {@link CompletableFuture futures} returned from calls to {@code receive} are completed sequentially.
     * If the methods used to handle completion in application code block this will block completions of
     * subsequent futures. This is to maintain ordering of consumption of completed futures.
     * If the application code handles {@link CompletableFuture future} completion using the asynchronous methods
     * of {@link CompletableFuture} (i.e. handling is handed off to another thread) this could cause out of order
     * consumption as received values are consumed on different threads.
     *
     * @return a {@link CompletableFuture} which can be used to access the result of this completed operation
     *
     * @throws IllegalStateException if the {@link Subscriber} is closed
     */
    public CompletableFuture<Element<V>> receive();

    /**
     * Receive a batch of {@link Element elements} from the topic.
     * <p>
     * The {@code cMessage} parameter specifies the maximum number of elements to receive in the batch. The subscriber
     * may return fewer elements than the {@code cMessage} parameter; this does not signify that the topic is empty.
     * <p>
     * If there is no value available then the future will complete according to the {@link CompleteOnEmpty} option used
     * to create the {@link Subscriber}.
     * <p>
     * If the poll of the topic returns nothing (i.e. the topic was empty and {@link CompleteOnEmpty}) is true then the
     * {@link java.util.function.Consumer} will not be called.
     * <p>
     * The {@link CompletableFuture futures} returned from calls to {@code receive} are completed sequentially.
     * If the methods used to handle completion in application code block this will block completions of
     * subsequent futures. This is to maintain ordering of consumption of completed futures.
     * If the application code handles {@link CompletableFuture future} completion using the asynchronous methods
     * of {@link CompletableFuture} (i.e. handling is handed off to another thread) this could cause out of order
     * consumption as received values are consumed on different threads.
     *
     * @param cBatch  the maximum number of elements to receive in the batch
     *
     * @return a future which can be used to access the result of this completed operation
     *
     * @throws IllegalStateException if the {@link Subscriber} is closed
     */
    public CompletableFuture<List<Element<V>>> receive(int cBatch);

    /**
     * Returns the current set of channels that this {@link Subscriber} owns.
     * <p>
     * Subscribers that are part of a subscriber group own a sub-set of the available channels.
     * A subscriber in a group should normally be assigned ownership of at least one channel. In the case where there
     * are more subscribers in a group that the number of channels configured for a topic, then some
     * subscribers will obviously own zero channels.
     * Anonymous subscribers that are not part of a group are always owners all the available channels.
     *
     * @return the current set of channels that this {@link Subscriber} is the owner of, or an
     *         empty array if this subscriber has not been assigned ownership any channels
     */
    public int[] getChannels();

    /**
     * Returns {@code true} if this subscriber is the owner of the specified channel.
     * <p>
     * This method only really applies to subscribers that are part of a group that may own zero or more channels.
     * This method will always return {@code true} for an anonymous subscriber that is not part of a group.
     * <p>
     * As channel ownership may change as subscribers in a group are created and closed the result of this method
     * is somewhat transient. To more accurately track channel ownership changes create a subscriber using the
     * {@link ChannelOwnershipListeners} option
     *
     * @param nChannel  the channel number
     *
     * @return {@code true} if this subscriber is currently the owner of the specified channel, otherwise
     *         returns {@code false} if the channel is not owned by this subscriber
     */
    public default boolean isOwner(int nChannel)
        {
        int[] anChannel = getChannels();
        for (int c : anChannel)
            {
            if (c == nChannel)
                {
                return true;
                }
            }
        return false;
        }

    /**
     * Returns the number of channels in the underlying {@link NamedTopic}.
     * <p>
     * This could be different to the number of channels {@link #getChannels() owned} by this {@link Subscriber}.
     *
     * @return the number of channels in the underlying {@link NamedTopic}
     */
    public int getChannelCount();

    /**
     * Return the {@link FlowControl} object governing this subscriber.
     *
     * @return the FlowControl object.
     */
    public FlowControl getFlowControl();

    /**
     * Close the Subscriber.
     * <p>
     * Closing a subscriber ensures that no new {@link #receive()} requests will be accepted and all pending
     * receive requests will be completed or safely cancelled.
     * <p>
     * For a direct topic {@code Subscriber.close()}  enables the release of storage resources for
     * unconsumed values.
     * <p>
     * For a {@link Subscriber group member}, {@code close()} indicates that this member has left its corresponding
     * {@link Name group}. One must actively manage a {@link NamedTopic#getSubscriberGroups() NamedTopic's logical
     * subscriber groups} since their life span is independent of active {@link Subscriber} group membership.
     * {@link NamedTopic#destroySubscriberGroup(String)} releases storage and stops accumulating topic values for a
     * subscriber group.
     * <p>
     * Calling {@code close()} on an already closed subscriber is a no-op.
     */
    @Override
    public void close();

    /**
     * Send a heartbeat to the server to keep this subscriber alive.
     * <p>
     * Heartbeat messages are sent on calls to any of the {@link #receive()} methods.
     * If a subscriber does not call receive within the configured timeout time, it will be considered dead
     * and lose its channel ownership. This is to stop badly behaving subscribers from blocking messages
     * from being processed, for example if a bug in application code causes a subscriber to deadlock or take
     * an excessive amount of time to process messages.
     * <p>
     * If application code knows that it will take longer to execute than the subscriber timeout, for example
     * the message processing communicates with a third-party system that is in some kind of back-off loop, then
     * the subscriber can send a heartbeat to keep itself alive.
     * <p>
     * A subscriber that times-out is still active and is not closed. On the next call to a {@link #receive()} method
     * the subscriber will reconnect and be reallocated channel ownerships. Although a timed-out subscriber is not
     * closed it would have lost ownership of channels and not be able to commit any messages that it was processing
     * when it timed out. This is because another subscriber in the group may have already been allocated ownership of
     * the same channels, already processed, and committed that same message. Allowing commits of unowned channels
     * is configurable, by default a subscriber can only commit positions for channels it owns.
     * <p>
     * Heart-beating only applies to subscribers that are members of a subscriber group. An anonymous subscriber
     * owns all channels and no other subscriber is sharing the workload, so anonymous subscriber will not be
     * timed-out. For an anonymous subscriber the heartbeat operation is a no-op.
     */
    public void heartbeat();

    /**
     * Determine whether this {@link Subscriber} is active.
     *
     * @return {@code true} if this {@link Subscriber} is active
     */
    public boolean isActive();

    /**
     * Add an action to be executed when this {@link Subscriber} is closed.
     *
     * @param action  the action to execute
     */
    public void onClose(Runnable action);

    /**
     * Commit the specified channel and position.
     *
     * @param nChannel  the channel to commit
     * @param position  the position within the channel to commit
     *
     * @return the result of the commit request
     *
     * @throws IllegalStateException if the {@link Subscriber} is closed
     */
    public default CommitResult commit(int nChannel, Position position)
        {
        return commit(Collections.singletonMap(nChannel, position)).get(nChannel);
        }

    /**
     * Asynchronously commit the specified channel and position.
     *
     * @param nChannel  the channel to commit
     * @param position  the position within the channel to commit
     *
     * @return the result of the commit request
     *
     * @throws IllegalStateException if the {@link Subscriber} is closed
     */
    public CompletableFuture<CommitResult> commitAsync(int nChannel, Position position);

    /**
     * Commit the specified channels and positions.
     *
     * @param mapPositions  a map of channels napped to the position to commit
     *
     * @return a map of results of the commit request for each channel
     *
     * @throws IllegalStateException if the {@link Subscriber} is closed
     */
    public default Map<Integer, CommitResult> commit(Map<Integer, Position> mapPositions)
        {
        try
            {
            return commitAsync(mapPositions).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Asynchronously commit the specified channels and positions.
     *
     * @param mapPositions  a map of channels mapped to the positions to commit
     *
     * @return a map of results of the commit request for each channel
     *
     * @throws IllegalStateException if the {@link Subscriber} is closed
     */
    public CompletableFuture<Map<Integer, CommitResult>> commitAsync(Map<Integer, Position> mapPositions);

    /**
     * Returns an {@link Optional} containing the latest position committed for a channel,
     * or {@link Optional#empty()} if the channel is not owned by this {@link Subscriber}
     *
     * @param nChannel  the channel to get the last committed position for
     *
     * @return the latest position committed for a channel, or {@link Optional#empty()} if the
     *         channel is not owned by this {@link Subscriber}
     *
     * @throws IllegalStateException if the {@link Subscriber} is closed
     */
    public default Optional<Position> getLastCommitted(int nChannel)
        {
        return Optional.ofNullable(getLastCommitted().get(nChannel));
        }

    /**
     * Returns a {@link Map} of channels to the latest {@link Position} committed for that
     * channel; the map will only contain channels owned by this {@link Subscriber}.
     *
     * @return a {@link Map} of channels to the latest {@link Position} committed for that
     *         channel; the map will only contain channels owned by this {@link Subscriber}
     *
     * @throws IllegalStateException if the {@link Subscriber} is closed
     */
    public Map<Integer, Position> getLastCommitted();

    /**
     * Seek to the specified position in a channel.
     * <p>
     * This method will position the subscriber such that the element returned by the next
     * call to any {@code receive} method will be the element <i>after</i> the specified
     * position in the channel.
     * <p>
     * An attempt to move the position after the current tail position for the channel will
     * reposition the channel at the tail, effectively making the channel appear empty.
     * <p>
     * An attempt to move the position before the current head position for the channel will
     * reposition the channel at the head, effectively moving the subscriber back to the first
     * available element in the channel.
     * <p>
     * Repositioning a channel ahead of its current position will skip unread elements in that channel.
     * It will not move the last committed position for the channel, so if the subscriber is disconnected
     * then it will restart at the previous commit.
     * To also move the committed position a subsequent call to {@link Subscriber#commit(int, Position)}
     * using the seeked to {@link Position} for the channel returned by this method will be required.
     * <p>
     * Repositioning a channel back before the current committed position will move the committed position
     * for the channel back to the seeked position (i.e. back to the position returned from this method).
     * <p>
     * Any in-flight {@link #receive()} requests will complete before the seek operation takes place, whilst
     * any requests still locally queued will be completed after seeking.
     *
     * @param nChannel  the channel to reposition
     * @param position  the {@link Position} to seek to
     *
     * @return the {@link Position} actually seeked to, which may be different to the {@code position}
     *         parameter if the {@code position} parameter is before the channel's head or after the
     *         channel's tail.
     *
     * @throws IllegalArgumentException if the {@link Position} is not the correct type for
     *                                  the topic implementation or is an invalid position
     * @throws IllegalStateException    if this subscriber is not the owner of the channel being repositioned
     */
    public Position seek(int nChannel, Position position);

    /**
     * Seek to the specified position in a channel and set the commit point to the new {@link Position}.
     * <p>
     * This method will position the subscriber such that the element returned by the next
     * call to any {@code receive} method will be the element <i>after</i> the specified
     * position in the channel.
     * <p>
     * An attempt to move the position after the current tail position for the channel will
     * reposition the channel at the tail, effectively making the channel appear empty.
     * <p>
     * An attempt to move the position before the current head position for the channel will
     * reposition the channel at the head, effectively moving the subscriber back to the first
     * available element in the channel.
     * <p>
     * Repositioning a channel back before the current committed position will move the committed position
     * for the channel back to the seeked position (i.e. back to the position returned from this method).
     * <p>
     * Any in-flight {@link #receive()} requests will complete before the seek operation takes place, whilst
     * any requests still locally queued will be completed after seeking.
     *
     * @param nChannel  the channel to reposition
     * @param position  the {@link Position} to seek to
     *
     * @return the {@link Position} actually seeked to, which may be different to the {@code position}
     *         parameter if the {@code position} parameter is before the channel's head or after the
     *         channel's tail.
     *
     * @throws IllegalArgumentException if the {@link Position} is not the correct type for
     *                                  the topic implementation or is an invalid position
     * @throws IllegalStateException    if this subscriber is not the owner of the channel being repositioned
     */
    public default Position seekAndCommit(int nChannel, Position position)
        {
        Position positionNew = seek(nChannel, position);
        if (positionNew != null)
            {
            commit(nChannel, positionNew);
            }
        return positionNew;
        }

    /**
     * Seek to the specified position in a set of channels.
     * <p>
     * This method will position the subscriber such that the element returned by the next
     * call to any {@code receive} method will be the element <i>after</i> the specified
     * position in each channel.
     * <p>
     * An attempt to move the position after the current tail position for a channel will
     * reposition the channel at the tail, effectively making the channel appear empty.
     * <p>
     * An attempt to move the position before the current head position for a channel will
     * reposition the channel at the head, effectively moving the subscriber back to the first
     * available element for that channel.
     * <p>
     * Repositioning a channel ahead of its current position will skip unread elements in that channel.
     * It will not move the last committed position for the channel, so if the subscriber is disconnected
     * then it will restart at the previous commit.
     * To also move the committed position a subsequent call to {@link Subscriber#commit(int, Position)}
     * using the seeked to {@link Position} for the channel returned by this method will be required.
     * <p>
     * Repositioning a channel back before the current committed position will move the committed position
     * for the channel back to the seeked position (i.e. back to the position returned for that channel
     * from this method).
     * <p>
     * Any in-flight {@link #receive()} requests will complete before the seek operation takes place, whilst
     * any requests still locally queued will be completed after seeking.
     *
     * @param mapPosition  a {@link Map} of {@link Position positions} keyed by channel to seek to
     *
     * @return a {@link Map} keyed by channel of the {@link Position} seeked to, which may be different
     *         to the {@code position} parameter if the {@code position} parameter is before the
     *         channel's head or after the channel's tail.
     *
     * @throws IllegalArgumentException if the {@link Position} is not the correct type for
     *                                  the topic implementation or is an invalid position
     * @throws IllegalStateException    if this subscriber is not the owner of the channel being repositioned
     */
    public Map<Integer, Position> seek(Map<Integer, Position> mapPosition);

    /**
     * Seek to the specified position in a set of channels and sets the commit position for the channels.
     * <p>
     * This method will position the subscriber such that the element returned by the next
     * call to any {@code receive} method will be the element <i>after</i> the specified
     * position in each channel.
     * <p>
     * An attempt to move the position after the current tail position for a channel will
     * reposition the channel at the tail, effectively making the channel appear empty.
     * <p>
     * An attempt to move the position before the current head position for a channel will
     * reposition the channel at the head, effectively moving the subscriber back to the first
     * available element for that channel.
     * <p>
     * Repositioning a channel back before the current committed position will move the committed position
     * for the channel back to the seeked position (i.e. back to the position returned for that channel
     * from this method).
     * <p>
     * Any in-flight {@link #receive()} requests will complete before the seek operation takes place, whilst
     * any requests still locally queued will be completed after seeking.
     *
     * @param mapPosition  a {@link Map} of {@link Position positions} keyed by channel to seek to
     *
     * @return a {@link Map} keyed by channel of the {@link Position} seeked to, which may be different
     *         to the {@code position} parameter if the {@code position} parameter is before the
     *         channel's head or after the channel's tail.
     *
     * @throws IllegalArgumentException if the {@link Position} is not the correct type for
     *                                  the topic implementation or is an invalid position
     * @throws IllegalStateException    if this subscriber is not the owner of the channel being repositioned
     */
    public default Map<Integer, Position> seekAndCommit(Map<Integer, Position> mapPosition)
        {
        Map<Integer, Position> map = seek(mapPosition);
        if (map != null && !map.isEmpty())
            {
            commit(map);
            }
        return map;
        }

    /**
     * Seek to a position in a channel based the published timestamp of the elements in the topic.
     * <p>
     * This method will position the subscriber such that the element returned by the next call to any
     * {@code receive} method that polls the specific channel will be the element with a published timestamp
     * <i>after</i> the specified timestamp. The published timestamp is the
     * {@link com.tangosol.net.Cluster#getTimeMillis() cluster timestamp} on the member receiving the published
     * element at the time the publish request was accepted.
     * <p>
     * An attempt to move the position using a timestamp later that the timestamp of the current tail position for
     * the channel will reposition the channel at the tail, effectively making the channel appear empty.
     * <p>
     * An attempt to move the position using a timestamp earlier than the current head position for the channel will
     * reposition the channel at the head, effectively moving the subscriber back to the first available element in
     * the channel.
     * <p>
     * Repositioning a channel ahead of its current position will skip unread elements in that channel.
     * It will not move the last committed position for the channel, so if the subscriber is disconnected
     * then it will restart at the previous commit.
     * To also move the committed position a subsequent call to {@link Subscriber#commit(int, Position)}
     * using the seeked to {@link Position} for the channel returned by this method will be required.
     * <p>
     * Repositioning a channel back before the current committed position will move the committed position for
     * the channel back to the seeked position (i.e. back to the position returned from this method) effectively
     * rolling back commits made to elements in the channel after the seeked.
     * <p>
     * Any in-flight {@link #receive()} requests will complete before the seek operation takes place, whilst
     * any requests still locally queued will be completed after seeking.
     *
     * @param nChannel  the channel to reposition
     * @param timestamp the timestamp to seek to
     *
     * @return the {@link Position} actually seeked to
     *
     * @throws IllegalStateException  if this subscriber is not the owner of the channel being repositioned
     * @throws NullPointerException   if the {@code timestamp} is {@code null}
     */
    public Position seek(int nChannel, Instant timestamp);

    /**
     * Seek to a position in a channel based the published timestamp of the elements in the topic
     * and set the commit point to the new position.
     * <p>
     * This method will position the subscriber such that the element returned by the next call to any
     * {@code receive} method that polls the specific channel will be the element with a published timestamp
     * <i>after</i> the specified timestamp. The published timestamp is the
     * {@link com.tangosol.net.Cluster#getTimeMillis() cluster timestamp} on the member receiving the published
     * element at the time the publish request was accepted.
     * <p>
     * An attempt to move the position using a timestamp later that the timestamp of the current tail position for
     * the channel will reposition the channel at the tail, effectively making the channel appear empty.
     * <p>
     * An attempt to move the position using a timestamp earlier than the current head position for the channel will
     * reposition the channel at the head, effectively moving the subscriber back to the first available element in
     * the channel.
     * <p>
     * Repositioning a channel back before the current committed position will move the committed position for
     * the channel back to the seeked position (i.e. back to the position returned from this method) effectively
     * rolling back commits made to elements in the channel after the seeked.
     * <p>
     * Any in-flight {@link #receive()} requests will complete before the seek operation takes place, whilst
     * any requests still locally queued will be completed after seeking.
     *
     * @param nChannel  the channel to reposition
     * @param timestamp the timestamp to seek to
     *
     * @return the {@link Position} actually seeked to
     *
     * @throws IllegalStateException  if this subscriber is not the owner of the channel being repositioned
     * @throws NullPointerException   if the {@code timestamp} is {@code null}
     */
    public default Position seekAndCommit(int nChannel, Instant timestamp)
        {
        Position position = seek(nChannel, timestamp);
        if (position != null)
            {
            commit(nChannel, position);
            }
        return position;
        }

    /**
     * Reposition one or more channels to their respective head positions.
     * <p>
     * If any of the specified channels have been committed their commits will also be reset,
     * effectively removing all committed positions for the channel.
     * <p>
     * The next message received will be the first available message in the topic for the channel.
     * Note that this may not be the first message originally received by the subscriber if a topic is
     * not set to retain received elements, as in that case messages are removed after being committed,
     * so cannot be re-received.
     * <p>
     * Any in-flight {@link #receive()} requests will complete before the seek operation takes place, whilst
     * any requests still locally queued will be completed after seeking.
     *
     * @param anChannel  one or more channels to reposition to the head
     *
     * @return a {@link Map} keyed by channel of the head {@link Position} seeked to for each channel
     *
     * @throws IllegalStateException if this subscriber is not the owner one or more of the specified channels
     */
    public Map<Integer, Position> seekToHead(int... anChannel);

    /**
     * Reposition one or more channels to their respective tail positions.
     * The channel will be repositioned to read the next message published to that channel.
     * <p>
     * Any in-flight {@link #receive()} requests will complete before the seek operation takes place, whilst
     * any requests still locally queued will be completed after seeking.
     *
     * @param anChannel  one or more channels to reposition to the tail
     *
     * @return a {@link Map} keyed by channel of the tail {@link Position} seeked to for each channel
     *
     * @throws IllegalStateException if this subscriber is not the owner of one or more of the specified channels
     */
    public Map<Integer, Position> seekToTail(int... anChannel);

    /**
     * Reposition one or more channels to their respective tail positions and set the commit point
     * to the new {@link Position}. The channel will be repositioned to read the next message
     * published to that channel.
     * <p>
     * Any in-flight {@link #receive()} requests will complete before the seek operation takes place, whilst
     * any requests still locally queued will be completed after seeking.
     *
     * @param anChannel  one or more channels to reposition to the tail
     *
     * @return a {@link Map} keyed by channel of the tail {@link Position} seeked to for each channel
     *
     * @throws IllegalStateException if this subscriber is not the owner of one or more of the specified channels
     */
    public default Map<Integer, Position> seekToTailAndCommit(int... anChannel)
        {
        Map<Integer, Position> mapPosition = seekToTail(anChannel);
        if (mapPosition != null && !mapPosition.isEmpty())
            {
            commit(mapPosition);
            }
        return mapPosition;
        }

    /**
     * Returns the {@link Position} that is currently the tail for the specified channel,
     * or {@link Optional#empty()} if the channel is not owned by this{@link Subscriber}.
     *
     * @param nChannel  the channel to obtain the tail {@link Position} for
     *
     * @return the {@link Position} that is currently the tail for the specified channel or
     *         {@link Optional#empty()} if the channel is not owned by this{@link Subscriber}.
     *
     * @throws IllegalStateException if the {@link Subscriber} is closed
     */
    public default Optional<Position> getHead(int nChannel)
        {
        return Optional.ofNullable(getHeads().get(nChannel));
        }
        
    /**
     * Returns a {@link Map} of the {@link Position Positions} that are currently the head
     * for each channel owned by this {@link Subscriber}.
     * <p>
     * This result is somewhat transient in situations where the Subscriber has in-flight
     * receive requests, so the heads returned may change just after the method returns.
     *
     * @return the {@link Position Positions} that are currently the heads for each channel owned
     *         by this {@link Subscriber}
     *
     * @throws IllegalStateException if the {@link Subscriber} is closed
     */
    public Map<Integer, Position> getHeads();

    /**
     * Returns the {@link Position} that is currently the tail for the specified channel
     * or {@link Optional#empty()} if the channel is not owned by this{@link Subscriber}.
     * <p>
     * This result is somewhat transient in situations where publishers are actively publishing
     * messages to the topic, so the tail position returned may change just after this method
     * returns.
     *
     * @param nChannel  the channel to obtain the tail {@link Position} for
     *
     * @return the {@link Position} that is currently the tail for the specified channel or
     *         {@link Optional#empty()} if the channel is not owned by this{@link Subscriber}.
     *
     * @throws IllegalStateException if the {@link Subscriber} is closed
     */
    public default Optional<Position> getTail(int nChannel)
        {
        return Optional.ofNullable(getTails().get(nChannel));
        }

    /**
     * Returns a {@link Map} of the {@link Position Positions} that are currently the tail
     * for each channel owned by this {@link Subscriber}; that is the last message in
     * the channel.
     * <p>
     * This result is somewhat transient in situations where publishers are actively publishing
     * messages to the topic, so the tail positions returned may change just after this method
     * returns.
     *
     * @return the {@link Position Positions} that are currently the tails for each channel
     *
     * @throws IllegalStateException if the {@link Subscriber} is closed
     */
    public Map<Integer, Position> getTails();

    /**
     * Returns the underlying {@link NamedTopic} that this {@link Subscriber}
     * is subscribed to, which could be of a different generic type to this
     * {@link Subscriber} if the subscriber is using a transformer.
     *
     * @param <T>  the type of the underlying topic
     *
     * @return the underlying {@link NamedTopic} that this {@link Subscriber}
     *         is subscribed to
     */
    public <T> NamedTopic<T> getNamedTopic();

    /**
     * Returns the number of remaining messages to be read from the topic for this subscriber.
     *
     * @return  the number of remaining messages
     */
    int getRemainingMessages();

    /**
     * Returns the number of remaining messages to be read from the topic channel for this subscriber.
     *
     * @param nChannel  the channel to count remaining messages in
     *
     * @return  the number of remaining messages, or zero if this subscriber does not own the channel
     */
    int getRemainingMessages(int nChannel);

    // ----- option methods -------------------------------------------------

    /**
     * Obtain a {@link Option} that specifies a group name
     * for a {@link Subscriber}.
     *
     * @param sName  the group name to use for the {Link Subscriber}.
     * @param <V>    the type of the elements being received from the topic
     *
     * @return a {@link Option} that specifies a group name
     *         for a {@link Subscriber}
     */
    static <V> Name<V> inGroup(String sName)
        {
        return Name.inGroup(sName);
        }

    /**
     * Obtain the Option indicating futures should complete if the topic is empty.
     *
     * @return the Option indicating futures should complete if the topic is empty
     */
    static <V> CompleteOnEmpty<V> completeOnEmpty()
        {
        return CompleteOnEmpty.enabled();
        }

    /**
     * Create a {@link ChannelOwnershipListeners} option with one or more
     * {@link ChannelOwnershipListener listeners}.
     *
     * @param aListener the {@link ChannelOwnershipListener listeners} to add to the subscriber
     * @param <V>       the type of the elements being received from the topic
     *
     * @return a {@link ChannelOwnershipListeners} option with one or more
     *          {@link ChannelOwnershipListener listeners}
     */
    static <V> ChannelOwnershipListeners<V> withListener(ChannelOwnershipListener... aListener)
        {
        return ChannelOwnershipListeners.of(aListener);
        }

    /**
     * Return a Convert option with the specified extractor.
     *
     * @param extractor  the converter extractor
     * @param <V>       the type of the elements being received from the topic
     *
     * @return the Filtered option
     */
    static <V, U> Convert<V, U> withConverter(ValueExtractor<? super V, U> extractor)
        {
        return Convert.using(extractor);
        }

    /**
     * Return a Filtered option with the specified filter.
     *
     * @param filter  the filter
     * @param <V>     the type of the elements being received from the topic
     *
     * @return the Filtered option
     */
    public static <V> Filtered<V> withFilter(Filter<? super V> filter)
        {
        return Filtered.by(filter);
        }

    // ----- inner interface Id ---------------------------------------------

    /**
     * A marker interface for {@link Subscriber} identifiers.
     */
    interface Id
        {
        }

    // ----- inner interface Element ----------------------------------------

    /**
     * Element represents a container for returned values.
     *
     * @param <V>  the type of value stored in the topic
     */
    interface Element<V>
        {
        /**
         * Return the received value
         *
         * @return the received value
         */
        V getValue();

        /**
         * Returns the element's value in serialized form.
         *
         * @return the element's value in serialized form
         */
        Binary getBinaryValue();

        /**
         * Return the channel that the element was received from.
         *
         * @return the channel that the element was received from
         */
        int getChannel();

        /**
         * Returns the position of this element within the channel in a topic.
         *
         * @return the position of this element within the channel in a topic
         */
        Position getPosition();

        /**
         * Return the timestamp that the element was published.
         * <p>
         * The timestamp is the epoch time that the element was accepted into the topic taken from the
         * {@link com.tangosol.net.Cluster#getTimeMillis() cluster time} on the member that accepted the
         * published element.
         *
         * @return the published timestamp
         */
        Instant getTimestamp();

        /**
         * Commit this position in the element's owning channel.
         *
         * @return the result of the commit request
         *
         * @throws UnsupportedOperationException if this element implementation does not support committing
         */
        default CommitResult commit()
            {
            try
                {
                return commitAsync().get();
                }
            catch (InterruptedException | ExecutionException e)
                {
                throw Exceptions.ensureRuntimeException(e);
                }
            }

        /**
         * Commit this position in the element's owning channel.
         *
         * @return the result of the commit request
         *
         * @throws UnsupportedOperationException if this element implementation does not support committing
         */
        CompletableFuture<CommitResult> commitAsync();
        }

    // ----- inner class: CommitResult --------------------------------------

    /**
     * The result of a commit request.
     */
    class CommitResult
            extends AbstractEvolvable
            implements PortableObject, ExternalizableLite
        {
        // ----- constructors -----------------------------------------------

        /**
         * Default constructor for serialization.
         */
        public CommitResult()
            {
            }

        /**
         * Create a {@link CommitResult}.
         *
         * @param position  the committed {@link Position}
         * @param status    the {@link CommitResultStatus result} of the commit request
         */
        public CommitResult(int nChannel, Position position, CommitResultStatus status)
            {
            this(nChannel, position, status, null);
            }

        /**
         * Create a  rejected {@link CommitResult}.
         *
         * @param position   the committed {@link Position}
         * @param throwable  the error that caused the commit to fail
         */
        public CommitResult(int nChannel, Position position, Throwable throwable)
            {
            this(nChannel, position, CommitResultStatus.Rejected, throwable);
            }

        /**
         * Create a  rejected {@link CommitResult}.
         *
         * @param position   the committed {@link Position}
         * @param status     the {@link CommitResultStatus result} of the commit request
         * @param throwable  an optional error
         */
        public CommitResult(int nChannel, Position position, CommitResultStatus status, Throwable throwable)
            {
            m_nChannel  = nChannel;
            m_position  = position;
            m_status    = status;
            m_throwable = throwable;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Returns the channel that the {@link Position} was committed in.
         *
         * @return the channel that the {@link Position} was committed in
         */
        public OptionalInt getChannel()
            {
            return m_nChannel < 0 ? OptionalInt.empty() : OptionalInt.of(m_nChannel);
            }

        /**
         * Returns the requested commit {@link Position}.
         *
         * @return the requested commit {@link Position}
         */
        public Optional<Position> getPosition()
            {
            return Optional.ofNullable(m_position);
            }

        /**
         * Returns the {@link CommitResultStatus result} of the commit request.
         *
         * @return the {@link CommitResultStatus result} of the commit request
         */
        public CommitResultStatus getStatus()
            {
            return m_status;
            }

        /**
         * Returns an optional error that caused a commit to fail.
         *
         * @return an optional error that caused a commit to fail
         */
        public Optional<Throwable> getError()
            {
            return Optional.ofNullable(m_throwable);
            }

        /**
         * Returns {@code true} if the result can be considered successful,
         * i.e. its status is not a failure status.
         *
         * @return {@code true} if the result can be considered successful
         */
        public boolean isSuccess()
            {
            return m_status != CommitResultStatus.Rejected;
            }

        // ----- ExternalizableLite methods ---------------------------------

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_nChannel  = in.readInt();
            m_position  = ExternalizableHelper.readObject(in);
            m_status    = ExternalizableHelper.readObject(in);
            m_throwable = ExternalizableHelper.readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            out.writeInt(m_nChannel);
            ExternalizableHelper.writeObject(out, m_position);
            ExternalizableHelper.writeObject(out, m_status);
            ExternalizableHelper.writeObject(out, m_throwable);
            }

        // ----- PortableObject methods -------------------------------------

        @Override
        public int getImplVersion()
            {
            return DATA_VERSION;
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_nChannel  = in.readInt(0);
            m_position  = in.readObject(1);
            m_status    = in.readObject(2);
            m_throwable = in.readObject(3);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeInt(0, m_nChannel);
            out.writeObject(1, m_position);
            out.writeObject(2, m_status);
            out.writeObject(3, m_throwable);
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public String toString()
            {
            return "CommitResult(" +
                    "channel=" + m_nChannel +
                    ", position=" + m_position +
                    ", status=" + m_status +
                    ", throwable=" + m_throwable +
                    ')';
            }

        // ----- constants --------------------------------------------------

        /**
         * The evolvable version of this class.
         */
        public static final int DATA_VERSION = 1;

        /**
         * A result indicating there was nothing to be committed.
         */
        public static final CommitResult NOTHING_TO_COMMIT
                = new CommitResult(-1, null, CommitResultStatus.NothingToCommit);

        // ----- data members -----------------------------------------------

        /**
         * The channel that the {@link Position} was committed in.
         */
        private int m_nChannel;

        /**
         * The committed {@link Position}.
         */
        private Position m_position;

        /**
         * The result of the commit request.
         */
        private CommitResultStatus m_status;

        /**
         * An optional error causing the commit to be rejected.
         */
        private Throwable m_throwable;
        }

    // ----- inner interface: CommitResultStatus ----------------------------

    /**
     * The different result statuses for a commit request.
     */
    enum CommitResultStatus
        {
        /**
         * The position was successfully committed.
         */
        Committed,
        /**
         * The position was already committed.
         * Typically, this is caused by a commit of a higher position in the channel
         * already being processed.
         */
        AlreadyCommitted,
        /**
         * The commit request was rejected.
         */
        Rejected,
        /**
         * The position was successfully committed but the committing subscriber
         * does not own the committed channel.
         */
        Unowned,
        /**
         * A commit request was made, but there was no position to be committed.
         */
        NothingToCommit
        }

    // ----- inner interface: ChannelOwnershipListener ----------------------

    /**
     * A listener that receives notification of channel ownership changes.
     */
    public static interface ChannelOwnershipListener
        {
        /**
         * The channels owned by a subscriber have changed.
         *
         * @param setAssigned  the set of channels assigned to the subscriber
         */
        public void onChannelsAssigned(Set<Integer> setAssigned);

        /**
         * Channels previously owned by a subscriber have been revoked.
         *
         * @param setRevoked  the set of revoked channels
         */
        public void onChannelsRevoked(Set<Integer> setRevoked);

        /**
         * Channels previously owned by a subscriber have been lost, this is typically as
         * a result of the subscriber being timed out or disconnection from the cluster.
         *
         * @param setLost  the set of lost channels
         */
        public void onChannelsLost(Set<Integer> setLost);
        }

    // ----- inner interface: Option ------------------------------------

    /**
     * A marker interface to indicate that a class is a valid {@link Option}
     * for a {@link Subscriber}.
     *  <p>
     *  <table>
     *    <caption>Options to use with {@link NamedTopic#createSubscriber(Subscriber.Option...)} </caption>
     *    <tr>
     *       <td valign="top"><b>Subscriber Option</b></td>
     *       <td valign="top"><b>Description</b></td>
     *    </tr>
     *    <tr>
     *       <td valign="top">{@link Name#of(String)}</td>
     *       <td valign="top">Specify subscriber group name.
     *         <br>For each value delivered to a subscriber group, only one member of the group receives the value.</td>
     *     </tr>
     *     <tr>
     *       <td valign="top">{@link Filtered#by(Filter)}</td>
     *       <td valign="top">Only {@link Subscriber#receive() Subscriber.receive()} values matching this <tt>Filter</tt>.
     *         <br>Only one <tt>Filter</tt> per subscription group.</td>
     *     </tr>
     *     <tr>
     *       <td valign="top">{@link Convert#using(ValueExtractor)}</td>
     *       <td valign="top">Convert topic value using provided {@link ValueExtractor} prior to {@link Subscriber#receive()}.
     *         <br>Only one <tt>Converter</tt> per subscription group.</td>
     *      </tr>
     *      <tr>
     *       <td valign="top">{@link CompleteOnEmpty#enabled()}</td>
     *       <td valign="top">When no values left to {@link #receive()}, returned {@link CompletableFuture#get()} returns <tt>null</tt> Element.
     *         <br>By default, returned {@link CompletableFuture#get()} blocks until next topic value is available to return.</td>
     *     </tr>
     *  </table>
     *  <p>
     *
     *  <p>
     *  All members of subscriber group share at most a single {@link Filter} and single {@link Convert Converter}. The last
     *  {@link Subscriber} joining the group and specifying these option(s) are the ones used by all subscriber group members.
     *  Note that when both a {@link Filtered#by(Filter)} and {@link Convert#using(ValueExtractor)} are specified, the {@link Filter} is
     *  applied first and only {@link NamedTopic} values matching the filter are converted before being
     *  {@link Subscriber#receive() received by the Subscriber(s)}.
     *  <p>
     *
     * @param <V>  the type of the value on the topic
     * @param <U>  the type of the value returned to the subscriber
     */
    public interface Option<V, U>
        {
        /**
         * A null implementation of an {@link Option}.
         *
         * @return a null implementation of an {@link Option}.
         * @param <V>  the type of the value on the topic
         * @param <U>  the type of the value returned to the subscriber
         */
        @SuppressWarnings("unchecked")
        static <V, U> Option<V, U> nullOption()
            {
            return NULL_OPTION;
            }

        @SuppressWarnings("rawtypes")
        Option NULL_OPTION = new Option(){};
        }

    // ----- inner interface: Name ------------------------------------------

    /**
     * The Name option is used to specify a subscriber group name.
     * <p>
     * Providing a group name allows multiple subscriber instances to share the
     * responsibility for processing the contents of the group's durable subscription.
     * Each item added to the durable subscription will only be received by one member
     * of the group, whereas each distinct subscriber group for the topic will see
     * every added item.
     * <p>
     * Naming a subscriber also allows it to outlive its subscriber instances.
     * For example a group can be created, all instances can terminate and
     * then later be recreated and pickup exactly where they left off in the
     * topic.  As the group's life is independent of its subscriber instances
     * the group must be explicitly
     * {@link NamedTopic#destroySubscriberGroup(String) destroyed}
     * in order to have the topic stop retaining values for it.
     * <p>
     * If the {@link Name} option is not specified then the subscriber will be
     * part of an anonymous group populated by no other members and will thus
     * be ensured to see the full contents of the topic and automatically
     * destroyed upon being {@link #close closed}, or when the Coherence member
     * terminates.
     */
    public static class Name<V>
            implements Option<V, V>, ExternalizableLite, PortableObject
        {
        /**
         * Default constructor for serialization.
         */
        public Name()
            {
            }

        /**
         * Construct a new group name.
         *
         * @param sName the group name
         */
        protected Name(String sName)
            {
            m_sName = sName;
            }

        /**
         * Obtain a {@link Option} that specifies a group name
         * for a {@link Subscriber}.
         *
         * @param sName  the group name to use for the {Link Subscriber}.
         *
         * @return a {@link Option} that specifies a group name
         *         for a {@link Subscriber}
         */
        public static <V> Name<V> of(String sName)
            {
            return new Name<>(sName);
            }

        /**
         * Obtain a {@link Option} that specifies a group name
         * for a {@link Subscriber}.
         *
         * @param sName  the group name to use for the {Link Subscriber}.
         *
         * @return a {@link Option} that specifies a group name
         *         for a {@link Subscriber}
         */
        public static <V> Name<V> inGroup(String sName)
            {
            return new Name<>(sName);
            }

        /**
         * Return the group name.
         *
         * @return the group name
         */
        public String getName()
            {
            return m_sName;
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_sName = ExternalizableHelper.readSafeUTF(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeSafeUTF(out, m_sName);
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_sName = in.readString(0);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeString(0, m_sName);
            }

        /**
         * The group name.
         */
        private String m_sName;
        }

    // ----- inner class: Filtered ------------------------------------------

    /**
     * The Filtered option specifies a filter that will determine which topic values a
     * subscriber is interested in receiving.  Note that all members of a subscriber group
     * share a single filter.  If members join the group using different filters then the last
     * one to join will set the filter for the group.
     *
     * @param <V>  the type of the value on the topic
     */
    public static class Filtered<V>
        implements Option<V, V>, ExternalizableLite, PortableObject
        {
        /**
         * Default constructor for serialization.
         */
        public Filtered()
            {
            }

        protected Filtered(Filter<? super V> filter)
            {
            m_filter = filter;
            }

        /**
         * Return the option's filter.
         *
         * @return the filter
         */
        public Filter<? super V> getFilter()
            {
            return m_filter;
            }

        /**
         * Return a Filtered option with the specified filter.
         *
         * @param filter  the filter
         *
         * @return the Filtered option
         */
        public static <V> Filtered<V> by(Filter<? super V> filter)
            {
            return new Filtered<>(filter);
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_filter = ExternalizableHelper.readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeObject(out, m_filter);
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_filter = in.readObject(0);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_filter);
            }

        /**
         * The filter.
         */
        private Filter<? super V> m_filter;
        }

    // ----- inner class: Convert ------------------------------------------

    /**
     * The Convert option specifies a {@link ValueExtractor} that will convert topic values that
     * a subscriber is interested in receiving prior to sending them to the subscriber.
     * Note that all members of a subscriber group share a single converter.  If members join the group using different
     * converter then the last one to join will set the converter function for the group.
     *
     * @param <V>  the type of the topic value
     * @param <U>  the type of the value returned to the subscriber
     */
    public static class Convert<V,U>
        implements Option<V, U>, ExternalizableLite, PortableObject
        {
        /**
         * Default constructor for serialization.
         */
        public Convert()
            {
            }

        protected Convert(ValueExtractor<? super V, U> extractor)
            {
            m_extractor = extractor;
            }

        /**
         * Return the option's converter function.
         *
         * @return the converter function
         */
        public ValueExtractor<? super V, U> getExtractor()
            {
            return m_extractor;
            }

        /**
         * Return a Convert option with the specified extractor.
         *
         * @param extractor  the converter extractor
         *
         * @return the Filtered option
         */
        public static <V, U> Convert<V, U> using(ValueExtractor<? super V, U> extractor)
            {
            return new Convert<>(extractor);
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_extractor = ExternalizableHelper.readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeObject(out, m_extractor);
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_extractor = in.readObject(0);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_extractor);
            }

        /**
         * The {@link ValueExtractor}.
         */
        private ValueExtractor<? super V, U> m_extractor;
        }

    // ----- inner class: CompleteOnEmpty -------------------------------

    /**
     * The CompleteOnEmpty option indicates that the {@link CompletableFuture} returned
     * from the {@link #receive} operation should complete with a <tt>null</tt> {@link Element}
     * upon identifying that the topic is or has become empty.  Without this option the
     * future will not complete until a new value becomes available.  It is supported for this
     * option to differ between members of the same subscriber group.
     */
    public static class CompleteOnEmpty<V>
            implements Option<V, V>, ExternalizableLite, PortableObject
        {
        /**
         * Default constructor for serialization.
         */
        public CompleteOnEmpty() {}

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            }

        /**
         * Obtain the Option indicating futures should complete if the topic is empty.
         *
         * @return the Option indicating futures should complete if the topic is empty
         */
        @SuppressWarnings("unchecked")
        public static <V> CompleteOnEmpty<V> enabled()
            {
            return (CompleteOnEmpty<V>) INSTANCE;
            }

        /**
         * The CompleteOnEmpty singleton.
         */
        protected static final CompleteOnEmpty<?> INSTANCE = new CompleteOnEmpty<>();
        }

    // ----- inner class: ChannelOwnershipListeners -------------------------

    /**
     * A subscriber {@link Option} that allows one or more {@link ChannelOwnershipListener listeners}
     * to be added to the subscriber, that will be notified of changes to the subscribers channel
     * ownership.
     *
     * @param <V>  the type of the elements being received from the topic
     */
    public static class ChannelOwnershipListeners<V>
            implements Option<V, V>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Private constructor to create a {@link ChannelOwnershipListeners} option.
         *
         * @param listListener the list of {@link ChannelOwnershipListener listeners} to add to the subscriber
         */
        private ChannelOwnershipListeners(List<ChannelOwnershipListener> listListener)
            {
            m_listListener = listListener;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Returns the list of {@link ChannelOwnershipListener listeners} to add to the subscriber.
         *
         * @return the list of {@link ChannelOwnershipListener listeners} to add to the subscriber
         */
        public List<ChannelOwnershipListener> getListeners()
            {
            return m_listListener;
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Create a {@link ChannelOwnershipListeners} option with one or more
         * {@link ChannelOwnershipListener listeners}.
         *
         * @param aListener the {@link ChannelOwnershipListener listeners} to add to the subscriber
         * @param <V>       the type of the elements being received from the topic
         *
         * @return a {@link ChannelOwnershipListeners} option with one or more
         *          {@link ChannelOwnershipListener listeners}
         */
        public static <V> ChannelOwnershipListeners<V> of(ChannelOwnershipListener... aListener)
            {
            return new ChannelOwnershipListeners<>(Arrays.asList(aListener));
            }

        /**
         * Create a {@link ChannelOwnershipListeners} option with one or more
         * {@link ChannelOwnershipListener listeners}.
         *
         * @param aListener the {@link ChannelOwnershipListener listeners} to add to the subscriber
         * @param <V>       the type of the elements being received from the topic
         *
         * @return a {@link ChannelOwnershipListeners} option with one or more
         *          {@link ChannelOwnershipListener listeners}
         */
        public static <V> ChannelOwnershipListeners<V> withListener(ChannelOwnershipListener... aListener)
            {
            return of(aListener);
            }

        /**
         * Create a {@link ChannelOwnershipListeners} option with zero {@link ChannelOwnershipListener listeners}.
         *
         * @param <V>  the type of the elements being received from the topic
         *
         * @return a {@link ChannelOwnershipListeners} option with one or more
         *          {@link ChannelOwnershipListener listeners}
         */
        @SuppressWarnings("unchecked")
        public static <V> ChannelOwnershipListeners<V> none()
            {
            return (ChannelOwnershipListeners<V>) EMPTY;
            }

        // ----- constants --------------------------------------------------

        /**
         * A singleton empty {@link ChannelOwnershipListeners} option.
         */
        @SuppressWarnings("rawtypes")
        private static final ChannelOwnershipListeners EMPTY = new ChannelOwnershipListeners<>(Collections.emptyList());

        // ----- data members -----------------------------------------------

        /**
         * The {@link ChannelOwnershipListener listeners} to add to the subscriber.
         */
        private final List<ChannelOwnershipListener> m_listListener;
        }

    // ----- inner class SubscriberChannel ----------------------------------

    /**
     * A representation of a topic channel within subscriber.
     */
    interface Channel
        {
        /**
         * Returns the identifier for this channel.
         *
         * @return the identifier for this channel
         */
        int getId();

        /**
         * Returns the current head position for the channel.
         *
         * @return the current head position for the channel
         */
        Position getHead();

        /**
         * Returns the last position committed by this subscriber.
         *
         * @return the last position committed by this subscriber
         */
        Position getLastCommit();

        /**
         * Return the number of completed commit requests.
         *
         * @return the number of completed commit requests
         */
        long getCommitCount();

        /**
         * Returns the last position received by this subscriber.
         *
         * @return the last position received by this subscriber
         */
        Position getLastReceived();

        /**
         * Return the number of completed receive requests.
         *
         * @return the number of completed receive requests
         */
        long getReceiveCount();

        /**
         * Returns the number of elements polled by this subscriber.
         *
         * @return the number of elements polled by this subscriber
         */
        long getPolls();

        /**
         * Returns the first position polled by this subscriber.
         *
         * @return the first position polled by this subscriber
         */
        Position getFirstPolled();

        /**
         * Returns the timestamp when the first element was polled by this subscriber.
         *
         * @return the timestamp when the first element was polled by this subscriber
         */
        long getFirstPolledTimestamp();

        /**
         * Returns the last position polled by this subscriber.
         *
         * @return the last position polled by this subscriber
         */
        Position getLastPolled();

        /**
         * Returns the timestamp when the last element was polled by this subscriber.
         *
         * @return the timestamp when the last element was polled by this subscriber
         */
        long getLastPolledTimestamp();

        /**
         * Returns {@code true} if the channel is empty.
         *
         * @return  {@code true} if the channel is empty
         */
        boolean isEmpty();

        /**
         * Returns {@code true} if the channel is owned by this subscriber.
         *
         * @return  {@code true} if the channel is owned by this subscriber
         */
        boolean isOwned();

        /**
         * Returns a numeric representation of if the channel is owned by this subscriber
         * where {@code 1} represents true and {@code 0} represents false.
         *
         * @return a numeric representation of if the channel is owned by this subscriber
         * where {@code 1} represents true and {@code 0} represents false
         */
        int getOwnedCode();

        /**
         * Return the number of completed receive requests.
         *
         * @return the number of completed receive requests
         */
        long getReceived();

        /**
         * Return the mean rate of completed receive requests.
         *
         * @return the mean rate of completed receive requests
         */
        double getReceivedMeanRate();

        /**
         * Return the one-minute rate of completed receive requests.
         *
         * @return the one-minute rate of completed receive requests
         */
        double getReceivedOneMinuteRate();

        /**
         * Return the five-minute rate of completed receive requests.
         *
         * @return the five-minute rate of completed receive requests
         */
        double getReceivedFiveMinuteRate();

        /**
         * Return the fifteen-minute rate of completed receive requests.
         *
         * @return the fifteen-minute rate of completed receive requests
         */
        double getReceivedFifteenMinuteRate();

        /**
         * A default empty channel implementation.
         */
        class EmptyChannel implements Channel
            {
            public EmptyChannel(int nId)
                {
                f_nId = nId;
                }

            @Override
            public int getId()
                {
                return f_nId;
                }

            @Override
            public Position getHead()
                {
                return PagedPosition.NULL_POSITION;
                }

            @Override
            public Position getLastCommit()
                {
                return PagedPosition.NULL_POSITION;
                }

            @Override
            public long getCommitCount()
                {
                return 0;
                }

            @Override
            public Position getLastReceived()
                {
                return PagedPosition.NULL_POSITION;
                }

            @Override
            public long getReceiveCount()
                {
                return 0;
                }

            @Override
            public long getPolls()
                {
                return 0;
                }

            @Override
            public Position getFirstPolled()
                {
                return PagedPosition.NULL_POSITION;
                }

            @Override
            public long getFirstPolledTimestamp()
                {
                return 0L;
                }

            @Override
            public Position getLastPolled()
                {
                return PagedPosition.NULL_POSITION;
                }

            @Override
            public long getLastPolledTimestamp()
                {
                return 0L;
                }

            @Override
            public boolean isEmpty()
                {
                return true;
                }

            @Override
            public boolean isOwned()
                {
                return false;
                }

            @Override
            public int getOwnedCode()
                {
                return 0;
                }

            @Override
            public long getReceived()
                {
                return 0L;
                }

            @Override
            public double getReceivedMeanRate()
                {
                return 0.0d;
                }

            @Override
            public double getReceivedOneMinuteRate()
                {
                return 0.0d;
                }

            @Override
            public double getReceivedFiveMinuteRate()
                {
                return 0.0d;
                }

            @Override
            public double getReceivedFifteenMinuteRate()
                {
                return 0.0d;
                }

            // ----- data members -------------------------------------------

            private final int f_nId;
            }
        }
    }
