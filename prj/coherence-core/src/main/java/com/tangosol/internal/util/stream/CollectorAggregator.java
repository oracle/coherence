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
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.stream.RemoteCollector;
import com.tangosol.util.stream.RemotePipeline;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Objects;

import java.util.Set;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;

import java.util.stream.Stream;

import javax.json.bind.annotation.JsonbProperty;

/**
 * An {@link com.tangosol.util.InvocableMap.StreamingAggregator} implementation
 * that uses a {@link RemotePipeline} of intermediate operations (as defined by the
 * {@link Stream} API) and a {@link RemoteCollector} to perform the aggregation.
 *
 * @param <K>  the type of the Map entry keys
 * @param <V>  the type of the Map entry values
 * @param <T>  the type of the elements in the final {@link Stream} returned
 *             by the {@link RemotePipeline} of intermediate operations. In the case
 *             of a simple pipeline based on a single {@link ValueExtractor},
 *             this represents the type of the extracted value
 * @param <P>  the mutable accumulation type of the partial aggregation result
 *             (often hidden as an implementation detail)
 * @param <R>  the result type of the aggregation, which can be the same as the
 *             accumulator type if the {@link RemoteCollector} used does not have a
 *             finisher which transforms the accumulator into a final result
 *
 * @author as  2014.09.26
 *
 * @since 12.2.1
 */
public class CollectorAggregator<K, V, T, P, R>
        implements InvocableMap.StreamingAggregator<K, V, P, R>,
                   ExternalizableLite, PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public CollectorAggregator()
        {
        }

    /**
     * Construct a CollectorAggregator that will aggregate values from a stream
     * of {@link com.tangosol.util.InvocableMap.Entry} objects after applying a
     * {@link RemotePipeline} of intermediate operations to them.
     *
     * @param pipeline   the pipeline of intermediate operations to apply to
     *                   the stream of entries
     * @param collector  the collector to use for aggregation
     */
    public CollectorAggregator(RemotePipeline<? extends Stream<T>> pipeline, RemoteCollector<? super T, P, R> collector)
        {
        Objects.requireNonNull(pipeline);
        Objects.requireNonNull(collector);

        m_collector = collector;
        m_pipeline  = pipeline;
        }

    // ---- InvocableMap.StreamingAggregator interface ----------------------

    @Override
    public R aggregate(Set<? extends InvocableMap.Entry<? extends K, ? extends V>> setEntries)
        {
        Stream<T> s = getPipeline().evaluate(setEntries.stream());
        return s.collect(getCollector());
        }

    @Override
    public boolean accumulate(Streamer<? extends InvocableMap.Entry<? extends K, ? extends V>> streamer)
        {
        Stream<T>                        stream      = getPipeline().evaluate(streamer.stream());
        RemoteCollector<? super T, P, ?> collector   = getCollector();
        BiConsumer<P, ? super T>         accumulator = collector.accumulator();
        P                                result      = collector.supplier().get();

        stream.forEach(t -> accumulator.accept(result, t));

        m_result = result;
        return true;
        }

    @Override
    public InvocableMap.StreamingAggregator<K, V, P, R> supply()
        {
        return new CollectorAggregator<>(getPipeline(), getCollector());
        }

    @Override
    public boolean accumulate(InvocableMap.Entry<? extends K, ? extends V> entry)
        {
        return true;
        }

    @Override
    public boolean combine(P partialResult)
        {
        if (m_result == null)
            {
            m_result   = getCollector().supplier().get();
            m_combiner = getCollector().combiner();
            }

        m_combiner.apply(m_result, partialResult);
        return true;
        }

    @Override
    public P getPartialResult()
        {
        return m_result;
        }

    @Override
    public R finalizeResult()
        {
        return getCollector().finisher().apply(m_result);
        }

    @Override
    public int characteristics()
        {
        return (getPipeline().isParallel() ? PARALLEL : SERIAL) | RETAINS_ENTRIES;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the pipeline of intermediate operations to apply to the stream
     * of entries.
     *
     * @return  the pipeline of intermediate operations to apply
     */
    public RemotePipeline<? extends Stream<T>> getPipeline()
        {
        RemotePipeline<? extends Stream<T>> pipeline = m_pipeline;
        if (pipeline == null)
            {
            pipeline = m_pipeline = createPipeline();
            }
        return pipeline;
        }

    /**
     * Create a pipeline of intermediate operations.
     * <p>
     * This method is intended to be overridden by the classes that extend
     * {@code CollectorAggregator} and would like to override the default
     * behavior.
     *
     * @return  the pipeline of intermediate operations
     */
    protected RemotePipeline<? extends Stream<T>> createPipeline()
        {
        return null;
        }

    /**
     * Return the collector to use for aggregation.
     *
     * @return  the collector to use for aggregation
     */
    public RemoteCollector<? super T, P, R> getCollector()
        {
        RemoteCollector<? super T, P, R> collector = m_collector;
        if (collector == null)
            {
            collector = m_collector = createCollector();
            }
        return collector;
        }

    /**
     * Create a collector to use for aggregation.
     * <p>
     * This method is intended to be overridden by the classes that extend
     * {@code CollectorAggregator} and would like to override the default
     * behavior.
     *
     * @return  the collector to use for aggregation
     */
    protected RemoteCollector<? super T, P, R> createCollector()
        {
        return null;
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_pipeline  = ExternalizableHelper.readObject(in);
        m_collector = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_pipeline);
        ExternalizableHelper.writeObject(out, m_collector);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_pipeline  = in.readObject(0);
        m_collector = in.readObject(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_pipeline);
        out.writeObject(1, m_collector);
        }

    // ---- data members ----------------------------------------------------

    /**
     * The pipeline of intermediate operations to apply to the stream of entries
     */
    @JsonbProperty("pipeline")
    protected RemotePipeline<? extends Stream<T>> m_pipeline;

    /**
     * The collector to use for aggregation
     */
    @JsonbProperty("collector")
    protected RemoteCollector<? super T, P, R> m_collector;

    /**
     * The accumulated partial result.
     */
    private transient P m_result;

    /**
     * The Combiner for the partial results.
     */
    private transient BinaryOperator<P> m_combiner;
    }
