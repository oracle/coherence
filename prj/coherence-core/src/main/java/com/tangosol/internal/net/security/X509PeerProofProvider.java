/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.PasswordProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URI;

import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import java.nio.charset.StandardCharsets;

import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static com.tangosol.internal.net.security.SeniorMetadataProofVerification.Status.EXPIRED;
import static com.tangosol.internal.net.security.SeniorMetadataProofVerification.Status.MALFORMED;
import static com.tangosol.internal.net.security.SeniorMetadataProofVerification.Status.MISSING;
import static com.tangosol.internal.net.security.SeniorMetadataProofVerification.Status.PAYLOAD_MISMATCH;
import static com.tangosol.internal.net.security.SeniorMetadataProofVerification.Status.PROOF_MISMATCH;
import static com.tangosol.internal.net.security.SeniorMetadataProofVerification.Status.UNKNOWN_KEY;
import static com.tangosol.internal.net.security.SeniorMetadataProofVerification.Status.WRONG_ALGORITHM;

/**
 * Built-in RSA/X.509 provider for production PEER proofs.
 *
 * <p>The provider owns one immutable identity/authority snapshot. Reload
 * builds and validates a complete replacement before the atomic swap; a
 * failed reload never partially changes trust.</p>
 *
 * @author Aleks Seovic  2026.07.15
 * @since 26.1.0.0
 */
public class X509PeerProofProvider
        implements PeerProofProvider
    {
    public X509PeerProofProvider(PeerProofDependencies deps)
        {
        this(deps, generation -> {}, new FileCredentialReader(), new PlatformKeyStoreLoader(),
                CREDENTIAL_OPERATION_TIMEOUT_MILLIS);
        }

    X509PeerProofProvider(PeerProofDependencies deps, RefreshObserver observer)
        {
        this(deps, observer, new FileCredentialReader(), new PlatformKeyStoreLoader(),
                CREDENTIAL_OPERATION_TIMEOUT_MILLIS);
        }

    X509PeerProofProvider(PeerProofDependencies deps, RefreshObserver observer, CredentialReader reader)
        {
        this(deps, observer, reader, new PlatformKeyStoreLoader(), CREDENTIAL_OPERATION_TIMEOUT_MILLIS);
        }

    X509PeerProofProvider(PeerProofDependencies deps, RefreshObserver observer, long cCredentialOperationTimeoutMillis)
        {
        this(deps, observer, new FileCredentialReader(), new PlatformKeyStoreLoader(),
                cCredentialOperationTimeoutMillis);
        }

    X509PeerProofProvider(PeerProofDependencies deps, RefreshObserver observer, CredentialReader reader,
            long cCredentialOperationTimeoutMillis)
        {
        this(deps, observer, reader, new PlatformKeyStoreLoader(), cCredentialOperationTimeoutMillis);
        }

    X509PeerProofProvider(PeerProofDependencies deps, RefreshObserver observer, CredentialReader reader,
            KeyStoreLoader keyStoreLoader, long cCredentialOperationTimeoutMillis)
        {
        m_deps = deps.validate();
        f_refreshObserver = observer == null ? generation -> {} : observer;
        f_credentialReader = reader == null ? new FileCredentialReader() : reader;
        f_keyStoreLoader = keyStoreLoader == null ? new PlatformKeyStoreLoader() : keyStoreLoader;
        f_cCredentialOperationTimeoutMillis = Math.max(1L, cCredentialOperationTimeoutMillis);
        if (!refresh())
            {
            if (m_deps.isSubjectProofRequired() || m_deps.isSeniorMetadataProofRequired())
                {
                String sStatus = f_state.get().f_sReloadStatus;
                close();
                throw new IllegalArgumentException("peer-proof provider has no usable credential snapshot: "
                        + sStatus);
                }
            }
        if (!f_state.get().f_fClosed)
            {
            f_executor.scheduleWithFixedDelay(this::scheduledRefresh, m_deps.getRefreshMillis(),
                    m_deps.getRefreshMillis(), TimeUnit.MILLISECONDS);
            }
        }

    @Override
    public String getProviderId() {return PROVIDER_ID;}

    @Override
    public String getAlgorithmId() {return ALGORITHM_ID;}

    @Override
    public String getKeyId()
        {
        State state = f_state.get();
        Snapshot snapshot = state.f_fClosed ? null : state.f_snapshot;
        return snapshot == null ? "" : snapshot.f_sKeyId;
        }

    @Override
    public String getIssuerId()
        {
        State state = f_state.get();
        return state.f_fClosed ? "" : state.f_sIssuerId;
        }

    @Override
    public void setLocalMemberName(String sMemberName)
        {
        synchronized (f_lifecycleLock)
            {
            State state = f_state.get();
            if (state.f_fClosed) {return;}
            String sName = normalize(sMemberName);
            f_state.set(bindIdentity(state.next(), state.f_snapshot, sName, state.f_sReloadStatus));
            }
        }

    @Override
    public void observeMemberNames(Collection<String> colMemberNames)
        {
        Set<String> set = new HashSet<>();
        String sDuplicate = "";
        if (colMemberNames != null)
            {
            for (String sName : colMemberNames)
                {
                if (sName != null && !sName.isEmpty())
                    {
                    String sIssuer = issuerId(sName);
                    if (!set.add(sIssuer))
                        {
                        sDuplicate = sIssuer;
                        break;
                        }
                    }
                }
            }
        synchronized (f_lifecycleLock)
            {
            State state = f_state.get();
            if (!state.f_fClosed)
                {
                f_state.set(state.withIdentitySet(sDuplicate, Collections.unmodifiableSet(set)));
                }
            }
        }

    @Override
    public void observeSeniorCapabilities(int cMembers, int cCompatible, int cPending)
        {
        synchronized (f_lifecycleLock)
            {
            State state = f_state.get();
            if (!state.f_fClosed)
                {
                int cAll = Math.max(0, cMembers);
                f_state.set(state.withCapabilities(cAll, Math.max(0, Math.min(cCompatible, cAll)),
                        Math.max(0, Math.min(cPending, cAll))));
                }
            }
        }

    @Override
    public boolean isEnabled() {return true;}

    @Override
    public boolean isReady()
        {
        return isReady(f_state.get(), System.currentTimeMillis());
        }

    @Override
    public String getReadinessStatus()
        {
        State state = f_state.get();
        if (state.f_fClosed)
            {
            return "closed";
            }
        if (!state.f_sDuplicateIdentity.isEmpty())
            {
            return "duplicate-identity:" + bounded(state.f_sDuplicateIdentity);
            }
        if (state.f_snapshot == null)
            {
            return state.f_sReloadStatus;
            }
        if (!"ready".equals(state.f_sIdentityStatus))
            {
            return state.f_sIdentityStatus;
            }
        return state.f_sReloadStatus;
        }

    @Override
    public String getSubjectReadinessStatus()
        {
        return roleReadiness(true);
        }

    @Override
    public String getSeniorReadinessStatus()
        {
        return roleReadiness(false);
        }

    @Override
    public String getClusterReadinessStatus()
        {
        State state = f_state.get();
        if (state.f_fClosed)
            {
            return "closed";
            }
        if (state.f_cCapabilityPending > 0)
            {
            return "capability-pending:" + state.f_cCapabilityPending;
            }
        if (state.f_cCapabilityMembers > state.f_cCapabilityCompatible)
            {
            return "capability-incompatible:"
                    + (state.f_cCapabilityMembers - state.f_cCapabilityCompatible);
            }
        return "all-member-query-required";
        }

    @Override
    public PeerProofReadiness.Observation getLocalReadinessObservation()
        {
        long now = System.currentTimeMillis();
        State state = f_state.get();
        Snapshot snapshot = state.f_snapshot;
        if (state.f_fClosed || snapshot == null)
            {
            return PeerProofReadiness.Observation.unavailable(PROVIDER_ID, state.f_lGeneration);
            }
        return new PeerProofReadiness.Observation(state.f_sIssuerId, PROVIDER_ID, snapshot.f_sKeyId,
                isReady(state, now), now, state.f_lGeneration, window(snapshot.f_certIdentity),
                windows(snapshot.f_mapSubject), windows(snapshot.f_mapSenior));
        }

    @Override
    public boolean refresh()
        {
        long lGeneration;
        PeerProofDependencies deps;
        ReadScope scope = new ReadScope(System.nanoTime() +
                TimeUnit.MILLISECONDS.toNanos(f_cCredentialOperationTimeoutMillis));
        synchronized (f_lifecycleLock)
            {
            if (f_state.get().f_fClosed)
                {
                return false;
                }
            lGeneration = ++m_lRefreshGeneration;
            m_cActiveRefresh++;
            f_setReadScopes.add(scope);
            deps = m_deps;
            }
        Snapshot snapshot = null;
        Exception failure = null;
        try
            {
            scope.startDeadline();
            snapshot = scope.execute(() -> loadSnapshot(deps, scope));
            f_refreshObserver.snapshotLoaded(lGeneration);
            }
        catch (Exception e)
            {
            failure = e;
            }

        synchronized (f_lifecycleLock)
            {
            try
                {
                State state = f_state.get();
                if (state.f_fClosed || lGeneration < m_lCommittedRefreshGeneration)
                    {
                    return false;
                    }
                if (scope.isCancelled())
                    {
                    Logger.warn("PEER proof credential refresh was cancelled or exceeded its absolute deadline");
                    return false;
                    }
                if (failure != null)
                    {
                    String sReason = reloadDiagnostic(failure);
                    String sStatus = (state.f_snapshot == null
                            ? "reload-unavailable:" : "reload-degraded-last-known-good:") + sReason;
                    if (!scope.tryCommit())
                        {
                        Logger.warn("PEER proof credential refresh exceeded its absolute publication deadline");
                        return false;
                        }
                    f_state.set(state.withReloadStatus(sStatus));
                    Logger.warn("PEER proof credential refresh failed; status=" + sStatus
                            + ", cause=" + failure.getClass().getSimpleName());
                    return false;
                    }

                State candidate = bindIdentity(state.next(), snapshot, state.f_sMemberName,
                        "ready:key=" + boundedKey(snapshot.f_sKeyId));
                if (!state.f_sMemberName.isEmpty() && !"ready".equals(candidate.f_sIdentityStatus))
                    {
                    String sStatus = (state.f_snapshot == null
                            ? "reload-unavailable:" : "reload-degraded-last-known-good:")
                            + candidate.f_sIdentityStatus;
                    if (!scope.tryCommit())
                        {
                        Logger.warn("PEER proof credential refresh exceeded its absolute publication deadline");
                        return false;
                        }
                    f_state.set(state.withReloadStatus(sStatus));
                    Logger.warn("PEER proof credential refresh failed; status=" + sStatus);
                    return false;
                    }
                if (!scope.tryCommit())
                    {
                    Logger.warn("PEER proof credential refresh exceeded its absolute publication deadline");
                    return false;
                    }
                f_state.set(candidate);
                m_lCommittedRefreshGeneration = lGeneration;
                return true;
                }
            finally
                {
                scope.complete();
                f_setReadScopes.remove(scope);
                m_cActiveRefresh--;
                f_lifecycleLock.notifyAll();
                }
            }
        }

    @Override
    public void close()
        {
        long lDeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(CLOSE_TIMEOUT_MILLIS);
        List<ReadScope> listScopes;
        synchronized (f_lifecycleLock)
            {
            State state = f_state.get();
            if (state.f_fClosed)
                {
                if (m_fCloseComplete)
                    {
                    return;
                    }
                listScopes = new ArrayList<>(f_setReadScopes);
                }
            else
                {
                ++m_lRefreshGeneration;
                f_state.set(State.closed(state.f_lGeneration + 1L));
                m_deps = null;
                listScopes = new ArrayList<>(f_setReadScopes);
                f_executor.shutdownNow();
                }
            }
        try
            {
            for (ReadScope scope : listScopes)
                {
                scope.cancel();
                }
            }
        finally
            {
            // Cancellation may enqueue bounded backend-close work.  Once
            // scopes have received cancellation, shut down both owned
            // executors before entering any wait that can consume the close
            // deadline.  The finally block also covers a hostile callback.
            f_executor.shutdownNow();
            f_ioExecutor.shutdown();
            f_cleanupExecutor.shutdown();
            }

        boolean fComplete = false;
        try
            {
            synchronized (f_lifecycleLock)
                {
                while (m_cActiveRefresh > 0)
                    {
                    waitUntil(f_lifecycleLock, lDeadline);
                    }
                }
            awaitTermination(f_executor, lDeadline, "refresh executor");
            awaitTermination(f_ioExecutor, lDeadline, "credential I/O executor");
            awaitTermination(f_cleanupExecutor, lDeadline, "credential cleanup executor");
            fComplete = true;
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while closing PEER proof provider", e);
            }
        finally
            {
            synchronized (f_lifecycleLock)
                {
                m_fCloseComplete |= fComplete;
                f_lifecycleLock.notifyAll();
                }
            }
        }

    private static void awaitTermination(ExecutorService executor, long lDeadline, String sName)
            throws InterruptedException
        {
        long cNanos = remainingNanos(lDeadline);
        if (!executor.awaitTermination(cNanos, TimeUnit.NANOSECONDS))
            {
            throw new IllegalStateException("PEER proof " + sName + " exceeded the bounded close interval");
            }
        }

    private static void waitUntil(Object lock, long lDeadline) throws InterruptedException
        {
        long cNanos = remainingNanos(lDeadline);
        long cMillis = TimeUnit.NANOSECONDS.toMillis(cNanos);
        int cExtraNanos = (int) (cNanos - TimeUnit.MILLISECONDS.toNanos(cMillis));
        lock.wait(cMillis, cExtraNanos);
        }

    private static long remainingNanos(long lDeadline)
        {
        long cNanos = lDeadline - System.nanoTime();
        if (cNanos <= 0L)
            {
            throw new IllegalStateException("PEER proof operation exceeded its absolute deadline");
            }
        return cNanos;
        }

    @Override
    public byte[] createProof(SubjectProofPayload payload)
        {
        State state = f_state.get();
        Snapshot snapshot = signingSnapshot(state, payload == null ? 0L : payload.getIssuedAtMillis());
        if (snapshot == null || payload == null || !snapshot.f_mapSubject.containsKey(snapshot.f_sKeyId)
                || !matchesSigningMetadata(payload.getAlgorithmId(), payload.getKeyId(),
                        payload.getIssuerId(), snapshot, state))
            {
            return null;
            }
        return new SubjectProof(payload, sign(snapshot.f_key, payload.toByteArray())).toByteArray();
        }

    @Override
    public SubjectProofVerification verifyProof(byte[] abProof, SubjectProofPayload expected, long lNowMillis)
        {
        State state = f_state.get();
        if (state.f_fClosed)
            {
            return SubjectProofVerification.failed(SubjectProofVerification.Status.PROVIDER_UNAVAILABLE, "closed");
            }
        if (abProof == null || abProof.length == 0)
            {
            return SubjectProofVerification.failed(SubjectProofVerification.Status.MISSING, "missing");
            }
        if (!state.f_sDuplicateIdentity.isEmpty())
            {
            return SubjectProofVerification.failed(SubjectProofVerification.Status.DUPLICATE_IDENTITY,
                    "duplicate-identity:" + bounded(state.f_sDuplicateIdentity));
            }
        try
            {
            SubjectProof proof = SubjectProof.fromByteArray(abProof);
            SubjectProofPayload payload = proof.getPayload();
            SubjectProofVerification failure = validateSubjectPayload(payload, expected, lNowMillis);
            if (failure != null) {return failure;}
            Snapshot snapshot = state.f_snapshot;
            X509Certificate cert = snapshot == null ? null : snapshot.f_mapSubject.get(payload.getKeyId());
            if (cert == null)
                {
                return SubjectProofVerification.failed(SubjectProofVerification.Status.UNKNOWN_KEY,
                        boundedKey(payload.getKeyId()));
                }
            if (!authorizedCertificate(cert, payload.getIssuerId(), lNowMillis))
                {
                return SubjectProofVerification.failed(SubjectProofVerification.Status.UNAUTHORIZED_ISSUER,
                        bounded(payload.getIssuerId()));
                }
            return verify(cert, payload.toByteArray(), proof.getProofBytes())
                    ? SubjectProofVerification.valid()
                    : SubjectProofVerification.failed(SubjectProofVerification.Status.PROOF_MISMATCH, "proof");
            }
        catch (Exception e)
            {
            return SubjectProofVerification.failed(SubjectProofVerification.Status.MALFORMED, "malformed");
            }
        }

    @Override
    public byte[] createProof(SeniorMetadataProofPayload payload)
        {
        State state = f_state.get();
        Snapshot snapshot = signingSnapshot(state, payload == null ? 0L : payload.getIssuedAtMillis());
        if (snapshot == null || payload == null || !snapshot.f_mapSenior.containsKey(snapshot.f_sKeyId)
                || !matchesSigningMetadata(payload.getAlgorithmId(), payload.getKeyId(),
                        payload.getIssuerId(), snapshot, state))
            {
            return null;
            }
        return new SeniorMetadataProof(payload, sign(snapshot.f_key, payload.toByteArray())).toByteArray();
        }

    @Override
    public SeniorMetadataProofVerification verifyProof(byte[] abProof, SeniorMetadataProofPayload expected,
            long lNowMillis)
        {
        State state = f_state.get();
        if (state.f_fClosed)
            {
            return SeniorMetadataProofVerification.failed(
                    SeniorMetadataProofVerification.Status.PROVIDER_UNAVAILABLE, "closed");
            }
        if (abProof == null || abProof.length == 0)
            {
            return SeniorMetadataProofVerification.failed(MISSING, "missing");
            }
        if (!state.f_sDuplicateIdentity.isEmpty())
            {
            return SeniorMetadataProofVerification.failed(
                    SeniorMetadataProofVerification.Status.DUPLICATE_IDENTITY,
                    "duplicate-identity:" + bounded(state.f_sDuplicateIdentity));
            }
        try
            {
            SeniorMetadataProof proof = SeniorMetadataProof.fromByteArray(abProof);
            SeniorMetadataProofPayload payload = proof.getPayload();
            if (!ALGORITHM_ID.equals(payload.getAlgorithmId()))
                {return SeniorMetadataProofVerification.failed(WRONG_ALGORITHM, bounded(payload.getAlgorithmId()));}
            SeniorMetadataProofVerification timeFailure = validateSeniorTime(payload, lNowMillis);
            if (timeFailure != null) {return timeFailure;}
            if (!payload.equals(expected))
                {return SeniorMetadataProofVerification.failed(PAYLOAD_MISMATCH, "payload");}
            Snapshot snapshot = state.f_snapshot;
            X509Certificate cert = snapshot == null ? null : snapshot.f_mapSenior.get(payload.getKeyId());
            if (cert == null)
                {return SeniorMetadataProofVerification.failed(UNKNOWN_KEY, boundedKey(payload.getKeyId()));}
            if (!authorizedCertificate(cert, payload.getIssuerId(), lNowMillis))
                {return SeniorMetadataProofVerification.failed(
                        SeniorMetadataProofVerification.Status.UNAUTHORIZED_ISSUER, bounded(payload.getIssuerId()));}
            return verify(cert, payload.toByteArray(), proof.getProofBytes())
                    ? SeniorMetadataProofVerification.valid()
                    : SeniorMetadataProofVerification.failed(PROOF_MISMATCH, "proof");
            }
        catch (Exception e)
            {
            return SeniorMetadataProofVerification.failed(MALFORMED, "malformed");
            }
        }

    @Override
    public boolean isSeniorMetadataProofAuthority(SeniorMetadataProofPayload payload)
        {
        State state = f_state.get();
        if (state.f_fClosed)
            {
            return false;
            }
        Snapshot snapshot = state.f_snapshot;
        X509Certificate cert = snapshot == null || payload == null ? null : snapshot.f_mapSenior.get(payload.getKeyId());
        return state.f_sDuplicateIdentity.isEmpty() && cert != null
                && authorizedCertificate(cert, payload.getIssuerId(), System.currentTimeMillis());
        }

    private SubjectProofVerification validateSubjectPayload(SubjectProofPayload payload,
            SubjectProofPayload expected, long now)
        {
        if (!ALGORITHM_ID.equals(payload.getAlgorithmId()))
            {return SubjectProofVerification.failed(SubjectProofVerification.Status.WRONG_ALGORITHM,
                    bounded(payload.getAlgorithmId()));}
        if (expected != null && (!payload.matchesScope(expected) || !payload.matchesPrincipals(expected)))
            {return SubjectProofVerification.failed(SubjectProofVerification.Status.PAYLOAD_MISMATCH, "payload");}
        ProofTimePolicy.Status status = ProofTimePolicy.validate(payload.getIssuedAtMillis(),
                payload.getExpiresAtMillis(), now);
        if (status != ProofTimePolicy.Status.VALID)
            {
            return SubjectProofVerification.failed(status == ProofTimePolicy.Status.NOT_YET_VALID
                            ? SubjectProofVerification.Status.NOT_YET_VALID : SubjectProofVerification.Status.EXPIRED,
                    status == ProofTimePolicy.Status.INVALID_VALIDITY_WINDOW ? "invalid-validity-window"
                            : status == ProofTimePolicy.Status.NOT_YET_VALID ? "future-issued" : "expired");
            }
        return null;
        }

    private SeniorMetadataProofVerification validateSeniorTime(SeniorMetadataProofPayload payload, long now)
        {
        ProofTimePolicy.Status status = ProofTimePolicy.validate(payload.getIssuedAtMillis(),
                payload.getExpiresAtMillis(), now);
        if (status != ProofTimePolicy.Status.VALID)
            {
            return SeniorMetadataProofVerification.failed(status == ProofTimePolicy.Status.NOT_YET_VALID
                            ? SeniorMetadataProofVerification.Status.NOT_YET_VALID : EXPIRED,
                    status == ProofTimePolicy.Status.INVALID_VALIDITY_WINDOW ? "invalid-validity-window"
                            : status == ProofTimePolicy.Status.NOT_YET_VALID ? "future-issued" : "expired");
            }
        return null;
        }

    private Snapshot signingSnapshot(State state, long now)
        {
        Snapshot snapshot = state.f_snapshot;
        return isReady(state, System.currentTimeMillis()) && snapshot != null
                && certificateTimeValid(snapshot.f_certIdentity,
                now == 0L ? System.currentTimeMillis() : now) ? snapshot : null;
        }

    private boolean matchesSigningMetadata(String algorithm, String key, String issuer,
            Snapshot snapshot, State state)
        {
        return ALGORITHM_ID.equals(algorithm) && snapshot.f_sKeyId.equals(key)
                && state.f_sIssuerId.equals(issuer);
        }

    private Snapshot loadSnapshot(PeerProofDependencies deps, ReadScope scope) throws Exception
        {
        if (deps == null)
            {
            throw new IllegalStateException("peer-proof provider is closed");
            }
        KeyStore store = loadKeyStore(deps.getIdentityStoreUrl(), deps.getIdentityStoreType(),
                deps.getIdentityPasswordProvider(), scope);
        char[] password = null;
        try
            {
            password = password(deps.getIdentityPasswordProvider(), scope);
            Key key = store.getKey(deps.getIdentityAlias(), password);
            Certificate cert = store.getCertificate(deps.getIdentityAlias());
            Certificate[] chain = store.getCertificateChain(deps.getIdentityAlias());
            if (chain != null && chain.length > MAX_IDENTITY_CHAIN_CERTIFICATES)
                {throw new IllegalArgumentException("identity certificate chain exceeds the supported bound");}
            if (chain != null)
                {
                for (Certificate certificate : chain)
                    {
                    preflightCertificate(certificate.getEncoded());
                    }
                }
            scope.checkOpen();
            if (!(key instanceof PrivateKey) || !(cert instanceof X509Certificate))
                {throw new IllegalArgumentException("identity alias must contain a private key and X.509 leaf");}
            X509Certificate certIdentity = (X509Certificate) cert;
            validateLeaf(certIdentity, System.currentTimeMillis());
            validateRsaPair((PrivateKey) key, certIdentity);
            Map<String, X509Certificate> mapSubject = loadAuthorities(
                    deps.getSubjectCertificateUrls(), deps.getSubjectAuthorityStore(), scope);
            Map<String, X509Certificate> mapSenior = loadAuthorities(
                    deps.getSeniorCertificateUrls(), deps.getSeniorAuthorityStore(), scope);
            return new Snapshot((PrivateKey) key, certIdentity, fingerprint(certIdentity), mapSubject, mapSenior);
            }
        finally
            {
            PasswordProvider.reset(password);
            }
        }

    private Map<String, X509Certificate> loadAuthorities(List<String> listUrls,
            PeerProofDependencies.AuthorityStore descriptor, ReadScope scope) throws Exception
        {
        List<X509Certificate> list = new ArrayList<>();
        if (descriptor != null)
            {
            KeyStore store = loadKeyStore(descriptor.getUrl(), descriptor.getType(),
                    descriptor.getPasswordProvider(), scope);
            Enumeration<String> aliases = store.aliases();
            int cAliases = 0;
            while (aliases.hasMoreElements())
                {
                scope.checkOpen();
                if (++cAliases > MAX_KEYSTORE_ALIASES)
                    {throw new IllegalArgumentException("authority store exceeds the supported alias bound");}
                Certificate cert = store.getCertificate(aliases.nextElement());
                if (cert instanceof X509Certificate)
                    {
                    preflightCertificate(cert.getEncoded());
                    list.add((X509Certificate) cert);
                    }
                }
            }
        else
            {
            for (String sUrl : listUrls)
                {
                byte[] bytes = readCredential(sUrl, scope);
                int cRemaining = MAX_AUTHORITY_CERTIFICATES - list.size();
                for (X509Certificate cert : parsePemCertificates(bytes, cRemaining, scope))
                    {
                    scope.checkOpen();
                    list.add(cert);
                    if (list.size() > MAX_AUTHORITY_CERTIFICATES)
                        {throw new IllegalArgumentException("authority set exceeds the supported certificate bound");}
                    }
                }
            }
        if (list.isEmpty()) {throw new IllegalArgumentException("authority set is empty");}
        if (list.size() > MAX_AUTHORITY_CERTIFICATES)
            {throw new IllegalArgumentException("authority set exceeds the supported certificate bound");}
        Map<String, X509Certificate> map = new HashMap<>();
        long now = System.currentTimeMillis();
        for (X509Certificate cert : list)
            {
            scope.checkOpen();
            validateLeaf(cert, now);
            String keyId = fingerprint(cert);
            if (map.put(keyId, cert) != null)
                {throw new IllegalArgumentException("duplicate authority key id " + boundedKey(keyId));}
            }
        return Collections.unmodifiableMap(map);
        }

    /** Load one supported key store through the provider-owned bounded read boundary. */
    private KeyStore loadKeyStore(String sUrl, String sType, PasswordProvider provider, ReadScope scope)
            throws Exception
        {
        byte[] bytes = readCredential(sUrl, scope);
        preflightKeyStore(bytes, sType);
        char[] password = null;
        try
            {
            password = password(provider, scope);
            if (password != null && password.length == 0) {password = null;}
            KeyStore store = f_keyStoreLoader.load(sType, bytes, password);
            scope.checkOpen();
            int cAliases = 0;
            Enumeration<String> aliases = store.aliases();
            while (aliases.hasMoreElements())
                {
                aliases.nextElement();
                if (++cAliases > MAX_KEYSTORE_ALIASES)
                    {throw new IllegalArgumentException("key store exceeds the supported alias bound");}
                }
            return store;
            }
        finally
            {
            PasswordProvider.reset(password);
            }
        }

    /** Read one named password under the supported bounded-complexity contract. */
    private static char[] password(PasswordProvider provider, ReadScope scope) throws Exception
        {
        scope.checkOpen();
        char[] password = null;
        boolean fTransferred = false;
        try
            {
            password = provider == null ? null : provider.get();
            scope.checkOpen();
            if (password != null && password.length > MAX_PASSWORD_CHARS)
                {
                throw new IllegalArgumentException(
                        "peer-proof credential password exceeds the supported length bound");
                }
            fTransferred = true;
            return password;
            }
        finally
            {
            if (!fTransferred)
                {
                PasswordProvider.reset(password);
                }
            }
        }

    /** Parse bounded PEM leaves only after a complete pre-sink structural pass. */
    private static List<X509Certificate> parsePemCertificates(byte[] bytes, int cMaximum,
            ReadScope scope) throws Exception
        {
        List<byte[]> encoded = PemWorkFactor.decode(bytes, cMaximum);
        CertificateFactory factory = CertificateFactory.getInstance("X.509", JCA_CERTIFICATE_PROVIDER);
        List<X509Certificate> certificates = new ArrayList<>(encoded.size());
        for (byte[] certificate : encoded)
            {
            scope.checkOpen();
            Certificate parsed = factory.generateCertificate(new ByteArrayInputStream(certificate));
            if (!(parsed instanceof X509Certificate))
                {throw new IllegalArgumentException("authority material must contain X.509 certificates");}
            certificates.add((X509Certificate) parsed);
            }
        return certificates;
        }

    /** Enforce format and work-factor restrictions before a key-store parser sees bytes. */
    private static void preflightKeyStore(byte[] bytes, String sType) throws IOException
        {
        if (bytes.length > MAX_KEYSTORE_BYTES)
            {throw new IOException("peer-proof key store exceeds the supported size bound");}
        if ("JKS".equalsIgnoreCase(sType))
            {
            JksWorkFactor.validate(bytes);
            }
        else if ("PKCS12".equalsIgnoreCase(sType))
            {
            DerWorkFactor.validatePkcs12(bytes);
            }
        else
            {
            throw new IOException("unsupported peer-proof key store type");
            }
        }

    /** Bound one certificate's encoded parser input. */
    private static void preflightCertificate(byte[] bytes) throws IOException
        {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_CERTIFICATE_BYTES)
            {throw new IOException("peer-proof certificate exceeds the supported encoded bound");}
        DerWorkFactor.validateCertificate(bytes);
        }

    /** Read a local credential under one absolute operation deadline and size bound. */
    private byte[] readCredential(String sUrl, ReadScope scope) throws Exception
        {
        try (InputStream in = scope.open(sUrl))
            {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int cbTotal = 0;
            int cb;
            while ((cb = in.read(buffer)) >= 0)
                {
                scope.checkOpen();
                if (cb == 0) {continue;}
                cbTotal += cb;
                if (cbTotal > MAX_CREDENTIAL_BYTES)
                    {
                    throw new IOException("peer-proof credential exceeds the supported size bound");
                    }
                out.write(buffer, 0, cb);
                }
            return out.toByteArray();
            }
        }

    private static void validateLeaf(X509Certificate cert, long now) throws Exception
        {
        if (cert.getBasicConstraints() != -1)
            {throw new IllegalArgumentException("peer-proof authority must be a non-CA leaf");}
        boolean[] usage = cert.getKeyUsage();
        if (usage == null || usage.length == 0 || !usage[0])
            {throw new IllegalArgumentException("peer-proof leaf requires digitalSignature KeyUsage");}
        if (!(cert.getPublicKey() instanceof RSAKey))
            {throw new IllegalArgumentException("peer-proof leaf requires RSA key of at least 2048 bits");}
        if (((RSAKey) cert.getPublicKey()).getModulus().bitLength() < MIN_RSA_BITS)
            {throw new IllegalArgumentException("peer-proof leaf requires RSA key of at least 2048 bits");}
        if (!certificateTimeValid(cert, now))
            {throw new IllegalArgumentException("peer-proof leaf is outside its certificate validity boundary");}
        }

    private static void validateRsaPair(PrivateKey key, X509Certificate cert) throws Exception
        {
        if (!(key instanceof RSAKey))
            {throw new IllegalArgumentException("peer-proof private key requires RSA key of at least 2048 bits");}
        if (((RSAKey) key).getModulus().bitLength() < MIN_RSA_BITS)
            {throw new IllegalArgumentException("peer-proof private key requires RSA key of at least 2048 bits");}
        byte[] challenge = "coherence-peer-proof-key-pair".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (!verify(cert, challenge, sign(key, challenge)))
            {throw new IllegalArgumentException("peer-proof identity private key does not match certificate");}
        }

    private static boolean authorizedCertificate(X509Certificate cert, String issuer, long now)
        {
        try
            {
            validateLeaf(cert, now);
            return issuer != null && !issuer.isEmpty() && hasUriSan(cert, issuer);
            }
        catch (Exception e)
            {
            return false;
            }
        }

    private static boolean hasUriSan(X509Certificate cert, String expected)
        {
        try
            {
            Collection<List<?>> sans = cert.getSubjectAlternativeNames();
            if (sans != null)
                {
                for (List<?> san : sans)
                    {
                    if (san.size() >= 2 && Integer.valueOf(6).equals(san.get(0)) && expected.equals(san.get(1)))
                        {return true;}
                    }
                }
            }
        catch (Exception ignored) {}
        return false;
        }

    private static boolean certificateTimeValid(X509Certificate cert, long now)
        {
        return cert.getNotBefore().getTime() <= ProofTimePolicy.addSaturated(now, CLOCK_SKEW_MILLIS)
                && cert.getNotAfter().getTime() >= ProofTimePolicy.subtractSaturated(now, CLOCK_SKEW_MILLIS);
        }

    private static byte[] sign(PrivateKey key, byte[] bytes)
        {
        try
            {
            Signature signature = Signature.getInstance(JCA_SIGNATURE);
            signature.initSign(key);
            signature.update(bytes);
            return signature.sign();
            }
        catch (Exception e) {throw new IllegalStateException("peer-proof signing failed", e);}
        }

    private static boolean verify(X509Certificate cert, byte[] bytes, byte[] proof) throws Exception
        {
        Signature signature = Signature.getInstance(JCA_SIGNATURE);
        signature.initVerify(cert);
        signature.update(bytes);
        return signature.verify(proof);
        }

    private static String fingerprint(X509Certificate cert) throws Exception
        {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(cert.getEncoded());
        StringBuilder sb = new StringBuilder(KEY_ID_PREFIX);
        for (byte b : digest) {sb.append(String.format("%02x", b & 0xff));}
        return sb.toString();
        }

    private void scheduledRefresh()
        {
        if (f_state.get().f_fClosed)
            {
            return;
            }
        try {refresh();} catch (Throwable t)
            {Logger.warn("PEER proof scheduled refresh failed; cause=" + t.getClass().getSimpleName());}
        }

    public static String issuerId(String sMemberName)
        {
        if (sMemberName == null)
            {
            return "";
            }
        StringBuilder sb = new StringBuilder(ISSUER_PREFIX.length() + sMemberName.length() * 4);
        sb.append(ISSUER_PREFIX);
        for (int i = 0; i < sMemberName.length(); i++)
            {
            int n = sMemberName.charAt(i);
            sb.append(HEX[(n >>> 12) & 0xf])
                    .append(HEX[(n >>> 8) & 0xf])
                    .append(HEX[(n >>> 4) & 0xf])
                    .append(HEX[n & 0xf]);
            }
        return sb.toString();
        }

    private String roleReadiness(boolean fSubject)
        {
        State state = f_state.get();
        String sBase = readinessStatus(state);
        if ("closed".equals(sBase))
            {
            return sBase;
            }
        if (state.f_sReloadStatus.startsWith("reload-degraded-last-known-good:"))
            {
            return state.f_sReloadStatus;
            }
        if (!isReady(state, System.currentTimeMillis()))
            {
            PeerProofDependencies deps = m_deps;
            boolean fRequired = deps != null && (fSubject
                    ? deps.isSubjectProofRequired() : deps.isSeniorMetadataProofRequired());
            return fRequired ? "required-policy-unsatisfied:" + bounded(sBase) : "staged-unavailable:" + bounded(sBase);
            }
        Snapshot snapshot = state.f_snapshot;
        Map<String, X509Certificate> map = fSubject ? snapshot.f_mapSubject : snapshot.f_mapSenior;
        X509Certificate cert = map.get(snapshot.f_sKeyId);
        if (cert == null || !certificateTimeValid(cert, System.currentTimeMillis()))
            {
            PeerProofDependencies deps = m_deps;
            return (deps != null && (fSubject
                    ? deps.isSubjectProofRequired() : deps.isSeniorMetadataProofRequired()))
                    ? "required-policy-unsatisfied:active-key-untrusted" : "active-key-untrusted";
            }
        return "ready";
        }

    private static String readinessStatus(State state)
        {
        if (state.f_fClosed) {return "closed";}
        if (!state.f_sDuplicateIdentity.isEmpty())
            {return "duplicate-identity:" + bounded(state.f_sDuplicateIdentity);}
        if (state.f_snapshot == null) {return state.f_sReloadStatus;}
        return "ready".equals(state.f_sIdentityStatus) ? state.f_sReloadStatus : state.f_sIdentityStatus;
        }

    private static boolean isReady(State state, long now)
        {
        Snapshot snapshot = state.f_snapshot;
        return !state.f_fClosed && snapshot != null && !state.f_sIssuerId.isEmpty()
                && state.f_sDuplicateIdentity.isEmpty()
                && "ready".equals(state.f_sIdentityStatus)
                && certificateTimeValid(snapshot.f_certIdentity, now);
        }

    private static State bindIdentity(State base, Snapshot snapshot, String sMemberName, String sReloadStatus)
        {
        String sName = normalize(sMemberName);
        if (sName.isEmpty())
            {
            return base.withSnapshot(snapshot, sName, "", "identity-unbound", sReloadStatus);
            }
        if (snapshot == null)
            {
            return base.withSnapshot(null, sName, "", "identity-staged-unavailable", sReloadStatus);
            }
        String sIssuer = issuerId(sName);
        if (!hasUriSan(snapshot.f_certIdentity, sIssuer))
            {
            return base.withSnapshot(snapshot, sName, "", "identity-san-mismatch:" + bounded(sName), sReloadStatus);
            }
        return base.withSnapshot(snapshot, sName, sIssuer, "ready", sReloadStatus);
        }

    private static PeerProofReadiness.CertificateWindow window(X509Certificate cert)
        {
        return new PeerProofReadiness.CertificateWindow(cert.getNotBefore().getTime(), cert.getNotAfter().getTime());
        }

    private static Map<String, PeerProofReadiness.CertificateWindow> windows(
            Map<String, X509Certificate> certificates)
        {
        Map<String, PeerProofReadiness.CertificateWindow> map = new HashMap<>();
        for (Map.Entry<String, X509Certificate> entry : certificates.entrySet())
            {
            map.put(entry.getKey(), window(entry.getValue()));
            }
        return map;
        }

    private static String normalize(String s) {return s == null ? "" : s.trim();}
    private static String bounded(String s) {s = normalize(s); return s.length() <= 96 ? s : s.substring(0, 96);}
    private static String boundedKey(String s) {s = normalize(s); return s.length() <= 28 ? s : s.substring(0, 28);}

    /**
     * Return a bounded, secret-free refresh diagnostic.  Product validation
     * messages are deliberately field/reason based; loader failures expose
     * only their exception type because their messages can contain locations.
     */
    private static String reloadDiagnostic(Exception e)
        {
        String s = normalize(e.getMessage());
        return s.startsWith("peer-proof ") || s.startsWith("identity alias ")
                || s.startsWith("authority set ") || s.startsWith("duplicate authority ")
                ? bounded(s)
                : bounded(e.getClass().getSimpleName());
        }

    /** One atomically published lifecycle generation. */
    private static final class State
        {
        private State(long lGeneration, Snapshot snapshot, String sMemberName, String sIssuerId,
                String sIdentityStatus, String sDuplicateIdentity, String sReloadStatus,
                Set<String> liveIssuers, int cMembers, int cCompatible, int cPending, boolean fClosed)
            {
            f_lGeneration = lGeneration;
            f_snapshot = snapshot;
            f_sMemberName = normalize(sMemberName);
            f_sIssuerId = normalize(sIssuerId);
            f_sIdentityStatus = normalize(sIdentityStatus);
            f_sDuplicateIdentity = normalize(sDuplicateIdentity);
            f_sReloadStatus = normalize(sReloadStatus);
            f_setLiveIssuers = liveIssuers == null ? Collections.emptySet() : liveIssuers;
            f_cCapabilityMembers = cMembers;
            f_cCapabilityCompatible = cCompatible;
            f_cCapabilityPending = cPending;
            f_fClosed = fClosed;
            }

        static State initial()
            {return new State(1L, null, "", "", "identity-unbound", "", "reload-unavailable",
                    Collections.emptySet(), 0, 0, 0, false);}

        static State closed(long lGeneration)
            {return new State(lGeneration, null, "", "", "closed", "", "closed",
                    Collections.emptySet(), 0, 0, 0, true);}

        State next()
            {return new State(f_lGeneration + 1L, f_snapshot, f_sMemberName, f_sIssuerId,
                    f_sIdentityStatus, f_sDuplicateIdentity, f_sReloadStatus, f_setLiveIssuers,
                    f_cCapabilityMembers, f_cCapabilityCompatible, f_cCapabilityPending, f_fClosed);}

        State withSnapshot(Snapshot snapshot, String sMemberName, String sIssuerId,
                String sIdentityStatus, String sReloadStatus)
            {return new State(f_lGeneration, snapshot, sMemberName, sIssuerId, sIdentityStatus,
                    f_sDuplicateIdentity, sReloadStatus, f_setLiveIssuers, f_cCapabilityMembers,
                    f_cCapabilityCompatible, f_cCapabilityPending, false);}

        State withReloadStatus(String sStatus)
            {return new State(f_lGeneration + 1L, f_snapshot, f_sMemberName, f_sIssuerId,
                    f_sIdentityStatus, f_sDuplicateIdentity, sStatus, f_setLiveIssuers,
                    f_cCapabilityMembers, f_cCapabilityCompatible, f_cCapabilityPending, false);}

        State withIdentitySet(String sDuplicate, Set<String> liveIssuers)
            {return new State(f_lGeneration + 1L, f_snapshot, f_sMemberName, f_sIssuerId,
                    f_sIdentityStatus, sDuplicate, f_sReloadStatus, liveIssuers,
                    f_cCapabilityMembers, f_cCapabilityCompatible, f_cCapabilityPending, false);}

        State withCapabilities(int cMembers, int cCompatible, int cPending)
            {return new State(f_lGeneration + 1L, f_snapshot, f_sMemberName, f_sIssuerId,
                    f_sIdentityStatus, f_sDuplicateIdentity, f_sReloadStatus, f_setLiveIssuers,
                    cMembers, cCompatible, cPending, false);}

        final long f_lGeneration;
        final Snapshot f_snapshot;
        final String f_sMemberName;
        final String f_sIssuerId;
        final String f_sIdentityStatus;
        final String f_sDuplicateIdentity;
        final String f_sReloadStatus;
        final Set<String> f_setLiveIssuers;
        final int f_cCapabilityMembers;
        final int f_cCapabilityCompatible;
        final int f_cCapabilityPending;
        final boolean f_fClosed;
        }

    /** Bounded source opener used by every supported credential read. */
    @FunctionalInterface
    interface CredentialReader
        {
        InputStream open(String sSource, CredentialControl control) throws IOException;
        }

    /** Cancellation/deadline control registered before backend open begins. */
    interface CredentialControl
        {
        void checkOpen() throws IOException;
        void onCancel(Runnable action) throws IOException;
        }

    /** Test-observable boundary around the pinned platform key-store sink. */
    @FunctionalInterface
    interface KeyStoreLoader
        {
        KeyStore load(String sType, byte[] bytes, char[] password) throws Exception;
        }

    /** Closed-profile key-store sink pinned to the platform provider. */
    private static final class PlatformKeyStoreLoader
            implements KeyStoreLoader
        {
        @Override
        public KeyStore load(String sType, byte[] bytes, char[] password) throws Exception
            {
            KeyStore store = KeyStore.getInstance(sType, JCA_KEYSTORE_PROVIDER);
            store.load(new ByteArrayInputStream(bytes), password);
            return store;
            }
        }

    /** Pre-parser bounds for the JKS binary grammar. */
    private static final class JksWorkFactor
        {
        static void validate(byte[] bytes) throws IOException
            {
            try
                {
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
                if (in.readInt() != JKS_MAGIC || in.readInt() != JKS_VERSION)
                    {
                    throw invalid();
                    }
                int cEntries = bounded(in.readInt(), MAX_KEYSTORE_ALIASES);
                for (int i = 0; i < cEntries; i++)
                    {
                    int nTag = in.readInt();
                    skipUtf(in);
                    in.readLong();
                    if (nTag == JKS_PRIVATE_KEY)
                        {
                        byte[] key = readBounded(in, MAX_ENCRYPTED_KEY_BYTES);
                        DerWorkFactor.validateEncoded(key);
                        int cChain = bounded(in.readInt(), MAX_IDENTITY_CHAIN_CERTIFICATES);
                        for (int j = 0; j < cChain; j++)
                            {
                            requireX509(in);
                            preflightCertificate(readBounded(in, MAX_CERTIFICATE_BYTES));
                            }
                        }
                    else if (nTag == JKS_TRUSTED_CERTIFICATE)
                        {
                        requireX509(in);
                        preflightCertificate(readBounded(in, MAX_CERTIFICATE_BYTES));
                        }
                    else
                        {
                        throw invalid();
                        }
                    }
                if (in.available() != JKS_DIGEST_BYTES)
                    {
                    throw invalid();
                    }
                }
            catch (EOFException e)
                {
                throw invalid(e);
                }
            }

        private static void requireX509(DataInputStream in) throws IOException
            {
            int cb = in.readUnsignedShort();
            if (cb != 5)
                {
                throw invalid();
                }
            byte[] bytes = new byte[cb];
            in.readFully(bytes);
            if (!"X.509".equals(new String(bytes, StandardCharsets.UTF_8)))
                {
                throw invalid();
                }
            }

        private static void skipUtf(DataInputStream in) throws IOException
            {
            int cb = in.readUnsignedShort();
            if (cb > MAX_ALIAS_UTF_BYTES)
                {
                throw new IOException("peer-proof key-store alias exceeds the supported encoded bound");
                }
            byte[] bytes = new byte[cb];
            in.readFully(bytes);
            }

        private static byte[] readBounded(DataInputStream in, int cbMaximum) throws IOException
            {
            int cb = in.readInt();
            if (cb <= 0 || cb > cbMaximum || cb > in.available())
                {
                throw invalid();
                }
            byte[] bytes = new byte[cb];
            in.readFully(bytes);
            return bytes;
            }

        private static int bounded(int c, int cMaximum) throws IOException
            {
            if (c < 0 || c > cMaximum)
                {
                throw new IOException("peer-proof key store exceeds the supported entry or chain bound");
                }
            return c;
            }

        private static IOException invalid()
            {return new IOException("invalid or unsupported peer-proof JKS structure");}

        private static IOException invalid(Exception cause)
            {return new IOException("invalid or unsupported peer-proof JKS structure", cause);}
        }

    /** Small, definite-length DER pass used before PKCS12 and X.509 parsers. */
    private static final class DerWorkFactor
        {
        static void validateEncoded(byte[] bytes) throws IOException
            {parse(bytes, false);}

        static void validateCertificate(byte[] bytes) throws IOException
            {
            Node root = parse(bytes, false);
            if (root.f_nTag != DER_SEQUENCE || root.f_listChildren.size() < 3)
                {
                throw invalid();
                }
            }

        static void validatePkcs12(byte[] bytes) throws IOException
            {
            Counter counter = new Counter();
            Node root = parse(bytes, true, counter, 0);
            if (root.f_nTag != DER_SEQUENCE || root.f_listChildren.size() < 2
                    || root.f_listChildren.size() > 3
                    || integer(root.f_listChildren.get(0)) != PKCS12_VERSION)
                {
                throw invalid();
                }
            validateAuthenticatedSafe(root.f_listChildren.get(1), counter);
            if (root.f_listChildren.size() == 3)
                {
                validateMacData(root.f_listChildren.get(2));
                }
            }

        /** Validate the supported outer integrity algorithm and parameter shape. */
        private static void validateMacData(Node mac) throws IOException
            {
            if (mac.f_nTag != DER_SEQUENCE || mac.f_listChildren.size() < 2
                    || mac.f_listChildren.size() > 3
                    || mac.f_listChildren.get(1).f_nTag != DER_OCTET_STRING
                    || mac.f_listChildren.get(1).f_abValue.length == 0)
                {
                throw invalid();
                }
            Node digestInfo = mac.f_listChildren.get(0);
            if (digestInfo.f_nTag != DER_SEQUENCE || digestInfo.f_listChildren.size() != 2
                    || digestInfo.f_listChildren.get(1).f_nTag != DER_OCTET_STRING)
                {
                throw invalid();
                }
            Node algorithm = digestInfo.f_listChildren.get(0);
            if (algorithm.f_nTag != DER_SEQUENCE || algorithm.f_listChildren.isEmpty()
                    || algorithm.f_listChildren.size() > 2)
                {
                throw invalid();
                }
            Integer cbDigest = OID_MAC_DIGEST.get(oid(algorithm.f_listChildren.get(0)));
            if (cbDigest == null || digestInfo.f_listChildren.get(1).f_abValue.length != cbDigest
                    || algorithm.f_listChildren.size() == 2
                    && !isNull(algorithm.f_listChildren.get(1)))
                {
                throw invalid();
                }
            if (mac.f_listChildren.size() == 3)
                {
                validateIterationCount(integer(mac.f_listChildren.get(2)));
                }
            }

        /** Validate the real PFX AuthenticatedSafe carried inside PKCS7 data. */
        private static void validateAuthenticatedSafe(Node authSafeInfo, Counter counter) throws IOException
            {
            byte[] encoded = contentOctets(authSafeInfo, OID_PKCS7_DATA);
            Node authenticatedSafe = parse(encoded, false, counter, 1);
            if (authenticatedSafe.f_nTag != DER_SEQUENCE || authenticatedSafe.f_listChildren.isEmpty())
                {
                throw invalid();
                }
            for (Node contentInfo : authenticatedSafe.f_listChildren)
                {
                validateSafeContentInfo(contentInfo, counter);
                }
            }

        /** Validate one supported data or encrypted-data ContentInfo. */
        private static void validateSafeContentInfo(Node contentInfo, Counter counter) throws IOException
            {
            String sType = contentType(contentInfo);
            if (OID_PKCS7_DATA.equals(sType))
                {
                validateSafeContents(parse(contentOctets(contentInfo, sType), false, counter, 2));
                }
            else if (OID_PKCS7_ENCRYPTED_DATA.equals(sType))
                {
                Node content = explicitContent(contentInfo);
                validateEncryptedData(content);
                }
            else
                {
                throw invalid();
                }
            }

        /** Validate visible safe bags and reject nested or uninspectable bag layouts. */
        private static void validateSafeContents(Node safeContents) throws IOException
            {
            if (safeContents.f_nTag != DER_SEQUENCE || safeContents.f_listChildren.isEmpty())
                {
                throw invalid();
                }
            for (Node bag : safeContents.f_listChildren)
                {
                if (bag.f_nTag != DER_SEQUENCE || bag.f_listChildren.size() < 2
                        || bag.f_listChildren.size() > 3)
                    {
                    throw invalid();
                    }
                String sBagType = oid(bag.f_listChildren.get(0));
                Node value = explicit(bag.f_listChildren.get(1));
                if (OID_PKCS12_SHROUDED_KEY_BAG.equals(sBagType))
                    {
                    if (value.f_nTag != DER_SEQUENCE || value.f_listChildren.size() != 2)
                        {
                        throw invalid();
                        }
                    validateEncryptionAlgorithm(value.f_listChildren.get(0));
                    Node encryptedData = value.f_listChildren.get(1);
                    if (encryptedData.f_nTag != DER_OCTET_STRING
                            || encryptedData.f_abValue.length == 0)
                        {
                        throw invalid();
                        }
                    }
                else if (OID_PKCS12_CERT_BAG.equals(sBagType))
                    {
                    validateCertificateBag(value);
                    }
                else
                    {
                    throw invalid();
                    }
                if (bag.f_listChildren.size() == 3)
                    {
                    validateBagAttributes(bag.f_listChildren.get(2));
                    }
                }
            }

        /** Validate the bounded attribute grammar emitted by the supported SUN store. */
        private static void validateBagAttributes(Node attributes) throws IOException
            {
            if (attributes.f_nTag != DER_SET || attributes.f_listChildren.isEmpty()
                    || attributes.f_listChildren.size() > MAX_SAFE_BAG_ATTRIBUTES)
                {
                throw invalid();
                }
            Set<String> setTypes = new HashSet<>();
            for (Node attribute : attributes.f_listChildren)
                {
                if (attribute.f_nTag != DER_SEQUENCE || attribute.f_listChildren.size() != 2)
                    {
                    throw invalid();
                    }
                String sType = oid(attribute.f_listChildren.get(0));
                Node values = attribute.f_listChildren.get(1);
                if (!setTypes.add(sType) || values.f_nTag != DER_SET
                        || values.f_listChildren.size() != 1)
                    {
                    throw invalid();
                    }
                Node value = values.f_listChildren.get(0);
                if (OID_PKCS9_FRIENDLY_NAME.equals(sType))
                    {
                    if (value.f_nTag != DER_BMP_STRING || value.f_abValue.length == 0
                            || (value.f_abValue.length & 1) != 0
                            || value.f_abValue.length > MAX_SAFE_BAG_ATTRIBUTE_BYTES)
                        {
                        throw invalid();
                        }
                    }
                else if (OID_PKCS9_LOCAL_KEY_ID.equals(sType))
                    {
                    if (value.f_nTag != DER_OCTET_STRING || value.f_abValue.length == 0
                            || value.f_abValue.length > MAX_SAFE_BAG_ATTRIBUTE_BYTES)
                        {
                        throw invalid();
                        }
                    }
                else if (OID_ORACLE_TRUSTED_KEY_USAGE.equals(sType))
                    {
                    if (!OID_ANY_EXTENDED_KEY_USAGE.equals(oid(value)))
                        {
                        throw invalid();
                        }
                    }
                else
                    {
                    throw invalid();
                    }
                }
            }

        /** Validate a visible, unencrypted X.509 certificate bag before JCA. */
        private static void validateCertificateBag(Node certBag) throws IOException
            {
            if (certBag.f_nTag != DER_SEQUENCE || certBag.f_listChildren.size() != 2
                    || !OID_PKCS9_X509_CERT.equals(oid(certBag.f_listChildren.get(0))))
                {
                throw invalid();
                }
            Node certificate = explicit(certBag.f_listChildren.get(1));
            if (certificate.f_nTag != DER_OCTET_STRING)
                {
                throw invalid();
                }
            preflightCertificate(certificate.f_abValue);
            }

        /** Validate encrypted certificate safe content and its visible KDF. */
        private static void validateEncryptedData(Node encryptedData) throws IOException
            {
            if (encryptedData.f_nTag != DER_SEQUENCE || encryptedData.f_listChildren.size() != 2
                    || integer(encryptedData.f_listChildren.get(0)) != 0)
                {
                throw invalid();
                }
            Node encryptedContentInfo = encryptedData.f_listChildren.get(1);
            if (encryptedContentInfo.f_nTag != DER_SEQUENCE
                    || encryptedContentInfo.f_listChildren.size() != 3
                    || !OID_PKCS7_DATA.equals(oid(encryptedContentInfo.f_listChildren.get(0))))
                {
                throw invalid();
                }
            validateEncryptionAlgorithm(encryptedContentInfo.f_listChildren.get(1));
            Node encryptedContent = encryptedContentInfo.f_listChildren.get(2);
            if (encryptedContent.f_nTag != DER_CONTEXT_0_PRIMITIVE
                    || encryptedContent.f_abValue.length == 0)
                {
                throw invalid();
                }
            }

        /** Validate PBES2/PBKDF2 or retained PKCS12 legacy-PBE parameters. */
        private static void validateEncryptionAlgorithm(Node algorithm) throws IOException
            {
            if (algorithm.f_nTag != DER_SEQUENCE || algorithm.f_listChildren.size() != 2)
                {
                throw invalid();
                }
            String sOid = oid(algorithm.f_listChildren.get(0));
            Node parameters = algorithm.f_listChildren.get(1);
            if (OID_PBES2.equals(sOid))
                {
                if (parameters.f_nTag != DER_SEQUENCE || parameters.f_listChildren.size() != 2)
                    {
                    throw invalid();
                    }
                int cbKey = validateAes(parameters.f_listChildren.get(1));
                validatePbkdf2(parameters.f_listChildren.get(0), cbKey);
                }
            else if (OID_PKCS12_PBE.contains(sOid))
                {
                if (parameters.f_nTag != DER_SEQUENCE || parameters.f_listChildren.size() != 2
                        || parameters.f_listChildren.get(0).f_nTag != DER_OCTET_STRING)
                    {
                    throw invalid();
                    }
                validateIterationCount(integer(parameters.f_listChildren.get(1)));
                }
            else
                {
                throw invalid();
                }
            }

        private static void validatePbkdf2(Node algorithm, int cbKey) throws IOException
            {
            if (algorithm.f_nTag != DER_SEQUENCE || algorithm.f_listChildren.size() != 2
                    || !OID_PBKDF2.equals(oid(algorithm.f_listChildren.get(0))))
                {
                throw invalid();
                }
            Node parameters = algorithm.f_listChildren.get(1);
            if (parameters.f_nTag != DER_SEQUENCE || parameters.f_listChildren.size() < 2
                    || parameters.f_listChildren.size() > 4
                    || parameters.f_listChildren.get(0).f_nTag != DER_OCTET_STRING)
                {
                throw invalid();
                }
            validateIterationCount(integer(parameters.f_listChildren.get(1)));
            int i = 2;
            if (i < parameters.f_listChildren.size()
                    && parameters.f_listChildren.get(i).f_nTag == DER_INTEGER)
                {
                if (integer(parameters.f_listChildren.get(i++)) != cbKey)
                    {
                    throw invalid();
                    }
                }
            if (i < parameters.f_listChildren.size())
                {
                Node prf = parameters.f_listChildren.get(i++);
                if (prf.f_nTag != DER_SEQUENCE || prf.f_listChildren.isEmpty()
                        || prf.f_listChildren.size() > 2
                        || !OID_HMAC.contains(oid(prf.f_listChildren.get(0)))
                        || prf.f_listChildren.size() == 2 && !isNull(prf.f_listChildren.get(1)))
                    {
                    throw invalid();
                    }
                }
            if (i != parameters.f_listChildren.size())
                {
                throw invalid();
                }
            }

        private static int validateAes(Node algorithm) throws IOException
            {
            if (algorithm.f_nTag != DER_SEQUENCE || algorithm.f_listChildren.size() != 2)
                {
                throw invalid();
                }
            Integer cbKey = OID_AES_CBC.get(oid(algorithm.f_listChildren.get(0)));
            Node iv = algorithm.f_listChildren.get(1);
            if (cbKey == null || iv.f_nTag != DER_OCTET_STRING || iv.f_abValue.length != AES_BLOCK_BYTES)
                {
                throw invalid();
                }
            return cbKey;
            }

        private static byte[] contentOctets(Node contentInfo, String sExpectedType) throws IOException
            {
            if (!sExpectedType.equals(contentType(contentInfo)))
                {
                throw invalid();
                }
            Node content = explicitContent(contentInfo);
            if (content.f_nTag != DER_OCTET_STRING || content.f_abValue == null)
                {
                throw invalid();
                }
            return content.f_abValue;
            }

        private static String contentType(Node contentInfo) throws IOException
            {
            if (contentInfo.f_nTag != DER_SEQUENCE || contentInfo.f_listChildren.size() != 2
                    || contentInfo.f_listChildren.get(0).f_nTag != DER_OID)
                {
                throw invalid();
                }
            return oid(contentInfo.f_listChildren.get(0));
            }

        private static Node explicitContent(Node contentInfo) throws IOException
            {return explicit(contentInfo.f_listChildren.get(1));}

        private static Node explicit(Node node) throws IOException
            {
            if (node.f_nTag != DER_CONTEXT_0_CONSTRUCTED || node.f_listChildren.size() != 1)
                {
                throw invalid();
                }
            return node.f_listChildren.get(0);
            }

        private static Node parse(byte[] bytes, boolean fPkcs12) throws IOException
            {
            return parse(bytes, fPkcs12, new Counter(), 0);
            }

        private static Node parse(byte[] bytes, boolean fPkcs12, Counter counter, int cDepth)
                throws IOException
            {
            Position position = new Position();
            Node root = node(bytes, position, bytes.length, cDepth, counter);
            if (position.f_nOffset != bytes.length)
                {
                throw invalid();
                }
            if (fPkcs12 && bytes.length > MAX_KEYSTORE_BYTES)
                {
                throw invalid();
                }
            return root;
            }

        private static Node node(byte[] bytes, Position position, int nLimit, int cDepth,
                Counter counter) throws IOException
            {
            if (++counter.f_cNodes > MAX_DER_NODES || cDepth > MAX_DER_DEPTH
                    || position.f_nOffset >= nLimit)
                {
                throw invalid();
                }
            int nTag = bytes[position.f_nOffset++] & 0xff;
            if ((nTag & DER_HIGH_TAG) == DER_HIGH_TAG)
                {
                throw invalid();
                }
            int cb = length(bytes, position, nLimit);
            int nEnd = position.f_nOffset + cb;
            if (cb < 0 || cb > MAX_DER_VALUE_BYTES || nEnd < position.f_nOffset || nEnd > nLimit)
                {
                throw invalid();
                }
            List<Node> children = Collections.emptyList();
            byte[] value = null;
            if ((nTag & DER_CONSTRUCTED) != 0)
                {
                children = new ArrayList<>();
                while (position.f_nOffset < nEnd)
                    {
                    if (children.size() >= MAX_DER_CHILDREN)
                        {
                        throw invalid();
                        }
                    children.add(node(bytes, position, nEnd, cDepth + 1, counter));
                    }
                }
            else
                {
                value = Arrays.copyOfRange(bytes, position.f_nOffset, nEnd);
                position.f_nOffset = nEnd;
                }
            if (position.f_nOffset != nEnd)
                {
                throw invalid();
                }
            return new Node(nTag, value, children);
            }

        private static int length(byte[] bytes, Position position, int nLimit) throws IOException
            {
            if (position.f_nOffset >= nLimit)
                {
                throw invalid();
                }
            int n = bytes[position.f_nOffset++] & 0xff;
            if ((n & 0x80) == 0)
                {
                return n;
                }
            int cbLength = n & 0x7f;
            if (cbLength == 0 || cbLength > 4 || position.f_nOffset + cbLength > nLimit
                    || bytes[position.f_nOffset] == 0)
                {
                throw invalid();
                }
            long cb = 0;
            for (int i = 0; i < cbLength; i++)
                {
                cb = cb << 8 | bytes[position.f_nOffset++] & 0xff;
                }
            if (cb < 128 || cb > Integer.MAX_VALUE)
                {
                throw invalid();
                }
            return (int) cb;
            }

        private static void validateIterationCount(long cIterations) throws IOException
            {
            if (cIterations < 1 || cIterations > MAX_PBE_ITERATIONS)
                {
                throw new IOException("peer-proof key derivation exceeds the supported work bound");
                }
            }

        private static long integer(Node node) throws IOException
            {
            byte[] bytes = node.f_abValue;
            if (node.f_nTag != DER_INTEGER || bytes == null || bytes.length == 0 || bytes.length > 8
                    || (bytes[0] & 0x80) != 0
                    || bytes.length > 1 && bytes[0] == 0 && (bytes[1] & 0x80) == 0)
                {
                throw invalid();
                }
            long value = 0;
            for (byte b : bytes)
                {
                value = value << 8 | b & 0xff;
                }
            return value;
            }

        private static String oid(Node node) throws IOException
            {
            if (node.f_nTag != DER_OID)
                {
                throw invalid();
                }
            return oid(node.f_abValue);
            }

        private static boolean isNull(Node node)
            {
            return node.f_nTag == DER_NULL && node.f_abValue.length == 0;
            }

        private static String oid(byte[] bytes) throws IOException
            {
            if (bytes == null || bytes.length == 0)
                {
                throw invalid();
                }
            StringBuilder builder = new StringBuilder();
            int nFirst = bytes[0] & 0xff;
            builder.append(Math.min(2, nFirst / 40)).append('.').append(nFirst < 80 ? nFirst % 40 : nFirst - 80);
            long value = 0;
            boolean fFirst = true;
            for (int i = 1; i < bytes.length; i++)
                {
                int n = bytes[i] & 0xff;
                if (fFirst && n == 0x80 || value > (Long.MAX_VALUE >>> 7))
                    {
                    throw invalid();
                    }
                value = value << 7 | n & 0x7f;
                if ((n & 0x80) == 0)
                    {
                    builder.append('.').append(value);
                    value = 0;
                    fFirst = true;
                    }
                else
                    {
                    fFirst = false;
                    }
                }
            if ((bytes[bytes.length - 1] & 0x80) != 0)
                {
                throw invalid();
                }
            return builder.toString();
            }

        private static IOException invalid()
            {return new IOException("invalid or unsupported peer-proof DER structure");}

        private static final class Position
            {int f_nOffset;}

        private static final class Counter
            {int f_cNodes;}

        private static final class Node
            {
            Node(int nTag, byte[] value, List<Node> children)
                {f_nTag = nTag; f_abValue = value; f_listChildren = children;}
            final int f_nTag;
            final byte[] f_abValue;
            final List<Node> f_listChildren;
            }
        }

    /** Pre-parser bounds and strict framing for supported PEM authorities. */
    private static final class PemWorkFactor
        {
        static List<byte[]> decode(byte[] bytes, int cMaximum) throws IOException
            {
            if (cMaximum < 0)
                {
                throw new IOException("authority set exceeds the supported certificate bound");
                }
            String value = new String(bytes, StandardCharsets.US_ASCII);
            List<byte[]> certificates = new ArrayList<>();
            int n = 0;
            while (true)
                {
                n = whitespace(value, n);
                if (n == value.length())
                    {
                    break;
                    }
                if (certificates.size() >= cMaximum)
                    {
                    throw new IOException("authority set exceeds the supported certificate bound");
                    }
                if (!value.startsWith(PEM_BEGIN, n))
                    {
                    throw new IOException("invalid peer-proof PEM authority material");
                    }
                n += PEM_BEGIN.length();
                int nEnd = value.indexOf(PEM_END, n);
                if (nEnd < 0)
                    {
                    throw new IOException("invalid peer-proof PEM authority material");
                    }
                StringBuilder encoded = new StringBuilder();
                for (int i = n; i < nEnd; i++)
                    {
                    char ch = value.charAt(i);
                    if (!Character.isWhitespace(ch))
                        {
                        if (!isBase64(ch) || encoded.length() >= MAX_PEM_BASE64_CHARS)
                            {
                            throw new IOException("peer-proof PEM certificate exceeds the supported encoded bound");
                            }
                        encoded.append(ch);
                        }
                    }
                byte[] certificate;
                try
                    {
                    certificate = Base64.getDecoder().decode(encoded.toString());
                    }
                catch (IllegalArgumentException e)
                    {
                    throw new IOException("invalid peer-proof PEM authority material", e);
                    }
                preflightCertificate(certificate);
                certificates.add(certificate);
                n = nEnd + PEM_END.length();
                }
            if (certificates.isEmpty())
                {
                throw new IOException("peer-proof PEM authority material is empty");
                }
            return certificates;
            }

        private static int whitespace(String value, int n)
            {
            while (n < value.length() && Character.isWhitespace(value.charAt(n)))
                {
                n++;
                }
            return n;
            }

        private static boolean isBase64(char ch)
            {return ch >= 'A' && ch <= 'Z' || ch >= 'a' && ch <= 'z'
                    || ch >= '0' && ch <= '9' || ch == '+' || ch == '/' || ch == '=';}
        }

    /** Owner-approved first-profile reader for local regular files only. */
    private static final class FileCredentialReader
            implements CredentialReader
        {
        @Override
        public InputStream open(String sSource, CredentialControl control) throws IOException
            {
            control.checkOpen();
            Path path;
            try
                {
                URI uri = URI.create(sSource);
                String sScheme = uri.getScheme();
                if (sScheme == null || sScheme.isEmpty())
                    {
                    path = Path.of(sSource);
                    }
                else if ("file".equalsIgnoreCase(sScheme) && uri.getAuthority() == null
                        && uri.getQuery() == null && uri.getFragment() == null)
                    {
                    path = Path.of(uri);
                    }
                else
                    {
                    throw new IOException("unsupported peer-proof credential source");
                    }
                }
            catch (IllegalArgumentException e)
                {
                throw new IOException("invalid peer-proof credential source", e);
                }
            if (!path.isAbsolute())
                {
                throw new IOException("peer-proof credential source must be an absolute local path");
                }
            path = path.normalize();
            control.checkOpen();
            if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                {
                throw new IOException("peer-proof credential source must be a local regular file");
                }
            control.checkOpen();
            return Channels.newInputStream(Files.newByteChannel(path,
                    StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS));
            }
        }

    /** Per-refresh absolute-deadline scope that owns every backend operation. */
    private final class ReadScope
            implements CredentialControl
        {
        ReadScope(long lDeadline)
            {
            f_lDeadline = lDeadline;
            }

        void startDeadline()
            {
            f_futureDeadline = f_ioExecutor.schedule(this::cancel,
                    remainingNanos(f_lDeadline), TimeUnit.NANOSECONDS);
            }

        /** Execute the complete credential operation on provider-owned work. */
        <T> T execute(Callable<T> callable) throws Exception
            {
            checkOpen();
            Future<T> future = f_ioExecutor.submit(() ->
                {
                try
                    {
                    synchronized (ReadScope.this)
                        {
                        if (m_nState != SCOPE_OPEN)
                            {
                            throw new IOException("credential operation cancelled");
                            }
                        m_threadOwner = Thread.currentThread();
                        }
                    checkOpen();
                    T result = callable.call();
                    checkOpen();
                    return result;
                    }
                finally
                    {
                    synchronized (ReadScope.this)
                        {
                        if (m_threadOwner == Thread.currentThread())
                            {
                            m_threadOwner = null;
                            }
                        }
                    f_workComplete.countDown();
                    }
                });
            boolean fCancelled;
            synchronized (this)
                {
                fCancelled = m_nState != SCOPE_OPEN;
                if (!fCancelled)
                    {
                    m_futureWork = future;
                    }
                }
            if (fCancelled)
                {
                future.cancel(true);
                awaitCancellationQuiescence();
                throw new IOException("credential operation cancelled");
                }
            try
                {
                return future.get(remainingNanos(f_lDeadline), TimeUnit.NANOSECONDS);
                }
            catch (InterruptedException e)
                {
                future.cancel(true);
                cancel();
                Thread.currentThread().interrupt();
                throw new IOException("credential operation interrupted", e);
                }
            catch (TimeoutException | IllegalStateException e)
                {
                future.cancel(true);
                cancel();
                throw new IOException("credential operation cancelled or timed out", e);
                }
            catch (ExecutionException e)
                {
                Throwable cause = e.getCause();
                if (cause instanceof Exception)
                    {
                    throw (Exception) cause;
                    }
                if (cause instanceof Error)
                    {
                    throw (Error) cause;
                    }
                throw new IOException("credential operation failed", cause);
                }
            finally
                {
                if (isCancelled())
                    {
                    awaitCancellationQuiescence();
                    }
                synchronized (this)
                    {
                    if (m_futureWork == future)
                        {
                        m_futureWork = null;
                        }
                    }
                }
            }

        InputStream open(String sUrl) throws IOException
            {
            checkOpen();
            InputStream backend = f_credentialReader.open(sUrl, this);
            TrackedInputStream tracked = new TrackedInputStream(backend, this);
            synchronized (this)
                {
                if (m_nState != SCOPE_OPEN)
                    {
                    tracked.requestClose();
                    throw new IOException("credential read cancelled");
                    }
                m_setStreams.add(tracked);
                }
            return tracked;
            }

        @Override
        public void checkOpen() throws IOException
            {
            synchronized (this)
                {
                if (m_nState != SCOPE_OPEN || System.nanoTime() >= f_lDeadline)
                    {
                    throw new IOException("credential operation cancelled or timed out");
                    }
                }
            }

        @Override
        public void onCancel(Runnable action) throws IOException
            {
            if (action == null)
                {
                throw new IOException("credential cancellation action is required");
                }
            boolean fRun;
            synchronized (this)
                {
                fRun = m_nState != SCOPE_OPEN;
                if (!fRun)
                    {
                    m_listCancelActions.add(action);
                    }
                }
            if (fRun)
                {
                runCancelAction(action);
                }
            }

        void cancel()
            {
            List<TrackedInputStream> streams;
            List<Runnable> actions;
            Future<?> futureWork;
            Thread threadOwner;
            synchronized (this)
                {
                if (m_nState != SCOPE_OPEN)
                    {
                    return;
                    }
                m_nState = SCOPE_CANCELLED;
                streams = new ArrayList<>(m_setStreams);
                actions = new ArrayList<>(m_listCancelActions);
                m_listCancelActions.clear();
                futureWork = m_futureWork;
                threadOwner = m_threadOwner;
                }
            try
                {
                ScheduledFuture<?> future = f_futureDeadline;
                if (future != null)
                    {
                    future.cancel(false);
                    }
                if (futureWork != null)
                    {
                    futureWork.cancel(true);
                    }
                if (threadOwner != null)
                    {
                    threadOwner.interrupt();
                    }
                for (Runnable action : actions)
                    {
                    runCancelAction(action);
                    }
                for (TrackedInputStream stream : streams)
                    {
                    stream.requestClose();
                    }
                }
            finally
                {
                f_cancellationComplete.countDown();
                }
            }

        void complete()
            {
            synchronized (this)
                {
                if (m_nState == SCOPE_OPEN)
                    {
                    m_nState = SCOPE_CANCELLED;
                    }
                }
            ScheduledFuture<?> future = f_futureDeadline;
            if (future != null)
                {
                future.cancel(false);
                }
            }

        boolean tryCommit()
            {
            synchronized (this)
                {
                if (m_nState != SCOPE_OPEN || System.nanoTime() >= f_lDeadline)
                    {
                    if (m_nState == SCOPE_OPEN)
                        {
                        m_nState = SCOPE_CANCELLED;
                        }
                    return false;
                    }
                m_nState = SCOPE_COMMITTED;
                }
            ScheduledFuture<?> future = f_futureDeadline;
            if (future != null)
                {
                future.cancel(false);
                }
            return true;
            }

        private void released(TrackedInputStream stream)
            {
            synchronized (this) {m_setStreams.remove(stream);}
            }

        private synchronized void trackCleanup(Future<?> future)
            {
            m_setCleanupWork.add(future);
            }

        /**
         * Wait a bounded grace interval for cancelled backend work to leave
         * both the credential worker and cleanup executor.  Cancelling a
         * {@link Future} marks it complete before its running task has
         * necessarily observed interruption, so Future.get() alone is not a
         * lifecycle boundary.
         */
        private void awaitCancellationQuiescence()
            {
            long lDeadline = System.nanoTime()
                    + TimeUnit.MILLISECONDS.toNanos(CREDENTIAL_CANCELLATION_TIMEOUT_MILLIS);
            try
                {
                long cNanos = lDeadline - System.nanoTime();
                if (cNanos <= 0L || !f_cancellationComplete.await(cNanos, TimeUnit.NANOSECONDS))
                    {
                    return;
                    }

                cNanos = lDeadline - System.nanoTime();
                if (cNanos <= 0L || !f_workComplete.await(cNanos, TimeUnit.NANOSECONDS))
                    {
                    return;
                    }

                List<Future<?>> listCleanup;
                synchronized (this)
                    {
                    listCleanup = new ArrayList<>(m_setCleanupWork);
                    }
                for (Future<?> future : listCleanup)
                    {
                    cNanos = lDeadline - System.nanoTime();
                    if (cNanos <= 0L)
                        {
                        return;
                        }
                    try
                        {
                        future.get(cNanos, TimeUnit.NANOSECONDS);
                        }
                    catch (ExecutionException ignored)
                        {
                        // The backend is quiescent even when its close failed.
                        }
                    }
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                }
            catch (TimeoutException | IllegalStateException ignored)
                {
                // Preserve the bounded refresh contract for hostile backends.
                }
            }

        private void await(Future<?> future) throws IOException
            {
            try
                {
                future.get(remainingNanos(f_lDeadline), TimeUnit.NANOSECONDS);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw new IOException("credential operation interrupted", e);
                }
            catch (ExecutionException e)
                {
                Throwable cause = e.getCause();
                if (cause instanceof IOException)
                    {
                    throw (IOException) cause;
                    }
                throw new IOException("credential backend close failed", cause);
                }
            catch (TimeoutException | IllegalStateException e)
                {
                throw new IOException("credential backend close timed out", e);
                }
            }

        private void runCancelAction(Runnable action)
            {
            try {action.run();} catch (RuntimeException ignored) {}
            }

        private synchronized boolean isCancelled()
            {return m_nState == SCOPE_CANCELLED || m_nState == SCOPE_OPEN && System.nanoTime() >= f_lDeadline;}

        private final long f_lDeadline;
        private int m_nState = SCOPE_OPEN;
        private final Set<TrackedInputStream> m_setStreams = new HashSet<>();
        private final Set<Future<?>> m_setCleanupWork = new HashSet<>();
        private final List<Runnable> m_listCancelActions = new ArrayList<>();
        private final CountDownLatch f_cancellationComplete = new CountDownLatch(1);
        private final CountDownLatch f_workComplete = new CountDownLatch(1);
        private volatile Thread m_threadOwner;
        private volatile Future<?> m_futureWork;
        private volatile ScheduledFuture<?> f_futureDeadline;
        }

    /** Stream wrapper that checks lifecycle cancellation at the real byte-read boundary. */
    private final class TrackedInputStream
            extends FilterInputStream
        {
        TrackedInputStream(InputStream in, ReadScope scope)
            {super(in); f_scope = scope;}

        @Override
        public int read() throws IOException
            {check(); return super.read();}

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException
            {check(); return super.read(bytes, offset, length);}

        @Override
        public void close() throws IOException
            {
            try
                {
                f_scope.await(requestClose());
                }
            finally
                {
                f_scope.released(this);
                }
            }

        synchronized Future<?> requestClose()
            {
            if (m_futureClose == null)
                {
                m_fClosed = true;
                try
                    {
                    m_futureClose = f_cleanupExecutor.submit(() ->
                        {
                        super.close();
                        return null;
                        });
                    }
                catch (RejectedExecutionException e)
                    {
                    // A close race can discover a stream after first-close
                    // shutdown.  Run that one cleanup on its already-owned
                    // credential worker rather than abandon the backend.
                    FutureTask<Void> task = new FutureTask<>(() ->
                        {
                        super.close();
                        return null;
                        });
                    m_futureClose = task;
                    task.run();
                    }
                }
            f_scope.trackCleanup(m_futureClose);
            return m_futureClose;
            }

        private void check() throws IOException
            {
            if (m_fClosed || f_scope.isCancelled()) {throw new IOException("credential read cancelled");}
            }

        private final ReadScope f_scope;
        private volatile boolean m_fClosed;
        private Future<?> m_futureClose;
        }

    private static class Snapshot
        {
        Snapshot(PrivateKey key, X509Certificate cert, String keyId,
                Map<String, X509Certificate> subject, Map<String, X509Certificate> senior)
            {f_key = key; f_certIdentity = cert; f_sKeyId = keyId; f_mapSubject = subject; f_mapSenior = senior;}
        final PrivateKey f_key;
        final X509Certificate f_certIdentity;
        final String f_sKeyId;
        final Map<String, X509Certificate> f_mapSubject;
        final Map<String, X509Certificate> f_mapSenior;
        }

    @FunctionalInterface
    interface RefreshObserver
        {
        void snapshotLoaded(long lGeneration) throws Exception;
        }

    public static final String PROVIDER_ID = "x509-signature-v1";
    public static final String ALGORITHM_ID = "rsa-sha256";
    public static final String JCA_SIGNATURE = "SHA256withRSA";
    public static final String KEY_ID_PREFIX = "x509-sha256:";
    public static final String ISSUER_PREFIX = "urn:coherence:member:utf16be:";
    public static final int MIN_RSA_BITS = 2048;
    public static final long MAX_PROOF_VALIDITY_MILLIS = ProofTimePolicy.MAX_PROOF_VALIDITY_MILLIS;
    public static final long CLOCK_SKEW_MILLIS = ProofTimePolicy.CLOCK_SKEW_MILLIS;
    /** Absolute deadline for one complete credential snapshot operation. */
    public static final long CREDENTIAL_OPERATION_TIMEOUT_MILLIS = 5_000L;
    /** Bounded grace interval for a cancelled backend to reach quiescence. */
    static final long CREDENTIAL_CANCELLATION_TIMEOUT_MILLIS = 1_000L;
    /** Maximum provider close wait for its scheduled executor. */
    public static final long CLOSE_TIMEOUT_MILLIS = 5_000L;
    /** Maximum supported bytes in one credential object. */
    public static final int MAX_CREDENTIAL_BYTES = 1024 * 1024;
    /** Key stores retain the one-MiB credential-object ceiling. */
    public static final int MAX_KEYSTORE_BYTES = MAX_CREDENTIAL_BYTES;
    public static final int MAX_KEYSTORE_ALIASES = 16;
    public static final int MAX_AUTHORITY_CERTIFICATES = 16;
    public static final int MAX_IDENTITY_CHAIN_CERTIFICATES = 4;
    public static final int MAX_PASSWORD_CHARS = 1_024;
    /** Closed-profile limits applied before JCA or certificate parsing. */
    public static final int MAX_CERTIFICATE_BYTES = 32 * 1024;
    public static final int MAX_ENCRYPTED_KEY_BYTES = 64 * 1024;
    public static final int MAX_ALIAS_UTF_BYTES = 1_024;
    public static final int MAX_DER_VALUE_BYTES = MAX_CREDENTIAL_BYTES;
    public static final int MAX_DER_NODES = 4_096;
    public static final int MAX_DER_DEPTH = 24;
    public static final int MAX_DER_CHILDREN = 256;
    public static final int MAX_PBE_ITERATIONS = 100_000;
    public static final int MAX_PEM_BASE64_CHARS = ((MAX_CERTIFICATE_BYTES + 2) / 3) * 4;
    private static final int JKS_MAGIC = 0xfeedfeed;
    private static final int JKS_VERSION = 2;
    private static final int JKS_PRIVATE_KEY = 1;
    private static final int JKS_TRUSTED_CERTIFICATE = 2;
    private static final int JKS_DIGEST_BYTES = 20;
    private static final int DER_INTEGER = 0x02;
    private static final int DER_OCTET_STRING = 0x04;
    private static final int DER_NULL = 0x05;
    private static final int DER_OID = 0x06;
    private static final int DER_BMP_STRING = 0x1e;
    private static final int DER_SEQUENCE = 0x30;
    private static final int DER_SET = 0x31;
    private static final int DER_CONTEXT_0_PRIMITIVE = 0x80;
    private static final int DER_CONTEXT_0_CONSTRUCTED = 0xa0;
    private static final int DER_CONSTRUCTED = 0x20;
    private static final int DER_HIGH_TAG = 0x1f;
    private static final int PKCS12_VERSION = 3;
    private static final String OID_PBKDF2 = "1.2.840.113549.1.5.12";
    private static final String OID_PBES2 = "1.2.840.113549.1.5.13";
    private static final String OID_PKCS7_DATA = "1.2.840.113549.1.7.1";
    private static final String OID_PKCS7_ENCRYPTED_DATA = "1.2.840.113549.1.7.6";
    private static final String OID_PKCS12_SHROUDED_KEY_BAG = "1.2.840.113549.1.12.10.1.2";
    private static final String OID_PKCS12_CERT_BAG = "1.2.840.113549.1.12.10.1.3";
    private static final String OID_PKCS9_X509_CERT = "1.2.840.113549.1.9.22.1";
    private static final String OID_PKCS9_FRIENDLY_NAME = "1.2.840.113549.1.9.20";
    private static final String OID_PKCS9_LOCAL_KEY_ID = "1.2.840.113549.1.9.21";
    private static final String OID_ORACLE_TRUSTED_KEY_USAGE = "2.16.840.1.113894.746875.1.1";
    private static final String OID_ANY_EXTENDED_KEY_USAGE = "2.5.29.37.0";
    private static final Set<String> OID_PKCS12_PBE = Set.of("1.2.840.113549.1.12.1.1",
            "1.2.840.113549.1.12.1.2", "1.2.840.113549.1.12.1.3",
            "1.2.840.113549.1.12.1.4", "1.2.840.113549.1.12.1.5",
            "1.2.840.113549.1.12.1.6");
    private static final Set<String> OID_HMAC = Set.of("1.2.840.113549.2.7",
            "1.2.840.113549.2.8", "1.2.840.113549.2.9", "1.2.840.113549.2.10",
            "1.2.840.113549.2.11");
    private static final Map<String, Integer> OID_AES_CBC = Map.of(
            "2.16.840.1.101.3.4.1.2", 16,
            "2.16.840.1.101.3.4.1.42", 32);
    private static final Map<String, Integer> OID_MAC_DIGEST = Map.of(
            "2.16.840.1.101.3.4.2.1", 32,
            "2.16.840.1.101.3.4.2.2", 48,
            "2.16.840.1.101.3.4.2.3", 64);
    private static final int AES_BLOCK_BYTES = 16;
    private static final int MAX_SAFE_BAG_ATTRIBUTES = 3;
    private static final int MAX_SAFE_BAG_ATTRIBUTE_BYTES = 256;
    private static final String PEM_BEGIN = "-----BEGIN CERTIFICATE-----";
    private static final String PEM_END = "-----END CERTIFICATE-----";
    private static final String JCA_KEYSTORE_PROVIDER = "SUN";
    private static final String JCA_CERTIFICATE_PROVIDER = "SUN";
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    /** Create owned credential capacity that removes cancelled deadline tasks immediately. */
    private static ScheduledThreadPoolExecutor newCredentialIoExecutor()
        {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(4, r ->
            {Thread thread = new Thread(r, "PeerProofCredentialIO"); thread.setDaemon(true); return thread;});
        executor.setRemoveOnCancelPolicy(true);
        return executor;
        }

    private volatile PeerProofDependencies m_deps;
    private final Object f_lifecycleLock = new Object();
    private final RefreshObserver f_refreshObserver;
    private final CredentialReader f_credentialReader;
    private final KeyStoreLoader f_keyStoreLoader;
    private final long f_cCredentialOperationTimeoutMillis;
    private final AtomicReference<State> f_state = new AtomicReference<>(State.initial());
    private final ScheduledExecutorService f_executor = Executors.newSingleThreadScheduledExecutor(r ->
        {Thread thread = new Thread(r, "PeerProofRefresh"); thread.setDaemon(true); return thread;});
    private final ScheduledExecutorService f_ioExecutor = newCredentialIoExecutor();
    private final ExecutorService f_cleanupExecutor = Executors.newFixedThreadPool(4, r ->
        {Thread thread = new Thread(r, "PeerProofCredentialCleanup"); thread.setDaemon(true); return thread;});
    private final Set<ReadScope> f_setReadScopes = new HashSet<>();
    private long m_lRefreshGeneration;
    private long m_lCommittedRefreshGeneration;
    private int m_cActiveRefresh;
    private boolean m_fCloseComplete;

    private static final int SCOPE_OPEN = 0;
    private static final int SCOPE_CANCELLED = 1;
    private static final int SCOPE_COMMITTED = 2;
    }
