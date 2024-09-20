/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common.concurrent.queue;

import com.tangosol.net.NamedQueue;
import com.tangosol.net.queue.WrapperNamedQueue;

public class NamedQueueProxy<E>
        extends WrapperNamedQueue<E>
    {
    /**
     * Create a {@link NamedQueueProxy}.
     *
     * @param queue        the wrapped {@link NamedQueue}
     * @param fCompatible  {@code true} if this queue is serialization compatible
     */
    public NamedQueueProxy(NamedQueue<E> queue, boolean fCompatible)
        {
        super(queue, queue.getName());
        f_fCompatible = fCompatible;
        }

    /**
     * Return {@code true} if this queue is serialization compatible.
     *
     * @return {@code true} if this queue is serialization compatible
     */
    public boolean isCompatible()
        {
        return f_fCompatible;
        }

    // ----- data members ---------------------------------------------------

    /**
     * {@code true} if this queue is serialization compatible.
     */
    private final boolean f_fCompatible;
    }
