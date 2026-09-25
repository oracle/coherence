/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

import com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory;
import com.tangosol.coherence.component.net.extend.protocol.NamedCacheProtocol;
import com.tangosol.coherence.component.net.MemberSet;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;

import com.tangosol.internal.net.ContinuousAggregationDefinition;
import com.tangosol.internal.net.ContinuousAggregationProcessor;
import com.tangosol.internal.net.ContinuousAggregationQuery;
import com.tangosol.internal.net.ContinuousAggregationSupport;
import com.tangosol.internal.net.SessionNamedCache;

import com.tangosol.io.DefaultSerializer;

import com.tangosol.net.cache.WrapperNamedCache;

import com.tangosol.net.cache.TypeAssertion;

import com.tangosol.util.SimpleMapEntry;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;

import com.tangosol.util.aggregator.Count;
import com.tangosol.util.aggregator.LongSum;
import com.tangosol.util.aggregator.PriorityAggregator;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.KeyAssociatedFilter;

import com.tangosol.util.function.Remote;

import com.tangosol.net.partition.KeyPartitioningStrategy;
import com.tangosol.net.partition.PartitionSet;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Continuous Aggregation client API.
 *
 * @author Aleks Seovic  2026.09.06
 * @since 26.10
 */
public class ContinuousAggregationApiTest
    {
    @Test
    public void shouldEnforceCommercialAndCommunityEditionVersionBoundaries()
        {
        assertFalse(ContinuousAggregationSupport.isVersionCompatible(
                "26.0.0.0.0", false));
        assertTrue(ContinuousAggregationSupport.isVersionCompatible(
                "26.1.0.0.0", false));
        assertTrue(ContinuousAggregationSupport.isVersionCompatible(
                "26.1.0-0-0-SNAPSHOT", false));

        assertFalse(ContinuousAggregationSupport.isVersionCompatible(
                "26.07", true));
        assertTrue(ContinuousAggregationSupport.isVersionCompatible(
                "26.10", true));
        assertTrue(ContinuousAggregationSupport.isVersionCompatible(
                "27.03", true));
        assertFalse(ContinuousAggregationSupport.isVersionCompatible(
                "not-a-version", true));

        assertEquals("26.1.0.0.0 or CE 26.10",
                ContinuousAggregationSupport.getMinimumVersionDescription());
        }

    @Test
    public void shouldRecognizeContinuousAggregationMemberCapability()
        {
        assertFalse(ContinuousAggregationSupport.isMemberCompatible(Map.of()));
        assertFalse(ContinuousAggregationSupport.isMemberCompatible(Map.of(
                ContinuousAggregationSupport.MEMBER_CONFIG_KEY, 0)));
        assertTrue(ContinuousAggregationSupport.isMemberCompatible(Map.of(
                ContinuousAggregationSupport.MEMBER_CONFIG_KEY,
                ContinuousAggregationSupport.MEMBER_CONFIG_VERSION)));
        }

    @Test
    public void shouldGateContinuousAggregationOnStorageMemberCompatibility()
        {
        TestPartitionedCache service = new TestPartitionedCache();
        service.m_fCompatible = false;

        UnsupportedOperationException error = assertThrows(
                UnsupportedOperationException.class,
                service::ensureContinuousAggregationCompatible);
        assertTrue(error.getMessage().contains("all storage-enabled members"));

        service.m_fCompatible = true;
        service.ensureContinuousAggregationCompatible();
        }

    @Test
    public void shouldReserveExtendProtocolVersionAndMessageType()
        {
        assertEquals(13, new NamedCacheProtocol().getCurrentVersion());
        assertEquals(62,
                new NamedCacheFactory.ContinuousAggregationRequest().getTypeId());
        }

    private static class TestPartitionedCache
            extends PartitionedCache
        {
        TestPartitionedCache()
            {
            super("ContinuousAggregationApiTest", null, true);
            }

        @Override
        public boolean isContinuousAggregationCompatible(MemberSet members)
            {
            return m_fCompatible;
            }

        private boolean m_fCompatible;
        }

    @Test
    public void shouldRegisterAndAggregateUsingHandle()
        {
        SupportingNamedMap<Integer, Integer> map = new SupportingNamedMap<>("numbers");
        map.m_result = 42;

        ContinuousAggregator<Integer, Integer, Integer> handle = map.addAggregator(new Count<>());

        assertEquals(1, map.m_cRegistrations);
        assertEquals(AlwaysFilter.INSTANCE(), map.m_definition.getFilter());
        assertEquals(new Count<>(), map.m_definition.getAggregator());
        assertEquals(Integer.valueOf(42), handle.aggregate());
        assertEquals(map.m_definition, map.m_aggregateDefinition);
        }

    @Test
    public void shouldDeclareContinuousAggregationOnInvocableMap()
        {
        assertEquals(3L, Arrays.stream(InvocableMap.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("addAggregator")
                        || method.getName().equals("removeAggregator"))
                .count());
        assertEquals(0L, Arrays.stream(NamedMap.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("addAggregator")
                        || method.getName().equals("removeAggregator"))
                .count());
        }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void shouldApplyProjectionToFinalizedPartitionResultOncePerPartition()
        {
        BackingMapContext       context  = mock(BackingMapContext.class);
        BackingMapManagerContext manager = mock(BackingMapManagerContext.class);
        DistributedCacheService service  = mock(DistributedCacheService.class);
        KeyPartitioningStrategy strategy = mock(KeyPartitioningStrategy.class);
        PartitionSet            parts    = new PartitionSet(17);
        parts.add(3);

        when(manager.getCacheService()).thenReturn(service);
        when(service.getKeyPartitioningStrategy()).thenReturn(strategy);
        when(strategy.getAssociatedPartitions(any())).thenReturn(parts);
        when(context.getContinuousAggregationResult(eq(3), any(), any())).thenReturn(4);

        BinaryEntry<Integer, Integer> entryOne = mockPartitionEntry(1, 3, context, manager);
        BinaryEntry<Integer, Integer> entryTwo = mockPartitionEntry(2, 3, context, manager);
        ContinuousAggregationDefinition definition =
                new ContinuousAggregationDefinition(null, new Count<>());
        ContinuousAggregationProcessor<Integer, Integer, Integer, Integer> processor =
                new ContinuousAggregationProcessor<>(definition,
                        (Remote.BiFunction<Integer, Integer, Integer>) Integer::sum);
        LinkedHashSet<InvocableMap.Entry<Integer, Integer>> entries =
                new LinkedHashSet<>(Arrays.asList(entryOne, entryTwo));

        Map<Integer, Integer> results = processor.processAll(entries);

        assertEquals(Integer.valueOf(5), results.get(1));
        assertEquals(Integer.valueOf(6), results.get(2));
        assertTrue(entries.isEmpty());
        verify(context, times(1)).getContinuousAggregationResult(eq(3), any(), any());
        }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void shouldRejectPartitionProjectionForBroadAssociation()
        {
        BackingMapContext       context  = mock(BackingMapContext.class);
        BackingMapManagerContext manager = mock(BackingMapManagerContext.class);
        DistributedCacheService service  = mock(DistributedCacheService.class);
        KeyPartitioningStrategy strategy = mock(KeyPartitioningStrategy.class);
        PartitionSet            parts    = new PartitionSet(17);
        parts.add(3);
        parts.add(4);

        when(manager.getCacheService()).thenReturn(service);
        when(service.getKeyPartitioningStrategy()).thenReturn(strategy);
        when(strategy.getAssociatedPartitions(any())).thenReturn(parts);

        BinaryEntry<Integer, Integer> entry = mockPartitionEntry(1, 3, context, manager);
        ContinuousAggregationProcessor<Integer, Integer, Integer, Integer> processor =
                new ContinuousAggregationProcessor<>(
                        new ContinuousAggregationDefinition(null, new Count<>()),
                        Remote.Function.<Integer>identity());

        assertThrows(IllegalArgumentException.class, () -> processor.process(entry));
        verify(context, never()).getContinuousAggregationResult(any(Integer.class), any(), any());
        }

    @SuppressWarnings("unchecked")
    private static <K, V> BinaryEntry<K, V> mockPartitionEntry(K key, int nPartition,
            BackingMapContext context, BackingMapManagerContext manager)
        {
        BinaryEntry<K, V> entry = mock(BinaryEntry.class);
        when(entry.asBinaryEntry()).thenReturn(entry);
        when(entry.getKey()).thenReturn(key);
        when(entry.getKeyPartition()).thenReturn(nPartition);
        when(entry.getBackingMapContext()).thenReturn(context);
        when(entry.getContext()).thenReturn(manager);
        return entry;
        }

    @Test
    public void shouldUseCompleteDefinitionAsRegistrationIdentity()
        {
        ContinuousAggregationDefinition definitionOne =
                new ContinuousAggregationDefinition(
                        new EqualsFilter<>("intValue", 10), new Count<>());
        ContinuousAggregationDefinition definitionTwo =
                new ContinuousAggregationDefinition(
                        new EqualsFilter<>("intValue", 10), new Count<>());
        ContinuousAggregationDefinition definitionThree =
                new ContinuousAggregationDefinition(
                        new EqualsFilter<>("intValue", 11), new Count<>());

        assertEquals(definitionOne, definitionTwo);
        assertEquals(definitionOne.hashCode(), definitionTwo.hashCode());
        assertFalse(definitionOne.equals(definitionThree));
        }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldPreserveKeyAssociationAsDefinitionIdentityAndQueryScope()
        {
        EqualsFilter<Integer, Integer> filter =
                new EqualsFilter<>("intValue", 10);
        ContinuousAggregationDefinition definition =
                new ContinuousAggregationDefinition(
                        new KeyAssociatedFilter<>(filter, "customer-1"),
                        new Count<>());
        ContinuousAggregationDefinition otherHost =
                new ContinuousAggregationDefinition(
                        new KeyAssociatedFilter<>(filter, "customer-2"),
                        new Count<>());

        assertTrue(definition.isKeyAssociated());
        assertEquals("customer-1", definition.getHostKey());
        assertEquals(filter, definition.getEvaluationFilter());
        assertFalse(definition.equals(otherHost));

        ContinuousAggregationQuery<Integer> query =
                new ContinuousAggregationQuery<>(definition);
        ContinuousAggregationQuery<Integer> copy =
                (ContinuousAggregationQuery<Integer>) ExternalizableHelper.fromBinary(
                        ExternalizableHelper.toBinary(query, new DefaultSerializer()),
                        new DefaultSerializer());

        assertEquals(definition, query.getDefinition());
        assertEquals(definition, copy.getDefinition());
        assertEquals(definition,
                ((ContinuousAggregationQuery<Integer>) query.supply()).getDefinition());
        }

    @Test
    public void shouldStoreAPristineAggregatorPrototype()
        {
        Count<Integer, Integer> count = new Count<>();
        count.accumulate(new SimpleMapEntry<>(1, 1));

        ContinuousAggregationDefinition definition =
                new ContinuousAggregationDefinition(null, count);

        assertEquals(Integer.valueOf(0), definition.getAggregator().getPartialResult());
        }

    @Test
    public void shouldIgnorePriorExecutionStateWhenIdentifyingDefinition()
        {
        LongSum<Integer> used = new LongSum<>("intValue");
        used.accumulate(new SimpleMapEntry<>(1, 10));
        used.getPartialResult();

        ContinuousAggregationDefinition usedDefinition =
                new ContinuousAggregationDefinition(null, used);
        ContinuousAggregationDefinition freshDefinition =
                new ContinuousAggregationDefinition(null, new LongSum<>("intValue"));

        assertEquals(freshDefinition, usedDefinition);
        }

    @Test
    public void shouldRejectUnsupportedAggregatorBeforeRegistration()
        {
        SupportingNamedMap<Integer, Integer> map = new SupportingNamedMap<>("numbers");

        try
            {
            map.addAggregator(new PriorityAggregator<>(new Count<>()));
            fail("Expected an IllegalArgumentException");
            }
        catch (IllegalArgumentException expected)
            {
            assertEquals(0, map.m_cRegistrations);
            }
        }

    @Test
    public void shouldBindHandleToCreatingMapAndRemoveIdempotently()
        {
        SupportingNamedMap<Integer, Integer> mapOne = new SupportingNamedMap<>("one");
        SupportingNamedMap<Integer, Integer> mapTwo = new SupportingNamedMap<>("two");

        ContinuousAggregator<Integer, Integer, Integer> handle = mapOne.addAggregator(new Count<>());

        try
            {
            mapTwo.removeAggregator(handle);
            fail("Expected an IllegalArgumentException");
            }
        catch (IllegalArgumentException expected)
            {
            assertEquals(0, mapOne.m_cRemovals);
            }

        mapOne.removeAggregator(handle);
        mapOne.removeAggregator(handle);
        assertEquals(1, mapOne.m_cRemovals);

        try
            {
            handle.aggregate();
            fail("Expected an IllegalStateException");
            }
        catch (IllegalStateException expected)
            {
            // expected
            }

        assertThrows(IllegalStateException.class,
                () -> handle.invoke(1, Remote.Function.<Integer>identity()));
        assertThrows(IllegalStateException.class,
                () -> handle.invokeAll(Arrays.asList(1), (key, result) -> result));
        }

    @Test
    public void shouldDelegateThroughSessionNamedCache()
        {
        SupportingNamedMap<Integer, Integer> internal = new SupportingNamedMap<>("numbers");
        internal.m_result = 42;
        SessionNamedCache<Integer, Integer> map = new SessionNamedCache<>(
                null, internal, TypeAssertion.withoutTypeChecking());

        ContinuousAggregator<Integer, Integer, Integer> handle = map.addAggregator(new Count<>());

        assertEquals(1, internal.m_cRegistrations);
        assertEquals(Integer.valueOf(42), handle.aggregate());
        map.removeAggregator(handle);
        assertEquals(1, internal.m_cRemovals);
        }

    // ----- inner class: SupportingNamedMap -------------------------------

    /**
     * A NamedMap with an in-memory service-provider implementation.
     */
    private static class SupportingNamedMap<K, V>
            extends WrapperNamedCache<K, V>
            implements ContinuousAggregationSupport
        {
        SupportingNamedMap(String sName)
            {
            super(sName);
            }

        @Override
        public void registerContinuousAggregation(ContinuousAggregationDefinition definition)
            {
            m_cRegistrations++;
            m_definition = definition;
            }

        @Override
        public void removeContinuousAggregation(ContinuousAggregationDefinition definition)
            {
            assertEquals(m_definition, definition);
            m_cRemovals++;
            }

        @Override
        public Object aggregateContinuousAggregation(ContinuousAggregationDefinition definition)
            {
            m_aggregateDefinition = definition;
            assertEquals(m_definition, definition);
            return m_result;
            }

        private ContinuousAggregationDefinition m_definition;

        private ContinuousAggregationDefinition m_aggregateDefinition;

        private Object m_result;

        private int m_cRegistrations;

        private int m_cRemovals;
        }
    }
