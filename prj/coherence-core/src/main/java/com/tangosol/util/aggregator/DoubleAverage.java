/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.aggregator;


import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
* Calculates an average for values of any numeric type extracted from a set
* of entries in a Map. All the extracted Number objects will be treated as
* Java <tt>double</tt> values. If the set of entries is empty, a
* <tt>null</tt> result is returned.
*
* @param <T>  the type of the value to extract from
*
* @author gg  2005.09.05
* @since Coherence 3.1
*/
public class DoubleAverage<T>
        extends AbstractDoubleAggregator<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public DoubleAverage()
        {
        super();
        }

    /**
    * Construct a DoubleAverage aggregator.
    *
    * @param extractor  the extractor that provides a value in the form of
    *                   any Java object that is a {@link Number}
    */
    public DoubleAverage(ValueExtractor<? super T, ? extends Number> extractor)
        {
        super(extractor);
        }

    /**
    * Construct an DoubleAverage object.
    *
    * @param sMethod  the name of the method that returns a value in the form
    *                 of any Java object that is a {@link Number}
    */
    public DoubleAverage(String sMethod)
        {
        super(sMethod);
        }

    // ----- StreamingAggregator methods ------------------------------------

    @Override
    public InvocableMap.StreamingAggregator<Object, Object, Object, Double> supply()
        {
        return new DoubleAverage<>(getValueExtractor());
        }

    @Override
    public int characteristics()
        {
        return PARALLEL | PRESENT_ONLY;
        }

    // ----- AbstractAggregator methods -------------------------------------

    /**
    * {@inheritDoc}
    */
    protected void init(boolean fFinal)
        {
        super.init(fFinal);

        m_dflResult = 0.0;
        }

    /**
    * {@inheritDoc}
    */
    protected void process(Object o, boolean fFinal)
        {
        if (o != null)
            {
            if (fFinal)
                {
                // aggregate partial results packed into a byte array
                try
                    {
                    ByteArrayReadBuffer buff =
                        new ByteArrayReadBuffer((byte[]) o);
                    DataInput in = buff.getBufferInput();

                    m_count     += in.readInt();
                    m_dflResult += in.readDouble();
                    }
                catch (IOException e)
                    {
                    throw ensureRuntimeException(e);
                    }
                }
            else
                {
                // collect partial results
                m_count++;
                m_dflResult += ((Number) o).doubleValue();
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    protected Object finalizeResult(boolean fFinal)
        {
        int    c   = m_count;
        double dfl = m_dflResult;

        if (fFinal)
            {
            // return the final aggregated result
            return c == 0 ? null : dfl / c;
            }
        else
            {
            // return partial aggregation data packed into a byte array
            try
                {
                ByteArrayWriteBuffer buff =
                    new ByteArrayWriteBuffer(12);
                DataOutput out = buff.getBufferOutput();

                out.writeInt(c);
                out.writeDouble(dfl);

                return buff.getRawByteArray();
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }
    }