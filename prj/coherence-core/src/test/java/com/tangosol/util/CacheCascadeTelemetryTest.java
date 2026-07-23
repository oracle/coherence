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
import com.tangosol.util.filter.AllFilter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Unit tests for per-class telemetry emitted by cache install cascades.
 *
 * @author Aleks Seovic  2026.05.12
 * @since 26.04
 */
public class CacheCascadeTelemetryTest
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
    public void compositeAggregatorEmitsOnePolicyEventPerClass()
        {
        RemoteInstallGate.enforceCacheAggregatorInstall(new CompositeAggregator<>(new InvocableMap.EntryAggregator[]
                {
                new CacheNamedCacheInstallGateTest.AnnotatedAggregator(),
                new CacheNamedCacheInstallGateTest.AnnotatedAggregator()
                }), SerializationRole.EXTEND_PROXY, null);

        assertCounter(OperationReason.AGGREGATE, "allowed", 3L);
        assertCounterAbsent(OperationReason.AGGREGATE, "rejected");
        }

    @Test
    public void arrayFilterEmitsOnePolicyEventPerClass()
        {
        RemoteInstallGate.enforceCacheFilterInstall(new AllFilter(new Filter<?>[]
                {
                new CacheNamedCacheInstallGateTest.AnnotatedFilter(),
                new CacheNamedCacheInstallGateTest.AnnotatedFilter()
                }), SerializationRole.EXTEND_PROXY, null);

        assertCounter(OperationReason.EVALUATE_FILTER, "allowed", 3L);
        assertCounterAbsent(OperationReason.EVALUATE_FILTER, "rejected");
        }

    private static void assertCounter(OperationReason reason, String sResult, long cExpected)
        {
        String sKey = key(reason, sResult);
        assertEquals("counter " + sKey + " in " + SerializationTelemetry.snapshot(),
                Long.valueOf(cExpected), SerializationTelemetry.snapshot().get(sKey));
        }

    private static void assertCounterAbsent(OperationReason reason, String sResult)
        {
        String sKey = key(reason, sResult);
        assertFalse("counter " + sKey + " in " + SerializationTelemetry.snapshot(),
                SerializationTelemetry.snapshot().containsKey(sKey));
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
    }
