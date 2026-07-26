/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link CoherenceMode}.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public class CoherenceModeTest
    {
    @After
    public void cleanup()
        {
        restoreProperty(m_sOldMode);
        CoherenceMode.resetForTesting();
        }

    @Test
    @SuppressWarnings("removal")
    public void testDefaultIsLegacy()
        {
        restoreProperty(null);

        assertSame(CoherenceMode.LEGACY, CoherenceMode.current());
        assertFalse(CoherenceMode.isDev());
        assertFalse(CoherenceMode.isProd());
        assertTrue(CoherenceMode.isLegacy());
        }

    @Test
    public void testExplicitProd()
        {
        restoreProperty("prod");

        assertSame(CoherenceMode.PROD, CoherenceMode.current());
        assertFalse(CoherenceMode.isDev());
        assertTrue(CoherenceMode.isProd());
        assertFalse(CoherenceMode.isLegacy());
        }

    @Test
    public void testExplicitDev()
        {
        restoreProperty("dev");

        assertSame(CoherenceMode.DEV, CoherenceMode.current());
        assertTrue(CoherenceMode.isDev());
        assertFalse(CoherenceMode.isProd());
        assertFalse(CoherenceMode.isLegacy());
        }

    @Test
    public void testCaseInsensitiveDev()
        {
        restoreProperty("DEV");

        assertSame(CoherenceMode.DEV, CoherenceMode.current());
        }

    @Test
    @SuppressWarnings("removal")
    public void testExplicitLegacy()
        {
        restoreProperty("legacy");

        assertSame(CoherenceMode.LEGACY, CoherenceMode.current());
        assertFalse(CoherenceMode.isDev());
        assertFalse(CoherenceMode.isProd());
        assertTrue(CoherenceMode.isLegacy());
        }

    @Test
    @SuppressWarnings("removal")
    public void testCaseInsensitiveLegacy()
        {
        restoreProperty("LEGACY");

        assertSame(CoherenceMode.LEGACY, CoherenceMode.current());
        }

    @Test
    @SuppressWarnings("removal")
    public void testLegacyCompatibilityAlias()
        {
        restoreProperty("legacy-compatibility");

        assertSame(CoherenceMode.LEGACY, CoherenceMode.current());
        }

    @Test
    @SuppressWarnings("removal")
    public void testBlankModeDefaultsToLegacy()
        {
        restoreProperty(" ");

        assertSame(CoherenceMode.LEGACY, CoherenceMode.current());
        }

    @Test
    @SuppressWarnings("removal")
    public void testMalformedModeDefaultsToLegacy()
        {
        restoreProperty("hybrid");

        assertSame(CoherenceMode.LEGACY, CoherenceMode.current());
        assertTrue(CoherenceMode.isLegacy());
        }

    @Test
    public void testMemoizedModeIgnoresLaterPropertyChange()
        {
        restoreProperty("dev");

        assertSame(CoherenceMode.DEV, CoherenceMode.current());

        setProperty("prod");

        assertSame(CoherenceMode.DEV, CoherenceMode.current());
        assertTrue(CoherenceMode.isDev());
        assertFalse(CoherenceMode.isProd());
        }

    @Test
    public void testModePredicates()
        {
        assertPredicates("dev", true, true, false, true);
        assertPredicates("prod", false, true, true, true);
        assertPredicates("legacy", false, false, false, false);
        }

    @Test
    public void testModeExclusivity()
        {
        assertExactlyOneMode("dev");
        assertExactlyOneMode("prod");
        assertExactlyOneMode("legacy");
        }

    @Test
    public void testLegacyBannerLogsOnce()
        {
        restoreProperty("legacy");
        List<String> listWarnings = new ArrayList<>();
        CoherenceMode.setWarningLoggerForTesting(listWarnings::add);

        CoherenceMode.current();
        CoherenceMode.current();

        assertEquals(List.of(CoherenceMode.LEGACY_WARNING), listWarnings);
        }

    private static void assertPredicates(String sMode, boolean fDev, boolean fAllowlist,
                                         boolean fDynamicRemoteDeny, boolean fExecutable)
        {
        restoreProperty(sMode);

        assertEquals(fDev, CoherenceMode.isDev());
        assertEquals("allowlist:" + sMode, fAllowlist, CoherenceMode.isAllowlistEnforced());
        assertEquals("dynamic:" + sMode, fDynamicRemoteDeny, CoherenceMode.isDynamicRemoteDefaultDeny());
        assertEquals("executable:" + sMode, fExecutable, CoherenceMode.isRemoteExecutableEnforced());
        // rest-01 prompts 02 and 05 add mode predicates that intentionally follow the SER-01 allowlist boundary
        assertEquals("rest-auth:" + sMode, fAllowlist, CoherenceMode.isCoherenceRestAuthEnforced());
        assertEquals("rest-passthrough:" + sMode, fAllowlist,
                CoherenceMode.isCoherenceRestPassThroughAllowlistRequired());
        assertEquals("xml-xxe:" + sMode, fAllowlist,
                CoherenceMode.isXmlExternalEntityProtectionRequired());
        // rest-01 osgi mode import plan: prove the exported REST facade follows the internal source of truth
        assertEquals("public-legacy:" + sMode, !fAllowlist, com.tangosol.util.CoherenceMode.isLegacy());
        assertEquals("public-rest-auth:" + sMode, fAllowlist,
                com.tangosol.util.CoherenceMode.isCoherenceRestAuthEnforced());
        assertEquals("public-rest-passthrough:" + sMode, fAllowlist,
                com.tangosol.util.CoherenceMode.isCoherenceRestPassThroughAllowlistRequired());
        }

    private static void assertExactlyOneMode(String sMode)
        {
        restoreProperty(sMode);

        int cModes = (CoherenceMode.isDev() ? 1 : 0)
                + (CoherenceMode.isProd() ? 1 : 0)
                + (CoherenceMode.isLegacy() ? 1 : 0);
        assertEquals(sMode, 1, cModes);
        }

    private static void restoreProperty(String sMode)
        {
        if (sMode == null)
            {
            System.clearProperty(CoherenceMode.PROP_COHERENCE_MODE);
            }
        else
            {
            System.setProperty(CoherenceMode.PROP_COHERENCE_MODE, sMode);
            }
        CoherenceMode.resetForTesting();
        }

    private static void setProperty(String sMode)
        {
        if (sMode == null)
            {
            System.clearProperty(CoherenceMode.PROP_COHERENCE_MODE);
            }
        else
            {
            System.setProperty(CoherenceMode.PROP_COHERENCE_MODE, sMode);
            }
        }

    private final String m_sOldMode = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
    }
