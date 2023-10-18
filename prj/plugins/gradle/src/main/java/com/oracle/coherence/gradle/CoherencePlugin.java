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
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.jvm.tasks.ProcessResources;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        project.getExtensions().create(POF_TASK_NAME, CoherenceExtension.class);

        final CoherenceExtension  coherenceExtension  = project.getExtensions().getByType(CoherenceExtension.class);
        final JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        final TaskContainer       taskContainer       = project.getTasks();

        // Main

        final JavaCompile                    javaCompileTask                              = (JavaCompile) project.getTasks().findByName(JavaPlugin.COMPILE_JAVA_TASK_NAME);
        final TaskProvider<ProcessResources> processResourcesTaskProvider                 = taskContainer.named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME, ProcessResources.class);
        final Provider<File>                 processResourcesDestinationDirProvider       = processResourcesTaskProvider.map(processResources -> processResources.getDestinationDir());
        final SourceSet                      mainSourceSet                                = javaPluginExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        final SourceSetOutput                mainSourceSetOutput                          = mainSourceSet.getOutput();

        final CoherenceTaskRegistrationAction coherenceTaskRegistrationAction = new CoherenceTaskRegistrationAction(
                coherenceExtension, project, javaCompileTask, processResourcesDestinationDirProvider);
        final TaskProvider<CoherenceTask>     coherenceTaskProvider           = taskContainer.register(POF_TASK_NAME, CoherenceTask.class, coherenceTaskRegistrationAction);

        // Test

        final JavaCompile                    javaTestCompileTask                        = (JavaCompile) project.getTasks().findByName(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME);
        final TaskProvider<ProcessResources> processTestResourcesTaskProvider           = taskContainer.named(JavaPlugin.PROCESS_TEST_RESOURCES_TASK_NAME, ProcessResources.class);
        final Provider<File>                 processTestResourcesDestinationDirProvider = processTestResourcesTaskProvider.map(Copy::getDestinationDir);
        final SourceSet                      testSourceSet                              = javaPluginExtension.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);
        final SourceSetOutput                testSourceSetOutput                        = testSourceSet.getOutput();

        final CoherenceTestTaskRegistrationAction coherenceTestTaskRegistrationAction = new CoherenceTestTaskRegistrationAction(
                coherenceExtension, project, javaTestCompileTask, processTestResourcesDestinationDirProvider);
        final TaskProvider<CoherenceTask>         coherenceTestTaskProvider           = taskContainer.register(POF_TEST_TASK_NAME, CoherenceTask.class, coherenceTestTaskRegistrationAction);

        final Directory           oldJavaDestinationDirectory = mainSourceSet.getJava().getDestinationDirectory().get();
        final Provider<Directory> newJavaDestinationDirectory = project.getLayout().getBuildDirectory().dir(DEFAULT_POF_CLASSES_OUTPUT_DIRECTORY);

        project.getLogger().info("Change Java Destination Directory for subsequent tasks from '{}' to '{}'.",
                oldJavaDestinationDirectory.getAsFile().getAbsolutePath(),
                newJavaDestinationDirectory.get().getAsFile().getAbsolutePath());
        project.getLogger().info("mainSourceSetOutput.getResourcesDir() '{}'", mainSourceSetOutput.getResourcesDir().getAbsolutePath());

        javaCompileTask.getDestinationDirectory().set(oldJavaDestinationDirectory);
        mainSourceSet.getJava().getDestinationDirectory().set(newJavaDestinationDirectory);

        project.afterEvaluate(evaluatedProject ->
            {
            final TaskContainer evaluatedTaskContainer = evaluatedProject.getTasks();
            final Set<String>   names                  = evaluatedTaskContainer.getNames();
            final Set<String>   nameFilter             = new HashSet<>();

            nameFilter.add(POF_TASK_NAME);
            nameFilter.add(POF_TEST_TASK_NAME);
            nameFilter.add(JavaPlugin.COMPILE_JAVA_TASK_NAME);
            nameFilter.add(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME);
            nameFilter.add(JavaPlugin.CLASSES_TASK_NAME);

            final List<String> filteredTaskNames = names.stream().filter(taskName -> !nameFilter.contains(taskName)).toList();
            for (String name : filteredTaskNames)
                {
                final Task task = evaluatedTaskContainer.getByName(name);
                if (javaCompileTask != null && task.getTaskDependencies().getDependencies(task).stream().anyMatch(javaCompileTask::equals))
                    {
                    evaluatedProject.getLogger().info("'{}' will depend on '{}'.", task.getName(), coherenceTaskProvider.getName());
                    task.dependsOn(coherenceTaskProvider);
                    }
                if (Boolean.TRUE.equals(coherenceExtension.getInstrumentTestClasses().getOrElse(false))
                        && javaTestCompileTask != null
                        && task.getTaskDependencies().getDependencies(task).stream().anyMatch(javaTestCompileTask::equals))
                    {
                    evaluatedProject.getLogger().info("'{}' will depend on '{}'.", task.getName(), coherenceTaskProvider.getName());
                    task.dependsOn(coherenceTestTaskProvider);
                    }
                }
            });
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
     * The default output directory for instrumented classes.
     */
    public static final String DEFAULT_POF_CLASSES_OUTPUT_DIRECTORY = "pof-instrumented-classes";

    /**
     * The default output directory for instrumented test classes.
     */
    public static final String DEFAULT_POF_TEST_CLASSES_OUTPUT_DIRECTORY = "pof-instrumented-test-classes";

    /**
     * Constant defining the minimally supported Gradle version.
     */
    private static final int MINIMAL_SUPPORTED_GRADLE_VERSION = 8;
    }
