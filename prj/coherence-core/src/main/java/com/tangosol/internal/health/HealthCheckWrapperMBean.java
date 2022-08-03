/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.health;

import com.tangosol.net.management.Registry;

import com.tangosol.net.management.annotation.Description;

import com.tangosol.util.HealthCheck;

/**
 * An MBean interface that wraps a {@link HealthCheck}.
 *
 * @author Jonathan Knight 2022.04.22
 * @since 22.06
 */
public interface HealthCheckWrapperMBean
        extends HealthCheck
    {
    /**
     * Return the {@link HealthCheck health check's} subtype.
     *
     * @return the {@link HealthCheck health check's} subtype
     */
    @Description("The sub-type of this health check.")
    String getSubType();

    /**
     * Returns the name of the wrapped {@link HealthCheck} class.
     *
     * @return the name of the wrapped {@link HealthCheck} class
     */
    @Description("The name of the class implementing this health check.")
    String getClassName();

    /**
     * Returns the result of calling {code toString()} on the wrapped {@link HealthCheck}.
     *
     * @return the result of calling {code toString()} on the wrapped {@link HealthCheck}
     */
    @Description("The description of this health check.")
    String getDescription();

    /**
     * The string pattern for a cluster health check MBean Object name.
     */
    String MBEAN_CLUSTER_PATTERN = Registry.HEALTH_TYPE + ",subType=%s";

    /**
     * The string pattern for a health check MBean Object name.
     */
    String MBEAN_NAME_PATTERN = MBEAN_CLUSTER_PATTERN + ",name=%s";

    /**
     * The health check subtype for a {@link com.tangosol.net.Coherence} health check.
     */
    String SUBTYPE_COHERENCE = "Coherence";

    /**
     * The health check subtype for a {@link com.tangosol.net.Cluster} health check.
     */
    String SUBTYPE_CLUSTER = "Cluster";

    /**
     * The health check subtype for a {@link com.tangosol.net.Service} health check.
     */
    String SUBTYPE_SERVICE = "Service";

    /**
     * The health check subtype for a custom application health check.
     */
    String SUBTYPE_APPLICATION = "Application";
    }
