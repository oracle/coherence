/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

/**
 * Overflow-safe time policy shared by PEER proof producers and verifiers.
 *
 * @author Aleks Seovic  2026.07.15
 * @since 26.1.0.0
 */
public final class ProofTimePolicy
    {
    /** A proof-time validation result. */
    public enum Status
        {
        VALID,
        INVALID_VALIDITY_WINDOW,
        NOT_YET_VALID,
        EXPIRED
        }

    /** Validate a proof validity interval against the fixed PEER clock skew. */
    public static Status validate(long lIssuedAt, long lExpiresAt, long lNow)
        {
        if (lExpiresAt < lIssuedAt || lExpiresAt > addSaturated(lIssuedAt, MAX_PROOF_VALIDITY_MILLIS))
            {
            return Status.INVALID_VALIDITY_WINDOW;
            }
        if (lIssuedAt > addSaturated(lNow, CLOCK_SKEW_MILLIS))
            {
            return Status.NOT_YET_VALID;
            }
        if (lExpiresAt < subtractSaturated(lNow, CLOCK_SKEW_MILLIS))
            {
            return Status.EXPIRED;
            }
        return Status.VALID;
        }

    /** Add two positive-duration values without wrapping the long domain. */
    public static long addSaturated(long lValue, long cMillis)
        {
        return cMillis > 0L && lValue > Long.MAX_VALUE - cMillis
                ? Long.MAX_VALUE : lValue + cMillis;
        }

    /** Subtract a positive duration without wrapping the long domain. */
    public static long subtractSaturated(long lValue, long cMillis)
        {
        return cMillis > 0L && lValue < Long.MIN_VALUE + cMillis
                ? Long.MIN_VALUE : lValue - cMillis;
        }

    public static final long MAX_PROOF_VALIDITY_MILLIS = 5L * 60L * 1000L;
    public static final long CLOCK_SKEW_MILLIS = 2L * 60L * 1000L;

    private ProofTimePolicy()
        {
        }
    }
