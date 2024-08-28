/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;


import com.tangosol.net.NamedQueue;
import com.tangosol.net.QueueService;

/**
 * The {@link QueueScheme} class is responsible for building a fully
 * configured instance of a {@link NamedQueue}.
 */
@SuppressWarnings("rawtypes")
public interface NamedQueueScheme<Q extends NamedQueue>
        extends QueueScheme<Q, QueueService>
    {

    }
