/*
 * Copyright (c) 2023, 2026, Oracle and/or its affiliates.
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

        final PortableTypesExtension  coherenceExtension      = project.getExtensions().create(PORTABLE_TYPES_TASK_NAME, PortableTypesExtension.class);
        final SecurityConfigExtension securityConfigExtension = project.getExtensions().create(SECURITY_CONFIG_TASK_NAME, SecurityConfigExtension.class);
        final JavaPluginExtension javaPluginExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        final TaskContainer       taskContainer       = project.getTasks();

        // Main

        final JavaCompile                    javaCompileTask                              = (JavaCompile) project.getTasks().findByName(JavaPlugin.COMPILE_JAVA_TASK_NAME);
        final SourceSet                      mainSourceSet                                = javaPluginExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        final FileCollection                 resourcesFolders                             = mainSourceSet.getResources().getSourceDirectories();

        final PortableTypesTaskRegistrationAction coherenceTaskRegistrationAction = new PortableTypesTaskRegistrationAction(
                coherenceExtension, project, javaCompileTask, resourcesFolders);
        final TaskProvider<PortableTypesTask>    coherenceTaskProvider            = taskContainer.register(PORTABLE_TYPES_TASK_NAME, PortableTypesTask.class, coherenceTaskRegistrationAction);

        final SecurityConfigTaskRegistrationAction securityConfigTaskRegistrationAction = new SecurityConfigTaskRegistrationAction(
                securityConfigExtension, project, javaCompileTask, resourcesFolders);
        final TaskProvider<SecurityConfigTask> securityConfigTaskProvider = taskContainer.register(
                SECURITY_CONFIG_TASK_NAME, SecurityConfigTask.class, securityConfigTaskRegistrationAction);

        // Test

        final JavaCompile                    javaTestCompileTask                        = (JavaCompile) project.getTasks().findByName(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME);
        final SourceSet                      testSourceSet                              = javaPluginExtension.getSourceSets().getByName(SourceSet.TEST_SOURCE_SET_NAME);
        final FileCollection                 testResourcesFolders                       = testSourceSet.getResources().getSourceDirectories();

        final PortableTypesTestTaskRegistrationAction coherenceTestTaskRegistrationAction = new PortableTypesTestTaskRegistrationAction(
              coherenceExtension, project, javaTestCompileTask, testResourcesFolders);
        final TaskProvider<PortableTypesTask>         coherenceTestTaskProvider           = taskContainer.register(PORTABLE_TYPES_TEST_TASK_NAME, PortableTypesTask.class, coherenceTestTaskRegistrationAction);

        final SecurityConfigTestTaskRegistrationAction securityConfigTestTaskRegistrationAction = new SecurityConfigTestTaskRegistrationAction(
                securityConfigExtension, project, javaTestCompileTask, testResourcesFolders);
        final TaskProvider<SecurityConfigTask> securityConfigTestTaskProvider = taskContainer.register(
                SECURITY_CONFIG_TEST_TASK_NAME, SecurityConfigTask.class, securityConfigTestTaskRegistrationAction);

        SecurityConfigShadowSupport.configure(project);

        project.afterEvaluate(evaluatedProject ->
                project.getPlugins().withType(JavaPlugin.class).forEach(javaPlugin -> {
                    project.getTasks().getByName("compileJava").doLast(e ->
                        {
                        project.getLogger().info("Run portableTypes at the end of task {}.", e.getName());
                        coherenceTaskProvider.get().instrumentPofClasses();
                        project.getLogger().info("Run securityConfig at the end of task {}.", e.getName());
                        securityConfigTaskProvider.get().generateSecurityConfig();
                        });

                    if (coherenceExtension.getInstrumentTestClasses().getOrElse(Boolean.FALSE))
                        {
                        project.getTasks().getByName("compileTestJava").doLast(e ->
                            {
                            project.getLogger().info("Run portableTypesTest at the end of task {}.", e.getName());
                            coherenceTestTaskProvider.get().instrumentPofClasses();
                            });
                        }

                    if (securityConfigExtension.getProcessTestClasses().getOrElse(Boolean.FALSE))
                        {
                        project.getTasks().getByName("compileTestJava").doLast(e ->
                            {
                            project.getLogger().info("Run securityConfigTest at the end of task {}.", e.getName());
                            securityConfigTestTaskProvider.get().generateSecurityConfig();
                            });
                        }
                }));
        }

    // ----- constants ------------------------------------------------------

    /**
     * The Gradle task name used by the Coherence Gradle Plugin to instrument main classes.
     */
    public static final String PORTABLE_TYPES_TASK_NAME = "portableTypes";

    /**
     * The Gradle task name used by the Coherence Gradle Plugin to instrument test classes.
     */
    public static final String PORTABLE_TYPES_TEST_TASK_NAME = "portableTypesTest";

    /**
     * The Gradle task name used by the Coherence Gradle Plugin to generate security config for main classes.
     */
    public static final String SECURITY_CONFIG_TASK_NAME = "securityConfig";

    /**
     * The Gradle task name used by the Coherence Gradle Plugin to generate security config for test classes.
     */
    public static final String SECURITY_CONFIG_TEST_TASK_NAME = "securityConfigTest";

    /**
     * Constant defining the minimally supported Gradle version.
     */
    private static final int MINIMAL_SUPPORTED_GRADLE_VERSION = 8;

    }
