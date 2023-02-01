/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.management;

import com.tangosol.internal.net.management.model.SimpleModelAttribute;

/**
 * A type that provides topic publisher metrics.
 *
 * @author Jonathan Knight 2022.09.10
 * @since 22.06.4
 */
public interface PublishedMetrics
    {
    /**
     * Return the published messages count.
     *
     * @return  the published messages count
     */
    long getPublishedCount();

    /**
     * Return the published messages fifteen-minute rate.
     *
     * @return  the published messages fifteen minute rate
     */
    double getPublishedFifteenMinuteRate();

    /**
     * Return the published messages five-minute rate.
     *
     * @return  the published messages five minute rate
     */
    double getPublishedFiveMinuteRate();

    /**
     * Return the published messages one-minute rate.
     *
     * @return  the published messages one minute rate
     */
    double getPublishedOneMinuteRate();

    /**
     * Return the published messages mean rate.
     *
     * @return  the published messages mean rate
     */
    double getPublishedMeanRate();

    /**
     * The published message count attribute template.
     */
    SimpleModelAttribute<?> ATTRIBUTE_COUNT = SimpleModelAttribute.longBuilder("PublishedCount", Object.class)
            .withDescription("The number of published messages.")
            .metric(true)
            .build();

    /**
     * The published message mean rate attribute template.
     */
    SimpleModelAttribute<?> ATTRIBUTE_MEAN_RATE = SimpleModelAttribute.doubleBuilder("PublishedMeanRate", Object.class)
            .withDescription("The published messages mean rate.")
            .metric("PublishedRate")
            .withMetricLabels("rate", "mean")
            .build();

    /**
     * The published message one-minute rate attribute template.
     */
    SimpleModelAttribute<?> ATTRIBUTE_ONE_MINUTE_RATE = SimpleModelAttribute.doubleBuilder("PublishedOneMinuteRate", Object.class)
            .withDescription("The published messages one-minute rate.")
            .metric("PublishedRate")
            .withMetricLabels("rate", "1min")
            .build();

    /**
     * The published message five-minute rate attribute template.
     */
    SimpleModelAttribute<?> ATTRIBUTE_FIVE_MINUTE_RATE = SimpleModelAttribute.doubleBuilder("PublishedFiveMinuteRate", Object.class)
            .withDescription("The published messages five-minute rate.")
            .metric("PublishedRate")
            .withMetricLabels("rate", "5min")
            .build();

    /**
     * The published message fifteen-minute rate attribute template.
     */
    SimpleModelAttribute<?> ATTRIBUTE_FIFTEEN_MINUTE_RATE = SimpleModelAttribute.doubleBuilder("PublishedFifteenMinuteRate", Object.class)
            .withDescription("The published messages fifteen-minute rate.")
            .metric("PublishedRate")
            .withMetricLabels("rate", "15min")
            .build();
    }
