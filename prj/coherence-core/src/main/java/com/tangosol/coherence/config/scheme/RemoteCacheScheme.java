/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.internal.net.service.extend.remote.DefaultRemoteCacheServiceDependencies;
import com.tangosol.internal.net.service.extend.remote.LegacyXmlRemoteCacheServiceHelper;
import com.tangosol.internal.net.service.extend.remote.RemoteCacheServiceDependencies;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.NamedCache;
import com.tangosol.net.OperationalContext;
import com.tangosol.net.Service;

import com.tangosol.net.cache.BundlingNamedCache;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;

/**
 * The {@link RemoteCacheScheme} is responsible for building a remote cache.
 *
 * @author pfm  2011.10.04
 * @since Coherence 12.1.2
 */
public class RemoteCacheScheme
        extends AbstractCachingScheme<RemoteCacheServiceDependencies>
        implements BundlingScheme
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link RemoteCacheScheme}.
     */
    public RemoteCacheScheme()
        {
        m_serviceDependencies = new DefaultRemoteCacheServiceDependencies();
        m_mgrBundle           = null;
        }

    // ----- ServiceScheme interface  ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServiceType()
        {
        return CacheService.TYPE_REMOTE;
        }

    // ----- ServiceBuilder interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRunningClusterNeeded()
        {
        return false;
        }

    /**
     * {@inheritDoc}
     */
    public Service realizeService(ParameterResolver resolver, ClassLoader loader, Cluster cluster)
        {
        Service service = super.realizeService(resolver, loader, cluster);

        injectScopeNameIntoService(service);

        return service;
        }

    // ----- BundlingScheme methods  -----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public BundleManager getBundleManager()
        {
        return m_mgrBundle;
        }

    // ----- RemoteCacheScheme methods  --------------------------------------

    /**
     * Set the {@link BundleManager}.
     *
     * @param mgrBundle  the BundleManager
     */
    @Injectable("operation-bundling")
    public void setBundleManager(BundleManager mgrBundle)
        {
        m_mgrBundle = mgrBundle;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link BundleManager}.
     */
    private BundleManager m_mgrBundle;
    }
