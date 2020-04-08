/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util.processor;

import com.tangosol.util.InvocableMap;

/**
 * A factory for processors.
 *
 * @param <K> the type of the Map entry key
 * @param <V> the type of the Map entry value
 * @param <R> the type of value returned by the EntryProcessor
 *
 * @author vp 2011.07.08
 */
public interface ProcessorFactory<K, V, R>
    {
    /**
     * Returns a processor instance.
     *
     * @param asArgs  processor configuration arguments
     *
     * @return a processor instance
     */
    InvocableMap.EntryProcessor<K, V, R> getProcessor(String... asArgs);
    }