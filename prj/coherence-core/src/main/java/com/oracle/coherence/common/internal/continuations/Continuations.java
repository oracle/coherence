/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.continuations;

import com.oracle.coherence.common.base.Continuation;


/**
 * Helper for working with Continuations.
 *
 * @author mf  2013.07.02
 */
public final class Continuations
    {
    private Continuations(){}

    /**
     * Invoke {Continuation#proceeed} on the supplied continuation.  If the continuation is null this method is a
     * no-op
     *
     * @param cont   the continuation or null
     * @param result the result
     * @param <R>    the result type
     */
    public static <R> void proceed(Continuation<? super R> cont, R result)
        {
        if (cont != null)
            {
            cont.proceed(result);
            }
        }
    }
