/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic;

import com.tangosol.internal.net.topic.NamedTopicSubscriber.TopicChannel;
import com.tangosol.internal.net.topic.SubscriberConnector.ConnectedSubscriber;
import com.tangosol.internal.net.topic.SubscriberConnector.RecoverableReceiveException;
import com.tangosol.internal.net.topic.SubscriberConnector.ReceiveHandler;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriberConnector;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.internal.net.topic.impl.paged.model.Subscription;

import com.tangosol.net.NamedCache;

import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.UUID;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.Assert.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PagedTopicSubscriberConnector}.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class PagedTopicSubscriberConnectorTest
    {
    @Test
    public void shouldThrowSynchronouslyWhenUnknownChannelHeadSubscriptionIsMissing()
        {
        int                                        cParts        = 257;
        int                                        nChannel      = 7;
        SubscriberGroupId                          groupId        = SubscriberGroupId.withName("missing-subscription-test");
        SubscriberId                               subscriberId  = new SubscriberId(1, 1, new UUID());
        PagedTopicCaches                           caches         = mock(PagedTopicCaches.class);
        NamedCache<Subscription.Key, Subscription> subscriptions = mock(NamedCache.class);
        ConnectedSubscriber<String>                subscriber     = mock(ConnectedSubscriber.class);
        ReceiveHandler                             handler        = mock(ReceiveHandler.class);
        NamedTopicSubscriber.WithSubscriberId<String, String> optionSubscriberId = ignored -> subscriberId;

        caches.Subscriptions = subscriptions;
        when(caches.getPartitionCount()).thenReturn(cParts);

        Subscriber.Option[] options = new Subscriber.Option[]
            {
            Subscriber.Name.of(groupId.getGroupName()),
            optionSubscriberId
            };

        PagedTopicSubscriberConnector<String> connector = new PagedTopicSubscriberConnector<>(caches, options);
        TopicChannel                          channel   = connector.createChannel(subscriber, nChannel);
        Subscription.Key                      syncKey   = Subscription.createSyncKey(groupId, nChannel, cParts);

        when(subscriptions.get(syncKey)).thenReturn(null);

        RecoverableReceiveException error = assertThrows(RecoverableReceiveException.class,
                () -> connector.receive(subscriber, nChannel, channel.getHead(), 1L, handler));

        assertThat(error.getSubscriberId(), sameInstance(subscriberId));
        assertThat(error.getSubscriberGroupId(), is(groupId));
        assertThat(error.getChannel(), is(nChannel));
        verify(subscriptions).get(syncKey);
        verify(subscriptions, never()).getCacheService();
        verify(handler, never()).onReceive(anyLong(), any(), any(), any());
        }
    }
