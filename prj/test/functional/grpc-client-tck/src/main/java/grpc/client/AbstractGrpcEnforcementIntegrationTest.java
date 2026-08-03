/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.client;

import com.google.protobuf.BytesValue;

import com.oracle.coherence.grpc.BinaryHelper;

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
import com.tangosol.util.aggregator.ScriptAggregator;
import com.tangosol.util.extractor.ScriptValueExtractor;
import com.tangosol.util.filter.ScriptFilter;
import com.tangosol.util.function.Remote;

import io.grpc.Channel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import java.lang.reflect.Field;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Shared cache data-plane executable policy coverage for gRPC v0 and v1.
 *
 * @author Aleks Seovic  2026.05.12
 * @since 26.07
 */
@SuppressWarnings({"rawtypes", "unchecked"})
abstract class AbstractGrpcEnforcementIntegrationTest
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
    void removeIndexAllowsAnnotatedExtractor()
        {
        NamedCache<String, Integer> cache = client();

        cache.removeIndex(new AnnotatedExtractor());

        assertCounter(OperationReason.EXTRACT, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
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
    void aggregateAllAllowsAnnotatedAggregator()
        {
        NamedCache<String, Integer> cache = clientWithData();

        assertEquals(Integer.valueOf(3), cache.aggregate(new AnnotatedAggregator()));

        assertCounter(OperationReason.AGGREGATE, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    void aggregateAllRejectsPlainAggregator()
        {
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> clientWithData().aggregate(new PlainAggregator()));

        assertContains(e, "Remote execution denied");
        assertCounter(OperationReason.AGGREGATE, "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    void aggregateFilterAllowsAnnotatedFilterAndAggregator()
        {
        NamedCache<String, Integer> cache = clientWithData();

        assertEquals(Integer.valueOf(3), cache.aggregate(new AnnotatedFilter(), new AnnotatedAggregator()));

        assertCounter(OperationReason.EVALUATE_FILTER, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounter(OperationReason.AGGREGATE, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
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
        Set<Map.Entry<String, Integer>> entries = clientWithData().entrySet(new AnnotatedFilter(),
                new AnnotatedComparator());

        assertEquals(3, entries.size());
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
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> clientWithData().entrySet(new AnnotatedFilter(), new PlainComparator()));

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
    void invokeAllKeysAllowsAnnotatedProcessor()
        {
        Map<String, Integer> map = clientWithData().invokeAll(Arrays.asList("one", "two"), new AnnotatedProcessor());

        assertEquals(2, map.size());
        assertCounter(OperationReason.PROCESS_ENTRY, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
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
    void invokeAllFilterAllowsAnnotatedFilterAndProcessor()
        {
        Map<String, Integer> map = clientWithData().invokeAll(new AnnotatedFilter(), new AnnotatedProcessor());

        assertEquals(3, map.size());
        assertCounter(OperationReason.EVALUATE_FILTER, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounter(OperationReason.PROCESS_ENTRY, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
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
    void keySetAllowsAnnotatedFilter()
        {
        assertEquals(3, clientWithData().keySet(new AnnotatedFilter()).size());

        assertCounter(OperationReason.EVALUATE_FILTER, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
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
    void scriptBackedCacheExecutablesRejectInProd()
        {
        RuntimeException eFilter = assertThrows(RuntimeException.class,
                () -> clientWithData().keySet(new ScriptFilter<>("js", "ValuePresentFilter")));

        assertContains(eFilter, "script-eval-denied-by-mode");
        assertCounter(OperationReason.SCRIPT_EVAL, SerializationRole.GRPC, "rejected",
                SerializationTelemetry.SUB_REASON_MODE_GATE, 1L);

        SerializationTelemetry.resetForTesting();
        RuntimeException eExtractor = assertThrows(RuntimeException.class,
                () -> clientWithData().addIndex(new ScriptValueExtractor<>("js", "IdentityExtractor"), false, null));

        assertContains(eExtractor, "script-eval-denied-by-mode");
        assertCounter(OperationReason.SCRIPT_EVAL, SerializationRole.GRPC, "rejected",
                SerializationTelemetry.SUB_REASON_MODE_GATE, 1L);

        SerializationTelemetry.resetForTesting();
        RuntimeException eAggregator = assertThrows(RuntimeException.class,
                () -> clientWithData().aggregate(Arrays.asList("one", "two"),
                        new ScriptAggregator<>("js", "CountAggregator", 0)));

        assertContains(eAggregator, "script-eval-denied-by-mode");
        assertCounter(OperationReason.SCRIPT_EVAL, SerializationRole.UNCLASSIFIED, "rejected",
                SerializationTelemetry.SUB_REASON_MODE_GATE, 1L);
        }

    @Test
    void valuesAllowsAnnotatedFilterAndComparator()
        {
        NamedCache<String, Integer> cache  = clientWithData();
        Collection<Integer>         values = values(cache, new AnnotatedFilter(), new AnnotatedComparator());

        assertEquals(3, values.size());
        assertCounter(OperationReason.EVALUATE_FILTER, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounter(OperationReason.COMPARE, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    void valuesRejectsPlainFilter()
        {
        NamedCache<String, Integer> cache = clientWithData();

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> values(cache, new PlainFilter(), new AnnotatedComparator()));

        assertContains(e, "Remote execution denied");
        assertCounter(OperationReason.EVALUATE_FILTER, "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    void valuesRejectsPlainComparator()
        {
        NamedCache<String, Integer> cache = clientWithData();

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> values(cache, new AnnotatedFilter(), new PlainComparator()));

        assertContains(e, "Remote execution denied");
        assertCounter(OperationReason.EVALUATE_FILTER, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounter(OperationReason.COMPARE, "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    protected abstract ServerHelper serverHelper();

    protected Collection<Integer> values(NamedCache<String, Integer> cache, Filter<Integer> filter,
            Comparator<Object> comparator)
        {
        return cache.values(filter, comparator);
        }

    protected Channel serverChannel()
        {
        try
            {
            Field field = ServerHelper.class.getDeclaredField("m_channel");
            field.setAccessible(true);
            return (Channel) field.get(serverHelper());
            }
        catch (ReflectiveOperationException e)
            {
            throw new IllegalStateException(e);
            }
        }

    protected static <T> T fromBytesValue(BytesValue value)
        {
        return BinaryHelper.fromBytesValue(value, SERIALIZER);
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
        return serverHelper().createClient(GrpcDependencies.DEFAULT_SCOPE,
                "grpc-cache-enforcement-" + COUNTER.incrementAndGet(), "", SERIALIZER);
        }

    private void assertCounter(OperationReason reason, String sResult, String sSubReason, long cExpected)
        {
        assertCounter(reason, SerializationRole.GRPC, sResult, sSubReason, cExpected);
        }

    private void assertCounter(OperationReason reason, SerializationRole role, String sResult, String sSubReason,
                               long cExpected)
        {
        String sKey = "coh.executable.policy_check{reason=" + reason.name()
                + ",role=" + role.name()
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

    // ----- fixtures ------------------------------------------------------

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
    }
