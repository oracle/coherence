/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Service;

/**
 * A Bedrock {@link RemoteCallable} to determine whether
 * a specific service is running.
 */
public class IsServiceRunning
        implements RemoteCallable<Boolean>
    {
    /**
     * Constructs an {@link IsServiceRunning}
     *
     * @param sServiceName the name of the service
     */
    public IsServiceRunning(String sServiceName)
        {
        m_sServiceName = sServiceName;
        }

    @Override
    public Boolean call()
        {
        try
            {
            com.tangosol.net.Cluster cluster = CacheFactory.getCluster();
            if (cluster != null && cluster.isRunning())
                {
                if ("Cluster".equals(m_sServiceName))
                    {
                    return true;
                    }
                Service service = cluster.getService(m_sServiceName);
                return service != null && service.isRunning();
                }
            }
        catch (Throwable t)
            {
            t.printStackTrace();
            }
        return false;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of the service.
     */
    private final String m_sServiceName;
    }
