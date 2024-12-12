/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;

import com.tangosol.net.FlowControl;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Binary;
import com.tangosol.util.Converter;

import java.time.Instant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.concurrent.CompletableFuture;

/**
 * A Converter {@link Subscriber} views an underlying {@link Subscriber}
 * through a {@link Converter}.
 *
 * @param <F> the type of elements the underlying {@link Subscriber} publishes
 * @param <T> the type that the elements should be converted to
 *
 * @author Jonathan Knight  2024.11.26
 */
public class ConverterSubscriber<F, T>
        implements Subscriber<T>
    {
    // ----- constructors -----------------------------------------------

    /**
     * Constructor.
     *
     * @param subscriber  the underlying {@link Subscriber}
     * @param topic       the parent {@link ConverterNamedTopic}
     */
    public ConverterSubscriber(Subscriber<F> subscriber, ConverterNamedTopic<F, T> topic)
        {
        f_subscriber     = subscriber;
        f_Topic          = topic;
        f_convUp         = topic.getConverterUp();
        f_convBinaryUp   = topic.getConverterBinaryUp();
        f_convDown       = topic.getConverterDown();
        f_convBinaryDown = topic.getConverterBinaryDown();
        }

    // ----- Subscriber methods -----------------------------------------

    @Override
    public CompletableFuture<Element<T>> receive()
        {
        return f_subscriber.receive()
                .thenApply(e -> new ConverterSubscriberElement<>(e, f_convUp, f_convBinaryUp));
        }

    @Override
    public CompletableFuture<List<Element<T>>> receive(int cBatch)
        {
        return f_subscriber.receive(cBatch)
                .thenApply(list ->
                    {
                    List<Element<T>> converted = new ArrayList<>(list.size());
                    for (Element<F> element : list)
                        {
                        converted.add(new ConverterSubscriberElement<>(element, f_convUp, f_convBinaryUp));
                        }
                    return converted;
                    });
        }

    @Override
    public int[] getChannels()
        {
        return f_subscriber.getChannels();
        }

    @Override
    public int getChannelCount()
        {
        return f_subscriber.getChannelCount();
        }

    @Override
    public FlowControl getFlowControl()
        {
        return f_subscriber.getFlowControl();
        }

    @Override
    public void close()
        {
        f_subscriber.close();
        }

    @Override
    public void heartbeat()
        {
        f_subscriber.heartbeat();
        }

    @Override
    public boolean isActive()
        {
        return f_subscriber.isActive();
        }

    @Override
    public void onClose(Runnable action)
        {
        f_subscriber.onClose(action);
        }

    @Override
    public CompletableFuture<CommitResult> commitAsync(int nChannel, Position position)
        {
        return f_subscriber.commitAsync(nChannel, position);
        }

    @Override
    public CompletableFuture<Map<Integer, CommitResult>> commitAsync(Map<Integer, Position> mapPositions)
        {
        return f_subscriber.commitAsync(mapPositions);
        }

    @Override
    public Map<Integer, Position> getLastCommitted()
        {
        return f_subscriber.getLastCommitted();
        }

    @Override
    public Position seek(int nChannel, Position position)
        {
        return f_subscriber.seek(nChannel, position);
        }

    @Override
    public Map<Integer, Position> seek(Map<Integer, Position> mapPosition)
        {
        return f_subscriber.seek(mapPosition);
        }

    @Override
    public Position seek(int nChannel, Instant timestamp)
        {
        return f_subscriber.seek(nChannel, timestamp);
        }

    @Override
    public Map<Integer, Position> seekToHead(int... anChannel)
        {
        return f_subscriber.seekToHead(anChannel);
        }

    @Override
    public Map<Integer, Position> seekToTail(int... anChannel)
        {
        return f_subscriber.seekToTail(anChannel);
        }

    @Override
    public Map<Integer, Position> getHeads()
        {
        return f_subscriber.getHeads();
        }

    @Override
    public Map<Integer, Position> getTails()
        {
        return f_subscriber.getTails();
        }

    @Override
    @SuppressWarnings("unchecked")
    public <V> NamedTopic<V> getNamedTopic()
        {
        return (NamedTopic<V>) f_Topic;
        }

    @Override
    public int getRemainingMessages()
        {
        return f_subscriber.getRemainingMessages();
        }

    @Override
    public int getRemainingMessages(int nChannel)
        {
        return f_subscriber.getRemainingMessages(nChannel);
        }

    @Override
    public SubscriberId getSubscriberId()
        {
        return f_subscriber.getSubscriberId();
        }

    // ----- accessors --------------------------------------------------

    /**
     * Obtain the underlying {@link Subscriber}.
     *
     * @return the underlying {@link Subscriber}
     */
    public Subscriber<F> getSubscriber()
        {
        return f_subscriber;
        }

    /**
     * Obtain the Converter from the underlying {@link Subscriber}.
     *
     * @return the Converter from the underlying {@link Subscriber}
     */
    public Converter<F, T> getConverterUp()
        {
        return f_convUp;
        }

    /**
     * Obtain the Converter to the underlying {@link Subscriber}.
     *
     * @return the Converter to the underlying {@link Subscriber}
     */
    public Converter<T, F> getConverterDown()
        {
        return f_convDown;
        }

    // ----- data members -----------------------------------------------

    /**
     * The underlying {@link Subscriber}.
     */
    protected final Subscriber<F> f_subscriber;

    /**
     * The Converter from the underlying {@link NamedTopic}.
     */
    protected final Converter<F, T> f_convUp;

    /**
     * The converter from the underlying binary format to the "viewing" binary format
     */
    protected final Converter<Binary, Binary> f_convBinaryUp;

    /**
     * The Converter to the underlying {@link NamedTopic}.
     */
    protected final Converter<T, F> f_convDown;

    /**
     * The converter from the "viewing" binary format to the underlying binary format
     */
    protected final Converter<Binary, Binary> f_convBinaryDown;

    /**
     * The converter {@link NamedTopic}.
     */
    protected final ConverterNamedTopic<F, T> f_Topic;
    }