/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService;

import com.tangosol.coherence.component.net.Message;
import com.tangosol.coherence.component.net.RequestContext;
import com.tangosol.coherence.component.net.message.RequestMessage;
import com.tangosol.coherence.component.util.PartialJob;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService$PinningIterator;

import com.tangosol.net.internal.PartitionVersions;
import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.Binary;

import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests for the final bug 39635940 review findings.
 *
 * @author fryp  2026.07.15
 * @since 26.04
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class PartitionedCacheFinalFollowupTest
    {
    @Test
    public void shouldPreservePriorKeyResultsWhenRemainingInvocationFails()
        {
        RuntimeException        expected = new RuntimeException("remaining key invocation failed");
        Map<Binary, Binary>      mapPrev  = new LinkedHashMap<>();
        InvokeFailureCache      service;
        FailingInvokeAllRequest request  = new FailingInvokeAllRequest(expected);
        Set<Binary>             setKeys  = new LinkedHashSet<>();

        mapPrev.put(KEY_PREVIOUS, RESULT_PREVIOUS);
        service = new InvokeFailureCache(mapPrev);
        setKeys.add(KEY_PREVIOUS);
        setKeys.add(KEY_REMAINING);
        request.setKeySet(setKeys);

        service.onInvokeAllRequest(request);

        PartitionedCache$PartialMapResponse response = service.getPartialMapResponse();
        assertSame(expected, response.getException());
        assertEquals(1, response.getSize());
        assertSame(KEY_PREVIOUS, response.getKey()[0]);
        assertSame(RESULT_PREVIOUS, response.getValue()[0]);
        assertEquals(1, service.getRegistrationCount());
        assertTrue(service.getRegisteredResults().isEmpty());
        assertEquals(1, service.getProcessChangesCount());
        }

    @Test
    public void shouldPreservePriorFilterResultsWhenRemainingInvocationFails()
        {
        RuntimeException           expected = new RuntimeException("remaining filter invocation failed");
        Map<Binary, Binary>         mapPrev  = new LinkedHashMap<>();
        InvokeFailureCache         service;
        FailingInvokeFilterRequest request  = new FailingInvokeFilterRequest(expected);
        PartitionSet               partMask = new PartitionSet(PARTITION_COUNT);

        mapPrev.put(KEY_PREVIOUS, RESULT_PREVIOUS);
        service = new InvokeFailureCache(mapPrev);
        partMask.add(PARTITION_PREVIOUS);
        partMask.add(PARTITION_REMAINING);
        request.setRequestMask(partMask);

        service.onInvokeFilterRequest(request);

        PartitionedCache$QueryResponse response = service.getQueryResponse();
        Map.Entry                      entry    = (Map.Entry) response.getResult()[0];
        assertSame(expected, response.getException());
        assertEquals(1, response.getSize());
        assertSame(KEY_PREVIOUS, entry.getKey());
        assertSame(RESULT_PREVIOUS, entry.getValue());
        assertEquals(1, service.getRegistrationCount());
        assertTrue(service.getRegisteredResults().isEmpty());
        assertEquals(1, service.getProcessChangesCount());
        }

    @Test
    public void shouldSuppressInvocationAndUnpinFailuresOntoRequestFailure()
        {
        RuntimeException        eOperation = new RuntimeException("request operation failed");
        RuntimeException        eContext   = new RuntimeException("context release failed");
        RuntimeException        eUnpin     = new RuntimeException("partition unpin failed");
        InvokeFailureCache      service    = new InvokeFailureCache(null);
        FailingInvokeAllRequest request    = new FailingInvokeAllRequest(eOperation);
        Set<Binary>             setKeys    = new LinkedHashSet<>();

        service.setCleanupFailures(eContext, eUnpin);
        setKeys.add(KEY_REMAINING);
        request.setKeySet(setKeys);

        service.onInvokeAllRequest(request);

        assertSame(eOperation, service.getPartialMapResponse().getException());
        assertEquals(2, eOperation.getSuppressed().length);
        assertSame(eContext, eOperation.getSuppressed()[0]);
        assertSame(eUnpin, eOperation.getSuppressed()[1]);
        assertEquals(1, service.getContextReleaseCount());
        assertEquals(1, service.getUnpinCount());
        }

    // ----- helper classes -------------------------------------------------

    private static class FailingInvokeAllRequest
            extends PartitionedCache$InvokeAllRequest
        {
        FailingInvokeAllRequest(RuntimeException expected)
            {
            m_expected = expected;
            }

        @Override
        public com.tangosol.util.InvocableMap.EntryProcessor deserializeProcessor()
            {
            throw m_expected;
            }

        @Override
        public Set getKeySetSafe()
            {
            return new LinkedHashSet(getKeySet());
            }

        private final RuntimeException m_expected;
        }

    private static class FailingInvokeFilterRequest
            extends PartitionedCache$InvokeFilterRequest
        {
        FailingInvokeFilterRequest(RuntimeException expected)
            {
            m_expected = expected;
            }

        @Override
        public com.tangosol.util.InvocableMap.EntryProcessor deserializeProcessor()
            {
            throw m_expected;
            }

        @Override
        public PartitionSet getRequestMaskSafe()
            {
            return new PartitionSet(getRequestMask());
            }

        private final RuntimeException m_expected;
        }

    private static class InvokeFailureCache
            extends PartitionedCache
        {
        InvokeFailureCache(Map mapPrevious)
            {
            super(null, null, false);

            m_context    = new TestInvocationContext(this);
            m_resultInfo = mapPrevious == null ? null : new PreviousResultInfo(this, mapPrevious);
            m_storage    = new PartitionedCache$Storage(null, null, false);
            }

        @Override
        public Message instantiateMessage(String sName)
            {
            if ("PartialMapResponse".equals(sName))
                {
                return m_responseMap = new PartitionedCache$PartialMapResponse(null, this, true);
                }
            if ("QueryResponse".equals(sName))
                {
                return m_responseQuery = new PartitionedCache$QueryResponse(null, this, true);
                }
            throw new IllegalArgumentException(sName);
            }

        @Override
        protected PartitionedCache$Storage validateRequestForStorage(RequestMessage msgRequest, Message msgResponse,
                boolean fEnsureSupport)
            {
            return m_storage;
            }

        @Override
        protected PartitionedCache$ResultInfo getResultInfo(RequestContext context)
            {
            return m_resultInfo;
            }

        @Override
        public int getPartitionCount()
            {
            return PARTITION_COUNT;
            }

        @Override
        public int getKeyPartition(Binary binKey)
            {
            return KEY_PREVIOUS.equals(binKey) ? PARTITION_PREVIOUS : PARTITION_REMAINING;
            }

        @Override
        public boolean isConcurrent()
            {
            return false;
            }

        @Override
        public boolean isPrimaryOwner(int nPartition)
            {
            return true;
            }

        @Override
        protected boolean flushOOBEvents()
            {
            return true;
            }

        @Override
        protected PartitionedService$PinningIterator createPinningIterator(Set set,
                PartitionVersions versions)
            {
            PartitionedService$PinningIterator pinner =
                    new PartitionedService$PinningIterator(null, this, true);
            pinner.setFullSet(set);
            pinner.setPartitionVersions(versions);
            return pinner;
            }

        @Override
        protected PartitionSet pinOwnedPartitions(PartitionSet partitions, PartitionVersions versions)
            {
            return new PartitionSet(PARTITION_COUNT);
            }

        @Override
        public PartitionedCache$InvocationContext ensureInvocationContext(PartitionSet partitions)
            {
            return m_context;
            }

        @Override
        protected void registerMultiResult(RequestContext context, PartitionSet partsResults, Map mapResults)
            {
            ++m_cRegistration;
            m_mapRegistered = mapResults;
            }

        @Override
        public PartitionedCache$BatchContext instantiateBatchContext(Message msgResponse)
            {
            return null;
            }

        @Override
        protected void processChanges(RequestContext context, PartialJob job, long lCacheId,
                Collection colStatus, PartitionedCache$BatchContext contextBatch)
            {
            ++m_cProcessChanges;
            }

        @Override
        public void unpinPartitions(PartitionSet partitions)
            {
            ++m_cUnpin;
            if (m_eUnpin != null)
                {
                throw m_eUnpin;
                }
            }

        @Override
        public RuntimeException tagException(Throwable e)
            {
            return (RuntimeException) e;
            }

        void setCleanupFailures(RuntimeException eContext, RuntimeException eUnpin)
            {
            m_context.setReleaseFailure(eContext);
            m_eUnpin = eUnpin;
            }

        PartitionedCache$PartialMapResponse getPartialMapResponse()
            {
            return m_responseMap;
            }

        PartitionedCache$QueryResponse getQueryResponse()
            {
            return m_responseQuery;
            }

        int getRegistrationCount()
            {
            return m_cRegistration;
            }

        Map getRegisteredResults()
            {
            return m_mapRegistered;
            }

        int getProcessChangesCount()
            {
            return m_cProcessChanges;
            }

        int getContextReleaseCount()
            {
            return m_context.getReleaseCount();
            }

        int getUnpinCount()
            {
            return m_cUnpin;
            }

        private final TestInvocationContext         m_context;
        private final PartitionedCache$ResultInfo  m_resultInfo;
        private final PartitionedCache$Storage                      m_storage;

        private PartitionedCache$PartialMapResponse m_responseMap;
        private PartitionedCache$QueryResponse      m_responseQuery;
        private Map                                 m_mapRegistered;
        private RuntimeException                    m_eUnpin;
        private int                                 m_cRegistration;
        private int                                 m_cProcessChanges;
        private int                                 m_cUnpin;
        }

    private static class PreviousResultInfo
            extends PartitionedCache$ResultInfo
        {
        PreviousResultInfo(PartitionedCache service, Map mapPrevious)
            {
            super(null, service, false);

            m_mapPrevious = mapPrevious;
            }

        @Override
        public synchronized Map extractResults(PartitionSet partMask)
            {
            if (partMask.contains(PARTITION_PREVIOUS))
                {
                partMask.remove(PARTITION_PREVIOUS);
                return m_mapPrevious;
                }
            return null;
            }

        private final Map m_mapPrevious;
        }

    private static class TestInvocationContext
            extends PartitionedCache$InvocationContext
        {
        TestInvocationContext(PartitionedCache service)
            {
            super(null, service, false);
            }

        @Override
        public void prepareAccess(RequestContext context, PartitionedCache$Storage storage, int nAccessRequired, int nReason)
            {
            }

        @Override
        public Collection getEntryStatuses()
            {
            return Collections.emptyList();
            }

        @Override
        public void resetAccess()
            {
            }

        @Override
        public void release(boolean fUnpin)
            {
            ++m_cRelease;
            if (m_eRelease != null)
                {
                throw m_eRelease;
                }
            }

        void setReleaseFailure(RuntimeException e)
            {
            m_eRelease = e;
            }

        int getReleaseCount()
            {
            return m_cRelease;
            }

        private RuntimeException m_eRelease;
        private int              m_cRelease;
        }

    private static final int PARTITION_COUNT     = 31;
    private static final int PARTITION_PREVIOUS  = 1;
    private static final int PARTITION_REMAINING = 2;

    private static final Binary KEY_PREVIOUS     = new Binary(new byte[] {1});
    private static final Binary KEY_REMAINING    = new Binary(new byte[] {2});
    private static final Binary RESULT_PREVIOUS  = new Binary(new byte[] {3});
    }
