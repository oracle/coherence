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
import com.tangosol.util.LiteSet;

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

import javax.json.bind.annotation.JsonbProperty;

/**
 * Simple implementation class for {@code RemoteCollector}.
 *
 * @param <T> the type of elements to be collected
 * @param <A> the type of the accumulator/partial result
 * @param <R> the type of the final result
 *
 * @author as 20014.10.01
 * @since 12.2.1
 */
public class SimpleRemoteCollector<T, A, R>
        implements RemoteCollector<T, A, R>, ExternalizableLite, PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public SimpleRemoteCollector()
        {
        }

    /**
     * Construct SimpleRemoteCollector instance.
     *
     * @param supplier         the supplier to use
     * @param accumulator      the accumulator to use
     * @param combiner         the combiner to use
     * @param finisher         the finisher to use
     * @param characteristics  the set of collector characteristics
     */
    public SimpleRemoteCollector(Supplier<A> supplier,
                                 BiConsumer<A, T> accumulator,
                                 BinaryOperator<A> combiner,
                                 Function<A, R> finisher,
                                 Set<Characteristics> characteristics)
        {
        m_supplier    = supplier;
        m_accumulator = accumulator;
        m_combiner    = combiner;
        m_finisher    = finisher;
        m_characteristics = characteristics;
        }

    /**
     * Construct SimpleRemoteCollector instance.
     *
     * @param supplier         the supplier to use
     * @param accumulator      the accumulator to use
     * @param combiner         the combiner to use
     * @param characteristics  the set of collector characteristics
     */
    public SimpleRemoteCollector(Supplier<A> supplier,
                                 BiConsumer<A, T> accumulator,
                                 BinaryOperator<A> combiner,
                                 Set<Characteristics> characteristics)
        {
        this(supplier, accumulator, combiner, castingIdentity(), characteristics);
        }

    /**
     * Construct SimpleRemoteCollector instance.
     *
     * @param supplier         the supplier to use
     * @param accumulator      the accumulator to use
     * @param combiner         the combiner to use
     * @param finisher         the finisher to use
     * @param characteristics  the set of collector characteristics
     */
    public SimpleRemoteCollector(Remote.Supplier<A> supplier,
                                 Remote.BiConsumer<A, T> accumulator,
                                 Remote.BinaryOperator<A> combiner,
                                 Remote.Function<A, R> finisher,
                                 Set<Characteristics> characteristics)
        {
        m_supplier    = supplier;
        m_accumulator = accumulator;
        m_combiner    = combiner;
        m_finisher    = finisher;
        m_characteristics = characteristics;
        }

    /**
     * Construct SimpleRemoteCollector instance.
     *
     * @param supplier         the supplier to use
     * @param accumulator      the accumulator to use
     * @param combiner         the combiner to use
     * @param characteristics  the set of collector characteristics
     */
    public SimpleRemoteCollector(Remote.Supplier<A> supplier,
                                 Remote.BiConsumer<A, T> accumulator,
                                 Remote.BinaryOperator<A> combiner,
                                 Set<Characteristics> characteristics)
        {
        this(supplier, accumulator, combiner, castingIdentity(), characteristics);
        }

    // ---- Collector interface ---------------------------------------------

    @Override
    public BiConsumer<A, T> accumulator()
        {
        return m_accumulator;
        }

    @Override
    public Supplier<A> supplier()
        {
        return m_supplier;
        }

    @Override
    public BinaryOperator<A> combiner()
        {
        return m_combiner;
        }

    @Override
    public Function<A, R> finisher()
        {
        return m_finisher;
        }

    @Override
    public Set<Characteristics> characteristics()
        {
        return m_characteristics;
        }

    // ---- ExternalizableLite interface ------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_supplier = (Supplier<A>) ExternalizableHelper.readObject(in);
        m_accumulator = (BiConsumer<A, T>) ExternalizableHelper.readObject(in);
        m_combiner = (BinaryOperator<A>) ExternalizableHelper.readObject(in);
        m_finisher = (Function<A, R>) ExternalizableHelper.readObject(in);

        Set<Characteristics> set = new LiteSet<>();
        ExternalizableHelper.readCollection(in, set, null);
        m_characteristics = set.size() > 0
                          ? Collections.unmodifiableSet(EnumSet.copyOf(set))
                          : Collections.emptySet();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_supplier);
        ExternalizableHelper.writeObject(out, m_accumulator);
        ExternalizableHelper.writeObject(out, m_combiner);
        ExternalizableHelper.writeObject(out, m_finisher);
        ExternalizableHelper.writeCollection(out, m_characteristics);
        }

    // ---- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_supplier = in.readObject(0);
        m_accumulator = in.readObject(1);
        m_combiner = in.readObject(2);
        m_finisher = in.readObject(3);

        Set<Characteristics> set = new LiteSet<>();
        in.readCollection(4, set);
        m_characteristics = set.size() > 0
                          ? Collections.unmodifiableSet(EnumSet.copyOf(set))
                          : Collections.emptySet();
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_supplier);
        out.writeObject(1, m_accumulator);
        out.writeObject(2, m_combiner);
        out.writeObject(3, m_finisher);
        out.writeCollection(4, m_characteristics, Characteristics.class);
        }

    // ---- helpers ---------------------------------------------------------

    @SuppressWarnings("unchecked")
    protected static <I, R> Remote.Function<I, R> castingIdentity()
        {
        return i -> (R) i;
        }

    // ---- data members ----------------------------------------------------

    @JsonbProperty("supplier")
    protected Supplier<A> m_supplier;

    @JsonbProperty("accumulator")
    protected BiConsumer<A, T> m_accumulator;

    @JsonbProperty("combiner")
    protected BinaryOperator<A> m_combiner;

    @JsonbProperty("finisher")
    protected Function<A, R> m_finisher;

    @JsonbProperty("characteristics")
    protected Set<Characteristics> m_characteristics;
    }
