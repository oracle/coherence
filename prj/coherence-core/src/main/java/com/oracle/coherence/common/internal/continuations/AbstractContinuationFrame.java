/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.continuations;

import com.oracle.coherence.common.base.Continuation;


/**
 * AbstractContinuationFrame is a Runnable which uses continuations to provide standard stack-frame like control flows.
 * <p>
 * The AbstractContinuationFrame takes a number of continuations which are invoked based on how the frame completes.
 * <ul>
 * <li>
 *     failure continuation - the continuation called upon failure, providing the exception
 * </li>
 * <li>
 *     finally continuation - the continuation called upon either normal or failure completion
 * </li>
 * <li>
 *     result continuation  - the continuation called upon normal completion of the operation, providing the result
 * </li>
 * </ul>
 * The AbstractContinuationFrame emulates the following standard call flow:
 * <pre>
 * R result;
 * try
 *     {
 *     result = call();
 *     }
 * catch (Throwable e)
 *     {
 *     continuationFail.proceed(e);
 *     return;
 *     }
 * finally
 *     {
 *     continuationFinally.proceed(null);
 *     }
 *
 * continuation.proceed(result);
 * </pre>
 *
 * @param <R> the result type
 *
 * @author mf  2013.07.02
 */
public abstract class AbstractContinuationFrame<R>
        implements Runnable
    {
    /**
     * Construct a AbstractContinuationFrame.
     *
     * @param continuationFinally  the continuation to run upon either normal or failed completion, may be null
     */
    public AbstractContinuationFrame(Continuation<? super Void> continuationFinally)
        {
        this (/*continuationFail*/ null, continuationFinally, /*continuation*/ null);
        }

    /**
     * Construct a AbstractContinuationFrame.
     *
     * @param continuationFail  the continuation to invoke if the operation fails, may be null
     * @param continuation      the continuation to invoke when the operation completes normally, may be null
     */
    public AbstractContinuationFrame(Continuation<? super Throwable> continuationFail, Continuation<? super R> continuation)
        {
        this (continuationFail, /*continuationFinally*/ null, continuation);
        }

    /**
     * Construct an AbstractContinuationFrame which inherits another's continuations.
     *
     * @param that  the frame to inherit continuations from
     */
    public AbstractContinuationFrame(AbstractContinuationFrame<? super R> that)
        {
        this(that.getFailureContinuation(), that.getContinuation());
        }

    /**
     * Construct a AbstractContinuationFrame.
     *
     * @param continuationFail     the continuation to invoke if the operation fails, may be null
     * @param continuationFinally  the continuation to run upon either normal or failed completion, may be null
     * @param continuation         the continuation to invoke when the operation completes, may be null
     */
    public AbstractContinuationFrame(
            Continuation<? super Throwable> continuationFail,
            final Continuation<? super Void> continuationFinally,
            Continuation<? super R> continuation)
        {
        if (continuationFinally != null)
            {
            continuationFail = new WrapperContinuation<Throwable>(continuationFail)
                {
                @Override
                public void proceed(Throwable e)
                    {
                    try
                        {
                        super.proceed(e);
                        }
                    finally
                        {
                        Continuations.proceed(continuationFinally, null);
                        }
                    }
                };

            continuation = new WrapperContinuation<R>(continuation)
                {
                @Override
                public void proceed(R result)
                    {
                    try
                        {
                        Continuations.proceed(continuationFinally, null);
                        }
                    finally
                        {
                        super.proceed(result);
                        }
                    }
                };

            }

        m_continuationFail = continuationFail;
        m_continuation     = continuation;
        }

    @Override
    public final void run()
        {
        try
            {
            R result = call();
            m_continuationFail = null; // continuationFail is not intended to handle other continuation exceptions
            Continuations.proceed(m_continuation, result);
            }
        catch (Throwable e)
            {
            Continuations.proceed(m_continuationFail, e);
            }
        }

    /**
     * Perform the operation.
     * <p>
     * If control returns from this method without continueAsync() having been called, then the appropriate continuation(s)
     * will be automatically invoked.
     * </p>
     *
     * @return return the operation result
     *
     * @throws Exception upon synchronous failure
     */
    protected abstract R call()
            throws Exception;

    /**
     * Return the continuation to invoke with the result of the operation.
     *
     * @return the result continuation
     */
    protected Continuation<? super R> getContinuation()
        {
        return m_continuation;
        }

    /**
     * Return the continuation to invoke upon failure.
     *
     * @return the failure continuation
     */
    protected Continuation<? super Throwable> getFailureContinuation()
        {
        return m_continuationFail;
        }

    /**
     * Invoking this method indicates that processing is continuing asynchronously, and thus any return value or
     * exception should not automatically trigger the invocation of the continuations.  More specifically invoking
     * this method <tt>null</tt>s out the continuations.
     * <p>
     * As this method returns <tt>null</tt>, it allows for a simple pattern of having the final statement of {@link #call}
     * to be <tt>return continueAsync();</tt> when continuing asynchronously.
     * </p>
     *
     * @return null
     */
    protected R continueAsync()
        {
        m_continuationFail = null;
        m_continuation     = null;
        return null; // we must use null as type specific implementations of call() introduce a cast to the real type
        }

    // ----- data members ----------------------------------------------------

    /**
     * The continuation (if any) to invoke upon failure.
     */
    private Continuation<? super Throwable> m_continuationFail;

    /**
     * The continuation (if any) to invoke with the invocation result.
     */
    private Continuation<? super R> m_continuation;
    }
