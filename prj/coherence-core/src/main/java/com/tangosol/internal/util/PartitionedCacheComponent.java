/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

/**
 * PartitionedCacheComponent is an internal interface to expose internal
 * methods in the PartitionedCache TDE component.
 *
 * @author mf 2017.06.29
 * @since Coherence 14.1.1
 */
public interface PartitionedCacheComponent
    extends PartitionedServiceComponent
    {
    /**
     * Return the id for the specified cache.
     *
     * @param sName  the cache name
     *
     * @return the cache id
     */
    public long getCacheId(String sName);
    }
