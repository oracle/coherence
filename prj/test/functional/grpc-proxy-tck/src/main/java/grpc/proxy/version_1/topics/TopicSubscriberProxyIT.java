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
import com.oracle.coherence.grpc.ErrorsHelper;
import com.oracle.coherence.grpc.NamedTopicProtocol;
import com.oracle.coherence.grpc.TopicHelper;

import com.oracle.coherence.grpc.messages.common.v1.ErrorMessage;

import com.oracle.coherence.grpc.messages.proxy.v1.InitRequest;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyRequest;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyResponse;


import com.oracle.coherence.grpc.messages.topic.v1.EnsureSubscriberRequest;
import com.oracle.coherence.grpc.messages.topic.v1.EnsureSubscriberResponse;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceRequest;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceRequestType;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceResponse;
import com.oracle.coherence.grpc.proxy.common.ProxyServiceChannel;

import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.io.Serializer;

import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.PagedTopicService;
import com.tangosol.net.RequestIncompleteException;

import com.tangosol.net.TopicService;
import com.tangosol.net.topic.NamedTopic;

import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.ValueExtractor;
import grpc.proxy.TestStreamObserver;

import grpc.proxy.version_1.BaseVersionOneGrpcServiceIT;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.emptyIterable;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;


@SuppressWarnings({"unchecked", "resource"})
public class TopicSubscriberProxyIT
        extends BaseVersionOneGrpcServiceIT
    {
    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldEnsureSubscriber(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                            sTopicName = newTopicName();
        TestStreamObserver<ProxyResponse> observer   = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel    = openChannel(observer);

        init(channel, observer, serializer, sScope);

        EnsureSubscriberResponse response = ensureSubscriber(channel, observer, sTopicName);
        assertThat(response.hasSubscriberId(), is(true));
        assertThat(response.hasGroupId(), is(true));

        SubscriberId      subscriberId = TopicHelper.fromProtobufSubscriberId(response);
        SubscriberGroupId groupId      = TopicHelper.fromProtobufSubscriberGroupId(response);
        assertThat(subscriberId, is(notNullValue()));
        assertThat(groupId, is(notNullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldEnsureSubscriberInGroup(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                            sTopicName = newTopicName();
        NamedTopic<?>                     topic      = ensureTopic(sScope, sTopicName);
        PagedTopicService                 service    = (PagedTopicService) topic.getTopicService();
        TestStreamObserver<ProxyResponse> observer   = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel    = openChannel(observer);
        String                            sGroup     = "test-group";

        init(channel, observer, serializer, sScope);

        assertThat(service.getSubscriberGroups(sTopicName), is(emptyIterable()));

        long subscriptionId = service.getSubscriptionId(sTopicName, SubscriberGroupId.withName(sGroup));
        assertThat(subscriptionId, is(0L));

        EnsureSubscriberResponse response = ensureSubscriber(channel, observer, sTopicName, sGroup);
        SubscriberGroupId        groupId  = TopicHelper.fromProtobufSubscriberGroupId(response);
        assertThat(groupId, is(notNullValue()));
        assertThat(groupId.getGroupName(), is(sGroup));

        Set<SubscriberGroupId> set = service.getSubscriberGroups(sTopicName);
        assertThat(set, is(Set.of(groupId)));

        subscriptionId = service.getSubscriptionId(sTopicName, SubscriberGroupId.withName(sGroup));
        assertThat(subscriptionId, is(not(0L)));
        PagedTopicSubscription subscription = service.getSubscription(subscriptionId);
        assertThat(subscription, is(notNullValue()));
        assertThat(subscription.getSubscriberGroupId(), is(groupId));
        assertThat(subscription.getFilter(), is(nullValue()));
        assertThat(subscription.getConverter(), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldEnsureSubscriberInGroupWithFilter(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                            sTopicName = newTopicName();
        NamedTopic<?>                     topic      = ensureTopic(sScope, sTopicName);
        PagedTopicService                 service    = (PagedTopicService) topic.getTopicService();
        TestStreamObserver<ProxyResponse> observer   = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel    = openChannel(observer);
        String                            sGroup     = "test-group";
        Filter<?>                         filter     = Filters.equal("foo", "bar");

        init(channel, observer, serializer, sScope);

        assertThat(service.getSubscriberGroups(sTopicName), is(emptyIterable()));

        long subscriptionId = service.getSubscriptionId(sTopicName, SubscriberGroupId.withName(sGroup));
        assertThat(subscriptionId, is(0L));

        EnsureSubscriberResponse response = ensureSubscriber(channel, observer, sTopicName, sGroup, filter, serializer);
        SubscriberGroupId        groupId  = TopicHelper.fromProtobufSubscriberGroupId(response);
        assertThat(groupId, is(notNullValue()));
        assertThat(groupId.getGroupName(), is(sGroup));

        Set<SubscriberGroupId> set = service.getSubscriberGroups(sTopicName);
        assertThat(set, is(Set.of(groupId)));

        subscriptionId = service.getSubscriptionId(sTopicName, SubscriberGroupId.withName(sGroup));
        assertThat(subscriptionId, is(not(0L)));
        PagedTopicSubscription subscription = service.getSubscription(subscriptionId);
        assertThat(subscription, is(notNullValue()));
        assertThat(subscription.getSubscriberGroupId(), is(groupId));
        assertThat(subscription.getFilter(), is(filter));
        assertThat(subscription.getConverter(), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldEnsureSubscriberInGroupWithDifferentFilter(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                            sTopicName = newTopicName();
        TestStreamObserver<ProxyResponse> observer   = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel    = openChannel(observer);
        String                            sGroup     = "test-group";
        Filter<?>                         filterOne  = Filters.equal("foo", "one");
        Filter<?>                         filterTwo  = Filters.equal("foo", "two");

        init(channel, observer, serializer, sScope);

        EnsureSubscriberResponse response = ensureSubscriber(channel, observer, sTopicName, sGroup, filterOne, serializer);
        SubscriberGroupId        groupId  = TopicHelper.fromProtobufSubscriberGroupId(response);
        assertThat(groupId, is(notNullValue()));
        assertThat(groupId.getGroupName(), is(sGroup));

        RequestIncompleteException error = assertThrows(RequestIncompleteException.class, () -> ensureSubscriber(channel, observer, sTopicName, sGroup, filterTwo, serializer));
        assertThat(error.getMessage(), containsString("Cannot change the Filter"));
        }

    // ----- helper methods -------------------------------------------------
    
    protected static String newTopicName()
        {
        return "test-topic-" + m_topicNameSuffix.incrementAndGet();
        }

    protected <V> NamedTopic<V> ensureTopic(String sScope, String name)
        {
        ExtensibleConfigurableCacheFactory eccf = (ExtensibleConfigurableCacheFactory) ensureCCF(sScope);
        return eccf.ensureTopic(name);
        }

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
    
    protected EnsureSubscriberResponse ensureSubscriber(StreamObserver<ProxyRequest> channel,
            TestStreamObserver<ProxyResponse> observer, String sTopicName) throws Exception
        {
        return ensureSubscriber(channel, observer, sTopicName, null, null, null, null);
        }

    protected EnsureSubscriberResponse ensureSubscriber(StreamObserver<ProxyRequest> channel,
            TestStreamObserver<ProxyResponse> observer, String sTopicName, String sGroup) throws Exception
        {
        return ensureSubscriber(channel, observer, sTopicName, sGroup, null, null, null);
        }

    protected EnsureSubscriberResponse ensureSubscriber(StreamObserver<ProxyRequest> channel,
            TestStreamObserver<ProxyResponse> observer, String sTopicName, String sGroup,
            Filter<?> filter, Serializer serializer) throws Exception
        {
        return ensureSubscriber(channel, observer, sTopicName, sGroup, filter, null, serializer);
        }

    protected EnsureSubscriberResponse ensureSubscriber(StreamObserver<ProxyRequest> channel,
            TestStreamObserver<ProxyResponse> observer, String sTopicName, String sGroup,
            ValueExtractor<?, ?> extractor, Serializer serializer) throws Exception
        {
        return ensureSubscriber(channel, observer, sTopicName, sGroup, null, extractor, serializer);
        }

    protected EnsureSubscriberResponse ensureSubscriber(StreamObserver<ProxyRequest> channel,
            TestStreamObserver<ProxyResponse> observer, String sTopicName, String sGroup,
            Filter<?> filter, ValueExtractor<?, ?> extractor, Serializer serializer) throws Exception
        {
        EnsureSubscriberRequest.Builder builder = EnsureSubscriberRequest.newBuilder()
                .setTopic(sTopicName);

        if (sGroup != null)
            {
            builder.setSubscriberGroup(sGroup);
            }

        if (filter != null)
            {
            builder.setFilter(BinaryHelper.toByteString(filter, serializer));
            }

        if (extractor != null)
            {
            builder.setExtractor(BinaryHelper.toByteString(extractor, serializer));
            }

        TopicServiceRequest request = TopicServiceRequest.newBuilder()
                .setType(TopicServiceRequestType.EnsureSubscriber)
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
            throw ErrorsHelper.createException(error, serializer);
            }

        TopicServiceResponse response     = unpackAny(proxyResponse, ProxyResponse::getMessage, TopicServiceResponse.class);
        long                 subscriberId = response.getProxyId();

        EnsureSubscriberResponse ensureResponse = unpackAny(response, TopicServiceResponse::getMessage, EnsureSubscriberResponse.class);


        // wait for the completion message
        observer.awaitCount(responseId + 1, 1, TimeUnit.MINUTES);

        assertThat(subscriberId, is(not(0)));
        assertThat(ensureResponse, is(notNullValue()));
        return ensureResponse;
        }

    protected <Resp extends Message> Resp sendTopicRequest(StreamObserver<ProxyRequest> channel,
            TestStreamObserver<ProxyResponse> observer, long subscriberId, TopicServiceRequestType type,
            Message message) throws Exception
        {
        TopicServiceRequest request = TopicServiceRequest.newBuilder()
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

    public static final AtomicLong m_topicNameSuffix = new AtomicLong();    }
