/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic;

import com.tangosol.internal.net.topic.SubscriberConnector.RecoverableReceiveException;

import com.tangosol.internal.net.topic.impl.paged.BatchingOperationsQueue;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriberConnector.PagedTopicChannel;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;

import com.tangosol.io.DefaultSerializer;

import com.tangosol.net.TopicService;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.TopicDependencies;

import com.tangosol.util.ThreadGateLite;
import com.tangosol.util.UUID;

import org.junit.Test;

import java.lang.reflect.Field;

import java.util.Collections;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression tests for receive recovery failures.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class NamedTopicSubscriberReceiveRecoveryTest
    {
    @Test
    public void shouldReconnectAndRearmReceiveQueueAfterRecoverableSynchronousFailure() throws Exception
        {
        NamedTopic<String>          topic        = mock(NamedTopic.class);
        TopicService                service      = mock(TopicService.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        SubscriberConnector<String> connector    = mock(SubscriberConnector.class);
        TopicDependencies           dependencies = mock(TopicDependencies.class);
        SubscriberId                subscriberId = new SubscriberId(1, 1, new UUID());
        SubscriberGroupId           groupId       = SubscriberGroupId.withName("receive-recovery-test-group");
        RecoverableReceiveException failure      = new RecoverableReceiveException(
                "Expected synchronous receive failure", subscriberId, groupId, 0);
        CompletableFuture<ReceiveResult> futureReplacementOne = new CompletableFuture<>();
        CountDownLatch              latchReplacement = new CountDownLatch(1);
        CountDownLatch              latchDaemonEntered = new CountDownLatch(1);
        CountDownLatch              latchDaemonRelease = new CountDownLatch(1);
        AtomicInteger               cReceive         = new AtomicInteger();
        AtomicBoolean               fFailAfterStop   = new AtomicBoolean();
        AtomicLong                  lVersionOne       = new AtomicLong();
        AtomicReference<SubscriberConnector.ReceiveHandler> handlerOne = new AtomicReference<>();

        when(topic.getName()).thenReturn("receive-recovery-test-topic");
        when(topic.getChannelCount()).thenReturn(1);
        when(topic.getService()).thenReturn(service);
        when(topic.getTopicService()).thenReturn(service);
        when(service.getSerializer()).thenReturn(new DefaultSerializer());
        when(service.getCluster().getDependencies().getPublisherCloggedCount()).thenReturn(100);
        when(service.isSuspended()).thenReturn(false);

        when(dependencies.getReconnectTimeoutMillis()).thenReturn(1_000L);
        when(dependencies.getReconnectRetryMillis()).thenReturn(1L);
        when(dependencies.getReconnectWaitMillis()).thenReturn(1L);

        when(connector.getSubscriberId()).thenReturn(subscriberId);
        when(connector.getSubscriberGroupId()).thenReturn(groupId);
        when(connector.getTopicDependencies()).thenReturn(dependencies);
        when(connector.getTypeName()).thenReturn("test");
        when(connector.isActive()).thenReturn(true);
        when(connector.getConnectionTimestamp()).thenReturn(1L);
        when(connector.getOwnedChannels(any())).thenReturn(channels(0));
        when(connector.createChannel(any(), anyInt())).thenAnswer(invocation ->
                new PagedTopicChannel(invocation.getArgument(1)));
        when(connector.initialize(any(), anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn(new Position[] {Position.EMPTY_POSITION});
        doAnswer(invocation ->
            {
            int cCall = cReceive.incrementAndGet();
            if (cCall == 1 || fFailAfterStop.get())
                {
                throw failure;
                }
            if (cCall == 2)
                {
                handlerOne.set(invocation.getArgument(5));
                lVersionOne.set(invocation.getArgument(3));
                latchReplacement.countDown();
                return futureReplacementOne;
                }
            throw new AssertionError("Unexpected receive call " + cCall);
            }).when(connector).receive(any(), anyInt(), any(), anyLong(), anyInt(), any());

        Subscriber.Option[] options = new Subscriber.Option[]
            {
            Subscriber.Name.of("receive-recovery-test-group"),
            Subscriber.CompleteOnEmpty.enabled()
            };

        try (TestSubscriber subscriber = new TestSubscriber(topic, connector, options))
            {
            subscriber.forceNestedReceiveOnce();
            CompletableFuture<Subscriber.Element<String>> futureOne = subscriber.receive();
            CompletableFuture<Subscriber.Element<String>> futureTwo = subscriber.receive();
            assertThat(latchReplacement.await(1, TimeUnit.MINUTES), is(true));
            BatchingOperationsQueue<?, ?> queue = subscriber.getReceiveQueue();

            assertThat(futureOne.isDone(), is(false));
            assertThat(futureTwo.isDone(), is(false));
            assertThat(queue.getCurrentBatchSize(), is(1));
            assertThat(queue.getPendingSize(), is(1));
            assertThat(queue.toString(), containsString("trigger=TRIGGER_CLOSED"));
            assertThat(subscriber.getDisconnectCount(), is(1L));
            assertThat(subscriber.wasGateEnteredDuringDisconnect(), is(false));
            verify(connector, times(2)).initialize(any(), anyBoolean(), anyBoolean(), anyBoolean());
            verify(connector, times(2)).receive(any(), anyInt(), any(), anyLong(), anyInt(), any());

            ReceiveResult result = new SimpleReceiveResult(new LinkedList<>(), 0, ReceiveResult.Status.Success);
            handlerOne.get().onReceive(lVersionOne.get(), result, null, null);
            futureReplacementOne.complete(result);
            assertThat(futureOne.get(1, TimeUnit.MINUTES), is(nullValue()));
            assertThat(futureTwo.get(1, TimeUnit.MINUTES), is(nullValue()));
            verify(connector, times(2)).receive(any(), anyInt(), any(), anyLong(), anyInt(), any());

            fFailAfterStop.set(true);
            subscriber.markChannelPopulated();
            subscriber.blockReceiveDaemon(latchDaemonEntered, latchDaemonRelease);
            assertThat(latchDaemonEntered.await(1, TimeUnit.MINUTES), is(true));
            try
                {
                subscriber.stopReceiveDaemon();
                CompletableFuture<Subscriber.Element<String>> futureShutdown = subscriber.receive();
                assertThat(futureShutdown.isDone(), is(false));
                assertThat(subscriber.getReceiveRecoveryOperation(), is(-1L));
                }
            finally
                {
                latchDaemonRelease.countDown();
                subscriber.awaitReceiveDaemonStopped();
                }
            }
        }

    @Test
    public void shouldRearmReceiveQueueOnceWhenDisconnectedDuringSynchronousFailure() throws Exception
        {
        NamedTopic<String>          topic        = mock(NamedTopic.class);
        TopicService                service      = mock(TopicService.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        SubscriberConnector<String> connector    = mock(SubscriberConnector.class);
        TopicDependencies           dependencies = mock(TopicDependencies.class);
        SubscriberId                subscriberId = new SubscriberId(1, 1, new UUID());
        SubscriberGroupId           groupId       = SubscriberGroupId.withName("receive-recovery-disconnect-test-group");
        RecoverableReceiveException failure      = new RecoverableReceiveException(
                "Expected synchronous receive failure", subscriberId, groupId, 0);
        CompletableFuture<ReceiveResult> futureReplacement = new CompletableFuture<>();
        CountDownLatch              latchReceiveEntered  = new CountDownLatch(1);
        CountDownLatch              latchReceiveRelease  = new CountDownLatch(1);
        CountDownLatch              latchReplacement     = new CountDownLatch(1);
        AtomicInteger               cReceive              = new AtomicInteger();
        ExecutorService             executor              = Executors.newSingleThreadExecutor();

        when(topic.getName()).thenReturn("receive-recovery-disconnect-test-topic");
        when(topic.getChannelCount()).thenReturn(1);
        when(topic.getService()).thenReturn(service);
        when(topic.getTopicService()).thenReturn(service);
        when(service.getSerializer()).thenReturn(new DefaultSerializer());
        when(service.getCluster().getDependencies().getPublisherCloggedCount()).thenReturn(100);
        when(service.isSuspended()).thenReturn(false);

        when(dependencies.getReconnectTimeoutMillis()).thenReturn(1_000L);
        when(dependencies.getReconnectRetryMillis()).thenReturn(1L);
        when(dependencies.getReconnectWaitMillis()).thenReturn(TimeUnit.MINUTES.toMillis(1));

        when(connector.getSubscriberId()).thenReturn(subscriberId);
        when(connector.getSubscriberGroupId()).thenReturn(groupId);
        when(connector.getTopicDependencies()).thenReturn(dependencies);
        when(connector.getTypeName()).thenReturn("test");
        when(connector.isActive()).thenReturn(true);
        when(connector.getConnectionTimestamp()).thenReturn(1L);
        when(connector.getOwnedChannels(any())).thenReturn(channels(0));
        when(connector.createChannel(any(), anyInt())).thenAnswer(invocation ->
                new PagedTopicChannel(invocation.getArgument(1)));
        when(connector.initialize(any(), anyBoolean(), anyBoolean(), anyBoolean()))
                .thenReturn(new Position[] {Position.EMPTY_POSITION});
        doAnswer(invocation ->
            {
            if (cReceive.incrementAndGet() == 1)
                {
                latchReceiveEntered.countDown();
                assertThat(latchReceiveRelease.await(1, TimeUnit.MINUTES), is(true));
                throw failure;
                }
            latchReplacement.countDown();
            return futureReplacement;
            }).when(connector).receive(any(), anyInt(), any(), anyLong(), anyInt(), any());

        Subscriber.Option[] options = new Subscriber.Option[]
            {
            Subscriber.Name.of("receive-recovery-disconnect-test-group")
            };

        try (TestSubscriber subscriber = new TestSubscriber(topic, connector, options))
            {
            CompletableFuture<CompletableFuture<Subscriber.Element<String>>> futureReceive =
                    CompletableFuture.supplyAsync(subscriber::receive, executor);
            assertThat(latchReceiveEntered.await(1, TimeUnit.MINUTES), is(true));

            CompletableFuture<Subscriber.Element<String>> futureTwo = subscriber.receive();
            subscriber.disconnect();
            assertThat(subscriber.isDisconnected(), is(true));
            assertThat(subscriber.getDisconnectCount(), is(1L));
            assertThat(subscriber.wasGateEnteredDuringDisconnect(), is(false));

            latchReceiveRelease.countDown();
            CompletableFuture<Subscriber.Element<String>> futureOne = futureReceive.get(1, TimeUnit.MINUTES);
            subscriber.reconnectNow();
            assertThat(latchReplacement.await(1, TimeUnit.MINUTES), is(true));

            BatchingOperationsQueue<?, ?> queue = subscriber.getReceiveQueue();
            assertThat(futureOne.isDone(), is(false));
            assertThat(futureTwo.isDone(), is(false));
            assertThat(queue.getCurrentBatchSize(), is(1));
            assertThat(queue.getPendingSize(), is(1));
            assertThat(queue.toString(), containsString("trigger=TRIGGER_CLOSED"));
            assertThat(subscriber.getDisconnectCount(), is(1L));
            verify(connector, times(2)).initialize(any(), anyBoolean(), anyBoolean(), anyBoolean());
            verify(connector, times(2)).receive(any(), anyInt(), any(), anyLong(), anyInt(), any());
            }
        finally
            {
            latchReceiveRelease.countDown();
            executor.shutdownNow();
            }
        }

    private static SortedSet<Integer> channels(Integer... anChannel)
        {
        SortedSet<Integer> setChannel = new TreeSet<>();
        Collections.addAll(setChannel, anChannel);
        return Collections.unmodifiableSortedSet(setChannel);
        }

    private static class TestSubscriber
            extends NamedTopicSubscriber<String>
        {
        private volatile boolean m_fGateEnteredDuringDisconnect;
        private final AtomicBoolean f_fForceNestedReceive = new AtomicBoolean();

        TestSubscriber(NamedTopic<?> topic, SubscriberConnector<String> connector, Subscriber.Option[] options)
            {
            super(topic, connector, options);
            }

        @Override
        protected void registerMBean()
            {
            }

        @Override
        protected void unregisterMBean()
            {
            }

        @Override
        public void disconnectInternal(boolean fForceReconnect)
            {
            m_fGateEnteredDuringDisconnect |= ((ThreadGateLite<?>) f_gate).isEnteredByCurrentThread();
            super.disconnectInternal(fForceReconnect);
            }

        BatchingOperationsQueue<?, ?> getReceiveQueue()
            {
            return f_queueReceiveOrders;
            }

        long getReceiveRecoveryOperation() throws ReflectiveOperationException
            {
            Field field = NamedTopicSubscriber.class.getDeclaredField("f_receiveRecoveryPending");
            field.setAccessible(true);
            return ((AtomicLong) field.get(this)).get();
            }

        boolean wasGateEnteredDuringDisconnect()
            {
            return m_fGateEnteredDuringDisconnect;
            }

        void forceNestedReceiveOnce()
            {
            f_fForceNestedReceive.set(true);
            }

        @Override
        protected int ensureOwnedChannel()
            {
            if (f_fForceNestedReceive.compareAndSet(true, false))
                {
                m_nChannel = -1;
                return -1;
                }
            return super.ensureOwnedChannel();
            }

        void reconnectNow()
            {
            reconnectInternal();
            }

        void stopReceiveDaemon()
            {
            f_daemon.stop(false);
            }

        void awaitReceiveDaemonStopped() throws InterruptedException
            {
            Thread thread = f_daemon.getThread();
            if (thread != null)
                {
                thread.join(TimeUnit.MINUTES.toMillis(1));
                assertThat(thread.isAlive(), is(false));
                }
            }

        void markChannelPopulated()
            {
            m_aChannel[0].setPopulated();
            }

        void blockReceiveDaemon(CountDownLatch latchEntered, CountDownLatch latchRelease)
            {
            f_daemon.executeTask(() ->
                {
                latchEntered.countDown();
                try
                    {
                    latchRelease.await(1, TimeUnit.MINUTES);
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    }
                });
            }
        }
    }
