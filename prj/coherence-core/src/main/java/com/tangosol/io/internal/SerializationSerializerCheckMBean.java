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
 * Management view for serializer-name checks.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.07
 */
@MetricsScope(MBeanMetric.Scope.VENDOR)
@Description("Provides serializer-name check counters.")
public interface SerializationSerializerCheckMBean
        extends SerializationTelemetryMBean
    {
    @Override
    @MetricsValue("coh.serialization.serializer_check")
    @Description("The serializer-name check tuple count.")
    public long getCount();
    }
