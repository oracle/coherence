/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber.Channel;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.Subscription;
import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.Member;
import com.tangosol.util.HashHelper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.tangosol.net.topic.Subscriber.Name.of;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author jk 2015.08.24
 */
public class PagedTopicSubscriberTest
    {
    @Test
    public void shouldNotCreateQueueSubscriberWithNullQueueCaches() throws Exception
        {
        PagedTopicCaches storage = null;
        String            sSubscriberId = "foo";

        m_expectedException.expect(NullPointerException.class);
        m_expectedException.expectMessage(containsString("The TopicCaches parameter cannot be null"));

        new PagedTopicSubscriber(storage, of(sSubscriberId));
        }

    @Test
    public void shouldCreateChannelForAnonymousSubscriber()
        {
        Member            member            = mock(Member.class);
        when(member.getTimestamp()).thenReturn(System.currentTimeMillis());

        SubscriberGroupId subscriberGroupId = new SubscriberGroupId(member);
        Channel           channel           = new PagedTopicSubscriber.Channel();
        int               nPart             = Math.abs((HashHelper.hash(subscriberGroupId.hashCode(), 3) % 257));

        channel.subscriberPartitionSync = new Subscription.Key(nPart, 0, subscriberGroupId);
        assertNotNull(channel.toString());
        }

    @Test
    public void shouldCreateChannelForGroupSubscriber()
        {
        Channel           channel           = new PagedTopicSubscriber.Channel();
        SubscriberGroupId subscriberGroupId = SubscriberGroupId.withName("durableSubscriber");
        int               nPart             = Math.abs((HashHelper.hash(subscriberGroupId.hashCode(), 3) % 257));

        channel.subscriberPartitionSync = new Subscription.Key(nPart, 0, subscriberGroupId);
        assertNotNull(channel.toString());
        }

//    @Test
//    public void shouldInitialiseSubscriberConstructor() throws Exception
//        {
//        TopicCaches storage     = mock(TopicCaches.class);
//        String             sSubscriberId = "Foo";
//
//        new TopicSubscriber(storage, of(sSubscriberId));
//
//        ArgumentCaptor<TopicSubscriberId> idCaptor = ArgumentCaptor.forClass(TopicSubscriberId.class);
//        ArgumentCaptor<OptionsMap> optsCaptor = ArgumentCaptor.forClass(OptionsMap.class);
//        verify(storage).createSubscriber(idCaptor.capture(), optsCaptor.capture());
//
//        assertThat(idCaptor.getValue().getSubscriberName(), is(sSubscriberId));
//        }

//    @Test
//    public void shouldInitialiseHeadOnlyOnce() throws Exception
//        {
//        TopicCaches   topicCaches   = mock(TopicCaches.class);
//        String              sSubscriber         = "Foo";
//        long                nHead               = 19L;
//        TopicMetaData topicMetaData = new TopicMetaData();
//
//        NamedCache<TopicSubscriberId, TopicSubscriberMetaData> cache = mock(NamedCache.class);
//
//        topicMetaData.setHeadPageId(nHead);
//
//        when(topicCaches.ensureCache(TopicCacheNames.Subscribers)).thenReturn(cache);
//        when(topicCaches.initiailiseQueueInfo()).thenReturn(topicMetaData);
//        when(cache.invoke(any(TopicSubscriberId.class), any(TopicSubscriberInitialiseProcessor.class)))
//                .thenReturn(nHead);
//
//        TopicSubscriber subscriber = new TopicSubscriber(topicCaches, of(sSubscriberId));
//
//        subscriber.initialiseMetaData();
//        subscriber.initialiseMetaData();
//
//        assertThat(subscriber.ensureHeadPointer(), is(nHead));
//
//        ArgumentCaptor<TopicSubscriberId> idCaptor = ArgumentCaptor.forClass(TopicSubscriberId.class);
//        ArgumentCaptor<TopicSubscriberInitialiseProcessor> processorCaptor = ArgumentCaptor.forClass(TopicSubscriberInitialiseProcessor.class);
//
//        verify(cache, times(1)).invoke(idCaptor.capture(), processorCaptor.capture());
//
//        assertThat(idCaptor.getValue().getSubscriberName(), is(sSubscriberId));
//        assertThat(processorCaptor.getValue().getSubscriptionHead(), is(19L));
//        }
//
//    @Test
//    public void shouldPoll() throws Exception
//        {
//        TopicCaches                      topicCaches = mock(TopicCaches.class);
//        String                                 sSubscriberId       = "Foo";
//        TopicPosition                    position          = new TopicPosition(1L,2);
//        String                                 sElement          = "Element-1";
//        SortedMap<TopicPosition, String> mapResult         = new TreeMap<>();
//
//        mapResult.put(position, sElement);
//
//        TopicSubscriber<String> subscriber    = new TopicSubscriber<>(topicCaches, 19L, of(sSubscriberId));
//        TopicSubscriber<String> subscriberSpy = spy(subscriber);
//
//        doReturn(mapResult).when(subscriberSpy).pollOrPeekFromHead(anyBoolean());
//
//        Optional<String> result = subscriberSpy.poll();
//
//        assertThat(result, is(notNullValue()));
//        assertThat(result.get(), is(sElement));
//
//        verify(subscriberSpy).pollOrPeekFromHead(true);
//        }
//
//    @Test
//    public void shouldPeek() throws Exception
//        {
//        TopicCaches                      topicCaches = mock(TopicCaches.class);
//        String                                 sSubscriberId       = "Foo";
//        TopicPosition                    position          = new TopicPosition(1L,2);
//        String                                 sElement          = "Element-1";
//        SortedMap<TopicPosition, String> mapResult         = new TreeMap<>();
//
//        mapResult.put(position, sElement);
//
//        TopicSubscriber<String> subscriber    = new TopicSubscriber<>(topicCaches, 19L, of(sSubscriberId));
//        TopicSubscriber<String> subscriberSpy = spy(subscriber);
//
//        doReturn(mapResult).when(subscriberSpy).pollOrPeekFromHead(anyBoolean());
//
//        Optional<String> result = subscriberSpy.peek();
//
//        assertThat(result, is(notNullValue()));
//        assertThat(result.get(), is(sElement));
//
//        verify(subscriberSpy).pollOrPeekFromHead(false);
//        }
//
//    @Test
//    public void shouldPollWhenNotInitialised() throws Exception
//        {
//        TopicCaches   topicCaches   = mock(TopicCaches.class);
//        String              sSubscriberId         = "Foo";
//        long                nHead               = TopicSubscriber.NOT_INITIALISED;
//        TopicMetaData topicMetaData = new TopicMetaData();
//
//        NamedCache<TopicSubscriberId, TopicSubscriberMetaData> cache = mock(NamedCache.class);
//
//        topicMetaData.setHeadPageId(nHead);
//
//        when(topicCaches.ensureCache(TopicCacheNames.Subscribers)).thenReturn(cache);
//        when(topicCaches.initiailiseQueueInfo()).thenReturn(topicMetaData);
//        when(cache.invoke(any(TopicSubscriberId.class), any(TopicSubscriberInitialiseProcessor.class))).thenReturn(nHead);
//
//        TopicSubscriber<String> subscriber = new TopicSubscriber<>(topicCaches, of(sSubscriberId));
//
//        SortedMap<TopicPosition, String> mapResults = subscriber.pollOrPeekFromHead(true);
//
//        assertThat(mapResults, is(notNullValue()));
//        assertThat(mapResults.isEmpty(), is(true));
//        }
//
//    @Test
//    public void shouldPollFromEmptyQueue() throws Exception
//        {
//        TopicCaches     topicCaches = mock(TopicCaches.class);
//        TopicSubscriberId subscriberId        = new TopicSubscriberId("Foo");
//        long                  nHead             = 19L;
//        TopicPollResult pollResult        = new TopicPollResult(TopicPollResult.Status.QueueEmpty);
//
//        NamedCache<TopicPageSubscriberKey, Integer> cacheHeads = mock(NamedCache.class, "Heads");
//
//        when(topicCaches.ensureCache(TopicCacheNames.SubscriberHeads)).thenReturn(cacheHeads);
//        when(cacheHeads.invoke(any(TopicPageSubscriberKey.class), any(InvocableMap.EntryProcessor.class))).thenReturn(pollResult);
//
//        TopicSubscriber<String> subscriber = new TopicSubscriber<>(topicCaches, nHead, of(subscriberId.getSubscriberName()));
//
//        SortedMap<TopicPosition, String> mapResults = subscriber.pollOrPeekFromHead(true);
//
//        assertThat(mapResults, is(notNullValue()));
//        assertThat(mapResults.isEmpty(), is(true));
//
//        ArgumentCaptor<TopicPollHeadProcessor> captor = ArgumentCaptor.forClass(TopicPollHeadProcessor.class);
//
//        verify(cacheHeads).invoke(eq(new TopicPageSubscriberKey(nHead, subscriberId.getGroupId())), captor.capture());
//
//        TopicPollHeadProcessor processor = captor.getValue();
//
//        assertThat(processor.m_fPoll, is(true));
//        }
//
//    @Test
//    public void shouldPeekAtEmptyQueue() throws Exception
//        {
//        TopicCaches     topicCaches = mock(TopicCaches.class);
//        TopicSubscriberId subscriberId        = new TopicSubscriberId("Foo");
//        long                  nHead             = 19L;
//        TopicPollResult pollResult        = new TopicPollResult(TopicPollResult.Status.QueueEmpty);
//
//        NamedCache<TopicPageSubscriberKey, Integer> cacheHeads = mock(NamedCache.class, "Heads");
//
//        when(topicCaches.ensureCache(TopicCacheNames.SubscriberHeads)).thenReturn(cacheHeads);
//        when(cacheHeads.invoke(any(TopicPageSubscriberKey.class), any(InvocableMap.EntryProcessor.class))).thenReturn(pollResult);
//
//        TopicSubscriber<String> subscriber = new TopicSubscriber<>(topicCaches, nHead, of(subscriberId.getSubscriberName()));
//
//        SortedMap<TopicPosition, String> mapResults = subscriber.pollOrPeekFromHead(false);
//
//        assertThat(mapResults, is(notNullValue()));
//        assertThat(mapResults.isEmpty(), is(true));
//
//        ArgumentCaptor<TopicPollHeadProcessor> captor = ArgumentCaptor.forClass(TopicPollHeadProcessor.class);
//
//        verify(cacheHeads).invoke(eq(new TopicPageSubscriberKey(nHead, subscriberId.getGroupId())), captor.capture());
//
//        TopicPollHeadProcessor processor = captor.getValue();
//
//        assertThat(processor.m_fPoll, is(false));
//        }
//
//    @Test
//    public void shouldPollFromQueue() throws Exception
//        {
//        TopicCaches                                 topicCaches = mock(TopicCaches.class);
//        TopicSubscriberId                             subscriberId        = new TopicSubscriberId("Foo");
//        NamedCache<TopicPageSubscriberKey, Integer> cacheHeads        = mock(NamedCache.class, "Heads");
//        long                                              nHead             = 19L;
//        TopicPosition                               position          = new TopicPosition(1L,2);
//        String                                            sElement          = "Element-1";
//        Binary                                            binElement        = ExternalizableHelper.toBinary(sElement, f_serializer);
//        SortedMap<TopicPosition, Binary>            mapElements       = new TreeMap<>();
//
//        TopicPollResult pollResult = new TopicPollResult(TopicPollResult.Status.Success, mapElements);
//
//        mapElements.put(position, binElement);
//
//        when(topicCaches.ensureCache(TopicCacheNames.SubscriberHeads)).thenReturn(cacheHeads);
//        when(topicCaches.getSerializer()).thenReturn(f_serializer);
//        when(cacheHeads.invoke(any(TopicPageSubscriberKey.class), any(InvocableMap.EntryProcessor.class))).thenReturn(pollResult);
//
//        TopicSubscriber<String> subscriber = new TopicSubscriber<>(topicCaches, nHead, of(subscriberId.getSubscriberName()));
//
//        SortedMap<TopicPosition, String> mapResults = subscriber.pollOrPeekFromHead(true);
//
//        assertThat(mapResults, is(notNullValue()));
//        assertThat(mapResults.size(), is(1));
//        assertThat(mapResults.get(position), is(sElement));
//
//        ArgumentCaptor<TopicPollHeadProcessor> captor = ArgumentCaptor.forClass(TopicPollHeadProcessor.class);
//
//        verify(cacheHeads).invoke(eq(new TopicPageSubscriberKey(nHead, subscriberId.getGroupId())), captor.capture());
//
//        TopicPollHeadProcessor processor = captor.getValue();
//
//        assertThat(processor.m_fPoll, is(true));
//        }
//
//    @Test
//    public void shouldMoveToNextHead() throws Exception
//        {
//        TopicCaches           topicCaches = mock(TopicCaches.class);
//        String                      sQueueName              = "QueueTest";
//        String                      sSubscriberId             = "Foo";
//        long                        nHeadCurrent            = 19L;
//        long                        nQueueTail              = 20L;
//        TopicSubscriberMetaData subscriberMetaDataUpdated = new TopicSubscriberMetaData(nQueueTail);
//        TopicMetaData         topicMetaData     = new TopicMetaData();
//
//        NamedCache<TopicSubscriberId, TopicSubscriberMetaData> cacheSubscribers = mock(NamedCache.class, "Subscriber");
//        NamedCache<String, TopicMetaData>                        cacheQueueMeta = mock(NamedCache.class, "Meta");
//
//        topicMetaData.setHeadPageId(nHeadCurrent);
//        topicMetaData.setTailPageId(nQueueTail);
//
//        when(topicCaches.getTopicName()).thenReturn(sQueueName);
//        when(topicCaches.ensureCache(TopicCacheNames.QueueMetaData)).thenReturn(cacheQueueMeta);
//        when(topicCaches.ensureCache(TopicCacheNames.Subscribers)).thenReturn(cacheSubscribers);
//        when(cacheQueueMeta.get(sQueueName)).thenReturn(topicMetaData);
//        when(cacheSubscribers.invoke(any(TopicSubscriberId.class), any(TopicSubscriberHeadIncrementor.class))).thenReturn(nQueueTail);
//
//        TopicSubscriber<String> subscriber = new TopicSubscriber<>(topicCaches, nHeadCurrent, of(sSubscriberId));
//
//        long nHeadNext = subscriber.moveToNextHead(nHeadCurrent);
//
//        assertThat(nHeadNext, is(subscriberMetaDataUpdated.getSubscriptionHead()));
//        }
//
//    @Test
//    public void shouldNotMoveNextHeadPastQueueTail() throws Exception
//        {
//        TopicCaches           topicCaches   = mock(TopicCaches.class);
//        String                      sQueueName          = "QueueTest";
//        String                      sSubscriberId         = "Foo";
//        long                        nHeadCurrent        = 19L;
//        long                        nQueueTail          = 19L;
//        TopicMetaData         topicMetaData = new TopicMetaData();
//
//        NamedCache<TopicSubscriberId, TopicSubscriberMetaData> cacheSubscribers = mock(NamedCache.class, "Subscriber");
//        NamedCache<String, TopicMetaData>                        cacheQueueMeta = mock(NamedCache.class, "Meta");
//
//        topicMetaData.setHeadPageId(nHeadCurrent);
//        topicMetaData.setTailPageId(nQueueTail);
//
//        when(topicCaches.getTopicName()).thenReturn(sQueueName);
//        when(topicCaches.ensureCache(TopicCacheNames.QueueMetaData)).thenReturn(cacheQueueMeta);
//        when(topicCaches.ensureCache(TopicCacheNames.Subscribers)).thenReturn(cacheSubscribers);
//        when(cacheQueueMeta.get(sQueueName)).thenReturn(topicMetaData);
//        when(cacheSubscribers.invoke(any(TopicSubscriberId.class), any(TopicSubscriberHeadIncrementor.class))).thenReturn(nQueueTail);
//
//        TopicSubscriber<String> subscriber = new TopicSubscriber<>(topicCaches, nHeadCurrent, of(sSubscriberId));
//
//        long nHeadNext = subscriber.moveToNextHead(nHeadCurrent);
//
//        assertThat(nHeadNext, is(nHeadCurrent));
//        }

    @Rule
    public ExpectedException m_expectedException = ExpectedException.none();

    public static final Serializer f_serializer = new ConfigurablePofContext("coherence-pof-config.xml");
    }
