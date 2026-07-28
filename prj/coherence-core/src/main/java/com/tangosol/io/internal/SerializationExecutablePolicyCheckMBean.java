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
 * Management view for remote executable policy checks.
 *
 * @author Aleks Seovic  2026.05.07
 * @since 26.04
 */
@MetricsScope(MBeanMetric.Scope.VENDOR)
@Description("Provides remote executable policy check counters.")
public interface SerializationExecutablePolicyCheckMBean
        extends SerializationTelemetryMBean
    {
    @Override
    @MetricsValue("coh.executable.policy_check")
    @Description("The remote executable policy check tuple count.")
    public long getCount();
    }
