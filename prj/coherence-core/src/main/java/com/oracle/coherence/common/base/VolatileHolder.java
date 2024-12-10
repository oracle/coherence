/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;


/**
 * VolatileHolder is a basic implementation of the Holder interface where
 * the held object is referenced from a volatile reference.
 *
 * @param <V>  the value type
 *
 * @author mf  2010.12.02
 */
public class VolatileHolder<V>
        implements Holder<V>
    {
    /**
     * Construct a VolatileHolder with no value.
     */
    public VolatileHolder()
        {
        }

    /**
     * Construct a VolatileHolder with an initial value.
     *
     * @param value  the initial value
     */
    public VolatileHolder(V value)
        {
        set(value);
        }


    // ----- Holder interface -----------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void set(V value)
        {
        m_value = value;
        }

    /**
     * {@inheritDoc}
     */
    public V get()
        {
        return m_value;
        }


    // ----- data members ---------------------------------------------------

    /**
     * The held value.
     */
    protected volatile V m_value;
    }
