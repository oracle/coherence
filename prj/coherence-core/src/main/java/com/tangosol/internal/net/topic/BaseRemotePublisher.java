
/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.RemoteNamedTopic

package com.tangosol.internal.net.topic;

import com.tangosol.internal.net.topic.NamedTopicPublisher.PublisherEvent;
import com.tangosol.internal.net.topic.NamedTopicPublisher.PublisherListener;

import com.tangosol.net.TopicService;

import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.TopicDependencies;

import com.tangosol.util.Binary;
import com.tangosol.util.Listeners;
import com.tangosol.util.TaskDaemon;

import java.util.List;

import java.util.concurrent.CompletableFuture;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.util.function.BiConsumer;

/**
 * A base class for remote publishers.
 *
 * @author Jonathan Knight  2025.01.01
 */
public abstract class BaseRemotePublisher<V>
        implements PublisherConnector<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link BaseRemotePublisher}.
     *
     * @param nId       the unique publisher identifier
     * @param cChannel  the default channel count for the topic
     * @param options   the publisher options
     */
    public BaseRemotePublisher(long nId, int cChannel, Publisher.Option<? super V>[] options)
        {
        Publisher.OptionSet<V> optionSet = Publisher.optionsFrom(options);
        f_nId      = nId;
        f_cChannel = optionSet.getChannelCount(cChannel);
        }

    // ----- ConnectedPublisher.Connector methods ---------------------------

    @Override
    public void close()
        {
        if (m_daemon != null)
            {
            m_daemon.stop();
            }
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
    public long getId()
        {
        return f_nId;
        }

    @Override
    public TopicDependencies getTopicDependencies()
        {
        return m_topicService.getTopicBackingMapManager().getTopicDependencies(m_sTopicName);
        }

    @Override
    public int getChannelCount()
        {
        return f_cChannel;
        }

    @Override
    public void addListener(PublisherListener listener)
        {
        f_listeners.add(listener);
        }

    @Override
    public void removeListener(PublisherListener listener)
        {
        f_listeners.remove(listener);
        }

    @Override
    public TopicService getTopicService()
        {
        return m_topicService;
        }

    /**
     * Getter for property TopicName.<p>
     */
    @Override
    public String getTopicName()
        {
        return m_sTopicName;
        }

    @Override
    public long getMaxBatchSizeBytes()
        {
        if (m_maxBatchSizeBytes <= 0)
            {
            return getTopicDependencies().getMaxBatchSizeBytes();
            }
        return m_maxBatchSizeBytes;
        }

    /**
     * Set the maximum batch size.
     *
     * @param maxBatchSizeBytes  the maximum batch size
     */
    public void setMaxBatchSizeBytes(long maxBatchSizeBytes)
        {
        m_maxBatchSizeBytes = maxBatchSizeBytes;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Setter for property TopicName.<p>
     */
    public void setTopicName(String sName)
        {
        m_sTopicName = sName;
        }

    /**
     * Setter for property TopicService.<p>
     */
    public void setTopicService(TopicService serviceTopic)
        {
        m_topicService = serviceTopic;
        }

    /**
     * Obtain the task daemon.
     *
     * @return the task daemon
     */
    public TaskDaemon ensureTaskDaemon()
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
                    daemon = m_daemon = new TaskDaemon(getClass().getSimpleName() + ":" + m_sTopicName + ":" + f_nId);
                    }
                }
            finally
                {
                f_lock.unlock();
                }
            }
        return daemon;
        }

    /**
     * Dispatch a publisher event.
     *
     * @param type the event type
     */
    public void dispatchEvent(PublisherEvent.Type type)
        {
        PublisherEvent event = new PublisherEvent(this, type);
        event.dispatch(f_listeners);
        }

    /**
     * Dispatch a publisher event.
     *
     * @param type       the event type
     * @param anChannel  the event channels
     */
    public void dispatchEvent(PublisherEvent.Type type, int[] anChannel)
        {
        PublisherEvent event = new PublisherEvent(this, type, anChannel);
        event.dispatch(f_listeners);
        }

    protected void dispatchEvent(PublisherEvent evt)
        {
        switch (evt.getType())
            {
            case Destroyed:
                m_fDestroyed = true;
                break;
            case Released:
                m_fReleased = true;
                break;
            }
        evt.dispatch(f_listeners);
        }

    // ----- inner class: ChannelConnector ----------------------------------

    /**
     * A base class for a publisher channel connector.
     */
    protected abstract class BaseChannelConnector
            implements PublisherChannelConnector<V>
        {
        /**
         * Create a {@link BaseChannelConnector}.
         *
         * @param nId       the unique identifier for the publisher
         * @param nChannel  the channel identifier for this connector
         */
        protected BaseChannelConnector(long nId, int nChannel)
            {
            f_nId      = nId;
            f_nChannel = nChannel;
            }

        @Override
        public boolean isActive()
            {
            return BaseRemotePublisher.this.isActive();
            }

        @Override
        public int getChannel()
            {
            return f_nChannel;
            }

        @Override
        public void close()
            {
            }

        @Override
        public void ensureConnected()
            {
            BaseRemotePublisher.this.ensureConnected();
            }

        @Override
        public String getTopicName()
            {
            return m_sTopicName;
            }

        @Override
        public CompletionStage<?> initialize()
            {
            return f_completedFuture;
            }

        @Override
        public void offer(Object oCookie, List<Binary> listBinary, int nNotifyPostFull, BiConsumer<PublishResult, Throwable> handler)
            {
            try
                {
                offerInternal(listBinary, nNotifyPostFull)
                        .handleAsync((result, error) ->
                            {
                            handler.accept(result, error);
                            return null;
                            }, ensureTaskDaemon());
                }
            catch (Throwable t)
                {
                handler.accept(null, t);
                }
            }

        /**
         * Publish the specified list of binary values.
         *
         * @param listBinary      the list of values to publish
         * @param nNotifyPostFull {@code true} to configure notifications for this publisher when the topic is full
         */
        protected abstract CompletionStage<PublishResult> offerInternal(List<Binary> listBinary, int nNotifyPostFull);

        @Override
        public CompletionStage<?> prepareOfferRetry(Object oCookie)
            {
            return f_completedFuture;
            }

        @Override
        public TopicDependencies getTopicDependencies()
            {
            return BaseRemotePublisher.this.getTopicDependencies();
            }

        @Override
        public TopicService getTopicService()
            {
            return m_topicService;
            }

        // ----- data members ---------------------------------------------------

        /**
         * A unique identifier for this publisher.
         */
        protected final long f_nId;

        /**
         * The channel identifier.
         */
        protected final int f_nChannel;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The configured channel count.
     */
    private final int f_cChannel;

    /**
     * The publisher identifier.
     */
    private final long f_nId;

    /**
     * Property Listeners
     */
    private final Listeners f_listeners = new Listeners();

    /**
     * Property TopicName
     */
    private String m_sTopicName;

    /**
     * Property TopicService
     */
    private TopicService m_topicService;

    /**
     * The daemon to use to complete async tasks.
     */
    private TaskDaemon m_daemon;

    /**
     * The lock to synchronize state.
     */
    private final Lock f_lock = new ReentrantLock();

    /**
     * The maximum batch size for the publisher.
     */
    private long m_maxBatchSizeBytes = -1;

    /**
     * A completed void {@link CompletableFuture}.
     */
    private static final CompletableFuture<Void> f_completedFuture = CompletableFuture.completedFuture(null);

    /**
     * A flag to indicate that this subscriber's topic was released.
     */
    private boolean m_fReleased;

    /**
     * A flag to indicate that this subscriber's topic was destroyed.
     */
    private boolean m_fDestroyed;
    }
