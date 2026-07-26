/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import com.tangosol.io.internal.SerializationTelemetry;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link GrpcSerializerPolicy}.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.04
 */
class GrpcSerializerPolicyTest
    {
    @BeforeEach
    void reset()
        {
        m_sAllowedOld      = System.getProperty(GrpcSerializerPolicy.PROP_ALLOWED_SERIALIZERS);
        m_sSecurityModeOld = System.getProperty(PROP_SECURITY_MODE);
        SerializationTelemetry.resetForTesting();
        clearMode();
        clearHardening();
        System.clearProperty(GrpcSerializerPolicy.PROP_ALLOWED_SERIALIZERS);
        }

    @AfterEach
    void restore()
        {
        restoreProperty(GrpcSerializerPolicy.PROP_ALLOWED_SERIALIZERS, m_sAllowedOld);
        restoreProperty(PROP_SECURITY_MODE, m_sSecurityModeOld);
        clearMode();
        SerializationTelemetry.resetForTesting();
        }

    @Test
    void shouldAllowDefaultPofAndJsonInProd()
        {
        try (ModeScope ignored = mode("prod"))
            {
            assertDoesNotThrow(() -> GrpcSerializerPolicy.validateClientFormat(""));
            assertDoesNotThrow(() -> GrpcSerializerPolicy.validateClientFormat("pof"));
            assertDoesNotThrow(() -> GrpcSerializerPolicy.validateClientFormat("json"));
            assertDoesNotThrow(() -> GrpcSerializerPolicy.validateClientFormat("genson"));
            assertDoesNotThrow(() -> GrpcSerializerPolicy.validateClientFormat(
                    "com.oracle.coherence.io.json.JsonSerializer"));
            }
        }

    @Test
    void shouldRejectUnknownFormatInDevAndProd()
        {
        assertRejected("dev", "custom-format");
        assertRejected("prod", "custom-format");
        }

    @Test
    void shouldRejectUnsafeFormatsInDevAndProd()
        {
        assertRejected("dev", "java");
        assertRejected("prod", "java");
        assertRejected("dev", "DefaultSerializer");
        assertRejected("prod", "DefaultSerializer");
        }

    @Test
    void shouldAllowExplicitlyAllowlistedFormatsInProd()
        {
        System.setProperty(GrpcSerializerPolicy.PROP_ALLOWED_SERIALIZERS, "java,json,custom-format");

        try (ModeScope ignored = mode("prod"))
            {
            assertDoesNotThrow(() -> GrpcSerializerPolicy.validateClientFormat("java"));
            assertDoesNotThrow(() -> GrpcSerializerPolicy.validateClientFormat("json"));
            assertDoesNotThrow(() -> GrpcSerializerPolicy.validateClientFormat("custom-format"));
            }
        }

    @Test
    void shouldAllowButRecordWouldRejectWhenHardeningDisabled()
        {
        setSecurityMode("compatibility");
        try (ModeScope ignored = mode("prod"))
            {
            assertDoesNotThrow(() -> GrpcSerializerPolicy.validateClientFormat("java"));
            assertDoesNotThrow(() -> GrpcSerializerPolicy.validateClientFormat("custom-format"));
            }

        Map<String, Long> map = SerializationTelemetry.snapshot();
        assertCounter(map, "coh.serialization.serializer_check{result=would_reject,reason="
                + GrpcSerializerPolicy.REASON_UNSAFE + ",mode=prod,route=GRPC}");
        assertCounter(map, "coh.serialization.serializer_check{result=would_reject,reason="
                + GrpcSerializerPolicy.REASON_NOT_ALLOWED + ",mode=prod,route=GRPC}");
        }

    private static void assertRejected(String sMode, String sFormat)
        {
        setSecurityMode("hardened");
        try (ModeScope ignored = mode(sMode))
            {
            StatusRuntimeException e = assertThrows(StatusRuntimeException.class,
                    () -> GrpcSerializerPolicy.validateClientFormat(sFormat));
            assertEquals(Status.INVALID_ARGUMENT.getCode(), e.getStatus().getCode());
            assertEquals("invalid request format, client-selected serializer is not allowed",
                    e.getStatus().getDescription());
            }
        }

    private static ModeScope mode(String sMode)
        {
        String sPrevious = System.getProperty(PROP_COHERENCE_MODE);
        restoreProperty(PROP_COHERENCE_MODE, sMode);
        resetMode();
        return new ModeScope(sPrevious);
        }

    private static void clearMode()
        {
        System.clearProperty(PROP_COHERENCE_MODE);
        resetMode();
        }

    private static void clearHardening()
        {
        System.clearProperty(PROP_SECURITY_MODE);
        resetMode();
        }

    private static void setSecurityMode(String sSecurityMode)
        {
        System.setProperty(PROP_SECURITY_MODE, sSecurityMode);
        resetMode();
        }

    private static void resetMode()
        {
        try
            {
            Method method = Class.forName(COHERENCE_MODE_CLASS).getDeclaredMethod("resetForTesting");
            method.setAccessible(true);
            method.invoke(null);
            }
        catch (ReflectiveOperationException | RuntimeException e)
            {
            throw new IllegalStateException("Unable to reset memoized Coherence mode", e);
            }
        }

    private static void assertCounter(Map<String, Long> map, String sKey)
        {
        assertTrue(map.containsKey(sKey), "missing counter " + sKey + " in " + map);
        assertEquals(1L, map.get(sKey));
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

    private static final String PROP_COHERENCE_MODE = "coherence.mode";

    private static final String PROP_SECURITY_MODE = "coherence.security.mode";

    private static final String COHERENCE_MODE_CLASS = "com.tangosol.internal.util.CoherenceMode";

    private String m_sAllowedOld;

    private String m_sSecurityModeOld;

    private static class ModeScope
            implements AutoCloseable
        {
        private ModeScope(String sPrevious)
            {
            previous = sPrevious;
            }

        @Override
        public void close()
            {
            restoreProperty(PROP_COHERENCE_MODE, previous);
            resetMode();
            }

        private final String previous;
        }
    }
