/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

/**
 * Provider abstraction for subject-proof production and verification.
 *
 * @author Aleks Seovic  2026.05.18
 * @since 26.07
 */
public interface SubjectProofProvider
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
    byte[] createProof(SubjectProofPayload payload);

    /**
     * Verify proof bytes against the expected canonical payload.
     *
     * @param abProof          the proof bytes
     * @param expectedPayload  the expected payload
     * @param lNowMillis       verification time
     *
     * @return the verification result
     */
    SubjectProofVerification verifyProof(byte[] abProof, SubjectProofPayload expectedPayload, long lNowMillis);
    }
