/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.oracle.coherence.common.base.Associated;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.net.DebouncedFlowControl;

import com.tangosol.internal.net.topic.impl.paged.BatchingOperationsQueue;

import com.tangosol.internal.util.DaemonPool;

import com.tangosol.io.Serializer;

import com.tangosol.net.messaging.ConnectionException;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.TopicDependencies;
import com.tangosol.net.topic.TopicException;
import com.tangosol.net.topic.TopicPublisherException;

import com.tangosol.util.Binary;
import com.tangosol.util.LongArray;
import com.tangosol.util.SimpleLongArray;
import com.tangosol.util.SparseArray;

import java.util.List;
import java.util.Objects;

import java.util.concurrent.CompletableFuture;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * A publisher that publishes to a specific channel in a {@link NamedTopic}.
 *
 * @param <V>  the type of element published to the topic
 *
 * @author Jonathan Knight  2024.11.26
 */
public class NamedTopicPublisherChannel<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link NamedTopicPublisherChannel}.
     *
     * @param lPublisherId     the identifier for the parent publisher
     * @param nChannel         the channel to publish to
     * @param nNotifyPostFull  the post full notification identifier
     * @param flowControl      the {@link DebouncedFlowControl} control to use
     * @param pool             the {@link DaemonPool} to execute publish completions
     */
    public NamedTopicPublisherChannel(PublisherChannelConnector<V>   connector,
                                      long                           lPublisherId,
                                      int                            nChannel,
                                      int                            nNotifyPostFull,
                                      DebouncedFlowControl           flowControl,
                                      DaemonPool                     pool,
                                      Serializer                     serializer,
                                      NamedTopic.ElementCalculator   calculator,
                                      BiConsumer<Throwable, Integer> onErrorHandler)
        {
        m_connector       = connector;
        f_lPublisherId    = lPublisherId;
        f_nChannel        = nChannel;
        f_sTopicName      = connector.getTopicName();
        f_onErrorHandler  = onErrorHandler;
        f_nNotifyPostFull = nNotifyPostFull;
        f_serializer      = serializer;

        //noinspection rawtypes
        BatchingOperationsQueue.Executor executor = new NamedTopicPublisherChannel.AssociatedExecutor(pool);

        f_batchingQueue = new BatchingOperationsQueue<>(this::addQueuedElements, 1, flowControl,
                calculator::calculateUnits, executor);

        m_state = State.Active;
        }

    /**
     * Publish the specified messages.
     *
     * @param binValue  the messages to publish as a serialized {@link Binary} value
     *
     * @return  a {@link CompletableFuture} that will complete when the messages have been published
     *          with the status of the publish request
     */
    public CompletableFuture<Publisher.Status> publish(Binary binValue)
        {
        try
            {
            return f_batchingQueue.add(binValue);
            }
        catch (IllegalStateException e)
            {
            // The batching queue throws an IllegalStateException if closed,
            // so we throw another with a more meaningful message
            throw new IllegalStateException("This publisher is no longer active", e);
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Ensure this channel publisher is connected to the underlying topic.
     */
    private void ensureConnected()
        {
        TopicDependencies dependencies = m_connector.getTopicDependencies();
        long              retry        = dependencies.getReconnectRetryMillis();
        long              now          = System.currentTimeMillis();
        long              timeout      = now + dependencies.getReconnectTimeoutMillis();
        Throwable         error        = null;

        while (now < timeout)
            {
            if (m_state != State.Active || m_connector == null)
                {
                // we're closed
                return;
                }

            try
                {
                m_connector.ensureConnected();
                break;
                }
            catch (Throwable thrown)
                {
                error = thrown;
                if (error instanceof TopicException)
                    {
                    break;
                    }
                }
            now = System.currentTimeMillis();
            if (now < timeout)
                {
                Logger.finer("Failed to reconnect publisher, will retry in "
                        + retry + " millis " + this + " due to " + error.getMessage());
                try
                    {
                    Thread.sleep(retry);
                    }
                catch (InterruptedException e)
                    {
                    // ignored
                    }
                }
            }

        if (error != null)
            {
            throw Exceptions.ensureRuntimeException(error);
            }
        }

    /**
     * Returns {@code true} if this publisher is active, otherwise returns {@code false}.
     *
     * @return {@code true} if this publisher is active, otherwise returns {@code false}
     */
    public boolean isActive()
        {
        return m_state == State.Active;
        }

    /**
     * Stop this publisher from accepting any further messages to publish
     */
    public synchronized void stop()
        {
        if (m_state == State.Active)
            {
            m_state = State.Closing;
            f_batchingQueue.close();
            }
        }

    /**
     * Close this publisher.
     */
    public synchronized void close()
        {
        if (m_state == State.Closing)
            {
            m_state  = State.Closed;
            // belt and braces cancellation of remaining publish requests
            f_batchingQueue.cancelAllAndClose("Publisher has been closed", null);
            m_connector = null;
            }
        }

    /**
     * Obtain a {@link CompletableFuture} that will be complete when
     * all the currently outstanding publish operations complete.
     *
     * @param mode  {@link NamedTopicPublisher.FlushMode} flush mode to use
     *
     * @return a {@link CompletableFuture} that will be completed when
     *         all the currently outstanding publish operations are complete
     */
    public CompletableFuture<Void> flush(NamedTopicPublisher.FlushMode mode)
        {
        String sDescription = null;

        switch (mode)
            {
            case FLUSH_DESTROY:
                return flushExceptionally("Topic " + f_sTopicName + " was destroyed");

            case FLUSH_CLOSE_EXCEPTIONALLY:
                return flushExceptionally("Force Close of Publisher " + f_lPublisherId + " channel "
                                                + f_nChannel + " for topic " + f_sTopicName);

            case FLUSH:
            default:
                return f_batchingQueue.flush();
            }
        }

    private CompletableFuture<Void> flushExceptionally(String sReason)
        {
        BiFunction<Throwable, Binary, Throwable> fn  = TopicPublisherException.createFactory(f_serializer, sReason);
        f_batchingQueue.handleError(fn, BatchingOperationsQueue.OnErrorAction.CompleteWithException);
        return f_batchingQueue.flush();
        }

    /**
     * Returns the channel this publisher publishes to.
     *
     * @return  the channel this publisher publishes to
     */
    public int getChannel()
        {
        return f_nChannel;
        }

    /**
     * Method called to notify this publisher that space is now available in a previously full topic.
     */
    protected void onNotification()
        {
        ++m_cNotify;
        if (f_batchingQueue.resume())
            {
            ++m_cWait;
            addQueuedElements(1);
            }
        }

    /**
     * Offer the batched elements from this channel.
     *
     * @param cbMaxElements  the maximum number of bytes to offer
     */
    protected void addQueuedElements(int cbMaxElements)
        {
        // Fill the current batch with the specified number of elements
        if (f_batchingQueue.fillCurrentBatch(cbMaxElements))
            {
            // There are elements in the queue so process them by
            // first ensuring the page id is set
            m_connector.initialize()
                    .thenAccept(this::addInternal)
                    .handle(this::handleError);
            }
        }

    /**
     * Asynchronously add elements to the specified page.
     *
     * @param oCookie  the opaque cookie to pass to the connector
     */
    protected void addInternal(Object oCookie)
        {
        List<Binary> listBinary = f_batchingQueue.getCurrentBatchValues();

        // If the list is empty (which would probably be due to the
        // application code calling cancel on the futures) then we
        // do not need to do anything else
        if (listBinary.isEmpty())
            {
            return;
            }

        m_connector.offer(oCookie, listBinary, f_nNotifyPostFull, (result, err) ->
            {
            try
                {
                if (err == null)
                    {
                    handleOfferCompletion(result);
                    }
                else
                    {
                    if (err instanceof ConnectionException)
                        {
                        // we probably disconnected so retry (which will attempt to reconnect)
                        handleOfferCompletion(new SimplePublishResult(f_nChannel, 0, new SimpleLongArray(), new SimpleLongArray(), 0, null, PublishResult.Status.Retry));
                        }
                    else
                        {
                        handleError(null, err);
                        }
                    }
                }
            catch (Exception e)
                {
                Logger.err("Error handling topic offer completion for topic " + f_sTopicName, e);
                }
            });
        }

    /**
     * Handle completion of a {@link CompletableFuture} linked to an async execution of
     * a {@link PublishResult}.
     *
     * @param result   the result returned from the {@link PublishResult}
     */
    protected void handleOfferCompletion(PublishResult result)
        {
        // Complete the offered elements
        LongArray<Throwable>        aErrors   = result.getErrors();
        LongArray<Publisher.Status> aMetadata = result.getPublishStatus();
        int                         cAccepted = result.getAcceptedCount();

        ++m_cOffers;
        m_cAccepted += cAccepted;

        if (cAccepted == 0)
            {
            ++m_cMisses;
            }

        if (f_nNotifyPostFull == 0 && result.getStatus() == PublishResult.Status.TopicFull)
            {
            int       ceBatch = f_batchingQueue.getCurrentBatch().size();
            Throwable error   = new IllegalStateException("the topic is at capacity"); // java.util.Queue.add throws ISE so we do to

            if (aErrors == null)
                {
                aErrors = new SparseArray<>();
                }

            while (cAccepted < ceBatch)
                {
                ++cAccepted;
                aErrors.add(error);
                }
            }

        f_batchingQueue.completeElements(cAccepted, aErrors, aMetadata, TopicPublisherException.createFactory(f_serializer), null);

        // If there are any errors
        handleIndividualErrors(aErrors);

        // we need to handle offer completions until actually closed to
        // allow for flushing during close
        if (m_state != NamedTopicPublisherChannel.State.Closed)
            {
            switch (result.getStatus())
                {
                case Retry:
                    try
                        {
                        ensureConnected();
                        }
                    catch (Exception e)
                        {
                        handleError(null, e);
                        }
                    m_connector.prepareOfferRetry(result.getRetryCookie())
                            .thenRun(() ->
                                {
                                addQueuedElements(result.getRemainingCapacity());
                                })
                            .handle(this::handleError);
                    break;

                case TopicFull:
                    if (f_nNotifyPostFull != 0)
                        {
                        f_batchingQueue.pause();
                        break;
                        }
                    // else; fall through

                default:
                    addQueuedElements(result.getRemainingCapacity());
                    break;
                }
            }
        // else; if the error handler closed the publisher there is nothing else to do
        }

    /**
     * Handle the specified error.
     *
     * @param throwable  the error to handle
     */
    protected Void handleError(Void ignored, Throwable throwable)
        {
        if (throwable != null)
            {
            // call the BatchingOperationsQueue's error handler to cancel all outstanding elements.
            synchronized (this)
                {
                stop();
                // Inform the error handler of the error, this should be the parent publisher
                // so that it can close itself if required.
                // We do this here so that the publisher closes before this channel publisher to catch
                // any race where a new message is in the middle of being added
                if (f_onErrorHandler != null)
                    {
                    try
                        {
                        f_onErrorHandler.accept(throwable, f_nChannel);
                        }
                    catch (Throwable t)
                        {
                        // shouldn't happen but if we do get an exception we can ignore it
                        // and carry on with our clean-up
                        Logger.err(t);
                        }
                    }
                f_batchingQueue.handleError(TopicPublisherException.createFactory(f_serializer, throwable.getMessage()),
                                            BatchingOperationsQueue.OnErrorAction.CancelAndClose);
                close();
                }
            }
        return null;
        }

    /**
     * Process the array of exceptions associated with an offer.
     *
     * @param aErrors the error array
     */
    protected void handleIndividualErrors(LongArray<Throwable> aErrors)
        {
        if (aErrors == null || aErrors.isEmpty())
            {
            return;
            }

        // Stop this publisher.
        Throwable throwable = aErrors.get(aErrors.getFirstIndex());
        handleError(null, throwable);
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
        NamedTopicPublisherChannel<?> that = (NamedTopicPublisherChannel<?>) o;

        return f_lPublisherId == that.f_lPublisherId
                && f_nChannel == that.f_nChannel
                && Objects.equals(m_connector, that.m_connector);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(f_lPublisherId, f_nChannel, m_connector);
        }

    @Override
    public String toString()
        {
        long cOffersNow   = m_cOffers;
        long cAcceptedNow = m_cAccepted;
        long cMissesNow   = m_cMisses;
        long cWaitNow     = m_cWait;
        long cNotifyNow   = m_cNotify;

        long cOffers   = cOffersNow   - m_cOffersLast;
        long cAccepted = cAcceptedNow - m_cAcceptedLast;
        long cMisses   = cMissesNow   - m_cMissesLast;
        long cWait     = cWaitNow     - m_cWaitsLast;
        long cNotify   = cNotifyNow   - m_cNotifyLast;

        m_cOffersLast   = cOffersNow;
        m_cAcceptedLast = cAcceptedNow;
        m_cMissesLast   = cMissesNow;
        m_cWaitsLast    = cWaitNow;
        m_cNotifyLast   = cNotifyNow;

        return getClass().getSimpleName() +
                "(topic=" + f_sTopicName +
                ", channel=" + f_nChannel +
                ", state=" + m_state +
                ", publisher=" + f_lPublisherId +
                ", batchSize=" + (cAccepted / Math.max(1, cOffers - cMisses)) +
                ", hitRate=" + ((cOffers - cMisses) * 100 / Math.max(1, cOffers)) + "%" +
                ", waitNotifyRate=" + (cWait * 100 / Math.max(1, cOffers)) + "/" + (cNotify * 100 / Math.max(1, cOffers)) + "%" +
                ")";
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
        }

    // ----- inner class: AssociatedExecutor --------------------------------

    /**
     * A {@link BatchingOperationsQueue.Executor} that expects the {@link DaemonPool}
     * to honour the {@link Associated} tasks submitted to it.
     */
    protected class AssociatedExecutor
            implements BatchingOperationsQueue.Executor
        {
        public AssociatedExecutor(DaemonPool pool)
            {
            f_pool = pool;
            }

        @Override
        public void execute(Runnable runnable)
            {
            f_pool.add(new AssociatedTask(runnable));
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link DaemonPool} used to complete published message futures
         * so that they are not on the service thread.
         */
        private final DaemonPool f_pool;
        }

    // ----- inner class: AssociatedTask ------------------------------------

    /**
     * A task this publisher can execute.
     */
    protected class AssociatedTask
            implements Runnable, Associated<Integer>
        {
        public AssociatedTask(Runnable task)
            {
            f_task = task;
            }

        @Override
        public Integer getAssociatedKey()
            {
            return f_nChannel;
            }

        @Override
        public void run()
            {
            f_task.run();
            }

        // ----- data members -----------------------------------------------

        private final Runnable f_task;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The publisher connector to use to connect to back end resources.
     */
    private PublisherChannelConnector<V> m_connector;

    /**
     * The identifier of the parent publisher.
     */
    private final long f_lPublisherId;

    /**
     * The channel to publish to.
     */
    private final int f_nChannel;

    /**
     * The name of the topic being published to
     */
    private final String f_sTopicName;

    /**
     * A consumer to be notified on publishing errors.
     */
    private final BiConsumer<Throwable, Integer> f_onErrorHandler;

    /**
     * The serializer to use to serialize message payloads.
     */
    private final Serializer f_serializer;

    /**
     * The post full notifier.
     */
    private final int f_nNotifyPostFull;

    /**
     * The {@link BatchingOperationsQueue} controlling the batches of add operations.
     */
    private final BatchingOperationsQueue<Binary, Publisher.Status> f_batchingQueue;

    /**
     * The current state of the publisher.
     */
    private volatile State m_state;

    /**
     * The number of times an offer was made
     */
    private long m_cOffers;

    /**
     * The last value of m_cOffers used within {@link #toString} stats.
     */
    private long m_cOffersLast;

    /**
     * The number of accepted items.
     */
    private long m_cAccepted;

    /**
     * The last value of m_cAccepted used within {@link #toString} stats.
     */
    private long m_cAcceptedLast;

    /**
     * The number of times no elements were accepted from an offer
     */
    private long m_cMisses;

    /**
     * The last value of m_cMisses used within {@link #toString} stats.
     */
    private long m_cMissesLast;

    /**
     * The number of this publisher has waited.
     */
    private long m_cWait;

    /**
     * The last value of m_cWait used within {@link #toString} stats.
     */
    private long m_cWaitsLast;

    /**
     * The number of times this publisher was notified.
     */
    private long m_cNotify;

    /**
     * The last value of m_cNotify used within {@link #toString} stats.
     */
    private long m_cNotifyLast;
    }
