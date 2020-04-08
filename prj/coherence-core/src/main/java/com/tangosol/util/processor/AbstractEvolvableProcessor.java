/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.processor;

import com.tangosol.io.AbstractEvolvable;

import com.tangosol.io.pof.EvolvablePortableObject;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMap.Entry;

import java.util.Map;
import java.util.Set;

/**
 * An Evolvable AbstractProcessor that is a partial EntryProcessor implementation that provides
 * the default implementation of the {@link #processAll} method.
 *
 * @param <K>  the type of the Map entry key
 * @param <V>  the type of the Map entry value
 * @param <R>  the type of value returned by the EntryProcessor
 *
 * @author jf 2019.11.21
 * @since Coherence 14.1.1
 */
public abstract class AbstractEvolvableProcessor<K, V, R>
    extends AbstractEvolvable
    implements InvocableMap.EntryProcessor<K, V, R>, EvolvablePortableObject
    {
    // ----- EntryProcessor interface ---------------------------------------

    @Override
    public R process(Entry<K, V> entry)
        {
        return null;
        }

    /**
     * {@inheritDoc}
     */
    public Map<K, R> processAll(Set<? extends Entry<K, V>> setEntries)
        {
        return InvocableMap.EntryProcessor.super.processAll(setEntries);
        }
    }
