/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import java.util.Set;


/**
 * MessagePublisher is an abstraction for publishing messages.
 *
 * @param <M> the message type
 * @param <D> the destination type
 *
 * @author mf  2013.09.24
 */
public interface MessagePublisher<M, D>
    {
    /**
     * Submit a message for publishing.
     *
     * @param msg  the message, must be an instance of Component.Net.Message
     *
     * @return true if the message was accepted; false on reject
     */
    public boolean post(M msg);

    /**
     * Inform the publisher that the caller has no more messages to publish at this time.
     * <p>
     * Any caller of {@link #post} must eventually call {@link #flush}.
     * </p>
     */
    public void flush();

    /**
     * Allow the publisher to hold the calling thread for up to the specified timeout if the
     * publisher is "backlogged"
     *
     * @param setDest         the set of members the sender is interested in (must be a Component.Net.MemberSet), or null
     * @param cMillisTimeout  the maximum time the thread wishes to be held, or 0 for infinite
     *
     * @return the remaining timeout
     */
    public long drainOverflow(Set<D> setDest, long cMillisTimeout)
        throws InterruptedException;
    }
