/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.stream;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.Streamer;

import com.tangosol.util.stream.RemotePipeline;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Set;

import java.util.function.BiConsumer;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;

import java.util.stream.DoubleStream;

import javax.json.bind.annotation.JsonbProperty;

/**
 * A primitive specialization of {@link CollectorAggregator} for doubles.
 * <p>
 * This aggregator always returns unfinished combined result and leaves finish
 * operation to the caller in order to avoid boxing.
 *
 * @param <K>  key type of the map this aggregator is invoked on
 * @param <V>  value type of the map this aggregator is invoked on
 * @param <R>  the type of the aggregation result
 *
 * @author as  2014.10.08
 * @since 12.2.1
 */
public class DoubleCollectorAggregator<K, V, R>
        implements InvocableMap.StreamingAggregator<K, V, R, R>,
                   ExternalizableLite, PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public DoubleCollectorAggregator()
        {
        }

    /**
     * Construct DoubleCollectorAggregator instance.
     *
     * @param pipeline     the pipeline of intermediate operations
     * @param supplier     supplies the container to collect into
     * @param accumulator  accumulates values into the container
     * @param combiner     combines partial results into the final result
     */
    public DoubleCollectorAggregator(RemotePipeline<DoubleStream> pipeline,
                                     Supplier<R> supplier,
                                     ObjDoubleConsumer<R> accumulator,
                                     BiConsumer<R, R> combiner)
        {
        m_pipeline    = pipeline;
        m_supplier    = supplier;
        m_accumulator = accumulator;
        m_combiner    = combiner;
        }

    // ---- EntryAggregator interface ---------------------------------------

    @Override
    public R aggregate(Set<? extends InvocableMap.Entry<? extends K, ? extends V>> setEntries)
        {
        DoubleStream s = m_pipeline.evaluate(setEntries.stream());
        return s.collect(m_supplier, m_accumulator, m_combiner);
        }

    // ---- StreamingAggregator interface -----------------------------------

    @Override
    public InvocableMap.StreamingAggregator<K, V, R, R> supply()
        {
        return new DoubleCollectorAggregator<>(m_pipeline, m_supplier, m_accumulator, m_combiner);
        }

    @Override
    public boolean accumulate(Streamer<? extends InvocableMap.Entry<? extends K, ? extends V>> streamer)
        {
        DoubleStream stream = m_pipeline.evaluate(streamer.stream());
        m_result = m_supplier.get();
        stream.forEach(t -> m_accumulator.accept(m_result, t));
        return true;
        }

    @Override
    public boolean accumulate(InvocableMap.Entry<? extends K, ? extends V> entry)
        {
        return true;
        }

    @Override
    public boolean combine(R partialResult)
        {
        if (m_result == null)
            {
            m_result = m_supplier.get();
            }

        m_combiner.accept(m_result, partialResult);
        return true;
        }

    @Override
    public R getPartialResult()
        {
        return m_result;
        }

    @Override
    public R finalizeResult()
        {
        return m_result;
        }

    @Override
    public int characteristics()
        {
        return m_pipeline.isParallel() ? PARALLEL : SERIAL;
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_pipeline    = ExternalizableHelper.readObject(in);
        m_supplier    = ExternalizableHelper.readObject(in);
        m_accumulator = ExternalizableHelper.readObject(in);
        m_combiner    = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_pipeline);
        ExternalizableHelper.writeObject(out, m_supplier);
        ExternalizableHelper.writeObject(out, m_accumulator);
        ExternalizableHelper.writeObject(out, m_combiner);
        }

    // ---- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_pipeline    = in.readObject(0);
        m_supplier    = in.readObject(1);
        m_accumulator = in.readObject(2);
        m_combiner    = in.readObject(3);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_pipeline);
        out.writeObject(1, m_supplier);
        out.writeObject(2, m_accumulator);
        out.writeObject(3, m_combiner);
        }

    // ---- data members ----------------------------------------------------

    /**
     * The pipeline of intermediate operations to apply to the stream of entries
     */
    @JsonbProperty("pipeline")
    protected RemotePipeline<DoubleStream> m_pipeline;

    /**
     * The supplier that creates result container.
     */
    @JsonbProperty("supplier")
    protected Supplier<R> m_supplier;

    /**
     * The accumulator that adds individual values into the result container.
     */
    @JsonbProperty("accumulator")
    protected ObjDoubleConsumer<R> m_accumulator;

    /**
     * The combiner that merges partial results into a final result.
     */
    @JsonbProperty("combiner")
    protected BiConsumer<R, R> m_combiner;

    /**
     * The aggregation result.
     */
    private transient R m_result;
    }
