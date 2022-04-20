/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber.PagedTopicChannel;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.Subscription;

import com.tangosol.net.Member;

import com.tangosol.util.HashHelper;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author jk 2015.08.24
 */
@SuppressWarnings("unchecked")
public class PagedTopicSubscriberTest
    {
    @Test
    public void shouldNotCreateQueueSubscriberWithNullParentTopic()
        {
        PagedTopicCaches storage       = mock(PagedTopicCaches.class);
        assertThrows(NullPointerException.class, () ->  new PagedTopicSubscriber<>(null, storage));
        }

    @Test
    @SuppressWarnings({"rawtypes"})
    public void shouldNotCreateQueueSubscriberWithNullQueueCaches()
        {
        PagedTopic<?> topic = mock(PagedTopic.class);
        assertThrows(NullPointerException.class, () ->  new PagedTopicSubscriber(topic, null));
        }

    @Test
    public void shouldCreateChannelForAnonymousSubscriber()
        {
        Member member = mock(Member.class);
        when(member.getTimestamp()).thenReturn(System.currentTimeMillis());

        SubscriberGroupId subscriberGroupId = new SubscriberGroupId(member);
        PagedTopicChannel channel           = new PagedTopicChannel();
        int               nPart             = Math.abs((HashHelper.hash(subscriberGroupId.hashCode(), 3) % 257));

        channel.subscriberPartitionSync = new Subscription.Key(nPart, 0, subscriberGroupId);
        assertNotNull(channel.toString());
        }

    @Test
    public void shouldCreateChannelForGroupSubscriber()
        {
        PagedTopicChannel channel           = new PagedTopicChannel();
        SubscriberGroupId subscriberGroupId = SubscriberGroupId.withName("durableSubscriber");
        int               nPart             = Math.abs((HashHelper.hash(subscriberGroupId.hashCode(), 3) % 257));

        channel.subscriberPartitionSync = new Subscription.Key(nPart, 0, subscriberGroupId);
        assertNotNull(channel.toString());
        }
    }
