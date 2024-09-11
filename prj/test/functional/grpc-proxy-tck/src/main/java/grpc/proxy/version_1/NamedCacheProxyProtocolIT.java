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
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.common.base.Objects;
import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.NamedCacheProtocol;

import com.oracle.coherence.grpc.messages.cache.v1.EnsureCacheRequest;
import com.oracle.coherence.grpc.messages.cache.v1.ExecuteRequest;
import com.oracle.coherence.grpc.messages.cache.v1.IndexRequest;
import com.oracle.coherence.grpc.messages.cache.v1.KeyOrFilter;
import com.oracle.coherence.grpc.messages.cache.v1.KeysOrFilter;
import com.oracle.coherence.grpc.messages.cache.v1.MapEventMessage;
import com.oracle.coherence.grpc.messages.cache.v1.MapListenerRequest;
import com.oracle.coherence.grpc.messages.cache.v1.NamedCacheRequest;
import com.oracle.coherence.grpc.messages.cache.v1.NamedCacheRequestType;
import com.oracle.coherence.grpc.messages.cache.v1.NamedCacheResponse;
import com.oracle.coherence.grpc.messages.cache.v1.PutAllRequest;
import com.oracle.coherence.grpc.messages.cache.v1.PutRequest;
import com.oracle.coherence.grpc.messages.cache.v1.QueryRequest;
import com.oracle.coherence.grpc.messages.cache.v1.ReplaceMappingRequest;
import com.oracle.coherence.grpc.messages.cache.v1.ResponseType;
import com.oracle.coherence.grpc.messages.common.v1.BinaryKeyAndValue;
import com.oracle.coherence.grpc.messages.common.v1.CollectionOfBytesValues;
import com.oracle.coherence.grpc.messages.common.v1.OptionalValue;
import com.oracle.coherence.grpc.messages.common.v1.ErrorMessage;
import com.oracle.coherence.grpc.messages.proxy.v1.InitRequest;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyRequest;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyResponse;

import com.oracle.coherence.grpc.proxy.common.ProxyServiceChannel;

import com.tangosol.io.Serializer;

import com.tangosol.net.NamedCache;
import com.tangosol.net.RequestIncompleteException;

import com.tangosol.net.cache.CacheEvent;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Extractors;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.aggregator.Count;
import com.tangosol.util.aggregator.ReducerAggregator;
import com.tangosol.util.comparator.SafeComparator;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.UniversalExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.InKeySetFilter;
import com.tangosol.util.filter.MapEventFilter;
import com.tangosol.util.processor.ExtractorProcessor;
import grpc.proxy.GetEntryExpiry;
import grpc.proxy.Person;
import grpc.proxy.PersonMapTrigger;
import grpc.proxy.TestStreamObserver;

import io.grpc.stub.StreamObserver;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.oracle.coherence.testing.matcher.CoherenceMatchers.cacheEvents;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasKey;

import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({"rawtypes", "unchecked", "DuplicatedCode", "resource", "UnusedReturnValue"})
public class NamedCacheProxyProtocolIT
        extends BaseVersionOneGrpcServiceIT
    {
    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldFailIfCacheNotEnsured(String ignored, Serializer serializer, String sScope) throws Exception
        {
        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        observer.awaitCount(1, 1, TimeUnit.MINUTES);

        RequestIncompleteException ex = assertThrows(RequestIncompleteException.class,
                () -> sendCacheRequest(channel, observer, 0, NamedCacheRequestType.Get, BytesValue.getDefaultInstance()));

        assertThat(ex.getMessage(), startsWith("Missing channel id in request"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldAddIndex(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String          sCacheName = "add-index-cache";
        NamedCache<?,?> cache      = ensureEmptyCache(sScope, sCacheName);

        Map<ValueExtractor, MapIndex> indexMap = removeIndexes(cache);

        ValueExtractor extractor    = new UniversalExtractor("foo");
        ByteString     binExtractor = toByteString(extractor, serializer);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        IndexRequest indexRequest = IndexRequest.newBuilder()
                .setAdd(true)
                .setExtractor(binExtractor)
                .setSorted(false)
                .build();

        sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Index, indexRequest);
        
        assertThat(indexMap, hasKey(extractor));
        assertThat(indexMap.get(extractor).isOrdered(), is(false));
        }



    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldAddSortedIndex(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String     sCacheName = "add-index-cache";
        NamedCache cache      = ensureEmptyCache(sScope, sCacheName);

        Map<ValueExtractor, MapIndex> indexMap = removeIndexes(cache);

        ValueExtractor extractor    = new UniversalExtractor("foo");
        ByteString     binExtractor = toByteString(extractor, serializer);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        IndexRequest indexRequest = IndexRequest.newBuilder()
                .setAdd(true)
                .setExtractor(binExtractor)
                .setSorted(true)
                .build();

        sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Index, indexRequest);
        
        assertThat(indexMap, hasKey(extractor));
        assertThat(indexMap.get(extractor).isOrdered(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldAddSortedIndexWithComparator(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String     sCacheName = "add-index-cache";
        NamedCache cache      = ensureEmptyCache(sScope, sCacheName);

        Map<ValueExtractor, MapIndex> indexMap = removeIndexes(cache);

        ValueExtractor extractor     = new UniversalExtractor("foo");
        ByteString binExtractor  = toByteString(extractor, serializer);
        Comparator comparator    = new SafeComparator(new UniversalExtractor("bar"));
        ByteString binComparator = toByteString(comparator, serializer);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        IndexRequest indexRequest = IndexRequest.newBuilder()
                .setAdd(true)
                .setExtractor(binExtractor)
                .setComparator(binComparator)
                .setSorted(true)
                .build();

        sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Index, indexRequest);

        assertThat(indexMap, hasKey(extractor));
        assertThat(indexMap.get(extractor).isOrdered(), is(true));
        assertThat(indexMap.get(extractor).getComparator(), is(comparator));
        }

    // ----- Aggregate ------------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallAggregateWithFilterExpectingSingleResult(String ignored, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "people";
        NamedCache<String, Person> cache      = ensureEmptyCache(sScope, sCacheName);

        cache.clear();

        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 35, "male");

        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        InvocableMap.EntryAggregator<String, Person, Integer> aggregator = new Count<>();

        Filter<Person> filter   = Filters.equal("age", 25);
        int            expected = cache.aggregate(filter, aggregator);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ExecuteRequest execute = ExecuteRequest.newBuilder()
                .setKeys(KeysOrFilter.newBuilder().setFilter(BinaryHelper.toByteString(filter, serializer)).build())
                .setAgent(BinaryHelper.toByteString(aggregator, serializer))
                .build();
        
        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Aggregate, execute);

        assertThat(list.size(), is(1));
        NamedCacheResponse response   = list.get(0);
        BytesValue         bytesValue = response.getMessage().unpack(BytesValue.class);
        Object             oResult    = BinaryHelper.fromBytesValue(bytesValue, serializer);
        assertThat(oResult, is(instanceOf(Integer.class)));
        assertThat(oResult, is(expected));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallAggregateWithFilterMatchingNoEntriesExpectingSingleResult(String     ignored,
                                                                                    Serializer serializer,
                                                                                    String     sScope)
            throws Exception
        {
        String                      sCacheName = "people";
        NamedCache<String, Person>  cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 35, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        InvocableMap.EntryAggregator<String, Person, Integer> aggregator = new Count<>();

        Filter<Person> filter    = Filters.equal("age", 100);
        int            cExpected = cache.aggregate(filter, aggregator);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);
        
        ExecuteRequest execute = ExecuteRequest.newBuilder()
                .setKeys(KeysOrFilter.newBuilder().setFilter(BinaryHelper.toByteString(filter, serializer)).build())
                .setAgent(BinaryHelper.toByteString(aggregator, serializer))
                .build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Aggregate, execute);

        assertThat(list.size(), is(1));
        NamedCacheResponse response   = list.get(0);
        BytesValue         bytesValue = response.getMessage().unpack(BytesValue.class);
        Object             oResult    = BinaryHelper.fromBytesValue(bytesValue, serializer);
        assertThat(oResult, is(instanceOf(Integer.class)));
        assertThat(oResult, is(cExpected));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallAggregateWithKeysExpectingSingleResult(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "people";
        NamedCache<String, Person> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 35, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        InvocableMap.EntryAggregator<String, Person, Integer> aggregator = new Count<>();

        List<String> listKeys = Arrays.asList(person1.getLastName(), person2.getLastName());
        List<ByteString> listSerializedKeys = listKeys.stream()
                .map(k -> BinaryHelper.toByteString(k, serializer))
                .collect(Collectors.toList());

        int nExpected = cache.aggregate(listKeys, aggregator);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        CollectionOfBytesValues keys = CollectionOfBytesValues.newBuilder()
                .addAllValues(listSerializedKeys)
                .build();

        ExecuteRequest execute = ExecuteRequest.newBuilder()
                .setKeys(KeysOrFilter.newBuilder().setKeys(keys).build())
                .setAgent(BinaryHelper.toByteString(aggregator, serializer))
                .build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Aggregate, execute);

        assertThat(list.size(), is(1));
        NamedCacheResponse response   = list.get(0);
        BytesValue         bytesValue = response.getMessage().unpack(BytesValue.class);
        Object             oResult    = BinaryHelper.fromBytesValue(bytesValue, serializer);
        assertThat(oResult, is(instanceOf(Integer.class)));
        assertThat(oResult, is(nExpected));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallAggregateWithKeysMatchingNoEntriesExpectingSingleResult(String ignored, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "people";
        NamedCache<String, Person> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 35, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        InvocableMap.EntryAggregator<String, Person, Integer> aggregator = new Count<>();

        List<String>     listKeys           = Arrays.asList("foo", "bar");
        List<ByteString> listSerializedKeys = listKeys.stream()
                .map(k -> BinaryHelper.toByteString(k, serializer))
                .collect(Collectors.toList());

        int nExpected = cache.aggregate(listKeys, aggregator);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        CollectionOfBytesValues keys = CollectionOfBytesValues.newBuilder()
                .addAllValues(listSerializedKeys)
                .build();

        ExecuteRequest execute = ExecuteRequest.newBuilder()
                .setKeys(KeysOrFilter.newBuilder().setKeys(keys).build())
                .setAgent(BinaryHelper.toByteString(aggregator, serializer))
                .build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Aggregate, execute);

        assertThat(list.size(), is(1));
        NamedCacheResponse response   = list.get(0);
        BytesValue         bytesValue = response.getMessage().unpack(BytesValue.class);
        Object             oResult    = BinaryHelper.fromBytesValue(bytesValue, serializer);
        assertThat(oResult, is(instanceOf(Integer.class)));
        assertThat(oResult, is(nExpected));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallAggregateWithNoKeysOrFilterExpectingSingleResult(String ignored, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "people";
        NamedCache<String, Person> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 35, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        InvocableMap.EntryAggregator<String, Person, Integer> aggregator = new Count<>();

        int nExpected = cache.aggregate(aggregator);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ExecuteRequest execute = ExecuteRequest.newBuilder()
                .setAgent(BinaryHelper.toByteString(aggregator, serializer))
                .build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Aggregate, execute);

        assertThat(list.size(), is(1));
        NamedCacheResponse response   = list.get(0);
        BytesValue         bytesValue = response.getMessage().unpack(BytesValue.class);
        Object             oResult    = BinaryHelper.fromBytesValue(bytesValue, serializer);
        assertThat(oResult, is(instanceOf(Integer.class)));
        assertThat(oResult, is(nExpected));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallAggregateWithFilterExpectingMapResult(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "people";
        NamedCache<String, Person> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 35, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        ValueExtractor<Person, String> extractor = Extractors.extract("getFirstName()");

        InvocableMap.EntryAggregator<String, Person, Map<String, String>> aggregator =
                new ReducerAggregator<>(extractor);

        Filter<Person>      filter      = Filters.equal("age", 25);
        Map<String, String> mapExpected = cache.aggregate(filter, aggregator);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ExecuteRequest execute = ExecuteRequest.newBuilder()
                .setKeys(KeysOrFilter.newBuilder().setFilter(BinaryHelper.toByteString(filter, serializer)).build())
                .setAgent(BinaryHelper.toByteString(aggregator, serializer))
                .build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Aggregate, execute);

        assertThat(list.size(), is(1));
        NamedCacheResponse response   = list.get(0);
        BytesValue         bytesValue = response.getMessage().unpack(BytesValue.class);
        Object             oResult    = BinaryHelper.fromBytesValue(bytesValue, serializer);
        assertThat(oResult, is(instanceOf(Map.class)));
        assertThat(oResult, is(mapExpected));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallAggregateWithKeysExpectingMapResult(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "people";
        NamedCache<String, Person> cache = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 35, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        ValueExtractor<Person, String> extractor = Extractors.extract("getFirstName()");

        InvocableMap.EntryAggregator<String, Person, Map<String, String>> aggregator =
                new ReducerAggregator<>(extractor);

        List<String>        listKeys           = Arrays.asList(person1.getLastName(), person2.getLastName());
        List<ByteString>    listSerializedKeys = listKeys.stream()
                .map(k -> BinaryHelper.toByteString(k, serializer))
                .collect(Collectors.toList());
        Map<String, String> nExpected          = cache.aggregate(listKeys, aggregator);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        CollectionOfBytesValues keys = CollectionOfBytesValues.newBuilder()
                .addAllValues(listSerializedKeys)
                .build();

        ExecuteRequest execute = ExecuteRequest.newBuilder()
                .setKeys(KeysOrFilter.newBuilder().setKeys(keys).build())
                .setAgent(BinaryHelper.toByteString(aggregator, serializer))
                .build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Aggregate, execute);

        assertThat(list.size(), is(1));
        NamedCacheResponse response   = list.get(0);
        BytesValue         bytesValue = response.getMessage().unpack(BytesValue.class);
        Object             oResult    = BinaryHelper.fromBytesValue(bytesValue, serializer);
        assertThat(oResult, is(instanceOf(Map.class)));
        assertThat(oResult, is(nExpected));
        }

    // ----- Clear ----------------------------------------------------------

    @ParameterizedTest(name = "{index} scope={0}")
    @MethodSource("serializers")
    public void shouldClearEmptyCache(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String     sCacheName = "test-cache";
        NamedCache cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Clear, Empty.getDefaultInstance());

        assertThat(cache.isEmpty(), is(true));
        }

    @ParameterizedTest(name = "{index} scope={0}")
    @MethodSource("serializers")
    public void shouldClearPopulatedCache(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 10);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Clear, Empty.getDefaultInstance());

        assertThat(cache.isEmpty(), is(true));
        }

    // ----- Contains Entry -------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldContainEntryWhenMappingPresent(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 3);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ByteString        key         = toByteString("key-1", serializer);
        ByteString        value       = toByteString("value-1", serializer);
        BinaryKeyAndValue keyAndValue = BinaryKeyAndValue.newBuilder()
                .setKey(key)
                .setValue(value)
                .build();

        NamedCacheResponse response = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.ContainsEntry, keyAndValue);

        BoolValue boolValue = response.getMessage().unpack(BoolValue.class);
        assertThat(boolValue.getValue(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldNotContainEntryWhenMappingHasDifferentValue(String ignored, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 3);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ByteString        key         = toByteString("key-1", serializer);
        ByteString        value       = toByteString("not-value-1", serializer);
        BinaryKeyAndValue keyAndValue = BinaryKeyAndValue.newBuilder()
                .setKey(key)
                .setValue(value)
                .build();

        NamedCacheResponse response = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.ContainsEntry, keyAndValue);

        BoolValue boolValue = response.getMessage().unpack(BoolValue.class);
        assertThat(boolValue.getValue(), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldNotContainEntryWhenMappingNotPresent(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 3);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ByteString        key         = toByteString("key-100", serializer);
        ByteString        value       = toByteString("value-100", serializer);
        BinaryKeyAndValue keyAndValue = BinaryKeyAndValue.newBuilder()
                .setKey(key)
                .setValue(value)
                .build();

        NamedCacheResponse response = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.ContainsEntry, keyAndValue);

        BoolValue boolValue = response.getMessage().unpack(BoolValue.class);
        assertThat(boolValue.getValue(), is(false));
        }

    // ----- Contains Key ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReturnTrueForContainsKeyWithExistingMapping(String ignored, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 5);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ByteString  key   = toByteString("key-2", serializer);
        BytesValue binKey = BytesValue.newBuilder().setValue(key).build();

        NamedCacheResponse response = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.ContainsKey, binKey);

        BoolValue boolValue = response.getMessage().unpack(BoolValue.class);
        assertThat(boolValue.getValue(), is(true));
        }


    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReturnFalseForContainsKeyWithNonExistentMapping(String ignored, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 5);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ByteString key    = toByteString("bad-key", serializer);
        BytesValue binKey = BytesValue.newBuilder().setValue(key).build();

        NamedCacheResponse response = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.ContainsKey, binKey);

        BoolValue boolValue = response.getMessage().unpack(BoolValue.class);
        assertThat(boolValue.getValue(), is(false));
        }

    // ----- Contains Value -------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldContainValueWhenValuePresent(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 3);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ByteString  value   = toByteString("value-2", serializer);
        BytesValue binValue = BytesValue.newBuilder().setValue(value).build();

        NamedCacheResponse response = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.ContainsValue, binValue);

        BoolValue boolValue = response.getMessage().unpack(BoolValue.class);
        assertThat(boolValue.getValue(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldContainValueWhenValuePresentMultipleTimes(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.put("key-1", "value-1");
        cache.put("key-2", "value-2");
        cache.put("key-11", "value-1");
        cache.put("key-22", "value-2");

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ByteString value    = toByteString("value-2", serializer);
        BytesValue binValue = BytesValue.newBuilder().setValue(value).build();

        NamedCacheResponse response = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.ContainsValue, binValue);

        BoolValue boolValue = response.getMessage().unpack(BoolValue.class);
        assertThat(boolValue.getValue(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldNotContainValueWhenMappingNotPresent(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 3);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ByteString  value   = toByteString("value-100", serializer);
        BytesValue binValue = BytesValue.newBuilder().setValue(value).build();

        NamedCacheResponse response = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.ContainsValue, binValue);

        BoolValue boolValue = response.getMessage().unpack(BoolValue.class);
        assertThat(boolValue.getValue(), is(false));
        }

    // ----- Destroy --------------------------------------------------------

    @ParameterizedTest(name = "{index} scope={0}")
    @MethodSource("serializers")
    public void shouldDestroyCache(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String     sCacheName = "test-cache";
        NamedCache cache      = ensureEmptyCache(sScope, sCacheName);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        int count = observer.valueCount();
        sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Destroy, Empty.getDefaultInstance());

        Optional<NamedCacheResponse> destroyedEvent = observer.values()
                .stream()
                .skip(count)
                .filter(ProxyResponse::hasMessage)
                .map(m ->
                    {
                    try
                        {
                        return m.getMessage().unpack(NamedCacheResponse.class);
                        }
                    catch (InvalidProtocolBufferException e)
                        {
                        throw new RuntimeException(e);
                        }
                    })
                .filter(m -> m.getCacheId() == cacheId)
                .filter(m -> m.getType() == ResponseType.Destroyed)
                .findFirst();

        Eventually.assertDeferred(cache::isDestroyed, is(true));
        assertThat(destroyedEvent.isPresent(), is(true));
        }

    // ----- entrySet(Filter) -----------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallEntrySetWithFilterWhenSomeEntriesMatch(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "people";
        NamedCache<String, Person> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 50, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        Filter<Person>                 filter   = new EqualsFilter<>("getAge", 25);
        Set<Map.Entry<String, Person>> expected = cache.entrySet(filter);

        ByteString   filterBytes  = BinaryHelper.toByteString(filter, serializer);
        QueryRequest queryRequest = QueryRequest.newBuilder().setFilter(filterBytes).build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.QueryEntries, queryRequest);

        observer.assertComplete()
                .assertNoErrors();

        Map<String, Person> oResult = toMap(list, serializer);
        assertThat(oResult.entrySet(), is(expected));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallEntrySetWithFilterWhenAllEntriesMatch(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "people";
        NamedCache<String, Person> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 50, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        Filter<Person>                 filter   = Filters.always();
        Set<Map.Entry<String, Person>> expected = cache.entrySet(filter);

        ByteString   filterBytes  = BinaryHelper.toByteString(filter, serializer);
        QueryRequest queryRequest = QueryRequest.newBuilder().setFilter(filterBytes).build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.QueryEntries, queryRequest);

        observer.assertComplete()
                .assertNoErrors();

        Map<String, Person> oResult = toMap(list, serializer);
        assertThat(oResult.entrySet(), is(expected));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallEntrySetWithFilterWhenNoEntriesMatch(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "people";
        NamedCache<String, Person> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 50, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        Filter<Person>                 filter   = new EqualsFilter<>("getAge", 100);
        Set<Map.Entry<String, Person>> expected = cache.entrySet(filter);

        ByteString   filterBytes  = BinaryHelper.toByteString(filter, serializer);
        QueryRequest queryRequest = QueryRequest.newBuilder().setFilter(filterBytes).build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.QueryEntries, queryRequest);

        observer.assertComplete()
                .assertNoErrors();

        Map<String, Person> oResult = toMap(list, serializer);
        assertThat(oResult.entrySet(), is(expected));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    public void shouldCallEntrySetWithFilterAndComparatorWhenSomeEntriesMatch(String ignored, Serializer serializer, String sScope)
            throws Exception
        {
        String                      sCacheName = "numbers";
        NamedCache<String, Integer> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        for (int i = 0; i < 100; i++)
            {
            cache.put("key-" + i, i);
            }

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        Filter<Integer>     filter     = Filters.greater(Extractors.identity(), 50);
        Comparator<Integer> comparator = (Comparator<Integer>) IdentityExtractor.INSTANCE().fromKey();

        Set<Map.Entry<String, Integer>>  expected     = cache.entrySet(filter, comparator);
        List<Map.Entry<String, Integer>> expectedList = new ArrayList<>(expected);
        expectedList.sort(Map.Entry.comparingByKey());

        ByteString   filterBytes     = BinaryHelper.toByteString(filter, serializer);
        ByteString   comparatorBytes = BinaryHelper.toByteString(comparator, serializer);
        QueryRequest queryRequest    = QueryRequest.newBuilder()
                .setFilter(filterBytes)
                .setComparator(comparatorBytes)
                .build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.QueryEntries, queryRequest);

        List<Map.Entry<String, Integer>> listResult = toListOfEntries(list, serializer);
        listResult.sort(Map.Entry.comparingByKey());
        assertThat(listResult.size(), is(expectedList.size()));
        assertThat(listResult, is(expectedList));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    public void shouldCallEntrySetWithFilterAndComparatorWhenAllEntriesMatch(String ignored, Serializer serializer, String sScope)
            throws Exception
        {
        String                      sCacheName = "numbers";
        NamedCache<String, Integer> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        for (int i = 0; i < 100; i++)
            {
            cache.put("key-" + i, i);
            }

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        Filter<Integer>     filter     = Filters.always();
        Comparator<Integer> comparator = IdentityExtractor.INSTANCE();

        Set<Map.Entry<String, Integer>>  expected     = cache.entrySet(filter, comparator);
        List<Map.Entry<String, Integer>> expectedList = new ArrayList<>(expected);
        expectedList.sort(Map.Entry.comparingByKey());

        ByteString   filterBytes     = BinaryHelper.toByteString(filter, serializer);
        ByteString   comparatorBytes = BinaryHelper.toByteString(comparator, serializer);
        QueryRequest queryRequest    = QueryRequest.newBuilder()
                .setFilter(filterBytes)
                .setComparator(comparatorBytes)
                .build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.QueryEntries, queryRequest);

        List<Map.Entry<String, Integer>> listResult = toListOfEntries(list, serializer);
        listResult.sort(Map.Entry.comparingByKey());
        assertThat(listResult.size(), is(expectedList.size()));
        assertThat(listResult, is(expectedList));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    public void shouldCallEntrySetWithFilterAndComparatorWhenNoEntriesMatch(String ignored, Serializer serializer, String sScope)
            throws Exception
        {
        String                      sCacheName = "numbers";
        NamedCache<String, Integer> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        for (int i = 0; i < 100; i++)
            {
            cache.put("key-" + i, i);
            }

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        Filter<Integer>     filter     = Filters.greater(Extractors.identity(), 500);
        Comparator<Integer> comparator = IdentityExtractor.INSTANCE();

        Set<Map.Entry<String, Integer>>  expected     = cache.entrySet(filter, comparator);
        List<Map.Entry<String, Integer>> expectedList = new ArrayList<>(expected);

        ByteString   filterBytes     = BinaryHelper.toByteString(filter, serializer);
        ByteString   comparatorBytes = BinaryHelper.toByteString(comparator, serializer);
        QueryRequest queryRequest    = QueryRequest.newBuilder()
                .setFilter(filterBytes)
                .setComparator(comparatorBytes)
                .build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.QueryEntries, queryRequest);

        List<Map.Entry<String, Integer>> listResult = toListOfEntries(list, serializer);
        assertThat(listResult, is(expectedList));
        }

    // ----- Events ---------------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReceiveTruncateEvent(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-events-" + System.currentTimeMillis();
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.put("foo", "bar");

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);
        int count = observer.valueCount();

        cache.truncate();

        Optional<NamedCacheResponse> truncatedEvent = observer.values()
                .stream()
                .skip(count)
                .filter(ProxyResponse::hasMessage)
                .map(m ->
                    {
                    try
                        {
                        return m.getMessage().unpack(NamedCacheResponse.class);
                        }
                    catch (InvalidProtocolBufferException e)
                        {
                        throw new RuntimeException(e);
                        }
                    })
                .filter(m -> m.getCacheId() == cacheId)
                .filter(m -> m.getType() == ResponseType.Truncated)
                .findFirst();

        assertThat(cache.size(), is(0));
        assertThat(truncatedEvent.isPresent(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldSubscribeToAllEvents(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                                sCacheName = "test-events-" + System.currentTimeMillis();
        CollectingMapListener<String, String> listener   = new CollectingMapListener<>();
        MapEventFilter<String, String>        filter     = new MapEventFilter<>(MapEventFilter.E_ALL, Filters.always());
        NamedCache<String, String>            cache      = ensureEmptyCache(sScope, sCacheName);

        cache.clear();
        cache.addMapListener(listener, filter, false);

        ListenableStreamObserver     observer = new ListenableStreamObserver();
        StreamObserver<ProxyRequest> channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        try
            {
            // subscribe to all events
            ByteString         filterBytes = BinaryHelper.toByteString(filter, serializer);
            long               nFilterId   = 19L;
            MapListenerRequest request     = MapListenerRequest.newBuilder()
                    .setSubscribe(true)
                    .setFilterId(nFilterId)
                    .setKeyOrFilter(KeyOrFilter.newBuilder().setFilter(filterBytes).build())
                    .build();

            sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.MapListener, request);

            CollectingMapListener<String, String> listenerActual
                    = new CollectingMapListener<>();

            observer.addListener(cache, listenerActual, serializer);

            // update the cache to generate events
            for (int i = 0; i < 10; i++)
                {
                cache.put("key-" + i, "value-" + i);
                }
            for (int i = 0; i < 10; i++)
                {
                cache.put("key-" + i, "value-" + i + "-updated");
                }
            for (int i = 0; i < 10; i++)
                {
                cache.remove("key-" + i);
                }

            // wait for the events
            Eventually.assertDeferred(listener::count, is(30));
            Eventually.assertDeferred(listenerActual::count, is(30));
            List<MapEvent<String, String>> expectedEvents = listener.safeValues();
            List<MapEvent<String, String>> actualEvents   = listenerActual.safeValues();
            assertThat(actualEvents, cacheEvents(expectedEvents));
            }
        finally
            {
            cache.removeMapListener(listener, filter);
            }
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldSubscribeToSingleEventForSingleKey(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                                                            sCacheName = "test-events-" + System.currentTimeMillis();
        CollectingMapListener<String, String> listener   = new CollectingMapListener<>();
        NamedCache<String, String>                                        cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.addMapListener(listener, "key-2", false);

        ListenableStreamObserver     observer = new ListenableStreamObserver();
        StreamObserver<ProxyRequest> channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        try
            {
            // subscribe to events for key-2
            ByteString         keyBytes = BinaryHelper.toByteString("key-2", serializer);
            MapListenerRequest request     = MapListenerRequest.newBuilder()
                    .setSubscribe(true)
                    .setKeyOrFilter(KeyOrFilter.newBuilder().setKey(keyBytes).build())
                    .build();

            sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.MapListener, request);

            CollectingMapListener<String, String> listenerActual
                    = new CollectingMapListener<>();

            observer.addListener(cache, listenerActual, serializer);

            // update the cache to generate events
            cache.put("key-2", "value-2");

            // wait for the events
            Eventually.assertDeferred(listener::count, is(1));
            Eventually.assertDeferred(listenerActual::count, is(1));
            List<MapEvent<String, String>> expectedEvents = listener.safeValues();
            List<MapEvent<String, String>> actualEvents   = listenerActual.safeValues();
            assertThat(actualEvents, cacheEvents(expectedEvents));
            }
        finally
            {
            cache.removeMapListener(listener, "key-2");
            }
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldSubscribeToAllEventsForSingleKey(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                                                            sCacheName = "test-events-" + System.currentTimeMillis();
        CollectingMapListener<String, String> listener   = new CollectingMapListener<>();
        NamedCache<String, String>                                        cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.addMapListener(listener, "key-2", false);

        ListenableStreamObserver     observer = new ListenableStreamObserver();
        StreamObserver<ProxyRequest> channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        try
            {
            // subscribe to events for key-2
            ByteString         keyBytes = BinaryHelper.toByteString("key-2", serializer);
            MapListenerRequest request  = MapListenerRequest.newBuilder()
                    .setSubscribe(true)
                    .setKeyOrFilter(KeyOrFilter.newBuilder().setKey(keyBytes).build())
                    .build();

            sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.MapListener, request);

            CollectingMapListener<String, String> listenerActual
                    = new CollectingMapListener<>();

            observer.addListener(cache, listenerActual, serializer);

            // update the cache to generate events
            for (int i = 0; i < 10; i++)
                {
                cache.put("key-" + i, "value-" + i);
                }
            for (int i = 0; i < 10; i++)
                {
                cache.put("key-" + i, "value-" + i + "-updated");
                }
            for (int i = 0; i < 10; i++)
                {
                cache.remove("key-" + i);
                }

            // wait for the events
            Eventually.assertDeferred(listener::count, is(3));
            Eventually.assertDeferred(listenerActual::count, is(3));
            List<MapEvent<String, String>> expectedEvents = listener.safeValues();
            List<MapEvent<String, String>> actualEvents   = listenerActual.safeValues();
            assertThat(actualEvents, cacheEvents(expectedEvents));
            }
        finally
            {
            cache.removeMapListener(listener, "key-2");
            }
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldAddPrimingListenerForNonExistentKey(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-events-" + System.currentTimeMillis();
        String                     key        = "key-2";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        ListenableStreamObserver     observer = new ListenableStreamObserver();
        StreamObserver<ProxyRequest> channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        // subscribe to events for key-2
        ByteString         keyBytes = BinaryHelper.toByteString(key, serializer);
        MapListenerRequest request  = MapListenerRequest.newBuilder()
                .setSubscribe(true)
                .setPriming(true)
                .setKeyOrFilter(KeyOrFilter.newBuilder().setKey(keyBytes).build())
                .build();

        CollectingMapListener<String, String> listenerActual
                = new CollectingMapListener<>();

        observer.addListener(cache, listenerActual, serializer);

        sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.MapListener, request);

        Eventually.assertDeferred(listenerActual::count, is(1));

        MapEvent<String, String> event = listenerActual.safeValues().get(0);
        assertThat(event, is(instanceOf(CacheEvent.class)));
        CacheEvent<?, ?> cacheEvent = (CacheEvent<?, ?>) event;

        assertThat(cacheEvent.getKey(), is(key));
        assertThat(cacheEvent.isPriming(), is(true));
        assertThat(cacheEvent.isSynthetic(), is(true));
        assertThat(cacheEvent.getOldValue(), is(nullValue()));
        assertThat(cacheEvent.getNewValue(), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldAddPrimingListenerForExistingKey(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-events-" + System.currentTimeMillis();
        String                     key        = "key-2";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.put(key, "value-2");

        ListenableStreamObserver     observer = new ListenableStreamObserver();
        StreamObserver<ProxyRequest> channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        // subscribe to events for key-2
        ByteString         keyBytes = BinaryHelper.toByteString(key, serializer);
        MapListenerRequest request  = MapListenerRequest.newBuilder()
                .setSubscribe(true)
                .setPriming(true)
                .setKeyOrFilter(KeyOrFilter.newBuilder().setKey(keyBytes).build())
                .build();

        CollectingMapListener<String, String> listenerActual
                = new CollectingMapListener<>();

        observer.addListener(cache, listenerActual, serializer);

        sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.MapListener, request);

        Eventually.assertDeferred(listenerActual::count, is(1));

        MapEvent<String, String> event = listenerActual.safeValues().get(0);
        assertThat(event, is(instanceOf(CacheEvent.class)));
        CacheEvent<?, ?> cacheEvent = (CacheEvent<?, ?>) event;

        assertThat(cacheEvent.getKey(), is(key));
        assertThat(cacheEvent.isPriming(), is(true));
        assertThat(cacheEvent.isSynthetic(), is(true));
        assertThat(cacheEvent.getOldValue(), is(nullValue()));
        assertThat(cacheEvent.getNewValue(), is("value-2"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldAddMapTrigger(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-events-" + System.currentTimeMillis();
        NamedCache<String, Person> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        ListenableStreamObserver     observer = new ListenableStreamObserver();
        StreamObserver<ProxyRequest> channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        String     sKey         = "iron-man";
        ByteString triggerBytes = BinaryHelper.toByteString(new PersonMapTrigger(), serializer);

        MapListenerRequest requestAdd = MapListenerRequest.newBuilder()
                .setSubscribe(true)
                .setTrigger(triggerBytes)
                .build();

        CollectingMapListener<String, Person> listenerActual
                = new CollectingMapListener<>();

        observer.addListener(cache, listenerActual, serializer);

        sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.MapListener, requestAdd);

        // with the trigger added the person's last name should be converted to upper case
        Person person = new Person("Tony", "Stark", 53, "male");
        cache.put(sKey, person);
        Person cached = cache.get(sKey);
        assertThat(cached.getLastName(), is(person.getLastName().toUpperCase()));

        // Now remove the trigger
        MapListenerRequest requestRemove = MapListenerRequest.newBuilder()
                .setSubscribe(false)
                .setTrigger(triggerBytes)
                .build();

        sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.MapListener, requestRemove);

        // with the trigger removed the person's last name should be unchanged
        cache.put(sKey, person);
        cached = cache.get(sKey);
        assertThat(cached.getLastName(), is(person.getLastName()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldSubscribeToEventsForMultipleKeys(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                                                            sCacheName = "test-events-" + System.currentTimeMillis();
        CollectingMapListener<String, String> listener   = new CollectingMapListener<>();
        NamedCache<String, String>                                        cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.addMapListener(listener, "key-2", false);
        cache.addMapListener(listener, "key-4", false);

        ListenableStreamObserver     observer = new ListenableStreamObserver();
        StreamObserver<ProxyRequest> channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);


        try
            {
            // subscribe to events for key-2
            ByteString         keyBytes = BinaryHelper.toByteString("key-2", serializer);
            MapListenerRequest request  = MapListenerRequest.newBuilder()
                    .setSubscribe(true)
                    .setKeyOrFilter(KeyOrFilter.newBuilder().setKey(keyBytes).build())
                    .build();

            sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.MapListener, request);

            // subscribe to events for key-4
            keyBytes = BinaryHelper.toByteString("key-4", serializer);
            request  = MapListenerRequest.newBuilder()
                    .setSubscribe(true)
                    .setKeyOrFilter(KeyOrFilter.newBuilder().setKey(keyBytes).build())
                    .build();

            sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.MapListener, request);

            CollectingMapListener<String, String> listenerActual
                    = new CollectingMapListener<>();

            observer.addListener(cache, listenerActual, serializer);

            // update the cache to generate events
            for (int i = 0; i < 10; i++)
                {
                cache.put("key-" + i, "value-" + i);
                }
            for (int i = 0; i < 10; i++)
                {
                cache.put("key-" + i, "value-" + i + "-updated");
                }
            for (int i = 0; i < 10; i++)
                {
                cache.remove("key-" + i);
                }

            // wait for the events
            Eventually.assertDeferred(listenerActual::count, is(6));
            Eventually.assertDeferred(listener::count, is(6));
            List<MapEvent<String, String>> expectedEvents = listener.safeValues();
            List<MapEvent<String, String>> actualEvents   = listenerActual.safeValues();
            assertThat(actualEvents, cacheEvents(expectedEvents));
            }
        finally
            {
            cache.removeMapListener(listener, "key-2");
            cache.removeMapListener(listener, "key-4");
            }
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldSubscribeToEventsForFilter(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                                                             sCacheName = "test-events-" + System.currentTimeMillis();
        CollectingMapListener<String, Integer> listener   = new CollectingMapListener<>();
        NamedCache<String, Integer>                                        cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        MapEventFilter<String, Integer> filter      = new MapEventFilter<>(Filters.less(Extractors.identity(), 10));
        ByteString                      filterBytes = BinaryHelper.toByteString(filter, serializer);
        cache.addMapListener(listener, filter, false);

        ListenableStreamObserver     observer = new ListenableStreamObserver();
        StreamObserver<ProxyRequest> channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        try
            {
            // subscribe to all events
            long               nFilterId = 19L;
            MapListenerRequest request   = MapListenerRequest.newBuilder()
                    .setSubscribe(true)
                    .setFilterId(nFilterId)
                    .setKeyOrFilter(KeyOrFilter.newBuilder().setFilter(filterBytes).build())
                    .build();

            CollectingMapListener<String, Integer> listenerActual
                    = new CollectingMapListener<>();

            observer.addListener(cache, listenerActual, serializer);

            sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.MapListener, request);

            // update the cache to generate events
            for (int i = 0; i < 20; i++)
                {
                cache.put("key-" + i, i);
                }
            for (int i = 0; i < 20; i++)
                {
                cache.put("key-" + i, i + 100);
                }
            for (int i = 0; i < 20; i++)
                {
                cache.remove("key-" + i);
                }

            // wait for the events
            Eventually.assertDeferred(listener::count, is(20));
            Eventually.assertDeferred(listenerActual::count, is(20));
            List<MapEvent<String, Integer>> expectedEvents = listener.safeValues();
            List<MapEvent<String, Integer>> actualEvents   = listenerActual.safeValues();
            assertThat(actualEvents, cacheEvents(expectedEvents));
            }
        finally
            {
            cache.removeMapListener(listener, filter);
            }
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldSubscribeToEventsForKeyAndFilter(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                                sCacheName = "test-events-" + System.currentTimeMillis();
        String                                                            key         = "key-2";
        CollectingMapListener<String, Person> listenerOne = new CollectingMapListener<>();
        CollectingMapListener<String, Person> listenerTwo = new CollectingMapListener<>();
        NamedCache<String, Person>                                        cache       = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        MapEventFilter<String, Person> filter      = new MapEventFilter<>(Filters.equal(
                Extractors.extract("getAge()"), 10));
        ByteString                     filterBytes = BinaryHelper.toByteString(filter, serializer);
        ByteString                     keyBytes    = BinaryHelper.toByteString(key, serializer);
        cache.addMapListener(listenerOne, filter, false);
        cache.addMapListener(listenerTwo, key, false);

        ListenableStreamObserver     observer = new ListenableStreamObserver();
        StreamObserver<ProxyRequest> channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        try
            {
            // subscribe to all events
            long               lFilterIdOne = 19L;
            MapListenerRequest requestOne   = MapListenerRequest.newBuilder()
                    .setSubscribe(true)
                    .setFilterId(lFilterIdOne)
                    .setKeyOrFilter(KeyOrFilter.newBuilder().setFilter(filterBytes).build())
                    .build();

            MapListenerRequest requestTwo   = MapListenerRequest.newBuilder()
                    .setSubscribe(true)
                    .setKeyOrFilter(KeyOrFilter.newBuilder().setKey(keyBytes).build())
                    .build();

            CollectingMapListener<String, Person> listenerActual
                    = new CollectingMapListener<>();

            observer.addListener(cache, listenerActual, serializer);

            sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.MapListener, requestOne);
            sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.MapListener, requestTwo);

            // update the cache to generate events
            cache.put("key-1", new Person("first", "last", 10, ""));
            cache.put(key, new Person("first", "last", 20, ""));

            // wait for the events
            listenerOne.awaitCount(1);
            listenerTwo.awaitCount(1);
            Eventually.assertDeferred(listenerActual::count, is(2));

            List<MapEvent<String, Person>> expectedEvents = new ArrayList<>();
            expectedEvents.addAll(listenerOne.safeValues());
            expectedEvents.addAll(listenerTwo.safeValues());
            List<MapEvent<String, Person>> actualEvents = listenerActual.safeValues();
            assertThat(actualEvents, cacheEvents(expectedEvents));
            }
        finally
            {
            cache.removeMapListener(listenerOne, filter);
            cache.removeMapListener(listenerTwo, key);
            }
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldSubscribeToEventsForMultipleFilters(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                                sCacheName  = "test-events-" + System.currentTimeMillis();
        CollectingMapListener<String, Person> listenerOne = new CollectingMapListener<>();
        CollectingMapListener<String, Person> listenerTwo = new CollectingMapListener<>();
        NamedCache<String, Person>            cache       = ensureEmptyCache(sScope, sCacheName);
        MapEventFilter<String, Person>        filterOne   = new MapEventFilter<>(Filters.equal(Extractors.extract("getAge()"), 10));
        MapEventFilter<String, Person>        filterTwo   = new MapEventFilter<>(Filters.equal(Extractors.extract("getAge()"), 20));

        cache.clear();
        cache.addMapListener(listenerOne, filterOne, false);
        cache.addMapListener(listenerTwo, filterTwo, false);

        ListenableStreamObserver     observer = new ListenableStreamObserver();
        StreamObserver<ProxyRequest> channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        try
            {
            long               nFilterIdOne   = 19L;
            ByteString         filterBytesOne = BinaryHelper.toByteString(filterOne, serializer);
            MapListenerRequest requestOne     = MapListenerRequest.newBuilder()
                    .setSubscribe(true)
                    .setFilterId(nFilterIdOne)
                    .setKeyOrFilter(KeyOrFilter.newBuilder().setFilter(filterBytesOne).build())
                    .build();

            long               nFilterIdTwo   = 20L;
            ByteString         filterBytesTwo = BinaryHelper.toByteString(filterTwo, serializer);
            MapListenerRequest requestTwo     = MapListenerRequest.newBuilder()
                    .setSubscribe(true)
                    .setFilterId(nFilterIdTwo)
                    .setKeyOrFilter(KeyOrFilter.newBuilder().setFilter(filterBytesTwo).build())
                    .build();
            
            CollectingMapListener<String, Person> listenerActual = new CollectingMapListener<>();
            observer.addListener(cache, listenerActual, serializer);

            sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.MapListener, requestOne);
            sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.MapListener, requestTwo);
            
            // update the cache to generate events
            cache.put("key-1", new Person("first", "last", 10, ""));
            cache.put("key-2", new Person("first", "last", 20, ""));

            // wait for the events
            Eventually.assertDeferred(listenerActual::count, is(2));
            listenerOne.awaitCount(1);
            listenerTwo.awaitCount(1);

            List<MapEventMessage> eventsOne = eventsFrom(observer.safeValues(), nFilterIdOne);
            List<MapEventMessage> eventsTwo = eventsFrom(observer.safeValues(), nFilterIdTwo);
            assertThat(eventsOne.size(), is(1));
            assertThat(eventsTwo.size(), is(1));

            List<MapEvent<String, Person>> expectedEvents = new ArrayList<>();
            expectedEvents.addAll(listenerOne.safeValues());
            expectedEvents.addAll(listenerTwo.safeValues());
            List<MapEvent<String, Person>> actualEvents = listenerActual.safeValues();
            assertThat(actualEvents, cacheEvents(expectedEvents));
            }
        finally
            {
            cache.removeMapListener(listenerOne, filterOne);
            cache.removeMapListener(listenerTwo, filterTwo);
            }
        }


    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldSubscribeToEventsForMultipleOverlappingFilters(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                                sCacheName  = "test-events-" + System.currentTimeMillis();
        CollectingMapListener<String, Person> listenerOne = new CollectingMapListener<>();
        CollectingMapListener<String, Person> listenerTwo = new CollectingMapListener<>();
        NamedCache<String, Person>            cache       = ensureEmptyCache(sScope, sCacheName);
        MapEventFilter<String, Person>        filterOne   = new MapEventFilter<>(AlwaysFilter.INSTANCE());
        MapEventFilter<String, Person>        filterTwo   = new MapEventFilter<>(Filters.equal(Extractors.extract("getAge()"), 20));

        cache.clear();
        cache.addMapListener(listenerOne, filterOne, false);
        cache.addMapListener(listenerTwo, filterTwo, false);

        ListenableStreamObserver     observer = new ListenableStreamObserver();
        StreamObserver<ProxyRequest> channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        try
            {
            long               nFilterIdOne   = 19L;
            ByteString         filterBytesOne = BinaryHelper.toByteString(filterOne, serializer);
            MapListenerRequest requestOne     = MapListenerRequest.newBuilder()
                    .setSubscribe(true)
                    .setFilterId(nFilterIdOne)
                    .setKeyOrFilter(KeyOrFilter.newBuilder().setFilter(filterBytesOne).build())
                    .build();

            long               nFilterIdTwo   = 20L;
            ByteString         filterBytesTwo = BinaryHelper.toByteString(filterTwo, serializer);
            MapListenerRequest requestTwo     = MapListenerRequest.newBuilder()
                    .setSubscribe(true)
                    .setFilterId(nFilterIdTwo)
                    .setKeyOrFilter(KeyOrFilter.newBuilder().setFilter(filterBytesTwo).build())
                    .build();

            CollectingMapListener<String, Person> listenerActual = new CollectingMapListener<>();
            observer.addListener(cache, listenerActual, serializer);

            sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.MapListener, requestOne);
            sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.MapListener, requestTwo);

            // update the cache to generate events
            cache.put("key-1", new Person("first", "last", 10, ""));
            cache.put("key-2", new Person("first", "last", 20, ""));

            // wait for the events
            Eventually.assertDeferred(listenerActual::count, is(2));
            listenerOne.awaitCount(2);
            listenerTwo.awaitCount(1);

            List<MapEventMessage> eventsOne = eventsFrom(observer.safeValues(), nFilterIdOne);
            List<MapEventMessage> eventsTwo = eventsFrom(observer.safeValues(), nFilterIdTwo);
            assertThat(eventsOne.size(), is(2));
            assertThat(eventsTwo.size(), is(1));
            }
        finally
            {
            cache.removeMapListener(listenerOne, filterOne);
            cache.removeMapListener(listenerTwo, filterTwo);
            }
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldAddPrimingListenerWithFilter(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-events-" + System.currentTimeMillis();
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.put("key-4", "value-4");

        ListenableStreamObserver     observer = new ListenableStreamObserver();
        StreamObserver<ProxyRequest> channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);
        // Only InKeySet filters are allowed for a priming listener
        Set<String>            keys        = new LinkedHashSet<>(Arrays.asList("key-2", "key-4"));
        InKeySetFilter<String> filter      = new InKeySetFilter<>(Filters.always(), keys);
        ByteString             filterBytes = BinaryHelper.toByteString(filter, serializer);
        long                   nFilterId   = 19L;
        MapListenerRequest     request     = MapListenerRequest.newBuilder()
                                                    .setSubscribe(true)
                                                    .setFilterId(nFilterId)
                                                    .setKeyOrFilter(KeyOrFilter.newBuilder().setFilter(filterBytes).build())
                                                    .setPriming(true)
                                                    .build();

        CollectingMapListener<String, String> listenerActual = new CollectingMapListener<>();
        observer.addListener(cache, listenerActual, serializer);

        sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.MapListener, request);

        Eventually.assertDeferred(listenerActual::count, is(2));
        // priming events will be for a key and not have a filter id
        List<MapEventMessage> events = eventsFrom(observer.safeValues());

        assertThat(events.size(), is(2));
        MapEventMessage messageOne = events.stream()
                .filter(e -> Objects.equals("key-2", BinaryHelper.fromByteString(e.getKey(), serializer)))
                .findFirst()
                .orElse(null);

        assertThat(messageOne, is(notNullValue()));
        assertThat(messageOne.getPriming(),   is(true));
        assertThat(messageOne.getSynthetic(), is(true));
        assertThat(BinaryHelper.fromByteString(messageOne.getOldValue(), serializer),  is(nullValue()));
        assertThat(BinaryHelper.fromByteString(messageOne.getNewValue(), serializer),  is(nullValue()));

        MapEventMessage messageTwo = events.stream()
                .filter(e -> Objects.equals("key-4", BinaryHelper.fromByteString(e.getKey(), serializer)))
                .findFirst()
                .orElse(null);

        assertThat(messageTwo, is(notNullValue()));
        assertThat(messageTwo.getPriming(),   is(true));
        assertThat(messageTwo.getSynthetic(), is(true));
        assertThat(BinaryHelper.fromByteString(messageTwo.getOldValue(), serializer),  is(nullValue()));
        assertThat(messageTwo.getNewValue(),  is(BinaryHelper.toByteString("value-4", serializer)));

        // insert a value for key-2 should receive another event
        cache.put("key-2", "value-2");
        Eventually.assertDeferred(listenerActual::count, is(3));
        events = eventsFrom(observer.safeValues());
        assertThat(events.size(), is(3));

        // remove the listener
        request = MapListenerRequest.newBuilder()
                                    .setSubscribe(false)
                                    .setFilterId(nFilterId)
                                    .setKeyOrFilter(KeyOrFilter.newBuilder().setFilter(filterBytes).build())
                                    .setPriming(true)
                                    .build();

        sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.MapListener, request);

        // insert a value for key-2 should not receive another event
        cache.put("key-2", "value-2.2");
        cache.put("key-4", "value-4.2");
        Eventually.assertDeferred(listenerActual::count, is(3));
        events = eventsFrom(observer.safeValues());
        assertThat(events.size(), is(3));
        }

    // ----- Get ------------------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldGetExistingKey(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);

        cache.clear();
        cache.put("key-1", "value-1");

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        Binary     binary  = ExternalizableHelper.toBinary("key-1", serializer);
        ByteString key     = BinaryHelper.toByteString(binary);
        BytesValue request = BytesValue.newBuilder().setValue(key).build();

        NamedCacheResponse response = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Get, request);
        OptionalValue      value    = response.getMessage().unpack(OptionalValue.class);
        assertThat(value, is(notNullValue()));
        assertThat(value.getPresent(), is(true));
        assertThat(fromByteString(value.getValue(), serializer, String.class), is("value-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldGetExistingKeyMappedToNull(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.put("key-1", null);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        Binary     binary  = ExternalizableHelper.toBinary("key-1", serializer);
        ByteString key     = BinaryHelper.toByteString(binary);
        BytesValue request = BytesValue.newBuilder().setValue(key).build();

        NamedCacheResponse response = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Get, request);
        OptionalValue      value    = response.getMessage().unpack(OptionalValue.class);
        assertThat(value, is(notNullValue()));
        assertThat(value.getPresent(), is(true));
        assertThat(fromByteString(value.getValue(), serializer, String.class), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldGetNonExistentKey(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<Binary, Binary> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        Binary     binary  = ExternalizableHelper.toBinary("key-1", serializer);
        ByteString key     = BinaryHelper.toByteString(binary);
        BytesValue request = BytesValue.newBuilder().setValue(key).build();

        NamedCacheResponse response = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Get, request);
        OptionalValue      value    = response.getMessage().unpack(OptionalValue.class);
        assertThat(value, is(notNullValue()));
        assertThat(value.getPresent(), is(false));
        assertThat(fromByteString(value.toByteString(), serializer, String.class), is(nullValue()));
        }

    // ----- GetAll ---------------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldGetAllForEmptyKeyCollection(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 5);

        assertGetAll(cache, serializer, sScope, Collections.emptyList());
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldGetAllWhenNoKeysMatch(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 4);

        Collection<String> colKeys = Arrays.asList("key-5", "key-6");

        assertGetAll(cache, serializer, sScope, colKeys);
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldGetAllWhenAllKeysMatch(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 4);

        Collection<String> colKeys = Arrays.asList("key-2", "key-4");

        assertGetAll(cache, serializer, sScope, colKeys);
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldGetAllWhenAllSomeKeysMatch(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 4);

        Collection<String> colKeys = Arrays.asList("key-0", "key-2", "key-4", "key-5");

        assertGetAll(cache, serializer, sScope, colKeys);
        }

    void assertGetAll(NamedCache<String, String> cache, Serializer serializer, String sScope, Collection<String> keys)
            throws Exception
        {
        Map<String, String> mapExpected = cache.getAll(keys);

        Collection<ByteString> serializedKeys = keys.stream()
                .map(s -> toByteString(s, serializer))
                .collect(Collectors.toList());

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, cache.getCacheName());

        CollectionOfBytesValues request = CollectionOfBytesValues.newBuilder()
                .addAllValues(serializedKeys)
                .build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.GetAll, request);

        Map<String, String> mapResult = new HashMap<>();
        for (NamedCacheResponse response : list)
            {
            BinaryKeyAndValue keyAndValue = response.getMessage().unpack(BinaryKeyAndValue.class);
            String            sKey   = fromByteString(keyAndValue.getKey(), serializer, String.class);
            String            sValue = fromByteString(keyAndValue.getValue(), serializer, String.class);
            mapResult.put(sKey, sValue);
            }
        assertThat(mapResult, is(mapExpected));
        }

    // ----- Invoke ---------------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallInvoke(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "people";
        NamedCache<String, Person> cache      = ensureEmptyCache(sScope, sCacheName);
        String                     sKey       = "bb";
        Person                     person     = new Person("bob", "builder", 25, "male");
        cache.put(sKey, person);

        ValueExtractor<Person, String>                      extractor = new UniversalExtractor<>("lastName");
        InvocableMap.EntryProcessor<String, Person, String> processor = new ExtractorProcessor<>(extractor);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, cache.getCacheName());

        ByteString keyBytes = BinaryHelper.toByteString(sKey, serializer);
        ExecuteRequest request = ExecuteRequest.newBuilder()
                .setAgent(BinaryHelper.toByteString(processor, serializer))
                .setKeys(KeysOrFilter.newBuilder()
                        .setKey(keyBytes)
                        .build())
                .build();

        NamedCacheResponse response = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Invoke, request);

        BinaryKeyAndValue keyAndValue = response.getMessage().unpack(BinaryKeyAndValue.class);
        assertThat(keyAndValue.getKey(), is(keyBytes));
        assertThat(keyAndValue.getValue(), is(notNullValue()));
        assertThat(BinaryHelper.fromByteString(keyAndValue.getValue(), serializer), is(person.getLastName()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallInvokeWithMissingEntryProcessor(String ignored, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "people";
        NamedCache<String, Person> cache      = ensureEmptyCache(sScope, sCacheName);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, cache.getCacheName());

        ExecuteRequest request = ExecuteRequest.newBuilder()
                .setKeys(KeysOrFilter.newBuilder().setKey(BinaryHelper.toByteString("foo", serializer)).build())
                .build();

        assertThrows(RequestIncompleteException.class,
                () -> sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Invoke, request));
        }

    // ----- InvokeAll ------------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallInvokeAllWithFilter(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "people";
        NamedCache<String, Person> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 35, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        ValueExtractor<Person, String>                      extractor = new UniversalExtractor<>("firstName");
        InvocableMap.EntryProcessor<String, Person, String> processor = new ExtractorProcessor<>(extractor);
        Filter<Person>                                      filter    = Filters.equal("age", 25);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ExecuteRequest request = ExecuteRequest.newBuilder()
                .setAgent(BinaryHelper.toByteString(processor, serializer))
                .setKeys(KeysOrFilter.newBuilder()
                        .setFilter(BinaryHelper.toByteString(filter, serializer))
                        .build())
                .build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Invoke, request);
        assertThat(list.size(), is(2));

        Map<String, String> map = list.stream()
                .map(r -> unpack(r.getMessage(), BinaryKeyAndValue.class))
                .collect(Collectors.toMap(keyAndValue -> BinaryHelper.fromByteString(keyAndValue.getKey(), serializer),
                                          keyAndValue -> BinaryHelper.fromByteString(keyAndValue.getValue(), serializer)));

        assertThat(map, hasEntry(person1.getLastName(), person1.getFirstName()));
        assertThat(map, hasEntry(person2.getLastName(), person2.getFirstName()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallInvokeAllWithNoFilterOrKeys(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "people";
        NamedCache<String, Person> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 25, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        ValueExtractor<Person, String>                      extractor = new UniversalExtractor<>("firstName");
        InvocableMap.EntryProcessor<String, Person, String> processor = new ExtractorProcessor<>(extractor);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ExecuteRequest request = ExecuteRequest.newBuilder()
                .setAgent(BinaryHelper.toByteString(processor, serializer))
                .build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Invoke, request);
        assertThat(list.size(), is(3));

        Map<String, String> map = list.stream()
                .map(r -> unpack(r.getMessage(), BinaryKeyAndValue.class))
                .collect(Collectors.toMap(keyAndValue -> BinaryHelper.fromByteString(keyAndValue.getKey(), serializer),
                                          keyAndValue -> BinaryHelper.fromByteString(keyAndValue.getValue(), serializer)));

        assertThat(map, hasEntry(person1.getLastName(), person1.getFirstName()));
        assertThat(map, hasEntry(person2.getLastName(), person2.getFirstName()));
        assertThat(map, hasEntry(person3.getLastName(), person3.getFirstName()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallInvokeAllWithKeys(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "people";
        NamedCache<String, Person> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 25, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        ValueExtractor<Person, String>                      extractor = new UniversalExtractor<>("firstName");
        InvocableMap.EntryProcessor<String, Person, String> processor = new ExtractorProcessor<>(extractor);

        List<ByteString> listKeys = Arrays.asList(BinaryHelper.toByteString(person1.getLastName(), serializer),
                                                  BinaryHelper.toByteString(person2.getLastName(), serializer));

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ExecuteRequest request = ExecuteRequest.newBuilder()
                .setAgent(BinaryHelper.toByteString(processor, serializer))
                .setKeys(KeysOrFilter.newBuilder()
                        .setKeys(CollectionOfBytesValues.newBuilder().addAllValues(listKeys).build())
                        .build())
                .build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Invoke, request);

        Map<String, String> map = list.stream()
                .map(r -> unpack(r.getMessage(), BinaryKeyAndValue.class))
                .collect(Collectors.toMap(keyAndValue -> BinaryHelper.fromByteString(keyAndValue.getKey(), serializer),
                                          keyAndValue -> BinaryHelper.fromByteString(keyAndValue.getValue(), serializer)));

        assertThat(map, hasEntry(person1.getLastName(), person1.getFirstName()));
        assertThat(map, hasEntry(person2.getLastName(), person2.getFirstName()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallInvokeAllWithMissingProcessor(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String sCacheName = "people";

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ExecuteRequest request = ExecuteRequest.newBuilder()
                .setKeys(KeysOrFilter.newBuilder()
                        .setFilter(BinaryHelper.toByteString(AlwaysFilter.INSTANCE, serializer))
                        .build())
                .build();

        assertThrows(RequestIncompleteException.class,
                () -> sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Invoke, request));
        }

    // ----- IsEmpty --------------------------------------------------------

    @ParameterizedTest(name = "{index} scope={0}")
    @MethodSource("serializers")
    public void shouldBeEmpty(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        NamedCacheResponse response  = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.IsEmpty, Empty.getDefaultInstance());
        BoolValue          boolValue = response.getMessage().unpack(BoolValue.class);
        assertThat(boolValue.getValue(), is(true));
        }

    @ParameterizedTest(name = "{index} scope={0}")
    @MethodSource("serializers")
    public void shouldNotBeEmpty(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 10);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        NamedCacheResponse response  = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.IsEmpty, Empty.getDefaultInstance());
        BoolValue          boolValue = response.getMessage().unpack(BoolValue.class);
        assertThat(boolValue.getValue(), is(false));
        }

    // ----- keySet(Filter) -------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallKeySetWithFilterWhenSomeEntriesMatch(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "people";
        NamedCache<String, Person> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 50, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        Filter<Person> filter       = new EqualsFilter<>("getAge", 25);
        List<String>   listExpected = new ArrayList<>(cache.keySet(filter));
        ByteString     filterBytes  = BinaryHelper.toByteString(filter, serializer);
        QueryRequest   request      = QueryRequest.newBuilder()
                                                .setFilter(filterBytes)
                                                .build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.QueryKeys, request);
        assertThat(list.size(), is(listExpected.size()));

        List<String> oResult = list.stream()
                .map(r -> unpack(r.getMessage(), BytesValue.class))
                .map(v -> (String) BinaryHelper.fromBytesValue(v, serializer))
                .collect(Collectors.toList());

        assertThat(oResult, containsInAnyOrder(listExpected.toArray()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallKeySetWithFilterWhenAllEntriesMatch(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "people";
        NamedCache<String, Person> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 50, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        Filter<Person> filter       = Filters.always();
        List<String>   listExpected = new ArrayList<>(cache.keySet(filter));
        ByteString     filterBytes  = BinaryHelper.toByteString(filter, serializer);
        QueryRequest   request      = QueryRequest.newBuilder()
                                                .setFilter(filterBytes)
                                                .build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.QueryKeys, request);
        assertThat(list.size(), is(listExpected.size()));

        List<String> oResult = list.stream()
                .map(r -> unpack(r.getMessage(), BytesValue.class))
                .map(v -> (String) BinaryHelper.fromBytesValue(v, serializer))
                .collect(Collectors.toList());

        assertThat(oResult, containsInAnyOrder(listExpected.toArray()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallKeySetWithFilterWhenNoEntriesMatch(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "people";
        NamedCache<String, Person> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 50, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        Filter<Person> filter      = new EqualsFilter<>("getAge", 100);
        ByteString     filterBytes = BinaryHelper.toByteString(filter, serializer);
        QueryRequest   request     = QueryRequest.newBuilder()
                                                .setFilter(filterBytes)
                                                .build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.QueryKeys, request);
        assertThat(list.isEmpty(), is(true));
        }

    // ----- Put ------------------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldInsertNewEntry(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ByteString key   = toByteString("key-1", serializer);
        ByteString value = toByteString("value-1", serializer);

        PutRequest request = PutRequest.newBuilder()
                .setKey(key)
                .setValue(value)
                .build();

        NamedCacheResponse response = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Put, request);
        BytesValue         oResult  = response.getMessage().unpack(BytesValue.class);
        assertThat(fromBytesValue(oResult, serializer, String.class), is(nullValue()));
        assertThat(cache.get("key-1"), is("value-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldUpdateEntry(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String sCacheName = "test-cache";
        NamedCache<String, String> cache = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.put("key-1", "value-1");

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ByteString key   = toByteString("key-1", serializer);
        ByteString value = toByteString("value-2", serializer);

        PutRequest request = PutRequest.newBuilder()
                .setKey(key)
                .setValue(value)
                .build();

        NamedCacheResponse response = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Put, request);
        BytesValue         oResult  = response.getMessage().unpack(BytesValue.class);
        assertThat(oResult, is(notNullValue()));
        assertThat(fromBytesValue(oResult, serializer, String.class), is("value-1"));
        assertThat(cache.get("key-1"), is("value-2"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldUpdateEntryPreviouslyMappedToNull(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.put("key-1", null);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ByteString                  key      = toByteString("key-1", serializer);
        ByteString                  value    = toByteString("value-2", serializer);

        PutRequest request = PutRequest.newBuilder()
                .setKey(key)
                .setValue(value)
                .build();

        NamedCacheResponse response = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Put, request);
        BytesValue         oResult  = response.getMessage().unpack(BytesValue.class);
        assertThat(oResult, is(notNullValue()));
        assertThat(fromBytesValue(oResult, serializer, String.class), is(nullValue()));

        assertThat(cache.get("key-1"), is("value-2"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldUpdateEntryWithNullValue(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        ByteString                 key        = toByteString("key-1", serializer);
        ByteString                 value      = toByteString(null, serializer);

        cache.clear();
        cache.put("key-1", "value-1");

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        PutRequest request = PutRequest.newBuilder()
                .setKey(key)
                .setValue(value)
                .build();

        NamedCacheResponse response = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Put, request);
        BytesValue         oResult  = response.getMessage().unpack(BytesValue.class);
        assertThat(oResult, is(notNullValue()));
        assertThat(fromBytesValue(oResult, serializer, String.class), is("value-1"));
        assertThat(cache.get("key-1"), is(nullValue()));
        }

    // ----- PutAll ---------------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldPutAll(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        ByteString  key1    = toByteString("key-1", serializer);
        ByteString  value1  = toByteString("value-1", serializer);
        ByteString  key2    = toByteString("key-2", serializer);
        ByteString  value2  = toByteString("value-2", serializer);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        PutAllRequest request = PutAllRequest.newBuilder()
                .addEntries(BinaryKeyAndValue.newBuilder().setKey(key1).setValue(value1).build())
                .addEntries(BinaryKeyAndValue.newBuilder().setKey(key2).setValue(value2).build())
                .build();

        sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.PutAll, request);

        assertThat(cache.get("key-1"), is("value-1"));
        assertThat(cache.get("key-2"), is("value-2"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldPutAllWithExpiry(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        ByteString key1    = toByteString("key-1", serializer);
        ByteString value1  = toByteString("value-1", serializer);
        ByteString key2    = toByteString("key-2", serializer);
        ByteString value2  = toByteString("value-2", serializer);
        long       cMillis = 50000L;

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        PutAllRequest request = PutAllRequest.newBuilder()
                .setTtl(cMillis)
                .addEntries(BinaryKeyAndValue.newBuilder().setKey(key1).setValue(value1).build())
                .addEntries(BinaryKeyAndValue.newBuilder().setKey(key2).setValue(value2).build())
                .build();

        long nExpire = System.currentTimeMillis() + cMillis -1;
        sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.PutAll, request);

        assertThat(cache.size(), is(2));
        assertThat((long) cache.invoke("key-1", GetEntryExpiry.INSTANCE), is(greaterThan(nExpire)));
        assertThat((long) cache.invoke("key-2", GetEntryExpiry.INSTANCE), is(greaterThan(nExpire)));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldPutAllWithZeroEntries(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        PutAllRequest request = PutAllRequest.newBuilder().build();
        sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.PutAll, request);

        assertThat(cache.isEmpty(), is(true));
        }

    // ----- PutIfAbsent ----------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldPutIfAbsentForNonExistentKey(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ByteString key     = toByteString("key-1", serializer);
        ByteString value   = toByteString("value-1", serializer);
        PutRequest request = PutRequest.newBuilder().setKey(key).setValue(value).build();

        NamedCacheResponse response = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.PutIfAbsent, request);
        BytesValue         oResult  = response.getMessage().unpack(BytesValue.class);
        assertThat(oResult, is(notNullValue()));
        assertThat(fromBytesValue(oResult, serializer, String.class), is(nullValue()));
        assertThat(cache.get("key-1"), is("value-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldPutIfAbsentForExistingKey(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.put("key-1", "value-1");

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ByteString key     = toByteString("key-1", serializer);
        ByteString value   = toByteString("value-2", serializer);
        PutRequest request = PutRequest.newBuilder().setKey(key).setValue(value).build();

        NamedCacheResponse response = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.PutIfAbsent, request);
        BytesValue         oResult  = response.getMessage().unpack(BytesValue.class);
        assertThat(oResult, is(notNullValue()));
        assertThat(fromBytesValue(oResult, serializer, String.class), is("value-1"));
        assertThat(cache.get("key-1"), is("value-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldPutIfAbsentForExistingKeyMappedToNull(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.put("key-1", null);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ByteString key     = toByteString("key-1", serializer);
        ByteString value   = toByteString("value-2", serializer);
        PutRequest request = PutRequest.newBuilder().setKey(key).setValue(value).build();

        NamedCacheResponse response = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.PutIfAbsent, request);
        BytesValue         oResult  = response.getMessage().unpack(BytesValue.class);
        assertThat(oResult, is(notNullValue()));
        assertThat(fromBytesValue(oResult, serializer, String.class), is(nullValue()));
        assertThat(cache.get("key-1"), is("value-2"));
        }

    // ----- Remove ---------------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldRemoveOnNonExistentEntry(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        int count = 10;
        clearAndPopulate(cache, count);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        BytesValue         key      = BytesValue.newBuilder().setValue(toByteString("key-100", serializer)).build();
        NamedCacheResponse response = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Remove, key);
        BytesValue         oResult  = response.getMessage().unpack(BytesValue.class);

        assertThat(oResult, is(notNullValue()));
        assertThat(fromBytesValue(oResult, serializer, String.class), is(nullValue()));
        assertThat(cache.size(), is(count));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReturnPreviousValueForRemoveOnExistingMapping(String ignored, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        int cCount = 10;
        clearAndPopulate(cache, cCount);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        BytesValue         key      = BytesValue.newBuilder().setValue(toByteString("key-1", serializer)).build();
        NamedCacheResponse response = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Remove, key);
        BytesValue         oResult  = response.getMessage().unpack(BytesValue.class);

        assertThat(oResult, is(notNullValue()));
        assertThat(fromBytesValue(oResult, serializer, String.class), is("value-1"));
        assertThat(cache.get("key-1"), is(nullValue()));
        assertThat(cache.size(), is(cCount - 1));
        }

    // ----- Remove Mapping -------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReturnFalseForRemoveMappingOnNonExistentMapping(String ignored, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<Binary, Binary> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ByteString         key         = toByteString("key-1", serializer);
        ByteString         value       = toByteString("value-123", serializer);
        BinaryKeyAndValue  keyAndValue = BinaryKeyAndValue.newBuilder().setKey(key).setValue(value).build();
        NamedCacheResponse response    = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.RemoveMapping, keyAndValue);
        BoolValue          oResult     = response.getMessage().unpack(BoolValue.class);

        assertThat(oResult, is(notNullValue()));
        assertThat(oResult.getValue(), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReturnFalseForRemoveMappingOnNonMatchingMapping(String ignored, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.put("key-1", "value-1");

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ByteString         key         = toByteString("key-1", serializer);
        ByteString         value       = toByteString("value-123", serializer);
        BinaryKeyAndValue  keyAndValue = BinaryKeyAndValue.newBuilder().setKey(key).setValue(value).build();
        NamedCacheResponse response    = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.RemoveMapping, keyAndValue);
        BoolValue          oResult     = response.getMessage().unpack(BoolValue.class);

        assertThat(oResult, is(notNullValue()));
        assertThat(oResult.getValue(), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReturnTrueForRemoveMappingOnMatchingMapping(String ignored, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.put("key-1", "value-123");

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ByteString         key         = toByteString("key-1", serializer);
        ByteString         value       = toByteString("value-123", serializer);
        BinaryKeyAndValue  keyAndValue = BinaryKeyAndValue.newBuilder().setKey(key).setValue(value).build();
        NamedCacheResponse response    = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.RemoveMapping, keyAndValue);
        BoolValue          oResult     = response.getMessage().unpack(BoolValue.class);

        assertThat(oResult, is(notNullValue()));
        assertThat(oResult.getValue(), is(true));
        }

    // ----- RemoveIndex ----------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    public void shouldRemoveIndexWhenIndexExists(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                        sCacheName = "add-index-cache";
        NamedCache                    cache      = ensureEmptyCache(sScope, sCacheName);
        Map<ValueExtractor, MapIndex> indexMap   = removeIndexes(cache);

        ValueExtractor extractor    = new UniversalExtractor("foo");
        ByteString     binExtractor = toByteString(extractor, serializer);

        // Add the index using the normal cache
        cache.addIndex(extractor, false, null);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        IndexRequest request = IndexRequest.newBuilder()
                .setAdd(false)
                .setExtractor(binExtractor)
                .build();

        sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Index, request);

        assertThat(indexMap.isEmpty(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldRemoveIndexWhenIndexDoesNotExist(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                        sCacheName = "add-index-cache";
        NamedCache                    cache      = ensureEmptyCache(sScope, sCacheName);
        Map<ValueExtractor, MapIndex> indexMap   = removeIndexes(cache);

        ValueExtractor extractor    = new UniversalExtractor("foo");
        ByteString     binExtractor = toByteString(extractor, serializer);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        IndexRequest request = IndexRequest.newBuilder()
                .setAdd(false)
                .setExtractor(binExtractor)
                .build();
        
        sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Index, request);

        assertThat(indexMap.isEmpty(), is(true));
        }

    // ----- Replace --------------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReturnNullValueForReplaceOnNonExistentMapping(String ignored, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ByteString         key         = toByteString("key-1", serializer);
        ByteString         value       = toByteString("value-123", serializer);
        BinaryKeyAndValue  keyAndValue = BinaryKeyAndValue.newBuilder().setKey(key).setValue(value).build();
        NamedCacheResponse response    = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Replace, keyAndValue);
        BytesValue         binResult   = response.getMessage().unpack(BytesValue.class);

        assertThat(binResult, is(notNullValue()));
        assertThat(fromBytesValue(binResult, serializer, String.class), is(nullValue()));
        assertThat(cache.get("key-1"), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReturnNonNullForReplaceOnExistingMapping(String ignored, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 5);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ByteString         key         = toByteString("key-1", serializer);
        ByteString         value       = toByteString("value-123", serializer);
        BinaryKeyAndValue  keyAndValue = BinaryKeyAndValue.newBuilder().setKey(key).setValue(value).build();
        NamedCacheResponse response    = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Replace, keyAndValue);
        BytesValue         binResult   = response.getMessage().unpack(BytesValue.class);

        assertThat(binResult, is(notNullValue()));
        assertThat(fromBytesValue(binResult, serializer, String.class), is("value-1"));
        assertThat(cache.get("key-1"), is("value-123"));
        }

    // ----- Replace Value --------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReturnFalseForReplaceMappingOnNonExistentMapping(String ignored, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ByteString            key       = toByteString("key-1", serializer);
        ByteString            prevValue = toByteString("value-1", serializer);
        ByteString            newValue  = toByteString("value-123", serializer);
        ReplaceMappingRequest request   = ReplaceMappingRequest.newBuilder()
                .setKey(key)
                .setPreviousValue(prevValue)
                .setNewValue(newValue)
                .build();

        NamedCacheResponse response  = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.ReplaceMapping, request);
        BoolValue          boolValue = response.getMessage().unpack(BoolValue.class);

        assertThat(boolValue, is(notNullValue()));
        assertThat(boolValue.getValue(), is(false));
        assertThat(cache.get("key-1"), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReturnFalseForReplaceMappingOnNonMatchingMapping(String ignored, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 5);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ByteString            key       = toByteString("key-1", serializer);
        ByteString            prevValue = toByteString("value-123", serializer);
        ByteString            newValue  = toByteString("value-456", serializer);
        ReplaceMappingRequest request   = ReplaceMappingRequest.newBuilder()
                .setKey(key)
                .setPreviousValue(prevValue)
                .setNewValue(newValue)
                .build();


        NamedCacheResponse response  = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.ReplaceMapping, request);
        BoolValue          boolValue = response.getMessage().unpack(BoolValue.class);

        assertThat(boolValue, is(notNullValue()));
        assertThat(boolValue.getValue(), is(false));
        assertThat(cache.get("key-1"), is("value-1"));
        }


    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReturnTrueForReplaceMappingOnMatchingMapping(String ignored, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 5);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        ByteString            key       = toByteString("key-1", serializer);
        ByteString            prevValue = toByteString("value-1", serializer);
        ByteString            newValue  = toByteString("value-123", serializer);
        ReplaceMappingRequest request   = ReplaceMappingRequest.newBuilder()
                .setKey(key)
                .setPreviousValue(prevValue)
                .setNewValue(newValue)
                .build();

        NamedCacheResponse response  = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.ReplaceMapping, request);
        BoolValue          boolValue = response.getMessage().unpack(BoolValue.class);

        assertThat(boolValue, is(notNullValue()));
        assertThat(boolValue.getValue(), is(true));
        assertThat(cache.get("key-1"), is("value-123"));
        }

    // ----- Size -----------------------------------------------------------

    @ParameterizedTest(name = "{index} scope={0}")
    @MethodSource("serializers")
    public void shouldGetSizeOfEmptyCache(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        NamedCacheResponse response = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Size, Empty.getDefaultInstance());
        Int32Value         nSize    = response.getMessage().unpack(Int32Value.class);

        assertThat(nSize, is(notNullValue()));
        assertThat(nSize.getValue(), is(0));
        }

    @ParameterizedTest(name = "{index} scope={0}")
    @MethodSource("serializers")
    public void shouldGetSizeOfPopulatedCache(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 10);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        NamedCacheResponse response = sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Size, Empty.getDefaultInstance());
        Int32Value         nSize    = response.getMessage().unpack(Int32Value.class);

        assertThat(nSize, is(notNullValue()));
        assertThat(nSize.getValue(), is(cache.size()));
        }

    // ----- Truncate -------------------------------------------------------

    @ParameterizedTest(name = "{index} scope={0}")
    @MethodSource("serializers")
    public void shouldTruncate(String ignored, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 5);

        ListenableStreamObserver     observer = new ListenableStreamObserver();
        StreamObserver<ProxyRequest> channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        assertThat(cache.isEmpty(), is(false));
        sendCacheRequest(channel, observer, cacheId, NamedCacheRequestType.Truncate, Empty.getDefaultInstance());

        assertThat(cache.isEmpty(), is(true));
        }

    // ----- values(Filter) -------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallValuesWithFilterWhenSomeEntriesMatch(String ignored, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "people";
        NamedCache<String, Person> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 50, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        Filter<Person>     filter      = new EqualsFilter<>("getAge", 25);
        Collection<Person> colExpected = cache.values(filter);
        ByteString         filterBytes = BinaryHelper.toByteString(filter, serializer);

        QueryRequest request = QueryRequest.newBuilder()
                .setFilter(filterBytes)
                .build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.QueryValues, request);
        assertThat(list.size(), is(colExpected.size()));

        List<Person> listResult = list.stream()
                .map(r -> unpack(r.getMessage(), BytesValue.class))
                .map(bv -> (Person) BinaryHelper.fromBytesValue(bv, serializer))
                .toList();

        assertThat(listResult, containsInAnyOrder(colExpected.toArray()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallValuesWithFilterWhenAllEntriesMatch(String ignored, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "people";
        NamedCache<String, Person> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 50, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        Filter<Person>     filter      = Filters.always();
        Collection<Person> colExpected = cache.values(filter);

        ByteString                     filterBytes = BinaryHelper.toByteString(filter, serializer);

        QueryRequest request = QueryRequest.newBuilder()
                .setFilter(filterBytes)
                .build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.QueryValues, request);
        assertThat(list.size(), is(colExpected.size()));

        List<Person> listResult = list.stream()
                .map(r -> unpack(r.getMessage(), BytesValue.class))
                .map(bv -> (Person) BinaryHelper.fromBytesValue(bv, serializer))
                .toList();

        assertThat(listResult, containsInAnyOrder(colExpected.toArray()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallValuesWithFilterWhenNoEntriesMatch(String ignored, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "people";
        NamedCache<String, Person> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 50, "male");
        cache.put(person1.getLastName(), person1);
        cache.put(person2.getLastName(), person2);
        cache.put(person3.getLastName(), person3);

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        Filter<Person>                 filter      = new EqualsFilter<>("getAge", 100);
        Collection<Person>             colExpected = cache.values(filter);
        ByteString                     filterBytes = BinaryHelper.toByteString(filter, serializer);

        QueryRequest request = QueryRequest.newBuilder()
                .setFilter(filterBytes)
                .build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.QueryValues, request);
        assertThat(list.size(), is(colExpected.size()));

        List<Person> listResult = list.stream()
                .map(r -> unpack(r.getMessage(), BytesValue.class))
                .map(bv -> (Person) BinaryHelper.fromBytesValue(bv, serializer))
                .toList();

        assertThat(listResult, containsInAnyOrder(colExpected.toArray()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    public void shouldCallValuesWithFilterAndComparatorWhenSomeEntriesMatch(String     ignored,
                                                                            Serializer serializer,
                                                                            String     sScope)
            throws Exception
        {
        String                      sCacheName = "numbers";
        NamedCache<String, Integer> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        for (int i = 0; i < 100; i++)
            {
            cache.put("key-" + i, i);
            }

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        Filter<Integer>     filter          = Filters.greater(Extractors.identity(), 50);
        Comparator<Integer> comparator      = IdentityExtractor.INSTANCE();
        List<Integer>       expected        = new ArrayList<>(cache.values(filter, comparator));
        ByteString          filterBytes     = BinaryHelper.toByteString(filter, serializer);
        ByteString          comparatorBytes = BinaryHelper.toByteString(comparator, serializer);

        QueryRequest request = QueryRequest.newBuilder()
                .setFilter(filterBytes)
                .setComparator(comparatorBytes)
                .build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.QueryValues, request);
        assertThat(list.size(), is(expected.size()));
        
        List<Integer> listResult = list.stream()
                .map(r -> unpack(r.getMessage(), BytesValue.class))
                .map(bv -> (Integer) BinaryHelper.fromBytesValue(bv, serializer))
                .sorted()
                .toList();

        expected.sort(Comparator.naturalOrder());
        assertThat(listResult.size(), is(expected.size()));
        assertThat(listResult, is(expected));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    public void shouldCallValuesWithFilterAndComparatorWhenAllEntriesMatch(String     ignored,
                                                                           Serializer serializer,
                                                                           String     sScope)
            throws Exception
        {
        String                      sCacheName = "numbers";
        NamedCache<String, Integer> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        for (int i = 0; i < 100; i++)
            {
            cache.put("key-" + i, i);
            }

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        Filter<Integer>     filter          = Filters.always();
        Comparator<Integer> comparator      = IdentityExtractor.INSTANCE();
        Collection<Integer> expected        = cache.values(filter, comparator);
        ByteString          filterBytes     = BinaryHelper.toByteString(filter, serializer);
        ByteString          comparatorBytes = BinaryHelper.toByteString(comparator, serializer);

        QueryRequest request = QueryRequest.newBuilder()
                .setFilter(filterBytes)
                .setComparator(comparatorBytes)
                .build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.QueryValues, request);
        assertThat(list.size(), is(expected.size()));

        List<Integer> listResult = list.stream()
                .map(r -> unpack(r.getMessage(), BytesValue.class))
                .map(bv -> (Integer) BinaryHelper.fromBytesValue(bv, serializer))
                .collect(Collectors.toList());

        assertThat(listResult, containsInAnyOrder(expected.toArray()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    public void shouldCallValuesWithFilterAndComparatorWhenNoEntriesMatch(String     ignored,
                                                                          Serializer serializer,
                                                                          String     sScope)
            throws Exception
        {
        String                      sCacheName = "numbers";
        NamedCache<String, Integer> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        for (int i = 0; i < 100; i++)
            {
            cache.put("key-" + i, i);
            }

        TestStreamObserver<ProxyResponse> observer = new TestStreamObserver<>();
        StreamObserver<ProxyRequest>      channel  = openChannel(observer);

        init(channel, observer, serializer, sScope);
        int cacheId = ensureCache(channel, observer, sCacheName);

        Filter<Integer>     filter          = Filters.greater(Extractors.identity(), 500);
        Comparator<Integer> comparator      = IdentityExtractor.INSTANCE();
        Collection<Integer> expected        = cache.values(filter, comparator);
        ByteString          filterBytes     = BinaryHelper.toByteString(filter, serializer);
        ByteString          comparatorBytes = BinaryHelper.toByteString(comparator, serializer);

        QueryRequest request = QueryRequest.newBuilder()
                .setFilter(filterBytes)
                .setComparator(comparatorBytes)
                .build();

        List<NamedCacheResponse> list = sendStreamCacheRequest(channel, observer, cacheId, NamedCacheRequestType.QueryValues, request);
        assertThat(list.size(), is(expected.size()));

        List<Integer> listResult = list.stream()
                .map(r -> unpack(r.getMessage(), BytesValue.class))
                .map(bv -> (Integer) BinaryHelper.fromBytesValue(bv, serializer))
                .toList();

        assertThat(listResult, containsInAnyOrder(expected.toArray()));
        }

    // ----- helper methods -------------------------------------------------

    protected <Resp extends Message> Resp sendCacheRequest(StreamObserver<ProxyRequest> channel,
            TestStreamObserver<ProxyResponse> observer, int cacheId, NamedCacheRequestType type,
            Message message) throws Exception
        {
        NamedCacheRequest request = NamedCacheRequest.newBuilder()
                .setCacheId(cacheId)
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
        return (Resp) proxyResponse.getMessage().unpack(NamedCacheResponse.class);
        }

    protected <Resp extends Message> List<Resp> sendStreamCacheRequest(StreamObserver<ProxyRequest> channel,
            TestStreamObserver<ProxyResponse> observer, int cacheId, NamedCacheRequestType type,
            Message message) throws Exception
        {
        NamedCacheRequest request = NamedCacheRequest.newBuilder()
                .setCacheId(cacheId)
                .setType(type)
                .setMessage(Any.pack(message))
                .build();

        int count      = observer.valueCount();
        int responseId = count + 1;

        List<Resp> list = new ArrayList<>();

        channel.onNext(newRequest(request));
        
        while (true)
            {
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
                break;
                }
            list.add((Resp) proxyResponse.getMessage().unpack(NamedCacheResponse.class));
            responseId++;
            }
        observer.onCompleted();
        return list;
        }

    protected int ensureCache(StreamObserver<ProxyRequest> channel, TestStreamObserver<ProxyResponse> observer,
            String sCacheName) throws Exception
        {
        EnsureCacheRequest ensureCacheRequest = EnsureCacheRequest.newBuilder()
                .setCache(sCacheName)
                .build();

        NamedCacheRequest request = NamedCacheRequest.newBuilder()
                .setType(NamedCacheRequestType.EnsureCache)
                .setMessage(Any.pack(ensureCacheRequest))
                .build();

        int count      = observer.valueCount();
        int responseId = count + 1;

        channel.onNext(newRequest(request));
        observer.awaitCount(responseId, 1, TimeUnit.MINUTES);
        observer.assertNoErrors();

        ProxyResponse      proxyResponse = observer.valueAt(responseId - 1);
        NamedCacheResponse response      = proxyResponse.getMessage().unpack(NamedCacheResponse.class);
        int                cacheId       = response.getCacheId();

        assertThat(cacheId, is(not(0)));
        return cacheId;
        }

    protected void init(StreamObserver<ProxyRequest> channel, TestStreamObserver<ProxyResponse> observer,
            Serializer serializer, String sScope) throws Exception
        {
        InitRequest initRequest = InitRequest.newBuilder()
                .setProtocol(NamedCacheProtocol.PROTOCOL_NAME)
                .setProtocolVersion(NamedCacheProtocol.VERSION)
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
    }
