/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.net.topic.TopicDependencies;

/**
 * The dependencies for a paged topic.
 *
 * @author Jonathan Knight 2002.09.10
 * @since 23.03
 */
public interface PagedTopicDependencies
        extends TopicDependencies
    {
    /**
     * Obtain the page capacity in bytes.
     *
     * @return the capacity
     */
    int getPageCapacity();

    /**
     * Get maximum capacity for a server.
     *
     * @return return the capacity or zero if unlimited.
     */
    long getServerCapacity();
    }
