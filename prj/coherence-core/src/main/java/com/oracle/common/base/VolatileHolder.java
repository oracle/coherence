/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.base;

/**
 * VolatileHolder is a basic implementation of the Holder interface where
 * the held object is referenced from a volatile reference.
 *
 * @param <V>  the value type
 *
 * @author mf  2010.12.02
 * @deprecated use {@link com.oracle.coherence.common.base.VolatileHolder} instead
 */
@Deprecated
public class VolatileHolder<V>
        extends com.oracle.coherence.common.base.VolatileHolder<V>
        implements Holder<V>
    {
    /**
     * Construct a VolatileHolder with no value.
     */
    public VolatileHolder()
        {
        super();
        }

    /**
     * Construct a VolatileHolder with an initial value.
     *
     * @param value  the initial value
     */
    public VolatileHolder(V value)
        {
        super(value);
        }
    }
