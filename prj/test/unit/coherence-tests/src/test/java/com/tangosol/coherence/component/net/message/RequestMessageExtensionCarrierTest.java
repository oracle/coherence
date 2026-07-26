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
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.UUID;

import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.util.function.IntPredicate;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for the PEER-01 Slice E request-message extension carrier.
 *
 * @author Aleks Seovic  2026.05.18
 * @since 15.1.2.0
 */
public class RequestMessageExtensionCarrierTest
    {
    @Test
    public void shouldParseMultipleRecordsAndSkipUnknownRecord()
            throws Exception
        {
        TestPartitionedCache service = new TestPartitionedCache();
        RequestMessage       result  = readWithExtensions(service,
                new Extension(99, 1, UNKNOWN),
                new Extension(TestPartitionedCache.SUBJECT_PROOF_TYPE,
                        TestPartitionedCache.SUBJECT_PROOF_VERSION, PROOF));

        assertArrayEquals(PROOF, result.getRequestContext().getSubjectProof());
        }

    @Test
    public void shouldSkipUnknownWellFormedRecordWithoutSubjectProof()
            throws Exception
        {
        TestPartitionedCache service = new TestPartitionedCache();
        RequestMessage       result  = readWithExtensions(service, new Extension(99, 1, UNKNOWN));

        assertNull(result.getRequestContext().getSubjectProof());
        }

    @Test
    public void shouldSkipLargeUnknownWellFormedRecordWithoutCopyingPayload()
            throws Exception
        {
        TestPartitionedCache service = new TestPartitionedCache();
        ReadBuffer.BufferInput input = inputWithLargeUnknownExtension(service);
        RequestMessage result = incoming(service);
        result.read(input);
        service.readExtensions(result, input);

        assertArrayEquals(PROOF, result.getRequestContext().getSubjectProof());
        }

    @Test
    public void shouldRejectOversizedUnknownRecordBeforePayloadAllocation()
            throws Exception
        {
        TestPartitionedCache service = new TestPartitionedCache();
        ReadBuffer.BufferInput input = inputWithOversizedHeader(service, 99,
                TestPartitionedCache.SUBJECT_PROOF_VERSION);
        RequestMessage result = incoming(service);
        result.read(input);

        assertFailure(service, result, input, "malformed request extension");
        }

    @Test
    public void shouldRejectOversizedKnownSubjectProofRecordBeforePayloadAllocation()
            throws Exception
        {
        TestPartitionedCache service = new TestPartitionedCache();
        ReadBuffer.BufferInput input = inputWithOversizedHeader(service, TestPartitionedCache.SUBJECT_PROOF_TYPE,
                TestPartitionedCache.SUBJECT_PROOF_VERSION);
        RequestMessage result = incoming(service);
        result.read(input);

        assertFailure(service, result, input, "malformed request extension");
        }

    @Test
    public void shouldRejectOversizedExtensionPayloadBeforeWritingRecord()
            throws Exception
        {
        TestPartitionedCache  service = new TestPartitionedCache();
        ByteArrayWriteBuffer  buffer  = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput output = buffer.getBufferOutput();

        try
            {
            service.writeExtension(output, TestPartitionedCache.SUBJECT_PROOF_TYPE,
                    TestPartitionedCache.SUBJECT_PROOF_VERSION, new byte[TestPartitionedCache.MAX_PAYLOAD + 1]);
            fail("expected oversized request extension");
            }
        catch (IOException e)
            {
            assertTrue(e.getMessage().contains("oversized request extension"));
            }

        assertEquals(0, buffer.toBinary().length());
        }

    @Test
    public void shouldWriteMaximumSizedExtensionPayload()
            throws Exception
        {
        TestPartitionedCache  service = new TestPartitionedCache();
        ByteArrayWriteBuffer  buffer  = new ByteArrayWriteBuffer(TestPartitionedCache.MAX_PAYLOAD
                + TestPartitionedCache.HEADER_LENGTH);
        WriteBuffer.BufferOutput output = buffer.getBufferOutput();

        service.writeExtension(output, TestPartitionedCache.SUBJECT_PROOF_TYPE,
                TestPartitionedCache.SUBJECT_PROOF_VERSION, new byte[TestPartitionedCache.MAX_PAYLOAD]);

        assertEquals(TestPartitionedCache.HEADER_LENGTH + TestPartitionedCache.MAX_PAYLOAD,
                buffer.toBinary().length());
        }

    @Test
    public void shouldRejectUnsupportedKnownSubjectProofVersion()
            throws Exception
        {
        TestPartitionedCache service = new TestPartitionedCache();
        ReadBuffer.BufferInput input = inputWithExtensions(service,
                new Extension(TestPartitionedCache.SUBJECT_PROOF_TYPE,
                        TestPartitionedCache.SUBJECT_PROOF_VERSION + 1, PROOF));
        RequestMessage result = incoming(service);
        result.read(input);

        assertFailure(service, result, input, "unsupported subject-proof request extension");
        }

    @Test
    public void shouldRejectMalformedRecordAfterPositiveMagic()
            throws Exception
        {
        TestPartitionedCache service = new TestPartitionedCache();
        ReadBuffer.BufferInput input = inputWithMalformedLength(service);
        RequestMessage result = incoming(service);
        result.read(input);

        assertFailure(service, result, input, "malformed request extension");
        }

    @Test
    public void shouldRejectTruncatedUnknownRecordAfterPositiveMagic()
            throws Exception
        {
        TestPartitionedCache service = new TestPartitionedCache();
        ReadBuffer.BufferInput input = inputWithTruncatedUnknownRecord(service);
        RequestMessage result = incoming(service);
        result.read(input);

        assertFailure(service, result, input, "malformed request extension");
        }

    @Test
    public void shouldRejectStrayBytesAfterPositiveMagic()
            throws Exception
        {
        TestPartitionedCache service = new TestPartitionedCache();
        ReadBuffer.BufferInput input = inputWithStrayBytes(service);
        RequestMessage result = incoming(service);
        result.read(input);

        assertFailure(service, result, input, "stray request-extension bytes");
        }

    private static RequestMessage readWithExtensions(TestPartitionedCache service, Extension... aExtension)
            throws Exception
        {
        ReadBuffer.BufferInput input = inputWithExtensions(service, aExtension);
        RequestMessage result = incoming(service);
        result.read(input);
        service.readExtensions(result, input);
        return result;
        }

    private static ReadBuffer.BufferInput inputWithExtensions(TestPartitionedCache service, Extension... aExtension)
            throws Exception
        {
        RequestMessage request = outgoing(service);

        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput output = buffer.getBufferOutput();
        request.write(output);
        for (Extension extension : aExtension)
            {
            service.writeExtension(output, extension.m_nType, extension.m_nVersion, extension.m_abPayload);
            }
        return buffer.toBinary().getBufferInput();
        }

    private static ReadBuffer.BufferInput inputWithLargeUnknownExtension(TestPartitionedCache service)
            throws Exception
        {
        RequestMessage request = outgoing(service);

        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(TestPartitionedCache.MAX_PAYLOAD + 1024);
        WriteBuffer.BufferOutput output = buffer.getBufferOutput();
        request.write(output);
        service.writeExtensionHeader(output, 99, 1, TestPartitionedCache.MAX_PAYLOAD);
        for (int i = 0; i < TestPartitionedCache.MAX_PAYLOAD; i++)
            {
            output.writeByte(0);
            }
        service.writeExtension(output, TestPartitionedCache.SUBJECT_PROOF_TYPE,
                TestPartitionedCache.SUBJECT_PROOF_VERSION, PROOF);
        return buffer.toBinary().getBufferInput();
        }

    private static ReadBuffer.BufferInput inputWithOversizedHeader(TestPartitionedCache service, int nType,
            int nVersion)
            throws Exception
        {
        RequestMessage request = outgoing(service);

        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput output = buffer.getBufferOutput();
        request.write(output);
        service.writeExtensionHeader(output, nType, nVersion, TestPartitionedCache.MAX_PAYLOAD + 1);
        return buffer.toBinary().getBufferInput();
        }

    private static ReadBuffer.BufferInput inputWithMalformedLength(TestPartitionedCache service)
            throws Exception
        {
        RequestMessage request = outgoing(service);

        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput output = buffer.getBufferOutput();
        request.write(output);
        service.writeMalformedExtension(output);
        return buffer.toBinary().getBufferInput();
        }

    private static ReadBuffer.BufferInput inputWithTruncatedUnknownRecord(TestPartitionedCache service)
            throws Exception
        {
        RequestMessage request = outgoing(service);

        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput output = buffer.getBufferOutput();
        request.write(output);
        service.writeExtensionHeader(output, 99, 1, UNKNOWN.length + 1);
        output.write(UNKNOWN);
        return buffer.toBinary().getBufferInput();
        }

    private static ReadBuffer.BufferInput inputWithStrayBytes(TestPartitionedCache service)
            throws Exception
        {
        RequestMessage request = outgoing(service);

        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput output = buffer.getBufferOutput();
        request.write(output);
        service.writeExtension(output, 99, 1, UNKNOWN);
        output.writeByte(1);
        return buffer.toBinary().getBufferInput();
        }

    private static void assertFailure(TestPartitionedCache service, RequestMessage result,
            ReadBuffer.BufferInput input, String sMessage)
            throws Exception
        {
        try
            {
            service.readExtensions(result, input);
            fail("expected " + sMessage);
            }
        catch (IOException e)
            {
            assertTrue(e.getMessage().contains(sMessage));
            }
        }

    private static RequestMessage outgoing(TestPartitionedCache service)
            throws Exception
        {
        RequestMessage request = new RequestMessage();
        request.setService(service);
        request.setToMemberSet(SingleMemberSet.instantiate(member(2)));

        RequestContext ctx = new RequestContext();
        ctx.setOldestPendingSUID(101L);
        ctx.setRequestSUID(103L);
        request.setRequestContext(ctx);
        return request;
        }

    private static RequestMessage incoming(TestPartitionedCache service)
            throws Exception
        {
        RequestMessage request = new RequestMessage();
        request.setService(service);
        request.setFromMember(member(1));
        return request;
        }

    private static Member member(int nId)
            throws Exception
        {
        Member member = new Member();
        member.configureDead(nId, new UUID(System.currentTimeMillis(), InetAddress.getByName("127.0.0.1"),
                7574 + nId, nId), System.currentTimeMillis());
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

    public static class TestPartitionedCache
            extends PartitionedCache
        {
        public TestPartitionedCache()
            {
            setSerializer(ExternalizableHelper.ensureSerializer(null));
            }

        public void readExtensions(RequestMessage request, ReadBuffer.BufferInput input)
                throws Exception
            {
            readRequestExtensions(request, input);
            }

        public void writeExtension(WriteBuffer.BufferOutput output, int nType, int nVersion, byte[] abPayload)
                throws Exception
            {
            writeRequestExtension(output, nType, nVersion, abPayload);
            }

        public void writeExtensionHeader(WriteBuffer.BufferOutput output, int nType, int nVersion, int cbPayload)
                throws Exception
            {
            output.writeInt(REQUEST_EXTENSION_MAGIC);
            output.writeByte(nType);
            output.writeByte(nVersion);
            output.writeInt(cbPayload);
            }

        public void writeMalformedExtension(WriteBuffer.BufferOutput output)
                throws Exception
            {
            output.writeInt(REQUEST_EXTENSION_MAGIC);
            output.writeByte(SUBJECT_PROOF_TYPE);
            output.writeByte(SUBJECT_PROOF_VERSION);
            output.writeInt(PROOF.length + 1);
            output.write(PROOF);
            }

        @Override
        public boolean isVersionCompatible(Member member, IntPredicate predicate)
            {
            return predicate.test(VersionHelper.encodeVersion(15, 1, 2, 0, 0));
            }

        @Override
        public boolean isVersionCompatible(MemberSet setMembers, IntPredicate predicate)
            {
            return predicate.test(VersionHelper.encodeVersion(15, 1, 2, 0, 0));
            }

        static final int SUBJECT_PROOF_TYPE    = REQUEST_EXTENSION_TYPE_SUBJECT_PROOF;
        static final int SUBJECT_PROOF_VERSION = REQUEST_EXTENSION_VERSION_SUBJECT_PROOF;
        static final int HEADER_LENGTH         = REQUEST_EXTENSION_HEADER_LENGTH;
        static final int MAX_PAYLOAD           = REQUEST_EXTENSION_MAX_PAYLOAD_LENGTH;
        }

    private static final byte[] PROOF   = new byte[] {3, 1, 4, 1, 5, 9};
    private static final byte[] UNKNOWN = new byte[] {2, 7, 1, 8};
    }
