/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.client.common.config;

import com.oracle.coherence.grpc.client.common.GrpcRemoteCacheService;
import com.tangosol.coherence.config.scheme.BaseGrpcCacheScheme;

import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.ClusterDependencies;

/**
 * The {@link GrpcCacheScheme} is responsible for building a remote gRPC cache.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public class GrpcCacheScheme
        extends BaseGrpcCacheScheme<CacheService>
        implements ClusterDependencies.ServiceProvider
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link GrpcCacheScheme}.
     */
    public GrpcCacheScheme()
        {
        }

    // ----- ServiceBuilder interface ---------------------------------------

    @Override
    protected ClusterDependencies.ServiceProvider getServiceProvider()
        {
        return this;
        }

    // ----- ClusterDependencies.ServiceProvider methods --------------------

    @Override
    public GrpcRemoteCacheService createService(String sName, Cluster cluster)
        {
        GrpcRemoteCacheService service = new GrpcRemoteCacheService();
        service.setServiceName(sName);
        service.setCluster(cluster);
        return service;
        }
    }
