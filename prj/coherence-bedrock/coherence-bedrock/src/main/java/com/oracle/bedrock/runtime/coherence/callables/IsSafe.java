/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.tangosol.net.management.Registry;

import com.tangosol.util.HealthCheck;

/**
 * A {@link RemoteCallable} to determine whether a Coherence member is "safe"
 * by calling {@link Registry#allHealthChecksSafe()} ()}.
 *
 * @author Jonathan Knight 2022.04.21
 * @since 22.06
 */
public class IsSafe
        extends AbstractHealthCheckCallable
        implements RemoteCallable<Boolean>
    {
    /**
     * Create a {@link IsSafe} to verify all member health checks are safe.
     */
    public IsSafe()
        {
        this(null);
        }

    /**
     * Create a {@link IsSafe} to verify all member health checks are safe.
     *
     * @param sName  the name of an additional HealthCheck to test
     */
    public IsSafe(String sName)
        {
        super(sName);
        }

    @Override
    protected boolean check(Registry registry)
        {
        return registry.allHealthChecksSafe();
        }

    @Override
    protected boolean check(HealthCheck healthCheck)
        {
        return healthCheck.isSafe();
        }

    /**
     * A singleton instance of an {@link IsSafe} callable.
     */
    public static final IsSafe INSTANCE = new IsSafe();
    }
