/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.common;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.cache.CacheEvent;

import com.tangosol.util.filter.EntryFilter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Map;

/**
 * Server side filter to filter out both coherence and jcache synthetic events.
 *
 * @author jf  2013.12.20
 * @since Coherence 12.1.3
 */
public abstract class NonSyntheticEntryFilter<T>
        implements EntryFilter<Object, T>, PortableObject, ExternalizableLite
    {
    // ----- EntryFilter interface ------------------------------------------

    @Override
    public boolean evaluate(T o)
        {
        boolean fResult = true;

        // filter out synthetic events.
        // currently filtering out deleted entries that were expired using entry.expire(1L).
        if (o instanceof CacheEvent)
            {
            CacheEvent evt = (CacheEvent) o;

            if (evt.isSynthetic())
                {
                return false;
                }
            else if (evt.getId() == CacheEvent.ENTRY_UPDATED)
                {
                fResult = !isJCacheSynthetic(evt);
                }
            }

        return fResult;
        }

    @Override
    public boolean evaluateEntry(Map.Entry entry)
        {
        return true;
        }

    // ----- NonSyntheticEntryFilter methods --------------------------------

    abstract public boolean isJCacheSynthetic(CacheEvent evt);

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader pofReader)
            throws IOException
        {
        }

    @Override
    public void writeExternal(PofWriter pofWriter)
            throws IOException
        {
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        }
    }
