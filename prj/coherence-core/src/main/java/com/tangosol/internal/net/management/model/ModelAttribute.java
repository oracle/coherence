/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management.model;

import com.tangosol.net.metrics.MBeanMetric;

import javax.management.MBeanAttributeInfo;
import javax.management.openmbean.OpenType;

import java.util.function.Function;

/**
 * An attribute in an {@link AbstractModel MBean model}.
 *
 * @param <M> the type of the parent model
 *
 * @author Jonathan Knight 2022.09.10
 * @since 23.03
 */
public interface ModelAttribute<M>
    {
    /**
     * Returns the attribute name.
     *
     * @return the attribute name
     */
    String getName();

    /**
     * Returns the attribute description.
     *
     * @return the attribute description
     */
    String getDescription();

    /**
     * Returns the attribute type.
     *
     * @return the attribute type
     */
    OpenType<?> getType();

    /**
     * Returns the {@link Function} to use to obtain the attribute value.
     *
     * @return the {@link Function} to use to obtain the attribute value
     */
    Function<M, ?> getFunction();

    /**
     * Returns the attribute {@link MBeanAttributeInfo}.
     *
     * @return the attribute {@link MBeanAttributeInfo}
     */
    MBeanAttributeInfo getMBeanAttributeInfo();

    /**
     * Returns a flag indicating whether the attribute is a metric.
     *
     * @return {@code true} if the attribute is a metric
     */
    boolean isMetric();

    /**
     * Returns the metrics scope.
     *
     * @return the metrics scope
     */
    MBeanMetric.Scope getMetricScope();

    // ----- constants ------------------------------------------------------

    /**
     * Metric label for mean rate.
     */
    String RATE_MEAN  = "mean";

    /**
     * Metric label for 1-minute rate.
     */
    String RATE_1MIN  = "1min";

    /**
     * Metric label for 5-minute rate.
     */
    String RATE_5MIN  = "5min";

    /**
     * Metric label for 15-minute rate.
     */
    String RATE_15MIN = "15min";
    }
