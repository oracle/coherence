/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.service.grid;

/**
 * The default implementation of {@link PagedQueueServiceDependencies}.
 */
public class DefaultPagedQueueServiceDependencies
        extends DefaultPartitionedCacheDependencies
        implements PagedQueueServiceDependencies
    {
    public DefaultPagedQueueServiceDependencies()
        {
        }

    public DefaultPagedQueueServiceDependencies(PartitionedCacheDependencies deps)
        {
        super(deps);
        }
    }
