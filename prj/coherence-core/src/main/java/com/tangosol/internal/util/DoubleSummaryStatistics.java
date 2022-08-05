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
 * Adds serialization support to {@link java.util.DoubleSummaryStatistics}.
 *
 * @author as  2014.10.09
 * @since 12.2.1
 */
public class DoubleSummaryStatistics
        extends java.util.DoubleSummaryStatistics
        implements Remote.DoubleConsumer, com.tangosol.io.ExternalizableLite, PortableObject, Externalizable
    {
    // ---- helpers ---------------------------------------------------------

    protected double getPrivateField(java.util.DoubleSummaryStatistics stats, String sFieldName)
            throws NoSuchFieldException, IllegalAccessException
        {
        Field field = java.util.DoubleSummaryStatistics.class.getDeclaredField(sFieldName);
        field.setAccessible(true);
        return (double) field.get(stats);
        }

    protected void setPrivateField(java.util.DoubleSummaryStatistics stats, String sFieldName, Object oValue)
            throws NoSuchFieldException, IllegalAccessException
        {
        Field field = java.util.DoubleSummaryStatistics.class.getDeclaredField(sFieldName);
        field.setAccessible(true);
        field.set(stats, oValue);
        }

    // ---- ExternalizableLite interface ------------------------------------

    @Override
    public void readExternal(DataInput input) throws IOException
        {
        try
            {
            setPrivateField(this, "count", input.readLong());
            setPrivateField(this, "sum", input.readDouble());
            setPrivateField(this, "sumCompensation", input.readDouble());
            setPrivateField(this, "simpleSum", input.readDouble());
            setPrivateField(this, "min", input.readDouble());
            setPrivateField(this, "max", input.readDouble());
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

    @Override
    public void writeExternal(DataOutput output) throws IOException
        {
        try
            {
            output.writeLong(getCount());
            output.writeDouble(getSum());
            output.writeDouble(getPrivateField(this, "sumCompensation"));
            output.writeDouble(getPrivateField(this, "simpleSum"));
            output.writeDouble(getMin());
            output.writeDouble(getMax());
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

    // ---- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader reader) throws IOException
        {
        try
            {
            setPrivateField(this, "count", reader.readLong(0));
            setPrivateField(this, "sum", reader.readDouble(1));
            setPrivateField(this, "sumCompensation", reader.readDouble(2));
            setPrivateField(this, "simpleSum", reader.readDouble(3));
            setPrivateField(this, "min", reader.readDouble(4));
            setPrivateField(this, "max", reader.readDouble(5));
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

    @Override
    public void writeExternal(PofWriter writer) throws IOException
        {
        try
            {
            writer.writeLong(0, getCount());
            writer.writeDouble(1, getSum());
            writer.writeDouble(2, getPrivateField(this, "sumCompensation"));
            writer.writeDouble(3, getPrivateField(this, "simpleSum"));
            writer.writeDouble(4, getMin());
            writer.writeDouble(5, getMax());
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
     * Create an {@link DoubleSummaryStatistics} instance from the annotated Json attributes.
     *
     * @param nCount  the statistics total count
     * @param nSum    the statistics total sum
     * @param nMin    the statistics min
     * @param nMax    the statistics max
     *
     * @return a new {@link DoubleSummaryStatistics}
     *
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @JsonbCreator
    public static DoubleSummaryStatistics createDoubleSummaryStatistics(
            @JsonbProperty("sumCompensation") double nSumCompensation,
            @JsonbProperty("simpleSum")       double nSimpleSum,
            @JsonbProperty("count")           long nCount,
            @JsonbProperty("sum")             double nSum,
            @JsonbProperty("min")             double  nMin,
            @JsonbProperty("max")             double  nMax)
            throws NoSuchFieldException, IllegalAccessException
        {
        DoubleSummaryStatistics statistics = new DoubleSummaryStatistics();
        statistics.setPrivateField(statistics, "sumCompensation", nSumCompensation);
        statistics.setPrivateField(statistics, "simpleSum", nSimpleSum);
        statistics.setPrivateField(statistics, "count", nCount);
        statistics.setPrivateField(statistics, "sum",   nSum);
        statistics.setPrivateField(statistics, "min",   nMin);
        statistics.setPrivateField(statistics, "max",   nMax);

        return statistics;
        }

    // ----- accessors for JsonSerialization --------------------------------

    /**
     * Returns the sumCompensation Json attribute.
     *
     * @return the sumCompensation Json attribute
     */
    @JsonbProperty("sumCompensation")
    private double getSumCompensation()
        {
        try
            {
            return getPrivateField(this, "sumCompensation");
            }
        catch (Exception e)
            {
            return 0.0d;
            }
        }

    /**
     * Returns the simpleSum Json attribute.
     *
     * @return the simpleSum Json attribute
     */
    @JsonbProperty("simpleSum")
    private double getSimpleSum()
        {
        try
            {
            return getPrivateField(this, "simpleSum");
            }
        catch (Exception e)
            {
            return 0.0d;
            }
        }

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
    private double getSumProperty()
        {
        return getSum();
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
    }
