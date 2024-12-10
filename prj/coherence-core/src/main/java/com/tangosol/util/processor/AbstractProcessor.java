/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.processor;

import com.tangosol.util.Base;
import com.tangosol.util.InvocableMap;

import java.util.Map;
import java.util.Set;

/**
 * An AbstractProcessor is a partial EntryProcessor implementation that provides
 * the default implementation of the {@link #processAll} method.
 *
 * @param <K> the type of the Map entry key
 * @param <V> the type of the Map entry value
 * @param <R> the type of the EntryProcessor return value
 *
 * @author cp/jh  2005.07.19
 * @since Coherence 3.1
 */
public abstract class AbstractProcessor<K, V, R>
        extends Base
        implements InvocableMap.EntryProcessor<K, V, R>
    {
    // ----- EntryProcessor interface ---------------------------------------

    /**
     * {@inheritDoc}
     *
     * Note: As of Coherence 12.2.1, this method simply delegates to the default
     * {@code processAll} implementation in {@link InvocableMap.EntryProcessor}.
     */
    public Map<K, R> processAll(Set<? extends InvocableMap.Entry<K, V>> setEntries)
        {
        return InvocableMap.EntryProcessor.super.processAll(setEntries);
        }
    }
