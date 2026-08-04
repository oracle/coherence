/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Message;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.NamedCacheProtocol;
import com.oracle.coherence.grpc.client.common.AsyncNamedCacheClient;
import com.oracle.coherence.grpc.client.common.BaseGrpcClient;
import com.oracle.coherence.grpc.client.common.StreamStreamObserver;
import com.oracle.coherence.grpc.messages.cache.v1.NamedCacheRequestType;
import com.oracle.coherence.grpc.messages.cache.v1.NamedCacheResponse;
import com.oracle.coherence.grpc.messages.cache.v1.QueryRequest;

import com.tangosol.internal.util.CoherenceMode;

import com.tangosol.net.NamedCache;

import com.tangosol.util.Filter;

import io.grpc.stub.StreamObserver;

import org.junit.jupiter.api.extension.RegisterExtension;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * gRPC v1 cache data-plane executable policy integration coverage.
 *
 * @author Aleks Seovic  2026.05.12
 * @since 26.07
 */
class GrpcV1EnforcementIntegrationTest
        extends AbstractGrpcEnforcementIntegrationTest
    {
    @Override
    protected ServerHelper serverHelper()
        {
        return s_serverHelper;
        }

    @Override
    protected Collection<Integer> values(NamedCache<String, Integer> cache, Filter<Integer> filter,
            Comparator<Object> comparator)
        {
        ByteString filterBytes     = BinaryHelper.toByteString(filter, SERIALIZER);
        ByteString comparatorBytes = BinaryHelper.toByteString(comparator, SERIALIZER);
        QueryRequest request       = QueryRequest.newBuilder()
                .setFilter(filterBytes)
                .setComparator(comparatorBytes)
                .build();
        StreamStreamObserver<NamedCacheResponse> observer = new StreamStreamObserver<>();

        poll(cache, NamedCacheRequestType.QueryValues, request, observer);
        try
            {
            List<NamedCacheResponse> responses = observer.future().get(1, TimeUnit.MINUTES);
            Collection<Integer>      values    = new ArrayList<>();

            for (NamedCacheResponse response : responses)
                {
                BytesValue value = response.getMessage().unpack(BytesValue.class);
                values.add(fromBytesValue(value));
                }
            return values;
            }
        catch (ExecutionException e)
            {
            throw new RuntimeException(e.getCause());
            }
        catch (TimeoutException | InterruptedException | com.google.protobuf.InvalidProtocolBufferException e)
            {
            throw new RuntimeException(e);
            }
        }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void poll(NamedCache<String, Integer> cache, NamedCacheRequestType type, Message message,
            StreamObserver<NamedCacheResponse> observer)
        {
        try
            {
            AsyncNamedCacheClient<?, ?> asyncClient =
                    ((com.oracle.coherence.grpc.client.common.NamedCacheClient) cache).getAsyncClient();
            Field field = BaseGrpcClient.class.getDeclaredField("f_client");
            field.setAccessible(true);
            Object protocol = field.get(asyncClient);
            Method method = protocol.getClass()
                    .getDeclaredMethod("poll", NamedCacheRequestType.class, Message.class, StreamObserver.class);
            method.setAccessible(true);
            method.invoke(protocol, type, message, observer);
            }
        catch (InvocationTargetException e)
            {
            throw new RuntimeException(e.getCause());
            }
        catch (ReflectiveOperationException e)
            {
            throw new RuntimeException(e);
            }
        }

    @RegisterExtension
    static ServerHelper s_serverHelper = new ServerHelper()
            .setProtocolVersion(NamedCacheProtocol.VERSION)
            .setProperty("coherence.ttl", "0")
            .setProperty("coherence.wka", "127.0.0.1")
            .setProperty("coherence.localhost", "127.0.0.1")
            .setProperty("coherence.cluster", "GrpcV1EnforcementIntegrationTest-" + System.nanoTime())
            .setProperty("coherence.override", "coherence-json-override.xml")
            .setProperty("coherence.mode", "prod")
            .setProperty(CoherenceMode.PROP_SECURITY_MODE, CoherenceMode.SECURITY_MODE_HARDENED)
            .setProperty("coherence.grpc.error-disclosure", "diagnostic")
            .setProperty("coherence.grpc.serializer.allowlist", "java")
            .setProperty("coherence.cacheconfig", "coherence-config.xml");
    }
