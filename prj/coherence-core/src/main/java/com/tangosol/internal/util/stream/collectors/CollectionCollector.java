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

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.json.bind.annotation.JsonbProperty;

/**
 * Simple implementation of {@code RemoteCollector} that collects stream
 * entries into a supplied collection.
 *
 * @param <T> the type of elements to be collected
 * @param <C> the type of the collection to collect into
 *
 * @author as 20014.12.30
 * @since 12.2.1
 */
public class CollectionCollector<T, C extends Collection<T>>
        implements RemoteCollector<T, C, C>, ExternalizableLite, PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public CollectionCollector()
        {
        }

    /**
     * Construct CollectionCollector instance.
     *
     * @param supplier         the supplier to use
     */
    public CollectionCollector(Supplier<C> supplier)
        {
        m_supplier = supplier;
        }

    /**
     * Construct CollectionCollector instance.
     *
     * @param supplier         the supplier to use
     */
    public CollectionCollector(Remote.Supplier<C> supplier)
        {
        m_supplier = supplier;
        }

    // ---- Collector interface ---------------------------------------------

    @Override
    public Supplier<C> supplier()
        {
        return m_supplier;
        }

    @Override
    public BiConsumer<C, T> accumulator()
        {
        return Collection::add;
        }

    @Override
    public BinaryOperator<C> combiner()
        {
        return (r1, r2) ->
                   {
                   r1.addAll(r2);
                   return r1;
                   };
        }

    @Override
    public Function<C, C> finisher()
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
        m_supplier = (Supplier<C>) ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_supplier);
        }

    // ---- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_supplier = in.readObject(0);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_supplier);
        }

    // ---- static members ----------------------------------------------------

    protected static final Set<Characteristics> S_CHARACTERISTICS =
            Collections.unmodifiableSet(EnumSet.of(Characteristics.IDENTITY_FINISH));

    // ---- data members ----------------------------------------------------

    @JsonbProperty("supplier")
    protected Supplier<C> m_supplier;
    }
