/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.base.NonBlocking;

import com.tangosol.internal.net.DebouncedFlowControl;

import com.tangosol.internal.util.DaemonPool;

import com.tangosol.util.Gate;
import com.tangosol.util.LongArray;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.TaskDaemon;
import com.tangosol.util.ThreadGateLite;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link BatchingOperationsQueue} is a queue that values can be added to for later processing.
 * When values are added a function is triggered to process a batch of values.
 * <p>
 * When values are added to the queue the {@link Consumer} function will be called with a batch size.
 *
 * @param <V>  the type of value added to the queue
 * @param <R>  the type of result returned by the {@link CompletableFuture} as values are added
 *
 * @author jk 2015.12.17
 * @since Coherence 14.1.1
 */
public class BatchingOperationsQueue<V, R>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a new {@link BatchingOperationsQueue} that will call the specified
     * {@link Consumer} function to process a batch of operations.
     *
     * @param functionBatch   the {@link Consumer} to call to process batches of operations
     * @param cbInitialBatch  the size of the initial batch of operations
     */
    public BatchingOperationsQueue(Consumer<Integer> functionBatch, int cbInitialBatch)
        {
        this ((q, i) -> functionBatch.accept(i),
              cbInitialBatch,
              new DebouncedFlowControl(cbInitialBatch, Integer.MAX_VALUE),
              v -> 1,
              Runnable::run);
        }

    /**
     * Create a new {@link BatchingOperationsQueue} that will call the specified
     * {@link Consumer} function to process a batch of operations.
     * <p>
     * This constructor takes a {@link Consumer} to use to complete futures. This allows us
     * to, for example, optionally use a daemon pool to complete futures that may otherwise
     * complete on a service thread. If the {code completer} parameter is {@code null} futures
     * will complete on the calling thread.
     *
     * @param functionBatch   the {@link Consumer} to call to process batches of operations
     * @param cbInitialBatch  the size of the initial batch of operations
     * @param backlog         the governing FlowControl object
     */
    public BatchingOperationsQueue(Consumer<Integer> functionBatch, int cbInitialBatch, DebouncedFlowControl backlog)
        {
        this(functionBatch, cbInitialBatch, backlog, v -> 1, Runnable::run);
        }

    /**
     * Create a new {@link BatchingOperationsQueue} that will call the specified
     * {@link Consumer} function to process a batch of operations.
     * <p>
     * This constructor takes a {@link Consumer} to use to complete futures. This allows us
     * to, for example, optionally use a daemon pool to complete futures that may otherwise
     * complete on a service thread. If the {code completer} parameter is {@code null} futures
     * will complete on the calling thread.
     *
     * @param functionBatch      the {@link Consumer} to call to process batches of operations
     * @param cbInitialBatch     the size of the initial batch of operations
     * @param backlog            the governing FlowControl object
     * @param backlogCalculator  a function that calculates a backlog from a value
     * @param executor           an {@link Executor} that will execute completion tasks for futures
     */
    public BatchingOperationsQueue(Consumer<Integer> functionBatch, int cbInitialBatch, DebouncedFlowControl backlog,
                                   ToLongFunction<V> backlogCalculator, Executor executor)
        {
        this((q, i) -> functionBatch.accept(i), cbInitialBatch, backlog, backlogCalculator, executor);
        }

    /**
     * Create a new {@link BatchingOperationsQueue} that will call the specified
     * {@link Consumer} function to process a batch of operations.
     *
     * @param functionBatch      the {@link Consumer} to call to process batches of operations
     * @param cbInitialBatch     the size of the initial batch of operations
     * @param backlog            the governing FlowControl object
     * @param backlogCalculator  a function that calculates a backlog from a value
     * @param executor           an {@link Executor} that will execute completion tasks for futures
     */
    public BatchingOperationsQueue(BiConsumer<BatchingOperationsQueue<V, R>, Integer> functionBatch, int cbInitialBatch,
            DebouncedFlowControl backlog, ToLongFunction<V> backlogCalculator, Executor executor)
        {
        f_functionBatch     = functionBatch;
        f_cbInitialBatch    = cbInitialBatch;
        f_queuePending      = new ConcurrentLinkedDeque<>();
        f_queueCurrentBatch = new ConcurrentLinkedDeque<>();
        f_gate              = new ThreadGateLite<>();
        f_backlog           = backlog;
        f_backlogCalculator = backlogCalculator == null ? v -> 1 : backlogCalculator;
        f_executor          = executor == null ? Executor.sameThread() : executor;

        resetTrigger();
        }

    // ----- BatchingOperationsQueue methods --------------------------------

    /**
     * Add the specified value to the pending operations queue.
     *
     * @param value  the value to add to the queue
     *
     * @return a future which will complete once the supplied value has been published to the topic.
     */
    public CompletableFuture<R> add(V value)
        {
        return add(value, /* fFirst */ false);
        }

    /**
     * Add the specified value to the head of the pending operations queue.
     *
     * @param value  the value to add to the head of the queue
     *
     * @return a future which will complete once the supplied value has been published to the topic.
     */
    public CompletableFuture<R> addFirst(V value)
        {
        return add(value, /* fFirst */ true);
        }

    /**
     * Close this {@link BatchingOperationsQueue}.
     * This {@link BatchingOperationsQueue} will no longer accept values
     * but the pending values can continue to be processed.
     */
    public void close()
        {
        // Close the gate so no more add can be made while we are closing
        Gate<?> gate = getGate();
        gate.close(-1);

        try
            {
            m_fActive = false;
            }
        finally
            {
            // Now we can open the gate, nothing can be added as we have deactivated
            gate.open();
            }
        }

    /**
     * Obtain a {@link CompletableFuture} that will be complete when
     * all the currently outstanding operations complete.
     * The returned {@link CompletableFuture} will always complete
     * normally, even if the outstanding operations complete exceptionally.
     *
     * @return a {@link CompletableFuture} that will be completed when
     *         all the currently outstanding operations are complete
     */
    public CompletableFuture<Void> flush()
        {
        // Close the gate so no more add can be made while we are
        // working out the outstanding set of operations
        Gate<?> gate = getGate();
        gate.close(-1);

        try
            {
            Queue<Element>   queueCurrent = getCurrentBatch();
            Deque<Element>   queuePending = getPending();

            // Collect the outstanding futures from the current batch and pending queues
            CompletableFuture<?>[] aFutures = Stream.concat(queueCurrent.stream(), queuePending.stream())
                    .map(Element::getFuture)
                    .filter((future) -> !future.isDone())
                    .toArray(CompletableFuture[]::new);

            return CompletableFuture.allOf(aFutures).handle((_void, throwable) -> null);
            }
        finally
            {
            // Now we can open the gate
            gate.open();
            }
        }

    /**
     * Obtain the values from the current batch to process.
     *
     * @return  the values from the current batch to process
     */
    public LinkedList<V> getCurrentBatchValues()
        {
        return getCurrentBatch().stream()
                .filter(((Predicate<Element>) BatchingOperationsQueue.Element::isDone).negate())
                .map(BatchingOperationsQueue.Element::getValue)
                .collect(Collectors.toCollection(LinkedList::new));
        }

    /**
     * Return true if the current batch is complete.
     *
     * @return  true if the current batch is complete
     */
    public boolean isBatchComplete()
        {
        return purgeCurrentBatch();
        }

    /**
     * Return the combined size of the current batch and pending queues.
     *
     * @return the combined size of the current batch and pending queues
     */
    public int size()
        {
        return getCurrentBatchSize() + getPendingSize();
        }

    /**
     * Return the size of the current batch queue.
     *
     * @return the size of the current batch queue
     */
    public int getCurrentBatchSize()
        {
        return f_queueCurrentBatch.size();
        }

    /**
     * Return the size of the pending queue.
     *
     * @return the size of the pending queue
     */
    public int getPendingSize()
        {
        return f_queuePending.size();
        }

    /**
     * Handle the error that occurred processing the current batch.
     *
     * @param function  the function to create the actual error to complete the future with
     * @param action    the action to take to handle the error
     */
    public void handleError(BiFunction<Throwable, V, Throwable> function, OnErrorAction action)
        {
        Gate<?> gate = getGate();
        gate.close(-1);

        try
            {
            boolean fClose = false;

            if (action == null)
                {
                action = OnErrorAction.CompleteWithException;
                }

            switch(action)
                {
                case Retry:
                    // Move uncompleted elements in the current batch back to the
                    // front of the pending queue - in the same order
                    Deque<Element> queueCurrent = getCurrentBatch();
                    while(!queueCurrent.isEmpty())
                        {
                        Element element = queueCurrent.pollLast();
                        long    cb      = f_backlogCalculator.applyAsLong(element.getValue());
                        m_cbCurrentBatch -= cb;
                        if (!element.isDone())
                            {
                            f_backlog.adjustBacklog(cb);
                            getPending().offerFirst(element);
                            }
                        }

                    // reset the trigger
                    resetTrigger();
                    triggerOperations(f_cbInitialBatch);
                    break;

                case CompleteAndClose:
                    fClose = true;
                case Complete:
                    // Complete all the futures
                    doErrorAction(e -> e.complete(null, null), fClose);
                    break;

                case CompleteWithExceptionAndClose:
                    fClose = true;
                case CompleteWithException:
                    // Complete exceptionally all the futures in the current queue
                    doErrorAction(e -> e.completeExceptionally(null, function), fClose);
                    break;

                case CancelAndClose:
                    fClose = true;
                case Cancel:
                    // Cancel all the futures in the current queue
                    doErrorAction(e -> e.cancel(function, null), fClose);
                    break;
                }
            }
        finally
            {
            gate.open();
            }
        }

    /**
     * Cancel all requests and close the queue.
     */
    public void cancelAllAndClose(String sReason, BiFunction<Throwable, V, Throwable> function)
        {
        Gate<?> gate = getGate();
        gate.close(-1);

        try
            {
            doErrorAction(e -> e.cancel(function, sReason), true);
            }
        finally
            {
            gate.open();
            }
        }

    /**
     * Specifies whether the publisher is active.
     *
     * @return true if the publisher is active; false otherwise
     */
    public boolean isActive()
        {
        return m_fActive;
        }

    /**
     * Create an {@link Element} containing the specified value.
     *
     * @param value  the value to use
     *
     * @return  a new {@link Element} containing the specified value
     */
    protected Element createElement(V value)
        {
        return new Element(value);
        }

    /**
     * Fill the current batch queue with {@link Element}s.
     * <p>
     * The queue will be filled up to a maximum of the specified
     * number of elements, or less if the pending queue has fewer
     * elements than required.
     *
     * @param cbMaxElements  the maximum byte limit to fill the current batch queue
     *
     * @return  true if the current batch queue has elements,
     *          false if the current batch queue is empty.
     */
    public boolean fillCurrentBatch(int cbMaxElements)
        {
        if (m_cbCurrentBatch >= cbMaxElements)
            {
            return true;
            }

        // Shut the gate so that no more offers come
        // into the queue while we remove some elements
        Gate<?> gate = getGate();
        gate.close(-1);

        try
            {
            // Pull elements from the pending queue into the current
            // batch queue until either the pending queue is empty or
            // we have lMaxElements in the current batch queue
            Queue<Element> queueCurrent = getCurrentBatch();
            Queue<Element> queuePending = getPending();
            Element        element      = queuePending.poll();

            while(element != null)
                {
                V value = element.getValue();
                long lSize = f_backlogCalculator.applyAsLong(value);
                element.setSize(lSize);
                try (@SuppressWarnings("unused") NonBlocking nb = new NonBlocking())
                    {
                    f_backlog.adjustBacklog(-lSize);
                    }
                if (!element.isDone())
                    {
                    queueCurrent.add(element);
                    long cbBatch = m_cbCurrentBatch += lSize;

                    if (cbBatch >= cbMaxElements)
                        {
                        // page will be filled
                        break;
                        }
                    }
                element = queuePending.poll();
                }

            // We might not have pulled anything from the queue
            // if, for example, the application has cancelled all
            // the queued futures
            if (queueCurrent.isEmpty())
                {
                // While the gate is shut create a new future to trigger
                // a round of adds when this set is done and more values
                // are added to the queue
                resetTrigger();
                return false;
                }

            return true;
            }
        finally
            {
            // Don't forget to open the gate
            gate.open();
            }
        }

    /**
     * Reset the operations trigger so that a new batch operation
     * will be triggered on another add;
     */
    protected void resetTrigger()
        {
        getTrigger().set(TRIGGER_OPEN);
        }

    /**
     * Pause the queue.
     */
    protected void pause()
        {
        getTrigger().set(TRIGGER_WAIT);
        }

    public boolean resume()
        {
        return getTrigger().compareAndSet(TRIGGER_WAIT, TRIGGER_CLOSED);
        }

    /**
     * If a batch of operations is not already in progress then
     * trigger a new batch of operations.
     */
    protected void triggerOperations()
        {
        triggerOperations(Math.max(f_cbInitialBatch, 1));
        }

    /**
     * If a batch of operations is not already in progress then
     * trigger a new batch of operations using the specified
     * batch size.
     *
     * @param cBatchSize  the batch size
     */
    protected void triggerOperations(int cBatchSize)
        {
        AtomicInteger trigger = getTrigger();

        if (trigger.get() == TRIGGER_OPEN && trigger.compareAndSet(TRIGGER_OPEN, TRIGGER_CLOSED))
            {
            f_functionBatch.accept(this, cBatchSize);
            }
        }

    /**
     * Complete the first {@link Element Element} in the current batch.
     *
     * @param oValue      the value to use to complete the elements
     * @param onComplete  an optional {@link Consumer} to call when requests are completed
     *
     * @return {@code true} if the element was completed
     */
    @SuppressWarnings("unchecked")
    public boolean completeElement(Object oValue, Consumer<R> onComplete)
        {
        Queue<Element> queueCurrent = getCurrentBatch();
        boolean        fCompleted   = false;

        // remove the element from the current batch
        Element element = queueCurrent.poll();
        if (element != null)
            {
            V value = element.getValue();
            m_cbCurrentBatch -= value != null ? f_backlogCalculator.applyAsLong(value) : 0;
            // If the element is not yet complete then...
            if (!element.isDone())
                {
                fCompleted = element.completeSynchronous((R) oValue, onComplete);
                }
            }
        return fCompleted;
        }

    /**
     * Complete the first n {@link Element Elements} in the current batch.
     *
     * @param cComplete   the number of {@link Element}s to complete
     * @param aValues     the values to use to complete the elements
     * @param onComplete  an optional {@link Consumer} to call when requests are completed
     */
    public void completeElements(int cComplete, LongArray<R> aValues, BiFunction<Throwable, V, Throwable> function, Consumer<R> onComplete)
        {
        completeElements(cComplete, NullImplementation.getLongArray(), aValues, function, onComplete);
        }

    /**
     * Complete the first n {@link Element Elements} in the current batch.
     * <p>
     * If any element in the current batch has a corresponding error
     * in the errors array then it will be completed exceptionally.
     *
     * @param cComplete  the number of {@link Element}s to complete
     * @param aErrors     the errors related to individual elements (could be null)
     * @param aValues     the values to use to complete the elements
     * @param onComplete  an optional {@link Consumer} to call when requests are completed
     */
    public void completeElements(int cComplete, LongArray<Throwable> aErrors, LongArray<R> aValues, BiFunction<Throwable, V, Throwable> errFunction, Consumer<R> onComplete)
        {
        Queue<Element> queueCurrent = getCurrentBatch();

        // Loop over the number of completed elements
        for (int i=0; i<cComplete; i++)
            {
            // remove the element from the current batch
            Element element = queueCurrent.poll();
            if (element != null)
                {
                V value = element.getValue();
                m_cbCurrentBatch -= value != null ? f_backlogCalculator.applyAsLong(value) : 0;
                // If the element is not yet complete then...
                if (!element.isDone())
                    {
                    Throwable error = aErrors == null ? null : aErrors.get(i);

                    // Complete normally if there is no error
                    // otherwise complete exceptionally
                    if (error == null)
                        {
                        R oValue = aValues.get(i);
                        element.complete(oValue, onComplete);
                        }
                    else
                        {
                        element.completeExceptionally(error, errFunction);
                        }
                    }
                }
            }
        }

    // ----- accessor methods -----------------------------------------------

    /**
     * Return the {@link Gate} used to synchronize access to the
     * pending queue.
     *
     * @return  the {@link Gate} used to synchronize access to
     *          the pending queue
     */
    protected Gate<?> getGate()
        {
        return f_gate;
        }

    /**
     * Return the {@link Deque} containing the current batch of {@link Element}s.
     *
     * @return  the {@link Deque} containing the current batch of {@link Element}s
     */
    protected Deque<Element> getCurrentBatch()
        {
        return f_queueCurrentBatch;
        }

    /**
     * Return the {@link Deque} containing the pending {@link Element}s.
     *
     * @return  the {@link Deque} containing the pending {@link Element}s
     */
    protected Deque<Element> getPending()
        {
        return f_queuePending;
        }

    protected AtomicInteger getTrigger()
        {
        return f_lockTrigger;
        }

    // ----- object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "BatchingOperationsQueue(" +
                "current=" + getCurrentBatch().size() +
                ", pending=" + getPending().size() +
                ", trigger=" + triggerToString(getTrigger().get()) +
                ", backlog=" + f_backlog +
                ')';
        }


    // ----- helper methods -------------------------------------------------

    /**
     * Add the specified value to the pending operations queue.
     *
     * @param value  the value to add to the queue
     *
     * @return a future which will complete once the supplied value has been published to the topic.
     */
    private CompletableFuture<R> add(V value, boolean fFirst)
        {
        Element element = createElement(value);
        Gate<?> gate    = getGate();

        // Wait to enter the gate
        gate.enter(-1);
        try
            {
            assertActive();

            // Add the new element containing the value and the future to the offer queue
            if (fFirst)
                {
                getPending().addFirst(element);
                }
            else
                {
                getPending().add(element);
                }
            }
        finally
            {
            // and finally exit from the gate
            gate.exit();
            }

        // This will cause the batch operation to be triggered if required.
        triggerOperations(f_cbInitialBatch);

        f_backlog.adjustBacklog(f_backlogCalculator.applyAsLong(value));

        return element.getFuture();
        }

    /**
     * Assert that this {@link BatchingOperationsQueue} is active.
     *
     * @throws com.tangosol.util.AssertionException if
     * this {@link BatchingOperationsQueue} is not active.
     */
    protected void assertActive()
        {
        if (!isActive())
            {
            throw new IllegalStateException("This batching queue is no longer active");
            }
        }

    /**
     * Poll all the elements from the queue and pass any non-done elements
     * to the consumer.
     *
     * @param action  the consumer to process non-done elements
     * @param fClose  {@code true} to close the queues
     */
    protected void doErrorAction(Consumer<Element> action, boolean fClose)
        {
        Deque<Element> current = getCurrentBatch();
        Deque<Element> pending = getPending();

        if (!current.isEmpty() || !pending.isEmpty())
            {
            Stream.concat(current.stream(), pending.stream())
                    .forEach(element ->
                             {
                             if (!element.isDone())
                                 {
                                 action.accept(element);
                                 }
                             });

            m_cbCurrentBatch = 0;

            try (@SuppressWarnings("unused") NonBlocking nb = new NonBlocking())
                {
                // must do this in a non-blocking try block in case we're backlogged
                long lBacklog = pending.stream()
                        .map(element -> f_backlogCalculator.applyAsLong(element.getValue()))
                        .mapToLong(Long::longValue)
                        .sum();

                f_backlog.adjustBacklog(-lBacklog);
                }
            }

        if (fClose)
            {
            close();
            }
        else
            {
            resetTrigger();
            }
        }

    private String triggerToString(int n)
        {
        switch (n)
            {
            case TRIGGER_OPEN:
                return "TRIGGER_OPEN";
            case TRIGGER_CLOSED:
                return "TRIGGER_CLOSED";
            case TRIGGER_WAIT:
                return "TRIGGER_WAIT";
            }
        return "TRIGGER_UNKNOWN";
        }

    /**
     * Remove any completed elements from the current batch.
     *
     * @return  {@code true} if the current batch is empty
     */
    private boolean purgeCurrentBatch()
        {
        if (f_queueCurrentBatch.isEmpty())
            {
            return true;
            }

        // Shut the gate so that no more offers come
        // into the queue while we remove some elements
        Gate<?> gate = getGate();
        gate.close(-1);

        try
            {
            Iterator<Element> iterator = f_queueCurrentBatch.iterator();
            long              cbSize   = m_cbCurrentBatch;
            while (iterator.hasNext())
                {
                Element element = iterator.next();
                if (element.isDone())
                    {
                    iterator.remove();
                    cbSize -= element.getSize();
                    }
                }
            m_cbCurrentBatch = cbSize;
            }
        finally
            {
            // Don't forget to open the gate
            gate.open();
            }
        return getCurrentBatchValues().isEmpty();
        }

    // ----- inner class: Element -------------------------------------------

    /**
     * A class holding a value and a {@link CompletableFuture} that will be
     * completed when async operation on this element completes
     */
    public class Element
        {
        /**
         * Create an Element with the specified value and future.
         *
         * @param value   the value for this element
         */
        public Element(V value)
            {
            f_value  = value;
            f_future = new CompletableFuture<>();
            // ensure we're set to done if the future is completed
            f_future.handle((r, error) ->
                            {
                            m_fCancelled = error instanceof CancellationException;
                            m_fDone = true;
                            return null;
                            });
            }

        /**
         * Obtain the value to add to the topic.
         *
         * @return  the value to add to the topic
         */
        public V getValue()
            {
            return f_value;
            }

        /**
         * Obtain the {@link CompletableFuture} to complete when the
         * value has been added to the topic.
         *
         * @return  the {@link CompletableFuture} to complete when
         *          the value has been added to the topic.
         */
        public CompletableFuture<R> getFuture()
            {
            return f_future;
            }

        /**
         * Determine whether this element's add operations has completed
         * either successfully or exceptionally, or has been cancelled.
         *
         * @return  true if this element's add operation has completed
         */
        public boolean isDone()
            {
            return m_fDone || f_future.isDone();
            }

        /**
         * Determine whether this element's add operations has been cancelled.
         *
         * @return  true if this element's add operation has been cancelled
         */
        public boolean isCancelled()
            {
            return m_fCancelled || f_future.isCancelled();
            }

        /**
         * Complete this element's {@link CompletableFuture}.
         *
         * @param result      the value to use to complete the future
         * @param onComplete  an optional {@link Consumer} to call when the future is actually completed
         */
        public void complete(R result, Consumer<R> onComplete)
            {
            if (!m_fDone)
                {
                m_fDone = true;
                f_executor.complete(f_future, result, onComplete);
                }
            }

        /**
         * Complete this element's {@link CompletableFuture} synchronously.
         *
         * @param result      the value to use to complete the future
         * @param onComplete  an optional {@link Consumer} to call when the future is actually completed
         */
        public boolean completeSynchronous(R result, Consumer<R> onComplete)
            {
            boolean fCompleted = false;
            if (!m_fDone)
                {
                m_fDone = true;
                fCompleted = f_future.complete(result);
                if (fCompleted && onComplete != null)
                    {
                    try
                        {
                        onComplete.accept(result);
                        }
                    catch (Throwable t)
                        {
                        Logger.err(t);
                        }
                    }
                }
            return fCompleted;
            }

        /**
         * Complete exceptionally this element's {@link CompletableFuture}.
         *
         * @param throwable  the error that occurred
         * @param function   the function to use to create the actual error to complete the future with
         */
        public void completeExceptionally(Throwable throwable, BiFunction<Throwable, V, Throwable> function)
            {
            if (!m_fDone)
                {
                m_fDone = true;
                f_executor.completeExceptionally(f_future, function.apply(throwable, f_value));
                }
            }

        /**
         * Cancel this element's {@link CompletableFuture}.
         *
         * @param function  a function to create an exception
         * @param sReason   an optional cancellation reason
         */
        public void cancel(BiFunction<Throwable, V, Throwable> function, String sReason)
            {
            if (!m_fDone)
                {
                CancellationException exception = sReason != null && !sReason.isEmpty()
                    ? new OperationCancelledException(sReason)
                    : new OperationCancelledException();

                Throwable throwable = function == null ? exception : function.apply(exception, f_value);
                f_executor.completeExceptionally(f_future, throwable);
                }
            }

        public long getSize()
            {
            return m_cbSize;
            }

        public void setSize(long cbSize)
            {
            m_cbSize = cbSize;
            }

        // ----- data members -------------------------------------------

        /**
         * The {@link CompletableFuture} that will be completed when this element's value
         * has been added to the topic.
         */
        private final CompletableFuture<R> f_future;

        /**
         * The value for this element.
         */
        private final V f_value;

        /**
         * A flag indicating whether this element has completed.
         */
        private volatile boolean m_fDone = false;

        /**
         * A flag indicating whether this element has been cancelled.
         */
        private volatile boolean m_fCancelled = false;

        /**
         * The size of this element;
         */
        private long m_cbSize;
        }

    // ----- inner class: SubscriberClosedException -------------------------------

    protected static class OperationCancelledException
            extends CancellationException
        {
        public OperationCancelledException()
            {
            }

        public OperationCancelledException(String message)
            {
            super(message);
            }
        }

    // ----- inner class: OnErrorAction -------------------------------------------

    /**
     * An enum of possible actions to take when an error occurs during
     * asynchronous topic operations.
     */
    public enum OnErrorAction
        {
        /**
         * Retry the failed operation.
         */
        Retry,

        /**
         * Complete the {@link CompletableFuture}s associated with the
         * failed operation and all outstanding pending futures.
         */
        Complete,

        /**
         * Complete the {@link CompletableFuture}s associated with the
         * failed operation and all outstanding pending futures and close
         * this {@link BatchingOperationsQueue}.
         */
        CompleteAndClose,

        /**
         * Complete exceptionally the {@link CompletableFuture}s associated
         * with the failed operation and all outstanding pending futures.
         */
        CompleteWithException,

        /**
         * Complete exceptionally the {@link CompletableFuture}s associated
         * with the failed operation and all outstanding pending futures and
         * close this {@link BatchingOperationsQueue}.
         */
        CompleteWithExceptionAndClose,

        /**
         * Cancel the {@link CompletableFuture}s associated with the
         * failed operation and all outstanding pending futures.
         */
        Cancel,

        /**
         * Cancel the {@link CompletableFuture}s associated with the
         * failed operation and all outstanding pending futures and
         * close this {@link BatchingOperationsQueue}.
         */
        CancelAndClose,
        }

    // ----- inner interface Executor ---------------------------------------

    /**
     * A simple executor that will be used to invoke completion tasks on
     * {@link CompletableFuture} instances.
     */
    public interface Executor
        {
        /**
         * Execute the completion task.
         *
         * @param runnable the task to complete.
         */
        void execute(Runnable runnable);

        /**
         * Complete the future with a value.
         *
         * @param future      the {@link CompletableFuture} to complete
         * @param oValue      the value to use to complete the future
         * @param onComplete  an optional {@link Consumer} to call when the future is actually completed
         *
         * @param <R>     the type of the future's value
         */
        default <R> void complete(CompletableFuture<R> future, R oValue, Consumer<R> onComplete)
            {
            execute(() ->
                {
                boolean fCompleted = future.complete(oValue);
                if (fCompleted && onComplete != null)
                    {
                    try
                        {
                        onComplete.accept(oValue);
                        }
                    catch (Throwable t)
                        {
                        Logger.err(t);
                        }
                    }
                });
            }

        /**
         * Complete the future with an error.
         *
         * @param future  the {@link CompletableFuture} to complete
         * @param t       the error to use to complete the future
         */
        default void completeExceptionally(CompletableFuture<?> future, Throwable t)
            {
            execute(() -> future.completeExceptionally(t));
            }

        /**
         * Return an {@link Executor} that completes futures
         * on the calling thread.
         *
         * @return an {@link Executor} that completes futures
         *         on the calling thread
         */
        static Executor sameThread()
            {
            return Runnable::run;
            }

        /**
         * Return an {@link Executor} that completes futures
         * using a {@link DaemonPool}.
         *
         * @param daemon  the {@link TaskDaemon} that executes the tasks
         *
         * @return an {@link Executor} that completes futures
         *         using a {@link TaskDaemon}.
         */
        static Executor fromTaskDaemon(TaskDaemon daemon)
            {
            return daemon::executeTask;
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Trigger state indicating that there is no request in progress.
     */
    public static final int TRIGGER_OPEN = 0;

    /**
     * Trigger state indicating that a request is in progress.
     */
    public static final int TRIGGER_CLOSED = 1;

    /**
     * Trigger state indicating that no request is in progress but requests are deferred
     * pending notification from the topic.
     */
    public static final int TRIGGER_WAIT = 2;


    // ----- data members ---------------------------------------------------

    /**
     * The {@link Consumer} to call to process batches of operations.
     */
    private final BiConsumer<BatchingOperationsQueue<V, R>, Integer> f_functionBatch;

    /**
     * The initial batch size to use.
     */
    private final int f_cbInitialBatch;

    /**
     * The {@link Deque} of {@link Element}s waiting to be offered to the topic.
     */
    private final Deque<Element> f_queuePending;

    /**
     * The {@link Deque} of currently in-flight operations.
     */
    private final Deque<Element> f_queueCurrentBatch;

    /**
     * The cumulative size of the current batch.
     */
    private long m_cbCurrentBatch;

    /**
     * The {@link Gate} controlling access to the {@link #f_queuePending} queue
     */
    private final Gate<?> f_gate;

    /**
     * The lock for submitting operations.
     */
    private final AtomicInteger f_lockTrigger = new AtomicInteger(TRIGGER_OPEN);

    /**
     * The FlowControl object.
     */
    private final DebouncedFlowControl f_backlog;

    /**
     * The function used to calculate backlog length from a value.
     */
    private final ToLongFunction<V> f_backlogCalculator;

    /**
     * The executor to use to complete the {@link Element} futures.
     */
    private final Executor f_executor;

    /**
     * A flag indicating whether this {@link PagedTopicPublisher} is active.
     */
    private boolean m_fActive = true;
    }
