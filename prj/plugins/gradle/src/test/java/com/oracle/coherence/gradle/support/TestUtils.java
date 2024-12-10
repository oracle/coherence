/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle.support;

import com.oracle.coherence.common.base.Exceptions;
import com.tangosol.io.pof.schema.annotation.internal.Instrumented;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import java.lang.annotation.Annotation;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Gunnar Hillert
 */
public class TestUtils
    {
    public static void appendToFile(File buildFile, String textToAppend)
        {
        try (FileWriter out = new FileWriter(buildFile, StandardCharsets.UTF_8, true))
            {
            out.write(textToAppend);
            }
        catch (IOException e)
            {
            throw Exceptions.ensureRuntimeException(e,
                    String.format("Something went wring while appending %s to file %s",
                            textToAppend, buildFile.getAbsoluteFile()));
            }
        }

    public static void copyUsingPaths(Path source, Path destination)
        {
        try
            {
            Files.copy(source, destination);
            }
        catch (IOException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    public static void copyFileTo(String sourceFileName, File root, String destinationDir, String destinationFilename)
        {
        File fileJavaDir = new File(root, destinationDir);
        if (!fileJavaDir.exists() && !fileJavaDir.mkdirs())
            {
            throw new IllegalStateException("Unable to create directory: " + fileJavaDir.getAbsolutePath());
            }

        URL url = TestUtils.class.getResource(sourceFileName);
        assertThat(url, is(notNullValue()));
        try
            {
            Files.copy(url.openStream(), new File(fileJavaDir, destinationFilename).toPath());
            }
        catch (IOException ex)
            {
            throw Exceptions.ensureRuntimeException(ex);
            }
        }

    public static void setupGradlePropertiesFileInDirectory(File gradlePropertiesFile)
            {
            String sProxyHost = System.getProperty("https.proxyHost");
            String sProxyPort = System.getProperty("https.proxyPort");

            if (sProxyHost == null)
                {
                return;
                }

            String sGradlePropertiesFileName = "gradle";
            String sPreparedBuildScript     = readToStringWithVariables(sGradlePropertiesFileName, ".properties",
                    sProxyHost,
                    sProxyHost,
                    sProxyPort,
                    sProxyPort
            );
            LOGGER.info(sPreparedBuildScript);
            appendToFile(gradlePropertiesFile, sPreparedBuildScript);
            }
    public static void setupGradlePropertiesFile(File gradleProjectRootDirectory)
        {
        File   fileBuild = new File(gradleProjectRootDirectory, "gradle.properties");
        setupGradlePropertiesFileInDirectory(fileBuild);
        }
    public static void setupGradleSettingsFile(File gradleProjectRootDirectory, String settingsFilename, Object... templateVariables)
        {
        File   fileBuild                    = new File(gradleProjectRootDirectory, "settings.gradle");
        String sPreparedGradleSettingsFile  = readToString(settingsFilename, templateVariables);
        LOGGER.info(sPreparedGradleSettingsFile);

        appendToFile(fileBuild, sPreparedGradleSettingsFile);
        }

    public static void setupGradleBuildFile(File gradleProjectRootDirectory, String gradleFilename, Object... templateVariables)
        {
        File   fileBuild            = new File(gradleProjectRootDirectory, "build.gradle");
        String sPreparedBuildScript = readToString(gradleFilename, templateVariables);
        LOGGER.info(sPreparedBuildScript);

        appendToFile(fileBuild, sPreparedBuildScript);
        }

    public static void copyTemplatedFile(File template, File destination, Object... templateVariables)
        {
        String sTemplate = readToString(template);
        String sResult = String.format(sTemplate, templateVariables);
        appendToFile(destination, sResult);
        }

    public static String readToString(String buildFileName, Object... templateVariables)
        {
        String sTemplate = readToString(buildFileName);
        return String.format(sTemplate, templateVariables);
        }

    public static String readToStringWithVariables(String buildFileName, String suffix, Object... templateVariables)
        {
        String sTemplate = readToString(buildFileName, suffix);
        return String.format(sTemplate, templateVariables);
        }

    public static String readToString(String buildFileName)
        {
            return readToString(buildFileName, ".gradle");
        }

    public static String readToString(String buildFileName, String suffix)
        {
        try
            {
            String sResource = "/build-files/" + buildFileName + suffix;
            URL    url       = TestUtils.class.getResource(sResource);
            Objects.requireNonNull(url, "InputStream cannot be null for resource " + sResource);

            return Files.readString(new File(url.toURI()).toPath());
            }
        catch (URISyntaxException | IOException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }
    public static String readToString(File fileToRead)
        {
        try
            {
            return Files.readString(fileToRead.toPath());
            }
        catch (IOException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }
    public static Class<?> getPofClass(File gradleProjectRootDirectory, String classname, String baseDirectory)
        {
        File fileClassDirectory = new File(gradleProjectRootDirectory, baseDirectory);

        assertThat(String.format("classDirectory %s does not exist.", fileClassDirectory.getAbsolutePath()), fileClassDirectory.exists(), is(true));
        assertThat(fileClassDirectory.isDirectory(), is(true));

        URL url;
        try
            {
            url = fileClassDirectory.toURI().toURL();
            }
        catch (MalformedURLException e)
            {
            throw new RuntimeException(e);
            }

        @SuppressWarnings("resource")
        ClassLoader classLoader = new URLClassLoader(new URL[]{url});
        try
            {
            return classLoader.loadClass(classname);
            }
        catch (ClassNotFoundException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    public static Set<String> getPofIndexedClasses(File gradleProjectRootDirectory, String baseDirectory)
        {
        File fileClassDirectory = new File(gradleProjectRootDirectory, baseDirectory);

        assertThat(String.format("classDirectory %s does not exist.", fileClassDirectory.getAbsolutePath()), fileClassDirectory.exists(), is(true));
        assertThat(fileClassDirectory.isDirectory(), is(true));

        URL url;
        try
        {
            url = fileClassDirectory.toURI().toURL();
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }

        Properties pofIndexProperties = new Properties();
        try
            {
            pofIndexProperties.load(new FileInputStream(new File(fileClassDirectory, "META-INF/pof.idx")));
            }
        catch (IOException e)
            {
            throw new RuntimeException(e);
            }

        return pofIndexProperties.keySet().stream().map(Object::toString).collect(Collectors.toSet());
        }

    public static void assertThatClassIsPofInstrumented(Class<?> pofClass)
        {
        assertThat(pofClass, is(notNullValue()));

        Annotation[] annotations = pofClass.getAnnotations();

        assertThat(String.format("Class '%s' should have 2 annotations,", pofClass.getName()), annotations.length, is(2));
        assertThat(pofClass.getAnnotation(Instrumented.class), is(notNullValue()));
        assertThat(pofClass.getInterfaces().length, is(1));
        }

    // ----- constants ------------------------------------------------------

    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class);
    }
