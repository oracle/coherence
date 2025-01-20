/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import com.oracle.coherence.common.base.Exceptions;

import com.oracle.coherence.grpc.messages.common.v1.CollectionOfInt32;

/**
 * Common helper methods for gRPC messages.
 *
 * @author Jonathan Knight  2025.01.25
 */
public class MessageHelper
    {
    /**
     * Private constructor for helper class.
     */
    private MessageHelper()
        {
        }

    /**
     * Convert a {@link CollectionOfInt32} to an {@code int} array.
     *
     * @param col  the {@link CollectionOfInt32} to convert
     *
     * @return an {@link int} array from the values in the {@link CollectionOfInt32}
     */
    public static int[] toIntArray(CollectionOfInt32 col)
        {
        if (col == null)
            {
            return new int[0];
            }

        int[] an = new int[col.getValuesCount()];
        for (int i = 0; i < an.length; i++)
            {
            an[i] = col.getValues(i);
            }
        return an;
        }

    /**
     * Convert an iterable of integers to a {@link CollectionOfInt32}.
     *
     * @param col  the iterable to convert
     * @param <C>  the type of the iterable
     *
     * @return a {@link CollectionOfInt32} containing the integers from the iterable
     */
    public static <C extends Iterable<Integer>> CollectionOfInt32 toCollectionOfInt32(C col)
        {
        CollectionOfInt32.Builder builder = CollectionOfInt32.newBuilder();
        for (Integer i : col)
            {
            builder.addValues(i);
            }
        return builder.build();
        }

    /**
     * Convert an int array of integers to a {@link CollectionOfInt32}.
     *
     * @param an   the int array
     * @param <C>  the type of the iterable
     *
     * @return a {@link CollectionOfInt32} containing the integers from the array
     */
    public static <C extends Iterable<Integer>> CollectionOfInt32 toCollectionOfInt32(int[] an)
        {
        CollectionOfInt32.Builder builder = CollectionOfInt32.newBuilder();
        for (int i : an)
            {
            builder.addValues(i);
            }
        return builder.build();
        }

    /**
     * Unpack an {@link Any} message.
     *
     * @param any   the request to get the message from
     * @param type  the expected type of the message
     * @param <T>   the expected type of the message
     *
     * @return the unpacked message
     */
    public static  <T extends Message> T unpack(Any any, Class<T> type)
        {
        if (any == null)
            {
            return null;
            }
        try
            {
            return any.unpack(type);
            }
        catch (InvalidProtocolBufferException e)
            {
            throw Exceptions.ensureRuntimeException(e, "Could not unpack message field of type " + type.getName() + " actual type " + any.getTypeUrl());
            }
        }

    }
