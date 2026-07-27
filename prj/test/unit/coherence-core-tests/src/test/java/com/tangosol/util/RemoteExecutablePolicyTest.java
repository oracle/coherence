/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.internal.util.security.SecurityConfig;

import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;

import com.tangosol.util.function.Remote;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.net.URL;
import java.net.URLClassLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link RemoteExecutablePolicy}.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public class RemoteExecutablePolicyTest
    {
    @Rule
    public TemporaryFolder m_folder = new TemporaryFolder();

    @After
    public void cleanup()
        {
        Thread.currentThread().setContextClassLoader(m_loaderOld);
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, m_sModeOld);
        resetMode();
        SerializationTelemetry.resetForTesting();
        resetSecurityConfig();
        RemoteExecutablePolicy.resetForTesting();
        }

    @Test
    public void isExecutable_returnsFalseForNull()
        {
        assertFalse(RemoteExecutablePolicy.current().isExecutable(null));
        }

    @Test
    public void isExecutable_returnsTrueWhenAnnotationPresent()
        {
        assertTrue(RemoteExecutablePolicy.current().isExecutable(AnnotatedExecutable.class));
        }

    @Test
    public void isExecutable_returnsTrueWhenSecurityConfigListsClassAsExecutable()
            throws Exception
        {
        withConfig(entry(XmlExecutable.class.getName(), "manual", true));

        assertTrue(RemoteExecutablePolicy.current().isExecutable(XmlExecutable.class));
        }

    @Test
    public void isExecutable_returnsFalseWhenNeitherPresent()
        {
        assertFalse(RemoteExecutablePolicy.current().isExecutable(PlainClass.class));
        }

    @Test
    public void isExecutable_returnsFalseWhenDescriptorLiteralOnly()
        {
        assertFalse(RemoteExecutablePolicy.current().isExecutable(DescriptorLiteralOnly.class));
        }

    @Test
    public void enforce_throwsSecurityExceptionWithReasonAndRoleWhenNotExecutable()
        {
        setMode("prod");

        SecurityException e = assertThrows(SecurityException.class,
                () -> RemoteExecutablePolicy.current().enforce(PlainClass.class, OperationReason.PROCESS_ENTRY,
                        SerializationRole.UNCLASSIFIED, null));

        assertTrue(e.getMessage().contains(PlainClass.class.getName()));
        assertTrue(e.getMessage().contains(OperationReason.PROCESS_ENTRY.name()));
        assertTrue(e.getMessage().contains(SerializationRole.UNCLASSIFIED.name()));
        }

    @Test
    public void enforce_recordsWouldRejectInLegacyModeWhenNotExecutable()
        {
        setMode("legacy");
        SerializationTelemetry.resetForTesting();

        RemoteExecutablePolicy.current().enforce(PlainClass.class, OperationReason.PROCESS_ENTRY,
                SerializationRole.UNCLASSIFIED, null);

        Map<String, Long> map = SerializationTelemetry.snapshot();
        assertEquals(Long.valueOf(1L), map.get("coh.executable.policy_check{result=would_reject,class="
                + PlainClass.class.getName() + ",reason=" + OperationReason.PROCESS_ENTRY.name()
                + ",role=" + SerializationRole.UNCLASSIFIED.name() + "}"));
        assertFalse(map.containsKey("coh.executable.policy_check{result=rejected,class="
                + PlainClass.class.getName() + ",reason=" + OperationReason.PROCESS_ENTRY.name()
                + ",role=" + SerializationRole.UNCLASSIFIED.name() + "}"));
        }

    @Test
    public void enforce_succeedsWhenExecutable()
        {
        setMode("prod");

        RemoteExecutablePolicy.current().enforce(AnnotatedExecutable.class, OperationReason.PROCESS_ENTRY,
                SerializationRole.UNCLASSIFIED, null);

        Map<String, Long> map = SerializationTelemetry.snapshot();
        assertEquals(Long.valueOf(1L), map.get("coh.executable.policy_check{reason="
                + OperationReason.PROCESS_ENTRY.name() + ",role=" + SerializationRole.UNCLASSIFIED.name()
                + ",result=allowed,mode=prod}"));
        }

    @Test
    public void current_returnsSameInstanceOnRepeatedCalls()
        {
        assertSame(RemoteExecutablePolicy.current(), RemoteExecutablePolicy.current());
        }

    private void withConfig(String... asEntries)
            throws Exception
        {
        Path dir  = m_folder.newFolder().toPath();
        Path path = dir.resolve(SecurityConfig.RESOURCE_SECURITY_CONFIG);
        Files.createDirectories(path.getParent());
        Files.write(path, xml(asEntries).getBytes(StandardCharsets.UTF_8));
        Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[] {dir.toUri().toURL()}, m_loaderOld));
        resetSecurityConfig();
        }

    private static void resetSecurityConfig()
        {
        try
            {
            var method = SecurityConfig.class.getDeclaredMethod("resetForTesting");
            method.setAccessible(true);
            method.invoke(null);
            }
        catch (ReflectiveOperationException e)
            {
            throw new AssertionError(e);
            }
        }

    private static void setMode(String sMode)
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, sMode);
        resetMode();
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
        }

    private static String entry(String sName, String sSource, boolean fExecutable)
        {
        return "<class name=\"" + sName + "\" source=\"" + sSource + "\""
                + (fExecutable ? " executable=\"true\"" : "") + "/>";
        }

    private static String xml(String... asEntries)
        {
        return "<?xml version=\"1.0\"?>\n"
                + "<security-config xmlns=\"http://xmlns.oracle.com/coherence/coherence-security-config\" "
                + "version=\"1.0\">\n"
                + "  <allowed-classes>\n"
                + String.join("\n", asEntries)
                + "\n  </allowed-classes>\n"
                + "</security-config>\n";
        }

    @Remote.Executable
    public static class AnnotatedExecutable
        {
        }

    public static class XmlExecutable
        {
        }

    public static class PlainClass
        {
        }

    public static class DescriptorLiteralOnly
        {
        public static final String DESCRIPTOR = "Lcom/tangosol/util/function/Remote$Executable;";
        }

    private final ClassLoader m_loaderOld = Thread.currentThread().getContextClassLoader();

    private final String m_sModeOld = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
    }
