/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

/**
 * Thrown to indicate that this thread has found itself in contention with
 * another thread in acquiring a lock. It was determined that the other thread
 * should be allowed to acquire the lock thus this thread should release all
 * acquired locks.
 *
 * @author hr  2013.04.07
 * @since Coherence 12.1.3
 */
public class LockContentionException
        extends RuntimeException
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default no-arg constructor.
     */
    public LockContentionException()
        {
        super();
        }

    /**
     * Construct a LockContentionException with the provided message.
     *
     * @param sMsg  the detailed message
     */
    public LockContentionException(String sMsg)
        {
        super(sMsg);
        }
    }
