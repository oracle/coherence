/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.internal.net.grpc.DefaultRemoteGrpcCacheServiceDependencies;
import com.tangosol.internal.net.grpc.DefaultRemoteGrpcServiceDependencies;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.ClusterDependencies;
import com.tangosol.net.Service;
import com.tangosol.net.grpc.GrpcDependencies;

/**
 * The {@link BaseGrpcScheme} is responsible for building a
 * remote gRPC service.
 * <p>
 * This class is sub-classed in the Coherence Java gRPC client module
 * and that subclass does all the actual work. This allows the grpc
 * remote scheme to be added to a cache configuration file even if the
 * gRPC client is not on the class path and nothing will break.
 *
 * @author Jonathan Knight  2023.02.02
 * @since 23.03
 */
public abstract class BaseGrpcScheme<T extends DefaultRemoteGrpcServiceDependencies, S extends Service>
        extends AbstractCachingScheme<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link BaseGrpcScheme}.
     */
    protected BaseGrpcScheme(T deps)
        {
        m_serviceDependencies = deps;
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

    // ----- ServiceBuilder interface ---------------------------------------

    @Override
    public boolean isRunningClusterNeeded()
        {
        return false;
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
