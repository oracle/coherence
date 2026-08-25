/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache;

import com.tangosol.internal.util.QueryResult;

import com.tangosol.net.internal.StorageVersion;
import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.Filter;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for partition-streamed query execution state.
 *
 * @author Aleks Seovic  2026.08.18
 * @since 26.1
 */
public class StoragePartitionedQueryExecutionTest
    {
    @Test
    public void shouldReuseIndexVersionAndPublishStatisticsOnce()
        {
        TestStorage   storage = new TestStorage();
        Filter<Object> filter = value -> true;
        PartitionSet partOne = partition(17, 1);
        PartitionSet partTwo = partition(17, 2);

        storage.m_version.setCommittedVersion(41L);

        Storage.PartitionedQueryExecution execution =
                storage.beginPartitionedQuery(filter, Storage.QUERY_ENTRIES, 17);

        storage.m_version.setCommittedVersion(42L);
        storage.queryPartition(execution, partOne);
        storage.m_version.setCommittedVersion(43L);
        storage.queryPartition(execution, partTwo);
        storage.completePartitionedQuery(execution);
        storage.completePartitionedQuery(execution);

        assertThat(storage.m_listIndexVersions, is(List.of(41L, 41L)));
        assertThat(storage.m_cStatisticsUpdates, is(1));
        assertThat(storage.m_filterStatistics, sameInstance(filter));
        assertThat(storage.m_fOptimized, is(false));
        assertThat(storage.m_cTotal, is(23));
        assertThat(storage.m_cScanned, is(7));
        assertThat(storage.m_cResults, is(3));
        assertThat(storage.m_nQueryType, is(Storage.QUERY_ENTRIES));
        assertThat(storage.m_partsStatistics, is(partitions(17, 1, 2)));
        }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectMultiPartitionStep()
        {
        TestStorage storage = new TestStorage();
        Storage.PartitionedQueryExecution execution =
                storage.beginPartitionedQuery(null, Storage.QUERY_KEYS, 17);

        storage.queryPartition(execution, partitions(17, 1, 2));
        }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectWorkAfterCompletion()
        {
        TestStorage storage = new TestStorage();
        Storage.PartitionedQueryExecution execution =
                storage.beginPartitionedQuery(null, Storage.QUERY_KEYS, 17);

        storage.completePartitionedQuery(execution);
        storage.queryPartition(execution, partition(17, 1));
        }

    private static PartitionSet partition(int cPartitions, int nPartition)
        {
        return new PartitionSet(cPartitions, nPartition);
        }

    private static PartitionSet partitions(int cPartitions, int... anPartition)
        {
        PartitionSet parts = new PartitionSet(cPartitions);
        for (int nPartition : anPartition)
            {
            parts.add(nPartition);
            }
        return parts;
        }

    /**
     * Test fixture that exposes deterministic partition results and statistics.
     */
    public static class TestStorage
            extends Storage
        {
        @Override
        public void onInit()
            {
            }

        @Override
        public StorageVersion getVersion()
            {
            return m_version;
            }

        @Override
        public int calculateSize(PartitionSet parts, boolean fStrict)
            {
            return 10 + parts.next(0);
            }

        @Override
        protected QueryResult queryInternal(Filter filter, int nQueryType,
                PartitionSet parts, long lIdxVersion)
            {
            m_listIndexVersions.add(lIdxVersion);

            int nPartition = parts.next(0);
            return nPartition == 1
                   ? new QueryResult(parts, new Object[4], 2)
                   : new QueryResult(parts, new Object[3], 1, value -> true);
            }

        @Override
        protected void updateQueryStatistics(Filter filter, boolean fOptimized,
                long ldtStart, int cTotal, int cScanned, int cResults,
                int nQueryType, PartitionSet parts)
            {
            ++m_cStatisticsUpdates;
            m_filterStatistics = filter;
            m_fOptimized       = fOptimized;
            m_cTotal           = cTotal;
            m_cScanned         = cScanned;
            m_cResults         = cResults;
            m_nQueryType       = nQueryType;
            m_partsStatistics  = new PartitionSet(parts);
            }

        private final TestStorageVersion m_version = new TestStorageVersion();
        private final List<Long> m_listIndexVersions = new ArrayList<>();

        private int          m_cStatisticsUpdates;
        private Filter       m_filterStatistics;
        private boolean      m_fOptimized;
        private int          m_cTotal;
        private int          m_cScanned;
        private int          m_cResults;
        private int          m_nQueryType;
        private PartitionSet m_partsStatistics;
        }

    /**
     * Mutable committed version used to prove request-scoped version capture.
     */
    public static class TestStorageVersion
            extends StorageVersion
        {
        @Override
        public long getCommittedVersion()
            {
            return m_lCommittedVersion;
            }

        public void setCommittedVersion(long lVersion)
            {
            m_lCommittedVersion = lVersion;
            }

        private long m_lCommittedVersion;
        }
    }
