/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.health;

import com.tangosol.util.HealthCheck;

// # tag::custom[]

/**
 * A simple custom health check.
 */
public class ApplicationHealth
        implements HealthCheck {

    /**
     * The health check name.
     */
    public static final String NAME = "Demo";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public boolean isLive() {
        return true;
    }

    @Override
    public boolean isStarted() {
        return true;
    }

    @Override
    public boolean isSafe() {
        return true;
    }
}
// # end::custom[]
