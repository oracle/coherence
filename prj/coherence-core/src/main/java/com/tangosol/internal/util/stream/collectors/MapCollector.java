/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.stream.collectors;

import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import com.tangosol.util.function.Remote;

import com.tangosol.util.stream.RemoteCollector;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.json.bind.annotation.JsonbProperty;

/**
 * An implementation of {@code RemoteCollector} that collects stream
 * elements into a supplied map.
 *
 * @param <T> the type of elements to be collected
 * @param <K> the output type of the key mapping function
 * @param <V> the output type of the value mapping function
 * @param <M> the type of the resulting {@code Map}
 *
 * @author as 20014.12.30
 * @since 12.2.1
 */
public class MapCollector<T, K, V, M extends Map<K, V>>
        extends AbstractEvolvable
        implements RemoteCollector<T, M, M>, ExternalizableLite, PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public MapCollector()
        {
        }

    /**
     * Construct MapCollector instance.
     *
     * @param keyMapper      a mapping function to produce keys
     * @param valueMapper    a mapping function to produce values
     * @param mergeFunction  a merge function, used to resolve collisions between
     *                       values associated with the same key, as supplied to
     *                       {@link Map#merge(Object, Object, BiFunction)}
     * @param supplier       a function which returns a new, empty {@code Map}
     *                       into which the results will be inserted
     */
    public MapCollector(Remote.Function<? super T, ? extends K> keyMapper,
                        Remote.Function<? super T, ? extends V> valueMapper,
                        Remote.BinaryOperator<V> mergeFunction,
                        Remote.Supplier<M> supplier)
        {
        this(keyMapper, valueMapper, mergeFunction, supplier, null);
        }

    /**
     * Construct CollectionCollector instance.
     *
     * @param keyMapper      a mapping function to produce keys
     * @param valueMapper    a mapping function to produce values
     * @param mergeFunction  a merge function, used to resolve collisions between
     *                       values associated with the same key, as supplied to
     *                       {@link Map#merge(Object, Object, BiFunction)}
     * @param supplier       a function which returns a new, empty {@code Map}
     *                       into which the results will be inserted
     * @param finisher       a function that is used to finalize the result
     *
     * @since 22.09
     */
    public MapCollector(Remote.Function<? super T, ? extends K> keyMapper,
                        Remote.Function<? super T, ? extends V> valueMapper,
                        Remote.BinaryOperator<V> mergeFunction,
                        Remote.Supplier<M> supplier,
                        Remote.Function<M, M> finisher)
        {
        m_keyMapper     = Lambdas.ensureRemotable(keyMapper);
        m_valueMapper   = Lambdas.ensureRemotable(valueMapper);
        m_mergeFunction = mergeFunction;
        m_supplier      = supplier;
        m_finisher      = finisher;
        }

    // ---- Collector interface ---------------------------------------------

    @Override
    public Supplier<M> supplier()
        {
        return m_supplier;
        }

    @Override
    public BiConsumer<M, T> accumulator()
        {
        final Function<? super T, ? extends K> keyMapper     = m_keyMapper;
        final Function<? super T, ? extends V> valueMapper   = m_valueMapper;
        final BinaryOperator<V>                mergeFunction = getMergeFunction();

        return (map, t) -> map.merge(keyMapper.apply(t), valueMapper.apply(t), mergeFunction);
        }

    @Override
    public BinaryOperator<M> combiner()
        {
        final BinaryOperator<V> mergeFunction = getMergeFunction();
        return Remote.BinaryOperator.mapMerger(mergeFunction);
        }

    @Override
    public Function<M, M> finisher()
        {
        return m_finisher == null ? Function.identity() : m_finisher;
        }

    @Override
    public Set<Characteristics> characteristics()
        {
        return S_CHARACTERISTICS;
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Return a merge function for map values.
     *
     * @return  a merge function, used to resolve collisions between
     *          values associated with the same key, as supplied to
     *          {@link Map#merge(Object, Object, BiFunction)}
     */
    protected BinaryOperator<V> getMergeFunction()
        {
        return m_mergeFunction == null
               ? throwingMerger()
               : m_mergeFunction;
        }

    /**
     * Returns a merge function, suitable for use in {@link Map#merge(Object,
     * Object, BiFunction) Map.merge()}, which always throws {@code
     * IllegalStateException}. This can be used to enforce the assumption that
     * the elements being collected are distinct.
     *
     * @return a merge function which always throw {@code IllegalStateException}
     */
    protected BinaryOperator<V> throwingMerger()
        {
        return (v1, v2) ->
            {
            throw new IllegalStateException(String.format("Duplicate key for values %s and %s", v1, v2));
            };
        }

    // ---- ExternalizableLite interface ------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_keyMapper     = ExternalizableHelper.readObject(in);
        m_valueMapper   = ExternalizableHelper.readObject(in);
        m_mergeFunction = ExternalizableHelper.readObject(in);
        m_supplier      = ExternalizableHelper.readObject(in);
        try
            {
            m_finisher = ExternalizableHelper.readObject(in);
            }
        catch (EOFException ignore)
            {
            // the best we can do when reading an older version
            }
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_keyMapper);
        ExternalizableHelper.writeObject(out, m_valueMapper);
        ExternalizableHelper.writeObject(out, m_mergeFunction);
        ExternalizableHelper.writeObject(out, m_supplier);
        ExternalizableHelper.writeObject(out, m_finisher);
        }

    // ---- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_keyMapper     = in.readObject(0);
        m_valueMapper   = in.readObject(1);
        m_mergeFunction = in.readObject(2);
        m_supplier      = in.readObject(3);
        if (in.getVersionId() >= 1)
            {
            m_finisher = in.readObject(4);
            }
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_keyMapper);
        out.writeObject(1, m_valueMapper);
        out.writeObject(2, m_mergeFunction);
        out.writeObject(3, m_supplier);
        out.writeObject(4, m_finisher);
        }

    // ---- Evolvable interface ---------------------------------------------

    public int getImplVersion()
        {
        return VERSION;
        }

    // ---- static members ----------------------------------------------------

    private static final int VERSION = 1;

    static final Set<Characteristics> S_CHARACTERISTICS = EnumSet.noneOf(Characteristics.class);

    // ---- data members ----------------------------------------------------

    @JsonbProperty("keyMapper")
    protected Function<? super T, ? extends K> m_keyMapper;

    @JsonbProperty("valueMapper")
    protected Function<? super T, ? extends V> m_valueMapper;

    @JsonbProperty("mergeFunction")
    protected BinaryOperator<V> m_mergeFunction;

    @JsonbProperty("supplier")
    protected Supplier<M> m_supplier;

    @JsonbProperty("finisher")
    protected Function<M, M> m_finisher;
    }
