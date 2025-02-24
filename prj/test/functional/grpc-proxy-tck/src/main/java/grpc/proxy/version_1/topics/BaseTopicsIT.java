/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy.version_1.topics;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.NamedTopicProtocol;
import com.oracle.coherence.grpc.messages.common.v1.ErrorMessage;
import com.oracle.coherence.grpc.messages.proxy.v1.InitRequest;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyRequest;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyResponse;
import com.oracle.coherence.grpc.messages.topic.v1.EnsureTopicRequest;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceRequest;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceRequestType;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceResponse;
import com.oracle.coherence.grpc.proxy.common.ProxyServiceChannel;
import com.tangosol.io.Serializer;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.RequestIncompleteException;
import com.tangosol.net.topic.NamedTopic;
import grpc.proxy.TestStreamObserver;
import grpc.proxy.version_1.BaseVersionOneGrpcServiceIT;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("unchecked")
public abstract class BaseTopicsIT
        extends BaseVersionOneGrpcServiceIT
    {
    // ----- helper methods -------------------------------------------------

    protected String newTopicName()
        {
        return "test-topic-" + m_topicNameSuffix.incrementAndGet();
        }

    protected <V> NamedTopic<V> ensureTopic(String sScope, String name)
        {
        ExtensibleConfigurableCacheFactory eccf = (ExtensibleConfigurableCacheFactory) ensureCCF(sScope);
        return eccf.ensureTopic(name);
        }

    protected void initTopic(StreamObserver<ProxyRequest> channel, TestStreamObserver<ProxyResponse> observer,
                             Serializer serializer, String sScope) throws Exception
        {
        InitRequest initRequest = InitRequest.newBuilder()
                .setProtocol(NamedTopicProtocol.PROTOCOL_NAME)
                .setProtocolVersion(NamedTopicProtocol.VERSION)
                .setFormat(serializer.getName())
                .setScope(sScope)
                .build();

        ProxyRequest request = ProxyRequest.newBuilder()
                .setId(m_messageID.incrementAndGet())
                .setInit(initRequest)
                .build();

        channel.onNext(request);
        observer.awaitCount(1, 1, TimeUnit.MINUTES);
        observer.assertNoErrors();
        }

    protected int ensureTopic(StreamObserver<ProxyRequest> channel, TestStreamObserver<ProxyResponse> observer,
                              String sTopicName) throws Exception
        {
        EnsureTopicRequest ensureTopicRequest = EnsureTopicRequest.newBuilder()
                .setTopic(sTopicName)
                .build();

        TopicServiceRequest request = TopicServiceRequest.newBuilder()
                .setType(TopicServiceRequestType.EnsureTopic)
                .setMessage(Any.pack(ensureTopicRequest))
                .build();

        int count      = observer.valueCount();
        int responseId = count + 1;

        channel.onNext(newRequest(request));
        observer.awaitCount(responseId, 1, TimeUnit.MINUTES);
        observer.assertNoErrors();

        ProxyResponse proxyResponse = observer.valueAt(responseId - 1);
        assertThat(proxyResponse, is(notNullValue()));

        ProxyResponse.ResponseCase responseCase  = proxyResponse.getResponseCase();
        if (responseCase == ProxyResponse.ResponseCase.ERROR)
            {
            ErrorMessage error = proxyResponse.getError();
            fail(error.getMessage());
            }

        TopicServiceResponse response = unpackAny(proxyResponse, ProxyResponse::getMessage, TopicServiceResponse.class);
        int                  topicId  = response.getProxyId();

        // wait for the completion message
        observer.awaitCount(responseId + 1, 1, TimeUnit.MINUTES);

        assertThat(topicId, is(not(0)));
        return topicId;
        }

    protected <Resp extends Message> Resp sendTopicRequest(StreamObserver<ProxyRequest> channel,
            TestStreamObserver<ProxyResponse> observer, int topicId,
            TopicServiceRequestType type, Message message) throws Exception
        {
        TopicServiceRequest request = TopicServiceRequest.newBuilder()
                .setProxyId(topicId)
                .setType(type)
                .setMessage(Any.pack(message))
                .build();

        int count      = observer.valueCount();
        int responseId = count + 1;

        channel.onNext(newRequest(request));
        observer.awaitCount(responseId, 1, TimeUnit.MINUTES);
        observer.assertNoErrors();

        ProxyResponse proxyResponse = observer.valueAt(responseId - 1);
        if (proxyResponse.getResponseCase() == ProxyResponse.ResponseCase.ERROR)
            {
            ErrorMessage error = proxyResponse.getError();
            Throwable    cause = null;
            if (error.hasError())
                {
                ProxyServiceChannel proxy = null;
                if (channel instanceof ProxyServiceChannel)
                    {
                    proxy = (ProxyServiceChannel) channel;
                    }
                if (channel instanceof ProxyServiceChannel.AsyncWrapper)
                    {
                    proxy = ((ProxyServiceChannel.AsyncWrapper) channel).getWrapped();
                    }
                if (proxy != null)
                    {
                    cause = BinaryHelper.fromByteString(error.getError(), proxy.getSerializer());
                    }
                throw new RequestIncompleteException(error.getMessage(), cause);
                }
            }
        if (proxyResponse.getResponseCase() == ProxyResponse.ResponseCase.COMPLETE)
            {
            return (Resp) proxyResponse.getComplete();
            }
        return (Resp) unpackAny(proxyResponse, ProxyResponse::getMessage, TopicServiceResponse.class);
        }

    // ----- data members ---------------------------------------------------

    public static final AtomicLong m_topicNameSuffix = new AtomicLong();
    }
