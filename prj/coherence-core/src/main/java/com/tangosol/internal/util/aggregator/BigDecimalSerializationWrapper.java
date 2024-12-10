/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.aggregator;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import com.tangosol.util.aggregator.BigDecimalAverage;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.math.BigDecimal;

/**
 * Wrapper used to enable consistent serialization of {@link BigDecimal} instances across all currently used formats.
 *
 * @since 12.2.1.4
 */
public final class BigDecimalSerializationWrapper
        implements ExternalizableLite, PortableObject
    {

    // ----- constructors -------------------------------------------

    /**
     * Construct a new {@link BigDecimalSerializationWrapper}.  Present only for serialization.
     */
    @SuppressWarnings("unused")
    public BigDecimalSerializationWrapper()
        {
        this(Integer.MIN_VALUE, null);
        }

    /**
     * Constructs a new {@link BigDecimalSerializationWrapper} for the provided partial result.
     *
     * @param partialResult aggregation partial result
     */
    public BigDecimalSerializationWrapper(BigDecimal partialResult)
        {
        this(Integer.MIN_VALUE, partialResult);
        }

    /**
     * Constructs a new {@link BigDecimalSerializationWrapper} for the provided partial result and the count
     * used for {@link BigDecimalAverage}.
     *
     * @param count counter for averaging
     * @param partialResult aggregation partial result
     */
    public BigDecimalSerializationWrapper(int count, BigDecimal partialResult)
        {
        m_count      = count;
        m_bigDecimal = partialResult;
        }

    // ----- accessors ----------------------------------------------

    /**
     * @return the count provided at construction time or {@link Integer#MIN_VALUE} (meaning no count set).
     */
    public int getCount()
        {
        return m_count;
        }

    /**
     * @return the {@link BigDecimal} aggregation partial result
     */
    public BigDecimal getBigDecimal()
        {
        return m_bigDecimal;
        }


    // ----- interface: ExternalizableLite --------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput dataInput) throws IOException
        {
        m_count = ExternalizableHelper.readInt(dataInput);
        if (m_count == Integer.MIN_VALUE || m_count > 0)
            {
            m_bigDecimal = ExternalizableHelper.readBigDecimal(dataInput);
            }
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput dataOutput) throws IOException
        {
        ExternalizableHelper.writeInt(dataOutput, m_count);
        if (m_count == Integer.MIN_VALUE || m_count > 0)
            {
            ExternalizableHelper.writeBigDecimal(dataOutput, m_bigDecimal);
            }
        }

    // ----- interface: PortableObject ------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader pofReader) throws IOException
        {
        m_count = pofReader.readInt(COUNT);
        if (m_count == Integer.MIN_VALUE || m_count > 0)
            {
            m_bigDecimal = pofReader.readBigDecimal(BIG_DECIMAL);
            }
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter pofWriter) throws IOException
        {
        pofWriter.writeInt(COUNT, m_count);
        if (m_count == Integer.MIN_VALUE || m_count > 0)
            {
            pofWriter.writeBigDecimal(BIG_DECIMAL, m_bigDecimal);
            }
        }

    // ----- data members -------------------------------------------

    private int m_count;
    private BigDecimal m_bigDecimal;

    // constants for POF
    private static final int COUNT = 0;
    private static final int BIG_DECIMAL = 1;
    }
