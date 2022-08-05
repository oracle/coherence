/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
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

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

/**
 * Adds serialization support to {@link java.util.LongSummaryStatistics}.
 *
 * @author as  2014.10.09
 * @since 12.2.1
 */
public class LongSummaryStatistics
        extends java.util.LongSummaryStatistics
        implements Remote.LongConsumer, Remote.IntConsumer, com.tangosol.io.ExternalizableLite, PortableObject,
            Externalizable
    {
    // ---- helpers ---------------------------------------------------------

    private void setPrivateField(java.util.LongSummaryStatistics stats, String sFieldName, Object oValue)
            throws NoSuchFieldException, IllegalAccessException
        {
        Field field = java.util.LongSummaryStatistics.class.getDeclaredField(sFieldName);
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
            setPrivateField(this, "min", input.readLong());
            setPrivateField(this, "max", input.readLong());
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
        output.writeLong(getMin());
        output.writeLong(getMax());
        }

    // ---- PortableObject interface ----------------------------------------

    public void readExternal(PofReader reader) throws IOException
        {
        try
            {
            setPrivateField(this, "count", reader.readLong(0));
            setPrivateField(this, "sum", reader.readLong(1));
            setPrivateField(this, "min", reader.readLong(2));
            setPrivateField(this, "max", reader.readLong(3));
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
     * @param nCount  the statistics total count
     * @param nSum    the statistics total sum
     * @param nMin    the statistics min
     * @param nMax    the statistics max
     *
     * @return a new {@link LongSummaryStatistics}
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @JsonbCreator
    public static LongSummaryStatistics createLongSummaryStatistics(
            @JsonbProperty("count") long nCount,
            @JsonbProperty("sum")   long nSum,
            @JsonbProperty("min")   long nMin,
            @JsonbProperty("max")   long nMax)
            throws NoSuchFieldException, IllegalAccessException
        {
        LongSummaryStatistics statistics = new LongSummaryStatistics();
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
    }
