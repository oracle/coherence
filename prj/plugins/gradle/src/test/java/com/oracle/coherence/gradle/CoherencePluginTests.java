/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import com.oracle.coherence.gradle.support.TestUtils;
import com.tangosol.io.pof.PortableTypeSerializer;
import com.tangosol.io.pof.SimplePofContext;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.testkit.runner.UnexpectedBuildFailure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import org.junit.jupiter.api.io.TempDir;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.text.StringContainsInOrder.stringContainsInOrder;

import static com.oracle.coherence.gradle.support.TestUtils.assertThatClassIsPofInstrumented;
import static com.oracle.coherence.gradle.support.TestUtils.copyFileTo;
import static com.oracle.coherence.gradle.support.TestUtils.getPofClass;
import static com.oracle.coherence.gradle.support.TestUtils.getPofIndexedClasses;
import static com.oracle.coherence.gradle.support.TestUtils.setupGradleBuildFile;
import static com.oracle.coherence.gradle.support.TestUtils.setupGradlePropertiesFile;
import static com.oracle.coherence.gradle.support.TestUtils.setupGradleSettingsFile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gunnar Hillert
 */
public class CoherencePluginTests
    {
    @BeforeEach
    void setup()
        {
        LOGGER.info("Gradle root directory for test: {}", m_gradleProjectRootDirectory.getAbsolutePath());
        LOGGER.info("Using Coherence Group Id '{}' and Coherence version {}",
                    f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                    f_coherenceBuildTimeProperties.getCoherenceVersion());
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
    void applyBasicCoherenceGradlePluginWithNoSources()
        {
        setupGradlePropertiesFile(m_gradleProjectRootDirectory);
        setupGradleSettingsFile(m_gradleProjectRootDirectory, "settings", f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo());
        setupGradleBuildFile(m_gradleProjectRootDirectory, "applyBasicCoherenceGradlePluginWithNoSources",
                             f_coherenceBuildTimeProperties.getCoherenceGroupId());

        BuildResult gradleResult = GradleRunner.create()
                .withProjectDir(m_gradleProjectRootDirectory)
                .withArguments("coherencePof")
                .withDebug(true)
                .withPluginClasspath()
                .build();

        logOutput(gradleResult);

        assertSuccess(gradleResult);
        String sOutput = gradleResult.getOutput();
        assertThat(sOutput, containsString("Task :compileJava NO-SOURCE"));
        }

    @Test
    void applyBasicCoherenceGradlePluginWithTestsNoSources()
        {
        setupGradlePropertiesFile(m_gradleProjectRootDirectory);
        setupGradleSettingsFile(m_gradleProjectRootDirectory, "settings", f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo());
        setupGradleBuildFile(m_gradleProjectRootDirectory, "applyBasicCoherenceGradlePluginWithTestsNoSources",
                f_coherenceBuildTimeProperties.getCoherenceGroupId());

        BuildResult gradleResult = GradleRunner.create()
                .withProjectDir(m_gradleProjectRootDirectory)
                .withArguments("coherencePofTest")
                .withDebug(true)
                .withPluginClasspath()
                .build();

        logOutput(gradleResult);

        assertSuccess(gradleResult, ":coherencePofTest");
        String sOutput = gradleResult.getOutput();
        assertThat(sOutput, containsString("Task :compileTestJava NO-SOURCE"));
        }
    @Test
    void applyBasicCoherenceGradlePluginAndCallTasks()
        {
        setupGradlePropertiesFile(m_gradleProjectRootDirectory);
        setupGradleSettingsFile(m_gradleProjectRootDirectory, "settings", f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo());
        setupGradleBuildFile(m_gradleProjectRootDirectory, "applyBasicCoherenceGradlePluginWithNoSources",
                f_coherenceBuildTimeProperties.getCoherenceGroupId());

        BuildResult gradleResult = GradleRunner.create()
                .withProjectDir(m_gradleProjectRootDirectory)
                .withArguments("tasks")
                .withDebug(false)
                .withPluginClasspath()
                .build();

        logOutput(gradleResult);

        String sOutput = gradleResult.getOutput();
        assertThat(sOutput, containsString("Coherence tasks"));
        assertThat(sOutput, containsString("coherencePof - Generate Pof-instrumented classes."));
        }

    @Test
    void applyBasicCoherenceGradlePluginWithClass()
        {
        setupGradlePropertiesFile(m_gradleProjectRootDirectory);
        setupGradleSettingsFile(m_gradleProjectRootDirectory, "settings", f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo());
        setupGradleBuildFile(m_gradleProjectRootDirectory, "applyBasicCoherenceGradlePluginWithClass",
                             f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                             f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo(),
                             f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                             f_coherenceBuildTimeProperties.getCoherenceVersion());

        copyFileTo("/Foo.txt", m_gradleProjectRootDirectory,
                   "/src/main/java/foo", "Foo.java");

        BuildResult gradleResult = GradleRunner.create()
                .withProjectDir(m_gradleProjectRootDirectory)
                .withArguments("build", "--info")
                .withDebug(true)
                .withPluginClasspath()
                .build();

        logOutput(gradleResult);

        assertSuccess(gradleResult, ":build");

        String sOutput = gradleResult.getOutput();
        assertThat(sOutput, containsString("Instrumenting type foo.Foo"));

        Class<?> foo = getPofClass(this.m_gradleProjectRootDirectory, "foo.Foo", "build/classes/java/main/");
        assertThatClassIsPofInstrumented(foo);
        }

    @Test
    void applyBasicCoherenceGradlePluginWithClassTwice()
        {
        setupGradlePropertiesFile(m_gradleProjectRootDirectory);
        setupGradleSettingsFile(m_gradleProjectRootDirectory, "settings", f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo());
        setupGradleBuildFile(m_gradleProjectRootDirectory, "applyBasicCoherenceGradlePluginWithClass",
                f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo(),
                f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                f_coherenceBuildTimeProperties.getCoherenceVersion());

        copyFileTo("/Foo.txt", m_gradleProjectRootDirectory,
                "/src/main/java", "Foo.java");

        BuildResult gradleResult = GradleRunner.create()
                .withProjectDir(m_gradleProjectRootDirectory)
                .withArguments("build", "--info")
                .withDebug(true)
                .withPluginClasspath()
                .build();

        logOutput(gradleResult);

        assertSuccess(gradleResult, ":build");

        String sOutput = gradleResult.getOutput();
        assertThat(sOutput, containsString("Instrumenting type foo.Foo"));

        Class<?> foo = getPofClass(this.m_gradleProjectRootDirectory, "foo.Foo", "build/classes/java/main/");
        assertThatClassIsPofInstrumented(foo);

        BuildResult gradleResult2 = GradleRunner.create()
                .withProjectDir(m_gradleProjectRootDirectory)
                .withArguments("build", "--info")
                .withDebug(true)
                .withPluginClasspath()
                .build();

        logOutput(gradleResult2);

        assertUpToDate(gradleResult2);

        String sOutput2 = gradleResult2.getOutput();
        assertThat(sOutput2, containsString("Skipping task ':compileJava' as it is up-to-date"));

        Class<?> foo2 = getPofClass(this.m_gradleProjectRootDirectory, "foo.Foo", "build/classes/java/main/");
        assertThatClassIsPofInstrumented(foo2);
        }

    @Test
    void applyBasicCoherenceGradlePluginWithClassChange()
        {
        setupGradlePropertiesFile(m_gradleProjectRootDirectory);
        setupGradleSettingsFile(m_gradleProjectRootDirectory, "settings", f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo());
        setupGradleBuildFile(m_gradleProjectRootDirectory, "applyBasicCoherenceGradlePluginWithClass",
                f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo(),
                f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                f_coherenceBuildTimeProperties.getCoherenceVersion());

        copyFileTo("/Foo.txt", m_gradleProjectRootDirectory,
                "/src/main/java", "Foo.java");

        BuildResult gradleResult = GradleRunner.create()
                .withProjectDir(m_gradleProjectRootDirectory)
                .withArguments("build", "--info")
                .withDebug(true)
                .withPluginClasspath()
                .build();

        logOutput(gradleResult);

        assertSuccess(gradleResult, ":build");

        String sOutput = gradleResult.getOutput();
        assertThat(sOutput, containsString("Instrumenting type foo.Foo"));

        Class<?> foo = getPofClass(this.m_gradleProjectRootDirectory, "foo.Foo", "build/classes/java/main/");
        assertThatClassIsPofInstrumented(foo);

        copyFileTo("/Bar.txt", m_gradleProjectRootDirectory,
                "/src/main/java", "Bar.java");
        copyFileTo("/Color.txt", m_gradleProjectRootDirectory,
                "/src/main/java", "Color.java");

        BuildResult gradleResult2 = GradleRunner.create()
                .withProjectDir(m_gradleProjectRootDirectory)
                .withArguments("build", "--info")
                .withDebug(true)
                .withPluginClasspath()
                .build();

        logOutput(gradleResult2);

        assertSuccess(gradleResult, ":compileJava");

        String sOutput2 = gradleResult2.getOutput();
        assertThat(sOutput2, containsString("Instrumenting type Bar"));
        assertThat(sOutput2, containsString("Skipping type foo.Foo. Type is already instrumented"));
        assertThat(sOutput2, containsString("Skipping type Color. Type does not exist in the schema or PofType extension is not defined"));

        Class<?> foo2 = getPofClass(this.m_gradleProjectRootDirectory, "foo.Foo", "build/classes/java/main/");
        assertThatClassIsPofInstrumented(foo2);

        Class<?> bar = getPofClass(this.m_gradleProjectRootDirectory, "Bar", "build/classes/java/main/");
        assertThatClassIsPofInstrumented(bar);

        }

    @Test
    void applyBasicCoherenceGradlePluginWithClassAndWithPofIndexing() throws IOException {
        setupGradlePropertiesFile(m_gradleProjectRootDirectory);
        setupGradleSettingsFile(m_gradleProjectRootDirectory, "settings", f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo());
        setupGradleBuildFile(m_gradleProjectRootDirectory, "applyWithIndexing",
                f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo(),
                f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                f_coherenceBuildTimeProperties.getCoherenceVersion());

        copyFileTo("/Foo.txt", m_gradleProjectRootDirectory,
                "/src/main/java/foo", "Foo.java");

        BuildResult gradleResult = GradleRunner.create()
                .withProjectDir(m_gradleProjectRootDirectory)
                .withArguments("build", "--info")
                .withDebug(true)
                .withPluginClasspath()
                .build();

        logOutput(gradleResult);

        assertSuccess(gradleResult, ":build");

        String sOutput = gradleResult.getOutput();
        assertThat(sOutput, containsString("Instrumenting type foo.Foo"));

        Class<?> foo = getPofClass(this.m_gradleProjectRootDirectory, "foo.Foo", "build/classes/java/main/");
        assertThatClassIsPofInstrumented(foo);

        File pofIndexFile = new File(m_gradleProjectRootDirectory, "build/classes/java/main/META-INF/pof.idx");
        assertTrue(pofIndexFile.exists(), "The pof.idx should exist at " + pofIndexFile.getAbsolutePath());
        assertTrue(pofIndexFile.isFile());

        Properties properties = new Properties();
        properties.load(new FileReader(pofIndexFile));

        assertFalse(properties.isEmpty());
        assertTrue(properties.containsKey("foo.Foo"));
        assertTrue(properties.get("foo.Foo").equals("1000"));

        }

    @Test
    void applyBasicCoherenceGradlePluginWithoutPofIndexing() throws IOException {
        setupGradlePropertiesFile(m_gradleProjectRootDirectory);
        setupGradleSettingsFile(m_gradleProjectRootDirectory, "settings", f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo());
        setupGradleBuildFile(m_gradleProjectRootDirectory, "applyWithoutIndexing",
                f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo(),
                f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                f_coherenceBuildTimeProperties.getCoherenceVersion());

        copyFileTo("/Foo.txt", m_gradleProjectRootDirectory,
                "/src/main/java/foo", "Foo.java");

        BuildResult gradleResult = GradleRunner.create()
                .withProjectDir(m_gradleProjectRootDirectory)
                .withArguments("build", "--info")
                .withDebug(true)
                .withPluginClasspath()
                .build();

        logOutput(gradleResult);

        assertSuccess(gradleResult, ":build");

        String sOutput = gradleResult.getOutput();
        assertThat(sOutput, containsString("Instrumenting type foo.Foo"));

        Class<?> foo = getPofClass(this.m_gradleProjectRootDirectory, "foo.Foo", "build/classes/java/main/"); // "build/classes/java/main/"
        assertThatClassIsPofInstrumented(foo);

        File pofIndexFile = new File(m_gradleProjectRootDirectory, "build/classes/java/main/META-INF/pof.idx");
        assertFalse(pofIndexFile.exists(), "The pof.idx should NOT exist at " + pofIndexFile.getAbsolutePath());
        }

    @Test
    void applyCoherenceGradlePluginWithTestClass()
        {
        setupGradlePropertiesFile(m_gradleProjectRootDirectory);
        setupGradleSettingsFile(m_gradleProjectRootDirectory, "settings", f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo());
        setupGradleBuildFile(m_gradleProjectRootDirectory, "applyCoherenceGradlePluginWithTestClass",
                             f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                             f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo(),
                             f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                             f_coherenceBuildTimeProperties.getCoherenceVersion());

        copyFileTo("/Foo.txt", m_gradleProjectRootDirectory,
                   "/src/main/java/foo", "Foo.java");

        copyFileTo("/Bar.txt", m_gradleProjectRootDirectory,
                   "/src/test/java", "Bar.java");

        copyFileTo("/Color.txt", m_gradleProjectRootDirectory,
                   "/src/test/java", "Color.java");

        BuildResult gradleResult = GradleRunner.create()
                .withProjectDir(m_gradleProjectRootDirectory)
                .withArguments("test", "--info")
                .withDebug(true)
                .withPluginClasspath()
                .build();

        logOutput(gradleResult);

        assertSuccess(gradleResult, ":test");

        // Class<?> foo = getPofClass(this.m_gradleProjectRootDirectory, "Foo", "build/pof-instrumented-classes/");
        Class<?> bar = getPofClass(this.m_gradleProjectRootDirectory, "Bar", "build/classes/java/test/");

        // assertThatClassIsPofInstrumented(foo);
        assertThatClassIsPofInstrumented(bar);
        }

    @Test
    void applyCoherenceGradlePluginWithClassAndSchemaInCustomResourcesFolder()
        {
        setupGradlePropertiesFile(m_gradleProjectRootDirectory);
        setupGradleSettingsFile(m_gradleProjectRootDirectory, "settings", f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo());
        setupGradleBuildFile(m_gradleProjectRootDirectory, "applyCoherenceGradlePluginWithClassAndSchemaInCustomResourcesFolder",
                             f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                             f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo(),
                             f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                             f_coherenceBuildTimeProperties.getCoherenceVersion());

        copyFileTo("/Foo.txt", m_gradleProjectRootDirectory,
                   "/src2/foo", "Foo.java");
        copyFileTo("/Bar.txt", m_gradleProjectRootDirectory,
                   "/src2", "Bar.java");
        copyFileTo("/Color.txt", m_gradleProjectRootDirectory,
                   "/src2", "Color.java");
        copyFileTo("/test-schema.xml", m_gradleProjectRootDirectory,
                   "/resources2/META-INF", "schema.xml");

        BuildResult gradleResult = GradleRunner.create()
                .withProjectDir(m_gradleProjectRootDirectory)
                .withArguments("compileJava", "--info")
                .withDebug(true)
                .withPluginClasspath()
                .build();

        logOutput(gradleResult);

        assertSuccess(gradleResult, ":compileJava");

        // we need to ensure file separators are correct for the build environment
        String expectedSchema = "resources2/META-INF/schema.xml".replace("/", File.separator);
        String expectedMain = "build/classes/java/main/".replace("/", File.separator);

        String sOutput = gradleResult.getOutput();
        assertThat(sOutput, stringContainsInOrder("Add XmlSchemaSource", expectedSchema));
        assertThat(sOutput, containsString(expectedSchema));
        assertThat(sOutput, containsString("Instrumenting type Bar"));
        assertThat(sOutput, containsString("Instrumenting type foo.Foo"));
        assertThat(sOutput, containsString("SUCCESS"));

        Class<?> foo = getPofClass(this.m_gradleProjectRootDirectory, "foo.Foo", expectedMain);
        assertThatClassIsPofInstrumented(foo);

        Class<?> bar = getPofClass(this.m_gradleProjectRootDirectory, "Bar", expectedMain);
        assertThatClassIsPofInstrumented(bar);
        }

    @Test
    void testNonExistingSchemaInMultipleCustomResourcesFolders()
        {
        setupGradlePropertiesFile(m_gradleProjectRootDirectory);
        setupGradleSettingsFile(m_gradleProjectRootDirectory, "settings", f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo());
        setupGradleBuildFile(m_gradleProjectRootDirectory, "testNonExistingSchemaInMultipleCustomResourcesFolders",
                f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo(),
                f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                f_coherenceBuildTimeProperties.getCoherenceVersion());

        copyFileTo("/Foo.txt", m_gradleProjectRootDirectory,
                "/src2/foo", "Foo.java");
        copyFileTo("/Bar.txt", m_gradleProjectRootDirectory,
                "/src2", "Bar.java");
        copyFileTo("/Color.txt", m_gradleProjectRootDirectory,
                "/src2", "Color.java");

        try
            {
            GradleRunner.create()
                    .withProjectDir(m_gradleProjectRootDirectory)
                    .withArguments("compileJava", "--info")
                    .withDebug(true)
                    .withPluginClasspath()
                    .build();

            }
        catch (UnexpectedBuildFailure ex)
            {
            String expectedSchema = "META-INF/schema.xml";
            LOGGER.info(ex.getMessage());
            assertThat(ex.getMessage(), containsString("The declared schemaSource XML file '" + expectedSchema + "' does not exist " +
                    "in the provided 2 resource folder(s)."));
            return;
            }
        fail("Expected an UnexpectedBuildFailure exception to be thrown.");
        }

    @Test
    void applyCoherenceGradlePluginWithClassAndSchema()
        {
        setupGradlePropertiesFile(m_gradleProjectRootDirectory);
        setupGradleSettingsFile(m_gradleProjectRootDirectory, "settings", f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo());
        setupGradleBuildFile(m_gradleProjectRootDirectory, "applyCoherenceGradlePluginWithClassAndSchema",
                f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo(),
                f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                f_coherenceBuildTimeProperties.getCoherenceVersion());

        copyFileTo("/Foo.txt", m_gradleProjectRootDirectory,
                "/src/main/java", "Foo.java");
        copyFileTo("/Bar.txt", m_gradleProjectRootDirectory,
                "/src/main/java", "Bar.java");
        copyFileTo("/Color.txt", m_gradleProjectRootDirectory,
                "/src/main/java", "Color.java");
        copyFileTo("/test-schema.xml", m_gradleProjectRootDirectory,
                "/src/main/resources/META-INF", "schema.xml");

        BuildResult gradleResult = GradleRunner.create()
                .withProjectDir(m_gradleProjectRootDirectory)
                .withArguments("compileJava", "--info")
                .withDebug(true)
                .withPluginClasspath()
                .build();

        logOutput(gradleResult);

        assertSuccess(gradleResult, ":compileJava");

        // we need to ensure file separators are correct for the build environment
        String expectedSchema = "src/main/resources/META-INF/schema.xml".replace("/", File.separator);
        String expectedMain = "build/classes/java/main/".replace("/", File.separator);

        String sOutput = gradleResult.getOutput();
        assertThat(sOutput, stringContainsInOrder("Add XmlSchemaSource", expectedSchema));
        assertThat(sOutput, containsString(expectedSchema));
        assertThat(sOutput, containsString("Instrumenting type Bar"));
        assertThat(sOutput, containsString("Instrumenting type foo.Foo"));
        assertThat(sOutput, containsString("SUCCESS"));

        Class<?> foo = getPofClass(this.m_gradleProjectRootDirectory, "foo.Foo", expectedMain);
        assertThatClassIsPofInstrumented(foo);

        Class<?> bar = getPofClass(this.m_gradleProjectRootDirectory, "Bar", expectedMain);
        assertThatClassIsPofInstrumented(bar);
        }

    @Test
    @DisabledOnOs(value = OS.WINDOWS, disabledReason =
            "GradleRunner does not seem to release all file handles. See https://github.com/gradle/gradle/issues/12535")
    void applyCoherenceGradlePluginWithJarDependency()
        {
        setupGradlePropertiesFile(m_gradleProjectRootDirectory);
        setupGradleSettingsFile(m_gradleProjectRootDirectory, "settings", f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo());
        setupGradleBuildFile(m_gradleProjectRootDirectory, "applyCoherenceGradlePluginWithJarDependency",
                             f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                             f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo(),
                             f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                             f_coherenceBuildTimeProperties.getCoherenceVersion());

        copyFileTo("/foo.jar", m_gradleProjectRootDirectory,
                   "/lib", "foo.jar");
        copyFileTo("/Bar.txt", m_gradleProjectRootDirectory,
                   "/src/main/java", "Bar.java");

        BuildResult gradleResult = GradleRunner.create()
                .withProjectDir(m_gradleProjectRootDirectory)
                .withArguments("coherencePof", "--info")
                .withDebug(true)
                .withPluginClasspath()
                .build();

        logOutput(gradleResult);

        assertSuccess(gradleResult);
        assertThat(gradleResult.getOutput(), containsString("foo.jar to schema"));
        }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void verifyCoherenceGradlePluginWithRoundTripSerialization()
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException
        {
        setupGradlePropertiesFile(m_gradleProjectRootDirectory);
        setupGradleSettingsFile(m_gradleProjectRootDirectory, "settings", f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo());
        setupGradleBuildFile(m_gradleProjectRootDirectory, "verifyCoherenceGradlePluginWithRoundTripSerialization",
                             f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                             f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo(),
                             f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                             f_coherenceBuildTimeProperties.getCoherenceVersion());

        copyFileTo("/Person.txt", m_gradleProjectRootDirectory,
                   "/src/main/java", "Person.java");

        BuildResult gradleResult = GradleRunner.create()
                .withProjectDir(m_gradleProjectRootDirectory)
                .withArguments("build", "--info")
                .withDebug(true)
                .withPluginClasspath()
                .build();

        logOutput(gradleResult);

        assertSuccess(gradleResult, ":build");

        Class<?> personClass = getPofClass(this.m_gradleProjectRootDirectory, "Person", "build/classes/java/main/");
        assertThatClassIsPofInstrumented(personClass);
        Class<?> addressClass = personClass.getClasses()[0];
        SimplePofContext ctx = new SimplePofContext();

        ctx.registerUserType(1000, personClass, new PortableTypeSerializer(1000, personClass));
        ctx.registerUserType(2, addressClass, new PortableTypeSerializer(2, addressClass));

        Constructor<?> constructor = personClass.getDeclaredConstructor(String.class, String.class, int.class);
        Object         oValue      = constructor.newInstance("Eric", "Cartman", 10);

        Constructor<?> addressConstructor = addressClass.getDeclaredConstructor(String.class, String.class, String.class);
        Object         addressInstance = addressConstructor.newInstance("123 Main St", "Springfield", "USA");

        Method setAddressMethod = personClass.getMethod("setAddress", addressClass);
        setAddressMethod.invoke(oValue, addressInstance);
        Binary binary  = ExternalizableHelper.toBinary(oValue, ctx);
        Object oResult = ExternalizableHelper.fromBinary(binary, ctx);

        assertThat(oResult, is(oValue));
        }

    @Test
    @DisabledOnOs(value = OS.WINDOWS, disabledReason =
            "GradleRunner does not seem to release all file handles. See https://github.com/gradle/gradle/issues/12535")
    void verifyCoherenceGradlePluginWithMultiProject() throws IOException
        {
        final String projectFolder = "test-multi-project";
        LOGGER.info("Copy '{}' to '{}'.", projectFolder, m_gradleProjectRootDirectory.getAbsolutePath());
        copyDirectory(projectFolder, m_gradleProjectRootDirectory.getAbsolutePath());

        BuildResult gradleResult = GradleRunner.create()
                .withProjectDir(m_gradleProjectRootDirectory)
                .withArguments("clean", "build", "--debug")
                .withDebug(true)
                .withPluginClasspath()
                .build();
        logOutput(gradleResult);
        assertSuccess(gradleResult, ":core:compileJava");
        assertSuccess(gradleResult, ":app:compileJava");

        assertThat(gradleResult.getOutput(), containsString("Instrumenting type coherence.pof.core.FooClass"));
        assertThat(gradleResult.getOutput(), containsString("Instrumenting type coherence.pof.app.PofClass"));
        assertThat(gradleResult.getOutput(), containsString("core.jar to schema"));

        final Class<?> fooClass = getPofClass(this.m_gradleProjectRootDirectory, "coherence.pof.core.FooClass", "core/build/classes/java/main");
        assertThatClassIsPofInstrumented(fooClass);

        final Class<?> publisherClass = getPofClass(this.m_gradleProjectRootDirectory, "coherence.pof.support.Publisher", "core/build/classes/java/main");
        assertThatClassIsPofInstrumented(publisherClass);

        final Class<?> pofClass = getPofClass(this.m_gradleProjectRootDirectory, "coherence.pof.app.PofClass", "app/build/classes/java/main");
        assertThatClassIsPofInstrumented(pofClass);

        Set<String> pofIndexedClasses = getPofIndexedClasses(this.m_gradleProjectRootDirectory, "core/build/classes/java/main");
        assertThat(pofIndexedClasses.size(), is(2));
        assertThat(pofIndexedClasses, containsInAnyOrder("coherence.pof.core.FooClass", "coherence.pof.support.Publisher"));
        }

    @Test
    @DisabledOnOs(value = OS.WINDOWS, disabledReason =
            "GradleRunner does not seem to release all file handles. See https://github.com/gradle/gradle/issues/12535")
    void verifyCoherenceGradlePluginWithMultiProjectAndPackageFilter() throws IOException
    {
    final String projectFolder = "test-multi-project";
    LOGGER.info("Copy '{}' to '{}'.", projectFolder, m_gradleProjectRootDirectory.getAbsolutePath());
    copyDirectory(projectFolder, m_gradleProjectRootDirectory.getAbsolutePath());

    BuildResult gradleResult = GradleRunner.create()
            .withProjectDir(m_gradleProjectRootDirectory)
            .withArguments("clean", "build", "--debug", "-PpofIndexPackages=coherence.pof.support")
            .withDebug(true)
            .withPluginClasspath()
            .build();
    logOutput(gradleResult);
    assertSuccess(gradleResult, ":core:compileJava");
    assertSuccess(gradleResult, ":app:compileJava");

    assertThat(gradleResult.getOutput(), containsString("Instrumenting type coherence.pof.core.FooClass"));
    assertThat(gradleResult.getOutput(), containsString("Instrumenting type coherence.pof.app.PofClass"));
    assertThat(gradleResult.getOutput(), containsString("core.jar to schema"));

    final Class<?> fooClass = getPofClass(this.m_gradleProjectRootDirectory, "coherence.pof.core.FooClass", "core/build/classes/java/main");
    assertThatClassIsPofInstrumented(fooClass);

    final Class<?> publisherClass = getPofClass(this.m_gradleProjectRootDirectory, "coherence.pof.support.Publisher", "core/build/classes/java/main");
    assertThatClassIsPofInstrumented(publisherClass);

    final Class<?> pofClass = getPofClass(this.m_gradleProjectRootDirectory, "coherence.pof.app.PofClass", "app/build/classes/java/main");
    assertThatClassIsPofInstrumented(pofClass);

    Set<String> pofIndexedClasses = getPofIndexedClasses(this.m_gradleProjectRootDirectory, "core/build/classes/java/main");
    assertThat(pofIndexedClasses.size(), is(1));
    assertThat(pofIndexedClasses, containsInAnyOrder("coherence.pof.support.Publisher"));
    }

    // ----- helper methods -------------------------------------------------

    public void copyDirectory(String testProjectFolder, String destinationDirectoryLocation)
            throws IOException
        {
        final Path sourceDirectoryLocation = Paths.get("src","test","resources", "test-projects", testProjectFolder);
        final File sourceDirectoryLocationAsFile = sourceDirectoryLocation.toFile();
        if (!sourceDirectoryLocationAsFile.exists())
            {
            throw new IllegalStateException(String.format("The directory '%s' does not exist.",
                    sourceDirectoryLocationAsFile.getAbsolutePath()));
            }

        try (Stream<Path> paths = Files.walk(sourceDirectoryLocation))
            {
            paths.forEach(source ->
                    {
                    if (source.getFileName().toString().equals(testProjectFolder))
                        {
                        return;
                        }
                    final Path destination = Paths.get(destinationDirectoryLocation, source.toString()
                            .substring(sourceDirectoryLocation.toString().length()));
                    if (source.toFile().isDirectory())
                        {
                        destination.toFile().mkdir();
                        LOGGER.info("Created directory '{}'.", destination);
                        return;
                        }

                    LOGGER.info("'{}' -> '{}'.", source.toString(), destination.toString());

                    if ("gradle.properties".equals(source.getFileName().toString()))
                        {
                        TestUtils.setupGradlePropertiesFileInDirectory(destination.toFile());
                        }
                    else if ("settings.gradle".equals(source.getFileName().toString()))
                        {
                        TestUtils.copyTemplatedFile(source.toFile(), destination.toFile(),
                                f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo());
                        }
                    else if ("build.gradle".equals(source.getFileName().toString()))
                        {
                        TestUtils.copyTemplatedFile(source.toFile(), destination.toFile(),
                                f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                                f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo(),
                                f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                                f_coherenceBuildTimeProperties.getCoherenceVersion());
                        }
                    else
                        {
                        TestUtils.copyUsingPaths(source, destination);
                        }
                    });
            }
        }

    void assertSuccess(BuildResult gradleResult)
        {
        assertSuccess(gradleResult, ":coherencePof");
        }

    void assertSuccess(BuildResult gradleResult, String sTaskName)
        {
        BuildTask task = gradleResult.task(sTaskName);
        assertThat(task, is(notNullValue()));
        TaskOutcome outcome = task.getOutcome();
        assertThat(outcome, is(notNullValue()));
        assertThat(outcome.name(), is("SUCCESS"));
        }
    void assertUpToDate(BuildResult gradleResult)
        {
        BuildTask task = gradleResult.task(":compileJava");
        assertThat(task, is(notNullValue()));
        TaskOutcome outcome = task.getOutcome();
        assertThat(outcome, is(notNullValue()));
        assertThat(outcome.name(), is("UP_TO_DATE"));
        }


    void logOutput(BuildResult gradleResult)
        {
        LOGGER.info(
                "\n-------- [ Gradle output] -------->>>>\n"
                + gradleResult.getOutput()
                + "<<<<------------------------------------"
                );
        }

    // ----- data members ---------------------------------------------------

    private static final Logger LOGGER = LoggerFactory.getLogger(CoherencePluginTests.class);

    @TempDir
    private File m_gradleProjectRootDirectory;

    private final CoherenceBuildTimeProperties f_coherenceBuildTimeProperties = new CoherenceBuildTimeProperties();
    }
