/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

import java.util.function.Predicate;

/**
 * Small helper for enforcing configured subject-proof policy.
 *
 * @author Aleks Seovic  2026.05.19
 * @since 26.07
 */
public final class SubjectProofVerifier
    {
    /**
     * Verify proof bytes using the configured provider.
     *
     * @param provider         the provider
     * @param abProof          the proof bytes
     * @param expectedPayload  the expected canonical payload
     * @param lNowMillis       verification time
     *
     * @return the verification result
     */
    public static SubjectProofVerification verify(SubjectProofProvider provider, byte[] abProof,
            SubjectProofPayload expectedPayload, long lNowMillis)
        {
        return verify(provider, abProof, expectedPayload, payload -> true, lNowMillis);
        }

    /**
     * Verify proof bytes using the configured provider.
     *
     * @param provider         the provider
     * @param abProof          the proof bytes
     * @param expectedPayload  the expected canonical payload
     * @param senderValidator  sender/topology validator for the signed payload
     * @param lNowMillis       verification time
     *
     * @return the verification result
     */
    public static SubjectProofVerification verify(SubjectProofProvider provider, byte[] abProof,
            SubjectProofPayload expectedPayload, Predicate<SubjectProofPayload> senderValidator, long lNowMillis)
        {
        if (provider == null || !provider.isEnabled())
            {
            return SubjectProofVerification.disabled();
            }
        if (abProof == null || abProof.length == 0)
            {
            return SubjectProofVerification.failed(SubjectProofVerification.Status.MISSING, "missing");
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
        if (payload.getExpiresAtMillis() < lNowMillis)
            {
            return SubjectProofVerification.failed(SubjectProofVerification.Status.EXPIRED, "expired");
            }
        if (!payload.matchesScope(expectedPayload))
            {
            return SubjectProofVerification.failed(SubjectProofVerification.Status.PAYLOAD_MISMATCH, "payload");
            }
        if (!payload.matchesPrincipals(expectedPayload))
            {
            return SubjectProofVerification.failed(SubjectProofVerification.Status.PRINCIPAL_MISMATCH,
                    "principals");
            }
        if (senderValidator == null || !senderValidator.test(payload))
            {
            return SubjectProofVerification.failed(SubjectProofVerification.Status.SENDER_MISMATCH, "sender");
            }

        return provider.verifyProof(abProof, payload, lNowMillis);
        }

    /**
     * Throw when verification is not valid.
     *
     * @param result  the verification result
     */
    public static void requireValid(SubjectProofVerification result)
        {
        if (result == null || !result.isValid())
            {
            SubjectProofVerification.Status status = result == null
                    ? SubjectProofVerification.Status.MALFORMED : result.getStatus();
            throw new SecurityException("subject proof verification failed: " + status);
            }
        }

    private SubjectProofVerifier()
        {
        }
    }
