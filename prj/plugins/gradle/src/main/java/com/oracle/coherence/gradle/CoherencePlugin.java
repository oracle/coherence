/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A Gradle plugin to generate Coherence PortableObject code.
 *
 * @author Gunnar Hillert  2023.03.16
 * @since 22.06.05
 */
public class CoherencePlugin
        implements Plugin<Project>
    {
    // ----- CoherencePlugin methods ----------------------------------------

    @Override
    public void apply(Project project)
        {
        project.getLogger().debug("Configuring the Coherence Gradle Plugin.");

        // The minimum supported version of the Coherence Gradle plugin is 8.x or higher
        if (PluginUtils.getGradleMajorVersion(project) < MINIMAL_SUPPORTED_GRADLE_VERSION)
            {
            throw new GradleException("The Coherence Gradle plugin requires Gradle version 8 or higher.");
            }

        if (!project.getPluginManager().hasPlugin("java"))
            {
            throw new GradleException("The Java Gradle plugin has not been applied.");
            }

        project.getExtensions().create(POF_TASK_NAME, CoherenceExtension.class);

        TaskProvider taskContainer = project.getTasks().register(POF_TASK_NAME, CoherenceTask.class, coherencePofTask ->
            {
            applyInitialConfig(project, coherencePofTask);
            final List<String> dependencies = new ArrayList<>();
            dependencies.add("compileJava");
            dependencies.add("compileTestJava");

            coherencePofTask.dependsOn(dependencies);

            final CoherenceExtension coherenceExtension = project.getExtensions().getByType(CoherenceExtension.class);

            if (coherenceExtension.getDebug().isPresent())
                {
                coherencePofTask.getDebug().set(coherenceExtension.getDebug().get());
                }

            if (coherenceExtension.getMainClassesDirectory().isPresent())
                {
                coherencePofTask.getMainClassesDirectory().set(coherenceExtension.getMainClassesDirectory().getAsFile());
                }

            if (coherenceExtension.getInstrumentTestClasses().isPresent())
                {
                coherencePofTask.getInstrumentTestClasses().set(coherenceExtension.getInstrumentTestClasses());
                }

            if (coherenceExtension.getTestClassesDirectory().isPresent())
                {
                coherencePofTask.getTestClassesDirectory().set(coherenceExtension.getTestClassesDirectory().getAsFile());
                }
            });

        final JavaCompile javaCompileTask = (JavaCompile) project.getTasks().findByName("compileJava");

        if (javaCompileTask != null)
            {
            javaCompileTask.finalizedBy(POF_TASK_NAME);
            }

        final Task javaCompileTestTask = project.getTasks().findByName("compileTestJava");

        if (javaCompileTestTask != null)
            {
            javaCompileTestTask.finalizedBy(POF_TASK_NAME);
            }
        }

        /**
         * Set up default values and
         * conventions for the properties of {@link CoherenceTask}.
         *
         * @param project gradle Project
         * @param coherenceTask the task to configure
         */
        private void applyInitialConfig(Project project, CoherenceTask coherenceTask)
            {
            project.getLogger().info("Setting up Task property conventions.");
            coherenceTask.getDebug().convention(false);
            coherenceTask.getInstrumentTestClasses().convention(false);

            Directory mainJavaOutputDir = PluginUtils.getMainJavaOutputDir(project);
            coherenceTask.getMainClassesDirectory().convention(mainJavaOutputDir.getAsFile());

            Directory testJavaOutputDir = PluginUtils.getTestJavaOutputDir(project);
            coherenceTask.getTestClassesDirectory().convention(testJavaOutputDir.getAsFile());

            File fileMainResourcesOutputDir = PluginUtils.getMainResourcesOutputDir(project);
            if (fileMainResourcesOutputDir != null)
                {
                coherenceTask.getSchemaSourceXmlFile().convention(fileMainResourcesOutputDir);
                }

            File testResourcesOutputDir = PluginUtils.getTestResourcesOutputDir(project);
            if (testResourcesOutputDir != null)
                {
                coherenceTask.getTestSchemaSourceXmlFile().convention(testResourcesOutputDir);
                }
            }

    // ----- constants ------------------------------------------------------

    /**
     * The Gradle task name used by the Coherence Gradle Plugin to provide additional configuration properties.
     */
    private static final String POF_TASK_NAME = "coherencePof";

    /**
     * Constant defining the minimally supported Gradle version.
     */
    private static final int MINIMAL_SUPPORTED_GRADLE_VERSION = 8;
    }
