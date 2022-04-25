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
 * A {@link RemoteCallable} to determine whether a Coherence member is ready
 * by calling {@link Registry#allHealthChecksReady()}.
 *
 * @author Jonathan Knight 2022.04.21
 * @since 22.06
 */
public class IsReady
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
                boolean fReady = cluster.getManagement().allHealthChecksReady();
                if (!fReady)
                    {
                    Logger.info("Bedrock: IsReady check failed");
                    }
                return true;
                }
            else
                {
                Logger.info("Bedrock: IsReady check - cluster is null or not running");
                return false;
                }
            }
        catch (Exception e)
            {
            Logger.err("Bedrock: IsReady check failed", e);
            return false;
            }
        }

    /**
     * A singleton instance of an {@link IsReady} callable.
     */
    public static final IsReady INSTANCE = new IsReady();
    }
