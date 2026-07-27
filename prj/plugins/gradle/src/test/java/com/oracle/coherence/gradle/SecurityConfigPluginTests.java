/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import com.oracle.coherence.gradle.support.TestUtils;
import com.oracle.coherence.gradle.support.TestUtils.SecurityConfigEntry;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import java.util.Set;

import static com.oracle.coherence.gradle.support.TestUtils.appendToFile;
import static com.oracle.coherence.gradle.support.TestUtils.copyTestProject;
import static com.oracle.coherence.gradle.support.TestUtils.getPofIndexedClasses;
import static com.oracle.coherence.gradle.support.TestUtils.getSecurityConfigEntries;
import static com.oracle.coherence.gradle.support.TestUtils.getSecurityConfigFqns;
import static com.oracle.coherence.gradle.support.TestUtils.setupGradlePropertiesFile;
import static com.oracle.coherence.gradle.support.TestUtils.setupGradleSettingsFile;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the security-config Gradle task.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public class SecurityConfigPluginTests
    {
    @BeforeEach
    void setup()
        {
        LOGGER.info("Gradle root directory for test: {}", m_gradleProjectRootDirectory.getAbsolutePath());
        LOGGER.info("Using Coherence Group Id '{}' and Coherence version {}",
                    f_buildTimeProperties.getCoherenceGroupId(),
                    f_buildTimeProperties.getCoherenceVersion());
        }

    @AfterEach
    void cleanUp()
        {
        GradleRunner.create()
                .withProjectDir(m_gradleProjectRootDirectory)
                .withArguments("clean", "--info")
                .withDebug(true)
                .withPluginClasspath()
                .build();
        }

    @Test
    void shouldApplySecurityConfigTaskWithNoSources()
        {
        setupBuild("securityConfig {\n}\n");

        BuildResult result = runner("securityConfig", "--info").build();

        logOutput(result);
        assertTask(result, ":compileJava", TaskOutcome.NO_SOURCE);
        BuildTask task = result.task(":securityConfig");
        assertThat(task, is(notNullValue()));
        assertThat(task.getOutcome(), is(TaskOutcome.SUCCESS));
        assertFalse(securityConfigFile("build/classes/java/main").exists());
        }

    @Test
    void shouldGenerateMainSecurityConfig()
            throws IOException
        {
        setupBuild("securityConfig {\n}\n");
        copyTestProject("test-security-config", m_gradleProjectRootDirectory);

        BuildResult result = runner("assemble", "--info").build();

        logOutput(result);
        assertTask(result, ":assemble", TaskOutcome.SUCCESS);

        Set<SecurityConfigEntry> entries = getSecurityConfigEntries(m_gradleProjectRootDirectory, "build/classes/java/main");
        assertThat(entries, containsInAnyOrder(
                entry("com.oracle.coherence.gradle.security.type.NonRecursiveAllowedType", "@Remote.Allowed"),
                entry("com.oracle.coherence.gradle.security.type.RecursiveAllowedType", "@Remote.Allowed"),
                entry("com.oracle.coherence.gradle.security.type.RecursiveAllowedType$Nested", "@Remote.Allowed"),
                entry("com.oracle.coherence.gradle.security.pkgrecursive.PackageAllowedClass", "@Remote.Allowed(package)"),
                entry("com.oracle.coherence.gradle.security.pkgrecursive.sub.SubPackageAllowedClass", "@Remote.Allowed(package)"),
                entry("com.oracle.coherence.gradle.security.executable.AllowedExecutableThing", "@Remote.Executable", true),
                entry("com.oracle.coherence.gradle.security.executable.ExecutableThing", "@Remote.Executable", true),
                entry("com.oracle.coherence.gradle.security.lambdatarget.LocalLambdaTargetThing",
                        "@Remote.Executable", false, true),
                entry("com.oracle.coherence.gradle.security.lambdatarget.InheritedLambdaTargetThing",
                        "@Remote.Executable", false, true),
                entry("com.oracle.coherence.gradle.security.portable.PortableThing", "@PortableType")));

        Set<String> names = getSecurityConfigFqns(m_gradleProjectRootDirectory, "build/classes/java/main");
        assertFalse(names.contains("com.oracle.coherence.gradle.security.plain.PlainThing"));
        assertFalse(names.contains("com.oracle.coherence.gradle.security.type.NonRecursiveAllowedType$Nested"));
        assertFalse(names.contains("com.oracle.coherence.gradle.security.lambdatarget.MissingFunctionalInterfaceThing"));
        assertFalse(names.contains("com.oracle.coherence.gradle.security.lambdatarget.NonSerializableLambdaTargetThing"));
        assertFalse(names.contains("com.oracle.coherence.gradle.security.lambdatarget.AbstractExecutableThing"));
        }

    @Test
    void shouldSkipSecurityConfigGeneration()
            throws IOException
        {
        setupBuild("securityConfig {\n    skip = true\n}\n");
        copyTestProject("test-security-config", m_gradleProjectRootDirectory);

        BuildResult result = runner("assemble", "--info").build();

        logOutput(result);
        assertTask(result, ":assemble", TaskOutcome.SUCCESS);
        assertFalse(securityConfigFile("build/classes/java/main").exists());
        }

    @Test
    void shouldGenerateMainAndTestSecurityConfig()
            throws IOException
        {
        setupBuild("securityConfig {\n    processTestClasses = true\n}\n");
        copyTestProject("test-security-config", m_gradleProjectRootDirectory);

        BuildResult result = runner("test", "--info").build();

        logOutput(result);
        assertTask(result, ":test", TaskOutcome.SUCCESS);

        assertTrue(getSecurityConfigFqns(m_gradleProjectRootDirectory, "build/classes/java/main")
                .contains("com.oracle.coherence.gradle.security.type.RecursiveAllowedType"));
        assertTrue(getSecurityConfigFqns(m_gradleProjectRootDirectory, "build/classes/java/test")
                .contains("com.oracle.coherence.gradle.security.test.TestAllowedType"));
        }

    @Test
    void shouldRunPortableTypesAndSecurityConfigTogether()
            throws IOException
        {
        setupBuild("portableTypes {\n    indexPofClasses = true\n}\n\nsecurityConfig {\n}\n");
        copyTestProject("test-security-config", m_gradleProjectRootDirectory);

        BuildResult result = runner("assemble", "--info").build();

        logOutput(result);
        assertTask(result, ":assemble", TaskOutcome.SUCCESS);

        File pofIndexFile = new File(m_gradleProjectRootDirectory, "build/classes/java/main/META-INF/pof.idx");
        assertTrue(pofIndexFile.exists(), "The pof.idx should exist at " + pofIndexFile.getAbsolutePath());
        assertThat(getPofIndexedClasses(m_gradleProjectRootDirectory, "build/classes/java/main"),
                containsInAnyOrder("com.oracle.coherence.gradle.security.portable.PortableThing"));
        assertTrue(getSecurityConfigFqns(m_gradleProjectRootDirectory, "build/classes/java/main")
                .contains("com.oracle.coherence.gradle.security.portable.PortableThing"));
        }

    private void setupBuild(String sExtensionBlock)
        {
        setupGradlePropertiesFile(m_gradleProjectRootDirectory);
        setupGradleSettingsFile(m_gradleProjectRootDirectory, "settings",
                f_buildTimeProperties.getCoherenceLocalDependencyRepo());

        appendToFile(new File(m_gradleProjectRootDirectory, "build.gradle"),
                String.format("plugins {\n"
                        + "    id 'java'\n"
                        + "    id '%s'\n"
                        + "}\n"
                        + "repositories {\n"
                        + "    maven { url '%s' }\n"
                        + "    mavenLocal()\n"
                        + "    mavenCentral()\n"
                        + "}\n"
                        + "dependencies {\n"
                        + "    implementation '%s:coherence:%s'\n"
                        + "}\n\n"
                        + "%s",
                        f_buildTimeProperties.getCoherenceGroupId(),
                        f_buildTimeProperties.getCoherenceLocalDependencyRepo(),
                        f_buildTimeProperties.getCoherenceGroupId(),
                        f_buildTimeProperties.getCoherenceVersion(),
                        sExtensionBlock));
        }

    private GradleRunner runner(String... asArguments)
        {
        return GradleRunner.create()
                .withProjectDir(m_gradleProjectRootDirectory)
                .withArguments(asArguments)
                .withDebug(true)
                .withPluginClasspath();
        }

    private File securityConfigFile(String sBaseDirectory)
        {
        return new File(m_gradleProjectRootDirectory, sBaseDirectory + "/META-INF/coherence/security-config.xml");
        }

    private SecurityConfigEntry entry(String sName, String sSource)
        {
        return new SecurityConfigEntry(sName, sSource);
        }

    private SecurityConfigEntry entry(String sName, String sSource, boolean fExecutable)
        {
        return new SecurityConfigEntry(sName, sSource, fExecutable);
        }

    private SecurityConfigEntry entry(String sName, String sSource, boolean fExecutable, boolean fLambdaTarget)
        {
        return new SecurityConfigEntry(sName, sSource, fExecutable, fLambdaTarget);
        }

    private void assertTask(BuildResult result, String sTaskName, TaskOutcome outcome)
        {
        BuildTask task = result.task(sTaskName);
        assertThat(task, is(notNullValue()));
        assertThat(task.getOutcome(), is(outcome));
        }

    private void logOutput(BuildResult gradleResult)
        {
        LOGGER.info(
                "\n-------- [ Gradle output] -------->>>>\n"
                + gradleResult.getOutput()
                + "<<<<------------------------------------"
                );
        }

    // ----- data members ---------------------------------------------------

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfigPluginTests.class);

    @TempDir
    private File m_gradleProjectRootDirectory;

    private final BuildTimeProperties f_buildTimeProperties = new BuildTimeProperties();
    }
