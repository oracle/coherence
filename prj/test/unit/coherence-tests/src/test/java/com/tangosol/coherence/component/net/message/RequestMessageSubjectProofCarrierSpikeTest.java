/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.component.net.message;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.MemberSet;
import com.tangosol.coherence.component.net.RequestContext;
import com.tangosol.coherence.component.net.memberSet.SingleMemberSet;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.internal.util.VersionHelper;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;
import com.tangosol.io.pof.PofPrincipal;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.UUID;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.function.IntPredicate;
import javax.security.auth.Subject;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Byte-compatibility spike for the PEER-01 Slice E subject-proof carrier.
 *
 * @author OpenAI  2026.05.19
 *
 * @since 26.07
 */
public class RequestMessageSubjectProofCarrierSpikeTest
    {
    @Test
    public void shouldPreserveLegacyRequestContextBytesForNullSubject()
            throws Exception
        {
        RequestContext ctx = new RequestContext();
        ctx.setOldestPendingSUID(17L);
        ctx.setRequestSUID(23L);
        ctx.setSubjectProof(PROOF);

        ByteArrayOutputStream outExpected = new ByteArrayOutputStream();
        DataOutputStream      outData     = new DataOutputStream(outExpected);
        outData.writeLong(17L);
        outData.writeInt(6);
        outData.writeByte(0);

        assertArrayEquals(outExpected.toByteArray(), writeContext(ctx));
        }

    @Test
    public void shouldPreserveLegacyRequestContextBytesForNonNullSubject()
            throws Exception
        {
        Subject subject = new Subject();
        subject.getPrincipals().add(new PofPrincipal("CN=admin"));
        subject.setReadOnly();

        RequestContext ctx = new RequestContext();
        ctx.setOldestPendingSUID(19L);
        ctx.setRequestSUID(29L);
        ctx.setSubject(subject);
        ctx.setSubjectProof(PROOF);

        ByteArrayOutputStream outExpected = new ByteArrayOutputStream();
        DataOutputStream      outData     = new DataOutputStream(outExpected);
        outData.writeLong(19L);
        outData.writeInt(10);
        outData.writeByte(1);
        outData.writeUTF("CN=admin");

        assertArrayEquals(outExpected.toByteArray(), writeContext(ctx));
        }

    @Test
    public void shouldEvaluateSubjectProofV1CompatibilityPredicate()
        {
        assertTrue(RequestMessage.isSubjectProofV1Compatible(VersionHelper.encodeVersion(15, 0, 0, 2601, 0)));
        assertTrue(RequestMessage.isSubjectProofV1Compatible(VersionHelper.encodeVersion(26, 1, 0)));
        assertTrue(RequestMessage.isSubjectProofV1Compatible(VersionHelper.encodeVersion(15, 1, 2, 0, 0)));
        assertTrue(RequestMessage.isSubjectProofV1Compatible(VersionHelper.encodeVersion(15, 1, 1, 0, 4)));
        assertTrue(RequestMessage.isSubjectProofV1Compatible(VersionHelper.encodeVersion(14, 1, 2, 0, 8)));
        assertTrue(RequestMessage.isSubjectProofV1Compatible(VersionHelper.encodeVersion(14, 1, 1, 2206, 18)));
        assertTrue(RequestMessage.isSubjectProofV1Compatible(VersionHelper.encodeVersion(14, 1, 1, 0, 27)));
        assertTrue(RequestMessage.isSubjectProofV1Compatible(VersionHelper.encodeVersion(12, 2, 1, 4, 31)));

        assertFalse(RequestMessage.isSubjectProofV1Compatible(VersionHelper.encodeVersion(15, 1, 1, 0, 3)));
        assertFalse(RequestMessage.isSubjectProofV1Compatible(VersionHelper.encodeVersion(14, 1, 2, 0, 7)));
        assertFalse(RequestMessage.isSubjectProofV1Compatible(VersionHelper.encodeVersion(14, 1, 1, 2206, 17)));
        assertFalse(RequestMessage.isSubjectProofV1Compatible(VersionHelper.encodeVersion(14, 1, 1, 0, 26)));
        assertFalse(RequestMessage.isSubjectProofV1Compatible(VersionHelper.encodeVersion(12, 2, 1, 4, 30)));
        }

    @Test
    public void shouldLeaveExtensionAfterFinalTracingReader()
            throws Exception
        {
        TestPartitionedCache service = new TestPartitionedCache();
        PartitionedCache.AggregateAllRequest request = new PartitionedCache.AggregateAllRequest(null, service, true);
        request.setKeySet(Collections.emptySet());
        request.setAggregatorBinary(new Binary(new byte[] {1, 2, 3}));

        PartitionedCache.AggregateAllRequest result = new PartitionedCache.AggregateAllRequest(null, service, true);
        assertRoundTripCarriesProof(service, request, result);
        }

    @Test
    public void shouldLeaveExtensionAfterOptionalFieldAfterTracing()
            throws Exception
        {
        TestPartitionedCache service = new TestPartitionedCache();
        PartitionedCache.GetAllRequest request = new PartitionedCache.GetAllRequest(null, service, true);
        request.setKeySet(Collections.emptySet());
        request.setSizeThreshold(123);
        request.setAllowBackupRead(true);

        PartitionedCache.GetAllRequest result = new PartitionedCache.GetAllRequest(null, service, true);
        assertRoundTripCarriesProof(service, request, result);
        assertTrue(result.isAllowBackupRead());
        }

    @Test
    public void shouldLeaveExtensionAfterOptionalFieldBeforeTracing()
            throws Exception
        {
        TestPartitionedCache service = new TestPartitionedCache();
        PartitionedCache.GetRequest request = new PartitionedCache.GetRequest(null, service, true);
        request.setCacheId(1L);
        request.setKey(new Binary(new byte[] {7}));
        request.setAllowBackupRead(true);

        PartitionedCache.GetRequest result = new PartitionedCache.GetRequest(null, service, true);
        assertRoundTripCarriesProof(service, request, result);
        assertTrue(result.isAllowBackupRead());
        }

    @Test
    public void shouldLeaveExtensionAfterNonTracingRequestMessage()
            throws Exception
        {
        TestPartitionedCache service = new TestPartitionedCache();

        assertRoundTripCarriesProof(service, new RequestMessage(), new RequestMessage());
        }

    @Test
    public void shouldNotConsumeBytesWhenMagicIsAbsent()
            throws Exception
        {
        TestPartitionedCache service = new TestPartitionedCache();
        RequestMessage       request = new RequestMessage();
        RequestMessage       result  = new RequestMessage();

        configure(request, service, false);

        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput output = buffer.getBufferOutput();
        request.write(output);
        output.writeInt(0x01020304);

        configureIncoming(result, service);
        ReadBuffer.BufferInput input = buffer.toBinary().getBufferInput();
        result.read(input);

        assertEquals(Integer.BYTES, input.available());
        service.readProof(result, input);
        assertEquals(Integer.BYTES, input.available());
        assertNull(result.getRequestContext().getSubjectProof());
        }

    @Test
    public void shouldRejectMalformedExtensionAfterMagic()
            throws Exception
        {
        TestPartitionedCache service = new TestPartitionedCache();
        RequestMessage       request = new RequestMessage();
        RequestMessage       result  = new RequestMessage();

        configure(request, service, false);

        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput output = buffer.getBufferOutput();
        request.write(output);
        service.writeMalformedProof(output);

        configureIncoming(result, service);
        ReadBuffer.BufferInput input = buffer.toBinary().getBufferInput();
        result.read(input);

        try
            {
            service.readProof(result, input);
            fail("expected malformed request extension");
            }
        catch (IOException e)
            {
            assertTrue(e.getMessage().contains("malformed request extension"));
            }
        }

    @Test
    public void shouldSuppressExtensionForIncompatibleOrMixedRecipients()
            throws Exception
        {
        TestPartitionedCache service = new TestPartitionedCache();
        RequestMessage       request = new RequestMessage();

        configure(request, service, true);
        Binary binLegacy = writeMessageBody(request);

        service.setRecipientsCompatible(false);
        Binary binMixed = writeMessageBodyAndProof(service, request);

        assertEquals(binLegacy, binMixed);
        }

    private static void assertRoundTripCarriesProof(TestPartitionedCache service, RequestMessage request,
            RequestMessage result)
            throws Exception
        {
        configure(request, service, true);

        Binary bin = writeMessageBodyAndProof(service, request);

        configureIncoming(result, service);
        ReadBuffer.BufferInput input = bin.getBufferInput();
        result.read(input);

        assertEquals(10 + PROOF.length, input.available());
        service.readProof(result, input);

        assertEquals(0, input.available());
        assertArrayEquals(PROOF, result.getRequestContext().getSubjectProof());
        }

    private static void configure(RequestMessage request, TestPartitionedCache service, boolean fProof)
            throws Exception
        {
        request.setService(service);
        request.setToMemberSet(SingleMemberSet.instantiate(member(2)));

        RequestContext ctx = new RequestContext();
        ctx.setOldestPendingSUID(101L);
        ctx.setRequestSUID(103L);
        if (fProof)
            {
            ctx.setSubjectProof(PROOF);
            }
        request.setRequestContext(ctx);
        }

    private static void configureIncoming(RequestMessage request, TestPartitionedCache service)
            throws Exception
        {
        request.setService(service);
        request.setFromMember(member(1));
        }

    private static byte[] writeContext(RequestContext ctx)
            throws Exception
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(128);
        ctx.writeExternal(buffer.getBufferOutput());
        return buffer.toByteArray();
        }

    private static Binary writeMessageBody(RequestMessage request)
            throws Exception
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        request.write(buffer.getBufferOutput());
        return buffer.toBinary();
        }

    private static Binary writeMessageBodyAndProof(TestPartitionedCache service, RequestMessage request)
            throws Exception
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput output = buffer.getBufferOutput();
        request.write(output);
        service.writeProof(request, output);
        return buffer.toBinary();
        }

    private static Member member(int nId)
            throws Exception
        {
        Member member = new Member();
        member.configureDead(nId, new UUID(System.currentTimeMillis(), InetAddress.getByName("127.0.0.1"),
                7574 + nId, nId), System.currentTimeMillis());
        return member;
        }

    public static class TestPartitionedCache
            extends PartitionedCache
        {
        public TestPartitionedCache()
            {
            setSerializer(ExternalizableHelper.ensureSerializer(null));
            }

        public void readProof(RequestMessage request, ReadBuffer.BufferInput input)
                throws Exception
            {
            readRequestExtensions(request, input);
            }

        public void writeProof(RequestMessage request, WriteBuffer.BufferOutput output)
                throws Exception
            {
            writeRequestExtensions(request, output);
            }

        public void writeMalformedProof(WriteBuffer.BufferOutput output)
                throws Exception
            {
            output.writeInt(REQUEST_EXTENSION_MAGIC);
            output.writeByte(REQUEST_EXTENSION_TYPE_SUBJECT_PROOF);
            output.writeByte(REQUEST_EXTENSION_VERSION_SUBJECT_PROOF);
            output.writeInt(PROOF.length + 1);
            }

        @Override
        public boolean isVersionCompatible(Member member, IntPredicate predicate)
            {
            return m_fSenderCompatible && predicate.test(VersionHelper.encodeVersion(15, 0, 0, 2601, 0));
            }

        @Override
        public boolean isVersionCompatible(MemberSet setMembers, IntPredicate predicate)
            {
            return m_fRecipientsCompatible && predicate.test(VersionHelper.encodeVersion(15, 0, 0, 2601, 0));
            }

        public void setRecipientsCompatible(boolean fCompatible)
            {
            m_fRecipientsCompatible = fCompatible;
            }

        private boolean m_fSenderCompatible     = true;
        private boolean m_fRecipientsCompatible = true;
        }

    private static final byte[] PROOF = new byte[] {3, 1, 4, 1, 5, 9};
    }
