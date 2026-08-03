/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;

import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static com.tangosol.internal.net.security.SeniorMetadataProofVerification.Status.EXPIRED;
import static com.tangosol.internal.net.security.SeniorMetadataProofVerification.Status.MALFORMED;
import static com.tangosol.internal.net.security.SeniorMetadataProofVerification.Status.MISSING;
import static com.tangosol.internal.net.security.SeniorMetadataProofVerification.Status.PAYLOAD_MISMATCH;
import static com.tangosol.internal.net.security.SeniorMetadataProofVerification.Status.PROOF_MISMATCH;
import static com.tangosol.internal.net.security.SeniorMetadataProofVerification.Status.UNKNOWN_KEY;
import static com.tangosol.internal.net.security.SeniorMetadataProofVerification.Status.WRONG_ALGORITHM;

/**
 * Senior-metadata proof provider factories.
 *
 * @author Aleks Seovic  2026.05.19
 * @since 26.07
 */
public final class SeniorMetadataProofProviders
    {
    /**
     * Return the behavior-neutral disabled provider.
     *
     * @return the disabled provider
     */
    public static SeniorMetadataProofProvider disabled()
        {
        return DISABLED;
        }

    /**
     * Return a deterministic HMAC provider for tests.
     *
     * @param sAlgorithmId  expected payload algorithm id
     * @param sKeyId        expected payload key id
     * @param abSecret      HMAC secret bytes
     *
     * @return deterministic test provider
     */
    public static SeniorMetadataProofProvider deterministicTestProvider(String sAlgorithmId, String sKeyId,
            byte[] abSecret)
        {
        return new DeterministicProvider(sAlgorithmId, sKeyId, abSecret);
        }

    private SeniorMetadataProofProviders()
        {
        }

    private static class DeterministicProvider
            implements SeniorMetadataProofProvider
        {
        DeterministicProvider(String sAlgorithmId, String sKeyId, byte[] abSecret)
            {
            f_sAlgorithmId = sAlgorithmId == null ? "" : sAlgorithmId;
            f_sKeyId       = sKeyId == null ? "" : sKeyId;
            f_abSecret     = abSecret == null || abSecret.length == 0
                    ? "senior-metadata-proof-test-secret".getBytes(StandardCharsets.UTF_8)
                    : Arrays.copyOf(abSecret, abSecret.length);
            }

        @Override
        public byte[] createProof(SeniorMetadataProofPayload payload)
            {
            return payload == null ? null : new SeniorMetadataProof(payload, mac(payload)).toByteArray();
            }

        @Override
        public SeniorMetadataProofVerification verifyProof(byte[] abProof, SeniorMetadataProofPayload expectedPayload,
                long lNowMillis)
            {
            if (abProof == null || abProof.length == 0)
                {
                return SeniorMetadataProofVerification.failed(MISSING, "missing");
                }

            SeniorMetadataProof proof;
            try
                {
                proof = SeniorMetadataProof.fromByteArray(abProof);
                }
            catch (Exception e)
                {
                return SeniorMetadataProofVerification.failed(MALFORMED, "malformed");
                }

            SeniorMetadataProofPayload payload = proof.getPayload();
            if (!f_sKeyId.equals(payload.getKeyId()))
                {
                return SeniorMetadataProofVerification.failed(UNKNOWN_KEY, payload.getKeyId());
                }
            if (!f_sAlgorithmId.equals(payload.getAlgorithmId()))
                {
                return SeniorMetadataProofVerification.failed(WRONG_ALGORITHM, payload.getAlgorithmId());
                }
            if (payload.getExpiresAtMillis() < lNowMillis)
                {
                return SeniorMetadataProofVerification.failed(EXPIRED, "expired");
                }
            if (!payload.equals(expectedPayload))
                {
                return SeniorMetadataProofVerification.failed(PAYLOAD_MISMATCH, "payload");
                }
            if (!MessageDigest.isEqual(mac(payload), proof.getProofBytes()))
                {
                return SeniorMetadataProofVerification.failed(PROOF_MISMATCH, "proof");
                }
            return SeniorMetadataProofVerification.valid();
            }

        private byte[] mac(SeniorMetadataProofPayload payload)
            {
            try
                {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(f_abSecret, "HmacSHA256"));
                return mac.doFinal(payload.toByteArray());
                }
            catch (Exception e)
                {
                throw new IllegalStateException(e);
                }
            }

        private final String f_sAlgorithmId;
        private final String f_sKeyId;
        private final byte[] f_abSecret;
        }

    private static final SeniorMetadataProofProvider DISABLED = new SeniorMetadataProofProvider()
        {
        @Override
        public boolean isEnabled()
            {
            return false;
            }

        @Override
        public byte[] createProof(SeniorMetadataProofPayload payload)
            {
            return null;
            }

        @Override
        public SeniorMetadataProofVerification verifyProof(byte[] abProof,
                SeniorMetadataProofPayload expectedPayload, long lNowMillis)
            {
            return SeniorMetadataProofVerification.disabled();
            }
        };
    }
