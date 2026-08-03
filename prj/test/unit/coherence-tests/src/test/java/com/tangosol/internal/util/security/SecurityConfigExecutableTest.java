/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.security;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.net.URL;
import java.net.URLClassLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for executable entries in {@link SecurityConfig}.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.07
 */
public class SecurityConfigExecutableTest
    {
    @Rule
    public TemporaryFolder m_folder = new TemporaryFolder();

    @After
    public void cleanup()
        {
        Thread.currentThread().setContextClassLoader(m_loaderOld);
        SecurityConfig.resetForTesting();
        }

    @Test
    public void shouldExposeExecutableEntries()
            throws Exception
        {
        withConfig(entry("com.example.ExecutableOne", "manual", "true"),
                entry("com.example.PlainAllowed", "manual", null),
                entry("com.example.ExecutableTwo", "@Remote.Executable", "true"));

        SecurityConfig config = SecurityConfig.current();

        assertTrue(config.isExecutable("com.example.ExecutableOne"));
        assertTrue(config.isExecutable("com.example.ExecutableTwo"));
        assertFalse(config.isExecutable("com.example.PlainAllowed"));
        assertFalse(config.isExecutable("com.example.Missing"));

        assertTrue(config.allowedFqns().contains("com.example.ExecutableOne"));
        assertTrue(config.allowedFqns().contains("com.example.ExecutableTwo"));
        assertTrue(config.allowedFqns().contains("com.example.PlainAllowed"));
        assertTrue(config.executableFqns().contains("com.example.ExecutableOne"));
        assertTrue(config.executableFqns().contains("com.example.ExecutableTwo"));
        assertFalse(config.executableFqns().contains("com.example.PlainAllowed"));
        }

    @Test
    public void shouldDefaultExecutableToFalseWhenAbsent()
            throws Exception
        {
        withConfig(entry("com.example.PlainAllowed", "manual", null));

        SecurityConfig config = SecurityConfig.current();

        assertTrue(config.contains("com.example.PlainAllowed"));
        assertFalse(config.isExecutable("com.example.PlainAllowed"));
        assertTrue(config.executableFqns().isEmpty());
        }

    @Test
    public void shouldExposeLambdaTargetEntries()
            throws Exception
        {
        withConfig(entry("com.example.RemoteFunction", "@Remote.Executable", null, "true"),
                entry("com.example.Executable", "@Remote.Executable", "true", null),
                entry("com.example.PlainAllowed", "manual", null, null));

        SecurityConfig config = SecurityConfig.current();

        assertTrue(config.isLambdaTarget("com.example.RemoteFunction"));
        assertFalse(config.isLambdaTarget("com.example.Executable"));
        assertFalse(config.isLambdaTarget("com.example.PlainAllowed"));
        assertFalse(config.isLambdaTarget("com.example.Missing"));

        assertTrue(config.lambdaTargetFqns().contains("com.example.RemoteFunction"));
        assertFalse(config.lambdaTargetFqns().contains("com.example.Executable"));
        assertTrue(config.contains("com.example.RemoteFunction"));
        }

    @Test
    public void shouldRejectInvalidExecutableAttribute()
            throws Exception
        {
        Path dir = withConfig(entry("com.example.Bad", "manual", "notabool"));

        IllegalStateException e = assertThrows(IllegalStateException.class, SecurityConfig::current);

        assertTrue(e.getMessage().contains(SecurityConfig.RESOURCE_SECURITY_CONFIG));
        assertTrue(e.getMessage().contains(resourceUrl(dir)));
        }

    private Path withConfig(String... asEntries)
            throws Exception
        {
        Path dir  = m_folder.newFolder().toPath();
        Path path = dir.resolve(SecurityConfig.RESOURCE_SECURITY_CONFIG);
        Files.createDirectories(path.getParent());
        Files.write(path, xml(asEntries).getBytes(StandardCharsets.UTF_8));

        Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[] {dir.toUri().toURL()}, null));
        SecurityConfig.resetForTesting();
        return dir;
        }

    private static String entry(String sName, String sSource, String sExecutable)
        {
        return entry(sName, sSource, sExecutable, null);
        }

    private static String entry(String sName, String sSource, String sExecutable, String sLambdaTarget)
        {
        return "<class name=\"" + sName + "\" source=\"" + sSource + "\""
                + (sExecutable == null ? "" : " executable=\"" + sExecutable + "\"")
                + (sLambdaTarget == null ? "" : " lambda-target=\"" + sLambdaTarget + "\"")
                + "/>";
        }

    private static String resourceUrl(Path dir)
            throws Exception
        {
        return dir.resolve(SecurityConfig.RESOURCE_SECURITY_CONFIG).toUri().toURL().toString();
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

    private final ClassLoader m_loaderOld = Thread.currentThread().getContextClassLoader();
    }
