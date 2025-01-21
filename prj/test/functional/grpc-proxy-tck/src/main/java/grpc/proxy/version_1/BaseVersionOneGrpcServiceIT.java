/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy.version_1;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.messages.cache.v1.MapEventMessage;
import com.oracle.coherence.grpc.messages.cache.v1.NamedCacheResponse;
import com.oracle.coherence.grpc.messages.cache.v1.ResponseType;
import com.oracle.coherence.grpc.messages.common.v1.BinaryKeyAndValue;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyRequest;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyResponse;
import com.oracle.coherence.grpc.proxy.common.BaseGrpcAcceptorController;
import com.oracle.coherence.grpc.proxy.common.ProxyServiceGrpcImpl;

import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.GrpcAcceptor;
import com.tangosol.io.Serializer;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Service;
import com.tangosol.net.grpc.GrpcDependencies;
import com.tangosol.util.SimpleMapEntry;
import grpc.proxy.BaseGrpcIT;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class BaseVersionOneGrpcServiceIT
        extends BaseGrpcIT
    {
    // ----- helper methods -------------------------------------------------

    protected StreamObserver<ProxyRequest> openChannelOnNewProxy(StreamObserver<ProxyResponse> observer)
        {
        return newProxyService().subChannel(observer);
        }

    protected StreamObserver<ProxyRequest> openChannel(StreamObserver<ProxyResponse> observer)
        {
        return ensureProxyService().subChannel(observer);
        }


    protected ProxyRequest newRequest(Message message)
        {
        return ProxyRequest.newBuilder()
                .setId(m_messageID.incrementAndGet())
                .setMessage(Any.pack(message))
                .build();
        }

    protected  <K, V> Map<K, V> toMap(List<NamedCacheResponse> list, Serializer serializer) throws Exception
        {
        Map<K, V> map = new HashMap<>();
        for (NamedCacheResponse response : list)
            {
            BinaryKeyAndValue keyAndValue =  response.getMessage().unpack(BinaryKeyAndValue.class);
            map.put(BinaryHelper.fromByteString(keyAndValue.getKey(), serializer),
                    BinaryHelper.fromByteString(keyAndValue.getValue(), serializer));
            }
        return map;
        }


    protected  <K, V> List<Map.Entry<K, V>> toListOfEntries(List<NamedCacheResponse> list, Serializer serializer) throws Exception
        {
        List<Map.Entry<K, V>> listEntry = new ArrayList<>();
        for (NamedCacheResponse response : list)
            {
            BinaryKeyAndValue keyAndValue =  response.getMessage().unpack(BinaryKeyAndValue.class);
            Map.Entry<K, V> entry = new SimpleMapEntry<>(BinaryHelper.fromByteString(keyAndValue.getKey(), serializer),
                    BinaryHelper.fromByteString(keyAndValue.getValue(), serializer));
            listEntry.add(entry);
            }
        return listEntry;
        }

    /**
     * Create an instance of the {@link ProxyServiceGrpcImpl} to use for testing.
     *
     * @return an instance of the {@link ProxyServiceGrpcImpl} to use for testing
     */
    protected ProxyServiceGrpcImpl ensureProxyService()
        {
        ProxyServiceGrpcImpl service = m_service;
        if (service == null)
            {
            service = m_service = newProxyService();
            }
        return service;
        }

    /**
     * Create an instance of the {@link ProxyServiceGrpcImpl} to use for testing.
     *
     * @return an instance of the {@link ProxyServiceGrpcImpl} to use for testing
     */
    protected ProxyServiceGrpcImpl newProxyService()
        {
        try
            {
            Service grpcService = CacheFactory.getCluster().getService(GrpcDependencies.SCOPED_PROXY_SERVICE_NAME);
            if (grpcService instanceof SafeService)
                {
                grpcService = ((SafeService) grpcService).getService();
                }
            ProxyService               proxyService = (ProxyService) grpcService;
            GrpcAcceptor               acceptor     = (GrpcAcceptor) proxyService.getAcceptor();
            BaseGrpcAcceptorController controller   = (BaseGrpcAcceptorController) acceptor.getController();
            return controller.getProxyService();
            }
        catch (Throwable t)
            {
            throw new IllegalStateException("Cannot find a TestProxyServiceProvider instance, are you running this " +
                    "test from the TCK module instead of from one of the specific Netty or Helidon test modules");
            }
        }

    public <T extends Message> T unpackResponse(ProxyResponse response, Class<T> type)
        {
        return unpack(response.getMessage(), type);
        }

    public <T extends Message> T unpack(Any any, Class<T> type)
        {
        try
            {
            return any.unpack(type);
            }
        catch (InvalidProtocolBufferException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    public List<MapEventMessage> eventsFrom(List<ProxyResponse> list)
        {
        return eventsFrom(list, -1L);
        }

    public List<MapEventMessage> eventsFrom(List<ProxyResponse> list, long filterId)
        {
        List<MapEventMessage> listEvent = new ArrayList<>();
        for (ProxyResponse response : list)
            {
            Any any = response.getMessage();
            if (any.is(NamedCacheResponse.class))
                {
                NamedCacheResponse ncr = unpackResponse(response, NamedCacheResponse.class);
                if (ncr.getType() == ResponseType.MapEvent)
                    {
                    MapEventMessage eventMessage = unpack(ncr.getMessage(), MapEventMessage.class);
                    if (filterId < 0 || eventMessage.getFilterIdsList().contains(filterId))
                        {
                        listEvent.add(eventMessage);
                        }
                    }
                }
            }
        return listEvent;
        }

    protected <M extends Message, T extends Message> T unpackAny(M message, Function<M, Any> fn, Class<T> expected)
        {
        assertThat(message, is(notNullValue()));
        try
            {
            return fn.apply(message).unpack(expected);
            }
        catch (Throwable e)
            {
            throw Exceptions.ensureRuntimeException(e,
                    "Failed to unpack proto message: " + e.getMessage()
                            + "\nMessage:\n" + message
                            + "\nExpected:\n" + expected);
            }
        }

    // ----- data members ---------------------------------------------------

    protected static final AtomicLong m_messageID = new AtomicLong();

    private ProxyServiceGrpcImpl m_service;
    }
