/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import com.oracle.coherence.gradle.support.TestUtils.SecurityConfigEntry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;
import java.util.Set;

import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static com.oracle.coherence.gradle.support.TestUtils.getSecurityConfigEntries;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link SecurityConfigResourceMerger}.
 *
 * @author Aleks Seovic  2026.08.10
 * @since 26.1
 */
class SecurityConfigResourceMergerTest
    {
    @Test
    void shouldMergeDirectoryAndJarResources()
            throws IOException
        {
        Path pathDirectory = createDirectoryResource("directory", config(
                clazz("com.example.Alpha", "manual", true, false),
                clazz("com.example.Duplicate", "manual", false, false)));
        Path pathJar = createJarResource("dependency.jar", config(
                clazz("com.example.Beta", "@Remote.Executable", false, true),
                clazz("com.example.Duplicate", "scan", false, false)));
        Path pathOutput = output();

        SecurityConfigResourceMerger.MergeResult result = SecurityConfigResourceMerger.merge(
                List.of(pathDirectory.toFile(), pathJar.toFile()), pathOutput);

        assertEquals(2, result.getSourceCount());
        assertEquals(3, result.getEntryCount());
        assertThat(entries(), containsInAnyOrder(
                new SecurityConfigEntry("com.example.Alpha", "manual", true, false),
                new SecurityConfigEntry("com.example.Beta", "@Remote.Executable", false, true),
                new SecurityConfigEntry("com.example.Duplicate", "manual", false, false)));
        }

    @Test
    void shouldRejectConflictingExecutableClassification()
            throws IOException
        {
        Path pathOne = createDirectoryResource("one", config(
                clazz("com.example.Conflict", "manual", false, false)));
        Path pathTwo = createJarResource("two.jar", config(
                clazz("com.example.Conflict", "manual", true, false)));

        IOException error = assertThrows(IOException.class, () -> SecurityConfigResourceMerger.merge(
                List.of(pathOne.toFile(), pathTwo.toFile()), output()));

        assertFalse(error.getMessage().isBlank());
        }

    @Test
    void shouldRejectConflictingLambdaTargetClassification()
            throws IOException
        {
        Path pathOne = createDirectoryResource("one", config(
                clazz("com.example.Conflict", "manual", false, false)));
        Path pathTwo = createJarResource("two.jar", config(
                clazz("com.example.Conflict", "manual", false, true)));

        IOException error = assertThrows(IOException.class, () -> SecurityConfigResourceMerger.merge(
                List.of(pathOne.toFile(), pathTwo.toFile()), output()));

        assertFalse(error.getMessage().isBlank());
        }

    @Test
    void shouldReplacePreviousOutputRatherThanRetainingStaleEntries()
            throws IOException
        {
        Path pathOne = createDirectoryResource("one", config(
                clazz("com.example.One", "manual", false, false)));
        Path pathTwo = createDirectoryResource("two", config(
                clazz("com.example.Two", "manual", false, false)));
        Path pathOutput = output();

        SecurityConfigResourceMerger.merge(List.of(pathOne.toFile(), pathTwo.toFile()), pathOutput);
        SecurityConfigResourceMerger.merge(List.of(pathOne.toFile(),
                pathOutput.getParent().getParent().getParent().toFile()), pathOutput);

        assertThat(entries(), containsInAnyOrder(
                new SecurityConfigEntry("com.example.One", "manual", false, false)));
        }

    @Test
    void shouldDeleteStaleOutputWhenClasspathContainsNoSecurityConfig()
            throws IOException
        {
        Path pathInput  = Files.createDirectories(f_pathTemp.resolve("empty"));
        Path pathOutput = output();

        Files.createDirectories(pathOutput.getParent());
        Files.writeString(pathOutput, config(clazz("com.example.Stale", "manual", false, false)));

        SecurityConfigResourceMerger.MergeResult result = SecurityConfigResourceMerger.merge(
                List.of(pathInput.toFile()), pathOutput);

        assertEquals(0, result.getSourceCount());
        assertFalse(Files.exists(pathOutput));
        }

    private Path createDirectoryResource(String sName, String sXml)
            throws IOException
        {
        Path pathRoot     = Files.createDirectories(f_pathTemp.resolve(sName));
        Path pathResource = pathRoot.resolve(SecurityConfigResourceMerger.RESOURCE_SECURITY_CONFIG);

        Files.createDirectories(pathResource.getParent());
        Files.writeString(pathResource, sXml, StandardCharsets.UTF_8);
        return pathRoot;
        }

    private Path createJarResource(String sName, String sXml)
            throws IOException
        {
        Path pathJar = f_pathTemp.resolve(sName);
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(pathJar)))
            {
            out.putNextEntry(new JarEntry(SecurityConfigResourceMerger.RESOURCE_SECURITY_CONFIG));
            out.write(sXml.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            }
        return pathJar;
        }

    private Set<SecurityConfigEntry> entries()
        {
        return getSecurityConfigEntries(f_pathTemp.toFile(), "output");
        }

    private Path output()
        {
        return f_pathTemp.resolve("output").resolve(SecurityConfigResourceMerger.RESOURCE_SECURITY_CONFIG);
        }

    private static String config(String... asClass)
        {
        return "<?xml version=\"1.0\"?>\n"
                + "<security-config xmlns=\"http://xmlns.oracle.com/coherence/coherence-security-config\""
                + " version=\"1.0\">\n"
                + "  <allowed-classes>\n"
                + String.join("", asClass)
                + "  </allowed-classes>\n"
                + "</security-config>\n";
        }

    private static String clazz(String sName, String sSource, boolean fExecutable, boolean fLambdaTarget)
        {
        return "    <class name=\"" + sName + "\" source=\"" + sSource + "\""
                + (fExecutable ? " executable=\"true\"" : "")
                + (fLambdaTarget ? " lambda-target=\"true\"" : "")
                + "/>\n";
        }

    @TempDir
    private Path f_pathTemp;
    }
