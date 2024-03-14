/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.queue;

import com.tangosol.net.metrics.Rates;
import com.tangosol.net.metrics.Snapshot;

import java.util.function.Consumer;

/**
 * An interface for exposing {@link com.tangosol.net.NamedQueue} statistics.
 */
public interface QueueStatistics
    {
    /**
     * Obtain a {@link Snapshot} of the poll metric histogram.
     *
     * @return a {@link Snapshot} of the poll metric histogram
     */
    Snapshot getPollSnapshot();

    /**
     * Obtain the poll rates metric.
     *
     * @return the poll rates metric
     */
    Rates getPollRates();

    /**
     * Obtain a {@link Snapshot} of the offer metric histogram.
     *
     * @return a {@link Snapshot} of the offer metric histogram
     */
    Snapshot getOfferSnapshot();

    /**
     * Obtain the offer rates metric.
     *
     * @return the offer rates metric
     */
    Rates getOfferRates();

    /**
     * Return the number of successful polls, that is
     * polls that returned a non-null value.
     *
     * @return the number of successful polls
     */
    long getHits();

    /**
     * Return the number of unsuccessful polls, that is
     * polls that returned a null value.
     *
     * @return the number of unsuccessful polls
     */
    long getMisses();

    /**
     * Return the number of offers where the value was
     * accepted into the queue.
     *
     * @return the number of offers where the value was
     *         accepted into the queue
     */
    long getAccepted();

    /**
     * Return the number of offers where the value was
     * rejected by the queue.
     *
     * @return the number of offers where the value was
     *         rejected by the queue
     */
    long getRejected();

    /**
     * Log the current metrics as a String.
     *
     * @return the log of the metrics
     */
    default String log()
        {
        StringBuilder s = new StringBuilder();
        logTo(s);
        return s.toString();
        }

    /**
     * Log the current metrics as a String.
     *
     * @param s  the {@link StringBuilder} to append the log to
     */
    void logTo(StringBuilder s);
    }
