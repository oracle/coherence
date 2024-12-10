/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.health;

/**
 * The configuration for a health check.
 *
 * @author Jonathan Knight  2022.02.14
 * @since 22.06
 */
public interface HealthCheckDependencies
    {
    /**
     * Returns {@code true} if the health check is included in the
     * member's overall health check.
     *
     * @return {@code true} if the health check is included in the
     *         member's overall health check
     */
    boolean isMemberHealthCheck();

    /**
     * Returns {@code true} if this health check is for a
     * partitioned service that is allowed to be endangered.
     *
     * @return {@code true} if this health check is for a
     *         partitioned service that is allowed to be
     *         endangered
     */
    boolean allowEndangered();
    }
