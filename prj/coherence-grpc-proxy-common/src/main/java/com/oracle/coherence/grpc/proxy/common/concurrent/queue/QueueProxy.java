/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common.concurrent.queue;

import com.tangosol.net.NamedQueue;
import com.tangosol.util.Binary;

public interface QueueProxy
    {
    NamedQueue<Binary> getQueue();

    /**
     * Return {@code true} if this queue is serialization compatible.
     *
     * @return {@code true} if this queue is serialization compatible
     */
    boolean isCompatible();
    }
