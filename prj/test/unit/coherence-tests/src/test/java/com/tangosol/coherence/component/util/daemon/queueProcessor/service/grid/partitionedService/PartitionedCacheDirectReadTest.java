/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService;

import com.tangosol.coherence.component.net.Message;
import com.tangosol.coherence.component.net.RequestContext;
import com.tangosol.coherence.component.net.message.responseMessage.SimpleResponse;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.Storage;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.security.StorageAccessAuthorizer;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ObservableHashMap;

import org.junit.Test;

import javax.security.auth.Subject;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the direct read branch in {@link PartitionedCache}.
 *
 * @author Aleks Seovic  2026.04.27
 * @since 26.04
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class PartitionedCacheDirectReadTest
    {
    @Test
    public void shouldUseDirectPathForPlainGet()
        {
        Binary      binKey   = binary("key-1");
        Binary      binValue = binary("value-1");
        TestStorage storage  = plainStorage();
        TestService service  = new TestService(storage);

        storage.put(binKey, binValue);

        PartitionedCache.GetRequest request = getRequest(service, binKey);

        service.onGetRequest(request);

        SimpleResponse response = (SimpleResponse) service.getPostedMessage();

        assertThat(response.getValue(), is(binValue));
        assertThat(storage.getDirectCount(), is(1));
        assertThat(storage.getTrackedGetCount(), is(0));
        assertThat(service.getEnsureInvocationContextCount(), is(0));
        assertThat(service.getProcessChangesCount(), is(0));
        assertThat(service.getPinCount(), is(1));
        assertThat(service.getUnpinCount(), is(1));
        }

    @Test
    public void shouldPreserveReadAuthorizationOnPlainDirectGet()
        {
        Binary             binKey     = binary("key-2");
        Binary             binValue   = binary("value-2");
        Subject            subject    = new Subject();
        CountingAuthorizer authorizer = new CountingAuthorizer();
        TestStorage        storage    = plainStorage();
        TestService        service    = new TestService(storage);

        storage.put(binKey, binValue);
        storage.setTestAccessAuthorizer(authorizer);

        PartitionedCache.GetRequest request = getRequest(service, binKey);
        RequestContext              context = new RequestContext();
        context.setSubject(subject);
        request.setRequestContext(context);

        service.onGetRequest(request);

        assertThat(authorizer.getReadCount(), is(1));
        assertThat(authorizer.getLastEntry().getBinaryKey(), is(binKey));
        assertThat(authorizer.getLastSubject(), sameInstance(subject));
        assertThat(authorizer.getLastReason(), is(StorageAccessAuthorizer.REASON_GET));
        assertThat(storage.getDirectCount(), is(1));
        assertThat(service.getEnsureInvocationContextCount(), is(0));
        assertThat(service.getProcessChangesCount(), is(0));
        }

    @Test
    public void shouldKeepWriteCapableGetOnExistingPath()
        {
        Binary      binKey   = binary("key-3");
        Binary      binValue = binary("value-3");
        TestStorage storage  = writeCapableStorage();
        TestService service  = new TestService(storage);

        storage.put(binKey, binValue);
        service.prepareInvocationContext();

        service.onGetRequest(getRequest(service, binKey));

        assertThat(storage.getTrackedGetCount(), is(1));
        assertThat(storage.getDirectCount(), is(0));
        assertThat(service.getEnsureInvocationContextCount(), is(1));
        assertThat(service.getProcessChangesCount(), is(1));
        }

    @Test
    public void shouldUseDirectPathForPlainGetAll()
        {
        Binary      binKeyOne   = binary("key-4");
        Binary      binKeyTwo   = binary("key-5");
        Binary      binValueOne = binary("value-4");
        Binary      binValueTwo = binary("value-5");
        TestStorage storage     = plainStorage();
        TestService service     = new TestService(storage);

        service.setPartition(binKeyOne, 0);
        service.setPartition(binKeyTwo, 1);
        service.setPrimaryPartitions(Set.of(Integer.valueOf(0), Integer.valueOf(1)));
        storage.put(binKeyOne, binValueOne);
        storage.put(binKeyTwo, binValueTwo);

        PartitionedCache.GetAllRequest request = getAllRequest(service, keys(binKeyOne, binKeyTwo));

        service.onGetAllRequest(request);

        PartitionedCache.PartialMapResponse response =
                (PartitionedCache.PartialMapResponse) service.getPostedMessage();

        assertThat(response.getSize(), is(2));
        assertThat(storage.getAllDirectCount(), is(1));
        assertThat(storage.getTrackedGetAllCount(), is(0));
        assertThat(service.getEnsureInvocationContextCount(), is(0));
        assertThat(service.getProcessChangesCount(), is(0));
        assertThat(service.getPinCount(), is(2));
        assertThat(service.getUnpinPartitionSetCount(), is(1));
        }

    @Test
    public void shouldPreserveReadAnyAuthorizationOnPlainDirectGetAll()
        {
        Binary             binKeyOne   = binary("key-4a");
        Binary             binKeyTwo   = binary("key-5a");
        Binary             binValueOne = binary("value-4a");
        Binary             binValueTwo = binary("value-5a");
        Subject            subject     = new Subject();
        CountingAuthorizer authorizer  = new CountingAuthorizer();
        TestStorage        storage     = plainStorage();
        TestService        service     = new TestService(storage);

        service.setPartition(binKeyOne, 0);
        service.setPartition(binKeyTwo, 1);
        service.setPrimaryPartitions(Set.of(Integer.valueOf(0), Integer.valueOf(1)));
        storage.put(binKeyOne, binValueOne);
        storage.put(binKeyTwo, binValueTwo);
        storage.setTestAccessAuthorizer(authorizer);

        PartitionedCache.GetAllRequest request = getAllRequest(service, keys(binKeyOne, binKeyTwo));
        RequestContext                 context = new RequestContext();
        context.setSubject(subject);
        request.setRequestContext(context);

        service.onGetAllRequest(request);

        assertThat(authorizer.getReadAnyCount(), is(1));
        assertThat(authorizer.getLastSubject(), sameInstance(subject));
        assertThat(authorizer.getLastReason(), is(StorageAccessAuthorizer.REASON_GET));
        assertThat(storage.getAllDirectCount(), is(1));
        assertThat(service.getEnsureInvocationContextCount(), is(0));
        assertThat(service.getProcessChangesCount(), is(0));
        }

    @Test
    public void shouldKeepWriteCapableGetAllOnExistingPath()
        {
        Binary      binKey   = binary("key-6");
        Binary      binValue = binary("value-6");
        TestStorage storage  = writeCapableStorage();
        TestService service  = new TestService(storage);

        storage.put(binKey, binValue);
        service.prepareInvocationContext();

        service.onGetAllRequest(getAllRequest(service, keys(binKey)));

        assertThat(storage.getTrackedGetAllCount(), is(1));
        assertThat(storage.getAllDirectCount(), is(0));
        assertThat(service.getEnsureInvocationContextCount(), is(1));
        assertThat(service.getProcessChangesCount(), is(1));
        }

    @Test
    public void shouldReadBackupDirectlyForPlainBackupGet()
        {
        Binary      binKey   = binary("key-7");
        Binary      binValue = binary("value-7");
        TestStorage storage  = plainStorage();
        TestService service  = new TestService(storage);

        service.setPrimaryOwner(false);
        service.setBackupOwner(true);
        storage.putBackup(binKey, binValue);

        PartitionedCache.GetRequest request = getRequest(service, binKey);
        request.setAllowBackupRead(true);

        service.onGetRequest(request);

        SimpleResponse response = (SimpleResponse) service.getPostedMessage();

        assertThat(response.getValue(), is(binValue));
        assertThat(storage.getBackupGetCount(), is(1));
        assertThat(storage.getDirectCount(), is(0));
        assertThat(service.getEnsureInvocationContextCount(), is(0));
        assertThat(service.getProcessChangesCount(), is(0));
        }

    @Test
    public void shouldReadMixedPrimaryAndBackupGetAllDirectly()
        {
        Binary      binPrimary = binary("primary-key");
        Binary      binBackup  = binary("backup-key");
        Binary      binValueP  = binary("primary-value");
        Binary      binValueB  = binary("backup-value");
        TestStorage storage    = plainStorage();
        TestService service    = new TestService(storage);

        service.setPartition(binPrimary, 0);
        service.setPartition(binBackup, 1);
        service.setPrimaryPartitions(Set.of(Integer.valueOf(0)));
        service.setBackupPartitions(Set.of(Integer.valueOf(1)));
        storage.put(binPrimary, binValueP);
        storage.putBackup(binBackup, binValueB);

        PartitionedCache.GetAllRequest request = getAllRequest(service, keys(binPrimary, binBackup));
        request.setAllowBackupRead(true);

        service.onGetAllRequest(request);

        PartitionedCache.PartialMapResponse response =
                (PartitionedCache.PartialMapResponse) service.getPostedMessage();

        assertThat(response.getSize(), is(2));
        assertThat(storage.getAllDirectCount(), is(1));
        assertThat(storage.getAllBackupCount(), is(1));
        assertThat(service.getEnsureInvocationContextCount(), is(0));
        assertThat(service.getProcessChangesCount(), is(0));
        assertThat(service.getPinCount(), is(1));
        assertThat(service.getUnpinPartitionSetCount(), is(1));
        }

    private static TestStorage plainStorage()
        {
        return new TestStorage(false);
        }

    private static TestStorage writeCapableStorage()
        {
        return new TestStorage(true);
        }

    private static PartitionedCache.GetRequest getRequest(TestService service, Binary binKey)
        {
        PartitionedCache.GetRequest request = new PartitionedCache.GetRequest();
        request.setService(service);
        request.setCacheId(1L);
        request.setKey(binKey);
        return request;
        }

    private static PartitionedCache.GetAllRequest getAllRequest(TestService service, Set setKeys)
        {
        PartitionedCache.GetAllRequest request = new PartitionedCache.GetAllRequest();
        request.setService(service);
        request.setCacheId(1L);
        request.setKeySet(setKeys);
        return request;
        }

    private static Set keys(Binary... aBinKey)
        {
        Set setKeys = new LinkedHashSet();
        for (Binary binKey : aBinKey)
            {
            setKeys.add(binKey);
            }
        return setKeys;
        }

    private static Binary binary(String s)
        {
        return ExternalizableHelper.toBinary(s);
        }

    public static class TestService
            extends PartitionedCache
        {
        TestService(TestStorage storage)
            {
            super("PartitionedCacheDirectReadTest", null, true);
            f_storage = storage;
            storage.setService(this);
            setPartitionCount(17);
            m_setPrimaryPartitions.add(Integer.valueOf(0));
            }

        @Override
        public Message instantiateMessage(String sMsgName)
            {
            Message message;
            if ("Response".equals(sMsgName))
                {
                message = new PartitionedCache.Response();
                }
            else if ("PartialMapResponse".equals(sMsgName))
                {
                message = new PartitionedCache.PartialMapResponse();
                }
            else
                {
                message = super.instantiateMessage(sMsgName);
                }
            message.setService(this);
            return message;
            }

        @Override
        protected Storage validateRequestForStorage(com.tangosol.coherence.component.net.message.RequestMessage msgRequest,
                Message msgResponse, boolean fEnsureSupport)
            {
            return f_storage;
            }

        @Override
        public PartitionedCache.InvocationContext ensureInvocationContext()
            {
            ++m_cEnsureInvocationContext;
            return f_ctxInvoke;
            }

        @Override
        protected void processChanges(RequestContext context, Binary binKey, Storage.EntryStatus status,
                long lCacheId, Message msgResponse)
            {
            ++m_cProcessChanges;
            post(msgResponse);
            }

        @Override
        protected void processChanges(RequestContext context, com.tangosol.coherence.component.util.PartialJob job,
                long lCacheId, Collection colStatus, PartitionedCache.BatchContext ctxBatch)
            {
            ++m_cProcessChanges;
            post(ctxBatch.getPrimaryResponse());
            }

        @Override
        public void post(Message msg)
            {
            f_msgPosted.set(msg);
            }

        @Override
        public int getKeyPartition(Binary binKey)
            {
            Integer IPart = (Integer) f_mapPartitions.get(binKey);
            return IPart == null ? 0 : IPart.intValue();
            }

        @Override
        public boolean isPrimaryOwner(int iPartition)
            {
            return m_setPrimaryPartitions.contains(Integer.valueOf(iPartition));
            }

        @Override
        public boolean isBackupOwner(int iPartition)
            {
            return m_setBackupPartitions.contains(Integer.valueOf(iPartition));
            }

        @Override
        protected boolean pinOwnedPartition(int nPartition)
            {
            if (isPrimaryOwner(nPartition))
                {
                ++m_cPin;
                return true;
                }
            return false;
            }

        @Override
        public void unpinPartition(int iPartition)
            {
            ++m_cUnpin;
            }

        @Override
        public void unpinPartitions(com.tangosol.net.partition.PartitionSet parts)
            {
            ++m_cUnpinPartitionSet;
            }

        void prepareInvocationContext()
            {
            f_ctxInvoke = mock(PartitionedCache.InvocationContext.class);
            Storage.EntryStatus status = mock(Storage.EntryStatus.class);
            when(f_ctxInvoke.lockEntry(any(Storage.class), any(Binary.class), anyBoolean())).thenReturn(status);
            when(f_ctxInvoke.getEntryStatuses()).thenReturn(Set.of(status));
            when(f_ctxInvoke.getPrePinnedPartitions()).thenReturn(instantiatePartitionSet(false));
            }

        Message getPostedMessage()
            {
            return f_msgPosted.get();
            }

        int getEnsureInvocationContextCount()
            {
            return m_cEnsureInvocationContext;
            }

        int getProcessChangesCount()
            {
            return m_cProcessChanges;
            }

        int getPinCount()
            {
            return m_cPin;
            }

        int getUnpinCount()
            {
            return m_cUnpin;
            }

        int getUnpinPartitionSetCount()
            {
            return m_cUnpinPartitionSet;
            }

        void setPrimaryOwner(boolean fPrimaryOwner)
            {
            m_setPrimaryPartitions.clear();
            if (fPrimaryOwner)
                {
                m_setPrimaryPartitions.add(Integer.valueOf(0));
                }
            }

        void setBackupOwner(boolean fBackupOwner)
            {
            m_setBackupPartitions.clear();
            if (fBackupOwner)
                {
                m_setBackupPartitions.add(Integer.valueOf(0));
                }
            }

        void setPartition(Binary binKey, int nPartition)
            {
            f_mapPartitions.put(binKey, Integer.valueOf(nPartition));
            }

        void setPrimaryPartitions(Set<Integer> setPartitions)
            {
            m_setPrimaryPartitions.clear();
            m_setPrimaryPartitions.addAll(setPartitions);
            }

        void setBackupPartitions(Set<Integer> setPartitions)
            {
            m_setBackupPartitions.clear();
            m_setBackupPartitions.addAll(setPartitions);
            }

        private final TestStorage             f_storage;
        private final AtomicReference<Message> f_msgPosted = new AtomicReference<>();
        private final Map                     f_mapPartitions = new HashMap();
        private final Set<Integer>            m_setPrimaryPartitions = new LinkedHashSet<>();
        private final Set<Integer>            m_setBackupPartitions  = new LinkedHashSet<>();
        private PartitionedCache.InvocationContext f_ctxInvoke;
        private int m_cEnsureInvocationContext;
        private int m_cProcessChanges;
        private int m_cPin;
        private int m_cUnpin;
        private int m_cUnpinPartitionSet;
        }

    public static class TestStorage
            extends Storage
        {
        TestStorage(boolean fMayWriteOnRead)
            {
            super(null, null, true);
            f_fMayWriteOnRead = fMayWriteOnRead;
            setBackingMapInternal(f_mapPrimary);
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

        @Override
        public boolean mayWriteOnRead()
            {
            return f_fMayWriteOnRead;
            }

        @Override
        public Binary getDirect(Binary binKey)
            {
            ++m_cGetDirect;
            return super.getDirect(binKey);
            }

        @Override
        public Map getAllDirect(Collection colKeys)
            {
            ++m_cGetAllDirect;
            return super.getAllDirect(colKeys);
            }

        @Override
        public Binary get(PartitionedCache.InvocationContext ctxInvoke, EntryStatus status, Binary binKey)
            {
            ++m_cTrackedGet;
            return (Binary) f_mapPrimary.get(binKey);
            }

        @Override
        public Map getAll(PartitionedCache.InvocationContext ctxInvoke, Collection colKeys)
            {
            ++m_cTrackedGetAll;
            return super.getAllDirect(colKeys);
            }

        @Override
        public Binary getFromBackup(Binary binKey)
            {
            ++m_cBackupGet;
            return (Binary) f_mapBackup.get(binKey);
            }

        @Override
        public Map getAllFromBackup(Map mapPartKeys, com.tangosol.net.partition.PartitionSet partsReject)
            {
            ++m_cBackupGetAll;
            Map mapResult = new HashMap();
            for (Object oKeys : mapPartKeys.values())
                {
                for (Object oKey : (Collection) oKeys)
                    {
                    Binary binKey = (Binary) oKey;
                    Binary binVal = (Binary) f_mapBackup.get(binKey);
                    if (binVal != null)
                        {
                        mapResult.put(binKey, binVal);
                        }
                    }
                }
            return mapResult;
            }

        void setService(PartitionedCache service)
            {
            m_service = service;
            }

        void setTestAccessAuthorizer(StorageAccessAuthorizer authorizer)
            {
            setAccessAuthorizer(authorizer);
            }

        void put(Binary binKey, Binary binValue)
            {
            f_mapPrimary.put(binKey, binValue);
            }

        void putBackup(Binary binKey, Binary binValue)
            {
            f_mapBackup.put(binKey, binValue);
            }

        int getDirectCount()
            {
            return m_cGetDirect;
            }

        int getAllDirectCount()
            {
            return m_cGetAllDirect;
            }

        int getTrackedGetCount()
            {
            return m_cTrackedGet;
            }

        int getTrackedGetAllCount()
            {
            return m_cTrackedGetAll;
            }

        int getBackupGetCount()
            {
            return m_cBackupGet;
            }

        int getAllBackupCount()
            {
            return m_cBackupGetAll;
            }

        private final boolean           f_fMayWriteOnRead;
        private final ObservableHashMap f_mapPrimary = new ObservableHashMap();
        private final Map               f_mapBackup  = new HashMap();
        private PartitionedCache m_service;
        private int m_cGetDirect;
        private int m_cGetAllDirect;
        private int m_cTrackedGet;
        private int m_cTrackedGetAll;
        private int m_cBackupGet;
        private int m_cBackupGetAll;
        }

    public static class CountingAuthorizer
            implements StorageAccessAuthorizer
        {
        @Override
        public void checkRead(BinaryEntry entry, Subject subject, int nReason)
            {
            ++m_cRead;
            m_entryLast   = entry;
            m_subjectLast = subject;
            m_nReasonLast = nReason;
            }

        @Override
        public void checkWrite(BinaryEntry entry, Subject subject, int nReason)
            {
            }

        @Override
        public void checkReadAny(BackingMapContext context, Subject subject, int nReason)
            {
            ++m_cReadAny;
            m_subjectLast = subject;
            m_nReasonLast = nReason;
            }

        @Override
        public void checkWriteAny(BackingMapContext context, Subject subject, int nReason)
            {
            }

        int getReadCount()
            {
            return m_cRead;
            }

        BinaryEntry getLastEntry()
            {
            return m_entryLast;
            }

        Subject getLastSubject()
            {
            return m_subjectLast;
            }

        int getLastReason()
            {
            return m_nReasonLast;
            }

        int getReadAnyCount()
            {
            return m_cReadAny;
            }

        private int m_cRead;
        private int m_cReadAny;
        private BinaryEntry m_entryLast;
        private Subject m_subjectLast;
        private int m_nReasonLast;
        }
    }
