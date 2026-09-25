/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.aggregator;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapTrigger;
import com.tangosol.util.Streamer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
* Calculates a number of values in an entry set.
*
* @author gg  2005.09.05
* @since Coherence 3.1
*/
public class Count<K, V>
        extends Base
        implements InvocableMap.StreamingAggregator<K, V, Integer, Integer>,
                   ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public Count()
        {
        super();
        }

    // ----- StreamingAggregator methods ----------------------------------------

    @Override
    public InvocableMap.StreamingAggregator<K, V, Integer, Integer> supply()
        {
        return new Count<>();
        }

    @Override
    public boolean accumulate(Streamer<? extends InvocableMap.Entry<? extends K, ? extends V>> streamer)
        {
        if (streamer.isSized())
            {
            m_count += streamer.size();
            return true;
            }
        else
            {
            return InvocableMap.StreamingAggregator.super.accumulate(streamer);
            }
        }

    @Override
    public boolean accumulate(InvocableMap.Entry<? extends K, ? extends V> entry)
        {
        m_count++;
        return true;
        }

    @Override
    public RetractionResult retract(InvocableMap.Entry<? extends K, ? extends V> entry)
        {
        if (entry instanceof MapTrigger.Entry
                && !((MapTrigger.Entry) entry).isOriginalPresent())
            {
            return RetractionResult.UPDATED;
            }

        if (m_count == 0)
            {
            return RetractionResult.REBUILD_REQUIRED;
            }

        m_count--;
        return RetractionResult.UPDATED;
        }

    @Override
    public RetractionResult update(InvocableMap.Entry<? extends K, ? extends V> entry)
        {
        if (entry instanceof MapTrigger.Entry
                && ((MapTrigger.Entry) entry).isOriginalPresent()
                && entry.isPresent())
            {
            return RetractionResult.UPDATED;
            }

        return InvocableMap.StreamingAggregator.super.update(entry);
        }

    @Override
    public boolean combine(Integer partialResult)
        {
        m_count += partialResult;
        return true;
        }

    @Override
    public Integer getPartialResult()
        {
        return m_count;
        }

    @Override
    public Integer finalizeResult()
        {
        int count = m_count;
        m_count = 0;
        return count;
        }

    @Override
    public Object snapshotState()
        {
        return Integer.valueOf(m_count);
        }

    @Override
    public void restoreState(Object state)
        {
        if (!(state instanceof Number) || ((Number) state).intValue() < 0)
            {
            throw new IllegalArgumentException("invalid count state snapshot");
            }
        m_count = ((Number) state).intValue();
        }

    @Override
    public int characteristics()
        {
        return PARALLEL | PRESENT_ONLY | CONTINUOUS | STATE_CHECKPOINTABLE;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        return this == o || o != null && getClass() == o.getClass();
        }

    @Override
    public int hashCode()
        {
        return getClass().hashCode();
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        }

    // ---- data members ----------------------------------------------------

    private transient int m_count;
    }
