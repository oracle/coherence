/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

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

import java.lang.reflect.Field;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;

/**
 * Adds serialization support to {@link java.util.IntSummaryStatistics}.
 *
 * @author as  2014.10.09
 * @since 12.2.1
 */
public class IntSummaryStatistics
        extends java.util.IntSummaryStatistics
        implements Remote.IntConsumer, com.tangosol.io.ExternalizableLite, PortableObject, Externalizable
    {
    // ---- helpers ---------------------------------------------------------

    private void setPrivateField(java.util.IntSummaryStatistics stats, String sFieldName, Object oValue)
            throws NoSuchFieldException, IllegalAccessException
        {
        Field field = java.util.IntSummaryStatistics.class.getDeclaredField(sFieldName);
        field.setAccessible(true);
        field.set(stats, oValue);
        }

    // ---- ExternalizableLite interface ------------------------------------

    public void readExternal(DataInput input) throws IOException
        {
        try
            {
            setPrivateField(this, "count", input.readLong());
            setPrivateField(this, "sum", input.readLong());
            setPrivateField(this, "min", input.readInt());
            setPrivateField(this, "max", input.readInt());
            }
        catch (IOException e)
            {
            throw e;
            }
        catch (Exception e)
            {
            throw new IOException(e);
            }
        }

    public void writeExternal(DataOutput output) throws IOException
        {
        output.writeLong(getCount());
        output.writeLong(getSum());
        output.writeInt (getMin());
        output.writeInt (getMax());
        }

    // ---- PortableObject interface ----------------------------------------

    public void readExternal(PofReader reader) throws IOException
        {
        try
            {
            setPrivateField(this,  "count", reader.readLong(0));
            setPrivateField(this, "sum", reader.readLong(1));
            setPrivateField(this, "min", reader.readInt(2));
            setPrivateField(this, "max", reader.readInt(3));
            }
        catch (IOException e)
            {
            throw e;
            }
        catch (Exception e)
            {
            throw new IOException(e);
            }
        }

    public void writeExternal(PofWriter writer) throws IOException
        {
        writer.writeLong(0, getCount());
        writer.writeLong(1, getSum());
        writer.writeInt (2, getMin());
        writer.writeInt (3, getMax());
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
     * Create an {@link IntSummaryStatistics} instance from the annotated Json attributes.
     *
     * @param nCount  the statistics total count
     * @param nSum    the statistics total sum
     * @param nMin    the statistics min
     * @param nMax    the statistics max
     *
     * @return a new {@link IntSummaryStatistics}
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @JsonbCreator
    public static IntSummaryStatistics createIntSummaryStatistics(
            @JsonbProperty("count") long nCount,
            @JsonbProperty("sum")   long nSum,
            @JsonbProperty("min")   int  nMin,
            @JsonbProperty("max")   int  nMax)
            throws NoSuchFieldException, IllegalAccessException
        {
        IntSummaryStatistics statistics = new IntSummaryStatistics();
        statistics.setPrivateField(statistics, "count", nCount);
        statistics.setPrivateField(statistics, "sum",   nSum);
        statistics.setPrivateField(statistics, "min",   nMin);
        statistics.setPrivateField(statistics, "max",   nMax);

        return statistics;
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
    private int getMinProperty()
        {
        return getMin();
        }

    /**
     * Returns the max Json attribute.
     *
     * @return the max Json attribute
     */
    @JsonbProperty("max")
    private int getMaxProperty()
        {
        return getMax();
        }
    }
