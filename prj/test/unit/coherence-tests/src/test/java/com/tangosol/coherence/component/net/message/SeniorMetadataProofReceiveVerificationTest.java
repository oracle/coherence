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
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService;
import com.tangosol.internal.net.security.SeniorMetadataProof;
import com.tangosol.internal.net.security.SeniorMetadataProofPayload;
import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.util.UUID;

import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for PEER-01 Slice D2 receive-side senior metadata proof checks.
 *
 * @author Aleks Seovic  2026.05.19
 * @since 15.1.2.0
 */
public class SeniorMetadataProofReceiveVerificationTest
    {
    @After
    public void cleanup()
            throws Exception
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, m_sOriginalMode);
        restoreProperty(PROP_SENIOR_METADATA_PROOF_REQUIRED, m_sOriginalRequired);
        resetMode();
        }

    @Test
    public void shouldAcceptValidProofsForProtectedSeniorMessagesInProd()
            throws Exception
        {
        setMode("prod");
        setProofRequired(true);

        ReceiveClusterService service = new ReceiveClusterService(true, false);

        assertTrue(service.verify(validHeartbeat(service)));
        assertTrue(service.verify(validKill(service)));
        assertTrue(service.verify(validPanic(service)));
        assertEquals(0, service.getWouldRejectCount());
        assertEquals(0, service.getDebugAllowCount());
        }

    @Test
    public void shouldAcceptSeniorPanicProofAfterRealGridDeserializePathInProd()
            throws Exception
        {
        setMode("prod");
        setProofRequired(true);

        ReceiveClusterService producer = new ReceiveClusterService(true, 1, 1, true);
        ReceiveClusterService receiver = new ReceiveClusterService(true, 2, 1, false);
        ClusterService.SeniorMemberPanic panic = deserializeSeniorPanic(producer, receiver,
                panic(producer, 1, 2, 3, false));

        assertNull(panic.getToMemberSet());
        assertTrue(receiver.verify(panic));
        }

    @Test
    public void shouldCarrySeniorPanicProofThroughDelegatedKillFlowInProd()
            throws Exception
        {
        setMode("prod");
        setProofRequired(true);

        ReceiveClusterService producer = new ReceiveClusterService(true, 1, 1, true);
        ReceiveClusterService junior   = new ReceiveClusterService(true, 2, 1, false);
        ClusterService.SeniorMemberPanic panic = deserializeSeniorPanic(producer, junior,
                panic(producer, 1, 2, 3, false));

        panic.onReceived();

        ClusterService.SeniorMemberKill kill = (ClusterService.SeniorMemberKill) junior.getLastSentMessage();
        assertNotNull(kill);
        assertArrayEquals(panic.getSeniorMetadataProof(), kill.getSeniorMetadataProof());

        ReceiveClusterService doomed = new ReceiveClusterService(true, 3, 1, false);
        kill.setService(doomed);
        kill.onReceived();

        assertEquals(1, doomed.getStopRunningCount());
        assertEquals(0, doomed.getWouldRejectCount());
        assertEquals(0, doomed.getDebugAllowCount());
        }

    @Test
    public void shouldCarryDelegatedProofThroughDoomedSeniorForwardingInProd()
            throws Exception
        {
        setMode("prod");
        setProofRequired(true);

        byte[] abProof = seniorPanicProof(1, 2, 3, false);

        ReceiveClusterService doomed = new ReceiveClusterService(true, 3, 3, true, 3, 4);
        ClusterService.SeniorMemberKill inbound = delegatedKill(doomed, abProof, 2, 3);
        inbound.onReceived();

        assertEquals(1, doomed.getStopRunningCount());
        assertEquals(1, doomed.getSentMessages().size());

        ClusterService.SeniorMemberKill forwarded =
                (ClusterService.SeniorMemberKill) doomed.getSentMessages().get(0);
        assertArrayEquals(abProof, forwarded.getSeniorMetadataProof());

        SeniorMetadataProofPayload payload = SeniorMetadataProof.fromByteArray(
                forwarded.getSeniorMetadataProof()).getPayload();
        assertEquals(SeniorMetadataProofPayload.MESSAGE_KIND_PANIC_TOKEN, payload.getMessageKind());
        assertEquals(memberId(1), payload.getIssuerId());
        assertEquals(memberId(1), payload.getSeniorId());
        assertEquals(memberId(1), payload.getSenderId());
        assertEquals(memberId(2), payload.getTargetId());
        assertEquals(memberId(3), payload.getCulpritId());

        ReceiveClusterService junior = new ReceiveClusterService(true, 4, 3, false, 2, 3, 4);
        forwarded.setService(junior);
        forwarded.onReceived();

        assertEquals(1, junior.getStopRunningCount());
        assertEquals(0, junior.getWouldRejectCount());
        assertEquals(0, junior.getDebugAllowCount());
        }

    @Test
    public void shouldAcceptForwardedFanOutProofScopedToDifferentKnownDelegateInProd()
            throws Exception
        {
        setMode("prod");
        setProofRequired(true);

        byte[] abProof = seniorPanicProof(1, 5, 3, false);

        ReceiveClusterService junior = new ReceiveClusterService(true, 4, 3, false, 2, 3, 4, 5);
        ClusterService.SeniorMemberKill forwarded = delegatedKill(junior, abProof, 3, 4);
        forwarded.onReceived();

        assertEquals(1, junior.getStopRunningCount());
        assertEquals(0, junior.getWouldRejectCount());
        assertEquals(0, junior.getDebugAllowCount());
        }

    @Test
    public void shouldRejectInvalidDelegatedKillProofsInProd()
            throws Exception
        {
        setMode("prod");
        setProofRequired(true);

        byte[] abValid = seniorPanicProof(1, 2, 3, false);

        assertDelegatedKillRejects(juniorPanicProof(), "payload_mismatch");
        assertDelegatedKillRejects(seniorPanicProof(1, 4, 3, false), "payload_mismatch");
        assertDelegatedKillRejects(seniorPanicProof(1, 2, 4, false), "payload_mismatch");
        ReceiveClusterService wrongTarget = new ReceiveClusterService(true, 3, 1, false);
        assertRejectsWith(wrongTarget, delegatedKill(wrongTarget, abValid, 2, 4), "payload_mismatch");

        ReceiveClusterService expired = new ReceiveClusterService(true, 3, 1, false);
        expired.setVerificationTimeMillis(Long.MAX_VALUE);
        assertRejectsWith(expired, delegatedKill(expired, abValid, 2, 3), "expired");

        assertDelegatedKillRejects(new byte[] {1, 2, 3}, "malformed");

        byte[] abRejected = Arrays.copyOf(abValid, abValid.length);
        abRejected[abRejected.length - 1] ^= 0x01;
        assertDelegatedKillRejects(abRejected, "proof_mismatch");
        }

    @Test
    public void shouldRejectInvalidForwardedDelegatedKillProofsInProd()
            throws Exception
        {
        setMode("prod");
        setProofRequired(true);

        ReceiveClusterService signer = new ReceiveClusterService(true, 1, 1, true);
        byte[] abValid = seniorPanicProof(1, 2, 3, false);

        assertForwardedKillRejects(rewritePanicProof(abValid, signer, memberId(5), memberId(1), memberId(1),
                memberId(2), memberId(3), SeniorMetadataProofPayload.MESSAGE_KIND_PANIC_TOKEN, 2_000L),
                3, 4, 4, "delegated");
        assertForwardedKillRejects(rewritePanicProof(abValid, signer, memberId(1), memberId(1), memberId(1),
                memberId(3), memberId(3), SeniorMetadataProofPayload.MESSAGE_KIND_PANIC_TOKEN, 2_000L),
                3, 4, 4, "delegated");
        assertForwardedKillRejects(rewritePanicProof(abValid, signer, memberId(1), memberId(1), memberId(1),
                memberId(5), memberId(3), SeniorMetadataProofPayload.MESSAGE_KIND_PANIC_TOKEN, 2_000L),
                3, 4, 4, "delegated");
        assertForwardedKillRejects(abValid, 5, 4, 4, "delegated");
        assertForwardedKillRejects(abValid, 3, 5, 4, "delegated");
        assertForwardedKillRejects(rewritePanicProof(abValid, signer, memberId(1), memberId(1), memberId(1),
                memberId(2), memberId(5), SeniorMetadataProofPayload.MESSAGE_KIND_PANIC_TOKEN, 2_000L),
                3, 4, 4, "delegated");
        assertForwardedKillRejects(juniorPanicProof(), 3, 4, 4, "payload_mismatch");

        ReceiveClusterService expired = new ReceiveClusterService(true, 4, 3, false, 2, 3, 4);
        expired.setVerificationTimeMillis(Long.MAX_VALUE);
        assertRejectsWith(expired, delegatedKill(expired, abValid, 3, 4), "expired");

        assertForwardedKillRejects(new byte[] {1, 2, 3}, 3, 4, 4, "malformed");
        assertForwardedKillRejects(null, 3, 4, 4, "missing");

        byte[] abRejected = Arrays.copyOf(abValid, abValid.length);
        abRejected[abRejected.length - 1] ^= 0x01;
        assertForwardedKillRejects(abRejected, 3, 4, 4, "proof_mismatch");
        }

    @Test
    public void shouldRejectSelfIssuedNonSeniorDelegatedKillProofInProd()
            throws Exception
        {
        setMode("prod");
        setProofRequired(true);

        ReceiveClusterService service = new ReceiveClusterService(true, 3, 1, false);
        ClusterService.SeniorMemberKill kill = delegatedKill(service, selfIssuedPanicToken(2, 2, 3), 2, 3);

        assertRejectsWith(service, kill, "payload_mismatch");
        assertEquals(0, service.getStopRunningCount());
        }

    @Test
    public void shouldRejectSelfIssuedNonSeniorForwardedFanOutProofInProd()
            throws Exception
        {
        setMode("prod");
        setProofRequired(true);

        ReceiveClusterService service = new ReceiveClusterService(true, 4, 3, false, 2, 3, 4, 5);
        ClusterService.SeniorMemberKill kill = delegatedKill(service, selfIssuedPanicToken(5, 2, 3), 3, 4);

        assertRejectsWith(service, kill, "payload_mismatch");
        assertEquals(0, service.getStopRunningCount());
        }

    @Test
    public void shouldAllowSelfIssuedNonSeniorDelegatedProofInLegacyDevAndDefaultFalse()
            throws Exception
        {
        byte[] abProof = selfIssuedPanicToken(2, 2, 3);

        setProofRequired(true);

        setMode("legacy");
        ReceiveClusterService legacy = new ReceiveClusterService(true, 3, 1, false);
        assertTrue(legacy.verify(delegatedKill(legacy, abProof, 2, 3)));
        assertEquals(1, legacy.getWouldRejectCount());
        assertTrue(legacy.getLastWouldRejectReason().contains("payload_mismatch"));

        setMode("dev");
        ReceiveClusterService dev = new ReceiveClusterService(true, 3, 1, false);
        assertTrue(dev.verify(delegatedKill(dev, abProof, 2, 3)));
        assertEquals(1, dev.getDebugAllowCount());
        assertTrue(dev.getLastDebugAllowReason().contains("payload_mismatch"));

        setMode("prod");
        setProofRequired(false);
        ReceiveClusterService neutral = new ReceiveClusterService(true, 3, 1, false);
        assertTrue(neutral.verify(delegatedKill(neutral, abProof, 2, 3)));
        assertEquals(0, neutral.getWouldRejectCount());
        assertEquals(0, neutral.getDebugAllowCount());
        }

    @Test
    public void shouldAllowInvalidDelegatedKillProofsInLegacyAndDev()
            throws Exception
        {
        byte[] abProof = seniorPanicProof(1, 4, 3, false);

        setProofRequired(true);

        setMode("legacy");
        ReceiveClusterService legacy = new ReceiveClusterService(true, 3, 1, false);
        assertTrue(legacy.verify(delegatedKill(legacy, abProof, 2, 3)));
        assertEquals(1, legacy.getWouldRejectCount());
        assertTrue(legacy.getLastWouldRejectReason().contains("payload_mismatch"));

        setMode("dev");
        ReceiveClusterService dev = new ReceiveClusterService(true, 3, 1, false);
        assertTrue(dev.verify(delegatedKill(dev, abProof, 2, 3)));
        assertEquals(1, dev.getDebugAllowCount());
        assertTrue(dev.getLastDebugAllowReason().contains("payload_mismatch"));
        }

    @Test
    public void shouldAllowInvalidForwardedDelegatedKillProofsInLegacyAndDev()
            throws Exception
        {
        byte[] abProof = seniorPanicProof(1, 2, 5, false);

        setProofRequired(true);

        setMode("legacy");
        ReceiveClusterService legacy = new ReceiveClusterService(true, 4, 3, false, 3, 4);
        assertTrue(legacy.verify(delegatedKill(legacy, abProof, 3, 4)));
        assertEquals(1, legacy.getWouldRejectCount());
        assertTrue(legacy.getLastWouldRejectReason().contains("payload_mismatch"));

        setMode("dev");
        ReceiveClusterService dev = new ReceiveClusterService(true, 4, 3, false, 3, 4);
        assertTrue(dev.verify(delegatedKill(dev, abProof, 3, 4)));
        assertEquals(1, dev.getDebugAllowCount());
        assertTrue(dev.getLastDebugAllowReason().contains("payload_mismatch"));
        }

    @Test
    public void shouldRejectMissingHeartbeatProofBeforeSeniorBroadcastValidation()
            throws Exception
        {
        setMode("prod");
        setProofRequired(true);

        ReceiveClusterService service   = new ReceiveClusterService(true, false);
        ClusterService.SeniorMemberHeartbeat heartbeat = heartbeat(service);

        SecurityException e = assertThrows(SecurityException.class, heartbeat::onReceived);

        assertTrue(e.getMessage().contains("missing"));
        assertEquals(0, service.getValidateSeniorBroadcastCount());
        }

    @Test
    public void shouldRejectMissingKillProofBeforeRunningStateCheck()
            throws Exception
        {
        setMode("prod");
        setProofRequired(true);

        ReceiveClusterService service = new ReceiveClusterService(true, false);
        ClusterService.SeniorMemberKill kill = kill(service);

        SecurityException e = assertThrows(SecurityException.class, kill::onReceived);

        assertTrue(e.getMessage().contains("missing"));
        assertEquals(0, service.getRunningCheckCount());
        }

    @Test
    public void shouldRejectMissingPanicProofBeforeRunningStateCheck()
            throws Exception
        {
        setMode("prod");
        setProofRequired(true);

        ReceiveClusterService service = new ReceiveClusterService(true, false);
        ClusterService.SeniorMemberPanic panic = panic(service);

        SecurityException e = assertThrows(SecurityException.class, panic::onReceived);

        assertTrue(e.getMessage().contains("missing"));
        assertEquals(0, service.getRunningCheckCount());
        }

    @Test
    public void shouldRejectMalformedExpiredMismatchedAndProviderRejectedProofsInProd()
            throws Exception
        {
        setMode("prod");
        setProofRequired(true);

        ReceiveClusterService service = new ReceiveClusterService(true, false);

        ClusterService.SeniorMemberHeartbeat malformed = heartbeat(service);
        malformed.setSeniorMetadataProof(new byte[] {1, 2, 3});
        assertRejectsWith(service, malformed, "malformed");

        ReceiveClusterService expiredService = new ReceiveClusterService(true, false);
        expiredService.setVerificationTimeMillis(Long.MAX_VALUE);
        assertRejectsWith(expiredService, validHeartbeat(expiredService), "expired");

        ClusterService.SeniorMemberHeartbeat mismatched = validHeartbeat(service);
        mismatched.setLastJoinTime(mismatched.getLastJoinTime() + 1);
        assertRejectsWith(service, mismatched, "payload_mismatch");

        ClusterService.SeniorMemberHeartbeat rejected = validHeartbeat(service);
        byte[] abProof = Arrays.copyOf(rejected.getSeniorMetadataProof(),
                rejected.getSeniorMetadataProof().length);
        abProof[abProof.length - 1] ^= 0x01;
        rejected.setSeniorMetadataProof(abProof);
        assertRejectsWith(service, rejected, "proof_mismatch");
        }

    @Test
    public void shouldRecordLegacyAndDevFailuresAndAllowCompatibilityBehavior()
            throws Exception
        {
        setProofRequired(true);

        setMode("legacy");
        ReceiveClusterService legacy = new ReceiveClusterService(true, false);
        assertTrue(legacy.verify(heartbeat(legacy)));
        assertEquals(1, legacy.getWouldRejectCount());
        assertTrue(legacy.getLastWouldRejectReason().contains("missing"));

        setMode("dev");
        ReceiveClusterService dev = new ReceiveClusterService(true, false);
        assertTrue(dev.verify(heartbeat(dev)));
        assertEquals(1, dev.getDebugAllowCount());
        assertTrue(dev.getLastDebugAllowReason().contains("missing"));
        }

    @Test
    public void shouldKeepDefaultFalseBehaviorNeutral()
            throws Exception
        {
        setMode("prod");
        setProofRequired(false);

        ReceiveClusterService service = new ReceiveClusterService(false, false);

        assertTrue(service.verify(heartbeat(service)));
        assertEquals(0, service.getWouldRejectCount());
        assertEquals(0, service.getDebugAllowCount());
        }

    @Test
    public void shouldLeaveNonSeniorMetadataMessagesOutsideD2Verification()
            throws Exception
        {
        setMode("prod");
        setProofRequired(true);

        ReceiveClusterService service = new ReceiveClusterService(true, false);

        assertTrue(service.verify(new Message()));
        }

    private static void assertRejectsWith(ReceiveClusterService service, Message msg, String sExpected)
        {
        SecurityException e = assertThrows(SecurityException.class, () -> service.verify(msg));

        assertTrue(e.getMessage().contains(sExpected));
        }

    private static void assertDelegatedKillRejects(byte[] abProof, String sExpected)
            throws Exception
        {
        ReceiveClusterService service = new ReceiveClusterService(true, 3, 1, false);
        assertRejectsWith(service, delegatedKill(service, abProof, 2, 3), sExpected);
        }

    private static void assertForwardedKillRejects(byte[] abProof, int nSender, int nTarget, int nThis,
            String sExpected)
            throws Exception
        {
        ReceiveClusterService service = new ReceiveClusterService(true, nThis, 3, false, 2, 3, 4);
        assertRejectsWith(service, delegatedKill(service, abProof, nSender, nTarget), sExpected);
        }

    private static ClusterService.SeniorMemberHeartbeat validHeartbeat(ReceiveClusterService service)
            throws Exception
        {
        ReceiveClusterService producer  = new ReceiveClusterService(true, true);
        ClusterService.SeniorMemberHeartbeat heartbeat = heartbeat(producer);
        writeBody(heartbeat);
        heartbeat.setService(service);
        return heartbeat;
        }

    private static ClusterService.SeniorMemberKill validKill(ReceiveClusterService service)
            throws Exception
        {
        ReceiveClusterService producer = new ReceiveClusterService(true, true);
        ClusterService.SeniorMemberKill kill = kill(producer);
        writeBody(kill);
        kill.setService(service);
        return kill;
        }

    private static ClusterService.SeniorMemberPanic validPanic(ReceiveClusterService service)
            throws Exception
        {
        ReceiveClusterService producer = new ReceiveClusterService(true, true);
        ClusterService.SeniorMemberPanic panic = panic(producer);
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        producer.serializeMessage(panic, buffer.getBufferOutput());
        panic.setService(service);
        return panic;
        }

    private static ClusterService.SeniorMemberPanic deserializeSeniorPanic(ReceiveClusterService producer,
            ReceiveClusterService receiver, ClusterService.SeniorMemberPanic panic)
            throws Exception
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        producer.serializeMessage(panic, buffer.getBufferOutput());

        ClusterService.SeniorMemberPanic result = new ClusterService.SeniorMemberPanic();
        result.setService(receiver);
        result.setFromMember(receiver.getServiceOldestMember());
        result.setReadBuffer(buffer.toBinary());
        result.setDeserializationRequired(true);

        assertTrue(receiver.deserializeMessage(result));
        return result;
        }

    private static ClusterService.SeniorMemberHeartbeat heartbeat(ReceiveClusterService service)
            throws Exception
        {
        ClusterService.SeniorMemberHeartbeat heartbeat = new ClusterService.SeniorMemberHeartbeat();
        heartbeat.setService(service);
        heartbeat.setFromMember(member(1));
        heartbeat.setToMember(member(2));
        heartbeat.setToMemberSet(SingleMemberSet.instantiate(member(2)));
        heartbeat.setLastReceivedMillis(101L);
        MemberSet setMember = new MemberSet();
        setMember.add(member(1));
        setMember.add(member(2));
        heartbeat.setMemberSet(setMember);
        heartbeat.setWkaEnabled(true);
        heartbeat.setLastJoinTime(202L);
        return heartbeat;
        }

    private static ClusterService.SeniorMemberKill kill(ReceiveClusterService service)
            throws Exception
        {
        ClusterService.SeniorMemberKill kill = new ClusterService.SeniorMemberKill();
        kill.setService(service);
        kill.setFromMember(member(1));
        kill.setToMember(member(2));
        kill.setToMemberSet(SingleMemberSet.instantiate(member(2)));
        return kill;
        }

    private static ClusterService.SeniorMemberPanic panic(ReceiveClusterService service)
            throws Exception
        {
        return panic(service, 1, 2, 3, true);
        }

    private static ClusterService.SeniorMemberPanic panic(ReceiveClusterService service, int nFrom, int nTarget,
            int nCulprit, boolean fZombie)
            throws Exception
        {
        ClusterService.SeniorMemberPanic panic = new ClusterService.SeniorMemberPanic();
        panic.setService(service);
        panic.setFromMember(service.getCanonicalMember(nFrom));
        panic.setToMemberSet(SingleMemberSet.instantiate(service.getCanonicalMember(nTarget)));
        panic.setZombie(fZombie);
        panic.setCulpritMember(service.getCanonicalMember(nCulprit));
        return panic;
        }

    private static byte[] seniorPanicProof(int nFrom, int nTarget, int nCulprit, boolean fZombie)
            throws Exception
        {
        ReceiveClusterService producer = new ReceiveClusterService(true, nFrom, 1, true);
        ClusterService.SeniorMemberPanic panic = panic(producer, nFrom, nTarget, nCulprit, fZombie);
        producer.serializeMessage(panic, new ByteArrayWriteBuffer(1024).getBufferOutput());
        assertEquals(SeniorMetadataProofPayload.MESSAGE_KIND_PANIC_TOKEN,
                SeniorMetadataProof.fromByteArray(panic.getSeniorMetadataProof()).getPayload().getMessageKind());
        return panic.getSeniorMetadataProof();
        }

    private static byte[] juniorPanicProof()
            throws Exception
        {
        ReceiveClusterService producer = new ReceiveClusterService(true, 2, 1, false);
        ClusterService.SeniorMemberPanic panic = panic(producer, 2, 1, 3, false);
        producer.serializeMessage(panic, new ByteArrayWriteBuffer(1024).getBufferOutput());
        assertEquals(SeniorMetadataProofPayload.MESSAGE_KIND_PANIC_REPORT,
                SeniorMetadataProof.fromByteArray(panic.getSeniorMetadataProof()).getPayload().getMessageKind());
        return panic.getSeniorMetadataProof();
        }

    private static byte[] selfIssuedPanicToken(int nSigner, int nDelegate, int nCulprit)
            throws Exception
        {
        byte[] abProof = seniorPanicProof(1, 2, 3, false);
        ReceiveClusterService signer = new ReceiveClusterService(true, nSigner, 1, false, 1, 2, 3, 4, 5);
        return rewritePanicProof(abProof, signer, memberId(nSigner), memberId(nSigner), memberId(nSigner),
                memberId(nDelegate), memberId(nCulprit), SeniorMetadataProofPayload.MESSAGE_KIND_PANIC_TOKEN,
                2_000L);
        }

    private static byte[] rewritePanicProof(byte[] abProof, ReceiveClusterService service, String sIssuer,
            String sSender, String sSenior, String sTarget, String sCulprit, byte nKind, long lExpires)
            throws Exception
        {
        SeniorMetadataProofPayload payload = SeniorMetadataProof.fromByteArray(abProof).getPayload();
        return service.createProof(new SeniorMetadataProofPayload(payload.getPayloadVersion(), nKind,
                payload.getAlgorithmId(), payload.getKeyId(), sIssuer, payload.getClusterName(),
                payload.getServiceName(), payload.getServiceId(), payload.getMessageType(), sSender, sSenior,
                sTarget, sCulprit, payload.getKillDirection(), payload.isZombie(), payload.getSeniorEpoch(),
                payload.getLastJoinTime(), payload.getMemberSetDigestVersion(), payload.getMemberSetCount(),
                payload.getMemberSetDigest(), payload.getReplayEpoch(), payload.getNonce(),
                payload.getIssuedAtMillis(), lExpires));
        }

    private static ClusterService.SeniorMemberKill delegatedKill(ReceiveClusterService service, byte[] abProof,
            int nSender, int nTarget)
            throws Exception
        {
        ClusterService.SeniorMemberKill kill = new ClusterService.SeniorMemberKill();
        kill.setService(service);
        kill.setFromMember(service.getCanonicalMember(nSender));
        kill.setToMember(service.getCanonicalMember(nTarget));
        kill.setSeniorMetadataProof(abProof);
        return kill;
        }

    private static void writeBody(Message msg)
            throws Exception
        {
        msg.write(new ByteArrayWriteBuffer(1024).getBufferOutput());
        }

    private static Member member(int nId)
            throws Exception
        {
        Member member = new Member();
        member.configureDead(nId, new UUID(TIMESTAMP + nId, InetAddress.getByName("127.0.0.1"),
                7574 + nId, nId), TIMESTAMP + nId);
        return member;
        }

    private static String memberId(int nId)
            throws Exception
        {
        return "member-uuid:" + member(nId).getUuid();
        }

    private static void setMode(String sMode)
            throws Exception
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, sMode);
        resetMode();
        }

    private static void setProofRequired(boolean fRequired)
        {
        System.setProperty(PROP_SENIOR_METADATA_PROOF_REQUIRED, Boolean.toString(fRequired));
        }

    private static void restoreProperty(String sProperty, String sValue)
        {
        if (sValue == null)
            {
            System.clearProperty(sProperty);
            }
        else
            {
            System.setProperty(sProperty, sValue);
            }
        }

    private static void resetMode()
            throws Exception
        {
        Method method = CoherenceMode.class.getDeclaredMethod("resetForTesting");
        method.setAccessible(true);
        method.invoke(null);
        }

    public static class ReceiveClusterService
            extends SeniorMetadataProofPolicyTest.PolicyClusterService
        {
        ReceiveClusterService(boolean fProviderEnabled, boolean fSenior)
            {
            this(fProviderEnabled, fSenior ? 1 : 2, 1, fSenior);
            }

        ReceiveClusterService(boolean fProviderEnabled, int nThisMemberId, int nSeniorMemberId, boolean fSenior)
            {
            this(fProviderEnabled, nThisMemberId, nSeniorMemberId, fSenior, 1, 2, 3, 4);
            }

        ReceiveClusterService(boolean fProviderEnabled, int nThisMemberId, int nSeniorMemberId, boolean fSenior,
                int... anMemberIds)
            {
            super(fProviderEnabled, nThisMemberId, nSeniorMemberId, fSenior);
            m_ldtVerification = 1_500L;
            f_anMemberIds = anMemberIds == null || anMemberIds.length == 0
                    ? new int[] {1, 2, 3, 4}
                    : Arrays.copyOf(anMemberIds, anMemberIds.length);
            f_memberOne   = selectCanonicalMember(1);
            f_memberTwo   = selectCanonicalMember(2);
            f_memberThree = selectCanonicalMember(3);
            f_memberFour  = selectCanonicalMember(4);
            f_setMembers   = createMemberSet();
            }

        boolean verify(Message msg)
            {
            return verifySeniorMetadataProofBeforeMutation(msg);
            }

        int getValidateSeniorBroadcastCount()
            {
            return m_cValidateSeniorBroadcast;
            }

        int getRunningCheckCount()
            {
            return m_cRunningCheck;
            }

        void setVerificationTimeMillis(long ldtVerification)
            {
            m_ldtVerification = ldtVerification;
            }

        Message getLastSentMessage()
            {
            return m_msgLastSent;
            }

        List<Message> getSentMessages()
            {
            return m_listSentMessages;
            }

        int getStopRunningCount()
            {
            return m_cStopRunning;
            }

        Member getCanonicalMember(int nMemberId)
            {
            switch (nMemberId)
                {
                case 1:
                    return f_memberOne;
                case 2:
                    return f_memberTwo;
                case 3:
                    return f_memberThree;
                case 4:
                    return f_memberFour;
                default:
                    return memberUnchecked(nMemberId);
                }
            }

        Member selectCanonicalMember(int nMemberId)
            {
            Member memberThis   = getThisMember();
            Member memberSenior = getServiceOldestMember();
            if (memberThis != null && memberThis.getId() == nMemberId)
                {
                return memberThis;
                }
            if (memberSenior != null && memberSenior.getId() == nMemberId)
                {
                return memberSenior;
                }
            return memberUnchecked(nMemberId);
            }

        @Override
        public boolean validateSeniorBroadcast(DiscoveryMessage message, MemberSet setFrom)
            {
            m_cValidateSeniorBroadcast++;
            return true;
            }

        @Override
        public boolean isRunning()
            {
            m_cRunningCheck++;
            return true;
            }

        @Override
        public int getState()
            {
            return STATE_JOINED;
            }

        @Override
        public MasterMemberSet getClusterMemberSet()
            {
            return f_setMembers == null ? super.getClusterMemberSet() : f_setMembers;
            }

        @Override
        public Message instantiateMessage(String sMsgName)
            {
            if ("SeniorMemberKill".equals(sMsgName))
                {
                ClusterService.SeniorMemberKill msg = new ClusterService.SeniorMemberKill();
                msg.setService(this);
                return msg;
                }
            return super.instantiateMessage(sMsgName);
            }

        @Override
        public void send(Message msg)
            {
            if (msg.getFromMember() == null)
                {
                msg.setFromMember(getThisMember());
                }
            if (msg instanceof DiscoveryMessage && ((DiscoveryMessage) msg).getToMember() != null
                    && msg.getToMemberSet() == null)
                {
                msg.setToMemberSet(SingleMemberSet.instantiate(((DiscoveryMessage) msg).getToMember()));
                }
            m_msgLastSent = msg;
            m_listSentMessages.add(msg);
            }

        @Override
        public void onStopRunning()
            {
            m_cStopRunning++;
            }

        @Override
        protected long getSeniorMetadataProofVerificationTimeMillis(Message msg,
                com.tangosol.internal.net.security.SeniorMetadataProofPayload payloadSigned)
            {
            return m_ldtVerification;
            }

        byte[] createProof(SeniorMetadataProofPayload payload)
            {
            return getSeniorMetadataProofProvider().createProof(payload);
            }

        private int  m_cValidateSeniorBroadcast;
        private int  m_cRunningCheck;
        private int  m_cStopRunning;
        private long m_ldtVerification;
        private Message m_msgLastSent;
        private final List<Message> m_listSentMessages = new ArrayList<>();
        private final Member f_memberOne;
        private final Member f_memberTwo;
        private final Member f_memberThree;
        private final Member f_memberFour;
        private final int[]  f_anMemberIds;
        private final MasterMemberSet f_setMembers;

        private MasterMemberSet createMemberSet()
            {
            MasterMemberSet setMember = new TestMasterMemberSet(getServiceOldestMember());
            for (int nMemberId : f_anMemberIds)
                {
                setMember.add(getCanonicalMember(nMemberId));
                }
            setMember.setThisMember(getThisMember());
            return setMember;
            }
        }

    private static class TestMasterMemberSet
            extends MasterMemberSet
        {
        TestMasterMemberSet(Member memberOldest)
            {
            f_memberOldest = memberOldest;
            }

        @Override
        public Member getOldestMember()
            {
            return f_memberOldest;
            }

        private final Member f_memberOldest;
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

    private static final String PROP_SENIOR_METADATA_PROOF_REQUIRED =
            "coherence.security.peer.senior-metadata-proof.required";

    private static final long TIMESTAMP = 123456789L;

    private final String m_sOriginalMode     = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
    private final String m_sOriginalRequired = System.getProperty(PROP_SENIOR_METADATA_PROOF_REQUIRED);
    }
