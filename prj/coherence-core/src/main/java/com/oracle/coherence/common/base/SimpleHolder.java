/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;

import javax.json.bind.annotation.JsonbProperty;
import java.io.Serializable;

/**
 * SimpleHolder is a basic implementation of the Holder interface.
 * <p>
 * There value is simply held by a non-volatile reference, thus SimpleHolder
 * does not provide any inter-thread visibility guarantees.
 *
 * @param <V>  the value type
 *
 * @author mf  2010.12.02
 */
public class SimpleHolder<V>
        implements Holder<V>, Serializable
    {
    /**
     * Construct a SimpleHolder with no value.
     */
    public SimpleHolder()
        {
        }

    /**
     * Construct a SimpleHolder with an initial value.
     *
     * @param value  the initial value
     */
    public SimpleHolder(V value)
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
    @JsonbProperty("value")
    protected V m_value;
    }
