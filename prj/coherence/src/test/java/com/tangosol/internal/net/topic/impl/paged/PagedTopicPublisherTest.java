/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.internal.net.topic.impl.paged.agent.OfferProcessor;
import com.tangosol.internal.net.topic.impl.paged.agent.OfferProcessor.Result;
import com.tangosol.internal.net.topic.impl.paged.model.Page;
import com.tangosol.internal.net.topic.impl.paged.model.Page.Key;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.partition.KeyPartitioningStrategy;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Publisher.OrderBy;
import com.tangosol.net.topic.Publisher.OrderById;
import com.tangosol.net.topic.Publisher.OrderByValue;

import com.tangosol.util.AsynchronousAgent;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.LongArray;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;
import com.tangosol.util.SparseArray;
import com.tangosol.util.processor.AsynchronousProcessor;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author jk 2015.06.19
 */
public class PagedTopicPublisherTest
    {
    // ----- PagedTopicPublisher tests --------------------------------------

    @BeforeClass
    public static void setup()
        {
        System.setProperty("coherence.topic.publisher.close.timeout", "3s");
        }

    @Test
    public void shouldHaveSpecifiedPublisherCloseTimeout()
        {
        assertThat(PagedTopicPublisher.CLOSE_TIMEOUT_SECS, is(3L));
        }

    @Test
    public void shouldNotAllowNullQueueCaches()
            throws Exception
        {
        m_expectedException.expect(NullPointerException.class);
        m_expectedException.expectMessage(is("The PagedTopicCaches parameter cannot be null"));

        new PagedTopicPublisher<String>(null);
        }

    @Test
    public void shouldHaveNameFromTopic()
            throws Exception
        {
        PagedTopicCaches caches = stubTopicCaches();

        PagedTopicPublisher publisher = new PagedTopicPublisher(caches);

        assertThat(publisher.getName(), is(caches.getTopicName()));
        }

    @Test
    public void shouldBeEqual()
            throws Exception
        {
        PagedTopicCaches caches     = stubTopicCaches();
        PagedTopicPublisher publisher1 = new PagedTopicPublisher(caches);
        PagedTopicPublisher publisher2 = new PagedTopicPublisher(caches);

        assertThat(publisher1.equals(publisher2), is(true));
        }

    @Test
    public void shouldBeActive() throws Exception
        {
        PagedTopicCaches caches = stubTopicCaches();
        BatchingOperationsQueue queue  = mock(BatchingOperationsQueue.class);

        when(queue.isActive()).thenReturn(true);
        when(caches.isActive()).thenReturn(true);

        PagedTopicPublisher publisher = new PagedTopicPublisher(caches, queue);

        assertThat(publisher.isActive(), is(true));
        }

    @Test
    public void shouldNotBeActiveIfBatchinQueueIsInactive() throws Exception
        {
        PagedTopicCaches caches = stubTopicCaches();
        BatchingOperationsQueue queue  = mock(BatchingOperationsQueue.class);

        when(queue.isActive()).thenReturn(false);
        when(caches.isActive()).thenReturn(true);

        PagedTopicPublisher publisher = new PagedTopicPublisher(caches, queue);

        assertThat(publisher.isActive(), is(false));
        }

    @Test
    public void shouldNotAllowAddIfInactive() throws Exception
        {
        PagedTopicCaches caches = stubTopicCaches();
        BatchingOperationsQueue queue  = mock(BatchingOperationsQueue.class);

        when(queue.isActive()).thenReturn(false);
        when(caches.isActive()).thenReturn(false);

        PagedTopicPublisher publisher = new PagedTopicPublisher(caches, queue);

        m_expectedException.expect(IllegalStateException.class);

        publisher.send("Foo");
        }

    @Test
    public void shouldAddToBatchingQueue() throws Exception
        {
        PagedTopicCaches caches    = mockTopicCaches();
        BatchingOperationsQueue<Void> queue     = mockBatchingQueue();
        String                               value     = "Foo";
        Binary                               binary    = toBinary(value);

        PagedTopicPublisher<String> publisher = new PagedTopicPublisher<>(caches, queue);

        publisher.send(value);

        verify(queue).add(binary);
        }

    @Test
    public void shouldClosePublisher() throws Exception
        {
        PagedTopicCaches caches    = mockTopicCaches();
        BatchingOperationsQueue<Void> queue     = mockBatchingQueue();
        CompletableFuture<Void>              future    = CompletableFuture.completedFuture(null);
        PagedTopicPublisher<String> publisher = new PagedTopicPublisher<>(caches, queue);

        when(queue.flush()).thenReturn(future);

        publisher.close();
        when(queue.isActive()).thenReturn(false);

        assertThat(publisher.isActive(), is(false));
        verify(queue, times(caches.getChannelCount())).close();
        verify(queue, times(caches.getChannelCount())).flush();
        }

    @Test
    public void shouldRequestFillCurrentBatchWithCorrectValue() throws Exception
        {
        BatchingOperationsQueue<Void>   queue        = mockBatchingQueue();
        PagedTopicCaches caches       = stubTopicCaches();
        PagedTopicPublisher<String> publisher    = new PagedTopicPublisher<>(caches, queue);
        PagedTopicPublisher<String> spyPublisher = Mockito.spy(publisher);
        CompletableFuture<Void>                future       = CompletableFuture.completedFuture(null);

        doReturn(future).when(spyPublisher).ensurePageId(any(PagedTopicPublisher.Channel.class));
        when(queue.fillCurrentBatch(anyInt())).thenReturn(false);

        spyPublisher.addQueuedElements(publisher.f_aChannel[0], 987);

        verify(queue).fillCurrentBatch(987);
        }

    @Test
    public void shouldNotAddToTopicIfCurrentBatchIsEmpty() throws Exception
        {
        BatchingOperationsQueue<Void>   queue        = mockBatchingQueue();
        PagedTopicCaches caches       = stubTopicCaches();
        PagedTopicPublisher<String> publisher    = new PagedTopicPublisher<>(caches, queue);
        PagedTopicPublisher<String> spyPublisher = Mockito.spy(publisher);
        CompletableFuture<Void>                future       = CompletableFuture.completedFuture(null);

        doReturn(future).when(spyPublisher).ensurePageId(any(PagedTopicPublisher.Channel.class));
        when(queue.fillCurrentBatch(anyInt())).thenReturn(false);

        spyPublisher.addQueuedElements(publisher.f_aChannel[0], 987);

        verify(spyPublisher, never()).ensurePageId(publisher.f_aChannel[0]);
        verify(spyPublisher, never()).addInternal(any(PagedTopicPublisher.Channel.class), anyLong());
        }

    @Test
    public void shouldNotAddToTopicIfEnsureTailCompletesExceptionally() throws Exception
        {
        BatchingOperationsQueue<Void>   queue        = mockBatchingQueue();
        PagedTopicCaches caches       = stubTopicCaches();
        PagedTopicPublisher<String> publisher    = new PagedTopicPublisher<>(caches, queue);
        PagedTopicPublisher<String> spyPublisher = Mockito.spy(publisher);
        Throwable                              throwable    = new RuntimeException("No!");
        CompletableFuture<Void>                future       = new CompletableFuture<>();

        future.completeExceptionally(throwable);

        doReturn(future).when(spyPublisher).ensurePageId(any(PagedTopicPublisher.Channel.class));
        when(queue.fillCurrentBatch(anyInt())).thenReturn(true);

        spyPublisher.addQueuedElements(publisher.f_aChannel[0], 987);

        verify(spyPublisher, never()).addInternal(any(PagedTopicPublisher.Channel.class), anyLong());
        verify(spyPublisher).ensurePageId(publisher.f_aChannel[0]);
        verify(queue).fillCurrentBatch(987);
        }

    @Test
    public void shouldHandleErrorIfEnsureTailCompletesExceptionally() throws Exception
        {
        BatchingOperationsQueue<Void>   queue        = mockBatchingQueue();
        PagedTopicCaches caches       = stubTopicCaches();
        PagedTopicPublisher<String> publisher    = new PagedTopicPublisher<>(caches, queue);
        PagedTopicPublisher<String> spyPublisher = Mockito.spy(publisher);
        Throwable                              throwable    = new RuntimeException("No!");
        CompletableFuture<Void>                future       = new CompletableFuture<>();

        future.completeExceptionally(throwable);

        doReturn(future).when(spyPublisher).ensurePageId(any(PagedTopicPublisher.Channel.class));
        when(queue.fillCurrentBatch(anyInt())).thenReturn(true);

        spyPublisher.addQueuedElements(publisher.f_aChannel[0], 987);

        verify(spyPublisher).ensurePageId(any(PagedTopicPublisher.Channel.class));
        verify(spyPublisher, never()).addInternal(any(PagedTopicPublisher.Channel.class), anyLong());

        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(spyPublisher).handleError(isNull(Void.class), captor.capture());

        Throwable throwableArg = captor.getValue();
        assertThat(throwableArg, is(instanceOf(CompletionException.class)));
        assertThat(throwableArg.getCause(), is(sameInstance(throwable)));
        }

    @Test
    public void shouldAddToPageAndHandleError() throws Exception
        {
        PagedTopicCaches caches = stubTopicCaches();
        BatchingOperationsQueue<Void> queue         = mockBatchingQueue();
        List<Binary>                         listBatch     = Collections.singletonList(Binary.NO_BINARY);

        when(queue.getCurrentBatchValues()).thenReturn(listBatch);


        PagedTopicPublisher<String> publisher    = new PagedTopicPublisher<>(caches, queue);
        PagedTopicPublisher<String> spyPublisher = Mockito.spy(publisher);

        doNothing().when(spyPublisher).handleOfferCompletion(any(Result.class), any(PagedTopicPublisher.Channel.class), anyLong());

        spyPublisher.addInternal(publisher.f_aChannel[0], 19L);

        ArgumentCaptor<AsynchronousProcessor> captorInvocable = ArgumentCaptor.forClass(AsynchronousProcessor.class);
        verify(caches.Pages).invoke(eq(new Key(0, 19L)), captorInvocable.capture());

        AsynchronousAgent                        processor    = captorInvocable.getValue();
        CompletableFuture<Result> futureResult = processor.getCompletableFuture();
        Throwable                                throwable    = new RuntimeException("No!");

        futureResult.completeExceptionally(throwable);

        verify(spyPublisher, never()).handleOfferCompletion(any(Result.class), any(PagedTopicPublisher.Channel.class), anyLong());

        ArgumentCaptor<Throwable> captorError = ArgumentCaptor.forClass(Throwable.class);
        verify(spyPublisher).handleError(isNull(Void.class), captorError.capture());

        Throwable throwableArg = captorError.getValue();
        assertThat(throwableArg, is(sameInstance(throwable)));
        }

    @Test
    public void shouldNotAddToPageIfBatchIsEmpty() throws Exception
        {
        String sTopicName = "TestTopic";
        String sCacheName = PagedTopicCaches.Names.PAGES.cacheNameForTopicName(sTopicName);

        NamedCache<Long, Page>                     cache         = mock(NamedCache.class);
        BiFunction<String,ClassLoader,NamedCache> functionCache = mock(BiFunction.class);
        ClassLoader                               loader        = mock(ClassLoader.class);
        DistributedCacheService                   cacheService  = mock(DistributedCacheService.class);
        BatchingOperationsQueue<Void>             queue         = mockBatchingQueue();
        List<Binary>                              listBatch     = Collections.emptyList();
        ResourceRegistry                          registry      = new SimpleResourceRegistry();

        registry.registerResource(Configuration.class, sTopicName, new Configuration());

        when(cacheService.getResourceRegistry()).thenReturn(registry);
        when(cacheService.getPartitionCount()).thenReturn(257);
        when(cacheService.getContextClassLoader()).thenReturn(loader);
        when(functionCache.apply(anyString(), same(loader))).thenReturn(mock(NamedCache.class));
        when(functionCache.apply(eq(sCacheName), same(loader))).thenReturn(cache);
        when(queue.getCurrentBatchValues()).thenReturn(listBatch);

        PagedTopicCaches            caches       = new PagedTopicCaches(sTopicName, cacheService, functionCache);
        PagedTopicPublisher<String> publisher    = new PagedTopicPublisher<>(caches, queue);
        PagedTopicPublisher<String> spyPublisher = Mockito.spy(publisher);

        doNothing().when(spyPublisher).handleOfferCompletion(any(Result.class), any(PagedTopicPublisher.Channel.class), anyLong());

        spyPublisher.addInternal(publisher.f_aChannel[0], 19L);

        verify(cache, never()).invoke(anyLong(), any(InvocableMap.EntryProcessor.class));
        }

    @Test
    public void shouldHandleOfferCompletionWhenBactchFullyCompleteAndPageNotSealed() throws Exception
        {
        PagedTopicCaches caches       = mockTopicCaches();
        BatchingOperationsQueue<Void>        queue        = mockBatchingQueue();
        long                                 lPage        = 1234L;
        int                                  cCapacity    = 10;
        Result result       = new Result(OfferProcessor.Result.Status.Success, 0, cCapacity);

        PagedTopicPublisher<String> publisher    = new PagedTopicPublisher<>(caches, queue);
        PagedTopicPublisher<String> spyPublisher = Mockito.spy(publisher);

        when(queue.isBatchComplete()).thenReturn(true);
        doReturn(null).when(spyPublisher).handleError(any(Void.class), any(Throwable.class));

        spyPublisher.handleOfferCompletion(result, publisher.f_aChannel[0], lPage);

        verify(spyPublisher).addQueuedElements(publisher.f_aChannel[0], cCapacity);
        }

    @Test
    public void shouldHandleOfferCompletionWhenBactchFullyCompleteAndPageSealed() throws Exception
        {
        PagedTopicCaches caches           = mockTopicCaches();
        BatchingOperationsQueue<Void> queue            = mockBatchingQueue();
        long                                 lPage          = 1234L;
        int                                  cCapacity        = 10;
        OfferProcessor.Result result           = new OfferProcessor.Result(OfferProcessor.Result.Status.PageSealed, 0, cCapacity);
        CompletableFuture<Void>              futureNextPage = new CompletableFuture<>();
        PagedTopicPublisher<String> publisher        = new PagedTopicPublisher<>(caches, queue);
        PagedTopicPublisher<String> spyPublisher     = Mockito.spy(publisher);

        when(queue.isBatchComplete()).thenReturn(true);
        doReturn(null).when(spyPublisher).handleError(any(Void.class), any(Throwable.class));
        doReturn(futureNextPage).when(spyPublisher).moveToNextPage(any(PagedTopicPublisher.Channel.class), anyLong());
        doNothing().when(spyPublisher).addQueuedElements(any(PagedTopicPublisher.Channel.class), anyInt());

        spyPublisher.handleOfferCompletion(result, publisher.f_aChannel[0], lPage);

        verify(spyPublisher).moveToNextPage(publisher.f_aChannel[0], lPage);

        futureNextPage.complete(null);

        verify(spyPublisher).addQueuedElements(publisher.f_aChannel[0], cCapacity);
        }


    @Test
    public void shouldHandleErrorAndClose() throws Exception
        {
        PagedTopicCaches              caches       = mockTopicCaches();
        BatchingOperationsQueue<Void> queue        = mockBatchingQueue();
        Throwable                     throwable    = new RuntimeException("Oops...");
        PagedTopicPublisher<String>   publisher    = new PagedTopicPublisher<>(caches, queue);
        PagedTopicPublisher<String>   publisherSpy = Mockito.spy(publisher);

        doNothing().when(publisherSpy).closeInternal(anyBoolean());

        publisherSpy.handleError(null, throwable);

        verify(queue, times(caches.getChannelCount())).handleError(same(throwable), eq(BatchingOperationsQueue.OnErrorAction.Cancel));

        verify(publisherSpy).closeInternal(false);
        }

    @Test
    public void shouldFlushQueue() throws Exception
        {
        PagedTopicCaches        caches = stubTopicCaches();
        BatchingOperationsQueue queue  = mock(BatchingOperationsQueue.class);
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);

        when(queue.isActive()).thenReturn(true);
        when(caches.isActive()).thenReturn(true);
        when(queue.flush()).thenReturn(future);

        PagedTopicPublisher publisher = new PagedTopicPublisher(caches, queue);

        CompletableFuture futureResult = publisher.flush();

        verify(queue, times(caches.getChannelCount())).flush();
        }

    @Test
    public void shouldHandleIndividualErrorsWhenErrorsArrayIsNull() throws Exception
        {
        PagedTopicCaches        caches  = stubTopicCaches();
        BatchingOperationsQueue queue   = mock(BatchingOperationsQueue.class);
        LongArray<Throwable>    aErrors = null;

        when(queue.isActive()).thenReturn(true);
        when(caches.isActive()).thenReturn(true);

        PagedTopicPublisher publisher = new PagedTopicPublisher(caches, queue);

        publisher.handleIndividualErrors(aErrors);

        assertThat(publisher.isActive(), is(true));
        verify(queue, never()).handleError(any(Throwable.class), any(BatchingOperationsQueue.OnErrorAction.class));
        }

    @Test
    public void shouldHandleIndividualErrorsWhenErrorsArrayIsEmpty() throws Exception
        {
        PagedTopicCaches        caches  = stubTopicCaches();
        BatchingOperationsQueue queue   = mock(BatchingOperationsQueue.class);
        LongArray<Throwable>    aErrors = new SparseArray<>();

        when(queue.isActive()).thenReturn(true);
        when(caches.isActive()).thenReturn(true);

        PagedTopicPublisher publisher = new PagedTopicPublisher(caches, queue);

        publisher.handleIndividualErrors(aErrors);

        assertThat(publisher.isActive(), is(true));
        verify(queue, never()).handleError(any(Throwable.class), any(BatchingOperationsQueue.OnErrorAction.class));
        }

    @Test
    public void shouldHandleIndividualErrorsWhenErrorsArrayHasErrorsAndFailureActionIsDefault() throws Exception
        {
        PagedTopicCaches        caches  = stubTopicCaches();
        BatchingOperationsQueue queue   = mock(BatchingOperationsQueue.class);
        CompletableFuture<Void> future  = CompletableFuture.completedFuture(null);
        LongArray<Throwable>    aErrors = new SparseArray<>();
        Throwable               error   = new RuntimeException("No!");

        aErrors.set(19L, error);

        when(queue.isActive()).thenReturn(true);
        when(queue.flush()).thenReturn(future);
        when(caches.isActive()).thenReturn(true);

        PagedTopicPublisher publisher = new PagedTopicPublisher(caches, queue);

        publisher.handleIndividualErrors(aErrors);


        verify(queue, times(caches.getChannelCount())).close();
        when(queue.isActive()).thenReturn(false);

        assertThat(publisher.isActive(), is(false));
        verify(queue, times(caches.getChannelCount())).handleError(same(error), eq(BatchingOperationsQueue.OnErrorAction.Cancel));
        }

    @Test
    public void shouldHandleIndividualErrorsWhenErrorsArrayHasErrorsAndFailureActionIsStop() throws Exception
        {
        PagedTopicCaches        caches  = stubTopicCaches();
        BatchingOperationsQueue queue   = mock(BatchingOperationsQueue.class);
        CompletableFuture<Void> future  = CompletableFuture.completedFuture(null);
        LongArray<Throwable>    aErrors = new SparseArray<>();
        Throwable               error   = new RuntimeException("No!");

        aErrors.set(19L, error);

        when(queue.isActive()).thenReturn(true);
        when(queue.flush()).thenReturn(future);
        when(caches.isActive()).thenReturn(true);

        PagedTopicPublisher publisher = new PagedTopicPublisher(caches, queue, Publisher.OnFailure.Stop);

        publisher.handleIndividualErrors(aErrors);

        verify(queue, times(caches.getChannelCount())).close();
        when(queue.isActive()).thenReturn(false);

        assertThat(publisher.isActive(), is(false));
        verify(queue, times(caches.getChannelCount())).handleError(same(error), eq(BatchingOperationsQueue.OnErrorAction.Cancel));
        }

    @Test
    public void shouldHandleIndividualErrorsWhenErrorsArrayHasErrorsAndFailureActionIsContinue() throws Exception
        {
        PagedTopicCaches        caches  = stubTopicCaches();
        BatchingOperationsQueue queue   = mock(BatchingOperationsQueue.class);
        CompletableFuture<Void> future  = CompletableFuture.completedFuture(null);
        LongArray<Throwable>    aErrors = new SparseArray<>();
        Throwable               error   = new RuntimeException("No!");

        aErrors.set(19L, error);

        when(queue.isActive()).thenReturn(true);
        when(queue.flush()).thenReturn(future);
        when(caches.isActive()).thenReturn(true);

        PagedTopicPublisher publisher = new PagedTopicPublisher(caches, queue, Publisher.OnFailure.Continue);

        publisher.handleIndividualErrors(aErrors);

        assertThat(publisher.isActive(), is(true));
        verify(queue, never()).handleError(any(Throwable.class), any(BatchingOperationsQueue.OnErrorAction.class));
        }

    @Test
    public void shouldHavePublisherWithDefaultOrderByOption()
        {
        PagedTopicCaches    caches    = stubTopicCaches();
        PagedTopicPublisher publisher = new PagedTopicPublisher(caches);

        // validate that OrderBy.Thread is the default when no Publisher.OrderBy option provided to create Publisher.
        assertThat(publisher.getOrderByOption(), is(OrderBy.thread()));
        }

    @Test
    public void shouldHavePublisherWithOrderByThread()
        {
        PagedTopicCaches    caches    = stubTopicCaches();
        PagedTopicPublisher publisher = new PagedTopicPublisher(caches, OrderBy.thread());

        assertThat(publisher.getOrderByOption(), is(OrderBy.thread()));
        }

    @Test
    public void shouldHavePublisherWithOrderByNone()
        {
        PagedTopicCaches    caches    = stubTopicCaches();
        PagedTopicPublisher publisher = new PagedTopicPublisher(caches, OrderBy.none());

        assertThat(publisher.getOrderByOption(), is(OrderBy.none()));
        }

    @Test
    public void shouldHavePublisherWithOrderById()
        {
        PagedTopicCaches    caches    = stubTopicCaches();
        OrderBy<Object>     option    = OrderBy.id(4);
        PagedTopicPublisher publisher = new PagedTopicPublisher(caches, option);

        assertThat(publisher.getOrderByOption().getOrderId("random-message"), is(option.getOrderId("random-message")));
        assertThat(publisher.getOrderByOption(), is(option));
        }

    @Test
    public void shouldHavePublisherWithOrderByValue()
        {
        ToIntFunction<String> getValue = s ->
            {
            return Integer.parseInt(s);
            };

        PagedTopicCaches    caches    = stubTopicCaches();
        PagedTopicPublisher publisher = new PagedTopicPublisher(caches, OrderBy.value(getValue));

        OrderBy option = publisher.getOrderByOption();
        assertTrue(option instanceof OrderByValue);

        // simulate published messages have string value of "5" and "7".
        assertThat(((OrderByValue)option).getOrderId("5"), is(5));
        assertThat(((OrderByValue)option).getOrderId("7"), is(7));
        }

    // ----- helper methods -------------------------------------------------

    public PagedTopicCaches mockTopicCaches()
        {
        PagedTopicCaches caches = stubTopicCaches();

        when(caches.isActive()).thenReturn(true);
        when(caches.getSerializer()).thenReturn(m_serializer);

        return caches;
        }

    public PagedTopicCaches stubTopicCaches()
        {
        String                                    sTopicName    = "TestTopic";
        DistributedCacheService                   cacheService  = mock(DistributedCacheService.class);
        KeyPartitioningStrategy                   partitioner   = mock(KeyPartitioningStrategy.class);
        ResourceRegistry                          registry      = new SimpleResourceRegistry();
        Map<String,NamedCache>                    mapCaches     = new HashMap<>();
        BiFunction<String,ClassLoader,NamedCache> functionCache = (sName, classLoader) -> mapCaches.get(sName);

        when(cacheService.getResourceRegistry()).thenReturn(registry);
        when(cacheService.getPartitionCount()).thenReturn(257);
        when(cacheService.getKeyPartitioningStrategy()).thenReturn(partitioner);
        when(partitioner.getKeyPartition(anyObject())).thenReturn(0);

        for (PagedTopicCaches.Names name : PagedTopicCaches.Names.values())
            {
            String     sCacheName = name.cacheNameForTopicName(sTopicName);
            NamedCache cache      = mock(NamedCache.class, sCacheName);

            mapCaches.put(sCacheName, cache);
            }

        PagedTopicCaches caches    = new PagedTopicCaches(sTopicName, cacheService, functionCache);
        PagedTopicCaches cachesSpy = spy(caches);

        registry.registerResource(Configuration.class, caches.getTopicName(), new Configuration());

        return cachesSpy;
        }

    @SuppressWarnings("unchecked")
    public <V,F> BatchingOperationsQueue<F> mockBatchingQueue()
        {
        BatchingOperationsQueue queue = mock(BatchingOperationsQueue.class);

        when(queue.isActive()).thenReturn(true);

        return queue;
        }

    public Binary toBinary(Object o)
        {
        return ExternalizableHelper.toBinary(o, m_serializer);
        }

    public <V> V fromBinary(Binary binary)
        {
        return (V) ExternalizableHelper.fromBinary(binary, m_serializer);
        }


    // ----- data members ---------------------------------------------------

    private ConfigurablePofContext m_serializer        = new ConfigurablePofContext("coherence-pof-config.xml");

    @Rule
    public ExpectedException       m_expectedException = ExpectedException.none();
    }
