/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.processor;


import com.tangosol.internal.util.Daemons;
import com.tangosol.net.NamedCache;

import com.tangosol.util.EntrySetMap;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.InvocableMap.EntryProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * An {@link EntryProcessor EntryProcessor} wrapper class that allows for
 * an asynchronous invocation of the underlying processor. When used as a
 * {@link Future} (without extending), this implementation will collect the
 * results of asynchronous invocation into a Map, providing the {@link Future#get
 * result} semantics identical to the {@link EntryProcessor#processAll
 * EntryProcessor.processAll} contract.
 * <p>
 * More advanced use would require extending this class and overriding
 * {@link #onResult}, {@link #onComplete}, and {@link #onException} methods.
 * <p>
 * <b>It is very important</b> that the overriding implementations of these methods
 * must be non-blocking. For example, any use of {@link NamedCache} API is
 * completely disallowed, with the only exception of asynchronous agents.
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
 * @see SingleEntryAsynchronousProcessor
 *
 * @author gg/mf 2012.12.21
 */
public class AsynchronousProcessor<K, V, R>
        extends AbstractAsynchronousProcessor<K, V, R, Map<K, R>>
    {
    /**
     * Construct an AsynchronousProcessor for a given processor.
     *
     * @param processor     the underlying {@link EntryProcessor}
     */
    public AsynchronousProcessor(EntryProcessor<K, V, R> processor)
        {
        this(processor, Thread.currentThread().hashCode(), null);
        }

    /**
     * Construct an AsynchronousProcessor for a given processor.
     *
     * @param processor  the underlying {@link EntryProcessor}
     * @param executor   an optional {@link Executor} to complete the future on,
     *                   if not provided the {@link Daemons#commonPool()} is used
     */
    public AsynchronousProcessor(EntryProcessor<K, V, R> processor, Executor executor)
        {
        this(processor, Thread.currentThread().hashCode(), executor);
        }

    /**
     * Construct an AsynchronousProcessor for a given processor.
     *
     * @param processor     the underlying {@link EntryProcessor}
     * @param iUnitOrderId  the unit-of-order id for this processor
     */
    public AsynchronousProcessor(EntryProcessor<K, V, R> processor, int iUnitOrderId)
        {
        this(processor, iUnitOrderId, null);
        }

    /**
     * Construct an AsynchronousProcessor for a given processor.
     *
     * @param processor     the underlying {@link EntryProcessor}
     * @param iUnitOrderId  the unit-of-order id for this processor
     * @param executor      an optional {@link Executor} to complete the future on,
     *                      if not provided the {@link Daemons#commonPool()} is used
     */
    public AsynchronousProcessor(EntryProcessor<K, V, R> processor, int iUnitOrderId, Executor executor)
        {
        super(processor, iUnitOrderId, executor);
        }

    // ----- AbstractAsynchronousProcessor API -------------------------------

    @Override
    public void onResult(Map.Entry<K, R> entry)
        {
        List<Map.Entry<K, R>> list = m_listResultEntries;
        if (list == null)
            {
            list = m_listResultEntries = new ArrayList<>();
            }
        list.add(entry);
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
            List<Map.Entry<K, R>> list = m_listResultEntries;
            complete(() -> list == null
                    ? Collections.emptyMap()
                    : new EntrySetMap(new ImmutableArrayList(list).getSet()));
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

    /**
     * List of result value entries. The reason we keep the result entries as a
     * List rather than a Set or a Map is to skip unnecessary "equals" checks
     * and defer potentially unneeded deserialization.
     */
    protected List<Map.Entry<K, R>> m_listResultEntries;
    }
