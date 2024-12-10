/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;

/**
 * A default implementation of {@link PagedTopicServiceDependencies}.
 *
 * @author Jonathan Knight 2022.09.10
 * @since 23.03
 */
public class DefaultPagedTopicServiceDependencies
        extends DefaultPartitionedCacheDependencies
        implements PagedTopicServiceDependencies
    {
    /**
     * Create a {@link DefaultPagedTopicServiceDependencies}.
     */
    public DefaultPagedTopicServiceDependencies()
        {
        }

    /**
     * Create a {@link DefaultPagedTopicServiceDependencies}.
     *
     * @param deps the {@link PagedTopicServiceDependencies} to use to initialize these dependencies
     */
    public DefaultPagedTopicServiceDependencies(PagedTopicServiceDependencies deps)
        {
        super(deps);
        }
    }
