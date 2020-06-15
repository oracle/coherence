/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.common;

import com.tangosol.io.pof.PortableException;

import com.tangosol.util.Base;
import com.tangosol.util.WrapperException;

/**
 * Helper methods for the Coherence JCache implementation.
 *
 * @author bo  2013.11.11
 * @since Coherence 12.1.3
 */
public class Helper
    {
    /**
     * Unwraps a potentially Coherence {@link WrapperException} or
     * {@link PortableException} to produce the underlying {@link Throwable}.
     *
     * @param throwable  the potentially wrapped exception (as a {@link Throwable}
     *
     * @return an unwrapped exception (as a {@link Throwable}
     */
    public static Throwable unwrap(Throwable throwable)
        {
        if (throwable == null)
            {
            return null;
            }
        else
            {
            while (throwable instanceof PortableException || throwable instanceof WrapperException)
                {
                throwable = throwable.getCause();
                }
            }

        return throwable;
        }

    /**
     * Common method for getting current time to make it easy to change time lookup.
     *
     * @return current time in milliseconds
     */
    public static long getCurrentTimeMillis()
        {
        return Base.getSafeTimeMillis();
        }
    }
