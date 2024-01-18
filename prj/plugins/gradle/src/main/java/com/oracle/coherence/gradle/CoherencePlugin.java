/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import org.gradle.api.file.FileCollection;

import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;

import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

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
    //----- constructors ----------------------------------------------------

    /**
     * Default constructor for the CoherencePlugin.
     */
    public CoherencePlugin()
        {
        }

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

        final CoherenceExtension  coherenceExtension  = project.getExtensions().create(POF_TASK_NAME, CoherenceExtension.class);;
        final JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        final TaskContainer       taskContainer       = project.getTasks();

        // Main

        final JavaCompile                    javaCompileTask                              = (JavaCompile) project.getTasks().findByName(JavaPlugin.COMPILE_JAVA_TASK_NAME);
        final SourceSet                      mainSourceSet                                = javaPluginExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        final FileCollection                 resourcesFolders                             = mainSourceSet.getResources().getSourceDirectories();

        final CoherenceTaskRegistrationAction coherenceTaskRegistrationAction = new CoherenceTaskRegistrationAction(
                coherenceExtension, project, javaCompileTask, resourcesFolders);
        final TaskProvider<CoherenceTask>    coherenceTaskProvider            = taskContainer.register(POF_TASK_NAME, CoherenceTask.class, coherenceTaskRegistrationAction);

        // Test

        final JavaCompile                    javaTestCompileTask                        = (JavaCompile) project.getTasks().findByName(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME);
        final SourceSet                      testSourceSet                              = javaPluginExtension.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);
        final FileCollection                 testResourcesFolders                       = testSourceSet.getResources().getSourceDirectories();

        final CoherenceTestTaskRegistrationAction coherenceTestTaskRegistrationAction = new CoherenceTestTaskRegistrationAction(
              coherenceExtension, project, javaTestCompileTask, testResourcesFolders);
        final TaskProvider<CoherenceTask>         coherenceTestTaskProvider           = taskContainer.register(POF_TEST_TASK_NAME, CoherenceTask.class, coherenceTestTaskRegistrationAction);

        project.afterEvaluate(evaluatedProject ->
                project.getPlugins().withType(JavaPlugin.class).forEach(javaPlugin -> {
                    project.getTasks().getByName("compileJava").doLast(e ->
                        {
                        project.getLogger().info("Run coherencePof at the end of task {}.", e.getName());
                        coherenceTaskProvider.get().instrumentPofClasses();
                        });

                    if (coherenceExtension.getInstrumentTestClasses().getOrElse(Boolean.FALSE))
                        {
                        project.getTasks().getByName("compileTestJava").doLast(e ->
                            {
                            project.getLogger().info("Run coherencePofTest at the end of task {}.", e.getName());
                            coherenceTestTaskProvider.get().instrumentPofClasses();
                            });
                        }
                }));
        }

    // ----- constants ------------------------------------------------------

    /**
     * The Gradle task name used by the Coherence Gradle Plugin to instrument main classes.
     */
    public static final String POF_TASK_NAME = "coherencePof";

    /**
     * The Gradle task name used by the Coherence Gradle Plugin to instrument test classes.
     */
    public static final String POF_TEST_TASK_NAME = "coherencePofTest";

    /**
     * Constant defining the minimally supported Gradle version.
     */
    private static final int MINIMAL_SUPPORTED_GRADLE_VERSION = 8;

    }
