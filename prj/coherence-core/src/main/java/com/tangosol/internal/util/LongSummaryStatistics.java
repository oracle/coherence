/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.SerializationSupport;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.function.Remote;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;

/**
 * Adds serialization support to {@link java.util.LongSummaryStatistics}.
 *
 * @author as  2014.10.09
 * @since 12.2.1
 */
public class LongSummaryStatistics
        extends java.util.LongSummaryStatistics
        implements Remote.LongConsumer, Remote.IntConsumer,
                   SerializationSupport,
                   ExternalizableLite, PortableObject, Externalizable
    {
    // ----- Constructors ---------------------------------------------------

    /**
     * Constructs an empty instance with zero count, zero sum,
     * {@code Long.MAX_VALUE} min, {@code LONG.MIN_VALUE}
     * max and zero average.
     */
    public LongSummaryStatistics()
        {
        super();
        }

    /**
     * Constructs a non-empty instance with the specified {@code count},
     * {@code min}, {@code max}, and {@code sum}.
     *
     * <p>If {@code count} is zero then the remaining arguments are ignored and
     * an empty instance is constructed.
     *
     * <p>If the arguments are inconsistent then an {@code IllegalArgumentException}
     * is thrown.  The necessary consistent argument conditions are:
     * <ul>
     *   <li>{@code count >= 0}</li>
     *   <li>{@code min <= max}</li>
     * </ul>
     *
     * @param count the count of values
     * @param min   the minimum value
     * @param max   the maximum value
     * @param sum   the sum of all values
     *
     * @throws IllegalArgumentException if the arguments are inconsistent
     * @since 22.06.5
     */
    public LongSummaryStatistics(long count, long min, long max, long sum)
        {
        // super class constructor added since JDK 10
        super(count, min, max, sum);
        m_cCount = count;
        m_lMin   = min;
        m_lMax   = max;
        m_lSum   = sum;
        }

    // ---- SerializationSupport interface ----------------------------------

    public Object readResolve() throws ObjectStreamException
        {
        // create a new instance from the temp fields we deserialized into
        return new LongSummaryStatistics(m_cCount, m_lMin, m_lMax, m_lSum);
        }

    // ---- ExternalizableLite interface ------------------------------------

    @Override
    public void readExternal(DataInput input) throws IOException
        {
        m_cCount = input.readLong();
        m_lSum   = input.readLong();
        m_lMin   = input.readLong();
        m_lMax   = input.readLong();
        }

    @Override
    public void writeExternal(DataOutput output) throws IOException
        {
        output.writeLong(getCount());
        output.writeLong(getSum());
        output.writeLong(getMin());
        output.writeLong(getMax());
        }

    // ---- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader reader) throws IOException
        {
        m_cCount = reader.readLong(0);
        m_lSum   = reader.readLong(1);
        m_lMin   = reader.readLong(2);
        m_lMax   = reader.readLong(3);
        }

    @Override
    public void writeExternal(PofWriter writer) throws IOException
        {
        writer.writeLong(0, getCount());
        writer.writeLong(1, getSum());
        writer.writeLong(2, getMin());
        writer.writeLong(3, getMax());
        }

    // ---- Externalizable interface ----------------------------------------

    @Override
    public void writeExternal(ObjectOutput out)
        throws IOException
        {
        this.writeExternal((DataOutput) out);
        }

    @Override
    public void readExternal(ObjectInput in)
        throws IOException
        {
        this.readExternal((DataInput) in);
        }

    // ----- Json Serialization ---------------------------------------------

    /**
     * Create an {@link LongSummaryStatistics} instance from the annotated Json attributes.
     *
     * @param count  the statistics total count
     * @param sum    the statistics total sum
     * @param min    the statistics min
     * @param max    the statistics max
     *
     * @return a new {@link LongSummaryStatistics}
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @JsonbCreator
    public static LongSummaryStatistics createLongSummaryStatistics(
            @JsonbProperty("count") long count,
            @JsonbProperty("sum")   long sum,
            @JsonbProperty("min")   long min,
            @JsonbProperty("max")   long max)
            throws NoSuchFieldException, IllegalAccessException
        {
        return new LongSummaryStatistics(count, min, max, sum);
        }

    // ----- accessors for JsonSerialization --------------------------------

    /**
     * Returns the count Json attribute.
     *
     * @return the count Json attribute
     */
    @JsonbProperty("count")
    private long getCountProperty()
        {
        return getCount();
        }

    /**
     * Returns the sum Json attribute.
     *
     * @return the sum Json attribute
     */
    @JsonbProperty("sum")
    private long getSumProperty()
        {
        return getSum();
        }

    /**
     * Returns the min Json attribute.
     *
     * @return the min Json attribute
     */
    @JsonbProperty("min")
    private long getMinProperty()
        {
        return getMin();
        }

    /**
     * Returns the max Json attribute.
     *
     * @return the max Json attribute
     */
    @JsonbProperty("max")
    private long getMaxProperty()
        {
        return getMax();
        }

    // ----- constants -------------------------------------------------------

    private static final long serialVersionUID = 6685745411569023906L;

    // ----- data members ----------------------------------------------------

    /*
     * The fields below are only used temporarily during deserialization,
     * to read data into from the input before calling readResolve to create the
     * actual instance of this class that will be used as the final deserialization
     * result.
     */

    @JsonbTransient
    private transient long m_cCount;

    @JsonbTransient
    private transient long m_lSum;

    @JsonbTransient
    private transient long m_lMin = Long.MAX_VALUE;

    @JsonbTransient
    private transient long m_lMax = Long.MIN_VALUE;
    }
