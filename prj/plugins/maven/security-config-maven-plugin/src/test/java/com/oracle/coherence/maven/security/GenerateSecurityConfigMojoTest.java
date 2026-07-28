/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.maven.security;

import java.net.URI;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link GenerateSecurityConfigMojo}.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public class GenerateSecurityConfigMojoTest
    {
    @Rule
    public TemporaryFolder m_folder = new TemporaryFolder();

    @Test
    public void shouldGenerateSecurityConfig()
            throws Exception
        {
        Path dir = m_folder.newFolder().toPath();
        copyClass(dir, MojoAllowedFixture.class);

        GenerateSecurityConfigMojo mojo = new GenerateSecurityConfigMojo();
        mojo.setClassesDirectory(dir.toFile());

        mojo.execute();

        Path path = dir.resolve("META-INF/coherence/security-config.xml");
        assertTrue(Files.exists(path));

        String sXml = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertThat(sXml, containsString(MojoAllowedFixture.class.getName()));
        assertThat(sXml, containsString("source=\"@Remote.Allowed\""));
        }

    @Test
    public void shouldMergeExistingSecurityConfig()
            throws Exception
        {
        Path dir = m_folder.newFolder().toPath();
        copyClass(dir, MojoAllowedFixture.class);

        Path path = dir.resolve("META-INF/coherence/security-config.xml");
        Files.createDirectories(path.getParent());
        Files.write(path, ("<?xml version=\"1.0\"?>\n"
                + "<security-config xmlns=\"http://xmlns.oracle.com/coherence/coherence-security-config\"\n"
                + "                 version=\"1.0\">\n"
                + "  <allowed-classes>\n"
                + "    <class name=\"example.ManualOwner\" source=\"manual\" lambda-target=\"true\"/>\n"
                + "  </allowed-classes>\n"
                + "</security-config>\n").getBytes(StandardCharsets.UTF_8));

        GenerateSecurityConfigMojo mojo = new GenerateSecurityConfigMojo();
        mojo.setClassesDirectory(dir.toFile());

        mojo.execute();

        String sXml = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertThat(sXml, containsString("example.ManualOwner"));
        assertThat(sXml, containsString("lambda-target=\"true\""));
        assertThat(sXml, containsString(MojoAllowedFixture.class.getName()));
        assertThat(sXml, containsString("source=\"@Remote.Allowed\""));
        }

    @Test
    public void shouldSkipWhenClassesDirectoryIsMissing()
            throws Exception
        {
        Path dir = m_folder.getRoot().toPath().resolve("missing");

        GenerateSecurityConfigMojo mojo = new GenerateSecurityConfigMojo();
        mojo.setClassesDirectory(dir.toFile());

        mojo.execute();

        assertFalse(Files.exists(dir.resolve("META-INF/coherence/security-config.xml")));
        }

    @Test
    public void shouldSkipWhenClassesDirectoryHasNoClassFiles()
            throws Exception
        {
        Path dir = m_folder.newFolder().toPath();
        Files.write(dir.resolve("readme.txt"), "no classes".getBytes(StandardCharsets.UTF_8));

        GenerateSecurityConfigMojo mojo = new GenerateSecurityConfigMojo();
        mojo.setClassesDirectory(dir.toFile());

        mojo.execute();

        assertFalse(Files.exists(dir.resolve("META-INF/coherence/security-config.xml")));
        }

    private static void copyClass(Path dir, Class<?> clz)
            throws Exception
        {
        String sResource = clz.getName().replace('.', '/') + ".class";
        URI    uri       = GenerateSecurityConfigMojoTest.class.getClassLoader().getResource(sResource).toURI();
        Path   from      = java.nio.file.Paths.get(uri);
        Path   to        = dir.resolve(sResource);

        Files.createDirectories(to.getParent());
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }
