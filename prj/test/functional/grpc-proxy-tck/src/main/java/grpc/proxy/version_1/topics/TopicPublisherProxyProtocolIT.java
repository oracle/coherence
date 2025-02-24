/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy.version_1.topics;

import com.google.protobuf.Any;
import com.google.protobuf.Int32Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.NamedTopicProtocol;
import com.oracle.coherence.grpc.TopicHelper;
import com.oracle.coherence.grpc.messages.common.v1.ErrorMessage;
import com.oracle.coherence.grpc.messages.proxy.v1.InitRequest;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyRequest;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyResponse;
import com.oracle.coherence.grpc.messages.topic.v1.EnsurePublisherRequest;
import com.oracle.coherence.grpc.messages.topic.v1.EnsurePublisherResponse;
import com.oracle.coherence.grpc.messages.topic.v1.PublishRequest;
import com.oracle.coherence.grpc.messages.topic.v1.PublisherEvent;
import com.oracle.coherence.grpc.messages.topic.v1.PublisherEventType;
import com.oracle.coherence.grpc.messages.topic.v1.ResponseType;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceRequest;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceRequestType;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceResponse;
import com.oracle.coherence.grpc.proxy.common.ProxyServiceChannel;
import com.tangosol.internal.net.topic.PublishResult;
import com.tangosol.io.Serializer;
import com.tangosol.net.RequestIncompleteException;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import grpc.proxy.TestStreamObserver;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings({"unchecked", "resource"})
public class TopicPublisherProxyProtocolIT
        extends BaseTopicsIT
    {
    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldEnsurePublisher(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                            sTopicName = newTopicName();
        TestStreamObserver<ProxyResponse> observer   = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel    = openChannel(observer);

        init(channel, observer, serializer, sScope);

        int publisherId = ensurePublisher(channel, observer, sTopicName);

        assertThat(publisherId, is(not(0)));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldDestroyPublisher(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                            sTopicName = newTopicName();
        TestStreamObserver<ProxyResponse> observer   = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel    = openChannel(observer);

        observer.enableLog(true);
        init(channel, observer, serializer, sScope);

        int count       = observer.valueCount();
        int publisherId = ensurePublisher(channel, observer, sTopicName);

        int cBefore = observer.valueCount();
        sendTopicRequest(channel, observer, 0, TopicServiceRequestType.DestroyPublisher, Int32Value.of(publisherId));

        // Wait for three more messages, the destroy response, the destroy complete and the destroy event.
        observer.awaitCount(cBefore + 3, 30, TimeUnit.SECONDS);

        Optional<TopicServiceResponse> destroyedEvent = observer.values()
                .stream()
                .skip(count)
                .filter(ProxyResponse::hasMessage)
                .map(m ->
                    {
                    try
                        {
                        return m.getMessage().unpack(TopicServiceResponse.class);
                        }
                    catch (InvalidProtocolBufferException e)
                        {
                        throw new RuntimeException(e);
                        }
                    })
                .filter(m -> m.getProxyId() == publisherId)
                .filter(m -> m.getType() == ResponseType.Event)
                .findFirst();

        assertThat(destroyedEvent.isPresent(), is(true));
        TopicServiceResponse response = destroyedEvent.get();
        assertThat(response.hasMessage(), is(true));
        PublisherEvent event = unpack(response.getMessage(), PublisherEvent.class);
        assertThat(event.getType(), is(PublisherEventType.PublisherDestroyed));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldPublishMessage(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String             sTopicName = newTopicName();
        NamedTopic<Object> topic      = ensureTopic(sScope, sTopicName);

        try (Subscriber<?> subscriber = topic.createSubscriber(Subscriber.completeOnEmpty()))
            {
            TestStreamObserver<ProxyResponse> observer   = new TestStreamObserver<>();
            StreamObserver<ProxyRequest>      channel    = openChannel(observer);

            init(channel, observer, serializer, sScope);

            int publisherId = ensurePublisher(channel, observer, sTopicName);

            String sMessage = "hello world";
            Binary binary   = ExternalizableHelper.toBinary(sMessage, serializer);
            int    nNotify  = 19;

            PublishRequest request  = PublishRequest.newBuilder()
                    .setChannel(2)
                    .setNotificationIdentifier(nNotify)
                    .addAllValues(List.of(BinaryHelper.toByteString(binary)))
                    .build();

            TopicServiceResponse response = sendTopicRequest(channel, observer, publisherId, TopicServiceRequestType.Publish, request);

            PublishResult publishResult = TopicHelper.fromProtoBufPublishResult(response, serializer);
            assertThat(publishResult, is(notNullValue()));

            // should be able to receive the published value
            Subscriber.Element<?> element = subscriber.receive().get(1, TimeUnit.MINUTES);
            assertThat(element, is(notNullValue()));
            assertThat(element.getValue(), is("hello world"));
            assertThat(element.getChannel(), is(2));
            }
        }

    // ----- helper methods -------------------------------------------------
    
    protected void init(StreamObserver<ProxyRequest> channel, TestStreamObserver<ProxyResponse> observer,
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
    
    protected int ensurePublisher(StreamObserver<ProxyRequest> channel, TestStreamObserver<ProxyResponse> observer, String sTopicName) throws Exception
        {
        return ensurePublisher(channel, observer, sTopicName, -1);
        }

    protected int ensurePublisher(StreamObserver<ProxyRequest> channel, TestStreamObserver<ProxyResponse> observer,
                                  String sTopicName, int cChannel) throws Exception
        {
        EnsurePublisherRequest.Builder builder = EnsurePublisherRequest.newBuilder()
                .setTopic(sTopicName);

        if (cChannel > 0)
            {
            builder.setChannelCount(cChannel);
            }

        TopicServiceRequest request = TopicServiceRequest.newBuilder()
                .setType(TopicServiceRequestType.EnsurePublisher)
                .setMessage(Any.pack(builder.build()))
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

        TopicServiceResponse    response       = unpackAny(proxyResponse, ProxyResponse::getMessage, TopicServiceResponse.class);
        EnsurePublisherResponse ensureResponse = unpackAny(response, TopicServiceResponse::getMessage, EnsurePublisherResponse.class);
        int                     publisherId    = response.getProxyId();

        // wait for the completion message
        observer.awaitCount(responseId + 1, 1, TimeUnit.MINUTES);

        assertThat(publisherId, is(not(0)));
        return publisherId;
        }

    protected <Resp extends Message> Resp sendTopicRequest(StreamObserver<ProxyRequest> channel,
            TestStreamObserver<ProxyResponse> observer, int publisherId, TopicServiceRequestType type,
            Message message) throws Exception
        {
        TopicServiceRequest request = TopicServiceRequest.newBuilder()
                .setType(type)
                .setProxyId(publisherId)
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
    }
