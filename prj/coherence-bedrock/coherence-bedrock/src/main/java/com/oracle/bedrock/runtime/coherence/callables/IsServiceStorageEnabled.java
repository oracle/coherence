/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.util.Trilean;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.Service;

public class IsServiceStorageEnabled
        implements RemoteCallable<Trilean>
    {
    /**
     * The name of the service.
     */
    private String serviceName;


    /**
     * Constructs an {@link IsServiceStorageEnabled}
     *
     * @param serviceName the name of the service
     */
    public IsServiceStorageEnabled(String serviceName)
        {
        this.serviceName = serviceName;
        }


    @Override
    public Trilean call() throws Exception
        {
        com.tangosol.net.Cluster cluster = CacheFactory.getCluster();
        Service service = cluster == null ? null : cluster.getService(serviceName);

        return service == null
                || !(service instanceof DistributedCacheService) ? Trilean.UNKNOWN
                : Trilean.of(((DistributedCacheService) service).isLocalStorageEnabled());
        }
    }
