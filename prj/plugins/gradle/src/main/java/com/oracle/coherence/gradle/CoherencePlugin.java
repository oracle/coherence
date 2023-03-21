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
import org.gradle.api.tasks.compile.JavaCompile;

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

        // The minimum supported version of the Coherence Gradle plugin is 7.x or higher
        if (PluginUtils.getGradleMajorVersion(project) < MINIMAL_SUPPORTED_GRADLE_VERSION)
            {
            throw new GradleException("The Coherence Gradle plugin requires Gradle version 7 or higher.");
            }

        if (!project.getPluginManager().hasPlugin("java"))
            {
            throw new GradleException("The Java Gradle plugin has not been applied.");
            }

        project.getExtensions().create(POF_TASK_NAME, CoherenceExtension.class);
        project.getTasks().register(POF_TASK_NAME, CoherenceTask.class, coherencePofTask ->
            {
            coherencePofTask.dependsOn("compileJava", "processResources");

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
                coherencePofTask.dependsOn("compileTestJava", "processTestResources");
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
        }

    // ----- constants ------------------------------------------------------

    /**
     * The Gradle task name used by the Coherence Gradle Plugin to provide additional configuration properties.
     */
    private static final String POF_TASK_NAME = "coherencePof";

    /**
     * Constant defining the minimally supported Gradle version.
     */
    private static final int MINIMAL_SUPPORTED_GRADLE_VERSION = 7;
    }
