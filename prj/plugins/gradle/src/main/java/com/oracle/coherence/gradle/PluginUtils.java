/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import org.gradle.api.Project;

import org.gradle.api.file.Directory;

import org.gradle.api.logging.Logger;

import org.gradle.api.plugins.JavaPluginExtension;

import org.gradle.api.tasks.SourceSet;

import java.io.File;

import java.util.Objects;

/**
 * Contains various utility methods used by the Coherence Gradle Plugin.
 *
 * @author Gunnar Hillert  2023.03.16
 * @since 22.06.05
 */
public final class PluginUtils
    {
    //----- constructors ----------------------------------------------------

    /**
     * Private constructor for utility class.
     */
    private PluginUtils()
        {
        throw new AssertionError("This is a static utility class.");
        }

    //----- PluginUtils methods ---------------------------------------------

    /**
     * Returns the major Gradle version used by the Gradle {@link Project}.
     *
     * @param project the provided Gradle project must not be null
     *
     * @return the major Gradle version
     */
    static int getGradleMajorVersion(Project project)
        {
        Objects.requireNonNull(project, "The provided project must not be null.");

        String sGradleVersion = project.getGradle().getGradleVersion();
        return Integer.parseInt(sGradleVersion.substring(0, sGradleVersion.indexOf(".")));
        }

    /**
     * Returns the Gradle project's output directory for main classes.
     *
     * @param project the provided Gradle project must not be null
     *
     * @return the output directory or null if not available
     */
    static Directory getMainJavaOutputDir(Project project)
        {
        Objects.requireNonNull(project, "The provided project must not be null.");

        JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        SourceSet           sourceSet           = javaPluginExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        Directory           fileClassesDir      = sourceSet.getJava().getClassesDirectory().getOrNull();
        Logger              logger              = project.getLogger();

        if (fileClassesDir == null)
            {
            logger.warn("Main Java output directory not available.");
            }
        else
            {
            logger.warn("Main Java output directory: {}.", fileClassesDir.getAsFile().getAbsolutePath());
            }
        return fileClassesDir;
        }

    /**
     * Returns the Gradle project's output directory for main resources.
     *
     * @param project the provided Gradle project must not be null
     *
     * @return the output directory or null if not available
     */
    static File getMainResourcesOutputDir(Project project)
        {
        Objects.requireNonNull(project, "The provided project must not be null.");

        JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        SourceSet           sourceSet           = javaPluginExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        File                fileResourcesDir    = sourceSet.getOutput().getResourcesDir();
        Logger              logger              = project.getLogger();

        if (fileResourcesDir == null)
            {
            logger.warn("Main Resources output directory not available.");
            }
        else
            {
            logger.warn("Main Resources output directory: {}.", fileResourcesDir.getAbsolutePath());
            }

        return fileResourcesDir;
        }

    /**
     * Returns the Gradle project's output directory for test classes.
     *
     * @param project the provided Gradle project must not be null
     *
     * @return the output directory or null if not available
     */
    static Directory getTestJavaOutputDir(Project project)
        {
        Objects.requireNonNull(project, "The provided project must not be null.");

        JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        SourceSet           sourceSet           = javaPluginExtension.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);
        Directory           fileClassesDir      = sourceSet.getJava().getClassesDirectory().getOrNull();
        Logger              logger              = project.getLogger();

        if (fileClassesDir == null)
            {
            logger.warn("Test Java output directory not available.");
            }
        else
            {
            logger.warn("Test Java output directory: {}.", fileClassesDir.getAsFile().getAbsolutePath());
            }

        return fileClassesDir;
        }

    /**
     * Returns the Gradle project's output directory for test resources.
     *
     * @param project the provided Gradle project must not be null
     *
     * @return the output directory or null if not available
     */
    static File getTestResourcesOutputDir(Project project)
        {
        Objects.requireNonNull(project, "The provided project must not be null.");

        JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        SourceSet           sourceSet           = javaPluginExtension.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);
        File                fileResourcesDir    = sourceSet.getOutput().getResourcesDir();
        Logger              logger              = project.getLogger();

        if (fileResourcesDir == null)
            {
            logger.warn("Test Resources output directory not available.");
            }
        else
            {
            logger.warn("Test Resources output directory: {}.", fileResourcesDir.getAbsolutePath());
            }

        return fileResourcesDir;
        }
    }
