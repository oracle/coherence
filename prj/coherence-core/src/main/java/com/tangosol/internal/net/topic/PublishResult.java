/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.tangosol.net.topic.Publisher;

import com.tangosol.util.LongArray;

/**
 * The result of publishing a message to a {@link com.tangosol.net.topic.NamedTopic}.
 *
 * @author Jonathan Knight  2024.11.26
 */
public interface PublishResult
    {
    /**
     * Obtain the result {@link Status}.
     *
     * @return the result {@link Status}
     */
    Status getStatus();

    /**
     * Obtain any errors which occurred as part of the offer.
     *
     * @return the LongArray of exceptions
     */
    LongArray<Throwable> getErrors();

    /**
     * Obtain the number of elements accepted into the page.
     *
     * @return the number of elements accepted into the page
     */
    int getAcceptedCount();

    /**
     * Obtain the identifier of the channel published to.
     *
     * @return the identifier of the channel published to
     */
    int getChannelId();

    /**
     * Obtain the status of the individual messages published.
     *
     * @return the status of the individual messages published
     */
    LongArray<Publisher.Status> getPublishStatus();

    /**
     * Obtain the maximum remaining capacity.
     * <p>
     * This value can be used to limit the size of the next
     * offer to the topic.
     *
     * @return the maximum capacity
     */
    int getRemainingCapacity();

    /**
     * Obtain the opaque cookie to use to retry an offer.
     *
     * @return the opaque cookie to use to retry an offer
     */
    Object getRetryCookie();

    // ----- inner enum Status ----------------------------------------------

    /**
     * The status of an offer.
     */
    enum Status
        {
        /**
         * The offer invocation was successful and all elements were
         * accepted into the page.
         */
        Success,

        /**
         * The offer invocation was unsuccessful but should be retried.
         */
        Retry,

        /**
         * The offer invocation was unsuccessful as the topic was full.
         * The offer may have been partially successful if multiple elements
         * had been offered.
         */
        TopicFull
        }
    }
