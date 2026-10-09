/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.internal.util.security.RemoteExecutionMode;

import org.junit.After;
import org.junit.Test;

import java.io.File;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link CoherenceMode}.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.07
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
        setSecurityMode(null);

        assertSame(CoherenceMode.DEV, CoherenceMode.current());
        assertTrue(CoherenceMode.isDev());
        assertFalse(CoherenceMode.isProd());
        assertTrue(CoherenceMode.isSecurityHardeningEnabled());
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
    public void testSecurityModeDefaultsHardenedForReleasedModes()
        {
        assertPredicates(null, true);
        assertPredicates("eval", true);
        assertPredicates("dev", true);
        assertPredicates("development", true);
        assertPredicates("prod", true);
        assertPredicates("production", true);
        }

    @Test
    public void testSecurityModeExplicitCompatibilityDisablesGates()
        {
        assertPredicates(null, false, CoherenceMode.SECURITY_MODE_COMPATIBILITY);
        assertPredicates("eval", false, CoherenceMode.SECURITY_MODE_COMPATIBILITY);
        assertPredicates("dev", false, CoherenceMode.SECURITY_MODE_COMPATIBILITY);
        assertPredicates("development", false, CoherenceMode.SECURITY_MODE_COMPATIBILITY);
        assertPredicates("prod", false, CoherenceMode.SECURITY_MODE_COMPATIBILITY);
        assertPredicates("production", false, CoherenceMode.SECURITY_MODE_COMPATIBILITY);
        }

    @Test
    public void testSecurityModeExplicitHardenedEnablesGates()
        {
        assertPredicates(null, true, CoherenceMode.SECURITY_MODE_HARDENED);
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
        assertInvalidSecurityMode("");
        assertInvalidSecurityMode(" ");
        assertInvalidSecurityMode("legacy");
        assertInvalidSecurityMode("legacy-compatibility");
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
        for (String sOldValue : new String[] {"true", "false"})
            {
            setProperty(OLD_SECURITY_HARDENED_PROPERTY, sOldValue);

            assertPredicates("prod", true);
            assertPredicates("prod", false, CoherenceMode.SECURITY_MODE_COMPATIBILITY);
            assertPredicates("prod", true, CoherenceMode.SECURITY_MODE_HARDENED);
            }
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

    @Test
    public void testMemoizedDefaultSecurityModeIgnoresLaterPropertyChange()
        {
        assertPredicates(null, true);

        setProperty(CoherenceMode.PROP_SECURITY_MODE, CoherenceMode.SECURITY_MODE_COMPATIBILITY);

        assertCurrentPredicates(null, true);
        }

    @Test
    public void testSecurityModesInFreshJvm()
            throws Exception
        {
        for (String sMode : new String[] {null, "eval", "dev", "development", "prod", "production"})
            {
            assertFreshJvm(sMode, null);
            assertFreshJvm(sMode, CoherenceMode.SECURITY_MODE_COMPATIBILITY);
            assertFreshJvm(sMode, CoherenceMode.SECURITY_MODE_HARDENED);
            }
        }

    /**
     * Verify the initial mode resolution in a fresh JVM without resetting or
     * modifying the properties.
     *
     * @param asArgs  the expected runtime, security, and dynamic-remote properties, with empty
     *                strings representing absent properties
     */
    public static void main(String[] asArgs)
        {
        String sMode         = asArgs[0].isEmpty() ? null : asArgs[0];
        String sSecurityMode = asArgs[1].isEmpty() ? null : asArgs[1];

        assertEquals(sMode, System.getProperty(CoherenceMode.PROP_COHERENCE_MODE));
        assertEquals(sSecurityMode, System.getProperty(CoherenceMode.PROP_SECURITY_MODE));
        String sDynamic = asArgs[2].isEmpty() ? null : asArgs[2];
        assertEquals(sDynamic, System.getProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH));
        assertCurrentPredicates(sMode, !CoherenceMode.SECURITY_MODE_COMPATIBILITY.equals(sSecurityMode));
        assertEquals(!"deny".equals(sDynamic), RemoteExecutionMode.isDynamicRemoteAllowed());
        }

    private static void assertFreshJvm(String sMode, String sSecurityMode)
            throws Exception
        {
        for (String sDynamic : new String[] {null, "allow", "deny"})
            {
            assertFreshJvm(sMode, sSecurityMode, sDynamic);
            }
        }

    private static void assertFreshJvm(String sMode, String sSecurityMode, String sDynamic)
            throws Exception
        {
        List<String> listCommand = new ArrayList<>();
        listCommand.add(new File(System.getProperty("java.home"), "bin/java").getAbsolutePath());
        listCommand.add("-cp");
        listCommand.add(System.getProperty("java.class.path"));
        if (sMode != null)
            {
            listCommand.add("-D" + CoherenceMode.PROP_COHERENCE_MODE + "=" + sMode);
            }
        if (sSecurityMode != null)
            {
            listCommand.add("-D" + CoherenceMode.PROP_SECURITY_MODE + "=" + sSecurityMode);
            }
        if (sDynamic != null)
            {
            listCommand.add("-D" + RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH + "=" + sDynamic);
            }
        listCommand.add(CoherenceModeTest.class.getName());
        listCommand.add(sMode == null ? "" : sMode);
        listCommand.add(sSecurityMode == null ? "" : sSecurityMode);
        listCommand.add(sDynamic == null ? "" : sDynamic);

        Path           pathOutput = Files.createTempFile("coherence-mode-", ".log");
        ProcessBuilder builder    = new ProcessBuilder(listCommand)
                .redirectErrorStream(true)
                .redirectOutput(pathOutput.toFile());

        // prevent inherited JVM options from supplying a security mode to the probe
        builder.environment().remove("JAVA_TOOL_OPTIONS");
        builder.environment().remove("JDK_JAVA_OPTIONS");
        builder.environment().remove("_JAVA_OPTIONS");
        builder.environment().remove("COHERENCE_REMOTE_DYNAMIC_UNAUTHENTICATED");
        builder.environment().remove("TANGOSOL_COHERENCE_REMOTE_DYNAMIC_UNAUTHENTICATED");
        builder.environment().remove("TANGOSOL_REMOTE_DYNAMIC_UNAUTHENTICATED");

        Process process = null;
        try
            {
            process = builder.start();
            assertTrue("Mode probe timed out: " + listCommand, process.waitFor(30, TimeUnit.SECONDS));
            assertEquals("Mode probe: " + listCommand + "\n" + Files.readString(pathOutput),
                    0, process.exitValue());
            }
        finally
            {
            if (process != null && process.isAlive())
                {
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
                }
            Files.deleteIfExists(pathOutput);
            }
        }

    private static void assertPredicates(String sMode, boolean fHardened)
        {
        assertPredicates(sMode, fHardened, null);
        }

    private static void assertPredicates(String sMode, boolean fHardened, String sSecurityMode)
        {
        setMode(sMode);
        setSecurityMode(sSecurityMode);

        assertCurrentPredicates(sMode, fHardened);
        }

    private static void assertCurrentPredicates(String sMode, boolean fHardened)
        {
        boolean fDev  = sMode == null || sMode.equals("dev") || sMode.equals("development");
        boolean fProd = "prod".equals(sMode) || "production".equals(sMode);

        assertSame(sMode, fDev ? CoherenceMode.DEV : fProd ? CoherenceMode.PROD : CoherenceMode.EVAL,
                CoherenceMode.current());
        assertEquals(sMode, fDev, CoherenceMode.isDev());
        assertEquals(sMode, fProd, CoherenceMode.isProd());
        assertEquals("hardening:" + sMode, fHardened, CoherenceMode.isSecurityHardeningEnabled());
        assertEquals("allowlist:" + sMode, fHardened, CoherenceMode.isAllowlistEnforced());
        assertFalse("dynamic:" + sMode, CoherenceMode.isDynamicRemoteDefaultDeny());
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
