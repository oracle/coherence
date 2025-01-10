/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.proxy;

/**
 * The {@link TopicServiceProxyDependencies} interface provides a TopicServiceProxy
 * with its external dependencies.
 *
 * @author Jonathan Knight  2025.01.01
 */
public interface TopicServiceProxyDependencies
        extends ServiceProxyDependencies
    {
    /**
     * Return the approximate maximum number of bytes transferred by a partial response.
     * Results that can be streamed are returned to the requester as a sequence of response
     * messages containing a portion of the total result.
     * Each of these response messages will be approximately no larger than the configured size.
     *
     * @return the transfer threshold
     */
    long getTransferThreshold();
    }
