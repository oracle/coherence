/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.continuations;

import com.oracle.coherence.common.base.Continuation;


/**
 * WrapperContinuation is a Continuation which simply delegates to another continuation.
 *
 * @author mf  2013.07.02
 */
public class WrapperContinuation<R>
    implements Continuation<R>
    {
    public WrapperContinuation(Continuation<? super R> delegate)
        {
        f_delegate = delegate;
        }

    @Override
    public void proceed(R result)
        {
        Continuations.proceed(f_delegate, result);
        }

    protected final Continuation<? super R> f_delegate;
    }
