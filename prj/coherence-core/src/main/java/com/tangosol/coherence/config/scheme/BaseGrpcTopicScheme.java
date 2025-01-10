/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.oracle.coherence.common.util.Options;

import com.tangosol.coherence.config.builder.NamedCollectionBuilder;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.internal.net.grpc.DefaultRemoteGrpcTopicServiceDependencies;

import com.tangosol.internal.net.topic.DefaultTopicDependencies;

import com.tangosol.net.Cluster;
import com.tangosol.net.ClusterDependencies;
import com.tangosol.net.NamedCollection;
import com.tangosol.net.Service;
import com.tangosol.net.TopicService;
import com.tangosol.net.ValueTypeAssertion;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.TopicDependencies;

/**
 * The {@link BaseGrpcTopicScheme} is responsible for building a
 * remote gRPC topic service.
 * <p>
 * This class is sub-classed in the Coherence Java gRPC client module
 * and that subclass does all the actual work. This allows the grpc
 * remote scheme to be added to a cache configuration file even if the
 * gRPC client is not on the class path and nothing will break.
 *
 * @author Jonathan Knight  2025.01.01
 */
public class BaseGrpcTopicScheme
        extends BaseGrpcScheme<DefaultRemoteGrpcTopicServiceDependencies, TopicService>
        implements NamedTopicScheme
    {
    /**
     * Constructs a {@link BaseGrpcTopicScheme}.
     */
    public BaseGrpcTopicScheme()
        {
        super(new DefaultRemoteGrpcTopicServiceDependencies());
        }

    @Override
    @SuppressWarnings("rawtypes")
    public <E> NamedTopic realize(ValueTypeAssertion<E> assertion, ParameterResolver resolver, Dependencies deps)
        {
        validate(resolver);

        String       sName   = deps.getCacheName();
        TopicService service = ensureConfiguredService(resolver, deps);

        return service.ensureTopic(sName, deps.getClassLoader());
        }

    @Override
    public <T extends NamedCollection> boolean realizes(Class<T> type)
        {
        return NamedTopic.class.equals(type);
        }

    @Override
    public TopicDependencies createConfiguration(ParameterResolver resolver, ClassLoader loader)
        {
        return new DefaultTopicDependencies();
        }

    @Override
    @SuppressWarnings("rawtypes")
    public NamedCollectionBuilder getNamedCollectionBuilder(Class<? extends NamedCollection> clz, Options<NamedCollection.Option> options)
        {
        // an exception will be thrown if the Coherence gRPC client is not on the class path,
        // as the client overrides this method.
        throw new UnsupportedOperationException("The Coherence gRPC client is not available");
        }

    // ----- ServiceScheme interface  ---------------------------------------

    @Override
    public String getServiceType()
        {
        return TopicService.TYPE_REMOTE_GRPC;
        }

    @Override
    @SuppressWarnings("unchecked")
    protected Service ensureService(String sService, Cluster cluster)
        {
        ClusterDependencies.ServiceProvider<TopicService> provider = getServiceProvider();
        if (provider == null)
            {
            // an exception will be thrown if the Coherence gRPC client is not on the class path,
            // as the client overrides this method.
            throw new UnsupportedOperationException("The Coherence gRPC client is not available");
            }
        cluster.getDependencies().addLocalServiceProvider(TopicService.TYPE_REMOTE_GRPC, provider);
        return super.ensureService(sService, cluster);
        }


    // ----- ServiceBuilder interface ---------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public TopicService ensureConfiguredService(ParameterResolver resolver, Dependencies deps)
        {
        ClusterDependencies.ServiceProvider<TopicService> provider = getServiceProvider();
        return provider.ensureConfiguredService(resolver, deps);
        }
    }
