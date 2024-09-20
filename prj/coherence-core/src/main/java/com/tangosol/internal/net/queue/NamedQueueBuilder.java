/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue;

import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedCollection;
import com.tangosol.net.NamedQueue;

public interface NamedQueueBuilder
        extends NamedCollection.Option
    {
    /**
     * Return the cache name to use for a given collection name.
     *
     * @param sName  the name of the collection
     *
     * @return the cache name to use for the given collection name
     */
    String getCacheName(String sName);

    /**
     * Return the collection name from a given cache name.
     *
     * @param sCacheName  the name of the cache
     *
     * @return the collection name from the given cache name
     */
    String getCollectionName(String sCacheName);

    /**
     * Returns {@code true} if this builder can build the specified class.
     *
     * @param clz  the class to build
     *
     * @return  {@code true} if this builder can build the specified class
     */
    @SuppressWarnings("rawtypes")
    boolean realizes(Class<? extends NamedQueue> clz);

    <E> NamedQueue<E> build(String sName, NamedCache<QueueKey, E> cache);
    }
