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

import com.google.protobuf.StringValue;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.grpc.messages.proxy.v1.ProxyRequest;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyResponse;

import com.oracle.coherence.grpc.messages.topic.v1.EnsureChannelCountRequest;
import com.oracle.coherence.grpc.messages.topic.v1.NamedTopicEvent;
import com.oracle.coherence.grpc.messages.topic.v1.ResponseType;

import com.oracle.coherence.grpc.messages.topic.v1.TopicEventType;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceRequestType;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceResponse;

import com.tangosol.io.Serializer;

import com.tangosol.net.TopicService;

import com.tangosol.net.topic.NamedTopic;

import grpc.proxy.TestStreamObserver;

import io.grpc.stub.StreamObserver;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;


public class NamedTopicServiceProxyProtocolIT
        extends BaseTopicsIT
    {
    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldEnsureTopic(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                            sTopicName = newTopicName();
        TestStreamObserver<ProxyResponse> observer   = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel    = openChannel(observer);

        NamedTopic<String> topic      = ensureTopic(sScope, sTopicName);
        TopicService       service    = topic.getTopicService();
        Set<String>        cacheNames = service.getTopicNames();
        topic.destroy();
        assertThat(service.getTopicNames(), is(not(cacheNames)));

        initTopic(channel, observer, serializer, sScope);

        int topicId = ensureTopic(channel, observer, sTopicName);
        assertThat(topicId, is(not(0)));
        assertThat(service.getTopicNames(), is(cacheNames));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldDestroyTopic(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                            sTopicName = newTopicName();
        NamedTopic<String>                topic      = ensureTopic(sScope, sTopicName);
        TestStreamObserver<ProxyResponse> observer   = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel    = openChannel(observer);

        initTopic(channel, observer, serializer, sScope);

        int topicId = ensureTopic(channel, observer, sTopicName);

        int cBefore = observer.valueCount();
        sendTopicRequest(channel, observer, 0, TopicServiceRequestType.DestroyTopic, StringValue.of(sTopicName));

        Eventually.assertDeferred(topic::isDestroyed, is(true));

        // Wait for three more messages, the destroy response, the destroy complete and the destroy event.
        observer.awaitCount(cBefore + 3, 30, TimeUnit.SECONDS);
        
        Optional<TopicServiceResponse> destroyedEvent = observer.values()
                .stream()
                .skip(cBefore)
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
                .filter(m -> m.getProxyId() == topicId)
                .filter(m -> m.getType() == ResponseType.Event)
                .findFirst();

        assertThat(destroyedEvent.isPresent(), is(true));
        TopicServiceResponse response = destroyedEvent.get();
        assertThat(response.hasMessage(), is(true));
        NamedTopicEvent event = response.getMessage().unpack(NamedTopicEvent.class);
        assertThat(event.getType(), is(TopicEventType.TopicDestroyed));
        }


    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldEnsureChannelCount(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                            sTopicName = newTopicName();
        NamedTopic<String>                topic      = ensureTopic(sScope, sTopicName);
        int                               cBefore    = topic.getChannelCount();
        TestStreamObserver<ProxyResponse> observer   = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel    = openChannel(observer);

        initTopic(channel, observer, serializer, sScope);

        int cRequired = cBefore + 1;
        int cCount    = cRequired + 3;

        EnsureChannelCountRequest request = EnsureChannelCountRequest.newBuilder()
                .setChannelCount(cCount)
                .setRequiredCount(cRequired)
                .setTopic(sTopicName)
                .build();

        TopicServiceResponse response = sendTopicRequest(channel, observer, 0, TopicServiceRequestType.EnsureChannelCount, request);
        assertThat(response.hasMessage(), is(true));
        Any        any      = response.getMessage();
        Int32Value cChannel = any.unpack(Int32Value.class);
        assertThat(cChannel.getValue(), is(cCount));

        assertThat(topic.getChannelCount(), is(cCount));
        }
    }
