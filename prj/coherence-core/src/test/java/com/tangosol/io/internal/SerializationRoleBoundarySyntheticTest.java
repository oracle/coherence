/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.io.SerializationRole;

import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Synthetic route-tag regressions for the Slice F.1 boundary matrix.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public class SerializationRoleBoundarySyntheticTest
    {
    @Before
    public void resetTelemetry()
        {
        SerializationTelemetry.resetForTesting();
        CoherenceModeHelper.reset();
        }

    @Test
    public void testEveryConcreteRouteTagsFilterRejection()
        {
        for (SerializationRole role : SerializationRole.values())
            {
            if (role != SerializationRole.TOOLING && role != SerializationRole.UNCLASSIFIED)
                {
                assertFilterRoute(role);
                }
            }
        }

    @Test
    public void testRepresentativeCounterFamiliesKeepRoute()
        {
        assertMetricRoute(SerializationRole.CLUSTER,
                () -> SerializationTelemetry.recordFmtCheck("rejected", "invalid-format", 255),
                "coh.serialization.fmt_check{result=rejected,reason=invalid-format,mode="
                        + mode() + ",fmt=255,route=CLUSTER}");
        assertMetricRoute(SerializationRole.EXTEND_PROXY,
                () -> SerializationTelemetry.recordPofCheck("rejected", "unknown-type", 999),
                "coh.serialization.pof_check{result=rejected,reason=unknown-type,mode="
                        + mode() + ",type_id=999,route=EXTEND_PROXY}");
        assertMetricRoute(SerializationRole.GRPC,
                () -> SerializationTelemetry.recordLambdaBytecodeCheck("rejected", "class-name-on-denylist"),
                "coh.serialization.lambda_bytecode_check{result=rejected,reason=class-name-on-denylist,mode="
                        + mode() + ",route=GRPC}");
        }

    private static void assertFilterRoute(SerializationRole role)
        {
        SerializationTelemetry.resetForTesting();
        CoherenceModeHelper.reset();
        assertMetricRoute(role, () -> SerializationTelemetry.recordFilterCheck("rejected", "synthetic",
                        Runtime.class, null),
                "coh.serialization.filter_check{result=rejected,reason=synthetic,mode="
                        + mode() + ",route=" + role.name() + ",principal=-}");
        }

    private static void assertMetricRoute(SerializationRole role, Runnable runnable, String sKey)
        {
        try (SerializationRole.Scope ignored = SerializationRole.setAndClose(role))
            {
            runnable.run();
            }

        Map<String, Long> map = SerializationTelemetry.snapshot();
        assertTrue("missing counter " + sKey + " in " + map, map.containsKey(sKey));
        assertEquals(Long.valueOf(1), map.get(sKey));
        assertEquals(SerializationRole.UNCLASSIFIED, SerializationRole.current());
        }

    private static String mode()
        {
        return CoherenceMode.current().name().toLowerCase(Locale.ROOT);
        }
    }
