/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.stream.collectors;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import com.tangosol.util.function.Remote;

import com.tangosol.util.stream.RemoteCollector;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Collections;
import java.util.Set;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

import javax.json.bind.annotation.JsonbProperty;

/**
 * An implementation of {@code RemoteCollector} that produces the sum of a
 * double-valued function applied to the input elements.
 *
 * @param <T>  the type of input elements to be collected
 *
 * @author as 20014.12.30
 * @since 12.2.1
 */
public class SummingDoubleCollector<T>
        implements RemoteCollector<T, double[], Double>, ExternalizableLite, PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public SummingDoubleCollector()
        {
        }

    /**
     * Construct SummingDoubleCollector instance.
     *
     * @param mapper  a function extracting the property to be summed
     */
    public SummingDoubleCollector(Remote.ToDoubleFunction<? super T> mapper)
        {
        m_mapper = mapper;
        }

    // ---- Collector interface ---------------------------------------------

    @Override
    public Supplier<double[]> supplier()
        {
        return () -> new double[3];
        }

    @Override
    public BiConsumer<double[], T> accumulator()
        {
        ToDoubleFunction<? super T> mapper = m_mapper;
        return (a, t) ->
                    {
                    double dblValue = mapper.applyAsDouble(t);
                    sumWithCompensation(a, dblValue);
                    a[2] += dblValue;
                    };
        }

    @Override
    public BinaryOperator<double[]> combiner()
        {
        return (a, b) ->
                    {
                    sumWithCompensation(a, b[0]);
                    a[2] += b[2];
                    return sumWithCompensation(a, b[1]);
                    };
        }

    @Override
    public Function<double[], Double> finisher()
        {
        return SummingDoubleCollector::computeFinalSum;
        }

    @Override
    public Set<Characteristics> characteristics()
        {
        return Collections.emptySet();
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Incorporate a new double value using Kahan summation / compensation
     * summation.
     * <p>
     * High-order bits of the sum are in intermediateSum[0], low-order bits of
     * the sum are in intermediateSum[1], any additional elements are
     * application-specific.
     *
     * @param intermediateSum the high-order and low-order words of the
     *                        intermediate sum
     * @param value           the name value to be included in the running sum
     */
    protected static double[] sumWithCompensation(double[] intermediateSum, double value)
        {
        double tmp = value - intermediateSum[1];
        double sum = intermediateSum[0];
        double velvel = sum + tmp; // Little wolf of rounding error
        intermediateSum[1] = (velvel - sum) - tmp;
        intermediateSum[0] = velvel;
        return intermediateSum;
        }

    /**
     * If the compensated sum is spuriously NaN from accumulating one or more
     * same-signed infinite values, return the correctly-signed infinity stored
     * in the simple sum.
     */
    protected static double computeFinalSum(double[] summands)
        {
        // Better error bounds to add both terms as the final sum
        double tmp = summands[0] + summands[1];
        double simpleSum = summands[summands.length - 1];
        if (Double.isNaN(tmp) && Double.isInfinite(simpleSum))
            {
            return simpleSum;
            }
        else
            {
            return tmp;
            }
        }

    // ---- ExternalizableLite interface ------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_mapper = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_mapper);
        }

    // ---- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_mapper = in.readObject(0);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_mapper);
        }

    // ---- data members ----------------------------------------------------

    @JsonbProperty("mapper")
    protected ToDoubleFunction<? super T> m_mapper;
    }
