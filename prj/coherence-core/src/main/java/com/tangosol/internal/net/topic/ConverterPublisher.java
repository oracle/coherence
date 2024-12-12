/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.tangosol.net.FlowControl;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;

import com.tangosol.util.Converter;

import java.util.concurrent.CompletableFuture;

/**
 * A Converter {@link Publisher} views an underlying {@link Publisher}
 * through a {@link Converter}.
 *
 * @param <F> the type of elements the underlying {@link Publisher} publishes
 * @param <T> the type that the elements should be converted to
 *
 * @author Jonathan Knight  2024.11.26
 */
public class ConverterPublisher<F, T>
        implements Publisher<T>
    {
    // ----- constructors -----------------------------------------------

    /**
     * Constructor.
     *
     * @param publisher  the underlying {@link Publisher}
     * @param topic      the parent {@link ConverterNamedTopic}
     */
    public ConverterPublisher(Publisher<F> publisher, ConverterNamedTopic<F, T> topic)
        {
        f_publisher = publisher;
        f_topic     = topic;
        f_convUp    = topic.getConverterUp();
        f_convDown  = topic.getConverterDown();
        }

    // ----- Publisher methods -----------------------------------------

    @Override
    public CompletableFuture<Status> publish(T value)
        {
        return f_publisher.publish(f_convDown.convert(value));
        }

    @Override
    public FlowControl getFlowControl()
        {
        return f_publisher.getFlowControl();
        }

    @Override
    public CompletableFuture<Void> flush()
        {
        return f_publisher.flush();
        }

    @Override
    public void close()
        {
        f_publisher.close();
        }

    @Override
    public void onClose(Runnable action)
        {
        f_publisher.onClose(action);
        }

    @Override
    public int getChannelCount()
        {
        return f_publisher.getChannelCount();
        }

    @Override
    public NamedTopic<T> getNamedTopic()
        {
        return f_topic;
        }

    @Override
    public boolean isActive()
        {
        return f_publisher.isActive();
        }

    @Override
    public long getId()
        {
        return f_publisher.getId();
        }

    // ----- accessors --------------------------------------------------

    /**
     * Obtain the underlying {@link Publisher}.
     *
     * @return the underlying {@link Publisher}
     */
    public Publisher<F> getPublisher()
        {
        return f_publisher;
        }

    /**
     * Obtain the Converter from the underlying {@link Publisher}.
     *
     * @return the Converter from the underlying {@link Publisher}
     */
    public Converter<F, T> getConverterUp()
        {
        return f_convUp;
        }

    /**
     * Obtain the Converter to the underlying {@link Publisher}.
     *
     * @return the Converter to the underlying {@link Publisher}
     */
    public Converter<T, F> getConverterDown()
        {
        return f_convDown;
        }

    // ----- data members -----------------------------------------------

    /**
     * The underlying {@link Publisher}.
     */
    private final Publisher<F> f_publisher;

    /**
     * The parent {@link ConverterNamedTopic}.
     */
    private final ConverterNamedTopic<F, T> f_topic;

    /**
     * The Converter from the underlying {@link NamedTopic}.
     */
    private final Converter<F, T> f_convUp;

    /**
     * The Converter to the underlying {@link NamedTopic}.
     */
    private final Converter<T, F> f_convDown;
    }
