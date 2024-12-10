/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.transformer;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapEventTransformer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;

/**
 * A {@link MapEventTransformer} implementation which will send at most one
 * event per storage member per sampling interval, in order to throttle down the
 * number of events received by the slow consumer.
 *
 * @author as  2015.02.21
 * @since 12.2.1
 */
public class SamplingEventTransformer<K, V>
        extends ExternalizableHelper
        implements MapEventTransformer<K, V, V>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Deserialization constructor.
    */
    public SamplingEventTransformer()
        {
        }

    /**
     * Construct SamplingEventTransformer instance.
     *
     * @param nSamplingInterval  the sampling interval to use
     */
    public SamplingEventTransformer(int nSamplingInterval)
        {
        m_nSamplingInterval = nSamplingInterval;
        }

    // ----- MapEventTransformer methods ------------------------------------

    @Override
    public MapEvent<K, V> transform(MapEvent<K, V> event)
        {
        long nCurrentTime = Base.getSafeTimeMillis();
        synchronized (this)
            {
            if (nCurrentTime - m_nLastSentTime >= m_nSamplingInterval)
                {
                m_nLastSentTime = nCurrentTime;
                return event;
                }
            }

        return null;
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_nSamplingInterval = readInt(in);
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        out.writeInt(m_nSamplingInterval);
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_nSamplingInterval = in.readInt(0);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeInt(0, m_nSamplingInterval);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The sampling interval to use.
     */
    @JsonbProperty("samplingInterval")
    protected int m_nSamplingInterval;

    /**
     * The last time an event was sent.
     */
    protected transient long m_nLastSentTime;
    }
