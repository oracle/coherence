/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;


import com.oracle.coherence.common.base.Continuation;
import com.oracle.coherence.common.base.NonBlocking;


/**
 * Communication facilities that provide an asynchronous (non-blocking) way
 * of submitting data exchange requests commonly implement mechanisms of
 * modulating the control flow for underlying data transfer units
 * (e.g. messages or packets). Those mechanism usually include some type of
 * request buffering and backlog detection.
 * <p>
 * While in many cases it's desirable to automate the flow control algorithms,
 * such automation may be sub-optimal (in a case of "auto-flush") or even
 * completely objectionable (in a case of backlog-related delays if the caller
 * is a part of an asynchronous communication flow by itself).
 * <p>
 * FlowControl represents a facet of a communication end point that allows clients
 * to opt-out from an automatic flow control and manually govern the rate of the
 * request flow.
 * <p>
 * Callers wishing to be exempt from automatic flow-control may declare themselves as
 * {@link com.oracle.coherence.common.base.NonBlocking non-blocking}, code directly interacting
 * with flow-control methods is expected to {@link NonBlocking#isNonBlockingCaller() check}
 * if the calling thread has been marked as non-blocking and bypass automatic flow-control
 * for such callers.
 *
 * @author gg/mf/rhl 2013.01.09
 */
public interface FlowControl
    {
    /**
     * Ensure that any buffered asynchronous operations are dispatched to the
     * underlying tier.
     * <p>
     * Note: this is a non-blocking call.
     */
    public void flush();

    /**
     * Check for an excessive backlog and allow blocking the calling thread for
     * up to the specified amount of time.
     *
     * @param cMillis the maximum amount of time to wait (in milliseconds), or
     *                zero for infinite wait
     *
     * @return the remaining timeout or a negative value if timeout has occurred
     *         (the return of zero is only allowed for infinite timeout and
     *          indicates that the backlog is no longer excessive)
     */
    public long drainBacklog(long cMillis);

    /**
     * Check for an excessive backlog and if the underlying communication channel
     * is indeed clogged, call the specified continuation when the backlog is
     * back to normal or the service terminates. It's important to remember that:
     * <ol>
     *   <li>The continuation could be called on any thread; concurrently with
     *       the calling thread or on the calling thread itself.
     *   <li>The continuation is called if and only if this method returns
     *       <code>true</code>.
     *   <li>The continuation <b>must not</b> make any blocking calls.
     * </ol>
     *
     * @param continueNormal  (optional) {@link Continuation} to be called when
     *                        the backlog has been reduced back to normal
     *
     * @return true if the underlying communication channel is backlogged;
     *         false otherwise
     *
     * @since Coherence 12.1.3
     */
    public boolean checkBacklog(Continuation<Void> continueNormal);
    }
