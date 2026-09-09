/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

import java.util.Collection;

/**
 * Shared product provider for PEER subject and senior-metadata proofs.
 *
 * @author Aleks Seovic  2026.07.15
 * @since 26.1.0.0
 */
public interface PeerProofProvider
        extends SubjectProofProvider, SeniorMetadataProofProvider, AutoCloseable
    {
    @Override
    default boolean isEnabled()
        {
        return true;
        }

    /** Return the bounded provider id. */
    String getProviderId();

    /** Return the active wire algorithm id. */
    String getAlgorithmId();

    /** Return the active signing key id. */
    String getKeyId();

    /** Return the certificate-bound local issuer identity. */
    String getIssuerId();

    /**
     * Bind the provider to the configured local member name.
     *
     * @param sMemberName  the non-empty local member name
     */
    void setLocalMemberName(String sMemberName);

    /**
     * Observe the complete receiver-known live member-name set.
     *
     * @param colMemberNames  live member names, including duplicates
     */
    void observeMemberNames(Collection<String> colMemberNames);

    /** Return true when the current snapshot and live identity set are usable. */
    boolean isReady();

    /** Return a bounded, non-secret readiness status. */
    String getReadinessStatus();

    /** Return the bounded subject-role readiness/rotation stage. */
    default String getSubjectReadinessStatus() {return getReadinessStatus();}

    /** Return the bounded senior-role readiness/rotation stage. */
    default String getSeniorReadinessStatus() {return getReadinessStatus();}

    /**
     * Return a bounded reminder that cluster readiness requires an explicit
     * all-member query; this local method never claims cluster-wide state.
     */
    default String getClusterReadinessStatus() {return "all-member-query-required";}

    /** Return one fresh, key-specific local observation for an all-member query. */
    default PeerProofReadiness.Observation getLocalReadinessObservation()
        {
        return PeerProofReadiness.Observation.unavailable(getProviderId(), 0L);
        }

    /** Observe the current senior-carrier capability state. */
    default void observeSeniorCapabilities(int cMembers, int cCompatible, int cPending) {}

    /** Reload all configured material atomically. */
    boolean refresh();

    /** Stop refresh activity and discard retained credential material. */
    @Override
    default void close() {}
    }
