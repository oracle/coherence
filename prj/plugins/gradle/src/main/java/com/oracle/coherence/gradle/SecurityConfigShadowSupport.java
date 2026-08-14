/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.AbstractCopyTask;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

/**
 * Configures Coherence security configuration merging for Gradle Shadow.
 *
 * @author Aleks Seovic  2026.08.10
 * @since 26.1
 */
public final class SecurityConfigShadowSupport
    {
    // ----- constructors ---------------------------------------------------

    private SecurityConfigShadowSupport()
        {
        }

    // ----- SecurityConfigShadowSupport methods --------------------------

    /**
     * Configure Shadow integration for a project using the Coherence Gradle
     * plugin. Repeated calls are ignored.
     *
     * @param project  the Gradle project
     */
    public static void configure(Project project)
        {
        if (project.getExtensions().getExtraProperties().has(CONFIGURED_MARKER))
            {
            return;
            }
        TaskContainer tasks = project.getTasks();
        SourceSet sourceSet = project.getExtensions().getByType(JavaPluginExtension.class)
                .getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        TaskProvider<SecurityConfigTask> taskSecurityConfig = tasks.named(
                CoherencePlugin.SECURITY_CONFIG_TASK_NAME, SecurityConfigTask.class);

        TaskProvider<SecurityConfigShadowTask> taskMerge = tasks.register(
                SECURITY_CONFIG_SHADOW_TASK_NAME, SecurityConfigShadowTask.class, task ->
                    {
                    task.setGroup("Coherence");
                    task.setDescription("Merge security-config.xml resources for a Shadow JAR.");
                    task.dependsOn(taskSecurityConfig);
                    task.dependsOn(tasks.named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME));
                    task.getClasspath().from(sourceSet.getRuntimeClasspath());
                    task.getOutputDirectory().set(project.getLayout().getBuildDirectory()
                            .dir("generated/coherence/security-config-shadow"));
                    });

        project.getPluginManager().withPlugin(SHADOW_PLUGIN_ID, plugin ->
                configureShadowTask(tasks, taskMerge));
        project.getPluginManager().withPlugin(LEGACY_SHADOW_PLUGIN_ID, plugin ->
                configureShadowTask(tasks, taskMerge));

        project.getExtensions().getExtraProperties().set(CONFIGURED_MARKER, Boolean.TRUE);
        }

    private static void configureShadowTask(TaskContainer tasks, TaskProvider<SecurityConfigShadowTask> taskMerge)
        {
        tasks.named(SHADOW_JAR_TASK_NAME, AbstractCopyTask.class)
                .configure(new SecurityConfigShadowTransformer(taskMerge));
        }

    // ----- constants ------------------------------------------------------

    /**
     * The Gradle task name used to merge Shadow security configuration.
     */
    public static final String SECURITY_CONFIG_SHADOW_TASK_NAME = "securityConfigShadow";

    private static final String SHADOW_PLUGIN_ID        = "com.gradleup.shadow";
    private static final String LEGACY_SHADOW_PLUGIN_ID = "com.github.johnrengelman.shadow";
    private static final String SHADOW_JAR_TASK_NAME    = "shadowJar";
    private static final String CONFIGURED_MARKER       = "coherence.securityConfig.shadow.configured";
    }
