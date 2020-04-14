/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;

import java.util.concurrent.TimeUnit;

/**
 * The Pollable interface describes a component which supports element removal.
 *
 * @author mf  2013.02.19
 */
public interface Pollable<E>
    {
    /**
     * Removes and returns the next element, or returns <tt>null</tt> if none is present.
     *
     * @return the next element or null
     */
    E poll();

    /**
     * Removes and returns the next element, or returns <tt>null</tt> upon timeout.
     *
     * @param timeout how long to wait before giving up, in units of
     *        <tt>unit</tt>
     * @param unit a <tt>TimeUnit</tt> determining how to interpret the
     *        <tt>timeout</tt> parameter
     *
     * @return the next element or null
     * @throws InterruptedException if interrupted while waiting
     */
    E poll(long timeout, TimeUnit unit)
            throws InterruptedException;
    }
