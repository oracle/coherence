/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberInfo;

import com.tangosol.net.topic.Position;

import com.tangosol.util.Binary;
import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import java.util.function.Consumer;

/**
 * A {@link ConverterConnectedSubscriber} views an underlying {@link ConverterSubscriber}
 * through a set of {@link com.tangosol.util.Converter} instances that convert topic
 * values.
 *
 * @param <F>  the type of the elements in the underlying topic
 * @param <T>  the type the elements are viewed as
 *
 * @author Jonathan Knight  2024.11.26
 */
public class ConverterConnectedSubscriber<F, T>
        extends ConverterSubscriber<F, T>
        implements SubscriberConnector.ConnectedSubscriber<T>
    {
    /**
     * Constructor.
     *
     * @param subscriber  the underlying {@link SubscriberConnector.ConnectedSubscriber}
     * @param topic       the parent {@link ConverterNamedTopic}
     */
    public ConverterConnectedSubscriber(NamedTopicSubscriber<F> subscriber, ConverterNamedTopic<F, T> topic)
        {
        super(subscriber, topic);
        f_connectedSubscriber = subscriber;
        f_connector           = new ConverterSubscriberConnector<>(subscriber.getConnector(), topic);
        }

    @Override
    public SubscriberGroupId getSubscriberGroupId()
        {
        return f_connectedSubscriber.getSubscriberGroupId();
        }

    @Override
    public SubscriberInfo.Key getKey()
        {
        return f_connectedSubscriber.getKey();
        }

    @Override
    public int getNotificationId()
        {
        return f_connectedSubscriber.getNotificationId();
        }

    @Override
    public long getSubscriptionId()
        {
        return f_connectedSubscriber.getSubscriptionId();
        }

    @Override
    public Filter<?> getFilter()
        {
        return f_connectedSubscriber.getFilter();
        }

    @Override
    public ValueExtractor<?, ?> getConverter()
        {
        return f_connectedSubscriber.getConverter();
        }

    @Override
    public Executor getExecutor()
        {
        return f_connectedSubscriber.getExecutor();
        }

    @Override
    public void updateChannel(int nChannel, Consumer<NamedTopicSubscriber.TopicChannel> fn)
        {
        f_connectedSubscriber.updateChannel(nChannel, fn);
        }

    @Override
    public Position updateSeekedChannel(int nChannel, SeekResult result)
        {
        return f_connectedSubscriber.updateSeekedChannel(nChannel, result);
        }

    @Override
    public long getConnectionTimestamp()
        {
        return f_connectedSubscriber.getConnectionTimestamp();
        }

    @Override
    public SubscriberConnector<T> getConnector()
        {
        return f_connector;
        }

    @Override
    public void setChannelHeadIfHigher(int nChannel, Position head)
        {
        f_connectedSubscriber.setChannelHeadIfHigher(nChannel, head);
        }

    @Override
    public Position getChannelHead(int nChannel)
        {
        return f_connectedSubscriber.getChannelHead(nChannel);
        }

    @Override
    public CompletableFuture<ReceiveResult> receive(int nChannel, int cMaxElements, SubscriberConnector.ReceiveHandler handler)
        {
        return f_connectedSubscriber.receive(nChannel, cMaxElements, new ConverterReceiveHandler(handler));
        }

    @Override
    public Element<T> createElement(Binary binary, int nChannel)
        {
        // Don't think this will be called.
        throw new UnsupportedOperationException();
        }

    // ----- inner class: ConverterReceiveHandler ---------------------------

    /**
     * A {@link ConverterReceiveHandler} views an underlying {@link SubscriberConnector.ReceiveHandler}
     * through a set of {@link com.tangosol.util.Converter} instances that convert topic values.
     */
    protected class ConverterReceiveHandler
            implements SubscriberConnector.ReceiveHandler
        {
        /**
         * Create a {@link ConverterReceiveHandler}.
         *
         * @param handler  the underlying {@link SubscriberConnector.ReceiveHandler}
         */
        public ConverterReceiveHandler(SubscriberConnector.ReceiveHandler handler)
            {
            f_handler = handler;
            }

        @Override
        public void onReceive(long lVersion, ReceiveResult result, Throwable error, SubscriberConnector.Continuation continuation)
            {
            if (result != null)
                {
                result = new ConverterReceiveResult(result, f_convBinaryUp, f_convBinaryDown);
                }
            f_handler.onReceive(lVersion, result, error, continuation);
            }

        // ----- data members -----------------------------------------------

        /**
         * The underlying {@link SubscriberConnector.ReceiveHandler}.
         */
        private final SubscriberConnector.ReceiveHandler f_handler;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The underlying {@link SubscriberConnector.ConnectedSubscriber}.
     */
    private final SubscriberConnector.ConnectedSubscriber<F> f_connectedSubscriber;

    /**
     * The converter {@link SubscriberConnector}.
     */
    private final SubscriberConnector<T> f_connector;
    }
