/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue;

import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.net.NamedQueue;

public class WrapperNamedCacheQueue<K extends QueueKey, E>
        extends AbstractWrapperNamedCacheQueue<E, K, BaseNamedCacheQueue<K, E>>
        implements NamedQueue<E>
    {
    public WrapperNamedCacheQueue(String sName, BaseNamedCacheQueue<K, E> delegate)
        {
        super(sName, delegate);
        }
    }
