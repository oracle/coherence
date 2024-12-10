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
import java.util.EnumSet;
import java.util.Set;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import java.util.stream.Collector;

import javax.json.bind.annotation.JsonbProperty;

/**
 * Simple implementation of {@code RemoteCollector} that collects stream
 * entries into a supplied collection.
 *
 * @param <T>   the type of elements to be collected
 * @param <A>   intermediate accumulation type of the downstream
 *              collector
 * @param <R>   result type of the downstream collector
 * @param <RR>  result type of the resulting collector
 *
 * @author as 20014.12.30
 * @since 12.2.1
 */
public class CollectingAndThenCollector<T, A, R, RR>
        implements RemoteCollector<T, A, RR>, ExternalizableLite, PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public CollectingAndThenCollector()
        {
        }

    /**
     * Construct CollectingAndThenCollector instance.
     *
     * @param downstream  a downstream collector
     * @param finisher    a function to be applied to the final result of the
     *                    downstream collector
     */
    public CollectingAndThenCollector(RemoteCollector<T, A, R> downstream, Remote.Function<R, RR> finisher)
        {
        m_downstream = downstream;
        m_finisher   = finisher;
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
        return m_downstream.accumulator();
        }

    @Override
    public BinaryOperator<A> combiner()
        {
        return m_downstream.combiner();
        }

    @Override
    public Function<A, RR> finisher()
        {
        Function<R, RR> finisher = m_finisher;
        return m_downstream.finisher().andThen(finisher);
        }

    @Override
    public Set<Characteristics> characteristics()
        {
        Set<Characteristics> characteristics = m_downstream.characteristics();
        if (characteristics.contains(Characteristics.IDENTITY_FINISH))
            {
            if (characteristics.size() == 1)
                {
                characteristics = Collections.emptySet();
                }
            else
                {
                characteristics = EnumSet.copyOf(characteristics);
                characteristics.remove(Collector.Characteristics.IDENTITY_FINISH);
                characteristics = Collections.unmodifiableSet(characteristics);
                }
            }

        return characteristics;
        }

    // ---- ExternalizableLite interface ------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_finisher = (Function<R, RR>) ExternalizableHelper.readObject(in);
        m_downstream = (RemoteCollector<T, A, R>) ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_finisher);
        ExternalizableHelper.writeObject(out, m_downstream);
        }

    // ---- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_finisher   = in.readObject(0);
        m_downstream = in.readObject(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_finisher);
        out.writeObject(1, m_downstream);
        }

    // ---- data members ----------------------------------------------------

    @JsonbProperty("finisher")
    protected Function<R, RR> m_finisher;

    @JsonbProperty("downstream")
    protected RemoteCollector<T, A, R> m_downstream;
    }
