/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.tangosol.util.Binary;

import java.util.Queue;

/**
 * The result of a subscriber polling a topic.
 *
 * @author Jonathan Knight  2024.11.26
 */
public interface ReceiveResult
    {
    /**
     * Return the polled topic elements in serialized binary format.
     *
     * @return the polled topic elements in serialized binary format
     */
    Queue<Binary> getElements();

    /**
     * Return the number of remaining elements.
     *
     * @return the number of remaining elements
     */
    int getRemainingElementCount();

    /**
     * Return the status of the receive operation.
     *
     * @return the status of the receive operation
     */
    Status getStatus();

    /**
     * The status of a receive operations.
     */
    enum Status
        {
        /**
         * The operation was successful.
         */
        Success,
        /**
         * The channel was empty.
         */
        Exhausted,
        /**
         * The channel polled is not owned by the subscriber.
         */
        NotAllocatedChannel,
        /**
         * The subscriber was not registered with the server.
         */
        UnknownSubscriber
        }
    }
