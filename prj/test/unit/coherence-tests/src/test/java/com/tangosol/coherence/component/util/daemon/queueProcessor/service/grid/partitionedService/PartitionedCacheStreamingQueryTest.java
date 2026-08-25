/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService;

import com.oracle.coherence.common.util.MemorySize;

import com.tangosol.coherence.component.net.Message;
import com.tangosol.coherence.component.net.RequestContext;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.Storage;

import com.tangosol.internal.util.QueryResult;

import com.tangosol.net.internal.StorageVersion;
import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for partition-streamed query coordination.
 *
 * @author Aleks Seovic  2026.08.18
 * @since 26.07
 */
public class PartitionedCacheStreamingQueryTest
    {
    @Test
    public void shouldFairSharePartitionWorkLanes()
        {
        TestService service = new TestService(new TestStorage(), Set.of());

        assertThat(service.calculatePartitionedWorkLaneCount(257, 16, 1, true), is(3));
        assertThat(service.calculatePartitionedWorkLaneCount(257, 16, 4, true), is(2));
        assertThat(service.calculatePartitionedWorkLaneCount(257, 16, 8, true), is(1));
        assertThat(service.calculatePartitionedWorkLaneCount(257, 16, 16, true), is(0));
        assertThat(service.calculatePartitionedWorkLaneCount(1, 16, 1, true), is(0));

        // VDP active executions are short-lived logical tasks, not fixed
        // workers to divide among request parents. Its shared hard budget and
        // idle limit still bound additional lanes.
        assertThat(service.calculatePartitionedWorkLaneCount(257, 16, 8, false), is(3));
        assertThat(service.calculatePartitionedWorkLaneCount(257, 16, 16, false), is(0));
        }

    @Test
    public void shouldRespondToEmptyPartitionMask()
        {
        TestStorage storage = new TestStorage();
        TestService service = new TestService(storage, Set.of());
        PartitionedCache.PartitionedQueryRequest request = request(service);

        service.onPartitionedStreamingQueryRequest(request, storage,
                partitions(17), null, true);

        List<PartitionedCache.QueryResponse> listResponse = service.getResponses();

        assertThat(listResponse.size(), is(1));
        assertThat(listResponse.get(0).getResponsePartitions(), is(partitions(17)));
        assertThat(listResponse.get(0).getSize(), is(0));
        assertThat(request.getProcessedPartitions(), is(partitions(17)));
        assertThat(storage.getAccessChecks(), is(1));
        assertThat(storage.getStatisticsUpdates(), is(0));
        }

    @Test
    public void shouldStreamBatchAndRejectPartitionsExactlyOnce()
        {
        TestStorage storage = new TestStorage();
        TestService service = new TestService(storage, Set.of(1, 3, 4));
        PartitionedCache.PartitionedQueryRequest request = request(service);

        service.onPartitionedStreamingQueryRequest(request, storage,
                partitions(17, 1, 2, 3, 4), null, true);

        List<PartitionedCache.QueryResponse> listResponse = service.getResponses();

        assertThat(listResponse.size(), is(3));
        assertThat(listResponse.get(0).getResponsePartitions(), is(partitions(17, 1)));
        assertThat(listResponse.get(0).getSize(), is(1));
        assertThat(listResponse.get(1).getRejectPartitions(), is(partitions(17, 2)));
        assertThat(listResponse.get(1).getResponsePartitions(), is(partitions(17)));
        assertThat(listResponse.get(2).getResponsePartitions(), is(partitions(17, 3, 4)));
        assertThat(listResponse.get(2).getSize(), is(2));

        assertThat(request.getProcessedPartitions(), is(partitions(17, 1, 3, 4)));
        assertThat(storage.getAccessChecks(), is(1));
        assertThat(storage.getStatisticsUpdates(), is(1));
        assertThat(storage.getQueriedPartitions(), is(List.of(1, 3, 4)));
        assertThat(service.getPinnedPartitions(), is(List.of(1, 3, 4)));
        assertThat(service.getUnpinnedPartitions(), is(Set.of(1, 3, 4)));
        assertThat(service.getProcessChangesCount(), is(2));
        }

    @Test
    public void shouldSendOneTerminalFailureAndReleaseEveryPin()
        {
        TestStorage storage = new TestStorage();
        storage.setFailPartition(2);

        TestService service = new TestService(storage, Set.of(1, 2, 3));
        PartitionedCache.PartitionedQueryRequest request = request(service);

        service.onPartitionedStreamingQueryRequest(request, storage,
                partitions(17, 1, 2, 3), null, true);

        List<PartitionedCache.QueryResponse> listResponse = service.getResponses();

        assertThat(listResponse.size(), is(2));
        assertThat(listResponse.get(0).getResponsePartitions(), is(partitions(17, 1)));
        assertThat(listResponse.get(1).getException(), is(notNullValue()));
        assertThat(request.getProcessedPartitions(), is(partitions(17, 1)));
        assertThat(storage.getStatisticsUpdates(), is(0));
        assertThat(storage.getQueriedPartitions(), is(List.of(1, 2)));
        assertThat(service.getUnpinnedPartitions(), is(Set.of(1, 2)));
        }

    @Test
    public void shouldReportCleanupFailureAndReleaseEveryPin()
        {
        TestStorage storage = new TestStorage();
        TestService service = new TestService(storage, Set.of(1));
        service.setProcessChangesFailure(new IllegalStateException("expected cleanup failure"));

        PartitionedCache.PartitionedQueryRequest request = request(service);
        service.onPartitionedStreamingQueryRequest(request, storage,
                partitions(17, 1), null, true);

        List<PartitionedCache.QueryResponse> listResponse = service.getResponses();

        assertThat(listResponse.size(), is(2));
        assertThat(listResponse.get(0).getResponsePartitions(), is(partitions(17, 1)));
        assertThat(listResponse.get(1).getException(), is(notNullValue()));
        assertThat(request.getProcessedPartitions(), is(partitions(17, 1)));
        assertThat(storage.getStatisticsUpdates(), is(0));
        assertThat(service.getUnpinnedPartitions(), is(Set.of(1)));
        }

    @Test
    public void shouldDrainPartitionsAcrossConcurrentLanesWithoutDuplication()
            throws InterruptedException
        {
        TestStorage storage = new TestStorage();
        storage.setQueryDelayMillis(2L);

        Set<Integer> setOwned = new HashSet<>();
        int[] anPartition = new int[16];
        for (int i = 0; i < anPartition.length; i++)
            {
            setOwned.add(i);
            anPartition[i] = i;
            }

        TestService service = new TestService(storage, setOwned);
        PartitionedCache.PartitionedQueryRequest request = request(service);
        PartitionedCache.PartitionedQueryContext context =
                new PartitionedCache.PartitionedQueryContext(service, request, storage,
                        storage.beginPartitionedQuery(null, Storage.QUERY_KEYS, 17),
                        anPartition, true, 2, 0, Long.MAX_VALUE);

        Thread thread = new Thread(() -> service.runPartitionedQueryLane(context));
        thread.start();
        service.runPartitionedQueryLane(context);
        thread.join();

        assertThat(new HashSet<>(storage.getQueriedPartitions()), is(setOwned));
        assertThat(storage.getQueriedPartitions().size(), is(anPartition.length));
        assertThat(request.getProcessedPartitions(), is(partitions(17, anPartition)));
        assertThat(storage.getStatisticsUpdates(), is(1));
        assertThat(service.getUnpinnedPartitions(), is(setOwned));
        }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldAggregatePartitionsAndCombineOneMemberResult()
        {
        TestStorage storage = new TestStorage();
        TestService service = new TestService(storage, Set.of(1, 3, 4));
        PartitionedCache.AggregateFilterRequest request = aggregateRequest(service);

        AtomicInteger nCombined = new AtomicInteger();
        InvocableMap.StreamingAggregator agent = mock(InvocableMap.StreamingAggregator.class);
        when(agent.supply()).thenReturn(mock(InvocableMap.StreamingAggregator.class));
        when(agent.combine(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation ->
            {
            nCombined.addAndGet((Integer) invocation.getArgument(0));
            return true;
            });
        when(agent.getPartialResult()).thenAnswer(invocation -> nCombined.get());

        service.onPartitionedStreamingAggregateRequest(request, storage,
                partitions(17, 1, 2, 3, 4), null, agent);

        List<PartitionedCache.PartialValueResponse> listResponse =
                service.getAggregateResponses();

        assertThat(listResponse.size(), is(1));
        assertThat(listResponse.get(0).getResult(), is(8));
        assertThat(listResponse.get(0).getRejectPartitions(), is(partitions(17, 2)));
        assertThat(request.getProcessedPartitions(), is(partitions(17, 1, 3, 4)));
        assertThat(storage.getAccessChecks(), is(1));
        assertThat(storage.getAggregatedPartitions(), is(List.of(1, 3, 4)));
        assertThat(service.getUnpinnedPartitions(), is(Set.of(1, 3, 4)));
        assertThat(service.getProcessChangesCount(), is(3));
        }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReturnOneAggregateFailureAndReleaseEveryPin()
        {
        TestStorage storage = new TestStorage();
        storage.setFailPartition(2);

        TestService service = new TestService(storage, Set.of(1, 2, 3));
        PartitionedCache.AggregateFilterRequest request = aggregateRequest(service);
        InvocableMap.StreamingAggregator agent = mock(InvocableMap.StreamingAggregator.class);
        when(agent.supply()).thenReturn(mock(InvocableMap.StreamingAggregator.class));

        service.onPartitionedStreamingAggregateRequest(request, storage,
                partitions(17, 1, 2, 3), null, agent);

        List<PartitionedCache.PartialValueResponse> listResponse =
                service.getAggregateResponses();

        assertThat(listResponse.size(), is(1));
        assertThat(listResponse.get(0).getException(), is(notNullValue()));
        assertThat(request.getProcessedPartitions(), is(partitions(17, 1)));
        assertThat(storage.getAggregatedPartitions(), is(List.of(1, 2)));
        assertThat(service.getUnpinnedPartitions(), is(Set.of(1, 2)));
        }

    private static PartitionedCache.PartitionedQueryRequest request(TestService service)
        {
        PartitionedCache.PartitionedQueryRequest request =
                new PartitionedCache.PartitionedQueryRequest();
        request.setService(service);
        request.setCacheId(1L);
        return request;
        }

    private static PartitionedCache.AggregateFilterRequest aggregateRequest(TestService service)
        {
        PartitionedCache.AggregateFilterRequest request =
                new PartitionedCache.AggregateFilterRequest();
        request.setService(service);
        request.setCacheId(1L);
        return request;
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
     * Single-lane service fixture with deterministic ownership and response
     * capture.
     */
    public static class TestService
            extends PartitionedCache
        {
        TestService(TestStorage storage, Set<Integer> setOwned)
            {
            super("PartitionedCacheStreamingQueryTest", null, true);
            f_storage = storage;
            f_setOwned.addAll(setOwned);
            setPartitionCount(17);
            setMaxPartialResponseSize(new MemorySize(1024L * 1024L));
            }

        @Override
        protected int reservePartitionedWorkLanes(int cPartitions)
            {
            return 1;
            }

        @Override
        protected boolean pinOwnedPartition(int nPartition)
            {
            if (f_setOwned.contains(nPartition))
                {
                f_listPinned.add(nPartition);
                return true;
                }
            return false;
            }

        @Override
        public InvocationContext ensureInvocationContext(int nPartition)
            {
            InvocationContext context = mock(InvocationContext.class);
            PartitionSet      parts   = new PartitionSet(getPartitionCount(), nPartition);
            when(context.getPrePinnedPartitions()).thenReturn(parts);
            return context;
            }

        @Override
        public void processChanges()
            {
            m_cProcessChanges.incrementAndGet();
            if (m_eProcessChanges != null)
                {
                throw m_eProcessChanges;
                }
            }

        @Override
        public void unpinPartitions(PartitionSet parts)
            {
            for (int nPartition : parts)
                {
                f_setUnpinned.add(nPartition);
                }
            }

        @Override
        public Message instantiateMessage(String sMsgName)
            {
            if ("PartitionedQueryResponse".equals(sMsgName))
                {
                PartitionedQueryResponse response = new PartitionedQueryResponse();
                response.setService(this);
                return response;
                }
            if ("PartialValueResponse".equals(sMsgName))
                {
                PartialValueResponse response = new PartialValueResponse();
                response.setService(this);
                return response;
                }
            return super.instantiateMessage(sMsgName);
            }

        @Override
        public void post(Message msg)
            {
            f_listResponse.add((QueryResponse) msg);
            }

        @Override
        protected void processChanges(Message msgResponse)
            {
            f_listAggregateResponse.add((PartialValueResponse) msgResponse);
            }

        @Override
        protected Object convertPartitionedAggregateResult(Object oResult)
            {
            return oResult;
            }

        List<QueryResponse> getResponses()
            {
            return f_listResponse;
            }

        List<PartialValueResponse> getAggregateResponses()
            {
            return f_listAggregateResponse;
            }

        List<Integer> getPinnedPartitions()
            {
            return f_listPinned;
            }

        Set<Integer> getUnpinnedPartitions()
            {
            return f_setUnpinned;
            }

        int getProcessChangesCount()
            {
            return m_cProcessChanges.get();
            }

        void setProcessChangesFailure(RuntimeException e)
            {
            m_eProcessChanges = e;
            }

        private final TestStorage         f_storage;
        private final Set<Integer>        f_setOwned = new HashSet<>();
        private final List<Integer>       f_listPinned = Collections.synchronizedList(new ArrayList<>());
        private final Set<Integer>        f_setUnpinned = Collections.synchronizedSet(new HashSet<>());
        private final List<QueryResponse> f_listResponse =
                Collections.synchronizedList(new ArrayList<>());
        private final List<PartialValueResponse> f_listAggregateResponse =
                Collections.synchronizedList(new ArrayList<>());
        private final AtomicInteger       m_cProcessChanges = new AtomicInteger();
        private RuntimeException          m_eProcessChanges;
        }

    /**
     * Storage fixture that returns one binary key per partition.
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
            return f_version;
            }

        @Override
        public void checkAccess(RequestContext context, int nAccessRequired, int nReason)
            {
            ++m_cAccessChecks;
            }

        @Override
        public int calculateSize(PartitionSet parts, boolean fStrict)
            {
            return 1;
            }

        @Override
        protected QueryResult queryInternal(Filter filter, int nQueryType,
                PartitionSet parts, long lIdxVersion)
            {
            int nPartition = parts.next(0);
            f_listQueried.add(nPartition);

            if (m_cQueryDelayMillis > 0L)
                {
                try
                    {
                    Thread.sleep(m_cQueryDelayMillis);
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                    }
                }

            if (nPartition == m_nFailPartition)
                {
                throw new IllegalStateException("expected partition failure");
                }

            Binary binKey = ExternalizableHelper.toBinary("key-" + nPartition);
            return new QueryResult(parts, new Object[] {binKey}, 1);
            }

        @Override
        public Object aggregatePartition(Filter filter,
                InvocableMap.StreamingAggregator agent, PartitionSet parts)
            {
            int nPartition = parts.next(0);
            f_listAggregated.add(nPartition);

            if (nPartition == m_nFailPartition)
                {
                throw new IllegalStateException("expected partition failure");
                }

            return nPartition;
            }

        @Override
        protected void updateQueryStatistics(Filter filter, boolean fOptimized,
                long ldtStart, int cTotal, int cScanned, int cResults,
                int nQueryType, PartitionSet parts)
            {
            ++m_cStatisticsUpdates;
            }

        void setFailPartition(int nPartition)
            {
            m_nFailPartition = nPartition;
            }

        void setQueryDelayMillis(long cMillis)
            {
            m_cQueryDelayMillis = cMillis;
            }

        int getAccessChecks()
            {
            return m_cAccessChecks;
            }

        int getStatisticsUpdates()
            {
            return m_cStatisticsUpdates;
            }

        List<Integer> getQueriedPartitions()
            {
            return f_listQueried;
            }

        List<Integer> getAggregatedPartitions()
            {
            return f_listAggregated;
            }

        private final StorageVersion f_version = new StorageVersion();
        private final List<Integer>  f_listQueried = Collections.synchronizedList(new ArrayList<>());
        private final List<Integer>  f_listAggregated = Collections.synchronizedList(new ArrayList<>());
        private int                  m_cAccessChecks;
        private int                  m_cStatisticsUpdates;
        private int                  m_nFailPartition = -1;
        private long                 m_cQueryDelayMillis;
        }
    }
