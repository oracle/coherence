/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

/**
 * Subject-proof provider factories.
 *
 * @author Aleks Seovic  2026.05.18
 * @since 26.07
 */
public final class SubjectProofProviders
    {
    /**
     * Return the behavior-neutral disabled provider.
     *
     * @return the disabled provider
     */
    public static SubjectProofProvider disabled()
        {
        return DISABLED;
        }

    private SubjectProofProviders()
        {
        }

    private static final SubjectProofProvider DISABLED = new SubjectProofProvider()
        {
        @Override
        public boolean isEnabled()
            {
            return false;
            }

        @Override
        public byte[] createProof(SubjectProofPayload payload)
            {
            return null;
            }

        @Override
        public SubjectProofVerification verifyProof(byte[] abProof, SubjectProofPayload expectedPayload,
                long lNowMillis)
            {
            return SubjectProofVerification.disabled();
            }
        };
    }
