/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import com.tangosol.internal.util.security.SecurityConfigGenerator;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.compile.JavaCompile;

/**
 * Gradle task configuration logic for main security-config generation.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.07
 */
public class SecurityConfigTaskRegistrationAction
        implements Action<SecurityConfigTask>
    {
    //----- constructors ----------------------------------------------------

    /**
     * Create a main security-config task registration action.
     *
     * @param extension         the security-config extension
     * @param project           the Gradle project
     * @param javaCompileTask   the Java compile task
     * @param resourcesFolders  the resource folders
     */
    public SecurityConfigTaskRegistrationAction(SecurityConfigExtension extension, Project project,
                                                JavaCompile javaCompileTask, FileCollection resourcesFolders)
        {
        m_extension        = extension;
        m_project          = project;
        m_javaCompileTask  = javaCompileTask;
        m_resourcesFolders = resourcesFolders;
        }

    //----- Action methods -------------------------------------------------

    @Override
    public void execute(SecurityConfigTask task)
        {
        task.getLogger().debug("SecurityConfigTaskRegistrationAction.execute() called");
        task.setGroup("Coherence");
        task.setDescription("Generate security-config.xml.");
        configure(task);
        }

    //----- helper methods -------------------------------------------------

    /**
     * Configure the security-config task.
     *
     * @param task  the task to configure
     */
    protected void configure(SecurityConfigTask task)
        {
        m_project.getLogger().info("Setting up security-config task property conventions.");
        task.dependsOn(m_javaCompileTask);
        task.getClassesDirectory().set(m_javaCompileTask.getDestinationDirectory());
        task.getOutputFile().set(m_javaCompileTask.getDestinationDirectory()
                .file(SecurityConfigGenerator.OUTPUT_RESOURCE));
        task.getSkip().convention(false);

        if (m_extension.getSkip().isPresent())
            {
            task.getSkip().set(m_extension.getSkip());
            }
        }

    //----- data members ---------------------------------------------------

    /**
     * Reference to the settings and properties for the security-config task.
     */
    protected final SecurityConfigExtension m_extension;

    /**
     * The Gradle {@link Project}.
     */
    protected final Project m_project;

    /**
     * The Gradle {@link JavaCompile} task.
     */
    protected final JavaCompile m_javaCompileTask;

    /**
     * The Gradle resource folders.
     */
    protected final FileCollection m_resourcesFolders;
    }
