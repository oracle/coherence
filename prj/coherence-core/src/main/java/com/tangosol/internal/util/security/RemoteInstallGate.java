/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.security;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.util.CoherenceMode;

import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;

import com.tangosol.net.events.EventInterceptor;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapTrigger;
import com.tangosol.util.OperationReason;
import com.tangosol.util.RemoteExecutablePolicy;
import com.tangosol.util.AbstractScript;
import com.tangosol.util.ValueManipulator;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;
import com.tangosol.util.aggregator.AbstractAggregator;
import com.tangosol.util.aggregator.AbstractAsynchronousAggregator;
import com.tangosol.util.aggregator.AbstractComparableAggregator;
import com.tangosol.util.aggregator.CompositeAggregator;
import com.tangosol.util.aggregator.GroupAggregator;
import com.tangosol.util.aggregator.PriorityAggregator;
import com.tangosol.util.aggregator.ReducerAggregator;
import com.tangosol.util.aggregator.TopNAggregator;
import com.tangosol.util.comparator.ChainedComparator;
import com.tangosol.util.comparator.ExtractorComparator;
import com.tangosol.util.comparator.SafeComparator;
import com.tangosol.util.extractor.AbstractCompositeExtractor;
import com.tangosol.util.extractor.ChainedFragmentExtractor;
import com.tangosol.util.extractor.ComparisonValueExtractor;
import com.tangosol.util.extractor.ConditionalExtractor;
import com.tangosol.util.extractor.DeserializationAccelerator;
import com.tangosol.util.extractor.FragmentExtractor;
import com.tangosol.util.extractor.KeyExtractor;
import com.tangosol.util.extractor.ScriptValueExtractor;
import com.tangosol.util.filter.ArrayFilter;
import com.tangosol.util.filter.ExtractorFilter;
import com.tangosol.util.filter.FilterTrigger;
import com.tangosol.util.filter.InKeySetFilter;
import com.tangosol.util.filter.KeyAssociatedFilter;
import com.tangosol.util.filter.LimitFilter;
import com.tangosol.util.filter.MapEventFilter;
import com.tangosol.util.filter.MapEventTransformerFilter;
import com.tangosol.util.filter.NotFilter;
import com.tangosol.util.filter.PartitionedFilter;
import com.tangosol.util.filter.PriorityFilter;
import com.tangosol.util.filter.ValueChangeEventFilter;
import com.tangosol.util.filter.WrapperQueryRecorderFilter;
import com.tangosol.util.processor.AbstractAsynchronousProcessor;
import com.tangosol.util.processor.CompositeProcessor;
import com.tangosol.util.processor.ConditionalProcessor;
import com.tangosol.util.processor.ConditionalPut;
import com.tangosol.util.processor.ConditionalPutAll;
import com.tangosol.util.processor.ConditionalRemove;
import com.tangosol.util.processor.ExtractorProcessor;
import com.tangosol.util.processor.PropertyProcessor;
import com.tangosol.util.processor.PriorityProcessor;
import com.tangosol.util.processor.UpdaterProcessor;

import com.tangosol.util.extractor.CompositeUpdater;

import java.util.Comparator;
import java.util.Set;
import java.util.function.Consumer;

import javax.security.auth.Subject;

/**
 * Shared install-time gates for remote cache data-plane executable hooks.
 *
 * @author Aleks Seovic  2026.05.11
 * @since 26.04
 */
public final class RemoteInstallGate
    {
    /**
     * Enforce remote MapTrigger installation policy.
     *
     * @param trigger  the trigger being installed
     * @param role     the serialization role
     * @param subject  the current subject, or {@code null}
     */
    public static final void enforceMapTriggerInstall(MapTrigger trigger, SerializationRole role, Subject subject)
        {
        enforceInstall(trigger == null ? null : trigger.getClass(), OperationReason.TRIGGER, role, subject,
                TOPIC_TRIGGER_INSTALL, REASON_TRIGGER_DENIED_BY_MODE);
        cascadeFilterTrigger(trigger, role, subject, 1);
        }

    /**
     * Enforce concurrent task/callback installation policy.
     *
     * @param executable  the concurrent task or callback being installed
     * @param role        the serialization role
     * @param subject     the current subject, or {@code null}
     */
    public static final void enforceConcurrentTaskInstall(Object executable, SerializationRole role, Subject subject)
        {
        enforceConcurrentTaskInstall(executable, role, subject, 0);
        }

    /**
     * Enforce concurrent task/callback installation policy for nested state.
     *
     * @param executable  the concurrent task or callback being installed
     * @param role        the serialization role
     * @param subject     the current subject, or {@code null}
     * @param cDepth      the cascade depth
     */
    public static final void enforceConcurrentTaskInstall(Object executable, SerializationRole role, Subject subject,
                                                          int cDepth)
        {
        if (executable == null)
            {
            return;
            }

        enforceDepth(cDepth, executable.getClass(), OperationReason.CONCURRENT_TASK, role, subject,
                CONCURRENT_TASK_INSTALL);
        enforceInstall(executable.getClass(), OperationReason.CONCURRENT_TASK, role, subject,
                CONCURRENT_TASK_INSTALL, REASON_CONCURRENT_TASK_DENIED_BY_MODE);
        }

    /**
     * Enforce remote topic subscriber filter/extractor installation policy.
     *
     * @param filter     the subscriber filter being installed
     * @param extractor  the subscriber extractor being installed
     * @param role       the serialization role
     * @param subject    the current subject, or {@code null}
     */
    public static final void enforceTopicSubscriberInstall(Filter<?> filter, ValueExtractor<?, ?> extractor,
                                                           SerializationRole role, Subject subject)
        {
        if (filter != null)
            {
            enforceInstall(filter.getClass(), OperationReason.EVALUATE_FILTER, role, subject,
                    TOPIC_SUBSCRIBER_INSTALL, REASON_TOPIC_SUBSCRIBER_DENIED_BY_MODE);
            }
        if (extractor != null)
            {
            enforceInstall(extractor.getClass(), OperationReason.EXTRACT, role, subject,
                    TOPIC_SUBSCRIBER_INSTALL, REASON_TOPIC_SUBSCRIBER_DENIED_BY_MODE);
            }
        }

    /**
     * Enforce remote NamedCache entry processor installation policy.
     *
     * @param processor  the processor being installed
     * @param role       the serialization role
     * @param subject    the current subject, or {@code null}
     */
    public static final void enforceCacheProcessorInstall(InvocableMap.EntryProcessor<?, ?, ?> processor,
                                                          SerializationRole role, Subject subject)
        {
        enforceCacheProcessorInstall(processor, role, subject, 0);
        }

    /**
     * Enforce remote NamedCache entry processor class policy before a factory
     * invokes the processor constructor.
     *
     * @param clz      the processor class being constructed
     * @param role     the serialization role
     * @param subject  the current subject, or {@code null}
     */
    public static final void enforceCacheProcessorClassInstall(Class<?> clz, SerializationRole role, Subject subject)
        {
        enforceInstall(clz, OperationReason.PROCESS_ENTRY, role, subject,
                CACHE_PROCESSOR_INSTALL, REASON_CACHE_PROCESSOR_DENIED_BY_MODE);
        }

    /**
     * Enforce remote NamedCache entry aggregator installation policy.
     *
     * @param aggregator  the aggregator being installed
     * @param role        the serialization role
     * @param subject     the current subject, or {@code null}
     */
    public static final void enforceCacheAggregatorInstall(InvocableMap.EntryAggregator<?, ?, ?> aggregator,
                                                           SerializationRole role, Subject subject)
        {
        enforceCacheAggregatorInstall(aggregator, role, subject, 0);
        }

    /**
     * Enforce remote NamedCache entry aggregator class policy before a factory
     * invokes the aggregator constructor.
     *
     * @param clz      the aggregator class being constructed
     * @param role     the serialization role
     * @param subject  the current subject, or {@code null}
     */
    public static final void enforceCacheAggregatorClassInstall(Class<?> clz, SerializationRole role, Subject subject)
        {
        enforceInstall(clz, OperationReason.AGGREGATE, role, subject,
                CACHE_AGGREGATOR_INSTALL, REASON_CACHE_AGGREGATOR_DENIED_BY_MODE);
        }

    /**
     * Enforce remote NamedCache filter installation policy.
     *
     * @param filter   the filter being installed
     * @param role     the serialization role
     * @param subject  the current subject, or {@code null}
     */
    public static final void enforceCacheFilterInstall(Filter<?> filter, SerializationRole role, Subject subject)
        {
        enforceCacheFilterInstall(filter, role, subject, 0);
        }

    /**
     * Enforce remote NamedCache extractor installation policy.
     *
     * @param extractor  the extractor being installed
     * @param role       the serialization role
     * @param subject    the current subject, or {@code null}
     */
    public static final void enforceCacheExtractorInstall(ValueExtractor<?, ?> extractor,
                                                          SerializationRole role, Subject subject)
        {
        enforceCacheExtractorInstall(extractor, role, subject, 0);
        }

    /**
     * Enforce remote NamedCache comparator installation policy.
     *
     * @param comparator  the comparator being installed
     * @param role        the serialization role
     * @param subject     the current subject, or {@code null}
     */
    public static final void enforceCacheComparatorInstall(Comparator<?> comparator,
                                                           SerializationRole role, Subject subject)
        {
        enforceCacheComparatorInstall(comparator, role, subject, 0);
        }

    /**
     * Emit an advisory for a cache-config-declared MapTrigger class.
     *
     * @param trigger   the declared trigger
     * @param setDedup  advisory keys already emitted in this bootstrap pass
     */
    public static final void adviseDeclaredMapTrigger(MapTrigger trigger, Set<String> setDedup)
        {
        adviseDeclaredClass("MapTrigger", trigger == null ? null : trigger.getClass(), setDedup);
        }

    /**
     * Emit an advisory for a cache-config-declared EventInterceptor class.
     *
     * @param interceptor  the declared interceptor
     * @param setDedup     advisory keys already emitted in this bootstrap pass
     */
    public static final void adviseDeclaredEventInterceptor(EventInterceptor interceptor, Set<String> setDedup)
        {
        adviseDeclaredClass("EventInterceptor", interceptor == null ? null : interceptor.getClass(), setDedup);
        }

    /**
     * Emit an advisory for cache-config-declared topic subscriber classes.
     *
     * @param filter     the declared subscriber filter
     * @param extractor  the declared subscriber extractor
     * @param setDedup   advisory keys already emitted in this bootstrap pass
     */
    public static final void adviseDeclaredTopicSubscriber(Filter<?> filter, ValueExtractor<?, ?> extractor,
                                                           Set<String> setDedup)
        {
        adviseDeclaredClass("topic subscriber filter", filter == null ? null : filter.getClass(), setDedup);
        adviseDeclaredClass("topic subscriber extractor", extractor == null ? null : extractor.getClass(), setDedup);
        }

    /**
     * Reset advisory logger for tests.
     */
    public static final void resetAdvisoryForTesting()
        {
        s_advisoryLogger = Logger::warn;
        }

    /**
     * Set the advisory logger for tests.
     *
     * @param logger  the logger, or {@code null} to restore the default
     */
    public static final void setAdvisoryLoggerForTesting(Consumer<String> logger)
        {
        s_advisoryLogger = logger == null ? Logger::warn : logger;
        }

    // ----- helpers -------------------------------------------------------

    private static void enforceCacheProcessorInstall(InvocableMap.EntryProcessor<?, ?, ?> processor,
                                                     SerializationRole role, Subject subject, int cDepth)
        {
        if (processor == null)
            {
            return;
            }

        enforceDepth(cDepth, processor.getClass(), OperationReason.PROCESS_ENTRY, role, subject,
                CACHE_PROCESSOR_INSTALL);
        enforceInstall(processor.getClass(), OperationReason.PROCESS_ENTRY, role, subject,
                CACHE_PROCESSOR_INSTALL, REASON_CACHE_PROCESSOR_DENIED_BY_MODE);
        enforceScriptInstall(processor, role, subject);
        cascadeCompositeProcessor(processor, role, subject, cDepth + 1);
        cascadeConditionalProcessor(processor, role, subject, cDepth + 1);
        cascadePriorityProcessor(processor, role, subject, cDepth + 1);
        cascadeAsynchronousProcessor(processor, role, subject, cDepth + 1);
        cascadeConditionalMutationProcessor(processor, role, subject, cDepth + 1);
        cascadeExtractorProcessor(processor, role, subject, cDepth + 1);
        cascadePropertyProcessor(processor, role, subject, cDepth + 1);
        cascadeUpdaterProcessor(processor, role, subject, cDepth + 1);
        cascadeProcessorCarrier(processor, role, subject, cDepth + 1);
        }

    private static void enforceCacheAggregatorInstall(InvocableMap.EntryAggregator<?, ?, ?> aggregator,
                                                      SerializationRole role, Subject subject, int cDepth)
        {
        if (aggregator == null)
            {
            return;
            }

        enforceDepth(cDepth, aggregator.getClass(), OperationReason.AGGREGATE, role, subject,
                CACHE_AGGREGATOR_INSTALL);
        enforceInstall(aggregator.getClass(), OperationReason.AGGREGATE, role, subject,
                CACHE_AGGREGATOR_INSTALL, REASON_CACHE_AGGREGATOR_DENIED_BY_MODE);
        enforceScriptInstall(aggregator, role, subject);
        cascadeAbstractAggregator(aggregator, role, subject, cDepth + 1);
        cascadeCompositeAggregator(aggregator, role, subject, cDepth + 1);
        cascadeGroupAggregator(aggregator, role, subject, cDepth + 1);
        cascadeReducerAggregator(aggregator, role, subject, cDepth + 1);
        cascadeAsynchronousAggregator(aggregator, role, subject, cDepth + 1);
        cascadePriorityAggregator(aggregator, role, subject, cDepth + 1);
        cascadeTopNAggregator(aggregator, role, subject, cDepth + 1);
        }

    private static void enforceCacheFilterInstall(Filter<?> filter, SerializationRole role, Subject subject,
                                                  int cDepth)
        {
        if (filter == null)
            {
            return;
            }

        enforceDepth(cDepth, filter.getClass(), OperationReason.EVALUATE_FILTER, role, subject,
                CACHE_FILTER_INSTALL);
        enforceInstall(filter.getClass(), OperationReason.EVALUATE_FILTER, role, subject,
                CACHE_FILTER_INSTALL, REASON_CACHE_FILTER_DENIED_BY_MODE);
        enforceScriptInstall(filter, role, subject);
        cascadeArrayFilter(filter, role, subject, cDepth + 1);
        cascadeExtractorFilter(filter, role, subject, cDepth + 1);
        cascadeLimitFilter(filter, role, subject, cDepth + 1);
        cascadeFilterWrapper(filter, role, subject, cDepth + 1);
        cascadeValueChangeEventFilter(filter, role, subject, cDepth + 1);
        }

    private static void enforceCacheExtractorInstall(ValueExtractor<?, ?> extractor, SerializationRole role,
                                                     Subject subject, int cDepth)
        {
        if (extractor == null)
            {
            return;
            }

        enforceDepth(cDepth, extractor.getClass(), OperationReason.EXTRACT, role, subject,
                CACHE_EXTRACTOR_INSTALL);
        enforceInstall(extractor.getClass(), OperationReason.EXTRACT, role, subject,
                CACHE_EXTRACTOR_INSTALL, REASON_CACHE_EXTRACTOR_DENIED_BY_MODE);
        enforceScriptExtractorInstall(extractor, role, subject);
        cascadeCompositeExtractor(extractor, role, subject, cDepth + 1);
        cascadeConditionalExtractor(extractor, role, subject, cDepth + 1);
        cascadeSingleExtractorWrapper(extractor, role, subject, cDepth + 1);
        cascadeFragmentExtractor(extractor, role, subject, cDepth + 1);
        cascadeComparisonValueExtractor(extractor, role, subject, cDepth + 1);
        }

    private static void enforceCacheComparatorInstall(Comparator<?> comparator, SerializationRole role,
                                                      Subject subject, int cDepth)
        {
        if (comparator == null)
            {
            return;
            }

        enforceDepth(cDepth, comparator.getClass(), OperationReason.COMPARE, role, subject,
                CACHE_COMPARATOR_INSTALL);
        enforceInstall(comparator.getClass(), OperationReason.COMPARE, role, subject,
                CACHE_COMPARATOR_INSTALL, REASON_CACHE_COMPARATOR_DENIED_BY_MODE);
        cascadeExtractorBackedComparator(comparator, role, subject, cDepth + 1);
        cascadeExtractorComparator(comparator, role, subject, cDepth + 1);
        cascadeChainedComparator(comparator, role, subject, cDepth + 1);
        cascadeSafeComparator(comparator, role, subject, cDepth + 1);
        }

    private static void enforceScriptInstall(Object executable, SerializationRole role, Subject subject)
        {
        if (executable instanceof AbstractScript)
            {
            AbstractScript script = (AbstractScript) executable;
            RemoteScriptGate.enforceScriptEvaluation(executable.getClass(), script.getLanguage(), script.getName(),
                    role, subject);
            }
        }

    private static void enforceScriptExtractorInstall(ValueExtractor<?, ?> extractor, SerializationRole role,
                                                      Subject subject)
        {
        if (extractor instanceof ScriptValueExtractor)
            {
            ScriptValueExtractor<?, ?> script = (ScriptValueExtractor<?, ?>) extractor;
            RemoteScriptGate.enforceScriptEvaluation(extractor.getClass(), script.getLanguage(), script.getName(),
                    role, subject);
            }
        }

    private static void cascadeCompositeProcessor(InvocableMap.EntryProcessor<?, ?, ?> processor,
                                                  SerializationRole role, Subject subject, int cDepth)
        {
        if (processor instanceof CompositeProcessor)
            {
            for (InvocableMap.EntryProcessor<?, ?, ?> nested : ((CompositeProcessor<?, ?>) processor).getProcessors())
                {
                enforceCacheProcessorInstall(nested, role, subject, cDepth);
                }
            }
        }

    private static void cascadeConditionalProcessor(InvocableMap.EntryProcessor<?, ?, ?> processor,
                                                    SerializationRole role, Subject subject, int cDepth)
        {
        if (processor instanceof ConditionalProcessor)
            {
            ConditionalProcessor<?, ?, ?> conditional = (ConditionalProcessor<?, ?, ?>) processor;
            enforceCacheFilterInstall(conditional.getFilter(), role, subject, cDepth);
            enforceCacheProcessorInstall(conditional.getProcessor(), role, subject, cDepth);
            }
        }

    private static void cascadePriorityProcessor(InvocableMap.EntryProcessor<?, ?, ?> processor,
                                                 SerializationRole role, Subject subject, int cDepth)
        {
        if (processor instanceof PriorityProcessor)
            {
            enforceCacheProcessorInstall(((PriorityProcessor<?, ?, ?>) processor).getProcessor(),
                    role, subject, cDepth);
            }
        }

    private static void cascadeAsynchronousProcessor(InvocableMap.EntryProcessor<?, ?, ?> processor,
                                                     SerializationRole role, Subject subject, int cDepth)
        {
        if (processor instanceof AbstractAsynchronousProcessor)
            {
            enforceCacheProcessorInstall(((AbstractAsynchronousProcessor<?, ?, ?, ?>) processor).getProcessor(),
                    role, subject, cDepth);
            }
        }

    private static void cascadeConditionalMutationProcessor(InvocableMap.EntryProcessor<?, ?, ?> processor,
                                                            SerializationRole role, Subject subject, int cDepth)
        {
        if (processor instanceof ConditionalPut)
            {
            enforceCacheFilterInstall(((ConditionalPut<?, ?>) processor).getFilter(), role, subject, cDepth);
            }
        else if (processor instanceof ConditionalPutAll)
            {
            enforceCacheFilterInstall(((ConditionalPutAll<?, ?>) processor).getFilter(), role, subject, cDepth);
            }
        else if (processor instanceof ConditionalRemove)
            {
            enforceCacheFilterInstall(((ConditionalRemove<?, ?>) processor).getFilter(), role, subject, cDepth);
            }
        }

    private static void cascadeExtractorProcessor(InvocableMap.EntryProcessor<?, ?, ?> processor,
                                                  SerializationRole role, Subject subject, int cDepth)
        {
        if (processor instanceof ExtractorProcessor)
            {
            enforceCacheExtractorInstall(((ExtractorProcessor<?, ?, ?, ?>) processor).getExtractor(),
                    role, subject, cDepth);
            }
        }

    private static void cascadePropertyProcessor(InvocableMap.EntryProcessor<?, ?, ?> processor,
                                                 SerializationRole role, Subject subject, int cDepth)
        {
        if (processor instanceof PropertyProcessor)
            {
            enforceCacheManipulatorInstall(((PropertyProcessor<?, ?, ?>) processor).getValueManipulator(),
                    role, subject, cDepth);
            }
        }

    private static void cascadeUpdaterProcessor(InvocableMap.EntryProcessor<?, ?, ?> processor,
                                                SerializationRole role, Subject subject, int cDepth)
        {
        if (processor instanceof UpdaterProcessor)
            {
            enforceCacheUpdaterInstall(((UpdaterProcessor<?, ?, ?>) processor).getValueUpdater(),
                    role, subject, cDepth);
            }
        }

    private static void cascadeProcessorCarrier(InvocableMap.EntryProcessor<?, ?, ?> processor,
                                                SerializationRole role, Subject subject, int cDepth)
        {
        if (processor instanceof CacheProcessorCarrier)
            {
            for (InvocableMap.EntryProcessor nested :
                    ((CacheProcessorCarrier) processor).getProcessorsForInstallGate())
                {
                enforceCacheProcessorInstall(nested, role, subject, cDepth);
                }
            }
        }

    private static void enforceCacheManipulatorInstall(ValueManipulator<?, ?> manipulator, SerializationRole role,
                                                       Subject subject, int cDepth)
        {
        if (manipulator == null)
            {
            return;
            }

        enforceDepth(cDepth, manipulator.getClass(), OperationReason.PROCESS_ENTRY, role, subject,
                CACHE_PROCESSOR_INSTALL);
        enforceInstall(manipulator.getClass(), OperationReason.PROCESS_ENTRY, role, subject,
                CACHE_PROCESSOR_INSTALL, REASON_CACHE_PROCESSOR_DENIED_BY_MODE);
        enforceCacheExtractorInstall(manipulator.getExtractor(), role, subject, cDepth + 1);
        enforceCacheUpdaterInstall(manipulator.getUpdater(), role, subject, cDepth + 1);
        }

    private static void enforceCacheUpdaterInstall(ValueUpdater<?, ?> updater, SerializationRole role,
                                                   Subject subject, int cDepth)
        {
        if (updater == null)
            {
            return;
            }

        enforceDepth(cDepth, updater.getClass(), OperationReason.PROCESS_ENTRY, role, subject,
                CACHE_PROCESSOR_INSTALL);
        enforceInstall(updater.getClass(), OperationReason.PROCESS_ENTRY, role, subject,
                CACHE_PROCESSOR_INSTALL, REASON_CACHE_PROCESSOR_DENIED_BY_MODE);
        if (updater instanceof CompositeUpdater)
            {
            CompositeUpdater composite = (CompositeUpdater) updater;
            enforceCacheExtractorInstall(composite.getExtractor(), role, subject, cDepth + 1);
            enforceCacheUpdaterInstall(composite.getUpdater(), role, subject, cDepth + 1);
            }
        }

    private static void cascadeAbstractAggregator(InvocableMap.EntryAggregator<?, ?, ?> aggregator,
                                                  SerializationRole role, Subject subject, int cDepth)
        {
        if (aggregator instanceof AbstractAggregator
                && !(aggregator instanceof GroupAggregator)
                && !(aggregator instanceof ReducerAggregator)
                && !(aggregator instanceof TopNAggregator))
            {
            enforceCacheExtractorInstall(((AbstractAggregator<?, ?, ?, ?, ?>) aggregator).getValueExtractor(),
                    role, subject, cDepth);
            }
        if (aggregator instanceof AbstractComparableAggregator
                && !(aggregator instanceof TopNAggregator))
            {
            enforceCacheComparatorInstall(((AbstractComparableAggregator<?, ?>) aggregator).getComparator(),
                    role, subject, cDepth);
            }
        }

    private static void cascadeCompositeAggregator(InvocableMap.EntryAggregator<?, ?, ?> aggregator,
                                                   SerializationRole role, Subject subject, int cDepth)
        {
        if (aggregator instanceof CompositeAggregator)
            {
            for (InvocableMap.EntryAggregator nested : ((CompositeAggregator) aggregator).getAggregators())
                {
                enforceCacheAggregatorInstall(nested, role, subject, cDepth);
                }
            }
        }

    private static void cascadeGroupAggregator(InvocableMap.EntryAggregator<?, ?, ?> aggregator,
                                               SerializationRole role, Subject subject, int cDepth)
        {
        if (aggregator instanceof GroupAggregator)
            {
            GroupAggregator group = (GroupAggregator) aggregator;
            enforceCacheExtractorInstall(group.getExtractor(), role, subject, cDepth);
            enforceCacheAggregatorInstall(group.getAggregator(), role, subject, cDepth);
            enforceCacheFilterInstall(group.getFilter(), role, subject, cDepth);
            }
        }

    private static void cascadeReducerAggregator(InvocableMap.EntryAggregator<?, ?, ?> aggregator,
                                                 SerializationRole role, Subject subject, int cDepth)
        {
        if (aggregator instanceof ReducerAggregator)
            {
            enforceCacheExtractorInstall(((ReducerAggregator) aggregator).getValueExtractor(), role, subject, cDepth);
            }
        }

    private static void cascadeAsynchronousAggregator(InvocableMap.EntryAggregator<?, ?, ?> aggregator,
                                                      SerializationRole role, Subject subject, int cDepth)
        {
        if (aggregator instanceof AbstractAsynchronousAggregator)
            {
            enforceCacheAggregatorInstall(((AbstractAsynchronousAggregator<?, ?, ?, ?>) aggregator).getAggregator(),
                    role, subject, cDepth);
            }
        }

    private static void cascadePriorityAggregator(InvocableMap.EntryAggregator<?, ?, ?> aggregator,
                                                  SerializationRole role, Subject subject, int cDepth)
        {
        if (aggregator instanceof PriorityAggregator)
            {
            enforceCacheAggregatorInstall(((PriorityAggregator<?, ?, ?, ?>) aggregator).getAggregator(),
                    role, subject, cDepth);
            }
        }

    private static void cascadeTopNAggregator(InvocableMap.EntryAggregator<?, ?, ?> aggregator,
                                              SerializationRole role, Subject subject, int cDepth)
        {
        if (aggregator instanceof TopNAggregator)
            {
            TopNAggregator<?, ?, ?, ?> top = (TopNAggregator<?, ?, ?, ?>) aggregator;
            enforceCacheExtractorInstall(top.getValueExtractor(), role, subject, cDepth);
            enforceCacheComparatorInstall(top.getComparator(), role, subject, cDepth);
            }
        }

    private static void cascadeArrayFilter(Filter<?> filter, SerializationRole role, Subject subject, int cDepth)
        {
        if (filter instanceof ArrayFilter)
            {
            for (Filter<?> nested : ((ArrayFilter) filter).getFilters())
                {
                enforceCacheFilterInstall(nested, role, subject, cDepth);
                }
            }
        }

    private static void cascadeExtractorFilter(Filter<?> filter, SerializationRole role, Subject subject, int cDepth)
        {
        if (filter instanceof ExtractorFilter)
            {
            enforceCacheExtractorInstall(((ExtractorFilter) filter).getValueExtractor(), role, subject, cDepth);
            }
        }

    private static void cascadeLimitFilter(Filter<?> filter, SerializationRole role, Subject subject, int cDepth)
        {
        if (filter instanceof LimitFilter)
            {
            LimitFilter<?> limitFilter = (LimitFilter<?>) filter;
            enforceCacheFilterInstall(limitFilter.getFilter(), role, subject, cDepth);
            enforceCacheComparatorInstall(limitFilter.getComparator(), role, subject, cDepth);
            }
        }

    private static void cascadeFilterWrapper(Filter<?> filter, SerializationRole role, Subject subject, int cDepth)
        {
        if (filter instanceof NotFilter)
            {
            enforceCacheFilterInstall(((NotFilter<?>) filter).getFilter(), role, subject, cDepth);
            }
        else if (filter instanceof KeyAssociatedFilter)
            {
            enforceCacheFilterInstall(((KeyAssociatedFilter<?>) filter).getFilter(), role, subject, cDepth);
            }
        else if (filter instanceof PartitionedFilter)
            {
            enforceCacheFilterInstall(((PartitionedFilter<?>) filter).getFilter(), role, subject, cDepth);
            }
        else if (filter instanceof PriorityFilter)
            {
            enforceCacheFilterInstall(((PriorityFilter<?>) filter).getFilter(), role, subject, cDepth);
            }
        else if (filter instanceof InKeySetFilter)
            {
            enforceCacheFilterInstall(((InKeySetFilter<?>) filter).getFilter(), role, subject, cDepth);
            }
        else if (filter instanceof MapEventFilter)
            {
            enforceCacheFilterInstall(((MapEventFilter<?, ?>) filter).getFilter(), role, subject, cDepth);
            }
        else if (filter instanceof MapEventTransformerFilter)
            {
            enforceCacheFilterInstall(((MapEventTransformerFilter<?>) filter).getFilter(), role, subject, cDepth);
            }
        else if (filter instanceof WrapperQueryRecorderFilter)
            {
            enforceCacheFilterInstall(((WrapperQueryRecorderFilter<?>) filter).getFilter(), role, subject, cDepth);
            }
        }

    private static void cascadeFilterTrigger(MapTrigger trigger, SerializationRole role, Subject subject, int cDepth)
        {
        if (trigger instanceof FilterTrigger)
            {
            enforceCacheFilterInstall(((FilterTrigger) trigger).getFilter(), role, subject, cDepth);
            }
        }

    private static void cascadeValueChangeEventFilter(Filter<?> filter, SerializationRole role, Subject subject,
                                                      int cDepth)
        {
        if (filter instanceof ValueChangeEventFilter)
            {
            enforceCacheExtractorInstall(((ValueChangeEventFilter<?, ?>) filter).getValueExtractor(),
                    role, subject, cDepth);
            }
        }

    private static void cascadeCompositeExtractor(ValueExtractor<?, ?> extractor, SerializationRole role,
                                                  Subject subject, int cDepth)
        {
        if (extractor instanceof AbstractCompositeExtractor)
            {
            for (ValueExtractor<?, ?> nested : ((AbstractCompositeExtractor<?, ?>) extractor).getExtractors())
                {
                enforceCacheExtractorInstall(nested, role, subject, cDepth);
                }
            }
        }

    private static void cascadeConditionalExtractor(ValueExtractor<?, ?> extractor, SerializationRole role,
                                                    Subject subject, int cDepth)
        {
        if (extractor instanceof ConditionalExtractor)
            {
            ConditionalExtractor<?, ?> conditional = (ConditionalExtractor<?, ?>) extractor;
            enforceCacheFilterInstall(conditional.getFilter(), role, subject, cDepth);
            enforceCacheExtractorInstall(conditional.getExtractor(), role, subject, cDepth);
            }
        }

    private static void cascadeSingleExtractorWrapper(ValueExtractor<?, ?> extractor, SerializationRole role,
                                                      Subject subject, int cDepth)
        {
        if (extractor instanceof DeserializationAccelerator)
            {
            enforceCacheExtractorInstall(((DeserializationAccelerator) extractor).getExtractor(),
                    role, subject, cDepth);
            }
        else if (extractor instanceof KeyExtractor)
            {
            enforceCacheExtractorInstall(((KeyExtractor<?, ?>) extractor).getExtractor(), role, subject, cDepth);
            }
        }

    private static void cascadeFragmentExtractor(ValueExtractor<?, ?> extractor, SerializationRole role,
                                                 Subject subject, int cDepth)
        {
        if (extractor instanceof FragmentExtractor)
            {
            for (ValueExtractor<?, ?> nested : ((FragmentExtractor<?>) extractor).getExtractors())
                {
                enforceCacheExtractorInstall(nested, role, subject, cDepth);
                }
            }
        else if (extractor instanceof ChainedFragmentExtractor)
            {
            ChainedFragmentExtractor<?, ?> chained = (ChainedFragmentExtractor<?, ?>) extractor;
            enforceCacheExtractorInstall(chained.getExtractor(), role, subject, cDepth);
            enforceCacheExtractorInstall(chained.getFragmentExtractor(), role, subject, cDepth);
            }
        }

    private static void cascadeComparisonValueExtractor(ValueExtractor<?, ?> extractor, SerializationRole role,
                                                        Subject subject, int cDepth)
        {
        if (extractor instanceof ComparisonValueExtractor)
            {
            enforceCacheComparatorInstall(((ComparisonValueExtractor<?, ?>) extractor).getComparator(),
                    role, subject, cDepth);
            }
        }

    private static void cascadeExtractorBackedComparator(Comparator<?> comparator, SerializationRole role,
                                                         Subject subject, int cDepth)
        {
        if (comparator instanceof ValueExtractor)
            {
            enforceCacheExtractorInstall((ValueExtractor<?, ?>) comparator, role, subject, cDepth);
            }
        }

    private static void cascadeExtractorComparator(Comparator<?> comparator, SerializationRole role, Subject subject,
                                                   int cDepth)
        {
        if (comparator instanceof ExtractorComparator)
            {
            enforceCacheExtractorInstall(((ExtractorComparator) comparator).getExtractor(), role, subject, cDepth);
            }
        }

    private static void cascadeSafeComparator(Comparator<?> comparator, SerializationRole role, Subject subject,
                                              int cDepth)
        {
        if (comparator instanceof SafeComparator)
            {
            enforceCacheComparatorInstall(((SafeComparator<?>) comparator).getComparator(),
                    role, subject, cDepth);
            }
        }

    private static void cascadeChainedComparator(Comparator<?> comparator, SerializationRole role, Subject subject,
                                                 int cDepth)
        {
        if (comparator instanceof ChainedComparator)
            {
            for (Comparator<?> nested : ((ChainedComparator<?>) comparator).getComparators())
                {
                enforceCacheComparatorInstall(nested, role, subject, cDepth);
                }
            }
        }

    private static void enforceDepth(int cDepth, Class<?> clz, OperationReason reason, SerializationRole role,
                                     Subject subject, String sTopic)
        {
        if (cDepth > MAX_CASCADE_DEPTH)
            {
            SerializationTelemetry.recordExecutablePolicyCheck("rejected", clz, reason, role, subject,
                    SerializationTelemetry.SUB_REASON_POLICY);
            SerializationTelemetry.logRejection(sTopic, roleName(role), subject, className(clz),
                    SerializationTelemetry.SUB_REASON_POLICY);
            throw new SecurityException("cache-install-cascade-depth-exceeded");
            }
        }

    private static void enforceInstall(Class<?> clz, OperationReason reason, SerializationRole role, Subject subject,
                                       String sTopic, String sModeReason)
        {
        boolean fDynamic = isDynamicRemoteClass(clz);
        try
            {
            RemoteExecutablePolicy.current().enforce(clz, reason, role, subject);
            }
        catch (SecurityException e)
            {
            if (!fDynamic)
                {
                SerializationTelemetry.logRejection(sTopic, roleName(role), subject, className(clz),
                        SerializationTelemetry.SUB_REASON_POLICY);
                throw e;
                }
            }

        if (fDynamic)
            {
            enforceDynamicInstall(clz, reason, role, subject, sTopic, sModeReason);
            }
        }

    private static void enforceDynamicInstall(Class<?> clz, OperationReason reason, SerializationRole role,
                                              Subject subject, String sTopic, String sModeReason)
        {
        if (CoherenceMode.isLegacy())
            {
            // policy owns class shadow telemetry; the install gate owns the dynamic-mode shadow.
            SerializationTelemetry.recordExecutablePolicyCheck("would_reject", clz, reason, role, subject,
                    SerializationTelemetry.SUB_REASON_MODE_GATE);
            return;
            }

        if (!RemoteExecutionMode.isDynamicRemoteAllowed())
            {
            SerializationTelemetry.recordExecutablePolicyCheck("rejected", clz, reason, role, subject,
                    SerializationTelemetry.SUB_REASON_MODE_GATE);
            SerializationTelemetry.logRejection(sTopic, roleName(role), subject, className(clz),
                    SerializationTelemetry.SUB_REASON_MODE_GATE);
            throw new SecurityException(sModeReason);
            }
        }

    private static void adviseDeclaredClass(String sKind, Class<?> clz, Set<String> setDedup)
        {
        if (clz == null || RemoteExecutablePolicy.current().isExecutable(clz))
            {
            return;
            }

        String sClassName = clz.getName();
        if (setDedup == null || setDedup.add(sKind + ':' + sClassName))
            {
            s_advisoryLogger.accept("Declared " + sKind + " class " + sClassName
                    + " is not annotated @Remote.Executable; remote-install of this class would be refused.");
            }
        }

    private static boolean isDynamicRemoteClass(Class<?> clz)
        {
        return clz != null && (clz.isSynthetic() || clz.getName().contains("$$Lambda"));
        }

    private static String className(Class<?> clz)
        {
        return clz == null ? "null" : clz.getName();
        }

    private static String roleName(SerializationRole role)
        {
        return (role == null ? SerializationRole.current() : role).name();
        }

    // ----- inner interface: CacheProcessorCarrier ------------------------

    /**
     * Narrow extension point for product entry-processor wrappers whose
     * nested processors must be walked by the cache processor install gate.
     */
    public interface CacheProcessorCarrier
        {
        /**
         * Return nested processors that must be install-gated before this
         * wrapper can invoke them.
         *
         * @return nested processors for install-gate cascade
         */
        Iterable<? extends InvocableMap.EntryProcessor> getProcessorsForInstallGate();
        }

    // ----- constants -----------------------------------------------------

    private static final String TOPIC_TRIGGER_INSTALL = "cache.trigger.install";

    private static final String TOPIC_SUBSCRIBER_INSTALL = "topic.subscriber.install";

    private static final String CONCURRENT_TASK_INSTALL = "concurrent.task.install";

    private static final String CACHE_PROCESSOR_INSTALL = "cache.processor.install";

    private static final String CACHE_AGGREGATOR_INSTALL = "cache.aggregator.install";

    private static final String CACHE_FILTER_INSTALL = "cache.filter.install";

    private static final String CACHE_EXTRACTOR_INSTALL = "cache.extractor.install";

    private static final String CACHE_COMPARATOR_INSTALL = "cache.comparator.install";

    private static final String REASON_TRIGGER_DENIED_BY_MODE = "map-trigger-install-denied-by-mode";

    private static final String REASON_TOPIC_SUBSCRIBER_DENIED_BY_MODE =
            "topic-subscriber-install-denied-by-mode";

    private static final String REASON_CONCURRENT_TASK_DENIED_BY_MODE =
            "concurrent-task-install-denied-by-mode";

    private static final String REASON_CACHE_PROCESSOR_DENIED_BY_MODE =
            "cache-processor-install-denied-by-mode";

    private static final String REASON_CACHE_AGGREGATOR_DENIED_BY_MODE =
            "cache-aggregator-install-denied-by-mode";

    private static final String REASON_CACHE_FILTER_DENIED_BY_MODE =
            "cache-filter-install-denied-by-mode";

    private static final String REASON_CACHE_EXTRACTOR_DENIED_BY_MODE =
            "cache-extractor-install-denied-by-mode";

    private static final String REASON_CACHE_COMPARATOR_DENIED_BY_MODE =
            "cache-comparator-install-denied-by-mode";

    private static final int MAX_CASCADE_DEPTH = 32;

    // ----- data members --------------------------------------------------

    private static volatile Consumer<String> s_advisoryLogger = Logger::warn;

    private RemoteInstallGate()
        {
        }
    }
