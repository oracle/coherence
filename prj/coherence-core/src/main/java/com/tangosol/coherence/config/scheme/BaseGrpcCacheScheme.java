/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
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
public class BaseGrpcCacheScheme
        extends AbstractCachingScheme<DefaultRemoteGrpcCacheServiceDependencies>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link BaseGrpcCacheScheme}.
     */
    public BaseGrpcCacheScheme()
        {
        m_serviceDependencies = new DefaultRemoteGrpcCacheServiceDependencies();
        }

    // ----- BaseGrpcCacheScheme interface  ---------------------------------------

    /**
     * Set the scope name to use to access resources on the remote cluster.
     *
     * @param sName the scope name to use to access resources on the remote cluster
     */
    @Injectable
    public void setRemoteScopeName(String sName)
        {
        if (GrpcDependencies.DEFAULT_SCOPE_ALIAS.equals(sName))
            {
            sName = GrpcDependencies.DEFAULT_SCOPE;
            }
        super.setScopeName(sName);
        }

    // ----- ServiceScheme interface  ---------------------------------------

    @Override
    public String getServiceType()
        {
        return CacheService.TYPE_REMOTE_GRPC;
        }

    // ----- ServiceBuilder interface ---------------------------------------

    @Override
    public boolean isRunningClusterNeeded()
        {
        return false;
        }

    @Override
    protected Service ensureService(String sService, Cluster cluster)
        {
        ClusterDependencies.ServiceProvider provider = getServiceProvider();
        if (provider == null)
            {
            // an exception will be thrown if the Coherence gRPC client is not on the class path,
            // as the client overrides this method.
            throw new UnsupportedOperationException("The Coherence gRPC client is not available");
            }
        cluster.getDependencies().addLocalServiceProvider(CacheService.TYPE_REMOTE_GRPC, provider);
        return super.ensureService(sService, cluster);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Returns the {@link ClusterDependencies.ServiceProvider} instance
     * to use to create new instances of the service.
     *
     * @return the {@link ClusterDependencies.ServiceProvider} instance
     *         to use to create new instances of the service
     */
    protected ClusterDependencies.ServiceProvider getServiceProvider()
        {
        return ClusterDependencies.ServiceProvider.NULL_IMPLEMENTATION;
        }
    }
