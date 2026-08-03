/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

/**
 * Internal optional resolver for storage-authorizer subject-proof policy.
 *
 * @author Aleks Seovic  2026.05.19
 * @since 26.07
 */
public interface StorageAccessAuthorizerPolicyResolver
    {
    /**
     * Return {@code true} iff the cache's configured operational storage
     * authorizer requires subject proof.
     *
     * @param sName  the cache name
     *
     * @return {@code true} iff subject proof is required
     */
    boolean isSubjectProofRequired(String sName);
    }
