/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;

import com.tangosol.internal.net.ContinuousAggregationDefinition;

import com.tangosol.net.PartitionedService;
import com.tangosol.net.partition.KeyPartitioningStrategy;
import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.SimpleMapEntry;

import com.tangosol.util.aggregator.Count;
import com.tangosol.util.aggregator.GroupAggregator;

import com.tangosol.util.extractor.IdentityExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.KeyAssociatedFilter;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for affinity-scoped continuous aggregation storage state.
 *
 * @author Aleks Seovic  2026.09.11
 * @since 26.10
 */
public class StorageContinuousAggregationAffinityTest
    {
    @Test
    public void shouldRetainOnlyAssociatedPartitionsForKeyAssociatedDefinition()
        {
        PartitionSet partsAssociated = partitions(17, 2, 4);
        TestStrategy strategy        = new TestStrategy(partsAssociated);
        TestStorage  storage         = new TestStorage(new TestPartitionedCache(strategy));
        PartitionSet partsOwned      = partitions(17, 1, 2, 3, 4);

        ContinuousAggregationDefinition definition =
                new ContinuousAggregationDefinition(
                        new KeyAssociatedFilter<>(AlwaysFilter.INSTANCE(), "customer-1"),
                        new Count<>());

        assertEquals(partsAssociated,
                storage.getApplicablePartitions(partsOwned, definition));
        assertEquals("customer-1", strategy.m_oHostKey);
        }

    @Test
    public void shouldRetainAllOwnedPartitionsForCacheWideDefinition()
        {
        TestStorage  storage    = new TestStorage(new TestPartitionedCache(
                new TestStrategy(partitions(17, 2, 4))));
        PartitionSet partsOwned = partitions(17, 1, 2, 3, 4);

        assertEquals(partsOwned, storage.getApplicablePartitions(partsOwned,
                new ContinuousAggregationDefinition(null, new Count<>())));
        }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void shouldReturnFinalizedDetachedPartitionResult()
        {
        GroupAggregator group = GroupAggregator.createInstance(
                IdentityExtractor.INSTANCE(), new Count<>());
        group.accumulate(new SimpleMapEntry<>(1, 10L));
        group.accumulate(new SimpleMapEntry<>(2, 10L));
        group.accumulate(new SimpleMapEntry<>(3, 20L));

        TestStorage storage = new TestStorage(null);
        storage.m_oPartial = group.getPartialResult();

        Map<Long, Integer> result = (Map<Long, Integer>)
                storage.getContinuousAggregationResult(7, null, group);

        assertEquals(Integer.valueOf(2), result.get(10L));
        assertEquals(Integer.valueOf(1), result.get(20L));
        assertEquals(new ContinuousAggregationDefinition(null, group),
                storage.m_definition);
        assertEquals(7, storage.m_nPartition);
        }

    private static PartitionSet partitions(int cPartitions, int... anPartitions)
        {
        PartitionSet parts = new PartitionSet(cPartitions);
        for (int nPartition : anPartitions)
            {
            parts.add(nPartition);
            }
        return parts;
        }

    private static class TestStorage
            extends Storage
        {
        TestStorage(PartitionedCache service)
            {
            m_service = service;
            }

        @Override
        public void onInit()
            {
            }

        @Override
        public PartitionedCache getService()
            {
            return m_service;
            }

        PartitionSet getApplicablePartitions(PartitionSet parts,
                ContinuousAggregationDefinition definition)
            {
            return getContinuousAggregationPartitions(parts, definition);
            }

        @Override
        protected Object getContinuousAggregationPartialResult(
                ContinuousAggregationDefinition definition, int nPartition)
            {
            m_definition = definition;
            m_nPartition = nPartition;
            return m_oPartial;
            }

        private final PartitionedCache m_service;
        private ContinuousAggregationDefinition m_definition;
        private Object m_oPartial;
        private int m_nPartition;
        }

    private static class TestPartitionedCache
            extends PartitionedCache
        {
        TestPartitionedCache(KeyPartitioningStrategy strategy)
            {
            super("StorageContinuousAggregationAffinityTest", null, true);
            m_strategy = strategy;
            }

        @Override
        public KeyPartitioningStrategy getKeyPartitioningStrategy()
            {
            return m_strategy;
            }

        private final KeyPartitioningStrategy m_strategy;
        }

    private static class TestStrategy
            implements KeyPartitioningStrategy
        {
        TestStrategy(PartitionSet parts)
            {
            m_parts = parts;
            }

        @Override
        public void init(PartitionedService service)
            {
            }

        @Override
        public int getKeyPartition(Object key)
            {
            return m_parts.first();
            }

        @Override
        public PartitionSet getAssociatedPartitions(Object key)
            {
            m_oHostKey = key;
            return new PartitionSet(m_parts);
            }

        private final PartitionSet m_parts;
        private Object m_oHostKey;
        }
    }
