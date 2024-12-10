/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.topic;

import com.tangosol.io.Serializer;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import java.util.function.BiFunction;

/**
 * An exception that occurred during publishing of a message on a topic.
 *
 * @author Jonathan Knight  2021.06.03
 * @since 21.06
 */
public class TopicPublisherException
        extends TopicException
    {
    /**
     * Create a {@link TopicPublisherException}.
     *
     * @param sMessage    the exception message
     * @param binValue    the binary value that failed to be published
     * @param serializer  the serializer to deserialize the value
     */
    public TopicPublisherException(String sMessage, Binary binValue, Serializer serializer)
        {
        super(sMessage);
        f_binValue   = binValue;
        f_serializer = serializer;
        }

    /**
     * Create a {@link TopicPublisherException}.
     *
     * @param cause       the underlying cause of the exception
     * @param binValue    the binary value that failed to be published
     * @param serializer  the serializer to deserialize the value
     */
    public TopicPublisherException(Throwable cause, Binary binValue, Serializer serializer)
        {
        super(cause);
        f_binValue   = binValue;
        f_serializer = serializer;
        }

    /**
     * Create a {@link TopicPublisherException}.
     *
     * @param sMessage    the exception message
     * @param cause       the underlying cause of the exception
     * @param binValue    the binary value that failed to be published
     * @param serializer  the serializer to deserialize the value
     */
    public TopicPublisherException(String sMessage, Throwable cause, Binary binValue, Serializer serializer)
        {
        super(sMessage, cause);
        f_binValue   = binValue;
        f_serializer = serializer;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns the value that failed to be published in serialized {@link Binary} format.
     *
     * @return the value that failed to be published in serialized {@link Binary} format.
     */
    public Binary getBinaryValue()
        {
        return f_binValue;
        }

    /**
     * Returns the deserialized value that failed to be published.
     *
     * @return  the deserialized value that failed to be published
     */
    public Object getValue()
        {
        return ExternalizableHelper.fromBinary(f_binValue, f_serializer);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create a factory function that creates a {@link TopicPublisherException}.
     *
     * @param serializer  the serializer to deserialize the value
     *
     * @return a factory function that creates a {@link TopicPublisherException}
     */
   public static BiFunction<Throwable, Binary, Throwable> createFactory(Serializer serializer)
        {
        return (error, binary) -> new TopicPublisherException(error, binary, serializer);
        }

    /**
     * Create a factory function that creates a {@link TopicPublisherException}.
     *
     * @param serializer  the serializer to deserialize the value
     * @param sReason     the reason message for the exception
     *
     * @return a factory function that creates a {@link TopicPublisherException}
     */
    public static BiFunction<Throwable, Binary, Throwable> createFactory(Serializer serializer, String sReason)
        {
        return (error, binary) -> new TopicPublisherException(sReason, error, binary, serializer);
        }

    // ----- data members ---------------------------------------------------

    /**
     * the value that failed to be published in serialized {@link Binary} format.
     */
    private final Binary f_binValue;

    /**
     * The serializer to use to deserialize the binary value.
     */
    private final Serializer f_serializer;
    }
