/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.internal.net.grpc.DefaultRemoteGrpcCacheServiceDependencies;

import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.ClusterDependencies;
import com.tangosol.net.Service;

import com.tangosol.net.grpc.GrpcDependencies;

/**
 * The {@link BaseGrpcCacheScheme} is responsible for building a
 * remote gRPC cache service.
 * <p>
 * This class is sub-classed in the Coherence Java gRPC client module
 * and that subclass does all the actual work. This allows the grpc
 * remote scheme to be added to a cache configuration file even if the
 * gRPC client is not on the class path and nothing will break.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public class BaseGrpcCacheScheme<S extends Service>
        extends BaseGrpcScheme<DefaultRemoteGrpcCacheServiceDependencies, S>
    {
    /**
     * Constructs a {@link BaseGrpcCacheScheme}.
     */
    public BaseGrpcCacheScheme()
        {
        super(new DefaultRemoteGrpcCacheServiceDependencies());
        }

    // ----- ServiceScheme interface  ---------------------------------------

    @Override
    public String getServiceType()
        {
        return CacheService.TYPE_REMOTE_GRPC;
        }

    // ----- ServiceBuilder interface ---------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    protected S ensureService(String sService, Cluster cluster)
        {
        ClusterDependencies.ServiceProvider provider = getServiceProvider();
        if (provider == null)
            {
            // an exception will be thrown if the Coherence gRPC client is not on the class path,
            // as the client overrides this method.
            throw new UnsupportedOperationException("The Coherence gRPC client is not available");
            }
        cluster.getDependencies().addLocalServiceProvider(CacheService.TYPE_REMOTE_GRPC, provider);
        return (S) super.ensureService(sService, cluster);
        }
    }
