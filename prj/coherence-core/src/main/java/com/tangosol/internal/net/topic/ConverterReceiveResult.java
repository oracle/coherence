/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.tangosol.internal.net.topic.impl.paged.model.PageElement;

import com.tangosol.io.SerializationSupport;

import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.ConverterCollections;

import java.io.ObjectStreamException;

import java.util.Queue;

/**
 * A {@link ConverterPublisherConnector} view an underlying {@link ReceiveResult}
 * through a set of {@link Converter} instances.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class ConverterReceiveResult
        implements ReceiveResult, SerializationSupport
    {
    /**
     * Create a {@link ConverterReceiveResult}.
     *
     * @param result    the underlying {@link ReceiveResult}
     * @param convUp    the converter to convert binary values to the underlying topic
     * @param convDown  the converter to convert binary values from the underlying topic
     */
    public ConverterReceiveResult(ReceiveResult result, Converter<Binary, Binary> convUp, Converter<Binary, Binary> convDown)
        {
        f_result   = result;
        f_convUp   = convUp;
        f_convDown = convDown;
        }

    @Override
    public Queue<Binary> getElements()
        {
        return new ConverterCollections.ConverterQueue<>(f_result.getElements(), new ConverterElement(), f_convDown);
        }

    @Override
    public int getRemainingElementCount()
        {
        return f_result.getRemainingElementCount();
        }

    @Override
    public Status getStatus()
        {
        return f_result.getStatus();
        }

    @Override
    public Object writeReplace() throws ObjectStreamException
        {
        return new SimpleReceiveResult(getElements(), getRemainingElementCount(), getStatus());
        }

    // ----- inner class: ConverterElement ----------------------------------

    /**
     * A {@link ConverterElement} converts a received element from
     * an underlying topic.
     */
    protected class ConverterElement
            implements Converter<Binary, Binary>
        {
        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Binary convert(Binary binary)
            {
            PageElement element = PageElement.fromBinary(binary, f_convDown);
            return element.convert(f_convDown);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The underlying {@link ReceiveResult}.
     */
    private final ReceiveResult f_result;

    /**
     * The converter to convert binary values to the underlying topic.
     */
    private final Converter<Binary, Binary> f_convUp;

    /**
     * The converter to convert binary values from the underlying topic.
     */
    private final Converter<Binary, Binary> f_convDown;
    }
