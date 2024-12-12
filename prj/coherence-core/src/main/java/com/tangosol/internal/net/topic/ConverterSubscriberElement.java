/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Binary;
import com.tangosol.util.Converter;

import java.time.Instant;

import java.util.concurrent.CompletableFuture;

/**
 * A {@link ConverterSubscriberElement} view an underlying {@link Subscriber.Element}
 * through a set of {@link Converter} instances.
 *
 * @param <F>  the type of element value in the underlying topic
 * @param <T>  the type of element value exposed by this converter element
 *
 * @author Jonathan Knight  2024.11.26
 */
public class ConverterSubscriberElement<F, T>
        implements Subscriber.Element<T>
    {
    /**
     * Create a {@link ConverterSubscriberElement}.
     *
     * @param element    the underlying {@link Subscriber.Element}
     * @param convUp     the Converter from the underlying {@link NamedTopic}
     * @param convBinUp  the converter that converts a {@link Binary} serialized in the underlying
     *                   topic's format to a {@link Binary} using the "from" serializer
     */
    public ConverterSubscriberElement(Subscriber.Element<F> element, Converter<F, T> convUp, Converter<Binary, Binary> convBinUp)
        {
        f_element   = element;
        f_convUp    = convUp;
        f_convBinUp = convBinUp;
        }

    @Override
    public T getValue()
        {
        T oValue = m_oValue;
        if (oValue == null)
            {
            oValue = m_oValue = f_convUp.convert(f_element.getValue());
            }
        return oValue;
        }

    @Override
    public Binary getBinaryValue()
        {
        Binary binary = m_binary;
        if (binary == null)
            {
            binary = m_binary = f_convBinUp.convert(f_element.getBinaryValue());
            }
        return binary;
        }

    @Override
    public int getChannel()
        {
        return f_element.getChannel();
        }

    @Override
    public Position getPosition()
        {
        return f_element.getPosition();
        }

    @Override
    public Instant getTimestamp()
        {
        return f_element.getTimestamp();
        }

    @Override
    public CompletableFuture<Subscriber.CommitResult> commitAsync()
        {
        return f_element.commitAsync();
        }

    // ----- data members ---------------------------------------------------

    private final Subscriber.Element<F> f_element;

    /**
     * The Converter from the underlying {@link NamedTopic}.
     */
    private final Converter<F, T> f_convUp;

    /**
     * The Converter from a {@link Binary} in the underlying format to a {@link Binary}
     * in the "viewing" format.
     */
    private final Converter<Binary, Binary> f_convBinUp;

    /**
     * The lazily converted value.
     */
    private T m_oValue;

    /**
     * The lazily converted binary value.
     */
    private Binary m_binary;
    }
