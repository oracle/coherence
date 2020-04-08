/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.internal.net.service.grid.DefaultPartitionedCacheDependencies;
import com.tangosol.internal.net.service.grid.DefaultReplicatedCacheDependencies;
import com.tangosol.internal.net.service.grid.LegacyXmlReplicatedCacheHelper;

import com.tangosol.internal.net.service.grid.ReplicatedCacheDependencies;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;

/**
 * The {@link ReplicatedScheme} class builds replicated cache.
 *
 * @author pfm  2011.12.06
 * @since Coherence 12.1.2
 */
public class ReplicatedScheme
        extends AbstractCachingScheme<ReplicatedCacheDependencies>
        implements ClusteredCachingScheme
    {
    // ----- constructors  --------------------------------------------------

    /**
     * Constructs a {@link ReplicatedScheme}.
     */
    public ReplicatedScheme()
        {
        m_serviceDependencies = new DefaultReplicatedCacheDependencies();
        }

    // ----- ServiceScheme interface  ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServiceType()
        {
        return CacheService.TYPE_REPLICATED;
        }

    // ----- ServiceBuilder interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRunningClusterNeeded()
        {
        return true;
        }

    // ----- ClusteredCachingScheme interface -------------------------------

    /**
     * Return the {@link BackingMapScheme} which builds the backing map for
     * the clustered scheme.
     *
     * @return  the scheme
     */
    public BackingMapScheme getBackingMapScheme()
        {
        return m_schemeBackingMap;
        }

    /**
     * Set the {@link BackingMapScheme} which builds the backing map for
     * the clustered scheme.
     *
     * @param scheme  the scheme builder
     */
    @Injectable
    public void setBackingMapScheme(BackingMapScheme scheme)
        {
        m_schemeBackingMap = scheme;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The backing map scheme.
     */
    private BackingMapScheme m_schemeBackingMap;
    }
