/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue;

import com.tangosol.net.NamedQueue;

public interface PagedQueue<E>
        extends NamedQueue<E>
    {
    /**
     * The default capacity of pages when using the default binary calculator (10MB).
     */
    int DEFAULT_PAGE_CAPACITY_BYTES = 1024 * 1024 * 10;

    /**
     * The maximum bucket identifier.
     */
    int DEFAULT_MAX_BUCKET_ID = Integer.MAX_VALUE;
    }
