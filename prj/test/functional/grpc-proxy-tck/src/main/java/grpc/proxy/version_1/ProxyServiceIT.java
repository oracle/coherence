/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy.version_1;

import com.google.protobuf.Any;
import com.google.protobuf.BytesValue;
import com.oracle.coherence.grpc.NamedCacheProtocol;
import com.oracle.coherence.grpc.NamedQueueProtocol;
import com.oracle.coherence.grpc.NamedTopicProtocol;
import com.oracle.coherence.grpc.GrpcSerializerPolicy;
import com.oracle.coherence.grpc.messages.common.v1.HeartbeatMessage;
import com.oracle.coherence.grpc.messages.proxy.v1.InitRequest;
import com.oracle.coherence.grpc.messages.proxy.v1.InitResponse;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyRequest;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyResponse;
import com.oracle.coherence.testing.util.CoherenceModeHelper;
import com.tangosol.io.Serializer;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.Member;
import com.tangosol.util.UUID;
import grpc.proxy.TestStreamObserver;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ProxyServiceIT
        extends BaseVersionOneGrpcServiceIT
    {
    @Test
    public void shouldAcceptHeartbeatBeforeInit() throws Exception
        {
        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannelOnNewProxy(observer);

        ProxyRequest request = ProxyRequest.newBuilder()
                .setId(19)
                .setHeartbeat(HeartbeatMessage.newBuilder().setAck(true).build())
                .build();

        channel.onNext(request);

        observer.awaitCount(1, 1, TimeUnit.MINUTES);

        ProxyResponse response = observer.valueAt(0);
        assertThat(response, is(notNullValue()));
        assertThat(response.getId(), is(request.getId()));
        assertThat(response.getResponseCase(), is(ProxyResponse.ResponseCase.HEARTBEAT));
        }

    @Test
    public void shouldNotAcceptMessageBeforeInit() throws Exception
        {
        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannelOnNewProxy(observer);

        channel.onNext(ProxyRequest.newBuilder()
                .setId(1)
                .setMessage(Any.pack(BytesValue.getDefaultInstance()))
                .build());

        observer.awaitDone(1, TimeUnit.MINUTES)
                .assertError(StatusRuntimeException.class)
                .assertError(t -> ((StatusRuntimeException) t).getStatus().getCode() == Status.INTERNAL.getCode());
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldInitialize(String serializerName, Serializer ignored, String sScope) throws Exception
        {
        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannelOnNewProxy(observer);

        InitRequest initRequest = InitRequest.newBuilder()
                .setProtocol(NamedCacheProtocol.PROTOCOL_NAME)
                .setProtocolVersion(NamedCacheProtocol.VERSION)
                .setFormat(serializerName)
                .setScope(sScope)
                .build();

        ProxyRequest request = ProxyRequest.newBuilder()
                .setId(21)
                .setInit(initRequest)
                .build();

        channel.onNext(request);

        observer.awaitCount(1, 1, TimeUnit.MINUTES);

        ProxyResponse response = observer.valueAt(0);
        assertThat(response, is(notNullValue()));
        assertThat(response.getResponseCase(), is(ProxyResponse.ResponseCase.INIT));
        assertThat(response.getId(), is(request.getId()));

        Member member = CacheFactory.getCluster().getLocalMember();

        InitResponse initResponse = response.getInit();
        assertThat(initResponse, is(notNullValue()));
        assertThat(initResponse.getProtocolVersion(), is(NamedCacheProtocol.VERSION));
        assertThat(initResponse.getUuid(), is(notNullValue()));
        assertThat(initResponse.getVersion(), is(CacheFactory.VERSION));
        assertThat(initResponse.getEncodedVersion(), is(CacheFactory.VERSION_ENCODED));
        assertThat(initResponse.getProxyMemberId(), is(member.getId()));
        assertThat(new UUID(initResponse.getProxyMemberUuid().toByteArray()), is(member.getUuid()));
        }

    @ParameterizedTest(name = "{index} protocol={0}")
    @MethodSource("clientSerializerProtocols")
    public void shouldRejectUnsafeClientSerializerInProd(String sProtocol, int nVersion) throws Exception
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.prod())
            {
            assertInitRejected(sProtocol, nVersion, "java", Status.INVALID_ARGUMENT);
            }
        }

    @Test
    public void shouldAllowUnsafeClientSerializerWhenExplicitlyAllowlistedInProd() throws Exception
        {
        String sAllowedOld = System.getProperty(GrpcSerializerPolicy.PROP_ALLOWED_SERIALIZERS);
        System.setProperty(GrpcSerializerPolicy.PROP_ALLOWED_SERIALIZERS, "java");
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.prod())
            {
            assertInitSucceeded(NamedCacheProtocol.PROTOCOL_NAME, NamedCacheProtocol.VERSION, "java");
            }
        finally
            {
            restoreProperty(GrpcSerializerPolicy.PROP_ALLOWED_SERIALIZERS, sAllowedOld);
            CoherenceModeHelper.reset();
            }
        }

    private void assertInitRejected(String sProtocol, int nVersion, String sFormat, Status status) throws Exception
        {
        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannelOnNewProxy(observer);

        channel.onNext(newInitProxyRequest(sProtocol, nVersion, sFormat));

        observer.awaitDone(1, TimeUnit.MINUTES)
                .assertError(StatusRuntimeException.class)
                .assertError(t -> ((StatusRuntimeException) t).getStatus().getCode() == status.getCode());
        }

    private void assertInitSucceeded(String sProtocol, int nVersion, String sFormat) throws Exception
        {
        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannelOnNewProxy(observer);
        ProxyRequest                      request  = newInitProxyRequest(sProtocol, nVersion, sFormat);

        channel.onNext(request);

        observer.awaitCount(1, 1, TimeUnit.MINUTES);

        ProxyResponse response = observer.valueAt(0);
        assertThat(response, is(notNullValue()));
        assertThat(response.getResponseCase(), is(ProxyResponse.ResponseCase.INIT));
        assertThat(response.getId(), is(request.getId()));
        }

    private ProxyRequest newInitProxyRequest(String sProtocol, int nVersion, String sFormat)
        {
        InitRequest initRequest = InitRequest.newBuilder()
                .setProtocol(sProtocol)
                .setProtocolVersion(nVersion)
                .setFormat(sFormat)
                .setScope(Coherence.DEFAULT_SCOPE)
                .build();

        return ProxyRequest.newBuilder()
                .setId(21)
                .setInit(initRequest)
                .build();
        }

    private static Stream<Arguments> clientSerializerProtocols()
        {
        return Stream.of(
                Arguments.of(NamedCacheProtocol.PROTOCOL_NAME, NamedCacheProtocol.VERSION),
                Arguments.of(NamedTopicProtocol.PROTOCOL_NAME, NamedTopicProtocol.VERSION),
                Arguments.of(NamedQueueProtocol.PROTOCOL_NAME, NamedQueueProtocol.VERSION));
        }

    private static void restoreProperty(String sName, String sValue)
        {
        if (sValue == null)
            {
            System.clearProperty(sName);
            }
        else
            {
            System.setProperty(sName, sValue);
            }
        }

    }
