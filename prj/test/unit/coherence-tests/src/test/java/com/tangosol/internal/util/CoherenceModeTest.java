/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, m_sOldMode);
        restoreProperty(CoherenceMode.PROP_SECURITY_MODE, m_sOldSecurityMode);
        restoreProperty(OLD_SECURITY_HARDENED_PROPERTY, m_sOldSecurityHardened);
        CoherenceMode.resetForTesting();
        }

    @Test
    public void testDefaultModeIsDev()
        {
        setMode(null);

        assertSame(CoherenceMode.DEV, CoherenceMode.current());
        assertTrue(CoherenceMode.isDev());
        assertFalse(CoherenceMode.isProd());
        assertFalse(CoherenceMode.isSecurityHardeningEnabled());
        }

    @Test
    public void testExplicitEval()
        {
        setMode("eval");

        assertSame(CoherenceMode.EVAL, CoherenceMode.current());
        assertFalse(CoherenceMode.isDev());
        assertFalse(CoherenceMode.isProd());
        }

    @Test
    public void testExplicitDev()
        {
        setMode("dev");

        assertSame(CoherenceMode.DEV, CoherenceMode.current());
        assertTrue(CoherenceMode.isDev());
        assertFalse(CoherenceMode.isProd());
        }

    @Test
    public void testExplicitDevelopment()
        {
        setMode("development");

        assertSame(CoherenceMode.DEV, CoherenceMode.current());
        assertTrue(CoherenceMode.isDev());
        assertFalse(CoherenceMode.isProd());
        }

    @Test
    public void testExplicitProd()
        {
        setMode("prod");

        assertSame(CoherenceMode.PROD, CoherenceMode.current());
        assertFalse(CoherenceMode.isDev());
        assertTrue(CoherenceMode.isProd());
        }

    @Test
    public void testExplicitProduction()
        {
        setMode("production");

        assertSame(CoherenceMode.PROD, CoherenceMode.current());
        assertFalse(CoherenceMode.isDev());
        assertTrue(CoherenceMode.isProd());
        }

    @Test
    public void testModesAreCaseInsensitive()
        {
        setMode("EVAL");
        assertSame(CoherenceMode.EVAL, CoherenceMode.current());

        setMode("DEV");
        assertSame(CoherenceMode.DEV, CoherenceMode.current());

        setMode("PROD");
        assertSame(CoherenceMode.PROD, CoherenceMode.current());
        }

    @Test
    public void testLegacyModeIsRejected()
        {
        assertInvalidMode("legacy");
        assertInvalidMode("LEGACY");
        assertInvalidMode("legacy-compatibility");
        }

    @Test
    public void testBlankModeIsRejected()
        {
        assertInvalidMode(" ");
        }

    @Test
    public void testMalformedModeIsRejected()
        {
        assertInvalidMode("hybrid");
        }

    @Test
    public void testMemoizedModeIgnoresLaterPropertyChange()
        {
        setMode("dev");

        assertSame(CoherenceMode.DEV, CoherenceMode.current());

        setProperty(CoherenceMode.PROP_COHERENCE_MODE, "prod");

        assertSame(CoherenceMode.DEV, CoherenceMode.current());
        assertTrue(CoherenceMode.isDev());
        assertFalse(CoherenceMode.isProd());
        }

    @Test
    public void testSecurityModeDefaultsCompatibilityForReleasedModes()
        {
        assertPredicates("eval", false);
        assertPredicates("dev", false);
        assertPredicates("development", false);
        assertPredicates("prod", false);
        assertPredicates("production", false);
        }

    @Test
    public void testSecurityModeExplicitCompatibilityDisablesGates()
        {
        assertPredicates("eval", false, CoherenceMode.SECURITY_MODE_COMPATIBILITY);
        assertPredicates("dev", false, CoherenceMode.SECURITY_MODE_COMPATIBILITY);
        assertPredicates("development", false, CoherenceMode.SECURITY_MODE_COMPATIBILITY);
        assertPredicates("prod", false, CoherenceMode.SECURITY_MODE_COMPATIBILITY);
        assertPredicates("production", false, CoherenceMode.SECURITY_MODE_COMPATIBILITY);
        }

    @Test
    public void testSecurityModeExplicitHardenedEnablesGates()
        {
        assertPredicates("eval", true, CoherenceMode.SECURITY_MODE_HARDENED);
        assertPredicates("dev", true, CoherenceMode.SECURITY_MODE_HARDENED);
        assertPredicates("development", true, CoherenceMode.SECURITY_MODE_HARDENED);
        assertPredicates("prod", true, CoherenceMode.SECURITY_MODE_HARDENED);
        assertPredicates("production", true, CoherenceMode.SECURITY_MODE_HARDENED);
        }

    @Test
    public void testSecurityModeIsTrimmedAndCaseInsensitive()
        {
        assertPredicates("prod", false, " COMPATIBILITY ");
        assertPredicates("prod", true, " HaRdEnEd ");
        }

    @Test
    public void testSecurityModeRejectsBlankUnknownAndBooleanAliases()
        {
        assertInvalidSecurityMode(" ");
        assertInvalidSecurityMode("unknown");
        assertInvalidSecurityMode("compat");
        assertInvalidSecurityMode("true");
        assertInvalidSecurityMode("false");
        assertInvalidSecurityMode("on");
        assertInvalidSecurityMode("off");
        assertInvalidSecurityMode("enabled");
        assertInvalidSecurityMode("disabled");
        assertInvalidSecurityMode("yes");
        assertInvalidSecurityMode("no");
        }

    @Test
    public void testSecurityModeHardenedCanBeEnabledWithoutExplicitRuntimeMode()
        {
        setMode(null);
        setSecurityMode(CoherenceMode.SECURITY_MODE_HARDENED);

        assertSame(CoherenceMode.DEV, CoherenceMode.current());
        assertTrue(CoherenceMode.isSecurityHardeningEnabled());
        assertTrue(CoherenceMode.isAllowlistEnforced());
        }

    @Test
    public void testOldSecurityHardenedPropertyIsNotAlias()
        {
        setMode("prod");
        setSecurityMode(null);
        setProperty(OLD_SECURITY_HARDENED_PROPERTY, "true");
        CoherenceMode.resetForTesting();

        assertFalse(CoherenceMode.isSecurityHardeningEnabled());
        assertFalse(CoherenceMode.isAllowlistEnforced());
        }

    @Test
    public void testMemoizedSecurityModeIgnoresLaterPropertyChange()
        {
        setMode("prod");
        setSecurityMode(CoherenceMode.SECURITY_MODE_COMPATIBILITY);

        assertFalse(CoherenceMode.isSecurityHardeningEnabled());

        setProperty(CoherenceMode.PROP_SECURITY_MODE, CoherenceMode.SECURITY_MODE_HARDENED);

        assertFalse(CoherenceMode.isSecurityHardeningEnabled());
        }

    private static void assertPredicates(String sMode, boolean fHardened)
        {
        assertPredicates(sMode, fHardened, null);
        }

    private static void assertPredicates(String sMode, boolean fHardened, String sSecurityMode)
        {
        setMode(sMode);
        setSecurityMode(sSecurityMode);

        assertEquals(sMode, sMode.equals("dev") || sMode.equals("development"), CoherenceMode.isDev());
        assertEquals("hardening:" + sMode, fHardened, CoherenceMode.isSecurityHardeningEnabled());
        assertEquals("allowlist:" + sMode, fHardened, CoherenceMode.isAllowlistEnforced());
        assertEquals("dynamic:" + sMode, fHardened, CoherenceMode.isDynamicRemoteDefaultDeny());
        assertEquals("executable:" + sMode, fHardened, CoherenceMode.isRemoteExecutableEnforced());
        assertEquals("rest-auth:" + sMode, fHardened, CoherenceMode.isCoherenceRestAuthEnforced());
        assertEquals("rest-passthrough:" + sMode, fHardened,
                CoherenceMode.isCoherenceRestPassThroughAllowlistRequired());
        assertEquals("xml-xxe:" + sMode, fHardened,
                CoherenceMode.isXmlExternalEntityProtectionRequired());
        assertEquals("public-hardening:" + sMode, fHardened,
                com.tangosol.util.CoherenceMode.isSecurityHardeningEnabled());
        assertEquals("public-rest-auth:" + sMode, fHardened,
                com.tangosol.util.CoherenceMode.isCoherenceRestAuthEnforced());
        assertEquals("public-rest-passthrough:" + sMode, fHardened,
                com.tangosol.util.CoherenceMode.isCoherenceRestPassThroughAllowlistRequired());
        }

    private static void assertInvalidMode(String sMode)
        {
        setMode(sMode);
        try
            {
            CoherenceMode.current();
            fail("Expected invalid mode " + sMode);
            }
        catch (IllegalArgumentException e)
            {
            assertEquals("Invalid mode " + sMode, e.getMessage());
            }
        }

    private static void assertInvalidSecurityMode(String sSecurityMode)
        {
        setSecurityMode(sSecurityMode);
        try
            {
            CoherenceMode.isSecurityHardeningEnabled();
            fail("Expected invalid security mode " + sSecurityMode);
            }
        catch (IllegalArgumentException e)
            {
            assertEquals("Invalid security mode " + sSecurityMode, e.getMessage());
            }
        }

    private static void setMode(String sMode)
        {
        setProperty(CoherenceMode.PROP_COHERENCE_MODE, sMode);
        CoherenceMode.resetForTesting();
        }

    private static void setSecurityMode(String sValue)
        {
        setProperty(CoherenceMode.PROP_SECURITY_MODE, sValue);
        CoherenceMode.resetForTesting();
        }

    private static void restoreProperty(String sName, String sValue)
        {
        setProperty(sName, sValue);
        }

    private static void setProperty(String sName, String sValue)
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

    private static final String OLD_SECURITY_HARDENED_PROPERTY = "coherence.security.hardened";

    private final String m_sOldMode = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);

    private final String m_sOldSecurityMode = System.getProperty(CoherenceMode.PROP_SECURITY_MODE);

    private final String m_sOldSecurityHardened = System.getProperty(OLD_SECURITY_HARDENED_PROPERTY);
    }
