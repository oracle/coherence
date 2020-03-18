/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.continuations;

import com.oracle.coherence.common.internal.continuations.AbstractContinuationFrame;
import com.oracle.coherence.common.base.Continuation;

import java.util.concurrent.Callable;


/**
 * SynchronousContinuationFrame is an adapter which allows pre-existing synchronous Callables to support continuations.
 *
 * @author mf/pp  2013.07.09
 */
public class SynchronousContinuationFrame<R>
        extends AbstractContinuationFrame<R>
    {
    /**
     * Construct a SynchronousContinuationFrame.
     *
     * @param callable          the callable to invoke when the frame is run
     * @param continuationFail  the continuation to invoke if the operation fails, may be null
     * @param continuation      the continuation to invoke when the operation completes, may be null
     */
    public SynchronousContinuationFrame(Callable<R> callable,
            Continuation<? super Throwable> continuationFail,
            Continuation<? super R> continuation)
        {
        this (callable, continuationFail, /*continuationFinally*/ null, continuation);
        }

    /**
     * Construct a SynchronousContinuationFrame.
     *
     * @param callable             the callable to invoke when the frame is run
     * @param continuationFinally  the continuation to run upon either normal or failed completion, may be null
     */
    public SynchronousContinuationFrame(Callable<R> callable, Continuation<? super Void> continuationFinally)
        {
        this (callable, /*continuationFail*/ null, continuationFinally, /*continuation*/ null);
        }

    /**
     * Construct an SynchronousContinuationFrame which inherits another's continuations.
     *
     * @param callable  the callable to invoke when the frame is run
     * @param that      the frame to inherit continuations from
     */
    public SynchronousContinuationFrame(Callable<R> callable, AbstractContinuationFrame<? super R> that)
        {
        this(callable, that.getFailureContinuation(), that.getContinuation());
        }

    /**
     * Construct a SynchronousContinuationFrame.
     *
     * @param callable             the callable to invoke when the frame is run
     * @param continuationFail     the continuation to invoke if the operation fails, may be null
     * @param continuationFinally  the continuation to run upon either normal or failed completion, may be null
     * @param continuation         the continuation to invoke when the operation completes, may be null
     */
    public SynchronousContinuationFrame(Callable<R> callable,
            Continuation<? super Throwable> continuationFail,
            Continuation<? super Void> continuationFinally,
            Continuation<? super R> continuation)
        {
        super(continuationFail, continuationFinally, continuation);
        f_callable = callable;
        }

    @Override
    protected R call()
            throws Exception
        {
        return f_callable.call();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The logic associated with the frame.
     */
    protected final Callable<R> f_callable;
    }
