/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.util.AssertionException;
import com.tangosol.util.Binary;
import com.tangosol.util.LongArray;
import com.tangosol.util.SparseArray;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.mockito.Mockito;

import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

/**
 * @author jk 2015.12.17
 */
public class BatchingOperationsQueueTest
    {

    @Test
    public void shouldBeActiveOnCreation() throws Exception
        {
        BatchingOperationsQueue queue = new BatchingOperationsQueue(functionDummy, 1);

        assertThat(queue.isActive(), is(true));
        }

    @Test
    public void shouldReturnFalseFillingFromEmptyQueue() throws Exception
        {
        BatchingOperationsQueue queue = new BatchingOperationsQueue(functionDummy, 1);

        assertThat(queue.fillCurrentBatch(1), is(false));
        }

    @Test
    public void shouldReturnEmptyBatch() throws Exception
        {
        BatchingOperationsQueue queue     = new BatchingOperationsQueue(functionDummy, 1);
        List          listBatch = queue.getCurrentBatchValues();

        assertThat(listBatch, is(notNullValue()));
        assertThat(listBatch.isEmpty(), is(true));
        }

    @Test
    public void shouldBeCompleteForEmptyQueue() throws Exception
        {
        BatchingOperationsQueue queue = new BatchingOperationsQueue(functionDummy, 1);

        assertThat(queue.isBatchComplete(), is(true));
        }

    @Test
    public void shouldCloseEmptyQueue() throws Exception
        {
        BatchingOperationsQueue queue = new BatchingOperationsQueue(functionDummy, 1);

        queue.close();

        assertThat(queue.isActive(), is(false));
        }

    @Test
    public void shouldNotBaAbleToAddToClosedQueue() throws Exception
        {
        BatchingOperationsQueue<Void> queue = new BatchingOperationsQueue<>(functionDummy, 1);

        queue.close();

        m_expectedException.expect(AssertionException.class);

        queue.add(new Binary());
        assertThat(queue.getCurrentBatch().isEmpty(), is(true));
        assertThat(queue.getPending().isEmpty(), is(true));
        assertThat(queue.getTrigger().get(), is(BatchingOperationsQueue.TRIGGER_OPEN));
        }

    @Test
    public void shouldCompleteTriggerAndCallFunctionOnAdd() throws Exception
        {
        AtomicInteger                        intValue     = new AtomicInteger(-1);
        int                                 cInitial      = 100;
        BatchingOperationsQueue<Void> queue         = new BatchingOperationsQueue<>(intValue::set, cInitial);
        AtomicInteger                        futureTrigger = queue.getTrigger();

        assertThat(futureTrigger.get(), is(BatchingOperationsQueue.TRIGGER_OPEN));

        queue.add(new Binary());

        assertThat(futureTrigger.get(), is(BatchingOperationsQueue.TRIGGER_CLOSED));
        assertThat(intValue.get(), is(cInitial));
        }

    @Test
    public void shouldAddElementsToPendingListInOrder() throws Exception
        {
        BatchingOperationsQueue<Void> queue = new BatchingOperationsQueue<>(functionDummy, 1);

        queue.add(new Binary());
        queue.add(new Binary());
        queue.add(new Binary());
        queue.add(new Binary());

        Deque<BatchingOperationsQueue< Void>.Element> listPending = queue.getPending();

        assertThat(listPending.size(), is(4));

        BatchingOperationsQueue<Void>.Element element1 = listPending.poll();
        BatchingOperationsQueue<Void>.Element element2 = listPending.poll();
        BatchingOperationsQueue<Void>.Element element3 = listPending.poll();
        BatchingOperationsQueue<Void>.Element element4 = listPending.poll();

        assertThat(element1.getValue(), is(new Binary()));
        assertThat(element2.getValue(), is(new Binary()));
        assertThat(element3.getValue(), is(new Binary()));
        assertThat(element4.getValue(), is(new Binary()));
        }


    @Test
    public void shouldReturnUncompletedElementsInBatch() throws Exception
        {
        BatchingOperationsQueue<Void> queue       = new BatchingOperationsQueue<>(functionDummy, 1);
        Queue<BatchingOperationsQueue< Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue< Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue< Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element4    = queue.createElement(new Binary());

        listBatch.add(element1);
        listBatch.add(element2);
        listBatch.add(element3);
        listPending.add(element4);

        element1.getFuture().complete(null);
        element3.getFuture().complete(null);

        List<Binary> list = queue.getCurrentBatchValues();

        assertThat(list, is(notNullValue()));
        assertThat(list.size(), is(1));
        assertThat(list.get(0), is(element2.getValue()));
        }

    @Test
    public void shouldNotBeCompleteIfBatchContainsUncompleteElements() throws Exception
        {
        BatchingOperationsQueue<Void> queue       = new BatchingOperationsQueue<>(functionDummy, 1);
        Queue<BatchingOperationsQueue< Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue< Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue< Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element4    = queue.createElement(new Binary());

        listBatch.add(element1);
        listBatch.add(element2);
        listBatch.add(element3);
        listPending.add(element4);

        element1.getFuture().complete(null);
        element3.getFuture().complete(null);

        assertThat(queue.isBatchComplete(), is(false));
        }

    @Test
    public void shouldNotBeCompleteIfBatchContainsAllCompleteElements() throws Exception
        {
        BatchingOperationsQueue<Void> queue       = new BatchingOperationsQueue<>(functionDummy, 1);
        Queue<BatchingOperationsQueue< Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue< Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue< Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element4    = queue.createElement(new Binary());

        listBatch.add(element1);
        listBatch.add(element2);
        listBatch.add(element3);
        listPending.add(element4);

        element1.getFuture().complete(null);
        element2.getFuture().complete(null);
        element3.getFuture().complete(null);

        assertThat(queue.isBatchComplete(), is(true));
        }

    @Test
    public void shouldBeCompleteIfBatchIsEmpty() throws Exception
        {
        BatchingOperationsQueue<Void> queue     = new BatchingOperationsQueue<>(functionDummy, 1);
        Queue<BatchingOperationsQueue<Void>.Element> listBatch = queue.getCurrentBatch();

        listBatch.clear();

        assertThat(queue.isBatchComplete(), is(true));
        }

    @Test
    public void shouldCompleteZeroElementsInCurrentBatch() throws Exception
        {
        BatchingOperationsQueue<Void> queue       = new BatchingOperationsQueue<>(functionDummy, 1);
        Queue<BatchingOperationsQueue< Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue< Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue< Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element4    = queue.createElement(new Binary());

        listBatch.add(element1);
        listBatch.add(element2);
        listBatch.add(element3);
        listPending.add(element4);

        queue.completeElements(0, null);

        assertThat(element1.isDone(), is(false));
        assertThat(element2.isDone(), is(false));
        assertThat(element3.isDone(), is(false));
        assertThat(element4.isDone(), is(false));

        assertThat(listBatch.size(), is(3));
        }

    @Test
    public void shouldCompleteSpecifiedNumberOfElementsInCurrentBatch() throws Exception
        {
        BatchingOperationsQueue<Void> queue       = new BatchingOperationsQueue<>(functionDummy, 1);
        Queue<BatchingOperationsQueue< Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue< Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue< Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element4    = queue.createElement(new Binary());

        listBatch.add(element1);
        listBatch.add(element2);
        listBatch.add(element3);
        listPending.add(element4);

        queue.completeElements(2, null);

        assertThat(element1.isDone(), is(true));
        assertThat(element2.isDone(), is(true));
        assertThat(element3.isDone(), is(false));
        assertThat(element4.isDone(), is(false));

        assertThat(listBatch.size(), is(1));
        }

    @Test
    public void shouldCompleteAllElementsInCurrentBatch() throws Exception
        {
        BatchingOperationsQueue<Void> queue       = new BatchingOperationsQueue<>(functionDummy, 1);
        Queue<BatchingOperationsQueue< Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue< Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue< Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element4    = queue.createElement(new Binary());

        listBatch.add(element1);
        listBatch.add(element2);
        listBatch.add(element3);
        listPending.add(element4);

        queue.completeElements(3, null);

        assertThat(element1.isDone(), is(true));
        assertThat(element2.isDone(), is(true));
        assertThat(element3.isDone(), is(true));
        assertThat(element4.isDone(), is(false));

        assertThat(listBatch.size(), is(0));
        }

    @Test
    public void shouldCompleteElementsExceptionally() throws Exception
        {
        BatchingOperationsQueue<Void> queue       = new BatchingOperationsQueue<>(functionDummy, 1);
        Queue<BatchingOperationsQueue< Void>.Element> listBatch   = queue.getCurrentBatch();
        BatchingOperationsQueue< Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element3    = queue.createElement(new Binary());
        LongArray<Throwable>                                 aErrors     = new SparseArray<>();
        Throwable                                            error1      = new RuntimeException("1");
        Throwable                                            error3      = new RuntimeException("2");

        listBatch.add(element1);
        listBatch.add(element2);
        listBatch.add(element3);

        aErrors.set(0, error1);
        aErrors.set(2, error3);

        queue.completeElements(3, aErrors);

        assertThat(element1.isDone(), is(true));
        assertThat(element1.getFuture().isCompletedExceptionally(), is(true));
        assertThat(element2.isDone(), is(true));
        assertThat(element2.getFuture().isCompletedExceptionally(), is(false));
        assertThat(element3.isDone(), is(true));
        assertThat(element3.getFuture().isCompletedExceptionally(), is(true));
        }

    @Test
    public void shouldFillCurrentBatchWithElements() throws Exception
        {
        byte ab[] = new byte[1];
        BatchingOperationsQueue<Void> queue       = new BatchingOperationsQueue<>(functionDummy, 1);
        Queue<BatchingOperationsQueue< Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue< Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue< Void>.Element        element1    = queue.createElement(new Binary(ab));
        BatchingOperationsQueue< Void>.Element        element2    = queue.createElement(new Binary(ab));
        BatchingOperationsQueue< Void>.Element        element3    = queue.createElement(new Binary(ab));
        BatchingOperationsQueue< Void>.Element        element4    = queue.createElement(new Binary(ab));

        listPending.add(element1);
        listPending.add(element2);
        listPending.add(element3);
        listPending.add(element4);

        queue.fillCurrentBatch(2);

        assertThat(listBatch.size(), is(2));
        assertThat(listBatch.poll(), is(sameInstance(element1)));
        assertThat(listBatch.poll(), is(sameInstance(element2)));

        assertThat(listPending.size(), is(2));
        assertThat(listPending.poll(), is(sameInstance(element3)));
        assertThat(listPending.poll(), is(sameInstance(element4)));
        }

    @Test
    public void shouldFillCurrentBatchWithZeroNewElementsIfAlreadyFilled() throws Exception
        {
        byte[] ab = new byte[1];
        BatchingOperationsQueue<Void> queue       = new BatchingOperationsQueue<>(functionDummy, 1);
        Queue<BatchingOperationsQueue< Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue< Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue< Void>.Element        element1    = queue.createElement(new Binary(ab));
        BatchingOperationsQueue< Void>.Element        element2    = queue.createElement(new Binary(ab));
        BatchingOperationsQueue< Void>.Element        element3    = queue.createElement(new Binary(ab));
        BatchingOperationsQueue< Void>.Element        element4    = queue.createElement(new Binary(ab));

        listPending.add(element1);
        queue.fillCurrentBatch(1);

        listPending.add(element2);
        listPending.add(element3);
        listPending.add(element4);

        queue.fillCurrentBatch(1);

        assertThat(listBatch.size(), is(1));
        assertThat(listBatch.poll(), is(sameInstance(element1)));

        assertThat(listPending.size(), is(3));
        assertThat(listPending.poll(), is(sameInstance(element2)));
        assertThat(listPending.poll(), is(sameInstance(element3)));
        assertThat(listPending.poll(), is(sameInstance(element4)));
        }

    @Test
    public void shouldFillCurrentBatchWithAllPendingElements() throws Exception
        {
        BatchingOperationsQueue<Void> queue       = new BatchingOperationsQueue<>(functionDummy, 1);
        Queue<BatchingOperationsQueue< Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue< Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue< Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element4    = queue.createElement(new Binary());

        listPending.add(element1);
        listPending.add(element2);
        listPending.add(element3);
        listPending.add(element4);

        queue.fillCurrentBatch(5);

        assertThat(listBatch.size(), is(4));
        assertThat(listBatch.poll(), is(sameInstance(element1)));
        assertThat(listBatch.poll(), is(sameInstance(element2)));
        assertThat(listBatch.poll(), is(sameInstance(element3)));
        assertThat(listBatch.poll(), is(sameInstance(element4)));

        assertThat(listPending.size(), is(0));
        }

    @Test
    public void shouldRetryOnError() throws Exception
        {
        BatchingOperationsQueue<Void> queue       = new BatchingOperationsQueue<>(functionDummy, 1);
        Queue<BatchingOperationsQueue< Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue< Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue< Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element4    = queue.createElement(new Binary());
        Throwable                                  throwable   = new RuntimeException("No!");

        listBatch.add(element1);
        listBatch.add(element2);
        listPending.add(element3);
        listPending.add(element4);

        AtomicInteger trigger = queue.getTrigger();

        assertThat(trigger.get(), is(BatchingOperationsQueue.TRIGGER_OPEN));

        queue.handleError(throwable, BatchingOperationsQueue.OnErrorAction.Retry);

        assertThat(trigger.get(), is(BatchingOperationsQueue.TRIGGER_CLOSED));

        assertThat(listBatch.isEmpty(), is(true));

        assertThat(listPending.size(), is(4));
        assertThat(listPending.poll(), is(sameInstance(element1)));
        assertThat(listPending.poll(), is(sameInstance(element2)));
        assertThat(listPending.poll(), is(sameInstance(element3)));
        assertThat(listPending.poll(), is(sameInstance(element4)));
        }

    @Test
    public void shouldCompleteAllElementsAndCloseOnError() throws Exception
        {
        BatchingOperationsQueue<Void> queue       = new BatchingOperationsQueue<>(functionDummy, 1);
        Queue<BatchingOperationsQueue< Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue< Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue< Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element4    = queue.createElement(new Binary());
        Throwable                                  throwable   = new RuntimeException("No!");

        listBatch.add(element1);
        listBatch.add(element2);
        listPending.add(element3);
        listPending.add(element4);

        AtomicInteger trigger = queue.getTrigger();

        assertThat(trigger.get(), is(BatchingOperationsQueue.TRIGGER_OPEN));

        BatchingOperationsQueue< Void> spyQueue = Mockito.spy(queue);

        spyQueue.handleError(throwable, BatchingOperationsQueue.OnErrorAction.Complete);

        verify(spyQueue).close();

        assertThat(element1.getFuture().isDone(), is(true));
        assertThat(element2.getFuture().isDone(), is(true));
        assertThat(element3.getFuture().isDone(), is(true));
        assertThat(element4.getFuture().isDone(), is(true));
        }

    @Test
    public void shouldCompleteExceptionallyAllElementsAndCloseOnError() throws Exception
        {
        BatchingOperationsQueue<Void> queue       = new BatchingOperationsQueue<>(functionDummy, 1);
        Queue<BatchingOperationsQueue< Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue< Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue< Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element4    = queue.createElement(new Binary());
        Throwable                                  throwable   = new RuntimeException("No!");

        listBatch.add(element1);
        listBatch.add(element2);
        listPending.add(element3);
        listPending.add(element4);

        AtomicInteger trigger = queue.getTrigger();

        assertThat(trigger.get(), is(BatchingOperationsQueue.TRIGGER_OPEN));

        BatchingOperationsQueue< Void> spyQueue = Mockito.spy(queue);

        spyQueue.handleError(throwable, BatchingOperationsQueue.OnErrorAction.CompleteWithException);

        verify(spyQueue).close();

        assertThat(element1.getFuture().isCompletedExceptionally(), is(true));
        assertThat(element2.getFuture().isCompletedExceptionally(), is(true));
        assertThat(element3.getFuture().isCompletedExceptionally(), is(true));
        assertThat(element4.getFuture().isCompletedExceptionally(), is(true));
        }

    @Test
    public void shouldCompleteExceptionallyIfOnErrorFunctionReturnsNull() throws Exception
        {
        BatchingOperationsQueue<Void> queue       = new BatchingOperationsQueue<>(functionDummy, 1);
        Queue<BatchingOperationsQueue< Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue< Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue< Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element4    = queue.createElement(new Binary());
        Throwable                                  throwable   = new RuntimeException("No!");

        listBatch.add(element1);
        listBatch.add(element2);
        listPending.add(element3);
        listPending.add(element4);

        BatchingOperationsQueue< Void> spyQueue = Mockito.spy(queue);

        spyQueue.handleError(throwable, null);

        verify(spyQueue).close();

        assertThat(element1.getFuture().isCompletedExceptionally(), is(true));
        assertThat(element2.getFuture().isCompletedExceptionally(), is(true));
        assertThat(element3.getFuture().isCompletedExceptionally(), is(true));
        assertThat(element4.getFuture().isCompletedExceptionally(), is(true));
        }

    @Test
    public void shouldCancelAllElementsAndCloseOnError() throws Exception
        {
        BatchingOperationsQueue<Void> queue       = new BatchingOperationsQueue<>(functionDummy, 1);
        Queue<BatchingOperationsQueue< Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue< Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue< Void>.Element       element1    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element       element2    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element       element3    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element       element4    = queue.createElement(new Binary());
        Throwable                                 throwable   = new RuntimeException("No!");

        listBatch.add(element1);
        listBatch.add(element2);
        listPending.add(element3);
        listPending.add(element4);

        BatchingOperationsQueue< Void> spyQueue = Mockito.spy(queue);

        spyQueue.handleError(throwable, BatchingOperationsQueue.OnErrorAction.Cancel);

        verify(spyQueue).close();

        assertThat(element1.getFuture().isCancelled(), is(true));
        assertThat(element2.getFuture().isCancelled(), is(true));
        assertThat(element3.getFuture().isCancelled(), is(true));
        assertThat(element4.getFuture().isCancelled(), is(true));
        }

    @Test
    public void shouldCreateElement() throws Exception
        {
        BatchingOperationsQueue<Void> queue   = new BatchingOperationsQueue<>(functionDummy, 1);
        BatchingOperationsQueue< Void>.Element element = queue.createElement(new Binary());

        assertThat(element.getValue(), is(new Binary()));
        assertThat(element.getFuture(), is(notNullValue()));
        }

    @Test
    public void shouldNotCompleteElementOnCreation() throws Exception
        {
        BatchingOperationsQueue<Void> queue   = new BatchingOperationsQueue<>(functionDummy, 1);
        BatchingOperationsQueue< Void>.Element element = queue.createElement(new Binary());

        assertThat(element.isDone(), is(false));
        assertThat(element.getFuture().isDone(), is(false));
        }


    @Test
    public void shouldCompleteElement() throws Exception
        {
        BatchingOperationsQueue<Void> queue   = new BatchingOperationsQueue<>(functionDummy, 1);
        BatchingOperationsQueue< Void>.Element element = queue.createElement(new Binary());

        element.complete();

        assertThat(element.isDone(), is(true));
        assertThat(element.getFuture().isDone(), is(true));
        assertThat(element.getFuture().isCancelled(), is(false));
        assertThat(element.getFuture().isCompletedExceptionally(), is(false));
        }

    @Test
    public void shouldCompleteElementExceptionally() throws Exception
        {
        BatchingOperationsQueue<Void> queue     = new BatchingOperationsQueue<>(functionDummy, 1);
        BatchingOperationsQueue< Void>.Element element   = queue.createElement(new Binary());
        Throwable                           throwable = new RuntimeException("No!");

        element.completeExceptionally(throwable);

        assertThat(element.isDone(), is(true));
        assertThat(element.getFuture().isDone(), is(true));
        assertThat(element.getFuture().isCancelled(), is(false));
        assertThat(element.getFuture().isCompletedExceptionally(), is(true));

        m_expectedException.expect(ExecutionException.class);
        m_expectedException.expectCause(is(sameInstance(throwable)));

        element.getFuture().get();
        }

    @Test
    public void shouldFlushQueue() throws Exception
        {
        BatchingOperationsQueue<Void> queue       = new BatchingOperationsQueue<>(functionDummy, 1);
        Queue<BatchingOperationsQueue< Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue< Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue< Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element4    = queue.createElement(new Binary());

        listBatch.add(element1);
        listBatch.add(element2);
        listPending.add(element3);
        listPending.add(element4);

        CompletableFuture<Void> future = queue.flush();

        assertThat(future.isDone(), is(false));

        element1.complete();
        assertThat(future.isDone(), is(false));

        element2.complete();
        assertThat(future.isDone(), is(false));

        element3.complete();
        assertThat(future.isDone(), is(false));

        element4.complete();
        assertThat(future.isDone(), is(true));
        assertThat(future.isCancelled(), is(false));
        assertThat(future.isCompletedExceptionally(), is(false));
        }

    @Test
    public void shouldFlushQueueWhenFuturesCompleteExceptionally() throws Exception
        {
        BatchingOperationsQueue<Void> queue       = new BatchingOperationsQueue<>(functionDummy, 1);
        Queue<BatchingOperationsQueue< Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue< Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue< Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue< Void>.Element        element4    = queue.createElement(new Binary());

        listBatch.add(element1);
        listBatch.add(element2);
        listPending.add(element3);
        listPending.add(element4);

        CompletableFuture<Void> future = queue.flush();

        assertThat(future.isDone(), is(false));

        element1.completeExceptionally(new RuntimeException("No!"));
        assertThat(future.isDone(), is(false));

        element2.completeExceptionally(new RuntimeException("No!"));
        assertThat(future.isDone(), is(false));

        element3.completeExceptionally(new RuntimeException("No!"));
        assertThat(future.isDone(), is(false));

        element4.completeExceptionally(new RuntimeException("No!"));
        assertThat(future.isDone(), is(true));
        assertThat(future.isCancelled(), is(false));
        assertThat(future.isCompletedExceptionally(), is(false));
        }

    @Rule
    public ExpectedException m_expectedException = ExpectedException.none();

    private static Consumer<Integer> functionDummy = (val) -> {};
    }
