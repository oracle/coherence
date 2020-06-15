/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.remote;

/**
 * The RemoteCacheServiceDependencies interface provides a RemoteCacheService with its external
 * dependencies.
 *
 * @author pfm  2011.09.05
 * @since Coherence 12.1.2
 */
public interface RemoteCacheServiceDependencies
        extends RemoteServiceDependencies
    {
    /**
     * Determine whether keys should be checked for KeyAssociation by the extend client (false)
     * or deferred until the key is received by the PartitionedService (true).
     *
     * @return false if keys should be check for KeyAssociation by the extend client,
     * true otherwise
     */
    public boolean isDeferKeyAssociationCheck();
    }
