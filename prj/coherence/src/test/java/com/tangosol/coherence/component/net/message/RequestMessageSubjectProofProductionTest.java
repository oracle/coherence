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
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet;
import com.tangosol.coherence.component.net.memberSet.SingleMemberSet;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.internal.net.security.SubjectProof;
import com.tangosol.internal.net.security.SubjectProofPayload;
import com.tangosol.internal.net.security.SubjectProofProvider;
import com.tangosol.internal.net.security.SubjectProofProviders;
import com.tangosol.internal.net.security.SubjectProofVerification;
import com.tangosol.util.VersionHelper;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;
import com.tangosol.io.pof.PofPrincipal;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.UUID;

import org.junit.Test;

import java.net.InetAddress;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;

import java.util.function.IntPredicate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.Subject;

import static com.tangosol.internal.net.security.SubjectProofVerification.Status.EXPIRED;
import static com.tangosol.internal.net.security.SubjectProofVerification.Status.MISSING;
import static com.tangosol.internal.net.security.SubjectProofVerification.Status.PAYLOAD_MISMATCH;
import static com.tangosol.internal.net.security.SubjectProofVerification.Status.PRINCIPAL_MISMATCH;
import static com.tangosol.internal.net.security.SubjectProofVerification.Status.PROOF_MISMATCH;
import static com.tangosol.internal.net.security.SubjectProofVerification.Status.UNKNOWN_KEY;
import static com.tangosol.internal.net.security.SubjectProofVerification.Status.VALID;
import static com.tangosol.internal.net.security.SubjectProofVerification.Status.WRONG_ALGORITHM;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for PEER-01 Slice E subject-proof production wiring.
 *
 * @author Aleks Seovic  2026.05.19
 * @since 15.1.2.0
 */
public class RequestMessageSubjectProofProductionTest
    {
    @Test
    public void shouldKeepDefaultDisabledProviderBehaviorNeutral()
            throws Exception
        {
        TestPartitionedCache service = new TestPartitionedCache();
        RequestMessage       request = requestWithSubject(service);
        RequestContext       context = request.getRequestContext();
        byte[]               abCtx   = writeContext(context);
        Binary               binBody = writeBody(request);

        assertEquals(binBody, writeBodyAndExtensions(service, request));
        assertArrayEquals(abCtx, writeContext(context));
        assertNull(context.getSubjectProof());
        }

    @Test
    public void shouldProduceStableProofBytesForEnabledProvider()
            throws Exception
        {
        TestPartitionedCache service  = enabledService();
        RequestMessage       request1 = requestWithSubject(service);
        RequestMessage       request2 = requestWithSubject(service);

        writeBodyAndExtensions(service, request1);
        writeBodyAndExtensions(service, request2);

        assertArrayEquals(request1.getRequestContext().getSubjectProof(),
                request2.getRequestContext().getSubjectProof());
        }

    @Test
    public void shouldAttachProducedProofOnlyForCompatibleRecipients()
            throws Exception
        {
        TestPartitionedCache service = enabledService();
        RequestMessage       request = requestWithSubject(service);
        Binary               bin     = writeBodyAndExtensions(service, request);
        RequestMessage       result  = readWithExtensions(service, bin);

        assertTrue(bin.length() > writeBody(request).length());
        assertEquals(VALID, service.getProvider()
                .verifyProof(result.getRequestContext().getSubjectProof(), service.payload(result), NOW)
                .getStatus());
        }

    @Test
    public void shouldSuppressProducedProofForMixedRecipientStreams()
            throws Exception
        {
        TestPartitionedCache service = enabledService();
        RequestMessage       request = requestWithSubject(service);
        Binary               binBody = writeBody(request);

        service.setRecipientsCompatible(false);

        assertEquals(binBody, writeBodyAndExtensions(service, request));
        assertNull(request.getRequestContext().getSubjectProof());
        }

    @Test
    public void shouldPreserveExistingForwardedProofBytes()
            throws Exception
        {
        TestPartitionedCache service = enabledService();
        RequestMessage       request = requestWithSubject(service);
        RequestContext       context = request.getRequestContext();
        byte[]               abProof = new byte[] {9, 8, 7, 6};

        context.setSubjectProof(abProof);

        RequestMessage result = readWithExtensions(service, writeBodyAndExtensions(service, request));

        assertArrayEquals(abProof, context.getSubjectProof());
        assertArrayEquals(abProof, result.getRequestContext().getSubjectProof());
        }

    private static RequestMessage requestWithSubject(TestPartitionedCache service)
            throws Exception
        {
        RequestMessage request = new RequestMessage();
        request.setService(service);
        request.setToMemberSet(SingleMemberSet.instantiate(member(2)));

        Subject subject = new Subject();
        subject.getPrincipals().add(new PofPrincipal("CN=user"));
        subject.getPrincipals().add(new PofPrincipal("CN=admin"));
        subject.setReadOnly();

        RequestContext ctx = new RequestContext();
        ctx.setOldestPendingSUID(101L);
        ctx.setRequestSUID(REQUEST_SUID);
        ctx.setSubject(subject);
        request.setRequestContext(ctx);
        return request;
        }

    private static RequestMessage readWithExtensions(TestPartitionedCache service, Binary bin)
            throws Exception
        {
        RequestMessage result = new RequestMessage();
        result.setService(service);
        result.setFromMember(member(1));

        ReadBuffer.BufferInput input = bin.getBufferInput();
        result.read(input);
        service.readExtensions(result, input);
        assertEquals(0, input.available());
        return result;
        }

    private static byte[] writeContext(RequestContext context)
            throws Exception
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(128);
        context.writeExternal(buffer.getBufferOutput());
        return buffer.toByteArray();
        }

    private static Binary writeBody(RequestMessage request)
            throws Exception
        {
        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        request.write(buffer.getBufferOutput());
        return buffer.toBinary();
        }

    private static Binary writeBodyAndExtensions(TestPartitionedCache service, RequestMessage request)
            throws Exception
        {
        ByteArrayWriteBuffer    buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput output = buffer.getBufferOutput();
        request.write(output);
        service.writeExtensions(request, output);
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

    private static TestPartitionedCache enabledService()
        {
        TestPartitionedCache service = new TestPartitionedCache();
        service.setProvider(new DeterministicSubjectProofProvider());
        return service;
        }

    private static MasterMemberSet memberSet(boolean fCompatible)
        {
        MasterMemberSet setMember = new MasterMemberSet();
        int             nVersion  = fCompatible
                ? VersionHelper.encodeVersion(15, 1, 2, 0, 0)
                : VersionHelper.encodeVersion(15, 1, 1, 0, 3);

        setMember.setServiceVersionInt(new int[] {0, nVersion, nVersion});
        return setMember;
        }

    public static class TestPartitionedCache
            extends PartitionedCache
        {
        public TestPartitionedCache()
            {
            setSerializer(ExternalizableHelper.ensureSerializer(null));
            setServiceName("DistributedCache");
            setClusterMemberSet(memberSet(true));
            }

        public SubjectProofProvider getProvider()
            {
            return m_provider;
            }

        public void setProvider(SubjectProofProvider provider)
            {
            m_provider = provider == null ? SubjectProofProviders.disabled() : provider;
            }

        public SubjectProofPayload payload(RequestMessage request)
            {
            return createSubjectProofPayload(request, request.getRequestContext());
            }

        public void readExtensions(RequestMessage request, ReadBuffer.BufferInput input)
                throws Exception
            {
            readRequestExtensions(request, input);
            }

        public void writeExtensions(RequestMessage request, WriteBuffer.BufferOutput output)
                throws Exception
            {
            writeRequestExtensions(request, output);
            }

        @Override
        protected SubjectProofProvider getSubjectProofProvider()
            {
            return m_provider;
            }

        @Override
        protected String getSubjectProofAlgorithmId()
            {
            return ALGORITHM;
            }

        @Override
        protected String getSubjectProofKeyId()
            {
            return KEY_ID;
            }

        @Override
        protected String getSubjectProofIssuerId()
            {
            return ISSUER;
            }

        @Override
        protected String getSubjectProofSubjectSource()
            {
            return SOURCE;
            }

        @Override
        protected long getSubjectProofNonce(RequestMessage msg, RequestContext ctx)
            {
            return NONCE;
            }

        @Override
        protected long getSubjectProofIssuedAtMillis(RequestMessage msg, RequestContext ctx)
            {
            return ISSUED_AT;
            }

        @Override
        protected long getSubjectProofExpiresAtMillis(RequestMessage msg, RequestContext ctx, long lIssuedAtMillis)
            {
            return EXPIRES_AT;
            }

        @Override
        public boolean isVersionCompatible(Member member, IntPredicate predicate)
            {
            return predicate.test(VersionHelper.encodeVersion(15, 1, 2, 0, 0));
            }

        @Override
        public boolean isVersionCompatible(MemberSet setMembers, IntPredicate predicate)
            {
            return m_fRecipientsCompatible && predicate.test(VersionHelper.encodeVersion(15, 1, 2, 0, 0));
            }

        public void setRecipientsCompatible(boolean fCompatible)
            {
            m_fRecipientsCompatible = fCompatible;
            setClusterMemberSet(memberSet(fCompatible));
            }

        private SubjectProofProvider m_provider = SubjectProofProviders.disabled();
        private boolean              m_fRecipientsCompatible = true;
        }

    private static class DeterministicSubjectProofProvider
            implements SubjectProofProvider
        {
        @Override
        public byte[] createProof(SubjectProofPayload payload)
            {
            return new SubjectProof(payload, mac(payload)).toByteArray();
            }

        @Override
        public SubjectProofVerification verifyProof(byte[] abProof, SubjectProofPayload expectedPayload,
                long lNowMillis)
            {
            if (abProof == null || abProof.length == 0)
                {
                return SubjectProofVerification.failed(MISSING, "missing");
                }

            SubjectProof proof;
            try
                {
                proof = SubjectProof.fromByteArray(abProof);
                }
            catch (Exception e)
                {
                return SubjectProofVerification.failed(SubjectProofVerification.Status.MALFORMED, "malformed");
                }

            SubjectProofPayload payload = proof.getPayload();
            if (!KEY_ID.equals(payload.getKeyId()))
                {
                return SubjectProofVerification.failed(UNKNOWN_KEY, payload.getKeyId());
                }
            if (!ALGORITHM.equals(payload.getAlgorithmId()))
                {
                return SubjectProofVerification.failed(WRONG_ALGORITHM, payload.getAlgorithmId());
                }
            if (payload.getExpiresAtMillis() < lNowMillis)
                {
                return SubjectProofVerification.failed(EXPIRED, "expired");
                }
            if (!payload.matchesScope(expectedPayload))
                {
                return SubjectProofVerification.failed(PAYLOAD_MISMATCH, "payload");
                }
            if (!payload.matchesPrincipals(expectedPayload))
                {
                return SubjectProofVerification.failed(PRINCIPAL_MISMATCH, "principals");
                }
            if (!MessageDigest.isEqual(mac(payload), proof.getProofBytes()))
                {
                return SubjectProofVerification.failed(PROOF_MISMATCH, "proof");
                }
            return SubjectProofVerification.valid();
            }

        private static byte[] mac(SubjectProofPayload payload)
            {
            try
                {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(SECRET, "HmacSHA256"));
                return mac.doFinal(payload.toByteArray());
                }
            catch (Exception e)
                {
                throw new IllegalStateException(e);
                }
            }
        }

    private static final String ALGORITHM = "test-hmac-sha256";
    private static final String KEY_ID    = "test-key";
    private static final String ISSUER    = "member-1";
    private static final String SOURCE    = "extend";

    private static final long REQUEST_SUID = 142L;
    private static final long NONCE        = 99L;
    private static final long ISSUED_AT    = 1_000L;
    private static final long EXPIRES_AT   = 2_000L;
    private static final long NOW          = 1_500L;

    private static final byte[] SECRET = "deterministic-secret".getBytes(StandardCharsets.UTF_8);
    }
