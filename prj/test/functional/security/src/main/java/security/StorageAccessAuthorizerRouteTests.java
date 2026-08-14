/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package security;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.options.Timeout;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.net.NamedCache;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.InvocationService;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet;
import com.tangosol.coherence.component.net.MemberSet;
import com.tangosol.coherence.component.net.Security;
import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService;
import com.tangosol.internal.net.security.PeerProofProvider;
import com.tangosol.internal.net.security.PeerProofReadiness;
import com.tangosol.internal.net.security.SeniorMetadataProof;
import com.tangosol.internal.net.security.SeniorMetadataProofPayload;
import com.tangosol.internal.net.security.SeniorMetadataProofVerification;
import com.tangosol.internal.net.security.SubjectProof;
import com.tangosol.internal.net.security.SubjectProofPayload;
import com.tangosol.internal.net.security.SubjectProofVerification;
import com.tangosol.internal.net.security.X509PeerProofProvider;
import com.tangosol.net.security.StorageAccessAuthorizer;
import com.tangosol.coherence.component.util.SafeCluster;
import com.tangosol.util.ValueUpdater;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.processor.UpdaterProcessor;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;

import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javax.security.auth.Subject;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * StorageAccessAuthorizer routed operation coverage.
 *
 * @author Aleks Seovic  2026.07.16
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class StorageAccessAuthorizerRouteTests
        extends AbstractFunctionalTest
    {
    public StorageAccessAuthorizerRouteTests()
        {
        super(FILE_CFG_CACHE);
        }

    @BeforeClass
    public static void _startup()
        {
        try
            {
            createPeerProofCredentials();
            }
        catch (Exception e)
            {
            throw new AssertionError("unable to create PEER proof functional credentials", e);
            }
        if (System.getProperty("coherence.cluster") == null)
            {
            System.setProperty("coherence.cluster", "StorageAccessAuthorizerRouteTests-" + System.currentTimeMillis());
            }
        System.setProperty("coherence.override", FILE_CFG_OVERRIDE);
        System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE);
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.member", "peer-proof-client");
        System.setProperty("coherence.role", "peer-subject-client");
        System.setProperty("coherence.distributed.localstorage", "false");
        System.setProperty("coherence.security.mode", "hardened");
        System.setProperty("peer.proof.identity.store", s_clientStore.toString());
        System.setProperty("peer.proof.authorities", s_authorities.toString());
        System.setProperty("peer.proof.password.file", s_passwordFile.toString());
        System.setProperty("coherence.security.peer.senior-metadata-proof.required", "true");
        System.setProperty("coherence.management.remote.registryport", String.valueOf(getAvailablePorts().next()));

        AbstractFunctionalTest._startup();

        Properties props = new Properties();
        props.setProperty("coherence.override", FILE_CFG_OVERRIDE);
        props.setProperty("coherence.localhost", "127.0.0.1");
        props.setProperty("coherence.wka", "127.0.0.1");
        props.setProperty("coherence.member", "peer-proof-server");
        props.setProperty("coherence.role", "peer-storage-one");
        props.setProperty("coherence.distributed.localstorage", "true");
        props.setProperty("coherence.security.mode", "hardened");
        props.setProperty("peer.proof.identity.store", s_serverStore.toString());
        props.setProperty("peer.proof.authorities", s_memberAuthorities.toString());
        props.setProperty("peer.proof.password.file", s_passwordFile.toString());
        props.setProperty("coherence.security.peer.senior-metadata-proof.required", "true");
        props.setProperty("coherence.management.remote.registryport", String.valueOf(getAvailablePorts().next()));

        s_member = startCacheServer("StorageAccessAuthorizerRouteTests", "security", FILE_CFG_CACHE, props);

        Properties propsTwo = new Properties();
        propsTwo.putAll(props);
        propsTwo.setProperty("coherence.member", "peer-proof-server-two");
        propsTwo.setProperty("coherence.role", "peer-storage-two");
        propsTwo.setProperty("peer.proof.identity.store", s_serverTwoStore.toString());
        propsTwo.setProperty("peer.proof.authorities", s_memberTwoAuthorities.toString());
        propsTwo.setProperty("coherence.management.remote.registryport", String.valueOf(getAvailablePorts().next()));
        s_memberTwo = startCacheServer("StorageAccessAuthorizerRouteTests-2", "security", FILE_CFG_CACHE, propsTwo);

        Eventually.assertThat(invoking(s_member).isServiceRunning("StorageAuthorizerRouteService"), is(true));
        Eventually.assertThat(invoking(s_member).isServiceRunning("StorageAuthorizerDefaultService"), is(true));
        Eventually.assertThat(invoking(s_member).isServiceRunning("StorageAuthorizerPerformanceService"), is(true));
        Eventually.assertThat(invoking(s_memberTwo).isServiceRunning("StorageAuthorizerPerformanceService"), is(true));
        CacheFactory.getCache("authorized-peer-proof-readiness").getCacheService();
        }

    @AfterClass
    public static void stopServer()
        {
        stopCacheServer("StorageAccessAuthorizerRouteTests");
        stopCacheServer("StorageAccessAuthorizerRouteTests-2");
        try
            {
            if (s_credentialDir != null)
                {
                Files.deleteIfExists(s_authorities);
                Files.deleteIfExists(s_memberAuthorities);
                Files.deleteIfExists(s_memberTwoAuthorities);
                Files.deleteIfExists(s_clientCert);
                Files.deleteIfExists(s_serverCert);
                Files.deleteIfExists(s_serverTwoCert);
                Files.deleteIfExists(s_clientStore);
                Files.deleteIfExists(s_serverStore);
                Files.deleteIfExists(s_serverTwoStore);
                Files.deleteIfExists(s_serverNewStore);
                Files.deleteIfExists(s_serverNewCert);
                Files.deleteIfExists(s_serverTwoNewStore);
                Files.deleteIfExists(s_serverTwoNewCert);
                Files.deleteIfExists(s_passwordFile);
                Files.deleteIfExists(s_clientOldStore);
                Files.deleteIfExists(s_clientNewStore);
                Files.deleteIfExists(s_clientNewCert);
                Files.deleteIfExists(s_untrustedStore);
                Files.deleteIfExists(s_untrustedCert);
                Files.deleteIfExists(s_wrongSanStore);
                Files.deleteIfExists(s_wrongSanCert);
                Files.deleteIfExists(s_retiredStore);
                Files.deleteIfExists(s_retiredCert);
                Files.deleteIfExists(s_optionalStore);
                Files.deleteIfExists(s_optionalCert);
                Files.deleteIfExists(s_caStore);
                Files.deleteIfExists(s_caCert);
                Files.deleteIfExists(s_noKeyUsageStore);
                Files.deleteIfExists(s_noKeyUsageCert);
                Files.deleteIfExists(s_falseKeyUsageStore);
                Files.deleteIfExists(s_falseKeyUsageCert);
                Files.deleteIfExists(s_expiredStore);
                Files.deleteIfExists(s_expiredCert);
                Files.deleteIfExists(s_credentialDir);
                }
            }
        catch (Exception ignored)
            {
            }
        }

    @Before
    public void resetAuthorizer()
        {
        s_member.invoke(new ResetAuthorizer());
        s_memberTwo.invoke(new ResetAuthorizer());
        restorePeerProofProvider();
        }

    @Test
    public void shouldAllowRoutesWithoutConfiguredAuthorizer()
        {
        NamedCache cache = getNamedCache("default-routes");
        cache.clear();

        cache.put("key", "value");
        cache.addIndex(IdentityExtractor.INSTANCE, false, null);
        cache.removeIndex(IdentityExtractor.INSTANCE);
        cache.invoke("key", new UpdaterProcessor((ValueUpdater) null, "updated"));

        assertTrue(cache.lock("lock-key", 0L));
        assertTrue(cache.unlock("lock-key"));

        assertThat(getEvents().isEmpty(), is(true));
        cache.destroy();
        }

    @Test
    public void shouldCaptureConfiguredAuthorizerIndexRoutes()
        {
        NamedCache cache = getNamedCache("authorized-index");
        cache.clear();

        cache.addIndex(IdentityExtractor.INSTANCE, false, null);
        assertContainsEvent(StorageAccessAuthorizer.REASON_INDEX_ADD, "writeAny", "authorized-index", null);

        resetAuthorizer();
        cache.removeIndex(IdentityExtractor.INSTANCE);
        assertContainsEvent(StorageAccessAuthorizer.REASON_INDEX_REMOVE, "writeAny", "authorized-index", null);

        cache.destroy();
        }

    @Test
    public void shouldCaptureConfiguredAuthorizerSingleKeyInvokeRoute()
        {
        NamedCache cache = getNamedCache("authorized-invoke");
        cache.clear();
        cache.put("key", "value");
        resetAuthorizer();

        cache.invoke("key", new UpdaterProcessor((ValueUpdater) null, "updated"));

        assertContainsEvent(StorageAccessAuthorizer.REASON_INVOKE, "writeAny", "authorized-invoke", null);
        cache.destroy();
        }

    @Test
    public void shouldUseProductionPeerProofBeforeProtectedMutation()
        {
        Subject subject = new Subject();
        subject.getPrincipals().add((Principal) () -> "peer-proof-functional-user");

        Subject.doAs(subject, (PrivilegedAction<Void>) () ->
            {
            NamedCache cache = getNamedCache("authorized-peer-proof");
            cache.clear();
            cache.put("key", "value");
            cache.invoke("key", new UpdaterProcessor((ValueUpdater) null, "proof-verified"));
            assertThat(cache.get("key"), is("proof-verified"));
            cache.destroy();
            return null;
            });

        assertContainsEvent(StorageAccessAuthorizer.REASON_INVOKE, "writeAny",
                "authorized-peer-proof", null);
        }

    @Test
    public void shouldRejectCompleteRealSubjectProofMatrixBeforeMutation()
        {
        for (ProofFault fault : ProofFault.values())
            {
            if (fault == ProofFault.ROTATED_OLD_KEY || fault == ProofFault.DUPLICATE
                    || fault == ProofFault.SENIOR_SIGNATURE || fault == ProofFault.SENIOR_CLAIM
                    || fault == ProofFault.OBSERVE_SENIOR)
                {
                continue;
                }
            String cacheName = "authorized-peer-proof-negative-" + fault.name().toLowerCase();
            NamedCache cache = getNamedCache(cacheName);
            cache.clear();
            cache.put("key", "before");
            resetAuthorizer();
            installPeerProofProvider(new FaultingPeerProofProvider(currentPeerProofProvider(), fault));
            try
                {
                Subject subject = subject("peer-proof-functional-user");
                try
                    {
                    Subject.doAs(subject, (PrivilegedAction<Void>) () ->
                        {
                        cache.invoke("key", new UpdaterProcessor((ValueUpdater) null, "after"));
                        return null;
                        });
                    fail("fault " + fault + " must be rejected");
                    }
                catch (RuntimeException e)
                    {
                    assertContainsCause(e, SecurityException.class);
                    }
                finally
                    {
                    restorePeerProofProvider();
                    }

                assertTrue("fault " + fault + " reached the authorizer before proof rejection", getEvents().isEmpty());
                assertThat("fault " + fault + " mutated storage", cache.get("key"), is("before"));
                }
            finally
                {
                restorePeerProofProvider();
                cache.destroy();
                }
            }
        }

    @Test
    public void shouldExposeBoundedReadinessRefreshAndRealRotationStages() throws Exception
        {
        byte[] oldStore = Files.readAllBytes(s_clientStore);
        byte[] baseline = baselineAuthorities();
        String oldKey = currentPeerProofProvider().getKeyId();
        String newKey = certificateKeyId(s_clientNewCert);
        try
            {
            byte[] overlap = concatenate(baseline, Files.readAllBytes(s_clientNewCert));
            Files.write(s_authorities, overlap);
            Files.write(s_memberAuthorities, overlap);
            Files.write(s_memberTwoAuthorities, baseline);
            assertTrue(refreshAllProviders());
            assertThat(rotationStatus(PeerProofReadiness.Role.SUBJECT, PeerProofReadiness.Stage.ADD,
                    oldKey, newKey), is("subject:add-new:recipient-lagging:1"));
            assertThat(rotationStatus(PeerProofReadiness.Role.SENIOR, PeerProofReadiness.Stage.ADD,
                    oldKey, newKey), is("senior:add-new:recipient-lagging:1"));
            Files.write(s_memberTwoAuthorities, overlap);
            assertTrue(s_memberTwo.invoke(new RefreshPeerProofProvider()));
            assertThat(rotationStatus(PeerProofReadiness.Role.SUBJECT, PeerProofReadiness.Stage.ADD,
                    oldKey, newKey), is("subject:add-new:ready"));
            assertThat(rotationStatus(PeerProofReadiness.Role.SENIOR, PeerProofReadiness.Stage.ADD,
                    oldKey, newKey), is("senior:add-new:ready"));
            assertTrue(refreshAllProviders());
            assertThat(rotationStatus(PeerProofReadiness.Role.SUBJECT, PeerProofReadiness.Stage.ADD,
                    oldKey, newKey), is("subject:add-new:ready"));

            assertThat(rotationStatus(PeerProofReadiness.Role.SUBJECT, PeerProofReadiness.Stage.ACTIVATE,
                    oldKey, newKey), is("subject:activate-new:producer-lagging:1"));
            assertThat(rotationStatus(PeerProofReadiness.Role.SENIOR, PeerProofReadiness.Stage.ACTIVATE,
                    oldKey, newKey), is("senior:activate-new:producer-lagging:1"));

            Files.write(s_clientStore, Files.readAllBytes(s_clientNewStore));
            assertTrue(currentPeerProofProvider().refresh());
            assertThat(rotationStatus(PeerProofReadiness.Role.SUBJECT, PeerProofReadiness.Stage.ACTIVATE,
                    oldKey, newKey), is("subject:activate-new:ready"));
            assertThat(rotationStatus(PeerProofReadiness.Role.SENIOR, PeerProofReadiness.Stage.ACTIVATE,
                    oldKey, newKey), is("senior:activate-new:ready"));
            assertThat(rotationStatus(PeerProofReadiness.Role.SUBJECT, PeerProofReadiness.Stage.OVERLAP,
                    oldKey, newKey), is("subject:overlap:ready"));
            assertThat(rotationStatus(PeerProofReadiness.Role.SENIOR, PeerProofReadiness.Stage.OVERLAP,
                    oldKey, newKey), is("senior:overlap:ready"));

            byte[] retired = concatenate(Files.readAllBytes(s_clientNewCert), Files.readAllBytes(s_serverCert),
                    Files.readAllBytes(s_serverTwoCert), Files.readAllBytes(s_wrongSanCert));
            Files.write(s_memberTwoAuthorities, retired);
            assertTrue(s_memberTwo.invoke(new RefreshPeerProofProvider()));
            assertThat(rotationStatus(PeerProofReadiness.Role.SUBJECT, PeerProofReadiness.Stage.OVERLAP,
                    oldKey, newKey), is("subject:overlap:recipient-lagging:1"));
            assertThat(rotationStatus(PeerProofReadiness.Role.SENIOR, PeerProofReadiness.Stage.OVERLAP,
                    oldKey, newKey), is("senior:overlap:recipient-lagging:1"));
            Files.write(s_memberTwoAuthorities, overlap);
            assertTrue(s_memberTwo.invoke(new RefreshPeerProofProvider()));

            NamedCache cache = getNamedCache("authorized-peer-proof-rotation");
            cache.clear();
            Subject.doAs(subject("rotation-new"), (PrivilegedAction<Void>) () ->
                {
                cache.put("new", "accepted");
                return null;
                });
            assertThat(cache.get("new"), is("accepted"));

            Files.write(s_authorities, retired);
            Files.write(s_memberAuthorities, retired);
            Files.write(s_memberTwoAuthorities, overlap);
            assertTrue(refreshAllProviders());
            assertThat(rotationStatus(PeerProofReadiness.Role.SUBJECT, PeerProofReadiness.Stage.RETIRE,
                    oldKey, newKey), is("subject:retire-old:recipient-lagging:1"));
            assertThat(rotationStatus(PeerProofReadiness.Role.SENIOR, PeerProofReadiness.Stage.RETIRE,
                    oldKey, newKey), is("senior:retire-old:recipient-lagging:1"));
            Files.write(s_memberTwoAuthorities, retired);
            assertTrue(s_memberTwo.invoke(new RefreshPeerProofProvider()));
            assertThat(rotationStatus(PeerProofReadiness.Role.SUBJECT, PeerProofReadiness.Stage.RETIRE,
                    oldKey, newKey), is("subject:retire-old:ready"));
            assertThat(rotationStatus(PeerProofReadiness.Role.SENIOR, PeerProofReadiness.Stage.RETIRE,
                    oldKey, newKey), is("senior:retire-old:ready"));

            cache.put("old", "before");
            installPeerProofProvider(new FaultingPeerProofProvider(
                    currentPeerProofProvider(), ProofFault.ROTATED_OLD_KEY));
            try
                {
                Subject.doAs(subject("rotation-old"), (PrivilegedAction<Void>) () ->
                    {
                    cache.put("old", "after");
                    return null;
                    });
                fail("retired key must be rejected");
                }
            catch (RuntimeException e)
                {
                assertContainsCause(e, SecurityException.class);
                }
            finally
                {
                restorePeerProofProvider();
                }
            assertThat(cache.get("old"), is("before"));
            cache.destroy();
            }
        finally
            {
            restorePeerProofProvider();
            Files.write(s_clientStore, oldStore);
            writeAuthorities(baseline);
            refreshAllProviders();
            }
        }

    @Test
    public void shouldRecomputeProductionReadinessAfterRealJoinLeaveAndRoleEpochs() throws Exception
        {
        String process = "PEER01ReadinessTopology";
        String oldKey = currentPeerProofProvider().getKeyId();
        // Use an authority key that no live producer owns so joining the optional
        // subject producer changes only the authoritative topology epoch.
        String newKey = certificateKeyId(s_wrongSanCert);
        assertThat(rotationStatus(PeerProofReadiness.Role.SUBJECT, PeerProofReadiness.Stage.ADD,
                oldKey, newKey), is("subject:add-new:ready"));

        CoherenceClusterMember member = null;
        try
            {
            member = startCacheServer(process, "security", FILE_CFG_CACHE,
                    topologyMemberProperties("peer-readiness-joined"));
            Eventually.assertThat(invoking(member).getClusterSize(), is(4));
            assertThat(rotationStatus(PeerProofReadiness.Role.SUBJECT, PeerProofReadiness.Stage.ADD,
                    oldKey, newKey), is("subject:add-new:ready"));

            stopCacheServer(process);
            member = null;
            Eventually.assertThat(invoking(s_member).getClusterSize(), is(3));
            assertThat(rotationStatus(PeerProofReadiness.Role.SUBJECT, PeerProofReadiness.Stage.ADD,
                    oldKey, newKey), is("subject:add-new:ready"));

            member = startCacheServer(process, "security", FILE_CFG_CACHE,
                    topologyMemberProperties("peer-readiness-role-changed"));
            Eventually.assertThat(invoking(member).getClusterSize(), is(4));
            assertThat(rotationStatus(PeerProofReadiness.Role.SUBJECT, PeerProofReadiness.Stage.ADD,
                    oldKey, newKey), is("subject:add-new:ready"));
            }
        finally
            {
            if (member != null) {member.close();}
            stopCacheServer(process);
            }
        }

    @Test
    public void shouldAbortProductionReadinessForInFlightProducerRecipientAndSeniorChanges() throws Exception
        {
        String oldKey = currentPeerProofProvider().getKeyId();
        String newKey = certificateKeyId(s_wrongSanCert);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CoherenceClusterMember joined = null;
        String joinedProcess = "PEER01InFlightOwnership";
        String seniorService = "PeerProofInFlightSenior-" + System.nanoTime();
        try
            {
            Path entered = s_credentialDir.resolve("readiness-entered-ownership-" + System.nanoTime());
            Path release = s_credentialDir.resolve("readiness-release-ownership-" + System.nanoTime());
            s_memberTwo.invoke(new InstallReadinessBarrier(entered.toString(), release.toString()));
            Future<PeerProofReadiness.Result> ownership = executor.submit(() -> rotationResult(
                    PeerProofReadiness.Role.SUBJECT, PeerProofReadiness.Stage.ADD, oldKey, newKey));
            awaitFile(entered);

            Properties props = topologyMemberProperties("peer-readiness-storage-joined");
            props.setProperty("coherence.member", "peer-proof-optional");
            props.setProperty("coherence.distributed.localstorage", "true");
            joined = startCacheServer(joinedProcess, "security", FILE_CFG_CACHE, props);
            Eventually.assertThat(invoking(joined).getClusterSize(), is(4));
            Files.writeString(release, "release");
            PeerProofReadiness.Result changed = ownership.get(45, TimeUnit.SECONDS);
            assertFalse("in-flight ownership/producer change was accepted: " + changed.getStatus(), changed.isReady());
            s_memberTwo.invoke(new RestorePeerProofProvider());
            assertThat(rotationStatus(PeerProofReadiness.Role.SUBJECT, PeerProofReadiness.Stage.ADD,
                    oldKey, newKey), is("subject:add-new:ready"));

            stopCacheServer(joinedProcess);
            joined = null;
            Eventually.assertThat(invoking(s_member).getClusterSize(), is(3));

            assertThat(s_member.invoke(new EnsureInvocationService(seniorService)), is("peer-proof-server"));
            assertThat(s_memberTwo.invoke(new EnsureInvocationService(seniorService)), is("peer-proof-server"));
            Path seniorEntered = s_credentialDir.resolve("readiness-entered-senior-" + System.nanoTime());
            Path seniorRelease = s_credentialDir.resolve("readiness-release-senior-" + System.nanoTime());
            s_memberTwo.invoke(new InstallReadinessBarrier(seniorEntered.toString(), seniorRelease.toString()));
            Future<PeerProofReadiness.Result> senior = executor.submit(() -> rotationResult(
                    PeerProofReadiness.Role.SUBJECT, PeerProofReadiness.Stage.ADD, oldKey, newKey));
            awaitFile(seniorEntered);
            s_member.invoke(new StopGridService(seniorService));
            Eventually.assertThat(s_memberTwo.invoke(new ServiceSenior(seniorService)), is("peer-proof-server-two"));
            Files.writeString(seniorRelease, "release");
            changed = senior.get(45, TimeUnit.SECONDS);
            assertFalse("in-flight service senior change was accepted: " + changed.getStatus(), changed.isReady());
            s_memberTwo.invoke(new RestorePeerProofProvider());
            assertThat(rotationStatus(PeerProofReadiness.Role.SUBJECT, PeerProofReadiness.Stage.ADD,
                    oldKey, newKey), is("subject:add-new:ready"));
            }
        finally
            {
            try {s_memberTwo.invoke(new RestorePeerProofProvider());} catch (RuntimeException ignored) {}
            try {s_member.invoke(new StopGridService(seniorService));} catch (RuntimeException ignored) {}
            try {s_memberTwo.invoke(new StopGridService(seniorService));} catch (RuntimeException ignored) {}
            if (joined != null) {joined.close();}
            stopCacheServer(joinedProcess);
            executor.shutdownNow();
            }
        }

    public static void assertProductionReadinessAbortsAfterServiceAndOwnershipAbaTransitions() throws Exception
        {
        String oldKey = currentPeerProofProvider().getKeyId();
        String newKey = certificateKeyId(s_wrongSanCert);
        String serviceName = "PeerProofAbaService-" + System.nanoTime();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try
            {
            assertThat(s_member.invoke(new EnsureInvocationService(serviceName)), is("peer-proof-server"));
            assertThat(s_memberTwo.invoke(new EnsureInvocationService(serviceName)), is("peer-proof-server"));
            awaitServiceMemberCount(serviceName, 2);

            Path serviceEntered = s_credentialDir.resolve("readiness-entered-service-aba-" + System.nanoTime());
            Path serviceRelease = s_credentialDir.resolve("readiness-release-service-aba-" + System.nanoTime());
            s_memberTwo.invoke(new InstallReadinessBarrier(serviceEntered.toString(), serviceRelease.toString()));
            Future<PeerProofReadiness.Result> serviceAba = executor.submit(() -> rotationResult(
                    PeerProofReadiness.Role.SUBJECT, PeerProofReadiness.Stage.ADD, oldKey, newKey));
            awaitFile(serviceEntered);
            s_memberTwo.invoke(new StopGridService(serviceName));
            awaitServiceMemberCount(serviceName, 1);
            s_memberTwo.invoke(new EnsureInvocationService(serviceName));
            awaitServiceMemberCount(serviceName, 2);
            Files.writeString(serviceRelease, "release");
            PeerProofReadiness.Result changed = serviceAba.get(45L, TimeUnit.SECONDS);
            assertFalse("service A-B-A transition was accepted: " + changed.getStatus(), changed.isReady());
            s_memberTwo.invoke(new RestorePeerProofProvider());
            assertThat(rotationStatus(PeerProofReadiness.Role.SUBJECT, PeerProofReadiness.Stage.ADD,
                    oldKey, newKey), is("subject:add-new:ready"));

            Path ownershipEntered = s_credentialDir.resolve(
                    "readiness-entered-ownership-aba-" + System.nanoTime());
            Path ownershipRelease = s_credentialDir.resolve(
                    "readiness-release-ownership-aba-" + System.nanoTime());
            s_memberTwo.invoke(new InstallReadinessBarrier(ownershipEntered.toString(), ownershipRelease.toString()));
            Future<PeerProofReadiness.Result> ownershipAba = executor.submit(() -> rotationResult(
                    PeerProofReadiness.Role.SUBJECT, PeerProofReadiness.Stage.ADD, oldKey, newKey));
            awaitFile(ownershipEntered);
            s_member.invoke(new SetOwnershipConfig(
                    com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService
                            .OWNERSHIP_PENDING));
            s_member.invoke(new SetOwnershipConfig(
                    com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService
                            .OWNERSHIP_ENABLED));
            Files.writeString(ownershipRelease, "release");
            changed = ownershipAba.get(45L, TimeUnit.SECONDS);
            assertFalse("ownership-recipient A-B-A transition was accepted: " + changed.getStatus(),
                    changed.isReady());
            s_memberTwo.invoke(new RestorePeerProofProvider());
            assertThat(rotationStatus(PeerProofReadiness.Role.SUBJECT, PeerProofReadiness.Stage.ADD,
                    oldKey, newKey), is("subject:add-new:ready"));
            }
        finally
            {
            try {s_memberTwo.invoke(new RestorePeerProofProvider());} catch (RuntimeException ignored) {}
            try {s_member.invoke(new SetOwnershipConfig(
                    com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService
                            .OWNERSHIP_ENABLED));} catch (RuntimeException ignored) {}
            try {s_member.invoke(new StopGridService(serviceName));} catch (RuntimeException ignored) {}
            try {s_memberTwo.invoke(new StopGridService(serviceName));} catch (RuntimeException ignored) {}
            executor.shutdownNow();
            }
        }

    private static void awaitServiceMemberCount(String serviceName, int expected) throws Exception
        {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30L);
        int actual;
        do
            {
            actual = s_member.invoke(new ServiceMemberCount(serviceName));
            if (actual == expected)
                {
                return;
                }
            Thread.sleep(100L);
            }
        while (System.nanoTime() < deadline);
        assertThat("service membership did not stabilize", actual, is(expected));
        }

    @Test
    public void shouldRotateDistinctRealSubjectAndPerServiceSeniorProducers() throws Exception
        {
        String process = "PEER01SeniorOnlyTopology";
        String serviceOne = "PeerProofSeniorOne-" + System.nanoTime();
        String serviceTwo = "PeerProofTopologySeniorService";
        Path seniorStore = s_credentialDir.resolve("senior-only.p12");
        Path seniorCert = s_credentialDir.resolve("senior-only.pem");
        Path seniorNewStore = s_credentialDir.resolve("senior-only-new.p12");
        Path seniorNewCert = s_credentialDir.resolve("senior-only-new.pem");
        byte[] serverStore = Files.readAllBytes(s_serverStore);
        byte[] serverTwoStore = Files.readAllBytes(s_serverTwoStore);
        CoherenceClusterMember seniorOnly = null;
        try
            {
            createCredential("peer-proof-senior-only", seniorStore, seniorCert);
            createCredential("peer-proof-senior-only", seniorNewStore, seniorNewCert);
            List<Path> authorities = new ArrayList<>(Arrays.asList(s_clientCert, s_serverCert,
                    s_serverTwoCert, s_wrongSanCert, s_optionalCert, seniorCert));
            writeAuthorities(authorities);

            Properties props = topologyOnlyMemberProperties("peer-proof-senior-only",
                    "peer-senior-only", seniorStore);
            seniorOnly = startCacheServer(process, "security", FILE_CFG_TOPOLOGY_ONLY, props, true);
            Eventually.assertThat(invoking(seniorOnly).getClusterSize(), is(4));

            assertThat(s_member.invoke(new EnsureInvocationService(serviceOne)), is("peer-proof-server"));
            assertThat(s_memberTwo.invoke(new EnsureInvocationService(serviceOne)), is("peer-proof-server"));
            assertThat(s_memberTwo.invoke(new EnsureInvocationService(serviceTwo)), is("peer-proof-senior-only"));
            assertThat(s_member.invoke(new ServiceSenior("Management")), is("peer-proof-client"));

            authorities = rotateRealProducer(PeerProofReadiness.Role.SUBJECT, "two", s_serverTwoStore,
                    s_serverTwoNewStore, s_serverTwoCert, s_serverTwoNewCert, authorities, seniorOnly, 3, 2);
            // Subject rotation above intentionally leaves non-recipients untouched.
            // Populate the independent senior-recipient stores only after that
            // proof, so the cross-role producer assertion is not masked by lag.
            writeAuthorities(authorities);
            assertTrue(refreshAllProvidersWith(seniorOnly));
            assertTrue(rotationStatus(PeerProofReadiness.Role.SENIOR, PeerProofReadiness.Stage.ACTIVATE,
                    certificateKeyId(s_serverTwoCert), certificateKeyId(s_serverTwoNewCert))
                    .contains("target-producer-count:0"));

            authorities = rotateRealProducer(PeerProofReadiness.Role.SENIOR, "one", s_serverStore,
                    s_serverNewStore, s_serverCert, s_serverNewCert, authorities, seniorOnly, 3, 4);
            authorities = rotateRealProducer(PeerProofReadiness.Role.SENIOR, "extra", seniorStore,
                    seniorNewStore, seniorCert, seniorNewCert, authorities, seniorOnly, 3, 4);
            assertTrue(rotationStatus(PeerProofReadiness.Role.SUBJECT, PeerProofReadiness.Stage.ACTIVATE,
                    certificateKeyId(seniorCert), certificateKeyId(seniorNewCert))
                    .contains("target-producer-count:0"));
            }
        finally
            {
            Files.write(s_serverStore, serverStore);
            Files.write(s_serverTwoStore, serverTwoStore);
            if (seniorOnly != null)
                {
                try {seniorOnly.invoke(new StopGridService(serviceTwo));} catch (RuntimeException ignored) {}
                }
            try {s_member.invoke(new StopGridService(serviceOne));} catch (RuntimeException ignored) {}
            try {s_memberTwo.invoke(new StopGridService(serviceOne));} catch (RuntimeException ignored) {}
            try {s_memberTwo.invoke(new StopGridService(serviceTwo));} catch (RuntimeException ignored) {}
            if (seniorOnly != null) {seniorOnly.close();}
            stopCacheServer(process);
            writeAuthorities(Arrays.asList(s_clientCert, s_serverCert, s_serverTwoCert,
                    s_wrongSanCert, s_optionalCert));
            refreshAllProviders();
            Files.deleteIfExists(seniorStore);
            Files.deleteIfExists(seniorCert);
            Files.deleteIfExists(seniorNewStore);
            Files.deleteIfExists(seniorNewCert);
            }
        }

    @Test
    public void shouldKeepLastKnownGoodOnFailedRefreshAndRecoverWithoutSecretDiagnostics() throws Exception
        {
        Path shortStore = s_credentialDir.resolve("short-lived-client.p12");
        Path shortCert = s_credentialDir.resolve("short-lived-client.pem");
        Path authorities = s_credentialDir.resolve("short-lived-authorities.pem");
        CoherenceClusterMember client = null;
        CoherenceClusterMember storageOne = null;
        CoherenceClusterMember storageTwo = null;
        try
            {
            Instant start = Instant.now().minusSeconds(TimeUnit.DAYS.toSeconds(1)).plusSeconds(20);
            String startDate = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault()).format(start);
            createShortLivedCredential("peer-proof-client", shortStore, shortCert, startDate);
            Files.write(authorities, concatenate(Files.readAllBytes(shortCert), Files.readAllBytes(s_serverCert),
                    Files.readAllBytes(s_serverTwoCert)));

            String cluster = "peer01-p68-lkg-expiry-" + System.nanoTime();
            Properties props = auxiliaryProperties("peer-proof-server", s_serverStore, authorities,
                    FILE_CFG_OVERRIDE, "dev", "hardened", false);
            props.setProperty("coherence.cluster", cluster);
            props.setProperty("coherence.distributed.localstorage", "true");
            storageOne = startCacheServer("PEER01LkgStorageOne", "security", FILE_CFG_CACHE, props, true);

            props = auxiliaryProperties("peer-proof-server-two", s_serverTwoStore, authorities,
                    FILE_CFG_OVERRIDE, "dev", "hardened", false);
            props.setProperty("coherence.cluster", cluster);
            props.setProperty("coherence.distributed.localstorage", "true");
            storageTwo = startCacheServer("PEER01LkgStorageTwo", "security", FILE_CFG_CACHE, props, true);

            props = auxiliaryProperties("peer-proof-client", shortStore, authorities,
                    FILE_CFG_OVERRIDE, "dev", "hardened", false);
            props.setProperty("coherence.cluster", cluster);
            client = startCacheServer("PEER01LkgClient", "security", FILE_CFG_CACHE, props, true);
            Eventually.assertThat(invoking(client).getClusterSize(), is(3));

            assertThat(client.invoke(new IsolatedProofPut("authorized-peer-proof-lkg", "key", "accepted")),
                    is("accepted"));

            Files.writeString(authorities, "password=" + PEER_PASSWORD + "; backend=/sensitive/location");
            assertFalse(client.invoke(new RefreshPeerProofProvider()));
            String status = client.invoke(new GetPeerProofStatus(Role.GENERAL));
            assertTrue(status.startsWith("reload-degraded-last-known-good:"));
            assertFalse(status.contains(PEER_PASSWORD));
            assertFalse(status.contains("/sensitive/location"));

            assertThat(client.invoke(new IsolatedProofPut("authorized-peer-proof-lkg", "key", "still-valid")),
                    is("still-valid"));

            KeyStore store = KeyStore.getInstance("PKCS12");
            try (java.io.InputStream in = Files.newInputStream(shortStore))
                {
                store.load(in, PEER_PASSWORD.toCharArray());
                }
            X509Certificate cert = (X509Certificate) store.getCertificate("peer-signing");
            long deadline = cert.getNotAfter().getTime() + X509PeerProofProvider.CLOCK_SKEW_MILLIS + 1_000L;
            while (System.currentTimeMillis() < deadline)
                {
                Thread.sleep(Math.min(5_000L, deadline - System.currentTimeMillis()));
                }

            try
                {
                client.invoke(new IsolatedProofPut("authorized-peer-proof-lkg", "key", "after-expiry"));
                fail("an expired last-known-good identity must fail before mutation");
                }
            catch (RuntimeException e)
                {
                assertContainsCause(e, SecurityException.class);
                }

            assertThat(client.invoke(new IsolatedProofGet("authorized-peer-proof-lkg", "key")),
                    is("still-valid"));

            Files.write(shortStore, Files.readAllBytes(s_clientStore));
            Files.write(authorities, baselineAuthorities());
            assertTrue(client.invoke(new RefreshPeerProofProvider()));
            assertTrue(storageOne.invoke(new RefreshPeerProofProvider()));
            assertTrue(storageTwo.invoke(new RefreshPeerProofProvider()));
            assertThat(client.invoke(new IsolatedProofPut("authorized-peer-proof-lkg", "key", "recovered")),
                    is("recovered"));
            }
        finally
            {
            if (client != null) {client.close();}
            if (storageOne != null) {storageOne.close();}
            if (storageTwo != null) {storageTwo.close();}
            Files.deleteIfExists(shortStore);
            Files.deleteIfExists(shortCert);
            Files.deleteIfExists(authorities);
            }
        }

    @Test
    public void shouldHonorOptionalRequiredAndModeStartupAcrossRealProcesses() throws Exception
        {
        Path stagedAuthorities = s_credentialDir.resolve("staged-startup-authorities.pem");
        Files.writeString(stagedAuthorities, "backend=/secret/location; password=" + PEER_PASSWORD);
        String optionalName = "PEER01OptionalStartup";
        Properties optional = auxiliaryProperties("peer-proof-optional", s_optionalStore, stagedAuthorities,
                "peer-proof-optional-override.xml", "prod", "hardened", false);
        CoherenceClusterMember member = startCacheServer(optionalName, "security", null, optional, true);
        try
            {
            String status = member.invoke(new GetPeerProofStatus(Role.GENERAL));
            assertTrue(status.startsWith("reload-unavailable:"));
            assertFalse(status.contains(PEER_PASSWORD));
            assertFalse(status.contains("/secret/location"));

            Files.write(stagedAuthorities, baselineAuthorities());
            assertTrue(member.invoke(new RefreshPeerProofProvider()));
            String general = member.invoke(new GetPeerProofStatus(Role.GENERAL));
            String subject = member.invoke(new GetPeerProofStatus(Role.SUBJECT));
            String senior = member.invoke(new GetPeerProofStatus(Role.SENIOR));
            String cluster = member.invoke(new GetPeerProofStatus(Role.CLUSTER));
            assertTrue(general, general.equals("identity-unbound") || general.startsWith("ready:key="));
            assertTrue(subject, subject.equals("staged-unavailable:identity-unbound")
                    || subject.equals("ready"));
            assertTrue(senior, senior.equals("staged-unavailable:identity-unbound")
                    || senior.equals("ready"));
            assertTrue(cluster, cluster.equals("all-member-query-required"));
            assertTrue(general.length() <= 128 && subject.length() <= 128 && senior.length() <= 128
                    && cluster.length() <= 128);
            }
        finally
            {
            member.close();
            }

        assertOptionalCredentialRejected("missing-authorities", "peer-proof-optional", s_optionalStore,
                s_credentialDir.resolve("missing-authorities.pem"));
        assertOptionalCredentialRejected("ca-identity", "peer-proof-ca", s_caStore, s_authorities);
        assertOptionalCredentialRejected("absent-key-usage", "peer-proof-no-ku", s_noKeyUsageStore, s_authorities);
        assertOptionalCredentialRejected("false-key-usage", "peer-proof-false-ku", s_falseKeyUsageStore,
                s_authorities);
        assertOptionalCredentialRejected("expired-identity", "peer-proof-expired", s_expiredStore, s_authorities);

        Files.writeString(stagedAuthorities, "malformed required authority");
        assertRequiredStartupFails("PEER01SubjectRequiredStartup", FILE_CFG_OVERRIDE, false, stagedAuthorities);
        assertRequiredStartupFails("PEER01SeniorRequiredStartup", "peer-proof-optional-override.xml", true,
                stagedAuthorities);
        Files.deleteIfExists(stagedAuthorities);

        for (String runtime : Arrays.asList("dev", "prod"))
            {
            for (String security : Arrays.asList("compatibility", "hardened"))
                {
                String name = "PEER01Axes-" + runtime + '-' + security;
                Properties axes = auxiliaryProperties("peer-proof-optional", s_optionalStore, s_authorities,
                        FILE_CFG_OVERRIDE, runtime, security, true);
                CoherenceClusterMember axisMember = startCacheServer(name, "security", FILE_CFG_CACHE, axes, true);
                try
                    {
                    assertTrue(runtime + '/' + security,
                            axisMember.invoke(new RefreshPeerProofProvider()));
                    String status = axisMember.invoke(new GetPeerProofStatus(Role.GENERAL));
                    assertTrue(runtime + '/' + security + ": " + status,
                            status.equals("identity-unbound") || status.startsWith("ready:key="));
                    assertTrue(runtime + '/' + security + " status is unbounded", status.length() <= 128);
                    }
                finally
                    {
                    axisMember.close();
                    }
                }
            }
        }

    private static void assertOptionalCredentialRejected(String label, String memberName, Path identity,
            Path authorities)
        {
        Properties props = auxiliaryProperties(memberName, identity, authorities,
                "peer-proof-optional-override.xml", "dev", "compatibility", false);
        CoherenceClusterMember member = startCacheServer("PEER01Optional-" + label, "security", null, props, true);
        try
            {
            String status = member.invoke(new GetPeerProofStatus(Role.GENERAL));
            assertTrue(label + ": " + status, status.startsWith("reload-unavailable:"));
            assertTrue("bounded diagnostic", status.length() <= 128);
            assertFalse(status.contains(PEER_PASSWORD));
            assertFalse(status.contains(s_credentialDir.toString()));
            }
        finally
            {
            member.close();
            }
        }

    private static void assertRequiredStartupFails(String processName, String override, boolean seniorRequired,
            Path authorities)
        {
        Properties required = auxiliaryProperties("peer-proof-optional", s_optionalStore, authorities,
                override, "dev", "hardened", seniorRequired);
        CoherenceClusterMember member = null;
        try
            {
            member = startCacheServer(processName, "security", FILE_CFG_CACHE, required, false);
            member.waitFor(Timeout.after(30, TimeUnit.SECONDS));
            }
        finally
            {
            if (member != null)
                {
                member.close();
                }
            }
        }

    private static Properties auxiliaryProperties(String memberName, Path identity, Path authorities,
            String override, String runtimeMode, String securityMode, boolean required)
        {
        Properties props = new Properties();
        props.setProperty("coherence.override", override);
        // Startup-policy and runtime/security-mode axes are independent of the
        // dev-mode functional fixture (Coherence deliberately forbids mixing
        // dev and prod members in one cluster), so give each auxiliary process
        // its own single-member cluster.
        props.setProperty("coherence.cluster", "peer01-p68-startup-" + memberName + '-' + runtimeMode
                + '-' + securityMode);
        props.setProperty("coherence.localhost", "127.0.0.1");
        props.setProperty("coherence.wka", "127.0.0.1");
        props.setProperty("coherence.member", memberName);
        props.setProperty("coherence.distributed.localstorage", "false");
        props.setProperty("coherence.mode", runtimeMode);
        props.setProperty("coherence.security.mode", securityMode);
        props.setProperty("peer.proof.identity.store", identity.toString());
        props.setProperty("peer.proof.authorities", authorities.toString());
        props.setProperty("peer.proof.password.file", s_passwordFile.toString());
        props.setProperty("coherence.security.peer.senior-metadata-proof.required", Boolean.toString(required));
        props.setProperty("coherence.management", "none");
        return props;
        }

    @Test
    public void shouldRejectCanonicalDuplicateStateAndRecoverAcrossRealMembers()
        {
        NamedCache cache = getNamedCache("authorized-peer-proof-duplicate");
        cache.clear();
        cache.put("key", "before");
        resetAuthorizer();
        s_member.invoke(new InstallPeerProofFault(ProofFault.DUPLICATE));
        s_memberTwo.invoke(new InstallPeerProofFault(ProofFault.DUPLICATE));
        try
            {
            try
                {
                Subject.doAs(subject("duplicate-state"), (PrivilegedAction<Void>) () ->
                    {
                    cache.put("key", "after");
                    return null;
                    });
                fail("duplicate canonical identity state must reject before mutation");
                }
            catch (RuntimeException e)
                {
                assertContainsCause(e, SecurityException.class);
                }
            assertTrue(getEvents().isEmpty());
            assertThat(cache.get("key"), is("before"));
            String statusOne = s_member.invoke(new GetPeerProofStatus(Role.GENERAL));
            String statusTwo = s_memberTwo.invoke(new GetPeerProofStatus(Role.GENERAL));
            assertTrue("no real receiver retained duplicate state: " + statusOne + ", " + statusTwo,
                    statusOne.startsWith("duplicate-identity:") || statusTwo.startsWith("duplicate-identity:"));
            }
        finally
            {
            s_member.invoke(new RestorePeerProofProvider());
            s_memberTwo.invoke(new RestorePeerProofProvider());
            }

        Subject.doAs(subject("duplicate-recovered"), (PrivilegedAction<Void>) () ->
            {
            cache.put("key", "recovered");
            return null;
            });
        assertThat(cache.get("key"), is("recovered"));
        cache.destroy();
        }

    @Test
    public void shouldDriveValidAndInvalidSeniorProofsAcrossRealHeartbeatCarriers() throws Exception
        {
        s_member.invoke(new ResetSeniorProofCounts());
        s_memberTwo.invoke(new ResetSeniorProofCounts());
        s_member.invoke(new InstallPeerProofFault(ProofFault.OBSERVE_SENIOR));
        s_memberTwo.invoke(new InstallPeerProofFault(ProofFault.OBSERVE_SENIOR));
        try
            {
            sendSeniorHeartbeat();
            awaitSeniorProofCount(true);
            installPeerProofProvider(new FaultingPeerProofProvider(
                    currentPeerProofProvider(), ProofFault.SENIOR_SIGNATURE));
            sendSeniorHeartbeat();
            awaitSeniorProofCount(false);
            }
        finally
            {
            restorePeerProofProvider();
            s_member.invoke(new RestorePeerProofProvider());
            s_memberTwo.invoke(new RestorePeerProofProvider());
            }

        Eventually.assertThat(invoking(s_member).isServiceRunning("StorageAuthorizerRouteService"), is(true));
        Eventually.assertThat(invoking(s_memberTwo).isServiceRunning("StorageAuthorizerRouteService"), is(true));
        assertTrue(currentPeerProofProvider().isReady());
        assertTrue(currentPeerProofProvider().getClusterReadinessStatus().length() <= 128);
        }

    @Test
    public void shouldRejectNonSeniorClaimBeforeRealPanicAction() throws Exception
        {
        s_cSeniorProofInvalid.set(0);
        installPeerProofProvider(new FaultingPeerProofProvider(
                currentPeerProofProvider(), ProofFault.OBSERVE_SENIOR));
        s_member.invoke(new InstallPeerProofFault(ProofFault.SENIOR_CLAIM));
        try
            {
            s_member.invoke(new SendJuniorPanic("peer-proof-server-two"));
            for (int i = 0; i < 20 && s_cSeniorProofInvalid.get() == 0; i++)
                {
                Thread.sleep(500L);
                }
            assertTrue("the forged senior identity must be rejected on the real panic carrier",
                    s_cSeniorProofInvalid.get() > 0);
            Eventually.assertDeferred(() -> ((SafeCluster) CacheFactory.getCluster()).getRunningCluster()
                    .getClusterService().getClusterMemberSet().size(), is(3));
            }
        finally
            {
            restorePeerProofProvider();
            s_member.invoke(new RestorePeerProofProvider());
            }
        }

    @Test
    public void shouldDrivePanicDelegatedKillAndFanOutAcrossRealProcesses() throws Exception
        {
        String cluster = "peer01-p68-panic-fanout-" + System.nanoTime();
        CoherenceClusterMember senior = null;
        CoherenceClusterMember juniorOne = null;
        CoherenceClusterMember juniorTwo = null;
        try
            {
            Properties props = auxiliaryProperties("peer-proof-client", s_clientStore, s_authorities,
                    FILE_CFG_OVERRIDE, "dev", "hardened", false);
            props.setProperty("coherence.cluster", cluster);
            senior = startCacheServer("PEER01PanicSenior", "security", null, props, true);

            props = auxiliaryProperties("peer-proof-server", s_serverStore, s_authorities,
                    FILE_CFG_OVERRIDE, "dev", "hardened", true);
            props.setProperty("coherence.cluster", cluster);
            juniorOne = startCacheServer("PEER01PanicJuniorOne", "security", null, props, true);

            props = auxiliaryProperties("peer-proof-server-two", s_serverTwoStore, s_authorities,
                    FILE_CFG_OVERRIDE, "dev", "hardened", true);
            props.setProperty("coherence.cluster", cluster);
            juniorTwo = startCacheServer("PEER01PanicJuniorTwo", "security", null, props, true);

            Eventually.assertThat(invoking(senior).getClusterSize(), is(3));
            senior.invoke(new SendSeniorPanic("peer-proof-server"));

            assertProcessOutputContains("PEER01PanicJuniorOne", "Received panic from senior");
            assertProcessOutputContains("PEER01PanicSenior", "Received a Kill message from a valid");
            assertProcessOutputContains("PEER01PanicJuniorTwo", "Received a Kill message from a valid");
            assertProcessOutputExcludes("PEER01PanicSenior", "Senior metadata proof rejected");
            assertProcessOutputExcludes("PEER01PanicJuniorOne", "Senior metadata proof rejected");
            assertProcessOutputExcludes("PEER01PanicJuniorTwo", "Senior metadata proof rejected");
            }
        finally
            {
            if (senior != null) {senior.close();}
            if (juniorOne != null) {juniorOne.close();}
            if (juniorTwo != null) {juniorTwo.close();}
            }
        }

    private static void assertProcessOutputContains(String processName, String expected)
            throws Exception
        {
        for (int i = 0; i < 40; i++)
            {
            if (readProcessOutput(processName).contains(expected))
                {
                return;
                }
            Thread.sleep(500L);
            }
        fail(processName + " output did not contain: " + expected);
        }

    private static void assertProcessOutputExcludes(String processName, String forbidden)
            throws Exception
        {
        assertFalse(processName + " output contained: " + forbidden,
                readProcessOutput(processName).contains(forbidden));
        }

    private static String readProcessOutput(String processName)
            throws Exception
        {
        Path output = Path.of(System.getProperty("test.project.dir"), "target", "test-output", "functional",
                StorageAccessAuthorizerRouteTests.class.getSimpleName(), processName + ".out");
        return Files.exists(output) ? Files.readString(output) : "";
        }

    /** Emit the production senior-heartbeat message over its real carrier. */
    private static void sendSeniorHeartbeat()
        {
        ClusterService service = ((SafeCluster) CacheFactory.getCluster()).getRunningCluster().getClusterService();
        MasterMemberSet setMembers = service.getClusterMemberSet();
        ClusterService.SeniorMemberHeartbeat heartbeat = (ClusterService.SeniorMemberHeartbeat)
                service.instantiateMessage("SeniorMemberHeartbeat");
        heartbeat.setLastReceivedMillis(setMembers.getThisMember().getLastIncomingMillis());
        heartbeat.setMemberSet(setMembers);
        heartbeat.setWkaEnabled(service.isWkaEnabled());
        heartbeat.setLastJoinTime(setMembers.getLastJoinTime());
        MemberSet recipients = new MemberSet();
        recipients.addAll(setMembers);
        recipients.remove(setMembers.getThisMember().getId());
        heartbeat.setToMemberSet(recipients);
        service.send(heartbeat);
        }

    private static void awaitSeniorProofCount(boolean fValid) throws Exception
        {
        for (int i = 0; i < 20; i++)
            {
            int count = s_member.invoke(new GetSeniorProofCount(fValid))
                    + s_memberTwo.invoke(new GetSeniorProofCount(fValid));
            if (count > 0)
                {
                return;
                }
            Thread.sleep(500L);
            }
        fail("no " + (fValid ? "valid" : "invalid") + " senior proof traversed a real heartbeat carrier");
        }

    @Test
    public void shouldMeetProductionPeerProofPerformanceGate()
        {
        Subject subject = new Subject();
        subject.getPrincipals().add((Principal) () -> "peer-proof-performance-user");

        Subject.doAs(subject, (PrivilegedAction<Void>) () ->
            {
            NamedCache cacheBaseline = getNamedCache("peer-proof-perf-baseline");
            NamedCache cacheProof = getNamedCache("peer-proof-perf-proof");
            try
                {
                runPerformanceWorkload(cacheBaseline, -1, PERFORMANCE_WARMUP_OPERATIONS);
                runPerformanceWorkload(cacheProof, -1, PERFORMANCE_WARMUP_OPERATIONS);

                double[] aBaselineThroughput = new double[PERFORMANCE_RUNS];
                double[] aProofThroughput = new double[PERFORMANCE_RUNS];
                double[] aBaselineP95 = new double[PERFORMANCE_RUNS];
                double[] aProofP95 = new double[PERFORMANCE_RUNS];

                for (int i = 0; i < PERFORMANCE_RUNS; i++)
                    {
                    WorkloadResult baseline;
                    WorkloadResult proof;
                    if ((i & 1) == 0)
                        {
                        baseline = runPerformanceWorkload(cacheBaseline, i, PERFORMANCE_OPERATIONS);
                        proof = runPerformanceWorkload(cacheProof, i, PERFORMANCE_OPERATIONS);
                        }
                    else
                        {
                        proof = runPerformanceWorkload(cacheProof, i, PERFORMANCE_OPERATIONS);
                        baseline = runPerformanceWorkload(cacheBaseline, i, PERFORMANCE_OPERATIONS);
                        }
                    aBaselineThroughput[i] = baseline.m_cThroughput;
                    aProofThroughput[i] = proof.m_cThroughput;
                    aBaselineP95[i] = baseline.m_cP95Micros;
                    aProofP95[i] = proof.m_cP95Micros;
                    }

                double cBaselineThroughput = median(aBaselineThroughput);
                double cProofThroughput = median(aProofThroughput);
                double cBaselineP95 = median(aBaselineP95);
                double cProofP95 = median(aProofP95);
                double nThroughputRatio = cProofThroughput / cBaselineThroughput;
                double nP95Ratio = cProofP95 / cBaselineP95;

                System.out.printf("PEER proof A/B: baseline throughput=%.2f ops/s, proof throughput=%.2f ops/s, "
                                + "ratio=%.3f; baseline p95=%.2f us, proof p95=%.2f us, ratio=%.3f%n",
                        cBaselineThroughput, cProofThroughput, nThroughputRatio,
                        cBaselineP95, cProofP95, nP95Ratio);

                assertTrue("proof throughput ratio must be at least 0.80 but was " + nThroughputRatio,
                        nThroughputRatio >= 0.80d);
                assertTrue("proof p95 latency ratio must be no more than 1.25 but was " + nP95Ratio,
                        nP95Ratio <= 1.25d);
                }
            finally
                {
                cacheBaseline.destroy();
                cacheProof.destroy();
                }
            return null;
            });
        }

    private static WorkloadResult runPerformanceWorkload(NamedCache cache, int nRun, int cOperations)
        {
        ExecutorService executor = Executors.newFixedThreadPool(PERFORMANCE_CONCURRENCY);
        CountDownLatch ready = new CountDownLatch(PERFORMANCE_CONCURRENCY);
        CountDownLatch start = new CountDownLatch(1);
        long[] aLatency = new long[cOperations];
        try
            {
            Future<?>[] aFuture = new Future[PERFORMANCE_CONCURRENCY];
            int cPerThread = cOperations / PERFORMANCE_CONCURRENCY;
            for (int i = 0; i < PERFORMANCE_CONCURRENCY; i++)
                {
                int nThread = i;
                aFuture[i] = executor.submit(() ->
                    {
                    ready.countDown();
                    try
                        {
                        start.await();
                        }
                    catch (InterruptedException e)
                        {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                        }
                    int nFirst = nThread * cPerThread;
                    int nLast = nThread == PERFORMANCE_CONCURRENCY - 1 ? cOperations : nFirst + cPerThread;
                    for (int n = nFirst; n < nLast; n++)
                        {
                        long lStart = System.nanoTime();
                        cache.put(nRun + ":" + nThread + ":" + n, n);
                        aLatency[n] = System.nanoTime() - lStart;
                        }
                    });
                }
            if (!ready.await(30, TimeUnit.SECONDS))
                {
                throw new AssertionError("performance workers did not become ready");
                }
            long lStart = System.nanoTime();
            start.countDown();
            for (Future<?> future : aFuture)
                {
                future.get(2, TimeUnit.MINUTES);
                }
            long cElapsed = System.nanoTime() - lStart;
            Arrays.sort(aLatency);
            long cP95 = aLatency[Math.min(aLatency.length - 1,
                    (int) Math.ceil(aLatency.length * 0.95d) - 1)];
            return new WorkloadResult(cOperations * 1_000_000_000.0d / cElapsed, cP95 / 1_000.0d);
            }
        catch (Exception e)
            {
            throw new AssertionError("PEER proof performance workload failed", e);
            }
        finally
            {
            executor.shutdownNow();
            }
        }

    private static double median(double[] values)
        {
        double[] copy = values.clone();
        Arrays.sort(copy);
        return copy[copy.length / 2];
        }

    private static Subject subject(String sName)
        {
        Subject subject = new Subject();
        subject.getPrincipals().add((Principal) () -> sName);
        return subject;
        }

    private static PeerProofProvider currentPeerProofProvider()
        {
        PeerProofProvider provider = Security.getPeerProofProvider();
        if (s_originalPeerProofProvider == null && !(provider instanceof FaultingPeerProofProvider))
            {
            s_originalPeerProofProvider = provider;
            }
        return provider;
        }

    private static void installPeerProofProvider(PeerProofProvider provider)
        {
        try
            {
            Field field = Security.class.getDeclaredField("__s_PeerProofProvider");
            field.setAccessible(true);
            field.set(null, provider);
            }
        catch (ReflectiveOperationException e)
            {
            throw new AssertionError("unable to install PEER proof test decorator", e);
            }
        }

    private static void restorePeerProofProvider()
        {
        if (s_originalPeerProofProvider != null)
            {
            installPeerProofProvider(s_originalPeerProofProvider);
            }
        }

    private static boolean refreshAllProviders()
        {
        restorePeerProofProvider();
        boolean result = currentPeerProofProvider().refresh();
        return result && s_member.invoke(new RefreshPeerProofProvider())
                && s_memberTwo.invoke(new RefreshPeerProofProvider());
        }

    private static byte[] baselineAuthorities() throws Exception
        {
        return concatenate(Files.readAllBytes(s_clientCert), Files.readAllBytes(s_serverCert),
                Files.readAllBytes(s_serverTwoCert), Files.readAllBytes(s_wrongSanCert),
                Files.readAllBytes(s_optionalCert));
        }

    private static void writeAuthorities(byte[]... arrays) throws Exception
        {
        byte[] authorities = concatenate(arrays);
        Files.write(s_authorities, authorities);
        Files.write(s_memberAuthorities, authorities);
        Files.write(s_memberTwoAuthorities, authorities);
        }

    private static void writeAuthorities(Collection<Path> certificates) throws Exception
        {
        List<byte[]> values = new ArrayList<>();
        for (Path certificate : certificates)
            {
            values.add(Files.readAllBytes(certificate));
            }
        writeAuthorities(values.toArray(new byte[0][]));
        }

    private static List<Path> rotateRealProducer(PeerProofReadiness.Role role, String target,
            Path liveStore, Path newStore, Path oldCert, Path newCert, List<Path> currentAuthorities,
            CoherenceClusterMember extra, int cProducers, int cRecipients) throws Exception
        {
        String oldKey = certificateKeyId(oldCert);
        String newKey = certificateKeyId(newCert);
        byte[] producerAuthorities = role == PeerProofReadiness.Role.SUBJECT
                ? Files.readAllBytes(s_authorities) : null;
        List<Path> overlap = new ArrayList<>(currentAuthorities);
        overlap.add(newCert);
        if (role == PeerProofReadiness.Role.SUBJECT)
            {
            byte[] bytes = concatenateCertificates(overlap);
            Files.write(s_memberAuthorities, bytes);
            assertTrue(s_member.invoke(new RefreshPeerProofProvider()));
            assertTrue(rotationStatus(role, PeerProofReadiness.Stage.ADD, oldKey, newKey)
                    .contains("recipient-lagging:1"));
            Files.write(s_memberTwoAuthorities, bytes);
            assertTrue(s_memberTwo.invoke(new RefreshPeerProofProvider()));
            assertTrue("storage-disabled producer was given unrelated subject authority",
                    Arrays.equals(producerAuthorities, Files.readAllBytes(s_authorities)));
            }
        else
            {
            writeAuthorities(overlap);
            assertTrue(refreshAllProvidersWith(extra));
            }

        PeerProofReadiness.Result result = rotationResult(role, PeerProofReadiness.Stage.ADD, oldKey, newKey);
        assertTrue(result.getStatus(), result.isReady());
        assertThat(result.getProducerCount(), is(cProducers));
        assertThat(result.getRecipientCount(), is(cRecipients));
        assertTrue(rotationStatus(role, PeerProofReadiness.Stage.ACTIVATE, oldKey, newKey)
                .contains("producer-lagging:1"));

        Files.write(liveStore, Files.readAllBytes(newStore));
        assertTrue(refreshTarget(target, extra));
        assertThat(rotationStatus(role, PeerProofReadiness.Stage.ACTIVATE, oldKey, newKey),
                is(role == PeerProofReadiness.Role.SUBJECT
                        ? "subject:activate-new:ready" : "senior:activate-new:ready"));
        assertThat(rotationStatus(role, PeerProofReadiness.Stage.OVERLAP, oldKey, newKey),
                is(role == PeerProofReadiness.Role.SUBJECT ? "subject:overlap:ready" : "senior:overlap:ready"));

        List<Path> retired = new ArrayList<>(currentAuthorities);
        assertTrue(retired.remove(oldCert));
        retired.add(newCert);
        if (role == PeerProofReadiness.Role.SUBJECT)
            {
            byte[] bytes = concatenateCertificates(retired);
            Files.write(s_memberAuthorities, bytes);
            assertTrue(s_member.invoke(new RefreshPeerProofProvider()));
            assertTrue(rotationStatus(role, PeerProofReadiness.Stage.RETIRE, oldKey, newKey)
                    .contains("recipient-lagging:1"));
            Files.write(s_memberTwoAuthorities, bytes);
            assertTrue(s_memberTwo.invoke(new RefreshPeerProofProvider()));
            assertTrue("storage-disabled producer authority changed during subject retirement",
                    Arrays.equals(producerAuthorities, Files.readAllBytes(s_authorities)));
            }
        else
            {
            writeAuthorities(retired);
            assertTrue(rotationStatus(role, PeerProofReadiness.Stage.RETIRE, oldKey, newKey)
                    .contains("recipient-lagging:"));
            assertTrue(refreshAllProvidersWith(extra));
            }
        assertThat(rotationStatus(role, PeerProofReadiness.Stage.RETIRE, oldKey, newKey),
                is(role == PeerProofReadiness.Role.SUBJECT ? "subject:retire-old:ready" : "senior:retire-old:ready"));
        return retired;
        }

    private static byte[] concatenateCertificates(Collection<Path> certificates) throws Exception
        {
        List<byte[]> values = new ArrayList<>();
        for (Path certificate : certificates)
            {
            values.add(Files.readAllBytes(certificate));
            }
        return concatenate(values.toArray(new byte[0][]));
        }

    private static void awaitFile(Path path) throws Exception
        {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30L);
        while (!Files.exists(path) && System.nanoTime() < deadline)
            {
            Thread.sleep(25L);
            }
        assertTrue("readiness collection barrier was not entered", Files.exists(path));
        }

    private static boolean refreshTarget(String target, CoherenceClusterMember extra)
        {
        switch (target)
            {
            case "one": return s_member.invoke(new RefreshPeerProofProvider());
            case "two": return s_memberTwo.invoke(new RefreshPeerProofProvider());
            case "extra": return extra.invoke(new RefreshPeerProofProvider());
            default: return currentPeerProofProvider().refresh();
            }
        }

    private static boolean refreshAllProvidersWith(CoherenceClusterMember extra)
        {
        return refreshAllProviders() && extra.invoke(new RefreshPeerProofProvider());
        }

    private static String rotationStatus(PeerProofReadiness.Role role, PeerProofReadiness.Stage stage,
            String oldKey, String newKey)
        {
        return rotationResult(role, stage, oldKey, newKey).getStatus();
        }

    private static PeerProofReadiness.Result rotationResult(PeerProofReadiness.Role role,
            PeerProofReadiness.Stage stage, String oldKey, String newKey)
        {
        InvocationService service = (InvocationService) CacheFactory.ensureCluster().getService("Management");
        return PeerProofReadiness.query(CacheFactory.ensureCluster(), service,
                new PeerProofReadiness.Query(role, stage, oldKey, newKey));
        }

    private static Properties topologyMemberProperties(String role)
        {
        Properties props = new Properties();
        props.setProperty("coherence.override", FILE_CFG_OVERRIDE);
        props.setProperty("coherence.localhost", "127.0.0.1");
        props.setProperty("coherence.wka", "127.0.0.1");
        props.setProperty("coherence.member", "peer-proof-optional");
        props.setProperty("coherence.role", role);
        props.setProperty("coherence.distributed.localstorage", "false");
        props.setProperty("coherence.security.mode", "hardened");
        props.setProperty("peer.proof.identity.store", s_optionalStore.toString());
        props.setProperty("peer.proof.authorities", s_authorities.toString());
        props.setProperty("peer.proof.password.file", s_passwordFile.toString());
        props.setProperty("coherence.security.peer.senior-metadata-proof.required", "true");
        props.setProperty("coherence.management.remote.registryport", String.valueOf(getAvailablePorts().next()));
        return props;
        }

    private static Properties topologyOnlyMemberProperties(String memberName, String role, Path identity)
        {
        Properties props = topologyMemberProperties(role);
        props.setProperty("coherence.member", memberName);
        props.setProperty("peer.proof.identity.store", identity.toString());
        props.setProperty("coherence.cacheconfig", FILE_CFG_TOPOLOGY_ONLY);
        return props;
        }

    private static String certificateKeyId(Path path) throws Exception
        {
        X509Certificate cert;
        try (java.io.InputStream in = Files.newInputStream(path))
            {
            cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
            }
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(cert.getEncoded());
        StringBuilder sb = new StringBuilder(X509PeerProofProvider.KEY_ID_PREFIX);
        for (byte b : digest)
            {
            sb.append(String.format("%02x", b & 0xff));
            }
        return sb.toString();
        }

    private static byte[] concatenate(byte[]... arrays)
        {
        int length = 0;
        for (byte[] array : arrays)
            {
            length += array.length;
            }
        byte[] result = new byte[length];
        int offset = 0;
        for (byte[] array : arrays)
            {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
            }
        return result;
        }

    private static class WorkloadResult
        {
        private WorkloadResult(double cThroughput, double cP95Micros)
            {
            m_cThroughput = cThroughput;
            m_cP95Micros = cP95Micros;
            }

        private final double m_cThroughput;
        private final double m_cP95Micros;
        }

    @Test
    public void shouldCaptureConfiguredAuthorizerLockRoutes()
        {
        NamedCache cache = getNamedCache("authorized-lock");
        cache.clear();

        assertTrue(cache.lock("key", 0L));
        assertContainsEvent(StorageAccessAuthorizer.REASON_LOCK, "write", "authorized-lock", "key");

        resetAuthorizer();
        assertTrue(cache.unlock("key"));
        assertContainsEvent(StorageAccessAuthorizer.REASON_UNLOCK, "write", "authorized-lock", "key");

        cache.destroy();
        }

    @Test
    public void shouldRejectLockBeforeStateMutation()
        {
        NamedCache cache = getNamedCache("authorized-deny-lock");
        cache.clear();

        setDeniedReasons(Collections.singleton(StorageAccessAuthorizer.REASON_LOCK));

        try
            {
            cache.lock("key", 0L);
            fail("lock should be rejected by the configured authorizer");
            }
        catch (RuntimeException e)
            {
            assertContainsCause(e, SecurityException.class);
            }

        setDeniedReasons(Collections.emptySet());

        assertTrue(cache.lock("key", 0L));
        assertTrue(cache.unlock("key"));
        cache.destroy();
        }

    @Test
    public void shouldExposeStableReasonStrings()
        {
        assertThat(StorageAccessAuthorizer.REASON_INTERCEPTOR_REMOVE, is(17));
        assertThat(StorageAccessAuthorizer.REASON_LOCK, is(18));
        assertThat(StorageAccessAuthorizer.REASON_UNLOCK, is(19));
        assertThat(StorageAccessAuthorizer.reasonToString(StorageAccessAuthorizer.REASON_LOCK), is("lock"));
        assertThat(StorageAccessAuthorizer.reasonToString(StorageAccessAuthorizer.REASON_UNLOCK), is("unlock"));
        }

    protected List<CapturingAuthorizer.Event> getEvents()
        {
        List<CapturingAuthorizer.Event> listEvents = new ArrayList<>();
        listEvents.addAll(s_member.invoke(new GetEvents()));
        listEvents.addAll(s_memberTwo.invoke(new GetEvents()));
        return listEvents;
        }

    protected void setDeniedReasons(Set<Integer> setReasons)
        {
        s_member.invoke(new SetDeniedReasons(setReasons));
        s_memberTwo.invoke(new SetDeniedReasons(setReasons));
        }

    protected void assertContainsEvent(int nReason, String sOperation, String sCacheName, Object oKey)
        {
        List<CapturingAuthorizer.Event> listEvents = getEvents();
        assertFalse("expected captured authorizer events", listEvents.isEmpty());

        for (CapturingAuthorizer.Event event : listEvents)
            {
            if (event.getReason() == nReason
                    && sOperation.equals(event.getOperation())
                    && sCacheName.equals(event.getCacheName())
                    && (oKey == null || oKey.equals(event.getKey())))
                {
                return;
                }
            }

        fail("missing event reason=" + StorageAccessAuthorizer.reasonToString(nReason)
                + ", operation=" + sOperation + ", cache=" + sCacheName + ", key=" + oKey);
        }

    protected void assertContainsCause(Throwable thrown, Class<?> clzCause)
        {
        for (Throwable cause = thrown; cause != null; cause = cause.getCause())
            {
            if (clzCause.isInstance(cause))
                {
                return;
                }
            }

        fail("missing cause " + clzCause.getName() + " in " + thrown);
        }

    private static void createPeerProofCredentials() throws Exception
        {
        s_credentialDir = Files.createTempDirectory("peer-proof-functional-");
        s_clientStore = s_credentialDir.resolve("client.p12");
        s_serverStore = s_credentialDir.resolve("server.p12");
        s_serverTwoStore = s_credentialDir.resolve("server-two.p12");
        s_serverNewStore = s_credentialDir.resolve("server-new.p12");
        s_serverTwoNewStore = s_credentialDir.resolve("server-two-new.p12");
        s_clientCert = s_credentialDir.resolve("client.pem");
        s_serverCert = s_credentialDir.resolve("server.pem");
        s_serverTwoCert = s_credentialDir.resolve("server-two.pem");
        s_serverNewCert = s_credentialDir.resolve("server-new.pem");
        s_serverTwoNewCert = s_credentialDir.resolve("server-two-new.pem");
        s_authorities = s_credentialDir.resolve("authorities.pem");
        s_memberAuthorities = s_credentialDir.resolve("member-authorities.pem");
        s_memberTwoAuthorities = s_credentialDir.resolve("member-two-authorities.pem");
        s_passwordFile = s_credentialDir.resolve("password.txt");
        s_clientOldStore = s_credentialDir.resolve("client-old.p12");
        s_clientNewStore = s_credentialDir.resolve("client-new.p12");
        s_clientNewCert = s_credentialDir.resolve("client-new.pem");
        s_untrustedStore = s_credentialDir.resolve("untrusted.p12");
        s_untrustedCert = s_credentialDir.resolve("untrusted.pem");
        s_wrongSanStore = s_credentialDir.resolve("wrong-san.p12");
        s_wrongSanCert = s_credentialDir.resolve("wrong-san.pem");
        s_retiredStore = s_credentialDir.resolve("retired.p12");
        s_retiredCert = s_credentialDir.resolve("retired.pem");
        s_optionalStore = s_credentialDir.resolve("optional.p12");
        s_optionalCert = s_credentialDir.resolve("optional.pem");
        s_caStore = s_credentialDir.resolve("ca.p12");
        s_caCert = s_credentialDir.resolve("ca.pem");
        s_noKeyUsageStore = s_credentialDir.resolve("no-key-usage.p12");
        s_noKeyUsageCert = s_credentialDir.resolve("no-key-usage.pem");
        s_falseKeyUsageStore = s_credentialDir.resolve("false-key-usage.p12");
        s_falseKeyUsageCert = s_credentialDir.resolve("false-key-usage.pem");
        s_expiredStore = s_credentialDir.resolve("expired.p12");
        s_expiredCert = s_credentialDir.resolve("expired.pem");
        Files.writeString(s_passwordFile, PEER_PASSWORD);
        createCredential("peer-proof-client", s_clientStore, s_clientCert);
        Files.copy(s_clientStore, s_clientOldStore);
        createCredential("peer-proof-client", s_clientNewStore, s_clientNewCert);
        createCredential("peer-proof-server", s_serverStore, s_serverCert);
        createCredential("peer-proof-server-two", s_serverTwoStore, s_serverTwoCert);
        createCredential("peer-proof-server", s_serverNewStore, s_serverNewCert);
        createCredential("peer-proof-server-two", s_serverTwoNewStore, s_serverTwoNewCert);
        createCredential("peer-proof-untrusted", s_untrustedStore, s_untrustedCert);
        createCredential("not-the-client", s_wrongSanStore, s_wrongSanCert);
        createCredential("peer-proof-client", s_retiredStore, s_retiredCert);
        createCredential("peer-proof-optional", s_optionalStore, s_optionalCert);
        createCredential("peer-proof-ca", s_caStore, s_caCert, "BC=ca:true", "KU=keyCertSign");
        createCredential("peer-proof-no-ku", s_noKeyUsageStore, s_noKeyUsageCert, new String[0]);
        createCredential("peer-proof-false-ku", s_falseKeyUsageStore, s_falseKeyUsageCert,
                "KU=keyEncipherment");
        createExpiredCredential("peer-proof-expired", s_expiredStore, s_expiredCert);
        writeAuthorities(baselineAuthorities());
        }

    private static void createCredential(String sMember, Path store, Path cert) throws Exception
        {
        createCredential(sMember, store, cert, "KU=digitalSignature");
        }

    private static void createCredential(String sMember, Path store, Path cert, String... extensions) throws Exception
        {
        List<String> args = new ArrayList<>(Arrays.asList("-genkeypair", "-alias", "peer-signing",
                "-keyalg", "RSA", "-keysize", "2048", "-validity", "365", "-dname", "CN=" + sMember,
                "-ext", "SAN=uri:" + X509PeerProofProvider.issuerId(sMember)));
        for (String extension : extensions)
            {
            args.add("-ext");
            args.add(extension);
            }
        args.addAll(Arrays.asList("-keystore", store.toString(), "-storetype", "PKCS12",
                "-storepass", PEER_PASSWORD, "-keypass", PEER_PASSWORD, "-noprompt"));
        keytool(args.toArray(new String[0]));
        keytool("-exportcert", "-rfc", "-alias", "peer-signing", "-keystore", store.toString(),
                "-storetype", "PKCS12", "-storepass", PEER_PASSWORD, "-file", cert.toString());
        }

    private static void createExpiredCredential(String sMember, Path store, Path cert) throws Exception
        {
        keytool("-genkeypair", "-alias", "peer-signing", "-keyalg", "RSA", "-keysize", "2048",
                "-startdate", "2020/01/01 00:00:00", "-validity", "1", "-dname", "CN=" + sMember,
                "-ext", "SAN=uri:" + X509PeerProofProvider.issuerId(sMember),
                "-ext", "KU=digitalSignature", "-keystore", store.toString(), "-storetype", "PKCS12",
                "-storepass", PEER_PASSWORD, "-keypass", PEER_PASSWORD, "-noprompt");
        keytool("-exportcert", "-rfc", "-alias", "peer-signing", "-keystore", store.toString(),
                "-storetype", "PKCS12", "-storepass", PEER_PASSWORD, "-file", cert.toString());
        }

    private static void createShortLivedCredential(String sMember, Path store, Path cert, String startDate) throws Exception
        {
        keytool("-genkeypair", "-alias", "peer-signing", "-keyalg", "RSA", "-keysize", "2048",
                "-startdate", startDate, "-validity", "1", "-dname", "CN=" + sMember,
                "-ext", "SAN=uri:" + X509PeerProofProvider.issuerId(sMember),
                "-ext", "KU=digitalSignature", "-keystore", store.toString(), "-storetype", "PKCS12",
                "-storepass", PEER_PASSWORD, "-keypass", PEER_PASSWORD, "-noprompt");
        keytool("-exportcert", "-rfc", "-alias", "peer-signing", "-keystore", store.toString(),
                "-storetype", "PKCS12", "-storepass", PEER_PASSWORD, "-file", cert.toString());
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

    // ----- inner class: ResetAuthorizer ------------------------------------

    public static class EnsureInvocationService
            implements RemoteCallable<String>
        {
        public EnsureInvocationService(String name)
            {
            f_name = name;
            }

        @Override
        public String call()
            {
            com.tangosol.net.Service service = CacheFactory.ensureCluster().ensureService(
                    f_name, InvocationService.TYPE_DEFAULT);
            service.start();
            return service.getInfo().getOldestMember().getMemberName();
            }

        private final String f_name;
        }

    public static class StopGridService
            implements RemoteCallable<Void>
        {
        public StopGridService(String name)
            {
            f_name = name;
            }

        @Override
        public Void call()
            {
            com.tangosol.net.Service service = CacheFactory.ensureCluster().getService(f_name);
            if (service != null)
                {
                service.stop();
                }
            return null;
            }

        private final String f_name;
        }

    public static class ServiceSenior
            implements RemoteCallable<String>
        {
        public ServiceSenior(String name)
            {
            f_name = name;
            }

        @Override
        public String call()
            {
            return CacheFactory.ensureCluster().getServiceInfo(f_name).getOldestMember().getMemberName();
            }

        private final String f_name;
        }

    public static class ServiceMemberCount
            implements RemoteCallable<Integer>
        {
        public ServiceMemberCount(String name)
            {
            f_name = name;
            }

        @Override
        public Integer call()
            {
            return CacheFactory.ensureCluster().getServiceInfo(f_name).getServiceMembers().size();
            }

        private final String f_name;
        }

    public static class SetOwnershipConfig
            implements RemoteCallable<Integer>
        {
        public SetOwnershipConfig(int status)
            {
            f_nStatus = status;
            }

        @Override
        public Integer call()
            {
            NamedCache cache = CacheFactory.getCache("authorized-peer-proof-readiness");
            SafeService safe = (SafeService) cache.getCacheService();
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService service =
                    (com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService)
                            safe.getRunningService();
            service.getThisMemberConfigMap().put("ownership-enabled", Integer.valueOf(f_nStatus));
            return service.getOwnershipEnabledMembers().size();
            }

        private final int f_nStatus;
        }

    public static class ResetAuthorizer
            implements RemoteCallable<Void>
        {
        @Override
        public Void call()
            {
            CapturingAuthorizer.clear();
            return null;
            }
        }

    // ----- inner class: SetDeniedReasons -----------------------------------

    public static class SetDeniedReasons
            implements RemoteCallable<Void>
        {
        public SetDeniedReasons(Set<Integer> setReasons)
            {
            m_setReasons = setReasons;
            }

        @Override
        public Void call()
            {
            CapturingAuthorizer.setDeniedReasons(m_setReasons);
            return null;
            }

        private final Set<Integer> m_setReasons;
        }

    // ----- inner class: GetEvents ------------------------------------------

    public static class GetEvents
            implements RemoteCallable<List<CapturingAuthorizer.Event>>
        {
        @Override
        public List<CapturingAuthorizer.Event> call()
            {
            return CapturingAuthorizer.getEvents();
            }
        }

    public static class RefreshPeerProofProvider
            implements RemoteCallable<Boolean>
        {
        @Override
        public Boolean call()
            {
            PeerProofProvider provider = Security.getPeerProofProvider();
            return provider != null && provider.refresh();
            }
        }

    public static class IsolatedProofPut
            implements RemoteCallable<String>
        {
        public IsolatedProofPut(String cacheName, String key, String value)
            {
            f_cacheName = cacheName;
            f_key = key;
            f_value = value;
            }

        @Override
        public String call()
            {
            Subject subject = new Subject();
            subject.getPrincipals().add((Principal) () -> "peer-proof-isolated-user");
            return Subject.doAs(subject, (PrivilegedAction<String>) () ->
                {
                NamedCache<String, String> cache = CacheFactory.getCache(f_cacheName);
                cache.put(f_key, f_value);
                return cache.get(f_key);
                });
            }

        private final String f_cacheName;
        private final String f_key;
        private final String f_value;
        }

    public static class IsolatedProofGet
            implements RemoteCallable<String>
        {
        public IsolatedProofGet(String cacheName, String key)
            {
            f_cacheName = cacheName;
            f_key = key;
            }

        @Override
        public String call()
            {
            return (String) CacheFactory.getCache(f_cacheName).get(f_key);
            }

        private final String f_cacheName;
        private final String f_key;
        }

    public static class GetPeerProofStatus
            implements RemoteCallable<String>
        {
        public GetPeerProofStatus(Role role)
            {
            f_role = role;
            }

        @Override
        public String call()
            {
            PeerProofProvider provider = Security.getPeerProofProvider();
            if (provider == null)
                {
                return "unavailable";
                }
            switch (f_role)
                {
                case SUBJECT:
                    return provider.getSubjectReadinessStatus();
                case SENIOR:
                    return provider.getSeniorReadinessStatus();
                case CLUSTER:
                    return provider.getClusterReadinessStatus();
                default:
                    return provider.getReadinessStatus();
                }
            }

        private final Role f_role;
        }

    public static class IsPeerProofReady
            implements RemoteCallable<Boolean>
        {
        @Override
        public Boolean call()
            {
            PeerProofProvider provider = Security.getPeerProofProvider();
            return provider != null && provider.isReady();
            }
        }

    public static class InstallPeerProofFault
            implements RemoteCallable<Void>
        {
        public InstallPeerProofFault(ProofFault fault)
            {
            f_fault = fault;
            }

        @Override
        public Void call()
            {
            installPeerProofProvider(new FaultingPeerProofProvider(currentPeerProofProvider(), f_fault));
            return null;
            }

        private final ProofFault f_fault;
        }

    public static class RestorePeerProofProvider
            implements RemoteCallable<Void>
        {
        @Override
        public Void call()
            {
            restorePeerProofProvider();
            return null;
            }
        }

    public static class InstallReadinessBarrier
            implements RemoteCallable<Void>
        {
        public InstallReadinessBarrier(String entered, String release)
            {
            f_sEntered = entered;
            f_sRelease = release;
            }

        @Override
        public Void call()
            {
            installPeerProofProvider(new ReadinessBarrierPeerProofProvider(currentPeerProofProvider(),
                    Path.of(f_sEntered), Path.of(f_sRelease)));
            return null;
            }

        private final String f_sEntered;
        private final String f_sRelease;
        }

    public static class ResetSeniorProofCounts
            implements RemoteCallable<Void>
        {
        @Override
        public Void call()
            {
            s_cSeniorProofValid.set(0);
            s_cSeniorProofInvalid.set(0);
            return null;
            }
        }

    public static class GetSeniorProofCount
            implements RemoteCallable<Integer>
        {
        public GetSeniorProofCount(boolean fValid)
            {
            f_fValid = fValid;
            }

        @Override
        public Integer call()
            {
            return (f_fValid ? s_cSeniorProofValid : s_cSeniorProofInvalid).get();
            }

        private final boolean f_fValid;
        }

    public static class SendJuniorPanic
            implements RemoteCallable<Void>
        {
        public SendJuniorPanic(String culpritName)
            {
            f_culpritName = culpritName;
            }

        @Override
        public Void call()
            {
            ClusterService service = ((SafeCluster) CacheFactory.getCluster()).getRunningCluster().getClusterService();
            MasterMemberSet members = service.getClusterMemberSet();
            com.tangosol.coherence.component.net.Member culprit = findMember(members, f_culpritName);
            ClusterService.SeniorMemberPanic panic = (ClusterService.SeniorMemberPanic)
                    service.instantiateMessage("SeniorMemberPanic");
            panic.setCulpritMember(culprit);
            panic.setZombie(false);
            panic.addToMember(members.getOldestMember());
            service.send(panic);
            return null;
            }

        private final String f_culpritName;
        }

    public static class SendSeniorPanic
            implements RemoteCallable<Void>
        {
        public SendSeniorPanic(String targetName)
            {
            f_targetName = targetName;
            }

        @Override
        public Void call()
            {
            ClusterService service = ((SafeCluster) CacheFactory.getCluster()).getRunningCluster().getClusterService();
            MasterMemberSet members = service.getClusterMemberSet();
            com.tangosol.coherence.component.net.Member target = findMember(members, f_targetName);
            ClusterService.SeniorMemberPanic panic = (ClusterService.SeniorMemberPanic)
                    service.instantiateMessage("SeniorMemberPanic");
            panic.setCulpritMember(members.getOldestMember());
            panic.setZombie(false);
            panic.addToMember(target);
            service.send(panic);
            return null;
            }

        private final String f_targetName;
        }

    private static com.tangosol.coherence.component.net.Member findMember(MasterMemberSet members, String name)
        {
        for (java.util.Iterator iterator = members.iterator(); iterator.hasNext(); )
            {
            com.tangosol.coherence.component.net.Member member =
                    (com.tangosol.coherence.component.net.Member) iterator.next();
            if (name.equals(member.getMemberName()))
                {
                return member;
                }
            }
        throw new IllegalStateException("member unavailable");
        }

    public enum Role
        {
        GENERAL, SUBJECT, SENIOR, CLUSTER
        }

    private enum ProofFault
        {
        MISSING,
        MALFORMED,
        UNTRUSTED,
        WRONG_SAN,
        WRONG_ISSUER,
        WRONG_KEY_ID,
        WRONG_SCOPE,
        WRONG_PRINCIPAL,
        WRONG_SENDER,
        WRONG_SIGNATURE,
        EXPIRED,
        FUTURE,
        OVERLONG,
        OVERFLOW,
        REPLAY,
        RETIRED_KEY,
        ROTATED_OLD_KEY,
        DUPLICATE,
        SENIOR_SIGNATURE,
        SENIOR_CLAIM,
        OBSERVE_SENIOR
        }

    private static class FaultingPeerProofProvider
            implements PeerProofProvider
        {
        FaultingPeerProofProvider(PeerProofProvider delegate, ProofFault fault)
            {
            f_delegate = delegate;
            f_fault = fault;
            }

        @Override
        public byte[] createProof(SubjectProofPayload payload)
            {
            try
                {
                if (f_fault == null || f_fault == ProofFault.DUPLICATE || f_fault == ProofFault.SENIOR_SIGNATURE
                        || f_fault == ProofFault.OBSERVE_SENIOR)
                    {
                    return f_delegate.createProof(payload);
                    }
                if (f_fault == ProofFault.MISSING)
                    {
                    return null;
                    }
                if (f_fault == ProofFault.MALFORMED)
                    {
                    return new byte[] {1, 2, 3};
                    }
                if (f_fault == ProofFault.WRONG_SIGNATURE)
                    {
                    byte[] proof = f_delegate.createProof(payload);
                    proof[proof.length - 1] ^= 1;
                    return proof;
                    }

                Path store = f_fault == ProofFault.UNTRUSTED ? s_untrustedStore
                        : f_fault == ProofFault.WRONG_SAN ? s_wrongSanStore
                        : f_fault == ProofFault.RETIRED_KEY ? s_retiredStore
                        : f_fault == ProofFault.ROTATED_OLD_KEY ? s_clientOldStore : s_clientStore;
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                try (java.io.InputStream in = Files.newInputStream(store))
                    {
                    keyStore.load(in, PEER_PASSWORD.toCharArray());
                    }
                PrivateKey key = (PrivateKey) keyStore.getKey("peer-signing", PEER_PASSWORD.toCharArray());
                X509Certificate cert = (X509Certificate) keyStore.getCertificate("peer-signing");

                long issued = payload.getIssuedAtMillis();
                long expires = payload.getExpiresAtMillis();
                String keyId = fingerprint(cert);
                String issuer = payload.getIssuerId();
                String cache = payload.getCacheName();
                long replay = payload.getReplayEpoch();
                List<String> principals = payload.getPrincipalNames();

                switch (f_fault)
                    {
                    case WRONG_KEY_ID:
                        keyId = "x509-sha256:0000000000000000000000000000000000000000000000000000000000000000";
                        break;
                    case WRONG_ISSUER:
                    case WRONG_SENDER:
                        issuer = X509PeerProofProvider.issuerId("not-the-sender");
                        break;
                    case WRONG_SCOPE:
                        cache += "-wrong";
                        break;
                    case WRONG_PRINCIPAL:
                        principals = Collections.singletonList("not-the-principal");
                        break;
                    case EXPIRED:
                        expires = System.currentTimeMillis() - X509PeerProofProvider.CLOCK_SKEW_MILLIS - 1L;
                        issued = expires - 60_000L;
                        break;
                    case FUTURE:
                        issued = System.currentTimeMillis() + X509PeerProofProvider.CLOCK_SKEW_MILLIS + 60_000L;
                        expires = issued + 60_000L;
                        break;
                    case OVERLONG:
                        expires = issued + X509PeerProofProvider.MAX_PROOF_VALIDITY_MILLIS + 1L;
                        break;
                    case OVERFLOW:
                        issued = Long.MIN_VALUE;
                        expires = Long.MAX_VALUE;
                        break;
                    case REPLAY:
                        replay++;
                        break;
                    default:
                        break;
                    }

                SubjectProofPayload changed = new SubjectProofPayload(payload.getProofVersion(),
                        payload.getAlgorithmId(), keyId, issuer, payload.getSubjectSource(), payload.getClusterName(),
                        payload.getServiceType(), payload.getServiceName(), cache, payload.getRequestSuid(), replay,
                        payload.getNonce(), issued, expires, principals);
                Signature signature = Signature.getInstance(X509PeerProofProvider.JCA_SIGNATURE);
                signature.initSign(key);
                signature.update(changed.toByteArray());
                return new SubjectProof(changed, signature.sign()).toByteArray();
                }
            catch (Exception e)
                {
                throw new IllegalStateException("unable to construct functional proof fault", e);
                }
            }

        private static String fingerprint(X509Certificate cert) throws Exception
            {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(cert.getEncoded());
            StringBuilder sb = new StringBuilder(X509PeerProofProvider.KEY_ID_PREFIX);
            for (byte b : digest)
                {
                sb.append(String.format("%02x", b & 0xff));
                }
            return sb.toString();
            }

        @Override public SubjectProofVerification verifyProof(byte[] proof, SubjectProofPayload expected, long now)
            {return f_delegate.verifyProof(proof, expected, now);}
        @Override public byte[] createProof(SeniorMetadataProofPayload payload)
            {
            try
                {
                byte[] proof = f_delegate.createProof(payload);
                if (f_fault == ProofFault.SENIOR_SIGNATURE && proof != null && proof.length > 0)
                    {
                    proof[proof.length - 1] ^= 1;
                    }
                if (f_fault == ProofFault.SENIOR_CLAIM)
                    {
                    KeyStore keyStore = KeyStore.getInstance("PKCS12");
                    Path identity = Path.of(System.getProperty("peer.proof.identity.store"));
                    try (java.io.InputStream in = Files.newInputStream(identity))
                        {
                        keyStore.load(in, PEER_PASSWORD.toCharArray());
                        }
                    PrivateKey key = (PrivateKey) keyStore.getKey("peer-signing", PEER_PASSWORD.toCharArray());
                    SeniorMetadataProofPayload changed = new SeniorMetadataProofPayload(payload.getPayloadVersion(),
                            payload.getMessageKind(), payload.getAlgorithmId(), payload.getKeyId(),
                            X509PeerProofProvider.issuerId("peer-proof-client"), payload.getClusterName(),
                            payload.getServiceName(), payload.getServiceId(), payload.getMessageType(),
                            X509PeerProofProvider.issuerId("peer-proof-client"), payload.getSeniorId(),
                            payload.getTargetId(), payload.getCulpritId(), payload.getKillDirection(),
                            payload.isZombie(), payload.getSeniorEpoch(), payload.getLastJoinTime(),
                            payload.getMemberSetDigestVersion(), payload.getMemberSetCount(),
                            payload.getMemberSetDigest(), payload.getReplayEpoch(), payload.getNonce(),
                            payload.getIssuedAtMillis(), payload.getExpiresAtMillis());
                    Signature signature = Signature.getInstance(X509PeerProofProvider.JCA_SIGNATURE);
                    signature.initSign(key);
                    signature.update(changed.toByteArray());
                    proof = new SeniorMetadataProof(changed, signature.sign()).toByteArray();
                    }
                return proof;
                }
            catch (Exception e)
                {
                throw new IllegalStateException("unable to construct senior proof fault", e);
                }
            }
        @Override public SeniorMetadataProofVerification verifyProof(byte[] proof, SeniorMetadataProofPayload expected,
                long now)
            {
            SeniorMetadataProofVerification result = f_delegate.verifyProof(proof, expected, now);
            if (f_fault == ProofFault.OBSERVE_SENIOR)
                {
                (result.isValid() ? s_cSeniorProofValid : s_cSeniorProofInvalid).incrementAndGet();
                }
            return result;
            }
        @Override public boolean isSeniorMetadataProofAuthority(SeniorMetadataProofPayload payload)
            {return f_delegate.isSeniorMetadataProofAuthority(payload);}
        @Override public String getProviderId() {return f_delegate.getProviderId();}
        @Override public String getAlgorithmId() {return f_delegate.getAlgorithmId();}
        @Override public String getKeyId() {return f_delegate.getKeyId();}
        @Override public String getIssuerId() {return f_delegate.getIssuerId();}
        @Override public void setLocalMemberName(String name) {f_delegate.setLocalMemberName(name);}
        @Override public void observeMemberNames(java.util.Collection<String> names)
            {
            if (f_fault == ProofFault.DUPLICATE)
                {
                f_delegate.observeMemberNames(Arrays.asList("canonical-duplicate", "canonical-duplicate"));
                }
            else
                {
                f_delegate.observeMemberNames(names);
                }
            }
        @Override public void observeSeniorCapabilities(int members, int compatible, int pending)
            {f_delegate.observeSeniorCapabilities(members, compatible, pending);}
        @Override public boolean isReady() {return f_delegate.isReady();}
        @Override public String getReadinessStatus() {return f_delegate.getReadinessStatus();}
        @Override public String getSubjectReadinessStatus() {return f_delegate.getSubjectReadinessStatus();}
        @Override public String getSeniorReadinessStatus() {return f_delegate.getSeniorReadinessStatus();}
        @Override public String getClusterReadinessStatus() {return f_delegate.getClusterReadinessStatus();}
        @Override public PeerProofReadiness.Observation getLocalReadinessObservation()
            {return f_delegate.getLocalReadinessObservation();}
        @Override public boolean refresh() {return f_delegate.refresh();}
        @Override public void close() {}

        private final PeerProofProvider f_delegate;
        private final ProofFault f_fault;
        }

    private static class ReadinessBarrierPeerProofProvider
            extends FaultingPeerProofProvider
        {
        ReadinessBarrierPeerProofProvider(PeerProofProvider delegate, Path entered, Path release)
            {
            super(delegate, null);
            f_delegateBarrier = delegate;
            f_pathEntered = entered;
            f_pathRelease = release;
            }

        @Override
        public PeerProofReadiness.Observation getLocalReadinessObservation()
            {
            try
                {
                Files.writeString(f_pathEntered, "entered");
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30L);
                while (!Files.exists(f_pathRelease) && System.nanoTime() < deadline)
                    {
                    try {Thread.sleep(10L);} catch (InterruptedException ignored) {}
                    }
                }
            catch (Exception e)
                {
                throw new IllegalStateException("readiness barrier failed", e);
                }
            return f_delegateBarrier.getLocalReadinessObservation();
            }

        private final PeerProofProvider f_delegateBarrier;
        private final Path f_pathEntered;
        private final Path f_pathRelease;
        }

    // ----- constants -------------------------------------------------------

    private static final String FILE_CFG_CACHE = "storage-authorizer-cache-config.xml";
    private static final String FILE_CFG_TOPOLOGY_ONLY = "peer-proof-topology-only-cache-config.xml";

    private static final String FILE_CFG_OVERRIDE = "storage-authorizer-override.xml";

    // ----- data members ---------------------------------------------------

    private static CoherenceClusterMember s_member;
    private static CoherenceClusterMember s_memberTwo;

    private static final int PERFORMANCE_RUNS = 3;
    private static final int PERFORMANCE_CONCURRENCY = 4;
    private static final int PERFORMANCE_WARMUP_OPERATIONS = 400;
    private static final int PERFORMANCE_OPERATIONS = 1_200;

    private static final String PEER_PASSWORD = "changeit";
    private static Path s_credentialDir;
    private static Path s_clientStore;
    private static Path s_serverStore;
    private static Path s_serverTwoStore;
    private static Path s_serverNewStore;
    private static Path s_serverTwoNewStore;
    private static Path s_clientCert;
    private static Path s_serverCert;
    private static Path s_serverTwoCert;
    private static Path s_serverNewCert;
    private static Path s_serverTwoNewCert;
    private static Path s_authorities;
    private static Path s_memberAuthorities;
    private static Path s_memberTwoAuthorities;
    private static Path s_passwordFile;
    private static Path s_clientOldStore;
    private static Path s_clientNewStore;
    private static Path s_clientNewCert;
    private static Path s_untrustedStore;
    private static Path s_untrustedCert;
    private static Path s_wrongSanStore;
    private static Path s_wrongSanCert;
    private static Path s_retiredStore;
    private static Path s_retiredCert;
    private static Path s_optionalStore;
    private static Path s_optionalCert;
    private static Path s_caStore;
    private static Path s_caCert;
    private static Path s_noKeyUsageStore;
    private static Path s_noKeyUsageCert;
    private static Path s_falseKeyUsageStore;
    private static Path s_falseKeyUsageCert;
    private static Path s_expiredStore;
    private static Path s_expiredCert;
    private static PeerProofProvider s_originalPeerProofProvider;
    private static final java.util.concurrent.atomic.AtomicInteger s_cSeniorProofValid =
            new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.concurrent.atomic.AtomicInteger s_cSeniorProofInvalid =
            new java.util.concurrent.atomic.AtomicInteger();
    }
