/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy.version_1;

import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.concurrent.Queues;
import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.NamedQueueProtocol;
import com.oracle.coherence.grpc.messages.common.v1.ErrorMessage;
import com.oracle.coherence.grpc.messages.common.v1.OptionalValue;
import com.oracle.coherence.grpc.messages.concurrent.queue.v1.EnsureQueueRequest;
import com.oracle.coherence.grpc.messages.concurrent.queue.v1.NamedQueueRequest;
import com.oracle.coherence.grpc.messages.concurrent.queue.v1.NamedQueueRequestType;
import com.oracle.coherence.grpc.messages.concurrent.queue.v1.NamedQueueResponse;
import com.oracle.coherence.grpc.messages.concurrent.queue.v1.NamedQueueResponseType;
import com.oracle.coherence.grpc.messages.concurrent.queue.v1.NamedQueueType;
import com.oracle.coherence.grpc.messages.concurrent.queue.v1.QueueOfferResult;
import com.oracle.coherence.grpc.messages.proxy.v1.InitRequest;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyRequest;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyResponse;
import com.oracle.coherence.grpc.proxy.common.ProxyServiceChannel;
import com.tangosol.coherence.config.scheme.PagedQueueScheme;
import com.tangosol.coherence.config.scheme.SimpleDequeScheme;
import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.net.CacheService;
import com.tangosol.net.Coherence;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedDeque;
import com.tangosol.net.NamedQueue;
import com.tangosol.net.QueueService;
import com.tangosol.net.RequestIncompleteException;
import com.tangosol.net.Session;
import grpc.proxy.TestStreamObserver;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings({"unchecked", "resource"})
public class NamedQueueProxyProtocolIT
        extends BaseVersionOneGrpcServiceIT
    {
    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldFailIfQueueNotEnsured(String ignored, Serializer serializer, String sScope) throws Exception
        {
        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        observer.awaitCount(1, 1, TimeUnit.MINUTES);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class,
                () -> sendQueueRequest(channel, observer, 0, NamedQueueRequestType.PeekHead, BytesValue.getDefaultInstance()));

        assertThat(ex.getMessage(), startsWith("Missing queue id in request"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldEnsureSimpleQueue(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                            sQueueName = newQueueName();
        TestStreamObserver<ProxyResponse> observer   = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel    = openChannel(observer);

        NamedQueue<String> queue      = ensureQueue(sScope, sQueueName);
        QueueService       service    = queue.getService();
        Set<String>        cacheNames = getCacheNames(service);
        queue.destroy();
        assertThat(getCacheNames(service), is(not(cacheNames)));

        init(channel, observer, serializer, sScope);

        int queueId = ensureQueue(channel, observer, sQueueName, NamedQueueType.Queue);
        assertThat(queueId, is(not(0)));
        assertThat(getCacheNames(service), is(cacheNames));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldEnsureSimpleQueueTwice(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                            sQueueName = newQueueName();
        TestStreamObserver<ProxyResponse> observer   = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel    = openChannel(observer);

        init(channel, observer, serializer, sScope);

        int queueIdOne = ensureQueue(channel, observer, sQueueName, NamedQueueType.Queue);
        int queueIdTwo = ensureQueue(channel, observer, sQueueName, NamedQueueType.Queue);
        assertThat(queueIdOne, is(not(0)));
        assertThat(queueIdTwo, is(queueIdOne));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldEnsureSimpleDequeAfterQueue(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                            sQueueName = newQueueName();
        TestStreamObserver<ProxyResponse> observer   = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel    = openChannel(observer);

        init(channel, observer, serializer, sScope);

        int queueIdOne = ensureQueue(channel, observer, sQueueName, NamedQueueType.Queue);
        assertThat(queueIdOne, is(not(0)));
        assertThat(queueIdOne, is(not(0)));
        assertThrows(AssertionError.class, () -> ensureQueue(channel, observer, sQueueName, NamedQueueType.Deque));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldEnsurePagedQueueAfterQueue(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                            sQueueName = newQueueName();
        TestStreamObserver<ProxyResponse> observer   = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel    = openChannel(observer);

        init(channel, observer, serializer, sScope);

        int queueIdOne = ensureQueue(channel, observer, sQueueName, NamedQueueType.Queue);
        assertThat(queueIdOne, is(not(0)));
        assertThat(queueIdOne, is(not(0)));
        assertThrows(AssertionError.class, () -> ensureQueue(channel, observer, sQueueName, NamedQueueType.PagedQueue));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldEnsureSimpleDeque(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                            sQueueName = newQueueName();
        TestStreamObserver<ProxyResponse> observer   = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel    = openChannel(observer);

        NamedDeque<String> deque      = ensureDeque(sScope, sQueueName);
        QueueService       service    = deque.getService();
        Set<String>        cacheNames = getCacheNames(service);
        deque.destroy();
        assertThat(getCacheNames(service), is(not(cacheNames)));

        init(channel, observer, serializer, sScope);

        int queueId = ensureQueue(channel, observer, sQueueName, NamedQueueType.Deque);
        assertThat(queueId, is(not(0)));
        assertThat(getCacheNames(service), is(cacheNames));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldEnsureSimpleDequeTwice(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                            sQueueName = newQueueName();
        TestStreamObserver<ProxyResponse> observer   = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel    = openChannel(observer);

        init(channel, observer, serializer, sScope);

        int queueIdOne = ensureQueue(channel, observer, sQueueName, NamedQueueType.Deque);
        int queueIdTwo = ensureQueue(channel, observer, sQueueName, NamedQueueType.Deque);
        assertThat(queueIdOne, is(not(0)));
        assertThat(queueIdTwo, is(queueIdOne));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldEnsureSimpleQueueAfterDeque(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                            sQueueName = newQueueName();
        TestStreamObserver<ProxyResponse> observer   = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel    = openChannel(observer);

        init(channel, observer, serializer, sScope);

        int queueIdOne = ensureQueue(channel, observer, sQueueName, NamedQueueType.Deque);
        int queueIdTwo = ensureQueue(channel, observer, sQueueName, NamedQueueType.Queue);
        assertThat(queueIdOne, is(not(0)));
        assertThat(queueIdTwo, is(queueIdOne));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldEnsurePagedQueueAfterDeque(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                            sQueueName = newQueueName();
        TestStreamObserver<ProxyResponse> observer   = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel    = openChannel(observer);

        init(channel, observer, serializer, sScope);

        int queueIdOne = ensureQueue(channel, observer, sQueueName, NamedQueueType.Deque);
        assertThat(queueIdOne, is(not(0)));
        assertThat(queueIdOne, is(not(0)));
        assertThrows(AssertionError.class, () -> ensureQueue(channel, observer, sQueueName, NamedQueueType.PagedQueue));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldEnsurePagedQueue(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                            sQueueName = newQueueName();
        TestStreamObserver<ProxyResponse> observer   = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel    = openChannel(observer);

        NamedQueue<String> queue      = ensurePaged(sScope, sQueueName);
        QueueService       service    = queue.getService();
        Set<String>        cacheNames = getCacheNames(service);
        queue.destroy();
        assertThat(getCacheNames(service), is(not(cacheNames)));

        init(channel, observer, serializer, sScope);

        int queueId = ensureQueue(channel, observer, sQueueName, NamedQueueType.PagedQueue);
        assertThat(queueId, is(not(0)));
        assertThat(getCacheNames(service), is(cacheNames));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldEnsurePagedQueueTwice(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                            sQueueName = newQueueName();
        TestStreamObserver<ProxyResponse> observer   = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel    = openChannel(observer);

        init(channel, observer, serializer, sScope);

        int queueIdOne = ensureQueue(channel, observer, sQueueName, NamedQueueType.PagedQueue);
        int queueIdTwo = ensureQueue(channel, observer, sQueueName, NamedQueueType.PagedQueue);
        assertThat(queueIdOne, is(not(0)));
        assertThat(queueIdTwo, is(queueIdOne));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldEnsureSimpleQueueAfterPagedQueue(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                            sQueueName = newQueueName();
        TestStreamObserver<ProxyResponse> observer   = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel    = openChannel(observer);

        init(channel, observer, serializer, sScope);

        int queueIdOne = ensureQueue(channel, observer, sQueueName, NamedQueueType.PagedQueue);
        int queueIdTwo = ensureQueue(channel, observer, sQueueName, NamedQueueType.Queue);
        assertThat(queueIdOne, is(not(0)));
        assertThat(queueIdTwo, is(queueIdOne));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldEnsureSimpleDequeAfterPagedQueue(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                            sQueueName = newQueueName();
        TestStreamObserver<ProxyResponse> observer   = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel    = openChannel(observer);

        init(channel, observer, serializer, sScope);

        int queueIdOne = ensureQueue(channel, observer, sQueueName, NamedQueueType.PagedQueue);
        assertThat(queueIdOne, is(not(0)));
        assertThrows(AssertionError.class, () -> ensureQueue(channel, observer, sQueueName, NamedQueueType.Deque));
        }

    @Test
    public void shouldEnsureConcurrentQueue() throws Exception
        {
        String                            sQueueName = newQueueName();
        TestStreamObserver<ProxyResponse> observer   = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel    = openChannel(observer);
        Serializer                        serializer = new DefaultSerializer();
        Session                           session    = Coherence.findSession(Queues.SESSION_NAME).orElse(null);

        assertThat(session, is(not(nullValue())));

        String sScope = session.getScopeName();

        NamedQueue<String> queue      = Queues.queue(sQueueName);
        QueueService       service    = queue.getService();
        Set<String>        cacheNames = getCacheNames(service);
        queue.destroy();
        assertThat(getCacheNames(service), is(not(cacheNames)));

        init(channel, observer, serializer, sScope);

        int queueId = ensureQueue(channel, observer, sQueueName, NamedQueueType.Queue);
        assertThat(queueId, is(not(0)));
        assertThat(getCacheNames(service), is(cacheNames));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldPeekHeadWithExistingElement(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String             sQueueName = newQueueName();
        NamedQueue<String> queue      = ensureEmptyQueue(sScope, sQueueName);

        queue.clear();
        assertThat(queue.offer("value-1"), is(true));
        assertThat(queue.offer("value-2"), is(true));
        assertThat(queue.peek(), is("value-1"));

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int queueId = ensureQueue(channel, observer, sQueueName);

        NamedQueueResponse response = sendQueueRequest(channel, observer, queueId, NamedQueueRequestType.PeekHead, Empty.getDefaultInstance());
        OptionalValue      value    = unpackAny(response, NamedQueueResponse::getMessage, OptionalValue.class);

        assertThat(value, is(notNullValue()));
        assertThat(value.getPresent(), is(true));
        assertThat(fromByteString(value.getValue(), serializer, String.class), is("value-1"));

        // element should still be in queue
        assertThat(queue.peek(), is("value-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldPeekHeadOnEmptyQueue(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String             sQueueName = newQueueName();
        NamedQueue<String> queue      = ensureEmptyQueue(sScope, sQueueName);

        queue.clear();

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int queueId = ensureQueue(channel, observer, sQueueName);

        NamedQueueResponse response = sendQueueRequest(channel, observer, queueId, NamedQueueRequestType.PeekHead, Empty.getDefaultInstance());
        OptionalValue      value    = unpackAny(response, NamedQueueResponse::getMessage, OptionalValue.class);

        assertThat(value, is(notNullValue()));
        assertThat(value.getPresent(), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldPeekTailWithExistingElement(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String             sQueueName = newQueueName();
        NamedDeque<String> queue      = ensureEmptyDeque(sScope, sQueueName);

        queue.clear();
        assertThat(queue.offer("value-1"), is(true));
        assertThat(queue.offer("value-2"), is(true));
        assertThat(queue.peekLast(), is("value-2"));

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int queueId = ensureDeque(channel, observer, sQueueName);

        NamedQueueResponse response = sendQueueRequest(channel, observer, queueId, NamedQueueRequestType.PeekTail, Empty.getDefaultInstance());
        OptionalValue      value    = unpackAny(response, NamedQueueResponse::getMessage, OptionalValue.class);

        assertThat(value, is(notNullValue()));
        assertThat(value.getPresent(), is(true));
        assertThat(fromByteString(value.getValue(), serializer, String.class), is("value-2"));

        // element should still be in queue
        assertThat(queue.peekLast(), is("value-2"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldPeekTailOnEmptyQueue(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String             sQueueName = newQueueName();
        NamedDeque<String> queue      = ensureEmptyDeque(sScope, sQueueName);

        queue.clear();

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int queueId = ensureDeque(channel, observer, sQueueName);

        NamedQueueResponse response = sendQueueRequest(channel, observer, queueId, NamedQueueRequestType.PeekTail, Empty.getDefaultInstance());
        OptionalValue      value    = unpackAny(response, NamedQueueResponse::getMessage, OptionalValue.class);

        assertThat(value, is(notNullValue()));
        assertThat(value.getPresent(), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldPollHeadWithExistingElement(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String             sQueueName = newQueueName();
        NamedQueue<String> queue      = ensureEmptyQueue(sScope, sQueueName);

        queue.clear();
        assertThat(queue.offer("value-1"), is(true));
        assertThat(queue.offer("value-2"), is(true));
        assertThat(queue.offer("value-3"), is(true));
        assertThat(queue.peek(), is("value-1"));

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int queueId = ensureQueue(channel, observer, sQueueName);

        NamedQueueResponse response = sendQueueRequest(channel, observer, queueId, NamedQueueRequestType.PollHead, Empty.getDefaultInstance());
        OptionalValue      value    = unpackAny(response, NamedQueueResponse::getMessage, OptionalValue.class);

        assertThat(value, is(notNullValue()));
        assertThat(value.getPresent(), is(true));
        assertThat(fromByteString(value.getValue(), serializer, String.class), is("value-1"));

        // element should not be in queue
        assertThat(queue.peek(), is("value-2"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldPollHeadOnEmptyQueue(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String             sQueueName = newQueueName();
        NamedQueue<String> queue      = ensureEmptyQueue(sScope, sQueueName);

        queue.clear();

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int queueId = ensureQueue(channel, observer, sQueueName);

        NamedQueueResponse response = sendQueueRequest(channel, observer, queueId, NamedQueueRequestType.PollHead, Empty.getDefaultInstance());
        OptionalValue      value    = unpackAny(response, NamedQueueResponse::getMessage, OptionalValue.class);

        assertThat(value, is(notNullValue()));
        assertThat(value.getPresent(), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldPollTailWithExistingElement(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String             sQueueName = newQueueName();
        NamedDeque<String> queue      = ensureEmptyDeque(sScope, sQueueName);

        queue.clear();
        assertThat(queue.offer("value-1"), is(true));
        assertThat(queue.offer("value-2"), is(true));
        assertThat(queue.offer("value-3"), is(true));
        assertThat(queue.peekLast(), is("value-3"));

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int queueId = ensureDeque(channel, observer, sQueueName);

        NamedQueueResponse response = sendQueueRequest(channel, observer, queueId, NamedQueueRequestType.PollTail, Empty.getDefaultInstance());
        OptionalValue      value    = unpackAny(response, NamedQueueResponse::getMessage, OptionalValue.class);

        assertThat(value, is(notNullValue()));
        assertThat(value.getPresent(), is(true));
        assertThat(fromByteString(value.getValue(), serializer, String.class), is("value-3"));

        // element should not be in queue
        assertThat(queue.peekLast(), is("value-2"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldPollTailOnEmptyQueue(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String             sQueueName = newQueueName();
        NamedDeque<String> queue      = ensureEmptyDeque(sScope, sQueueName);

        queue.clear();

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int queueId = ensureDeque(channel, observer, sQueueName);

        NamedQueueResponse response = sendQueueRequest(channel, observer, queueId, NamedQueueRequestType.PollTail, Empty.getDefaultInstance());
        OptionalValue      value    = unpackAny(response, NamedQueueResponse::getMessage, OptionalValue.class);

        assertThat(value, is(notNullValue()));
        assertThat(value.getPresent(), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldOfferToTail(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String             sQueueName = newQueueName();
        NamedQueue<String> queue      = ensureEmptyQueue(sScope, sQueueName);

        queue.clear();

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int queueId = ensureQueue(channel, observer, sQueueName);

        ByteString bytesOne = toByteString("value-1", serializer);
        ByteString bytesTwo = toByteString("value-2", serializer);

        NamedQueueResponse response = sendQueueRequest(channel, observer, queueId, NamedQueueRequestType.OfferTail, BytesValue.of(bytesOne));
        QueueOfferResult   result   = unpackAny(response, NamedQueueResponse::getMessage, QueueOfferResult.class);
        assertThat(result, is(notNullValue()));
        assertThat(result.getSucceeded(), is(true));

        response = sendQueueRequest(channel, observer, queueId, NamedQueueRequestType.OfferTail, BytesValue.of(bytesTwo));
        result   = unpackAny(response, NamedQueueResponse::getMessage, QueueOfferResult.class);
        assertThat(result, is(notNullValue()));
        assertThat(result.getSucceeded(), is(true));

        assertThat(queue.poll(), is("value-1"));
        assertThat(queue.poll(), is("value-2"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldOfferToHead(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String             sQueueName = newQueueName();
        NamedDeque<String> queue      = ensureEmptyDeque(sScope, sQueueName);

        queue.clear();

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int queueId = ensureDeque(channel, observer, sQueueName);

        ByteString bytesOne = toByteString("value-1", serializer);
        ByteString bytesTwo = toByteString("value-2", serializer);

        NamedQueueResponse response = sendQueueRequest(channel, observer, queueId, NamedQueueRequestType.OfferHead, BytesValue.of(bytesOne));
        QueueOfferResult   result   = unpackAny(response, NamedQueueResponse::getMessage, QueueOfferResult.class);
        assertThat(result, is(notNullValue()));
        assertThat(result.getSucceeded(), is(true));

        response = sendQueueRequest(channel, observer, queueId, NamedQueueRequestType.OfferHead, BytesValue.of(bytesTwo));
        result   = unpackAny(response, NamedQueueResponse::getMessage, QueueOfferResult.class);
        assertThat(result, is(notNullValue()));
        assertThat(result.getSucceeded(), is(true));

        assertThat(queue.poll(), is("value-2"));
        assertThat(queue.poll(), is("value-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallIsReady(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String             sQueueName = newQueueName();
        NamedQueue<String> queue      = ensureEmptyQueue(sScope, sQueueName);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int queueId = ensureQueue(channel, observer, sQueueName);

        NamedQueueResponse response = sendQueueRequest(channel, observer, queueId, NamedQueueRequestType.IsReady, Empty.getDefaultInstance());
        BoolValue          result   = unpackAny(response, NamedQueueResponse::getMessage, BoolValue.class);
        assertThat(result, is(notNullValue()));
        assertThat(result.getValue(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallIsEmptyOnEmptyQueue(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String             sQueueName = newQueueName();
        NamedQueue<String> queue      = ensureEmptyQueue(sScope, sQueueName);

        queue.clear();

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int queueId = ensureQueue(channel, observer, sQueueName);

        NamedQueueResponse response = sendQueueRequest(channel, observer, queueId, NamedQueueRequestType.IsEmpty, Empty.getDefaultInstance());
        BoolValue          result   = unpackAny(response, NamedQueueResponse::getMessage, BoolValue.class);
        assertThat(result, is(notNullValue()));
        assertThat(result.getValue(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallIsEmptyOnPopulatedQueue(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String             sQueueName = newQueueName();
        NamedQueue<String> queue      = ensureEmptyQueue(sScope, sQueueName);

        assertThat(queue.offer("value-1"), is(true));
        assertThat(queue.offer("value-2"), is(true));
        assertThat(queue.offer("value-3"), is(true));

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int queueId = ensureQueue(channel, observer, sQueueName);

        NamedQueueResponse response = sendQueueRequest(channel, observer, queueId, NamedQueueRequestType.IsEmpty, Empty.getDefaultInstance());
        BoolValue          result   = unpackAny(response, NamedQueueResponse::getMessage, BoolValue.class);
        assertThat(result, is(notNullValue()));
        assertThat(result.getValue(), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallSizeOnEmptyQueue(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String             sQueueName = newQueueName();
        NamedQueue<String> queue      = ensureEmptyQueue(sScope, sQueueName);

        queue.clear();

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int queueId = ensureQueue(channel, observer, sQueueName);

        NamedQueueResponse response = sendQueueRequest(channel, observer, queueId, NamedQueueRequestType.Size, Empty.getDefaultInstance());
        Int32Value         result   = unpackAny(response, NamedQueueResponse::getMessage, Int32Value.class);
        assertThat(result, is(notNullValue()));
        assertThat(result.getValue(), is(0));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallSizeOnPopulatedQueue(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String             sQueueName = newQueueName();
        NamedQueue<String> queue      = ensureEmptyQueue(sScope, sQueueName);

        assertThat(queue.offer("value-1"), is(true));
        assertThat(queue.offer("value-2"), is(true));
        assertThat(queue.offer("value-3"), is(true));

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int queueId = ensureQueue(channel, observer, sQueueName);

        NamedQueueResponse response = sendQueueRequest(channel, observer, queueId, NamedQueueRequestType.Size, Empty.getDefaultInstance());
        Int32Value          result  = unpackAny(response, NamedQueueResponse::getMessage, Int32Value.class);
        assertThat(result, is(notNullValue()));
        assertThat(result.getValue(), is(queue.size()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldClearPopulatedQueue(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String             sQueueName = newQueueName();
        NamedQueue<String> queue      = ensureEmptyQueue(sScope, sQueueName);

        assertThat(queue.offer("value-1"), is(true));
        assertThat(queue.offer("value-2"), is(true));
        assertThat(queue.offer("value-3"), is(true));

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int queueId = ensureQueue(channel, observer, sQueueName);

        sendQueueRequest(channel, observer, queueId, NamedQueueRequestType.Clear, Empty.getDefaultInstance());

        assertThat(queue.isEmpty(), is(true));
        assertThat(queue.peek(), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldClearEmptyQueue(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String             sQueueName = newQueueName();
        NamedQueue<String> queue      = ensureEmptyQueue(sScope, sQueueName);

        queue.clear();

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int queueId = ensureQueue(channel, observer, sQueueName);

        sendQueueRequest(channel, observer, queueId, NamedQueueRequestType.Clear, Empty.getDefaultInstance());

        assertThat(queue.isEmpty(), is(true));
        assertThat(queue.peek(), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldDestroyQueue(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String             sQueueName = newQueueName();
        NamedQueue<String> queue      = ensureEmptyQueue(sScope, sQueueName);

        assertThat(queue.isDestroyed(), is(false));

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int queueId = ensureQueue(channel, observer, sQueueName);

        int count = observer.valueCount();
        sendQueueRequest(channel, observer, queueId, NamedQueueRequestType.Destroy, Empty.getDefaultInstance());

        Eventually.assertDeferred(queue::isDestroyed, is(true));

        Optional<NamedQueueResponse> destroyedEvent = observer.values()
                .stream()
                .skip(count)
                .filter(ProxyResponse::hasMessage)
                .map(m -> unpackAny(m, ProxyResponse::getMessage, NamedQueueResponse.class))
                .filter(m -> m.getQueueId() == queueId)
                .filter(m -> m.getType() == NamedQueueResponseType.Destroyed)
                .findFirst();

        Eventually.assertDeferred(queue::isDestroyed, is(true));
        assertThat(destroyedEvent.isPresent(), is(true));

        }

    // ----- helper methods -------------------------------------------------

    protected static String newQueueName()
        {
        return "test-queue-" + m_queueNameSuffix.incrementAndGet();
        }

    protected <V> NamedQueue<V> ensureEmptyQueue(String sScope, String name)
        {
        NamedQueue<V> queue = ensureQueue(sScope, name);
        queue.clear();
        return queue;
        }

    protected <V> NamedQueue<V> ensureQueue(String sScope, String name)
        {
        ExtensibleConfigurableCacheFactory eccf = (ExtensibleConfigurableCacheFactory) ensureCCF(sScope);
        return SimpleDequeScheme.INSTANCE.realize(name, eccf);
        }

    protected <V> NamedDeque<V> ensureEmptyDeque(String sScope, String name)
        {
        NamedDeque<V> queue = ensureDeque(sScope, name);
        queue.clear();
        return queue;
        }

    protected <V> NamedDeque<V> ensureDeque(String sScope, String name)
        {
        ExtensibleConfigurableCacheFactory eccf = (ExtensibleConfigurableCacheFactory) ensureCCF(sScope);
        return SimpleDequeScheme.INSTANCE.realize(name, eccf);
        }

    protected <V> NamedQueue<V> ensurePaged(String sScope, String name)
        {
        ExtensibleConfigurableCacheFactory eccf = (ExtensibleConfigurableCacheFactory) ensureCCF(sScope);
        return PagedQueueScheme.INSTANCE.realize(name, eccf);
        }

    protected int ensureQueue(StreamObserver<ProxyRequest> channel, TestStreamObserver<ProxyResponse> observer,
            String sQueueName) throws Exception
        {
        return ensureQueue(channel, observer, sQueueName, NamedQueueType.Queue);
        }

    protected int ensureDeque(StreamObserver<ProxyRequest> channel, TestStreamObserver<ProxyResponse> observer,
            String sQueueName) throws Exception
        {
        return ensureQueue(channel, observer, sQueueName, NamedQueueType.Deque);
        }

    protected int ensurePagedQueue(StreamObserver<ProxyRequest> channel, TestStreamObserver<ProxyResponse> observer,
            String sQueueName) throws Exception
        {
        return ensureQueue(channel, observer, sQueueName, NamedQueueType.PagedQueue);
        }

    protected int ensureQueue(StreamObserver<ProxyRequest> channel, TestStreamObserver<ProxyResponse> observer,
                              String sQueueName, NamedQueueType type) throws Exception
        {
        EnsureQueueRequest ensureQueueRequest = EnsureQueueRequest.newBuilder()
                .setQueue(sQueueName)
                .setType(type)
                .build();

        NamedQueueRequest request = NamedQueueRequest.newBuilder()
                .setType(NamedQueueRequestType.EnsureQueue)
                .setMessage(Any.pack(ensureQueueRequest))
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

        NamedQueueResponse response = unpackAny(proxyResponse, ProxyResponse::getMessage, NamedQueueResponse.class);
        int                queueId  = response.getQueueId();

        assertThat(queueId, is(not(0)));
        return queueId;
        }

    protected <Resp extends Message> Resp sendQueueRequest(StreamObserver<ProxyRequest> channel,
            TestStreamObserver<ProxyResponse> observer, int queueId, NamedQueueRequestType type,
            Message message) throws Exception
        {
        NamedQueueRequest request = NamedQueueRequest.newBuilder()
                .setQueueId(queueId)
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
        return (Resp) unpackAny(proxyResponse, ProxyResponse::getMessage, NamedQueueResponse.class);
        }

    protected void init(StreamObserver<ProxyRequest> channel, TestStreamObserver<ProxyResponse> observer,
            Serializer serializer, String sScope) throws Exception
        {
        InitRequest initRequest = InitRequest.newBuilder()
                .setProtocol(NamedQueueProtocol.PROTOCOL_NAME)
                .setProtocolVersion(NamedQueueProtocol.VERSION)
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

    protected Set<String> getCacheNames(CacheService service)
        {
        Enumeration<String> enumeration = service.getCacheNames();
        Set<String> cacheNames  = new HashSet<>();
        enumeration.asIterator().forEachRemaining(cacheNames::add);
        return cacheNames;
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
                    "Failed to unpack proto message: " + e.getMessage() + "\nMessage:\n" + message);
            }
        }


    // ----- data members ---------------------------------------------------

    public static final AtomicLong m_queueNameSuffix = new AtomicLong();
    }
