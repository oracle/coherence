/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;

import com.tangosol.net.management.Registry;

/**
 * A {@link RemoteCallable} to determine whether a Coherence member is "safe"
 * by calling {@link Registry#allHealthChecksSafe()} ()}.
 *
 * @author Jonathan Knight 2022.04.21
 * @since 22.06
 */
public class IsSafe
        implements RemoteCallable<Boolean>
    {
    @Override
    public Boolean call() throws Exception
        {
        try
            {
            Cluster cluster = CacheFactory.getCluster();
            if (cluster != null && cluster.isRunning())
                {
                boolean fReady = cluster.getManagement().allHealthChecksSafe();
                if (!fReady)
                    {
                    Logger.info("Bedrock: IsSafe check failed");
                    }
                return true;
                }
            else
                {
                Logger.info("Bedrock: IsSafe check - cluster is null or not running");
                return false;
                }
            }
        catch (Exception e)
            {
            Logger.err("Bedrock: IsSafe check failed", e);
            return false;
            }
        }

    /**
     * A singleton instance of an {@link IsSafe} callable.
     */
    public static final IsSafe INSTANCE = new IsSafe();
    }
