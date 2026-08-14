/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

/**
 * Structured subject-proof verification result.
 *
 * @author Aleks Seovic  2026.05.18
 * @since 26.07
 */
public final class SubjectProofVerification
    {
    /**
     * Verification status.
     */
    public enum Status
        {
        VALID,
        DISABLED,
        MISSING,
        MALFORMED,
        UNKNOWN_KEY,
        WRONG_ALGORITHM,
        EXPIRED,
        PAYLOAD_MISMATCH,
        PRINCIPAL_MISMATCH,
        SENDER_MISMATCH,
        NOT_YET_VALID,
        UNAUTHORIZED_ISSUER,
        DUPLICATE_IDENTITY,
        PROVIDER_UNAVAILABLE,
        PROOF_MISMATCH
        }

    /**
     * Return a valid result.
     *
     * @return a valid result
     */
    public static SubjectProofVerification valid()
        {
        return new SubjectProofVerification(Status.VALID, "");
        }

    /**
     * Return a disabled-provider result.
     *
     * @return a disabled-provider result
     */
    public static SubjectProofVerification disabled()
        {
        return new SubjectProofVerification(Status.DISABLED, "");
        }

    /**
     * Return a failed result.
     *
     * @param status   the status
     * @param sDetail  the detail
     *
     * @return the failed result
     */
    public static SubjectProofVerification failed(Status status, String sDetail)
        {
        if (status == Status.VALID || status == Status.DISABLED)
            {
            throw new IllegalArgumentException("status must be a failure");
            }
        return new SubjectProofVerification(status, sDetail);
        }

    /**
     * Return true iff verification succeeded.
     *
     * @return true iff verification succeeded
     */
    public boolean isValid()
        {
        return f_status == Status.VALID;
        }

    /**
     * Return the verification status.
     *
     * @return the verification status
     */
    public Status getStatus()
        {
        return f_status;
        }

    /**
     * Return bounded verification detail.
     *
     * @return bounded verification detail
     */
    public String getDetail()
        {
        return f_sDetail;
        }

    private SubjectProofVerification(Status status, String sDetail)
        {
        f_status  = status;
        f_sDetail = sDetail == null ? "" : sDetail;
        }

    private final Status f_status;
    private final String f_sDetail;
    }
