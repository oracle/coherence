/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import com.tangosol.net.management.annotation.Description;
import com.tangosol.net.management.annotation.MetricsScope;
import com.tangosol.net.management.annotation.MetricsValue;

import com.tangosol.net.metrics.MBeanMetric;

/**
 * Management view for serialization filter checks.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.07
 */
@MetricsScope(MBeanMetric.Scope.VENDOR)
@Description("Provides serialization filter check counters.")
public interface SerializationFilterCheckMBean
        extends SerializationTelemetryMBean
    {
    @Override
    @MetricsValue("coh.serialization.filter_check")
    @Description("The serialization filter check tuple count.")
    public long getCount();
    }
