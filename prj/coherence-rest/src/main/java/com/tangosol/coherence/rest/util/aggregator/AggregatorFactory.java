/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util.aggregator;

import com.tangosol.util.InvocableMap;

/**
 * A factory for aggregators.
 *
 * @param <K> the type of the Map entry keys
 * @param <V> the type of the Map entry values
 * @param <R> the type of the value returned by the EntryAggregator
 *
 * @author vp 2011.07.07
 */
public interface AggregatorFactory<K, V, R>
    {
    /**
     * Returns an aggregator instance.
     *
     * @param asArgs  aggregator configuration arguments
     *
     * @return an aggregator instance
     */
    InvocableMap.EntryAggregator<K, V, R> getAggregator(String... asArgs);
    }