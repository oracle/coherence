/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue;

import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.net.NamedMap;
import com.tangosol.net.NamedQueue;

/**
 * A {@link NamedQueue} implementation that wraps a {@link NamedMapQueue}.
 *
 * @param <K>  the type of the underlying cache key
 * @param <E>  the type of the elements stored in the queue
 */
public class WrapperNamedMapQueue<K extends QueueKey, E>
        extends AbstractWrapperNamedMapQueue<K, E, NamedMapQueue<K, E>>
        implements NamedQueue<E>
    {
    /**
     * Create a {@link WrapperNamedMapQueue}.
     *
     * @param delegate  the {@link NamedMapQueue} to delegate to
     */
    public WrapperNamedMapQueue(NamedMapQueue<K, E> delegate)
        {
        super(delegate);
        }

    /**
     * Create a {@link WrapperNamedMapQueue}.
     *
     * @param sName     the name of this queue
     * @param delegate  the {@link NamedMapQueue} to delegate to
     */
    public WrapperNamedMapQueue(String sName, NamedMapQueue<K, E> delegate)
        {
        super(sName, delegate);
        }

    /**
     * Obtain the underlying {@link NamedMap}.
     *
     * @return the underlying {@link NamedMap}
     */
    public NamedMap<K, E> getNamedMap()
        {
        return f_delegate.getNamedMap();
        }
    }
