/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.oracle.coherence.common.net.InetAddresses;
import com.oracle.coherence.common.util.Options;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.config.injection.SimpleInjector;
import com.tangosol.internal.net.service.extend.remote.DefaultRemoteTopicServiceDependencies;
import com.tangosol.internal.net.service.extend.remote.RemoteTopicServiceDependencies;

import com.tangosol.internal.net.topic.DefaultTopicDependencies;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCollection;
import com.tangosol.net.TopicService;
import com.tangosol.net.Cluster;
import com.tangosol.net.Service;
import com.tangosol.net.ValueTypeAssertion;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.TopicDependencies;
import com.tangosol.util.ResourceResolver;
import com.tangosol.util.ResourceResolverHelper;

/**
 * The {@link RemoteTopicScheme} is responsible for building a remote topic.
 *
 * @author Jonathan Knight  2025.01.01
 */
public class RemoteTopicScheme
        extends AbstractCachingScheme<RemoteTopicServiceDependencies>
        implements NamedTopicScheme
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link RemoteTopicScheme}.
     */
    public RemoteTopicScheme()
        {
        m_serviceDependencies = new DefaultRemoteTopicServiceDependencies();
        }

    // ----- ServiceScheme interface  ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServiceType()
        {
        return TopicService.TYPE_REMOTE;
        }

    // ----- ServiceBuilder interface ---------------------------------------

    @Override
    public boolean isRunningClusterNeeded()
        {
        return false;
        }

    @Override
    public Service realizeService(ParameterResolver resolver, ClassLoader loader, Cluster cluster)
        {
        Service service = super.realizeService(resolver, loader, cluster);

        injectScopeNameIntoService(service);

        return service;
        }

    @Override
    public RemoteTopicScheme getNamedCollectionBuilder(Class<? extends NamedCollection> clz, Options<NamedCollection.Option> options)
        {
        if (clz.isAssignableFrom(NamedTopic.class))
            {
            return this;
            }
        return null;
        }

    // ----- NamedTopicScheme methods ---------------------------------------

    @Override
    public TopicDependencies createConfiguration(ParameterResolver resolver, ClassLoader loader)
        {
        SimpleInjector   injector         = new SimpleInjector();
        ResourceResolver resourceResolver = ResourceResolverHelper.resourceResolverFrom(RemoteTopicScheme.class, this);

        injector.inject(this, ResourceResolverHelper.resourceResolverFrom(resourceResolver, resourceResolver));

        Cluster cluster = CacheFactory.getCluster();
        int     nMTU    = InetAddresses.getLocalMTU(cluster.getLocalMember().getAddress());

        if (nMTU == 0)
            {
            nMTU = 1500;
            }

        int nMaxBatchSizeBytes;
        try
            {
            // Detect Overflow.InetAddresses.getLocalMTU(NetworkInterface) returns Integer.MAX_VALUE on Windows/jdk11 for loopback.
            nMaxBatchSizeBytes = Math.multiplyExact(nMTU, cluster.getDependencies().getPublisherCloggedCount());
            }
        catch (ArithmeticException e)
            {
            nMaxBatchSizeBytes = Integer.MAX_VALUE;
            }

        DefaultTopicDependencies deps = new DefaultTopicDependencies();
        deps.setMaxBatchSizeBytes(nMaxBatchSizeBytes);

        return deps;
        }

    @Override
    public TopicService ensureConfiguredService(ParameterResolver resolver, Dependencies deps)
        {
        return getOrEnsureService(deps);
        }

    @SuppressWarnings("rawtypes")
    @Override
    public <V> NamedTopic realize(ValueTypeAssertion<V> typeConstraint,
                                      ParameterResolver resolver, Dependencies deps)
        {
        validate(resolver);

        String           sName   = deps.getCacheName();
        TopicService     service = ensureConfiguredService(resolver, deps);

        return service.ensureTopic(sName, deps.getClassLoader());
        }

    @Override
    public <T extends NamedCollection> boolean realizes(Class<T> type)
        {
        return NamedTopic.class.equals(type);
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
        Service service =
            ((ExtensibleConfigurableCacheFactory) deps.getConfigurableCacheFactory()).ensureService(this);

        if (!(service instanceof TopicService))
            {
            throw new IllegalArgumentException("Error: ensureTopic is using service "
                                               + service.getInfo().getServiceName() + "that is not a TopicService ");
            }

        return (TopicService) service;
        }
    }
