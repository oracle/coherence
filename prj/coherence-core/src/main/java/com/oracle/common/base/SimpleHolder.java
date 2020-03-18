/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.base;

/**
 * SimpleHolder is a basic implementation of the Holder interface.
 * <p>
 * There value is simply held by a non-volatile reference, thus SimpleHolder
 * does not provide any inter-thread visibility guarantees.
 *
 * @param <V>  the value type
 *
 * @author mf  2010.12.02
 * @deprecated use {@link com.oracle.coherence.common.base.SimpleHolder} instead
 */
@Deprecated
public class SimpleHolder<V>
        extends com.oracle.coherence.common.base.SimpleHolder<V>
        implements Holder<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a SimpleHolder with no value.
     */
    public SimpleHolder()
        {
        super();
        }

    /**
     * Construct a SimpleHolder with an initial value.
     *
     * @param value  the initial value
     */
    public SimpleHolder(V value)
        {
        super(value);
        }
    }
