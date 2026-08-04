/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package netty.grpc.proxy;

import com.google.protobuf.Any;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Int32Value;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.GrpcAuthentication;
import com.oracle.coherence.grpc.NamedCacheProtocol;
import com.oracle.coherence.grpc.messages.cache.v1.EnsureCacheRequest;
import com.oracle.coherence.grpc.messages.cache.v1.NamedCacheRequest;
import com.oracle.coherence.grpc.messages.cache.v1.NamedCacheRequestType;
import com.oracle.coherence.grpc.messages.cache.v1.NamedCacheResponse;
import com.oracle.coherence.grpc.messages.cache.v1.PutRequest;
import com.oracle.coherence.grpc.messages.proxy.v1.InitRequest;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyRequest;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyResponse;
import com.oracle.coherence.grpc.services.cache.v0.NamedCacheServiceGrpc;
import com.oracle.coherence.grpc.services.proxy.v1.ProxyServiceGrpc;
import com.oracle.coherence.grpc.v0.Requests;
import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.net.BackingMapContext;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;
import com.tangosol.net.Session;
import com.tangosol.net.grpc.GrpcDependencies;
import com.tangosol.net.security.IdentityAsserter;
import com.tangosol.net.security.StorageAccessAuthorizer;
import com.tangosol.net.security.UsernameAndPassword;
import com.tangosol.util.BinaryEntry;

import grpc.proxy.FindGrpcProxyPort;
import grpc.proxy.TestStreamObserver;
import grpc.proxy.version_1.TestProxyResponseStreamObserver;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ManagedChannelProvider;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.opentest4j.TestAbortedException;

import java.io.Serializable;

import java.nio.charset.StandardCharsets;

import java.security.Principal;

import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.security.auth.Subject;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live Netty gRPC authentication tests.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.07
 */
class GrpcAuthenticationIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setup() throws Exception
        {
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.cluster",
                System.getProperty("coherence.cluster", "GrpcAuthenticationIT-" + System.currentTimeMillis()));
        System.setProperty("coherence.cacheconfig", "grpc-auth-cache-config.xml");
        System.setProperty("coherence.pof.config", "test-pof-config.xml");
        System.setProperty("coherence.override", "grpc-auth-coherence-override.xml");
        System.setProperty("coherence.serializer", "pof");
        CoherenceModeHelper.restore("prod");
        CoherenceModeHelper.restoreSecurityMode("compatibility");
        System.setProperty(GrpcDependencies.PROP_ENABLED, "true");
        System.setProperty(GrpcAuthentication.PROP_GRPC_AUTH_METHOD, GrpcAuthentication.AUTH_METHOD_BASIC);

        Coherence coherence = Coherence.clusterMember().start().get(5, TimeUnit.MINUTES);
        s_session = coherence.getSession();

        int nPort = FindGrpcProxyPort.local();
        try
            {
            s_channel = ManagedChannelBuilder
                    .forAddress("127.0.0.1", nPort)
                    .usePlaintext()
                    .build();
            }
        catch (ManagedChannelProvider.ProviderNotFoundException e)
            {
            throw new TestAbortedException("Test aborted, cannot load gRPC client", e);
            }
        }

    @AfterAll
    static void cleanup()
        {
        if (s_channel != null)
            {
            s_channel.shutdownNow();
            }
        Coherence.closeAll();
        System.clearProperty(GrpcAuthentication.PROP_GRPC_AUTH_METHOD);
        CoherenceModeHelper.clear();
        }

    @BeforeEach
    void reset()
        {
        TestIdentityAsserter.reset();
        CapturingAuthorizer.reset();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    void shouldRejectMissingCredentialsOnV0Route()
        {
        StatusRuntimeException error = assertThrows(StatusRuntimeException.class, () ->
                NamedCacheServiceGrpc.newBlockingStub(s_channel)
                        .size(Requests.size(GrpcDependencies.DEFAULT_SCOPE, "grpc-auth-missing")));

        assertUnauthenticated(error);
        assertThat(TestIdentityAsserter.s_token.get(), is((Object) null));
        }

    @Test
    void shouldRejectMalformedCredentialsOnV0Route()
        {
        StatusRuntimeException error = assertThrows(StatusRuntimeException.class, () ->
                v0BlockingStub("Basic not-base64")
                        .size(Requests.size(GrpcDependencies.DEFAULT_SCOPE, "grpc-auth-malformed")));

        assertUnauthenticated(error);
        assertThat(TestIdentityAsserter.s_token.get(), is((Object) null));
        }

    @Test
    void shouldRejectFailedCredentialsOnV0Route()
        {
        StatusRuntimeException error = assertThrows(StatusRuntimeException.class, () ->
                v0BlockingStub(basic("grpc-user", "wrong"))
                        .size(Requests.size(GrpcDependencies.DEFAULT_SCOPE, "grpc-auth-rejected")));

        assertUnauthenticated(error);
        assertThat(TestIdentityAsserter.s_token.get(), is(notNullValue()));
        }

    @Test
    void shouldBindSubjectOnV0RouteWithValidBasicCredentials() throws InterruptedException
        {
        String                     sCacheName = "grpc-auth-v0";
        NamedCache<Object, Object> cache      = s_session.getCache(sCacheName);

        cache.clear();
        CapturingAuthorizer.AuthorizationCapture capture = CapturingAuthorizer.beginCapture(sCacheName,
                CapturingAuthorizer.Hook.WRITE_ANY, StorageAccessAuthorizer.REASON_INVOKE);

        v0BlockingStub(basic("grpc-user", "secret"))
                .put(Requests.put(GrpcDependencies.DEFAULT_SCOPE, sCacheName, "pof",
                        BinaryHelper.toByteString("key", SERIALIZER),
                        BinaryHelper.toByteString("value", SERIALIZER)));

        assertUsernameAndPasswordToken("grpc-user", "secret");
        assertCapturedAuthorization(capture, "grpc-user");
        }

    @Test
    void shouldRejectMissingCredentialsOnV1SubChannelRoute() throws Exception
        {
        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = ProxyServiceGrpc.newStub(s_channel).subChannel(observer);

        channel.onNext(initRequest(1));

        observer.awaitDone(1, TimeUnit.MINUTES)
                .assertError(StatusRuntimeException.class)
                .assertError(t ->
                    {
                    assertUnauthenticated((StatusRuntimeException) t);
                    return true;
                    });
        }

    @Test
    void shouldBindSubjectOnV1SubChannelRouteWithValidBasicCredentials() throws Exception
        {
        String                      sCacheName = "grpc-auth-v1";
        NamedCache<Object, Object>  cache      = s_session.getCache(sCacheName);
        cache.clear();

        TestProxyResponseStreamObserver observer = new TestProxyResponseStreamObserver();
        StreamObserver<ProxyRequest>    channel  = v1Stub(basic("grpc-user", "secret")).subChannel(observer);

        channel.onNext(initRequest(1));
        observer.awaitCount(1, 1, TimeUnit.MINUTES).assertNoErrors();
        assertThat(observer.valueAt(0).getResponseCase(), is(ProxyResponse.ResponseCase.INIT));

        int nCacheId = ensureCache(channel, observer, sCacheName, 2);
        CapturingAuthorizer.AuthorizationCapture capture = CapturingAuthorizer.beginCapture(sCacheName,
                CapturingAuthorizer.Hook.WRITE, StorageAccessAuthorizer.REASON_PUT);
        put(channel, observer, nCacheId, 3);

        assertUsernameAndPasswordToken("grpc-user", "secret");
        assertCapturedAuthorization(capture, "grpc-user");
        }

    @Test
    void shouldNotAllowUnrelatedSubjectToMaskNullSubjectOnV1Put() throws InterruptedException
        {
        String                                    sCacheName = "grpc-auth-v1-negative-control";
        Subject                                   subject    = new Subject();
        CapturingAuthorizer.AuthorizationCapture prior      = CapturingAuthorizer.beginCapture(sCacheName,
                CapturingAuthorizer.Hook.WRITE, StorageAccessAuthorizer.REASON_PUT);

        subject.getPrincipals().add(new TestPrincipal("grpc-user"));
        CapturingAuthorizer.record(sCacheName, CapturingAuthorizer.Hook.WRITE,
                StorageAccessAuthorizer.REASON_PUT, subject);

        CapturingAuthorizer.AuthorizationCapture capture = CapturingAuthorizer.beginCapture(sCacheName,
                CapturingAuthorizer.Hook.WRITE, StorageAccessAuthorizer.REASON_PUT);

        CapturingAuthorizer.record("unrelated-cache", CapturingAuthorizer.Hook.WRITE,
                StorageAccessAuthorizer.REASON_PUT, subject);
        CapturingAuthorizer.record(sCacheName, CapturingAuthorizer.Hook.READ,
                StorageAccessAuthorizer.REASON_PUT, subject);
        CapturingAuthorizer.record(sCacheName, CapturingAuthorizer.Hook.READ_ANY,
                StorageAccessAuthorizer.REASON_PUT, subject);
        CapturingAuthorizer.record(sCacheName, CapturingAuthorizer.Hook.WRITE_ANY,
                StorageAccessAuthorizer.REASON_PUT, subject);
        CapturingAuthorizer.record(sCacheName, CapturingAuthorizer.Hook.WRITE,
                StorageAccessAuthorizer.REASON_CLEAR, subject);
        CapturingAuthorizer.record(sCacheName, CapturingAuthorizer.Hook.WRITE,
                StorageAccessAuthorizer.REASON_PUT, null);
        CapturingAuthorizer.record("another-unrelated-cache", CapturingAuthorizer.Hook.WRITE,
                StorageAccessAuthorizer.REASON_PUT, subject);
        CapturingAuthorizer.record(sCacheName, CapturingAuthorizer.Hook.WRITE,
                StorageAccessAuthorizer.REASON_PUT, subject);

        CapturingAuthorizer.AuthorizationEvent priorEvent = prior.await(1, TimeUnit.SECONDS);
        assertThat(priorEvent, is(notNullValue()));
        assertSubject(priorEvent.f_subject, "grpc-user");
        assertThat(priorEvent.f_lGeneration, not(is(capture.f_lGeneration)));

        CapturingAuthorizer.AuthorizationEvent event = capture.await(1, TimeUnit.SECONDS);
        assertThat(event, is(notNullValue()));
        assertThat(event.f_lGeneration, is(capture.f_lGeneration));
        assertThat(event.f_sCacheName, is(sCacheName));
        assertThat(event.f_hook, is(capture.f_expectedHook));
        assertThat(event.f_nReason, is(capture.f_nExpectedReason));
        assertThat(event.f_subject, is((Subject) null));
        }

    // ----- helper methods -------------------------------------------------

    private static NamedCacheServiceGrpc.NamedCacheServiceBlockingStub v0BlockingStub(String sAuthorization)
        {
        return NamedCacheServiceGrpc.newBlockingStub(s_channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers(sAuthorization)));
        }

    private static ProxyServiceGrpc.ProxyServiceStub v1Stub(String sAuthorization)
        {
        return ProxyServiceGrpc.newStub(s_channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers(sAuthorization)));
        }

    private static Metadata headers(String sAuthorization)
        {
        Metadata headers = new Metadata();
        headers.put(GrpcAuthentication.KEY_AUTHORIZATION, sAuthorization);
        return headers;
        }

    private static String basic(String sUser, String sPassword)
        {
        return "Basic " + Base64.getEncoder().encodeToString((sUser + ':' + sPassword)
                .getBytes(StandardCharsets.UTF_8));
        }

    private static ProxyRequest initRequest(long nId)
        {
        InitRequest initRequest = InitRequest.newBuilder()
                .setProtocol(NamedCacheProtocol.PROTOCOL_NAME)
                .setProtocolVersion(NamedCacheProtocol.VERSION)
                .setFormat("pof")
                .setScope(GrpcDependencies.DEFAULT_SCOPE)
                .build();

        return ProxyRequest.newBuilder()
                .setId(nId)
                .setInit(initRequest)
                .build();
        }

    private static int ensureCache(StreamObserver<ProxyRequest> channel,
            TestProxyResponseStreamObserver observer, String sCacheName, long nId) throws Exception
        {
        EnsureCacheRequest ensureCacheRequest = EnsureCacheRequest.newBuilder()
                .setCache(sCacheName)
                .build();

        NamedCacheRequest request = NamedCacheRequest.newBuilder()
                .setType(NamedCacheRequestType.EnsureCache)
                .setMessage(Any.pack(ensureCacheRequest))
                .build();

        ProxyRequest proxyRequest = ProxyRequest.newBuilder()
                .setId(nId)
                .setMessage(Any.pack(request))
                .build();

        ProxyResponse      proxyResponse = sendAndAwait(channel, observer, proxyRequest);
        NamedCacheResponse response      = proxyResponse.getMessage().unpack(NamedCacheResponse.class);
        assertThat(response.getCacheId(), is(not(0)));
        return response.getCacheId();
        }

    private static void put(StreamObserver<ProxyRequest> channel,
            TestProxyResponseStreamObserver observer, int nCacheId, long nId) throws Exception
        {
        PutRequest putRequest = PutRequest.newBuilder()
                .setKey(BinaryHelper.toByteString("key", SERIALIZER))
                .setValue(BinaryHelper.toByteString("value", SERIALIZER))
                .build();

        NamedCacheRequest request = NamedCacheRequest.newBuilder()
                .setCacheId(nCacheId)
                .setType(NamedCacheRequestType.Put)
                .setMessage(Any.pack(putRequest))
                .build();

        ProxyRequest proxyRequest = ProxyRequest.newBuilder()
                .setId(nId)
                .setMessage(Any.pack(request))
                .build();

        ProxyResponse proxyResponse = sendAndAwait(channel, observer, proxyRequest);
        assertThat(proxyResponse.getMessage().unpack(NamedCacheResponse.class).getMessage()
                .unpack(BytesValue.class), is(notNullValue()));
        }

    private static ProxyResponse sendAndAwait(StreamObserver<ProxyRequest> channel,
            TestProxyResponseStreamObserver observer, ProxyRequest proxyRequest) throws Exception
        {
        int nCount = observer.valueCount();
        channel.onNext(proxyRequest);
        long ldtStop = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
        while (System.currentTimeMillis() < ldtStop)
            {
            observer.awaitCount(nCount + 1, 1, TimeUnit.SECONDS);
            observer.assertNoErrors();
            while (nCount < observer.valueCount())
                {
                ProxyResponse response = observer.valueAt(nCount++);
                if (response.getId() == proxyRequest.getId())
                    {
                    assertThat(response.getResponseCase(), is(ProxyResponse.ResponseCase.MESSAGE));
                    return response;
                    }
                }
            }
        throw new TimeoutException("Timed out waiting for response " + proxyRequest.getId());
        }

    private static void assertUnauthenticated(StatusRuntimeException error)
        {
        assertThat(error.getStatus().getCode(), is(Status.UNAUTHENTICATED.getCode()));
        assertThat(error.getStatus().getDescription(), is("invalid authentication credentials"));
        }

    private static void assertUsernameAndPasswordToken(String sUser, String sPassword)
        {
        Object token = TestIdentityAsserter.s_token.get();
        assertThat(token, is(notNullValue()));
        assertTrue(token instanceof UsernameAndPassword);

        UsernameAndPassword usernameAndPassword = (UsernameAndPassword) token;
        assertThat(usernameAndPassword.getUsername(), is(sUser));
        assertTrue(Arrays.equals(usernameAndPassword.getPassword(), sPassword.toCharArray()));
        }

    private static void assertSubject(Subject subject, String sUser)
        {
        assertThat(subject, is(notNullValue()));
        assertTrue(subject.getPrincipals().stream().anyMatch(principal -> sUser.equals(principal.getName())));
        }

    private static void assertCapturedAuthorization(CapturingAuthorizer.AuthorizationCapture capture, String sUser)
            throws InterruptedException
        {
        CapturingAuthorizer.AuthorizationEvent event = capture.await(30, TimeUnit.SECONDS);
        assertThat("No matching authorization event for cache=" + capture.f_sCacheName
                        + ", hook=" + capture.f_expectedHook
                        + ", reason=" + StorageAccessAuthorizer.reasonToString(capture.f_nExpectedReason)
                        + "; observed " + capture.f_events,
                event, is(notNullValue()));
        assertThat(event.f_lGeneration, is(capture.f_lGeneration));
        assertThat(event.f_sCacheName, is(capture.f_sCacheName));
        assertThat(event.f_hook, is(capture.f_expectedHook));
        assertThat(event.f_nReason, is(capture.f_nExpectedReason));
        assertSubject(event.f_subject, sUser);
        }

    // ----- inner class: TestIdentityAsserter -----------------------------

    public static class TestIdentityAsserter
            implements IdentityAsserter
        {
        @Override
        public Subject assertIdentity(Object token, Service service)
                throws SecurityException
            {
            if (token instanceof Subject)
                {
                return (Subject) token;
                }

            if (!(token instanceof UsernameAndPassword))
                {
                throw new SecurityException("Unexpected token");
                }

            s_token.set(token);
            UsernameAndPassword usernameAndPassword = (UsernameAndPassword) token;
            if (!"grpc-user".equals(usernameAndPassword.getUsername())
                    || !Arrays.equals(usernameAndPassword.getPassword(), "secret".toCharArray()))
                {
                throw new SecurityException("Invalid credentials");
                }

            Subject subject = new Subject();
            subject.getPrincipals().add(new TestPrincipal(usernameAndPassword.getUsername()));
            return subject;
            }

        static void reset()
            {
            s_token.set(null);
            }

        static final AtomicReference<Object> s_token = new AtomicReference<>();
        }

    // ----- inner class: CapturingAuthorizer ------------------------------

    public static class CapturingAuthorizer
            implements StorageAccessAuthorizer
        {
        @Override
        public void checkRead(BinaryEntry entry, Subject subject, int nReason)
            {
            record(entry.getBackingMapContext().getCacheName(), Hook.READ, nReason, subject);
            }

        @Override
        public void checkWrite(BinaryEntry entry, Subject subject, int nReason)
            {
            record(entry.getBackingMapContext().getCacheName(), Hook.WRITE, nReason, subject);
            }

        @Override
        public void checkReadAny(BackingMapContext context, Subject subject, int nReason)
            {
            record(context.getCacheName(), Hook.READ_ANY, nReason, subject);
            }

        @Override
        public void checkWriteAny(BackingMapContext context, Subject subject, int nReason)
            {
            record(context.getCacheName(), Hook.WRITE_ANY, nReason, subject);
            }

        static void reset()
            {
            s_capture.set(null);
            }

        static AuthorizationCapture beginCapture(String sCacheName, Hook expectedHook, int nExpectedReason)
            {
            AuthorizationCapture capture = new AuthorizationCapture(s_generation.incrementAndGet(),
                    sCacheName, expectedHook, nExpectedReason);
            s_capture.set(capture);
            return capture;
            }

        static void record(String sCacheName, Hook hook, int nReason, Subject subject)
            {
            AuthorizationCapture capture = s_capture.get();
            if (capture != null)
                {
                capture.record(sCacheName, hook, nReason, subject);
                }
            }

        enum Hook
            {
            READ,
            WRITE,
            READ_ANY,
            WRITE_ANY
            }

        static class AuthorizationCapture
            {
            AuthorizationCapture(long lGeneration, String sCacheName, Hook expectedHook, int nExpectedReason)
                {
                f_lGeneration     = lGeneration;
                f_sCacheName      = sCacheName;
                f_expectedHook    = expectedHook;
                f_nExpectedReason = nExpectedReason;
                }

            void record(String sCacheName, Hook hook, int nReason, Subject subject)
                {
                AuthorizationEvent event = new AuthorizationEvent(f_lGeneration, sCacheName,
                        hook, nReason, subject);
                f_events.add(event);
                if (f_sCacheName.equals(sCacheName)
                        && hook == f_expectedHook
                        && nReason == f_nExpectedReason)
                    {
                    if (f_event.compareAndSet(null, event))
                        {
                        f_latch.countDown();
                        }
                    }
                }

            AuthorizationEvent await(long cTimeout, TimeUnit unit) throws InterruptedException
                {
                return f_latch.await(cTimeout, unit) ? f_event.get() : null;
                }

            final long f_lGeneration;

            final String f_sCacheName;

            final Hook f_expectedHook;

            final int f_nExpectedReason;

            final CountDownLatch f_latch = new CountDownLatch(1);

            final AtomicReference<AuthorizationEvent> f_event = new AtomicReference<>();

            final ConcurrentLinkedQueue<AuthorizationEvent> f_events = new ConcurrentLinkedQueue<>();
            }

        static class AuthorizationEvent
            {
            AuthorizationEvent(long lGeneration, String sCacheName, Hook hook, int nReason, Subject subject)
                {
                f_lGeneration = lGeneration;
                f_sCacheName  = sCacheName;
                f_hook        = hook;
                f_nReason     = nReason;
                f_subject     = subject;
                }

            @Override
            public String toString()
                {
                return "AuthorizationEvent{" + "generation=" + f_lGeneration
                        + ", cache='" + f_sCacheName + '\''
                        + ", hook=" + f_hook
                        + ", reason=" + StorageAccessAuthorizer.reasonToString(f_nReason)
                        + ", subject=" + f_subject
                        + '}';
                }

            final long f_lGeneration;

            final String f_sCacheName;

            final Hook f_hook;

            final int f_nReason;

            final Subject f_subject;
            }

        static final AtomicLong s_generation = new AtomicLong();

        static final AtomicReference<AuthorizationCapture> s_capture = new AtomicReference<>();
        }

    // ----- inner class: TestPrincipal ------------------------------------

    private static class TestPrincipal
            implements Principal, Serializable
        {
        TestPrincipal(String sName)
            {
            f_sName = sName;
            }

        @Override
        public String getName()
            {
            return f_sName;
            }

        private final String f_sName;
        }

    // ----- constants ------------------------------------------------------

    private static final Serializer SERIALIZER = new ConfigurablePofContext("test-pof-config.xml");

    // ----- data members ---------------------------------------------------

    private static Session s_session;

    private static ManagedChannel s_channel;
    }
