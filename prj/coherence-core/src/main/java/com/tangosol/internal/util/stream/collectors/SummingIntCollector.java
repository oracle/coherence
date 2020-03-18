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
import java.util.function.ToIntFunction;

import javax.json.bind.annotation.JsonbProperty;

/**
 * An implementation of {@code RemoteCollector} that produces the sum of a
 * integer-valued function applied to the input elements.
 *
 * @param <T>  the type of input elements to be collected
 *
 * @author as 20014.12.30
 * @since 12.2.1
 */
public class SummingIntCollector<T>
        implements RemoteCollector<T, int[], Integer>, ExternalizableLite, PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public SummingIntCollector()
        {
        }

    /**
     * Construct SummingIntCollector instance.
     *
     * @param mapper  a function extracting the property to be summed
     */
    public SummingIntCollector(Remote.ToIntFunction<? super T> mapper)
        {
        m_mapper = mapper;
        }

    // ---- Collector interface ---------------------------------------------

    @Override
    public Supplier<int[]> supplier()
        {
        return () -> new int[1];
        }

    @Override
    public BiConsumer<int[], T> accumulator()
        {
        ToIntFunction<? super T> mapper = m_mapper;
        return (a, t) -> a[0] += mapper.applyAsInt(t);
        }

    @Override
    public BinaryOperator<int[]> combiner()
        {
        return (a, b) ->
                    {
                    a[0] += b[0];
                    return a;
                    };
        }

    @Override
    public Function<int[], Integer> finisher()
        {
        return a -> a[0];
        }

    @Override
    public Set<Characteristics> characteristics()
        {
        return Collections.emptySet();
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
    protected ToIntFunction<? super T> m_mapper;
    }
