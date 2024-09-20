/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.metrics;

/**
 * A rate metrics.
 */
public interface Rates
    {
    /**
     * The total count of operations.
     *
     * @return the total count of operations
     */
    long getCount();

    /**
     * The mean rate, in units of TPS.
     *
     * @return mean rate, in units of TPS
     */
    double getMeanRate();

    /**
     * The one-minute rate, in units of TPS.
     *
     * @return  one-minute rate, in units of TPS
     */
    double getOneMinuteRate();

    /**
     * The five-minute rate, in units of TPS.
     *
     * @return five-minute rate, in units of TPS
     */
    double getFiveMinuteRate();

    /**
     * The fifteen-minute rate, in units of TPS.
     *
     * @return fifteen-minute rate, in units of TPS
     */
    double getFifteenMinuteRate();
    }
