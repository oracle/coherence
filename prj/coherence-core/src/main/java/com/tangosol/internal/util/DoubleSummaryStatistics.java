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
 * Adds serialization support to {@link java.util.DoubleSummaryStatistics}.
 *
 * @author as  2014.10.09
 * @since 12.2.1
 */
public class DoubleSummaryStatistics
        extends java.util.DoubleSummaryStatistics
        implements Remote.DoubleConsumer, SerializationSupport,
                   ExternalizableLite, PortableObject, Externalizable
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Constructs an empty instance with zero count, zero sum,
     * {@code Double.POSITIVE_INFINITY} min, {@code Double.NEGATIVE_INFINITY}
     * max and zero average.
     */
    public DoubleSummaryStatistics()
        {
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
     *   <li>{@code (min <= max && !isNaN(sum)) || (isNaN(min) && isNaN(max) && isNaN(sum))}</li>
     * </ul>
     *
     * @param count  the count of values
     * @param min    the minimum value
     * @param max    the maximum value
     * @param sum    the sum of all values
     *
     * @throws IllegalArgumentException if the arguments are inconsistent
     * @since 22.06.5
     */
    public DoubleSummaryStatistics(long count, double min, double max, double sum)
        {
        super(count, min, max, sum);
        m_cCount = count;
        m_dMin   = min;
        m_dMax   = max;
        m_dSum   = sum;
        }

    // ---- SerializationSupport interface ----------------------------------

    public Object readResolve() throws ObjectStreamException
        {
        // create a new instance from the temp fields we deserialized into
        return new DoubleSummaryStatistics(m_cCount, m_dMin, m_dMax, m_dSum);
        }

    // ---- ExternalizableLite interface ------------------------------------

    @Override
    public void readExternal(DataInput input) throws IOException
        {
        m_cCount = input.readLong();
        m_dSum   = input.readDouble();
        input.readDouble(); // for backwards compatibility: sumCompensation
        input.readDouble(); // for backwards compatibility: simpleSum
        m_dMin   = input.readDouble();
        m_dMax   = input.readDouble();
        }

    @Override
    public void writeExternal(DataOutput output) throws IOException
        {
        output.writeLong(getCount());
        output.writeDouble(getSum());
        output.writeDouble(0.0d);  // for backwards compatibility: sumCompensation
        output.writeDouble(getSum()); // for backwards compatibility: simpleSum
        output.writeDouble(getMin());
        output.writeDouble(getMax());
        }

    // ---- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader reader) throws IOException
        {
        m_cCount = reader.readLong(0);
        m_dSum   = reader.readDouble(1);
        reader.readDouble(2); // for backwards compatibility: sumCompensation
        reader.readDouble(3); // for backwards compatibility: simpleSum
        m_dMin   = reader.readDouble(4);
        m_dMax   = reader.readDouble(5);
        }

    @Override
    public void writeExternal(PofWriter writer) throws IOException
        {
        writer.writeLong(0, getCount());
        writer.writeDouble(1, getSum());
        writer.writeDouble(2, 0.0d);  // for backwards compatibility: sumCompensation
        writer.writeDouble(3, getSum());  // for backwards compatibility: simpleSum
        writer.writeDouble(4, getMin());
        writer.writeDouble(5, getMax());
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
     * Create an {@link DoubleSummaryStatistics} instance from the annotated JSON attributes.
     *
     * @param count  the count of values
     * @param min    the minimum value
     * @param max    the maximum value
     * @param sum    the sum of all values
     *
     * @return a new {@link DoubleSummaryStatistics}
     */
    @JsonbCreator
    public static DoubleSummaryStatistics createDoubleSummaryStatistics(
            @JsonbProperty("count") long   count,
            @JsonbProperty("min")   double min,
            @JsonbProperty("max")   double max,
            @JsonbProperty("sum")   double sum)
        {
        return new DoubleSummaryStatistics(count, min, max, sum);
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
     * Returns the min Json attribute.
     *
     * @return the min Json attribute
     */
    @JsonbProperty("min")
    private double getMinProperty()
        {
        return getMin();
        }

    /**
     * Returns the max Json attribute.
     *
     * @return the max Json attribute
     */
    @JsonbProperty("max")
    private double getMaxProperty()
        {
        return getMax();
        }

    /**
     * Returns the sum Json attribute.
     *
     * @return the sum Json attribute
     */
    @JsonbProperty("sum")
    private double getSumProperty()
        {
        return getSum();
        }

    // ----- constants -------------------------------------------------------

    /**
     * Added since updated class and it implements {@link Externalizable}.
     */
    private static final long serialVersionUID = -697250990486066143L;

    // ---- data members ----------------------------------------------------

    /*
     * The fields below are only used temporarily during deserialization,
     * to read data into from the input before calling readResolve to create the
     * actual instance of this class that will be used as the final deserialization
     * result.
     */

    @JsonbTransient
    private transient long   m_cCount;
    @JsonbTransient
    private transient double m_dMin = Double.POSITIVE_INFINITY;
    @JsonbTransient
    private transient double m_dMax = Double.NEGATIVE_INFINITY;
    @JsonbTransient
    private transient double m_dSum;
    }
