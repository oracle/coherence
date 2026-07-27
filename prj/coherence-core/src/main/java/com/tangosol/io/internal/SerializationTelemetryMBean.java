/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import com.tangosol.net.management.annotation.Description;
/**
 * Base management view for serialization gate telemetry counters.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public interface SerializationTelemetryMBean
    {
    /**
     * Return the tuple counter.
     *
     * @return the tuple counter
     */
    @Description("The serialization gate tuple count.")
    public long getCount();
    }
