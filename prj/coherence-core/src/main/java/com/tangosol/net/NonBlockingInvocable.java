/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.oracle.coherence.common.base.Continuation;


/**
 * NonBlockingInvocable is an {@link Invocable} that can be executed asynchronously.
 * <p>
 * The NonBlockingInvocable is designed to allow invocation service thread to
 * execute the corresponding task and get invocation result without blocking.
 *
 * @author bbc 2014-10-16
 * @since Coherence 12.2.1
 */
public interface NonBlockingInvocable
        extends Invocable
    {
    /**
     * Called exactly once by the InvocationService to execute this task.
     * The implementation must hold on the provided {@link Continuation} and
     * call {@link Continuation#proceed}, passing in the result, when the
     * execution completes.
     * <p>
     * <b>Important note:</b> failure to call the Continuation may cause the
     * caller thread to be blocked indefinitely.
     *
     * @param cont  the Continuation to call when the execution completes
     */
    public void run(Continuation cont);
    }
