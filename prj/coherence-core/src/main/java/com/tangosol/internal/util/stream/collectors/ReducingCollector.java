/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.stream.collectors;

import com.oracle.coherence.common.base.Holder;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.SimpleHolder;

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

import javax.json.bind.annotation.JsonbProperty;

/**
 * An implementation of {@code RemoteCollector} that reduces stream of values
 * into a single value.
 *
 * @param <T> the type of elements to be collected
 *
 * @author as 20014.12.30
 * @since 12.2.1
 */
public class ReducingCollector<T>
        implements RemoteCollector<T, SimpleHolder<T>, T>, ExternalizableLite, PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public ReducingCollector()
        {
        }

    /**
     * Construct ReducingCollector instance.
     *
     * @param operator  a {@code BinaryOperator<T>} used to reduce the input
     *                  elements
     */
    public ReducingCollector(Remote.BinaryOperator<T> operator)
        {
        this(null, operator);
        }

    /**
     * Construct ReducingCollector instance.
     *
     * @param operator  a {@code BinaryOperator<T>} used to reduce the input
     *                  elements
     */
    public ReducingCollector(BinaryOperator<T> operator)
        {
        this(null, operator);
        }

    /**
     * Construct ReducingCollector instance.
     *
     * @param identity  the identity value for the reduction (also, the value
     *                  that is returned when there are no input elements)
     * @param operator  a {@code BinaryOperator<T>} used to reduce the input
     *                  elements
     */
    public ReducingCollector(T identity, Remote.BinaryOperator<T> operator)
        {
        m_identity = identity;
        m_operator = operator;
        }

    /**
     * Construct ReducingCollector instance.
     *
     * @param identity  the identity value for the reduction (also, the value
     *                  that is returned when there are no input elements)
     * @param operator  a {@code BinaryOperator<T>} used to reduce the input
     *                  elements
     */
    public ReducingCollector(T identity, BinaryOperator<T> operator)
        {
        m_identity = identity;
        m_operator = operator;
        }

    // ---- Collector interface ---------------------------------------------

    @Override
    public Supplier<SimpleHolder<T>> supplier()
        {
        final T identity = m_identity;
        return () -> new SimpleHolder<>(identity);
        }

    @Override
    public BiConsumer<SimpleHolder<T>, T> accumulator()
        {
        final BinaryOperator<T> op = m_operator;
        return (a, t) ->
            {
            if (t != null)
                {
                if (a.isPresent())
                    {
                    a.set(op.apply(a.get(), t));
                    }
                else
                    {
                    a.set(t);
                    }
                }
            };
        }

    @Override
    public BinaryOperator<SimpleHolder<T>> combiner()
        {
        final BinaryOperator<T> op = m_operator;
        return (a, b) ->
            {
            if (b.isPresent())
                {
                if (a.isPresent())
                    {
                    a.set(op.apply(a.get(), b.get()));
                    }
                else
                    {
                    a.set(b.get());
                    }
                }
            return a;
            };
        }

    @Override
    public Function<SimpleHolder<T>, T> finisher()
        {
        return Holder::get;
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
        m_identity = (T) ExternalizableHelper.readObject(in);
        m_operator = (BinaryOperator<T>) ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_identity);
        ExternalizableHelper.writeObject(out, m_operator);
        }

    // ---- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_identity = in.readObject(0);
        m_operator = in.readObject(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_identity);
        out.writeObject(1, m_operator);
        }

    // ---- data members ----------------------------------------------------

    @JsonbProperty("identity")
    protected T m_identity;

    @JsonbProperty("operator")
    protected BinaryOperator<T> m_operator;
    }
