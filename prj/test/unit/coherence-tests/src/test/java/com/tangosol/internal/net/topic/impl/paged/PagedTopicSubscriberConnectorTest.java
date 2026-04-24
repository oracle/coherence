/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;

import com.tangosol.net.PagedTopicService;

import com.tangosol.util.UUID;

import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PagedTopicSubscriberConnectorTest
    {
    @Test
    public void shouldRepeatCloseNotificationUntilSubscriberRemoved()
        {
        PagedTopicService  service      = mock(PagedTopicService.class);
        SubscriberGroupId  groupId      = SubscriberGroupId.withName("foo");
        SubscriberId       subscriberId = new SubscriberId(19, 7, new UUID());
        AtomicInteger      cNotify      = new AtomicInteger();

        when(service.getSubscribers("topic-one", groupId))
                .thenReturn(Collections.singleton(subscriberId), Collections.emptySet());

        boolean fClosed = PagedTopicSubscriberConnector.waitForSubscriberToClose(service, "topic-one", groupId,
                subscriberId, cNotify::incrementAndGet, 5000L);

        assertThat(fClosed, is(true));
        assertThat(cNotify.get(), is(1));
        verify(service, times(2)).getSubscribers("topic-one", groupId);
        }

    @Test
    public void shouldStopRetryingWhenCloseTimeoutExpires()
        {
        PagedTopicService  service      = mock(PagedTopicService.class);
        SubscriberGroupId  groupId      = SubscriberGroupId.withName("foo");
        SubscriberId       subscriberId = new SubscriberId(27, 9, new UUID());
        AtomicInteger      cNotify      = new AtomicInteger();

        when(service.getSubscribers("topic-two", groupId))
                .thenReturn(Collections.singleton(subscriberId));

        boolean fClosed = PagedTopicSubscriberConnector.waitForSubscriberToClose(service, "topic-two", groupId,
                subscriberId, cNotify::incrementAndGet, 0L);

        assertThat(fClosed, is(false));
        assertThat(cNotify.get(), is(0));
        verify(service).getSubscribers("topic-two", groupId);
        }
    }
