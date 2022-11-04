/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicBackingMapManager;

import com.tangosol.net.topic.TopicBackingMapManager;

/**
 * A {@link TopicService} which provides globally ordered topics.
 */
public interface PagedTopicService
        extends TopicService, DistributedCacheService
    {
    /**
     * Return the {@link TopicBackingMapManager} for this service.
     *
     * @return the {@link TopicBackingMapManager} for this service
     */
    PagedTopicBackingMapManager getTopicBackingMapManager();
    }
