/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.GuardSupport;
import com.tangosol.net.Guardian.GuardContext;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.LiteMap;

import com.tangosol.util.function.Remote;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An entry processor that applies a function to the continuously maintained
 * result for an entry's partition.
 *
 * @param <K>  the type of the map entry keys
 * @param <V>  the type of the map entry values
 * @param <R>  the type of the final aggregation result
 * @param <T>  the type of the processor result
 *
 * @author Aleks Seovic  2026.09.12
 * @since 26.10
 */
@Remote.Executable
public class ContinuousAggregationProcessor<K, V, R, T>
        implements InvocableMap.EntryProcessor<K, V, T>,
                   ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct an empty processor for serialization.
     */
    public ContinuousAggregationProcessor()
        {
        }

    /**
     * Construct a processor using a single-result function.
     *
     * @param definition  the continuous aggregation definition
     * @param function    the function to apply to the partition result
     */
    @SuppressWarnings("unchecked")
    public ContinuousAggregationProcessor(ContinuousAggregationDefinition definition,
            Remote.Function<? super R, ? extends T> function)
        {
        Objects.requireNonNull(definition, "definition cannot be null");
        m_filter     = definition.getFilter();
        m_aggregator = (InvocableMap.StreamingAggregator<?, ?, ?, R>) definition.getAggregator();
        m_function   = Objects.requireNonNull(function, "function cannot be null");
        }

    /**
     * Construct a processor using a key-aware function.
     *
     * @param definition  the continuous aggregation definition
     * @param function    the function to apply to each key and partition result
     */
    @SuppressWarnings("unchecked")
    public ContinuousAggregationProcessor(ContinuousAggregationDefinition definition,
            Remote.BiFunction<? super K, ? super R, ? extends T> function)
        {
        Objects.requireNonNull(definition, "definition cannot be null");
        m_filter     = definition.getFilter();
        m_aggregator = (InvocableMap.StreamingAggregator<?, ?, ?, R>) definition.getAggregator();
        m_biFunction = Objects.requireNonNull(function, "function cannot be null");
        }

    // ----- EntryProcessor methods ----------------------------------------

    @Override
    public T process(InvocableMap.Entry<K, V> entry)
        {
        BinaryEntry<K, V> binaryEntry = entry.asBinaryEntry();
        int               nPartition  = ensureSinglePartition(binaryEntry);
        R                 result      = getPartitionResult(binaryEntry, nPartition);

        return apply(entry.getKey(), result);
        }

    @Override
    public Map<K, T> processAll(Set<? extends InvocableMap.Entry<K, V>> setEntries)
        {
        Map<K, T>       mapResults    = new LiteMap<>();
        Map<Integer, R> mapPartitions = new HashMap<>();
        GuardContext    context       = GuardSupport.getThreadContext();
        long            cMillis       = context == null ? 0L : context.getTimeoutMillis();

        for (Iterator<? extends InvocableMap.Entry<K, V>> iterator = setEntries.iterator();
                iterator.hasNext(); )
            {
            InvocableMap.Entry<K, V> entry       = iterator.next();
            BinaryEntry<K, V>        binaryEntry = entry.asBinaryEntry();
            int                      nPartition  = ensureSinglePartition(binaryEntry);
            Integer                  partition   = Integer.valueOf(nPartition);
            R                        result;

            if (mapPartitions.containsKey(partition))
                {
                result = mapPartitions.get(partition);
                }
            else
                {
                result = getPartitionResult(binaryEntry, nPartition);
                mapPartitions.put(partition, result);
                }

            K key = entry.getKey();
            mapResults.put(key, apply(key, result));
            iterator.remove();

            if (context != null)
                {
                context.heartbeat(cMillis);
                }
            }
        return mapResults;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the complete registration filter.
     *
     * @return the registration filter
     */
    public Filter<?> getFilter()
        {
        return m_filter;
        }

    /**
     * Return the registered streaming aggregator definition.
     *
     * @return the aggregator definition
     */
    public InvocableMap.StreamingAggregator<?, ?, ?, R> getAggregator()
        {
        return m_aggregator;
        }

    // ----- ExternalizableLite methods ------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public void readExternal(DataInput in)
            throws IOException
        {
        m_filter     = ExternalizableHelper.readObject(in);
        m_aggregator = ExternalizableHelper.readObject(in);
        m_function   = ExternalizableHelper.readObject(in);
        m_biFunction = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_filter);
        ExternalizableHelper.writeObject(out, m_aggregator);
        ExternalizableHelper.writeObject(out, m_function);
        ExternalizableHelper.writeObject(out, m_biFunction);
        }

    // ----- PortableObject methods ----------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public void readExternal(PofReader in)
            throws IOException
        {
        m_filter     = in.readObject(0);
        m_aggregator = in.readObject(1);
        m_function   = in.readObject(2);
        m_biFunction = in.readObject(3);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_filter);
        out.writeObject(1, m_aggregator);
        out.writeObject(2, m_function);
        out.writeObject(3, m_biFunction);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return the exact, finalized result for a partition.
     */
    private R getPartitionResult(BinaryEntry<K, V> entry, int nPartition)
        {
        if (m_aggregator == null)
            {
            throw new IllegalStateException("continuous aggregator definition is missing");
            }
        return entry.getBackingMapContext().getContinuousAggregationResult(
                nPartition, m_filter, m_aggregator);
        }

    /**
     * Apply the configured projection function.
     */
    private T apply(K key, R result)
        {
        if (m_function != null)
            {
            return m_function.apply(result);
            }
        if (m_biFunction != null)
            {
            return m_biFunction.apply(key, result);
            }
        throw new IllegalStateException("continuous aggregation projection function is missing");
        }

    /**
     * Return the entry partition, rejecting broad association spans that
     * cannot be answered exactly by a single-partition invocation.
     */
    private int ensureSinglePartition(BinaryEntry<K, V> entry)
        {
        if (!(entry.getContext().getCacheService() instanceof PartitionedService))
            {
            throw new UnsupportedOperationException(
                    "partition-local continuous aggregation requires a partitioned service");
            }

        PartitionedService service = (PartitionedService) entry.getContext().getCacheService();
        PartitionSet       parts   = service.getKeyPartitioningStrategy()
                .getAssociatedPartitions(entry.getKey());
        if (parts.cardinality() != 1)
            {
            throw new IllegalArgumentException("continuous aggregation invocation requires a key associated "
                    + "with exactly one partition; key spans " + parts.cardinality() + " partitions");
            }
        return entry.getKeyPartition();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The complete registration filter.
     */
    private Filter<?> m_filter;

    /**
     * The pristine registered aggregator prototype.
     */
    private InvocableMap.StreamingAggregator<?, ?, ?, R> m_aggregator;

    /**
     * The single-result projection function.
     */
    private Remote.Function<? super R, ? extends T> m_function;

    /**
     * The key-aware projection function.
     */
    private Remote.BiFunction<? super K, ? super R, ? extends T> m_biFunction;
    }
