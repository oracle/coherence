/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.tangosol.net.TopicService;

import com.tangosol.net.topic.TopicDependencies;

import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.ConverterCollections;

import java.util.List;
import java.util.Map;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import java.util.function.BiConsumer;

/**
 * A {@link ConverterPublisherConnector} view an underlying {@link PublisherConnector}
 * through a set of {@link Converter} instances.
 *
 * @param <F>  the type of element published to the underlying topic
 * @param <T>  the type of element accepted by this publisher
 *
 * @author Jonathan Knight  2024.11.26
 */
public class ConverterPublisherConnector<F, T>
        implements PublisherConnector<T>
    {
    /**
     * Constructor.
     *
     * @param connector  the underlying {@link PublisherConnector}
     * @param convUp     the Converter from the underlying {@link PublisherConnector}
     * @param convDown   the Converter to the underlying {@link PublisherConnector}
     */
    public ConverterPublisherConnector(PublisherConnector<F> connector, Converter<F, T> convUp, Converter<T, F> convDown)
        {
        f_connector = connector;
        f_convUp    = convUp;
        f_convDown  = convDown;
        }

    @Override
    public boolean isActive()
        {
        return f_connector.isActive();
        }

    @Override
    public boolean isDestroyed()
        {
        return f_connector.isDestroyed();
        }

    @Override
    public boolean isReleased()
        {
        return f_connector.isReleased();
        }

    @Override
    public long getId()
        {
        return f_connector.getId();
        }

    @Override
    public void close()
        {
        f_connector.close();
        f_mapChannel.values().forEach(c ->
            {
            try
                {
                c.close();
                }
            catch (Exception e)
                {
                // ignored
                }
            });
        f_mapChannel.clear();
        }

    @Override
    public String getTopicName()
        {
        return f_connector.getTopicName();
        }

    @Override
    public TopicService getTopicService()
        {
        return f_connector.getTopicService();
        }

    @Override
    public TopicDependencies getTopicDependencies()
        {
        return f_connector.getTopicDependencies();
        }

    @Override
    public int getChannelCount()
        {
        return f_connector.getChannelCount();
        }

    @Override
    public void ensureConnected()
        {
        f_connector.ensureConnected();
        }

    @Override
    public void addListener(NamedTopicPublisher.PublisherListener listener)
        {
        f_connector.addListener(listener);
        }

    @Override
    public void removeListener(NamedTopicPublisher.PublisherListener listener)
        {
        f_connector.removeListener(listener);
        }

    @Override
    public PublisherChannelConnector<T> createChannelConnector(int nChannel)
        {
        return f_mapChannel.computeIfAbsent(nChannel, k -> new ConverterChannelConnector(f_connector.createChannelConnector(nChannel)));
        }

    @Override
    public long getMaxBatchSizeBytes()
        {
        return f_connector.getMaxBatchSizeBytes();
        }

    // ----- inner class: ConverterChannelConnector -------------------------

    /**
     * A {@link ConverterChannelConnector} view an underlying {@link PublisherChannelConnector}
     * through a set of {@link Converter} instances.
     */
    protected class ConverterChannelConnector
            implements PublisherChannelConnector<T>
        {
        /**
         * Create a {@link ConverterChannelConnector}.
         *
         * @param channelConnector  the underlying {@link PublisherChannelConnector}
         */
        public ConverterChannelConnector(PublisherChannelConnector<F> channelConnector)
            {
            f_channelConnector = channelConnector;
            }

        @Override
        public boolean isActive()
            {
            return f_channelConnector.isActive();
            }

        @Override
        public int getChannel()
            {
            return f_channelConnector.getChannel();
            }

        @Override
        public void close()
            {
            f_channelConnector.close();
            }

        @Override
        public String getTopicName()
            {
            return f_channelConnector.getTopicName();
            }

        @Override
        public void ensureConnected()
            {
            f_channelConnector.ensureConnected();
            }

        @Override
        public CompletionStage<?> initialize()
            {
            return f_channelConnector.initialize();
            }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public void offer(Object oCookie, List<Binary> listBinary, int nNotifyPostFull, BiConsumer<PublishResult, Throwable> handler)
            {
            f_channelConnector.offer(oCookie, ConverterCollections.getList(listBinary, (Converter) f_convUp,
                    (Converter) f_convDown), nNotifyPostFull, handler);
            }

        @Override
        public CompletionStage<?> prepareOfferRetry(Object oCookie)
            {
            return f_channelConnector.prepareOfferRetry(oCookie);
            }

        @Override
        public TopicDependencies getTopicDependencies()
            {
            return f_channelConnector.getTopicDependencies();
            }

        @Override
        public TopicService getTopicService()
            {
            return f_channelConnector.getTopicService();
            }

        // ----- data members -----------------------------------------------

        /**
         * The underlying {@link PublisherChannelConnector}.
         */
        private final PublisherChannelConnector<F> f_channelConnector;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The underlying {@link PublisherConnector}.
     */
    private final PublisherConnector<F> f_connector;

    /**
     * The converter to convert from the underlying topic.
     */
    private final Converter<F, T> f_convUp;

    /**
     * The converter to convert to the underlying topic.
     */
    private final Converter<T, F> f_convDown;

    /**
     * A map of channel to the corresponding {@link ConverterChannelConnector} for that channel.
     */
    private final Map<Integer, ConverterChannelConnector> f_mapChannel = new ConcurrentHashMap<>();
    }
