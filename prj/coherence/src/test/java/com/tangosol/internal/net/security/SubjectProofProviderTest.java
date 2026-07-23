/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

import com.tangosol.io.pof.PofPrincipal;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.Principal;

import java.util.Arrays;

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the PEER-01 Slice E subject-proof provider foundation.
 *
 * @author Aleks Seovic  2026.05.18
 * @since 15.1.2.0
 */
public class SubjectProofProviderTest
    {
    @Test
    public void shouldKeepDisabledProviderBehaviorNeutral()
        {
        SubjectProofProvider provider = SubjectProofProviders.disabled();

        assertFalse(provider.isEnabled());
        assertNull(provider.createProof(payload()));
        assertEquals(SubjectProofVerification.Status.DISABLED,
                provider.verifyProof(null, payload(), NOW).getStatus());
        }

    @Test
    public void shouldProduceStableProofForSameCanonicalPayload()
        {
        DeterministicSubjectProofProvider provider = new DeterministicSubjectProofProvider();
        SubjectProofPayload payload = payload();

        assertArrayEquals(provider.createProof(payload), provider.createProof(payload));
        }

    @Test
    public void shouldRoundTripSubjectProofFormat()
            throws Exception
        {
        DeterministicSubjectProofProvider provider = new DeterministicSubjectProofProvider();
        byte[] abProof = provider.createProof(payload());

        SubjectProof proof = SubjectProof.fromByteArray(abProof);

        assertEquals(ALGORITHM, proof.getPayload().getAlgorithmId());
        assertEquals(KEY_ID, proof.getPayload().getKeyId());
        assertEquals(CLUSTER_NAME, proof.getPayload().getClusterName());
        assertEquals(SERVICE_TYPE, proof.getPayload().getServiceType());
        assertEquals(Arrays.asList("CN=admin", "CN=user"), proof.getPayload().getPrincipalNames());
        assertTrue(proof.getProofBytes().length > 0);
        }

    @Test
    public void shouldAlignSubjectProofMaximumsWithCarrierPayloadLimit()
        {
        assertEquals(SubjectProof.MAX_ENCODED_BYTES,
                SubjectProof.FIXED_ENCODED_LENGTH + SubjectProof.MAX_PAYLOAD_BYTES + SubjectProof.MAX_PROOF_BYTES);
        }

    @Test
    public void shouldKeepMaximumProofBytesWithinCarrierPayloadLimit()
            throws Exception
        {
        byte[] abProof = new SubjectProof(payload(), new byte[SubjectProof.MAX_PROOF_BYTES]).toByteArray();

        assertTrue(abProof.length <= SubjectProof.MAX_ENCODED_BYTES);
        assertEquals(SubjectProof.MAX_PROOF_BYTES, SubjectProof.fromByteArray(abProof).getProofBytes().length);
        }

    @Test
    public void shouldVerifyValidProof()
        {
        DeterministicSubjectProofProvider provider = new DeterministicSubjectProofProvider();
        SubjectProofPayload payload = payload();

        SubjectProofVerification result = provider.verifyProof(provider.createProof(payload), payload, NOW);

        assertEquals(VALID, result.getStatus());
        assertTrue(result.isValid());
        }

    @Test
    public void shouldRejectMissingProof()
        {
        SubjectProofVerification result = new DeterministicSubjectProofProvider().verifyProof(null, payload(), NOW);

        assertEquals(MISSING, result.getStatus());
        }

    @Test
    public void shouldRejectUnknownKeyId()
        {
        DeterministicSubjectProofProvider provider = new DeterministicSubjectProofProvider();
        SubjectProofPayload payload = payload(KEY_ID + "-other", ALGORITHM, EXPIRES_AT, "CN=admin", "CN=user");

        SubjectProofVerification result = provider.verifyProof(provider.createProof(payload), payload, NOW);

        assertEquals(UNKNOWN_KEY, result.getStatus());
        }

    @Test
    public void shouldRejectWrongAlgorithm()
        {
        DeterministicSubjectProofProvider provider = new DeterministicSubjectProofProvider();
        SubjectProofPayload payload = payload(KEY_ID, "other-algorithm", EXPIRES_AT, "CN=admin", "CN=user");

        SubjectProofVerification result = provider.verifyProof(provider.createProof(payload), payload, NOW);

        assertEquals(WRONG_ALGORITHM, result.getStatus());
        }

    @Test
    public void shouldRejectExpiredProof()
        {
        DeterministicSubjectProofProvider provider = new DeterministicSubjectProofProvider();
        SubjectProofPayload payload = payload(KEY_ID, ALGORITHM, NOW - 1, "CN=admin", "CN=user");

        SubjectProofVerification result = provider.verifyProof(provider.createProof(payload), payload, NOW);

        assertEquals(EXPIRED, result.getStatus());
        }

    @Test
    public void shouldRejectPayloadMismatch()
        {
        DeterministicSubjectProofProvider provider = new DeterministicSubjectProofProvider();
        SubjectProofPayload payload = payload();
        SubjectProofPayload expected = new SubjectProofPayload(SubjectProofPayload.PROOF_VERSION, ALGORITHM,
                KEY_ID, ISSUER, SOURCE, CLUSTER_NAME, SERVICE_TYPE, "DistributedCache", "other-cache",
                REQUEST_SUID, REPLAY_EPOCH, NONCE, ISSUED_AT, EXPIRES_AT, Arrays.asList("CN=admin", "CN=user"));

        SubjectProofVerification result = provider.verifyProof(provider.createProof(payload), expected, NOW);

        assertEquals(PAYLOAD_MISMATCH, result.getStatus());
        }

    @Test
    public void shouldRejectCanonicalPrincipalMismatch()
        {
        DeterministicSubjectProofProvider provider = new DeterministicSubjectProofProvider();
        SubjectProofPayload payload = payload();
        SubjectProofPayload expected = payload(KEY_ID, ALGORITHM, EXPIRES_AT, "CN=other");

        SubjectProofVerification result = provider.verifyProof(provider.createProof(payload), expected, NOW);

        assertEquals(PRINCIPAL_MISMATCH, result.getStatus());
        }

    @Test
    public void shouldRejectPayloadSignatureMismatch()
        {
        DeterministicSubjectProofProvider provider = new DeterministicSubjectProofProvider();
        SubjectProofPayload payload = payload();
        byte[] abProof = provider.createProof(payload);
        abProof[abProof.length - 1] ^= 0x01;

        SubjectProofVerification result = provider.verifyProof(abProof, payload, NOW);

        assertEquals(PROOF_MISMATCH, result.getStatus());
        }

    @Test
    public void shouldRejectOversizedSubjectProofPayloadLength()
            throws Exception
        {
        assertFailure(() -> SubjectProof.fromByteArray(subjectProofWithLength(SubjectProof.MAX_PAYLOAD_BYTES + 1,
                0)), "oversized subject-proof payload length");
        }

    @Test
    public void shouldRejectOversizedSubjectProofProofLength()
            throws Exception
        {
        byte[] abPayload = payload().toByteArray();

        assertFailure(() -> SubjectProof.fromByteArray(subjectProofWithPayloadAndProofLength(abPayload,
                SubjectProof.MAX_PROOF_BYTES + 1)), "oversized subject-proof proof length");
        }

    @Test
    public void shouldRejectEncodedSubjectProofLargerThanCarrierPayload()
            throws Exception
        {
        assertFailure(() -> SubjectProof.fromByteArray(new byte[SubjectProof.MAX_ENCODED_BYTES + 1]),
                "oversized subject-proof encoded length");
        }

    @Test
    public void shouldRejectOversizedSubjectProofPayloadString()
            throws Exception
        {
        assertFailure(() -> SubjectProofPayload.fromByteArray(payloadWithOversizedAlgorithm()),
                "oversized subject-proof algorithm string");
        }

    @Test
    public void shouldRejectOversizedSubjectProofPrincipalCount()
            throws Exception
        {
        assertFailure(() -> SubjectProofPayload.fromByteArray(payloadWithPrincipalCount(
                SubjectProofPayload.MAX_PRINCIPAL_COUNT + 1)), "oversized subject-proof principal count");
        }

    @Test
    public void shouldRejectNegativeSubjectProofPrincipalCount()
            throws Exception
        {
        assertFailure(() -> SubjectProofPayload.fromByteArray(payloadWithPrincipalCount(-1)),
                "negative subject-proof principal count");
        }

    @Test
    public void shouldCanonicalizeSubjectPrincipals()
        {
        Subject subject = new Subject();
        subject.getPrincipals().add(new PofPrincipal("CN=user"));
        subject.getPrincipals().add(new PofPrincipal("CN=admin"));
        subject.getPrincipals().add(new Principal()
            {
            @Override
            public String getName()
                {
                return "CN=user";
                }
            });

        assertEquals(Arrays.asList("CN=admin", "CN=user"), SubjectProofPayload.canonicalPrincipalNames(subject));
        }

    private static SubjectProofPayload payload()
        {
        return payload(KEY_ID, ALGORITHM, EXPIRES_AT, "CN=admin", "CN=user");
        }

    private static SubjectProofPayload payload(String sKeyId, String sAlgorithm, long lExpiresAt,
            String... asPrincipal)
        {
        return new SubjectProofPayload(SubjectProofPayload.PROOF_VERSION, sAlgorithm, sKeyId, ISSUER, SOURCE,
                CLUSTER_NAME, SERVICE_TYPE, "DistributedCache", "test-cache", REQUEST_SUID, REPLAY_EPOCH, NONCE,
                ISSUED_AT, lExpiresAt,
                Arrays.asList(asPrincipal));
        }

    private static byte[] subjectProofWithLength(int cbPayload, int cbProof)
            throws IOException
        {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        DataOutputStream      out      = new DataOutputStream(outBytes);
        out.writeInt(SUBJECT_PROOF_MAGIC);
        out.writeInt(SubjectProof.FORMAT_VERSION);
        out.writeInt(cbPayload);
        out.writeInt(cbProof);
        out.flush();
        return outBytes.toByteArray();
        }

    private static byte[] subjectProofWithPayloadAndProofLength(byte[] abPayload, int cbProof)
            throws IOException
        {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        DataOutputStream      out      = new DataOutputStream(outBytes);
        out.writeInt(SUBJECT_PROOF_MAGIC);
        out.writeInt(SubjectProof.FORMAT_VERSION);
        out.writeInt(abPayload.length);
        out.write(abPayload);
        out.writeInt(cbProof);
        out.flush();
        return outBytes.toByteArray();
        }

    private static byte[] payloadWithOversizedAlgorithm()
            throws IOException
        {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        DataOutputStream      out      = new DataOutputStream(outBytes);
        out.writeInt(SubjectProofPayload.PROOF_VERSION);
        out.writeUTF(repeated('a', SubjectProofPayload.MAX_UTF_BYTES + 1));
        out.flush();
        return outBytes.toByteArray();
        }

    private static byte[] payloadWithPrincipalCount(int cNames)
            throws IOException
        {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        DataOutputStream      out      = new DataOutputStream(outBytes);
        out.writeInt(SubjectProofPayload.PROOF_VERSION);
        out.writeUTF(ALGORITHM);
        out.writeUTF(KEY_ID);
        out.writeUTF(ISSUER);
        out.writeUTF(SOURCE);
        out.writeUTF(CLUSTER_NAME);
        out.writeUTF(SERVICE_TYPE);
        out.writeUTF("DistributedCache");
        out.writeUTF("test-cache");
        out.writeLong(REQUEST_SUID);
        out.writeLong(REPLAY_EPOCH);
        out.writeLong(NONCE);
        out.writeLong(ISSUED_AT);
        out.writeLong(EXPIRES_AT);
        out.writeInt(cNames);
        out.flush();
        return outBytes.toByteArray();
        }

    private static String repeated(char ch, int cch)
        {
        char[] ach = new char[cch];
        Arrays.fill(ach, ch);
        return new String(ach);
        }

    private static void assertFailure(ThrowingRunnable runnable, String sMessage)
            throws Exception
        {
        try
            {
            runnable.run();
            }
        catch (IOException e)
            {
            assertTrue(e.getMessage().contains(sMessage));
            return;
            }
        throw new AssertionError("expected " + sMessage);
        }

    private interface ThrowingRunnable
        {
        void run()
                throws Exception;
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
    private static final String CLUSTER_NAME = "test-cluster";
    private static final String SERVICE_TYPE = "DistributedCache";

    private static final long REQUEST_SUID = 42L;
    private static final long REPLAY_EPOCH = 7L;
    private static final long NONCE        = 99L;
    private static final long ISSUED_AT    = 1_000L;
    private static final long EXPIRES_AT   = 2_000L;
    private static final long NOW          = 1_500L;

    private static final int SUBJECT_PROOF_MAGIC = 0x53504631;

    private static final byte[] SECRET = "deterministic-secret".getBytes(StandardCharsets.UTF_8);
    }
