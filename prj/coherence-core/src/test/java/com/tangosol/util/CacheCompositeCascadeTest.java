/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.internal.util.security.RemoteExecutionMode;
import com.tangosol.internal.util.security.RemoteInstallGate;
import com.tangosol.internal.util.security.SecurityConfig;

import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;

import com.tangosol.util.aggregator.CompositeAggregator;
import com.tangosol.util.aggregator.GroupAggregator;
import com.tangosol.util.aggregator.ReducerAggregator;
import com.tangosol.util.comparator.ChainedComparator;
import com.tangosol.util.comparator.EntryComparator;
import com.tangosol.util.comparator.ExtractorComparator;
import com.tangosol.util.comparator.InverseComparator;
import com.tangosol.util.comparator.SafeComparator;
import com.tangosol.util.extractor.KeyExtractor;
import com.tangosol.util.filter.AllFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.FilterTrigger;
import com.tangosol.util.filter.LimitFilter;
import com.tangosol.util.filter.MapEventTransformerFilter;
import com.tangosol.util.filter.NotFilter;
import com.tangosol.util.filter.WrapperQueryRecorderFilter;
import com.tangosol.util.processor.AsynchronousProcessor;
import com.tangosol.util.processor.CompositeProcessor;
import com.tangosol.util.processor.ConditionalProcessor;
import com.tangosol.util.processor.ConditionalPut;
import com.tangosol.util.processor.ExtractorProcessor;
import com.tangosol.util.processor.NumberIncrementor;
import com.tangosol.util.processor.PriorityProcessor;
import com.tangosol.util.processor.PropertyManipulator;
import com.tangosol.util.processor.UpdaterProcessor;
import com.tangosol.util.transformer.SemiLiteEventTransformer;

import com.tangosol.util.extractor.ReflectionUpdater;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import java.util.Comparator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the NamedCache install-gate composite cascade.
 *
 * @author Aleks Seovic  2026.05.12
 * @since 26.04
 */
public class CacheCompositeCascadeTest
    {
    @Before
    public void capturePropertyDefaults()
        {
        m_sModeOld          = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
        m_sDynamicRemoteOld = System.getProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH);
        setMode("prod");
        resetSecurityConfig();
        RemoteExecutablePolicy.resetForTesting();
        }

    @After
    public void cleanup()
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, m_sModeOld);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, m_sDynamicRemoteOld);
        resetMode();
        resetSecurityConfig();
        SerializationTelemetry.resetForTesting();
        RemoteExecutablePolicy.resetForTesting();
        }

    @Test
    public void cascadesCompositeAggregator()
        {
        assertAggregatorRejected(new CompositeAggregator<>(new InvocableMap.EntryAggregator[]
                {new CacheNamedCacheInstallGateTest.PlainAggregator()}), OperationReason.AGGREGATE);

        RemoteInstallGate.enforceCacheAggregatorInstall(new CompositeAggregator<>(new InvocableMap.EntryAggregator[]
                {new CacheNamedCacheInstallGateTest.AnnotatedAggregator()}), SerializationRole.EXTEND_PROXY, null);
        assertAllowed(OperationReason.AGGREGATE, 2L);
        }

    @Test
    public void cascadesGroupAggregator()
        {
        assertAggregatorRejected(GroupAggregator.createInstance(
                new CacheNamedCacheInstallGateTest.PlainExtractor(),
                new CacheNamedCacheInstallGateTest.AnnotatedAggregator()), OperationReason.EXTRACT);

        RemoteInstallGate.enforceCacheAggregatorInstall(GroupAggregator.createInstance(
                new CacheNamedCacheInstallGateTest.AnnotatedExtractor(),
                new CacheNamedCacheInstallGateTest.AnnotatedAggregator(),
                new CacheNamedCacheInstallGateTest.AnnotatedFilter()), SerializationRole.EXTEND_PROXY, null);
        assertAllowed(OperationReason.AGGREGATE, 2L);
        assertAllowed(OperationReason.EXTRACT, 1L);
        assertAllowed(OperationReason.EVALUATE_FILTER, 1L);
        }

    @Test
    public void cascadesReducerAggregator()
        {
        assertAggregatorRejected(new ReducerAggregator<>(new CacheNamedCacheInstallGateTest.PlainExtractor()),
                OperationReason.EXTRACT);

        RemoteInstallGate.enforceCacheAggregatorInstall(
                new ReducerAggregator<>(new CacheNamedCacheInstallGateTest.AnnotatedExtractor()),
                SerializationRole.EXTEND_PROXY, null);
        assertAllowed(OperationReason.AGGREGATE, 1L);
        assertAllowed(OperationReason.EXTRACT, 1L);
        }

    @Test
    public void cascadesCompositeProcessor()
        {
        assertProcessorRejected(new CompositeProcessor<>(new InvocableMap.EntryProcessor[]
                {new CacheNamedCacheInstallGateTest.PlainProcessor()}), OperationReason.PROCESS_ENTRY);

        RemoteInstallGate.enforceCacheProcessorInstall(new CompositeProcessor<>(new InvocableMap.EntryProcessor[]
                {new CacheNamedCacheInstallGateTest.AnnotatedProcessor()}), SerializationRole.EXTEND_PROXY, null);
        assertAllowed(OperationReason.PROCESS_ENTRY, 2L);
        }

    @Test
    public void cascadesConditionalProcessor()
        {
        assertProcessorRejected(new ConditionalProcessor<>(
                new CacheNamedCacheInstallGateTest.PlainFilter(),
                new CacheNamedCacheInstallGateTest.AnnotatedProcessor()), OperationReason.EVALUATE_FILTER);

        assertProcessorRejected(new ConditionalProcessor<>(
                new CacheNamedCacheInstallGateTest.AnnotatedFilter(),
                new CacheNamedCacheInstallGateTest.PlainProcessor()), OperationReason.PROCESS_ENTRY);

        RemoteInstallGate.enforceCacheProcessorInstall(new ConditionalProcessor<>(
                new CacheNamedCacheInstallGateTest.AnnotatedFilter(),
                new CacheNamedCacheInstallGateTest.AnnotatedProcessor()), SerializationRole.EXTEND_PROXY, null);
        assertAllowed(OperationReason.PROCESS_ENTRY, 2L);
        assertAllowed(OperationReason.EVALUATE_FILTER, 1L);
        }

    @Test
    public void cascadesProcessorWrappers()
        {
        assertProcessorRejected(new PriorityProcessor<>(new CacheNamedCacheInstallGateTest.PlainProcessor()),
                OperationReason.PROCESS_ENTRY);
        assertProcessorRejected(new AsynchronousProcessor<>(new CacheNamedCacheInstallGateTest.PlainProcessor()),
                OperationReason.PROCESS_ENTRY);
        assertProcessorRejected(new ConditionalPut<>(new CacheNamedCacheInstallGateTest.PlainFilter(), "value"),
                OperationReason.EVALUATE_FILTER);
        assertProcessorRejected(new ExtractorProcessor<>(new CacheNamedCacheInstallGateTest.PlainExtractor()),
                OperationReason.EXTRACT);
        }

    @Test
    public void cascadesUpdaterProcessor()
        {
        assertProcessorRejected(new UpdaterProcessor<>(new PlainValueUpdater(), "value"),
                OperationReason.PROCESS_ENTRY);

        RemoteInstallGate.enforceCacheProcessorInstall(new UpdaterProcessor<>(
                new ReflectionUpdater("setValue"), "value"), SerializationRole.EXTEND_PROXY, null);
        assertAllowed(OperationReason.PROCESS_ENTRY, 2L);
        }

    @Test
    public void cascadesPropertyProcessorManipulator()
        {
        assertProcessorRejected(new NumberIncrementor<>(new PlainValueManipulator(), 1, false),
                OperationReason.PROCESS_ENTRY);

        RemoteInstallGate.enforceCacheProcessorInstall(new NumberIncrementor<>(
                new PropertyManipulator<>("value"), 1, false), SerializationRole.EXTEND_PROXY, null);
        assertAllowed(OperationReason.PROCESS_ENTRY, 3L);
        assertAllowed(OperationReason.EXTRACT, 1L);
        }

    @Test
    public void cascadesArrayFilter()
        {
        assertFilterRejected(new AllFilter(new Filter<?>[] {new CacheNamedCacheInstallGateTest.PlainFilter()}),
                OperationReason.EVALUATE_FILTER);

        RemoteInstallGate.enforceCacheFilterInstall(new AllFilter(new Filter<?>[]
                {new CacheNamedCacheInstallGateTest.AnnotatedFilter()}), SerializationRole.EXTEND_PROXY, null);
        assertAllowed(OperationReason.EVALUATE_FILTER, 2L);
        }

    @Test
    public void cascadesExtractorFilter()
        {
        assertFilterRejected(new EqualsFilter<>(new CacheNamedCacheInstallGateTest.PlainExtractor(), "value"),
                OperationReason.EXTRACT);

        RemoteInstallGate.enforceCacheFilterInstall(
                new EqualsFilter<>(new CacheNamedCacheInstallGateTest.AnnotatedExtractor(), "value"),
                SerializationRole.EXTEND_PROXY, null);
        assertAllowed(OperationReason.EVALUATE_FILTER, 1L);
        assertAllowed(OperationReason.EXTRACT, 1L);
        }

    @Test
    public void cascadesLimitFilter()
        {
        assertFilterRejected(new LimitFilter<>(new CacheNamedCacheInstallGateTest.PlainFilter(), 10),
                OperationReason.EVALUATE_FILTER);

        LimitFilter<String> filter = new LimitFilter<>(new CacheNamedCacheInstallGateTest.AnnotatedFilter(), 10);
        filter.setComparator(new CacheNamedCacheInstallGateTest.AnnotatedComparator());

        RemoteInstallGate.enforceCacheFilterInstall(filter, SerializationRole.EXTEND_PROXY, null);
        assertAllowed(OperationReason.EVALUATE_FILTER, 2L);
        assertAllowed(OperationReason.COMPARE, 1L);
        }

    @Test
    public void cascadesFilterWrappers()
        {
        assertFilterRejected(new NotFilter<>(new CacheNamedCacheInstallGateTest.PlainFilter()),
                OperationReason.EVALUATE_FILTER);

        RemoteInstallGate.enforceCacheFilterInstall(
                new NotFilter<>(new CacheNamedCacheInstallGateTest.AnnotatedFilter()),
                SerializationRole.EXTEND_PROXY, null);
        assertAllowed(OperationReason.EVALUATE_FILTER, 2L);
        }

    @Test
    public void cascadesRemainingFilterWrappers()
        {
        assertMapTriggerRejected(new FilterTrigger(new CacheNamedCacheInstallGateTest.PlainFilter()),
                OperationReason.EVALUATE_FILTER);
        assertFilterRejected(new MapEventTransformerFilter<>(
                new CacheNamedCacheInstallGateTest.PlainFilter(), SemiLiteEventTransformer.INSTANCE),
                OperationReason.EVALUATE_FILTER);
        assertFilterRejected(new WrapperQueryRecorderFilter<>(new CacheNamedCacheInstallGateTest.PlainFilter()),
                OperationReason.EVALUATE_FILTER);

        RemoteInstallGate.enforceMapTriggerInstall(
                new FilterTrigger(new CacheNamedCacheInstallGateTest.AnnotatedFilter()),
                SerializationRole.EXTEND_PROXY, null);
        assertAllowed(OperationReason.TRIGGER, 1L);
        assertAllowed(OperationReason.EVALUATE_FILTER, 1L);

        SerializationTelemetry.resetForTesting();
        RemoteInstallGate.enforceCacheFilterInstall(new MapEventTransformerFilter<>(
                new CacheNamedCacheInstallGateTest.AnnotatedFilter(), SemiLiteEventTransformer.INSTANCE),
                SerializationRole.EXTEND_PROXY, null);
        assertAllowed(OperationReason.EVALUATE_FILTER, 2L);

        SerializationTelemetry.resetForTesting();
        RemoteInstallGate.enforceCacheFilterInstall(
                new WrapperQueryRecorderFilter<>(new CacheNamedCacheInstallGateTest.AnnotatedFilter()),
                SerializationRole.EXTEND_PROXY, null);
        assertAllowed(OperationReason.EVALUATE_FILTER, 2L);
        }

    @Test
    public void cascadesExtractorComparator()
        {
        assertComparatorRejected(new ExtractorComparator<>(new CacheNamedCacheInstallGateTest.PlainExtractor()),
                OperationReason.EXTRACT);

        RemoteInstallGate.enforceCacheComparatorInstall(
                new ExtractorComparator<>(new CacheNamedCacheInstallGateTest.AnnotatedExtractor()),
                SerializationRole.EXTEND_PROXY, null);
        assertAllowed(OperationReason.COMPARE, 1L);
        assertAllowed(OperationReason.EXTRACT, 1L);
        }

    @Test
    public void cascadesChainedComparator()
        {
        assertComparatorRejected(new ChainedComparator<>(new CacheNamedCacheInstallGateTest.PlainComparator()),
                OperationReason.COMPARE);

        RemoteInstallGate.enforceCacheComparatorInstall(
                new ChainedComparator<>(new CacheNamedCacheInstallGateTest.AnnotatedComparator()),
                SerializationRole.EXTEND_PROXY, null);
        assertAllowed(OperationReason.COMPARE, 2L);
        }

    @Test
    public void cascadesSafeComparator()
        {
        assertComparatorRejected(new SafeComparator<>(new CacheNamedCacheInstallGateTest.PlainComparator()),
                OperationReason.COMPARE);

        RemoteInstallGate.enforceCacheComparatorInstall(
                new SafeComparator<>(new CacheNamedCacheInstallGateTest.AnnotatedComparator()),
                SerializationRole.EXTEND_PROXY, null);
        assertAllowed(OperationReason.COMPARE, 2L);
        }

    @Test
    public void cascadesExtractorBackedComparator()
        {
        assertComparatorRejected(new KeyExtractor<>(new CacheNamedCacheInstallGateTest.PlainExtractor()),
                OperationReason.EXTRACT);

        RemoteInstallGate.enforceCacheComparatorInstall(
                new KeyExtractor<>(new CacheNamedCacheInstallGateTest.AnnotatedExtractor()),
                SerializationRole.EXTEND_PROXY, null);
        assertAllowed(OperationReason.COMPARE, 1L);
        assertAllowed(OperationReason.EXTRACT, 2L);
        }

    @Test
    public void cascadesWrappedExtractorBackedComparator()
        {
        assertComparatorRejected(new SafeComparator<>(
                new KeyExtractor<>(new CacheNamedCacheInstallGateTest.PlainExtractor())),
                OperationReason.EXTRACT);
        assertComparatorRejected(new InverseComparator<>(
                new KeyExtractor<>(new CacheNamedCacheInstallGateTest.PlainExtractor())),
                OperationReason.EXTRACT);
        assertComparatorRejected(new EntryComparator(
                new KeyExtractor<>(new CacheNamedCacheInstallGateTest.PlainExtractor())),
                OperationReason.EXTRACT);

        RemoteInstallGate.enforceCacheComparatorInstall(new SafeComparator<>(
                new KeyExtractor<>(new CacheNamedCacheInstallGateTest.AnnotatedExtractor())),
                SerializationRole.EXTEND_PROXY, null);
        assertAllowed(OperationReason.COMPARE, 2L);
        assertAllowed(OperationReason.EXTRACT, 2L);
        }

    @Test
    public void rejectsPathologicalCascadeDepth()
        {
        Comparator<String> comparator = new CacheNamedCacheInstallGateTest.AnnotatedComparator();
        for (int i = 0; i < 34; i++)
            {
            comparator = new ChainedComparator<>(comparator);
            }
        Comparator<String> deepComparator = comparator;

        SecurityException e = assertThrows(SecurityException.class,
                () -> RemoteInstallGate.enforceCacheComparatorInstall(
                        deepComparator, SerializationRole.EXTEND_PROXY, null));

        assertEquals("cache-install-cascade-depth-exceeded", e.getMessage());
        assertTrue(SerializationTelemetry.snapshot().toString(),
                SerializationTelemetry.snapshot().containsKey(key(OperationReason.COMPARE, "rejected")));
        }

    private static void assertAggregatorRejected(InvocableMap.EntryAggregator<?, ?, ?> aggregator,
                                                 OperationReason reason)
        {
        SerializationTelemetry.resetForTesting();

        assertThrows(SecurityException.class, () -> RemoteInstallGate.enforceCacheAggregatorInstall(
                aggregator, SerializationRole.EXTEND_PROXY, null));
        assertRejected(reason);
        SerializationTelemetry.resetForTesting();
        }

    private static void assertProcessorRejected(InvocableMap.EntryProcessor<?, ?, ?> processor, OperationReason reason)
        {
        SerializationTelemetry.resetForTesting();

        assertThrows(SecurityException.class, () -> RemoteInstallGate.enforceCacheProcessorInstall(
                processor, SerializationRole.EXTEND_PROXY, null));
        assertRejected(reason);
        SerializationTelemetry.resetForTesting();
        }

    private static void assertFilterRejected(Filter<?> filter, OperationReason reason)
        {
        SerializationTelemetry.resetForTesting();

        assertThrows(SecurityException.class, () -> RemoteInstallGate.enforceCacheFilterInstall(
                filter, SerializationRole.EXTEND_PROXY, null));
        assertRejected(reason);
        SerializationTelemetry.resetForTesting();
        }

    private static void assertMapTriggerRejected(MapTrigger trigger, OperationReason reason)
        {
        SerializationTelemetry.resetForTesting();

        assertThrows(SecurityException.class, () -> RemoteInstallGate.enforceMapTriggerInstall(
                trigger, SerializationRole.EXTEND_PROXY, null));
        assertRejected(reason);
        SerializationTelemetry.resetForTesting();
        }

    private static void assertExtractorRejected(ValueExtractor<?, ?> extractor, OperationReason reason)
        {
        SerializationTelemetry.resetForTesting();

        assertThrows(SecurityException.class, () -> RemoteInstallGate.enforceCacheExtractorInstall(
                extractor, SerializationRole.EXTEND_PROXY, null));
        assertRejected(reason);
        SerializationTelemetry.resetForTesting();
        }

    private static void assertComparatorRejected(Comparator<?> comparator, OperationReason reason)
        {
        SerializationTelemetry.resetForTesting();

        assertThrows(SecurityException.class, () -> RemoteInstallGate.enforceCacheComparatorInstall(
                comparator, SerializationRole.EXTEND_PROXY, null));
        assertRejected(reason);
        SerializationTelemetry.resetForTesting();
        }

    private static void assertRejected(OperationReason reason)
        {
        String sKey = key(reason, "rejected");
        assertEquals("counter " + sKey + " in " + SerializationTelemetry.snapshot(),
                Long.valueOf(1L), SerializationTelemetry.snapshot().get(sKey));
        }

    private static void assertAllowed(OperationReason reason, long cExpected)
        {
        String sKey = key(reason, "allowed");
        assertEquals("counter " + sKey + " in " + SerializationTelemetry.snapshot(),
                Long.valueOf(cExpected), SerializationTelemetry.snapshot().get(sKey));
        }

    private static String key(OperationReason reason, String sResult)
        {
        return "coh.executable.policy_check{reason=" + reason.name()
                + ",role=" + SerializationRole.EXTEND_PROXY.name()
                + ",result=" + sResult
                + ",mode=prod"
                + ",sub_reason=" + SerializationTelemetry.SUB_REASON_POLICY + "}";
        }

    private static void setMode(String sMode)
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, sMode);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, null);
        resetMode();
        SerializationTelemetry.resetForTesting();
        }

    private static void restoreProperty(String sName, String sValue)
        {
        if (sValue == null)
            {
            System.clearProperty(sName);
            }
        else
            {
            System.setProperty(sName, sValue);
            }
        }

    private static void resetMode()
        {
        CoherenceModeHelper.reset();
        RemoteExecutionMode.resetForTesting();
        }

    private static void resetSecurityConfig()
        {
        try
            {
            Method method = SecurityConfig.class.getDeclaredMethod("resetForTesting");
            method.setAccessible(true);
            method.invoke(null);
            }
        catch (ReflectiveOperationException e)
            {
            throw new AssertionError(e);
            }
        }

    private String m_sModeOld;
    private String m_sDynamicRemoteOld;

    private static class PlainValueUpdater
            implements ValueUpdater<Object, Object>
        {
        @Override
        public void update(Object target, Object value)
            {
            }
        }

    private static class PlainValueManipulator
            implements ValueManipulator<Object, Number>
        {
        @Override
        public ValueExtractor<Object, Number> getExtractor()
            {
            return target -> Integer.valueOf(1);
            }

        @Override
        public ValueUpdater<Object, Number> getUpdater()
            {
            return (target, value) -> { };
            }
        }
    }
