/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.security;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.io.internal.ClassIdentityAllowlist;
import com.tangosol.io.internal.SerializationAllowlist;
import com.tangosol.io.internal.SerializationTelemetry;
import com.tangosol.io.pof.ConfigurablePofContext;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.net.URL;
import java.net.URLClassLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for runtime {@link SecurityConfig} consumers.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public class SecurityConfigIntegrationTest
    {
    @Rule
    public TemporaryFolder m_folder = new TemporaryFolder();

    @After
    public void cleanup()
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, m_sModeOld);
        Thread.currentThread().setContextClassLoader(m_loaderOld);
        SecurityConfig.resetForTesting();
        SerializationTelemetry.resetForTesting();
        CoherenceModeHelper.reset();
        }

    @Test
    public void shouldRejectApplicationClassNameMissingFromSecurityConfig()
            throws Exception
        {
        setMode("prod");
        withConfig();
        SerializationTelemetry.resetForTesting();

        LambdaBytecodeGate.Result result = LambdaBytecodeGate.checkClassName("com.example.Missing",
                LambdaBytecodeGate.Site.STATIC_LAMBDA);

        assertTrue(result instanceof LambdaBytecodeGate.Result.Rejected);
        LambdaBytecodeGate.Result.Rejected rejected = (LambdaBytecodeGate.Result.Rejected) result;
        assertEquals(LambdaBytecodeGate.REASON_SECURITY_CONFIG_MISSING, rejected.reason());

        Map<String, Long> map = SerializationTelemetry.snapshot();
        assertTrue(map.containsKey("coh.serialization.lambda_bytecode_check{result=rejected,"
                + "reason=security-config-missing,mode=" + mode() + ",route=UNCLASSIFIED,site=static_lambda}"));
        }

    @Test
    public void shouldShadowApplicationClassNameMissingFromSecurityConfigInLegacyMode()
            throws Exception
        {
        setMode("legacy");
        withConfig();
        SerializationTelemetry.resetForTesting();

        LambdaBytecodeGate.Result result = LambdaBytecodeGate.checkClassName("com.example.Missing",
                LambdaBytecodeGate.Site.STATIC_LAMBDA);

        assertTrue(result instanceof LambdaBytecodeGate.Result.Rejected);
        LambdaBytecodeGate.ensureAllowed(result, LambdaBytecodeGate.Site.STATIC_LAMBDA);

        Map<String, Long> map = SerializationTelemetry.snapshot();
        assertTrue(map.containsKey("coh.serialization.lambda_bytecode_check{result=would_reject,"
                + "reason=security-config-missing,mode=legacy,route=UNCLASSIFIED,site=static_lambda}"));
        }

    @Test
    public void shouldRejectApplicationClassNameMissingFromSecurityConfigInDevMode()
            throws Exception
        {
        setMode("dev");
        withConfig();
        SerializationTelemetry.resetForTesting();

        LambdaBytecodeGate.Result result = LambdaBytecodeGate.checkClassName("com.example.Missing",
                LambdaBytecodeGate.Site.STATIC_LAMBDA);

        assertTrue(result instanceof LambdaBytecodeGate.Result.Rejected);
        assertThrows(SecurityException.class,
                () -> LambdaBytecodeGate.ensureAllowed(result, LambdaBytecodeGate.Site.STATIC_LAMBDA));

        Map<String, Long> map = SerializationTelemetry.snapshot();
        assertTrue(map.containsKey("coh.serialization.lambda_bytecode_check{result=rejected,"
                + "reason=security-config-missing,mode=dev,route=UNCLASSIFIED,site=static_lambda}"));
        }

    @Test
    public void shouldAllowApplicationClassNamePresentInSecurityConfig()
            throws Exception
        {
        withConfig("com.example.Allowed");

        LambdaBytecodeGate.Result result = LambdaBytecodeGate.checkClassName("com.example.Allowed",
                LambdaBytecodeGate.Site.STATIC_LAMBDA);

        assertTrue(result instanceof LambdaBytecodeGate.Result.Allowed);
        }

    @Test
    public void shouldUnionSecurityConfigWithSerializationAllowlist()
            throws Exception
        {
        withConfig(java.io.File.class.getName());

        assertTrue(SerializationAllowlist.isAllowlisted(java.io.File.class));
        }

    @Test
    public void shouldEnrichStrictPofUnregisteredTypeMessage()
        {
        ConfigurablePofContext context = new ConfigurablePofContext();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> context.getUserTypeIdentifier(SecurityConfigIntegrationTest.class));

        assertTrue(e.getMessage().contains("@PortableType"));
        assertTrue(e.getMessage().contains("@Remote.Allowed"));
        assertTrue(e.getMessage().contains("coherence-gradle-plugin"));
        }

    @Test
    public void shouldAllowClassIdentityPackageFromSecurityConfig()
            throws Exception
        {
        withConfig("com.example.app.Task");

        assertTrue(ClassIdentityAllowlist.isAllowed("com/example/app", "Task"));
        assertFalse(ClassIdentityAllowlist.isAllowed("com/example/other", "Task"));
        }

    private void withConfig(String... asNames)
            throws Exception
        {
        Path dir = m_folder.newFolder().toPath();
        Path path = dir.resolve(SecurityConfig.RESOURCE_SECURITY_CONFIG);
        Files.createDirectories(path.getParent());
        Files.write(path, xml(asNames).getBytes(StandardCharsets.UTF_8));

        Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[] {dir.toUri().toURL()}, null));
        SecurityConfig.resetForTesting();
        CoherenceModeHelper.reset();
        }

    private static String xml(String... asNames)
        {
        StringBuilder sb = new StringBuilder()
                .append("<?xml version=\"1.0\"?>\n")
                .append("<security-config xmlns=\"http://xmlns.oracle.com/coherence/coherence-security-config\" ")
                .append("version=\"1.0\">\n")
                .append("  <allowed-classes>\n");
        for (String sName : asNames)
            {
            sb.append("    <class name=\"").append(sName).append("\" source=\"manual\"/>\n");
            }
        return sb.append("  </allowed-classes>\n")
                .append("</security-config>\n")
                .toString();
        }

    private static void setMode(String sMode)
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, sMode);
        CoherenceModeHelper.reset();
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

    private static String mode()
        {
        return CoherenceMode.current().name().toLowerCase(Locale.ROOT);
        }

    private final ClassLoader m_loaderOld = Thread.currentThread().getContextClassLoader();
    private final String      m_sModeOld  = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
    }
