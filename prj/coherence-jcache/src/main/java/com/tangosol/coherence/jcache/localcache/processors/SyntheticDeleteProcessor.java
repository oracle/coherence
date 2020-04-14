/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.localcache.processors;

import com.tangosol.coherence.jcache.localcache.LocalCache;

import com.tangosol.util.InvocableMap;

/**
 * An {@link com.tangosol.util.InvocableMap.EntryProcessor} to
 * clear an entry without generating a Coherence DELETE event.
 *
 * @author bo  2013.10.31
 * @since Coherence 12.1.3
 */
public class SyntheticDeleteProcessor<K, V>
        extends AbstractEntryProcessor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link SyntheticDeleteProcessor}.
     */
    public SyntheticDeleteProcessor(LocalCache cache)
        {
        super(cache);
        }

    // ----- EntryProcessor interface ---------------------------------------

    @Override
    public Object process(InvocableMap.Entry entry)
        {
        Object result = null;

        if (entry.isPresent())
            {
            result = entry.getValue();
            entry.remove(true);
            }

        return result;
        }
    }
