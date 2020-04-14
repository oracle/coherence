/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.stream.collectors;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.function.Remote;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

/**
 * An implementation of {@code RemoteCollector} that produces the arithmetic
 * mean of a double-valued function applied to the input elements.
 *
 * @param <T>  the type of input elements to be collected
 *
 * @author as 20014.12.30
 * @since 12.2.1
 */
public class AveragingDoubleCollector<T>
        extends SummingDoubleCollector<T>
        implements ExternalizableLite, PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public AveragingDoubleCollector()
        {
        }

    /**
     * Construct AveragingDoubleCollector instance.
     *
     * @param mapper  a function extracting the property to be averaged
     */
    public AveragingDoubleCollector(Remote.ToDoubleFunction<? super T> mapper)
        {
        super(mapper);
        }

    // ---- Collector interface ---------------------------------------------

    @Override
    public Supplier<double[]> supplier()
        {
        return () -> new double[4];
        }

    @Override
    public BiConsumer<double[], T> accumulator()
        {
        ToDoubleFunction<? super T> mapper = m_mapper;
        return (a, t) ->
                    {
                    double dblValue = mapper.applyAsDouble(t);
                    sumWithCompensation(a, dblValue);
                    a[2]++;
                    a[3] += dblValue;
                    };
        }

    @Override
    public BinaryOperator<double[]> combiner()
        {
        return (a, b) ->
                    {
                    sumWithCompensation(a, b[0]);
                    sumWithCompensation(a, b[1]);
                    a[2] += b[2];
                    a[3] += b[3];
                    return a;
                    };
        }

    @Override
    public Function<double[], Double> finisher()
        {
        return a -> (a[2] == 0) ? 0.0d : (computeFinalSum(a) / a[2]);
        }
    }
