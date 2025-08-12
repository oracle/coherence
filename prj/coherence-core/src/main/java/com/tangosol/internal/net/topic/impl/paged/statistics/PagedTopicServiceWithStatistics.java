/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic.impl.paged.statistics;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;

import com.tangosol.net.PagedTopicService;

/**
 * A {@link PagedTopicService} that can also supply statistics.
 *
 * @author Jonathan Knight 2022.08.11
 */
public interface PagedTopicServiceWithStatistics
        extends PagedTopicService
    {
    /**
     * Return the number messages remaining on this cluster member in the topic
     * after the last committed message.
     * <p>
     * This value is a count of messages remaining to be polled after the last committed message.
     * This value is transitive and could already have changed by another in-flight commit request
     * immediately after this value is returned.
     * <p>
     * Note, getting the remaining messages count is a cluster wide operation as the value is stored
     * by each member.
     *
     * @param sTopic             the name of the topic
     * @param subscriberGroupId  the subscriber group
     * @param anChannel          the channels to get the remaining message count from,
     *                           or specify no channels to return the count for all channels
     *
     * @return the number of unread messages
     */
    int getLocalRemainingMessages(String sTopic, SubscriberGroupId subscriberGroupId, int... anChannel);
    }
