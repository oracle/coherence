/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.util.CoherenceMode;

import com.tangosol.io.SerializationRole;

import com.tangosol.net.management.AnnotatedStandardMBean;
import com.tangosol.net.management.Registry;
import com.tangosol.net.security.SecurityHelper;
import com.tangosol.util.OperationReason;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.security.auth.Subject;

/**
 * Shared telemetry for serialization gate checks.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public final class SerializationTelemetry
    {
    /**
     * Record a serialization filter check.
     *
     * @param sResult      the check result
     * @param sReason      the check reason
     * @param clzDenied    the denied class, if any
     * @param subject      the current subject, if known
     */
    public static void recordFilterCheck(String sResult, String sReason, Class<?> clzDenied, Subject subject)
        {
        SerializationRole role = SerializationRole.current();
        String            sMode = modeTag();
        record(METRIC_FILTER_CHECK, sResult, sReason, role, null, null,
                "result", sResult,
                "reason", sReason,
                "mode", sMode,
                "route", role.name(),
                "principal", principal(subject));

        if ("rejected".equals(sResult))
            {
            logRejection("filter", role.name(), subject,
                    clzDenied == null ? null : clzDenied.getName(), sReason);
            }
        }

    /**
     * Record a serialization format check.
     *
     * @param sResult  the check result
     * @param sReason  the check reason
     * @param nFmt     the format code
     */
    public static void recordFmtCheck(String sResult, String sReason, int nFmt)
        {
        SerializationRole role = SerializationRole.current();
        String            sMode = modeTag();
        record(METRIC_FMT_CHECK, sResult, sReason, role, TAG_FMT, nFmt,
                "result", sResult,
                "reason", sReason,
                "mode", sMode,
                "fmt", String.valueOf(nFmt),
                "route", role.name());

        if ("rejected".equals(sResult))
            {
            logRejection("fmt", role.name(), currentSubject(), null, sReason);
            }
        }

    /**
     * Record a POF type check.
     *
     * @param sResult  the check result
     * @param sReason  the check reason
     * @param nTypeId  the POF type id
     */
    public static void recordPofCheck(String sResult, String sReason, int nTypeId)
        {
        SerializationRole role = SerializationRole.current();
        String            sMode = modeTag();
        record(METRIC_POF_CHECK, sResult, sReason, role, TAG_TYPE_ID, nTypeId,
                "result", sResult,
                "reason", sReason,
                "mode", sMode,
                "type_id", String.valueOf(nTypeId),
                "route", role.name());

        if ("rejected".equals(sResult))
            {
            logRejection("pof", role.name(), currentSubject(), null, sReason);
            }
        }

    /**
     * Record a lambda bytecode check.
     *
     * @param sResult  the check result
     * @param sReason  the check reason
     */
    public static void recordLambdaBytecodeCheck(String sResult, String sReason)
        {
        SerializationRole role = SerializationRole.current();
        String            sMode = modeTag();
        record(METRIC_LAMBDA_BYTECODE_CHECK, sResult, sReason, role, null, null,
                "result", sResult,
                "reason", sReason,
                "mode", sMode,
                "route", role.name());
        }

    /**
     * Record a remote executable policy check.
     * <p>
     * LEGACY shadow checks ({@code result=would_reject}) are keyed by
     * {@code result}, {@code class}, {@code reason}, and {@code role}; the mode
     * is implicit and the class is retained for per-class dry-run diagnostics.
     * Live DEV and PROD checks ({@code result=allowed|rejected}) are keyed by
     * {@code reason}, {@code role}, {@code result}, and {@code mode}; the class
     * is omitted to bound MBean cardinality, and the MBean counter is updated.
     *
     * @param sResult  the check result
     * @param clz      the denied class
     * @param reason   the operation category
     * @param role     the serialization role
     * @param subject  the current subject, if known
     */
    public static void recordExecutablePolicyCheck(String sResult, Class<?> clz, OperationReason reason,
                                                   SerializationRole role, Subject subject)
        {
        String sClass  = clz == null ? "null" : clz.getName();
        String sReason = reason == null ? "-" : reason.name();
        SerializationRole roleResolved = role == null ? SerializationRole.current() : role;
        String sRole = roleResolved.name();
        if ("would_reject".equals(sResult))
            {
            COUNTERS.computeIfAbsent(metricKey(METRIC_EXECUTABLE_POLICY_CHECK_NAME,
                            "result", sResult,
                            "class", sClass,
                            "reason", sReason,
                            "role", sRole),
                    s -> new LongAdder()).increment();
            }
        else
            {
            String sMode = modeTag();
            COUNTERS.computeIfAbsent(metricKey(METRIC_EXECUTABLE_POLICY_CHECK_NAME,
                            "reason", sReason,
                            "role", sRole,
                            "result", sResult,
                            "mode", sMode),
                    s -> new LongAdder()).increment();
            incrementMBeanCounter(new CounterKey(METRIC_EXECUTABLE_POLICY_CHECK, sResult, sReason,
                    roleResolved, sMode, null, null));
            }

        if ("rejected".equals(sResult) || "would_reject".equals(sResult))
            {
            logRejection("remote-executable-policy", sRole, subject, sClass, sReason);
            }
        }

    /**
     * Log a structured rejection.
     *
     * @param sGate        the gate name
     * @param sRoute       the role route
     * @param subject      the subject, if known
     * @param sDeniedClass the denied class, if known
     * @param sReason      the rejection reason
     */
    public static void logRejection(String sGate, String sRoute, Subject subject, String sDeniedClass, String sReason)
        {
        Logger.warn(formatRejection(sGate, sRoute, subject, sDeniedClass, sReason));
        }

    /**
     * Return a snapshot of recorded counters.
     *
     * @return the counter snapshot
     */
    public static Map<String, Long> snapshot()
        {
        Map<String, Long> map = new TreeMap<>();
        COUNTERS.forEach((sKey, counter) -> map.put(sKey, counter.sum()));
        return map;
        }

    /**
     * Register the serialization telemetry MBean with the specified registry.
     *
     * @param registry  the management registry
     */
    public static void register(Registry registry)
        {
        if (registry == null)
            {
            return;
            }

        s_registry = registry;
        }

    /**
     * Reset counters for tests.
     */
    public static void resetForTesting()
        {
        Registry registry = s_registry;
        if (registry != null)
            {
            MBEANS.values().forEach(registration ->
                {
                String sName = registration.getName();
                if (sName != null)
                    {
                    registry.unregister(sName);
                    }
                });
            }

        COUNTERS.clear();
        MBEANS.clear();
        F_OVERFLOW_LOGGED.set(false);
        }

    /**
     * Format a rejection for tests.
     *
     * @param sGate        the gate name
     * @param sRoute       the route
     * @param subject      the subject
     * @param sDeniedClass the denied class
     * @param sReason      the reason
     *
     * @return the formatted message
     */
    public static String formatRejectionForTesting(String sGate, String sRoute, Subject subject,
                                                   String sDeniedClass, String sReason)
        {
        return formatRejection(sGate, sRoute, subject, sDeniedClass, sReason);
        }

    // ----- helper methods ---------------------------------------------------

    private static void record(String sMetric, String sResult, String sReason, SerializationRole role,
                               String sExtraTag, Integer nExtraValue, String... asTags)
        {
        COUNTERS.computeIfAbsent(metricKey(METRIC_PREFIX + sMetric, asTags), s -> new LongAdder()).increment();
        incrementMBeanCounter(new CounterKey(sMetric, sResult, sReason, role, modeTag(), sExtraTag, nExtraValue));
        }

    private static String metricKey(String sMetric, String... asTags)
        {
        StringBuilder sb = new StringBuilder(sMetric).append('{');
        for (int i = 0; i < asTags.length; i += 2)
            {
            if (i > 0)
                {
                sb.append(',');
                }
            sb.append(asTags[i]).append('=').append(asTags[i + 1]);
            }
        return sb.append('}').toString();
        }

    private static void incrementMBeanCounter(CounterKey key)
        {
        Registry registry = s_registry;
        if (registry == null)
            {
            return;
            }

        CounterRegistration registration = MBEANS.get(key);
        if (registration == null)
            {
            CounterKey keyRegister;
            boolean    fRegister;
            synchronized (MBEANS)
                {
                registration = MBEANS.get(key);
                if (registration == null)
                    {
                    keyRegister = key;
                    CounterKey keyOverflow = overflowKey(key.mode());
                    if (!keyOverflow.equals(keyRegister) && MBEANS.size() >= MAX_REGISTERED_TUPLES - 1)
                        {
                        keyRegister = keyOverflow;
                        logOverflow();
                        }

                    registration = MBEANS.get(keyRegister);
                    if (registration == null)
                        {
                        registration = new CounterRegistration(new SerializationGateCounter());
                        MBEANS.put(keyRegister, registration);
                        fRegister = true;
                        }
                    else
                        {
                        fRegister = false;
                        }
                    }
                else
                    {
                    keyRegister = key;
                    fRegister   = false;
                    }
                }

            if (fRegister)
                {
                try
                    {
                    registerCounter(registry, keyRegister, registration);
                    }
                catch (RuntimeException e)
                    {
                    MBEANS.remove(keyRegister, registration);
                    throw e;
                    }
                }
            }

        registration.increment();
        }

    private static void registerCounter(Registry registry, CounterKey key, CounterRegistration registration)
        {
        String sName = registry.ensureGlobalName(key.toMBeanName());
        registry.register(sName, createMBean(registration.getCounter(), key));
        registration.setName(sName);
        }

    private static Object createMBean(SerializationGateCounter counter, CounterKey key)
        {
        try
            {
            switch (key.metric())
                {
                case METRIC_FILTER_CHECK:
                    return new AnnotatedStandardMBean((SerializationFilterCheckMBean) counter,
                            SerializationFilterCheckMBean.class);
                case METRIC_FMT_CHECK:
                    return new AnnotatedStandardMBean((SerializationFmtCheckMBean) counter,
                            SerializationFmtCheckMBean.class);
                case METRIC_POF_CHECK:
                    return new AnnotatedStandardMBean((SerializationPofCheckMBean) counter,
                            SerializationPofCheckMBean.class);
                case METRIC_LAMBDA_BYTECODE_CHECK:
                    return new AnnotatedStandardMBean((SerializationLambdaBytecodeCheckMBean) counter,
                            SerializationLambdaBytecodeCheckMBean.class);
                case METRIC_EXECUTABLE_POLICY_CHECK:
                    return new AnnotatedStandardMBean((SerializationExecutablePolicyCheckMBean) counter,
                            SerializationExecutablePolicyCheckMBean.class);
                default:
                    throw new IllegalArgumentException("Unknown serialization metric " + key.metric());
                }
            }
        catch (NotCompliantMBeanException e)
            {
            throw new IllegalStateException("Unable to create serialization telemetry MBean", e);
            }
        }

    private static void logOverflow()
        {
        if (F_OVERFLOW_LOGGED.compareAndSet(false, true))
            {
            Logger.warn("Serialization telemetry MBean tag cardinality reached " + MAX_REGISTERED_TUPLES
                    + "; recording overflow under reason=tag-ceiling-reached");
            }
        }

    private static String objectNameValue(String sValue)
        {
        String sSafe = value(sValue);
        return sSafe.matches("[A-Za-z0-9_.-]+") ? sSafe : ObjectName.quote(sSafe);
        }

    private static String modeTag()
        {
        return CoherenceMode.current().name().toLowerCase();
        }

    private static CounterKey overflowKey(String sMode)
        {
        return new CounterKey(METRIC_FILTER_CHECK, "rejected", "tag-ceiling-reached",
                SerializationRole.UNCLASSIFIED, sMode, null, null);
        }

    private static String formatRejection(String sGate, String sRoute, Subject subject,
                                          String sDeniedClass, String sReason)
        {
        return String.format("route=%s|gate=%s|principal=%s|denied-class=%s|reason=%s",
                value(sRoute), value(sGate), principal(subject), value(sDeniedClass), value(sReason));
        }

    private static Subject currentSubject()
        {
        try
            {
            return SecurityHelper.getCurrentSubject();
            }
        catch (RuntimeException e)
            {
            return null;
            }
        }

    private static String principal(Subject subject)
        {
        if (subject == null || subject.getPrincipals().isEmpty())
            {
            return "-";
            }

        return bounded(subject.getPrincipals().iterator().next().getName());
        }

    private static String value(String sValue)
        {
        return sValue == null || sValue.isBlank() ? "-" : bounded(sValue);
        }

    private static String bounded(String sValue)
        {
        String sClean = sValue.replaceAll("[^\\p{Print}]", "?");
        return sClean.length() <= 160 ? sClean : sClean.substring(0, 160);
        }

    // ----- inner class: CounterRegistration --------------------------------

    private static class CounterRegistration
        {
        private CounterRegistration(SerializationGateCounter counter)
            {
            f_counter = counter;
            }

        private String getName()
            {
            return m_sName;
            }

        private void setName(String sName)
            {
            m_sName = sName;
            }

        private SerializationGateCounter getCounter()
            {
            return f_counter;
            }

        private void increment()
            {
            f_counter.increment();
            }

        private volatile String m_sName;

        private final SerializationGateCounter f_counter;
        }

    // ----- inner class: CounterKey -----------------------------------------

    private static class CounterKey
        {
        CounterKey(String sMetric, String sResult, String sReason, SerializationRole route, String sMode,
                   String sExtraTagName, Integer nExtraTagValue)
            {
            m_sMetric        = sMetric;
            m_sResult        = sResult;
            m_sReason        = sReason;
            m_route          = route;
            m_sMode          = sMode;
            m_sExtraTagName  = sExtraTagName;
            m_nExtraTagValue = nExtraTagValue;
            }

        String metric()
            {
            return m_sMetric;
            }

        String result()
            {
            return m_sResult;
            }

        String reason()
            {
            return m_sReason;
            }

        SerializationRole route()
            {
            return m_route;
            }

        String mode()
            {
            return m_sMode;
            }

        String extraTagName()
            {
            return m_sExtraTagName;
            }

        Integer extraTagValue()
            {
            return m_nExtraTagValue;
            }

        private String toMBeanName()
            {
            StringBuilder sb = new StringBuilder(MBEAN_NAME)
                    .append(",metric=").append(metric())
                    .append(",route=").append(route().name())
                    .append(",mode=").append(mode())
                    .append(",result=").append(objectNameValue(result()))
                    .append(",reason=").append(objectNameValue(reason()));

            if (extraTagName() != null)
                {
                sb.append(',').append(extraTagName()).append('=').append(extraTagValue());
                }

            return sb.toString();
            }

        @Override
        public boolean equals(Object o)
            {
            if (o == this)
                {
                return true;
                }
            if (!(o instanceof CounterKey))
                {
                return false;
                }
            CounterKey that = (CounterKey) o;
            return Objects.equals(m_sMetric, that.m_sMetric)
                    && Objects.equals(m_sResult, that.m_sResult)
                    && Objects.equals(m_sReason, that.m_sReason)
                    && m_route == that.m_route
                    && Objects.equals(m_sMode, that.m_sMode)
                    && Objects.equals(m_sExtraTagName, that.m_sExtraTagName)
                    && Objects.equals(m_nExtraTagValue, that.m_nExtraTagValue);
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(m_sMetric, m_sResult, m_sReason, m_route, m_sMode, m_sExtraTagName, m_nExtraTagValue);
            }

        private final String            m_sMetric;
        private final String            m_sResult;
        private final String            m_sReason;
        private final SerializationRole m_route;
        private final String            m_sMode;
        private final String            m_sExtraTagName;
        private final Integer           m_nExtraTagValue;
        }

    // ----- constants --------------------------------------------------------

    private static final ConcurrentHashMap<String, LongAdder> COUNTERS = new ConcurrentHashMap<>();

    /**
     * Registered MBean counters.
     */
    private static final ConcurrentHashMap<CounterKey, CounterRegistration> MBEANS = new ConcurrentHashMap<>();

    /**
     * Serialization gates MBean name.
     */
    public static final String MBEAN_NAME = "type=SerializationGates";

    /**
     * The maximum registered tuple count.
     */
    static final int MAX_REGISTERED_TUPLES = 1024;

    private static final String METRIC_FILTER_CHECK = "filter_check";

    private static final String METRIC_FMT_CHECK = "fmt_check";

    private static final String METRIC_POF_CHECK = "pof_check";

    private static final String METRIC_LAMBDA_BYTECODE_CHECK = "lambda_bytecode_check";

    private static final String METRIC_PREFIX = "coh.serialization.";

    private static final String METRIC_EXECUTABLE_POLICY_CHECK = "executable_policy_check";

    private static final String METRIC_EXECUTABLE_POLICY_CHECK_NAME = "coh.executable.policy_check";

    private static final String TAG_FMT = "fmt";

    private static final String TAG_TYPE_ID = "type_id";

    private static final AtomicBoolean F_OVERFLOW_LOGGED = new AtomicBoolean();

    /**
     * The registry used for lazy MBean registration.
     */
    private static volatile Registry s_registry;

    /**
     * Utility class.
     */
    private SerializationTelemetry()
        {
        }
    }
