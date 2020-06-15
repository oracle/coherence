/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.processor;


import com.tangosol.net.NamedCache;

import com.tangosol.util.InvocableMap.EntryProcessor;

import java.util.Map;
import java.util.Objects;

import java.util.function.Consumer;


/**
 * An {@link EntryProcessor EntryProcessor} wrapper class that allows for
 * an asynchronous invocation of the underlying processor. Unlike
 * {@link AsynchronousProcessor}, this implementation does not collect the
 * results of the underlying entry processor execution, but simply streams
 * partial results to the provided partial results callback.
 * <p>
 * This allows for a much lower memory overhead if the complete result set does
 * not to be realized on the client.
 * <p>
 * <b>It's very important</b> that the overriding implementations of
 * {@link #onComplete}, {@link #onResult} and {@link #onException},
 * and provided callbacks must be non-blocking. For example, any use of
 * {@link NamedCache} API is completely disallowed, with the only exception of
 * asynchronous agents <b>with disabled flow control.</b>
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
 * @see SingleEntryAsynchronousProcessor
 *
 * @author as  2015.01.28
 */
public class StreamingAsynchronousProcessor<K, V, R>
        extends AbstractAsynchronousProcessor<K, V, R, Void>
    {
    /**
     * Construct a StreamingAsynchronousProcessor for a given processor and one or more callbacks.
     * <p>
     * <b>Important Note:</b> All provided callbacks must be non-blocking.
     * For example, any use of {@link NamedCache} API is completely disallowed.
     *
     * @param processor     the underlying {@link EntryProcessor}
     * @param onPartial     a user-defined callback that will be called for each
     *                      partial result
     */
    public StreamingAsynchronousProcessor(EntryProcessor<K, V, R> processor,
                                          Consumer<? super Map.Entry<? extends K, ? extends R>> onPartial)

        {
        this(processor, Thread.currentThread().hashCode(), onPartial);
        }

    /**
     * Construct a StreamingAsynchronousProcessor for a given processor and one or more callbacks.
     * <p>
     * <b>Important Note:</b> All provided callbacks must be non-blocking.
     * For example, any use of {@link NamedCache} API is completely disallowed.
     *
     * @param processor     the underlying {@link EntryProcessor}
     * @param iUnitOrderId  the unit-of-order id for this processor
     * @param onPartial     a user-defined callback that will be called for each
     *                      partial result
     */
    public StreamingAsynchronousProcessor(EntryProcessor<K, V, R> processor, int iUnitOrderId,
                                          Consumer<? super Map.Entry<? extends K, ? extends R>> onPartial)
        {
        super(processor, iUnitOrderId);

        Objects.requireNonNull(onPartial);
        f_onPartial = onPartial;
        }

    // ----- AsynchronousProcessor API ---------------------------------------

    @Override
    public void onResult(Map.Entry<K, R> entry)
        {
        f_onPartial.accept(entry);
        }

    @Override
    public void onException(Throwable eReason)
        {
        }

    @Override
    public void onComplete()
        {
        complete(() -> null);
        }

    // ---- data members ----------------------------------------------------

    /**
     * The user-provided callback that will be invoked for each partial result.
     */
    protected final Consumer<? super Map.Entry<? extends K, ? extends R>> f_onPartial;
    }
