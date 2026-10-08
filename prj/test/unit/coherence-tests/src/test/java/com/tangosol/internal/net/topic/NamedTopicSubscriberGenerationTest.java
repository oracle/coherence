/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic;

import com.tangosol.internal.net.topic.SubscriberConnector.SubscriberEvent;
import com.tangosol.internal.net.topic.SubscriberConnector.SubscriberListener;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriberConnector.PagedTopicChannel;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;

import com.tangosol.io.DefaultSerializer;

import com.tangosol.net.TopicService;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.TopicDependencies;
import com.tangosol.net.topic.TopicException;

import com.tangosol.util.UUID;

import org.junit.Test;

import java.lang.reflect.Field;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression tests for subscriber-local channel-allocation generations.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class NamedTopicSubscriberGenerationTest
    {
    @Test
    public void shouldRejectChannelsLostFromPreviousConnection() throws Exception
        {
        for (int i = 0; i < 100; i++)
            {
            TestContext context = new TestContext(channels(1, 2));

            try (TestSubscriber subscriber = context.createSubscriber())
                {
                assertThat(subscriber.getChannelSet(), is(channels(1, 2)));

                subscriber.disconnect();
                context.f_setOwned.set(channels(3, 4));

                Thread thread = new Thread(subscriber::connect, "subscriber-reconnect-" + i);
                thread.start();
                assertThat(context.f_latchReconnectEntered.await(1, TimeUnit.MINUTES), is(true));

                context.f_listener.get().onEvent(new SubscriberEvent(
                        context.f_connector, SubscriberEvent.Type.ChannelsLost));

                context.f_latchAllowReconnect.countDown();
                thread.join(TimeUnit.MINUTES.toMillis(1));
                assertThat(thread.isAlive(), is(false));
                assertThat(subscriber.drainChannelDaemon(), is(true));

                assertThat(subscriber.getChannelSet(), is(channels(3, 4)));
                }
            }
        }

    @Test
    public void shouldApplyCurrentChannelsLostAndAllocation() throws Exception
        {
        TestContext context = new TestContext(channels(1, 2));

        try (TestSubscriber subscriber = context.createSubscriber())
            {
            clearInvocations(context.f_ownershipListener);

            context.emit(SubscriberEvent.Type.ChannelAllocation, channels(3, 4));
            assertThat(subscriber.drainChannelDaemon(), is(true));
            assertThat(subscriber.getChannelSet(), is(channels(3, 4)));

            context.emit(SubscriberEvent.Type.ChannelsLost, null);
            assertThat(subscriber.drainChannelDaemon(), is(true));
            assertThat(subscriber.getChannelSet(), is(Collections.emptySet()));
            verify(context.f_ownershipListener).onChannelsLost(channels(3, 4));
            }
        }

    @Test
    public void shouldApplyAllocationDeliveredAfterGenerationAdvance() throws Exception
        {
        TestContext context = new TestContext(channels(1, 2));

        try (TestSubscriber subscriber = context.createSubscriber())
            {
            subscriber.disconnect();
            context.f_setOwned.set(channels(2, 3));
            context.blockOwnedChannels(2);

            Thread thread = new Thread(subscriber::connect, "subscriber-reconnect");
            thread.start();
            assertThat(context.f_latchReconnectEntered.await(1, TimeUnit.MINUTES), is(true));
            context.f_latchAllowReconnect.countDown();
            assertThat(context.f_latchOwnedEntered.await(1, TimeUnit.MINUTES), is(true));

            context.emit(SubscriberEvent.Type.ChannelAllocation, channels(3, 4));
            context.f_latchAllowOwned.countDown();
            thread.join(TimeUnit.MINUTES.toMillis(1));
            assertThat(subscriber.drainChannelDaemon(), is(true));

            assertThat(subscriber.getChannelSet(), is(channels(3, 4)));
            }
        }

    @Test
    public void shouldRejectEventsFromFailedCandidate() throws Exception
        {
        TestContext context = new TestContext(channels(1, 2));
        CountDownLatch latchChannelDaemonEntered = new CountDownLatch(1);
        CountDownLatch latchAllowChannelDaemon   = new CountDownLatch(1);

        try (TestSubscriber subscriber = context.createSubscriber())
            {
            subscriber.blockChannelDaemon(latchChannelDaemonEntered, latchAllowChannelDaemon);
            assertThat(latchChannelDaemonEntered.await(1, TimeUnit.MINUTES), is(true));
            subscriber.disconnect();
            context.f_setOwned.set(channels(2, 3));
            context.blockOwnedChannels(2);
            context.f_nFailOnInitializedCall = 2;

            AtomicReference<Throwable> error = new AtomicReference<>();
            Thread thread = new Thread(() ->
                {
                try
                    {
                    subscriber.connect();
                    }
                catch (Throwable t)
                    {
                    error.set(t);
                    }
                }, "failed-subscriber-reconnect");
            thread.start();
            assertThat(context.f_latchReconnectEntered.await(1, TimeUnit.MINUTES), is(true));
            context.f_latchAllowReconnect.countDown();
            assertThat(context.f_latchOwnedEntered.await(1, TimeUnit.MINUTES), is(true));
            context.emit(SubscriberEvent.Type.ChannelAllocation, channels(3, 4));
            context.f_latchAllowOwned.countDown();
            thread.join(TimeUnit.MINUTES.toMillis(1));
            assertThat(error.get() instanceof TopicException, is(true));
            assertThat(subscriber.getOwnedChannelSet(), is(channels(2, 3)));

            context.f_nFailOnInitializedCall = -1;
            context.f_setOwned.set(channels(0, 4));
            subscriber.connect();

            latchAllowChannelDaemon.countDown();
            assertThat(subscriber.drainChannelDaemon(), is(true));
            assertThat(subscriber.getChannelSet(), is(channels(0, 4)));
            }
        finally
            {
            latchAllowChannelDaemon.countDown();
            }
        }

    @Test
    public void shouldPreserveAcceptedGenerationWhenCandidateFailsBeforeAdvance() throws Exception
        {
        TestContext context = new TestContext(channels(1, 2));

        try (TestSubscriber subscriber = context.createSubscriber())
            {
            clearInvocations(context.f_ownershipListener);
            subscriber.disconnect();
            context.f_nFailInitializeCall = 2;

            AtomicReference<Throwable> error = new AtomicReference<>();
            Thread thread = new Thread(() ->
                {
                try
                    {
                    subscriber.connect();
                    }
                catch (Throwable t)
                    {
                    error.set(t);
                    }
                }, "failed-before-generation-reconnect");
            thread.start();
            assertThat(context.f_latchReconnectEntered.await(1, TimeUnit.MINUTES), is(true));
            context.emit(SubscriberEvent.Type.ChannelsLost, null);
            context.f_latchAllowReconnect.countDown();
            thread.join(TimeUnit.MINUTES.toMillis(1));
            assertThat(error.get() instanceof TopicException, is(true));
            assertThat(subscriber.drainChannelDaemon(), is(true));

            assertThat(subscriber.getOwnedChannelSet(), is(Collections.emptySet()));
            verify(context.f_ownershipListener).onChannelsLost(channels(1, 2));

            context.f_nFailInitializeCall = -1;
            context.f_setOwned.set(channels(3, 4));
            subscriber.connect();
            assertThat(subscriber.getChannelSet(), is(channels(3, 4)));
            }
        }

    @Test
    public void shouldRejectStaleUnsubscribedAsOneAction() throws Exception
        {
        TestContext context = new TestContext(channels(1, 2));

        try (TestSubscriber subscriber = context.createSubscriber())
            {
            clearInvocations(context.f_ownershipListener);
            subscriber.disconnect();
            context.f_setOwned.set(channels(3, 4));

            Thread thread = new Thread(subscriber::connect, "subscriber-reconnect");
            thread.start();
            assertThat(context.f_latchReconnectEntered.await(1, TimeUnit.MINUTES), is(true));
            context.emit(SubscriberEvent.Type.Unsubscribed, null);
            context.f_latchAllowReconnect.countDown();
            thread.join(TimeUnit.MINUTES.toMillis(1));
            assertThat(subscriber.drainChannelDaemon(), is(true));

            assertThat(subscriber.getChannelSet(), is(channels(3, 4)));
            assertThat(subscriber.getState(), is(NamedTopicSubscriber.STATE_CONNECTED));
            assertThat(subscriber.getDisconnectCount(), is(1L));
            verify(context.f_ownershipListener, never()).onChannelsLost(any());
            }
        }

    @Test
    public void shouldApplyCurrentUnsubscribedAsOneAction() throws Exception
        {
        TestContext context = new TestContext(channels(1, 2));

        try (TestSubscriber subscriber = context.createSubscriber())
            {
            clearInvocations(context.f_ownershipListener);
            context.emit(SubscriberEvent.Type.Unsubscribed, null);
            assertThat(subscriber.drainChannelDaemon(), is(true));

            assertThat(subscriber.getOwnedChannelSet(), is(Collections.emptySet()));
            assertThat(subscriber.getState(), is(NamedTopicSubscriber.STATE_DISCONNECTED));
            assertThat(subscriber.getDisconnectCount(), is(1L));
            verify(context.f_ownershipListener, times(1)).onChannelsLost(channels(1, 2));
            }
        }

    @Test
    public void shouldReconnectPendingReceiveAfterCurrentUnsubscribed() throws Exception
        {
        TestContext context = new TestContext(channels(1, 2));
        context.f_cReconnectWaitMillis = 0L;

        try (TestSubscriber subscriber = context.createSubscriber())
            {
            subscriber.receive();
            context.f_setOwned.set(channels(3, 4));
            context.emit(SubscriberEvent.Type.Unsubscribed, null);

            assertThat(context.f_latchReconnectEntered.await(1, TimeUnit.MINUTES), is(true));
            context.f_latchAllowReconnect.countDown();
            assertThat(subscriber.drainDaemon(), is(true));
            assertThat(subscriber.drainChannelDaemon(), is(true));

            assertThat(subscriber.getState(), is(NamedTopicSubscriber.STATE_CONNECTED));
            assertThat(subscriber.getChannelSet(), is(channels(3, 4)));
            assertThat(subscriber.getDisconnectCount(), is(1L));
            assertThat(context.f_cInitialize.get(), is(2));
            assertThat(subscriber.getAllocationGeneration(), is(2L));
            assertThat(subscriber.getAcceptedAllocationGeneration(), is(2L));
            }
        }

    @Test
    public void shouldRetainDisconnectedEventBehavior() throws Exception
        {
        TestContext context = new TestContext(channels(1, 2));

        try (TestSubscriber subscriber = context.createSubscriber())
            {
            context.emit(SubscriberEvent.Type.Disconnected, null);
            assertThat(subscriber.drainEventExecutor(), is(true));

            assertThat(subscriber.getState(), is(NamedTopicSubscriber.STATE_DISCONNECTED));
            assertThat(subscriber.getChannelSet(), is(Collections.emptySet()));
            assertThat(subscriber.getOwnedChannelSet(), is(channels(1, 2)));
            assertThat(subscriber.getDisconnectCount(), is(1L));
            assertThat(context.f_cInitialize.get(), is(1));
            assertThat(subscriber.getAllocationGeneration(), is(1L));
            assertThat(subscriber.getAcceptedAllocationGeneration(), is(1L));
            verify(context.f_ownershipListener, never()).onChannelsLost(any());
            }
        }

    @Test
    public void shouldAdvanceGenerationForAnonymousSubscriberReconnect() throws Exception
        {
        SubscriberGroupId groupId = SubscriberGroupId.unsafe("anonymous-generation-test", 1L);
        TestContext       context = new TestContext(channels(1, 2), groupId);

        try (TestSubscriber subscriber = context.createSubscriber())
            {
            assertThat(subscriber.isAnonymous(), is(true));
            assertThat(subscriber.getChannelSet(), is(channels(0, 1, 2, 3, 4)));

            subscriber.disconnect();
            context.f_latchAllowReconnect.countDown();
            subscriber.connect();

            assertThat(subscriber.getChannelSet(), is(channels(0, 1, 2, 3, 4)));
            assertThat(subscriber.getOwnedChannelSet(), is(channels(0, 1, 2, 3, 4)));
            assertThat(context.f_cInitialize.get(), is(2));
            assertThat(subscriber.getAllocationGeneration(), is(2L));
            assertThat(subscriber.getAcceptedAllocationGeneration(), is(2L));
            }
        }

    @Test
    public void shouldRejectEventsBeforeAnyGenerationIsAccepted() throws Exception
        {
        TestContext context = new TestContext(channels(1, 2));
        context.f_fBlockInitialInitialize = true;
        context.f_nFailInitializeCall = 1;

        FutureTask<TestSubscriber> future = new FutureTask<>(context::createSubscriber);
        Thread thread = new Thread(future, "initial-subscriber-construction");
        thread.start();
        assertThat(context.f_latchInitialInitializeEntered.await(1, TimeUnit.MINUTES), is(true));

        TestSubscriber subscriber = context.f_subscriber.get();
        context.emit(SubscriberEvent.Type.ChannelsLost, null);
        context.emit(SubscriberEvent.Type.Unsubscribed, null);
        context.f_latchAllowInitialInitialize.countDown();

        try
            {
            future.get(1, TimeUnit.MINUTES);
            throw new AssertionError("Expected initial connection to fail");
            }
        catch (ExecutionException expected)
            {
            assertThat(expected.getCause() instanceof TopicException, is(true));
            }

        assertThat(subscriber.drainChannelDaemon(), is(true));
        assertThat(subscriber.getDisconnectCount(), is(0L));
        verify(context.f_ownershipListener, never()).onChannelsLost(any());
        subscriber.close();
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
        TestSubscriber(NamedTopic<?> topic, SubscriberConnector<String> connector, Subscriber.Option[] options)
            {
            super(topic, connector, options);
            }

        long getAllocationGeneration() throws ReflectiveOperationException
            {
            Field field = NamedTopicSubscriber.class.getDeclaredField("f_allocationGeneration");
            field.setAccessible(true);
            return ((AtomicLong) field.get(this)).get();
            }

        long getAcceptedAllocationGeneration() throws ReflectiveOperationException
            {
            Field field = NamedTopicSubscriber.class.getDeclaredField("m_lAcceptedAllocationGeneration");
            field.setAccessible(true);
            return field.getLong(this);
            }

        boolean drainChannelDaemon() throws InterruptedException
            {
            CountDownLatch latch = new CountDownLatch(1);
            f_daemonChannels.executeTask(latch::countDown);
            return latch.await(1, TimeUnit.MINUTES);
            }

        void blockChannelDaemon(CountDownLatch latchEntered, CountDownLatch latchRelease)
            {
            f_daemonChannels.executeTask(() ->
                {
                latchEntered.countDown();
                try
                    {
                    if (!latchRelease.await(1, TimeUnit.MINUTES))
                        {
                        throw new AssertionError("Timed out waiting to release channel daemon");
                        }
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                    }
                });
            }

        boolean drainEventExecutor() throws InterruptedException
            {
            CountDownLatch latch = new CountDownLatch(1);
            f_executor.executeTask(latch::countDown);
            return latch.await(1, TimeUnit.MINUTES);
            }

        boolean drainDaemon() throws InterruptedException
            {
            CountDownLatch latch = new CountDownLatch(1);
            f_daemon.executeTask(latch::countDown);
            return latch.await(1, TimeUnit.MINUTES);
            }

        SortedSet<Integer> getOwnedChannelSet()
            {
            SortedSet<Integer> setChannel = new TreeSet<>();
            if (m_aChannelOwned != null)
                {
                for (int nChannel : m_aChannelOwned)
                    {
                    setChannel.add(nChannel);
                    }
                }
            return setChannel;
            }

        @Override
        protected void registerMBean()
            {
            }

        @Override
        protected void unregisterMBean()
            {
            }
        }

    private static class TestContext
        {
        TestContext(SortedSet<Integer> setInitial)
            {
            this(setInitial, SubscriberGroupId.withName("generation-test-group"));
            }

        TestContext(SortedSet<Integer> setInitial, SubscriberGroupId groupId)
            {
            f_topic     = mock(NamedTopic.class);
            f_service   = mock(TopicService.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
            f_connector = mock(SubscriberConnector.class);
            f_groupId   = groupId;

            when(f_topic.getName()).thenReturn("generation-test-topic");
            when(f_topic.getChannelCount()).thenReturn(5);
            when(f_topic.getService()).thenReturn(f_service);
            when(f_topic.getTopicService()).thenReturn(f_service);
            when(f_service.getSerializer()).thenReturn(new DefaultSerializer());
            when(f_service.getCluster().getDependencies().getPublisherCloggedCount()).thenReturn(100);
            when(f_service.isSuspended()).thenReturn(false);

            TopicDependencies dependencies = mock(TopicDependencies.class);
            when(dependencies.getReconnectTimeoutMillis()).thenReturn(1_000L);
            when(dependencies.getReconnectRetryMillis()).thenReturn(1L);
            when(dependencies.getReconnectWaitMillis()).thenAnswer(invocation -> f_cReconnectWaitMillis);

            when(f_connector.getSubscriberId()).thenReturn(new SubscriberId(1, 1, new UUID()));
            when(f_connector.getSubscriberGroupId()).thenReturn(f_groupId);
            when(f_connector.getTopicDependencies()).thenReturn(dependencies);
            when(f_connector.getTypeName()).thenReturn("test");
            when(f_connector.isActive()).thenReturn(true);
            when(f_connector.getConnectionTimestamp()).thenReturn(1L);
            when(f_connector.getOwnedChannels(any())).thenAnswer(invocation ->
                {
                int cCall = f_cOwned.incrementAndGet();
                if (cCall == f_nBlockOwnedCall)
                    {
                    f_latchOwnedEntered.countDown();
                    if (!f_latchAllowOwned.await(1, TimeUnit.MINUTES))
                        {
                        throw new AssertionError("Timed out waiting to read owned channels");
                        }
                    }
                return f_setOwned.get();
                });
            when(f_connector.createChannel(any(), anyInt())).thenAnswer(invocation ->
                    new PagedTopicChannel(invocation.getArgument(1)));
            when(f_connector.receive(any(), anyInt(), any(), anyLong(), anyInt(), any()))
                    .thenReturn(new CompletableFuture<>());

            f_setOwned.set(setInitial);

            doAnswer(invocation ->
                {
                int cAttempt = f_cInitialize.incrementAndGet();
                if (cAttempt == 1 && f_fBlockInitialInitialize)
                    {
                    f_latchInitialInitializeEntered.countDown();
                    if (!f_latchAllowInitialInitialize.await(1, TimeUnit.MINUTES))
                        {
                        throw new AssertionError("Timed out waiting to allow initial connection");
                        }
                    }
                if (cAttempt == 2)
                    {
                    f_latchReconnectEntered.countDown();
                    if (!f_latchAllowReconnect.await(1, TimeUnit.MINUTES))
                        {
                        throw new AssertionError("Timed out waiting to allow reconnect");
                        }
                    }
                if (cAttempt == f_nFailInitializeCall)
                    {
                    throw new TopicException("Expected initialize failure");
                    }
                return new Position[] {Position.EMPTY_POSITION, Position.EMPTY_POSITION,
                        Position.EMPTY_POSITION, Position.EMPTY_POSITION, Position.EMPTY_POSITION};
                }).when(f_connector).initialize(any(), any(Boolean.class), any(Boolean.class), any(Boolean.class));

            doAnswer(invocation ->
                {
                f_listener.set(invocation.getArgument(0));
                return null;
                }).when(f_connector).addListener(any());

            doAnswer(invocation ->
                {
                int cCall = f_cInitialized.incrementAndGet();
                if (cCall == f_nFailOnInitializedCall)
                    {
                    throw new TopicException("Expected post-snapshot failure");
                    }
                return null;
                }).when(f_connector).onInitialized(any());

            doAnswer(invocation ->
                {
                f_subscriber.set((TestSubscriber) invocation.getArgument(0));
                return null;
                }).when(f_connector).postConstruct(any());
            }

        TestSubscriber createSubscriber()
            {
            Subscriber.Option[] options = f_groupId.isAnonymous()
                    ? new Subscriber.Option[] {Subscriber.withListener(f_ownershipListener)}
                    : new Subscriber.Option[] {Subscriber.Name.of("generation-test-group"),
                            Subscriber.withListener(f_ownershipListener)};
            return new TestSubscriber(f_topic, f_connector, options);
            }

        void emit(SubscriberEvent.Type type, SortedSet<Integer> setChannel)
            {
            f_listener.get().onEvent(new SubscriberEvent(f_connector, type, setChannel));
            }

        void blockOwnedChannels(int nCall)
            {
            f_nBlockOwnedCall = nCall;
            }

        final NamedTopic<String>                    f_topic;
        final TopicService                          f_service;
        final SubscriberConnector<String>           f_connector;
        final SubscriberGroupId                     f_groupId;
        final AtomicInteger                         f_cInitialize = new AtomicInteger();
        final AtomicInteger                         f_cInitialized = new AtomicInteger();
        final AtomicInteger                         f_cOwned = new AtomicInteger();
        final AtomicReference<SortedSet<Integer>>   f_setOwned = new AtomicReference<>();
        final AtomicReference<SubscriberListener>   f_listener = new AtomicReference<>();
        final AtomicReference<TestSubscriber>       f_subscriber = new AtomicReference<>();
        final Subscriber.ChannelOwnershipListener   f_ownershipListener = mock(Subscriber.ChannelOwnershipListener.class);
        final CountDownLatch                        f_latchReconnectEntered = new CountDownLatch(1);
        final CountDownLatch                        f_latchAllowReconnect = new CountDownLatch(1);
        final CountDownLatch                        f_latchOwnedEntered = new CountDownLatch(1);
        final CountDownLatch                        f_latchAllowOwned = new CountDownLatch(1);
        final CountDownLatch                        f_latchInitialInitializeEntered = new CountDownLatch(1);
        final CountDownLatch                        f_latchAllowInitialInitialize = new CountDownLatch(1);
        volatile int                               f_nBlockOwnedCall = -1;
        volatile int                               f_nFailInitializeCall = -1;
        volatile int                               f_nFailOnInitializedCall = -1;
        volatile boolean                           f_fBlockInitialInitialize;
        volatile long                              f_cReconnectWaitMillis = 1_000L;
        }
    }
