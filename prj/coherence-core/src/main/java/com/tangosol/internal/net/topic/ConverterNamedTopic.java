/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.tangosol.net.Service;

import com.tangosol.net.TopicService;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.NamedTopicEvent;
import com.tangosol.net.topic.NamedTopicListener;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;

import java.util.Objects;
import java.util.Set;

/**
 * A Converter {@link NamedTopic} views an underlying {@link NamedTopic}
 * through a {@link Converter}.
 *
 * @param <F> the type of elements in the underlying {@link NamedTopic}
 * @param <T> the type that the elements should be converted to
 *
 * @author Jonathan Knight  2024.11.26
 */
public class ConverterNamedTopic<F, T>
        implements NamedTopic<T>
    {
    // ----- constructors -----------------------------------------------

    /**
     * Constructor.
     *
     * @param topic           the underlying {@link NamedTopic}
     * @param convUp          the Converter from the underlying {@link NamedTopic}
     * @param convBinaryUp    the converter that converts a {@link Binary} serialized in the underlying
     *                        connector's format to a {@link Binary} using the "from" serializer
     * @param convDown        the Converter to the underlying {@link NamedTopic}
     * @param convBinaryDown  the converter that converts a {@link Binary} serialized in the "from"
     *                        format to a {@link Binary} using the underlying connector's serializer
     */
    public ConverterNamedTopic(NamedTopic<F> topic, Converter<F, T> convUp, Converter<Binary, Binary> convBinaryUp,
            Converter<T, F> convDown, Converter<Binary, Binary> convBinaryDown)
        {
        f_topic          = topic;
        f_convUp         = convUp;
        f_convBinaryUp   = convBinaryUp;
        f_convDown       = convDown;
        f_convBinaryDown = convBinaryDown;
        }

    // ----- NamedTopic methods -----------------------------------------

    @Override
    public Publisher<T> createPublisher()
        {
        return new ConverterPublisher<>(f_topic.createPublisher(), this);
        }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Publisher<T> createPublisher(Publisher.Option<? super T>... options)
        {
        Publisher.Option[] opts = options;
        return new ConverterPublisher<>(f_topic.createPublisher(opts), this);
        }

    @Override
    public Subscriber<T> createSubscriber()
        {
        return new ConverterSubscriber<>(f_topic.createSubscriber(), this);
        }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <U> Subscriber<U> createSubscriber(Subscriber.Option<? super T, U>... options)
        {
        Subscriber.Option[] opts = options;
        return new ConverterSubscriber<>(f_topic.createSubscriber(opts), this);
        }

    @Override
    public void ensureSubscriberGroup(String sName)
        {
        f_topic.ensureSubscriberGroup(sName);
        }

    @Override
    public TopicService getTopicService()
        {
        return f_topic.getTopicService();
        }

    @Override
    public void ensureSubscriberGroup(String sGroup, Filter<?> filter, ValueExtractor<?, ?> extractor)
        {
        f_topic.ensureSubscriberGroup(sGroup, filter, extractor);
        }

    @Override
    public void destroySubscriberGroup(String sGroup)
        {
        f_topic.destroySubscriberGroup(sGroup);
        }

    @Override
    public Set<String> getSubscriberGroups()
        {
        return f_topic.getSubscriberGroups();
        }

    @Override
    public int getChannelCount()
        {
        return f_topic.getChannelCount();
        }

    @Override
    public int getRemainingMessages(String sSubscriberGroup, int... anChannel)
        {
        return f_topic.getRemainingMessages(sSubscriberGroup, anChannel);
        }

    @Override
    public void addListener(NamedTopicListener listener)
        {
        f_topic.addListener(new ConverterTopicListener(listener));
        }

    @Override
    public void removeListener(NamedTopicListener listener)
        {
        f_topic.removeListener(new ConverterTopicListener(listener));
        }

    @Override
    public String getName()
        {
        return f_topic.getName();
        }

    @Override
    public Service getService()
        {
        return getNamedTopic().getService();
        }

    @Override
    public void destroy()
        {
        f_topic.destroy();
        }

    @Override
    public boolean isDestroyed()
        {
        return f_topic.isDestroyed();
        }

    @Override
    public boolean isReleased()
        {
        return f_topic.isReleased();
        }

    @Override
    public int getRemainingMessages(String sSubscriberGroup)
        {
        return f_topic.getRemainingMessages(sSubscriberGroup);
        }

    @Override
    public void close()
        {
        f_topic.close();
        }

    @Override
    public boolean isActive()
        {
        return f_topic.isActive();
        }

    @Override
    public void release()
        {
        f_topic.release();
        }

    // ----- accessors --------------------------------------------------

    /**
     * Obtain the underlying {@link NamedTopic}.
     *
     * @return the underlying {@link NamedTopic}
     */
    public NamedTopic<F> getNamedTopic()
        {
        return f_topic;
        }

    /**
     * Obtain the Converter from the underlying {@link NamedTopic}.
     *
     * @return the Converter from the underlying {@link NamedTopic}
     */
    public Converter<F, T> getConverterUp()
        {
        return f_convUp;
        }

    /**
     * Obtain the Converter to convert a binary value from the underlying {@link NamedTopic}
     * into a binary value in the viewing format.
     *
     * @return the Converter from the underlying {@link NamedTopic} binary format to the
     *         viewing binary format
     */
    public Converter<Binary, Binary> getConverterBinaryUp()
        {
        return f_convBinaryUp;
        }

    /**
     * Obtain the Converter to the underlying {@link NamedTopic}.
     *
     * @return the Converter to the underlying {@link NamedTopic}
     */
    public Converter<T, F> getConverterDown()
        {
        return f_convDown;
        }

    /**
     * Obtain the Converter to convert a binary value to the underlying {@link NamedTopic}
     * binary format.
     *
     * @return the Converter to convert a binary value to the underlying {@link NamedTopic}
     *         binary format
     */
    public Converter<Binary, Binary> getConverterBinaryDown()
        {
        return f_convBinaryDown;
        }

    // ----- inner class ConverterTopicListener -------------------------

    /**
     * A wrapper around a {@link NamedTopicListener} to convert
     * events from the underlying topic to events expected by
     * a viewing listener.
     */
    protected class ConverterTopicListener
            implements NamedTopicListener
        {
        /**
         * Create a {@link ConverterTopicListener}.
         *
         * @param listener  the {@link NamedTopicListener} to convert events to
         */
        public ConverterTopicListener(NamedTopicListener listener)
            {
            f_listener = listener;
            }

        @Override
        public void onEvent(NamedTopicEvent evt)
            {
            f_listener.onEvent(evt.replaceSource(ConverterNamedTopic.this));
            }

        @Override
        public boolean equals(Object o)
            {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            //noinspection unchecked
            ConverterTopicListener that = (ConverterTopicListener) o;
            return Objects.equals(f_listener, that.f_listener);
            }

        @Override
        public int hashCode()
            {
            return Objects.hashCode(f_listener);
            }

        // ----- data members -------------------------------------------

        /**
         * The wrapped {@link NamedTopicListener}.
         */
        private final NamedTopicListener f_listener;
        }

    // ----- data members -----------------------------------------------

    /**
     * The underlying {@link NamedTopic}.
     */
    protected final NamedTopic<F> f_topic;

    /**
     * The Converter from the underlying {@link NamedTopic}.
     */
    protected final Converter<F, T> f_convUp;

    /**
     * The converter that converts a {@link Binary} serialized in the underlying
     * connector's format to a {@link Binary} using the "from" serializer
     */
    protected final Converter<Binary, Binary> f_convBinaryUp;

    /**
     * The Converter to the underlying {@link NamedTopic}.
     */
    protected final Converter<T, F> f_convDown;

    /**
     * The converter that converts a {@link Binary} serialized in the "from"
     * format to a {@link Binary} using the underlying connector's serializer.
     */
    protected final Converter<Binary, Binary> f_convBinaryDown;
    }