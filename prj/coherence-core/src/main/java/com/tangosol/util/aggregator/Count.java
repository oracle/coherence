/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.aggregator;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.InvocableMap;
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
    public int characteristics()
        {
        return PARALLEL | PRESENT_ONLY;
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