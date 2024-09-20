/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.net.management.annotation.Description;

/**
 * A class that can return its health status
 * <p>
 * Applications can implement custom health checks and register
 * them with the Coherence Management framework using
 * {@link com.tangosol.net.management.Registry#register(HealthCheck)}.
 * <p>
 * Custom health checks, and built-in Coherence health checks will then
 * appear as Coherence MBeans and can also be queried using the Coherence
 * REST management and health APIs.
 *
 * @author Jonathan Knight  2022.02.14
 * @since 22.06
 *
 * @see com.tangosol.net.management.Registry#register(HealthCheck)
 * @see com.tangosol.net.management.Registry#unregister(HealthCheck)
 * @see com.tangosol.net.management.Registry#getHealthChecks()
 */
public interface HealthCheck
    {
    /**
     * Returns the unique name of this health check.
     *
     * @return the unique name of this health check
     */
    @Description("The unique name of this health check.")
    String getName();

    /**
     * Return {@code true} if this {@link HealthCheck} should
     * be included when working out this Coherence member's
     * health status.
     *
     * @return {@code true} if this {@link HealthCheck} should
     *         be included in the member's health status
     */
    @Description("Indicates if this health check should be included when working out this Coherence member's health status.")
    default boolean isMemberHealthCheck()
        {
        return true;
        }

    /**
     * Returns {@code true} if the resource represented by
     * this {@link HealthCheck} is ready, otherwise returns
     * {@code false}.
     * <p>
     * The concept of what "ready" means may vary for different
     * types of resources.
     *
     * @return {@code true} if the resource represented by this
     *         {@link HealthCheck} is ready, otherwise {@code false}
     */
    @Description("Indicates if the resource represented by this health check is ready.")
    boolean isReady();

    /**
     * Returns {@code true} if the resource represented by
     * this {@link HealthCheck} is alive, otherwise returns
     * {@code false}.
     * <p>
     * The concept of what "alive" means may vary for different
     * types of resources.
     *
     * @return {@code true} if the resource represented by this
     *         {@link HealthCheck} is alive, otherwise returns
     *         {@code false}
     */
    @Description("Indicates if the resource represented by this health check is alive.")
    boolean isLive();

    /**
     * Returns {@code true} if the resource represented by
     * this {@link HealthCheck} is started, otherwise returns
     * {@code false}.
     * <p>
     * The concept of what "started" means may vary for different
     * types of resources.
     *
     * @return {@code true} if the resource represented by this
     *         {@link HealthCheck} is started, otherwise returns
     *         {@code false}
     */
    @Description("Indicates if the resource represented by this health check is started.")
    boolean isStarted();

    /**
     * Returns {@code true} if the resource represented by this
     * {@link HealthCheck} is in a safe state to allow a rolling
     * upgrade to proceed, otherwise returns {@code false}.
     * <p>
     * The concept of what "safe" means may vary for different
     * types of resources.
     *
     * @return {@code true} if the resource represented by this
     *         {@link HealthCheck} is in a safe state to allow
     *         a rolling upgrade to proceed, otherwise returns
     *         {@code false}
     */
    @Description("Indicates if the resource represented by this health check is safe.")
    boolean isSafe();

    /**
     * The http request path for the ready endpoint.
     */
    String PATH_READY = "/ready";

    /**
     * The http request path for the live endpoint.
     */
    String PATH_LIVE = "/live";

    /**
     * The http request path for the healthz endpoint.
     */
    String PATH_HEALTHZ = "/healthz";

    /**
     * The http request path for the started endpoint.
     */
    String PATH_STARTED = "/started";

    /**
     * The http request path for the safe endpoint.
     */
    String PATH_SAFE = "/safe";
    }
