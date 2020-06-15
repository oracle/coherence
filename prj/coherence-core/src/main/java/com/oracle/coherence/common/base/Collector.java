/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;


/**
 * A Collector is mechanism for receiving items.
 *
 * @param <V> the collected type
 *
 * @author mf  2010.10.06
 */
public interface Collector<V>
    {
    /**
     * Notify the collector of a item of interest.
     *
     * @param value  the item to collect
     */
    public void add(V value);

    /**
     * Request processing of any added values.
     * <p>
     * This method should be called after a call or series of calls to {@link #add}.
     */
    public default void flush() {};
    }
