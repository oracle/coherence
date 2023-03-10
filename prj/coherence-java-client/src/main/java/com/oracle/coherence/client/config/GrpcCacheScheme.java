/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client.config;

import com.oracle.coherence.client.GrpcRemoteCacheService;

import com.tangosol.coherence.config.scheme.BaseGrpcCacheScheme;

import com.tangosol.net.Cluster;
import com.tangosol.net.ClusterDependencies;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The {@link GrpcCacheScheme} is responsible for building a remote gRPC cache.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public class GrpcCacheScheme
        extends BaseGrpcCacheScheme
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
        return this::createService;
        }

    // ----- helper methods -------------------------------------------------

    private GrpcRemoteCacheService createService(String sName, Cluster cluster)
        {
        GrpcRemoteCacheService service = new GrpcRemoteCacheService();
        service.setServiceName(sName);
        service.setCluster(cluster);
        return service;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The service lock.
     */
    private final Lock f_lock = new ReentrantLock(true);

    /**
     * The service for this scheme.
     */
    private volatile GrpcRemoteCacheService m_service;
    }
