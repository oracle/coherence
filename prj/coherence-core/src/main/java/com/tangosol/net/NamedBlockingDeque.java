/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

import java.util.concurrent.BlockingDeque;

/**
 * A {@link BlockingDeque} based data-structure that manages values across one or
 * more processes. Values are typically managed in memory.
 *
 * @param <E> the type of values in the queue
 */
public interface NamedBlockingDeque<E>
        extends NamedBlockingQueue<E>, NamedDeque<E>, BlockingDeque<E>
    {
    }
