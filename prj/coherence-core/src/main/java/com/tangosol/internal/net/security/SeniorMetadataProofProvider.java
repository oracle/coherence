/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

/**
 * Provider abstraction for senior-metadata proof production and verification.
 *
 * @author Aleks Seovic  2026.05.19
 * @since 15.1.2.0
 */
public interface SeniorMetadataProofProvider
    {
    /**
     * Return true iff this provider is enabled.
     *
     * @return true iff this provider is enabled
     */
    default boolean isEnabled()
        {
        return true;
        }

    /**
     * Create proof bytes for a canonical payload.
     *
     * @param payload  the payload
     *
     * @return proof bytes, or {@code null} when no proof is produced
     */
    byte[] createProof(SeniorMetadataProofPayload payload);

    /**
     * Verify proof bytes against the expected canonical payload.
     *
     * @param abProof          the proof bytes
     * @param expectedPayload  the expected payload
     * @param lNowMillis       verification time
     *
     * @return the verification result
     */
    SeniorMetadataProofVerification verifyProof(byte[] abProof, SeniorMetadataProofPayload expectedPayload,
            long lNowMillis);

    /**
     * Return true iff this provider recognizes the signed senior identity as a
     * senior metadata authority for delegated panic-flow tokens.
     *
     * @param payload  the signed payload
     *
     * @return true iff the signed senior is a provider-owned authority
     */
    default boolean isSeniorMetadataProofAuthority(SeniorMetadataProofPayload payload)
        {
        return false;
        }
    }
