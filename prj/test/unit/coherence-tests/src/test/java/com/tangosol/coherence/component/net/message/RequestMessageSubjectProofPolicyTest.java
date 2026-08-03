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
import com.tangosol.coherence.component.net.message.requestMessage.DistributedCacheRequest;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.Storage;
import com.tangosol.internal.net.security.SubjectProof;
import com.tangosol.internal.net.security.SubjectProofPayload;
import com.tangosol.internal.net.security.SubjectProofProvider;
import com.tangosol.internal.net.security.SubjectProofProviders;
import com.tangosol.internal.net.security.SubjectProofVerification;
import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.internal.util.VersionHelper;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.WriteBuffer;
import com.tangosol.io.pof.PofPrincipal;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.UUID;

import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Method;

import java.net.InetAddress;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntPredicate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.Subject;

import static com.tangosol.internal.net.security.SubjectProofVerification.Status.EXPIRED;
import static com.tangosol.internal.net.security.SubjectProofVerification.Status.MALFORMED;
import static com.tangosol.internal.net.security.SubjectProofVerification.Status.MISSING;
import static com.tangosol.internal.net.security.SubjectProofVerification.Status.PAYLOAD_MISMATCH;
import static com.tangosol.internal.net.security.SubjectProofVerification.Status.PRINCIPAL_MISMATCH;
import static com.tangosol.internal.net.security.SubjectProofVerification.Status.PROOF_MISMATCH;
import static com.tangosol.internal.net.security.SubjectProofVerification.Status.UNKNOWN_KEY;
import static com.tangosol.internal.net.security.SubjectProofVerification.Status.VALID;
import static com.tangosol.internal.net.security.SubjectProofVerification.Status.WRONG_ALGORITHM;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

/**
 * Unit tests for PEER-01 Slice E proof-required policy wiring.
 *
 * @author Aleks Seovic  2026.05.19
 * @since 26.07
 */
public class RequestMessageSubjectProofPolicyTest
    {
    @After
    public void cleanup()
            throws Exception
        {
        setMode(m_sOriginalMode);
        }

    @Test
    public void shouldPreserveBehaviorWhenProofIsNotRequired()
            throws Exception
        {
        setMode("prod");
        TestPartitionedCache service = serviceWithProvider(SubjectProofProviders.disabled());
        TestStorage          storage = new TestStorage(service, false);
        RequestContext       context = contextWithSubject();

        assertSame(context.getSubject(), service.getStorageAccessSubject(context, storage));
        }

    @Test
    public void shouldRejectMissingProofBeforeAuthorizerInProd()
            throws Exception
        {
        setMode("prod");
        TestPartitionedCache service = enabledService();
        TestStorage          storage = new TestStorage(service, true);

        assertThrows(SecurityException.class,
                () -> service.getStorageAccessSubject(contextWithSubject(), storage));
        }

    @Test
    public void shouldRejectDisabledProviderWhenProofIsRequired()
            throws Exception
        {
        setMode("dev");
        TestPartitionedCache service = serviceWithProvider(SubjectProofProviders.disabled());
        TestStorage          storage = new TestStorage(service, true);
        RequestContext       context = contextWithSubject();

        context.setSubjectProof(new byte[] {1, 2, 3});
        context.setSubjectProofSenderId(ISSUER);

        assertThrows(SecurityException.class,
                () -> service.getStorageAccessSubject(context, storage));
        }

    @Test
    public void shouldRejectInvalidProofBeforeAuthorizer()
            throws Exception
        {
        setMode("prod");
        TestPartitionedCache service = enabledService();
        TestStorage          storage = new TestStorage(service, true);
        RequestContext       context = contextWithSubject();

        context.setSubjectProof(service.getProvider().createProof(service.payload(context, storage)));
        context.setSubjectProofSenderId(ISSUER);
        context.getSubjectProof()[context.getSubjectProof().length - 1] ^= 0x01;

        assertThrows(SecurityException.class,
                () -> service.getStorageAccessSubject(context, storage));
        }

    @Test
    public void shouldAllowValidProofBeforeAuthorizer()
            throws Exception
        {
        setMode("prod");
        TestPartitionedCache service = enabledService();
        TestStorage          storage = new TestStorage(service, true);
        RequestContext       context = contextWithSubject();
        byte[]               abProof = service.getProvider().createProof(service.payload(context, storage));

        context.setSubjectProof(abProof);
        context.setSubjectProofSenderId(ISSUER);

        assertSame(context.getSubject(), service.getStorageAccessSubject(context, storage));
        assertArrayEquals(abProof, context.getSubjectProof());
        }

    @Test
    public void shouldLeaveNullSubjectToConfiguredAuthorizer()
            throws Exception
        {
        setMode("prod");
        TestPartitionedCache service = enabledService();
        TestStorage          storage = new TestStorage(service, true);
        RequestContext       context = new RequestContext();

        context.setOldestPendingSUID(101L);
        context.setRequestSUID(REQUEST_SUID);

        assertNull(service.getStorageAccessSubject(context, storage));
        }

    @Test
    public void shouldPreserveUnsignedSubjectPropagationInLegacy()
            throws Exception
        {
        setMode("legacy");
        TestPartitionedCache service = serviceWithProvider(SubjectProofProviders.disabled());
        TestStorage          storage = new TestStorage(service, true);
        RequestContext       context = contextWithSubject();

        assertSame(context.getSubject(), service.getStorageAccessSubject(context, storage));
        }

    @Test
    public void shouldRejectIncompatibleProofRequiredSend()
            throws Exception
        {
        setMode("prod");
        TestPartitionedCache service = enabledService();
        RequestMessage       request = requestWithSubject(service);

        service.setProofRequired(true);
        service.setRecipientsCompatible(false);

        assertThrows(java.io.IOException.class, () -> writeBodyAndExtensions(service, request));
        assertNull(request.getRequestContext().getSubjectProof());
        }

    @Test
    public void shouldVerifyWritePathProofWithReceiverStablePayload()
            throws Exception
        {
        setMode("prod");
        TestPartitionedCache service = enabledService();
        TestStorage          storage = new TestStorage(service, true);
        RequestMessage       request = requestWithSubject(service);

        service.setProofRequired(true);
        service.setProofMetadata(7L, 1_000L, Long.MAX_VALUE - 1);
        writeBodyAndExtensions(service, request);

        byte[] abProof = request.getRequestContext().getSubjectProof();
        assertNotNull(abProof);

        service.setProofMetadata(11L, 2_000L, Long.MAX_VALUE - 2);

        assertSame(request.getRequestContext().getSubject(),
                service.getStorageAccessSubject(request.getRequestContext(), storage));
        assertArrayEquals(abProof, request.getRequestContext().getSubjectProof());
        }

    @Test
    public void shouldRejectReusedMiniIdWithDifferentStableSender()
            throws Exception
        {
        setMode("prod");
        Member               issuer   = member(7, 100L);
        Member               rejoined = member(7, 200L);
        String               sIssuer  = stableMemberId(issuer);
        String               sSender  = stableMemberId(rejoined);
        TestPartitionedCache service  = serviceWithProvider(new DeterministicSubjectProofProvider(sIssuer));
        TestStorage          storage  = new TestStorage(service, true);
        RequestContext       context  = contextWithSubject();

        service.setIssuerId(sIssuer);
        context.setSubjectProof(service.getProvider().createProof(service.payload(context, storage)));
        context.setSubjectProofSenderId(sSender);

        assertThrows(SecurityException.class, () -> service.getStorageAccessSubject(context, storage));
        }

    @Test
    public void shouldAllowStableRemoteIssuerOnDifferentReceiver()
            throws Exception
        {
        setMode("prod");
        Member               issuer   = member(8, 300L);
        String               sIssuer  = stableMemberId(issuer);
        TestPartitionedCache producer = serviceWithProvider(new DeterministicSubjectProofProvider(sIssuer));
        TestPartitionedCache receiver = serviceWithProvider(new DeterministicSubjectProofProvider(sIssuer));
        TestStorage          storage  = new TestStorage(receiver, true);
        RequestContext       context  = contextWithSubject();

        producer.setIssuerId(sIssuer);
        receiver.setIssuerId(stableMemberId(member(9, 400L)));
        context.setSubjectProof(producer.getProvider().createProof(producer.payload(context, storage)));
        context.setSubjectProofSenderId(sIssuer);

        assertSame(context.getSubject(), receiver.getStorageAccessSubject(context, storage));
        }

    @Test
    public void shouldUseStableSenderIdentityInsteadOfMiniId()
            throws Exception
        {
        Member               memberOne = member(10, 500L);
        Member               memberTwo = member(10, 600L);
        TestPartitionedCache service   = enabledService();

        assertNotNull(service.senderId(memberOne));
        org.junit.Assert.assertNotEquals("member-" + memberOne.getId(), service.senderId(memberOne));
        org.junit.Assert.assertNotEquals(service.senderId(memberOne), service.senderId(memberTwo));
        }

    @Test
    public void shouldVerifyRemoteIssuerProofOnDifferentReceiver()
            throws Exception
        {
        setMode("prod");
        TestPartitionedCache issuer   = enabledService();
        TestPartitionedCache receiver = enabledService();
        TestStorage          storage  = new TestStorage(receiver, true);
        RequestContext       context  = contextWithSubject();

        receiver.setIssuerId("member-2");
        context.setSubjectProof(issuer.getProvider().createProof(issuer.payload(context, storage)));
        context.setSubjectProofSenderId(ISSUER);

        assertSame(context.getSubject(), receiver.getStorageAccessSubject(context, storage));
        }

    @Test
    public void shouldRejectProofReplayedByNonForwarder()
            throws Exception
        {
        setMode("prod");
        TestPartitionedCache service = enabledService();
        TestStorage          storage = new TestStorage(service, true);
        RequestContext       context = contextWithSubject();

        context.setSubjectProof(service.getProvider().createProof(service.payload(context, storage)));
        context.setSubjectProofSenderId("member-3");

        assertThrows(SecurityException.class, () -> service.getStorageAccessSubject(context, storage));
        }

    @Test
    public void shouldRejectTopologyInvalidForwarder()
            throws Exception
        {
        setMode("prod");
        TestPartitionedCache service = enabledService();
        TestStorage          storage = new TestStorage(service, true);
        RequestContext       context = contextWithSubject();

        context.setSubjectProof(service.getProvider().createProof(service.payload(context, storage)));
        context.setSubjectProofSenderId("member-4");
        service.setForwarderValid(false);

        assertThrows(SecurityException.class, () -> service.getStorageAccessSubject(context, storage));
        }

    @Test
    public void shouldAllowTopologyValidForwarder()
            throws Exception
        {
        setMode("prod");
        TestPartitionedCache service = enabledService();
        TestStorage          storage = new TestStorage(service, true);
        RequestContext       context = contextWithSubject();

        context.setSubjectProof(service.getProvider().createProof(service.payload(context, storage)));
        context.setSubjectProofSenderId("member-4");
        service.setForwarderValid(true);

        assertSame(context.getSubject(), service.getStorageAccessSubject(context, storage));
        }

    @Test
    public void shouldRejectClusterIdentityMismatch()
            throws Exception
        {
        setMode("prod");
        TestPartitionedCache issuer   = enabledService();
        TestPartitionedCache receiver = enabledService();
        TestStorage          storage  = new TestStorage(receiver, true);
        RequestContext       context  = contextWithSubject();

        issuer.setClusterName("cluster-a");
        receiver.setClusterName("cluster-b");
        context.setSubjectProof(issuer.getProvider().createProof(issuer.payload(context, storage)));
        context.setSubjectProofSenderId(ISSUER);

        assertThrows(SecurityException.class, () -> receiver.getStorageAccessSubject(context, storage));
        }

    @Test
    public void shouldRejectServiceTypeMismatch()
            throws Exception
        {
        setMode("prod");
        TestPartitionedCache issuer   = enabledService();
        TestPartitionedCache receiver = enabledService();
        TestStorage          storage  = new TestStorage(receiver, true);
        RequestContext       context  = contextWithSubject();

        receiver.setServiceType("OtherService");
        context.setSubjectProof(issuer.getProvider().createProof(issuer.payload(context, storage)));
        context.setSubjectProofSenderId(ISSUER);

        assertThrows(SecurityException.class, () -> receiver.getStorageAccessSubject(context, storage));
        }

    @Test
    public void shouldRejectReplayEpochMismatch()
            throws Exception
        {
        setMode("prod");
        TestPartitionedCache service = enabledService();
        TestStorage          storage = new TestStorage(service, true);
        RequestContext       context = contextWithSubject();

        context.setSubjectProof(service.getProvider().createProof(service.payload(context, storage)));
        context.setSubjectProofSenderId(ISSUER);
        context.setOldestPendingSUID(context.getOldestPendingSUID() + 1);

        assertThrows(SecurityException.class, () -> service.getStorageAccessSubject(context, storage));
        }

    private static RequestContext contextWithSubject()
        {
        Subject subject = new Subject();
        subject.getPrincipals().add(new PofPrincipal("CN=user"));
        subject.getPrincipals().add(new PofPrincipal("CN=admin"));
        subject.setReadOnly();

        RequestContext context = new RequestContext();
        context.setOldestPendingSUID(101L);
        context.setRequestSUID(REQUEST_SUID);
        context.setSubject(subject);
        return context;
        }

    private static RequestMessage requestWithSubject(TestPartitionedCache service)
            throws Exception
        {
        DistributedCacheRequest request = new DistributedCacheRequest();
        request.setCacheId(CACHE_ID);
        request.setService(service);
        request.setToMemberSet(SingleMemberSet.instantiate(member(2)));
        request.setRequestContext(contextWithSubject());
        return request;
        }

    private static Member member(int nId)
            throws Exception
        {
        return member(nId, System.currentTimeMillis());
        }

    private static Member member(int nId, long lTimestamp)
            throws Exception
        {
        Member member = new Member();
        member.configureDead(nId, new UUID(lTimestamp, InetAddress.getByName("127.0.0.1"),
                7574 + nId, nId), lTimestamp);
        return member;
        }

    private static String stableMemberId(Member member)
        {
        return "member-uuid:" + member.getUuid();
        }

    private static TestPartitionedCache enabledService()
        {
        return serviceWithProvider(new DeterministicSubjectProofProvider());
        }

    private static TestPartitionedCache serviceWithProvider(SubjectProofProvider provider)
        {
        TestPartitionedCache service = new TestPartitionedCache();
        service.setProvider(provider);
        return service;
        }

    private static void writeBodyAndExtensions(TestPartitionedCache service, RequestMessage request)
            throws Exception
        {
        ByteArrayWriteBuffer     buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput output = buffer.getBufferOutput();
        request.write(output);
        service.writeExtensions(request, output);
        }

    private static void setMode(String sMode)
            throws Exception
        {
        if (sMode == null)
            {
            System.clearProperty(CoherenceMode.PROP_COHERENCE_MODE);
            }
        else
            {
            System.setProperty(CoherenceMode.PROP_COHERENCE_MODE, sMode);
            }

        Method method = CoherenceMode.class.getDeclaredMethod("resetForTesting");
        method.setAccessible(true);
        method.invoke(null);
        }

    public static class TestPartitionedCache
            extends PartitionedCache
        {
        public TestPartitionedCache()
            {
            setSerializer(ExternalizableHelper.ensureSerializer(null));
            setServiceName("DistributedCache");
            }

        public SubjectProofProvider getProvider()
            {
            return m_provider;
            }

        public void setProvider(SubjectProofProvider provider)
            {
            m_provider = provider == null ? SubjectProofProviders.disabled() : provider;
            }

        public SubjectProofPayload payload(RequestContext context, Storage storage)
            {
            return createSubjectProofPayload(context, storage.getCacheName());
            }

        @Override
        public String getCacheName(long lCacheId)
            {
            return lCacheId == CACHE_ID ? CACHE_NAME : super.getCacheName(lCacheId);
            }

        public void writeExtensions(RequestMessage request, WriteBuffer.BufferOutput output)
                throws Exception
            {
            writeRequestExtensions(request, output);
            }

        public String senderId(Member member)
            {
            return getSubjectProofSenderId(member);
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
            return m_sIssuerId;
            }

        @Override
        protected String getSubjectProofSubjectSource()
            {
            return SOURCE;
            }

        @Override
        protected String getSubjectProofClusterName()
            {
            return m_sClusterName;
            }

        @Override
        protected String getSubjectProofServiceType()
            {
            return m_sServiceType;
            }

        @Override
        protected boolean isSubjectProofForwarderValid(SubjectProofPayload payload, RequestContext context,
                Storage storage, String sSenderId)
            {
            return m_fForwarderValid;
            }

        @Override
        protected long getSubjectProofNonce(RequestMessage msg, RequestContext ctx)
            {
            return m_lNonce;
            }

        @Override
        protected long getSubjectProofIssuedAtMillis(RequestMessage msg, RequestContext ctx)
            {
            return m_lIssuedAtMillis;
            }

        @Override
        protected long getSubjectProofExpiresAtMillis(RequestMessage msg, RequestContext ctx, long lIssuedAtMillis)
            {
            return m_lExpiresAtMillis;
            }

        @Override
        protected boolean isSubjectProofRequired(RequestMessage msg)
            {
            return m_fProofRequired;
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

        public void setProofRequired(boolean fRequired)
            {
            m_fProofRequired = fRequired;
            }

        public void setRecipientsCompatible(boolean fCompatible)
            {
            m_fRecipientsCompatible = fCompatible;
            }

        public void setClusterName(String sClusterName)
            {
            m_sClusterName = sClusterName;
            }

        public void setForwarderValid(boolean fForwarderValid)
            {
            m_fForwarderValid = fForwarderValid;
            }

        public void setIssuerId(String sIssuerId)
            {
            m_sIssuerId = sIssuerId;
            }

        public void setServiceType(String sServiceType)
            {
            m_sServiceType = sServiceType;
            }

        public void setProofMetadata(long lNonce, long lIssuedAtMillis, long lExpiresAtMillis)
            {
            m_lNonce           = lNonce;
            m_lIssuedAtMillis  = lIssuedAtMillis;
            m_lExpiresAtMillis = lExpiresAtMillis;
            }

        private SubjectProofProvider m_provider = SubjectProofProviders.disabled();
        private boolean              m_fProofRequired;
        private boolean              m_fRecipientsCompatible = true;
        private boolean              m_fForwarderValid;
        private String               m_sClusterName = CLUSTER_NAME;
        private String               m_sServiceType = SERVICE_TYPE;
        private String               m_sIssuerId = ISSUER;
        private long                 m_lNonce = NONCE;
        private long                 m_lIssuedAtMillis = ISSUED_AT;
        private long                 m_lExpiresAtMillis = EXPIRES_AT;
        }

    public static class TestStorage
            extends Storage
        {
        public TestStorage(TestPartitionedCache service, boolean fProofRequired)
            {
            super(null, service, false);
            m_service = service;
            setCacheName(CACHE_NAME);
            setSubjectProofRequired(fProofRequired);
            }

        @Override
        public PartitionedCache getService()
            {
            return m_service;
            }

        private final TestPartitionedCache m_service;
        }

    private static class DeterministicSubjectProofProvider
            implements SubjectProofProvider
        {
        private DeterministicSubjectProofProvider(String... asIssuer)
            {
            m_setIssuer = new HashSet<>(Arrays.asList(asIssuer.length == 0 ? new String[] {ISSUER} : asIssuer));
            }

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
                return SubjectProofVerification.failed(MALFORMED, "malformed");
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
            if (!m_setIssuer.contains(payload.getIssuerId()) || !SOURCE.equals(payload.getSubjectSource()))
                {
                return SubjectProofVerification.failed(UNKNOWN_KEY, payload.getIssuerId());
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

        private final Set<String> m_setIssuer;
        }

    private static final String ALGORITHM = "test-hmac-sha256";
    private static final String KEY_ID    = "test-key";
    private static final String ISSUER    = "member-1";
    private static final String SOURCE    = "extend";
    private static final String CLUSTER_NAME = "test-cluster";
    private static final String SERVICE_TYPE = "DistributedCache";
    private static final String CACHE_NAME = "orders";

    private static final long REQUEST_SUID = 142L;
    private static final long CACHE_ID     = 123L;
    private static final long NONCE        = 99L;
    private static final long ISSUED_AT    = 1_000L;
    private static final long EXPIRES_AT   = Long.MAX_VALUE - 1;

    private static final byte[] SECRET = "deterministic-secret".getBytes(StandardCharsets.UTF_8);

    private final String m_sOriginalMode = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
    }
