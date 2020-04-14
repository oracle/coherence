/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.net.Member;

import java.util.Set;


/**
 * A null implementation of  MessagePublisher.
 *
 * @author mf  2013.09.24
 */
public class NullMessagePublisher<M, D>
    implements MessagePublisher<M, D>
    {
    @Override
    public boolean post(M msg)
        {
        return false;
        }

    @Override
    public void flush()
        {
        }

    @Override
    public long drainOverflow(Set<D> setDest, long cMillisTimeout)
            throws InterruptedException
        {
        return cMillisTimeout;
        }

    /**
     * Singleton instance.
     */
    public static final NullMessagePublisher INSTANCE = new NullMessagePublisher();
    }
