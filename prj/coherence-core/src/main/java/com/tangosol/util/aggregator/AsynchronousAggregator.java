/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.aggregator;


import com.tangosol.internal.util.Daemons;
import com.tangosol.net.NamedCache;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMap.EntryAggregator;
import com.tangosol.util.InvocableMap.StreamingAggregator;

import com.tangosol.util.processor.AsynchronousProcessor;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;


/**
 * A marker {@link EntryAggregator EntryAggregator} wrapper class that allows for
 * an asynchronous invocation of the underlying aggregator. When used as a
 * {@link Future} (without extending), this implementation will simply provide
 * {@link Future#get the result} of asynchronous streaming aggregation according
 * to semantics of the corresponding {@link EntryAggregator#aggregate
 * EntryAggregator.aggregate} contract.
 * <p>
 * More advanced use would require extending this class and overriding {@link
 * #onResult} and {@link #onException} methods. <b>It's very important</b> that
 * overriding implementations of these methods must be non-blocking. For example,
 * any use of {@link NamedCache} API is completely disallowed with the only
 * exception of {@link AsynchronousAggregator#AsynchronousAggregator(InvocableMap.StreamingAggregator) AsynchronousAggregators} and
 * {@link AsynchronousProcessor#AsynchronousProcessor(InvocableMap.EntryProcessor)}
 * <p>
 * The underlying entry processor is guaranteed to have been fully executed when
 * either {@link #onResult onResult()} or {@link #onException onException()} are called.
 * <p>
 * Note 1: Neither this class nor its extensions need to be serializable. Only the
 * underlying aggregator is serialized and sent to corresponding servers for
 * execution.
 * <br>
 * Note 2: This feature is not available on Coherence*Extend clients.
 *
 * @param <K> the type of the Map entry keys
 * @param <V> the type of the Map entry values
 * @param <P> the type of the intermediate result during the parallel stage
 * @param <R> the type of the value returned by the StreamingAggregator
 *
 * @see AsynchronousProcessor
 * @author gg/mf 2012.12.21
 */
public class AsynchronousAggregator<K, V, P, R>
        extends    AbstractAsynchronousAggregator<K, V, P, R>
        implements EntryAggregator<K, V, R>
    {
    /**
     * Construct an AsynchronousAggregator for a given streaming aggregator.
     *
     * @param aggregator  the underlying streaming aggregator
     */
    public AsynchronousAggregator(StreamingAggregator<K, V, P, R> aggregator)
        {
        this(aggregator, Thread.currentThread().hashCode(), null);
        }

    /**
     * Construct an AsynchronousAggregator for a given streaming aggregator.
     *
     * @param aggregator  the underlying streaming aggregator
     * @param executor    an optional {@link Executor} to complete the future on,
     *                    if not provided the {@link Daemons#commonPool()} is used
     */
    public AsynchronousAggregator(StreamingAggregator<K, V, P, R> aggregator, Executor executor)
        {
        this(aggregator, Thread.currentThread().hashCode(), executor);
        }

    /**
     * Construct an AsynchronousAggregator for a given streaming aggregator.
     *
     * @param aggregator    the underlying streaming aggregator
     * @param iUnitOrderId  the unit-of-order id for this aggregator
     */
    public AsynchronousAggregator(StreamingAggregator<K, V, P, R> aggregator,
                                  int iUnitOrderId)
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
    public AsynchronousAggregator(StreamingAggregator<K, V, P, R> aggregator,
                                  int iUnitOrderId, Executor executor)
        {
        super(aggregator, iUnitOrderId, executor);
        }

    // ----- AsynchronousAggregator API -------------------------------------

    /**
     * Called when the aggregation result is available.
     * <p>
     * For ordering guarantees see {@link #getUnitOfOrderId}.
     * <p>
     * Note: Overriding implementations of this method must be non-blocking.
     *
     * @param result the result
     */
    public void onResult(P result)
        {
        if (!isDone())
            {
            if (!m_aggregator.combine(result))
                {
                // short-circuit - we are done
                onComplete();
                }
            }
        }

    /**
     * Called if the operation failed for any reason.
     * <p>
     * Note: Overriding implementations of this method must be non-blocking.
     *
     * @param eReason  the reason of failure
     */
    public void onException(Throwable eReason)
        {
        m_eReason = eReason;
        }

    @Override
    public void onComplete()
        {
        Throwable eReason = m_eReason;
        if (eReason == null)
            {
            complete(m_aggregator::finalizeResult);
            }
        else
            {
            completeExceptionally(eReason);
            }
        }

    // ----- data fields -----------------------------------------------------

    /**
     * Reason for the failed operation.
     */
    protected volatile Throwable m_eReason;
    }
