/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.net.CacheService;

/**
 * The {@link OptimisticScheme} class builds an optimistic cache.
 *
 * @author pfm  2011.12.06
 * @since Coherence 12.1.2
 */
public class OptimisticScheme
        extends ReplicatedScheme
    {
    // ----- ServiceScheme interface  ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServiceType()
        {
        return CacheService.TYPE_OPTIMISTIC;
        }
    }
