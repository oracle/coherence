/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue;

import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedDeque;
import com.tangosol.net.NamedQueue;

/**
 * A {@link NamedQueue} implementation that wraps a {@link NamedCache}.
 *
 * @param <E> the type of elements held in this queue
 */
public interface NamedMapDeque<K, E>
        extends NamedMapQueue<K, E>, NamedDeque<E>
    {
    }
