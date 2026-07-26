/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.Entry;
import com.oracle.coherence.grpc.EntrySetRequest;
import com.oracle.coherence.grpc.NamedCacheServiceGrpc;
import com.oracle.coherence.grpc.Requests;
import com.oracle.coherence.grpc.ValuesRequest;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;

import com.tangosol.net.NamedCache;

import com.tangosol.net.grpc.GrpcDependencies;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.OperationReason;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.aggregator.Count;
import com.tangosol.util.function.Remote;

import io.grpc.Channel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.Serializable;

import java.lang.reflect.Field;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * gRPC NamedCache executable install gate integration tests.
 *
 * @author Aleks Seovic  2026.05.14
 * @since 14.1.1.2206.17
 */
@SuppressWarnings({"rawtypes", "unchecked"})
class GrpcNamedCacheInstallGateIntegrationTest
    {
    @BeforeEach
    void resetTelemetry()
        {
        SerializationTelemetry.resetForTesting();
        }

    @Test
    void addIndexAllowsAnnotatedExtractorAndComparator()
        {
        NamedCache<String, Integer> cache = client();

        cache.addIndex(new AnnotatedExtractor(), true, new AnnotatedComparator());

        assertCounter(OperationReason.EXTRACT, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounter(OperationReason.COMPARE, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    void addIndexRejectsPlainExtractor()
        {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> client().addIndex(new PlainExtractor(), false, null));

        assertContains(e, "Remote execution denied");
        assertCounter(OperationReason.EXTRACT, "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    void addIndexRejectsPlainComparator()
        {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> client().addIndex(new AnnotatedExtractor(), true, new PlainComparator()));

        assertContains(e, "Remote execution denied");
        assertCounter(OperationReason.EXTRACT, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounter(OperationReason.COMPARE, "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    void removeIndexRejectsPlainExtractor()
        {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> client().removeIndex(new PlainExtractor()));

        assertContains(e, "Remote execution denied");
        assertCounter(OperationReason.EXTRACT, "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    void aggregateKeysAllowsAnnotatedAggregator()
        {
        NamedCache<String, Integer> cache = clientWithData();

        assertEquals(Integer.valueOf(2), cache.aggregate(Arrays.asList("one", "two"), new AnnotatedAggregator()));

        assertCounter(OperationReason.AGGREGATE, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    void aggregateKeysRejectsPlainAggregator()
        {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> clientWithData().aggregate(Arrays.asList("one", "two"), new PlainAggregator()));

        assertContains(e, "Remote execution denied");
        assertCounter(OperationReason.AGGREGATE, "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    void aggregateFilterRejectsPlainFilter()
        {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> clientWithData().aggregate(new PlainFilter(), new AnnotatedAggregator()));

        assertContains(e, "Remote execution denied");
        assertCounter(OperationReason.EVALUATE_FILTER, "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    void aggregateFilterRejectsPlainAggregator()
        {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> clientWithData().aggregate(new AnnotatedFilter(), new PlainAggregator()));

        assertContains(e, "Remote execution denied");
        assertCounter(OperationReason.EVALUATE_FILTER, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounter(OperationReason.AGGREGATE, "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    void entrySetAllowsAnnotatedFilterAndComparator()
        {
        NamedCache<String, Integer> cache = clientWithData();

        assertEquals(3, entrySetSize(cache, new AnnotatedFilter(), new AnnotatedComparator()));
        assertCounter(OperationReason.EVALUATE_FILTER, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounter(OperationReason.COMPARE, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    void entrySetRejectsPlainFilter()
        {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> clientWithData().entrySet(new PlainFilter(), new AnnotatedComparator()));

        assertContains(e, "Remote execution denied");
        assertCounter(OperationReason.EVALUATE_FILTER, "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    void entrySetRejectsPlainComparator()
        {
        NamedCache<String, Integer> cache = clientWithData();

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> entrySetSize(cache, new AnnotatedFilter(), new PlainComparator()));

        assertContains(e, "Remote execution denied");
        assertCounter(OperationReason.EVALUATE_FILTER, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounter(OperationReason.COMPARE, "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    void invokeAllowsAnnotatedProcessor()
        {
        assertEquals(Integer.valueOf(1), clientWithData().invoke("one", new AnnotatedProcessor()));

        assertCounter(OperationReason.PROCESS_ENTRY, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    void invokeRejectsPlainProcessor()
        {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> clientWithData().invoke("one", new PlainProcessor()));

        assertContains(e, "Remote execution denied");
        assertCounter(OperationReason.PROCESS_ENTRY, "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    void invokeAllKeysRejectsPlainProcessor()
        {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> clientWithData().invokeAll(Arrays.asList("one", "two"), new PlainProcessor()));

        assertContains(e, "Remote execution denied");
        assertCounter(OperationReason.PROCESS_ENTRY, "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    void invokeAllFilterRejectsPlainFilter()
        {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> clientWithData().invokeAll(new PlainFilter(), new AnnotatedProcessor()));

        assertContains(e, "Remote execution denied");
        assertCounter(OperationReason.EVALUATE_FILTER, "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    void invokeAllFilterRejectsPlainProcessor()
        {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> clientWithData().invokeAll(new AnnotatedFilter(), new PlainProcessor()));

        assertContains(e, "Remote execution denied");
        assertCounter(OperationReason.EVALUATE_FILTER, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounter(OperationReason.PROCESS_ENTRY, "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    void keySetRejectsPlainFilter()
        {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> clientWithData().keySet(new PlainFilter()));

        assertContains(e, "Remote execution denied");
        assertCounter(OperationReason.EVALUATE_FILTER, "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    void valuesRejectsPlainFilter()
        {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> clientWithData().values(new PlainFilter(), new AnnotatedComparator()));

        assertContains(e, "Remote execution denied");
        assertCounter(OperationReason.EVALUATE_FILTER, "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    void valuesRejectsPlainComparator()
        {
        NamedCache<String, Integer> cache = clientWithData();

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> valuesSize(cache, new AnnotatedFilter(), new PlainComparator()));

        assertContains(e, "Remote execution denied");
        assertCounter(OperationReason.EVALUATE_FILTER, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounter(OperationReason.COMPARE, "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    private int entrySetSize(NamedCache<String, Integer> cache, Filter<Integer> filter, Comparator<Object> comparator)
        {
        ByteString      filterBytes     = BinaryHelper.toByteString(filter, SERIALIZER);
        ByteString      comparatorBytes = BinaryHelper.toByteString(comparator, SERIALIZER);
        EntrySetRequest request         = Requests.entrySet(GrpcDependencies.DEFAULT_SCOPE, cache.getCacheName(), "",
                filterBytes, comparatorBytes);
        Iterator<Entry> iterator        = NamedCacheServiceGrpc.newBlockingStub(serverChannel()).entrySet(request);
        int             cEntries        = 0;

        while (iterator.hasNext())
            {
            iterator.next();
            cEntries++;
            }
        return cEntries;
        }

    private int valuesSize(NamedCache<String, Integer> cache, Filter<Integer> filter, Comparator<Object> comparator)
        {
        ByteString           filterBytes     = BinaryHelper.toByteString(filter, SERIALIZER);
        ByteString           comparatorBytes = BinaryHelper.toByteString(comparator, SERIALIZER);
        ValuesRequest        request         = Requests.values(GrpcDependencies.DEFAULT_SCOPE, cache.getCacheName(), "",
                filterBytes, comparatorBytes);
        Iterator<BytesValue> iterator        = NamedCacheServiceGrpc.newBlockingStub(serverChannel()).values(request);
        int                  cValues         = 0;

        while (iterator.hasNext())
            {
            iterator.next();
            cValues++;
            }
        return cValues;
        }

    private Channel serverChannel()
        {
        try
            {
            Field field = ServerHelper.class.getDeclaredField("m_channel");
            field.setAccessible(true);
            return (Channel) field.get(SERVER_HELPER);
            }
        catch (ReflectiveOperationException e)
            {
            throw new IllegalStateException(e);
            }
        }

    private NamedCache<String, Integer> clientWithData()
        {
        NamedCache<String, Integer> cache = client();
        cache.clear();
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
        return cache;
        }

    private NamedCache<String, Integer> client()
        {
        return SERVER_HELPER.createClient(GrpcDependencies.DEFAULT_SCOPE,
                "grpc-cache-install-gate-" + COUNTER.incrementAndGet(), "", SERIALIZER);
        }

    private void assertCounter(OperationReason reason, String sResult, String sSubReason, long cExpected)
        {
        String sKey = "coh.executable.policy_check{reason=" + reason.name()
                + ",role=" + SerializationRole.GRPC.name()
                + ",result=" + sResult
                + ",mode=prod"
                + ",sub_reason=" + sSubReason + "}";
        assertEquals(cExpected, SerializationTelemetry.snapshot().getOrDefault(sKey, 0L),
                () -> "counter " + sKey + " in " + SerializationTelemetry.snapshot());
        }

    private static void assertContains(Throwable t, String sMessage)
        {
        while (t != null)
            {
            if (String.valueOf(t).contains(sMessage)
                    || String.valueOf(t.getMessage()).contains(sMessage))
                {
                assertThat(String.valueOf(t), containsString(sMessage));
                return;
                }
            t = t.getCause();
            }
        assertThat("exception chain", containsString(sMessage));
        }

    @Remote.Executable
    public static class AnnotatedProcessor
            implements InvocableMap.EntryProcessor<String, Integer, Integer>, Serializable
        {
        @Override
        public Integer process(InvocableMap.Entry<String, Integer> entry)
            {
            return 1;
            }
        }

    public static class PlainProcessor
            implements InvocableMap.EntryProcessor<String, Integer, Integer>, Serializable
        {
        @Override
        public Integer process(InvocableMap.Entry<String, Integer> entry)
            {
            return 1;
            }
        }

    @Remote.Executable
    public static class AnnotatedAggregator
            extends Count<String, Integer>
        {
        }

    public static class PlainAggregator
            extends Count<String, Integer>
        {
        }

    @Remote.Executable
    public static class AnnotatedFilter
            implements Filter<Integer>, Serializable
        {
        @Override
        public boolean evaluate(Integer value)
            {
            return true;
            }
        }

    public static class PlainFilter
            implements Filter<Integer>, Serializable
        {
        @Override
        public boolean evaluate(Integer value)
            {
            return true;
            }
        }

    @Remote.Executable
    public static class AnnotatedExtractor
            implements ValueExtractor<Integer, Integer>, Serializable
        {
        @Override
        public Integer extract(Integer target)
            {
            return 1;
            }
        }

    public static class PlainExtractor
            implements ValueExtractor<Integer, Integer>, Serializable
        {
        @Override
        public Integer extract(Integer target)
            {
            return 1;
            }
        }

    @Remote.Executable
    public static class AnnotatedComparator
            implements Comparator<Object>, Serializable
        {
        @Override
        public int compare(Object o1, Object o2)
            {
            return String.valueOf(value(o1)).compareTo(String.valueOf(value(o2)));
            }
        }

    public static class PlainComparator
            implements Comparator<Object>, Serializable
        {
        @Override
        public int compare(Object o1, Object o2)
            {
            return String.valueOf(value(o1)).compareTo(String.valueOf(value(o2)));
            }
        }

    private static Object value(Object o)
        {
        return o instanceof Map.Entry ? ((Map.Entry<?, ?>) o).getValue() : o;
        }

    protected static final DefaultSerializer SERIALIZER = new DefaultSerializer();

    private static final AtomicInteger COUNTER = new AtomicInteger();

    @RegisterExtension
    static ServerHelper SERVER_HELPER = new ServerHelper()
            .setProperty("coherence.ttl", "0")
            .setProperty("coherence.wka", "127.0.0.1")
            .setProperty("coherence.localhost", "127.0.0.1")
            .setProperty("coherence.cluster", "GrpcNamedCacheInstallGateIntegrationTest-" + System.nanoTime())
            .setProperty("coherence.override", "coherence-json-override.xml")
            .setProperty("coherence.mode", "prod")
            .setProperty("coherence.cacheconfig", "coherence-config.xml");
    }
