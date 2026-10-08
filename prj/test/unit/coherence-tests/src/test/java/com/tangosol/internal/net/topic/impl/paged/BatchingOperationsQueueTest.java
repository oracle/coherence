/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.net.topic.TopicPublisherException;
import com.tangosol.util.AssertionException;
import com.tangosol.util.Binary;
import com.tangosol.util.LongArray;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.SparseArray;

import org.junit.Test;

import org.mockito.Mockito;

import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.Assert.assertThrows;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author jk 2015.12.17
 */
@SuppressWarnings("rawtypes")
public class BatchingOperationsQueueTest
    {

    @Test
    public void shouldBeActiveOnCreation()
        {
        BatchingOperationsQueue<?, ?> queue = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);

        assertThat(queue.isActive(), is(true));
        }

    @Test
    public void shouldReturnFalseFillingFromEmptyQueue()
        {
        BatchingOperationsQueue<?, ?> queue = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);

        assertThat(queue.fillCurrentBatch(1), is(false));
        }

    @Test
    public void shouldReturnEmptyBatch()
        {
        BatchingOperationsQueue<?, ?> queue     = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        List                          listBatch = queue.getCurrentBatchValues();

        assertThat(listBatch, is(notNullValue()));
        assertThat(listBatch.isEmpty(), is(true));
        }

    @Test
    public void shouldBeCompleteForEmptyQueue()
        {
        BatchingOperationsQueue<?, ?> queue = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);

        assertThat(queue.isBatchComplete(), is(true));
        }

    @Test
    public void shouldCloseEmptyQueue()
        {
        BatchingOperationsQueue<?, ?> queue = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);

        queue.close();

        assertThat(queue.isActive(), is(false));
        }

    @Test
    public void shouldNotBaAbleToAddToClosedQueue()
        {
        BatchingOperationsQueue<Binary, Void> queue = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);

        queue.close();

        assertThrows(IllegalStateException.class, () -> queue.add(new Binary()));
        assertThat(queue.getCurrentBatch().isEmpty(), is(true));
        assertThat(queue.getPending().isEmpty(), is(true));
        assertThat(queue.getTrigger().get(), is(BatchingOperationsQueue.TRIGGER_OPEN));
        }

    @Test
    public void shouldCompleteTriggerAndCallFunctionOnAdd()
        {
        AtomicInteger                         intValue      = new AtomicInteger(-1);
        int                                   cInitial      = 100;
        BatchingOperationsQueue<Binary, Void> queue         = new BatchingOperationsQueue<>(intValue::set, cInitial);
        AtomicInteger                         futureTrigger = queue.getTrigger();

        assertThat(futureTrigger.get(), is(BatchingOperationsQueue.TRIGGER_OPEN));

        queue.add(new Binary());

        assertThat(futureTrigger.get(), is(BatchingOperationsQueue.TRIGGER_CLOSED));
        assertThat(intValue.get(), is(cInitial));
        }

    @Test
    public void shouldRearmCurrentBatchWithoutMovingOrCompletingElements()
        {
        AtomicInteger                         cTrigger = new AtomicInteger();
        BatchingOperationsQueue<Binary, Void> queue    = new BatchingOperationsQueue<>(ignored -> cTrigger.incrementAndGet(), 1);

        CompletableFuture<Void> future = queue.add(new Binary());
        queue.fillCurrentBatch(1);
        long lOperation = queue.getOperationSequence();

        assertThat(queue.rearmCurrentBatch(lOperation), is(BatchingOperationsQueue.RearmResult.STARTED));
        assertThat(cTrigger.get(), is(2));
        assertThat(queue.getOperationSequence(), is(lOperation + 1));
        assertThat(queue.getCurrentBatchSize(), is(1));
        assertThat(queue.getPendingSize(), is(0));
        assertThat(future.isDone(), is(false));
        assertThat(queue.getTrigger().get(), is(BatchingOperationsQueue.TRIGGER_CLOSED));
        }

    @Test
    public void shouldOnlyRearmEachFailedOperationOnce()
        {
        AtomicInteger                         cTrigger = new AtomicInteger();
        BatchingOperationsQueue<Binary, Void> queue    = new BatchingOperationsQueue<>(ignored -> cTrigger.incrementAndGet(), 1);

        CompletableFuture<Void> future = queue.add(new Binary());
        queue.fillCurrentBatch(1);
        long lFirst = queue.getOperationSequence();

        assertThat(queue.rearmCurrentBatch(lFirst), is(BatchingOperationsQueue.RearmResult.STARTED));
        assertThat(queue.rearmCurrentBatch(lFirst), is(BatchingOperationsQueue.RearmResult.ALREADY_STARTED));
        assertThat(cTrigger.get(), is(2));

        long lReplacement = queue.getOperationSequence();
        assertThat(queue.rearmCurrentBatch(lReplacement), is(BatchingOperationsQueue.RearmResult.STARTED));
        assertThat(cTrigger.get(), is(3));
        assertThat(queue.getCurrentBatchSize(), is(1));
        assertThat(queue.getPendingSize(), is(0));
        assertThat(future.isDone(), is(false));
        }

    @Test
    public void shouldDeferRearmWhilePausedAndStartAfterReset()
        {
        AtomicInteger                         cTrigger = new AtomicInteger();
        BatchingOperationsQueue<Binary, Void> queue    = new BatchingOperationsQueue<>(ignored -> cTrigger.incrementAndGet(), 1);

        CompletableFuture<Void> future = queue.add(new Binary());
        queue.fillCurrentBatch(1);
        long lOperation = queue.getOperationSequence();
        queue.pause();

        assertThat(queue.rearmCurrentBatch(lOperation), is(BatchingOperationsQueue.RearmResult.DEFERRED));
        assertThat(cTrigger.get(), is(1));
        assertThat(queue.getCurrentBatchSize(), is(1));
        assertThat(future.isDone(), is(false));

        queue.resetTrigger();
        assertThat(queue.rearmCurrentBatch(lOperation), is(BatchingOperationsQueue.RearmResult.STARTED));
        assertThat(cTrigger.get(), is(2));
        assertThat(queue.getCurrentBatchSize(), is(1));
        assertThat(queue.getPendingSize(), is(0));
        assertThat(future.isDone(), is(false));
        }

    @Test
    public void shouldStartRearmFromOpenTrigger()
        {
        AtomicInteger                         cTrigger = new AtomicInteger();
        BatchingOperationsQueue<Binary, Void> queue    = new BatchingOperationsQueue<>(ignored -> cTrigger.incrementAndGet(), 1);

        CompletableFuture<Void> future = queue.add(new Binary());
        queue.fillCurrentBatch(1);
        long lOperation = queue.getOperationSequence();
        queue.resetTrigger();

        assertThat(queue.rearmCurrentBatch(lOperation), is(BatchingOperationsQueue.RearmResult.STARTED));
        assertThat(cTrigger.get(), is(2));
        assertThat(queue.getCurrentBatchSize(), is(1));
        assertThat(queue.getPendingSize(), is(0));
        assertThat(future.isDone(), is(false));
        }

    @Test
    public void shouldNotRearmAnEmptyCurrentBatch()
        {
        AtomicInteger                         cTrigger = new AtomicInteger();
        BatchingOperationsQueue<Binary, Void> queue    = new BatchingOperationsQueue<>(ignored -> cTrigger.incrementAndGet(), 1);

        assertThat(queue.rearmCurrentBatch(queue.getOperationSequence()),
                is(BatchingOperationsQueue.RearmResult.NO_CURRENT_BATCH));
        assertThat(cTrigger.get(), is(0));
        assertThat(queue.getCurrentBatchSize(), is(0));
        assertThat(queue.getPendingSize(), is(0));
        }

    @Test
    public void shouldRejectFutureOperationIdentifier()
        {
        AtomicInteger                         cTrigger = new AtomicInteger();
        BatchingOperationsQueue<Binary, Void> queue    = new BatchingOperationsQueue<>(ignored -> cTrigger.incrementAndGet(), 1);

        CompletableFuture<Void> future = queue.add(new Binary());
        queue.fillCurrentBatch(1);
        long lOperation = queue.getOperationSequence();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> queue.rearmCurrentBatch(lOperation + 1));

        assertThat(error.getMessage(), is("Unknown failed operation " + (lOperation + 1)
                + ", current operation is " + lOperation));
        assertThat(cTrigger.get(), is(1));
        assertThat(queue.getOperationSequence(), is(lOperation));
        assertThat(queue.getCurrentBatchSize(), is(1));
        assertThat(queue.getPendingSize(), is(0));
        assertThat(future.isDone(), is(false));
        }

    @Test
    public void shouldStartOneRearmForConcurrentCallers() throws Exception
        {
        ExecutorService                       executor = Executors.newFixedThreadPool(2);

        try
            {
            for (int i = 0; i < 100; i++)
                {
                AtomicInteger                         cTrigger = new AtomicInteger();
                BatchingOperationsQueue<Binary, Void> queue    = new BatchingOperationsQueue<>(ignored -> cTrigger.incrementAndGet(), 1);
                CompletableFuture<Void>               future   = queue.add(new Binary());
                CountDownLatch                        ready    = new CountDownLatch(2);
                CountDownLatch                        start    = new CountDownLatch(1);

                queue.fillCurrentBatch(1);
                long lOperation = queue.getOperationSequence();
                CompletableFuture<BatchingOperationsQueue.RearmResult> first = CompletableFuture.supplyAsync(() ->
                    {
                    ready.countDown();
                    await(start);
                    return queue.rearmCurrentBatch(lOperation);
                    }, executor);
                CompletableFuture<BatchingOperationsQueue.RearmResult> second = CompletableFuture.supplyAsync(() ->
                    {
                    ready.countDown();
                    await(start);
                    return queue.rearmCurrentBatch(lOperation);
                    }, executor);

                assertThat(ready.await(1, TimeUnit.MINUTES), is(true));
                start.countDown();
                BatchingOperationsQueue.RearmResult resultOne = first.get(1, TimeUnit.MINUTES);
                BatchingOperationsQueue.RearmResult resultTwo = second.get(1, TimeUnit.MINUTES);

                assertThat(resultOne == BatchingOperationsQueue.RearmResult.STARTED
                        || resultTwo == BatchingOperationsQueue.RearmResult.STARTED, is(true));
                assertThat(resultOne == BatchingOperationsQueue.RearmResult.ALREADY_STARTED
                        || resultTwo == BatchingOperationsQueue.RearmResult.ALREADY_STARTED, is(true));
                assertThat(cTrigger.get(), is(2));
                assertThat(queue.getCurrentBatchSize(), is(1));
                assertThat(queue.getPendingSize(), is(0));
                assertThat(future.isDone(), is(false));
                }
            }
        finally
            {
            executor.shutdownNow();
            }
        }

    private static void await(CountDownLatch latch)
        {
        try
            {
            assertThat(latch.await(1, TimeUnit.MINUTES), is(true));
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
            }
        }

    @Test
    public void shouldAddElementsToPendingListInOrder()
        {
        BatchingOperationsQueue<Binary, Void> queue = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);

        queue.add(new Binary());
        queue.add(new Binary());
        queue.add(new Binary());
        queue.add(new Binary());

        Deque<BatchingOperationsQueue<Binary, Void>.Element> listPending = queue.getPending();

        assertThat(listPending.size(), is(4));

        BatchingOperationsQueue<Binary, Void>.Element element1 = listPending.poll();
        BatchingOperationsQueue<Binary, Void>.Element element2 = listPending.poll();
        BatchingOperationsQueue<Binary, Void>.Element element3 = listPending.poll();
        BatchingOperationsQueue<Binary, Void>.Element element4 = listPending.poll();

        assertThat(element1, is(notNullValue()));
        assertThat(element1.getValue(), is(new Binary()));
        assertThat(element2, is(notNullValue()));
        assertThat(element2.getValue(), is(new Binary()));
        assertThat(element3, is(notNullValue()));
        assertThat(element3.getValue(), is(new Binary()));
        assertThat(element4, is(notNullValue()));
        assertThat(element4.getValue(), is(new Binary()));
        }


    @Test
    public void shouldReturnUncompletedElementsInBatch()
        {
        BatchingOperationsQueue<Binary, Void>                queue       = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        Queue<BatchingOperationsQueue<Binary, Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue<Binary, Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue<Binary, Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element4    = queue.createElement(new Binary());

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
    public void shouldNotBeCompleteIfBatchContainsUncompleteElements()
        {
        BatchingOperationsQueue<Binary, Void>                queue       = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        Queue<BatchingOperationsQueue<Binary, Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue<Binary, Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue<Binary, Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element4    = queue.createElement(new Binary());

        listBatch.add(element1);
        listBatch.add(element2);
        listBatch.add(element3);
        listPending.add(element4);

        element1.getFuture().complete(null);
        element3.getFuture().complete(null);

        assertThat(queue.isBatchComplete(), is(false));
        }

    @Test
    public void shouldNotBeCompleteIfBatchContainsAllCompleteElements()
        {
        BatchingOperationsQueue<Binary, Void>                queue       = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        Queue<BatchingOperationsQueue<Binary, Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue<Binary, Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue<Binary, Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element4    = queue.createElement(new Binary());

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
    public void shouldBeCompleteIfBatchIsEmpty()
        {
        BatchingOperationsQueue<Binary, Void>                queue     = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        Queue<BatchingOperationsQueue<Binary, Void>.Element> listBatch = queue.getCurrentBatch();

        listBatch.clear();

        assertThat(queue.isBatchComplete(), is(true));
        }

    @Test
    public void shouldCompleteZeroElementsInCurrentBatch()
        {
        BatchingOperationsQueue<Binary, Void>                queue       = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        Queue<BatchingOperationsQueue<Binary, Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue<Binary, Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue<Binary, Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element4    = queue.createElement(new Binary());

        listBatch.add(element1);
        listBatch.add(element2);
        listBatch.add(element3);
        listPending.add(element4);

        queue.completeElements(0, null, NullImplementation.getLongArray(), TopicPublisherException.createFactory(null), null);

        assertThat(element1.isDone(), is(false));
        assertThat(element2.isDone(), is(false));
        assertThat(element3.isDone(), is(false));
        assertThat(element4.isDone(), is(false));

        assertThat(listBatch.size(), is(3));
        }

    @Test
    public void shouldCompleteSpecifiedNumberOfElementsInCurrentBatch()
        {
        BatchingOperationsQueue<Binary, Void>                queue       = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        Queue<BatchingOperationsQueue<Binary, Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue<Binary, Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue<Binary, Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element4    = queue.createElement(new Binary());

        listBatch.add(element1);
        listBatch.add(element2);
        listBatch.add(element3);
        listPending.add(element4);

        queue.completeElements(2, null, NullImplementation.getLongArray(), TopicPublisherException.createFactory(null), null);

        assertThat(element1.isDone(), is(true));
        assertThat(element2.isDone(), is(true));
        assertThat(element3.isDone(), is(false));
        assertThat(element4.isDone(), is(false));

        assertThat(listBatch.size(), is(1));
        }

    @Test
    public void shouldCompleteAllElementsInCurrentBatch()
        {
        BatchingOperationsQueue<Binary, Void>                queue       = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        Queue<BatchingOperationsQueue<Binary, Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue<Binary, Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue<Binary, Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element4    = queue.createElement(new Binary());

        listBatch.add(element1);
        listBatch.add(element2);
        listBatch.add(element3);
        listPending.add(element4);

        queue.completeElements(3, null, NullImplementation.getLongArray(), TopicPublisherException.createFactory(null), null);

        assertThat(element1.isDone(), is(true));
        assertThat(element2.isDone(), is(true));
        assertThat(element3.isDone(), is(true));
        assertThat(element4.isDone(), is(false));

        assertThat(listBatch.size(), is(0));
        }

    @Test
    public void shouldCompleteElementsExceptionally()
        {
        BatchingOperationsQueue<Binary, Void>                queue     = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        Queue<BatchingOperationsQueue<Binary, Void>.Element> listBatch = queue.getCurrentBatch();
        BatchingOperationsQueue<Binary, Void>.Element        element1  = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element2  = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element3  = queue.createElement(new Binary());
        LongArray<Throwable>                                 aErrors   = new SparseArray<>();
        Throwable                                            error1    = new RuntimeException("1");
        Throwable                                            error3    = new RuntimeException("2");

        listBatch.add(element1);
        listBatch.add(element2);
        listBatch.add(element3);

        aErrors.set(0, error1);
        aErrors.set(2, error3);

        queue.completeElements(3, aErrors, NullImplementation.getLongArray(), TopicPublisherException.createFactory(null), null);

        assertThat(element1.isDone(), is(true));
        assertThat(element1.getFuture().isCompletedExceptionally(), is(true));
        assertThat(element2.isDone(), is(true));
        assertThat(element2.getFuture().isCompletedExceptionally(), is(false));
        assertThat(element3.isDone(), is(true));
        assertThat(element3.getFuture().isCompletedExceptionally(), is(true));
        }

    @Test
    public void shouldFillCurrentBatchWithElements()
        {
        byte[]                                               ab          = new byte[1];
        BatchingOperationsQueue<Binary, Void>                queue       = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        Queue<BatchingOperationsQueue<Binary, Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue<Binary, Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue<Binary, Void>.Element        element1    = queue.createElement(new Binary(ab));
        BatchingOperationsQueue<Binary, Void>.Element        element2    = queue.createElement(new Binary(ab));
        BatchingOperationsQueue<Binary, Void>.Element        element3    = queue.createElement(new Binary(ab));
        BatchingOperationsQueue<Binary, Void>.Element        element4    = queue.createElement(new Binary(ab));

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
    public void shouldFillCurrentBatchWithZeroNewElementsIfAlreadyFilled()
        {
        byte[] ab = new byte[1];
        BatchingOperationsQueue<Binary, Void>                queue       = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        Queue<BatchingOperationsQueue<Binary, Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue<Binary, Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue<Binary, Void>.Element        element1    = queue.createElement(new Binary(ab));
        BatchingOperationsQueue<Binary, Void>.Element        element2    = queue.createElement(new Binary(ab));
        BatchingOperationsQueue<Binary, Void>.Element        element3    = queue.createElement(new Binary(ab));
        BatchingOperationsQueue<Binary, Void>.Element        element4    = queue.createElement(new Binary(ab));

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
    public void shouldFillCurrentBatchWithAllPendingElements()
        {
        BatchingOperationsQueue<Binary, Void>                queue       = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        Queue<BatchingOperationsQueue<Binary, Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue<Binary, Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue<Binary, Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element4    = queue.createElement(new Binary());

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
    public void shouldRetryOnError()
        {
        BatchingOperationsQueue<Binary, Void>                queue       = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        Queue<BatchingOperationsQueue<Binary, Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue<Binary, Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue<Binary, Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element4    = queue.createElement(new Binary());
        Throwable                                            throwable   = new RuntimeException("No!");

        listBatch.add(element1);
        listBatch.add(element2);
        listPending.add(element3);
        listPending.add(element4);

        AtomicInteger trigger = queue.getTrigger();

        assertThat(trigger.get(), is(BatchingOperationsQueue.TRIGGER_OPEN));

        queue.handleError((bin, err) -> throwable, BatchingOperationsQueue.OnErrorAction.Retry);

        assertThat(trigger.get(), is(BatchingOperationsQueue.TRIGGER_CLOSED));

        assertThat(listBatch.isEmpty(), is(true));

        assertThat(listPending.size(), is(4));
        assertThat(listPending.poll(), is(sameInstance(element1)));
        assertThat(listPending.poll(), is(sameInstance(element2)));
        assertThat(listPending.poll(), is(sameInstance(element3)));
        assertThat(listPending.poll(), is(sameInstance(element4)));
        }

    @Test
    public void shouldCompleteAllElementsWithoutCloseOnError()
        {
        BatchingOperationsQueue<Binary, Void>                queue       = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        Queue<BatchingOperationsQueue<Binary, Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue<Binary, Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue<Binary, Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element4    = queue.createElement(new Binary());
        Throwable                                            throwable   = new RuntimeException("No!");

        listBatch.add(element1);
        listBatch.add(element2);
        listPending.add(element3);
        listPending.add(element4);

        AtomicInteger trigger = queue.getTrigger();

        assertThat(trigger.get(), is(BatchingOperationsQueue.TRIGGER_OPEN));

        BatchingOperationsQueue<Binary, Void> spyQueue = Mockito.spy(queue);

        spyQueue.handleError((bin, err) -> throwable, BatchingOperationsQueue.OnErrorAction.Complete);

        verify(spyQueue, never()).close();

        assertThat(element1.getFuture().isDone(), is(true));
        assertThat(element2.getFuture().isDone(), is(true));
        assertThat(element3.getFuture().isDone(), is(true));
        assertThat(element4.getFuture().isDone(), is(true));
        }

    @Test
    public void shouldCompleteAllElementsAndCloseOnError()
        {
        BatchingOperationsQueue<Binary, Void>                queue       = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        Queue<BatchingOperationsQueue<Binary, Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue<Binary, Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue<Binary, Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element4    = queue.createElement(new Binary());
        Throwable                                            throwable   = new RuntimeException("No!");

        listBatch.add(element1);
        listBatch.add(element2);
        listPending.add(element3);
        listPending.add(element4);

        AtomicInteger trigger = queue.getTrigger();

        assertThat(trigger.get(), is(BatchingOperationsQueue.TRIGGER_OPEN));

        BatchingOperationsQueue<Binary, Void> spyQueue = Mockito.spy(queue);

        spyQueue.handleError((bin, err) -> throwable, BatchingOperationsQueue.OnErrorAction.CompleteAndClose);

        verify(spyQueue).close();

        assertThat(element1.getFuture().isDone(), is(true));
        assertThat(element2.getFuture().isDone(), is(true));
        assertThat(element3.getFuture().isDone(), is(true));
        assertThat(element4.getFuture().isDone(), is(true));
        }

    @Test
    public void shouldCompleteExceptionallyAllElementsWithoutCloseOnError()
        {
        BatchingOperationsQueue<Binary, Void>                queue       = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        Queue<BatchingOperationsQueue<Binary, Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue<Binary, Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue<Binary, Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element4    = queue.createElement(new Binary());
        Throwable                                            throwable   = new RuntimeException("No!");

        listBatch.add(element1);
        listBatch.add(element2);
        listPending.add(element3);
        listPending.add(element4);

        AtomicInteger trigger = queue.getTrigger();

        assertThat(trigger.get(), is(BatchingOperationsQueue.TRIGGER_OPEN));

        BatchingOperationsQueue<Binary, Void> spyQueue = Mockito.spy(queue);

        spyQueue.handleError((bin, err) -> throwable, BatchingOperationsQueue.OnErrorAction.CompleteWithException);

        verify(spyQueue, never()).close();

        assertThat(element1.getFuture().isCompletedExceptionally(), is(true));
        assertThat(element2.getFuture().isCompletedExceptionally(), is(true));
        assertThat(element3.getFuture().isCompletedExceptionally(), is(true));
        assertThat(element4.getFuture().isCompletedExceptionally(), is(true));
        }

    @Test
    public void shouldCompleteExceptionallyAllElementsAndCloseOnError()
        {
        BatchingOperationsQueue<Binary, Void>                queue       = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        Queue<BatchingOperationsQueue<Binary, Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue<Binary, Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue<Binary, Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element4    = queue.createElement(new Binary());
        Throwable                                            throwable   = new RuntimeException("No!");

        listBatch.add(element1);
        listBatch.add(element2);
        listPending.add(element3);
        listPending.add(element4);

        AtomicInteger trigger = queue.getTrigger();

        assertThat(trigger.get(), is(BatchingOperationsQueue.TRIGGER_OPEN));

        BatchingOperationsQueue<Binary, Void> spyQueue = Mockito.spy(queue);

        spyQueue.handleError((bin, err) -> throwable, BatchingOperationsQueue.OnErrorAction.CompleteWithExceptionAndClose);

        verify(spyQueue).close();

        assertThat(element1.getFuture().isCompletedExceptionally(), is(true));
        assertThat(element2.getFuture().isCompletedExceptionally(), is(true));
        assertThat(element3.getFuture().isCompletedExceptionally(), is(true));
        assertThat(element4.getFuture().isCompletedExceptionally(), is(true));
        }

    @Test
    public void shouldCompleteExceptionallyIfOnErrorFunctionReturnsNull()
        {
        BatchingOperationsQueue<Binary, Void>                queue       = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        Queue<BatchingOperationsQueue<Binary, Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue<Binary, Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue<Binary, Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element4    = queue.createElement(new Binary());
        Throwable                                            throwable   = new RuntimeException("No!");

        listBatch.add(element1);
        listBatch.add(element2);
        listPending.add(element3);
        listPending.add(element4);

        BatchingOperationsQueue<Binary, Void> spyQueue = Mockito.spy(queue);

        spyQueue.handleError((bin, err) -> throwable, null);

        verify(spyQueue, never()).close();

        assertThat(element1.getFuture().isCompletedExceptionally(), is(true));
        assertThat(element2.getFuture().isCompletedExceptionally(), is(true));
        assertThat(element3.getFuture().isCompletedExceptionally(), is(true));
        assertThat(element4.getFuture().isCompletedExceptionally(), is(true));
        }

    @Test
    public void shouldCancelAllElementsWithoutCloseOnError()
        {
        BatchingOperationsQueue<Binary, Void>                queue       = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        Queue<BatchingOperationsQueue<Binary, Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue<Binary, Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue<Binary, Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element4    = queue.createElement(new Binary());
        Throwable                                            throwable   = new RuntimeException("No!");

        listBatch.add(element1);
        listBatch.add(element2);
        listPending.add(element3);
        listPending.add(element4);

        BatchingOperationsQueue<Binary, Void> spyQueue = Mockito.spy(queue);

        spyQueue.handleError((bin, err) -> throwable, BatchingOperationsQueue.OnErrorAction.Cancel);

        verify(spyQueue, never()).close();

        assertThat(element1.getFuture().isCompletedExceptionally(), is(true));
        assertThat(element2.getFuture().isCompletedExceptionally(), is(true));
        assertThat(element3.getFuture().isCompletedExceptionally(), is(true));
        assertThat(element4.getFuture().isCompletedExceptionally(), is(true));
        }

    @Test
    public void shouldCancelAllElementsAndCloseOnError()
        {
        BatchingOperationsQueue<Binary, Void>                queue       = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        Queue<BatchingOperationsQueue<Binary, Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue<Binary, Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue<Binary, Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element4    = queue.createElement(new Binary());
        Throwable                                            throwable   = new RuntimeException("No!");

        listBatch.add(element1);
        listBatch.add(element2);
        listPending.add(element3);
        listPending.add(element4);

        BatchingOperationsQueue<Binary, Void> spyQueue = Mockito.spy(queue);

        spyQueue.handleError((bin, err) -> throwable, BatchingOperationsQueue.OnErrorAction.CancelAndClose);

        verify(spyQueue).close();

        assertThat(element1.getFuture().isCompletedExceptionally(), is(true));
        assertThat(element2.getFuture().isCompletedExceptionally(), is(true));
        assertThat(element3.getFuture().isCompletedExceptionally(), is(true));
        assertThat(element4.getFuture().isCompletedExceptionally(), is(true));
        }

    @Test
    public void shouldCreateElement()
        {
        BatchingOperationsQueue<Binary, Void>         queue   = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        BatchingOperationsQueue<Binary, Void>.Element element = queue.createElement(new Binary());

        assertThat(element.getValue(), is(new Binary()));
        assertThat(element.getFuture(), is(notNullValue()));
        }

    @Test
    public void shouldNotCompleteElementOnCreation()
        {
        BatchingOperationsQueue<Binary, Void>         queue   = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        BatchingOperationsQueue<Binary, Void>.Element element = queue.createElement(new Binary());

        assertThat(element.isDone(), is(false));
        assertThat(element.getFuture().isDone(), is(false));
        }


    @Test
    public void shouldCompleteElement()
        {
        BatchingOperationsQueue<Binary, Void>         queue   = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        BatchingOperationsQueue<Binary, Void>.Element element = queue.createElement(new Binary());

        element.complete(null, null);

        assertThat(element.isDone(), is(true));
        assertThat(element.getFuture().isDone(), is(true));
        assertThat(element.getFuture().isCancelled(), is(false));
        assertThat(element.getFuture().isCompletedExceptionally(), is(false));
        }

    @Test
    public void shouldCompleteElementExceptionally()
        {
        BatchingOperationsQueue<Binary, Void>         queue     = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        BatchingOperationsQueue<Binary, Void>.Element element   = queue.createElement(new Binary());
        Throwable                                     throwable = new RuntimeException("No!");

        element.completeExceptionally(throwable, TopicPublisherException.createFactory(null));

        assertThat(element.isDone(), is(true));
        assertThat(element.getFuture().isDone(), is(true));
        assertThat(element.getFuture().isCancelled(), is(false));
        assertThat(element.getFuture().isCompletedExceptionally(), is(true));

        assertThrows(ExecutionException.class, () -> element.getFuture().get(1, TimeUnit.MINUTES));
        }

    @Test
    public void shouldFlushQueue()
        {
        BatchingOperationsQueue<Binary, Void>                queue       = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        Queue<BatchingOperationsQueue<Binary, Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue<Binary, Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue<Binary, Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element4    = queue.createElement(new Binary());

        listBatch.add(element1);
        listBatch.add(element2);
        listPending.add(element3);
        listPending.add(element4);

        CompletableFuture<Void> future = queue.flush();

        assertThat(future.isDone(), is(false));

        element1.complete(null, null);
        assertThat(future.isDone(), is(false));

        element2.complete(null, null);
        assertThat(future.isDone(), is(false));

        element3.complete(null, null);
        assertThat(future.isDone(), is(false));

        element4.complete(null, null);
        assertThat(future.isDone(), is(true));
        assertThat(future.isCancelled(), is(false));
        assertThat(future.isCompletedExceptionally(), is(false));
        }

    @Test
    public void shouldFlushQueueWhenFuturesCompleteExceptionally()
        {
        BatchingOperationsQueue<Binary, Void>                queue       = new BatchingOperationsQueue<>(FUNCTION_DUMMY, 1);
        Queue<BatchingOperationsQueue<Binary, Void>.Element> listBatch   = queue.getCurrentBatch();
        Deque<BatchingOperationsQueue<Binary, Void>.Element> listPending = queue.getPending();
        BatchingOperationsQueue<Binary, Void>.Element        element1    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element2    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element3    = queue.createElement(new Binary());
        BatchingOperationsQueue<Binary, Void>.Element        element4    = queue.createElement(new Binary());

        listBatch.add(element1);
        listBatch.add(element2);
        listPending.add(element3);
        listPending.add(element4);

        CompletableFuture<Void> future = queue.flush();

        assertThat(future.isDone(), is(false));

        element1.completeExceptionally(new RuntimeException("No!"), TopicPublisherException.createFactory(null));
        assertThat(future.isDone(), is(false));

        element2.completeExceptionally(new RuntimeException("No!"), TopicPublisherException.createFactory(null));
        assertThat(future.isDone(), is(false));

        element3.completeExceptionally(new RuntimeException("No!"), TopicPublisherException.createFactory(null));
        assertThat(future.isDone(), is(false));

        element4.completeExceptionally(new RuntimeException("No!"), TopicPublisherException.createFactory(null));
        assertThat(future.isDone(), is(true));
        assertThat(future.isCancelled(), is(false));
        assertThat(future.isCompletedExceptionally(), is(false));
        }

    private static final Consumer<Integer> FUNCTION_DUMMY = (val) -> {};
    }
