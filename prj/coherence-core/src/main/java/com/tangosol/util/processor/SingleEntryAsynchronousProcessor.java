/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.processor;


import com.tangosol.internal.util.Daemons;
import com.tangosol.net.NamedCache;

import com.tangosol.util.InvocableMap.EntryProcessor;

import java.util.Map;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;


/**
 * An {@link EntryProcessor EntryProcessor} wrapper class that allows for
 * an asynchronous invocation of the underlying processor against a single
 * cache entry. When used as a {@link Future} (without extending), this
 * implementation will collect the results of asynchronous invocation,
 * providing the {@link Future#get result} semantics identical to the
 * {@link EntryProcessor#process EntryProcessor.process} contract.
 * <p>
 * More advanced use would require extending this class and overriding
 * {@link #onResult}, {@link #onComplete}, and {@link #onException} methods.
 * <p>
 * <b>It's very important</b> that the overriding implementations of these methods
 * must be non-blocking. For example, any use of {@link NamedCache} API is
 * completely disallowed, with the only exception of asynchronous agents
 * <b>with disabled flow control.</b>
 * <p>
 * The underlying entry processor is guaranteed to have been fully executed when
 * {@link #onComplete} is called.
 * <p>
 * Note 1: Neither this class nor its extensions need to be serializable. Only the
 * underlying processor is serialized and sent to corresponding servers for execution.
 * <br>
 * Note 2: This feature is not available on Coherence*Extend clients.
 *
 * @param <K>  the type of the Map entry key
 * @param <V>  the type of the Map entry value
 * @param <R>  the type of value returned by the EntryProcessor
 *
 * @see AsynchronousProcessor
 *
 * @author as  2015.01.26
 * @since 12.2.1
 */
public class SingleEntryAsynchronousProcessor<K, V, R>
        extends AbstractAsynchronousProcessor<K, V, R, R>
    {
    /**
     * Construct a SingleEntryAsynchronousProcessor for a given processor.
     *
     * @param processor  the underlying {@link EntryProcessor}
     */
    public SingleEntryAsynchronousProcessor(EntryProcessor<K, V, R> processor)
        {
        this(processor, Thread.currentThread().hashCode(), null);
        }

    /**
     * Construct a SingleEntryAsynchronousProcessor for a given processor.
     *
     * @param processor  the underlying {@link EntryProcessor}
     * @param executor   an optional {@link Executor} to complete the future on,
     *                   if not provided the {@link Daemons#commonPool()} is used
     */
    public SingleEntryAsynchronousProcessor(EntryProcessor<K, V, R> processor, Executor executor)
        {
        this(processor, Thread.currentThread().hashCode(), executor);
        }

    /**
     * Construct a SingleEntryAsynchronousProcessor for a given processor.
     *
     * @param processor     the underlying {@link EntryProcessor}
     * @param iUnitOrderId  the unit-of-order id for this processor
     */
    public SingleEntryAsynchronousProcessor(EntryProcessor<K, V, R> processor,
                                            int iUnitOrderId)
        {
        this(processor, iUnitOrderId, null);
        }

    /**
     * Construct a SingleEntryAsynchronousProcessor for a given processor.
     *
     * @param processor     the underlying {@link EntryProcessor}
     * @param iUnitOrderId  the unit-of-order id for this processor
     * @param executor      an optional {@link Executor} to complete the future on,
     *                      if not provided the {@link Daemons#commonPool()} is used
     */
    public SingleEntryAsynchronousProcessor(EntryProcessor<K, V, R> processor,
                                            int iUnitOrderId, Executor executor)
        {
        super(processor, iUnitOrderId, executor);
        }

    // ----- AsynchronousProcessor API ---------------------------------------

    @Override
    public void onResult(Map.Entry<K, R> entry)
        {
        m_entry = entry;
        }

    @Override
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
            complete(() -> m_entry == null ? null : m_entry.getValue());
            }
        else
            {
            completeExceptionally(eReason);
            }
        }

    // ----- data fields ----------------------------------------------------

    /**
     * Reason for the failed operation.
     */
    protected volatile Throwable m_eReason;

    /**
     * The result of entry processor invocation.
     */
    protected Map.Entry<K, R> m_entry;
    }
