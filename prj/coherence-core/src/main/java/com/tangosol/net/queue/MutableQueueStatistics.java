/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.queue;

/**
 * An interface for updating {@link com.tangosol.net.NamedQueue} statistics.
 */
public interface MutableQueueStatistics
        extends QueueStatistics
    {
    /**
     * Add a poll to the statistics.
     *
     * @param cNanos  the time taken to complete the poll (in nanos)
     */
    void polled(long cNanos);

    /**
     * Add an offer to the statistics.
     *
     * @param cNanos  the time taken to complete the offer (in nanos)
     */
    void offered(long cNanos);

    void registerHit();

    void registerMiss();

    void registerAccepted();

    void registerRejected();
    }
