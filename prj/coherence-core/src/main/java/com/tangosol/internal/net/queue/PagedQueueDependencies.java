/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue;

/**
 * The dependencies for a distributed queue.
 */
public interface PagedQueueDependencies
        extends NamedQueueDependencies
    {
    /**
     * Obtain the page capacity in bytes.
     *
     * @return the capacity
     */
    int getPageCapacity();
    }
