/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.internal.net.DebouncedFlowControl;

import com.tangosol.net.CacheFactory;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.Gate;
import com.tangosol.util.LongArray;
import com.tangosol.util.ThreadGateLite;

import java.util.Deque;
import java.util.List;
import java.util.Queue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.function.Consumer;
import java.util.function.Predicate;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link BatchingOperationsQueue} is a queue that values can be added to for later processing.
 * When values are added a function is triggered to process a batch of values.
 * <p>
 * When values are added to the queue the {@link Consumer} function will be called with a batch size.
 *
 * @author jk 2015.12.17
 * @since Coherence 14.1.1
 */
public class BatchingOperationsQueue<F>
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
        this (functionBatch, cbInitialBatch, new DebouncedFlowControl(cbInitialBatch, Integer.MAX_VALUE));
        }

    /**
     * Create a new {@link BatchingOperationsQueue} that will call the specified
     * {@link Consumer} function to process a batch of operations.
     *
     * @param functionBatch   the {@link Consumer} to call to process batches of operations
     * @param cbInitialBatch  the size of the initial batch of operations
     * @param backlog         the governing FlowControl object
     */
    public BatchingOperationsQueue(Consumer<Integer> functionBatch, int cbInitialBatch, DebouncedFlowControl backlog)
        {
        f_functionBatch     = functionBatch;
        f_cbInitialBatch    = cbInitialBatch;
        f_queuePending      = new ConcurrentLinkedDeque<>();
        f_queueCurrentBatch = new ConcurrentLinkedDeque<>();
        f_gate              = new ThreadGateLite();
        f_backlog           = backlog;

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
    public CompletableFuture<F> add(Binary value)
        {
        Element element = createElement(value);
        Gate    gate    = getGate();

        // Wait to enter the gate
        gate.enter(-1);
        try
            {
            assertActive();

            // Add the new element containing the binary and the future to the offer queue
            getPending().add(element);
            }
        finally
            {
            // and finally exit from the gate
            gate.exit();
            }

        // This will cause the batch operation to be triggered if required.
        triggerOperations(f_cbInitialBatch);

        f_backlog.adjustBacklog(value.length());

        return element.getFuture();
        }

    /**
     * Close this {@link BatchingOperationsQueue}.
     * This {@link BatchingOperationsQueue} will no longer accept values
     * but the pending values can continue to be processed.
     */
    public void close()
        {
        // Close the gate so no more add can be made while we are closing
        Gate gate = getGate();
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
     * all of the currently outstanding operations complete.
     * The returned {@link CompletableFuture} will always complete
     * normally, even if the outstanding operations complete exceptionally.
     *
     * @return a {@link CompletableFuture} that will be completed when
     *         all of the currently outstanding operations are complete
     */
    public CompletableFuture<Void> flush()
        {
        // Close the gate so no more add can be made while we are
        // working out the outstanding set of operations
        Gate gate = getGate();
        gate.close(-1);

        try
            {
            Queue<Element>   queueCurrent = getCurrentBatch();
            Deque<Element>   queuePending = getPending();

            // Collect the outstanding futures from the current batch and pending queues
            CompletableFuture[] aFutures = Stream.concat(queueCurrent.stream(), queuePending.stream())
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
    public List<Binary> getCurrentBatchValues()
        {
        return getCurrentBatch().stream()
                .filter(((Predicate<Element>) BatchingOperationsQueue.Element::isDone).negate())
                .map(BatchingOperationsQueue.Element::getValue)
                .collect(Collectors.toList());
        }

    /**
     * Return true if the current batch is complete.
     *
     * @return  true if the current batch is complete
     */
    public boolean isBatchComplete()
        {
        return getCurrentBatchValues().isEmpty();
        }

    /**
     * Handle the error that occurred processing the current batch.
     *
     * @param throwable  the error that occurred
     * @param action     the action to take to handle the error
     */
    public void handleError(Throwable throwable, OnErrorAction action)
        {
        Gate gate = getGate();
        gate.close(-1);

        try
            {
            Deque<Element> queueCurrent   = getCurrentBatch();
            Deque<Element> queuePending   = getPending();

            if (action == null)
                {
                action = OnErrorAction.CompleteWithException;
                }

            if (throwable != null)
                {
                CacheFactory.log("Caught asynchronous error " + throwable.getClass().getName() + " - action is " + action, Base.LOG_QUIET);
                }

            switch(action)
                {
                case Retry:
                    // Move uncompleted elements in the current batch back to the
                    // front of the pending queue - in the same order
                    while(!queueCurrent.isEmpty())
                        {
                        Element element = queueCurrent.pollLast();
                        int     cb      = element.getValue().length();
                        m_cbCurrentBatch -= cb;
                        if (!element.isDone())
                            {
                            f_backlog.adjustBacklog(cb);
                            queuePending.offerFirst(element);
                            }
                        }

                    // reset the trigger
                    resetTrigger();
                    triggerOperations(f_cbInitialBatch);
                    break;

                case Complete:
                    // Complete all of the futures in both queues
                    Stream.concat(queueCurrent.stream(), queuePending.stream())
                            .filter(((Predicate<Element>) Element::isDone).negate())
                            .forEach(Element::complete);

                    close();
                    break;

                case CompleteWithException:
                    // Complete exceptionally all of the futures in both queues
                    Stream.concat(queueCurrent.stream(), queuePending.stream())
                            .filter(((Predicate<Element>) Element::isDone).negate())
                            .forEach((element) -> element.completeExceptionally(throwable));

                    close();
                    break;

                case Cancel:
                    // Cancel all of the futures in both queues
                    Stream.concat(queueCurrent.stream(), queuePending.stream())
                            .filter(((Predicate<Element>)Element::isDone).negate())
                            .forEach(Element::cancel);

                    close();
                    break;
                }
            }
        finally
            {
            gate.open();
            }
        }

    /**
     * Specifies whether or not the publisher is active.
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
    protected Element createElement(Binary value)
        {
        return new Element(value, new CompletableFuture<>());
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
        Gate gate = getGate();
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
                f_backlog.adjustBacklog(-element.getValue().length());
                if (!element.isDone())
                    {
                    queueCurrent.add(element);
                    int cbBatch = m_cbCurrentBatch += element.getValue().length();

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
            // of the queued futures
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
            f_functionBatch.accept(cBatchSize);
            }
        }

    /**
     * Complete the first n {@link Element}s in the current batch.
     * <p>
     * If any element in the current batch has a corresponding error
     * in the errors array then it will be completed exceptionally.
     *
     * @param cComplete  the number of {@link Element}s to complete
     * @param aErrors    the errors related to individual elements (may be null)
     */
    public void completeElements(int cComplete, LongArray<Throwable> aErrors)
        {
        Queue<Element> queueCurrent = getCurrentBatch();

        // Loop over the number of completed elements
        for (int i=0; i<cComplete; i++)
            {
            // remove the element from the current batch
            Element element = queueCurrent.poll();

            m_cbCurrentBatch -= element.getValue().length();
            // If the element is not yet complete then...
            if (!element.isDone())
                {
                Throwable error = aErrors == null ? null : aErrors.get(i);

                // Complete normally if there is no error
                // otherwise complete exceptionally
                if (error == null)
                    {
                    element.complete();
                    }
                else
                    {
                    element.completeExceptionally(error);
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
    protected Gate getGate()
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

    // ----- helper methods -------------------------------------------------

    /**
     * Assert that this {@link BatchingOperationsQueue} is active.
     *
     * @throws com.tangosol.util.AssertionException if
     * this {@link BatchingOperationsQueue} is not active.
     */
    protected void assertActive()
        {
        Base.azzert(isActive(), "This batching queue is no longer active");
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
         * @param binValue  the value for this element
         * @param future    the {@link CompletableFuture} that will be completed
         *                  when the value has been added to the topic
         */
        public Element(Binary binValue, CompletableFuture<F> future)
            {
            f_binValue = binValue;
            f_future   = future;
            }

        /**
         * Obtain the value to add to the topic.
         *
         * @return  the value to add to the topic
         */
        public Binary getValue()
            {
            return f_binValue;
            }

        /**
         * Obtain the {@link CompletableFuture} to complete when the
         * value has been added to the topic.
         *
         * @return  the {@link CompletableFuture} to complete when
         *          the value has been added to the topic.
         */
        public CompletableFuture<F> getFuture()
            {
            return f_future;
            }

        /**
         * Determine whether this element's add operations has completed
         * either successfully or exceptionally.
         *
         * @return  true if this element;s add operation has completed
         */
        public boolean isDone()
            {
            return f_future.isDone();
            }

        /**
         * Complete this element's {@link CompletableFuture}
         */
        public void complete()
            {
            f_future.complete(null);
            }

        /**
         * Complete exceptionally this element's {@link CompletableFuture}
         */
        public void completeExceptionally(Throwable throwable)
            {
            f_future.completeExceptionally(throwable);
            }

        /**
         * Cancel this element's {@link CompletableFuture}
         */
        public void cancel()
            {
            f_future.cancel(true);
            }


        // ----- data members -------------------------------------------

        /**
         * The {@link CompletableFuture} that will be completed when this element's value
         * has been added to the topic.
         */
        private final CompletableFuture<F> f_future;

        /**
         * The value for this element.
         */
        private final Binary f_binValue;
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
         * Complete exceptionally the {@link CompletableFuture}s associated
         * with the failed operation and all outstanding pending futures.
         */
        CompleteWithException,

        /**
         * Cancel the {@link CompletableFuture}s associated with the
         * failed operation and all outstanding pending futures.
         */
        Cancel,
        }

    // ----- constants ------------------------------------------------------

    /**
     * Trigger state indicating that there is no send in progress.
     */
    public static final int TRIGGER_OPEN = 0;

    /**
     * Trigger state indicating that a send is in progress.
     */
    public static final int TRIGGER_CLOSED = 1;

    /**
     * Trigger state indicating that no send is in progress but sends are deferred
     * pending notification from the topic.
     */
    public static final int TRIGGER_WAIT = 2;


    // ----- data members ---------------------------------------------------

    /**
     * The {@link Consumer} to call to process batches of operations.
     */
    private final Consumer<Integer> f_functionBatch;

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
    private int m_cbCurrentBatch;

    /**
     * The {@link Gate} controlling access to the {@link #f_queuePending} queue
     */
    private final Gate f_gate;

    /**
     * The lock for submitting operations.
     */
    private final AtomicInteger f_lockTrigger = new AtomicInteger(TRIGGER_OPEN);

    /**
     * The FlowControl object.
     */
    private final DebouncedFlowControl f_backlog;

    /**
     * A flag indicating whether this {@link PagedTopicPublisher} is active.
     */
    private boolean m_fActive = true;
    }
