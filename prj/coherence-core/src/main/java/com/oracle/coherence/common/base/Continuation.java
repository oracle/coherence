/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;


/**
 * Continuation interface is used to implement asynchronous post-processing,
 * the pattern that is also known as the
 * <a href="http://en.wikipedia.org/wiki/Continuation-passing_style">
 * "Continuation-passing style"</a>.
 * <p>
 * Most commonly, a continuation can be used to encode a single program control
 * mechanism (i.e. a logical "return").  Advanced usages may also need to encode
 * multiple control mechanisms (e.g. an exceptional execution path).  For such
 * usages, each control path could be explicitly represented as a separate
 * Continuation, e.g.:
 * <pre>
 * void doAsync(Continuation&lt;Result&gt; contNormal, Continuation&lt;Exception&gt; contExceptional);
 * </pre>
 *
 * @param <R> the result type
 *
 * @author gg 02.17.2011
 */
public interface Continuation<R>
    {
    /**
     * Resume the execution after the completion of an asynchronous call.
     *
     * @param r  the result of the execution preceding this continuation
     */
    public void proceed(R r);
    }