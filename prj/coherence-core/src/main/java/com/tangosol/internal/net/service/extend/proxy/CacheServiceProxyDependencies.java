/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.proxy;

/**
 * The CacheServiceProxyDependencies interface provides a CacheServiceProxy with its external
 * dependencies.
 *
 * @author pfm  2011.07.25
 * @since Coherence 12.1.2
 */
public interface CacheServiceProxyDependencies
        extends ServiceProxyDependencies
    {
    /**
     * Return true if NamedCache lock and unlock operations are allowed.
     *
     * @return true if NamedCache lock and unlock operations are allowed
     */
    public boolean isLockEnabled();

    /**
     * Return true if the ProxyCacheService is read only.
     *
     * @return true if the ProxyCacheService is read only
     */
    public boolean isReadOnly();

    /**
     * Return the approximate maximum number of bytes transfered by a partial response.
     * Results that can be streamed, such as query requests, are returned to the requester
     * as a sequence of response messages containing a portion of the total result.
     * Each of these response messages will be approximately no larger than the configured size.
     *
     * @return the transfer threshold
     */
    public long getTransferThreshold();
    }
