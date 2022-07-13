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
 * A {@link RemoteCallable} to determine whether a Coherence member is ready
 * by calling {@link Registry#allHealthChecksReady()}.
 *
 * @author Jonathan Knight 2022.04.21
 * @since 22.06
 */
public class IsReady
        extends AbstractHealthCheckCallable
        implements RemoteCallable<Boolean>
    {
    /**
     * Create a {@link IsReady} to verify all member health checks are ready.
     */
    public IsReady()
        {
        this(null);
        }

    /**
     * Create a {@link IsReady} to verify all member health checks are ready.
     *
     * @param sName  the name of an additional HealthCheck to test
     */
    public IsReady(String sName)
        {
        super(sName);
        }

    @Override
    protected boolean check(Registry registry)
        {
        return registry.allHealthChecksReady();
        }

    @Override
    protected boolean check(HealthCheck healthCheck)
        {
        return healthCheck.isReady();
        }

    /**
     * A singleton instance of an {@link IsReady} callable.
     */
    public static final IsReady INSTANCE = new IsReady(null);
    }
