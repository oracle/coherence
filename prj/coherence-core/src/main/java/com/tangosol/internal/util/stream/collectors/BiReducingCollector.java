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
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.json.bind.annotation.JsonbProperty;

/**
 * An implementation of {@code RemoteCollector} which performs a reduction of
 * its input elements under a specified mapping function and {@code BinaryOperator}.
 *
 * @param <T>  the type of elements to be collected
 * @param <U>  the type of the mapped values
 *
 * @author as 20014.12.30
 * @since 12.2.1
 */
public class BiReducingCollector<T, U>
        implements RemoteCollector<T, SimpleHolder<U>, U>, ExternalizableLite, PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public BiReducingCollector()
        {
        }

    /**
     * Construct BiReducingCollector instance.
     *
     * @param identity  the identity value for the reduction (also, the value
     *                  that is returned when there are no input elements)
     * @param operator  a {@code BinaryOperator<T>} used to reduce the input
     *                  elements
     */
    public BiReducingCollector(U identity, Remote.BiFunction<? super U, ? super T, ? extends U> mapper, Remote.BinaryOperator<U> operator)
        {
        m_identity = identity;
        m_mapper   = mapper;
        m_operator = operator;
        }

    /**
     * Construct BiReducingCollector instance.
     *
     * @param identity  the identity value for the reduction (also, the value
     *                  that is returned when there are no input elements)
     * @param operator  a {@code BinaryOperator<T>} used to reduce the input
     *                  elements
     */
    public BiReducingCollector(U identity, BiFunction<? super U, ? super T, ? extends U> mapper, BinaryOperator<U> operator)
        {
        m_identity = identity;
        m_mapper   = mapper;
        m_operator = operator;
        }

    // ---- Collector interface ---------------------------------------------

    @Override
    public Supplier<SimpleHolder<U>> supplier()
        {
        final U identity = m_identity;
        return () -> new SimpleHolder<>(identity);
        }

    @Override
    public BiConsumer<SimpleHolder<U>, T> accumulator()
        {
        final BiFunction<? super U, ? super T, ? extends U> mapper = m_mapper;
        final BinaryOperator<U> op = m_operator;
        return (a, t) ->
                    {
                    if (t != null)
                        {
                        a.set(mapper.apply(a.get(), t));
                        }
                    };
        }

    @Override
    public BinaryOperator<SimpleHolder<U>> combiner()
        {
        final BinaryOperator<U> op = m_operator;
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
    public Function<SimpleHolder<U>, U> finisher()
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
        m_identity = (U) ExternalizableHelper.readObject(in);
        m_mapper   = (BiFunction<? super U, ? super T, ? extends U>) ExternalizableHelper.readObject(in);
        m_operator = (BinaryOperator<U>) ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_identity);
        ExternalizableHelper.writeObject(out, m_mapper);
        ExternalizableHelper.writeObject(out, m_operator);
        }

    // ---- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_identity = in.readObject(0);
        m_mapper   = in.readObject(1);
        m_operator = in.readObject(2);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_identity);
        out.writeObject(1, m_mapper);
        out.writeObject(2, m_operator);
        }

    // ---- data members ----------------------------------------------------

    @JsonbProperty("identity")
    protected U m_identity;

    @JsonbProperty("mapper")
    protected BiFunction<? super U, ? super T, ? extends U> m_mapper;

    @JsonbProperty("operator")
    protected BinaryOperator<U> m_operator;
    }
