/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.management;

import com.tangosol.internal.net.management.model.SimpleModelAttribute;

/**
 * A type that provides topic subscriber metrics.
 *
 * @author Jonathan Knight 2022.09.10
 * @since 23.03
 */
public interface PolledMetrics
    {
    /**
     * Return the polled messages count.
     *
     * @return  the polled messages count
     */
    long getPolledCount();

    /**
     * Return the polled messages fifteen-minute rate.
     *
     * @return  the polled messages fifteen minute rate
     */
    double getPolledFifteenMinuteRate();

    /**
     * Return the polled messages five-minute rate.
     *
     * @return  the polled messages five minute rate
     */
    double getPolledFiveMinuteRate();

    /**
     * Return the polled messages one-minute rate.
     *
     * @return  the polled messages one minute rate
     */
    double getPolledOneMinuteRate();

    /**
     * Return the polled messages mean rate.
     *
     * @return  the polled messages mean rate
     */
    double getPolledMeanRate();

    /**
     * The polled message count attribute template.
     */
    SimpleModelAttribute<?> ATTRIBUTE_COUNT = SimpleModelAttribute.longBuilder("PolledCount", Object.class)
            .withDescription("The number of polled messages")
            .withMetricLabels("count")
            .build();

    /**
     * The polled message mean rate attribute template.
     */
    SimpleModelAttribute<?> ATTRIBUTE_MEAN_RATE = SimpleModelAttribute.doubleBuilder("PolledMeanRate", Object.class)
            .withDescription("The polled messages mean rate")
            .withMetricLabels("rate", "mean")
            .build();

    /**
     * The polled message one-minute rate attribute template.
     */
    SimpleModelAttribute<?> ATTRIBUTE_ONE_MINUTE_RATE = SimpleModelAttribute.doubleBuilder("PolledOneMinuteRate", Object.class)
            .withDescription("The polled messages one-minute rate")
            .withMetricLabels("rate", "1min")
            .build();

    /**
     * The polled message five-minute rate attribute template.
     */
    SimpleModelAttribute<?> ATTRIBUTE_FIVE_MINUTE_RATE = SimpleModelAttribute.doubleBuilder("PolledFiveMinuteRate", Object.class)
            .withDescription("The polled messages five-minute rate")
            .withMetricLabels("rate", "5min")
            .build();

    /**
     * The polled message fifteen-minute rate attribute template.
     */
    SimpleModelAttribute<?> ATTRIBUTE_FIFTEEN_MINUTE_RATE = SimpleModelAttribute.doubleBuilder("PolledFifteenMinuteRate", Object.class)
            .withDescription("The polled messages fifteen-minute rate")
            .withMetricLabels("rate", "15min")
            .build();    }
