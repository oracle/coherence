/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.stream.collectors;

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

import java.util.Set;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.json.bind.annotation.JsonbProperty;

/**
 * An implementation of {@code RemoteCollector} thata dapts a collector
 * accepting elements of type {@code U} to one accepting elements of type
 * {@code T} by applying a mapping function to each input element before
 * accumulation.
 *
 * @param <T>  the type of elements to be collected
 * @param <U>  type of elements accepted by downstream collector
 * @param <A>  intermediate accumulation type of the downstream
 *             collector
 * @param <R>  result type of collector
 *
 * @author as 20014.12.30
 * @since 12.2.1
 */
public class MappingCollector<T, U, A, R>
        implements RemoteCollector<T, A, R>, ExternalizableLite, PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public MappingCollector()
        {
        }

    /**
     * Construct MappingCollector instance.
     *
     * @param mapper      a function to be applied to the input elements
     * @param downstream  a downstream collector which will accept mapped values
     */
    public MappingCollector(Function<? super T, ? extends U> mapper, RemoteCollector<U, A, R> downstream)
        {
        m_mapper     = Lambdas.ensureRemotable((Remote.Function<? super T, ? extends U>) mapper);
        m_downstream = downstream;
        }

    /**
     * Construct MappingCollector instance.
     *
     * @param mapper      a function to be applied to the input elements
     * @param downstream  a downstream collector which will accept mapped values
     */
    public MappingCollector(Remote.Function<? super T, ? extends U> mapper, RemoteCollector<U, A, R> downstream)
        {
        m_mapper     = Lambdas.ensureRemotable(mapper);
        m_downstream = downstream;
        }

    // ---- Collector interface ---------------------------------------------

    @Override
    public Supplier<A> supplier()
        {
        return m_downstream.supplier();
        }

    @Override
    public BiConsumer<A, T> accumulator()
        {
        final Function<? super T, ? extends U> mapper = m_mapper;
        final BiConsumer<A, U> accumulator = m_downstream.accumulator();

        return (a, t) -> accumulator.accept(a, mapper.apply(t));
        }

    @Override
    public BinaryOperator<A> combiner()
        {
        return m_downstream.combiner();
        }

    @Override
    public Function<A, R> finisher()
        {
        return m_downstream.finisher();
        }

    @Override
    public Set<Characteristics> characteristics()
        {
        return m_downstream.characteristics();
        }

    // ---- ExternalizableLite interface ------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_mapper     = ExternalizableHelper.readObject(in);
        m_downstream = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_mapper);
        ExternalizableHelper.writeObject(out, m_downstream);
        }

    // ---- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_mapper     = in.readObject(0);
        m_downstream = in.readObject(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_mapper);
        out.writeObject(1, m_downstream);
        }

    // ---- data members ----------------------------------------------------

    @JsonbProperty("mapper")
    protected Function<? super T, ? extends U> m_mapper;

    @JsonbProperty("downstream")
    protected RemoteCollector<U, A, R> m_downstream;
    }
