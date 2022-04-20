/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.coherence.config.scheme.ServiceScheme;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.Service;

import java.util.Iterator;

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

        Iterator<ServiceScheme> iterator = eccf.getCacheConfig().getServiceSchemeRegistry().iterator();

        while (iterator.hasNext())
            {
            ServiceScheme serviceScheme = iterator.next();

            if (serviceScheme.isAutoStart())
                {
                Service service = cluster.getService(serviceScheme.getServiceName());
                if (service == null || !service.isRunning())
                    {
                    return false;
                    }
                }
            }

        return true;
        }
    }