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
 * Management view for serialization lambda bytecode checks.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.07
 */
@MetricsScope(MBeanMetric.Scope.VENDOR)
@Description("Provides serialization lambda bytecode check counters.")
public interface SerializationLambdaBytecodeCheckMBean
        extends SerializationTelemetryMBean
    {
    @Override
    @MetricsValue("coh.serialization.lambda_bytecode_check")
    @Description("The serialization lambda bytecode check tuple count.")
    public long getCount();
    }
