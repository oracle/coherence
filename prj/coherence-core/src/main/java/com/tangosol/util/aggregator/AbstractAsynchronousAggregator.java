/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.aggregator;


import com.tangosol.internal.util.Daemons;
import com.tangosol.util.AsynchronousAgent;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMap.EntryAggregator;
import com.tangosol.util.InvocableMap.StreamingAggregator;

import com.tangosol.util.processor.AsynchronousProcessor;

import java.util.Set;
import java.util.concurrent.Executor;

import javax.json.bind.annotation.JsonbProperty;

/**
 * Abstract base class for asynchronous entry aggregators.
 *
 * @param <K> the type of the Map entry keys
 * @param <V> the type of the Map entry values
 * @param <P> the type of the intermediate result during the parallel stage
 * @param <R> the type of the value returned by the StreamingAggregator
 *
 * @see AsynchronousAggregator
 *
 * @author gg/bb    2015.04.02
 */
public abstract class AbstractAsynchronousAggregator<K, V, P, R>
        extends    AsynchronousAgent<R>
        implements EntryAggregator<K, V, R>
    {

    /**
     * Construct an AsynchronousAggregator for a given streaming aggregator.
     *
     * @param aggregator    the underlying streaming aggregator
     * @param iUnitOrderId  the unit-of-order id for this aggregator
     */
    protected AbstractAsynchronousAggregator(StreamingAggregator<K,V,P,R> aggregator, int iUnitOrderId)
        {
        this(aggregator, iUnitOrderId, null);
        }

    /**
     * Construct an AsynchronousAggregator for a given streaming aggregator.
     *
     * @param aggregator    the underlying streaming aggregator
     * @param iUnitOrderId  the unit-of-order id for this aggregator
     * @param executor      an optional {@link Executor} to complete the future on,
     *                      if not provided the {@link Daemons#commonPool()} is used
     */
    protected AbstractAsynchronousAggregator(StreamingAggregator<K,V,P,R> aggregator,
                                          int iUnitOrderId, Executor executor)
        {
        super(iUnitOrderId, executor);

        m_aggregator = aggregator.supply();
        }

    // ----- AsynchronousAggregator API -------------------------------------

    /**
     * Called when there is a partial result of the asynchronous execution.
     * <p>
     * For a given request, calls to this method and {@link #onException}
     * may come concurrently.
     * <p>
     * For ordering guarantees across different aggregator invocations see
     * {@link #getUnitOfOrderId}.
     * <p>
     * Note: Overriding implementations of this method must be non-blocking.
     *
     * @param result the partial result holder
     */
    public abstract void onResult(P result);

    /**
     * Called after this asynchronous aggregator has been notified about all
     * possible partial results or failures and no more are forthcoming.
     * <p>
     * As long as this aggregator was submitted to any of {@link InvocableMap}'s methods,
     * this method is guaranteed to be called once and only once.
     * <p>
     * Possible call back sequences are:
     * <pre>
     *  cache.aggregate
     *    ...
     *    onResult
     *    onComplete
     *
     *  cache.aggregate
     *    ...
     *    onException
     *    onComplete
     *
     *  cache.aggregate
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
     * Note:
     * <p>
     * Overriding implementations of this method must be non-blocking.
     * <p>
     * If the StreamingAggregator signaled to short-circuit the aggregation
     * while {@link StreamingAggregator#combine combining partial results},
     * onComplete() could be called before all the results are received.
     */
    public abstract void onComplete();

    /**
     * Called if the operation failed for any reason.
     * <p>
     * For a given request, calls to this method and {@link #onResult}
     * may come concurrently.
     * <p>
     * Note: Overriding implementations of this method must be non-blocking.
     *
     * @param eReason  the reason of failure
     */
    public abstract void onException(Throwable eReason);

    /**
     * Return a unit-of-order id associated with this aggregator. By default,
     * the unit-of-order id is assigned to the calling thread's hashCode.
     * <p>
     * If two consecutive "aggregate" calls made using {@link
     * AbstractAsynchronousAggregator AsynchronousAggregators} with the same order id
     * and involve the same key set, then the corresponding {@link
     * EntryAggregator#aggregate execution} and calls to {@link #onResult} are
     * going to happen in the exact same order.
     * <p>
     * Note 1: the ordering guarantee is respected between {@link
     * AbstractAsynchronousAggregator}s and {@link AsynchronousProcessor}s with the same
     * unit-of-order id;
     * <br>
     * Note 2: there is no ordering guarantee between asynchronous and synchronous
     * operations.
     *
     * @return the order id
     */
    public int getUnitOfOrderId()
        {
        return m_iOrderId;
        }

    /**
     * Return the underlying streaming aggregator.
     *
     * @return the underlying aggregator
     */
    public StreamingAggregator<K, V, P, R> getAggregator()
        {
        return m_aggregator;
        }

    // ---- not supported methods --------------------------------------------

    /**
     * Not supported.
     */
    public R aggregate(Set<? extends InvocableMap.Entry<? extends K, ? extends V>> setEntries)
        {
        throw new UnsupportedOperationException();
        }

    // ----- data fields ----------------------------------------------------

    /**
     * The underlying aggregator.
     */
    @JsonbProperty("aggregator")
    protected StreamingAggregator<K, V, P, R> m_aggregator;
    }
