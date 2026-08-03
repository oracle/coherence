/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * LEGACY shadow coverage for processor operation reasons.
 * <p>
 * This synthetic test stands in for the slice-plan tests
 * {@code legacyMipShadowsClassEnforce} and
 * {@code legacySpShadowsClassEnforce}; after MIP and SP became
 * {@code final} and {@code @Remote.Executable}, those per-class names are no
 * longer attainable through the built-in processors themselves.
 *
 * @author Aleks Seovic  2026.05.08
 * @since 26.04
 */
public class LegacyShadowExecutablePolicyTest
    {
    @Before
    public void reset()
        {
        SerializationTelemetry.resetForTesting();
        RemoteExecutablePolicy.resetForTesting();
        }

    @After
    public void cleanup()
        {
        CoherenceModeHelper.clear();
        SerializationTelemetry.resetForTesting();
        RemoteExecutablePolicy.resetForTesting();
        }

    @Test
    public void legacyShadowsProcessEntryClassEnforce()
        {
        assertLegacyWouldReject(OperationReason.PROCESS_ENTRY);
        }

    @Test
    public void legacyShadowsScriptEvalClassEnforce()
        {
        assertLegacyWouldReject(OperationReason.SCRIPT_EVAL);
        }

    private static void assertLegacyWouldReject(OperationReason reason)
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.legacy())
            {
            RemoteExecutablePolicy.current().enforce(NonAllowlisted.class, reason,
                    SerializationRole.UNCLASSIFIED, null);
            }

        String sKey = "coh.executable.policy_check{result=would_reject,class="
                + NonAllowlisted.class.getName()
                + ",reason=" + reason.name()
                + ",role=" + SerializationRole.UNCLASSIFIED.name() + "}";
        Map<String, Long> map = SerializationTelemetry.snapshot();
        assertEquals("counter " + sKey + " in " + map, Long.valueOf(1L), map.get(sKey));
        assertFalse(map.keySet().stream()
                .anyMatch(s -> s.contains("result=would_reject") && s.contains("sub_reason")));
        }

    public static class NonAllowlisted
            implements InvocableMap.EntryProcessor<Object, Object, Object>
        {
        @Override
        public Object process(InvocableMap.Entry<Object, Object> entry)
            {
            return null;
            }
        }
    }
