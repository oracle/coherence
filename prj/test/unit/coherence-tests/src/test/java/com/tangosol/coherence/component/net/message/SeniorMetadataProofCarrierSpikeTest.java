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
import com.tangosol.internal.util.VersionHelper;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.UUID;

import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.function.IntPredicate;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Byte-compatibility spike for the PEER-01 Slice D2 senior-metadata carrier.
 *
 * @author OpenAI  2026.05.19
 *
 * @since 15.1.2.0
 */
public class SeniorMetadataProofCarrierSpikeTest
    {
    @Test
    public void shouldEvaluateSeniorMetadataProofV1CompatibilityPredicate()
        {
        assertTrue(Message.isSeniorMetadataProofV1Compatible(VersionHelper.encodeVersion(15, 1, 2, 0, 0)));
        assertTrue(Message.isSeniorMetadataProofV1Compatible(VersionHelper.encodeVersion(15, 1, 1, 0, 4)));
        assertTrue(Message.isSeniorMetadataProofV1Compatible(VersionHelper.encodeVersion(14, 1, 2, 0, 8)));
        assertTrue(Message.isSeniorMetadataProofV1Compatible(VersionHelper.encodeVersion(14, 1, 1, 2206, 18)));
        assertTrue(Message.isSeniorMetadataProofV1Compatible(VersionHelper.encodeVersion(14, 1, 1, 0, 27)));
        assertTrue(Message.isSeniorMetadataProofV1Compatible(VersionHelper.encodeVersion(12, 2, 1, 4, 31)));

        assertFalse(Message.isSeniorMetadataProofV1Compatible(VersionHelper.encodeVersion(15, 1, 1, 0, 3)));
        assertFalse(Message.isSeniorMetadataProofV1Compatible(VersionHelper.encodeVersion(14, 1, 2, 0, 7)));
        assertFalse(Message.isSeniorMetadataProofV1Compatible(VersionHelper.encodeVersion(14, 1, 1, 2206, 17)));
        assertFalse(Message.isSeniorMetadataProofV1Compatible(VersionHelper.encodeVersion(14, 1, 1, 0, 26)));
        assertFalse(Message.isSeniorMetadataProofV1Compatible(VersionHelper.encodeVersion(12, 2, 1, 4, 30)));
        }

    @Test
    public void shouldPreserveLegacyHeartbeatBytesWhenNoProofIsEmitted()
            throws Exception
        {
        TestClusterService service   = new TestClusterService();
        Binary             binLegacy = writeLegacyHeartbeat(heartbeat(service, false, true, true));
        Binary             binNoProof = writeHeartbeat(service, false, true, true);

        assertEquals(binLegacy, binNoProof);
        }

    @Test
    public void shouldWriteHeartbeatProofForCompatibleBroadcastButNotIncompatibleRecipients()
            throws Exception
        {
        TestClusterService service      = new TestClusterService();
        Binary             binBroadcast = writeLegacyHeartbeat(heartbeat(service, true, false, true));
        Binary             binDirected  = writeLegacyHeartbeat(heartbeat(service, true, true, false));

        assertNotEquals(binBroadcast, writeHeartbeat(service, true, false, true));
        assertEquals(binDirected, writeHeartbeat(service, true, true, false));
        }

    @Test
    public void shouldPreserveLegacyHeartbeatBytesForUnknownOrMixedRecipients()
            throws Exception
        {
        TestClusterService service      = new TestClusterService();
        ClusterService.SeniorMemberHeartbeat heartbeatUnknown = heartbeat(service, true, false, true);
        heartbeatUnknown.setMemberSet(new MemberSet());
        Binary             binBroadcast = writeLegacyHeartbeat(heartbeatUnknown);
        Binary             binDirected  = writeLegacyHeartbeat(heartbeat(service, true, true, true, true));

        assertEquals(binBroadcast, writeMessageBody(heartbeatUnknown));
        assertEquals(binDirected, writeHeartbeat(service, true, true, true, true));
        }

    @Test
    public void shouldReadHeartbeatExtensionBeforeEnsureEos()
            throws Exception
        {
        TestClusterService service = new TestClusterService();
        Binary             bin     = writeHeartbeat(service, true, true, true);

        ClusterService.SeniorMemberHeartbeat result = heartbeat(service, false, true, true);
        result.read(bin.getBufferInput());

        assertArrayEquals(PROOF, result.getSeniorMetadataProof());
        }

    @Test
    public void shouldPreserveLegacyKillBytesForBroadcastOrIncompatibleRecipients()
            throws Exception
        {
        TestClusterService service      = new TestClusterService();
        Binary             binBroadcast = writeLegacyKill(kill(service, true, false, true));
        Binary             binDirected  = writeLegacyKill(kill(service, false, true, true));

        assertEquals(binDirected, writeKill(service, false, true, true));
        assertEquals(binBroadcast, writeKill(service, true, false, true));
        assertEquals(binDirected, writeKill(service, true, true, false));
        }

    @Test
    public void shouldPreserveLegacyKillBytesForUnknownOrMixedRecipients()
            throws Exception
        {
        TestClusterService service      = new TestClusterService();
        Binary             binBroadcast = writeLegacyKill(kill(service, true, false, true));
        Binary             binDirected  = writeLegacyKill(kill(service, true, true, true, true));

        assertEquals(binBroadcast, writeKill(service, true, false, true));
        assertEquals(binDirected, writeKill(service, true, true, true, true));
        }

    @Test
    public void shouldRoundTripKillExtensionForCompatibleDirectedRecipient()
            throws Exception
        {
        TestClusterService service = new TestClusterService();
        Binary             bin     = writeKill(service, true, true, true);

        ClusterService.SeniorMemberKill result = kill(service, false, true, true);
        result.read(bin.getBufferInput());

        assertArrayEquals(PROOF, result.getSeniorMetadataProof());
        }

    @Test
    public void shouldPreserveLegacyPanicBytesForNoProofOrIncompatibleRecipients()
            throws Exception
        {
        TestClusterService service   = new TestClusterService();
        Binary             binLegacy = writeLegacyPanicBody(panic(service, false, true));

        assertEquals(binLegacy, writePanic(service, false, true));
        assertEquals(binLegacy, writePanic(service, true, false));
        }

    @Test
    public void shouldPreserveLegacyPanicEnvelopeForNoProofOrSuppressedRecipients()
            throws Exception
        {
        TestClusterService service   = new TestClusterService();
        Binary             binLegacy = writeLegacyPanicEnvelope(service, panic(service, false, true));

        assertEquals(binLegacy, serializeMessage(service, panic(service, false, true)));
        assertEquals(binLegacy, serializeMessage(service, panic(service, true, false)));
        assertEquals(binLegacy, serializeMessage(service, panic(service, true, true, false, false)));
        assertEquals(binLegacy, serializeMessage(service, panic(service, true, true, true, true)));
        }

    @Test
    public void shouldPreserveLegacyPanicBytesForUnknownOrMixedRecipients()
            throws Exception
        {
        TestClusterService service   = new TestClusterService();
        Binary             binLegacy = writeLegacyPanicBody(panic(service, false, true));

        assertEquals(binLegacy, writePanic(service, true, true, false, false));
        assertEquals(binLegacy, writePanic(service, true, true, true, true));
        }

    @Test
    public void shouldRoundTripPanicExtensionForCompatibleRecipient()
            throws Exception
        {
        TestClusterService service = new TestClusterService();
        Binary             bin     = writePanic(service, true, true);

        ClusterService.SeniorMemberPanic result = incomingPanic(service);
        ReadBuffer.BufferInput           input  = bin.getBufferInput();
        result.read(input);
        service.readSeniorMetadataExtensions(result, input);

        assertEquals(0, input.available());
        assertArrayEquals(PROOF, result.getSeniorMetadataProof());
        assertTrue(result.isZombie());
        }

    @Test
    public void shouldRoundTripPanicExtensionThroughGridSerializeDeserialize()
            throws Exception
        {
        TestClusterService              service = new TestClusterService();
        ClusterService.SeniorMemberPanic panic   = panic(service, true, true);
        Binary                          bin      = serializeMessage(service, panic);
        Binary                          binLegacy = writeLegacyPanicEnvelope(service, panic);
        ReadBuffer.BufferInput          input    = bin.getBufferInput();

        assertEquals(service.getServiceId(), input.readUnsignedShort());
        assertEquals(panic.getMessageType(), input.readUnsignedShort());
        assertTrue(bin.length() > binLegacy.length());
        assertArrayEquals(binLegacy.toByteArray(), Arrays.copyOf(bin.toByteArray(), binLegacy.length()));

        ClusterService.SeniorMemberPanic result = incomingPanic(service);
        result.setReadBuffer(bin);
        result.setDeserializationRequired(true);

        service.expectRequestExtensionsBeforeSeniorMetadata();
        assertTrue(service.deserializeMessage(result));
        assertTrue(service.wasRequestExtensionsRead());
        assertTrue(service.wasSeniorMetadataRead());
        assertArrayEquals(PROOF, result.getSeniorMetadataProof());
        assertTrue(result.isZombie());
        assertEquals(member(3).getUid32(), result.getCulpritMember().getUid32());
        }

    @Test
    public void shouldParseMultipleRecordsAndSkipUnknownRecord()
            throws Exception
        {
        TestClusterService             service = new TestClusterService();
        ClusterService.SeniorMemberPanic result = readPanicWithExtensions(service,
                new Extension(99, 1, UNKNOWN),
                new Extension(TestClusterService.PROOF_TYPE, TestClusterService.PROOF_VERSION, PROOF));

        assertArrayEquals(PROOF, result.getSeniorMetadataProof());
        }

    @Test
    public void shouldSkipLargeUnknownWellFormedRecordWithoutCopyingPayload()
            throws Exception
        {
        TestClusterService service = new TestClusterService();
        ReadBuffer.BufferInput input = inputWithLargeUnknownExtension(service);
        ClusterService.SeniorMemberPanic result = incomingPanic(service);
        result.read(input);
        service.readSeniorMetadataExtensions(result, input);

        assertArrayEquals(PROOF, result.getSeniorMetadataProof());
        }

    @Test
    public void shouldRejectOversizedKnownProofRecordBeforePayloadAllocation()
            throws Exception
        {
        TestClusterService service = new TestClusterService();
        ReadBuffer.BufferInput input = inputWithOversizedHeader(service, TestClusterService.PROOF_TYPE,
                TestClusterService.PROOF_VERSION);
        ClusterService.SeniorMemberPanic result = incomingPanic(service);
        result.read(input);

        assertFailure(service, result, input, "malformed senior-metadata extension");
        }

    @Test
    public void shouldRejectOversizedUnknownRecordBeforePayloadAllocation()
            throws Exception
        {
        TestClusterService service = new TestClusterService();
        ReadBuffer.BufferInput input = inputWithOversizedHeader(service, 99, TestClusterService.PROOF_VERSION);
        ClusterService.SeniorMemberPanic result = incomingPanic(service);
        result.read(input);

        assertFailure(service, result, input, "malformed senior-metadata extension");
        }

    @Test
    public void shouldRejectOversizedPayloadBeforeWritingRecord()
            throws Exception
        {
        TestClusterService  service = new TestClusterService();
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);

        try
            {
            service.writeSeniorMetadataExtension(buffer.getBufferOutput(), TestClusterService.PROOF_TYPE,
                    TestClusterService.PROOF_VERSION, new byte[TestClusterService.MAX_PAYLOAD + 1]);
            fail("expected oversized senior-metadata extension");
            }
        catch (IOException e)
            {
            assertTrue(e.getMessage().contains("oversized senior-metadata extension"));
            }

        assertEquals(0, buffer.toBinary().length());
        }

    @Test
    public void shouldRejectUnsupportedKnownProofVersion()
            throws Exception
        {
        TestClusterService service = new TestClusterService();
        ReadBuffer.BufferInput input = inputWithExtensions(service,
                new Extension(TestClusterService.PROOF_TYPE, TestClusterService.PROOF_VERSION + 1, PROOF));
        ClusterService.SeniorMemberPanic result = incomingPanic(service);
        result.read(input);

        assertFailure(service, result, input, "unsupported senior-metadata proof extension");
        }

    @Test
    public void shouldRejectMalformedRecordAfterPositiveMagic()
            throws Exception
        {
        TestClusterService service = new TestClusterService();
        ReadBuffer.BufferInput input = inputWithMalformedLength(service);
        ClusterService.SeniorMemberPanic result = incomingPanic(service);
        result.read(input);

        assertFailure(service, result, input, "malformed senior-metadata extension");
        }

    @Test
    public void shouldRejectNegativePayloadLengthAfterPositiveMagic()
            throws Exception
        {
        TestClusterService service = new TestClusterService();
        ReadBuffer.BufferInput input = inputWithNegativeLength(service);
        ClusterService.SeniorMemberPanic result = incomingPanic(service);
        result.read(input);

        assertFailure(service, result, input, "malformed senior-metadata extension");
        }

    @Test
    public void shouldRejectTruncatedUnknownRecordAfterPositiveMagic()
            throws Exception
        {
        TestClusterService service = new TestClusterService();
        ReadBuffer.BufferInput input = inputWithTruncatedUnknownRecord(service);
        ClusterService.SeniorMemberPanic result = incomingPanic(service);
        result.read(input);

        assertFailure(service, result, input, "malformed senior-metadata extension");
        }

    @Test
    public void shouldRejectStrayBytesAfterPositiveMagic()
            throws Exception
        {
        TestClusterService service = new TestClusterService();
        ReadBuffer.BufferInput input = inputWithStrayBytes(service);
        ClusterService.SeniorMemberPanic result = incomingPanic(service);
        result.read(input);

        assertFailure(service, result, input, "stray senior-metadata extension bytes");
        }

    @Test
    public void shouldLeaveNonSeniorMessagesUntouched()
            throws Exception
        {
        TestClusterService service = new TestClusterService();
        Message            message = new Message();
        message.setSeniorMetadataProof(PROOF);
        message.setToMemberSet(SingleMemberSet.instantiate(member(2)));

        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        service.writeSeniorMetadataExtensions(message, buffer.getBufferOutput());

        assertEquals(0, buffer.toBinary().length());
        }

    private static ClusterService.SeniorMemberPanic readPanicWithExtensions(TestClusterService service,
            Extension... aExtension)
            throws Exception
        {
        ReadBuffer.BufferInput input = inputWithExtensions(service, aExtension);
        ClusterService.SeniorMemberPanic result = incomingPanic(service);
        result.read(input);
        service.readSeniorMetadataExtensions(result, input);
        return result;
        }

    private static ReadBuffer.BufferInput inputWithExtensions(TestClusterService service, Extension... aExtension)
            throws Exception
        {
        ClusterService.SeniorMemberPanic request = panic(service, false, true);

        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput output = buffer.getBufferOutput();
        request.write(output);
        for (Extension extension : aExtension)
            {
            service.writeSeniorMetadataExtension(output, extension.m_nType, extension.m_nVersion,
                    extension.m_abPayload);
            }
        return buffer.toBinary().getBufferInput();
        }

    private static ReadBuffer.BufferInput inputWithLargeUnknownExtension(TestClusterService service)
            throws Exception
        {
        ClusterService.SeniorMemberPanic request = panic(service, false, true);

        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(TestClusterService.MAX_PAYLOAD + 1024);
        WriteBuffer.BufferOutput output = buffer.getBufferOutput();
        request.write(output);
        service.writeSeniorMetadataExtensionHeader(output, 99, 1, TestClusterService.MAX_PAYLOAD);
        for (int i = 0; i < TestClusterService.MAX_PAYLOAD; i++)
            {
            output.writeByte(0);
            }
        service.writeSeniorMetadataExtension(output, TestClusterService.PROOF_TYPE,
                TestClusterService.PROOF_VERSION, PROOF);
        return buffer.toBinary().getBufferInput();
        }

    private static ReadBuffer.BufferInput inputWithOversizedHeader(TestClusterService service, int nType,
            int nVersion)
            throws Exception
        {
        ClusterService.SeniorMemberPanic request = panic(service, false, true);

        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput output = buffer.getBufferOutput();
        request.write(output);
        service.writeSeniorMetadataExtensionHeader(output, nType, nVersion, TestClusterService.MAX_PAYLOAD + 1);
        return buffer.toBinary().getBufferInput();
        }

    private static ReadBuffer.BufferInput inputWithMalformedLength(TestClusterService service)
            throws Exception
        {
        ClusterService.SeniorMemberPanic request = panic(service, false, true);

        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput output = buffer.getBufferOutput();
        request.write(output);
        service.writeSeniorMetadataMalformedExtension(output);
        return buffer.toBinary().getBufferInput();
        }

    private static ReadBuffer.BufferInput inputWithNegativeLength(TestClusterService service)
            throws Exception
        {
        ClusterService.SeniorMemberPanic request = panic(service, false, true);

        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput output = buffer.getBufferOutput();
        request.write(output);
        service.writeSeniorMetadataExtensionHeader(output, TestClusterService.PROOF_TYPE,
                TestClusterService.PROOF_VERSION, -1);
        return buffer.toBinary().getBufferInput();
        }

    private static ReadBuffer.BufferInput inputWithTruncatedUnknownRecord(TestClusterService service)
            throws Exception
        {
        ClusterService.SeniorMemberPanic request = panic(service, false, true);

        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput output = buffer.getBufferOutput();
        request.write(output);
        service.writeSeniorMetadataExtensionHeader(output, 99, 1, UNKNOWN.length + 1);
        output.write(UNKNOWN);
        return buffer.toBinary().getBufferInput();
        }

    private static ReadBuffer.BufferInput inputWithStrayBytes(TestClusterService service)
            throws Exception
        {
        ClusterService.SeniorMemberPanic request = panic(service, false, true);

        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput output = buffer.getBufferOutput();
        request.write(output);
        service.writeSeniorMetadataExtension(output, 99, 1, UNKNOWN);
        output.writeByte(1);
        return buffer.toBinary().getBufferInput();
        }

    private static void assertFailure(TestClusterService service, ClusterService.SeniorMemberPanic result,
            ReadBuffer.BufferInput input, String sMessage)
            throws Exception
        {
        try
            {
            service.readSeniorMetadataExtensions(result, input);
            fail("expected " + sMessage);
            }
        catch (IOException e)
            {
            assertTrue(e.getMessage().contains(sMessage));
            }
        }

    private static Binary writeHeartbeat(TestClusterService service, boolean fProof, boolean fDirected,
            boolean fCompatible)
            throws Exception
        {
        return writeHeartbeat(service, fProof, fDirected, fCompatible, false);
        }

    private static Binary writeHeartbeat(TestClusterService service, boolean fProof, boolean fDirected,
            boolean fCompatible, boolean fMixed)
            throws Exception
        {
        service.setRecipientsCompatible(fCompatible);
        ClusterService.SeniorMemberHeartbeat heartbeat = heartbeat(service, fProof, fDirected, fCompatible, fMixed);
        return writeMessageBody(heartbeat);
        }

    private static ClusterService.SeniorMemberHeartbeat heartbeat(TestClusterService service, boolean fProof,
            boolean fDirected, boolean fCompatible)
            throws Exception
        {
        return heartbeat(service, fProof, fDirected, fCompatible, false);
        }

    private static ClusterService.SeniorMemberHeartbeat heartbeat(TestClusterService service, boolean fProof,
            boolean fDirected, boolean fCompatible, boolean fMixed)
            throws Exception
        {
        ClusterService.SeniorMemberHeartbeat heartbeat = new ClusterService.SeniorMemberHeartbeat();
        configureDiscovery(heartbeat, service, fProof, fDirected, fCompatible, fMixed);
        heartbeat.setLastReceivedMillis(101L);
        MemberSet setMember = new MemberSet();
        setMember.add(member(1));
        heartbeat.setMemberSet(setMember);
        heartbeat.setWkaEnabled(true);
        heartbeat.setLastJoinTime(202L);
        return heartbeat;
        }

    private static Binary writeKill(TestClusterService service, boolean fProof, boolean fDirected,
            boolean fCompatible)
            throws Exception
        {
        return writeKill(service, fProof, fDirected, fCompatible, false);
        }

    private static Binary writeKill(TestClusterService service, boolean fProof, boolean fDirected,
            boolean fCompatible, boolean fMixed)
            throws Exception
        {
        service.setRecipientsCompatible(fCompatible);
        ClusterService.SeniorMemberKill kill = kill(service, fProof, fDirected, fCompatible, fMixed);
        return writeMessageBody(kill);
        }

    private static ClusterService.SeniorMemberKill kill(TestClusterService service, boolean fProof,
            boolean fDirected, boolean fCompatible)
            throws Exception
        {
        return kill(service, fProof, fDirected, fCompatible, false);
        }

    private static ClusterService.SeniorMemberKill kill(TestClusterService service, boolean fProof,
            boolean fDirected, boolean fCompatible, boolean fMixed)
            throws Exception
        {
        ClusterService.SeniorMemberKill kill = new ClusterService.SeniorMemberKill();
        configureDiscovery(kill, service, fProof, fDirected, fCompatible, fMixed);
        return kill;
        }

    private static void configureDiscovery(DiscoveryMessage message, TestClusterService service, boolean fProof,
            boolean fDirected, boolean fCompatible)
            throws Exception
        {
        configureDiscovery(message, service, fProof, fDirected, fCompatible, false);
        }

    private static void configureDiscovery(DiscoveryMessage message, TestClusterService service, boolean fProof,
            boolean fDirected, boolean fCompatible, boolean fMixed)
            throws Exception
        {
        service.setRecipientsCompatible(fCompatible);
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
        if (fProof)
            {
            message.setSeniorMetadataProof(PROOF);
            }
        }

    private static Binary writePanic(TestClusterService service, boolean fProof, boolean fCompatible)
            throws Exception
        {
        return writePanic(service, fProof, fCompatible, true, false);
        }

    private static Binary writePanic(TestClusterService service, boolean fProof, boolean fCompatible,
            boolean fKnownRecipients, boolean fMixed)
            throws Exception
        {
        service.setRecipientsCompatible(fCompatible);
        ClusterService.SeniorMemberPanic panic = panic(service, fProof, fCompatible, fKnownRecipients, fMixed);
        ByteArrayWriteBuffer             buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput         output = buffer.getBufferOutput();
        panic.write(output);
        service.writeSeniorMetadataExtensions(panic, output);
        return buffer.toBinary();
        }

    private static ClusterService.SeniorMemberPanic panic(TestClusterService service, boolean fProof,
            boolean fCompatible)
            throws Exception
        {
        return panic(service, fProof, fCompatible, true, false);
        }

    private static ClusterService.SeniorMemberPanic panic(TestClusterService service, boolean fProof,
            boolean fCompatible, boolean fKnownRecipients, boolean fMixed)
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
        if (fProof)
            {
            panic.setSeniorMetadataProof(PROOF);
            }
        return panic;
        }

    private static Binary serializeMessage(TestClusterService service, Message message)
            throws Exception
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        service.serializeMessage(message, buffer.getBufferOutput());
        return buffer.toBinary();
        }

    private static ClusterService.SeniorMemberPanic incomingPanic(TestClusterService service)
            throws Exception
        {
        ClusterService.SeniorMemberPanic panic = new ClusterService.SeniorMemberPanic();
        panic.setService(service);
        panic.setFromMember(member(1));
        return panic;
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

    private static Binary writeLegacyPanicEnvelope(TestClusterService service, ClusterService.SeniorMemberPanic panic)
            throws Exception
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput output = buffer.getBufferOutput();
        output.writeShort(service.getServiceId());
        output.writeShort(panic.getMessageType());
        output.writeBoolean(false);
        writeLegacyPanicBody(panic, output);
        return buffer.toBinary();
        }

    private static Binary writeLegacyPanicBody(ClusterService.SeniorMemberPanic panic)
            throws Exception
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        writeLegacyPanicBody(panic, buffer.getBufferOutput());
        return buffer.toBinary();
        }

    private static void writeLegacyPanicBody(ClusterService.SeniorMemberPanic panic, WriteBuffer.BufferOutput output)
            throws Exception
        {
        output.writeBoolean(panic.isZombie());
        panic.getCulpritMember().writeExternal(output);
        }

    private static Member member(int nId)
            throws Exception
        {
        Member member = new Member();
        member.configureDead(nId, new UUID(TIMESTAMP + nId, InetAddress.getByName("127.0.0.1"),
                7574 + nId, nId), TIMESTAMP + nId);
        return member;
        }

    private static class Extension
        {
        Extension(int nType, int nVersion, byte[] abPayload)
            {
            m_nType = nType;
            m_nVersion = nVersion;
            m_abPayload = abPayload;
            }

        private final int    m_nType;
        private final int    m_nVersion;
        private final byte[] m_abPayload;
        }

    public static class TestClusterService
            extends ClusterService
        {
        public TestClusterService()
            {
            setSerializer(ExternalizableHelper.ensureSerializer(null));
            }

        public void writeSeniorMetadataExtension(WriteBuffer.BufferOutput output, int nType, int nVersion,
                byte[] abPayload)
                throws IOException
            {
            super.writeSeniorMetadataExtension(output, nType, nVersion, abPayload);
            }

        public void writeSeniorMetadataExtensionHeader(WriteBuffer.BufferOutput output, int nType, int nVersion,
                int cbPayload)
                throws Exception
            {
            output.writeInt(SENIOR_METADATA_EXTENSION_MAGIC);
            output.writeByte(nType);
            output.writeByte(nVersion);
            output.writeInt(cbPayload);
            }

        public void writeSeniorMetadataMalformedExtension(WriteBuffer.BufferOutput output)
                throws Exception
            {
            output.writeInt(SENIOR_METADATA_EXTENSION_MAGIC);
            output.writeByte(PROOF_TYPE);
            output.writeByte(PROOF_VERSION);
            output.writeInt(PROOF.length + 1);
            output.write(PROOF);
            }

        @Override
        public boolean isVersionCompatible(Member member, IntPredicate predicate)
            {
            return m_fSenderCompatible && predicate.test(VersionHelper.encodeVersion(15, 1, 2, 0, 0));
            }

        @Override
        public boolean isVersionCompatible(MemberSet setMembers, IntPredicate predicate)
            {
            return m_fRecipientsCompatible && !m_fHasIncompatibleRecipient
                    && predicate.test(VersionHelper.encodeVersion(15, 1, 2, 0, 0));
            }

        public void setRecipientsCompatible(boolean fCompatible)
            {
            m_fRecipientsCompatible = fCompatible;
            m_fHasIncompatibleRecipient = false;
            }

        public void setMemberCompatible(int nMemberId, boolean fCompatible)
            {
            if (!fCompatible)
                {
                m_fHasIncompatibleRecipient = true;
                }
            }

        @Override
        protected void readRequestExtensions(Message msg, ReadBuffer.BufferInput input)
                throws IOException
            {
            if (m_fExpectRequestBeforeSenior)
                {
                assertNull(msg.getSeniorMetadataProof());
                m_fRequestExtensionsRead = true;
                }
            super.readRequestExtensions(msg, input);
            }

        @Override
        public void readSeniorMetadataExtensions(Message msg, ReadBuffer.BufferInput input)
                throws IOException
            {
            if (m_fExpectRequestBeforeSenior)
                {
                assertTrue(m_fRequestExtensionsRead);
                m_fSeniorMetadataRead = true;
                }
            super.readSeniorMetadataExtensions(msg, input);
            }

        public void expectRequestExtensionsBeforeSeniorMetadata()
            {
            m_fExpectRequestBeforeSenior = true;
            }

        public boolean wasRequestExtensionsRead()
            {
            return m_fRequestExtensionsRead;
            }

        public boolean wasSeniorMetadataRead()
            {
            return m_fSeniorMetadataRead;
            }

        static final int PROOF_TYPE    = SENIOR_METADATA_EXTENSION_TYPE_PROOF;
        static final int PROOF_VERSION = SENIOR_METADATA_EXTENSION_VERSION_PROOF;
        static final int MAX_PAYLOAD   = SENIOR_METADATA_EXTENSION_MAX_PAYLOAD_LENGTH;

        private boolean m_fSenderCompatible     = true;
        private boolean m_fRecipientsCompatible = true;
        private boolean m_fHasIncompatibleRecipient;
        private boolean m_fExpectRequestBeforeSenior;
        private boolean m_fRequestExtensionsRead;
        private boolean m_fSeniorMetadataRead;
        }

    private static final byte[] PROOF   = new byte[] {3, 1, 4, 1, 5, 9};
    private static final byte[] UNKNOWN = new byte[] {2, 7, 1, 8};
    private static final long   TIMESTAMP = 123456789L;
    }
