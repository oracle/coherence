/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.oracle.coherence.common.base.Converter;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.util.MemorySize;
import com.oracle.coherence.common.util.Options;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.net.DebouncedFlowControl;

import com.tangosol.internal.util.DaemonPool;
import com.tangosol.internal.util.Daemons;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.Serializer;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.FlowControl;
import com.tangosol.net.TopicService;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.TopicDependencies;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * A {@link NamedTopicPublisher} is a {@link Publisher} implementation which
 * publishes topic values into a topic within ordered channels.
 * <p>
 * This class provides the functionality of a {@link Publisher} and uses a
 * {@link PublisherConnector} to provides a connection to clustered topic
 * resources.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class NamedTopicPublisher<V>
        implements Publisher<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link NamedTopicPublisher}.
     *
     * @param opts   the {@link Option}s controlling this {@link NamedTopicPublisher}
     */
    @SuppressWarnings("unchecked")
    public NamedTopicPublisher(NamedTopic<V> topic, PublisherConnector<V> connector, Option<? super V>[] opts)
        {
        f_topic     = topic;
        f_connector = Objects.requireNonNull(connector);

        f_listener = new ConnectorListener();
        f_connector.addListener(f_listener);

        TopicDependencies dependencies  = connector.getTopicDependencies();
        long              cbBatch       = connector.getMaxBatchSizeBytes();
        TopicService      service       = connector.getTopicService();
        OptionSet<V>      options       = Publisher.optionsFrom(opts);
        Serializer        serializer    = service.getSerializer();

        f_nId                 = connector.getId();
        f_sTopicName          = connector.getTopicName();
        f_calculator          = dependencies.getElementCalculator();
        f_funcOrder           = options.getOrderBy();
        f_nNotifyPostFull     = options.getNotifyPostFull(connector);
        f_onFailure           = options.getOnFailure();
        f_convValueToBinary   = options.getConverter()
                                    .orElse(value -> ExternalizableHelper.toBinary(value, serializer));

        int cChannel = connector.getChannelCount();
        f_aChannel          = new NamedTopicPublisherChannel[cChannel];
        f_setOfferedChannel = new BitSet(cChannel);

        DebouncedFlowControl backlog = new DebouncedFlowControl(
                /*normal*/ cbBatch * 2, /*excessive*/ cbBatch * 3,
                (l) -> new MemorySize(Math.abs(l)).toString()); // attempt to always have at least one batch worth
        f_flowControl = backlog;

        DefaultDaemonPoolDependencies depsPool = new DefaultDaemonPoolDependencies();
        depsPool.setName("Publisher-" + f_sTopicName + "-" + f_nId);
        depsPool.setThreadCountMin(1);
        depsPool.setThreadCount(1);
        depsPool.setThreadCountMax(Integer.MAX_VALUE);

        f_daemon   = Daemons.newDaemonPool(depsPool);
        f_executor = f_daemon::add;
        f_daemon.start();

        for (int nChannel = 0; nChannel < cChannel; ++nChannel)
            {
            PublisherChannelConnector<V> connChannel = f_connector.createChannelConnector(nChannel);
            f_aChannel[nChannel] = new NamedTopicPublisherChannel<>(connChannel, f_nId, nChannel, f_nNotifyPostFull, backlog, f_daemon, serializer, f_calculator, this::handlePublishError);
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

    @Override
    public long getId()
        {
        return f_nId;
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
                NamedTopicPublisherChannel<V> channelPublisher = ensureChannelPublisher(value);
                CompletableFuture<Status>     future           = channelPublisher.publish(f_convValueToBinary.convert(value));
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
        return flushInternal(FlushMode.FLUSH);
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
        return f_connector.getChannelCount();
        }

    @Override
    public NamedTopic<V> getNamedTopic()
        {
        return f_topic;
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
            NamedTopicPublisherChannel<V> channel = f_aChannel[nChannel];
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

        NamedTopicPublisher<?> that = (NamedTopicPublisher<?>) o;
        return f_connector.equals(that.f_connector);
        }

    @Override
    public int hashCode()
        {
        return f_connector.hashCode();
        }

    @Override
    public String toString()
        {
        if (!isActive())
            {
            return getClass().getSimpleName() + "(inactive)";
            }

        int cChannels = f_setOfferedChannel.cardinality();

        StringBuilder buf = new StringBuilder(getClass().getSimpleName())
                .append("(topic=").append(f_sTopicName)
                .append(", id=").append(f_nId)
                .append(", orderBy=").append(f_funcOrder)
                .append(", backlog=").append(f_flowControl)
                .append(", channels=").append(cChannels);

        if (!f_setOfferedChannel.isEmpty())
            {
            for (NamedTopicPublisherChannel<V> channel : f_aChannel)
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
     * Returns the {@link NamedTopicPublisherChannel} to publish a value to.
     *
     * @param value  the value to publish
     *
     * @return the publisher to publish a value to
     */
    private NamedTopicPublisherChannel<V> ensureChannelPublisher(V value)
        {
        int nOrder = value instanceof Orderable
                ? ((Orderable) value).getOrderId()
                : f_funcOrder.getOrderId(value);

        int                           nChannel  = Base.mod(nOrder, f_aChannel.length);
        NamedTopicPublisherChannel<V> publisher = f_aChannel[nChannel];

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
                    f_connector.ensureConnected();
                    Logger.finer("Restarted publisher for channel " + nChannel + " topic " + f_sTopicName + " publisher " + f_nId);
                    PublisherChannelConnector<V> connChannel = f_connector.createChannelConnector(nChannel);
                    Serializer                   serializer  = f_connector.getTopicService().getSerializer();

                    publisher = f_aChannel[nChannel] = new NamedTopicPublisherChannel<>(connChannel, f_nId, nChannel,
                            f_nNotifyPostFull, f_flowControl, f_daemon, serializer, f_calculator, this::handlePublishError);
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
     * Ensure that this publisher is active.
     *
     * @throws IllegalStateException if not active
     */
    private void ensureActive()
        {
        if (!isActive())
            {
            throw new IllegalStateException("This publisher is no longer active (state=" + m_state + ")");
            }
        }

    /**
     * Close this {@link NamedTopicPublisher}.
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
        if (m_state == State.Closing || m_state == State.Closed)
            {
            // already closed or closing
            return;
            }

        f_lock.lock();
        try
            {
            if (m_state == State.Closing || m_state == State.Closed)
                {
                // already closed
                return;
                }

            m_state = State.Closing;

            try
                {
                if (!fDestroyed)
                    {
                    f_connector.removeListener(f_listener);
                    }

                // Stop the channel publishers
                for (NamedTopicPublisherChannel<V> channel : f_aChannel)
                    {
                    channel.stop();
                    }

                // flush this publisher to wait for all the outstanding
                // add operations to complete (or to be cancelled if we're destroying)
                try
                    {
                    flushInternal(fDestroyed ? FlushMode.FLUSH_DESTROY : FlushMode.FLUSH).get(CLOSE_TIMEOUT_SECS, TimeUnit.SECONDS);
                    }
                catch (TimeoutException e)
                    {
                    // too long to wait for completion; force all outstanding futures to complete exceptionally
                    flushInternal(FlushMode.FLUSH_CLOSE_EXCEPTIONALLY).join();
                    Logger.warn("Publisher.close: timeout after waiting " + CLOSE_TIMEOUT_SECS
                            + " seconds for completion with flush.join(), forcing complete exceptionally");
                    }
                catch (ExecutionException | InterruptedException e)
                    {
                    // ignore
                    }

                // Close the channel publishers
                for (NamedTopicPublisherChannel<V> channel : f_aChannel)
                    {
                    channel.close();
                    }

                f_connector.close();
                }
            finally
                {
                // clean up
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

    // ----- inner class: WithIdentifier ---------------------------------

    /**
     * A publisher option to set the publisher identifier.
     *
     * @param <V>  the type of published value
     */
    public static class WithIdentifier<V>
            implements Publisher.Option<V>
        {
        public WithIdentifier(long nId)
            {
            f_nId = nId;
            }

        public long getId()
            {
            return f_nId;
            }

        // ----- data members -----------------------------------------------

        /**
         * The publisher identifier.
         */
        private final long f_nId;
        }

    // ----- inner class: WithConverter -------------------------------------

    /**
     * A publisher option to set the value serializer.
     *
     * @param <V>  the type of published value
     */
    public static class WithConverter<V>
            implements Publisher.Option<V>
        {
        public WithConverter(Converter<V, Binary> converter)
            {
            f_converter = converter;
            }

        public Converter<V, Binary> getConverter()
            {
            return f_converter;
            }

        // ----- data members -----------------------------------------------

        /**
         * The publisher converter.
         */
        private final Converter<V, Binary> f_converter;

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


    public static ChannelCount withChannelCount(int cChannel)
        {
        if (cChannel <= 0)
            {
            throw new IllegalArgumentException("channel count must be positive");
            }
        return new ChannelCount(cChannel);
        }

    // ----- inner class: PublisherListener ---------------------------------

    /**
     * A listener that receives events related to a {@link PublisherConnector}.
     */
    public interface PublisherListener
            extends BaseTopicEvent.Listener<PublisherEvent>
        {
        /**
         * Receives {@link PublisherEvent events}.
         *
         * @param evt the {@link PublisherEvent events} from the publisher
         */
        void onEvent(PublisherEvent evt);
        }

    // ----- inner class: PublisherEvent ------------------------------------

    /**
     * An event related to a {@link PublisherConnector}.
     */
    public static class PublisherEvent
            extends BaseTopicEvent<PublisherConnector<?>, PublisherEvent.Type, PublisherListener>
        {
        public PublisherEvent(PublisherConnector<?> source, Type type)
            {
            this(source, type, null);
            }

        public PublisherEvent(PublisherConnector<?> source, Type type, int[] anChannel)
            {
            super(source, type);
            m_anChannel = anChannel;
            }

        // ----- accessors ------------------------------------------------------

        /**
         * Obtain the channels this event applies to.
         *
         * @return the channels this event applies to
         */
        public int[] getChannels()
            {
            return m_anChannel;
            }

        /**
         * Create a copy of this event with a different source.
         *
         * @param connector  the new event source
         * @param <V>        the type of values published by the publisher
         *
         * @return a copy of this event with a different source
         */
        public <V> PublisherEvent withNewSource(PublisherConnector<V> connector)
            {
            return new PublisherEvent(connector, m_type, m_anChannel);
            }

        /**
         * Return a {@link Type} from an ordinal
         *
         * @param n the ordinal
         *
         * @return {@link Type} from an ordinal
         */
        public static Type typeFromOrdinal(int n)
            {
            Type[] aType = Type.values();
            if (n >=0 && n < aType.length)
                {
                return aType[n];
                }
            throw new IllegalArgumentException("unknown event publisher event type: " + n);
            }

        // ----- constants ------------------------------------------------------

        public enum Type
            {
            /**
             * The event is a connected event.
             */
            Connected,
            /**
             * The event is a disconnected event.
             */
            Disconnected,
            /**
             * The event is a released event.
             */
            Released,
            /**
             * The event is a destroyed event.
             */
            Destroyed,
            /**
             * The event is a channels freed event.
             */
            ChannelsFreed;
            }

        // ----- data members ---------------------------------------------------

        /**
         * The channels the event relates to.
         */
        protected int[] m_anChannel;
        }

    // ----- inner class: ConnectorListener ------------------------------

    /**
     * A {@link PublisherListener} to receive events from the connector.
     */
    protected class ConnectorListener
            implements PublisherListener
        {
        @Override
        public void onEvent(PublisherEvent evt)
            {
            switch (evt.getType())
                {
                case Released ->
                    {
                    if (isActive())
                        {
                        // destroy/disconnect event
                        Logger.fine("Detected release of topic "
                                            + f_sTopicName + ", closing publisher "
                                            + NamedTopicPublisher.this);
                        closeInternal(false);
                        }
                    }
                case Destroyed ->
                    {
                    if (isActive())
                        {
                        // destroy/disconnect event
                        Logger.fine("Detected destroy of topic "
                                            + f_sTopicName + ", closing publisher "
                                            + NamedTopicPublisher.this);
                        closeInternal(true);
                        }
                    }
                case ChannelsFreed ->
                        {
                        onNotification(evt.getChannels());
                        }
                }
            }
        }

    // ----- inner class: FlushMode ----------------------------------------

    /**
     * An enum representing different flush modes for a publisher.
     */
    public enum FlushMode
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
        FLUSH_CLOSE_EXCEPTIONALLY,
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

    // ----- constants ------------------------------------------------------

    /**
     * Publisher close timeout on first flush attempt. After this time is exceeded, all outstanding asynchronous operations will be completed exceptionally.
     */
    public static final long CLOSE_TIMEOUT_SECS = TimeUnit.MILLISECONDS.toSeconds(Base.parseTime(Config.getProperty("coherence.topic.publisher.close.timeout", "30s"), Base.UNIT_S));

    // ----- data members ---------------------------------------------------

    /**
     * The parent topic.
     */
    private final NamedTopic<V> f_topic;

    /**
     * The {@link PublisherConnector} to use to connect to back end service.
     */
    private final PublisherConnector<V> f_connector;

    /**
     * The current state of the {@link Publisher}.
     */
    private volatile State m_state;

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
    protected final NamedTopicPublisherChannel<V>[] f_aChannel;

    /**
     * The set of channels which elements have been offered to since the last @{link #toString} call.
     */
    private final BitSet f_setOfferedChannel;

    /**
     * The listener to receive events from the connector.
     */
    private final PublisherListener f_listener;

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

    /**
     * The calculator to use to calculate the size of topic elements.
     */
    private final NamedTopic.ElementCalculator f_calculator;
    }
