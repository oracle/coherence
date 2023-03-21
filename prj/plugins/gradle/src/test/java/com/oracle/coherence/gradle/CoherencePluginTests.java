/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import com.tangosol.io.pof.PortableTypeSerializer;
import com.tangosol.io.pof.SimplePofContext;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;

import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.text.StringContainsInOrder.stringContainsInOrder;

import static com.oracle.coherence.gradle.support.TestUtils.getPofClass;
import static com.oracle.coherence.gradle.support.TestUtils.assertThatClassIsPofInstrumented;
import static com.oracle.coherence.gradle.support.TestUtils.copyFileTo;
import static com.oracle.coherence.gradle.support.TestUtils.setupGradleBuildFile;


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

    @Test
    void applyBasicCoherenceGradlePluginWithNoSources()
        {
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
        assertThat(sOutput, containsString("PortableTypeGenerator skipping test classes directory as it does not exist."));
        assertThat(sOutput, containsString("PortableTypeGenerator skipping main classes directory as it does not exist."));
        }

    @Test
    void applyBasicCoherenceGradlePluginWithClass()
        {
        setupGradleBuildFile(m_gradleProjectRootDirectory, "applyBasicCoherenceGradlePluginWithClass",
                             f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                             f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo(),
                             f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                             f_coherenceBuildTimeProperties.getCoherenceVersion());

        copyFileTo("/Foo.txt", m_gradleProjectRootDirectory,
                   "/src/main/java", "Foo.java");

        BuildResult gradleResult = GradleRunner.create()
                .withProjectDir(m_gradleProjectRootDirectory)
                .withArguments("coherencePof")
                .withDebug(true)
                .withPluginClasspath()
                .build();

        logOutput(gradleResult);

        assertSuccess(gradleResult);

        String sOutput = gradleResult.getOutput();
        assertThat(sOutput, containsString("Instrumenting type Foo"));

        Class<?> foo = getPofClass(this.m_gradleProjectRootDirectory, "Foo", "build/classes/java/main/");
        assertThatClassIsPofInstrumented(foo);
        }

    @Test
    void applyCoherenceGradlePluginWithTestClass()
        {
        setupGradleBuildFile(m_gradleProjectRootDirectory, "applyCoherenceGradlePluginWithTestClass",
                             f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                             f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo(),
                             f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                             f_coherenceBuildTimeProperties.getCoherenceVersion());

        copyFileTo("/Foo.txt", m_gradleProjectRootDirectory,
                   "/src/main/java", "Foo.java");

        copyFileTo("/Bar.txt", m_gradleProjectRootDirectory,
                   "/src/test/java", "Bar.java");

        copyFileTo("/Color.txt", m_gradleProjectRootDirectory,
                   "/src/test/java", "Color.java");

        BuildResult gradleResult = GradleRunner.create()
                .withProjectDir(m_gradleProjectRootDirectory)
                .withArguments("coherencePof")
                .withDebug(true)
                .withPluginClasspath()
                .build();

        logOutput(gradleResult);

        assertSuccess(gradleResult);

        Class<?> foo = getPofClass(this.m_gradleProjectRootDirectory, "Foo", "build/classes/java/main/");
        Class<?> bar = getPofClass(this.m_gradleProjectRootDirectory, "Bar", "build/classes/java/test/");

        assertThatClassIsPofInstrumented(foo);
        assertThatClassIsPofInstrumented(bar);
        }

    @Test
    void applyCoherenceGradlePluginWithClassAndSchema()
        {
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
                .withArguments("coherencePof")
                .withDebug(true)
                .withPluginClasspath()
                .build();

        logOutput(gradleResult);

        assertSuccess(gradleResult);

        // we need to ensure file separators are correct for the build environment
        String expectedSchema = "build/resources/main/META-INF/schema.xml".replace("/", File.separator);
        String expectedMain = "build/classes/java/main/".replace("/", File.separator);

        String sOutput = gradleResult.getOutput();
        assertThat(sOutput, stringContainsInOrder("Add XmlSchemaSource", expectedSchema));
        assertThat(sOutput, containsString(expectedSchema));
        assertThat(sOutput, containsString("Instrumenting type Bar"));
        assertThat(sOutput, containsString("Instrumenting type Foo"));
        assertThat(sOutput, containsString("SUCCESS"));

        Class<?> foo = getPofClass(this.m_gradleProjectRootDirectory, "Foo", expectedMain);
        assertThatClassIsPofInstrumented(foo);

        Class<?> bar = getPofClass(this.m_gradleProjectRootDirectory, "Bar", expectedMain);
        assertThatClassIsPofInstrumented(bar);
        }

    @Test
    void applyCoherenceGradlePluginWithJarDependency()
        {
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
                .withArguments("coherencePof")
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
        setupGradleBuildFile(m_gradleProjectRootDirectory, "verifyCoherenceGradlePluginWithRoundTripSerialization",
                             f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                             f_coherenceBuildTimeProperties.getCoherenceLocalDependencyRepo(),
                             f_coherenceBuildTimeProperties.getCoherenceGroupId(),
                             f_coherenceBuildTimeProperties.getCoherenceVersion());

        copyFileTo("/Person.txt", m_gradleProjectRootDirectory,
                   "/src/main/java", "Person.java");

        BuildResult gradleResult = GradleRunner.create()
                .withProjectDir(m_gradleProjectRootDirectory)
                .withArguments("coherencePof")
                .withDebug(true)
                .withPluginClasspath()
                .build();

        logOutput(gradleResult);

        assertSuccess(gradleResult);

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

    // ----- helper methods -------------------------------------------------

    void assertSuccess(BuildResult gradleResult)
        {
        BuildTask task = gradleResult.task(":coherencePof");
        assertThat(task, is(notNullValue()));
        TaskOutcome outcome = task.getOutcome();
        assertThat(outcome, is(notNullValue()));
        assertThat(outcome.name(), is("SUCCESS"));
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
