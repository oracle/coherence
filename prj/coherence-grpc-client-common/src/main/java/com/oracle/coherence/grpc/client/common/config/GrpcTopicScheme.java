/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.client.common.config;


import com.oracle.coherence.common.util.Options;

import com.oracle.coherence.grpc.client.common.topics.GrpcRemoteTopicService;
import com.tangosol.coherence.config.scheme.BaseGrpcTopicScheme;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.Cluster;
import com.tangosol.net.ClusterDependencies;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCollection;
import com.tangosol.net.Service;
import com.tangosol.net.TopicService;

import com.tangosol.net.topic.NamedTopic;

/**
 * A scheme for configuring remote topics.
 *
 * @author Jonathan Knight  2025.01.25
 */
public class GrpcTopicScheme
        extends BaseGrpcTopicScheme
        implements ClusterDependencies.ServiceProvider<TopicService>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link GrpcTopicScheme}.
     */
    public GrpcTopicScheme()
        {
        }

    // ----- ServiceBuilder interface ---------------------------------------

    @Override
    protected ClusterDependencies.ServiceProvider<TopicService> getServiceProvider()
        {
        return this;
        }

    // ----- ClusterDependencies.ServiceProvider methods --------------------

    @Override
    public TopicService createService(String sName, Cluster cluster)
        {
        GrpcRemoteTopicService service = new GrpcRemoteTopicService();
        service.setServiceName(sName);
        service.setCluster(cluster);
        return service;
        }

    @Override
    public TopicService ensureConfiguredService(ParameterResolver resolver, Dependencies deps)
        {
        return getOrEnsureService(deps);
//        ClusterDependencies.ServiceProvider<TopicService> provider = getServiceProvider();
//        return provider.ensureConfiguredService(resolver, deps);
        }

    @Override
    public GrpcTopicScheme getNamedCollectionBuilder(Class<? extends NamedCollection> clz, Options<NamedCollection.Option> options)
        {
        if (clz.isAssignableFrom(NamedTopic.class))
            {
            return this;
            }
        return null;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Get or ensure service corresponding to this scheme.
     * <p>
     * Optimized to avoid ensureService synchronization on cluster and service
     * when possible. This behavior is required on server side. Intermittent deadlock occurs
     * calling ensureService on server side from inside service implementation.
     *
     * @return {@link TopicService}
     */
    private TopicService getOrEnsureService(Dependencies deps)
        {
        // Call ECFF to ensure the Service. CCF must be used to ensure the service, rather
        // than the service builder.  This is because ECCF.ensureService provides additional
        // logic like injecting a BackingMapManager into the service and starting the Service.
        ExtensibleConfigurableCacheFactory eccf    = (ExtensibleConfigurableCacheFactory) deps.getConfigurableCacheFactory();
        Service                            service = eccf.ensureService(this);

        if (!(service instanceof TopicService))
            {
            throw new IllegalArgumentException("Error: ensureTopic is using service "
                    + service.getInfo().getServiceName() + ", which is a " + service.getClass() + " not a TopicService");
            }
        return (TopicService) service;
        }
    }