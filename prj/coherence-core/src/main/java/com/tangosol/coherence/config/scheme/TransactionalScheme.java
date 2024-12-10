/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;

/**
 * The {@link TransactionalScheme} class builds a transactional cache.  The
 * transactional cache is a logical cache backed by a set of distributed caches.
 * A distributed service is used to handle the internal txn caches, since they are
 * all distributed caches.  Because the transactional cache is logical, it
 * implements the realizeNamedCache used by ECCF.ensureCache. There is no backing
 * map for transactional caches (because they are logical) so realizeMap is not
 * needed.  However, the internal distributed txn caches do have backing maps
 * which are handled by the normal DistributedScheme code.
 *
 * @author pfm  2011.12.06
 * @since Coherence 12.1.2
 */
public class TransactionalScheme
        extends AbstractCachingScheme
    {
    // ----- ServiceScheme interface  ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServiceType()
        {
        return CacheService.TYPE_DISTRIBUTED;
        }

    // ----- ServiceBuilder interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Service realizeService(ParameterResolver resolver, ClassLoader loader, Cluster cluster)
        {
        throw new UnsupportedOperationException("Transactions are not supported in Coherence CE");
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRunningClusterNeeded()
        {
        return true;
        }

    // ----- NamedCacheBuilder interface ------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public NamedCache realizeCache(ParameterResolver resolver, Dependencies dependencies)
        {
        throw new UnsupportedOperationException("Transactions are not supported in Coherence CE");
        }
    }
