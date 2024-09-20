/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package grpc.client;

import com.oracle.bedrock.junit.CoherenceClusterExtension;
import com.oracle.bedrock.junit.SessionBuilders;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.Ports;
import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;
import com.tangosol.io.Serializer;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.aggregator.Count;
import com.tangosol.util.extractor.UniversalExtractor;
import com.tangosol.util.processor.ExtractorProcessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsMapContaining.hasEntry;

public class DeferKeyAssociationCheckGrpcIT
        extends AbstractGrpcClientIT
    {
    @BeforeAll
    static void setupCluster() throws Exception
        {
        System.setProperty("coherence.extend.defer.key.association", "true");
        System.setProperty("coherence.grpc.defer.key.association", "true");

        CoherenceCluster cluster = CLUSTER_EXTENSION.getCluster();

        for (CoherenceClusterMember member : cluster)
            {
            Eventually.assertDeferred(() -> member.invoke(IsGrpcProxyRunning.INSTANCE), is(true));
            }

        CoherenceClusterMember member      = cluster.getAny();
        int                    nGrpcPort   = member.getGrpcProxyPort();
        int                    nExtendPort = member.getExtendProxyPort();

        System.setProperty("coherence.pof.config", "test-pof-config.xml");

        CoherenceConfiguration.Builder cfgBuilder = CoherenceConfiguration.builder();

        Set<String> setSessionName = new HashSet<>();
        Set<String> setSerializer  = serializers().map(a -> String.valueOf(a.get()[0]))
                .collect(Collectors.toSet());

        for (String sSerializer : setSerializer)
            {
            String sName = sessionNameFromSerializerName(sSerializer);
            SessionConfiguration cfg = SessionConfiguration.builder()
                    .named(sName)
                    .withScopeName(sName)
                    .withMode(Coherence.Mode.GrpcFixed)
                    .withParameter("coherence.serializer", sSerializer)
                    .withParameter("coherence.profile", "thin")
                    .withParameter("coherence.grpc.address", "127.0.0.1")
                    .withParameter("coherence.grpc.port", nGrpcPort)
                    .withParameter("coherence.extend.port", nExtendPort)
                    .build();

            setSessionName.add(sName);
            cfgBuilder.withSession(cfg);
            }

        Coherence coherence = Coherence.client(cfgBuilder.build()).start().get(5, TimeUnit.MINUTES);

        for (String sName : setSessionName)
            {
            Session session = coherence.getSession(sName);
            SESSIONS.put(sName, session);
            }

        s_ccfExtend = CLUSTER_EXTENSION.createSession(SessionBuilders.extendClient("client-cache-config.xml",
                SystemProperty.of("coherence.extend.defer.key.association", true),
                SystemProperty.of("coherence.extend.port", nExtendPort)));
        }

    @AfterAll
    static void shutdownCoherence()
        {
        Coherence.closeAll();
        }

    @BeforeEach
    public void logStart(TestInfo info)
        {
        String sClass  = info.getTestClass().map(Class::toString).orElse("");
        String sMethod = info.getTestMethod().map(Method::toString).orElse("");
        String sMsg = ">>>>>>> Starting test " + sClass + "." + sMethod + " - " + info.getDisplayName();
        for (CoherenceClusterMember member : CLUSTER_EXTENSION.getCluster())
            {
            member.submit(() ->
                {
                System.err.println(sMsg);
                System.err.flush();
                return null;
                }).join();
            }
        }

    @AfterEach
    public void logEnd(TestInfo info)
        {
        String sClass  = info.getTestClass().map(Class::toString).orElse("");
        String sMethod = info.getTestMethod().map(Method::toString).orElse("");
        String sMsg = ">>>>>>> Finished test " + sClass + "." + sMethod + " - " + info.getDisplayName();
        for (CoherenceClusterMember member : CLUSTER_EXTENSION.getCluster())
            {
            member.submit(() ->
                {
                System.err.println(sMsg);
                System.err.flush();
                return null;
                }).join();
            }
        }


    @Override
    protected <K, V> NamedCache<K, V> createClient(String sCacheName, String sSerializerName, Serializer serializer)
        {
        String sName = sessionNameFromSerializerName(sSerializerName);
        Session session = SESSIONS.get(sName);
        assertThat(session, is(notNullValue()));
        return session.getCache(sCacheName);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldReturnTrueForContainsPartitionedKeyWithExistingMapping(String sSerializerName, Serializer serializer)
        {
        String                               cacheName  = createCacheName();
        NamedCache<TestPartitionKey, String> cache      = ensureCache(cacheName);
        NamedCache<TestPartitionKey, String> grpcClient = createClient(cacheName, sSerializerName, serializer);

        assumeDeferKeyAssociation(cache);
        assumeDeferKeyAssociation(grpcClient);

        clearAndPopulatePartition(cache, 19, 5);

        TestPartitionKey key    = new TestPartitionKey("key-2", 19);
        boolean          result = grpcClient.containsKey(key);

        assertThat(result, is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetExistingPartitionedKey(String sSerializerName, Serializer serializer)
        {
        String                               cacheName  = createCacheName();
        NamedCache<TestPartitionKey, String> cache      = ensureCache(cacheName);
        NamedCache<TestPartitionKey, String> grpcClient = createClient(cacheName, sSerializerName, serializer);

        assumeDeferKeyAssociation(cache);
        assumeDeferKeyAssociation(grpcClient);

        clearAndPopulatePartition(cache, 19, 5);

        TestPartitionKey key    = new TestPartitionKey("key-2", 19);
        String           result = grpcClient.get(key);
        assertThat(result, is("value-2"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldGetAllWhenAllPartitionedKeysMatch(String sSerializerName, Serializer serializer)
        {
        String                               cacheName  = createCacheName();
        NamedCache<TestPartitionKey, String> cache      = ensureCache(cacheName);
        NamedCache<TestPartitionKey, String> grpcClient = createClient(cache.getCacheName(), sSerializerName, serializer);

        assumeDeferKeyAssociation(cache);
        assumeDeferKeyAssociation(grpcClient);

        clearAndPopulatePartition(cache, 19, 4);

        Collection<TestPartitionKey> keys = Arrays.asList(
                new TestPartitionKey("key-2", 19),
                new TestPartitionKey("key-4", 19));

        assertGetAll(cache, grpcClient, keys);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldInsertNewPartitionedEntry(String sSerializerName, Serializer serializer)
        {
        String                               cacheName  = createCacheName();
        NamedCache<TestPartitionKey, String> cache      = ensureCache(cacheName);
        NamedCache<TestPartitionKey, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        int                                  nPart      = 19;

        assumeDeferKeyAssociation(cache);
        assumeDeferKeyAssociation(grpcClient);

        cache.clear();

        TestPartitionKey key    = new TestPartitionKey("key-1", nPart);
        String           result = grpcClient.put(key, "value-1");
        assertThat(result, is(nullValue()));

        assertThat(cache.get(key), is("value-1"));
        Integer nActual = cache.invoke(key, new GetEntryPartition<>());
        assertThat(nActual, is(notNullValue()));
        assertThat(nActual, is(nPart));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldPutIfAbsentForExistingPartitionedKey(String sSerializerName, Serializer serializer)
        {
        String                               cacheName  = createCacheName();
        NamedCache<TestPartitionKey, String> cache      = ensureCache(cacheName);
        NamedCache<TestPartitionKey, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        TestPartitionKey                     key        = new TestPartitionKey("key-1", 19);

        assumeDeferKeyAssociation(cache);
        assumeDeferKeyAssociation(grpcClient);

        cache.clear();
        cache.put(key, "value-1");

        String result = grpcClient.putIfAbsent(key, "value-2");
        assertThat(result, is("value-1"));
        assertThat(cache.get(key), is("value-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldPutAllPartitionedEntries(String sSerializerName, Serializer serializer)
        {
        String                               cacheName  = createCacheName();
        NamedCache<TestPartitionKey, String> cache      = ensureCache(cacheName);
        NamedCache<TestPartitionKey, String> grpcClient = createClient(cacheName, sSerializerName, serializer);

        assumeDeferKeyAssociation(cache);
        assumeDeferKeyAssociation(grpcClient);

        cache.clear();

        Map<TestPartitionKey, String> map = new HashMap<>();
        TestPartitionKey              key1 = new TestPartitionKey("key-1", 19);
        TestPartitionKey              key2 = new TestPartitionKey("key-2", 19);
        map.put(key1, "value-1");
        map.put(key2, "value-2");


        grpcClient.putAll(map);

        assertThat(cache.size(), is(2));
        assertThat(cache.get(key1), is("value-1"));
        assertThat(cache.get(key2), is("value-2"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldReturnPreviousPartitionedValueForRemoveOnExistingMapping(String sSerializerName, Serializer serializer)
        {
        String                               cacheName  = createCacheName();
        NamedCache<TestPartitionKey, String> cache      = ensureCache(cacheName);
        NamedCache<TestPartitionKey, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        int                                  count      = 10;

        assumeDeferKeyAssociation(cache);
        assumeDeferKeyAssociation(grpcClient);

        clearAndPopulatePartition(cache, 19, count);

        TestPartitionKey key    = new TestPartitionKey("key-1", 19);
        String           result = grpcClient.remove(key);

        assertThat(result, is("value-1"));
        assertThat(cache.get(key), is(nullValue()));
        assertThat(cache.size(), is(count - 1));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldReturnTrueForRemoveMappingOnMatchingMappingWithPartitionedKey(String sSerializerName, Serializer serializer)
        {
        String                               cacheName  = createCacheName();
        NamedCache<TestPartitionKey, String> cache      = ensureCache(cacheName);
        NamedCache<TestPartitionKey, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        TestPartitionKey                     key        = new TestPartitionKey("key-1", 19);

        assumeDeferKeyAssociation(cache);
        assumeDeferKeyAssociation(grpcClient);

        cache.clear();
        cache.put(key, "value-1");

        boolean result = grpcClient.remove(key, "value-1");
        assertThat(result, is(true));
        assertThat(cache.containsKey(key), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldReturnNonNullForReplaceOnExistentMappingWithPartitionedKey(String sSerializerName, Serializer serializer)
        {
        String                               cacheName  = createCacheName();
        NamedCache<TestPartitionKey, String> cache      = ensureCache(cacheName);
        NamedCache<TestPartitionKey, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        TestPartitionKey                     key        = new TestPartitionKey("key-1", 19);

        assumeDeferKeyAssociation(cache);
        assumeDeferKeyAssociation(grpcClient);

        clearAndPopulatePartition(cache, 19, 5);

        String result = grpcClient.replace(key, "value-123");
        assertThat(result, is("value-1"));
        assertThat(cache.get(key), is("value-123"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldReplaceAllWithPartitionedKeySet(String sSerializerName, Serializer serializer)
        {
        String                               cacheName  = createCacheName();
        NamedCache<TestPartitionKey, String> cache      = ensureCache(cacheName);
        NamedCache<TestPartitionKey, String> grpcClient = createClient(cacheName, sSerializerName, serializer);

        assumeDeferKeyAssociation(cache);
        assumeDeferKeyAssociation(grpcClient);

        clearAndPopulatePartition(cache, 19, 5);

        List<TestPartitionKey> keys           = new ArrayList<>(cache.keySet());
        Object[]               expectedValues = new ArrayList<>(cache.values()).stream().map(v -> v + '1').toArray();


        grpcClient.replaceAll(keys, (k, v) ->
           {
           v = v + "1";
           return v;
           });

        Collection<String> newValues = cache.values();

        assertThat(newValues, containsInAnyOrder(expectedValues));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldComputeAndUpdateEntryWithPartitionedKey(String sSerializerName, Serializer serializer)
        {
        String                                cacheName  = createCacheName();
        NamedCache<TestPartitionKey, Integer> cache      = ensureCache(cacheName);
        NamedCache<TestPartitionKey, Integer> grpcClient = createClient(cacheName, sSerializerName, serializer);
        TestPartitionKey                      key1       = new TestPartitionKey("k1", 19);
        TestPartitionKey                      key2       = new TestPartitionKey("k2", 19);

        assumeDeferKeyAssociation(cache);
        assumeDeferKeyAssociation(grpcClient);

        cache.clear();
        cache.put(key1, 1);
        cache.put(key2, 2);


        //noinspection ConstantConditions
        int newValue = grpcClient.compute(key1, (k, v) -> v + v);
        assertThat(newValue, is(2));

        grpcClient.compute(key2, (k, v) -> null);

        assertThat(cache.get(key2), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldCallInvokeWithPartitionedKey(String sSerializerName, Serializer serializer)
        {
        String                               cacheName  = createCacheName();
        NamedCache<TestPartitionKey, Person> cache      = ensureCache(cacheName);
        TestPartitionKey                     key        = new TestPartitionKey("bb", 19);
        NamedCache<TestPartitionKey, Person> grpcClient = createClient(cacheName, sSerializerName, serializer);

        assumeDeferKeyAssociation(cache);
        assumeDeferKeyAssociation(grpcClient);

        cache.clear();

        Person person = new Person("bob", "builder", 25, "male");
        cache.put(key, person);

        ValueExtractor<Person, String>                                extractor = new UniversalExtractor<>("lastName");
        InvocableMap.EntryProcessor<TestPartitionKey, Person, String> processor = new ExtractorProcessor<>(extractor);


        String lastName = grpcClient.invoke(key, processor);

        assertThat(lastName, is(notNullValue()));
        assertThat(lastName, is("builder"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldCallInvokeAllWithPartitionedKeys(String sSerializerName, Serializer serializer)
        {
        String                               cacheName  = createCacheName("people");
        NamedCache<TestPartitionKey, Person> cache      = ensureCache(cacheName);
        NamedCache<TestPartitionKey, Person> grpcClient = createClient(cacheName, sSerializerName, serializer);

        assumeDeferKeyAssociation(cache);
        assumeDeferKeyAssociation(grpcClient);

        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 25, "male");

        TestPartitionKey key1 = new TestPartitionKey(person1.getLastName(), 19);
        TestPartitionKey key2 = new TestPartitionKey(person2.getLastName(), 19);
        TestPartitionKey key3 = new TestPartitionKey(person3.getLastName(), 19);
        cache.put(key1, person1);
        cache.put(key2, person2);
        cache.put(key3, person3);

        ValueExtractor<Person, String>                                extractor = new UniversalExtractor<>("firstName");
        InvocableMap.EntryProcessor<TestPartitionKey, Person, String> processor = new ExtractorProcessor<>(extractor);
        List<TestPartitionKey>                                        keys      = Arrays.asList(key1, key2);


        Map<TestPartitionKey, String> map = grpcClient.invokeAll(keys, processor);

        assertThat(map, hasEntry(key1, person1.getFirstName()));
        assertThat(map, hasEntry(key2, person2.getFirstName()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldCallAggregateWithPartitionedKeysExpectingSingleResult(String sSerializerName, Serializer serializer)
        {
        String                               cacheName = createCacheName("people");
        NamedCache<TestPartitionKey, Person> cache     = ensureCache(cacheName);
        NamedCache<TestPartitionKey, Person> grpcClient = createClient(cacheName, sSerializerName, serializer);

        assumeDeferKeyAssociation(cache);
        assumeDeferKeyAssociation(grpcClient);

        cache.clear();
        Person person1 = new Person("Arthur", "Dent", 25, "male");
        Person person2 = new Person("Dirk", "Gently", 25, "male");
        Person person3 = new Person("Ford", "Prefect", 35, "male");

        TestPartitionKey key1 = new TestPartitionKey(person1.getLastName(), 19);
        TestPartitionKey key2 = new TestPartitionKey(person2.getLastName(), 19);
        TestPartitionKey key3 = new TestPartitionKey(person3.getLastName(), 19);
        cache.put(key1, person1);
        cache.put(key2, person2);
        cache.put(key3, person3);

        InvocableMap.EntryAggregator<TestPartitionKey, Person, Integer> aggregator = new Count<>();
        List<TestPartitionKey>                                          keys       = Arrays.asList(key1, key2);

        int expected = cache.aggregate(keys, aggregator);
        int result   = grpcClient.aggregate(keys, aggregator);

        assertThat(result, is(expected));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldSubscribeToEventsForSinglePartitionedKey(String sSerializerName, Serializer serializer) throws Exception
        {
        int              nPart = 19;
        TestPartitionKey key   = new TestPartitionKey("key-2", nPart);

        assertSubscribeToEventsForSingleKey(key, cache -> generateCacheEventsForPartitionedKey(cache, nPart),
                sSerializerName, serializer);
        }

    protected static String sessionNameFromSerializerName(String sSerializerName)
        {
        return sSerializerName.isEmpty() ? "default" : sSerializerName;
        }

    @Override
    protected <K, V> NamedCache<K, V> ensureCache(String sName, ClassLoader loader)
        {
        return s_ccfExtend.ensureCache(sName, loader);
        }


    // ----- data members ---------------------------------------------------

    static ConfigurableCacheFactory s_ccfExtend;

    static final Map<String, Session> SESSIONS = new HashMap<>();

    static final String CLUSTER_NAME = "DefaultCacheConfigGrpcIT";

    static final LocalPlatform PLATFORM = LocalPlatform.get();

    static final Iterator<Integer> PORTS = PLATFORM.getAvailablePorts();

    static final int CLUSTER_SIZE = 3;

    @RegisterExtension
    static final TestLogsExtension TEST_LOGS = new TestLogsExtension(DeferKeyAssociationCheckGrpcIT.class);

    @RegisterExtension
    static final CoherenceClusterExtension CLUSTER_EXTENSION = new CoherenceClusterExtension()
            .with(CacheConfig.of("coherence-config.xml"),
                  OperationalOverride.of("test-coherence-override.xml"),
                  Pof.config("test-pof-config.xml"),
                  SystemProperty.of("coherence.serializer", "pof"),
                  SystemProperty.of("coherence.extend.port", PORTS, Ports.capture()),
                  WellKnownAddress.loopback(),
                  ClusterName.of(CLUSTER_NAME),
                  DisplayName.of("storage"),
                  RoleName.of("storage"),
                  Logging.atMax(),
                  LocalHost.only(),
                  IPv4Preferred.yes(),
                  StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning()),
                  TEST_LOGS)
            .include(CLUSTER_SIZE, CoherenceClusterMember.class);
    }
