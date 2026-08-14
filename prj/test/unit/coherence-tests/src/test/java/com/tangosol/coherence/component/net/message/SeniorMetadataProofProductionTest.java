/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.component.net.message;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.MemberSet;
import com.tangosol.coherence.component.net.Message;
import com.tangosol.coherence.component.net.memberSet.SingleMemberSet;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService;
import com.tangosol.internal.net.security.SeniorMetadataProof;
import com.tangosol.internal.net.security.SeniorMetadataProofPayload;
import com.tangosol.internal.net.security.SeniorMetadataProofProvider;
import com.tangosol.internal.net.security.SeniorMetadataProofProviders;
import com.tangosol.internal.net.security.SeniorMetadataProofVerification;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;
import com.tangosol.util.Binary;
import com.tangosol.util.UUID;

import org.junit.Test;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for PEER-01 Slice D2 senior-metadata proof production plumbing.
 *
 * @author Aleks Seovic  2026.05.19
 * @since 26.07
 */
public class SeniorMetadataProofProductionTest
    {
    @Test
    public void shouldKeepDefaultProviderBehaviorNeutralForHeartbeat()
            throws Exception
        {
        ProductionClusterService service = new ProductionClusterService(false, true);
        ClusterService.SeniorMemberHeartbeat heartbeat = heartbeat(service, true, true, false);
        Binary binLegacy = writeLegacyHeartbeat(heartbeat);

        assertEquals(binLegacy, writeMessageBody(heartbeat));
        assertNull(heartbeat.getSeniorMetadataProof());
        }

    @Test
    public void shouldProduceStableHeartbeatProofForCompatibleRecipient()
            throws Exception
        {
        ProductionClusterService service = new ProductionClusterService(true, true);
        ClusterService.SeniorMemberHeartbeat heartbeatOne = heartbeat(service, true, true, false);
        ClusterService.SeniorMemberHeartbeat heartbeatTwo = heartbeat(service, true, true, false);

        Binary binOne = writeMessageBody(heartbeatOne);
        Binary binTwo = writeMessageBody(heartbeatTwo);

        assertArrayEquals(heartbeatOne.getSeniorMetadataProof(), heartbeatTwo.getSeniorMetadataProof());
        assertProofRecord(binOne, writeLegacyHeartbeat(heartbeatOne), heartbeatOne.getSeniorMetadataProof());
        assertProofRecord(binTwo, writeLegacyHeartbeat(heartbeatTwo), heartbeatTwo.getSeniorMetadataProof());
        assertVerified(service, heartbeatOne.getSeniorMetadataProof());
        assertEquals(SeniorMetadataProofPayload.MESSAGE_KIND_HEARTBEAT,
                SeniorMetadataProof.fromByteArray(heartbeatOne.getSeniorMetadataProof()).getPayload().getMessageKind());
        assertEquals(202L,
                SeniorMetadataProof.fromByteArray(heartbeatOne.getSeniorMetadataProof()).getPayload().getLastJoinTime());
        }

    @Test
    public void shouldProduceBroadcastHeartbeatProofAndSuppressIncompatibleAndMixedRecipients()
            throws Exception
        {
        ProductionClusterService service = new ProductionClusterService(true, true);
        ClusterService.SeniorMemberHeartbeat heartbeatBroadcast = heartbeat(service, false, true, false);
        Binary binBroadcast = writeLegacyHeartbeat(heartbeatBroadcast);
        Binary binProof = writeMessageBody(heartbeatBroadcast);
        assertProofRecord(binProof, binBroadcast, heartbeatBroadcast.getSeniorMetadataProof());
        assertVerified(service, heartbeatBroadcast.getSeniorMetadataProof());

        Binary binDirected = writeLegacyHeartbeat(heartbeat(service, true, false, false));
        assertEquals(binDirected, writeMessageBody(heartbeat(service, true, false, false)));

        Binary binMixed = writeLegacyHeartbeat(heartbeat(service, true, true, true));
        assertEquals(binMixed, writeMessageBody(heartbeat(service, true, true, true)));
        }

    @Test
    public void shouldProduceJuniorPanicReportProofThroughGridCarrier()
            throws Exception
        {
        ProductionClusterService service = new ProductionClusterService(true, false);
        ClusterService.SeniorMemberPanic panic = panic(service, true, false);
        Binary bin = serializeMessage(service, panic);
        Binary binLegacy = writeLegacyPanicEnvelope(service, panic);
        SeniorMetadataProof proof = SeniorMetadataProof.fromByteArray(panic.getSeniorMetadataProof());

        assertProofRecord(bin, binLegacy, panic.getSeniorMetadataProof());
        assertVerified(service, panic.getSeniorMetadataProof());
        assertEquals(SeniorMetadataProofPayload.MESSAGE_KIND_PANIC_REPORT, proof.getPayload().getMessageKind());
        assertEquals(memberId(member(2)), proof.getPayload().getTargetId());
        assertEquals(memberId(member(3)), proof.getPayload().getCulpritId());
        assertTrue(proof.getPayload().isZombie());
        }

    @Test
    public void shouldProduceSeniorPanicTokenProofThroughGridCarrier()
            throws Exception
        {
        ProductionClusterService service = new ProductionClusterService(true, true);
        ClusterService.SeniorMemberPanic panic = panic(service, true, false);
        serializeMessage(service, panic);

        assertEquals(SeniorMetadataProofPayload.MESSAGE_KIND_PANIC_TOKEN,
                SeniorMetadataProof.fromByteArray(panic.getSeniorMetadataProof()).getPayload().getMessageKind());
        assertEquals(memberId(member(2)),
                SeniorMetadataProof.fromByteArray(panic.getSeniorMetadataProof()).getPayload().getTargetId());
        }

    @Test
    public void shouldSuppressSeniorPanicTokenWithoutSingleTarget()
            throws Exception
        {
        ProductionClusterService service = new ProductionClusterService(true, true);
        ClusterService.SeniorMemberPanic panic = panic(service, true, true);

        assertEquals(writeLegacyPanicEnvelope(service, panic), serializeMessage(service, panic));
        assertNull(panic.getSeniorMetadataProof());
        }

    @Test
    public void shouldSuppressPanicProofForIncompatibleUnknownAndMixedRecipients()
            throws Exception
        {
        ProductionClusterService service = new ProductionClusterService(true, false);
        ClusterService.SeniorMemberPanic panic = panic(service, true, false);
        Binary binLegacy = writeLegacyPanicEnvelope(service, panic);

        assertEquals(binLegacy, serializeMessage(service, panic(service, false, false)));
        assertEquals(binLegacy, serializeMessage(service, panic(service, true, true)));
        assertEquals(binLegacy, serializeMessage(service, panic(service, true, false, false)));
        }

    @Test
    public void shouldProduceSeniorKillTokenProofForCompatibleRecipient()
            throws Exception
        {
        ProductionClusterService service = new ProductionClusterService(true, true);
        ClusterService.SeniorMemberKill kill = kill(service, true, false);
        Binary bin = writeMessageBody(kill);
        Binary binLegacy = writeLegacyKill(kill);
        SeniorMetadataProof proof = SeniorMetadataProof.fromByteArray(kill.getSeniorMetadataProof());

        assertProofRecord(bin, binLegacy, kill.getSeniorMetadataProof());
        assertVerified(service, kill.getSeniorMetadataProof());
        assertEquals(SeniorMetadataProofPayload.MESSAGE_KIND_KILL_TOKEN, proof.getPayload().getMessageKind());
        assertEquals(SeniorMetadataProofPayload.KILL_DIRECTION_SENIOR_TO_JUNIOR,
                proof.getPayload().getKillDirection());
        assertEquals(memberId(member(2)), proof.getPayload().getTargetId());
        }

    @Test
    public void shouldNotInventJuniorKillAuthorityProof()
            throws Exception
        {
        ProductionClusterService service = new ProductionClusterService(true, false);
        ClusterService.SeniorMemberKill kill = kill(service, true, false);

        assertEquals(writeLegacyKill(kill), writeMessageBody(kill));
        assertNull(kill.getSeniorMetadataProof());
        }

    @Test
    public void shouldPreserveExistingProofBytesInsteadOfReplacingThem()
            throws Exception
        {
        ProductionClusterService service = new ProductionClusterService(true, true);
        ClusterService.SeniorMemberKill kill = kill(service, true, false);
        kill.setSeniorMetadataProof(EXISTING_PROOF);

        Binary bin = writeMessageBody(kill);

        assertArrayEquals(EXISTING_PROOF, kill.getSeniorMetadataProof());
        assertEquals(0, service.getCreateCount());
        assertProofRecord(bin, writeLegacyKill(kill), EXISTING_PROOF);
        }

    @Test
    public void shouldUseDefaultFiveMinuteValidityWindow()
            throws Exception
        {
        DefaultExpiryClusterService service = new DefaultExpiryClusterService();
        ClusterService.SeniorMemberHeartbeat heartbeat = heartbeat(service, true, true, false);

        writeMessageBody(heartbeat);
        SeniorMetadataProofPayload payload =
                SeniorMetadataProof.fromByteArray(heartbeat.getSeniorMetadataProof()).getPayload();

        assertEquals(DEFAULT_VALIDITY_MILLIS, payload.getExpiresAtMillis() - payload.getIssuedAtMillis());
        assertEquals(SeniorMetadataProofVerification.Status.VALID,
                service.verifyAt(heartbeat.getSeniorMetadataProof(), payload, payload.getIssuedAtMillis() + 1).getStatus());
        assertEquals(SeniorMetadataProofVerification.Status.EXPIRED,
                service.verifyAt(heartbeat.getSeniorMetadataProof(), payload, payload.getExpiresAtMillis() + 1).getStatus());
        }

    private static void assertVerified(ProductionClusterService service, byte[] abProof)
            throws Exception
        {
        SeniorMetadataProof proof = SeniorMetadataProof.fromByteArray(abProof);
        SeniorMetadataProofVerification result = service.verify(abProof, proof.getPayload());

        assertEquals(SeniorMetadataProofVerification.Status.VALID, result.getStatus());
        assertTrue(result.isValid());
        assertNotNull(proof.getPayload());
        }

    private static void assertProofRecord(Binary bin, Binary binLegacy, byte[] abProof)
            throws Exception
        {
        assertTrue(bin.length() > binLegacy.length());
        assertArrayEquals(binLegacy.toByteArray(), Arrays.copyOf(bin.toByteArray(), binLegacy.length()));

        ReadBuffer.BufferInput input = bin.getBufferInput();
        input.skipBytes(binLegacy.length());
        assertEquals(SENIOR_METADATA_EXTENSION_MAGIC, input.readInt());
        assertEquals(SeniorMetadataProofCarrierSpikeTest.TestClusterService.PROOF_TYPE, input.readUnsignedByte());
        assertEquals(SeniorMetadataProofCarrierSpikeTest.TestClusterService.PROOF_VERSION, input.readUnsignedByte());
        int cbProof = input.readInt();
        assertEquals(abProof.length, cbProof);
        byte[] ab = new byte[cbProof];
        input.readFully(ab);
        assertArrayEquals(abProof, ab);
        assertEquals(0, input.available());
        }

    private static ClusterService.SeniorMemberHeartbeat heartbeat(SeniorMetadataProofCarrierSpikeTest.TestClusterService service,
            boolean fDirected, boolean fCompatible, boolean fMixed)
            throws Exception
        {
        service.setRecipientsCompatible(fCompatible);
        ClusterService.SeniorMemberHeartbeat heartbeat = new ClusterService.SeniorMemberHeartbeat();
        configureDiscovery(heartbeat, service, fDirected, fMixed);
        heartbeat.setLastReceivedMillis(101L);
        MemberSet setMember = new MemberSet();
        setMember.add(member(1));
        setMember.add(member(2));
        heartbeat.setMemberSet(setMember);
        heartbeat.setWkaEnabled(true);
        heartbeat.setLastJoinTime(202L);
        return heartbeat;
        }

    private static ClusterService.SeniorMemberKill kill(SeniorMetadataProofCarrierSpikeTest.TestClusterService service,
            boolean fDirected, boolean fMixed)
            throws Exception
        {
        ClusterService.SeniorMemberKill kill = new ClusterService.SeniorMemberKill();
        configureDiscovery(kill, service, fDirected, fMixed);
        return kill;
        }

    private static void configureDiscovery(DiscoveryMessage message,
            SeniorMetadataProofCarrierSpikeTest.TestClusterService service,
            boolean fDirected, boolean fMixed)
            throws Exception
        {
        message.setService(service);
        message.setFromMember(member(1));
        if (fDirected)
            {
            message.setToMember(member(2));
            if (fMixed)
                {
                MemberSet setMember = new MemberSet();
                setMember.add(member(2));
                setMember.add(member(4));
                message.setToMemberSet(setMember);
                service.setMemberCompatible(4, false);
                }
            else
                {
                message.setToMemberSet(SingleMemberSet.instantiate(member(2)));
                }
            }
        }

    private static ClusterService.SeniorMemberPanic panic(ProductionClusterService service, boolean fKnownRecipients,
            boolean fMixed)
            throws Exception
        {
        return panic(service, fKnownRecipients, true, fMixed);
        }

    private static ClusterService.SeniorMemberPanic panic(ProductionClusterService service, boolean fKnownRecipients,
            boolean fCompatible, boolean fMixed)
            throws Exception
        {
        service.setRecipientsCompatible(fCompatible);
        ClusterService.SeniorMemberPanic panic = new ClusterService.SeniorMemberPanic();
        panic.setService(service);
        panic.setFromMember(member(1));
        if (fKnownRecipients)
            {
            if (fMixed)
                {
                MemberSet setMember = new MemberSet();
                setMember.add(member(2));
                setMember.add(member(4));
                panic.setToMemberSet(setMember);
                service.setMemberCompatible(4, false);
                }
            else
                {
                panic.setToMemberSet(SingleMemberSet.instantiate(member(2)));
                }
            }
        panic.setZombie(true);
        panic.setCulpritMember(member(3));
        return panic;
        }

    private static Binary serializeMessage(ProductionClusterService service, Message message)
            throws Exception
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        service.serializeMessage(message, buffer.getBufferOutput());
        return buffer.toBinary();
        }

    private static Binary writeMessageBody(Message message)
            throws Exception
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        message.write(buffer.getBufferOutput());
        return buffer.toBinary();
        }

    private static Binary writeLegacyHeartbeat(ClusterService.SeniorMemberHeartbeat heartbeat)
            throws Exception
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput output = buffer.getBufferOutput();
        writeLegacyDiscovery(heartbeat, output);
        output.writeLong(heartbeat.getLastReceivedMillis());
        heartbeat.getMemberSet().writeExternal(output);
        output.writeBoolean(heartbeat.isWkaEnabled());
        output.writeLong(heartbeat.getLastJoinTime());
        return buffer.toBinary();
        }

    private static Binary writeLegacyKill(ClusterService.SeniorMemberKill kill)
            throws Exception
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        writeLegacyDiscovery(kill, buffer.getBufferOutput());
        return buffer.toBinary();
        }

    private static void writeLegacyDiscovery(DiscoveryMessage message, WriteBuffer.BufferOutput output)
            throws Exception
        {
        message.getFromMember().writeExternal(output);
        Member memberTo = message.getToMember();
        output.writeBoolean(memberTo != null);
        if (memberTo != null)
            {
            memberTo.writeExternal(output);
            }
        }

    private static Binary writeLegacyPanicEnvelope(ProductionClusterService service,
            ClusterService.SeniorMemberPanic panic)
            throws Exception
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput output = buffer.getBufferOutput();
        output.writeShort(service.getServiceId());
        output.writeShort(panic.getMessageType());
        output.writeBoolean(false);
        output.writeBoolean(panic.isZombie());
        panic.getCulpritMember().writeExternal(output);
        return buffer.toBinary();
        }

    private static Member member(int nId)
            throws Exception
        {
        Member member = new Member();
        member.configureDead(nId, new UUID(TIMESTAMP + nId, InetAddress.getByName("127.0.0.1"),
                7574 + nId, nId), TIMESTAMP + nId);
        return member;
        }

    private static String memberId(Member member)
        {
        return "member-uuid:" + member.getUuid();
        }

    public static class ProductionClusterService
            extends SeniorMetadataProofCarrierSpikeTest.TestClusterService
        {
        ProductionClusterService(boolean fProviderEnabled, boolean fSenior)
            {
            this(fProviderEnabled, fSenior ? 1 : 2, 1, fSenior);
            }

        ProductionClusterService(boolean fProviderEnabled, int nThisMemberId, int nSeniorMemberId,
                boolean fSenior)
            {
            f_fProviderEnabled = fProviderEnabled;
            f_fSenior = fSenior;
            f_memberSenior = memberUnchecked(nSeniorMemberId);
            f_memberThis = nThisMemberId == nSeniorMemberId ? f_memberSenior : memberUnchecked(nThisMemberId);
            }

        @Override
        protected SeniorMetadataProofProvider getSeniorMetadataProofProvider()
            {
            return f_fProviderEnabled ? f_provider : SeniorMetadataProofProviders.disabled();
            }

        @Override
        protected boolean isSeniorMetadataHeartbeatProductionAllowed(ClusterService.SeniorMemberHeartbeat msg)
            {
            return f_fSenior;
            }

        @Override
        protected boolean isSeniorMetadataKillProductionAllowed(ClusterService.SeniorMemberKill msg)
            {
            return f_fSenior;
            }

        @Override
        protected byte getSeniorMetadataPanicMessageKind(ClusterService.SeniorMemberPanic msg)
            {
            return f_fSenior
                    ? SeniorMetadataProofPayload.MESSAGE_KIND_PANIC_TOKEN
                    : SeniorMetadataProofPayload.MESSAGE_KIND_PANIC_REPORT;
            }

        @Override
        public Member getThisMember()
            {
            return f_memberThis;
            }

        @Override
        public Member getServiceOldestMember()
            {
            return f_memberSenior;
            }

        @Override
        protected String getSeniorMetadataProofMemberId(Member member)
            {
            return member == null || member.getUuid() == null ? "" : memberId(member);
            }

        @Override
        protected Member getSeniorMetadataSender(Message msg)
            {
            Member member = super.getSeniorMetadataSender(msg);
            return member != null && member.getId() == f_memberSenior.getId() ? f_memberSenior : member;
            }

        @Override
        protected String getSeniorMetadataProofAlgorithmId()
            {
            return ALGORITHM;
            }

        @Override
        protected String getSeniorMetadataProofKeyId()
            {
            return KEY_ID;
            }

        @Override
        protected String getSeniorMetadataProofClusterName()
            {
            return CLUSTER_NAME;
            }

        @Override
        protected String getSeniorMetadataProofServiceName()
            {
            return SERVICE_NAME;
            }

        @Override
        protected String getSeniorMetadataProofSeniorId(Message msg)
            {
            return memberId(msg.getFromMember());
            }

        @Override
        protected long getSeniorMetadataProofSeniorEpoch(Message msg)
            {
            return SENIOR_EPOCH;
            }

        @Override
        protected long getSeniorMetadataProofReplayEpoch(Message msg)
            {
            return REPLAY_EPOCH;
            }

        @Override
        protected long getSeniorMetadataProofNonce(Message msg)
            {
            return NONCE;
            }

        @Override
        protected long getSeniorMetadataProofIssuedAtMillis(Message msg)
            {
            return ISSUED_AT;
            }

        @Override
        protected long getSeniorMetadataProofExpiresAtMillis(Message msg, long lIssuedAtMillis)
            {
            return EXPIRES_AT;
            }

        SeniorMetadataProofVerification verify(byte[] abProof, SeniorMetadataProofPayload payload)
            {
            return f_provider.verifyProof(abProof, payload, NOW);
            }

        int getCreateCount()
            {
            return f_provider.getCreateCount();
            }

        private final boolean f_fProviderEnabled;
        private final boolean f_fSenior;
        private final Member  f_memberThis;
        private final Member  f_memberSenior;
        private final CountingProvider f_provider = new CountingProvider();
        }

    public static class DefaultExpiryClusterService
            extends SeniorMetadataProofCarrierSpikeTest.TestClusterService
        {
        DefaultExpiryClusterService()
            {
            f_memberSenior = memberUnchecked(1);
            }

        @Override
        protected SeniorMetadataProofProvider getSeniorMetadataProofProvider()
            {
            return f_provider;
            }

        @Override
        public Member getThisMember()
            {
            return f_memberSenior;
            }

        @Override
        public Member getServiceOldestMember()
            {
            return f_memberSenior;
            }

        @Override
        protected String getSeniorMetadataProofAlgorithmId()
            {
            return ALGORITHM;
            }

        @Override
        protected String getSeniorMetadataProofKeyId()
            {
            return KEY_ID;
            }

        @Override
        protected String getSeniorMetadataProofClusterName()
            {
            return CLUSTER_NAME;
            }

        @Override
        protected String getSeniorMetadataProofServiceName()
            {
            return SERVICE_NAME;
            }

        @Override
        protected Member getSeniorMetadataSender(Message msg)
            {
            Member member = super.getSeniorMetadataSender(msg);
            return member != null && member.getId() == f_memberSenior.getId() ? f_memberSenior : member;
            }

        SeniorMetadataProofVerification verifyAt(byte[] abProof, SeniorMetadataProofPayload payload, long ldtNow)
            {
            return f_provider.verifyProof(abProof, payload, ldtNow);
            }

        private final Member f_memberSenior;
        private final CountingProvider f_provider = new CountingProvider();
        }

    private static Member memberUnchecked(int nId)
        {
        try
            {
            return member(nId);
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    private static class CountingProvider
            implements SeniorMetadataProofProvider
        {
        @Override
        public byte[] createProof(SeniorMetadataProofPayload payload)
            {
            m_cCreate++;
            return f_delegate.createProof(payload);
            }

        @Override
        public SeniorMetadataProofVerification verifyProof(byte[] abProof, SeniorMetadataProofPayload expectedPayload,
                long lNowMillis)
            {
            return f_delegate.verifyProof(abProof, expectedPayload, lNowMillis);
            }

        @Override
        public boolean isSeniorMetadataProofAuthority(SeniorMetadataProofPayload payload)
            {
            return payload != null && memberId(memberUnchecked(1)).equals(payload.getSeniorId());
            }

        int getCreateCount()
            {
            return m_cCreate;
            }

        private int m_cCreate;
        private final SeniorMetadataProofProvider f_delegate =
                SeniorMetadataProofProviders.deterministicTestProvider(ALGORITHM, KEY_ID, SECRET);
        }

    private static final String ALGORITHM    = "test-hmac-sha256";
    private static final String KEY_ID       = "test-key";
    private static final String CLUSTER_NAME = "test-cluster";
    private static final String SERVICE_NAME = "Cluster";

    private static final long SENIOR_EPOCH = 11L;
    private static final long REPLAY_EPOCH = 22L;
    private static final long NONCE        = 33L;
    private static final long ISSUED_AT    = 1_000L;
    private static final long EXPIRES_AT   = 2_000L;
    private static final long NOW          = 1_500L;
    private static final long TIMESTAMP    = 123456789L;

    private static final int SENIOR_METADATA_EXTENSION_MAGIC = 0x534D5831;
    private static final long DEFAULT_VALIDITY_MILLIS = 5 * 60 * 1000L;

    private static final byte[] EXISTING_PROOF = new byte[] {9, 8, 7, 6};
    private static final byte[] SECRET         = "deterministic-secret".getBytes(StandardCharsets.UTF_8);
    }
