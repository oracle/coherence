/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy.version_0;

import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.v0.Requests;

import com.oracle.coherence.grpc.messages.cache.v0.AggregateRequest;
import com.oracle.coherence.grpc.messages.cache.v0.Entry;
import com.oracle.coherence.grpc.messages.cache.v0.InvokeAllRequest;
import com.oracle.coherence.grpc.messages.cache.v0.InvokeRequest;
import com.oracle.coherence.grpc.messages.cache.v0.MapEventResponse;
import com.oracle.coherence.grpc.messages.cache.v0.MapListenerRequest;
import com.oracle.coherence.grpc.messages.cache.v0.MapListenerResponse;
import com.oracle.coherence.grpc.messages.cache.v0.OptionalValue;

import com.oracle.coherence.grpc.proxy.common.v0.NamedCacheService;

import com.tangosol.io.Serializer;

import com.tangosol.net.NamedCache;

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

import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.InKeySetFilter;
import com.tangosol.util.filter.MapEventFilter;

import com.tangosol.util.processor.ExtractorProcessor;

import grpc.proxy.Person;
import grpc.proxy.PersonMapTrigger;
import grpc.proxy.TestStreamObserver;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.TimeUnit;

import java.util.stream.Collectors;

import org.junit.jupiter.params.ParameterizedTest;

import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasKey;

/**
 * Integration tests to verify {@link NamedCacheService}.
 *
 * @author Jonathan Knight  2019.11.01
 * @since 20.06
 */
@SuppressWarnings({"rawtypes", "resource", "unchecked"})
public class NamedCacheServiceImplIT
        extends BaseNamedCacheServiceImplIT
    {
    // ----- AddIndex -------------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldAddIndex(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String     sCacheName = "add-index-cache";
        NamedCache cache      = ensureEmptyCache(sScope, sCacheName);

        Map<ValueExtractor, MapIndex> indexMap = removeIndexes(cache);

        ValueExtractor extractor    = new UniversalExtractor("foo");
        ByteString     binExtractor = toByteString(extractor, serializer);

        NamedCacheService         service  = createService();
        TestStreamObserver<Empty> observer = new TestStreamObserver<>();

        service.addIndex(Requests.addIndex(sScope, sCacheName, serializerName, binExtractor), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        assertThat(indexMap, hasKey(extractor));
        assertThat(indexMap.get(extractor).isOrdered(), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldAddSortedIndex(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String     sCacheName = "add-index-cache";
        NamedCache cache      = ensureEmptyCache(sScope, sCacheName);

        Map<ValueExtractor, MapIndex> indexMap = removeIndexes(cache);

        ValueExtractor extractor    = new UniversalExtractor("foo");
        ByteString     binExtractor = toByteString(extractor, serializer);

        NamedCacheService         service  = createService();
        TestStreamObserver<Empty> observer = new TestStreamObserver<>();
        service.addIndex(Requests.addIndex(sScope, sCacheName, serializerName, binExtractor, true), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        assertThat(indexMap, hasKey(extractor));
        assertThat(indexMap.get(extractor).isOrdered(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldAddSortedIndexWithComparator(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String     sCacheName = "add-index-cache";
        NamedCache cache      = ensureEmptyCache(sScope, sCacheName);

        Map<ValueExtractor, MapIndex> indexMap = removeIndexes(cache);

        ValueExtractor extractor     = new UniversalExtractor("foo");
        ByteString     binExtractor  = toByteString(extractor, serializer);
        Comparator     comparator    = new SafeComparator(new UniversalExtractor("bar"));
        ByteString     binComparator = toByteString(comparator, serializer);

        NamedCacheService         service  = createService();
        TestStreamObserver<Empty> observer = new TestStreamObserver<>();

        service.addIndex(Requests.addIndex(sScope, sCacheName, serializerName, 
                binExtractor, true, binComparator), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        assertThat(indexMap, hasKey(extractor));
        assertThat(indexMap.get(extractor).isOrdered(), is(true));
        assertThat(indexMap.get(extractor).getComparator(), is(comparator));
        }

    // ----- Aggregate ------------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallAggregateWithFilterExpectingSingleResult(String serializerName, Serializer serializer, String sScope)
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

        AggregateRequest request = Requests.aggregate(sScope, sCacheName,
                                                      serializerName,
                                                      BinaryHelper.toByteString(filter, serializer),
                                                      BinaryHelper.toByteString(aggregator, serializer));

        NamedCacheService               service  = createService();
        TestStreamObserver<BytesValue>  observer = new TestStreamObserver<>();
        
        service.aggregate(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BytesValue bytesValue = observer.valueAt(0);
        Object     oResult    = BinaryHelper.fromBytesValue(bytesValue, serializer);
        assertThat(oResult, is(instanceOf(Integer.class)));
        assertThat(oResult, is(expected));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallAggregateWithFilterMatchingNoEntriesExpectingSingleResult(String     serializerName, 
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

        AggregateRequest request = Requests.aggregate(sScope, sCacheName,
                                                      serializerName,
                                                      BinaryHelper.toByteString(filter, serializer),
                                                      BinaryHelper.toByteString(aggregator, serializer));

        NamedCacheService               service  = createService();
        TestStreamObserver<BytesValue>  observer = new TestStreamObserver<>();
        
        service.aggregate(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BytesValue bytesValue = observer.valueAt(0);
        Object     oResult    = BinaryHelper.fromBytesValue(bytesValue, serializer);
        assertThat(oResult, is(instanceOf(Integer.class)));
        assertThat(oResult, is(cExpected));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallAggregateWithKeysExpectingSingleResult(String serializerName, Serializer serializer, String sScope) throws Exception
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

        List<String>     listKeys           = Arrays.asList(person1.getLastName(), person2.getLastName());
        List<ByteString> listSerializedKeys = listKeys.stream()
                .map(k -> BinaryHelper.toByteString(k, serializer))
                .collect(Collectors.toList());
        int              nExpected          = cache.aggregate(listKeys, aggregator);

        AggregateRequest request = Requests.aggregate(sScope, sCacheName,
                                                      serializerName,
                                                      listSerializedKeys,
                                                      BinaryHelper.toByteString(aggregator, serializer));

        NamedCacheService               service  = createService();
        TestStreamObserver<BytesValue>  observer = new TestStreamObserver<>();
        
        service.aggregate(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BytesValue bytesValue = observer.valueAt(0);
        Object     oResult    = BinaryHelper.fromBytesValue(bytesValue, serializer);
        assertThat(oResult, is(instanceOf(Integer.class)));
        assertThat(oResult, is(nExpected));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallAggregateWithKeysMatchingNoEntriesExpectingSingleResult(String serializerName, Serializer serializer, String sScope)
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
        int               nExpected         = cache.aggregate(listKeys, aggregator);

        AggregateRequest request = Requests.aggregate(sScope, sCacheName,
                                                      serializerName,
                                                      listSerializedKeys,
                                                      BinaryHelper.toByteString(aggregator, serializer));

        NamedCacheService               service  = createService();
        TestStreamObserver<BytesValue>  observer = new TestStreamObserver<>();

        service.aggregate(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BytesValue bytesValue = observer.valueAt(0);
        Object     oResult    = BinaryHelper.fromBytesValue(bytesValue, serializer);
        assertThat(oResult, is(instanceOf(Integer.class)));
        assertThat(oResult, is(nExpected));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallAggregateWithNoKeysOrFilterExpectingSingleResult(String serializerName, Serializer serializer, String sScope)
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

        AggregateRequest request = AggregateRequest.newBuilder()
                .setScope(sScope)
                .setCache(sCacheName)
                .setFormat(serializerName)
                .setAggregator(BinaryHelper.toByteString(aggregator, serializer))
                .build();

        NamedCacheService               service  = createService();
        TestStreamObserver<BytesValue>  observer = new TestStreamObserver<>();
        
        service.aggregate(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BytesValue bytesValue = observer.valueAt(0);
        Object     oResult    = BinaryHelper.fromBytesValue(bytesValue, serializer);
        assertThat(oResult, is(instanceOf(Integer.class)));
        assertThat(oResult, is(nExpected));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallAggregateWithFilterExpectingMapResult(String serializerName, Serializer serializer, String sScope) throws Exception
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

        AggregateRequest request = Requests.aggregate(sScope, sCacheName,
                                                      serializerName,
                                                      BinaryHelper.toByteString(filter, serializer),
                                                      BinaryHelper.toByteString(aggregator, serializer));

        NamedCacheService               service  = createService();
        TestStreamObserver<BytesValue>  observer = new TestStreamObserver<>();
        
        service.aggregate(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BytesValue bytesValue = observer.valueAt(0);
        Object     oResult    = BinaryHelper.fromBytesValue(bytesValue, serializer);
        assertThat(oResult, is(instanceOf(Map.class)));
        assertThat(oResult, is(mapExpected));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallAggregateWithKeysExpectingMapResult(String serializerName, Serializer serializer, String sScope) throws Exception
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

        AggregateRequest request = Requests.aggregate(sScope, sCacheName,
                                                      serializerName,
                                                      listSerializedKeys,
                                                      BinaryHelper.toByteString(aggregator, serializer));

        NamedCacheService               service  = createService();
        TestStreamObserver<BytesValue>  observer = new TestStreamObserver<>();
        
        service.aggregate(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BytesValue bytesValue = observer.valueAt(0);
        Object     oResult    = BinaryHelper.fromBytesValue(bytesValue, serializer);
        assertThat(oResult, is(instanceOf(Map.class)));
        assertThat(oResult, is(nExpected));
        }

    // ----- Clear ----------------------------------------------------------

    @ParameterizedTest(name = "{index} scope={0}")
    @MethodSource("getTestScopes")
    public void shouldClearEmptyCache(String sScope) throws Exception
        {
        String     sCacheName = "test-cache";
        NamedCache cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        NamedCacheService          service  = createService();
        TestStreamObserver<Empty>  observer = new TestStreamObserver<>();
        
        service.clear(Requests.clear(sScope, sCacheName), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        assertThat(cache.isEmpty(), is(true));
        }

    @ParameterizedTest(name = "{index} scope={0}")
    @MethodSource("getTestScopes")
    public void shouldClearPopulatedCache(String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 10);

        NamedCacheService         service  = createService();
        TestStreamObserver<Empty> observer = new TestStreamObserver<>();
        
        service.clear(Requests.clear(sScope, sCacheName), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        assertThat(cache.isEmpty(), is(true));
        }

    // ----- Contains Entry -------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldContainEntryWhenMappingPresent(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 3);

        NamedCacheService service  = createService();
        ByteString                 key      = toByteString("key-1", serializer);
        ByteString                 value    = toByteString("value-1", serializer);


        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        service.containsEntry(Requests.containsEntry(sScope, sCacheName, serializerName, key, value), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1)
                .assertValue(BoolValue.of(true));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldNotContainEntryWhenMappingHasDifferentValue(String serializerName, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 3);

        NamedCacheService service  = createService();
        ByteString                 key      = toByteString("key-1", serializer);
        ByteString                 value    = toByteString("not-value-1", serializer);

        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        service.containsEntry(Requests.containsEntry(sScope, sCacheName, serializerName, key, value), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1)
                .assertValue(BoolValue.of(false));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldNotContainEntryWhenMappingNotPresent(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 3);

        NamedCacheService service  = createService();
        ByteString                 key      = toByteString("key-100", serializer);
        ByteString                 value    = toByteString("value-100", serializer);
        
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        service.containsEntry(Requests.containsEntry(sScope, sCacheName, serializerName, key, value), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1)
                .assertValue(BoolValue.of(false));
        }

    // ----- Contains Key ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReturnTrueForContainsKeyWithExistingMapping(String serializerName, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 5);

        NamedCacheService service  = createService();
        ByteString                 key      = toByteString("key-2", serializer);
        
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        service.containsKey(Requests.containsKey(sScope, sCacheName, serializerName, key), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1)
                .assertValue(BoolValue.of(true));
        }


    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReturnFalseForContainsKeyWithNonExistentMapping(String serializerName, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 5);

        NamedCacheService service  = createService();
        ByteString                 key      = toByteString("bad-key", serializer);
        
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        service.containsKey(Requests.containsKey(sScope, sCacheName, serializerName, key), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1)
                .assertValue(BoolValue.of(false));
        }

    // ----- Contains Value -------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldContainValueWhenValuePresent(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 3);

        NamedCacheService service  = createService();
        ByteString                 value    = toByteString("value-2", serializer);
        
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        service.containsValue(Requests.containsValue(sScope, sCacheName, serializerName, value), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1)
                .assertValue(BoolValue.of(true));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldContainValueWhenValuePresentMultipleTimes(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.put("key-1", "value-1");
        cache.put("key-2", "value-2");
        cache.put("key-11", "value-1");
        cache.put("key-22", "value-2");

        NamedCacheService service  = createService();
        ByteString                 value    = toByteString("value-2", serializer);
        
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        service.containsValue(Requests.containsValue(sScope, sCacheName, serializerName, value), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1)
                .assertValue(BoolValue.of(true));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldNotContainValueWhenMappingNotPresent(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 3);

        NamedCacheService service  = createService();
        ByteString                 value    = toByteString("value-100", serializer);
        
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        service.containsValue(Requests.containsValue(sScope, sCacheName, serializerName, value), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1)
                .assertValue(BoolValue.of(false));
        }

    // ----- Destroy --------------------------------------------------------

    @ParameterizedTest(name = "{index} scope={0}")
    @MethodSource("getTestScopes")
    public void shouldDestroyCache(String sScope) throws Exception
        {
        String     sCacheName = "test-cache";
        NamedCache cache      = ensureEmptyCache(sScope, sCacheName);

        NamedCacheService service = createService();
        
        TestStreamObserver<Empty> observer = new TestStreamObserver<>();
        service.destroy(Requests.destroy(sScope, sCacheName), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);
        
        assertThat(cache.isDestroyed(), is(true));
        }

    // ----- entrySet(Filter) -----------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallEntrySetWithFilterWhenSomeEntriesMatch(String serializerName, Serializer serializer, String sScope) throws Exception
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

        NamedCacheService service = createService();
        Filter<Person>   filter  = new EqualsFilter<>("getAge", 25);

        Set<Map.Entry<String, Person>> expected = cache.entrySet(filter);

        ByteString                filterBytes = BinaryHelper.toByteString(filter, serializer);
        TestStreamObserver<Entry> observer    = new TestStreamObserver<>();
        service.entrySet(Requests.entrySet(sScope, sCacheName, serializerName, filterBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(expected.size());

        Map<String, Person> oResult = toMap(observer.values(), serializer);
        assertThat(oResult.entrySet(), is(expected));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallEntrySetWithFilterWhenAllEntriesMatch(String serializerName, Serializer serializer, String sScope) throws Exception
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

        NamedCacheService service = createService();
        Filter<Person>   filter  = Filters.always();

        Set<Map.Entry<String, Person>> expected = cache.entrySet(filter);

        ByteString                filterBytes = BinaryHelper.toByteString(filter, serializer);
        TestStreamObserver<Entry> observer    = new TestStreamObserver<>();
        service.entrySet(Requests.entrySet(sScope, sCacheName, serializerName, filterBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(expected.size());

        Map<String, Person> oResult = toMap(observer.values(), serializer);
        assertThat(oResult.entrySet(), is(expected));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallEntrySetWithFilterWhenNoEntriesMatch(String serializerName, Serializer serializer, String sScope) throws Exception
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

        NamedCacheService service = createService();
        Filter<Person>   filter  = new EqualsFilter<>("getAge", 100);

        Set<Map.Entry<String, Person>> expected = cache.entrySet(filter);

        ByteString                filterBytes = BinaryHelper.toByteString(filter, serializer);
        TestStreamObserver<Entry> observer    = new TestStreamObserver<>();
        service.entrySet(Requests.entrySet(sScope, sCacheName, serializerName, filterBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(expected.size());

        Map<String, Person> oResult = toMap(observer.values(), serializer);
        assertThat(oResult.entrySet(), is(expected));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    public void shouldCallEntrySetWithFilterAndComparatorWhenSomeEntriesMatch(String serializerName, Serializer serializer, String sScope)
            throws Exception
        {
        String                      sCacheName = "numbers";
        NamedCache<String, Integer> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        for (int i = 0; i < 100; i++)
            {
            cache.put("key-" + i, i);
            }

        NamedCacheService service    = createService();
        Filter<Integer>     filter     = Filters.greater(Extractors.identity(), 50);
        Comparator<Integer> comparator = IdentityExtractor.INSTANCE();

        Set<Map.Entry<String, Integer>>  expected     = cache.entrySet(filter, comparator);
        List<Map.Entry<String, Integer>> expectedList = new ArrayList<>(expected);
        expectedList.sort(Map.Entry.comparingByKey());

        ByteString                filterBytes     = BinaryHelper.toByteString(filter, serializer);
        ByteString                comparatorBytes = BinaryHelper.toByteString(comparator, serializer);
        TestStreamObserver<Entry> observer        = new TestStreamObserver<>();
        service.entrySet(Requests.entrySet(sScope, sCacheName, serializerName, filterBytes, comparatorBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(expected.size());

        List<Map.Entry<String, Integer>> listResult = toList(observer.values(), serializer);
        listResult.sort(Map.Entry.comparingByKey());
        assertThat(listResult.size(), is(expectedList.size()));
        assertThat(listResult, is(expectedList));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    public void shouldCallEntrySetWithFilterAndComparatorWhenAllEntriesMatch(String serializerName, Serializer serializer, String sScope)
            throws Exception
        {
        String                      sCacheName = "numbers";
        NamedCache<String, Integer> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        for (int i = 0; i < 100; i++)
            {
            cache.put("key-" + i, i);
            }

        NamedCacheService service    = createService();
        Filter<Integer>     filter     = Filters.always();
        Comparator<Integer> comparator = IdentityExtractor.INSTANCE();

        Set<Map.Entry<String, Integer>>  expected     = cache.entrySet(filter, comparator);
        List<Map.Entry<String, Integer>> expectedList = new ArrayList<>(expected);
        expectedList.sort(Map.Entry.comparingByKey());

        ByteString                filterBytes    = BinaryHelper.toByteString(filter, serializer);
        ByteString                comparatorBytes = BinaryHelper.toByteString(comparator, serializer);
        TestStreamObserver<Entry> observer = new TestStreamObserver<>();
        service.entrySet(Requests.entrySet(sScope, sCacheName, serializerName, filterBytes, comparatorBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(expected.size());

        List<Map.Entry<String, Integer>> listResult = toList(observer.values(), serializer);
        listResult.sort(Map.Entry.comparingByKey());
        assertThat(listResult.size(), is(expectedList.size()));
        assertThat(listResult, is(expectedList));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    public void shouldCallEntrySetWithFilterAndComparatorWhenNoEntriesMatch(String serializerName, Serializer serializer, String sScope)
            throws Exception
        {
        String                      sCacheName = "numbers";
        NamedCache<String, Integer> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        for (int i = 0; i < 100; i++)
            {
            cache.put("key-" + i, i);
            }

        NamedCacheService   service    = createService();
        Filter<Integer>     filter     = Filters.greater(Extractors.identity(), 500);
        Comparator<Integer> comparator = IdentityExtractor.INSTANCE();

        Set<Map.Entry<String, Integer>>  expected     = cache.entrySet(filter, comparator);
        List<Map.Entry<String, Integer>> expectedList = new ArrayList<>(expected);

        ByteString                filterBytes     = BinaryHelper.toByteString(filter, serializer);
        ByteString                comparatorBytes = BinaryHelper.toByteString(comparator, serializer);
        TestStreamObserver<Entry> observer        = new TestStreamObserver<>();
        service.entrySet(Requests.entrySet(sScope, sCacheName, serializerName, filterBytes, comparatorBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(expected.size());

        List<Map.Entry<String, Integer>> oResult = toList(observer.values(), serializer);
        assertThat(oResult, is(expectedList));
        }

    // ----- Events ---------------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldSubscribeToAllEvents(String serializerName, Serializer serializer, String sScope)
        {
        String                                sCacheName = "test-events-" + System.currentTimeMillis();
        CollectingMapListener<String, String> listener   = new CollectingMapListener<>();
        MapEventFilter<String, String>        filter     = new MapEventFilter<>(MapEventFilter.E_ALL,
                                                                                Filters.always());
        NamedCache<String, String>            cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.addMapListener(listener, filter, false);

        NamedCacheService service = createService();

        TestStreamObserver<MapListenerResponse> responseObserver = new TestStreamObserver<>();
        StreamObserver<MapListenerRequest>      requestObserver  = service.events(responseObserver);

        try
            {
            assertThat(requestObserver, is(notNullValue()));

            // subscribe to all events
            ByteString         filterBytes = BinaryHelper.toByteString(filter, serializer);
            long               nFilterId   = 19L;
            MapListenerRequest request     = Requests.addFilterMapListener(sScope, sCacheName, serializerName, filterBytes,
                                                                           nFilterId, false, false,
                                                                           ByteString.EMPTY);
            requestObserver.onNext(request);

            // wait for subscribed response
            responseObserver.awaitCount(1)
                    .assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(1);

            MapListenerResponse response = responseObserver.valueAt(0);
            assertThat(response, is(notNullValue()));
            assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
            assertThat(response.getSubscribed().getUid(), is(request.getUid()));

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
            listener.awaitCount(30);
            responseObserver.awaitCount(31)  // <--- observer has one more response due to the subscribed response
                    .assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(31);

            List<MapEvent<String, String>> expectedEvents = listener.values();
            List<MapEvent<String, String>> actualEvents   = toMapEventsForFilterId(cache, responseObserver.values(),
                                                                                  serializer, nFilterId);
            assertEqual(actualEvents, expectedEvents);
            }
        finally
            {
            requestObserver.onCompleted();
            cache.removeMapListener(listener, filter);
            }
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReceiveDeactivationEvent(String serializerName, Serializer ignored, String sScope) throws Exception
        {
        String                     sCacheName = "test-destroy";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        NamedCacheService service = createService();

        TestStreamObserver<MapListenerResponse> responseObserver = new TestStreamObserver<>();
        StreamObserver<MapListenerRequest>      requestObserver  = service.events(responseObserver);

        try
            {
            assertThat(requestObserver, is(notNullValue()));

            MapListenerRequest request = Requests.initListenerChannel(sScope, sCacheName, serializerName);
            requestObserver.onNext(request);

            // wait for subscribed response
            responseObserver.awaitCount(1)
                    .assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(1);

            MapListenerResponse response = responseObserver.valueAt(0);
            assertThat(response, is(notNullValue()));
            assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
            assertThat(response.getSubscribed().getUid(), is(request.getUid()));

            destroyCache(sScope, cache);

            // wait for the events
            assertThat(responseObserver.await(1, TimeUnit.MINUTES), is(true));
            responseObserver.assertComplete()
                    .assertNoErrors()
                    .assertValueCount(2);

            response = responseObserver.valueAt(1);
            assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.DESTROYED));
            assertThat(response.getDestroyed().getCache(), is(cache.getCacheName()));
            }
        finally
            {
            requestObserver.onCompleted();
            }
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReceiveTruncateEvent(String serializerName, Serializer ignored, String sScope)
        {
        String                     sCacheName = "test-events-" + System.currentTimeMillis();
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.put("foo", "bar");

        NamedCacheService service = createService();

        TestStreamObserver<MapListenerResponse> responseObserver = new TestStreamObserver<>();
        StreamObserver<MapListenerRequest>      requestObserver  = service.events(responseObserver);

        try
            {
            assertThat(requestObserver, is(notNullValue()));

            MapListenerRequest request = Requests.initListenerChannel(sScope, sCacheName, serializerName);
            requestObserver.onNext(request);

            // wait for subscribed response
            responseObserver.awaitCount(1)
                    .assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(1);

            MapListenerResponse response = responseObserver.valueAt(0);
            assertThat(response, is(notNullValue()));
            assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
            assertThat(response.getSubscribed().getUid(), is(request.getUid()));

            cache.truncate();

            // wait for the events
            responseObserver.awaitCount(2)
                    .assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(2);

            response = responseObserver.valueAt(1);
            assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.TRUNCATED));
            assertThat(response.getTruncated().getCache(), is(cache.getCacheName()));
            }
        finally
            {
            requestObserver.onCompleted();
            }
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldSubscribeToSingleEventForSingleKey(String serializerName, Serializer serializer, String sScope)
        {
        String                                sCacheName = "test-events-" + System.currentTimeMillis();
        CollectingMapListener<String, String> listener   = new CollectingMapListener<>();
        NamedCache<String, String>            cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.addMapListener(listener, "key-2", false);

        TestStreamObserver<MapListenerResponse> responseObserver = new TestStreamObserver<>();

        NamedCacheService service = createService();
        StreamObserver<MapListenerRequest> requestObserver = service.events(responseObserver);

        try
            {
            assertThat(requestObserver, is(notNullValue()));

            // subscribe to events for key-2
            ByteString         keyBytes = BinaryHelper.toByteString("key-2", serializer);
            MapListenerRequest request = Requests.addKeyMapListener(sScope, sCacheName, serializerName, keyBytes,
                                                                    false, false, ByteString.EMPTY);
            requestObserver.onNext(request);

            // wait for subscribed response
            responseObserver.awaitCount(1)
                    .assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(1);

            MapListenerResponse response = responseObserver.valueAt(0);
            assertThat(response, is(notNullValue()));
            assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
            assertThat(response.getSubscribed().getUid(), is(request.getUid()));

            // update the cache to generate events
            cache.put("key-2", "value-2");

            // wait for the events
            listener.awaitCount(1);
            responseObserver.awaitCount(2)  // <--- observer has one more response due to the subscribed response
                    .assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(2);

            List<MapEvent<String, String>> expectedEvents = listener.values();
            List<MapEvent<String, String>> actualEvents   = toMapEvents(cache, responseObserver.values(), serializer);
            assertEqual(actualEvents, expectedEvents);
            }
        finally
            {
            requestObserver.onCompleted();
            cache.removeMapListener(listener, "key-2");
            }
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldSubscribeToAllEventsForSingleKey(String serializerName, Serializer serializer, String sScope)
        {
        String                                sCacheName = "test-events-" + System.currentTimeMillis();
        CollectingMapListener<String, String> listener   = new CollectingMapListener<>();
        NamedCache<String, String>            cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.addMapListener(listener, "key-2", false);

        TestStreamObserver<MapListenerResponse> responseObserver = new TestStreamObserver<>();

        NamedCacheService service         = createService();
        StreamObserver<MapListenerRequest> requestObserver = service.events(responseObserver);

        try
            {
            assertThat(requestObserver, is(notNullValue()));

            // subscribe to events for key-2
            ByteString         keyBytes = BinaryHelper.toByteString("key-2", serializer);
            MapListenerRequest request  = Requests.addKeyMapListener(sScope, sCacheName, serializerName, keyBytes,
                                                                     false, false, ByteString.EMPTY);
            requestObserver.onNext(request);

            // wait for subscribed response
            responseObserver.awaitCount(1)
                    .assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(1);

            MapListenerResponse response = responseObserver.valueAt(0);
            assertThat(response, is(notNullValue()));
            assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
            assertThat(response.getSubscribed().getUid(), is(request.getUid()));

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
            listener.awaitCount(3);
            responseObserver.awaitCount(4)  // <--- observer has one more response due to the subscribed response
                    .assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(4);

            List<MapEvent<String, String>> expectedEvents = listener.values();
            List<MapEvent<String, String>> actualEvents   = toMapEvents(cache, responseObserver.values(), serializer);
            assertEqual(actualEvents, expectedEvents);
            }
        finally
            {
            requestObserver.onCompleted();
            cache.removeMapListener(listener, "key-2");
            }
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldAddPrimingListenerForNonExistentKey(String serializerName, Serializer serializer, String sScope)
        {
        String                     sCacheName = "test-events-" + System.currentTimeMillis();
        String                     key        = "key-2";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        TestStreamObserver<MapListenerResponse> responseObserver = new TestStreamObserver<>();
        NamedCacheService service          = createService();
        StreamObserver<MapListenerRequest>      requestObserver  = service.events(responseObserver);

        try
            {
            assertThat(requestObserver, is(notNullValue()));

            // subscribe to events for key-2
            ByteString         keyBytes = BinaryHelper.toByteString(key, serializer);
            MapListenerRequest request = Requests.addKeyMapListener(sScope, sCacheName, serializerName, keyBytes,
                                                                    false, true, ByteString.EMPTY);
            requestObserver.onNext(request);

            // wait for the priming and subscribed responses
            responseObserver.awaitCount(2)
                    .assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(2);

            MapListenerResponse response = responseObserver.valueAt(0);
            assertThat(response, is(notNullValue()));
            assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.EVENT));
            MapEventResponse eventResponse = response.getEvent();
            assertThat(eventResponse.getKey(), is(notNullValue()));
            assertThat(BinaryHelper.fromByteString(eventResponse.getKey(), serializer), is(key));
            assertThat(eventResponse.getPriming(), is(true));
            assertThat(eventResponse.getPriming(), is(true));
            assertThat(eventResponse.getSynthetic(), is(true));
            assertThat(eventResponse.getOldValue(), is(ByteString.EMPTY));
            assertThat(eventResponse.getNewValue(), is(ByteString.EMPTY));

            response = responseObserver.valueAt(1);
            assertThat(response, is(notNullValue()));
            assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
            assertThat(response.getSubscribed().getUid(), is(request.getUid()));
            }
        finally
            {
            requestObserver.onCompleted();
            }
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldAddPrimingListenerForExistingKey(String serializerName, Serializer serializer, String sScope)
        {
        String                     sCacheName = "test-events-" + System.currentTimeMillis();
        String                     key        = "key-2";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.put(key, "value-2");

        TestStreamObserver<MapListenerResponse> responseObserver = new TestStreamObserver<>();
        NamedCacheService service          = createService();
        StreamObserver<MapListenerRequest>      requestObserver  = service.events(responseObserver);

        try
            {
            assertThat(requestObserver, is(notNullValue()));

            // subscribe to events for key-2
            ByteString         keyBytes = BinaryHelper.toByteString(key, serializer);
            MapListenerRequest request  = Requests
                    .addKeyMapListener(sScope, sCacheName, serializerName, keyBytes, false, true, ByteString.EMPTY);
            requestObserver.onNext(request);

            // wait for the priming and subscribed responses
            responseObserver.awaitCount(2)
                    .assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(2);

            MapListenerResponse response = responseObserver.valueAt(0);
            assertThat(response, is(notNullValue()));
            assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.EVENT));
            MapEventResponse eventResponse = response.getEvent();
            assertThat(eventResponse.getKey(), is(notNullValue()));
            assertThat(BinaryHelper.fromByteString(eventResponse.getKey(), serializer), is(key));
            assertThat(eventResponse.getPriming(), is(true));
            assertThat(eventResponse.getSynthetic(), is(true));
            assertThat(eventResponse.getOldValue(), is(ByteString.EMPTY));
            assertThat(eventResponse.getNewValue(), is(BinaryHelper.toByteString("value-2", serializer)));

            response = responseObserver.valueAt(1);
            assertThat(response, is(notNullValue()));
            assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
            assertThat(response.getSubscribed().getUid(), is(request.getUid()));
            }
        finally
            {
            requestObserver.onCompleted();
            }
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldAddMapTrigger(String serializerName, Serializer serializer, String sScope)
        {
        String                     sCacheName = "test-events-" + System.currentTimeMillis();
        NamedCache<String, Person> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        TestStreamObserver<MapListenerResponse> responseObserver = new TestStreamObserver<>();
        NamedCacheService service          = createService();
        StreamObserver<MapListenerRequest>      requestObserver  = service.events(responseObserver);

        try
            {
            assertThat(requestObserver, is(notNullValue()));

            String             sKey         = "iron-man";
            ByteString         triggerBytes = BinaryHelper.toByteString(new PersonMapTrigger(), serializer);
            MapListenerRequest request      = Requests.addFilterMapListener(sScope, sCacheName, serializerName,
                                                                            ByteString.EMPTY,
                                                                       0L, false, false,
                                                                            triggerBytes);
            requestObserver.onNext(request);

            // wait for subscribed response
            responseObserver.awaitCount(1)
                    .assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(1);

            MapListenerResponse response = responseObserver.valueAt(0);
            assertThat(response, is(notNullValue()));
            assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
            assertThat(response.getSubscribed().getUid(), is(request.getUid()));

            // with the trigger removed the person's last name should be converted to upper case
            Person person = new Person("Tony", "Stark", 53, "male");
            cache.put(sKey, person);
            Person cached = cache.get(sKey);
            assertThat(cached.getLastName(), is(person.getLastName().toUpperCase()));

            // should not get any more events
            responseObserver.assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(1);

            // Now remove the trigger
            request = Requests.removeFilterMapListener(sScope, sCacheName, serializerName, ByteString.EMPTY, 0L, false, false, triggerBytes);
            requestObserver.onNext(request);

            // wait for unsubscribed response
            responseObserver.awaitCount(2)
                    .assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(2);

            response = responseObserver.valueAt(1);
            assertThat(response, is(notNullValue()));
            assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.UNSUBSCRIBED));
            assertThat(response.getUnsubscribed().getUid(), is(request.getUid()));

            // with the trigger removed the person's last name should be unchanged
            cache.put(sKey, person);
            cached = cache.get(sKey);
            assertThat(cached.getLastName(), is(person.getLastName()));
            }
        finally
            {
            requestObserver.onCompleted();
            }
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldSubscribeToEventsForMultipleKeys(String serializerName, Serializer serializer, String sScope)
        {
        String                                sCacheName = "test-events-" + System.currentTimeMillis();
        CollectingMapListener<String, String> listener   = new CollectingMapListener<>();
        NamedCache<String, String>            cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.addMapListener(listener, "key-2", false);
        cache.addMapListener(listener, "key-4", false);

        TestStreamObserver<MapListenerResponse> responseObserver = new TestStreamObserver<>();
        NamedCacheService service          = createService();
        StreamObserver<MapListenerRequest>      requestObserver  = service.events(responseObserver);

        try
            {
            assertThat(requestObserver, is(notNullValue()));

            // subscribe to events for key-2
            ByteString         keyBytes = BinaryHelper.toByteString("key-2", serializer);
            MapListenerRequest request  = Requests.addKeyMapListener(sScope, sCacheName, serializerName, keyBytes,
                                                                     false, false, ByteString.EMPTY);
            requestObserver.onNext(request);

            // wait for subscribed response
            responseObserver.awaitCount(1)
                    .assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(1);

            MapListenerResponse response = responseObserver.valueAt(0);
            assertThat(response, is(notNullValue()));
            assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
            assertThat(response.getSubscribed().getUid(), is(request.getUid()));

            // subscribe to events for key-4
            keyBytes = BinaryHelper.toByteString("key-4", serializer);
            request  = Requests.addKeyMapListener(sScope, sCacheName, serializerName, keyBytes, false,
                                                  false, ByteString.EMPTY);
            requestObserver.onNext(request);

            // wait for subscribed response
            responseObserver.awaitCount(2)
                    .assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(2);

            response = responseObserver.valueAt(1);
            assertThat(response, is(notNullValue()));
            assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
            assertThat(response.getSubscribed().getUid(), is(request.getUid()));

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
            listener.awaitCount(6);
            responseObserver.awaitCount(8)  // <--- observer has two more responses due to the subscribed responses
                    .assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(8);

            List<MapEvent<String, String>> expectedEvents = listener.values();
            List<MapEvent<String, String>> actualEvents   = toMapEvents(cache, responseObserver.values(), serializer);
            assertEqual(actualEvents, expectedEvents);
            }
        finally
            {
            requestObserver.onCompleted();
            cache.removeMapListener(listener, "key-2");
            cache.removeMapListener(listener, "key-4");
            }
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldSubscribeToEventsForFilter(String serializerName, Serializer serializer, String sScope)
        {
        String                                 sCacheName = "test-events-" + System.currentTimeMillis();
        CollectingMapListener<String, Integer> listener   = new CollectingMapListener<>();
        NamedCache<String, Integer>            cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        MapEventFilter<String, Integer> filter      = new MapEventFilter<>(Filters.less(Extractors.identity(), 10));
        ByteString                      filterBytes = BinaryHelper.toByteString(filter, serializer);
        cache.addMapListener(listener, filter, false);

        TestStreamObserver<MapListenerResponse> responseObserver = new TestStreamObserver<>();
        NamedCacheService service          = createService();
        StreamObserver<MapListenerRequest>      requestObserver  = service.events(responseObserver);

        try
            {
            assertThat(requestObserver, is(notNullValue()));

            // subscribe to all events
            long               nFilterId = 19L;
            MapListenerRequest request   = Requests.addFilterMapListener(sScope, sCacheName, serializerName, filterBytes,
                                                                         nFilterId, false, false,
                                                                         ByteString.EMPTY);
            requestObserver.onNext(request);

            // wait for subscribed response
            responseObserver.awaitCount(1)
                    .assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(1);

            MapListenerResponse response = responseObserver.valueAt(0);
            assertThat(response, is(notNullValue()));
            assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
            assertThat(response.getSubscribed().getUid(), is(request.getUid()));

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
            listener.awaitCount(20);
            responseObserver.awaitCount(21)  // <--- observer has one more response due to the subscribed response
                    .assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(21);

            List<MapEvent<String, Integer>> expectedEvents = listener.values();
            List<MapEvent<String, Integer>> actualEvents   = toMapEventsForFilterId(cache, responseObserver.values(),
                                                                                   serializer, nFilterId);
            assertEqual(actualEvents, expectedEvents);
            }
        finally
            {
            requestObserver.onCompleted();
            cache.removeMapListener(listener, filter);
            }
        }

    //@Disabled
    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldSubscribeToEventsForKeyAndFilter(String serializerName, Serializer serializer, String sScope)
        {
        String                                sCacheName = "test-events-" + System.currentTimeMillis();
        String                                key         = "key-2";
        CollectingMapListener<String, Person> listenerOne = new CollectingMapListener<>();
        CollectingMapListener<String, Person> listenerTwo = new CollectingMapListener<>();
        NamedCache<String, Person>            cache       = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        MapEventFilter<String, Person> filter      = new MapEventFilter<>(Filters.equal(
                Extractors.extract("getAge()"), 10));
        ByteString                     filterBytes = BinaryHelper.toByteString(filter, serializer);
        ByteString                     keyBytes    = BinaryHelper.toByteString(key, serializer);
        cache.addMapListener(listenerOne, filter, false);
        cache.addMapListener(listenerTwo, key, false);

        TestStreamObserver<MapListenerResponse> responseObserver = new TestStreamObserver<>();
        NamedCacheService service          = createService();
        StreamObserver<MapListenerRequest>      requestObserver  = service.events(responseObserver);

        try
            {
            assertThat(requestObserver, is(notNullValue()));

            // subscribe to all events
            long               lFilterIdOne = 19L;
            MapListenerRequest requestOne   = Requests.addFilterMapListener(sScope, sCacheName, serializerName, filterBytes,
                                                                            lFilterIdOne, false, false,
                                                                            ByteString.EMPTY);
            MapListenerRequest requestTwo   = Requests.addKeyMapListener(sScope, sCacheName, serializerName, keyBytes,
                                                                         false,false, ByteString.EMPTY);
            requestObserver.onNext(requestOne);
            requestObserver.onNext(requestTwo);

            // wait for subscribed responses
            responseObserver.awaitCount(2)
                    .assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(2);

            MapListenerResponse response = responseObserver.valueAt(0);
            assertThat(response, is(notNullValue()));
            assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
            assertThat(response.getSubscribed().getUid(), is(requestOne.getUid()));

            // update the cache to generate events
            cache.put("key-1", new Person("first", "last", 10, ""));
            cache.put(key, new Person("first", "last", 20, ""));

            // wait for the events
            listenerOne.awaitCount(1);
            listenerTwo.awaitCount(1);
            responseObserver.awaitCount(4)  // <--- observer has one more response due to the subscribed response
                    .assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(4);

            List<MapEvent<String, Person>> actualEventsOne = toMapEventsForFilterId(cache, responseObserver.values(), serializer, lFilterIdOne);
            assertEqual(actualEventsOne, listenerOne.values());
//            List<MapEvent<String, Person>> actualEventsTwo = toMapEventsForFilterId(cache, responseObserver.values(), serializer, filterIdTwo);
//            assertEqual(actualEventsTwo, listenerTwo.values());
            }
        finally
            {
            requestObserver.onCompleted();
            cache.removeMapListener(listenerOne, filter);
            cache.removeMapListener(listenerTwo, key);
            }
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldSubscribeToEventsForMultipleFilter(String serializerName, Serializer serializer, String sScope)
        {
        String                                sCacheName  = "test-events-" + System.currentTimeMillis();
        CollectingMapListener<String, Person> listenerOne = new CollectingMapListener<>();
        CollectingMapListener<String, Person> listenerTwo = new CollectingMapListener<>();
        NamedCache<String, Person>            cache       = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        MapEventFilter<String, Person> filterOne      = new MapEventFilter<>(Filters.equal(
                Extractors.extract("getAge()"), 10));
        MapEventFilter<String, Person> filterTwo      = new MapEventFilter<>(Filters.equal(
                Extractors.extract("getAge()"), 20));
        ByteString                     filterBytesOne = BinaryHelper.toByteString(filterOne, serializer);
        ByteString                     filterBytesTwo = BinaryHelper.toByteString(filterTwo, serializer);
        cache.addMapListener(listenerOne, filterOne, false);
        cache.addMapListener(listenerTwo, filterTwo, false);

        TestStreamObserver<MapListenerResponse> responseObserver = new TestStreamObserver<>();
        NamedCacheService service          = createService();
        StreamObserver<MapListenerRequest>      requestObserver  = service.events(responseObserver);

        try
            {
            assertThat(requestObserver, is(notNullValue()));

            // subscribe to all events
            long               nFilterIdOne = 19L;
            long               nFilterIdTwo = 20L;
            MapListenerRequest requestOne    = Requests.addFilterMapListener(sScope, sCacheName, serializerName,
                                                                             filterBytesOne, nFilterIdOne,
                                                                             false, false, ByteString.EMPTY);
            MapListenerRequest requestTwo    = Requests.addFilterMapListener(sScope, sCacheName, serializerName, filterBytesTwo,
                                                                             nFilterIdTwo, false, false,
                                                                             ByteString.EMPTY);
            requestObserver.onNext(requestOne);
            requestObserver.onNext(requestTwo);

            // wait for subscribed responses
            responseObserver.awaitCount(2)
                    .assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(2);

            MapListenerResponse response = responseObserver.valueAt(0);
            assertThat(response, is(notNullValue()));
            assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
            assertThat(response.getSubscribed().getUid(), is(requestOne.getUid()));

            // update the cache to generate events
            cache.put("key-1", new Person("first", "last", 10, ""));
            cache.put("key-2", new Person("first", "last", 20, ""));

            // wait for the events
            listenerOne.awaitCount(1);
            listenerOne.awaitCount(1);
            responseObserver.awaitCount(4)  // <--- observer has one more response due to the subscribed response
                    .assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(4);

            List<MapEvent<String, Person>> actualEventsOne = toMapEventsForFilterId(cache, responseObserver.values(),
                                                                                    serializer, nFilterIdOne);
            assertEqual(actualEventsOne, listenerOne.values());
            List<MapEvent<String, Person>> actualEventsTwo = toMapEventsForFilterId(cache, responseObserver.values(),
                                                                                    serializer, nFilterIdTwo);
            assertEqual(actualEventsTwo, listenerTwo.values());
            }
        finally
            {
            requestObserver.onCompleted();
            cache.removeMapListener(listenerOne, filterOne);
            cache.removeMapListener(listenerTwo, filterTwo);
            }
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldAddPrimingListenerWithFilter(String serializerName, Serializer serializer, String sScope)
        {
        String                     sCacheName = "test-events-" + System.currentTimeMillis();
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.put("key-4", "value-4");

        Set<String>            keys        = new LinkedHashSet<>(Arrays.asList("key-2", "key-4"));
        InKeySetFilter<String> filter      = new InKeySetFilter<>(Filters.always(), keys);
        ByteString             filterBytes = BinaryHelper.toByteString(filter, serializer);

        TestStreamObserver<MapListenerResponse> responseObserver = new TestStreamObserver<>();
        NamedCacheService service          = createService();
        StreamObserver<MapListenerRequest>      requestObserver  = service.events(responseObserver);

        try
            {
            assertThat(requestObserver, is(notNullValue()));

            // subscribe to all events
            long               nFilterId = 19L;
            MapListenerRequest request   = Requests.addFilterMapListener(sScope, sCacheName, serializerName, filterBytes,
                                                                         nFilterId, false, true,
                                                                         ByteString.EMPTY);
            requestObserver.onNext(request);

            // wait for subscribed response
            responseObserver.awaitCount(3)
                    .assertNotComplete()
                    .assertNoErrors()
                    .assertValueCount(3);

            // response 2 is the subscribe response
            MapListenerResponse response = responseObserver.valueAt(2);
            assertThat(response, is(notNullValue()));
            assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
            assertThat(response.getSubscribed().getUid(), is(request.getUid()));

            assertThat(responseObserver.valueAt(0).getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.EVENT));
            assertThat(responseObserver.valueAt(1).getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.EVENT));

            MapEventResponse responseKey2;
            MapEventResponse responseKey4;

            if (BinaryHelper.fromByteString(responseObserver.valueAt(0).getEvent().getKey(), serializer).equals("key-2"))
                {
                responseKey2 = responseObserver.valueAt(0).getEvent();
                responseKey4 = responseObserver.valueAt(1).getEvent();
                }
            else
                {
                responseKey4 = responseObserver.valueAt(0).getEvent();
                responseKey2 = responseObserver.valueAt(1).getEvent();
                }

            assertThat(BinaryHelper.fromByteString(responseKey2.getKey(), serializer), is("key-2"));
            assertThat(responseKey2.getPriming(),   is(true));
            assertThat(responseKey2.getSynthetic(), is(true));
            assertThat(responseKey2.getOldValue(),  is(ByteString.EMPTY));
            assertThat(responseKey2.getNewValue(),  is(ByteString.EMPTY));

            assertThat(BinaryHelper.fromByteString(responseKey4.getKey(), serializer), is("key-4"));
            assertThat(responseKey4.getPriming(),   is(true));
            assertThat(responseKey4.getSynthetic(), is(true));
            assertThat(responseKey4.getOldValue(),  is(ByteString.EMPTY));
            assertThat(responseKey4.getNewValue(),  is(BinaryHelper.toByteString("value-4", serializer)));

            }
        finally
            {
            requestObserver.onCompleted();
            }
        }

    // ----- Get ------------------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldGetExistingKey(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.put("key-1", "value-1");

        NamedCacheService              service  = createService();
        Binary                         binary   = ExternalizableHelper.toBinary("key-1", serializer);
        ByteString                     key      = BinaryHelper.toByteString(binary);

        TestStreamObserver<OptionalValue> observer = new TestStreamObserver<>();
        
        service.get(Requests.get(sScope, sCacheName, serializerName, key), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);
        
        OptionalValue value  = observer.valueAt(0);
        assertThat(value, is(notNullValue()));
        assertThat(value.getPresent(), is(true));
        assertThat(fromByteString(value.getValue(), serializer, String.class), is("value-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldGetExistingKeyMappedToNull(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.put("key-1", null);

        NamedCacheService service  = createService();
        Binary                         binary   = ExternalizableHelper.toBinary("key-1", serializer);
        ByteString                     key      = BinaryHelper.toByteString(binary);
        
        TestStreamObserver<OptionalValue> observer = new TestStreamObserver<>();
        service.get(Requests.get(sScope, sCacheName, serializerName, key), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);
        
        OptionalValue value  = observer.valueAt(0);
        assertThat(value, is(notNullValue()));
        assertThat(value.getPresent(), is(true));
        assertThat(fromByteString(value.getValue(), serializer, String.class), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldGetNonExistentKey(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<Binary, Binary> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        NamedCacheService service  = createService();
        Binary                         binary   = ExternalizableHelper.toBinary("key-1", serializer);
        ByteString                     key      = BinaryHelper.toByteString(binary);
        
        TestStreamObserver<OptionalValue> observer = new TestStreamObserver<>();
        service.get(Requests.get(sScope, sCacheName, serializerName, key), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);
        
        OptionalValue value  = observer.valueAt(0);
        assertThat(value, is(notNullValue()));
        assertThat(value.getPresent(), is(false));
        assertThat(fromByteString(value.toByteString(), serializer, String.class), is(nullValue()));
        }

    // ----- GetAll ---------------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldGetAllForEmptyKeyCollection(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 5);

        assertGetAll(cache, serializerName, serializer, sScope, Collections.emptyList());
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldGetAllWhenNoKeysMatch(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 4);

        Collection<String> colKeys = Arrays.asList("key-5", "key-6");

        assertGetAll(cache, serializerName, serializer, sScope, colKeys);
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldGetAllWhenAllKeysMatch(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 4);

        Collection<String> colKeys = Arrays.asList("key-2", "key-4");

        assertGetAll(cache, serializerName, serializer, sScope, colKeys);
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldGetAllWhenAllSomeKeysMatch(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 4);

        Collection<String> colKeys = Arrays.asList("key-0", "key-2", "key-4", "key-5");

        assertGetAll(cache, serializerName, serializer, sScope, colKeys);
        }

    void assertGetAll(NamedCache<String, String> cache, String serializerName, Serializer serializer, String sScope, Collection<String> keys)
            throws Exception
        {
        Map<String, String> mapExpected = cache.getAll(keys);

        Collection<ByteString> serializedKeys = keys.stream()
                .map(s -> toByteString(s, serializer))
                .collect(Collectors.toList());

        NamedCacheService service  = createService();
        TestStreamObserver<Entry> observer = new TestStreamObserver<>();
        service.getAll(Requests.getAll(sScope, cache.getCacheName(), serializerName, serializedKeys), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(mapExpected.size());

        Map<String, String> oResult = new HashMap<>();
        for (Entry entry : observer.values())
            {
            String sKey   = fromByteString(entry.getKey(), serializer, String.class);
            String sValue = fromByteString(entry.getValue(), serializer, String.class);
            oResult.put(sKey, sValue);
            }

        assertThat(oResult, is(mapExpected));
        }

    // ----- Invoke ---------------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallInvoke(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "people";
        NamedCache<String, Person> cache      = ensureEmptyCache(sScope, sCacheName);
        String                     sKey       = "bb";
        Person                     person     = new Person("bob", "builder", 25, "male");
        cache.put(sKey, person);

        ValueExtractor<Person, String>                      extractor = new UniversalExtractor<>("lastName");
        InvocableMap.EntryProcessor<String, Person, String> processor = new ExtractorProcessor<>(extractor);

        InvokeRequest request = Requests.invoke(sScope, sCacheName, serializerName,
                                                BinaryHelper.toByteString(sKey, serializer),
                                                BinaryHelper.toByteString(processor, serializer));

        NamedCacheService              service  = createService();
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        
        service.invoke(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BytesValue value = observer.valueAt(0);
        assertThat(value, is(notNullValue()));
        assertThat(BinaryHelper.fromBytesValue(value, serializer), is(person.getLastName()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallInvokeWithMissingEntryProcessor(String serializerName, Serializer serializer, String sScope)
            throws Exception
        {
        String sCacheName = "people";

        InvokeRequest request = InvokeRequest.newBuilder()
                .setScope(sScope)
                .setCache(sCacheName)
                .setFormat(serializerName)
                .setKey(BinaryHelper.toByteString("foo", serializer))
                .build();

        NamedCacheService              service  = createService();
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        
        service.invoke(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertError(Throwable.class);
        }

    // ----- InvokeAll ------------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallInvokeAllWithFilter(String serializerName, Serializer serializer, String sScope) throws Exception
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

        InvokeAllRequest request = Requests.invokeAll(sScope, sCacheName, serializerName,
                                                      BinaryHelper.toByteString(filter, serializer),
                                                      BinaryHelper.toByteString(processor, serializer));

        NamedCacheService service  = createService();
        TestStreamObserver<Entry> observer = new TestStreamObserver<>();
        service.invokeAll(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(2);

        Map<String, String> map = observer.values().stream()
                .collect(Collectors.toMap(e -> BinaryHelper.fromByteString(e.getKey(), serializer),
                                          e -> BinaryHelper.fromByteString(e.getValue(), serializer)));

        assertThat(map, hasEntry(person1.getLastName(), person1.getFirstName()));
        assertThat(map, hasEntry(person2.getLastName(), person2.getFirstName()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallInvokeAllWithNoFilterOrKeys(String serializerName, Serializer serializer, String sScope) throws Exception
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

        InvokeAllRequest request = InvokeAllRequest.newBuilder()
                .setScope(sScope)
                .setCache(sCacheName)
                .setFormat(serializerName)
                .setProcessor(BinaryHelper.toByteString(processor, serializer))
                .build();

        NamedCacheService service  = createService();
        TestStreamObserver<Entry> observer = new TestStreamObserver<>();
        service.invokeAll(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(cache.size());

        Map<String, String> map = observer.values().stream()
                .collect(Collectors.toMap(e -> BinaryHelper.fromByteString(e.getKey(), serializer),
                                          e -> BinaryHelper.fromByteString(e.getValue(), serializer)));

        assertThat(map, hasEntry(person1.getLastName(), person1.getFirstName()));
        assertThat(map, hasEntry(person2.getLastName(), person2.getFirstName()));
        assertThat(map, hasEntry(person3.getLastName(), person3.getFirstName()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallInvokeAllWithKeys(String serializerName, Serializer serializer, String sScope) throws Exception
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

        InvokeAllRequest request = Requests.invokeAll(sScope, sCacheName, serializerName,
                                                      listKeys,
                                                      BinaryHelper.toByteString(processor, serializer));

        NamedCacheService service  = createService();
        TestStreamObserver<Entry> observer = new TestStreamObserver<>();
        service.invokeAll(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(2);

        Map<String, String> map = observer.values().stream()
                .collect(Collectors.toMap(e -> BinaryHelper.fromByteString(e.getKey(), serializer),
                                          e -> BinaryHelper.fromByteString(e.getValue(), serializer)));

        assertThat(map, hasEntry(person1.getLastName(), person1.getFirstName()));
        assertThat(map, hasEntry(person2.getLastName(), person2.getFirstName()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallInvokeAllWithMissingProcessor(String serializerName, Serializer ignored, String sScope) throws Exception
        {
        String sCacheName = "people";

        InvokeAllRequest request = InvokeAllRequest.newBuilder()
                .setScope(sScope)
                .setCache(sCacheName)
                .setFormat(serializerName)
                .build();

        NamedCacheService service  = createService();
        TestStreamObserver<Entry> observer = new TestStreamObserver<>();
        service.invokeAll(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertError(t -> t instanceof StatusRuntimeException
                                  && ((StatusRuntimeException) t).getStatus().getCode().equals(Status.INVALID_ARGUMENT.getCode()));
        }

    // ----- IsEmpty --------------------------------------------------------

    @ParameterizedTest(name = "{index} scope={0}")
    @MethodSource("getTestScopes")
    public void shouldBeEmpty(String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        NamedCacheService             service  = createService();
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();

        service.isEmpty(Requests.isEmpty(sScope, sCacheName), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValue(BoolValue.of(true));
        }

    @ParameterizedTest(name = "{index} scope={0}")
    @MethodSource("getTestScopes")
    public void shouldNotBeEmpty(String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 10);

        NamedCacheService             service  = createService();
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();

        service.isEmpty(Requests.isEmpty(sScope, sCacheName), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValue(BoolValue.of(false));
        }

    // ----- keySet(Filter) -------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallKeySetWithFilterWhenSomeEntriesMatch(String serializerName, Serializer serializer, String sScope) throws Exception
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

        NamedCacheService              service      = createService();
        Filter<Person>                 filter       = new EqualsFilter<>("getAge", 25);
        List<String>                   listExpected = new ArrayList<>(cache.keySet(filter));
        ByteString                     filterBytes  = BinaryHelper.toByteString(filter, serializer);
        TestStreamObserver<BytesValue> observer     = new TestStreamObserver<>();
        service.keySet(Requests.keySet(sScope, sCacheName, serializerName, filterBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(listExpected.size());

        List<String> oResult = observer.values()
                .stream()
                .map(v -> (String) BinaryHelper.fromBytesValue(v, serializer))
                .collect(Collectors.toList());

        assertThat(oResult, containsInAnyOrder(listExpected.toArray()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallKeySetWithFilterWhenAllEntriesMatch(String serializerName, Serializer serializer, String sScope) throws Exception
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

        NamedCacheService              service      = createService();
        Filter<Person>                 filter       = Filters.always();
        List<String>                   listExpected = new ArrayList<>(cache.keySet(filter));
        ByteString                     filterBytes  = BinaryHelper.toByteString(filter, serializer);
        TestStreamObserver<BytesValue> observer     = new TestStreamObserver<>();

        service.keySet(Requests.keySet(sScope, sCacheName, serializerName, filterBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(listExpected.size());

        List<String> oResult = observer.values()
                .stream()
                .map(v -> (String) BinaryHelper.fromBytesValue(v, serializer))
                .collect(Collectors.toList());

        assertThat(oResult, containsInAnyOrder(listExpected.toArray()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallKeySetWithFilterWhenNoEntriesMatch(String serializerName, Serializer serializer, String sScope) throws Exception
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

        NamedCacheService              service  = createService();
        Filter<Person>                 filter      = new EqualsFilter<>("getAge", 100);
        ByteString                     filterBytes = BinaryHelper.toByteString(filter, serializer);
        TestStreamObserver<BytesValue> observer    = new TestStreamObserver<>();
        service.keySet(Requests.keySet(sScope, sCacheName, serializerName, filterBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(0);

        List<String> oResult = observer.values()
                .stream()
                .map(v -> (String) BinaryHelper.fromBytesValue(v, serializer))
                .collect(Collectors.toList());

        assertThat(oResult.isEmpty(), is(true));
        }

    // ----- Put ------------------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldInsertNewEntry(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        NamedCacheService              service  = createService();
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        ByteString                  key      = toByteString("key-1", serializer);
        ByteString                  value    = toByteString("value-1", serializer);

        service.put(Requests.put(sScope, sCacheName, serializerName, key, value), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BytesValue oResult = observer.valueAt(0);
        assertThat(fromBytesValue(oResult, serializer, String.class), is(nullValue()));

        assertThat(cache.get("key-1"), is("value-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldUpdateEntry(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String sCacheName = "test-cache";
        NamedCache<String, String> cache = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.put("key-1", "value-1");

        NamedCacheService              service  = createService();
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        ByteString                  key      = toByteString("key-1", serializer);
        ByteString                  value    = toByteString("value-2", serializer);

        service.put(Requests.put(sScope, sCacheName, serializerName, key, value), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BytesValue oResult = observer.valueAt(0);
        assertThat(oResult, is(notNullValue()));
        assertThat(fromBytesValue(oResult, serializer, String.class), is("value-1"));

        assertThat(cache.get("key-1"), is("value-2"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldUpdateEntryPreviouslyMappedToNull(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.put("key-1", null);

        NamedCacheService              service  = createService();
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        ByteString                  key      = toByteString("key-1", serializer);
        ByteString                  value    = toByteString("value-2", serializer);

        service.put(Requests.put(sScope, sCacheName, serializerName, key, value), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BytesValue oResult = observer.valueAt(0);
        assertThat(oResult, is(notNullValue()));
        assertThat(fromBytesValue(oResult, serializer, String.class), is(nullValue()));

        assertThat(cache.get("key-1"), is("value-2"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldUpdateEntryWithNullValue(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        ByteString                 key        = toByteString("key-1", serializer);
        ByteString                 value      = toByteString(null, serializer);

        cache.clear();
        cache.put("key-1", "value-1");

        NamedCacheService              service  = createService();
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();

        service.put(Requests.put(sScope, sCacheName, serializerName, key, value), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BytesValue oResult = observer.valueAt(0);
        assertThat(oResult, is(notNullValue()));
        assertThat(fromBytesValue(oResult, serializer, String.class), is("value-1"));

        assertThat(cache.get("key-1"), is(nullValue()));
        }

    // ----- PutAll ---------------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldPutAll(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        ByteString  key1    = toByteString("key-1", serializer);
        ByteString  value1  = toByteString("value-1", serializer);
        ByteString  key2    = toByteString("key-2", serializer);
        ByteString  value2  = toByteString("value-2", serializer);
        List<Entry> listEntries = new ArrayList<>();

        listEntries.add(Entry.newBuilder().setKey(key1).setValue(value1).build());
        listEntries.add(Entry.newBuilder().setKey(key2).setValue(value2).build());

        NamedCacheService         service  = createService();
        TestStreamObserver<Empty> observer = new TestStreamObserver<>();

        service.putAll(Requests.putAll(sScope, sCacheName, serializerName, listEntries), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        assertThat(cache.get("key-1"), is("value-1"));
        assertThat(cache.get("key-2"), is("value-2"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldPutAllWithExpiry(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        ByteString key1   = toByteString("key-1", serializer);
        ByteString value1 = toByteString("value-1", serializer);
        ByteString key2   = toByteString("key-2", serializer);
        ByteString value2 = toByteString("value-2", serializer);

        List<Entry> listEntries = new ArrayList<>();
        listEntries.add(Entry.newBuilder().setKey(key1).setValue(value1).build());
        listEntries.add(Entry.newBuilder().setKey(key2).setValue(value2).build());

        long                      cMillis  = 5000L;
        NamedCacheService         service  = createService();
        TestStreamObserver<Empty> observer = new TestStreamObserver<>();

        service.putAll(Requests.putAll(sScope, sCacheName, serializerName, listEntries, cMillis), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        assertThat(cache.size(), is(2));
        Eventually.assertDeferred(cache::size, is(0));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldPutAllWithZeroEntries(String serializerName, Serializer ignored, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        NamedCacheService         service     = createService();
        TestStreamObserver<Empty> observer    = new TestStreamObserver<>();
        List<Entry>               listEntries = new ArrayList<>();

        service.putAll(Requests.putAll(sScope, sCacheName, serializerName, listEntries), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        assertThat(cache.isEmpty(), is(true));
        }

    // ----- PutIfAbsent ----------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldPutIfAbsentForNonExistentKey(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        NamedCacheService              service  = createService();
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        ByteString                     key      = toByteString("key-1", serializer);
        ByteString                     value    = toByteString("value-1", serializer);

        service.putIfAbsent(Requests.putIfAbsent(sScope, sCacheName, serializerName, key, value), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BytesValue oResult = observer.valueAt(0);
        assertThat(oResult, is(notNullValue()));
        assertThat(fromBytesValue(oResult, serializer, String.class), is(nullValue()));
        assertThat(cache.get("key-1"), is("value-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldPutIfAbsentForExistingKey(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.put("key-1", "value-1");

        NamedCacheService              service  = createService();
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        ByteString                     key      = toByteString("key-1", serializer);
        ByteString                     value    = toByteString("value-2", serializer);

        service.putIfAbsent(Requests.putIfAbsent(sScope, sCacheName, serializerName, key, value), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BytesValue oResult = observer.valueAt(0);
        assertThat(oResult, is(notNullValue()));
        assertThat(fromBytesValue(oResult, serializer, String.class), is("value-1"));
        assertThat(cache.get("key-1"), is("value-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldPutIfAbsentForExistingKeyMappedToNull(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.put("key-1", null);

        NamedCacheService              service  = createService();
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        ByteString                     key      = toByteString("key-1", serializer);
        ByteString                     value    = toByteString("value-2", serializer);

        service.putIfAbsent(Requests.putIfAbsent(sScope, sCacheName, serializerName, key, value), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BytesValue oResult = observer.valueAt(0);
        assertThat(oResult, is(notNullValue()));
        assertThat(fromBytesValue(oResult, serializer, String.class), is(nullValue()));
        assertThat(cache.get("key-1"), is("value-2"));
        }

    // ----- Remove ---------------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldRemoveOnNonExistentEntry(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        int count = 10;
        clearAndPopulate(cache, count);

        NamedCacheService              service  = createService();
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        ByteString                     key      = toByteString("key-100", serializer);

        service.remove(Requests.remove(sScope, sCacheName, serializerName, key), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BytesValue oResult = observer.valueAt(0);
        assertThat(oResult, is(notNullValue()));
        assertThat(fromBytesValue(oResult, serializer, String.class), is(nullValue()));
        assertThat(cache.size(), is(count));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReturnPreviousValueForRemoveOnExistingMapping(String serializerName, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        int cCount = 10;
        clearAndPopulate(cache, cCount);

        NamedCacheService              service  = createService();
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        ByteString                     key      = toByteString("key-1", serializer);

        service.remove(Requests.remove(sScope, sCacheName, serializerName, key), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BytesValue oResult = observer.valueAt(0);
        assertThat(oResult, is(notNullValue()));
        assertThat(fromBytesValue(oResult, serializer, String.class), is("value-1"));
        assertThat(cache.get("key-1"), is(nullValue()));
        assertThat(cache.size(), is(cCount - 1));
        }

    // ----- Remove Value ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReturnFalseForRemoveMappingOnNonExistentMapping(String serializerName, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<Binary, Binary> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        NamedCacheService             service  = createService();
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        ByteString                    key      = toByteString("key-1", serializer);
        ByteString                    value    = toByteString("value-123", serializer);

        service.removeMapping(Requests.remove(sScope, sCacheName, serializerName, key, value), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BoolValue oResult = observer.valueAt(0);
        assertThat(oResult, is(notNullValue()));
        assertThat(oResult.getValue(), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReturnFalseForRemoveMappingOnNonMatchingMapping(String serializerName, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.put("key-1", "value-1");

        NamedCacheService             service  = createService();
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        ByteString                    key      = toByteString("key-1", serializer);
        ByteString                    value    = toByteString("value-123", serializer);

        service.removeMapping(Requests.remove(sScope, sCacheName, serializerName, key, value), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BoolValue oResult = observer.valueAt(0);
        assertThat(oResult, is(notNullValue()));
        assertThat(oResult.getValue(), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReturnTrueForRemoveMappingOnMatchingMapping(String serializerName, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();
        cache.put("key-1", "value-123");

        NamedCacheService             service  = createService();
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        ByteString                    key      = toByteString("key-1", serializer);
        ByteString                    value    = toByteString("value-123", serializer);

        service.removeMapping(Requests.remove(sScope, sCacheName, serializerName, key, value), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BoolValue oResult = observer.valueAt(0);
        assertThat(oResult, is(notNullValue()));
        assertThat(oResult.getValue(), is(true));
        }

    // ----- RemoveIndex ----------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    public void shouldRemoveIndexWhenIndexExists(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                        sCacheName = "add-index-cache";
        NamedCache                    cache      = ensureEmptyCache(sScope, sCacheName);
        Map<ValueExtractor, MapIndex> indexMap   = removeIndexes(cache);

        ValueExtractor extractor    = new UniversalExtractor("foo");
        ByteString     binExtractor = toByteString(extractor, serializer);

        // Add the index using the normal cache
        cache.addIndex(extractor, false, null);

        NamedCacheService         service  = createService();
        TestStreamObserver<Empty> observer = new TestStreamObserver<>();

        service.removeIndex(Requests.removeIndex(sScope, sCacheName, serializerName, binExtractor), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        assertThat(indexMap.isEmpty(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldRemoveIndexWhenIndexDoesNotExist(String serializerName, Serializer serializer, String sScope) throws Exception
        {
        String                        sCacheName = "add-index-cache";
        NamedCache                    cache      = ensureEmptyCache(sScope, sCacheName);
        Map<ValueExtractor, MapIndex> indexMap   = removeIndexes(cache);

        ValueExtractor extractor    = new UniversalExtractor("foo");
        ByteString     binExtractor = toByteString(extractor, serializer);

        NamedCacheService         service  = createService();
        TestStreamObserver<Empty> observer = new TestStreamObserver<>();

        service.removeIndex(Requests.removeIndex(sScope, sCacheName, serializerName, binExtractor), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        assertThat(indexMap.isEmpty(), is(true));
        }

    // ----- Replace --------------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReturnNullValueForReplaceOnNonExistentMapping(String serializerName, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        NamedCacheService              service  = createService();
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        ByteString                     key      = toByteString("key-1", serializer);
        ByteString                     value    = toByteString("value-123", serializer);

        service.replace(Requests.replace(sScope, sCacheName, serializerName, key, value), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BytesValue oResult = observer.valueAt(0);
        assertThat(oResult, is(notNullValue()));
        assertThat(fromBytesValue(oResult, serializer, String.class), is(nullValue()));
        assertThat(cache.get("key-1"), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReturnNonNullForReplaceOnExistentMapping(String serializerName, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 5);

        NamedCacheService              service  = createService();
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        ByteString                     key      = toByteString("key-1", serializer);
        ByteString                     value    = toByteString("value-123", serializer);

        service.replace(Requests.replace(sScope, sCacheName, serializerName, key, value), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BytesValue oResult = observer.valueAt(0);
        assertThat(oResult, is(notNullValue()));
        assertThat(fromBytesValue(oResult, serializer, String.class), is("value-1"));
        assertThat(cache.get("key-1"), is("value-123"));
        }

    // ----- Replace Value --------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReturnFalseForReplaceMappingOnNonExistentMapping(String serializerName, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        NamedCacheService             service  = createService();
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        ByteString                    key       = toByteString("key-1", serializer);
        ByteString                    prevValue = toByteString("value-1", serializer);
        ByteString                    newValue  = toByteString("value-123", serializer);

        service.replaceMapping(Requests.replace(sScope, sCacheName, serializerName, key, prevValue, newValue), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BoolValue oResult = observer.valueAt(0);
        assertThat(oResult, is(notNullValue()));
        assertThat(oResult.getValue(), is(false));
        assertThat(cache.get("key-1"), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReturnFalseForReplaceMappingOnNonMatchingMapping(String serializerName, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 5);

        NamedCacheService             service  = createService();
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        ByteString                 key       = toByteString("key-1", serializer);
        ByteString                 prevValue = toByteString("value-123", serializer);
        ByteString                 newValue  = toByteString("value-456", serializer);

        service.replaceMapping(Requests.replace(sScope, sCacheName, serializerName, key, prevValue, newValue), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BoolValue oResult = observer.valueAt(0);
        assertThat(oResult, is(notNullValue()));
        assertThat(oResult.getValue(), is(false));
        assertThat(cache.get("key-1"), is("value-1"));
        }


    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldReturnTrueForReplaceMappingOnMatchingMapping(String serializerName, Serializer serializer, String sScope)
            throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 5);

        NamedCacheService             service  = createService();
        TestStreamObserver<BoolValue> observer = new TestStreamObserver<>();
        ByteString                 key       = toByteString("key-1", serializer);
        ByteString                 prevValue = toByteString("value-1", serializer);
        ByteString                 newValue  = toByteString("value-123", serializer);

        service.replaceMapping(Requests.replace(sScope, sCacheName, serializerName, key, prevValue, newValue), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        BoolValue oResult = observer.valueAt(0);
        assertThat(oResult, is(notNullValue()));
        assertThat(oResult.getValue(), is(true));
        assertThat(cache.get("key-1"), is("value-123"));
        }

    // ----- Size -----------------------------------------------------------

    @ParameterizedTest(name = "{index} scope={0}")
    @MethodSource("getTestScopes")
    public void shouldGetSizeOfEmptyCache(String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        cache.clear();

        NamedCacheService              service  = createService();
        TestStreamObserver<Int32Value> observer = new TestStreamObserver<>();

        service.size(Requests.size(sScope, sCacheName), observer);
        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        Int32Value oResult = observer.valueAt(0);
        assertThat(oResult, is(notNullValue()));
        assertThat(oResult.getValue(), is(0));
        }

    @ParameterizedTest(name = "{index} scope={0}")
    @MethodSource("getTestScopes")
    public void shouldGetSizeOfPopulatedCache(String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 10);

        NamedCacheService              service  = createService();
        TestStreamObserver<Int32Value> observer = new TestStreamObserver<>();

        service.size(Requests.size(sScope, sCacheName), observer);
        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        Int32Value oResult = observer.valueAt(0);
        assertThat(oResult, is(notNullValue()));
        assertThat(oResult.getValue(), is(cache.size()));
        }

    // ----- Truncate -------------------------------------------------------

    @ParameterizedTest(name = "{index} scope={0}")
    @MethodSource("getTestScopes")
    public void shouldTruncate(String sScope) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = ensureEmptyCache(sScope, sCacheName);
        clearAndPopulate(cache, 5);

        NamedCacheService         service  = createService();
        TestStreamObserver<Empty> observer = new TestStreamObserver<>();

        service.truncate(Requests.truncate(sScope, sCacheName), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        assertThat(cache.isEmpty(), is(true));
        }

    // ----- values(Filter) -------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallValuesWithFilterWhenSomeEntriesMatch(String serializerName, Serializer serializer, String sScope)
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

        NamedCacheService service     = createService();
        Filter<Person>     filter      = new EqualsFilter<>("getAge", 25);
        Collection<Person> colExpected = cache.values(filter);

        ByteString                     filterBytes = BinaryHelper.toByteString(filter, serializer);
        TestStreamObserver<BytesValue> observer    = new TestStreamObserver<>();
        service.values(Requests.values(sScope, sCacheName, serializerName, filterBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(colExpected.size());

        List<Person> oResult = observer.values()
                .stream()
                .map(bv -> (Person) BinaryHelper.fromBytesValue(bv, serializer))
                .collect(Collectors.toList());

        assertThat(oResult, containsInAnyOrder(colExpected.toArray()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallValuesWithFilterWhenAllEntriesMatch(String serializerName, Serializer serializer, String sScope)
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

        NamedCacheService service     = createService();
        Filter<Person>     filter      = Filters.always();
        Collection<Person> colExpected = cache.values(filter);

        ByteString                     filterBytes = BinaryHelper.toByteString(filter, serializer);
        TestStreamObserver<BytesValue> observer    = new TestStreamObserver<>();
        service.values(Requests.values(sScope, sCacheName, serializerName, filterBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(colExpected.size());

        List<Person> oResult = observer.values()
                .stream()
                .map(bv -> (Person) BinaryHelper.fromBytesValue(bv, serializer))
                .collect(Collectors.toList());

        assertThat(oResult, containsInAnyOrder(colExpected.toArray()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    public void shouldCallValuesWithFilterWhenNoEntriesMatch(String serializerName, Serializer serializer, String sScope)
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

        NamedCacheService service     = createService();
        Filter<Person>                 filter      = new EqualsFilter<>("getAge", 100);
        Collection<Person>             colExpected = cache.values(filter);
        ByteString                     filterBytes = BinaryHelper.toByteString(filter, serializer);
        TestStreamObserver<BytesValue> observer    = new TestStreamObserver<>();
        service.values(Requests.values(sScope, sCacheName, serializerName, filterBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(colExpected.size());

        List<Person> oResult = observer.values()
                .stream()
                .map(bv -> (Person) BinaryHelper.fromBytesValue(bv, serializer))
                .collect(Collectors.toList());

        assertThat(oResult, containsInAnyOrder(colExpected.toArray()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    public void shouldCallValuesWithFilterAndComparatorWhenSomeEntriesMatch(String     serializerName,
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

        NamedCacheService              service         = createService();
        Filter<Integer>                filter          = Filters.greater(Extractors.identity(), 50);
        Comparator<Integer>            comparator      = IdentityExtractor.INSTANCE();
        List<Integer>                  expected        = new ArrayList<>(cache.values(filter, comparator));
        ByteString                     filterBytes     = BinaryHelper.toByteString(filter, serializer);
        ByteString                     comparatorBytes = BinaryHelper.toByteString(comparator, serializer);
        TestStreamObserver<BytesValue> observer        = new TestStreamObserver<>();

        service.values(Requests.values(sScope, sCacheName, serializerName, filterBytes, comparatorBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(expected.size());

        List<Integer> listResult = observer.values()
                .stream()
                .map(bv -> (Integer) BinaryHelper.fromBytesValue(bv, serializer))
                .collect(Collectors.toList());

        expected.sort(Comparator.naturalOrder());
        listResult.sort(Comparator.naturalOrder());
        assertThat(listResult.size(), is(expected.size()));
        assertThat(listResult, is(expected));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    public void shouldCallValuesWithFilterAndComparatorWhenAllEntriesMatch(String     serializerName,
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

        NamedCacheService service         = createService();
        Filter<Integer>                filter          = Filters.always();
        Comparator<Integer>            comparator      = IdentityExtractor.INSTANCE();
        Collection<Integer>            expected        = cache.values(filter, comparator);
        ByteString                     filterBytes     = BinaryHelper.toByteString(filter, serializer);
        ByteString                     comparatorBytes = BinaryHelper.toByteString(comparator, serializer);
        TestStreamObserver<BytesValue> observer        = new TestStreamObserver<>();
        service.values(Requests.values(sScope, sCacheName, serializerName, filterBytes, comparatorBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(expected.size());

        List<Integer> oResult = observer.values()
                .stream()
                .map(bv -> (Integer) BinaryHelper.fromBytesValue(bv, serializer))
                .collect(Collectors.toList());

        assertThat(oResult, containsInAnyOrder(expected.toArray()));
        }

    @ParameterizedTest(name = "{index} serializer={0} scope={2}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    public void shouldCallValuesWithFilterAndComparatorWhenNoEntriesMatch(String     serializerName,
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

        NamedCacheService service         = createService();
        Filter<Integer>                filter          = Filters.greater(Extractors.identity(), 500);
        Comparator<Integer>            comparator      = IdentityExtractor.INSTANCE();
        Collection<Integer>            expected        = cache.values(filter, comparator);
        ByteString                     filterBytes     = BinaryHelper.toByteString(filter, serializer);
        ByteString                     comparatorBytes = BinaryHelper.toByteString(comparator, serializer);
        TestStreamObserver<BytesValue> observer        = new TestStreamObserver<>();
        service.values(Requests.values(sScope, sCacheName, serializerName, filterBytes, comparatorBytes), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(expected.size());

        List<Integer> oResult = observer.values()
                .stream()
                .map(bv -> (Integer) BinaryHelper.fromBytesValue(bv, serializer))
                .collect(Collectors.toList());

        assertThat(oResult, containsInAnyOrder(expected.toArray()));
        }
    }
