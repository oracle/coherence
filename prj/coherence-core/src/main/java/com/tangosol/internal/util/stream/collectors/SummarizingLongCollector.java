/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.stream.collectors;

import com.tangosol.internal.util.LongSummaryStatistics;

import com.tangosol.internal.util.invoke.Lambdas;

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
import java.util.EnumSet;
import java.util.Set;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

import javax.json.bind.annotation.JsonbProperty;

/**
 * An implementation of {@code RemoteCollector} which applies an
 * {@code long}-producing mapping function to each input element, and returns
 * summary statistics for the resulting values.
 *
 * @param <T>  the type of input elements to be collected
 *
 * @author as 20014.12.30
 * @since 12.2.1
 */
public class SummarizingLongCollector<T>
        implements RemoteCollector<T, LongSummaryStatistics, LongSummaryStatistics>,
                   ExternalizableLite, PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public SummarizingLongCollector()
        {
        }

    /**
     * Construct SummarizingLongCollector instance.
     *
     * @param mapper  a function extracting the property to be summarized
     */
    public SummarizingLongCollector(Remote.ToLongFunction<? super T> mapper)
        {
        m_mapper = Lambdas.ensureRemotable(mapper);
        }

    // ---- Collector interface ---------------------------------------------

    @Override
    public Supplier<LongSummaryStatistics> supplier()
        {
        return LongSummaryStatistics::new;
        }

    @Override
    public BiConsumer<LongSummaryStatistics, T> accumulator()
        {
        final ToLongFunction<? super T> mapper = m_mapper;
        return (a, t) -> a.accept(mapper.applyAsLong(t));
        }

    @Override
    public BinaryOperator<LongSummaryStatistics> combiner()
        {
        return (a, b) ->
                    {
                    a.combine(b);
                    return a;
                    };
        }

    @Override
    public Function<LongSummaryStatistics, LongSummaryStatistics> finisher()
        {
        return Function.identity();
        }

    @Override
    public Set<Characteristics> characteristics()
        {
        return S_CHARACTERISTICS;
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

    // ---- static members ----------------------------------------------------

    protected static final Set<Characteristics> S_CHARACTERISTICS =
            Collections.unmodifiableSet(EnumSet.of(Characteristics.IDENTITY_FINISH));

    // ---- data members ----------------------------------------------------

    @JsonbProperty("mapper")
    protected ToLongFunction<? super T> m_mapper;
    }
