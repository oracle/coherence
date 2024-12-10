/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.processor;


import com.tangosol.internal.util.Daemons;
import com.tangosol.util.AsynchronousAgent;
import com.tangosol.util.InvocableMap;

import com.tangosol.util.aggregator.AsynchronousAggregator;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;


/**
 * Abstract base class for asynchronous entry processors.
 *
 * @param <K>  the type of the Map entry key
 * @param <V>  the type of the Map entry value
 * @param <R>  the type of value returned by the EntryProcessor
 * @param <T>  the type of the result
 *
 * @author as  2015.01.26
 * @since 12.2.1
 */
public abstract class AbstractAsynchronousProcessor<K, V, R, T>
        extends AsynchronousAgent<T>
        implements InvocableMap.EntryProcessor<K, V, R>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct AbstractAsynchronousProcessor instance.
     *
     * @param processor     the underlying {@link InvocableMap.EntryProcessor}
     * @param iUnitOrderId  the unit-of-order id for this processor
     */
    protected AbstractAsynchronousProcessor(InvocableMap.EntryProcessor<K, V, R> processor, int iUnitOrderId)
        {
        this(processor, iUnitOrderId, null);
        }

    /**
     * Construct AbstractAsynchronousProcessor instance.
     *
     * @param processor     the underlying {@link InvocableMap.EntryProcessor}
     * @param iUnitOrderId  the unit-of-order id for this processor
     * @param executor      an optional {@link Executor} to complete the future on,
     *                      if not provided the {@link Daemons#commonPool()} is used
     */
    protected AbstractAsynchronousProcessor(InvocableMap.EntryProcessor<K, V, R> processor,
                                 int iUnitOrderId, Executor executor)
        {
        super(iUnitOrderId, executor);

        f_processor = processor;
        }

    // ---- AbstractAsynchronousProcessor API --------------------------------

    /**
     * Called when there is a partial result of the asynchronous execution.
     * <p>
     * For a given request, calls to this method and {@link #onException}
     * may come concurrently.
     * <p>
     * For ordering guarantees across different processor invocations see
     * {@link #getUnitOfOrderId}.
     * <p>
     * Note: Overriding implementations of this method must be non-blocking.
     *
     * @param entry  an entry holding the key and a result of the operation for
     *               the given key
     */
    public abstract void onResult(Map.Entry<K, R> entry);

    /**
     * Called after the processor has been notified about all possible partial
     * results or failures and no more are forthcoming. As long as this processor
     * was submitted to any of {@link InvocableMap}'s methods, this method is
     * guaranteed to be called once and only once.
     * <p>
     * Possible call back sequences are:
     * <pre>
     *  cache.invoke
     *    ...
     *    onResult
     *    onComplete
     *
     *  cache.invoke
     *    ...
     *    onException
     *    onComplete
     *
     *  cache.invokeAll
     *    ...
     *     onResult
     *     onException
     *     onException
     *     onResult
     *     onComplete
     * </pre>
     *
     * For ordering guarantees across processors see {@link #getUnitOfOrderId}.
     * <p>
     * Note: Overriding implementations of this method must be non-blocking.
     */
    public abstract void onComplete();

    /**
     * Called if any part of the operation failed for any reason. For operations
     * that span multiple partitions this method could be called more than once.
     * However, unless subclasses override this method, any failure will
     * {@link #isDone() "complete"} the operation.
     * <p>
     * Note: Overriding implementations of this method must be non-blocking.
     *
     * @param eReason  the reason of failure
     */
    public abstract void onException(Throwable eReason);

    // ---- accessors -------------------------------------------------------

    /**
     * Return a unit-of-order id associated with this processor. By default,
     * the unit-of-order id is assigned to the calling thread's hashCode.
     * <p>
     * If two consecutive "invoke" calls are made using {@link AsynchronousProcessor
     * AsynchronousProcessors} with the same order id and the same key set,
     * then the corresponding {@link InvocableMap.EntryProcessor#process execution} and calls
     * to {@link #onResult} are going to happen in the exact same order.
     * <p>
     * If two consecutive "invoke" calls are made using {@link AsynchronousProcessor
     * AsynchronousProcessors} with the same order id and the same partition set,
     * then the corresponding {@link InvocableMap.EntryProcessor#process execution} and
     * calls to {@link #onComplete} are going to happen in the exact same order.
     * <p>
     * Note 1: The ordering guarantee is respected between {@link AsynchronousProcessor
     * AsynchronousProcessors} and {@link AsynchronousAggregator}s with the same
     * unit-of-order id.
     * <br>
     * Note 2: There is no ordering guarantees between asynchronous and synchronous
     * operations.
     *
     * @return the unit-of-order id associated with this processor
     */
    public int getUnitOfOrderId()
        {
        return m_iOrderId;
        }

    /**
     * Obtain the underlying entry processor.
     *
     * @return the underlying entry processor
     */
    public InvocableMap.EntryProcessor<K, V, R> getProcessor()
        {
        return f_processor;
        }

    // ---- EntryProcessor interface ----------------------------------------

    /**
     * Not supported.
     */
    public R process(InvocableMap.Entry<K, V> entry)
        {
        throw new UnsupportedOperationException();
        }

    /**
     * Not supported.
     */
    public Map<K, R> processAll(Set<? extends InvocableMap.Entry<K, V>> setEntries)
        {
        throw new UnsupportedOperationException();
        }

    // ---- data members ----------------------------------------------------

    /**
     * The underlying entry processor.
     */
    protected final InvocableMap.EntryProcessor<K, V, R> f_processor;
    }
