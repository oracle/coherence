/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.util.Arrays;

import static com.tangosol.internal.net.security.SeniorMetadataProofVerification.Status.EXPIRED;
import static com.tangosol.internal.net.security.SeniorMetadataProofVerification.Status.MISSING;
import static com.tangosol.internal.net.security.SeniorMetadataProofVerification.Status.PAYLOAD_MISMATCH;
import static com.tangosol.internal.net.security.SeniorMetadataProofVerification.Status.PROOF_MISMATCH;
import static com.tangosol.internal.net.security.SeniorMetadataProofVerification.Status.UNKNOWN_KEY;
import static com.tangosol.internal.net.security.SeniorMetadataProofVerification.Status.VALID;
import static com.tangosol.internal.net.security.SeniorMetadataProofVerification.Status.WRONG_ALGORITHM;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the PEER-01 Slice D2 senior-metadata proof foundation.
 *
 * @author Aleks Seovic  2026.05.19
 * @since 26.07
 */
public class SeniorMetadataProofProviderTest
    {
    @Test
    public void shouldKeepDisabledProviderBehaviorNeutral()
        {
        SeniorMetadataProofProvider provider = SeniorMetadataProofProviders.disabled();

        assertFalse(provider.isEnabled());
        assertNull(provider.createProof(payload()));
        assertEquals(SeniorMetadataProofVerification.Status.DISABLED,
                provider.verifyProof(null, payload(), NOW).getStatus());
        }

    @Test
    public void shouldProduceStableProofForSameCanonicalPayload()
        {
        SeniorMetadataProofProvider provider = provider();
        SeniorMetadataProofPayload  payload  = payload();

        assertArrayEquals(provider.createProof(payload), provider.createProof(payload));
        }

    @Test
    public void shouldRoundTripPayload()
            throws Exception
        {
        SeniorMetadataProofPayload payload = payload();
        SeniorMetadataProofPayload decoded = SeniorMetadataProofPayload.fromByteArray(payload.toByteArray());

        assertEquals(payload, decoded);
        assertEquals(SeniorMetadataProofPayload.PAYLOAD_VERSION, decoded.getPayloadVersion());
        assertEquals(SeniorMetadataProofPayload.MESSAGE_KIND_KILL_TOKEN, decoded.getMessageKind());
        assertEquals(ALGORITHM, decoded.getAlgorithmId());
        assertEquals(KEY_ID, decoded.getKeyId());
        assertEquals(ISSUER, decoded.getIssuerId());
        assertEquals(CLUSTER_NAME, decoded.getClusterName());
        assertEquals(SERVICE_NAME, decoded.getServiceName());
        assertEquals(SERVICE_ID, decoded.getServiceId());
        assertEquals(MESSAGE_TYPE, decoded.getMessageType());
        assertEquals(SENDER_ID, decoded.getSenderId());
        assertEquals(SENIOR_ID, decoded.getSeniorId());
        assertEquals(TARGET_ID, decoded.getTargetId());
        assertEquals(CULPRIT_ID, decoded.getCulpritId());
        assertEquals(SeniorMetadataProofPayload.KILL_DIRECTION_SENIOR_TO_JUNIOR, decoded.getKillDirection());
        assertTrue(decoded.isZombie());
        assertEquals(SENIOR_EPOCH, decoded.getSeniorEpoch());
        assertEquals(LAST_JOIN_TIME, decoded.getLastJoinTime());
        assertEquals(DIGEST_VERSION, decoded.getMemberSetDigestVersion());
        assertEquals(MEMBER_COUNT, decoded.getMemberSetCount());
        assertArrayEquals(MEMBER_SET_DIGEST, decoded.getMemberSetDigest());
        assertEquals(REPLAY_EPOCH, decoded.getReplayEpoch());
        assertEquals(NONCE, decoded.getNonce());
        assertEquals(ISSUED_AT, decoded.getIssuedAtMillis());
        assertEquals(EXPIRES_AT, decoded.getExpiresAtMillis());
        }

    @Test
    public void shouldRoundTripProof()
            throws Exception
        {
        byte[] abProof = provider().createProof(payload());
        SeniorMetadataProof proof = SeniorMetadataProof.fromByteArray(abProof);

        assertEquals(payload(), proof.getPayload());
        assertTrue(proof.getProofBytes().length > 0);
        }

    @Test
    public void shouldAlignSeniorMetadataProofMaximumsWithCarrierPayloadLimit()
        {
        assertEquals(SeniorMetadataProof.MAX_ENCODED_BYTES,
                SeniorMetadataProof.FIXED_ENCODED_LENGTH
                        + SeniorMetadataProof.MAX_PAYLOAD_BYTES
                        + SeniorMetadataProof.MAX_PROOF_BYTES);
        }

    @Test
    public void shouldKeepMaximumProofBytesWithinCarrierPayloadLimit()
            throws Exception
        {
        byte[] abProof = new SeniorMetadataProof(payload(), new byte[SeniorMetadataProof.MAX_PROOF_BYTES])
                .toByteArray();

        assertTrue(abProof.length <= SeniorMetadataProof.MAX_ENCODED_BYTES);
        assertEquals(SeniorMetadataProof.MAX_PROOF_BYTES,
                SeniorMetadataProof.fromByteArray(abProof).getProofBytes().length);
        }

    @Test
    public void shouldRejectEncodedProofLargerThanCarrierPayload()
            throws Exception
        {
        assertFailure(() -> SeniorMetadataProof.fromByteArray(new byte[SeniorMetadataProof.MAX_ENCODED_BYTES + 1]),
                "oversized senior-metadata proof encoded length");
        }

    @Test
    public void shouldRejectEncodedPayloadLargerThanProofPayloadLimit()
            throws Exception
        {
        assertFailure(() -> SeniorMetadataProofPayload.fromByteArray(
                new byte[SeniorMetadataProofPayload.MAX_ENCODED_BYTES + 1]),
                "oversized senior-metadata payload length");
        }

    @Test
    public void shouldRejectOversizedProofPayloadLength()
            throws Exception
        {
        assertFailure(() -> SeniorMetadataProof.fromByteArray(proofWithLength(
                SeniorMetadataProof.MAX_PAYLOAD_BYTES + 1, 0)),
                "oversized senior-metadata proof payload length");
        }

    @Test
    public void shouldRejectOversizedProofBytes()
            throws Exception
        {
        assertFailure(() -> SeniorMetadataProof.fromByteArray(proofWithPayloadAndProofLength(
                payload().toByteArray(), SeniorMetadataProof.MAX_PROOF_BYTES + 1)),
                "oversized senior-metadata proof proof length");
        }

    @Test
    public void shouldRejectOversizedPayloadStringBeforeAllocation()
            throws Exception
        {
        assertFailure(() -> SeniorMetadataProofPayload.fromByteArray(payloadWithOversizedAlgorithm()),
                "oversized senior-metadata algorithm string");
        }

    @Test
    public void shouldRejectOversizedDigestBytesBeforeAllocation()
            throws Exception
        {
        assertFailure(() -> SeniorMetadataProofPayload.fromByteArray(
                payloadWithDigestLength(SeniorMetadataProofPayload.MAX_DIGEST_BYTES + 1, false)),
                "oversized senior-metadata member-set digest length");
        }

    @Test
    public void shouldRejectNegativeProofPayloadLength()
            throws Exception
        {
        assertFailure(() -> SeniorMetadataProof.fromByteArray(proofWithLength(-1, 0)),
                "negative senior-metadata proof payload length");
        }

    @Test
    public void shouldRejectNegativeProofBytesLength()
            throws Exception
        {
        assertFailure(() -> SeniorMetadataProof.fromByteArray(proofWithPayloadAndProofLength(
                payload().toByteArray(), -1)),
                "negative senior-metadata proof proof length");
        }

    @Test
    public void shouldRejectNegativeDigestLength()
            throws Exception
        {
        assertFailure(() -> SeniorMetadataProofPayload.fromByteArray(payloadWithDigestLength(-1, false)),
                "negative senior-metadata member-set digest length");
        }

    @Test
    public void shouldRejectTruncatedProofPayload()
            throws Exception
        {
        assertFailure(() -> SeniorMetadataProof.fromByteArray(proofWithLength(payload().toByteArray().length, 0)),
                "truncated senior-metadata proof payload");
        }

    @Test
    public void shouldRejectTruncatedProofBytes()
            throws Exception
        {
        assertFailure(() -> SeniorMetadataProof.fromByteArray(proofWithPayloadAndProofLength(
                payload().toByteArray(), 1)),
                "truncated senior-metadata proof proof");
        }

    @Test
    public void shouldRejectTruncatedPayloadString()
            throws Exception
        {
        assertFailure(() -> SeniorMetadataProofPayload.fromByteArray(payloadWithTruncatedAlgorithm()),
                "truncated senior-metadata algorithm string");
        }

    @Test
    public void shouldRejectTruncatedDigestBytes()
            throws Exception
        {
        assertFailure(() -> SeniorMetadataProofPayload.fromByteArray(payloadWithDigestLength(4, true)),
                "truncated senior-metadata member-set digest");
        }

    @Test
    public void shouldRejectTrailingPayloadBytes()
            throws Exception
        {
        byte[] abPayload = payload().toByteArray();
        byte[] abTrailing = Arrays.copyOf(abPayload, abPayload.length + 1);

        assertFailure(() -> SeniorMetadataProofPayload.fromByteArray(abTrailing),
                "trailing senior-metadata payload bytes");
        }

    @Test
    public void shouldVerifyValidProof()
        {
        SeniorMetadataProofProvider provider = provider();
        SeniorMetadataProofPayload  payload  = payload();

        SeniorMetadataProofVerification result = provider.verifyProof(provider.createProof(payload), payload, NOW);

        assertEquals(VALID, result.getStatus());
        assertTrue(result.isValid());
        }

    @Test
    public void shouldRejectMissingProof()
        {
        SeniorMetadataProofVerification result = provider().verifyProof(null, payload(), NOW);

        assertEquals(MISSING, result.getStatus());
        }

    @Test
    public void shouldRejectUnknownKeyId()
        {
        SeniorMetadataProofProvider provider = provider();
        SeniorMetadataProofPayload  payload  = payload(KEY_ID + "-other", ALGORITHM, EXPIRES_AT);

        SeniorMetadataProofVerification result = provider.verifyProof(provider.createProof(payload), payload, NOW);

        assertEquals(UNKNOWN_KEY, result.getStatus());
        }

    @Test
    public void shouldRejectWrongAlgorithm()
        {
        SeniorMetadataProofProvider provider = provider();
        SeniorMetadataProofPayload  payload  = payload(KEY_ID, "other-algorithm", EXPIRES_AT);

        SeniorMetadataProofVerification result = provider.verifyProof(provider.createProof(payload), payload, NOW);

        assertEquals(WRONG_ALGORITHM, result.getStatus());
        }

    @Test
    public void shouldRejectExpiredProof()
        {
        SeniorMetadataProofProvider provider = provider();
        SeniorMetadataProofPayload  payload  = payload(KEY_ID, ALGORITHM, NOW - 1);

        SeniorMetadataProofVerification result = provider.verifyProof(provider.createProof(payload), payload, NOW);

        assertEquals(EXPIRED, result.getStatus());
        }

    @Test
    public void shouldRejectPayloadMismatch()
        {
        SeniorMetadataProofProvider provider = provider();
        SeniorMetadataProofPayload  payload  = payload();
        SeniorMetadataProofPayload  expected = payload(KEY_ID, ALGORITHM, EXPIRES_AT, "different-sender");

        SeniorMetadataProofVerification result = provider.verifyProof(provider.createProof(payload), expected, NOW);

        assertEquals(PAYLOAD_MISMATCH, result.getStatus());
        }

    @Test
    public void shouldRejectProofMismatch()
        {
        SeniorMetadataProofProvider provider = provider();
        SeniorMetadataProofPayload  payload  = payload();
        byte[]                      abProof  = provider.createProof(payload);
        abProof[abProof.length - 1] ^= 0x01;

        SeniorMetadataProofVerification result = provider.verifyProof(abProof, payload, NOW);

        assertEquals(PROOF_MISMATCH, result.getStatus());
        }

    private static SeniorMetadataProofProvider provider()
        {
        return SeniorMetadataProofProviders.deterministicTestProvider(ALGORITHM, KEY_ID, SECRET);
        }

    private static SeniorMetadataProofPayload payload()
        {
        return payload(KEY_ID, ALGORITHM, EXPIRES_AT);
        }

    private static SeniorMetadataProofPayload payload(String sKeyId, String sAlgorithm, long lExpiresAt)
        {
        return payload(sKeyId, sAlgorithm, lExpiresAt, SENDER_ID);
        }

    private static SeniorMetadataProofPayload payload(String sKeyId, String sAlgorithm, long lExpiresAt,
            String sSenderId)
        {
        return new SeniorMetadataProofPayload(SeniorMetadataProofPayload.PAYLOAD_VERSION,
                SeniorMetadataProofPayload.MESSAGE_KIND_KILL_TOKEN, sAlgorithm, sKeyId, ISSUER, CLUSTER_NAME,
                SERVICE_NAME, SERVICE_ID, MESSAGE_TYPE, sSenderId, SENIOR_ID, TARGET_ID, CULPRIT_ID,
                SeniorMetadataProofPayload.KILL_DIRECTION_SENIOR_TO_JUNIOR, true, SENIOR_EPOCH, LAST_JOIN_TIME,
                DIGEST_VERSION, MEMBER_COUNT, MEMBER_SET_DIGEST, REPLAY_EPOCH, NONCE, ISSUED_AT, lExpiresAt);
        }

    private static byte[] proofWithLength(int cbPayload, int cbProof)
            throws IOException
        {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        DataOutputStream      out      = new DataOutputStream(outBytes);
        out.writeInt(SENIOR_METADATA_PROOF_MAGIC);
        out.writeInt(SeniorMetadataProof.FORMAT_VERSION);
        out.writeInt(cbPayload);
        out.writeInt(cbProof);
        out.flush();
        return outBytes.toByteArray();
        }

    private static byte[] proofWithPayloadAndProofLength(byte[] abPayload, int cbProof)
            throws IOException
        {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        DataOutputStream      out      = new DataOutputStream(outBytes);
        out.writeInt(SENIOR_METADATA_PROOF_MAGIC);
        out.writeInt(SeniorMetadataProof.FORMAT_VERSION);
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
        out.writeInt(SeniorMetadataProofPayload.PAYLOAD_VERSION);
        out.writeByte(SeniorMetadataProofPayload.MESSAGE_KIND_HEARTBEAT);
        out.writeUTF(repeated('a', SeniorMetadataProofPayload.MAX_UTF_BYTES + 1));
        out.flush();
        return outBytes.toByteArray();
        }

    private static byte[] payloadWithTruncatedAlgorithm()
            throws IOException
        {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        DataOutputStream      out      = new DataOutputStream(outBytes);
        out.writeInt(SeniorMetadataProofPayload.PAYLOAD_VERSION);
        out.writeByte(SeniorMetadataProofPayload.MESSAGE_KIND_HEARTBEAT);
        out.writeShort(4);
        out.writeByte('a');
        out.flush();
        return outBytes.toByteArray();
        }

    private static byte[] payloadWithDigestLength(int cbDigest, boolean fTruncate)
            throws IOException
        {
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        DataOutputStream      out      = new DataOutputStream(outBytes);
        out.writeInt(SeniorMetadataProofPayload.PAYLOAD_VERSION);
        out.writeByte(SeniorMetadataProofPayload.MESSAGE_KIND_HEARTBEAT);
        out.writeUTF(ALGORITHM);
        out.writeUTF(KEY_ID);
        out.writeUTF(ISSUER);
        out.writeUTF(CLUSTER_NAME);
        out.writeUTF(SERVICE_NAME);
        out.writeInt(SERVICE_ID);
        out.writeInt(MESSAGE_TYPE);
        out.writeUTF(SENDER_ID);
        out.writeUTF(SENIOR_ID);
        out.writeUTF(TARGET_ID);
        out.writeUTF(CULPRIT_ID);
        out.writeByte(SeniorMetadataProofPayload.KILL_DIRECTION_NONE);
        out.writeBoolean(false);
        out.writeLong(SENIOR_EPOCH);
        out.writeLong(LAST_JOIN_TIME);
        out.writeInt(DIGEST_VERSION);
        out.writeInt(MEMBER_COUNT);
        out.writeInt(cbDigest);
        if (!fTruncate && cbDigest > 0 && cbDigest <= SeniorMetadataProofPayload.MAX_DIGEST_BYTES)
            {
            out.write(new byte[cbDigest]);
            }
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

    private static final String ALGORITHM    = "test-hmac-sha256";
    private static final String KEY_ID       = "test-key";
    private static final String ISSUER       = "member-issuer-uid";
    private static final String CLUSTER_NAME = "test-cluster";
    private static final String SERVICE_NAME = "Cluster";
    private static final String SENDER_ID    = "sender-uid";
    private static final String SENIOR_ID    = "senior-uid";
    private static final String TARGET_ID    = "target-uid";
    private static final String CULPRIT_ID   = "culprit-uid";

    private static final int SERVICE_ID     = 0;
    private static final int MESSAGE_TYPE   = 40;
    private static final int DIGEST_VERSION = 1;
    private static final int MEMBER_COUNT   = 3;

    private static final long SENIOR_EPOCH   = 12L;
    private static final long LAST_JOIN_TIME = 34L;
    private static final long REPLAY_EPOCH   = 56L;
    private static final long NONCE          = 78L;
    private static final long ISSUED_AT      = 1_000L;
    private static final long EXPIRES_AT     = 2_000L;
    private static final long NOW            = 1_500L;

    private static final int SENIOR_METADATA_PROOF_MAGIC = 0x534d4631;

    private static final byte[] MEMBER_SET_DIGEST = new byte[] {1, 2, 3, 4};
    private static final byte[] SECRET            = "deterministic-secret".getBytes(StandardCharsets.UTF_8);
    }
