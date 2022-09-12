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
import com.tangosol.util.HealthCheck;

import java.util.Optional;

/**
 * A base class for {@link RemoteCallable} to determine the state of a
 * Coherence member health check
 *
 * @author Jonathan Knight 2022.07.06
 * @since 22.06.1
 */
public abstract class AbstractHealthCheckCallable
        implements RemoteCallable<Boolean>
    {
    /**
     * Create a {@link AbstractHealthCheckCallable}.
     *
     * @param sName  an additional HealthCheck name to verify
     */
    protected AbstractHealthCheckCallable(String sName)
        {
        m_sName = sName;
        }

    @Override
    public Boolean call() throws Exception
        {
        try
            {
            Cluster cluster = CacheFactory.getCluster();
            if (cluster != null)
                {
                Registry management = cluster.getManagement();
                boolean fCheck = check(management);
                if (!fCheck)
                    {
                    Logger.info("Bedrock: " + getClass().getSimpleName() + " check failed");
                    return false;
                    }
                if (m_sName != null && !m_sName.isBlank())
                    {
                    Optional<HealthCheck> optional = management.getHealthCheck(m_sName);
                    if (optional.isPresent())
                        {
                        fCheck = check(optional.get());
                        if (!fCheck)
                            {
                            Logger.info("Bedrock: " + getClass().getSimpleName() + " check failed for HealthCheck " + m_sName);
                            return false;
                            }
                        }
                    else
                        {
                        Logger.info("Bedrock: " + getClass().getSimpleName() + " check failed, HealthCheck " + m_sName + " is not registered");
                        return false;
                        }
                    }
                if (m_sName == null || m_sName.isBlank())
                    {
                    Logger.finest("Bedrock: " + getClass().getSimpleName() + "  check passed");
                    }
                else
                    {
                    Logger.finest("Bedrock: " + getClass().getSimpleName() + "  check passed for " + m_sName);
                    }
                return true;
                }
            else
                {
                Logger.info("Bedrock: "  + getClass().getSimpleName() + " check - cluster is null");
                return false;
                }
            }
        catch (Exception e)
            {
            Logger.err("Bedrock: " + getClass().getSimpleName() + " check failed", e);
            return false;
            }
        }

    /**
     * Perform the member health check.
     *
     * @param registry  the management registry
     *
     * @return {@code true} if the check passed
     */
    protected abstract boolean check(Registry registry);

    /**
     * Perform a specific health check.
     *
     * @param healthCheck  the {@link HealthCheck} to test
     *
     * @return {@code true} if the check passed
     */
    protected abstract boolean check(HealthCheck healthCheck);

    /**
     * The name of a specific health check to verify
     */
    private final String m_sName;
    }
