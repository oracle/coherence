/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.metrics;

import java.io.OutputStream;

/**
 * A snapshot of a metrics histogram.
 */
public interface Snapshot
    {
    /**
     * Returns the value at the given quantile.
     *
     * @param quantile    a given quantile, in {@code [0..1]}
     * @return the value in the distribution at {@code quantile}
     */
    double getValue(double quantile);

    /**
     * Returns the entire set of values in the snapshot.
     *
     * @return the entire set of values
     */
    long[] getValues();

    /**
     * Returns the number of values in the snapshot.
     *
     * @return the number of values
     */
    int size();

    /**
     * Returns the median value in the distribution.
     *
     * @return the median value
     */
    default double getMedian() {
        return getValue(0.5);
    }

    /**
     * Returns the value at the 75th percentile in the distribution.
     *
     * @return the value at the 75th percentile
     */
    default double get75thPercentile() {
        return getValue(0.75);
    }

    /**
     * Returns the value at the 95th percentile in the distribution.
     *
     * @return the value at the 95th percentile
     */
    default double get95thPercentile() {
        return getValue(0.95);
    }

    /**
     * Returns the value at the 98th percentile in the distribution.
     *
     * @return the value at the 98th percentile
     */
    default double get98thPercentile() {
        return getValue(0.98);
    }

    /**
     * Returns the value at the 99th percentile in the distribution.
     *
     * @return the value at the 99th percentile
     */
    default double get99thPercentile() {
        return getValue(0.99);
    }

    /**
     * Returns the value at the 99.9th percentile in the distribution.
     *
     * @return the value at the 99.9th percentile
     */
    default double get999thPercentile() {
        return getValue(0.999);
    }

    /**
     * Returns the highest value in the snapshot.
     *
     * @return the highest value
     */
    long getMax();

    /**
     * Returns the arithmetic mean of the values in the snapshot.
     *
     * @return the arithmetic mean
     */
    double getMean();

    /**
     * Returns the lowest value in the snapshot.
     *
     * @return the lowest value
     */
    long getMin();

    /**
     * Returns the standard deviation of the values in the snapshot.
     *
     * @return the standard value
     */
    double getStdDev();

    /**
     * Writes the values of the snapshot to the given stream.
     *
     * @param output an output stream
     */
    void dump(OutputStream output);
    }
