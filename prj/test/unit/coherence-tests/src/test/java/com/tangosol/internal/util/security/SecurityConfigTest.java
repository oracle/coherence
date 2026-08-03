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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link SecurityConfig}.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.07
 */
public class SecurityConfigTest
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
    public void shouldReturnEmptyConfigWhenClasspathHasNoResources()
            throws Exception
        {
        withLoader();

        SecurityConfig config = SecurityConfig.current();

        assertFalse(config.contains("com.example.Missing"));
        assertTrue(config.allowedFqns().isEmpty());
        }

    @Test
    public void shouldLoadSingleSecurityConfig()
            throws Exception
        {
        withLoader(writeConfig("one", entry("com.example.One", "manual")));

        SecurityConfig config = SecurityConfig.current();

        assertTrue(config.contains("com.example.One"));
        assertEquals("manual", config.source("com.example.One"));
        assertEquals(1, config.allowedFqns().size());
        }

    @Test
    public void shouldUnionMultipleResourcesAndKeepFirstDuplicateSource()
            throws Exception
        {
        Path dirOne = writeConfig("one", entry("com.example.Shared", "manual"), entry("com.example.One", "scan"));
        Path dirTwo = writeConfig("two", entry("com.example.Shared", "scan"),
                entry("com.example.Two", "@PortableType"));
        withLoader(dirOne, dirTwo);

        SecurityConfig config = SecurityConfig.current();

        assertTrue(config.contains("com.example.One"));
        assertTrue(config.contains("com.example.Two"));
        assertEquals("manual", config.source("com.example.Shared"));
        assertEquals(3, config.allowedFqns().size());
        }

    @Test
    public void shouldFailFastForMalformedXml()
            throws Exception
        {
        Path dir = writeRaw("bad-xml", "<security-config");
        withLoader(dir);

        IllegalStateException e = assertThrows(IllegalStateException.class, SecurityConfig::current);

        assertTrue(e.getMessage().contains(SecurityConfig.RESOURCE_SECURITY_CONFIG));
        assertTrue(e.getMessage().contains(resourceUrl(dir)));
        }

    @Test
    public void shouldFailFastForXsdInvalidXml()
            throws Exception
        {
        Path dir = writeRaw("bad-xsd", xml("<class name=\"NoPackage\" source=\"manual\"/>"));
        withLoader(dir);

        IllegalStateException e = assertThrows(IllegalStateException.class, SecurityConfig::current);

        assertTrue(e.getMessage().contains(SecurityConfig.RESOURCE_SECURITY_CONFIG));
        assertTrue(e.getMessage().contains(resourceUrl(dir)));
        }

    @Test
    public void shouldMemoizeCurrentConfig()
            throws Exception
        {
        Path dir = writeConfig("memo", entry("com.example.First", "manual"));
        withLoader(dir);

        SecurityConfig config = SecurityConfig.current();

        writeConfig(dir, entry("com.example.Second", "manual"));

        assertSame(config, SecurityConfig.current());
        assertTrue(config.contains("com.example.First"));
        assertFalse(config.contains("com.example.Second"));
        }

    private void withLoader(Path... dirs)
            throws Exception
        {
        URL[] urls = new URL[dirs.length];
        for (int i = 0; i < dirs.length; i++)
            {
            urls[i] = dirs[i].toUri().toURL();
            }
        Thread.currentThread().setContextClassLoader(new URLClassLoader(urls, null));
        SecurityConfig.resetForTesting();
        }

    private Path writeConfig(String sName, String... asEntries)
            throws Exception
        {
        Path dir = m_folder.newFolder(sName).toPath();
        return writeConfig(dir, asEntries);
        }

    private Path writeConfig(Path dir, String... asEntries)
            throws Exception
        {
        return writeRaw(dir, xml(asEntries));
        }

    private Path writeRaw(String sName, String sXml)
            throws Exception
        {
        return writeRaw(m_folder.newFolder(sName).toPath(), sXml);
        }

    private Path writeRaw(Path dir, String sXml)
            throws Exception
        {
        Path path = dir.resolve(SecurityConfig.RESOURCE_SECURITY_CONFIG);
        Files.createDirectories(path.getParent());
        Files.write(path, sXml.getBytes(StandardCharsets.UTF_8));
        return dir;
        }

    private static String entry(String sName, String sSource)
        {
        return "<class name=\"" + sName + "\" source=\"" + sSource + "\"/>";
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
