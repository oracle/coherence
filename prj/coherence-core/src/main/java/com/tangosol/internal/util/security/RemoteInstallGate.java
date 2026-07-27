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

import com.tangosol.util.MapTrigger;
import com.tangosol.util.OperationReason;
import com.tangosol.util.RemoteExecutablePolicy;
import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

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
     * Enforce topic subscriber filter/extractor installation policy.
     *
     * @param filter     the subscriber filter
     * @param extractor  the subscriber extractor
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
     * Enforce persisted topic subscriber filter/extractor replay policy.
     *
     * @param filter     the persisted subscriber filter
     * @param extractor  the persisted subscriber extractor
     * @param role       the serialization role
     * @param subject    the current subject, or {@code null}
     */
    public static final void enforceTopicSubscriberReplay(Filter<?> filter, Function<?, ?> extractor,
                                                          SerializationRole role, Subject subject)
        {
        enforceTopicSubscriberReplay(filter, extractor, role, subject, null);
        }

    /**
     * Enforce persisted topic subscriber filter/extractor replay policy.
     *
     * @param filter     the persisted subscriber filter
     * @param extractor  the persisted subscriber extractor
     * @param role       the serialization role
     * @param subject    the current subject, or {@code null}
     * @param setDedup   warning keys already emitted for this replay pass
     */
    public static final void enforceTopicSubscriberReplay(Filter<?> filter, Function<?, ?> extractor,
                                                          SerializationRole role, Subject subject,
                                                          Set<String> setDedup)
        {
        enforceReplay(filter == null ? null : filter.getClass(), OperationReason.EVALUATE_FILTER, role, subject,
                setDedup);
        enforceReplay(extractor == null ? null : extractor.getClass(), OperationReason.EXTRACT, role, subject,
                setDedup);
        }

    /**
     * Reset advisory logger for tests.
     */
    public static final void resetAdvisoryForTesting()
        {
        s_advisoryLogger = Logger::warn;
        }

    /**
     * Reset replay deduplication state for tests.
     */
    public static final void resetReplayDedupForTesting()
        {
        s_setReplayDedup.clear();
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

    private static void enforceReplay(Class<?> clz, OperationReason reason, SerializationRole role, Subject subject,
                                      Set<String> setDedup)
        {
        if (clz == null)
            {
            return;
            }

        RemoteExecutablePolicy policy = RemoteExecutablePolicy.current();
        if ((CoherenceMode.isLegacy() || !TopicsPersistedPolicyDrift.isReject()) && !policy.isExecutable(clz)
                && !recordReplayDedup(clz, reason, setDedup))
            {
            return;
            }

        try
            {
            policy.enforce(clz, reason, role, subject);
            return;
            }
        catch (SecurityException e)
            {
            if (TopicsPersistedPolicyDrift.isReject())
                {
                SerializationTelemetry.recordExecutablePolicyCheck("rejected", clz, reason, role, subject,
                        SerializationTelemetry.SUB_REASON_REPLAY_DRIFT);
                SerializationTelemetry.logRejection(TOPIC_SUBSCRIBER_REPLAY, roleName(role), subject,
                        className(clz), SerializationTelemetry.SUB_REASON_REPLAY_DRIFT);
                throw new SecurityException("topic-subscriber-replay-drift-rejected", e);
                }

            SerializationTelemetry.recordExecutablePolicyCheck("allowed", clz, reason, role, subject,
                    SerializationTelemetry.SUB_REASON_REPLAY_DRIFT);
            warnReplayDrift(clz, setDedup);
            }
        }

    private static void warnReplayDrift(Class<?> clz, Set<String> setDedup)
        {
        String sClassName = clz.getName();
        if (setDedup == null || setDedup.add("warn:" + sClassName))
            {
            s_advisoryLogger.accept("Persisted topic subscriber class " + sClassName
                    + " is no longer executable; replay allowed by "
                    + TopicsPersistedPolicyDrift.PROP_PERSISTED_POLICY_DRIFT + "="
                    + TopicsPersistedPolicyDrift.VALUE_WARN_ALLOW + ".");
            }
        }

    private static String replayDedupKey(Class<?> clz, OperationReason reason)
        {
        return "replay:" + reason.name() + ':' + clz.getName();
        }

    private static boolean recordReplayDedup(Class<?> clz, OperationReason reason, Set<String> setDedup)
        {
        String  sKey       = replayDedupKey(clz, reason);
        boolean fLocalNew  = setDedup == null || setDedup.add(sKey);
        boolean fGlobalNew = s_setReplayDedup.add(sKey);
        return fLocalNew && fGlobalNew;
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

    // ----- constants -----------------------------------------------------

    private static final String TOPIC_TRIGGER_INSTALL = "cache.trigger.install";

    private static final String TOPIC_SUBSCRIBER_INSTALL = "topic.subscriber.install";

    private static final String TOPIC_SUBSCRIBER_REPLAY = "topic.subscriber.replay";

    private static final String REASON_TRIGGER_DENIED_BY_MODE = "map-trigger-install-denied-by-mode";

    private static final String REASON_TOPIC_SUBSCRIBER_DENIED_BY_MODE = "topic-subscriber-install-denied-by-mode";

    // ----- data members --------------------------------------------------

    private static volatile Consumer<String> s_advisoryLogger = Logger::warn;

    private static final Set<String> s_setReplayDedup = ConcurrentHashMap.newKeySet();

    private RemoteInstallGate()
        {
        }
    }
