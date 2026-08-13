/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.security;

import com.tangosol.coherence.component.net.Security;

import com.tangosol.io.internal.SerializationAllowlist;

import com.tangosol.net.PasswordProvider;

import org.junit.AfterClass;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Product-credential tests for the PEER X.509 proof provider.
 *
 * @author Aleks Seovic  2026.07.15
 * @since 26.1.0.0
 */
public class X509PeerProofProviderTest
    {
    @BeforeClass
    public static void createCredentials() throws Exception
        {
        s_dir         = Files.createTempDirectory("peer-proof-provider-");
        s_memberOne   = credential("member-one", "KU=digitalSignature");
        s_memberTwo   = credential("member-two", "KU=digitalSignature");
        s_memberOneNew = credentialForMember("member-one-new", "member-one", "KU=digitalSignature");
        s_memberOneNewTwo = credentialForMember("member-one-new-two", "member-one", "KU=digitalSignature");
        s_memberThree = credential("member-three", "KU=digitalSignature");
        s_memberFour = credential("member-four", "KU=digitalSignature");
        s_memberFourNew = credentialForMember("member-four-new", "member-four", "KU=digitalSignature");
        s_ca          = credential("member-ca", "BC=ca:true", "KU=digitalSignature");
        s_noUsage     = credential("member-no-usage");
        s_wrongUsage  = credential("member-wrong-usage", "KU=keyEncipherment");
        s_expired     = expiredCredential("member-expired");
        for (int i = 0; i < 9; i++)
            {
            s_complexityCredentials.add(credential("member-complexity-" + i, "KU=digitalSignature"));
            }
        }

    @AfterClass
    public static void cleanup() throws Exception
        {
        if (s_dir != null)
            {
            try (java.util.stream.Stream<Path> stream = Files.walk(s_dir))
                {
                for (Path path : (Iterable<Path>) stream.sorted(Comparator.reverseOrder())::iterator)
                    {
                    Files.deleteIfExists(path);
                    }
                }
            }
        }

    @After
    public void closeProviders()
        {
        for (X509PeerProofProvider provider : s_providers)
            {
            provider.close();
            }
        s_providers.clear();
        }

    @Test
    public void shouldProduceAndVerifyBothProofRoles()
        {
        X509PeerProofProvider provider = provider();
        long now = System.currentTimeMillis();
        SubjectProofPayload subject = subject(provider, now, now + 60_000L);
        SeniorMetadataProofPayload senior = senior(provider, now, now + 60_000L);

        assertTrue(provider.isReady());
        assertTrue(provider.getKeyId().matches("x509-sha256:[0-9a-f]{64}"));
        assertEquals(X509PeerProofProvider.issuerId("member-one"), provider.getIssuerId());
        assertTrue(provider.verifyProof(provider.createProof(subject), subject, now).isValid());
        assertTrue(provider.verifyProof(provider.createProof(senior), senior, now).isValid());
        assertTrue(provider.isSeniorMetadataProofAuthority(senior));
        }

    @Test
    public void shouldFailClosedForDuplicateIdentityAndRecover()
        {
        X509PeerProofProvider provider = provider();
        long now = System.currentTimeMillis();
        SubjectProofPayload payload = subject(provider, now, now + 60_000L);
        byte[] proof = provider.createProof(payload);

        provider.observeMemberNames(Arrays.asList("member-one", "member-one"));
        assertFalse(provider.isReady());
        assertTrue(provider.getReadinessStatus().startsWith("duplicate-identity:"));
        assertNull(provider.createProof(payload));
        assertEquals(SubjectProofVerification.Status.DUPLICATE_IDENTITY,
                provider.verifyProof(proof, payload, now).getStatus());

        provider.observeMemberNames(Arrays.asList("member-one", "member-two"));
        assertTrue(provider.isReady());
        assertTrue(provider.verifyProof(proof, payload, now).isValid());
        }

    @Test
    public void shouldEncodeEveryMemberNameTotallyAndInjectively()
        {
        List<String> names = Arrays.asList("é", "%C3%A9", "member one", "member%20one", " member", "member ",
                "member/name?#[]@!$&'()*+,;=", "member\tname", "member\nname", "\u0000", "\uD800", "\uDC00",
                "\uD83D\uDE80", String.join("", Collections.nCopies(512, "x")));
        Set<String> encoded = new java.util.HashSet<>();
        for (String name : names)
            {
            String issuer = X509PeerProofProvider.issuerId(name);
            assertTrue(issuer, issuer.startsWith(X509PeerProofProvider.ISSUER_PREFIX));
            assertTrue("canonical collision for " + name, encoded.add(issuer));
            assertEquals(issuer, X509PeerProofProvider.issuerId(name));
            }
        assertEquals("", X509PeerProofProvider.issuerId(null));
        assertFalse(X509PeerProofProvider.issuerId(" a").equals(X509PeerProofProvider.issuerId("a")));
        assertFalse(X509PeerProofProvider.issuerId("é").equals(X509PeerProofProvider.issuerId("%C3%A9")));
        }

    @Test
    public void shouldRejectCaAndInvalidKeyUsageForIdentityAndBothRoles()
        {
        assertProviderFailure(s_ca, s_memberOne.f_cert, s_memberOne.f_cert, "non-CA");
        assertProviderFailure(s_noUsage, s_memberOne.f_cert, s_memberOne.f_cert, "digitalSignature");
        assertProviderFailure(s_wrongUsage, s_memberOne.f_cert, s_memberOne.f_cert, "digitalSignature");

        for (Credential invalid : Arrays.asList(s_ca, s_noUsage, s_wrongUsage))
            {
            String expected = invalid == s_ca ? "non-CA" : "digitalSignature";
            assertProviderFailure(s_memberOne, invalid.f_cert, s_memberOne.f_cert, expected);
            assertProviderFailure(s_memberOne, s_memberOne.f_cert, invalid.f_cert, expected);
            }
        }

    @Test
    public void shouldEnforceValidityAlgorithmKeyAndIssuerBounds()
        {
        X509PeerProofProvider provider = provider();
        long now = System.currentTimeMillis();

        SubjectProofPayload maximum = subject(provider, now,
                now + X509PeerProofProvider.MAX_PROOF_VALIDITY_MILLIS);
        assertTrue(provider.verifyProof(provider.createProof(maximum), maximum, now).isValid());

        SubjectProofPayload overlong = subject(provider, now,
                now + X509PeerProofProvider.MAX_PROOF_VALIDITY_MILLIS + 1L);
        assertEquals(SubjectProofVerification.Status.EXPIRED,
                provider.verifyProof(provider.createProof(overlong), overlong, now).getStatus());

        SubjectProofPayload futureBoundary = subject(provider,
                now + X509PeerProofProvider.CLOCK_SKEW_MILLIS, now + 180_000L);
        assertTrue(provider.verifyProof(provider.createProof(futureBoundary), futureBoundary, now).isValid());

        SubjectProofPayload future = subject(provider,
                now + X509PeerProofProvider.CLOCK_SKEW_MILLIS + 1L, now + 180_001L);
        assertEquals(SubjectProofVerification.Status.NOT_YET_VALID,
                provider.verifyProof(provider.createProof(future), future, now).getStatus());

        SubjectProofPayload expiryBoundary = subject(provider,
                now - X509PeerProofProvider.CLOCK_SKEW_MILLIS,
                now - X509PeerProofProvider.CLOCK_SKEW_MILLIS);
        byte[] expiryProof = provider.createProof(expiryBoundary);
        assertTrue(provider.verifyProof(expiryProof, expiryBoundary, now).isValid());
        assertEquals(SubjectProofVerification.Status.EXPIRED,
                provider.verifyProof(expiryProof, expiryBoundary, now + 1L).getStatus());

        SubjectProofPayload wrongAlgorithm = payload("ecdsa-p256-sha256", provider.getKeyId(),
                provider.getIssuerId(), now, now + 60_000L);
        assertEquals(SubjectProofVerification.Status.WRONG_ALGORITHM,
                provider.verifyProof(encoded(wrongAlgorithm), wrongAlgorithm, now).getStatus());

        SubjectProofPayload unknownKey = payload(provider.getAlgorithmId(), "x509-sha256:unknown",
                provider.getIssuerId(), now, now + 60_000L);
        assertEquals(SubjectProofVerification.Status.UNKNOWN_KEY,
                provider.verifyProof(encoded(unknownKey), unknownKey, now).getStatus());

        SubjectProofPayload wrongIssuer = payload(provider.getAlgorithmId(), provider.getKeyId(),
                "urn:coherence:member:not-member-one", now, now + 60_000L);
        assertEquals(SubjectProofVerification.Status.UNAUTHORIZED_ISSUER,
                provider.verifyProof(encoded(wrongIssuer), wrongIssuer, now).getStatus());
        }

    @Test
    public void shouldApplyOverflowSafeTimePolicyToBothRolesAndFullSubjectVerifier()
        {
        X509PeerProofProvider provider = provider();
        long now = System.currentTimeMillis();

        SubjectProofPayload skewExpiry = subject(provider, now, now + 60_000L);
        byte[] proof = provider.createProof(skewExpiry);
        long boundary = skewExpiry.getExpiresAtMillis() + X509PeerProofProvider.CLOCK_SKEW_MILLIS;
        assertTrue(SubjectProofVerifier.verify(provider, proof, skewExpiry, boundary).isValid());
        assertEquals(SubjectProofVerification.Status.EXPIRED,
                SubjectProofVerifier.verify(provider, proof, skewExpiry, boundary + 1L).getStatus());

        assertSubjectTime(provider, Long.MIN_VALUE, Long.MAX_VALUE, now,
                SubjectProofVerification.Status.EXPIRED);
        assertSubjectTime(provider, Long.MAX_VALUE - 1L, Long.MAX_VALUE, now,
                SubjectProofVerification.Status.NOT_YET_VALID);
        assertSubjectTime(provider, Long.MIN_VALUE, Long.MIN_VALUE + 1L, Long.MIN_VALUE,
                SubjectProofVerification.Status.UNAUTHORIZED_ISSUER);

        assertSeniorTime(provider, Long.MIN_VALUE, Long.MAX_VALUE, now, SeniorMetadataProofVerification.Status.EXPIRED);
        assertSeniorTime(provider, Long.MAX_VALUE - 1L, Long.MAX_VALUE, now,
                SeniorMetadataProofVerification.Status.NOT_YET_VALID);
        assertSeniorTime(provider, Long.MIN_VALUE, Long.MIN_VALUE + 1L, Long.MIN_VALUE,
                SeniorMetadataProofVerification.Status.UNAUTHORIZED_ISSUER);
        }

    @Test
    public void shouldStageOptionalUnavailableMaterialRecoverAndCloseIdempotently() throws Exception
        {
        Path authorities = s_dir.resolve("staged-authorities-" + System.nanoTime() + ".pem");
        Files.writeString(authorities, "backend detail that must not be exposed");
        X509PeerProofProvider provider = new X509PeerProofProvider(
                dependencies(s_memberOne, authorities, authorities));
        s_providers.add(provider);
        provider.setLocalMemberName(s_memberOne.f_name);

        assertFalse(provider.isReady());
        assertTrue(provider.getReadinessStatus().startsWith("reload-unavailable:"));
        assertFalse(provider.getReadinessStatus().contains(authorities.toString()));
        assertTrue(provider.getSubjectReadinessStatus().startsWith("staged-unavailable:"));

        Files.writeString(authorities, Files.readString(s_memberOne.f_cert));
        assertTrue(provider.refresh());
        assertTrue(provider.isReady());

        provider.close();
        provider.close();
        assertFalse(provider.refresh());
        assertEquals("", provider.getKeyId());
        assertEquals("closed", provider.getReadinessStatus());
        }

    @Test
    public void shouldFailStartupWhenEitherRequiredPolicyHasUnavailableMaterial() throws Exception
        {
        Path unavailable = s_dir.resolve("required-unavailable-" + System.nanoTime() + ".pem");
        Files.writeString(unavailable, "not a certificate");
        for (boolean subject : Arrays.asList(false, true))
            {
            PeerProofDependencies deps = dependencies(s_memberOne, unavailable, unavailable)
                    .setSubjectProofRequired(subject).setSeniorMetadataProofRequired(!subject);
            assertProviderFailure(deps, "no usable credential snapshot");
            }
        }

    @Test
    public void shouldStageMissingAndExpiredMaterialOnlyWhenPolicyIsOptional()
        {
        PeerProofDependencies missing = dependencies(s_memberOne, s_memberOne.f_cert, s_memberOne.f_cert)
                .setIdentityStoreUrl(s_dir.resolve("missing-identity.p12").toString());
        X509PeerProofProvider stagedMissing = new X509PeerProofProvider(missing);
        s_providers.add(stagedMissing);
        assertTrue(stagedMissing.getReadinessStatus().startsWith("reload-unavailable:"));

        X509PeerProofProvider stagedExpired = new X509PeerProofProvider(
                dependencies(s_expired, s_memberOne.f_cert, s_memberOne.f_cert));
        s_providers.add(stagedExpired);
        assertTrue(stagedExpired.getReadinessStatus().startsWith("reload-unavailable:"));

        assertProviderFailure(dependencies(s_expired, s_memberOne.f_cert, s_memberOne.f_cert)
                .setSeniorMetadataProofRequired(true), "no usable credential snapshot");
        }

    @Test
    public void shouldCloseReplacedAndDetachedSecurityProvidersIdempotently() throws Exception
        {
        X509PeerProofProvider first = provider();
        X509PeerProofProvider second = provider();
        java.lang.reflect.Method setter = Security.class.getDeclaredMethod("setPeerProofProvider", PeerProofProvider.class);
        setter.setAccessible(true);
        setter.invoke(null, first);
        setter.invoke(null, second);
        assertEquals("closed", first.getReadinessStatus());

        Security.closePeerProofProvider();
        Security.closePeerProofProvider();
        assertEquals("closed", second.getReadinessStatus());
        java.lang.reflect.Field field = Security.class.getDeclaredField("__s_PeerProofProvider");
        field.setAccessible(true);
        assertNull(field.get(null));
        }

    @Test
    public void shouldKeepAuthorityRolesSeparate()
        {
        X509PeerProofProvider verifier = provider(s_memberOne, s_memberOne.f_cert, s_memberTwo.f_cert);
        X509PeerProofProvider memberTwo = provider(s_memberTwo, s_memberTwo.f_cert, s_memberTwo.f_cert);
        long now = System.currentTimeMillis();

        SubjectProofPayload subject = subject(memberTwo, now, now + 60_000L);
        assertEquals(SubjectProofVerification.Status.UNKNOWN_KEY,
                verifier.verifyProof(memberTwo.createProof(subject), subject, now).getStatus());

        SeniorMetadataProofPayload senior = senior(memberTwo, now, now + 60_000L);
        assertTrue(verifier.verifyProof(memberTwo.createProof(senior), senior, now).isValid());
        }

    @Test
    public void shouldRefreshAtomicallyAcrossRotationAndRetirement() throws Exception
        {
        Path authorities = s_dir.resolve("rotation-authorities-" + System.nanoTime() + ".pem");
        Files.writeString(authorities, Files.readString(s_memberOne.f_cert)
                + System.lineSeparator() + Files.readString(s_memberTwo.f_cert));

        X509PeerProofProvider memberOne = provider(s_memberOne, authorities, authorities);
        X509PeerProofProvider memberTwo = provider(s_memberTwo, authorities, authorities);
        long now = System.currentTimeMillis();
        SubjectProofPayload oldPayload = subject(memberOne, now, now + 60_000L);
        SubjectProofPayload newPayload = subject(memberTwo, now, now + 60_000L);
        byte[] oldProof = memberOne.createProof(oldPayload);
        byte[] newProof = memberTwo.createProof(newPayload);

        assertTrue(memberTwo.verifyProof(oldProof, oldPayload, now).isValid());
        assertTrue(memberTwo.verifyProof(newProof, newPayload, now).isValid());

        Files.writeString(authorities, Files.readString(s_memberTwo.f_cert));
        assertTrue(memberTwo.refresh());
        assertEquals(SubjectProofVerification.Status.UNKNOWN_KEY,
                memberTwo.verifyProof(oldProof, oldPayload, now).getStatus());
        assertTrue(memberTwo.verifyProof(newProof, newPayload, now).isValid());

        Files.writeString(authorities, "not a certificate");
        assertFalse(memberTwo.refresh());
        assertTrue(memberTwo.getReadinessStatus().startsWith("reload-degraded-last-known-good:"));
        assertTrue(memberTwo.verifyProof(newProof, newPayload, now).isValid());
        }

    @Test
    public void shouldDiscardOlderRefreshAfterNewerRetirementCommits() throws Exception
        {
        Path authorities = s_dir.resolve("concurrent-retirement-" + System.nanoTime() + ".pem");
        Files.writeString(authorities, Files.readString(s_memberOne.f_cert)
                + System.lineSeparator() + Files.readString(s_memberTwo.f_cert));
        final CountDownLatch[] loaded = {new CountDownLatch(1)};
        final CountDownLatch[] resume = {new CountDownLatch(1)};
        X509PeerProofProvider provider = new X509PeerProofProvider(
                dependencies(s_memberTwo, authorities, authorities), generation ->
                    {
                    if (generation == 2L)
                        {
                        loaded[0].countDown();
                        assertTrue(resume[0].await(10, TimeUnit.SECONDS));
                        }
                    });
        s_providers.add(provider);
        provider.setLocalMemberName(s_memberTwo.f_name);
        provider.observeMemberNames(Collections.singleton(s_memberTwo.f_name));

        X509PeerProofProvider oldSigner = provider(s_memberOne, authorities, authorities);
        long now = System.currentTimeMillis();
        SubjectProofPayload oldPayload = subject(oldSigner, now, now + 60_000L);
        byte[] oldProof = oldSigner.createProof(oldPayload);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try
            {
            Future<Boolean> stale = executor.submit(provider::refresh);
            assertTrue(loaded[0].await(10, TimeUnit.SECONDS));
            Files.writeString(authorities, Files.readString(s_memberTwo.f_cert));
            assertTrue(provider.refresh());
            resume[0].countDown();
            assertFalse(stale.get(10, TimeUnit.SECONDS));
            assertEquals(SubjectProofVerification.Status.UNKNOWN_KEY,
                    provider.verifyProof(oldProof, oldPayload, now).getStatus());
            }
        finally
            {
            resume[0].countDown();
            executor.shutdownNow();
            }
        }

    @Test
    public void shouldCloseAcrossPausedRefreshWithoutReinstallOrBackendReuse() throws Exception
        {
        final CountDownLatch[] loaded = {new CountDownLatch(1)};
        final CountDownLatch[] resume = {new CountDownLatch(1)};
        AtomicInteger loads = new AtomicInteger();
        X509PeerProofProvider provider = new X509PeerProofProvider(
                dependencies(s_memberOne, s_memberOne.f_cert, s_memberOne.f_cert), generation ->
                    {
                    loads.incrementAndGet();
                    if (generation == 2L)
                        {
                        loaded[0].countDown();
                        assertTrue(resume[0].await(10, TimeUnit.SECONDS));
                        }
                    });
        s_providers.add(provider);
        provider.setLocalMemberName(s_memberOne.f_name);
        provider.observeMemberNames(Collections.singleton(s_memberOne.f_name));
        long now = System.currentTimeMillis();
        SubjectProofPayload subject = subject(provider, now, now + 60_000L);
        SeniorMetadataProofPayload senior = senior(provider, now, now + 60_000L);
        byte[] subjectProof = provider.createProof(subject);
        byte[] seniorProof = provider.createProof(senior);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try
            {
            Future<Boolean> refresh = executor.submit(provider::refresh);
            assertTrue(loaded[0].await(10, TimeUnit.SECONDS));
            Future<?> close = executor.submit(provider::close);
            for (int i = 0; i < 100 && !"closed".equals(provider.getReadinessStatus()); i++)
                {
                Thread.sleep(10L);
                }
            assertEquals("closed", provider.getReadinessStatus());
            assertNull(provider.createProof(subject));
            assertNull(provider.createProof(senior));
            resume[0].countDown();
            close.get(10, TimeUnit.SECONDS);
            assertFalse(refresh.get(10, TimeUnit.SECONDS));
            assertEquals(SubjectProofVerification.Status.PROVIDER_UNAVAILABLE,
                    provider.verifyProof(subjectProof, subject, now).getStatus());
            assertEquals(SeniorMetadataProofVerification.Status.PROVIDER_UNAVAILABLE,
                    provider.verifyProof(seniorProof, senior, now).getStatus());
            assertFalse(provider.isSeniorMetadataProofAuthority(senior));
            int count = loads.get();
            assertFalse(provider.refresh());
            assertEquals(count, loads.get());
            assertEquals("", provider.getKeyId());
            }
        finally
            {
            resume[0].countDown();
            executor.shutdownNow();
            }
        }

    @Test
    public void shouldPublishCompleteLifecycleGenerationsAcrossSanRefreshRebindAndClose() throws Exception
        {
        Path identity = s_dir.resolve("publication-identity-" + System.nanoTime() + ".p12");
        Files.copy(s_memberOne.f_store, identity);
        Credential mutable = new Credential("member-one", identity, s_memberOne.f_cert);
        final CountDownLatch[] loaded = {new CountDownLatch(1)};
        final CountDownLatch[] resume = {new CountDownLatch(1)};
        X509PeerProofProvider provider = new X509PeerProofProvider(
                dependencies(mutable, s_memberOne.f_cert, s_memberOne.f_cert), generation ->
                    {
                    if (generation == 2L || generation == 3L)
                        {
                        loaded[0].countDown();
                        assertTrue(resume[0].await(10, TimeUnit.SECONDS));
                        }
                    });
        s_providers.add(provider);
        provider.setLocalMemberName("member-one");
        provider.observeMemberNames(Collections.singleton("member-one"));
        String oldKey = provider.getKeyId();

        Files.copy(s_memberOneNew.f_store, identity, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try
            {
            Future<Boolean> valid = executor.submit(provider::refresh);
            assertTrue(loaded[0].await(10, TimeUnit.SECONDS));
            PeerProofReadiness.Observation old = provider.getLocalReadinessObservation();
            assertTrue(old.isReady());
            assertEquals(oldKey, old.getActiveKeyId());
            long oldGeneration = old.getLifecycleGeneration();
            resume[0].countDown();
            assertTrue(valid.get(10, TimeUnit.SECONDS));
            PeerProofReadiness.Observation current = provider.getLocalReadinessObservation();
            assertTrue(current.isReady());
            assertEquals(provider.getKeyId(), current.getActiveKeyId());
            assertFalse(oldKey.equals(current.getActiveKeyId()));
            assertTrue(current.getLifecycleGeneration() > oldGeneration);

            loaded[0] = new CountDownLatch(1);
            resume[0] = new CountDownLatch(1);
            // A wrong-SAN candidate cannot replace the complete usable generation.
            Files.copy(s_memberTwo.f_store, identity, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Future<Boolean> wrongSan = executor.submit(provider::refresh);
            assertTrue(loaded[0].await(10, TimeUnit.SECONDS));
            String retainedKey = provider.getKeyId();
            assertTrue(provider.getLocalReadinessObservation().isReady());
            resume[0].countDown();
            assertFalse(wrongSan.get(10, TimeUnit.SECONDS));
            assertEquals(retainedKey, provider.getKeyId());
            assertTrue(provider.getLocalReadinessObservation().isReady());

            provider.setLocalMemberName("member-two");
            assertFalse(provider.getLocalReadinessObservation().isReady());
            provider.setLocalMemberName("member-one");
            assertTrue(provider.getLocalReadinessObservation().isReady());
            provider.close();
            assertFalse(provider.getLocalReadinessObservation().isReady());
            assertEquals("", provider.getLocalReadinessObservation().getActiveKeyId());
            }
        finally
            {
            resume[0].countDown();
            executor.shutdownNow();
            }
        }

    @Test
    public void shouldRejectPostReadCandidateThatCrossesAbsoluteDeadline() throws Exception
        {
        Path identity = s_dir.resolve("deadline-identity-" + System.nanoTime() + ".p12");
        Files.copy(s_memberOne.f_store, identity);
        Credential mutable = new Credential("member-one", identity, s_memberOne.f_cert);
        AtomicBoolean armed = new AtomicBoolean();
        AtomicBoolean failAfterDelay = new AtomicBoolean();
        CountDownLatch entered = new CountDownLatch(1);
        X509PeerProofProvider provider = new X509PeerProofProvider(
                dependencies(mutable, s_memberOne.f_cert, s_memberOne.f_cert), generation ->
                    {
                    if (armed.get() && generation > 1L)
                        {
                        entered.countDown();
                        long until = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(300L);
                        while (System.nanoTime() < until)
                            {
                            try
                                {
                                Thread.sleep(10L);
                                }
                            catch (InterruptedException ignored)
                                {
                                // The regression deliberately models supported post-read
                                // work that does not convert interruption into failure.
                                }
                            }
                        if (failAfterDelay.get())
                            {
                            throw new IllegalStateException("late post-read validation failure");
                            }
                        }
                    }, 100L);
        s_providers.add(provider);
        provider.setLocalMemberName("member-one");
        provider.observeMemberNames(Collections.singleton("member-one"));
        PeerProofReadiness.Observation before = provider.getLocalReadinessObservation();
        String oldKey = provider.getKeyId();
        long now = System.currentTimeMillis();
        SubjectProofPayload payload = subject(provider, now, now + 60_000L);
        byte[] proof = provider.createProof(payload);

        Files.copy(s_memberOneNew.f_store, identity, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        armed.set(true);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try
            {
            Future<Boolean> refresh = executor.submit(provider::refresh);
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertFalse(refresh.get(5, TimeUnit.SECONDS));
            PeerProofReadiness.Observation after = provider.getLocalReadinessObservation();
            assertEquals(oldKey, provider.getKeyId());
            assertEquals(before.getLifecycleGeneration(), after.getLifecycleGeneration());
            assertEquals(before.getActiveKeyId(), after.getActiveKeyId());
            assertEquals(SubjectProofVerification.Status.VALID,
                    provider.verifyProof(proof, payload, now).getStatus());
            Thread.sleep(150L);
            assertEquals(oldKey, provider.getKeyId());
            assertEquals(before.getLifecycleGeneration(),
                    provider.getLocalReadinessObservation().getLifecycleGeneration());

            failAfterDelay.set(true);
            assertFalse(executor.submit(provider::refresh).get(5, TimeUnit.SECONDS));
            assertEquals(oldKey, provider.getKeyId());
            assertEquals(before.getLifecycleGeneration(),
                    provider.getLocalReadinessObservation().getLifecycleGeneration());
            }
        finally
            {
            executor.shutdownNow();
            }
        }

    @Test
    public void shouldLinearizeManualAndScheduledDeadlineRacesAndClosePostReadWork() throws Exception
        {
        AtomicInteger delay = new AtomicInteger();
        CountDownLatch closeEntered = new CountDownLatch(1);
        AtomicBoolean closeMode = new AtomicBoolean();
        X509PeerProofProvider provider = new X509PeerProofProvider(
                dependencies(s_memberOne, s_memberOne.f_cert, s_memberOne.f_cert), generation ->
                    {
                    if (generation <= 1L)
                        {
                        return;
                        }
                    if (closeMode.get())
                        {
                        closeEntered.countDown();
                        long until = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(200L);
                        while (System.nanoTime() < until)
                            {
                            try {Thread.sleep(5L);} catch (InterruptedException ignored) {}
                            }
                        return;
                        }
                    int millis = delay.get();
                    long until = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
                    while (System.nanoTime() < until)
                        {
                        try {Thread.sleep(2L);} catch (InterruptedException ignored) {}
                        }
                    }, 125L);
        s_providers.add(provider);
        provider.setLocalMemberName("member-one");
        provider.observeMemberNames(Collections.singleton("member-one"));

        ExecutorService manual = Executors.newSingleThreadExecutor();
        try
            {
            for (int i = 0; i < 12; i++)
                {
                boolean shouldExpire = (i & 1) == 1;
                delay.set(shouldExpire ? 180 : 0);
                long generation = provider.getLocalReadinessObservation().getLifecycleGeneration();
                Future<Boolean> refresh = (i & 2) == 0
                        ? manual.submit(provider::refresh)
                        : providerRefreshExecutor(provider).submit(provider::refresh);
                boolean committed = refresh.get(5, TimeUnit.SECONDS);
                assertEquals("deadline race " + i, !shouldExpire, committed);
                long current = provider.getLocalReadinessObservation().getLifecycleGeneration();
                assertEquals("deadline generation race " + i,
                        shouldExpire ? generation : generation + 1L, current);
                }
            }
        finally
            {
            manual.shutdownNow();
            }

        closeMode.set(true);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try
            {
            Future<Boolean> refresh = executor.submit(provider::refresh);
            assertTrue(closeEntered.await(5, TimeUnit.SECONDS));
            long start = System.nanoTime();
            Future<?> close = executor.submit(provider::close);
            close.get(X509PeerProofProvider.CLOSE_TIMEOUT_MILLIS + 1_000L, TimeUnit.MILLISECONDS);
            assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
                    <= X509PeerProofProvider.CLOSE_TIMEOUT_MILLIS + 500L);
            assertFalse(refresh.get(5, TimeUnit.SECONDS));
            assertClosedAndFailClosed(provider);
            }
        finally
            {
            executor.shutdownNow();
            }
        }

    @Test
    public void shouldShutdownBothOwnedExecutorsWhenNonCooperativePostReadWorkExceedsCloseBound() throws Exception
        {
        AtomicBoolean armed = new AtomicBoolean();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        X509PeerProofProvider provider = new X509PeerProofProvider(
                dependencies(s_memberOne, s_memberOne.f_cert, s_memberOne.f_cert), generation ->
                    {
                    if (!armed.get())
                        {
                        return;
                        }
                    entered.countDown();
                    while (release.getCount() != 0L)
                        {
                        try {release.await(25L, TimeUnit.MILLISECONDS);} catch (InterruptedException ignored) {}
                        }
                    });
        s_providers.add(provider);
        provider.setLocalMemberName("member-one");
        provider.observeMemberNames(Collections.singleton("member-one"));
        long generation = provider.getLocalReadinessObservation().getLifecycleGeneration();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try
            {
            armed.set(true);
            Future<Boolean> refresh = executor.submit(provider::refresh);
            assertTrue(entered.await(5L, TimeUnit.SECONDS));
            long start = System.nanoTime();
            try
                {
                provider.close();
                fail("non-cooperative post-read work must exhaust the bounded close");
                }
            catch (IllegalStateException expected)
                {
                assertNotNull(expected.getMessage());
                }
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            assertTrue("close exceeded its documented bound: " + elapsed,
                    elapsed <= X509PeerProofProvider.CLOSE_TIMEOUT_MILLIS + 1_000L);
            assertTrue(providerRefreshExecutor(provider).isShutdown());
            assertTrue(providerIoExecutor(provider).isShutdown());
            assertTrue(providerCleanupExecutor(provider).isShutdown());
            assertClosedAndFailClosed(provider);
            assertEquals(generation + 1L,
                    provider.getLocalReadinessObservation().getLifecycleGeneration());

            release.countDown();
            assertFalse(refresh.get(5L, TimeUnit.SECONDS));
            provider.close();
            provider.close();
            assertTrue(providerRefreshExecutor(provider).isTerminated());
            assertTrue(providerIoExecutor(provider).isTerminated());
            assertTrue(providerCleanupExecutor(provider).isTerminated());
            }
        finally
            {
            release.countDown();
            executor.shutdownNow();
            }
        }

    @Test
    public void shouldCancelBlockedOpenAndCloseAcrossCredentialPositionsAndRefreshOwners() throws Exception
        {
        for (LifecycleMode mode : Arrays.asList(LifecycleMode.OPEN, LifecycleMode.CLOSE))
            {
            for (int iRead = 1; iRead <= 3; iRead++)
                {
                LifecycleCredentialReader reader = new LifecycleCredentialReader(mode, iRead);
                X509PeerProofProvider provider = new X509PeerProofProvider(
                        dependencies(s_memberOne, s_memberOne.f_cert, s_memberOne.f_cert), generation -> {}, reader);
                s_providers.add(provider);
                provider.setLocalMemberName("member-one");
                provider.observeMemberNames(Collections.singleton("member-one"));
                reader.arm();

                ExecutorService executor = iRead % 2 == 0
                        ? providerRefreshExecutor(provider) : Executors.newSingleThreadExecutor();
                try
                    {
                    Future<Boolean> refresh = executor.submit(provider::refresh);
                    assertTrue(mode + " credential " + iRead + " did not stall", reader.awaitStalled());
                    long start = System.nanoTime();
                    provider.close();
                    long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                    assertTrue("close exceeded bound for " + mode + " credential " + iRead + ": " + elapsedMillis,
                            elapsedMillis <= X509PeerProofProvider.CLOSE_TIMEOUT_MILLIS + 1_000L);
                    assertFalse(refresh.get(10, TimeUnit.SECONDS));
                    assertTrue(reader.wasCancelled());
                    assertBackendQuiescent(reader);
                    assertClosedAndFailClosed(provider);
                    }
                finally
                    {
                    provider.close();
                    if (iRead % 2 != 0)
                        {
                        executor.shutdownNow();
                        }
                    }
                }
            }
        }

    @Test
    public void shouldApplyOneAbsoluteDeadlineToIndefiniteTrickleAtEveryCredentialPosition() throws Exception
        {
        for (int iRead = 1; iRead <= 3; iRead++)
            {
            LifecycleCredentialReader reader = new LifecycleCredentialReader(LifecycleMode.TRICKLE, iRead);
            X509PeerProofProvider provider = new X509PeerProofProvider(
                    dependencies(s_memberOne, s_memberOne.f_cert, s_memberOne.f_cert), generation -> {}, reader);
            s_providers.add(provider);
            provider.setLocalMemberName("member-one");
            provider.observeMemberNames(Collections.singleton("member-one"));
            reader.arm();

            ExecutorService executor = iRead == 2
                    ? providerRefreshExecutor(provider) : Executors.newSingleThreadExecutor();
            try
                {
                long start = System.nanoTime();
                Future<Boolean> refresh = executor.submit(provider::refresh);
                assertTrue("trickle credential " + iRead + " did not stall", reader.awaitStalled());
                assertFalse(refresh.get(X509PeerProofProvider.CREDENTIAL_OPERATION_TIMEOUT_MILLIS + 2_000L,
                        TimeUnit.MILLISECONDS));
                long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                assertTrue("trickle escaped the absolute deadline: " + elapsedMillis,
                        elapsedMillis <= X509PeerProofProvider.CREDENTIAL_OPERATION_TIMEOUT_MILLIS + 1_500L);
                assertTrue(reader.wasCancelled());
                assertBackendQuiescent(reader);
                assertTrue(provider.isReady());
                }
            finally
                {
                provider.close();
                if (iRead != 2)
                    {
                    executor.shutdownNow();
                    }
                }
            }
        }

    @Test
    public void shouldCancelMaximumCredentialWorkAtEverySupportedPosition() throws Exception
        {
        List<Credential> authorities = new ArrayList<>(Arrays.asList(s_memberOne, s_memberTwo,
                s_memberOneNew, s_memberOneNewTwo, s_memberThree, s_memberFour, s_memberFourNew));
        authorities.addAll(s_complexityCredentials);
        Path pem = s_dir.resolve("maximum-lifecycle-authorities.pem");
        java.io.ByteArrayOutputStream pemBytes = new java.io.ByteArrayOutputStream();
        for (Credential credential : authorities)
            {
            pemBytes.write(Files.readAllBytes(credential.f_cert));
            }
        Files.write(pem, pemBytes.toByteArray());
        Path identityJks = boundedStore("maximum-lifecycle-identity.jks", "JKS", authorities, true);
        Path identityPkcs12 = boundedStore("maximum-lifecycle-identity.p12", "PKCS12", authorities, true);
        Path authorityJks = boundedStore("maximum-lifecycle-authorities.jks", "JKS", authorities, false);
        Path authorityPkcs12 = boundedStore("maximum-lifecycle-authorities.p12", "PKCS12", authorities, false);

        List<PeerProofDependencies> configurations = Arrays.asList(
                storeDependencies(identityPkcs12, "PKCS12", authorityJks, "JKS", authorityPkcs12, "PKCS12"),
                storeDependencies(identityJks, "JKS", authorityPkcs12, "PKCS12", authorityJks, "JKS"),
                dependencies(s_memberOne, pem, pem).setIdentityStoreUrl(identityPkcs12.toString()));
        int nConfiguration = 0;
        for (PeerProofDependencies deps : configurations)
            {
            nConfiguration++;
            for (int iRead = 1; iRead <= 3; iRead++)
                {
                LifecycleCredentialReader reader = new LifecycleCredentialReader(LifecycleMode.OPEN, iRead);
                X509PeerProofProvider provider = new X509PeerProofProvider(deps, generation -> {}, reader);
                s_providers.add(provider);
                provider.setLocalMemberName("member-one");
                provider.observeMemberNames(Collections.singleton("member-one"));
                reader.arm();
                ExecutorService executor = Executors.newSingleThreadExecutor();
                try
                    {
                    Future<Boolean> refresh = executor.submit(provider::refresh);
                    assertTrue("maximum configuration " + nConfiguration + " credential " + iRead
                            + " did not enter", reader.awaitStalled());
                    provider.close();
                    assertFalse(refresh.get(5L, TimeUnit.SECONDS));
                    assertTrue(reader.wasCancelled());
                    assertBackendQuiescent(reader);
                    assertTrue(providerRefreshExecutor(provider).isTerminated());
                    assertTrue(providerIoExecutor(provider).isTerminated());
                    assertTrue(providerCleanupExecutor(provider).isTerminated());
                    provider.close();
                    }
                finally
                    {
                    executor.shutdownNow();
                    }
                }
            }
        }

    @Test
    public void shouldAggregateExplicitKeySpecificRotationAcrossRefreshRestartAndSecondRotation() throws Exception
        {
        Path identity = s_dir.resolve("rotation-identity-" + System.nanoTime() + ".p12");
        Path authorities = s_dir.resolve("rotation-stages-" + System.nanoTime() + ".pem");
        Files.copy(s_memberOne.f_store, identity);
        Files.writeString(authorities, Files.readString(s_memberOne.f_cert));
        Credential mutable = new Credential("member-one", identity, s_memberOne.f_cert);
        X509PeerProofProvider provider = provider(mutable, authorities, authorities);
        X509PeerProofProvider newSigner = provider(s_memberOneNew, s_memberOneNew.f_cert, s_memberOneNew.f_cert);
        String issuer = X509PeerProofProvider.issuerId("member-one");
        String oldKey = provider.getKeyId();
        String newKey = newSigner.getKeyId();
        Set<String> members = Collections.singleton(issuer);

        assertEquals("ready", provider.getSubjectReadinessStatus());
        Files.writeString(authorities, Files.readString(s_memberOne.f_cert)
                + System.lineSeparator() + Files.readString(s_memberOneNew.f_cert));
        assertTrue(provider.refresh());
        assertEquals("subject:add-new:ready", readiness(PeerProofReadiness.Stage.ADD, oldKey, newKey,
                members, members, provider));
        assertTrue(provider.refresh());
        assertEquals("subject:add-new:ready", readiness(PeerProofReadiness.Stage.ADD, oldKey, newKey,
                members, members, provider));
        provider.close();
        provider = provider(mutable, authorities, authorities);
        assertEquals("subject:add-new:ready", readiness(PeerProofReadiness.Stage.ADD, oldKey, newKey,
                members, members, provider));

        Files.copy(s_memberOneNew.f_store, identity, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        assertTrue(provider.refresh());
        assertEquals("subject:activate-new:ready", readiness(PeerProofReadiness.Stage.ACTIVATE, oldKey, newKey,
                members, members, provider));
        assertEquals("subject:overlap:ready", readiness(PeerProofReadiness.Stage.OVERLAP, oldKey, newKey,
                members, members, provider));

        provider.close();
        provider = provider(mutable, authorities, authorities);
        assertEquals("subject:activate-new:ready", readiness(PeerProofReadiness.Stage.ACTIVATE, oldKey, newKey,
                members, members, provider));
        assertEquals("subject:overlap:ready", readiness(PeerProofReadiness.Stage.OVERLAP, oldKey, newKey,
                members, members, provider));

        Files.writeString(authorities, Files.readString(s_memberOneNew.f_cert));
        assertTrue(provider.refresh());
        assertEquals("subject:retire-old:ready", readiness(PeerProofReadiness.Stage.RETIRE, oldKey, newKey,
                members, members, provider));
        provider.close();
        provider = provider(mutable, authorities, authorities);
        assertEquals("subject:retire-old:ready", readiness(PeerProofReadiness.Stage.RETIRE, oldKey, newKey,
                members, members, provider));

        X509PeerProofProvider secondSigner = provider(
                s_memberOneNewTwo, s_memberOneNewTwo.f_cert, s_memberOneNewTwo.f_cert);
        String secondKey = secondSigner.getKeyId();
        Files.writeString(authorities, Files.readString(s_memberOneNew.f_cert)
                + System.lineSeparator() + Files.readString(s_memberOneNewTwo.f_cert));
        assertTrue(provider.refresh());
        assertEquals("subject:add-new:ready", readiness(PeerProofReadiness.Stage.ADD, newKey, secondKey,
                members, members, provider));
        Files.copy(s_memberOneNewTwo.f_store, identity, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        assertTrue(provider.refresh());
        assertEquals("subject:overlap:ready", readiness(PeerProofReadiness.Stage.OVERLAP, newKey, secondKey,
                members, members, provider));
        Files.writeString(authorities, Files.readString(s_memberOneNewTwo.f_cert));
        assertTrue(provider.refresh());
        assertEquals("subject:retire-old:ready", readiness(PeerProofReadiness.Stage.RETIRE, newKey, secondKey,
                members, members, provider));
        }

    @Test
    public void shouldRejectMalformedTopologyQueriesAndUnboundObservations()
        {
        assertTrue(SerializationAllowlist.isAllowlisted(PeerProofReadiness.ReadinessInvocable.class));
        assertTrue(SerializationAllowlist.isAllowlisted(PeerProofReadiness.Response.class));

        X509PeerProofProvider subject = provider(s_memberOne, s_memberOne.f_cert, s_memberTwo.f_cert);
        X509PeerProofProvider senior = provider(s_memberTwo, s_memberOne.f_cert, s_memberTwo.f_cert);
        long now = System.currentTimeMillis();
        PeerProofReadiness.MemberBinding one = new PeerProofReadiness.MemberBinding(
                "uid-one", "member-one", "subject-producer");
        PeerProofReadiness.MemberBinding two = new PeerProofReadiness.MemberBinding(
                "uid-two", "member-two", "senior-recipient");
        PeerProofReadiness.Topology topology = PeerProofReadiness.Topology.of(Arrays.asList(one, two), two);
        String oldKey = subject.getKeyId();
        String newKey = senior.getKeyId();
        PeerProofReadiness.Query subjectQuery = new PeerProofReadiness.Query(PeerProofReadiness.Role.SUBJECT,
                PeerProofReadiness.Stage.ADD, oldKey, newKey);
        PeerProofReadiness.Query seniorQuery = new PeerProofReadiness.Query(PeerProofReadiness.Role.SENIOR,
                PeerProofReadiness.Stage.ADD, oldKey, newKey);
        java.util.Map<PeerProofReadiness.MemberBinding, PeerProofReadiness.Response> responses =
                new java.util.LinkedHashMap<>();
        responses.put(one, new PeerProofReadiness.Response(one, topology.getEpoch(),
                subject.getLocalReadinessObservation()));
        responses.put(two, new PeerProofReadiness.Response(two, topology.getEpoch(),
                senior.getLocalReadinessObservation()));

        assertFalse(PeerProofReadiness.evaluate(new PeerProofReadiness.Query(null,
                PeerProofReadiness.Stage.ADD, oldKey, newKey, Collections.singleton("subject-producer"),
                Arrays.asList("subject-producer", "senior-recipient")), topology, responses, now).isReady());
        assertFalse(PeerProofReadiness.evaluate(new PeerProofReadiness.Query(PeerProofReadiness.Role.SUBJECT,
                null, oldKey, newKey, Collections.singleton("subject-producer"),
                Arrays.asList("subject-producer", "senior-recipient")), topology, responses, now).isReady());
        assertFalse(PeerProofReadiness.evaluate(new PeerProofReadiness.Query(PeerProofReadiness.Role.SUBJECT,
                PeerProofReadiness.Stage.ADD, "", newKey, Collections.singleton("subject-producer"),
                Arrays.asList("subject-producer", "senior-recipient")), topology, responses, now).isReady());
        assertFalse(PeerProofReadiness.evaluate(new PeerProofReadiness.Query(PeerProofReadiness.Role.SUBJECT,
                PeerProofReadiness.Stage.ADD, oldKey, oldKey, Collections.singleton("subject-producer"),
                Arrays.asList("subject-producer", "senior-recipient")), topology, responses, now).isReady());
        assertFalse(PeerProofReadiness.evaluate(new PeerProofReadiness.Query(PeerProofReadiness.Role.SUBJECT,
                PeerProofReadiness.Stage.ADD, oldKey, newKey, Collections.emptySet(),
                Arrays.asList("subject-producer", "senior-recipient")), topology, responses, now).isReady());
        assertFalse(PeerProofReadiness.evaluate(new PeerProofReadiness.Query(PeerProofReadiness.Role.SUBJECT,
                PeerProofReadiness.Stage.ADD, oldKey, newKey, Collections.singleton("subject-producer"),
                Collections.singleton("subject-producer")), topology, responses, now).isReady());
        assertFalse(PeerProofReadiness.evaluate(new PeerProofReadiness.Query(PeerProofReadiness.Role.SENIOR,
                PeerProofReadiness.Stage.ADD, oldKey, newKey, Collections.singleton("senior-recipient"),
                Arrays.asList("subject-producer", "senior-recipient")), topology, responses, now).isReady());

        java.util.Map<PeerProofReadiness.MemberBinding, PeerProofReadiness.Response> omitted =
                new java.util.LinkedHashMap<>(responses);
        omitted.remove(two);
        assertFalse(PeerProofReadiness.evaluate(subjectQuery, topology, omitted, now).isReady());

        PeerProofReadiness.MemberBinding extra = new PeerProofReadiness.MemberBinding(
                "uid-extra", "member-extra", "senior-recipient");
        java.util.Map<PeerProofReadiness.MemberBinding, PeerProofReadiness.Response> substituted =
                new java.util.LinkedHashMap<>(omitted);
        substituted.put(extra, new PeerProofReadiness.Response(extra, topology.getEpoch(),
                senior.getLocalReadinessObservation()));
        assertFalse(PeerProofReadiness.evaluate(subjectQuery, topology, substituted, now).isReady());

        java.util.Map<PeerProofReadiness.MemberBinding, PeerProofReadiness.Response> wrongSource =
                new java.util.LinkedHashMap<>(responses);
        wrongSource.put(one, new PeerProofReadiness.Response(two, topology.getEpoch(),
                subject.getLocalReadinessObservation()));
        assertFalse(PeerProofReadiness.evaluate(subjectQuery, topology, wrongSource, now).isReady());

        java.util.Map<PeerProofReadiness.MemberBinding, PeerProofReadiness.Response> wrongEpoch =
                new java.util.LinkedHashMap<>(responses);
        wrongEpoch.put(one, new PeerProofReadiness.Response(one, "stale-epoch",
                subject.getLocalReadinessObservation()));
        assertFalse(PeerProofReadiness.evaluate(subjectQuery, topology, wrongEpoch, now).isReady());

        PeerProofReadiness.MemberBinding joined = new PeerProofReadiness.MemberBinding(
                "uid-joined", "member-joined", "joined-recipient");
        PeerProofReadiness.Topology joinedTopology = PeerProofReadiness.Topology.of(
                Arrays.asList(one, two, joined), two);
        assertFalse(PeerProofReadiness.evaluate(subjectQuery, joinedTopology, responses, now).isReady());
        PeerProofReadiness.Topology leftTopology = PeerProofReadiness.Topology.of(
                Collections.singleton(one), one);
        assertFalse(PeerProofReadiness.evaluate(subjectQuery, leftTopology, responses, now).isReady());
        PeerProofReadiness.MemberBinding changedRole = new PeerProofReadiness.MemberBinding(
                "uid-two", "member-two", "changed-recipient");
        PeerProofReadiness.Topology changedRoleTopology = PeerProofReadiness.Topology.of(
                Arrays.asList(one, changedRole), changedRole);
        assertFalse(PeerProofReadiness.evaluate(subjectQuery, changedRoleTopology, responses, now).isReady());

        PeerProofReadiness.MemberBinding duplicateName = new PeerProofReadiness.MemberBinding(
                "uid-duplicate", "member-one", "senior-recipient");
        PeerProofReadiness.Topology duplicateTopology = PeerProofReadiness.Topology.of(
                Arrays.asList(one, duplicateName), duplicateName);
        java.util.Map<PeerProofReadiness.MemberBinding, PeerProofReadiness.Response> duplicateResponses =
                new java.util.LinkedHashMap<>();
        duplicateResponses.put(one, new PeerProofReadiness.Response(one, duplicateTopology.getEpoch(),
                subject.getLocalReadinessObservation()));
        duplicateResponses.put(duplicateName, new PeerProofReadiness.Response(duplicateName,
                duplicateTopology.getEpoch(), subject.getLocalReadinessObservation()));
        assertFalse(PeerProofReadiness.evaluate(subjectQuery, duplicateTopology, duplicateResponses, now).isReady());

        PeerProofReadiness.Observation expired = new PeerProofReadiness.Observation(
                X509PeerProofProvider.issuerId("member-one"), X509PeerProofProvider.PROVIDER_ID,
                subject.getKeyId(), true, now, 1L,
                new PeerProofReadiness.CertificateWindow(0L, 1L), Collections.emptyMap(), Collections.emptyMap());
        java.util.Map<PeerProofReadiness.MemberBinding, PeerProofReadiness.Response> stale =
                new java.util.LinkedHashMap<>(responses);
        stale.put(one, new PeerProofReadiness.Response(one, topology.getEpoch(), expired));
        assertFalse(PeerProofReadiness.evaluate(subjectQuery, topology, stale, now).isReady());
        assertFalse(PeerProofReadiness.evaluate(seniorQuery, topology, responses, now).isReady());
        }

    @Test
    public void shouldDeriveDistinctSubjectAndPerServiceSeniorPopulationsAndBindTheirEpochs() throws Exception
        {
        Path subjectAuthorities = s_dir.resolve("subject-topology-" + System.nanoTime() + ".pem");
        Path seniorAuthorities = s_dir.resolve("senior-topology-" + System.nanoTime() + ".pem");
        Files.writeString(subjectAuthorities, Files.readString(s_memberOne.f_cert) + System.lineSeparator()
                + Files.readString(s_memberOneNew.f_cert));
        Files.writeString(seniorAuthorities, Files.readString(s_memberFour.f_cert) + System.lineSeparator()
                + Files.readString(s_memberFourNew.f_cert));

        List<X509PeerProofProvider> providers = Arrays.asList(
                provider(s_memberOne, subjectAuthorities, seniorAuthorities),
                provider(s_memberTwo, subjectAuthorities, seniorAuthorities),
                provider(s_memberThree, subjectAuthorities, seniorAuthorities),
                provider(s_memberFour, subjectAuthorities, seniorAuthorities));
        List<PeerProofReadiness.MemberBinding> members = Arrays.asList(
                new PeerProofReadiness.MemberBinding("one", "member-one", "subject-only"),
                new PeerProofReadiness.MemberBinding("two", "member-two", "subject-senior"),
                new PeerProofReadiness.MemberBinding("three", "member-three", "subject-senior"),
                new PeerProofReadiness.MemberBinding("four", "member-four", "senior-only"));
        List<PeerProofReadiness.ServiceBinding> services = Arrays.asList(
                service("Management", "Invocation", "four", "one", "two", "three", "four"),
                serviceWithRecipients("SubjectOne", "DistributedCache", "two",
                        Arrays.asList("one", "two"), Collections.singleton("two")),
                serviceWithRecipients("SubjectTwo", "FederatedCache", "three",
                        Arrays.asList("one", "three"), Collections.singleton("three")),
                service("SeniorOnly", "Invocation", "four", "three", "four"));
        PeerProofReadiness.Topology topology = PeerProofReadiness.Topology.of(members, services);
        java.util.Map<PeerProofReadiness.MemberBinding, PeerProofReadiness.Response> responses =
                readinessResponses(topology, members, providers);

        assertEquals(new java.util.LinkedHashSet<>(members.subList(0, 3)),
                topology.producers(PeerProofReadiness.Role.SUBJECT));
        assertEquals(new java.util.LinkedHashSet<>(members.subList(1, 3)),
                topology.recipients(PeerProofReadiness.Role.SUBJECT));
        assertEquals(new java.util.LinkedHashSet<>(Arrays.asList(members.get(1), members.get(2), members.get(3))),
                topology.producers(PeerProofReadiness.Role.SENIOR));
        assertEquals(new java.util.LinkedHashSet<>(members), topology.recipients(PeerProofReadiness.Role.SENIOR));

        PeerProofReadiness.Result subject = PeerProofReadiness.evaluate(new PeerProofReadiness.Query(
                PeerProofReadiness.Role.SUBJECT, PeerProofReadiness.Stage.ADD,
                providers.get(0).getKeyId(), keyId(s_memberOneNew)), topology, responses,
                System.currentTimeMillis());
        assertTrue(subject.getStatus(), subject.isReady());
        assertEquals(3, subject.getProducerCount());
        assertEquals(2, subject.getRecipientCount());

        PeerProofReadiness.Result senior = PeerProofReadiness.evaluate(new PeerProofReadiness.Query(
                PeerProofReadiness.Role.SENIOR, PeerProofReadiness.Stage.ADD,
                providers.get(3).getKeyId(), keyId(s_memberFourNew)), topology, responses,
                System.currentTimeMillis());
        assertTrue(senior.getStatus(), senior.isReady());
        assertEquals(3, senior.getProducerCount());
        assertEquals(4, senior.getRecipientCount());

        java.util.Map<PeerProofReadiness.MemberBinding, PeerProofReadiness.Response> omitted =
                new java.util.LinkedHashMap<>(responses);
        omitted.remove(members.get(0));
        assertFalse(PeerProofReadiness.evaluate(new PeerProofReadiness.Query(PeerProofReadiness.Role.SUBJECT,
                PeerProofReadiness.Stage.ADD, providers.get(0).getKeyId(), keyId(s_memberOneNew)),
                topology, omitted, System.currentTimeMillis()).isReady());

        List<PeerProofReadiness.ServiceBinding> changedServices = Arrays.asList(
                service("Management", "Invocation", "four", "one", "two", "three", "four"),
                serviceWithRecipients("SubjectOne", "DistributedCache", "one",
                        Arrays.asList("one", "two"), Collections.singleton("two")),
                serviceWithRecipients("SubjectTwo", "FederatedCache", "three",
                        Arrays.asList("one", "three", "four"), Arrays.asList("three", "four")),
                service("SeniorOnly", "Invocation", "four", "three", "four"));
        PeerProofReadiness.Topology changed = PeerProofReadiness.Topology.of(members, changedServices);
        assertFalse(PeerProofReadiness.evaluate(new PeerProofReadiness.Query(PeerProofReadiness.Role.SUBJECT,
                PeerProofReadiness.Stage.ADD, providers.get(0).getKeyId(), keyId(s_memberOneNew)),
                changed, responses, System.currentTimeMillis()).isReady());

        List<PeerProofReadiness.ServiceBinding> changedRecipients = Arrays.asList(
                service("Management", "Invocation", "four", "one", "two", "three", "four"),
                serviceWithRecipients("SubjectOne", "DistributedCache", "two",
                        Arrays.asList("one", "two"), Arrays.asList("one", "two")),
                serviceWithRecipients("SubjectTwo", "FederatedCache", "three",
                        Arrays.asList("one", "three"), Collections.singleton("three")),
                service("SeniorOnly", "Invocation", "four", "three", "four"));
        PeerProofReadiness.Topology recipientChanged = PeerProofReadiness.Topology.of(members, changedRecipients);
        assertFalse(PeerProofReadiness.evaluate(new PeerProofReadiness.Query(PeerProofReadiness.Role.SUBJECT,
                PeerProofReadiness.Stage.ADD, providers.get(0).getKeyId(), keyId(s_memberOneNew)),
                recipientChanged, responses, System.currentTimeMillis()).isReady());
        assertFalse(PeerProofReadiness.evaluate(new PeerProofReadiness.Query(PeerProofReadiness.Role.SENIOR,
                PeerProofReadiness.Stage.ADD, providers.get(3).getKeyId(), keyId(s_memberFourNew)),
                changed, responses, System.currentTimeMillis()).isReady());

        List<PeerProofReadiness.MemberBinding> changedRole = new ArrayList<>(members);
        changedRole.set(0, new PeerProofReadiness.MemberBinding("one", "member-one", "changed-role"));
        PeerProofReadiness.Topology roleChanged = PeerProofReadiness.Topology.of(changedRole, services);
        assertFalse(PeerProofReadiness.evaluate(new PeerProofReadiness.Query(PeerProofReadiness.Role.SUBJECT,
                PeerProofReadiness.Stage.ADD, providers.get(0).getKeyId(), keyId(s_memberOneNew)),
                roleChanged, responses, System.currentTimeMillis()).isReady());

        assertFalse(PeerProofReadiness.evaluate(new PeerProofReadiness.Query(PeerProofReadiness.Role.SUBJECT,
                PeerProofReadiness.Stage.ADD, providers.get(0).getKeyId(), keyId(s_memberOneNew),
                Collections.singleton("subject-only"), Collections.emptySet()), topology, responses,
                System.currentTimeMillis()).isReady());
        }

    @Test
    public void shouldNotAccumulateRefreshThreadsAcrossRepeatedClose() throws Exception
        {
        long before = refreshThreadCount();
        for (int i = 0; i < 20; i++)
            {
            X509PeerProofProvider provider = provider();
            provider.close();
            provider.close();
            }
        for (int i = 0; i < 20 && refreshThreadCount() > before; i++)
            {
            Thread.sleep(25L);
            }
        assertTrue("refresh threads accumulated after close: before=" + before + ", after=" + refreshThreadCount(),
                refreshThreadCount() <= before);
        }

    @Test
    public void shouldRetainMonotonicEvidenceAcrossAnIdenticalTopologyAbaTransition()
        {
        List<PeerProofReadiness.MemberBinding> members = Arrays.asList(
                new PeerProofReadiness.MemberBinding("one", "member-one", "storage"),
                new PeerProofReadiness.MemberBinding("two", "member-two", "storage"));
        PeerProofReadiness.Topology before = PeerProofReadiness.Topology.of(members, members.get(0));
        String epoch = before.getEpoch();

        PeerProofReadiness.recordTopologyChange();
        PeerProofReadiness.recordTopologyChange();
        PeerProofReadiness.Topology after = PeerProofReadiness.Topology.of(members, members.get(0));

        assertEquals("current-state hash should demonstrate the A-B-A blind spot", epoch, after.getEpoch());
        assertFalse("monotonic transition generation must retain A-B-A evidence", before.sameEpoch(after));
        }

    @Test
    public void shouldRejectUnsupportedOrSecretlessConfiguration()
        {
        PeerProofDependencies unknown = dependencies(s_memberOne, s_memberOne.f_cert, s_memberOne.f_cert)
                .setProviderId("arbitrary.Provider");
        assertValidationFailure(unknown, "unknown peer-proof provider");

        PeerProofDependencies noPassword = dependencies(s_memberOne, s_memberOne.f_cert, s_memberOne.f_cert)
                .setIdentityPasswordProvider(null);
        assertValidationFailure(noPassword, "named PasswordProvider");

        PeerProofDependencies protectedAuthority = new PeerProofDependencies()
                .setIdentityStoreUrl(s_memberOne.f_store.toString()).setIdentityStoreType("PKCS12")
                .setIdentityAlias(ALIAS).setIdentityPasswordProvider(() -> PASSWORD.toCharArray())
                .setSubjectAuthorityStore(new PeerProofDependencies.AuthorityStore(
                        s_memberOne.f_store.toString(), "PKCS12", null))
                .addSeniorCertificateUrl(s_memberOne.f_cert.toString());
        assertValidationFailure(protectedAuthority, "named PasswordProvider");
        }

    @Test
    public void shouldClearPasswordsReturnedAfterDeadlineAtEveryPosition() throws Exception
        {
        Path subjectStore = boundedStore("late-subject.jks", "JKS",
                Collections.singletonList(s_memberOne), false);
        Path seniorStore = boundedStore("late-senior.p12", "PKCS12",
                Collections.singletonList(s_memberOne), false);

        for (PasswordPosition position : PasswordPosition.values())
            {
            PeerProofDependencies deps = protectedDependencies(subjectStore, seniorStore,
                    () -> PASSWORD.toCharArray(), () -> PASSWORD.toCharArray(), () -> PASSWORD.toCharArray());
            X509PeerProofProvider provider = new X509PeerProofProvider(deps, generation -> {}, 100L);
            s_providers.add(provider);
            provider.setLocalMemberName("member-one");
            provider.observeMemberNames(Collections.singleton("member-one"));
            long generation = provider.getLocalReadinessObservation().getLifecycleGeneration();
            String key = provider.getKeyId();
            LatePasswordProvider late = LatePasswordProvider.afterDeadline("late-secret-" + position, 250L);
            installPassword(deps, position, subjectStore, seniorStore, late);

            assertFalse("late password accepted at " + position, provider.refresh());
            assertTrue("late password did not return at " + position, late.awaitReturned());
            assertEventuallyCleared(late.password());
            assertEquals(generation, provider.getLocalReadinessObservation().getLifecycleGeneration());
            assertEquals(key, provider.getKeyId());
            assertTrue(provider.isReady());
            assertFalse(provider.getReadinessStatus().contains("late-secret"));
            provider.close();
            }
        }

    @Test
    public void shouldClearPasswordsReturnedAfterCloseAtEveryPosition() throws Exception
        {
        Path subjectStore = boundedStore("close-subject.jks", "JKS",
                Collections.singletonList(s_memberOne), false);
        Path seniorStore = boundedStore("close-senior.p12", "PKCS12",
                Collections.singletonList(s_memberOne), false);

        for (PasswordPosition position : PasswordPosition.values())
            {
            PeerProofDependencies deps = protectedDependencies(subjectStore, seniorStore,
                    () -> PASSWORD.toCharArray(), () -> PASSWORD.toCharArray(), () -> PASSWORD.toCharArray());
            X509PeerProofProvider provider = new X509PeerProofProvider(deps);
            s_providers.add(provider);
            provider.setLocalMemberName("member-one");
            provider.observeMemberNames(Collections.singleton("member-one"));
            long generation = provider.getLocalReadinessObservation().getLifecycleGeneration();
            LatePasswordProvider late = LatePasswordProvider.afterRelease("close-secret-" + position);
            installPassword(deps, position, subjectStore, seniorStore, late);

            ExecutorService refreshExecutor = Executors.newSingleThreadExecutor();
            ExecutorService closeExecutor = Executors.newSingleThreadExecutor();
            try
                {
                Future<Boolean> refresh = refreshExecutor.submit(provider::refresh);
                assertTrue("password provider was not entered at " + position, late.awaitEntered());
                Future<?> close = closeExecutor.submit(provider::close);
                for (int i = 0; i < 100 && !providerIoExecutor(provider).isShutdown(); i++)
                    {
                    Thread.sleep(10L);
                    }
                assertTrue(providerRefreshExecutor(provider).isShutdown());
                assertTrue(providerIoExecutor(provider).isShutdown());
                assertTrue(providerCleanupExecutor(provider).isShutdown());
                late.release();
                close.get(5L, TimeUnit.SECONDS);
                assertFalse(refresh.get(5L, TimeUnit.SECONDS));
                assertTrue(late.awaitReturned());
                assertEventuallyCleared(late.password());
                assertEquals(generation + 1L,
                        provider.getLocalReadinessObservation().getLifecycleGeneration());
                assertClosedAndFailClosed(provider);
                assertTrue(providerRefreshExecutor(provider).isTerminated());
                assertTrue(providerIoExecutor(provider).isTerminated());
                assertTrue(providerCleanupExecutor(provider).isTerminated());
                provider.close();
                }
            finally
                {
                late.release();
                refreshExecutor.shutdownNow();
                closeExecutor.shutdownNow();
                }
            }
        }

    @Test
    public void shouldEnforceApprovedCredentialComplexityForJksPkcs12AndPem() throws Exception
        {
        List<Credential> authorities = new ArrayList<>(Arrays.asList(s_memberOne, s_memberTwo,
                s_memberOneNew, s_memberOneNewTwo, s_memberThree, s_memberFour, s_memberFourNew));
        authorities.addAll(s_complexityCredentials);
        assertEquals(X509PeerProofProvider.MAX_AUTHORITY_CERTIFICATES, authorities.size());

        Path pem = s_dir.resolve("maximum-authorities.pem");
        List<byte[]> certificates = new ArrayList<>();
        for (Credential credential : authorities)
            {
            certificates.add(Files.readAllBytes(credential.f_cert));
            }
        Files.write(pem, concatenate(certificates.toArray(new byte[0][])));

        Path identityJks = boundedStore("maximum-identity.jks", "JKS", authorities, true);
        PeerProofDependencies jksAndPem = dependencies(s_memberOne, pem, pem)
                .setIdentityStoreUrl(identityJks.toString()).setIdentityStoreType("JKS");
        X509PeerProofProvider providerJks = new X509PeerProofProvider(jksAndPem);
        s_providers.add(providerJks);
        providerJks.setLocalMemberName("member-one");
        providerJks.observeMemberNames(Collections.singleton("member-one"));
        assertTrue(providerJks.isReady());

        Path identityPkcs12 = boundedStore("maximum-identity.p12", "PKCS12", authorities, true);
        Path authorityJks = boundedStore("maximum-authorities.jks", "JKS", authorities, false);
        Path authorityPkcs12 = boundedStore("maximum-authorities.p12", "PKCS12", authorities, false);
        PeerProofDependencies protectedStores = new PeerProofDependencies()
                .setIdentityStoreUrl(identityPkcs12.toString()).setIdentityStoreType("PKCS12")
                .setIdentityAlias(ALIAS).setIdentityPasswordProvider(() -> PASSWORD.toCharArray())
                .setSubjectAuthorityStore(new PeerProofDependencies.AuthorityStore(
                        authorityJks.toString(), "JKS", () -> PASSWORD.toCharArray()))
                .setSeniorAuthorityStore(new PeerProofDependencies.AuthorityStore(
                        authorityPkcs12.toString(), "PKCS12", () -> PASSWORD.toCharArray()));
        X509PeerProofProvider providerStores = new X509PeerProofProvider(protectedStores);
        s_providers.add(providerStores);
        providerStores.setLocalMemberName("member-one");
        providerStores.observeMemberNames(Collections.singleton("member-one"));
        assertTrue(providerStores.isReady());

        PeerProofDependencies reverseStores = new PeerProofDependencies()
                .setIdentityStoreUrl(identityPkcs12.toString()).setIdentityStoreType("PKCS12")
                .setIdentityAlias(ALIAS).setIdentityPasswordProvider(() -> PASSWORD.toCharArray())
                .setSubjectAuthorityStore(new PeerProofDependencies.AuthorityStore(
                        authorityPkcs12.toString(), "PKCS12", () -> PASSWORD.toCharArray()))
                .setSeniorAuthorityStore(new PeerProofDependencies.AuthorityStore(
                        authorityJks.toString(), "JKS", () -> PASSWORD.toCharArray()));
        X509PeerProofProvider providerReverse = new X509PeerProofProvider(reverseStores);
        s_providers.add(providerReverse);
        providerReverse.setLocalMemberName("member-one");
        providerReverse.observeMemberNames(Collections.singleton("member-one"));
        assertTrue(providerReverse.getReadinessStatus(), providerReverse.isReady());

        char[] maximumPassword = new char[X509PeerProofProvider.MAX_PASSWORD_CHARS];
        Arrays.fill(maximumPassword, 'p');
        java.security.cert.Certificate[] chain = certificateChain(s_memberOne, s_memberTwo,
                s_memberThree, s_memberFour);
        Path chainStore = identityStore("maximum-chain-and-password.jks", "JKS", chain, maximumPassword);
        CapturingPasswordProvider maximumPasswords = new CapturingPasswordProvider(maximumPassword);
        X509PeerProofProvider maximumChain = new X509PeerProofProvider(
                dependencies(s_memberOne, pem, pem)
                        .setIdentityStoreUrl(chainStore.toString()).setIdentityStoreType("JKS")
                        .setIdentityPasswordProvider(maximumPasswords));
        s_providers.add(maximumChain);
        maximumChain.setLocalMemberName("member-one");
        maximumChain.observeMemberNames(Collections.singleton("member-one"));
        assertTrue(maximumChain.isReady());
        assertEquals(2, maximumPasswords.passwords().size());
        for (char[] returned : maximumPasswords.passwords())
            {
            assertCleared(returned);
            }

        List<Credential> tooManyAuthorities = new ArrayList<>(authorities);
        tooManyAuthorities.add(s_memberOne);
        for (String type : Arrays.asList("JKS", "PKCS12"))
            {
            Path overAliases = boundedStore("too-many-aliases." + type.toLowerCase(), type,
                    tooManyAuthorities, false);
            PeerProofDependencies overAliasDeps = new PeerProofDependencies()
                    .setIdentityStoreUrl(s_memberOne.f_store.toString()).setIdentityStoreType("PKCS12")
                    .setIdentityAlias(ALIAS).setIdentityPasswordProvider(() -> PASSWORD.toCharArray())
                    .setSubjectAuthorityStore(new PeerProofDependencies.AuthorityStore(
                            overAliases.toString(), type, () -> PASSWORD.toCharArray()))
                    .addSeniorCertificateUrl(s_memberOne.f_cert.toString());
            X509PeerProofProvider overAlias = new X509PeerProofProvider(overAliasDeps);
            s_providers.add(overAlias);
            assertFalse("over-limit aliases accepted for " + type, overAlias.isReady());
            }

        Path overChainStore = identityStore("too-long-chain.jks", "JKS",
                certificateChain(s_memberOne, s_memberTwo, s_memberThree, s_memberFour, s_memberFourNew),
                PASSWORD.toCharArray());
        X509PeerProofProvider overChain = new X509PeerProofProvider(
                dependencies(s_memberOne, s_memberOne.f_cert, s_memberOne.f_cert)
                        .setIdentityStoreUrl(overChainStore.toString()).setIdentityStoreType("JKS"));
        s_providers.add(overChain);
        assertFalse(overChain.isReady());

        Path tooManyPem = s_dir.resolve("too-many-authorities.pem");
        Files.write(tooManyPem, concatenate(Files.readAllBytes(pem), Files.readAllBytes(s_memberOne.f_cert)));
        X509PeerProofProvider tooMany = new X509PeerProofProvider(
                dependencies(s_memberOne, tooManyPem, s_memberOne.f_cert));
        s_providers.add(tooMany);
        assertFalse(tooMany.isReady());
        assertTrue(tooMany.getReadinessStatus().contains("authority set exceeds"));

        Path tooLarge = s_dir.resolve("too-large.p12");
        Files.write(tooLarge, new byte[X509PeerProofProvider.MAX_CREDENTIAL_BYTES + 1]);
        X509PeerProofProvider oversized = new X509PeerProofProvider(
                dependencies(s_memberOne, s_memberOne.f_cert, s_memberOne.f_cert)
                        .setIdentityStoreUrl(tooLarge.toString()));
        s_providers.add(oversized);
        assertFalse(oversized.isReady());
        assertTrue(oversized.getReadinessStatus().contains("supported size bound"));

        char[] longPassword = new char[X509PeerProofProvider.MAX_PASSWORD_CHARS + 1];
        Arrays.fill(longPassword, 'x');
        X509PeerProofProvider passwordBound = new X509PeerProofProvider(
                dependencies(s_memberOne, s_memberOne.f_cert, s_memberOne.f_cert)
                        .setIdentityPasswordProvider(() -> longPassword.clone()));
        s_providers.add(passwordBound);
        assertFalse(passwordBound.isReady());
        }

    @Test
    public void shouldSupportOnlyPlainAndFileUriRegularFileCredentialSources() throws Exception
        {
        PeerProofDependencies fileUris = new PeerProofDependencies()
                .setIdentityStoreUrl(s_memberOne.f_store.toUri().toString()).setIdentityStoreType("PKCS12")
                .setIdentityAlias(ALIAS).setIdentityPasswordProvider(() -> PASSWORD.toCharArray())
                .addSubjectCertificateUrl(s_memberOne.f_cert.toUri().toString())
                .addSeniorCertificateUrl(s_memberOne.f_cert.toUri().toString());
        X509PeerProofProvider uriProvider = new X509PeerProofProvider(fileUris);
        s_providers.add(uriProvider);
        uriProvider.setLocalMemberName("member-one");
        uriProvider.observeMemberNames(Collections.singleton("member-one"));
        assertTrue(uriProvider.isReady());

        for (String source : Arrays.asList("http://credential.invalid/identity.p12",
                "https://credential.invalid/identity.p12", "classpath:identity.p12",
                "jar:file:/credential.jar!/identity.p12", "relative-identity.p12", s_dir.toString()))
            {
            PeerProofDependencies optional = dependencies(s_memberOne, s_memberOne.f_cert, s_memberOne.f_cert)
                    .setIdentityStoreUrl(source);
            X509PeerProofProvider unavailable = new X509PeerProofProvider(optional);
            s_providers.add(unavailable);
            assertFalse(unavailable.isReady());
            assertTrue(unavailable.getReadinessStatus().startsWith("reload-unavailable:"));
            assertFalse(unavailable.getReadinessStatus().contains(source));
            String expected = "relative-identity.p12".equals(source) ? "absolute local path"
                    : s_dir.toString().equals(source) ? "local regular file" : "IOException";
            assertProviderFailure(optional.setSubjectProofRequired(true), expected);
            }

        Path link = s_dir.resolve("identity-link-" + System.nanoTime() + ".p12");
        try
            {
            Files.createSymbolicLink(link, s_memberOne.f_store);
            PeerProofDependencies linked = dependencies(s_memberOne, s_memberOne.f_cert, s_memberOne.f_cert)
                    .setIdentityStoreUrl(link.toString()).setSubjectProofRequired(true);
            assertProviderFailure(linked, "local regular file");
            }
        catch (UnsupportedOperationException | SecurityException ignored)
            {
            // The supported-source contract still rejects every non-file scheme above.
            }
        finally
            {
            Files.deleteIfExists(link);
            }
        }

    @Test
    public void shouldRejectEveryPreParserWorkFactorBeforeJca() throws Exception
        {
        byte[] nested = derNull();
        for (int i = 0; i <= X509PeerProofProvider.MAX_DER_DEPTH; i++)
            {
            nested = derSequence(nested);
            }
        assertUnavailableIdentity(writeCredential("over-depth.p12",
                derSequence(derInteger(3), nested)), "PKCS12");

        byte[][] tooManyChildren = new byte[X509PeerProofProvider.MAX_DER_CHILDREN + 1][];
        Arrays.fill(tooManyChildren, derNull());
        assertUnavailableIdentity(writeCredential("over-children.p12",
                derSequence(derInteger(3), derSequence(tooManyChildren))), "PKCS12");

        byte[][] groups = new byte[20][];
        for (int i = 0; i < groups.length; i++)
            {
            byte[][] nodes = new byte[220][];
            Arrays.fill(nodes, derNull());
            groups[i] = derSequence(nodes);
            }
        assertUnavailableIdentity(writeCredential("over-nodes.p12",
                derSequence(derInteger(3), derSequence(groups))), "PKCS12");

        byte[] pbkdf2 = new byte[] {0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86,
                (byte) 0xf7, 0x0d, 0x01, 0x05, 0x0c};
        byte[] parameters = derSequence(new byte[] {0x04, 0x01, 0x00},
                derInteger(X509PeerProofProvider.MAX_PBE_ITERATIONS + 1));
        assertUnavailableIdentity(writeCredential("over-pbe.p12",
                derSequence(derInteger(3), derSequence(pbkdf2, parameters))), "PKCS12");

        assertUnavailableIdentity(writeCredential("over-alias.jks",
                jksWithOversizedAlias()), "JKS");
        assertUnavailableIdentity(writeCredential("over-encrypted-key.jks",
                jksWithOversizedEncryptedKey()), "JKS");

        byte[] oversizedCertificate = new byte[X509PeerProofProvider.MAX_CERTIFICATE_BYTES + 1];
        String pem = "-----BEGIN CERTIFICATE-----\n"
                + java.util.Base64.getEncoder().encodeToString(oversizedCertificate)
                + "\n-----END CERTIFICATE-----\n";
        Path oversizedPem = s_dir.resolve("over-certificate.pem");
        Files.writeString(oversizedPem, pem);
        X509PeerProofProvider provider = new X509PeerProofProvider(
                dependencies(s_memberOne, oversizedPem, s_memberOne.f_cert));
        s_providers.add(provider);
        assertFalse(provider.isReady());
        assertTrue(provider.getReadinessStatus().startsWith("reload-unavailable:"));
        }

    @Test
    public void shouldInspectRealPkcs12AuthenticatedSafeBeforeJca() throws Exception
        {
        int cMaximum = X509PeerProofProvider.MAX_PBE_ITERATIONS;
        Path exact = pkcs12WithIterations("p75-exact", cMaximum, cMaximum, cMaximum);
        Path overMac = pkcs12WithIterations("p75-over-mac", cMaximum + 1, cMaximum, cMaximum);
        Path overKey = pkcs12WithIterations("p75-over-key", cMaximum, cMaximum + 1, cMaximum);
        Path overCertificate = pkcs12WithIterations(
                "p75-over-certificate", cMaximum, cMaximum, cMaximum + 1);

        CountingKeyStoreLoader exactLoader = new CountingKeyStoreLoader();
        PeerProofDependencies exactDependencies = storeDependencies(
                exact, "PKCS12", exact, "PKCS12", exact, "PKCS12");
        X509PeerProofProvider exactProvider = new X509PeerProofProvider(exactDependencies,
                generation -> {}, null, exactLoader,
                X509PeerProofProvider.CREDENTIAL_OPERATION_TIMEOUT_MILLIS);
        s_providers.add(exactProvider);
        exactProvider.setLocalMemberName("member-one");
        exactProvider.observeMemberNames(Collections.singleton("member-one"));
        assertTrue(exactProvider.getReadinessStatus(), exactProvider.isReady());
        assertEquals("exact-limit identity and both authority stores must reach JCA", 3,
                exactLoader.getLoadCount());

        for (Path overLimit : Arrays.asList(overMac, overKey, overCertificate))
            {
            for (PasswordPosition position : PasswordPosition.values())
                {
                CountingKeyStoreLoader loader = new CountingKeyStoreLoader();
                PeerProofDependencies deps = storeDependencies(
                        exact, "PKCS12", exact, "PKCS12", exact, "PKCS12");
                X509PeerProofProvider provider = new X509PeerProofProvider(deps,
                        generation -> {}, null, loader,
                        X509PeerProofProvider.CREDENTIAL_OPERATION_TIMEOUT_MILLIS);
                s_providers.add(provider);
                provider.setLocalMemberName("member-one");
                provider.observeMemberNames(Collections.singleton("member-one"));
                long generation = provider.getLocalReadinessObservation().getLifecycleGeneration();
                String key = provider.getKeyId();

                installPkcs12(deps, position, overLimit);
                loader.reset();
                assertFalse("over-limit real PFX accepted at " + position + " from " + overLimit,
                        provider.refresh());
                assertEquals("rejected PFX entered the platform key-store sink at " + position,
                        position.ordinal(), loader.getLoadCount());
                assertEquals("only the degraded diagnostic generation may advance", generation + 1L,
                        provider.getLocalReadinessObservation().getLifecycleGeneration());
                assertEquals(key, provider.getKeyId());
                assertTrue(provider.isReady());
                assertFalse(provider.getReadinessStatus().contains(overLimit.toString()));
                }
            }
        }

    @Test
    public void shouldValidateEveryPkcs12AlgorithmParameterBeforeJca() throws Exception
        {
        List<PfxParameterCase> cases = pfxParameterCases();
        byte[] validStore = Files.readAllBytes(s_memberOne.f_store);
        for (PasswordPosition position : PasswordPosition.values())
            {
            SubstitutingKeyStoreLoader loader = new SubstitutingKeyStoreLoader(validStore);
            PeerProofDependencies deps = storeDependencies(s_memberOne.f_store, "PKCS12",
                    s_memberOne.f_store, "PKCS12", s_memberOne.f_store, "PKCS12");
            X509PeerProofProvider provider = new X509PeerProofProvider(deps,
                    generation -> {}, null, loader,
                    X509PeerProofProvider.CREDENTIAL_OPERATION_TIMEOUT_MILLIS);
            s_providers.add(provider);
            provider.setLocalMemberName("member-one");
            provider.observeMemberNames(Collections.singleton("member-one"));
            assertTrue(provider.getReadinessStatus(), provider.isReady());

            for (PfxParameterCase parameterCase : cases)
                {
                Path path = writeCredential(position + "-" + parameterCase.f_sName + ".p12",
                        parameterCase.f_abPfx);
                CapturingPasswordProvider passwords = new CapturingPasswordProvider(PASSWORD.toCharArray());
                installPkcs12(deps, position, path, passwords);
                loader.reset();
                long generation = provider.getLocalReadinessObservation().getLifecycleGeneration();
                String key = provider.getKeyId();
                boolean refreshed = provider.refresh();
                assertFalse(parameterCase.f_sName + " reached JCA at " + position, refreshed);
                assertEquals("rejected parameter form entered its target sink at " + position,
                        position.ordinal(), loader.getLoadCount());
                assertEquals("only degraded status may advance for " + parameterCase.f_sName,
                        generation + 1L,
                        provider.getLocalReadinessObservation().getLifecycleGeneration());
                assertEquals(key, provider.getKeyId());
                assertTrue(provider.isReady());
                assertFalse(provider.getReadinessStatus().contains(path.toString()));
                for (char[] password : passwords.passwords())
                    {
                    assertCleared(password);
                    }
                }
            provider.close();
            assertTrue(providerRefreshExecutor(provider).isTerminated());
            assertTrue(providerIoExecutor(provider).isTerminated());
            assertTrue(providerCleanupExecutor(provider).isTerminated());
            provider.close();
            }
        }

    @Test
    public void shouldLoadEveryApprovedPkcs12FamilyWithSun() throws Exception
        {
        List<PfxPlatformFamily> families = pkcs12PlatformFamilies();
        for (PfxPlatformFamily family : families)
            {
            assertPkcs12PlatformFamily(family);
            CountingKeyStoreLoader loader = new CountingKeyStoreLoader();
            PeerProofDependencies deps = storeDependencies(
                    family.f_path, "PKCS12", family.f_path, "PKCS12", family.f_path, "PKCS12");
            X509PeerProofProvider provider = new X509PeerProofProvider(deps,
                    generation -> {}, null, loader,
                    X509PeerProofProvider.CREDENTIAL_OPERATION_TIMEOUT_MILLIS);
            s_providers.add(provider);
            provider.setLocalMemberName("member-one");
            provider.observeMemberNames(Collections.singleton("member-one"));
            assertEquals("accepted family did not enter all three SUN sinks: " + family.f_path,
                    3, loader.getLoadCount());
            assertTrue(provider.getReadinessStatus(), provider.isReady());
            provider.close();
            assertTrue(providerRefreshExecutor(provider).isTerminated());
            assertTrue(providerIoExecutor(provider).isTerminated());
            assertTrue(providerCleanupExecutor(provider).isTerminated());
            provider.close();
            }

        Path authorityStore = pkcs12TrustedAttributeStore("p77-supported-trusted-attributes.p12");
        for (Path authorities : Collections.singletonList(authorityStore))
            {
            CountingKeyStoreLoader loader = new CountingKeyStoreLoader();
            PeerProofDependencies deps = storeDependencies(s_memberOne.f_store, "PKCS12",
                    authorities, "PKCS12", authorities, "PKCS12");
            X509PeerProofProvider provider = new X509PeerProofProvider(deps,
                    generation -> {}, null, loader,
                    X509PeerProofProvider.CREDENTIAL_OPERATION_TIMEOUT_MILLIS);
            s_providers.add(provider);
            provider.setLocalMemberName("member-one");
            provider.observeMemberNames(Collections.singleton("member-one"));
            assertEquals("supported attribute form did not enter all three SUN sinks: " + authorities,
                    3, loader.getLoadCount());
            assertTrue(provider.getReadinessStatus(), provider.isReady());
            provider.close();
            assertTrue(providerRefreshExecutor(provider).isTerminated());
            assertTrue(providerIoExecutor(provider).isTerminated());
            assertTrue(providerCleanupExecutor(provider).isTerminated());
            provider.close();
            }
        }

    @Test
    public void shouldAdmitAbsentBagAttributesThroughStructuralGate() throws Exception
        {
        Path canonical = pkcs12PlatformFamily("p77-absent-attribute-canonical",
                "PBEWithHmacSHA256AndAES_256", "HmacPBESHA256");
        Path absent = omitVisibleBagAttributes("p77-absent-bag-attributes.p12", canonical);
        byte[] validStore = Files.readAllBytes(canonical);
        for (PasswordPosition position : PasswordPosition.values())
            {
            SubstitutingKeyStoreLoader loader = new SubstitutingKeyStoreLoader(validStore);
            PeerProofDependencies deps = storeDependencies(s_memberOne.f_store, "PKCS12",
                    s_memberOne.f_store, "PKCS12", s_memberOne.f_store, "PKCS12");
            X509PeerProofProvider provider = new X509PeerProofProvider(deps,
                    generation -> {}, null, loader,
                    X509PeerProofProvider.CREDENTIAL_OPERATION_TIMEOUT_MILLIS);
            s_providers.add(provider);
            provider.setLocalMemberName("member-one");
            provider.observeMemberNames(Collections.singleton("member-one"));
            CapturingPasswordProvider passwords = new CapturingPasswordProvider(PASSWORD.toCharArray());
            installPkcs12(deps, position, absent, passwords);
            loader.reset();
            assertTrue("absent optional attributes rejected at " + position, provider.refresh());
            assertEquals("absent optional attributes did not cross the structural gate at " + position,
                    3, loader.getLoadCount());
            assertFalse(passwords.passwords().isEmpty());
            for (char[] password : passwords.passwords())
                {
                assertCleared(password);
                }
            provider.close();
            assertTrue(providerRefreshExecutor(provider).isTerminated());
            assertTrue(providerIoExecutor(provider).isTerminated());
            assertTrue(providerCleanupExecutor(provider).isTerminated());
            provider.close();
            }
        }

    @Test
    public void shouldRejectEveryUnsupportedPkcs12StructureBeforeJca() throws Exception
        {
        Path canonical = pkcs12PlatformFamily("p77-structural-canonical",
                "PBEWithHmacSHA256AndAES_256", "HmacPBESHA256");
        List<PfxStructureCase> cases = pfxStructureCases(canonical);
        byte[] validStore = Files.readAllBytes(canonical);
        for (PasswordPosition position : PasswordPosition.values())
            {
            SubstitutingKeyStoreLoader loader = new SubstitutingKeyStoreLoader(validStore);
            PeerProofDependencies deps = storeDependencies(s_memberOne.f_store, "PKCS12",
                    s_memberOne.f_store, "PKCS12", s_memberOne.f_store, "PKCS12");
            X509PeerProofProvider provider = new X509PeerProofProvider(deps,
                    generation -> {}, null, loader,
                    X509PeerProofProvider.CREDENTIAL_OPERATION_TIMEOUT_MILLIS);
            s_providers.add(provider);
            provider.setLocalMemberName("member-one");
            provider.observeMemberNames(Collections.singleton("member-one"));
            assertTrue(provider.getReadinessStatus(), provider.isReady());
            long proofTime = System.currentTimeMillis();
            SubjectProofPayload retainedSubject = subject(provider, proofTime, proofTime + 60_000L);
            SeniorMetadataProofPayload retainedSenior = senior(provider, proofTime, proofTime + 60_000L);
            byte[] retainedSubjectProof = provider.createProof(retainedSubject);
            byte[] retainedSeniorProof = provider.createProof(retainedSenior);

            for (PfxStructureCase structureCase : cases)
                {
                Path path = writeCredential(position + "-" + structureCase.f_sName + ".p12",
                        structureCase.f_abPfx);
                CapturingPasswordProvider passwords = new CapturingPasswordProvider(PASSWORD.toCharArray());
                installPkcs12(deps, position, path, passwords);
                loader.reset();
                long generation = provider.getLocalReadinessObservation().getLifecycleGeneration();
                String key = provider.getKeyId();
                long started = System.nanoTime();
                assertFalse(structureCase.f_sName + " reached JCA at " + position, provider.refresh());
                long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                assertTrue("structural rejection exceeded the credential deadline: " + elapsed,
                        elapsed < X509PeerProofProvider.CREDENTIAL_OPERATION_TIMEOUT_MILLIS);
                assertEquals("rejected structure entered its target sink at " + position,
                        position.ordinal(), loader.getLoadCount());
                assertEquals(generation + 1L,
                        provider.getLocalReadinessObservation().getLifecycleGeneration());
                assertEquals(key, provider.getKeyId());
                assertTrue(provider.isReady());
                long verificationTime = System.currentTimeMillis();
                assertTrue(provider.verifyProof(retainedSubjectProof, retainedSubject,
                        verificationTime).isValid());
                assertTrue(provider.verifyProof(retainedSeniorProof, retainedSenior,
                        verificationTime).isValid());
                String status = provider.getReadinessStatus();
                assertTrue("unbounded structural diagnostic", status.length() <= 128);
                assertFalse(status.contains(path.toString()));
                assertTrue("rejected target acquired its password for " + structureCase.f_sName,
                        passwords.passwords().isEmpty());
                for (char[] password : passwords.passwords())
                    {
                    assertCleared(password);
                    }
                assertNoQueuedCredentialWork(provider);
                }
            provider.close();
            assertTrue(providerRefreshExecutor(provider).isTerminated());
            assertTrue(providerIoExecutor(provider).isTerminated());
            assertTrue(providerCleanupExecutor(provider).isTerminated());
            provider.close();
            }
        }

    @Test
    public void shouldNotStarveConcurrentCredentialCleanup() throws Exception
        {
        ConcurrentEofCredentialReader reader = new ConcurrentEofCredentialReader(
                s_memberOne.f_store.toString(), 4, 2, 4);
        X509PeerProofProvider provider = new X509PeerProofProvider(
                dependencies(s_memberOne, s_memberOne.f_cert, s_memberOne.f_cert),
                generation -> {}, reader);
        s_providers.add(provider);
        provider.setLocalMemberName("member-one");
        provider.observeMemberNames(Collections.singleton("member-one"));
        reader.arm();

        ExecutorService callers = Executors.newFixedThreadPool(10);
        List<Future<Boolean>> refreshes = new ArrayList<>();
        long start = System.nanoTime();
        try
            {
            refreshes.add(providerRefreshExecutor(provider).submit(provider::refresh));
            for (int i = 1; i < 10; i++)
                {
                refreshes.add(callers.submit(provider::refresh));
                }
            for (int i = 0; i < 3; i++)
                {
                assertTrue("credential wave " + i + " did not align at EOF", reader.awaitEof(i));
                reader.releaseEof(i);
                assertTrue("cleanup wave " + i + " did not receive independent capacity",
                        reader.awaitClose(i));
                reader.releaseClose(i);
                }
            for (Future<Boolean> refresh : refreshes)
                {
                refresh.get(X509PeerProofProvider.CREDENTIAL_OPERATION_TIMEOUT_MILLIS,
                        TimeUnit.MILLISECONDS);
                }
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            assertTrue("valid concurrent refreshes consumed the credential deadline: " + elapsed,
                    elapsed < X509PeerProofProvider.CREDENTIAL_OPERATION_TIMEOUT_MILLIS);
            assertEquals(10, reader.getClosedCount());
            assertTrue(provider.isReady());
            }
        finally
            {
            reader.releaseAll();
            callers.shutdownNow();
            provider.close();
            }
        assertTrue(providerRefreshExecutor(provider).isTerminated());
        assertTrue(providerIoExecutor(provider).isTerminated());
        assertTrue(providerCleanupExecutor(provider).isTerminated());
        provider.close();
        }

    @Test
    public void shouldCloseScheduledAndManualRefreshesWithCleanupInFlight() throws Exception
        {
        ConcurrentEofCredentialReader reader = new ConcurrentEofCredentialReader(
                s_memberOne.f_store.toString(), 4);
        X509PeerProofProvider provider = new X509PeerProofProvider(
                dependencies(s_memberOne, s_memberOne.f_cert, s_memberOne.f_cert),
                generation -> {}, reader);
        s_providers.add(provider);
        provider.setLocalMemberName("member-one");
        provider.observeMemberNames(Collections.singleton("member-one"));
        reader.arm();

        ExecutorService callers = Executors.newFixedThreadPool(3);
        ExecutorService closer = Executors.newSingleThreadExecutor();
        List<Future<Boolean>> refreshes = new ArrayList<>();
        try
            {
            refreshes.add(providerRefreshExecutor(provider).submit(provider::refresh));
            for (int i = 0; i < 3; i++)
                {
                refreshes.add(callers.submit(provider::refresh));
                }
            assertTrue(reader.awaitEof(0));
            reader.releaseEof(0);
            assertTrue(reader.awaitClose(0));

            Future<?> close = closer.submit(provider::close);
            for (int i = 0; i < 100 && !providerCleanupExecutor(provider).isShutdown(); i++)
                {
                Thread.sleep(10L);
                }
            assertTrue(providerRefreshExecutor(provider).isShutdown());
            assertTrue(providerIoExecutor(provider).isShutdown());
            assertTrue(providerCleanupExecutor(provider).isShutdown());
            reader.releaseClose(0);
            close.get(X509PeerProofProvider.CLOSE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            for (Future<Boolean> refresh : refreshes)
                {
                assertFalse(refresh.get(5L, TimeUnit.SECONDS));
                }
            assertEquals(4, reader.getClosedCount());
            assertClosedAndFailClosed(provider);
            assertTrue(providerRefreshExecutor(provider).isTerminated());
            assertTrue(providerIoExecutor(provider).isTerminated());
            assertTrue(providerCleanupExecutor(provider).isTerminated());
            provider.close();
            }
        finally
            {
            reader.releaseAll();
            callers.shutdownNow();
            closer.shutdownNow();
            }
        }

    @Test
    public void shouldCompleteWarmedRsaSignVerifyMicrobenchmark()
        {
        X509PeerProofProvider provider = provider();
        long now = System.currentTimeMillis();
        SubjectProofPayload payload = subject(provider, now, now + 60_000L);
        for (int i = 0; i < 200; i++)
            {
            byte[] proof = provider.createProof(payload);
            assertTrue(provider.verifyProof(proof, payload, now).isValid());
            }

        long start = System.nanoTime();
        for (int i = 0; i < 1_000; i++)
            {
            byte[] proof = provider.createProof(payload);
            assertNotNull(proof);
            assertTrue(provider.verifyProof(proof, payload, now).isValid());
            }
        long elapsed = System.nanoTime() - start;
        assertTrue("RSA sign/verify microbenchmark exceeded 10 seconds: " + elapsed,
                elapsed < 10_000_000_000L);
        }

    private static X509PeerProofProvider provider()
        {
        return provider(s_memberOne, s_memberOne.f_cert, s_memberOne.f_cert);
        }

    private static X509PeerProofProvider provider(Credential identity, Path subject, Path senior)
        {
        X509PeerProofProvider provider = new X509PeerProofProvider(dependencies(identity, subject, senior));
        s_providers.add(provider);
        provider.setLocalMemberName(identity.f_name);
        provider.observeMemberNames(Collections.singleton(identity.f_name));
        return provider;
        }

    private static String readiness(PeerProofReadiness.Stage stage, String oldKey, String newKey,
            Collection<String> producers, Collection<String> recipients, X509PeerProofProvider... providers)
        {
        List<PeerProofReadiness.MemberBinding> members = new ArrayList<>();
        java.util.Map<PeerProofReadiness.MemberBinding, PeerProofReadiness.Response> responses =
                new java.util.LinkedHashMap<>();
        for (int i = 0; i < providers.length; i++)
            {
            X509PeerProofProvider provider = providers[i];
            String name = provider.getIssuerId().equals(X509PeerProofProvider.issuerId("member-two"))
                    ? "member-two" : "member-one";
            PeerProofReadiness.MemberBinding member = new PeerProofReadiness.MemberBinding(
                    "uid-" + i, name, "subject-producer");
            members.add(member);
            }
        PeerProofReadiness.Topology topology = PeerProofReadiness.Topology.of(members, members.get(0));
        for (int i = 0; i < providers.length; i++)
            {
            PeerProofReadiness.MemberBinding member = members.get(i);
            responses.put(member, new PeerProofReadiness.Response(member, topology.getEpoch(),
                    providers[i].getLocalReadinessObservation()));
            }
        return PeerProofReadiness.evaluate(new PeerProofReadiness.Query(PeerProofReadiness.Role.SUBJECT,
                stage, oldKey, newKey), topology, responses,
                System.currentTimeMillis()).getStatus();
        }

    private static PeerProofReadiness.ServiceBinding service(String name, String type, String senior,
            String... memberUids)
        {
        return new PeerProofReadiness.ServiceBinding(name, type, Arrays.asList(memberUids), senior);
        }

    private static PeerProofReadiness.ServiceBinding serviceWithRecipients(String name, String type, String senior,
            Collection<String> memberUids, Collection<String> recipientUids)
        {
        return new PeerProofReadiness.ServiceBinding(name, type, memberUids, senior, recipientUids);
        }

    private static java.util.Map<PeerProofReadiness.MemberBinding, PeerProofReadiness.Response> readinessResponses(
            PeerProofReadiness.Topology topology, List<PeerProofReadiness.MemberBinding> members,
            List<X509PeerProofProvider> providers)
        {
        java.util.Map<PeerProofReadiness.MemberBinding, PeerProofReadiness.Response> responses =
                new java.util.LinkedHashMap<>();
        for (int i = 0; i < members.size(); i++)
            {
            responses.put(members.get(i), new PeerProofReadiness.Response(members.get(i), topology.getEpoch(),
                    providers.get(i).getLocalReadinessObservation()));
            }
        return responses;
        }

    private static String keyId(Credential credential)
        {
        return provider(credential, credential.f_cert, credential.f_cert).getKeyId();
        }

    private static PeerProofDependencies dependencies(Credential identity, Path subject, Path senior)
        {
        return new PeerProofDependencies()
                .setIdentityStoreUrl(identity.f_store.toString()).setIdentityStoreType("PKCS12")
                .setIdentityAlias(ALIAS).setIdentityPasswordProvider(() -> PASSWORD.toCharArray())
                .addSubjectCertificateUrl(subject.toString()).addSeniorCertificateUrl(senior.toString());
        }

    private static PeerProofDependencies protectedDependencies(Path subject, Path senior,
            PasswordProvider identityPassword, PasswordProvider subjectPassword, PasswordProvider seniorPassword)
        {
        return new PeerProofDependencies()
                .setIdentityStoreUrl(s_memberOne.f_store.toString()).setIdentityStoreType("PKCS12")
                .setIdentityAlias(ALIAS).setIdentityPasswordProvider(identityPassword)
                .setSubjectAuthorityStore(new PeerProofDependencies.AuthorityStore(
                        subject.toString(), "JKS", subjectPassword))
                .setSeniorAuthorityStore(new PeerProofDependencies.AuthorityStore(
                        senior.toString(), "PKCS12", seniorPassword));
        }

    private static PeerProofDependencies storeDependencies(Path identity, String identityType,
            Path subject, String subjectType, Path senior, String seniorType)
        {
        return new PeerProofDependencies()
                .setIdentityStoreUrl(identity.toString()).setIdentityStoreType(identityType)
                .setIdentityAlias(ALIAS).setIdentityPasswordProvider(() -> PASSWORD.toCharArray())
                .setSubjectAuthorityStore(new PeerProofDependencies.AuthorityStore(
                        subject.toString(), subjectType, () -> PASSWORD.toCharArray()))
                .setSeniorAuthorityStore(new PeerProofDependencies.AuthorityStore(
                        senior.toString(), seniorType, () -> PASSWORD.toCharArray()));
        }

    private static void installPassword(PeerProofDependencies deps, PasswordPosition position,
            Path subject, Path senior, PasswordProvider password)
        {
        switch (position)
            {
            case IDENTITY:
                deps.setIdentityPasswordProvider(password);
                break;
            case SUBJECT:
                deps.setSubjectAuthorityStore(new PeerProofDependencies.AuthorityStore(
                        subject.toString(), "JKS", password));
                break;
            case SENIOR:
                deps.setSeniorAuthorityStore(new PeerProofDependencies.AuthorityStore(
                        senior.toString(), "PKCS12", password));
                break;
            default:
                throw new AssertionError(position);
            }
        }

    private static void installPkcs12(PeerProofDependencies deps, PasswordPosition position, Path store)
        {installPkcs12(deps, position, store, () -> PASSWORD.toCharArray());}

    private static void installPkcs12(PeerProofDependencies deps, PasswordPosition position, Path store,
            PasswordProvider password)
        {
        switch (position)
            {
            case IDENTITY:
                deps.setIdentityStoreUrl(store.toString()).setIdentityStoreType("PKCS12")
                        .setIdentityPasswordProvider(password);
                break;
            case SUBJECT:
                deps.setSubjectAuthorityStore(new PeerProofDependencies.AuthorityStore(
                        store.toString(), "PKCS12", password));
                break;
            case SENIOR:
                deps.setSeniorAuthorityStore(new PeerProofDependencies.AuthorityStore(
                        store.toString(), "PKCS12", password));
                break;
            default:
                throw new AssertionError(position);
            }
        }

    private static Path pkcs12WithIterations(String name, int cMac, int cKey, int cCertificate)
            throws Exception
        {
        Path store = s_dir.resolve(name + ".p12");
        keytool("-J-Dkeystore.pkcs12.macIterationCount=" + cMac,
                "-J-Dkeystore.pkcs12.keyPbeIterationCount=" + cKey,
                "-J-Dkeystore.pkcs12.certPbeIterationCount=" + cCertificate,
                "-genkeypair", "-alias", ALIAS, "-keyalg", "RSA", "-keysize", "2048",
                "-validity", "3650", "-dname", "CN=" + name,
                "-ext", "SAN=uri:" + X509PeerProofProvider.issuerId("member-one"),
                "-ext", "KU=digitalSignature", "-keystore", store.toString(),
                "-storetype", "PKCS12", "-storepass", PASSWORD, "-keypass", PASSWORD,
                "-noprompt");
        return store;
        }

    private static List<PfxPlatformFamily> pkcs12PlatformFamilies() throws Exception
        {
        String aes128 = "2.16.840.1.101.3.4.1.2";
        String aes256 = "2.16.840.1.101.3.4.1.42";
        String hmac1 = "1.2.840.113549.2.7";
        String hmac224 = "1.2.840.113549.2.8";
        String hmac256 = "1.2.840.113549.2.9";
        String hmac384 = "1.2.840.113549.2.10";
        String hmac512 = "1.2.840.113549.2.11";
        String mac256 = "2.16.840.1.101.3.4.2.1";
        String mac384 = "2.16.840.1.101.3.4.2.2";
        String mac512 = "2.16.840.1.101.3.4.2.3";
        Path sha1 = pkcs12PlatformFamily("p77-aes128-sha1-sha256",
                "PBEWithHmacSHA1AndAES_128", "HmacPBESHA256");
        Path sha224 = pkcs12PlatformFamily("p77-aes128-sha224-sha384",
                "PBEWithHmacSHA224AndAES_128", "HmacPBESHA384");
        Path sha256 = pkcs12PlatformFamily("p77-aes128-sha256-sha512",
                "PBEWithHmacSHA256AndAES_128", "HmacPBESHA512");
        Path sha384 = pkcs12PlatformFamily("p77-aes256-sha384-sha256",
                "PBEWithHmacSHA384AndAES_256", "HmacPBESHA256");
        Path sha512 = pkcs12PlatformFamily("p77-aes256-sha512-sha384",
                "PBEWithHmacSHA512AndAES_256", "HmacPBESHA384");
        return Arrays.asList(
                new PfxPlatformFamily(sha1, aes128, 16, hmac1, ParameterShape.NULL,
                        mac256, ParameterShape.NULL),
                new PfxPlatformFamily(sha224, aes128, 16, hmac224, ParameterShape.NULL,
                        mac384, ParameterShape.NULL),
                new PfxPlatformFamily(sha256, aes128, 16, hmac256, ParameterShape.NULL,
                        mac512, ParameterShape.NULL),
                new PfxPlatformFamily(sha384, aes256, 32, hmac384, ParameterShape.NULL,
                        mac256, ParameterShape.NULL),
                new PfxPlatformFamily(sha512, aes256, 32, hmac512, ParameterShape.NULL,
                        mac384, ParameterShape.NULL),
                new PfxPlatformFamily(mutatePbkdf2("p77-omitted-key-length.p12", sha256,
                        Pbkdf2Mutation.OMIT_KEY_LENGTH), aes128, null, hmac256,
                        ParameterShape.NULL, mac512, ParameterShape.NULL),
                new PfxPlatformFamily(mutatePbkdf2("p77-default-prf.p12", sha1,
                        Pbkdf2Mutation.OMIT_PRF), aes128, 16, hmac1,
                        ParameterShape.OMITTED, mac256, ParameterShape.NULL),
                new PfxPlatformFamily(mutatePbkdf2("p78-absent-sha1-prf-parameters.p12", sha1,
                        Pbkdf2Mutation.OMIT_PRF_PARAMETERS), aes128, 16, hmac1,
                        ParameterShape.ABSENT, mac256, ParameterShape.NULL),
                new PfxPlatformFamily(mutatePbkdf2("p78-absent-sha224-prf-parameters.p12", sha224,
                        Pbkdf2Mutation.OMIT_PRF_PARAMETERS), aes128, 16, hmac224,
                        ParameterShape.ABSENT, mac384, ParameterShape.NULL),
                new PfxPlatformFamily(mutatePbkdf2("p78-absent-sha256-prf-parameters.p12", sha256,
                        Pbkdf2Mutation.OMIT_PRF_PARAMETERS), aes128, 16, hmac256,
                        ParameterShape.ABSENT, mac512, ParameterShape.NULL),
                new PfxPlatformFamily(mutatePbkdf2("p78-absent-sha384-prf-parameters.p12", sha384,
                        Pbkdf2Mutation.OMIT_PRF_PARAMETERS), aes256, 32, hmac384,
                        ParameterShape.ABSENT, mac256, ParameterShape.NULL),
                new PfxPlatformFamily(mutatePbkdf2("p78-absent-sha512-prf-parameters.p12", sha512,
                        Pbkdf2Mutation.OMIT_PRF_PARAMETERS), aes256, 32, hmac512,
                        ParameterShape.ABSENT, mac384, ParameterShape.NULL),
                new PfxPlatformFamily(omitOuterMacParameters(
                        "p78-absent-sha256-mac-parameters.p12", sha1), aes128, 16, hmac1,
                        ParameterShape.NULL, mac256, ParameterShape.ABSENT),
                new PfxPlatformFamily(omitOuterMacParameters(
                        "p78-absent-sha384-mac-parameters.p12", sha224), aes128, 16, hmac224,
                        ParameterShape.NULL, mac384, ParameterShape.ABSENT),
                new PfxPlatformFamily(omitOuterMacParameters(
                        "p78-absent-sha512-mac-parameters.p12", sha256), aes128, 16, hmac256,
                        ParameterShape.NULL, mac512, ParameterShape.ABSENT));
        }

    /** Assert exact algorithms and parameter shapes before treating a fixture as SUN evidence. */
    private static void assertPkcs12PlatformFamily(PfxPlatformFamily family) throws Exception
        {
        PfxDer pfx = PfxDer.parse(Files.readAllBytes(family.f_path));
        PfxDer macAlgorithm = pfx.child(2, 0, 0);
        assertAlgorithmIdentifier(macAlgorithm, family.f_sMacOid, family.f_macShape);
        int cbDigest = family.f_sMacOid.endsWith(".1") ? 32
                : family.f_sMacOid.endsWith(".2") ? 48 : 64;
        assertEquals(0x04, pfx.child(2, 0, 1).f_nTag);
        assertEquals(cbDigest, pfx.child(2, 0, 1).f_abValue.length);

        PfxDer authSafeOctets = pfx.child(1, 1, 0);
        PfxDer authenticatedSafe = PfxDer.parse(authSafeOctets.f_abValue);
        int cAlgorithms = 0;
        for (PfxDer contentInfo : authenticatedSafe.f_listChildren)
            {
            if (contentInfo.isOidChild(0, "1.2.840.113549.1.7.1"))
                {
                PfxDer safeContents = PfxDer.parse(contentInfo.child(1, 0).f_abValue);
                for (PfxDer bag : safeContents.f_listChildren)
                    {
                    if (bag.isOidChild(0, "1.2.840.113549.1.12.10.1.2"))
                        {
                        assertPbes2Algorithm(bag.child(1, 0, 0), family);
                        cAlgorithms++;
                        }
                    }
                }
            else if (contentInfo.isOidChild(0, "1.2.840.113549.1.7.6"))
                {
                assertPbes2Algorithm(contentInfo.child(1, 0, 1, 1), family);
                cAlgorithms++;
                }
            }
        assertEquals("expected exact key and certificate algorithms in " + family.f_path,
                2, cAlgorithms);
        }

    private static void assertPbes2Algorithm(PfxDer algorithm, PfxPlatformFamily family)
            throws Exception
        {
        assertEquals(0x30, algorithm.f_nTag);
        assertTrue(algorithm.isOidChild(0, "1.2.840.113549.1.5.13"));
        assertEquals(2, algorithm.f_listChildren.size());
        PfxDer parameters = algorithm.child(1);
        assertEquals(2, parameters.f_listChildren.size());

        PfxDer kdf = parameters.child(0);
        assertTrue(kdf.isOidChild(0, "1.2.840.113549.1.5.12"));
        assertEquals(2, kdf.f_listChildren.size());
        PfxDer pbkdf2 = kdf.child(1);
        assertEquals(0x04, pbkdf2.child(0).f_nTag);
        assertEquals(0x02, pbkdf2.child(1).f_nTag);
        int index = 2;
        if (family.f_nKeyLength == null)
            {
            assertTrue(index >= pbkdf2.f_listChildren.size()
                    || pbkdf2.child(index).f_nTag != 0x02);
            }
        else
            {
            assertEquals(0x02, pbkdf2.child(index).f_nTag);
            assertEquals(family.f_nKeyLength.intValue(),
                    new java.math.BigInteger(pbkdf2.child(index).f_abValue).intValueExact());
            index++;
            }
        if (family.f_prfShape == ParameterShape.OMITTED)
            {
            assertEquals(index, pbkdf2.f_listChildren.size());
            }
        else
            {
            assertEquals(index + 1, pbkdf2.f_listChildren.size());
            assertAlgorithmIdentifier(pbkdf2.child(index), family.f_sPrfOid, family.f_prfShape);
            }

        PfxDer cipher = parameters.child(1);
        assertEquals(0x30, cipher.f_nTag);
        assertTrue(cipher.isOidChild(0, family.f_sAesOid));
        assertEquals(2, cipher.f_listChildren.size());
        assertEquals(0x04, cipher.child(1).f_nTag);
        assertEquals(16, cipher.child(1).f_abValue.length);
        }

    private static void assertAlgorithmIdentifier(PfxDer algorithm, String oid, ParameterShape shape)
            throws Exception
        {
        assertEquals(0x30, algorithm.f_nTag);
        assertTrue(algorithm.isOidChild(0, oid));
        if (shape == ParameterShape.ABSENT)
            {
            assertEquals(1, algorithm.f_listChildren.size());
            }
        else
            {
            assertEquals(ParameterShape.NULL, shape);
            assertEquals(2, algorithm.f_listChildren.size());
            assertEquals(0x05, algorithm.child(1).f_nTag);
            assertEquals(0, algorithm.child(1).f_abValue.length);
            }
        }

    private static Path pkcs12PlatformFamily(String name, String pbe, String mac) throws Exception
        {
        Path store = s_dir.resolve(name + ".p12");
        keytool("-J-Dkeystore.pkcs12.keyProtectionAlgorithm=" + pbe,
                "-J-Dkeystore.pkcs12.certProtectionAlgorithm=" + pbe,
                "-J-Dkeystore.pkcs12.macAlgorithm=" + mac,
                "-genkeypair", "-alias", ALIAS, "-keyalg", "RSA", "-keysize", "2048",
                "-validity", "3650", "-dname", "CN=" + name,
                "-ext", "SAN=uri:" + X509PeerProofProvider.issuerId("member-one"),
                "-ext", "KU=digitalSignature", "-keystore", store.toString(),
                "-storetype", "PKCS12", "-storepass", PASSWORD, "-keypass", PASSWORD,
                "-noprompt");
        return store;
        }

    private static Path pkcs12TrustedAttributeStore(String name) throws Exception
        {
        Path store = s_dir.resolve(name);
        keytool("-J-Dkeystore.pkcs12.certProtectionAlgorithm=NONE",
                "-importcert", "-alias", "member-one", "-file", s_memberOne.f_cert.toString(),
                "-keystore", store.toString(), "-storetype", "PKCS12",
                "-storepass", PASSWORD, "-noprompt");
        keytool("-J-Dkeystore.pkcs12.certProtectionAlgorithm=NONE",
                "-importcert", "-alias", "member-two", "-file", s_memberTwo.f_cert.toString(),
                "-keystore", store.toString(), "-storetype", "PKCS12",
                "-storepass", PASSWORD, "-noprompt");
        return store;
        }

    private static Path mutatePbkdf2(String name, Path source, Pbkdf2Mutation mutation)
            throws Exception
        {
        PfxDer pfx = PfxDer.parse(Files.readAllBytes(source));
        PfxDer authSafeOctets = pfx.child(1, 1, 0);
        PfxDer authenticatedSafe = PfxDer.parse(authSafeOctets.f_abValue);
        int cMutated = 0;
        for (PfxDer contentInfo : authenticatedSafe.f_listChildren)
            {
            if (contentInfo.isOidChild(0, "1.2.840.113549.1.7.1"))
                {
                PfxDer safeOctets = contentInfo.child(1, 0);
                PfxDer safeContents = PfxDer.parse(safeOctets.f_abValue);
                for (PfxDer bag : safeContents.f_listChildren)
                    {
                    if (bag.isOidChild(0, "1.2.840.113549.1.12.10.1.2"))
                        {
                        mutation.apply(bag.child(1, 0, 0, 1, 0, 1));
                        cMutated++;
                        }
                    }
                safeOctets.f_abValue = safeContents.encode();
                }
            else if (contentInfo.isOidChild(0, "1.2.840.113549.1.7.6"))
                {
                mutation.apply(contentInfo.child(1, 0, 1, 1, 1, 0, 1));
                cMutated++;
                }
            }
        assertEquals("expected key and certificate PBKDF2 declarations", 2, cMutated);
        authSafeOctets.f_abValue = authenticatedSafe.encode();
        refreshPfxMac(pfx, authSafeOctets.f_abValue);
        return writeCredential(name, pfx.encode());
        }

    private static Path omitOuterMacParameters(String name, Path source) throws Exception
        {
        PfxDer pfx = PfxDer.parse(Files.readAllBytes(source));
        PfxDer algorithm = pfx.child(2, 0, 0);
        assertEquals(2, algorithm.f_listChildren.size());
        algorithm.f_listChildren.remove(1);
        return writeCredential(name, pfx.encode());
        }

    private static Path omitVisibleBagAttributes(String name, Path source) throws Exception
        {
        PfxDer pfx = PfxDer.parse(Files.readAllBytes(source));
        PfxDer authSafeOctets = pfx.child(1, 1, 0);
        PfxDer authenticatedSafe = PfxDer.parse(authSafeOctets.f_abValue);
        int cRemoved = 0;
        for (PfxDer contentInfo : authenticatedSafe.f_listChildren)
            {
            if (!contentInfo.isOidChild(0, "1.2.840.113549.1.7.1"))
                {
                continue;
                }
            PfxDer safeOctets = contentInfo.child(1, 0);
            PfxDer safeContents = PfxDer.parse(safeOctets.f_abValue);
            for (PfxDer bag : safeContents.f_listChildren)
                {
                if (bag.f_listChildren.size() == 3)
                    {
                    bag.f_listChildren.remove(2);
                    cRemoved++;
                    }
                }
            safeOctets.f_abValue = safeContents.encode();
            }
        assertTrue("expected visible bag attributes", cRemoved > 0);
        authSafeOctets.f_abValue = authenticatedSafe.encode();
        refreshPfxMac(pfx, authSafeOctets.f_abValue);
        return writeCredential(name, pfx.encode());
        }

    private static void refreshPfxMac(PfxDer pfx, byte[] authenticatedSafe) throws Exception
        {
        PfxDer macData = pfx.child(2);
        PfxDer digestInfo = macData.child(0);
        String oid = digestInfo.child(0, 0).oid();
        String algorithm;
        if ("2.16.840.1.101.3.4.2.1".equals(oid))
            {
            algorithm = "HmacPBESHA256";
            }
        else if ("2.16.840.1.101.3.4.2.2".equals(oid))
            {
            algorithm = "HmacPBESHA384";
            }
        else if ("2.16.840.1.101.3.4.2.3".equals(oid))
            {
            algorithm = "HmacPBESHA512";
            }
        else
            {
            throw new AssertionError("unsupported test MAC " + oid);
            }
        int iterations = macData.f_listChildren.size() == 3
                ? new java.math.BigInteger(macData.child(2).f_abValue).intValueExact() : 1;
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance(algorithm);
        javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBE");
        javax.crypto.spec.PBEKeySpec keySpec = new javax.crypto.spec.PBEKeySpec(PASSWORD.toCharArray());
        try
            {
            mac.init(factory.generateSecret(keySpec), new javax.crypto.spec.PBEParameterSpec(
                    macData.child(1).f_abValue, iterations));
            digestInfo.child(1).f_abValue = mac.doFinal(authenticatedSafe);
            }
        finally
            {
            keySpec.clearPassword();
            }
        }

    private static List<PfxParameterCase> pfxParameterCases() throws IOException
        {
        String aes128 = "2.16.840.1.101.3.4.1.2";
        String aes192 = "2.16.840.1.101.3.4.1.22";
        String aes256 = "2.16.840.1.101.3.4.1.42";
        String sha256 = "2.16.840.1.101.3.4.2.1";
        String sha384 = "2.16.840.1.101.3.4.2.2";
        String sha512 = "2.16.840.1.101.3.4.2.3";
        String hmac256 = "1.2.840.113549.2.9";
        List<PfxParameterCase> cases = new ArrayList<>();
        cases.add(pfxCase("unsupported-aes192", aes192, 24, 16,
                ParameterShape.NULL, hmac256, sha384, ParameterShape.ABSENT, 48,
                PfxIdentifier.NONE, false));

        for (int keyLength : new int[] {0, -1, Integer.MAX_VALUE})
            {
            cases.add(pfxCase("invalid-key-length-" + keyLength, aes256, keyLength, 16,
                    ParameterShape.NULL, hmac256, sha256, ParameterShape.NULL, 32,
                    PfxIdentifier.NONE, false));
            }
        for (Object[] mismatch : new Object[][] {{aes128, 24}, {aes128, 32},
                {aes192, 16}, {aes192, 32}, {aes256, 16}, {aes256, 24}})
            {
            cases.add(pfxCase("mismatched-" + mismatch[0] + "-" + mismatch[1],
                    (String) mismatch[0], (Integer) mismatch[1], 16,
                    ParameterShape.NULL, hmac256, sha256, ParameterShape.NULL, 32,
                    PfxIdentifier.NONE, false));
            }
        cases.add(pfxCase("iv-15", aes256, 32, 15, ParameterShape.NULL, hmac256,
                sha256, ParameterShape.NULL, 32, PfxIdentifier.NONE, false));
        cases.add(pfxCase("iv-17", aes256, 32, 17, ParameterShape.NULL, hmac256,
                sha256, ParameterShape.NULL, 32, PfxIdentifier.NONE, false));
        cases.add(pfxCase("unsupported-mac-sha1", aes256, 32, 16, ParameterShape.NULL,
                hmac256, "1.3.14.3.2.26", ParameterShape.NULL, 20, PfxIdentifier.NONE, false));
        cases.add(pfxCase("wrong-mac-digest-width", aes256, 32, 16, ParameterShape.NULL,
                hmac256, sha256, ParameterShape.NULL, 31, PfxIdentifier.NONE, false));
        cases.add(pfxCase("malformed-mac-parameters", aes256, 32, 16, ParameterShape.NULL,
                hmac256, sha256, ParameterShape.MALFORMED, 32, PfxIdentifier.NONE, false));
        cases.add(pfxCase("non-empty-null-mac-parameters", aes256, 32, 16, ParameterShape.NULL,
                hmac256, sha256, ParameterShape.NON_EMPTY_NULL, 32, PfxIdentifier.NONE, false));
        cases.add(pfxCase("unsupported-prf", aes256, 32, 16, ParameterShape.NULL,
                "1.2.840.113549.2.6", sha256, ParameterShape.NULL, 32,
                PfxIdentifier.NONE, false));
        cases.add(pfxCase("malformed-prf-parameters", aes256, 32, 16,
                ParameterShape.MALFORMED, hmac256, sha256, ParameterShape.NULL, 32,
                PfxIdentifier.NONE, false));
        cases.add(pfxCase("non-empty-null-prf-parameters", aes256, 32, 16,
                ParameterShape.NON_EMPTY_NULL, hmac256, sha256, ParameterShape.NULL, 32,
                PfxIdentifier.NONE, false));
        for (PfxIdentifier identifier : PfxIdentifier.values())
            {
            if (identifier != PfxIdentifier.NONE)
                {
                cases.add(pfxCase("non-oid-" + identifier.name().toLowerCase(), aes256, 32, 16,
                        ParameterShape.NULL, hmac256, sha256, ParameterShape.NULL, 32,
                        identifier, false));
                }
            }
        return cases;
        }

    private static PfxParameterCase pfxCase(String name, String aesOid, Integer keyLength,
            int cbIv, ParameterShape prfShape, String prfOid, String macOid,
            ParameterShape macShape, int cbDigest, PfxIdentifier malformedIdentifier,
            boolean accepted) throws IOException
        {
        if (accepted)
            {
            throw new AssertionError("accepted forms require an integrity-correct SUN fixture");
            }
        byte[] salt = der(0x04, new byte[16]);
        List<byte[]> pbkdf2Values = new ArrayList<>(Arrays.asList(salt, derSignedInteger(1)));
        if (keyLength != null)
            {
            pbkdf2Values.add(derSignedInteger(keyLength));
            }
        if (prfShape != ParameterShape.OMITTED)
            {
            pbkdf2Values.add(algorithmIdentifier(prfOid, prfShape,
                    malformedIdentifier == PfxIdentifier.PRF));
            }
        byte[] kdf = derSequence(identifier("1.2.840.113549.1.5.12",
                malformedIdentifier == PfxIdentifier.KDF),
                derSequence(pbkdf2Values.toArray(new byte[0][])));
        byte[] aes = derSequence(identifier(aesOid, malformedIdentifier == PfxIdentifier.AES),
                der(0x04, new byte[cbIv]));
        byte[] pbes2 = derSequence(identifier("1.2.840.113549.1.5.13",
                malformedIdentifier == PfxIdentifier.ENCRYPTION), derSequence(kdf, aes));
        byte[] encryptedPrivateKey = derSequence(pbes2, der(0x04, new byte[] {1}));
        byte[] keyBag = derSequence(identifier("1.2.840.113549.1.12.10.1.2",
                malformedIdentifier == PfxIdentifier.BAG), der(0xa0, encryptedPrivateKey));

        byte[] certificate = derSequence(derNull(), derNull(), derNull());
        byte[] certificateBag = derSequence(identifier("1.2.840.113549.1.9.22.1",
                malformedIdentifier == PfxIdentifier.CERTIFICATE), der(0xa0, der(0x04, certificate)));
        byte[] certBag = derSequence(identifier("1.2.840.113549.1.12.10.1.3", false),
                der(0xa0, certificateBag));
        byte[] safeContents = derSequence(keyBag, certBag);
        byte[] innerContent = derSequence(identifier("1.2.840.113549.1.7.1",
                malformedIdentifier == PfxIdentifier.CONTENT), der(0xa0, der(0x04, safeContents)));
        byte[] authenticatedSafe = derSequence(innerContent);
        byte[] authSafe = derSequence(identifier("1.2.840.113549.1.7.1", false),
                der(0xa0, der(0x04, authenticatedSafe)));

        byte[] macAlgorithm = algorithmIdentifier(macOid, macShape,
                malformedIdentifier == PfxIdentifier.MAC);
        byte[] digestInfo = derSequence(macAlgorithm, der(0x04, new byte[cbDigest]));
        byte[] macData = derSequence(digestInfo, der(0x04, new byte[8]), derSignedInteger(1));
        return new PfxParameterCase(name, derSequence(derSignedInteger(3), authSafe, macData));
        }

    private static List<PfxStructureCase> pfxStructureCases(Path canonical) throws Exception
        {
        List<PfxStructureCase> cases = new ArrayList<>();
        for (PfxStructure structure : PfxStructure.values())
            {
            String name = structure.name().toLowerCase();
            cases.add(new PfxStructureCase(name,
                    Files.readAllBytes(mutatePfxStructure("p77-" + name + ".p12", canonical, structure))));
            }
        return cases;
        }

    /** Mutate exactly one field in an integrity-correct keytool PFX and refresh its outer MAC. */
    private static Path mutatePfxStructure(String name, Path canonical, PfxStructure structure)
            throws Exception
        {
        PfxDer pfx = PfxDer.parse(Files.readAllBytes(canonical));
        PfxDer authSafeOctets = pfx.child(1, 1, 0);
        PfxDer authenticatedSafe = PfxDer.parse(authSafeOctets.f_abValue);
        PfxDer dataContent = contentInfo(authenticatedSafe, "1.2.840.113549.1.7.1");
        PfxDer safeOctets = dataContent.child(1, 0);
        PfxDer safeContents = PfxDer.parse(safeOctets.f_abValue);
        PfxDer keyBag = safeBag(safeContents, "1.2.840.113549.1.12.10.1.2");
        PfxDer encryptedPrivateKey = keyBag.child(1, 0);
        PfxDer encryptedData = contentInfo(authenticatedSafe, "1.2.840.113549.1.7.6").child(1, 0);
        PfxDer encryptedContentInfo = encryptedData.child(1);
        PfxDer friendly = PfxDer.parse(bagAttribute(
                "1.2.840.113549.1.9.20", derBmpString("peer")));

        switch (structure)
            {
            case KEY_PAYLOAD_WRONG_TAG:
                encryptedPrivateKey.f_listChildren.set(1, PfxDer.parse(derSignedInteger(1)));
                break;
            case KEY_PAYLOAD_CONSTRUCTED:
                encryptedPrivateKey.f_listChildren.set(1,
                        PfxDer.parse(der(0x24, der(0x04, new byte[] {1}))));
                break;
            case KEY_PAYLOAD_EMPTY:
                encryptedPrivateKey.f_listChildren.set(1, PfxDer.parse(der(0x04, new byte[0])));
                break;
            case KEY_PAYLOAD_OMITTED:
                encryptedPrivateKey.f_listChildren.remove(1);
                break;
            case KEY_PAYLOAD_EXTRA:
                encryptedPrivateKey.f_listChildren.add(PfxDer.parse(derNull()));
                break;
            case BAG_MISSING_VALUE:
                while (keyBag.f_listChildren.size() > 1)
                    {
                    keyBag.f_listChildren.remove(1);
                    }
                break;
            case BAG_EXTRA_CHILD:
                while (keyBag.f_listChildren.size() < 4)
                    {
                    keyBag.f_listChildren.add(PfxDer.parse(derNull()));
                    }
                break;
            case SAFE_CONTENTS_EMPTY:
                safeContents.f_listChildren.clear();
                break;
            case ATTRIBUTE_WRONG_CONTAINER:
                setBagAttributes(keyBag, PfxDer.parse(derSequence(friendly.encode())));
                break;
            case ATTRIBUTE_MALFORMED_SEQUENCE:
                setBagAttributes(keyBag, PfxDer.parse(derSet(derNull())));
                break;
            case ATTRIBUTE_NON_OID:
                setBagAttributes(keyBag, PfxDer.parse(derSet(derSequence(
                        der(0x04, oidValue("1.2.840.113549.1.9.20")),
                        derSet(derBmpString("peer"))))));
                break;
            case ATTRIBUTE_MALFORMED_OID:
                setBagAttributes(keyBag, PfxDer.parse(derSet(derSequence(
                        der(0x06, new byte[] {(byte) 0x80}), derSet(derBmpString("peer"))))));
                break;
            case ATTRIBUTE_WRONG_VALUE_SET:
                setBagAttributes(keyBag, PfxDer.parse(derSet(derSequence(
                        identifier("1.2.840.113549.1.9.20", false),
                        derSequence(derBmpString("peer"))))));
                break;
            case ATTRIBUTE_EMPTY_VALUE_SET:
                setBagAttributes(keyBag, PfxDer.parse(derSet(derSequence(
                        identifier("1.2.840.113549.1.9.20", false), derSet()))));
                break;
            case ATTRIBUTE_MULTI_VALUE_SET:
                setBagAttributes(keyBag, PfxDer.parse(derSet(derSequence(
                        identifier("1.2.840.113549.1.9.20", false),
                        derSet(derBmpString("peer-one"), derBmpString("peer-two"))))));
                break;
            case ATTRIBUTE_EMPTY_SET:
                setBagAttributes(keyBag, PfxDer.parse(derSet()));
                break;
            case ATTRIBUTE_UNKNOWN:
                setBagAttributes(keyBag, PfxDer.parse(derSet(
                        bagAttribute("1.2.3.4", der(0x04, new byte[] {1})))));
                break;
            case ATTRIBUTE_DUPLICATE_OID:
                setBagAttributes(keyBag, PfxDer.parse(derSet(friendly.encode(), friendly.encode())));
                break;
            case ATTRIBUTE_FRIENDLY_NAME_WRONG_TAG:
                setBagAttributes(keyBag, PfxDer.parse(derSet(bagAttribute(
                        "1.2.840.113549.1.9.20", der(0x04, new byte[] {1})))));
                break;
            case ATTRIBUTE_FRIENDLY_NAME_EMPTY:
                setBagAttributes(keyBag, PfxDer.parse(derSet(bagAttribute(
                        "1.2.840.113549.1.9.20", der(0x1e, new byte[0])))));
                break;
            case ATTRIBUTE_FRIENDLY_NAME_ODD_WIDTH:
                setBagAttributes(keyBag, PfxDer.parse(derSet(bagAttribute(
                        "1.2.840.113549.1.9.20", der(0x1e, new byte[] {0, 1, 2})))));
                break;
            case ATTRIBUTE_FRIENDLY_NAME_EXCESSIVE:
                setBagAttributes(keyBag, PfxDer.parse(derSet(bagAttribute(
                        "1.2.840.113549.1.9.20", der(0x1e, new byte[258])))));
                break;
            case ATTRIBUTE_LOCAL_KEY_ID_WRONG_TAG:
                setBagAttributes(keyBag, PfxDer.parse(derSet(bagAttribute(
                        "1.2.840.113549.1.9.21", derBmpString("id")))));
                break;
            case ATTRIBUTE_LOCAL_KEY_ID_EMPTY:
                setBagAttributes(keyBag, PfxDer.parse(derSet(bagAttribute(
                        "1.2.840.113549.1.9.21", der(0x04, new byte[0])))));
                break;
            case ATTRIBUTE_LOCAL_KEY_ID_EXCESSIVE:
                setBagAttributes(keyBag, PfxDer.parse(derSet(bagAttribute(
                        "1.2.840.113549.1.9.21", der(0x04, new byte[257])))));
                break;
            case ATTRIBUTE_TRUSTED_KEY_USAGE_WRONG_TAG:
                setBagAttributes(keyBag, PfxDer.parse(derSet(bagAttribute(
                        "2.16.840.1.113894.746875.1.1",
                        der(0x04, oidValue("2.5.29.37.0"))))));
                break;
            case ATTRIBUTE_TRUSTED_KEY_USAGE_WRONG_OID:
                setBagAttributes(keyBag, PfxDer.parse(derSet(bagAttribute(
                        "2.16.840.1.113894.746875.1.1",
                        identifier("2.5.29.37.1", false)))));
                break;
            case ATTRIBUTE_EXCESSIVE:
                setBagAttributes(keyBag, PfxDer.parse(derSet(
                        friendly.encode(), friendly.encode(), friendly.encode(), friendly.encode())));
                break;
            case ENCRYPTED_CONTENT_WRONG_TAG:
                encryptedContentInfo.f_listChildren.set(2, PfxDer.parse(der(0x04, new byte[] {1})));
                break;
            case ENCRYPTED_CONTENT_CONSTRUCTED:
                encryptedContentInfo.f_listChildren.set(2,
                        PfxDer.parse(der(0xa0, der(0x04, new byte[] {1}))));
                break;
            case ENCRYPTED_CONTENT_EMPTY:
                encryptedContentInfo.f_listChildren.set(2, PfxDer.parse(der(0x80, new byte[0])));
                break;
            case ENCRYPTED_CONTENT_OMITTED:
                encryptedContentInfo.f_listChildren.remove(2);
                break;
            case ENCRYPTED_CONTENT_EXTRA:
                encryptedContentInfo.f_listChildren.add(PfxDer.parse(derNull()));
                break;
            case ENCRYPTED_CONTENT_MALFORMED_FRAGMENT:
                encryptedContentInfo.f_listChildren.set(2,
                        PfxDer.parse(der(0xa0, derSignedInteger(1))));
                break;
            case ENCRYPTED_CONTENT_EXCESSIVE_FRAGMENTS:
                byte[][] fragments = new byte[X509PeerProofProvider.MAX_DER_CHILDREN + 1][];
                Arrays.fill(fragments, der(0x04, new byte[] {1}));
                encryptedContentInfo.f_listChildren.set(2,
                        PfxDer.parse(der(0xa0, concatenate(fragments))));
                break;
            case ENCRYPTED_DATA_EXTRA:
                encryptedData.f_listChildren.add(PfxDer.parse(der(0xa1, derSet())));
                break;
            default:
                throw new AssertionError(structure);
            }

        safeOctets.f_abValue = safeContents.encode();
        authSafeOctets.f_abValue = authenticatedSafe.encode();
        refreshPfxMac(pfx, authSafeOctets.f_abValue);
        return writeCredential(name, pfx.encode());
        }

    private static PfxDer contentInfo(PfxDer authenticatedSafe, String oid) throws IOException
        {
        for (PfxDer contentInfo : authenticatedSafe.f_listChildren)
            {
            if (contentInfo.isOidChild(0, oid))
                {
                return contentInfo;
                }
            }
        throw new IOException("missing test ContentInfo");
        }

    private static PfxDer safeBag(PfxDer safeContents, String oid) throws IOException
        {
        for (PfxDer bag : safeContents.f_listChildren)
            {
            if (bag.isOidChild(0, oid))
                {
                return bag;
                }
            }
        throw new IOException("missing test SafeBag");
        }

    private static void setBagAttributes(PfxDer bag, PfxDer attributes)
        {
        if (bag.f_listChildren.size() == 2)
            {
            bag.f_listChildren.add(attributes);
            }
        else
            {
            bag.f_listChildren.set(2, attributes);
            }
        }

    private static byte[] bagAttribute(String oid, byte[] value) throws IOException
        {return derSequence(identifier(oid, false), derSet(value));}

    private static byte[] derBmpString(String value) throws IOException
        {
        byte[] bytes = new byte[value.length() * 2];
        for (int i = 0; i < value.length(); i++)
            {
            bytes[i * 2] = (byte) (value.charAt(i) >>> 8);
            bytes[i * 2 + 1] = (byte) value.charAt(i);
            }
        return der(0x1e, bytes);
        }

    private static byte[] derSet(byte[]... values) throws IOException
        {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        for (byte[] value : values)
            {
            bytes.write(value);
            }
        return der(0x31, bytes.toByteArray());
        }

    private static byte[] algorithmIdentifier(String oid, ParameterShape shape, boolean malformedOid)
            throws IOException
        {
        byte[] identifier = identifier(oid, malformedOid);
        switch (shape)
            {
            case ABSENT:
                return derSequence(identifier);
            case NULL:
                return derSequence(identifier, derNull());
            case MALFORMED:
                return derSequence(identifier, der(0x04, new byte[0]));
            case NON_EMPTY_NULL:
                return derSequence(identifier, der(0x05, new byte[] {0}));
            default:
                throw new IllegalArgumentException("algorithm identifier cannot be omitted");
            }
        }

    private static byte[] identifier(String oid, boolean malformed) throws IOException
        {return der(malformed ? 0x04 : 0x06, oidValue(oid));}

    private static byte[] oidValue(String oid) throws IOException
        {
        String[] parts = oid.split("\\.");
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        bytes.write(Integer.parseInt(parts[0]) * 40 + Integer.parseInt(parts[1]));
        for (int i = 2; i < parts.length; i++)
            {
            long value = Long.parseLong(parts[i]);
            byte[] encoded = new byte[10];
            int offset = encoded.length;
            encoded[--offset] = (byte) (value & 0x7f);
            while ((value >>>= 7) != 0)
                {
                encoded[--offset] = (byte) (value & 0x7f | 0x80);
                }
            bytes.write(encoded, offset, encoded.length - offset);
            }
        return bytes.toByteArray();
        }

    private static byte[] derSignedInteger(int value) throws IOException
        {return der(0x02, java.math.BigInteger.valueOf(value).toByteArray());}

    private static void assertCleared(char[] password)
        {
        for (char ch : password)
            {
            assertEquals('0', ch);
            }
        }

    private static void assertEventuallyCleared(char[] password) throws InterruptedException
        {
        for (int i = 0; i < 100 && !isCleared(password); i++)
            {
            Thread.sleep(10L);
            }
        assertTrue("returned password array retained clear text", isCleared(password));
        }

    private static boolean isCleared(char[] password)
        {
        for (char ch : password)
            {
            if (ch != '0')
                {
                return false;
                }
            }
        return true;
        }

    private static void assertProviderFailure(Credential identity, Path subject, Path senior, String expected)
        {
        assertProviderFailure(dependencies(identity, subject, senior).setSubjectProofRequired(true), expected);
        }

    private static void assertProviderFailure(PeerProofDependencies dependencies, String expected)
        {
        try
            {
            new X509PeerProofProvider(dependencies);
            fail("expected provider initialization failure containing " + expected);
            }
        catch (IllegalArgumentException e)
            {
            assertTrue(e.getMessage(), e.getMessage().contains(expected));
            }
        }

    private static void assertSubjectTime(X509PeerProofProvider provider, long issued, long expires, long now,
            SubjectProofVerification.Status expected)
        {
        SubjectProofPayload payload = subject(provider, issued, expires);
        assertEquals(expected, provider.verifyProof(encoded(payload), payload, now).getStatus());
        assertEquals(expected, SubjectProofVerifier.verify(provider, encoded(payload), payload, now).getStatus());
        }

    private static void assertSeniorTime(X509PeerProofProvider provider, long issued, long expires, long now,
            SeniorMetadataProofVerification.Status expected)
        {
        SeniorMetadataProofPayload payload = senior(provider, issued, expires);
        byte[] proof = new SeniorMetadataProof(payload, new byte[] {1}).toByteArray();
        assertEquals(expected, provider.verifyProof(proof, payload, now).getStatus());
        }

    private static void assertValidationFailure(PeerProofDependencies deps, String expected)
        {
        try
            {
            deps.validate();
            fail("expected validation failure containing " + expected);
            }
        catch (IllegalArgumentException e)
            {
            assertTrue(e.getMessage(), e.getMessage().contains(expected));
            }
        }

    private static byte[] encoded(SubjectProofPayload payload)
        {
        return new SubjectProof(payload, new byte[] {1}).toByteArray();
        }

    private static SubjectProofPayload subject(X509PeerProofProvider provider, long issued, long expires)
        {
        return payload(provider.getAlgorithmId(), provider.getKeyId(), provider.getIssuerId(), issued, expires);
        }

    private static SubjectProofPayload payload(String algorithm, String key, String issuer,
            long issued, long expires)
        {
        return new SubjectProofPayload(SubjectProofPayload.PROOF_VERSION, algorithm, key, issuer,
                "member", "cluster", "type", "service", "cache", 1L, 1L, 2L, issued, expires,
                Collections.singleton("principal"));
        }

    private static SeniorMetadataProofPayload senior(X509PeerProofProvider provider, long issued, long expires)
        {
        return new SeniorMetadataProofPayload(SeniorMetadataProofPayload.PAYLOAD_VERSION,
                SeniorMetadataProofPayload.MESSAGE_KIND_HEARTBEAT, provider.getAlgorithmId(), provider.getKeyId(),
                provider.getIssuerId(), "cluster", "service", 1, 2, provider.getIssuerId(), provider.getIssuerId(),
                "", "", SeniorMetadataProofPayload.KILL_DIRECTION_NONE, false, 1L, 1L, 1, 1,
                new byte[] {1}, 1L, 2L, issued, expires);
        }

    private static Credential credential(String name, String... extensions) throws Exception
        {
        return credentialForMember(name, name, extensions);
        }

    private static Credential credentialForMember(String label, String name, String... extensions) throws Exception
        {
        Path store = s_dir.resolve(label + ".p12");
        Path cert  = s_dir.resolve(label + ".pem");
        List<String> args = new ArrayList<>(Arrays.asList("-genkeypair", "-alias", ALIAS,
                "-keyalg", "RSA", "-keysize", "2048", "-validity", "365", "-dname", "CN=" + name,
                "-ext", "SAN=uri:" + X509PeerProofProvider.issuerId(name)));
        for (String extension : extensions)
            {
            args.add("-ext");
            args.add(extension);
            }
        Collections.addAll(args, "-keystore", store.toString(), "-storetype", "PKCS12",
                "-storepass", PASSWORD, "-keypass", PASSWORD, "-noprompt");
        keytool(args.toArray(new String[0]));
        keytool("-exportcert", "-rfc", "-alias", ALIAS, "-keystore", store.toString(),
                "-storetype", "PKCS12", "-storepass", PASSWORD, "-file", cert.toString());
        return new Credential(name, store, cert);
        }

    private static Path boundedStore(String name, String type, List<Credential> credentials,
            boolean identity) throws Exception
        {
        java.security.KeyStore source = java.security.KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(s_memberOne.f_store))
            {
            source.load(in, PASSWORD.toCharArray());
            }
        java.security.KeyStore target = java.security.KeyStore.getInstance(type);
        target.load(null, PASSWORD.toCharArray());
        int start = 0;
        if (identity)
            {
            target.setKeyEntry(ALIAS, source.getKey(ALIAS, PASSWORD.toCharArray()), PASSWORD.toCharArray(),
                    source.getCertificateChain(ALIAS));
            start = 1;
            }
        java.security.cert.CertificateFactory factory =
                java.security.cert.CertificateFactory.getInstance("X.509");
        for (int i = start; i < credentials.size(); i++)
            {
            try (InputStream in = Files.newInputStream(credentials.get(i).f_cert))
                {
                target.setCertificateEntry("authority-" + i, factory.generateCertificate(in));
                }
            }
        Path path = s_dir.resolve(name);
        try (java.io.OutputStream out = Files.newOutputStream(path))
            {
            target.store(out, PASSWORD.toCharArray());
            }
        return path;
        }

    private static java.security.cert.Certificate[] certificateChain(Credential... credentials)
            throws Exception
        {
        java.security.cert.CertificateFactory factory =
                java.security.cert.CertificateFactory.getInstance("X.509");
        java.security.cert.Certificate[] chain = new java.security.cert.Certificate[credentials.length];
        for (int i = 0; i < credentials.length; i++)
            {
            try (InputStream in = Files.newInputStream(credentials[i].f_cert))
                {
                chain[i] = factory.generateCertificate(in);
                }
            }
        return chain;
        }

    private static Path identityStore(String name, String type,
            java.security.cert.Certificate[] chain, char[] password) throws Exception
        {
        java.security.KeyStore source = java.security.KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(s_memberOne.f_store))
            {
            source.load(in, PASSWORD.toCharArray());
            }
        java.security.KeyStore target = java.security.KeyStore.getInstance(type);
        target.load(null, password);
        target.setKeyEntry(ALIAS, source.getKey(ALIAS, PASSWORD.toCharArray()), password, chain);
        Path path = s_dir.resolve(name);
        try (java.io.OutputStream out = Files.newOutputStream(path))
            {
            target.store(out, password);
            }
        return path;
        }

    private static Path writeCredential(String name, byte[] bytes) throws IOException
        {
        Path path = s_dir.resolve(name);
        Files.write(path, bytes);
        return path;
        }

    private static void assertUnavailableIdentity(Path path, String type)
        {
        X509PeerProofProvider provider = new X509PeerProofProvider(
                dependencies(s_memberOne, s_memberOne.f_cert, s_memberOne.f_cert)
                        .setIdentityStoreUrl(path.toString()).setIdentityStoreType(type));
        s_providers.add(provider);
        assertFalse(provider.isReady());
        assertTrue(provider.getReadinessStatus().startsWith("reload-unavailable:"));
        }

    private static byte[] derNull()
        {return new byte[] {0x05, 0x00};}

    private static byte[] derInteger(int value) throws IOException
        {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream out = new java.io.DataOutputStream(bytes);
        out.writeInt(value);
        byte[] encoded = bytes.toByteArray();
        int n = 0;
        while (n < encoded.length - 1 && encoded[n] == 0 && (encoded[n + 1] & 0x80) == 0)
            {
            n++;
            }
        return der(0x02, Arrays.copyOfRange(encoded, n, encoded.length));
        }

    private static byte[] derSequence(byte[]... values) throws IOException
        {return der(0x30, concatenate(values));}

    private static byte[] der(int nTag, byte[] value) throws IOException
        {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream out = new java.io.DataOutputStream(bytes);
        out.writeByte(nTag);
        if (value.length < 128)
            {
            out.writeByte(value.length);
            }
        else if (value.length <= 0xff)
            {
            out.writeByte(0x81);
            out.writeByte(value.length);
            }
        else if (value.length <= 0xffff)
            {
            out.writeByte(0x82);
            out.writeShort(value.length);
            }
        else
            {
            out.writeByte(0x84);
            out.writeInt(value.length);
            }
        out.write(value);
        return bytes.toByteArray();
        }

    private static byte[] jksWithOversizedAlias() throws IOException
        {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream out = new java.io.DataOutputStream(bytes);
        out.writeInt(0xfeedfeed);
        out.writeInt(2);
        out.writeInt(1);
        out.writeInt(2);
        out.writeShort(X509PeerProofProvider.MAX_ALIAS_UTF_BYTES + 1);
        out.write(new byte[X509PeerProofProvider.MAX_ALIAS_UTF_BYTES + 1]);
        return bytes.toByteArray();
        }

    private static byte[] jksWithOversizedEncryptedKey() throws IOException
        {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream out = new java.io.DataOutputStream(bytes);
        out.writeInt(0xfeedfeed);
        out.writeInt(2);
        out.writeInt(1);
        out.writeInt(1);
        out.writeShort(0);
        out.writeLong(0L);
        out.writeInt(X509PeerProofProvider.MAX_ENCRYPTED_KEY_BYTES + 1);
        out.write(new byte[X509PeerProofProvider.MAX_ENCRYPTED_KEY_BYTES + 1]);
        return bytes.toByteArray();
        }

    private static byte[] concatenate(byte[]... values) throws IOException
        {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (byte[] value : values)
            {
            out.write(value);
            }
        return out.toByteArray();
        }

    private static long refreshThreadCount()
        {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.isAlive() && ("PeerProofRefresh".equals(thread.getName())
                        || "PeerProofCredentialIO".equals(thread.getName()))).count();
        }

    private static void assertBackendQuiescent(LifecycleCredentialReader reader) throws Exception
        {
        int calls = reader.getBackendCallCount();
        Thread.sleep(100L);
        assertEquals("credential backend continued after lifecycle completion", calls, reader.getBackendCallCount());
        }

    private static void assertClosedAndFailClosed(X509PeerProofProvider provider)
        {
        assertEquals("closed", provider.getReadinessStatus());
        assertEquals("", provider.getKeyId());
        assertNull(provider.createProof(subject(provider, 1L, 2L)));
        assertFalse(provider.getLocalReadinessObservation().isReady());
        }

    private static ScheduledExecutorService providerRefreshExecutor(X509PeerProofProvider provider)
            throws Exception
        {
        java.lang.reflect.Field field = X509PeerProofProvider.class.getDeclaredField("f_executor");
        field.setAccessible(true);
        return (ScheduledExecutorService) field.get(provider);
        }

    private static ScheduledExecutorService providerIoExecutor(X509PeerProofProvider provider)
            throws Exception
        {
        java.lang.reflect.Field field = X509PeerProofProvider.class.getDeclaredField("f_ioExecutor");
        field.setAccessible(true);
        return (ScheduledExecutorService) field.get(provider);
        }

    private static ExecutorService providerCleanupExecutor(X509PeerProofProvider provider)
            throws Exception
        {
        java.lang.reflect.Field field = X509PeerProofProvider.class.getDeclaredField("f_cleanupExecutor");
        field.setAccessible(true);
        return (ExecutorService) field.get(provider);
        }

    private static void assertNoQueuedCredentialWork(X509PeerProofProvider provider) throws Exception
        {
        assertTrue(((ScheduledThreadPoolExecutor) providerIoExecutor(provider)).getQueue().isEmpty());
        assertTrue(((ThreadPoolExecutor) providerCleanupExecutor(provider)).getQueue().isEmpty());
        }

    private static void keytool(String... args) throws Exception
        {
        String[] command = new String[args.length + 1];
        command[0] = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
        System.arraycopy(args, 0, command, 1, args.length);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        byte[] output = process.getInputStream().readAllBytes();
        if (process.waitFor() != 0)
            {
            throw new AssertionError("keytool failed: " + new String(output));
            }
        }

    private static Credential expiredCredential(String name) throws Exception
        {
        Path store = s_dir.resolve(name + ".p12");
        Path cert = s_dir.resolve(name + ".pem");
        keytool("-genkeypair", "-alias", ALIAS, "-keyalg", "RSA", "-keysize", "2048",
                "-startdate", "2020/01/01 00:00:00", "-validity", "1", "-dname", "CN=" + name,
                "-ext", "SAN=uri:" + X509PeerProofProvider.issuerId(name), "-ext", "KU=digitalSignature",
                "-keystore", store.toString(), "-storetype", "PKCS12", "-storepass", PASSWORD,
                "-keypass", PASSWORD, "-noprompt");
        keytool("-exportcert", "-rfc", "-alias", ALIAS, "-keystore", store.toString(),
                "-storetype", "PKCS12", "-storepass", PASSWORD, "-file", cert.toString());
        return new Credential(name, store, cert);
        }

    private static class Credential
        {
        Credential(String name, Path store, Path cert)
            {
            f_name = name;
            f_store = store;
            f_cert = cert;
            }
        final String f_name;
        final Path f_store;
        final Path f_cert;
        }

    private static class CountingKeyStoreLoader
            implements X509PeerProofProvider.KeyStoreLoader
        {
        @Override
        public java.security.KeyStore load(String type, byte[] bytes, char[] password) throws Exception
            {
            f_cLoads.incrementAndGet();
            java.security.KeyStore store = java.security.KeyStore.getInstance(type, "SUN");
            store.load(new java.io.ByteArrayInputStream(bytes), password);
            return store;
            }

        int getLoadCount() {return f_cLoads.get();}
        void reset() {f_cLoads.set(0);}

        private final AtomicInteger f_cLoads = new AtomicInteger();
        }

    /** Counts preflight sink entry while substituting a valid platform store. */
    private static class SubstitutingKeyStoreLoader
            implements X509PeerProofProvider.KeyStoreLoader
        {
        SubstitutingKeyStoreLoader(byte[] validStore) throws Exception
            {
            f_store = java.security.KeyStore.getInstance("PKCS12", "SUN");
            f_store.load(new java.io.ByteArrayInputStream(validStore), PASSWORD.toCharArray());
            }

        @Override
        public java.security.KeyStore load(String type, byte[] bytes, char[] password)
            {
            f_cLoads.incrementAndGet();
            return f_store;
            }

        int getLoadCount() {return f_cLoads.get();}
        void reset() {f_cLoads.set(0);}

        private final java.security.KeyStore f_store;
        private final AtomicInteger f_cLoads = new AtomicInteger();
        }

    /** Aligns complete refreshes at EOF and backend close in deterministic waves. */
    private static class ConcurrentEofCredentialReader
            implements X509PeerProofProvider.CredentialReader
        {
        ConcurrentEofCredentialReader(String target, int... waveSizes)
            {
            f_sTarget = target;
            f_anWaveSizes = waveSizes.clone();
            f_aEofEntered = latches(waveSizes);
            f_aEofRelease = latches(fill(waveSizes.length, 1));
            f_aCloseEntered = latches(waveSizes);
            f_aCloseRelease = latches(fill(waveSizes.length, 1));
            }

        void arm() {m_fArmed.set(true);}
        boolean awaitEof(int wave) throws InterruptedException
            {return f_aEofEntered[wave].await(5L, TimeUnit.SECONDS);}
        boolean awaitClose(int wave) throws InterruptedException
            {return f_aCloseEntered[wave].await(5L, TimeUnit.SECONDS);}
        void releaseEof(int wave) {f_aEofRelease[wave].countDown();}
        void releaseClose(int wave) {f_aCloseRelease[wave].countDown();}
        int getClosedCount() {return m_cClosed.get();}

        void releaseAll()
            {
            for (CountDownLatch latch : f_aEofRelease) {latch.countDown();}
            for (CountDownLatch latch : f_aCloseRelease) {latch.countDown();}
            }

        @Override
        public InputStream open(String source, X509PeerProofProvider.CredentialControl control)
                throws IOException
            {
            control.checkOpen();
            InputStream delegate = Files.newInputStream(Path.of(source));
            if (!m_fArmed.get() || !f_sTarget.equals(source))
                {
                return delegate;
                }
            int wave = wave(m_cStreams.getAndIncrement());
            if (wave < 0)
                {
                return delegate;
                }
            return new InputStream()
                {
                @Override
                public int read() throws IOException
                    {return atEof(delegate.read());}

                @Override
                public int read(byte[] bytes, int offset, int length) throws IOException
                    {return atEof(delegate.read(bytes, offset, length));}

                private int atEof(int result) throws IOException
                    {
                    if (result < 0 && f_fEof.compareAndSet(false, true))
                        {
                        f_aEofEntered[wave].countDown();
                        await(f_aEofRelease[wave], "EOF");
                        }
                    return result;
                    }

                @Override
                public void close() throws IOException
                    {
                    if (f_fClose.compareAndSet(false, true))
                        {
                        f_aCloseEntered[wave].countDown();
                        await(f_aCloseRelease[wave], "close");
                        delegate.close();
                        m_cClosed.incrementAndGet();
                        }
                    }

                private final AtomicBoolean f_fEof = new AtomicBoolean();
                private final AtomicBoolean f_fClose = new AtomicBoolean();
                };
            }

        private int wave(int index)
            {
            int start = 0;
            for (int i = 0; i < f_anWaveSizes.length; i++)
                {
                start += f_anWaveSizes[i];
                if (index < start) {return i;}
                }
            return -1;
            }

        private static void await(CountDownLatch latch, String boundary) throws IOException
            {
            try
                {
                if (!latch.await(10L, TimeUnit.SECONDS))
                    {
                    throw new IOException("timed out awaiting credential " + boundary + " release");
                    }
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw new IOException("credential " + boundary + " interrupted", e);
                }
            }

        private static CountDownLatch[] latches(int[] counts)
            {
            CountDownLatch[] latches = new CountDownLatch[counts.length];
            for (int i = 0; i < counts.length; i++) {latches[i] = new CountDownLatch(counts[i]);}
            return latches;
            }

        private static int[] fill(int length, int value)
            {
            int[] values = new int[length];
            Arrays.fill(values, value);
            return values;
            }

        private final String f_sTarget;
        private final int[] f_anWaveSizes;
        private final CountDownLatch[] f_aEofEntered;
        private final CountDownLatch[] f_aEofRelease;
        private final CountDownLatch[] f_aCloseEntered;
        private final CountDownLatch[] f_aCloseRelease;
        private final AtomicBoolean m_fArmed = new AtomicBoolean();
        private final AtomicInteger m_cStreams = new AtomicInteger();
        private final AtomicInteger m_cClosed = new AtomicInteger();
        }

    private enum PasswordPosition {IDENTITY, SUBJECT, SENIOR}

    private enum ParameterShape {OMITTED, ABSENT, NULL, MALFORMED, NON_EMPTY_NULL}

    private enum PfxIdentifier {NONE, CONTENT, BAG, CERTIFICATE, ENCRYPTION, AES, KDF, PRF, MAC}

    private enum PfxStructure
        {
        KEY_PAYLOAD_WRONG_TAG,
        KEY_PAYLOAD_CONSTRUCTED,
        KEY_PAYLOAD_EMPTY,
        KEY_PAYLOAD_OMITTED,
        KEY_PAYLOAD_EXTRA,
        BAG_MISSING_VALUE,
        BAG_EXTRA_CHILD,
        SAFE_CONTENTS_EMPTY,
        ATTRIBUTE_WRONG_CONTAINER,
        ATTRIBUTE_MALFORMED_SEQUENCE,
        ATTRIBUTE_NON_OID,
        ATTRIBUTE_MALFORMED_OID,
        ATTRIBUTE_WRONG_VALUE_SET,
        ATTRIBUTE_EMPTY_VALUE_SET,
        ATTRIBUTE_MULTI_VALUE_SET,
        ATTRIBUTE_EMPTY_SET,
        ATTRIBUTE_UNKNOWN,
        ATTRIBUTE_DUPLICATE_OID,
        ATTRIBUTE_FRIENDLY_NAME_WRONG_TAG,
        ATTRIBUTE_FRIENDLY_NAME_EMPTY,
        ATTRIBUTE_FRIENDLY_NAME_ODD_WIDTH,
        ATTRIBUTE_FRIENDLY_NAME_EXCESSIVE,
        ATTRIBUTE_LOCAL_KEY_ID_WRONG_TAG,
        ATTRIBUTE_LOCAL_KEY_ID_EMPTY,
        ATTRIBUTE_LOCAL_KEY_ID_EXCESSIVE,
        ATTRIBUTE_TRUSTED_KEY_USAGE_WRONG_TAG,
        ATTRIBUTE_TRUSTED_KEY_USAGE_WRONG_OID,
        ATTRIBUTE_EXCESSIVE,
        ENCRYPTED_CONTENT_WRONG_TAG,
        ENCRYPTED_CONTENT_CONSTRUCTED,
        ENCRYPTED_CONTENT_EMPTY,
        ENCRYPTED_CONTENT_OMITTED,
        ENCRYPTED_CONTENT_EXTRA,
        ENCRYPTED_CONTENT_MALFORMED_FRAGMENT,
        ENCRYPTED_CONTENT_EXCESSIVE_FRAGMENTS,
        ENCRYPTED_DATA_EXTRA
        }

    private enum Pbkdf2Mutation
        {
        OMIT_KEY_LENGTH
            {
            @Override
            void apply(PfxDer parameters)
                {
                if (parameters.child(2).f_nTag != 0x02)
                    {
                    throw new AssertionError("expected explicit PBKDF2 key length");
                    }
                parameters.f_listChildren.remove(2);
                }
            },
        OMIT_PRF
            {
            @Override
            void apply(PfxDer parameters)
                {
                int index = parameters.child(2).f_nTag == 0x02 ? 3 : 2;
                parameters.f_listChildren.remove(index);
                }
            },
        OMIT_PRF_PARAMETERS
            {
            @Override
            void apply(PfxDer parameters)
                {
                int index = parameters.child(2).f_nTag == 0x02 ? 3 : 2;
                PfxDer prf = parameters.child(index);
                if (prf.f_listChildren.size() != 2 || prf.child(1).f_nTag != 0x05)
                    {
                    throw new AssertionError("expected explicit NULL PRF parameters");
                    }
                prf.f_listChildren.remove(1);
                }
            };

        abstract void apply(PfxDer parameters);
        }

    /** Minimal mutable DER tree used only to create integrity-correct platform fixtures. */
    private static class PfxDer
        {
        static PfxDer parse(byte[] bytes) throws IOException
            {
            int[] offset = {0};
            PfxDer value = parse(bytes, offset, bytes.length);
            if (offset[0] != bytes.length)
                {
                throw new IOException("trailing DER data");
                }
            return value;
            }

        private static PfxDer parse(byte[] bytes, int[] offset, int limit) throws IOException
            {
            if (offset[0] >= limit)
                {
                throw new IOException("truncated DER value");
                }
            int tag = bytes[offset[0]++] & 0xff;
            int length = readLength(bytes, offset, limit);
            int end = offset[0] + length;
            if (end < offset[0] || end > limit)
                {
                throw new IOException("invalid DER length");
                }
            if ((tag & 0x20) == 0)
                {
                byte[] value = Arrays.copyOfRange(bytes, offset[0], end);
                offset[0] = end;
                return new PfxDer(tag, value, new ArrayList<>());
                }
            List<PfxDer> children = new ArrayList<>();
            while (offset[0] < end)
                {
                children.add(parse(bytes, offset, end));
                }
            return new PfxDer(tag, null, children);
            }

        private static int readLength(byte[] bytes, int[] offset, int limit) throws IOException
            {
            if (offset[0] >= limit)
                {
                throw new IOException("missing DER length");
                }
            int value = bytes[offset[0]++] & 0xff;
            if ((value & 0x80) == 0)
                {
                return value;
                }
            int count = value & 0x7f;
            if (count == 0 || count > 4 || offset[0] + count > limit)
                {
                throw new IOException("invalid DER long length");
                }
            int length = 0;
            for (int i = 0; i < count; i++)
                {
                length = length << 8 | bytes[offset[0]++] & 0xff;
                }
            return length;
            }

        PfxDer child(int... path)
            {
            PfxDer value = this;
            for (int index : path)
                {
                value = value.f_listChildren.get(index);
                }
            return value;
            }

        boolean isOidChild(int index, String expected) throws IOException
            {return child(index).f_nTag == 0x06 && expected.equals(child(index).oid());}

        String oid() throws IOException
            {
            if (f_nTag != 0x06 || f_abValue == null || f_abValue.length == 0)
                {
                throw new IOException("invalid OID");
                }
            int first = f_abValue[0] & 0xff;
            StringBuilder value = new StringBuilder()
                    .append(Math.min(2, first / 40)).append('.')
                    .append(first < 80 ? first % 40 : first - 80);
            long arc = 0;
            for (int i = 1; i < f_abValue.length; i++)
                {
                int b = f_abValue[i] & 0xff;
                arc = arc << 7 | b & 0x7f;
                if ((b & 0x80) == 0)
                    {
                    value.append('.').append(arc);
                    arc = 0;
                    }
                }
            return value.toString();
            }

        byte[] encode() throws IOException
            {
            if ((f_nTag & 0x20) == 0)
                {
                return der(f_nTag, f_abValue);
                }
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            for (PfxDer child : f_listChildren)
                {
                bytes.write(child.encode());
                }
            return der(f_nTag, bytes.toByteArray());
            }

        PfxDer(int tag, byte[] value, List<PfxDer> children)
            {
            f_nTag = tag;
            f_abValue = value;
            f_listChildren = children;
            }

        private final int f_nTag;
        private byte[] f_abValue;
        private final List<PfxDer> f_listChildren;
        }

    private static class PfxParameterCase
        {
        PfxParameterCase(String name, byte[] pfx)
            {
            f_sName = name;
            f_abPfx = pfx;
            }

        private final String f_sName;
        private final byte[] f_abPfx;
        }

    private static class PfxPlatformFamily
        {
        PfxPlatformFamily(Path path, String aesOid, Integer keyLength, String prfOid,
                ParameterShape prfShape, String macOid, ParameterShape macShape)
            {
            f_path = path;
            f_sAesOid = aesOid;
            f_nKeyLength = keyLength;
            f_sPrfOid = prfOid;
            f_prfShape = prfShape;
            f_sMacOid = macOid;
            f_macShape = macShape;
            }

        private final Path f_path;
        private final String f_sAesOid;
        private final Integer f_nKeyLength;
        private final String f_sPrfOid;
        private final ParameterShape f_prfShape;
        private final String f_sMacOid;
        private final ParameterShape f_macShape;
        }

    private static class PfxStructureCase
        {
        PfxStructureCase(String name, byte[] pfx)
            {
            f_sName = name;
            f_abPfx = pfx;
            }

        private final String f_sName;
        private final byte[] f_abPfx;
        }

    private static class CapturingPasswordProvider
            implements PasswordProvider
        {
        CapturingPasswordProvider(char[] password)
            {f_password = password.clone();}

        @Override
        public char[] get()
            {
            char[] returned = f_password.clone();
            f_passwords.add(returned);
            return returned;
            }

        List<char[]> passwords() {return f_passwords;}

        private final char[] f_password;
        private final List<char[]> f_passwords = new ArrayList<>();
        }

    /** Captures the exact array returned after a deadline or lifecycle cancellation. */
    private static class LatePasswordProvider
            implements PasswordProvider
        {
        static LatePasswordProvider afterDeadline(String value, long cDelayMillis)
            {return new LatePasswordProvider(value.toCharArray(), cDelayMillis, false);}

        static LatePasswordProvider afterRelease(String value)
            {return new LatePasswordProvider(value.toCharArray(), 0L, true);}

        private LatePasswordProvider(char[] password, long cDelayMillis, boolean fWaitForRelease)
            {
            f_password = password;
            f_cDelayMillis = cDelayMillis;
            f_fWaitForRelease = fWaitForRelease;
            }

        @Override
        public char[] get()
            {
            f_entered.countDown();
            if (f_fWaitForRelease)
                {
                while (f_release.getCount() != 0L)
                    {
                    try {f_release.await(10L, TimeUnit.MILLISECONDS);} catch (InterruptedException ignored) {}
                    }
                }
            else
                {
                long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(f_cDelayMillis);
                while (System.nanoTime() < deadline)
                    {
                    try {Thread.sleep(10L);} catch (InterruptedException ignored) {}
                    }
                }
            f_returned.countDown();
            return f_password;
            }

        boolean awaitEntered() throws InterruptedException
            {return f_entered.await(5L, TimeUnit.SECONDS);}

        boolean awaitReturned() throws InterruptedException
            {return f_returned.await(5L, TimeUnit.SECONDS);}

        void release() {f_release.countDown();}

        char[] password() {return f_password;}

        private final char[] f_password;
        private final long f_cDelayMillis;
        private final boolean f_fWaitForRelease;
        private final CountDownLatch f_entered = new CountDownLatch(1);
        private final CountDownLatch f_release = new CountDownLatch(1);
        private final CountDownLatch f_returned = new CountDownLatch(1);
        }

    private enum LifecycleMode {OPEN, TRICKLE, CLOSE}

    /** Interrupt-resistant backend covering open, total-read, and final-close boundaries. */
    private static class LifecycleCredentialReader
            implements X509PeerProofProvider.CredentialReader
        {
        LifecycleCredentialReader(LifecycleMode mode, int nTargetOpen)
            {
            f_mode = mode;
            f_nTargetOpen = nTargetOpen;
            }

        void arm()
            {
            m_cRefreshOpens.set(0);
            m_fCancelled.set(false);
            m_fArmed.set(true);
            }

        boolean awaitStalled() throws InterruptedException
            {return f_stalled.await(10, TimeUnit.SECONDS);}

        boolean wasCancelled() {return m_fCancelled.get();}
        int getBackendCallCount() {return m_cBackendCalls.get();}

        @Override
        public InputStream open(String sUrl, X509PeerProofProvider.CredentialControl control) throws IOException
            {
            control.onCancel(() -> m_fCancelled.set(true));
            control.checkOpen();
            m_cBackendCalls.incrementAndGet();
            int nOpen = m_cRefreshOpens.incrementAndGet();
            if (!m_fArmed.get() || nOpen != f_nTargetOpen)
                {
                return Files.newInputStream(Path.of(sUrl));
                }
            if (f_mode == LifecycleMode.OPEN)
                {
                f_stalled.countDown();
                awaitCancellation();
                throw new IOException("cancelled blocked credential open");
                }

            InputStream delegate = Files.newInputStream(Path.of(sUrl));
            return new InputStream()
                {
                @Override
                public int read() throws IOException
                    {
                    byte[] one = new byte[1];
                    int count = read(one, 0, 1);
                    return count < 0 ? -1 : one[0] & 0xff;
                    }

                @Override
                public int read(byte[] bytes, int offset, int length) throws IOException
                    {
                    m_cBackendCalls.incrementAndGet();
                    if (f_mode != LifecycleMode.TRICKLE)
                        {
                        return delegate.read(bytes, offset, length);
                        }
                    f_stalled.countDown();
                    if (m_fCancelled.get())
                        {
                        throw new IOException("cancelled credential trickle");
                        }
                    try {Thread.sleep(10L);} catch (InterruptedException ignored) {}
                    bytes[offset] = 0;
                    return 1;
                    }

                @Override
                public void close() throws IOException
                    {
                    m_cBackendCalls.incrementAndGet();
                    if (f_mode == LifecycleMode.CLOSE)
                        {
                        f_stalled.countDown();
                        awaitCancellation();
                        }
                    delegate.close();
                    }
                };
            }

        private void awaitCancellation()
            {
            while (!m_fCancelled.get())
                {
                try {Thread.sleep(10L);} catch (InterruptedException ignored) {}
                }
            }

        private final LifecycleMode f_mode;
        private final int f_nTargetOpen;
        private final CountDownLatch f_stalled = new CountDownLatch(1);
        private final AtomicBoolean m_fArmed = new AtomicBoolean();
        private final AtomicBoolean m_fCancelled = new AtomicBoolean();
        private final AtomicInteger m_cBackendCalls = new AtomicInteger();
        private final AtomicInteger m_cRefreshOpens = new AtomicInteger();
        }

    private static final String PASSWORD = "changeit";
    private static final String ALIAS = "peer-signing";
    private static Path s_dir;
    private static Credential s_memberOne;
    private static Credential s_memberTwo;
    private static Credential s_memberOneNew;
    private static Credential s_memberOneNewTwo;
    private static Credential s_memberThree;
    private static Credential s_memberFour;
    private static Credential s_memberFourNew;
    private static Credential s_ca;
    private static Credential s_noUsage;
    private static Credential s_wrongUsage;
    private static Credential s_expired;
    private static final List<Credential> s_complexityCredentials = new ArrayList<>();
    private static final List<X509PeerProofProvider> s_providers = new ArrayList<>();
    }
