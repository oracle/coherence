/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.topic;

/**
 * A exception that occurred during an operation on a {@link NamedTopic}.
 *
 * @author Jonathan Knight  2021.05.05
 * @since 21.06
 */
public class TopicException
        extends RuntimeException
    {
    /**
     * Create a {@link TopicException}.
     *
     * @param sMessage  the error message
     */
    public TopicException(String sMessage)
        {
        super(sMessage);
        }

    /**
     * Create a {@link TopicException}.
     *
     * @param cause the root cause exception
     */
    public TopicException(Throwable cause)
        {
        this(cause.getMessage(), cause);
        }

    /**
     * Create a {@link TopicException}.
     *
     * @param sMessage  the error message
     * @param cause     the root cause exception
     */
    public TopicException(String sMessage, Throwable cause)
        {
        super(sMessage, cause);
        }
    }
