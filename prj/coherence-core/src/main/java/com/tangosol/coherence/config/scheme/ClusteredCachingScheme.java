/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

/**
 * The ClusteredCachingScheme interface represents schemes that are used
 * for clustered caches.
 *
 * @author pfm  2012.02.27
 * @since Coherence 12.1.2
 */
public interface ClusteredCachingScheme
    {
    /**
     * Return the {@link BackingMapScheme} used to create a backing map.
     *
     * @return the {@link BackingMapScheme}
     */
    public BackingMapScheme getBackingMapScheme();
    }
