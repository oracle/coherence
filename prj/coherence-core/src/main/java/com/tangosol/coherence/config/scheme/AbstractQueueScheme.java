/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.config.scheme;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.net.queue.model.QueueKey;

import com.tangosol.internal.net.queue.processor.MaybePagedQueueProcessor;

import com.tangosol.internal.net.service.grid.PartitionedCacheDependencies;

import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedQueue;

import java.util.Optional;

/**
 * A base class for {@link NamedQueueScheme} classes.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AbstractQueueScheme<Q extends NamedQueue>
        extends DistributedScheme
        implements NamedQueueScheme<Q>
    {
    protected AbstractQueueScheme(PartitionedCacheDependencies deps)
        {
        super(deps);
        }

    /**
     * Assert that a given cache could be the element cache for a simple queue.
     *
     * @param sQueueName  the name of the queue
     * @param cache       the underlying queue cache
     */
    protected void assertMaybeSimpledQueue(String sQueueName, NamedCache cache)
        {
        Optional<Boolean> isPaged = isPagedQueue(sQueueName, cache);
        if (isPaged.isPresent() && isPaged.get())
            {
            throw new IllegalStateException("Ensure queue is being called for a previously ensured queue of a different type. Queue name \""
                    + sQueueName + "\" requested type \"Simple Queue\" actual type \"Paged Queue\"");
            }
        }

    /**
     * Assert that a given cache could be the element cache for a paged queue.
     *
     * @param sQueueName  the name of the queue
     * @param cache       the underlying queue cache
     */
    protected void assertMaybePagedQueue(String sQueueName, NamedCache cache)
        {
        Optional<Boolean> isPaged = isPagedQueue(sQueueName, cache);
        if (isPaged.isPresent() && !isPaged.get())
            {
            throw new IllegalStateException("Ensure queue is being called for a previously ensured queue of a different type. Queue name \""
                    + sQueueName + "\" requested type \"Paged Queue\" actual type \"Simple Queue\"");
            }
        }

    private Optional<Boolean> isPagedQueue(String sQueueName, NamedCache cache)
        {
        Boolean isPaged = null;
        try
            {
            isPaged = (Boolean) cache.invoke(QueueKey.head(sQueueName), MaybePagedQueueProcessor.INSTANCE);
            }
        catch (Throwable t)
            {
            // We ignore an exception as it may just be a backwards compatibility issue
            Logger.err("Failed to assert requested queue can be the required type due to " + t.getMessage());
            }
        return Optional.ofNullable(isPaged);
        }
    }
