/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.queue;

import com.tangosol.internal.net.service.grid.DefaultPartitionedCacheDependencies;

/**
 * A default implementation of {@link NamedQueueDependencies}.
 */
public class DefaultNamedQueueDependencies
        extends DefaultPartitionedCacheDependencies
        implements NamedQueueDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link DefaultNamedQueueDependencies}.
     */
    public DefaultNamedQueueDependencies()
        {
        }

    /**
     * A copy constructor to create a {@link DefaultNamedQueueDependencies}
     * from another {@link PagedQueueDependencies} instance.
     *
     * @param deps  the {@link PagedQueueDependencies} to copy
     */
    public DefaultNamedQueueDependencies(PagedQueueDependencies deps)
        {
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "NamedQueueScheme Configuration";
        }

    }
