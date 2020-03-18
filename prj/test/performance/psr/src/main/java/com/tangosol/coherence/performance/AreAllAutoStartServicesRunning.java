/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.coherence.config.scheme.ServiceScheme;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.Service;


/**
 * A {@link RemoteCallable} to be used with the Oracle Tools
 * test framework that can be submitted to a cluster
 * member to determine whether all of the autostart services
 * defined in that members cache configuration are running.
 *
 * @author bo 2014.11.14
 */
public class AreAllAutoStartServicesRunning
        implements RemoteCallable<Boolean>
    {
    @Override
    public Boolean call() throws Exception
        {
        Cluster cluster     = CacheFactory.getCluster();
        ExtensibleConfigurableCacheFactory eccf
            = (ExtensibleConfigurableCacheFactory) CacheFactory.getConfigurableCacheFactory();

        for (ServiceScheme serviceScheme : eccf.getCacheConfig().getServiceSchemeRegistry())
            {
            if (serviceScheme.isAutoStart())
                {
                String sServiceName = serviceScheme.getServiceName();
                Service service     = cluster.getService(sServiceName);

                if (CacheService.TYPE_LOCAL.equals(sServiceName))
                    {
                    continue;
                    }

                if (service == null || !service.isRunning())
                    {
                    System.out.println("Service " + sServiceName + " not started");
                    return false;
                    }
                }
            }

        return true;
        }
    }