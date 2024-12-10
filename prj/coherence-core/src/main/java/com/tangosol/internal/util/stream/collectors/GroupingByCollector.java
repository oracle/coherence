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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.json.bind.annotation.JsonbProperty;

/**
 * A {@code RemoteCollector} implementing a cascaded "group by" operation
 * on input elements of type {@code T}, grouping elements according to a
 * classification function, and then performing a reduction operation on the
 * values associated with a given key using the specified downstream {@code
 * Collector}.
 *
 * @param <T>  the type of elements to be collected
 * @param <K>  the type of the keys
 * @param <A>  the intermediate accumulation type of the downstream
 *             collector
 * @param <D>  the result type of the downstream reduction
 * @param <M>  the type of the resulting {@code Map}
 *
 * @author as 20014.12.30
 * @since 12.2.1
 */
public class GroupingByCollector<T, K, D, A, M extends Map<K, D>>
        implements RemoteCollector<T, Map<K, A>, M>, ExternalizableLite, PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public GroupingByCollector()
        {
        }

    /**
     * Construct MappingCollector instance.
     *
     * @param classifier  a classifier function mapping input elements to keys
     * @param downstream  a {@code RemoteCollector} implementing the downstream
     *                    reduction
     * @param mapFactory  a supplier which produces a new result {@code Map}
     */
    public GroupingByCollector(Remote.Function<? super T, ? extends K> classifier,
                               RemoteCollector<? super T, A, D> downstream,
                               Remote.Supplier<M> mapFactory)
        {
        m_classifier = Lambdas.ensureRemotable(classifier);
        m_downstream = downstream;
        m_mapFactory = mapFactory;
        }

    // ---- Collector interface ---------------------------------------------

    @Override
    public Supplier<Map<K, A>> supplier()
        {
        return HashMap::new;
        }

    @Override
    public BiConsumer<Map<K, A>, T> accumulator()
        {
        final Function<? super T, ? extends K> classifier = m_classifier;
        final Supplier<A> supplier = m_downstream.supplier();
        final BiConsumer<A, ? super T> accumulator = m_downstream.accumulator();

        return (m, t) ->
            {
            K key = Objects.requireNonNull(classifier.apply(t), "element cannot be mapped to a null key");
            A container = m.computeIfAbsent(key, k -> supplier.get());
            accumulator.accept(container, t);
            };
        }

    @Override
    public BinaryOperator<Map<K, A>> combiner()
        {
        return Remote.BinaryOperator.mapMerger(m_downstream.combiner());
        }

    @Override
    public Function<Map<K, A>, M> finisher()
        {
        final Supplier<M>    mapFactory  = m_mapFactory;
        final Function<A, D> finisher    = m_downstream.finisher();

        return (m) ->
            {
            M result = mapFactory.get();
            m.forEach((k, a) -> result.put(k, finisher.apply(a)));
            return result;
            };
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
        m_classifier = ExternalizableHelper.readObject(in);
        m_downstream = ExternalizableHelper.readObject(in);
        m_mapFactory = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_classifier);
        ExternalizableHelper.writeObject(out, m_downstream);
        ExternalizableHelper.writeObject(out, m_mapFactory);
        }

    // ---- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_classifier = in.readObject(0);
        m_downstream = in.readObject(1);
        m_mapFactory = in.readObject(2);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_classifier);
        out.writeObject(1, m_downstream);
        out.writeObject(2, m_mapFactory);
        }

    // ---- data members ----------------------------------------------------

    @JsonbProperty("classifier")
    protected Function<? super T, ? extends K> m_classifier;

    @JsonbProperty("downstream")
    protected RemoteCollector<? super T, A, D> m_downstream;

    @JsonbProperty("mapFactory")
    protected Supplier<M> m_mapFactory;
    }
