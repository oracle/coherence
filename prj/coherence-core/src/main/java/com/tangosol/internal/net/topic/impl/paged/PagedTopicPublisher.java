/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.oracle.coherence.common.base.Converter;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.util.MemorySize;
import com.oracle.coherence.common.util.Options;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.net.DebouncedFlowControl;

import com.tangosol.internal.net.topic.impl.paged.model.NotificationKey;
import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;

import com.tangosol.internal.util.DaemonPool;
import com.tangosol.internal.util.Daemons;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.Cluster;
import com.tangosol.net.FlowControl;
import com.tangosol.net.NamedCache;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Publisher;

import com.tangosol.io.Serializer;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapListener;

import com.tangosol.util.filter.InKeySetFilter;

import com.tangosol.util.listener.SimpleMapListener;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.tangosol.internal.net.topic.impl.paged.PagedTopicPublisher.FlushMode.*;

/**
 * A {@link PagedTopicPublisher} is a Topic implementation which publishes topic values
 * into pages stored within ordered channels on top of a partitioned cache.
 * <p>
 * This implementation uses various underlying {@link NamedCache} instances to
 * hold the data for the topic.
 * <p>
 * All interactions with the topic are via {@link InvocableMap.EntryProcessor}s
 * that run against keys in the page cache. This ensures that a page will be
 * locked for operations that add or remove from the topic and that the operations
 * happen against the correct head and tail pages.
 *
 * @author jk/mf 2015.05.15
 * @since Coherence 14.1.1
 */
public class PagedTopicPublisher<V>
        implements Publisher<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link PagedTopicPublisher}.
     *
     * @param pagedTopicCaches  the {@link PagedTopicCaches} managing this topic's caches
     * @param opts              the {@link Option}s controlling this {@link PagedTopicPublisher}
     */
    @SafeVarargs
    @SuppressWarnings({"rawtypes", "unchecked"})
    public PagedTopicPublisher(NamedTopic<V> topic, PagedTopicCaches pagedTopicCaches, Option<? super V>... opts)
        {
        m_caches = Objects.requireNonNull(pagedTopicCaches,"The PagedTopicCaches parameter cannot be null");
        m_topic  = topic;

        registerDeactivationListener();

        Serializer      serializer = pagedTopicCaches.getSerializer();
        Cluster         cluster    = m_caches.getService().getCluster();
        Options<Option> options    = Options.from(Option.class, opts);

        f_nId                 = createId(System.identityHashCode(this), cluster.getLocalMember().getId());
        f_convValueToBinary   = (value) -> ExternalizableHelper.toBinary(value, serializer);
        f_sTopicName          = pagedTopicCaches.getTopicName();
        f_nNotifyPostFull     = options.contains(FailOnFull.class) ? 0 : System.identityHashCode(this);
        f_funcOrder           = computeOrderByOption(options);
        f_onFailure           = options.get(OnFailure.class);

        ChannelCount channelCount = options.get(ChannelCount.class, ChannelCount.USE_CONFIGURED);
        int          cChannel     = channelCount.isUseConfigured()
                                            ? topic.getChannelCount()
                                            : channelCount.getChannelCount();

        long cbBatch  = m_caches.getDependencies().getMaxBatchSizeBytes();

        f_aChannel          = new PagedTopicChannelPublisher[cChannel];
        f_setOfferedChannel = new BitSet(cChannel);

        DebouncedFlowControl backlog = new DebouncedFlowControl(
                /*normal*/ cbBatch * 2, /*excessive*/ cbBatch * 3,
                (l) -> new MemorySize(Math.abs(l)).toString()); // attempt to always have at least one batch worth
        f_flowControl = backlog;

        DefaultDaemonPoolDependencies dependencies = new DefaultDaemonPoolDependencies();
        dependencies.setName("Publisher-" + m_caches.getTopicName() + "-" + f_nId);
        dependencies.setThreadCountMin(1);
        dependencies.setThreadCount(1);
        dependencies.setThreadCountMax(Integer.MAX_VALUE);

        f_daemon   = Daemons.newDaemonPool(dependencies);
        f_executor = f_daemon::add;
        f_daemon.start();

        for (int nChannel = 0; nChannel < cChannel; ++nChannel)
            {
            f_aChannel[nChannel]
                    = new PagedTopicChannelPublisher(f_nId, nChannel, cChannel, m_caches, f_nNotifyPostFull, backlog, f_daemon, this::handlePublishError);
            }

        f_listenerNotification = new SimpleMapListener<NotificationKey, int[]>()
                            .addDeleteHandler(evt -> onNotification(evt.getOldValue()))
                            .synchronous();
        f_filterListenerNotification = f_nNotifyPostFull == 0
                                       ? null
                                       : new InKeySetFilter<>(/*filter*/ null, pagedTopicCaches.getPartitionNotifierSet(f_nNotifyPostFull));

        if (f_nNotifyPostFull != 0)
            {
            // register a publisher listener in each partition, we do this even if the config isn't declared
            // with high-units as the server may have an alternate config
            pagedTopicCaches.Notifications.addMapListener(f_listenerNotification, f_filterListenerNotification, /*fLite*/ false);
            }

        m_state = State.Active;
        }

    // ----- TopicPublisher methods -----------------------------------------

    /**
     * Specifies whether the publisher is active.
     *
     * @return true if the publisher is active; false otherwise
     */
    public boolean isActive()
        {
        return m_state == State.Active || m_state == State.Disconnected;
        }

    // ----- NamedTopic.Publisher methods -----------------------------------

    @Override
    public CompletableFuture<Status> publish(V value)
        {
        ensureActive();

        Throwable thrown = null;

        for (int attempt = 0; attempt < 2; attempt++)
            {
            try
                {
                PagedTopicChannelPublisher channelPublisher = ensureChannelPublisher(value);
                CompletableFuture<Status>  future           = channelPublisher.publish(f_convValueToBinary.convert(value));

                future.handleAsync((status, error) -> handlePublished(channelPublisher.getChannel()), f_executor);

                return future;
                }
            catch (IllegalStateException e)
                {
                if (thrown == null)
                    {
                    thrown = e;
                    }
                else
                    {
                    thrown.addSuppressed(e);
                    }
                }
            ensureActive();
            }
        throw Exceptions.ensureRuntimeException(thrown);
        }

    @Override
    public FlowControl getFlowControl()
        {
        return f_flowControl;
        }

    @Override
    public CompletableFuture<Void> flush()
        {
        ensureActive();
        return flushInternal(FLUSH);
        }

    @Override
    public void close()
        {
        if (isActive())
            {
            closeInternal(false);
            }
        }

    @Override
    public void onClose(Runnable action)
        {
        f_listOnCloseActions.add(action);
        }

    @Override
    public int getChannelCount()
        {
        return m_topic.getChannelCount();
        }

    @Override
    public NamedTopic<V> getNamedTopic()
        {
        return m_topic;
        }

    // ----- accessor methods -----------------------------------------------

    /**
     * Obtain the name of the {@link NamedTopic} that this publisher offers elements to.
     *
     * @return the name of the {@link NamedTopic} that this publisher offers elements to
     */
    public String getName()
        {
        return f_sTopicName;
        }

    /**
     * Method called to notify this publisher that space is now available in a previously full topic.
     *
     * @param anChannel  the free channels
     */
    protected void onNotification(int[] anChannel)
        {
        for (int nChannel : anChannel)
            {
            PagedTopicChannelPublisher channel = f_aChannel[nChannel];
            channel.onNotification();
            }
        }

    // ----- Object methods -------------------------------------------------

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

        PagedTopicPublisher<?> that = (PagedTopicPublisher<?>) o;

        return m_caches.equals(that.m_caches);

        }

    @Override
    public int hashCode()
        {
        return m_caches.hashCode();
        }

    @Override
    public String toString()
        {
        PagedTopicCaches caches = m_caches;
        if (caches == null)
            {
            return getClass().getSimpleName() + "(inactive)";
            }

        int cChannels = f_setOfferedChannel.cardinality();

        StringBuilder buf = new StringBuilder(getClass().getSimpleName())
                .append("(topic=").append(caches.getTopicName())
                .append(", id=").append(f_nId)
                .append(", orderBy=").append(f_funcOrder)
                .append(", backlog=").append(f_flowControl)
                .append(", channels=").append(cChannels);


        if (!f_setOfferedChannel.isEmpty())
            {
            for (PagedTopicChannelPublisher channel : f_aChannel)
                {
                int nChannel = channel.getChannel();
                if (f_setOfferedChannel.get(nChannel))
                    {
                    buf.append("  ").append(nChannel).append(": ").append(channel);
                    }
                }
            f_setOfferedChannel.clear();
            }

        return buf.toString();
        }

    // ----- helper methods -------------------------------------------------


    /**
     * Handle a publish request completion.
     *
     * @param nChannel  the channel the message was published to
     */
    protected Void handlePublished(int nChannel)
        {
        f_setOfferedChannel.set(nChannel);
        return null;
        }

    /**
     * Handle a publishing error.
     *
     * @param error     the error that occurred
     * @param nChannel  the channel causing the error
     */
    protected void handlePublishError(Throwable error, int nChannel)
        {
        if (error != null)
            {
            switch (f_onFailure)
                {
                case Stop:
                    // Stop the publisher.
                    Logger.fine("Closing publisher due to publishing error from channel " + nChannel + ", " + error);
                    // we need to do the actual close async as we're on the service thread here
                    // setting the state to OnError will stop us accepting further messages to publish
                    m_state = State.OnError;
                    CompletableFuture.runAsync(() -> closeInternal(false), Daemons.commonPool());
                    break;
                case Continue:
                    // Do nothing as the individual errors will
                    // already have been handled
                    Logger.finer("Publisher set to continue on error, ignoring publishing error from channel " + nChannel + ", " + error);
                    break;
                }
            }
        }

    /**
     * Returns the {@link PagedTopicChannelPublisher} to publish a value to.
     *
     * @param value  the value to publish
     *
     * @return the {@link PagedTopicChannelPublisher} to publish a value to
     */
    private PagedTopicChannelPublisher ensureChannelPublisher(V value)
        {
        int nOrder = value instanceof Orderable
                ? ((Orderable) value).getOrderId()
                : f_funcOrder.getOrderId(value);

        int nChannel = Base.mod(nOrder, f_aChannel.length);
        PagedTopicChannelPublisher publisher = f_aChannel[nChannel];

        if (!publisher.isActive())
            {
            // This publisher has failed/closed

            if (f_onFailure == OnFailure.Stop)
                {
                // The error action is to Stop, so we should be closed
                closeInternal(false);
                throw new IllegalStateException("This publisher is no longer active");
                }

            f_lock.lock();
            try
                {
                // create a new publisher for the closed channel
                publisher = f_aChannel[nChannel];
                if (isActive() && !publisher.isActive())
                    {
                    m_caches.ensureConnected();
                    Logger.finer("Restarted publisher for channel " + nChannel + " topic " + m_caches.getTopicName() + " publisher " + f_nId);
                    publisher = f_aChannel[nChannel] = new PagedTopicChannelPublisher(f_nId, nChannel, f_aChannel.length, m_caches,
                            f_nNotifyPostFull, f_flowControl, f_daemon, this::handlePublishError);
                    }
                }
            finally
                {
                f_lock.unlock();
                }
            }

        return publisher;
        }

    /**
     * Compute the OrderBy option for sent messages from this publisher.
     * Defaults to {@link OrderByThread} when no {@link OrderBy} option specified.
     *
     * @param options  All Options for this Publisher.
     *
     * @return {@link OrderBy} option for this publisher
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private OrderBy computeOrderByOption(Options options)
        {
        Iterator<OrderBy> iter = options.getInstancesOf(OrderBy.class).iterator();

        return iter.hasNext() ? iter.next() : OrderBy.thread();
        }

    /**
     * Ensure that this publisher is active.
     *
     * @throws IllegalStateException if not active
     */
    private void ensureActive()
        {
        if (!isActive())
            {
            throw new IllegalStateException("This publisher is no longer active");
            }
        }

    /**
     * Close this {@link PagedTopicPublisher}.
     * <p>
     * The {@link Option}s passed to this method will override any options
     * that were set when this publisher was created.
     *
     * @param fDestroyed  {@code true} if this method is being called in response
     *                    to the topic being destroyed, in which case there is no
     *                    need to clean up cached data
     */
    protected void closeInternal(boolean fDestroyed)
        {
        if (m_caches == null || m_state == State.Closing || m_state == State.Closed)
            {
            // already closed or closing
            return;
            }

        f_lock.lock();
        try
            {
            if (m_caches == null || m_state == State.Closing || m_state == State.Closed)
                {
                // already closed
                return;
                }

            m_state = State.Closing;

            try
                {
                if (!fDestroyed)
                    {
                    unregisterDeactivationListener();

                    if (f_nNotifyPostFull != 0)
                        {
                        // unregister the publisher listener in each partition
                        PagedTopicCaches caches = m_caches;

                        if (caches.Notifications.isActive())
                            {
                            caches.Notifications.removeMapListener(f_listenerNotification, f_filterListenerNotification);
                            }
                        }
                    }

                // Stop the channel publishers
                for (PagedTopicChannelPublisher channel : f_aChannel)
                    {
                    channel.stop();
                    }

                // flush this publisher to wait for all the outstanding
                // add operations to complete (or to be cancelled if we're destroying)
                try
                    {
                    flushInternal(fDestroyed ? FLUSH_DESTROY : FLUSH).get(CLOSE_TIMEOUT_SECS, TimeUnit.SECONDS);
                    }
                catch (TimeoutException e)
                    {
                    // too long to wait for completion; force all outstanding futures to complete exceptionally
                    flushInternal(FLUSH_CLOSE_EXCEPTIONALLY).join();
                    Logger.warn("Publisher.close: timeout after waiting " + CLOSE_TIMEOUT_SECS
                            + " seconds for completion with flush.join(), forcing complete exceptionally");
                    }
                catch (ExecutionException | InterruptedException e)
                    {
                    // ignore
                    }

                // Close the channel publishers
                for (PagedTopicChannelPublisher channel : f_aChannel)
                    {
                    channel.close();
                    }
                }
            finally
                {
                // clean up
                m_caches = null;
                Arrays.fill(f_aChannel, null);

                f_listOnCloseActions.forEach(action ->
                    {
                    try
                        {
                        action.run();
                        }
                    catch (Throwable t)
                        {
                        Logger.fine(this.getClass().getName() + ".close(): handled onClose exception: " +
                            t.getClass().getCanonicalName() + ": " + t.getMessage());
                        }
                    });

                f_daemon.shutdown();
                m_state = State.Closed;
                }
            }
        finally
            {
            f_lock.unlock();
            }
        }

    /**
     * Obtain a {@link CompletableFuture} that will be complete when
     * all the currently outstanding add operations complete.
     * <p>
     * If this method is called in response to a topic destroy then the
     * outstanding operations will be completed with an exception as the underlying
     * topic caches have been destroyed, so they can never complete normally.
     * <p>
     * if this method is called in response to a timeout waiting for flush to complete normally,
     * indicated by {@link FlushMode#FLUSH_CLOSE_EXCEPTIONALLY}, complete exceptionally all outstanding
     * asynchronous operations so close finishes.
     * <p>
     * The returned {@link CompletableFuture} will always complete
     * normally, even if the outstanding operations complete exceptionally.
     *
     * @param mode  {@link FlushMode} flush mode to use
     *
     * @return a {@link CompletableFuture} that will be completed when
     *         all the currently outstanding add operations are complete
     */
    private CompletableFuture<Void> flushInternal(FlushMode mode)
        {
        CompletableFuture<?>[] aFuture = new CompletableFuture[f_aChannel.length];
        for (int i = 0; i < aFuture.length; ++i)
            {
            aFuture[i] = f_aChannel[i].flush(mode);
            }
        return CompletableFuture.allOf(aFuture);
        }

    /**
     * Instantiate and register a DeactivationListener with the topic data cache.
     */
    protected void registerDeactivationListener()
        {
        try
            {
            m_caches.addListener(f_listenerDeactivation);
            }
        catch (RuntimeException e)
            {
            // intentionally empty
            }
        }

    /**
     * Unregister cache deactivation listener.
     */
    protected void unregisterDeactivationListener()
        {
        try
            {
            m_caches.removeListener(f_listenerDeactivation);
            }
        catch (RuntimeException e)
            {
            // intentionally empty
            }
        }

    /**
     * Create a publisher identifier.
     *
     * @param nNotificationId  the publisher's notification identifier
     * @param nMemberId        the local member id
     *
     * @return a publisher identifier
     */
    static long createId(long nNotificationId, long nMemberId)
        {
        return (nMemberId << 32) | (nNotificationId & 0xFFFFFFFFL);
        }

    // ----- inner class: DeactivationListener ------------------------------

    /**
     * A {@link PagedTopicCaches.Listener} to detect the subscribed topic
     * being released, destroyed or disconnected.
     */
    protected class DeactivationListener
            implements PagedTopicCaches.Listener
        {
        @Override
        public void onConnect()
            {
            }

        @Override
        public void onDisconnect()
            {
            }

        @Override
        public void onDestroy()
            {
            if (isActive())
                {
                // destroy/disconnect event
                Logger.fine("Detected destroy of topic "
                                    + f_sTopicName + ", closing publisher "
                                    + PagedTopicPublisher.this);
                closeInternal(true);
                }
            }

        @Override
        public void onRelease()
            {
            if (isActive())
                {
                // destroy/disconnect event
                Logger.fine("Detected release of topic "
                                    + f_sTopicName + ", closing publisher "
                                    + PagedTopicPublisher.this);
                closeInternal(false);
                }
            }
        }

    // ----- inner class: FlushMode ----------------------------------------

    /**
     * An enum representing different flush modes for a publisher.
     */
    enum FlushMode
        {
        /**
         *  Wait for all outstanding asynchronous operations to complete.
         */
        FLUSH,

        /**
         * Cancel all outstanding asynchronous operations due to topic being destroyed.
         */
        FLUSH_DESTROY,

        /**
         * Complete exceptionally all outstanding asynchronous operations due to timeout during initial {@link #FLUSH} during close.
         */
        FLUSH_CLOSE_EXCEPTIONALLY
        }

    // ----- inner enum: State ----------------------------------------------

    /**
     * An enum representing the {@link Publisher} state.
     */
    public enum State
        {
        /**
         * The publisher is active.
         */
        Active,
        /**
         * The publisher is closing.
         */
        Closing,
        /**
         * The publisher is closed.
         */
        Closed,
        /**
         * The publisher is disconnected from storage.
         */
        Disconnected,
        /**
         * The publisher is closing due to an error.
         */
        OnError
        }

    // ----- inner class: PublishedMetadata --------------------------------

    /**
     * An implementation of {@link Status}.
     */
    protected static class PublishedStatus
            implements Status
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a {@link PublishedStatus}.
         *
         * @param nChannel  the channel the element was published to
         * @param lPage     the page the element was published to
         * @param nOffset   the offset the element was published to
         */
        protected PublishedStatus(int nChannel, long lPage, int nOffset)
            {
            f_nChannel = nChannel;
            f_position = new PagedPosition(lPage, nOffset);
            }

        // ----- ElementMetadata methods ------------------------------------

        @Override
        public int getChannel()
            {
            return f_nChannel;
            }

        @Override
        public Position getPosition()
            {
            return f_position;
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public String toString()
            {
            return "PublishedStatus(" +
                    "channel=" + f_nChannel +
                    ", position=" + f_position +
                    ')';
            }

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
            PublishedStatus that = (PublishedStatus) o;
            return f_nChannel == that.f_nChannel && Objects.equals(f_position, that.f_position);
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(f_nChannel, f_position);
            }

        // ----- data members -----------------------------------------------

        /**
         * The channel number.
         */
        private final int f_nChannel;

        /**
         * The position that the element was published to.
         */
        private final PagedPosition f_position;
        }

    // ----- inner class: ChannelCount --------------------------------------

    /**
     * This option controls the channel count for a {@link Publisher}, which
     * may be different to the channel count configured for the topic.
     */
    public static class ChannelCount
            implements Option<Object>, ExternalizableLite, PortableObject
        {
        /**
         * Default constructor for serialization.
         */
        public ChannelCount()
            {
            this(-1);
            }

        /**
         * Create a {@link ChannelCount} option.
         *
         * @param cChannel  the channel count (a value less than zero will
         *                  use the configured channel count)
         */
        public ChannelCount(int cChannel)
            {
            m_cChannel = cChannel;
            }

        /**
         * Whether the publisher should use the configured channel count.
         *
         * @return {code true} if the publisher should use the configured channel count
         */
        public boolean isUseConfigured()
            {
            return m_cChannel < 0;
            }

        /**
         * Return the channel count the publisher should use.
         *
         * @return  the channel count the publisher should use
         */
        public int getChannelCount()
            {
            return m_cChannel;
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_cChannel = in.readInt();
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            out.writeInt(m_cChannel);
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_cChannel = in.readInt(0);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeInt(0, m_cChannel);
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Return a {@link ChannelCount} option with the specified channel count.
         *
         * @param cChannel  the channel count the publisher should use
         *
         * @return a {@link ChannelCount} option with the specified channel count
         */
        public static ChannelCount of(int cChannel)
            {
            return new ChannelCount(cChannel);
            }

        /**
         * Return a {@link ChannelCount} option to make a publisher to use
         * the configured channel count.
         *
         * @return a {@link ChannelCount} option to make a publisher to use
         *         the configured channel count
         */
        @Options.Default
        public static ChannelCount useConfigured()
            {
            return USE_CONFIGURED;
            }

        // ----- constants --------------------------------------------------

        /**
         * A singleton {@link ChannelCount} option to make a publisher to use
         * the configured channel count.
         */
        public static final ChannelCount USE_CONFIGURED = new ChannelCount(-1);

        // ----- data members -----------------------------------------------

        /**
         * The channel count to use for the publisher.
         */
        private int m_cChannel;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Publisher close timeout on first flush attempt. After this time is exceeded, all outstanding asynchronous operations will be completed exceptionally.
     */
    public static final long CLOSE_TIMEOUT_SECS = TimeUnit.MILLISECONDS.toSeconds(Base.parseTime(Config.getProperty("coherence.topic.publisher.close.timeout", "30s"), Base.UNIT_S));

    // ----- data members ---------------------------------------------------

    /**
     * The current state of the {@link Publisher}.
     */
    private volatile State m_state;

    /**
     * The underlying {@link NamedTopic} being published to.
     */
    private final NamedTopic<V> m_topic;

    /**
     * The {@link PagedTopicCaches} instance managing the caches backing this topic.
     */
    private PagedTopicCaches m_caches;

    /**
     * The name of the topic.
     */
    private final String f_sTopicName;

    /**
     * The converter that will convert values being offered to {@link Binary} instances.
     */
    private final Converter<V, Binary> f_convValueToBinary;

    /**
     * The post full notifier.
     */
    private final int f_nNotifyPostFull;

    /**
     * The ordering function.
     */
    private final OrderBy<V> f_funcOrder;

    /**
     * The publisher flow control.
     */
    private final DebouncedFlowControl f_flowControl;

    /**
     * Channel array.
     */
    protected final PagedTopicChannelPublisher[] f_aChannel;

    /**
     * The set of channels which elements have been offered to since the last @{link #toString} call.
     */
    private final BitSet f_setOfferedChannel;

    /**
     * The NamedCache deactivation listener.
     */
    private final DeactivationListener f_listenerDeactivation = new DeactivationListener();

    /**
     * The listener used to notify this publisher that previously full topics now have more space.
     */
    private final MapListener<NotificationKey, int[]> f_listenerNotification;

    /**
     * Filter used with f_listenerNotification.
     */
    private final Filter<int[]> f_filterListenerNotification;

    /**
     * A {@link List} of actions to run when this publisher closes.
     */
    private final List<Runnable> f_listOnCloseActions = new ArrayList<>();

    /**
     * The {@link DaemonPool} used to complete published message futures so that they are not on the service thread.
     */
    private final DaemonPool f_daemon;

    /**
     * The {@link Executor} used to complete async operations (this will wrap {@link #f_daemon}).
     */
    private final Executor f_executor;


    /**
     * A unique identifier for this publisher.
     */
    private final long f_nId;

    /**
     * The action to take when a publish request fails.
     */
    private final OnFailure f_onFailure;

    /**
     * A lock to control access to internal state.
     */
    private final Lock f_lock = new ReentrantLock();
    }
