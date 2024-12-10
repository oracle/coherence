/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue;

import com.tangosol.net.CacheService;
import com.tangosol.net.QueueService;
import com.tangosol.net.WrapperCacheService;

/**
 * A {@link com.tangosol.net.QueueService} that wraps a {}
 */
public class CacheQueueService
        extends WrapperCacheService
        implements QueueService
    {
    public CacheQueueService(CacheService service)
        {
        super(service);
        }
    }
