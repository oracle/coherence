/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.safeNamedTopic;

import com.tangosol.coherence.component.util.SafeNamedTopic;

import com.tangosol.internal.net.topic.NamedTopicPublisher;
import com.tangosol.internal.net.topic.PublishResult;
import com.tangosol.internal.net.topic.PublisherChannelConnector;
import com.tangosol.internal.net.topic.PublisherConnector;

import com.tangosol.net.TopicService;

import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.TopicDependencies;

import com.tangosol.util.Binary;
import com.tangosol.util.Listeners;

import java.util.Arrays;
import java.util.List;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.util.function.BiConsumer;

/**
 * A safe wrapper around a {@link PublisherConnector}.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class SafePublisherConnector<V>
        implements PublisherConnector<V>
    {
    /**
     * Create a {@link SafePublisherConnector}.
     *
     * @param safeTopic  the {@link SafeNamedTopic}
     * @param options    the options used to configure the connector
     */
    @SuppressWarnings("rawtypes")
    public SafePublisherConnector(SafeNamedTopic<?> safeTopic, Publisher.Option[] options)
        {
        f_safeTopic = safeTopic;
        f_options   = Arrays.copyOf(options, options.length);
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
    public long getId()
        {
        return ensureRunningConnector().getId();
        }

    @Override
    public void close()
        {
        m_fReleased = true;
        try
            {
            PublisherConnector<V> connector = ensureRunningConnector();
            connector.close();
            connector.removeListener(f_Listener);
            }
        catch (Exception e)
            {
            // ignored
            }
        }

    @Override
    public String getTopicName()
        {
        return f_safeTopic.getName();
        }

    @Override
    public TopicService getTopicService()
        {
        return f_safeTopic.getTopicService();
        }

    @Override
    public TopicDependencies getTopicDependencies()
        {
        return getTopicService().getTopicBackingMapManager()
                .getTopicDependencies(f_safeTopic.getName());
        }

    @Override
    public int getChannelCount()
        {
        return ensureRunningConnector().getChannelCount();
        }

    @Override
    public void ensureConnected()
        {
        ensureRunningConnector().ensureConnected();
        }

    @Override
    public long getMaxBatchSizeBytes()
        {
        return ensureRunningConnector().getMaxBatchSizeBytes();
        }

    @Override
    public PublisherChannelConnector<V> createChannelConnector(int nChannel)
        {
        return new SafePublisherChannelConnector(nChannel);
        }

    @Override
    public void addListener(NamedTopicPublisher.PublisherListener listener)
        {
        f_Listeners.add(listener);
        }

    @Override
    public void removeListener(NamedTopicPublisher.PublisherListener listener)
        {
        f_Listeners.remove(listener);
        }

    // ----- helper methods -------------------------------------------------

    @SuppressWarnings({"unchecked"})
    protected PublisherConnector<V> ensureRunningConnector()
        {
        PublisherConnector<V> connector = m_connector;
        if (connector == null || !connector.isActive())
            {
            f_lock.lock();
            try
                {
                connector = m_connector;
                if (connector == null || !connector.isActive())
                    {
                    if (isReleased() || isDestroyed())
                        {
                        String reason = isDestroyed() ? "destroyed" : "released";
                        throw new IllegalStateException("SafePublisherConnector was explicitly " + reason);
                        }

                    Factory<V> factory = (Factory<V>) f_safeTopic;
                    connector = m_connector = factory.createPublisherConnector(f_options);
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
            implements NamedTopicPublisher.PublisherListener
        {
        @Override
        public void onEvent(NamedTopicPublisher.PublisherEvent evt)
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
            NamedTopicPublisher.PublisherEvent event = evt.withNewSource(SafePublisherConnector.this);
            event.dispatch(f_Listeners);
            }
        }

    // ----- inner class SafePublisherChannelConnector ----------------------

    /**
     * A safe wrapper around a {@link PublisherChannelConnector}.
     */
    protected class SafePublisherChannelConnector
            implements PublisherChannelConnector<V>
        {
        public SafePublisherChannelConnector(int nChannel)
            {
            f_nChannel = nChannel;
            }

        @Override
        public boolean isActive()
            {
            try
                {
                return m_channelConnector.isActive();
                }
            catch (Exception e)
                {
                return false;
                }
            }

        @Override
        public int getChannel()
            {
            return ensureRunningConnector().getChannelCount();
            }

        @Override
        public void close()
            {
            m_channelConnector.close();
            }

        @Override
        public String getTopicName()
            {
            return SafePublisherConnector.this.getTopicName();
            }

        @Override
        public void ensureConnected()
            {
            ensureRunningConnector().ensureConnected();
            }

        @Override
        public CompletableFuture<?> initialize()
            {
            return ensureRunningChannelConnector().initialize();
            }

        @Override
        public void offer(Object oCookie, List<Binary> listBinary, int nNotifyPostFull, BiConsumer<PublishResult, Throwable> handler)
            {
            ensureRunningChannelConnector().offer(oCookie, listBinary, nNotifyPostFull, handler);
            }

        @Override
        public CompletableFuture<?> prepareOfferRetry(Object oCookie)
            {
            return ensureRunningChannelConnector().prepareOfferRetry(oCookie);
            }

        @Override
        public TopicDependencies getTopicDependencies()
            {
            return SafePublisherConnector.this.getTopicDependencies();
            }

        // ----- helper methods ---------------------------------------------

        protected PublisherChannelConnector<V> ensureRunningChannelConnector()
            {
            PublisherChannelConnector<V> connector = m_channelConnector;
            if (connector == null || !connector.isActive())
                {
                f_lock.lock();
                try
                    {
                    connector = m_channelConnector;
                    if (connector == null || !connector.isActive())
                        {
                        connector = m_channelConnector = ensureRunningConnector().createChannelConnector(f_nChannel);
                        }
                    }
                finally
                    {
                    f_lock.unlock();
                    }
                }
            return connector;
            }

        // ----- data members -----------------------------------------------

        /**
         * The channel this connector publishes to.
         */
        private final int f_nChannel;

        private PublisherChannelConnector<V> m_channelConnector;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The underlying {@link PublisherConnector}.
     */
    private PublisherConnector<V> m_connector;

    /**
     * The lock to use when updating state.
     */
    private final Lock f_lock = new ReentrantLock();

    /**
     * The listeners registered with this publisher.
     */
    private final Listeners f_Listeners = new Listeners();

    /**
     * The listener registered with the underlying publisher.
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
    private final Publisher.Option[] f_options;

    /**
     * A flag to indicate that this subscriber's topic was released.
     */
    private boolean m_fReleased;

    /**
     * A flag to indicate that this subscriber's topic was destroyed.
     */
    private boolean m_fDestroyed;
    }
