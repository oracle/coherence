/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.compile.JavaCompile;

/**
 * Gradle task configuration logic for test security-config generation.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public class SecurityConfigTestTaskRegistrationAction
        extends SecurityConfigTaskRegistrationAction
    {
    //----- constructors ----------------------------------------------------

    /**
     * Create a test security-config task registration action.
     *
     * @param extension             the security-config extension
     * @param project               the Gradle project
     * @param javaCompileTask       the Java test compile task
     * @param testResourcesFolders  the test resource folders
     */
    public SecurityConfigTestTaskRegistrationAction(SecurityConfigExtension extension, Project project,
                                                    JavaCompile javaCompileTask, FileCollection testResourcesFolders)
        {
        super(extension, project, javaCompileTask, testResourcesFolders);
        }

    //----- Action methods -------------------------------------------------

    @Override
    public void execute(SecurityConfigTask task)
        {
        task.getLogger().debug("SecurityConfigTestTaskRegistrationAction.execute() called");
        task.onlyIf(t -> m_extension.getProcessTestClasses().getOrElse(false));

        boolean fEnabled = m_extension.getProcessTestClasses().getOrElse(false);
        if (fEnabled)
            {
            task.setGroup("Coherence");
            task.setDescription("Generate test security-config.xml.");
            }

        configure(task);
        }
    }
