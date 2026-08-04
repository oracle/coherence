/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import com.tangosol.net.grpc.GrpcDiagnosticsPolicy;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link GrpcDiagnosticsPolicy}.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.07
 */
class GrpcDiagnosticsPolicyTest
    {
    @Test
    void shouldNormalizeChannelz()
        {
        assertEquals(GrpcDiagnosticsPolicy.CHANNELZ_AUTO, GrpcDiagnosticsPolicy.normalizeChannelz(null));
        assertEquals(GrpcDiagnosticsPolicy.CHANNELZ_AUTO, GrpcDiagnosticsPolicy.normalizeChannelz(" "));
        assertEquals(GrpcDiagnosticsPolicy.CHANNELZ_ENABLED, GrpcDiagnosticsPolicy.normalizeChannelz(" Enabled "));
        assertEquals(GrpcDiagnosticsPolicy.CHANNELZ_DISABLED, GrpcDiagnosticsPolicy.normalizeChannelz("DISABLED"));
        assertThrows(IllegalArgumentException.class, () -> GrpcDiagnosticsPolicy.normalizeChannelz("on"));
        }

    @Test
    void shouldResolveChannelzAutoByHardening()
        {
        try (ModeScope ignored = scope("prod", null))
            {
            assertTrue(GrpcDiagnosticsPolicy.isChannelzEnabled(GrpcDiagnosticsPolicy.CHANNELZ_AUTO));
            }
        try (ModeScope ignored = scope("dev", SECURITY_MODE_HARDENED))
            {
            assertFalse(GrpcDiagnosticsPolicy.isChannelzEnabled(GrpcDiagnosticsPolicy.CHANNELZ_AUTO));
            assertTrue(GrpcDiagnosticsPolicy.isChannelzEnabled(GrpcDiagnosticsPolicy.CHANNELZ_ENABLED));
            assertFalse(GrpcDiagnosticsPolicy.isChannelzEnabled(GrpcDiagnosticsPolicy.CHANNELZ_DISABLED));
            }
        }

    @Test
    void shouldNormalizeErrorDisclosure()
        {
        assertEquals(GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_AUTO, GrpcDiagnosticsPolicy.normalizeErrorDisclosure(null));
        assertEquals(GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_AUTO, GrpcDiagnosticsPolicy.normalizeErrorDisclosure(" "));
        assertEquals(GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_SAFE, GrpcDiagnosticsPolicy.normalizeErrorDisclosure(" Safe "));
        assertEquals(GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_DIAGNOSTIC, GrpcDiagnosticsPolicy.normalizeErrorDisclosure("DIAGNOSTIC"));
        assertThrows(IllegalArgumentException.class, () -> GrpcDiagnosticsPolicy.normalizeErrorDisclosure("verbose"));
        }

    @Test
    void shouldResolveErrorDisclosureAutoByHardening()
        {
        try (ModeScope ignored = scope("prod", null))
            {
            assertFalse(GrpcDiagnosticsPolicy.isErrorDisclosureSafe(GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_AUTO));
            }
        try (ModeScope ignored = scope("dev", SECURITY_MODE_HARDENED))
            {
            assertTrue(GrpcDiagnosticsPolicy.isErrorDisclosureSafe(GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_AUTO));
            assertTrue(GrpcDiagnosticsPolicy.isErrorDisclosureSafe(GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_SAFE));
            assertFalse(GrpcDiagnosticsPolicy.isErrorDisclosureSafe(GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_DIAGNOSTIC));
            }
        }

    private static ModeScope scope(String sMode, String sSecurityMode)
        {
        String sPreviousMode         = System.getProperty(PROP_COHERENCE_MODE);
        String sPreviousSecurityMode = System.getProperty(PROP_SECURITY_MODE);
        restoreProperty(PROP_COHERENCE_MODE, sMode);
        restoreProperty(PROP_SECURITY_MODE, sSecurityMode);
        resetMode();
        return new ModeScope(sPreviousMode, sPreviousSecurityMode);
        }

    private static final String SECURITY_MODE_HARDENED = "hardened";

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

    private record ModeScope(String previousMode, String previousSecurityMode)
            implements AutoCloseable
        {
        @Override
        public void close()
            {
            restoreProperty(PROP_COHERENCE_MODE, previousMode);
            restoreProperty(PROP_SECURITY_MODE, previousSecurityMode);
            resetMode();
            }
        }
    }
