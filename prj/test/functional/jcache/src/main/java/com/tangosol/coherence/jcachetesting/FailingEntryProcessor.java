/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.jcachetesting;

import java.io.Serializable;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.MutableEntry;

/**
 * EntryProcessor that throws clazz exception.
 *
 * @param <K> key type
 * @param <V> value type
 * @param <T> return type
 */
public class FailingEntryProcessor<K, V, T>
        implements EntryProcessor<K, V, T>, Serializable
    {
    /**
     * Constructs ...
     *
     *
     * @param clazz
     */
    public FailingEntryProcessor(Class<? extends Throwable> clazz)
        {
        m_clazz = clazz;
        }

    /**
     * EntryProcessor that throws clazz exception .
     *
     * @param entry  entry
     * @param arguments optional arguments
     *
     * @return  nothing.  always throws specified exception for testing
     *          exception handling of entry processor.
     */
    @Override
    public T process(MutableEntry<K, V> entry, Object... arguments)
        {
        try
            {
            throw m_clazz.newInstance();
            }
        catch (Throwable t)
            {
            // implementation must wrap this for test to succeed
            throw new RuntimeException(t);
            }
        }

    /**
     * Return class
     *
     * @return return the exception class thrown by this entry processor
     */
    public Class<? extends Throwable> getClazz()
        {
        return m_clazz;
        }

    // ----- data members ---------------------------------------------------

    /**
     * exception thrown by this entry processor testing exception handling.
     */
    private final Class<? extends Throwable> m_clazz;
    }
