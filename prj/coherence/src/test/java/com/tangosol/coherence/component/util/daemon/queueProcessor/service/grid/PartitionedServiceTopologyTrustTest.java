/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid;

import com.tangosol.coherence.Component;
import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache$TransferRequest;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache$Storage;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache$Storage$DeferredEvent;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;
import com.tangosol.io.internal.BridgeObjectInputFilter;
import com.tangosol.io.internal.DefaultObjectInputFilter;
import com.tangosol.net.partition.VersionedOwnership;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.MapEvent;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.WrapperObservableMap;

import org.junit.Test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for partition topology metadata decoding.
 *
 * @author OpenAI  2026.05.17
 *
 * @since 15.1.2.0
 */
public class PartitionedServiceTopologyTrustTest
    {
    @Test
    public void shouldRoundTripSupportedPartitionConfigValues()
            throws Exception
        {
        PartitionedService$PartitionConfig config = new PartitionedService$PartitionConfig();
        VersionedOwnership                 owners = new VersionedOwnership(1, 7);
        owners.setOwners(new int[] {1, 2});
        Binary binary = new Binary(new byte[] {1, 2, 3});

        assertEquals(Integer.valueOf(42), roundTrip(config, Integer.valueOf(42)));

        VersionedOwnership ownersResult = (VersionedOwnership) roundTrip(config, owners);
        assertArrayEquals(owners.getOwners(), ownersResult.getOwners());
        assertEquals(owners.getVersion(), ownersResult.getVersion());

        assertEquals(binary, roundTrip(config, binary));
        }

    @Test
    public void shouldRejectPartitionConfigTypeAnyBeforeMaterialization()
            throws Exception
        {
        Exploit.reset();

        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput out = buffer.getBufferOutput();
        out.write(PartitionedService$PartitionConfig.TYPE_ANY);
        ExternalizableHelper.writeObject(out, new Exploit());

        assertRejected(() -> new PartitionedService$PartitionConfig().readObject(buffer.toBinary().getBufferInput()));
        assertFalse(Exploit.wasRead());
        }

    @Test
    public void shouldRejectUnsupportedPartitionConfigWrites()
        {
        assertRejected(() ->
                new PartitionedService$PartitionConfig().writeObject(new ByteArrayWriteBuffer(64).getBufferOutput(), "bad"));
        }

    @Test
    public void shouldAcceptPrimaryTransferFromCurrentPrimaryToThisMember()
            throws Exception
        {
        Member               memberThis    = member(1);
        Member               memberPrimary = member(2);
        TestPartitionedCache service       = service(memberThis, memberPrimary);

        service.configureForTopology(new int[][] {{2, 1}}, true);

        assertTrue(service.isTransferTopologyValidForTest(
                transferRequest(0, 0, memberPrimary, owners(1, 2))));
        }

    @Test
    public void shouldAcceptPrimaryTransferWhenLocalAssignmentAlreadyNamesReceiver()
            throws Exception
        {
        Member               memberThis    = member(1);
        Member               memberPrimary = member(2);
        TestPartitionedCache service       = service(memberThis, memberPrimary);

        service.configureForTopology(new int[][] {{1, 2}}, true);

        assertTrue(service.isTransferTopologyValidForTest(
                transferRequest(0, 0, memberPrimary, owners(2, 1))));
        }

    @Test
    public void shouldRejectPrimaryTransferWhenSenderIsNotLocalOrMessagePrimary()
            throws Exception
        {
        Member               memberThis  = member(1);
        Member               memberOwner = member(2);
        Member               memberOther = member(3);
        TestPartitionedCache service     = service(memberThis, memberOwner, memberOther);

        service.configureForTopology(new int[][] {{2, 1}}, true);

        assertFalse(service.isTransferTopologyValidForTest(
                transferRequest(0, 0, memberOther, owners(2, 1))));
        }

    @Test
    public void shouldAcceptBackupTransferFromPartitionPrimary()
            throws Exception
        {
        Member               memberThis    = member(1);
        Member               memberPrimary = member(2);
        TestPartitionedCache service       = service(memberThis, memberPrimary);

        service.configureForTopology(new int[][] {{2, 1}}, false);

        assertTrue(service.isTransferTopologyValidForTest(
                transferRequest(0, 1, memberPrimary, owners(2, 1))));
        }

    @Test
    public void shouldRejectBackupTransferFromNonPrimarySender()
            throws Exception
        {
        Member               memberThis    = member(1);
        Member               memberPrimary = member(2);
        Member               memberOther   = member(3);
        TestPartitionedCache service       = service(memberThis, memberPrimary, memberOther);

        service.configureForTopology(new int[][] {{2, 1}}, false);

        assertFalse(service.isTransferTopologyValidForTest(
                transferRequest(0, 1, memberOther, owners(2, 1))));
        }

    @Test
    public void shouldKeepTransferAddendumsPassiveUntilTopologyValidation()
            throws Exception
        {
        Member               memberThis    = member(1);
        Member               memberPrimary = member(2);
        Member               memberOther   = member(3);
        TestPartitionedCache service       = service(memberThis, memberPrimary, memberOther);
        TestTransferRequest  request       = transferRequestForTest(0, 0, memberOther, owners(2, 1));

        service.configureForTopology(new int[][] {{2, 1}}, true);

        request.readAddendumsForTest(writeAddendum(42L));

        assertEquals(0, service.getEnsureStorageCount());
        assertEquals(0, service.getLinkedDeferredEventCount());

        service.onTransferRequest(request);

        assertEquals(1, service.getRejectedTransferCount());
        assertEquals(0, service.getEnsureStorageCount());
        assertEquals(0, service.getLinkedDeferredEventCount());
        }

    @Test
    public void shouldMaterializeTransferAddendumsAfterTopologyValidation()
            throws Exception
        {
        Member               memberThis    = member(1);
        Member               memberPrimary = member(2);
        TestPartitionedCache service       = service(memberThis, memberPrimary);
        TestTransferRequest  request       = transferRequestForTest(0, 0, memberPrimary, owners(2, 1));

        service.configureForTopology(new int[][] {{2, 1}}, true);
        request.readAddendumsForTest(writeAddendum(42L));

        assertTrue(service.isTransferTopologyValidForTest(request));

        service.onTransferRequestForTest(request);

        List listAddendum = request.getAddendums();
        assertEquals(1, listAddendum.size());
        assertTrue(listAddendum.get(0) instanceof PartitionedCache$Storage$DeferredEvent);
        assertSame(service.getLastStorage(), ((PartitionedCache$Storage$DeferredEvent) listAddendum.get(0)).getStorage());
        assertEquals(1, service.getEnsureStorageCount());
        assertEquals(1, service.getLinkedDeferredEventCount());
        }

    private static Object roundTrip(PartitionedService$PartitionConfig config, Object value)
            throws IOException
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        config.writeObject(buffer.getBufferOutput(), value);
        return config.readObject(buffer.toBinary().getBufferInput());
        }

    private static ReadBuffer.BufferInput writeObject(Object value)
            throws IOException
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        ExternalizableHelper.writeObject(buffer.getBufferOutput(), value);
        return buffer.toBinary().getBufferInput();
        }

    private static TestPartitionedCache service(Member memberThis, Member... aMember)
        {
        ServiceMemberSet setMember = new ServiceMemberSet();
        setMember.add(memberThis);
        for (Member member : aMember)
            {
            setMember.add(member);
            }
        setMember.setThisMember(memberThis);

        TestPartitionedCache service = new TestPartitionedCache();
        service.setThisMemberForTest(memberThis);
        service.setServiceMemberSet(setMember);
        return service;
        }

    private static PartitionedCache$TransferRequest transferRequest(int iPartition, int iStore, Member memberFrom,
            VersionedOwnership owners)
        {
        PartitionedCache$TransferRequest request = new PartitionedCache$TransferRequest();
        request.setPartition(iPartition);
        request.setStore(iStore);
        request.setFromMember(memberFrom);
        request.setOwners(owners);
        return request;
        }

    private static TestTransferRequest transferRequestForTest(int iPartition, int iStore, Member memberFrom,
            VersionedOwnership owners)
        {
        TestTransferRequest request = new TestTransferRequest();
        request.setPartition(iPartition);
        request.setStore(iStore);
        request.setFromMember(memberFrom);
        request.setOwners(owners);
        request.setLastInPartition(true);
        return request;
        }

    private static ReadBuffer.BufferInput writeAddendum(long lCacheId)
            throws IOException
        {
        Binary                binKey   = new Binary(new byte[] {1});
        Binary                binValue = new Binary(new byte[] {2});
        ByteArrayWriteBuffer  buffer   = new ByteArrayWriteBuffer(256);
        WriteBuffer.BufferOutput out   = buffer.getBufferOutput();

        out.writeInt(1);
        out.writeLong(lCacheId);
        out.writeBoolean(true);
        ExternalizableHelper.writeInt(out, MapEvent.ENTRY_INSERTED);
        ExternalizableHelper.writeObject(out, binKey);
        ExternalizableHelper.writeObject(out, null);
        ExternalizableHelper.writeObject(out, binValue);

        return buffer.toBinary().getBufferInput();
        }

    private static VersionedOwnership owners(int... anOwner)
        {
        VersionedOwnership owners = new VersionedOwnership(anOwner.length - 1, 1);
        owners.setOwners(anOwner);
        return owners;
        }

    private static Member member(int nId)
            throws IOException
        {
        Member member = new Member();
        member.configureDead(nId,
                new com.tangosol.util.UUID(System.currentTimeMillis(), InetAddress.getByName("127.0.0." + nId),
                        7574 + nId, nId),
                System.currentTimeMillis());
        return member;
        }

    private static void assertRejected(ThrowingRunnable runnable)
        {
        try
            {
            runnable.run();
            fail("expected rejection");
            }
        catch (IOException | RuntimeException e)
            {
            // expected
            }
        }

    private interface ThrowingRunnable
        {
        void run()
                throws IOException;
        }

    public static class Exploit
            implements Serializable
        {
        private void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException
            {
            s_fRead = true;
            in.defaultReadObject();
            }

        static void reset()
            {
            s_fRead = false;
            }

        static boolean wasRead()
            {
            return s_fRead;
            }

        private static boolean s_fRead;
        }

    public static class TestTransferRequest
            extends PartitionedCache$TransferRequest
        {
        void readAddendumsForTest(ReadBuffer.BufferInput input)
                throws IOException
            {
            int  cAddendum    = input.readInt();
            List listAddendum = new ArrayList(cAddendum);
            for (int i = 0; i < cAddendum; i++)
                {
                Object[] aoAddendum = new Object[6];
                aoAddendum[0] = Long.valueOf(input.readLong());
                aoAddendum[1] = Boolean.valueOf(input.readBoolean());
                aoAddendum[2] = Integer.valueOf(ExternalizableHelper.readInt(input));
                aoAddendum[3] = ExternalizableHelper.readObject(input);
                aoAddendum[4] = ExternalizableHelper.readObject(input);
                aoAddendum[5] = ExternalizableHelper.readObject(input);
                listAddendum.add(aoAddendum);
                }
            setAddendums(listAddendum);
            }
        }

    public static class TestPartitionedCache
            extends PartitionedCache
        {
        void configureForTopology(int[][] aaiOwner, boolean fDistribution)
            {
            setBackupCount(aaiOwner[0].length - 1);
            setPartitionCount(aaiOwner.length);
            setPartitionAssignments(aaiOwner);
            m_fDistribution = fDistribution;
            }

        boolean isTransferTopologyValidForTest(PartitionedService$TransferRequest request)
            {
            return isTransferRequestTopologyValid(request);
            }

        void onTransferRequestForTest(PartitionedService$TransferRequest request)
            {
            try
                {
                onTransferRequest(request);
                }
            catch (NullPointerException e)
                {
                // old-branch unit service is not fully started; materialization happens before transfer continuation
                if (isTransferRequestTopologyValid(request) && getEnsureStorageCount() > 0)
                    {
                    return;
                    }
                throw e;
                }
            }

        void setThisMemberForTest(Member member)
            {
            m_memberThis = member;
            }

        @Override
        public Member getThisMember()
            {
            return m_memberThis;
            }

        @Override
        public boolean isDistributionInProgress()
            {
            return m_fDistribution;
            }

        @Override
        public PartitionedCache$Storage ensureStorage(long lCacheId, boolean fCheckGraveyard)
            {
            m_cEnsureStorage++;
            m_storageLast = new TestStorage(this);
            m_storageLast.setCacheId(lCacheId);
            return m_storageLast;
            }

        @Override
        protected void rejectTransferRequest(PartitionedService$TransferRequest request)
            {
            m_cRejectedTransfer++;
            }

        int getEnsureStorageCount()
            {
            return m_cEnsureStorage;
            }

        int getLinkedDeferredEventCount()
            {
            return m_storageLast == null ? 0 : m_storageLast.getLinkedChildCount();
            }

        int getRejectedTransferCount()
            {
            return m_cRejectedTransfer;
            }

        PartitionedCache$Storage getLastStorage()
            {
            return m_storageLast;
            }

        private boolean m_fDistribution;
        private int     m_cEnsureStorage;
        private int     m_cRejectedTransfer;
        private Member  m_memberThis;
        private TestStorage m_storageLast;
        }

    public static class TestStorage
            extends PartitionedCache$Storage
        {
        TestStorage(PartitionedCache service)
            {
            super(null, service, false);
            }

        @Override
        public void _linkChild(Component child)
            {
            m_cLinkedChild++;
            super._linkChild(child);
            }

        @Override
        public ObservableMap getBackingMap()
            {
            return m_mapBacking;
            }

        int getLinkedChildCount()
            {
            return m_cLinkedChild;
            }

        private int m_cLinkedChild;

        private final ObservableMap m_mapBacking = new WrapperObservableMap(new HashMap());
        }
    }
