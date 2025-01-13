/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;

import java.util.SortedSet;

/**
 * A representation of a subscription (subscriber group) in a topic.
 *
 * @author Jonathan Knight  2024.11.26
 */
public interface TopicSubscription
    {
    /**
     * Return the timestamp of a subscription creation.
     *
     * @param id  the subscriber identifier
     *
     * @return the timestamp of the subscription creation
     */
    long getSubscriberTimestamp(SubscriberId id);

    /**
     * Return the channels owned by a subscriber.
     *
     * @param id  the subscriber identifier
     *
     * @return the channels owned by a subscriber
     */
    SortedSet<Integer> getOwnedChannels(SubscriberId id);
    }
